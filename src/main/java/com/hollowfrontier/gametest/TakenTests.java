package com.hollowfrontier.gametest;

import java.util.List;
import java.util.Optional;

import com.hollowfrontier.content.frontier.mystery.Suspicion;
import com.hollowfrontier.content.hollow.gray.Captives;
import com.hollowfrontier.content.ufo.GrayWarden;
import com.hollowfrontier.content.ufo.Leash;
import com.hollowfrontier.content.ufo.UfoConfig;
import com.hollowfrontier.content.ufo.taken.CoreSocketBlock;
import com.hollowfrontier.content.ufo.taken.Ship;
import com.hollowfrontier.content.ufo.taken.ShipConsoleBlock;
import com.hollowfrontier.content.ufo.taken.ShipLayout;
import com.hollowfrontier.content.ufo.taken.ShipLayout.Side;
import com.hollowfrontier.content.ufo.taken.ShockRodItem;
import com.hollowfrontier.content.ufo.taken.Taken;
import com.hollowfrontier.content.ufo.taken.TakenData;
import com.hollowfrontier.registry.ModAttachments;
import com.hollowfrontier.registry.ModBlocks;
import com.hollowfrontier.registry.ModItems;
import com.hollowfrontier.worldgen.structure.ShipBlueprints;

import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.Difficulty;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

/**
 * Phase 50: taken. Who the Grays keep; the kit whole after every way off; two in one beam on one ship; a ship cleared
 * when it's empty; what the crew make out, walking, sneaking and in the vents; the Shock Rod; the alarm and its doors;
 * what breaks aboard and what doesn't; the core's cells and the fall; a pod with everybody in it; a crash site raised.
 * <p>
 * The game test server has no world of the Grays' own, so ships are raised high over this one, far from the tests, and
 * each test takes its player from a stone platform of its own, a long way off, and sets them back down on it.
 */
final class TakenTests {
    private TakenTests() {}

    /** A 9 x 9 stone platform far from everything, one for each test: its middle (the block to stand on). */
    private static BlockPos platform(ServerLevel level, int slot) {
        BlockPos middle = new BlockPos(20000 + slot * 400, 100, 20000);
        // Kept loaded: a fake player keeps no chunks, and a captive set down in one that isn't loaded is put away with it
        level.setChunkForced(middle.getX() >> 4, middle.getZ() >> 4, true);
        for (BlockPos at : BlockPos.betweenClosed(middle.offset(-4, 0, -4), middle.offset(4, 0, 4))) {
            level.setBlockAndUpdate(at, Blocks.STONE.defaultBlockState());
        }
        return middle;
    }

    /** A survival player standing on their test's platform, with the stand-in world set and the difficulty the crew need. */
    private static ServerPlayer onPlatform(GameTestHelper h, int slot, String name) {
        Taken.standIn(Level.OVERWORLD, 0);
        h.getLevel().getServer().setDifficulty(Difficulty.NORMAL, true); // peaceful would send the crew away
        ServerPlayer player = TestPlayers.survival(h, new BlockPos(1, 2, 1), name);
        BlockPos middle = platform(h.getLevel(), slot);
        player.teleportTo(middle.getX() + 0.5, middle.getY() + 1, middle.getZ() + 0.5);
        return player;
    }

    private static void move(ServerPlayer player, Ship ship, BlockPos local) {
        Vec3 at = ShipLayout.feet(ship.origin(), local);
        player.teleportTo(at.x, at.y, at.z);
    }

    private static int count(ServerPlayer player, net.minecraft.world.item.Item item) {
        int count = 0;
        for (ItemStack stack : player.getInventory()) {
            if (stack.is(item)) {
                count += stack.getCount();
            }
        }
        return count;
    }

    /** Nobody is kept until the men in black mind them; then they are; never in creative, with the leash broken, or twice at once. */
    static void keptByTheRule(GameTestHelper h) {
        ServerLevel level = h.getLevel();
        ServerPlayer player = onPlatform(h, 0, "hf-taken-rule");
        ServerPlayer creative = TestPlayers.creative(h, new BlockPos(1, 2, 1));
        Leash.set(level, false);
        h.assertFalse(Taken.keeps(level, player), "kept somebody the men in black have never heard of");
        Suspicion.add(player, UfoConfig.takenSuspicion());
        h.assertTrue(Taken.keeps(level, player), "not kept at suspicion " + Suspicion.level(player));
        creative.setData(ModAttachments.SUSPICION.get(), new Suspicion(Suspicion.MAX, 0, 0));
        h.assertFalse(Taken.keeps(level, creative), "kept a creative player");
        Leash.set(level, true);
        h.assertFalse(Taken.keeps(level, player), "kept somebody with the leash broken");
        Leash.set(level, false);
        Ship ship = Taken.take(level, player);
        h.assertTrue(ship != null && Taken.shipOf(player) == ship, "not aboard");
        h.assertTrue(TakenData.of(player).aboard() && TakenData.of(player).times() == 1, "not on the books: " + TakenData.of(player));
        h.assertFalse(Taken.keeps(level, player), "kept somebody already aboard");
        h.assertTrue(AdvancementTests.earned(player, "ufo/taken"), "Taken not earned");
        Taken.leave(player, Taken.Way.LEFT);
        h.assertTrue(Taken.shipOf(player) == null && !TakenData.of(player).aboard(), "still aboard after leaving");
        h.succeed();
    }

