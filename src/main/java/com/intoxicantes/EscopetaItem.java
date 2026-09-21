package com.intoxicantes;

import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

/**
 * A Escopeta do Gago versao do jogador: pump-action de calibre 12 ficticio.
 * Cartucho por tiro (8 balins com dispersao), 128 usos, cooldown e recuo.
 * Municao: intoxicantes:cartucho (polvora + papel + prego de ferro).
 */
public class EscopetaItem extends Item {

    private static final int COOLDOWN_TICKS = 20;
    // valores configuraveis em config/intoxicantes.json (padrao do ModConfig)
    private static int BALINS() { return ModConfig.get().escopetaBalins; }
    private static float DANO_POR_BALIM() { return ModConfig.get().escopetaDanoPorBalim; }
    private static double ALCANCE_MAXIMO() { return ModConfig.get().escopetaAlcanceMaximo; }

    public EscopetaItem(Properties properties) {
        super(properties);
    }

    @Override
    public void appendHoverText(ItemStack stack, Item.TooltipContext context,
            net.minecraft.world.item.component.TooltipDisplay display,
            java.util.function.Consumer<net.minecraft.network.chat.Component> output,
            net.minecraft.world.item.TooltipFlag flag) {
        output.accept(net.minecraft.network.chat.Component.translatable("item.intoxicantes.escopeta.dica"));
        super.appendHoverText(stack, context, display, output, flag);
    }

