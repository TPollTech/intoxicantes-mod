package com.intoxicantes;

import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
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
 * O CÉREBRO DA DORNA: lote em curso (item + quantidade), timer de
 * fermentação e o resultado servido. Tudo persistido (ValueInput/Output) —
 * fechar o mundo, desligar o server ou descarregar o chunk não perde o
 * mosto. 100% server-side (o client só vê partículas).
 *
 * PERFORMANCE (regra 25): o tick conta em SEGUNDOS (20 ticks) — um lote de
 * 7 minutos são 420 verificações triviais, não 8400.
 */
public class DornaBebidaBlockEntity extends BlockEntity {

    /** Insumo do lote em curso (null = dorna vazia). */
    private ItemStack insumo = ItemStack.EMPTY;
    /** Ticks que faltam pra fechar a fermentação. */
    private int ticksRestantes;
    /** Total do lote atual (pra tooltip de progresso). */
    private int ticksTotal;
    /** Fermentação concluída esperando ser recolhida. */
    private boolean servido;
    private ItemStack resultado = ItemStack.EMPTY;

    /** 1 verificação por segundo (20 ticks). */
    private static final int INTERVALO = 20;

    public DornaBebidaBlockEntity(BlockPos pos, BlockState state) {
        super(IntoxicantesMod.DORNA_BEBIDA_ENTITY, pos, state);
    }

    // ==================================================== INTERAÇÃO

    /** Quantidade do insumo que esta stack carrega (a receita define). */
    public int qtdNecessaria(ItemStack stack) {
        Optional<ProcessosBebida.Dorna> r = ProcessosBebida.dornaQueAceita(
                stack.getItem(), stack.getCount());
        return r.map(ProcessosBebida.Dorna::qtdIn).orElse(0);
    }

    /**
     * Clique com item: carrega a dorna se a receita casar (quantidade EXATA —
     * a dorna não conta troco, igual a vida real: ou joga o lote inteiro ou
     * nada).
     */
    public boolean tentarCarregar(ServerLevel level, Player player, ItemStack stack) {
        if (fermentando() || servido) {
            return false; // lote em curso é respeitado
        }
        Optional<ProcessosBebida.Dorna> r = ProcessosBebida.dornaQueAceita(
                stack.getItem(), stack.getCount());
        if (r.isEmpty()) {
            return false;
        }
        ProcessosBebida.Dorna receita = r.get();
        this.insumo = stack.copy();
        this.resultado = new ItemStack(receita.output(), receita.qtdOut());
        this.ticksTotal = ProcessosBebida.tempoDorna();
        this.ticksRestantes = ticksTotal;
        this.servido = false;
        setChanged();
        sincronizar(); // v1.2.55: o client acorda o renderer (bolhas/vapor)
        avisar(player, Component.translatable("block.intoxicantes.dorna_carregada",
                ticksTotal / 20 / 60, (ticksTotal / 20) % 60));
        return true;
    }

    /** Clique de mão vazia: recolhe o pronto ou mostra o progresso. */
    public void interagir(ServerLevel level, Player player) {
        if (servido) {
            ItemStack saida = resultado.copy();
            resultado = ItemStack.EMPTY;
            servido = false;
            insumo = ItemStack.EMPTY;
            setChanged();
            sincronizar(); // v1.2.55: dorna vazia de novo
            entregar(level, player, saida);
            level.playSound(null, worldPosition, SoundEvents.BREWING_STAND_BREW,
                    SoundSource.BLOCKS, 0.8F, 1.2F);
            return;
        }
        if (fermentando()) {
            int seg = ticksRestantes / 20;
            avisar(player, Component.translatable("block.intoxicantes.dorna_fermentando",
                    seg / 60, seg % 60, porcentagem()));
        } else {
            avisar(player, Component.translatable("block.intoxicantes.dorna_vazia"));
        }
    }

    // ==================================================== MOTOR (server, 1x/s)

    public void tick(ServerLevel level) {
        if (!fermentando()) {
            return;
        }
        if (level.getGameTime() % INTERVALO != 0L) {
            return; // 1 verificação por segundo
        }
        ticksRestantes -= INTERVALO;
        // som ocasional de borbulha (a cada ~13s de jogo, o lote "respira")
        if (ticksRestantes > 0 && (ticksRestantes / 20) % 13 == 0) {
            level.playSound(null, worldPosition, SoundEvents.BUBBLE_COLUMN_UPWARDS_AMBIENT,
                    SoundSource.BLOCKS, 0.6F, 0.85F);
        }
        if (ticksRestantes <= 0) {
            ticksRestantes = 0;
            servido = true;
            setChanged();
            sincronizar(); // v1.2.55: fim do lote (renderer para)
            level.playSound(null, worldPosition, SoundEvents.BREWING_STAND_BREW,
                    SoundSource.BLOCKS, 0.9F, 0.8F);
        } else {
            setChanged();
            // v1.2.55: o VAPOR da fermentação (1 puff por segundo, server-side,
            // só enquanto borbulha — a máquina viva sem custo quando ociosa)
            if (level.getRandom().nextInt(3) == 0) {
                level.sendParticles(net.minecraft.core.particles.ParticleTypes.CAMPFIRE_COSY_SMOKE,
                        worldPosition.getX() + 0.5, worldPosition.getY() + 0.75,
                        worldPosition.getZ() + 0.5, 1, 0.2, 0.05, 0.2, 0.004);
            }
        }
    }

    public boolean fermentando() {
        return !insumo.isEmpty() && !servido && ticksRestantes > 0;
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

    /** Progresso do lote 0..1 (o client usa pro nível do caldo). */
    public float progressoClient() {
        if (ticksTotal <= 0) return 0F;
        return Math.max(0F, Math.min(1F, 1F - ticksRestantes / (float) ticksTotal));
    }

    private int porcentagem() {
        if (ticksTotal <= 0) {
            return 0;
        }
        return Math.max(0, Math.min(100,
                100 - (ticksRestantes * 100 / ticksTotal)));
    }

    private void entregar(ServerLevel level, Player player, ItemStack stack) {
        if (player instanceof ServerPlayer sp && sp.getInventory().add(stack)) {
            return;
        }
        // inventário cheio: dropa no chão (nunca apaga item)
        level.addFreshEntity(new ItemEntity(level,
                worldPosition.getX() + 0.5, worldPosition.getY() + 0.6,
                worldPosition.getZ() + 0.5, stack));
    }

    private void avisar(Player player, Component msg) {
        avisar(player, msg, true);
    }

    private void avisar(Player player, Component msg, boolean overlay) {
        if (player instanceof net.minecraft.server.level.ServerPlayer sp) {
            sp.sendSystemMessage(msg, overlay);
        } else {
            player.sendSystemMessage(msg);
        }
    }

    // ==================================================== PERSISTÊNCIA

    @Override
    protected void loadAdditional(ValueInput input) {
        super.loadAdditional(input);
        ValueInput raiz = input.childOrEmpty("lote");
        this.insumo = raiz.read("insumoStack", ItemStack.CODEC).orElse(ItemStack.EMPTY);
        this.resultado = raiz.read("resultadoStack", ItemStack.CODEC).orElse(ItemStack.EMPTY);
        this.ticksRestantes = raiz.getIntOr("ticksRestantes", 0);
        this.ticksTotal = raiz.getIntOr("ticksTotal", 0);
        this.servido = raiz.getBooleanOr("servido", false);
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
        raiz.putInt("ticksRestantes", ticksRestantes);
        raiz.putInt("ticksTotal", ticksTotal);
        raiz.putBoolean("servido", servido);
    }
}
