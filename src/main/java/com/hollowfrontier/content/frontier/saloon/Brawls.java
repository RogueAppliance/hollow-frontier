package com.hollowfrontier.content.frontier.saloon;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;

import org.jspecify.annotations.Nullable;

import com.hollowfrontier.HollowFrontier;
import com.hollowfrontier.content.frontier.talk.Chatter;
import com.hollowfrontier.content.frontier.talk.Moment;
import com.hollowfrontier.content.frontier.townsfolk.Lawman;
import com.hollowfrontier.content.frontier.townsfolk.Townsfolk;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.item.component.SwingAnimation;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.DoorBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.DoubleBlockHalf;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.tick.ServerTickEvent;

/**
 * Saloon brawls (21b). After dark, townsfolk drinking near a barmaid get Tipsy, a round at a time (up to Tipsy II). Now
 * and then two of them standing close, or a tipsy townsperson and a lawman, come to blows: fists only, nobody hurt, for
 * {@link #LENGTH} ticks of swings, shoves and the barmaid shouting. Then whoever landed fewer punches is thrown out
 * through the nearest door ({@link #knockOut}), dazed, and both are sobered up. A saloon piano nearby stops for the
 * fight ({@link #near}). Brawls aren't saved: one cut off by a restart just never finishes.
 */
@EventBusSubscriber(modid = HollowFrontier.MODID)
public final class Brawls {
    private Brawls() {}

    public static final int LENGTH = 200, SWING_EVERY = 12, CHECK_EVERY = 200, DAZED = 200;
    public static final double PAIR_RANGE = 5.0, SALOON_RANGE = 14.0, WATCH_RANGE = 24.0, REACH = 2.2;
    public static final float ROUND_CHANCE = 0.2F, START_CHANCE = 0.25F;
    /** The drunkest the house lets townsfolk get. */
    public static final int HOUSE_LIMIT = 2;

    /** One fight: the two in it, how long it's gone on, who's landed how many. */
    public static final class Brawl {
        final Mob a, b;
        final Vec3 saloon;
        int ticks, hitsA, hitsB;

        Brawl(Mob a, Mob b, Vec3 saloon) {
            this.a = a;
            this.b = b;
            this.saloon = saloon;
        }

        public boolean involves(Entity entity) {
            return entity == a || entity == b;
        }

        public int ticks() {
            return ticks;
        }
    }

    private static final List<Brawl> ACTIVE = new ArrayList<>();

    /** Who can come to blows: townsfolk, and a lawman with a townsperson. */
    private static boolean brawler(Mob mob) {
        return (mob instanceof Townsfolk && !(mob instanceof Barmaid) || mob instanceof Lawman) && mob.isAlive() && mob.getTarget() == null && !fighting(mob);
    }

    public static boolean fighting(Entity entity) {
        return ACTIVE.stream().anyMatch(b -> b.involves(entity));
    }

    /** Whether a brawl is going on within {@code range} of {@code pos} (the saloon piano stops for one). */
    public static boolean near(Level level, BlockPos pos, double range) {
        Vec3 at = Vec3.atCenterOf(pos);
        return ACTIVE.stream().anyMatch(b -> b.a.level() == level && b.a.position().distanceTo(at) <= range);
    }

    @SubscribeEvent
    static void onServerTick(ServerTickEvent.Post event) {
        for (Iterator<Brawl> it = ACTIVE.iterator(); it.hasNext(); ) {
            Brawl brawl = it.next();
            if (!(brawl.a.level() instanceof ServerLevel level) || tick(level, brawl)) {
                it.remove();
            }
        }
        if (event.getServer().getTickCount() % CHECK_EVERY != 0) {
            return;
        }
        for (ServerPlayer player : event.getServer().getPlayerList().getPlayers()) {
            if (!player.isSpectator() && player.level() instanceof ServerLevel level && level.isDarkOutside()) {
                saloonNight(level, player);
            }
        }
    }

