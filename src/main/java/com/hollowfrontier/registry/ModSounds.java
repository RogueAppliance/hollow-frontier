package com.hollowfrontier.registry;

import java.util.List;

import com.hollowfrontier.HollowFrontier;

import net.minecraft.core.registries.Registries;
import net.minecraft.sounds.SoundEvent;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

/**
 * The mod's sound events, defined in sounds.json by ModSoundDefinitionsProvider. Music (discs, the piano) is real recordings (public
 * domain and CC0, see ASSETS.md); the effects are remixes of vanilla sound events until recordings exist. Swapping in real .ogg
 * files only touches the provider; the code keeps playing these events.
 */
public final class ModSounds {
    private ModSounds() {}

    public static final DeferredRegister<SoundEvent> SOUNDS =
            DeferredRegister.create(Registries.SOUND_EVENT, HollowFrontier.MODID);

    // Creatures and gear
    public static final DeferredHolder<SoundEvent, SoundEvent> RATTLESNAKE_RATTLE = register("entity.rattlesnake.rattle");
    public static final DeferredHolder<SoundEvent, SoundEvent> RATTLESNAKE_HURT = register("entity.rattlesnake.hurt");
    public static final DeferredHolder<SoundEvent, SoundEvent> RATTLESNAKE_DEATH = register("entity.rattlesnake.death");
    public static final DeferredHolder<SoundEvent, SoundEvent> DYNAMITE_FUSE = register("item.dynamite.fuse");
    public static final DeferredHolder<SoundEvent, SoundEvent> GHOST_CAT_FADE = register("entity.ghost_cat.fade");

    // Town life
    public static final DeferredHolder<SoundEvent, SoundEvent> COIN_CLINK = register("item.coin.clink");
    public static final DeferredHolder<SoundEvent, SoundEvent> DRINK_POUR = register("entity.barmaid.pour");
    public static final DeferredHolder<SoundEvent, SoundEvent> WANTED_RAISED = register("event.wanted.raised");

    // Town raids (TownRaid)
    public static final DeferredHolder<SoundEvent, SoundEvent> RAID_ALARM = register("event.raid.alarm");
    public static final DeferredHolder<SoundEvent, SoundEvent> RAID_HOOVES = register("event.raid.hooves");
    public static final DeferredHolder<SoundEvent, SoundEvent> RAID_GUNFIRE = register("event.raid.gunfire");
    public static final DeferredHolder<SoundEvent, SoundEvent> VAULT_WORK = register("event.raid.vault_work");
    public static final DeferredHolder<SoundEvent, SoundEvent> VAULT_BLOWN = register("event.raid.vault_blown");
    public static final DeferredHolder<SoundEvent, SoundEvent> RAID_WON = register("event.raid.won");
    public static final DeferredHolder<SoundEvent, SoundEvent> RAID_LOST = register("event.raid.lost");

    // Music: the saloon piano's tunes (recordings).
    // The saloon piano's repertoire, played in turn (SaloonPianoBlockEntity). Rendered in-house from public-domain scores.
    public static final DeferredHolder<SoundEvent, SoundEvent> PIANO_THE_ENTERTAINER = register("block.saloon_piano.the_entertainer");
    public static final DeferredHolder<SoundEvent, SoundEvent> PIANO_ELITE_SYNCOPATIONS = register("block.saloon_piano.elite_syncopations");
    public static final DeferredHolder<SoundEvent, SoundEvent> PIANO_PINEAPPLE_RAG = register("block.saloon_piano.pineapple_rag");
    public static final DeferredHolder<SoundEvent, SoundEvent> PIANO_THE_STRENUOUS_LIFE = register("block.saloon_piano.the_strenuous_life");
    public static final List<DeferredHolder<SoundEvent, SoundEvent>> PIANO_REPERTOIRE =
            List.of(PIANO_THE_ENTERTAINER, PIANO_ELITE_SYNCOPATIONS, PIANO_PINEAPPLE_RAG, PIANO_THE_STRENUOUS_LIFE);
    // 35c: December's carols, worked in every other tune. Our own arrangements of public-domain tunes, rendered the same way.
    public static final DeferredHolder<SoundEvent, SoundEvent> PIANO_JINGLE_BELLS = register("block.saloon_piano.jingle_bells");
    public static final DeferredHolder<SoundEvent, SoundEvent> PIANO_GOD_REST_YE = register("block.saloon_piano.god_rest_ye_merry_gentlemen");
    public static final DeferredHolder<SoundEvent, SoundEvent> PIANO_O_COME = register("block.saloon_piano.o_come_all_ye_faithful");
    public static final List<DeferredHolder<SoundEvent, SoundEvent>> PIANO_CAROLS = List.of(PIANO_JINGLE_BELLS, PIANO_GOD_REST_YE, PIANO_O_COME);

