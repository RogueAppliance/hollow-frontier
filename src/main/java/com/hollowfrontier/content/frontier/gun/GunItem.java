package com.hollowfrontier.content.frontier.gun;

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
import com.hollowfrontier.content.frontier.outlaw.AbstractOutlaw;
import com.hollowfrontier.registry.ModCriteria;
import com.hollowfrontier.registry.ModDataComponents;
import com.hollowfrontier.registry.ModItems;
import com.hollowfrontier.registry.ModTags;

import net.minecraft.ChatFormatting;
import net.minecraft.core.Holder;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.item.component.TooltipDisplay;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

/**
 * Black powder muzzleloader. Right-click fires a loaded chamber/barrel. Loading is one chamber at a time:
 * right-click when empty, sneak + right-click to top up, or press the reload key (again to stop). Each chamber
 * takes loadTicks and uses one cartridge + one percussion cap; firing or putting the gun away stops loading
 * (chambers already loaded stay loaded). Wet powder can misfire in rain and always does underwater.
 *
 * State lives in components: LOADED_AMMO, RELOAD_START (game time the current chamber started) and LAST_FIRED.
 * The GeckoLib model animates from those (see client/GunRenderer), so the server never sends animation packets.
 */
public class GunItem extends Item implements GeoItem {
    public static final float RAIN_MISFIRE_CHANCE = 0.2F;

    // Render-state data the client renderer fills in from the stack; read by the animation controller.
    public static final DataTicket<Double> RELOAD_TICKS = DataTicket.create("hollowfrontier_reload_ticks", Double.class);
    public static final DataTicket<Long> LAST_FIRED_TICK = DataTicket.create("hollowfrontier_last_fired", Long.class);
    public static final DataTicket<Double> SINCE_FIRED = DataTicket.create("hollowfrontier_since_fired", Double.class);

    private static final RawAnimation IDLE = RawAnimation.begin().thenLoop("idle");
    private static final RawAnimation FIRE = RawAnimation.begin().thenPlay("fire").thenLoop("idle");
    private static final RawAnimation RELOAD = RawAnimation.begin().thenLoop("reload");

    private final GunStats stats;
    private final AnimatableInstanceCache geoCache = GeckoLibUtil.createInstanceCache(this);

    public GunItem(GunStats stats, Properties properties) {
        super(properties);
        this.stats = stats;
    }

    public GunStats stats() {
        return stats;
    }

    public static int getAmmo(ItemStack stack) {
        return stack.getOrDefault(ModDataComponents.LOADED_AMMO.get(), 0);
    }

    public static void setAmmo(ItemStack stack, int ammo) {
        stack.set(ModDataComponents.LOADED_AMMO.get(), ammo);
    }

    public static boolean isReloading(ItemStack stack) {
        return stack.has(ModDataComponents.RELOAD_START.get());
    }

    /** Starts (or, with null, stops) the reload animation without touching ammo. Mobs use this for their reload pause. */
    public static void setReloadStart(ItemStack stack, @Nullable Long gameTime) {
        stack.set(ModDataComponents.RELOAD_START.get(), gameTime);
    }

    public static void markFired(ItemStack stack, long gameTime) {
        stack.set(ModDataComponents.LAST_FIRED.get(), gameTime);
    }

