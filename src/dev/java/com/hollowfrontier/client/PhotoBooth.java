package com.hollowfrontier.client;

import java.util.List;

import com.hollowfrontier.HollowFrontier;
import com.hollowfrontier.content.frontier.gear.DynamiteEntity;
import com.hollowfrontier.content.frontier.gun.GunItem;
import com.hollowfrontier.content.frontier.gun.GunMechanics;
import com.hollowfrontier.content.frontier.gun.GunTypes;
import com.hollowfrontier.content.frontier.gun.GunWielder;
import com.hollowfrontier.content.frontier.outlaw.AbstractOutlaw;
import com.hollowfrontier.content.frontier.raid.RaidTimings;
import com.hollowfrontier.content.frontier.raid.TownRaids;
import com.hollowfrontier.content.frontier.raid.TownSite;
import com.hollowfrontier.content.frontier.saloon.Barmaid;
import com.hollowfrontier.content.frontier.townsfolk.Lawman;
import com.hollowfrontier.worldgen.structure.TownLayout;
import com.hollowfrontier.worldgen.structure.TownStyle;
import com.hollowfrontier.content.frontier.townsfolk.Townsfolk;
import com.hollowfrontier.content.steam.BoilerBlockEntity;
import com.hollowfrontier.content.steam.SteamHandcar;
import com.hollowfrontier.content.steam.airship.Airship;
import com.hollowfrontier.content.hollow.gateway.GatewayEffects;
import com.hollowfrontier.content.hollow.gray.ImplantRigBlockEntity;
import com.hollowfrontier.content.hollow.gateway.GatewayLinks;
import com.hollowfrontier.registry.ModAttachments;
import com.hollowfrontier.content.hollow.gateway.PolarGateways;
import com.hollowfrontier.worldgen.structure.HollowBlueprints;
import com.hollowfrontier.worldgen.structure.ConcaviaBlueprints;
import com.hollowfrontier.worldgen.structure.WildBlueprints;
import com.hollowfrontier.content.concavia.WaystoneBlock;
import com.hollowfrontier.content.concavia.Waystones;
import com.hollowfrontier.worldgen.structure.PolarBlueprints;
import com.hollowfrontier.worldgen.HollowEarth;
import com.hollowfrontier.registry.ModEntities;
import com.hollowfrontier.registry.ModItems;

import net.minecraft.client.CameraType;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.levelgen.structure.StructureStart;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.animal.equine.Horse;
import net.minecraft.world.entity.decoration.ArmorStand;
import net.minecraft.world.entity.decoration.ItemFrame;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.level.material.Fluids;
import net.neoforged.neoforge.transfer.fluid.FluidResource;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.advancements.AdvancementsScreen;
import net.minecraft.client.gui.screens.inventory.CreativeModeInventoryScreen;
import net.minecraft.world.item.CreativeModeTab;
import com.hollowfrontier.registry.ModTabs;
import net.minecraft.client.Screenshot;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;

/**
 * Dev-only visual check (`gradlew runPhoto`, i.e. -Dhollowfrontier.photo=true): once a singleplayer world is open,
 * builds a sky platform, lines up the mod's humanoids (armed, facing the camera), saves screenshots to
 * run/screenshots/hf_*.png from several angles, then quits. Does nothing unless the property is set.
 */
@EventBusSubscriber(modid = HollowFrontier.MODID, value = Dist.CLIENT)
public final class PhotoBooth {
    private PhotoBooth() {}

    private static final boolean ENABLED = Boolean.getBoolean("hollowfrontier.photo");
    private static final List<String> MOBS = List.of("outlaw", "outlaw_gunslinger", "outlaw_boss", "townsfolk");
    private static final int BARMAID_Z = -6; // all five hostesses in a row north of the start, facing south
    private static final int CAT_Z = -14;    // cats further north, facing south: every coat (and the ghost), then the accessories
    private static final int ACCESSORY_X = 10;
    private static final List<String> COATS = List.of("minecraft:tabby", "minecraft:black", "minecraft:red", "minecraft:siamese",
            "minecraft:british_shorthair", "minecraft:calico", "minecraft:persian", "minecraft:ragdoll", "minecraft:white", "minecraft:jellie",
            "minecraft:all_black", "hollowfrontier:desert", "hollowfrontier:saloon");
    // Red bandana, blue bandana, cowboy hat, brass star: standing, then the same four sitting.
    private static final List<String> ACCESSORIES = List.of(
            "{id:\"hollowfrontier:bandana\",count:1,components:{\"minecraft:dyed_color\":11546150}}",
            "{id:\"hollowfrontier:bandana\",count:1,components:{\"minecraft:dyed_color\":3949738}}",
            "{id:\"hollowfrontier:cowboy_hat\",count:1}",
            "{id:\"hollowfrontier:brass_star\",count:1}");
    // The town visits: one slot of TOWN_TICKS per style, from TOWNS_START (after the cats, before the raid)
    private static final int TOWNS_START = 1435;
    private static final int TOWN_TICKS = 480;
    private static final int VAULT_X = 30; // the staged vault, on the platform north-east of the barmaids
    private static final int VAULT_Z = -10;
    private static final int RIDER_X = 44; // a lone rider, east of the vault
    private static java.util.UUID riderId = new java.util.UUID(0, 0);
    private static final int Y = 250;
    private static final int ROW_Z = 5;

    private static int ticks = -1;
    private static int baseX;
    private static int baseZ;

    @SubscribeEvent
    static void onClientTick(ClientTickEvent.Post event) {
        if (!ENABLED) {
            return;
        }
        Minecraft mc = Minecraft.getInstance();
        MinecraftServer server = mc.getSingleplayerServer();
        if (mc.player == null || server == null) {
            return;
        }
        if (ticks < 0) {
            baseX = mc.player.getBlockX();
            baseZ = mc.player.getBlockZ();
            releaseForceloads(server); // in case an earlier run stopped before it could
            HollowFrontier.LOGGER.info("[PhotoBooth] world loaded, staging at {} {} {}", baseX, Y, baseZ);
        }
        ticks++;
        // Keep shooting when the window is in the background: no pause menu (it would cover every shot and stop the server).
        mc.options.pauseOnLostFocus = false;
        if (mc.gui.screen() instanceof net.minecraft.client.gui.screens.PauseScreen) {
            mc.gui.setScreen(null);
        }
        // The crash site's cargo pod and the Grays' gadgets (32c), only on their own with -PphotoScene=cache
        if (CACHE_ONLY) {
            if (ticks >= 40) {
                cache(mc, server, ticks - 40);
            }
            return;
        }
        // The airship's bail-out and the Polar Compass (32b), only on their own with -PphotoScene=bailout
        if (BAILOUT_ONLY) {
            if (ticks >= 40) {
                bailout(mc, server, ticks - 40);
            }
            return;
        }
        // The Hollow Earth's ores and gear (9c), only on their own with -PphotoScene=gear
        if (GEAR_ONLY) {
            if (ticks >= 40) {
                gear(mc, server, ticks - 40);
            }
            return;
        }
        // The card table (17) and the bounty board (18), only on their own with -PphotoScene=saloon
        if (SALOON_ONLY) {
            if (ticks >= 40) {
                saloon(mc, server, ticks - 40);
            }
            return;
        }
        // The Mothership (13d), only on its own with -PphotoScene=mothership
        if (MOTHERSHIP_ONLY) {
            if (ticks >= 40) {
                mothership(mc, server, ticks - 40);
            }
            return;
        }
        // The Prototype Saucer (13c), only on its own with -PphotoScene=saucer
        if (SAUCER_ONLY) {
            if (ticks >= 40) {
                saucer(mc, server, ticks - 40);
            }
            return;
        }
        // Phlogiston tech (13b), only on its own with -PphotoScene=phlogiston
        if (PHLOGISTON_ONLY) {
            if (ticks >= 40) {
                phlogiston(mc, server, ticks - 40);
            }
            return;
        }
        // Gray Landings (15a), only on their own with -PphotoScene=landing
        if (LANDING_ONLY) {
            if (ticks >= 40) {
                landing(mc, server, ticks - 40);
            }
            return;
        }
        // Dust devils (29), only on their own with -PphotoScene=dustdevils
        if (DUST_DEVILS_ONLY) {
            if (ticks >= 40) {
                dustDevils(mc, server, ticks - 40);
            }
            return;
        }
        // The Grays arrive (13a), only on their own with -PphotoScene=grays
        if (GRAYS_ONLY) {
            if (ticks >= 40) {
                grays(mc, server, ticks - 40);
            }
            return;
        }
        // The Sun Archon (12b), only on its own with -PphotoScene=archon
        if (ARCHON_ONLY) {
            if (ticks >= 40) {
                archon(mc, server, ticks - 40);
            }
            return;
        }
        // The Kindled (12a), only on their own with -PphotoScene=crystal
        if (CRYSTAL_ONLY) {
            if (ticks >= 40) {
                crystal(mc, server, ticks - 40);
            }
            return;
        }
        // A Crystal Ruins restored and its waystone (15c), only on their own with -PphotoScene=restored
        if (RESTORED_ONLY) {
            if (ticks >= 40) {
                restored(mc, server, ticks - 40);
            }
            return;
        }
        // Grays in disguise (21c), only on its own with -PphotoScene=disguise
        if (DISGUISE_ONLY) {
            if (ticks >= 40) {
                disguise(mc, server, ticks - 40);
            }
            return;
        }
        // A night at the saloon (21b), only on its own with -PphotoScene=tipsy
        if (TIPSY_ONLY) {
            if (ticks >= 40) {
                tipsy(mc, server, ticks - 40);
            }
            return;
        }
        // The Gray pens and their captives (20), only on their own with -PphotoScene=pens
        if (PENS_ONLY) {
            if (ticks >= 40) {
                pens(mc, server, ticks - 40);
            }
            return;
        }
        // Town playtest checks (the depot's back, stairs, wheat fields), only on their own with -PphotoScene=towncheck
        if (TOWNCHECK_ONLY) {
            if (ticks >= 40) {
                townCheck(mc, server, ticks - 40);
            }
            return;
        }
        // The town buildings one by one, outside and with the roof off, then a real town of each style (23), only with
        // -PphotoScene=townbuild
        if (TOWNBUILD_ONLY) {
            if (ticks >= 40) {
                townBuild(mc, server, ticks - 40);
            }
            return;
        }
        // The Hollow Earth's sites one by one, outside, inside and with the roof off (25-27), only with -PphotoScene=hollowbuild
        if (HOLLOWBUILD_ONLY) {
            if (ticks >= 40) {
                hollowBuild(mc, server, ticks - 40);
            }
            return;
        }
        // The dungeons under the sites (26), each grown whole in the sky and shot with its roofs off, only with
        // -PphotoScene=dungeons
        if (DUNGEONS_ONLY) {
            if (ticks >= 40) {
                dungeons(mc, server, ticks - 40);
            }
            return;
        }
        // Close-ups of the spots playtesting found, only with -PphotoScene=townfix
        if (TOWNFIX_ONLY) {
            if (ticks >= 40) {
                townFix(mc, server, ticks - 40);
            }
            return;
        }
        // The worn Cowboy Hat, only on its own with -PphotoScene=hat
        if (HAT_ONLY) {
            if (ticks >= 40) {
                hat(mc, server, ticks - 40);
            }
            return;
        }
        // The enchanted guns' glint (22), only on its own with -PphotoScene=glint
        if (GLINT_ONLY) {
            if (ticks >= 40) {
                glint(mc, server, ticks - 40);
            }
            return;
        }
        // The muzzle smoke as the shooter sees it, only on its own with -PphotoScene=smoke
        if (SMOKE_ONLY) {
            if (ticks >= 40) {
                smoke(mc, server, ticks - 40);
            }
            return;
        }
        // Every humanoid look on the shared model (31c), only on their own with -PphotoScene=people
        if (PEOPLE_ONLY) {
            if (ticks >= 40) {
                people(mc, server, ticks - 40);
            }
            return;
        }
        // The men in black (11b), only on their own with -PphotoScene=black
        if (BLACK_ONLY) {
            if (ticks >= 40) {
                black(mc, server, ticks - 40);
            }
            return;
        }
        // The frontier's mysteries (11a), only on their own with -PphotoScene=mystery
        if (MYSTERY_ONLY) {
            if (ticks >= 40) {
                mystery(mc, server, ticks - 40);
            }
            return;
        }
        // The Gray experiments (10b), only on their own with -PphotoScene=gray
        if (GRAY_ONLY) {
            if (ticks >= 40) {
                gray(mc, server, ticks - 40);
            }
            return;
        }
        // The wild megafauna (10a), only on their own with -PphotoScene=megafauna
        if (MEGAFAUNA_ONLY) {
            if (ticks >= 40) {
                megafauna(mc, server, ticks - 40);
            }
            return;
        }
        // The Hollow Earth's lore sites (9b3), only on their own with -PphotoScene=sites
        if (SITES_ONLY) {
            if (ticks >= 40) {
                sites(mc, server, ticks - 40);
            }
            return;
        }
        // The Hollow Earth's life (9b): flora and creatures close up, only on its own with -PphotoScene=life
        if (LIFE_ONLY) {
            if (ticks >= 40) {
                life(mc, server, ticks - 40);
            }
            return;
        }
        // The Hollow Earth (phase 9a): after everything else, or on its own with -PphotoScene=hollow
        if (HOLLOW_ONLY) {
            if (ticks >= 40) {
                hollow(mc, server, ticks - 40);
            }
            return;
        }
        if (ticks >= HOLLOW_START) {
            hollow(mc, server, ticks - HOLLOW_START);
            return;
        }
        // The airship and polar gateway scene: after everything else, or on its own with -PphotoScene=expedition
        if (EXPEDITION_ONLY) {
            if (ticks >= 40) {
                expedition(mc, server, ticks - 40, true);
            }
            return;
        }
        if (ticks >= EXPEDITION_START) {
            expedition(mc, server, ticks - EXPEDITION_START, false);
            return;
        }
        // Real towns, one of each style, generated fresh (see visitTowns)
        if (ticks >= TOWNS_START && ticks < TOWNS_START + TOWN_TICKS * TownStyle.values().length) {
            visitTowns(mc, server, ticks - TOWNS_START);
            return;
        }
        switch (ticks) {
            case 40 -> setup(server);
            case 50 -> {
                mc.options.cloudStatus().set(net.minecraft.client.CloudStatus.OFF); // no cloud layer between the aerial shots and the towns
                // Entities from earlier runs load in after setup's kill; clear the props again (not the new mobs).
                run(server, "kill @e[type=minecraft:armor_stand]");
                run(server, "kill @e[type=minecraft:item_frame]");
                run(server, "kill @e[type=minecraft:item]");
                faceCamera(server);
                mc.player.getInventory().setSelectedSlot(8); // empty slot: no hands in the way
                tp(server, baseX, baseZ, 0, 10);
            }
            case 110 -> shot(mc, "hf_1_lineup.png");
            case 115 -> {
                tp(server, baseX - 3, baseZ + 2, 0, 20);
                gunPose(server, GunWielder.GunPose.AIMING);
            }
            case 160 -> shot(mc, "hf_2_outlaw_close_aiming.png");
            case 165 -> tp(server, baseX - 7, baseZ + ROW_Z, -90, 5);
            case 210 -> shot(mc, "hf_3_row_side_aiming.png");
            case 212 -> gunPose(server, GunWielder.GunPose.RELOADING);
            case 240 -> shot(mc, "hf_3b_row_side_reloading.png");
            case 242 -> {
                gunPose(server, GunWielder.GunPose.LOWERED);
                tp(server, baseX, baseZ + ROW_Z + 4, 180, 10); // behind the row (mobs face north), looking north
            }
            case 260 -> shot(mc, "hf_4_lineup_back.png");
            case 265 -> {
                mc.player.getInventory().setSelectedSlot(0); // revolver
                mc.options.setCameraType(CameraType.THIRD_PERSON_FRONT);
                tp(server, baseX, baseZ, 0, 0);
            }
            case 310 -> shot(mc, "hf_5_player_revolver_3p.png");
            case 315 -> mc.options.setCameraType(CameraType.FIRST_PERSON);
            case 360 -> shot(mc, "hf_6_player_revolver_1p.png");
            case 365 -> mc.player.getInventory().setSelectedSlot(1); // plains rifle
            case 400 -> shot(mc, "hf_6b_player_rifle_1p.png");
            case 405 -> mc.player.getInventory().setSelectedSlot(2); // shotgun
            case 440 -> shot(mc, "hf_6c_player_shotgun_1p.png");
            // The server must see the new slot before loading starts, or it cancels the reload (gun not in hand).
            case 442 -> {
                mc.player.getInventory().setSelectedSlot(1);
                // Invisible camera stand 3 blocks east, looking west at the player, for side-on third-person shots.
                run(server, String.format("summon minecraft:armor_stand %d.5 %d %d.5 {Invisible:1b,NoGravity:1b,Rotation:[90f,15f],Tags:[\"hf_cam\"]}",
                        baseX + 3, Y, baseZ));
            }
            case 447 -> startReload(server, 1);
            case 472 -> shot(mc, "hf_6d_player_rifle_reload_1p.png");
            case 474 -> sideCamera(mc, true);
            case 480 -> shot(mc, "hf_6e_player_rifle_reload_3p.png");
            case 500 -> shot(mc, "hf_6f_player_rifle_3p.png");
            case 502 -> mc.player.getInventory().setSelectedSlot(0);
            case 510 -> shot(mc, "hf_6g_player_revolver_3p.png");
            case 512 -> mc.player.getInventory().setSelectedSlot(2);
            case 520 -> shot(mc, "hf_6h_player_shotgun_3p.png");
            case 522 -> {
                sideCamera(mc, false);
                run(server, "kill @e[tag=hf_cam]");
                mc.player.getInventory().setSelectedSlot(0);
            }
            case 527 -> startReload(server, 0);
            case 535 -> shot(mc, "hf_6i_player_revolver_reload_1p.png");
            case 538 -> {
                mc.player.getInventory().setSelectedSlot(8);
                setupGallery(server);
            }
            case 550 -> fireBoiler(server);
            case 570 -> tp(server, baseX + 32, baseZ + 1, 0, 25);
            case 600 -> shot(mc, "hf_7a_blocks_decor.png");
            case 605 -> tp(server, baseX + 24, baseZ + 1, 0, 25);
            case 620 -> shot(mc, "hf_7b_blocks_materials.png");
            case 625 -> tp(server, baseX + 46, baseZ - 1, 0, 25);
            case 670 -> shot(mc, "hf_8_blocks_steam.png");
            case 675 -> tp(server, baseX + 68, baseZ - 4, 0, -6);
            case 720 -> shot(mc, "hf_9_item_wall.png");
            case 725 -> tp(server, baseX + 64, baseZ + 2, 0, -8);
            case 765 -> shot(mc, "hf_10_item_wall_left.png");
            case 770 -> tp(server, baseX + 72, baseZ + 2, 0, -8);
            case 810 -> shot(mc, "hf_11_item_wall_right.png");
            case 815 -> tp(server, baseX + 84, baseZ + 2, 0, 12);
            case 860 -> shot(mc, "hf_12_cowboy_armor.png");
            case 862 -> tp(server, baseX, baseZ - 1, 180, 6);
            case 900 -> shot(mc, "hf_13_barmaids.png");
            case 902 -> tp(server, baseX - 3, baseZ + BARMAID_Z + 2, 180, 14);
            case 930 -> shot(mc, "hf_14_barmaids_close_left.png");
            case 932 -> tp(server, baseX + 3, baseZ + BARMAID_Z + 2, 180, 14);
            case 960 -> shot(mc, "hf_15_barmaids_close_right.png");
            case 962 -> tp(server, baseX - 8, baseZ + BARMAID_Z, -90, 4);
            case 990 -> shot(mc, "hf_16_barmaids_side.png");
            case 995 -> run(server, "gamemode spectator @p"); // so the camera hangs where it is put instead of falling
            // Town cats, back at the start (the camera is still a spectator, so it hangs where it's put)
            case 1275 -> tp(server, baseX, baseZ + CAT_Z + 7, 180, 20); // just north of the barmaid row
            case 1330 -> shot(mc, "hf_21_cat_coats.png");
            case 1335 -> tp(server, baseX + ACCESSORY_X + 3, baseZ + CAT_Z + 5, 180, 22);
            case 1370 -> shot(mc, "hf_22_cat_accessories.png");
            case 1375 -> tp(server, baseX + ACCESSORY_X + 1, baseZ + CAT_Z + 3, 180, 30); // the four standing
            case 1400 -> shot(mc, "hf_23_cat_accessories_close.png");
            case 1405 -> tp(server, baseX + ACCESSORY_X + 7, baseZ + CAT_Z + 3, 135, 25); // three-quarter view of the sitting four
            case 1430 -> shot(mc, "hf_24_cat_accessories_side.png");
            // A town raid by night on the same town: a live one (warning, the gang riding in, the fight), then staged
            // shots (the gang in the street, a dynamiter mid-throw, the sheriff's cat showing one up, a safecracker at a vault)
            case 2900 -> {
                run(server, "time set 18000");
                mc.options.gamma().set(1.0);
                stageVault(server);
                stageRider(server);
                pickRaidSite(server);
            }
            case 2903 -> raidView(server, 0);
            case 2910 -> startLiveRaid(server); // once the camera is in town, so it gets the title
            case 2940 -> shot(mc, "hf_25_raid_warning.png");
            case 3000 -> shot(mc, "hf_26_raid_riders_aerial.png");
            case 3005 -> raidView(server, 1);
            case 3065 -> shot(mc, "hf_27_raid_street.png");
            case 3070 -> raidView(server, 0);
            case 3125 -> shot(mc, "hf_28_raid_fight_aerial.png");
            case 3130 -> raidView(server, 1);
            case 3165 -> shot(mc, "hf_28b_raid_fight_street.png");
            case 3170 -> stageTableau(server);
            case 3177 -> raidView(server, 2);
            case 3225 -> shot(mc, "hf_29_raid_tableau.png");
            case 3230 -> raidView(server, 3);
            case 3265 -> shot(mc, "hf_30_dynamiter_throw.png");
            case 3270 -> raidView(server, 4);
            case 3305 -> shot(mc, "hf_31_star_cat_glow.png");
            case 3310 -> {
                tp(server, baseX + VAULT_X - 1, baseZ + VAULT_Z + 4, -145, 12); // front left: the door beside the safecracker
                vaultFx(server);
            }
            case 3337 -> vaultFx(server);
            case 3345 -> shot(mc, "hf_32_safecracker_vault.png");
            // A rider in the saddle side on, then knocked off (the server unseats him, as a hit does in a raid) and seen again
            case 3347 -> {
                run(server, "kill @e[type=hollowfrontier:outlaw_boss]"); // earlier runs' lineup bosses, and their bars
                tp(server, baseX + RIDER_X + 3, baseZ + VAULT_Z, 90, 12);
            }
            case 3375 -> shot(mc, "hf_33_rider_mounted.png");
            case 3377 -> server.execute(() -> {
                Entity rider = server.overworld().getEntity(riderId);
                if (rider != null) {
                    rider.stopRiding();
                }
            });
            // Once he has landed and stood a moment, bring him in front of the camera (frozen, facing it) to check his legs
            case 3403 -> server.execute(() -> {
                if (server.overworld().getEntity(riderId) instanceof AbstractOutlaw rider) {
                    rider.setNoAi(true);
                    rider.snapTo(baseX + RIDER_X + 1.5, Y, baseZ + VAULT_Z - 0.8, -90.0F, 0.0F);
                    rider.setYBodyRot(-90.0F);
                    rider.setYHeadRot(-90.0F);
                }
            });
            case 3415 -> shot(mc, "hf_34_rider_unseated.png");
            // The steam bridge: a railroad depot on the platform with its standpipe and parked handcar, a second handcar
            // with the engineer aboard pulling away under steam, the handcar in hand, then the advancement tabs.
            case 3420 -> stageDepot(server);
            case 3430 -> tpLooking(server, depot(13, 9, 30), depot(12, 0, 10));
            case 3445 -> server.execute(() -> depotHandcars(server).forEach(car -> {
                car.setSteam(SteamHandcar.CAPACITY);
                if (car.isVehicle()) {
                    car.setThrottle(1);
                }
            }));
            case 3460 -> shot(mc, "hf_35_depot.png");
            case 3462 -> tpLooking(server, depot(10, 1.5, 18.5), depot(11, 0.5, 13.5));
            case 3480 -> shot(mc, "hf_36_handcars.png");
            case 3482 -> tpLooking(server, depot(18.5, 1.6, 16.5), depot(21.5, 1.0, 12.5));
            case 3500 -> shot(mc, "hf_37_standpipe.png");
            case 3502 -> {
                run(server, "gamemode creative @p");
                run(server, "item replace entity @p hotbar.0 with hollowfrontier:steam_handcar");
                mc.player.getInventory().setSelectedSlot(0);
                tpLooking(server, depot(6, 0, 18), depot(6, 0, 30));
            }
            case 3525 -> shot(mc, "hf_38_handcar_in_hand.png");
            case 3527 -> {
                mc.player.getInventory().setSelectedSlot(8);
                // Some of each tab earned (so later ones show as goals), toasts cleared so they don't cover the screen.
                run(server, "advancement grant @p until hollowfrontier:frontier/hold_the_line");
                run(server, "advancement grant @p until hollowfrontier:frontier/wet_your_whistle");
                run(server, "advancement grant @p until hollowfrontier:frontier/dressed_to_the_nines");
                run(server, "advancement grant @p until hollowfrontier:steam/toot_toot");
            }
            case 3560 -> {
                mc.gui.toastManager().clear();
                AdvancementsScreen screen = new AdvancementsScreen(mc.player.connection.getAdvancements());
                mc.gui.setScreen(screen);
                mc.player.connection.getAdvancements().setSelectedTab(mc.player.connection.getAdvancements().get(HollowFrontier.id("frontier/root")), true);
            }
            case 3580 -> shot(mc, "hf_39_advancements_frontier.png");
            case 3582 -> {
                mc.gui.toastManager().clear();
                mc.player.connection.getAdvancements().setSelectedTab(mc.player.connection.getAdvancements().get(HollowFrontier.id("steam/root")), true);
            }
            case 3600 -> shot(mc, "hf_40_advancements_steam.png");
            case 3602 -> mc.gui.setScreen(null);
            // Each mod tab open in the creative inventory
            case 3605 -> creativeTab(mc, ModTabs.HOLLOW_FRONTIER.get());
            case 3625 -> shot(mc, "hf_41_tab_frontier.png");
            case 3627 -> creativeTab(mc, ModTabs.HOLLOW_FRONTIER.get());
            case 3645 -> shot(mc, "hf_42_tab_frontier_building.png");
            case 3647 -> creativeTab(mc, ModTabs.HOLLOW_FRONTIER.get());
            case 3665 -> shot(mc, "hf_43_tab_steam.png");
            case 3667 -> mc.gui.setScreen(null);
            default -> {}
        }
    }

    // ---------------------------------------------------------------- 32b: bailing out, and the Polar Compass

    private static final boolean BAILOUT_ONLY = "bailout".equals(System.getProperty("hollowfrontier.photo.scene"));
    private static Airship bailShip;
    private static @org.jspecify.annotations.Nullable BlockPos compassTarget;

