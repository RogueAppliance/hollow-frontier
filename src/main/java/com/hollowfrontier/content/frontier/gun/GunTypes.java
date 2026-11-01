package com.hollowfrontier.content.frontier.gun;

import com.hollowfrontier.registry.ModItems;
import com.hollowfrontier.registry.ModSounds;

import net.minecraft.core.Holder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;

// The guns' tuning: the frontier's black powder guns, their weaker mob-held versions, and the phlogiston guns (13b).
public final class GunTypes {
    private GunTypes() {}

    private static final Holder<SoundEvent> BLAST = BuiltInRegistries.SOUND_EVENT.wrapAsHolder(SoundEvents.FIREWORK_ROCKET_BLAST);
    private static final Holder<SoundEvent> LARGE_BLAST = BuiltInRegistries.SOUND_EVENT.wrapAsHolder(SoundEvents.FIREWORK_ROCKET_LARGE_BLAST);

    // Reload stage sounds, played at 0%, 45% and 90% of each loadTicks: pour powder + seat ball, ram, press the cap on.
    public static final Holder<SoundEvent> LOAD_POWDER = SoundEvents.CROSSBOW_LOADING_START;
    public static final Holder<SoundEvent> LOAD_RAM = SoundEvents.CROSSBOW_LOADING_MIDDLE;
    public static final Holder<SoundEvent> LOAD_CAP = BuiltInRegistries.SOUND_EVENT.wrapAsHolder(SoundEvents.STONE_BUTTON_CLICK_ON);

    //                                          mag  ammo                                dmg  pel spread range  cd  load
    // The last number is the headshot multiplier: a rifle ball to the head doubles, the rest take half again.
    // Cap & ball revolver: six chambers, each loaded through the front with the lever rammer.
    public static final GunStats REVOLVER = new GunStats(6, ModItems.REVOLVER_CARTRIDGE, 7.0F, 1, 1.0F, 40.0, 12, 15,
            BLAST, 1.4F, 1.5F);
    // Hawken-style plains rifle: one heavy round ball, rammed down a long barrel.
    public static final GunStats RIFLE = new GunStats(1, ModItems.RIFLE_CARTRIDGE, 18.0F, 1, 0.0F, 100.0, 10, 50,
            BLAST, 0.8F, 2.0F);
    // Side-by-side percussion shotgun: two barrels of buckshot.
    public static final GunStats SHOTGUN = new GunStats(2, ModItems.BUCKSHOT_CARTRIDGE, 3.5F, 8, 7.0F, 20.0, 8, 35,
            LARGE_BLAST, 1.0F, 1.5F);

    // Phlogiston guns (13b, PhlogistonGunItem): a straight beam, no magazine to load (they draw on Phlogiston Cells), never a misfire.
    public static final GunStats RAY_PISTOL = new GunStats(0, ModItems.PHLOGISTON_CELL, 7.0F, 1, 0.0F, 40.0, 10, 0,
            ModSounds.PHLOGISTON_GUN_FIRE, 1.0F, 1.5F);
    // 20 a shot (was 16, under the plains rifle's 18; raised in the phase 14 balance pass)
    public static final GunStats PHLOGISTON_RIFLE = new GunStats(0, ModItems.PHLOGISTON_CELL, 20.0F, 1, 0.0F, 80.0, 30, 0,
            ModSounds.PHLOGISTON_RIFLE_FIRE, 1.0F, 2.0F);

    // Mob-held versions: weaker and less accurate than the player's (bullet damage also scales with difficulty).
    // Mobs reload the whole gun in one pause of magazine * loadTicks.
    public static final GunStats OUTLAW_REVOLVER = new GunStats(6, ModItems.REVOLVER_CARTRIDGE, 3.0F, 1, 4.0F, 24.0, 25, 15,
            BLAST, 1.4F, 1.0F);
    public static final GunStats OUTLAW_RIFLE = new GunStats(1, ModItems.RIFLE_CARTRIDGE, 7.0F, 1, 2.0F, 40.0, 45, 50,
            BLAST, 0.8F, 1.0F);
    public static final GunStats BOSS_SHOTGUN = new GunStats(2, ModItems.BUCKSHOT_CARTRIDGE, 1.5F, 6, 8.0F, 16.0, 30, 35,
            LARGE_BLAST, 1.0F, 1.0F);
}
