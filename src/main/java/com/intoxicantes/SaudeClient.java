package com.intoxicantes;

import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElementRegistry;

import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.Font;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Mth;
import net.minecraft.world.effect.MobEffectInstance;

import com.mojang.blaze3d.platform.InputConstants;

/**
 * v1.2.54 — CLIENT DA SAUDE. Tres pecas:
 *
 * 1. HUD CONTEXTUAL (o "HUDzinho"): sede (gota + barra) so quando < 100,
 *    vicio (agulha/estrela + barra) so quando > 0, abstinencia com TREMOR
 *    (o painel literalmente treme com o fregues). Nada na tela quando saudavel.
 * 2. OVERLAY DAS VIAGENS: tintas de tela e efeitos por viagem ativa — o
 *    MORNO tinta ambar, o SONHO dessatura, o OVERDRIVE pulsa o zoom com a
 *    "batida", o VIAGEM cicla hue, e a ABSTINENCIA escurece as bordas.
 *    (O psicodelico pesado do VIAGEM fica no SaudeVisionClient.)
 * 3. A TECLA H: manda o payload C2S; o servidor responde com o sync +
 *    abrirProntuario=true e a tela abre (receiver do payload, abaixo).
 */
public final class SaudeClient {
    private SaudeClient() {}

    // ==================================================== ESTADO RECEBIDO (S2C)

    private static volatile int hidratacao = SaudeData.HIDRATACAO_MAX;
    private static volatile int vicio;
    private static volatile String drogaVicio = "";
    private static volatile int estagioAbstinencia;
    private static volatile int alcool, erva, po, pilula; // histórico pro prontuário
    /** Última viagem ativa (nome do enum) + fim estimado (ms do client). */
    private static volatile String viagemAtiva = "";
    private static volatile int intensidadeViagem = 1;
    private static long fimDaViagem;

    // ==================================================== TECLA H

    private static net.minecraft.client.KeyMapping teclaProntuario;

    // paleta do HUD (a mesma vibe discreta do RelogioHud)
    private static final int COR_FUNDO = 0x5A0E0C0A;
    private static final int COR_BORDA = 0x5A3A342C;
    private static final int COR_AGUA = 0xFF4AA8E8;
    private static final int COR_AGUA_FRACA = 0xFF2A5A80;
    private static final int COR_VICIO = 0xFFC050C8;
    private static final int COR_VICIO_FRACA = 0xFF5A2A60;
    private static final int COR_ABSTINENCIA = 0xFFD8B048;

    public static void init() {
        // S2C: o sync periodico + o pedido de abrir o prontuario
        ClientPlayNetworking.registerGlobalReceiver(SaudeNetworking.SaudeSyncPayload.TYPE,
                (payload, ctx) -> ctx.client().execute(() -> {
                    hidratacao = payload.hidratacao();
                    vicio = payload.vicio();
                    drogaVicio = payload.drogaVicio();
                    estagioAbstinencia = payload.estagioAbstinencia();
                    alcool = payload.alcool();
                    erva = payload.erva();
                    po = payload.po();
                    pilula = payload.pilula();
                    if (payload.abrirProntuario()) {
                        Minecraft mc = Minecraft.getInstance();
                        mc.gui.setScreen(new ProntuarioScreen(new ProntuarioScreen.Dados(
                                payload.hidratacao(), payload.figado(), payload.pulmao(),
                                payload.estomago(), payload.vicio(), payload.drogaVicio(),
                                payload.alcool(), payload.erva(), payload.po(),
                                payload.pilula(), payload.curasSeguidas(),
                                payload.estagioAbstinencia())));
                    }
                }));
        // S2C: a viagem ligou
        ClientPlayNetworking.registerGlobalReceiver(SaudeNetworking.ViagemPayload.TYPE,
                (payload, ctx) -> ctx.client().execute(() -> {
                    viagemAtiva = payload.viagem();
                    intensidadeViagem = payload.intensidade();
                    fimDaViagem = System.currentTimeMillis() + payload.segundos() * 1000L;
                }));

        ClientTickEventsFimDeTick.registrar();

        // a TECLA H do prontuario (GAMEPLAY, na fileira das armas)
        teclaProntuario = net.fabricmc.fabric.api.client.keymapping.v1.KeyMappingHelper.registerKeyMapping(
                new net.minecraft.client.KeyMapping(
                        "key.intoxicantes.prontuario",
                        InputConstants.KEY_H,
                        net.minecraft.client.KeyMapping.Category.GAMEPLAY));

        // o HUDzinho (depois das armas, antes do relogio na ordem de pintura)
        HudElementRegistry.addLast(
                Identifier.fromNamespaceAndPath(IntoxicantesMod.MOD_ID, "saude_hud"),
                SaudeClient::desenharHud);
    }

    /** Ponte pro ClientTickEvents (classe separada pra manter o init limpo). */
    private static final class ClientTickEventsFimDeTick {
        static void registrar() {
            net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents
                    .END_CLIENT_TICK.register(mc -> {
                        while (teclaProntuario.consumeClick()) {
                            if (mc.player != null && mc.gui.screen() == null) {
                                ClientPlayNetworking.send(new SaudeNetworking.AbrirProntuarioPayload());
                            }
                        }
                        // viagem vencida: limpa (o servidor reenvia na proxima dose)
                        if (!viagemAtiva.isEmpty() && System.currentTimeMillis() > fimDaViagem) {
                            viagemAtiva = "";
                        }
                    });
        }
    }