    /**
     * A survival player at the helm high over the staging spot with a Polar Compass in hand (the gauge line carries its
     * reading), one sneak (a warning, still aboard), a second (off, drifting down under slow falling), then a look straight
     * down on the gateway the compass found, to see the needle aims at the ring itself. The log says what happened.
     */
    private static void bailout(Minecraft mc, MinecraftServer server, int t) {
        switch (t) {
            case 0 -> {
                run(server, "time set noon");
                run(server, "weather clear");
                run(server, "gamemode survival @p");
                run(server, "kill @e[type=hollowfrontier:airship]");
                run(server, "item replace entity @p weapon.mainhand with hollowfrontier:polar_compass");
            }
            case 5 -> server.execute(() -> {
                ServerPlayer player = server.getPlayerList().getPlayers().get(0);
                bailShip = airshipAt(server.overworld(), new Vec3(baseX + 0.5, Y + 40, baseZ + 0.5), 0, Airship.CAPACITY, Airship.COAL_TICKS * 4);
                player.teleportTo(bailShip.getX(), bailShip.getY() + 1, bailShip.getZ());
                player.startRiding(bailShip, true, false);
            });
            case 10 -> mc.options.setCameraType(CameraType.FIRST_PERSON);
            case 60 -> {
                shot(mc, "hf_32b_gauge_with_compass.png");
                HollowFrontier.LOGGER.info("[PhotoBooth] 32b: aboard {}, {} blocks up", mc.player.getVehicle() != null,
                        bailShip.heightAboveGround(server.overworld()));
            }
            case 62 -> mc.options.keyShift.setDown(true);
            case 66 -> mc.options.keyShift.setDown(false);
            case 70 -> {
                shot(mc, "hf_32b_bail_warning.png");
                HollowFrontier.LOGGER.info("[PhotoBooth] 32b: after one sneak, still aboard: {}", mc.player.getVehicle() != null);
            }
            case 74 -> mc.options.keyShift.setDown(true);
            case 78 -> mc.options.keyShift.setDown(false);
            case 90 -> {
                shot(mc, "hf_32b_bailed_out.png");
                HollowFrontier.LOGGER.info("[PhotoBooth] 32b: after two sneaks, aboard: {}, slow falling: {}", mc.player.getVehicle() != null,
                        mc.player.hasEffect(net.minecraft.world.effect.MobEffects.SLOW_FALLING));
            }
            case 100 -> server.execute(() -> {
                ServerLevel level = server.overworld();
                ServerPlayer player = server.getPlayerList().getPlayers().get(0);
                var gateway = level.registryAccess().lookupOrThrow(net.minecraft.core.registries.Registries.STRUCTURE)
                        .getOrThrow(com.hollowfrontier.worldgen.structure.ModStructures.POLAR_GATEWAY);
                var found = level.getChunkSource().getGenerator().findNearestMapStructure(level, net.minecraft.core.HolderSet.direct(gateway),
                        player.blockPosition(), com.hollowfrontier.content.hollow.gateway.PolarCompassItem.SEARCH_CHUNKS, false);
                compassTarget = com.hollowfrontier.content.hollow.gateway.PolarCompassItem.bearing(level, player.blockPosition()).orElse(null);
                HollowFrontier.LOGGER.info("[PhotoBooth] 32b: structure search found {}, the compass aims at {}",
                        found == null ? null : found.getFirst(), compassTarget);
            });
            case 105 -> {
                if (compassTarget != null) {
                    run(server, "effect clear @p");
                    run(server, "gamemode spectator @p");
                    run(server, String.format("tp @p %d %d %d 0 90", compassTarget.getX(), compassTarget.getY() + 25, compassTarget.getZ()));
                }
            }
            case 260 -> {
                shot(mc, "hf_32b_compass_target_from_above.png");
                HollowFrontier.LOGGER.info("[PhotoBooth] 32b: done");
            }
            default -> {}
        }
    }

    // ---------------------------------------------------------------- 32c: the crash site's cargo pod and the Grays' gadgets

    private static final boolean CACHE_ONLY = "cache".equals(System.getProperty("hollowfrontier.photo.scene"));

    /** A sealed and an open cargo pod side by side, and the five gadgets and a charged resonator in the hotbar. */
    private static void cache(Minecraft mc, MinecraftServer server, int t) {
        switch (t) {
            case 0 -> {
                run(server, "time set noon");
                run(server, "weather clear");
                run(server, "gamemode creative @p");
                run(server, String.format("setblock %d %d %d hollowfrontier:gray_cargo_pod[open=false]", baseX + 2, Y, baseZ + 4));
                run(server, String.format("setblock %d %d %d hollowfrontier:gray_cargo_pod[open=true]", baseX + 4, Y, baseZ + 4));
                String[] gadgets = {"humming_doohickey", "self_warming_rock", "backward_watch", "restless_marble", "unopenable_box"};
                for (int i = 0; i < gadgets.length; i++) {
                    run(server, "item replace entity @p hotbar." + i + " with hollowfrontier:" + gadgets[i]);
                }
                run(server, "item replace entity @p hotbar.5 with hollowfrontier:polar_resonator[hollowfrontier:steam=1200]");
                run(server, "item replace entity @p hotbar.6 with hollowfrontier:gray_cargo_pod");
                mc.player.getInventory().setSelectedSlot(8);
            }
            case 5 -> tpLooking(server, new Vec3(baseX + 3.5, Y + 1.6, baseZ + 1.0), new Vec3(baseX + 3.5, Y + 0.5, baseZ + 4.5));
            case 40 -> shot(mc, "hf_32c_cargo_pods.png");
            default -> {}
        }
    }

    // ---------------------------------------------------------------- phase 8: the airship and the polar gateway

    private static final boolean EXPEDITION_ONLY = "expedition".equals(System.getProperty("hollowfrontier.photo.scene"));
    private static final int EXPEDITION_START = 3680;
    private static final int EXPEDITION_X = 100; // east of the main platform
    private static Airship parked;
    private static Airship flying;

    private static Vec3 exp(double x, double y, double z) {
        return new Vec3(baseX + EXPEDITION_X + x, Y + y, baseZ + z);
    }

    /** The crater's well centre: the template goes in at (30, -10, -30) from the scene's corner, its well 11 in and 2 up. */
    private static BlockPos well() {
        return BlockPos.containing(exp(30 + PolarBlueprints.CENTER, -PolarBlueprints.SINK + PolarBlueprints.RING_Y, -30 + PolarBlueprints.CENTER));
    }

    /**
     * A Polar Gateway crater hung in the sky (dark, then opened), a parked airship from three sides and from above, one
     * under way with its propeller turning, the helm in first and third person, the compass and resonator in hand, the
     * Steam tab again, and finally a trip through the gateway to look round the Hollow Earth (`hf_44`-`hf_57`).
     */
    private static void expedition(Minecraft mc, MinecraftServer server, int t, boolean alone) {
        Vec3 a = exp(-5, -1, -35), b = exp(60, 12, 30);
        if (t == 0) {
            // The scene is east of where the camera starts: load it before building (commands fail on unloaded chunks).
            run(server, String.format("forceload add %d %d %d %d", (int) a.x, (int) a.z, (int) b.x, (int) b.z));
            run(server, "gamemode spectator @p");
            tpLooking(server, exp(28, 10, 0), exp(28, 0, -10));
            run(server, "kill @e[type=hollowfrontier:airship]");
            return;
        }
        if (t < 40) {
            return;
        }
        switch (t - 40) {
            case 0 -> {
                run(server, "time set noon");
                run(server, "weather clear");
                mc.options.cloudStatus().set(net.minecraft.client.CloudStatus.OFF);
                for (int y = Y; y <= Y + 20; y++) { // one layer at a time: a single fill this size is over the command block limit
                    run(server, String.format("fill %d %d %d %d %d %d minecraft:air", (int) a.x, y, (int) a.z, (int) b.x, y, (int) b.z));
                }
                run(server, String.format("fill %d %d %d %d %d %d minecraft:stone", (int) a.x, Y - 1, (int) a.z, (int) b.x, Y - 1, (int) b.z));
                Vec3 crater = exp(30, -PolarBlueprints.SINK, -30);
                run(server, String.format("place template hollowfrontier:polar_gateway %d %d %d", (int) crater.x, (int) crater.y, (int) crater.z));
                run(server, "gamemode spectator @p");
            }
            case 5 -> server.execute(() -> {
                ServerLevel level = server.overworld();
                level.getEntitiesOfClass(Airship.class, new AABB(exp(-5, -2, -35), exp(60, 30, 30))).forEach(Entity::discard);
                parked = airshipAt(level, exp(12.5, 0, 10.5), 0, Airship.CAPACITY, Airship.COAL_TICKS * 2);
                flying = airshipAt(level, exp(12.5, 6, -14.5), 90, Airship.CAPACITY, Airship.COAL_TICKS);
            });
            case 10 -> tpLooking(server, exp(55, 14, -2), Vec3.atCenterOf(well()));
            case 50 -> shot(mc, "hf_44_gateway_crater.png");
            case 52 -> tpLooking(server, Vec3.atCenterOf(well()).add(0, 5, 7), Vec3.atCenterOf(well()));
            case 75 -> shot(mc, "hf_45_gateway_ring_dark.png");
            // Woken as a resonator would (stone by stone); it floods while the airship is being shot, and is shot again after
            case 77 -> server.execute(() -> GatewayEffects.awaken(server.overworld(), well()));
            case 95 -> shot(mc, "hf_46_gateway_waking.png");
            // The parked airship: side, three-quarter, bow, above
            case 97 -> tpLooking(server, exp(28, 3, 10.5), exp(12.5, 2.5, 10.5));
            case 125 -> shot(mc, "hf_47_airship_side.png");
            case 127 -> tpLooking(server, exp(24, 6, 24), exp(12.5, 2.5, 10.5));
            case 150 -> shot(mc, "hf_48_airship_three_quarter.png");
            case 152 -> tpLooking(server, exp(12.5, 3, 26), exp(12.5, 2.5, 10.5));
            case 175 -> shot(mc, "hf_49_airship_bow.png");
            case 177 -> tpLooking(server, exp(12.6, 18, 12), exp(12.5, 0, 10.5));
            case 200 -> shot(mc, "hf_50_airship_above.png");
            // Under way: propeller turning, smoke from the stack
            case 202 -> {
                tpLooking(server, exp(18, 9, -2), exp(12.5, 7.5, -14.5));
                server.execute(() -> flying.setHelm(1, 0));
            }
            case 214 -> shot(mc, "hf_51_airship_under_way.png");
            case 216 -> server.execute(() -> flying.setHelm(0, 0));
            // At the helm: board the parked one in creative
            case 220 -> run(server, "gamemode creative @p");
            case 222 -> server.execute(() -> {
                ServerPlayer player = server.getPlayerList().getPlayers().get(0);
                player.teleportTo(parked.getX(), parked.getY() + 1, parked.getZ());
                player.startRiding(parked, true, false);
                player.setYRot(0);
                player.setXRot(10);
            });
            case 226 -> {
                mc.options.setCameraType(CameraType.FIRST_PERSON);
                mc.player.setYRot(0);
                mc.player.setXRot(10);
            }
            case 250 -> shot(mc, "hf_52_airship_helm_1p.png");
            case 252 -> mc.options.setCameraType(CameraType.THIRD_PERSON_BACK);
            case 275 -> shot(mc, "hf_53_airship_helm_3p.png");
            case 277 -> {
                mc.options.setCameraType(CameraType.FIRST_PERSON);
                server.execute(() -> server.getPlayerList().getPlayers().get(0).stopRiding());
                run(server, "item replace entity @p hotbar.0 with hollowfrontier:polar_compass");
                run(server, "item replace entity @p hotbar.1 with hollowfrontier:polar_resonator");
                mc.player.getInventory().setSelectedSlot(0);
            }
            case 280 -> tpLooking(server, exp(30, 1.6, 0), exp(30, 1.6, -10));
            case 300 -> shot(mc, "hf_54_compass_in_hand.png");
            case 302 -> mc.player.getInventory().setSelectedSlot(1);
            case 320 -> shot(mc, "hf_55_resonator_in_hand.png");
            case 322 -> {
                mc.player.getInventory().setSelectedSlot(8);
                creativeTab(mc, ModTabs.HOLLOW_FRONTIER.get());
            }
            case 342 -> shot(mc, "hf_56_tab_steam_expedition.png");
            case 344 -> mc.gui.setScreen(null);
            // The open gateway: its beam from far off, then the well close up
            case 346 -> tpLooking(server, exp(-5, 30, 45), Vec3.atCenterOf(well()).add(0, 25, 0));
            case 370 -> shot(mc, "hf_46b_gateway_beam.png");
            case 372 -> tpLooking(server, Vec3.atCenterOf(well()).add(0, 5, 7), Vec3.atCenterOf(well()));
            case 388 -> shot(mc, "hf_46c_gateway_open.png");
            // Through the gateway as a survival player: frost while it charges, the white-out, the drift down, the landing.
            // The first-visit title is wanted in the shots, so forget earlier runs' visit.
            case 390 -> {
                run(server, "gamemode survival @p");
                server.execute(() -> server.getPlayerList().getPlayers().get(0).setData(ModAttachments.VISITED_HOLLOW_EARTH, false));
                BlockPos well = well();
                run(server, String.format("tp @p %d.5 %d %d.5 0 25", well.getX(), well.getY(), well.getZ()));
            }
            case 414 -> shot(mc, "hf_56b_gateway_charging.png");
            case 437 -> shot(mc, "hf_57a_crossing_flash.png");
            case 458 -> shot(mc, "hf_57b_descent.png");
            case 462 -> mc.options.setCameraType(CameraType.THIRD_PERSON_BACK);
            case 470 -> shot(mc, "hf_57c_descent_3p.png");
            case 472 -> mc.options.setCameraType(CameraType.FIRST_PERSON);
            case 520 -> shot(mc, "hf_57d_landed.png");
            case 525 -> {
                ServerPlayer player = server.getPlayerList().getPlayers().get(0);
                BlockPos inside = GatewayLinks.get(server.overworld()).fromOverworld(well()).map(GatewayLinks.Link::hollow).orElse(player.blockPosition());
                ServerLevel hollow = server.getLevel(PolarGateways.inside());
                HollowFrontier.LOGGER.info("[PhotoBooth] after the gateway: {} at {}; return gateway at {}, ring {}, open {}",
                        player.level().dimension().identifier(), player.blockPosition(), inside,
                        hollow != null && PolarGateways.isRing(hollow, inside), hollow != null && PolarGateways.isOpen(hollow, inside));
                run(server, "gamemode spectator @p");
                // From the south, above and behind the stone travellers arrive on, looking down at the well.
                run(server, String.format("execute in %s run tp @p %d.5 %d %d.5 180 32", PolarGateways.inside().identifier(),
                        inside.getX(), inside.getY() + 7, inside.getZ() + 11));
            }
            case 560 -> shot(mc, "hf_57_hollow_earth_return_gateway.png");
            case 562 -> {
                ServerPlayer player = server.getPlayerList().getPlayers().get(0);
                run(server, String.format(java.util.Locale.ROOT, "tp @p %.1f %.1f %.1f 0 -5", player.getX(), player.getY() + 20, player.getZ()));
            }
            case 630 -> shot(mc, "hf_58_hollow_earth_view.png");
            case 640 -> // back to the start, so the next run begins in the Overworld where it always has
                    run(server, String.format("execute in minecraft:overworld run tp @p %d.5 %d %d.5", baseX, Y, baseZ));
            case 650 -> {
                if (alone) { // otherwise the Hollow Earth scene follows, and uses the same crater
                    run(server, String.format("execute in minecraft:overworld run forceload remove %d %d %d %d", (int) a.x, (int) a.z, (int) b.x, (int) b.z));
                    HollowFrontier.LOGGER.info("[PhotoBooth] done, quitting");
                    quit(mc, server);
                }
            }
            default -> {}
        }
    }

    // ---------------------------------------------------------------- phase 9a: the Hollow Earth

    private static final boolean HOLLOW_ONLY = "hollow".equals(System.getProperty("hollowfrontier.photo.scene"));
    private static final int HOLLOW_START = EXPEDITION_START + 40 + 660;
    /** Waits this long after a long-distance teleport, for the land there to generate and draw. */
    private static final int GENERATE_TICKS = 300;
    private static int hollowStep;
    private static int hollowWait;
    private static int biomeIndex;
    private static BlockPos landedAt = BlockPos.ZERO;
    private static boolean revealed;
    private static final String HOLLOW_TP = "execute in " + HollowEarth.LEVEL.identifier() + " run tp @p ";
    private static BlockPos biomeSpot;
    private static final java.util.List<net.minecraft.resources.ResourceKey<net.minecraft.world.level.biome.Biome>> TOUR =
            com.hollowfrontier.worldgen.HollowBiomes.ALL;

    /**
     * The crossing as a player makes it, then a tour (`hf_60`-`hf_7x`): the crater's gateway opened, into the well in
     * survival, down Symmes' shaft (looking down it), out of the crust (looking down, then up at the inner sun, then down
     * and out over the land mid-fall), landed by the return gateway; the sun and the ceiling from the ground; then each
     * biome from high above and from its ground. Shots are cued by the fall's own progress (HollowDescent), not by the clock.
     */
    private static void hollow(Minecraft mc, MinecraftServer server, int t) {
        if (t == 0) {
            hollowStep = 0;
            revealed = false;
            hollowWait = 0;
            biomeIndex = 0;
            Vec3 a = exp(-5, -1, -35), b = exp(60, 12, 30);
            run(server, String.format("execute in minecraft:overworld run forceload add %d %d %d %d", (int) a.x, (int) a.z, (int) b.x, (int) b.z));
            run(server, "gamemode spectator @p");
            run(server, String.format("execute in minecraft:overworld run tp @p %d.5 %d %d.5", baseX, Y + 10, baseZ));
            mc.options.cloudStatus().set(net.minecraft.client.CloudStatus.OFF);
            return;
        }
        if (hollowWait > 0) {
            hollowWait--;
            return;
        }
        switch (hollowStep) {
            case 0 -> { // the crater, built afresh in the sky (as the expedition scene does), and its well opened
                Vec3 a = exp(-5, -1, -35), b = exp(60, 12, 30);
                for (int y = Y; y <= Y + 20; y++) {
                    run(server, String.format("execute in minecraft:overworld run fill %d %d %d %d %d %d minecraft:air", (int) a.x, y, (int) a.z, (int) b.x, y, (int) b.z));
                }
                run(server, String.format("execute in minecraft:overworld run fill %d %d %d %d %d %d minecraft:stone", (int) a.x, Y - 1, (int) a.z, (int) b.x, Y - 1, (int) b.z));
                Vec3 crater = exp(30, -PolarBlueprints.SINK, -30);
                run(server, String.format("execute in minecraft:overworld run place template hollowfrontier:polar_gateway %d %d %d", (int) crater.x, (int) crater.y, (int) crater.z));
                next(30);
            }
            case 1 -> {
                server.execute(() -> PolarGateways.open(server.overworld(), well()));
                run(server, "time set noon");
                next(10);
            }
            case 2 -> { // into the well as a survival player, first visit again so the title shows
                run(server, "gamemode survival @p");
                server.execute(() -> server.getPlayerList().getPlayers().get(0).setData(ModAttachments.VISITED_HOLLOW_EARTH, false));
                BlockPos well = well();
                run(server, String.format("execute in minecraft:overworld run tp @p %d.5 %d %d.5 0 25", well.getX(), well.getY(), well.getZ()));
                next(0);
            }
            case 3 -> { // through, falling down the shaft
                if (mc.level != null && mc.level.dimension() == HollowEarth.LEVEL && HollowDescent.active()) {
                    HollowFrontier.LOGGER.info("[PhotoBooth] into the Hollow Earth at {}", mc.player.blockPosition());
                    next(14);
                }
            }
            case 4 -> {
                shot(mc, "hf_60_shaft_falling.png");
                next(0);
            }
            case 5 -> { // out of the crust: looking down at it all, then up at the sun, then out over the land
                int since = HollowDescent.sinceReveal();
                if (since >= 0) {
                    revealed = true;
                }
                if (since == 10) {
                    shot(mc, "hf_61_out_of_the_crust.png");
                } else if (since == 100) {
                    mc.player.setXRot(-82.0F);
                } else if (since == 118) {
                    shot(mc, "hf_62_falling_sun_overhead.png");
                } else if (since == 124) {
                    mc.player.setXRot(38.0F);
                } else if (since == 140) {
                    shot(mc, "hf_63_mid_fall_looking_down.png");
                } else if (since == 142) {
                    mc.player.setXRot(70.0F);
                } else if (revealed && !HollowDescent.active()) {
                    landedAt = mc.player.blockPosition();
                    HollowFrontier.LOGGER.info("[PhotoBooth] landed at {} ({} in the Hollow Earth)", landedAt,
                            mc.level.dimension() == HollowEarth.LEVEL ? "still" : "NOT");
                    mc.player.setXRot(20.0F);
                    revealed = false;
                    next(30);
                } else if (since > 0 && since % 40 == 0) {
                    HollowFrontier.LOGGER.info("[PhotoBooth] falling: at {}, {} ticks out of the crust", mc.player.blockPosition(), since);
                }
            }
            case 6 -> {
                shot(mc, "hf_64_landed.png");
                run(server, "gamemode spectator @p");
                next(10);
            }
            case 7 -> { // the inner sun and the crust, from the ground
                run(server, String.format(java.util.Locale.ROOT, HOLLOW_TP + "%d.5 %d %d.5 0 -89.9", landedAt.getX() + 12, landedAt.getY() + 45, landedAt.getZ() + 12));
                next(40);
            }
            case 8 -> {
                shot(mc, "hf_65_sun_overhead.png");
                run(server, String.format(java.util.Locale.ROOT, HOLLOW_TP + "%d.5 %d %d.5 30 -24", landedAt.getX() + 12, landedAt.getY() + 45, landedAt.getZ() + 12));
                next(30);
            }
            case 9 -> {
                shot(mc, "hf_66_crust_to_the_horizon.png");
                run(server, String.format(java.util.Locale.ROOT, HOLLOW_TP + "%d.5 %d %d.5 210 -6", landedAt.getX() + 12, landedAt.getY() + 45, landedAt.getZ() + 12));
                next(30);
            }
            case 10 -> {
                shot(mc, "hf_67_horizon.png");
                next(0);
            }
            case 11 -> { // each biome: find it, hang high over it while it generates, shoot it from above
                if (biomeIndex >= TOUR.size()) {
                    hollowStep = 14;
                    return;
                }
                var key = TOUR.get(biomeIndex);
                server.execute(() -> {
                    ServerLevel level = server.getLevel(HollowEarth.LEVEL);
                    var found = level.findClosestBiome3d(b -> b.is(key), landedAt.offset(biomeIndex * 900, 0, 0), 6400, 32, 64);
                    biomeSpot = found == null ? landedAt.offset(biomeIndex * 900, 0, 0) : found.getFirst();
                    HollowFrontier.LOGGER.info("[PhotoBooth] {} at {}", key.identifier().getPath(), biomeSpot);
                });
                next(5);
            }
            case 12 -> {
                // Above: high and off to the south, looking north and down across it
                run(server, String.format(java.util.Locale.ROOT, "execute in %s run tp @p %d.5 %d %d.5 180 16", HollowEarth.LEVEL.identifier(),
                        biomeSpot.getX(), HollowEarth.CEILING_LOWEST - 60, biomeSpot.getZ() + 90));
                next(GENERATE_TICKS);
            }
            case 13 -> {
                String name = TOUR.get(biomeIndex).identifier().getPath();
                shot(mc, String.format("hf_%d_%s_above.png", 70 + biomeIndex * 2, name));
                server.execute(() -> {
                    ServerLevel level = server.getLevel(HollowEarth.LEVEL);
                    // Down from the open air under the crust to the first solid ground (trees and plants don't count)
                    BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos(biomeSpot.getX(), HollowEarth.CEILING_LOWEST - 20, biomeSpot.getZ());
                    while (pos.getY() > HollowEarth.SEA_LEVEL && (level.getBlockState(pos).isAir()
                            || level.getBlockState(pos).is(net.minecraft.tags.BlockTags.LEAVES) || level.getBlockState(pos).canBeReplaced())) {
                        pos.move(0, -1, 0);
                    }
                    biomeSpot = new BlockPos(biomeSpot.getX(), pos.getY() + 2, biomeSpot.getZ());
                });
                hollowStep = 20;
                hollowWait = 5;
            }
            case 20 -> { // and from its ground, looking out north a little upward, so the crust shows over the land
                run(server, String.format(java.util.Locale.ROOT, HOLLOW_TP + "%d.5 %d %d.5 180 -6", biomeSpot.getX(), biomeSpot.getY(), biomeSpot.getZ()));
                next(60);
            }
            case 21 -> {
                String name = TOUR.get(biomeIndex).identifier().getPath();
                shot(mc, String.format("hf_%d_%s_ground.png", 71 + biomeIndex * 2, name));
                biomeIndex++;
                hollowStep = 11;
                hollowWait = 5;
            }
            case 14 -> {
                run(server, String.format("execute in minecraft:overworld run tp @p %d.5 %d %d.5", baseX, Y, baseZ));
                next(20);
            }
            case 15 -> {
                Vec3 a = exp(-5, -1, -35), b = exp(60, 12, 30);
                run(server, String.format("execute in minecraft:overworld run forceload remove %d %d %d %d", (int) a.x, (int) a.z, (int) b.x, (int) b.z));
                HollowFrontier.LOGGER.info("[PhotoBooth] done, quitting");
                quit(mc, server);
            }
            default -> {}
        }
    }

    private static void next(int wait) {
        hollowStep++;
        hollowWait = wait;
    }

    // ---------------------------------------------------------------- phase 9b: the Hollow Earth's life, close up

    private static final boolean LIFE_ONLY = "life".equals(System.getProperty("hollowfrontier.photo.scene"));
    /** Somewhere new each run (by the clock), far from anything earlier runs generated, so every chunk is fresh. */
    private static final BlockPos LIFE_BASE = new BlockPos(61_000 + (int) (System.currentTimeMillis() / 1000 % 900) * 3000, 0, 61_000);

    /**
     * Something to photograph: the first matching block on top of a column (plants and leaves don't count as the top), in
     * the given biome, shot from {@code dist} blocks away and {@code up} blocks above it, aimed {@code aim} blocks over it,
     * from whichever side has a clear view (Framing).
     */
    private record Subject(String shot, net.minecraft.resources.ResourceKey<net.minecraft.world.level.biome.Biome> biome,
            java.util.function.Predicate<net.minecraft.world.level.block.state.BlockState> top, int dist, int up, int aim, int settle,
            String summon, int count, double lift) {
        Subject(String shot, net.minecraft.resources.ResourceKey<net.minecraft.world.level.biome.Biome> biome,
                java.util.function.Predicate<net.minecraft.world.level.block.state.BlockState> top, int dist, int up, int aim, int settle) {
            this(shot, biome, top, dist, up, aim, settle, null, 0, 0);
        }
    }

    /** Where the camera goes for a subject, and which way it looks. */
    private record Framing(BlockPos target, Vec3 camera, float yaw, float pitch) {}

    private static final List<Subject> SUBJECTS = List.of(
            new Subject("hf_80_tree_fern", com.hollowfrontier.worldgen.HollowBiomes.PRIMORDIAL_JUNGLE, s -> s.is(Blocks.DARK_OAK_LOG), 9, 2, -1, 60),
            new Subject("hf_81_cycad", com.hollowfrontier.worldgen.HollowBiomes.PRIMORDIAL_JUNGLE, s -> s.is(Blocks.JUNGLE_WOOD), 6, 2, 0, 60),
            new Subject("hf_82_crystal_outcrop", com.hollowfrontier.worldgen.HollowBiomes.CRYSTAL_STEPPES,
                    s -> s.is(Blocks.AMETHYST_CLUSTER) || s.is(Blocks.AMETHYST_BLOCK), 9, 3, -2, 60),
            new Subject("hf_83_ash_vent", com.hollowfrontier.worldgen.HollowBiomes.EMBER_BADLANDS,
                    s -> s.is(com.hollowfrontier.registry.ModBlocks.ASH_VENT.get()), 7, 2, 1, 100),
            new Subject("hf_84_ash_field", com.hollowfrontier.worldgen.HollowBiomes.EMBER_BADLANDS,
                    s -> s.is(com.hollowfrontier.registry.ModBlocks.ASH_VENT.get()), 22, 9, 4, 60),
            new Subject("hf_85_standing_stone", com.hollowfrontier.worldgen.HollowBiomes.CONCAVIAN_HIGHLANDS,
                    s -> s.is(Blocks.CHISELED_QUARTZ_BLOCK) || s.is(Blocks.QUARTZ_PILLAR), 8, 2, -1, 60),
            // 9b2's creatures, posed (no AI) on their home ground, facing the camera; the raven also in flight
            new Subject("hf_86_fern_skitters", com.hollowfrontier.worldgen.HollowBiomes.PRIMORDIAL_JUNGLE, s -> s.is(Blocks.GRASS_BLOCK), 5, 2, 1, 60,
                    "hollowfrontier:fern_skitter", 3, 0),
            new Subject("hf_87_glassback_hare", com.hollowfrontier.worldgen.HollowBiomes.CRYSTAL_STEPPES, s -> s.is(Blocks.GRASS_BLOCK), 4, 1, 1, 60,
                    "hollowfrontier:glassback_hare", 1, 0),
            new Subject("hf_88_ash_salamander", com.hollowfrontier.worldgen.HollowBiomes.EMBER_BADLANDS,
                    s -> s.is(com.hollowfrontier.registry.ModBlocks.ASH_VENT.get()), 4, 2, 1, 60, "hollowfrontier:ash_salamander", 1, 0),
            new Subject("hf_89_pale_raven_perched", com.hollowfrontier.worldgen.HollowBiomes.CONCAVIAN_HIGHLANDS,
                    s -> s.is(Blocks.CHISELED_QUARTZ_BLOCK), 4, 1, 1, 60, "hollowfrontier:pale_raven", 1, 0),
            new Subject("hf_8a_pale_raven_flying", com.hollowfrontier.worldgen.HollowBiomes.CONCAVIAN_HIGHLANDS, s -> s.is(Blocks.GRASS_BLOCK), 5, 4, 4, 60,
                    "hollowfrontier:pale_raven", 1, 3));

