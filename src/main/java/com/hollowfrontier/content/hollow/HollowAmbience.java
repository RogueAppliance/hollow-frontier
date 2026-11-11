package com.hollowfrontier.content.hollow;

import java.util.List;

import com.hollowfrontier.registry.ModSounds;
import com.hollowfrontier.worldgen.HollowBiomes;

import net.minecraft.core.Holder;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.util.Mth;
import net.minecraft.world.level.biome.Biome;

/**
 * The numbers behind the Hollow Earth's soundscape (45b), kept apart from the client class that plays it
 * ({@code client/HollowSoundscape}) so they can be tested: which creatures are heard far off in which biome, and how
 * depth changes what the sea and a submersible sound like.
 */
public final class HollowAmbience {
    private HollowAmbience() {}

    /** Mean ticks between far-off calls (a minute and a half), and between rumbles from the crust (six minutes). */
    public static final int FAR_CALL_TICKS = 1800;
    public static final int RUMBLE_TICKS = 7200;
    /** How far off a call is placed. */
    public static final double FAR_MIN = 40.0;
    public static final double FAR_MAX = 80.0;
    /** Gray machines within this many blocks hush the country round them: nothing calls, nothing rumbles. */
    public static final double GRAY_HUSH = 64.0;
    /** Blocks of water overhead at which the sea is as dark as it gets, and a hull creaks as often as it will. */
    public static final int FULL_DEPTH = 30;

    /**
     * What's heard from a long way off in a biome: only creatures that live there (the megafauna's biome tags, and the
     * sea's own), and only their big voices. Empty outside the Hollow Earth.
     */
    public static List<Holder<SoundEvent>> farCalls(Holder<Biome> biome) {
        if (biome.is(HollowBiomes.PRIMORDIAL_JUNGLE)) {
            return List.of(ModSounds.MEGATHERIUM_AMBIENT);
        } else if (biome.is(HollowBiomes.CRYSTAL_STEPPES)) {
            return List.of(ModSounds.MAMMOTH_TRUMPET, ModSounds.MAMMOTH_TRUMPET, ModSounds.TERROR_BIRD_SCREECH);
        } else if (biome.is(HollowBiomes.EMBER_BADLANDS)) {
            return List.of(ModSounds.TERROR_BIRD_SCREECH);
        } else if (biome.is(HollowBiomes.CONCAVIAN_HIGHLANDS)) {
            return List.of(ModSounds.MAMMOTH_TRUMPET, ModSounds.PALE_RAVEN_AMBIENT);
        } else if (biome.is(HollowBiomes.STILL_SEA)) {
            return List.of(ModSounds.BASILOSAURUS_AMBIENT, ModSounds.BASILOSAURUS_AMBIENT, ModSounds.PLESIOSAUR_AMBIENT, ModSounds.SEA_COW_AMBIENT);
        }
        return List.of();
    }

    /** 0 at the surface to 1 at {@link #FULL_DEPTH} blocks of water overhead and below. */
    public static float deepness(int waterOverhead) {
        return Mth.clamp(waterOverhead / (float) FULL_DEPTH, 0.0F, 1.0F);
    }

    /** The deep bed under water: louder and lower the deeper you are. */
    public static float deepVolume(int waterOverhead) {
        return 0.35F + 0.65F * deepness(waterOverhead);
    }

    public static float deepPitch(int waterOverhead) {
        return 1.0F - 0.25F * deepness(waterOverhead);
    }

    /**
     * Mean ticks between a submersible's hull creaks: none while it's awash, one in thirty seconds just under, one in six
     * at depth.
     */
    public static int creakTicks(int waterOverhead) {
        return waterOverhead <= 0 ? Integer.MAX_VALUE : Math.round(Mth.lerp(deepness(waterOverhead), 600.0F, 120.0F));
    }
}
