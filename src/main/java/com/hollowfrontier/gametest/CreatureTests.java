package com.hollowfrontier.gametest;

import java.util.Map;

import com.hollowfrontier.content.hollow.creature.AshSalamander;
import com.hollowfrontier.content.hollow.creature.FernSkitter;
import com.hollowfrontier.content.hollow.creature.GlassbackHare;
import com.hollowfrontier.content.hollow.creature.PaleRaven;
import com.hollowfrontier.registry.ModBlocks;
import com.hollowfrontier.registry.ModEntities;
import com.hollowfrontier.registry.ModItems;
import com.hollowfrontier.worldgen.HollowBiomes;
import com.hollowfrontier.worldgen.HollowEarth;

import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.attribute.EnvironmentAttributes;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.MobSpawnSettings;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.dimension.DimensionType;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.phys.Vec3;

/**
 * Phase 9b2: the Hollow Earth's small creatures. Their spawns (and the dimension's ceiling, which chunk-generation spawning
 * needs to find the ground under the crust), then each creature's behaviour: the skitter's shyness and thieving, the
 * hare's weakness for carrots, the salamander's lava-walking, basking and dislike of water, the raven's perching and
 * flight. The behaviour tests run on the 16-block garden plot (grass at y 0).
 */
final class CreatureTests {
    private CreatureTests() {}

    /** Each biome spawns its own creature, and the dimension has a ceiling (so they spawn on the land, not on the crust). */
    static void creaturesSpawnInTheirBiomes(GameTestHelper h) {
        var biomes = h.getLevel().registryAccess().lookupOrThrow(Registries.BIOME);
        Map<ResourceKey<Biome>, EntityType<?>> expected = Map.of(
                HollowBiomes.PRIMORDIAL_JUNGLE, ModEntities.FERN_SKITTER.get(),
                HollowBiomes.CRYSTAL_STEPPES, ModEntities.GLASSBACK_HARE.get(),
                HollowBiomes.EMBER_BADLANDS, ModEntities.ASH_SALAMANDER.get(),
                HollowBiomes.CONCAVIAN_HIGHLANDS, ModEntities.PALE_RAVEN.get());
        expected.forEach((key, type) -> {
            MobSpawnSettings spawns = biomes.getOrThrow(key).value().getAttributes().applyModifier(EnvironmentAttributes.NATURAL_MOB_SPAWNS, MobSpawnSettings.EMPTY);
            h.assertTrue(spawns.getMobsToSpawn(MobCategory.CREATURE).unwrap().stream().anyMatch(w -> w.value().type() == type),
                    key.identifier() + " doesn't spawn " + type);
        });
        DimensionType type = h.getLevel().registryAccess().lookupOrThrow(Registries.DIMENSION_TYPE).getOrThrow(HollowEarth.TYPE).value();
        h.assertTrue(type.hasCeiling() && type.hasSkyLight(), "the Hollow Earth should have a ceiling and still be sky-lit");
        h.succeed();
    }

    /** Food dropped near a skitter gets snatched. */
    static void skitterSnatchesFood(GameTestHelper h) {
        FernSkitter skitter = h.spawn(ModEntities.FERN_SKITTER.get(), new BlockPos(4, 1, 8));
        h.spawnItem(Items.BREAD, new Vec3(10.5, 1.2, 8.5));
        h.succeedWhen(() -> h.assertTrue(skitter.isCarryingFood(), "the skitter hasn't snatched the bread (at " + h.relativePos(skitter.blockPosition()) + ")"));
    }

