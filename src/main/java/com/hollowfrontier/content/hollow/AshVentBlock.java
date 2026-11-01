package com.hollowfrontier.content.hollow;

import com.hollowfrontier.registry.ModSounds;

import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;

/**
 * An ash vent (Ember Badlands, 9b1): a crack in the ground over the heat below, smoking all the time, now and then
 * spitting a cinder, hissing. Hot underfoot like magma (sneak to cross it). Found at the top of a little cone of basalt in
 * a field of ash (HollowFeatures); the ash salamanders of 9b2 bask on them.
 */
public class AshVentBlock extends Block {
    public AshVentBlock(Properties properties) {
        super(properties);
    }

    @Override
    public void stepOn(Level level, BlockPos pos, BlockState state, Entity entity) {
        if (!entity.isSteppingCarefully() && entity instanceof LivingEntity) {
            entity.hurt(level.damageSources().hotFloor(), 1.0F);
        }
        super.stepOn(level, pos, state, entity);
    }

    @Override
    public void animateTick(BlockState state, Level level, BlockPos pos, RandomSource random) {
        if (!level.getBlockState(pos.above()).isAir()) {
            return;
        }
        double x = pos.getX() + 0.5 + (random.nextDouble() - 0.5) * 0.4;
        double z = pos.getZ() + 0.5 + (random.nextDouble() - 0.5) * 0.4;
        // A column of smoke you can see across the badlands, like a campfire's signal smoke. A block gets a display tick
        // only about every two seconds, so each one puts out a good puff; the signal smoke lingers and makes the column.
        for (int i = 0; i < 3; i++) {
            level.addAlwaysVisibleParticle(ParticleTypes.CAMPFIRE_SIGNAL_SMOKE, true, x + (random.nextDouble() - 0.5) * 0.3,
                    pos.getY() + 1.1 + i * 0.3, z + (random.nextDouble() - 0.5) * 0.3, 0.0, 0.07, 0.0);
        }
        for (int i = 0; i < 4; i++) {
            level.addParticle(ParticleTypes.LARGE_SMOKE, x, pos.getY() + 1.05, z, (random.nextDouble() - 0.5) * 0.04, 0.05 + random.nextDouble() * 0.04,
                    (random.nextDouble() - 0.5) * 0.04);
        }
        if (random.nextInt(3) == 0) {
            level.addParticle(ParticleTypes.LAVA, x, pos.getY() + 1.0, z, 0.0, 0.0, 0.0);
        }
        if (random.nextInt(6) == 0) {
            level.playLocalSound(pos, ModSounds.ASH_VENT_HISS.get(), SoundSource.BLOCKS, 0.4F + random.nextFloat() * 0.2F,
                    0.8F + random.nextFloat() * 0.3F, false);
        }
    }
}
