package com.intoxicantes;

import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import java.util.Optional;

import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;

/**
 * O CÉREBRO DO ALAMBIQUE: lote de mosto fermentado, timer e a REGRA DO
 * CALOR — sem fogo embaixo, a destilação PAUSA (o mosto não estraga, o
 * cobre só esfria). Com fogo, o timer corre 1 checagem por segundo.
 */
public class AlambiqueBlockEntity extends BlockEntity {

    private ItemStack insumo = ItemStack.EMPTY;
    private ItemStack resultado = ItemStack.EMPTY;
    private int ticksRestantes;
    private int ticksTotal;
    private boolean servido;

    private static final int INTERVALO = 20;

    public AlambiqueBlockEntity(BlockPos pos, BlockState state) {
        super(IntoxicantesMod.ALAMBIQUE_ENTITY, pos, state);
    }

    // ==================================================== INTERAÇÃO

    public int qtdNecessaria(ItemStack stack) {
        Optional<ProcessosBebida.Alambique> r = ProcessosBebida.alambiqueQueAceita(
                stack.getItem(), stack.getCount());
        return r.map(ProcessosBebida.Alambique::qtdIn).orElse(0);
    }

    public boolean tentarCarregar(ServerLevel level, Player player, ItemStack stack) {
        if (destilando() || servido) {
            return false;
        }
        Optional<ProcessosBebida.Alambique> r = ProcessosBebida.alambiqueQueAceita(
                stack.getItem(), stack.getCount());
        if (r.isEmpty()) {
            return false;
        }
        ProcessosBebida.Alambique receita = r.get();
        this.insumo = stack.copy();
        this.resultado = new ItemStack(receita.output(), receita.qtdOut());
        this.ticksTotal = ProcessosBebida.tempoAlambique();
        this.ticksRestantes = ticksTotal;
        this.servido = false;
        setChanged();
        sincronizar(); // v1.2.55: o client acorda o renderer (calor/serpentina)
        avisar(player, Component.translatable(
                "block.intoxicantes.alambique_carregado",
                ticksTotal / 20 / 60, (ticksTotal / 20) % 60), true);
        return true;
    }

    public void interagir(ServerLevel level, Player player) {
        if (servido) {
            ItemStack saida = resultado.copy();
            resultado = ItemStack.EMPTY;
            servido = false;
            insumo = ItemStack.EMPTY;
            setChanged();
            sincronizar(); // v1.2.55: alambique vazio de novo
            entregar(level, player, saida);
            level.playSound(null, worldPosition, SoundEvents.BREWING_STAND_BREW,
                    net.minecraft.sounds.SoundSource.BLOCKS, 0.8F, 1.2F);
            return;
        }
        if (destilando()) {
            int seg = ticksRestantes / 20;
            boolean calor = temCalor(level);
            avisar(player, Component.translatable(
                    calor ? "block.intoxicantes.alambique_destilando"
                          : "block.intoxicantes.alambique_sem_fogo",
                    seg / 60, seg % 60,
                    Math.max(0, Math.min(100, 100 - ticksRestantes * 100 / Math.max(1, ticksTotal)))),
                    true);
        } else {
            avisar(player, Component.translatable(
                    "block.intoxicantes.alambique_vazio"), true);
        }
    }

    // ==================================================== MOTOR (server, 1x/s)

    public void tick(ServerLevel level) {
        if (!destilando()) {
            return;
        }
        if (level.getGameTime() % INTERVALO != 0L) {
            return;
        }
        // REGRA DO CALOR: fogo/campfire/magma embaixo. Sem calor o lote espera.
        if (!temCalor(level)) {
            return;
        }
        ticksRestantes -= INTERVALO;
        if (ticksRestantes <= 0) {
            ticksRestantes = 0;
            servido = true;
            setChanged();
            sincronizar(); // v1.2.55: fim da destilação
            level.playSound(null, worldPosition, SoundEvents.POINTED_DRIPSTONE_DRIP_WATER,
                    net.minecraft.sounds.SoundSource.BLOCKS, 0.9F, 0.7F);
        } else {
            setChanged();
            // v1.2.55: o VAPOR do destilado (1 puff por segundo, server-side)
            if (level.getRandom().nextInt(2) == 0) {
                level.sendParticles(net.minecraft.core.particles.ParticleTypes.CAMPFIRE_COSY_SMOKE,
                        worldPosition.getX() + 0.85, worldPosition.getY() + 0.78,
                        worldPosition.getZ() + 0.73, 1, 0.03, 0.02, 0.03, 0.002);
            }
        }
    }

    /** Fogo embaixo: campfire, fogo ou magma (o queimador da destilaria). */
    private boolean temCalor(net.minecraft.world.level.Level level) {
        BlockState abaixo = level.getBlockState(worldPosition.below());
        return abaixo.is(net.minecraft.world.level.block.Blocks.CAMPFIRE)
                || abaixo.is(net.minecraft.world.level.block.Blocks.FIRE)
                || abaixo.is(net.minecraft.world.level.block.Blocks.MAGMA_BLOCK);
    }

    /** Overlay (actionbar) pra ServerPlayer; chat pro resto. */
    private void avisar(Player player, Component msg, boolean overlay) {
        if (player instanceof net.minecraft.server.level.ServerPlayer sp) {
            sp.sendSystemMessage(msg, overlay);
        } else {
            player.sendSystemMessage(msg);
        }
    }

    public boolean destilando() {
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

    /** Progresso da destilação 0..1 (o client usa pro brilho da serpentina). */
    public float progressoClient() {
        if (ticksTotal <= 0) return 0F;
        return Math.max(0F, Math.min(1F, 1F - ticksRestantes / (float) ticksTotal));
    }

    /** O calor liga o brilho da serpentina no client (leitura do próprio nível). */
    public boolean comCalorClient() {
        return level != null && temCalor(level);
    }

    private void entregar(ServerLevel level, Player player, ItemStack stack) {
        if (player instanceof net.minecraft.server.level.ServerPlayer sp
                && sp.getInventory().add(stack)) {
            return;
        }
        level.addFreshEntity(new ItemEntity(level,
                worldPosition.getX() + 0.5, worldPosition.getY() + 0.8,
                worldPosition.getZ() + 0.5, stack));
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
