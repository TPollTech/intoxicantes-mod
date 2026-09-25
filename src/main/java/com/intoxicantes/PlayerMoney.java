package com.intoxicantes;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import net.minecraft.server.level.ServerPlayer;

import java.io.*;
import java.lang.reflect.Type;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Saldo de R$ do player.
 * Salva em arquivo JSON no diretorio do mundo (persistente).
 * Consulta: PlayerMoney.get(player) / PlayerMoney.set(player, valor)
 */
public final class PlayerMoney {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final Map<UUID, Integer> moneyMap = new HashMap<>();
    /** v1.2.44: divida do fiado com o traficante (persistente junto com o saldo). */
    private static final Map<UUID, Integer> dividaMap = new HashMap<>();
    private static File saveFile;
    private static File dividaFile;

    private PlayerMoney() {}

    /** Inicializa o sistema de dinheiro com o diretorio do mundo. */
    public static void init(File worldDir) {
        saveFile = new File(worldDir, "intoxicantes_money.json");
        dividaFile = new File(worldDir, "intoxicantes_fiado.json");
        load();
        loadDividas();
    }

    // ==================================================== FIADO (v1.2.44)

    /** Quanto o fregues deve pro traficante. */
    public static int getDivida(ServerPlayer player) {
        return dividaMap.getOrDefault(player.getUUID(), 0);
    }

    /** Soma divida (saturada no int). */
    public static void addDivida(ServerPlayer player, int quantia) {
        long novo = Math.min((long) getDivida(player) + (long) quantia, (long) Integer.MAX_VALUE);
        dividaMap.put(player.getUUID(), (int) novo);
        saveDividas();
    }

    /** Quita a divida (inteira ou ate o valor pago). Devolve o valor realmente pago. */
    public static int pagarDivida(ServerPlayer player, int quantia) {
        int devendo = getDivida(player);
        int pago = Math.min(Math.max(0, quantia), devendo);
        if (pago <= 0) return 0;
        dividaMap.put(player.getUUID(), devendo - pago);
        saveDividas();
        return pago;
    }

    public static int get(ServerPlayer player) {
        return moneyMap.getOrDefault(player.getUUID(), 0);
    }

    public static void set(ServerPlayer player, int valor) {
        moneyMap.put(player.getUUID(), Math.max(0, valor));
        save();
    }

    public static int add(ServerPlayer player, int quantia) {
        // saturado: sem isso, /pagar 2bi estoura o int e vira saldo NEGATIVO
        // (que set() zera) — dinheiro do nada pro bug do “saldo zerou sozinho”
        long novo = Math.min((long) get(player) + (long) quantia, (long) Integer.MAX_VALUE);
        set(player, (int) novo);
        return (int) novo;
    }

    public static boolean tem(ServerPlayer player, int quantia) {
        return get(player) >= quantia;
    }

    public static boolean subtrair(ServerPlayer player, int quantia) {
        if (!tem(player, quantia)) return false;
        add(player, -quantia);
        return true;
    }

    private static void load() {
        if (saveFile == null || !saveFile.exists()) return;
        try (Reader reader = Files.newBufferedReader(saveFile.toPath())) {
            Type mapType = new TypeToken<HashMap<UUID, Integer>>() {}.getType();
            HashMap<UUID, Integer> loaded = GSON.fromJson(reader, mapType);
            if (loaded != null) {
                moneyMap.clear();
                moneyMap.putAll(loaded);
            }
        } catch (Exception e) {
            IntoxicantesMod.LOGGER.warn("[Intoxicantes] Erro ao carregar dinheiro: {}", e.getMessage());
        }
    }

    private static void loadDividas() {
        if (dividaFile == null || !dividaFile.exists()) return;
        try (Reader reader = Files.newBufferedReader(dividaFile.toPath())) {
            Type mapType = new TypeToken<HashMap<UUID, Integer>>() {}.getType();
            HashMap<UUID, Integer> loaded = GSON.fromJson(reader, mapType);
            if (loaded != null) {
                dividaMap.clear();
                dividaMap.putAll(loaded);
            }
        } catch (Exception e) {
            IntoxicantesMod.LOGGER.warn("[Intoxicantes] Erro ao carregar fiado: {}", e.getMessage());
        }
    }

    private static void saveDividas() {
        if (dividaFile == null) return;
        try (Writer writer = Files.newBufferedWriter(dividaFile.toPath())) {
            GSON.toJson(dividaMap, writer);
        } catch (Exception e) {
            IntoxicantesMod.LOGGER.warn("[Intoxicantes] Erro ao salvar fiado: {}", e.getMessage());
        }
    }

    private static void save() {
        if (saveFile == null) return;
        try (Writer writer = Files.newBufferedWriter(saveFile.toPath())) {
            GSON.toJson(moneyMap, writer);
        } catch (Exception e) {
            IntoxicantesMod.LOGGER.warn("[Intoxicantes] Erro ao salvar dinheiro: {}", e.getMessage());
        }
    }
}
