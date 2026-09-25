package com.intoxicantes;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.TooltipFlag;

/**
 * O LIVRO-DE-RECEITAS DAS BEBIDAS (v1.2.50) — o registro central de todos os
 * processos: fermentação de dorna, destilação de alambique, fermentação/
 * maturação de barril e engarrafamento. Tudo aqui, NADA espalhado pelas
 * máquinas: o Alambique não sabe o que é rum, ele apenas consulta este
 * registro com o item que recebeu.
 *
 * ARQUITETURA EXTENSÍVEL: uma bebida futura (uísque, vodka, saquê...) é só
 * registrar as etapas dela aqui (e os itens intermediários) — as MÁQUINAS
 * (dorna, alambique, barril) são genéricas e não mudam. O hidromel segue
 * como receita de crafting simples (não tem cadeia pedida).
 *
 * Tempos: segundos de JOGO convertidos em ticks (20/s) — configuráveis no
 * ModConfig (nada de relógio de parede do computador). O progresso mora nas
 * BlockEntities (sobrevive a save/restart/chunk descarregado) e toda a lógica
 * roda no servidor.
 */
public final class ProcessosBebida {

    private ProcessosBebida() {}

    // ==================================================== TIPOS DE RECEITA

    /**
     * FERMENTAÇÃO DE DORNA: mosto in natura → mosto fermentado.
     * (caldo de cana, mosto de rum; futuras frutas caem aqui de graça)
     */
    public record Dorna(Item input, int qtdIn, Item output, int qtdOut) {}

    /**
     * DESTILAÇÃO DE ALAMBIQUE: mosto fermentado → destilado jovem.
     * Reutilizada por cachaça, rum e futuros destilados (uísque, tequila...).
     */
    public record Alambique(Item input, int qtdIn, Item output, int qtdOut) {}

    /**
     * BARRIL: enche o barril com o insumo; ele FERMENTA (fase 1, opcional se
     * tempoFermentacao == 0) e/ou MADIURA (fase 2) e depois rende
     * {@code garrafas} garrafas da bebida final (retiradas 1 a 1 com garrafa
     * de vidro).
     *
     * Fase 1 = FERMENTANDO (mosto cru; bolhas/partículas);
     * Fase 2 = MATURANDO (destilado jovem ou pós-fermentação; silencioso).
     * Cerveja/vinho: fase 1 + fase 2 (condicionamento dentro do mesmo barril,
     * como a spec pede). Cachaça/rum: só fase 2 (o alambique já entregou o
     * jovem destilado).
     */
    public record Barril(String bebida, Item input, int qtdIn,
            int tempoFermentacaoSeg, int tempoMaturacaoSeg,
            Item bebidaFinal, int garrafas) {}

    /**
     * Uma máquina de PRIMA (moenda/prensa/caldeirão): entrada → saída direta.
     * O caldeirão usa {@code secIn}/{@code secQtd}: o SEGUNDO ingrediente da
     * mostura (o lúpulo da fervura), carregado DEPOIS do principal — duas
     * doses no mesmo caldeirão, como na cervejaria real. secIn null = máquina
     * de ingrediente único (moenda, prensa).
     */
    public record Prima(Item input, int qtdIn, Item output, int qtdOut,
            Item secIn, int secQtd, Item extraOut, int extraQtd) {}

    // ==================================================== O REGISTRO

    private static final List<Dorna> DORNAS = new ArrayList<>();
    private static final List<Alambique> ALAMBIQUES = new ArrayList<>();
    private static final List<Barril> BARRIS = new ArrayList<>();
    private static final List<Prima> MOENDAS = new ArrayList<>();
    private static final List<Prima> PRENSAS = new ArrayList<>();
    private static final List<Prima> CALDEIROES = new ArrayList<>();

