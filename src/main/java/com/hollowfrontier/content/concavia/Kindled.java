package com.hollowfrontier.content.concavia;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import org.jspecify.annotations.Nullable;

import com.geckolib.animatable.GeoEntity;
import com.geckolib.animatable.instance.AnimatableInstanceCache;
import com.geckolib.animatable.manager.AnimatableManager;
import com.geckolib.constant.DefaultAnimations;
import com.geckolib.util.GeckoLibUtil;
import com.hollowfrontier.content.frontier.talk.Chatter;
import com.hollowfrontier.content.hollow.creature.GrayProbe;
import com.hollowfrontier.registry.ModCriteria;
import com.hollowfrontier.registry.ModItems;
import com.hollowfrontier.registry.ModTags;
import com.hollowfrontier.worldgen.structure.ModStructures;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.DifficultyInstance;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.AgeableMob;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.SpawnGroupData;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.AvoidEntityGoal;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.LookAtTradingPlayerGoal;
import net.minecraft.world.entity.ai.goal.MoveTowardsRestrictionGoal;
import net.minecraft.world.entity.ai.goal.PanicGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.goal.TradeWithPlayerGoal;
import net.minecraft.world.entity.ai.goal.WaterAvoidingRandomStrollGoal;
import net.minecraft.world.entity.npc.villager.AbstractVillager;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.trading.ItemCost;
import net.minecraft.world.item.trading.MerchantOffer;
import net.minecraft.world.item.trading.MerchantOffers;
import net.minecraft.world.level.ItemLike;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.ServerLevelAccessor;

/**
 * A Kindled (12a): one of the last Concavians, tall and pale, keeping to the sanctum of a crystal ruin. They trade in phlogiston
 * shards, buying what the Grays leave behind (to keep it out of their hands, and to learn from it) and selling their
 * crystal stone and metal. Each talk tells the player the next piece of their story ({@link #LINES}), as far as their
 * {@link KindledStanding} allows: the fall of Concavia to strangers they let in; then, to a friend, what the Grays do
 * with the frontier's drifters (the outlaws, and the men in black); then, to an ally, the sigil, and the last king who
 * went into the inner sun (12b's Archon). An ally also gets two trades no one else does. They won't deal with anyone who's
 * hurt one of them; hurting one rouses the guardians. Never despawns; keeps near where it was placed (home 12).
 */
public class Kindled extends AbstractVillager implements GeoEntity {
    private static final int MAX_USES = 12;
    public static final int HOME_RADIUS = 12;
    /** Story lines, {@code message.hollowfrontier.kindled.line.<n>}: the first three for anyone, then three for friends, three for allies. */
    public static final int LINES = 9;
    private static final int TIER = 3;
    /** How far out (in chunks) a Kindled knows of a Sun Temple. */
    public static final int TEMPLE_SEARCH = 100;

    private final AnimatableInstanceCache geoCache = GeckoLibUtil.createInstanceCache(this);
    // The ally's trades, taken off the list while someone else is trading (offers are the merchant's, not per player)
    private final List<MerchantOffer> alliedOffersHeld = new ArrayList<>();

    public Kindled(EntityType<? extends Kindled> type, Level level) {
        super(type, level);
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Mob.createMobAttributes()
                .add(Attributes.MAX_HEALTH, 40.0)
                .add(Attributes.MOVEMENT_SPEED, 0.45)
                .add(Attributes.FOLLOW_RANGE, 32.0);
    }

    @Override
    public SpawnGroupData finalizeSpawn(ServerLevelAccessor level, DifficultyInstance difficulty, EntitySpawnReason reason, @Nullable SpawnGroupData groupData) {
        setHomeTo(blockPosition(), HOME_RADIUS);
        return super.finalizeSpawn(level, difficulty, reason, groupData);
    }

    @Override
    protected void registerGoals() {
        goalSelector.addGoal(0, new FloatGoal(this));
        goalSelector.addGoal(1, new TradeWithPlayerGoal(this));
        goalSelector.addGoal(1, new AvoidEntityGoal<>(this, GrayProbe.class, 10.0F, 0.5, 0.6));
        goalSelector.addGoal(1, new PanicGoal(this, 0.6));
        goalSelector.addGoal(2, new LookAtTradingPlayerGoal(this));
        goalSelector.addGoal(4, new MoveTowardsRestrictionGoal(this, 0.4));
        goalSelector.addGoal(5, new WaterAvoidingRandomStrollGoal(this, 0.3));
        goalSelector.addGoal(6, new LookAtPlayerGoal(this, Player.class, 10.0F));
        goalSelector.addGoal(7, new RandomLookAroundGoal(this));
    }