    /** Around one player: the house pours a round, and maybe two drinkers come to blows. */
    static void saloonNight(ServerLevel level, ServerPlayer player) {
        for (var barmaid : level.getEntitiesOfClass(Barmaid.class, player.getBoundingBox().inflate(WATCH_RANGE), Entity::isAlive)) {
            List<Mob> drinkers = level.getEntitiesOfClass(Mob.class, barmaid.getBoundingBox().inflate(SALOON_RANGE), Brawls::brawler);
            for (Mob drinker : drinkers) {
                if (drinker instanceof Townsfolk && Drink.tipsyLevel(drinker) < HOUSE_LIMIT && level.getRandom().nextFloat() < ROUND_CHANCE) {
                    Drink.addTipsy(drinker, 1);
                }
            }
            if (!SaloonConfig.brawls() || near(level, barmaid.blockPosition(), SALOON_RANGE) || level.getRandom().nextFloat() >= START_CHANCE) {
                continue;
            }
            for (Mob a : drinkers) {
                Mob b = drinkers.stream().filter(m -> m != a && m.distanceTo(a) <= PAIR_RANGE && pair(a, m)).findFirst().orElse(null);
                if (b != null) {
                    start(level, a, b, barmaid);
                    break;
                }
            }
        }
    }

    /** Whether these two would come to blows: a tipsy townsperson and another tipsy one, or a lawman. */
    public static boolean pair(Mob a, Mob b) {
        boolean aTipsy = a instanceof Townsfolk && Drink.tipsyLevel(a) > 0, bTipsy = b instanceof Townsfolk && Drink.tipsyLevel(b) > 0;
        return aTipsy && (bTipsy || b instanceof Lawman) || bTipsy && a instanceof Lawman;
    }

    /** Starts a brawl between these two (the barmaid, if any, shouts at them). Null if either is already in one. */
    public static @Nullable Brawl start(ServerLevel level, Mob a, Mob b, @Nullable Mob barmaid) {
        if (fighting(a) || fighting(b) || a == b) {
            return null;
        }
        Brawl brawl = new Brawl(a, b, barmaid != null ? barmaid.position() : a.position());
        ACTIVE.add(brawl);
        Chatter.bark(a, Moment.BRAWL);
        if (barmaid != null) {
            Chatter.bark(barmaid, Moment.BRAWL);
        }
        level.playSound(null, a.blockPosition(), SoundEvents.GLASS_BREAK, SoundSource.NEUTRAL, 0.8F, 1.2F);
        return brawl;
    }

