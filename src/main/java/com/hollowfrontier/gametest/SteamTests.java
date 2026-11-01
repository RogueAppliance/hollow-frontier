package com.hollowfrontier.gametest;

import com.hollowfrontier.content.steam.BoilerBlock;
import com.hollowfrontier.content.steam.BoilerBlockEntity;
import com.hollowfrontier.content.steam.CopperPipeBlock;
import com.hollowfrontier.content.steam.CrusherBlockEntity;
import com.hollowfrontier.content.steam.PressureGaugeBlockEntity;
import com.hollowfrontier.content.steam.SteamCanisterItem;
import com.hollowfrontier.content.steam.SteamStorageBlockEntity;
import com.hollowfrontier.registry.ModBlocks;
import com.hollowfrontier.registry.ModFluids;
import com.hollowfrontier.registry.ModItems;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.transfer.fluid.FluidResource;

// Phase 7: boiler, pipes, tank, gauge, crusher, canister, drill.
final class SteamTests {
    private SteamTests() {}

    static void boilerBoilsWater(GameTestHelper h) {
        BlockPos pos = new BlockPos(2, 1, 2);
        h.setBlock(pos, ModBlocks.BOILER.get());
        BoilerBlockEntity boiler = h.getBlockEntity(pos, BoilerBlockEntity.class);
        boiler.waterTank().add(FluidResource.of(Fluids.WATER), 4000);
        boiler.setItem(0, new ItemStack(Items.COAL));
        h.succeedWhen(() -> {
            h.assertTrue(boiler.steamTank().amount() > 0, "no steam yet");
            h.assertTrue(boiler.waterTank().amount() < 4000, "water not consumed");
            h.assertTrue(h.getLevel().getBlockState(h.absolutePos(pos)).getValue(BoilerBlock.LIT), "boiler not lit");
            h.assertTrue(boiler.getItem(0).isEmpty(), "coal not burned");
        });
    }

    static void pipesCarrySteamToTank(GameTestHelper h) {
        h.setBlock(new BlockPos(1, 1, 2), ModBlocks.BOILER.get());
        for (int x = 2; x <= 4; x++) {
            h.setBlock(new BlockPos(x, 1, 2), ModBlocks.COPPER_PIPE.get());
        }
        h.setBlock(new BlockPos(5, 1, 2), ModBlocks.STEAM_TANK.get());
        BoilerBlockEntity boiler = h.getBlockEntity(new BlockPos(1, 1, 2), BoilerBlockEntity.class);
        boiler.waterTank().add(FluidResource.of(Fluids.WATER), BoilerBlockEntity.WATER_CAPACITY);
        boiler.setItem(0, new ItemStack(Items.COAL_BLOCK));
        h.assertTrue(h.getLevel().getBlockState(h.absolutePos(new BlockPos(2, 1, 2))).getValue(CopperPipeBlock.EAST), "pipe did not connect to the next pipe");
        SteamStorageBlockEntity tank = h.getBlockEntity(new BlockPos(5, 1, 2), SteamStorageBlockEntity.class);
        h.succeedWhen(() -> h.assertTrue(tank.tank().amount() > 0, "steam has not reached the tank"));
    }

    static void crusherCrushesRawIron(GameTestHelper h) {
        BlockPos pos = new BlockPos(2, 1, 2);
        h.setBlock(pos, ModBlocks.CRUSHER.get());
        CrusherBlockEntity crusher = h.getBlockEntity(pos, CrusherBlockEntity.class);
        crusher.steamTank().add(ModFluids.steam(), CrusherBlockEntity.STEAM_CAPACITY);
        crusher.setItem(0, new ItemStack(Items.RAW_IRON));
        h.succeedWhen(() -> {
            ItemStack out = crusher.getItem(1);
            h.assertTrue(out.is(ModItems.IRON_DUST.get()) && out.getCount() == 2, "expected 2 iron dust, got " + out);
            h.assertTrue(crusher.getItem(0).isEmpty(), "raw iron not consumed");
            h.assertTrue(crusher.steamTank().amount() < CrusherBlockEntity.STEAM_CAPACITY, "no steam used");
        });
    }

    static void crusherIdlesWithoutSteam(GameTestHelper h) {
        BlockPos pos = new BlockPos(2, 1, 2);
        h.setBlock(pos, ModBlocks.CRUSHER.get());
        CrusherBlockEntity crusher = h.getBlockEntity(pos, CrusherBlockEntity.class);
        crusher.setItem(0, new ItemStack(Items.RAW_IRON));
        h.runAfterDelay(120, () -> {
            h.assertTrue(crusher.getItem(1).isEmpty() && crusher.getItem(0).is(Items.RAW_IRON), "crusher worked without steam");
            h.succeed();
        });
    }

