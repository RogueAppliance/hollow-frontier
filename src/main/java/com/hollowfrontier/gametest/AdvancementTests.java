package com.hollowfrontier.gametest;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

import com.hollowfrontier.HollowFrontier;
import com.hollowfrontier.content.frontier.bounty.BountyData;
import com.hollowfrontier.content.frontier.cat.TownCat;
import com.hollowfrontier.content.frontier.cat.TownCat.Role;
import com.hollowfrontier.content.frontier.gun.GunItem;
import com.hollowfrontier.content.frontier.law.Wanted;
import com.hollowfrontier.content.frontier.music.SaloonPianoBlock;
import com.hollowfrontier.content.steam.SteamHandcar;
import com.hollowfrontier.content.steam.SteamStorageBlockEntity;
import com.hollowfrontier.datagen.ModAdvancements;
import com.hollowfrontier.registry.ModBlocks;
import com.hollowfrontier.registry.ModCriteria;
import com.hollowfrontier.registry.ModDataComponents;
import com.hollowfrontier.registry.ModEntities;
import com.hollowfrontier.registry.ModFluids;
import com.hollowfrontier.registry.ModItems;

import net.minecraft.advancements.AdvancementHolder;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.ServerAdvancementManager;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.animal.equine.Horse;
import net.minecraft.world.entity.animal.feline.Cat;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.Vec3;

// The advancement tabs: every advancement and parent is there, and the mod's own triggers fire from where things happen.
final class AdvancementTests {
    private AdvancementTests() {}

    static boolean earned(ServerPlayer player, String name) {
        AdvancementHolder advancement = player.level().getServer().getAdvancements().get(HollowFrontier.id(name));
        return advancement != null && player.getAdvancements().getOrStartProgress(advancement).isDone();
    }

    /** Every advancement in ModAdvancements loaded (so its criteria parsed), with a parent in its own tab, roots excepted. */
    static void advancementsLoad(GameTestHelper h) {
        ServerAdvancementManager manager = h.getLevel().getServer().getAdvancements();
        for (String name : ModAdvancements.TEXT.keySet()) {
            AdvancementHolder advancement = manager.get(HollowFrontier.id(name));
            h.assertTrue(advancement != null, "advancement didn't load: " + name);
            String tab = name.substring(0, name.indexOf('/'));
            if (name.endsWith("/root")) {
                h.assertTrue(advancement.value().parent().isEmpty() && advancement.value().display().orElseThrow().background().isPresent(),
                        "root " + name + " should have no parent and a background");
            } else {
                var parent = advancement.value().parent();
                h.assertTrue(parent.isPresent() && manager.get(parent.get()) != null, name + " has a missing parent");
                h.assertTrue(parent.get().getPath().startsWith(tab + "/"), name + "'s parent is in another tab: " + parent.get());
            }
            h.assertTrue(!advancement.value().criteria().isEmpty() && advancement.value().display().isPresent(), name + " has no criteria or display");
        }
        // Every trigger the mod registers is used by at least one advancement.
        Set<String> used = new HashSet<>();
        for (String name : ModAdvancements.TEXT.keySet()) {
            manager.get(HollowFrontier.id(name)).value().criteria().values()
                    .forEach(c -> used.add(String.valueOf(BuiltInRegistries.TRIGGER_TYPES.getKey(c.trigger()))));
        }
        for (var trigger : ModCriteria.TRIGGERS.getEntries()) {
            h.assertTrue(used.contains(trigger.getId().toString()), "trigger used by no advancement: " + trigger.getId());
        }
        h.succeed();
    }

    /** Guns, horseshoes, the Bounty Board, the wanted level and the fine, a blackout, cats: each trigger from its code path. */
    static void frontierTriggersGrant(GameTestHelper h) {
        ServerPlayer player = TestPlayers.survival(h, new BlockPos(2, 1, 2));

        // Load all six chambers of a revolver.
        ItemStack gun = new ItemStack(ModItems.REVOLVER.get());
        player.setItemInHand(InteractionHand.MAIN_HAND, gun);
        player.getInventory().add(new ItemStack(ModItems.REVOLVER_CARTRIDGE.get(), 6));
        player.getInventory().add(new ItemStack(ModItems.PERCUSSION_CAP.get(), 6));
        GunItem item = (GunItem) gun.getItem();
        item.startReload(player, gun);
        for (int i = 0; i < 6; i++) {
            GunItem.setReloadStart(gun, h.getLevel().getGameTime() - item.stats().loadTicks());
            item.tickReload(player, gun);
        }
        h.assertTrue(earned(player, "frontier/load_gun") && earned(player, "frontier/six_for_six"), "loading a revolver earned nothing");

        // Shoe a horse.
        Horse horse = h.spawnWithNoFreeWill(EntityTypes.HORSE, new BlockPos(4, 1, 4));
        horse.setTamed(true);
        player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(ModItems.IRON_HORSESHOES.get()));
        player.interactOn(horse, InteractionHand.MAIN_HAND, Vec3.ZERO);
        h.assertTrue(earned(player, "frontier/well_heeled"), "shoeing a horse earned nothing");

