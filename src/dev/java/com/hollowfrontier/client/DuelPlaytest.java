package com.hollowfrontier.client;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import com.hollowfrontier.HollowFrontier;
import com.hollowfrontier.content.frontier.bounty.BoardPosting;
import com.hollowfrontier.content.frontier.bounty.BountyBoards;
import com.hollowfrontier.content.frontier.bounty.BountyData;
import com.hollowfrontier.content.frontier.bounty.Purse;
import com.hollowfrontier.content.frontier.duel.DuelData;
import com.hollowfrontier.content.frontier.duel.Duels;
import com.hollowfrontier.content.frontier.gun.GunItem;
import com.hollowfrontier.content.frontier.outlaw.OutlawBoss;
import com.hollowfrontier.content.frontier.raid.TownSite;
import com.hollowfrontier.content.frontier.stake.Standing;
import com.hollowfrontier.content.frontier.town.Towns;
import com.hollowfrontier.content.frontier.townsfolk.Lawman;
import com.hollowfrontier.content.frontier.townsfolk.Townsfolk;
import com.hollowfrontier.network.DuelAnswerPayload;
import com.hollowfrontier.registry.ModAttachments;
import com.hollowfrontier.registry.ModDataComponents;
import com.hollowfrontier.registry.ModItems;
import com.hollowfrontier.worldgen.structure.ModStructures;

import net.minecraft.client.CameraType;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.structure.StructureStart;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientChatReceivedEvent;
import net.neoforged.neoforge.client.network.ClientPacketDistributor;

/**
 * High noon (51) played through (`runPhoto -PphotoScene=duel`): a survival player near a real, generated town at noon,
 * asked by the game's own rule, answering through the client's own payload, standing the count, and firing with the
 * game's own use of the gun. It looks at up to six towns for one whose street will do (and says how many would), then
 * wins one duel with its own revolver, loses one with the town's, refuses one and lets one go unanswered. It logs
 * `[Playtest] PASS / FAIL / NOTE` lines and what the game said (`heard`), with pictures as `hf_duel_*`. The aiming is done
 * for the player; whether a real hand can beat him is what it can't tell.
 */
@EventBusSubscriber(modid = HollowFrontier.MODID, value = Dist.CLIENT)
public final class DuelPlaytest {
    private DuelPlaytest() {}

    static final boolean ENABLED = "duel".equals(System.getProperty("hollowfrontier.photo.scene"));
    private static final int TRIES = 3;

    private static int step;
    private static int wait;
    private static boolean busy;
    private static boolean done;
    private static volatile String shot;
    private static volatile int shotIn;
    private static int pass, fail;

    private static ServerLevel level;
    private static ServerPlayer player;
    private static int baseX, baseZ;
    private static int townsSeen, townsWithStreet;
    private static BlockPos looking = BlockPos.ZERO;
    private static long town = Long.MIN_VALUE;
    private static Duels.Ground ground;
    private static BountyData bounty;
    private static Duels.Duel duel;
    private static ItemStack revolver = ItemStack.EMPTY;

    @SubscribeEvent
    static void heard(ClientChatReceivedEvent.System event) {
        if (ENABLED) {
            HollowFrontier.LOGGER.info("[Playtest] heard{}: {}", event.isOverlay() ? " (bar)" : "", event.getMessage().getString());
        }
    }

    static void tick(Minecraft mc, MinecraftServer server) {
        if (shot != null && --shotIn <= 0) {
            PhotoBooth.shot(mc, shot);
            shot = null;
        }
        if (done || busy) {
            return;
        }
        busy = true;
        server.execute(() -> {
            try {
                level = server.overworld();
                player = server.getPlayerList().getPlayers().getFirst();
                if (shot != null || wait-- > 0) {
                    return;
                }
                wait = 0;
                advance(mc, server);
            } catch (Throwable e) {
                HollowFrontier.LOGGER.error("[Playtest] FAIL step {} threw", step, e);
                fail++;
                step = 9000;
            } finally {
                busy = false;
            }
        });
    }

    private static void check(boolean ok, String what) {
        if (ok) {
            pass++;
        } else {
            fail++;
        }
        HollowFrontier.LOGGER.info("[Playtest] {} {}", ok ? "PASS" : "FAIL", what);
    }

    private static void note(String what) {
        HollowFrontier.LOGGER.info("[Playtest] NOTE {}", what);
    }

