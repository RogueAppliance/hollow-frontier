package com.hollowfrontier.content.frontier.cat;

import java.util.EnumSet;

import org.jspecify.annotations.Nullable;

import com.hollowfrontier.content.frontier.music.SaloonPianoBlock;
import com.hollowfrontier.registry.ModBlocks;

import net.minecraft.core.BlockPos;
import net.minecraft.tags.BlockTags;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.animal.feline.Cat;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

/**
 * A town cat picks a favourite perch nearby and settles on it for a while: the top of a playing Saloon Piano best of all,
 * then the bar counter, hay bales, hitching rails (any wooden fence), barrels and troughs (water cauldrons). On the bar or the hay it may
 * curl up asleep; elsewhere it sits. Perches a cat can't walk onto (fences, cauldrons) it hops up onto from beside them.
 * Bar stools are left for people.
 */
public class CatLoungeGoal extends Goal {
    private static final int SEARCH = 10;
    private static final int GIVE_UP = 240;

    private final Cat cat;
    private @Nullable BlockPos perch;
    private boolean settled;
    private int ticksLeft;
    private int travelTicks;
    private int hopCooldown;

    public CatLoungeGoal(Cat cat) {
        this.cat = cat;
        setFlags(EnumSet.of(Flag.MOVE, Flag.JUMP));
    }

    /** How much a cat likes perching on this block (0 = not at all). */
    static int appeal(Level level, BlockPos pos) {
        BlockState state = level.getBlockState(pos);
        if (state.is(ModBlocks.SALOON_PIANO.get())) {
            return state.getValue(SaloonPianoBlock.PLAYING) ? 10 : 0;
        }
        if (state.is(ModBlocks.BAR_COUNTER.get())) return 6;
        if (state.is(Blocks.HAY_BLOCK)) return 5;
        if (state.is(BlockTags.WOODEN_FENCES)) return 4;
        if (state.is(Blocks.BARREL) || state.is(Blocks.WATER_CAULDRON)) return 3;
        return 0;
    }

    @Override
    public boolean canUse() {
        if (!TownCat.isTownCat(cat) || TownCat.of(cat).faded() || cat.isInSittingPose() || cat.isLying() || cat.getRandom().nextInt(reducedTickDelay(200)) != 0) {
            return false;
        }
        perch = findPerch();
        return perch != null;
    }

    private @Nullable BlockPos findPerch() {
        Level level = cat.level();
        BlockPos origin = cat.blockPosition();
        BlockPos best = null;
        float bestScore = 0;
        for (BlockPos pos : BlockPos.betweenClosed(origin.offset(-SEARCH, -2, -SEARCH), origin.offset(SEARCH, 2, SEARCH))) {
            int appeal = appeal(level, pos);
            if (appeal == 0 || !level.getBlockState(pos.above()).getCollisionShape(level, pos.above()).isEmpty()
                    || (cat.hasHome() && !cat.isWithinHome(pos))) {
                continue;
            }
            // Closer is better, with a little whim so cats don't all pick the same perch.
            float score = appeal - (float) Math.sqrt(pos.distSqr(origin)) * 0.3F + cat.getRandom().nextFloat() * 2.0F;
            if (score > bestScore && !taken(pos)) {
                bestScore = score;
                best = pos.immutable();
            }
        }
        return best;
    }

    private boolean taken(BlockPos pos) {
        return !cat.level().getEntitiesOfClass(Cat.class, new AABB(pos.above()), other -> other != cat).isEmpty();
    }

    @Override
    public void start() {
        settled = false;
        travelTicks = 0;
        hopCooldown = 0;
        ticksLeft = adjustedTickDelay(600 + cat.getRandom().nextInt(1200));
        moveToPerch();
    }

    private void moveToPerch() {
        cat.getNavigation().moveTo(perch.getX() + 0.5, perch.getY() + 1, perch.getZ() + 0.5, 1.0);
    }

    private boolean onPerch() {
        return cat.blockPosition().equals(perch.above()) && cat.onGround();
    }

    @Override
    public boolean canContinueToUse() {
        if (perch == null || !TownCat.isTownCat(cat) || TownCat.of(cat).faded() || cat.getLastHurtByMob() != null || appeal(cat.level(), perch) == 0) {
            return false;
        }
        return settled ? ticksLeft > 0 && cat.blockPosition().equals(perch.above()) : travelTicks < GIVE_UP;
    }

    @Override
    public boolean requiresUpdateEveryTick() {
        return true;
    }

    @Override
    public void tick() {
        if (settled) {
            ticksLeft--;
            return;
        }
        travelTicks++;
        hopCooldown--;
        if (onPerch()) {
            settle();
            return;
        }
        Vec3 centre = Vec3.atBottomCenterOf(perch.above());
        double dx = centre.x - cat.getX(), dz = centre.z - cat.getZ();
        double flat = Math.sqrt(dx * dx + dz * dz);
        if (flat < 1.7 && cat.onGround() && cat.getY() < perch.getY() + 0.9 && hopCooldown <= 0) {
            // Beside a perch it can't path onto (a rail, a trough): hop up.
            cat.getNavigation().stop();
            cat.setDeltaMovement(dx / Math.max(flat, 0.1) * Mth.clamp(flat * 0.16, 0.06, 0.24), 0.52, dz / Math.max(flat, 0.1) * Mth.clamp(flat * 0.16, 0.06, 0.24));
            cat.needsSync = true;
            hopCooldown = 20;
        } else if (travelTicks % 40 == 0 || cat.getNavigation().isDone() && hopCooldown <= 0) {
            moveToPerch();
        }
    }

    private void settle() {
        settled = true;
        cat.getNavigation().stop();
        Level level = cat.level();
        boolean bed = level.getBlockState(perch).is(ModBlocks.BAR_COUNTER.get()) || level.getBlockState(perch).is(Blocks.HAY_BLOCK);
        if (bed && cat.getRandom().nextInt(2) == 0) {
            cat.setLying(true);     // curled up asleep on the bar
        } else {
            cat.setInSittingPose(true);
        }
    }

    @Override
    public void stop() {
        cat.setInSittingPose(false);
        cat.setLying(false);
        perch = null;
    }
}
