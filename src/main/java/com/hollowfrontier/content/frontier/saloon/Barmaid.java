package com.hollowfrontier.content.frontier.saloon;

import java.util.Arrays;

import org.jspecify.annotations.Nullable;

import com.geckolib.animatable.manager.AnimatableManager;
import com.geckolib.animation.AnimationController;
import com.geckolib.animation.RawAnimation;
import com.geckolib.animation.object.PlayState;
import com.hollowfrontier.content.frontier.raid.RaidGoals;
import com.hollowfrontier.content.frontier.townsfolk.Townsfolk;
import com.hollowfrontier.registry.ModItems;
import com.hollowfrontier.registry.ModSounds;

import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.contents.TranslatableContents;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.DifficultyInstance;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.SpawnGroupData;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.LookAtTradingPlayerGoal;
import net.minecraft.world.entity.ai.goal.PanicGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.goal.TradeWithPlayerGoal;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.trading.MerchantOffer;
import net.minecraft.world.item.trading.MerchantOffers;
import net.minecraft.world.level.ItemLike;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.ServerLevelAccessor;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;

/**
 * Saloon barmaid: a Townsfolk who keeps to her post behind the bar (no wandering) and pours drinks for coins.
 * Each one is a {@link Hostess} with her own look, name, idle pose, lines (her own Chatter voice) and cheaper house
 * specialties.
 * Buys brewing goods. Outlaws treat her like any other townsfolk.
 */
public class Barmaid extends Townsfolk {
    private static final EntityDataAccessor<Integer> DATA_HOSTESS = SynchedEntityData.defineId(Barmaid.class, EntityDataSerializers.INT);
    // Ticks left of the pour animation after a trade (synced, like the outlaws' shoot ticks).
    private static final EntityDataAccessor<Integer> DATA_POUR_TICKS = SynchedEntityData.defineId(Barmaid.class, EntityDataSerializers.INT);
    private static final RawAnimation WALK = RawAnimation.begin().thenLoop("move.walk");
    private static final RawAnimation POUR = RawAnimation.begin().thenPlay("interact.pour");
    private static final RawAnimation[] IDLES = Arrays.stream(Hostess.values())
            .map(h -> RawAnimation.begin().thenLoop("idle." + h.id())).toArray(RawAnimation[]::new);
    private static final int POUR_TICKS = 24;

    public Barmaid(EntityType<? extends Barmaid> type, Level level) {
        super(type, level);
    }

    public Hostess getHostess() {
        return Hostess.values()[entityData.get(DATA_HOSTESS)];
    }

    /** Also names her after the hostess (her name shows when you look at her), unless a player renamed her. */
    public void setHostess(Hostess hostess) {
        entityData.set(DATA_HOSTESS, hostess.ordinal());
        Component name = getCustomName();
        if (name == null || (name.getContents() instanceof TranslatableContents t && t.getKey().startsWith("entity.hollowfrontier.barmaid."))) {
            setCustomName(Component.translatable(hostess.nameKey()));
        }
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
        builder.define(DATA_HOSTESS, 0);
        builder.define(DATA_POUR_TICKS, 0);
    }

    @Override
    protected void addAdditionalSaveData(ValueOutput output) {
        super.addAdditionalSaveData(output);
        output.putString("hostess", getHostess().id());
    }

    @Override
    protected void readAdditionalSaveData(ValueInput input) {
        super.readAdditionalSaveData(input);
        setHostess(Hostess.byId(input.getStringOr("hostess", Hostess.RUBY.id())));
    }

    @Override
    public SpawnGroupData finalizeSpawn(ServerLevelAccessor level, DifficultyInstance difficulty, EntitySpawnReason reason, @Nullable SpawnGroupData groupData) {
        Hostess[] all = Hostess.values();
        setHostess(all[level.getRandom().nextInt(all.length)]);
        return super.finalizeSpawn(level, difficulty, reason, groupData);
    }

    // Unnamed barmaids (e.g. summoned with data) still go by their hostess name in menus.
    @Override
    protected Component getTypeName() {
        return Component.translatable(getHostess().nameKey());
    }

