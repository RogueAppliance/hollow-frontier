package com.hollowfrontier.worldgen.structure;

import static com.hollowfrontier.worldgen.structure.TownDecor.barrel;
import static com.hollowfrontier.worldgen.structure.TownDecor.candle;
import static com.hollowfrontier.worldgen.structure.TownDecor.chair;
import static com.hollowfrontier.worldgen.structure.TownDecor.stool;

import java.util.ArrayList;
import java.util.List;

import com.hollowfrontier.registry.ModBlocks;
import com.hollowfrontier.registry.ModEntities;
import com.hollowfrontier.registry.ModItems;

import net.minecraft.core.Direction;
import net.minecraft.core.FrontAndTop;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.BarrelBlock;
import net.minecraft.world.level.block.BellBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.CampfireBlock;
import net.minecraft.world.level.block.CandleBlock;
import net.minecraft.world.level.block.ChiseledBookShelfBlock;
import net.minecraft.world.level.block.DoorBlock;
import net.minecraft.world.level.block.FenceGateBlock;
import net.minecraft.world.level.block.GrindstoneBlock;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.LadderBlock;
import net.minecraft.world.level.block.LanternBlock;
import net.minecraft.world.level.block.LayeredCauldronBlock;
import net.minecraft.world.level.block.RotatedPillarBlock;
import net.minecraft.world.level.block.SlabBlock;
import net.minecraft.world.level.block.StairBlock;
import net.minecraft.world.level.block.TrapDoorBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.AttachFace;
import net.minecraft.world.level.block.state.properties.BellAttachType;
import net.minecraft.world.level.block.state.properties.DoorHingeSide;
import net.minecraft.world.level.block.state.properties.Half;
import net.minecraft.world.level.block.state.properties.SlabType;

/**
 * The frontier town's buildings, and the list of every town piece (TownLayout's squares, streets and lanes, these
 * buildings, TownShops' and TownLandmarks') in every {@link TownStyle} and palette ({@link TownPalettes}). {@link #pools}
 * says which buildings each lot draws and how often; the datagen pools (ModStructures) are built from it, so templates
 * and pools always match. Cat spots (Blueprint.cat) are in the saloons, store, sheriff's office, blacksmith, livery,
 * homes, undertaker and Boot Hill; each keeps its cat by chance (TownCatOdds).
 *
 * Every building is drawn facing NORTH: its front (porch or step) is the z = 0 row with a jigsaw connector at
 * (centre x, 0, 0) facing north; the jigsaw system turns it to face the street. Floors are at y = 0. Shops have their
 * name over the door (TownDecor.shopSign), and are furnished to their trade: goods on shelves, a stove with its pipe out
 * through the roof, a back yard for the homes.
 */
public final class TownBlueprints {
    private TownBlueprints() {}

    public static final String FRONT = "hollowfrontier:town_front";
    public static final String LOT = "hollowfrontier:town_lot";

    /** Base names of the buildings (and outskirts) drawn here, in TownShops and in TownLandmarks, each restyled per style and palette. */
    public static List<Blueprint> buildings() {
        // Buildings on street and lane lots stand in full lots, side yards and all (TownLots)
        List<Blueprint> out = new ArrayList<>(List.of(saloon(), TownLots.inLot(generalStore()), sheriffOffice(), TownLots.inLot(bank()), hotel(),
                TownLots.inLot(blacksmith()), TownLots.inLot(livery()), TownLots.inLot(undertaker()), TownLots.inLot(house()), TownLots.inLot(adobe()),
                chapel(), waterTower()));
        out.addAll(TownShops.all());
        out.addAll(TownLandmarks.all());
        return out;
    }

    public static List<Blueprint> all() {
        List<Blueprint> out = new ArrayList<>();
        List<Blueprint> buildings = buildings();
        for (TownStyle style : TownStyle.values()) {
            TownLayout.pieces(style).forEach(piece -> out.add(piece.restyled(piece.name(), style.restyle()).hardenGround().hangLanterns()));
            for (Blueprint building : buildings) {
                String base = building.name().substring("town/".length());
                List<java.util.Map<Block, Block>> palettes = TownPalettes.of(base);
                for (int i = 0; i < palettes.size(); i++) {
                    java.util.Map<Block, Block> palette = palettes.get(i);
                    java.util.function.UnaryOperator<BlockState> restyle = style.restyle();
                    out.add(building.restyled(style.template(TownPalettes.variant(base, i)), state -> restyle.apply(TownStyle.swap(palette, state))).hardenGround().hangLanterns().bareThresholds());
                }
            }
        }
        return out;
    }

    /**
     * Each pool of a style: pool short name (TownLayout) to building base name and weight. Every palette variant of a
     * building is in its pool, sharing the building's weight.
     */
    public static java.util.Map<String, java.util.Map<String, Integer>> pools(TownStyle style) {
        boolean desert = style == TownStyle.DESERT;
        boolean mining = style == TownStyle.MINING;
        java.util.Map<String, java.util.Map<String, Integer>> pools = new java.util.HashMap<>(java.util.Map.ofEntries(
                java.util.Map.entry(TownLayout.SQUARES, java.util.Map.of("square_cross_bandstand", 2, "square_cross_well", 2,
                        "square_tee_bandstand", 3, "square_tee_well", 3, "square_pair_bandstand", 2, "square_pair_well", 2)),
                java.util.Map.entry(TownLayout.STREETS, java.util.Map.of("street_long", 3, "street_short", 2)),
                java.util.Map.entry(TownLayout.CIVIC, java.util.Map.of("sheriff_office", 3, "bank", 1)),
                java.util.Map.entry(TownLayout.COMMERCE, java.util.Map.of("hotel", 1, "hotel_grand", 1)),
                java.util.Map.entry(TownLayout.SALOON, java.util.Map.of("saloon", 1, "saloon_grand", 1)),
                java.util.Map.entry(TownLayout.SHOPS, java.util.Map.of("general_store", 3, "blacksmith", mining ? 3 : 2, "livery", 2,
                        "undertaker", 1, "bank", 1, "gunsmith", 1, "doctor", 2, "post_office", 2, "bathhouse", 1, "gazette_office", 1)),
                java.util.Map.entry(TownLayout.GUNSMITH, java.util.Map.of("gunsmith", 1)),
                java.util.Map.entry(TownLayout.HOMES, java.util.Map.of("house", desert ? 1 : 3, "adobe", desert ? 4 : mining ? 1 : 2,
                        "homestead", desert ? 1 : 2)),
                java.util.Map.entry(TownLayout.LANDMARKS, java.util.Map.of("courthouse", 3, "church", 2)),
                java.util.Map.entry(TownLayout.STREET_ENDS, java.util.Map.of("chapel", 2, "water_tower", 1, "depot", 2, "lane", 4, "town_limits", 1,
                        "schoolhouse", 2)),
                java.util.Map.entry(TownLayout.LANE_LOTS, java.util.Map.of("house", desert ? 1 : 3, "adobe", desert ? 3 : 1, "garden", 2, "homestead", 1)),
                java.util.Map.entry(TownLayout.OUTSKIRTS, java.util.Map.of("windpump", 6, "corral", 6, "boot_hill", 2, "boot_hill_b", 2,
                        "boot_hill_c", 1, "boot_hill_strange", 1, "wheat_field", desert ? 3 : 6, "ranch", 4)),
                java.util.Map.entry(TownLayout.FARMS, java.util.Map.of("farm_track", 1)),
                java.util.Map.entry(TownLayout.FIELDS, java.util.Map.of("wheat_field", 1))));
        if (style == TownStyle.FRONTIER) { // one railroad pool serves every style (the depot names it)
            pools.put(TownLayout.RAILROAD, java.util.Map.of("rail_spur", 2, "rail_boxcar", 2, "rail_tank", 1));
        }
        return pools;
    }

    // ---------------------------------------------------------------- block helpers

    static BlockState s(Block block) {
        return block.defaultBlockState();
    }

