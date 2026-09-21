package com.intoxicantes;

import java.util.HashSet;
import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.util.ProblemReporter;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.trading.MerchantOffer;
import net.minecraft.world.item.trading.MerchantOffers;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.storage.TagValueInput;
import net.minecraft.world.level.storage.TagValueOutput;

/** Exercises vanilla offers, actual NPC save data and the daily stock policy. */
public class CommerceGameTest {
    @GameTest
    public void immaturePlantsCannotGenerateSellableHarvest(GameTestHelper helper) {
        var crops = java.util.List.of(IntoxicantesMod.LOUPULO_PLANT, IntoxicantesMod.UVA_PLANT,
                IntoxicantesMod.CAFE_PLANT, IntoxicantesMod.MACONHA_PLANT, IntoxicantesMod.PAPOULA_PLANT);
        var seeds = java.util.List.of(IntoxicantesMod.SEMENTE_LOUPULO, IntoxicantesMod.SEMENTE_UVA,
                IntoxicantesMod.SEMENTE_CAFE, IntoxicantesMod.SEMENTE_MACONHA, IntoxicantesMod.SEMENTE_PAPOULA);
        for (int i = 0; i < crops.size(); i++) {
            var crop = (UvCropBlock) crops.get(i);
            var product = java.util.List.of(IntoxicantesMod.LOUPULO_FRESCO, IntoxicantesMod.UVA,
                    IntoxicantesMod.CAFE_VERDE, IntoxicantesMod.MACONHA_SEDA, IntoxicantesMod.OPIO).get(i);
            var state = crop.defaultBlockState().setValue(UvCropBlock.AGE, 0).setValue(UvCropBlock.UV_AGE, 0);
            var drops = net.minecraft.world.level.block.Block.getDrops(state, helper.getLevel(), helper.absolutePos(BlockPos.ZERO), null);
            helper.assertTrue(drops.stream().noneMatch(stack -> stack.is(product)), "Breaking seedlings must not create sellable products");
            final var seed = seeds.get(i);
            helper.assertTrue(drops.stream().filter(stack -> stack.is(seed)).mapToInt(ItemStack::getCount).sum() == 1,
                    "A seedling returns only its planted seed");
            state = state.setValue(UvCropBlock.AGE, 4);
            helper.assertTrue(crop.isRandomlyTicking(state), "Fully grown crop must still tick for UV ripening");
            drops = net.minecraft.world.level.block.Block.getDrops(state, helper.getLevel(), helper.absolutePos(BlockPos.ZERO), null);
            helper.assertTrue(drops.stream().filter(stack -> stack.is(product)).mapToInt(ItemStack::getCount).sum() == 1,
                    "Grown but unripe crop yields one product");
            state = state.setValue(UvCropBlock.UV_AGE, 3);
            helper.assertFalse(crop.isRandomlyTicking(state), "Fully ripe crop stops ticking");
            drops = net.minecraft.world.level.block.Block.getDrops(state, helper.getLevel(), helper.absolutePos(BlockPos.ZERO), null);
            // v1.2.16: regra UNIFORME (gerador = disk = teste): esperar a
            // maturação vale 3x o produto pra TODAS as culturas (antes o lúpulo
            // tinha 4 afinado à mão — a regeneração das textures expôs a deriva)
            helper.assertTrue(drops.stream().filter(stack -> stack.is(product)).mapToInt(ItemStack::getCount).sum() == 3,
                    "Ripe crops yield the uniform 3x bonus (seed always returns)");
        }
        helper.succeed();
    }

    @GameTest
    public void sharedQuotaAndInvalidCustomersCannotDuplicateMoney(GameTestHelper helper) {
        GagoEntity merchant = helper.spawn(IntoxicantesMod.GAGO, new BlockPos(1, 1, 1));
        merchant.refreshTradeStock();
        var player = helper.makeMockServerPlayerInLevel();
        player.absSnapTo(merchant.getX(), merchant.getY(), merchant.getZ());
        player.getInventory().clearContent();
        player.getInventory().setItem(0, new ItemStack(IntoxicantesMod.UVA, 16));
        PlayerMoney.set(player, 0);
        helper.assertTrue(MarketTransactions.trade(player, merchant, 1, true) == MarketTransactions.Result.INVALID,
                "Trade without an active merchant session is rejected");
        merchant.setTradingPlayer(player);
        var entry = TradeCatalog.harvests().get(1);
        for (int i = 0; i < entry.stock() - 1; i++) merchant.marketInventory().consume(entry);
        helper.assertTrue(MarketTransactions.trade(player, merchant, 1, true) == MarketTransactions.Result.OK,
                "Last remaining quota can be used");
        helper.assertTrue(MarketTransactions.trade(player, merchant, 1, true) == MarketTransactions.Result.OUT_OF_STOCK,
                "Repeated request cannot exceed shared quota");
        helper.assertTrue(PlayerMoney.get(player) == 6 && player.getInventory().countItem(IntoxicantesMod.UVA) == 8,
                "Rejected repeat preserves money and remaining crop");
        helper.assertTrue(MarketTransactions.trade(player, merchant, -1, false) == MarketTransactions.Result.INVALID,
                "Negative index rejected");
        helper.assertTrue(MarketTransactions.trade(player, merchant, 99, false) == MarketTransactions.Result.INVALID,
                "Locked or nonexistent offers cannot be bought");
        merchant.setTradingPlayer(null);
        helper.succeed();
    }

    @GameTest
    public void harvestPaysExactlyAndRejectsShortPayment(GameTestHelper helper) {
        GagoEntity merchant = helper.spawn(IntoxicantesMod.GAGO, new BlockPos(1, 1, 1));
        merchant.refreshTradeStock();
        var player = helper.makeMockServerPlayerInLevel();
        player.absSnapTo(merchant.getX(), merchant.getY(), merchant.getZ());
        merchant.setTradingPlayer(player);
        PlayerMoney.set(player, 100);
        player.getInventory().clearContent();
        player.getInventory().setItem(0, new ItemStack(IntoxicantesMod.LOUPULO_FRESCO, 7));
        helper.assertTrue(MarketTransactions.trade(player, merchant, 0, true) == MarketTransactions.Result.NO_HARVEST,
                "Insufficient harvest must not pay");
        helper.assertTrue(PlayerMoney.get(player) == 100 && player.getInventory().getItem(0).getCount() == 7,
                "Rejected trade must preserve cash and harvest");
        player.getInventory().setItem(0, new ItemStack(IntoxicantesMod.LOUPULO_FRESCO, 4));
        player.getInventory().setItem(1, new ItemStack(IntoxicantesMod.LOUPULO_FRESCO, 8));
        helper.assertTrue(MarketTransactions.trade(player, merchant, 0, true) == MarketTransactions.Result.OK,
                "Full harvest is purchased across multiple inventory slots");
        helper.assertTrue(PlayerMoney.get(player) == 106, "Eight hops pay exactly six reais");
        helper.assertTrue(MarketTransactions.countHarvest(player, TradeCatalog.harvests().get(0)) == 4,
                "Exactly eight crops consumed");
        helper.assertTrue(MarketTransactions.trade(player, merchant, 0, false) == MarketTransactions.Result.OK,
                "Earned cash can buy a beverage");
        helper.assertTrue(PlayerMoney.get(player) == 91, "Beverage deducts the displayed price");
        helper.assertTrue(player.getInventory().countItem(IntoxicantesMod.CERVEJA) == 1,
                "Purchase delivers the item");
        merchant.setTradingPlayer(null);
        helper.succeed();
    }

