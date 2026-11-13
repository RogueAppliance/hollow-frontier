package com.hollowfrontier.content.frontier.gun;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.WeakHashMap;

import com.hollowfrontier.HollowFrontier;
import com.hollowfrontier.registry.ModSounds;

import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.protocol.game.ClientboundSoundPacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.FluidTags;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.common.Tags;
import net.neoforged.neoforge.event.tick.LevelTickEvent;

/**
 * What a shot feels like (46a), apart from what it does: the kick the shooter's view takes, what a ball does to the
 * thing it hits, the crack of one going past, and the powder haze a gunfight leaves. None of it touches damage, spread,
 * reload or cooldown; those are {@link GunStats}' and {@link GunMechanics}' alone.
 * <p>
 * The numbers are here, in a common class, so game tests can hold them. The view itself is moved by
 * {@code client/CameraFeel}.
 */
@EventBusSubscriber(modid = HollowFrontier.MODID)
public final class GunFeel {
    private GunFeel() {}

    // ---- the kick ----

    /** A phlogiston gun has no powder behind it: its kick is this much of what its damage would give a muzzleloader. */
    public static final float RAY_KICK = 0.15F;
    /** No shot throws the view further than this, in degrees. */
    public static final float MAX_KICK = 4.5F;

    /**
     * How far a shot throws the view up, in degrees, before it eases back: more for a heavier load. A Blunderbuss's two
     * barrels of buckshot kick most, a rifle next, a revolver least; a ray hardly at all.
     */
    public static float kick(GunStats stats) {
        float kick = Math.min(MAX_KICK, 0.7F + stats.shotDamage() * 0.12F);
        return stats.magazine() == 0 ? kick * RAY_KICK : kick;
    }

    /** The sideways part, as a share of the kick: a little, to one side or the other. */
    public static final float KICK_SIDEWAYS = 0.3F;

    // ---- what a ball hits ----

    public enum Impact {
        /** Stone, ore, metal, glass: chips, a spark, and now and then a ricochet. */
        HARD,
        /** Wood, earth, sand, wool and everything else: chips and the block's own sound. */
        SOFT
    }

    /** One ricochet in this many hits on something hard. */
    public static final int RICOCHET_ODDS = 4;
    /** Only this many pellets of one shot make a sound where they land; all of them throw chips. */
    public static final int LOUD_PELLETS = 2;

    public static Impact impact(BlockState state) {
        boolean hard = state.is(BlockTags.MINEABLE_WITH_PICKAXE) || state.is(Tags.BlockItems.GLASS_BLOCKS.block()) || state.is(Tags.BlockItems.GLASS_PANES.block());
        return hard ? Impact.HARD : Impact.SOFT;
    }

    /** A ball stopped by a block: chips of that block, its own hit sound, and for something hard a spark and maybe a whine. */
    public static void strike(ServerLevel level, BlockHitResult hit, int pellet, RandomSource random) {
        BlockPos pos = hit.getBlockPos();
        BlockState state = level.getBlockState(pos);
        // A little out from the face, so the chips aren't born inside the block
        Vec3 at = hit.getLocation().add(Vec3.atLowerCornerOf(hit.getDirection().getUnitVec3i()).scale(0.06));
        level.sendParticles(new BlockParticleOption(ParticleTypes.BLOCK, state), at.x, at.y, at.z, 5, 0.06, 0.06, 0.06, 0.05);
        level.sendParticles(ParticleTypes.SMOKE, at.x, at.y, at.z, 1, 0.03, 0.03, 0.03, 0.01);
        Impact impact = impact(state);
        if (impact == Impact.HARD) {
            level.sendParticles(ParticleTypes.ELECTRIC_SPARK, at.x, at.y, at.z, 3, 0.05, 0.05, 0.05, 0.3);
        }
        if (pellet < LOUD_PELLETS) {
            SoundType sound = state.getSoundType(level, pos, null);
            level.playSound(null, at.x, at.y, at.z, sound.getHitSound(), SoundSource.BLOCKS, 0.9F, sound.getPitch() * 1.3F);
            if (impact == Impact.HARD && random.nextInt(RICOCHET_ODDS) == 0) {
                level.playSound(null, at.x, at.y, at.z, ModSounds.GUN_RICOCHET.get(), SoundSource.PLAYERS, 0.7F, 0.9F + random.nextFloat() * 0.3F);
            }
        }
    }

