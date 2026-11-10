package com.hollowfrontier.content.hollow.sea;

import java.util.List;
import java.util.Optional;

import com.hollowfrontier.HollowFrontier;
import com.hollowfrontier.registry.ModEntities;

import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.FluidTags;
import net.minecraft.tags.TagKey;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.storage.loot.LootTable;
import net.minecraft.world.phys.AABB;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.tick.LevelTickEvent;

/**
 * The Still Sea's own creatures (43b), and where they come from. Like the dust devils they have their own spawner rather
 * than biome spawn entries: under the crust vanilla's spawning picks its heights from a heightmap that is the roof of the
 * world, so water 250 blocks below it is found too rarely to stock a sea, and each kind wants its own depth besides.
 * <p>
 * Every {@link #INTERVAL} ticks, for each player in a biome tagged {@code has_sea_life}, one spot of water 24 to 56 blocks
 * off is tried for one kind, picked by weight: a herd of Great Sea Cows in the shallows, a shoal of Lampjellies anywhere,
 * a Plesiosaur or two in the middle water, a lone Basilosaurus in the deep. A kind already at its {@link Kind#crowd} within
 * {@link #CROWD_RANGE} blocks of the player is skipped. Never within sight-range of the player's own position (24 blocks).
 */
@EventBusSubscriber(modid = HollowFrontier.MODID)
public final class SeaLife {
    private SeaLife() {}

    public static final TagKey<Biome> HAS_SEA_LIFE = TagKey.create(Registries.BIOME, HollowFrontier.id("has_sea_life"));
    /** What a Plesiosaur brings up from the bed. */
    public static final ResourceKey<LootTable> DIVE_LOOT = ResourceKey.create(Registries.LOOT_TABLE, HollowFrontier.id("gameplay/plesiosaur_dive"));
    public static final int INTERVAL = 100;
    public static final int NEAR_MIN = 24, NEAR_MAX = 56;
    public static final double CROWD_RANGE = 96;

    /** A kind of sea creature: how deep the water must be where it appears, how many come at once, and how many are enough. */
    public enum Kind {
        SEA_COW(30, 4, 14, 2, 4, 6, 2, 5),
        LAMPJELLY(40, 4, 64, 3, 6, 12, 3, -1),
        PLESIOSAUR(20, 10, 64, 1, 2, 2, 4, -1),
        BASILOSAURUS(10, 18, 64, 1, 1, 1, 8, 10);

        public final int weight, minDepth, maxDepth, minGroup, maxGroup, crowd;
        /** Where in the water it keeps: at least this far under the surface, and (if not negative) no more than this over the bed. */
        public final int minBelowSurface, maxAboveBed;

        Kind(int weight, int minDepth, int maxDepth, int minGroup, int maxGroup, int crowd, int minBelowSurface, int maxAboveBed) {
            this.minBelowSurface = minBelowSurface;
            this.maxAboveBed = maxAboveBed;
            this.weight = weight;
            this.minDepth = minDepth;
            this.maxDepth = maxDepth;
            this.minGroup = minGroup;
            this.maxGroup = maxGroup;
            this.crowd = crowd;
        }

        public EntityType<? extends Mob> type() {
            return switch (this) {
                case SEA_COW -> ModEntities.GREAT_SEA_COW.get();
                case LAMPJELLY -> ModEntities.LAMPJELLY.get();
                case PLESIOSAUR -> ModEntities.PLESIOSAUR.get();
                case BASILOSAURUS -> ModEntities.BASILOSAURUS.get();
            };
        }

        /** Whether water {@code depth} blocks deep (surface to bed) suits it. */
        public boolean suits(int depth) {
            return depth >= minDepth && depth <= maxDepth;
        }

        static Kind pick(RandomSource random) {
            int total = 0;
            for (Kind kind : values()) {
                total += kind.weight;
            }
            int roll = random.nextInt(total);
            for (Kind kind : values()) {
                roll -= kind.weight;
                if (roll < 0) {
                    return kind;
                }
            }
            return LAMPJELLY;
        }
    }

    @SubscribeEvent
    static void onLevelTick(LevelTickEvent.Post event) {
        if (!(event.getLevel() instanceof ServerLevel level) || level.getGameTime() % INTERVAL != 0) {
            return;
        }
        RandomSource random = level.getRandom();
        for (ServerPlayer player : level.players()) {
            if (player.isSpectator() || !level.getBiome(player.blockPosition()).is(HAS_SEA_LIFE)) {
                continue;
            }
            double angle = random.nextDouble() * Math.PI * 2, distance = NEAR_MIN + random.nextDouble() * (NEAR_MAX - NEAR_MIN);
            int x = Mth.floor(player.getX() + Math.cos(angle) * distance), z = Mth.floor(player.getZ() + Math.sin(angle) * distance);
            if (level.hasChunk(x >> 4, z >> 4)) {
                stock(level, new BlockPos(x, level.getSeaLevel() - 1, z), Kind.pick(random), player.getBoundingBox().inflate(CROWD_RANGE), random);
            }
        }
    }

