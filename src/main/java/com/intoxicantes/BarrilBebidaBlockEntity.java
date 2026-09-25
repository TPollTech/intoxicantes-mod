package com.intoxicantes;

import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import java.util.Optional;

import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;

/**
 * O CÉREBRO DO BARRIL: rótulo (qual bebida), fase (FERMENTANDO/MATURANDO/
 * PRONTO), timers de cada fase e as garrafas restantes no lote.
 *
 * Estados persistidos (spec 9): sobrevive a save/restart/chunk unload — os
 * ticks que faltam viajam no NBT e continuam de onde pararam.
 *
 * Lote: 1 carga do insumo = N garrafas (spec 14, config bebidaGarrafasPorLote,
 * padrão 4). A maturação acontece no BARRIL INTEIRO: cada garrafa retirada é
 * da mesma qualidade.
 */
public class BarrilBebidaBlockEntity extends BlockEntity {

    /** Fases do barril (spec 9). */
    public static final int FASE_VAZIA = 0;
    public static final int FASE_FERMENTANDO = 1;
    public static final int FASE_MATURANDO = 2;
    public static final int FASE_PRONTA = 3;

    /** Rótulo: "cachaca", "cerveja", "rum", "vinho" (grava no NBT). */
    private String bebida = "";
    /** Fase atual. */
    private int fase = FASE_VAZIA;
    /** Ticks restantes da fase corrente. */
    private int ticksRestantes;
    private int ticksTotalFase;
    /** Garrafas que ainda dá pra tirar do lote pronto. */
    private int garrafasRestantes;
    /** Insumo guardado (devolve se quebrar antes de pronto). */
    private ItemStack insumoGuardado = ItemStack.EMPTY;
    private int insumoQuantidade;

    private static final int INTERVALO = 20;

    public BarrilBebidaBlockEntity(BlockPos pos, BlockState state) {
        super(IntoxicantesMod.BARRIL_BEBIDA_ENTITY, pos, state);
    }

    // ==================================================== INTERAÇÃO

    /**
     * Clique no barril. Com garrafa de vidro: engarrafa (se PRONTO). Com o
     * insumo da receita (barril vazio): carrega. Mão vazia: status. Devolve
     * true se CONSUMIU o item da mão.
     */
    public boolean interagir(ServerLevel level, Player player, ItemStack mao) {
        Optional<ProcessosBebida.Barril> receita = getBebida().isEmpty()
                ? Optional.empty() : ProcessosBebida.barrilDe(getBebida());

        // 1) PRONTO + garrafa na mão = engarrafa 1
        if (fase == FASE_PRONTA && ProcessosBebida.eGarrafa(mao.getItem())) {
            if (receita.isEmpty() || garrafasRestantes <= 0) {
                return false;
            }
            ItemStack bebidaStack = new ItemStack(receita.get().bebidaFinal(), 1);
            entregar(level, player, bebidaStack);
            garrafasRestantes--;
            level.playSound(null, worldPosition, SoundEvents.BOTTLE_FILL,
                    SoundSource.BLOCKS, 0.8F, 1.0F);
            if (garrafasRestantes <= 0) {
                // lote acabou: barril volta a vazio (pronto pro próximo)
                fase = FASE_VAZIA;
                insumoGuardado = ItemStack.EMPTY;
                insumoQuantidade = 0;
                setChanged();
                avisar(player, Component.translatable(
                "block.intoxicantes.barril_lote_acabou"), true);
            } else {
                setChanged();
                avisar(player, Component.translatable(
                "block.intoxicantes.barril_engarrafou", garrafasRestantes), true);
            }
            return true;
        }

        // 2) VAZIO + insumo da receita na mão = carrega o lote
        if (fase == FASE_VAZIA && receita.isPresent()
                && mao.getItem() == receita.get().input()) {
            if (mao.getCount() < receita.get().qtdIn()) {
                avisar(player, Component.translatable(
                "block.intoxicantes.barril_falta_insumo",
                receita.get().qtdIn(), mao.getHoverName()), true);
                return false;
            }
            mao.shrink(receita.get().qtdIn());
            iniciarFase(receita.get());
            level.playSound(null, worldPosition, SoundEvents.BARREL_OPEN,
                    SoundSource.BLOCKS, 0.7F, 0.9F);
            return true;
        }

        // 3) status (mão vazia ou clique sem ação)
        mostrarStatus(level, player, receita);
        return false;
    }

