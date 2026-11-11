package com.hollowfrontier.gametest;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

import com.hollowfrontier.HollowFrontier;

import net.minecraft.core.Holder;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.FunctionGameTestInstance;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.gametest.framework.TestData;
import net.minecraft.gametest.framework.TestEnvironmentDefinition;
import net.minecraft.resources.ResourceKey;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.RegisterGameTestsEvent;
import net.neoforged.neoforge.registries.DeferredRegister;

/**
 * Game tests for everything built so far. Run headless with `gradlew runGameTestServer`
 * (exits non-zero if any required test fails). Test functions are always registered (harmless);
 * test instances only exist when NeoForge enables game tests.
 */
@EventBusSubscriber(modid = HollowFrontier.MODID)
public final class ModGameTests {
    private ModGameTests() {}

    public static final DeferredRegister<Consumer<GameTestHelper>> FUNCTIONS =
            DeferredRegister.create(Registries.TEST_FUNCTION, HollowFrontier.MODID);

    private record Entry(ResourceKey<Consumer<GameTestHelper>> key, int maxTicks, String structure) {}

    private static final List<Entry> TESTS = new ArrayList<>();

    static {
        // Data
        test("recipes_load", 20, DataTests::recipesLoad);
        test("brass_steps_aside", 20, DataTests::brassStepsAside);
        test("loot_tables_load", 20, DataTests::lootTablesLoad);
        test("towns_stay_off_lootr_blacklist", 20, DataTests::townsStayOffLootrBlacklist);
        test("tags_apply", 20, DataTests::tagsApply);
        test("worldgen_registered", 20, DataTests::worldgenRegistered);
        test("structure_templates_load", 20, DataTests::structureTemplatesLoad);
        test("saloon_places", 40, DataTests::saloonPlaces);
        test("town_pieces_connect", 20, DataTests::townPiecesConnect);
        test("town_layout_rules", 200, DataTests::townLayoutRules);
        test("templates_connect_blocks", 20, DataTests::templatesConnectBlocks);
        test("blacksmith_front_is_open", 20, DataTests::blacksmithFrontIsOpen);
        test("every_item_in_a_tab", 20, TabTests::everyItemInATab);
        test("tab_variants_and_order", 20, TabTests::tabVariantsAndOrder);
        test("vanilla_tabs_get_mod_items", 20, TabTests::vanillaTabsGetModItems);
        // Frontier
        test("revolver_hits_and_uses_ammo", 20, FrontierTests::revolverHitsAndUsesAmmo);
        test("gun_reload_takes_inventory_ammo", 20, FrontierTests::gunReloadTakesInventoryAmmo);
        test("gun_reload_needs_caps", 20, FrontierTests::gunReloadNeedsCaps);
        test("firing_stops_reload", 20, FrontierTests::firingStopsReload);
        test("shotgun_pellets_stack_damage", 20, FrontierTests::shotgunPelletsStackDamage);
        test("headshots_hit_harder", 60, FrontierTests::headshotsHitHarder);
        // Gun enchantments (22)
        test("gun_enchantments_fit_the_right_guns", 20, GunEnchantmentTests::enchantmentsFitTheRightGuns);
        test("gun_enchantments_change_the_tuning", 20, GunEnchantmentTests::enchantmentsChangeTheTuning);
        test("rifling_hits_harder", 20, GunEnchantmentTests::riflingHitsHarder);
        test("focus_hits_harder", 20, GunEnchantmentTests::focusHitsHarder);
        test("phlogiston_guns_stay_ahead", 20, GunEnchantmentTests::phlogistonGunsStayAhead);
        test("gun_tooltips_show_damage", 20, GunEnchantmentTests::gunTooltipsShowDamage);
        test("quick_load_loads_sooner", 20, GunEnchantmentTests::quickLoadLoadsSooner);
        test("frugal_saves_rounds", 20, GunEnchantmentTests::frugalSavesRounds);
        test("infinity_loads_for_free", 20, GunEnchantmentTests::infinityLoadsForFree);
        test("blacksmith_sells_a_gun_book", 20, GunEnchantmentTests::blacksmithSellsAGunBook);
        test("lasso_leashes_cow", 80, FrontierTests::lassoLeashesCow);
        test("cowboy_set_bonus_speeds_horse", 20, FrontierTests::cowboySetBonusSpeedsHorse);
        test("horse_gear_fit_and_strip", 20, FrontierTests::horseGearFitAndStrip);
        test("outlaw_spawns_armed", 20, FrontierTests::outlawSpawnsArmed);
        test("outlaw_shoots_player", 300, FrontierTests::outlawShootsPlayer);
        test("outlaw_stops_to_aim", 300, FrontierTests::outlawStopsToAim);
        test("boss_drops_badge", 60, FrontierTests::bossDropsBadge);
        test("bounty_board_loop", 20, FrontierTests::bountyBoardLoop);
        test("bounty_gang_spawns_near_hideout", 20, FrontierTests::bountyGangSpawnsNearHideout);
        test("townsfolk_trades", 20, FrontierTests::townsfolkTrades);
        test("barmaid_sells_drinks", 20, FrontierTests::barmaidSellsDrinks);
        test("barmaid_specialties", 20, FrontierTests::barmaidSpecialties);
        test("whiskey_makes_tipsy", 20, FrontierTests::whiskeyMakesTipsy);
        test("too_much_liquor_blacks_out", 20, FrontierTests::tooMuchLiquorBlacksOut);
        test("sobering", 20, FrontierTests::sobering);
        // Town life
        test("bar_stool_seats_player", 20, TownTests::barStoolSeatsPlayer);
        test("jailed_outlaw_is_harmless", 120, TownTests::jailedOutlawIsHarmless);
        test("lawman_shoots_outlaw", 300, TownTests::lawmanShootsOutlaw);
        test("lawman_answers_townsfolk", 20, TownTests::lawmanAnswersTownsfolk);
        test("townsfolk_have_looks", 20, TownTests::townsfolkHaveLooks);
        test("sheriff_office_staffed", 40, TownTests::sheriffOfficeStaffed);
        test("town_doors_are_clear", 20, TownTests::townDoorsAreClear);
        test("town_doors_have_bare_thresholds", 20, TownTests::townDoorsHaveBareThresholds);
        test("sheriff_rug_clears_the_door", 20, TownTests::sheriffRugClearsTheDoor, TestBlueprints.YARD);
        test("folk_give_up_on_unreachable_place", 400, TownTests::folkGiveUpOnUnreachablePlace);
        test("folk_pass_bare_door", 400, TownTests::folkPassBareDoor);
        test("folk_pass_door_beside_carpet", 400, TownTests::folkPassDoorBesideCarpet);
        test("town_buildings_furnished", 40, TownTests::townBuildingsFurnished);
        test("town_names_and_arches", 20, TownTests::townNamesAndArches);
        test("every_town_has_gunsmith", 20, TownTests::everyTownHasGunsmith);
        test("bath_sobers_up", 20, TownTests::bathSobersUp);
        test("town_lanterns_hang", 20, TownTests::townLanternsHang);
        test("chair_seats_player", 20, TownTests::chairSeatsPlayer);
        test("town_lamp_follows_the_day", 20, TownTests::townLampFollowsTheDay);
        test("new_folk_trade", 20, TownTests::newFolkTrade);
        test("routine_sends_folk_about", 20, TownTests::routineSendsFolkAbout);
        test("town_stairs_climb", 20, TownTests::townStairsClimb, TestBlueprints.YARD);
        test("depot_walk_through", 20, TownTests::depotWalkThrough, TestBlueprints.YARD);
        // Town cats
        test("town_templates_have_cats", 20, CatTests::townTemplatesHaveCats);
        test("one_sheriff_star_per_town", 40, CatTests::oneSheriffStarPerTown);
        test("cat_data_survives_save", 20, CatTests::catDataSurvivesSave);
        test("cat_accessories_on_and_off", 20, CatTests::accessoriesOnAndOff);
        test("rattlesnakes_avoid_cats", 200, CatTests::rattlesnakesAvoidCats);
        test("vanilla_cat_taming_still_works", 20, CatTests::vanillaTamingStillWorks);
        test("ghost_cat_ritual", 20, CatTests::ghostCatRitual);
        test("star_cat_spots_outlaws", 20, CatTests::starCatSpotsOutlaws);
        test("sheriff_cat_follows_sheriff", 300, CatTests::sheriffCatFollowsSheriff);
        test("cats_settle_by_role", 20, CatTests::catsSettleByRole);
        test("cat_finds_hearth", 20, CatTests::catFindsHearth);
        // The card table (17)
        test("blackjack_hand_values", 20, CardTableTests::handValues);
        test("blackjack_dealer_soft_17", 20, CardTableTests::dealerSoft17);
        test("blackjack_payouts", 20, CardTableTests::payouts);
        test("blackjack_menu_moves_coins", 20, CardTableTests::menuMovesCoins);
        test("blackjack_leaving_settles", 20, CardTableTests::leavingSettles);
        test("card_table_needs_a_dealer", 20, CardTableTests::needsADealer);
        // The saloon bounty board (18)
        test("bounty_board_posts_bounties", 20, BountyBoardTests::postsBounties);
        test("bounty_board_take_marks_taken", 20, BountyBoardTests::takeMarksTaken);
        test("bounty_board_turn_in_and_dawn", 20, BountyBoardTests::turnInAndDawn);
        test("bounty_board_stale_posters", 20, BountyBoardTests::stalePostersComeDown);
        test("bounty_boards_share_a_list", 20, BountyBoardTests::boardsShareAList);
        test("bounty_board_posts_wanted_players", 20, BountyBoardTests::wantedPlayersPosted);
        test("bounty_board_old_boards_catch_up", 20, BountyBoardTests::oldBoardsCatchUp);
        test("hanging_bounty_board_sizes", 20, BountyBoardTests::hangingBoardSizes);
        test("hanging_bounty_board_take_by_click", 20, BountyBoardTests::hangingBoardTakeByClick);
        test("structure_bounty_boards_hang", 20, BountyBoardTests::structureBoardsHang);
        // Law and order
        test("crimes_make_you_wanted", 20, LawTests::crimesMakeYouWanted);
        test("wanted_cools_off", 20, LawTests::wantedCoolsOff);
        test("lawmen_shoot_wanted_on_sight", 100, LawTests::lawmenShootWantedOnSight);
        test("posse_rides_out", 20, LawTests::posseRidesOut);
        test("folk_have_own_trades", 20, LawTests::folkHaveOwnTrades);
        test("gazette_prints_on_read", 20, LawTests::gazettePrintsOnRead);
        // Out on the range
        test("dynamite_blasts_blocks", 80, WildernessTests::dynamiteBlastsBlocks);
        test("rattlesnake_warns_first", 80, WildernessTests::rattlesnakeWarnsFirst);
        test("rattlesnake_bite_poisons", 200, WildernessTests::rattlesnakeBitePoisons);
        test("snakeskin_boots_spare_wearer", 100, WildernessTests::snakeskinBootsSpareWearer);
        test("snakeskin_boots_recipe", 20, WildernessTests::snakeskinBootsRecipe);
        // Music and sound
        test("sound_events_registered", 20, MusicTests::soundEventsRegistered);
        test("piano_controls", 20, MusicTests::pianoControls);
        test("piano_repertoire_resolves", 20, MusicTests::pianoRepertoireResolves);
        // The steam bridge: handcar, depot standpipe, railroad engineer
        test("handcar_burns_steam_and_moves", 40, RailTests::handcarBurnsSteamAndMoves);
        test("handcar_fills_at_standpipe", 200, RailTests::handcarFillsAtStandpipe);
        test("rider_drives_handcar", 40, RailTests::riderDrivesHandcar);
        test("boiler_draws_from_well", 60, RailTests::boilerDrawsFromWell);
        test("engineer_trades", 20, RailTests::engineerTrades);
        test("depot_has_standpipe", 40, RailTests::depotHasStandpipe);
        // Phase 8: the airship and the Polar Gateway
        test("airship_burns_coal_and_climbs", 60, ExpeditionTests::airshipBurnsCoalAndClimbs);
        test("dry_airship_sinks_gently", 40, ExpeditionTests::dryAirshipSinksGently);
        test("airship_drives_ahead", 50, ExpeditionTests::airshipDrivesAhead);
        test("pilot_flies_airship", 50, ExpeditionTests::pilotFliesAirship);
        test("moored_airship_fills_from_tank", 60, ExpeditionTests::mooredAirshipFillsFromTank);
        // Phase 31a: the airship's owner, and the coal and steam it keeps when broken
        test("airship_owner_lock_holds", 20, ExpeditionTests::airshipOwnerLockHolds);
        test("rider_uses_items_aboard", 20, ExpeditionTests::riderUsesItemsAboard);
        test("rider_bails_out_on_second_sneak", 20, ExpeditionTests::riderBailsOutOnSecondSneak, TestBlueprints.YARD);
        test("rider_steps_off_near_the_ground", 20, ExpeditionTests::riderStepsOffNearTheGround);
        test("unattended_airship_comes_down", 50, ExpeditionTests::unattendedAirshipComesDown, TestBlueprints.YARD);
        test("polar_compass_finds_its_own_way", 20, ExpeditionTests::polarCompassFindsItsOwnWay);
        test("broken_airship_keeps_its_fuel", 20, ExpeditionTests::brokenAirshipKeepsItsFuel);
        test("high_altitude_earns_advancement", 60, ExpeditionTests::highAltitudeEarnsAdvancement);
        test("resonator_opens_gateway", 80, ExpeditionTests::resonatorOpensGateway);
        test("resonator_charges_and_draws_from_airship", 20, ExpeditionTests::resonatorChargesAndDrawsFromAirship);
        test("crash_site_has_a_cargo_pod", 20, CrashCacheTests::crashSiteHasACargoPod);
        test("cargo_pod_spills_its_load", 20, CrashCacheTests::cargoPodSpillsItsLoad);
        test("gadgets_do_their_tricks", 20, CrashCacheTests::gadgetsDoTheirTricks);
        test("gateway_leads_to_hollow_earth_and_back", 40, ExpeditionTests::gatewayLeadsToHollowEarthAndBack);
        test("gateway_template_has_ring_and_chest", 20, ExpeditionTests::gatewayTemplateHasRingAndChest);
        // Phase 9a: the Hollow Earth's own world
        test("hollow_earth_registered", 20, HollowEarthTests::hollowEarthRegistered);
        test("hollow_sky_not_overridden", 20, HollowEarthTests::skyIsNotOverridden);
        test("hollow_every_biome_appears", 200, HollowEarthTests::everyBiomeAppears);
        test("hollow_land_and_crust_take_shape", 400, HollowEarthTests::landAndCrustTakeShape);
        test("hollow_landing_is_safe", 400, HollowEarthTests::landingIsSafe);
        test("hollow_landing_skips_trees_and_lava", 20, HollowEarthTests::landingSkipsTreesAndLava);
        test("return_gateway_clears_trees", 20, HollowEarthTests::returnGatewayClearsTrees);
        test("crust_lets_light_through", 20, HollowEarthTests::crustLetsLightThrough);
        test("crossing_fall_cant_kill", 60, HollowEarthTests::crossingFallCantKill);
        // 43a: the Still Sea
        test("still_sea_takes_only_the_low_ground", 400, StillSeaTests::seaTakesOnlyTheLowGround);
        test("still_sea_deep_water_has_a_bed", 400, StillSeaTests::deepWaterHasABed);
        test("still_sea_biome_is_its_own", 20, StillSeaTests::seaBiomeIsItsOwn);
        garden("still_sea_kelp_grows_on_the_shelf", 20, StillSeaTests::kelpGrowsOnTheShelf);
        garden("still_sea_seagrass_grows_in_the_shallows", 20, StillSeaTests::seagrassGrowsInTheShallows);
        // 43b: the sea's creatures and the Harpoon
        field("sea_life_stocks_by_depth", 20, SeaLifeTests::seaLifeStocksByDepth);
        field("sea_drops_and_recipes", 60, SeaLifeTests::seaDropsAndRecipes);
        field("sea_cow_grazes", 20, SeaLifeTests::seaCowGrazes);
        field("sea_cows_breed", 600, SeaLifeTests::seaCowsBreed);
        field("basilosaurus_hunts", 20, SeaLifeTests::basilosaurusHunts);
        field("plesiosaur_dives_and_fetches", 900, SeaLifeTests::plesiosaurDivesAndFetches);
        field("lampjelly_stings", 20, SeaLifeTests::lampjellyStings);
        field("harpoon_holds_reels_and_tows", 400, SeaLifeTests::harpoonHoldsReelsAndTows);
        // 43c: the Steam Submersible
        field("submersible_launches_and_charges", 100, SubmersibleTests::submersibleLaunchesAndCharges);
        field("submersible_dives_holds_and_surfaces", 600, SubmersibleTests::submersibleDivesHoldsAndSurfaces);
        field("submersible_keeps_its_crew", 60, SubmersibleTests::submersibleKeepsItsCrew);
        field("submersible_owner_lock_and_fold", 20, SubmersibleTests::submersibleOwnerLockAndFold);
        field("basilosaurus_rams_the_hull", 20, SubmersibleTests::basilosaurusRamsTheHull);
        // 43d: the Drowned Quarter, the Symmes Landing and the Sealed Hull
        test("sea_sites_go_where_they_should", 20, SeaSiteTests::seaSitesGoWhereTheyShould);
        field("drowned_quarter_keeps_its_air", 100, SeaSiteTests::drownedQuarterKeepsItsAir);
        field("symmes_landing_places", 20, SeaSiteTests::symmesLandingPlaces);
        test("sea_tables_give_what_they_promise", 20, SeaSiteTests::seaTablesGiveWhatTheyPromise);
        field("sealed_hull_lets_a_saucer_dive", 100, SeaSiteTests::sealedHullLetsASaucerDive);
        // 9b1: flora and ambience
        test("hollow_flora_in_biomes", 20, HollowFloraTests::floraInBiomes);
        garden("giant_fern_grows", 20, HollowFloraTests::giantFernGrows);
        garden("cycad_grows", 20, HollowFloraTests::cycadGrows);
        garden("crystal_outcrop_forms", 20, HollowFloraTests::crystalOutcropForms);
        garden("ash_vent_forms", 20, HollowFloraTests::ashVentForms);
        garden("standing_stones_raise", 20, HollowFloraTests::standingStonesRaise);
        test("ash_vent_burns", 20, HollowFloraTests::ashVentBurns);
        test("ash_falls", 60, HollowFloraTests::ashFalls);
        // 9b2: small creatures
        test("hollow_creatures_spawn_in_biomes", 20, CreatureTests::creaturesSpawnInTheirBiomes);
        garden("skitter_snatches_food", 200, CreatureTests::skitterSnatchesFood);
        garden("snatch_survives_the_tick_after", 20, CreatureTests::snatchSurvivesTheTickAfter);
        garden("skitter_eats_or_drops", 40, CreatureTests::skitterEatsOrDrops);
        garden("skitter_shies_from_player", 200, CreatureTests::skitterShiesFromPlayer);
        garden("skitter_tames_with_meat", 200, CreatureTests::skitterTamesWithMeat);
        garden("hare_comes_to_carrot", 300, CreatureTests::hareComesToCarrot);
        test("salamander_walks_on_lava", 80, CreatureTests::salamanderWalksOnLava);
        test("salamander_hurt_by_water", 100, CreatureTests::salamanderHurtByWater);
        garden("salamander_basks_on_vent", 1200, CreatureTests::salamanderBasksOnVent);
        garden("raven_perches_on_stone", 1200, // random wander and a 1-in-40 perch roll: 600 ticks was sometimes short (14)
                CreatureTests::ravenPerchesOnStone);
        garden("raven_flushed_by_player", 200, CreatureTests::ravenFlushedByPlayer);
        // 9b3: lore sites
        test("hollow_site_ground_skips_lakes_and_islands", 20, LoreSiteTests::groundSkipsLakesAndIslands);
        test("hollow_sites_stand_on_the_ground", 600, LoreSiteTests::sitesStandOnTheGround);
        test("kindled_sites_hold_their_stones", 20, LoreSiteTests::kindledSitesHoldTheirStones);
        field("gray_lab_places", 60, LoreSiteTests::grayLabPlaces);
        field("symmes_camp_places", 20, LoreSiteTests::symmesCampPlaces);
        garden("gray_probe_hovers", 60, LoreSiteTests::probeHovers);
        garden("gray_probe_flees_from_player", 120, LoreSiteTests::probeFleesFromPlayer);
        garden("gray_probe_flees_when_stared_at", 80, LoreSiteTests::probeFleesWhenStaredAt);
        garden("gray_probe_flees_when_hit", 20, LoreSiteTests::probeFleesWhenHit);
        // 9c: ores and gear
        test("hollow_ores_in_biomes", 20, HollowGearTests::oresInBiomes);
        test("hollow_ores_glow_and_need_iron", 20, HollowGearTests::oresGlowAndNeedIron);
        test("starsteel_paxel_does_all_three", 20, HollowGearTests::paxelDoesAllThree);
        test("starsteel_between_diamond_and_netherite", 20, HollowGearTests::starsteelSitsBetweenDiamondAndNetherite);
        test("starsteel_sun_forged_mends", 20, HollowGearTests::sunForgedMends);
        garden("starsteel_horseshoes_break_falls", 100, HollowGearTests::starsteelShoesBreakFalls);
        test("phlogiston_is_fuel", 20, HollowGearTests::phlogistonIsFuel);
        test("strange_grave_on_boot_hill", 20, HollowGearTests::strangeGraveOnBootHill);
        // 10a: the wild megafauna
        test("megafauna_spawn_in_biomes", 20, MegafaunaTests::megafaunaSpawnInTheirBiomes);
        test("megafauna_recipes_load", 20, MegafaunaTests::megafaunaRecipesLoad);
        field("megafauna_drop_their_own", 40, MegafaunaTests::megafaunaDropTheirOwn);
        field("mammoth_herd_defends", 100, MegafaunaTests::mammothHerdDefends);
        field("oliphant_calms_and_scatters", 120, MegafaunaTests::oliphantCalmsAndScatters);
        field("terror_bird_hunts", 300, MegafaunaTests::terrorBirdHunts);
        field("terror_bird_shuns_mammoths", 300, MegafaunaTests::terrorBirdShunsMammoths);
        field("terror_birds_stay_few", 20, MegafaunaTests::terrorBirdsStayFew);
        field("megatherium_browses", 1200, MegafaunaTests::megatheriumBrowses);
        field("browse_survives_the_tick_after", 20, MegafaunaTests::browseSurvivesTheTickAfter);
        field("glyptodon_hunkers", 60, MegafaunaTests::glyptodonHunkers);
        field("glyptodon_brushed_for_scute", 40, MegafaunaTests::glyptodonBrushedForScute);
        field("talon_knife_skins", 20, MegafaunaTests::talonKnifeSkins);
        field("shell_armor_turns_arrows", 20, MegafaunaTests::shellArmorTurnsArrows);
        field("hide_barding_armors_horse", 40, MegafaunaTests::hideBardingArmorsHorse);
        // 10b: the Gray experiments
        field("tagged_beasts_are_tougher", 20, GrayTests::taggedBeastsAreTougher);
        field("tagged_mammoth_hunts", 200, GrayTests::taggedMammothHunts);
        field("implant_overrides_instinct", 20, GrayTests::implantOverridesInstinct);
        field("tagged_bird_fears_no_mammoth", 160, GrayTests::taggedBirdFearsNoMammoth);
        field("tagged_beasts_drop_fragments", 40, GrayTests::taggedBeastsDropFragments);
        field("escapees_are_rare", 20, GrayTests::escapeesAreRare);
        field("sentinel_stuns_and_alarms", 200, GrayTests::sentinelStunsAndAlarms);
        test("signal_locator_listens", 20, GrayTests::signalLocatorListens);
        test("gray_station_places", 20, GrayTests::grayStationPlaces, TestBlueprints.YARD);
        // 11a: the frontier's mysteries
        field("sighting_crosses_the_sky", 60, MysteryTests::sightingCrossesTheSky);
        field("crop_circle_pressed", 40, MysteryTests::cropCirclePressed);
        field("town_wheat_fields_take_circles", 20, MysteryTests::townWheatFieldsTakeCircles);
        field("steer_mutilated", 40, MysteryTests::steerMutilated);
        field("probe_does_the_job", 500, MysteryTests::probeDoesTheJob);
        field("probe_spooked_off", 200, MysteryTests::probeSpookedOff);
        field("probe_caught_in_the_act", 40, MysteryTests::probeCaughtInTheAct);
        field("gazette_and_grumbles", 40, MysteryTests::gazetteAndGrumbles);
        test("grave_holds_the_pilot", 20, MysteryTests::graveHoldsThePilot);
        // 11b: the men in black dusters
        field("suspicion_rises_and_fades", 20, MenInBlackTests::suspicionRisesAndFades);
        field("visit_follows_mystery", 20, MenInBlackTests::visitFollowsMystery);
        field("gazette_prints_correction", 40, MenInBlackTests::gazettePrintsCorrection);
        field("they_buy_it_back", 20, MenInBlackTests::theyBuyItBack);
        field("warning_then_gunfire", 300, MenInBlackTests::warningThenGunfire);
        field("sent_hunting", 20, MenInBlackTests::sentHunting);
        field("killing_their_things_is_noticed", 20, MenInBlackTests::killingTheirThingsIsNoticed);
        // 12a: the Kindled
        test("crystal_ruins_place", 20, ConcaviaTests::crystalRuinsPlace, TestBlueprints.YARD);
        field("trading_wins_an_ally", 20, ConcaviaTests::tradingWinsAnAlly);
        field("story_told_in_order", 20, ConcaviaTests::storyToldInOrder);
        field("wronging_the_kindled", 20, ConcaviaTests::wrongingTheKindled);
        field("guardian_smells_the_grays", 100, ConcaviaTests::guardianSmellsTheGrays);
        field("guardian_warns_first", 120, ConcaviaTests::guardianWarnsFirst);
        field("guardian_beams_a_probe", 200, ConcaviaTests::guardianBeamsAProbe);
        field("sigil_shows_the_implanted", 20, ConcaviaTests::sigilShowsTheImplanted);
        // 12b: the Sun Archon
        test("sun_temple_places", 20, ConcaviaTests::sunTemplePlaces, TestBlueprints.YARD);
        // 26: the dungeons under the sites
        test("dungeon_kits_join_up", 20, DungeonTests::kitsJoinUp);
        test("dungeon_vault_seal_opens", 40, DungeonTests::vaultSealOpens, TestBlueprints.YARD);
        test("dungeon_trap_fires", 60, DungeonTests::trapFires, TestBlueprints.YARD);
        test("dungeon_hoard_moved_down", 20, DungeonTests::hoardMovedDown);
        // 27: the wild sites
        test("wild_sites_hold_their_parts", 20, WildSiteTests::wildSitesHoldTheirParts);
        test("gray_relay_hangs", 40, WildSiteTests::relayHangs, TestBlueprints.YARD);
        // 28: the Hollow loot
        test("hollow_tables_roll", 20, HollowLootTests::hollowTablesRoll);
        test("hollow_maps_point_the_right_way", 20, HollowLootTests::mapsPointTheRightWay);
        test("prospector_buys_relics", 20, HollowLootTests::prospectorBuysRelics);
        test("hollow_digs_and_patterns", 20, HollowLootTests::digsAndPatterns);
        field("archon_wakes", 60, ConcaviaTests::archonWakes);
        field("archon_phases_and_shield", 40, ConcaviaTests::archonPhasesAndShield);
        field("archon_resets", 400, ConcaviaTests::archonResets);
        field("archon_sunburst", 20, ConcaviaTests::archonSunburst);
        field("archon_falls", 40, ConcaviaTests::archonFalls);
        field("crown_wards", 20, ConcaviaTests::crownWards);
        field("lens_shines", 20, ConcaviaTests::lensShines);
        field("codex_writes", 20, ConcaviaTests::codexWrites);
        // 13a: the Grays arrive
        field("flyover_is_a_sighting", 20, UfoTests::flyoverIsASighting);
        field("beam_takes_an_animal", 400, UfoTests::beamTakesAnAnimal);
        field("beam_and_the_sneaking_player", 40, UfoTests::beamAndTheSneakingPlayer);
        field("missing_time", 20, UfoTests::missingTime);
        field("warden_fires", 300, UfoTests::wardenFires);
        field("warden_blinks", 20, UfoTests::wardenBlinks);
        test("crash_site_places", 20, UfoTests::crashSitePlaces, TestBlueprints.YARD);
        field("wreckage_gives_salvage", 20, UfoTests::wreckageGivesSalvage);
        // 15a: Gray Landings
        field("landing_needs_open_ground", 20, UfoTests::landingNeedsOpenGround);
        field("landing_comes_down", 60, UfoTests::landingComesDown);
        field("landing_leaves_at_dawn", 140, UfoTests::landingLeavesAtDawn);
        field("cleared_landing_pays_out", 140, UfoTests::clearedLandingPaysOut);
        // 13b: phlogiston tech
        field("ray_pistol_fires", 20, UfoTests::rayPistolFires);
        field("phlogiston_rifle_pierces", 20, UfoTests::phlogistonRiflePierces);
        field("beams_set_alight", 20, UfoTests::beamsSetAlight);
        field("beams_spare_the_shooters_mount", 20, UfoTests::beamsSpareTheShootersMount);
        field("cells_drain_and_empty", 20, UfoTests::cellsDrainAndEmpty);
        // 13c: the Prototype Saucer
        field("saucer_places_fits_and_folds", 20, UfoTests::saucerPlacesFitsAndFolds);
        field("saucer_flies_with_pilot", 60, UfoTests::saucerFliesWithPilot);
        field("saucer_draws_pilots_cells", 40, UfoTests::saucerDrawsPilotsCells);
        field("saucer_flies_on_charge", 100, UfoTests::saucerFliesOnCharge);
        field("tractor_beam_lifts", 60, UfoTests::tractorBeamLifts);
        field("heat_laser_burns_and_overheats", 20, UfoTests::heatLaserBurnsAndOverheats);
        field("laser_hits_what_the_pilot_sees", 20, UfoTests::laserHitsWhatThePilotSees);
        field("laser_fires_at_the_reported_sight", 20, UfoTests::laserFiresAtTheReportedSight);
        field("crew_use_items_aboard", 20, UfoTests::crewUseItemsAboard);
        // 13d: the Mothership
        field("beacon_calls_the_mothership", 130, MothershipTests::beaconCallsTheMothership);
        field("shield_holds_while_drones_fly", 20, MothershipTests::shieldHoldsWhileDronesFly);
        field("death_ray_falls_on_its_mark", 60, MothershipTests::deathRayFallsOnItsMark);
        field("abduction_beam_lifts", 20, MothershipTests::abductionBeamLifts);
        field("mothership_falls_and_leaves_salvage", 130, MothershipTests::mothershipFallsAndLeavesSalvage);
        field("leash_breaks_and_frees", 20, MothershipTests::leashBreaksAndFrees);
        // 15b: the Mothership's return
        field("rematches_come_harder", 20, MothershipReturnTests::rematchesComeHarder);
        field("rematches_give_up_fittings", 60, MothershipReturnTests::rematchesGiveUpFittings);
        field("cloak_hides_the_crew_at_night", 40, MothershipReturnTests::cloakHidesTheCrewAtNight);
        field("cargo_hold_keeps_through_folding", 20, MothershipReturnTests::cargoHoldKeepsThroughFolding);
        field("owner_lock_holds", 20, MothershipReturnTests::ownerLockHolds);
        // 15c: what the frontier makes of itself
        field("freed_man_sworn_in", 20, AftermathTests::freedManSwornIn);
        field("freed_man_goes_home", 20, AftermathTests::freedManGoesHome);
        test("ruin_restores_in_stages", 20, AftermathTests::ruinRestoresInStages, TestBlueprints.YARD);
        test("fountain_wants_an_ally_and_the_cost", 20, AftermathTests::fountainWantsAnAllyAndTheCost, TestBlueprints.YARD);
        field("waystones_carry_for_a_fare", 20, AftermathTests::waystonesCarryForAFare);
        field("gazette_reports_the_aftermath", 20, AftermathTests::gazetteReportsTheAftermath);
        // 20: the captives
        test("pens_hold_their_captives", 20, GrayPensTests::pensHoldTheirCaptives, TestBlueprints.YARD);
        test("procedure_implants", 20, GrayPensTests::procedureImplants, TestBlueprints.YARD);
        test("procedure_stops", 20, GrayPensTests::procedureStops, TestBlueprints.YARD);
        field("letter_goes_home", 20, GrayPensTests::letterGoesHome);
        field("outlaw_brought_in_alive", 20, GrayPensTests::outlawBroughtInAlive);
        test("pens_after_the_leash", 20, GrayPensTests::pensAfterTheLeash, TestBlueprints.YARD);
        // 21b: a night at the saloon
        test("slurring_is_steady", 20, SaloonNightTests::slurringIsSteady);
        test("snake_oil_promises", 20, SaloonNightTests::snakeOilPromises);
        test("missing_time_is_safe", 20, SaloonNightTests::missingTimeIsSafe, TestBlueprints.YARD);
        test("brawl_throws_one_out", 20, SaloonNightTests::brawlThrowsOneOut, TestBlueprints.YARD);
        field("moonshine_shows_things", 20, SaloonNightTests::moonshineShowsThings);
        // 21c: Grays in disguise
        field("strangers_come_after_the_leash", 20, DisguiseTests::strangersComeAfterTheLeash);
        field("lens_exposes_a_stranger", 20, DisguiseTests::lensExposesAStranger);
        field("third_visit_stays", 20, DisguiseTests::thirdVisitStays);
        field("exposing_the_regular_costs", 20, DisguiseTests::exposingTheRegularCosts);
        // 14: optional compat
        test("compat_text_and_tags", 20, CompatTests::compatTextAndTags);
        test("crushing_recipes_show_in_and_out", 20, CompatTests::crushingRecipesShowInAndOut);
        test("curios_worn_artifacts_work", 60, CompatTests::curiosWornArtifactsWork);
        // Advancements
        test("advancements_load", 20, AdvancementTests::advancementsLoad);
        test("frontier_triggers_grant", 40, AdvancementTests::frontierTriggersGrant);
        test("cat_triggers_grant", 40, AdvancementTests::catTriggersGrant);
        test("steam_triggers_grant", 200, AdvancementTests::steamTriggersGrant);
        // Steam
        test("boiler_boils_water", 100, SteamTests::boilerBoilsWater);
        test("pipes_carry_steam_to_tank", 200, SteamTests::pipesCarrySteamToTank);
        test("crusher_crushes_raw_iron", 200, SteamTests::crusherCrushesRawIron);
        test("crusher_idles_without_steam", 140, SteamTests::crusherIdlesWithoutSteam);
        test("gauge_reads_pressure", 60, SteamTests::gaugeReadsPressure);
        test("steam_finds_new_neighbour", 80, SteamTests::steamFindsNewNeighbour);
        test("canister_fills_and_drains", 20, SteamTests::canisterFillsAndDrains);
        test("steam_drill_tool_rules", 20, SteamTests::steamDrillToolRules);
        // Town raids (on their own little town)
        raid("raid_site_finds_vault", 20, RaidTests::siteFindsVault);
        raid("raid_spawns_leader_and_gang", 40, RaidTests::raidSpawnsLeaderAndGang);
        raid("one_raid_per_town", 20, RaidTests::oneRaidPerTown);
        raid("townsfolk_shelter_indoors", 400, RaidTests::townsfolkShelterIndoors);
        raid("lawmen_engage_raiders", 400, RaidTests::lawmenEngageRaiders);
        raid("mounted_gang_opens_doors", 40, RaidTests::mountedGangOpensDoors);
        raid("strongbox_ignores_cell_doors", 20, RaidTests::strongboxIgnoresCellDoors);
        raid("vault_chests_stock_themselves", 20, RaidTests::vaultChestsStockThemselves);
        raid("vault_chests_stock_while_pending", 20, RaidTests::vaultChestsStockWhilePending);
        raid("safecracker_walks_round_the_block", 300, RaidTests::safecrackerWalksRoundTheBlock);
        raid("vault_timer_robs_chest", 200, RaidTests::vaultTimerRobsChest);
        raid("killing_safecracker_interrupts", 100, RaidTests::killingSafecrackerInterrupts);
        raid("defended_raid_pays_and_pardons", 40, RaidTests::defendedRaidPaysAndPardons);
        raid("robbed_raid_posts_poster", 300, RaidTests::robbedRaidPostsPoster);
        raid("recovered_loot_earns_advancement", 200, RaidTests::recoveredLootEarnsAdvancement);
        raid("outlaw_cut_earns_advancement", 200, RaidTests::outlawCutEarnsAdvancement);
        raid("raider_dynamite_spares_town", 120, RaidTests::raiderDynamiteSparesTown);
        raid("raid_forgives_stray_shots", 20, RaidTests::raidForgivesStrayShots);
        raid("raid_state_survives_save", 40, RaidTests::raidStateSurvivesSave);
        raid("orphan_raiders_removed", 20, RaidTests::orphanRaidersRemoved);
        raid("raiders_leave_outlaw_players_be", 40, RaidTests::raidersLeaveOutlawPlayersBe);
        raid("bagman_returns_box_coins", 300, RaidTests::bagmanReturnsBoxCoins);
        raid("robbed_bank_tells_owners", 300, RaidTests::robbedBankTellsOwners);
        raid("town_talks_of_raid_and_robbery", 20, RaidTests::townTalksOfRaidAndRobbery);
        raid("arrivals_wait_out_the_raid", 40, RaidTests::arrivalsWaitOutTheRaid);
        // Towns replacing their dead
        raid("town_replaces_its_dead_one_at_a_time", 40, ArrivalTests::replacedOneAtATime);
        raid("barmaid_replaced_as_barmaid", 20, ArrivalTests::barmaidReplacedAsBarmaid);
        raid("strangers_not_replaced", 20, ArrivalTests::strangersNotReplaced);
        raid("arrivals_keep_out_of_sight", 20, ArrivalTests::arrivalsKeepOutOfSight);
        raid("arrivals_find_ground_over_lost_post", 20, ArrivalTests::arrivalsFindGroundOverLostPost);
        raid("vacancies_survive_save", 20, ArrivalTests::vacanciesSurviveSave);
        // New worlds starting in town
        field("every_square_has_a_landing", 40, TownSpawnTests::everySquareHasALanding);
        field("landing_keeps_out_from_under_roofs", 20, TownSpawnTests::landingKeepsOutFromUnderRoofs);
        test("no_town_leaves_the_spawn_alone", 20, TownSpawnTests::noTownLeavesTheSpawnAlone);
        test("towns_tag_holds_every_town", 20, TownSpawnTests::townsTagHoldsEveryTown);
        // Voices (19)
        test("postmaster_hands_over_the_letter", 20, SurveyTests::postmasterHandsOverTheLetter);
        test("letter_ticks_off_the_road", 20, SurveyTests::letterTicksOffTheRoad);
        test("survey_notice_pays_once", 20, SurveyTests::surveyNoticePaysOnce);
        test("frontier_nudges_north", 20, SurveyTests::frontierNudgesNorth);
        test("chatter_table_complete", 20, ChatterTests::tableComplete);
        test("chatter_conditions_let_lines_in", 20, ChatterTests::conditionsLetLinesIn);
        test("chatter_picks_avoid_repeats", 20, ChatterTests::picksAvoidRepeats);
        field("chatter_reads_the_world", 20, ChatterTests::contextReadsTheWorld);
        field("chatter_cooldowns_hold", 80, ChatterTests::cooldownsHold);
        field("outlaws_and_lawmen_bark", 120, ChatterTests::outlawsAndLawmenBark);
        field("townsfolk_gossip", 200, ChatterTests::townsfolkGossip);
        // Frontier humour: words (21a)
        test("gazette_back_page_table", 20, HumourTests::backPageTable);
        field("gazette_back_page_follows_the_story", 20, HumourTests::backPageFollowsTheStory);
        test("field_notes_in_the_caches", 20, HumourTests::fieldNotesInTheCaches);
        test("epitaphs_on_boot_hill", 20, HumourTests::epitaphsOnBootHill);
        field("outlaws_taunt_reloading", 80, HumourTests::outlawsTauntReloading);
        // Dust devils (29)
        field("dust_devils_their_country", 20, DustDevilTests::theirCountry);
        field("dust_devil_lifts_items_and_critters", 200, DustDevilTests::liftsItemsAndCritters);
        field("dust_devil_flings_and_lets_go", 300, DustDevilTests::flingsAndLetsGo);
        field("dust_devil_lifts_player_and_takes_hat", 200, DustDevilTests::liftsPlayerAndTakesHat);
        field("dust_devil_sneaking_and_binding", 120, DustDevilTests::sneakingAndBinding);
        field("dust_devil_horseshoes_steady", 120, DustDevilTests::horseshoesSteady);
        test("dust_devil_leaves_its_loot", 20, DustDevilTests::leavesItsLoot);
        // 30: modpack friendliness
        test("modpack_machines_expose_their_slots", 20, ModpackTests::machinesExposeTheirSlots);
        test("modpack_any_steam_works", 20, ModpackTests::anySteamWorks);
        test("modpack_convention_tags", 20, ModpackTests::conventionTags);
        test("modpack_mystery_switches_default_on", 20, ModpackTests::mysterySwitchesDefaultOn);
        field("ash_devil_burns", 400, DustDevilTests::ashDevilBurns);
        field("dust_devils_the_frontier_talks", 20, DustDevilTests::theFrontierTalks);
        // A frontier December (35)
        test("season_follows_the_date", 20, SeasonTests::seasonFollowsTheDate);
        test("lamps_dress_by_date", 20, SeasonTests::lampsDressByDate);
        test("town_tree_stands_in_december", 20, SeasonTests::townTreeStandsInDecember);
        test("parcel_once_per_year", 20, SeasonTests::parcelOncePerYear);
        test("december_drinks_come_and_go", 20, SeasonTests::decemberDrinksComeAndGo);
        test("piano_plays_carols", 20, SeasonTests::pianoPlaysCarols);
        test("december_words_keep_to_the_season", 20, SeasonTests::decemberWordsKeepToTheSeason);
        test("christmas_truce", 20, SeasonTests::christmasTruce);
        // The bank (16)
        test("bank_box_opens_for_a_gold_coin", 20, BankTests::boxOpensForAGoldCoin);
        test("bank_boxes_are_private", 20, BankTests::boxesArePrivate);
        test("bank_box_survives_save", 20, BankTests::boxSurvivesSave);
        test("bank_interest_rules", 20, BankTests::interestRules);
        test("bank_interest_pays_on_visit", 20, BankTests::interestPaysOnVisit);
        test("bank_news_waits_for_owner", 20, BankTests::newsWaitsForTheOwner);
        test("banker_serves_only_in_town", 20, BankTests::bankerServesOnlyInTown);
    }

