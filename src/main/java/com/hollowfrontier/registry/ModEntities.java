package com.hollowfrontier.registry;

import com.hollowfrontier.HollowFrontier;
import com.hollowfrontier.content.frontier.bounty.BountyBoardEntity;
import com.hollowfrontier.content.steam.SteamHandcar;
import com.hollowfrontier.content.steam.airship.Airship;
import com.hollowfrontier.content.steam.submersible.SteamSubmersible;
import com.hollowfrontier.content.frontier.furniture.SeatEntity;
import com.hollowfrontier.content.frontier.gear.DynamiteEntity;
import com.hollowfrontier.content.frontier.gear.LassoEntity;
import com.hollowfrontier.content.frontier.mystery.AirshipSighting;
import com.hollowfrontier.content.frontier.mystery.CattleCarcass;
import com.hollowfrontier.content.frontier.mystery.ManInBlack;
import com.hollowfrontier.content.frontier.weather.DustDevil;
import com.hollowfrontier.content.frontier.outlaw.AbstractOutlaw;
import com.hollowfrontier.content.frontier.outlaw.Outlaw;
import com.hollowfrontier.content.frontier.outlaw.OutlawBoss;
import com.hollowfrontier.content.frontier.outlaw.OutlawGunslinger;
import com.hollowfrontier.content.frontier.saloon.Barmaid;
import com.hollowfrontier.content.frontier.townsfolk.Lawman;
import com.hollowfrontier.content.frontier.townsfolk.Townsfolk;
import com.hollowfrontier.content.frontier.wildlife.Rattlesnake;
import com.hollowfrontier.content.hollow.creature.AshSalamander;
import com.hollowfrontier.content.hollow.creature.FernSkitter;
import com.hollowfrontier.content.hollow.creature.GrayProbe;
import com.hollowfrontier.content.hollow.creature.GlassbackHare;
import com.hollowfrontier.content.hollow.creature.PaleRaven;
import com.hollowfrontier.content.hollow.gray.GraySentinel;
import com.hollowfrontier.content.hollow.megafauna.Glyptodon;
import com.hollowfrontier.content.hollow.sea.Basilosaurus;
import com.hollowfrontier.content.hollow.sea.GreatSeaCow;
import com.hollowfrontier.content.hollow.sea.HarpoonEntity;
import com.hollowfrontier.content.hollow.sea.Lampjelly;
import com.hollowfrontier.content.hollow.sea.Plesiosaur;
import com.hollowfrontier.content.hollow.sea.SeaLife;
import com.hollowfrontier.content.hollow.megafauna.Megatherium;
import com.hollowfrontier.content.hollow.megafauna.TerrorBird;
import com.hollowfrontier.content.hollow.megafauna.WoollyMammoth;
import com.hollowfrontier.content.concavia.CrystalGuardian;
import com.hollowfrontier.content.concavia.Kindled;
import com.hollowfrontier.content.concavia.SunArchon;
import com.hollowfrontier.content.ufo.GraySaucer;
import com.hollowfrontier.content.ufo.GrayWarden;
import com.hollowfrontier.content.ufo.GrayDrone;
import com.hollowfrontier.content.ufo.Mothership;
import com.hollowfrontier.content.ufo.PrototypeSaucer;
import com.hollowfrontier.content.ufo.PhlogistonBeam;

import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.entity.SpawnPlacementTypes;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.level.levelgen.Heightmap;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.EntityAttributeCreationEvent;
import net.neoforged.neoforge.event.entity.RegisterSpawnPlacementsEvent;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

@EventBusSubscriber(modid = HollowFrontier.MODID)
public final class ModEntities {
    private ModEntities() {}

    public static final DeferredRegister<EntityType<?>> ENTITIES =
            DeferredRegister.create(Registries.ENTITY_TYPE, HollowFrontier.MODID);

    public static final DeferredHolder<EntityType<?>, EntityType<LassoEntity>> LASSO = ENTITIES.register("lasso",
            id -> EntityType.Builder.<LassoEntity>of(LassoEntity::new, MobCategory.MISC)
                    .noLootTable()
                    .sized(0.25F, 0.25F)
                    .clientTrackingRange(4)
                    .updateInterval(10)
                    .build(key(id)));