    /** Water on the way: a splash where the ball goes in. Shots through water were already allowed; this only shows them. */
    public static void splash(ServerLevel level, LivingEntity shooter, Vec3 start, Vec3 end, int pellet) {
        if (level.getFluidState(BlockPos.containing(start)).is(FluidTags.WATER)) {
            return; // fired from under water: no surface to break on the way in
        }
        BlockHitResult water = level.clip(new ClipContext(start, end, ClipContext.Block.COLLIDER, ClipContext.Fluid.ANY, shooter));
        if (water.getType() == HitResult.Type.MISS || !level.getFluidState(water.getBlockPos()).is(FluidTags.WATER)) {
            return;
        }
        Vec3 at = water.getLocation();
        level.sendParticles(ParticleTypes.SPLASH, at.x, at.y + 0.1, at.z, 8, 0.1, 0.02, 0.1, 0.1);
        if (pellet < LOUD_PELLETS) {
            level.playSound(null, at.x, at.y, at.z, SoundEvents.GENERIC_SPLASH, SoundSource.BLOCKS, 0.25F, 1.6F);
        }
    }

    /** A hit on a creature, for the shooter alone: a dull thud (a headshot has its own ding and gets no thud). */
    public static void thud(ServerPlayer shooter) {
        shooter.connection.send(new ClientboundSoundPacket(ModSounds.GUN_THUD, SoundSource.PLAYERS, shooter.getX(), shooter.getY(), shooter.getZ(),
                0.5F, 0.9F + shooter.getRandom().nextFloat() * 0.2F, shooter.getRandom().nextLong()));
    }

    // ---- near misses ----

    /** A ball passing this close to a player who isn't the shooter, and wasn't hit, is heard going by. */
    public static final double NEAR_MISS = 2.0;
    /** But not at the muzzle: within this many blocks of where it was fired it's just the shot. */
    public static final double NEAR_MISS_FROM = 4.0;

    /**
     * The point on a ball's path nearest {@code ear}, or null if the path never comes within {@link #NEAR_MISS} of it
     * (or only does so at the muzzle).
     */
    public static Vec3 nearMiss(Vec3 start, Vec3 end, Vec3 ear) {
        Vec3 path = end.subtract(start);
        double length = path.length();
        if (length < NEAR_MISS_FROM) {
            return null;
        }
        double along = Mth.clamp(ear.subtract(start).dot(path) / length, 0.0, length);
        if (along < NEAR_MISS_FROM) {
            return null;
        }
        Vec3 nearest = start.add(path.scale(along / length));
        return nearest.distanceToSqr(ear) <= NEAR_MISS * NEAR_MISS ? nearest : null;
    }

    /** Tells every player a ball went past, each once a shot, except the shooter and anyone it hit. */
    public static void whiz(ServerLevel level, LivingEntity shooter, Vec3 start, Vec3 end, Set<Entity> struck, Set<ServerPlayer> told) {
        for (ServerPlayer player : level.players()) {
            if (player == shooter || struck.contains(player) || told.contains(player) || player.isSpectator()) {
                continue;
            }
            Vec3 nearest = nearMiss(start, end, player.getEyePosition());
            if (nearest != null) {
                told.add(player);
                player.connection.send(new ClientboundSoundPacket(ModSounds.GUN_WHIZ, SoundSource.PLAYERS, nearest.x, nearest.y, nearest.z,
                        0.8F, 0.9F + player.getRandom().nextFloat() * 0.3F, player.getRandom().nextLong()));
            }
        }
    }

    // ---- the haze ----

