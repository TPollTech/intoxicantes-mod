package com.intoxicantes;

import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtAccounter;
import net.minecraft.nbt.NbtIo;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.clock.WorldClock;
import net.minecraft.world.clock.WorldClocks;
import net.minecraft.world.level.levelgen.structure.Structure;
import net.minecraft.world.level.levelgen.structure.StructureStart;
import net.minecraft.world.level.storage.LevelResource;
import net.minecraft.world.phys.AABB;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;

import org.jspecify.annotations.Nullable;

/**
 * Sistema do Mercado Esquinão do Gago.
 * - Gago atende 24h, nunca some
 * - 07:00 ~ 00:00: fica no balcão (area principal)
 * - 00:00 ~ 07:00: fica na porta do mercado
 * - Clock na action bar perto do mercado
 * - Posição do mercado é PERSISTENTE (NBT no diretório do mundo) e, se o admin
 *   nunca setou, o sistema acha a estrutura mercado_gago sozinho (igual /locate)
 */
public final class MarketSystem {
    private static final int MARKET_RANGE = 32;
    private static final int CLOCK_INTERVAL = 100;
    private static final int MANAGE_INTERVAL = 100;
    private static final int DISCOVER_INTERVAL = 600; // 30s entre tentativas de achar a estrutura
    private static final int SCAN_BUDGET_POR_TENTATIVA = 600; // chunks NOVOS por tentativa (o cursor nunca revista)
    private static final int DISCOVER_BACKOFF = 12000; // rodada completa sem achar: re-tenta daqui a 10 min
    private static final int DISCOVER_RADIUS_CHUNKS = 24;
    private static final int CLOCK_COOLDOWN = 60;     // action bar no max 1x a cada 3s por player
    private static final String DATA_FILE = "intoxicantes_market.dat";

    /**
     * Offsets locais (rotacao NONE) a partir do CENTRO do template 15x5x11:
     * - porta: alpendre na frente da porta (z=10, centro x=7)
     * - balcao: atras do balcão (x=4, z=3) — onde o Gago nasce pelo template
     * O jigsaw gira os DOIS junto com a rotacao real da estrutura.
     */
    private static final net.minecraft.core.Vec3i PORTA_LOCAL = new net.minecraft.core.Vec3i(0, 0, 5);
    private static final net.minecraft.core.Vec3i BALCAO_LOCAL = new net.minecraft.core.Vec3i(-3, 0, -2);

    @Nullable
    private static BlockPos marketPos = null;
    /** Offsets JA girados (padrao: sem rotacao). */
    private static net.minecraft.core.Vec3i portaOffset = PORTA_LOCAL;
    private static net.minecraft.core.Vec3i balcaoOffset = BALCAO_LOCAL;
    private static java.util.UUID gagoAtivoId = null; // evita spawn duplicado quando o Gago sai puto
    private static boolean discovered = false; // achou a estrutura? (nao tenta de novo)
    /** Cursor da espiral: anel atual e posicao dentro dele (nunca revista chunk). */
    private static int varreduraRaio = 0;
    private static int varreduraIndice = 0;
    private static boolean avisouVarreduraCompleta = false;
    /** Centro da espiral fixado na sessao: o cursor so faz sentido com centro fixo. */
    @Nullable
    private static ChunkPos centroVarredura = null;
    private static int clockCooldownTick = 0;
    private static int manageCooldown = 0;
    private static int discoverCooldown = 0;

    private MarketSystem() {}

