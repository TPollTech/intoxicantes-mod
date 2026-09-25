package com.intoxicantes;

import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;

/**
 * O CÉREBRO DA MÁQUINA DE PRIMA: executa a receita do {@link ProcessosBebida}.
 *
 * Modo simples (moenda/prensa): insumo exato → timer curto → produto (+ extra).
 *
 * Modo DUAS DOSES (caldeirão de mostura, spec 11): 1º carga = MALTE →
 * mostura (o timer corre); terminou, pede o SEGUNDO ingrediente (o LÚPULO
 * da fervura) → fervura → mosto lupulado servido. E o caldeirão só carrega
 * com ÁGUA embaixo (a diluição da mostura) — a dica aparece na interação.
 */
import java.util.Optional;

public class MaquinaPrimaBlockEntity extends BlockEntity {

    private final MaquinaPrimaBlock.Tipo tipo;
    private ItemStack insumo = ItemStack.EMPTY;
    private ItemStack resultado = ItemStack.EMPTY;
    private ItemStack extra = ItemStack.EMPTY;
    private int ticksRestantes;
    private int ticksTotal;
    private boolean servido;
    /** Caldeirão: 1ª dose processada, esperando o 2º ingrediente (lúpulo). */
    private boolean esperandoSegunda;
    /** Caldeirão: o lúpulo foi adicionado — a fervura em curso pode servir. */
    private boolean fervuraEmCurso;

    private static final int INTERVALO = 20;

    public MaquinaPrimaBlockEntity(BlockPos pos, BlockState state, MaquinaPrimaBlock.Tipo tipo) {
        super(switch (tipo) {
            case MOENDA -> IntoxicantesMod.MOENDA_CANA_ENTITY;
            case PRENSA -> IntoxicantesMod.PRENSA_UVAS_ENTITY;
            case CALDEIRAO -> IntoxicantesMod.CALDEIRAO_MOSTURA_ENTITY;
        }, pos, state);
        this.tipo = tipo;
    }

    public MaquinaPrimaBlock.Tipo tipo() {
        return tipo;
    }

    // ==================================================== INTERAÇÃO

    public int qtdNecessaria(ItemStack stack) {
        Optional<ProcessosBebida.Prima> r = ProcessosBebida.primaDe(tipo,
                stack.getItem(), stack.getCount());
        return r.map(ProcessosBebida.Prima::qtdIn).orElse(0);
    }

    /** O caldeirão exige água (fonte ou caldeirão cheio) embaixo. */
    private boolean temAgua(ServerLevel level) {
        if (tipo != MaquinaPrimaBlock.Tipo.CALDEIRAO) {
            return true;
        }
        var estado = level.getBlockState(worldPosition.below());
        if (estado.is(net.minecraft.world.level.block.Blocks.WATER)) {
            return true;
        }
        return estado.hasProperty(net.minecraft.world.level.block.state.properties.BlockStateProperties.LEVEL_CAULDRON)
                && estado.getValue(net.minecraft.world.level.block.state.properties.BlockStateProperties.LEVEL_CAULDRON) > 0;
    }

