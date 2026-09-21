package com.intoxicantes;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.item.ItemStack;

/**
 * Rede do Cardapio do Mercado Esquinao.
 *
 * O Gago NAO usa a UI de vilarejo vanilla: clique-direito abre uma tela propria
 * (EsquinaoCardapioScreen) alimentada por payload S2C; as compras voltam por
 * payload C2S e sao validadas SEMPRE no servidor (saldo, distancia, sessao).
 *
 * Sessao: enquanto a tela esta aberta, o player esta "em atendimento" no Gago
 * (setTradingPlayer — o Gago congela a IA). Um watchdog derruba a sessao se o
 * fregues correr longe, trocar de dimensao ou se o Gago morrer.
 */
public final class EsquinaoNetworking {
    /** Sessoes de atendimento abertas: UUID do fregues -> Gago que atende. */
    private static final Map<UUID, GagoEntity> SESSOES = new HashMap<>();

    private EsquinaoNetworking() {}

    // ==================================================== REGISTRO (common init: roda nos 2 lados)

    public static void register() {
        // Codecs (o registro e global na JVM: vale pros dois lados logados)
        PayloadTypeRegistry.clientboundPlay().register(
                AbrirCardapioPayload.TYPE, AbrirCardapioPayload.STREAM_CODEC);
        PayloadTypeRegistry.serverboundPlay().register(
                ComprarPayload.TYPE, ComprarPayload.STREAM_CODEC);
        PayloadTypeRegistry.serverboundPlay().register(
                FecharCardapioPayload.TYPE, FecharCardapioPayload.STREAM_CODEC);

        PayloadTypeRegistry.clientboundPlay().register(
                FecharCardapioPayload.TYPE, FecharCardapioPayload.STREAM_CODEC);
        PayloadTypeRegistry.serverboundPlay().register(VenderPayload.TYPE, VenderPayload.STREAM_CODEC);
        ServerPlayNetworking.registerGlobalReceiver(ComprarPayload.TYPE,
                (payload, ctx) -> negociar(ctx.player(), payload.indice(), false));
        ServerPlayNetworking.registerGlobalReceiver(VenderPayload.TYPE,
                (payload, ctx) -> negociar(ctx.player(), payload.indice(), true));
        net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents.SERVER_STOPPED.register(server -> SESSOES.clear());
        net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents.DISCONNECT.register(
                (handler, server) -> encerrarSessao(handler.player));

        // C2S: fregues fechou a tela (Esc ou botao Fechar)
        ServerPlayNetworking.registerGlobalReceiver(FecharCardapioPayload.TYPE,
                (payload, ctx) -> encerrarSessao(ctx.player()));

        // Watchdog: fregues longe demais / mudou de dimensao / Gago morreu
        ServerTickEvents.END_SERVER_TICK.register(server -> {
            if (server.getTickCount() % 10 != 0) {
                return; // a cada meio segundo basta
            }
            List<UUID> encerradas = null;
            for (Map.Entry<UUID, GagoEntity> sessao : SESSOES.entrySet()) {
                ServerPlayer player = server.getPlayerList().getPlayer(sessao.getKey());
                GagoEntity gago = sessao.getValue();
                boolean valida = player != null && gago != null && gago.isAlive()
                        && !gago.isRemoved() && !gago.isPuto() && player.isAlive()
                        && player.level() == gago.level()
                        && gago.getTradingPlayer() == player
                        && player.distanceToSqr(gago) <= 6.0 * 6.0;
                if (valida) {
                    continue;
                }
                if (encerradas == null) {
                    encerradas = new ArrayList<>();
                }
                encerradas.add(sessao.getKey());
                if (player != null) {
                    // v1.2.14: pro lang (era pt cravado)
                    player.sendSystemMessage(Component.translatable(
                            "commerce.intoxicantes.sessao.encerrada"));
                    ServerPlayNetworking.send(player, new FecharCardapioPayload());
                }
                if (gago != null && gago.getTradingPlayer() != null
                        && gago.getTradingPlayer().getUUID().equals(sessao.getKey())) {
                    gago.encerrarAtendimento();
                }
            }
            if (encerradas != null) {
                for (UUID uuid : encerradas) {
                    SESSOES.remove(uuid);
                }
            }
        });
    }

    private static void negociar(ServerPlayer player, int index, boolean selling) {
        GagoEntity gago = SESSOES.get(player.getUUID());
        if (gago == null) return;
        MarketTransactions.Result result = MarketTransactions.trade(player, gago, index, selling);
        if (result == MarketTransactions.Result.INVALID) {
            encerrarSessao(player);
            ServerPlayNetworking.send(player, new FecharCardapioPayload());
            return;
        }
        player.sendSystemMessage(Component.translatable("commerce.intoxicantes.result."
                + result.name().toLowerCase(java.util.Locale.ROOT)), true);
        player.playSound(result == MarketTransactions.Result.OK ? SoundEvents.EXPERIENCE_ORB_PICKUP
                : SoundEvents.VILLAGER_NO, 0.7F, 1.1F);
        enviarCardapio(player, gago, false);
    }