    /** Pack, armour and off hand are held while aboard and come back to the slots they left, after each way off. */
    static void kitWholeAfterEachWayOut(GameTestHelper h) {
        ServerLevel level = h.getLevel();
        ServerPlayer player = onPlatform(h, 1, "hf-taken-kit");
        player.getInventory().setItem(0, new ItemStack(ModItems.REVOLVER.get()));
        player.getInventory().setItem(5, new ItemStack(ModItems.GOLD_COIN.get(), 3));
        player.setItemSlot(EquipmentSlot.HEAD, new ItemStack(ModItems.COWBOY_HAT.get()));
        player.setItemSlot(EquipmentSlot.OFFHAND, new ItemStack(Items.TORCH, 7));
        Runnable whole = () -> {
            h.assertTrue(player.getInventory().getItem(0).is(ModItems.REVOLVER.get()), "the revolver isn't back in its slot");
            h.assertValueEqual(player.getInventory().getItem(5).getCount(), 3, "coins in slot 5");
            h.assertTrue(player.getItemBySlot(EquipmentSlot.HEAD).is(ModItems.COWBOY_HAT.get()), "the hat isn't back on");
            h.assertValueEqual(player.getItemBySlot(EquipmentSlot.OFFHAND).getCount(), 7, "torches in the off hand");
            h.assertValueEqual(count(player, ModItems.GOLD_COIN.get()), 3, "coins in all");
            h.assertFalse(TakenData.of(player).hasKit(), "kit still held");
            h.assertValueEqual(player.getData(ModAttachments.ABOARD.get()), 0, "ship state told to the client");
        };
        for (Taken.Way way : List.of(Taken.Way.FELL, Taken.Way.LEFT)) {
            Taken.take(level, player);
            h.assertTrue(player.getInventory().isEmpty() && TakenData.of(player).kit().size() == 4, "the kit wasn't taken: " + TakenData.of(player).kit().size());
            Taken.leave(player, way);
            whole.run();
        }
        // Death aboard isn't one: the player is set down alive with everything
        Taken.take(level, player);
        player.connection.markClientLoaded(); // a fake player's client never says it has; until it does nothing can hurt them
        player.hurtServer(level, level.damageSources().genericKill(), Float.MAX_VALUE);
        h.assertTrue(player.isAlive() && Taken.shipOf(player) == null, "died aboard, or is still there: alive " + player.isAlive() + ", health " + player.getHealth() + ", ship " + Taken.shipOf(player));
        whole.run();
        // The pod in the hold hands it back, and leaving after that hands nothing back twice
        Taken.take(level, player);
        TakenData.restore(player);
        Taken.leave(player, Taken.Way.LEFT);
        whole.run();
        // Something picked up aboard and sitting where the revolver goes makes room for it
        Taken.take(level, player);
        player.getInventory().setItem(0, new ItemStack(ModItems.LETTER_HOME.get()));
        TakenData.restore(player);
        h.assertValueEqual(count(player, ModItems.LETTER_HOME.get()), 1, "letters kept when the kit came back over them");
        for (int slot = 0; slot < player.getInventory().getContainerSize(); slot++) {
            if (player.getInventory().getItem(slot).is(ModItems.LETTER_HOME.get())) {
                player.getInventory().setItem(slot, ItemStack.EMPTY);
            }
        }
        Taken.leave(player, Taken.Way.LEFT);
        whole.run();
        // Leaving the game: the kit comes back at once, and the player is set down when they return
        Taken.take(level, player);
        Taken.finish(player);
        whole.run();
        h.assertTrue(TakenData.of(player).aboard(), "forgotten that they're owed a setting down");
        Taken.leave(player, Taken.Way.LEFT);
        h.assertFalse(TakenData.of(player).aboard(), "not set down");
        h.succeed();
    }

