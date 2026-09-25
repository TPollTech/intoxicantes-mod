package com.intoxicantes;

import java.util.ArrayList;
import java.util.List;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.trading.ItemCost;
import net.minecraft.world.item.trading.MerchantOffer;
import net.minecraft.world.item.trading.MerchantOffers;

/** Canonical prices and daily quantities for the market and street dealer. */
final class TradeCatalog {
    private TradeCatalog() {}

    public record Entry(String id, Item item, int count, int price, int stock, int tier, boolean harvest) {
        public ItemStack stack() { return new ItemStack(item, count); }
    }

    static List<Entry> gago(int tier) {
        List<Entry> menu = new ArrayList<>();
        menu.add(new Entry("cerveja", IntoxicantesMod.CERVEJA, 1, 15, 8, 0, false));
        menu.add(new Entry("vinho", IntoxicantesMod.VINHO, 1, 25, 8, 0, false));
        menu.add(new Entry("hidromel", IntoxicantesMod.HIDROMEL, 1, 20, 8, 0, false));
        menu.add(new Entry("cachaca", IntoxicantesMod.CACHACA, 1, 35, 8, 0, false));
        menu.add(new Entry("rum", IntoxicantesMod.RUM, 1, 40, 8, 0, false));
        menu.add(new Entry("baseado", IntoxicantesMod.BASEADO, 1, 50, 4, 0, false));
        menu.add(new Entry("cocaina", IntoxicantesMod.COCAINA, 1, 60, 4, 0, false));
        menu.add(new Entry("lsd", IntoxicantesMod.LSD, 1, 64, 4, 0, false));
        menu.add(new Entry("po_estelar", IntoxicantesMod.PO_ESTELAR, 1, 60, 4, 0, false));
        menu.add(new Entry("extrato_cafeina", IntoxicantesMod.EXTRATO_CAFEINA, 1, 30, 6, 0, false));
        menu.add(new Entry("glass_bottle", Items.GLASS_BOTTLE, 3, 5, 16, 0, false));
        menu.add(new Entry("bread", Items.BREAD, 2, 10, 8, 0, false));
        menu.add(new Entry("semente_loupulo", IntoxicantesMod.SEMENTE_LOUPULO, 4, 4, 8, 0, false));
        menu.add(new Entry("semente_uva", IntoxicantesMod.SEMENTE_UVA, 4, 4, 8, 0, false));
        menu.add(new Entry("semente_cafe", IntoxicantesMod.SEMENTE_CAFE, 4, 4, 8, 0, false));
        menu.add(new Entry("semente_maconha", IntoxicantesMod.SEMENTE_MACONHA, 4, 4, 8, 0, false));
        menu.add(new Entry("semente_papoula", IntoxicantesMod.SEMENTE_PAPOULA, 4, 4, 8, 0, false));
        // v1.2.17 (balance): R$ 36 e 2/dia — a lâmpada virou INVESTIMENTO de
        // growshop (era R$ 24 com 4/dia: dava pra montar fazenda no dia 1)
        menu.add(new Entry("lampada_uv", IntoxicantesMod.LAMPADA_UV.asItem(), 1, 36, 2, 0, false));
        if (tier >= 1) menu.add(new Entry("cartuchos_fidelidade", IntoxicantesMod.CARTUCHO, 8, 60, 4, 1, false));
        if (tier >= 1) menu.add(new Entry("cartucho38_fidelidade", IntoxicantesMod.CARTUCHO_38, 8, 60, 4, 1, false));
        if (tier >= 2) menu.add(new Entry("hidromel_fidelidade", IntoxicantesMod.HIDROMEL, 4, 120, 2, 2, false));
        if (tier >= 3) menu.add(new Entry("escopeta_fidelidade", IntoxicantesMod.ESCOPETA, 1, 300, 1, 3, false));
        if (tier >= 3) menu.add(new Entry("revolver_fidelidade", IntoxicantesMod.REVOLVER, 1, 340, 1, 3, false));
        return List.copyOf(menu);
    }

    /**
     * v1.2.44 — PAUTA FIXA DO PONTO: o catalogo COMPLETO do traficante (7
     * produtos, preco de referencia do dia, estoque diario). A ordem E o
     * indice da tela (0..6) e do array EXCLUSIVOS abaixo.
     */
    static List<Entry> traficanteFixo() {
        return List.of(
                new Entry("maconha_seda", IntoxicantesMod.MACONHA_SEDA, 1, 12, 6, 0, false),
                new Entry("cocaina", IntoxicantesMod.COCAINA, 1, 42, 4, 0, false),
                new Entry("heroina", IntoxicantesMod.HEROINA, 1, 44, 4, 0, false),
                new Entry("lsd", IntoxicantesMod.LSD, 1, 48, 4, 0, false),
                new Entry("baseado", IntoxicantesMod.BASEADO, 1, 34, 6, 0, false),
                new Entry("opio", IntoxicantesMod.OPIO, 1, 14, 6, 0, false),
                new Entry("extrato_cafeina", IntoxicantesMod.EXTRATO_CAFEINA, 1, 20, 6, 0, false));
    }

