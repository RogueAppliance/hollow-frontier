package com.hollowfrontier.content.frontier.mystery;

import java.util.Comparator;
import java.util.Optional;

import com.hollowfrontier.content.hollow.creature.GrayProbe;
import com.hollowfrontier.registry.ModBlocks;
import com.hollowfrontier.registry.ModEntities;
import com.hollowfrontier.registry.ModSounds;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.animal.cow.Cow;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.CropBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

/**
 * The frontier's mysteries (11a), as things that happen: airship-wave sightings, cattle mutilations and crop circles.
 * <ul>
 * <li>Now and then at night ({@link MysteryEvents}: every {@link #ROLL_INTERVAL} ticks a {@link #ROLL_CHANCE} roll, at
 *     most one a night) something happens near a player: a sighting half the time, otherwise a mutilation or a circle
 *     if there's a steer or a ripe wheat field within {@link #SEARCH} blocks (a sighting if there isn't).</li>
 * <li>Somebody awake, at a distance ({@link #VISIT_MIN}-{@link #VISIT_MAX} blocks), can watch it done: a Gray Probe
 *     comes down and works at it a while. That's the stake-out: get close unseen and bring it down mid-job. Further off
 *     it's simply done; and a night slept through may turn something up by morning ({@link #OVERNIGHT_CHANCE}).</li>
 * <li>Each is logged ({@link MysteryLog}) for the Gazette and the townsfolk, who grumble about it for a few days.</li>
 * </ul>
 */
public final class Mysteries {
    private Mysteries() {}

    public static final int ROLL_INTERVAL = 1200;
    public static final float ROLL_CHANCE = 0.06F;
    public static final float OVERNIGHT_CHANCE = 0.3F;
    public static final int SEARCH = 96;
    public static final int VISIT_MIN = 20, VISIT_MAX = 64;
    /** A probe at work hangs this high over its target. */
    public static final int VISIT_HEIGHT = 6;
    /** How far out from its middle a crop circle reaches (its outer ring, 7 blocks, and a little over). */
    public static final int CIRCLE_REACH = 8;
    /** How far round a spot {@link #middle} weighs the wheat: past the pattern, so it settles where the pattern fits. */
    static final int MIDDLE_REACH = 12;
    /** How long a mystery stays news, and how far off the Gazette still cares. */
    public static final long NEWS_WINDOW = 7 * 24000L;
    public static final double NEWS_RANGE = 2000;
    /** How long, and how near, the townsfolk grumble about one (Chatter's MUTILATIONS, CROP_CIRCLES and SIGHTINGS facts). */
    public static final long GRUMBLE_WINDOW = 3 * 24000L;
    public static final double GRUMBLE_RANGE = 400;

    /** A sighting over {@code over}: across the sky past it, or (hovering {@code hover} ticks) coming to hang right over it. */
    public static AirshipSighting sighting(ServerLevel level, Vec3 over, RandomSource random, int hover) {
        double angle = random.nextDouble() * Math.PI * 2;
        Vec3 direction = new Vec3(Math.cos(angle), 0, Math.sin(angle));
        double height = Math.min(over.y + 60 + random.nextInt(20), level.getMaxY() - 8);
        Vec3 start;
        if (hover > 0) {
            start = new Vec3(over.x, Math.min(over.y + 30, level.getMaxY() - 8), over.z);
        } else {
            Vec3 side = new Vec3(-direction.z, 0, direction.x).scale(random.nextInt(81) - 40);
            start = new Vec3(over.x, height, over.z).add(side).subtract(direction.scale(AirshipSighting.HALF_RUN));
        }
        AirshipSighting sighting = ModEntities.AIRSHIP_SIGHTING.get().create(level, EntitySpawnReason.EVENT);
        sighting.setPos(start);
        sighting.launch(direction, hover);
        level.addFreshEntity(sighting);
        MysteryLog.get(level).record(MysteryLog.Kind.SIGHTING, level.getGameTime(), BlockPos.containing(over));
        return sighting;
    }

    /** A grown steer between {@code min} and {@code max} blocks from {@code around}, the nearest first. */
    public static Optional<Cow> findCattle(ServerLevel level, BlockPos around, double min, double max) {
        return level.getEntitiesOfClass(Cow.class, new AABB(around).inflate(max), cow -> cow.isAlive() && !cow.isBaby()
                        && cow.distanceToSqr(Vec3.atCenterOf(around)) >= min * min && cow.distanceToSqr(Vec3.atCenterOf(around)) <= max * max)
                .stream().min(Comparator.comparingDouble(cow -> cow.distanceToSqr(Vec3.atCenterOf(around))));
    }

