package com.hollowfrontier.gametest;

import java.util.List;
import java.util.Map;

import com.hollowfrontier.HollowFrontier;
import com.hollowfrontier.content.hollow.gear.StarsteelGear;
import com.hollowfrontier.registry.ModAttachments;
import com.hollowfrontier.registry.ModBlocks;
import com.hollowfrontier.registry.ModItems;
import com.hollowfrontier.registry.ModTags;
import com.hollowfrontier.worldgen.HollowBiomes;
import com.hollowfrontier.worldgen.HollowFeatures;
import com.hollowfrontier.worldgen.structure.ModChestLoot;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderSet;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.animal.equine.Horse;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.ToolMaterial;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.WallSignBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.placement.PlacedFeature;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructurePlaceSettings;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplate;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;

/** Phase 9c: the Hollow Earth's ores, starsteel and its gear, phlogiston, and the strange grave on Boot Hill. */
final class HollowGearTests {
    private HollowGearTests() {}

    /** Each land has its ores: starsteel everywhere (more in the Badlands), phlogiston everywhere (more under the crystal country). */
    static void oresInBiomes(GameTestHelper h) {
        var biomes = h.getLevel().registryAccess().lookupOrThrow(Registries.BIOME);
        Map<ResourceKey<Biome>, List<ResourceKey<PlacedFeature>>> expected = Map.of(
                HollowBiomes.PRIMORDIAL_JUNGLE, List.of(HollowFeatures.STARSTEEL_ORE, HollowFeatures.PHLOGISTON_ORE),
                HollowBiomes.CRYSTAL_STEPPES, List.of(HollowFeatures.STARSTEEL_ORE, HollowFeatures.PHLOGISTON_ORE, HollowFeatures.PHLOGISTON_ORE_CRYSTAL),
                HollowBiomes.EMBER_BADLANDS, List.of(HollowFeatures.STARSTEEL_ORE, HollowFeatures.STARSTEEL_ORE_BADLANDS, HollowFeatures.PHLOGISTON_ORE),
                HollowBiomes.CONCAVIAN_HIGHLANDS, List.of(HollowFeatures.STARSTEEL_ORE, HollowFeatures.PHLOGISTON_ORE, HollowFeatures.PHLOGISTON_ORE_CRYSTAL));
        expected.forEach((key, ores) -> {
            List<HolderSet<PlacedFeature>> steps = biomes.getOrThrow(key).value().getGenerationSettings().features();
            for (ResourceKey<PlacedFeature> ore : ores) {
                h.assertTrue(steps.stream().anyMatch(step -> step.stream().anyMatch(f -> f.is(ore))), key.identifier() + " is missing " + ore.identifier());
            }
        });
        h.succeed();
    }

    /** Phlogiston glows in the rock, the lamp at full light; both ores want an iron pickaxe or better. */
    static void oresGlowAndNeedIron(GameTestHelper h) {
        h.assertValueEqual(ModBlocks.PHLOGISTON_ORE.get().defaultBlockState().getLightEmission(), 7, "phlogiston ore light");
        h.assertValueEqual(ModBlocks.PHLOGISTON_LAMP.get().defaultBlockState().getLightEmission(), 15, "phlogiston lamp light");
        for (BlockState ore : List.of(ModBlocks.STARSTEEL_ORE.get().defaultBlockState(), ModBlocks.DEEPSLATE_PHLOGISTON_ORE.get().defaultBlockState())) {
            h.assertFalse(new ItemStack(Items.STONE_PICKAXE).isCorrectToolForDrops(ore), ore + " shouldn't drop for stone");
            h.assertTrue(new ItemStack(Items.IRON_PICKAXE).isCorrectToolForDrops(ore), ore + " should drop for iron");
            h.assertTrue(new ItemStack(ModItems.STARSTEEL_PAXEL.get()).isCorrectToolForDrops(ore), ore + " should drop for starsteel");
        }
        h.assertTrue(new ItemStack(ModItems.STARSTEEL_PAXEL.get()).isCorrectToolForDrops(Blocks.OBSIDIAN.defaultBlockState()),
                "starsteel should mine obsidian, as diamond does");
        h.succeed();
    }

    /** The paxel: digs stone, wood and earth at speed; strips a log as an axe would, and makes a path of grass as a shovel. */
    static void paxelDoesAllThree(GameTestHelper h) {
        ItemStack paxel = new ItemStack(ModItems.STARSTEEL_PAXEL.get());
        for (BlockState state : List.of(Blocks.STONE.defaultBlockState(), Blocks.OAK_LOG.defaultBlockState(), Blocks.DIRT.defaultBlockState())) {
            h.assertTrue(paxel.getDestroySpeed(state) >= StarsteelGear.TOOLS.speed(), "the paxel should dig " + state + " fast");
            h.assertTrue(paxel.isCorrectToolForDrops(state) || !state.requiresCorrectToolForDrops(), "the paxel should get drops from " + state);
        }
        ServerPlayer player = TestPlayers.survival(h, new BlockPos(1, 1, 1));
        player.setItemInHand(InteractionHand.MAIN_HAND, paxel);
        BlockPos log = new BlockPos(4, 1, 4), grass = new BlockPos(6, 0, 4);
        h.setBlock(log, Blocks.OAK_LOG);
        h.setBlock(grass, Blocks.GRASS_BLOCK);
        use(h, player, log, Direction.UP);
        use(h, player, grass, Direction.UP);
        h.assertBlockPresent(Blocks.STRIPPED_OAK_LOG, log);
        h.assertBlockPresent(Blocks.DIRT_PATH, grass);
        h.succeed();
    }