    /** Indices (do traficanteFixo) que podem virar o LANCAMENTO DO DIA. */
    static final int[] EXCLUSIVOS = {0, 1, 2, 3, 4, 5};

    static List<Entry> harvests() {
        return List.of(
                new Entry("colheita_lupulo", IntoxicantesMod.LOUPULO_FRESCO, 8, 6, 12, 0, true),
                new Entry("colheita_uva", IntoxicantesMod.UVA, 8, 6, 12, 0, true),
                new Entry("ingrediente_trigo", Items.WHEAT, 8, 4, 12, 0, true),
                new Entry("ingrediente_cana", IntoxicantesMod.CANA_DE_ACUCAR, 8, 4, 12, 0, true),
                new Entry("ingrediente_mel", Items.HONEY_BOTTLE, 4, 8, 12, 0, true),
                new Entry("ingrediente_garrafas", Items.GLASS_BOTTLE, 8, 4, 12, 0, true));
    }

    static List<Entry> all() {
        List<Entry> all = new ArrayList<>(gago(3));
        all.addAll(harvests());
        return List.copyOf(all);
    }

    /**
     * Cotacao da RUA: oscila por DIA (seed = dia comercial), variando -30%..+40%
     * em volta da referencia. A mesma oferta custa o mesmo o dia inteiro (toda a
     * "rede" precifica junto, viu kkkk), muda a cada reposicao das 07h.
     * Combinado com o preco FIXO (e mais caro) do Gago, cria escolha real:
     * esperar a rua baixar, pagar no Esquinao ou arriscar o aleatorio.
     */
    static MerchantOffers traficante(RandomSource random, long seedDia) {
        List<StreetOffer> pool = new ArrayList<>(List.of(
                new StreetOffer(IntoxicantesMod.BASEADO, 34),
                new StreetOffer(IntoxicantesMod.COCAINA, 42),
                new StreetOffer(IntoxicantesMod.HEROINA, 44),
                new StreetOffer(IntoxicantesMod.LSD, 48),
                new StreetOffer(IntoxicantesMod.MACONHA_SEDA, 12),
                new StreetOffer(IntoxicantesMod.OPIO, 14),
                new StreetOffer(IntoxicantesMod.EXTRATO_CAFEINA, 20)));
        MerchantOffers offers = new MerchantOffers();
        for (int i = 0; i < 3; i++) {
            StreetOffer chosen = pool.remove(random.nextInt(pool.size()));
            // a rua oscila por dia, mas NUNCA passa do preco fixo do Esquinao:
            // a escolha do fregues e "rua barata e escassa" vs "Gago caro e
            // abastecido" — nunca " rua mais cara por azar do dado"
            int teto = precoGago(chosen.item()) - 1;
            int preco = Math.min(precoDoDia(seedDia, chosen.item().getDescriptionId(), chosen.price()), teto);
            sell(offers, chosen.item(), 1, Math.max(1, preco), 4);
        }
        return offers;
    }

    /** Preco fixo de varejo do Gago pra um produto (Integer.MAX_VALUE se ele nao vende). */
    private static int precoGago(Item item) {
        for (Entry e : gago(0)) {
            if (e.item() == item) {
                return e.price();
            }
        }
        return Integer.MAX_VALUE;
    }

    /**
     * Preco flutuante do dia pra um produto da rua (mesma seed pra qualquer NPC).
     * Cache por dia comercial: o refresh de 1s dos NPCs le isso toda hora — o
     * java.util.Random so roda UMA vez por produto/dia (pauta fixa do dia kkkk).
     */
    private static long pautaDia = Long.MIN_VALUE;
    private static final java.util.Map<String, Float> FATORES_DIA = new java.util.HashMap<>();

    static int precoDoDia(long seedDia, String produto, int base) {
        if (seedDia != pautaDia) {
            pautaDia = seedDia;
            FATORES_DIA.clear();
        }
        Float fator = FATORES_DIA.get(produto);
        if (fator == null) {
            fator = ModConfig.get().fatorCotacao(seedDia, produto);
            FATORES_DIA.put(produto, fator);
        }
        return Math.max(1, Math.round(base * fator));
    }



    private static void sell(MerchantOffers offers, Item item, int count, int price, int stock) {
        offers.add(new MerchantOffer(new ItemCost(IntoxicantesMod.REAL, price),
                new ItemStack(item, count), stock, 0, 0.0F));
    }

    private record StreetOffer(Item item, int price) {}
}