    private static int lifeStep;
    private static int lifeWait;
    private static int subjectIndex;
    private static int lifeTries;
    private static BlockPos lifeSpot = BlockPos.ZERO;
    private static final java.util.concurrent.atomic.AtomicReference<java.util.Optional<Framing>> lifeFound = new java.util.concurrent.atomic.AtomicReference<>();

    /** Each subject in turn (`hf_80`-`hf_8x`): find its biome, hang over it while it generates, find one, frame it, shoot. */
    private static void life(Minecraft mc, MinecraftServer server, int t) {
        if (t == 0) {
            lifeStep = 0;
            subjectIndex = 0;
            run(server, "gamemode spectator @p");
            mc.options.cloudStatus().set(net.minecraft.client.CloudStatus.OFF);
            return;
        }
        if (lifeWait > 0) {
            lifeWait--;
            return;
        }
        if (subjectIndex >= SUBJECTS.size()) {
            HollowFrontier.LOGGER.info("[PhotoBooth] life done, quitting");
            quit(mc, server);
            return;
        }
        Subject subject = SUBJECTS.get(subjectIndex);
        boolean sameBiome = subjectIndex > 0 && SUBJECTS.get(subjectIndex - 1).biome() == subject.biome();
        switch (lifeStep) {
            case 0 -> { // the subject's biome, nearest the base (the same biome is reused while it's the same)
                lifeTries = 0;
                BlockPos from = sameBiome ? lifeSpot : LIFE_BASE;
                server.execute(() -> {
                    ServerLevel level = server.getLevel(HollowEarth.LEVEL);
                    var found = level.findClosestBiome3d(b -> b.is(subject.biome()), from.atY(100), 6400, 32, 64);
                    lifeSpot = found == null ? from : found.getFirst().atY(100);
                    HollowFrontier.LOGGER.info("[PhotoBooth] {}: {} at {}", subject.shot(), subject.biome().identifier().getPath(), lifeSpot);
                });
                lifeStep = 1;
                lifeWait = 5;
            }
            case 1 -> { // hang high over it while the land round it generates
                run(server, String.format(java.util.Locale.ROOT, HOLLOW_TP + "%d.5 %d %d.5 0 90", lifeSpot.getX(), HollowEarth.CEILING_LOWEST - 40, lifeSpot.getZ()));
                lifeStep = 2;
                lifeWait = lifeTries == 0 && !sameBiome ? GENERATE_TICKS : 100;
            }
            case 2 -> { // the nearest match on top of a column, in the subject's biome, with a clear view of it
                lifeFound.set(null);
                server.execute(() -> lifeFound.set(frame(server.getLevel(HollowEarth.LEVEL), lifeSpot, subject)));
                lifeStep = 3;
                lifeWait = 5;
            }
            case 3 -> {
                var found = lifeFound.get();
                if (found == null) {
                    return; // still looking
                }
                if (found.isEmpty()) {
                    if (++lifeTries > 3) {
                        HollowFrontier.LOGGER.warn("[PhotoBooth] {}: none found near {}", subject.shot(), lifeSpot);
                        subjectIndex++;
                        lifeStep = 0;
                        return;
                    }
                    lifeSpot = lifeSpot.offset(160, 0, 0); // look further along
                    lifeStep = 1;
                    return;
                }
                Framing framing = found.get();
                HollowFrontier.LOGGER.info("[PhotoBooth] {}: framing {} from {}", subject.shot(), framing.target(), framing.camera());
                run(server, String.format(java.util.Locale.ROOT, HOLLOW_TP + "%.2f %.2f %.2f %.1f %.1f", framing.camera().x, framing.camera().y,
                        framing.camera().z, framing.yaw(), framing.pitch()));
                // The creature, posed on the subject's top facing the camera (a flock spread round it)
                for (int i = 0; i < subject.count(); i++) {
                    double ox = i == 0 ? 0 : (i % 2 == 0 ? 1.2 : -1.2), oz = i == 0 ? 0 : -0.8 * i;
                    run(server, String.format(java.util.Locale.ROOT, "execute in %s run summon %s %.2f %.2f %.2f {NoAI:1b,PersistenceRequired:1b,NoGravity:%s,Rotation:[%.1ff,0f]}",
                            HollowEarth.LEVEL.identifier(), subject.summon(), framing.target().getX() + 0.5 + ox, framing.target().getY() + 1 + subject.lift(),
                            framing.target().getZ() + 0.5 + oz, subject.lift() > 0 ? "1b" : "0b", framing.yaw() + 180.0F - i * 25.0F));
                }
                lifeStep = 4;
                lifeWait = subject.settle();
            }
            case 4 -> {
                shot(mc, subject.shot() + ".png");
                subjectIndex++;
                lifeStep = 0;
                lifeWait = 5;
            }
            default -> {}
        }
    }

    /**
     * Within 96 blocks of {@code around}, in loaded chunks: the nearest columns whose top block matches, and for the first
     * of them that can be seen, a camera spot round it (eight sides tried, south first) in open air with nothing between
     * it and the subject (leaves count as in the way: their outline, not their visual shape, which is empty).
     */
    private static java.util.Optional<Framing> frame(ServerLevel level, BlockPos around, Subject subject) {
        List<BlockPos> found = new java.util.ArrayList<>();
        BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();
        for (int x = around.getX() - 96; x <= around.getX() + 96; x++) {
            for (int z = around.getZ() - 96; z <= around.getZ() + 96; z++) {
                if (!level.hasChunk(x >> 4, z >> 4)) {
                    continue;
                }
                for (int y = HollowEarth.CEILING_LOWEST - 30; y > HollowEarth.SEA_LEVEL - 4; y--) {
                    var state = level.getBlockState(pos.set(x, y, z));
                    if (state.isAir() || state.is(net.minecraft.tags.BlockTags.LEAVES) || state.canBeReplaced() && !subject.top().test(state)) {
                        continue;
                    }
                    if (subject.top().test(state) && level.getBiome(pos).is(subject.biome())) {
                        found.add(pos.immutable());
                    }
                    break;
                }
            }
        }
        found.sort(java.util.Comparator.comparingDouble(p -> p.distSqr(around.atY(p.getY()))));
        for (BlockPos target : found.subList(0, Math.min(found.size(), 80))) {
            Vec3 aim = Vec3.atCenterOf(target).add(0, subject.aim(), 0);
            for (int side = 0; side < 8; side++) {
                double angle = Math.PI / 2 + side * Math.PI / 4;
                Vec3 camera = aim.add(Math.cos(angle) * subject.dist(), subject.up() - subject.aim(), Math.sin(angle) * subject.dist());
                BlockPos at = BlockPos.containing(camera);
                if (!level.getBlockState(at).getCollisionShape(level, at).isEmpty() || !level.getBlockState(at.above()).getCollisionShape(level, at.above()).isEmpty()) {
                    continue;
                }
                var hit = level.clip(new net.minecraft.world.level.ClipContext(camera, aim, net.minecraft.world.level.ClipContext.Block.OUTLINE,
                        net.minecraft.world.level.ClipContext.Fluid.NONE, net.minecraft.world.phys.shapes.CollisionContext.empty()));
                if (hit.getType() != net.minecraft.world.phys.HitResult.Type.MISS && hit.getLocation().distanceTo(aim) > 2.0) {
                    continue;
                }
                Vec3 d = aim.subtract(camera);
                float yaw = (float) (Math.toDegrees(Math.atan2(d.z, d.x)) - 90.0);
                float pitch = (float) -Math.toDegrees(Math.atan2(d.y, Math.hypot(d.x, d.z)));
                return java.util.Optional.of(new Framing(target, camera, yaw, pitch));
            }
        }
        return java.util.Optional.empty();
    }

    // ---------------------------------------------------------------- phase 10a: the wild megafauna

    private static final boolean MEGAFAUNA_ONLY = "megafauna".equals(System.getProperty("hollowfrontier.photo.scene"));

    /**
     * `hf_b0`-`hf_b5`: the four beasts side by side on a grass stage in the sky (posed, no AI, turned three-quarters to the
     * camera), with an armor stand in shell armor holding the knife and the Oliphant and a horse in Hide Barding for scale;
     * then each beast close up, and the Hollow Earth creative tab.
     */
    private static void megafauna(Minecraft mc, MinecraftServer server, int t) {
        switch (t) {
            case 0 -> {
                run(server, "gamemode creative @p");
                run(server, "time set 6000");
                run(server, "weather clear");
            }
            case 20 -> {
                run(server, String.format("fill %d %d %d %d %d %d minecraft:grass_block", baseX - 8, Y - 1, baseZ - 4, baseX + 32, Y - 1, baseZ + 14));
                run(server, String.format("fill %d %d %d %d %d %d minecraft:air", baseX - 8, Y, baseZ - 16, baseX + 32, Y + 8, baseZ + 14));
                String[] beasts = {"woolly_mammoth", "terror_bird", "megatherium", "glyptodon"};
                int[] at = {0, 7, 13, 19};
                for (int i = 0; i < beasts.length; i++) {
                    run(server, String.format("summon hollowfrontier:%s %d.5 %d %d.5 {NoAI:1b,PersistenceRequired:1b,Rotation:[150f,0f]}", beasts[i], baseX + at[i], Y, baseZ + 6));
                }
                run(server, String.format("summon minecraft:armor_stand %d.5 %d %d.5 {ShowArms:1b,Rotation:[160f,0f],equipment:{"
                        + "head:{id:\"hollowfrontier:glyptodon_helmet\",count:1},chest:{id:\"hollowfrontier:glyptodon_chestplate\",count:1},"
                        + "legs:{id:\"hollowfrontier:glyptodon_leggings\",count:1},feet:{id:\"hollowfrontier:glyptodon_boots\",count:1},"
                        + "mainhand:{id:\"hollowfrontier:talon_knife\",count:1},offhand:{id:\"hollowfrontier:oliphant\",count:1}}}", baseX + 23, Y, baseZ + 5));
                run(server, String.format("summon minecraft:horse %d.5 %d %d.5 {NoAI:1b,PersistenceRequired:1b,Tame:1b,Variant:2,Rotation:[150f,0f],"
                        + "equipment:{body:{id:\"hollowfrontier:hide_barding\",count:1}}}", baseX + 27, Y, baseZ + 6));
                tpLooking(server, new Vec3(baseX + 13.5, Y + 4, baseZ - 13), new Vec3(baseX + 13.5, Y + 1.2, baseZ + 6));
            }
            case 100 -> shot(mc, "hf_b0_megafauna_lineup.png");
            case 102 -> tpLooking(server, new Vec3(baseX - 4.5, Y + 3.5, baseZ - 1), new Vec3(baseX + 0.5, Y + 1.8, baseZ + 6.5));
            case 130 -> shot(mc, "hf_b1_woolly_mammoth.png");
            case 132 -> tpLooking(server, new Vec3(baseX + 9.5, Y + 2.4, baseZ + 2), new Vec3(baseX + 7.5, Y + 1.5, baseZ + 6.5));
            case 160 -> shot(mc, "hf_b2_terror_bird.png");
            case 162 -> tpLooking(server, new Vec3(baseX + 16.5, Y + 2.8, baseZ + 1), new Vec3(baseX + 13.5, Y + 1.3, baseZ + 6.5));
            case 190 -> shot(mc, "hf_b3_megatherium.png");
            case 192 -> tpLooking(server, new Vec3(baseX + 22, Y + 2.4, baseZ - 0.5), new Vec3(baseX + 22, Y + 0.9, baseZ + 6));
            case 220 -> shot(mc, "hf_b4_glyptodon_and_gear.png");
            case 222 -> creativeTab(mc, ModTabs.HOLLOW_FRONTIER.get());
            case 242 -> shot(mc, "hf_b5_tab_hollow_earth.png");
            case 245 -> {
                HollowFrontier.LOGGER.info("[PhotoBooth] megafauna done, quitting");
                quit(mc, server);
            }
            default -> {}
        }
    }

    // ---------------------------------------------------------------- phase 13d: the Mothership

    private static final boolean MOTHERSHIP_ONLY = "mothership".equals(System.getProperty("hollowfrontier.photo.scene"));

    /**
     * `hf_l0`-`hf_l6`, at dusk: a Phlogiston Beacon lit, its beam going up; the Mothership hanging over it with its drones out;
     * its death ray falling beside the player; its belly opened on the core in the last phase, from below; its abduction
     * beam over the player; burning as it falls; and the Grays' tab with the beacon. Its fall here leaves the photo world's
     * leash whole.
     */
    private static void mothership(Minecraft mc, MinecraftServer server, int t) {
        int x = baseX, z = baseZ;
        BlockPos beacon = new BlockPos(x, Y, z + 14);
        switch (t) {
            case 0 -> {
                // Survival, so the Mothership fights (it ignores creative players), and proof against what it throws
                run(server, "gamemode survival @p");
                run(server, "effect give @p minecraft:resistance infinite 4 true");
                run(server, "effect give @p minecraft:fire_resistance infinite 0 true");
                run(server, "time set 13200");
                run(server, "weather clear");
                run(server, "kill @e[type=hollowfrontier:mothership]");
                run(server, "kill @e[type=hollowfrontier:gray_drone]");
                run(server, String.format("fill %d %d %d %d %d %d minecraft:grass_block", x - 24, Y - 1, z - 10, x + 24, Y - 1, z + 38));
                run(server, String.format("fill %d %d %d %d %d %d minecraft:air", x - 24, Y, z - 10, x + 24, Y + 40, z + 38));
                // Lit as it stands (lighting it by use would start a count down to a second ship)
                run(server, String.format("setblock %d %d %d hollowfrontier:phlogiston_beacon[lit=true]", beacon.getX(), beacon.getY(), beacon.getZ()));
            }
            case 5 -> {
                tpLooking(server, new Vec3(x + 4.5, Y + 1.8, z + 9.5), new Vec3(x + 0.5, Y + 4.0, z + 14.5));
            }
            case 40 -> shot(mc, "hf_l0_beacon_lit.png");
            case 42 -> server.execute(() -> {
                // Straight to the fight, hanging over the beacon
                var ship = ModEntities.MOTHERSHIP.get().create(server.overworld(), net.minecraft.world.entity.EntitySpawnReason.COMMAND);
                ship.snapTo(x + 0.5, Y + 16, z + 20.5, 0, 0);
                ship.setAnchor(beacon);
                ship.setEndsTheLeash(false);
                ship.engage();
                server.overworld().addFreshEntity(ship);
                for (int i = 0; i < 4; i++) {
                    com.hollowfrontier.content.ufo.GrayDrone.launch(server.overworld(), ship);
                }
            });
            case 44 -> tpLooking(server, new Vec3(x + 0.5, Y + 1.6, z - 6.5), new Vec3(x + 0.5, Y + 13.0, z + 18.5));
            case 90 -> shot(mc, "hf_l1_mothership.png");
            case 92 -> forShip(server, x, z, ship -> ship.startRay(new Vec3(x + 4.5, Y, z + 4.5)));
            case 92 + com.hollowfrontier.content.ufo.Mothership.RAY_CHARGE + 3 -> shot(mc, "hf_l2_death_ray.png");
            case 140 -> forShip(server, x, z, ship -> ship.setHealth(ship.getMaxHealth() * 0.5F));
            case 145 -> forShip(server, x, z, ship -> ship.setHealth(ship.getMaxHealth() * 0.3F));
            case 150 -> tpLooking(server, new Vec3(x + 6.5, Y + 1.6, z + 6.5), new Vec3(x + 0.5, Y + 14.0, z + 16.5));
            case 210 -> shot(mc, "hf_l3_core_exposed.png");
            case 212 -> {
                tpLooking(server, new Vec3(x + 0.5, Y, z + 12.5), new Vec3(x + 0.5, Y + 16.0, z + 13.5));
                server.execute(() -> forShip(server, x, z, ship -> ship.startBeam(server.getPlayerList().getPlayers().getFirst())));
            }
            case 270 -> shot(mc, "hf_l4_abduction_beam.png");
            case 272 -> {
                tpLooking(server, new Vec3(x + 0.5, Y + 2.0, z - 8.5), new Vec3(x + 0.5, Y + 10.0, z + 14.5));
                forShip(server, x, z, ship -> ship.hurtServer(server.overworld(), server.overworld().damageSources().genericKill(), 100000));
            }
            case 312 -> shot(mc, "hf_l5_mothership_falls.png");
            case 380 -> {
                run(server, "gamemode creative @p");
                run(server, "effect clear @p");
                creativeTab(mc, ModTabs.HOLLOW_FRONTIER.get());
            }
            case 400 -> shot(mc, "hf_l6_tab_grays.png");
            case 403 -> {
                run(server, "kill @e[type=hollowfrontier:gray_drone]");
                HollowFrontier.LOGGER.info("[PhotoBooth] mothership done, quitting");
                quit(mc, server);
            }
            default -> {}
        }
    }

    private static void forShip(MinecraftServer server, int x, int z, java.util.function.Consumer<com.hollowfrontier.content.ufo.Mothership> action) {
        server.execute(() -> server.overworld().getEntitiesOfClass(com.hollowfrontier.content.ufo.Mothership.class,
                new AABB(x - 64, Y - 16, z - 64, x + 64, Y + 96, z + 64)).forEach(action));
    }

    // ---------------------------------------------------------------- phase 13c: the Prototype Saucer

    private static final boolean SAUCER_ONLY = "saucer".equals(System.getProperty("hollowfrontier.photo.scene"));

    /**
     * `hf_k0`-`hf_k5`: a Prototype Saucer set down from its item with every fitting, from the front; hovering, from below
     * (drive, emitter, lens turret); flown by the player (third person) beaming up a cow; firing the heat laser at the
     * ground; the pilot's own view through the dome; and the Grays' tab with the saucer item.
     */
    private static void saucer(Minecraft mc, MinecraftServer server, int t) {
        int x = baseX, z = baseZ;
        BlockPos at = new BlockPos(x, Y, z + 8);
        switch (t) {
            case 0 -> {
                run(server, "gamemode creative @p");
                run(server, "time set 6000");
                run(server, "weather clear");
                run(server, "kill @e[type=hollowfrontier:prototype_saucer]");
                run(server, String.format("fill %d %d %d %d %d %d minecraft:grass_block", x - 14, Y - 1, z - 8, x + 14, Y - 1, z + 24));
                run(server, String.format("fill %d %d %d %d %d %d minecraft:air", x - 14, Y, z - 8, x + 14, Y + 20, z + 24));
            }
            case 10 -> {
                server.execute(() -> {
                    var item = com.hollowfrontier.content.ufo.PrototypeSaucerItem.of(new com.hollowfrontier.content.ufo.SaucerFittings(true, true, true), 200);
                    var saucer = com.hollowfrontier.content.ufo.PrototypeSaucerItem.place(server.overworld(), at, 180.0F, item);
                    if (saucer != null) {
                        server.overworld().addFreshEntity(saucer);
                    }
                });
                tpLooking(server, new Vec3(x + 4.5, Y + 2.6, z + 3.0), new Vec3(x + 0.5, Y + 1.0, z + 8.5));
            }
            case 60 -> shot(mc, "hf_k0_saucer_parked.png");
            case 62 -> {
                server.execute(() -> {
                    for (var s : server.overworld().getEntitiesOfClass(com.hollowfrontier.content.ufo.PrototypeSaucer.class, new AABB(at).inflate(4))) {
                        s.setPos(x + 0.5, Y + 5, z + 8.5);
                    }
                });
                tpLooking(server, new Vec3(x + 4.5, Y + 1.2, z + 3.5), new Vec3(x + 0.5, Y + 5.3, z + 8.5));
            }
            case 100 -> shot(mc, "hf_k1_saucer_underside.png");
            case 102 -> server.execute(() -> {
                for (var s : server.overworld().getEntitiesOfClass(com.hollowfrontier.content.ufo.PrototypeSaucer.class, new AABB(at).inflate(8))) {
                    s.setPos(x + 4.5, Y + 9, z + 12.5);
                }
            });
            case 106 -> {
                run(server, String.format("summon minecraft:cow %d.5 %d %d.5", x + 4, Y, z + 12));
                run(server, "ride @p mount @e[type=hollowfrontier:prototype_saucer,limit=1,sort=nearest]");
                mc.options.setCameraType(CameraType.THIRD_PERSON_BACK);
            }
            case 110 -> server.execute(() -> {
                for (var s : server.overworld().getEntitiesOfClass(com.hollowfrontier.content.ufo.PrototypeSaucer.class, new AABB(at).inflate(16))) {
                    s.setControls(false, true);
                }
            });
            case 150 -> shot(mc, "hf_k2_saucer_beam.png");
            case 152 -> {
                mc.player.setXRot(35.0F);
                server.execute(() -> {
                    for (var s : server.overworld().getEntitiesOfClass(com.hollowfrontier.content.ufo.PrototypeSaucer.class, new AABB(at).inflate(16))) {
                        s.setControls(true, false);
                    }
                });
            }
            case 172 -> shot(mc, "hf_k3_saucer_laser.png");
            case 174 -> {
                server.execute(() -> {
                    for (var s : server.overworld().getEntitiesOfClass(com.hollowfrontier.content.ufo.PrototypeSaucer.class, new AABB(at).inflate(16))) {
                        s.setControls(false, false);
                    }
                });
                mc.options.setCameraType(CameraType.FIRST_PERSON);
                mc.player.setXRot(10.0F);
            }
            case 194 -> shot(mc, "hf_k4_saucer_cockpit.png");
            case 196 -> {
                run(server, "ride @p dismount");
                creativeTab(mc, ModTabs.HOLLOW_FRONTIER.get());
            }
            case 216 -> shot(mc, "hf_k5_tab_grays.png");
            case 219 -> {
                run(server, "kill @e[type=hollowfrontier:prototype_saucer]");
                HollowFrontier.LOGGER.info("[PhotoBooth] saucer done, quitting");
                quit(mc, server);
            }
            default -> {}
        }
    }

    // ---------------------------------------------------------------- phase 13b: phlogiston tech

    private static final boolean PHLOGISTON_ONLY = "phlogiston".equals(System.getProperty("hollowfrontier.photo.scene"));

    /**
     * `hf_j0`-`hf_j5`: the Ray Pistol and the Phlogiston Rifle held (third person from the front, then first person; the hotbar
     * shows their GUI pose), the pistol fired at a zombie (the beam and the recoil), and the Grays' tab with the 13b items.
     */
    private static void phlogiston(Minecraft mc, MinecraftServer server, int t) {
        int x = baseX, z = baseZ;
        switch (t) {
            case 0 -> {
                run(server, "gamemode creative @p");
                run(server, "time set 6000");
                run(server, "weather clear");
                run(server, String.format("fill %d %d %d %d %d %d minecraft:grass_block", x - 8, Y - 1, z - 8, x + 8, Y - 1, z + 16));
                run(server, String.format("fill %d %d %d %d %d %d minecraft:air", x - 8, Y, z - 8, x + 8, Y + 8, z + 16));
                String[] hotbar = {"ray_pistol", "phlogiston_rifle", "phlogiston_cell", "empty_phlogiston_cell", "gravity_drive", "damaged_gravity_drive"};
                for (int i = 0; i < hotbar.length; i++) {
                    run(server, "item replace entity @p hotbar." + i + " with hollowfrontier:" + hotbar[i]);
                }
                run(server, "item replace entity @p weapon.offhand with minecraft:air");
            }
            case 10 -> {
                mc.player.getInventory().setSelectedSlot(0);
                mc.options.setCameraType(CameraType.THIRD_PERSON_FRONT);
                tp(server, x, z, 0, 0);
            }
            case 50 -> shot(mc, "hf_j0_ray_pistol_3p.png");
            case 52 -> mc.options.setCameraType(CameraType.FIRST_PERSON);
            case 80 -> shot(mc, "hf_j1_ray_pistol_1p.png");
            case 82 -> mc.player.getInventory().setSelectedSlot(1);
            case 110 -> shot(mc, "hf_j2_phlogiston_rifle_1p.png");
            case 112 -> mc.options.setCameraType(CameraType.THIRD_PERSON_FRONT);
            case 140 -> shot(mc, "hf_j3_phlogiston_rifle_3p.png");
            case 142 -> {
                mc.options.setCameraType(CameraType.FIRST_PERSON);
                mc.player.getInventory().setSelectedSlot(0);
                run(server, String.format("summon minecraft:zombie %d.5 %d %d.5 {NoAI:1b,Rotation:[180f,0f]}", x, Y, z + 8));
            }
            case 160 -> server.execute(() -> {
                ServerPlayer player = server.getPlayerList().getPlayers().getFirst();
                ModItems.RAY_PISTOL.get().shoot(server.overworld(), player, player.getMainHandItem(), net.minecraft.world.InteractionHand.MAIN_HAND);
            });
            case 162 -> shot(mc, "hf_j4_ray_pistol_fired.png");
            case 164 -> {
                mc.player.getInventory().setSelectedSlot(1);
                run(server, "kill @e[type=minecraft:zombie,distance=..20]");
                run(server, String.format("summon minecraft:zombie %d.5 %d %d.5 {NoAI:1b,Rotation:[180f,0f]}", x, Y, z + 8));
            }
            case 170 -> server.execute(() -> {
                ServerPlayer player = server.getPlayerList().getPlayers().getFirst();
                ModItems.PHLOGISTON_RIFLE.get().shoot(server.overworld(), player, player.getMainHandItem(), net.minecraft.world.InteractionHand.MAIN_HAND);
            });
            case 172 -> shot(mc, "hf_j4c_phlogiston_rifle_fired.png");
            case 175 -> {
                mc.options.setCameraType(CameraType.FIRST_PERSON);
                run(server, "kill @e[type=minecraft:zombie,distance=..20]");
                creativeTab(mc, ModTabs.HOLLOW_FRONTIER.get());
            }
            case 195 -> shot(mc, "hf_j5_tab_grays.png");
            case 198 -> {
                HollowFrontier.LOGGER.info("[PhotoBooth] phlogiston done, quitting");
                quit(mc, server);
            }
            default -> {}
        }
    }

    // ---------------------------------------------------------------- phase 13a: the Grays arrive

    private static final boolean GRAYS_ONLY = "grays".equals(System.getProperty("hollowfrontier.photo.scene"));

    /**
     * `hf_i0`-`hf_i4`: a crash site placed on a grass stage (from above the furrow), the saucer wreck close up, a Warden, a
     * saucer beaming up a cow at night (by {@code /hollowfrontier ufo abduction}), and the Grays' creative tab.
     */
    private static void grays(Minecraft mc, MinecraftServer server, int t) {
        int x = baseX, z = baseZ;
        switch (t) {
            case 0 -> {
                run(server, "gamemode spectator @p");
                run(server, "time set 6000");
                run(server, "weather clear");
                run(server, "kill @e[type=hollowfrontier:gray_warden]");
                run(server, "kill @e[type=hollowfrontier:gray_saucer]");
            }
            case 20 -> {
                run(server, String.format("fill %d %d %d %d %d %d minecraft:grass_block", x - 4, Y - 1, z - 12, x + 50, Y - 1, z + 50));
                run(server, String.format("fill %d %d %d %d %d %d minecraft:air", x - 4, Y, z - 12, x + 50, Y + 30, z + 50));
                run(server, String.format("place template hollowfrontier:crash_site %d %d %d", x, Y - 1, z));
                tpLooking(server, new Vec3(x + 10.5, Y + 14, z - 8), new Vec3(x + 10.5, Y + 2, z + 30));
            }
            case 88 -> run(server, "kill @e[type=minecraft:item,distance=..80]");
            case 90 -> shot(mc, "hf_i0_crash_site.png");
            case 92 -> tpLooking(server, new Vec3(x + 2.5, Y + 4, z + 24), new Vec3(x + 10.5, Y + 3, z + 36));
            case 130 -> shot(mc, "hf_i1_saucer_wreck.png");
            case 132 -> {
                run(server, "kill @e[type=hollowfrontier:gray_warden]");
                run(server, String.format("summon hollowfrontier:gray_warden %d.5 %d %d.5 {NoAI:1b,Rotation:[180f,0f]}", x + 10, Y, z + 15));
                tpLooking(server, new Vec3(x + 10.5, Y + 1.8, z + 11.5), new Vec3(x + 10.5, Y + 1.5, z + 15.5));
            }
            case 170 -> shot(mc, "hf_i2_warden.png");
            case 172 -> {
                run(server, "time set 18000");
                run(server, String.format("summon minecraft:cow %d.5 %d %d.5", x + 36, Y, z + 6));
                run(server, String.format("execute positioned %d %d %d run hollowfrontier ufo abduction", x + 36, Y, z + 6));
                tpLooking(server, new Vec3(x + 24.5, Y + 4, z - 4), new Vec3(x + 36.5, Y + 10, z + 6.5));
            }
            case 330 -> shot(mc, "hf_i3_abduction.png");
            case 332 -> creativeTab(mc, ModTabs.HOLLOW_FRONTIER.get());
            case 352 -> shot(mc, "hf_i4_tab_grays.png");
            case 355 -> {
                HollowFrontier.LOGGER.info("[PhotoBooth] grays done, quitting");
                quit(mc, server);
            }
            default -> {}
        }
    }

