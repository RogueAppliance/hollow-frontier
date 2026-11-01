package com.hollowfrontier.gametest;

import com.hollowfrontier.HollowFrontier;
import com.hollowfrontier.content.hollow.gateway.GatewayEffects;
import com.hollowfrontier.content.hollow.gateway.GatewayFrameBlock;
import com.hollowfrontier.content.hollow.gateway.GatewayLinks;
import com.hollowfrontier.content.hollow.gateway.PolarCompassItem;
import com.hollowfrontier.content.hollow.gateway.PolarGateways;
import com.hollowfrontier.content.hollow.gateway.PolarResonatorItem;
import com.hollowfrontier.content.steam.SteamCanisterItem;
import com.hollowfrontier.content.steam.SteamStorageBlockEntity;
import com.hollowfrontier.content.steam.airship.Airship;
import com.hollowfrontier.content.steam.airship.AirshipItem;
import com.hollowfrontier.registry.ModAttachments;
import com.hollowfrontier.registry.ModBlocks;
import com.hollowfrontier.registry.ModDataComponents;
import com.hollowfrontier.registry.ModEntities;
import com.hollowfrontier.registry.ModFluids;
import com.hollowfrontier.registry.ModItems;
import com.hollowfrontier.worldgen.HollowEarth;
import com.hollowfrontier.worldgen.structure.ModChestLoot;
import com.hollowfrontier.worldgen.structure.PolarBlueprints;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.player.Input;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructurePlaceSettings;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplate;
import net.minecraft.world.level.portal.TeleportTransition;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;

// Phase 8: the airship, the Polar Compass and Resonator, and the Polar Gateway into the Hollow Earth.
final class ExpeditionTests {
    private ExpeditionTests() {}

    private static Airship airship(GameTestHelper h, Vec3 relative, int steam, int fuel) {
        Airship ship = ModEntities.AIRSHIP.get().create(h.getLevel(), EntitySpawnReason.COMMAND);
        Vec3 at = h.absoluteVec(relative);
        ship.snapTo(at.x, at.y, at.z, 0, 0);
        ship.setSteam(steam);
        ship.setFuel(fuel);
        h.getLevel().addFreshEntity(ship);
        return ship;
    }

    /** Coal in the firebox boils steam; climbing burns it and lifts the ship. */
    static void airshipBurnsCoalAndClimbs(GameTestHelper h) {
        Airship ship = airship(h, new Vec3(6, 1, 6), 0, Airship.COAL_TICKS);
        ship.setHelm(0, 1);
        double startY = ship.getY();
        // The test box tops out about two blocks above the ship, so look early (a full climb would take it 1.3 by now).
        h.runAfterDelay(15, () -> {
            h.assertTrue(ship.fuel() < Airship.COAL_TICKS, "the firebox burned nothing");
            h.assertTrue(ship.getY() > startY + 1.0, "the airship didn't climb: " + (ship.getY() - startY));
            h.assertTrue(ship.isDriving(), "climbing should turn the propeller");
            ship.discard();
            h.succeed();
        });
    }

    /** A cold airship drifts down slowly instead of falling, and lands without breaking. */
    static void dryAirshipSinksGently(GameTestHelper h) {
        Airship ship = airship(h, new Vec3(6, 6, 6), 0, 0);
        double startY = ship.getY();
        h.runAfterDelay(20, () -> {
            double fell = startY - ship.getY();
            h.assertTrue(fell > 0.3, "a dry airship should sink: " + fell);
            h.assertTrue(-ship.getDeltaMovement().y <= 0.0601 + 1.0E-3, "sinking too fast: " + ship.getDeltaMovement().y);
            h.assertTrue(ship.isAlive(), "the airship broke");
            ship.discard();
            h.succeed();
        });
    }

