package com.intoxicantes;

import java.text.Normalizer;
import java.io.File;
import java.util.List;
import java.util.Locale;

import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.creativetab.v1.CreativeModeTabEvents;
import net.fabricmc.fabric.api.creativetab.v1.FabricCreativeModeTab;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.event.player.AttackBlockCallback;
import net.fabricmc.fabric.api.event.player.UseBlockCallback;
import net.fabricmc.fabric.api.loot.v3.LootTableEvents;
import net.fabricmc.fabric.api.message.v1.ServerMessageEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.fabricmc.fabric.api.object.builder.v1.entity.FabricDefaultAttributeRegistry;
import net.fabricmc.fabric.api.biome.v1.BiomeModifications;
import net.fabricmc.fabric.api.biome.v1.BiomeSelectors;
import net.fabricmc.fabric.api.biome.v1.BiomeSelectionContext;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderSet;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.Registries;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.food.FoodProperties;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemUseAnimation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.ItemLike;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.TintedParticleLeavesBlock;
import net.minecraft.world.level.block.RotatedPillarBlock;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.storage.loot.LootPool;
import net.minecraft.world.level.storage.loot.LootTable;
import net.minecraft.world.level.storage.loot.entries.LootItem;
import net.minecraft.world.level.storage.loot.predicates.LootItemRandomChanceCondition;
import net.minecraft.world.item.component.Consumable;
import net.minecraft.world.item.component.UseRemainder;
import net.minecraft.world.item.consume_effects.ApplyStatusEffectsConsumeEffect;
import net.minecraft.world.item.consume_effects.PlaySoundConsumeEffect;
import net.minecraft.world.item.consume_effects.RemoveStatusEffectsConsumeEffect;
import net.minecraft.world.item.ItemStackTemplate;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.item.SpawnEggItem;
import net.minecraft.world.level.levelgen.placement.PlacedFeature;
import net.minecraft.world.level.levelgen.GenerationStep;
import net.minecraft.world.level.biome.Biomes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.item.component.TypedEntityData;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.RandomSource;


import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Bebidas, drogas e PLANTACOES 100% FICTICIAS, tudo fabricado dentro do jogo.
 * Nada aqui representa drogas reais nem incentiva uso: e roleplay de mundo de GTA.
 */
public class IntoxicantesMod implements ModInitializer {
    public static final String MOD_ID = "intoxicantes";
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

    // ============================================================ BLOCOS: CULTURAS
    // (declarados antes das sementes, que sao BlockItems deles)
    public static final Block MACONHA_PLANT = registerCropBlock("maconha_plant", true);
    public static final Block LOUPULO_PLANT = registerCropBlock("lupulo_plant", false);
    public static final Block UVA_PLANT = registerCropBlock("uva_plant", false);
    public static final Block CAFE_PLANT = registerCropBlock("cafe_plant", false);
    public static final Block PAPOULA_PLANT = registerCropBlock("papoula_plant", false);

    // ============================================================ BLOCOS: COQUEIRO (v1.2.58)
    // A praia tem dono: tronco curvado, folhas e coco comível. O coco no pé
    // (CocoBlock) dropa o item; a AGUA_DE_COCO vira craft real.
    public static final Block COQUEIRO_TRONCO = registerBlockWithItem("coqueiro_tronco",
            new RotatedPillarBlock(BlockBehaviour.Properties.of()
                    .strength(0.8F)
                    .sound(SoundType.WOOD)
                    .mapColor(net.minecraft.world.level.material.MapColor.WOOD)
                    .setId(ResourceKey.create(Registries.BLOCK,
                            Identifier.fromNamespaceAndPath(MOD_ID, "coqueiro_tronco")))));
    public static final Block COQUEIRO_FOLHAS = registerBlockWithItem("coqueiro_folhas",
            new TintedParticleLeavesBlock(0.3F, BlockBehaviour.Properties.of()
                    .strength(0.2F)
                    .randomTicks()
                    .sound(SoundType.GRASS)
                    .noOcclusion()
                    .mapColor(net.minecraft.world.level.material.MapColor.PLANT)
                    .setId(ResourceKey.create(Registries.BLOCK,
                            Identifier.fromNamespaceAndPath(MOD_ID, "coqueiro_folhas")))));
    public static final Block COCO_BLOCO = registerBlockWithItem("coco_bloco",
            new CocoBlock(BlockBehaviour.Properties.of()
                    .strength(0.5F)
                    .sound(SoundType.WOOD)
                    .mapColor(net.minecraft.world.level.material.MapColor.WOOD)
                    .setId(ResourceKey.create(Registries.BLOCK,
                            Identifier.fromNamespaceAndPath(MOD_ID, "coco_bloco")))));

    // ============================================================ BLOCOS: LAMPADA UV
    public static final Block LAMPADA_UV = registerBlockWithItem("lampada_uv", new LampadaUvBlock(
            BlockBehaviour.Properties.of()
                    .strength(0.8F)
                    .sound(SoundType.GLASS)
                    .lightLevel(state -> state.getValue(LampadaUvBlock.LIT) ? 15 : 0)
                    .setId(ResourceKey.create(Registries.BLOCK,
                            Identifier.fromNamespaceAndPath(MOD_ID, "lampada_uv")))));

    // ============================================================ BLOCOS: POSTE DE LUZ (v1.2.19)
    // O lampeão do estacionamento: acende sozinho às 19h, apaga às 5h.
    // noOcclusion: corpo fino (coluna 4x16x4) — sem isso o vizinho "some".
    // v1.2.24: a LUZ (14) mora só no TOPO aceso — a fonte é a luminária,
    // não a coluna (3 blocos iluminados “de graça” inflava o light engine).
    public static final Block POSTE_LUZ = registerBlockWithItem("poste_luz", new PosteLuzBlock(
            BlockBehaviour.Properties.of()
                    .strength(0.6F)
                    .sound(SoundType.GLASS)
                    .noOcclusion()
                    .lightLevel(state -> state.getValue(PosteLuzBlock.PARTE) == PosteLuzBlock.Parte.TOPO
                            && state.getValue(PosteLuzBlock.LIT) ? 14 : 0)
                    .setId(ResourceKey.create(Registries.BLOCK,
                            Identifier.fromNamespaceAndPath(MOD_ID, "poste_luz")))));

    // v1.2.19: ASFALTO do estacionamento (piso denso da esquina — o pátio do
    // mercado nasce pavimentado; o bloco fica disponível pra construir rua)
    public static final Block ASFALTO = registerBlockWithItem("asfalto",
            BlockBehaviour.Properties.of()
                    .strength(1.2F)
                    .sound(SoundType.STONE));

    // ============================================================ BLOCOS: DECORACAO DA ESQUINA (v1.2.24)
    // FAIXA DE PEDESTRE: a tinta branca da travessia — bloco PLANO (1px) sobre
    // o asfalto (anda por cima sem degrau); some se o chão ceder.
    public static final Block FAIXA_PEDESTRE = registerBlockWithItem("faixa_pedestre",
            new FaixaPedestreBlock(BlockBehaviour.Properties.of()
                    .strength(0.8F)
                    .sound(SoundType.STONE)
                    .noOcclusion()
                    .mapColor(net.minecraft.world.level.material.MapColor.COLOR_LIGHT_GRAY)
                    .setId(ResourceKey.create(Registries.BLOCK,
                            Identifier.fromNamespaceAndPath(MOD_ID, "faixa_pedestre")))));

    // HIDRANTE: o vermelhão de ferro na calçada — decorativo, com jato de
    // água cômico ao usar (a esquina inteira é interativa).
    public static final Block HIDRANTE = registerBlockWithItem("hidrante",
            new HidranteBlock(BlockBehaviour.Properties.of()
                    .strength(1.0F, 4.0F)
                    .sound(SoundType.METAL)
                    .noOcclusion()
                    .mapColor(net.minecraft.world.level.material.MapColor.COLOR_RED)
                    .setId(ResourceKey.create(Registries.BLOCK,
                            Identifier.fromNamespaceAndPath(MOD_ID, "hidrante")))));

    // ============================================================ BLOCOS: PORTA-GRADE DO ESQUINÃO (v1.2.51)
    // A porta do guichê: de madrugada o guichê FECHA (colisão plena no vão
    // embaixo) e o Gago atende POR TRÁS da grade de ferro — de dia abre
    // (passagem livre). A virada é do Zelador do mercado (MarketSystem).
    public static final Block PORTA_GRADE = registerBlockWithItem("porta_grade",
            new PortaGradeBlock(BlockBehaviour.Properties.of()
                    .strength(1.2F, 4.0F)
                    .sound(SoundType.METAL)
                    .noOcclusion()
                    .mapColor(net.minecraft.world.level.material.MapColor.COLOR_BROWN)
                    .setId(ResourceKey.create(Registries.BLOCK,
                            Identifier.fromNamespaceAndPath(MOD_ID, "porta_grade")))));

    // ============================================================ BLOCOS: DESTILARIA (v1.2.50)
    // A cadeia das bebidas: máquinas de prima, fermentação, destilação e
    // maturação — CADA bebida com o SEU barril (spec 6/8: identidade visual
    // própria, 1 classe + 4 IDs, como os signs do vanilla). Processos em
    // ProcessosBebida; o BE de barril é ÚNICO e válido pros 4 blocos.
    public static final Block BARRIL_CACHACA = registerBarril("barril_cachaca", "cachaca");
    public static final Block BARRIL_CERVEJA = registerBarril("barril_cerveja", "cerveja");
    public static final Block BARRIL_RUM = registerBarril("barril_rum", "rum");
    public static final Block BARRIL_VINHO = registerBarril("barril_vinho", "vinho");
    public static final net.minecraft.world.level.block.entity.BlockEntityType<BarrilBebidaBlockEntity> BARRIL_BEBIDA_ENTITY =
            Registry.register(BuiltInRegistries.BLOCK_ENTITY_TYPE,
                    ResourceKey.create(Registries.BLOCK_ENTITY_TYPE,
                            Identifier.fromNamespaceAndPath(MOD_ID, "barril_bebida")),
                    new net.minecraft.world.level.block.entity.BlockEntityType<>(
                            BarrilBebidaBlockEntity::new,
                            java.util.Set.of(BARRIL_CACHACA, BARRIL_CERVEJA, BARRIL_RUM, BARRIL_VINHO)));

