package com.hollowfrontier.gametest;

import java.util.List;
import java.util.Map;

import com.hollowfrontier.HollowFrontier;
import com.hollowfrontier.content.hollow.sea.Basilosaurus;
import com.hollowfrontier.content.hollow.sea.GreatSeaCow;
import com.hollowfrontier.content.hollow.sea.HarpoonEntity;
import com.hollowfrontier.content.hollow.sea.Lampjelly;
import com.hollowfrontier.content.hollow.sea.Plesiosaur;
import com.hollowfrontier.content.hollow.sea.SeaLife;
import com.hollowfrontier.registry.ModBlocks;
import com.hollowfrontier.registry.ModEntities;
import com.hollowfrontier.registry.ModItems;
import com.hollowfrontier.worldgen.HollowBiomes;

import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.KelpBlock;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

/**
 * Phase 43b: the Still Sea's creatures and the Harpoon. Each test floods the 40-block field into a walled pool
 * ({@link #pool}: sand at y 0, water above it, with a shallow shelf along the west side). Behaviour that hangs on
 * pathfinding through water is driven by hand where it can be (the goals' own {@code tick}, as phase 34's tests do), so the
 * tests check the rules and not the navigator; the Plesiosaur's dive and the sea cows' breeding run for real.
 */
final class SeaLifeTests {
    private SeaLifeTests() {}

    /** The pool's depth in most tests: deep enough for a Basilosaurus to call it deep water. */
    private static final int DEEP = 10;
    /** The shelf: columns west of this are only {@link #SHALLOW} deep. */
    private static final int SHELF_X = 9, SHALLOW = 3;

