package com.hollowfrontier.gametest;

import com.hollowfrontier.content.ufo.Leash;
import com.hollowfrontier.content.ufo.Mothership;
import com.hollowfrontier.content.ufo.PrototypeSaucer;
import com.hollowfrontier.content.ufo.PrototypeSaucerItem;
import com.hollowfrontier.content.ufo.SaucerFittings;
import com.hollowfrontier.registry.ModEntities;
import com.hollowfrontier.registry.ModItems;

import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.monster.zombie.Husk;
import net.minecraft.world.inventory.ChestMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.ItemContainerContents;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

/**
 * Phase 15b: the Mothership's return. Each rematch a step harder, up to the cap, and the count kept with the leash; a
 * rematch's fitting (the cloak, then the hold); the cloak hiding a crew from mobs at night; the cargo hold, kept through
 * folding; and the owner lock on folding and fitting. The world's count is only ever changed and put back in one tick.
 */
final class MothershipReturnTests {
    private MothershipReturnTests() {}

    /** A Mothership in the fight at once, {@code at} (relative), after {@code fallen} others; its fall breaks and counts nothing. */
    private static Mothership ship(GameTestHelper h, BlockPos at, int fallen) {
        Mothership ship = ModEntities.MOTHERSHIP.get().create(h.getLevel(), EntitySpawnReason.TRIGGERED);
        ship.snapTo(Vec3.atBottomCenterOf(h.absolutePos(at)));
        ship.setAnchor(h.absolutePos(new BlockPos(at.getX(), 1, at.getZ())));
        ship.setEndsTheLeash(false);
        ship.setEncore(fallen);
        ship.engage();
        h.getLevel().addFreshEntity(ship);
        return ship;
    }

    private static PrototypeSaucer saucer(GameTestHelper h, BlockPos at, SaucerFittings fittings) {
        PrototypeSaucer saucer = PrototypeSaucerItem.place(h.getLevel(), h.absolutePos(at), 0.0F, PrototypeSaucerItem.of(fittings, 64));
        h.assertTrue(saucer != null, "no room to set the saucer down");
        h.getLevel().addFreshEntity(saucer);
        return saucer;
    }

    private static ItemStack folded(ServerPlayer player) {
        for (int slot = 0; slot < player.getInventory().getContainerSize(); slot++) {
            if (player.getInventory().getItem(slot).is(ModItems.PROTOTYPE_SAUCER.get())) {
                return player.getInventory().getItem(slot);
            }
        }
        return ItemStack.EMPTY;
    }

    /**
     * The count: each fall adds one, the rematch step stops at the cap, and a summoned ship takes it up. A rematch comes
     * with more health, bigger waves (the shield's too) and a quicker death ray; past the cap, no harder.
     */
    static void rematchesComeHarder(GameTestHelper h) {
        int was = Leash.fallen(h.getLevel());
        try {
            Leash.setFallen(h.getLevel(), 2);
            h.assertValueEqual(Leash.rematch(h.getLevel()), 2, "rematch after two falls");
            h.assertValueEqual(Leash.countFallen(h.getLevel()), 3, "fallen after one more");
            Leash.setFallen(h.getLevel(), 9);
            h.assertValueEqual(Leash.rematch(h.getLevel()), Leash.MAX_REMATCH, "rematch past the cap");
            Mothership summoned = Mothership.summon(h.getLevel(), h.absolutePos(new BlockPos(20, 1, 20)));
            h.assertValueEqual(summoned.encore(), 9, "a summoned ship's encore");
            summoned.discard();
        } finally {
            Leash.setFallen(h.getLevel(), was);
        }
        Mothership first = ship(h, new BlockPos(8, 16, 20), 0);
        Mothership second = ship(h, new BlockPos(20, 16, 20), 2);
        Mothership last = ship(h, new BlockPos(32, 16, 20), 12);
        h.assertValueEqual(first.getMaxHealth(), 600.0F, "the first's health");
        h.assertValueEqual(second.getMaxHealth(), 780.0F, "the second rematch's health");
        h.assertValueEqual(last.getMaxHealth(), 960.0F, "health past the cap");
        h.assertValueEqual(second.getHealth(), second.getMaxHealth(), "a rematch comes at full health");
        h.assertValueEqual(second.droneWave(Mothership.DRONES), Mothership.DRONE_WAVE[Mothership.DRONES] + 2, "the second rematch's waves");
        h.assertValueEqual(second.maxDrones(), Mothership.MAX_DRONES + 2, "the second rematch's drones up");
        h.assertTrue(second.rayInterval(Mothership.EXPOSED_CORE) < first.rayInterval(Mothership.EXPOSED_CORE), "the rematch's ray isn't quicker");
        h.assertValueEqual(last.rematch(), Leash.MAX_REMATCH, "a rematch past the cap");
        second.setHealth(second.getMaxHealth() * 0.6F);
        h.runAfterDelay(2, () -> {
            h.assertTrue(second.isShielded(), "no shield at the turn");
            h.assertValueEqual(second.shieldWave().size(), Mothership.SHIELD_WAVE + 2, "the rematch's shield wave");
            first.discard();
            second.discard();
            last.discard();
            h.succeed();
        });
    }