    public static void register() {
        ServerTickEvents.END_SERVER_TICK.register(server -> {
            ServerLevel overworld = server.overworld();
            if (overworld == null) return;

            // 1) Se ainda nao tem posicao, tenta achar a estrutura (espiral de chunks)
            if (marketPos == null) {
                if (!discovered) {
                    if (--discoverCooldown <= 0) {
                        discoverCooldown = DISCOVER_INTERVAL;
                        tryDescobrirEstrutura(overworld);
                    }
                }
                if (marketPos == null) return; // sem posicao ainda: nada a fazer
            }

            BlockPos pos = marketPos;

            // 2) Clock na action bar
            if (--clockCooldownTick <= 0) {
                clockCooldownTick = CLOCK_INTERVAL;
                String hora = formatarHora(overworld);
                String local = estaNaPorta(overworld) ? "\u00a77na porta" : "\u00a77no balcao";
                for (ServerPlayer player : server.getPlayerList().getPlayers()) {
                    boolean noOverworld = player.level().dimension().equals(Level.OVERWORLD);
                    if (noOverworld && player.blockPosition().distSqr(pos) < MARKET_RANGE * MARKET_RANGE) {
                        // v1.2.14: o local ("na porta"/"no balcao") vai pro lang —
                        // era pt cravado no código (misturava com inglês no cliente)
                        player.sendSystemMessage(Component.translatable(
                                "market.intoxicantes.relogio", hora,
                                Component.translatable(estaNaPorta(overworld)
                                        ? "market.intoxicantes.local.porta"
                                        : "market.intoxicantes.local.balcao")),
                                true);
                    }
                }
            }

            // 3) Gerenciador do Gago (throttle de 5s)
            if (--manageCooldown > 0) return;
            manageCooldown = MANAGE_INTERVAL;
            gerenciarGago(overworld, pos);
        });
    }

    // ==================================================== DESCOBERTA DA ESTRUTURA

    /**
     * Procura a estrutura mercado_gago numa espiral de chunks (estilo /locate),
     * com CURSOR PROGRESSIVO: cada tentativa gasta no maximo SCAN_BUDGET_POR_TENTATIVA
     * chunks NOVOS (nunca revista o que ja descartou) e o anel/indice ficam guardados
     * pra continuar de onde parou. Uma rodada completa (raio 24 ~ 2401 chunks, ~4
     * tentativas) sem achar entra em backoff de 10 min antes de recomecar.
     */
    private static void tryDescobrirEstrutura(ServerLevel level) {
        ResourceKey<Structure> chave = ResourceKey.create(Registries.STRUCTURE,
                Identifier.fromNamespaceAndPath(IntoxicantesMod.MOD_ID, "mercado_gago"));
        Holder<Structure> estrutura = level.registryAccess()
                .lookupOrThrow(Registries.STRUCTURE)
                .get(chave)
                .orElse(null);
        if (estrutura == null) {
            // JSON de worldgen ausente: desiste de tentar (log uma vez por boot)
            discovered = true;
            return;
        }
        // centro fixo por sessao: se o spawn mudar no meio, a espiral NAO escorrega
        if (centroVarredura == null) {
            centroVarredura = ChunkPos.containing(level.getRespawnData().pos());
        }
        ChunkPos centro = centroVarredura;
        var manager = level.structureManager();
        int checados = 0;
        while (varreduraRaio <= DISCOVER_RADIUS_CHUNKS && checados < SCAN_BUDGET_POR_TENTATIVA) {
            int[] d = chunkDoAnel(varreduraRaio, varreduraIndice);
            int cx = centro.x() + d[0];
            int cz = centro.z() + d[1];
            List<StructureStart> starts = manager.startsForStructure(cx, cz, estrutura.value());
            for (StructureStart start : starts) {
                if (start != null && start.isValid()) {
                    var bb = start.getBoundingBox();
                    // centro no PISO da estrutura (minY), nao no meio do telhado
                    setMarketPos(new BlockPos(
                            bb.minX() + bb.getXSpan() / 2,
                            bb.minY(),
                            bb.minZ() + bb.getZSpan() / 2));
                    descobrirOffsets(start);
                    discovered = true;
                    IntoxicantesMod.LOGGER.info("[Market] Mercado Esquinao descoberto em {} (varredura: {} chunks)",
                            marketPos.toShortString(), chunksVarridos(varreduraRaio, varreduraIndice));
                    anunciarDescoberta(level);
                    return;
                }
            }
            checados++;
            avancarCursor();
        }
        if (varreduraRaio > DISCOVER_RADIUS_CHUNKS) {
            // rodada completa sem achar (setado errado? removeram o worldgen?):
            // reinicia a espiral e espera o backoff — a estrutura pode aparecer
            // num /gagomarket set ou num datapack carregado depois
            if (!avisouVarreduraCompleta) {
                avisouVarreduraCompleta = true;
                IntoxicantesMod.LOGGER.warn("[Market] espiral completa (raio {}) sem achar mercado_gago; recomecando em {}s",
                        DISCOVER_RADIUS_CHUNKS, DISCOVER_BACKOFF / 20);
            }
            varreduraRaio = 0;
            varreduraIndice = 0;
            discoverCooldown = DISCOVER_BACKOFF;
        }
    }

