package com.hollowfrontier.gametest;

import com.hollowfrontier.content.frontier.mystery.ManInBlack;
import com.hollowfrontier.content.frontier.mystery.MenInBlack;
import com.hollowfrontier.content.frontier.mystery.Suspicion;
import com.hollowfrontier.content.frontier.outlaw.AbstractOutlaw;
import com.hollowfrontier.content.ufo.GrayDrone;
import com.hollowfrontier.content.ufo.Leash;
import com.hollowfrontier.content.ufo.Mothership;
import com.hollowfrontier.content.ufo.PhlogistonBeaconBlock;
import com.hollowfrontier.registry.ModBlocks;
import com.hollowfrontier.registry.ModEntities;
import com.hollowfrontier.registry.ModItems;

import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.monster.zombie.Husk;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

/**
 * Phase 13d: the Mothership. The Phlogiston Beacon's refusals and its call; the shield behind each wave of drones; the death
 * ray on its marked spot; the abduction beam over a player who isn't sneaking; the fall and its salvage; and the leash
 * breaking, freeing the implanted and stopping suspicion. On the 40-block field. The world's leash is only ever broken
 * and mended again inside one tick, so the other tests running alongside never see it broken.
 */
final class MothershipTests {
    private MothershipTests() {}

    /** A Mothership in the fight at once, {@code at} (relative), circling {@code anchor}; its fall breaks nothing. */
    private static Mothership ship(GameTestHelper h, BlockPos at, BlockPos anchor) {
        Mothership ship = ModEntities.MOTHERSHIP.get().create(h.getLevel(), EntitySpawnReason.TRIGGERED);
        ship.snapTo(Vec3.atBottomCenterOf(h.absolutePos(at)));
        ship.setAnchor(h.absolutePos(anchor));
        ship.setEndsTheLeash(false);
        ship.engage();
        h.getLevel().addFreshEntity(ship);
        return ship;
    }

    // A husk: it stands still, and unlike a zombie it doesn't burn in the sun of its own accord
    private static Husk dummy(GameTestHelper h, BlockPos at) {
        Husk zombie = h.spawn(EntityTypes.HUSK, at);
        zombie.setNoAi(true);
        return zombie;
    }

    /** It won't call with a roof over it; lit, it counts down and the Mothership comes; its fall puts the beacon out. */
    static void beaconCallsTheMothership(GameTestHelper h) {
        BlockPos at = new BlockPos(20, 1, 20);
        h.setBlock(at, ModBlocks.PHLOGISTON_BEACON.get());
        BlockPos beacon = h.absolutePos(at);
        h.setBlock(at.above(3), Blocks.STONE);
        h.assertValueEqual(PhlogistonBeaconBlock.refusal(h.getLevel(), beacon), "no_sky", "refusal under a roof");
        h.setBlock(at.above(3), Blocks.AIR);
        h.assertTrue(PhlogistonBeaconBlock.refusal(h.getLevel(), beacon) == null, "it refused under the open sky");
        PhlogistonBeaconBlock.light(h.getLevel(), beacon);
        h.assertTrue(h.getBlockState(at).getValue(PhlogistonBeaconBlock.LIT), "it didn't light");
        h.assertValueEqual(PhlogistonBeaconBlock.refusal(h.getLevel(), beacon), "already", "refusal while lit");
        h.runAfterDelay(105, () -> {
            var ships = h.getLevel().getEntitiesOfClass(Mothership.class, new AABB(beacon).inflate(4).expandTowards(0, Mothership.ARRIVE_FROM + 4, 0));
            h.assertValueEqual(ships.size(), 1, "Motherships come");
            Mothership ship = ships.getFirst();
            h.assertTrue(ship.anchor().equals(beacon) && ship.getY() > beacon.getY() + Mothership.HOVER, "it isn't coming down over the beacon");
            ship.setEndsTheLeash(false);
            ship.kill(h.getLevel());
            h.assertFalse(h.getBlockState(at).getValue(PhlogistonBeaconBlock.LIT), "the beacon is still lit");
            ship.discard();
            h.succeed();
        });
    }