    /** One tick of a fight. Whether it's over. */
    public static boolean tick(ServerLevel level, Brawl brawl) {
        if (!brawl.a.isAlive() || !brawl.b.isAlive() || brawl.a.level() != brawl.b.level()) {
            return true;
        }
        brawl.ticks++;
        double distance = brawl.a.distanceTo(brawl.b);
        for (Mob[] pair : new Mob[][] {{brawl.a, brawl.b}, {brawl.b, brawl.a}}) {
            pair[0].getLookControl().setLookAt(pair[1], 30.0F, 30.0F);
            if (distance > REACH - 0.6) {
                pair[0].getNavigation().moveTo(pair[1], 1.1);
            } else {
                pair[0].getNavigation().stop();
            }
        }
        if (brawl.ticks % SWING_EVERY == 0 && distance <= REACH) {
            boolean aSwings = level.getRandom().nextBoolean();
            Mob attacker = aSwings ? brawl.a : brawl.b, target = aSwings ? brawl.b : brawl.a;
            attacker.swing(InteractionHand.MAIN_HAND, SwingAnimation.DEFAULT, true);
            Vec3 shove = target.position().subtract(attacker.position()).multiply(1, 0, 1).normalize().scale(0.35);
            target.push(shove.x, 0.1, shove.z);
            level.playSound(null, target.blockPosition(), SoundEvents.PLAYER_ATTACK_WEAK, SoundSource.NEUTRAL, 1.0F, 0.9F + level.getRandom().nextFloat() * 0.2F);
            level.sendParticles(ParticleTypes.CRIT, target.getX(), target.getY(0.8), target.getZ(), 4, 0.2, 0.2, 0.2, 0.1);
            if (aSwings) {
                brawl.hitsA++;
            } else {
                brawl.hitsB++;
            }
        }
        if (brawl.ticks < LENGTH) {
            return false;
        }
        Mob winner = brawl.hitsA >= brawl.hitsB ? brawl.a : brawl.b, loser = winner == brawl.a ? brawl.b : brawl.a;
        knockOut(level, loser, brawl.saloon);
        Drink.sober(brawl.a, TipsyEffect.MAX_AMPLIFIER + 1);
        Drink.sober(brawl.b, TipsyEffect.MAX_AMPLIFIER + 1);
        for (ServerPlayer player : level.getPlayers(p -> p.distanceTo(loser) <= WATCH_RANGE)) {
            player.sendSystemMessage(Component.translatable("message.hollowfrontier.brawl.thrown_out", winner.getDisplayName(), loser.getDisplayName())
                    .withStyle(ChatFormatting.GRAY, ChatFormatting.ITALIC));
        }
        return true;
    }

    /** Out through the nearest door (away from the bar), dazed. Where they landed. */
    public static Vec3 knockOut(ServerLevel level, Mob loser, Vec3 saloon) {
        BlockPos door = nearestDoor(level, loser.blockPosition());
        Vec3 landed = loser.position();
        if (door != null) {
            Vec3 middle = Vec3.atBottomCenterOf(door);
            Direction out = Direction.getApproximateNearest(middle.x - saloon.x, 0, middle.z - saloon.z);
            for (int step : new int[] {2, 1}) {
                BlockPos at = door.relative(out, step);
                if (level.getBlockState(at).getCollisionShape(level, at).isEmpty() && level.getBlockState(at.above()).getCollisionShape(level, at.above()).isEmpty()) {
                    landed = Vec3.atBottomCenterOf(at);
                    loser.getNavigation().stop();
                    loser.teleportTo(landed.x, landed.y, landed.z);
                    loser.setDeltaMovement(out.getStepX() * 0.4, 0.3, out.getStepZ() * 0.4);
                    break;
                }
            }
        }
        loser.addEffect(new MobEffectInstance(MobEffects.SLOWNESS, DAZED, 3));
        loser.addEffect(new MobEffectInstance(MobEffects.NAUSEA, DAZED, 0));
        level.sendParticles(ParticleTypes.POOF, loser.getX(), loser.getY(0.5), loser.getZ(), 10, 0.3, 0.4, 0.3, 0.02);
        level.playSound(null, loser.blockPosition(), SoundEvents.WOODEN_DOOR_OPEN, SoundSource.NEUTRAL, 1.0F, 0.8F);
        return landed;
    }

    private static @Nullable BlockPos nearestDoor(ServerLevel level, BlockPos around) {
        List<BlockPos> doors = new ArrayList<>();
        for (BlockPos pos : BlockPos.betweenClosed(around.offset(-12, -2, -12), around.offset(12, 2, 12))) {
            BlockState state = level.getBlockState(pos);
            if (state.getBlock() instanceof DoorBlock && state.getValue(DoorBlock.HALF) == DoubleBlockHalf.LOWER) {
                doors.add(pos.immutable());
            }
        }
        return doors.stream().min(Comparator.comparingDouble(d -> d.distSqr(around))).orElse(null);
    }

    /** For tests: every fight stopped. */
    public static void clear() {
        ACTIVE.clear();
    }

    /** For tests: a fight cut short to its last tick. */
    public static void hurry(Brawl brawl) {
        brawl.ticks = LENGTH - 1;
    }
}