    static BlockState stairs(Block block, Direction facing, Half half) {
        return block.defaultBlockState().setValue(StairBlock.FACING, facing).setValue(StairBlock.HALF, half);
    }

    static BlockState slab(Block block, SlabType type) {
        return block.defaultBlockState().setValue(SlabBlock.TYPE, type);
    }

    static BlockState facing(Block block, Direction facing) {
        return block.defaultBlockState().setValue(HorizontalDirectionalBlock.FACING, facing);
    }

    static BlockState log(Block block, Direction.Axis axis) {
        return block.defaultBlockState().setValue(RotatedPillarBlock.AXIS, axis);
    }

    // Entity NBT with one string, e.g. a townsfolk's "folk" or a lawman's "rank".
    static CompoundTag tag(String key, String value) {
        CompoundTag tag = new CompoundTag();
        tag.putString(key, value);
        return tag;
    }

    static BlockState hangingLantern() {
        return Blocks.LANTERN.defaultBlockState().setValue(LanternBlock.HANGING, true);
    }

    public static BlockState water(int level) {
        return Blocks.WATER_CAULDRON.defaultBlockState().setValue(LayeredCauldronBlock.LEVEL, level);
    }

    static BlockState candles(int count) {
        return Blocks.CANDLE.defaultBlockState().setValue(CandleBlock.CANDLES, count).setValue(CandleBlock.LIT, true);
    }

    /** A barrel standing open, its goods showing (a cracker barrel, a bin of nails). */
    static BlockState openBarrel() {
        return Blocks.BARREL.defaultBlockState().setValue(BarrelBlock.FACING, Direction.UP).setValue(BarrelBlock.OPEN, true);
    }

    /** Empty pigeonholes: a chiseled bookshelf with no books (a post office's sorting case, a hotel's key rack). */
    static BlockState pigeonholes(Direction facing) {
        return Blocks.CHISELED_BOOKSHELF.defaultBlockState().setValue(ChiseledBookShelfBlock.FACING, facing);
    }

    /** This building's own connector: front centre, facing the street, becomes {@code floor} once placed. */
    static void front(Blueprint b, int x, BlockState floor) {
        b.jigsaw(x, 0, 0, FrontAndTop.NORTH_UP, FRONT, LOT, "minecraft:empty", floor);
    }

    /**
     * Western shopfront, w wide and d deep: a boardwalk porch (z 0..1) under an awning at awningY, walls z 2..d-1 up
     * to y h with a flat roof, and a false front at z 2 that rises {@code rise} above the roof, stepped at the top.
     * Door in the middle of the facade with a window either side.
     */
    static void shopfront(Blueprint b, int w, int d, int h, int rise, int awningY,
                                  BlockState wall, BlockState post, BlockState facade, Block trimSlab, Block roofSlab, DoorBlock door) {
        int cx = w / 2;
        b.air(0, 1, 0, w - 1, h + rise + 2, d - 1);
        b.fill(0, 0, 0, w - 1, 0, d - 1, Blocks.SPRUCE_PLANKS);
        b.walls(0, 1, 2, w - 1, h, d - 1, wall);
        for (int[] c : new int[][] {{0, 2}, {w - 1, 2}, {0, d - 1}, {w - 1, d - 1}}) {
            b.fill(c[0], 1, c[1], c[0], h, c[1], post);
        }
        b.fill(0, h + 1, 3, w - 1, h + 1, d - 1, slab(roofSlab, SlabType.BOTTOM));
        // False front with a stepped parapet and a cornice of slabs
        b.fill(0, 1, 2, w - 1, h + rise, 2, facade);
        b.fill(2, h + rise + 1, 2, w - 3, h + rise + 1, 2, facade);
        b.fill(0, h + rise, 1, w - 1, h + rise, 1, slab(trimSlab, SlabType.TOP));
        b.fill(2, h + rise + 2, 2, w - 3, h + rise + 2, 2, slab(trimSlab, SlabType.BOTTOM));
        // Porch: fence posts, awning, door and windows
        for (int x : new int[] {0, cx - 2, cx + 2, w - 1}) {
            b.fill(x, 1, 0, x, awningY - 1, 0, Blocks.SPRUCE_FENCE);
        }
        b.fill(0, awningY, 0, w - 1, awningY, 1, slab(Blocks.SPRUCE_SLAB, SlabType.BOTTOM));
        b.door(cx, 1, 2, door, Direction.NORTH, DoorHingeSide.LEFT);
        b.fill(cx - 3, 2, 2, cx - 2, 3, 2, Blocks.GLASS_PANE);
        b.fill(cx + 2, 2, 2, cx + 3, 3, 2, Blocks.GLASS_PANE);
        // Side windows, two high, every few blocks down each side; a post between each pair
        for (int z = 4; z < d - 1; z += 3) {
            b.fill(0, 2, z, 0, 3, z, Blocks.GLASS_PANE);
            b.fill(w - 1, 2, z, w - 1, 3, z, Blocks.GLASS_PANE);
            if (z + 2 < d - 1) {
                b.fill(0, 1, z + 2, 0, h, z + 2, post);
                b.fill(w - 1, 1, z + 2, w - 1, h, z + 2, post);
            }
        }
        front(b, cx, s(Blocks.SPRUCE_PLANKS));
    }

    /**
     * Gable roof over x 0..w-1 (ridge running front to back, along z) from eave height y; the end walls at z0 and z1
     * are filled up to the roof with {@code gable}. w should be odd.
     */
    static void gableRoof(Blueprint b, int w, int z0, int z1, int y, Block stairsBlock, BlockState ridge, BlockState gable) {
        for (int i = 0; i <= w / 2; i++) {
            int left = i, right = w - 1 - i, yy = y + i;
            if (left == right) {
                b.fill(left, yy, z0 - 1, left, yy, z1 + 1, ridge);
            } else {
                b.fill(left, yy, z0 - 1, left, yy, z1 + 1, stairs(stairsBlock, Direction.EAST, Half.BOTTOM));
                b.fill(right, yy, z0 - 1, right, yy, z1 + 1, stairs(stairsBlock, Direction.WEST, Half.BOTTOM));
                if (right - left > 1) {
                    b.fill(left + 1, yy, z0, right - 1, yy, z0, gable);
                    b.fill(left + 1, yy, z1, right - 1, yy, z1, gable);
                }
            }
        }
    }

    // ---------------------------------------------------------------- buildings (all face north)

    /** The saloon (barmaid, drinks, bounty board), with a connector in the middle of its porch. */
    static Blueprint saloon() {
        Blueprint b = new Blueprint("town/saloon");
        b.paste(FrontierBlueprints.saloon(), 0, 0, 0);
        front(b, 6, s(Blocks.OAK_PLANKS));
        return b;
    }