    @GameTest
    public void beverageIngredientsPayCorrectBatchesWithoutResaleProfit(GameTestHelper helper) {
        var expected = java.util.List.of(IntoxicantesMod.LOUPULO_FRESCO, IntoxicantesMod.UVA,
                net.minecraft.world.item.Items.WHEAT, IntoxicantesMod.CANA_DE_ACUCAR,
                net.minecraft.world.item.Items.HONEY_BOTTLE, net.minecraft.world.item.Items.GLASS_BOTTLE);
        var ingredients = TradeCatalog.harvests();
        helper.assertTrue(ingredients.stream().map(TradeCatalog.Entry::item).toList().equals(expected),
                "Gago buys exactly the six ingredients used by his beverage recipes");
        var merchant = helper.spawn(IntoxicantesMod.GAGO, new BlockPos(1, 1, 1));
        merchant.refreshTradeStock();
        var player = helper.makeMockServerPlayerInLevel();
        player.absSnapTo(merchant.getX(), merchant.getY(), merchant.getZ());
        merchant.setTradingPlayer(player);
        PlayerMoney.set(player, 0);
        int balance = 0;
        for (int i = 0; i < ingredients.size(); i++) {
            var entry = ingredients.get(i);
            player.getInventory().clearContent();
            player.getInventory().setItem(0, new ItemStack(entry.item(), entry.count() - 1));
            helper.assertTrue(MarketTransactions.trade(player, merchant, i, true) == MarketTransactions.Result.NO_HARVEST,
                    "Each ingredient requires its own full batch");
            player.getInventory().setItem(0, entry.stack());
            helper.assertTrue(MarketTransactions.trade(player, merchant, i, true) == MarketTransactions.Result.OK,
                    "Each ingredient can be sold");
            balance += entry.price();
            helper.assertTrue(PlayerMoney.get(player) == balance && player.getInventory().countItem(entry.item()) == 0,
                    "Exact ingredients and containers are handed over, exact price paid");
            helper.assertTrue(merchant.marketInventory().remaining(entry) == entry.stock() - 1, "Only one quota consumed");
            for (var retail : TradeCatalog.gago(3)) if (retail.item() == entry.item()) {
                int discounted = Math.max(1, (int) Math.floor(retail.price() * 0.7));
                helper.assertTrue(discounted * entry.count() > entry.price() * retail.count(),
                        "Even maximum loyalty discount cannot profit from buying bottles to resell");
            }
        }
        merchant.setTradingPlayer(null);
        helper.succeed();
    }

    @GameTest
    public void everyPaymentFitsAndStreetPricesAreLower(GameTestHelper helper) {
        var shop = TradeCatalog.gago(0);
        helper.assertTrue(shop.size() == 18, "Retail includes seeds and UV reinvestment");
        HashSet<Item> seen = new HashSet<>();
        for (int seed = 0; seed < 100; seed++) {
            MerchantOffers street = TradeCatalog.traficante(RandomSource.create(seed), seed);
            helper.assertTrue(street.size() == 3, "Street dealer has exactly three offers");
            HashSet<Item> dailyItems = new HashSet<>();
            for (MerchantOffer offer : street) {
                Item item = offer.getResult().getItem();
                helper.assertTrue(dailyItems.add(item), "No duplicate street offer");
                seen.add(item);
                helper.assertTrue(offer.getMaxUses() == 4, "Street stock is scarce");
                for (var retail : shop) {
                    if (retail.item() == item) {
                        helper.assertTrue(offer.getCostA().getCount() < retail.price(),
                                "Street price must be lower than the market price");
                    }

                }
                for (var harvest : TradeCatalog.harvests()) {
                    if (harvest.item() == item) helper.assertTrue(offer.getCostA().getCount() * harvest.count() > harvest.price(),
                            "Buying to resell must not create free money");
                }
            }
        }
        helper.assertTrue(seen.size() == 7, "Rotation includes the entire street catalog");
        helper.succeed();
    }

    @GameTest
    public void restockAtSevenIsDeferredWhileTradingAndIgnoresRewind(GameTestHelper helper) {
        DailyTradeStock stock = new DailyTradeStock();
        MerchantOffers offers = stock.refresh(null, 1000, false, () -> TradeCatalog.traficante(RandomSource.create(7), 7L));
        offers.get(0).setToOutOfStock();
        helper.assertTrue(stock.refresh(offers, 24999, false, () -> TradeCatalog.traficante(RandomSource.create(7), 7L)) == offers,
                "No early refill before 07:00");
        helper.assertTrue(stock.refresh(offers, 25000, true, () -> TradeCatalog.traficante(RandomSource.create(7), 7L)) == offers,
                "An open trade keeps its offers stable");
        MerchantOffers next = stock.refresh(offers, 25000, false, () -> TradeCatalog.traficante(RandomSource.create(7), 7L));
        helper.assertFalse(next.get(0).isOutOfStock(), "Stock returns at 07:00 after trading ends");
        next.get(0).setToOutOfStock();
        helper.assertTrue(stock.refresh(next, 0, false, () -> TradeCatalog.traficante(RandomSource.create(7), 7L)) == next,
                "Rewinding time does not replenish stock");
        helper.assertTrue(stock.refresh(next, 25000, false, () -> TradeCatalog.traficante(RandomSource.create(7), 7L)) == next,
                "Repeating the same morning does not replenish stock");
        helper.assertTrue(DailyTradeStock.tradingDay(999) == -1 && DailyTradeStock.tradingDay(1000) == 0,
                "First-day boundary is exactly 07:00");
        helper.succeed();
    }

    @GameTest
    public void oldOffersMigrateWithoutRestoringUsedStock(GameTestHelper helper) {
        MerchantOffers old = new MerchantOffers();
        old.add(new MerchantOffer(new net.minecraft.world.item.trading.ItemCost(IntoxicantesMod.REAL, 15),
                new ItemStack(IntoxicantesMod.CERVEJA), 99, 0, 0.05F));
        for (int i = 0; i < 9; i++) old.get(0).increaseUses();
        MarketInventory stock = new MarketInventory();
        stock.refresh(1000, false, old);
        helper.assertTrue(stock.remaining(TradeCatalog.gago(0).get(0)) == 0, "Migration preserves spent stock");
        stock.refresh(1200, false, old);
        helper.assertTrue(stock.remaining(TradeCatalog.gago(0).get(0)) == 0, "Migration only runs once");
        stock.refresh(25000, true, old);
        helper.assertTrue(stock.remaining(TradeCatalog.gago(0).get(0)) == 0, "Open shop cannot restock");
        stock.refresh(25000, false, old);
        helper.assertTrue(stock.remaining(TradeCatalog.gago(0).get(0)) == 8, "Restock restores daily quota");
        helper.succeed();
    }

    @GameTest
    public void npcSaveReloadPreservesStockAndMerchantsAreIndependent(GameTestHelper helper) {
        GagoEntity first = helper.spawn(IntoxicantesMod.GAGO, new BlockPos(1, 1, 1));
        GagoEntity second = helper.spawn(IntoxicantesMod.GAGO, new BlockPos(3, 1, 1));
        first.refreshTradeStock();
        second.refreshTradeStock();
        var beer = TradeCatalog.gago(0).get(0);
        for (int i = 0; i < beer.stock(); i++) first.marketInventory().consume(beer);
        helper.assertTrue(second.marketInventory().remaining(beer) == 8, "Each merchant owns its stock");
        var output = TagValueOutput.createWithContext(ProblemReporter.DISCARDING, helper.getLevel().registryAccess());
        first.saveWithoutId(output);
        var input = TagValueInput.create(ProblemReporter.DISCARDING, helper.getLevel().registryAccess(), output.buildResult());
        second.load(input);
        second.refreshTradeStock();
        helper.assertTrue(second.marketInventory().remaining(beer) == 0, "Reload must not refill used stock");
        TraficanteEntity dealer = helper.spawn(IntoxicantesMod.TRAFICANTE, new BlockPos(2, 1, 3));
        dealer.refreshTradeStock();
        dealer.getOffers().get(0).increaseUses();
        var streetOutput = TagValueOutput.createWithContext(ProblemReporter.DISCARDING, helper.getLevel().registryAccess());
        dealer.saveWithoutId(streetOutput);
        Item originalItem = dealer.getOffers().get(0).getResult().getItem();
        dealer.load(TagValueInput.create(ProblemReporter.DISCARDING, helper.getLevel().registryAccess(), streetOutput.buildResult()));
        dealer.refreshTradeStock();
        helper.assertTrue(dealer.getOffers().get(0).getUses() == 1 && dealer.getOffers().get(0).getResult().is(originalItem),
                "Street selection and purchases survive reload");
        helper.succeed();
    }