    public static final Block DORNA_BEBIDA = registerBlockWithItem("dorna_bebida", new DornaBebidaBlock(
            BlockBehaviour.Properties.of()
                    .strength(1.2F)
                    .sound(SoundType.WOOD)
                    .noOcclusion()
                    .setId(ResourceKey.create(Registries.BLOCK,
                            Identifier.fromNamespaceAndPath(MOD_ID, "dorna_bebida")))));
    public static final net.minecraft.world.level.block.entity.BlockEntityType<DornaBebidaBlockEntity> DORNA_BEBIDA_ENTITY =
            Registry.register(BuiltInRegistries.BLOCK_ENTITY_TYPE,
                    ResourceKey.create(Registries.BLOCK_ENTITY_TYPE,
                            Identifier.fromNamespaceAndPath(MOD_ID, "dorna_bebida")),
                    new net.minecraft.world.level.block.entity.BlockEntityType<>(
                            DornaBebidaBlockEntity::new, java.util.Set.of(DORNA_BEBIDA)));

    public static final Block ALAMBIQUE = registerBlockWithItem("alambique", new AlambiqueBlock(
            BlockBehaviour.Properties.of()
                    .strength(3.0F, 6.0F)
                    .sound(SoundType.COPPER)
                    .noOcclusion()
                    .mapColor(net.minecraft.world.level.material.MapColor.COLOR_ORANGE)
                    .setId(ResourceKey.create(Registries.BLOCK,
                            Identifier.fromNamespaceAndPath(MOD_ID, "alambique")))));
    public static final net.minecraft.world.level.block.entity.BlockEntityType<AlambiqueBlockEntity> ALAMBIQUE_ENTITY =
            Registry.register(BuiltInRegistries.BLOCK_ENTITY_TYPE,
                    ResourceKey.create(Registries.BLOCK_ENTITY_TYPE,
                            Identifier.fromNamespaceAndPath(MOD_ID, "alambique")),
                    new net.minecraft.world.level.block.entity.BlockEntityType<>(
                            AlambiqueBlockEntity::new, java.util.Set.of(ALAMBIQUE)));

    public static final Block MOENDA_CANA = registerBlockWithItem("moenda_cana", new MaquinaPrimaBlock(
            BlockBehaviour.Properties.of()
                    .strength(1.5F)
                    .sound(SoundType.WOOD)
                    .noOcclusion()
                    .setId(ResourceKey.create(Registries.BLOCK,
                            Identifier.fromNamespaceAndPath(MOD_ID, "moenda_cana"))),
            MaquinaPrimaBlock.Tipo.MOENDA));
    public static final net.minecraft.world.level.block.entity.BlockEntityType<MaquinaPrimaBlockEntity> MOENDA_CANA_ENTITY =
            Registry.register(BuiltInRegistries.BLOCK_ENTITY_TYPE,
                    ResourceKey.create(Registries.BLOCK_ENTITY_TYPE,
                            Identifier.fromNamespaceAndPath(MOD_ID, "moenda_cana")),
                    new net.minecraft.world.level.block.entity.BlockEntityType<>(
                            (pos, state) -> new MaquinaPrimaBlockEntity(pos, state,
                                    MaquinaPrimaBlock.Tipo.MOENDA),
                            java.util.Set.of(MOENDA_CANA)));

    public static final Block PRENSA_UVAS = registerBlockWithItem("prensa_uvas", new MaquinaPrimaBlock(
            BlockBehaviour.Properties.of()
                    .strength(1.5F)
                    .sound(SoundType.WOOD)
                    .noOcclusion()
                    .setId(ResourceKey.create(Registries.BLOCK,
                            Identifier.fromNamespaceAndPath(MOD_ID, "prensa_uvas"))),
            MaquinaPrimaBlock.Tipo.PRENSA));
    public static final net.minecraft.world.level.block.entity.BlockEntityType<MaquinaPrimaBlockEntity> PRENSA_UVAS_ENTITY =
            Registry.register(BuiltInRegistries.BLOCK_ENTITY_TYPE,
                    ResourceKey.create(Registries.BLOCK_ENTITY_TYPE,
                            Identifier.fromNamespaceAndPath(MOD_ID, "prensa_uvas")),
                    new net.minecraft.world.level.block.entity.BlockEntityType<>(
                            (pos, state) -> new MaquinaPrimaBlockEntity(pos, state,
                                    MaquinaPrimaBlock.Tipo.PRENSA),
                            java.util.Set.of(PRENSA_UVAS)));

    public static final Block CALDEIRAO_MOSTURA = registerBlockWithItem("caldeirao_mostura", new MaquinaPrimaBlock(
            BlockBehaviour.Properties.of()
                    .strength(2.0F)
                    .sound(net.minecraft.world.level.block.SoundType.METAL)
                    .noOcclusion()
                    .mapColor(net.minecraft.world.level.material.MapColor.METAL)
                    .setId(ResourceKey.create(Registries.BLOCK,
                            Identifier.fromNamespaceAndPath(MOD_ID, "caldeirao_mostura"))),
            MaquinaPrimaBlock.Tipo.CALDEIRAO));
    public static final net.minecraft.world.level.block.entity.BlockEntityType<MaquinaPrimaBlockEntity> CALDEIRAO_MOSTURA_ENTITY =
            Registry.register(BuiltInRegistries.BLOCK_ENTITY_TYPE,
                    ResourceKey.create(Registries.BLOCK_ENTITY_TYPE,
                            Identifier.fromNamespaceAndPath(MOD_ID, "caldeirao_mostura")),
                    new net.minecraft.world.level.block.entity.BlockEntityType<>(
                            (pos, state) -> new MaquinaPrimaBlockEntity(pos, state,
                                    MaquinaPrimaBlock.Tipo.CALDEIRAO),
                            java.util.Set.of(CALDEIRAO_MOSTURA)));

    // ============================================================ GUI DAS MÁQUINAS
    // v1.2.59: UM MenuType pros SEIS tipos de máquina — o TipoMaquina viaja no
    // payload de abertura (ExtendedMenuType do fabric-menu-api-v1) e a tela do
    // client reabre com a mesma geometria (MenuMaquinaSNC.reabrir). O conteúdo
    // dos slots e o ContainerData (progresso REAL) são sync vanilla.
    public static final net.fabricmc.fabric.api.menu.v1.ExtendedMenuType<MenuMaquinaSNC, Integer> MENU_MAQUINA_SNC =
            Registry.register(BuiltInRegistries.MENU,
                    ResourceKey.create(Registries.MENU,
                            Identifier.fromNamespaceAndPath(MOD_ID, "maquina_snc")),
                    new net.fabricmc.fabric.api.menu.v1.ExtendedMenuType<>(
                            (id, inv, tipoOrdinal) -> MenuMaquinaSNC.reabrir(id, inv, tipoOrdinal),
                            net.minecraft.network.codec.ByteBufCodecs.VAR_INT));

    // ============================================================ CROP: CEVADA
    // v1.2.50: crop vanilla-style (7 estágios) — a matéria-prima da cerveja.
    public static final Block CEVADA_PLANT = Registry.register(BuiltInRegistries.BLOCK,
            ResourceKey.create(Registries.BLOCK,
                    Identifier.fromNamespaceAndPath(MOD_ID, "cevada_plant")),
            new CevadaCropBlock(BlockBehaviour.Properties.of()
                    .noCollision()
                    .randomTicks()
                    .instabreak()
                    .sound(SoundType.CROP)
                    .setId(ResourceKey.create(Registries.BLOCK,
                            Identifier.fromNamespaceAndPath(MOD_ID, "cevada_plant")))));

    // ============================================================ BLOCOS: LETREIRO DO ESQUINAO
    // v1.2.18: a placa DO ZERO — painel preto com texto verde de LED, renderizado
    // por código (PlacaEsquinaoRenderer). Nada de wall_sign vanilla na fachada.
    public static final Block PLACA_ESQUINAO = registerBlockWithItem("placa_esquinao", new PlacaEsquinaoBlock(
            BlockBehaviour.Properties.of()
                    .strength(1.5F, 6.0F)
                    .sound(SoundType.METAL)
                    .noOcclusion()
                    .mapColor(net.minecraft.world.level.material.MapColor.METAL)
                    .setId(ResourceKey.create(Registries.BLOCK,
                            Identifier.fromNamespaceAndPath(MOD_ID, "placa_esquinao")))));
    // Block entity do letreiro (texto + vínculo com o mercado); valida só contra
    // o bloco da placa (colunas e painel compartilham o mesmo bloco/BE)
    public static final net.minecraft.world.level.block.entity.BlockEntityType<PlacaEsquinaoBlockEntity> PLACA_ESQUINAO_ENTITY =
            Registry.register(BuiltInRegistries.BLOCK_ENTITY_TYPE,
                    ResourceKey.create(Registries.BLOCK_ENTITY_TYPE,
                            Identifier.fromNamespaceAndPath(MOD_ID, "placa_esquinao")),
                    new net.minecraft.world.level.block.entity.BlockEntityType<>(
                            PlacaEsquinaoBlockEntity::new, java.util.Set.of(PLACA_ESQUINAO)));

    // ============================================================ BLOCOS: PAINEL DE LED CRAFTÁVEL
    // v1.2.36 — a TV de tela plana do Esquinão: painel FINO (3px) que o
    // jogador crafta e programa pela CENTRAL DE COMANDO (texto, cor, brilho,
    // modo). Suporta linha de até 3 (o painel-cabeça manda o texto).
    public static final Block PAINEL_LED = registerBlockWithItem("painel_led", new PainelLedBlock(
            BlockBehaviour.Properties.of()
                    .strength(1.0F, 4.0F)
                    .sound(SoundType.METAL)
                    .noOcclusion()
                    .mapColor(net.minecraft.world.level.material.MapColor.METAL)
                    .setId(ResourceKey.create(Registries.BLOCK,
                            Identifier.fromNamespaceAndPath(MOD_ID, "painel_led")))));
    public static final net.minecraft.world.level.block.entity.BlockEntityType<PainelLedBlockEntity> PAINEL_LED_ENTITY =
            Registry.register(BuiltInRegistries.BLOCK_ENTITY_TYPE,
                    ResourceKey.create(Registries.BLOCK_ENTITY_TYPE,
                            Identifier.fromNamespaceAndPath(MOD_ID, "painel_led")),
                    new net.minecraft.world.level.block.entity.BlockEntityType<>(
                            PainelLedBlockEntity::new, java.util.Set.of(PAINEL_LED)));