    // 11 x 12: shelves of goods behind the counter, open barrels and a dummy in a duster on the floor, a pot-bellied stove
    // with a checkers table by it, the shopkeeper.
    static Blueprint generalStore() {
        Blueprint b = new Blueprint("town/general_store");
        int w = 11, d = 12, h = 4, cx = 5;
        BlockState post = log(Blocks.STRIPPED_OAK_LOG, Direction.Axis.Y);
        shopfront(b, w, d, h, 3, 4, s(Blocks.OAK_PLANKS), post, s(Blocks.SPRUCE_PLANKS), Blocks.DARK_OAK_SLAB, Blocks.DARK_OAK_SLAB,
                (DoorBlock) Blocks.SPRUCE_DOOR);
        TownDecor.cornice(b, w, h, 3, post, Blocks.DARK_OAK_STAIRS);
        TownDecor.shutters(b, w, Blocks.DARK_OAK_TRAPDOOR);
        TownDecor.shopSign(b, cx, 3, "general_store");
        // The counter, with the till and a lamp on it; behind it, kegs and two rows of shelves along the back wall
        b.fill(2, 1, 7, 8, 1, 7, ModBlocks.BAR_COUNTER.get());
        b.set(3, 2, 7, Blocks.LANTERN);
        b.set(6, 2, 7, candles(3));                                       // (a pot here stood as tall as the shopkeeper)
        b.fill(1, 1, 10, 9, 1, 10, barrel(Direction.NORTH));
        net.minecraft.world.level.ItemLike[][] low = {
                {Items.BREAD, Items.BREAD, Items.COOKIE}, {Items.APPLE, Items.APPLE, Items.CARROT}, {Items.POTATO, Items.BEETROOT, Items.PUMPKIN_PIE},
                {ModItems.COWBOY_COFFEE.get(), ModItems.COWBOY_COFFEE.get(), Items.SUGAR}, {Items.LANTERN, Items.CANDLE, Items.TORCH},
                {Items.LEAD, Items.STRING, Items.SHEARS}, {Items.BUCKET, Items.IRON_SHOVEL, Items.IRON_HOE}, {Items.COMPASS, Items.CLOCK, Items.BOWL},
                {ModItems.PERCUSSION_CAP.get(), ModItems.REVOLVER_CARTRIDGE.get(), ModItems.BUCKSHOT_CARTRIDGE.get()}};
        net.minecraft.world.level.ItemLike[][] high = {
                {Items.LEATHER, Items.LEATHER, Items.RABBIT_HIDE}, {Items.BOOK, Items.PAPER, Items.INK_SAC}, {ModItems.FRONTIER_GAZETTE.get(), Items.PAPER, Items.FEATHER},
                {Items.GLASS_BOTTLE, Items.GLASS_BOTTLE, Items.HONEY_BOTTLE}, {ModItems.BANDANA.get(), ModItems.BANDANA.get(), ModItems.BANDANA.get()},
                {ModItems.SNAKE_OIL.get(), ModItems.SARSAPARILLA.get(), ModItems.SNAKE_OIL.get()}, {Items.WHEAT_SEEDS, Items.MELON_SEEDS, Items.PUMPKIN_SEEDS},
                {Items.IRON_NUGGET, Items.FLINT, Items.FLINT_AND_STEEL}, {Items.GUNPOWDER, Items.GUNPOWDER, Items.FIRE_CHARGE}};
        for (int i = 0; i < 9; i++) {
            b.shelf(1 + i, 2, 10, Blocks.DARK_OAK_SHELF, Direction.NORTH, low[i]);
            b.shelf(1 + i, 3, 10, Blocks.DARK_OAK_SHELF, Direction.NORTH, high[i]);
        }
        b.chest(1, 1, 8, Direction.EAST, ModChestLoot.GENERAL_STORE);
        // The shop floor: open barrels and feed sacks down the west wall, the dummy at the end of them
        b.set(1, 1, 3, openBarrel());
        b.set(1, 1, 4, openBarrel());
        b.set(1, 2, 3, barrel(Direction.EAST));
        b.set(2, 1, 3, Blocks.HAY_BLOCK);
        b.armorStand(1, 1, 6, -90, ModItems.COWBOY_HAT.get(), ModItems.DUSTER.get());
        // The stove on the east side with the checkers table by it, two stools and a cracker barrel
        TownDecor.stove(b, 9, 1, 5, Direction.WEST, h + 2);
        TownDecor.table(b, 7, 1, 4, Blocks.SPRUCE_FENCE, Blocks.BIRCH_PRESSURE_PLATE);
        b.set(7, 1, 3, stool());
        b.set(8, 1, 4, stool());
        b.set(9, 1, 3, openBarrel());
        b.set(3, 4, 5, hangingLantern());
        b.set(7, 4, 5, hangingLantern());
        b.set(5, 4, 9, hangingLantern());
        b.set(2, 1, 0, Blocks.BARREL);                                    // stock out on the porch
        b.set(8, 1, 0, Blocks.HAY_BLOCK);
        TownDecor.bench(b, 9, 1, 1, Direction.SOUTH);                     // a bench under the east window
        b.entity(ModEntities.TOWNSFOLK.get(), 5, 1, 8, tag("folk", "shopkeeper"));
        b.cat(2, 2, 0, "store");                                          // on the barrel out on the porch
        return b;
    }

    // 9 x 12: the sheriff's desk, a gun locker behind bars and the bounty board up front, a stove, two jail cells at the back.
    static Blueprint sheriffOffice() {
        Blueprint b = new Blueprint("town/sheriff_office");
        int w = 9, h = 4, cx = 4;
        BlockState post = log(Blocks.SPRUCE_LOG, Direction.Axis.Y);
        shopfront(b, w, 12, h, 2, 4, s(Blocks.SPRUCE_PLANKS), post, s(Blocks.STRIPPED_SPRUCE_WOOD), Blocks.SPRUCE_SLAB, Blocks.SPRUCE_SLAB,
                (DoorBlock) Blocks.SPRUCE_DOOR);
        TownDecor.cornice(b, w, h, 2, post, Blocks.SPRUCE_STAIRS);
        TownDecor.shutters(b, w, Blocks.SPRUCE_TRAPDOOR);
        TownDecor.shopSign(b, cx, 3, "sheriff");
        b.fill(0, 2, 10, 0, 3, 10, Blocks.IRON_BARS);                     // the cells' side windows are barred
        b.fill(w - 1, 2, 10, w - 1, 3, 10, Blocks.IRON_BARS);
        b.fill(1, 1, 5, 2, 1, 5, ModBlocks.BAR_COUNTER.get());          // desk
        b.set(1, 1, 6, stairs(Blocks.SPRUCE_STAIRS, Direction.SOUTH, Half.BOTTOM));
        b.set(1, 2, 5, candles(2));
        b.set(2, 2, 5, Blocks.LANTERN);
        // The gun locker in the corner: rifles on a shelf behind bars, over a cupboard
        b.fill(1, 1, 3, 2, 1, 4, Blocks.SPRUCE_PLANKS);
        b.shelf(1, 2, 3, Blocks.SPRUCE_SHELF, Direction.EAST, ModItems.PLAINS_RIFLE.get(), ModItems.SHOTGUN.get());
        b.shelf(1, 2, 4, Blocks.SPRUCE_SHELF, Direction.EAST, ModItems.LASSO.get(), ModItems.WANTED_POSTER.get(), ModItems.BRASS_STAR.get());
        b.fill(2, 2, 3, 2, 3, 4, Blocks.IRON_BARS);
        b.set(1, 3, 3, Blocks.SPRUCE_PLANKS);
        b.set(1, 3, 4, Blocks.SPRUCE_PLANKS);
        b.fill(1, 4, 3, 2, 4, 4, slab(Blocks.SPRUCE_SLAB, SlabType.BOTTOM));
        TownDecor.rug(b, 3, 1, 3, 5, 4, DyeColor.RED, DyeColor.BROWN);
        b.bountyBoard(7, 1, 3, Direction.WEST, 2); // on the east wall, by the door
        b.chest(7, 1, 6, Direction.WEST, ModChestLoot.SHERIFF_OFFICE).vault(7, 1, 6, 1); // the strongbox: raiders go for it if the town has no bank
        TownDecor.stove(b, 7, 1, 7, Direction.WEST, h + 2);
        // Cells: a wall of bars with an iron door each, split down the middle; a cot, a bucket and a barred window each
        b.fill(1, 1, 8, 7, h, 8, Blocks.IRON_BARS);                       // floor to ceiling, no gap to climb through
        b.fill(4, 1, 9, 4, h, 10, Blocks.IRON_BARS);
        b.door(2, 1, 8, (DoorBlock) Blocks.IRON_DOOR, Direction.NORTH, DoorHingeSide.LEFT);
        b.door(6, 1, 8, (DoorBlock) Blocks.IRON_DOOR, Direction.NORTH, DoorHingeSide.RIGHT);
        b.set(1, 1, 10, Blocks.HAY_BLOCK);
        b.set(7, 1, 10, Blocks.HAY_BLOCK);
        b.set(3, 1, 10, Blocks.CAULDRON);
        b.set(5, 1, 10, Blocks.CAULDRON);
        b.set(2, 2, 11, Blocks.IRON_BARS);
        b.set(6, 2, 11, Blocks.IRON_BARS);
        b.set(4, 4, 5, hangingLantern());
        b.set(1, 1, 0, Blocks.BARREL);                                    // the porch: a barrel and the hitching rail
        b.fill(7, 1, 0, 7, 1, 1, Blocks.SPRUCE_FENCE);
        b.entity(ModEntities.LAWMAN.get(), 4, 1, 5, tag("rank", "sheriff"));
        b.entity(ModEntities.LAWMAN.get(), 5, 1, 7, tag("rank", "deputy"));
        b.cat(3, 1, 6, "sheriff");                                        // the sheriff's cat by the desk: the town's one star
        CompoundTag jailed = new CompoundTag();
        jailed.putBoolean("jailed", true);
        b.entity(ModEntities.OUTLAW.get(), 2, 1, 9, jailed);           // a prisoner in the west cell
        return b;
    }

