package com.hollowfrontier.client;

import java.util.List;

import com.hollowfrontier.HollowFrontier;
import com.hollowfrontier.content.hollow.HollowAmbience;
import com.hollowfrontier.content.hollow.creature.GrayProbe;
import com.hollowfrontier.content.steam.submersible.SteamSubmersible;
import com.hollowfrontier.content.ufo.GraySaucer;
import com.hollowfrontier.content.ufo.GrayWarden;
import com.hollowfrontier.registry.ModSounds;
import com.hollowfrontier.worldgen.HollowBiomes;
import com.hollowfrontier.worldgen.HollowEarth;

import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.resources.sounds.AbstractTickableSoundInstance;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.client.resources.sounds.SoundInstance;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.FluidTags;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Entity;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;

/**
 * What the Hollow Earth sounds like beyond its biomes' own beds and one-shots (45b), as {@link TownSoundscape} is for a
 * town:
 * <ul>
 * <li><b>Far-off calls</b>: now and then a creature that lives in this biome, heard from 40 to 80 blocks away in some
 *     direction: a mammoth across the steppes, something singing over deep water.</li>
 * <li><b>The crust</b>: rarely, a rumble from overhead.</li>
 * <li><b>The hush</b>: neither is heard with Gray machines near. The country round a Gray site has gone quiet.</li>
 * <li><b>Under the Still Sea</b>: a deep bed that grows louder and lower with the water overhead.</li>
 * <li><b>Aboard a Steam Submersible</b> (in any sea): the thrum of its works, and hull creaks that come more often the
 *     deeper it sits.</li>
 * </ul>
 * The numbers are in {@link HollowAmbience}.
 */
@EventBusSubscriber(modid = HollowFrontier.MODID, value = Dist.CLIENT)
public final class HollowSoundscape {
    private HollowSoundscape() {}

    private static final RandomSource RANDOM = RandomSource.create();
    private static DeepBed deepBed;
    private static Thrum thrum;

    @SubscribeEvent
    static void onClientTick(ClientTickEvent.Post event) {
        Minecraft mc = Minecraft.getInstance();
        LocalPlayer player = mc.player;
        if (player == null || mc.level == null || mc.isPaused()) {
            return;
        }
        if (player.getVehicle() instanceof SteamSubmersible sub) {
            aboard(mc, sub);
        }
        if (mc.level.dimension() != HollowEarth.LEVEL) {
            return;
        }
        Holder<net.minecraft.world.level.biome.Biome> biome = mc.level.getBiome(player.blockPosition());
        if (player.isUnderWater() && biome.is(HollowBiomes.STILL_SEA) && (deepBed == null || deepBed.isStopped())) {
            deepBed = new DeepBed(player);
            mc.getSoundManager().play(deepBed);
        }
        boolean call = RANDOM.nextInt(HollowAmbience.FAR_CALL_TICKS) == 0;
        boolean rumble = RANDOM.nextInt(HollowAmbience.RUMBLE_TICKS) == 0;
        if ((call || rumble) && !graysNear(player)) {
            if (call) {
                farCall(mc, player, HollowAmbience.farCalls(biome));
            }
            if (rumble) {
                // From the crust: well overhead, a little off to one side
                play(mc, ModSounds.CRUST_RUMBLE.get(), player.getX() + RANDOM.nextInt(49) - 24, player.getY() + 56,
                        player.getZ() + RANDOM.nextInt(49) - 24, 60.0, 0.8F + RANDOM.nextFloat() * 0.2F);
            }
        }
    }

    /** One call from somewhere out there, a little low and slow for the distance. */
    private static void farCall(Minecraft mc, LocalPlayer player, List<Holder<SoundEvent>> calls) {
        if (calls.isEmpty()) {
            return;
        }
        float angle = RANDOM.nextFloat() * Mth.TWO_PI;
        double distance = HollowAmbience.FAR_MIN + RANDOM.nextDouble() * (HollowAmbience.FAR_MAX - HollowAmbience.FAR_MIN);
        play(mc, calls.get(RANDOM.nextInt(calls.size())).value(), player.getX() + Mth.cos(angle) * distance,
                player.getY() + RANDOM.nextInt(9) - 4, player.getZ() + Mth.sin(angle) * distance, distance, 0.85F + RANDOM.nextFloat() * 0.1F);
    }