    // ============================================================ ITENS DE COMANDO
    // v1.2.38: o CONTROLE REMOTO do painel de LED — aponta pro display e
    // edita (texto, cor, brilho, modo) sem tocar no bloco
    public static final Item CENTRAL_COMANDO = registerItem("central_comando",
            new CentralComandoItem(new Item.Properties().stacksTo(1)
                    .setId(itemKey("central_comando"))));

    // ============================================================ ENTIDADES: NPCs
    // O Traficante: vendedor de drogas ficticias que aparece de vez em quando.
    public static final EntityType<TraficanteEntity> TRAFICANTE = registerEntity("traficante",
            EntityType.Builder.of(TraficanteEntity::new, MobCategory.CREATURE)
                    .sized(0.6F, 1.95F)
                    .clientTrackingRange(10)
                    .updateInterval(2));
    // O Gago: vende bebidas. CHAMA ELE DE GAGO NO CHAT E VOCE VAI VER kkkk
    public static final EntityType<GagoEntity> GAGO = registerEntity("gago",
            EntityType.Builder.of(GagoEntity::new, MobCategory.CREATURE)
                    .sized(0.6F, 1.95F)
                    .clientTrackingRange(10)
                    .updateInterval(2));
    // v1.2.39: O JUÇA — o parça do Gago. Fuma Camel, veste o Matanza e tem
    // tema de entrada (o riff toca na aproximação). Cachaça nele = show.
    public static final EntityType<JucelinoEntity> JUCA = registerEntity("juca",
            EntityType.Builder.of(JucelinoEntity::new, MobCategory.CREATURE)
                    .sized(0.6F, 1.95F)
                    .clientTrackingRange(10)
                    .updateInterval(2));

    // Ovos de spawn: SpawnEggItem le a entidade do componente ENTITY_DATA
    public static final Item OVO_TRAFICANTE = registerItem("ovo_traficante",
            new SpawnEggItem(new Item.Properties().stacksTo(64)
                    .setId(itemKey("ovo_traficante"))
                    .component(DataComponents.ENTITY_DATA,
                            TypedEntityData.of(TRAFICANTE, new CompoundTag()))));
    public static final Item OVO_GAGO = registerItem("ovo_gago",
            new SpawnEggItem(new Item.Properties().stacksTo(64)
                    .setId(itemKey("ovo_gago"))
                    .component(DataComponents.ENTITY_DATA,
                            TypedEntityData.of(GAGO, new CompoundTag()))));
    // v1.2.39: o ovo do Juça (base preta, mancha amarela-Camel)
    public static final Item OVO_JUCA = registerItem("ovo_juca",
            new SpawnEggItem(new Item.Properties().stacksTo(64)
                    .setId(itemKey("ovo_juca"))
                    .component(DataComponents.ENTITY_DATA,
                            TypedEntityData.of(JUCA, new CompoundTag()))));

    // ============================================================ SONS PROPRIOS
    // O estouro da 12 (a voz da escopeta), o blip do Gago e o zumbido da lampada.
    // Assets: assets/intoxicantes/sounds/*.ogg mapeados no sounds.json
    public static final SoundEvent SOCO_D12 = registrarSom("soco_d12");
    public static final SoundEvent VOZ_GAGO = registrarSom("voz_gago");
    public static final SoundEvent ZUMBIDO_UV = registrarSom("zumbido_uv");
    // v1.2.7: o "ca-ching" da venda e o "tum" do balim acertando
    public static final SoundEvent CAIXA_REGISTRADORA = registrarSom("caixa_registradora");
    public static final SoundEvent BALIM_ACERTO = registrarSom("balim_acerto");
    public static final SoundEvent HIC = registrarSom("hic");
    // v1.2.10: o gole em si tem som proprio (e o refluxo tambem, pitch grave)
    public static final SoundEvent GLUP = registrarSom("glup");
    // v1.2.19: o arpejo da virada do letreiro (ABERTO verde sobe, FECHADO desce)
    public static final SoundEvent LETREIRO_VIRADA = registrarSom("letreiro_virada");
    // v1.2.39: o TEMA do Juça (riff cowpunk original) e a voz dele
    public static final SoundEvent JUCA_RIFF = registrarSom("juca_riff");
    public static final SoundEvent JUCA_VOZ = registrarSom("juca_voz");

    // v1.2.32: o MECANISMO da escopeta (tubo/camara/timer/fase) vive num
    // DataComponent da stack — acompanha o item no bau, no chao e pela rede.
    public static final DataComponentType<EscopetaEstado> TIPO_ESTADO_ESCOPETA =
            Registry.register(BuiltInRegistries.DATA_COMPONENT_TYPE,
                    ResourceKey.create(Registries.DATA_COMPONENT_TYPE,
                            Identifier.fromNamespaceAndPath(MOD_ID, "estado_escopeta")),
                    DataComponentType.<EscopetaEstado>builder()
                            .persistent(EscopetaEstado.CODEC)
                            .networkSynchronized(EscopetaEstado.STREAM_CODEC)
                            .build());

    // Cartucho do calibre 12: polvora + prego + papel (declarado antes: a escopeta usa no reparo)
    public static final Item CARTUCHO = registerItem("cartucho",
            new Item(new Item.Properties().stacksTo(64).setId(itemKey("cartucho"))));
    // A escopeta: 128 usos, municao propria e REPARAVEL com 2 cartuchos na bigorna
    public static final Item ESCOPETA = registerItem("escopeta",
            new EscopetaItem(new Item.Properties().stacksTo(1)
                    .durability(128)
                    .component(DataComponents.REPAIRABLE,
                            new net.minecraft.world.item.enchantment.Repairable(
                                    net.minecraft.core.HolderSet.direct(CARTUCHO.builtInRegistryHolder())))
                    .setId(itemKey("escopeta"))));

    // ============================================================ CAMISA DO MATANZA (v1.2.39)
    /**
     * A camisa da banda do Juça — PODERES MATANZÍSTICOS DEMONÍACOS (idéia
     * do Discord): peito de couro com FIRE_RESISTANCE permanente enquanto
     * vestida (quem veste o Matanza não queima). Repara com couro.
     */
    public static final Item CAMISA_MATANZA = registerItem("camisa_matanza",
            new Item(new Item.Properties()
                    .humanoidArmor(new net.minecraft.world.item.equipment.ArmorMaterial(
                            10, // durabilidade (proximo do couro)
                            java.util.Map.of(net.minecraft.world.item.equipment.ArmorType.CHESTPLATE, 4),
                            3, // encantabilidade
                            SoundEvents.ARMOR_EQUIP_LEATHER, // já é Holder<SoundEvent>
                            0.0F, 0.0F,
                            net.minecraft.tags.ItemTags.REPAIRS_LEATHER_ARMOR,
                            net.minecraft.world.item.equipment.EquipmentAssets.LEATHER),
                            net.minecraft.world.item.equipment.ArmorType.CHESTPLATE)
                    .component(DataComponents.EQUIPPABLE,
                            net.minecraft.world.item.equipment.Equippable.builder(
                                            net.minecraft.world.entity.EquipmentSlot.CHEST)
                                    .setEquipSound(SoundEvents.ARMOR_EQUIP_LEATHER)
                                    .setAsset(
                                            net.minecraft.world.item.equipment.EquipmentAssets.LEATHER)
                                    .build())
                    .setId(itemKey("camisa_matanza"))));

    // ============================================================ REVÓLVER .38 (o "três oitão", v1.2.33)
    // Estado do tambor (6 buracos) — declarado antes dos itens que o usam
    public static final DataComponentType<RevolverEstado> TIPO_ESTADO_REVOLVER =
            Registry.register(BuiltInRegistries.DATA_COMPONENT_TYPE,
                    ResourceKey.create(Registries.DATA_COMPONENT_TYPE,
                            Identifier.fromNamespaceAndPath(MOD_ID, "estado_revolver")),
                    DataComponentType.<RevolverEstado>builder()
                            .persistent(RevolverEstado.CODEC)
                            .networkSynchronized(RevolverEstado.STREAM_CODEC)
                            .build());

    // Cartucho .38: chumbo + polvora + latao (o tres-oitao tem munição própria)
    public static final Item CARTUCHO_38 = registerItem("cartucho_38",
            new Item(new Item.Properties().stacksTo(64).setId(itemKey("cartucho_38"))));
    // O revólver: tambor de 6, mais preciso e forte por bala que a 12, reparável com 2 cartuchos .38
    public static final Item REVOLVER = registerItem("revolver",
            new RevolverItem(new Item.Properties().stacksTo(1)
                    .durability(256)
                    .component(DataComponents.REPAIRABLE,
                            new net.minecraft.world.item.enchantment.Repairable(
                                    net.minecraft.core.HolderSet.direct(CARTUCHO_38.builtInRegistryHolder())))
                    .setId(itemKey("revolver"))));

    // ============================================================ DINHEIRO R$
    public static final Item REAL = registerItem("real",
            new RealItem(new Item.Properties().stacksTo(64).setId(itemKey("real"))));

    // ============================================================ GUIA DO SNC ADVENTURES (v1.2.51)
    // O livro-guia oficial: use com o botão direito e a tela abre (client).
    // Entrega única na 1ª entrada (GuiaPrimeiraVez); recuperação SÓ por craft
    // (livro + R$) — sem comando, decisão do usuário.
    public static final Item GUIA_SNC = registerItem("guia_snc",
            new GuiaItem(new Item.Properties().stacksTo(1).setId(itemKey("guia_snc"))));