    /** Registra no mapa estatico sem tocar em arquivo (get().set() gravaria no JSON). */
    private static void setSaldoTeste(ServerPlayer player, int valor) {
        try {
            var campo = PlayerMoney.class.getDeclaredField("moneyMap");
            campo.setAccessible(true);
            @SuppressWarnings("unchecked")
            java.util.Map<java.util.UUID, Integer> mapa = (java.util.Map<java.util.UUID, Integer>) campo.get(null);
            mapa.put(player.getUUID(), valor);
        } catch (ReflectiveOperationException e) {
            throw new AssertionError("moneyMap inacessivel", e);
        }
    }

    @GameTest
    public void balanceSaturatesAtCeilingAndNeverGoesNegative(GameTestHelper helper) {
        var player = helper.makeMockServerPlayerInLevel();
        // v1.2.2: 2.000.000.000 + 2.000.000.000 em int = saldo NEGATIVO que o set zerava
        setSaldoTeste(player, 2_000_000_000);
        PlayerMoney.add(player, 2_000_000_000);
        helper.assertTrue(PlayerMoney.get(player) == Integer.MAX_VALUE,
                "Overflow of add saturates at Integer.MAX_VALUE");
        PlayerMoney.add(player, 100);
        helper.assertTrue(PlayerMoney.get(player) == Integer.MAX_VALUE,
                "Ceiling holds after repeated additions");
        // /pagar (subtrair + add no destinatario) no limite: quem paga nunca fica negativo
        setSaldoTeste(player, 1_000_000_000);
        PlayerMoney.subtrair(player, 1_500_000_000);
        helper.assertTrue(PlayerMoney.get(player) == 1_000_000_000,
                "Payer without funds is untouched by subtrair");
        PlayerMoney.add(player, Integer.MAX_VALUE);
        helper.assertTrue(PlayerMoney.get(player) == Integer.MAX_VALUE,
                "Receiver of an enormous payment saturates instead of wrapping");
        PlayerMoney.add(player, -50);
        helper.assertTrue(PlayerMoney.get(player) == Integer.MAX_VALUE - 50,
                "Negative adjustment still works under saturation");
        helper.succeed();
    }

    /** Config de teste: troca a instancia em memoria (o config real no disco fica intocado). */
    private static void configDeTeste(ModConfig alteracoes) {
        try {
            var instancia = ModConfig.class.getDeclaredField("instancia");
            instancia.setAccessible(true);
            instancia.set(null, alteracoes);
        } catch (ReflectiveOperationException e) {
            throw new AssertionError("instancia inacessivel", e);
        }
    }

    /** Igual ao mock da GameTestHelper (perfil + connection embutida + placeNewPlayer),
     *  mas com gameMode() = SURVIVAL: o mock vanilla e' CREATIVE hardcoded na
     *  subclasse anonima, inalteravel por reflexao ou mutacao de campos. */
    private static ServerPlayer vitimaSobrevivente(GameTestHelper helper) {
        var level = helper.getLevel();
        var perfil = new com.mojang.authlib.GameProfile(java.util.UUID.randomUUID(), "vitima-teste");
        var cookie = net.minecraft.server.network.CommonListenerCookie.createInitial(perfil, false);
        ServerPlayer vitima = new ServerPlayer(level.getServer(), level, perfil, cookie.clientInformation()) {
            @Override
            public net.minecraft.world.level.GameType gameMode() {
                return net.minecraft.world.level.GameType.SURVIVAL;
            }
        };
        var conexao = new net.minecraft.network.Connection(net.minecraft.network.protocol.PacketFlow.SERVERBOUND);
        new io.netty.channel.embedded.EmbeddedChannel(conexao);
        level.getServer().getPlayerList().placeNewPlayer(conexao, vitima, cookie);
        // Complete the vanilla load handshake: unloaded clients are immune to damage.
        vitima.connection.handleAcceptPlayerLoad(new net.minecraft.network.protocol.game.ServerboundPlayerLoadedPacket());
        return vitima;
    }

    /** Injeta compras de fidelidade no mapa estatico sem tocar em arquivo. */
    private static void fidelidadeTeste(ServerPlayer player, int compras) {
        try {
            var campo = FidelidadeData.class.getDeclaredField("compras");
            campo.setAccessible(true);
            @SuppressWarnings("unchecked")
            java.util.Map<java.util.UUID, Integer> mapa = (java.util.Map<java.util.UUID, Integer>) campo.get(null);
            mapa.put(player.getUUID(), compras);
        } catch (ReflectiveOperationException e) {
            throw new AssertionError("compras inacessivel", e);
        }
    }

    /** Conquista concedida? (holder inexistente = dados de progressao quebrados) */
    private static boolean progresso(GameTestHelper helper, ServerPlayer player,
            net.minecraft.resources.Identifier id) {
        var holder = player.level().getServer().getAdvancements().get(id);
        helper.assertTrue(holder != null, "Advancement exists in the datapack: " + id);
        return holder != null && player.getAdvancements().getOrStartProgress(holder).isDone();
    }

    @GameTest
    public void loyaltyCardTiersAndAdvancementsProgressCorrectly(GameTestHelper helper) {
        var player = helper.makeMockServerPlayerInLevel();
        fidelidadeTeste(player, 0);
        helper.assertTrue(FidelidadeData.getTier(player) == 0 && FidelidadeData.descontoDe(player) == 0,
                "New customer starts on tier 0 with no discount");
        // 4 compras: ainda Fregues comum (a RAIZ sai ja na 1a compra; os tiers sao 5/15/30)
        for (int i = 0; i < 4; i++) {
            FidelidadeData.registrarCompra(player);
        }
        helper.assertTrue(FidelidadeData.getTier(player) == 0,
                "Four purchases stay on tier 0");
        helper.assertTrue(progresso(helper, player, Progressoes.RAIZ),
                "The very first purchase grants the root advancement");
        helper.assertFalse(progresso(helper, player, Progressoes.FREGUES_DA_ESQUINA),
                "Four purchases do not reach Fregues da Esquina");
        // 5a compra: Fregues da Esquina (tier 1, 5%) + raiz
        FidelidadeData.registrarCompra(player);
        helper.assertTrue(FidelidadeData.getTier(player) == 1 && FidelidadeData.descontoDe(player) == 5,
                "Five purchases reach tier 1 with 5% discount");
        helper.assertTrue(progresso(helper, player, Progressoes.RAIZ),
                "First purchase grants the root advancement");
        helper.assertTrue(progresso(helper, player, Progressoes.FREGUES_DA_ESQUINA),
                "Tier 1 grants Fregues da Esquina");
        helper.assertFalse(progresso(helper, player, Progressoes.DONO_DA_ESQUINA),
                "Tier 3 advancement is still locked");
        // pulo direto pro Dono da Esquina (30 compras, 15%)
        fidelidadeTeste(player, 29);
        FidelidadeData.registrarCompra(player);
        helper.assertTrue(FidelidadeData.getTier(player) == 3 && FidelidadeData.descontoDe(player) == 15,
                "Thirty purchases reach tier 3 with 15% discount");
        helper.assertTrue(progresso(helper, player, Progressoes.DONO_DA_ESQUINA),
                "Tier 3 grants Dono da Esquina");
        helper.assertTrue(FidelidadeData.precoComDesconto(player, 100) == 85,
                "Tier 3 discount prices a 100-real item at 85");
        helper.assertTrue(FidelidadeData.precoComDesconto(player, 1) == 1,
                "Discount never drops the price below one real");
        fidelidadeTeste(player, 0); // nao polui o JSON do mundo de teste
        helper.succeed();
    }

