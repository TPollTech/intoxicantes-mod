package com.intoxicantes;

import java.util.ArrayList;
import java.util.List;

import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

/**
 * A TELA BASE DAS MÁQUINAS DO SNC (v1.2.59): UMA classe, SEIS caras.
 *
 * Desenho 100% vetorial (fill/outline/text/item — zero textura de GUI), na
 * mesma escola do Esquinão: o painel usa a PALETA do {@link TipoMaquina} e
 * cada máquina tem seu ORNAMENTO pixel-art no letreiro. A GEOMETRIA (slots,
 * barra, painel de receitas) é compartilhada — consistente como pede o plano,
 * mas cada máquina é reconhecível de longe.
 *
 * A BARRA É REAL: lê o {@code ContainerData} da BlockEntity
 * ({@code progresso/total} — ticks que faltam sobre o total do lote). Nada
 * de animação inventada no client (regra 9/10 do plano).
 *
 * O painel "PRODUZ" lista as receitas direto do {@link ProcessosBebida} —
 * mesmo catálogo do Guia, nada escrito duas vezes (regra 17).
 *
 * Ponto de costura do vanilla 26.3: {@code AbstractContainerScreen} desenha
 * Screen-bg → translate → labels → slots → item carregado → tooltip. Este
 * método {@code extractLabels} roda DEPOIS do fundo e ANTES dos slots, então
 * o painel pintado aqui fica embaixo dos itens de graça.
 */
public class TelaMaquinaBase extends AbstractContainerScreen<MenuMaquinaSNC> {

    /** Geometria compartilhada (as coordenadas do TipoMaquina mandam nisto). */
    public static final int IMG_W = 208;
    public static final int IMG_H = 222;

    private final TipoMaquina tipo;
    /** Retângulos das linhas de receita desenhadas (tooltip no hover). */
    private final List<int[]> retangulosReceita = new ArrayList<>();
    /** Stacks por linha de receita (mesma ordem dos retângulos). */
    private final List<ItemStack[]> stacksReceita = new ArrayList<>();

    public TelaMaquinaBase(MenuMaquinaSNC menu, Inventory inv, Component titulo) {
        super(menu, inv, titulo, IMG_W, IMG_H);
        this.tipo = menu.tipo();
    }

    // ==================================================== LAYOUT

    private int lx(int x) {
        return leftPos + x;
    }

    private int ly(int y) {
        return topPos + y;
    }

    // ==================================================== DESENHO

    @Override
    protected void extractLabels(GuiGraphicsExtractor g, int mx, int my) {
        desenharPainel(g);
        desenharLetreiro(g);
        desenharCena(g);
        desenharBarra(g);
        desenharReceitas(g, mx, my);
        g.text(this.font, Component.translatable("gui.intoxicantes.maquina.inventario"),
                lx(8), ly(134), tipo.corTextoFraco);
    }

    /** Corpo da máquina: sombra, fundo da paleta e moldura dupla. */
    private void desenharPainel(GuiGraphicsExtractor g) {
        g.fill(lx(3), ly(3), lx(IMG_W + 3), ly(IMG_H + 3), 0x66000000);
        g.fill(lx(0), ly(0), lx(IMG_W), ly(IMG_H), tipo.corFundo);
        g.outline(lx(0), ly(0), IMG_W, IMG_H, tipo.corMoldura);
        g.outline(lx(1), ly(1), IMG_W - 2, IMG_H - 2, tipo.corFaixa);
        g.outline(lx(3), ly(3), IMG_W - 6, IMG_H - 6,
                shade(tipo.corFaixa, 0.8F));
    }

    /** Faixa do título com rebites (a cara "industrial artesanal" da linha). */
    private void desenharLetreiro(GuiGraphicsExtractor g) {
        g.fill(lx(4), ly(4), lx(IMG_W - 4), ly(23), tipo.corFaixa);
        for (int px = lx(6); px < lx(IMG_W - 6); px += 7) {
            g.fill(px, ly(4), px + 3, ly(6), tipo.corAcento);
        }
        g.centeredText(this.font, this.title, lx(IMG_W / 2), ly(11), 0xFFF3E9CF);
    }

    /**
     * A CENA ÚNICA de cada máquina (vignetta do ofício): os retângulos vivem
     * no {@link TipoMaquina}; aqui só resolvemos os códigos simbólicos para a
     * paleta (nunca dessincroniza) e pintamos na ordem do array.
     */
    private void desenharCena(GuiGraphicsExtractor g) {
        for (int[] r : tipo.cena) {
            if (r[4] == -10) {
                g.outline(lx(r[0]), ly(r[1]), r[2] - r[0], r[3] - r[1],
                        tipo.corMoldura);
            } else {
                g.fill(lx(r[0]), ly(r[1]), lx(r[2]), ly(r[3]),
                        resolverCor(r[4]));
            }
        }
    }