    // ============================================================ RÓTULO DO BARRIL (v1.2.50)
    // DataComponent string no BlockItem: qual bebida o barril carrega
    // (o craft carimba; o BE lê na hora da colocação). Codec simples.
    public static final DataComponentType<String> ROTULO_BARRIL =
            Registry.register(BuiltInRegistries.DATA_COMPONENT_TYPE,
                    ResourceKey.create(Registries.DATA_COMPONENT_TYPE,
                            Identifier.fromNamespaceAndPath(MOD_ID, "rotulo_barril")),
                    DataComponentType.<String>builder()
                            .persistent(com.mojang.serialization.Codec.STRING)
                            .networkSynchronized(net.minecraft.network.codec.ByteBufCodecs.STRING_UTF8)
                            .build());


    // ============================================================ SEMENTES (BlockItem das plantas)
    public static final Item SEMENTE_MACONHA = seedItem("semente_maconha", MACONHA_PLANT);
    public static final Item SEMENTE_LOUPULO = seedItem("semente_lupulo", LOUPULO_PLANT);
    public static final Item SEMENTE_UVA = seedItem("semente_uva", UVA_PLANT);
    public static final Item SEMENTE_CAFE = seedItem("semente_cafe", CAFE_PLANT);
    public static final Item SEMENTE_PAPOULA = seedItem("semente_papoula", PAPOULA_PLANT);
    // v1.2.50: a cevada entra na família (mesmo modelo de pacote de sementes)
    public static final Item SEMENTE_CEVADA = seedItem("semente_cevada", CEVADA_PLANT);

    // ============================================================ PRODUTOS DAS PLANTACOES
    public static final Item LOUPULO_FRESCO = product("lupulo");
    public static final Item UVA = product("uva");
    public static final Item CAFE_VERDE = product("cafe_verde");
    public static final Item CANA_DE_ACUCAR = product("cana_de_acucar");

    // ============================================================ MATÉRIA-PRIMA DAS BEBIDAS (v1.2.50)
    // Cerveja: cevada (crop novo) → malte (forja) → mosto (caldeirão + lúpulo)
    public static final Item CEVADA = product("cevada");
    public static final Item MALTE = product("malte");
    // Cachaça: cana → caldo (moenda); caldo → mosto fermentado (dorna)
    public static final Item CALDO_DE_CANA = product("caldo_de_cana");
    public static final Item MOSTO_CANA_FERMENTADO = product("mosto_cana_fermentado");
    // Rum: cana → melaço (forna caldo) → mosto (dorna)
    public static final Item MELACO = product("melaco");
    public static final Item MOSTO_RUM_FERMENTADO = product("mosto_rum_fermentado");
    // Vinho: uva → mosto de uva (prensa)
    public static final Item MOSTO_DE_UVA = product("mosto_de_uva");
    // Cerveja: malte + agua (caldeirao) + lupulo na fervura -> mosto lupulado
    public static final Item MOSTO_CERVEJA_LUPULADO = product("mosto_cerveja_lupulado");
    // Destilados jovens (saem do alambique; o barril completa)
    public static final Item CACHACA_JOVEM = product("cachaca_jovem");
    public static final Item RUM_JOVEM = product("rum_jovem");
    // Subproduto da moenda (combustível de fornalha)
    public static final Item BAGACO_DE_CANA = registerItem("bagaco_de_cana",
            new Item(new Item.Properties().stacksTo(64).setId(itemKey("bagaco_de_cana"))));

    // ============================================================ BEBIDAS
    public static final Item CERVEJA = drink("cerveja",
            effect(MobEffects.STRENGTH, 900, 0),
            effect(MobEffects.NAUSEA, 200, 0));

    public static final Item VINHO = drink("vinho",
            effect(MobEffects.REGENERATION, 600, 0),
            effect(MobEffects.NAUSEA, 300, 0));

    public static final Item CACHACA = drink("cachaca",
            effect(MobEffects.STRENGTH, 900, 1),
            effect(MobEffects.NAUSEA, 400, 0),
            effect(MobEffects.SLOWNESS, 300, 0));

    public static final Item HIDROMEL = drink("hidromel",
            effect(MobEffects.ABSORPTION, 1200, 0),
            effect(MobEffects.NAUSEA, 200, 0));

    public static final Item RUM = drink("rum",
            effect(MobEffects.FIRE_RESISTANCE, 1200, 0),
            effect(MobEffects.NAUSEA, 300, 0));

    // ============================================================ ERVAS & DROGAS
    /** Seda: folha colhida direto, precisa secar/rolar. */
    public static final Item MACONHA_SEDA = powder("maconha_seda",
            effect(MobEffects.NAUSEA, 200, 0),
            effect(MobEffects.HUNGER, 200, 0));

    /** Baseado: fumavel (tipo colunar), efeito tranquilo. */
    public static final Item BASEADO = smoke("baseado",
            effect(MobEffects.REGENERATION, 300, 0),
            effect(MobEffects.SLOW_FALLING, 600, 0),
            effect(MobEffects.NAUSEA, 150, 0));

    /**
     * v1.2.39 — CIGARRO CAMEL (amarelo): o cigarro do Juça. Trago curto
     * (colunar igual o baseado) com um shot rápido de pressa + tontura —
     * é nicotina de roleplay, não remédio.
     */
    public static final Item CIGARRO_CAMEL = smoke("cigarro_camel",
            effect(MobEffects.SPEED, 200, 0),
            effect(MobEffects.NAUSEA, 100, 0));

    /** Opio: anestesico de rua — tanque barato que deixa lento (nicho real de uso). */
    public static final Item OPIO = powder("opio",
            effect(MobEffects.RESISTANCE, 600, 0),
            effect(MobEffects.NAUSEA, 300, 0),
            effect(MobEffects.WEAKNESS, 300, 0));

    /** Cocaina: burst de energia brutal + queda brutal depois. */
    public static final Item COCAINA = powder("cocaina",
            effect(MobEffects.SPEED, 1200, 2),
            effect(MobEffects.HASTE, 1200, 1),
            effect(MobEffects.MINING_FATIGUE, 400, 0),
            effect(MobEffects.WEAKNESS, 300, 0));

    /** Heroína: anestesia total, câmera lenta e regeneração. */
    public static final Item HEROINA = powder("heroina",
            effect(MobEffects.RESISTANCE, 600, 1),
            effect(MobEffects.REGENERATION, 400, 0),
            effect(MobEffects.SLOWNESS, 600, 2),
            effect(MobEffects.NAUSEA, 300, 0));

    /** LSD: viagem total — levitação, visão distorcida, trevas. */
    public static final Item LSD = pill("lsd",
            effect(MobEffects.LEVITATION, 200, 0),
            effect(MobEffects.NIGHT_VISION, 2400, 0),
            effect(MobEffects.DARKNESS, 200, 0),
            effect(MobEffects.NAUSEA, 400, 0));

    // ============================================================ SUBSTÂNCIAS FICTÍCIAS
    public static final Item PO_ESTELAR = powder("po_estelar",
            effect(MobEffects.SPEED, 1800, 1),
            effect(MobEffects.HASTE, 1800, 0),
            effect(MobEffects.HUNGER, 200, 0));

    public static final Item COGUMELO_XAMANICO = powder("cogumelo_xamanico",
            effect(MobEffects.NAUSEA, 400, 0),
            effect(MobEffects.NIGHT_VISION, 2400, 0),
            effect(MobEffects.LEVITATION, 100, 0));

    public static final Item NEVOA_DO_DESERTO = powder("nevoa_do_deserto",
            effect(MobEffects.BLINDNESS, 300, 0),
            effect(MobEffects.NIGHT_VISION, 2400, 0),
            effect(MobEffects.JUMP_BOOST, 600, 1));

    public static final Item RAIZ_DE_SOMBRA = powder("raiz_de_sombra",
            effect(MobEffects.SLOW_FALLING, 1200, 0),
            effect(MobEffects.WEAKNESS, 900, 0));

    public static final Item CRISTAL_DE_EUFORIA = powder("cristal_de_euforia",
            effect(MobEffects.REGENERATION, 400, 1),
            effect(MobEffects.SPEED, 1200, 0),
            effect(MobEffects.JUMP_BOOST, 1200, 0));

    public static final Item EXTRATO_CAFEINA = powder("extrato_cafeina",
            effect(MobEffects.HASTE, 2400, 0),
            effect(MobEffects.SPEED, 2400, 0));

    // ==================================================== SAUDE: O CAMINHO DE CURA (v1.2.54)
    // SUCO DETOX: a redenção do fregues — regenera os órgãos devagar (o
    // SaudeSystem aplica a cura e a hidratação no fim do gole).
    public static final Item SUCO_DETOX = drink("suco_detox");
    // AGUA DE COCO: o isotônico do sertão (+50 de hidratação).
    public static final Item AGUA_DE_COCO = drink("agua_de_coco");

    // ==================================================== A VIDA ALÉM DA CERVEJA (v1.2.58)
    // COCO: o fruto in natura — quebra o coco no pé, come e já mata fome e sede
    // (a hidratação +30 o SaudeSystem aplica no fim da mordida).
    public static final Item COCO_FRUTO = register("coco", comLore("coco",
            new Item.Properties().stacksTo(64)
                    .food(new FoodProperties.Builder().nutrition(2).saturationModifier(0.3F).alwaysEdible().build())));
    // CHÁ DE LÚPULO: a calma da flor — corta a viagem na hora (é o "café" do
    // psicodélico) e regenera devagar. Devolve a garrafa vazia.
    public static final Item CHA_LUPULO = comLoreConsumivel("cha_lupulo",
            new Item.Properties().stacksTo(16),
            Consumable.builder()
                    .animation(ItemUseAnimation.DRINK)
                    .sound(SoundEvents.GENERIC_DRINK)
                    .onConsume(new ApplyStatusEffectsConsumeEffect(List.of(
                            new MobEffectInstance(MobEffects.REGENERATION, 200, 0))))
                    .onConsume(new RemoveStatusEffectsConsumeEffect(HolderSet.direct(
                            Efeitos.OVERDRIVE, Efeitos.VIAGEM)))
                    .build());
    // PÃO DE CEVADA: a comida honesta da colheita — mata fome de verdade
    // (o trigo tem pão, a cevada tem o dela agora)
    public static final Item PAO_CEVADA = register("pao_cevada", comLore("pao_cevada",
            new Item.Properties().stacksTo(64)
                    .food(new FoodProperties.Builder().nutrition(6).saturationModifier(0.6F).build())));

