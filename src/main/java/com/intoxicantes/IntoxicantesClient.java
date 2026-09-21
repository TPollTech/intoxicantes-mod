package com.intoxicantes;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.rendering.v1.EntityRendererRegistry;

import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.model.npc.VillagerModel;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.client.renderer.entity.layers.CrossedArmsItemLayer;
import net.minecraft.client.renderer.entity.state.HoldingEntityRenderState;
import net.minecraft.client.renderer.entity.state.VillagerRenderState;
import net.minecraft.resources.Identifier;

/**
 * Parte client do mod: renderer dos NPCs.
 * Os dois usam o modelo classico de vilarejo, mudando so a skin.
 * A CrossedArmsItemLayer e o que faz o item na mao RENDERIZAR (mesma camada
 * do wandering trader vanilla — sem ela o renderer ignora o heldItem).
 */
public class IntoxicantesClient implements ClientModInitializer {
    @Override
    public void onInitializeClient() {
        // partículas proprias do mod (atlas + providers, antes do primeiro render)
        ParticulasClient.init();

        EntityRendererRegistry.register(IntoxicantesMod.TRAFICANTE, TraficanteRenderer::new);
        EntityRendererRegistry.register(IntoxicantesMod.GAGO, GagoRenderer::new);

        // ==================================================== CARDAPIO DO ESQUINAO
        // S2C: o Gago mandou o cardapio -> abre a tela custom (em tick de client).
        net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking
                .registerGlobalReceiver(EsquinaoNetworking.AbrirCardapioPayload.TYPE,
                        (payload, ctx) -> ctx.client().execute(() -> {
                            if (ctx.player() != null) {
                                if (ctx.client().gui.screen() instanceof EsquinaoCardapioScreen current) {
                                    current.atualizar(payload);
                                } else if (payload.abrir()) {
                                    ctx.client().gui.setScreen(new EsquinaoCardapioScreen(payload));
                                }
                            }
                        }));
        // S2C: o servidor derrubou a sessao (fregues sumiu do balcao) -> fecha a tela.
        net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking
                .registerGlobalReceiver(EsquinaoNetworking.FecharCardapioPayload.TYPE,
                        (payload, ctx) -> ctx.client().execute(() -> {
                            if (ctx.client().gui.screen()
                                    instanceof EsquinaoCardapioScreen) {
                                ctx.client().gui.setScreen(null);
                            }
                        }));
    }

    /** Traficante: hood cinza-escuro, oculos e corrente (kkkk). */
    private static class TraficanteRenderer
            extends MobRenderer<TraficanteEntity, VillagerRenderState, VillagerModel> {
        TraficanteRenderer(net.minecraft.client.renderer.entity.EntityRendererProvider.Context ctx) {
            super(ctx, new VillagerModel(ctx.bakeLayer(ModelLayers.VILLAGER)), 0.5F);
            this.addLayer(new CrossedArmsItemLayer<>(this));
        }

        @Override
        public VillagerRenderState createRenderState() {
            return new VillagerRenderState();
        }

        @Override
        public void extractRenderState(TraficanteEntity entidade, VillagerRenderState state, float parcial) {
            super.extractRenderState(entidade, state, parcial);
            HoldingEntityRenderState.extractHoldingEntityRenderState(entidade, state,
                    this.itemModelResolver);
        }

        @Override
        public Identifier getTextureLocation(VillagerRenderState state) {
            return Identifier.fromNamespaceAndPath(
                    IntoxicantesMod.MOD_ID, "textures/entity/traficante.png");
        }
    }

    /**
     * Gago: fantasia tematica do bioma do mercado + escopeta na mao (kkkk).
     * O indice da roupa chega sincronizado (DATA_ROUPA no servidor); aqui ele
     * vira um VillagerType vanilla no campo villagerData do render state — e'
     * EXATAMENTE o canal que o vanilla usa pra escolher textura de vilarejo
     * (7 tipos = nossas 7 roupas). NAO subclassificamos o render state: a
     * CrossedArmsItemLayer exige o generics exato do VillagerRenderState.
     */
    private static class GagoRenderer
            extends MobRenderer<GagoEntity, VillagerRenderState, VillagerModel> {
        GagoRenderer(net.minecraft.client.renderer.entity.EntityRendererProvider.Context ctx) {
            super(ctx, new VillagerModel(ctx.bakeLayer(ModelLayers.VILLAGER)), 0.5F);
            this.addLayer(new CrossedArmsItemLayer<>(this));
        }

        @Override
        public VillagerRenderState createRenderState() {
            return new VillagerRenderState();
        }

        @Override
        public void extractRenderState(GagoEntity entidade, VillagerRenderState state, float parcial) {
            super.extractRenderState(entidade, state, parcial);
            // item na mao: mesma rotina do WanderingTrader (a escopeta vai aparecer!)
            HoldingEntityRenderState.extractHoldingEntityRenderState(entidade, state,
                    this.itemModelResolver);
            // roupa -> VillagerType vanilla (plains/desert/jungle/savanna/snow/taiga/swamp)
            int roupa = Math.floorMod(entidade.getRoupa(), GagoEntity.ROUPAS);
            state.villagerData = new net.minecraft.world.entity.npc.villager.VillagerData(
                    tipoHolder(NOME_TIPO[roupa]), profissaoNone(), 1);
        }

        @Override
        public Identifier getTextureLocation(VillagerRenderState state) {
            String tipo = state.villagerData == null ? "plains"
                    : state.villagerData.type().unwrapKey()
                            .map(k -> k.identifier().getPath()).orElse("plains");
            return Identifier.fromNamespaceAndPath(IntoxicantesMod.MOD_ID,
                    "textures/entity/" + texturaDoTipo(tipo) + ".png");
        }

        private static final String[] NOME_TIPO = {
                "plains", "desert", "jungle", "savanna", "snow", "taiga", "swamp"
        };

        private static String texturaDoTipo(String tipo) {
            return switch (tipo) {
                case "desert" -> "gago_sertao";
                case "jungle" -> "gago_mata";
                case "savanna" -> "gago_cerrado";
                case "snow" -> "gago_sul";      // sede matriz kkkk
                case "taiga" -> "gago_serra";
                case "swamp" -> "gago_brejo";
                default -> "gago";               // plains (e qualquer outro)
            };
        }

        private static net.minecraft.core.Holder<net.minecraft.world.entity.npc.villager.VillagerType> tipoHolder(
                String caminho) {
            return net.minecraft.core.registries.BuiltInRegistries.VILLAGER_TYPE
                    .get(Identifier.withDefaultNamespace(caminho)).orElseThrow();
        }

        private static net.minecraft.core.Holder<net.minecraft.world.entity.npc.villager.VillagerProfession> profissaoNone() {
            return net.minecraft.core.registries.BuiltInRegistries.VILLAGER_PROFESSION
                    .get(Identifier.withDefaultNamespace("none")).orElseThrow();
        }
    }
}
