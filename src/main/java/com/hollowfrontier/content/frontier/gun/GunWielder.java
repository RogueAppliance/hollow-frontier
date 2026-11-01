package com.hollowfrontier.content.frontier.gun;

import com.geckolib.animatable.GeoAnimatable;
import com.geckolib.animatable.manager.AnimatableManager;
import com.geckolib.animation.AnimationController;
import com.geckolib.animation.RawAnimation;
import com.geckolib.animation.object.PlayState;
import com.geckolib.constant.DefaultAnimations;
import com.hollowfrontier.registry.ModItems;

import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.Mob;

/**
 * A gun-toting humanoid mob (outlaws, lawmen) driven by {@link GunAttackGoal}. The pose and shoot ticks are synced
 * entity data on the implementing class; {@link Animations} turns them into the shared "humanoid" arm animations.
 */
public interface GunWielder {
    /** What the arms do with the gun: hang at the side, aim it at the target, hold it to load, or (dynamiters) hold a lit stick up to throw. */
    enum GunPose { LOWERED, AIMING, RELOADING, THROWING }

    GunStats gunStats();

    /** Called by GunAttackGoal after each shot (server side); starts the recoil animation. */
    void onShot();

    /** Called by GunAttackGoal when the gun runs dry and the mob starts to reload (server side). */
    default void onReload() {
    }

    /** Ticks left of the recoil animation. */
    int shootTicks();

    GunPose getGunPose();

    void setGunPose(GunPose pose);

    /** Entities this mob's bullets pass straight through (e.g. a lawman never hits townsfolk). */
    default boolean holdsFireFor(Entity entity) {
        return false;
    }

    /** Shared arm controllers and the aiming body turn for every GunWielder. */
    final class Animations {
        private Animations() {}

        private static final RawAnimation SHOOT_PISTOL = RawAnimation.begin().thenPlay("attack.shoot_pistol");
        private static final RawAnimation SHOOT_LONG = RawAnimation.begin().thenPlay("attack.shoot_long");
        private static final RawAnimation AIM_PISTOL = RawAnimation.begin().thenLoop("attack.aim_pistol");
        private static final RawAnimation AIM_LONG = RawAnimation.begin().thenLoop("attack.aim_long");
        private static final RawAnimation RELOAD_PISTOL = RawAnimation.begin().thenLoop("attack.reload_pistol");
        private static final RawAnimation RELOAD_LONG = RawAnimation.begin().thenLoop("attack.reload_long");
        private static final RawAnimation THROW = RawAnimation.begin().thenLoop("attack.throw");
        private static final RawAnimation RIDE = RawAnimation.begin().thenLoop("misc.ride");

        // The revolver is fired one-handed; long guns are shouldered with both hands.
        private static boolean holdsPistol(Mob mob) {
            return mob.getMainHandItem().is(ModItems.REVOLVER.get());
        }

        public static <T extends Mob & GeoAnimatable & GunWielder> void register(AnimatableManager.ControllerRegistrar controllers) {
            // Base pose: in the saddle (legs astride, hands low on the reins), walking, or standing. One controller that
            // always plays something (idle keys the legs straight), so a rider knocked off his horse stands up at once
            // instead of keeping the seated legs a stopped controller left behind.
            controllers.add(new AnimationController<T>("base", 4, test -> test.setAndContinue(test.animatable().isPassenger() ? RIDE
                    : test.isMoving() ? DefaultAnimations.WALK : DefaultAnimations.IDLE)));
            // Later controllers win on shared bones: the gun pose overrides the walk/idle arm swing, the recoil overrides the pose.
            controllers.add(new AnimationController<T>("gun_pose", 4, test -> {
                boolean pistol = holdsPistol(test.animatable());
                return switch (test.animatable().getGunPose()) {
                    case AIMING -> test.setAndContinue(pistol ? AIM_PISTOL : AIM_LONG);
                    case RELOADING -> test.setAndContinue(pistol ? RELOAD_PISTOL : RELOAD_LONG);
                    case THROWING -> test.setAndContinue(THROW);
                    case LOWERED -> PlayState.STOP;
                };
            }));
            controllers.add(new AnimationController<T>("shoot", test -> test.animatable().shootTicks() > 0
                    ? test.setAndContinue(holdsPistol(test.animatable()) ? SHOOT_PISTOL : SHOOT_LONG) : PlayState.STOP));
        }

        /**
         * While aiming, the whole body squares up to the target instead of lagging behind the head (call from
         * tickHeadTurn on both sides, since the client turns the body itself and GeckoLib renders from it).
         * Returns false when the mob should turn as usual.
         */
        public static boolean squareUp(Mob mob, GunWielder wielder) {
            if (wielder.getGunPose() != GunPose.AIMING && wielder.getGunPose() != GunPose.THROWING) {
                return false;
            }
            mob.yBodyRot = Mth.approachDegrees(mob.yBodyRot, mob.yHeadRot, 30.0F);
            return true;
        }
    }
}
