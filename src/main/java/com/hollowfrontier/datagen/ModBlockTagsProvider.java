package com.hollowfrontier.datagen;

import java.util.concurrent.CompletableFuture;

import com.hollowfrontier.HollowFrontier;
import com.hollowfrontier.registry.ModBlocks;
import com.hollowfrontier.registry.ModTags;

import net.minecraft.core.HolderLookup;
import net.minecraft.data.PackOutput;
import net.minecraft.tags.BlockItemTags;
import net.minecraft.tags.BlockTags;
import net.neoforged.neoforge.common.Tags;
import net.neoforged.neoforge.common.data.BlockTagsProvider;

public class ModBlockTagsProvider extends BlockTagsProvider {
    public ModBlockTagsProvider(PackOutput output, CompletableFuture<HolderLookup.Provider> lookupProvider) {
        super(output, lookupProvider, HollowFrontier.MODID);
    }

    @Override
    protected void addTags(HolderLookup.Provider registries) {
        tag(ModTags.BlockItems.STORAGE_BLOCKS_BRASS.block()).add(ModBlocks.BRASS_BLOCK.key());
        tag(Tags.BlockItems.STORAGE_BLOCKS.block())
                .addTag(ModTags.BlockItems.STORAGE_BLOCKS_BRASS.block());

        tag(BlockTags.MINEABLE_WITH_PICKAXE).add(ModBlocks.BRASS_BLOCK.key());
        tag(BlockTags.MINEABLE_WITH_AXE).add(ModBlocks.BOUNTY_BOARD.key(), ModBlocks.BAR_COUNTER.key(),
                ModBlocks.BAR_STOOL.key(), ModBlocks.SALOON_DOORS.key(), ModBlocks.SALOON_PIANO.key(), ModBlocks.CARD_TABLE.key());
        tag(BlockTags.MINEABLE_WITH_AXE).add(ModBlocks.OAK_CHAIR.key(), ModBlocks.SPRUCE_CHAIR.key(), ModBlocks.DARK_OAK_CHAIR.key(),
                ModBlocks.OAK_TABLE.key(), ModBlocks.SPRUCE_TABLE.key(), ModBlocks.DARK_OAK_TABLE.key());
        tag(BlockTags.MINEABLE_WITH_PICKAXE).add(ModBlocks.TOWN_LAMP.key(), ModBlocks.MAILBOX.key());
        tag(BlockTags.MINEABLE_WITH_AXE).add(ModBlocks.CLAIM_STAKE.key());
        tag(BlockTags.WOODEN_DOORS).add(ModBlocks.SALOON_DOORS.key());
        tag(BlockTags.MINEABLE_WITH_PICKAXE).add(ModBlocks.BOILER.key(), ModBlocks.COPPER_PIPE.key(), ModBlocks.STEAM_TANK.key(),
                ModBlocks.PRESSURE_GAUGE.key(), ModBlocks.CRUSHER.key());
        // Polar Gateways can't be blown up, eaten by the wither or the dragon, or built over by worldgen
        tag(BlockTags.DRAGON_IMMUNE).add(ModBlocks.GATEWAY_FRAME.key(), ModBlocks.POLAR_PORTAL.key(), ModBlocks.WAYSTONE.key());
        tag(BlockTags.WITHER_IMMUNE).add(ModBlocks.GATEWAY_FRAME.key(), ModBlocks.POLAR_PORTAL.key(), ModBlocks.WAYSTONE.key());
        tag(BlockTags.FEATURES_CANNOT_REPLACE).add(ModBlocks.GATEWAY_FRAME.key(), ModBlocks.POLAR_PORTAL.key(), ModBlocks.WAYSTONE.key());
        // The Hollow Earth's crust: mined like stone; its shell is bedrock
        tag(BlockTags.MINEABLE_WITH_PICKAXE).add(ModBlocks.CRUST_ROCK.key());
        tag(BlockTags.BASE_STONE_OVERWORLD).add(ModBlocks.CRUST_ROCK.key());
        tag(BlockTags.DRAGON_IMMUNE).add(ModBlocks.SHELL_BEDROCK.key());
        tag(BlockTags.WITHER_IMMUNE).add(ModBlocks.SHELL_BEDROCK.key());
        tag(BlockTags.FEATURES_CANNOT_REPLACE).add(ModBlocks.SHELL_BEDROCK.key());
        // Hollow Earth flora and ground (9b1). Fronds are leaves (so the landing and gateway clearing treat them as canopy).
        tag(BlockItemTags.LEAVES.block()).add(ModBlocks.FERN_FROND.key());
        tag(BlockTags.MINEABLE_WITH_HOE).add(ModBlocks.FERN_FROND.key());
        tag(BlockTags.MINEABLE_WITH_SHOVEL).add(ModBlocks.ASH.key());
        tag(BlockTags.MINEABLE_WITH_PICKAXE).add(ModBlocks.ASH_VENT.key());
        tag(BlockTags.INFINIBURN_OVERWORLD).add(ModBlocks.ASH_VENT.key());
        tag(BlockTags.NEEDS_STONE_TOOL).add(ModBlocks.BRASS_BLOCK.key());
        // The Hollow Earth's ores (9c): diamond-tier, so an iron pickaxe or better
        tag(ModTags.BlockItems.ORES_STARSTEEL.block()).add(ModBlocks.STARSTEEL_ORE.key(), ModBlocks.DEEPSLATE_STARSTEEL_ORE.key());
        tag(ModTags.BlockItems.ORES_PHLOGISTON.block()).add(ModBlocks.PHLOGISTON_ORE.key(), ModBlocks.DEEPSLATE_PHLOGISTON_ORE.key());
        tag(Tags.BlockItems.ORES.block()).addTag(ModTags.BlockItems.ORES_STARSTEEL.block()).addTag(ModTags.BlockItems.ORES_PHLOGISTON.block());
        tag(Tags.BlockItems.ORES_IN_GROUND_STONE.block()).add(ModBlocks.STARSTEEL_ORE.key(), ModBlocks.PHLOGISTON_ORE.key());
        tag(Tags.BlockItems.ORES_IN_GROUND_DEEPSLATE.block()).add(ModBlocks.DEEPSLATE_STARSTEEL_ORE.key(), ModBlocks.DEEPSLATE_PHLOGISTON_ORE.key());
        tag(Tags.BlockItems.ORE_RATES_SINGULAR.block()).add(ModBlocks.STARSTEEL_ORE.key(), ModBlocks.DEEPSLATE_STARSTEEL_ORE.key());
        tag(Tags.BlockItems.ORE_RATES_DENSE.block()).add(ModBlocks.PHLOGISTON_ORE.key(), ModBlocks.DEEPSLATE_PHLOGISTON_ORE.key());
        tag(ModTags.BlockItems.STORAGE_BLOCKS_RAW_STARSTEEL.block()).add(ModBlocks.RAW_STARSTEEL_BLOCK.key());
        tag(ModTags.BlockItems.STORAGE_BLOCKS_STARSTEEL.block()).add(ModBlocks.STARSTEEL_BLOCK.key());
        tag(Tags.BlockItems.STORAGE_BLOCKS.block()).addTag(ModTags.BlockItems.STORAGE_BLOCKS_RAW_STARSTEEL.block())
                .addTag(ModTags.BlockItems.STORAGE_BLOCKS_STARSTEEL.block());
        tag(BlockTags.MINEABLE_WITH_PICKAXE).add(ModBlocks.STARSTEEL_ORE.key(), ModBlocks.DEEPSLATE_STARSTEEL_ORE.key(),
                ModBlocks.RAW_STARSTEEL_BLOCK.key(), ModBlocks.STARSTEEL_BLOCK.key(), ModBlocks.PHLOGISTON_ORE.key(), ModBlocks.DEEPSLATE_PHLOGISTON_ORE.key(),
                ModBlocks.PHLOGISTON_LAMP.key());
        tag(BlockTags.NEEDS_IRON_TOOL).add(ModBlocks.STARSTEEL_ORE.key(), ModBlocks.DEEPSLATE_STARSTEEL_ORE.key(),
                ModBlocks.RAW_STARSTEEL_BLOCK.key(), ModBlocks.STARSTEEL_BLOCK.key(), ModBlocks.PHLOGISTON_ORE.key(), ModBlocks.DEEPSLATE_PHLOGISTON_ORE.key());
        tag(BlockTags.BEACON_BASE_BLOCKS).add(ModBlocks.STARSTEEL_BLOCK.key());
        // Concavia's crystal stone (12a): a pickaxe's work, and the brick set where vanilla's stairs, slabs and walls go
        tag(BlockTags.MINEABLE_WITH_PICKAXE).add(ModBlocks.CONCAVIAN_CRYSTAL.key(), ModBlocks.CRYSTAL_BRICKS.key(), ModBlocks.CRYSTAL_BRICK_STAIRS.key(),
                ModBlocks.CRYSTAL_BRICK_SLAB.key(), ModBlocks.CRYSTAL_BRICK_WALL.key(), ModBlocks.CHISELED_CRYSTAL_BRICKS.key(), ModBlocks.SUN_PYLON.key(),
                ModBlocks.SAUCER_HULL.key(), ModBlocks.SAUCER_WRECKAGE.key(), ModBlocks.PHLOGISTON_BEACON.key());
        tag(BlockTags.NEEDS_IRON_TOOL).add(ModBlocks.SAUCER_HULL.key(), ModBlocks.SAUCER_WRECKAGE.key()); // 13a: alien alloy
        tag(BlockTags.NEEDS_DIAMOND_TOOL).add(ModBlocks.PHLOGISTON_BEACON.key()); // 13d
        tag(BlockTags.MINEABLE_WITH_PICKAXE).add(ModBlocks.IMPLANT_RIG.key()); // 20
        tag(BlockTags.NEEDS_IRON_TOOL).add(ModBlocks.IMPLANT_RIG.key());
        // 50: what comes away in the hands of someone taken: the grilles over the vents, and the rig
        tag(ModTags.Blocks.TAKEN_BREAKABLE).add(ModBlocks.IMPLANT_RIG.key())
                .add(net.minecraft.world.level.block.Blocks.COPPER_GRATE.waxed().oxidized().builtInRegistryHolder().key());
        tag(BlockItemTags.STAIRS.block()).add(ModBlocks.CRYSTAL_BRICK_STAIRS.key());
        tag(BlockItemTags.SLABS.block()).add(ModBlocks.CRYSTAL_BRICK_SLAB.key());
        tag(BlockItemTags.WALLS.block()).add(ModBlocks.CRYSTAL_BRICK_WALL.key());
        tag(BlockTags.CRYSTAL_SOUND_BLOCKS).add(ModBlocks.CONCAVIAN_CRYSTAL.key());
        // 11a: a crop circle's flattened wheat keeps its farmland, as the standing crop did
        tag(BlockTags.MAINTAINS_FARMLAND).add(ModBlocks.FLATTENED_WHEAT.key());
        tag(ModTags.Blocks.MINEABLE_WITH_PAXEL).addTag(BlockTags.MINEABLE_WITH_PICKAXE).addTag(BlockTags.MINEABLE_WITH_AXE)
                .addTag(BlockTags.MINEABLE_WITH_SHOVEL);
    }
}