    private static void use(GameTestHelper h, ServerPlayer player, BlockPos relative, Direction face) {
        BlockPos pos = h.absolutePos(relative);
        BlockHitResult hit = new BlockHitResult(Vec3.atCenterOf(pos).relative(face, 0.5), face, pos, false);
        player.getMainHandItem().useOn(new UseOnContext(player, InteractionHand.MAIN_HAND, hit));
    }

    /** The tier: outlasts diamond but not netherite, digs fastest, enchants like gold; armor tougher than diamond's. */
    static void starsteelSitsBetweenDiamondAndNetherite(GameTestHelper h) {
        ToolMaterial ori = StarsteelGear.TOOLS;
        h.assertTrue(ori.durability() > ToolMaterial.DIAMOND.durability() && ori.durability() < ToolMaterial.NETHERITE.durability(), "tool durability");
        h.assertTrue(ori.speed() > ToolMaterial.NETHERITE.speed(), "the fastest to dig with");
        h.assertTrue(ori.enchantmentValue() >= ToolMaterial.GOLD.enchantmentValue(), "enchants like gold");
        h.assertTrue(ori.attackDamageBonus() > ToolMaterial.DIAMOND.attackDamageBonus() && ori.attackDamageBonus() < ToolMaterial.NETHERITE.attackDamageBonus(),
                "damage between diamond and netherite");
        float toughness = StarsteelGear.ARMOR.toughness();
        h.assertTrue(toughness > 2.0F && toughness < 3.0F, "armor toughness between diamond and netherite: " + toughness);
        h.assertTrue(new ItemStack(ModItems.STARSTEEL_HELMET.get()).is(ModTags.Items.STARSTEEL_GEAR), "the helmet should be sun-forged gear");
        h.succeed();
    }

    /** Sun-forged: damaged starsteel worn or held mends a point at a time; other gear doesn't. */
    static void sunForgedMends(GameTestHelper h) {
        ServerPlayer player = TestPlayers.survival(h, new BlockPos(2, 1, 2));
        ItemStack pick = new ItemStack(ModItems.STARSTEEL_PAXEL.get());
        pick.setDamageValue(50);
        ItemStack chest = new ItemStack(ModItems.STARSTEEL_CHESTPLATE.get());
        chest.setDamageValue(20);
        ItemStack diamond = new ItemStack(Items.DIAMOND_BOOTS);
        diamond.setDamageValue(30);
        player.setItemSlot(EquipmentSlot.MAINHAND, pick);
        player.setItemSlot(EquipmentSlot.CHEST, chest);
        player.setItemSlot(EquipmentSlot.FEET, diamond);
        h.assertValueEqual(StarsteelGear.mend(player), 2, "pieces mended");
        h.assertValueEqual(player.getItemBySlot(EquipmentSlot.MAINHAND).getDamageValue(), 50 - StarsteelGear.MEND_AMOUNT, "pickaxe damage");
        h.assertValueEqual(player.getItemBySlot(EquipmentSlot.CHEST).getDamageValue(), 20 - StarsteelGear.MEND_AMOUNT, "chestplate damage");
        h.assertValueEqual(player.getItemBySlot(EquipmentSlot.FEET).getDamageValue(), 30, "diamond boots shouldn't mend");
        h.succeed();
    }

    /** A horse shod in starsteel takes no fall damage; an unshod one does. */
    static void starsteelShoesBreakFalls(GameTestHelper h) {
        Horse shod = h.spawn(EntityTypes.HORSE, new BlockPos(4, 14, 8));
        Horse bare = h.spawn(EntityTypes.HORSE, new BlockPos(12, 14, 8));
        shod.setData(ModAttachments.HORSESHOE_TIER.get(), 3);
        float full = shod.getHealth(), bareFull = bare.getHealth();
        h.succeedWhen(() -> {
            h.assertTrue(shod.onGround() && bare.onGround(), "the horses haven't landed");
            h.assertTrue(bare.getHealth() < bareFull, "the unshod horse should be hurt by the fall");
            h.assertValueEqual(shod.getHealth(), full, "the shod horse's health");
        });
    }

    /** A phlogiston shard burns as long as a block of coal (in a furnace or a boiler). */
    static void phlogistonIsFuel(GameTestHelper h) {
        ItemStack shard = new ItemStack(ModItems.PHLOGISTON_SHARD.get());
        h.assertTrue(shard.has(DataComponents.COOKING_FUEL), "phlogiston should be fuel");
        h.assertValueEqual(shard.get(DataComponents.COOKING_FUEL), new ItemStack(Items.COAL_BLOCK).get(DataComponents.COOKING_FUEL), "burn time");
        h.succeed();
    }

    /** The rare Boot Hill: a headstone with its epitaph on a sign, and the chest buried in the mound. */
    static void strangeGraveOnBootHill(GameTestHelper h) {
        StructureTemplate template = h.getLevel().getStructureTemplateManager().get(HollowFrontier.id("town/boot_hill_strange")).orElse(null);
        h.assertTrue(template != null, "the strange Boot Hill template is missing");
        var signs = template.filterBlocks(BlockPos.ZERO, new StructurePlaceSettings(), Blocks.SPRUCE_WALL_SIGN);
        h.assertTrue(signs.size() == 1 && signs.getFirst().nbt() != null && signs.getFirst().nbt().toString().contains("sign.hollowfrontier.strange_grave.1"),
                "the epitaph sign");
        h.assertTrue(signs.getFirst().state().getBlock() instanceof WallSignBlock, "a wall sign");
        var chests = template.filterBlocks(BlockPos.ZERO, new StructurePlaceSettings(), Blocks.CHEST);
        h.assertTrue(chests.size() == 1 && chests.getFirst().pos().getY() == 0
                && chests.getFirst().nbt().getStringOr("LootTable", "").equals(ModChestLoot.STRANGE_GRAVE.identifier().toString()), "the buried chest");
        h.succeed();
    }
}