    public static final DeferredHolder<EntityType<?>, EntityType<DynamiteEntity>> DYNAMITE = ENTITIES.register("dynamite",
            id -> EntityType.Builder.<DynamiteEntity>of(DynamiteEntity::new, MobCategory.MISC)
                    .noLootTable()
                    .sized(0.25F, 0.25F)
                    .clientTrackingRange(4)
                    .updateInterval(10)
                    .build(key(id)));
    public static final DeferredHolder<EntityType<?>, EntityType<Rattlesnake>> RATTLESNAKE = ENTITIES.register("rattlesnake",
            id -> EntityType.Builder.<Rattlesnake>of(Rattlesnake::new, MobCategory.CREATURE)
                    .sized(0.7F, 0.35F)
                    .clientTrackingRange(8)
                    .build(key(id)));
    // The Hollow Earth's small creatures (9b2)
    public static final DeferredHolder<EntityType<?>, EntityType<FernSkitter>> FERN_SKITTER = ENTITIES.register("fern_skitter",
            id -> EntityType.Builder.<FernSkitter>of(FernSkitter::new, MobCategory.CREATURE)
                    .sized(0.4F, 0.7F)
                    .clientTrackingRange(8)
                    .build(key(id)));
    public static final DeferredHolder<EntityType<?>, EntityType<GlassbackHare>> GLASSBACK_HARE = ENTITIES.register("glassback_hare",
            id -> EntityType.Builder.<GlassbackHare>of(GlassbackHare::new, MobCategory.CREATURE)
                    .sized(0.45F, 0.55F)
                    .clientTrackingRange(8)
                    .build(key(id)));
    public static final DeferredHolder<EntityType<?>, EntityType<AshSalamander>> ASH_SALAMANDER = ENTITIES.register("ash_salamander",
            id -> EntityType.Builder.<AshSalamander>of(AshSalamander::new, MobCategory.CREATURE)
                    .sized(0.6F, 0.3F)
                    .fireImmune()
                    .clientTrackingRange(8)
                    .build(key(id)));
    public static final DeferredHolder<EntityType<?>, EntityType<PaleRaven>> PALE_RAVEN = ENTITIES.register("pale_raven",
            id -> EntityType.Builder.<PaleRaven>of(PaleRaven::new, MobCategory.CREATURE)
                    .sized(0.5F, 0.6F)
                    .clientTrackingRange(8)
                    .build(key(id)));
    // The wild megafauna (10a). The terror bird is a monster (it comes and goes); the rest are creatures (placed as chunks generate).
    public static final DeferredHolder<EntityType<?>, EntityType<WoollyMammoth>> WOOLLY_MAMMOTH = ENTITIES.register("woolly_mammoth",
            id -> EntityType.Builder.<WoollyMammoth>of(WoollyMammoth::new, MobCategory.CREATURE)
                    .sized(2.4F, 3.1F)
                    .eyeHeight(2.6F)
                    .clientTrackingRange(10)
                    .build(key(id)));
    public static final DeferredHolder<EntityType<?>, EntityType<TerrorBird>> TERROR_BIRD = ENTITIES.register("terror_bird",
            id -> EntityType.Builder.<TerrorBird>of(TerrorBird::new, MobCategory.MONSTER)
                    .sized(1.0F, 2.4F)
                    .eyeHeight(2.2F)
                    .clientTrackingRange(10)
                    .build(key(id)));
    public static final DeferredHolder<EntityType<?>, EntityType<Megatherium>> MEGATHERIUM = ENTITIES.register("megatherium",
            id -> EntityType.Builder.<Megatherium>of(Megatherium::new, MobCategory.CREATURE)
                    .sized(1.9F, 2.4F)
                    .eyeHeight(2.0F)
                    .clientTrackingRange(10)
                    .build(key(id)));
    public static final DeferredHolder<EntityType<?>, EntityType<Glyptodon>> GLYPTODON = ENTITIES.register("glyptodon",
            id -> EntityType.Builder.<Glyptodon>of(Glyptodon::new, MobCategory.CREATURE)
                    .sized(1.8F, 1.4F)
                    .eyeHeight(0.8F)
                    .clientTrackingRange(10)
                    .build(key(id)));
    // 43b: the Still Sea's creatures (placed by SeaLife's own spawner, by depth), and the thrown Harpoon
    public static final DeferredHolder<EntityType<?>, EntityType<GreatSeaCow>> GREAT_SEA_COW = ENTITIES.register("great_sea_cow",
            id -> EntityType.Builder.<GreatSeaCow>of(GreatSeaCow::new, MobCategory.CREATURE)
                    .sized(2.2F, 1.5F)
                    .eyeHeight(0.9F)
                    .clientTrackingRange(10)
                    .build(key(id)));
    public static final DeferredHolder<EntityType<?>, EntityType<Basilosaurus>> BASILOSAURUS = ENTITIES.register("basilosaurus",
            id -> EntityType.Builder.<Basilosaurus>of(Basilosaurus::new, MobCategory.WATER_CREATURE)
                    .sized(2.6F, 1.8F)
                    .eyeHeight(1.0F)
                    .clientTrackingRange(12)
                    .build(key(id)));
    public static final DeferredHolder<EntityType<?>, EntityType<Plesiosaur>> PLESIOSAUR = ENTITIES.register("plesiosaur",
            id -> EntityType.Builder.<Plesiosaur>of(Plesiosaur::new, MobCategory.WATER_CREATURE)
                    .sized(2.0F, 1.3F)
                    .eyeHeight(1.1F)
                    .clientTrackingRange(10)
                    .build(key(id)));
    public static final DeferredHolder<EntityType<?>, EntityType<Lampjelly>> LAMPJELLY = ENTITIES.register("lampjelly",
            id -> EntityType.Builder.<Lampjelly>of(Lampjelly::new, MobCategory.WATER_AMBIENT)
                    .sized(0.6F, 0.7F)
                    .clientTrackingRange(8)
                    .build(key(id)));
    public static final DeferredHolder<EntityType<?>, EntityType<HarpoonEntity>> HARPOON = ENTITIES.register("harpoon",
            id -> EntityType.Builder.<HarpoonEntity>of(HarpoonEntity::new, MobCategory.MISC)
                    .noLootTable()
                    .sized(0.5F, 0.5F)
                    .clientTrackingRange(4)
                    .updateInterval(2)
                    .build(key(id)));
    // 9b3: the watcher over the abandoned Gray field lab (placed by the structure only)
    public static final DeferredHolder<EntityType<?>, EntityType<GrayProbe>> GRAY_PROBE = ENTITIES.register("gray_probe",
            id -> EntityType.Builder.<GrayProbe>of(GrayProbe::new, MobCategory.MISC)
                    .sized(0.6F, 0.4F)
                    .clientTrackingRange(10)
                    .build(key(id)));

