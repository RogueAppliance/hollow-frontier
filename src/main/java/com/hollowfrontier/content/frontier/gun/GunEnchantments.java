package com.hollowfrontier.content.frontier.gun;

import com.hollowfrontier.registry.ModEnchantments;

import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.level.Level;

/**
 * What the gun enchantments (22) do to a gun's tuning. Only a player's gun is ever enchanted; mobs fire their own fixed
 * stats (GunTypes.OUTLAW_*), so none of this reaches them.
 */
public final class GunEnchantments {
    private GunEnchantments() {}

    public static final float RIFLING_PER_LEVEL = 0.15F;
    public static final float QUICK_LOAD_PER_LEVEL = 0.2F;
    public static final float DEADEYE_PER_LEVEL = 0.25F;
    public static final float CHOKE_PER_LEVEL = 0.25F;
    public static final float FRUGAL_PER_LEVEL = 0.15F;
    public static final float FOCUS_PER_LEVEL = 0.15F;

    /** {@code base} as this gun shoots it: Rifling's and Focus's damage, Choke's spread, Quick Load's loading and Deadeye's headshots. */
    public static GunStats apply(GunStats base, ItemStack gun, Level level) {
        int rifling = ModEnchantments.level(level, ModEnchantments.RIFLING, gun);
        int quickLoad = ModEnchantments.level(level, ModEnchantments.QUICK_LOAD, gun);
        int deadeye = ModEnchantments.level(level, ModEnchantments.DEADEYE, gun);
        int choke = ModEnchantments.level(level, ModEnchantments.CHOKE, gun);
        int focus = ModEnchantments.level(level, ModEnchantments.FOCUS, gun);
        if (rifling + quickLoad + deadeye + choke + focus == 0) {
            return base;
        }
        return new GunStats(base.magazine(), base.ammo(),
                base.damage() * (1.0F + RIFLING_PER_LEVEL * rifling + FOCUS_PER_LEVEL * focus),
                base.pellets(),
                base.spreadDegrees() * Math.max(0.0F, 1.0F - CHOKE_PER_LEVEL * choke),
                base.range(), base.fireCooldown(),
                base.loadTicks() == 0 ? 0 : Math.max(1, Mth.ceil(base.loadTicks() * Math.max(0.0F, 1.0F - QUICK_LOAD_PER_LEVEL * quickLoad))),
                base.fireSound(), base.firePitch(),
                base.headshot() + DEADEYE_PER_LEVEL * deadeye);
    }

    /** Dry Powder: the rain can't get at the charge (a gun underwater still fails). */
    public static boolean keepsPowderDry(ItemStack gun, Level level) {
        return ModEnchantments.level(level, ModEnchantments.DRY_POWDER, gun) > 0;
    }

    /** Vanilla Infinity on a black powder gun (GunItem lets it on): loading never spends a cartridge or a cap. */
    public static boolean endlessAmmo(ItemStack gun, Level level) {
        return ModEnchantments.level(level, Enchantments.INFINITY, gun) > 0;
    }

    /** Frugal: whether loading this chamber costs nothing. */
    public static boolean savesRound(ItemStack gun, Level level, RandomSource random) {
        int frugal = ModEnchantments.level(level, ModEnchantments.FRUGAL, gun);
        return frugal > 0 && random.nextFloat() < FRUGAL_PER_LEVEL * frugal;
    }
}
