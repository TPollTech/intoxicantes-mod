package com.intoxicantes;

import java.util.List;
import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.level.block.state.properties.DoubleBlockHalf;
import net.minecraft.world.phys.AABB;

/**
 * O OVERHAUL DO MERCADO (v1.2.51): a faixa de LED completa, o guichê que
 * segue o relógio e a âncora dupla do Gago (balcão de dia, guichê de
 * madrugada — NUNCA mais solto).
 */
public class MarketOverhaulGameTest {

    /** O raio de gestão do gerenciador (código): raio 48². */
    private static final double RAIO_GESTAO = 48L * 48L;

    @GameTest
    public void faixaDeLedFicaCompletaAposZelador(GameTestHelper helper) {
        BlockPos painel = helper.absolutePos(new BlockPos(2, 1, 2));
        ServerLevel level = helper.getLevel();
        // placa do TEMPLATE (vínculo com o mercado): o zelador só age nela
        level.setBlockAndUpdate(painel, IntoxicantesMod.PLACA_ESQUINAO.defaultBlockState()
                .setValue(PlacaEsquinaoBlock.FACING, Direction.SOUTH)
                .setValue(PlacaEsquinaoBlock.PARTE, PlacaEsquinaoBlock.Parte.PAINEL)
                .setValue(PlacaEsquinaoBlock.NIVEL, PlacaEsquinaoBlock.Nivel.COLUNA));
        if (!(level.getBlockEntity(painel) instanceof PlacaEsquinaoBlockEntity be)) {
            helper.fail("painel sem block entity");
            return;
        }
        // A CAIXA DE GAMETEST tem ~8 blocos: a varredura ±7 passa da borda.
        // O que importa testar: 1) ELA SEMEIA (não fica no painel solto);
        // 2) PARAR no primeiro obstáculo — pedra ("parede do jogador") NUNCA
        // é sobrescrita e a fileira não pula por cima dela.
        Direction eixo = Direction.SOUTH.getClockWise();
        // A pedra JÁ EXISTE ANTES do zelador (é a parede do jogador): o cenário
        // real é "mundo velho com obstáculo", não "pedra trocada no meio da
        // faixa viva" — trocar DEPOIS dispararia a cascata v1.2.31 (a faixa
        // inteira cai quando um pedaço é removido), que não é este teste.
        BlockPos pedra = painel.relative(eixo, -3);
        level.setBlockAndUpdate(pedra, net.minecraft.world.level.block.Blocks.STONE.defaultBlockState());

        // mundo velho 1.2.31–50: largura gravada 15 mas SÓ o painel semeado
        be.definirConteudo(List.of("MERCADO ESQUINÃO"), true, true, 15);
        PlacaEsquinaoBlockEntity.completarFaixa(level, painel, be);

        int montados = 0;
        for (int d = -7; d <= 7; d++) {
            if (level.getBlockState(painel.relative(eixo, d))
                    .getBlock() == IntoxicantesMod.PLACA_ESQUINAO) {
                montados++;
            }
        }
        // A sala de testes não cabe a faixa de 15: o que importa é SEMEAR dos
        // dois lados (montados > painel) e respeitar o obstáculo.
        helper.assertTrue(montados >= 5,
                "a faixa deve semear dos DOIS lados do painel (veio " + montados + ")");
        helper.assertTrue(level.getBlockState(pedra)
                        .getBlock() == net.minecraft.world.level.block.Blocks.STONE,
                "a pedra do jogador NUNCA é sobrescrita pela autocura");
        helper.assertTrue(level.getBlockState(painel.relative(eixo, -4)).isAir(),
                "a fileira PARA no obstáculo (não pula por cima)");
        // o texto é SÓ do painel: extensão NUNCA tem block entity
        BlockPos vizinha = painel.relative(eixo, 3);
        helper.assertTrue(level.getBlockEntity(vizinha) == null,
                "extensão não pode ter block entity (texto duplicado)");
        // e a largura do BE reflete a faixa REAL montada (não fica 15 mentiroso)
        if (level.getBlockEntity(painel) instanceof PlacaEsquinaoBlockEntity depois) {
            helper.assertTrue(depois.getLargura() == montados,
                    "largura do BE deve refletir a faixa real (BE="
                            + depois.getLargura() + " real=" + montados + ")");
        }
        helper.succeed();
    }