    // 11a: the frontier's mysteries: a sighting (cosmetic, never saved) and what a mutilation leaves
    public static final DeferredHolder<EntityType<?>, EntityType<AirshipSighting>> AIRSHIP_SIGHTING = ENTITIES.register("airship_sighting",
            id -> EntityType.Builder.<AirshipSighting>of(AirshipSighting::new, MobCategory.MISC)
                    .sized(12.0F, 3.0F) // as big as the model, so it isn't culled while any of it is on screen
                    .noSave()
                    .clientTrackingRange(16)
                    .updateInterval(2)
                    .build(key(id)));
    // 18: the hanging Bounty Board (like a painting: no loot table, its item dropped by hand, no position updates)
    public static final DeferredHolder<EntityType<?>, EntityType<BountyBoardEntity>> HANGING_BOUNTY_BOARD = ENTITIES.register("hanging_bounty_board",
            id -> EntityType.Builder.<BountyBoardEntity>of(BountyBoardEntity::new, MobCategory.MISC)
                    .noLootTable()
                    .sized(0.5F, 0.5F)
                    .clientTrackingRange(10)
                    .noUpdateInterval()
                    .build(key(id)));
    public static final DeferredHolder<EntityType<?>, EntityType<CattleCarcass>> CATTLE_CARCASS = ENTITIES.register("cattle_carcass",
            id -> EntityType.Builder.<CattleCarcass>of(CattleCarcass::new, MobCategory.MISC)
                    .sized(1.6F, 0.8F)
                    .clientTrackingRange(8)
                    .build(key(id)));
    // 35b: a town's December tree. Sized for the eye (so it isn't culled with its foot off screen); it has no collision
    public static final DeferredHolder<EntityType<?>, EntityType<com.hollowfrontier.content.frontier.season.TownTree>> TOWN_TREE = ENTITIES.register("town_tree",
            id -> EntityType.Builder.<com.hollowfrontier.content.frontier.season.TownTree>of(com.hollowfrontier.content.frontier.season.TownTree::new, MobCategory.MISC)
                    .noLootTable()
                    .sized(3.0F, 7.0F)
                    .clientTrackingRange(10)
                    .updateInterval(20)
                    .build(key(id)));
    // 11b: the men in black dusters (come by event only: after a mystery, or hunting a marked player)
    public static final DeferredHolder<EntityType<?>, EntityType<ManInBlack>> MAN_IN_BLACK = ENTITIES.register("man_in_black",
            id -> EntityType.Builder.<ManInBlack>of(ManInBlack::new, MobCategory.MISC)
                    .sized(0.6F, 1.95F)
                    .clientTrackingRange(10)
                    .build(key(id)));
    // 10b: the sentinel over a live Gray field station (placed by the structure only)
    public static final DeferredHolder<EntityType<?>, EntityType<GraySentinel>> GRAY_SENTINEL = ENTITIES.register("gray_sentinel",
            id -> EntityType.Builder.<GraySentinel>of(GraySentinel::new, MobCategory.MISC)
                    .sized(0.7F, 0.45F)
                    .clientTrackingRange(10)
                    .build(key(id)));

