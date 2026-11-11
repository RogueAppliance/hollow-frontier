package com.hollowfrontier.content.hollow.sea;

import com.hollowfrontier.registry.ModSounds;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EntitySelector;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

/**
 * Lampjelly (the Still Sea's own small creature, 43b): a bell of pale jelly the size of a pumpkin, lit from inside,
 * drifting in shoals. It goes nowhere in particular, slowly. Touch one and it stings ({@link #sting}): a prick and three
 * seconds of poison, no more often than every two seconds. It drops Lamp Gel.
 */
public class Lampjelly extends SeaBeast {
    public static final float STING_DAMAGE = 1.0F;
    public static final int STING_POISON_TICKS = 60, STING_COOLDOWN = 40;

    private int stingCooldown;

    public Lampjelly(EntityType<? extends Lampjelly> type, Level level) {
        super(type, level, 10);
        xpReward = 1;
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Mob.createMobAttributes()
                .add(Attributes.MAX_HEALTH, 4.0)
                .add(Attributes.MOVEMENT_SPEED, 0.35);
    }

    @Override
    protected void registerGoals() {
        goalSelector.addGoal(4, new SwimAtDepthGoal(this, 1.0, 20, 3, -1, 0.0F)); // anywhere under the top few blocks
    }

    @Override
    protected void travelInWater(Vec3 input, double baseGravity, boolean isFalling, double oldY) {
        super.travelInWater(input, baseGravity, isFalling, oldY);
        // A slow pulse up and a slower sink, so a shoal bobs
        setDeltaMovement(getDeltaMovement().add(0.0, Math.sin((tickCount + getId() * 7) * 0.08) * 0.004, 0.0));
    }

    @Override
    protected void customServerAiStep(ServerLevel level) {
        super.customServerAiStep(level);
        if (stingCooldown > 0) {
            stingCooldown--;
        }
    }

    @Override
    public void playerTouch(Player player) {
        if (level() instanceof ServerLevel level && EntitySelector.NO_CREATIVE_OR_SPECTATOR.test(player)) {
            sting(level, player);
        }
    }

    /** Stings {@code victim} if it's ready to: a prick and a little poison. True if it did. */
    public boolean sting(ServerLevel level, LivingEntity victim) {
        if (stingCooldown > 0 || !isAlive()) {
            return false;
        }
        stingCooldown = STING_COOLDOWN;
        if (!victim.hurtServer(level, damageSources().sting(this), STING_DAMAGE)) {
            return false;
        }
        victim.addEffect(new MobEffectInstance(MobEffects.POISON, STING_POISON_TICKS, 0), this);
        playSound(ModSounds.LAMPJELLY_STING.get(), 0.8F, 1.0F + random.nextFloat() * 0.3F);
        return true;
    }

    @Override
    protected SoundEvent getHurtSound(DamageSource source) {
        return ModSounds.LAMPJELLY_HURT.get();
    }

    @Override
    protected SoundEvent getDeathSound() {
        return ModSounds.LAMPJELLY_DEATH.get();
    }
}
