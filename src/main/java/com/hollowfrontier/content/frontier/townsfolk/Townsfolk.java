package com.hollowfrontier.content.frontier.townsfolk;

import java.util.Optional;

import org.jspecify.annotations.Nullable;

import com.geckolib.animatable.GeoEntity;
import com.geckolib.animatable.instance.AnimatableInstanceCache;
import com.geckolib.animatable.manager.AnimatableManager;
import com.geckolib.constant.DefaultAnimations;
import com.geckolib.util.GeckoLibUtil;
import com.hollowfrontier.content.frontier.bank.Banking;
import com.hollowfrontier.content.frontier.law.Wanted;
import com.hollowfrontier.content.frontier.outlaw.AbstractOutlaw;
import com.hollowfrontier.content.frontier.raid.RaidGoals;
import com.hollowfrontier.content.frontier.raid.TownRaids;
import com.hollowfrontier.content.frontier.talk.Chatter;
import com.hollowfrontier.content.steam.SteamCanisterItem;
import com.hollowfrontier.registry.ModEntities;
import com.hollowfrontier.registry.ModItems;
import com.hollowfrontier.registry.ModSounds;
import com.hollowfrontier.registry.ModTags;

import net.minecraft.network.chat.Component;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.DifficultyInstance;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.AgeableMob;
import net.minecraft.world.entity.EntitySelector;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.SpawnGroupData;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.AvoidEntityGoal;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.LookAtTradingPlayerGoal;
import net.minecraft.world.entity.ai.goal.OpenDoorGoal;
import net.minecraft.world.entity.ai.goal.PanicGoal;
import net.minecraft.world.entity.ai.goal.TradeWithPlayerGoal;
import net.minecraft.world.entity.ai.goal.WaterAvoidingRandomStrollGoal;
import net.minecraft.world.entity.npc.villager.AbstractVillager;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.item.enchantment.EnchantmentInstance;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.trading.ItemCost;
import net.minecraft.world.item.trading.MerchantOffer;
import net.minecraft.world.item.trading.MerchantOffers;
import net.minecraft.world.level.ItemLike;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.ServerLevelAccessor;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;

/**
 * Frontier town trader. Fixed coin trades (no professions/levels): buys raw goods for copper coins,
 * sells ammo and frontier gear. Never despawns; flees outlaws (not jailed ones) and calls nearby lawmen on whoever
 * hurts them. Each is one of the {@link Folk} (look and name). When outlaws raid the town they get indoors and stay
 * there (RaidGoals.Shelter), shutting doors behind them; a robbed town charges a coin more for everything. Spawned by
 * towns (phase 6) or spawn egg.
 */
public class Townsfolk extends AbstractVillager implements GeoEntity {
    private static final int MAX_USES = 12;
    private static final int GUN_BOOK_USES = 3;
    private static final EntityDataAccessor<Integer> DATA_FOLK = SynchedEntityData.defineId(Townsfolk.class, EntityDataSerializers.INT);

    private final AnimatableInstanceCache geoCache = GeckoLibUtil.createInstanceCache(this);
    // Set when the folk came from NBT (a town building chose it), so spawning doesn't reroll it.
    private boolean folkChosen;
    private @Nullable BlockPos workplace;
    // Not one of the town's own (a spawn egg's, a freed man): the town doesn't replace them (TownVacancies).
    private boolean stranger;

    public Townsfolk(EntityType<? extends Townsfolk> type, Level level) {
        super(type, level);
        getNavigation().setCanOpenDoors(true);
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Mob.createMobAttributes()
                .add(Attributes.MAX_HEALTH, 20.0)
                .add(Attributes.MOVEMENT_SPEED, 0.5)
                .add(Attributes.FOLLOW_RANGE, 32.0);
    }

    /** Runs from armed outlaws; a jailbird behind bars is nothing to fear. */
    protected static AvoidEntityGoal<AbstractOutlaw> fleeOutlaws(Townsfolk mob, float distance) {
        return new AvoidEntityGoal<>(mob, AbstractOutlaw.class, e -> !((AbstractOutlaw) e).isJailed(), distance, 0.5, 0.6,
                EntitySelector.NO_CREATIVE_OR_SPECTATOR::test);
    }

