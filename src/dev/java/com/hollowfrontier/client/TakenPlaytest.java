package com.hollowfrontier.client;

import java.util.ArrayList;
import java.util.List;

import com.hollowfrontier.HollowFrontier;
import com.hollowfrontier.content.frontier.mystery.Suspicion;
import com.hollowfrontier.content.hollow.gray.Captives;
import com.hollowfrontier.content.ufo.GrayWarden;
import com.hollowfrontier.content.ufo.UfoConfig;
import com.hollowfrontier.content.ufo.taken.Ship;
import com.hollowfrontier.content.ufo.taken.ShipLayout;
import com.hollowfrontier.content.ufo.taken.ShipLayout.Side;
import com.hollowfrontier.content.ufo.taken.Taken;
import com.hollowfrontier.content.ufo.taken.TakenData;
import com.hollowfrontier.registry.ModAttachments;
import com.hollowfrontier.registry.ModBlocks;
import com.hollowfrontier.registry.ModItems;
import com.hollowfrontier.worldgen.Aboard;

import net.minecraft.advancements.AdvancementHolder;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;

/**
 * Phase 50 played through (`runPhoto -PphotoScene=taken`): a survival player the men in black mind is kept, wakes in the
 * pen in the ship's own world, and goes through all of it by the game's own entry points (blocks used, blocks broken,
 * captives used): waits out the surgeon, opens the pens, frees the held, gets their things back in the hold, lets the
 * beast out, takes the rod off the tray and shorts the rig with it, takes a grille off and stands in the vents with the
 * Warden walking past, pulls the core's three cells, and leaves in a pod with everyone as the ship goes down. Then a
 * second, short stay: taken again, and killed aboard. Each step logs PASS or FAIL; pictures are {@code hf_taken_*}.
 * <p>
 * What it does for the player: the walking (they're moved from room to room) and the waiting.
 */
final class TakenPlaytest {
    private TakenPlaytest() {}

    static final boolean ENABLED = "taken".equals(System.getProperty("hollowfrontier.photo.scene"));

    private static int tick, passed, failed, step, wait, patience;
    private static Ship ship;
    private static BlockPos home;
    private static final List<Mob> walked = new ArrayList<>();
    private static GrayWarden brawled;
    private static float brawlHealth;

    private static void check(boolean ok, String what) {
        if (ok) {
            passed++;
        } else {
            failed++;
        }
        HollowFrontier.LOGGER.info("[Taken] {} {}", ok ? "PASS" : "FAIL", what);
    }

    private static void info(String what) {
        HollowFrontier.LOGGER.info("[Taken] {}", what);
    }

    /** How long the server's ticks are taking, for the log: a ship shouldn't cost anything anyone would notice. */
    private static String pace(MinecraftServer server) {
        return String.format("server tick %.1f ms", server.getAverageTickTimeNanos() / 1.0e6);
    }

    private static void put(ServerPlayer player, BlockPos local, float yaw, float pitch) {
        Vec3 at = ShipLayout.feet(ship.origin(), local);
        player.teleportTo((ServerLevel) player.level(), at.x, at.y, at.z, java.util.Set.of(), yaw, pitch, true);
    }

    private static void use(ServerPlayer player, BlockPos local) {
        BlockPos pos = ship.world(local);
        player.gameMode.useItemOn(player, player.level(), player.getMainHandItem(), InteractionHand.MAIN_HAND,
                new BlockHitResult(Vec3.atCenterOf(pos), Direction.UP, pos, false));
    }

