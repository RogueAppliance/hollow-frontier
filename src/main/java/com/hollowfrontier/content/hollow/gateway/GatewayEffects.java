package com.hollowfrontier.content.hollow.gateway;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.UUID;

import com.hollowfrontier.HollowFrontier;
import com.hollowfrontier.registry.ModSounds;

import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.server.ServerStoppedEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;
import net.neoforged.neoforge.event.entity.living.LivingFallEvent;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

/**
 * The show around a Polar Gateway, run from the server tick:
 * <ul>
 * <li><b>Waking</b> ({@link #awaken}): the ring's stones light one after another round the ring, each chiming a little
 *     higher, then after a breath the well floods with light ({@link PolarGateways#open}), {@link #WAKE_TICKS} in all.</li>
 * <li><b>Descent</b> ({@link #descend}): a traveller just through drifts down a column of light inside (or is lifted out
 *     of the well and floats down to the rim coming home), with snow swirling round them, and lands with a soft burst.</li>
 * </ul>
 * Neither is saved: a ring that was waking when the server stopped stays half lit until the resonator is used again.
 * Both are dropped when the server stops, so nothing carries over into the next world opened in the same game.
 */
@EventBusSubscriber(modid = HollowFrontier.MODID)
public final class GatewayEffects {
    private GatewayEffects() {}

    public static final int STONE_TICKS = 3;
    public static final int FLOOD_PAUSE = 10;
    public static final int WAKE_TICKS = PolarGateways.RING_ORDER.size() * STONE_TICKS + FLOOD_PAUSE;
    /** Slow Falling for the drift down inside: plenty to reach the ground, taken off on landing. */
    public static final int DESCENT_TICKS = 200;
    /** Longest a fall down Symmes' shaft can take (it takes about 15 s); Slow Falling holds that long as a safety net. */
    public static final int FALL_TICKS = 900;
    static final int LIFT_TICKS = 18;

    private static final class Waking {
        final ResourceKey<Level> level;
        final BlockPos center;
        int ticks;

        Waking(ResourceKey<Level> level, BlockPos center) {
            this.level = level;
            this.center = center;
        }
    }

    /** A player's fall down Symmes' shaft: where they'll land (the return gateway's south stone) and where the crust ends. */
    public record Fall(Vec3 target, int undersideY) {}

    private static final class Descent {
        final ResourceKey<Level> level;
        final UUID entity;
        final boolean inside;
        final @Nullable Fall fall;
        final boolean firstVisit;
        boolean revealed;
        int ticks;

        Descent(ResourceKey<Level> level, UUID entity, boolean inside, @Nullable Fall fall, boolean firstVisit) {
            this.level = level;
            this.entity = entity;
            this.inside = inside;
            this.fall = fall;
            this.firstVisit = firstVisit;
        }

        int maxTicks() {
            return fall != null ? FALL_TICKS : DESCENT_TICKS;
        }
    }

    private static final List<Waking> WAKING = new ArrayList<>();
    private static final List<Descent> DESCENTS = new ArrayList<>();

    // ---------------------------------------------------------------- waking

    /** Starts waking the ring at center (a whole ring with an empty well); the resonator has already been paid. */
    public static void awaken(ServerLevel level, BlockPos center) {
        if (!isWaking(level, center)) {
            WAKING.add(new Waking(level.dimension(), center.immutable()));
        }
    }

    public static boolean isWaking(Level level, BlockPos center) {
        return WAKING.stream().anyMatch(w -> w.level == level.dimension() && w.center.equals(center));
    }

    private static boolean tickWaking(MinecraftServer server, Waking w) {
        ServerLevel level = server.getLevel(w.level);
        if (level == null) {
            return true;
        }
        if (!level.isLoaded(w.center)) {
            return false; // the steam's been paid: it carries on once its chunk is back
        }
        if (!PolarGateways.isRing(level, w.center)) {
            return true; // broken in creative mid-way: give up
        }
        w.ticks++;
        int stone = w.ticks / STONE_TICKS - 1;
        if (w.ticks % STONE_TICKS == 0 && stone < PolarGateways.RING_ORDER.size()) {
            BlockPos pos = w.center.offset(PolarGateways.RING_ORDER.get(stone));
            BlockState state = level.getBlockState(pos);
            level.setBlock(pos, state.setValue(GatewayFrameBlock.ACTIVE, true), Block.UPDATE_ALL);
            level.playSound(null, pos, ModSounds.GATEWAY_STONE.get(), SoundSource.BLOCKS, 1.2F, 0.6F + stone * 0.09F);
            level.sendParticles(ParticleTypes.END_ROD, pos.getX() + 0.5, pos.getY() + 1.1, pos.getZ() + 0.5, 6, 0.2, 0.3, 0.2, 0.02);
        }
        if (w.ticks >= WAKE_TICKS) {
            if (PolarGateways.wellClear(level, w.center)) {
                PolarGateways.open(level, w.center);
            }
            return true;
        }
        return false;
    }

    // ---------------------------------------------------------------- descent