    /**
     * Puts a group of {@code kind} in the water under {@code surface} (the top block of water there), if the biome, the
     * depth and the numbers already within {@code crowdBox} allow. Returns what it spawned.
     */
    public static List<Mob> stock(ServerLevel level, BlockPos surface, Kind kind, AABB crowdBox, RandomSource random) {
        return level.getBiome(surface).is(HAS_SEA_LIFE) ? place(level, surface, kind, crowdBox, random) : List.of();
    }

    /** {@link #stock} without the biome check (the tests' water has no biome of ours). */
    public static List<Mob> place(ServerLevel level, BlockPos surface, Kind kind, AABB crowdBox, RandomSource random) {
        if (!isWater(level, surface) || isWater(level, surface.above())) {
            return List.of();
        }
        int depth = depthBelow(level, surface, kind.maxDepth + 1);
        if (!kind.suits(depth) || level.getEntities(kind.type(), crowdBox, e -> e.isAlive()).size() >= kind.crowd) {
            return List.of();
        }
        int count = kind.minGroup + random.nextInt(kind.maxGroup - kind.minGroup + 1);
        List<Mob> spawned = new java.util.ArrayList<>();
        for (int i = 0; i < count; i++) {
            // A few blocks apart, each at its own kind of depth (SwimAtDepthGoal keeps it there afterwards)
            net.minecraft.world.phys.Vec3 spot = SwimAtDepthGoal.atDepth(level, surface.offset(random.nextInt(7) - 3, 0, random.nextInt(7) - 3),
                    kind.minBelowSurface, kind.maxAboveBed, 0.0F, random);
            if (spot == null) {
                continue;
            }
            BlockPos at = BlockPos.containing(spot);
            if (!isWater(level, at) || !isWater(level, at.above())) {
                continue;
            }
            Mob mob = kind.type().create(level, EntitySpawnReason.NATURAL);
            if (mob == null) {
                continue;
            }
            mob.snapTo(at.getX() + 0.5, at.getY(), at.getZ() + 0.5, random.nextFloat() * 360.0F, 0.0F);
            if (!level.noCollision(mob)) {
                continue; // a big one in a tight spot (kelp doesn't count, rock does)
            }
            mob.finalizeSpawn(level, level.getCurrentDifficultyAt(at), EntitySpawnReason.NATURAL, null);
            level.addFreshEntityWithPassengers(mob);
            spawned.add(mob);
        }
        return spawned;
    }

    public static boolean isWater(LevelReader level, BlockPos pos) {
        return level.getFluidState(pos).is(FluidTags.WATER) && !level.getBlockState(pos).isSolid();
    }

    /** How many blocks of water lie at and under {@code pos}, down to the bed, counted up to {@code limit}. */
    public static int depthBelow(LevelReader level, BlockPos pos, int limit) {
        BlockPos.MutableBlockPos at = pos.mutable();
        int depth = 0;
        while (depth < limit && isWater(level, at)) {
            depth++;
            at.move(0, -1, 0);
        }
        return depth;
    }

    /** The lowest water over the bed under {@code pos} (itself, if it isn't in water). */
    public static BlockPos bedBelow(LevelReader level, BlockPos pos) {
        BlockPos.MutableBlockPos at = pos.mutable();
        while (at.getY() > level.getMinY() + 1 && isWater(level, at.below())) {
            at.move(0, -1, 0);
        }
        return at.immutable();
    }

    /** The whole column of water through {@code pos}: from the bed up to the surface, however deep {@code pos} is in it. */
    public static int columnDepth(LevelReader level, BlockPos pos) {
        if (!isWater(level, pos)) {
            return 0;
        }
        BlockPos.MutableBlockPos top = pos.mutable();
        int up = 0;
        while (up < 64 && isWater(level, top.above())) {
            top.move(0, 1, 0);
            up++;
        }
        return depthBelow(level, top, 96);
    }

    /** Spawn rule for the sea's creatures (spawn eggs and commands aside, {@link #stock} places them): in water. */
    public static boolean inWater(EntityType<? extends Mob> type, LevelAccessor level, EntitySpawnReason reason, BlockPos pos, RandomSource random) {
        return isWater(level, pos);
    }

    /**
     * Where a Plesiosaur looks when it surfaces with something: the Drowned Quarter (43d), once there is one to find.
     * Until then, nowhere in particular (it looks at whoever fed it).
     */
    public static Optional<BlockPos> quarterBearing(ServerLevel level, BlockPos from) {
        return Optional.empty();
    }
}
