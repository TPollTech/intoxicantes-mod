package com.intoxicantes;


import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.DifficultyInstance;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.AgeableMob;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.SpawnGroupData;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.MeleeAttackGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.goal.WaterAvoidingRandomStrollGoal;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.entity.npc.villager.AbstractVillager;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.trading.MerchantOffer;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.ServerLevelAccessor;

import org.jspecify.annotations.Nullable;

/**
 * O Traficante: vendedor ambulante de drogas 100% FICTICIAS.
 * Vende na boa, mas se agredir ele te caça (e dropa o estoque kkkk).
 * Nada aqui e real nem incentiva uso: e roleplay de mundo de GTA.
 */
public class TraficanteEntity extends AbstractVillager {
    /** Falas de apresentacao quando spawna perto do player. */
    static final String[] FRASES_CHEGADA = {
            "entity.intoxicantes.traficante.chegada.1",
            "entity.intoxicantes.traficante.chegada.2",
            "entity.intoxicantes.traficante.chegada.3",
            "entity.intoxicantes.traficante.chegada.4"
    };

    private boolean anunciouChegada;

    public TraficanteEntity(EntityType<? extends AbstractVillager> type, Level level) {
        super(type, level);
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Mob.createMobAttributes()
                .add(Attributes.MAX_HEALTH, 30.0)
                .add(Attributes.MOVEMENT_SPEED, 0.32)
                .add(Attributes.ATTACK_DAMAGE, 6.0)
                .add(Attributes.FOLLOW_RANGE, 32.0);
    }

    @Override
    protected void registerGoals() {
        this.goalSelector.addGoal(0, new FloatGoal(this));
        this.goalSelector.addGoal(2, new MeleeAttackGoal(this, 1.25, true));
        this.goalSelector.addGoal(5, new WaterAvoidingRandomStrollGoal(this, 0.9));
        this.goalSelector.addGoal(6, new LookAtPlayerGoal(this, Player.class, 8.0F));
        this.goalSelector.addGoal(7, new RandomLookAroundGoal(this));
        this.targetSelector.addGoal(1, new HurtByTargetGoal(this));
    }

    private final DailyTradeStock tradeStock = new DailyTradeStock();

    void refreshTradeStock() {
        if (this.level() instanceof ServerLevel server) {
            long ticks = MarketSystem.tempoTotal(server.getServer().overworld());
            long seedDia = DailyTradeStock.tradingDay(ticks);
            this.offers = tradeStock.refresh(this.offers, ticks, this.isTrading(),
                    () -> TradeCatalog.traficante(this.random, seedDia));
        }
    }

    @Override
    public void tick() {
        super.tick();
        if (!this.level().isClientSide() && this.tickCount % 20 == 0) refreshTradeStock();
        // EM ATENDIMENTO: estatueta (o nego nao sai andando no meio da negociação kkkk)
        if (!this.level().isClientSide() && this.getTradingPlayer() != null) {
            this.setNoAi(true);
            this.getNavigation().stop();
            this.setDeltaMovement(net.minecraft.world.phys.Vec3.ZERO);
        }
    }

    /** Fechou a tela: volta a andar (IA de novo). */
    @Override
    protected void stopTrading() {
        super.stopTrading();
        if (!this.level().isClientSide()) {
            this.setNoAi(false);
        }
    }

    @Override
    public net.minecraft.world.InteractionResult mobInteract(
            net.minecraft.world.entity.player.Player player, net.minecraft.world.InteractionHand mao) {
        // botao direito abre o cardapio (mesmo padrao do WanderingTrader)
        ItemStack naMao = player.getItemInHand(mao);
        if (!naMao.is(Items.VILLAGER_SPAWN_EGG) && !naMao.is(IntoxicantesMod.OVO_TRAFICANTE)
                && this.isAlive() && !this.isTrading() && !this.isBaby()) {
            if (mao == net.minecraft.world.InteractionHand.MAIN_HAND) {
                player.awardStat(net.minecraft.stats.Stats.TALKED_TO_VILLAGER);
            }
            if (!this.level().isClientSide()) {
                refreshTradeStock();
                if (this.getOffers().isEmpty()) {
                    return net.minecraft.world.InteractionResult.CONSUME;
                }
                this.setTradingPlayer(player);
                this.openTradingScreen(player, Component.translatable("commerce.intoxicantes.traficante.title"), 1);
                if (player instanceof net.minecraft.server.level.ServerPlayer serverPlayer) {
                    serverPlayer.sendSystemMessage(Component.translatable("commerce.intoxicantes.traficante.hint"), true);
                }
                return net.minecraft.world.InteractionResult.SUCCESS_SERVER;
            }
            return net.minecraft.world.InteractionResult.SUCCESS;
        }
        return super.mobInteract(player, mao);
    }

