package com.hollowfrontier.content.concavia;

import java.util.function.Consumer;

import org.jspecify.annotations.Nullable;

import com.geckolib.animatable.GeoItem;
import com.geckolib.animatable.client.GeoRenderProvider;
import com.geckolib.animatable.instance.AnimatableInstanceCache;
import com.geckolib.animatable.manager.AnimatableManager;
import com.geckolib.animation.AnimationController;
import com.geckolib.animation.RawAnimation;
import com.geckolib.animation.object.PlayState;
import com.geckolib.constant.dataticket.DataTicket;
import com.geckolib.util.GeckoLibUtil;
import com.hollowfrontier.content.frontier.mystery.ManInBlack;
import com.hollowfrontier.content.hollow.creature.GrayProbe;
import com.hollowfrontier.content.hollow.gray.Specimens;
import com.hollowfrontier.content.ufo.GrayWarden;
import com.hollowfrontier.registry.ModCriteria;
import com.hollowfrontier.registry.ModDamageTypes;
import com.hollowfrontier.registry.ModDataComponents;

import net.minecraft.ChatFormatting;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.EntityTypeTags;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.ProjectileUtil;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.TooltipDisplay;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.Vec3;

/**
 * The Sunstone Lens (12b): sunstone ground into a lens and set in starsteel. Held in either hand it lends its holder
 * night vision. Used, it throws a beam of sunlight up to {@link #RANGE} blocks: the first creature in its path takes
 * {@link #DAMAGE} (twice that if it's undead or of the Grays') and catches fire. A point of wear a beam, and
 * {@link #COOLDOWN} ticks between them.
 */
public class SunstoneLensItem extends Item implements GeoItem {
    public static final int DURABILITY = 250;
    public static final double RANGE = 32.0;
    public static final float DAMAGE = 6.0F;
    public static final int COOLDOWN = 30;
    private static final int NIGHT_VISION = 400;
    private static final DustParticleOptions SUNLIGHT = new DustParticleOptions(0xFFE070, 0.7F);
    // Render-state data the client renderer fills in from the stack (its LAST_FIRED component): the game time of its last
    // beam, and ticks since.
    public static final DataTicket<Long> LAST_BEAM_TICK = DataTicket.create("hollowfrontier_last_beam", Long.class);
    public static final DataTicket<Double> SINCE_BEAM = DataTicket.create("hollowfrontier_since_beam", Double.class);

    private static final RawAnimation IDLE = RawAnimation.begin().thenLoop("idle");
    private static final RawAnimation FLARE = RawAnimation.begin().thenPlay("flare").thenLoop("idle");
    private final AnimatableInstanceCache geoCache = GeckoLibUtil.createInstanceCache(this);

    public SunstoneLensItem(Properties properties) {
        super(properties);
    }

    @Override
    public void inventoryTick(ItemStack stack, ServerLevel level, Entity entity, @Nullable EquipmentSlot slot) {
        GeoItem.getOrAssignId(stack, level);
        if ((slot == EquipmentSlot.MAINHAND || slot == EquipmentSlot.OFFHAND) && entity instanceof LivingEntity living && entity.tickCount % 20 == 0) {
            lendSight(living);
        }
    }

    /** Night vision for whoever holds (or, with Curios, wears) the lens; call once a second. */
    public static void lendSight(LivingEntity living) {
        MobEffectInstance sight = living.getEffect(MobEffects.NIGHT_VISION);
        // Topped up long before it runs low, so the screen never flickers as vanilla's does under ten seconds
        if (sight == null || sight.getDuration() < NIGHT_VISION - 100) {
            living.addEffect(new MobEffectInstance(MobEffects.NIGHT_VISION, NIGHT_VISION, 0, true, false, true));
        }
    }

