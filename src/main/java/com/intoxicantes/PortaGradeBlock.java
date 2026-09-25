package com.intoxicantes;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.DoubleBlockHalf;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

/**
 * A PORTA-GRADE DO ESQUINÃO (v1.2.51) — a porta de guichê de bodega, codada
 * do zero: moldura de spruce com METADE DE BAIXO sólida e metade de cima
 * recortada em GRADE de ferro.
 *
 * A regra do horário (a porta TEM DONO):
 *  - 07:00 ~ 00:00 (mercado aberto): ABERTA de verdade — passagem livre,
 *    o freguês entra e compra no balcão;
 *  - 00:00 ~ 07:00 (madrugada): o guichê FECHA — embaixo vira parede
 *    (colisão plena, ninguém entra), em cima fica a grade de ferro
 *    TRANSPARENTE com colisão (dá pra ver o Gago atender por trás dela,
 *    do jeitinho que o dono da esquina segura a porta de madrugada kkkk).
 *
 * A VIRADA é automática: o Zelador do mercado (MarketSystem, 1×/30s) lê o
 * relógio e sincroniza as duas metades — e o player também pode clicar
 * (toque de ferro; o Zelador acerta na passada seguinte, como o poste).
 *
 * Anatomia: blockstate DOUBLE-BLOCK como a porta vanilla — {@link #HALF}
 * LOWER/UPPER, a metade de cima não tem colisão de bloco próprio (a grade
 * mora no LOWER). Quebrar qualquer metade derruba a dupla inteira (item só
 * na metade de baixo). FACING na convenção da FORNALHA: aponta pra onde a
 * porta ENCARA (pra rua = facing oposto ao interior).
 */
public class PortaGradeBlock extends Block {

    /** Pra onde a porta encara (a rua). Mesma convenção da fornalha. */
    public static final EnumProperty<Direction> FACING = BlockStateProperties.HORIZONTAL_FACING;
    /** Qual metade da porta dupla (lower = com grade e colisão; upper = coroa). */
    public static final EnumProperty<DoubleBlockHalf> HALF = BlockStateProperties.DOUBLE_BLOCK_HALF;
    /** v1.2.51: o GUICHÊ fechou (madrugada)? true = grade/porta na posição fechada. */
    public static final BooleanProperty FECHADA = BooleanProperty.create("fechada");

    /** Corpo da porta: moldura de spruce com o vão no eixo da fachada. */
    private static final VoxelShape SHAPE = Block.box(1.0, 0.0, 0.0, 15.0, 16.0, 4.0);