        // Turn in a badge.
        BlockPos board = new BlockPos(8, 1, 2);
        h.setBlock(board, ModBlocks.BOUNTY_BOARD.get());
        ItemStack badge = new ItemStack(ModItems.OUTLAW_BADGE.get());
        badge.set(ModDataComponents.BOUNTY.get(), new BountyData(UUID.randomUUID(), "Test Pete", 0, 0, 3, false));
        player.setItemInHand(InteractionHand.MAIN_HAND, badge);
        h.useBlock(board, player);
        h.assertTrue(earned(player, "frontier/dead_or_alive"), "a bounty claim earned nothing");

        // Run up three stars, then pay the fine.
        Wanted.addHeat(player, 1);
        h.assertTrue(earned(player, "frontier/reputation") && !earned(player, "frontier/most_wanted"), "one star: wrong advancements");
        Wanted.addHeat(player, 8);
        h.assertTrue(earned(player, "frontier/most_wanted"), "three stars earned nothing");
        player.setItemInHand(InteractionHand.MAIN_HAND, ItemStack.EMPTY);
        player.getInventory().add(new ItemStack(ModItems.GOLD_COIN.get(), 9));
        h.useBlock(board, player);
        h.assertTrue(Wanted.stars(player) == 0 && earned(player, "frontier/clean_slate"), "paying the fine earned nothing");

        // One drink too many.
        for (int i = 0; i < 2; i++) {
            ItemStack moonshine = new ItemStack(ModItems.MOONSHINE.get());
            moonshine.getItem().finishUsingItem(moonshine, h.getLevel(), player);
        }
        h.assertTrue(earned(player, "frontier/lights_out"), "a blackout earned nothing");
        h.assertTrue(player.getInventory().countItem(ModItems.COWBOY_COFFEE.get()) == 1, "Lights Out should come with a coffee");
        h.succeed();
    }

    /** Dressing your cat, and the ghost cat's supper-and-a-song ritual. */
    static void catTriggersGrant(GameTestHelper h) {
        ServerPlayer player = TestPlayers.survival(h, new BlockPos(5, 1, 3));
        Cat cat = h.spawnWithNoFreeWill(EntityTypes.CAT, new BlockPos(3, 1, 5));
        cat.tame(player);
        player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(ModItems.BANDANA.get()));
        player.interactOn(cat, InteractionHand.MAIN_HAND, cat.position());
        h.assertTrue(earned(player, "frontier/dressed_to_the_nines"), "dressing a cat earned nothing");

        Cat ghost = h.spawnWithNoFreeWill(EntityTypes.CAT, new BlockPos(7, 1, 5));
        TownCat.set(ghost, new TownCat(Role.GHOST, ItemStack.EMPTY, true, true, false));
        h.setBlock(new BlockPos(9, 1, 9), ModBlocks.SALOON_PIANO.get().defaultBlockState().setValue(SaloonPianoBlock.PLAYING, true));
        player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(Items.COOKED_COD, 64));
        for (int i = 0; i < 60 && !ghost.isTame(); i++) {
            player.interactOn(ghost, InteractionHand.MAIN_HAND, ghost.position());
        }
        h.assertTrue(ghost.isTame() && earned(player, "frontier/nine_lives"), "taming the ghost cat earned nothing");
        h.succeed();
    }

    /** Filling a canister from a tank, and filling a handcar to the brim at a standpipe while aboard. */
    static void steamTriggersGrant(GameTestHelper h) {
        BlockPos tankPos = new BlockPos(6, 1, 6);
        h.setBlock(tankPos, ModBlocks.STEAM_TANK.get());
        SteamStorageBlockEntity tank = h.getBlockEntity(tankPos, SteamStorageBlockEntity.class);
        tank.tank().add(ModFluids.steam(), 16_000);
        ServerPlayer player = TestPlayers.survival(h, new BlockPos(6, 1, 3));
        player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(ModItems.STEAM_CANISTER.get()));
        h.useBlock(tankPos, player);
        h.assertTrue(earned(player, "steam/canned_steam"), "filling a canister earned nothing");

        h.setBlock(new BlockPos(6, 1, 5), Blocks.RAIL);
        SteamHandcar car = handcarAt(h, new BlockPos(6, 1, 5));
        player.setItemInHand(InteractionHand.MAIN_HAND, ItemStack.EMPTY);
        player.startRiding(car, true, false);
        h.succeedWhen(() -> {
            h.assertTrue(earned(player, "steam/water_stop"), "a full fill-up at the standpipe earned nothing (handcar has " + car.steam() + ")");
            player.stopRiding();
            car.discard();
        });
    }

    private static SteamHandcar handcarAt(GameTestHelper h, BlockPos pos) {
        SteamHandcar car = ModEntities.STEAM_HANDCAR.get().create(h.getLevel(), EntitySpawnReason.COMMAND);
        Vec3 at = Vec3.atBottomCenterOf(h.absolutePos(pos));
        car.snapTo(at.x, at.y, at.z, 0, 0);
        h.getLevel().addFreshEntity(car);
        return car;
    }
}
