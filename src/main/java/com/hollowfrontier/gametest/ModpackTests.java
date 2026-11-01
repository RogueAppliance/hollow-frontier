package com.hollowfrontier.gametest;

import com.hollowfrontier.HollowFrontier;
import com.hollowfrontier.content.frontier.mystery.MysteryConfig;
import com.hollowfrontier.content.frontier.mystery.MysteryLog;
import com.hollowfrontier.content.steam.BoilerBlockEntity;
import com.hollowfrontier.content.steam.CrusherBlockEntity;
import com.hollowfrontier.content.steam.FluidTank;
import com.hollowfrontier.content.steam.SteamStorageBlockEntity;
import com.hollowfrontier.registry.ModBlocks;
import com.hollowfrontier.registry.ModEntities;
import com.hollowfrontier.registry.ModFluids;
import com.hollowfrontier.registry.ModItems;
import com.hollowfrontier.registry.ModTags;
import com.hollowfrontier.worldgen.structure.ModStructures;
import com.hollowfrontier.worldgen.structure.TownStyle;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.SingleItemRecipe;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.Biomes;
import net.minecraft.world.level.material.Fluids;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.common.Tags;
import net.neoforged.neoforge.transfer.ResourceHandler;
import net.neoforged.neoforge.transfer.fluid.FluidResource;
import net.neoforged.neoforge.transfer.item.ItemResource;
import net.neoforged.neoforge.transfer.transaction.Transaction;

// Phase 30: what other mods and pack makers lean on.
final class ModpackTests {
    private ModpackTests() {}

    private static ResourceHandler<ItemResource> items(GameTestHelper h, BlockPos pos, Direction side) {
        ResourceHandler<ItemResource> handler = h.getLevel().getCapability(Capabilities.Item.BLOCK, h.absolutePos(pos), side);
        h.assertTrue(handler != null, "no item capability at " + pos + " on " + side);
        return handler;
    }

    /** Other mods' pipes reach the Boiler's and Crusher's slots by capability, with the faces hoppers get. */
    static void machinesExposeTheirSlots(GameTestHelper h) {
        BlockPos boilerPos = new BlockPos(1, 1, 2);
        BlockPos crusherPos = new BlockPos(3, 1, 2);
        h.setBlock(boilerPos, ModBlocks.BOILER.get());
        h.setBlock(crusherPos, ModBlocks.CRUSHER.get());
        BoilerBlockEntity boiler = h.getBlockEntity(boilerPos, BoilerBlockEntity.class);
        CrusherBlockEntity crusher = h.getBlockEntity(crusherPos, CrusherBlockEntity.class);
        try (Transaction tx = Transaction.openRoot()) {
            h.assertTrue(items(h, boilerPos, Direction.NORTH).insert(ItemResource.of(Items.DIRT), 1, tx) == 0, "the boiler took something that doesn't burn");
            h.assertTrue(items(h, boilerPos, Direction.UP).insert(ItemResource.of(Items.COAL), 4, tx) == 4, "the boiler took no coal");
            h.assertTrue(items(h, crusherPos, Direction.UP).insert(ItemResource.of(Items.RAW_IRON), 2, tx) == 2, "the crusher took no raw iron");
            tx.commit();
        }
        h.assertTrue(boiler.getItem(0).is(Items.COAL) && boiler.getItem(0).getCount() == 4, "coal not in the boiler: " + boiler.getItem(0));
        h.assertTrue(crusher.getItem(0).is(Items.RAW_IRON), "raw iron not in the crusher: " + crusher.getItem(0));
        crusher.setItem(1, new ItemStack(ModItems.IRON_DUST.get(), 3));
        try (Transaction tx = Transaction.openRoot()) {
            h.assertTrue(items(h, crusherPos, Direction.DOWN).extract(ItemResource.of(ModItems.IRON_DUST.get()), 3, tx) == 3, "no dust out of the bottom");
            h.assertTrue(items(h, crusherPos, Direction.UP).extract(ItemResource.of(Items.RAW_IRON), 1, tx) == 0, "ore came out of the top");
            tx.commit();
        }
        h.assertTrue(crusher.getItem(1).isEmpty(), "dust left in the crusher");
        h.succeed();
    }