    private static void cmd(String command) {
        level.getServer().getCommands().performPrefixedCommand(level.getServer().createCommandSourceStack().withSuppressedOutput(), command);
    }

    private static void go(int next, int after) {
        step = next;
        wait = after;
    }

    private static void picture(String name, int in) {
        shotIn = in;
        shot = name;
    }

    private static BlockPos surface(double x, double z) {
        return level.getHeightmapPos(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, BlockPos.containing(x, 0, z));
    }

    private static void turnTo(Vec3 at) {
        Vec3 eye = player.getEyePosition();
        Vec3 v = at.subtract(eye);
        float yaw = (float) (Math.toDegrees(Math.atan2(v.z, v.x)) - 90.0);
        float pitch = (float) -Math.toDegrees(Math.atan2(v.y, Math.sqrt(v.x * v.x + v.z * v.z)));
        player.teleportTo(level, player.getX(), player.getY(), player.getZ(), Set.of(), yaw, pitch, false);
    }

    private static BountyData caller(int reward) {
        while (true) {
            BountyData made = new BountyData(UUID.randomUUID(), "Black Tom Varney", player.getBlockX() + 300, player.getBlockZ() + 300, reward, false);
            if (Duels.callsOut(made)) {
                return made;
            }
        }
    }

    // The poster in the pack and on the town's board, as taking it from the board leaves things
    private static void takePoster(int reward) {
        bounty = caller(reward);
        Inventory inventory = player.getInventory();
        for (int slot = 0; slot < inventory.getContainerSize(); slot++) {
            if (inventory.getItem(slot).is(ModItems.WANTED_POSTER.get())) {
                inventory.setItem(slot, ItemStack.EMPTY);
            }
        }
        ItemStack poster = new ItemStack(ModItems.WANTED_POSTER.get());
        poster.set(ModDataComponents.BOUNTY.get(), bounty);
        inventory.setItem(8, poster);
        BountyBoards boards = BountyBoards.get(level);
        String key = "town:" + town;
        List<BoardPosting> postings = new ArrayList<>(boards.board(level, key, BlockPos.containing(ground.player())).postings());
        postings.add(BoardPosting.open(bounty, false, BountyBoards.day(level)).taken(player.getGameProfile().name()));
        boards.put(key, postings, BountyBoards.day(level));
    }

    private static BountyData posterNow() {
        return player.getInventory().getItem(8).get(ModDataComponents.BOUNTY.get());
    }

    private static boolean anyLent() {
        Inventory inventory = player.getInventory();
        for (int slot = 0; slot < inventory.getContainerSize(); slot++) {
            if (Duels.lent(inventory.getItem(slot))) {
                return true;
            }
        }
        return false;
    }

    private static int inTheLane() {
        Vec3 a = ground.player(), b = ground.boss();
        Vec3 unit = b.subtract(a).normalize();
        Vec3 aside = new Vec3(-unit.z, 0, unit.x);
        double length = a.distanceTo(b);
        return level.getEntitiesOfClass(Mob.class, new AABB(BlockPos.containing(ground.centre())).inflate(24, 6, 24), m -> {
            if (!(m instanceof Townsfolk) && !(m instanceof Lawman)) {
                return false;
            }
            Vec3 rel = m.position().subtract(a);
            double forward = rel.dot(unit);
            return forward > 0.5 && forward < length - 0.5 && Math.abs(rel.dot(aside)) < 1.5;
        }).size();
    }