    /** The first gives up no fitting; the first rematch the cloaking device, the next the cargo hold. */
    static void rematchesGiveUpFittings(GameTestHelper h) {
        Mothership first = ship(h, new BlockPos(6, 4, 20), 0);
        Mothership cloak = ship(h, new BlockPos(20, 4, 20), 1);
        Mothership hold = ship(h, new BlockPos(34, 4, 20), 2);
        ServerPlayer player = TestPlayers.survival(h, new BlockPos(20, 1, 4));
        h.runAfterDelay(2, () -> {
            for (Mothership ship : new Mothership[] {first, cloak, hold}) {
                ship.hurtServer(h.getLevel(), h.getLevel().damageSources().playerAttack(player), 100000);
            }
        });
        h.runAfterDelay(30, () -> {
            for (int x : new int[] {6, 20, 34}) {
                var drops = h.getLevel().getEntitiesOfClass(ItemEntity.class, new AABB(h.absolutePos(new BlockPos(x, 1, 20))).inflate(5, 6, 5));
                boolean cloaks = drops.stream().anyMatch(i -> i.getItem().is(ModItems.CLOAKING_DEVICE.get()));
                boolean holds = drops.stream().anyMatch(i -> i.getItem().is(ModItems.CARGO_HOLD.get()));
                h.assertValueEqual(cloaks, x == 20, "a cloaking device under the ship at x " + x);
                h.assertValueEqual(holds, x == 34, "a cargo hold under the ship at x " + x);
                drops.forEach(ItemEntity::discard);
            }
            for (Mothership ship : new Mothership[] {first, cloak, hold}) {
                ship.discard();
            }
            h.succeed();
        });
    }

    /**
     * Cloaked at night, a saucer's pilot goes invisible and a husk hunting them loses them, and can't take them up again;
     * by day the cloak drops, and the husk can.
     */
    static void cloakHidesTheCrewAtNight(GameTestHelper h) {
        PrototypeSaucer saucer = saucer(h, new BlockPos(20, 1, 20), new SaucerFittings(true, true, false, true, false));
        saucer.setDarkForTests(true);
        ServerPlayer pilot = TestPlayers.survival(h, new BlockPos(17, 1, 20));
        pilot.startRiding(saucer, true, true);
        Husk husk = h.spawn(EntityTypes.HUSK, new BlockPos(12, 1, 20));
        husk.setNoAi(true);
        husk.setTarget(pilot);
        h.assertTrue(husk.getTarget() == pilot, "the husk didn't take up the pilot before the cloak");
        h.runAfterDelay(12, () -> {
            h.assertTrue(saucer.isCloaked(), "it isn't cloaked at night with a pilot");
            h.assertTrue(pilot.hasEffect(MobEffects.INVISIBILITY), "the pilot isn't invisible");
            h.assertTrue(husk.getTarget() == null, "the husk is still on the pilot");
            husk.setTarget(pilot);
            h.assertTrue(husk.getTarget() == null, "the husk took up a cloaked pilot");
            saucer.setDarkForTests(false);
        });
        h.runAfterDelay(15, () -> {
            h.assertFalse(saucer.isCloaked(), "it stayed cloaked by day");
            husk.setTarget(pilot);
            h.assertTrue(husk.getTarget() == pilot, "the husk can't take up the pilot by day");
            pilot.stopRiding();
            h.succeed();
        });
    }