    /** Under steam with the throttle open it drives off the way it faces (south at yaw 0) and burns steam doing it. */
    static void airshipDrivesAhead(GameTestHelper h) {
        Airship ship = airship(h, new Vec3(6, 3, 1), 4000, 0);
        ship.setHelm(1, 0);
        Vec3 start = ship.position();
        h.runAfterDelay(30, () -> {
            Vec3 moved = ship.position().subtract(start);
            h.assertTrue(moved.z > 2.0 && Math.abs(moved.x) < 0.5, "the airship didn't drive ahead: " + moved);
            h.assertTrue(ship.steam() < 4000 && ship.steam() >= 4000 - 35 * (Airship.HOVER_STEAM + Airship.DRIVE_STEAM), "steam used: " + ship.steam());
            ship.discard();
            h.succeed();
        });
    }

    /** The pilot's keys: jump climbs, and coal and a canister fill it up by hand. */
    static void pilotFliesAirship(GameTestHelper h) {
        Airship ship = airship(h, new Vec3(6, 1, 6), 0, 0);
        ServerPlayer pilot = TestPlayers.survival(h, new BlockPos(6, 1, 4));
        pilot.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(Items.COAL, 2));
        ship.interact(pilot, InteractionHand.MAIN_HAND, ship.position());
        h.assertTrue(ship.fuel() == Airship.COAL_TICKS && pilot.getMainHandItem().getCount() == 1, "stoking took no coal: " + ship.fuel());
        ItemStack canister = new ItemStack(ModItems.STEAM_CANISTER.get());
        SteamCanisterItem.setSteam(canister, 3000);
        pilot.setItemInHand(InteractionHand.MAIN_HAND, canister);
        ship.interact(pilot, InteractionHand.MAIN_HAND, ship.position());
        h.assertTrue(ship.steam() == 3000 && SteamCanisterItem.steam(canister) == 0, "the canister didn't pour in: " + ship.steam());
        pilot.setItemInHand(InteractionHand.MAIN_HAND, ItemStack.EMPTY);
        ship.interact(pilot, InteractionHand.MAIN_HAND, ship.position());
        h.assertTrue(pilot.getVehicle() == ship, "an empty hand should board the airship");
        pilot.setLastClientInput(new Input(false, false, false, false, true, false, false));
        double startY = ship.getY();
        h.runAfterDelay(30, () -> {
            h.assertTrue(ship.getY() > startY + 1.5, "the pilot's jump key didn't climb: " + (ship.getY() - startY));
            pilot.stopRiding();
            ship.discard();
            h.succeed();
        });
    }

    /** Parked over a steam tank it takes on steam. */
    static void mooredAirshipFillsFromTank(GameTestHelper h) {
        BlockPos tankPos = new BlockPos(6, 1, 6);
        h.setBlock(tankPos, ModBlocks.STEAM_TANK.get());
        SteamStorageBlockEntity tank = h.getBlockEntity(tankPos, SteamStorageBlockEntity.class);
        tank.tank().add(ModFluids.steam(), 16_000);
        Airship ship = airship(h, new Vec3(6.5, 2, 6.5), 0, 0);
        h.succeedWhen(() -> {
            h.assertTrue(ship.steam() >= 1000, "a moored airship took on no steam: " + ship.steam());
            ship.discard();
        });
    }

    /**
     * 31a: a stranger's blows and shots don't harm someone else's airship, but its owner's teammate can break it, and
     * the weather and mobs still can. An airship with no owner yet is its first pilot's.
     */
    static void airshipOwnerLockHolds(GameTestHelper h) {
        ServerLevel level = h.getLevel();
        ServerPlayer owner = TestPlayers.survival(h, new BlockPos(2, 1, 6), "hf-airship-owner");
        ServerPlayer stranger = TestPlayers.survival(h, new BlockPos(10, 1, 6), "hf-airship-stranger");
        ServerPlayer friend = TestPlayers.survival(h, new BlockPos(6, 1, 10), "hf-airship-friend");
        Airship ship = airship(h, new Vec3(6.5, 1, 6.5), 0, 0);
        h.assertTrue(ship.owner() == null, "a placed-by-test airship has an owner");
        ship.interact(owner, InteractionHand.MAIN_HAND, ship.position());
        h.assertTrue(owner.getUUID().equals(ship.owner()), "the first pilot didn't claim it");
        owner.stopRiding();
        ship.interact(stranger, InteractionHand.MAIN_HAND, ship.position());
        h.assertTrue(stranger.getVehicle() == ship, "a stranger couldn't ride it");
        h.assertTrue(owner.getUUID().equals(ship.owner()), "a stranger aboard took it over");
        stranger.stopRiding();
        h.assertFalse(ship.hurtServer(level, level.damageSources().playerAttack(stranger), 100.0F), "a stranger's blow landed");
        h.assertTrue(ship.isAlive() && ship.getDamage() == 0.0F, "a stranger broke someone else's airship");
        var scoreboard = level.getScoreboard();
        var team = scoreboard.addPlayerTeam("hf_airship_crew");
        try {
            scoreboard.addPlayerToTeam(owner.getScoreboardName(), team);
            scoreboard.addPlayerToTeam(friend.getScoreboardName(), team);
            h.assertFalse(ship.mayHandle(stranger), "a stranger got in with a team about");
            h.assertTrue(ship.hurtServer(level, level.damageSources().playerAttack(friend), 100.0F), "the teammate's blow glanced off");
            h.assertFalse(ship.isAlive(), "the teammate couldn't break it");
        } finally {
            scoreboard.removePlayerTeam(team);
        }
        Airship other = airship(h, new Vec3(6.5, 1, 2.5), 0, 0);
        other.setOwner(owner);
        h.assertTrue(other.hurtServer(level, level.damageSources().lava(), 100.0F) && !other.isAlive(), "lava spared an owned airship");
        h.succeed();
    }

    /**
     * 32a: aboard, a click on the hull passes through to what the rider holds (so they can eat, shoot and read a compass),
     * coal still stokes it, and the owner's own swing from the helm doesn't break it.
     */
    static void riderUsesItemsAboard(GameTestHelper h) {
        ServerLevel level = h.getLevel();
        ServerPlayer owner = TestPlayers.survival(h, new BlockPos(2, 1, 6), "hf-airship-rider");
        Airship ship = airship(h, new Vec3(6.5, 1, 6.5), 0, 0);
        ship.setOwner(owner);
        ship.interact(owner, InteractionHand.MAIN_HAND, ship.position());
        h.assertTrue(owner.getVehicle() == ship, "the owner couldn't board");
        owner.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(Items.BREAD));
        h.assertTrue(ship.interact(owner, InteractionHand.MAIN_HAND, ship.position()) == InteractionResult.PASS,
                "a rider's click on the hull was swallowed");
        owner.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(Items.COAL));
        ship.interact(owner, InteractionHand.MAIN_HAND, ship.position());
        h.assertValueEqual(ship.fuel(), Airship.COAL_TICKS, "coal stoked from aboard");
        h.assertFalse(ship.hurtServer(level, level.damageSources().playerAttack(owner), 100.0F), "the rider's own swing landed");
        h.assertTrue(ship.isAlive() && ship.getDamage() == 0.0F, "the rider broke their own ship");
        owner.stopRiding();
        h.assertTrue(ship.hurtServer(level, level.damageSources().playerAttack(owner), 100.0F) && !ship.isAlive(),
                "the owner couldn't break it from the ground");
        h.succeed();
    }

    /**
     * 32b: aloft, a rider's first sneak only warns them; a second soon after gets them off, with slow falling to see them
     * down. (The ship hovers on its steam with nobody at the keys.)
     */
    static void riderBailsOutOnSecondSneak(GameTestHelper h) {
        Airship ship = airship(h, new Vec3(10.5, 12, 10.5), 8_000, 0);
        ServerPlayer rider = TestPlayers.survival(h, new BlockPos(10, 1, 8), "hf-airship-bailer");
        ship.interact(rider, InteractionHand.MAIN_HAND, ship.position());
        h.assertTrue(rider.getVehicle() == ship, "couldn't board");
        h.assertTrue(ship.heightAboveGround(h.getLevel()) > Airship.BAIL_HEIGHT, "the ship isn't aloft: " + ship.heightAboveGround(h.getLevel()));
        rider.setShiftKeyDown(true);
        h.runAfterDelay(2, () -> {
            h.assertTrue(rider.getVehicle() == ship, "one sneak aloft dropped the rider");
            h.assertFalse(rider.isShiftKeyDown(), "the first sneak wasn't caught");
            rider.setShiftKeyDown(true);
            h.runAfterDelay(2, () -> {
                h.assertTrue(rider.getVehicle() == null, "a second sneak didn't get them off");
                h.assertTrue(rider.hasEffect(MobEffects.SLOW_FALLING), "no slow falling for a rider leaving aloft");
                ship.discard();
                h.succeed();
            });
        });
    }

    /** 32b: near the ground one sneak gets you off, as on any vehicle, and there's no slow falling to wait out. */
    static void riderStepsOffNearTheGround(GameTestHelper h) {
        Airship ship = airship(h, new Vec3(6.5, 1, 6.5), 0, 0);
        ServerPlayer rider = TestPlayers.survival(h, new BlockPos(6, 1, 4), "hf-airship-stepper");
        ship.interact(rider, InteractionHand.MAIN_HAND, ship.position());
        h.assertTrue(rider.getVehicle() == ship, "couldn't board");
        rider.setShiftKeyDown(true);
        h.runAfterDelay(2, () -> {
            h.assertTrue(rider.getVehicle() == null, "one sneak on the ground didn't get them off");
            h.assertFalse(rider.hasEffect(MobEffects.SLOW_FALLING), "slow falling for stepping off on the ground");
            ship.discard();
            h.succeed();
        });
    }

    /** 32b: under steam with nobody aboard a ship holds its height for a while, then comes down to wait. */
    static void unattendedAirshipComesDown(GameTestHelper h) {
        Airship left = airship(h, new Vec3(4.5, 12, 6.5), 8_000, 0);
        Airship fresh = airship(h, new Vec3(14.5, 12, 6.5), 8_000, 0);
        left.leaveUnattended(Airship.UNATTENDED_TICKS);
        double leftY = left.getY(), freshY = fresh.getY();
        h.runAfterDelay(30, () -> {
            h.assertTrue(left.isAdrift() && leftY - left.getY() > 0.5, "the unattended ship didn't come down: " + (leftY - left.getY()));
            h.assertTrue(-left.getDeltaMovement().y <= 0.0601 + 1.0E-3, "coming down too fast: " + left.getDeltaMovement().y);
            h.assertTrue(!fresh.isAdrift() && Math.abs(fresh.getY() - freshY) < 0.1, "a just-left ship should hold its height: " + (fresh.getY() - freshY));
            left.discard();
            fresh.discard();
            h.succeed();
        });
    }

    /**
     * 32b: a carried Polar Compass takes its own bearing, a held one reads out how far and which way (and says when you're
     * there), and it searches again only after a long way. The test world stands in for the Hollow Earth, where the
     * compass points to return gateways, so no gateway has to generate.
     */
    static void polarCompassFindsItsOwnWay(GameTestHelper h) {
        PolarGateways.standInForHollowEarth(Level.OVERWORLD);
        try {
            ServerLevel level = h.getLevel();
            BlockPos near = h.absolutePos(new BlockPos(6, 1, 6)), far = near.east(PolarCompassItem.RESEARCH_DISTANCE + 100);
            GatewayLinks.get(level).add(new GatewayLinks.Link(near, near));
            GatewayLinks.get(level).add(new GatewayLinks.Link(far, far));
            ServerPlayer player = TestPlayers.survival(h, new BlockPos(1, 1, 1), "hf-compass-reader");
            ItemStack compass = new ItemStack(ModItems.POLAR_COMPASS.get());
            player.setItemInHand(InteractionHand.MAIN_HAND, compass);
            PolarCompassItem.update(level, player, compass);
            h.assertValueEqual(target(compass), near, "the first bearing");
            h.assertValueEqual(key(PolarCompassItem.reading(player, compass)), "message.hollowfrontier.polar_compass.reading", "the held reading");
            player.snapTo(Vec3.atBottomCenterOf(near));
            h.assertValueEqual(key(PolarCompassItem.reading(player, compass)), "message.hollowfrontier.polar_compass.here", "the reading at the gateway");
            player.snapTo(Vec3.atBottomCenterOf(far.west(20)));
            PolarCompassItem.update(level, player, compass);
            h.assertValueEqual(target(compass), far, "the bearing after a long way");
            player.snapTo(Vec3.atBottomCenterOf(near.east(170))); // 166 blocks back: nearer the first gateway now, but not far enough to search again
            PolarCompassItem.update(level, player, compass);
            h.assertValueEqual(target(compass), far, "the bearing a short way on (it shouldn't search again)");
            h.succeed();
        } finally {
            PolarGateways.standInForHollowEarth(null);
        }
    }

    private static @org.jspecify.annotations.Nullable BlockPos target(ItemStack compass) {
        var tracker = compass.get(DataComponents.LODESTONE_TRACKER);
        return tracker == null ? null : tracker.target().map(net.minecraft.core.GlobalPos::pos).orElse(null);
    }

    private static String key(java.util.Optional<net.minecraft.network.chat.Component> reading) {
        return reading.map(c -> c.getContents() instanceof net.minecraft.network.chat.contents.TranslatableContents t ? t.getKey() : c.getString()).orElse("none");
    }

    /** 31a: a broken airship drops with its coal and steam, and laying it out again puts them back (and makes it yours). */
    static void brokenAirshipKeepsItsFuel(GameTestHelper h) {
        ServerLevel level = h.getLevel();
        ServerPlayer owner = TestPlayers.survival(h, new BlockPos(2, 1, 6), "hf-airship-owner");
        Airship ship = airship(h, new Vec3(6.5, 1, 6.5), 5_000, 2_400);
        ship.setOwner(owner);
        ship.hurtServer(level, level.damageSources().playerAttack(owner), 100.0F);
        h.assertFalse(ship.isAlive(), "the owner couldn't break their airship");
        var drops = level.getEntitiesOfClass(net.minecraft.world.entity.item.ItemEntity.class, ship.getBoundingBox().inflate(3),
                drop -> drop.getItem().is(ModItems.AIRSHIP.get()));
        h.assertValueEqual(drops.size(), 1, "airship items dropped");
        ItemStack folded = drops.get(0).getItem().copy();
        h.assertValueEqual(AirshipItem.fuel(folded), 2_400, "coal on the folded airship");
        h.assertValueEqual(AirshipItem.steam(folded), 5_000, "steam on the folded airship");
        drops.forEach(drop -> drop.discard());

        ServerPlayer layer = TestPlayers.survival(h, new BlockPos(10, 1, 10), "hf-airship-layer");
        layer.setItemInHand(InteractionHand.MAIN_HAND, folded);
        BlockPos ground = h.absolutePos(new BlockPos(6, 0, 6));
        folded.useOn(new UseOnContext(layer, InteractionHand.MAIN_HAND,
                new BlockHitResult(Vec3.atCenterOf(ground), Direction.UP, ground, false)));
        var laid = level.getEntitiesOfClass(Airship.class, new net.minecraft.world.phys.AABB(ground).inflate(3));
        h.assertValueEqual(laid.size(), 1, "airships laid out");
        Airship again = laid.get(0);
        h.assertValueEqual(again.fuel(), 2_400, "coal after laying it out");
        h.assertValueEqual(again.steam(), 5_000, "steam after laying it out");
        h.assertTrue(layer.getUUID().equals(again.owner()), "whoever laid it out doesn't own it");
        again.discard();
        h.succeed();
    }

    private static BlockPos ring(GameTestHelper h, BlockPos center, boolean whole) {
        for (BlockPos offset : PolarGateways.RING) {
            h.setBlock(center.offset(offset), ModBlocks.GATEWAY_FRAME.get());
        }
        if (!whole) {
            h.setBlock(center.offset(PolarGateways.RING.get(0)), Blocks.STONE);
        }
        return center;
    }

    /**
     * 32c: the resonator charges from a steam tank like a canister and opens a ring on its own charge; with none left and no
     * canisters it draws from the player's airship close by, and takes nothing at all when even that falls short.
     */
    static void resonatorChargesAndDrawsFromAirship(GameTestHelper h) {
        ServerLevel level = h.getLevel();
        BlockPos first = ring(h, new BlockPos(3, 1, 3), true), second = ring(h, new BlockPos(8, 1, 8), true);
        BlockPos tankPos = new BlockPos(10, 1, 2);
        h.setBlock(tankPos, ModBlocks.STEAM_TANK.get());
        h.getBlockEntity(tankPos, SteamStorageBlockEntity.class).tank().add(ModFluids.steam(), 16_000);
        ServerPlayer player = TestPlayers.survival(h, new BlockPos(6, 1, 3), "hf-resonator-charger");
        ItemStack resonator = new ItemStack(ModItems.POLAR_RESONATOR.get());
        player.setItemInHand(InteractionHand.MAIN_HAND, resonator);
        BlockPos tank = h.absolutePos(tankPos);
        resonator.useOn(new UseOnContext(player, InteractionHand.MAIN_HAND, new BlockHitResult(Vec3.atCenterOf(tank), Direction.UP, tank, false)));
        h.assertValueEqual(PolarResonatorItem.charge(resonator), PolarResonatorItem.CAPACITY, "charge after a tank");

        h.useBlock(first.offset(PolarGateways.RING.get(0)), player);
        h.assertTrue(GatewayEffects.isWaking(level, h.absolutePos(first)), "it didn't open on its own charge");
        h.assertValueEqual(PolarResonatorItem.charge(resonator), 0, "charge after opening");
        h.assertTrue(resonator.has(ModDataComponents.RESONATED.get()), "opening a ring didn't set it ringing");

        Airship ship = airship(h, new Vec3(2, 1, 9), 500, 0);
        ship.setOwner(player);
        h.useBlock(second.offset(PolarGateways.RING.get(0)), player);
        h.assertFalse(GatewayEffects.isWaking(level, h.absolutePos(second)), "it opened on too little steam");
        h.assertValueEqual(ship.steam(), 500, "steam taken from the airship though it fell short");
        ship.setSteam(3_000);
        h.useBlock(second.offset(PolarGateways.RING.get(0)), player);
        h.assertTrue(GatewayEffects.isWaking(level, h.absolutePos(second)), "it didn't draw on the airship nearby");
        h.assertValueEqual(ship.steam(), 3_000 - PolarResonatorItem.OPEN_STEAM, "steam left in the airship");
        ship.discard();
        h.succeed();
    }

    /** The resonator opens a whole ring for 2000 mB of steam, and won't open a broken one or one without steam. */
    static void resonatorOpensGateway(GameTestHelper h) {
        BlockPos center = ring(h, new BlockPos(5, 1, 5), true);
        ServerPlayer player = TestPlayers.survival(h, new BlockPos(1, 1, 1));
        player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(ModItems.POLAR_RESONATOR.get()));
        BlockPos stone = center.offset(PolarGateways.RING.get(3));
        h.useBlock(stone, player);
        h.assertBlockNotPresent(ModBlocks.POLAR_PORTAL.get(), center);

        ItemStack canister = new ItemStack(ModItems.STEAM_CANISTER.get());
        SteamCanisterItem.setSteam(canister, SteamCanisterItem.CAPACITY);
        player.getInventory().setItem(9, canister);
        h.useBlock(stone, player);
        h.assertTrue(SteamCanisterItem.steam(canister) == SteamCanisterItem.CAPACITY - PolarResonatorItem.OPEN_STEAM, "steam spent: " + SteamCanisterItem.steam(canister));
        h.assertTrue(AdvancementTests.earned(player, "steam/open_sesame"), "opening a gateway earned nothing");
        h.useBlock(stone, player); // a second use while it wakes costs nothing
        h.assertTrue(SteamCanisterItem.steam(canister) == SteamCanisterItem.CAPACITY - PolarResonatorItem.OPEN_STEAM, "paid twice");

        // It wakes stone by stone round the ring, then the well floods.
        h.runAfterDelay(GatewayEffects.STONE_TICKS * 4 + 1, () -> {
            long lit = PolarGateways.RING.stream().filter(o -> h.getBlockState(center.offset(o)).getValue(GatewayFrameBlock.ACTIVE)).count();
            h.assertTrue(lit >= 3 && lit < PolarGateways.RING.size(), "stones should light one by one, " + lit + " lit");
            h.assertBlockNotPresent(ModBlocks.POLAR_PORTAL.get(), center);
        });
        h.runAfterDelay(GatewayEffects.WAKE_TICKS + 2, () -> {
            for (int dx = -1; dx <= 1; dx++) {
                for (int dz = -1; dz <= 1; dz++) {
                    h.assertBlockPresent(ModBlocks.POLAR_PORTAL.get(), center.offset(dx, 0, dz));
                }
            }
            h.assertTrue(PolarGateways.RING.stream().allMatch(o -> h.getBlockState(center.offset(o)).getValue(GatewayFrameBlock.ACTIVE)),
                    "the ring didn't all light");

            player.setShiftKeyDown(true);
            h.useBlock(stone, player);
            h.assertBlockNotPresent(ModBlocks.POLAR_PORTAL.get(), center);
            h.assertFalse(h.getBlockState(stone).getValue(GatewayFrameBlock.ACTIVE), "the ring stayed lit after closing");
            player.setShiftKeyDown(false);

            BlockPos broken = ring(h, new BlockPos(5, 1, 5), false);
            h.useBlock(center.offset(PolarGateways.RING.get(5)), player);
            h.assertFalse(GatewayEffects.isWaking(h.getLevel(), h.absolutePos(broken)), "a broken ring started waking");
            h.succeed();
        });
    }

    /**
     * Through an open gateway: the first crossing builds a lit return gateway inside and links the two; the player lands
     * beside it, a Polar Compass there points to it, and the way back leads home. The game test server loads no datapack
     * dimensions, so the Nether stands in for the Hollow Earth here (whose own registry entries are checked first).
     */
    static void gatewayLeadsToHollowEarthAndBack(GameTestHelper h) {
        var registries = h.getLevel().registryAccess();
        h.assertTrue(registries.lookupOrThrow(Registries.DIMENSION_TYPE).get(HollowEarth.TYPE).isPresent(), "no Hollow Earth dimension type");
        h.assertTrue(registries.lookupOrThrow(Registries.BIOME).get(HollowEarth.PRIMORDIAL_JUNGLE).isPresent(), "no Primordial Jungle biome");
        PolarGateways.standInForHollowEarth(Level.NETHER);
        try {
            crossAndReturn(h);
        } finally {
            PolarGateways.standInForHollowEarth(null);
        }
    }

    private static void crossAndReturn(GameTestHelper h) {
        BlockPos center = ring(h, new BlockPos(5, 1, 5), true);
        ServerLevel overworld = h.getLevel();
        PolarGateways.open(overworld, h.absolutePos(center));
        ServerPlayer player = TestPlayers.survival(h, new BlockPos(1, 1, 1));
        BlockPos absCenter = h.absolutePos(center);

        TeleportTransition in = PolarGateways.destination(overworld, player, absCenter.east());
        h.assertTrue(in != null && in.newLevel().dimension() == PolarGateways.inside(), "the gateway doesn't lead inside");
        ServerLevel hollow = in.newLevel();
        GatewayLinks.Link link = GatewayLinks.get(overworld).fromOverworld(absCenter).orElse(null);
        h.assertTrue(link != null, "no link recorded for the gateway");
        h.assertTrue(PolarGateways.isRing(hollow, link.hollow()) && PolarGateways.isOpen(hollow, link.hollow()), "no open return gateway inside");
        h.assertTrue(link.hollow().getX() == absCenter.getX() && link.hollow().getZ() == absCenter.getZ(), "the return gateway isn't at the same x/z");
        TeleportTransition again = PolarGateways.destination(overworld, player, absCenter);
        h.assertTrue(again.position().equals(in.position()), "a second crossing landed somewhere else");
        h.assertTrue(in.position().y > link.hollow().getY() + 10, "a player should come out high above the return gateway: " + in.position());

        player.teleport(in);
        h.assertTrue(player.level().dimension() == PolarGateways.inside(), "the player didn't cross");
        var drift = player.getEffect(MobEffects.SLOW_FALLING);
        h.assertTrue(drift != null && drift.isAmbient() && !drift.isVisible(), "no gentle drift down after crossing");
        h.assertTrue(player.getData(ModAttachments.VISITED_HOLLOW_EARTH), "the first visit wasn't recorded (no title card)");
        ItemStack compass = new ItemStack(ModItems.POLAR_COMPASS.get());
        player.setItemInHand(InteractionHand.MAIN_HAND, compass);
        compass.use(hollow, player, InteractionHand.MAIN_HAND);
        var tracker = compass.get(DataComponents.LODESTONE_TRACKER);
        h.assertTrue(tracker != null && tracker.target().isPresent() && tracker.target().get().pos().equals(link.hollow()),
                "the compass doesn't point to the return gateway inside: " + tracker);
        h.assertTrue(AdvancementTests.earned(player, "steam/true_north"), "taking a bearing earned nothing");

        TeleportTransition out = PolarGateways.destination(hollow, player, link.hollow().north());
        h.assertTrue(out.newLevel().dimension() == Level.OVERWORLD, "the return gateway doesn't lead home");
        h.assertTrue(out.position().distanceTo(Vec3.atCenterOf(absCenter)) < 4, "came out far from the gateway: " + out.position());
        player.teleport(out);
        h.assertTrue(player.hasEffect(MobEffects.LEVITATION), "coming home should lift the player out of the well");
        h.succeed();
    }

    /** The crater template: twelve dark frame stones round an empty well, and the expedition's chest. */
    static void gatewayTemplateHasRingAndChest(GameTestHelper h) {
        StructureTemplate template = h.getLevel().getStructureTemplateManager().get(HollowFrontier.id("polar_gateway")).orElse(null);
        h.assertTrue(template != null, "polar_gateway template missing");
        StructurePlaceSettings settings = new StructurePlaceSettings();
        var frames = template.filterBlocks(BlockPos.ZERO, settings, ModBlocks.GATEWAY_FRAME.get());
        h.assertTrue(frames.size() == 12, "expected 12 frame stones, found " + frames.size());
        h.assertTrue(frames.stream().noneMatch(info -> info.state().getValue(GatewayFrameBlock.ACTIVE)), "a frame stone is lit in the template");
        BlockPos well = new BlockPos(PolarBlueprints.CENTER, PolarBlueprints.RING_Y, PolarBlueprints.CENTER);
        h.assertTrue(PolarGateways.RING.stream().allMatch(offset -> frames.stream().anyMatch(info -> info.pos().equals(well.offset(offset)))),
                "the frame stones don't ring the well");
        h.assertTrue(template.filterBlocks(BlockPos.ZERO, settings, Blocks.AIR).stream().anyMatch(info -> info.pos().equals(well)),
                "the well should be empty until a resonator opens it");
        boolean chest = template.filterBlocks(BlockPos.ZERO, settings, Blocks.CHEST).stream().anyMatch(info -> {
            CompoundTag nbt = info.nbt();
            return nbt != null && nbt.getStringOr("LootTable", "").equals(ModChestLoot.POLAR_GATEWAY.identifier().toString());
        });
        h.assertTrue(chest, "no expedition chest");
        h.succeed();
    }

    /** Climbing past height 200 with the pilot aboard earns Above the Clouds (the altitude trigger's real path). */
    static void highAltitudeEarnsAdvancement(GameTestHelper h) {
        Airship ship = airship(h, new Vec3(6, 1, 6), 8000, 0);
        ServerPlayer pilot = TestPlayers.survival(h, new BlockPos(6, 1, 4));
        pilot.startRiding(ship, true, false);
        ship.teleportTo(ship.getX(), 205, ship.getZ());
        h.succeedWhen(() -> {
            h.assertTrue(AdvancementTests.earned(pilot, "steam/above_the_clouds"), "flying at " + (int) ship.getY() + " earned nothing");
            pilot.stopRiding();
            ship.discard();
        });
    }
}