    @GameTest
    public void perfectHarvestAdvancementOnlyAtFullRipeness(GameTestHelper helper) {
        var player = helper.makeMockServerPlayerInLevel();
        var crop = (UvCropBlock) IntoxicantesMod.MACONHA_PLANT;
        // imatura (AGE 2, UV 0): colher NAO concede
        helper.setBlock(new BlockPos(3, 1, 1), net.minecraft.world.level.block.Blocks.FARMLAND);
        var verde = crop.defaultBlockState().setValue(UvCropBlock.AGE, 2).setValue(UvCropBlock.UV_AGE, 0);
        helper.setBlock(new BlockPos(3, 2, 1), verde);
        crop.playerWillDestroy(helper.getLevel(), helper.absolutePos(new BlockPos(3, 2, 1)), verde, player);
        helper.assertFalse(progresso(helper, player, Progressoes.COLHEITA_PERFEITA),
                "Harvesting an unripe crop grants nothing");
        // no ponto (AGE 4 + UV 3): concede
        helper.setBlock(new BlockPos(1, 1, 1), net.minecraft.world.level.block.Blocks.FARMLAND);
        var perfeita = crop.defaultBlockState().setValue(UvCropBlock.AGE, 4).setValue(UvCropBlock.UV_AGE, 3);
        helper.setBlock(new BlockPos(1, 2, 1), perfeita);
        crop.playerWillDestroy(helper.getLevel(), helper.absolutePos(new BlockPos(1, 2, 1)), perfeita, player);
        helper.assertTrue(progresso(helper, player, Progressoes.COLHEITA_PERFEITA),
                "Harvesting at AGE 4 + UV 3 grants Colheita Perfeita");
        helper.succeed();
    }

    @GameTest
    public void gagoEscalatesAgainstJointSmokeAtCounter(GameTestHelper helper) {
        GagoEntity gago = helper.spawn(IntoxicantesMod.GAGO, new BlockPos(1, 1, 1));
        // vitima sobrevivente (o mock do helper e CREATIVE hardcoded): fumante
        var fumante = vitimaSobrevivente(helper);
        fumante.absSnapTo(gago.getX() + 1.5, gago.getY(), gago.getZ());
        fumante.getInventory().clearContent();
        fumante.getInventory().setItem(0, new ItemStack(IntoxicantesMod.BASEADO));
        fumante.getFoodData().setFoodLevel(20);
        fumante.setItemInHand(net.minecraft.world.InteractionHand.MAIN_HAND,
                new ItemStack(IntoxicantesMod.BASEADO));
        fumante.startUsingItem(net.minecraft.world.InteractionHand.MAIN_HAND);
        helper.assertTrue(fumante.isUsingItem(), "Victim is puffing the joint next to the counter");

        // 1a conversa: reclama (fumaca.1), nao apaga ainda
        gago.fiscalizarFumaca();
        helper.assertTrue(gago.fumacaConversas == 1 && gago.cooldownFumaca > 0,
                "First smoke talk scolds and starts the throttle");
        helper.assertTrue(fumante.isUsingItem(), "First offense only scolds");

        // throttle ativo: sem fala nova mesmo com fumaca rolando
        gago.fiscalizarFumaca();
        helper.assertTrue(gago.fumacaConversas == 1,
                "Throttle blocks spam while cooling down");

        // 2a conversa (throttle expirado): reclama de novo, ainda nao apaga
        gago.cooldownFumaca = 0;
        gago.fiscalizarFumaca();
        helper.assertTrue(gago.fumacaConversas == 2, "Second talk escalates the scolding");
        helper.assertTrue(fumante.isUsingItem(), "Second talk still only scolds");

        // 3a conversa: apaga na mao — uso interrompido + 10s de cooldown
        gago.cooldownFumaca = 0;
        gago.fiscalizarFumaca();
        helper.assertTrue(!fumante.isUsingItem(), "Third talk SNATCHES the joint (use interrupted)");
        helper.assertTrue(fumante.getCooldowns().isOnCooldown(new ItemStack(IntoxicantesMod.BASEADO)),
                "Snuffed joint goes on a 10s cooldown");
        helper.assertTrue(gago.fumacaConversas == 0,
                "Counter resets after the snuff so the next joint starts over");

        // com a fumaça sumida (uso interrompido), contador nao reaparece do nada
        resetFumacaTeste(gago);
        helper.succeed();
    }

    /** Zera o estado de fumaca do Gago (isolamento entre testes). */
    private static void resetFumacaTeste(GagoEntity gago) {
        gago.fumacaConversas = 0;
        gago.cooldownFumaca = 0;
    }

    /** Uma passada de 1s com o relógio do mock alinhado na janela (20 ticks). */
    private static void passaSegundoTeste(ServerPlayer player) {
        player.tickCount = 20;
        Embriaguez.tickJogador(player);
    }

    /** Injeta a sobriedade acumulada (reflection, padrão dos outros helpers). */
    private static void sobriedadeTeste(ServerPlayer player, int segundos) {
        try {
            var campo = Embriaguez.class.getDeclaredField("SOBRIEDADE");
            campo.setAccessible(true);
            @SuppressWarnings("unchecked")
            java.util.Map<java.util.UUID, Integer> mapa =
                    (java.util.Map<java.util.UUID, Integer>) campo.get(null);
            mapa.put(player.getUUID(), segundos); // a passada soma +1 e atinge o alvo
        } catch (ReflectiveOperationException e) {
            throw new AssertionError("SOBRIEDADE inacessivel", e);
        }
    }

    @GameTest
    public void drunkennessDosesScrambleChatAndDecayOverTime(GameTestHelper helper) {
        var player = helper.makeMockServerPlayerInLevel();
        Embriaguez.setNivelTeste(player, 0);

        // dose de cachaça (3): sobe pra 3 = LIMIAR FONAR
        Embriaguez.beber(player, 3);
        helper.assertTrue(Embriaguez.nivel(player) == 3,
                "One cachaça dose reaches the slurred speech threshold");

        // fala limpa abaixo do limiar, fonada no limiar
        String fala = "Vou levar duas cervejas";
        helper.assertTrue(Embriaguez.fonar(fala, 2).equals(fala),
                "Below threshold speech stays clean");
        String fonada = Embriaguez.fonar(fala, 3);
        helper.assertTrue(!fonada.equals(fala),
                "At the threshold the speech comes out scrambled");
        helper.assertTrue(fonada.length() >= fala.length(),
                "Slurring stretches or swaps but never loses content");
        // nivel alto embaralha MAIS que o limiar
        helper.assertTrue(!Embriaguez.fonar(fala, 6).equals(fonada),
                "Higher level scrambles harder than the threshold");
        // v1.2.10: caído soluça NA FALA (*hic!* intercalado)
        helper.assertTrue(Embriaguez.fonar(fala, 6).contains("*hic!*"),
                "Passed-out level punctuates the speech with hiccups");

        // decay: 60 passadas de 1s derrubam 1 nivel (decay padrao = 60s)
        Embriaguez.setNivelTeste(player, 3);
        for (int i = 0; i < 60; i++) {
            passaSegundoTeste(player);
        }
        helper.assertTrue(Embriaguez.nivel(player) == 2,
                "A clean minute of sobriety drops one level");

        // dose nova zera a conta da sobriedade (o gole reseta o relógio)
        Embriaguez.beber(player, 1);
        helper.assertTrue(Embriaguez.nivel(player) == 3,
                "New dose tops the level back up");

        Embriaguez.setNivelTeste(player, 0);
        helper.succeed();
    }