    /**
     * Its hold opens for its crew (not without the fitting), closes when they climb out, and goes with the saucer when
     * it's folded and set down again.
     */
    static void cargoHoldKeepsThroughFolding(GameTestHelper h) {
        PrototypeSaucer plain = saucer(h, new BlockPos(8, 1, 20), SaucerFittings.STANDARD);
        PrototypeSaucer saucer = saucer(h, new BlockPos(24, 1, 20), new SaucerFittings(true, true, false, false, true));
        ServerPlayer owner = TestPlayers.survival(h, new BlockPos(20, 1, 20), "hf-hold-owner");
        saucer.setOwner(owner);
        plain.setOwner(owner);
        owner.startRiding(plain, true, true);
        h.assertValueEqual(plain.openHold(owner), "no_hold", "a saucer without a hold");
        owner.stopRiding();
        h.assertValueEqual(saucer.openHold(owner), "hold_outside", "opening the hold from outside");
        owner.startRiding(saucer, true, true);
        h.assertTrue(saucer.openHold(owner) == null, "the hold didn't open");
        h.assertTrue(owner.containerMenu instanceof ChestMenu menu && menu.getContainer() == saucer.cargo(), "the window isn't the hold");
        saucer.cargo().setItem(0, new ItemStack(Items.GOLD_INGOT, 5));
        saucer.cargo().setItem(8, new ItemStack(ModItems.PHLOGISTON_SHARD.get(), 2));
        h.assertTrue(saucer.cargo().stillValid(owner), "the hold won't stay open for its crew");
        owner.stopRiding();
        h.assertFalse(saucer.cargo().stillValid(owner), "the hold stays open with its viewer outside");
        owner.closeContainer();
        h.runAfterDelay(3, () -> {
            h.assertTrue(saucer.fold(owner) == null, "it didn't fold");
            ItemStack item = folded(owner);
            var contents = item.getOrDefault(DataComponents.CONTAINER, ItemContainerContents.EMPTY);
            h.assertValueEqual(contents.nonEmptyItemCopyStream().count(), 2L, "stacks in the folded saucer");
            h.assertTrue(PrototypeSaucerItem.fittings(item).hold(), "the folded saucer lost its hold");
            PrototypeSaucer again = PrototypeSaucerItem.place(h.getLevel(), h.absolutePos(new BlockPos(24, 1, 20)), 0.0F, item);
            h.assertTrue(again != null, "no room to set it down again");
            h.assertTrue(again.cargo().getItem(0).is(Items.GOLD_INGOT) && again.cargo().getItem(0).getCount() == 5
                    && again.cargo().getItem(8).is(ModItems.PHLOGISTON_SHARD.get()), "the hold came back different");
            again.discard();
            plain.discard();
            h.succeed();
        });
    }

    /**
     * Only its owner and their team may fit parts to it or fold it: a stranger is turned away (and keeps their part); a
     * teammate isn't. A saucer with no owner yet is the first handler's.
     */
    static void ownerLockHolds(GameTestHelper h) {
        PrototypeSaucer saucer = saucer(h, new BlockPos(20, 1, 20), SaucerFittings.STANDARD);
        ServerPlayer owner = TestPlayers.survival(h, new BlockPos(16, 1, 20), "hf-saucer-owner");
        ServerPlayer stranger = TestPlayers.survival(h, new BlockPos(24, 1, 20), "hf-saucer-stranger");
        ServerPlayer friend = TestPlayers.survival(h, new BlockPos(20, 1, 24), "hf-saucer-friend");
        h.assertTrue(saucer.owner() == null, "a placed-by-test saucer has an owner");
        h.assertTrue(saucer.mayHandle(owner), "the first handler was refused");
        h.assertTrue(owner.getUUID().equals(saucer.owner()), "the first handler didn't claim it");
        stranger.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(ModItems.NAVIGATION_CORE.get()));
        saucer.interact(stranger, InteractionHand.MAIN_HAND, saucer.position());
        h.assertFalse(saucer.fittings().nav(), "a stranger fitted a part");
        h.assertValueEqual(stranger.getMainHandItem().getCount(), 1, "a stranger's part after being refused");
        h.assertValueEqual(saucer.fold(stranger), "not_yours", "a stranger folding it");
        var scoreboard = h.getLevel().getScoreboard();
        var team = scoreboard.addPlayerTeam("hf_saucer_crew");
        try {
            scoreboard.addPlayerToTeam(owner.getScoreboardName(), team);
            scoreboard.addPlayerToTeam(friend.getScoreboardName(), team);
            h.assertTrue(saucer.mayHandle(friend), "the owner's teammate was refused");
            h.assertFalse(saucer.mayHandle(stranger), "a stranger got in with a team about");
            h.assertTrue(saucer.fold(friend) == null, "the teammate couldn't fold it");
            h.assertTrue(PrototypeSaucerItem.fittings(folded(friend)).lens(), "the teammate didn't get the saucer");
        } finally {
            scoreboard.removePlayerTeam(team);
        }
        h.succeed();
    }
}
