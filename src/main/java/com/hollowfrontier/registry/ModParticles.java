package com.hollowfrontier.registry;

import com.hollowfrontier.HollowFrontier;
import com.mojang.serialization.MapCodec;

import net.minecraft.core.particles.ColorParticleOption;
import net.minecraft.core.particles.ParticleType;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

/** The mod's particle types. Their sprites are listed by ModParticleDescriptionProvider; the client draws them (ModEntityRenderers). */
public final class ModParticles {
    private ModParticles() {}

    public static final DeferredRegister<ParticleType<?>> PARTICLES = DeferredRegister.create(Registries.PARTICLE_TYPE, HollowFrontier.MODID);

    /**
     * 29: a mote of a dust devil's dust, in a colour, that goes round and up the devil it belongs to (its entity id is
     * passed as the x speed) instead of drifting off on its own like vanilla's dust.
     */
    public static final DeferredHolder<ParticleType<?>, ParticleType<ColorParticleOption>> DUST_DEVIL_MOTE = PARTICLES.register("dust_devil_mote",
            () -> new ParticleType<ColorParticleOption>(false) {
                @Override
                public MapCodec<ColorParticleOption> codec() {
                    return ColorParticleOption.codec(this);
                }

                @Override
                public StreamCodec<? super RegistryFriendlyByteBuf, ColorParticleOption> streamCodec() {
                    return ColorParticleOption.streamCodec(this);
                }
            });
}