    @GameTest
    public void guicheFechaEBaixaColisao(GameTestHelper helper) {
        BlockPos raiz = helper.absolutePos(new BlockPos(2, 1, 2));
        ServerLevel level = helper.getLevel();
        level.setBlockAndUpdate(raiz, IntoxicantesMod.PORTA_GRADE.defaultBlockState()
                .setValue(PortaGradeBlock.FACING, Direction.SOUTH)
                .setValue(PortaGradeBlock.HALF, DoubleBlockHalf.LOWER)
                .setValue(PortaGradeBlock.FECHADA, Boolean.FALSE));
        level.setBlockAndUpdate(raiz.above(), IntoxicantesMod.PORTA_GRADE.defaultBlockState()
                .setValue(PortaGradeBlock.FACING, Direction.SOUTH)
                .setValue(PortaGradeBlock.HALF, DoubleBlockHalf.UPPER)
                .setValue(PortaGradeBlock.FECHADA, Boolean.FALSE));

        // ZELADOR fecha o guichê (madrugada): as duas metades viram juntas
        PortaGradeBlock.sincronizar(raiz, Direction.SOUTH, true, level);
        var baixo = level.getBlockState(raiz);
        var cima = level.getBlockState(raiz.above());
        helper.assertTrue(baixo.getValue(PortaGradeBlock.FECHADA),
                "o guichê deve FECHAR de madrugada");
        helper.assertTrue(cima.getBlock() instanceof PortaGradeBlock
                && cima.getValue(PortaGradeBlock.FECHADA),
                "a metade de cima (a grade) fecha junto");
        // facing=south → a passagem é no EIXO Z: fechado = bloco cheio
        // (max(Z)=1.0 em unidades normalizadas); aberto = moldura de 4px
        var colisao = baixo.getCollisionShape(level, raiz);
        helper.assertTrue(colisao.max(Direction.Axis.Z) >= 0.999,
                "guichê fechado: o vão embaixo vira parede (bloco cheio) "
                        + "[estado=" + baixo + " maxZ=" + colisao.max(Direction.Axis.Z) + "]");

        // 07:00: o Zelador abre — passagem livre
        PortaGradeBlock.sincronizar(raiz, Direction.SOUTH, false, level);
        var aberta = level.getBlockState(raiz);
        helper.assertTrue(!aberta.getValue(PortaGradeBlock.FECHADA),
                "o guichê deve ABRIR às 07:00");
        var colisaoAberta = aberta.getCollisionShape(level, raiz);
        helper.assertTrue(colisaoAberta.max(Direction.Axis.Z) <= 0.26,
                "guichê aberto: passagem livre no vão (moldura de 4px) "
                        + "[maxZ=" + colisaoAberta.max(Direction.Axis.Z) + "]");
        helper.succeed();
    }