    /** The sea's kinds come by depth and stop at their crowd; only the Still Sea is stocked. */
    static void seaLifeStocksByDepth(GameTestHelper h) {
        pool(h, DEEP);
        var biomes = h.getLevel().registryAccess().lookupOrThrow(Registries.BIOME);
        h.assertTrue(biomes.getOrThrow(HollowBiomes.STILL_SEA).is(SeaLife.HAS_SEA_LIFE), "the Still Sea has no sea life");
        for (ResourceKey<Biome> land : HollowBiomes.LANDS) {
            h.assertFalse(biomes.getOrThrow(land).is(SeaLife.HAS_SEA_LIFE), land.identifier() + " has sea life");
        }
        h.assertTrue(SeaLife.Kind.SEA_COW.suits(6) && !SeaLife.Kind.SEA_COW.suits(20), "sea cows belong in the shallows");
        h.assertTrue(SeaLife.Kind.BASILOSAURUS.suits(24) && !SeaLife.Kind.BASILOSAURUS.suits(DEEP), "a Basilosaurus needs deep water");
        h.assertTrue(SeaLife.Kind.PLESIOSAUR.suits(DEEP) && !SeaLife.Kind.PLESIOSAUR.suits(6), "a Plesiosaur needs the middle water");

        RandomSource random = RandomSource.create(43L);
        AABB all = new AABB(h.absolutePos(new BlockPos(0, 0, 0))).inflate(64);
        BlockPos deep = h.absolutePos(new BlockPos(24, DEEP, 20)), shelf = h.absolutePos(new BlockPos(4, DEEP, 20));
        h.assertTrue(SeaLife.depthBelow(h.getLevel(), deep, 64) == DEEP, "the pool is " + SeaLife.depthBelow(h.getLevel(), deep, 64) + " deep");
        h.assertTrue(SeaLife.columnDepth(h.getLevel(), deep.below(4)) == DEEP, "the column should measure the same from the middle");
        h.assertTrue(SeaLife.bedBelow(h.getLevel(), deep).getY() == h.absolutePos(new BlockPos(0, 1, 0)).getY(), "the bed is the water on the sand");
        h.assertTrue(SeaLife.place(h.getLevel(), deep, SeaLife.Kind.BASILOSAURUS, all, random).isEmpty(), "a Basilosaurus in " + DEEP + " blocks of water");
        h.assertTrue(SeaLife.place(h.getLevel(), shelf, SeaLife.Kind.LAMPJELLY, all, random).isEmpty(), "jellies in " + SHALLOW + " blocks of water");
        h.assertTrue(SeaLife.place(h.getLevel(), deep.above(3), SeaLife.Kind.SEA_COW, all, random).isEmpty(), "cows in mid-air");
        h.assertTrue(SeaLife.stock(h.getLevel(), deep, SeaLife.Kind.SEA_COW, all, random).isEmpty(), "this isn't the Still Sea: nothing should be stocked");
        List<Mob> herd = SeaLife.place(h.getLevel(), deep, SeaLife.Kind.SEA_COW, all, random);
        h.assertTrue(herd.size() >= 1 && herd.size() <= SeaLife.Kind.SEA_COW.maxGroup, "a herd of " + herd.size());
        h.assertTrue(herd.stream().allMatch(cow -> cow instanceof GreatSeaCow && SeaLife.isWater(h.getLevel(), cow.blockPosition())), "the herd isn't in the water");
        int cows = herd.size();
        for (int i = 0; i < 12; i++) {
            boolean crowded = cows >= SeaLife.Kind.SEA_COW.crowd;
            List<Mob> more = SeaLife.place(h.getLevel(), deep, SeaLife.Kind.SEA_COW, all, random);
            h.assertTrue(!crowded || more.isEmpty(), "more cows came with " + cows + " already there");
            cows += more.size();
        }
        h.assertTrue(cows >= SeaLife.Kind.SEA_COW.crowd, "the herd never filled out: " + cows);
        // Each kind keeps to its own depth (the playtest found everything idling at the surface): the herd came in near the
        // bed, and the band a creature swims in holds for every roll, in deep water and on the shelf
        int bedY = h.absolutePos(new BlockPos(0, 1, 0)).getY(), surfaceY = deep.getY();
        h.assertTrue(herd.stream().allMatch(cow -> cow.getBlockY() <= bedY + SeaLife.Kind.SEA_COW.maxAboveBed), "the herd came in up at the surface");
        for (SeaLife.Kind kind : SeaLife.Kind.values()) {
            for (int i = 0; i < 40; i++) {
                net.minecraft.world.phys.Vec3 at = com.hollowfrontier.content.hollow.sea.SwimAtDepthGoal.atDepth(h.getLevel(), deep.below(i % DEEP),
                        kind.minBelowSurface, kind.maxAboveBed, 0.0F, random);
                h.assertTrue(at != null && at.y >= bedY && at.y <= surfaceY - Math.min(kind.minBelowSurface, DEEP - 1), kind + " idles at " + at + " in water from " + bedY + " to " + surfaceY);
                h.assertTrue(kind.maxAboveBed < 0 || at.y <= bedY + kind.maxAboveBed, kind + " strays " + (at.y - bedY) + " over the bed");
            }
        }
        net.minecraft.world.phys.Vec3 up = com.hollowfrontier.content.hollow.sea.SwimAtDepthGoal.atDepth(h.getLevel(), deep.below(5), 4, -1, 1.0F, random);
        h.assertTrue(up != null && up.y == surfaceY, "a look at the surface should be at the surface: " + up);
        net.minecraft.world.phys.Vec3 shallow = com.hollowfrontier.content.hollow.sea.SwimAtDepthGoal.atDepth(h.getLevel(), shelf, 8, 10, 0.0F, random);
        h.assertTrue(shallow != null && SeaLife.isWater(h.getLevel(), BlockPos.containing(shallow)), "in shallow water it should still be in the water: " + shallow);
        h.assertTrue(com.hollowfrontier.content.hollow.sea.SwimAtDepthGoal.atDepth(h.getLevel(), deep.above(20), 2, 5, 0.0F, random) == null, "a depth found with no water near");
        h.succeed();
    }

