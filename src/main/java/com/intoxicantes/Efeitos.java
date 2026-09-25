package com.intoxicantes;

import net.minecraft.core.Holder;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;

/**
 * v1.2.54 — OS EFEITOS ASSINATURA ("Viagens"): cada droga do mod tem SEU
 * efeito proprio, escalando por tier — nada de MobEffects vanilla generico
 * contando a historia de substancia.
 *
 *   T1 BASEADO -> TRANQUILO : morgue boa, estomago vazio, regen lenta
 *   T2 OPIO     -> MORNO     : casaco quente, corpo de chumbo, "nao doer nada"
 *   T2.5 HEROINA -> SONHO    : o nod, la e aqui ao mesmo tempo, dano desligado
 *   T3 COCAINA  -> OVERDRIVE : coracao de metralhadora, o mundo em fast-forward
 *   T4 LSD      -> VIAGEM    : a maldicao apropriada (visual no SaudeClient)
 *
 * A regra da casa: efeito de verdade, registrado, com cor e tick — o VISUAL
 * pesado (overlay, alucinacao, som) vive no SaudeClient, que liga quando o
 * jogador carrega um destes efeitos no HUD.
 */
public final class Efeitos {
    private Efeitos() {}

    /** T1: a morgue do baseado — corpo solto, fome da boa e regen lenta. */
    public static final Holder<MobEffect> TRANQUILO = registrar("tranquilo", new MobEffect(MobEffectCategory.NEUTRAL, 0x5D8F4A) {
        @Override
        public boolean applyEffectTick(ServerLevel level, LivingEntity quem, int amplificador) {
            // a barriga roda: maconha da fome de verdade (1 ponto por 3s)
            if (quem instanceof net.minecraft.world.entity.player.Player fregues
                    && quem.getRandom().nextInt(3) == 0) {
                fregues.getFoodData().eat(1, 0.1F);
            }
            return true;
        }

        @Override
        public boolean shouldApplyEffectTickThisTick(int duracaoRestante, int amplificador) {
            return duracaoRestante % 60 == 0; // 1x a cada 3 segundos
        }

        @Override
        public void onEffectStarted(LivingEntity quem, int amplificador) {
            // regen lenta por cima (a morgue cura o corpo devagar)
            quem.addEffect(new MobEffectInstance(MobEffects.REGENERATION, 200, 0, true, false));
        }
    });

    /** T2: o OPIO — casaco quente. "Nao doer nada" = resistencia + regen. */
    public static final Holder<MobEffect> MORNO = registrar("morno", new MobEffect(MobEffectCategory.NEUTRAL, 0xB5783C) {
        @Override
        public void onEffectStarted(LivingEntity quem, int amplificador) {
            quem.addEffect(new MobEffectInstance(MobEffects.RESISTANCE, 200, 0, true, false));
            quem.addEffect(new MobEffectInstance(MobEffects.REGENERATION, 200, 0, true, false));
            // corpo de chumbo: o teto pesa
            quem.addEffect(new MobEffectInstance(MobEffects.SLOWNESS, 200, 1, true, false));
        }
    });

    /** T2.5: a HEROINA — o nod. La e aqui ao mesmo tempo. */
    public static final Holder<MobEffect> SONHO = registrar("sonho", new MobEffect(MobEffectCategory.NEUTRAL, 0x7C5C9E) {
        @Override
        public boolean applyEffectTick(ServerLevel level, LivingEntity quem, int amplificador) {
            // o nod: o mundo balanca — afoga de leve e volta (sem dano real)
            if (quem.getRandom().nextInt(4) == 0 && quem.onGround()) {
                quem.push(0, -0.12, 0); // afunda no colchao
            }
            return true;
        }

        @Override
        public boolean shouldApplyEffectTickThisTick(int duracaoRestante, int amplificador) {
            return duracaoRestante % 20 == 0; // 1x por segundo
        }

        @Override
        public void onEffectStarted(LivingEntity quem, int amplificador) {
            quem.addEffect(new MobEffectInstance(MobEffects.RESISTANCE, 200, 1, true, false));
            quem.addEffect(new MobEffectInstance(MobEffects.REGENERATION, 300, 1, true, false));
            // a anestesia total: lento, sem pressa, sem mundo
            quem.addEffect(new MobEffectInstance(MobEffects.SLOWNESS, 300, 2, true, false));
        }
    });

