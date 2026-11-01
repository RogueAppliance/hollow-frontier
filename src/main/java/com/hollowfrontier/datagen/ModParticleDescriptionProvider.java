package com.hollowfrontier.datagen;

import com.hollowfrontier.registry.ModParticles;

import net.minecraft.data.PackOutput;
import net.minecraft.resources.Identifier;
import net.neoforged.neoforge.client.data.ParticleDescriptionProvider;

// Which sprites each particle of ours is drawn with: assets/hollowfrontier/particles/*.json. Vanilla's own sprites only.
public class ModParticleDescriptionProvider extends ParticleDescriptionProvider {
    public ModParticleDescriptionProvider(PackOutput output) {
        super(output);
    }

    @Override
    protected void addDescriptions() {
        // 29: a dust devil's motes shrink through vanilla's dust sprites (generic_7 down to generic_0), as its dust does
        spriteSet(ModParticles.DUST_DEVIL_MOTE.get(), Identifier.withDefaultNamespace("generic"), 8, true);
    }
}