    /** Inicia a 1ª fase válida da receita (fermentação ou direto a maturação). */
    private void iniciarFase(ProcessosBebida.Barril receita) {
        this.insumoGuardado = new ItemStack(receita.input(), 1); // só o "tipo" pro devolver
        this.insumoQuantidade = receita.qtdIn();
        if (ProcessosBebida.tempoFermentacao(receita) > 0) {
            this.fase = FASE_FERMENTANDO;
            this.ticksTotalFase = ProcessosBebida.tempoFermentacao(receita);
        } else {
            this.fase = FASE_MATURANDO;
            this.ticksTotalFase = ProcessosBebida.tempoMaturacao(receita);
        }
        this.ticksRestantes = ticksTotalFase;
        setChanged();
    }

    private void mostrarStatus(ServerLevel level, Player player,
            Optional<ProcessosBebida.Barril> receita) {
        switch (fase) {
            case FASE_FERMENTANDO -> {
                int seg = ticksRestantes / 20;
                avisar(player, Component.translatable(
                        "block.intoxicantes.barril_fermentando",
                        nomeBebida(), seg / 60, seg % 60,
                        Math.max(0, Math.min(100, 100 - ticksRestantes * 100 / Math.max(1, ticksTotalFase)))),
                        true);
            }
            case FASE_MATURANDO -> {
                int seg = ticksRestantes / 20;
                avisar(player, Component.translatable(
                        "block.intoxicantes.barril_maturando",
                        nomeBebida(), seg / 60, seg % 60,
                        Math.max(0, Math.min(100, 100 - ticksRestantes * 100 / Math.max(1, ticksTotalFase)))),
                        true);
            }
            case FASE_PRONTA -> avisar(player, Component.translatable(
                    "block.intoxicantes.barril_pronto", nomeBebida(), garrafasRestantes), true);
            default -> {
                if (receita.isPresent()) {
                    avisar(player, Component.translatable(
                            "block.intoxicantes.barril_vazio_receita",
                            new ItemStack(receita.get().input(), receita.get().qtdIn())
                                    .getHoverName()), true);
                } else {
                    avisar(player, Component.translatable(
                            "block.intoxicantes.barril_vazio"), true);
                }
            }
        }
    }

    /** Overlay (actionbar) pra ServerPlayer; chat pro resto. */
    private void avisar(Player player, Component msg, boolean overlay) {
        if (player instanceof net.minecraft.server.level.ServerPlayer sp) {
            sp.sendSystemMessage(msg, overlay);
        } else {
            player.sendSystemMessage(msg);
        }
    }

    private Component nomeBebida() {
        return Component.translatable("block.intoxicantes.barril_" + getBebida());
    }

    // ==================================================== MOTOR (server, 1x/s)

    public void tick(ServerLevel level) {
        if (fase != FASE_FERMENTANDO && fase != FASE_MATURANDO) {
            return;
        }
        if (level.getGameTime() % INTERVALO != 0L) {
            return;
        }
        ticksRestantes -= INTERVALO;
        // fim da fermentação: pula pra maturação (se existir) ou fica pronto
        if (ticksRestantes <= 0 && fase == FASE_FERMENTANDO) {
            Optional<ProcessosBebida.Barril> receita = ProcessosBebida.barrilDe(getBebida());
            int maturacao = receita.map(ProcessosBebida::tempoMaturacao).orElse(0);
            if (maturacao > 0) {
                fase = FASE_MATURANDO;
                ticksTotalFase = maturacao;
                ticksRestantes = maturacao;
                level.playSound(null, worldPosition, SoundEvents.BARREL_CLOSE,
                        SoundSource.BLOCKS, 0.7F, 0.8F);
            } else {
                concluir(level);
            }
        } else if (ticksRestantes <= 0) {
            concluir(level);
        } else {
            // borbulha ocasional (1 a cada ~17s)
            if ((ticksRestantes / 20) % 17 == 0) {
                level.playSound(null, worldPosition, SoundEvents.BUBBLE_COLUMN_UPWARDS_AMBIENT,
                        SoundSource.BLOCKS, 0.5F, 0.8F);
            }
        }
        setChanged();
    }

