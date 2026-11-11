package com.hollowfrontier.content.frontier.season;

import java.util.List;
import java.util.Optional;

import com.geckolib.animatable.GeoEntity;
import com.geckolib.animatable.instance.AnimatableInstanceCache;
import com.geckolib.animatable.manager.AnimatableManager;
import com.geckolib.util.GeckoLibUtil;
import com.hollowfrontier.content.frontier.raid.TownSite;
import com.hollowfrontier.registry.ModEntities;

import net.minecraft.core.BlockPos;

import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.level.levelgen.structure.StructureStart;
import net.minecraft.world.level.material.PushReaction;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.AABB;

/**
 * A town's December tree (35b): seven blocks of spruce in a tub, hung with tin stars and candles that are lit after dark.
 * It's an entity, not blocks, so it can't damage a town or a player's build, and there's nothing to clean up: it takes
 * itself away once the month is over. It can't be hit, pushed, walked into, caught or sent anywhere.
 * {@link #ensure} stands one up in a town's square while a player is in a December town and none is there.
 */
public class TownTree extends Entity implements GeoEntity {
    /** How far from the middle of the square a tree may stand, and how near one counts as the town's. */
    public static final int SEARCH = 16, RANGE = 64;
    private static final EntityDataAccessor<Boolean> DATA_LIT = SynchedEntityData.defineId(TownTree.class, EntityDataSerializers.BOOLEAN);

    private final AnimatableInstanceCache geoCache = GeckoLibUtil.createInstanceCache(this);

    public TownTree(EntityType<? extends TownTree> type, Level level) {
        super(type, level);
        noPhysics = true;
        setNoGravity(true);
    }

    /** Candles lit: after dark. */
    public boolean isLit() {
        return entityData.get(DATA_LIT);
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        builder.define(DATA_LIT, false);
    }

    @Override
    public void tick() {
        super.tick();
        if (level() instanceof ServerLevel level && (tickCount + getId()) % 40 == 0) {
            keep(level);
        }
    }

    /** The tree's own housekeeping, every two seconds (public so game tests can drive it): gone out of season, lit after dark. */
    public void keep(ServerLevel level) {
        if (outOfSeason(level)) {
            discard();
            return;
        }
        // Two players walking in from either end can each stand one up: the younger steps aside
        for (TownTree other : level.getEntitiesOfClass(TownTree.class, getBoundingBox().inflate(RANGE), t -> t != this && t.isAlive())) {
            if (other.getUUID().compareTo(getUUID()) < 0) {
                discard();
                return;
            }
        }
        boolean lit = level.isDarkOutside();
        if (isLit() != lit) {
            entityData.set(DATA_LIT, lit);
        }
    }

    /** No tree outside December, or anywhere but the Overworld. */
    public static boolean outOfSeason(ServerLevel level) {
        return level.dimension() != Level.OVERWORLD || !Season.isDecember(level);
    }

    /**
     * Stands a tree up in the square of the town the player is in, if it's December and the town has none. The tree goes
     * on the nearest clear, level ground to the middle of the square. Returns the tree if one was put up.
     */
    public static Optional<TownTree> ensure(ServerLevel level, ServerPlayer player) {
        if (outOfSeason(level)) {
            return Optional.empty();
        }
        Optional<StructureStart> town = TownSite.townAt(level, player.blockPosition());
        if (town.isEmpty()) {
            return Optional.empty();
        }
        BoundingBox square = town.get().getPieces().getFirst().getBoundingBox();
        return ensure(level, new BlockPos(square.getCenter().getX(), square.minY() + 1, square.getCenter().getZ()));
    }

    /** As above, round a known middle of a square (game tests and the photo booth have no structure start). */
    public static Optional<TownTree> ensure(ServerLevel level, BlockPos rally) {
        if (outOfSeason(level) || !level.isLoaded(rally)) {
            return Optional.empty();
        }
        List<TownTree> standing = level.getEntitiesOfClass(TownTree.class, new AABB(rally).inflate(RANGE), Entity::isAlive);
        if (!standing.isEmpty()) {
            return Optional.empty();
        }
        Optional<BlockPos> spot = clearGround(level, rally);
        if (spot.isEmpty()) {
            return Optional.empty();
        }
        TownTree tree = ModEntities.TOWN_TREE.get().create(level, EntitySpawnReason.EVENT);
        if (tree == null) {
            return Optional.empty();
        }
        BlockPos at = spot.get();
        tree.snapTo(at.getX() + 0.5, at.getY(), at.getZ() + 0.5, 0.0F, 0.0F);
        tree.entityData.set(DATA_LIT, level.isDarkOutside());
        return level.addFreshEntity(tree) ? Optional.of(tree) : Optional.empty();
    }

    // The nearest spot to the middle with level ground five blocks square under it and open sky over it: the lowest boughs
    // reach nearly two blocks out, and a tree hard against a bandstand grew through its posts (the first photo)
    private static Optional<BlockPos> clearGround(ServerLevel level, BlockPos rally) {
        BlockPos best = null;
        double bestDistance = Double.MAX_VALUE;
        for (int dx = -SEARCH; dx <= SEARCH; dx++) {
            for (int dz = -SEARCH; dz <= SEARCH; dz++) {
                double distance = dx * dx + dz * dz;
                if (distance >= bestDistance) {
                    continue;
                }
                BlockPos column = rally.offset(dx, 0, dz);
                if (!level.isLoaded(column)) {
                    continue;
                }
                BlockPos ground = level.getHeightmapPos(Heightmap.Types.MOTION_BLOCKING, column);
                if (Math.abs(ground.getY() - rally.getY()) <= 3 && clear(level, ground)) {
                    best = ground;
                    bestDistance = distance;
                }
            }
        }
        return Optional.ofNullable(best);
    }

    private static boolean clear(ServerLevel level, BlockPos ground) {
        for (int dx = -2; dx <= 2; dx++) {
            for (int dz = -2; dz <= 2; dz++) {
                BlockPos at = ground.offset(dx, 0, dz);
                BlockState below = level.getBlockState(at.below());
                if (level.getHeightmapPos(Heightmap.Types.MOTION_BLOCKING, at).getY() != ground.getY()
                        || !ground(level, at.below(), below) || !level.getFluidState(at).isEmpty()
                        || !level.getBlockState(at).canBeReplaced()) {
                    return false;
                }
            }
        }
        return true;
    }

    // Something to stand on: earth, paving, planks, a path (which isn't a full block, so "sturdy" would turn a street down).
    // Not a treetop or a roof of glass.
    private static boolean ground(ServerLevel level, BlockPos pos, BlockState below) {
        return !below.getCollisionShape(level, pos).isEmpty() && !below.is(net.minecraft.tags.BlockTags.LEAVES)
                && !below.is(net.neoforged.neoforge.common.Tags.BlockItems.GLASS_BLOCKS.block());
    }

    @Override
    public boolean hurtServer(ServerLevel level, DamageSource source, float amount) {
        return false;
    }

    @Override
    public boolean isPickable() {
        return false;
    }

    @Override
    public boolean isPushable() {
        return false;
    }

    @Override
    public boolean canBeCollidedWith(Entity other) {
        return false;
    }

    @Override
    public boolean isAttackable() {
        return false;
    }

    @Override
    public PushReaction getPistonPushReaction() {
        return PushReaction.IGNORE_ENTITY;
    }

    @Override
    public boolean isPushedByFluid() {
        return false;
    }

    @Override
    protected void readAdditionalSaveData(ValueInput input) {}

    @Override
    protected void addAdditionalSaveData(ValueOutput output) {}

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {}

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return geoCache;
    }
}