    /** A tela H fecha com H também (o KeyMapping compara a KeyEvent). */
    static boolean teclaProntuarioMatches(net.minecraft.client.input.KeyEvent ev) {
        return teclaProntuario != null && teclaProntuario.matches(ev);
    }

    // ==================================================== HUD

    private static void desenharHud(GuiGraphicsExtractor g, DeltaTracker delta) {
        Minecraft mc = Minecraft.getInstance();
        LocalPlayer player = mc.player;
        if (player == null || mc.options.keyToggleGui.isDown()) {
            return;
        }
        if (mc.gui.screen() != null) {
            return;
        }
        // nada pra mostrar = nada desenhado (o HUDzinho invisivel da casa saudavel)
        if (hidratacao >= SaudeData.HIDRATACAO_MAX && vicio <= 0) {
            return;
        }

        int larguraPainel = 62;
        int alturaPainel = vicio > 0 ? 30 : 18;
        // canto INFERIOR ESQUERDO (o das armas é o direito; o relogio, o superior)
        int x = 4;
        int y = g.guiHeight() - alturaPainel - 4;
        // TREMOR da abstinencia: o painel segue a mao do fregues
        if (estagioAbstinencia > 0) {
            long t = System.currentTimeMillis();
            int forca = Math.min(2, estagioAbstinencia);
            x += (int) ((t / 60) % 2 == 0 ? forca : -forca);
            y += (int) ((t / 90) % 2 == 0 ? -1 : 1);
        }

        g.fill(x, y, x + larguraPainel, y + alturaPainel, COR_FUNDO);
        g.fill(x, y, x + larguraPainel, y + 1, COR_BORDA);
        g.fill(x, y + alturaPainel - 1, x + larguraPainel, y + alturaPainel, COR_BORDA);

        Font fonte = mc.font;
        int linha = y + 3;

        // --- sede: gota + barra (so quando < 100)
        if (hidratacao < SaudeData.HIDRATACAO_MAX) {
            desenharGota(g, x + 4, linha + 2, hidratacao <= 25);
            int barraX = x + 12;
            int barraW = larguraPainel - 16;
            g.fill(barraX, linha + 1, barraX + barraW, linha + 5, COR_AGUA_FRACA);
            g.fill(barraX, linha + 1, barraX + (int) (barraW * hidratacao / (float) SaudeData.HIDRATACAO_MAX),
                    linha + 5, hidratacao <= 25 ? 0xFFE06050 : COR_AGUA);
            g.text(fonte, String.valueOf(hidratacao), barraX + barraW + 1, linha, textoBarra(hidratacao), false);
            linha += 12;
        }

        // --- vicio: estrela roxa + barra + estagio da sindrome
        if (vicio > 0) {
            g.fill(x + 4, linha + 1, x + 8, linha + 5, COR_VICIO); // pastilha do vício
            int barraX = x + 12;
            int barraW = larguraPainel - 16;
            g.fill(barraX, linha + 1, barraX + barraW, linha + 5, COR_VICIO_FRACA);
            g.fill(barraX, linha + 1, barraX + (int) (barraW * vicio / (float) SaudeData.VICIO_MAX),
                    linha + 5, COR_VICIO);
            g.text(fonte, String.valueOf(vicio), barraX + barraW + 1, linha, textoBarra(vicio), false);
            linha += 12;

            if (estagioAbstinencia > 0) {
                String rotulo = Component.translatable(
                        "hud.intoxicantes.saude.abstinencia", estagioAbstinencia).getString();
                g.text(fonte, rotulo, x + 4, linha, COR_ABSTINENCIA, false);
            }
        }
    }

    private static int textoBarra(int valor) {
        return valor <= 25 ? 0xFFE06050 : 0xFFD8D2C4;
    }

    /** Gota de agua 5x5 de pixels (cheia ou piscando quando critica). */
    private static void desenharGota(GuiGraphicsExtractor g, int cx, int cy, boolean critica) {
        boolean piscar = critica && (System.currentTimeMillis() / 300) % 2 == 0;
        if (piscar) {
            return;
        }
        g.fill(cx + 1, cy, cx + 3, cy + 4, COR_AGUA);   // corpo
        g.fill(cx, cy + 1, cx + 4, cy + 3, COR_AGUA);   // ombros
        g.fill(cx + 2, cy + 1, cx + 3, cy + 2, 0xFF8CC8F0); // brilho
    }

    // ==================================================== LEITURAS (pra visao das viagens)

    /** Nome da viagem ativa ("" = nenhuma). O SaudeVisionClient le daqui. */
    public static String viagemAtiva() {
        return viagemAtiva;
    }

    public static int intensidadeViagem() {
        return intensidadeViagem;
    }

    /** A intensidade 0..1 da tinta do OVERDRIVE (pulso com a batida). */
    public static float pulsoOverdrive(long tempoMs) {
        double batida = Math.abs(Math.sin(tempoMs / 300.0)); // ~200 bpm
        return (float) batida;
    }

    /** Util: efeito ativo no jogador local (pra overlays que checam o HUD). */
    public static boolean temEfeito(LocalPlayer player, net.minecraft.core.Holder<net.minecraft.world.effect.MobEffect> efeito) {
        MobEffectInstance inst = player.getEffect(efeito);
        return inst != null;
    }

    /** Clamp util da casa. */
    static float clamp01(float v) {
        return Mth.clamp(v, 0.0F, 1.0F);
    }
}