    // 9 x 12, brick with a sandstone front: a tiled lobby with a bench, the teller's cage with the ledgers behind it, a vault.
    static Blueprint bank() {
        Blueprint b = new Blueprint("town/bank");
        int w = 9, h = 5, cx = 4;
        shopfront(b, w, 12, h, 2, 4, s(Blocks.BRICKS), s(Blocks.CUT_SANDSTONE),
                s(Blocks.SMOOTH_SANDSTONE), Blocks.CUT_SANDSTONE_SLAB, Blocks.BRICK_SLAB, (DoorBlock) Blocks.DARK_OAK_DOOR);
        TownDecor.cornice(b, w, h, 2, s(Blocks.CUT_SANDSTONE), Blocks.SMOOTH_SANDSTONE_STAIRS);
        TownDecor.shutters(b, w, Blocks.DARK_OAK_TRAPDOOR);
        TownDecor.shopSign(b, cx, 3, "bank");
        b.fill(0, 2, 10, 0, 3, 10, Blocks.BRICKS);                         // no windows into the vault
        b.fill(w - 1, 2, 10, w - 1, 3, 10, Blocks.BRICKS);
        b.set(4, 6, 2, Blocks.CHISELED_SANDSTONE);
        // A chequered floor
        for (int x = 1; x <= 7; x++) {
            for (int z = 3; z <= 10; z++) {
                b.set(x, 0, z, (x + z) % 2 == 0 ? Blocks.POLISHED_ANDESITE : Blocks.POLISHED_DIORITE);
            }
        }
        b.fill(1, 1, 6, 6, 1, 6, ModBlocks.BAR_COUNTER.get());           // teller's counter...
        b.fill(1, 2, 6, 7, 3, 6, Blocks.IRON_BARS);                       // ...and cage
        b.door(7, 1, 6, (DoorBlock) Blocks.SPRUCE_DOOR, Direction.NORTH, DoorHingeSide.RIGHT); // the teller's gate, to the vault
        // The lobby: a bench along the west wall, a writing desk, a potted palm
        b.fill(1, 1, 3, 1, 1, 4, stairs(Blocks.DARK_OAK_STAIRS, Direction.WEST, Half.BOTTOM));
        b.set(7, 1, 3, Blocks.POTTED_BAMBOO);
        b.set(5, 1, 3, Blocks.LECTERN.defaultBlockState().setValue(HorizontalDirectionalBlock.FACING, Direction.SOUTH));
        // Behind the counter: ledgers on shelves against the vault wall, the clock over the vault door
        b.shelf(1, 2, 8, Blocks.DARK_OAK_SHELF, Direction.NORTH, Items.BOOK, Items.PAPER, Items.BOOK);
        b.shelf(2, 2, 8, Blocks.DARK_OAK_SHELF, Direction.NORTH, Items.INK_SAC, Items.FEATHER, Items.PAPER);
        b.shelf(6, 2, 8, Blocks.DARK_OAK_SHELF, Direction.NORTH, Items.BOOK, Items.WRITABLE_BOOK, Items.BOOK);
        // Vault at the back
        b.fill(1, 1, 9, 7, 4, 9, Blocks.STONE_BRICKS);
        b.door(4, 1, 9, (DoorBlock) Blocks.IRON_DOOR, Direction.NORTH, DoorHingeSide.LEFT);
        b.set(4, 4, 9, Blocks.GLAZED_TERRACOTTA.white());
        b.fill(1, 0, 10, 7, 0, 10, Blocks.SMOOTH_STONE);                  // the vault's stone floor
        b.chest(2, 1, 10, Direction.EAST, ModChestLoot.BANK).vault(2, 1, 10, 2);  // what raiders come for
        b.chest(6, 1, 10, Direction.WEST, ModChestLoot.BANK).vault(6, 1, 10, 2);
        b.set(1, 1, 10, Blocks.GOLD_BLOCK);                               // bullion in the corner, clear of the door
        b.set(7, 1, 10, openBarrel());
        b.set(4, 5, 4, hangingLantern());
        b.set(4, 4, 10, hangingLantern());
        b.entity(ModEntities.TOWNSFOLK.get(), 4, 1, 7, tag("folk", "banker"));
        b.entity(ModEntities.LAWMAN.get(), 6, 1, 4, tag("rank", "deputy"));   // guard in the lobby
        return b;
    }