    /** Abas vanilla onde os itens tambem aparecem (facilidade de descoberta). */
    private static final ResourceKey<CreativeModeTab> FOOD_AND_DRINKS = ResourceKey.create(
            Registries.CREATIVE_MODE_TAB, Identifier.fromNamespaceAndPath("minecraft", "food_and_drinks"));
    private static final ResourceKey<CreativeModeTab> INGREDIENTS = ResourceKey.create(
            Registries.CREATIVE_MODE_TAB, Identifier.fromNamespaceAndPath("minecraft", "ingredients"));
    private static final ResourceKey<CreativeModeTab> NATURAL = ResourceKey.create(
            Registries.CREATIVE_MODE_TAB, Identifier.fromNamespaceAndPath("minecraft", "natural"));
    private static final ResourceKey<CreativeModeTab> FUNCTIONAL = ResourceKey.create(
            Registries.CREATIVE_MODE_TAB, Identifier.fromNamespaceAndPath("minecraft", "functional"));

    public static final CreativeModeTab TAB = FabricCreativeModeTab.builder()
            .title(Component.translatable("itemGroup.intoxicantes"))
            .icon(() -> new ItemStack(CERVEJA))
            .displayItems((parameters, output) -> {
                // Sementes
                output.accept(SEMENTE_MACONHA);
                output.accept(SEMENTE_LOUPULO);
                output.accept(SEMENTE_UVA);
                output.accept(SEMENTE_CAFE);
                output.accept(SEMENTE_PAPOULA);
                // Ovos dos NPCs
                output.accept(OVO_TRAFICANTE);
                output.accept(OVO_GAGO);
                // Lampada (as plantas nao entram: a semente e o proprio BlockItem delas)
                output.accept(LAMPADA_UV);
                // v1.2.18: o letreiro da casa (placa custom de LED)
                output.accept(PLACA_ESQUINAO);
                // v1.2.36: o painel de LED craftável (a TV de tela plana)
                output.accept(PAINEL_LED);
                // v1.2.38: o CONTROLE REMOTO (Central de Comando portátil)
                output.accept(CENTRAL_COMANDO);
                // v1.2.19: o poste de luz do estacionamento (acende de noite)
                output.accept(POSTE_LUZ);
                output.accept(ASFALTO);
                // v1.2.51: a porta-grade do guichê (a porta com dono)
                output.accept(PORTA_GRADE);
                // Produtos agricolas
                output.accept(LOUPULO_FRESCO);
                output.accept(UVA);
                output.accept(CAFE_VERDE);
                output.accept(CANA_DE_ACUCAR);
                // v1.2.58: o coqueiro (bloco + fruto)
                output.accept(COQUEIRO_TRONCO);
                output.accept(COQUEIRO_FOLHAS);
                output.accept(COCO_FRUTO);
                // v1.2.50: a cadeia das bebidas (matéria-prima, máquinas e barris)
                output.accept(SEMENTE_CEVADA);
                output.accept(CEVADA);
                output.accept(MALTE);
                output.accept(MOENDA_CANA);
                output.accept(CALDO_DE_CANA);
                output.accept(BAGACO_DE_CANA);
                output.accept(DORNA_BEBIDA);
                output.accept(MOSTO_CANA_FERMENTADO);
                output.accept(ALAMBIQUE);
                output.accept(CACHACA_JOVEM);
                output.accept(MELACO);
                output.accept(MOSTO_RUM_FERMENTADO);
                output.accept(RUM_JOVEM);
                output.accept(PRENSA_UVAS);
                output.accept(MOSTO_DE_UVA);
                output.accept(CALDEIRAO_MOSTURA);
                output.accept(MOSTO_CERVEJA_LUPULADO);
                output.accept(BARRIL_CACHACA);
                output.accept(BARRIL_CERVEJA);
                output.accept(BARRIL_RUM);
                output.accept(BARRIL_VINHO);
                // Ervas
                output.accept(MACONHA_SEDA);
                output.accept(BASEADO);
                output.accept(CIGARRO_CAMEL);
                output.accept(OPIO);
                // Quimicos
                output.accept(COCAINA);
                output.accept(HEROINA);
                output.accept(LSD);
                // Bebidas
                output.accept(CERVEJA);
                output.accept(VINHO);
                output.accept(CACHACA);
                output.accept(HIDROMEL);
                output.accept(RUM);
                // Substancias misticas
                output.accept(PO_ESTELAR);
                output.accept(COGUMELO_XAMANICO);
                output.accept(NEVOA_DO_DESERTO);
                output.accept(RAIZ_DE_SOMBRA);
                output.accept(CRISTAL_DE_EUFORIA);
                output.accept(EXTRATO_CAFEINA);
                // v1.2.54: o caminho de cura (saúde do fregues)
                output.accept(SUCO_DETOX);
                output.accept(AGUA_DE_COCO);
                // v1.2.58: a vida além da cerveja (chá, pão de cevada)
                // (COCO_FRUTO não entra aqui de novo: já está no grupo do
                // coqueiro acima — item repetido na MESMA aba quebra o client
                // com "Accidentally adding the same item stack twice")
                output.accept(CHA_LUPULO);
                output.accept(PAO_CEVADA);
                // Armas do Gago
                output.accept(ESCOPETA);
                output.accept(CARTUCHO);
                // O três-oitão
                output.accept(REVOLVER);
                output.accept(CARTUCHO_38);
                // Dinheiro
                output.accept(REAL);
                // v1.2.51: o livro-guia (topo da aba é a cerveja; o guia fica
                // no fim, junto do dinheiro — manual de consulta)
                output.accept(GUIA_SNC);
            })
            .build();