    @Override
    protected void registerGoals() {
        goalSelector.addGoal(0, new FloatGoal(this));
        goalSelector.addGoal(1, new TradeWithPlayerGoal(this));
        goalSelector.addGoal(1, new RaidGoals.Shelter(this));
        goalSelector.addGoal(1, fleeOutlaws(this, 8.0F));
        goalSelector.addGoal(1, new PanicGoal(this, 0.6));
        goalSelector.addGoal(2, new LookAtTradingPlayerGoal(this));
        goalSelector.addGoal(6, new LookAtPlayerGoal(this, Player.class, 8.0F));
        goalSelector.addGoal(7, new RandomLookAroundGoal(this));
    }

    @Override
    public void notifyTrade(MerchantOffer offer) {
        super.notifyTrade(offer);
        entityData.set(DATA_POUR_TICKS, POUR_TICKS);
    }

    @Override
    public void tick() {
        super.tick();
        if (!level().isClientSide()) {
            int left = entityData.get(DATA_POUR_TICKS);
            if (left > 0) {
                entityData.set(DATA_POUR_TICKS, left - 1);
            }
        }
    }

    // The sound of a drink being poured when a trade goes through.
    @Override
    public SoundEvent getNotifyTradeSound() {
        return ModSounds.DRINK_POUR.get();
    }

    @Override
    protected void updateTrades(ServerLevel level) {
        MerchantOffers offers = getOffers();
        Item copper = ModItems.COPPER_COIN.get();
        Item gold = ModItems.GOLD_COIN.get();
        // Brewing goods
        offers.add(buy(Items.WHEAT, 20, copper, 1));
        offers.add(buy(Items.SUGAR, 16, copper, 1));
        offers.add(buy(Items.SWEET_BERRIES, 16, copper, 1));
        offers.add(buy(Items.GLASS_BOTTLE, 8, copper, 1));
        // Drinks, cheapest first; her house specialties cost a coin less (or come two to a coin)
        offers.add(pour(copper, 1, ModItems.SARSAPARILLA.get()));
        offers.add(pour(copper, 1, ModItems.COWBOY_COFFEE.get()));
        offers.add(pour(copper, 1, ModItems.FRONTIER_BEER.get()));
        offers.add(pour(copper, 2, ModItems.RYE_WHISKEY.get()));
        offers.add(pour(copper, 3, ModItems.MEZCAL.get()));
        offers.add(pour(copper, 4, ModItems.MOONSHINE.get()));
        offers.add(pour(copper, 3, ModItems.SNAKE_OIL.get()));
        offers.add(buy(gold, 1, ModItems.RYE_WHISKEY.get(), 6));
    }

    // 35c: her offers are built once, so December's two drinks go on the list when trading opens in December and come off
    // it when trading opens at any other time. Whatever she already has is left alone (its uses and prices keep).
    @Override
    public net.minecraft.world.InteractionResult mobInteract(Player player, net.minecraft.world.InteractionHand hand) {
        if (level() instanceof ServerLevel server) {
            seasonal(com.hollowfrontier.content.frontier.season.Season.isDecember(server));
        }
        return super.mobInteract(player, hand);
    }

    /** Puts December's drinks on her list, or takes them off (public so game tests can drive it). */
    public void seasonal(boolean december) {
        MerchantOffers offers = getOffers();
        Item eggnog = ModItems.EGGNOG.get(), cider = ModItems.HOT_CIDER.get();
        boolean pouring = offers.stream().anyMatch(o -> o.getResult().is(eggnog) || o.getResult().is(cider));
        if (december && !pouring) {
            Item copper = ModItems.COPPER_COIN.get();
            offers.add(pour(copper, 2, eggnog));
            offers.add(pour(copper, 2, cider));
        } else if (!december && pouring) {
            offers.removeIf(o -> o.getResult().is(eggnog) || o.getResult().is(cider));
        }
    }

    private MerchantOffer pour(ItemLike coin, int price, Item drink) {
        if (!getHostess().isSpecialty(drink)) {
            return buy(coin, price, drink, 1);
        }
        return price > 1 ? buy(coin, price - 1, drink, 1) : buy(coin, 1, drink, 2);
    }

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
        controllers.add(new AnimationController<Barmaid>("base", 5, test -> test.setAndContinue(test.isMoving()
                ? WALK : IDLES[test.animatable().getHostess().ordinal()])));
        controllers.add(new AnimationController<Barmaid>("pour",
                test -> test.animatable().entityData.get(DATA_POUR_TICKS) > 0 ? test.setAndContinue(POUR) : PlayState.STOP));
    }
}