    // Steam handcar: the engine's beat and its whistle
    public static final DeferredHolder<SoundEvent, SoundEvent> HANDCAR_CHUFF = register("entity.steam_handcar.chuff");
    public static final DeferredHolder<SoundEvent, SoundEvent> HANDCAR_WHISTLE = register("entity.steam_handcar.whistle");
    public static final DeferredHolder<SoundEvent, SoundEvent> AIRSHIP_ENGINE = register("entity.airship.engine");
    public static final DeferredHolder<SoundEvent, SoundEvent> AIRSHIP_VENT = register("entity.airship.vent");
    public static final DeferredHolder<SoundEvent, SoundEvent> AIRSHIP_STOKE = register("entity.airship.stoke");
    public static final DeferredHolder<SoundEvent, SoundEvent> GATEWAY_OPEN = register("block.polar_gateway.open");
    public static final DeferredHolder<SoundEvent, SoundEvent> GATEWAY_CLOSE = register("block.polar_gateway.close");
    public static final DeferredHolder<SoundEvent, SoundEvent> GATEWAY_STONE = register("block.polar_gateway.stone");
    public static final DeferredHolder<SoundEvent, SoundEvent> GATEWAY_CHARGE = register("block.polar_gateway.charge");
    public static final DeferredHolder<SoundEvent, SoundEvent> GATEWAY_CROSS = register("block.polar_gateway.cross");
    public static final DeferredHolder<SoundEvent, SoundEvent> GATEWAY_LAND = register("block.polar_gateway.land");
    // Falling out of the crust into the Hollow Earth: a slow swell of strings and voices (tools/music/Swell.java)
    public static final DeferredHolder<SoundEvent, SoundEvent> HOLLOW_REVEAL = register("ambient.hollow_earth.reveal");
    // Each Hollow Earth biome's ambient additions (HollowBiomes), and the ash vents' hiss
    public static final DeferredHolder<SoundEvent, SoundEvent> JUNGLE_AMBIENT = register("ambient.primordial_jungle.additions");
    public static final DeferredHolder<SoundEvent, SoundEvent> STEPPES_AMBIENT = register("ambient.crystal_steppes.additions");
    public static final DeferredHolder<SoundEvent, SoundEvent> BADLANDS_AMBIENT = register("ambient.ember_badlands.additions");
    public static final DeferredHolder<SoundEvent, SoundEvent> HIGHLANDS_AMBIENT = register("ambient.concavian_highlands.additions");
    public static final DeferredHolder<SoundEvent, SoundEvent> SEA_AMBIENT = register("ambient.still_sea.additions");
    public static final DeferredHolder<SoundEvent, SoundEvent> ASH_VENT_HISS = register("block.ash_vent.hiss");
    // The Hollow Earth's small creatures (9b2)
    public static final DeferredHolder<SoundEvent, SoundEvent> FERN_SKITTER_AMBIENT = register("entity.fern_skitter.ambient");
    public static final DeferredHolder<SoundEvent, SoundEvent> FERN_SKITTER_HURT = register("entity.fern_skitter.hurt");
    public static final DeferredHolder<SoundEvent, SoundEvent> FERN_SKITTER_DEATH = register("entity.fern_skitter.death");
    public static final DeferredHolder<SoundEvent, SoundEvent> GLASSBACK_HARE_AMBIENT = register("entity.glassback_hare.ambient");
    public static final DeferredHolder<SoundEvent, SoundEvent> GLASSBACK_HARE_HURT = register("entity.glassback_hare.hurt");
    public static final DeferredHolder<SoundEvent, SoundEvent> GLASSBACK_HARE_DEATH = register("entity.glassback_hare.death");
    public static final DeferredHolder<SoundEvent, SoundEvent> ASH_SALAMANDER_AMBIENT = register("entity.ash_salamander.ambient");
    public static final DeferredHolder<SoundEvent, SoundEvent> ASH_SALAMANDER_HURT = register("entity.ash_salamander.hurt");
    public static final DeferredHolder<SoundEvent, SoundEvent> ASH_SALAMANDER_DEATH = register("entity.ash_salamander.death");
    public static final DeferredHolder<SoundEvent, SoundEvent> PALE_RAVEN_AMBIENT = register("entity.pale_raven.ambient");
    public static final DeferredHolder<SoundEvent, SoundEvent> PALE_RAVEN_HURT = register("entity.pale_raven.hurt");
    public static final DeferredHolder<SoundEvent, SoundEvent> PALE_RAVEN_DEATH = register("entity.pale_raven.death");
    // The Gray probe and the lab (9b3)
    public static final DeferredHolder<SoundEvent, SoundEvent> GRAY_PROBE_HUM = register("entity.gray_probe.hum");
    public static final DeferredHolder<SoundEvent, SoundEvent> GRAY_PROBE_HURT = register("entity.gray_probe.hurt");
    public static final DeferredHolder<SoundEvent, SoundEvent> GRAY_PROBE_FLEE = register("entity.gray_probe.flee");
    public static final DeferredHolder<SoundEvent, SoundEvent> GRAY_PROBE_VANISH = register("entity.gray_probe.vanish");
    // The wild megafauna (10a)
    public static final DeferredHolder<SoundEvent, SoundEvent> MAMMOTH_AMBIENT = register("entity.woolly_mammoth.ambient");
    public static final DeferredHolder<SoundEvent, SoundEvent> MAMMOTH_HURT = register("entity.woolly_mammoth.hurt");
    public static final DeferredHolder<SoundEvent, SoundEvent> MAMMOTH_DEATH = register("entity.woolly_mammoth.death");
    public static final DeferredHolder<SoundEvent, SoundEvent> MAMMOTH_TRUMPET = register("entity.woolly_mammoth.trumpet");
    public static final DeferredHolder<SoundEvent, SoundEvent> TERROR_BIRD_AMBIENT = register("entity.terror_bird.ambient");
    public static final DeferredHolder<SoundEvent, SoundEvent> TERROR_BIRD_HURT = register("entity.terror_bird.hurt");
    public static final DeferredHolder<SoundEvent, SoundEvent> TERROR_BIRD_DEATH = register("entity.terror_bird.death");
    public static final DeferredHolder<SoundEvent, SoundEvent> TERROR_BIRD_SCREECH = register("entity.terror_bird.screech");
    public static final DeferredHolder<SoundEvent, SoundEvent> MEGATHERIUM_AMBIENT = register("entity.megatherium.ambient");
    public static final DeferredHolder<SoundEvent, SoundEvent> MEGATHERIUM_HURT = register("entity.megatherium.hurt");
    public static final DeferredHolder<SoundEvent, SoundEvent> MEGATHERIUM_DEATH = register("entity.megatherium.death");
    public static final DeferredHolder<SoundEvent, SoundEvent> MEGATHERIUM_BROWSE = register("entity.megatherium.browse");
    public static final DeferredHolder<SoundEvent, SoundEvent> GLYPTODON_AMBIENT = register("entity.glyptodon.ambient");
    public static final DeferredHolder<SoundEvent, SoundEvent> GLYPTODON_HURT = register("entity.glyptodon.hurt");
    public static final DeferredHolder<SoundEvent, SoundEvent> GLYPTODON_DEATH = register("entity.glyptodon.death");
    public static final DeferredHolder<SoundEvent, SoundEvent> GLYPTODON_HUNKER = register("entity.glyptodon.hunker");
    public static final DeferredHolder<SoundEvent, SoundEvent> GLYPTODON_BRUSH = register("entity.glyptodon.brush");
    // The Still Sea's creatures (43b)
    public static final DeferredHolder<SoundEvent, SoundEvent> SEA_COW_AMBIENT = register("entity.great_sea_cow.ambient");
    public static final DeferredHolder<SoundEvent, SoundEvent> SEA_COW_HURT = register("entity.great_sea_cow.hurt");
    public static final DeferredHolder<SoundEvent, SoundEvent> SEA_COW_DEATH = register("entity.great_sea_cow.death");
    public static final DeferredHolder<SoundEvent, SoundEvent> BASILOSAURUS_AMBIENT = register("entity.basilosaurus.ambient");
    public static final DeferredHolder<SoundEvent, SoundEvent> BASILOSAURUS_HURT = register("entity.basilosaurus.hurt");
    public static final DeferredHolder<SoundEvent, SoundEvent> BASILOSAURUS_DEATH = register("entity.basilosaurus.death");
    public static final DeferredHolder<SoundEvent, SoundEvent> BASILOSAURUS_BITE = register("entity.basilosaurus.bite");
    public static final DeferredHolder<SoundEvent, SoundEvent> PLESIOSAUR_AMBIENT = register("entity.plesiosaur.ambient");
    public static final DeferredHolder<SoundEvent, SoundEvent> PLESIOSAUR_HURT = register("entity.plesiosaur.hurt");
    public static final DeferredHolder<SoundEvent, SoundEvent> PLESIOSAUR_DEATH = register("entity.plesiosaur.death");
    public static final DeferredHolder<SoundEvent, SoundEvent> LAMPJELLY_HURT = register("entity.lampjelly.hurt");
    public static final DeferredHolder<SoundEvent, SoundEvent> LAMPJELLY_DEATH = register("entity.lampjelly.death");
    public static final DeferredHolder<SoundEvent, SoundEvent> LAMPJELLY_STING = register("entity.lampjelly.sting");
    public static final DeferredHolder<SoundEvent, SoundEvent> OLIPHANT_BLOW = register("item.oliphant.blow");
    // The Gray experiments (10b)
    public static final DeferredHolder<SoundEvent, SoundEvent> SENTINEL_CHARGE = register("entity.gray_sentinel.charge");
    public static final DeferredHolder<SoundEvent, SoundEvent> SENTINEL_ZAP = register("entity.gray_sentinel.zap");
    public static final DeferredHolder<SoundEvent, SoundEvent> SENTINEL_ALARM = register("entity.gray_sentinel.alarm");
    public static final DeferredHolder<SoundEvent, SoundEvent> SIGNAL_LOCATOR_PING = register("item.signal_locator.ping");
    // Phlogiston tech (13b): the phlogiston guns' shots (recordings of ours: tools/music/Laser.java)
    public static final DeferredHolder<SoundEvent, SoundEvent> PHLOGISTON_GUN_FIRE = register("item.phlogiston_gun.fire");
    public static final DeferredHolder<SoundEvent, SoundEvent> PHLOGISTON_RIFLE_FIRE = register("item.phlogiston_rifle.fire");
    // 13c: the Prototype Saucer's hum (ours: tools/music/Laser.java), two seconds, played end to end while it flies
    public static final DeferredHolder<SoundEvent, SoundEvent> SAUCER_HUM = register("entity.prototype_saucer.hum");
    // 13d: the Mothership's hum, and its death ray charging and falling
    public static final DeferredHolder<SoundEvent, SoundEvent> MOTHERSHIP_HUM = register("entity.mothership.hum");
    public static final DeferredHolder<SoundEvent, SoundEvent> DEATH_RAY_CHARGE = register("entity.mothership.death_ray_charge");
    public static final DeferredHolder<SoundEvent, SoundEvent> DEATH_RAY_FIRE = register("entity.mothership.death_ray_fire");
    // The frontier's mysteries (11a)
    public static final DeferredHolder<SoundEvent, SoundEvent> SIGHTING_HUM = register("entity.airship_sighting.hum");
    // 29: a dust devil's wind
    public static final DeferredHolder<SoundEvent, SoundEvent> DUST_DEVIL_WIND = register("entity.dust_devil.wind");

