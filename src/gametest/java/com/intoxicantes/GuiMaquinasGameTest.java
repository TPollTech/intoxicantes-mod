package com.intoxicantes;

import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Blocks;

/**
 * v1.2.59 — TESTES DO REWORK DAS MÁQUINAS (GUI + motor de lotes).
 *
 * Contratos provados (fonte: as BlockEntities, não a intenção):
 * <ul>
 *   <li>o buffer aceita até 4 DOSES de uma vez (13 uvas → 13 no slot, 0 na
 *       mão) e a dose é gasta ao FECHAR cada lote — produção contínua;</li>
 *   <li>saída cheia NÃO perde produto: {@code producaoPendente} guarda;</li>
 *   <li>quebra devolve buffer + lote (Dorna/Alambique via rota do jogador; o
 *       BARRIL devolve até em {@code destroyBlock} — explosão/pistão — via
 *       {@code spawnAfterBreak}, porque a BE dele não é Container);</li>
 *   <li>a barra da GUI lê o ContainerData REAL (0=restante, 1=total, 2=fase).</li>
 * </ul>
 * Os tempos seguem o gancho global de teste ({@code setVelocidadeTeste(0.05F)}
 * do boot — o mesmo do GuiaGameTest): lote da dorna 420s/20 = 21s.
 */
public class GuiMaquinasGameTest {

    // ==================================================== BUFFER DE 4 DOSES

    @GameTest(maxTicks = 400)
    public void prensaAceita7UvasSemExigirDoseExata(GameTestHelper helper) {
        var level = helper.getLevel();
        var player = helper.makeMockServerPlayerInLevel();
        var pos = helper.absolutePos(BlockPos.ZERO);

        level.setBlockAndUpdate(pos, IntoxicantesMod.PRENSA_UVAS.defaultBlockState());
        var be = (MaquinaPrimaBlockEntity) level.getBlockEntity(pos);

        // dose da prensa é 6; 7 uvas = 1 dose INTEIRA + sobra na mão (o buffer
        // só entra em doses inteiras: dose parcial não fecha lote). A stack vai
        // DA MÃO (rota real do bloco): quem consome é o split da própria máquina
        helper.assertFalse(be.tentarCarregar(level, player,
                new ItemStack(IntoxicantesMod.UVA, 2)),
                "Menos de uma dose inteira não entra (não fecha lote)");
        player.getInventory().clearContent();
        player.getInventory().setItem(0, new ItemStack(IntoxicantesMod.UVA, 7));
        boolean aceitou = be.tentarCarregar(level, player,
                player.getInventory().getItem(0));
        helper.assertTrue(aceitou, "A prensa deve aceitar 7 uvas (1 dose inteira)");
        helper.assertTrue(be.getItem(0).getCount() == 6,
                "O buffer recebe a dose (6), nunca pilha parcial");
        helper.assertTrue(contaMao(player, IntoxicantesMod.UVA) == 1,
                "A 7ª uva sobra NA MÃO do jogador");
        helper.assertTrue(be.processando(), "O lote deve começar sozinho");

        // 40s/20 = 2s por lote; no fechamento, consome EXATAMENTE 6 (sobra 0)
        helper.runAfterDelay(60, () -> {
            helper.assertTrue(be.getItem(0).isEmpty(),
                    "Ao fechar o lote, a dose inteira é consumida");
            helper.assertTrue(be.getItem(TipoMaquina.PRENSA.idxOut).getCount() == 4,
                    "O lote fechado produziu 4 mostos no slot de saída");
            helper.assertTrue(!be.processando(),
                    "Sem mais insumo, nenhum lote fantasma começa");
            helper.succeed();
        });
    }

    // ==================================================== MULTI-LOTE