    /** This many black powder shots from within {@link #HAZE_SPREAD} blocks inside {@link #HAZE_WINDOW} ticks leave a haze. */
    public static final int HAZE_SHOTS = 3;
    public static final int HAZE_WINDOW = 120;
    public static final double HAZE_SPREAD = 6.0;
    /** How long a haze is fed after the last shot that made it; the smoke already there then thins away by itself. */
    public static final int HAZE_TICKS = 60;
    private static final int MAX_HAZES = 16;

    private record Shot(Vec3 at, long time) {}

    private static final class Haze {
        final Vec3 at;
        long until;

        Haze(Vec3 at, long until) {
            this.at = at;
            this.until = until;
        }
    }

    private static final Map<Level, List<Shot>> SHOTS = new WeakHashMap<>();
    private static final Map<Level, List<Haze>> HAZES = new WeakHashMap<>();

    /** Whether shots at these times and places, the last just fired, are enough for a haze. */
    public static boolean hazy(List<Vec3> places, List<Long> times, Vec3 at, long now) {
        int near = 0;
        for (int i = 0; i < places.size(); i++) {
            if (now - times.get(i) <= HAZE_WINDOW && places.get(i).distanceToSqr(at) <= HAZE_SPREAD * HAZE_SPREAD) {
                near++;
            }
        }
        return near >= HAZE_SHOTS;
    }

    /** A black powder shot was fired from {@code muzzle}. Enough of them close together and the smoke stays. */
    public static void powder(ServerLevel level, Vec3 muzzle) {
        long now = level.getGameTime();
        List<Shot> shots = SHOTS.computeIfAbsent(level, l -> new ArrayList<>());
        shots.removeIf(shot -> now - shot.time() > HAZE_WINDOW);
        shots.add(new Shot(muzzle, now));
        List<Vec3> places = new ArrayList<>();
        List<Long> times = new ArrayList<>();
        for (Shot shot : shots) {
            places.add(shot.at());
            times.add(shot.time());
        }
        if (!hazy(places, times, muzzle, now)) {
            return;
        }
        List<Haze> hazes = HAZES.computeIfAbsent(level, l -> new ArrayList<>());
        for (Haze haze : hazes) {
            if (haze.at.distanceToSqr(muzzle) <= HAZE_SPREAD * HAZE_SPREAD) {
                haze.until = now + HAZE_TICKS; // the same fight: it just lasts longer
                return;
            }
        }
        if (hazes.size() < MAX_HAZES) {
            hazes.add(new Haze(muzzle, now + HAZE_TICKS));
        }
    }

    /** How many hazes hang in a level (for tests). */
    public static int hazes(Level level) {
        List<Haze> hazes = HAZES.get(level);
        return hazes == null ? 0 : hazes.size();
    }

    @SubscribeEvent
    static void onLevelTick(LevelTickEvent.Post event) {
        if (!(event.getLevel() instanceof ServerLevel level)) {
            return;
        }
        List<Haze> hazes = HAZES.get(level);
        if (hazes == null || hazes.isEmpty()) {
            return;
        }
        long now = level.getGameTime();
        for (Iterator<Haze> it = hazes.iterator(); it.hasNext();) {
            Haze haze = it.next();
            if (now >= haze.until) {
                it.remove();
            } else if (now % 5 == 0) {
                // Wisps of white smoke all the while, and every half second a slow grey-white billow that hangs and fades
                // by itself over the next several seconds (a campfire's, which is the one smoke that stays)
                level.sendParticles(ParticleTypes.WHITE_SMOKE, haze.at.x, haze.at.y + 0.3, haze.at.z, 4, 1.2, 0.4, 1.2, 0.002);
                if (now % 10 == 0) {
                    level.sendParticles(ParticleTypes.CAMPFIRE_COSY_SMOKE, haze.at.x, haze.at.y + 0.2, haze.at.z, 1, 1.0, 0.25, 1.0, 0.002);
                }
            }
        }
    }
}