    private static boolean ripeWheat(BlockState state) {
        return state.is(Blocks.WHEAT) && state.getValue(CropBlock.AGE) >= CropBlock.MAX_AGE;
    }

    /**
     * The middle of a ripe wheat field (a spot with 20 ripe stalks within four blocks of it) between {@code min} and
     * {@code max} blocks from {@code around}, the nearest first, moved to the middle of the wheat there ({@link #middle})
     * so a field big enough takes the whole circle.
     */
    public static Optional<BlockPos> findWheat(ServerLevel level, BlockPos around, double min, double max) {
        BlockPos best = null;
        double bestDist = Double.MAX_VALUE;
        int r = (int) max;
        for (int dx = -r; dx <= r; dx += 3) {
            for (int dz = -r; dz <= r; dz += 3) {
                double d = Math.hypot(dx, dz);
                if (d < min || d > max || d >= bestDist) {
                    continue;
                }
                int x = around.getX() + dx, z = around.getZ() + dz;
                if (!level.hasChunk(x >> 4, z >> 4)) {
                    continue;
                }
                BlockPos top = wheatNear(level, x, around.getY(), z);
                if (top != null && ripeAround(level, top) >= 20) {
                    best = top;
                    bestDist = d;
                }
            }
        }
        return Optional.ofNullable(best).map(spot -> middle(level, spot));
    }

    /**
     * Walks {@code spot} to the middle of the ripe wheat round it: the average of the stalks a little further out than the
     * pattern reaches ({@link #MIDDLE_REACH}), again from there, until it settles. A field the circle fits is inside that
     * window after a step, so it lands dead in the middle; in a bigger one it moves in from the edge until it's at least
     * that far in, room for the whole pattern.
     */
    static BlockPos middle(ServerLevel level, BlockPos spot) {
        BlockPos at = spot;
        for (int i = 0; i < 8; i++) {
            long sumX = 0, sumZ = 0, sumY = 0, n = 0;
            for (BlockPos pos : BlockPos.betweenClosed(at.offset(-MIDDLE_REACH, -1, -MIDDLE_REACH), at.offset(MIDDLE_REACH, 1, MIDDLE_REACH))) {
                if (ripeWheat(level.getBlockState(pos))) {
                    sumX += pos.getX();
                    sumY += pos.getY();
                    sumZ += pos.getZ();
                    n++;
                }
            }
            if (n == 0) {
                return at;
            }
            BlockPos next = new BlockPos((int) Math.round((double) sumX / n), (int) Math.round((double) sumY / n), (int) Math.round((double) sumZ / n));
            if (next.equals(at)) {
                break;
            }
            at = next;
        }
        return at;
    }

    /** The topmost ripe wheat in a column, from 24 over {@code y} to 24 under (a roof or a tree over the field is no matter), or null. */
    private static @Nullable BlockPos wheatNear(ServerLevel level, int x, int y, int z) {
        BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos(x, y + 24, z);
        for (int i = 0; i < 48; i++, pos.move(0, -1, 0)) {
            if (ripeWheat(level.getBlockState(pos))) {
                return pos.immutable();
            }
        }
        return null;
    }

    private static int ripeAround(ServerLevel level, BlockPos middle) {
        int count = 0;
        for (BlockPos pos : BlockPos.betweenClosed(middle.offset(-4, -1, -4), middle.offset(4, 1, 4))) {
            count += ripeWheat(level.getBlockState(pos)) ? 1 : 0;
        }
        return count;
    }

    /** Leaves a carcass where the steer stood, in a ring of scorched grass; the steer's gone. */
    public static CattleCarcass mutilate(ServerLevel level, Cow cow) {
        CattleCarcass carcass = ModEntities.CATTLE_CARCASS.get().create(level, EntitySpawnReason.EVENT);
        carcass.snapTo(cow.getX(), cow.getY(), cow.getZ(), cow.getYRot(), 0.0F);
        level.addFreshEntity(carcass);
        BlockPos at = cow.blockPosition();
        for (int dx = -4; dx <= 4; dx++) {
            for (int dz = -4; dz <= 4; dz++) {
                double d = Math.hypot(dx, dz);
                if (d < 2.5 || d > 3.5) {
                    continue;
                }
                for (int dy = 1; dy >= -2; dy--) {
                    BlockPos pos = at.offset(dx, dy, dz);
                    if (level.getBlockState(pos).is(Blocks.GRASS_BLOCK)) {
                        level.setBlockAndUpdate(pos, Blocks.COARSE_DIRT.defaultBlockState());
                        break;
                    }
                }
            }
        }
        cow.discard();
        MysteryLog.get(level).record(MysteryLog.Kind.MUTILATION, level.getGameTime(), at);
        return carcass;
    }