    // 12a: the last Concavians, and the crystal constructs that guard their ruins (both placed by the structure only)
    public static final DeferredHolder<EntityType<?>, EntityType<Kindled>> KINDLED = ENTITIES.register("kindled",
            id -> EntityType.Builder.<Kindled>of(Kindled::new, MobCategory.MISC)
                    .sized(0.7F, 2.3F)
                    .eyeHeight(2.05F)
                    .clientTrackingRange(10)
                    .build(key(id)));
    public static final DeferredHolder<EntityType<?>, EntityType<CrystalGuardian>> CRYSTAL_GUARDIAN = ENTITIES.register("crystal_guardian",
            id -> EntityType.Builder.<CrystalGuardian>of(CrystalGuardian::new, MobCategory.MISC)
                    .sized(1.3F, 2.8F)
                    .eyeHeight(2.3F)
                    .fireImmune()
                    .clientTrackingRange(10)
                    .build(key(id)));

    // 12b: the boss on the Sun Temple's throne (structure-placed; a spawn egg for ops)
    public static final DeferredHolder<EntityType<?>, EntityType<SunArchon>> SUN_ARCHON = ENTITIES.register("sun_archon",
            id -> EntityType.Builder.<SunArchon>of(SunArchon::new, MobCategory.MONSTER)
                    .sized(1.2F, 3.1F)
                    .eyeHeight(2.7F)
                    .fireImmune()
                    .clientTrackingRange(12)
                    .build(key(id)));

