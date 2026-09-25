package com.intoxicantes;

import java.util.ArrayList;
import java.util.List;

import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;
import net.minecraft.world.item.ItemStack;

/**
 * O PONTO do traficante (v1.2.44): tela própria no estilo de rua — fundo
 * asfalto, faixa amarela no topo, caixa de madeira pro estoque, aba de
 * fiado e o card "LANÇAMENTO DO DIA" (o diamante).
 *
 * Mesma engenharia do cardápio do Gago: desenho 100% vetorial pelo
 * GuiGraphicsExtractor (fill/outline/text/item), hit-testing manual nas
 * mesmas caixas que são desenhadas, zero textura e zero widget vanilla.
 * Toda regra de negócio é do servidor; aqui é só pintura e envio de ação.
 */
public class PontoTraficanteScreen extends Screen {
    // ==================================================== PALETA (a cara da rua)
    private static final int ASFALTO = 0xFF23262B;
    private static final int ASFALTO_CLARO = 0xFF2E3238;
    private static final int FAIXA = 0xFFD9B23A;        // faixa amarela da rua
    private static final int MADEIRA = 0xFF6B4A2B;      // caixa de estoque
    private static final int MADEIRA_ESCURA = 0xFF4A3320;
    private static final int PAPEL = 0xFFF1E4C3;
    private static final int PAPEL_SOMBRA = 0xFFD8C79A;
    private static final int TINTA = 0xFF2B2417;
    private static final int TINTA_FRACA = 0xFF8A7B5A;
    private static final int VERDE_CLARO = 0xFF1E6B38;  // dinheiro
    private static final int VERDE_HOVER = 0xFF2E8A4A;
    private static final int VERDE_BORDA = 0xFF083D1E;
    private static final int VERMELHO = 0xFFB03030;
    private static final int OURO = 0xFFC8971E;
    private static final int OURO_CLARO = 0xFFF2D06B;
    private static final int CIANO_NEON = 0xFF39D0C4;   // letreiro do ponto
    private static final int LINHA_ZEBRA = 0x28D9C89A;

    // ==================================================== LAYOUT
    private static final int LARGURA = 310;
    private static final int LINHA_ALT = 32;

    private EsquinaoNetworking.AbrirPontoPayload p;
    private int aba;            // 0=estoque 1=fiado 2=colheitas
    private boolean aguardando;
    private int x0;
    private int y0;
    private int altPainel;
    private int listaY;
    private int linhasVisiveis;
    private int scrollLinha;
    private boolean arrastandoBarra;
    private double arrastoOffset;
    private final int[] scrollAbas = new int[3];

    /** Catalogo fixo do traficante, na MESMA ordem do servidor (0..6). */
    private static final String[] IDS = {
            "maconha_seda", "cocaina", "heroina", "lsd", "baseado", "opio", "extrato_cafeina"
    };

    public PontoTraficanteScreen(EsquinaoNetworking.AbrirPontoPayload payload) {
        super(Component.translatable("gui.intoxicantes.ponto.titulo"));
        this.p = payload;
    }

    public void atualizar(EsquinaoNetworking.AbrirPontoPayload payload) {
        this.p = payload;
        this.aguardando = false;
        this.scrollLinha = Mth.clamp(scrollLinha, 0, maxScroll());
    }

    // ==================================================== GEOMETRIA

    @Override
    protected void init() {
        this.altPainel = Math.min(380, this.height - 12);
        this.x0 = (this.width - LARGURA) / 2;
        this.y0 = (this.height - altPainel) / 2;
        this.listaY = this.y0 + 78;
        this.linhasVisiveis = Math.max(1, (this.y0 + altPainel - 36 - this.listaY) / LINHA_ALT);
        this.scrollLinha = Mth.clamp(this.scrollLinha, 0, maxScroll());
    }

    private int totalLinhas() {
        return switch (aba) {
            case 1 -> 2;                      // pedir fiado + pagar divida
            case 2 -> 6;                      // colheitas (harvests do Gago)
            default -> IDS.length;            // estoque 0..6
        };
    }