    /** T3: a COCAINA — o coracao de metralhadora. O mundo em fast-forward. */
    public static final Holder<MobEffect> OVERDRIVE = registrar("overdrive", new MobEffect(MobEffectCategory.NEUTRAL, 0xE8E8E8) {
        @Override
        public boolean applyEffectTick(ServerLevel level, LivingEntity quem, int amplificador) {
            // o coracao cobra: comida queima rapido (energia nao e' de graca)
            if (quem instanceof net.minecraft.world.entity.player.Player fregues) {
                fregues.getFoodData().eat(0, -0.5F); // gasta saturacao de verdade
            }
            return true;
        }

        @Override
        public boolean shouldApplyEffectTickThisTick(int duracaoRestante, int amplificador) {
            return duracaoRestante % 40 == 0; // 1x cada 2 segundos
        }

        @Override
        public void onEffectStarted(LivingEntity quem, int amplificador) {
            // o burst em si (além do vanilla que o item já aplica)
            quem.addEffect(new MobEffectInstance(MobEffects.SPEED, 200, 0, true, false));
            quem.addEffect(new MobEffectInstance(MobEffects.HASTE, 200, 0, true, false));
        }
    });

    /** T4: o LSD — a viagem. O tick do efeito é o pulso da alma (visual no client). */
    public static final Holder<MobEffect> VIAGEM = registrar("viagem", new MobEffect(MobEffectCategory.NEUTRAL, 0xC040E0) {
        @Override
        public void onEffectStarted(LivingEntity quem, int amplificador) {
            // a visao abre: night vision dura a viagem inteira
            quem.addEffect(new MobEffectInstance(MobEffects.NIGHT_VISION, 600, 0, true, false));
        }
    });

    /** T6.5 — SINDROME DE ABSTINENCIA: o corpo cobrando a dose. */
    public static final Holder<MobEffect> ABSTINENCIA = registrar("abstinencia", new MobEffect(MobEffectCategory.HARMFUL, 0x4A4A5A) {
        @Override
        public boolean applyEffectTick(ServerLevel level, LivingEntity quem, int amplificador) {
            // o pulso do vicio: as vezes a mao treme (o visual do tremor e' no client,
            // que balanca a camera quando ve este efeito)
            if (quem.getRandom().nextInt(10) == 0) {
                quem.push((quem.getRandom().nextDouble() - 0.5) * 0.2, 0,
                        (quem.getRandom().nextDouble() - 0.5) * 0.2);
            }
            return true;
        }

        @Override
        public boolean shouldApplyEffectTickThisTick(int duracaoRestante, int amplificador) {
            return duracaoRestante % 40 == 0;
        }

        @Override
        public void onEffectStarted(LivingEntity quem, int amplificador) {
            // o combo: cabeca doendo, corpo sem forca, olhos fechando
            quem.addEffect(new MobEffectInstance(MobEffects.NAUSEA, 300, 0, true, false));
            quem.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, 300, 0, true, false));
            quem.addEffect(new MobEffectInstance(MobEffects.DARKNESS, 200, 0, true, false));
        }
    });

    /**
     * TOQUE na classe pra registrar (o static roda na primeira referência).
     * O IntoxicantesMod.onInitialize chama antes de qualquer uso — v1.2.54.
     */
    public static void carga() {
        // no-op: o static initializer da classe já registrou tudo
    }

    private static Holder<MobEffect> registrar(String nome, MobEffect efeito) {
        ResourceKey<MobEffect> key = ResourceKey.create(Registries.MOB_EFFECT,
                Identifier.fromNamespaceAndPath(IntoxicantesMod.MOD_ID, nome));
        return Registry.registerForHolder(BuiltInRegistries.MOB_EFFECT, key, efeito);
    }
}