    /** Recipes and drops: tallow (a fuel) from blubber, candles from it, the Jelly Lamp, both harpoons; each beast's own drop. */
    static void seaDropsAndRecipes(GameTestHelper h) {
        pool(h, DEEP);
        for (String id : List.of("tallow_from_smelting", "tallow_from_campfire_cooking", "candle_from_tallow", "jelly_lamp", "harpoon", "toothed_harpoon")) {
            ResourceKey<Recipe<?>> key = ResourceKey.create(Registries.RECIPE, HollowFrontier.id(id));
            h.assertTrue(h.getLevel().recipeAccess().byKey(key).isPresent(), "missing recipe " + id);
        }
        h.assertTrue(new ItemStack(ModItems.TALLOW.get()).has(DataComponents.COOKING_FUEL), "tallow should burn");
        h.assertFalse(new ItemStack(ModItems.BLUBBER.get()).has(DataComponents.COOKING_FUEL), "blubber shouldn't burn until it's rendered");
        h.assertTrue(ModBlocks.JELLY_LAMP.get().defaultBlockState().getLightEmission() == 15, "the Jelly Lamp should be as bright as a sea lantern");

        ServerPlayer player = TestPlayers.survival(h, new BlockPos(20, DEEP + 2, 2));
        Map<LivingEntity, Item> beasts = Map.of(
                h.spawnWithNoFreeWill(ModEntities.GREAT_SEA_COW.get(), new BlockPos(14, 5, 12)), ModItems.BLUBBER.get(),
                h.spawnWithNoFreeWill(ModEntities.BASILOSAURUS.get(), new BlockPos(28, 5, 12)), ModItems.BASILOSAUR_TOOTH.get(),
                h.spawnWithNoFreeWill(ModEntities.PLESIOSAUR.get(), new BlockPos(14, 5, 28)), Items.BONE);
        Map<Item, Vec3> where = new java.util.HashMap<>();
        beasts.forEach((beast, item) -> {
            where.put(item, beast.position());
            beast.hurtServer(h.getLevel(), h.getLevel().damageSources().playerAttack(player), 10_000.0F);
        });
        Plesiosaur diver = h.spawnWithNoFreeWill(ModEntities.PLESIOSAUR.get(), new BlockPos(28, 5, 28));
        int found = 0;
        for (int i = 0; i < 20; i++) {
            found += diver.bringUp(h.getLevel()).size();
        }
        h.assertTrue(found >= 20, "twenty dives brought up only " + found + " things");
        h.succeedWhen(() -> where.forEach((item, pos) -> h.assertTrue(!h.getLevel().getEntitiesOfClass(ItemEntity.class, new AABB(pos, pos).inflate(12),
                drop -> drop.getItem().is(item)).isEmpty(), "no " + item + " dropped")));
    }

    /**
     * A hungry sea cow finds the kelp, eats the top off the stalk and leaves the stalk growing; then the goal is run by hand,
     * past its own end and after it's stopped (phase 34's case).
     */
    static void seaCowGrazes(GameTestHelper h) {
        pool(h, DEEP);
        BlockPos stalk = new BlockPos(20, 1, 20), second = new BlockPos(26, 1, 26);
        for (BlockPos base : List.of(stalk, second)) {
            for (int y = 0; y < 4; y++) {
                h.setBlock(base.above(y), Blocks.KELP_PLANT);
            }
            h.setBlock(base.above(4), Blocks.KELP.defaultBlockState().setValue(KelpBlock.AGE, 25));
        }
        GreatSeaCow cow = h.spawnWithNoFreeWill(ModEntities.GREAT_SEA_COW.get(), new BlockPos(20, 5, 23));
        cow.setHungry(true);
        BlockPos top = cow.findKelp().orElse(null);
        h.assertTrue(top != null && top.equals(h.absolutePos(stalk.above(4))), "it should find the nearer stalk's top: " + top);
        h.assertTrue(cow.graze(top), "it didn't eat");
        h.assertBlockPresent(Blocks.WATER, stalk.above(4));
        h.assertBlockPresent(Blocks.KELP, stalk.above(3));
        h.assertBlockPresent(Blocks.KELP_PLANT, stalk.above(2));
        h.assertFalse(cow.hungry(), "it's still hungry");
        h.assertFalse(cow.graze(top), "it ate water");
        // A single kelp with no stalk under it is left alone: it would never grow back
        h.setBlock(new BlockPos(14, 1, 14), Blocks.KELP);
        h.assertFalse(cow.graze(h.absolutePos(new BlockPos(14, 1, 14))), "it ate a plant down to nothing");

        // The goal, by hand, on the second stalk
        cow.snapTo(Vec3.atCenterOf(h.absolutePos(second.above(4).east())));
        cow.setHungry(true);
        GreatSeaCow.GrazeGoal goal = new GreatSeaCow.GrazeGoal(cow);
        boolean started = false;
        for (int i = 0; i < 400 && !started; i++) {
            cow.setHungry(true);
            started = goal.canUse();
        }
        h.assertTrue(started, "the goal never started");
        goal.start();
        for (int i = 0; i < 200 && goal.canContinueToUse(); i++) {
            goal.tick();
        }
        h.assertBlockPresent(Blocks.WATER, second.above(4));
        h.assertBlockPresent(Blocks.KELP, second.above(3));
        goal.tick(); // the tick after it's finished
        goal.stop();
        goal.tick(); // and one after it's stopped
        h.assertFalse(cow.isGrazing(), "it's still chewing");
        h.succeed();
    }

