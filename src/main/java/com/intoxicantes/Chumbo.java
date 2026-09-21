package com.intoxicantes;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;

/**
 * O chumbo da 12 (v1.2.11) — vale pros DOIS atiradores (item do player e o NPC).
 *
 * O CASO DO WITHER: no 26.3 os i-frames mudaram de lugar — o cooldown do dano
 * vive no campo PUBLICO damageCooldownTime e, com ele ativo, so entra dano
 * MAIOR que o ultimo hit (lastHurt). O setInvulnerableTime(0) legado NAO limpa
 * esse campo. Resultado: os 8 balins do mesmo tamanho viravam 1 hit, e o
 * volley seguinte do Gago (dano igual, cooldown re-ativado pelo 1o balim)
 * quicava inteiro — contra um chefe de 300 de vida, "o gago nao consegue dar
 * hit no wither". Chumbo.aplicar zera o cooldown REAL a cada balim.
 */
final class Chumbo {

    private Chumbo() {}

    /**
     * Aplica o dano do balim com a blindagem padrao da 12: escudo Invulnerable
     * de save antigo derrubado (o hurt engolia SEM LOG) e os i-frames do 26.3
     * zerados — o tiro inteiro e' UM hit e todo volley entra limpo.
     */
    static void aplicar(ServerLevel level, LivingEntity vitima, DamageSource fonte, float dano) {
        if (vitima.isInvulnerable()) {
            vitima.setPermanentlyInvulnerable(false);
            IntoxicantesMod.LOGGER.warn("[Escopeta] vitima {} estava Invulnerable — escudo derrubado",
                    vitima.getName().getString());
        }
        vitima.damageCooldownTime = 0; // o cooldown REAL do 26.3
        if (vitima.getInvulnerableTime() > 0) {
            vitima.setInvulnerableTime(0); // campo legado, por garantia
        }
        vitima.hurtServer(level, fonte, dano);
    }
}