    // 13a: the Grays' saucers over the Overworld at night (event-spawned; only a landed one, 15a, is saved), and their Wardens
    public static final DeferredHolder<EntityType<?>, EntityType<GraySaucer>> GRAY_SAUCER = ENTITIES.register("gray_saucer",
            id -> EntityType.Builder.<GraySaucer>of(GraySaucer::new, MobCategory.MISC)
                    .sized(7.0F, 1.6F)
                    .clientTrackingRange(16)
                    .updateInterval(2)
                    .build(key(id)));
    public static final DeferredHolder<EntityType<?>, EntityType<GrayWarden>> GRAY_WARDEN = ENTITIES.register("gray_warden",
            id -> EntityType.Builder.<GrayWarden>of(GrayWarden::new, MobCategory.MONSTER)
                    .sized(0.6F, 2.1F)
                    .eyeHeight(1.85F)
                    .clientTrackingRange(10)
                    .build(key(id)));
    // 21c: a Gray in a duster and a hat, trying to pass in a frontier town
    public static final DeferredHolder<EntityType<?>, EntityType<com.hollowfrontier.content.ufo.DisguisedGray>> DISGUISED_GRAY = ENTITIES.register("disguised_gray",
            id -> EntityType.Builder.<com.hollowfrontier.content.ufo.DisguisedGray>of(com.hollowfrontier.content.ufo.DisguisedGray::new, MobCategory.MISC)
                    .sized(0.6F, 2.1F)
                    .eyeHeight(1.85F)
                    .clientTrackingRange(10)
                    .build(key(id)));

    // 29: dust devils on the frontier and Ash Devils in the Ember Badlands (drawn in particles; never saved)
    public static final DeferredHolder<EntityType<?>, EntityType<DustDevil>> DUST_DEVIL = ENTITIES.register("dust_devil",
            id -> EntityType.Builder.<DustDevil>of(DustDevil::new, MobCategory.MISC)
                    .sized(2.0F, 9.0F)
                    .noSave()
                    .clientTrackingRange(10)
                    .updateInterval(2)
                    .build(key(id)));
    public static final DeferredHolder<EntityType<?>, EntityType<DustDevil>> ASH_DEVIL = ENTITIES.register("ash_devil",
            id -> EntityType.Builder.<DustDevil>of(DustDevil::new, MobCategory.MISC)
                    .sized(2.0F, 9.0F)
                    .noSave()
                    .fireImmune()
                    .clientTrackingRange(10)
                    .updateInterval(2)
                    .build(key(id)));

    // 13b: a phlogiston gun's beam as it's seen (a few ticks of light; never saved)
    public static final DeferredHolder<EntityType<?>, EntityType<PhlogistonBeam>> PHLOGISTON_BEAM = ENTITIES.register("phlogiston_beam",
            id -> EntityType.Builder.<PhlogistonBeam>of(PhlogistonBeam::new, MobCategory.MISC)
                    .sized(0.1F, 0.1F)
                    .noSave()
                    .clientTrackingRange(8)
                    .updateInterval(20)
                    .build(key(id)));

    // 13d: the Mothership (a 12-block disc; it passes through the world as it flies) and its drones
    public static final DeferredHolder<EntityType<?>, EntityType<Mothership>> MOTHERSHIP = ENTITIES.register("mothership",
            id -> EntityType.Builder.<Mothership>of(Mothership::new, MobCategory.MONSTER)
                    .sized(12.0F, 2.5F)
                    .fireImmune()
                    .clientTrackingRange(16)
                    .updateInterval(1)
                    .build(key(id)));
    public static final DeferredHolder<EntityType<?>, EntityType<GrayDrone>> GRAY_DRONE = ENTITIES.register("gray_drone",
            id -> EntityType.Builder.<GrayDrone>of(GrayDrone::new, MobCategory.MONSTER)
                    .sized(0.7F, 0.45F)
                    .clientTrackingRange(10)
                    .build(key(id)));

    // 13c: the player's own saucer (its hit box takes in the glass dome)
    public static final DeferredHolder<EntityType<?>, EntityType<PrototypeSaucer>> PROTOTYPE_SAUCER = ENTITIES.register("prototype_saucer",
            id -> EntityType.Builder.<PrototypeSaucer>of(PrototypeSaucer::new, MobCategory.MISC)
                    .sized(3.6F, 2.25F)
                    .clientTrackingRange(12)
                    .updateInterval(1)
                    .build(key(id)));

