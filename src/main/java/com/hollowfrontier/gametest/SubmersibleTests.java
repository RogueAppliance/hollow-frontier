package com.hollowfrontier.gametest;

import com.hollowfrontier.content.hollow.sea.Basilosaurus;
import com.hollowfrontier.content.steam.SteamCanisterItem;
import com.hollowfrontier.content.steam.SteamStorageBlockEntity;
import com.hollowfrontier.content.steam.submersible.SteamSubmersible;
import com.hollowfrontier.content.steam.submersible.SubmersibleItem;
import com.hollowfrontier.registry.ModBlocks;
import com.hollowfrontier.registry.ModEntities;
import com.hollowfrontier.registry.ModFluids;
import com.hollowfrontier.registry.ModItems;

import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.animal.pig.Pig;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

/**
 * Phase 43c: the Steam Submersible, in the flooded field of {@link SeaLifeTests#pool}. It has no pilot in most of these:
 * {@link SteamSubmersible#setHelm} stands in for one, as the airship's tests do.
 */
final class SubmersibleTests {
    private SubmersibleTests() {}

    private static final int DEPTH = 12;

    /** Its recipe loads; it's launched on water and not on land; a canister pours in; a steam tank beside the hull fills it. */
    static void submersibleLaunchesAndCharges(GameTestHelper h) {
        SeaLifeTests.pool(h, DEPTH);
        ServerLevel level = h.getLevel();
        ResourceKey<Recipe<?>> recipe = ResourceKey.create(Registries.RECIPE, com.hollowfrontier.HollowFrontier.id("steam_submersible"));
        h.assertTrue(level.recipeAccess().byKey(recipe).isPresent(), "missing recipe steam_submersible");

        ServerPlayer player = TestPlayers.survival(h, new BlockPos(20, DEPTH + 1, 2), "hf-sub-owner");
        ItemStack folded = SubmersibleItem.of(2_500);
        h.assertFalse(SubmersibleItem.launch(level, player, folded, h.absolutePos(new BlockPos(20, DEPTH, 0))), "it was launched on the wall");
        h.assertTrue(folded.getCount() == 1, "a launch that failed used the item up");
        h.assertTrue(SubmersibleItem.launch(level, player, folded, h.absolutePos(new BlockPos(20, DEPTH - 4, 20))), "it wouldn't launch on water");
        var launched = level.getEntitiesOfClass(SteamSubmersible.class, new AABB(h.absolutePos(new BlockPos(20, DEPTH, 20))).inflate(3));
        h.assertValueEqual(launched.size(), 1, "submersibles launched");
        SteamSubmersible sub = launched.get(0);
        h.assertTrue(folded.isEmpty(), "the item wasn't used up");
        h.assertValueEqual(sub.steam(), 2_500, "steam it was folded with");
        h.assertTrue(player.getUUID().equals(sub.owner()), "whoever launches it should own it");
        h.assertTrue(sub.afloat() && sub.awash() && sub.depth() == 0, "it should be launched awash, however deep the water looked at");

        ItemStack canister = new ItemStack(ModItems.STEAM_CANISTER.get());
        SteamCanisterItem.setSteam(canister, 3_000);
        player.setItemInHand(InteractionHand.MAIN_HAND, canister);
        sub.interact(player, InteractionHand.MAIN_HAND, sub.position());
        h.assertTrue(sub.steam() == 5_500 && SteamCanisterItem.steam(canister) == 0, "the canister didn't pour in: " + sub.steam());

        h.assertValueEqual(sub.charge(level), 0, "steam taken with no holder near");
        BlockPos tankPos = new BlockPos(22, DEPTH, 20);
        h.setBlock(tankPos, ModBlocks.STEAM_TANK.get());
        h.getBlockEntity(tankPos, SteamStorageBlockEntity.class).tank().add(ModFluids.steam(), 16_000);
        h.assertValueEqual(sub.charge(level), SteamSubmersible.CHARGE_AMOUNT, "steam taken from a tank beside the hull");
        h.succeedWhen(() -> h.assertTrue(sub.steam() >= 8_000, "left beside a tank it should fill by itself: " + sub.steam()));
    }

