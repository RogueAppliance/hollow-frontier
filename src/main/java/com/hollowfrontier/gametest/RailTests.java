package com.hollowfrontier.gametest;

import java.util.List;

import com.hollowfrontier.HollowFrontier;
import com.hollowfrontier.content.frontier.townsfolk.Folk;
import com.hollowfrontier.content.frontier.townsfolk.Townsfolk;
import com.hollowfrontier.content.steam.BoilerBlockEntity;
import com.hollowfrontier.content.steam.SteamCanisterItem;
import com.hollowfrontier.content.steam.SteamHandcar;
import com.hollowfrontier.content.steam.SteamStorageBlockEntity;
import com.hollowfrontier.registry.ModBlocks;
import com.hollowfrontier.registry.ModCriteria;
import com.hollowfrontier.registry.ModEntities;
import com.hollowfrontier.registry.ModFluids;
import com.hollowfrontier.registry.ModItems;

import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.player.Input;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.trading.MerchantOffer;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.RailBlock;
import net.minecraft.world.level.block.state.properties.RailShape;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructurePlaceSettings;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplate;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

// The steam bridge: the steam handcar, the depot's standpipe and its railroad engineer.
final class RailTests {
    private RailTests() {}

    private static void rail(GameTestHelper h, int z) {
        for (int x = 0; x < 12; x++) {
            h.setBlock(new BlockPos(x, 1, z), Blocks.RAIL.defaultBlockState().setValue(RailBlock.SHAPE, RailShape.EAST_WEST));
        }
    }

    private static SteamHandcar handcar(GameTestHelper h, BlockPos pos, int steam) {
        SteamHandcar car = ModEntities.STEAM_HANDCAR.get().create(h.getLevel(), EntitySpawnReason.COMMAND);
        Vec3 at = Vec3.atBottomCenterOf(h.absolutePos(pos));
        car.snapTo(at.x, at.y, at.z, 0, 0);
        car.setSteam(steam);
        h.getLevel().addFreshEntity(car);
        return car;
    }

    /**
     * With the throttle open a handcar burns steam and runs faster than a minecart can (0.4 blocks a tick); with an empty
     * tank the throttle does nothing.
     */
    static void handcarBurnsSteamAndMoves(GameTestHelper h) {
        rail(h, 3);
        rail(h, 8);
        SteamHandcar car = handcar(h, new BlockPos(0, 1, 3), 2000);
        car.setDeltaMovement(0.55, 0, 0); // a minecart going this fast still only covers 0.4 blocks a tick
        car.setThrottle(1);
        SteamHandcar dry = handcar(h, new BlockPos(5, 1, 8), 0);
        dry.setThrottle(1);
        double[] mark = new double[1];
        h.runAfterDelay(6, () -> mark[0] = car.getX());
        h.runAfterDelay(10, () -> {
            double perTick = (car.getX() - mark[0]) / 4;
            h.assertTrue(perTick > 0.45, "handcar no faster than a minecart: " + perTick + " blocks/tick");
            h.assertTrue(car.steam() < 2000 && car.steam() >= 2000 - 10 * SteamHandcar.STEAM_PER_TICK, "steam burned: " + car.steam());
            h.assertTrue(car.isRunning(), "engine not running under throttle");
            h.assertTrue(dry.getDeltaMovement().horizontalDistance() < 1.0E-3 && !dry.isRunning(), "a dry handcar moved on its own");
            car.discard();
            dry.discard();
            h.succeed();
        });
    }

    /** Stopped next to a steam tank, a handcar draws steam through the transfer API (the depot standpipe). */
    static void handcarFillsAtStandpipe(GameTestHelper h) {
        rail(h, 5);
        h.setBlock(new BlockPos(6, 1, 6), ModBlocks.STEAM_TANK.get());
        SteamStorageBlockEntity tank = h.getBlockEntity(new BlockPos(6, 1, 6), SteamStorageBlockEntity.class);
        tank.tank().add(ModFluids.steam(), 10_000);
        SteamHandcar car = handcar(h, new BlockPos(6, 1, 5), 0);
        h.succeedWhen(() -> {
            h.assertTrue(car.steam() >= SteamHandcar.CAPACITY, "handcar only took " + car.steam());
            h.assertTrue(tank.tank().amount() == 10_000 - SteamHandcar.CAPACITY, "tank holds " + tank.tank().amount());
            car.discard();
        });
    }

    /**
     * A rider fills up from a canister, opens the throttle and blows the whistle (their client input), and it all goes
     * through the rider's real input path.
     */
    static void riderDrivesHandcar(GameTestHelper h) {
        rail(h, 5);
        SteamHandcar car = handcar(h, new BlockPos(1, 1, 5), 0);
        ServerPlayer player = TestPlayers.survival(h, new BlockPos(1, 1, 4));
        ItemStack canister = new ItemStack(ModItems.STEAM_CANISTER.get());
        SteamCanisterItem.setSteam(canister, SteamCanisterItem.CAPACITY);
        player.setItemInHand(InteractionHand.MAIN_HAND, canister);
        player.interactOn(car, InteractionHand.MAIN_HAND, car.position());
        h.assertTrue(car.steam() == SteamCanisterItem.CAPACITY && SteamCanisterItem.steam(canister) == 0, "canister not poured: " + car.steam());
        player.setItemInHand(InteractionHand.MAIN_HAND, ItemStack.EMPTY);
        h.assertTrue(player.startRiding(car, true, false), "couldn't board");
        player.setYRot(-90); // facing east, along the rail
        player.setLastClientInput(new Input(true, false, false, false, true, false, false));
        h.runAfterDelay(15, () -> {
            h.assertTrue(car.getX() > h.absolutePos(new BlockPos(2, 1, 5)).getX(), "handcar didn't pull away: " + car.position());
            h.assertTrue(car.steam() < SteamCanisterItem.CAPACITY - SteamHandcar.WHISTLE_STEAM, "no steam used: " + car.steam());
            h.assertTrue(AdvancementTests.earned(player, "steam/toot_toot"), "whistle didn't earn Toot Toot");
            // The ride is reported to the long-haul trigger as it goes; a short hop isn't a thousand blocks.
            h.assertFalse(AdvancementTests.earned(player, "steam/long_haul"), "Transcontinental for a few blocks");
            ModCriteria.HANDCAR_DRIVEN.get().trigger(player, 1000);
            h.assertTrue(AdvancementTests.earned(player, "steam/long_haul"), "a thousand blocks didn't earn Transcontinental");
            player.stopRiding();
            car.discard();
            h.succeed();
        });
    }