    static void registrar() {
        if (!DORNAS.isEmpty()) {
            return; // já registrado (recarga de classe em dev não duplica)
        }

        // ---------------- FERMENTAÇÃO DE DORNA (cachaça e rum, o "sistema geral")
        // 4 caldo de cana → 4 mosto fermentado (a DORNA leva para a frente do alambique)
        DORNAS.add(new Dorna(IntoxicantesMod.CALDO_DE_CANA, 4,
                IntoxicantesMod.MOSTO_CANA_FERMENTADO, 4));
        // melaço (caldo reduzido na fornalha) → mosto de rum: 1 melaço = 1 lote
        DORNAS.add(new Dorna(IntoxicantesMod.MELACO, 1,
                IntoxicantesMod.MOSTO_RUM_FERMENTADO, 4));

        // ---------------- DESTILAÇÃO DE ALAMBIQUE (a MESMA máquina pros dois)
        // destilar CONCENTRA: 4 de mosto rendem 2 de destilado jovem
        ALAMBIQUES.add(new Alambique(IntoxicantesMod.MOSTO_CANA_FERMENTADO, 4,
                IntoxicantesMod.CACHACA_JOVEM, 2));
        ALAMBIQUES.add(new Alambique(IntoxicantesMod.MOSTO_RUM_FERMENTADO, 4,
                IntoxicantesMod.RUM_JOVEM, 2));

        // ---------------- BARRIS (fermentação e/ou maturação + engarrafamento)
        // cachaça: 2 jovens do alambique descansam na madeira (só fase 2)
        BARRIS.add(new Barril("cachaca", IntoxicantesMod.CACHACA_JOVEM, 2,
                0, 600, IntoxicantesMod.CACHACA, 4));
        // cerveja: 1 lote do caldeirão FERMENTA e condiciona no mesmo barril
        BARRIS.add(new Barril("cerveja", IntoxicantesMod.MOSTO_CERVEJA_LUPULADO, 4,
                480, 120, IntoxicantesMod.CERVEJA, 4));
        // rum: 2 jovens envelhecem na madeira escura tostada
        BARRIS.add(new Barril("rum", IntoxicantesMod.RUM_JOVEM, 2,
                0, 600, IntoxicantesMod.RUM, 4));
        // vinho: 1 lote da prensa FERMENTA e matura na adega
        BARRIS.add(new Barril("vinho", IntoxicantesMod.MOSTO_DE_UVA, 4,
                300, 300, IntoxicantesMod.VINHO, 4));

        // ---------------- MÁQUINAS DE PRIMA
        // MOENDA: 4 canas → 4 caldo + 1 bagaço (o bagaço queima na fornalha)
        MOENDAS.add(new Prima(IntoxicantesMod.CANA_DE_ACUCAR, 4,
                IntoxicantesMod.CALDO_DE_CANA, 4, null, 0,
                IntoxicantesMod.BAGACO_DE_CANA, 1));
        // PRENSA: 6 uvas → 4 mosto de uva (esmagadas e coadas)
        PRENSAS.add(new Prima(IntoxicantesMod.UVA, 6,
                IntoxicantesMod.MOSTO_DE_UVA, 4, null, 0, null, 0));
        // CALDEIRÃO (mostura + fervura numa estação): 4 malte + 1 lúpulo →
        // 4 mosto lupulado. Precisa de ÁGUA embaixo (a diluição da mostura).
        CALDEIROES.add(new Prima(IntoxicantesMod.MALTE, 4,
                IntoxicantesMod.MOSTO_CERVEJA_LUPULADO, 4,
                IntoxicantesMod.LOUPULO_FRESCO, 1, null, 0));
    }

    // ==================================================== CONSULTAS

    /** A fermentação de dorna que consome {@code qtd} deste item (a pilha da mão
     *  pode ser MAIOR: consome só o qtdIn — v1.2.51, fim da exigência de quantidade exata). */
    public static Optional<Dorna> dornaQueAceita(Item item, int qtd) {
        for (Dorna r : DORNAS) {
            if (r.input() == item && r.qtdIn() <= qtd) {
                return Optional.of(r);
            }
        }
        return Optional.empty();
    }

    /** Alguma receita de dorna usa este item? (tooltip da máquina) */
    public static Optional<Dorna> dornaDe(Item item) {
        for (Dorna r : DORNAS) {
            if (r.input() == item) {
                return Optional.of(r);
            }
        }
        return Optional.empty();
    }

    public static Optional<Alambique> alambiqueQueAceita(Item item, int qtd) {
        for (Alambique r : ALAMBIQUES) {
            if (r.input() == item && r.qtdIn() <= qtd) {
                return Optional.of(r);
            }
        }
        return Optional.empty();
    }

    public static Optional<Alambique> alambiqueDe(Item item) {
        for (Alambique r : ALAMBIQUES) {
            if (r.input() == item) {
                return Optional.of(r);
            }
        }
        return Optional.empty();
    }

    /** A receita de barril de uma bebida (id "cachaca", "cerveja"...). */
    public static Optional<Barril> barrilDe(String bebida) {
        for (Barril r : BARRIS) {
            if (r.bebida().equals(bebida)) {
                return Optional.of(r);
            }
        }
        return Optional.empty();
    }

    /** Qual barril aceita este insumo? (tooltip/instinto do jogador) */
    public static Optional<Barril> barrilQueAceita(Item item) {
        for (Barril r : BARRIS) {
            if (r.input() == item) {
                return Optional.of(r);
            }
        }
        return Optional.empty();
    }

    public static Optional<Prima> moendaQueAceita(Item item, int qtd) {
        for (Prima r : MOENDAS) {
            if (r.input() == item && r.qtdIn() == qtd) {
                return Optional.of(r);
            }
        }
        return Optional.empty();
    }