    @Override
    public void onInitialize() {
        // v1.2.7: partículas proprias — registrar AQUI (mod init), antes do freeze
        // das registries. Referenciar Particulas.DINHEIRO do Server thread depois
        // que a registry congelou explode "Registry is already frozen".
        Particulas.init();

        // v1.2.41: a TECLA R das armas — payload C2S de recarga + canal S2C de status
        RecargaPayload.registrar();
        GatilhoPayload.registrar();
        RecargaPayload.registrarStatus();

        // v1.2.53: botões CoD/BF — estado de mira (ADS) via payload C2S, limpa no logout
        MiraPayload.registrar();
        ServerPlayConnectionEvents.DISCONNECT.register((handler, server) ->
                MiraPayload.limpar(handler.player));

        ResourceKey<CreativeModeTab> tabKey = ResourceKey.create(Registries.CREATIVE_MODE_TAB,
                Identifier.fromNamespaceAndPath(MOD_ID, "main"));
        Registry.register(BuiltInRegistries.CREATIVE_MODE_TAB, tabKey, TAB);

        // Bebidas tambem na aba vanilla "Comidas e Bebidas"
        CreativeModeTabEvents.modifyOutputEvent(FOOD_AND_DRINKS).register(output -> {
            output.accept(CERVEJA);
            output.accept(VINHO);
            output.accept(CACHACA);
            output.accept(HIDROMEL);
            output.accept(RUM);
            // v1.2.54: os sucos entram na família
            output.accept(SUCO_DETOX);
            output.accept(AGUA_DE_COCO);
        });
        // Sementes na aba "Natureza" — nao duplica se ja estiver na aba do mod
        // Lampada UV na aba "Funcional"
        CreativeModeTabEvents.modifyOutputEvent(FUNCTIONAL).register(output -> {
            output.accept(LAMPADA_UV);
        });
        // Ervas e quimicos tambem na aba vanilla "Ingredientes"
        CreativeModeTabEvents.modifyOutputEvent(INGREDIENTS).register(output -> {
            output.accept(MACONHA_SEDA);
            output.accept(BASEADO);
            output.accept(OPIO);
            output.accept(COCAINA);
            output.accept(HEROINA);
            output.accept(LSD);
            output.accept(CANA_DE_ACUCAR);
            // v1.2.50: intermediários da destilaria também descobríveis
            output.accept(CEVADA);
            output.accept(MALTE);
            output.accept(CALDO_DE_CANA);
            output.accept(MELACO);
            output.accept(PO_ESTELAR);
            output.accept(COGUMELO_XAMANICO);
            output.accept(NEVOA_DO_DESERTO);
            output.accept(RAIZ_DE_SOMBRA);
            output.accept(CRISTAL_DE_EUFORIA);
            output.accept(EXTRATO_CAFEINA);
            output.accept(REAL);
        });

        // ======================================================== LOOT TABLES
        // Sementes: drops naturais ao quebrar grama, samambaia, trepadeiras e papoula
        LootTableEvents.MODIFY.register((key, tableBuilder, source, registries) -> {
            if (!source.isBuiltin()) {
                return;
            }
            // chaves de loot tables de bloco vanilla: minecraft:blocks/<bloco>
            java.util.function.Function<String, ResourceKey<LootTable>> blockTable = id -> ResourceKey.create(
                    Registries.LOOT_TABLE, Identifier.withDefaultNamespace("blocks/" + id));
            addSeedPool(tableBuilder, key, blockTable.apply("short_grass"), SEMENTE_MACONHA, 0.10F);
            addSeedPool(tableBuilder, key, blockTable.apply("fern"), SEMENTE_MACONHA, 0.10F);
            // v1.2.50: a cevada brota selvagem na taiga e nas planícies frias
            addSeedPool(tableBuilder, key, blockTable.apply("short_grass"), SEMENTE_CEVADA, 0.06F);
            addSeedPool(tableBuilder, key, blockTable.apply("vine"), SEMENTE_UVA, 0.20F);
            addSeedPool(tableBuilder, key, blockTable.apply("jungle_leaves"), SEMENTE_UVA, 0.05F);
            addSeedPool(tableBuilder, key, blockTable.apply("large_fern"), SEMENTE_CAFE, 0.10F);
            addSeedPool(tableBuilder, key, blockTable.apply("poppy"), SEMENTE_PAPOULA, 0.08F);
            addSeedPool(tableBuilder, key, blockTable.apply("sweet_berry_bush"), SEMENTE_LOUPULO, 0.15F);
            // Papoula vanilla passa a dropar o OPIO (seiva) junto
            addSeedPool(tableBuilder, key, blockTable.apply("poppy"), OPIO, 0.35F);
            // Zumbis dropam R$ (15% chance, 1-3 unidades)
            addSeedPool(tableBuilder, key, ResourceKey.create(Registries.LOOT_TABLE,
                    Identifier.fromNamespaceAndPath("minecraft", "entities/zombie")), REAL, 0.15F);
            // Piglins tambem dropam R$ (25% chance)
            addSeedPool(tableBuilder, key, ResourceKey.create(Registries.LOOT_TABLE,
                    Identifier.fromNamespaceAndPath("minecraft", "entities/piglin")), REAL, 0.25F);
            // Esqueletos dropam cartucho (20% chance): fonte alternativa de municao
            addSeedPool(tableBuilder, key, ResourceKey.create(Registries.LOOT_TABLE,
                    Identifier.fromNamespaceAndPath("minecraft", "entities/skeleton")), CARTUCHO, 0.20F);
            // Esqueletos dropam cartucho .38 (12% chance): munição do três-oitão
            addSeedPool(tableBuilder, key, ResourceKey.create(Registries.LOOT_TABLE,
                    Identifier.fromNamespaceAndPath("minecraft", "entities/skeleton")), CARTUCHO_38, 0.12F);
        });

        // ======================================================== NPCS: atributos + ovos nas abas
        FabricDefaultAttributeRegistry.register(TRAFICANTE, TraficanteEntity.createAttributes());
        FabricDefaultAttributeRegistry.register(GAGO, GagoEntity.createAttributes());
        FabricDefaultAttributeRegistry.register(JUCA, JucelinoEntity.createAttributes());
        CreativeModeTabEvents.modifyOutputEvent(CreativeModeTabs.SPAWN_EGGS).register(output -> {
            output.accept(OVO_TRAFICANTE);
            output.accept(OVO_GAGO);
            output.accept(OVO_JUCA);
        });

        // ======================================================== SISTEMA DE DINHEIRO R$
        MoneyCommands.register();

        // ======================================================== GUIA DO SNC ADVENTURES
        // Entrega única na 1ª entrada (server-side; flag em JSON no mundo).
        GuiaPrimeiraVez.registrar();

        // ======================================================== MERCADO ESQUINÃO
        MarketSystem.register();

        // v1.2.7: fumaca propria do baseado (enquanto o fregues puxa)
        BaseadoFumaca.register();

        // v1.2.9: embriaguez — dose, fala fonar no chat e HIC
        Embriaguez.register();

        // ==================================================== SAUDE + VIAGENS (v1.2.54)
        // Os MobEffects assinatura precisam estar registrados ANTES do freeze
        // das registries: tocar a classe dispara o static que registra.
        Efeitos.carga();
        // Rede da saúde (sync S2C + tecla H do prontuário) e o motor (sede,
        // vício/abstinência, queda das viagens, efeitos por droga)
        SaudeNetworking.registrar();
        SaudeSystem.register();

        // ======================================================== CARDAPIO DO ESQUINAO (rede)
        EsquinaoNetworking.register();

        // v1.2.36: rede da CENTRAL DE COMANDO (painel de LED + letreiro)
        CentralComandoNetworking.register();


        // v1.2.32: rede da 12 — o kick de camera (S2C). O registro do codec e'
        // global na JVM: precisa existir antes do 1o tiro em qualquer lado.
        net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry.clientboundPlay().register(
                RecuoPayload.TYPE, RecuoPayload.STREAM_CODEC);

        // ======================================================== SPAWN PERIODICO DE TRAFICANTE
        // O Gago agora so aparece no Mercado Esquinão (24h, muda de posicao).
        // O traficante continua aparecendo aleatoriamente.
        final int[] cooldown = {120 * 20};
        ServerTickEvents.END_SERVER_TICK.register(server -> {
            if (--cooldown[0] > 0) {
                return;
            }
            cooldown[0] = 120 * 20;
            RandomSource rng = RandomSource.create();
            for (ServerPlayer player : server.getPlayerList().getPlayers()) {
                if (player.isSpectator()) {
                    continue;
                }
                ServerLevel lvl = player.level() instanceof ServerLevel sl ? sl : null;
                if (lvl == null) {
                    continue;
                }
                for (int i = 0; i < 3; i++) {
                    int dx = rng.nextInt(17) - 8;
                    int dz = rng.nextInt(17) - 8;
                    int dist = 24 + rng.nextInt(17);
                    double ang = rng.nextDouble() * Math.PI * 2;
                    int x = (int) (player.getX() + Math.cos(ang) * dist) + dx / 2;
                    int z = (int) (player.getZ() + Math.sin(ang) * dist) + dz / 2;
                    if (!lvl.hasChunkAt(new BlockPos(x, 0, z))) {
                        continue;
                    }
                    int y = lvl.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, x, z) + 1;
                    if (y < lvl.getMinY() + 1) {
                        continue;
                    }
                    BlockPos pos = new BlockPos(x, y, z);
                    if (!lvl.getFluidState(pos.below()).isEmpty()
                            || !lvl.getBlockState(pos.below()).isSolidRender()) {
                        continue;
                    }
                    // So traficante aparece aleatoriamente (1/6 chance)
                    if (rng.nextInt(6) == 0) {
                        TraficanteEntity npc = TRAFICANTE.create(lvl, EntitySpawnReason.EVENT);
                        if (npc != null) {
                            npc.absSnapTo(x + 0.5, y, z + 0.5, rng.nextFloat() * 360F, 0F);
                            npc.finalizeSpawn(lvl, lvl.getCurrentDifficultyAt(pos), EntitySpawnReason.EVENT, null);
                            lvl.addFreshEntity(npc);
                            npc.anunciarChegada(lvl);
                        }
                    }
                }
            }
        });

        // ======================================================== CHAT: O SISTEMA DO "GAGO"
        // Se alguem escrever "gago" (com ou sem acento) no chat, TODO gago num raio
        // de 48 blocos fica PUTO DA CARA e vai atras do infeliz kkkk
        // Delay de 5 ticks pra mensagem do player aparecer primeiro
        java.util.List<Runnable> gagoQueue = new java.util.ArrayList<>();
        ServerTickEvents.END_SERVER_TICK.register(server -> {
            synchronized (gagoQueue) {
                if (!gagoQueue.isEmpty()) {
                    for (Runnable r : gagoQueue) r.run();
                    gagoQueue.clear();
                }
            }
        });
        ServerMessageEvents.CHAT_MESSAGE.register((message, sender, params) -> {
            String texto = message.signedContent();
            String normalizada = Normalizer.normalize(texto, Normalizer.Form.NFD)
                    .replaceAll("\\p{M}+", "")
                    .toLowerCase(Locale.ROOT);
            if (!normalizada.contains("gago")) {
                return;
            }
            for (ServerLevel level : sender.level().getServer().getAllLevels()) {
                for (var npc : level.getEntitiesOfClass(GagoEntity.class,
                        sender.getBoundingBox().inflate(48.0))) {
                    GagoEntity g = npc;
                    synchronized (gagoQueue) {
                        gagoQueue.add(() -> g.ouvirChat(sender, texto));
                    }
                }
            }
        });

        // ==================================================== CHAT: O SISTEMA DO "JUÇA"
        // v1.2.39: escreveu "juca" (com ou sem acento)? O Juça responde com o
        // "hé hé" e uma puxada no Camel — o Gago fica PUTO, o Juça só zoa.
        ServerMessageEvents.CHAT_MESSAGE.register((message, sender, params) -> {
            String texto = message.signedContent();
            String normalizada = Normalizer.normalize(texto, Normalizer.Form.NFD)
                    .replaceAll("\\p{M}+", "")
                    .toLowerCase(Locale.ROOT);
            if (!normalizada.contains("juca")) {
                return;
            }
            for (ServerLevel level : sender.level().getServer().getAllLevels()) {
                for (JucelinoEntity juca : level.getEntitiesOfClass(JucelinoEntity.class,
                        sender.getBoundingBox().inflate(48.0))) {
                    JucelinoEntity j = juca;
                    synchronized (gagoQueue) {
                        gagoQueue.add(() -> j.ouvirChat(sender, level));
                    }
                }
            }
        });

        // ==================================================== A CAMISA DO MATANZA
        // v1.2.39: PODERES MATANZISTICOS DEMONIACOS — quem veste a camisa da
        // banda não queima (fire resistance permanente enquanto no peito).
        ServerTickEvents.END_SERVER_TICK.register(server -> {
            MobEffectInstance chama = new MobEffectInstance(
                    MobEffects.FIRE_RESISTANCE, 220, 0, true, false, true);
            for (ServerPlayer jogador : server.getPlayerList().getPlayers()) {
                ItemStack peito = jogador.getItemBySlot(net.minecraft.world.entity.EquipmentSlot.CHEST);
                if (peito.is(IntoxicantesMod.CAMISA_MATANZA)) {
                    jogador.addEffect(chama);
                }
                // v1.2.40 — SEM IMUNIDADE FANTASMA: tirou a camisa, o efeito
                // vai junto. O efeito da camisa é AMBIENT (poção de fogo
                // nunca é) — o fire res de poção/fogueira do jogador não é
                // tocado. O print do Skyu: banhou em lava e nem esquentou
                // 11s depois de guardar a camisa.
                else if (jogador.hasEffect(MobEffects.FIRE_RESISTANCE)) {
                    MobEffectInstance atual = jogador.getEffect(MobEffects.FIRE_RESISTANCE);
                    if (atual != null && atual.isAmbient()) {
                        jogador.removeEffect(MobEffects.FIRE_RESISTANCE);
                    }
                }
            }
        });