    // ---------------------------------------------------------------- phase 15c: the ruins restored

    private static final boolean RESTORED_ONLY = "restored".equals(System.getProperty("hollowfrontier.photo.scene"));

    /**
     * `hf_r0`-`hf_r3`: a Crystal Ruins on a grass stage as it's found, then with its three restoration stages laid over it
     * (from the same spot above the plaza), its waystone on the dais close up, and the waystone's list of where it goes.
     */
    private static void restored(Minecraft mc, MinecraftServer server, int t) {
        int x = baseX, z = baseZ;
        switch (t) {
            case 0 -> {
                run(server, "gamemode spectator @p");
                run(server, "time set 6000");
                run(server, "weather clear");
            }
            case 20 -> {
                run(server, String.format("fill %d %d %d %d %d %d minecraft:grass_block", x - 4, Y - 1, z - 16, x + 44, Y - 1, z + 44));
                run(server, String.format("fill %d %d %d %d %d %d minecraft:air", x - 4, Y, z - 16, x + 44, Y + 26, z + 44));
                run(server, "kill @e[type=hollowfrontier:crystal_guardian]");
                run(server, "kill @e[type=hollowfrontier:kindled]");
                run(server, String.format("place template hollowfrontier:crystal_ruins %d %d %d", x, Y - 1, z));
                tpLooking(server, new Vec3(x + 20.5, Y + 22, z - 14), new Vec3(x + 20.5, Y + 4, z + 21));
            }
            case 88 -> run(server, "kill @e[type=minecraft:item,distance=..80]");
            case 90 -> shot(mc, "hf_r0_ruin_as_found.png");
            case 92 -> {
                for (int stage = 1; stage <= ConcaviaBlueprints.STAGES; stage++) {
                    run(server, String.format("place template hollowfrontier:%s %d %d %d", ConcaviaBlueprints.stageName(stage), x, Y - 1, z));
                }
            }
            case 140 -> shot(mc, "hf_r1_ruin_restored.png");
            case 142 -> tpLooking(server, new Vec3(x + 23.5, Y + 2.2, z + 7.5), new Vec3(x + 20.5, Y + 1, z + 2.5));
            case 182 -> shot(mc, "hf_r2_waystone.png");
            case 184 -> {
                run(server, "gamemode creative @p");
                server.execute(() -> {
                    ServerLevel level = server.overworld();
                    BlockPos stone = new BlockPos(x + ConcaviaBlueprints.WAYSTONE.getX(), Y - 1 + ConcaviaBlueprints.WAYSTONE.getY(),
                            z + ConcaviaBlueprints.WAYSTONE.getZ());
                    Waystones network = Waystones.get(level);
                    Waystones.Stone here = network.wake(level.dimension(), stone, stone.south(2));
                    network.wake(level.dimension(), stone.offset(400, 0, 0), stone.offset(400, 0, 2));
                    network.wake(level.dimension(), stone.offset(-600, 0, 300), stone.offset(-600, 0, 302));
                    ServerPlayer player = server.getPlayerList().getPlayers().getFirst();
                    WaystoneBlock.open(player, network.fromStone(level, here), stone, here.label());
                });
            }
            case 214 -> shot(mc, "hf_r3_waystone_screen.png");
            case 216 -> mc.gui.setScreen(null);
            case 220 -> {
                HollowFrontier.LOGGER.info("[PhotoBooth] restored done, quitting");
                quit(mc, server);
            }
            default -> {}
        }
    }

    // ---------------------------------------------------------------- phase 21c: Grays in disguise

    private static final boolean DISGUISE_ONLY = "disguise".equals(System.getProperty("hollowfrontier.photo.scene"));
    private static com.hollowfrontier.content.ufo.DisguisedGray stranger;

    /** `hf_g0`-`hf_g2`: a stranger at the saloon; the same one as Mr. Gray on his stool; and found out, hat off. */
    private static void disguise(Minecraft mc, MinecraftServer server, int t) {
        int x = baseX, z = baseZ;
        switch (t) {
            case 0 -> {
                run(server, "gamemode spectator @p");
                run(server, "time set 6000");
                run(server, "weather clear");
            }
            case 20 -> {
                run(server, String.format("fill %d %d %d %d %d %d minecraft:grass_block", x - 8, Y - 1, z - 8, x + 24, Y - 1, z + 30));
                run(server, String.format("fill %d %d %d %d %d %d minecraft:air", x - 8, Y, z - 8, x + 24, Y + 14, z + 30));
                run(server, "kill @e[type=!minecraft:player,distance=..80]");
                run(server, String.format("setblock %d %d %d hollowfrontier:bar_stool", x + 7, Y, z + 5));
                server.execute(() -> stranger = com.hollowfrontier.content.ufo.GrayVisitors.visit(server.overworld(), 4242L, new BlockPos(x + 5, Y, z + 5)));
            }
            case 40 -> server.execute(() -> {
                if (stranger != null) {
                    stranger.setNoAi(true);
                    Vec3 s = stranger.position();
                    stranger.snapTo(s.x, s.y, s.z, 0.0F, 0.0F);
                    stranger.setYHeadRot(0.0F);
                    stranger.yBodyRot = 0.0F;
                    tpLooking(server, s.add(0.8, 1.5, 3.2), s.add(0, 1.3, 0));
                }
            });
            case 100 -> shot(mc, "hf_g0_stranger.png");
            case 102 -> server.execute(() -> {
                if (stranger != null) {
                    stranger.setNoAi(false);
                    stranger.becomeRegular();
                }
            });
            case 120 -> server.execute(() -> {
                if (stranger != null) {
                    Vec3 s = stranger.position();
                    tpLooking(server, s.add(0.5, 1.3, 3.2), s.add(0, 0.9, 0));
                }
            });
            case 170 -> shot(mc, "hf_g1_mr_gray.png");
            case 172 -> server.execute(() -> {
                if (stranger != null) {
                    stranger.expose(server.overworld(), null);
                    stranger.setNoAi(true);
                }
            });
            case 200 -> shot(mc, "hf_g2_exposed.png");
            case 204 -> {
                HollowFrontier.LOGGER.info("[PhotoBooth] disguise done, quitting");
                quit(mc, server);
            }
            default -> {}
        }
    }

    // ---------------------------------------------------------------- phase 21b: a night at the saloon

    private static final boolean TIPSY_ONLY = "tipsy".equals(System.getProperty("hollowfrontier.photo.scene"));

    /**
     * `hf_t0`-`hf_t1`: a sheriff's office with the player wanted, their poster's bad likeness on the board; then outside
     * at night at Tipsy IV, a barmaid's line slurred in chat and the moonshine's airship over them.
     */
    private static void tipsy(Minecraft mc, MinecraftServer server, int t) {
        int x = baseX, z = baseZ;
        switch (t) {
            case 0 -> {
                run(server, "gamemode survival @p");
                run(server, "time set 6000");
                run(server, "weather clear");
                run(server, "effect give @p minecraft:resistance infinite 255 true");
            }
            case 20 -> {
                run(server, String.format("fill %d %d %d %d %d %d minecraft:grass_block", x - 8, Y - 1, z - 8, x + 24, Y - 1, z + 24));
                run(server, String.format("fill %d %d %d %d %d %d minecraft:air", x - 8, Y, z - 8, x + 24, Y + 14, z + 24));
                run(server, String.format("kill @e[type=!minecraft:player,distance=..60]"));
                run(server, String.format("place template hollowfrontier:town/sheriff_office %d %d %d", x, Y - 1, z));
                server.execute(() -> {
                    ServerPlayer player = server.getPlayerList().getPlayers().getFirst();
                    com.hollowfrontier.content.frontier.law.Wanted.addHeat(player, 9);
                });
            }
            case 40 -> {
                run(server, "kill @e[type=hollowfrontier:lawman,distance=..60]");
                tpLooking(server, new Vec3(x + 4.5, Y + 1.0, z + 3.5), new Vec3(x + 7.5, Y + 1.8, z + 3.5));
            }
            case 120 -> shot(mc, "hf_t0_bad_likeness.png");
            case 122 -> {
                run(server, "time set 18000");
                tpLooking(server, new Vec3(x + 4.5, Y, z + 18.5), new Vec3(x + 4.5, Y + 30, z + 4.5));
                server.execute(() -> {
                    ServerPlayer player = server.getPlayerList().getPlayers().getFirst();
                    com.hollowfrontier.content.frontier.law.Wanted.clear(player);
                    player.removeAllEffects();
                    com.hollowfrontier.content.frontier.saloon.Drink.addTipsy(player, 3);
                });
            }
            case 130 -> {
                TipsyClient.phantom((float) (Math.PI / 2));
                server.execute(() -> {
                    ServerLevel level = server.overworld();
                    ServerPlayer player = server.getPlayerList().getPlayers().getFirst();
                    Barmaid barmaid = ModEntities.BARMAID.get().create(level, net.minecraft.world.entity.EntitySpawnReason.COMMAND);
                    barmaid.snapTo(player.getX() + 2, player.getY(), player.getZ());
                    level.addFreshEntity(barmaid);
                    com.hollowfrontier.content.frontier.talk.Chatter.talk(barmaid, player);
                    com.hollowfrontier.content.frontier.saloon.Drink.gamble(player, 7);
                });
            }
            case 210 -> shot(mc, "hf_t1_moonshine.png");
            case 214 -> {
                HollowFrontier.LOGGER.info("[PhotoBooth] tipsy done, quitting");
                quit(mc, server);
            }
            default -> {}
        }
    }

    // ---------------------------------------------------------------- phase 20: the captives

    private static final boolean PENS_ONLY = "pens".equals(System.getProperty("hollowfrontier.photo.scene"));

    /**
     * `hf_p0`-`hf_p2`: the fullest Gray pens on a grass stage, from above the door; the cell block from its corridor; and
     * the operating room with the procedure under way, its bar up.
     */
    private static void pens(Minecraft mc, MinecraftServer server, int t) {
        int x = baseX, z = baseZ;
        switch (t) {
            case 0 -> {
                run(server, "gamemode spectator @p");
                run(server, "time set 6000");
                run(server, "weather clear");
            }
            case 20 -> {
                run(server, String.format("fill %d %d %d %d %d %d minecraft:grass_block", x - 8, Y - 1, z - 8, x + 40, Y - 1, z + 40));
                run(server, String.format("fill %d %d %d %d %d %d minecraft:air", x - 8, Y, z - 8, x + 40, Y + 20, z + 40));
                run(server, String.format("place template hollowfrontier:%s %d %d %d", HollowBlueprints.pensTemplate(3), x, Y - 1, z));
                tpLooking(server, new Vec3(x + 6.5, Y + 16, z + 40), new Vec3(x + 16.5, Y + 2, z + 11));
            }
            case 90 -> shot(mc, "hf_p0_pens.png");
            case 92 -> tpLooking(server, new Vec3(x + 16.5, Y + 2.6, z + 11.5), new Vec3(x + 6.5, Y + 1.2, z + 4.5));
            case 132 -> shot(mc, "hf_p1_cells.png");
            case 134 -> {
                run(server, "gamemode creative @p");
                tpLooking(server, new Vec3(x + 20.5, Y + 2.8, z + 16.5), new Vec3(x + 24.5, Y + 1.2, z + 12.5));
                server.execute(() -> {
                    ServerLevel level = server.overworld();
                    BlockPos rig = new BlockPos(x + HollowBlueprints.RIG_X, Y - 1 + HollowBlueprints.RIG_Y, z + HollowBlueprints.RIG_Z);
                    if (level.getBlockEntity(rig) instanceof ImplantRigBlockEntity procedure) {
                        procedure.start(level, rig);
                        procedure.setProgress(ImplantRigBlockEntity.DURATION / 3);
                    }
                });
            }
            case 190 -> shot(mc, "hf_p2_procedure.png");
            case 194 -> {
                HollowFrontier.LOGGER.info("[PhotoBooth] pens done, quitting");
                quit(mc, server);
            }
            default -> {}
        }
    }

    // ---------------------------------------------------------------- phase 15a: Gray Landings

    private static final boolean LANDING_ONLY = "landing".equals(System.getProperty("hollowfrontier.photo.scene"));

    /**
     * `hf_n0`-`hf_n3`, at night on a grass stage: a Gray Landing coming down, set down with its Wardens, drones and cache,
     * the cache close up, and the scorched clearing once its keepers are down and the saucer has fled.
     */
    private static void landing(Minecraft mc, MinecraftServer server, int t) {
        int x = baseX, z = baseZ + 16;
        switch (t) {
            case 0 -> {
                run(server, "gamemode spectator @p");
                run(server, "time set 18000");
                run(server, "weather clear");
                run(server, "difficulty normal"); // peaceful would send its Wardens away
                run(server, "kill @e[type=hollowfrontier:gray_saucer]");
                run(server, "kill @e[type=hollowfrontier:gray_warden]");
                run(server, "kill @e[type=hollowfrontier:gray_drone]");
                // earlier scenes' props (boards hang as entities), then the stage, cleared in slabs under the fill limit
                run(server, String.format("execute positioned %d %d %d run kill @e[type=!minecraft:player,distance=..40]", x, Y, z));
                run(server, String.format("fill %d %d %d %d %d %d minecraft:grass_block", x - 24, Y - 1, z - 24, x + 24, Y - 1, z + 24));
                for (int y = Y; y <= Y + 45; y += 9) {
                    run(server, String.format("fill %d %d %d %d %d %d minecraft:air", x - 24, y, z - 24, x + 24, Math.min(y + 8, Y + 45), z + 24));
                }
                run(server, String.format("fill %d %d %d %d %d %d minecraft:short_grass replace minecraft:air", x - 24, Y, z - 24, x + 24, Y, z + 24));
            }
            case 5 -> tpLooking(server, new Vec3(x + 0.5, Y + 3, z - 20), new Vec3(x + 0.5, Y + 16, z + 0.5));
            case 10 -> server.execute(() -> com.hollowfrontier.content.ufo.GrayLandings.landAt(server.overworld(), new BlockPos(x, Y - 1, z),
                    com.hollowfrontier.content.ufo.GrayLandings.day(server.overworld())));
            case 70 -> shot(mc, "hf_n0_landing_descends.png");
            case 72 -> tpLooking(server, new Vec3(x + 12.5, Y + 6, z - 14), new Vec3(x + 0.5, Y + 1.5, z + 0.5));
            case 230 -> shot(mc, "hf_n1_landed.png");
            case 232 -> server.execute(() -> {
                var saucers = server.overworld().getEntitiesOfClass(com.hollowfrontier.content.ufo.GraySaucer.class,
                        new net.minecraft.world.phys.AABB(new BlockPos(x, Y, z)).inflate(16), s -> s.cache() != null);
                if (!saucers.isEmpty()) {
                    BlockPos cache = saucers.getFirst().cache();
                    tpLooking(server, Vec3.atCenterOf(cache).add(-3.5, 1.6, -3.5), Vec3.atCenterOf(cache));
                }
            });
            case 262 -> shot(mc, "hf_n2_cache.png");
            case 264 -> run(server, "kill @e[tag=" + com.hollowfrontier.content.ufo.GrayLandings.GUARD + "]");
            case 266 -> tpLooking(server, new Vec3(x + 0.5, Y + 16, z - 12), new Vec3(x + 0.5, Y, z + 0.5));
            case 300 -> run(server, "kill @e[type=minecraft:item,distance=..80]");
            case 330 -> shot(mc, "hf_n3_cleared.png");
            case 335 -> {
                HollowFrontier.LOGGER.info("[PhotoBooth] landing done, quitting");
                quit(mc, server);
            }
            default -> {}
        }
    }

    // ---------------------------------------------------------------- 29: dust devils

    private static final boolean DUST_DEVILS_ONLY = "dustdevils".equals(System.getProperty("hollowfrontier.photo.scene"));

    /**
     * `hf_d0`-`hf_d3`, by day on a sky stage: a dust devil over sand (also close up, and from above to show the turn),
     * the same over red sand (it takes the ground's colour), an Ash Devil over ash, and a dust devil carrying hens and
     * things dropped at its foot. Each held in place.
     */
    private static void dustDevils(Minecraft mc, MinecraftServer server, int t) {
        int x = baseX, z = baseZ + 16;
        switch (t) {
            case 0 -> {
                run(server, "gamemode spectator @p");
                run(server, "time set 6000");
                run(server, "weather clear");
                run(server, "gamerule fire_spread_radius_around_player 0"); // the Ash Devil sets nothing alight up here
                run(server, String.format("execute positioned %d %d %d run kill @e[type=!minecraft:player,distance=..40]", x, Y, z));
                for (int y = Y; y <= Y + 18; y += 9) {
                    run(server, String.format("fill %d %d %d %d %d %d minecraft:air", x - 24, y, z - 24, x + 24, Math.min(y + 8, Y + 18), z + 24));
                }
                devilFloor(server, x, z, "minecraft:sand");
            }
            case 5 -> tpLooking(server, new Vec3(x + 0.5, Y + 4, z - 16), new Vec3(x + 0.5, Y + 4.5, z + 0.5));
            case 10 -> server.execute(() -> devil(server, x, z, false));
            case 100 -> shot(mc, "hf_d0_dust_devil_sand.png");
            case 102 -> tpLooking(server, new Vec3(x + 0.5, Y + 2.5, z - 7), new Vec3(x + 0.5, Y + 5, z + 0.5));
            case 115 -> shot(mc, "hf_d0b_dust_devil_close.png");
            // high and to one side, not straight down: that view stalled the client compiling a translucent pipeline
            case 117 -> tpLooking(server, new Vec3(x + 6.5, Y + 12, z - 5.5), new Vec3(x + 0.5, Y + 3, z + 0.5));
            case 130 -> shot(mc, "hf_d0c_dust_devil_from_above.png");
            case 132 -> {
                tpLooking(server, new Vec3(x + 0.5, Y + 4, z - 16), new Vec3(x + 0.5, Y + 4.5, z + 0.5));
                devilFloor(server, x, z, "minecraft:red_sand");
            }
            case 160 -> shot(mc, "hf_d1_dust_devil_red_sand.png");
            case 162 -> {
                run(server, "kill @e[type=hollowfrontier:dust_devil]");
                devilFloor(server, x, z, "hollowfrontier:ash");
                server.execute(() -> devil(server, x, z, true));
            }
            case 250 -> shot(mc, "hf_d2_ash_devil.png");
            case 252 -> {
                run(server, "kill @e[type=hollowfrontier:ash_devil]");
                devilFloor(server, x, z, "minecraft:sand");
                server.execute(() -> devil(server, x, z, false));
            }
            case 320 -> {
                for (int i = 0; i < 3; i++) {
                    run(server, "summon minecraft:chicken " + (x + i * 0.6 - 0.1) + " " + Y + " " + (z + 1.7));
                }
                for (String item : List.of("minecraft:stick", "minecraft:dead_bush", "minecraft:feather", "hollowfrontier:copper_coin")) {
                    run(server, "summon minecraft:item " + (x + 1.9) + " " + Y + " " + (z + 0.5) + " {Item:{id:\"" + item + "\",count:1}}");
                }
            }
            case 330 -> tpLooking(server, new Vec3(x + 0.5, Y + 3.5, z - 10), new Vec3(x + 0.5, Y + 4.5, z + 0.5));
            case 350 -> shot(mc, "hf_d3_dust_devil_carrying.png");
            case 355 -> {
                run(server, "kill @e[type=hollowfrontier:dust_devil]");
                HollowFrontier.LOGGER.info("[PhotoBooth] dust devils done, quitting");
                quit(mc, server);
            }
            default -> {}
        }
    }

    private static void devilFloor(MinecraftServer server, int x, int z, String block) {
        run(server, String.format("fill %d %d %d %d %d %d %s", x - 24, Y - 1, z - 24, x + 24, Y - 1, z + 24, block));
        run(server, String.format("fill %d %d %d %d %d %d minecraft:sandstone", x - 24, Y - 2, z - 24, x + 24, Y - 2, z + 24));
    }

    private static void devil(MinecraftServer server, int x, int z, boolean ash) {
        com.hollowfrontier.content.frontier.weather.DustDevil.spawn(server.overworld(),
                ash ? com.hollowfrontier.registry.ModEntities.ASH_DEVIL.get() : com.hollowfrontier.registry.ModEntities.DUST_DEVIL.get(),
                new Vec3(x + 0.5, Y, z + 0.5), com.hollowfrontier.content.frontier.weather.DustDevil.MAX_LIFE).fixed();
    }

    // ---------------------------------------------------------------- 17 and 18: the card table and the bounty board

    private static final boolean SALOON_ONLY = "saloon".equals(System.getProperty("hollowfrontier.photo.scene"));

    /**
     * `hf_m0`-`hf_m2`: a saloon placed on a grass stage and its card table, then a hand of blackjack dealt by the saloon's
     * barmaid (the hole card face down), and the same hand stood and settled.
     */
    private static void saloon(Minecraft mc, MinecraftServer server, int t) {
        int x = baseX, z = baseZ;
        BlockPos table = new BlockPos(x + 9, Y, z + 5);
        switch (t) {
            case 0 -> {
                if (mc.player != null && mc.player.isDeadOrDying()) {
                    mc.player.respawn(); // the photo world keeps the player as they were left
                }
                run(server, "gamemode survival @p");
                run(server, "effect give @p minecraft:instant_health 1 10 true");
                run(server, "time set 6000");
                run(server, "weather clear");
                run(server, "kill @e[type=hollowfrontier:barmaid]");
            }
            // Back to the stage (a respawn lands at the world spawn), so its chunks are loaded before anything's built there
            case 10 -> {
                run(server, "effect give @p minecraft:slow_falling 5 0 true");
                run(server, String.format(java.util.Locale.ROOT, "tp @p %d.5 %d %d.5", x + 7, Y + 2, z + 4));
            }
            case 20 -> {
                run(server, String.format("fill %d %d %d %d %d %d minecraft:grass_block", x - 4, Y - 1, z - 8, x + 20, Y - 1, z + 20));
                run(server, String.format("fill %d %d %d %d %d %d minecraft:air", x - 4, Y, z - 8, x + 20, Y + 12, z + 20));
                run(server, String.format("place template hollowfrontier:saloon %d %d %d", x, Y - 1, z));
                run(server, "kill @e[type=hollowfrontier:barmaid]");
                run(server, String.format("summon hollowfrontier:barmaid %d.5 %d %d.5 {NoAI:1b,Rotation:[0f,0f],hostess:\"josie\"}", x + 5, Y, z + 10));
                run(server, "clear @p"); // nothing in hand to block the view
                run(server, "give @p hollowfrontier:gold_coin 4");
                tpLooking(server, new Vec3(x + 7.5, Y, z + 4.3), new Vec3(x + 9.5, Y + 0.8 - 1.62, z + 5.5)); // feet inside the door; aimed from the eyes
            }
            case 70 -> shot(mc, "hf_m0_card_table.png");
            case 72 -> server.execute(() -> {
                ServerPlayer player = server.getPlayerList().getPlayers().getFirst();
                BlockHitResult hit = new BlockHitResult(Vec3.atCenterOf(table).relative(Direction.UP, 0.5), Direction.UP, table, false);
                server.overworld().getBlockState(table).useWithoutItem(server.overworld(), player, hit);
                if (player.containerMenu instanceof com.hollowfrontier.content.frontier.saloon.BlackjackMenu menu) {
                    menu.clickMenuButton(player, com.hollowfrontier.content.frontier.saloon.BlackjackMenu.DEAL);
                    menu.broadcastChanges();
                }
            });
            case 100 -> shot(mc, "hf_m1_blackjack_dealt.png");
            case 102 -> server.execute(() -> {
                ServerPlayer player = server.getPlayerList().getPlayers().getFirst();
                if (player.containerMenu instanceof com.hollowfrontier.content.frontier.saloon.BlackjackMenu menu) {
                    menu.clickMenuButton(player, com.hollowfrontier.content.frontier.saloon.BlackjackMenu.STAND);
                    menu.broadcastChanges();
                }
            });
            case 130 -> shot(mc, "hf_m2_blackjack_settled.png");
            // The board the saloon itself hangs, on its east wall (two across)
            case 132 -> server.execute(() -> {
                ServerPlayer player = server.getPlayerList().getPlayers().getFirst();
                player.closeContainer();
                player.setData(com.hollowfrontier.registry.ModAttachments.WANTED.get(), com.hollowfrontier.content.frontier.law.Wanted.NONE); // left from an earlier run
                tpLooking(server, new Vec3(x + 8.5, Y, z + 4.4), new Vec3(x + 12.0, Y + 1.0 - 1.62, z + 5.0)); // feet; aimed from the eyes
            });
            case 180 -> shot(mc, "hf_m3_bounty_board_saloon.png");
            // Out on the grass, three across on a stone wall, face to face with a survival player (who can be wanted)
            case 182 -> run(server, String.format("fill %d %d %d %d %d %d minecraft:stone", x + 15, Y, z + 19, x + 17, Y + 2, z + 19));
            case 184 -> server.execute(() -> { // the wall's up now (commands run queued)
                var level = server.overworld();
                com.hollowfrontier.content.frontier.bounty.BountyBoardEntity.create(level, new BlockPos(x + 16, Y + 1, z + 18), Direction.NORTH)
                        .ifPresent(level::addFreshEntity);
                tpLooking(server, new Vec3(x + 16.5, Y, z + 16.2), new Vec3(x + 16.5, Y + 2.0 - 1.62, z + 19.0));
            });
            case 188 -> server.execute(() -> stageBoard(server, new BlockPos(x + 16, Y + 1, z + 18), true));
            case 230 -> shot(mc, "hf_m4_bounty_board_raider.png");
            case 232 -> server.execute(() -> {
                ServerPlayer player = server.getPlayerList().getPlayers().getFirst();
                com.hollowfrontier.content.frontier.law.Wanted.addHeat(player, 5);
                stageBoard(server, new BlockPos(x + 16, Y + 1, z + 18), false);
            });
            case 280 -> shot(mc, "hf_m5_bounty_board_wanted.png");
            case 285 -> {
                HollowFrontier.LOGGER.info("[PhotoBooth] saloon done, quitting");
                quit(mc, server);
            }
            default -> {}
        }
    }

    // A hanging board at `at` with a known set of posters: a raid leader, then one taken, one stamped dead and one open (or,
    // without the raider, three outlaws and room for the wanted player standing by)
    private static void stageBoard(MinecraftServer server, BlockPos at, boolean raider) {
        var level = server.overworld();
        var found = level.getEntitiesOfClass(com.hollowfrontier.content.frontier.bounty.BountyBoardEntity.class, new AABB(at).inflate(1));
        if (found.isEmpty()) {
            return;
        }
        var board = found.getFirst();
        board.sync(level);
        var boards = com.hollowfrontier.content.frontier.bounty.BountyBoards.get(level);
        long day = com.hollowfrontier.content.frontier.bounty.BountyBoards.day(level);
        var random = net.minecraft.util.RandomSource.create(17);
        java.util.function.Function<String, com.hollowfrontier.content.frontier.bounty.BountyData> bounty = name -> {
            var b = com.hollowfrontier.content.frontier.bounty.BountyBoards.newBounty(at, random);
            return new com.hollowfrontier.content.frontier.bounty.BountyData(b.id(), name, b.x(), b.z(), b.reward(), false);
        };
        java.util.List<com.hollowfrontier.content.frontier.bounty.BoardPosting> postings = new java.util.ArrayList<>();
        if (raider) {
            postings.add(com.hollowfrontier.content.frontier.bounty.BoardPosting.open(bounty.apply("Black Bart Calloway"), true, day));
        }
        postings.add(com.hollowfrontier.content.frontier.bounty.BoardPosting.open(bounty.apply("Rattlesnake Ricky"), false, day).taken("Wyatt"));
        postings.add(com.hollowfrontier.content.frontier.bounty.BoardPosting.open(bounty.apply("Two-Gun Tess"), false, day).dead());
        postings.add(com.hollowfrontier.content.frontier.bounty.BoardPosting.open(bounty.apply("Doc Hollister"), false, day));
        boards.put(board.key(level), postings, day);
        board.sync(level);
    }

