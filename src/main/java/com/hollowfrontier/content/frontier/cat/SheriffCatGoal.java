package com.hollowfrontier.content.frontier.cat;

import java.util.EnumSet;
import java.util.List;

import org.jspecify.annotations.Nullable;

import com.hollowfrontier.content.frontier.townsfolk.Lawman;

import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.animal.feline.Cat;

/**
 * The sheriff's cat trots after the town's Sheriff like his deputy, loosely (it hangs back a few blocks), and sits down
 * beside him whenever he stands still, at his desk most of all. With the Sheriff gone it stays home at the office.
 */
public class SheriffCatGoal extends Goal {
    public static final double RANGE = 32.0;
    private static final double HEEL = 3.0;

    private final Cat cat;
    private @Nullable Lawman sheriff;
    private int repath;

    public SheriffCatGoal(Cat cat) {
        this.cat = cat;
        setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK));
    }

    /** The nearest living Sheriff within range of the cat, if any. */
    public static @Nullable Lawman findSheriff(Cat cat) {
        List<Lawman> sheriffs = cat.level().getEntitiesOfClass(Lawman.class, cat.getBoundingBox().inflate(RANGE),
                l -> l.isAlive() && l.getRank() == Lawman.Rank.SHERIFF);
        return sheriffs.stream().min((a, b) -> Double.compare(a.distanceToSqr(cat), b.distanceToSqr(cat))).orElse(null);
    }

    private boolean isSheriffsCat() {
        return !cat.isTame() && TownCat.of(cat).role() == TownCat.Role.SHERIFF;
    }

    @Override
    public boolean canUse() {
        // canUse is only tried every other tick (which ones depends on the id), so a plain tickCount % 20 never passes
        // for cats with odd ids: this passes once a second for every cat (as RaidGoals.due).
        if (!isSheriffsCat() || (cat.level().getGameTime() + cat.getId()) % 20 >= 2) {
            return false;
        }
        sheriff = findSheriff(cat);
        return sheriff != null;
    }

    @Override
    public boolean canContinueToUse() {
        return isSheriffsCat() && sheriff != null && sheriff.isAlive() && sheriff.distanceToSqr(cat) < RANGE * RANGE;
    }

    @Override
    public void start() {
        repath = 0;
    }

    @Override
    public void tick() {
        cat.getLookControl().setLookAt(sheriff, 10.0F, cat.getMaxHeadXRot());
        if (cat.distanceToSqr(sheriff) > HEEL * HEEL) {
            cat.setInSittingPose(false);
            if (--repath <= 0) {
                repath = adjustedTickDelay(10);
                cat.getNavigation().moveTo(sheriff, 1.0);
            }
        } else {
            cat.getNavigation().stop();
            // He's stopped (at his desk, or watching the street): sit by him.
            cat.setInSittingPose(sheriff.getDeltaMovement().horizontalDistanceSqr() < 1.0E-3);
        }
    }

    @Override
    public void stop() {
        cat.setInSittingPose(false);
        sheriff = null;
    }
}