    private static void garden(String name, int maxTicks, Consumer<GameTestHelper> function) {
        test(name, maxTicks, function, TestBlueprints.GARDEN);
    }

    private static void field(String name, int maxTicks, Consumer<GameTestHelper> function) {
        test(name, maxTicks, function, TestBlueprints.FIELD);
    }

    private static void raid(String name, int maxTicks, Consumer<GameTestHelper> function) {
        test(name, maxTicks, function, TestBlueprints.RAID_TOWN);
    }

    private static void test(String name, int maxTicks, Consumer<GameTestHelper> function) {
        test(name, maxTicks, function, TestBlueprints.PLATFORM);
    }

    private static void test(String name, int maxTicks, Consumer<GameTestHelper> function, String structure) {
        FUNCTIONS.register(name, () -> function);
        TESTS.add(new Entry(ResourceKey.create(Registries.TEST_FUNCTION, HollowFrontier.id(name)), maxTicks, structure));
    }

    @SubscribeEvent
    static void registerTests(RegisterGameTestsEvent event) {
        Holder<TestEnvironmentDefinition<?>> environment = event.registerEnvironment(HollowFrontier.id("default"));
        for (Entry entry : TESTS) {
            event.registerTest(entry.key().identifier(), new FunctionGameTestInstance(entry.key(),
                    new TestData<>(environment, HollowFrontier.id(entry.structure()), entry.maxTicks(), 1, true)));
        }
    }
}