    @GameTest
    public void drunkennessCapRefluxAndPersistenceRoundTrip(GameTestHelper helper) {
        ModConfig atual = ModConfig.get();
        ModConfig teste = new ModConfig();
        teste.embriaguezDecaySegundos = 10;
        teste.embriaguezLimiarFonar = 3;
        teste.embriaguezLimiarHic = 5;
        teste.embriaguezCap = 4;
        teste.embriaguezTrancaTeto = true;
        configDeTeste(teste);
        try {
            var player = helper.makeMockServerPlayerInLevel();
            Embriaguez.setNivelTeste(player, 0);

            // aponta a persistencia pra um arquivo temporario (game test nao tem mundo)
            java.io.File tmp;
            try {
                tmp = java.io.File.createTempFile("intoxicantes_embriaguez", ".json");
            } catch (java.io.IOException e) {
                throw new AssertionError("temp file indisponivel", e);
            }
            tmp.deleteOnExit();
            Embriaguez.initTeste(tmp);

            // config manda: 4 doses (2+2) enchem o TETO CONFIGURADO (não um 7 fixo)
            Embriaguez.beber(player, 2);
            Embriaguez.beber(player, 2);
            helper.assertTrue(Embriaguez.nivel(player) == 4,
                    "Cap comes from the config, not a hardcoded 7");

            // 5a dose no teto: REFLUXO — o corpo devolve e o nivel fica no cap
            Embriaguez.beber(player, 3);
            helper.assertTrue(Embriaguez.nivel(player) == 4,
                    "Ceiling with the cork on stays at the cap (reflux)");

            // persistencia: estado vai pro arquivo, memoria zera, load reconstroi
            Embriaguez.save();
            Embriaguez.setNivelTeste(player, 0);
            helper.assertTrue(Embriaguez.nivel(player) == 0,
                    "Memory wiped to prove the reload does the work");
            Embriaguez.loadTeste();
            helper.assertTrue(Embriaguez.nivel(player) == 4,
                    "Drunkenness survives save/load round-trip (relog/restart)");

            // decay usa o config: 10s acumulados + uma passada de 1s = 1 nivel
            sobriedadeTeste(player, teste.embriaguezDecaySegundos - 1);
            passaSegundoTeste(player);
            helper.assertTrue(Embriaguez.nivel(player) == 3,
                    "Decay interval comes from the config (one level per 10s here)");

            Embriaguez.setNivelTeste(player, 0);
        } finally {
            configDeTeste(atual);
        }
        helper.succeed();
    }

    @GameTest
    public void gagoRidesADrunkCustomerAtTheCounter(GameTestHelper helper) {
        GagoEntity gago = helper.spawn(IntoxicantesMod.GAGO, new BlockPos(1, 1, 1));
        var player = helper.makeMockServerPlayerInLevel();
        player.absSnapTo(gago.getX() + 1.5, gago.getY(), gago.getZ());

        // doses sem cruzar o limiar: nada de deboche
        Embriaguez.beber(player, 1);
        Embriaguez.beber(player, 1);
        helper.assertTrue(gago.cooldownZoacao == 0,
                "Staying under the threshold earns no mockery");

        // cruzou o limiar da fala bêbada: o deboche vem SOZINHO (beber -> Gago)
        Embriaguez.beber(player, 1);
        helper.assertTrue(gago.cooldownZoacao > 0,
                "Crossing the slurred threshold pulls the Gago's mockery automatically");

        // throttle: deboche recente bloqueia repetição (cooldown nao reseta pra 600)
        gago.cooldownZoacao = 590;
        gago.zoFreguesBebado(player);
        helper.assertTrue(gago.cooldownZoacao <= 590,
                "Throttle blocks mockery spam while cooling down");

        // fregues sóbrio de novo: gatilho direto não debocha, estado limpo
        Embriaguez.setNivelTeste(player, 0);
        gago.cooldownZoacao = 0;
        gago.zoFreguesBebado(player);
        helper.assertTrue(gago.cooldownZoacao == 0,
                "A sober customer gets no mockery even from a direct trigger");
        helper.succeed();
    }

    @GameTest
    public void gagoShotgunFollowsConfigNotHardcodedValues(GameTestHelper helper) {
        ModConfig atual = ModConfig.get();
        ModConfig teste = new ModConfig();
        teste.escopetaCooldownTicks = 10;
        teste.escopetaDanoPorBalim = 0.5F;
        teste.escopetaBalins = 1;
        teste.escopetaAlcanceMaximo = 12;
        configDeTeste(teste);
        try {
            // canAttack() recusa alvo Player em dificuldade PACIFICA: o servidor de
            // game test nasce peaceful — NORMAL pra o Gago poder ficar puto kkkk
            helper.getLevel().getServer().setDifficulty(net.minecraft.world.Difficulty.NORMAL, true);
            GagoEntity gago = helper.spawn(IntoxicantesMod.GAGO, new BlockPos(1, 1, 1));
            // makeMockServerPlayerInLevel() devolve um mock cuja subclasse anonima
            // (GameTestHelper$3) SOBRESCREVE gameMode() pra devolver CREATIVE
            // hardcoded no bytecode: nenhuma mutacao no ServerPlayerGameMode muda
            // isso, e asValidTarget() descarta alvo criativo. Criamos um irmao do
            // mock do vanilla em modo sobrevivencia — mesmo padrao do helper.
            var vitima = vitimaSobrevivente(helper);
            vitima.absSnapTo(gago.getX() + 2, gago.getY(), gago.getZ());
            vitima.setPermanentlyInvulnerable(false);
            vitima.setInvulnerableTime(0);
            vitima.getAbilities().invulnerable = false; // Player.hurtServer recusa dano se true
            float vidaAntes = vitima.getHealth();
            gago.enraivecer(vitima);
            helper.assertTrue(gago.getTarget() == vitima,
                "Angered Gago targets the offender (survival mock)");
            helper.assertTrue(gago.getItemBySlot(net.minecraft.world.entity.EquipmentSlot.MAINHAND).is(IntoxicantesMod.ESCOPETA),
                "Angered Gago draws the shotgun");
            // tiro direto via package-private: o tick do NPC dispararia sozinho,
            // mas o teste precisa do cooldown EXATO pra provar que vem do config
            gago.atirarEscopeta(vitima);
            helper.assertTrue(gago.cooldownTiro == teste.escopetaCooldownTicks,
                "Shotgun cooldown comes from the config (was hardcoded 60)");
            var area = helper.getLevel().getEntitiesOfClass(net.minecraft.world.entity.LivingEntity.class,
                    gago.getBoundingBox().inflate(teste.escopetaAlcanceMaximo));
            helper.assertTrue(vitima.getHealth() < vidaAntes,
                "Diagnostico dano: vida=" + vitima.getHealth() + "/" + vidaAntes
                    + " naArea=" + area.contains(vitima) + " totalArea=" + area.size()
                    + " dist=" + gago.distanceTo(vitima)
                    + " invEnt=" + vitima.isInvulnerable()
                    + " invAbi=" + vitima.getAbilities().invulnerable
                    + " invTemp=" + vitima.getInvulnerableTime()
                    + " morto=" + vitima.isDeadOrDying());
            // restabelece: a raiva passou, sem arma na mao, config original de volta
            gago.acalmar(vitima);
            helper.assertTrue(gago.getItemBySlot(net.minecraft.world.entity.EquipmentSlot.MAINHAND).isEmpty(),
                "Calming the Gago puts the shotgun away");
        } finally {
            configDeTeste(atual);
        }
        helper.succeed();
    }

    /**
     * v1.2.11 — O CASO DO WITHER, reproducao direta do report: o Gago atira
     * volleys de balins IGUAIS (3.0) num wither blindado (vida <= 50%). Com os
     * i-frames do 26.3 (damageCooldownTime + regra "so dano maior"), o 2o balim
     * em diante quicava — e o volley seguinte tambem. Chumbo.aplicar zera o
     * cooldown REAL: os dois volleys completos entram.
     */
    @GameTest
    public void gagoBuckshotLandsOnTheWitherThroughItsShield(GameTestHelper helper) {
        helper.getLevel().getServer().setDifficulty(net.minecraft.world.Difficulty.NORMAL, true);
        var atirador = helper.makeMockServerPlayerInLevel();
        var tipoWither = helper.getLevel().registryAccess()
                .lookupOrThrow(net.minecraft.core.registries.Registries.ENTITY_TYPE)
                .getValue(net.minecraft.resources.Identifier.withDefaultNamespace("wither"));
        var wither = (net.minecraft.world.entity.boss.wither.WitherBoss) helper.spawn(
                tipoWither, new BlockPos(2, 1, 1));
        // blindado: vida <= 50% (isPowered) — o estado do report
        wither.setHealth(wither.getMaxHealth() / 2.0F);
        var fonte = atirador.damageSources().mobAttack(atirador);
        float base = wither.getHealth();

        // volley 1: 8 "balins" de 3.0 — SEM o fix, so o 1o entraria (~2.7 de dano
        // apos a armadura do wither); com o fix, os 8 entram (~21.6)
        for (int i = 0; i < 8; i++) {
            Chumbo.aplicar(helper.getLevel(), wither, fonte, 3.0F);
        }
        float danoV1 = base - wither.getHealth();
        helper.assertTrue(danoV1 > 16.0F,
                "Volley 1 lands far beyond a single pellet (armor-taxed): " + danoV1);

        // volley 2, mesma coisa (1s depois no jogo): espelha o primeiro volley
        float antesV2 = wither.getHealth();
        for (int i = 0; i < 8; i++) {
            Chumbo.aplicar(helper.getLevel(), wither, fonte, 3.0F);
        }
        float danoV2 = antesV2 - wither.getHealth();
        helper.assertTrue(Math.abs(danoV2 - danoV1) < 0.5F,
                "Volley 2 mirrors volley 1 (the reported Gago-vs-wither case): " + danoV2);
        helper.succeed();
    }

