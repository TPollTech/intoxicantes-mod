package com.intoxicantes;

import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
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
 * MÁQUINA DE PRIMA (v1.2.50) — a base reutilizável do processamento de
 * matéria-prima: UMA classe, DOIS blocos com identidade própria:
 *
 * <ul>
 *   <li><b>MOENDA DE CANA</b> — os rolos esmagam a cana: 4 canas → 4 caldo
 *       + 1 bagaço (queima na fornalha);</li>
 *   <li><b>PRENSA DE UVAS</b> — o fuso esmaga e coa: 6 uvas → 4 mosto de uva.</li>
 * </ul>
 *
 * Uma futura máquina de prima (torrador de café, etc.) é só registrar outro
 * bloco desta classe com a receita no {@link ProcessosBebida}.
 */
public class MaquinaPrimaBlock extends BaseEntityBlock {

    /** Identidade da máquina (qual livro-de-receitas consultar). */
    public enum Tipo {
        MOENDA("moenda_cana"),
        PRENSA("prensa_uvas"),
        CALDEIRAO("caldeirao_mostura");

        public final String id;

        Tipo(String id) {
            this.id = id;
        }
    }

    private final Tipo tipo;

    public MaquinaPrimaBlock(Properties properties, Tipo tipo) {
        super(properties);
        this.tipo = tipo;
    }

    public Tipo tipo() {
        return tipo;
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos,
            CollisionContext context) {
        return Block.box(1.0, 0.0, 1.0, 15.0, 13.0, 15.0);
    }

    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new MaquinaPrimaBlockEntity(pos, state, tipo);
    }

    @Override
    protected RenderShape getRenderShape(BlockState state) {
        return RenderShape.MODEL;
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos,
            Player player, BlockHitResult hit) {
        if (!(level.getBlockEntity(pos) instanceof MaquinaPrimaBlockEntity be)) {
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
        if (!(level.getBlockEntity(pos) instanceof MaquinaPrimaBlockEntity be)) {
            return InteractionResult.PASS;
        }
        if (level.isClientSide()) {
            return InteractionResult.SUCCESS_SERVER;
        }
        boolean aceitou = be.tentarCarregar((ServerLevel) level, player, stack);
        if (aceitou) {
            stack.shrink(be.qtdNecessaria(stack));
            level.playSound(null, pos, SoundEvents.WOOD_BREAK,
                    SoundSource.BLOCKS, 0.7F, 0.9F);
            return InteractionResult.SUCCESS_SERVER;
        }
        be.interagir((ServerLevel) level, player);
        return InteractionResult.SUCCESS_SERVER;
    }

    /** Poeira do processamento (client, só com lote em curso). */
    @Override
    public void animateTick(BlockState state, Level level, BlockPos pos,
            net.minecraft.util.RandomSource random) {
        if (!(level.getBlockEntity(pos) instanceof MaquinaPrimaBlockEntity be)
                || !be.processando()) {
            return;
        }
        if (random.nextInt(15) != 0) {
            return;
        }
        level.addParticle(ParticleTypes.POOF,
                pos.getX() + 0.4 + random.nextDouble() * 0.2,
                pos.getY() + 0.9,
                pos.getZ() + 0.4 + random.nextDouble() * 0.2,
                0, 0.01, 0);
    }

    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level,
            BlockState state, BlockEntityType<T> type) {
        if (level.isClientSide()) {
            return null;
        }
        return (nivel, pos, estado, be) -> {
            if (be instanceof MaquinaPrimaBlockEntity maquina) {
                maquina.tick((ServerLevel) nivel);
            }
        };
    }
}
