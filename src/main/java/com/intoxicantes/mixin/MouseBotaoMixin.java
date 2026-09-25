package com.intoxicantes.mixin;

import com.intoxicantes.ArmasClient;
import net.minecraft.client.Minecraft;
import net.minecraft.client.MouseHandler;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * v1.2.53 — CONTROLES DE FPS (padrão CoD/Battlefield) com as armas na mão:
 * - BOTÃO ESQUERDO = ATIRA (o vanilla trata clique esquerdo como quebrar/
 *   atacar — antes o tiro morava no direito, colidindo com o "usar");
 * - BOTÃO DIREITO = MIRAR (segurar = ADS/iron sight; soltar = quadril).
 *
 * O mixin intercepta onButton ANTES do jogo: com arma de fogo na mão, o
 * esquerdo vira o gatilho (manda atirar via ArmasClient) e o direito é
 * engolido (não coloca bloco / não usa item) e liga o ADS. Sem arma na mão
 * o vanilla segue intacto. A mira NÃO é mais o SHIFT (que encurvalava e
 * rouba o sneak — agora o shift volta a ser só agachar).
 */
@Mixin(MouseHandler.class)
public abstract class MouseBotaoMixin {

    @Inject(method = "onButton", at = @At("HEAD"), cancellable = true)
    private void intoxicantes$armaNoGatilho(long janela, net.minecraft.client.input.MouseButtonInfo info,
            int acao, CallbackInfo ci) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.gui.screen() != null || !ArmasClient.segurandoArma()) {
            return;
        }
        int botao = info.button();
        if (botao == 0) { // GLFW_MOUSE_BUTTON_LEFT
            // pressionou (acao 1): puxa o gatilho; soltar (0) é inerte (a arma
            // é semi-auto — um clique, um tiro). Engolimos o evento inteiro:
            // nenhum bloco quebra, nenhuma entidade apanha de soco.
            if (acao == 1) {
                ArmasClient.gatilhoPuxado();
            }
            ci.cancel();
            return;
        }
        if (botao == 1) { // GLFW_MOUSE_BUTTON_RIGHT
            // EXCEÇÃO: a crosshair numa ENTIDADE (Gago, Traficante, Juça…) —
            // o direito passa intacto pra abrir o menu/interagir em vez de
            // mirar (mirar em NPC não faz sentido; perder o menu, sim).
            if (mc.hitResult != null && mc.hitResult.getType() == net.minecraft.world.phys.HitResult.Type.ENTITY) {
                return;
            }
            // o ADS é CONTÍNUO (segurar): o client só espelha o estado — o
            // ArmasClient lê isRightPressed() por frame. Engolir o evento
            // impede o "usar" vanilla (colocar bloco etc.).
            ci.cancel();
        }
    }
}