    // 11 x 12, two storeys: a lobby with a sofa and armchairs, the front desk with its key rack; upstairs two rooms off a
    // back hall (the west one with the door out to the balcony over the street).
    static Blueprint hotel() {
        Blueprint b = new Blueprint("town/hotel");
        int w = 11, h = 9, cx = 5;
        BlockState post = log(Blocks.DARK_OAK_LOG, Direction.Axis.Y);
        shopfront(b, w, 12, h, 2, 5, s(Blocks.BIRCH_PLANKS), post, s(Blocks.BIRCH_PLANKS), Blocks.DARK_OAK_SLAB, Blocks.DARK_OAK_SLAB,
                (DoorBlock) Blocks.DARK_OAK_DOOR);
        TownDecor.cornice(b, w, h, 2, post, Blocks.DARK_OAK_STAIRS);
        TownDecor.shutters(b, w, Blocks.DARK_OAK_TRAPDOOR);
        TownDecor.shopSign(b, cx, 3, "hotel");
        // Balcony on the porch roof, railed, with a door out from the west room
        b.fill(0, 5, 0, 10, 5, 1, Blocks.SPRUCE_PLANKS);
        b.fill(0, 6, 0, 10, 6, 0, Blocks.DARK_OAK_FENCE);
        b.door(4, 6, 2, (DoorBlock) Blocks.DARK_OAK_DOOR, Direction.NORTH, DoorHingeSide.LEFT);
        b.fill(1, 7, 2, 2, 8, 2, Blocks.GLASS_PANE);
        b.fill(7, 7, 2, 8, 8, 2, Blocks.GLASS_PANE);
        b.set(0, 7, 5, Blocks.GLASS_PANE);
        b.set(10, 7, 5, Blocks.GLASS_PANE);
        b.set(0, 7, 0, TownDecor.lamp(false));                            // lamps on the balcony rail's end posts
        b.set(10, 7, 0, TownDecor.lamp(false));
        // Lobby: front desk and key rack, a sofa and armchairs round a low table on a rug, a jukebox
        b.fill(1, 0, 3, 8, 0, 8, Blocks.DARK_OAK_PLANKS);
        b.fill(1, 1, 9, 4, 1, 9, ModBlocks.BAR_COUNTER.get());
        b.set(1, 2, 9, candles(3));
        b.fill(1, 3, 10, 4, 3, 10, pigeonholes(Direction.NORTH));
        TownDecor.rug(b, 2, 1, 4, 5, 7, DyeColor.RED, DyeColor.BROWN);
        b.fill(1, 1, 4, 1, 1, 6, stairs(Blocks.WOOL_STAIRS.pick(DyeColor.GREEN), Direction.WEST, Half.BOTTOM));
        TownDecor.table(b, 3, 1, 5, Blocks.DARK_OAK_FENCE, Blocks.DARK_OAK_PRESSURE_PLATE);
        b.set(7, 1, 4, stairs(Blocks.WOOL_STAIRS.pick(DyeColor.RED), Direction.EAST, Half.BOTTOM));
        b.set(7, 1, 6, stairs(Blocks.WOOL_STAIRS.pick(DyeColor.RED), Direction.EAST, Half.BOTTOM));
        b.set(1, 1, 3, Blocks.POTTED_FERN);
        b.set(6, 1, 9, Blocks.JUKEBOX);                                 // an empty jukebox for the lobby
        b.set(5, 4, 6, hangingLantern());
        // Stairs up the east wall to the first floor at y 5, the last step set in the floor so it's level with it
        b.fill(1, 5, 3, 9, 5, 10, Blocks.SPRUCE_PLANKS);
        b.air(9, 5, 3, 9, 5, 7);
        for (int i = 0; i < 5; i++) {
            b.set(9, 1 + i, 4 + i, stairs(Blocks.SPRUCE_STAIRS, Direction.SOUTH, Half.BOTTOM));
        }
        // Upstairs: the stairwell walled off, two rooms at the front, the hall along the back to the landing
        b.fill(8, 6, 3, 8, 8, 8, Blocks.BIRCH_PLANKS);
        b.fill(1, 6, 8, 7, 8, 8, Blocks.BIRCH_PLANKS);
        b.fill(5, 6, 3, 5, 8, 7, Blocks.BIRCH_PLANKS);
        b.door(2, 6, 8, (DoorBlock) Blocks.DARK_OAK_DOOR, Direction.NORTH, DoorHingeSide.LEFT);
        b.door(7, 6, 8, (DoorBlock) Blocks.DARK_OAK_DOOR, Direction.NORTH, DoorHingeSide.RIGHT);
        // The west room: a double bed, a chest, a washstand, a shelf
        b.bed(2, 6, 4, Blocks.BED.red(), Direction.WEST);
        b.bed(2, 6, 5, Blocks.BED.red(), Direction.WEST);
        b.chest(1, 6, 7, Direction.EAST, ModChestLoot.HOTEL_ROOM);
        b.set(4, 6, 7, water(1));
        b.shelf(1, 7, 6, Blocks.DARK_OAK_SHELF, Direction.EAST, Items.BOOK, Items.CANDLE, Items.GLASS_BOTTLE);
        TownDecor.rug(b, 2, 6, 6, 3, 7, DyeColor.BLUE, DyeColor.BLUE);
        b.set(3, 9, 5, hangingLantern());
        // The east room: a single bed, a chest and a basin
        b.bed(7, 6, 4, Blocks.BED.blue(), Direction.NORTH);
        b.chest(6, 6, 3, Direction.SOUTH, ModChestLoot.HOTEL_ROOM);
        b.set(6, 6, 7, water(1));
        b.set(6, 9, 5, hangingLantern());
        // The hall: linen on a shelf, a fern
        b.shelf(3, 6, 10, Blocks.DARK_OAK_SHELF, Direction.NORTH, Items.WOOL.pick(DyeColor.WHITE), Items.WOOL.pick(DyeColor.WHITE), Items.CANDLE);
        b.set(1, 6, 10, Blocks.POTTED_FERN);
        b.set(5, 9, 9, hangingLantern());
        b.entity(ModEntities.TOWNSFOLK.get(), 2, 1, 10);
        return b;
    }

    // 9 x 10: open-fronted forge with a brick chimney (smoking), anvil and tools on the wall, coal, a wagon wheel for mending.
    static Blueprint blacksmith() {
        Blueprint b = new Blueprint("town/blacksmith");
        int w = 9, d = 10;
        b.air(0, 1, 0, w - 1, 9, d - 1);
        b.fill(0, 0, 0, w - 1, 0, d - 1, Blocks.COBBLESTONE);
        b.fill(0, 0, 0, w - 1, 0, 1, Blocks.SPRUCE_PLANKS);
        b.walls(0, 1, 2, w - 1, 1, d - 1, s(Blocks.COBBLESTONE));
        b.walls(0, 2, 2, w - 1, 4, d - 1, s(Blocks.SPRUCE_PLANKS));     // from the front row, so the opening below clears it
        b.air(1, 1, 2, w - 2, 4, 2);                                      // open front
        for (int x : new int[] {0, w - 1}) b.fill(x, 1, 2, x, 4, 2, log(Blocks.SPRUCE_LOG, Direction.Axis.Y));
        gableRoof(b, w, 2, d - 1, 5, Blocks.DARK_OAK_STAIRS, slab(Blocks.DARK_OAK_SLAB, SlabType.BOTTOM), s(Blocks.SPRUCE_PLANKS));
        b.wallSign(4, 5, 1, Blocks.DARK_OAK_WALL_SIGN, Direction.NORTH, DyeColor.WHITE, false, "", TownDecor.signKey("blacksmith"),
                TownDecor.signKey("blacksmith") + ".sub");
        // Forge and chimney in the back corner
        b.fill(6, 1, 7, 7, 1, 8, Blocks.BRICKS);
        b.set(6, 2, 7, facing(Blocks.BLAST_FURNACE, Direction.NORTH));
        b.fill(7, 2, 8, 7, 9, 8, Blocks.BRICKS);
        b.set(7, 10, 8, Blocks.CAMPFIRE.defaultBlockState().setValue(CampfireBlock.LIT, true));
        TownDecor.chimneyPot(b, 7, 10, 8);
        b.set(4, 1, 5, facing(Blocks.ANVIL, Direction.EAST));
        b.set(2, 1, 8, Blocks.SMITHING_TABLE);
        b.set(1, 1, 8, Blocks.GRINDSTONE.defaultBlockState().setValue(GrindstoneBlock.FACE, AttachFace.FLOOR));
        b.set(1, 1, 4, water(3));                                         // quench tub
        b.chest(4, 1, 8, Direction.NORTH, ModChestLoot.BLACKSMITH);
        // Tools on shelves along the west wall, the coal bin and a wheel waiting on the east
        b.shelf(1, 2, 5, Blocks.SPRUCE_SHELF, Direction.EAST, Items.SHEARS, Items.IRON_NUGGET, Items.BUCKET);
        b.shelf(1, 2, 6, Blocks.SPRUCE_SHELF, Direction.EAST, Items.IRON_CHAIN, Items.FLINT_AND_STEEL, Items.IRON_NUGGET);
        b.shelf(1, 3, 5, Blocks.SPRUCE_SHELF, Direction.EAST, ModItems.IRON_HORSESHOES.get(), Items.IRON_NUGGET, Items.IRON_NUGGET);
        b.set(7, 1, 4, openBarrel());
        b.set(7, 1, 5, openBarrel());
        b.set(7, 2, 4, Blocks.DARK_OAK_TRAPDOOR.defaultBlockState().setValue(TrapDoorBlock.FACING, Direction.WEST).setValue(TrapDoorBlock.OPEN, true));
        b.set(4, 4, 5, hangingLantern());
        b.set(0, 1, 0, Blocks.SPRUCE_FENCE);                              // a horse waits at the rail out front
        b.set(1, 1, 0, Blocks.SPRUCE_FENCE);
        b.set(8, 1, 1, Blocks.ANVIL.defaultBlockState().setValue(HorizontalDirectionalBlock.FACING, Direction.NORTH));
        b.entity(ModEntities.TOWNSFOLK.get(), 4, 1, 6, tag("folk", "blacksmith"));
        b.cat(5, 1, 7, "smithy");                                         // warming itself by the forge
        front(b, 4, s(Blocks.SPRUCE_PLANKS));
        return b;
    }

