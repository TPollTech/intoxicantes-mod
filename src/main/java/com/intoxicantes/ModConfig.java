package com.intoxicantes;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import net.minecraft.server.MinecraftServer;

/**
 * Config do mod: config/intoxicantes.json na raiz da instancia (irma de mods/),
 * criado com padroes no primeiro boot. Deixa calibrar escopeta, cotacao da rua
 * e UV sem recompilar. Valores fora da faixa saneada caem pro padrao — config
 * quebrado nao derruba o mod.
 */
public final class ModConfig {
    ModConfig() {} // package-private: o game test cria instancia com valores de calibracao

    // ==================================================== escopeta
    public int escopetaCooldownTicks = 20;      // 1s entre tiros
    public float escopetaDanoPorBalim = 3.0F;   // 8 balins = ate 24 de perto
    public int escopetaAlcanceMaximo = 24;      // blocos de efetivo
    public int escopetaBalins = 8;
    // v1.2.12: o Gago nao e' serrilha de boss — dano maximo do volley dele
    // contra alvos de vida maxima alta (withers, dragoes, golem...)
    public float escopetaCapDanoBoss = 6.0F;

    // ==================================================== cotacao da rua
    public boolean cotacaoFlutuante = true;     // false = Traficante fixo
    public float cotacaoMinima = 0.7F;          // -30%
    public float cotacaoMaxima = 1.4F;          // +40%

    // ==================================================== maturacao UV
    public float uvChanceMaturacao = 0.35F;     // chance por randomTick no UV max
    public boolean uvCueMaturacao = true;       // som + particulas ao amadurecer
    public int uvRaio = 2;                      // raio de acao da lampada (2 = 5x5x5)

    // ==================================================== embriaguez (v1.2.10)
    public int embriaguezDecaySegundos = 60;    // 1 nivel a cada 60s de vida limpa
    public int embriaguezLimiarFonar = 3;       // a partir daqui o chat sai embaralhado
    public int embriaguezLimiarHic = 5;         // a partir daqui vem soluço audivel
    public int embriaguezCap = 7;               // teto do nivel de embriaguez
    public boolean embriaguezTrancaTeto = true; // no teto o corpo devolve o gole (refluxo)
    // v1.2.13: o refinamento supimpa — ressaca, cambaleio e a vantagem do Gago
    public int embriaguezRessacaSegundos = 90;  // voltar a zero deixa de ressaca (0 = desligada)
    public boolean embriaguezCambaleio = true;  // no nivel do soluco: empurrao aleatorio
    public int embriaguezMarkup = 10;           // % a mais por nivel acima do limiar de fala (teto 30)

    private static ModConfig instancia;

    public static ModConfig get() {
        if (instancia == null) {
            instancia = new ModConfig(); // fallback seguro antes do load
        }
        return instancia;
    }

    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

    /** Chamado no boot do servidor: carrega ou cria config/intoxicantes.json. */
    public static void init(MinecraftServer server) {
        File mundo = server.getWorldPath(net.minecraft.world.level.storage.LevelResource.ROOT).toFile();
        // .../saves/<mundo> -> .../saves -> raiz da instancia (irma de mods/)
        File raiz = mundo.getParentFile() != null && mundo.getParentFile().getParentFile() != null
                ? mundo.getParentFile().getParentFile()
                : mundo.getParentFile();
        File arquivo = new File(raiz, "config" + File.separator + "intoxicantes.json");
        if (arquivo.exists()) {
            try {
                ModConfig lida = GSON.fromJson(Files.readString(arquivo.toPath()), ModConfig.class);
                if (lida != null) {
                    instancia = lida;
                    instancia.sanear();
                }
            } catch (Exception e) {
                System.err.println("[Intoxicantes] config quebrado, usando padroes: " + e);
                instancia = new ModConfig();
            }
        } else {
            instancia = new ModConfig();
            try {
                Files.createDirectories(arquivo.getParentFile().toPath());
                Files.writeString(arquivo.toPath(), GSON.toJson(instancia));
            } catch (IOException e) {
                System.err.println("[Intoxicantes] nao consegui criar o config: " + e);
            }
        }
    }

    /** Valores fora de faixa voltam pro padrao. */
    private void sanear() {
        escopetaCooldownTicks = (int) clamp(escopetaCooldownTicks, 5, 100);
        escopetaDanoPorBalim = (float) clamp(escopetaDanoPorBalim, 0.5, 20.0);
        escopetaAlcanceMaximo = (int) clamp(escopetaAlcanceMaximo, 4, 64);
        escopetaBalins = (int) clamp(escopetaBalins, 1, 16);
        escopetaCapDanoBoss = (float) clamp(escopetaCapDanoBoss, 1.0, 50.0);
        cotacaoMinima = (float) clamp(cotacaoMinima, 0.3, 1.0);
        cotacaoMaxima = (float) clamp(cotacaoMaxima, 1.0, 3.0);
        uvChanceMaturacao = (float) clamp(uvChanceMaturacao, 0.05, 1.0);
        uvRaio = (int) clamp(uvRaio, 1, 6);
        embriaguezDecaySegundos = (int) clamp(embriaguezDecaySegundos, 10, 600);
        embriaguezLimiarFonar = (int) clamp(embriaguezLimiarFonar, 1, 20);
        embriaguezLimiarHic = (int) clamp(embriaguezLimiarHic, 2, 20);
        embriaguezCap = (int) clamp(embriaguezCap, 2, 20);
        embriaguezRessacaSegundos = (int) clamp(embriaguezRessacaSegundos, 0, 600);
        embriaguezMarkup = (int) clamp(embriaguezMarkup, 0, 30);
        // coerencia entre limiares: fala embaralha antes do soluço, ninguem passa do teto
        embriaguezLimiarHic = Math.max(embriaguezLimiarHic, embriaguezLimiarFonar + 1);
        embriaguezLimiarFonar = Math.min(embriaguezLimiarFonar, embriaguezCap);
        embriaguezLimiarHic = Math.min(embriaguezLimiarHic, embriaguezCap + 1);
    }

    private static double clamp(double v, double min, double max) {
        return Math.max(min, Math.min(max, v));
    }

    // ==================================================== helpers de leitura
    /** Fator de cotacao do dia (1.0 se a flutuacao estiver desligada). */
    public float fatorCotacao(long seedDia, String produto) {
        if (!cotacaoFlutuante) {
            return 1.0F;
        }
        java.util.Random r = new java.util.Random(seedDia * 31 + produto.hashCode());
        return cotacaoMinima + r.nextFloat() * (cotacaoMaxima - cotacaoMinima);
    }
}