    // ---------------------------------------------------------------- phase 12b: the Sun Archon

    private static final boolean ARCHON_ONLY = "archon".equals(System.getProperty("hollowfrontier.photo.scene"));

    /**
     * `hf_h0`-`hf_h5`: the Sun Temple placed on a grass stage, the Archon on its throne, woken by a survival player (kept
     * alive by Resistance V), its health set to two thirds (the pylons shield it aloft), then the pylons broken and its
     * health at a third (the eclipse), and the Concavia tab with the 12b items.
     */
    private static void archon(Minecraft mc, MinecraftServer server, int t) {
        int x = baseX, z = baseZ, arena = Y + 8;
        switch (t) {
            case 0 -> {
                run(server, "gamemode spectator @p");
                run(server, "time set 6000");
                run(server, "weather clear");
                run(server, "kill @e[type=hollowfrontier:sun_archon]");
            }
            case 20 -> {
                run(server, String.format("fill %d %d %d %d %d %d minecraft:grass_block", x - 4, Y - 1, z - 20, x + 44, Y - 1, z + 62));
                run(server, String.format("fill %d %d %d %d %d %d minecraft:air", x - 4, Y, z - 20, x + 44, Y + 32, z + 62));
                run(server, String.format("place template hollowfrontier:sun_temple %d %d %d", x, Y - 1, z));
                tpLooking(server, new Vec3(x + 20.5, Y + 22, z - 16), new Vec3(x + 20.5, Y + 6, z + 22));
            }
            case 88 -> run(server, "kill @e[type=minecraft:item,distance=..80]");
            case 90 -> shot(mc, "hf_h0_sun_temple.png");
            case 92 -> tpLooking(server, new Vec3(x + 20.5, arena + 1.5, z + 36.5), new Vec3(x + 20.5, arena + 3, z + 48.5));
            case 130 -> shot(mc, "hf_h1_archon_on_throne.png");
            case 132 -> {
                run(server, "gamemode survival @p");
                run(server, "effect give @p minecraft:resistance 120 4 true");
                run(server, "effect give @p minecraft:fire_resistance 120 0 true");
                tpLooking(server, new Vec3(x + 20.5, arena, z + 41.5), new Vec3(x + 20.5, arena + 2, z + 48.5));
            }
            case 175 -> shot(mc, "hf_h2_archon_wakes.png");
            case 177 -> {
                run(server, "data merge entity @e[type=hollowfrontier:sun_archon,limit=1] {Health:180f}");
                tpLooking(server, new Vec3(x + 20.5, arena, z + 31.5), new Vec3(x + 20.5, arena + 6, z + 38.5));
            }
            case 240 -> shot(mc, "hf_h3_corona.png");
            case 242 -> {
                for (int[] p : com.hollowfrontier.worldgen.structure.ConcaviaBlueprints.PYLONS) {
                    run(server, String.format("setblock %d %d %d minecraft:air", x + p[0], arena + 2, z + p[1])); // the template sits a block down
                }
            }
            case 262 -> run(server, "data merge entity @e[type=hollowfrontier:sun_archon,limit=1] {Health:90f}");
            case 320 -> shot(mc, "hf_h4_eclipse.png");
            case 322 -> {
                run(server, "gamemode creative @p");
                run(server, "kill @e[type=hollowfrontier:sun_archon]");
                creativeTab(mc, ModTabs.HOLLOW_FRONTIER.get());
            }
            case 342 -> shot(mc, "hf_h5_tab_concavia.png");
            case 345 -> {
                HollowFrontier.LOGGER.info("[PhotoBooth] archon done, quitting");
                quit(mc, server);
            }
            default -> {}
        }
    }

    // ---------------------------------------------------------------- phase 12a: the Kindled

    private static final boolean CRYSTAL_ONLY = "crystal".equals(System.getProperty("hollowfrontier.photo.scene"));

    /**
     * `hf_g0`-`hf_g4`: the crystal ruins placed on a grass stage (from above the plaza), the sanctum and its Kindled, a
     * guardian at the door, the plaza's guardian drawing its beam on a survival player carrying a Gray fragment, and the
     * Concavia creative tab.
     */
    private static void crystal(Minecraft mc, MinecraftServer server, int t) {
        int x = baseX, z = baseZ;
        switch (t) {
            case 0 -> {
                run(server, "gamemode spectator @p"); // a camera that doesn't fall
                run(server, "time set 6000");
                run(server, "weather clear");
            }
            case 20 -> {
                run(server, String.format("fill %d %d %d %d %d %d minecraft:grass_block", x - 4, Y - 1, z - 16, x + 44, Y - 1, z + 44));
                run(server, String.format("fill %d %d %d %d %d %d minecraft:air", x - 4, Y, z - 16, x + 44, Y + 26, z + 44));
                run(server, "kill @e[type=hollowfrontier:crystal_guardian]");
                run(server, "kill @e[type=hollowfrontier:kindled]");
                run(server, String.format("place template hollowfrontier:crystal_ruins %d %d %d", x, Y - 1, z));
                tpLooking(server, new Vec3(x + 20.5, Y + 17, z - 12), new Vec3(x + 20.5, Y + 2, z + 21));
            }
            case 88 -> run(server, "kill @e[type=minecraft:item,distance=..80]"); // drops from the last scenes' trees, decaying
            case 90 -> shot(mc, "hf_g0_crystal_ruins.png");
            case 92 -> tpLooking(server, new Vec3(x + 20.5, Y + 1.8, z + 17.5), new Vec3(x + 20.5, Y + 1.4, z + 25.5));
            case 140 -> shot(mc, "hf_g1_sanctum.png");
            case 142 -> {
                server.overworld().getEntities(ModEntities.CRYSTAL_GUARDIAN.get(), g -> true).forEach(g -> HollowFrontier.LOGGER.info(
                        "[PhotoBooth] guardian at {} {} {}", g.getBlockX() - x, g.getBlockY() - Y, g.getBlockZ() - z));
                tpLooking(server, new Vec3(x + 12.5, Y + 2.0, z + 11.5), new Vec3(x + 17.5, Y + 1.6, z + 15.5));
            }
            case 180 -> shot(mc, "hf_g2_guardian.png");
            case 182 -> {
                tpLooking(server, new Vec3(x + 10.5, Y, z + 18.5), new Vec3(x + 7.5, Y + 2.2, z + 21.5));
                run(server, "gamemode survival @p");
                run(server, "give @p hollowfrontier:gray_tech_fragment");
            }
            case 222 -> shot(mc, "hf_g3_guardian_beam.png");
            case 224 -> {
                run(server, "gamemode creative @p");
                run(server, "clear @p hollowfrontier:gray_tech_fragment");
                creativeTab(mc, ModTabs.HOLLOW_FRONTIER.get());
            }
            case 244 -> shot(mc, "hf_g4_tab_concavia.png");
            case 247 -> {
                HollowFrontier.LOGGER.info("[PhotoBooth] crystal done, quitting");
                quit(mc, server);
            }
            default -> {}
        }
    }

    // ---------------------------------------------------------------- town checks: the depot's back, stairs, wheat fields

    private static final boolean TOWNCHECK_ONLY = "towncheck".equals(System.getProperty("hollowfrontier.photo.scene"));
    private static final int CHECK_TRIES = 8;
    private static final int CHECK_TRY_TICKS = 340;
    /** The four buildings with stairs to an upper floor, their widths and depths (the stairs run up inside the east wall). */
    private static final String[] STAIR_HOUSES = {"hotel", "hotel_grand", "saloon_grand", "courthouse"};
    private static final int[][] STAIR_SIZES = {{11, 12}, {13, 13}, {15, 14}, {17, 16}};
    private static volatile int checkFrom = -1;
    private static volatile int checkTry;
    private static net.minecraft.world.level.levelgen.structure.PoolElementStructurePiece checkDepot;
    private static net.minecraft.world.level.levelgen.structure.PoolElementStructurePiece checkField;

    /**
     * `hf_t0`-`hf_t9`, the town pieces playtesting found trouble with. Real towns (generated, terrain and all) are tried
     * until one has a depot: its back door from the track, the back from behind and above, and the way out from inside
     * with the back door open; then one of its wheat fields from above, before and after a crop circle pressed from its
     * gate. Last, the four staircases on a stage in the sky, each building's east wall taken off.
     */
    private static void townCheck(Minecraft mc, MinecraftServer server, int t) {
        if (t == 0) {
            run(server, "gamemode spectator @p");
            run(server, "time set 6000");
            run(server, "weather clear");
        }
        if (checkFrom < 0) {
            int step = t % CHECK_TRY_TICKS;
            if (step == 5) {
                findCheckTown(server, checkTry);
            } else if (step == 320) {
                server.execute(() -> {
                    pickCheckPieces(server, checkTry);
                    if (checkDepot != null || checkTry + 1 >= CHECK_TRIES) {
                        checkFrom = t + 5;
                    } else {
                        checkTry++;
                    }
                });
            }
            return;
        }
        int s = t - checkFrom;
        var depot = checkDepot;
        var field = checkField;
        switch (s) {
            case 0 -> { if (depot != null) tpLooking(server, local(depot, 12.5, 2.62, 14.5), local(depot, 12.5, 2.0, 8.5)); }
            case 40 -> { if (depot != null) shot(mc, "hf_t0_depot_back_door.png"); }
            case 42 -> { if (depot != null) tpLooking(server, local(depot, 12.5, 6.0, 27.5), local(depot, 12.5, 1.0, 9.5)); }
            case 80 -> { if (depot != null) shot(mc, "hf_t1_depot_from_behind.png"); }
            case 82 -> { if (depot != null) tpLooking(server, local(depot, -8.0, 16.0, 30.0), local(depot, 12.5, 1.0, 10.0)); }
            case 120 -> { if (depot != null) shot(mc, "hf_t2_depot_above.png"); }
            case 122 -> {
                if (depot != null) {
                    openDepotDoor(server, depot);
                    tpLooking(server, local(depot, 12.5, 2.62, 3.5), local(depot, 12.5, 2.2, 13.5));
                }
            }
            case 155 -> { if (depot != null) openDepotDoor(server, depot); } // the station agent may have shut it
            case 160 -> { if (depot != null) shot(mc, "hf_t3_depot_way_out.png"); }
            case 162 -> { if (field != null) tpLooking(server, local(field, 9.5, 22.0, -7.0), local(field, 9.5, 0.0, 11.5)); }
            case 200 -> { if (field != null) shot(mc, "hf_t4_wheat_field.png"); }
            case 202 -> {
                if (field != null) {
                    Vec3 gate = local(field, 9.5, 1.0, 0.5);
                    run(server, String.format(java.util.Locale.ROOT, "execute positioned %.1f %.1f %.1f run hollowfrontier mystery circle", gate.x, gate.y, gate.z));
                }
            }
            case 240 -> { if (field != null) shot(mc, "hf_t5_wheat_field_circle.png"); }
            case 242 -> { // back to the stage, loaded before anything's built on it (commands fail on unloaded chunks)
                run(server, String.format("forceload add %d %d %d %d", baseX - 8, baseZ - 8, baseX + 26 * STAIR_HOUSES.length, baseZ + 26));
                run(server, String.format("tp @p %d %d %d", baseX + 40, Y + 20, baseZ - 20));
            }
            case 262 -> {
                int x = baseX, z = baseZ;
                run(server, "kill @e[type=!player,distance=..120]");
                for (int i = 0; i < STAIR_HOUSES.length; i++) {
                    int bx = x + 26 * i, w = STAIR_SIZES[i][0], d = STAIR_SIZES[i][1];
                    // a plot each, cleared in slabs under the fill limit
                    run(server, String.format("fill %d %d %d %d %d %d minecraft:grass_block", bx - 2, Y - 1, z - 6, bx + 23, Y - 1, z + 24));
                    run(server, String.format("fill %d %d %d %d %d %d minecraft:air", bx - 2, Y, z - 6, bx + 23, Y + 24, z + 24));
                    run(server, String.format("place template hollowfrontier:town/%s %d %d %d", STAIR_HOUSES[i], bx, Y, z));
                    run(server, String.format("fill %d %d %d %d %d %d minecraft:air", bx + w - 1, Y + 1, z + 3, bx + w - 1, Y + 12, z + d - 1));
                }
            }
            default -> {
                int i = (s - 270) / 40, k = (s - 270) % 40;
                if (s >= 270 && i < STAIR_HOUSES.length) {
                    int bx = baseX + 26 * i, w = STAIR_SIZES[i][0], z = baseZ;
                    if (k == 0) {
                        tpLooking(server, new Vec3(bx + w + 7.5, Y + 6.5, z + 8.5), new Vec3(bx + w - 2.5, Y + 3.5, z + 7.5));
                    } else if (k == 35) {
                        shot(mc, "hf_t" + (6 + i) + "_stairs_" + STAIR_HOUSES[i] + ".png");
                    }
                } else if (s == 270 + 40 * STAIR_HOUSES.length) {
                    run(server, "kill @e[type=!player,distance=..120]");
                    HollowFrontier.LOGGER.info("[PhotoBooth] towncheck done, quitting");
                    quit(mc, server);
                }
            }
        }
    }

    private static void openDepotDoor(MinecraftServer server, net.minecraft.world.level.levelgen.structure.PoolElementStructurePiece depot) {
        server.execute(() -> {
            ServerLevel level = server.overworld();
            BlockPos door = BlockPos.containing(local(depot, 12.5, 1.5, 8.5));
            var state = level.getBlockState(door);
            if (state.getBlock() instanceof net.minecraft.world.level.block.DoorBlock block) {
                block.setOpen(null, level, state, door, true);
            }
        });
    }

    // A town a long way from the stage, a different one (and style) each try, and fresh ground each run (the saved world
    // keeps the towns earlier runs generated, as they were then).
    private static void findCheckTown(MinecraftServer server, int attempt) {
        server.execute(() -> {
            ServerLevel level = server.overworld();
            TownStyle style = TownStyle.values()[attempt % TownStyle.values().length];
            var town = level.registryAccess().lookupOrThrow(Registries.STRUCTURE).getOrThrow(style.structure());
            int fresh = (int) (System.currentTimeMillis() / 1000 % 97);
            BlockPos from = new BlockPos(baseX + 9000 + attempt * 2600, 0, baseZ - 7000 - fresh * 4000 + attempt * 1700);
            var found = level.getChunkSource().getGenerator().findNearestMapStructure(level, net.minecraft.core.HolderSet.direct(town), from, 200, false);
            if (found == null) {
                HollowFrontier.LOGGER.warn("[PhotoBooth] towncheck: no {} town near {}", style, from);
                return;
            }
            ChunkPos chunk = ChunkPos.containing(found.getFirst());
            TOWN_CHUNKS.put(style, chunk);
            int x = chunk.getMinBlockX(), z = chunk.getMinBlockZ();
            run(server, String.format("forceload add %d %d %d %d", x - 112, z - 112, x + 112, z + 112));
            run(server, String.format("tp @p %d 230 %d 0 90", x, z));
            HollowFrontier.LOGGER.info("[PhotoBooth] towncheck try {}: {} town at chunk {}", attempt, style, chunk);
        });
    }

    // The tried town's depot and a wheat field (null where it has none).
    private static void pickCheckPieces(MinecraftServer server, int attempt) {
        TownStyle style = TownStyle.values()[attempt % TownStyle.values().length];
        ChunkPos chunk = TOWN_CHUNKS.get(style);
        checkDepot = null;
        checkField = null;
        if (chunk == null) {
            return;
        }
        ServerLevel level = server.overworld();
        var town = level.registryAccess().lookupOrThrow(Registries.STRUCTURE).getOrThrow(style.structure()).value();
        StructureStart start = level.getChunk(chunk.x(), chunk.z()).getStartForStructure(town);
        if (start == null || !start.isValid()) {
            HollowFrontier.LOGGER.warn("[PhotoBooth] towncheck: the {} town at {} didn't generate", style, chunk);
            return;
        }
        for (var piece : start.getPieces()) {
            if (piece instanceof net.minecraft.world.level.levelgen.structure.PoolElementStructurePiece pool) {
                String name = pool.getElement().toString();
                if (checkDepot == null && name.contains("/depot")) {
                    checkDepot = pool;
                } else if (checkField == null && name.contains("/wheat_field")) {
                    checkField = pool;
                }
            }
        }
        HollowFrontier.LOGGER.info("[PhotoBooth] towncheck: {} pieces, depot {}, wheat field {}", start.getPieces().size(),
                checkDepot == null ? "none" : checkDepot.getPosition() + " " + checkDepot.getRotation(),
                checkField == null ? "none" : checkField.getPosition() + " " + checkField.getRotation());
    }

    // A point given in a jigsaw piece's own template coordinates, in the world.
    private static Vec3 local(net.minecraft.world.level.levelgen.structure.PoolElementStructurePiece piece, double x, double y, double z) {
        BlockPos block = net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplate.transform(
                BlockPos.containing(x, y, z), net.minecraft.world.level.block.Mirror.NONE, piece.getRotation(), BlockPos.ZERO).offset(piece.getPosition());
        // keep the fraction within the block, turned the same way
        Vec3 in = net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplate.transform(
                new Vec3(x - Math.floor(x) - 0.5, 0, z - Math.floor(z) - 0.5), net.minecraft.world.level.block.Mirror.NONE, piece.getRotation(), BlockPos.ZERO);
        return new Vec3(block.getX() + 0.5 + in.x, block.getY() + (y - Math.floor(y)), block.getZ() + 0.5 + in.z);
    }

    // ---------------------------------------------------------------- the town's buildings (23)

    private static final boolean TOWNBUILD_ONLY = "townbuild".equals(System.getProperty("hollowfrontier.photo.scene"));
    /** A building to shoot: its template, width and depth, and the heights (template y, highest first) to cut it down to. */
    private record Plot(String template, int w, int d, int... cuts) {}
    private static final List<Plot> PLOTS = List.of(
            new Plot("town/general_store", 12, 12, 5), new Plot("town/sheriff_office", 9, 12, 5), new Plot("town/bank", 12, 12, 6),
            new Plot("town/hotel", 11, 12, 10, 5), new Plot("town/hotel_grand", 13, 13, 9, 5), new Plot("town/saloon", 13, 13, 6),
            new Plot("town/saloon_grand", 15, 14, 10, 5), new Plot("town/gunsmith", 12, 12, 5), new Plot("town/doctor", 12, 12, 5),
            new Plot("town/post_office", 12, 12, 6), new Plot("town/blacksmith", 12, 10, 5), new Plot("town/livery", 12, 12, 6),
            new Plot("town/undertaker", 12, 10, 5), new Plot("town/house", 12, 12, 5), new Plot("town/adobe", 12, 12, 5),
            new Plot("town/homestead", 12, 12, 9, 5), new Plot("town/schoolhouse", 9, 15, 6), new Plot("town/desert/gunsmith", 12, 12),
            new Plot("town/mining/homestead", 12, 12), new Plot("town/desert/general_store", 12, 12), new Plot("town/desert/courthouse", 17, 16),
            new Plot("town/bathhouse", 12, 12, 5), new Plot("town/gazette_office", 12, 12, 6), new Plot("town/ranch", 21, 21, 5),
            new Plot("town/street_short", 9, 44), new Plot("town/rail_boxcar", 7, 28), new Plot("town/rail_tank", 7, 28),
            new Plot("town/square_tee_bandstand", 39, 39));
    private static final int PLOT_TICKS = 100;

    /**
     * `hf_w*`: each town building placed alone on a grass stage in the sky (cleared between them), shot from the street
     * at an angle, then from above with everything over each cut height taken away, so its rooms show floor by floor;
     * last the square from above. Then a real town of each style, as the full run shoots them (`hf_17`-`hf_20`).
     */
    private static void townBuild(Minecraft mc, MinecraftServer server, int t) {
        int x = baseX, z = baseZ, oy = Y - 1;
        if (t == 0) {
            run(server, "gamemode spectator @p");
            run(server, "time set 6000");
            run(server, "weather clear");
            mc.options.cloudStatus().set(net.minecraft.client.CloudStatus.OFF);
            run(server, String.format("forceload add %d %d %d %d", x - 24, z - 24, x + 64, z + 64));
            run(server, String.format("tp @p %d %d %d", x, Y + 30, z - 20));
            return;
        }
        int plots = PLOTS.size();
        int s = t - 20;
        if (s >= 0 && s < plots * PLOT_TICKS) {
            Plot p = PLOTS.get(s / PLOT_TICKS);
            int k = s % PLOT_TICKS, n = s / PLOT_TICKS;
            String tag = String.format("hf_w%02d_%s", n, p.template().substring("town/".length()).replace('/', '_'));
            double cx = x + p.w() / 2.0, cz = z + p.d() / 2.0;
            if (k == 0) {
                run(server, "kill @e[type=!player,distance=..120]");
                for (int dy = 0; dy < 32; dy += 8) { // cleared in slabs under the fill limit
                    run(server, String.format("fill %d %d %d %d %d %d minecraft:air", x - 6, Y + dy, z - 12, x + 44, Y + dy + 7, z + 44));
                }
                run(server, String.format("fill %d %d %d %d %d %d minecraft:grass_block", x - 6, oy, z - 12, x + 44, oy, z + 44));
                run(server, String.format("place template hollowfrontier:%s %d %d %d", p.template(), x, oy, z));
            } else if (k == 6) {
                double far = Math.max(p.w(), p.d()) * 0.5 + 5;
                tpLooking(server, new Vec3(x - far * 0.35, Y + far * 0.4, z - far), new Vec3(cx, Y + 2, cz));
            } else if (k == 30) {
                shot(mc, tag + "_out.png");
            } else {
                for (int c = 0; c < p.cuts().length; c++) {
                    int at = 32 + c * 30;
                    if (k == at) {
                        int cutY = oy + p.cuts()[c];
                        run(server, String.format("kill @e[type=minecraft:item,distance=..80]"));
                        for (int dy = 0; dy < 24; dy += 8) {
                            run(server, String.format("fill %d %d %d %d %d %d minecraft:air", x - 1, cutY + dy, z - 1, x + p.w(), cutY + dy + 7, z + p.d()));
                        }
                        tpLooking(server, new Vec3(cx, cutY + Math.max(p.w(), p.d()) * 0.55 + 3, cz - 3), new Vec3(cx, oy, cz + 0.5));
                    } else if (k == at + 20) {
                        run(server, "kill @e[type=minecraft:item,distance=..80]"); // what fell when the floor above went
                    } else if (k == at + 26) {
                        shot(mc, tag + "_in" + (c + 1) + ".png");
                    }
                }
            }
            return;
        }
        if (s >= plots * PLOT_TICKS) {
            townTour(mc, server);
        }
    }

    // The real towns, paced by what's happened rather than by the clock: generating a town can stall the server for many
    // seconds while the client ticks on, so each step waits for the last (the town found, the camera moved) before the
    // next, with a cap so a lost step can't hang the run.
    private static int tourStep;
    private static int tourWait;
    private static boolean tourIssued;
    private static Vec3 tourFrom = Vec3.ZERO;
    private static final String[] TOUR_SHOTS = {"hf_17_%s_aerial.png", "hf_18_%s_overview.png", "hf_19_%s_square.png", "hf_20_%s_street.png"};

    private static void townTour(Minecraft mc, MinecraftServer server) {
        TownStyle[] styles = TownStyle.values();
        if (tourStep >= styles.length * 6) {
            if (tourStep++ == styles.length * 6) {
                HollowFrontier.LOGGER.info("[PhotoBooth] townbuild done, quitting");
                quit(mc, server);
            }
            return;
        }
        TownStyle style = styles[tourStep / 6];
        int phase = tourStep % 6;
        tourWait++;
        Vec3 at = mc.player.position();
        boolean moved = at.distanceTo(tourFrom) > 2;
        switch (phase) {
            case 0 -> {
                tourFrom = at;
                findTown(server, style);
                next();
            }
            case 1 -> { // the town found and the camera over it, then a while for it to generate
                if (TOWN_CHUNKS.containsKey(style) && moved && tourWait > 200 || tourWait > 1200) {
                    frameTown(server, style);
                    next();
                }
            }
            default -> {
                int view = phase - 2;
                if (!tourIssued) { // the town read off first (frameTown runs on the server, in its own time)
                    if (FRAMES.containsKey(style) || tourWait > 600) {
                        tourFrom = at;
                        townView(server, style, view);
                        tourIssued = true;
                        tourWait = 0;
                    }
                } else if ((moved || tourWait > 400) && tourWait > 40) {
                    shot(mc, String.format(TOUR_SHOTS[view], style.name().toLowerCase(java.util.Locale.ROOT)));
                    tourIssued = false;
                    next();
                }
            }
        }
    }

    private static void next() {
        tourStep++;
        tourWait = 0;
    }

    // ---------------------------------------------------------------- the Hollow Earth's sites, one by one (25-27)

    private static final boolean HOLLOWBUILD_ONLY = "hollowbuild".equals(System.getProperty("hollowfrontier.photo.scene"));
    /** -PphotoFilter=gate,lab shoots only the sites whose template name holds one of those words. */
    private static final List<String> SITE_FILTER = java.util.Arrays.stream(System.getProperty("hollowfrontier.photo.filter", "").split(","))
            .filter(s -> !s.isBlank()).toList();
    /**
     * A site to shoot: its template, the heights (template y, highest first) to cut it down to so its rooms show from above,
     * and views from inside it (eye and target in the template's coordinates).
     */
    private record SitePlot(String template, int[] cuts, SiteView... views) {}
    private static List<SitePlot> sitePlots() {
        List<SitePlot> all = new java.util.ArrayList<>(List.of(
                new SitePlot(HollowBlueprints.KINDLED_HENGE, new int[] {},
                        new SiteView("ring", new Vec3(15.5, 1, 2.5), new Vec3(15.5, 6, 15.5)),
                        new SiteView("niche", new Vec3(17.5, 1, 12.5), new Vec3(26, 3, 18))),
                new SitePlot(HollowBlueprints.KINDLED_GATE, new int[] {11},
                        new SiteView("front", new Vec3(13.5, 3, 36), new Vec3(13.5, 9, 18)),
                        new SiteView("hall", new Vec3(13.5, 3, 15.5), new Vec3(13.5, 5, 5)),
                        new SiteView("tomb", new Vec3(13.5, 3, 8.5), new Vec3(13.5, 5, 2.5)),
                        new SiteView("vault", new Vec3(8.5, 3, 10.5), new Vec3(3, 4, 10.5)),
                        new SiteView("ossuary", new Vec3(18.5, 3, 10.5), new Vec3(24, 4, 10.5))),
                new SitePlot(HollowBlueprints.KINDLED_TEMPLE, new int[] {9},
                        new SiteView("mural", new Vec3(12.5, 3, 23.5), new Vec3(12.5, 7, 8)),
                        new SiteView("scriptorium", new Vec3(3.5, 3, 5.5), new Vec3(11, 4, 5))),
                new SitePlot(HollowBlueprints.GRAY_LAB, new int[] {5},
                        new SiteView("dome", new Vec3(17.5, 1.5, 24.5), new Vec3(6, 2.5, 24.5)),
                        new SiteView("control", new Vec3(16, 1, 6.5), new Vec3(9, 1.5, 3)),
                        new SiteView("bunks", new Vec3(7.5, 1, 6.5), new Vec3(2, 1.5, 5))),
                new SitePlot(HollowBlueprints.GRAY_STATION, new int[] {6},
                        new SiteView("dome", new Vec3(12.5, 1.5, 24.5), new Vec3(12.5, 2.5, 34)),
                        new SiteView("ops", new Vec3(28.5, 1, 20), new Vec3(34, 2, 18.5))),
                new SitePlot(HollowBlueprints.pensTemplate(3), new int[] {6},
                        new SiteView("cells", new Vec3(16.5, 1.5, 11.5), new Vec3(4, 2, 4)),
                        new SiteView("locker", new Vec3(6.5, 1, 12.5), new Vec3(4, 2, 18))),
                new SitePlot(HollowBlueprints.SYMMES_CAMP, new int[] {},
                        new SiteView("wreck", new Vec3(17.5, 1.5, 19), new Vec3(17, 2.5, 6)),
                        new SiteView("gondola", new Vec3(21.5, 2, 12.5), new Vec3(12, 3, 12.5)),
                        new SiteView("dig", new Vec3(32, 2, 20.5), new Vec3(39, 3, 20.5))),
                new SitePlot(ConcaviaBlueprints.CRYSTAL_RUINS, new int[] {8},
                        new SiteView("plaza", new Vec3(20.5, 1.5, 1.5), new Vec3(20.5, 4, 20)),
                        new SiteView("house", new Vec3(10.5, 1, 14.5), new Vec3(3, 1.5, 14.5)),
                        new SiteView("market", new Vec3(27.5, 1, 20), new Vec3(35, 2, 20)),
                        new SiteView("library", new Vec3(20.5, 1, 30.5), new Vec3(20.5, 2, 37))),
                new SitePlot(ConcaviaBlueprints.SUN_TEMPLE, new int[] {},
                        new SiteView("arena", new Vec3(20.5, 9.5, 26.5), new Vec3(20.5, 12, 50)),
                        new SiteView("hall", new Vec3(20.5, 9, 41.5), new Vec3(20.5, 13, 52)),
                        new SiteView("approach", new Vec3(20.5, 2, 1), new Vec3(20.5, 6, 18))),
                // 27: the wild sites
                new SitePlot(WildBlueprints.ZIGGURAT, new int[] {7},
                        new SiteView("passage", new Vec3(12.5, 1, -3), new Vec3(12.5, 2, 12)),
                        new SiteView("idol", new Vec3(12.5, 1, 9.5), new Vec3(12.5, 3, 13))),
                new SitePlot(WildBlueprints.TAR_PIT, new int[] {},
                        new SiteView("camp", new Vec3(18, 2, 28), new Vec3(24, 1.5, 24))),
                new SitePlot(WildBlueprints.PHLOGISTON_MINE, new int[] {5},
                        new SiteView("headframe", new Vec3(20, 2, 22), new Vec3(12, 8, 12))),
                new SitePlot(WildBlueprints.MINE_CAVERN, new int[] {9}),
                new SitePlot(WildBlueprints.OBSERVATORY, new int[] {6, 1},
                        new SiteView("lens", new Vec3(13, 21, 13), new Vec3(10, 23, 10))),
                new SitePlot(WildBlueprints.FOUNDRY, new int[] {},
                        new SiteView("hall", new Vec3(14.5, 1.5, 16), new Vec3(7, 2.5, 6)),
                        new SiteView("workshop", new Vec3(21.5, 1.5, 15), new Vec3(25, 2, 15))),
                new SitePlot(WildBlueprints.SKY_SHRINE, new int[] {},
                        new SiteView("waystone", new Vec3(8.5, 1, 14), new Vec3(8.5, 2.5, 8.5))),
                new SitePlot(WildBlueprints.GRAY_RELAY, new int[] {},
                        new SiteView("under", new Vec3(16, 2, 16), new Vec3(7.5, 7, 7.5)))));        return all.stream().filter(p -> SITE_FILTER.isEmpty() || SITE_FILTER.stream().anyMatch(p.template()::contains)).toList();
    }
    private static final int SITE_OUT_TICKS = 90, SITE_VIEW_TICKS = 24, SITE_CUT_TICKS = 32;
    private static int siteSizeX = 32, siteSizeY = 24, siteSizeZ = 32;