    @Override
    public InteractionResult use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        int ammo = getAmmo(stack);
        if (ammo <= 0 || (player.isShiftKeyDown() && ammo < stats.magazine())) {
            if (isReloading(stack)) {
                return InteractionResult.CONSUME;
            }
            return startReload(player, stack) ? InteractionResult.CONSUME : InteractionResult.FAIL;
        }
        if (level instanceof ServerLevel serverLevel) {
            setReloadStart(stack, null);
            if (!player.getAbilities().instabuild) {
                setAmmo(stack, ammo - 1);
            }
            if (misfires(player, stack)) {
                Vec3 muzzle = player.getEyePosition().add(player.getViewVector(1.0F).scale(0.8));
                serverLevel.sendParticles(ParticleTypes.SMOKE, muzzle.x, muzzle.y - 0.1, muzzle.z, 3, 0.03, 0.03, 0.03, 0.005);
                level.playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.FIRE_EXTINGUISH, SoundSource.PLAYERS, 0.5F, 1.6F);
                player.sendOverlayMessage(Component.translatable("message.hollowfrontier.misfire").withStyle(ChatFormatting.GRAY));
            } else {
                GunMechanics.fire(serverLevel, player, GunEnchantments.apply(stats, stack, level));
                markFired(stack, level.getGameTime());
            }
            stack.hurtAndBreak(1, player, hand);
        }
        player.getCooldowns().addCooldown(stack, stats.fireCooldown());
        return InteractionResult.CONSUME;
    }

    private static boolean misfires(LivingEntity shooter, ItemStack gun) {
        if (shooter.isUnderWater()) {
            return true;
        }
        return shooter.isInWaterOrRain() && !GunEnchantments.keepsPowderDry(gun, shooter.level()) && shooter.getRandom().nextFloat() < RAIN_MISFIRE_CHANCE;
    }

    /** Begins loading the next chamber. Returns false if full, already loading, or out of cartridges/caps. */
    public boolean startReload(Player player, ItemStack stack) {
        if (getAmmo(stack) >= stats.magazine() || isReloading(stack) || !hasAmmo(player)) {
            return false;
        }
        if (!player.level().isClientSide()) {
            setReloadStart(stack, player.level().getGameTime());
            playLoadSound(player, GunTypes.LOAD_POWDER);
            AbstractOutlaw.onPlayerReload(player);
        }
        return true;
    }

    public void stopReload(ItemStack stack) {
        setReloadStart(stack, null);
    }

    /**
     * Advances loading by game time: plays the stage sounds and, once loadTicks have passed since the chamber
     * started, loads it and moves on to the next one (or stops when full / out of ammo).
     * Public so game tests can drive it (fake players don't tick their inventory).
     */
    public void tickReload(Player player, ItemStack stack) {
        Long start = stack.get(ModDataComponents.RELOAD_START.get());
        if (start == null) {
            return;
        }
        long elapsed = player.level().getGameTime() - start;
        int load = GunEnchantments.apply(stats, stack, player.level()).loadTicks();
        if (elapsed == load * 45L / 100) {
            playLoadSound(player, GunTypes.LOAD_RAM);
        } else if (elapsed == load * 90L / 100) {
            playLoadSound(player, GunTypes.LOAD_CAP);
        }
        if (elapsed < load) {
            return;
        }
        if (!takeRound(player, stack)) {
            stopReload(stack);
            return;
        }
        int ammo = getAmmo(stack) + 1;
        setAmmo(stack, ammo);
        ModCriteria.fire(player, ModCriteria.GUN_LOADED, ammo);
        player.sendOverlayMessage(Component.translatable("message.hollowfrontier.loaded", ammo, stats.magazine()).withStyle(ChatFormatting.GRAY));
        if (ammo >= stats.magazine() || !hasAmmo(player)) {
            stopReload(stack);
        } else {
            setReloadStart(stack, player.level().getGameTime());
            playLoadSound(player, GunTypes.LOAD_POWDER);
        }
    }

    private void playLoadSound(Player player, Holder<SoundEvent> sound) {
        player.level().playSound(null, player.getX(), player.getY(), player.getZ(), sound, SoundSource.PLAYERS, 0.6F, 1.1F);
    }

    @Override
    public void inventoryTick(ItemStack stack, ServerLevel level, Entity owner, @Nullable EquipmentSlot slot) {
        GeoItem.getOrAssignId(stack, level);
        if (!isReloading(stack) || !(owner instanceof Player player)) {
            return;
        }
        // Loading needs both hands on the gun: stop if it isn't held.
        if (slot != EquipmentSlot.MAINHAND && slot != EquipmentSlot.OFFHAND) {
            stopReload(stack);
            return;
        }
        tickReload(player, stack);
    }

    private boolean hasAmmo(Player player) {
        return player.getAbilities().instabuild
                || (count(player, stats.ammo().get()) > 0 && count(player, ModItems.PERCUSSION_CAP.get()) > 0);
    }

    // Uses one cartridge and one cap (creative, an Infinity gun, or a Frugal one now and then: free). Returns false if either
    // is missing: like a bow with Infinity, the gun still wants one of each to hand.
    private boolean takeRound(Player player, ItemStack gun) {
        if (player.getAbilities().instabuild) {
            return true;
        }
        if (!hasAmmo(player)) {
            return false;
        }
        if (GunEnchantments.endlessAmmo(gun, player.level()) || GunEnchantments.savesRound(gun, player.level(), player.getRandom())) {
            return true;
        }
        take(player, stats.ammo().get());
        take(player, ModItems.PERCUSSION_CAP.get());
        return true;
    }

    private static int count(Player player, Item item) {
        Inventory inventory = player.getInventory();
        int n = 0;
        for (int slot = 0; slot < inventory.getContainerSize(); slot++) {
            ItemStack candidate = inventory.getItem(slot);
            if (candidate.is(item)) {
                n += candidate.getCount();
            }
        }
        return n;
    }

    private static void take(Player player, Item item) {
        Inventory inventory = player.getInventory();
        for (int slot = 0; slot < inventory.getContainerSize(); slot++) {
            ItemStack candidate = inventory.getItem(slot);
            if (candidate.is(item)) {
                candidate.shrink(1);
                return;
            }
        }
    }

    // Vanilla's Infinity is a bow's (its supported items are #enchantable/bow, and that tag would bring Power, Punch and Flame
    // with it), so the black powder guns take it here: on the table, at the anvil and from random loot.
    @Override
    public boolean supportsEnchantment(ItemStack stack, Holder<Enchantment> enchantment) {
        return takesInfinity(stack, enchantment) || super.supportsEnchantment(stack, enchantment);
    }

    @Override
    public boolean isPrimaryItemFor(ItemStack stack, Holder<Enchantment> enchantment) {
        return takesInfinity(stack, enchantment) || super.isPrimaryItemFor(stack, enchantment);
    }

    private static boolean takesInfinity(ItemStack stack, Holder<Enchantment> enchantment) {
        return enchantment.is(Enchantments.INFINITY) && stack.is(ModTags.Items.ENCHANTABLE_MUZZLELOADER);
    }

    // Ammo and reload progress change every few ticks; don't replay the equip animation for them.
    @Override
    public boolean shouldCauseReequipAnimation(ItemStack oldStack, ItemStack newStack, boolean slotChanged) {
        return slotChanged || !ItemStack.isSameItem(oldStack, newStack);
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, TooltipDisplay display, Consumer<Component> builder, TooltipFlag flag) {
        builder.accept(damageLine(stack, context));
        builder.accept(Component.translatable("tooltip.hollowfrontier.ammo", getAmmo(stack), stats.magazine()).withStyle(ChatFormatting.GRAY));
        builder.accept(Component.translatable("tooltip.hollowfrontier.uses_ammo", Component.translatable(stats.ammo().get().getDescriptionId()),
                Component.translatable(ModItems.PERCUSSION_CAP.get().getDescriptionId())).withStyle(ChatFormatting.DARK_GRAY));
        builder.accept(Component.translatable("tooltip.hollowfrontier.reload_hint").withStyle(ChatFormatting.DARK_GRAY));
    }

    /** What this gun hits for, its enchantments counted: the plain number to set one gun against another. */
    protected Component damageLine(ItemStack stack, TooltipContext context) {
        Level level = context.level();
        GunStats shown = level == null ? stats : GunEnchantments.apply(stats, stack, level);
        Component line = shown.pellets() > 1
                ? Component.translatable("tooltip.hollowfrontier.gun_damage.pellets", number(shown.damage()), shown.pellets())
                : Component.translatable("tooltip.hollowfrontier.gun_damage", number(shown.damage()));
        return line.copy().withStyle(ChatFormatting.GRAY);
    }

    // 30, not 30.0; 11.2 as it stands
    private static String number(float value) {
        return value == Math.round(value) ? Integer.toString(Math.round(value)) : String.format(java.util.Locale.ROOT, "%.1f", value);
    }

    // ---------------------------------------------------------------- GeckoLib

    @Override
    public void createGeoRenderer(Consumer<GeoRenderProvider> consumer) {
        consumer.accept(new GeoRenderProvider() {
            private com.hollowfrontier.client.GunRenderer renderer;

            @Override
            public com.geckolib.renderer.GeoItemRenderer<?> getGeoItemRenderer() {
                if (renderer == null) {
                    renderer = new com.hollowfrontier.client.GunRenderer(GunItem.this);
                }
                return renderer;
            }
        });
    }

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
        long[] seenShot = {Long.MIN_VALUE};
        controllers.add(new AnimationController<GunItem>("gun", 3, test -> {
            if (test.getDataOrDefault(RELOAD_TICKS, -1.0) >= 0) {
                return test.setAndContinue(RELOAD);
            }
            long shot = test.getDataOrDefault(LAST_FIRED_TICK, Long.MIN_VALUE);
            if (shot != seenShot[0]) {
                seenShot[0] = shot;
                // Only a fresh shot replays the recoil; a stack seen for the first time just idles.
                if (test.getDataOrDefault(SINCE_FIRED, 1000.0) < 5) {
                    test.controller().reset();
                    return test.setAndContinue(FIRE);
                }
            }
            if (test.isCurrentAnimation(FIRE)) {
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
