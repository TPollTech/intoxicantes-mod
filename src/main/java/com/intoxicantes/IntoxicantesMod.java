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
import net.fabricmc.fabric.api.object.builder.v1.entity.FabricDefaultAttributeRegistry;
import net.fabricmc.fabric.api.biome.v1.BiomeModifications;
import net.fabricmc.fabric.api.biome.v1.BiomeSelectors;
import net.fabricmc.fabric.api.biome.v1.BiomeSelectionContext;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
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
import net.minecraft.world.item.ItemStackTemplate;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.item.SpawnEggItem;
import net.minecraft.world.level.levelgen.placement.PlacedFeature;
import net.minecraft.world.level.levelgen.GenerationStep;
import net.minecraft.world.level.biome.Biomes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.core.component.DataComponents;
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

    // ============================================================ BLOCOS: LAMPADA UV
    public static final Block LAMPADA_UV = registerBlockWithItem("lampada_uv", new LampadaUvBlock(
            BlockBehaviour.Properties.of()
                    .strength(0.8F)
                    .sound(SoundType.GLASS)
                    .lightLevel(state -> state.getValue(LampadaUvBlock.LIT) ? 15 : 0)
                    .setId(ResourceKey.create(Registries.BLOCK,
                            Identifier.fromNamespaceAndPath(MOD_ID, "lampada_uv")))));

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

    // ============================================================ DINHEIRO R$
    public static final Item REAL = registerItem("real",
            new RealItem(new Item.Properties().stacksTo(64).setId(itemKey("real"))));

    // ============================================================ SEMENTES (BlockItem das plantas)
    public static final Item SEMENTE_MACONHA = seedItem("semente_maconha", MACONHA_PLANT);
    public static final Item SEMENTE_LOUPULO = seedItem("semente_lupulo", LOUPULO_PLANT);
    public static final Item SEMENTE_UVA = seedItem("semente_uva", UVA_PLANT);
    public static final Item SEMENTE_CAFE = seedItem("semente_cafe", CAFE_PLANT);
    public static final Item SEMENTE_PAPOULA = seedItem("semente_papoula", PAPOULA_PLANT);

    // ============================================================ PRODUTOS DAS PLANTACOES
    public static final Item LOUPULO_FRESCO = product("lupulo");
    public static final Item UVA = product("uva");
    public static final Item CAFE_VERDE = product("cafe_verde");
    public static final Item CANA_DE_ACUCAR = product("cana_de_acucar");

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
                // Produtos agricolas
                output.accept(LOUPULO_FRESCO);
                output.accept(UVA);
                output.accept(CAFE_VERDE);
                output.accept(CANA_DE_ACUCAR);
                // Ervas
                output.accept(MACONHA_SEDA);
                output.accept(BASEADO);
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
                // Armas do Gago
                output.accept(ESCOPETA);
                output.accept(CARTUCHO);
                // Dinheiro
                output.accept(REAL);
            })
            .build();

    @Override
    public void onInitialize() {
        // v1.2.7: partículas proprias — registrar AQUI (mod init), antes do freeze
        // das registries. Referenciar Particulas.DINHEIRO do Server thread depois
        // que a registry congelou explode "Registry is already frozen".
        Particulas.init();

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
        });

        // ======================================================== NPCS: atributos + ovos nas abas
        FabricDefaultAttributeRegistry.register(TRAFICANTE, TraficanteEntity.createAttributes());
        FabricDefaultAttributeRegistry.register(GAGO, GagoEntity.createAttributes());
        CreativeModeTabEvents.modifyOutputEvent(CreativeModeTabs.SPAWN_EGGS).register(output -> {
            output.accept(OVO_TRAFICANTE);
            output.accept(OVO_GAGO);
        });

        // ======================================================== SISTEMA DE DINHEIRO R$
        MoneyCommands.register();

        // ======================================================== MERCADO ESQUINÃO
        MarketSystem.register();

        // v1.2.7: fumaca propria do baseado (enquanto o fregues puxa)
        BaseadoFumaca.register();

        // v1.2.9: embriaguez — dose, fala fonar no chat e HIC
        Embriaguez.register();

        // ======================================================== CARDAPIO DO ESQUINAO (rede)
        EsquinaoNetworking.register();

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

        // ======================================================== LETREIRO + PEDRADAS NO MERCADO
        // Direita no letreiro = fregues lendo o nome da loja: o Gago cumprimenta.
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
            MarketSystem.load(worldDir);
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

        // ======================================================== WORLDGEN: MATOS SELVAGENS
        // Plantacoes abandonadas/plantas selvagens espalhadas pelo mundo.
        // Cada cultura tem seu bioma de preferencia, igual weed na natureza kkkk
        addWildPatches();

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
    }

    /** Injeta a placed_feature (JSON) nos biomas selecionados, na etapa de vegetação. */
    private static void patch(String nome,
            java.util.function.Predicate<BiomeSelectionContext> seletor) {
        ResourceKey<PlacedFeature> chave = ResourceKey.create(Registries.PLACED_FEATURE,
                Identifier.fromNamespaceAndPath(MOD_ID, nome));
        BiomeModifications.addFeature(seletor,
                GenerationStep.Decoration.VEGETAL_DECORATION, chave);
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

    /** Pó/substância: animação de comer. */
    private static Item powder(String name, MobEffectInstance... effects) {
        Consumable.Builder builder = Consumable.builder()
                .animation(ItemUseAnimation.EAT)
                .sound(SoundEvents.GENERIC_EAT);
        return register(name, comLore(name, new Item.Properties().stacksTo(16)
                .food(alwaysEdible(), withEffects(builder, effects))));
    }

    /** Comprimido/pílula. */
    private static Item pill(String name, MobEffectInstance... effects) {
        return powder(name, effects);
    }

    /** Erva pra fumar: colunar, som de acendedor + fumaça no fim. */
    private static Item smoke(String name, MobEffectInstance... effects) {
        Holder<SoundEvent> flint = Holder.direct(SoundEvents.FLINTANDSTEEL_USE);
        Consumable.Builder builder = Consumable.builder()
                .animation(ItemUseAnimation.DRINK) // colunar, tipo col
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