    /** Chunk `indice` do anel `raio` em espiral (raio 0 = so o centro). Ordem estavel. */
    private static int[] chunkDoAnel(int raio, int indice) {
        if (raio == 0) return new int[]{0, 0};
        int lado = 2 * raio;
        int perimetro = 4 * lado;
        int i = indice % perimetro; // defesa: indice nunca passa do perimetro
        // topo (esq->dir), direita (cima->baixo), base (dir->esq), esquerda (baixo->cima)
        if (i < lado) return new int[]{-raio + i, -raio};
        i -= lado;
        if (i < lado) return new int[]{raio, -raio + i};
        i -= lado;
        if (i < lado) return new int[]{raio - i, raio};
        i -= lado;
        return new int[]{-raio, raio - i};
    }

    /** Avanca o cursor da espiral (indice dentro do anel, depois o anel). */
    private static void avancarCursor() {
        varreduraIndice++;
        if (varreduraIndice >= tamanhoAnel(varreduraRaio)) {
            varreduraIndice = 0;
            varreduraRaio++;
        }
    }

    /** Numero de chunks no anel `raio` (raio 0 = 1 chunk). */
    private static int tamanhoAnel(int raio) {
        return raio == 0 ? 1 : 8 * raio;
    }

    /** Chunks varridos ate a posicao atual do cursor (so pra log). */
    private static int chunksVarridos(int raio, int indice) {
        int total = indice;
        for (int r = 0; r < raio; r++) total += tamanhoAnel(r);
        return total;
    }

    private static void anunciarDescoberta(ServerLevel level) {
        for (ServerPlayer player : level.players()) {
            // v1.2.14: pro lang (era pt cravado)
            player.sendSystemMessage(Component.translatable("market.intoxicantes.descoberta"));
        }
    }

    // ==================================================== GAGERO (gerente do Gago)

    private static void gerenciarGago(ServerLevel level, BlockPos pos) {
        boolean naPorta = estaNaPorta(level);
        // raio 48: cobre a perseguição quando o Gago fica puto e sai correndo
        var gagos = level.getEntitiesOfClass(
                GagoEntity.class, new AABB(pos).inflate(48));

        GagoEntity gago = null;
        for (GagoEntity g : gagos) {
            if (g.isPuto() || (gagoAtivoId != null && g.getUUID().equals(gagoAtivoId))) {
                gago = g;
                break;
            }
        }
        if (gago == null && !gagos.isEmpty()) {
            gago = gagos.get(0);
        }
        if (gago == null) {
            spawnarGago(level, naPorta ? getPosPorta(pos) : getPosBalcao(pos));
            return;
        }
        gagoAtivoId = gago.getUUID();

        // EM ATENDIMENTO: ninguem toca no vendedor — sem teleporte de plantao,
        // sem re-organizacao de posto. O tick dele congela a IA; quando a tela
        // fechar (stopTrading) ele volta pro posto sozinho.
        if (gago.isTrading()) {
            return;
        }

        BlockPos destino = naPorta ? getPosPorta(pos) : getPosBalcao(pos);
        // Gago PUTO ninguem mexe: deixa ele perseguir em paz; quando a raiva passar,
        // o tick dele o devolve ao posto e o gerenciador reassume
        if (gago.isPuto()) return;

        // Teleporta (mudanca de plantao) ou organiza o posto
        if (gago.blockPosition().distSqr(destino) > 4) {
            gago.anunciarMudancaPosicao(level);
            gago.absSnapTo(destino.getX() + 0.5, destino.getY(), destino.getZ() + 0.5, gago.getYRot(), gago.getXRot());
        }
        gago.setupPostoMercado(destino);
    }