    public Folk getFolk() {
        return Folk.values()[entityData.get(DATA_FOLK)];
    }

    public void setFolk(Folk folk) {
        entityData.set(DATA_FOLK, folk.ordinal());
        folkChosen = true;
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
        builder.define(DATA_FOLK, 0);
    }

    /** Where this townsperson works and lives (24c): where they first stood. Null until their first tick in the world. */
    public @Nullable BlockPos getWorkplace() {
        return workplace;
    }

    /** Sets where they work, for someone taking over a post. */
    public void setWorkplace(BlockPos pos) {
        workplace = pos;
    }

    /** Whether their town would replace them. */
    public boolean isResident() {
        return !stranger;
    }

    public void markStranger() {
        stranger = true;
    }

    @Override
    protected void addAdditionalSaveData(ValueOutput output) {
        super.addAdditionalSaveData(output);
        output.putString("folk", getFolk().id());
        if (workplace != null) {
            output.store("workplace", BlockPos.CODEC, workplace);
        }
        if (stranger) {
            output.putBoolean("stranger", true);
        }
    }

    @Override
    protected void readAdditionalSaveData(ValueInput input) {
        super.readAdditionalSaveData(input);
        input.getString("folk").ifPresent(id -> setFolk(Folk.byId(id)));
        workplace = input.read("workplace", BlockPos.CODEC).orElse(null);
        stranger = input.getBooleanOr("stranger", false);
    }

    // Asleep in bed: no walking off with the blankets.
    @Override
    protected boolean isImmobile() {
        return super.isImmobile() || isSleeping();
    }

    @Override
    public SpawnGroupData finalizeSpawn(ServerLevelAccessor level, DifficultyInstance difficulty, EntitySpawnReason reason, @Nullable SpawnGroupData groupData) {
        if (!folkChosen) {
            setFolk(Folk.RANDOM[level.getRandom().nextInt(Folk.RANDOM.length)]);
        }
        // A town's own come with its buildings or arrive to replace someone (TownVacancies).
        stranger = reason != EntitySpawnReason.STRUCTURE && reason != EntitySpawnReason.EVENT;
        return super.finalizeSpawn(level, difficulty, reason, groupData);
    }

    @Override
    protected Component getTypeName() {
        return Component.translatable(getFolk().nameKey());
    }

    @Override
    public boolean hurtServer(ServerLevel level, DamageSource source, float damage) {
        boolean hurt = super.hurtServer(level, source, damage);
        if (hurt && source.getEntity() instanceof LivingEntity attacker) {
            Lawman.callForHelp(level, this, attacker);
        }
        return hurt;
    }

    @Override
    protected void registerGoals() {
        goalSelector.addGoal(0, new FloatGoal(this));
        goalSelector.addGoal(1, new TradeWithPlayerGoal(this));
        goalSelector.addGoal(1, new RaidGoals.Shelter(this));
        goalSelector.addGoal(1, new OpenDoorGoal(this, true));
        goalSelector.addGoal(1, fleeOutlaws(this, 12.0F));
        goalSelector.addGoal(1, new PanicGoal(this, 0.6));
        goalSelector.addGoal(2, new LookAtTradingPlayerGoal(this));
        goalSelector.addGoal(4, new Routine.RoutineGoal(this));
        goalSelector.addGoal(5, new WaterAvoidingRandomStrollGoal(this, 0.35));
        goalSelector.addGoal(6, new LookAtPlayerGoal(this, Player.class, 8.0F));
    }