    // 11 x 12 barn: stalls with two horses, hay, trough, tack on shelves and a chest.
    static Blueprint livery() {
        Blueprint b = new Blueprint("town/livery");
        int w = 11, d = 12;
        b.air(0, 1, 0, w - 1, 11, d - 1);
        b.fill(0, 0, 0, w - 1, 0, d - 1, Blocks.COARSE_DIRT);
        b.fill(0, 0, 0, w - 1, 0, 1, Blocks.SPRUCE_PLANKS);
        b.walls(0, 1, 2, w - 1, 5, d - 1, s(Blocks.SPRUCE_PLANKS));
        for (int[] c : new int[][] {{0, 2}, {w - 1, 2}, {0, d - 1}, {w - 1, d - 1}, {0, 7}, {w - 1, 7}}) {
            b.fill(c[0], 1, c[1], c[0], 5, c[1], log(Blocks.DARK_OAK_LOG, Direction.Axis.Y));
        }
        gableRoof(b, w, 2, d - 1, 6, Blocks.DARK_OAK_STAIRS, s(Blocks.DARK_OAK_PLANKS), s(Blocks.SPRUCE_PLANKS));
        b.air(4, 1, 2, 6, 3, 2);                                          // big barn door, open
        for (int x : new int[] {3, 7}) {                                  // its leaves, swung back against the front
            b.fill(x, 1, 1, x, 3, 1, Blocks.SPRUCE_TRAPDOOR.defaultBlockState().setValue(TrapDoorBlock.FACING, Direction.NORTH).setValue(TrapDoorBlock.OPEN, true));
        }
        b.wallSign(5, 4, 1, Blocks.DARK_OAK_WALL_SIGN, Direction.NORTH, DyeColor.WHITE, false, "", TownDecor.signKey("livery"),
                TownDecor.signKey("livery") + ".sub");
        b.set(5, 6, 2, Blocks.HAY_BLOCK);                                 // hay loft door
        b.set(5, 7, 2, Blocks.SPRUCE_TRAPDOOR.defaultBlockState().setValue(TrapDoorBlock.FACING, Direction.NORTH).setValue(TrapDoorBlock.OPEN, true));
        b.set(5, 8, 1, Blocks.SPRUCE_FENCE);                              // the hay hoist
        b.set(5, 7, 1, Blocks.IRON_CHAIN);
        // Stalls along the west wall: fence partitions and gates
        for (int z : new int[] {3, 6, 9}) b.fill(1, 1, z, 2, 1, z, Blocks.SPRUCE_FENCE);
        for (int z : new int[] {4, 5, 7, 8}) {
            b.set(3, 1, z, Blocks.SPRUCE_FENCE_GATE.defaultBlockState().setValue(FenceGateBlock.FACING, Direction.EAST));
        }
        b.set(1, 1, 4, Blocks.HAY_BLOCK);
        b.set(1, 1, 8, Blocks.HAY_BLOCK);
        b.entity(EntityTypes.HORSE, 2, 1, 5);
        b.entity(EntityTypes.HORSE, 2, 1, 7);
        // East side: hay stack, trough, tack on shelves over it, the tack chest
        b.fill(8, 1, 9, 9, 2, 10, Blocks.HAY_BLOCK);
        b.set(9, 1, 4, water(3));
        b.set(9, 1, 5, water(3));
        b.shelf(9, 3, 4, Blocks.SPRUCE_SHELF, Direction.WEST, Items.LEAD, Items.SADDLE, Items.LEAD);
        b.shelf(9, 3, 5, Blocks.SPRUCE_SHELF, Direction.WEST, Items.WHEAT, Items.APPLE, Items.CARROT);
        b.chest(9, 1, 7, Direction.WEST, ModChestLoot.LIVERY);
        b.set(9, 1, 8, openBarrel());
        b.set(7, 1, 3, Blocks.COMPOSTER);
        b.fill(1, 6, 6, w - 2, 6, 6, log(Blocks.STRIPPED_DARK_OAK_LOG, Direction.Axis.X)); // tie beams, the lamps hung from them
        b.fill(1, 6, 9, w - 2, 6, 9, log(Blocks.STRIPPED_DARK_OAK_LOG, Direction.Axis.X));
        b.set(5, 5, 6, hangingLantern());
        b.set(5, 5, 9, hangingLantern());
        b.entity(ModEntities.TOWNSFOLK.get(), 6, 1, 8, tag("folk", "wrangler"));
        b.cat(8, 3, 9, "livery");                                         // the barn cat, on top of the hay stack
        b.fill(0, 1, 0, 0, 2, 0, Blocks.SPRUCE_FENCE);                    // hitching post out front
        b.fill(10, 1, 0, 10, 2, 0, Blocks.SPRUCE_FENCE);
        b.fill(1, 2, 0, 2, 2, 0, Blocks.SPRUCE_FENCE);                    // ...and its rail
        b.fill(8, 2, 0, 9, 2, 0, Blocks.SPRUCE_FENCE);
        b.set(9, 1, 1, water(3));
        front(b, 5, s(Blocks.SPRUCE_PLANKS));
        return b;
    }

    // 7 x 10, dark and narrow: coffins on trestles and stood against the wall, candles, cobwebs, flowers for the burying.
    static Blueprint undertaker() {
        Blueprint b = new Blueprint("town/undertaker");
        shopfront(b, 7, 10, 4, 2, 4, s(Blocks.DARK_OAK_PLANKS), log(Blocks.DARK_OAK_LOG, Direction.Axis.Y),
                s(Blocks.DARK_OAK_PLANKS), Blocks.POLISHED_BLACKSTONE_SLAB, Blocks.DARK_OAK_SLAB, (DoorBlock) Blocks.DARK_OAK_DOOR);
        TownDecor.cornice(b, 7, 4, 2, log(Blocks.DARK_OAK_LOG, Direction.Axis.Y), Blocks.DARK_OAK_STAIRS);
        TownDecor.shopSign(b, 3, 3, "undertaker");
        BlockState lid = Blocks.SPRUCE_TRAPDOOR.defaultBlockState().setValue(TrapDoorBlock.HALF, Half.TOP);
        for (int x : new int[] {1, 5}) {
            b.fill(x, 1, 4, x, 1, 5, lid);                                // coffins on the floor
        }
        BlockState standing = Blocks.SPRUCE_TRAPDOOR.defaultBlockState().setValue(TrapDoorBlock.FACING, Direction.NORTH).setValue(TrapDoorBlock.OPEN, true);
        b.fill(2, 1, 8, 2, 2, 8, standing);                               // stood against the back wall
        b.fill(4, 1, 8, 4, 2, 8, standing);
        b.set(3, 1, 8, Blocks.BARREL);
        b.set(3, 2, 8, candles(3));
        b.chest(5, 1, 8, Direction.WEST, ModChestLoot.UNDERTAKER);
        b.shelf(1, 2, 7, Blocks.DARK_OAK_SHELF, Direction.EAST, Items.POPPY, Items.CANDLE, Items.BOOK);
        b.shelf(5, 2, 6, Blocks.DARK_OAK_SHELF, Direction.WEST, Items.LILY_OF_THE_VALLEY, Items.WITHER_ROSE, Items.LILY_OF_THE_VALLEY);
        b.set(1, 1, 7, Blocks.POTTED_WITHER_ROSE);
        b.set(1, 4, 8, Blocks.COBWEB);
        b.set(5, 4, 3, Blocks.COBWEB);
        b.set(1, 1, 3, candles(1));
        b.set(3, 4, 5, hangingLantern());
        b.set(5, 1, 0, Blocks.SPRUCE_TRAPDOOR.defaultBlockState().setValue(TrapDoorBlock.FACING, Direction.SOUTH).setValue(TrapDoorBlock.OPEN, true)); // a new pine box out front
        b.cat(3, 1, 5, "ghost");                                          // rarely, a pale cat among the coffins (by night)
        return b;
    }

