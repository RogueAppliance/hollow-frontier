package com.hollowfrontier.content.frontier.mystery;

import com.geckolib.animatable.GeoEntity;
import com.geckolib.animatable.instance.AnimatableInstanceCache;
import com.geckolib.animatable.manager.AnimatableManager;
import com.geckolib.util.GeckoLibUtil;
import com.hollowfrontier.registry.ModItems;

import net.minecraft.ChatFormatting;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.Vec3;

/**
 * What a cattle mutilation leaves (11a): a steer on its side, cut with a surgeon's neatness (an eye and a square of hide
 * gone, no blood anywhere), in a ring of scorched grass. The cuts glow faintly, and at night a mote of light now and
 * then rises off it: the faint trace the Grays leave. Look closer (use it) and you're told as much. Break it up and it
 * gives leather, and sometimes a Gray tech fragment from somewhere inside. It rots away after {@link #LIFETIME} ticks.
 */
public class CattleCarcass extends Entity implements GeoEntity {
    public static final int LIFETIME = 3 * 24000;
    /** The chance breaking it up turns up a Gray tech fragment. */
    public static final float FRAGMENT_CHANCE = 0.15F;

    private final AnimatableInstanceCache geoCache = GeckoLibUtil.createInstanceCache(this);
    private int age;

    public CattleCarcass(EntityType<? extends CattleCarcass> type, Level level) {
        super(type, level);
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {}

    @Override
    public void tick() {
        super.tick();
        if (level() instanceof ServerLevel level) { // it falls and settles on the server only (26.3 won't move it on the client)
            if (!isNoGravity()) {
                setDeltaMovement(getDeltaMovement().add(0, -0.04, 0));
            }
            move(MoverType.SELF, getDeltaMovement());
            setDeltaMovement(getDeltaMovement().multiply(0.5, 0.98, 0.5));
            if (onGround()) {
                setDeltaMovement(getDeltaMovement().multiply(0.7, -0.5, 0.7));
            }
            if (++age > LIFETIME) {
                discard();
                return;
            }
            if (tickCount % 60 == 0 && level.isDarkOutside()) {
                level.sendParticles(ParticleTypes.END_ROD, getX(), getY() + 0.6, getZ(), 1, 0.4, 0.1, 0.4, 0.01);
            }
        }
    }

    /** Looking it over. */
    @Override
    public InteractionResult interact(Player player, InteractionHand hand, Vec3 at) {
        if (!level().isClientSide()) {
            player.sendSystemMessage(Component.translatable("message.hollowfrontier.carcass.look").withStyle(ChatFormatting.GRAY, ChatFormatting.ITALIC));
        }
        return InteractionResult.SUCCESS;
    }

    /** Broken up by a blow: leather, maybe a fragment. */
    @Override
    public boolean hurtServer(ServerLevel level, DamageSource source, float amount) {
        if (isRemoved() || !(source.getEntity() instanceof Player)) {
            return false;
        }
        spawnAtLocation(level, new ItemStack(Items.LEATHER, 1 + random.nextInt(2)));
        if (random.nextFloat() < FRAGMENT_CHANCE) {
            spawnAtLocation(level, new ItemStack(ModItems.GRAY_TECH_FRAGMENT.get()));
        }
        playSound(SoundEvents.HONEY_BLOCK_BREAK, 1.0F, 0.6F);
        discard();
        return true;
    }

    @Override
    public boolean isPickable() {
        return true;
    }

    @Override
    public boolean canBeCollidedWith(Entity other) {
        return false;
    }

    @Override
    protected void readAdditionalSaveData(ValueInput input) {
        age = input.getIntOr("Age", 0);
    }

    @Override
    protected void addAdditionalSaveData(ValueOutput output) {
        output.putInt("Age", age);
    }

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {}

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return geoCache;
    }
}