    /** Two caught in one beam wake on one ship, in pens of their own; once both are off, the ship is gone. */
    static void twoOnOneShip(GameTestHelper h) {
        ServerLevel level = h.getLevel();
        ServerPlayer one = onPlatform(h, 2, "hf-taken-one"), two = onPlatform(h, 2, "hf-taken-two");
        Ship ship = Taken.take(level, one);
        h.assertTrue(Taken.take(level, two) == ship, "the second woke on another ship");
        h.assertTrue(one.distanceTo(two) > 4.0, "both in one pen");
        BlockPos deck = ship.world(ShipLayout.WAKE).below();
        h.assertFalse(level.getBlockState(deck).isAir(), "no deck under the pen");
        Taken.leave(one, Taken.Way.LEFT);
        h.assertTrue(Taken.ships().contains(ship), "the ship went with somebody still on it");
        Taken.leave(two, Taken.Way.LEFT);
        h.runAfterDelay(3, () -> {
            h.assertFalse(Taken.ships().contains(ship), "the empty ship is still on the books");
            h.assertTrue(level.getBlockState(deck).isAir(), "the empty ship is still there");
            h.succeed();
        });
    }

    /** The crew see a walker at twelve blocks and a sneak at four, in plain view; nobody in the vents; and nothing while stunned. */
    static void whatTheCrewMakeOut(GameTestHelper h) {
        ServerLevel level = h.getLevel();
        ServerPlayer player = onPlatform(h, 3, "hf-taken-sight");
        Ship ship = Taken.raise(level);
        ship.keep(true);
        ship.man(level);
        h.assertTrue(ship.crew().size() == 4 && ship.held().size() == 4 && ship.beast() != null, "crew " + ship.crew().size() + ", held " + ship.held().size());
        GrayWarden warden = ship.crew().get(0);
        // Along the walk in front of the pens: a straight row with nothing in it
        Vec3 post = ShipLayout.feet(ship.origin(), Side.NORTH.deck(-8, 20));
        warden.snapTo(post.x, post.y, post.z, 0, 0);
        move(player, ship, Side.NORTH.deck(2, 20));
        h.assertTrue(ship.sees(warden, player), "a walker ten blocks off wasn't seen");
        move(player, ship, Side.NORTH.deck(6, 20));
        h.assertFalse(ship.sees(warden, player), "a walker fourteen blocks off was seen");
        player.setShiftKeyDown(true);
        player.setPose(net.minecraft.world.entity.Pose.CROUCHING);
        move(player, ship, Side.NORTH.deck(-2, 20));
        h.assertFalse(ship.sees(warden, player), "a sneak six blocks off was seen");
        move(player, ship, Side.NORTH.deck(-5, 20));
        h.assertTrue(ship.sees(warden, player), "a sneak three blocks off wasn't seen");
        player.setShiftKeyDown(false);
        player.setPose(net.minecraft.world.entity.Pose.STANDING);
        // In the duct, a grille away
        Vec3 ring = ShipLayout.feet(ship.origin(), Side.NORTH.deck(0, 13));
        warden.snapTo(ring.x, ring.y, ring.z, 0, 0);
        level.setBlockAndUpdate(ship.world(ShipLayout.GRILLES[0]), Blocks.AIR.defaultBlockState());
        level.setBlockAndUpdate(ship.world(ShipLayout.GRILLES[0]).above(), Blocks.AIR.defaultBlockState());
        move(player, ship, Side.NORTH.deck(0, 10));
        h.assertFalse(ship.sees(warden, player), "seen in the vents");
        // The rod: stunned, it makes out nothing, however near
        warden.snapTo(post.x, post.y, post.z, 0, 0);
        move(player, ship, Side.NORTH.deck(-6, 20));
        h.assertTrue(ship.sees(warden, player), "not seen two blocks off");
        h.assertTrue(ShockRodItem.shock(level, player, warden) && warden.isStunned(), "the rod didn't stun a Warden");
        h.assertFalse(ship.sees(warden, player), "a stunned Warden still sees");
        TestPlayers.leave(player);
        ship.keep(false);
        h.succeed();
    }