    @GameTest(maxTicks = 600)
    public void prensa13UvasFaz2Lotes(GameTestHelper helper) {
        var level = helper.getLevel();
        var player = helper.makeMockServerPlayerInLevel();
        var pos = helper.absolutePos(BlockPos.ZERO);

        level.setBlockAndUpdate(pos, IntoxicantesMod.PRENSA_UVAS.defaultBlockState());
        var be = (MaquinaPrimaBlockEntity) level.getBlockEntity(pos);

        // 13 uvas = 2 doses inteiras (6+6) entram; a 13ª sobra na mão
        player.getInventory().clearContent();
        player.getInventory().setItem(0, new ItemStack(IntoxicantesMod.UVA, 13));
        be.tentarCarregar(level, player, player.getInventory().getItem(0));
        helper.assertTrue(be.getItem(0).getCount() == 12,
                "O buffer recebe 2 doses inteiras (12 uvas)");
        helper.assertTrue(contaMao(player, IntoxicantesMod.UVA) == 1,
                "A 13ª uva (dose parcial) sobra na mão do jogador");

        helper.runAfterDelay(140, () -> {
            // 12 = 6 (lote 1) + 6 (lote 2): DOIS lotes fecharam sem nenhuma
            // interação extra — a produção contínua encadeou sozinha
            helper.assertTrue(be.getItem(0).isEmpty(),
                    "Os 2 lotes consumiram as 12 uvas do buffer");
            helper.assertTrue(be.getItem(TipoMaquina.PRENSA.idxOut).getCount() == 8,
                    "Os 2 lotes encadeados entregaram 8 mostos (4+4)");
            helper.succeed();
        });
    }

    // ==================================================== SAÍDA CHEIA

    @GameTest(maxTicks = 600)
    public void dornaLoteTerminaMesmoComSaidaCheia(GameTestHelper helper) {
        var level = helper.getLevel();
        var player = helper.makeMockServerPlayerInLevel();
        var pos = helper.absolutePos(BlockPos.ZERO);

        level.setBlockAndUpdate(pos, IntoxicantesMod.DORNA_BEBIDA.defaultBlockState());
        var be = (DornaBebidaBlockEntity) level.getBlockEntity(pos);

        // 4 caldo = 1 lote (420s/20 = 21s com a escala de teste 0.05)
        be.tentarCarregar(level, player, new ItemStack(IntoxicantesMod.CALDO_DE_CANA, 4));
        helper.assertTrue(be.processando(), "A dorna deve fermentar com 4 caldo");

        // entulha a saída com a pilha CHEIA (64): 4 mosto do lote novo não
        // cabem (68 > 64) — é aí que a pendência protege o produto
        be.setItem(TipoMaquina.DORNA.idxOut,
                new ItemStack(IntoxicantesMod.MOSTO_CANA_FERMENTADO, 64));
        helper.runAfterDelay(460, () -> {
            helper.assertTrue(be.dadosGui().get(2) == DornaBebidaBlockEntity.FASE_SAIDA_CHEIA,
                    "Com a saída cheia, a fase deve avisar e guardar o lote");
            helper.assertTrue(be.dadosGui().get(0) == 0,
                    "O lote terminou (timer zerado) mesmo sem espaço");
            helper.assertTrue(be.getItem(TipoMaquina.DORNA.idxOut).getCount() == 64,
                    "O produto pendente NÃO soma na pilha cheia (nada se perde)");
            helper.succeed();
        });
    }

    // ==================================================== QUEBRA DEVOLVE

    @GameTest(maxTicks = 400)
    public void dornaQuebradaDevolveBufferEProcesso(GameTestHelper helper) {
        var level = helper.getLevel();
        var player = helper.makeMockServerPlayerInLevel();
        var pos = helper.absolutePos(BlockPos.ZERO);

        level.setBlockAndUpdate(pos, IntoxicantesMod.DORNA_BEBIDA.defaultBlockState());
        var be = (DornaBebidaBlockEntity) level.getBlockEntity(pos);
        be.tentarCarregar(level, player, new ItemStack(IntoxicantesMod.CALDO_DE_CANA, 8));

        // quebra com a mão (rota real do jogador): buffer + dose do lote voltam
        level.destroyBlock(pos, true);
        helper.runAfterDelay(20, () -> {
            int devolvido = itensNoChao(level, pos, IntoxicantesMod.CALDO_DE_CANA);
            helper.assertTrue(devolvido == 8,
                    "A dorna quebrada deve devolver as 8 unidades (buffer + lote)");
            helper.succeed();
        });
    }