    /**
     * Under way: it dives on its ballast and holds its depth with the helm let go (which costs nothing); it drives ahead
     * under water; with the reservoir dry it comes up by itself and stops awash. Out of the water it won't drive at all.
     */
    static void submersibleDivesHoldsAndSurfaces(GameTestHelper h) {
        SeaLifeTests.pool(h, DEPTH);
        SteamSubmersible sub = sub(h, new Vec3(20.5, DEPTH - 0.25, 12.5), 4_000);
        SteamSubmersible beached = sub(h, new Vec3(4.5, DEPTH + 1.0, 30.5), 4_000);
        // Over the shelf the water is three deep; this one is set down on the sand of a dry island built on it
        for (int x = 2; x <= 7; x++) {
            for (int z = 28; z <= 33; z++) {
                for (int y = DEPTH - 2; y <= DEPTH; y++) {
                    h.setBlock(new BlockPos(x, y, z), net.minecraft.world.level.block.Blocks.SANDSTONE);
                }
            }
        }
        beached.setHelm(1, 0);
        double top = sub.getY();
        double[] mark = new double[4];
        int[] steam = new int[2];
        sub.setHelm(0, -1);
        h.runAfterDelay(50, () -> {
            mark[0] = sub.getY();
            h.assertTrue(mark[0] < top - 3.0 && sub.depth() >= 2, "it didn't dive: " + top + " -> " + mark[0]);
            h.assertTrue(sub.steam() < 4_000, "diving used no steam");
            sub.setHelm(0, 0);
        });
        h.runAfterDelay(70, () -> {
            mark[0] = sub.getY(); // once the way is off it
            steam[0] = sub.steam();
        });
        h.runAfterDelay(100, () -> {
            mark[1] = sub.getY();
            h.assertTrue(Math.abs(mark[1] - mark[0]) < 0.6, "it didn't hold its depth: " + mark[0] + " -> " + mark[1]);
            h.assertValueEqual(sub.steam(), steam[0], "steam while holding still");
            mark[2] = sub.getZ();
            sub.setYRot(0.0F);
            sub.setHelm(1, 0);
        });
        h.runAfterDelay(150, () -> {
            h.assertTrue(sub.getZ() > mark[2] + 4.0 && sub.isDriving(), "it didn't drive ahead: " + mark[2] + " -> " + sub.getZ());
            h.assertTrue(Math.abs(sub.getY() - mark[1]) < 0.6, "driving changed its depth");
            sub.setHelm(1, -1);
            sub.setSteam(0);
        });
        h.runAfterDelay(160, () -> h.assertFalse(sub.isDriving(), "it drives on a dry reservoir"));
        h.succeedWhen(() -> {
            h.assertTrue(sub.tickCount > 170 && sub.awash(), "dry, it should come up until it's awash: depth " + sub.depth());
            h.assertTrue(sub.getY() <= top + 0.5, "it came up out of the water: " + sub.getY() + " (launched at " + top + ")");
            h.assertFalse(beached.afloat(), "the beached one is afloat");
            h.assertTrue(beached.position().horizontal().distanceTo(Vec3.atLowerCornerOf(h.absolutePos(new BlockPos(4, 0, 30))).add(0.5, 0, 0.5).horizontal()) < 0.3,
                    "it drove on dry land");
            h.assertValueEqual(beached.steam(), 4_000, "steam used on dry land");
        });
    }

    /** Whoever rides it doesn't drown and sees by its lamp; the lamp goes out when they get out. */
    static void submersibleKeepsItsCrew(GameTestHelper h) {
        SeaLifeTests.pool(h, DEPTH);
        SteamSubmersible sub = sub(h, new Vec3(20.5, 4.0, 20.5), 1_000);
        Pig rider = h.spawnWithNoFreeWill(EntityTypes.PIG, new BlockPos(20, 5, 20));
        Pig second = h.spawnWithNoFreeWill(EntityTypes.PIG, new BlockPos(21, 5, 20));
        Pig third = h.spawnWithNoFreeWill(EntityTypes.PIG, new BlockPos(22, 5, 20));
        h.assertTrue(rider.startRiding(sub) && second.startRiding(sub), "it should take two aboard");
        h.assertFalse(third.startRiding(sub), "it took a third");
        third.discard();
        rider.setAirSupply(5);
        h.runAfterDelay(30, () -> {
            h.assertTrue(sub.depth() >= 5, "the test wants it well under: depth " + sub.depth());
            // topped up each tick by the boat, then one breath taken by the rider's own tick
            h.assertTrue(rider.getAirSupply() >= rider.getMaxAirSupply() - 1, "the rider is running out of air: " + rider.getAirSupply());
            h.assertTrue(rider.hasEffect(MobEffects.NIGHT_VISION), "no lamp to see by");
            rider.stopRiding();
            h.assertFalse(rider.hasEffect(MobEffects.NIGHT_VISION), "the lamp came out of the boat with its rider");
            h.assertTrue(second.hasEffect(MobEffects.NIGHT_VISION), "the lamp went out for the one still aboard");
            h.succeed();
        });
    }