    @Override
    public InteractionResult use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);

        if (!consumirMunicao(player)) {
            // click seco de percurssor vazia + aviso na action bar (o momento certo de saber)
            if (level instanceof ServerLevel servidor) {
                servidor.playSound(null, player.getX(), player.getY(), player.getZ(),
                        SoundEvents.WOODEN_TRAPDOOR_CLOSE, SoundSource.PLAYERS, 0.6F, 1.9F);
                player.sendSystemMessage(
                        net.minecraft.network.chat.Component.translatable("item.intoxicantes.escopeta.semmunicao"));
            }
            return InteractionResult.FAIL;
        }

        if (level instanceof ServerLevel servidor) {
            atirar(servidor, player, stack);
            // indicador de municao na ACTION BAR (overlay): o bombeador sabe quantos tem
            int restantes = contarCartuchos(player);
            player.sendOverlayMessage(net.minecraft.network.chat.Component.translatable(
                    "item.intoxicantes.escopeta.cartuchos", restantes));
        }
        return InteractionResult.SUCCESS;
    }

    /** Cartuchos no inventario (0 se criativo — nao gasta mesmo). */
    private static int contarCartuchos(Player player) {
        if (player.getAbilities().instabuild) {
            return -1;
        }
        int total = 0;
        var inventario = player.getInventory();
        for (int i = 0; i < inventario.getContainerSize(); i++) {
            ItemStack slot = inventario.getItem(i);
            if (slot.is(IntoxicantesMod.CARTUCHO)) {
                total += slot.getCount();
            }
        }
        return total;
    }

    /** Gasta 1 cartucho do inventario (criativo nao gasta). */
    private boolean consumirMunicao(Player player) {
        if (player.getAbilities().instabuild) {
            return true;
        }
        var inventario = player.getInventory();
        for (int i = 0; i < inventario.getContainerSize(); i++) {
            ItemStack slot = inventario.getItem(i);
            if (slot.is(IntoxicantesMod.CARTUCHO)) {
                slot.shrink(1);
                return true;
            }
        }
        return false;
    }

    private void atirar(ServerLevel level, Player player, ItemStack stack) {
        Vec3 origem = player.getEyePosition();
        Vec3 olhando = player.getViewVector(1.0F);
        Vec3 boca = origem.add(olhando.scale(1.1));

        // som PROPRIETARIO do mod (soco_d12.ogg) + corpo grave + bomba mecanica
        level.playSound(null, player.getX(), player.getY(), player.getZ(),
                IntoxicantesMod.SOCO_D12, SoundSource.PLAYERS, 2.0F, 1.0F);
        level.playSound(null, player.getX(), player.getY(), player.getZ(),
                SoundEvents.WOODEN_TRAPDOOR_CLOSE, SoundSource.PLAYERS, 1.3F, 0.5F);
        level.sendParticles(ParticleTypes.LARGE_SMOKE, boca.x, boca.y, boca.z,
                8, 0.1, 0.1, 0.1, 0.02);
        level.sendParticles(ParticleTypes.FLAME, boca.x, boca.y, boca.z,
                4, 0.05, 0.05, 0.05, 0.01);

        // 8 balins com dispersao que cresce com a distancia (hitscan).
        // Acumulo o dano por vitima e aplico UMA vez (balins no mesmo tick casariam
        // nos i-frames do vanilla). Deteccao de acerto por CONE: amostro pontos ao
        // longo do raio e testo esfera-a-esfera contra os AABBs — o sampler do
        // ProjectileUtil varre por celulas de chunk no stream e deixa alvos finos
        // escorrerem entre as celulas; em jogo isso virou "a 12 nao da dano em ninguem".
        java.util.Map<LivingEntity, Double> feridos = new java.util.HashMap<>();
        java.util.List<LivingEntity> alvos = level.getEntitiesOfClass(LivingEntity.class,
                player.getBoundingBox().inflate(ALCANCE_MAXIMO() + 4),
                a -> a != player && a.isAlive() && !a.isSpectator());
        int acertosTotais = 0;
        for (int i = 0; i < BALINS(); i++) {
            double desvio = 0.05;
            Vec3 mira = origem.add(olhando.scale(ALCANCE_MAXIMO())).add(
                    (player.getRandom().nextDouble() - 0.5) * 2 * desvio * ALCANCE_MAXIMO(),
                    (player.getRandom().nextDouble() - 0.5) * 2 * desvio * ALCANCE_MAXIMO()
                            + ALCANCE_MAXIMO() * 0.005,
                    (player.getRandom().nextDouble() - 0.5) * 2 * desvio * ALCANCE_MAXIMO());
            Vec3 direcao = mira.subtract(origem).normalize();
            Vec3 fim = origem.add(direcao.scale(ALCANCE_MAXIMO() + 1.5));

            // alcance efetivo do balim: clip que ATRAVESSA vidro/pane (vitrine do
            // mercado, janelas). Vidro NAO e' full-block -> "nao trava chumbo"
            Vec3 inicio = origem;
            BlockHitResult bloco = level.clip(new ClipContext(inicio, fim,
                    ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, player));
            for (int perfurar = 0; perfurar < 4 && bloco.getType() != HitResult.Type.MISS
                    && !level.getBlockState(bloco.getBlockPos()).isCollisionShapeFullBlock(
                            level, bloco.getBlockPos()); perfurar++) {
                inicio = bloco.getLocation().add(direcao.scale(0.1));
                bloco = level.clip(new ClipContext(inicio, fim,
                        ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, player));
            }
            double alcance = bloco.getType() == HitResult.Type.MISS
                    ? ALCANCE_MAXIMO() + 1.5
                    : bloco.getLocation().distanceTo(origem);

            // cone do balim: 12 amostras do raiinho; a vitima mais PROXIMA leva (o
            // balim para na primeira coisa que atravessa)
            LivingEntity vitima = null;
            double melhorDist = Double.MAX_VALUE;
            for (double d = 0.8; d <= alcance; d += (alcance - 0.4) / 12.0) {
                Vec3 ponto = origem.add(direcao.scale(d));
                for (LivingEntity a : alvos) {
                    double raio = a.getBbWidth() / 2.0 + 0.6 + d / ALCANCE_MAXIMO();
                    if (a.getBoundingBox().inflate(raio).contains(ponto) && d < melhorDist) {
                        vitima = a;
                        melhorDist = d;
                    }
                }
            }
            if (vitima != null) {
                // falloff previsivel: 100% de perto ate ~8 blocos, decaindo ate 35%
                // no limite do alcance — forte de perto, honesto de longe
                double fator = Math.max(0.35, 1.0 - melhorDist / 32.0);
                feridos.merge(vitima, DANO_POR_BALIM() * fator, Double::sum);
                acertosTotais++;
            }
        }
        for (var ferido : feridos.entrySet()) {
            LivingEntity v = ferido.getKey();
            Chumbo.aplicar(level, v, player.damageSources().playerAttack(player),
                    ferido.getValue().floatValue());
            // v1.2.7: punchline do chumbo - "ding" agudo + nota subindo da vitima.
            // Dano mecanico, comédia auditiva: quem toma 12 fica com o punchline.
            level.playSound(null, v.getX(), v.getY(), v.getZ(),
                    IntoxicantesMod.BALIM_ACERTO, SoundSource.PLAYERS, 0.7F, 1.0F);
            level.sendParticles(ParticleTypes.NOTE,
                    v.getX(), v.getY() + v.getBbHeight() + 0.2, v.getZ(),
                    1, 0.2, 0.1, 0.2, 1.0);
        }
        // diagnostico: cada tiro registra o resultado. Se algum dia "nao da dano"
        // de novo, o log diz onde a cadeia quebrou (0 balins = mira/AABB; vitima
        // presente mas 0 dano = escudo/dano source).
        IntoxicantesMod.LOGGER.info("[Escopeta] tiro: {} balim(s) em {} vitima(s)",
                acertosTotais, feridos.size());

        // recuo: empurra o jogador pra tras (hurtMarked sincroniza com o cliente)
        player.push(-olhando.x * 0.6, 0.18, -olhando.z * 0.6);
        player.syncVelocity = true;

        // durabilidade (quebra com som, igual vanilla)
        if (!player.getAbilities().instabuild && player instanceof ServerPlayer serverPlayer) {
            stack.hurtAndBreak(1, level, serverPlayer, sq ->
                    level.playSound(null, player.getX(), player.getY(), player.getZ(),
                            SoundEvents.ITEM_BREAK.value(), SoundSource.PLAYERS, 1.0F, 0.9F));
        }

        player.getCooldowns().addCooldown(stack, ModConfig.get().escopetaCooldownTicks);
    }
}