    /** Códigos da cena: negativo = simbólico (paleta), senão ARGB literal. */
    private int resolverCor(int codigo) {
        return switch (codigo) {
            case -1 -> tipo.corAcento;
            case -2 -> tipo.corMoldura;
            case -3 -> tipo.corFaixa;
            case -4 -> tipo.corTexto;
            case -5 -> tipo.corTextoFraco;
            default -> codigo;
        };
    }

    /**
     * A BARRA REAL do lote (ContainerData da BlockEntity), na posição que a
     * CENA de cada máquina define, com o status da fase por baixo.
     */
    private void desenharBarra(GuiGraphicsExtractor g) {
        int restante = menu.dado(0);
        int total = menu.dado(1);
        int fase = menu.dado(2);
        int bx = lx(tipo.barraGui[0]);
        int by = ly(tipo.barraGui[1]);
        int bw = tipo.barraGui[2];
        int bh = 6;
        // trilho + preenchimento (a barra do SNC: metal escuro, conteúdo aceso)
        g.fill(bx - 1, by - 1, bx + bw + 1, by + bh + 1, tipo.corMoldura);
        g.fill(bx, by, bx + bw, by + bh, shade(tipo.corFundo, 0.55F));
        boolean pronta = fase == fasePronta();
        float frac = total > 0 ? 1F - restante / (float) total : (pronta ? 1F : 0F);
        frac = net.minecraft.util.Mth.clamp(frac, 0F, 1F);
        int cheio = Math.round(bw * frac);
        if (cheio > 0) {
            g.fill(bx, by, bx + cheio, by + bh, pronta ? 0xFF67C24A : tipo.corAcento);
        }
        // sem rótulo solto: o percentual já mora na frase de fase abaixo
        // status da fase (a dica de "por que não começou" mora aqui)
        Component status = statusDaFase(fase);
        g.centeredText(this.font, status, lx(IMG_W / 2), ly(66),
                pronta ? 0xFF67C24A : tipo.corTexto);
    }

    /** Painel "PRODUZ": receitas direto do catálogo (mesma fonte do Guia). */
    private void desenharReceitas(GuiGraphicsExtractor g, int mx, int my) {
        retangulosReceita.clear();
        stacksReceita.clear();
        g.text(this.font, Component.translatable("gui.intoxicantes.maquina.produz"),
                lx(8), ly(78), tipo.corTextoFraco);
        g.fill(lx(6), ly(84), lx(IMG_W - 6), ly(128), shade(tipo.corFundo, 0.88F));
        List<ItemStack[]> linhas = receitasDoCatalogo();
        int y = 90;
        for (ItemStack[] linha : linhas) {
            int x = 14;
            for (int i = 0; i < linha.length; i++) {
                ItemStack st = linha[i];
                if (st == null) {
                    // separador visual (a seta do processo)
                    g.text(this.font, "→", lx(x), ly(y + 4), tipo.corTextoFraco);
                    x += 14;
                    continue;
                }
                if (st.getCount() < 0) {
                    // sinal "+" entre os dois insumos do caldeirão
                    g.text(this.font, "+", lx(x), ly(y + 4), tipo.corTextoFraco);
                    x += 12;
                    continue;
                }
                g.item(st, lx(x), ly(y));
                g.itemDecorations(this.font, st, lx(x), ly(y));
                x += 20;
            }
            int[] rect = {lx(8), ly(y - 2), IMG_W - 16, 22};
            retangulosReceita.add(rect);
            stacksReceita.add(linha);
            y += 20;
        }
    }

    /** Linhas do catálogo desta máquina: [in, (sinal), (sec), seta, out]. */
    private List<ItemStack[]> receitasDoCatalogo() {
        List<ItemStack[]> linhas = new ArrayList<>();
        switch (tipo) {
            case MOENDA -> {
                for (ProcessosBebida.Prima r : ProcessosBebida.receitasPrima(
                        MaquinaPrimaBlock.Tipo.MOENDA)) {
                    List<ItemStack> linha = new ArrayList<>();
                    linha.add(new ItemStack(r.input(), r.qtdIn()));
                    linha.add(null); // seta
                    linha.add(new ItemStack(r.output(), r.qtdOut()));
                    if (r.extraQtd() > 0) {
                        linha.add(sinalPositivo());
                        linha.add(new ItemStack(r.extraOut(), r.extraQtd()));
                    }
                    linhas.add(linha.toArray(new ItemStack[0]));
                }
            }
            case PRENSA -> {
                for (ProcessosBebida.Prima r : ProcessosBebida.receitasPrima(
                        MaquinaPrimaBlock.Tipo.PRENSA)) {
                    linhas.add(new ItemStack[]{
                            new ItemStack(r.input(), r.qtdIn()), null,
                            new ItemStack(r.output(), r.qtdOut())});
                }
            }
            case CALDEIRAO -> {
                for (ProcessosBebida.Prima r : ProcessosBebida.receitasPrima(
                        MaquinaPrimaBlock.Tipo.CALDEIRAO)) {
                    if (r.secIn() != null) {
                        linhas.add(new ItemStack[]{
                                new ItemStack(r.input(), r.qtdIn()),
                                sinalPositivo(), new ItemStack(r.secIn(), r.secQtd()),
                                null, new ItemStack(r.output(), r.qtdOut())});
                    } else {
                        linhas.add(new ItemStack[]{
                                new ItemStack(r.input(), r.qtdIn()), null,
                                new ItemStack(r.output(), r.qtdOut())});
                    }
                }
            }
            case DORNA -> {
                for (ProcessosBebida.Dorna r : ProcessosBebida.receitasDorna()) {
                    linhas.add(new ItemStack[]{
                            new ItemStack(r.input(), r.qtdIn()), null,
                            new ItemStack(r.output(), r.qtdOut())});
                }
            }
            case ALAMBIQUE -> {
                for (ProcessosBebida.Alambique r : ProcessosBebida.receitasAlambique()) {
                    linhas.add(new ItemStack[]{
                            new ItemStack(r.input(), r.qtdIn()), null,
                            new ItemStack(r.output(), r.qtdOut())});
                }
            }
            case BARRIL -> {
                for (ProcessosBebida.Barril r : ProcessosBebida.receitasBarril()) {
                    linhas.add(new ItemStack[]{
                            new ItemStack(r.input(), r.qtdIn()), null,
                            new ItemStack(r.bebidaFinal(),
                                    ProcessosBebida.garrafasPorLote(r))});
                }
            }
        }
        return linhas;
    }

