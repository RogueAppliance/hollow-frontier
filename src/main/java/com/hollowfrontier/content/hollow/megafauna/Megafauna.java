package com.hollowfrontier.content.hollow.megafauna;

import java.util.EnumSet;
import java.util.List;

import com.hollowfrontier.content.hollow.creature.HollowCreatures;

import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.level.LevelReader;

/**
 * What the Hollow Earth's wild megafauna (10a) share: the Woolly Mammoth, Terror Bird, Megatherium and Glyptodon.
 * <ul>
 * <li><b>Neutral beasts</b> (mammoth, megatherium) leave you be until hurt, then go for whoever hurt them, and the rest
 *     of the herd joins in ({@code HurtByTargetGoal.setAlertOthers}). They give up when the attacker gets away (out of
 *     follow range, or unseen a few seconds), or when an {@link OliphantItem} is blown.</li>
 * <li><b>The Oliphant</b> calms angry neutral beasts and puts terror birds to flight ({@link #calm}, {@link TerrorBird#scare}).</li>
 * </ul>
 */
public final class Megafauna {
    private Megafauna() {}

    /** Spawn rule: out on open, lit ground (the creatures' rule from 9b2), with room overhead for something this big. */
    public static boolean onOpenGround(LevelReader level, BlockPos pos, int headroom) {
        if (!HollowCreatures.onLitGround(level, pos)) {
            return false;
        }
        for (int y = 1; y < headroom; y++) {
            if (!level.getBlockState(pos.above(y)).getCollisionShape(level, pos.above(y)).isEmpty()) {
                return false;
            }
        }
        return true;
    }

    /** An angry neutral beast forgets its quarrel: no target, no grudge (until it's hurt again). */
    public static void calm(Mob mob) {
        mob.setTarget(null);
        mob.setLastHurtByMob(null);
        mob.setAggressive(false);
        mob.getNavigation().stop();
    }

    /** Whether a neutral beast is angry with someone. */
    public static boolean isAngry(Mob mob) {
        return mob.getTarget() != null && mob.getTarget().isAlive();
    }

    /**
     * Keeps a herd together: a beast with none of its kind within {@code close} walks to the nearest one within
     * {@code far}. Checked every couple of seconds.
     */
    public static final class FollowHerdGoal<T extends PathfinderMob> extends Goal {
        private final T mob;
        private final Class<T> kind;
        private final double close, far, speed;
        private Mob mate;
        private int recheck;

        public FollowHerdGoal(T mob, Class<T> kind, double close, double far, double speed) {
            this.mob = mob;
            this.kind = kind;
            this.close = close;
            this.far = far;
            this.speed = speed;
            setFlags(EnumSet.of(Flag.MOVE));
        }

        @Override
        public boolean canUse() {
            if (mob.getTarget() != null || --recheck > 0) {
                return false;
            }
            recheck = reducedTickDelay(40 + mob.getRandom().nextInt(40));
            List<T> herd = mob.level().getEntitiesOfClass(kind, mob.getBoundingBox().inflate(far), other -> other != mob && other.isAlive());
            if (herd.isEmpty() || herd.stream().anyMatch(other -> other.distanceTo(mob) < close)) {
                return false;
            }
            mate = herd.stream().min((a, b) -> Double.compare(a.distanceToSqr(mob), b.distanceToSqr(mob))).orElse(null);
            return mate != null;
        }

        @Override
        public boolean canContinueToUse() {
            return mate != null && mate.isAlive() && mob.getTarget() == null && mob.distanceTo(mate) > close * 0.6 && !mob.getNavigation().isDone();
        }

        @Override
        public void start() {
            mob.getNavigation().moveTo(mate, speed);
        }

        @Override
        public void stop() {
            mate = null;
        }
    }
}