    // ==================================================== SESSAO

    /** Abre o atendimento: trava o Gago no balcao e manda o cardapio pro client. */
    public static void iniciarSessao(ServerPlayer player, GagoEntity gago) {
        GagoEntity anterior = SESSOES.get(player.getUUID());
        if (anterior != null && anterior != gago && anterior.getTradingPlayer() == player) {
            anterior.encerrarAtendimento();
        }
        if (gago.isTrading() && gago.getTradingPlayer() != player) return;
        gago.refreshTradeStock();
        SESSOES.put(player.getUUID(), gago);
        gago.setTradingPlayer(player);
        enviarCardapio(player, gago, true);
    }

    /** Monta e envia o cardapio do fregues (precos com o desconto do nivel). */
    public static void enviarCardapio(ServerPlayer player, GagoEntity gago, boolean abrir) {
        var catalog = TradeCatalog.gago(FidelidadeData.getTier(player));
        var harvests = TradeCatalog.harvests();
        List<ItemStack> produtos = catalog.stream().map(TradeCatalog.Entry::stack).toList();
        List<Integer> cheios = catalog.stream().map(TradeCatalog.Entry::price).toList();
        List<Integer> precos = catalog.stream().map(e -> FidelidadeData.precoComDesconto(player, e.price())).toList();
        List<Integer> estoque = catalog.stream().map(gago.marketInventory()::remaining).toList();
        int tier = FidelidadeData.getTier(player);
        int proximo = tier < FidelidadeData.META_TIER.length - 1 ? FidelidadeData.META_TIER[tier + 1] : -1;
        ServerPlayNetworking.send(player, new AbrirCardapioPayload(abrir, tier, FidelidadeData.getCompras(player),
                proximo, produtos, precos, cheios, FidelidadeData.INDICE_EXCLUSIVOS, PlayerMoney.get(player), estoque,
                harvests.stream().map(TradeCatalog.Entry::stack).toList(),
                harvests.stream().map(TradeCatalog.Entry::price).toList(),
                harvests.stream().map(gago.marketInventory()::remaining).toList(),
                harvests.stream().map(e -> MarketTransactions.countHarvest(player, e)).toList()));
    }

    /** Fecha o atendimento do fregues (Esc, botao Fechar ou watchdog). */
    public static void encerrarSessao(ServerPlayer player) {
        GagoEntity gago = SESSOES.remove(player.getUUID());
        if (gago != null && gago.getTradingPlayer() == player) {
            gago.encerrarAtendimento();
        }
    }

    // ==================================================== PAYLOADS