    /** Marcador do "+" entre os dois insumos (count negativo = sinal). */
    private ItemStack sinalPositivo() {
        ItemStack s = new ItemStack(net.minecraft.world.item.Items.PAPER);
        s.setCount(-1);
        return s;
    }

    // ==================================================== STATUS / FASES

    /**
     * A fase vem do ContainerData (índice 2). Cada BlockEntity numera as
     * próprias fases — a chave de lang é data-driven por máquina
     * ({@code gui.intoxicantes.maquina.<id>.fase.<n>}), sem colisão.
     */
    private Component statusDaFase(int fase) {
        if (fase == fasePronta()) {
            return Component.translatable(
                    "gui.intoxicantes.maquina." + tipo.id + ".fase." + fase);
        }
        int restante = menu.dado(0);
        int total = menu.dado(1);
        int pct = total > 0
                ? Math.max(0, Math.min(100, (total - restante) * 100 / total)) : 0;
        return Component.translatable(
                "gui.intoxicantes.maquina." + tipo.id + ".fase." + fase, pct);
    }

    /** A fase "produto pronto" de cada BE (2 na dorna/alambique, 3 barril, 4 prima). */
    private int fasePronta() {
        return switch (tipo) {
            case MOENDA, PRENSA, CALDEIRAO -> MaquinaPrimaBlockEntity.FASE_PRONTA;
            case DORNA -> DornaBebidaBlockEntity.FASE_PRONTA;
            case ALAMBIQUE -> AlambiqueBlockEntity.FASE_PRONTA;
            case BARRIL -> BarrilBebidaBlockEntity.FASE_PRONTA;
        };
    }

    // ==================================================== SLOTS / TOOLTIP

    /** Quadro do slot na cor da máquina (o vanilla desenha o fundo+item). */
    @Override
    protected void extractSlot(GuiGraphicsExtractor g, Slot slot, int mx, int my) {
        super.extractSlot(g, slot, mx, my);
        int cor = (slot.index < tipo.totalSlots())
                ? tipo.corAcento : shade(tipo.corFaixa, 0.85F);
        g.outline(lx(slot.x) - 1, ly(slot.y) - 1, 18, 18, cor);
    }

    @Override
    protected void extractTooltip(GuiGraphicsExtractor g, int mx, int my) {
        super.extractTooltip(g, mx, my);
        for (int i = 0; i < retangulosReceita.size(); i++) {
            int[] r = retangulosReceita.get(i);
            if (mx >= r[0] && mx < r[0] + r[2] && my >= r[1] && my < r[1] + r[3]) {
                List<Component> tip = new ArrayList<>();
                ItemStack[] linha = stacksReceita.get(i);
                for (ItemStack st : linha) {
                    if (st == null || st.isEmpty() || st.getCount() < 0) {
                        continue;
                    }
                    tip.add(Component.translatable(
                            "gui.intoxicantes.maquina.receita_linha",
                            st.getCount(), st.getHoverName()));
                }
                g.setComponentTooltipForNextFrame(this.font, tip, mx, my);
                return;
            }
        }
    }

    // ==================================================== GERAL

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    /** Escurece/clareia uma cor ARGB (molduras internas sem paleta extra). */
    private static int shade(int argb, float fator) {
        int a = (argb >>> 24) & 0xFF;
        int r = Math.min(255, Math.round(((argb >>> 16) & 0xFF) * fator));
        int gr = Math.min(255, Math.round(((argb >>> 8) & 0xFF) * fator));
        int b = Math.min(255, Math.round((argb & 0xFF) * fator));
        return (a << 24) | (r << 16) | (gr << 8) | b;
    }

    /** Fonte exposta pras subclasses de ornamento (nada por enquanto). */
    protected Font fonte() {
        return this.font;
    }
}
