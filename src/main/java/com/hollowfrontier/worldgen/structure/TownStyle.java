package com.hollowfrontier.worldgen.structure;

import java.util.HashMap;
import java.util.Map;
import java.util.function.UnaryOperator;

import com.hollowfrontier.HollowFrontier;

import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.tags.TagKey;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.structure.Structure;

/**
 * The three kinds of frontier town, like vanilla's village types: one structure each, with its own biomes, its own
 * template and pool names (town/&lt;prefix&gt;...), and a block palette laid over the same blueprints.
 * <ul>
 * <li><b>Frontier</b> (plains, savannas): clapboard false fronts, the original look.</li>
 * <li><b>Desert</b> (deserts, badlands): a mission town of whitewashed sandstone and adobe under red tile roofs.</li>
 * <li><b>Mining</b> (taigas, meadows, windswept hills): a mountain camp of dark timber and stone under slate roofs.</li>
 * </ul>
 * Each building's palette variants (TownPalettes) are applied first, then the style on top.
 */
public enum TownStyle {
    FRONTIER("", "frontier_town", "has_frontier_town", Map.of()),
    DESERT("desert/", "desert_town", "has_desert_town", Map.ofEntries(
            Map.entry(Blocks.BIRCH_PLANKS, Blocks.SMOOTH_SANDSTONE),
            Map.entry(Blocks.OAK_PLANKS, Blocks.CUT_SANDSTONE),
            Map.entry(Blocks.PALE_OAK_PLANKS, Blocks.SMOOTH_SANDSTONE),
            Map.entry(Blocks.STRIPPED_BIRCH_LOG, Blocks.CUT_SANDSTONE),
            Map.entry(Blocks.STRIPPED_PALE_OAK_LOG, Blocks.CUT_SANDSTONE),
            Map.entry(Blocks.DARK_OAK_STAIRS, Blocks.BRICK_STAIRS),       // red tile roofs
            Map.entry(Blocks.DARK_OAK_SLAB, Blocks.BRICK_SLAB),
            Map.entry(Blocks.DARK_OAK_PLANKS, Blocks.BRICKS),
            Map.entry(Blocks.COBBLESTONE, Blocks.SANDSTONE),
            Map.entry(Blocks.STONE_BRICKS, Blocks.CUT_SANDSTONE),
            Map.entry(Blocks.SHORT_GRASS, Blocks.DEAD_BUSH),
            Map.entry(Blocks.OAK_LOG, Blocks.ACACIA_LOG),                 // the square's shade trees
            Map.entry(Blocks.OAK_LEAVES, Blocks.ACACIA_LEAVES))),
    MINING("mining/", "mining_town", "has_mining_town", Map.ofEntries(
            Map.entry(Blocks.BIRCH_PLANKS, Blocks.STRIPPED_SPRUCE_WOOD),  // rough-hewn timber
            Map.entry(Blocks.OAK_PLANKS, Blocks.SPRUCE_PLANKS),
            Map.entry(Blocks.PALE_OAK_PLANKS, Blocks.STRIPPED_SPRUCE_WOOD),
            Map.entry(Blocks.ACACIA_PLANKS, Blocks.SPRUCE_PLANKS),
            Map.entry(Blocks.STRIPPED_BIRCH_LOG, Blocks.SPRUCE_LOG),
            Map.entry(Blocks.STRIPPED_PALE_OAK_LOG, Blocks.SPRUCE_LOG),
            Map.entry(Blocks.STRIPPED_OAK_LOG, Blocks.STRIPPED_SPRUCE_LOG),
            Map.entry(Blocks.DARK_OAK_STAIRS, Blocks.DEEPSLATE_TILE_STAIRS), // slate roofs
            Map.entry(Blocks.DARK_OAK_SLAB, Blocks.DEEPSLATE_TILE_SLAB),
            Map.entry(Blocks.SPRUCE_STAIRS, Blocks.DEEPSLATE_TILE_STAIRS),
            Map.entry(Blocks.SPRUCE_SLAB, Blocks.DEEPSLATE_TILE_SLAB),
            Map.entry(Blocks.BRICKS, Blocks.STONE_BRICKS),
            Map.entry(Blocks.SMOOTH_SANDSTONE, Blocks.POLISHED_ANDESITE),
            Map.entry(Blocks.SMOOTH_SANDSTONE_STAIRS, Blocks.POLISHED_ANDESITE_STAIRS),
            Map.entry(Blocks.CUT_SANDSTONE, Blocks.STONE_BRICKS),
            Map.entry(Blocks.CHISELED_SANDSTONE, Blocks.CHISELED_STONE_BRICKS),
            Map.entry(Blocks.CUT_SANDSTONE_SLAB, Blocks.STONE_BRICK_SLAB),
            Map.entry(Blocks.SANDSTONE, Blocks.COBBLESTONE),
            Map.entry(Blocks.MUD_BRICKS, Blocks.COBBLESTONE),
            Map.entry(Blocks.MUD_BRICK_SLAB, Blocks.COBBLESTONE_SLAB),
            Map.entry(Blocks.MUD_BRICK_WALL, Blocks.COBBLESTONE_WALL),
            Map.entry(Blocks.TERRACOTTA, Blocks.COBBLESTONE),
            Map.entry(Blocks.DEAD_BUSH, Blocks.FERN),
            Map.entry(Blocks.POTTED_CACTUS, Blocks.POTTED_SPRUCE_SAPLING),
            Map.entry(Blocks.POTTED_DEAD_BUSH, Blocks.POTTED_FERN),
            Map.entry(Blocks.OAK_LOG, Blocks.SPRUCE_LOG),
            Map.entry(Blocks.OAK_LEAVES, Blocks.SPRUCE_LEAVES)));

    private final String prefix;
    private final ResourceKey<Structure> structure;
    private final TagKey<Biome> biomes;
    private final Map<Block, Block> palette;

    TownStyle(String prefix, String structure, String biomes, Map<Block, Block> palette) {
        this.prefix = prefix;
        this.structure = ResourceKey.create(Registries.STRUCTURE, HollowFrontier.id(structure));
        this.biomes = TagKey.create(Registries.BIOME, HollowFrontier.id(biomes));
        this.palette = palette;
    }

    public ResourceKey<Structure> structure() {
        return structure;
    }

    public TagKey<Biome> biomes() {
        return biomes;
    }

    /** Template name for a town piece in this style, e.g. "town/desert/house" (the frontier style keeps "town/house"). */
    public String template(String name) {
        return "town/" + prefix + name;
    }

    /** Pool id for a town pool in this style, e.g. "hollowfrontier:town/desert/shops". */
    public String pool(String name) {
        return HollowFrontier.MODID + ":" + template(name);
    }

    public UnaryOperator<BlockState> restyle() {
        return state -> swap(palette, state);
    }

    /** Puts the mapped block in place of the state's own, keeping whatever properties both share (stairs stay stairs). */
    public static BlockState swap(Map<Block, Block> palette, BlockState state) {
        Block to = palette.get(state.getBlock());
        return to == null ? state : to.withPropertiesOf(state);
    }

    /** Joins palettes: later maps win, and each map is applied to the result of the earlier ones. */
    @SafeVarargs
    public static Map<Block, Block> chain(Map<Block, Block>... maps) {
        Map<Block, Block> out = new HashMap<>();
        for (Map<Block, Block> map : maps) {
            out.replaceAll((from, to) -> map.getOrDefault(to, to));
            map.forEach(out::putIfAbsent);
        }
        return out;
    }
}
