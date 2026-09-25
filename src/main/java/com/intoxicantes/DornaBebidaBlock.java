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
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

/**
 * A DORNA DE FERMENTAÇÃO (v1.2.50) — o "sistema geral de fermentação" da
 * spec: uma tina de madeira onde o mosto in natura borbulha até virar mosto
 * fermentado. Reutilizada por TODA cadeia que ferementa antes de destilar
 * (cachaça, rum e futuras bebidas).
 *
 * Fluxo: clique-direito com a QUANTIDADE certa de insumo → a dorna carrega;
 * o timer corre (server-side, no BE); quando zera, o resultado fica "servido"
 * — clique-direito de mão vazia recolhe. Clique com outro insumo antes do fim
 * NÃO sobrescreve (o mosto em curso é respeitado).
 */
public class DornaBebidaBlock extends Block implements EntityBlock {

    /** Corpo baixo da tina (meio bloco — dá pra ver o líquido por cima). */
    private static final VoxelShape SHAPE = Block.box(1.0, 0.0, 1.0, 15.0, 10.0, 15.0);

    public DornaBebidaBlock(Properties properties) {
        super(properties);
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos,
            CollisionContext context) {
        return SHAPE;
    }

    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new DornaBebidaBlockEntity(pos, state);
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos,
            Player player, BlockHitResult hit) {
        if (!(level.getBlockEntity(pos) instanceof DornaBebidaBlockEntity be)) {
            return InteractionResult.PASS;
        }
        if (!level.isClientSide()) {
            // mão vazia: recolhe o lote pronto (ou mostra o status)
            be.interagir((ServerLevel) level, player);
        }
        return InteractionResult.SUCCESS_SERVER;
    }

    @Override
    protected InteractionResult useItemOn(ItemStack stack, BlockState state, Level level,
            BlockPos pos, Player player, InteractionHand hand, BlockHitResult hit) {
        if (!(level.getBlockEntity(pos) instanceof DornaBebidaBlockEntity be)) {
            return InteractionResult.PASS;
        }
        if (level.isClientSide()) {
            return InteractionResult.SUCCESS_SERVER;
        }
        // com item na mão: tenta carregar (valida receita/quantidade no servidor)
        boolean aceitou = be.tentarCarregar((ServerLevel) level, player, stack);
        if (aceitou) {
            stack.shrink(be.qtdNecessaria(stack));
            level.playSound(null, pos, SoundEvents.BREWING_STAND_BREW,
                    SoundSource.BLOCKS, 0.7F, 0.9F);
            return InteractionResult.SUCCESS_SERVER;
        }
        // não aceitou: mostra o status (o jogador entende o porquê)
        be.interagir((ServerLevel) level, player);
        return InteractionResult.SUCCESS_SERVER;
    }

    /** Bolhas de fermentação (client, baratas: 1 a cada 30 ticks quando ativa). */
    @Override
    public void animateTick(BlockState state, Level level, BlockPos pos,
            net.minecraft.util.RandomSource random) {
        if (!(level.getBlockEntity(pos) instanceof DornaBebidaBlockEntity be) || !be.fermentando()) {
            return;
        }
        if (random.nextInt(30) != 0) {
            return;
        }
        double x = pos.getX() + 0.25 + random.nextDouble() * 0.5;
        double z = pos.getZ() + 0.25 + random.nextDouble() * 0.5;
        level.addParticle(ParticleTypes.BUBBLE_POP, x, pos.getY() + 0.75, z, 0, 0.02, 0);
    }

    /** Ticker do servidor: o relógio da fermentação (barato, 1x/s). */
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level,
            BlockState state, BlockEntityType<T> type) {
        if (level.isClientSide()) {
            return null;
        }
        return (nivel, pos, estado, be) -> {
            if (be instanceof DornaBebidaBlockEntity dorna) {
                dorna.tick((ServerLevel) nivel);
            }
        };
    }
}