    private static void spawnarGago(ServerLevel level, BlockPos pos) {
        GagoEntity gago = IntoxicantesMod.GAGO.create(level, EntitySpawnReason.EVENT);
        if (gago == null) return;
        gago.absSnapTo(pos.getX() + 0.5, pos.getY(), pos.getZ() + 0.5, 180F, 0F);
        gago.finalizeSpawn(level, level.getCurrentDifficultyAt(pos), EntitySpawnReason.EVENT, null);
        gago.setPersistenceRequired();
        gago.setupPostoMercado(pos);
        level.addFreshEntity(gago);
        // se apresente pro fregues mais proximo (a frase de chegada finalmente roda)
        Player maisPerto = level.getNearestPlayer(pos.getX(), pos.getY(), pos.getZ(), 32.0, false);
        if (maisPerto != null) {
            gago.anunciarChegada(level, maisPerto);
        }
    }

    /** Atras do balcao (posto do plantao diurno), JA girado pela rotacao real. */
    public static BlockPos getPosBalcao(BlockPos pos) {
        return pos.above().offset(balcaoOffset.getX(), balcaoOffset.getY(), balcaoOffset.getZ());
    }

    /** Alpendre na frente da porta (plantao da madrugada), JA girado. */
    public static BlockPos getPosPorta(BlockPos pos) {
        return pos.above().offset(portaOffset.getX(), portaOffset.getY(), portaOffset.getZ());
    }

    /** Offset da porta ja girado (usado pelo /gagomarket rebuild pra derivar a rotacao). */
    public static net.minecraft.core.Vec3i getPortaOffset() {
        return portaOffset;
    }

    /**
     * A estrutura gira aleatoriamente na geracao (jigsaw). Le a rotacao do piece e
     * gira os offsets locais da porta e do balcao junto — senao o Gago fica de
     * frente pro muro ou atras do balcao do vizinho kkkk.
     */
    private static void descobrirOffsets(StructureStart start) {
        net.minecraft.core.Vec3i porta = PORTA_LOCAL;
        net.minecraft.core.Vec3i balcao = BALCAO_LOCAL;
        for (net.minecraft.world.level.levelgen.structure.StructurePiece piece : start.getPieces()) {
            if (piece instanceof net.minecraft.world.level.levelgen.structure.PoolElementStructurePiece p) {
                var rot = p.getRotation();
                porta = girar(porta, rot);
                balcao = girar(balcao, rot);
                break;
            }
        }
        portaOffset = porta;
        balcaoOffset = balcao;
    }

    private static net.minecraft.core.Vec3i girar(net.minecraft.core.Vec3i local,
            net.minecraft.world.level.block.Rotation rot) {
        return switch (rot) {
            case NONE -> local;
            case CLOCKWISE_90 -> new net.minecraft.core.Vec3i(-local.getZ(), local.getY(), local.getX());
            case CLOCKWISE_180 -> new net.minecraft.core.Vec3i(-local.getX(), local.getY(), -local.getZ());
            case COUNTERCLOCKWISE_90 -> new net.minecraft.core.Vec3i(local.getZ(), local.getY(), -local.getX());
        };
    }

    // ==================================================== TEMPO

    /** true = 00:00 ~ 07:00 (Gago fica na porta) */
    public static boolean estaNaPorta(ServerLevel level) {
        return horaDoDia(level) < 7;
    }

    /** Ticks totais do clock do overworld (equivalente ao getDayTime das versoes antigas). */
    public static long tempoTotal(ServerLevel level) {
        Holder<WorldClock> clock = level.registryAccess()
                .lookupOrThrow(Registries.WORLD_CLOCK)
                .getOrThrow(WorldClocks.OVERWORLD);
        return level.clockManager().getInstance(clock).totalTicks();
    }

    public static int horaDoDia(ServerLevel level) {
        return (int) ((tempoTotal(level) / 1000L + 6) % 24);
    }