    static void gaugeReadsPressure(GameTestHelper h) {
        h.setBlock(new BlockPos(2, 1, 2), ModBlocks.STEAM_TANK.get());
        h.setBlock(new BlockPos(3, 1, 2), ModBlocks.PRESSURE_GAUGE.get());
        SteamStorageBlockEntity tank = h.getBlockEntity(new BlockPos(2, 1, 2), SteamStorageBlockEntity.class);
        tank.tank().add(ModFluids.steam(), SteamStorageBlockEntity.TANK_CAPACITY / 2);
        PressureGaugeBlockEntity gauge = h.getBlockEntity(new BlockPos(3, 1, 2), PressureGaugeBlockEntity.class);
        h.succeedWhen(() -> h.assertTrue(gauge.signal() == 8, "half-full tank should read 8, got " + gauge.signal()));
    }

    // Neighbour lookups are cached: a tank set down beside a ticking one, and then taken away, must still be noticed.
    static void steamFindsNewNeighbour(GameTestHelper h) {
        BlockPos full = new BlockPos(2, 1, 2);
        BlockPos beside = new BlockPos(3, 1, 2);
        h.setBlock(full, ModBlocks.STEAM_TANK.get());
        SteamStorageBlockEntity source = h.getBlockEntity(full, SteamStorageBlockEntity.class);
        source.tank().add(ModFluids.steam(), SteamStorageBlockEntity.TANK_CAPACITY);
        h.runAfterDelay(10, () -> {
            h.assertTrue(source.tank().amount() == SteamStorageBlockEntity.TANK_CAPACITY, "steam went somewhere with nothing beside it");
            h.setBlock(beside, ModBlocks.STEAM_TANK.get());
            SteamStorageBlockEntity first = h.getBlockEntity(beside, SteamStorageBlockEntity.class);
            h.runAfterDelay(10, () -> {
                h.assertTrue(first.tank().amount() > 0, "steam did not reach a tank placed after the first tick");
                h.setBlock(beside, Blocks.AIR);
                int left = source.tank().amount();
                h.runAfterDelay(10, () -> {
                    h.assertTrue(source.tank().amount() == left, "steam still flowing into a removed tank");
                    h.setBlock(beside, ModBlocks.STEAM_TANK.get());
                    SteamStorageBlockEntity second = h.getBlockEntity(beside, SteamStorageBlockEntity.class);
                    h.succeedWhen(() -> h.assertTrue(second.tank().amount() > 0, "steam did not reach the replacement tank"));
                });
            });
        });
    }

    static void canisterFillsAndDrains(GameTestHelper h) {
        BlockPos pos = new BlockPos(2, 1, 2);
        h.setBlock(pos, ModBlocks.STEAM_TANK.get());
        SteamStorageBlockEntity tank = h.getBlockEntity(pos, SteamStorageBlockEntity.class);
        tank.tank().add(ModFluids.steam(), 5000);
        ServerPlayer player = TestPlayers.survival(h, new BlockPos(2, 1, 4));
        ItemStack canister = new ItemStack(ModItems.STEAM_CANISTER.get());
        player.setItemInHand(InteractionHand.MAIN_HAND, canister);
        BlockPos abs = h.absolutePos(pos);
        canister.getItem().useOn(new UseOnContext(player, InteractionHand.MAIN_HAND, new BlockHitResult(Vec3.atCenterOf(abs), Direction.UP, abs, false)));
        h.assertTrue(SteamCanisterItem.steam(canister) == SteamCanisterItem.CAPACITY, "canister not filled: " + SteamCanisterItem.steam(canister));
        h.assertTrue(tank.tank().amount() == 1000, "tank not drained by the fill: " + tank.tank().amount());
        h.assertTrue(SteamCanisterItem.drawFromInventory(player, 25) == 25, "drill draw failed");
        h.assertTrue(SteamCanisterItem.steam(canister) == SteamCanisterItem.CAPACITY - 25, "canister not drained by the draw");
        h.succeed();
    }

    static void steamDrillToolRules(GameTestHelper h) {
        ItemStack drill = new ItemStack(ModItems.STEAM_DRILL.get());
        h.assertTrue(drill.getDestroySpeed(Blocks.STONE.defaultBlockState()) == 9.0F, "drill stone speed " + drill.getDestroySpeed(Blocks.STONE.defaultBlockState()));
        h.assertTrue(drill.getDestroySpeed(Blocks.DIRT.defaultBlockState()) == 9.0F, "drill dirt speed " + drill.getDestroySpeed(Blocks.DIRT.defaultBlockState()));
        h.assertTrue(drill.isCorrectToolForDrops(Blocks.DIAMOND_ORE.defaultBlockState()), "drill can't drop diamond ore");
        h.assertTrue(!drill.isCorrectToolForDrops(Blocks.OBSIDIAN.defaultBlockState()), "drill should not mine obsidian (iron tier)");
        h.assertTrue(drill.getMaxDamage() == 750, "drill durability " + drill.getMaxDamage());
        h.succeed();
    }
}