    public boolean tentarCarregar(ServerLevel level, Player player, ItemStack stack) {
        // 2ª dose do caldeirão (o lúpulo da fervura)
        if (esperandoSegunda && !insumo.isEmpty()) {
            Optional<ProcessosBebida.Prima> r = ProcessosBebida.primaDe(tipo,
                    insumo.getItem(), insumoQuantidadeCarregada());
            if (r.isPresent() && r.get().secIn() != null
                    && stack.getItem() == r.get().secIn()
                    && stack.getCount() >= r.get().secQtd()) {
                stack.shrink(r.get().secQtd());
                this.esperandoSegunda = false;
                this.fervuraEmCurso = true; // a próxima expiração SERVE (não volta pro lúpulo)
                this.ticksTotal = ModConfig.ticksDeSegundos(30F); // v1.2.51: seg→ticks
                this.ticksRestantes = ticksTotal; // a fervura com lúpulo
                setChanged();
                avisar(player, Component.translatable(
                "block.intoxicantes.prima_segunda_dose", ticksTotal / 20), true);
                level.playSound(null, worldPosition, SoundEvents.BREWING_STAND_BREW,
                        net.minecraft.sounds.SoundSource.BLOCKS, 0.8F, 1.1F);
                return true;
            }
            avisar(player, Component.translatable(
                "block.intoxicantes.prima_falta_segunda"), true);
            return false;
        }

        if (processando() || servido) {
            return false;
        }
        Optional<ProcessosBebida.Prima> r = ProcessosBebida.primaDe(tipo,
                stack.getItem(), stack.getCount());
        if (r.isEmpty()) {
            return false;
        }
        if (!temAgua(level)) {
            avisar(player, Component.translatable(
                    "block.intoxicantes.caldeirao_sem_agua"), true);
            return false;
        }
        ProcessosBebida.Prima rec = r.get();
        this.insumo = new ItemStack(rec.input(), rec.qtdIn());
        this.resultado = new ItemStack(rec.output(), rec.qtdOut());
        this.extra = rec.extraQtd() > 0 ? new ItemStack(rec.extraOut(), rec.extraQtd())
                : ItemStack.EMPTY;
        // 1ª dose (mostura): 40s (v1.2.51: seg→ticks — era 40 TICKS = 2 segundos)
        this.ticksTotal = ModConfig.ticksDeSegundos(40F);
        this.ticksRestantes = ticksTotal;
        this.servido = false;
        this.esperandoSegunda = false;
        this.fervuraEmCurso = false;
        setChanged();
        sincronizar(); // v1.2.55: o client acorda o renderer (rolos/parafuso)
        avisar(player, Component.translatable(
                "block.intoxicantes.prima_carregada", ticksTotal / 20), true);
        return true;
    }

    /** Overlay (actionbar) pra ServerPlayer; chat pro resto. */
    private void avisar(Player player, Component msg, boolean overlay) {
        if (player instanceof net.minecraft.server.level.ServerPlayer sp) {
            sp.sendSystemMessage(msg, overlay);
        } else {
            player.sendSystemMessage(msg);
        }
    }

    private int insumoQuantidadeCarregada() {
        return insumo.isEmpty() ? 0 : insumo.getCount();
    }

    public void interagir(ServerLevel level, Player player) {
        if (servido) {
            entregar(level, player, resultado.copy());
            if (!extra.isEmpty()) {
                entregar(level, player, extra.copy());
            }
            resultado = ItemStack.EMPTY;
            extra = ItemStack.EMPTY;
            insumo = ItemStack.EMPTY;
            servido = false;
            fervuraEmCurso = false;
            setChanged();
            sincronizar(); // v1.2.55: máquina vazia de novo
            level.playSound(null, worldPosition, SoundEvents.WOOD_PLACE,
                    net.minecraft.sounds.SoundSource.BLOCKS, 0.8F, 1.1F);
            return;
        }
        if (esperandoSegunda) {
            avisar(player, Component.translatable(
                "block.intoxicantes.prima_falta_segunda"), true);
            return;
        }
        if (processando()) {
            int seg = ticksRestantes / 20;
            avisar(player, Component.translatable(
                "block.intoxicantes.prima_processando", seg,
                    Math.max(0, Math.min(100, 100 - ticksRestantes * 100 / Math.max(1, ticksTotal)))),
                    true);
        } else {
            avisar(player, Component.translatable(
                "block.intoxicantes.prima_vazia"), true);
        }
    }

    // ==================================================== MOTOR

    public void tick(ServerLevel level) {
        if (servido || insumo.isEmpty() || esperandoSegunda) {
            return;
        }
        if (level.getGameTime() % INTERVALO != 0L) {
            return;
        }
        ticksRestantes -= INTERVALO;
        if (ticksRestantes <= 0) {
            // caldeirão: a mostura acabou → espera o lúpulo (não serve sozinho);
            // depois do lúpulo, a expiração é a FERVURA terminando = serve
            if (tipo == MaquinaPrimaBlock.Tipo.CALDEIRAO && temSegundaDosePendente()
                    && !fervuraEmCurso) {
                esperandoSegunda = true;
                level.playSound(null, worldPosition, SoundEvents.WOOD_HIT,
                        net.minecraft.sounds.SoundSource.BLOCKS, 0.8F, 0.9F);
            } else {
                ticksRestantes = 0;
                servido = true;
                level.playSound(null, worldPosition, SoundEvents.WOOD_BREAK,
                        net.minecraft.sounds.SoundSource.BLOCKS, 0.9F, 0.8F);
            }
        }
        setChanged();
        sincronizar(); // v1.2.55: progresso anda (barra/parafuso do client)
    }