    private static boolean earned(ServerPlayer player, String name) {
        AdvancementHolder advancement = player.level().getServer().getAdvancements().get(HollowFrontier.id(name));
        return advancement != null && player.getAdvancements().getOrStartProgress(advancement).isDone();
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

    /** Next step, after this many ticks. */
    private static void next(int after) {
        step++;
        wait = after;
    }

    private static String pendingShot;

    /**
     * One step. The client's tick drives it, but everything it does is the server's business, so it's run on the server's
     * own thread (two threads moving a player between worlds at once is a crash); a picture it asks for is taken back here.
     */
    static void tick(Minecraft mc, MinecraftServer server) {
        pendingShot = null;
        server.submit(() -> step(mc, server)).join();
        if (pendingShot != null) {
            PhotoBooth.shot(mc, pendingShot);
        }
    }

    private static void shot(String name) {
        pendingShot = name;
    }

    private static void step(Minecraft mc, MinecraftServer server) {
        ServerPlayer player = server.getPlayerList().getPlayers().getFirst();
        tick++;
        if (wait > 0) {
            wait--;
            return;
        }
        ServerLevel level = (ServerLevel) player.level();
        switch (step) {
            case 0 -> {
                server.setDifficulty(net.minecraft.world.Difficulty.NORMAL, true);
                player.setGameMode(net.minecraft.world.level.GameType.SURVIVAL);
                player.getInventory().clearContent();
                player.removeAllEffects();
                player.setHealth(player.getMaxHealth());
                com.hollowfrontier.content.ufo.Leash.set(server.overworld(), false);
                player.setData(ModAttachments.TAKEN.get(), TakenData.NONE);
                player.getInventory().setItem(0, new ItemStack(ModItems.REVOLVER.get()));
                player.getInventory().setItem(1, new ItemStack(ModItems.GOLD_COIN.get(), 12));
                player.setItemSlot(EquipmentSlot.HEAD, new ItemStack(ModItems.COWBOY_HAT.get()));
                check(server.getLevel(Aboard.LEVEL) != null, "the ship's world exists in this (older) save");
                check(level.dimension() == Level.OVERWORLD, "starting on the ground: " + level.dimension().identifier());
                check(!Taken.keeps(level, player) || Suspicion.level(player) >= UfoConfig.takenSuspicion(), "nobody the men in black don't mind is kept");
                player.setData(ModAttachments.SUSPICION.get(), new Suspicion(UfoConfig.takenSuspicion(), level.getGameTime(), 0));
                check(Taken.keeps(level, player), "kept at suspicion " + Suspicion.level(player));
                home = player.blockPosition();
                ship = Taken.take(level, player);
                check(ship != null, "taken");
                next(ship == null ? 0 : 20);
                if (ship == null) {
                    step = 99;
                }
            }
            case 1 -> {
                check(player.level().dimension() == Aboard.LEVEL, "woke in the ship's world: " + player.level().dimension().identifier());
                check(player.getInventory().isEmpty() && TakenData.of(player).kit().size() == 3, "nothing on them; kit of " + TakenData.of(player).kit().size() + " held");
                check(ship.crew().size() == 4 && ship.held().size() == 4 && ship.beast() != null, "crew " + ship.crew().size() + ", held " + ship.held().size() + ", beast " + ship.beast());
                check(level.getBlockState(ship.world(ShipLayout.penField(ShipLayout.PLAYER_PEN).get(0))).is(ModBlocks.GRAY_FIELD.get()), "the pen's field is up");
                check(player.getFoodData().getFoodLevel() == 20 && player.getHealth() == player.getMaxHealth(), "woke fed and whole");
                next(130);
            }
            case 2 -> {
                GrayWarden coming = ship.crew().get(0);
                check(coming.isCurrentlyGlowing(), "the one coming for the pen shows through the walls");
                shot("hf_taken_01_pen.png");
                info("waiting for the surgeon (it's sent at tick " + Ship.CALL_AT + " of the ship's age; now " + ship.age() + ")");
                next(1);
            }
            case 3 -> {
                if (ship.penOpen()) {
                    check(ship.age() < Ship.PEN_OPENS_BY, "the surgeon reached the pen and dropped the field at age " + ship.age() + " (the fallback is " + Ship.PEN_OPENS_BY + ")");
                    GrayWarden caller = ship.crew().get(0);
                    check(!ship.sees(caller, player), "the caller is busy at the wall and doesn't see the pen's occupant");
                    next(15);
                }
            }
            case 4 -> {
                shot("hf_taken_02_pen_open.png");
                put(player, Side.NORTH.deck(-6, 20), -60, 5);
                next(10);
            }
            case 5 -> {
                shot("hf_taken_03_pens_walk.png");
                use(player, ShipLayout.PENS_CONSOLE);
                boolean open = level.getBlockState(ship.world(ShipLayout.penField(0).get(0))).isAir();
                check(open, "the pens' console drops every field");
                int letters = 0;
                for (Mob mob : List.copyOf(ship.held())) {
                    if (!Captives.onTable(mob)) {
                        player.interactOn(mob, InteractionHand.MAIN_HAND, mob.position());
                        letters++;
                        walked.add(mob);
                    }
                }
                check(count(player, ModItems.LETTER_HOME.get()) == letters && letters == 3, "three in the pens freed by using them, a letter from each: " + count(player, ModItems.LETTER_HOME.get()));
                next(30);
            }
            case 6 -> {
                check(ship.freed().size() == 3, "the ship knows three are loose: " + ship.freed().size());
                check(count(player, net.minecraft.world.item.Items.BREAD) == 3 * Ship.BREAD_EACH, "each of the freed handed over bread: " + count(player, net.minecraft.world.item.Items.BREAD));
                put(player, Side.EAST.deck(0, 20), -90, 8);
                next(12);
            }
            case 7 -> {
                shot("hf_taken_04_hold.png");
                use(player, ShipLayout.KIT_POD);
                check(player.getInventory().getItem(0).is(ModItems.REVOLVER.get()) && count(player, ModItems.GOLD_COIN.get()) == 12
                        && player.getItemBySlot(EquipmentSlot.HEAD).is(ModItems.COWBOY_HAT.get()) && !TakenData.of(player).hasKit(), "the pod in the hold hands the kit back, each thing where it was");
                use(player, ShipLayout.HOLD_CONSOLE);
                check(level.getBlockState(ship.world(ShipLayout.beastField().get(0))).isAir(), "the hold's console lets the beast out");
                next(60);
            }
            case 8 -> {
                Mob beast = ship.beast();
                check(beast != null && beast.isAlive() && beast.getTarget() instanceof GrayWarden, "the beast goes for the crew: " + (beast == null ? null : beast.getTarget()));
                check(beast == null || !(beast.getTarget() instanceof ServerPlayer), "and not for the taken");
                put(player, Side.SOUTH.deck(6, 20), 35, 5);
                next(15);
            }
            case 9 -> {
                shot("hf_taken_05_surgery.png");
                use(player, ShipLayout.TRAY_POD);
                check(count(player, ModItems.SHOCK_ROD.get()) == 1 && count(player, ModItems.PHLOGISTON_CELL.get()) == 1, "the tray gives the rod and a cell");
                for (int slot = 0; slot < player.getInventory().getContainerSize(); slot++) {
                    if (player.getInventory().getItem(slot).is(ModItems.SHOCK_ROD.get())) {
                        ItemStack rod = player.getInventory().removeItemNoUpdate(slot);
                        ItemStack was = player.getMainHandItem().copy();
                        player.setItemInHand(InteractionHand.MAIN_HAND, rod);
                        player.getInventory().setItem(slot, was);
                        break;
                    }
                }
                Mob patient = ship.held().stream().filter(Captives::onTable).findFirst().orElse(null);
                check(patient != null, "somebody is on the table");
                GrayWarden surgeon = ship.crew().stream().filter(GrayWarden::isUnarmed).findFirst().orElse(null);
                if (surgeon != null) {
                    player.attack(surgeon);
                    check(surgeon.isStunned(), "a blow of the rod stuns a surgeon");
                }
                use(player, ShipLayout.RIG);
                check(level.getBlockState(ship.world(ShipLayout.RIG)).isAir(), "the rod shorts the rig");
                if (patient != null) {
                    player.interactOn(patient, InteractionHand.MAIN_HAND, patient.position());
                    walked.add(patient);
                }
                check(count(player, ModItems.LETTER_HOME.get()) == 4, "the one off the table is freed too: letters " + count(player, ModItems.LETTER_HOME.get()));
                next(5);
            }
            case 10 -> {
                // The vents: a grille comes away, and nobody looks in
                put(player, Side.NORTH.deck(0, 13), 0, 10);
                BlockPos grille = ship.world(ShipLayout.GRILLES[0]);
                player.gameMode.destroyBlock(grille);
                player.gameMode.destroyBlock(grille.above());
                check(level.getBlockState(grille).isAir() && level.getBlockState(grille.above()).isAir(), "a grille comes away in the hands");
                BlockPos hull = ship.world(Side.NORTH.at(0, 27, ShipLayout.DECK));
                put(player, Side.NORTH.deck(0, 25), 180, 0);
                player.gameMode.destroyBlock(hull);
                check(level.getBlockState(hull).is(ModBlocks.SAUCER_HULL.get()), "the hull doesn't");
                put(player, Side.NORTH.deck(0, 10), 90, 0);
                next(10);
            }
            case 11 -> {
                shot("hf_taken_06_vent.png");
                GrayWarden walker = ship.crew().stream().filter(w -> !w.isUnarmed() && w.hasRound()).findFirst().orElse(null);
                if (walker != null) {
                    Vec3 by = ShipLayout.feet(ship.origin(), Side.NORTH.deck(0, 13));
                    walker.snapTo(by.x, by.y, by.z, 180, 0);
                    check(!ship.sees(walker, player), "the Warden three blocks off doesn't make out somebody in the vent");
                }
                next(40);
            }
            case 12 -> {
                check(ship.crew().stream().noneMatch(w -> w.getTarget() == player), "nobody has come after them in there");
                player.setShiftKeyDown(true);
                player.setPose(net.minecraft.world.entity.Pose.CROUCHING);
                next(15);
            }
            case 13 -> {
                check(ship.crew().stream().anyMatch(w -> w.isCurrentlyGlowing()), "keeping low, the crew near by show through the walls");
                shot("hf_taken_06b_listening.png");
                player.setShiftKeyDown(false);
                player.setPose(net.minecraft.world.entity.Pose.STANDING);
                // A Gray made to go for the player in the middle of the freed: they pitch in
                GrayWarden walker = ship.crew().stream().filter(w -> !w.isUnarmed() && w.hasRound()).findFirst().orElse(null);
                if (walker != null && !walked.isEmpty()) {
                    Vec3 by = walked.get(0).position();
                    walker.snapTo(by.x + 2, by.y, by.z, 0, 0);
                    put(player, ship.local(walked.get(0).blockPosition()).offset(-3, 0, 0), 90, 0);
                    walker.hurtServer((ServerLevel) player.level(), player.damageSources().playerAttack(player), 1.0F);
                    brawled = walker;
                    brawlHealth = walker.getHealth();
                }
                next(70);
            }
            case 14 -> {
                check(brawled == null || !brawled.isAlive() || brawled.getHealth() < brawlHealth - 2.5F, "the freed pitched in against a Gray that came after the player: its health " + (brawled == null ? "?" : brawlHealth + " to " + brawled.getHealth()));
                put(player, new BlockPos(ShipLayout.C, ShipLayout.DECK, ShipLayout.C + 5), 180, -10);
                next(12);
            }
            case 15 -> {
                shot("hf_taken_07_engine.png");
                for (BlockPos socket : ShipLayout.SOCKETS) {
                    use(player, socket);
                }
                check(ship.falling() && count(player, ModItems.PHLOGISTON_CELL.get()) >= 3, "three cells pulled by hand: the ship is falling, cells in hand " + count(player, ModItems.PHLOGISTON_CELL.get()));
                check(player.getData(ModAttachments.ABOARD.get()) != 0, "the client has been told it's aboard");
                next(100);
            }
            case 16 -> {
                shot("hf_taken_08_falling_core.png");
                info("camera shake " + CameraFeel.shaking() + " with " + ship.fallLeft() + " ticks left; " + pace(server));
                check((player.getData(ModAttachments.ABOARD.get()) & ~Taken.VENT_NOTED) == Ship.Stage.FALLING.code, "the client knows it's falling");
                put(player, Side.WEST.deck(0, 15), 90, 0);
                next(300);
            }
            case 17 -> {
                shot("hf_taken_09_falling_ring.png");
                check(level.getBlockState(ship.world(ShipLayout.door(Side.WEST, ShipLayout.ROOM_DOOR_V).get(0))).isAir(), "the doors stand open while it falls");
                info("crew left aboard: " + ship.crew().size() + " (they run for the bay and go); " + pace(server));
                // Everybody into the pod
                Vec3 bay = ShipLayout.feet(ship.origin(), Side.WEST.deck(ShipLayout.PODS[0], 21));
                for (Mob mob : walked) {
                    info(mob.getName().getString() + " is " + String.format("%.0f", Math.sqrt(mob.distanceToSqr(player))) + " blocks from the player after following");
                    mob.snapTo(bay.x, bay.y, bay.z, 0, 0);
                }
                put(player, Side.WEST.deck(ShipLayout.PODS[0], 24), 270, 0);
                next(25);
            }
            case 18 -> {
                shot("hf_taken_10_pod.png");
                check(ship.nobodyLeft(), "nobody is left held");
                use(player, ShipLayout.podConsole(0));
                patience = 400;
                next(Ship.LAUNCH_DELAY);
            }
            case 19 -> {
                if (Taken.shipOf(player) != null && patience-- > 0) {
                    return; // the server keeps its own time: wait for the pod, not for a count of the client's ticks
                }
                info("the pod is down; " + pace(server));
                next(30);
            }
            case 20 -> {
                check(player.level().dimension() == Level.OVERWORLD && Taken.shipOf(player) == null && !TakenData.of(player).aboard(), "down, in the Overworld: " + player.level().dimension().identifier());
                double away = Math.sqrt(player.blockPosition().distSqr(home));
                check(away > 250 && away < 760, "set down " + (int) away + " blocks from where they were taken");
                check(earned(player, "ufo/checked_out_early") && earned(player, "ufo/what_goes_up") && earned(player, "ufo/nobody_left_behind"), "Checked Out Early, What Goes Up and Nobody Left Behind earned");
                check(count(player, ModItems.GOLD_COIN.get()) == 12 && count(player, ModItems.REVOLVER.get()) == 1, "kit still whole");
                check(walked.stream().allMatch(m -> m.isAlive() || m.isRemoved()), "the freed came with the pod");
                check(Taken.ships().isEmpty(), "the ship is gone");
                player.removeAllEffects();
                ServerLevel ground = (ServerLevel) player.level();
                int x = player.getBlockX() + 7, z = player.getBlockZ() + 7;
                int y = ground.getHeight(net.minecraft.world.level.levelgen.Heightmap.Types.MOTION_BLOCKING, x, z);
                server.getCommands().performPrefixedCommand(server.createCommandSourceStack().withSuppressedOutput(), "time set 23200"); // first light, so the wreck can be seen
                player.teleportTo(ground, x + 0.5, y + 1, z + 0.5, java.util.Set.of(), 135, 20, true);
                next(40);
            }
            case 21 -> {
                shot("hf_taken_11_landed.png");
                // A second, short stay: kept again, and killed aboard
                player.setData(ModAttachments.SUSPICION.get(), new Suspicion(UfoConfig.takenSuspicion(), level.getGameTime(), 0));
                ship = Taken.take((ServerLevel) player.level(), player);
                next(80);
            }
            case 22 -> {
                check(ship != null && player.getInventory().isEmpty() && TakenData.of(player).times() == 2, "taken a second time, kit held again");
                player.connection.markClientLoaded();
                player.hurtServer((ServerLevel) player.level(), player.level().damageSources().genericKill(), Float.MAX_VALUE);
                next(40);
            }
            case 23 -> {
                check(player.isAlive() && player.level().dimension() == Level.OVERWORLD && Taken.shipOf(player) == null, "killed aboard: alive, on the ground");
                check(count(player, ModItems.GOLD_COIN.get()) == 12 && count(player, ModItems.REVOLVER.get()) == 1 && !TakenData.of(player).hasKit(), "and with everything");
                HollowFrontier.LOGGER.info("[Taken] done: {} passed, {} failed", passed, failed);
                next(20);
            }
            default -> PhotoBooth.quit(mc, server);
        }
    }
}