    /**
     * Any c:steam fluid goes into our tanks as our steam, and our pulls draw it from tanks that keep it as it is. Our own
     * flowing steam stands in for another mod's: it's in c:steam but isn't the fluid our machines run on.
     */
    static void anySteamWorks(GameTestHelper h) {
        h.assertTrue(ModFluids.STEAM.get().is(ModTags.Fluids.STEAM) && ModFluids.STEAM.get().is(Tags.Fluids.GASEOUS), "our steam isn't c:steam and gaseous");
        FluidResource other = FluidResource.of(ModFluids.FLOWING_STEAM.get());
        h.setBlock(new BlockPos(2, 1, 2), ModBlocks.STEAM_TANK.get());
        SteamStorageBlockEntity tank = h.getBlockEntity(new BlockPos(2, 1, 2), SteamStorageBlockEntity.class);
        try (Transaction tx = Transaction.openRoot()) {
            h.assertTrue(tank.tank().insert(other, 1000, tx) == 1000, "the tank refused other steam");
            h.assertTrue(tank.tank().insert(FluidResource.of(Fluids.WATER), 1000, tx) == 0, "the tank took water");
            tx.commit();
        }
        h.assertTrue(tank.tank().getResource(0).equals(ModFluids.steam()), "other steam wasn't kept as ours: " + tank.tank().getResource(0));

        FluidTank theirs = new FluidTank(4000, fluid -> true, () -> {});
        theirs.add(other, 3000);
        try (Transaction tx = Transaction.openRoot()) {
            h.assertTrue(ModFluids.extractSteam(theirs, 2000, tx) == 2000, "couldn't draw other steam from another mod's tank");
            tx.commit();
        }
        h.assertTrue(theirs.amount() == 1000, "wrong steam left in their tank: " + theirs.amount());
        h.succeed();
    }

    /** The convention tags pack tools read: bosses, catchers, Polar Gateway and mining town biomes, any mod's dust. */
    static void conventionTags(GameTestHelper h) {
        for (var boss : java.util.List.of(ModEntities.OUTLAW_BOSS, ModEntities.SUN_ARCHON, ModEntities.MOTHERSHIP)) {
            h.assertTrue(boss.is(Tags.EntityTypes.BOSSES), boss.getId() + " isn't c:bosses");
            h.assertTrue(boss.is(Tags.EntityTypes.CAPTURING_NOT_SUPPORTED) && boss.is(Tags.EntityTypes.TELEPORTING_NOT_SUPPORTED),
                    boss.getId() + " can be caught or teleported");
        }
        h.assertTrue(ModEntities.TOWNSFOLK.is(Tags.EntityTypes.CAPTURING_NOT_SUPPORTED), "townsfolk can be caught");
        h.assertTrue(!ModEntities.WOOLLY_MAMMOTH.is(Tags.EntityTypes.CAPTURING_NOT_SUPPORTED), "wild mammoths should be fair game");

        var biomes = h.getLevel().registryAccess().lookupOrThrow(Registries.BIOME);
        for (ResourceKey<Biome> biome : java.util.List.of(Biomes.SNOWY_PLAINS, Biomes.ICE_SPIKES, Biomes.SNOWY_TAIGA, Biomes.GROVE, Biomes.SNOWY_SLOPES)) {
            h.assertTrue(biomes.getOrThrow(biome).is(ModStructures.HAS_POLAR_GATEWAY), biome.identifier() + " lost its Polar Gateways");
        }
        for (ResourceKey<Biome> biome : java.util.List.of(Biomes.SNOWY_BEACH, Biomes.FROZEN_PEAKS, Biomes.JAGGED_PEAKS, Biomes.FROZEN_OCEAN, Biomes.FROZEN_RIVER)) {
            h.assertTrue(!biomes.getOrThrow(biome).is(ModStructures.HAS_POLAR_GATEWAY), biome.identifier() + " shouldn't have Polar Gateways");
        }
        for (ResourceKey<Biome> biome : java.util.List.of(Biomes.TAIGA, Biomes.MEADOW, Biomes.WINDSWEPT_HILLS, Biomes.WINDSWEPT_GRAVELLY_HILLS)) {
            h.assertTrue(biomes.getOrThrow(biome).is(TownStyle.MINING.biomes()), biome.identifier() + " has no mining towns");
        }

        var recipe = h.getLevel().recipeAccess().byKey(ResourceKey.create(Registries.RECIPE, HollowFrontier.id("iron_ingot_from_smelting_iron_dust")));
        h.assertTrue(recipe.isPresent() && recipe.get().value() instanceof SingleItemRecipe, "no iron dust smelting recipe");
        var input = ((SingleItemRecipe) recipe.get().value()).input().getValues().unwrapKey();
        h.assertTrue(input.isPresent() && input.get().equals(ModTags.Items.DUSTS_IRON), "iron dust smelting doesn't take c:dusts/iron: " + input);
        h.succeed();
    }

    /** The mysteries' switches default to on, at the chances the mysteries always had. */
    static void mysterySwitchesDefaultOn(GameTestHelper h) {
        h.assertTrue(MysteryConfig.enabled() && MysteryConfig.visits() && MysteryConfig.hunts(), "a mystery switch is off by default");
        for (MysteryLog.Kind kind : MysteryLog.Kind.values()) {
            h.assertTrue(MysteryConfig.allows(kind), kind + " is off by default");
        }
        h.assertTrue(Math.abs(MysteryConfig.chance() - com.hollowfrontier.content.frontier.mystery.Mysteries.ROLL_CHANCE) < 1e-6, "the roll chance changed");
        h.succeed();
    }
}
