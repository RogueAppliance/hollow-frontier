package com.hollowfrontier.content.frontier.raid;

import java.util.EnumSet;

import org.jspecify.annotations.Nullable;

import com.hollowfrontier.content.frontier.gear.DynamiteEntity;
import com.hollowfrontier.content.frontier.gun.GunWielder.GunPose;
import com.hollowfrontier.content.frontier.law.Wanted;
import com.hollowfrontier.content.frontier.outlaw.AbstractOutlaw;
import com.hollowfrontier.content.frontier.raid.RaidMember.Role;
import com.hollowfrontier.content.frontier.talk.Chatter;
import com.hollowfrontier.content.frontier.talk.Moment;
import com.hollowfrontier.content.frontier.townsfolk.Lawman;
import com.hollowfrontier.content.frontier.townsfolk.Townsfolk;
import com.hollowfrontier.registry.ModItems;
import com.hollowfrontier.registry.ModSounds;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.ai.goal.OpenDoorGoal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.ai.util.DefaultRandomPos;
import net.minecraft.world.entity.animal.equine.AbstractHorse;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.SwingAnimation;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.DoorBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.DoubleBlockHalf;
import net.minecraft.world.phys.Vec3;

/**
 * Raid AI. A raider gets its jobs when it joins the world ({@link #equip}): every one rides out when the raid is
 * decided, the safecracker works the vault, the dynamiter throws, the rest hold the middle of the street, and none of
 * them go after townsfolk (they came for the money) or three-star players (one of their own, unless shot at).
 * Lawmen form up in the street ({@link Muster}) and townsfolk get indoors ({@link Shelter}); those goals are always
 * there and only act while a raid is on in their town.
 */
public final class RaidGoals {
    private RaidGoals() {}

    /** Gives a raider its raid goals and targets (once per entity instance). */
    public static void equip(AbstractOutlaw outlaw) {
        if (outlaw.goalSelector.getAvailableGoals().stream().anyMatch(g -> g.getGoal() instanceof Advance)) {
            return;
        }
        outlaw.learnDoors(); // into the bank by its front door
        outlaw.goalSelector.addGoal(0, new Retreat(outlaw));
        outlaw.goalSelector.addGoal(1, new OpenDoorGoal(outlaw, false));
        outlaw.goalSelector.addGoal(1, new Safecrack(outlaw));
        outlaw.goalSelector.addGoal(1, new ThrowDynamite(outlaw));
        outlaw.goalSelector.addGoal(3, new Advance(outlaw));
        outlaw.targetSelector.removeAllGoals(goal -> goal instanceof NearestAttackableTargetGoal);
        outlaw.targetSelector.addGoal(2, new NearestAttackableTargetGoal<>(outlaw, Player.class, true, (p, level) -> Wanted.stars((Player) p) < 3));
        outlaw.targetSelector.addGoal(3, new NearestAttackableTargetGoal<>(outlaw, Lawman.class, true));
    }

    static @Nullable TownRaid raidOf(Mob mob) {
        RaidMember member = RaidMember.of(mob);
        return member == null || !(mob.level() instanceof ServerLevel level) ? null : TownRaids.get(level).byId(member.raid());
    }

    /**
     * Throttle for canUse. Goal selectors only try canUse every other tick (which ticks depends on the mob's id), so
     * a plain tickCount % n check never passes for half of all mobs; this passes once every n ticks for every mob.
     */
    static boolean due(Mob mob, int interval) {
        return (mob.level().getGameTime() + mob.getId()) % interval < 2;
    }

    static Role role(Mob mob) {
        RaidMember member = RaidMember.of(mob);
        return member == null ? Role.GUNSLINGER : member.role();
    }

    // ---------------------------------------------------------------- raiders

    /** No one to shoot: make for this raider's spot in the middle of the street. */
    static class Advance extends Goal {
        private final AbstractOutlaw mob;
        private @Nullable BlockPos post;

        Advance(AbstractOutlaw mob) {
            this.mob = mob;
            setFlags(EnumSet.of(Flag.MOVE));
        }

        @Override
        public boolean canUse() {
            if (mob.getTarget() != null || mob.isPassenger() || !due(mob, 10)) {
                return false;
            }
            TownRaid raid = raidOf(mob);
            if (raid == null || raid.phase() != TownRaid.Phase.ASSAULT || role(mob) == Role.SAFECRACKER) {
                return false;
            }
            post = raid.post(mob, false);
            return mob.distanceToSqr(Vec3.atBottomCenterOf(post)) > 9.0;
        }

        @Override
        public boolean canContinueToUse() {
            return mob.getTarget() == null && !mob.getNavigation().isDone();
        }

        @Override
        public void start() {
            mob.getNavigation().moveTo(post.getX() + 0.5, post.getY(), post.getZ() + 0.5, 1.0);
        }
    }