    /** It eats what it carries (in time; here at once), and drops it if killed first. */
    static void skitterEatsOrDrops(GameTestHelper h) {
        FernSkitter eater = h.spawnWithNoFreeWill(ModEntities.FERN_SKITTER.get(), new BlockPos(4, 1, 4));
        eater.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(Items.BREAD));
        eater.eat(h.getLevel());
        h.assertFalse(eater.isCarryingFood(), "it didn't eat the bread");
        FernSkitter thief = h.spawnWithNoFreeWill(ModEntities.FERN_SKITTER.get(), new BlockPos(11, 1, 11));
        thief.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(Items.COOKED_BEEF));
        thief.kill(h.getLevel());
        h.succeedWhen(() -> h.assertTrue(!h.getLevel().getEntitiesOfClass(ItemEntity.class, thief.getBoundingBox().inflate(3),
                item -> item.getItem().is(Items.COOKED_BEEF)).isEmpty(), "the thief didn't drop the beef"));
    }

    /** It keeps away from a player who isn't sneaking. */
    static void skitterShiesFromPlayer(GameTestHelper h) {
        FernSkitter skitter = h.spawn(ModEntities.FERN_SKITTER.get(), new BlockPos(8, 1, 8));
        ServerPlayer player = TestPlayers.survival(h, new BlockPos(8, 1, 6));
        h.succeedWhen(() -> h.assertTrue(skitter.distanceTo(player) > 5.0, "the skitter stayed by the player: " + skitter.distanceTo(player)));
    }

    /** Any meat tames it (mammoth meat as much as beef), other food doesn't, and then it isn't shy of its owner. */
    static void skitterTamesWithMeat(GameTestHelper h) {
        ServerPlayer player = TestPlayers.survival(h, new BlockPos(8, 1, 6));
        h.assertTrue(FernSkitter.isTamingFood(new ItemStack(ModItems.RAW_MAMMOTH.get())) && FernSkitter.isTamingFood(new ItemStack(Items.COOKED_CHICKEN))
                && FernSkitter.isTamingFood(new ItemStack(Items.ROTTEN_FLESH)), "every meat should tame it");
        FernSkitter fed = h.spawnWithNoFreeWill(ModEntities.FERN_SKITTER.get(), new BlockPos(4, 1, 4));
        player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(Items.BREAD, 64));
        for (int i = 0; i < 30; i++) {
            fed.mobInteract(player, InteractionHand.MAIN_HAND);
        }
        h.assertFalse(fed.isTame(), "bread tamed it");
        FernSkitter skitter = h.spawn(ModEntities.FERN_SKITTER.get(), new BlockPos(8, 1, 8));
        player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(ModItems.RAW_MAMMOTH.get(), 64));
        for (int i = 0; i < 30 && !skitter.isTame(); i++) {
            skitter.mobInteract(player, InteractionHand.MAIN_HAND);
        }
        h.assertTrue(skitter.isOwnedBy(player), "30 bites of mammoth didn't tame it");
        skitter.setOrderedToSit(false);
        // A shy one bolts well past 8 blocks; a tame one wanders but follows back from 6
        h.onEachTick(() -> h.assertTrue(skitter.distanceTo(player) < 8.0, "the tame skitter still shied from its owner: " + skitter.distanceTo(player)));
        h.runAfterDelay(100, h::succeed);
    }

    /** A hare keeps away, unless you hold out a carrot: then it comes. */
    static void hareComesToCarrot(GameTestHelper h) {
        GlassbackHare hare = h.spawn(ModEntities.GLASSBACK_HARE.get(), new BlockPos(13, 1, 8));
        ServerPlayer player = TestPlayers.survival(h, new BlockPos(3, 1, 8));
        player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(Items.CARROT));
        h.assertTrue(GlassbackHare.isTreat(player.getMainHandItem()), "a carrot should tempt it");
        h.succeedWhen(() -> h.assertTrue(hare.distanceTo(player) < 3.0, "the hare didn't come to the carrot: " + hare.distanceTo(player)));
    }

    /** A salamander walks on lava, unhurt (standing on its surface, half a block up the lava block, as a strider does). */
    static void salamanderWalksOnLava(GameTestHelper h) {
        for (int x = 2; x <= 6; x++) {
            for (int z = 2; z <= 6; z++) {
                h.setBlock(new BlockPos(x, 0, z), Blocks.LAVA);
            }
        }
        AshSalamander salamander = h.spawn(ModEntities.ASH_SALAMANDER.get(), new BlockPos(4, 1, 4));
        h.assertTrue(salamander.canStandOnFluid(Fluids.LAVA.defaultFluidState()), "it should stand on lava");
        h.runAfterDelay(60, () -> {
            h.assertTrue(salamander.isAlive() && salamander.getHealth() == salamander.getMaxHealth(), "the lava hurt it: " + salamander.getHealth());
            h.assertTrue(salamander.getY() >= h.absoluteVec(new Vec3(0, 0.45, 0)).y, "it sank into the lava: " + h.relativeVec(salamander.position()));
            h.succeed();
        });
    }

    /** Water hurts it, like a strider. */
    static void salamanderHurtByWater(GameTestHelper h) {
        h.setBlock(new BlockPos(3, 1, 3), Blocks.WATER);
        AshSalamander salamander = h.spawnWithNoFreeWill(ModEntities.ASH_SALAMANDER.get(), new BlockPos(3, 1, 3));
        h.succeedWhen(() -> h.assertTrue(salamander.getHealth() < salamander.getMaxHealth(), "the water didn't hurt it"));
    }

    /** Left alone near an ash vent, it goes and basks on it. */
    static void salamanderBasksOnVent(GameTestHelper h) {
        BlockPos vent = new BlockPos(12, 0, 8);
        h.setBlock(vent, ModBlocks.ASH_VENT.get());
        AshSalamander salamander = h.spawn(ModEntities.ASH_SALAMANDER.get(), new BlockPos(5, 1, 8));
        h.succeedWhen(() -> h.assertTrue(salamander.isBasking() && salamander.blockPosition().below().equals(h.absolutePos(vent)),
                "it isn't basking on the vent (at " + h.relativePos(salamander.blockPosition()) + ", basking " + salamander.isBasking() + ")"));
    }

    /** A raven settles on top of a standing stone nearby. */
    static void ravenPerchesOnStone(GameTestHelper h) {
        for (int y = 1; y <= 3; y++) {
            h.setBlock(new BlockPos(12, y, 8), Blocks.CALCITE);
        }
        BlockPos top = new BlockPos(12, 4, 8);
        h.setBlock(top, Blocks.CHISELED_QUARTZ_BLOCK);
        h.assertTrue(PaleRaven.isPerch(h.getLevel(), h.absolutePos(top)), "the stone's top should be a perch");
        h.assertFalse(PaleRaven.isPerch(h.getLevel(), h.absolutePos(new BlockPos(5, 0, 5))), "the ground shouldn't be a perch");
        PaleRaven raven = h.spawn(ModEntities.PALE_RAVEN.get(), new BlockPos(4, 1, 8));
        h.succeedWhen(() -> h.assertTrue(raven.onGround() && raven.blockPosition().below().equals(h.absolutePos(top)),
                "the raven hasn't perched (at " + h.relativePos(raven.blockPosition()) + ")"));
    }

    /** Come close without sneaking and it flies off, away from you (with no Kindled site about to make for). */
    static void ravenFlushedByPlayer(GameTestHelper h) {
        PaleRaven raven = h.spawn(ModEntities.PALE_RAVEN.get(), new BlockPos(8, 1, 8));
        ServerPlayer player = TestPlayers.survival(h, new BlockPos(8, 1, 5));
        Vec3 destination = raven.flightDestination(player);
        h.assertTrue(destination.distanceTo(player.position()) > raven.distanceTo(player) + 10, "it would fly toward the player: " + destination);
        Vec3 start = raven.position();
        h.succeedWhen(() -> h.assertTrue(raven.getY() > start.y + 2 || raven.position().distanceTo(start) > 5,
                "the raven didn't take off (at " + raven.position() + ")"));
    }
}
