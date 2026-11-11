package com.hollowfrontier.datagen;

import com.hollowfrontier.HollowFrontier;
import com.hollowfrontier.registry.ModSounds;

import net.minecraft.data.PackOutput;
import net.minecraft.resources.Identifier;
import net.minecraft.sounds.SoundEvent;
import net.neoforged.neoforge.common.data.SoundDefinition;
import net.neoforged.neoforge.common.data.SoundDefinitionsProvider;
import net.neoforged.neoforge.registries.DeferredHolder;

/**
 * assets/hollowfrontier/sounds.json. Every event is a remix of vanilla sound events (type "event": no files of
 * our own yet), with its own pitch and volume, plus a subtitle. Replace an entry with real .ogg files to re-voice it.
 */
public class ModSoundDefinitionsProvider extends SoundDefinitionsProvider {
    public ModSoundDefinitionsProvider(PackOutput output) {
        super(output, HollowFrontier.MODID);
    }

    @Override
    public void registerSounds() {
        // The piano's tunes, one event each so the piano can play them in turn (tools/music/PianoRoll.java renders them).
        for (var tune : ModSounds.PIANO_REPERTOIRE) {
            String name = tune.getId().getPath().substring("block.saloon_piano.".length());
            add(tune, definition().subtitle("subtitles." + HollowFrontier.MODID + ".saloon_piano.play")
                    .with(recording("block/saloon_piano/" + name).attenuationDistance(24)));
        }
        for (var tune : ModSounds.PIANO_CAROLS) { // 35c
            String name = tune.getId().getPath().substring("block.saloon_piano.".length());
            add(tune, definition().subtitle("subtitles." + HollowFrontier.MODID + ".saloon_piano.carol")
                    .with(recording("block/saloon_piano/" + name).attenuationDistance(24)));
        }

        remix(ModSounds.RATTLESNAKE_RATTLE, "rattlesnake.rattle", vanilla("entity.silverfish.ambient", 0.8F, 1.7F), vanilla("entity.silverfish.ambient", 0.8F, 1.85F));
        remix(ModSounds.RATTLESNAKE_HURT, "rattlesnake.hurt", vanilla("entity.silverfish.hurt", 1.0F, 1.3F));
        remix(ModSounds.RATTLESNAKE_DEATH, "rattlesnake.death", vanilla("entity.silverfish.death", 1.0F, 1.3F));
        remix(ModSounds.DYNAMITE_FUSE, "dynamite.fuse", vanilla("entity.tnt.primed", 0.7F, 1.3F));
        remix(ModSounds.GHOST_CAT_FADE, "ghost_cat.fade", vanilla("particle.soul_escape", 1.0F, 1.3F), vanilla("entity.cat.purreow", 0.25F, 1.6F));

        // Steam handcar: a piston's thump with a hiss for each beat of the engine; the whistle is a goat horn, pitched up.
        remix(ModSounds.HANDCAR_CHUFF, "steam_handcar.chuff", vanilla("block.piston.extend", 0.25F, 0.6F), vanilla("block.fire.extinguish", 0.15F, 1.9F));
        remix(ModSounds.HANDCAR_WHISTLE, "steam_handcar.whistle", vanilla("item.goat_horn.sound.1", 0.7F, 1.45F));
        // Airship: a slower, deeper engine beat than the handcar's; venting steam to climb; coal into the firebox.
        remix(ModSounds.AIRSHIP_ENGINE, "airship.engine", vanilla("block.piston.extend", 0.3F, 0.45F), vanilla("block.fire.extinguish", 0.12F, 1.5F));
        remix(ModSounds.AIRSHIP_VENT, "airship.vent", vanilla("block.fire.extinguish", 0.35F, 0.7F), vanilla("block.lava.extinguish", 0.25F, 1.2F));
        remix(ModSounds.AIRSHIP_STOKE, "airship.stoke", vanilla("block.furnace.fire_crackle", 1.0F, 0.9F), vanilla("item.firecharge.use", 0.4F, 1.4F));
        // Polar Gateway: the ring rings and the well fills with light; going dark is the same, lower and duller.
        remix(ModSounds.GATEWAY_OPEN, "polar_gateway.open", vanilla("block.end_portal.spawn", 0.6F, 1.4F), vanilla("block.amethyst_block.resonate", 1.0F, 0.7F));
        remix(ModSounds.GATEWAY_CLOSE, "polar_gateway.close", vanilla("block.beacon.deactivate", 1.0F, 0.8F), vanilla("block.amethyst_block.break", 1.0F, 0.6F));
        // The crossing: each ring stone chimes as it lights (pitched up stone by stone in code); the hum while you stand in
        // the well (a loop, its pitch rising with the charge); the rush of the crossing; the soft landing inside.
        remix(ModSounds.GATEWAY_STONE, "polar_gateway.stone", vanilla("block.amethyst_block.resonate", 1.0F, 1.0F), vanilla("block.amethyst_cluster.hit", 0.8F, 1.0F));
        remix(ModSounds.GATEWAY_CHARGE, "polar_gateway.charge", vanilla("block.beacon.ambient", 1.0F, 0.8F));
        remix(ModSounds.GATEWAY_CROSS, "polar_gateway.cross", vanilla("item.trident.riptide_3", 0.8F, 0.7F), vanilla("block.end_portal.spawn", 0.3F, 1.8F));
        remix(ModSounds.GATEWAY_LAND, "polar_gateway.land", vanilla("block.powder_snow.fall", 1.0F, 0.8F), vanilla("block.amethyst_block.fall", 1.0F, 0.7F));
        // Out of the crust into the open: our own recording (rendered by tools/music/Swell.java), streamed
        add(ModSounds.HOLLOW_REVEAL, definition().subtitle("subtitles." + HollowFrontier.MODID + ".hollow_earth.reveal")
                .with(recording("ambient/hollow_earth/reveal")));
        // The Hollow Earth's biomes (9b1), heard now and then: big birds, frogs and something old in the Jungle; the crystals
        // chiming on the Steppes; cracks, pops and the lava's mutter in the Badlands; wind and a faint hum of old stone in the
        // Highlands. The vents hiss.
        remix(ModSounds.JUNGLE_AMBIENT, "primordial_jungle.additions", vanilla("entity.parrot.ambient", 0.5F, 0.55F),
                vanilla("entity.frog.ambient", 0.6F, 0.7F), vanilla("entity.sniffer.idle", 0.5F, 0.8F), vanilla("entity.ravager.ambient", 0.15F, 0.5F));
        remix(ModSounds.STEPPES_AMBIENT, "crystal_steppes.additions", vanilla("block.amethyst_block.chime", 0.6F, 0.6F),
                vanilla("block.amethyst_block.chime", 0.6F, 0.85F), vanilla("block.amethyst_block.resonate", 0.3F, 1.2F));
        remix(ModSounds.BADLANDS_AMBIENT, "ember_badlands.additions", vanilla("ambient.basalt_deltas.additions", 0.6F, 0.8F),
                vanilla("block.lava.pop", 0.5F, 0.7F), vanilla("block.lava.ambient", 0.4F, 0.6F));
        remix(ModSounds.HIGHLANDS_AMBIENT, "concavian_highlands.additions", vanilla("item.elytra.flying", 0.12F, 0.5F),
                vanilla("item.elytra.flying", 0.1F, 0.6F), vanilla("block.bell.resonate", 0.1F, 0.5F));
        // The Still Sea (43a): no surf and no wind, only the water moving against itself now and then, and something far off
        remix(ModSounds.SEA_AMBIENT, "still_sea.additions", vanilla("block.water.ambient", 0.5F, 0.6F),
                vanilla("entity.dolphin.splash", 0.25F, 0.55F), vanilla("ambient.underwater.loop.additions.rare", 0.3F, 0.7F));
        remix(ModSounds.ASH_VENT_HISS, "ash_vent.hiss", vanilla("block.fire.extinguish", 0.5F, 0.6F), vanilla("block.lava.ambient", 0.6F, 0.8F));
        // The small creatures (9b2): a chirping little dinosaur, a hare whose crystals chime, a croaking fire lizard, a raven
        remix(ModSounds.FERN_SKITTER_AMBIENT, "fern_skitter.ambient", vanilla("entity.chicken.ambient", 0.6F, 1.7F), vanilla("entity.parrot.ambient", 0.4F, 1.8F));
        remix(ModSounds.FERN_SKITTER_HURT, "fern_skitter.hurt", vanilla("entity.chicken.hurt", 0.8F, 1.6F));
        remix(ModSounds.FERN_SKITTER_DEATH, "fern_skitter.death", vanilla("entity.chicken.death", 0.8F, 1.5F));
        remix(ModSounds.GLASSBACK_HARE_AMBIENT, "glassback_hare.ambient", vanilla("block.amethyst_block.chime", 0.35F, 1.4F),
                vanilla("entity.rabbit.ambient", 0.5F, 0.9F));
        remix(ModSounds.GLASSBACK_HARE_HURT, "glassback_hare.hurt", vanilla("entity.rabbit.hurt", 0.8F, 0.9F), vanilla("block.amethyst_block.hit", 0.6F, 1.2F));
        remix(ModSounds.GLASSBACK_HARE_DEATH, "glassback_hare.death", vanilla("entity.rabbit.death", 0.8F, 0.9F), vanilla("block.amethyst_cluster.break", 0.7F, 1.0F));
        remix(ModSounds.ASH_SALAMANDER_AMBIENT, "ash_salamander.ambient", vanilla("entity.frog.ambient", 0.5F, 0.75F), vanilla("block.lava.pop", 0.3F, 1.2F));
        remix(ModSounds.ASH_SALAMANDER_HURT, "ash_salamander.hurt", vanilla("entity.frog.hurt", 0.8F, 0.8F), vanilla("block.fire.extinguish", 0.3F, 1.6F));
        remix(ModSounds.ASH_SALAMANDER_DEATH, "ash_salamander.death", vanilla("entity.frog.death", 0.8F, 0.8F));
        remix(ModSounds.PALE_RAVEN_AMBIENT, "pale_raven.ambient", vanilla("entity.parrot.ambient", 0.8F, 0.45F), vanilla("entity.parrot.ambient", 0.8F, 0.5F));
        remix(ModSounds.PALE_RAVEN_HURT, "pale_raven.hurt", vanilla("entity.parrot.hurt", 0.8F, 0.6F));
        remix(ModSounds.PALE_RAVEN_DEATH, "pale_raven.death", vanilla("entity.parrot.death", 0.8F, 0.6F));
        // The Gray probe (9b3): a thin electric hum, a rising whine as it bolts, a soft pop as it blinks out
        remix(ModSounds.GRAY_PROBE_HUM, "gray_probe.hum", vanilla("block.beacon.ambient", 0.5F, 1.9F), vanilla("block.conduit.ambient.short", 0.4F, 1.6F));
        remix(ModSounds.GRAY_PROBE_HURT, "gray_probe.hurt", vanilla("block.copper_bulb.turn_off", 0.8F, 1.6F), vanilla("entity.iron_golem.hurt", 0.4F, 2.0F));
        remix(ModSounds.GRAY_PROBE_FLEE, "gray_probe.flee", vanilla("block.beacon.activate", 1.0F, 2.0F), vanilla("entity.breeze.wind_burst", 0.6F, 1.6F));
        remix(ModSounds.GRAY_PROBE_VANISH, "gray_probe.vanish", vanilla("block.beacon.deactivate", 0.8F, 2.0F), vanilla("entity.chicken.egg", 0.6F, 0.5F));
        // The wild megafauna (10a): a deep-chested mammoth that trumpets on a goat horn's low note; a terror bird's rattling
        // squawk and its screech as it picks you out; a grunting, rumbling ground sloth that crunches leaves; the glyptodon's
        // snuffle and the clack of its shell closing. The Oliphant is a goat horn blown low and long.
        remix(ModSounds.MAMMOTH_AMBIENT, "woolly_mammoth.ambient", vanilla("entity.ravager.ambient", 0.6F, 0.6F), vanilla("entity.camel.ambient", 0.8F, 0.5F));
        remix(ModSounds.MAMMOTH_HURT, "woolly_mammoth.hurt", vanilla("entity.ravager.hurt", 0.9F, 0.7F));
        remix(ModSounds.MAMMOTH_DEATH, "woolly_mammoth.death", vanilla("entity.ravager.death", 1.0F, 0.6F));
        remix(ModSounds.MAMMOTH_TRUMPET, "woolly_mammoth.trumpet", vanilla("item.goat_horn.sound.2", 1.0F, 0.75F), vanilla("item.goat_horn.sound.6", 1.0F, 0.8F));
        remix(ModSounds.TERROR_BIRD_AMBIENT, "terror_bird.ambient", vanilla("entity.chicken.ambient", 0.8F, 0.45F), vanilla("entity.hoglin.ambient", 0.4F, 1.5F));
        remix(ModSounds.TERROR_BIRD_HURT, "terror_bird.hurt", vanilla("entity.chicken.hurt", 1.0F, 0.5F));
        remix(ModSounds.TERROR_BIRD_DEATH, "terror_bird.death", vanilla("entity.chicken.death", 1.0F, 0.45F), vanilla("entity.hoglin.death", 0.4F, 1.4F));
        remix(ModSounds.TERROR_BIRD_SCREECH, "terror_bird.screech", vanilla("entity.phantom.ambient", 1.0F, 0.7F), vanilla("entity.ravager.roar", 0.5F, 1.8F));
        remix(ModSounds.MEGATHERIUM_AMBIENT, "megatherium.ambient", vanilla("entity.polar_bear.ambient", 0.8F, 0.55F), vanilla("entity.sniffer.idle", 0.7F, 0.6F));
        remix(ModSounds.MEGATHERIUM_HURT, "megatherium.hurt", vanilla("entity.polar_bear.hurt", 0.9F, 0.6F));
        remix(ModSounds.MEGATHERIUM_DEATH, "megatherium.death", vanilla("entity.polar_bear.death", 1.0F, 0.55F));
        remix(ModSounds.MEGATHERIUM_BROWSE, "megatherium.browse", vanilla("block.azalea_leaves.break", 0.8F, 0.8F), vanilla("block.grass.break", 0.8F, 0.7F));
        remix(ModSounds.GLYPTODON_AMBIENT, "glyptodon.ambient", vanilla("entity.armadillo.ambient", 0.8F, 0.6F), vanilla("entity.sniffer.idle", 0.4F, 1.1F));
        remix(ModSounds.GLYPTODON_HURT, "glyptodon.hurt", vanilla("entity.armadillo.hurt", 0.9F, 0.65F));
        remix(ModSounds.GLYPTODON_DEATH, "glyptodon.death", vanilla("entity.armadillo.death", 1.0F, 0.6F));
        remix(ModSounds.GLYPTODON_HUNKER, "glyptodon.hunker", vanilla("entity.armadillo.roll", 1.0F, 0.6F), vanilla("entity.turtle.shamble", 0.8F, 0.6F));
        remix(ModSounds.GLYPTODON_BRUSH, "glyptodon.brush", vanilla("entity.armadillo.brush", 1.0F, 0.8F));
        // The Still Sea's creatures (43b): a sea cow's low lowing, a whale's groan pitched down into the floor, a
        // plesiosaur's honk, and a jelly that hardly makes a sound
        remix(ModSounds.SEA_COW_AMBIENT, "great_sea_cow.ambient", vanilla("entity.cow.ambient", 0.7F, 0.5F), vanilla("entity.dolphin.ambient_water", 0.4F, 0.5F));
        remix(ModSounds.SEA_COW_HURT, "great_sea_cow.hurt", vanilla("entity.cow.hurt", 0.8F, 0.5F));
        remix(ModSounds.SEA_COW_DEATH, "great_sea_cow.death", vanilla("entity.cow.death", 0.9F, 0.5F));
        remix(ModSounds.BASILOSAURUS_AMBIENT, "basilosaurus.ambient", vanilla("entity.elder_guardian.ambient", 0.6F, 0.5F), vanilla("entity.ravager.ambient", 0.4F, 0.5F));
        remix(ModSounds.BASILOSAURUS_HURT, "basilosaurus.hurt", vanilla("entity.ravager.hurt", 0.9F, 0.55F));
        remix(ModSounds.BASILOSAURUS_DEATH, "basilosaurus.death", vanilla("entity.ravager.death", 1.0F, 0.5F));
        remix(ModSounds.BASILOSAURUS_BITE, "basilosaurus.bite", vanilla("entity.ravager.attack", 1.0F, 0.6F), vanilla("entity.generic.splash", 0.8F, 0.7F));
        remix(ModSounds.PLESIOSAUR_AMBIENT, "plesiosaur.ambient", vanilla("entity.camel.ambient", 0.7F, 0.7F), vanilla("entity.dolphin.ambient", 0.4F, 0.6F));
        remix(ModSounds.PLESIOSAUR_HURT, "plesiosaur.hurt", vanilla("entity.camel.hurt", 0.8F, 0.7F));
        remix(ModSounds.PLESIOSAUR_DEATH, "plesiosaur.death", vanilla("entity.camel.death", 0.9F, 0.7F));
        remix(ModSounds.LAMPJELLY_HURT, "lampjelly.hurt", vanilla("entity.slime.hurt_small", 0.5F, 1.4F));
        remix(ModSounds.LAMPJELLY_DEATH, "lampjelly.death", vanilla("entity.slime.death_small", 0.6F, 1.3F));
        remix(ModSounds.LAMPJELLY_STING, "lampjelly.sting", vanilla("entity.bee.sting", 0.6F, 1.5F));
        remix(ModSounds.OLIPHANT_BLOW, "oliphant.blow", vanilla("item.goat_horn.sound.0", 1.0F, 0.6F), vanilla("item.goat_horn.sound.1", 1.0F, 0.6F));
        // The Gray station (10b): the sentinel's whine as it locks on, its crack of a shot, the station's alarm; the locator's ping
        remix(ModSounds.SENTINEL_CHARGE, "gray_sentinel.charge", vanilla("block.beacon.activate", 0.8F, 1.8F), vanilla("entity.guardian.attack", 0.5F, 1.6F));
        remix(ModSounds.SENTINEL_ZAP, "gray_sentinel.zap", vanilla("entity.breeze.shoot", 1.0F, 1.6F), vanilla("block.respawn_anchor.deplete", 0.6F, 2.0F));
        // The phlogiston guns (13b): synthesized ray-gun shots of ours (tools/music/Laser.java), three takes each
        add(ModSounds.PHLOGISTON_GUN_FIRE, definition().subtitle("subtitles." + HollowFrontier.MODID + ".phlogiston_gun.fire")
                .with(shot("item/phlogiston_gun/pistol_1"), shot("item/phlogiston_gun/pistol_2"), shot("item/phlogiston_gun/pistol_3")));
        add(ModSounds.PHLOGISTON_RIFLE_FIRE, definition().subtitle("subtitles." + HollowFrontier.MODID + ".phlogiston_gun.fire")
                .with(shot("item/phlogiston_gun/rifle_1"), shot("item/phlogiston_gun/rifle_2"), shot("item/phlogiston_gun/rifle_3")));
        add(ModSounds.SAUCER_HUM, definition().subtitle("subtitles." + HollowFrontier.MODID + ".prototype_saucer.hum").with(shot("entity/prototype_saucer/hum")));
        // 13d: the Mothership (synthesized: tools/music/Laser.java)
        add(ModSounds.MOTHERSHIP_HUM, definition().subtitle("subtitles." + HollowFrontier.MODID + ".mothership.hum").with(shot("entity/mothership/hum")));
        add(ModSounds.DEATH_RAY_CHARGE, definition().subtitle("subtitles." + HollowFrontier.MODID + ".mothership.death_ray_charge").with(shot("entity/mothership/death_ray_charge")));
        add(ModSounds.DEATH_RAY_FIRE, definition().subtitle("subtitles." + HollowFrontier.MODID + ".mothership.death_ray_fire").with(shot("entity/mothership/death_ray_fire")));
        remix(ModSounds.SENTINEL_ALARM, "gray_sentinel.alarm", vanilla("block.note_block.bit", 1.0F, 1.5F), vanilla("block.note_block.bit", 1.0F, 1.0F));
        remix(ModSounds.SIGNAL_LOCATOR_PING, "signal_locator.ping", vanilla("block.note_block.chime", 0.8F, 2.0F), vanilla("block.beacon.power_select", 0.4F, 2.0F));
        // The frontier's mysteries (11a): a sighting's low hum, carried a long way
        remix(ModSounds.SIGHTING_HUM, "airship_sighting.hum", vanilla("block.beacon.ambient", 1.0F, 0.5F), vanilla("block.conduit.ambient", 0.6F, 0.5F));
        // 29: a dust devil's wind, a breeze's whirl dragged low with an elytra's rush under it
        remix(ModSounds.DUST_DEVIL_WIND, "dust_devil.wind", vanilla("entity.breeze.whirl", 1.0F, 0.6F), vanilla("item.elytra.flying", 0.35F, 0.5F),
                vanilla("entity.breeze.idle_ground", 0.8F, 0.55F));

        remix(ModSounds.COIN_CLINK, "coin.clink", vanilla("block.chain.hit", 0.8F, 1.5F), vanilla("block.chain.hit", 0.8F, 1.7F));
        remix(ModSounds.DRINK_POUR, "drink.pour", vanilla("item.bottle.fill", 1.0F, 0.9F), vanilla("item.bottle.fill", 1.0F, 1.1F));
        remix(ModSounds.WANTED_RAISED, "wanted.raised", vanilla("block.bell.use", 0.8F, 0.8F));

        // Raids: an iron triangle beaten in the street, hooves and shots from out of town, the safecracker's tools, the vault going
        remix(ModSounds.RAID_ALARM, "raid.alarm", vanilla("block.bell.use", 1.0F, 1.5F), vanilla("block.anvil.land", 0.3F, 2.0F));
        remix(ModSounds.RAID_HOOVES, "raid.hooves", vanilla("entity.horse.gallop", 1.0F, 0.8F), vanilla("entity.horse.gallop", 1.0F, 0.9F),
                vanilla("entity.horse.gallop", 1.0F, 0.7F));
        remix(ModSounds.RAID_GUNFIRE, "raid.gunfire", vanilla("entity.firework_rocket.blast_far", 1.0F, 0.6F),
                vanilla("entity.firework_rocket.large_blast_far", 1.0F, 0.7F));
        remix(ModSounds.VAULT_WORK, "raid.vault_work", vanilla("block.chain.hit", 0.8F, 0.6F), vanilla("block.anvil.use", 0.25F, 1.8F),
                vanilla("block.iron_door.close", 0.4F, 1.6F));
        remix(ModSounds.VAULT_BLOWN, "raid.vault_blown", vanilla("entity.generic.explode", 1.0F, 1.2F), vanilla("block.iron_door.open", 1.0F, 0.6F));
        remix(ModSounds.RAID_WON, "raid.won", vanilla("entity.player.levelup", 0.8F, 0.8F));
        remix(ModSounds.RAID_LOST, "raid.lost", vanilla("entity.horse.angry", 0.8F, 0.8F));

        remix(ModSounds.TOWN_BLACKSMITH, "town.blacksmith", vanilla("block.anvil.use", 0.35F, 0.8F), vanilla("block.anvil.use", 0.35F, 0.95F));
        remix(ModSounds.TOWN_HORSE, "town.horse", vanilla("entity.horse.ambient", 0.6F, 1.0F), vanilla("entity.horse.breathe", 0.6F, 0.9F),
                vanilla("entity.donkey.ambient", 0.4F, 1.0F));
        remix(ModSounds.TOWN_BELL, "town.bell", vanilla("block.bell.use", 0.7F, 0.55F));
        remix(ModSounds.TOWN_DOOR, "town.door", vanilla("block.wooden_door.open", 0.5F, 0.75F));
        remix(ModSounds.TOWN_SPURS, "town.spurs", vanilla("block.chain.step", 0.35F, 1.6F), vanilla("block.chain.step", 0.35F, 1.8F));
        remix(ModSounds.TOWN_BOARDWALK, "town.boardwalk", vanilla("block.wood.step", 0.5F, 0.8F), vanilla("block.wood.step", 0.5F, 0.9F));
        remix(ModSounds.SALOON_GLASSES, "saloon.glasses", vanilla("block.amethyst_block.chime", 0.3F, 1.8F), vanilla("item.bottle.fill", 0.3F, 1.4F));
        remix(ModSounds.TOWN_CAT, "town.cat", vanilla("entity.cat.stray_ambient", 0.35F, 1.0F), vanilla("entity.cat.ambient", 0.35F, 1.1F),
                vanilla("entity.cat.purr", 0.6F, 1.0F));
    }

    private void remix(DeferredHolder<SoundEvent, SoundEvent> event, String subtitle, SoundDefinition.Sound... sounds) {
        add(event, definition().subtitle("subtitles." + HollowFrontier.MODID + "." + subtitle).with(sounds));
    }

    // A recording of ours: assets/hollowfrontier/sounds/<path>.ogg, streamed rather than loaded whole.
    private static SoundDefinition.Sound recording(String path) {
        return sound(HollowFrontier.id(path)).stream();
    }

    // A short sound of ours: assets/hollowfrontier/sounds/<path>.ogg, loaded whole (not streamed: it plays often and at once).
    private static SoundDefinition.Sound shot(String path) {
        return sound(HollowFrontier.id(path));
    }

    private static SoundDefinition.Sound vanilla(String event, float volume, float pitch) {
        return sound(Identifier.withDefaultNamespace(event), SoundDefinition.SoundType.EVENT).volume(volume).pitch(pitch);
    }
}