    /** Kelp is their food: two fed sea cows have a calf. */
    static void seaCowsBreed(GameTestHelper h) {
        pool(h, 6);
        ServerPlayer player = TestPlayers.survival(h, new BlockPos(20, 8, 2));
        GreatSeaCow a = h.spawn(ModEntities.GREAT_SEA_COW.get(), new BlockPos(19, 3, 20));
        GreatSeaCow b = h.spawn(ModEntities.GREAT_SEA_COW.get(), new BlockPos(22, 3, 20));
        h.assertTrue(a.isFood(new ItemStack(Items.KELP)) && !a.isFood(new ItemStack(Items.WHEAT)), "kelp is what they eat");
        h.runAfterDelay(5, () -> {
            a.setInLove(player);
            b.setInLove(player);
        });
        h.succeedWhen(() -> h.assertTrue(!h.getLevel().getEntitiesOfClass(GreatSeaCow.class, a.getBoundingBox().inflate(24), GreatSeaCow::isBaby).isEmpty(), "no calf yet"));
    }

    /**
     * The hunt, by hand: a swimmer over deep water is prey, one on the shelf isn't; it circles, then strikes, and a strike
     * that reaches bites and goes back to circling; prey that gets to the shallows is given up after a while. Then the tick
     * after the target's gone, and one after it's stopped.
     */
    static void basilosaurusHunts(GameTestHelper h) {
        pool(h, DEEP);
        ServerPlayer player = TestPlayers.survival(h, new BlockPos(24, 6, 20));
        ServerPlayer wader = TestPlayers.survival(h, new BlockPos(4, DEEP, 20), "hf-test-wader");
        ServerPlayer builder = TestPlayers.creative(h, new BlockPos(24, 6, 24));
        h.assertTrue(Basilosaurus.isPrey(player), "a swimmer over deep water should be prey");
        h.assertFalse(Basilosaurus.isPrey(wader), "it should leave the shallows alone");
        h.assertFalse(Basilosaurus.isPrey(builder), "it shouldn't hunt a creative player");

        // The hunt itself is run on a sea cow: a fake player never ticks, so it never leaves its spawn invulnerability
        GreatSeaCow prey = h.spawnWithNoFreeWill(ModEntities.GREAT_SEA_COW.get(), new BlockPos(24, 6, 16));
        h.assertTrue(Basilosaurus.isPrey(prey), "anything afloat over deep water is prey");
        Basilosaurus beast = h.spawnWithNoFreeWill(ModEntities.BASILOSAURUS.get(), new BlockPos(32, 6, 30));
        Basilosaurus.HuntGoal hunt = new Basilosaurus.HuntGoal(beast);
        h.assertFalse(hunt.canUse(), "it hunts nothing yet");
        beast.setTarget(prey);
        h.assertTrue(hunt.canUse(), "it should hunt its target");
        hunt.start();
        h.assertTrue(hunt.phase() == Basilosaurus.Phase.CIRCLE && !beast.isStriking(), "it should circle first");
        float health = prey.getHealth();
        int circled = 0;
        while (hunt.phase() == Basilosaurus.Phase.CIRCLE && circled < Basilosaurus.CIRCLE_MAX + 5) {
            hunt.tick();
            circled++;
        }
        h.assertTrue(hunt.phase() == Basilosaurus.Phase.STRIKE && beast.isStriking(), "it never came in to strike");
        h.assertTrue(circled >= Basilosaurus.CIRCLE_MIN, "it struck after only " + circled + " ticks of circling");
        h.assertTrue(prey.getHealth() == health, "it bit from across the pool");
        beast.snapTo(prey.position().add(1.5, 0.0, 0.0));
        hunt.tick();
        h.assertTrue(prey.getHealth() < health, "the strike reached and didn't bite: " + prey.getHealth());
        h.assertTrue(hunt.phase() == Basilosaurus.Phase.CIRCLE && !beast.isStriking(), "after a bite it should circle again");

        // To the shallows: it waits, then lets go
        prey.snapTo(wader.position());
        for (int i = 0; i < Basilosaurus.GIVE_UP_TICKS - 1; i++) {
            hunt.tick();
        }
        h.assertTrue(beast.getTarget() == prey && hunt.lostTicks() == Basilosaurus.GIVE_UP_TICKS - 1, "it gave up too soon");
        hunt.tick();
        h.assertTrue(beast.getTarget() == null, "it never gave up on prey in the shallows");
        hunt.tick(); // the tick after the target's gone
        h.assertFalse(hunt.canContinueToUse(), "the hunt should be over");
        hunt.stop();
        hunt.tick(); // and one after it's stopped
        h.succeed();
    }