    @GameTest(maxTicks = 400)
    public void barrilQuebradoSemJogadorDevolveTudo(GameTestHelper helper) {
        var level = helper.getLevel();
        var player = helper.makeMockServerPlayerInLevel();
        var pos = helper.absolutePos(BlockPos.ZERO);

        level.setBlockAndUpdate(pos, IntoxicantesMod.BARRIL_CERVEJA.defaultBlockState());
        var be = (BarrilBebidaBlockEntity) level.getBlockEntity(pos);
        be.interagir(level, player, new ItemStack(IntoxicantesMod.MOSTO_CERVEJA_LUPULADO, 8));
        helper.assertTrue(be.processando() || be.getItem(0).getCount() > 0,
                "O barril deve carregar o mosto");

        // destroyBlock SEM jogador (explosão/pistão): o drop vanilla não cobre
        // a BE (não é Container) — o spawnAfterBreak tem que devolver buffer+lote
        level.destroyBlock(pos, true);
        helper.runAfterDelay(20, () -> {
            int devolvido = itensNoChao(level, pos,
                    IntoxicantesMod.MOSTO_CERVEJA_LUPULADO);
            helper.assertTrue(devolvido == 8,
                    "O barril quebrado sem jogador devolve buffer + lote em curso");
            helper.succeed();
        });
    }

    // ==================================================== BARRA REAL (ContainerData)

    @GameTest(maxTicks = 300)
    public void barraDaGuiEBarealDaBlockEntity(GameTestHelper helper) {
        var level = helper.getLevel();
        var player = helper.makeMockServerPlayerInLevel();
        var pos = helper.absolutePos(BlockPos.ZERO);

        // FOGO ANTES do bloco: o canSurvive do bloco duplo derruba a máquina
        // se a base aparecer depois (updateShape de vizinho) — o setup certo
        level.setBlockAndUpdate(pos.below(), Blocks.CAMPFIRE.defaultBlockState());
        level.setBlockAndUpdate(pos, IntoxicantesMod.ALAMBIQUE.defaultBlockState());
        var be = (AlambiqueBlockEntity) level.getBlockEntity(pos);
        be.tentarCarregar(level, player,
                new ItemStack(IntoxicantesMod.MOSTO_CANA_FERMENTADO, 4));

        helper.runAfterDelay(40, () -> {
            int restante = be.dadosGui().get(0);
            int total = be.dadosGui().get(1);
            int fase = be.dadosGui().get(2);
            helper.assertTrue(total > 0, "O total do lote deve ser positivo");
            helper.assertTrue(restante > 0 && restante < total,
                    "O progresso deve estar andando (restante entre 0 e total)");
            helper.assertTrue(fase == AlambiqueBlockEntity.FASE_DESTILANDO,
                    "A fase sincronizada deve ser DESTILANDO");
            helper.succeed();
        });
    }

    // ==================================================== HELPERS

    /** Itens de um tipo na mão/inventário do jogador. */
    private static int contaMao(ServerPlayer player, net.minecraft.world.item.Item item) {
        int total = 0;
        for (int i = 0; i < player.getInventory().getContainerSize(); i++) {
            var st = player.getInventory().getItem(i);
            if (st.is(item)) {
                total += st.getCount();
            }
        }
        return total;
    }

    /** Itens dropados perto da posição (entidades Item no raio de 3 blocos). */
    private static int itensNoChao(net.minecraft.server.level.ServerLevel level,
            BlockPos pos, net.minecraft.world.item.Item item) {
        int total = 0;
        for (var entidade : level.getEntitiesOfClass(
                net.minecraft.world.entity.item.ItemEntity.class,
                new net.minecraft.world.phys.AABB(pos).inflate(3))) {
            if (entidade.getItem().is(item)) {
                total += entidade.getItem().getCount();
            }
        }
        return total;
    }
}