        // ======================================================== LETREIRO + PEDRADAS NO MERCADO
        // Direita no letreiro = fregues lendo o nome da loja: o Gago cumprimenta.
        // v1.2.18: só pra placa VANILLA (saves antigos); a placa nova cumprimenta
        // no próprio PlacaEsquinaoBlock.useWithoutItem
        UseBlockCallback.EVENT.register((player, level, mao, hit) -> {
            if (level.isClientSide()) return net.minecraft.world.InteractionResult.PASS;
            if (!(level.getBlockState(hit.getBlockPos()).getBlock()
                    instanceof net.minecraft.world.level.block.SignBlock)) {
                return net.minecraft.world.InteractionResult.PASS;
            }
            BlockPos posPlaca = hit.getBlockPos();
            var mercado = MarketSystem.getMarketPos();
            if (mercado == null || !posPlaca.closerThan(mercado, 24.0)) {
                return net.minecraft.world.InteractionResult.PASS;
            }
            if (player instanceof net.minecraft.server.level.ServerPlayer sp) {
                for (GagoEntity g : level.getEntitiesOfClass(GagoEntity.class,
                        sp.getBoundingBox().inflate(32.0))) {
                    g.cumprimentarLeitor((ServerLevel) level, sp);
                    break;
                }
            }
            return net.minecraft.world.InteractionResult.PASS; // nao consome: a placa abre a edicao normal
        });
        // Soco/bomba em bloco na area do mercado = vandalismo: Gago saca a 12 por 30s.
        // (quebrar bloco de verdade ja ativa o HurtByTargetGoal vanilla via dano? NAO —
        // quebrar bloco nao machuca ninguem, por isso o evento dedicado)
        AttackBlockCallback.EVENT.register((player, level, mao, pos, direcao) -> {
            if (level.isClientSide()) return net.minecraft.world.InteractionResult.PASS;
            var mercado = MarketSystem.getMarketPos();
            if (mercado == null || !pos.closerThan(mercado, 16.0)) {
                return net.minecraft.world.InteractionResult.PASS;
            }
            if (player instanceof net.minecraft.server.level.ServerPlayer sp
                    && !sp.getAbilities().instabuild) { // criativo e obra, nao vandalismo kkkk
                for (GagoEntity g : level.getEntitiesOfClass(GagoEntity.class,
                        sp.getBoundingBox().inflate(32.0))) {
                    g.naPedrada((ServerLevel) level, sp);
                    break;
                }
            }
            return net.minecraft.world.InteractionResult.PASS; // o soco funciona normal
        });

        // ======================================================== DINHEIRO + MERCADO: persistencia
        net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents.SERVER_STARTING.register(server -> {
            File worldDir = server.getWorldPath(net.minecraft.world.level.storage.LevelResource.ROOT).toFile();
            ModConfig.init(server);
            PlayerMoney.init(worldDir);
            FidelidadeData.init(worldDir);
            // v1.2.10: embriaguez sobrevive a relog/restart (intoxicantes_embriaguez.json)
            Embriaguez.init(worldDir);
            // v1.2.54: saúde do fregues sobrevive a relog/restart (intoxicantes_saude.json)
            SaudeData.init(worldDir);
            MarketSystem.load(worldDir);
            // v1.2.51: flag "recebeu o guia" (intoxicantes_guia.json)
            GuiaPrimeiraVez.init(worldDir);
            LOGGER.info("[Intoxicantes] Sistema de dinheiro R$ e Mercado Esquinao inicializados.");
        });
        // Blindagem anti-Invulnerable: saves antigos (template com Invulnerable=1)
        // gravaram NPCs com escudo de dano que o hurt engole SEM LOG — "a 12 nao da
        // dano". No primeiro tick de cada NPC do mod, derruba o escudo.
        net.fabricmc.fabric.api.event.lifecycle.v1.ServerEntityEvents.ENTITY_LOAD.register((ent, level) -> {
            if (!level.isClientSide()
                    && (ent instanceof GagoEntity || ent instanceof TraficanteEntity)
                    && ent.isInvulnerable()) {
                ent.setPermanentlyInvulnerable(false);
                LOGGER.warn("[Intoxicantes] {} carregou com Invulnerable=1 — escudo derrubado",
                        ent.getName().getString());
            }
        });
        net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents.SERVER_STOPPING.register(server -> {
            File worldDir = server.getWorldPath(net.minecraft.world.level.storage.LevelResource.ROOT).toFile();
            MarketSystem.save(worldDir);
        });

        // ======================================================== BEBIDAS: O LIVRO-RECEITAS
        // v1.2.50: o registro central dos processos (dorna, alambique, barris,
        // moenda, prensa, caldeirão). Chamar antes de qualquer máquina rodar.
        ProcessosBebida.registrar();
        // v1.2.60: o catálogo virou DATAPACK (JSON em
        // data/intoxicantes/processo_bebida/<maquina>/) com serializer próprio;
        // os defaults de fábrica continuam valendo se a pasta vier vazia.
        CatalogoBebidas.registrar();

        // ======================================================== WORLDGEN: MATOS SELVAGENS
        // Plantacoes abandonadas/plantas selvagens espalhadas pelo mundo.
        // Cada cultura tem seu bioma de preferencia, igual weed na natureza kkkk
        addWildPatches();

        // v1.2.58: a FEATURE do coqueiro (java) + o JSON de placed_feature que
        // o patch() acima injeta nas praias
        registrarFeature("coqueiro", CoqueiroFeature.CODEC);

