package com.hollowfrontier.client;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.Set;

import com.hollowfrontier.HollowFrontier;
import com.hollowfrontier.content.frontier.bounty.Purse;
import com.hollowfrontier.content.frontier.outlaw.AbstractOutlaw;
import com.hollowfrontier.content.frontier.raid.TownSite;
import com.hollowfrontier.content.frontier.stake.Deputies;
import com.hollowfrontier.content.frontier.stake.Drives;
import com.hollowfrontier.content.frontier.stake.Herds;
import com.hollowfrontier.content.frontier.stake.MailboxBlock;
import com.hollowfrontier.content.frontier.stake.Neighbours;
import com.hollowfrontier.content.frontier.stake.OldPlace;
import com.hollowfrontier.content.frontier.stake.SheriffJob;
import com.hollowfrontier.content.frontier.stake.SheriffJobs;
import com.hollowfrontier.content.frontier.stake.StakeData;
import com.hollowfrontier.content.frontier.stake.StakeItems;
import com.hollowfrontier.content.frontier.stake.Standing;
import com.hollowfrontier.content.frontier.stake.TownRef;
import com.hollowfrontier.content.frontier.town.Towns;
import com.hollowfrontier.content.frontier.townsfolk.Folk;
import com.hollowfrontier.content.frontier.townsfolk.Lawman;
import com.hollowfrontier.content.frontier.townsfolk.Townsfolk;
import com.hollowfrontier.content.hollow.gray.Captives;
import com.hollowfrontier.registry.ModAttachments;
import com.hollowfrontier.registry.ModBlocks;
import com.hollowfrontier.registry.ModDataComponents;
import com.hollowfrontier.registry.ModEntities;
import com.hollowfrontier.registry.ModItems;
import com.hollowfrontier.worldgen.structure.ModStructures;
import com.hollowfrontier.worldgen.structure.OldPlaceBlueprint;

import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.Container;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.Leashable;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.entity.animal.cow.Cow;
import net.minecraft.world.entity.npc.villager.Villager;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.trading.MerchantOffer;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.level.levelgen.structure.PoolElementStructurePiece;
import net.minecraft.world.level.levelgen.structure.StructureStart;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplate;
import net.minecraft.world.level.pathfinder.Path;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientChatReceivedEvent;

/**
 * A played-through phase 47 (`runPhoto -PphotoScene=stake`): a survival player in real, generated towns, doing what a
 * player does through the game's own entry points (using the sheriff, the land seller, the stake, the hearth, a cow, the
 * wrangler), with the walking done for them. It finds a town with an Old Place standing and one without, and logs
 * `[Playtest] PASS / FAIL / NOTE` lines and what the game said to the player (`heard`), with pictures as `hf_p*`.
 * What it can't do is say whether any of it is fun.
 */
@EventBusSubscriber(modid = HollowFrontier.MODID, value = Dist.CLIENT)
public final class StakePlaytest {
    private StakePlaytest() {}

    static final boolean ENABLED = "stake".equals(System.getProperty("hollowfrontier.photo.scene"));
    private static final int TRIES = 12;
    /** -PphotoFilter=place: only the Old Place (the deed, the house, the iron, the mail, rustlers). */
    private static final boolean SHORT = "place".equals(System.getProperty("hollowfrontier.photo.filter"));

    private static int step;
    private static int wait;
    private static int count;
    private static boolean busy;
    private static boolean done;
    private static volatile String shot;
    private static volatile int shotIn;
    private static boolean spectating;
    private static int pass, fail;

    private static ServerLevel level;
    private static ServerPlayer player;
    private static int baseX, baseZ;
    private static long townA = Long.MIN_VALUE, townB = Long.MIN_VALUE, townC = Long.MIN_VALUE;
    private static int townsSeen, townsWithPlace, townsWithSheriff;
    private static boolean wranglerAtA;

    private static long townAOrSelf(long key) {
        return townA == Long.MIN_VALUE ? key : townA;
    }
    private static BlockPos lotOrigin = BlockPos.ZERO;
    private static Rotation lotTurn = Rotation.NONE;
    private static BlockPos stake = BlockPos.ZERO;
    private static BlockPos walkTo = BlockPos.ZERO;
    private static Vec3 walkFrom = Vec3.ZERO;
    private static int purse;
    private static Vec3 driveStart = Vec3.ZERO;
    private static final List<int[]> candidates = new ArrayList<>();

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