    /** S2C: cardapio completo do Esquinao, ja com os precos do fidelidade. */
    public record AbrirCardapioPayload(boolean abrir, int nivel, int compras, int comprasProximoNivel,
            List<ItemStack> produtos, List<Integer> precos, List<Integer> precoCheio,
            int primeiroExclusivo, int saldo, List<Integer> estoques,
            List<ItemStack> colheitas, List<Integer> pagamentos, List<Integer> cotas,
            List<Integer> disponiveis) implements CustomPacketPayload {

        public static final CustomPacketPayload.Type<AbrirCardapioPayload> TYPE =
                new CustomPacketPayload.Type<>(net.minecraft.resources.Identifier.fromNamespaceAndPath("intoxicantes", "abrir_cardapio"));

        public static final StreamCodec<RegistryFriendlyByteBuf, AbrirCardapioPayload> STREAM_CODEC =
                CustomPacketPayload.codec(AbrirCardapioPayload::escrever, AbrirCardapioPayload::ler);

        private static void escrever(AbrirCardapioPayload p, RegistryFriendlyByteBuf buf) {
            buf.writeBoolean(p.abrir);
            ByteBufCodecs.VAR_INT.encode(buf, p.nivel);
            ByteBufCodecs.VAR_INT.encode(buf, p.compras);
            ByteBufCodecs.VAR_INT.encode(buf, p.comprasProximoNivel);
            ByteBufCodecs.VAR_INT.encode(buf, p.produtos.size());
            for (ItemStack item : p.produtos) {
                ItemStack.STREAM_CODEC.encode(buf, item);
            }
            for (int preco : p.precos) {
                ByteBufCodecs.VAR_INT.encode(buf, preco);
            }
            for (int preco : p.precoCheio) {
                ByteBufCodecs.VAR_INT.encode(buf, preco);
            }
            ByteBufCodecs.VAR_INT.encode(buf, p.primeiroExclusivo);
            ByteBufCodecs.VAR_INT.encode(buf, p.saldo);
            for (int stock : p.estoques) ByteBufCodecs.VAR_INT.encode(buf, stock);
            ByteBufCodecs.VAR_INT.encode(buf, p.colheitas.size());
            for (int i = 0; i < p.colheitas.size(); i++) {
                ItemStack.STREAM_CODEC.encode(buf, p.colheitas.get(i));
                ByteBufCodecs.VAR_INT.encode(buf, p.pagamentos.get(i));
                ByteBufCodecs.VAR_INT.encode(buf, p.cotas.get(i));
                ByteBufCodecs.VAR_INT.encode(buf, p.disponiveis.get(i));
            }
        }

        private static AbrirCardapioPayload ler(RegistryFriendlyByteBuf buf) {
            boolean abrir = buf.readBoolean();
            int nivel = ByteBufCodecs.VAR_INT.decode(buf);
            int compras = ByteBufCodecs.VAR_INT.decode(buf);
            int proximo = ByteBufCodecs.VAR_INT.decode(buf);
            int tamanho = ByteBufCodecs.VAR_INT.decode(buf);
            if (tamanho < 0 || tamanho > 64) throw new IllegalArgumentException("Invalid market catalog size");
            List<ItemStack> produtos = new ArrayList<>(tamanho);
            for (int i = 0; i < tamanho; i++) {
                produtos.add(ItemStack.STREAM_CODEC.decode(buf));
            }
            List<Integer> precos = new ArrayList<>(tamanho);
            List<Integer> cheios = new ArrayList<>(tamanho);
            for (int i = 0; i < tamanho; i++) {
                precos.add(ByteBufCodecs.VAR_INT.decode(buf));
            }
            for (int i = 0; i < tamanho; i++) {
                cheios.add(ByteBufCodecs.VAR_INT.decode(buf));
            }
            int primeiroExclusivo = ByteBufCodecs.VAR_INT.decode(buf);
            int saldo = ByteBufCodecs.VAR_INT.decode(buf);
            List<Integer> estoques = new ArrayList<>();
            for (int i = 0; i < tamanho; i++) estoques.add(ByteBufCodecs.VAR_INT.decode(buf));
            int crops = ByteBufCodecs.VAR_INT.decode(buf);
            if (crops < 0 || crops > 16) throw new IllegalArgumentException("Invalid harvest catalog size");
            List<ItemStack> colheitas = new ArrayList<>();
            List<Integer> pagamentos = new ArrayList<>(), cotas = new ArrayList<>(), disponiveis = new ArrayList<>();
            for (int i = 0; i < crops; i++) {
                colheitas.add(ItemStack.STREAM_CODEC.decode(buf));
                pagamentos.add(ByteBufCodecs.VAR_INT.decode(buf));
                cotas.add(ByteBufCodecs.VAR_INT.decode(buf));
                disponiveis.add(ByteBufCodecs.VAR_INT.decode(buf));
            }
            return new AbrirCardapioPayload(abrir, nivel, compras, proximo, produtos,
                    precos, cheios, primeiroExclusivo, saldo, estoques, colheitas, pagamentos, cotas, disponiveis);
        }

        @Override
        public CustomPacketPayload.Type<? extends CustomPacketPayload> type() {
            return TYPE;
        }
    }

    /** C2S: fregues clicou em Comprar no indice tal do cardapio. */
    public record ComprarPayload(int indice) implements CustomPacketPayload {

        public static final CustomPacketPayload.Type<ComprarPayload> TYPE =
                new CustomPacketPayload.Type<>(net.minecraft.resources.Identifier.fromNamespaceAndPath("intoxicantes", "comprar"));

        public static final StreamCodec<RegistryFriendlyByteBuf, ComprarPayload> STREAM_CODEC =
                CustomPacketPayload.codec(
                        (p, buf) -> ByteBufCodecs.VAR_INT.encode(buf, p.indice()),
                        buf -> new ComprarPayload(ByteBufCodecs.VAR_INT.decode(buf)));

        @Override
        public CustomPacketPayload.Type<? extends CustomPacketPayload> type() {
            return TYPE;
        }
    }

    public record VenderPayload(int indice) implements CustomPacketPayload {
        public static final CustomPacketPayload.Type<VenderPayload> TYPE =
                new CustomPacketPayload.Type<>(net.minecraft.resources.Identifier.fromNamespaceAndPath("intoxicantes", "vender_colheita"));
        public static final StreamCodec<RegistryFriendlyByteBuf, VenderPayload> STREAM_CODEC =
                CustomPacketPayload.codec((p, buf) -> ByteBufCodecs.VAR_INT.encode(buf, p.indice()),
                        buf -> new VenderPayload(ByteBufCodecs.VAR_INT.decode(buf)));
        @Override
        public CustomPacketPayload.Type<? extends CustomPacketPayload> type() { return TYPE; }
    }

    /** Nos dois sentidos: "fecha a tela do cardapio". Sem corpo. */
    public record FecharCardapioPayload() implements CustomPacketPayload {

        public static final CustomPacketPayload.Type<FecharCardapioPayload> TYPE =
                new CustomPacketPayload.Type<>(net.minecraft.resources.Identifier.fromNamespaceAndPath("intoxicantes", "fechar_cardapio"));

        public static final StreamCodec<RegistryFriendlyByteBuf, FecharCardapioPayload> STREAM_CODEC =
                CustomPacketPayload.codec(
                        (p, buf) -> {},
                        buf -> new FecharCardapioPayload());

        @Override
        public CustomPacketPayload.Type<? extends CustomPacketPayload> type() {
            return TYPE;
        }
    }
}