    @Override
    public InteractionResult mobInteract(Player player, InteractionHand hand) {
        if (!isAlive() || isTrading()) {
            return super.mobInteract(player, hand);
        }
        if (level() instanceof ServerLevel && player instanceof ServerPlayer serverPlayer) {
            if (KindledStanding.wronged(player)) {
                say(serverPlayer, "message.hollowfrontier.kindled.refuse");
                return InteractionResult.SUCCESS_SERVER;
            }
            ModCriteria.fire(serverPlayer, ModCriteria.MET_KINDLED);
            talk(serverPlayer);
            showOffersTo(player);
            setTradingPlayer(player);
            openTradingScreen(player, getDisplayName(), 1);
        }
        return InteractionResult.SUCCESS_SERVER;
    }

    /** The next line of the story, if the player has earned it; otherwise, now and then, a greeting. */
    public void talk(ServerPlayer player) {
        KindledStanding standing = KindledStanding.of(player);
        if (standing.heard() < told(player)) {
            say(player, "message.hollowfrontier.kindled.line." + standing.heard());
            KindledStanding.heardOne(player);
            ModCriteria.fire(player, ModCriteria.KINDLED_STORY, standing.heard() + 1);
            // 12b: the last line is of the sun's temple, and they say where it stands
            if (standing.heard() + 1 == LINES) {
                pointToTemple(player);
            }
        } else if (KindledStanding.allied(player) && getRandom().nextInt(3) == 0) {
            pointToTemple(player);
        } else if (getRandom().nextInt(2) == 0) {
            Chatter.talk(this, player); // now and then, a word (Chatter: their voice is "kindled")
        }
    }

    /** Tells an ally where the nearest Sun Temple stands (within {@link #TEMPLE_SEARCH} chunks), if one does. */
    public void pointToTemple(ServerPlayer player) {
        if (!(level() instanceof ServerLevel level)) {
            return;
        }
        BlockPos temple = level.findNearestMapStructure(ModStructures.SUN_TEMPLES, blockPosition(), TEMPLE_SEARCH, false);
        if (temple != null) {
            player.sendSystemMessage(Component.translatable("message.hollowfrontier.npc.say", getDisplayName(),
                    Component.translatable("message.hollowfrontier.kindled.temple", temple.getX(), temple.getZ()).withStyle(ChatFormatting.AQUA)));
        }
    }

    /** How much of the story a player may hear: a tier to anyone, two to a friend, all of it to an ally. */
    public static int told(Player player) {
        return KindledStanding.allied(player) ? LINES : KindledStanding.friend(player) ? 2 * TIER : TIER;
    }

    private void say(ServerPlayer player, String key) {
        player.sendSystemMessage(Component.translatable("message.hollowfrontier.npc.say", getDisplayName(),
                Component.translatable(key).withStyle(ChatFormatting.AQUA)));
    }

    // ---------------------------------------------------------------- trading

    @Override
    protected void updateTrades(ServerLevel level) {
        MerchantOffers offers = getOffers();
        Item phlogiston = ModItems.PHLOGISTON_SHARD.get();
        // What the Grays leave behind, bought up and taken out of their reach
        offers.add(buy(ModItems.GRAY_TECH_FRAGMENT.get(), 1, phlogiston, 2));
        offers.add(buy(ModItems.SIGNAL_LOCATOR.get(), 1, phlogiston, 4));
        offers.add(buy(ModItems.STRANGE_SKULL.get(), 1, phlogiston, 6));
        // Plainer things they're short of
        offers.add(buy(Items.AMETHYST_SHARD, 12, phlogiston, 1));
        offers.add(buy(ModItems.MAMMOTH_TUSK.get(), 2, phlogiston, 1));
        // Their stone, and their metal
        offers.add(buy(phlogiston, 1, ModItems.CONCAVIAN_CRYSTAL.get(), 8));
        offers.add(buy(phlogiston, 1, ModItems.CRYSTAL_BRICKS.get(), 8));
        offers.add(buy(phlogiston, 2, ModItems.CHISELED_CRYSTAL_BRICKS.get(), 4));
        offers.add(buy(phlogiston, 3, ModItems.STARSTEEL_INGOT.get(), 1));
        offers.add(buy(phlogiston, 2, ModItems.PHLOGISTON_LAMP.get(), 1));
        offers.add(buy(phlogiston, 1, ModItems.CONCAVIAN_CODEX.get(), 1)); // 12b
        offers.addAll(alliedOffers());
    }