    /** A boiler with a water source block touching it fills itself (the depot's well). */
    static void boilerDrawsFromWell(GameTestHelper h) {
        h.setBlock(new BlockPos(3, 0, 3), Blocks.WATER);
        h.setBlock(new BlockPos(3, 1, 3), ModBlocks.BOILER.get());
        BoilerBlockEntity boiler = h.getBlockEntity(new BlockPos(3, 1, 3), BoilerBlockEntity.class);
        h.setBlock(new BlockPos(8, 1, 8), ModBlocks.BOILER.get());
        BoilerBlockEntity dry = h.getBlockEntity(new BlockPos(8, 1, 8), BoilerBlockEntity.class);
        h.succeedWhen(() -> {
            h.assertTrue(boiler.waterTank().amount() >= 10 * BoilerBlockEntity.WATER_INTAKE, "well gave " + boiler.waterTank().amount());
            h.assertTrue(dry.waterTank().amount() == 0, "a boiler with no water near it filled up");
        });
    }

    /** The engineer sells rail, steam and a valve, buys crusher dust, and never turns up at random. */
    static void engineerTrades(GameTestHelper h) {
        Townsfolk engineer = h.spawnWithNoFreeWill(ModEntities.TOWNSFOLK.get(), new BlockPos(5, 1, 5));
        engineer.setFolk(Folk.ENGINEER);
        List<MerchantOffer> offers = engineer.getOffers();
        h.assertTrue(offers.stream().anyMatch(o -> o.getResult().is(Items.RAIL)), "no rail for sale");
        h.assertTrue(offers.stream().anyMatch(o -> o.getResult().is(ModItems.STEAM_CANISTER.get())
                && SteamCanisterItem.steam(o.getResult()) == SteamCanisterItem.CAPACITY), "no full canister for sale");
        h.assertTrue(offers.stream().anyMatch(o -> o.getBaseCostA().is(ModItems.IRON_DUST.get())), "doesn't buy iron dust");
        h.assertTrue(offers.stream().anyMatch(o -> o.getResult().is(ModItems.PRESSURE_VALVE.get())), "no pressure valve for sale");
        h.assertFalse(List.of(Folk.RANDOM).contains(Folk.ENGINEER), "engineers can be rolled at random");
        // Buying rails goes through the vanilla trade trigger and earns Laying Track.
        ServerPlayer player = TestPlayers.survival(h, new BlockPos(5, 1, 3));
        MerchantOffer rails = offers.stream().filter(o -> o.getResult().is(Items.RAIL)).findFirst().orElseThrow();
        engineer.setTradingPlayer(player);
        engineer.notifyTrade(rails);
        engineer.setTradingPlayer(null);
        h.assertTrue(AdvancementTests.earned(player, "steam/laying_track"), "buying rails didn't earn Laying Track");
        // And the handcar recipe is there.
        h.assertTrue(h.getLevel().getServer().getRecipeManager().byKey(ResourceKey.create(Registries.RECIPE, HollowFrontier.id("steam_handcar"))).isPresent(),
                "no handcar recipe");
        h.succeed();
    }

    /** The depot comes with the engineer, a handcar on the siding and a working standpipe: a boiler with coal over a well. */
    static void depotHasStandpipe(GameTestHelper h) {
        ServerLevel level = h.getLevel();
        StructureTemplate depot = level.getStructureTemplateManager().get(HollowFrontier.id("town/depot")).orElseThrow();
        BlockPos origin = h.absolutePos(new BlockPos(0, 80, 0));
        depot.placeInWorld(level, origin, origin, new StructurePlaceSettings(), level.getRandom(), Block.UPDATE_CLIENTS);
        AABB area = new AABB(origin).inflate(32);
        try {
            h.assertTrue(level.getBlockEntity(origin.offset(22, 1, 12)) instanceof BoilerBlockEntity boiler && boiler.getItem(0).is(Items.COAL),
                    "no boiler with coal at the standpipe");
            h.assertTrue(level.getBlockState(origin.offset(22, 0, 12)).is(Blocks.WATER), "no well under the boiler");
            h.assertTrue(level.getBlockState(origin.offset(21, 1, 12)).is(ModBlocks.STEAM_TANK.get()), "no steam tank by the rail");
            h.assertTrue(level.getBlockState(origin.offset(21, 1, 13)).is(Blocks.RAIL), "the standpipe isn't beside the track");
            h.assertTrue(!level.getEntitiesOfClass(SteamHandcar.class, area).isEmpty(), "no handcar on the siding");
            h.assertTrue(level.getEntitiesOfClass(Townsfolk.class, area, t -> t.getFolk() == Folk.ENGINEER).size() == 1, "no railroad engineer");
        } finally {
            level.getEntitiesOfClass(Mob.class, area).forEach(Mob::discard);
            level.getEntitiesOfClass(SteamHandcar.class, area).forEach(Entity::discard);
        }
        h.succeed();
    }
}
