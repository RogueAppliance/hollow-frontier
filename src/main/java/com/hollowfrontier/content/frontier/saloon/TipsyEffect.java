package com.hollowfrontier.content.frontier.saloon;

import com.hollowfrontier.registry.ModMobEffects;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;

/**
 * Tipsy I-IV from saloon liquor. Each level widens gun spread (GunMechanics), and from Tipsy III the world
 * starts to sway in short bouts of nausea. Stacking and the blackout past Tipsy IV live in {@link Drink#drink}.
 */
public class TipsyEffect extends MobEffect {
    public static final int MAX_AMPLIFIER = 3;
    /** Extra gun spread, in degrees, per Tipsy level. */
    public static final float SPREAD_PER_LEVEL = 2.5F;
    // Each level takes this long to wear off; the whole effect lasts levels * this.
    public static final int TICKS_PER_LEVEL = 1200;

    public TipsyEffect() {
        super(MobEffectCategory.NEUTRAL, 0xC8862A);
    }

    /** Extra spread for a shooter, 0 when sober. */
    public static float extraSpread(LivingEntity shooter) {
        MobEffectInstance tipsy = shooter.getEffect(ModMobEffects.TIPSY);
        return tipsy == null ? 0.0F : (tipsy.getAmplifier() + 1) * SPREAD_PER_LEVEL;
    }

    @Override
    public boolean shouldApplyEffectTickThisTick(int tickCount, int amplifier) {
        return tickCount % 20 == 0;
    }

    @Override
    public boolean applyEffectTick(ServerLevel level, LivingEntity mob, int amplifier) {
        // Tipsy III+: about one bout of swaying every 6 seconds, longer the drunker you are.
        if (amplifier >= 2 && !mob.hasEffect(MobEffects.NAUSEA) && mob.getRandom().nextInt(6) == 0) {
            mob.addEffect(new MobEffectInstance(MobEffects.NAUSEA, 100 + 40 * (amplifier - 2), 0, false, false, true));
        }
        return true;
    }
}