    /**
     * v1.2.11 — O CASO DO WITHER: os i-frames do 26.3 vivem em damageCooldownTime
     * (campo publico) e a regra e' "cooldown ativo so aceita dano MAIOR que o
     * ultimo". O setInvulnerableTime(0) legado NAO limpa esse campo — os 8 balins
     * viravam 1 e o volley seguinte do Gago quicava. Prova: com o reset correto,
     * dois hits IGUAIS seguidos entram (impossivel com o reset velho).
     */
    @GameTest
    public void shotgunIFrameResetUsesThe263CooldownField(GameTestHelper helper) {
        var atirador = helper.makeMockServerPlayerInLevel();
        var tipoZumbi = helper.getLevel().registryAccess()
                .lookupOrThrow(net.minecraft.core.registries.Registries.ENTITY_TYPE)
                .getValue(net.minecraft.resources.Identifier.withDefaultNamespace("zombie"));
        var alvo = (net.minecraft.world.entity.monster.zombie.Zombie) helper.spawn(
                tipoZumbi, new BlockPos(2, 1, 1));
        var fonte = atirador.damageSources().playerAttack(atirador);

        // 1o hit: entra (cooldown do alvo esta zerado)
        Chumbo.aplicar(helper.getLevel(), alvo, fonte, 4.0F);
        float aposPrimeiro = alvo.getHealth();
        helper.assertTrue(alvo.damageCooldownTime > 0,
                "A landed hit starts the 26.3 damage cooldown");

        // 2o hit IGUAL logo em seguida (i-frames NO ar, dano nao-maior):
        // sem o reset correto, este hit QUICARIA (a regra nova exige dano maior)
        Chumbo.aplicar(helper.getLevel(), alvo, fonte, 4.0F);
        helper.assertTrue(alvo.getHealth() < aposPrimeiro,
                "Second equal pellet lands because aplicar clears damageCooldownTime");

        // e o reset do campo legado sozinho NAO basta — prova do porque do fix
        var alvo2 = (net.minecraft.world.entity.monster.zombie.Zombie) helper.spawn(
                tipoZumbi, new BlockPos(4, 1, 1));
        alvo2.hurtServer(helper.getLevel(), fonte, 4.0F);
        float aposPrimeiro2 = alvo2.getHealth();
        alvo2.setInvulnerableTime(0); // o jeito VELHO de resetar (nao toca o campo novo)
        boolean quicou = !alvo2.hurtServer(helper.getLevel(), fonte, 4.0F);
        helper.assertTrue(quicou,
                "Legacy setInvulnerableTime(0) does NOT clear the 26.3 cooldown (the bug)");
        helper.assertTrue(alvo2.getHealth() == aposPrimeiro2,
                "Rejected second hit leaves health untouched");
        helper.succeed();
    }

    /** Injeta o remaining de uso (simula o timer do gole correndo). */
    private static void usoRemainingTeste(ServerPlayer player, int ticks) {
        try {
            var campo = Embriaguez.class.getDeclaredField("USO_REMAINING");
            campo.setAccessible(true);
            @SuppressWarnings("unchecked")
            java.util.Map<java.util.UUID, Integer> mapa =
                    (java.util.Map<java.util.UUID, Integer>) campo.get(null);
            mapa.put(player.getUUID(), ticks);
        } catch (ReflectiveOperationException e) {
            throw new AssertionError("USO_REMAINING inacessivel", e);
        }
    }

    /**
     * v1.2.12 — O BUG DO CLIQUE SEM BEBER: dose so conta se o gole COMPLETOU
     * (remaining chegou ao fim). Interromper no meio (soltar o botao, trocar de
     * item, ter o baseado apagado pelo Gago) NAO conta mais — e a cura com
     * cafeina desce 2 niveis. v1.2.14: o consumivel de teste e' CERVEJA — so
     * ALCOOL alimenta o medidor (erva/pó/comprimido nao emborracham).
     */
    @GameTest
    public void interruptedSipDoesNotDoseAndCaffeineCures(GameTestHelper helper) {
        var bebedor = vitimaSobrevivente(helper);
        bebedor.getInventory().clearContent();
        bebedor.getInventory().setItem(0, new ItemStack(IntoxicantesMod.CERVEJA));
        bebedor.getFoodData().setFoodLevel(20);

        // --- interrompido no meio: NADA conta (o bug do beta tester)
        bebedor.setItemInHand(net.minecraft.world.InteractionHand.MAIN_HAND,
                new ItemStack(IntoxicantesMod.CERVEJA));
        bebedor.startUsingItem(net.minecraft.world.InteractionHand.MAIN_HAND);
        passaSegundoTeste(bebedor); // registra EM_USO + USO_REMAINING
        bebedor.releaseUsingItem(); // soltou o botao no meio do puxao
        passaSegundoTeste(bebedor); // detecta o fim do uso com remaining alto
        helper.assertTrue(Embriaguez.nivel(bebedor) == 0,
                "Interrupted sip counts NOTHING (was a dose before the fix)");

        // --- gole completo: dose entra (remaining <= 1 quando o uso acaba);
        // cerveja vale dose 2 (medidor agora e' SO de alcool)
        bebedor.startUsingItem(net.minecraft.world.InteractionHand.MAIN_HAND);
        passaSegundoTeste(bebedor); // registra o inicio
        usoRemainingTeste(bebedor, 1); // simula o timer chegando ao fim
        bebedor.releaseUsingItem(); // fim do uso (completo)
        passaSegundoTeste(bebedor);
        helper.assertTrue(Embriaguez.nivel(bebedor) == 2,
                "A COMPLETED beer counts its dose (alcohol-only meter)");

        // --- v1.2.14: ERVA NAO EMBORRACHA — um baseado COMPLETO não sobe o
        // medidor (a fumaça e os efeitos próprios do item cuidam do resto)
        bebedor.getInventory().setItem(0, new ItemStack(IntoxicantesMod.BASEADO));
        bebedor.setItemInHand(net.minecraft.world.InteractionHand.MAIN_HAND,
                new ItemStack(IntoxicantesMod.BASEADO));
        bebedor.startUsingItem(net.minecraft.world.InteractionHand.MAIN_HAND);
        passaSegundoTeste(bebedor);
        usoRemainingTeste(bebedor, 1);
        bebedor.releaseUsingItem();
        passaSegundoTeste(bebedor);
        helper.assertTrue(Embriaguez.nivel(bebedor) == 2,
                "A completed JOINT does NOT feed the alcohol meter (report: 'bêbado com maconha não faz sentido')");

        // --- a cura da bebedeira: cafeina derruba 2 niveis
        Embriaguez.setNivelTeste(bebedor, 5);
        bebedor.getInventory().setItem(0, new ItemStack(IntoxicantesMod.EXTRATO_CAFEINA));
        bebedor.setItemInHand(net.minecraft.world.InteractionHand.MAIN_HAND,
                new ItemStack(IntoxicantesMod.EXTRATO_CAFEINA));
        bebedor.startUsingItem(net.minecraft.world.InteractionHand.MAIN_HAND);
        passaSegundoTeste(bebedor);
        usoRemainingTeste(bebedor, 1);
        bebedor.releaseUsingItem();
        passaSegundoTeste(bebedor);
        helper.assertTrue(Embriaguez.nivel(bebedor) == 3,
                "Caffeine extract cures 2 drunkenness levels");

        // --- piso zero: bêbado de nível 1 vira limpo, nunca negativo
        Embriaguez.setNivelTeste(bebedor, 1);
        bebedor.startUsingItem(net.minecraft.world.InteractionHand.MAIN_HAND);
        passaSegundoTeste(bebedor);
        usoRemainingTeste(bebedor, 1);
        bebedor.releaseUsingItem();
        passaSegundoTeste(bebedor);
        helper.assertTrue(Embriaguez.nivel(bebedor) == 0,
                "Cure floors at zero");
        helper.succeed();
    }