    @GameTest
    public void gagoAncoraNoPostoDaVirada(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        BlockPos mercado = helper.absolutePos(new BlockPos(2, 1, 2));
        // o gerenciador segue o RELÓGIO do mundo (não do teste): o destino
        // é calculado pela MESMA conta dele
        boolean naPorta = MarketSystem.estaNaPorta(level);
        BlockPos destino = naPorta ? MarketSystem.getPosPorta(mercado)
                : MarketSystem.getPosBalcao(mercado);

        // dono da esquina SOLTO a 5 blocos (dentro da gestão, fora do posto)
        GagoEntity gago = IntoxicantesMod.GAGO.create(level, EntitySpawnReason.EVENT);
        if (gago == null) {
            helper.fail("não consegui criar o Gago");
            return;
        }
        BlockPos solto = destino.offset(5, 0, 5);
        gago.absSnapTo(solto.getX() + 0.5, solto.getY(), solto.getZ() + 0.5, 0F, 0F);
        gago.finalizeSpawn(level, level.getCurrentDifficultyAt(solto),
                EntitySpawnReason.EVENT, null);
        level.addFreshEntity(gago);

        MarketSystem.gerenciarGago(level, mercado);

        helper.assertTrue(gago.isAlive(), "o Gago não pode morrer na âncora");
        helper.assertTrue(gago.blockPosition().distSqr(destino) < 4.0,
                "o Gago deve ficar ANCORADO no posto da virada (balcão/guichê),"
                        + " não solto no pátio");
        helper.assertTrue(gago.isNoAi(),
                "ancorado = estatueta (NoAI): ele não passeia no meio da compra");
        helper.assertTrue(gago.estaDePlantao(),
                "o plantão deve estar marcado no posto certo");
        helper.succeed();
    }

    @GameTest
    public void gagoDePlantaoNaoEImportunado(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        BlockPos mercado = helper.absolutePos(new BlockPos(2, 1, 2));
        boolean naPorta = MarketSystem.estaNaPorta(level);
        BlockPos destino = naPorta ? MarketSystem.getPosPorta(mercado)
                : MarketSystem.getPosBalcao(mercado);

        GagoEntity gago = IntoxicantesMod.GAGO.create(level, EntitySpawnReason.EVENT);
        if (gago == null) {
            helper.fail("não consegui criar o Gago");
            return;
        }
        gago.absSnapTo(destino.getX() + 0.5, destino.getY(), destino.getZ() + 0.5, 180F, 0F);
        gago.finalizeSpawn(level, level.getCurrentDifficultyAt(destino),
                EntitySpawnReason.EVENT, null);
        level.addFreshEntity(gago);
        gago.setupPostoMercado(destino);
        double antes = gago.blockPosition().distSqr(destino);

        // caminho feliz: gerenciador NÃO teleporta quem já tá ancorado
        MarketSystem.gerenciarGago(level, mercado);
        helper.assertTrue(gago.isAlive(), "o dono da esquina segue vivo");
        helper.assertTrue(gago.blockPosition().distSqr(destino) < 4.0
                && Math.abs(gago.blockPosition().distSqr(destino) - antes) < 4.0,
                "Gago já ancorado: NENHUM snap desnecessário (fim do teleporte em loop)");
        helper.assertTrue(gago.isNoAi(), "posto mantém o NoAI");
        helper.succeed();
    }

    /** O descartador de excedentes NÃO pode descartar o dono ancorado. */
    @GameTest
    public void gerenciadorMantemUmDonoVivo(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        BlockPos mercado = helper.absolutePos(new BlockPos(2, 1, 2));
        boolean naPorta = MarketSystem.estaNaPorta(level);
        BlockPos destino = naPorta ? MarketSystem.getPosPorta(mercado)
                : MarketSystem.getPosBalcao(mercado);

        GagoEntity dono = IntoxicantesMod.GAGO.create(level, EntitySpawnReason.EVENT);
        if (dono == null) {
            helper.fail("não consegui criar o Gago");
            return;
        }
        dono.absSnapTo(destino.getX() + 0.5, destino.getY(), destino.getZ() + 0.5, 180F, 0F);
        dono.finalizeSpawn(level, level.getCurrentDifficultyAt(destino),
                EntitySpawnReason.EVENT, null);
        level.addFreshEntity(dono);
        dono.setupPostoMercado(destino);

        MarketSystem.gerenciarGago(level, mercado);
        var vivos = level.getEntitiesOfClass(GagoEntity.class,
                new AABB(mercado).inflate(96));
        helper.assertTrue(vivos.size() == 1,
                "DEVE existir exatamente UM dono da esquina (veio " + vivos.size() + ")");
        helper.assertTrue(dono.isAlive(),
                "o dono ancorado NUNCA é descartado como excedente");
        helper.succeed();
    }
}