    /** The alarm shuts the doors; the fall opens them again and doesn't stop. */
    static void alarmAndTheFall(GameTestHelper h) {
        ServerLevel level = h.getLevel();
        onPlatform(h, 4, "hf-taken-alarm");
        Ship ship = Taken.raise(level);
        ship.keep(true);
        ship.man(level);
        BlockPos door = ship.world(ShipLayout.door(Side.NORTH, ShipLayout.ROOM_DOOR_V).get(0));
        h.assertTrue(level.getBlockState(door).isAir(), "the pens' door is shut before any alarm");
        ship.alarm(level, List.of());
        h.runAfterDelay(12, () -> {
            h.assertTrue(ship.stage() == Ship.Stage.ALARM, "no alarm: " + ship.stage());
            h.assertTrue(level.getBlockState(door).equals(ModBlocks.GRAY_FIELD.get().defaultBlockState()), "the pens' door under alarm" + " is " + level.getBlockState(door));
            ship.startFall(level);
            int left = ship.fallLeft();
            h.runAfterDelay(12, () -> {
                h.assertTrue(ship.falling() && ship.stage() == Ship.Stage.FALLING && ship.fallLeft() < left, "not falling");
                h.assertTrue(level.getBlockState(door).isAir(), "the pens' door is still shut with the ship going down");
                ship.keep(false);
                h.succeed();
            });
        });
    }

    /** Aboard, a grille comes away in the hands and the hull doesn't; nothing is placed. */
    static void theHullHolds(GameTestHelper h) {
        ServerLevel level = h.getLevel();
        ServerPlayer player = onPlatform(h, 5, "hf-taken-hull");
        Ship ship = Taken.take(level, player);
        BlockPos wall = ship.world(Side.NORTH.at(0, 27, ShipLayout.DECK)), grille = ship.world(ShipLayout.GRILLES[0]);
        h.assertTrue(level.getBlockState(wall).equals(ModBlocks.SAUCER_HULL.get().defaultBlockState()), "the hull behind the pen" + " is " + level.getBlockState(wall));
        player.gameMode.destroyBlock(wall);
        h.assertTrue(level.getBlockState(wall).equals(ModBlocks.SAUCER_HULL.get().defaultBlockState()), "the hull after a go at it" + " is " + level.getBlockState(wall));
        h.assertTrue(level.getBlockState(grille).equals(ShipBlueprints.grille()), "the grille off the ring" + " is " + level.getBlockState(grille));
        player.gameMode.destroyBlock(grille);
        h.assertTrue(level.getBlockState(grille).isAir(), "the grille didn't come away");
        // No law up here: nothing done aboard earns a wanted star
        com.hollowfrontier.content.frontier.law.Wanted.addHeat(player, com.hollowfrontier.content.frontier.law.Wanted.MAX_HEAT);
        h.assertValueEqual(com.hollowfrontier.content.frontier.law.Wanted.stars(player), 0, "wanted stars earned aboard");
        Taken.leave(player, Taken.Way.LEFT);
        com.hollowfrontier.content.frontier.law.Wanted.addHeat(player, com.hollowfrontier.content.frontier.law.Wanted.MAX_HEAT);
        h.assertTrue(com.hollowfrontier.content.frontier.law.Wanted.stars(player) > 0, "the law still counts on the ground");
        h.succeed();
    }

    /** All three cells pulled: the ship is falling, and a pod taken then earns both. The kit comes down with its owner. */
    static void cellsBringItDown(GameTestHelper h) {
        ServerLevel level = h.getLevel();
        ServerPlayer player = onPlatform(h, 6, "hf-taken-down");
        BlockPos home = player.blockPosition();
        player.getInventory().setItem(3, new ItemStack(ModItems.GOLD_COIN.get(), 5));
        Ship ship = Taken.take(level, player);
        for (BlockPos socket : ShipLayout.SOCKETS) {
            BlockPos at = ship.world(socket);
            h.assertFalse(ship.falling(), "falling with a cell still seated");
            CoreSocketBlock.pull(level, player, at, level.getBlockState(at));
        }
        h.assertTrue(ship.falling() && ship.pulled() == Ship.CELLS, "three cells out and not falling");
        h.assertValueEqual(count(player, ModItems.PHLOGISTON_CELL.get()), 3, "cells in hand");
        move(player, ship, Side.WEST.deck(ShipLayout.PODS[0], 24));
        BlockPos console = ship.world(ShipLayout.podConsole(0));
        ship.console(level, player, console, ShipConsoleBlock.Kind.POD);
        h.runAfterDelay(Ship.LAUNCH_DELAY + 4, () -> {
            h.assertTrue(Taken.shipOf(player) == null && !TakenData.of(player).aboard(), "still aboard after the pod went");
            h.assertTrue(player.blockPosition().closerThan(home, 8), "set down far from the platform: " + player.blockPosition());
            h.assertValueEqual(count(player, ModItems.GOLD_COIN.get()), 5, "coins back");
            h.assertTrue(AdvancementTests.earned(player, "ufo/checked_out_early"), "Checked Out Early not earned");
            h.assertTrue(AdvancementTests.earned(player, "ufo/what_goes_up"), "What Goes Up not earned");
            boolean wreck = BlockPos.betweenClosedStream(home.offset(-4, -1, -4), home.offset(4, 3, 4)).anyMatch(p -> level.getBlockState(p).is(ModBlocks.SAUCER_WRECKAGE.get()));
            h.assertTrue(wreck, "no pod wreck where they came down");
            h.succeed();
        });
    }