    /**
     * Plays a sound {@code distance} blocks off so that it's heard, faintly. A sound carries 16 blocks for each point of
     * volume and fades evenly over that, so carrying 1.43 times the distance leaves it at 0.3 of full when it arrives
     * (it was 1.6 and 0.375 until the user asked for everything 20% quieter, 2026-11-10).
     */
    private static void play(Minecraft mc, SoundEvent sound, double x, double y, double z, double distance, float pitch) {
        float volume = (float) (distance * 1.43 / 16.0);
        mc.getSoundManager().play(new SimpleSoundInstance(sound, SoundSource.AMBIENT, volume, pitch, RANDOM, x, y, z));
    }

    private static boolean graysNear(LocalPlayer player) {
        return !player.level().getEntities(player, player.getBoundingBox().inflate(HollowAmbience.GRAY_HUSH),
                e -> e instanceof GrayProbe || e instanceof GrayWarden || e instanceof GraySaucer).isEmpty();
    }

    /** Riding a submersible: keep its thrum going, and let the hull complain. */
    private static void aboard(Minecraft mc, SteamSubmersible sub) {
        if (thrum == null || thrum.isStopped() || thrum.sub != sub) {
            if (thrum != null) {
                thrum.end();
            }
            thrum = new Thrum(mc.player, sub);
            mc.getSoundManager().play(thrum);
        }
        int depth = sub.depth();
        int ticks = HollowAmbience.creakTicks(depth);
        if (ticks != Integer.MAX_VALUE && RANDOM.nextInt(ticks) == 0) {
            float deep = HollowAmbience.deepness(depth);
            mc.getSoundManager().play(new SimpleSoundInstance(ModSounds.SUBMERSIBLE_CREAK.get(), SoundSource.NEUTRAL, 0.4F + 0.5F * deep,
                    0.75F + RANDOM.nextFloat() * 0.3F, RANDOM, sub.getX() + RANDOM.nextInt(5) - 2, sub.getY() + 1.0, sub.getZ() + RANDOM.nextInt(5) - 2));
        }
    }

    /** Blocks of water over a point, up to the depth past which nothing changes. */
    private static int waterOverhead(Entity entity) {
        BlockPos.MutableBlockPos pos = BlockPos.containing(entity.getX(), entity.getEyeY(), entity.getZ()).mutable();
        int depth = 0;
        while (depth < HollowAmbience.FULL_DEPTH && entity.level().getFluidState(pos).is(FluidTags.WATER)) {
            depth++;
            pos.move(0, 1, 0);
        }
        return depth;
    }

    /** The Still Sea from under it. Comes up over a second or two, follows the depth, and fades when you surface or leave. */
    static final class DeepBed extends AbstractTickableSoundInstance {
        private final LocalPlayer player;

        DeepBed(LocalPlayer player) {
            super(ModSounds.SEA_DEEP.get(), SoundSource.AMBIENT, SoundInstance.createUnseededRandom());
            this.player = player;
            this.looping = true;
            this.delay = 0;
            this.volume = 0.01F;
            this.relative = true;
        }

        @Override
        public void tick() {
            boolean under = !player.isRemoved() && player.isUnderWater() && player.level().dimension() == HollowEarth.LEVEL
                    && player.level().getBiome(player.blockPosition()).is(HollowBiomes.STILL_SEA);
            int depth = under ? waterOverhead(player) : 0;
            float target = under ? HollowAmbience.deepVolume(depth) : 0.0F;
            volume = Mth.approach(volume, target, 0.025F);
            if (under) {
                pitch = Mth.approach(pitch, HollowAmbience.deepPitch(depth), 0.005F);
            } else if (volume <= 0.0F) {
                stop();
            }
        }
    }

    /** The submersible's works, for whoever is aboard: quiet at rest, fuller and a little quicker under way. */
    static final class Thrum extends AbstractTickableSoundInstance {
        private final LocalPlayer player;
        final SteamSubmersible sub;

        Thrum(LocalPlayer player, SteamSubmersible sub) {
            super(ModSounds.SUBMERSIBLE_THRUM.get(), SoundSource.NEUTRAL, SoundInstance.createUnseededRandom());
            this.player = player;
            this.sub = sub;
            this.looping = true;
            this.delay = 0;
            this.volume = 0.01F;
            this.relative = true;
        }

        void end() {
            stop();
        }

        @Override
        public void tick() {
            boolean riding = !player.isRemoved() && !sub.isRemoved() && player.getVehicle() == sub;
            boolean working = riding && sub.steam() > 0;
            float target = !working ? 0.0F : sub.isDriving() ? 0.6F : 0.25F;
            volume = Mth.approach(volume, target, 0.03F);
            pitch = Mth.approach(pitch, sub.isDriving() ? 1.08F : 0.92F, 0.01F);
            if (!riding && volume <= 0.0F) {
                stop();
            }
        }
    }
}