    public static final DeferredHolder<EntityType<?>, EntityType<Outlaw>> OUTLAW = ENTITIES.register("outlaw",
            id -> EntityType.Builder.<Outlaw>of(Outlaw::new, MobCategory.MONSTER)
                    .sized(0.6F, 1.95F)
                    .ridingOffset(-0.7F) // seated in a saddle, like a zombie (same height)
                    .clientTrackingRange(8)
                    .build(key(id)));
    public static final DeferredHolder<EntityType<?>, EntityType<OutlawGunslinger>> OUTLAW_GUNSLINGER = ENTITIES.register("outlaw_gunslinger",
            id -> EntityType.Builder.<OutlawGunslinger>of(OutlawGunslinger::new, MobCategory.MONSTER)
                    .sized(0.6F, 1.95F)
                    .ridingOffset(-0.7F)
                    .clientTrackingRange(10)
                    .build(key(id)));
    public static final DeferredHolder<EntityType<?>, EntityType<OutlawBoss>> OUTLAW_BOSS = ENTITIES.register("outlaw_boss",
            id -> EntityType.Builder.<OutlawBoss>of(OutlawBoss::new, MobCategory.MONSTER)
                    .sized(0.7F, 2.25F)
                    .ridingOffset(-0.81F) // scaled to his height
                    .clientTrackingRange(10)
                    .build(key(id)));
    public static final DeferredHolder<EntityType<?>, EntityType<Townsfolk>> TOWNSFOLK = ENTITIES.register("townsfolk",
            id -> EntityType.Builder.<Townsfolk>of(Townsfolk::new, MobCategory.MISC)
                    .sized(0.6F, 1.95F)
                    .clientTrackingRange(10)
                    .build(key(id)));
    public static final DeferredHolder<EntityType<?>, EntityType<Barmaid>> BARMAID = ENTITIES.register("barmaid",
            id -> EntityType.Builder.<Barmaid>of(Barmaid::new, MobCategory.MISC)
                    .sized(0.6F, 1.95F)
                    .clientTrackingRange(10)
                    .build(key(id)));
    public static final DeferredHolder<EntityType<?>, EntityType<Lawman>> LAWMAN = ENTITIES.register("lawman",
            id -> EntityType.Builder.<Lawman>of(Lawman::new, MobCategory.MISC)
                    .sized(0.6F, 1.95F)
                    .ridingOffset(-0.7F)
                    .clientTrackingRange(10)
                    .build(key(id)));
    // Steam handcar: a minecart (same size and rider seat as the furnace minecart) with an engine aboard.
    public static final DeferredHolder<EntityType<?>, EntityType<SteamHandcar>> STEAM_HANDCAR = ENTITIES.register("steam_handcar",
            id -> EntityType.Builder.<SteamHandcar>of(SteamHandcar::new, MobCategory.MISC)
                    .noLootTable()
                    .sized(0.98F, 0.7F)
                    .passengerAttachments(0.1875F)
                    .clientTrackingRange(8)
                    .build(key(id)));
    // 43c: the Steam Submersible. The hit box is the hull amidships and the crew under the dome.
    public static final DeferredHolder<EntityType<?>, EntityType<SteamSubmersible>> STEAM_SUBMERSIBLE = ENTITIES.register("steam_submersible",
            id -> EntityType.Builder.<SteamSubmersible>of(SteamSubmersible::new, MobCategory.MISC)
                    .noLootTable()
                    .sized(1.6F, 1.9F)
                    .clientTrackingRange(10)
                    .updateInterval(2)
                    .build(key(id)));
    // Airship: the hit box is the gondola and the lower half of the envelope (the rest of the envelope is only drawn).
    public static final DeferredHolder<EntityType<?>, EntityType<Airship>> AIRSHIP = ENTITIES.register("airship",
            id -> EntityType.Builder.<Airship>of(Airship::new, MobCategory.MISC)
                    .noLootTable()
                    .sized(2.5F, 3.0F)
                    .clientTrackingRange(10)
                    .updateInterval(2)
                    .build(key(id)));
    // Invisible mount for sitting on bar stools. Saved like any entity (the game refuses riders on unsaveable ones).
    public static final DeferredHolder<EntityType<?>, EntityType<SeatEntity>> SEAT = ENTITIES.register("seat",
            id -> EntityType.Builder.<SeatEntity>of(SeatEntity::new, MobCategory.MISC)
                    .noLootTable()
                    .noSummon()
                    .sized(0.001F, 0.001F)
                    .clientTrackingRange(10)
                    .build(key(id)));