    /** A quiet way out with everyone: the held freed, a letter from each, all of them down with the pod, and no ship brought down. */
    static void nobodyLeftBehind(GameTestHelper h) {
        ServerLevel level = h.getLevel();
        ServerPlayer player = onPlatform(h, 7, "hf-taken-all");
        BlockPos home = player.blockPosition();
        Ship ship = Taken.take(level, player);
        h.runAfterDelay(3, () -> {
            List<Mob> held = List.copyOf(ship.held());
            h.assertValueEqual(held.size(), 4, "held aboard");
            Vec3 bay = ShipLayout.feet(ship.origin(), Side.WEST.deck(0, 20));
            for (Mob mob : held) {
                h.assertTrue(Captives.free(level, player, mob).equals("freed"), "couldn't free " + mob.getName().getString());
                mob.snapTo(bay.x, bay.y, bay.z, 0, 0);
            }
            h.assertValueEqual(count(player, ModItems.LETTER_HOME.get()), 4, "letters home");
            move(player, ship, Side.WEST.deck(ShipLayout.PODS[1], 24));
            h.runAfterDelay(25, () -> {
                h.assertTrue(ship.nobodyLeft() && ship.freed().size() == 4, "freed " + ship.freed().size() + ", still held " + ship.held().size());
                h.assertValueEqual(count(player, Items.BREAD), 4 * Ship.BREAD_EACH, "bread handed over by the freed");
                move(player, ship, Side.WEST.deck(ShipLayout.PODS[1], 24));
                ship.console(level, player, ship.world(ShipLayout.podConsole(1)), ShipConsoleBlock.Kind.POD);
                h.runAfterDelay(Ship.LAUNCH_DELAY + 4, () -> {
                    h.assertTrue(Taken.shipOf(player) == null, "still aboard");
                    h.assertTrue(AdvancementTests.earned(player, "ufo/nobody_left_behind"), "Nobody Left Behind not earned");
                    h.assertFalse(AdvancementTests.earned(player, "ufo/what_goes_up"), "What Goes Up earned on a ship left flying");
                    for (Mob mob : held) {
                        h.assertTrue(mob.isAlive() && mob.blockPosition().closerThan(home, 12), mob.getName().getString() + " didn't come down with the pod: alive " + mob.isAlive() + " at " + mob.blockPosition() + ", home " + home);
                        mob.discard();
                    }
                    h.succeed();
                });
            });
        });
    }

    /** A wrecked ship is raised on level ground, with its Wardens. */
    static void crashSiteRaised(GameTestHelper h) {
        ServerLevel level = h.getLevel();
        Taken.standIn(Level.OVERWORLD, 0);
        level.getServer().setDifficulty(Difficulty.NORMAL, true);
        BlockPos corner = new BlockPos(24000, 100, 20000);
        BlockState stone = Blocks.STONE.defaultBlockState();
        for (BlockPos at : BlockPos.betweenClosed(corner.offset(-2, 0, -2), corner.offset(26, 0, 50))) {
            level.setBlock(at, stone, 2);
        }
        Optional<BlockPos> site = Taken.raiseCrashSite(level, corner.above(), level.getRandom());
        h.assertTrue(site.isPresent(), "no crash site on level ground");
        boolean hull = BlockPos.betweenClosedStream(site.get().offset(-8, -2, -8), site.get().offset(8, 6, 8)).anyMatch(p -> level.getBlockState(p).is(ModBlocks.SAUCER_HULL.get()));
        h.assertTrue(hull, "no saucer at the site");
        List<GrayWarden> wardens = level.getEntitiesOfClass(GrayWarden.class, new net.minecraft.world.phys.AABB(site.get()).inflate(48));
        h.assertValueEqual(wardens.size(), 2, "Wardens keeping the wreck");
        wardens.forEach(GrayWarden::discard);
        // Over open air there's nowhere for one
        h.assertTrue(Taken.raiseCrashSite(level, corner.offset(3000, 1, 0), level.getRandom()).isEmpty() || level.getHeight(net.minecraft.world.level.levelgen.Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, corner.getX() + 3000, corner.getZ()) > level.getMinY() + 1,
                "a crash site raised on nothing");
        h.succeed();
    }
}