    public static String formatarHora(ServerLevel level) {
        long ticks = tempoTotal(level) % 24000L;
        int totalMinutos = (int) ((ticks * 24 * 60) / 24000);
        int hora = (totalMinutos / 60 + 6) % 24;
        int minuto = totalMinutos % 60;
        return String.format("%02d:%02d", hora, minuto);
    }

    // ==================================================== ESTADO / PERSISTENCIA

    @Nullable
    public static BlockPos getMarketPos() {
        return marketPos;
    }

    public static void setMarketPos(BlockPos pos) {
        marketPos = pos;
        portaOffset = PORTA_LOCAL;   // default; a descoberta sobrescreve com a rotacao real
        balcaoOffset = BALCAO_LOCAL;
        discovered = true;
    }

    /** Chamado no SERVER_STARTING: carrega o NBT salvo no diretorio do mundo. */
    public static void load(File worldDir) {
        File file = new File(worldDir, DATA_FILE);
        if (!file.exists()) return;
        try (InputStream in = new FileInputStream(file)) {
            CompoundTag tag = NbtIo.readCompressed(in, NbtAccounter.unlimitedHeap());
            if (tag.contains("x") && tag.contains("y") && tag.contains("z")) {
                marketPos = new BlockPos(tag.getIntOr("x", 0), tag.getIntOr("y", 0), tag.getIntOr("z", 0));
                portaOffset = new BlockPos(
                        tag.getIntOr("portax", PORTA_LOCAL.getX()),
                        tag.getIntOr("portay", PORTA_LOCAL.getY()),
                        tag.getIntOr("portaz", PORTA_LOCAL.getZ()));
                balcaoOffset = new BlockPos(
                        tag.getIntOr("balcaox", BALCAO_LOCAL.getX()),
                        tag.getIntOr("balcaoy", BALCAO_LOCAL.getY()),
                        tag.getIntOr("balcaoz", BALCAO_LOCAL.getZ()));
                discovered = true;
                IntoxicantesMod.LOGGER.info("[Market] Posicao do Mercado carregada: {}",
                        marketPos.toShortString());
            }
        } catch (IOException e) {
            IntoxicantesMod.LOGGER.warn("[Market] Erro ao carregar posicao do mercado: {}", e.getMessage());
        }
    }

    /** Chamado no SERVER_STOPPING: salva o NBT. */
    public static void save(File worldDir) {
        if (marketPos == null) return;
        File file = new File(worldDir, DATA_FILE);
        CompoundTag tag = new CompoundTag();
        tag.putInt("x", marketPos.getX());
        tag.putInt("y", marketPos.getY());
        tag.putInt("z", marketPos.getZ());
        tag.putInt("portax", portaOffset.getX());
        tag.putInt("portay", portaOffset.getY());
        tag.putInt("portaz", portaOffset.getZ());
        tag.putInt("balcaox", balcaoOffset.getX());
        tag.putInt("balcaoy", balcaoOffset.getY());
        tag.putInt("balcaoz", balcaoOffset.getZ());
        try (FileOutputStream out = new FileOutputStream(file)) {
            NbtIo.writeCompressed(tag, out);
        } catch (IOException e) {
            IntoxicantesMod.LOGGER.warn("[Market] Erro ao salvar posicao do mercado: {}", e.getMessage());
        }
    }

    // ==================================================== HELPER PRA OUTROS SISTEMAS

    /** O player ta dentro da area do mercado? */
    public static boolean estaNoMercado(Player player) {
        if (marketPos == null) return false;
        if (!player.level().dimension().equals(Level.OVERWORLD)) return false;
        return player.blockPosition().distSqr(marketPos) < 16 * 16;
    }

    /** Usado pelo /gagomarket (comando). Salva na hora pro server nao perder se der crash. */
    public static void setMarketPosComando(MinecraftServer server, BlockPos pos) {
        setMarketPos(pos);
        File worldDir = server.getWorldPath(LevelResource.ROOT).toFile();
        save(worldDir);
    }
}