    private static final boolean DUNGEONS_ONLY = "dungeons".equals(System.getProperty("hollowfrontier.photo.scene"));
    private static final int DUNGEON_TICKS = 260, DUNGEON_REACH = 72;

    /**
     * `hf_v*`: each dungeon kit grown from its shaft by the jigsaw (as under its site, two runs each so the side rooms
     * differ), in the sky over a cleared stage: shot from an angle with its roofs on, then with everything over its rooms'
     * walls taken away, from straight above and from two corners.
     */
    private static void dungeons(Minecraft mc, MinecraftServer server, int t) {
        int x = baseX, z = baseZ, top = Y + 10;
        List<com.hollowfrontier.worldgen.structure.DungeonBlueprints.Kit> kits = com.hollowfrontier.worldgen.structure.DungeonBlueprints.KITS;
        if (t == 0) {
            run(server, "gamemode spectator @p");
            run(server, "time set 6000");
            run(server, "weather clear");
            mc.options.cloudStatus().set(net.minecraft.client.CloudStatus.OFF);
            run(server, String.format("forceload add %d %d %d %d", x - DUNGEON_REACH, z - DUNGEON_REACH, x + DUNGEON_REACH, z + DUNGEON_REACH));
            return;
        }
        int s = t - 20, n = s / DUNGEON_TICKS, k = s % DUNGEON_TICKS;
        if (s < 0) {
            return;
        }
        if (n >= kits.size() * 2) {
            if (k == 0) {
                HollowFrontier.LOGGER.info("[PhotoBooth] dungeons done, quitting");
                quit(mc, server);
            }
            return;
        }
        var kit = kits.get(n / 2);
        int shaft = kit == com.hollowfrontier.worldgen.structure.DungeonBlueprints.UNDERCROFT
                ? com.hollowfrontier.worldgen.structure.DungeonBlueprints.UNDERCROFT_SHAFT : com.hollowfrontier.worldgen.structure.DungeonBlueprints.SHAFT;
        int floor = top - shaft;
        String tag = String.format("hf_v%d%d_%s", n / 2, n % 2, kit.id());
        int r = DUNGEON_REACH;
        switch (k) {
            case 0 -> {
                run(server, "kill @e[type=!player,distance=..200]");
                for (int y = floor - 4; y <= top + 2; y += 3) {
                    for (int half = 0; half < 4; half++) {
                        int x0 = x - r + half * (r / 2), x1 = x0 + r / 2 - 1;
                        run(server, String.format("fill %d %d %d %d %d %d minecraft:air", x0, y, z - r, x1, y + 2, z + r));
                    }
                }
            }
            case 4 -> run(server, String.format("place jigsaw %s %s %d %d %d %d", kit.pool("shaft"), kit.name("shaft"), kit == com.hollowfrontier.worldgen.structure.DungeonBlueprints.UNDERCROFT ? 5 : 6, x, top, z));
            case 30 -> tpLooking(server, new Vec3(x - 40, top + 30, z - 40), new Vec3(x, floor, z));
            case 70 -> shot(mc, tag + "_roofed.png");
            case 74 -> {
                run(server, "kill @e[type=minecraft:item,distance=..200]");
                for (int y = floor + 5; y <= top + 2; y += 3) {
                    for (int half = 0; half < 4; half++) {
                        int x0 = x - r + half * (r / 2), x1 = x0 + r / 2 - 1;
                        run(server, String.format("fill %d %d %d %d %d %d minecraft:air", x0, y, z - r, x1, y + 2, z + r));
                    }
                }
                run(server, String.format(java.util.Locale.ROOT, "tp @p %d.5 %d %d.5 0 90", x, floor + 70, z));
            }
            case 120 -> shot(mc, tag + "_plan.png");
            case 124 -> tpLooking(server, new Vec3(x + 34, floor + 34, z + 34), new Vec3(x, floor, z));
            case 170 -> shot(mc, tag + "_cut_se.png");
            case 174 -> tpLooking(server, new Vec3(x - 34, floor + 34, z - 30), new Vec3(x, floor, z));
            case 220 -> shot(mc, tag + "_cut_nw.png");
            default -> {
            }
        }
    }

    private static int siteTicks(SitePlot p) {
        return SITE_OUT_TICKS + p.views().length * SITE_VIEW_TICKS + p.cuts().length * SITE_CUT_TICKS;
    }

    /**
     * `hf_y*`: each Hollow site placed alone on a grass stage in the sky (cleared between them), shot from two corners,
     * then from inside, then from above with everything over each cut height taken away.
     */
    private static void hollowBuild(Minecraft mc, MinecraftServer server, int t) {
        int x = baseX, z = baseZ, oy = Y - 1;
        if (t == 0) {
            run(server, "gamemode spectator @p");
            run(server, "time set 6000");
            run(server, "weather clear");
            mc.options.cloudStatus().set(net.minecraft.client.CloudStatus.OFF);
            run(server, String.format("forceload add %d %d %d %d", x - 24, z - 24, x + 80, z + 80));
            run(server, String.format("tp @p %d %d %d", x, Y + 30, z - 20));
            return;
        }
        List<SitePlot> plots = sitePlots();
        int s = t - 20, n = 0;
        while (n < plots.size() && s >= siteTicks(plots.get(n))) {
            s -= siteTicks(plots.get(n));
            n++;
        }
        if (s < 0) {
            return;
        }
        if (n >= plots.size()) {
            if (s == 0) {
                HollowFrontier.LOGGER.info("[PhotoBooth] hollowbuild done, quitting");
                quit(mc, server);
            }
            return;
        }
        SitePlot p = plots.get(n);
        String tag = String.format("hf_y%02d_%s", n, p.template().replace('/', '_'));
        double cx = x + siteSizeX / 2.0, cz = z + siteSizeZ / 2.0;
        double far = Math.max(siteSizeX, siteSizeZ) * 0.6 + 6;
        if (s == 0) {
            run(server, "kill @e[type=!player,distance=..160]");
            for (int dy = -24; dy < 48; dy += 6) { // cleared in slabs under the fill limit
                run(server, String.format("fill %d %d %d %d %d %d minecraft:air", x - 8, Y + dy, z - 8, x + 60, Y + dy + 5, z + 60));
            }
            run(server, String.format("fill %d %d %d %d %d %d minecraft:grass_block", x - 8, oy, z - 8, x + 60, oy, z + 60));
            server.execute(() -> server.overworld().getStructureTemplateManager().get(HollowFrontier.id(p.template())).ifPresent(template -> {
                siteSizeX = template.getSize().getX();
                siteSizeY = template.getSize().getY();
                siteSizeZ = template.getSize().getZ();
            }));
            run(server, String.format("place template hollowfrontier:%s %d %d %d", p.template(), x, oy, z));
        } else if (s == 8) {
            tpLooking(server, new Vec3(x - far * 0.45, Y + far * 0.55, z - far * 0.45), new Vec3(cx, Y + 3, cz));
        } else if (s == 40) {
            run(server, "kill @e[type=minecraft:item,distance=..120]");
            shot(mc, tag + "_out.png");
        } else if (s == 44) {
            tpLooking(server, new Vec3(x + siteSizeX + far * 0.4, Y + far * 0.35, z + siteSizeZ + far * 0.4), new Vec3(cx, Y + 4, cz));
        } else if (s == 80) {
            shot(mc, tag + "_out2.png");
        } else if (s >= SITE_OUT_TICKS) {
            int k = s - SITE_OUT_TICKS;
            if (k < p.views().length * SITE_VIEW_TICKS) {
                SiteView view = p.views()[k / SITE_VIEW_TICKS];
                if (k % SITE_VIEW_TICKS == 0) {
                    tpLooking(server, new Vec3(x, oy, z).add(view.eye()), new Vec3(x, oy, z).add(view.target()));
                } else if (k % SITE_VIEW_TICKS == SITE_VIEW_TICKS - 4) {
                    shot(mc, tag + "_" + view.shot() + ".png");
                }
                return;
            }
            k -= p.views().length * SITE_VIEW_TICKS;
            int c = k / SITE_CUT_TICKS;
            if (k % SITE_CUT_TICKS == 0) {
                int cutY = oy + p.cuts()[c];
                run(server, "kill @e[type=minecraft:item,distance=..120]");
                for (int dy = 0; dy < Math.max(8, siteSizeY - p.cuts()[c] + 2); dy += 6) {
                    run(server, String.format("fill %d %d %d %d %d %d minecraft:air", x - 1, cutY + dy, z - 1, x + siteSizeX, cutY + dy + 5, z + siteSizeZ));
                }
                tpLooking(server, new Vec3(cx, cutY + Math.max(siteSizeX, siteSizeZ) * 0.6 + 4, cz - 4), new Vec3(cx, oy, cz + 0.5));
            } else if (k % SITE_CUT_TICKS == 20) {
                run(server, "kill @e[type=minecraft:item,distance=..120]");
            } else if (k % SITE_CUT_TICKS == SITE_CUT_TICKS - 4) {
                shot(mc, tag + "_cut" + (c + 1) + ".png");
            }
        }
    }

    // ---------------------------------------------------------------- town close-ups (playtest spots)

    private static final boolean TOWNFIX_ONLY = "townfix".equals(System.getProperty("hollowfrontier.photo.scene"));
    /** A close-up: a template placed on the stage, the camera's feet and what it looks at, in the template's coordinates. */
    private record CloseUp(String template, String name, Vec3 feet, Vec3 at) {}
    private static final List<CloseUp> CLOSE_UPS = List.of(
            new CloseUp("town/livery", "livery_lamps", new Vec3(8.5, 1, 4.5), new Vec3(5.5, 5.5, 7.5)),
            new CloseUp("town/chapel", "chapel_lamps", new Vec3(4.5, 1, 3.5), new Vec3(4.5, 5.0, 9.5)),
            new CloseUp("town/house", "house_chimney", new Vec3(11.5, 6, 2.5), new Vec3(7.5, 7.5, 6.5)),
            new CloseUp("town/blacksmith", "forge_chimney", new Vec3(12.5, 9, 4.5), new Vec3(8.5, 10.5, 8.5)),
            new CloseUp("town/general_store", "store_chimney", new Vec3(14.5, 6, 2.5), new Vec3(9.5, 6.5, 5.5)));

    /** `hf_x*`: each close-up's template placed alone on the stage, the camera stood inside or before it, one shot. */
    private static void townFix(Minecraft mc, MinecraftServer server, int t) {
        int x = baseX, z = baseZ, oy = Y - 1;
        if (t == 0) {
            run(server, "gamemode spectator @p");
            run(server, "time set 6000");
            run(server, "weather clear");
            run(server, String.format("forceload add %d %d %d %d", x - 24, z - 24, x + 48, z + 48));
            run(server, String.format("tp @p %d %d %d", x, Y + 30, z - 20));
            return;
        }
        int s = t - 20, n = s / 60, k = s % 60;
        if (s < 0) {
            return;
        }
        if (n >= CLOSE_UPS.size()) {
            if (k == 0) {
                HollowFrontier.LOGGER.info("[PhotoBooth] townfix done, quitting");
                quit(mc, server);
            }
            return;
        }
        CloseUp c = CLOSE_UPS.get(n);
        Vec3 origin = new Vec3(x, oy, z);
        if (k == 0) {
            run(server, "kill @e[type=!player,distance=..120]");
            for (int dy = 0; dy < 32; dy += 8) {
                run(server, String.format("fill %d %d %d %d %d %d minecraft:air", x - 6, Y + dy, z - 12, x + 30, Y + dy + 7, z + 30));
            }
            run(server, String.format("fill %d %d %d %d %d %d minecraft:grass_block", x - 6, oy, z - 12, x + 30, oy, z + 30));
            run(server, String.format("place template hollowfrontier:%s %d %d %d", c.template(), x, oy, z));
        } else if (k == 10) {
            run(server, "kill @e[type=!player,distance=..120]"); // the townsfolk would stand in the way
            tpLooking(server, origin.add(c.feet()), origin.add(c.at()).subtract(0, 1.62, 0));
        } else if (k == 50) {
            shot(mc, String.format("hf_x%02d_%s.png", n, c.name()));
        }
    }

    // ---------------------------------------------------------------- the worn Cowboy Hat (CowboyHatModel)

    private static final boolean HAT_ONLY = "hat".equals(System.getProperty("hollowfrontier.photo.scene"));

    /**
     * `hf_h0`-`hf_h3`: armor stands in the hat (front, three-quarter, side, head tilted) and a zombie wearing one, a close-up,
     * then the player wearing it in third person from the front and from behind.
     */
    private static void hat(Minecraft mc, MinecraftServer server, int t) {
        int x = baseX, z = baseZ;
        String hat = "head:{id:\"hollowfrontier:cowboy_hat\",count:1}";
        switch (t) {
            case 0 -> {
                run(server, "gamemode spectator @p");
                run(server, "time set 6000");
                run(server, "weather clear");
            }
            case 20 -> {
                run(server, String.format("fill %d %d %d %d %d %d minecraft:grass_block", x - 8, Y - 1, z - 8, x + 12, Y - 1, z + 10));
                run(server, String.format("fill %d %d %d %d %d %d minecraft:air", x - 8, Y, z - 8, x + 12, Y + 6, z + 10));
                run(server, "kill @e[type=!player,distance=..40]");
                String[] stands = {"Rotation:[180f,0f]", "Rotation:[135f,0f]", "Rotation:[90f,0f]", "Rotation:[180f,0f],Pose:{Head:[-25f,30f,0f]}"};
                for (int i = 0; i < stands.length; i++) {
                    run(server, String.format("summon minecraft:armor_stand %d.5 %d %d.5 {ShowArms:1b,%s,equipment:{%s,chest:{id:\"hollowfrontier:duster\",count:1}}}",
                            x + 2 * i, Y, z + 4, stands[i], hat));
                }
                run(server, String.format("summon minecraft:zombie %d.5 %d %d.5 {NoAI:1b,PersistenceRequired:1b,Rotation:[160f,0f],equipment:{%s}}", x + 8, Y, z + 4, hat));
                tpLooking(server, new Vec3(x + 4.5, Y + 2.2, z - 1.0), new Vec3(x + 4.5, Y + 1.4, z + 4.5));
            }
            case 70 -> shot(mc, "hf_h0_hat_lineup.png");
            case 72 -> tpLooking(server, new Vec3(x + 1.2, Y + 2.3, z + 2.2), new Vec3(x + 0.5, Y + 1.8, z + 4.5));
            case 100 -> shot(mc, "hf_h1_hat_close.png");
            case 102 -> {
                run(server, "gamemode creative @p");
                run(server, "item replace entity @p armor.head with hollowfrontier:cowboy_hat");
                mc.player.getInventory().setSelectedSlot(8);
                mc.options.setCameraType(CameraType.THIRD_PERSON_FRONT);
                tp(server, x - 4, z - 4, 20, 10);
            }
            case 140 -> shot(mc, "hf_h2_player_front.png");
            case 142 -> {
                mc.options.setCameraType(CameraType.THIRD_PERSON_BACK);
                tp(server, x - 4, z - 4, 20, 25);
            }
            case 170 -> shot(mc, "hf_h3_player_back.png");
            case 175 -> {
                mc.options.setCameraType(CameraType.FIRST_PERSON);
                run(server, "item replace entity @p armor.head with minecraft:air");
                run(server, "kill @e[type=!player,distance=..40]");
                HollowFrontier.LOGGER.info("[PhotoBooth] hat done, quitting");
                quit(mc, server);
            }
            default -> {}
        }
    }

    // ---------------------------------------------------------------- phase 22: the enchanted guns' glint (GlintGeoLayer)

    private static final boolean GLINT_ONLY = "glint".equals(System.getProperty("hollowfrontier.photo.scene"));

    /**
     * `hf_s0`-`hf_s1`: armor stands holding each gun, plain in the front row and enchanted behind, then an enchanted revolver
     * in first person with enchanted guns on the hotbar.
     */
    private static void glint(Minecraft mc, MinecraftServer server, int t) {
        int x = baseX, z = baseZ;
        String[] guns = {"revolver", "plains_rifle", "shotgun", "ray_pistol", "phlogiston_rifle"};
        String enchanted = ",components:{\"minecraft:enchantments\":{\"hollowfrontier:deadeye\":1}}";
        switch (t) {
            case 0 -> {
                run(server, "gamemode spectator @p");
                run(server, "time set 6000");
                run(server, "weather clear");
            }
            case 20 -> {
                run(server, String.format("fill %d %d %d %d %d %d minecraft:grass_block", x - 8, Y - 1, z - 8, x + 12, Y - 1, z + 10));
                run(server, String.format("fill %d %d %d %d %d %d minecraft:air", x - 8, Y, z - 8, x + 12, Y + 6, z + 10));
                run(server, "kill @e[type=!player,distance=..40]");
                for (int i = 0; i < guns.length; i++) {
                    for (int row = 0; row < 2; row++) {
                        run(server, String.format("summon minecraft:armor_stand %d.5 %d %d.5 {ShowArms:1b,Rotation:[180f,0f],equipment:{mainhand:{id:\"hollowfrontier:%s\",count:1%s}}}",
                                x + 2 * i, Y, z + 3 + 2 * row, guns[i], row == 1 ? enchanted : ""));
                    }
                }
                tpLooking(server, new Vec3(x + 4.5, Y + 2.0, z - 1.5), new Vec3(x + 4.5, Y + 1.2, z + 4.0));
            }
            case 70 -> shot(mc, "hf_s0_gun_glint_lineup.png");
            case 72 -> {
                run(server, "kill @e[type=!player,distance=..40]");
                run(server, "gamemode creative @p");
                for (int i = 0; i < guns.length; i++) {
                    run(server, String.format("item replace entity @p hotbar.%d with hollowfrontier:%s[enchantments={\"hollowfrontier:deadeye\":1}]", i, guns[i]));
                }
                run(server, "item replace entity @p hotbar.5 with hollowfrontier:revolver");
                mc.player.getInventory().setSelectedSlot(0);
                mc.options.setCameraType(CameraType.FIRST_PERSON);
                tpLooking(server, new Vec3(x + 4.5, Y + 1.0, z - 1.5), new Vec3(x + 4.5, Y + 1.0, z + 8.0));
            }
            case 110 -> shot(mc, "hf_s1_gun_glint_first_person.png");
            case 115 -> {
                for (int i = 0; i < 6; i++) {
                    run(server, String.format("item replace entity @p hotbar.%d with minecraft:air", i));
                }
                HollowFrontier.LOGGER.info("[PhotoBooth] glint done, quitting");
                quit(mc, server);
            }
            default -> {}
        }
    }

    private static final boolean SMOKE_ONLY = "smoke".equals(System.getProperty("hollowfrontier.photo.scene"));

    /** `hf_s2`-`hf_s3`: the muzzle smoke as the shooter sees it, a few ticks after a revolver shot and a shotgun blast. */
    private static void smoke(Minecraft mc, MinecraftServer server, int t) {
        int x = baseX, z = baseZ;
        switch (t) {
            case 0 -> {
                run(server, "gamemode creative @p");
                run(server, "time set 6000");
                run(server, "weather clear");
            }
            case 20 -> {
                run(server, String.format("fill %d %d %d %d %d %d minecraft:grass_block", x - 8, Y - 1, z - 8, x + 12, Y - 1, z + 30));
                run(server, String.format("fill %d %d %d %d %d %d minecraft:air", x - 8, Y, z - 8, x + 12, Y + 6, z + 30));
                run(server, "kill @e[type=!player,distance=..40]");
                run(server, String.format("summon minecraft:zombie %d.5 %d %d.5 {NoAI:1b,PersistenceRequired:1b,Rotation:[180f,0f]}", x, Y, z + 12));
                run(server, "item replace entity @p hotbar.0 with hollowfrontier:revolver");
                run(server, "item replace entity @p hotbar.1 with hollowfrontier:shotgun");
                mc.player.getInventory().setSelectedSlot(0);
                mc.options.setCameraType(CameraType.FIRST_PERSON);
                tpLooking(server, new Vec3(x + 0.5, Y, z - 2.5), new Vec3(x + 0.5, Y + 1.0, z + 12.5));
            }
            case 60 -> server.execute(() -> GunMechanics.fire(server.overworld(), server.getPlayerList().getPlayers().getFirst(), GunTypes.REVOLVER));
            case 64 -> shot(mc, "hf_s2_smoke_revolver.png");
            case 100 -> mc.player.getInventory().setSelectedSlot(1);
            case 120 -> server.execute(() -> GunMechanics.fire(server.overworld(), server.getPlayerList().getPlayers().getFirst(), GunTypes.SHOTGUN));
            case 124 -> shot(mc, "hf_s3_smoke_shotgun.png");
            case 130 -> {
                run(server, "item replace entity @p hotbar.0 with minecraft:air");
                run(server, "item replace entity @p hotbar.1 with minecraft:air");
                run(server, "kill @e[type=!player,distance=..40]");
                HollowFrontier.LOGGER.info("[PhotoBooth] smoke done, quitting");
                quit(mc, server);
            }
            default -> {}
        }
    }

    // ---------------------------------------------------------------- phase 11b: the men in black dusters

    // ---------------------------------------------------------------- 31c: the humanoid model's people

    private static final boolean PEOPLE_ONLY = "people".equals(System.getProperty("hollowfrontier.photo.scene"));
    /** Every look on the humanoid model, as summon ids with their NBT, six to a group. */
    private static final List<String> PEOPLE = List.of(
            "outlaw", "outlaw_gunslinger", "outlaw_boss", "man_in_black", "kindled", "lawman {rank:\"marshal\"}",
            "lawman {rank:\"sheriff\"}", "lawman {rank:\"deputy\"}", "lawman {rank:\"ranger\"}",
            "townsfolk {folk:\"shopkeeper\"}", "townsfolk {folk:\"banker\"}", "townsfolk {folk:\"blacksmith\"}",
            "townsfolk {folk:\"wrangler\"}", "townsfolk {folk:\"prospector\"}", "townsfolk {folk:\"engineer\"}",
            "townsfolk {folk:\"doctor\"}", "townsfolk {folk:\"teacher\"}", "townsfolk {folk:\"preacher\"}",
            "townsfolk {folk:\"postmaster\"}", "townsfolk {folk:\"gunsmith\"}", "townsfolk {folk:\"editor\"}");
    private static final List<String> WALKERS = List.of("outlaw", "outlaw_boss", "man_in_black", "lawman {rank:\"marshal\"}",
            "townsfolk {folk:\"teacher\"}", "kindled");

    private static String summon(String person, double x, double z, String extra) {
        int brace = person.indexOf(" {");
        String id = brace < 0 ? person : person.substring(0, brace);
        String nbt = brace < 0 ? "" : person.substring(brace + 2, person.length() - 1) + ",";
        return String.format(java.util.Locale.ROOT, "summon hollowfrontier:%s %.2f %d %.2f {%sPersistenceRequired:1b,%s}", id, x, Y, z, nbt, extra);
    }

    /**
     * `hf_people_*`: the people of the humanoid model on a grass stage, six at a time from the front, three-quarter and back
     * (the hat brims' curls, the bandanas, the coat tails, dresses and robes), then six walking past side-on (the tails
     * flaring with the stride) and two on horseback.
     */
    /** Clears the people stage (commands run at the world's spawn, so the kill is placed on the stage). */
    private static void clearStage(MinecraftServer server, int x, int z) {
        run(server, String.format("execute positioned %d %d %d run kill @e[type=!player,distance=..60]", x, Y, z + 10));
    }