    private int maxScroll() {
        return Math.max(0, totalLinhas() - linhasVisiveis);
    }

    private int barraX() {
        return this.x0 + LARGURA - 8;
    }

    private int[] botaoDaLinha(int linhaVisivel) {
        int y = this.listaY + linhaVisivel * LINHA_ALT + 2;
        return new int[]{this.x0 + LARGURA - 68, y + 3, 54, 16};
    }

    private int[] retanguloFechar() {
        return new int[]{this.x0 + LARGURA - 52, this.y0 + altPainel - 30, 46, 14};
    }

    private int[] aba(int i) {
        int larguraAba = 100;
        return new int[]{x0 + 6 + i * (larguraAba + 3), y0 + 58, larguraAba, 16};
    }

    private boolean dentro(double mx, double my, int[] r) {
        return mx >= r[0] && mx < r[0] + r[2] && my >= r[1] && my < r[1] + r[3];
    }

    private boolean disponivel(int linha) {
        if (aguardando) return false;
        if (aba == 0) {
            return estoqueDo(linha) > 0 && p.saldo() >= precoBaseDo(linha)
                    * (100 - p.desconto()) / 100;
        }
        if (aba == 2) {
            return precoColheitaDo(linha) > 0;
        }
        return switch (linha) {
            case 0 -> p.fiadoNivel() >= 0 && p.divida() < 200;   // pedir fiado
            case 1 -> p.divida() > 0;                             // pagar divida
            default -> false;
        };
    }

    // ==================================================== ENTRADA

    @Override
    public boolean mouseClicked(MouseButtonEvent ev, boolean duplo) {
        if (ev.button() != com.mojang.blaze3d.platform.InputConstants.MOUSE_BUTTON_LEFT) {
            return super.mouseClicked(ev, duplo);
        }
        double mx = ev.x();
        double my = ev.y();
        for (int i = 0; i < 3; i++) {
            if (dentro(mx, my, aba(i))) {
                if (aba != i) {
                    scrollAbas[aba] = scrollLinha;
                    aba = i;
                    scrollLinha = Mth.clamp(scrollAbas[aba], 0, maxScroll());
                    arrastandoBarra = false;
                }
                return true;
            }
        }
        if (dentro(mx, my, retanguloFechar())) {
            this.onClose();
            return true;
        }
        for (int v = 0; v < linhasVisiveis; v++) {
            int linha = scrollLinha + v;
            if (linha >= totalLinhas()) break;
            if (dentro(mx, my, botaoDaLinha(v))) {
                if (!disponivel(linha)) return true;
                aguardando = true;
                switch (aba) {
                    case 1 -> ClientPlayNetworking.send(linha == 0
                            ? new EsquinaoNetworking.FiadoPayload()
                            : new EsquinaoNetworking.DiazinhoPayload());
                    case 2 -> ClientPlayNetworking.send(
                            new EsquinaoNetworking.VenderPayload(linha));
                    default -> ClientPlayNetworking.send(
                            new EsquinaoNetworking.ComprarPayload(linha));
                }
                return true;
            }
        }
        if (maxScroll() > 0 && mx >= barraX() - 1 && mx <= barraX() + 6
                && my >= listaY && my < listaY + linhasVisiveis * LINHA_ALT) {
            this.arrastandoBarra = true;
            int thumb = thumbHeight();
            double thumbTop = listaY + (linhasVisiveis * LINHA_ALT - thumb)
                    * (double) scrollLinha / maxScroll();
            arrastoOffset = my >= thumbTop && my < thumbTop + thumb ? my - thumbTop : thumb / 2.0;
            atualizarArrasto(my);
            return true;
        }
        return super.mouseClicked(ev, duplo);
    }

    @Override
    public boolean mouseReleased(MouseButtonEvent ev) {
        this.arrastandoBarra = false;
        return super.mouseReleased(ev);
    }

    private int thumbHeight() {
        return Math.max(10, linhasVisiveis * LINHA_ALT * linhasVisiveis / totalLinhas());
    }