    /** A fish sends a Plesiosaur to the bed and back; what it found comes up with it, and it won't go again for a while. */
    static void plesiosaurDivesAndFetches(GameTestHelper h) {
        pool(h, DEEP);
        ServerPlayer player = TestPlayers.survival(h, new BlockPos(22, DEEP, 20));
        Plesiosaur plesiosaur = h.spawn(ModEntities.PLESIOSAUR.get(), new BlockPos(20, DEEP - 1, 20));
        h.runAfterDelay(5, () -> {
            player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(Items.STICK));
            h.assertTrue(plesiosaur.mobInteract(player, InteractionHand.MAIN_HAND) != InteractionResult.SUCCESS, "a stick sent it diving");
            h.assertTrue(plesiosaur.dive() == Plesiosaur.Dive.NONE, "it's diving for a stick");
            player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(Items.COD, 2));
            h.assertTrue(plesiosaur.willDive(), "it should be willing");
            h.assertTrue(plesiosaur.mobInteract(player, InteractionHand.MAIN_HAND) == InteractionResult.SUCCESS, "it wouldn't take the fish");
            h.assertTrue(plesiosaur.dive() == Plesiosaur.Dive.DOWN && plesiosaur.isDiving(), "it didn't dive: " + plesiosaur.dive());
            h.assertTrue(player.getMainHandItem().getCount() == 1, "the fish wasn't eaten");
            h.assertTrue(plesiosaur.mobInteract(player, InteractionHand.MAIN_HAND) == InteractionResult.FAIL, "it took a second fish mid-dive");
        });
        h.succeedWhen(() -> {
            h.assertTrue(plesiosaur.tickCount > 10 && plesiosaur.dive() == Plesiosaur.Dive.NONE && !plesiosaur.isDiving(), "still diving: " + plesiosaur.dive());
            h.assertFalse(plesiosaur.willDive(), "it should want a rest before it dives again");
            h.assertTrue(!h.getLevel().getEntitiesOfClass(ItemEntity.class, plesiosaur.getBoundingBox().inflate(16)).isEmpty(), "it came up with nothing");
            h.assertTrue(AdvancementTests.earned(player, "hollow/plesiosaur_fetch"), "Good Neck not earned");
            h.assertTrue(plesiosaur.mobInteract(player, InteractionHand.MAIN_HAND) == InteractionResult.FAIL, "it went again without a rest");
        });
    }

    /** A Lampjelly's sting: a prick and a little poison, and not twice in a breath. */
    static void lampjellyStings(GameTestHelper h) {
        pool(h, DEEP);
        GreatSeaCow prey = h.spawnWithNoFreeWill(ModEntities.GREAT_SEA_COW.get(), new BlockPos(20, 5, 20)); // not a fake player: see the hunt
        Lampjelly jelly = h.spawnWithNoFreeWill(ModEntities.LAMPJELLY.get(), new BlockPos(21, 5, 20));
        float health = prey.getHealth();
        h.assertTrue(jelly.sting(h.getLevel(), prey), "it didn't sting");
        h.assertTrue(prey.getHealth() < health && prey.getHealth() >= health - 2.0F, "a sting should be a prick: " + health + " -> " + prey.getHealth());
        h.assertTrue(prey.hasEffect(MobEffects.POISON), "no poison");
        float after = prey.getHealth();
        h.assertFalse(jelly.sting(h.getLevel(), prey), "it stung twice in a breath");
        h.assertTrue(prey.getHealth() == after, "the second sting hurt");
        h.succeed();
    }

    /**
    /**
     * The Harpoon is a line weapon. Thrown underwater it keeps its speed (an arrow would have lost nearly all of it). What
     * it hits takes a jab and is held: a pig is reeled in to the thrower and let go at arm's length; an item afloat is
     * caught in passing and reeled in too; a Plesiosaur is too big to come, and tows the thrower instead until they sneak.
     * A line stretched too far snaps. Let go, the harpoon comes home and goes back into the pack. The item throws one and
     * wears by one.
     */
    static void harpoonHoldsReelsAndTows(GameTestHelper h) {
        pool(h, DEEP);
        ServerPlayer player = TestPlayers.survival(h, new BlockPos(12, 5, 20));
        ServerPlayer other = TestPlayers.survival(h, new BlockPos(12, 5, 34), "hf-test-snapper");
        net.minecraft.world.entity.animal.pig.Pig pig = h.spawn(net.minecraft.world.entity.EntityTypes.PIG, new BlockPos(20, 5, 14));
        Plesiosaur big = h.spawnWithNoFreeWill(ModEntities.PLESIOSAUR.get(), new BlockPos(24, 5, 26));
        Plesiosaur far = h.spawnWithNoFreeWill(ModEntities.PLESIOSAUR.get(), new BlockPos(24, 5, 34));
        ItemEntity flotsam = new ItemEntity(h.getLevel(), 0, 0, 0, new ItemStack(Items.NAUTILUS_SHELL));
        flotsam.setPos(h.absoluteVec(new Vec3(22.5, 5.5, 20.5)));
        flotsam.setNoGravity(true);
        flotsam.setDeltaMovement(Vec3.ZERO);
        h.getLevel().addFreshEntity(flotsam);
        h.assertTrue(HarpoonEntity.tows(big) && !HarpoonEntity.tows(pig) && !HarpoonEntity.tows(player), "a Plesiosaur tows; a pig and a player are reeled in");
        h.assertTrue(HarpoonEntity.jab(new ItemStack(ModItems.TOOTHED_HARPOON.get())) > HarpoonEntity.jab(new ItemStack(ModItems.HARPOON.get())),
                "teeth should make a harpoon bite harder");
        float pigHealth = pig.getHealth(), bigHealth = big.getHealth();

        HarpoonEntity atPig = thrown(h, player, ModItems.HARPOON.get(), new BlockPos(13, 5, 14), new Vec3(1, 0, 0));
        HarpoonEntity atItem = thrown(h, player, ModItems.HARPOON.get(), new BlockPos(13, 5, 20), new Vec3(1, 0, 0));
        HarpoonEntity atBig = thrown(h, player, ModItems.TOOTHED_HARPOON.get(), new BlockPos(13, 5, 26), new Vec3(1, 0, 0));
        HarpoonEntity atFar = thrown(h, other, ModItems.HARPOON.get(), new BlockPos(13, 5, 34), new Vec3(1, 0, 0));
        double[] speed = new double[1];
        boolean[] saw = new boolean[6]; // pig hooked, pig reeled in, item hooked, item reeled in, towed, far hooked
        h.runAfterDelay(4, () -> speed[0] = atFar.getDeltaMovement().length());
        h.onEachTick(() -> {
            saw[0] |= atPig.hooked() == pig;
            saw[1] |= saw[0] && !atPig.isHooked() && pig.distanceTo(player) < HarpoonEntity.ARMS_LENGTH + 2.5;
            saw[2] |= atItem.hooked() == flotsam;
            saw[3] |= saw[2] && !atItem.isHooked() && flotsam.distanceTo(player) < HarpoonEntity.ARMS_LENGTH + 2.5;
            saw[4] |= atBig.hooked() == big && player.getDeltaMovement().x > 0.05;
            if (atFar.hooked() == far && !saw[5]) {
                saw[5] = true;
                far.snapTo(far.position().add(0, 0, HarpoonEntity.LINE_LENGTH + 8)); // off with the line
            }
        });

        // The item itself: one throw, one wear, the hand emptied
        ServerPlayer thrower = TestPlayers.survival(h, new BlockPos(30, DEEP + 2, 4), "hf-test-thrower");
        thrower.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(ModItems.HARPOON.get()));
        h.assertTrue(ModItems.HARPOON.get().use(h.getLevel(), thrower, InteractionHand.MAIN_HAND) == InteractionResult.SUCCESS, "the harpoon wasn't thrown");
        h.assertTrue(thrower.getMainHandItem().isEmpty(), "the harpoon is still in hand");
        List<HarpoonEntity> flying = h.getLevel().getEntitiesOfClass(HarpoonEntity.class, thrower.getBoundingBox().inflate(4));
        h.assertTrue(flying.stream().anyMatch(harpoon -> harpoon.getItem().getDamageValue() == 1), "the thrown harpoon should have worn by one");

        h.runAfterDelay(100, () -> player.setShiftKeyDown(true));
        h.succeedWhen(() -> {
            h.assertTrue(speed[0] > 1.5, "it slowed in the water like an arrow: " + speed[0] + " after 4 ticks");
            h.assertTrue(saw[0] && pig.getHealth() == pigHealth - HarpoonEntity.JAB, "the pig wasn't jabbed and held: " + pig.getHealth());
            h.assertTrue(saw[1], "the pig wasn't reeled in and let go: " + pig.distanceTo(player) + " away, held " + atPig.isHooked());
            h.assertTrue(saw[2] && saw[3], "the item afloat wasn't caught and reeled in: " + flotsam.distanceTo(player) + " away");
            h.assertTrue(saw[4] && big.getHealth() == bigHealth - HarpoonEntity.TOOTHED_JAB, "the Plesiosaur didn't tow the thrower");
            h.assertTrue(player.isShiftKeyDown() && !atBig.isHooked() && atBig.isReturning(), "sneaking should let go of the line");
            h.assertTrue(big.distanceTo(player) > 6, "a Plesiosaur was reeled in");
            h.assertTrue(saw[5] && !atFar.isHooked(), "a line stretched past its length should snap");
            h.assertTrue(atPig.isAlive() && atPig.distanceTo(player) < 4.0, "the harpoon isn't home: " + atPig.distanceTo(player));
            atPig.playerTouch(player);
            h.assertTrue(player.getInventory().contains(new ItemStack(ModItems.HARPOON.get())), "the harpoon didn't go back into the pack");
            h.assertFalse(atPig.isAlive(), "the harpoon is still out after being taken in");
        });
    }

    private static HarpoonEntity thrown(GameTestHelper h, ServerPlayer player, Item item, BlockPos from, Vec3 toward) {
        HarpoonEntity harpoon = new HarpoonEntity(h.getLevel(), player, new ItemStack(item));
        Vec3 at = Vec3.atCenterOf(h.absolutePos(from));
        harpoon.setPos(at.x, at.y, at.z);
        harpoon.shoot(toward.x, toward.y, toward.z, 2.2F, 0.0F);
        h.getLevel().addFreshEntity(harpoon);
        return harpoon;
    }

    /**
     * Floods the 40-block field: sandstone walls round it, a sand bed, {@code depth} blocks of water, and along the west side
     * (x up to {@link #SHELF_X}) a shelf of sand that leaves only {@link #SHALLOW} blocks of water over it.
     */
    static void pool(GameTestHelper h, int depth) {
        for (int x = 0; x < 40; x++) {
            for (int z = 0; z < 40; z++) {
                boolean wall = x == 0 || z == 0 || x == 39 || z == 39;
                int bed = x <= SHELF_X ? Math.max(0, depth - SHALLOW) : 0;
                h.setBlock(new BlockPos(x, 0, z), Blocks.SAND);
                for (int y = 1; y <= depth; y++) {
                    h.setBlock(new BlockPos(x, y, z), wall ? Blocks.SANDSTONE : y <= bed ? Blocks.SAND : Blocks.WATER);
                }
            }
        }
    }
}