    private static void people(Minecraft mc, MinecraftServer server, int t) {
        int x = baseX, z = baseZ;
        int groups = (PEOPLE.size() + 5) / 6;
        if (t == 0) {
            run(server, "gamemode creative @p");
            run(server, "time set 6000");
            run(server, "weather clear");
            run(server, String.format("fill %d %d %d %d %d %d minecraft:grass_block", x - 8, Y - 1, z - 6, x + 40, Y - 1, z + 30));
            run(server, String.format("fill %d %d %d %d %d %d minecraft:air", x - 8, Y, z - 6, x + 40, Y + 8, z + 30));
            run(server, "clear @p"); // nothing in hand to block the view
            return;
        }
        int g = (t - 20) / 80, at = (t - 20) % 80;
        if (t >= 20 && g < groups) {
            List<String> group = PEOPLE.subList(g * 6, Math.min(PEOPLE.size(), g * 6 + 6));
            double left = x + 0.5 - (group.size() - 1) * 0.7;
            switch (at) {
                case 0 -> {
                    clearStage(server, x, z);
                    for (int i = 0; i < group.size(); i++) {
                        run(server, summon(group.get(i), left + i * 1.4, z + 6.5, "NoAI:1b,Rotation:[180f,0f]"));
                    }
                    tpLooking(server, new Vec3(x + 0.5, Y + 1.5, z + 1.0), new Vec3(x + 0.5, Y + 1.1, z + 6.5));
                }
                case 40 -> shot(mc, "hf_people_" + g + "a_front.png");
                case 42 -> tpLooking(server, new Vec3(x - 2.5, Y + 2.2, z + 3.0), new Vec3(left, Y + 1.3, z + 6.5));
                case 55 -> shot(mc, "hf_people_" + g + "b_three_quarter.png");
                case 57 -> tpLooking(server, new Vec3(x + 0.5, Y + 1.5, z + 12.0), new Vec3(x + 0.5, Y + 1.1, z + 6.5));
                case 75 -> shot(mc, "hf_people_" + g + "c_back.png");
                default -> {}
            }
            return;
        }
        int w = t - 20 - groups * 80;
        switch (w) {
            case 0 -> {
                clearStage(server, x, z);
                for (int i = 0; i < WALKERS.size(); i++) {
                    run(server, summon(WALKERS.get(i), x - 3.5 + i * 0.3, z + 8.5 + i * 1.5, "Tags:[\"hf_walk\"],Rotation:[-90f,0f]"));
                }
                tpLooking(server, new Vec3(x + 3.5, Y + 1.6, z + 3.0), new Vec3(x + 3.5, Y + 1.0, z + 11.0));
            }
            case 30, 38, 46 -> shot(mc, "hf_people_walk_" + (w - 30) / 8 + ".png");
            case 70 -> {
                clearStage(server, x, z);
                run(server, String.format(java.util.Locale.ROOT, "summon minecraft:horse %.1f %d %.1f {NoAI:1b,Tame:1b,Variant:3,Rotation:[90f,0f],Passengers:[{id:\"hollowfrontier:outlaw_boss\",NoAI:1b}]}", x - 1.0, Y, z + 8.5));
                run(server, String.format(java.util.Locale.ROOT, "summon minecraft:horse %.1f %d %.1f {NoAI:1b,Tame:1b,Variant:1,Rotation:[90f,0f],Passengers:[{id:\"hollowfrontier:lawman\",rank:\"marshal\",NoAI:1b}]}", x + 2.5, Y, z + 8.5));
                tpLooking(server, new Vec3(x + 0.8, Y + 2.2, z + 4.5), new Vec3(x + 0.8, Y + 1.7, z + 8.5));
            }
            case 110 -> shot(mc, "hf_people_ride_side.png");
            case 112 -> tpLooking(server, new Vec3(x + 6.5, Y + 2.2, z + 11.5), new Vec3(x + 0.8, Y + 1.6, z + 8.5));
            case 130 -> shot(mc, "hf_people_ride_behind.png");
            // Close-ups: the masked outlaw, the gunslinger's neckerchief, the teacher's dress and the Kindled's robe
            case 135 -> {
                clearStage(server, x, z);
                List<String> close = List.of("outlaw", "outlaw_gunslinger", "townsfolk {folk:\"teacher\"}", "kindled");
                for (int i = 0; i < close.size(); i++) {
                    run(server, summon(close.get(i), x - 1.6 + i * 1.1, z + 4.5, "NoAI:1b,Rotation:[180f,0f]"));
                }
                tpLooking(server, new Vec3(x - 2.2, Y + 2.0, z + 2.2), new Vec3(x - 0.6, Y + 1.4, z + 4.5));
            }
            case 175 -> shot(mc, "hf_people_close_three_quarter.png");
            case 177 -> tpLooking(server, new Vec3(x - 4.4, Y + 1.7, z + 4.5), new Vec3(x - 1.6, Y + 1.3, z + 4.5)); // the outlaw in profile, the rest behind
            case 195 -> shot(mc, "hf_people_close_side.png");
            case 197 -> tpLooking(server, new Vec3(x - 0.2, Y + 1.5, z + 7.5), new Vec3(x - 0.2, Y + 1.2, z + 4.5));
            case 215 -> shot(mc, "hf_people_close_back.png");
            // Face on at hat height, a little above: which way the brims curl
            case 217 -> tpLooking(server, new Vec3(x - 1.0, Y + 2.3, z + 2.6), new Vec3(x - 1.0, Y + 1.85, z + 4.5));
            case 235 -> shot(mc, "hf_people_close_hats.png");
            case 240 -> {
                clearStage(server, x, z);
                HollowFrontier.LOGGER.info("[PhotoBooth] people done, quitting");
                quit(mc, server);
            }
            default -> {}
        }
        // Walk them east along their lines, past the camera
        if (w > 0 && w < 70 && w % 5 == 1) {
            server.execute(() -> {
                ServerLevel level = server.overworld();
                for (Mob mob : level.getEntitiesOfClass(Mob.class, new AABB(x - 20, Y - 2, z - 10, x + 50, Y + 6, z + 40),
                        mob -> mob.entityTags().contains("hf_walk"))) {
                    mob.getNavigation().moveTo(mob.getX() + 6, Y, mob.getZ(), 0.5);
                }
            });
        }
    }

    private static final boolean BLACK_ONLY = "black".equals(System.getProperty("hollowfrontier.photo.scene"));

    /** `hf_f0`-`hf_f1`: two men in black posed close up (the implant behind the ear), then one drawing on a marked player. */
    private static void black(Minecraft mc, MinecraftServer server, int t) {
        int x = baseX, z = baseZ;
        switch (t) {
            case 0 -> {
                run(server, "gamemode creative @p");
                run(server, "time set 6000");
                run(server, "weather clear");
            }
            case 20 -> {
                run(server, String.format("fill %d %d %d %d %d %d minecraft:grass_block", x - 6, Y - 1, z - 6, x + 12, Y - 1, z + 20));
                run(server, String.format("fill %d %d %d %d %d %d minecraft:air", x - 6, Y, z - 6, x + 12, Y + 6, z + 20));
                run(server, String.format("summon hollowfrontier:man_in_black %d.5 %d %d.5 {NoAI:1b,Rotation:[160f,0f]}", x, Y, z + 4));
                run(server, String.format("summon hollowfrontier:man_in_black %d.5 %d %d.5 {NoAI:1b,Rotation:[215f,0f]}", x + 2, Y, z + 5));
                tpLooking(server, new Vec3(x + 1.5, Y + 1.7, z + 1.2), new Vec3(x + 1.0, Y + 1.5, z + 4.5));
            }
            case 70 -> shot(mc, "hf_f0_men_in_black.png");
            case 72 -> {
                run(server, String.format("kill @e[type=hollowfrontier:man_in_black,distance=..40]"));
                run(server, String.format("summon hollowfrontier:man_in_black %d.5 %d %d.5", x + 1, Y, z + 12));
                tpLooking(server, new Vec3(x + 1.5, Y + 1.6, z + 4.5), new Vec3(x + 1.5, Y + 1.5, z + 12.5));
                run(server, "gamemode survival @p");
                run(server, "execute as @p run hollowfrontier suspicion set 80");
            }
            case 120 -> shot(mc, "hf_f1_drawn_on.png");
            case 125 -> {
                run(server, "gamemode creative @p");
                run(server, "kill @e[type=hollowfrontier:man_in_black,distance=..40]");
                HollowFrontier.LOGGER.info("[PhotoBooth] black done, quitting");
                quit(mc, server);
            }
            default -> {}
        }
    }

    // ---------------------------------------------------------------- phase 11a: the frontier's mysteries

    private static final boolean MYSTERY_ONLY = "mystery".equals(System.getProperty("hollowfrontier.photo.scene"));

    /**
     * `hf_e0`-`hf_e3`, on a grass stage in the sky: a crop circle pressed into a town's wheat field (from above, by day), a
     * mutilated steer in its scorched ring, a probe at work on another, and at midnight a sighting come to hang overhead
     * (all by the {@code /hollowfrontier mystery} command, run where each should happen).
     */
    private static void mystery(Minecraft mc, MinecraftServer server, int t) {
        int x = baseX, z = baseZ;
        switch (t) {
            case 0 -> {
                run(server, "gamemode spectator @p"); // a camera that doesn't fall and trample the field
                run(server, "time set 6000");
                run(server, "weather clear");
            }
            case 20 -> {
                run(server, String.format("fill %d %d %d %d %d %d minecraft:grass_block", x - 6, Y - 1, z - 6, x + 50, Y - 1, z + 40));
                run(server, String.format("fill %d %d %d %d %d %d minecraft:air", x - 6, Y, z - 6, x + 50, Y + 10, z + 40));
                // A town's wheat field (19 x 21, its farmland at the template's y 0)
                run(server, String.format("place template hollowfrontier:town/wheat_field %d %d %d", x, Y - 1, z + 10));
                run(server, String.format("summon minecraft:cow %d.5 %d %d.5 {NoAI:1b}", x + 34, Y, z + 14));
                run(server, String.format("summon minecraft:cow %d.5 %d %d.5 {NoAI:1b}", x + 44, Y, z + 28));
            }
            case 30 -> {
                run(server, String.format("execute positioned %d %d %d run hollowfrontier mystery circle", x + 9, Y, z + 21));
                run(server, String.format("execute positioned %d %d %d run hollowfrontier mystery cattle", x + 34, Y, z + 14));
                tpLooking(server, new Vec3(x + 9.5, Y + 26, z + 6), new Vec3(x + 9.5, Y, z + 20.5));
            }
            case 90 -> shot(mc, "hf_e0_crop_circle.png");
            case 92 -> tpLooking(server, new Vec3(x + 30.5, Y + 3, z + 10), new Vec3(x + 34.5, Y + 0.3, z + 14.5));
            case 120 -> shot(mc, "hf_e1_mutilated_steer.png");
            case 122 -> {
                run(server, String.format("execute positioned %d %d %d run hollowfrontier mystery visit cattle", x + 44, Y, z + 28));
                tpLooking(server, new Vec3(x + 30.5, Y + 4, z + 22), new Vec3(x + 44.5, Y + 3.5, z + 28.5));
            }
            case 260 -> shot(mc, "hf_e2_probe_at_work.png");
            case 262 -> {
                run(server, "time set 18000");
                tpLooking(server, new Vec3(x - 2.5, Y + 1.6, z - 2.5), new Vec3(x + 8.5, Y + 28, z + 8.5));
                run(server, String.format("execute positioned %d %d %d run hollowfrontier mystery sighting hover", x + 8, Y, z + 8));
            }
            case 330 -> shot(mc, "hf_e3_sighting_overhead.png");
            case 335 -> {
                HollowFrontier.LOGGER.info("[PhotoBooth] mystery done, quitting");
                quit(mc, server);
            }
            default -> {}
        }
    }

    // ---------------------------------------------------------------- phase 10b: the Gray experiments

    private static final boolean GRAY_ONLY = "gray".equals(System.getProperty("hollowfrontier.photo.scene"));

    /**
     * `hf_c0`-`hf_c5`: a live Gray field station placed on a stage in the sky (its own template, by command), from above;
     * the four penned specimens in their plating; the lab with the hare on the table; the reactor; and, in survival, a
     * sentinel drawing its beam on the player.
     */
    private static void gray(Minecraft mc, MinecraftServer server, int t) {
        int x = baseX, z = baseZ;
        switch (t) {
            case 0 -> {
                run(server, "gamemode creative @p");
                run(server, "time set 6000");
                run(server, "weather clear");
            }
            case 20 -> {
                run(server, String.format("fill %d %d %d %d %d %d minecraft:grass_block", x - 4, Y - 1, z - 4, x + 48, Y - 1, z + 48));
                run(server, String.format("place template hollowfrontier:gray_station %d %d %d", x, Y - 1, z));
                tpLooking(server, new Vec3(x + 22.5, Y + 30, z - 16), new Vec3(x + 22.5, Y, z + 22));
            }
            case 100 -> shot(mc, "hf_c0_gray_station.png");
            case 102 -> tpLooking(server, new Vec3(x + 11.5, Y + 3.2, z + 17), new Vec3(x + 11.5, Y + 1.2, z + 7));
            case 130 -> shot(mc, "hf_c1_pens_mammoth_megatherium.png");
            case 132 -> tpLooking(server, new Vec3(x + 33.5, Y + 3.2, z + 17), new Vec3(x + 33.5, Y + 1.0, z + 7));
            case 160 -> shot(mc, "hf_c2_pens_bird_glyptodon.png");
            case 162 -> tpLooking(server, new Vec3(x + 12.5, Y + 4.5, z + 25.5), new Vec3(x + 12.5, Y + 1.5, z + 32.5));
            case 190 -> shot(mc, "hf_c3_lab.png");
            case 192 -> tpLooking(server, new Vec3(x + 33.5, Y + 5, z + 24), new Vec3(x + 33.5, Y + 5, z + 32.5));
            case 220 -> shot(mc, "hf_c4_reactor.png");
            case 222 -> {
                tpLooking(server, new Vec3(x + 22.5, Y + 1.6, z + 16.5), new Vec3(x + 22.5, Y + 9, z + 9.5));
                run(server, "gamemode survival @p");
            }
            case 252 -> shot(mc, "hf_c5_sentinel_beam.png");
            case 255 -> {
                run(server, "gamemode creative @p");
                HollowFrontier.LOGGER.info("[PhotoBooth] gray done, quitting");
                quit(mc, server);
            }
            default -> {}
        }
    }

    // ---------------------------------------------------------------- phase 9c: the Hollow Earth's ores and gear

    private static final boolean GEAR_ONLY = "gear".equals(System.getProperty("hollowfrontier.photo.scene"));

    /**
     * `hf_a0`-`hf_a2`: the new blocks in a row (stone behind, so the phlogiston glows), an armor stand in full starsteel with
     * the sword and the horseshoes beside it, the Hollow Earth creative tab, and its advancement tab.
     */
    private static void gear(Minecraft mc, MinecraftServer server, int t) {
        switch (t) {
            case 0 -> {
                run(server, "gamemode creative @p");
                run(server, "time set 6000");
            }
            case 20 -> { // a second on, once the chunks round the stage are in
                String[] blocks = {"starsteel_ore", "deepslate_starsteel_ore", "raw_starsteel_block", "starsteel_block", "phlogiston_ore",
                        "deepslate_phlogiston_ore", "phlogiston_lamp"};
                run(server, String.format("fill %d %d %d %d %d %d minecraft:stone", baseX - 2, Y - 1, baseZ + 6, baseX + 14, Y + 3, baseZ + 7));
                run(server, String.format("fill %d %d %d %d %d %d minecraft:air", baseX - 2, Y, baseZ - 4, baseX + 14, Y + 4, baseZ + 5));
                for (int i = 0; i < blocks.length; i++) {
                    run(server, String.format("setblock %d %d %d hollowfrontier:%s", baseX + i * 2, Y, baseZ + 5, blocks[i]));
                }
                run(server, String.format("summon minecraft:armor_stand %d.5 %d %d.5 {ShowArms:1b,Rotation:[180f,0f],equipment:{"
                        + "head:{id:\"hollowfrontier:starsteel_helmet\",count:1},chest:{id:\"hollowfrontier:starsteel_chestplate\",count:1},"
                        + "legs:{id:\"hollowfrontier:starsteel_leggings\",count:1},feet:{id:\"hollowfrontier:starsteel_boots\",count:1},"
                        + "mainhand:{id:\"hollowfrontier:starsteel_sword\",count:1},offhand:{id:\"hollowfrontier:starsteel_paxel\",count:1}}}",
                        baseX + 6, Y, baseZ + 2));
                tpLooking(server, new Vec3(baseX + 6.5, Y + 1.6, baseZ - 4), new Vec3(baseX + 6.5, Y + 0.8, baseZ + 4));
            }
            case 80 -> shot(mc, "hf_a0_starsteel_and_phlogiston.png");
            case 82 -> {
                creativeTab(mc, ModTabs.HOLLOW_FRONTIER.get());
            }
            case 102 -> shot(mc, "hf_a1_tab_hollow_earth.png");
            case 104 -> {
                mc.gui.setScreen(null);
                run(server, "advancement grant @p until hollowfrontier:hollow/sun_forged");
                run(server, "advancement grant @p only hollowfrontier:hollow/primordial_jungle");
            }
            case 130 -> {
                mc.gui.toastManager().clear();
                mc.gui.setScreen(new AdvancementsScreen(mc.player.connection.getAdvancements()));
                mc.player.connection.getAdvancements().setSelectedTab(mc.player.connection.getAdvancements().get(HollowFrontier.id("hollow/root")), true);
            }
            case 150 -> shot(mc, "hf_a2_advancements_hollow.png");
            case 155 -> {
                HollowFrontier.LOGGER.info("[PhotoBooth] gear done, quitting");
                quit(mc, server);
            }
            default -> {}
        }
    }

    // ---------------------------------------------------------------- phase 9b3: the Hollow Earth's lore sites

    private static final boolean SITES_ONLY = "sites".equals(System.getProperty("hollowfrontier.photo.scene"));

    /** A view of a site: eye and target in its template's own coordinates (turned and moved with the piece). */
    private record SiteView(String shot, Vec3 eye, Vec3 target) {}

    /** The views of each template; the Kindled structure's piece is whichever of its three the pool picked. */
    private static final java.util.Map<String, List<SiteView>> SITE_VIEWS = java.util.Map.of(
            HollowBlueprints.KINDLED_HENGE, List.of(new SiteView("henge", new Vec3(36, 18, 36), new Vec3(15.5, 5, 15.5)),
                    new SiteView("henge_avenue", new Vec3(15.5, 3.2, -3), new Vec3(15.5, 8, 15.5))),
            HollowBlueprints.KINDLED_GATE, List.of(new SiteView("gate", new Vec3(13.5, 9, 44), new Vec3(13.5, 9, 18)),
                    new SiteView("gate_hall", new Vec3(13.5, 5.5, 15.5), new Vec3(13.5, 5, 5))),
            HollowBlueprints.KINDLED_TEMPLE, List.of(new SiteView("temple", new Vec3(36, 16, 46), new Vec3(12.5, 5, 16)),
                    new SiteView("temple_mural", new Vec3(12.5, 5, 23.5), new Vec3(12.5, 7, 8))),
            HollowBlueprints.GRAY_LAB, List.of(new SiteView("gray_lab", new Vec3(44, 20, 44), new Vec3(19, 3, 19)),
                    new SiteView("gray_lab_dome", new Vec3(17.5, 3.5, 24.5), new Vec3(6, 2.5, 24.5)),
                    new SiteView("gray_lab_cages", new Vec3(29.5, 3, 40), new Vec3(29.5, 1.5, 30))),
            HollowBlueprints.SYMMES_CAMP, List.of(new SiteView("symmes_camp", new Vec3(40, 18, 42), new Vec3(17, 2, 16)),
                    new SiteView("symmes_wreck", new Vec3(17.5, 3.5, 19), new Vec3(17, 2.5, 6)),
                    new SiteView("symmes_journal", new Vec3(10.5, 2.6, 20.5), new Vec3(7.5, 1.6, 20.5))));
    /** The sites to visit: Kindled ones sought from several places until each kind has turned up, then the lab and the camp. */
    private static final List<net.minecraft.resources.ResourceKey<net.minecraft.world.level.levelgen.structure.Structure>> SITE_TOUR = List.of(
            com.hollowfrontier.worldgen.structure.ModStructures.KINDLED_SITE, com.hollowfrontier.worldgen.structure.ModStructures.KINDLED_SITE,
            com.hollowfrontier.worldgen.structure.ModStructures.KINDLED_SITE, com.hollowfrontier.worldgen.structure.ModStructures.KINDLED_SITE,
            com.hollowfrontier.worldgen.structure.ModStructures.KINDLED_SITE, com.hollowfrontier.worldgen.structure.ModStructures.KINDLED_SITE,
            com.hollowfrontier.worldgen.structure.ModStructures.KINDLED_SITE, com.hollowfrontier.worldgen.structure.ModStructures.KINDLED_SITE,
            com.hollowfrontier.worldgen.structure.ModStructures.KINDLED_SITE, com.hollowfrontier.worldgen.structure.ModStructures.GRAY_LAB,
            com.hollowfrontier.worldgen.structure.ModStructures.SYMMES_CAMP);

    private static int siteIndex;
    private static int siteStep;
    private static int siteWait;
    private static ChunkPos siteChunk;
    private static final List<SiteView> siteViews = new java.util.ArrayList<>();
    private static final List<Vec3[]> siteCameras = new java.util.ArrayList<>();
    private static final java.util.Set<String> sitesShot = new java.util.HashSet<>();

    /** Each site in turn (`hf_90`+): locate it from fresh ground, hang over it while it generates, then shoot its views. */
    private static void sites(Minecraft mc, MinecraftServer server, int t) {
        if (t == 0) {
            siteIndex = 0;
            siteStep = 0;
            run(server, "gamemode spectator @p");
            mc.options.cloudStatus().set(net.minecraft.client.CloudStatus.OFF);
            return;
        }
        if (siteWait > 0) {
            siteWait--;
            return;
        }
        if (siteIndex >= SITE_TOUR.size()) {
            HollowFrontier.LOGGER.info("[PhotoBooth] sites done, quitting");
            quit(mc, server);
            return;
        }
        var key = SITE_TOUR.get(siteIndex);
        switch (siteStep) {
            case 0 -> { // locate it (no more Kindled sites once all three kinds are shot)
                if (key == com.hollowfrontier.worldgen.structure.ModStructures.KINDLED_SITE && sitesShot.containsAll(
                        List.of(HollowBlueprints.KINDLED_HENGE, HollowBlueprints.KINDLED_GATE, HollowBlueprints.KINDLED_TEMPLE))) {
                    siteIndex++;
                    return;
                }
                siteChunk = null;
                server.execute(() -> {
                    ServerLevel level = server.getLevel(HollowEarth.LEVEL);
                    var holder = level.registryAccess().lookupOrThrow(Registries.STRUCTURE).getOrThrow(key);
                    BlockPos from = LIFE_BASE.offset(siteIndex * 2500, 0, 4000);
                    var found = level.getChunkSource().getGenerator().findNearestMapStructure(level, net.minecraft.core.HolderSet.direct(holder), from, 120, false);
                    if (found == null) {
                        HollowFrontier.LOGGER.warn("[PhotoBooth] no {} near {}", key.identifier(), from);
                        return;
                    }
                    siteChunk = ChunkPos.containing(found.getFirst());
                    run(server, String.format("execute in %s run forceload add %d %d %d %d", HollowEarth.LEVEL.identifier(),
                            siteChunk.getMinBlockX() - 32, siteChunk.getMinBlockZ() - 32, siteChunk.getMinBlockX() + 48, siteChunk.getMinBlockZ() + 48));
                    run(server, String.format(java.util.Locale.ROOT, HOLLOW_TP + "%d.5 %d %d.5 0 90", siteChunk.getMinBlockX(), HollowEarth.CEILING_LOWEST - 40, siteChunk.getMinBlockZ()));
                    HollowFrontier.LOGGER.info("[PhotoBooth] {} at chunk {}", key.identifier(), siteChunk);
                });
                siteStep = 1;
                siteWait = GENERATE_TICKS;
            }
            case 1 -> { // work out where its views are, now it's generated
                siteViews.clear();
                siteCameras.clear();
                if (siteChunk == null) {
                    siteIndex++;
                    siteStep = 0;
                    return;
                }
                server.execute(() -> {
                    ServerLevel level = server.getLevel(HollowEarth.LEVEL);
                    var structure = level.registryAccess().lookupOrThrow(Registries.STRUCTURE).getOrThrow(key).value();
                    StructureStart start = level.getChunk(siteChunk.x(), siteChunk.z()).getStartForStructure(structure);
                    if (start == null || !start.isValid()
                            || !(start.getPieces().getFirst() instanceof net.minecraft.world.level.levelgen.structure.PoolElementStructurePiece piece)) {
                        HollowFrontier.LOGGER.warn("[PhotoBooth] the {} at {} didn't generate", key.identifier(), siteChunk);
                        return;
                    }
                    String element = piece.getElement().toString();
                    String template = SITE_VIEWS.keySet().stream().filter(name -> element.contains(HollowFrontier.MODID + ":" + name)).findFirst().orElse(null);
                    if (template == null || !sitesShot.add(template)) {
                        HollowFrontier.LOGGER.info("[PhotoBooth] {} is {} (already shot, or unknown)", key.identifier(), element);
                        return;
                    }
                    HollowFrontier.LOGGER.info("[PhotoBooth] {}: {} at {} turned {}", key.identifier(), template, piece.getPosition(), piece.getRotation());
                    for (SiteView view : SITE_VIEWS.get(template)) {
                        siteViews.add(view);
                        siteCameras.add(new Vec3[] {local(piece, view.eye()), local(piece, view.target())});
                    }
                });
                siteStep = 2;
                siteWait = 5;
            }
            default -> { // each view: place the camera, wait, shoot
                int view = (siteStep - 2) / 2;
                if (view >= siteViews.size()) {
                    siteIndex++;
                    siteStep = 0;
                    return;
                }
                if (siteStep % 2 == 0) {
                    Vec3[] camera = siteCameras.get(view);
                    hollowLooking(server, camera[0], camera[1]);
                    siteWait = 60;
                } else {
                    shot(mc, String.format("hf_9%d_%s.png", sitesShot.size() - 1, siteViews.get(view).shot()));
                    siteWait = 5;
                }
                siteStep++;
            }
        }
    }

    private static void hollowLooking(MinecraftServer server, Vec3 eye, Vec3 target) {
        Vec3 v = target.subtract(eye);
        float pitch = (float) -Math.toDegrees(Math.atan2(v.y, Math.sqrt(v.x * v.x + v.z * v.z)));
        run(server, String.format(java.util.Locale.ROOT, HOLLOW_TP + "%.2f %.2f %.2f %.1f %.1f", eye.x, eye.y, eye.z, yawOf(v), pitch));
    }

    /** A point in a site's template, where the piece put it in the world. */
    private static Vec3 local(net.minecraft.world.level.levelgen.structure.PoolElementStructurePiece piece, Vec3 local) {
        Vec3 turned = net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplate.transform(local,
                net.minecraft.world.level.block.Mirror.NONE, piece.getRotation(), BlockPos.ZERO);
        return turned.add(piece.getPosition().getX(), piece.getPosition().getY(), piece.getPosition().getZ());
    }

    private static Airship airshipAt(ServerLevel level, Vec3 pos, float yaw, int steam, int fuel) {
        Airship ship = ModEntities.AIRSHIP.get().create(level, EntitySpawnReason.COMMAND);
        ship.snapTo(pos.x, pos.y, pos.z, yaw, 0);
        ship.setSteam(steam);
        ship.setFuel(fuel);
        level.addFreshEntity(ship);
        return ship;
    }

    // ---------------------------------------------------------------- the raid

    // ---------------------------------------------------------------- real towns

    // Where each style's town was found this run (its structure start's chunk), and how it lies once generated.
    private static final java.util.Map<TownStyle, ChunkPos> TOWN_CHUNKS = new java.util.EnumMap<>(TownStyle.class);
    private static final java.util.Map<TownStyle, TownFrame> FRAMES = new java.util.EnumMap<>(TownStyle.class);

    /** A generated town: the middle of its square (standing height), the way out along one of its streets, its pieces. */
    private record TownFrame(Vec3 center, Vec3 dir, StructureStart start) {}

    /**
     * Each style's slot: find the nearest such town to fresh ground (somewhere new every run, since a saved world
     * keeps the towns of earlier runs), forceload it and hang the camera over it while it generates; then shoot it
     * from straight above, at an angle, in its square, and down a street.
     */
    private static void visitTowns(Minecraft mc, MinecraftServer server, int t) {
        TownStyle style = TownStyle.values()[t / TOWN_TICKS];
        String name = style.name().toLowerCase(java.util.Locale.ROOT);
        switch (t % TOWN_TICKS) {
            case 0 -> findTown(server, style);
            case 320 -> frameTown(server, style);
            case 325 -> townView(server, style, 0);
            case 355 -> shot(mc, "hf_17_" + name + "_aerial.png");
            case 360 -> townView(server, style, 1);
            case 390 -> shot(mc, "hf_18_" + name + "_overview.png");
            case 395 -> townView(server, style, 2);
            case 425 -> shot(mc, "hf_19_" + name + "_square.png");
            case 430 -> townView(server, style, 3);
            case 465 -> shot(mc, "hf_20_" + name + "_street.png");
            default -> {}
        }
    }

    private static void findTown(MinecraftServer server, TownStyle style) {
        server.execute(() -> {
            ServerLevel level = server.overworld();
            var town = level.registryAccess().lookupOrThrow(Registries.STRUCTURE).getOrThrow(style.structure());
            int fresh = (int) (System.currentTimeMillis() / 1000 % 97);
            BlockPos from = new BlockPos(baseX + 8000 + fresh * 3000, 0, baseZ + 6000 * (style.ordinal() + 1));
            var found = level.getChunkSource().getGenerator().findNearestMapStructure(level, net.minecraft.core.HolderSet.direct(town), from, 200, false);
            if (found == null) {
                HollowFrontier.LOGGER.warn("[PhotoBooth] no {} town near {}", style, from);
                return;
            }
            ChunkPos chunk = ChunkPos.containing(found.getFirst());
            TOWN_CHUNKS.put(style, chunk);
            int x = chunk.getMinBlockX(), z = chunk.getMinBlockZ();
            run(server, String.format("forceload add %d %d %d %d", x - 112, z - 112, x + 112, z + 112));
            run(server, String.format("tp @p %d 230 %d 0 90", x, z));
            HollowFrontier.LOGGER.info("[PhotoBooth] {} town at chunk {}", style, chunk);
        });
    }