    private static ResourceKey<EntityType<?>> key(net.minecraft.resources.Identifier id) {
        return ResourceKey.create(Registries.ENTITY_TYPE, id);
    }

    @SubscribeEvent
    static void createAttributes(EntityAttributeCreationEvent event) {
        event.put(OUTLAW.get(), AbstractOutlaw.createOutlawAttributes().build());
        event.put(OUTLAW_GUNSLINGER.get(), OutlawGunslinger.createAttributes().build());
        event.put(OUTLAW_BOSS.get(), OutlawBoss.createAttributes().build());
        event.put(TOWNSFOLK.get(), Townsfolk.createAttributes().build());
        event.put(BARMAID.get(), Townsfolk.createAttributes().build());
        event.put(LAWMAN.get(), Lawman.createAttributes().build());
        event.put(RATTLESNAKE.get(), Rattlesnake.createAttributes().build());
        event.put(FERN_SKITTER.get(), FernSkitter.createAttributes().build());
        event.put(GLASSBACK_HARE.get(), GlassbackHare.createAttributes().build());
        event.put(ASH_SALAMANDER.get(), AshSalamander.createAttributes().build());
        event.put(PALE_RAVEN.get(), PaleRaven.createAttributes().build());
        event.put(GRAY_PROBE.get(), GrayProbe.createAttributes().build());
        event.put(WOOLLY_MAMMOTH.get(), WoollyMammoth.createAttributes().build());
        event.put(TERROR_BIRD.get(), TerrorBird.createAttributes().build());
        event.put(MEGATHERIUM.get(), Megatherium.createAttributes().build());
        event.put(GLYPTODON.get(), Glyptodon.createAttributes().build());
        event.put(GREAT_SEA_COW.get(), GreatSeaCow.createAttributes().build());
        event.put(BASILOSAURUS.get(), Basilosaurus.createAttributes().build());
        event.put(PLESIOSAUR.get(), Plesiosaur.createAttributes().build());
        event.put(LAMPJELLY.get(), Lampjelly.createAttributes().build());
        event.put(GRAY_SENTINEL.get(), GraySentinel.createAttributes().build());
        event.put(MAN_IN_BLACK.get(), ManInBlack.createAttributes().build());
        event.put(KINDLED.get(), Kindled.createAttributes().build());
        event.put(CRYSTAL_GUARDIAN.get(), CrystalGuardian.createAttributes().build());
        event.put(SUN_ARCHON.get(), SunArchon.createAttributes().build());
        event.put(GRAY_WARDEN.get(), GrayWarden.createAttributes().build());
        event.put(DISGUISED_GRAY.get(), com.hollowfrontier.content.ufo.DisguisedGray.createAttributes().build());
        event.put(MOTHERSHIP.get(), Mothership.createAttributes().build());
        event.put(GRAY_DRONE.get(), GrayDrone.createAttributes().build());
    }

