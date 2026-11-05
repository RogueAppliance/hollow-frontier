package com.hollowfrontier.content.frontier.gun;

import java.util.function.Supplier;

import net.minecraft.core.Holder;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.item.Item;

/**
 * Tuning for one muzzleloader. Damage is per pellet; spread is the max cone half-angle in degrees.
 * magazine = chambers/barrels. loadTicks = time to load one of them (powder, ball, ram, cap); it is also
 * the length of the item's looping "reload" animation, so keep the two in sync.
 * headshot = damage multiplier for a player's shot that lands at the head (1 = none; mob shots never get it).
 * Sounds are vanilla stand-ins until real gun sounds exist (see ASSETS.md).
 */
public record GunStats(
        int magazine,
        Supplier<? extends Item> ammo,
        float damage,
        int pellets,
        float spreadDegrees,
        double range,
        int fireCooldown,
        int loadTicks,
        Holder<SoundEvent> fireSound,
        float firePitch,
        float headshot) {
    /** A shot's damage with every pellet on target. */
    public float shotDamage() {
        return damage * pellets;
    }

    /**
     * Damage a second kept up over a whole magazine, fired and loaded again, with every pellet on target: each round costs
     * its cooldown and its load, whatever the magazine holds. For a phlogiston gun, with nothing to load, it's only the cooldown.
     */
    public float damagePerSecond() {
        return shotDamage() * 20.0F / (fireCooldown + loadTicks);
    }
}
