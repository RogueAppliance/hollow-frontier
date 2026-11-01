package com.hollowfrontier.content.frontier.cat;

import java.util.EnumSet;

import org.jspecify.annotations.Nullable;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.SectionPos;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.animal.feline.Cat;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.CampfireBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.phys.Vec3;

/**
 * The evening gathering: from dusk until midnight, town cats drift over to a lit campfire nearby (every town street has a
 * fire pit at its landmark end) and loaf around it, a couple of blocks out, until the night wears on.
 */
public class CatHearthGoal extends Goal {
    private static final int SEARCH = 40;
    // Ticks a cat waits before looking for a fire again after finding none.
    private static final int RETRY = 2400;

    private final Cat cat;
    private @Nullable BlockPos hearth;
    private @Nullable BlockPos spot;
    private int nextSearch;
    private int ticks;
    private boolean resting;

    public CatHearthGoal(Cat cat) {
        this.cat = cat;
        setFlags(EnumSet.of(Flag.MOVE, Flag.JUMP, Flag.LOOK));
    }

    /** Dusk to midnight (the time of day the fire draws them). */
    public static boolean isEvening(Level level) {
        long time = level.getDefaultClockTime() % 24000L;
        return time >= 11800L && time < 18000L;
    }

    static boolean isLitFire(BlockState state) {
        return state.getBlock() instanceof CampfireBlock && state.getValue(CampfireBlock.LIT);
    }

    @Override
    public boolean canUse() {
        if (!TownCat.isTownCat(cat) || TownCat.of(cat).ghost() || !isEvening(cat.level()) || cat.getRandom().nextInt(reducedTickDelay(100)) != 0) {
            return false;
        }
        if (hearth == null || !isLitFire(cat.level().getBlockState(hearth))) {
            hearth = null;
            if (cat.tickCount < nextSearch) {
                return false;
            }
            nextSearch = cat.tickCount + RETRY;
            hearth = findFire(cat.level(), cat.blockPosition());
        }
        if (hearth == null) {
            return false;
        }
        spot = pickSpot();
        return spot != null;
    }

    /**
     * The nearest lit campfire within {@link #SEARCH} blocks across and 4 up or down. Campfires are block entities, so only
     * the loaded chunks' block entities need a look: walking every block in range read some 59,000 states a search, and
     * could load chunks past the edge of the ticking area.
     */
    public static @Nullable BlockPos findFire(Level level, BlockPos origin) {
        BlockPos found = null;
        double best = Double.MAX_VALUE;
        for (int cx = SectionPos.blockToSectionCoord(origin.getX() - SEARCH); cx <= SectionPos.blockToSectionCoord(origin.getX() + SEARCH); cx++) {
            for (int cz = SectionPos.blockToSectionCoord(origin.getZ() - SEARCH); cz <= SectionPos.blockToSectionCoord(origin.getZ() + SEARCH); cz++) {
                LevelChunk chunk = level.getChunkSource().getChunkNow(cx, cz);
                if (chunk == null) {
                    continue;
                }
                for (BlockPos pos : chunk.getBlockEntities().keySet()) {
                    if (Math.abs(pos.getX() - origin.getX()) <= SEARCH && Math.abs(pos.getZ() - origin.getZ()) <= SEARCH
                            && Math.abs(pos.getY() - origin.getY()) <= 4 && pos.distSqr(origin) < best && isLitFire(level.getBlockState(pos))) {
                        best = pos.distSqr(origin);
                        found = pos.immutable();
                    }
                }
            }
        }
        return found;
    }

    // A free patch of ground two or so blocks from the fire, at a random angle around it.
    private @Nullable BlockPos pickSpot() {
        Level level = cat.level();
        for (int attempt = 0; attempt < 8; attempt++) {
            float angle = cat.getRandom().nextFloat() * Mth.TWO_PI;
            float radius = 1.8F + cat.getRandom().nextFloat() * 0.9F;
            BlockPos pos = BlockPos.containing(hearth.getX() + 0.5 + Mth.cos(angle) * radius, hearth.getY(), hearth.getZ() + 0.5 + Mth.sin(angle) * radius);
            for (int dy = 1; dy >= -1; dy--) {
                BlockPos at = pos.above(dy);
                if (level.getBlockState(at).getCollisionShape(level, at).isEmpty() && level.getBlockState(at.above()).isAir()
                        && level.getBlockState(at.below()).isFaceSturdy(level, at.below(), Direction.UP)) {
                    return at;
                }
            }
        }
        return null;
    }

    @Override
    public void start() {
        ticks = 0;
        resting = false;
        cat.setInSittingPose(false);
        cat.getNavigation().moveTo(spot.getX() + 0.5, spot.getY(), spot.getZ() + 0.5, 0.9);
    }

    @Override
    public boolean canContinueToUse() {
        return spot != null && hearth != null && TownCat.isTownCat(cat) && isEvening(cat.level()) && isLitFire(cat.level().getBlockState(hearth))
                && cat.getLastHurtByMob() == null && (resting || ticks < 400);
    }

    @Override
    public boolean requiresUpdateEveryTick() {
        return true;
    }

    @Override
    public void tick() {
        ticks++;
        if (resting) {
            return;
        }
        if (cat.position().closerThan(Vec3.atBottomCenterOf(spot), 1.0)) {
            resting = true;
            cat.getNavigation().stop();
            cat.getLookControl().setLookAt(hearth.getX() + 0.5, hearth.getY() + 0.3, hearth.getZ() + 0.5);
            // Most loaf by the fire asleep; a few sit up and watch it.
            if (cat.getRandom().nextInt(3) == 0) {
                cat.setInSittingPose(true);
            } else {
                cat.setLying(true);
            }
        } else if (ticks % 40 == 0) {
            cat.getNavigation().moveTo(spot.getX() + 0.5, spot.getY(), spot.getZ() + 0.5, 0.9);
        }
    }

    @Override
    public void stop() {
        cat.setLying(false);
        cat.setInSittingPose(false);
        spot = null;
    }
}
