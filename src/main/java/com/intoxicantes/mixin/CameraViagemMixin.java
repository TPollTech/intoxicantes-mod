package com.intoxicantes.mixin;

import com.intoxicantes.SaudeClient;
import com.intoxicantes.SaudeVisionClient;

import net.minecraft.client.Camera;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * v1.2.54 — O FOV DAS VIAGENS: o OVERDRIVE pulsa com a batida do coração
 * (mundo aperta/solta ~200 bpm no pico), o VIAGEM respira lento, o SONHO
 * fecha a pálpebra 1,5%. Soma ao zoom do ADS das armas (a prioridade é do
 * ADS: o fator maior vence — não multiplicam juntos pra não enjooar).
 */
@Mixin(Camera.class)
public abstract class CameraViagemMixin {

    @Inject(method = "calculateFov", at = @At("RETURN"), cancellable = true)
    private void intoxicantes$fovViagem(float parcial, CallbackInfoReturnable<Float> cir) {
        float fatorViagem = SaudeVisionClient.fovViagem(parcial);
        if (fatorViagem != 1.0F) {
            cir.setReturnValue(cir.getReturnValueF() * fatorViagem);
        }
    }
}