    // Natural spawns themselves come from the add_outlaw_spawns biome modifier (datagen in ModSpawns).
    @SubscribeEvent
    static void registerSpawnPlacements(RegisterSpawnPlacementsEvent event) {
        event.register(OUTLAW.get(), SpawnPlacementTypes.ON_GROUND, Heightmap.Types.MOTION_BLOCKING_NO_LEAVES,
                Monster::checkMonsterSpawnRules, RegisterSpawnPlacementsEvent.Operation.REPLACE);
        event.register(OUTLAW_GUNSLINGER.get(), SpawnPlacementTypes.ON_GROUND, Heightmap.Types.MOTION_BLOCKING_NO_LEAVES,
                Monster::checkMonsterSpawnRules, RegisterSpawnPlacementsEvent.Operation.REPLACE);
        event.register(RATTLESNAKE.get(), SpawnPlacementTypes.ON_GROUND, Heightmap.Types.MOTION_BLOCKING_NO_LEAVES,
                Rattlesnake::checkSpawnRules, RegisterSpawnPlacementsEvent.Operation.REPLACE);
        // The Hollow Earth's creatures: its dimension has a ceiling, so chunk-generation spawning looks down past the crust
        event.register(FERN_SKITTER.get(), SpawnPlacementTypes.ON_GROUND, Heightmap.Types.MOTION_BLOCKING_NO_LEAVES,
                FernSkitter::checkSpawnRules, RegisterSpawnPlacementsEvent.Operation.REPLACE);
        event.register(GLASSBACK_HARE.get(), SpawnPlacementTypes.ON_GROUND, Heightmap.Types.MOTION_BLOCKING_NO_LEAVES,
                GlassbackHare::checkSpawnRules, RegisterSpawnPlacementsEvent.Operation.REPLACE);
        event.register(ASH_SALAMANDER.get(), SpawnPlacementTypes.ON_GROUND, Heightmap.Types.MOTION_BLOCKING_NO_LEAVES,
                AshSalamander::checkSpawnRules, RegisterSpawnPlacementsEvent.Operation.REPLACE);
        event.register(PALE_RAVEN.get(), SpawnPlacementTypes.ON_GROUND, Heightmap.Types.MOTION_BLOCKING_NO_LEAVES,
                PaleRaven::checkSpawnRules, RegisterSpawnPlacementsEvent.Operation.REPLACE);
        event.register(WOOLLY_MAMMOTH.get(), SpawnPlacementTypes.ON_GROUND, Heightmap.Types.MOTION_BLOCKING_NO_LEAVES,
                WoollyMammoth::checkSpawnRules, RegisterSpawnPlacementsEvent.Operation.REPLACE);
        event.register(TERROR_BIRD.get(), SpawnPlacementTypes.ON_GROUND, Heightmap.Types.MOTION_BLOCKING_NO_LEAVES,
                TerrorBird::checkSpawnRules, RegisterSpawnPlacementsEvent.Operation.REPLACE);
        event.register(MEGATHERIUM.get(), SpawnPlacementTypes.ON_GROUND, Heightmap.Types.MOTION_BLOCKING_NO_LEAVES,
                Megatherium::checkSpawnRules, RegisterSpawnPlacementsEvent.Operation.REPLACE);
        event.register(GRAY_WARDEN.get(), SpawnPlacementTypes.ON_GROUND, Heightmap.Types.MOTION_BLOCKING_NO_LEAVES,
                GrayWarden::checkSpawnRules, RegisterSpawnPlacementsEvent.Operation.REPLACE);
        event.register(GLYPTODON.get(), SpawnPlacementTypes.ON_GROUND, Heightmap.Types.MOTION_BLOCKING_NO_LEAVES,
                Glyptodon::checkSpawnRules, RegisterSpawnPlacementsEvent.Operation.REPLACE);
        // 43b: the sea's creatures, in water (SeaLife places them; this is for whatever else asks)
        event.register(GREAT_SEA_COW.get(), SpawnPlacementTypes.IN_WATER, Heightmap.Types.MOTION_BLOCKING_NO_LEAVES,
                SeaLife::inWater, RegisterSpawnPlacementsEvent.Operation.REPLACE);
        event.register(BASILOSAURUS.get(), SpawnPlacementTypes.IN_WATER, Heightmap.Types.MOTION_BLOCKING_NO_LEAVES,
                SeaLife::inWater, RegisterSpawnPlacementsEvent.Operation.REPLACE);
        event.register(PLESIOSAUR.get(), SpawnPlacementTypes.IN_WATER, Heightmap.Types.MOTION_BLOCKING_NO_LEAVES,
                SeaLife::inWater, RegisterSpawnPlacementsEvent.Operation.REPLACE);
        event.register(LAMPJELLY.get(), SpawnPlacementTypes.IN_WATER, Heightmap.Types.MOTION_BLOCKING_NO_LEAVES,
                SeaLife::inWater, RegisterSpawnPlacementsEvent.Operation.REPLACE);
    }
}
