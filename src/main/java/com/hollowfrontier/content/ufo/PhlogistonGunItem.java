package com.hollowfrontier.content.ufo;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.function.Consumer;

import org.jspecify.annotations.Nullable;

import com.geckolib.animatable.GeoItem;
import com.geckolib.animatable.client.GeoRenderProvider;
import com.hollowfrontier.content.frontier.gun.GunEnchantments;
import com.hollowfrontier.content.frontier.gun.GunItem;
import com.hollowfrontier.content.frontier.gun.GunMechanics;
import com.hollowfrontier.content.frontier.gun.GunStats;
import com.hollowfrontier.registry.ModDamageTypes;
import com.hollowfrontier.registry.ModEntities;
import com.hollowfrontier.registry.ModSounds;

import net.minecraft.ChatFormatting;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.TooltipDisplay;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

/**
 * A phlogiston gun (13b): the Ray Pistol and the Phlogiston Rifle, Gray designs rebuilt by frontier hands in starsteel and brass.
 * They're {@link GunItem}s for their looks (the same GeckoLib renderer, raised-arms pose and recoil) but fire a straight
 * beam of phlogiston (a {@link PhlogistonBeam} of light to see, a phlogiston beam's harm), instead of a ball: no loading, no misfires in the wet, just {@link #cost} charges a
 * shot drawn from the Phlogiston Cells the shooter carries ({@link PhlogistonCellItem#draw}). The rifle's beam goes through up to
 * {@link #pierce} creatures in a line; the pistol's stops at the first. Phlogiston being what it is, a beam sets what it harms
 * alight for {@link #burnSeconds}. Focus is their damage enchantment, as Rifling is the black powder guns'.
 */
public class PhlogistonGunItem extends GunItem {
    private final int cost;
    private final int pierce;
    private final float burnSeconds;
    private final int colour;
    private final Vec3 muzzle;
    private final DustParticleOptions spark;

    /**
     * {@code muzzle} is where the beam leaves the gun, from the shooter's eye: so far along their look, so far to the right,
     * so far down (matching where the gun's emitter sits in first person).
     */
    public PhlogistonGunItem(GunStats stats, int cost, int pierce, float burnSeconds, int colour, Vec3 muzzle, Properties properties) {
        super(stats, properties);
        this.cost = cost;
        this.pierce = pierce;
        this.burnSeconds = burnSeconds;
        this.colour = colour;
        this.muzzle = muzzle;
        this.spark = new DustParticleOptions(colour, 0.8F);
    }

    public int cost() {
        return cost;
    }

    public int pierce() {
        return pierce;
    }