    // 7 x 12 clapboard house: gable roof, porch with a rocking chair; a stove, a table for two, a bed and a chest; out back
    // the woodpile, a rain barrel and the outhouse.
    static Blueprint house() {
        Blueprint b = new Blueprint("town/house");
        int w = 7, d = 9;
        b.air(0, 1, 0, w - 1, 9, d + 2);
        b.fill(0, 0, 0, w - 1, 0, d - 1, Blocks.SPRUCE_PLANKS);
        b.fill(0, 0, d, w - 1, 0, d + 2, Blocks.COARSE_DIRT);
        b.walls(0, 1, 2, w - 1, 4, d - 1, s(Blocks.BIRCH_PLANKS));
        for (int[] c : new int[][] {{0, 2}, {w - 1, 2}, {0, d - 1}, {w - 1, d - 1}}) {
            b.fill(c[0], 1, c[1], c[0], 4, c[1], log(Blocks.STRIPPED_BIRCH_LOG, Direction.Axis.Y));
        }
        gableRoof(b, w, 2, d - 1, 5, Blocks.SPRUCE_STAIRS, slab(Blocks.SPRUCE_SLAB, SlabType.BOTTOM), s(Blocks.BIRCH_PLANKS));
        b.door(3, 1, 2, (DoorBlock) Blocks.SPRUCE_DOOR, Direction.NORTH, DoorHingeSide.LEFT);
        b.set(1, 2, 2, Blocks.GLASS_PANE);
        b.set(5, 2, 2, Blocks.GLASS_PANE);
        BlockState shutter = Blocks.SPRUCE_TRAPDOOR.defaultBlockState().setValue(TrapDoorBlock.FACING, Direction.NORTH).setValue(TrapDoorBlock.OPEN, true);
        b.set(0, 2, 1, shutter);
        b.set(6, 2, 1, shutter);
        b.set(0, 2, 5, Blocks.GLASS_PANE);
        b.set(w - 1, 2, 5, Blocks.GLASS_PANE);
        b.set(3, 3, 8, Blocks.GLASS_PANE);
        // Porch
        b.fill(0, 1, 0, 0, 3, 0, Blocks.SPRUCE_FENCE);
        b.fill(w - 1, 1, 0, w - 1, 3, 0, Blocks.SPRUCE_FENCE);
        b.fill(0, 4, 0, w - 1, 4, 1, slab(Blocks.SPRUCE_SLAB, SlabType.BOTTOM));
        b.set(1, 1, 1, stairs(Blocks.OAK_STAIRS, Direction.SOUTH, Half.BOTTOM)); // rocking chair
        b.set(5, 1, 1, Blocks.POTTED_CACTUS);
        b.set(3, 3, 1, TownDecor.lamp(true));                            // the porch lamp, lit at dusk
        // Inside: the bed and a chest at its foot, a table for two, the stove and a dresser with the crockery
        b.bed(1, 1, 6, Blocks.BED.brown(), Direction.SOUTH);
        b.chest(1, 1, 5, Direction.EAST, ModChestLoot.TOWN_HOUSE);
        TownDecor.table(b, 2, 1, 3, Blocks.OAK_FENCE, Blocks.OAK_PRESSURE_PLATE);
        chair(b, 1, 1, 3, Blocks.OAK_STAIRS, Direction.WEST);
        TownDecor.stove(b, 5, 1, 6, Direction.WEST, 7);
        b.set(5, 1, 7, Blocks.CRAFTING_TABLE);
        b.set(5, 1, 4, barrel(Direction.WEST));
        b.set(5, 1, 3, water(1));                                         // the wash basin
        b.shelf(5, 2, 4, Blocks.SPRUCE_SHELF, Direction.WEST, Items.BOWL, Items.BREAD, Items.BOWL);
        b.shelf(5, 2, 7, Blocks.SPRUCE_SHELF, Direction.WEST, Items.CANDLE, Items.BOOK, Items.CLOCK);
        TownDecor.rug(b, 2, 1, 5, 4, 7, DyeColor.ORANGE, DyeColor.BROWN);
        b.set(3, 4, 5, hangingLantern());
        // Out back: the back door, the woodpile, a rain barrel, the outhouse
        TownDecor.backDoor(b, 2, d, (DoorBlock) Blocks.SPRUCE_DOOR);
        TownDecor.woodpile(b, 0, d, 2);
        TownDecor.rainBarrel(b, 0, d + 2);
        TownDecor.outhouse(b, 3, d, s(Blocks.SPRUCE_PLANKS), (DoorBlock) Blocks.SPRUCE_DOOR);
        b.cat(2, 1, 1, "home");                                           // by the rocking chair
        front(b, 3, s(Blocks.SPRUCE_PLANKS));
        return b;
    }

    // 7 x 12 adobe: mud brick walls, a flat roof with vigas poking out, a ramada out front; inside a stove, a table, a bed,
    // a rug; out back the beehive oven (a horno) and a woodpile.
    static Blueprint adobe() {
        Blueprint b = new Blueprint("town/adobe");
        int w = 7, d = 8;
        b.air(0, 1, 0, w - 1, 7, d + 3);
        b.fill(0, 0, 0, w - 1, 0, d + 3, Blocks.COARSE_DIRT);
        b.fill(0, 0, 2, w - 1, 0, d - 1, Blocks.TERRACOTTA);
        b.walls(0, 1, 2, w - 1, 4, d - 1, s(Blocks.MUD_BRICKS));
        b.fill(0, 5, 2, w - 1, 5, d - 1, slab(Blocks.MUD_BRICK_SLAB, SlabType.BOTTOM));
        b.walls(0, 5, 2, w - 1, 5, d - 1, s(Blocks.MUD_BRICK_WALL));      // parapet
        for (int x : new int[] {1, 3, 5}) {
            b.set(x, 4, 1, log(Blocks.STRIPPED_OAK_LOG, Direction.Axis.Z)); // vigas
        }
        b.door(3, 1, 2, (DoorBlock) Blocks.SPRUCE_DOOR, Direction.NORTH, DoorHingeSide.LEFT);
        b.set(1, 2, 2, Blocks.SPRUCE_TRAPDOOR.defaultBlockState().setValue(TrapDoorBlock.FACING, Direction.NORTH).setValue(TrapDoorBlock.OPEN, true));
        b.set(5, 2, 2, Blocks.SPRUCE_TRAPDOOR.defaultBlockState().setValue(TrapDoorBlock.FACING, Direction.NORTH).setValue(TrapDoorBlock.OPEN, true));
        // Ramada: posts and a slatted shade over the doorway, a bench in its shade
        b.fill(0, 1, 0, 0, 3, 0, Blocks.STRIPPED_OAK_LOG);
        b.fill(w - 1, 1, 0, w - 1, 3, 0, Blocks.STRIPPED_OAK_LOG);
        b.fill(0, 4, 0, w - 1, 4, 0, slab(Blocks.SPRUCE_SLAB, SlabType.BOTTOM));
        b.set(1, 1, 0, Blocks.POTTED_CACTUS);
        b.set(5, 1, 1, Blocks.DECORATED_POT);
        b.set(4, 1, 1, stairs(Blocks.SPRUCE_STAIRS, Direction.SOUTH, Half.BOTTOM));
        // Inside: the stove in the corner, a bed and chest, a table and stool, a woven rug
        TownDecor.stove(b, 5, 1, 6, Direction.WEST, 6);
        b.bed(1, 1, 5, Blocks.BED.orange(), Direction.SOUTH);
        b.chest(5, 1, 3, Direction.WEST, ModChestLoot.TOWN_HOUSE);
        TownDecor.table(b, 5, 1, 4, Blocks.SPRUCE_FENCE, Blocks.SPRUCE_PRESSURE_PLATE);
        b.set(4, 1, 4, stool());
        b.shelf(1, 2, 3, Blocks.SPRUCE_SHELF, Direction.EAST, Items.BOWL, Items.BEETROOT, Items.BREAD);
        TownDecor.rug(b, 2, 1, 4, 4, 6, DyeColor.RED, DyeColor.ORANGE);
        b.set(3, 4, 4, hangingLantern());
        // Out back: the horno (a mud dome with its mouth to the west) and firewood
        b.fill(4, 1, d + 1, 6, 1, d + 3, Blocks.MUD_BRICKS);
        b.fill(4, 2, d + 1, 6, 2, d + 3, slab(Blocks.MUD_BRICK_SLAB, SlabType.BOTTOM));
        b.set(5, 2, d + 2, Blocks.MUD_BRICKS);
        b.set(5, 3, d + 2, slab(Blocks.MUD_BRICK_SLAB, SlabType.BOTTOM));
        b.set(4, 1, d + 2, facing(Blocks.SMOKER, Direction.WEST));
        TownDecor.woodpile(b, 0, d + 1, 2);
        b.cat(2, 1, 1, "home");                                           // in the ramada's shade
        front(b, 3, s(Blocks.COARSE_DIRT));
        return b;
    }