    // ---------------------------------------------------------------- helpers

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
        count = 0;
    }

    private static BlockPos ground(int x, int z) {
        return level.getHeightmapPos(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, new BlockPos(x, 0, z));
    }

    private static void survive() {
        if (spectating) {
            cmd("gamemode survival @p");
            spectating = false;
        }
    }

    private static void stand(Vec3 at, Vec3 lookAt) {
        survive();
        Vec3 v = lookAt.subtract(at);
        float yaw = (float) (Math.toDegrees(Math.atan2(v.z, v.x)) - 90.0);
        player.teleportTo(level, at.x, at.y, at.z, Set.of(), yaw, 0.0F, false);
    }

    private static void standBy(Entity entity) {
        Vec3 at = entity.position();
        stand(new Vec3(at.x + 1.2, at.y, at.z), at);
    }

    /** A picture from {@code eye} toward {@code target}, taken as a spectator a second on. */
    private static void view(Vec3 eye, Vec3 target, String name) {
        if (!spectating) {
            cmd("gamemode spectator @p");
            spectating = true;
        }
        Vec3 v = target.subtract(eye);
        float yaw = (float) (Math.toDegrees(Math.atan2(v.z, v.x)) - 90.0);
        float pitch = (float) -Math.toDegrees(Math.atan2(v.y, Math.sqrt(v.x * v.x + v.z * v.z)));
        cmd(String.format(Locale.ROOT, "tp @p %.2f %.2f %.2f %.1f %.1f", eye.x, eye.y, eye.z, yaw, pitch));
        shotIn = 30;
        shot = name;
    }

    private static void use(Entity entity) {
        player.interactOn(entity, InteractionHand.MAIN_HAND, entity.position());
    }

    private static void hold(ItemStack stack) {
        player.setItemInHand(InteractionHand.MAIN_HAND, stack);
    }

    private static BoundingBox bounds(long town) {
        return Towns.start(level, town).orElseThrow().getBoundingBox();
    }

    private static AABB around(long town) {
        return AABB.of(bounds(town)).inflate(24);
    }

    private static Optional<Lawman> sheriff(long town) {
        List<Lawman> law = level.getEntitiesOfClass(Lawman.class, around(town), l -> l.isAlive() && !l.isPosse());
        return law.stream().filter(l -> l.getRank() == Lawman.Rank.SHERIFF).findFirst().or(() -> law.stream().findFirst());
    }

    private static List<Townsfolk> folk(long town, Folk kind) {
        return level.getEntitiesOfClass(Townsfolk.class, around(town), f -> f.isAlive() && f.getType() == ModEntities.TOWNSFOLK.get() && f.getFolk() == kind);
    }

    private static BlockPos lot(int x, int y, int z) {
        return StructureTemplate.transform(new BlockPos(x, y, z), Mirror.NONE, lotTurn, BlockPos.ZERO).offset(lotOrigin);
    }

    private static Vec3 lotAt(double x, double y, double z) {
        BlockPos block = lot((int) Math.floor(x), (int) Math.floor(y), (int) Math.floor(z));
        return new Vec3(block.getX() + 0.5, block.getY() + (y - Math.floor(y)), block.getZ() + 0.5);
    }

    private static void resist() {
        cmd("effect give @p minecraft:resistance infinite 4 true");
        cmd("effect give @p minecraft:saturation infinite 0 true");
        cmd("effect give @p minecraft:night_vision infinite 0 true");
    }

    /** Walks the player a step toward {@link #walkTo} over the ground. Whether they're there. */
    private static boolean walk(double speed) {
        Vec3 at = player.position();
        double dx = walkTo.getX() + 0.5 - at.x, dz = walkTo.getZ() + 0.5 - at.z;
        double far = Math.hypot(dx, dz);
        if (far < 1.5) {
            return true;
        }
        double nx = at.x + dx / far * Math.min(speed, far), nz = at.z + dz / far * Math.min(speed, far);
        BlockPos g = ground((int) Math.floor(nx), (int) Math.floor(nz));
        float yaw = (float) (Math.toDegrees(Math.atan2(dz, dx)) - 90.0);
        player.teleportTo(level, nx, g.getY(), nz, Set.of(), yaw, 10.0F, false);
        return false;
    }

    // ---------------------------------------------------------------- the play

    private static void advance(Minecraft mc, MinecraftServer server) {
        switch (step) {
            case 0 -> {
                baseX = player.getBlockX();
                baseZ = player.getBlockZ();
                cmd("gamemode spectator @p");
                spectating = true;
                cmd("time set 1000");
                cmd("weather clear");
                cmd("gamerule advance_weather false");
                cmd("clear @p");
                for (var type : List.of(ModAttachments.STANDING, ModAttachments.SHERIFF_JOB, ModAttachments.LAST_JOB_DAY, ModAttachments.DRIVE)) {
                    player.removeData(type.get());
                }
                new ArrayList<>(StakeData.get(level).of(player.getUUID())).forEach(claim -> StakeData.get(level).remove(claim.stake()));
                resist();
                note("playtest begins; the player is " + player.getGameProfile().name());
                go(1, 5);
            }
            // ---- towns: look at up to eight, fresh ones each run, until one has an Old Place and one hasn't
            case 1 -> {
                int fresh = (int) (System.currentTimeMillis() / 1000 % 89);
                BlockPos from = new BlockPos(baseX + 12000 + count * 0 + townsSeen * 2900, 0, baseZ + 9000 + fresh * 3700 + townsSeen * 1300);
                BlockPos found = level.findNearestMapStructure(ModStructures.TOWN_TAG, from, 100, false);
                if (found == null) {
                    note("no town near " + from);
                    townsSeen++;
                    go(townsSeen >= TRIES ? 3 : 1, 5);
                    return;
                }
                cmd(String.format("tp @p %d 220 %d 0 90", found.getX(), found.getZ()));
                walkTo = found;
                go(2, 320);
            }
            case 2 -> {
                townsSeen++;
                Optional<StructureStart> start = Towns.near(level, ground(walkTo.getX(), walkTo.getZ()), 48);
                if (start.isEmpty()) {
                    note("the town at " + walkTo + " didn't generate");
                } else {
                    long key = TownSite.keyOf(start.get());
                    boolean place = !OldPlace.standing(start.get()).isEmpty();
                    townsWithPlace += place ? 1 : 0;
                    List<Lawman> law = level.getEntitiesOfClass(Lawman.class, around(key), Entity::isAlive);
                    boolean office = start.get().getPieces().stream().anyMatch(p -> p.toString().contains("sheriff_office") || p instanceof PoolElementStructurePiece pool && pool.getElement().toString().contains("sheriff_office"));
                    note("town " + townsSeen + " is at " + walkTo.getX() + ", " + walkTo.getZ() + "; its pieces: " + start.get().getPieces().stream().filter(p -> p instanceof PoolElementStructurePiece).map(p -> ((PoolElementStructurePiece) p).getElement().toString().replaceAll(".*town/", "").replaceAll("[\\]].*", "")).filter(n -> n.contains("ranch") || n.contains("wheat") || n.contains("old_place") || n.contains("boot_hill") || n.contains("limits")).toList());
                    note(String.format("town %d: %s (%s), %d pieces, Old Place standing: %s, sheriff's office %s, sheriff %s, lawmen %s, banker %d, postmaster %d, wrangler %d", townsSeen,
                            Towns.name(level, key), Towns.style(level, start.get()), start.get().getPieces().size(), place, office,
                            sheriff(key).isPresent(), law.stream().map(l -> l.getRank() + (l.isPosse() ? "(posse)" : "")).toList(),
                            folk(key, Folk.BANKER).size(), folk(key, Folk.POSTMASTER).size(), folk(key, Folk.WRANGLER).size()));
                    townsWithSheriff += sheriff(key).isPresent() ? 1 : 0;
                    boolean noSheriff = law.stream().noneMatch(l -> l.getRank() == Lawman.Rank.SHERIFF);
                    boolean wrangler = !folk(key, Folk.WRANGLER).isEmpty();
                    if (place && (townA == Long.MIN_VALUE || !wranglerAtA && wrangler)) {
                        wranglerAtA = wrangler;
                        townA = key;
                    } else if (!place && townB == Long.MIN_VALUE) {
                        townB = key;
                    }
                }
                boolean enough = townA != Long.MIN_VALUE && (SHORT || townB != Long.MIN_VALUE && townsSeen >= 6);
                go(enough || townsSeen >= TRIES ? 3 : 1, 5);
            }
            case 3 -> {
                note(String.format("%d of %d towns looked at have an Old Place standing; %d of them have a sheriff", townsWithPlace, townsSeen, townsWithSheriff));
                check(townA != Long.MIN_VALUE, "found a town with an Old Place standing");
                if (townA == Long.MIN_VALUE) {
                    go(townB != Long.MIN_VALUE ? 700 : 9000, 5);
                    return;
                }
                BlockPos middle = Towns.middle(townA);
                cmd(String.format("tp @p %d 200 %d 0 90", middle.getX(), middle.getZ()));
                go(10, 200);
            }
            // ---- a stranger in town
            case 10 -> {
                Optional<Lawman> sheriff = sheriff(townA);
                check(sheriff.isPresent(), "town A has a sheriff to ask");
                if (sheriff.isEmpty()) {
                    Standing.add(player, townA, 32);
                    go(40, 5);
                    return;
                }
                standBy(sheriff.get());
                go(11, 20);
            }
            case 11 -> {
                Lawman sheriff = sheriff(townA).orElseThrow();
                standBy(sheriff);
                use(sheriff);
                check(!Deputies.carries(player, townA), "a stranger is not handed a star");
                Standing.add(player, townA, 12);
                check(Standing.rank(player, townA) == Standing.Rank.REGULAR, "12 points make a Regular");
                use(sheriff);
                check(!Deputies.carries(player, townA), "a Regular is not handed a star");
                Standing.add(player, townA, 20);
                check(Standing.rank(player, townA) == Standing.Rank.NEIGHBOUR, "32 points make a Neighbour (points given by the script, not earned)");
                go(12, 30);
            }
            case 12 -> {
                Lawman sheriff = sheriff(townA).orElseThrow();
                standBy(sheriff);
                use(sheriff);
                check(Deputies.carries(player, townA) && Standing.of(player).deputyOf(townA), "the sheriff swears a Neighbour in and hands over the star");
                go(SHORT ? 40 : 13, 30);
            }
            case 13 -> {   // by day: a warrant or a drifter
                Lawman sheriff = sheriff(townA).orElseThrow();
                standBy(sheriff);
                long before = System.nanoTime();
                use(sheriff);
                SheriffJob job = SheriffJobs.of(player);
                check(job != null, "asked by day, the sheriff gives a job" + (job == null ? "" : ": " + job.kind())
                        + String.format(" (the asking took %d ms)", (System.nanoTime() - before) / 1_000_000));
                // the drifter, whatever he gave
                cmd("clear @p hollowfrontier:warrant");
                BlockPos at = sheriff.blockPosition();
                BlockPos spot = ground(at.getX() + 85, at.getZ() + 20);
                SheriffJobs.giveBringIn(player, townA, spot);
                BlockPos near = ground(spot.getX() - 12, spot.getZ());
                stand(Vec3.atBottomCenterOf(near), Vec3.atBottomCenterOf(spot));
                go(14, 60);
            }
            case 14 -> {
                SheriffJob job = SheriffJobs.of(player);
                Optional<AbstractOutlaw> drifter = job == null ? Optional.empty() : job.quarry().map(level::getEntity).filter(AbstractOutlaw.class::isInstance).map(AbstractOutlaw.class::cast);
                check(drifter.isPresent(), "the drifter turns up as the deputy nears his spot");
                if (drifter.isEmpty()) {
                    go(20, 5);
                    return;
                }
                AbstractOutlaw outlaw = drifter.get();
                note("the drifter is " + outlaw.getDisplayName().getString() + ", " + (int) outlaw.distanceTo(player) + " blocks off, after " + (outlaw.getTarget() == null ? "nobody" : outlaw.getTarget().getName().getString()));
                check(SheriffJobs.rope(level, player, outlaw) && !outlaw.isJailed(), "an unhurt drifter shrugs off the rope");
                outlaw.hurtServer(level, level.damageSources().playerAttack(player), outlaw.getMaxHealth() * 0.6F);
                check(outlaw.isAlive() && SheriffJobs.rope(level, player, outlaw) && outlaw.isJailed() && Captives.isPrisonerOf(outlaw, player), "hurt, he gives up to the rope and is the deputy's prisoner");
                walkTo = sheriff(townA).map(Entity::blockPosition).orElse(Towns.middle(townA));
                walkFrom = player.position();
                go(15, 10);
            }
            case 15 -> {   // the walk back, at a walk
                boolean there = walk(0.215);
                count++;
                if (count % 100 == 0 || there) {
                    SheriffJob job = SheriffJobs.of(player);
                    Entity prisoner = job == null ? null : job.quarry().map(level::getEntity).orElse(null);
                    note(String.format("walking him in: %d ticks, %d blocks to go, prisoner %s", count, (int) Math.sqrt(player.blockPosition().distSqr(walkTo)),
                            prisoner == null ? "gone" : (int) prisoner.distanceTo(player) + " blocks behind"));
                }
                if (there || count > 1200) {
                    step = 16;
                    wait = 40;
                }
            }
            case 16 -> {
                Lawman sheriff = sheriff(townA).orElseThrow();
                SheriffJob job = SheriffJobs.of(player);
                Entity prisoner = job == null ? null : job.quarry().map(level::getEntity).orElse(null);
                check(prisoner != null && prisoner.distanceTo(player) < 12, "the prisoner kept up all the way to the sheriff");
                purse = Purse.count(player);
                int before = Standing.of(player).points(townA);
                standBy(sheriff);
                use(sheriff);
                check(SheriffJobs.of(player) == null, "using the sheriff with the prisoner in tow finishes the job");
                note(String.format("bring-in paid %d (purse %d -> %d), standing %d -> %d", Purse.count(player) - purse, purse, Purse.count(player), before, Standing.of(player).points(townA)));
                go(20, 20);
            }
            // ---- night rounds, asked for the way a player would
            case 20 -> {
                player.removeData(ModAttachments.SHERIFF_JOB.get());
                player.removeData(ModAttachments.LAST_JOB_DAY.get());
                cmd("time set 14500");
                go(21, 40);
            }
            case 21 -> {
                Lawman sheriff = sheriff(townA).orElseThrow();
                standBy(sheriff);
                use(sheriff);
                SheriffJob job = SheriffJobs.of(player);
                check(job != null && job.kind() == SheriffJob.Kind.ROUNDS, "asked at night, the sheriff gives the rounds" + (job == null ? "" : " (" + job.points().size() + " stops)"));
                go(job != null && job.kind() == SheriffJob.Kind.ROUNDS ? 22 : 30, 10);
            }
            case 22 -> {
                SheriffJob job = SheriffJobs.of(player);
                if (job == null || job.walked() || count > 3000) {
                    check(job != null && job.walked(), "every stop of the rounds can be walked to (" + count + " ticks at a walk)");
                    go(23, 10);
                    return;
                }
                walkTo = job.points().get(job.progress());
                walk(0.215);
                count++;
            }
            case 23 -> {
                Lawman sheriff = sheriff(townA).orElseThrow();
                purse = Purse.count(player);
                standBy(sheriff);
                use(sheriff);
                check(SheriffJobs.of(player) == null && Purse.count(player) == purse + SheriffJobs.ROUNDS_PAY, "reporting back pays the rounds");
                level.getEntitiesOfClass(AbstractOutlaw.class, around(townA).inflate(40)).forEach(Entity::discard);
                cmd("time set 1000");
                go(30, 20);
            }
            // ---- the warrant: to the next town and its sheriff
            case 30 -> {
                player.removeData(ModAttachments.LAST_JOB_DAY.get());
                long before = System.nanoTime();
                Optional<Neighbours.Neighbour> next = Neighbours.of(level, townA);
                note(String.format("looking for town A's neighbour took %d ms", (System.nanoTime() - before) / 1_000_000));
                {   // how thick on the ground towns are round here, out to twice the reach
                    BlockPos home = Towns.middle(townA);
                    List<net.minecraft.world.level.ChunkPos> spots = Neighbours.spots(level, townA, 6000);
                    List<Integer> found = new ArrayList<>();
                    for (net.minecraft.world.level.ChunkPos spot : spots) {
                        if (Towns.start(level, spot.pack()).isPresent()) {
                            found.add((int) Math.sqrt(home.distSqr(spot.getMiddleBlockPosition(64))));
                        }
                    }
                    note("of " + spots.size() + " places a town could stand within 6000 blocks of town A, " + found.size() + " have one; how far off: " + found);
                }
                check(next.isPresent(), "town A has a neighbour within reach" + next.map(n -> ": " + n.name() + ", " + (int) Math.sqrt(n.pos().distSqr(Towns.middle(townA))) + " blocks").orElse(""));
                if (next.isEmpty()) {
                    go(40, 5);
                    return;
                }
                townC = next.get().key();
                SheriffJobs.giveWarrant(level, player, townA, next.get());
                check(player.getInventory().contains(new ItemStack(ModItems.WARRANT.get())) || player.getInventory().hasAnyOf(Set.of(ModItems.WARRANT.get())), "the warrant is in the deputy's pack");
                cmd("gamemode spectator @p");
                spectating = true;
                cmd(String.format("tp @p %d 200 %d 0 90", next.get().pos().getX(), next.get().pos().getZ()));
                go(31, 320);
            }
            case 31 -> {
                Optional<Lawman> sheriff = sheriff(townC);
                check(sheriff.isPresent(), "the neighbour town has a sheriff to serve it on");
                if (sheriff.isPresent()) {
                    standBy(sheriff.get());
                    int slot = player.getInventory().findSlotMatchingItem(new ItemStack(ModItems.WARRANT.get()));
                    ItemStack warrant = ItemStack.EMPTY;
                    for (int i = 0; i < player.getInventory().getContainerSize(); i++) {
                        if (player.getInventory().getItem(i).is(ModItems.WARRANT.get())) {
                            warrant = player.getInventory().removeItemNoUpdate(i);
                        }
                    }
                    hold(warrant);
                    purse = Purse.count(player);
                    use(sheriff.get());
                    check(SheriffJobs.of(player) == null && !player.getMainHandItem().is(ModItems.WARRANT.get()), "the warrant is served");
                    note(String.format("the warrant paid %d; standing in the neighbour town is now %d (slot hint %d)", Purse.count(player) - purse, Standing.of(player).points(townC), slot));
                }
                hold(ItemStack.EMPTY);
                cmd("gamemode spectator @p");
                spectating = true;
                BlockPos middle = Towns.middle(townA);
                cmd(String.format("tp @p %d 200 %d 0 90", middle.getX(), middle.getZ()));
                go(40, 200);
            }
            // ---- the deed
            case 40 -> {
                List<Townsfolk> sellers = new ArrayList<>(folk(townA, Folk.BANKER));
                sellers.addAll(folk(townA, Folk.POSTMASTER));
                check(!sellers.isEmpty(), "town A has somebody to sell the deed (banker or postmaster)");
                if (sellers.isEmpty()) {
                    go(9000, 5);
                    return;
                }
                Townsfolk seller = sellers.getFirst();
                standBy(seller);
                use(seller);   // a postmaster's first business with a newcomer is the Survey's letter
                player.closeContainer();
                seller.setTradingPlayer(null);
                use(seller);
                Optional<MerchantOffer> offer = seller.getOffers().stream().filter(o -> o.getResult().is(ModItems.DEED.get())).findFirst();
                check(offer.isPresent(), "the " + seller.getFolk() + " offers a Neighbour the deed" + offer.map(o -> " for " + o.getCostA().getCount() + " " + o.getCostA().getHoverName().getString()).orElse(""));
                player.closeContainer();
                seller.setTradingPlayer(null);
                if (offer.isEmpty()) {
                    go(9000, 5);
                    return;
                }
                hold(offer.get().getResult().copy());   // the buying itself is a click in a screen: taken as read
                TownRef ref = player.getMainHandItem().get(ModDataComponents.TOWN.get());
                check(ref != null && ref.key() == townA, "the deed names the town: " + (ref == null ? "nothing" : ref.name()));
                go(41, 10);
            }
            case 41 -> {
                StructureStart start = Towns.start(level, townA).orElseThrow();
                PoolElementStructurePiece piece = start.getPieces().stream().filter(p -> p instanceof PoolElementStructurePiece pool && pool.getElement().toString().contains(OldPlaceBlueprint.NAME))
                        .map(PoolElementStructurePiece.class::cast).findFirst().orElseThrow();
                lotOrigin = piece.getPosition();
                lotTurn = piece.getRotation();
                stake = lot(OldPlaceBlueprint.STAKE.getX(), OldPlaceBlueprint.STAKE.getY(), OldPlaceBlueprint.STAKE.getZ());
                check(level.getBlockState(stake).is(ModBlocks.CLAIM_STAKE.get()), "the Claim Stake stands by the gate of the generated Old Place");
                // first on bare ground somewhere else in town: the deed should point at the place that's standing
                BlockPos elsewhere = ground(Towns.middle(townA).getX(), Towns.middle(townA).getZ()).below();
                stand(Vec3.atBottomCenterOf(elsewhere.above()), Vec3.atBottomCenterOf(stake));
                player.gameMode.useItemOn(player, level, player.getMainHandItem(), InteractionHand.MAIN_HAND, new BlockHitResult(Vec3.atCenterOf(elsewhere), Direction.UP, elsewhere, false));
                check(StakeData.get(level).of(player.getUUID(), townA).isEmpty() && player.getMainHandItem().is(ModItems.DEED.get()), "used on bare ground, the deed is kept and points to the standing place");
                go(42, 30);
            }
            case 42 -> {
                stand(lotAt(OldPlaceBlueprint.STAKE.getX(), 1, -2), Vec3.atBottomCenterOf(stake));
                player.gameMode.useItemOn(player, level, player.getMainHandItem(), InteractionHand.MAIN_HAND, new BlockHitResult(Vec3.atCenterOf(stake), Direction.NORTH, stake, false));
                Optional<StakeData.Claim> claim = StakeData.get(level).at(stake);
                check(claim.isPresent() && claim.get().owner().equals(player.getUUID()) && !player.getMainHandItem().is(ModItems.DEED.get()), "used on the stake, the deed is spent and the place is the player's");
                check(claim.isPresent() && claim.get().contains(lot(1, 1, OldPlaceBlueprint.DEPTH - 2)) && claim.get().contains(lot(OldPlaceBlueprint.WIDTH - 2, 1, OldPlaceBlueprint.DEPTH - 2)), "the claim covers the whole lot, back corners and all");
                go(43, 20);
            }
            // ---- a look round the place as it generated
            case 43 -> {
                terrain();
                view(lotAt(OldPlaceBlueprint.GATE_X - 9, 5, -16), lotAt(OldPlaceBlueprint.GATE_X - 2, 4, 12), "hf_p01_old_place_from_the_street.png");
                go(44, 5);
            }
            case 44 -> {
                view(lotAt(OldPlaceBlueprint.GATE_X, 30, -22), lotAt(OldPlaceBlueprint.GATE_X, 0, 14), "hf_p02_old_place_in_its_town.png");
                go(45, 5);
            }
            case 45 -> {
                view(lotAt(8, 2.7, 5), lotAt(9, 5, 9), "hf_p03_veranda.png");
                go(46, 5);
            }
            case 46 -> {
                view(lotAt(9, 2.7, 10), lotAt(4, 1.5, 14), "hf_p04_parlour.png");
                go(47, 5);
            }
            case 47 -> {
                view(lotAt(10, 2.7, 10), lotAt(14, 1.5, 15), "hf_p05_kitchen.png");
                go(48, 5);
            }
            case 48 -> {
                view(lotAt(7, 7.7, 10), lotAt(4, 6.5, 14), "hf_p06_big_bedroom.png");
                go(49, 5);
            }
            case 49 -> {
                view(lotAt(9, 7.7, 6), lotAt(OldPlaceBlueprint.GATE_X, 2, -8), "hf_p07_from_the_balcony.png");
                go(50, 5);
            }
            case 50 -> {
                view(lotAt(26, 2.7, 4), lotAt(26, 4, 12), "hf_p08_barn.png");
                go(51, 5);
            }
            case 51 -> {
                reach();
                go(60, 10);
            }
            // ---- the iron: heated at the hearth, used on the corral's cattle
            case 60 -> {
                ItemStack iron = new ItemStack(ModItems.BRANDING_IRON.get());
                hold(iron);
                List<Cow> cows = level.getEntitiesOfClass(Cow.class, new AABB(stake).inflate(StakeData.RADIUS, 12, StakeData.RADIUS));
                check(cows.size() >= OldPlaceBlueprint.CATTLE, "the corral's cattle are there (" + cows.size() + " head)");
                if (cows.isEmpty()) {
                    go(70, 5);
                    return;
                }
                standBy(cows.getFirst());
                use(cows.getFirst());
                check(Herds.brandOf(cows.getFirst()) == null, "a cold iron brands nothing");
                BlockPos hearth = lot(3, 0, 13);
                stand(lotAt(5, 1, 13), Vec3.atCenterOf(hearth));
                player.gameMode.useItemOn(player, level, player.getMainHandItem(), InteractionHand.MAIN_HAND, new BlockHitResult(Vec3.atCenterOf(hearth), Direction.EAST, hearth, false));
                check(StakeItems.BrandingIron.hot(player.getMainHandItem(), level), "the iron heats at the parlour hearth (" + level.getBlockState(hearth).getBlock().getName().getString() + ")");
                if (!StakeItems.BrandingIron.hot(player.getMainHandItem(), level)) {
                    player.getMainHandItem().set(ModDataComponents.HOT_UNTIL.get(), level.getGameTime() + 1200);
                }
                for (Cow cow : cows) {
                    standBy(cow);
                    use(cow);
                }
                long branded = cows.stream().filter(c -> Herds.brandOf(c) != null).count();
                check(branded == cows.size(), "a hot iron brands every head (" + branded + " of " + cows.size() + "), named: " + cows.getFirst().getName().getString());
                check(Herds.count(level, StakeData.get(level).at(stake).orElseThrow()) == cows.size(), "the place counts its branded stock");
                hold(ItemStack.EMPTY);
                go(61, 10);
            }
            // ---- the mail
            case 61 -> {
                stand(lotAt(OldPlaceBlueprint.GATE_X, 1, 3), Vec3.atBottomCenterOf(stake));
                go(62, 20);
            }
            case 62 -> {
                Optional<Container> box = MailboxBlock.of(level, StakeData.get(level).at(stake).orElseThrow());
                check(box.isPresent(), "the place's mailbox is found at the gate");
                count++;
                boolean paper = box.isPresent() && box.get().countItem(ModItems.FRONTIER_GAZETTE.get()) == 1;
                if (paper || count > 70 || box.isEmpty()) {
                    check(paper, "the Gazette is in the mailbox within a mail round (waited " + count * 10 + " ticks)");
                    box.ifPresent(Container::clearContent);
                    cmd("time add 24000");
                    go(63, 10);
                } else {
                    wait = 10;
                    pass--;   // the same check, asked again
                }
            }
            case 63 -> {
                Optional<Container> box = MailboxBlock.of(level, StakeData.get(level).at(stake).orElseThrow());
                count++;
                boolean paper = box.isPresent() && box.get().countItem(ModItems.FRONTIER_GAZETTE.get()) == 1;
                if (paper || count > 70) {
                    check(paper, "and the next day's comes once the last was taken in (waited " + count * 10 + " ticks)");
                    go(64, 5);
                } else {
                    wait = 10;
                }
            }
            // ---- rustlers, sent for (they come by chance at night)
            case 64 -> {
                cmd("time set 18000");
                StakeData.Claim claim = StakeData.get(level).at(stake).orElseThrow();
                List<Animal> stock = Herds.on(level, claim);
                int sent = Herds.rustle(level, claim, stock.subList(0, Math.min(2, stock.size())));
                check(sent > 0, sent + " rustlers ride in for the stock");
                purse = stock.size();
                view(lotAt(OldPlaceBlueprint.GATE_X, 22, OldPlaceBlueprint.DEPTH + 14), lotAt(26, 0, 22), "hf_p09_rustlers.png");
                go(65, 5);
            }
            case 65 -> {
                count++;
                StakeData.Claim claim = StakeData.get(level).at(stake).orElseThrow();
                List<AbstractOutlaw> rustlers = level.getEntitiesOfClass(AbstractOutlaw.class, new AABB(stake).inflate(70, 30, 70), Entity::isAlive);
                List<Animal> stock = Herds.on(level, claim);
                long roped = stock.stream().filter(a -> a instanceof Leashable l && l.isLeashed()).count();
                if (count % 5 == 0 || rustlers.isEmpty()) {
                    note(String.format("rustling, %d s: %d rustlers about (nearest %d blocks from a cow), %d head on the place, %d on a rope", count * 2, rustlers.size(),
                            rustlers.stream().mapToInt(r -> stock.stream().mapToInt(c -> (int) c.distanceTo(r)).min().orElse(99)).min().orElse(-1), stock.size(), roped));
                }
                if (rustlers.isEmpty() || count >= 70) {
                    check(stock.size() < purse || roped > 0, "left alone, the rustlers get at the stock in the corral (" + (purse - stock.size()) + " head gone, " + roped + " roped, after " + count * 2 + " s)");
                    rustlers.forEach(Entity::discard);
                    stock.forEach(a -> ((Leashable) a).removeLeash());
                    cmd("time set 1000");
                    go(SHORT ? 9000 : 70, 10);
                } else {
                    wait = 40;
                }
            }
            // ---- a herd for the next town
            case 70 -> {
                List<Townsfolk> wranglers = folk(townA, Folk.WRANGLER);
                check(!wranglers.isEmpty(), "town A has a wrangler (" + wranglers.size() + ")");
                if (wranglers.isEmpty()) {
                    go(700, 5);
                    return;
                }
                Townsfolk wrangler = wranglers.getFirst();
                standBy(wrangler);
                player.setShiftKeyDown(true);
                use(wrangler);
                player.setShiftKeyDown(false);
                Drives.Drive drive = Drives.of(player);
                check(drive != null, "sneak-using the wrangler turns out a herd");
                if (drive == null) {
                    go(700, 5);
                    return;
                }
                List<Animal> herd = level.getEntitiesOfClass(Animal.class, around(townA).inflate(80), a -> Herds.herdOf(a) != null && Herds.herdOf(a).drive().equals(drive.id()));
                int far = (int) Math.sqrt(drive.dest().distSqr(player.blockPosition()));
                note(String.format("the drive: %d head turned out (%d asked for), bound %s, %d blocks; it would pay %d a head", herd.size(), drive.head(),
                        drive.to().map(k -> Towns.name(level, k)).orElse("for the trail's end"), far, Drives.perHead(far)));
                driveStart = centre(herd);
                note("the herd is held " + (int) Math.sqrt(wrangler.position().distanceToSqr(driveStart)) + " blocks from the wrangler, inside the town: " + Towns.keyAt(level, BlockPos.containing(driveStart)).isPresent());
                Vec3 behind = driveStart.subtract(new Vec3(drive.dest().getX() - driveStart.x, 0, drive.dest().getZ() - driveStart.z).normalize().scale(7));
                BlockPos g = ground((int) behind.x, (int) behind.z);
                player.teleportTo(level, g.getX() + 0.5, g.getY(), g.getZ() + 0.5, Set.of(), 0, 0, false);
                view(player.position().add(0, 14, 0).add(driveDir(drive).scale(-14)), driveStart, "hf_p10_herd_turned_out.png");
                go(71, 5);
            }
            case 71 -> {
                survive();
                Drives.Drive drive = Drives.of(player);
                if (drive == null) {
                    check(false, "the drive ended on its own after " + count + " ticks");
                    go(700, 5);
                    return;
                }
                List<Animal> herd = Drives.herd(level, player, drive);
                Vec3 c = herd.isEmpty() ? player.position() : centre(herd);
                Vec3 dir = new Vec3(drive.dest().getX() - c.x, 0, drive.dest().getZ() - c.z).normalize();
                Vec3 behind = c.subtract(dir.scale(6.0));
                walkTo = BlockPos.containing(behind);
                walk(0.215);
                count++;
                if (count == 2700) {
                    check(Herds.spook(level, player.position(), Herds.SPOOK_RANGE), "a shot behind the herd stampedes it");
                }
                if (count % 300 == 0) {
                    double spread = herd.stream().mapToDouble(a -> Math.sqrt(a.position().distanceToSqr(c))).max().orElse(0);
                    double made = Math.sqrt(c.distanceToSqr(driveStart));
                    double toGo = Math.sqrt(drive.dest().distSqr(BlockPos.containing(c)));
                    int inWater = (int) herd.stream().filter(Entity::isInWater).count();
                    note(String.format(Locale.ROOT, "driving, %d s: %d of %d head with the drover, widest %.0f blocks from the bunch, %.0f blocks made (%.1f a second), %.0f to go, %d in water",
                            count / 20, herd.size(), drive.head(), spread, made, made / (count / 20.0), toGo, inWater));
                }
                if (count >= 3600) {
                    double made = Math.sqrt(c.distanceToSqr(driveStart));
                    double toGo = Math.sqrt(drive.dest().distSqr(BlockPos.containing(c)));
                    check(herd.size() >= drive.head() * 3 / 4, "after three minutes' driving most of the herd is still with the drover (" + herd.size() + " of " + drive.head() + ")");
                    check(made > 150, String.format(Locale.ROOT, "the herd makes real ground: %.0f blocks in 180 s; at that pace the rest is %.0f minutes", made, toGo / Math.max(0.01, made / 180.0) / 60.0));
                    view(player.position().add(0, 16, 0).subtract(dir.scale(16)), c, "hf_p11_herd_on_the_trail.png");
                    go(72, 5);
                }
            }
            case 72 -> {   // the rest of the way in one step, herd and all, to see the arrival
                Drives.Drive drive = Drives.of(player);
                if (drive == null) {
                    go(700, 5);
                    return;
                }
                survive();
                List<Animal> herd = Drives.herd(level, player, drive);
                purse = Purse.count(player);
                count = herd.size();
                BlockPos dest = drive.dest();
                cmd(String.format("forceload add %d %d %d %d", dest.getX() - 40, dest.getZ() - 40, dest.getX() + 40, dest.getZ() + 40));
                walkTo = dest.offset(40, 0, 0);   // short of the end: the herd is brought up, then walked the last of it
                go(73, 5);
                step = 73;
                wait = 60;
                int kept = count;
                count = kept;
                carried.clear();
                carried.addAll(herd);
            }
            case 73 -> {
                BlockPos g = ground(walkTo.getX(), walkTo.getZ());
                for (int i = 0; i < carried.size(); i++) {
                    carried.get(i).teleportTo(level, g.getX() + 2 + i % 4, g.getY() + 1, g.getZ() + 2 + i / 4, Set.of(), 0, 0, false);
                }
                player.teleportTo(level, g.getX() + 6.5, g.getY() + 1, g.getZ() + 0.5, Set.of(), 0, 0, false);
                cmd("gamemode survival @p");
                spectating = false;
                step = 75;
                wait = 40;
                count = 0;
            }
            case 75 -> {   // the last forty blocks, driven
                Drives.Drive drive = Drives.of(player);
                count++;
                if (drive == null || count > 1200) {
                    note("the last 40 blocks to the trail's end took " + count / 20 + " s with the herd");
                    step = 74;
                    wait = 5;
                    return;
                }
                List<Animal> herd = Drives.herd(level, player, drive);
                Vec3 c = centre(herd);
                Vec3 dir = new Vec3(drive.dest().getX() - c.x, 0, drive.dest().getZ() - c.z).normalize();
                walkTo = BlockPos.containing(c.subtract(dir.scale(6.0)));
                walk(0.215);
                if (count % 100 == 1) {
                    note("bringing them in: " + herd.size() + " head by the drover, " + (int) Math.sqrt(drive.dest().distSqr(player.blockPosition())) + " blocks to the end, ground " + level.getBlockState(player.blockPosition().below()).getBlock().getName().getString());
                }
            }
            case 74 -> {
                int head = carried.size();
                Drives.Drive drive = Drives.of(player);
                if (drive != null) {
                    note("not paid on reaching the place the wrangler named (" + walkTo + ", in a town: " + Towns.keyAt(level, player.blockPosition()).isPresent() + "); trying the town's own wrangler");
                    List<Townsfolk> wranglers = drive.to().map(k -> folk(k, Folk.WRANGLER)).orElse(List.of());
                    if (!wranglers.isEmpty()) {
                        standBy(wranglers.getFirst());
                        for (Animal cow : carried) {
                            cow.teleportTo(level, player.getX() + 2, player.getY(), player.getZ() + 2, Set.of(), 0, 0, false);
                        }
                        player.setShiftKeyDown(true);
                        use(wranglers.getFirst());
                        player.setShiftKeyDown(false);
                    }
                }
                check(Drives.of(player) == null && Purse.count(player) > purse, String.format("arriving with %d head pays (%d copper)", head, Purse.count(player) - purse));
                cmd("forceload remove all");
                carried.clear();
                go(700, 20);
            }
            // ---- a town with no Old Place: the deed raises one
            case 700 -> {
                if (townB == Long.MIN_VALUE) {
                    note("every town looked at had an Old Place standing: the deed's raising wasn't played");
                    go(9000, 5);
                    return;
                }
                BlockPos middle = Towns.middle(townB);
                cmd("gamemode spectator @p");
                spectating = true;
                cmd(String.format("tp @p %d 200 %d 0 90", middle.getX(), middle.getZ()));
                go(701, 200);
            }
            case 701 -> {
                survive();
                Standing.add(player, townB, 32);
                ItemStack deed = new ItemStack(ModItems.DEED.get());
                deed.set(ModDataComponents.TOWN.get(), new TownRef(townB, Towns.name(level, townB)));
                survive();
                hold(deed);
                BoundingBox box = bounds(townB);
                candidates.clear();
                for (int out : new int[] {10, 22, 36, 52}) {
                    for (int i = 0; i < 16; i++) {
                        double a = Math.PI * 2 * i / 16;
                        double rx = box.getXSpan() / 2.0 + out, rz = box.getZSpan() / 2.0 + out;
                        candidates.add(new int[] {(int) Math.round(box.getCenter().getX() + Math.cos(a) * rx), (int) Math.round(box.getCenter().getZ() + Math.sin(a) * rz), out});
                    }
                }
                go(702, 5);
            }
            case 702 -> {
                if (StakeData.get(level).of(player.getUUID(), townB).isPresent() || count >= candidates.size()) {
                    Optional<StakeData.Claim> claim = StakeData.get(level).of(player.getUUID(), townB);
                    check(claim.isPresent(), "the deed raises an Old Place by a town that has none (ground " + count + " of " + candidates.size() + " tried took it)");
                    if (claim.isEmpty()) {
                        go(9000, 5);
                        return;
                    }
                    stake = claim.get().stake();
                    int[] spot = candidates.get(count - 1);
                    BlockPos centre = bounds(townB).getCenter();
                    Direction facing = Direction.getApproximateNearest(spot[0] - centre.getX(), 0, spot[1] - centre.getZ());
                    lotTurn = OldPlace.rotation(facing.getOpposite());
                    BlockPos groundAt = ground(spot[0], spot[1]).below();
                    lotOrigin = groundAt.subtract(StructureTemplate.transform(new BlockPos(OldPlaceBlueprint.GATE_X, 0, 0), Mirror.NONE, lotTurn, BlockPos.ZERO));
                    check(level.getBlockState(lot(OldPlaceBlueprint.STAKE.getX(), 1, 0)).is(ModBlocks.CLAIM_STAKE.get()), "the raised place's stake is where the script reckons the lot");
                    note("raised " + spot[2] + " blocks out from the town's edge, " + level.getEntitiesOfClass(Cow.class, new AABB(stake).inflate(40, 12, 40)).size() + " cows on it");
                    go(703, 20);
                    return;
                }
                int[] spot = candidates.get(count++);
                BlockPos centre = bounds(townB).getCenter();
                BlockPos groundAt = ground(spot[0], spot[1]).below();
                Vec3 at = Vec3.atBottomCenterOf(groundAt.above());
                Vec3 outward = new Vec3(spot[0] - centre.getX(), 0, spot[1] - centre.getZ()).normalize();
                stand(at.subtract(outward.scale(2)), at.add(outward.scale(10)));
                player.gameMode.useItemOn(player, level, player.getMainHandItem(), InteractionHand.MAIN_HAND, new BlockHitResult(Vec3.atCenterOf(groundAt), Direction.UP, groundAt, false));
                wait = 3;
            }
            case 703 -> {
                terrain();
                view(lotAt(OldPlaceBlueprint.GATE_X - 9, 6, -18), lotAt(OldPlaceBlueprint.GATE_X - 2, 4, 12), "hf_p12_raised_from_the_front.png");
                go(704, 5);
            }
            case 704 -> {
                view(lotAt(OldPlaceBlueprint.GATE_X, 34, -26), lotAt(OldPlaceBlueprint.GATE_X, 0, 14), "hf_p13_raised_by_its_town.png");
                go(705, 5);
            }
            case 705 -> {
                reach();
                go(9000, 10);
            }
            default -> {
                HollowFrontier.LOGGER.info("[Playtest] DONE: {} passed, {} failed", pass, fail);
                done = true;
                cmd("gamemode spectator @p");
                PhotoBooth.quit(mc, server);
            }
        }
    }

    private static final List<Animal> carried = new ArrayList<>();

    private static Vec3 centre(List<Animal> herd) {
        Vec3 sum = Vec3.ZERO;
        for (Animal a : herd) {
            sum = sum.add(a.position());
        }
        return herd.isEmpty() ? player.position() : sum.scale(1.0 / herd.size());
    }

    private static Vec3 driveDir(Drives.Drive drive) {
        return new Vec3(drive.dest().getX() - player.getX(), 0, drive.dest().getZ() - player.getZ()).normalize();
    }

    /** How the lot sits in the land: the ground just outside its fence against its own floor. */
    private static void terrain() {
        int floor = lot(0, 1, 0).getY();
        int worst = 0, steps = 0, n = 0;
        for (int x = -2; x <= OldPlaceBlueprint.WIDTH + 1; x++) {
            for (int z = -2; z <= OldPlaceBlueprint.DEPTH + 1; z++) {
                boolean rim = x == -2 || z == -2 || x == OldPlaceBlueprint.WIDTH + 1 || z == OldPlaceBlueprint.DEPTH + 1;
                if (rim) {
                    BlockPos at = lot(x, 0, z);
                    int rise = ground(at.getX(), at.getZ()).getY() - floor;
                    worst = Math.abs(rise) > Math.abs(worst) ? rise : worst;
                    steps += Math.abs(rise) > 1 ? 1 : 0;
                    n++;
                }
            }
        }
        note(String.format("the lot in the land: floor at y %d; of %d points just outside the fence, %d are more than a block off it, the worst %+d", floor, n, steps, worst));
    }

    /** Whether somebody on foot who opens doors can get from the gate to each part of the place. */
    private static void reach() {
        Villager walker = EntityTypes.VILLAGER.create(level, EntitySpawnReason.EVENT);
        if (walker == null) {
            return;
        }
        Vec3 gate = lotAt(OldPlaceBlueprint.GATE_X, 1, 3);
        walker.snapTo(gate.x, gate.y, gate.z, 0, 0);
        walker.setNoAi(true);
        walker.setOnGround(true);
        walker.getAttribute(Attributes.FOLLOW_RANGE).setBaseValue(96);
        level.addFreshEntity(walker);
        walker.getNavigation().setCanOpenDoors(true);
        Object[][] goals = {{"the front door", 9, 1, 9}, {"the parlour hearth", 5, 1, 13}, {"the kitchen table", 11, 1, 13}, {"the foot of the stairs", 9, 1, 17},
                {"the landing", 9, 6, 10}, {"the big bedroom", 6, 6, 15}, {"the small bedroom", 12, 6, 14}, {"the balcony", 9, 6, 6}, {"the back door, outside", 10, 1, 19},
                {"the barn floor", 26, 1, 10}, {"the corral gate", 26, 1, 19}, {"the garden gate", 9, 1, 20}, {"the outhouse door", 13, 1, 23}, {"the well", 28, 1, 3}};
        for (Object[] goal : goals) {
            BlockPos to = lot((int) goal[1], (int) goal[2], (int) goal[3]);
            Path path = walker.getNavigation().createPath(to, 0);
            boolean ok = path != null && path.canReach();
            check(ok, "on foot from the gate to " + goal[0] + (ok ? " (" + path.getNodeCount() + " steps)" : " (stopped at " + (path == null ? "the start" : path.getTarget() + " / " + path.getEndNode().asBlockPos()) + ", bound for " + to + ")"));
        }
        walker.discard();
    }
}