    @Override
    public InteractionResult mobInteract(Player player, InteractionHand hand) {
        if (isSleeping() && !level().isClientSide()) {
            stopSleeping(); // woken, grumbling, but they'll trade
        }
        // Folk won't do business with a known outlaw.
        if (isAlive() && Wanted.stars(player) >= 2) {
            if (!level().isClientSide()) {
                player.sendOverlayMessage(Component.translatable("message.hollowfrontier.townsfolk.refuse"));
            }
            return InteractionResult.SUCCESS;
        }
        // 32d: a postmaster hands a newcomer the Polar Survey's uncollected letter first, before any business.
        if (isAlive() && getType() == ModEntities.TOWNSFOLK.get() && getFolk() == Folk.POSTMASTER
                && level() instanceof ServerLevel serverLevel && player instanceof ServerPlayer serverPlayer
                && com.hollowfrontier.content.frontier.survey.SurveyLetterItem.handOver(serverLevel, serverPlayer, blockPosition())) {
            serverPlayer.sendSystemMessage(Component.translatable("message.hollowfrontier.survey.postmaster"));
            return InteractionResult.SUCCESS_SERVER;
        }
        // 48b: the Gazette's editor pays, once, for a journal with ten strange doings in it
        if (isAlive() && getType() == ModEntities.TOWNSFOLK.get() && getFolk() == Folk.EDITOR && player instanceof ServerPlayer serverPlayer
                && com.hollowfrontier.content.journal.JournalSeen.sellTally(serverPlayer)) {
            return InteractionResult.SUCCESS_SERVER;
        }
        // 35b: in December a postmaster (or, with no post office, the storekeeper) has this year's parcel for you
        if (isAlive() && getType() == ModEntities.TOWNSFOLK.get() && level() instanceof ServerLevel serverLevel
                && player instanceof ServerPlayer serverPlayer
                && com.hollowfrontier.content.frontier.season.ChristmasParcelItem.due(serverLevel, serverPlayer)
                && com.hollowfrontier.content.frontier.season.ChristmasParcelItem.handsOver(serverLevel, this)
                && com.hollowfrontier.content.frontier.season.ChristmasParcelItem.handOver(serverLevel, serverPlayer)) {
            serverPlayer.sendSystemMessage(Component.translatable("message.hollowfrontier.parcel." + getFolk().id()));
            return InteractionResult.SUCCESS_SERVER;
        }
        // Sneaking up to a banker: your safe deposit box in this town's bank, not his trades.
        if (isAlive() && player.isSecondaryUseActive() && getType() == ModEntities.TOWNSFOLK.get() && getFolk() == Folk.BANKER) {
            if (level() instanceof ServerLevel serverLevel && player instanceof ServerPlayer serverPlayer) {
                Banking.visit(serverLevel, serverPlayer, this);
            }
            return InteractionResult.SUCCESS_SERVER;
        }
        if (isAlive() && !isTrading() && !isBaby()) {
            if (level() instanceof ServerLevel serverLevel && !getOffers().isEmpty()) {
                hardTimes(serverLevel);
                com.hollowfrontier.content.frontier.stake.StakeEvents.offerDeed(serverLevel, this, player); // 47b: the deed, for a neighbour without a place
                knownHere(serverLevel, player);
                if (player instanceof ServerPlayer serverPlayer) {
                    Chatter.talk(this, serverPlayer);
                }
                setTradingPlayer(player);
                openTradingScreen(player, getDisplayName(), 1);
            }
            return InteractionResult.SUCCESS_SERVER;
        }
        return super.mobInteract(player, hand);
    }

    // After a robbery everything costs a coin more (the banker's Chatter says why).
    private void hardTimes(ServerLevel level) {
        boolean robbed = TownRaids.isRobbed(level, blockPosition());
        for (MerchantOffer offer : getOffers()) {
            offer.setSpecialPriceDiff(robbed ? 1 : 0);
        }
    }

    // 47a: a neighbour gets a coin off anything dear, a pillar of the town a coin more off what's dearer still
    private void knownHere(ServerLevel level, Player player) {
        var rank = com.hollowfrontier.content.frontier.town.Towns.keyAt(level, blockPosition())
                .map(town -> com.hollowfrontier.content.frontier.stake.Standing.rank(player, town)).orElse(com.hollowfrontier.content.frontier.stake.Standing.Rank.STRANGER);
        for (MerchantOffer offer : getOffers()) {
            int off = (rank.atLeast(com.hollowfrontier.content.frontier.stake.Standing.Rank.NEIGHBOUR) && offer.getBaseCostA().getCount() >= 3 ? 1 : 0)
                    + (rank.atLeast(com.hollowfrontier.content.frontier.stake.Standing.Rank.PILLAR) && offer.getBaseCostA().getCount() >= 6 ? 1 : 0);
            offer.setSpecialPriceDiff(offer.getSpecialPriceDiff() - off);
        }
    }