        LOGGER.info("[Intoxicantes] Plantacoes, bebidas, NPCs e substancias ficticias registradas. Lembre: e so jogo!");
    }

    // ============================================================ WORLDGEN
    /**
     * Plantações abandonadas espalhadas pelo mundo: cada cultura nasce num bioma
     * que combina com ela, em manchas pequenas e raras (achar é metade da diversão).
     * Os JSONs de feature/placed_feature ficam em data/intoxicantes/worldgen/.
     */
    private static void addWildPatches() {
        patch("maconha_selvagem",
                BiomeSelectors.includeByKey(
                        Biomes.PLAINS, Biomes.SUNFLOWER_PLAINS, Biomes.FOREST, Biomes.BIRCH_FOREST));
        patch("lupulo_selvagem",
                BiomeSelectors.includeByKey(
                        Biomes.FOREST, Biomes.FLOWER_FOREST, Biomes.OLD_GROWTH_BIRCH_FOREST));
        patch("uva_selvagem",
                BiomeSelectors.includeByKey(
                        Biomes.JUNGLE, Biomes.SPARSE_JUNGLE, Biomes.WOODED_BADLANDS));
        patch("cafe_selvagem",
                BiomeSelectors.includeByKey(
                        Biomes.TAIGA, Biomes.OLD_GROWTH_PINE_TAIGA, Biomes.OLD_GROWTH_SPRUCE_TAIGA));
        patch("papoula_selvagem",
                BiomeSelectors.includeByKey(
                        Biomes.SWAMP, Biomes.SAVANNA, Biomes.MEADOW));
        // v1.2.50: a cevada selvagem (planícies e taiga — o cereal do frio)
        patch("cevada_selvagem",
                BiomeSelectors.includeByKey(
                        Biomes.PLAINS, Biomes.SUNFLOWER_PLAINS, Biomes.TAIGA, Biomes.SNOWY_PLAINS));
        // v1.2.58: o coqueiro nas praias — a fonte do coco (e da água de coco)
        patch("coqueiro_praia",
                BiomeSelectors.includeByKey(
                        Biomes.BEACH, Biomes.JUNGLE, Biomes.STONY_SHORE));
    }

    /** Injeta a placed_feature (JSON) nos biomas selecionados, na etapa de vegetação. */
    private static void patch(String nome,
            java.util.function.Predicate<BiomeSelectionContext> seletor) {
        ResourceKey<PlacedFeature> chave = ResourceKey.create(Registries.PLACED_FEATURE,
                Identifier.fromNamespaceAndPath(MOD_ID, nome));
        BiomeModifications.addFeature(seletor,
                GenerationStep.Decoration.VEGETAL_DECORATION, chave);
    }

    /**
     * v1.2.58: registra uma feature JAVA (a do coqueiro) no registry do
     * worldgen — JSON sozinho não segura lógica de construção (tronco curvo,
     * coroa, cocos pendurados).
     */
    private static void registrarFeature(String nome, com.mojang.serialization.MapCodec<? extends net.minecraft.world.level.levelgen.feature.Feature> codec) {
        Registry.register(BuiltInRegistries.FEATURE_TYPE,
                ResourceKey.create(Registries.FEATURE_TYPE,
                        Identifier.fromNamespaceAndPath(MOD_ID, nome)), codec);
    }

    // ============================================================ UV / LUZ
    /**
     * Luz "UV" pra maturação: sol forte (>= 12, sem chuva em cima) ou lampada UV
     * a ate 3 blocos de distancia.
     */
    public static boolean isUvLit(net.minecraft.world.level.Level level, net.minecraft.core.BlockPos pos) {
        // Sol direto: luz >= 12, ceu aberto e sem chuva caindo no bloco (checagem mais barata)
        if (level.getRawBrightness(pos, 0) >= 12
                && level.canSeeSky(pos)
                && !level.isRainingAt(pos)) {
            return true;
        }
        return temLampadaUvPerto(level, pos);
    }

    /**
     * v1.2.15: a LÂMPADA UV sozinha — separada do isUvLit porque o BOOST DE
     * CRESCIMENTO agora é exclusivo dela (sol é sol; a lâmpada é a tecnologia
     * do mod e tem que valer o investimento — report do beta tester:
     * "as plantas crescem bem rápido"). Raio 2 (125 blocos vs 343).
     */
    public static boolean temLampadaUvPerto(net.minecraft.world.level.Level level, net.minecraft.core.BlockPos pos) {
        // v1.2.17: raio configurável (uvRaio, padrão 2 = 5x5x5)
        int raio = Math.max(1, ModConfig.get().uvRaio);
        for (int dx = -raio; dx <= raio; dx++) {
            for (int dy = -raio; dy <= raio; dy++) {
                for (int dz = -raio; dz <= raio; dz++) {
                    var estado = level.getBlockState(pos.offset(dx, dy, dz));
                    // v1.2.17: só conta se a lâmpada estiver LIGADA (redstone
                    // cortou a energia? a maturação para no tempo)
                    if (estado.is(LAMPADA_UV)
                            && estado.getValueOrElse(LampadaUvBlock.LIT, Boolean.TRUE)) {
                        return true;
                    }
                }
            }
        }
        return false;
    }

    // ============================================================ LOOT HELPER
    private static void addSeedPool(LootTable.Builder table, ResourceKey<LootTable> key,
                                    ResourceKey<LootTable> alvo, Item item, float chance) {
        if (!key.equals(alvo)) {
            return;
        }
        table.withPool(LootPool.lootPool()
                .when(LootItemRandomChanceCondition.randomChance(chance))
                .setRolls(Holder.direct(
                        new net.minecraft.world.level.storage.loot.providers.number.ints.ConstantValue(1)))
                .add(LootItem.lootTableItem(item)));
    }

    // ============================================================ FABRICAS DE ITEM
    private static MobEffectInstance effect(Holder<MobEffect> holder, int durationTicks, int amplifier) {
        return new MobEffectInstance(holder, durationTicks, amplifier);
    }

    /** Bebida SEM efeito (v1.2.54: suco detox e água de coco — a saúde aplica no consumo). */
    private static Item drink(String name) {
        return drink(name, new MobEffectInstance[0]);
    }

    /** Bebida: animação de beber + devolve garrafa de vidro ao terminar. */
    private static Item drink(String name, MobEffectInstance... effects) {
        Consumable.Builder builder = Consumable.builder()
                .animation(ItemUseAnimation.DRINK)
                .sound(SoundEvents.GENERIC_DRINK);
        return register(name, comLore(name, new Item.Properties().stacksTo(16)
                .food(alwaysEdible(), withEffects(builder, effects))
                .component(net.minecraft.core.component.DataComponents.USE_REMAINDER,
                        new UseRemainder(new ItemStackTemplate(Items.GLASS_BOTTLE)))));
    }

    /** Lore de personalidade: cada consumível explica o nicho dele no tooltip. */
    private static Item.Properties comLore(String name, Item.Properties properties) {
        return properties.component(net.minecraft.core.component.DataComponents.LORE,
                new net.minecraft.world.item.component.ItemLore(java.util.List.of(
                        net.minecraft.network.chat.Component.translatable(
                                "item.intoxicantes." + name + ".lore"))));
    }

    /**
     * v1.2.58: consumível com lore + efeitos de consumo + sobra (a garrafa).
     * O chá de lúpulo usa isto em vez do drink() padrão porque PRECISA limpar
     * a viagem ativa (RemoveStatusEffects) — coisa que o drink() não faz.
     */
    private static Item comLoreConsumivel(String name, Item.Properties properties,
            Consumable consumavel) {
        return register(name, comLore(name, properties
                .food(new FoodProperties.Builder().alwaysEdible().build(), consumavel)
                .component(net.minecraft.core.component.DataComponents.USE_REMAINDER,
                        new UseRemainder(new ItemStackTemplate(Items.GLASS_BOTTLE)))));
    }

    /** Registra um SoundEvent (o sounds.json define quais .ogg ele toca). */
    private static SoundEvent registrarSom(String name) {
        return SoundEvent.createVariableRangeEvent(
                Identifier.fromNamespaceAndPath(MOD_ID, name));
    }

    /** Registra um item ja construido (pra classes que nao sao Item puro). */
    private static Item registerItem(String name, Item item) {
        return Registry.register(BuiltInRegistries.ITEM, itemKey(name), item);
    }

    /** Chave de item padrao. */
    private static ResourceKey<Item> itemKey(String name) {
        return ResourceKey.create(Registries.ITEM,
                Identifier.fromNamespaceAndPath(MOD_ID, name));
    }

    /** Registra entidade com ID setado no builder (obrigatorio nessa versao). */
    private static <T extends Entity> EntityType<T> registerEntity(String name,
            EntityType.Builder<T> builder) {
        ResourceKey<EntityType<?>> key = ResourceKey.create(Registries.ENTITY_TYPE,
                Identifier.fromNamespaceAndPath(MOD_ID, name));
        return Registry.register(BuiltInRegistries.ENTITY_TYPE, key, builder.build(key));
    }

    /** Semente: BlockItem que planta o bloco, empilhável, nome próprio. */
    private static Item seedItem(String name, Block plant) {
        ResourceKey<Item> key = ResourceKey.create(Registries.ITEM,
                Identifier.fromNamespaceAndPath(MOD_ID, name));
        return Registry.register(BuiltInRegistries.ITEM, key,
                new BlockItem(plant, new Item.Properties().stacksTo(64).setId(key)));
    }

    /** Produto agricola: item simples. */
    private static Item product(String name) {
        return register(name, new Item.Properties().stacksTo(64));
    }

    /** Pó/substância: v1.2.53 — SPYGLASS (leva a mão ao nariz, como cheirar) em vez de EAT (comer): droga não é comida. */
    private static Item powder(String name, MobEffectInstance... effects) {
        Consumable.Builder builder = Consumable.builder()
                .animation(ItemUseAnimation.SPYGLASS)
                .consumeSeconds(1.6F)
                .sound(Holder.direct(SoundEvents.WOOL_STEP));  // rufar surdo do papel/ficato
        return register(name, comLore(name, new Item.Properties().stacksTo(16)
                .food(alwaysEdible(), withEffects(builder, effects))));
    }

    /** Comprimido/pílula. */
    private static Item pill(String name, MobEffectInstance... effects) {
        return powder(name, effects);
    }

    /** Erva pra fumar: v1.2.53 — BOW (a mão leva o baseado à boca e "puxa" como arco e flecha) + fósforo. */
    private static Item smoke(String name, MobEffectInstance... effects) {
        Holder<SoundEvent> flint = Holder.direct(SoundEvents.FLINTANDSTEEL_USE);
        Consumable.Builder builder = Consumable.builder()
                .animation(ItemUseAnimation.BOW) // puxada: o braço recua igual ao arco
                .consumeSeconds(2.2F)           // 3 puxadas curtas (o fumo não é golado)
                .sound(flint)
                .onConsume(new PlaySoundConsumeEffect(Holder.direct(SoundEvents.FIRE_EXTINGUISH)));
        return register(name, comLore(name, new Item.Properties().stacksTo(16)
                .food(alwaysEdible(), withEffects(builder, effects))));
    }

    private static FoodProperties alwaysEdible() {
        return new FoodProperties.Builder().alwaysEdible().build();
    }

    private static Consumable withEffects(Consumable.Builder builder, MobEffectInstance... effects) {
        builder.onConsume(new ApplyStatusEffectsConsumeEffect(List.of(effects), 1.0f));
        return builder.build();
    }

    private static Item register(String name, Item.Properties properties) {
        ResourceKey<Item> key = ResourceKey.create(Registries.ITEM,
                Identifier.fromNamespaceAndPath(MOD_ID, name));
        return Registry.register(BuiltInRegistries.ITEM, key, new Item(properties.setId(key)));
    }

    // ============================================================ BLOCOS
    /** Registra um bloco JA CONSTRUIDO (com comportamento proprio, ex. LampadaUvBlock) + o item. */
    private static Block registerBlockWithItem(String name, Block bloco) {
        ResourceKey<Block> blockKey = ResourceKey.create(Registries.BLOCK,
                Identifier.fromNamespaceAndPath(MOD_ID, name));
        Registry.register(BuiltInRegistries.BLOCK, blockKey, bloco);
        ResourceKey<Item> itemKey = ResourceKey.create(Registries.ITEM,
                Identifier.fromNamespaceAndPath(MOD_ID, name));
        Registry.register(BuiltInRegistries.ITEM, itemKey,
                new BlockItem(bloco, new Item.Properties().setId(itemKey)));
        return bloco;
    }

    private static Block registerBlockWithItem(String name, BlockBehaviour.Properties properties) {
        ResourceKey<Block> blockKey = ResourceKey.create(Registries.BLOCK,
                Identifier.fromNamespaceAndPath(MOD_ID, name));
        Block block = new Block(properties.setId(blockKey));
        Registry.register(BuiltInRegistries.BLOCK, blockKey, block);
        ResourceKey<Item> itemKey = ResourceKey.create(Registries.ITEM,
                Identifier.fromNamespaceAndPath(MOD_ID, name));
        Registry.register(BuiltInRegistries.ITEM, itemKey,
                new BlockItem(block, new Item.Properties().setId(itemKey)));
        return block;
    }

    /**
     * Barril de bebida (v1.2.50): 1 classe, 4 IDs — o rótulo nasce no ITEM
     * (DataComponent string) e o BE lê do bloco colocado. O loot e o craft
     * são por barril; as receitas de processo consultam o rótulo.
     */
    private static Block registerBarril(String name, String rotulo) {
        ResourceKey<Block> blockKey = ResourceKey.create(Registries.BLOCK,
                Identifier.fromNamespaceAndPath(MOD_ID, name));
        Block bloco = new BarrilBebidaBlock(BlockBehaviour.Properties.of()
                .strength(1.8F)
                .sound(SoundType.WOOD)
                .noOcclusion()
                .mapColor(rotulo.equals("vinho")
                        ? net.minecraft.world.level.material.MapColor.COLOR_PURPLE
                        : net.minecraft.world.level.material.MapColor.WOOD)
                .setId(blockKey));
        Registry.register(BuiltInRegistries.BLOCK, blockKey, bloco);
        ResourceKey<Item> itemKey = ResourceKey.create(Registries.ITEM,
                Identifier.fromNamespaceAndPath(MOD_ID, name));
        Registry.register(BuiltInRegistries.ITEM, itemKey, new BlockItem(bloco,
                new Item.Properties().setId(itemKey)
                        .component(ROTULO_BARRIL, rotulo)));
        return bloco;
    }

    /** Registra só o bloco da cultura; a semente (BlockItem) vem depois. */
    private static Block registerCropBlock(String name, boolean aceitaSoloComum) {
        ResourceKey<Block> blockKey = ResourceKey.create(Registries.BLOCK,
                Identifier.fromNamespaceAndPath(MOD_ID, name));
        UvCropBlock crop = new UvCropBlock(BlockBehaviour.Properties.of()
                .noCollision()
                .randomTicks()
                .instabreak()
                .sound(SoundType.CROP)
                .setId(blockKey), aceitaSoloComum);
        Registry.register(BuiltInRegistries.BLOCK, blockKey, crop);
        return crop;
    }
}