    /** At two thirds it shields itself behind a wave of drones: nothing harms it till that wave is down, whatever else flies. */
    static void shieldHoldsWhileDronesFly(GameTestHelper h) {
        Mothership ship = ship(h, new BlockPos(20, 16, 20), new BlockPos(20, 1, 20));
        ship.setHealth(ship.getMaxHealth() * 0.6F);
        h.runAfterDelay(2, () -> {
            h.assertValueEqual(ship.phase(), Mothership.ABDUCTION, "phase at 60%");
            h.assertTrue(ship.isShielded(), "no shield at the turn");
            h.assertValueEqual(ship.dronesUp(h.getLevel()), Mothership.SHIELD_WAVE, "drones up");
            float health = ship.getHealth();
            ship.hurtServer(h.getLevel(), h.getLevel().damageSources().magic(), 20);
            h.assertValueEqual(ship.getHealth(), health, "health through the shield");
            // An ordinary drone joins; only the shield's own wave is brought down, and that's enough
            java.util.UUID ordinary = ship.launchDrone(h.getLevel());
            var wave = ship.shieldWave();
            h.assertValueEqual(wave.size(), Mothership.SHIELD_WAVE, "the shield's wave");
            h.getLevel().getEntitiesOfClass(GrayDrone.class, ship.getBoundingBox().inflate(16), d -> wave.contains(d.getUUID())).forEach(drone -> drone.discard());
            h.assertTrue(h.getLevel().getEntity(ordinary) != null, "the ordinary drone went too");
        });
        h.runAfterDelay(5, () -> {
            h.assertFalse(ship.isShielded(), "the shield held with its wave down (an ordinary drone still up)");
            float health = ship.getHealth();
            ship.hurtServer(h.getLevel(), h.getLevel().damageSources().magic(), 20);
            h.assertTrue(ship.getHealth() < health, "it took no harm with its shield down");
            ship.discard();
            h.succeed();
        });
    }

    /** The death ray falls on the spot it marked, a while after: the zombie there burns, one ten blocks off doesn't. */
    static void deathRayFallsOnItsMark(GameTestHelper h) {
        Mothership ship = ship(h, new BlockPos(20, 20, 20), new BlockPos(20, 1, 20));
        Husk marked = dummy(h, new BlockPos(20, 1, 20));
        Husk spared = dummy(h, new BlockPos(30, 1, 20));
        TestPlayers.survival(h, new BlockPos(4, 1, 4)); // something for it to fight
        ship.startRay(marked.position());
        h.runAfterDelay(Mothership.RAY_CHARGE - 10, () -> h.assertTrue(marked.getHealth() == marked.getMaxHealth(), "it fell before its charge"));
        h.runAfterDelay(Mothership.RAY_CHARGE + 3, () -> {
            h.assertTrue(marked.getHealth() < marked.getMaxHealth() && marked.isOnFire(), "the marked zombie wasn't burned");
            h.assertTrue(spared.getHealth() == spared.getMaxHealth(), "it burned one off its mark");
            ship.discard();
            h.succeed();
        });
    }

    /** Over a player, its beam draws them up; a sneaking one beside them stays down. */
    static void abductionBeamLifts(GameTestHelper h) {
        Mothership ship = ship(h, new BlockPos(20, 16, 20), new BlockPos(20, 1, 20));
        ship.setHealth(ship.getMaxHealth() * 0.5F);
        ServerPlayer taken = TestPlayers.survival(h, new BlockPos(20, 1, 20));
        ServerPlayer crouching = TestPlayers.survival(h, new BlockPos(21, 1, 21));
        crouching.setShiftKeyDown(true);
        h.runAfterDelay(2, () -> ship.startBeam(taken));
        h.runAfterDelay(6, () -> {
            h.assertTrue(ship.isBeaming(), "the beam isn't on");
            h.assertTrue(taken.getDeltaMovement().y > 0.2, "the player isn't being drawn up: " + taken.getDeltaMovement().y);
            h.assertTrue(crouching.getDeltaMovement().y < 0.1, "it drew up a sneaking player");
            ship.discard();
            h.succeed();
        });
    }