    // Town ambience: one-shots played around a player who's in town (TownSoundscape)
    public static final DeferredHolder<SoundEvent, SoundEvent> TOWN_BLACKSMITH = register("ambient.town.blacksmith");
    public static final DeferredHolder<SoundEvent, SoundEvent> TOWN_HORSE = register("ambient.town.horse");
    public static final DeferredHolder<SoundEvent, SoundEvent> TOWN_BELL = register("ambient.town.bell");
    public static final DeferredHolder<SoundEvent, SoundEvent> TOWN_DOOR = register("ambient.town.door");
    public static final DeferredHolder<SoundEvent, SoundEvent> TOWN_SPURS = register("ambient.town.spurs");
    public static final DeferredHolder<SoundEvent, SoundEvent> TOWN_BOARDWALK = register("ambient.town.boardwalk");
    public static final DeferredHolder<SoundEvent, SoundEvent> SALOON_GLASSES = register("ambient.saloon.glasses");
    public static final DeferredHolder<SoundEvent, SoundEvent> TOWN_CAT = register("ambient.town.cat");

    private static DeferredHolder<SoundEvent, SoundEvent> register(String name) {
        return SOUNDS.register(name, () -> SoundEvent.createVariableRangeEvent(HollowFrontier.id(name)));
    }
}