    /**
     * A living traveller just through: inside, Slow Falling for the drift down from where they came out (high above the
     * return gateway, or a player at the top of Symmes' shaft, whose client steers the fall: HollowDescent); coming home,
     * a short lift out of the well, then Slow Falling down to the rim. Neither shows an icon. While it lasts the fall can't
     * hurt them, whatever the client does. On a first visit the title card comes up as a falling player leaves the crust.
     */
    public static void descend(LivingEntity traveller, boolean inside, @Nullable Fall fall, boolean firstVisit) {
        if (!inside) {
            traveller.addEffect(new MobEffectInstance(MobEffects.LEVITATION, LIFT_TICKS, 2, true, false, false));
        }
        traveller.addEffect(new MobEffectInstance(MobEffects.SLOW_FALLING, fall != null ? FALL_TICKS : DESCENT_TICKS, 0, true, false, false));
        DESCENTS.add(new Descent(traveller.level().dimension(), traveller.getUUID(), inside, fall, firstVisit));
    }

    /** Whether this traveller is still on its way down from a crossing (so the fall can't hurt it). */
    public static boolean isDescending(Entity entity) {
        return DESCENTS.stream().anyMatch(d -> d.entity.equals(entity.getUUID()));
    }

    private static boolean tickDescent(MinecraftServer server, Descent d) {
        ServerLevel level = server.getLevel(d.level);
        Entity entity = level == null ? null : level.getEntity(d.entity);
        d.ticks++;
        if (!(entity instanceof LivingEntity traveller) || !traveller.isAlive()) {
            return d.ticks > 5;
        }
        traveller.resetFallDistance();
        if (d.fall != null && !d.revealed && traveller.getY() < d.fall.undersideY() - 6) {
            d.revealed = true; // out of the crust, into the open: the Hollow Earth spread out below
            if (d.firstVisit && traveller instanceof ServerPlayer player) {
                PolarGateways.showTitle(player);
            }
        }
        boolean landed = d.ticks > (d.inside ? 3 : LIFT_TICKS + 3) && traveller.onGround();
        if (landed || d.ticks > d.maxTicks()) {
            land(level, traveller);
            return true;
        }
        // Snow spiralling round them, and a column of light under them down to where they'll land
        double angle = d.ticks * 0.6;
        for (int i = 0; i < 2; i++) {
            double a = angle + i * Math.PI;
            // (out at arm's length and below the eyes: closer, the flakes fill a first-person view)
            level.sendParticles(ParticleTypes.SNOWFLAKE, traveller.getX() + Math.cos(a) * 2.2, traveller.getY() + 0.2, traveller.getZ() + Math.sin(a) * 2.2,
                    1, 0.0, 0.0, 0.0, 0.0);
        }
        if (d.inside && d.ticks % 2 == 0) {
            double y = traveller.getY() - level.getRandom().nextDouble() * 24;
            level.sendParticles(ParticleTypes.END_ROD, traveller.getX(), y, traveller.getZ(), 2, 0.25, 0.4, 0.25, 0.0);
        }
        return false;
    }

    private static void land(ServerLevel level, LivingEntity traveller) {
        MobEffectInstance drift = traveller.getEffect(MobEffects.SLOW_FALLING);
        if (drift != null && drift.isAmbient()) {
            traveller.removeEffect(MobEffects.SLOW_FALLING);
        }
        level.playSound(null, traveller.getX(), traveller.getY(), traveller.getZ(), ModSounds.GATEWAY_LAND.get(), SoundSource.PLAYERS, 1.0F, 1.0F);
        level.sendParticles(ParticleTypes.SNOWFLAKE, traveller.getX(), traveller.getY() + 0.2, traveller.getZ(), 30, 0.8, 0.2, 0.8, 0.05);
        for (int i = 0; i < 16; i++) {
            double a = i * Math.PI / 8;
            level.sendParticles(ParticleTypes.END_ROD, traveller.getX() + Mth.cos((float) a) * 0.6, traveller.getY() + 0.1,
                    traveller.getZ() + Mth.sin((float) a) * 0.6, 0, Mth.cos((float) a), 0.05, Mth.sin((float) a), 0.12);
        }
    }

    // No fall damage on the way down a crossing, whatever the client did (a lagging client can land faster than Slow Falling allows)
    @SubscribeEvent
    static void onFall(LivingFallEvent event) {
        if (isDescending(event.getEntity())) {
            event.setCanceled(true);
        }
    }

    // ---------------------------------------------------------------- tick

    @SubscribeEvent
    static void onServerTick(ServerTickEvent.Post event) {
        tick(WAKING, w -> tickWaking(event.getServer(), w));
        tick(DESCENTS, d -> tickDescent(event.getServer(), d));
    }

    // The lists outlive the server. In singleplayer a descent left from the last world would keep its traveller (the
    // same UUID in every world) safe from falls in the next one.
    @SubscribeEvent
    static void onServerStopped(ServerStoppedEvent event) {
        WAKING.clear();
        DESCENTS.clear();
    }

    private static <T> void tick(List<T> list, java.util.function.Predicate<T> done) {
        for (Iterator<T> it = list.iterator(); it.hasNext();) {
            if (done.test(it.next())) {
                it.remove();
            }
        }
    }
}