    private static void advance(Minecraft mc, MinecraftServer server) {
        switch (step) {
            case 0 -> {
                if (!player.isAlive()) { // left dead by an earlier run
                    mc.execute(() -> mc.player.respawn());
                    go(0, 60);
                    return;
                }
                baseX = player.getBlockX();
                baseZ = player.getBlockZ();
                cmd("gamemode spectator @p");
                cmd("difficulty normal");
                cmd("weather clear");
                cmd("gamerule advance_weather false");
                cmd("gamerule advance_time false");
                cmd("time set 3000");
                cmd("clear @p");
                cmd("effect give @p minecraft:saturation infinite 0 true");
                for (var type : List.of(ModAttachments.STANDING, ModAttachments.DUELS)) {
                    player.removeData(type.get());
                }
                note("playtest begins; the player is " + player.getGameProfile().name());
                go(1, 5);
            }
            // ---- towns: up to six, fresh ones each run, to see how many have a street that will do; the duel is in the first
            case 1 -> {
                int fresh = (int) (System.currentTimeMillis() / 1000 % 89);
                BlockPos from = new BlockPos(baseX + 12000 + townsSeen * 2900, 0, baseZ + 9000 + fresh * 3700 + townsSeen * 1300);
                BlockPos found = level.findNearestMapStructure(ModStructures.TOWN_TAG, from, 100, false);
                if (found == null) {
                    note("no town near " + from);
                    townsSeen++;
                    go(townsSeen >= TRIES ? 3 : 1, 5);
                    return;
                }
                cmd(String.format("tp @p %d 200 %d 0 90", found.getX(), found.getZ()));
                looking = found;
                go(2, 320);
            }
            case 2 -> {
                townsSeen++;
                Optional<StructureStart> start = Towns.near(level, surface(looking.getX(), looking.getZ()), 48);
                if (start.isEmpty()) {
                    note("the town at " + looking + " didn't generate");
                } else {
                    long key = TownSite.keyOf(start.get());
                    Optional<Duels.Ground> lane = Duels.ground(level, key);
                    long streets = start.get().getPieces().stream().filter(p -> p.toString().contains("street_")
                            || p instanceof net.minecraft.world.level.levelgen.structure.PoolElementStructurePiece pool && pool.getElement().toString().contains("street_")).count();
                    note(String.format("town %d: %s, %d street pieces, a street for a duel: %s%s", townsSeen, Towns.name(level, key), streets, lane.isPresent(),
                            lane.map(g -> " (marks " + BlockPos.containing(g.player()) + " and " + BlockPos.containing(g.boss()) + ")").orElse("")));
                    if (lane.isPresent()) {
                        townsWithStreet++;
                        if (town == Long.MIN_VALUE) {
                            town = key;
                            ground = lane.get();
                        }
                    }
                }
                go(townsSeen >= TRIES ? 3 : 1, 5);
            }
            case 3 -> {
                note(townsWithStreet + " of " + townsSeen + " towns have a street that will do for a duel");
                check(town != Long.MIN_VALUE, "a town with a street for a duel was found");
                if (town == Long.MIN_VALUE) {
                    go(9000, 5);
                    return;
                }
                // Back over the first town (it unloaded while the others were looked at), and wait for it to load
                cmd(String.format("tp @p %d 200 %d 0 90", (int) ground.player().x, (int) ground.player().z));
                go(35, 200);
            }
            case 35 -> {
                // Thirty blocks behind the player's mark, on the ground, in survival
                Vec3 back = ground.player().add(ground.player().subtract(ground.boss()).normalize().scale(30));
                BlockPos at = surface(back.x, back.z);
                cmd("gamemode survival @p");
                player.teleportTo(level, at.getX() + 0.5, at.getY(), at.getZ() + 0.5, Set.of(), 0.0F, 0.0F, false);
                go(4, 100);
            }
            // ---- the question: not at nine in the morning, at noon; and yes, by the client's own answer
            case 4 -> {
                check(Duels.ground(level, town).isPresent(), "the street still does with the town loaded and peopled");
                ground = Duels.ground(level, town).orElse(ground);
                Standing.add(player, town, 10);
                revolver = new ItemStack(ModItems.REVOLVER.get());
                GunItem.setAmmo(revolver, 2);
                Inventory inventory = player.getInventory();
                inventory.setItem(4, revolver);
                inventory.setItem(0, new ItemStack(net.minecraft.world.item.Items.BREAD, 5));
                inventory.setItem(1, new ItemStack(ModItems.REVOLVER_CARTRIDGE.get(), 3));
                inventory.setItem(2, new ItemStack(ModItems.PERCUSSION_CAP.get(), 3));
                inventory.setSelectedSlot(0);
                takePoster(3);
                Duels.lookForCall(player);
                check(!Duels.asked(player), "not asked at nine in the morning");
                cmd("time set 5500");
                go(5, 5);
            }
            case 5 -> {
                Duels.lookForCall(player);
                check(Duels.asked(player), "asked near noon, thirty blocks from the mark");
                check(DuelData.of(player).times(bounty.id()) == 1, "asked once so far");
                picture("hf_duel_1_asked.png", 20);
                go(6, 5);
            }
            case 6 -> {
                mc.execute(() -> ClientPacketDistributor.sendToServer(new DuelAnswerPayload(true)));
                go(7, 12);
            }
            case 7 -> {
                duel = Duels.of(player);
                check(duel != null, "yes, through the client's payload, started a duel");
                if (duel == null) {
                    go(9000, 5);
                    return;
                }
                duel.landsNext = false;
                check(player.position().distanceTo(ground.player()) < 0.5, "set on the mark (" + player.position() + ")");
                check(player.getMainHandItem() == revolver && GunItem.getAmmo(revolver) == 6, "their own revolver fetched to hand and loaded: " + player.getMainHandItem());
                check(player.getAttributeValue(Attributes.MOVEMENT_SPEED) <= 0.0, "rooted to the mark");
                check(posterNow().spawned(), "the poster reads spotted, so his camp stays empty");
                turnTo(player.getEyePosition().add(ground.boss().subtract(ground.player()).normalize().scale(5)).add(0, 3, 0)); // looking up, to see the eyes brought down
                go(8, 1);
            }
            // ---- the count: a look from behind, then through their own eyes
            case 8 -> {
                if (duel.ticks() < Duels.WALK + 3) {
                    return;
                }
                OutlawBoss boss = duel.boss();
                check(boss != null && boss.isAlive() && boss.position().distanceTo(ground.boss()) < 0.5, "he's on his mark");
                check(boss != null && boss.getMainHandItem().is(ModItems.REVOLVER.get()), "with a revolver, not his shotgun");
                check(mc.player != null && mc.player.getXRot() >= DuelClient.EYES_DOWN - 0.5F, "the eyes are held down from the horizon (pitch " + (mc.player == null ? "?" : mc.player.getXRot()) + ")");
                check(Duels.clientHolstered, "the client knows its trigger is locked");
                mc.execute(() -> mc.options.setCameraType(CameraType.THIRD_PERSON_BACK));
                picture("hf_duel_2_on_the_marks.png", 6);
                go(9, 1);
            }
            case 9 -> {
                if (duel.ticks() < Duels.WALK + Duels.TOLL_GAP + 2) {
                    return;
                }
                mc.execute(() -> mc.options.setCameraType(CameraType.FIRST_PERSON));
                // A pull before the draw, through the game's own use of the gun
                player.gameMode.useItem(player, level, player.getMainHandItem(), InteractionHand.MAIN_HAND);
                check(duel.flinches() == 1 && duel.boss().isAlive() && GunItem.getAmmo(revolver) == 6, "a pull before the draw is a flinch and fires nothing");
                picture("hf_duel_3_the_count.png", 6);
                go(10, 1);
            }
            case 10 -> {
                if (duel.ticks() < duel.drawAt() - 3) {
                    return;
                }
                check(inTheLane() == 0, "nobody of the town is standing between the two (" + inTheLane() + ")");
                go(11, 0);
            }
            case 11 -> {
                if (duel.ticks() < duel.drawAt() + 2) {
                    return;
                }
                picture("hf_duel_4_draw.png", 1);
                go(12, 0);
            }
            case 12 -> {
                if (duel.ticks() < duel.unlockAt() + 1) {
                    return;
                }
                check(!Duels.clientHolstered || duel.ticks() < duel.unlockAt() + 4, "the client's trigger comes free with the server's");
                OutlawBoss boss = duel.boss();
                turnTo(boss.position().add(0, 1.0, 0));
                player.gameMode.useItem(player, level, player.getMainHandItem(), InteractionHand.MAIN_HAND);
                check(duel.outcome() == Duels.Outcome.WON && !boss.isAlive(), "one shot at his middle after the draw wins it: " + duel.outcome());
                check(GunItem.getAmmo(revolver) == 6, "the shot spent nothing");
                picture("hf_duel_5_won.png", 12);
                go(13, 1);
            }
            case 13 -> {
                if (!duel.over()) {
                    return;
                }
                Inventory inventory = player.getInventory();
                check(inventory.getItem(4) == revolver && GunItem.getAmmo(revolver) == 2, "the revolver is back in its slot with the two rounds it had");
                check(inventory.countItem(ModItems.REVOLVER_CARTRIDGE.get()) == 3 && inventory.countItem(ModItems.PERCUSSION_CAP.get()) == 3, "no cartridge or cap was used");
                check(player.getAttributeValue(Attributes.MOVEMENT_SPEED) > 0.0, "free to walk again");
                check(Standing.of(player).points(town) == 10 + Duels.WON_STANDING, "the town thinks better of the winner: " + Standing.of(player).points(town));
                DuelData data = DuelData.of(player);
                check(data.wins() == 1 && data.won(town) == 1, "the win is on record");
                List<ItemEntity> badges = level.getEntitiesOfClass(ItemEntity.class, new AABB(BlockPos.containing(ground.boss())).inflate(5), e -> e.getItem().is(ModItems.OUTLAW_BADGE.get()));
                check(badges.size() == 1 && badges.getFirst().getItem().get(ModDataComponents.BOUNTY.get()).reward() == Duels.raised(3), "his badge is in the street and pays half as much again");
                badges.forEach(ItemEntity::discard);
                level.getEntitiesOfClass(ItemEntity.class, new AABB(BlockPos.containing(ground.boss())).inflate(5)).forEach(ItemEntity::discard);
                go(20, 20);
            }
            // ---- a second: no handgun, the town's lent, and he's quicker
            case 20 -> {
                Inventory inventory = player.getInventory();
                inventory.setItem(4, ItemStack.EMPTY);
                Purse.give(player, 5 * Purse.GOLD);
                player.setHealth(player.getMaxHealth());
                takePoster(6);
                Duels.ask(player, bounty, town, ground, BountyBoards.day(level));
                duel = Duels.answer(player, true);
                check(duel != null, "a second duel in the same street, once the first is over");
                if (duel == null) {
                    go(9000, 5);
                    return;
                }
                duel.landsNext = true;
                duel.lethal = false;
                check(Duels.lent(player.getMainHandItem()), "with no handgun, the town's revolver is in hand: " + player.getMainHandItem());
                check(inventory.countItem(net.minecraft.world.item.Items.BREAD) == 5, "what was in hand is still carried");
                note("his reaction at 6 gold: " + duel.reaction() + " ticks; the draw comes at tick " + duel.drawAt());
                go(21, 1);
            }
            case 21 -> {
                if (duel.outcome() == Duels.Outcome.NONE) {
                    return;
                }
                check(duel.outcome() == Duels.Outcome.LOST && duel.ticks() <= duel.drawAt() + duel.reaction() + 3, "he fired when his reaction said, and it landed (tick " + duel.ticks() + ")");
                check(player.isAlive() && player.getHealth() == Duels.LOSER_HEALTH, "left at one heart: " + player.getHealth());
                check(Purse.count(player) == 5 * Purse.GOLD - 5 * Purse.GOLD * Duels.PURSE_PERCENT / 100, "a tenth of the purse is gone: " + Purse.count(player));
                check(posterNow().reward() == Duels.raised(6) && !posterNow().spawned(), "he's worth half as much again on the poster, and back at his camp");
                check(Standing.of(player).points(town) == 10 + Duels.WON_STANDING - Duels.LOST_STANDING, "the town thinks less of the loser: " + Standing.of(player).points(town));
                picture("hf_duel_6_lost.png", 10);
                go(22, 1);
            }
            case 22 -> {
                if (!duel.over()) {
                    return;
                }
                check(!anyLent(), "the town has its revolver back");
                check(level.getEntitiesOfClass(OutlawBoss.class, new AABB(BlockPos.containing(ground.boss())).inflate(8)).isEmpty(), "he's gone from the street");
                cmd("effect clear @p");
                cmd("effect give @p minecraft:saturation infinite 0 true");
                player.setHealth(player.getMaxHealth());
                go(30, 20);
            }
            // ---- no, by the client's own answer; and no answer at all
            case 30 -> {
                takePoster(4);
                Duels.ask(player, bounty, town, ground, BountyBoards.day(level));
                mc.execute(() -> ClientPacketDistributor.sendToServer(new DuelAnswerPayload(false)));
                go(31, 12);
            }
            case 31 -> {
                check(!Duels.asked(player) && Duels.of(player) == null, "no, through the client's payload, costs nothing");
                check(!posterNow().spawned(), "and the poster is as it was");
                Duels.ask(player, bounty, town, ground, BountyBoards.day(level) + 1);
                go(32, Duels.ASK_TICKS + 20);
            }
            case 32 -> {
                check(!Duels.asked(player) && Duels.of(player) == null, "no answer in thirty seconds is no");
                check(!Duels.mayAsk(DuelData.of(player), bounty, 5900, BountyBoards.day(level) + 2), "asked twice, he doesn't ask again");
                go(9000, 5);
            }
            default -> {
                HollowFrontier.LOGGER.info("[Playtest] DONE: {} passed, {} failed", pass, fail);
                done = true;
                cmd("gamemode spectator @p");
                PhotoBooth.quit(mc, server);
            }
        }
    }
}
