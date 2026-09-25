package com.intoxicantes;

import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElementRegistry;

import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.resources.Identifier;

/**
 * v1.2.53 — O RELÓGIO DE SEMPRE NO HUD: canto superior esquerdo, discreto —
 * um painel escuro translúcido com o ícone sol/lua (o ciclo do dia em 1
 * olhada), a hora do mundo (HH:MM) e o dia atual. Sufocado no canto, não
 * cobre hotbar, crosshair nem o HUD das armas (que vive embaixo à direita).
 *
 * A hora usa a fórmula vanilla: tick 0 = 06:00 (amanhecer), 1000 ticks = 1h.
 * Desenha com fills (sem textura nova) — ícone de sol é um ponto com raios,
 * o de lua um crescente de dois quadrados.
 */
public final class RelogioHud {

    // paleta discreta: fundo escuro translúcido, texto claro, âmbar pra hora
    private static final int COR_FUNDO = 0x5A0E0C0A;
    private static final int COR_BORDA = 0x5A3A342C;
    private static final int COR_HORA = 0xFFE8D8B0;
    private static final int COR_DIA = 0xFF8A8274;
    private static final int COR_SOL = 0xFFE8C84A;
    private static final int COR_LUA = 0xFFC8CCD8;

    private RelogioHud() {}

    public static void init() {
        HudElementRegistry.addLast(
                Identifier.fromNamespaceAndPath(IntoxicantesMod.MOD_ID, "relogio_hud"),
                RelogioHud::desenhar);
    }

    private static void desenhar(GuiGraphicsExtractor g, DeltaTracker delta) {
        Minecraft mc = Minecraft.getInstance();
        LocalPlayer player = mc.player;
        if (player == null || mc.level == null || mc.options.keyToggleGui.isDown()) {
            return;
        }
        // esconde com qualquer tela aberta (chat, inventário, guia) — discreto
        if (mc.gui.screen() != null) {
            return;
        }

        long tempo = mc.level.getOverworldClockTime();
        long dia = tempo / 24000L + 1L;
        long ticks = ((tempo % 24000L) + 24000L) % 24000L;
        int horas = (int) ((ticks / 1000L + 6L) % 24L);
        int minutos = (int) ((ticks % 1000L) * 60L / 1000L);
        boolean diaLuz = ticks < 12500L; // sol até ~18:30, depois lua

        String hora = String.format("%02d:%02d", horas, minutos);
        String rotuloDia = "Dia " + dia;

        Font fonte = mc.font;
        int larguraHora = fonte.width(hora);
        int larguraDia = fonte.width(rotuloDia);
        int largura = 10 + larguraHora + 6 + larguraDia + 5;
        int altura = 12;
        int x = 4;
        int y = 4;

        g.fill(x, y, x + largura, y + altura, COR_FUNDO);
        g.fill(x, y, x + largura, y + 1, COR_BORDA);           // borda fina
        g.fill(x, y + altura - 1, x + largura, y + altura, COR_BORDA);

        desenharCorpo(g, x + 2, y + 6, diaLuz);

        int tx = x + 10;
        g.text(fonte, hora, tx, y + 2, COR_HORA, false);
        g.text(fonte, rotuloDia, tx + larguraHora + 6, y + 3, COR_DIA, false);
    }

    /**
     * O corpo celeste de 7px: SOL = núcleo + 4 raios; LUA = crescente
     * (quadrado claro com recorte escuro no canto superior direito).
     */
    private static void desenharCorpo(GuiGraphicsExtractor g, int cx, int cy, boolean sol) {
        if (sol) {
            g.fill(cx + 2, cy - 2, cx + 4, cy + 2, COR_SOL);   // núcleo
            g.fill(cx, cy - 1, cx + 1, cy + 1, COR_SOL);        // raios
            g.fill(cx + 5, cy - 1, cx + 6, cy + 1, COR_SOL);
            g.fill(cx + 2, cy - 4, cx + 4, cy - 3, COR_SOL);
            g.fill(cx + 2, cy + 3, cx + 4, cy + 4, COR_SOL);
        } else {
            g.fill(cx + 1, cy - 2, cx + 5, cy + 2, COR_LUA);    // disco
            g.fill(cx + 3, cy - 3, cx + 6, cy, COR_FUNDO);      // recorte = crescente
        }
    }
}