    /**
     * Presses a crop circle into the wheat round {@code middle}: a disc in the centre, a ring four out and another seven
     * out, joined by a spoke to the east, flattening whatever wheat lies in the pattern. Returns how many stalks it laid.
     */
    public static int cropCircle(ServerLevel level, BlockPos middle) {
        int laid = 0;
        for (int dx = -CIRCLE_REACH; dx <= CIRCLE_REACH; dx++) {
            for (int dz = -CIRCLE_REACH; dz <= CIRCLE_REACH; dz++) {
                if (!inPattern(dx, dz)) {
                    continue;
                }
                for (int dy = -2; dy <= 2; dy++) {
                    BlockPos pos = middle.offset(dx, dy, dz);
                    if (level.getBlockState(pos).is(Blocks.WHEAT)) {
                        level.setBlock(pos, ModBlocks.FLATTENED_WHEAT.get().defaultBlockState(), 3);
                        laid++;
                        break;
                    }
                }
            }
        }
        if (laid > 0) {
            MysteryLog.get(level).record(MysteryLog.Kind.CROP_CIRCLE, level.getGameTime(), middle);
        }
        return laid;
    }

    /** Whether a spot {@code dx}, {@code dz} from a circle's middle is in its pattern: the disc, the two rings, the spoke. */
    public static boolean inPattern(int dx, int dz) {
        double d = Math.hypot(dx, dz);
        return d <= 1.6 || Math.abs(d - 4) < 0.55 || Math.abs(d - 7) < 0.55 || (dz == 0 && dx > 4 && dx < 7);
    }

    /** Sends a probe down to do the job: mutilate {@code cow}, or press a circle at {@code pos}. */
    public static GrayProbe visit(ServerLevel level, MysteryLog.Kind kind, BlockPos pos, @Nullable Cow cow) {
        GrayProbe probe = ModEntities.GRAY_PROBE.get().create(level, EntitySpawnReason.EVENT);
        probe.snapTo(pos.getX() + 0.5, pos.getY() + 28, pos.getZ() + 0.5, 0.0F, 0.0F);
        probe.giveJob(kind, pos, cow);
        level.addFreshEntity(probe);
        level.playSound(null, pos.getX(), pos.getY() + 20, pos.getZ(), ModSounds.GRAY_PROBE_HUM.get(), SoundSource.HOSTILE, 4.0F, 0.6F);
        return probe;
    }

    /**
     * Makes something happen near {@code player}: a sighting, or a mutilation or crop circle where there's a steer or ripe
     * wheat about (watched being done, if they're awake and at the right distance). Returns whether anything did.
     */
    public static boolean happen(ServerLevel level, ServerPlayer player, MysteryLog.Kind kind, RandomSource random, boolean awake) {
        BlockPos here = player.blockPosition();
        switch (kind) {
            case SIGHTING -> {
                sighting(level, player.position(), random, 0);
                return true;
            }
            case MUTILATION -> {
                Optional<Cow> cow = findCattle(level, here, awake ? VISIT_MIN : 0, SEARCH);
                if (cow.isEmpty()) {
                    return false;
                }
                if (awake && cow.get().distanceTo(player) <= VISIT_MAX) {
                    visit(level, kind, cow.get().blockPosition(), cow.get());
                } else {
                    mutilate(level, cow.get());
                }
                return true;
            }
            case CROP_CIRCLE -> {
                Optional<BlockPos> field = findWheat(level, here, awake ? VISIT_MIN : 0, SEARCH);
                if (field.isEmpty()) {
                    return false;
                }
                if (awake && Math.sqrt(field.get().distSqr(here)) <= VISIT_MAX) {
                    visit(level, kind, field.get(), null);
                } else {
                    cropCircle(level, field.get());
                }
                return true;
            }
        }
        return false;
    }

    /** Picks what happens tonight: a sighting half the time, a mutilation or a circle the rest. */
    public static MysteryLog.Kind pick(RandomSource random) {
        float roll = random.nextFloat();
        return roll < 0.5F ? MysteryLog.Kind.SIGHTING : roll < 0.75F ? MysteryLog.Kind.MUTILATION : MysteryLog.Kind.CROP_CIRCLE;
    }

    /** The newest mystery within news range of {@code pos} this week, for the Gazette. */
    public static Optional<MysteryLog.Sign> news(ServerLevel level, BlockPos pos) {
        return MysteryLog.get(level).latest(null, pos, NEWS_RANGE, level.getGameTime(), NEWS_WINDOW);
    }

    /** Days since a sign, at least one (yesterday). */
    public static int daysAgo(ServerLevel level, MysteryLog.Sign sign) {
        return Math.max(1, Mth.ceil((level.getGameTime() - sign.time()) / 24000.0));
    }
}