    public PortaGradeBlock(Properties properties) {
        super(properties);
        registerDefaultState(stateDefinition.any()
                .setValue(FACING, Direction.SOUTH)
                .setValue(HALF, DoubleBlockHalf.LOWER)
                .setValue(FECHADA, Boolean.FALSE));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING, HALF, FECHADA);
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos,
            CollisionContext context) {
        return SHAPE;
    }

    @Override
    protected VoxelShape getCollisionShape(BlockState state, BlockGetter level, BlockPos pos,
            CollisionContext context) {
        // aberta: só a moldura de lado (passagem livre); fechada: BLOCO cheio
        // no LOWER (ninguém entra de madrugada). A UPPER nunca colide (a grade
        // da madrugada é "olho mágico" — se bloqueasse, nem ver o Gago dava).
        if (state.getValue(HALF) == DoubleBlockHalf.UPPER) {
            return state.getValue(FECHADA) ? SHAPE : getShape(state, level, pos, context);
        }
        return state.getValue(FECHADA) ? FULL_SHAPE : getShape(state, level, pos, context);
    }

    private static final VoxelShape FULL_SHAPE = Block.box(0.0, 0.0, 0.0, 16.0, 16.0, 16.0);

    /** Nascimento: encara quem colocou (frente pra rua), nasce no horário do dia. */
    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        BlockPos pos = context.getClickedPos();
        Level level = context.getLevel();
        return defaultBlockState()
                .setValue(FACING, context.getHorizontalDirection().getOpposite())
                .setValue(HALF, DoubleBlockHalf.LOWER)
                .setValue(FECHADA, !level.isClientSide() && estaNaMadrugada(level));
    }

    /** A dupla: colocou a de baixo, sobe a de cima (igual porta vanilla). */
    @Override
    public void setPlacedBy(Level level, BlockPos pos, BlockState state,
            net.minecraft.world.entity.LivingEntity colocador, ItemStack stack) {
        super.setPlacedBy(level, pos, state, colocador, stack);
        if (!level.isClientSide() && state.getValue(HALF) == DoubleBlockHalf.LOWER) {
            level.setBlock(pos.above(), state.setValue(HALF, DoubleBlockHalf.UPPER), 3);
        }
    }

    @Override
    protected BlockState updateShape(BlockState state, LevelReader level,
            net.minecraft.world.level.ScheduledTickAccess ticks, BlockPos pos,
            Direction direction, BlockPos posVizinho,
            BlockState estadoVizinho, net.minecraft.util.RandomSource random) {
        DoubleBlockHalf half = state.getValue(HALF);
        if (direction.getAxis() == Direction.Axis.Y) {
            if (half == DoubleBlockHalf.LOWER == (direction == Direction.UP)) {
                // a irmã sumiu (quebrou/pistão)? a porta caía como item do vanilla
                return estadoVizinho.is(this) && estadoVizinho.getValue(HALF) != half
                        ? state : net.minecraft.world.level.block.Blocks.AIR.defaultBlockState();
            }
            if (half == DoubleBlockHalf.LOWER && direction == Direction.DOWN
                    && !state.canSurvive(level, pos)) {
                return net.minecraft.world.level.block.Blocks.AIR.defaultBlockState();
            }
        }
        return super.updateShape(state, level, ticks, pos, direction, posVizinho,
                estadoVizinho, random);
    }

    @Override
    protected boolean canSurvive(BlockState state, LevelReader level, BlockPos pos) {
        if (state.getValue(HALF) == DoubleBlockHalf.LOWER) {
            return level.getBlockState(pos.below()).isFaceSturdy(level, pos.below(), Direction.UP);
        }
        return level.getBlockState(pos.below()).is(this);
    }

    /**
     * Aquele toque: clicou na porta — de madrugada o guichê balança "trancado"
     * (a chave é do dono: o Zelador que abre às 07:00), de dia ela abre com o
     * toque de ferro. Server decide, client escuta o som pela interação.
     */
    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos,
            Player player, BlockHitResult hit) {
        if (!level.isClientSide() && player instanceof ServerPlayer sp) {
            BlockPos raiz = posDaRaiz(level, pos, state);
            boolean madrugada = estaNaMadrugada(level);
            level.playSound(null, pos,
                    madrugada ? SoundEvents.IRON_DOOR_CLOSE : SoundEvents.IRON_DOOR_OPEN,
                    SoundSource.BLOCKS, 0.7F, madrugada ? 0.8F : 1.1F);
            if (madrugada) {
                sp.sendSystemMessage(net.minecraft.network.chat.Component.translatable(
                        "block.intoxicantes.porta_grade.madrugada"));
            } else {
                sp.sendSystemMessage(net.minecraft.network.chat.Component.translatable(
                        "block.intoxicantes.porta_grade.dia"));
            }
            // de dia o Zelador também abre: reforço imediato de plantão
            if (!madrugada && raiz != null) {
                sincronizar(raiz, level.getBlockState(raiz).getValue(FACING), false, (ServerLevel) level);
            }
        }
        return InteractionResult.SUCCESS_SERVER;
    }

    /** A regra do relógio (a MESMA conta do plantão do Gago): 00:00 ~ 07:00. */
    public static boolean estaNaMadrugada(Level level) {
        if (!(level instanceof ServerLevel server)) return false;
        return MarketSystem.estaNaPorta(server); // true = 00:00 ~ 07:00
    }

    /** A metade de baixo da dupla (o dono do estado). */
    public static BlockPos posDaRaiz(Level level, BlockPos pos, BlockState state) {
        BlockPos p = state.getValue(HALF) == DoubleBlockHalf.UPPER ? pos.below() : pos;
        return level.getBlockState(p).getBlock() instanceof PortaGradeBlock ? p : null;
    }

    /**
     * A VIRADA DO GUICHÊ (o Zelador chama a cada passada): fecha às 00:00,
     * abre às 07:00 — as DUAS metades num setBlock só cada uma, sem flicker.
     */
    public static void sincronizar(BlockPos raiz, Direction facing, boolean fechada,
            ServerLevel level) {
        BlockState base = level.getBlockState(raiz);
        if (!(base.getBlock() instanceof PortaGradeBlock)) return;
        base = base.setValue(FACING, facing);
        BlockPos cima = raiz.above();
        BlockState topo = level.getBlockState(cima);
        boolean mudaBaixo = base.getValue(FECHADA) != fechada;
        // v1.2.53: se o bloco de cima NÃO é porta, NÃO planta meia-porta por
        // cima (o bug que transformava paredes vizinhas em porta solta) — só
        // sincroniza a irmã que existe de verdade.
        boolean mudaTopo = topo.getBlock() instanceof PortaGradeBlock
                && topo.getValue(FECHADA) != fechada;
        if (mudaBaixo) {
            level.setBlock(raiz, base.setValue(FECHADA, fechada), 3);
        }
        if (mudaTopo) {
            level.setBlock(cima, topo.setValue(FECHADA, fechada).setValue(FACING, facing), 3);
        }
        if ((mudaBaixo || mudaTopo) && !level.isClientSide()) {
            // o "tum" do guichê na virada (a loja põe a tranca de ferro kkkk)
            level.playSound(null, raiz,
                    fechada ? SoundEvents.IRON_DOOR_CLOSE : SoundEvents.IRON_DOOR_OPEN,
                    SoundSource.BLOCKS, 0.55F, fechada ? 0.7F : 1.2F);
        }
    }

    @Override
    protected BlockState rotate(BlockState state, Rotation rot) {
        return state.setValue(FACING, rot.rotate(state.getValue(FACING)));
    }

    @Override
    protected BlockState mirror(BlockState state, Mirror mirror) {
        return state.rotate(mirror.getRotation(state.getValue(FACING)));
    }
}