    /** The safecracker goes to the vault and works it. TownRaid counts the progress while he's on the spot. */
    static class Safecrack extends Goal {
        // How far he looks for a way in. Mobs plan 40 blocks by default, and the way from behind a bank to its front door can be longer.
        private static final float ROUTE_LENGTH = 128.0F;
        private final AbstractOutlaw mob;
        private int repath;

        Safecrack(AbstractOutlaw mob) {
            this.mob = mob;
            setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK));
        }

        private @Nullable TownRaid job() {
            if (role(mob) != Role.SAFECRACKER || mob.isPassenger()) {
                return null;
            }
            TownRaid raid = raidOf(mob);
            return raid != null && raid.phase() == TownRaid.Phase.ASSAULT && raid.site().workAt().isPresent() ? raid : null;
        }

        @Override
        public boolean canUse() {
            return job() != null;
        }

        @Override
        public boolean requiresUpdateEveryTick() {
            return true;
        }

        @Override
        public void start() {
            repath = 0;
            mob.setTarget(null);
            mob.getNavigation().setRequiredPathLength(ROUTE_LENGTH);
        }

        @Override
        public void tick() {
            TownRaid raid = job();
            if (raid == null) {
                return;
            }
            BlockPos work = raid.site().workAt().orElseThrow();
            BlockPos crack = raid.site().crackAt().orElse(work);
            if (raid.atWork(mob)) {
                mob.getNavigation().stop();
                mob.getLookControl().setLookAt(crack.getX() + 0.5, crack.getY() + 0.8, crack.getZ() + 0.5);
            } else if (--repath <= 0) {
                repath = 20;
                mob.getNavigation().moveTo(work.getX() + 0.5, work.getY(), work.getZ() + 0.5, 1.15);
            }
        }

        @Override
        public void stop() {
            mob.getNavigation().stop();
            mob.getNavigation().setRequiredPathLength(0.0F);
        }
    }

    /**
     * The dynamiter keeps 6-14 blocks from his target, then lights a stick (a fuse hiss and sparks in his raised hand
     * for a second) and lobs it at where they stand. One throw per RaidConfig.dynamiteCooldown.
     */
    static class ThrowDynamite extends Goal {
        static final int WINDUP = 20;
        private static final double MIN = 6.0;
        private static final double MAX = 14.0;
        private final AbstractOutlaw mob;
        private long nextThrow;
        private int windup = -1;
        private int repath;

        ThrowDynamite(AbstractOutlaw mob) {
            this.mob = mob;
            setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK));
        }

        @Override
        public boolean canUse() {
            LivingEntity target = mob.getTarget();
            if (target == null || !target.isAlive() || mob.isPassenger() || role(mob) != Role.DYNAMITER) {
                return false;
            }
            TownRaid raid = raidOf(mob);
            return raid != null && raid.phase() == TownRaid.Phase.ASSAULT;
        }

        @Override
        public boolean requiresUpdateEveryTick() {
            return true;
        }

        @Override
        public void start() {
            windup = -1;
            repath = 0;
        }

        @Override
        public void stop() {
            windup = -1;
            mob.setGunPose(GunPose.LOWERED);
        }

        @Override
        public void tick() {
            LivingEntity target = mob.getTarget();
            if (target == null) {
                return;
            }
            ServerLevel level = (ServerLevel) mob.level();
            mob.getLookControl().setLookAt(target, 30.0F, 30.0F);
            if (windup >= 0) {
                // Fuse lit, arm up: a second's warning before the throw.
                mob.getNavigation().stop();
                Vec3 hand = handPos(mob);
                if (windup % 2 == 0) {
                    level.sendParticles(ParticleTypes.SMALL_FLAME, hand.x, hand.y, hand.z, 1, 0.02, 0.02, 0.02, 0.01);
                    level.sendParticles(ParticleTypes.SMOKE, hand.x, hand.y + 0.1, hand.z, 1, 0.02, 0.02, 0.02, 0.01);
                }
                if (++windup >= WINDUP) {
                    throwAt(level, target);
                    windup = -1;
                    mob.setGunPose(GunPose.LOWERED);
                    nextThrow = level.getGameTime() + RaidConfig.dynamiteCooldown() + mob.getRandom().nextInt(40);
                }
                return;
            }
            double dist = mob.distanceTo(target);
            boolean canSee = mob.getSensing().hasLineOfSight(target);
            if (dist > MAX || !canSee) {
                if (--repath <= 0) {
                    repath = 10;
                    mob.getNavigation().moveTo(target, 1.0);
                }
            } else if (dist < MIN) {
                if (--repath <= 0) {
                    repath = 10;
                    Vec3 away = DefaultRandomPos.getPosAway(mob, 8, 4, target.position());
                    if (away != null) {
                        mob.getNavigation().moveTo(away.x, away.y, away.z, 1.1);
                    }
                }
            } else {
                mob.getNavigation().stop();
                if (level.getGameTime() >= nextThrow) {
                    windup = 0;
                    mob.setGunPose(GunPose.THROWING);
                    level.playSound(null, mob.getX(), mob.getY(), mob.getZ(), ModSounds.DYNAMITE_FUSE.get(), SoundSource.HOSTILE, 1.0F, 1.0F);
                }
            }
        }

        private static Vec3 handPos(Mob mob) {
            Vec3 right = Vec3.directionFromRotation(0, mob.yBodyRot + 90.0F);
            return mob.position().add(right.scale(0.35)).add(0, 2.2, 0);
        }

        // A lob that lands at the target's feet (gravity and drag of a thrown stick, flight time scaled to distance).
        private void throwAt(ServerLevel level, LivingEntity target) {
            DynamiteEntity stick = new DynamiteEntity(level, mob, new ItemStack(ModItems.DYNAMITE.get()));
            stick.setRaider(true);
            Vec3 from = stick.position();
            Vec3 to = target.position().add(target.getDeltaMovement().scale(10.0));
            Vec3 d = to.subtract(from);
            double flat = Math.sqrt(d.x * d.x + d.z * d.z);
            double ticks = Math.max(12.0, Math.min(28.0, flat * 1.8));
            double drag = 1.0 + 0.005 * ticks; // roughly undoes 1% drag a tick over the flight
            double gravity = stick.getGravity();
            stick.setDeltaMovement(d.x / ticks * drag, (d.y + 0.5 * gravity * ticks * ticks) / ticks, d.z / ticks * drag);
            level.addFreshEntity(stick);
            mob.swing(net.minecraft.world.InteractionHand.MAIN_HAND, SwingAnimation.DEFAULT, false);
        }
    }

    /** The raid is decided: every raider makes for his horse and rides out (on foot if the horse is gone). */
    static class Retreat extends Goal {
        private final AbstractOutlaw mob;
        private int repath;

        Retreat(AbstractOutlaw mob) {
            this.mob = mob;
            setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK));
        }

        private @Nullable TownRaid raid() {
            TownRaid raid = raidOf(mob);
            return raid != null && raid.phase() == TownRaid.Phase.RETREAT ? raid : null;
        }

        @Override
        public boolean canUse() {
            return !mob.isPassenger() && raid() != null;
        }

        @Override
        public boolean requiresUpdateEveryTick() {
            return true;
        }

        @Override
        public void start() {
            repath = 0;
            mob.setTarget(null);
            mob.setGunPose(GunPose.LOWERED);
            Chatter.bark(mob, Moment.RUN);
        }

        @Override
        public void tick() {
            TownRaid raid = raid();
            RaidMember member = RaidMember.of(mob);
            if (raid == null || member == null || mob.isPassenger()) {
                return;
            }
            AbstractHorse horse = member.mount().map(id -> mob.level() instanceof ServerLevel level ? level.getEntity(id) : null)
                    .filter(e -> e instanceof AbstractHorse h && h.isAlive() && !h.isVehicle() && h.distanceToSqr(mob) < 32 * 32)
                    .map(e -> (AbstractHorse) e).orElse(null);
            if (horse != null && mob.distanceToSqr(horse) < 2.5 * 2.5) {
                mob.startRiding(horse, true, false);
                horse.clearHome();
                return;
            }
            if (--repath <= 0 || mob.getNavigation().isDone()) {
                repath = 20;
                if (horse != null) {
                    mob.getNavigation().moveTo(horse, 1.3);
                } else {
                    BlockPos out = raid.site().approach();
                    mob.getNavigation().moveTo(out.getX() + 0.5, out.getY(), out.getZ() + 0.5, 1.3);
                }
            }
        }
    }

    // ---------------------------------------------------------------- the law

    /** While a raid is on in his town, a lawman with nothing to shoot forms up in the street, facing the way the gang comes. */
    public static class Muster extends Goal {
        private final Lawman lawman;
        private @Nullable TownRaid raid;
        private int repath;

        public Muster(Lawman lawman) {
            this.lawman = lawman;
            setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK));
        }

        private boolean onDuty() {
            return raid != null && raid.phase() != TownRaid.Phase.RETREAT && lawman.getTarget() == null && !lawman.isPosse();
        }

        @Override
        public boolean canUse() {
            if (!due(lawman, 20) || !(lawman.level() instanceof ServerLevel level) || lawman.isPosse()) {
                return false;
            }
            raid = TownRaids.raidAt(level, lawman.blockPosition());
            return onDuty();
        }

        @Override
        public boolean canContinueToUse() {
            if (lawman.tickCount % 20 == 0 && lawman.level() instanceof ServerLevel level) {
                TownRaid now = TownRaids.raidAt(level, lawman.blockPosition());
                raid = now != null && now.id().equals(raid.id()) ? now : null;
            }
            return onDuty();
        }

        @Override
        public void start() {
            repath = 0;
            Chatter.bark(lawman, Moment.MUSTER);
        }

        @Override
        public void tick() {
            BlockPos post = raid.post(lawman, true);
            if (lawman.distanceToSqr(Vec3.atBottomCenterOf(post)) > 4.0) {
                if (--repath <= 0) {
                    repath = 20;
                    lawman.getNavigation().moveTo(post.getX() + 0.5, post.getY(), post.getZ() + 0.5, 1.0);
                }
            } else {
                lawman.getNavigation().stop();
                BlockPos edge = raid.site().edge();
                lawman.getLookControl().setLookAt(edge.getX() + 0.5, edge.getY() + 1.6, edge.getZ() + 0.5);
            }
        }

        @Override
        public void stop() {
            raid = null;
        }
    }

    // ---------------------------------------------------------------- townsfolk

    /**
     * While a raid is on in their town, townsfolk get under a roof and stay put: the nearest wooden door that leads
     * somewhere roofed, and in through it (they shut doors behind them; see Townsfolk). Already indoors, they just
     * stay where they are. Out once the raid is over.
     */
    public static class Shelter extends Goal {
        private static final int SEARCH = 16;
        private final Townsfolk folk;
        private @Nullable BlockPos hideout;
        private int repath;

        public Shelter(Townsfolk folk) {
            this.folk = folk;
            setFlags(EnumSet.of(Flag.MOVE));
        }

        private boolean raidHere() {
            return folk.level() instanceof ServerLevel level && TownRaids.raidAt(level, folk.blockPosition()) != null;
        }

        @Override
        public boolean canUse() {
            return due(folk, 20) && raidHere();
        }

        @Override
        public boolean canContinueToUse() {
            return folk.tickCount % 20 != 0 || raidHere();
        }

        @Override
        public void start() {
            repath = 0;
            hideout = sheltered(folk.level(), folk.blockPosition()) ? null : findHideout(folk.level(), folk.blockPosition());
        }

        @Override
        public void tick() {
            if (hideout == null) {
                folk.getNavigation().stop();
                return;
            }
            if (folk.distanceToSqr(Vec3.atBottomCenterOf(hideout)) < 1.5 || sheltered(folk.level(), folk.blockPosition())
                    && folk.distanceToSqr(Vec3.atBottomCenterOf(hideout)) < 9.0) {
                hideout = null;
                folk.getNavigation().stop();
            } else if (--repath <= 0) {
                repath = 20;
                folk.getNavigation().moveTo(hideout.getX() + 0.5, hideout.getY(), hideout.getZ() + 0.5, 0.65);
            }
        }

        public boolean hiding() {
            return hideout == null && sheltered(folk.level(), folk.blockPosition());
        }

        /** Indoors: no sky above head height, nor two blocks off either way (a porch awning doesn't count). */
        public static boolean sheltered(Level level, BlockPos feet) {
            BlockPos head = feet.above();
            if (level.canSeeSky(head)) {
                return false;
            }
            for (Direction dir : Direction.Plane.HORIZONTAL) {
                if (level.canSeeSky(head.relative(dir, 2))) {
                    return false;
                }
            }
            return true;
        }

        private static boolean roofed(Level level, BlockPos feet) {
            return !level.canSeeSky(feet.above());
        }

        /** Two blocks inside the nearest wooden door whose far side is roofed for at least three blocks. */
        public static @Nullable BlockPos findHideout(Level level, BlockPos from) {
            BlockPos best = null;
            for (BlockPos pos : BlockPos.betweenClosed(from.offset(-SEARCH, -3, -SEARCH), from.offset(SEARCH, 3, SEARCH))) {
                BlockState state = level.getBlockState(pos);
                if (!(state.getBlock() instanceof DoorBlock) || state.is(Blocks.IRON_DOOR) || state.getValue(DoorBlock.HALF) != DoubleBlockHalf.LOWER) {
                    continue;
                }
                Direction.Axis axis = state.getValue(DoorBlock.FACING).getAxis();
                for (Direction.AxisDirection sign : Direction.AxisDirection.values()) {
                    Direction in = Direction.fromAxisAndDirection(axis, sign);
                    int roofed = 0;
                    while (roofed < 4 && roofed(level, pos.relative(in, roofed + 1))) {
                        roofed++;
                    }
                    BlockPos inside = pos.relative(in, 2);
                    if (roofed >= 3 && level.getBlockState(inside).isAir() && (best == null || inside.distSqr(from) < best.distSqr(from))) {
                        best = inside.immutable();
                    }
                }
            }
            return best;
        }
    }
}