    // Folk say hello to whoever comes near (Chatter decides whether it's been long enough).
    @Override
    public void tick() {
        super.tick();
        if (!level().isClientSide() && workplace == null) {
            workplace = blockPosition(); // where they were put: their shop or their house
        }
        if (!level().isClientSide() && (tickCount + getId()) % 20 == 0 && !isSleeping()) {
            Chatter.lookAround(this);
        }
    }

    @Override
    protected void updateTrades(ServerLevel level) {
        MerchantOffers offers = getOffers();
        Item copper = ModItems.COPPER_COIN.get();
        Item gold = ModItems.GOLD_COIN.get();
        // Each trade sells what its shop would (24c review): there's no common list, and only the gunsmith sells guns.
        switch (getFolk()) {
            // The general store: ammunition and caps, rope, the paper, a lamp, coffee; it buys the farms' and trappers' goods.
            case SHOPKEEPER -> {
                offers.add(buy(copper, 2, ModItems.PERCUSSION_CAP.get(), 16));
                offers.add(buy(copper, 3, ModItems.REVOLVER_CARTRIDGE.get(), 8));
                offers.add(buy(copper, 3, ModItems.RIFLE_CARTRIDGE.get(), 5));
                offers.add(buy(copper, 4, ModItems.BUCKSHOT_CARTRIDGE.get(), 4));
                offers.add(buy(copper, 5, ModItems.LASSO.get(), 1));
                offers.add(buy(copper, 1, ModItems.FRONTIER_GAZETTE.get(), 1));
                offers.add(buy(copper, 2, Items.LANTERN, 1));
                offers.add(buy(copper, 2, ModItems.COWBOY_COFFEE.get(), 2));
                offers.add(buy(Items.WHEAT, 16, copper, 1));
                offers.add(buy(Items.LEATHER, 4, copper, 1));
                offers.add(buy(Items.COPPER_INGOT, 6, copper, 1));
            }
            // The bank: changing coin, and buying gold and gems off the prospectors.
            case BANKER -> {
                offers.add(buy(copper, 9, gold, 1));
                offers.add(buy(gold, 1, copper, 9));
                offers.add(buy(Items.GOLD_INGOT, 1, gold, 1));
                offers.add(buy(Items.EMERALD, 1, copper, 2));
                offers.add(buy(Items.DIAMOND, 1, gold, 2));
                offers.add(buy(copper, 1, ModItems.FRONTIER_GAZETTE.get(), 1));
            }
            // The smithy: horseshoes, dynamite for the claims, the one good gun book; iron and coal bought in.
            case BLACKSMITH -> {
                offers.add(buy(gold, 1, ModItems.IRON_HORSESHOES.get(), 1));
                offers.add(buy(copper, 3, ModItems.DYNAMITE.get(), 2));
                offers.add(buy(copper, 2, Items.SHEARS, 1));
                offers.add(buy(copper, 1, Items.IRON_CHAIN, 2));
                gunBook(level).ifPresent(offers::add);
                offers.add(buy(Items.IRON_INGOT, 4, copper, 1));
                offers.add(buy(Items.COAL, 8, copper, 1));
            }
            // The livery: tack and feed; he buys hides, snakeskins and grain.
            case WRANGLER -> {
                offers.add(buy(gold, 2, ModItems.SADDLEBAGS.get(), 1));
                offers.add(buy(gold, 1, Items.SADDLE, 1));
                offers.add(buy(copper, 2, Items.LEAD, 1));
                offers.add(buy(copper, 5, ModItems.LASSO.get(), 1));
                offers.add(buy(copper, 3, Items.HAY_BLOCK, 2));
                offers.add(buy(ModItems.SNAKESKIN.get(), 2, copper, 1));
                offers.add(buy(Items.LEATHER, 4, copper, 1));
                offers.add(buy(Items.WHEAT, 16, copper, 1));
            }
            // The prospector: dynamite and what a claim needs; he buys what the hills give up.
            case PROSPECTOR -> {
                offers.add(buy(copper, 3, ModItems.DYNAMITE.get(), 2));
                offers.add(buy(copper, 2, Items.TORCH, 8));
                offers.add(buy(copper, 6, Items.IRON_PICKAXE, 1));
                offers.add(buy(ModItems.SNAKESKIN.get(), 2, copper, 1));
                offers.add(buy(Items.GOLD_NUGGET, 6, copper, 1));
                offers.add(buy(Items.COPPER_INGOT, 6, copper, 1));
                offers.add(buy(Items.RAW_IRON, 4, copper, 1));
                // 32d: a Polar Compass, for anyone set on the pole and short of luck in the chests
                offers.add(buy(gold, 3, ModItems.POLAR_COMPASS.get(), 1));
                // 28: and the Hollow Earth's relics, each for what it's worth to a collector back East
                for (var relic : ModItems.RELICS) {
                    offers.add(buy(relic.get(), 1, gold, relic.get().price()));
                }
                // 32c: and the Grays' odds and ends, which he can't explain either
                for (var gadget : ModItems.GADGETS) {
                    offers.add(buy(gadget.get(), 1, gold, gadget.get().price()));
                }
            }
            // The railroad: track, steam to try a handcar with, the valve that's hard to make, and he buys crusher dust
            // (for the smelter down the line) and coal for the locomotives.
            case ENGINEER -> {
                offers.add(buy(copper, 2, Items.RAIL, 16));
                offers.add(buy(gold, 1, Items.POWERED_RAIL, 6));
                ItemStack canister = new ItemStack(ModItems.STEAM_CANISTER.get());
                SteamCanisterItem.setSteam(canister, SteamCanisterItem.CAPACITY);
                offers.add(new MerchantOffer(new ItemCost(copper, 6), Optional.empty(), canister, MAX_USES, 1, 0.05F));
                offers.add(buy(gold, 1, ModItems.PRESSURE_VALVE.get(), 1));
                offers.add(buy(ModItems.IRON_DUST.get(), 4, copper, 1));
                offers.add(buy(ModItems.COPPER_DUST.get(), 6, copper, 1));
                offers.add(buy(ModItems.GOLD_DUST.get(), 2, copper, 1));
                offers.add(buy(Items.COAL, 8, copper, 1));
            }
            // 24c. The doctor: a healing draught, snake oil and a golden carrot; he buys what his remedies are made of.
            case DOCTOR -> {
                ItemStack healing = new ItemStack(Items.POTION);
                healing.set(net.minecraft.core.component.DataComponents.POTION_CONTENTS,
                        new net.minecraft.world.item.alchemy.PotionContents(net.minecraft.world.item.alchemy.Potions.HEALING));
                offers.add(new MerchantOffer(new ItemCost(copper, 5), Optional.empty(), healing, MAX_USES, 1, 0.05F));
                offers.add(buy(copper, 2, ModItems.SNAKE_OIL.get(), 1));
                offers.add(buy(gold, 1, Items.GOLDEN_CARROT, 2));
                offers.add(buy(Items.SPIDER_EYE, 2, copper, 1));
                offers.add(buy(Items.HONEY_BOTTLE, 2, copper, 1));
            }
            // The teacher: books, slates (paper) and a compass for the geography lesson; apples gladly taken.
            case TEACHER -> {
                offers.add(buy(copper, 3, Items.BOOK, 1));
                offers.add(buy(copper, 4, Items.WRITABLE_BOOK, 1));
                offers.add(buy(copper, 1, Items.PAPER, 6));
                offers.add(buy(gold, 1, Items.COMPASS, 1));
                offers.add(buy(Items.APPLE, 6, copper, 1));
            }
            // The preacher: candles for the altar, the Good Book, holy water; he takes candles' wax (honeycomb) for alms.
            case PREACHER -> {
                offers.add(buy(copper, 1, Items.CANDLE, 3));
                offers.add(buy(copper, 3, Items.BOOK, 1));
                ItemStack water = new ItemStack(Items.POTION);
                water.set(net.minecraft.core.component.DataComponents.POTION_CONTENTS,
                        new net.minecraft.world.item.alchemy.PotionContents(net.minecraft.world.item.alchemy.Potions.WATER));
                offers.add(new MerchantOffer(new ItemCost(copper, 1), Optional.empty(), water, MAX_USES, 1, 0.05F));
                offers.add(buy(Items.HONEYCOMB, 3, copper, 1));
            }
            // The postmaster: the paper, stationery, maps of the territory; he buys feathers for quills.
            case POSTMASTER -> {
                offers.add(buy(copper, 1, ModItems.FRONTIER_GAZETTE.get(), 1));
                offers.add(buy(copper, 1, Items.PAPER, 8));
                offers.add(buy(copper, 2, Items.MAP, 1));
                offers.add(buy(Items.FEATHER, 6, copper, 1));
            }
            // The gunsmith: the town's only guns, the gun books and better rates on ammunition than the store; he buys powder
            // and brass.
            case GUNSMITH -> {
                offers.add(buy(gold, 2, ModItems.REVOLVER.get(), 1));
                offers.add(buy(gold, 3, ModItems.PLAINS_RIFLE.get(), 1));
                offers.add(buy(gold, 3, ModItems.SHOTGUN.get(), 1));
                offers.add(buy(copper, 2, ModItems.PERCUSSION_CAP.get(), 20));
                offers.add(buy(copper, 2, ModItems.REVOLVER_CARTRIDGE.get(), 8));
                offers.add(buy(copper, 2, ModItems.RIFLE_CARTRIDGE.get(), 5));
                offers.add(buy(copper, 3, ModItems.BUCKSHOT_CARTRIDGE.get(), 4));
                gunBook(level).ifPresent(offers::add);
                offers.add(buy(Items.GUNPOWDER, 4, copper, 1));
                offers.add(buy(ModItems.BRASS_INGOT.get(), 2, copper, 1));
            }
            // The editor: the Gazette, hot off the press; he buys paper and ink.
            case EDITOR -> {
                offers.add(buy(copper, 1, ModItems.FRONTIER_GAZETTE.get(), 1));
                offers.add(buy(Items.PAPER, 8, copper, 1));
                offers.add(buy(Items.INK_SAC, 2, copper, 1));
            }
        }
    }