    public static Optional<Prima> prensaQueAceita(Item item, int qtd) {
        for (Prima r : PRENSAS) {
            if (r.input() == item && r.qtdIn() == qtd) {
                return Optional.of(r);
            }
        }
        return Optional.empty();
    }

    /** A receita da máquina de prima por TIPO (moenda/prensa/caldeirão). */
    public static Optional<Prima> primaDe(MaquinaPrimaBlock.Tipo tipo, Item item, int qtd) {
        List<Prima> lista = switch (tipo) {
            case MOENDA -> MOENDAS;
            case PRENSA -> PRENSAS;
            case CALDEIRAO -> CALDEIROES;
        };
        for (Prima r : lista) {
            // v1.2.51: aceita pilha MAIOR que a receita (consome só qtdIn);
            // antes exigia contagem EXATA (6 uvas = 6, nem 7, nem uma pilha)
            if (r.input() == item && r.qtdIn() <= qtd) {
                return Optional.of(r);
            }
        }
        return Optional.empty();
    }

    /** Alguma receita deste tipo de máquina usa o insumo como PRINCIPAL? */
    public static Optional<Prima> primaPrincipal(MaquinaPrimaBlock.Tipo tipo, Item item) {
        List<Prima> lista = switch (tipo) {
            case MOENDA -> MOENDAS;
            case PRENSA -> PRENSAS;
            case CALDEIRAO -> CALDEIROES;
        };
        for (Prima r : lista) {
            if (r.input() == item) {
                return Optional.of(r);
            }
        }
        return Optional.empty();
    }

    public static List<Barril> barris() {
        return List.copyOf(BARRIS);
    }

    // ==================================================== CONSULTAS PRA GUIA
    // (o Guia do SNC Adventures lê o registro direto — tempos/quantidades
    // nunca são escritos em dois lugares, regra data-driven do AGENTS.md)

    /** Base de tempo da DORNA em segundos (a BE escala pelo config). */
    public static final int SEG_DORNA_BASE = 420;
    /** Base de tempo do ALAMBIQUE em segundos (a BE escala pelo config). */
    public static final int SEG_ALAMBIQUE_BASE = 90;

    /** A receita da PRENSA que usa este item como principal (guia). */
    public static Optional<Prima> prensaDe(Item item) {
        return primaPrincipal(MaquinaPrimaBlock.Tipo.PRENSA, item);
    }

    /** A receita da MOENDA que usa este item como principal (guia). */
    public static Optional<Prima> moendaDe(Item item) {
        return primaPrincipal(MaquinaPrimaBlock.Tipo.MOENDA, item);
    }

    /** A receita do CALDEIRÃO que usa este item como principal (guia). */
    public static Optional<Prima> caldeiraoDe(Item item) {
        return primaPrincipal(MaquinaPrimaBlock.Tipo.CALDEIRAO, item);
    }

    // ==================================================== HELPERS DE LOTE

    /**
     * Segundos de cada fase com o balanceamento do ModConfig (as receitas
     * declaram "ritmo base" e o config escala — teste rápido fica fácil).
     */
    public static int tempoFermentacao(Barril r) {
        int base = r.tempoFermentacaoSeg();
        // v1.2.51: base está em SEGUNDOS de design — converte pra ticks (antes
        // o número entrava como ticks: tudo 20× mais rápido que o planejado)
        return base == 0 ? 0 : ModConfig.ticksDeSegundos(base);
    }

    public static int tempoMaturacao(Barril r) {
        int base = r.tempoMaturacaoSeg();
        return base == 0 ? 0 : ModConfig.ticksDeSegundos(base);
    }

    public static int tempoDorna() {
        return ModConfig.ticksDeSegundos(420F);
    }

    public static int tempoAlambique() {
        return ModConfig.ticksDeSegundos(90F);
    }

    public static int garrafasPorLote(Barril r) {
        return Math.max(1, ModConfig.get().bebidaGarrafasPorLote);
    }

    /**
     * Ícone de lote: 1 item "jarra" invisível no barril? NÃO — o lote inteiro
     * mora como contagem no BlockEntity (sem item fantasma).
     */

    /** Tooltip do item intermediário: "etapa X da cadeia de Y" (lore automática). */
    public static Component etapaTooltip(String chave) {
        return Component.translatable(chave).withStyle(s -> s.withColor(0xB8B0A0)
                .withItalic(false));
    }

    /** Stack de um intermediário com a lore da etapa (usado pelos registros do mod). */
    public static ItemStack comEtapa(Item item, String chave) {
        ItemStack stack = new ItemStack(item);
        stack.set(DataComponents.LORE, new net.minecraft.world.item.component.ItemLore(
                List.of(etapaTooltip(chave))));
        return stack;
    }

    /** A garrafa de vidro do vanilla (o item de engarrafamento). */
    public static boolean eGarrafa(Item item) {
        return item == Items.GLASS_BOTTLE;
    }
}