    // ---------------------------------------------------------------- landmarks (north end of the street)

    // 9 x 15 white clapboard chapel: pews, altar, bell tower with a cross.
    static Blueprint chapel() {
        Blueprint b = new Blueprint("town/chapel");
        int w = 9, d = 15;
        b.air(0, 1, 0, w - 1, 15, d - 1);
        b.fill(0, 0, 0, w - 1, 0, d - 1, Blocks.SPRUCE_PLANKS);
        b.fill(3, 0, 0, 5, 0, 0, Blocks.STONE_BRICKS);
        b.walls(0, 1, 1, w - 1, 5, d - 1, s(Blocks.BIRCH_PLANKS));
        for (int[] c : new int[][] {{0, 1}, {w - 1, 1}, {0, d - 1}, {w - 1, d - 1}}) {
            b.fill(c[0], 1, c[1], c[0], 5, c[1], log(Blocks.STRIPPED_BIRCH_LOG, Direction.Axis.Y));
        }
        gableRoof(b, w, 1, d - 1, 6, Blocks.DARK_OAK_STAIRS, s(Blocks.DARK_OAK_PLANKS), s(Blocks.BIRCH_PLANKS));
        b.door(4, 1, 1, (DoorBlock) Blocks.SPRUCE_DOOR, Direction.NORTH, DoorHingeSide.LEFT);
        b.set(4, 4, 1, Blocks.GLASS_PANE);
        for (int z = 4; z < d - 2; z += 3) {
            b.fill(0, 2, z, 0, 4, z, Blocks.STAINED_GLASS_PANE.yellow());
            b.fill(w - 1, 2, z, w - 1, 4, z, Blocks.STAINED_GLASS_PANE.yellow());
        }
        // Bell tower over the door: open belfry with the bell hung from its ceiling, a cross on top
        b.walls(3, 7, 1, 5, 10, 3, s(Blocks.BIRCH_PLANKS));
        b.fill(3, 9, 1, 5, 9, 3, Blocks.AIR);
        for (int[] c : new int[][] {{3, 1}, {5, 1}, {3, 3}, {5, 3}}) b.set(c[0], 9, c[1], log(Blocks.STRIPPED_BIRCH_LOG, Direction.Axis.Y));
        b.fill(3, 10, 1, 5, 10, 3, Blocks.DARK_OAK_PLANKS);
        b.set(4, 9, 2, Blocks.BELL.defaultBlockState().setValue(BellBlock.ATTACHMENT, BellAttachType.CEILING).setValue(BellBlock.FACING, Direction.NORTH));
        b.fill(3, 11, 1, 5, 11, 3, slab(Blocks.DARK_OAK_SLAB, SlabType.BOTTOM));
        b.fill(4, 11, 2, 4, 13, 2, Blocks.DARK_OAK_FENCE);
        b.set(3, 12, 2, Blocks.DARK_OAK_FENCE);
        b.set(5, 12, 2, Blocks.DARK_OAK_FENCE);
        // Pews either side of a red aisle, altar at the back with flowers and candles, hymn books on a shelf
        b.fill(4, 1, 2, 4, 1, d - 4, Blocks.CARPET.red());
        for (int z = 4; z <= 10; z += 2) {
            b.fill(1, 1, z, 3, 1, z, stairs(Blocks.SPRUCE_STAIRS, Direction.NORTH, Half.BOTTOM));
            b.fill(5, 1, z, 7, 1, z, stairs(Blocks.SPRUCE_STAIRS, Direction.NORTH, Half.BOTTOM));
        }
        b.fill(2, 1, d - 2, 6, 1, d - 2, Blocks.SPRUCE_PLANKS);
        b.set(4, 1, d - 3, facing(Blocks.LECTERN, Direction.NORTH));
        b.entity(ModEntities.TOWNSFOLK.get(), 5, 1, d - 3, tag("folk", "preacher")); // the preacher by his lectern (24c)
        b.set(2, 2, d - 2, candles(3));
        b.set(6, 2, d - 2, candles(3));
        b.set(3, 2, d - 2, Blocks.POTTED_LILY_OF_THE_VALLEY);
        b.set(5, 2, d - 2, Blocks.POTTED_LILY_OF_THE_VALLEY);
        b.shelf(1, 2, 2, Blocks.SPRUCE_SHELF, Direction.EAST, Items.BOOK, Items.BOOK, Items.BOOK);
        b.set(4, 5, 7, hangingLantern());
        b.set(4, 5, 11, hangingLantern());
        front(b, 4, s(Blocks.STONE_BRICKS));
        return b;
    }

    // 7 x 7 water tower on four log legs, with a ladder, beside a well trough.
    static Blueprint waterTower() {
        Blueprint b = new Blueprint("town/water_tower");
        int w = 7;
        b.air(0, 1, 0, w - 1, 14, w - 1);
        b.fill(0, 0, 0, w - 1, 0, w - 1, Blocks.COARSE_DIRT);
        b.fill(2, 0, 2, 4, 0, 4, Blocks.GRAVEL);
        for (int[] c : new int[][] {{1, 1}, {5, 1}, {1, 5}, {5, 5}}) {
            b.fill(c[0], 1, c[1], c[0], 7, c[1], log(Blocks.SPRUCE_LOG, Direction.Axis.Y));
        }
        // Cross bracing
        b.fill(2, 4, 1, 4, 4, 1, Blocks.SPRUCE_FENCE);
        b.fill(2, 4, 5, 4, 4, 5, Blocks.SPRUCE_FENCE);
        b.fill(1, 4, 2, 1, 4, 4, Blocks.SPRUCE_FENCE);
        b.fill(5, 4, 2, 5, 4, 4, Blocks.SPRUCE_FENCE);
        // Deck and tank
        b.fill(0, 8, 0, w - 1, 8, w - 1, Blocks.SPRUCE_SLAB);
        b.walls(1, 9, 1, 5, 12, 5, s(Blocks.SPRUCE_PLANKS));
        for (int y : new int[] {10, 12}) b.walls(1, y, 1, 5, y, 5, s(Blocks.STRIPPED_SPRUCE_LOG)); // hoops
        b.fill(2, 9, 2, 4, 11, 4, Blocks.WATER);
        for (int i = 0; i < 2; i++) {                                    // conical cap
            b.walls(1 + i, 13 + i, 1 + i, 5 - i, 13 + i, 5 - i, slab(Blocks.DARK_OAK_SLAB, SlabType.BOTTOM));
        }
        b.fill(2, 13, 2, 4, 13, 4, Blocks.DARK_OAK_PLANKS);
        b.set(3, 15, 3, Blocks.DARK_OAK_SLAB);
        // Ladder up the front-left leg
        for (int y = 1; y <= 8; y++) b.set(1, y, 0, Blocks.LADDER.defaultBlockState().setValue(LadderBlock.FACING, Direction.NORTH));
        b.set(1, 8, 1, Blocks.AIR);
        // Trough and pump at the foot
        b.set(3, 1, 3, water(3));
        b.set(3, 1, 4, water(3));
        b.set(4, 1, 3, Blocks.HAY_BLOCK);
        front(b, 3, s(Blocks.COARSE_DIRT));
        return b;
    }
}