    /** For allies only: a sigil to replace a lost one, and starsteel by the block, cheap. */
    private static List<MerchantOffer> alliedOffers() {
        Item phlogiston = ModItems.PHLOGISTON_SHARD.get();
        return List.of(buy(phlogiston, 6, ModItems.KINDLED_SIGIL.get(), 1), buy(phlogiston, 9, ModItems.STARSTEEL_BLOCK.get(), 1));
    }

    public static boolean isAlliedOffer(MerchantOffer offer) {
        return offer.getResult().is(ModItems.KINDLED_SIGIL.get()) || offer.getResult().is(ModItems.STARSTEEL_BLOCK.get());
    }

    /**
     * Puts the ally's trades on the list for an ally, and takes them off for anyone else; and adds what their ruin's
     * restoration has opened (15c), for anyone.
     */
    public void showOffersTo(Player player) {
        MerchantOffers offers = getOffers();
        if (level() instanceof ServerLevel level) {
            for (MerchantOffer offer : Restoration.stageOffers(Restoration.stageAt(level, blockPosition()))) {
                if (offers.stream().noneMatch(o -> ItemStack.isSameItem(o.getResult(), offer.getResult()))) {
                    offers.add(offer);
                }
            }
        }
        if (KindledStanding.allied(player)) {
            if (offers.stream().noneMatch(Kindled::isAlliedOffer)) {
                offers.addAll(alliedOffersHeld.isEmpty() ? alliedOffers() : alliedOffersHeld);
            }
            alliedOffersHeld.clear();
        } else {
            for (MerchantOffer offer : List.copyOf(offers)) {
                if (isAlliedOffer(offer)) {
                    offers.remove(offer);
                    alliedOffersHeld.add(offer);
                }
            }
        }
    }

    protected static MerchantOffer buy(ItemLike cost, int costCount, ItemLike result, int resultCount) {
        return new MerchantOffer(new ItemCost(cost, costCount), Optional.empty(), new ItemStack(result, resultCount), MAX_USES, 1, 0.05F);
    }

    /** Every trade wins a little favour; a trade in the Grays' things wins more. */
    @Override
    protected void rewardTradeXp(MerchantOffer offer) {
        if (getTradingPlayer() instanceof ServerPlayer player) {
            KindledStanding.add(player, offer.getCostA().is(ModTags.Items.STRANGE_GOODS) ? KindledStanding.GRAY_TRADE : KindledStanding.TRADE);
        }
    }

    @Override
    public SoundEvent getNotifyTradeSound() {
        return SoundEvents.AMETHYST_BLOCK_CHIME;
    }

    @Override
    protected @Nullable SoundEvent getTradeUpdatedSound(boolean validTrade) {
        return validTrade ? SoundEvents.AMETHYST_CLUSTER_STEP : null;
    }

    @Override
    public void playCelebrateSound() {
    }

    // ---------------------------------------------------------------- harm

    /** A Kindled hurt by a player costs them favour, and the guardians nearby take it up. */
    @Override
    public boolean hurtServer(ServerLevel level, DamageSource source, float damage) {
        boolean hurt = super.hurtServer(level, source, damage);
        if (hurt && source.getEntity() instanceof ServerPlayer player && !player.isCreative()) {
            KindledStanding.add(player, KindledStanding.WRONG);
            CrystalGuardian.rally(level, position(), player);
        }
        return hurt;
    }

    @Override
    public void die(DamageSource source) {
        if (source.getEntity() instanceof ServerPlayer player && !player.isCreative()) {
            KindledStanding.add(player, KindledStanding.MIN - KindledStanding.MAX);
        }
        super.die(source);
    }

    @Override
    public AgeableMob getBreedOffspring(ServerLevel level, AgeableMob partner) {
        return null;
    }

    @Override
    public boolean removeWhenFarAway(double distSqr) {
        return false;
    }

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
        controllers.add(DefaultAnimations.genericWalkIdleController());
    }

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return geoCache;
    }
}