    private static void frameTown(MinecraftServer server, TownStyle style) {
        server.execute(() -> {
            ServerLevel level = server.overworld();
            ChunkPos chunk = TOWN_CHUNKS.get(style);
            if (chunk == null) {
                return;
            }
            var town = level.registryAccess().lookupOrThrow(Registries.STRUCTURE).getOrThrow(style.structure()).value();
            StructureStart start = level.getChunk(chunk.x(), chunk.z()).getStartForStructure(town);
            if (start == null || !start.isValid()) {
                HollowFrontier.LOGGER.warn("[PhotoBooth] the {} town at {} didn't generate", style, chunk);
                return;
            }
            BoundingBox square = start.getPieces().getFirst().getBoundingBox();
            Vec3 center = new Vec3(square.getCenter().getX() + 0.5, square.minY() + 1, square.getCenter().getZ() + 0.5);
            Vec3 dir = start.getPieces().stream().filter(p -> p instanceof net.minecraft.world.level.levelgen.structure.PoolElementStructurePiece piece
                            && piece.getElement().toString().contains("street_")).findFirst()
                    .map(p -> Vec3.atCenterOf(p.getBoundingBox().getCenter()).subtract(center))
                    .map(v -> Math.abs(v.x) > Math.abs(v.z) ? new Vec3(Math.signum(v.x), 0, 0) : new Vec3(0, 0, Math.signum(v.z)))
                    .orElse(new Vec3(0, 0, 1));
            FRAMES.put(style, new TownFrame(center, dir, start));
            HollowFrontier.LOGGER.info("[PhotoBooth] {} town: {} pieces, square at {}, spans {}", style, start.getPieces().size(), center, start.getBoundingBox());
        });
    }

    /** 0 = straight down from high over the square, 1 = at an angle from off a corner, 2 = in the square, 3 = down a street. */
    private static void townView(MinecraftServer server, TownStyle style, int view) {
        server.execute(() -> {
            TownFrame f = FRAMES.get(style);
            if (f == null) {
                return;
            }
            ServerLevel level = server.overworld();
            Vec3 c = f.center();
            Vec3 d = f.dir();
            Vec3 across = new Vec3(-d.z, 0, d.x);
            switch (view) {
                case 0 -> tpLooking(server, c.add(0, 130, 0), c.add(d.scale(0.01)));
                case 1 -> tpLooking(server, c.subtract(d.scale(70)).add(across.scale(45)).add(0, 55, 0), c);
                case 2 -> tpLooking(server, onGround(level, c.add(d.scale(15)).add(across.scale(6))).add(0, 1.5, 0), c.add(0, 4, 0).subtract(d.scale(12)));
                default -> tpLooking(server, onGround(level, c.add(d.scale(TownLayout.MID + 4))).add(0, 1.5, 0), c.add(d.scale(90)).add(0, 3, 0));
            }
        });
    }

    private static Vec3 onGround(ServerLevel level, Vec3 at) {
        return new Vec3(at.x, level.getHeight(net.minecraft.world.level.levelgen.Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, (int) Math.floor(at.x), (int) Math.floor(at.z)), at.z);
    }

    // Puts the camera's feet at eye (near enough) looking at target.
    private static void tpLooking(MinecraftServer server, Vec3 eye, Vec3 target) {
        Vec3 v = target.subtract(eye);
        float yaw = yawOf(v);
        float pitch = (float) -Math.toDegrees(Math.atan2(v.y, Math.sqrt(v.x * v.x + v.z * v.z)));
        run(server, String.format(java.util.Locale.ROOT, "tp @p %.2f %.2f %.2f %.1f %.1f", eye.x, eye.y, eye.z, yaw, pitch));
    }

    // ---------------------------------------------------------------- the raid (in the frontier town)

    /** The raid's street: the middle of the town square, a spot partway down the street the gang comes in by, its way out. */
    private record Street(Vec3 square, Vec3 middle, Vec3 dir) {
        Vec3 across() {
            return new Vec3(-dir.z, 0, dir.x);
        }
    }

    private static TownSite raidSite;

    private static Street street(ServerLevel level) {
        if (raidSite == null) {
            return null;
        }
        Vec3 square = Vec3.atBottomCenterOf(raidSite.rally());
        Vec3 out = Vec3.atBottomCenterOf(raidSite.edge()).subtract(square);
        Vec3 dir = Math.abs(out.x) > Math.abs(out.z) ? new Vec3(Math.signum(out.x), 0, 0) : new Vec3(0, 0, Math.signum(out.z));
        return new Street(square, onGround(level, square.add(dir.scale(TownLayout.MID + 14))), dir);
    }

    private static float yawOf(Vec3 v) {
        return (float) Math.toDegrees(Math.atan2(-v.x, v.z));
    }

    private static long liveRaid = Long.MIN_VALUE;
    private static Vec3 tableau = Vec3.ZERO;
    private static Vec3 dynamiter = Vec3.ZERO;
    private static Vec3 starCat = Vec3.ZERO;

    /** Reads the raid site off the frontier town visited earlier, like any raid (the street the gang takes is random). */
    private static void pickRaidSite(MinecraftServer server) {
        server.execute(() -> {
            ServerLevel level = server.overworld();
            TownFrame f = FRAMES.get(TownStyle.FRONTIER);
            if (f == null) {
                HollowFrontier.LOGGER.warn("[PhotoBooth] no frontier town to raid");
                return;
            }
            raidSite = TownSite.of(level, f.start(), level.getRandom());
            liveRaid = raidSite.key();
            HollowFrontier.LOGGER.info("[PhotoBooth] raid on the town: vault {} (work at {}), bell {}, in by {}", raidSite.vault(), raidSite.workAt(),
                    raidSite.bell(), raidSite.edge());
        });
    }

    /** Starts the real raid on that site (once the camera is in town, so it sees the warning). */
    private static void startLiveRaid(MinecraftServer server) {
        server.execute(() -> {
            if (raidSite != null) {
                ServerLevel level = server.overworld();
                TownRaids.get(level).start(level, raidSite, new RaidTimings(60, 600, 2400, 200), 6, true);
            }
        });
    }

    /** 0 = high over the saloon end looking up the street, 1 = in the street facing the gang, 2 = the staged gang, 3 = the dynamiter side on, 4 = the star cat. */
    private static void raidView(MinecraftServer server, int view) {
        server.execute(() -> {
            Street s = street(server.overworld());
            if (s == null) {
                return;
            }
            Vec3 eye;
            float yaw;
            float pitch;
            switch (view) {
                case 0 -> { eye = s.square().subtract(s.dir().scale(16)).add(0, 20, 0); yaw = yawOf(s.dir()); pitch = 35; }
                case 1 -> { eye = s.square().add(s.dir().scale(12)).add(0, 0.5, 0); yaw = yawOf(s.dir()); pitch = 2; }
                case 2 -> { eye = tableau.subtract(s.dir().scale(9)).add(0, 0.5, 0); yaw = yawOf(s.dir()); pitch = 6; }
                case 3 -> { eye = dynamiter.add(s.across().scale(5)).subtract(s.dir().scale(1)); yaw = yawOf(s.across().scale(-1)); pitch = -8; }
                default -> { eye = starCat.subtract(s.dir().scale(4)).add(s.across().scale(-2)); yaw = yawOf(s.dir().add(s.across().scale(0.5))); pitch = 18; }
            }
            run(server, String.format(java.util.Locale.ROOT, "tp @p %.2f %.2f %.2f %.1f %.1f", eye.x, eye.y, eye.z, yaw, pitch));
        });
    }

    /** Calls off the live raid, clears the street, and stands a frozen gang in it: aiming, mounted, a stick in the air, the star cat watching. */
    private static void stageTableau(MinecraftServer server) {
        server.execute(() -> {
            ServerLevel level = server.overworld();
            Street s = street(level);
            if (s == null) {
                return;
            }
            if (liveRaid != Long.MIN_VALUE) {
                TownRaids.get(level).stop(level, liveRaid);
            }
            AABB area = new AABB(BlockPos.containing(s.middle())).inflate(70, 30, 70);
            level.getEntitiesOfClass(Entity.class, area, e -> e instanceof AbstractOutlaw || e instanceof Lawman || e instanceof Horse
                    || e instanceof net.minecraft.world.entity.item.ItemEntity || e instanceof DynamiteEntity).forEach(Entity::discard);
            Vec3 dir = s.dir();
            Vec3 across = s.across();
            tableau = s.middle().add(dir.scale(2));
            float facing = yawOf(dir.scale(-1)); // toward the saloon end, where the camera is
            mounted(level, ModEntities.OUTLAW_BOSS.get(), tableau.add(dir.scale(4)), facing, new ItemStack(ModItems.SHOTGUN.get()), "Silas 'Rattlesnake' McGraw");
            mounted(level, ModEntities.OUTLAW_GUNSLINGER.get(), tableau.add(dir.scale(5)).add(across.scale(3)), facing, new ItemStack(ModItems.PLAINS_RIFLE.get()), null);
            staged(level, ModEntities.OUTLAW_GUNSLINGER.get(), tableau.add(across.scale(2.5)), facing, new ItemStack(ModItems.PLAINS_RIFLE.get()), GunWielder.GunPose.AIMING);
            staged(level, ModEntities.OUTLAW.get(), tableau.subtract(across.scale(2.5)), facing, new ItemStack(ModItems.REVOLVER.get()), GunWielder.GunPose.AIMING);
            dynamiter = tableau.subtract(dir.scale(1)).add(across.scale(0.8));
            AbstractOutlaw thrower = staged(level, ModEntities.OUTLAW.get(), dynamiter, facing, new ItemStack(ModItems.DYNAMITE.get()), GunWielder.GunPose.THROWING);
            DynamiteEntity stick = new DynamiteEntity(level, thrower, new ItemStack(ModItems.DYNAMITE.get()));
            Vec3 arc = dynamiter.subtract(dir.scale(2.5)).add(0, 3.0, 0);
            stick.setPos(arc.x, arc.y, arc.z);
            stick.setNoGravity(true);
            stick.setDeltaMovement(Vec3.ZERO);
            stick.setRaider(true);
            stick.setFuse(Integer.MAX_VALUE);
            level.addFreshEntity(stick);
            starCat = tableau.subtract(dir.scale(4)).subtract(across.scale(2));
            run(server, String.format(java.util.Locale.ROOT, "summon minecraft:cat %.2f %d %.2f {NoAI:1b,PersistenceRequired:1b,Rotation:[%.1ff,0f],"
                    + "\"neoforge:attachments\":{\"hollowfrontier:town_cat\":{role:\"sheriff\",settled:1b,worn:{id:\"hollowfrontier:brass_star\",count:1}}}}",
                    starCat.x, (int) Math.floor(starCat.y), starCat.z, yawOf(dir)));
        });
    }

    private static AbstractOutlaw staged(ServerLevel level, EntityType<? extends AbstractOutlaw> type, Vec3 at, float yaw, ItemStack hand, GunWielder.GunPose pose) {
        AbstractOutlaw outlaw = type.create(level, EntitySpawnReason.COMMAND);
        outlaw.snapTo(at.x, at.y, at.z, yaw, 0.0F);
        outlaw.setYBodyRot(yaw);
        outlaw.setYHeadRot(yaw);
        outlaw.setNoAi(true);
        outlaw.setPersistenceRequired();
        outlaw.setItemSlot(EquipmentSlot.MAINHAND, hand);
        outlaw.setGunPose(pose);
        level.addFreshEntity(outlaw);
        return outlaw;
    }

    private static AbstractOutlaw mounted(ServerLevel level, EntityType<? extends AbstractOutlaw> type, Vec3 at, float yaw, ItemStack hand, String name) {
        Horse horse = EntityTypes.HORSE.create(level, EntitySpawnReason.COMMAND);
        horse.snapTo(at.x, at.y, at.z, yaw, 0.0F);
        horse.setYBodyRot(yaw);
        horse.setYHeadRot(yaw);
        horse.setNoAi(true);
        horse.setTamed(true);
        horse.setItemSlot(EquipmentSlot.SADDLE, new ItemStack(Items.SADDLE));
        AbstractOutlaw rider = type.create(level, EntitySpawnReason.COMMAND);
        rider.snapTo(at.x, at.y, at.z, yaw, 0.0F);
        rider.setNoAi(true);
        rider.setPersistenceRequired();
        rider.setItemSlot(EquipmentSlot.MAINHAND, hand);
        if (name != null) {
            rider.setCustomName(net.minecraft.network.chat.Component.literal(name));
            rider.setCustomNameVisible(true);
        }
        rider.startRiding(horse, true, false);
        level.addFreshEntityWithPassengers(horse);
        return rider;
    }

    private static void stageRider(MinecraftServer server) {
        // With his AI on, so once unseated he falls and stands like a raider would (a spectator camera is no target).
        server.execute(() -> {
            AbstractOutlaw rider = mounted(server.overworld(), ModEntities.OUTLAW.get(), new Vec3(baseX + RIDER_X + 0.5, Y, baseZ + VAULT_Z + 0.5),
                    0.0F, new ItemStack(ModItems.REVOLVER.get()), null);
            rider.setNoAi(false);
            riderId = rider.getUUID();
        });
    }

    /** A vault on the platform: a stone wall with an iron door (cracking), the strongroom chest behind, a safecracker at work. */
    private static void stageVault(MinecraftServer server) {
        int x = baseX + VAULT_X;
        int z = baseZ + VAULT_Z;
        run(server, String.format("fill %d %d %d %d %d %d minecraft:stone_bricks", x, Y, z, x + 4, Y + 3, z));
        run(server, String.format("fill %d %d %d %d %d %d minecraft:stone_bricks", x, Y + 3, z - 3, x + 4, Y + 3, z));
        run(server, String.format("setblock %d %d %d minecraft:iron_door[half=lower,facing=south]", x + 2, Y, z));
        run(server, String.format("setblock %d %d %d minecraft:iron_door[half=upper,facing=south]", x + 2, Y + 1, z));
        run(server, String.format("setblock %d %d %d minecraft:chest[facing=south]", x + 2, Y, z - 2));
        run(server, String.format("fill %d %d %d %d %d %d minecraft:stone_bricks", x, Y + 3, z + 1, x + 4, Y + 3, z + 1));
        run(server, String.format("setblock %d %d %d minecraft:lantern[hanging=true]", x, Y + 2, z + 1));
        run(server, String.format("setblock %d %d %d minecraft:lantern[hanging=true]", x + 4, Y + 2, z + 1));
        run(server, String.format("summon hollowfrontier:outlaw %d.9 %d %d.6 {NoAI:1b,PersistenceRequired:1b,Rotation:[180f,10f],CustomName:\"Safecracker\","
                + "CustomNameVisible:1b,equipment:{mainhand:{id:\"minecraft:iron_pickaxe\",count:1}}}", x + 2, Y, z + 1));
        vaultFx(server);
    }

    // The crack overlay on the vault door and a spray of sparks (the overlay fades on the client if not refreshed).
    private static void vaultFx(MinecraftServer server) {
        server.execute(() -> {
            ServerLevel level = server.overworld();
            BlockPos door = new BlockPos(baseX + VAULT_X + 2, Y, baseZ + VAULT_Z);
            level.destroyBlockProgress(-77, door, 6);
            level.destroyBlockProgress(-78, door.above(), 6);
            level.sendParticles(net.minecraft.core.particles.ParticleTypes.ELECTRIC_SPARK, door.getX() + 0.5, door.getY() + 0.9, door.getZ() + 1.0, 12, 0.2, 0.3, 0.05, 0.08);
        });
    }

    // Sets the lineup outlaws' arm pose (NoAI mobs never run GunAttackGoal).
    private static void gunPose(MinecraftServer server, GunWielder.GunPose pose) {
        server.execute(() -> server.overworld().getEntitiesOfClass(AbstractOutlaw.class,
                new AABB(baseX - 10, Y - 1, baseZ - 4, baseX + 10, Y + 5, baseZ + 12)).forEach(outlaw -> outlaw.setGunPose(pose)));
    }


    // Views from the hf_cam armor stand (third person, side on) or back from the player's eyes.
    private static void sideCamera(Minecraft mc, boolean on) {
        Entity camera = mc.player;
        if (on) {
            List<ArmorStand> stands = mc.level.getEntitiesOfClass(ArmorStand.class, mc.player.getBoundingBox().inflate(5), ArmorStand::isInvisible);
            if (!stands.isEmpty()) {
                camera = stands.getFirst();
            }
        }
        mc.setCameraEntity(camera);
        if (mc.gui.hud.isHidden() != on) {
            mc.gui.hud.toggle(); // no first-person gun, HUD or name tag over the side shot
        }
    }

    // Empties the gun in the given hotbar slot and starts loading it, so the reload animation plays.
    private static void startReload(MinecraftServer server, int slot) {
        server.execute(() -> {
            ServerPlayer player = server.getPlayerList().getPlayers().getFirst();
            ItemStack stack = player.getInventory().getItem(slot);
            GunItem.setAmmo(stack, 0);
            GunItem.setReloadStart(stack, player.level().getGameTime());
        });
    }

    // ---------------------------------------------------------------- the steam bridge

    private static final int DEPOT_X = 56; // the depot template, east of the rider
    private static final int DEPOT_Z = -18;

    private static Vec3 depot(double x, double y, double z) {
        return new Vec3(baseX + DEPOT_X + x, Y + y, baseZ + DEPOT_Z + z);
    }

    private static void stageDepot(MinecraftServer server) {
        run(server, "time set noon");
        int x = baseX + DEPOT_X, z = baseZ + DEPOT_Z;
        run(server, String.format("fill %d %d %d %d %d %d minecraft:air", x, Y, z, x + 24, Y + 14, z + 15));
        run(server, String.format("place template hollowfrontier:town/depot %d %d %d", x, Y - 1, z));
        // A second handcar on the main line with the engineer aboard (frozen), ready to pull away.
        run(server, String.format("summon hollowfrontier:steam_handcar %d.5 %d %d.5 {Passengers:[{id:\"hollowfrontier:townsfolk\",folk:\"engineer\",NoAI:1b,PersistenceRequired:1b}]}",
                x + 13, Y, z + 13));
    }

    private static List<SteamHandcar> depotHandcars(MinecraftServer server) {
        Vec3 a = depot(0, -1, 0), b = depot(25, 6, 16);
        return server.overworld().getEntitiesOfClass(SteamHandcar.class, new AABB(a, b));
    }

    // The creative screen reopens on the tab it last showed (a private static); set that, then open it.
    private static void creativeTab(Minecraft mc, CreativeModeTab tab) {
        try {
            java.lang.reflect.Field selected = CreativeModeInventoryScreen.class.getDeclaredField("selectedTab");
            selected.setAccessible(true);
            selected.set(null, tab);
        } catch (ReflectiveOperationException e) {
            HollowFrontier.LOGGER.warn("[PhotoBooth] can't pick the creative tab", e);
        }
        mc.gui.setScreen(new CreativeModeInventoryScreen(mc.player, mc.player.connection.enabledFeatures(), false));
    }

    /**
     * Ends the run. Every forceload the scenes added is released first: the photo world is reused, and forced chunks are
     * kept with it and all loaded at the next start, which (before this) had piled up into minutes of "preparing spawn area".
     */
    private static void quit(Minecraft mc, MinecraftServer server) {
        releaseForceloads(server);
        server.execute(() -> mc.execute(mc::stop)); // after the release (queued before it), so it lands before the save
    }

    private static void releaseForceloads(MinecraftServer server) {
        run(server, "execute in minecraft:overworld run forceload remove all");
        run(server, "execute in " + HollowEarth.LEVEL.identifier() + " run forceload remove all");
    }

    private static void run(MinecraftServer server, String command) {
        server.execute(() -> server.getCommands().performPrefixedCommand(server.createCommandSourceStack().withSuppressedOutput(), command));
    }

    private static void tp(MinecraftServer server, int x, int z, float yaw, float pitch) {
        run(server, String.format("tp @p %d.5 %d %d.5 %s %s", x, Y, z, yaw, pitch));
    }

    private static void setup(MinecraftServer server) {
        run(server, "time set noon");
        run(server, "weather clear");
        run(server, String.format("fill %d %d %d %d %d %d minecraft:stone", baseX - 10, Y - 1, baseZ - 20, baseX + 90, Y - 1, baseZ + 12));
        run(server, String.format("fill %d %d %d %d %d %d minecraft:air", baseX - 10, Y, baseZ - 20, baseX + 90, Y + 7, baseZ + 12));
        run(server, "kill @e[type=!player,distance=..120]");
        for (int i = 0; i < MOBS.size(); i++) {
            String gun = switch (MOBS.get(i)) {
                case "outlaw" -> "hollowfrontier:revolver";
                case "outlaw_gunslinger" -> "hollowfrontier:plains_rifle";
                case "outlaw_boss" -> "hollowfrontier:shotgun";
                default -> null;
            };
            String equipment = gun == null ? "" : ",equipment:{mainhand:{id:\"" + gun + "\",count:1}}";
            run(server, String.format("summon hollowfrontier:%s %d %d %d {NoAI:1b,PersistenceRequired:1b%s}",
                    MOBS.get(i), baseX - 3 + i * 2, Y, baseZ + ROW_Z, equipment));
        }
        String[] hostesses = {"ruby", "clementine", "josie", "belle", "lupe"};
        for (int i = 0; i < hostesses.length; i++) {
            run(server, String.format("summon hollowfrontier:barmaid %d %d %d {NoAI:1b,PersistenceRequired:1b,hostess:\"%s\"}",
                    baseX - 4 + i * 2, Y, baseZ + BARMAID_Z, hostesses[i]));
        }
        // Every coat in a row, then the ghost; the accessory cats beside them (settled strays, so nothing rerolls).
        for (int i = 0; i < COATS.size(); i++) {
            cat(server, baseX - 7 + i, COATS.get(i), "role:\"stray\",settled:1b", false);
        }
        cat(server, baseX - 7 + COATS.size(), "minecraft:white", "role:\"ghost\",ghost:1b,settled:1b", false);
        for (int i = 0; i < ACCESSORIES.size() * 2; i++) {
            String coat = COATS.get((i * 3) % COATS.size());
            cat(server, baseX + ACCESSORY_X + i, coat, "role:\"stray\",settled:1b,worn:" + ACCESSORIES.get(i % ACCESSORIES.size()), i >= ACCESSORIES.size());
        }
        // An outlaw behind the star cats: they should spot him, so he glows (checks the GeckoLib outline pass too).
        run(server, String.format("summon hollowfrontier:outlaw %d %d %d {NoAI:1b,PersistenceRequired:1b,Rotation:[0f,0f]}", baseX + ACCESSORY_X + 5, Y, baseZ + CAT_Z - 3));
        // Hotbar shows the item icons; slot 8 stays empty for clean mob shots.
        String[] hotbar = {"revolver", "plains_rifle", "shotgun", "revolver_cartridge", "rifle_cartridge", "percussion_cap", "cowboy_hat", "lasso"};
        for (int i = 0; i < hotbar.length; i++) {
            run(server, "item replace entity @p hotbar." + i + " with hollowfrontier:" + hotbar[i]);
        }
        run(server, "item replace entity @p hotbar.8 with minecraft:air");
        run(server, "item replace entity @p weapon.offhand with minecraft:air");
    }

    // A staged (NoAI) vanilla cat in the cat row with the given coat and town_cat attachment fields.
    private static void cat(MinecraftServer server, int x, String coat, String townCat, boolean sitting) {
        run(server, String.format("summon minecraft:cat %d %d %d {NoAI:1b,PersistenceRequired:1b,Rotation:[0f,0f],Sitting:%db,variant:\"%s\",\"neoforge:attachments\":{\"hollowfrontier:town_cat\":{%s}}}",
                x, Y, baseZ + CAT_Z, sitting ? 1 : 0, coat, townCat));
    }

    /** Block rows, an item-frame wall with every mod item, and an armor stand in the cowboy set (east of the mob row). */
    private static void setupGallery(MinecraftServer server) {
        // Decor/material row, spaced so side faces show (x+20..x+36, z+5)
        String[] decor = {"brass_block", "bounty_board", "bar_counter", "bar_stool"};
        for (int i = 0; i < decor.length; i++) {
            run(server, String.format("setblock %d %d %d hollowfrontier:%s", baseX + 20 + i * 2, Y, baseZ + 5, decor[i]));
        }
        int doorX = baseX + 36;
        run(server, String.format("setblock %d %d %d hollowfrontier:saloon_doors[half=lower,facing=north]", doorX, Y, baseZ + 5));
        run(server, String.format("setblock %d %d %d hollowfrontier:saloon_doors[half=upper,facing=north]", doorX, Y + 1, baseZ + 5));
        // Steam row: boiler -> pipe -> pipe -> tank, crusher, gauge on the tank
        int sx = baseX + 42;
        run(server, String.format("setblock %d %d %d hollowfrontier:boiler", sx, Y, baseZ + 5));
        run(server, String.format("setblock %d %d %d hollowfrontier:copper_pipe[east=true,west=true]", sx + 1, Y, baseZ + 5));
        run(server, String.format("setblock %d %d %d hollowfrontier:copper_pipe[east=true,west=true]", sx + 2, Y, baseZ + 5));
        run(server, String.format("setblock %d %d %d hollowfrontier:steam_tank", sx + 3, Y, baseZ + 5));
        run(server, String.format("setblock %d %d %d hollowfrontier:pressure_gauge", sx + 3, Y + 1, baseZ + 5));
        run(server, String.format("setblock %d %d %d hollowfrontier:crusher", sx + 5, Y, baseZ + 5));
        // Item wall: dark planks at z+9, frames facing north at z+8, 16 per row from the top
        int wx = baseX + 60;
        run(server, String.format("fill %d %d %d %d %d %d minecraft:dark_oak_planks", wx - 1, Y, baseZ + 9, wx + 16, Y + 6, baseZ + 9));
        server.execute(() -> {
            ServerLevel level = server.overworld();
            List<Item> items = BuiltInRegistries.ITEM.stream()
                    .filter(item -> BuiltInRegistries.ITEM.getKey(item).getNamespace().equals(HollowFrontier.MODID))
                    .toList();
            for (int i = 0; i < items.size(); i++) {
                ItemFrame frame = new ItemFrame(level, new BlockPos(wx + i % 16, Y + 5 - i / 16, baseZ + 8), Direction.NORTH);
                frame.setItem(new ItemStack(items.get(i)), false);
                level.addFreshEntity(frame);
            }
            HollowFrontier.LOGGER.info("[PhotoBooth] item wall: {} items", items.size());
        });
        // Armor stand in the full cowboy set holding a revolver
        run(server, String.format("summon minecraft:armor_stand %d %d %d {ShowArms:1b,Rotation:[180f,0f],equipment:{head:{id:\"hollowfrontier:cowboy_hat\",count:1},"
                + "chest:{id:\"hollowfrontier:duster\",count:1},legs:{id:\"hollowfrontier:chaps\",count:1},feet:{id:\"hollowfrontier:cowboy_boots\",count:1},"
                + "mainhand:{id:\"hollowfrontier:revolver\",count:1}}}", baseX + 84, Y, baseZ + 5));
    }

    /** Water + fuel so the boiler renders lit and steam reaches the tank/gauge. */
    private static void fireBoiler(MinecraftServer server) {
        server.execute(() -> {
            BlockPos pos = new BlockPos(baseX + 42, Y, baseZ + 5);
            if (server.overworld().getBlockEntity(pos) instanceof BoilerBlockEntity boiler) {
                boiler.waterTank().add(FluidResource.of(Fluids.WATER), BoilerBlockEntity.WATER_CAPACITY);
                boiler.setItem(0, new ItemStack(Items.COAL_BLOCK));
            }
        });
    }

    // NoAI mobs never turn their bodies, and GeckoLib renders from body rotation, so set it directly (yaw 180 = north).
    private static void faceCamera(MinecraftServer server) {
        server.execute(() -> {
            ServerLevel level = server.overworld();
            for (Mob mob : level.getEntitiesOfClass(Mob.class, new AABB(baseX - 10, Y - 1, baseZ - 4, baseX + 10, Y + 5, baseZ + 12))) {
                mob.setYRot(180.0F);
                mob.setYBodyRot(180.0F);
                mob.setYHeadRot(180.0F);
            }
            for (Mob mob : level.getEntitiesOfClass(Mob.class, new AABB(baseX - 10, Y - 1, baseZ + BARMAID_Z - 1, baseX + 10, Y + 5, baseZ + BARMAID_Z + 1))) {
                mob.setYRot(0.0F); // barmaid row faces south
                mob.setYBodyRot(0.0F);
                mob.setYHeadRot(0.0F);
            }
            for (Mob mob : level.getEntitiesOfClass(Mob.class, new AABB(baseX - 10, Y - 1, baseZ + CAT_Z - 4, baseX + 30, Y + 5, baseZ + CAT_Z + 1))) {
                mob.setYRot(0.0F); // and so does the cat row (and the outlaw behind it)
                mob.setYBodyRot(0.0F);
                mob.setYHeadRot(0.0F);
            }
        });
    }

    private static void shot(Minecraft mc, String name) {
        Screenshot.grab(mc.gameDirectory, name, mc.gameRenderer.mainRenderTarget(), 1,
                message -> HollowFrontier.LOGGER.info("[PhotoBooth] {}", message.getString()));
    }
}
