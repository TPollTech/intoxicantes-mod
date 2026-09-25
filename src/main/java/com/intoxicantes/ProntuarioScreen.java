package com.intoxicantes;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;

/**
 * v1.2.54 — O PRONTUARIO DO FREGUES: a ficha médica da esquina (tecla H).
 * Papel-moeda bege igual ao cardápio, três ORGÃOS com barra de saúde e o
 * estágio com humor ("Fígado Zangado" → "Cirrose em estágio de Jucelino" 🥃),
 * o HISTÓRICO de doses da vida e o VEREDITO do médico da esquina no rodapé.
 *
 * Zero textura, zero widget vanilla: mesmo desenho vetorial do cardápio
 * (GuiGraphicsExtractor), mesmo papel, mesma moldura verde — a clínica é
 * filial do mercado kkkk.
 */
public class ProntuarioScreen extends Screen {
    // ==================================================== PALETA (a clínica)
    private static final int VERDE_BORDA = 0xFF083D1E;
    private static final int VERDE_LETREIRO = 0xFF0E5A2E;
    private static final int PAPEL = 0xFFF1E4C3;
    private static final int PAPEL_SOMBRA = 0xFFD8C79A;
    private static final int TINTA = 0xFF2B2417;
    private static final int TINTA_FRACA = 0xFF8A7B5A;
    private static final int VERDE_SAUDE = 0xFF3E9A50;
    private static final int AMARELO_ALERTA = 0xFFC8971E;
    private static final int VERMELHO_GRAVE = 0xFFB03030;

    /** Dados que chegam do SaudeSyncPayload (server-authoritative). */
    public record Dados(int hidratacao, int figado, int pulmao, int estomago,
                        int vicio, String drogaVicio, int alcool, int erva,
                        int po, int pilula) {}

    private final Dados d;
    private int x0;
    private int y0;
    private static final int LARGURA = 260;
    private int altPainel;

    public ProntuarioScreen(Dados dados) {
        super(Component.translatable("gui.intoxicantes.prontuario.titulo"));
        this.d = dados;
    }

    // ==================================================== GEOMETRIA

    @Override
    protected void init() {
        this.altPainel = Math.min(240, this.height - 20);
        this.x0 = (this.width - LARGURA) / 2;
        this.y0 = (this.height - altPainel) / 2;
    }