    private boolean temSegundaDosePendente() {
        Optional<ProcessosBebida.Prima> r = ProcessosBebida.primaDe(tipo,
                insumo.getItem(), insumoQuantidadeCarregada());
        return r.isPresent() && r.get().secIn() != null;
    }

    public boolean processando() {
        return !insumo.isEmpty() && !servido && !esperandoSegunda && ticksRestantes > 0;
    }

    // ==================================================== SYNC DO RENDERER (v1.2.55)

    /** Manda o estado pro client (o renderer lê no getRenderData). */
    private void sincronizar() {
        if (level != null && !level.isClientSide()) {
            level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), 3);
        }
    }

    @Override
    public net.minecraft.nbt.CompoundTag getUpdateTag(HolderLookup.Provider registries) {
        return saveWithoutMetadata(registries);
    }

    @Override
    public net.minecraft.network.protocol.Packet<net.minecraft.network.protocol.game.ClientGamePacketListener> getUpdatePacket() {
        return net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket.create(this);
    }

    /** O estado vivo pro renderer (mesma convenção do PainelLed). */
    @Override
    public Object getRenderData() {
        return this;
    }

    /** Progresso do lote 0..1 (o client usa no parafuso/barra). */
    public float progressoClient() {
        if (ticksTotal <= 0) return 0F;
        return Math.max(0F, Math.min(1F, 1F - ticksRestantes / (float) ticksTotal));
    }

    /** O TIPO da máquina (o renderer escolhe a animação: rolo/parafuso/rodopio). */
    public MaquinaPrimaBlock.Tipo tipoClient() {
        return tipo;
    }

    private void entregar(ServerLevel level, Player player, ItemStack stack) {
        if (player instanceof net.minecraft.server.level.ServerPlayer sp
                && sp.getInventory().add(stack)) {
            return;
        }
        level.addFreshEntity(new ItemEntity(level,
                worldPosition.getX() + 0.5, worldPosition.getY() + 0.7,
                worldPosition.getZ() + 0.5, stack));
    }

    // ==================================================== PERSISTÊNCIA

    @Override
    protected void loadAdditional(ValueInput input) {
        super.loadAdditional(input);
        ValueInput raiz = input.childOrEmpty("lote");
        this.insumo = raiz.read("insumoStack", ItemStack.CODEC).orElse(ItemStack.EMPTY);
        this.resultado = raiz.read("resultadoStack", ItemStack.CODEC).orElse(ItemStack.EMPTY);
        this.extra = raiz.read("extraStack", ItemStack.CODEC).orElse(ItemStack.EMPTY);
        this.ticksRestantes = raiz.getIntOr("ticksRestantes", 0);
        this.ticksTotal = raiz.getIntOr("ticksTotal", 0);
        this.servido = raiz.getBooleanOr("servido", false);
        this.esperandoSegunda = raiz.getBooleanOr("esperandoSegunda", false);
        this.fervuraEmCurso = raiz.getBooleanOr("fervuraEmCurso", false);
    }

    @Override
    protected void saveAdditional(ValueOutput output) {
        super.saveAdditional(output);
        ValueOutput raiz = output.child("lote");
        if (!insumo.isEmpty()) {
            raiz.store("insumoStack", ItemStack.CODEC, insumo);
        }
        if (!resultado.isEmpty()) {
            raiz.store("resultadoStack", ItemStack.CODEC, resultado);
        }
        if (!extra.isEmpty()) {
            raiz.store("extraStack", ItemStack.CODEC, extra);
        }
        raiz.putInt("ticksRestantes", ticksRestantes);
        raiz.putInt("ticksTotal", ticksTotal);
        raiz.putBoolean("servido", servido);
        raiz.putBoolean("esperandoSegunda", esperandoSegunda);
        raiz.putBoolean("fervuraEmCurso", fervuraEmCurso);
    }
}