    private void atualizarArrasto(double my) {
        int travel = linhasVisiveis * LINHA_ALT - thumbHeight();
        scrollLinha = Mth.clamp((int) Math.round((my - listaY - arrastoOffset)
                * maxScroll() / Math.max(1, travel)), 0, maxScroll());
    }

    @Override
    public boolean mouseScrolled(double mx, double my, double dx, double dy) {
        if (maxScroll() > 0 && mx >= x0 && mx < x0 + LARGURA
                && my >= listaY && my < listaY + linhasVisiveis * LINHA_ALT && dy != 0) {
            scrollLinha = Mth.clamp(scrollLinha - (int) Math.signum(dy), 0, maxScroll());
            return true;
        }
        return super.mouseScrolled(mx, my, dx, dy);
    }

    @Override
    public boolean keyPressed(net.minecraft.client.input.KeyEvent ev) {
        if (this.minecraft.options.keyInventory.matches(ev)) {
            this.onClose();
            return true;
        }
        return super.keyPressed(ev);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    @Override
    public void onClose() {
        ClientPlayNetworking.send(new EsquinaoNetworking.FecharCardapioPayload());
        super.onClose();
    }

    // ==================================================== DESENHO

    @Override
    public void extractRenderState(GuiGraphicsExtractor g, int mx, int my, float parcial) {
        desenharDim(g);
        desenharPainel(g);
        desenharCabecalho(g);
        desenharBarraDesconto(g);
        for (int i = 0; i < 3; i++) {
            int[] tab = aba(i);
            boolean ativa = aba == i;
            g.fill(tab[0], tab[1], tab[0] + tab[2], tab[1] + tab[3], ativa ? MADEIRA : PAPEL_SOMBRA);
            textoCentrado(g, Component.translatable(NOME_ABA[i]), tab[0] + tab[2] / 2, tab[1] + 4, ativa ? 0xFFFFFFFF : TINTA);
        }
        desenharLista(g, mx, my);
        desenharRodape(g, mx, my);
    }

    private static final String[] NOME_ABA = {
            "gui.intoxicantes.ponto.aba.estoque", "gui.intoxicantes.ponto.aba.fiado",
            "gui.intoxicantes.ponto.aba.colheitas"
    };

    /** v1.2.44: texto CENTRADO sem sombra (letra limpa, sem negrito falso). */
    private void textoCentrado(GuiGraphicsExtractor g, Component texto, int cx, int y, int cor) {
        g.text(this.font, texto, cx - this.font.width(texto) / 2, y, cor, false);
    }

    private void desenharDim(GuiGraphicsExtractor g) {
        g.fill(0, 0, this.width, this.height, 0x99000000);
    }

    /** Caixa de rua: asfalto, moldura de madeira e a faixa amarela. */
    private void desenharPainel(GuiGraphicsExtractor g) {
        g.fill(x0 + 3, y0 + 3, x0 + LARGURA + 3, y0 + altPainel + 3, 0x66000000);
        g.fill(x0, y0, x0 + LARGURA, y0 + altPainel, ASFALTO);
        // moldura de madeira (4 lados)
        g.fill(x0, y0, x0 + LARGURA, y0 + 4, MADEIRA);
        g.fill(x0, y0 + altPainel - 4, x0 + LARGURA, y0 + altPainel, MADEIRA);
        g.fill(x0, y0, x0 + 4, y0 + altPainel, MADEIRA);
        g.fill(x0 + LARGURA - 4, y0, x0 + LARGURA, y0 + altPainel, MADEIRA);
        g.outline(x0, y0, LARGURA, altPainel, MADEIRA_ESCURA);
        // risquinhos de tarmac no fundo
        for (int y = y0 + 10; y < y0 + altPainel - 10; y += 24) {
            g.fill(x0 + 10, y, x0 + LARGURA - 10, y + 1, 0x10FFFFFF);
        }
    }

    /** Letreiro neon do ponto + faixa amarela de rua. */
    private void desenharCabecalho(GuiGraphicsExtractor g) {
        int y = y0 + 8;
        // faixa amarela tracejada (a rua na frente do ponto)
        for (int px = x0 + 10; px < x0 + LARGURA - 16; px += 22) {
            g.fill(px, y, px + 12, y + 3, FAIXA);
        }
        textoCentrado(g, Component.translatable("gui.intoxicantes.ponto.letreiro"), this.x0 + LARGURA / 2, y + 8, CIANO_NEON);
        textoCentrado(g, Component.translatable("gui.intoxicantes.ponto.subtitulo"), this.x0 + LARGURA / 2, y + 18, 0xFF9AA0A6);
    }

    /** Desconto de fila ativo (a regua de LARGO da rua). */
    private void desenharBarraDesconto(GuiGraphicsExtractor g) {
        int y = y0 + 36;
        int alt = 18;
        g.fill(x0 + 6, y, x0 + LARGURA - 6, y + alt, ASFALTO_CLARO);
        g.outline(x0 + 6, y, LARGURA - 12, alt, MADEIRA_ESCURA);
        String rotulo = Component.translatable("gui.intoxicantes.ponto.desconto",
                p.desconto(), p.comprasHoje(), p.exclusiveEstoque() > 0
                        ? Component.translatable("gui.intoxicantes.ponto.tem_lancamento").getString()
                        : Component.translatable("gui.intoxicantes.ponto.sem_lancamento").getString()).getString();
        g.text(this.font, rotulo, x0 + 12, y + 5, p.desconto() > 0 ? OURO_CLARO : 0xFF9AA0A6, false);
    }

    private int estoqueDo(int i) {
        return switch (i) {
            case 0 -> p.estoqueMaconha();
            case 1 -> p.estoqueCocaina();
            case 2 -> p.estoqueHeroina();
            case 3 -> p.estoqueLsd();
            case 4 -> p.estoqueBaseado();
            case 5 -> p.estoqueOpio();
            default -> p.estoqueExtrato();
        };
    }

    /** Preço de referência do dia (o server aplica os descontos no final). */
    private static final int[] PRECO_REF = {12, 42, 44, 48, 34, 14, 20};

    private int precoBaseDo(int i) {
        return PRECO_REF[Math.min(i, PRECO_REF.length - 1)];
    }

    /** Pagamentos das colheitas (mesma ordem do harvests() do servidor). */
    private int precoColheitaDo(int i) {
        return switch (i) {
            case 0, 1 -> 6;    // lúpulo, uva
            case 2, 3 -> 4;    // trigo, cana
            case 4 -> 8;       // mel
            default -> 4;      // garrafas
        };
    }

    private void desenharLista(GuiGraphicsExtractor g, int mx, int my) {
        int altLista = linhasVisiveis * LINHA_ALT;
        g.fill(x0 + 6, listaY, x0 + LARGURA - 6, listaY + altLista, 0x18D9C89A);
        g.enableScissor(x0 + 6, listaY, x0 + LARGURA - 6, listaY + altLista);

        for (int v = 0; v < linhasVisiveis; v++) {
            int indice = scrollLinha + v;
            if (indice >= totalLinhas()) break;
            int ry = listaY + v * LINHA_ALT;
            boolean hover = (int) mx >= x0 + 6 && (int) mx < x0 + LARGURA - 10
                    && (int) my >= ry && (int) my < ry + LINHA_ALT;
            if (indice % 2 == 0) {
                g.fill(x0 + 6, ry, x0 + LARGURA - 10, ry + LINHA_ALT, LINHA_ZEBRA);
            }
            if (hover) {
                g.fill(x0 + 6, ry, x0 + LARGURA - 10, ry + LINHA_ALT, 0x20FFFFFF);
            }

            if (aba == 0) {
                desenharLinhaEstoque(g, indice, ry);
            } else if (aba == 1) {
                desenharLinhaFiado(g, indice, ry);
            } else {
                desenharLinhaColheita(g, indice, ry);
            }

            // botão da ação
            int[] b = botaoDaLinha(v);
            boolean ok = disponivel(indice);
            boolean botaoHover = dentro(mx, my, b) && ok;
            g.fill(b[0], b[1], b[0] + b[2], b[1] + b[3],
                    !ok ? 0xFF5A5A50 : botaoHover ? VERDE_HOVER : VERDE_CLARO);
            g.outline(b[0], b[1], b[2], b[3], VERDE_BORDA);
            String rotulo = rotuloDoBotao(indice);
            textoCentrado(g, Component.literal(this.font.plainSubstrByWidth(rotulo, b[2] - 4)), b[0] + b[2] / 2, b[1] + 4, 0xFFF3E9CF);

            List<Component> tip = tooltipDe(indice);
            if (hover && tip != null && !tip.isEmpty()) {
                g.setComponentTooltipForNextFrame(this.font, tip, mx, my);
            }
        }
        g.disableScissor();

        if (maxScroll() > 0) {
            int altLista2 = linhasVisiveis * LINHA_ALT;
            g.fill(barraX(), listaY, barraX() + 4, listaY + altLista2, 0xFFC9B888);
            float frac = (float) scrollLinha / maxScroll();
            int pad = thumbHeight();
            int py = listaY + (int) ((altLista2 - pad) * frac);
            g.fill(barraX(), py, barraX() + 4, py + pad, OURO);
            g.outline(barraX(), py, 4, pad, MADEIRA_ESCURA);
        }
    }

    private void desenharLinhaEstoque(GuiGraphicsExtractor g, int indice, int ry) {
        ItemStack demo = demoStack(indice);
        g.item(demo, x0 + 8, ry);
        g.itemDecorations(this.font, demo, x0 + 8, ry);
        String nome = demo.getHoverName().getString();
        g.text(this.font, this.font.plainSubstrByWidth(nome, 154), x0 + 26, ry + 4, TINTA, false);
        int estoque = estoqueDo(indice);
        int preco = Math.max(1, Math.round(precoBaseDo(indice)
                * (100 - p.desconto()) / 100.0F));
        String detalhe = Component.translatable(estoque == 0
                ? "commerce.intoxicantes.sold_out" : "gui.intoxicantes.ponto.unidades", estoque).getString();
        g.text(this.font, this.font.plainSubstrByWidth(detalhe, 154), x0 + 26, ry + 14,
                estoque == 0 ? VERMELHO : TINTA_FRACA, false);
        String etiqueta = "R$" + preco;
        g.text(this.font, etiqueta, x0 + 185, ry + 8,
                p.saldo() < preco ? VERMELHO : OURO_CLARO, false);
    }

    private void desenharLinhaFiado(GuiGraphicsExtractor g, int indice, int ry) {
        String titulo = Component.translatable(indice == 0
                ? "gui.intoxicantes.ponto.pedir_fiado" : "gui.intoxicantes.ponto.pagar_fiado").getString();
        g.text(this.font, this.font.plainSubstrByWidth(titulo, 220), x0 + 10, ry + 4, TINTA, false);
        String nivel = Component.translatable("gui.intoxicantes.ponto.nivel_fiado",
                p.fiadoNivel(), p.fiadoDevendo(), p.divida()).getString();
        g.text(this.font, this.font.plainSubstrByWidth(nivel, 230), x0 + 10, ry + 15, TINTA_FRACA, false);
        String detalhe = indice == 0
                ? Component.translatable("gui.intoxicantes.ponto.empresta_50").getString()
                : Component.translatable("gui.intoxicantes.ponto.quita_juros").getString();
        g.text(this.font, this.font.plainSubstrByWidth(detalhe, 220), x0 + 10, ry + 24, TINTA_FRACA, false);
    }

    private void desenharLinhaColheita(GuiGraphicsExtractor g, int indice, int ry) {
        ItemStack demo = demoColheita(indice);
        g.item(demo, x0 + 8, ry);
        g.itemDecorations(this.font, demo, x0 + 8, ry);
        String nome = demo.getHoverName().getString();
        g.text(this.font, this.font.plainSubstrByWidth(nome, 154), x0 + 26, ry + 4, TINTA, false);
        String detalhe = Component.translatable("gui.intoxicantes.ponto.compra_lote",
                demo.getCount()).getString();
        g.text(this.font, this.font.plainSubstrByWidth(detalhe, 154), x0 + 26, ry + 14, TINTA_FRACA, false);
        String etiqueta = "R$" + precoColheitaDo(indice);
        g.text(this.font, etiqueta, x0 + 185, ry + 8, OURO_CLARO, false);
    }

    /** ItemStack de demonstração pro ícone (o servidor entrega o real). */
    private ItemStack demoStack(int indice) {
        return switch (indice) {
            case 0 -> new ItemStack(IntoxicantesMod.MACONHA_SEDA);
            case 1 -> new ItemStack(IntoxicantesMod.COCAINA);
            case 2 -> new ItemStack(IntoxicantesMod.HEROINA);
            case 3 -> new ItemStack(IntoxicantesMod.LSD);
            case 4 -> new ItemStack(IntoxicantesMod.BASEADO);
            case 5 -> new ItemStack(IntoxicantesMod.OPIO);
            default -> new ItemStack(IntoxicantesMod.EXTRATO_CAFEINA);
        };
    }

    private ItemStack demoColheita(int indice) {
        var colheitas = TradeCatalogClient.colheitas();
        return indice >= 0 && indice < colheitas.size()
                ? colheitas.get(indice) : new ItemStack(net.minecraft.world.item.Items.WHEAT);
    }

    private String rotuloDoBotao(int indice) {
        if (aguardando) return "…";
        return switch (aba) {
            case 1 -> indice == 0
                    ? Component.translatable("gui.intoxicantes.ponto.pedir").getString()
                    : Component.translatable("gui.intoxicantes.ponto.pagar").getString();
            case 2 -> Component.translatable("commerce.intoxicantes.sell_short").getString();
            default -> estoqueDo(indice) == 0
                    ? Component.translatable("commerce.intoxicantes.sold_out").getString()
                    : Component.translatable("gui.intoxicantes.cardapio.comprar_curtinho").getString();
        };
    }

    private List<Component> tooltipDe(int indice) {
        List<Component> tip = new ArrayList<>();
        if (aba == 0) {
            ItemStack demo = demoStack(indice);
            tip.add(demo.getHoverName());
            String id = IDS[indice];
            tip.add(Component.translatable("commerce.intoxicantes.use." + id));
            if (p.exclusiveN() == indice && p.exclusiveEstoque() > 0) {
                tip.add(Component.translatable("gui.intoxicantes.ponto.e_lancamento"));
            }
        } else if (aba == 1) {
            if (indice == 0) {
                tip.add(Component.translatable("gui.intoxicantes.ponto.fiado_dica1"));
                tip.add(Component.translatable("gui.intoxicantes.ponto.fiado_dica2"));
            } else {
                tip.add(Component.translatable("gui.intoxicantes.ponto.pagar_dica1"));
            }
        }
        return tip;
    }

    /** Rodapé: saldo, divida e fechar. */
    private void desenharRodape(GuiGraphicsExtractor g, int mx, int my) {
        int y = this.y0 + altPainel - 34;
        g.horizontalLine(x0 + 8, x0 + LARGURA - 8, y - 2, MADEIRA_ESCURA);
        String saldo = Component.translatable("gui.intoxicantes.cardapio.saldo", p.saldo()).getString();
        g.text(this.font, saldo, x0 + 10, y + 4, TINTA, false);
        if (p.divida() > 0) {
            String divida = Component.translatable("gui.intoxicantes.ponto.devendo", p.divida()).getString();
            g.text(this.font, divida, x0 + 10, y + 17, VERMELHO, false);
        } else {
            String limpo = Component.translatable("gui.intoxicantes.ponto.sem_divida_rodape").getString();
            g.text(this.font, limpo, x0 + 10, y + 17, TINTA_FRACA, false);
        }
        int[] f = retanguloFechar();
        boolean hover = dentro(mx, my, f);
        g.fill(f[0], f[1], f[0] + f[2], f[1] + f[3], hover ? VERDE_HOVER : VERDE_CLARO);
        g.outline(f[0], f[1], f[2], f[3], VERDE_BORDA);
        String fechar = Component.translatable("gui.intoxicantes.cardapio.fechar").getString();
        textoCentrado(g, Component.literal(fechar), f[0] + f[2] / 2, f[1] + 3, 0xFFF3E9CF);
    }
}