    @Override
    public InteractionResult use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (level instanceof ServerLevel serverLevel) {
            shine(serverLevel, player);
            stack.set(ModDataComponents.LAST_FIRED.get(), level.getGameTime()); // the glass flares (its held model)
            stack.hurtAndBreak(1, player, hand);
            player.getCooldowns().addCooldown(stack, COOLDOWN);
        }
        return InteractionResult.SUCCESS;
    }

    /** Throws the beam from {@code holder}'s eyes along their look; what it struck, if anything. */
    public static @Nullable LivingEntity shine(ServerLevel level, LivingEntity holder) {
        Vec3 eye = holder.getEyePosition();
        Vec3 look = holder.getViewVector(1.0F);
        Vec3 end = level.clip(new ClipContext(eye, eye.add(look.scale(RANGE)), ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, holder)).getLocation();
        EntityHitResult hit = ProjectileUtil.getEntityHitResult(level, holder, eye, end, holder.getBoundingBox().expandTowards(look.scale(RANGE)).inflate(1.0),
                e -> e instanceof LivingEntity && e.isAlive() && !e.isSpectator() && e != holder, 0.3F);
        LivingEntity struck = hit != null ? (LivingEntity) hit.getEntity() : null;
        Vec3 stop = struck != null ? hit.getLocation() : end;
        Vec3 step = stop.subtract(eye);
        int points = (int) Math.max(3, step.length() * 2);
        for (int i = 1; i <= points; i++) {
            Vec3 at = eye.add(step.scale(i / (double) points)).subtract(0, 0.2, 0);
            level.sendParticles(SUNLIGHT, at.x, at.y, at.z, 1, 0, 0, 0, 0);
        }
        level.playSound(null, holder.getX(), holder.getY(), holder.getZ(), SoundEvents.BEACON_POWER_SELECT, SoundSource.PLAYERS, 1.0F, 1.8F);
        if (struck != null) {
            boolean bane = struck.getType().builtInRegistryHolder().is(EntityTypeTags.UNDEAD) || struck instanceof GrayProbe || struck instanceof GrayWarden || struck instanceof ManInBlack || Specimens.isTagged(struck);
            if (struck.hurtServer(level, ModDamageTypes.phlogistonBeam(holder), bane ? DAMAGE * 2 : DAMAGE)) {
                struck.igniteForSeconds(3.0F);
            }
            if (holder instanceof ServerPlayer player) {
                ModCriteria.fire(player, ModCriteria.SUNBEAM_HIT);
            }
        }
        return struck;
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, TooltipDisplay display, Consumer<Component> builder, TooltipFlag flag) {
        builder.accept(Component.translatable("tooltip.hollowfrontier.sunstone_lens").withStyle(ChatFormatting.GRAY));
    }

    // ---------------------------------------------------------------- GeckoLib (held, it's a 3D model; its icon is flat)

    // A beam marks and wears the stack; that shouldn't drop it out of the hand and back, as a new item would
    @Override
    public boolean shouldCauseReequipAnimation(ItemStack oldStack, ItemStack newStack, boolean slotChanged) {
        return slotChanged || !ItemStack.isSameItem(oldStack, newStack);
    }

    @Override
    public void createGeoRenderer(Consumer<GeoRenderProvider> consumer) {
        consumer.accept(new GeoRenderProvider() {
            private com.hollowfrontier.client.LensRenderer renderer;

            @Override
            public com.geckolib.renderer.GeoItemRenderer<?> getGeoItemRenderer() {
                if (renderer == null) {
                    renderer = new com.hollowfrontier.client.LensRenderer(SunstoneLensItem.this);
                }
                return renderer;
            }
        });
    }

    // A fresh beam flares the glass (as a fresh shot recoils a gun); a stack seen for the first time just glows
    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
        long[] seenBeam = {Long.MIN_VALUE};
        controllers.add(new AnimationController<SunstoneLensItem>("lens", 2, test -> {
            long beam = test.getDataOrDefault(LAST_BEAM_TICK, Long.MIN_VALUE);
            if (beam != seenBeam[0]) {
                seenBeam[0] = beam;
                if (test.getDataOrDefault(SINCE_BEAM, 1000.0) < 5) {
                    test.controller().reset();
                    return test.setAndContinue(FLARE);
                }
            }
            if (test.isCurrentAnimation(FLARE)) {
                return PlayState.CONTINUE;
            }
            return test.setAndContinue(IDLE);
        }));
    }

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return geoCache;
    }
}