    /**
     * v1.2.12 — O GAGO NAO E' SERRILHA DE BOSS: contra alvo de vida maxima alta
     * o volley dele capA (config, padrao 6.0) — com tiro por segundo e municao
     * infinita, ele derretia um wither. Fregues comum leva dano pleno.
     */
    @GameTest
    public void gagoVolleyIsCappedAgainstBossesButNotCustomers(GameTestHelper helper) {
        helper.getLevel().getServer().setDifficulty(net.minecraft.world.Difficulty.NORMAL, true);
        GagoEntity gago = helper.spawn(IntoxicantesMod.GAGO, new BlockPos(1, 1, 1));
        var tipoWither = helper.getLevel().registryAccess()
                .lookupOrThrow(net.minecraft.core.registries.Registries.ENTITY_TYPE)
                .getValue(net.minecraft.resources.Identifier.withDefaultNamespace("wither"));
        var wither = (net.minecraft.world.entity.boss.wither.WitherBoss) helper.spawn(
                tipoWither, new BlockPos(2, 1, 1));
        wither.absSnapTo(gago.getX() + 2, gago.getY(), gago.getZ());
        wither.setHealth(wither.getMaxHealth()); // longe do escudo, so pro cap

        float base = wither.getHealth();
        gago.atirarEscopeta(wither);
        float dano = base - wither.getHealth();
        helper.assertTrue(dano > 0.0F,
                "Boss still takes buckshot damage");
        helper.assertTrue(dano <= ModConfig.get().escopetaCapDanoBoss + 0.01F,
                "Boss damage is capped (was 24/s of shredding): " + dano);

        // fregues comum (player sobrevivente, 20 de vida maxima): SEM cap — dano
        // pleno do volley. O wither sai da area (o chefe de 3 blocos bloqueava a
        // linha de visao na mesma direcao) e o fregues ocupa o ponto provado.
        wither.discard();
        var fregues = vitimaSobrevivente(helper);
        fregues.absSnapTo(gago.getX() + 2, gago.getY(), gago.getZ());
        fregues.setPermanentlyInvulnerable(false);
        fregues.setInvulnerableTime(0);
        fregues.getAbilities().invulnerable = false; // igual ao teste da 1.2.4
        float baseZ = fregues.getHealth();
        gago.cooldownTiro = 0; // o tiro no wither acima gastou o cooldown do Gago
        gago.atirarEscopeta(fregues);
        float danoZ = baseZ - fregues.getHealth();
        helper.assertTrue(danoZ > ModConfig.get().escopetaCapDanoBoss,
                "Regular customer takes the FULL volley (cap is boss-only): " + danoZ);
        helper.succeed();
    }

    /**
     * v1.2.13 — A RESSACA: voltar a ZERO vindo de estado bêbado derruba o
     * fregues (nausea+lentidao+fraqueza) por `embriaguezRessacaSegundos`; o
     * "cabelo do cachorro" (dose durante a ressaca) cura na hora. Config em 0
     * = desligada. Ressaca em nivel 1-2 (alegre) NAO existe.
     */
    @GameTest
    public void hangoverHitsAfterTheLastLevelAndDogHairCuresIt(GameTestHelper helper) {
        ModConfig teste = new ModConfig();
        teste.embriaguezRessacaSegundos = 60; // 60s de ressaca
        ModConfig atual = ModConfig.get();
        configDeTeste(teste);
        try {
            var player = vitimaSobrevivente(helper);
            Embriaguez.setNivelTeste(player, 0);

            // alegre (nível 1) zerando NÃO dá ressaca: só quem cruzou o limiar
            Embriaguez.setNivelTeste(player, 1);
            sobriedadeTeste(player, teste.embriaguezDecaySegundos - 1);
            passaSegundoTeste(player); // decay -> 0
            helper.assertTrue(Embriaguez.nivel(player) == 0,
                    "Tipsy level decays to zero");
            helper.assertFalse(Embriaguez.emRessaca(player),
                    "No hangover when you never crossed the drunk threshold");

            // bêbado (nível 3 = limiar) zerando: RESSACA entra, com efeitos.
            // O decay cai 1 NÍVEL por passada: 3->2->1->0 são TRÊS passadas —
            // a memória de travessia (ESTEVE_BEBADO) é o que sobrevive a elas
            Embriaguez.setNivelTeste(player, 3);
            for (int i = 0; i < 3; i++) {
                sobriedadeTeste(player, teste.embriaguezDecaySegundos - 1);
                passaSegundoTeste(player); // decay -1 nível; o último cruza pra ressaca
            }
            helper.assertTrue(Embriaguez.nivel(player) == 0,
                    "Three decays take the drunk customer to zero");
            helper.assertTrue(Embriaguez.emRessaca(player),
                    "Reaching zero from DRUNK starts the hangover");
            // os efeitos entram na PASSADA seguinte (a ressaca comeca no decay)
            passaSegundoTeste(player);
            helper.assertTrue(player.hasEffect(net.minecraft.world.effect.MobEffects.SLOWNESS),
                    "Hangover applies slowness");
            helper.assertTrue(player.hasEffect(net.minecraft.world.effect.MobEffects.WEAKNESS),
                    "Hangover applies weakness");

            // CABELO DO CACHORRO: dose durante a ressaca remove ela (e sobe nivel)
            Embriaguez.beber(player, 1);
            helper.assertFalse(Embriaguez.emRessaca(player),
                    "Hair of the dog cures the hangover instantly");
            helper.assertTrue(Embriaguez.nivel(player) == 1,
                    "The curing dose still counts as a dose");

            // config desligada (0s): voltar a zero NÃO deixa ressaca
            teste.embriaguezRessacaSegundos = 0;
            Embriaguez.setNivelTeste(player, 3);
            for (int i = 0; i < 3; i++) {
                sobriedadeTeste(player, teste.embriaguezDecaySegundos - 1);
                passaSegundoTeste(player);
            }
            helper.assertTrue(Embriaguez.nivel(player) == 0,
                    "Second decay run reaches zero");
            helper.assertFalse(Embriaguez.emRessaca(player),
                    "Config with 0s hangover disables the feature");
        } finally {
            configDeTeste(atual);
        }
        helper.succeed();
    }