    // The blacksmith does gunsmithing too: one book of a gun enchantment (22), at I or II, picked when his trades are first
    // set. Fewer uses than the rest: it's his one good book.
    private Optional<MerchantOffer> gunBook(ServerLevel level) {
        return level.registryAccess().lookupOrThrow(Registries.ENCHANTMENT).get(ModTags.Enchantments.GUN)
                .flatMap(enchantments -> enchantments.getRandomElement(getRandom()))
                .map(enchantment -> {
                    int bookLevel = Math.min(enchantment.value().getMaxLevel(), 1 + getRandom().nextInt(2));
                    ItemStack book = EnchantmentHelper.createBook(new EnchantmentInstance(enchantment, bookLevel));
                    return new MerchantOffer(new ItemCost(ModItems.GOLD_COIN.get(), 1 + 2 * bookLevel), Optional.empty(), book, GUN_BOOK_USES, 1, 0.05F);
                });
    }

    protected static MerchantOffer buy(ItemLike cost, int costCount, ItemLike result, int resultCount) {
        return new MerchantOffer(new ItemCost(cost, costCount), Optional.empty(), new ItemStack(result, resultCount), MAX_USES, 1, 0.05F);
    }

    @Override
    protected void rewardTradeXp(MerchantOffer offer) {
    }

    // Frontier folk aren't villagers: no "hrmm"s. A clink of coin when a trade goes through, silence otherwise.
    @Override
    public SoundEvent getNotifyTradeSound() {
        return ModSounds.COIN_CLINK.get();
    }

    @Override
    protected @Nullable SoundEvent getTradeUpdatedSound(boolean validTrade) {
        return null;
    }

    @Override
    public void playCelebrateSound() {
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