    @Override
    public boolean keyPressed(KeyEvent ev) {
        if (this.minecraft.options.keyInventory.matches(ev)
                || SaudeClient.teclaProntuarioMatches(ev)) {
            this.onClose();
            return true;
        }
        return super.keyPressed(ev);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    // ==================================================== DESENHO

    @Override
    public void extractRenderState(GuiGraphicsExtractor g, int mx, int my, float parcial) {
        // dim + papel + moldura dupla (o mesmo papel do cardápio)
        g.fill(0, 0, this.width, this.height, 0x99000000);
        g.fill(x0 + 3, y0 + 3, x0 + LARGURA + 3, y0 + altPainel + 3, 0x66000000);
        g.fill(x0, y0, x0 + LARGURA, y0 + altPainel, PAPEL);
        g.outline(x0, y0, LARGURA, altPainel, VERDE_BORDA);
        g.outline(x0 + 1, y0 + 1, LARGURA - 2, altPainel - 2, VERDE_LETREIRO);
        g.outline(x0 + 3, y0 + 3, LARGURA - 6, altPainel - 6, PAPEL_SOMBRA);

        desenharCabecalho(g);
        desenharOrgaos(g);
        desenharHistorico(g);
        desenharVeredito(g);
    }

    private void desenharCabecalho(GuiGraphicsExtractor g) {
        g.fill(x0 + 4, y0 + 4, x0 + LARGURA - 4, y0 + 26, VERDE_LETREIRO);
        g.centeredText(this.font,
                Component.translatable("gui.intoxicantes.prontuario.titulo"),
                x0 + LARGURA / 2, y0 + 9, 0xFFF3E9CF);
        g.centeredText(this.font,
                Component.translatable("gui.intoxicantes.prontuario.subtitulo"),
                x0 + LARGURA / 2, y0 + 19, 0xFFF2D06B);
    }

    /** Os 3 órgãos: nome + estágio + barra de saúde (verde→amarelo→vermelho). */
    private void desenharOrgaos(GuiGraphicsExtractor g) {
        int y = y0 + 34;
        SaudeData.Orgao[] orgaos = {SaudeData.Orgao.FIGADO, SaudeData.Orgao.PULMAO, SaudeData.Orgao.ESTOMAGO};
        int[] danos = {d.figado(), d.pulmao(), d.estomago()};
        for (int i = 0; i < orgaos.length; i++) {
            int dano = danos[i];
            int saudePct = Math.max(0, 100 - dano * 100 / SaudeData.ORGAO_MAX);
            // linha zebra
            if (i % 2 == 1) {
                g.fill(x0 + 6, y - 2, x0 + LARGURA - 6, y + 22, 0x28D9C89A);
            }
            // nome do órgão + estágio
            g.text(this.font, Component.translatable("gui.intoxicantes.prontuario.orgao." + orgaos[i].name()),
                    x0 + 10, y, TINTA);
            String estagio = estagioDe(orgaos[i], saudePct);
            g.text(this.font, Component.translatable(estagio), x0 + 10, y + 10,
                    saudePct > 60 ? TINTA_FRACA : (saudePct > 25 ? AMARELO_ALERTA : VERMELHO_GRAVE));
            // barra de saúde (a direita, 90px)
            int barraX = x0 + 150;
            int barraW = 96;
            g.fill(barraX, y + 2, barraX + barraW, y + 8, 0x30302A18);
            int cor = saudePct > 60 ? VERDE_SAUDE : (saudePct > 25 ? AMARELO_ALERTA : VERMELHO_GRAVE);
            g.fill(barraX, y + 2, barraX + (int) (barraW * saudePct / 100F), y + 8, cor);
            g.text(this.font, saudePct + "%", barraX + barraW + 4, y + 2, TINTA);
            y += 26;
        }
    }

    /** A chave de lang do estágio (server/client compartilham o critério). */
    static String estagioDe(SaudeData.Orgao orgao, int saudePct) {
        String orgaoId = orgao.name().toLowerCase();
        if (saudePct > 80) return "gui.intoxicantes.prontuario.estagio.0." + orgaoId;
        if (saudePct > 60) return "gui.intoxicantes.prontuario.estagio.1." + orgaoId;
        if (saudePct > 40) return "gui.intoxicantes.prontuario.estagio.2." + orgaoId;
        if (saudePct > 20) return "gui.intoxicantes.prontuario.estagio.3." + orgaoId;
        return "gui.intoxicantes.prontuario.estagio.4." + orgaoId;
    }

    private void desenharHistorico(GuiGraphicsExtractor g) {
        int y = y0 + 34 + 3 * 26 + 4;
        g.text(this.font, Component.translatable("gui.intoxicantes.prontuario.historico"),
                x0 + 10, y, TINTA);
        g.text(this.font,
                Component.translatable("gui.intoxicantes.prontuario.historico.linhas",
                        d.alcool(), d.erva(), d.po(), d.pilula()),
                x0 + 10, y + 11, TINTA_FRACA);
    }

    private void desenharVeredito(GuiGraphicsExtractor g) {
        int y = y0 + altPainel - 24;
        g.fill(x0 + 6, y - 4, x0 + LARGURA - 6, y + 16, 0x300E5A2E);
        SaudeData.Orgao pior = piorOrgao();
        int saudePct = Math.max(0, 100 - saudeDoPior(pior) * 100 / SaudeData.ORGAO_MAX);
        g.centeredText(this.font, Component.translatable(vereditoDe(saudePct)),
                x0 + LARGURA / 2, y, 0xFF2B2417);
        g.centeredText(this.font,
                Component.translatable("gui.intoxicantes.prontuario.medico"),
                x0 + LARGURA / 2, y + 9, TINTA_FRACA);
    }

    private SaudeData.Orgao piorOrgao() {
        if (d.figado() >= d.pulmao() && d.figado() >= d.estomago()) return SaudeData.Orgao.FIGADO;
        if (d.pulmao() >= d.estomago()) return SaudeData.Orgao.PULMAO;
        return SaudeData.Orgao.ESTOMAGO;
    }

    private int saudeDoPior(SaudeData.Orgao orgao) {
        return switch (orgao) {
            case FIGADO -> d.figado();
            case PULMAO -> d.pulmao();
            case ESTOMAGO -> d.estomago();
        };
    }

    static String vereditoDe(int saudePct) {
        if (saudePct > 80) return "gui.intoxicantes.prontuario.veredito.0";
        if (saudePct > 60) return "gui.intoxicantes.prontuario.veredito.1";
        if (saudePct > 40) return "gui.intoxicantes.prontuario.veredito.2";
        if (saudePct > 20) return "gui.intoxicantes.prontuario.veredito.3";
        return "gui.intoxicantes.prontuario.veredito.4";
    }
}