    /**
     * v1.2.13 — A VANTAGEM DO GAGO + O CAMBALEIO: fregues bêbado paga markup
     * por nível acima do limiar (teto 30%), a tela e a cobranca usam o mesmo
     * metodo; e no nivel do soluco o corpo empurra o fregues sem pedir licenca.
     */
    @GameTest
    public void drunkCustomerPaysMarkupAndStumbles(GameTestHelper helper) {
        ModConfig teste = new ModConfig();
        teste.embriaguezMarkup = 10;
        teste.embriaguezLimiarFonar = 3;
        ModConfig atual = ModConfig.get();
        configDeTeste(teste);
        try {
            var player = vitimaSobrevivente(helper);

            // limpo e alegre (nivel 1 < limiar 3): preco NORMAL
            Embriaguez.setNivelTeste(player, 0);
            int precoLimpo = FidelidadeData.precoComDesconto(player, 10);
            helper.assertTrue(precoLimpo == 10,
                    "Sober customer pays the regular price");
            Embriaguez.setNivelTeste(player, 1);
            helper.assertTrue(FidelidadeData.precoComDesconto(player, 10) == 10,
                    "Tipsy (below the threshold) pays regular too");

            // no limiar (3): markup de 1 nivel (10%)
            Embriaguez.setNivelTeste(player, 3);
            helper.assertTrue(FidelidadeData.precoComDesconto(player, 10) == 11,
                    "Drunk at the threshold pays +1 level of markup");

            // nível 5: markup de 3 níveis (30%)... pega o teto ABSOLUTO de 30%
            Embriaguez.setNivelTeste(player, 5);
            helper.assertTrue(FidelidadeData.precoComDesconto(player, 10) == 13,
                    "Drunk at level 5 pays +3 levels of markup (30%) capped");

            // teto absoluto: 6 níveis acima tentaria 60%, mas o cap segura em 30%
            Embriaguez.setNivelTeste(player, 6);
            helper.assertTrue(FidelidadeData.precoComDesconto(player, 10) == 13,
                "Markup cap is 30% absolute regardless of level");

            // CAMBALEIO: empurrao verificavel (delta horizontal, sem vertical)
            Embriaguez.setNivelTeste(player, 5); // >= limiar do soluco (5)
            // CAMBALEIO: empurrao verificavel — o push aparece no deltaMovement
            // (a posicao so muda depois de um tick de fisica, que o teste nao roda)
            boolean empurrou = false;
            for (int i = 0; i < 12 && !empurrou; i++) {
                Embriaguez.cambaleia(player);
                net.minecraft.world.phys.Vec3 delta = player.getDeltaMovement();
                empurrou = Math.abs(delta.x) > 1.0E-6 || Math.abs(delta.z) > 1.0E-6;
            }
            helper.assertTrue(empurrou, "Stumble pushes the drunk customer around");
        } finally {
            configDeTeste(atual);
        }
        helper.succeed();
    }

    /**
     * v1.2.15 — O BALANCE DA FAZENDA (report: "as plantas crescem bem rápido"):
     * o boost ×2 de crescimento é EXCLUSIVO da LÂMPADA UV — sol forte não
     * multiplica mais (fazenda ao ar livre anda no ritmo vanilla). A MATURAÇÃO
     * continua valendo sob sol forte (early game não trava).
     */
    @GameTest
    public void uvLampBoostsGrowthButSunlightDoesNot(GameTestHelper helper) {
        var level = helper.getLevel();
        BlockPos base = helper.absolutePos(new BlockPos(2, 1, 1));
        // solo LISO (sem farmland): a velocidade base é o 0.15 fixo, isolado
        // de bioma/umidade — a conta fica exata
        helper.setBlock(2, 1, 1, IntoxicantesMod.MACONHA_PLANT);
        helper.setBlock(2, 0, 1, net.minecraft.world.level.block.Blocks.DIRT);

        // SEM lâmpada: 0.15 (solo comum) — mesmo a céu aberto (gametest é open
        // sky: sol forte, que NÃO multiplica mais)
        float semUv = ((UvCropBlock) IntoxicantesMod.MACONHA_PLANT)
                .velocidadeCrescimento(level, base);
        helper.assertTrue(Math.abs(semUv - 0.15F) < 0.001F,
                "Open-sky crop on plain dirt grows at the BASE rate (sunlight no longer doubles it): " + semUv);

        // com lâmpada ao lado: 0.15 × 2 = 0.30 — o boost é da TECNOLOGIA
        helper.setBlock(4, 1, 1, IntoxicantesMod.LAMPADA_UV);
        float comUv = ((UvCropBlock) IntoxicantesMod.MACONHA_PLANT)
                .velocidadeCrescimento(level, base);
        helper.assertTrue(Math.abs(comUv - 0.30F) < 0.001F,
                "UV lamp doubles the growth rate (the mod's tech is worth the investment): " + comUv);

        // e a maturação continua valendo sob sol forte (isUvLit inclui o céu)
        helper.assertTrue(IntoxicantesMod.isUvLit(level, base),
                "Open sky still counts as UV for RIPENING (early game keeps working)");
        helper.succeed();
    }

    /**
     * v1.2.17 — O DISJUNTOR DA LÂMPADA UV: redstone liga e DESLIGA (plantação
     * indoor com risco de corte de energia). Regra: sinal = LIGADA; controle
     * sem sinal = DESLIGADA; sem circuito = LIGADA (saves antigos funcionam).
     */
    @GameTest
    public void uvLampHasARedstoneBreaker(GameTestHelper helper) {
        var level = helper.getLevel();
        BlockPos pos = helper.absolutePos(new BlockPos(2, 2, 1));
        level.setBlockAndUpdate(pos, IntoxicantesMod.LAMPADA_UV.defaultBlockState()
                .setValue(LampadaUvBlock.LIT, Boolean.TRUE));

        // --- caso 1: SEM circuito nenhum = LIGADA (plugada na tomada)
        helper.assertTrue(LampadaUvBlock.deveEstarLigada(level, pos),
                "No redstone around: the lamp is ON (plugged in, saves keep working)");

        // --- caso 2: sinal chegando (REDSTONE BLOCK adjacente) = LIGADA
        level.setBlockAndUpdate(pos.offset(1, 0, 0),
                net.minecraft.world.level.block.Blocks.REDSTONE_BLOCK.defaultBlockState());
        helper.assertTrue(LampadaUvBlock.deveEstarLigada(level, pos),
                "Active signal: the lamp is ON");

        // --- caso 3: alavanca DESLIGADA adjacente = DESLIGADA (disjuntor aberto)
        level.setBlockAndUpdate(pos.offset(1, 0, 0),
                net.minecraft.world.level.block.Blocks.LEVER.defaultBlockState()
                        .setValue(net.minecraft.world.level.block.LeverBlock.FACING,
                                net.minecraft.core.Direction.NORTH)
                        .setValue(net.minecraft.world.level.block.LeverBlock.POWERED,
                                Boolean.FALSE));
        helper.assertFalse(LampadaUvBlock.deveEstarLigada(level, pos),
                "Unpowered lever next to it: the breaker is OFF (energy cut)");

        // e a maturação PÁRA: a planta não enxerga mais a lâmpada apagada
        BlockPos planta = pos.offset(1, -1, 0);
        level.setBlockAndUpdate(planta, IntoxicantesMod.MACONHA_PLANT.defaultBlockState()
                .setValue(UvCropBlock.AGE, 4).setValue(UvCropBlock.UV_AGE, 0));
        level.setBlockAndUpdate(pos, IntoxicantesMod.LAMPADA_UV.defaultBlockState()
                .setValue(LampadaUvBlock.LIT, Boolean.FALSE));
        helper.assertFalse(IntoxicantesMod.temLampadaUvPerto(level, planta),
                "A LIT=false lamp does NOT ripen the crop below");
        level.setBlockAndUpdate(pos, IntoxicantesMod.LAMPADA_UV.defaultBlockState()
                .setValue(LampadaUvBlock.LIT, Boolean.TRUE));
        helper.assertTrue(IntoxicantesMod.temLampadaUvPerto(level, planta),
                "A LIT=true lamp ripens the crop again");

        // --- alavanca LIGADA = energia chegando (disjuntor fechado)
        level.setBlockAndUpdate(pos.offset(1, 0, 0),
                net.minecraft.world.level.block.Blocks.LEVER.defaultBlockState()
                        .setValue(net.minecraft.world.level.block.LeverBlock.FACING,
                                net.minecraft.core.Direction.NORTH)
                        .setValue(net.minecraft.world.level.block.LeverBlock.POWERED,
                                Boolean.TRUE));
        helper.assertTrue(LampadaUvBlock.deveEstarLigada(level, pos),
                "Powered lever: energy flows, lamp is ON");
        helper.succeed();
    }
}