    @Override
    @Nullable
    public SpawnGroupData finalizeSpawn(ServerLevelAccessor accessor, DifficultyInstance difficulty,
            EntitySpawnReason reason, @Nullable SpawnGroupData data) {
        SpawnGroupData resultado = super.finalizeSpawn(accessor, difficulty, reason, data);
        refreshTradeStock();
        // invocado por ovo/comando: fica pra sempre (nao some longe)
        if (reason == EntitySpawnReason.SPAWN_ITEM_USE || reason == EntitySpawnReason.COMMAND) {
            this.setPersistenceRequired();
        }
        return resultado;
    }

    /** Nao reproduz: o nego e solo kkkk */
    @Override
    @Nullable
    public AgeableMob getBreedOffspring(ServerLevel level, AgeableMob outro) {
        return null;
    }

    /** Traficante e tipo andarilho: some se ninguem tiver perto (os invocados ficam). */
    @Override
    public boolean removeWhenFarAway(double distancia) {
        return distancia > 64.0 && !this.isPersistenceRequired() && !this.hasCustomName();
    }

    @Override
    protected void updateTrades(ServerLevel level) {
        refreshTradeStock();
    }

    @Override
    protected void rewardTradeXp(MerchantOffer offer) {
        this.overrideXp(this.getVillagerXp() + offer.getXp());
    }

    @Override
    protected void readAdditionalSaveData(net.minecraft.world.level.storage.ValueInput input) {
        super.readAdditionalSaveData(input);
        this.tradeStock.load(input);
    }

    @Override
    protected void addAdditionalSaveData(net.minecraft.world.level.storage.ValueOutput output) {
        super.addAdditionalSaveData(output);
        this.tradeStock.save(output);
    }

    /** Chamado pelo spawner do mod: avisa TODO MUNDO no chat (pura criacao de rumores kkkk). */
    public void anunciarChegada(ServerLevel level) {
        if (this.anunciouChegada) {
            return;
        }
        this.anunciouChegada = true;
        for (var p : level.players()) {
            p.sendSystemMessage(Component.translatable(
                    FRASES_CHEGADA[this.random.nextInt(FRASES_CHEGADA.length)],
                    this.getName(), p.getName()));
        }
        level.sendParticles(ParticleTypes.SMOKE,
                this.getX(), this.getY() + 1.8, this.getZ(), 12, 0.3, 0.4, 0.3, 0.02);
    }

    @Override
    public void die(DamageSource source) {
        if (!this.level().isClientSide()) {
            if (source.getEntity() instanceof Player matador) {
                matador.sendSystemMessage(Component.translatable(
                        this.random.nextBoolean()
                                ? "entity.intoxicantes.traficante.morte.1"
                                : "entity.intoxicantes.traficante.morte.2",
                        this.getName(), matador.getName()));
            }
            ((ServerLevel) this.level()).sendParticles(ParticleTypes.CLOUD,
                    this.getX(), this.getY() + 1.0, this.getZ(), 20, 0.4, 0.5, 0.4, 0.05);
        }
        super.die(source);
    }

    @Override
    protected void dropCustomDeathLoot(ServerLevel level, DamageSource source, boolean recentlyHit) {
        // "o cara deixou cair o estoque" kkkk
        this.spawnAtLocation(level, new ItemStack(IntoxicantesMod.BASEADO));
        if (this.random.nextInt(3) == 0) {
            this.spawnAtLocation(level, new ItemStack(IntoxicantesMod.COCAINA));
        }
        super.dropCustomDeathLoot(level, source, recentlyHit);
    }

    @Override
    protected SoundEvent getAmbientSound() {
        return SoundEvents.WANDERING_TRADER_AMBIENT;
    }

    @Override
    protected SoundEvent getHurtSound(DamageSource source) {
        return SoundEvents.WANDERING_TRADER_HURT;
    }

    @Override
    protected SoundEvent getDeathSound() {
        return SoundEvents.WANDERING_TRADER_DEATH;
    }
}