    private void concluir(ServerLevel level) {
        fase = FASE_PRONTA;
        ticksRestantes = 0;
        Optional<ProcessosBebida.Barril> receita = ProcessosBebida.barrilDe(getBebida());
        garrafasRestantes = receita.map(ProcessosBebida::garrafasPorLote).orElse(4);
        setChanged();
        level.playSound(null, worldPosition, SoundEvents.BARREL_CLOSE,
                SoundSource.BLOCKS, 0.8F, 1.15F);
    }

    public boolean fermentando() {
        return fase == FASE_FERMENTANDO;
    }

    /**
     * Quebrou o barril antes de pronto: devolve o INSUMO do lote (spec: não
     * perder o trabalho). Pronto: quem quebra leva o barril; as garrafas
     * restantes caem no chão junto.
     */
    public void soltarConteudo(ServerLevel level) {
        if (fase == FASE_PRONTA && garrafasRestantes > 0) {
            Optional<ProcessosBebida.Barril> receita = ProcessosBebida.barrilDe(getBebida());
            receita.ifPresent(r -> dropar(level,
                    new ItemStack(r.bebidaFinal(), garrafasRestantes)));
        } else if (insumoQuantidade > 0 && !insumoGuardado.isEmpty()) {
            dropar(level, new ItemStack(insumoGuardado.getItem(), insumoQuantidade));
        }
        limpar();
    }

    private void dropar(ServerLevel level, ItemStack stack) {
        level.addFreshEntity(new ItemEntity(level,
                worldPosition.getX() + 0.5, worldPosition.getY() + 0.5,
                worldPosition.getZ() + 0.5, stack));
    }

    private void entregar(ServerLevel level, Player player, ItemStack stack) {
        if (player instanceof net.minecraft.server.level.ServerPlayer sp
                && sp.getInventory().add(stack)) {
            return;
        }
        dropar(level, stack);
    }

    private void limpar() {
        bebida = "";
        fase = FASE_VAZIA;
        ticksRestantes = 0;
        ticksTotalFase = 0;
        garrafasRestantes = 0;
        insumoGuardado = ItemStack.EMPTY;
        insumoQuantidade = 0;
        setChanged();
    }

    /** Grava o rótulo no placement (o item sabe qual bebida carrega). */
    public void carregarRotulo(String novaBebida) {
        this.bebida = novaBebida == null ? "" : novaBebida;
        setChanged();
    }

    public String getBebida() {
        // Rótulo preguiçoso: blocos colocados por setBlock/estrutura/teste não
        // passam pelo setPlacedBy — deriva do próprio registro (barril_rum ->
        // "rum"). Blocos normais já chegam com rótulo pelo placement.
        if ((bebida == null || bebida.isEmpty())
                && getBlockState().getBlock() instanceof BarrilBebidaBlock barril) {
            bebida = barril.rotuloDoBlocoPublico();
            setChanged();
        }
        return bebida;
    }

    // ==================================================== PERSISTÊNCIA

    @Override
    protected void loadAdditional(ValueInput input) {
        super.loadAdditional(input);
        ValueInput raiz = input.childOrEmpty("barril");
        this.bebida = raiz.getStringOr("bebida", "");
        this.fase = raiz.getIntOr("fase", FASE_VAZIA);
        this.ticksRestantes = raiz.getIntOr("ticksRestantes", 0);
        this.ticksTotalFase = raiz.getIntOr("ticksTotalFase", 0);
        this.garrafasRestantes = raiz.getIntOr("garrafas", 0);
        this.insumoQuantidade = raiz.getIntOr("insumoQtd", 0);
        this.insumoGuardado = raiz.read("insumo", ItemStack.CODEC).orElse(ItemStack.EMPTY);
    }

    @Override
    protected void saveAdditional(ValueOutput output) {
        super.saveAdditional(output);
        ValueOutput raiz = output.child("barril");
        raiz.putString("bebida", bebida);
        raiz.putInt("fase", fase);
        raiz.putInt("ticksRestantes", ticksRestantes);
        raiz.putInt("ticksTotalFase", ticksTotalFase);
        raiz.putInt("garrafas", garrafasRestantes);
        raiz.putInt("insumoQtd", insumoQuantidade);
        if (!insumoGuardado.isEmpty()) {
            raiz.store("insumo", ItemStack.CODEC, insumoGuardado);
        }
    }
}