    @Override
    public InteractionResult use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (level instanceof ServerLevel serverLevel) {
            if (!shoot(serverLevel, player, stack, hand)) {
                level.playSound(null, player.getX(), player.getY(), player.getZ(), net.minecraft.sounds.SoundEvents.DISPENSER_FAIL,
                        SoundSource.PLAYERS, 0.6F, 1.6F);
                player.sendOverlayMessage(Component.translatable("message.hollowfrontier.no_charge").withStyle(ChatFormatting.GRAY));
                return InteractionResult.FAIL;
            }
        }
        player.getCooldowns().addCooldown(stack, stats().fireCooldown());
        return InteractionResult.CONSUME;
    }

    /** Fires if the shooter's cells hold the charge: draws it, burns what the beam meets, wears the gun. Whether it fired. */
    public boolean shoot(ServerLevel level, Player shooter, ItemStack stack, InteractionHand hand) {
        if (!PhlogistonCellItem.draw(shooter, cost)) {
            return false;
        }
        beam(level, shooter, stack);
        markFired(stack, level.getGameTime());
        stack.hurtAndBreak(1, shooter, hand);
        return true;
    }

    /** The beam from the shooter's eye along their look, to the first block; what it struck, nearest first. */
    public List<LivingEntity> beam(ServerLevel level, LivingEntity shooter) {
        return beam(level, shooter, ItemStack.EMPTY);
    }

    /** As above, from {@code gun}, whose enchantments (Deadeye) count. */
    public List<LivingEntity> beam(ServerLevel level, LivingEntity shooter, ItemStack gun) {
        GunStats stats = GunEnchantments.apply(stats(), gun, level);
        Vec3 eye = shooter.getEyePosition();
        Vec3 look = shooter.getViewVector(1.0F);
        Vec3 end = level.clip(new ClipContext(eye, eye.add(look.scale(stats.range())), ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, shooter)).getLocation();
        List<LivingEntity> struck = new ArrayList<>();
        AABB along = shooter.getBoundingBox().expandTowards(end.subtract(eye)).inflate(1.0);
        for (LivingEntity target : level.getEntitiesOfClass(LivingEntity.class, along, e -> e != shooter && e.isAlive() && e.isPickable() && !e.isSpectator()
                && !shooter.isPassengerOfSameVehicle(e) && e.getBoundingBox().inflate(0.3).clip(eye, end).isPresent())) {
            struck.add(target);
        }
        struck.sort(Comparator.comparingDouble(e -> e.distanceToSqr(eye)));
        if (struck.size() > pierce) {
            struck = new ArrayList<>(struck.subList(0, pierce));
        }
        Vec3 stop = pierce == 1 && !struck.isEmpty() ? struck.getFirst().getBoundingBox().getCenter() : end;
        // The beam itself (PhlogistonBeam, a shaft of light from the gun's muzzle, held low at the right hand), and a spark where it stops
        Vec3 right = look.cross(new Vec3(0, 1, 0)).normalize();
        Vec3 from = eye.add(look.scale(muzzle.x)).add(right.scale(muzzle.y)).add(0, -muzzle.z, 0);
        PhlogistonBeam shot = ModEntities.PHLOGISTON_BEAM.get().create(level, EntitySpawnReason.EVENT);
        shot.aim(from, stop, colour);
        level.addFreshEntity(shot);
        level.sendParticles(spark, stop.x, stop.y, stop.z, 6, 0.1, 0.1, 0.1, 0);
        for (LivingEntity target : struck) {
            // Where the beam went in, for a headshot
            Vec3 hit = target.getBoundingBox().inflate(0.3).clip(eye, end).orElse(target.getBoundingBox().getCenter());
            float headshot = GunMechanics.headshot(shooter, target, hit, stats);
            if (target.hurtServer(level, ModDamageTypes.phlogistonBeam(shooter), stats.damage() * headshot)) {
                target.igniteForSeconds(burnSeconds);
            }
            if (headshot > 1.0F) {
                GunMechanics.headshotFeedback(level, shooter, hit);
            }
        }
        level.playSound(null, shooter.getX(), shooter.getY(), shooter.getZ(), stats.fireSound(), SoundSource.PLAYERS, 1.0F,
                stats.firePitch() + (shooter.getRandom().nextFloat() - 0.5F) * 0.06F);
        return struck;
    }

    /** A ray pistol's shot (the Warden's too), at {@code pitch}. */
    public static void fireSound(ServerLevel level, Entity shooter, float pitch, SoundSource source) {
        level.playSound(null, shooter.getX(), shooter.getY(), shooter.getZ(), ModSounds.PHLOGISTON_GUN_FIRE.get(), source, 1.0F, pitch);
    }

    /** Nothing to load. */
    @Override
    public boolean startReload(Player player, ItemStack stack) {
        return false;
    }

    @Override
    public void inventoryTick(ItemStack stack, ServerLevel level, Entity owner, @Nullable EquipmentSlot slot) {
        GeoItem.getOrAssignId(stack, level);
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, TooltipDisplay display, Consumer<Component> builder, TooltipFlag flag) {
        builder.accept(damageLine(stack, context));
        builder.accept(Component.translatable("tooltip.hollowfrontier.phlogiston_gun", cost).withStyle(ChatFormatting.GRAY));
        if (pierce > 1) {
            builder.accept(Component.translatable("tooltip.hollowfrontier.phlogiston_gun.pierce", pierce).withStyle(ChatFormatting.DARK_GRAY));
        }
        builder.accept(Component.translatable("tooltip.hollowfrontier.phlogiston_gun.burn").withStyle(ChatFormatting.DARK_GRAY));
    }

    @Override
    public void createGeoRenderer(Consumer<GeoRenderProvider> consumer) {
        consumer.accept(new GeoRenderProvider() {
            private com.hollowfrontier.client.GunRenderer renderer;

            @Override
            public com.geckolib.renderer.GeoItemRenderer<?> getGeoItemRenderer() {
                if (renderer == null) {
                    renderer = com.hollowfrontier.client.GunRenderer.glowing(PhlogistonGunItem.this);
                }
                return renderer;
            }
        });
    }
}