    /** Brought down, it burns and breaks apart, leaving its salvage (and its drones gone with it). */
    static void mothershipFallsAndLeavesSalvage(GameTestHelper h) {
        Mothership ship = ship(h, new BlockPos(20, 6, 20), new BlockPos(20, 1, 20));
        ServerPlayer player = TestPlayers.survival(h, new BlockPos(4, 1, 4));
        h.runAfterDelay(2, () -> ship.hurtServer(h.getLevel(), h.getLevel().damageSources().playerAttack(player), 100000));
        h.runAfterDelay(Mothership.DEATH_TICKS + 10, () -> {
            h.assertTrue(ship.isRemoved(), "the wreck is still about");
            var drops = h.getLevel().getEntitiesOfClass(ItemEntity.class, new AABB(h.absolutePos(new BlockPos(20, 1, 20))).inflate(12, 12, 12));
            h.assertTrue(drops.stream().anyMatch(i -> i.getItem().is(ModItems.GRAVITY_DRIVE.get())), "no gravity drive in the wreck");
            h.assertTrue(drops.stream().anyMatch(i -> i.getItem().is(ModItems.ALIEN_ALLOY_PLATING.get())), "no plating in the wreck");
            drops.forEach(ItemEntity::discard);
            h.succeed();
        });
    }

    /**
     * The leash breaking: an outlaw near throws his gun down and fights no more, a man in black is gone; and with it
     * broken, suspicion can't rise and the men in black send no one. (Broken and mended again in the same tick.)
     */
    static void leashBreaksAndFrees(GameTestHelper h) {
        AbstractOutlaw outlaw = ModEntities.OUTLAW.get().create(h.getLevel(), EntitySpawnReason.MOB_SUMMONED);
        outlaw.snapTo(Vec3.atBottomCenterOf(h.absolutePos(new BlockPos(18, 1, 20))));
        outlaw.finalizeSpawn(h.getLevel(), h.getLevel().getCurrentDifficultyAt(outlaw.blockPosition()), EntitySpawnReason.MOB_SUMMONED, null);
        h.getLevel().addFreshEntity(outlaw);
        ManInBlack man = h.spawn(ModEntities.MAN_IN_BLACK.get(), new BlockPos(22, 1, 20));
        ServerPlayer player = TestPlayers.survival(h, new BlockPos(20, 1, 24));
        h.assertTrue(outlaw.getMainHandItem().getItem() != net.minecraft.world.item.Items.AIR, "the outlaw came unarmed");
        h.runAfterDelay(2, () -> {
            int freed = Leash.free(h.getLevel(), Vec3.atCenterOf(h.absolutePos(new BlockPos(20, 1, 20))), 12);
            h.assertValueEqual(freed, 2, "freed");
            h.assertTrue(outlaw.isFreed() && outlaw.getMainHandItem().isEmpty() && outlaw.getTarget() == null, "the outlaw is still armed");
            h.assertTrue(man.isRemoved(), "the man in black is still about");
            h.assertTrue(h.getLevel().getEntitiesOfClass(ItemEntity.class, outlaw.getBoundingBox().inflate(3)).stream()
                    .anyMatch(i -> i.getItem().getItem() instanceof com.hollowfrontier.content.frontier.gun.GunItem), "he didn't throw his gun down");
            boolean was = Leash.isBroken(h.getLevel());
            try {
                Leash.set(h.getLevel(), true);
                int before = Suspicion.level(player);
                Suspicion.add(player, 30);
                h.assertValueEqual(Suspicion.level(player), before, "suspicion with the leash broken");
                h.assertTrue(MenInBlack.huntIfDue(player).isEmpty() && MenInBlack.visitIfDue(h.getLevel()).isEmpty(), "they still came");
            } finally {
                Leash.set(h.getLevel(), was);
            }
            outlaw.discard();
            h.succeed();
        });
    }
}
