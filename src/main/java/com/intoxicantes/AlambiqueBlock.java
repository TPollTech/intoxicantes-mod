package com.intoxicantes;

import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

import java.util.Optional;

/**
 * O ALAMBIQUE DE COBRE (v1.2.50) — a destilação de verdade: caldeira de
 * cobre + serpentina. Regra da spec: cachaça e rum são DESTILADAS, e as
 * duas passam por ESTE bloco (a receita determina a saída — nunca um
 * "alambique de rum" duplicado).
 *
 * Como funciona: coloca FOGO/campfire embaixo (a chama aquece), carrega
 * com o mosto fermentado (clique-direito), o cobre trabalha (vapor +
 * borbulha) e, quando termina, o destilado jovem fica pronto na bica —
 * mão vazia recolhe. Sem fogo embaixo, o lote simplesmente pausa (a
 * destilação precisa de calor constante).
 */
public class AlambiqueBlock extends BaseEntityBlock {

    /** Silhueta: caldeira gorda + pescoço (o modelo 3D completa o resto). */
    private static final VoxelShape SHAPE = Block.box(2.0, 0.0, 2.0, 14.0, 14.0, 14.0);

    public AlambiqueBlock(Properties properties) {
        super(properties);
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos,
            CollisionContext context) {
        return SHAPE;
    }

    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new AlambiqueBlockEntity(pos, state);
    }

    @Override
    protected RenderShape getRenderShape(BlockState state) {
        return RenderShape.MODEL;
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos,
            Player player, BlockHitResult hit) {
        if (!(level.getBlockEntity(pos) instanceof AlambiqueBlockEntity be)) {
            return InteractionResult.PASS;
        }
        if (!level.isClientSide()) {
            be.interagir((ServerLevel) level, player);
        }
        return InteractionResult.SUCCESS_SERVER;
    }

    @Override
    protected InteractionResult useItemOn(ItemStack stack, BlockState state, Level level,
            BlockPos pos, Player player, InteractionHand hand, BlockHitResult hit) {
        if (!(level.getBlockEntity(pos) instanceof AlambiqueBlockEntity be)) {
            return InteractionResult.PASS;
        }
        if (level.isClientSide()) {
            return InteractionResult.SUCCESS_SERVER;
        }
        boolean aceitou = be.tentarCarregar((ServerLevel) level, player, stack);
        if (aceitou) {
            stack.shrink(be.qtdNecessaria(stack));
            level.playSound(null, pos, SoundEvents.BLAZE_SHOOT,
                    SoundSource.BLOCKS, 0.7F, 1.1F);
            return InteractionResult.SUCCESS_SERVER;
        }
        be.interagir((ServerLevel) level, player);
        return InteractionResult.SUCCESS_SERVER;
    }

    /** Vapor e brilho do cobre quente (client, só quando destilando). */
    @Override
    public void animateTick(BlockState state, Level level, BlockPos pos,
            net.minecraft.util.RandomSource random) {
        if (!(level.getBlockEntity(pos) instanceof AlambiqueBlockEntity be) || !be.destilando()) {
            return;
        }
        if (random.nextInt(20) != 0) {
            return;
        }
        // vapor sai da válvula do topo da caldeira
        double x = pos.getX() + 0.3 + random.nextDouble() * 0.4;
        double z = pos.getZ() + 0.3 + random.nextDouble() * 0.4;
        level.addParticle(ParticleTypes.CAMPFIRE_COSY_SMOKE, x, pos.getY() + 1.0, z,
                0, 0.015, 0);
        if (random.nextInt(4) == 0) {
            level.addParticle(ParticleTypes.SMOKE, x, pos.getY() + 1.05, z, 0, 0.01, 0);
        }
    }

    /** Ticker do servidor: o relógio da destilação (pausa sem fogo). */
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level,
            BlockState state, BlockEntityType<T> type) {
        if (level.isClientSide()) {
            return null;
        }
        return (nivel, pos, estado, be) -> {
            if (be instanceof AlambiqueBlockEntity alambique) {
                alambique.tick((ServerLevel) nivel);
            }
        };
    }
}