    /** A stranger's blows glance off; its owner breaks it up, and it folds with its steam; launched again it has it still. */
    static void submersibleOwnerLockAndFold(GameTestHelper h) {
        SeaLifeTests.pool(h, DEPTH);
        ServerLevel level = h.getLevel();
        ServerPlayer owner = TestPlayers.survival(h, new BlockPos(20, DEPTH + 1, 2), "hf-sub-owner");
        ServerPlayer stranger = TestPlayers.survival(h, new BlockPos(24, DEPTH + 1, 2), "hf-sub-stranger");
        SteamSubmersible sub = sub(h, new Vec3(20.5, DEPTH - 0.25, 20.5), 5_000);
        sub.setOwner(owner);
        h.assertFalse(sub.hurtServer(level, level.damageSources().playerAttack(stranger), 100.0F), "a stranger's blow landed");
        h.assertTrue(sub.isAlive() && sub.hull() == SteamSubmersible.HULL, "a stranger harmed someone else's submersible");
        sub.interact(stranger, InteractionHand.MAIN_HAND, sub.position());
        h.assertTrue(stranger.getVehicle() == sub && owner.getUUID().equals(sub.owner()), "a stranger should be able to ride it, and not take it over");
        h.assertFalse(sub.hurtServer(level, level.damageSources().playerAttack(stranger), 100.0F), "a rider broke it up from inside");
        stranger.stopRiding();
        h.assertTrue(sub.hurtServer(level, level.damageSources().playerAttack(owner), 100.0F), "the owner's blow glanced off");
        h.assertFalse(sub.isAlive(), "the owner couldn't break it up");
        var drops = level.getEntitiesOfClass(ItemEntity.class, sub.getBoundingBox().inflate(3), drop -> drop.getItem().is(ModItems.STEAM_SUBMERSIBLE.get()));
        h.assertValueEqual(drops.size(), 1, "submersible items dropped");
        ItemStack folded = drops.get(0).getItem().copy();
        drops.forEach(ItemEntity::discard);
        h.assertValueEqual(SubmersibleItem.steam(folded), 5_000, "steam on the folded submersible");
        h.assertTrue(SubmersibleItem.launch(level, stranger, folded, h.absolutePos(new BlockPos(28, DEPTH, 28))), "it wouldn't launch again");
        var again = level.getEntitiesOfClass(SteamSubmersible.class, new AABB(h.absolutePos(new BlockPos(28, DEPTH, 28))).inflate(3));
        h.assertTrue(again.size() == 1 && again.get(0).steam() == 5_000 && stranger.getUUID().equals(again.get(0).owner()),
                "launched again it should have its steam, and belong to whoever launched it");
        h.succeed();
    }

    /** A Basilosaurus hunts a submersible over deep water and rams it: the crew is unhurt, the hull wears, and five rams break it up. */
    static void basilosaurusRamsTheHull(GameTestHelper h) {
        SeaLifeTests.pool(h, DEPTH);
        ServerLevel level = h.getLevel();
        SteamSubmersible sub = sub(h, new Vec3(24.5, 5.0, 20.5), 3_000);
        Pig rider = h.spawnWithNoFreeWill(EntityTypes.PIG, new BlockPos(24, 6, 20));
        h.assertTrue(rider.startRiding(sub), "the rider couldn't get in");
        h.assertTrue(Basilosaurus.isPrey(rider), "a submersible over deep water should be prey");
        Basilosaurus beast = h.spawnWithNoFreeWill(ModEntities.BASILOSAURUS.get(), new BlockPos(28, 5, 20));
        float health = rider.getHealth();
        h.assertTrue(beast.bite(level, rider), "the ram didn't land");
        h.assertValueEqual(rider.getHealth(), health, "the rider's health behind the hull");
        h.assertValueEqual(sub.hull(), SteamSubmersible.HULL - Basilosaurus.RAM_DAMAGE, "the hull after one ram");
        h.assertTrue(sub.isAlive() && rider.getVehicle() == sub, "one ram broke it up");
        h.assertTrue(sub.getDeltaMovement().horizontalDistance() > 0.2, "the ram didn't shove it");
        for (int i = 0; i < 3; i++) {
            beast.bite(level, rider);
        }
        h.assertTrue(sub.isAlive() && sub.hull() > 0, "it broke up after four rams: hull " + sub.hull());
        beast.bite(level, rider);
        h.assertFalse(sub.isAlive(), "five rams should break it up");
        h.assertTrue(rider.isAlive() && rider.getVehicle() == null, "the rider should be in the water now");
        var drops = level.getEntitiesOfClass(ItemEntity.class, sub.getBoundingBox().inflate(3), drop -> drop.getItem().is(ModItems.STEAM_SUBMERSIBLE.get()));
        h.assertTrue(drops.size() == 1 && SubmersibleItem.steam(drops.get(0).getItem()) > 0, "broken up by a ram it should still fold with its steam");
        h.succeed();
    }

    private static SteamSubmersible sub(GameTestHelper h, Vec3 relative, int steam) {
        SteamSubmersible sub = ModEntities.STEAM_SUBMERSIBLE.get().create(h.getLevel(), net.minecraft.world.entity.EntitySpawnReason.COMMAND);
        Vec3 at = h.absoluteVec(relative);
        sub.snapTo(at.x, at.y, at.z, 0.0F, 0.0F);
        sub.setSteam(steam);
        h.getLevel().addFreshEntity(sub);
        return sub;
    }
}
