package com.hollowfrontier.worldgen.structure;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import com.hollowfrontier.registry.ModBlocks;
import com.hollowfrontier.registry.ModEntities;
import com.hollowfrontier.registry.ModItems;

import net.minecraft.core.Direction;
import net.minecraft.core.FrontAndTop;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.network.chat.Component;
import net.minecraft.server.network.Filterable;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.WrittenBookContent;
import net.minecraft.world.level.block.AmethystClusterBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.CampfireBlock;
import net.minecraft.world.level.block.CandleBlock;
import net.minecraft.world.level.block.LadderBlock;
import net.minecraft.world.level.block.LanternBlock;
import net.minecraft.world.level.block.LeavesBlock;
import net.minecraft.world.level.block.LecternBlock;
import net.minecraft.world.level.block.RailBlock;
import net.minecraft.world.level.block.RotatedPillarBlock;
import net.minecraft.world.level.block.SlabBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.RailShape;
import net.minecraft.world.level.block.state.properties.SlabType;

/**
 * The Hollow Earth's wild sites (27), so every biome has finds of its own, each one piece of a {@link HollowSiteStructure}
 * (the mine with its cavern under it) with its floor ({@link HollowBlueprints#FLOOR}) on the ground:
 * <ul>
 * <li><b>Primordial Jungle</b>: the <b>Overgrown Ziggurat</b>, a stepped pyramid of mossy stone under leaves and vines, a
 *     stair up its face to a shrine on top and a passage in at its foot to the idol chamber in its heart; and the <b>Tar
 *     Pit</b>, a black pool with megafauna skeletons sunk in it, a dead prospector's camp at its edge and his notes.</li>
 * <li><b>Crystal Steppes</b>: the <b>Abandoned Phlogiston Mine</b>, a headframe over a shaft, a bunkhouse, an ore cart on its
 *     rails and the spoil heaped by it, the shaft down to a crystal cavern where the steam drill broke; and the <b>Kindled
 *     Observatory</b>, a round tower of three floors over a star chart, a great lens on its top turned up at the sun.</li>
 * <li><b>Ember Badlands</b>: the <b>Starsteel Foundry</b>, a roofless hall of blackstone with its crucibles still
 *     glowing, a channel of cooled lava to the molds, the workshop with a starsteel anvil, slag heaped outside.</li>
 * <li><b>Floating islands</b>: the <b>Sky Shrine</b>, a ring of crystal pillars on an island's top round a Kindled waystone
 *     (12a's network), and a bridge out from it that ends in the air.</li>
 * <li><b>Under the crust</b>: the <b>Gray Relay</b>, hung from the ceiling on an iron pylon: a lit deck, a dish pointing
 *     down, a probe on watch (placed by {@link com.hollowfrontier.worldgen.feature.GrayRelayFeature}).</li>
 * </ul>
 */
public final class WildBlueprints {
    private WildBlueprints() {}

    public static final String ZIGGURAT = "overgrown_ziggurat";
    public static final String TAR_PIT = "tar_pit";
    public static final String PHLOGISTON_MINE = "phlogiston_mine";
    public static final String MINE_CAVERN = "phlogiston_mine/cavern";
    public static final String OBSERVATORY = "kindled_observatory";
    public static final String FOUNDRY = "starsteel_foundry";
    public static final String SKY_SHRINE = "sky_shrine";
    public static final String GRAY_RELAY = "gray_relay";
    /** The prospector's notes at the tar pit: {@code book.hollowfrontier.prospector_notes.<n>}. */
    public static final int NOTES_PAGES = 4;
    /** How deep the mine's shaft goes before the cavern opens. */
    public static final int MINE_SHAFT = 12;

    public static List<Blueprint> all() {
        return List.of(ziggurat(), tarPit(), phlogistonMine(), mineCavern(), observatory(), foundry(), skyShrine(), grayRelay());
    }

    private static BlockState jungleLeaves() {
        return Blocks.JUNGLE_LEAVES.defaultBlockState().setValue(LeavesBlock.PERSISTENT, true);
    }

    private static BlockState mossy(Random r) {
        int n = r.nextInt(10);
        return (n < 5 ? Blocks.MOSSY_STONE_BRICKS : n < 7 ? Blocks.STONE_BRICKS : n < 9 ? Blocks.CRACKED_STONE_BRICKS : Blocks.MOSSY_COBBLESTONE).defaultBlockState();
    }

    // ---------------------------------------------------------------- the Primordial Jungle

    /**
     * 25 x 25: four tiers of mossy stone (each three high, stepping in three), a stair up the south face to the shrine on
     * top (crystal pillars under a slab roof, a phlogiston lamp and an offering), and a passage in at the foot of the north face
     * to the idol chamber in the heart (x 8..16, z 8..16): the idol on its plinth (dark stone, a crystal body, a
     * starsteel head with lit eyes), pots round it, the chest, candles, webs; vines and leaves over all of it.
     */
    static Blueprint ziggurat() {
        Blueprint b = new Blueprint(ZIGGURAT);
        Random r = new Random(2701);
        int s = HollowBlueprints.FLOOR;
        b.air(0, s + 1, 0, 24, s + 18, 24);
        for (int tier = 0; tier < 4; tier++) {
            int m = tier * 3, y0 = s + tier * 3, y1 = y0 + 2 + (tier == 0 ? 1 : 0);
            for (int x = m; x <= 24 - m; x++) {
                for (int z = m; z <= 24 - m; z++) {
                    for (int y = Math.max(y0, s); y <= y1; y++) {
                        boolean band = y == y1 && (x == m || x == 24 - m || z == m || z == 24 - m);
                        b.set(x, y, z, band ? Blocks.CHISELED_STONE_BRICKS.defaultBlockState() : mossy(r));
                    }
                }
            }
        }
        // The stair up the south face, from the ground to the top (y 12)
        int top = s + 11; // the top tier's top
        for (int k = 0; k <= 10; k++) { // up to the top, landing on it at z 13
            int z = 24 - k, y = s + 1 + k;
            for (int x = 11; x <= 13; x++) {
                b.fill(x, s, z, x, y - 1, z, Blocks.STONE_BRICKS);
                b.set(x, y, z, Masonry.stair(Blocks.MOSSY_STONE_BRICK_STAIRS, Direction.NORTH));
                b.air(x, y + 1, z, x, y + 3, z);
            }
        }
        // The shrine on top (x 9..15, z 9..15)
        for (int[] p : new int[][] {{9, 9}, {15, 9}, {9, 15}, {15, 15}}) {
            b.fill(p[0], top + 1, p[1], p[0], top + 4, p[1], Masonry.crystalBricks());
            b.set(p[0], top + 5, p[1], Masonry.chiseledCrystal());
        }
        b.fill(9, top + 6, 9, 15, top + 6, 15, Blocks.MOSSY_STONE_BRICK_SLAB.defaultBlockState().setValue(SlabBlock.TYPE, SlabType.BOTTOM));
        b.fill(11, top + 1, 11, 13, top + 1, 13, Blocks.CHISELED_STONE_BRICKS);
        b.set(12, top + 2, 12, Masonry.phlogistonLamp());
        b.pot(11, top + 2, 11, Direction.SOUTH, ModChestLoot.KINDLED_OFFERING);
        b.set(13, top + 2, 13, Blocks.CANDLE.defaultBlockState().setValue(CandleBlock.CANDLES, 3).setValue(CandleBlock.LIT, true));
        // The passage in at the north foot, and the idol chamber in the heart
        b.air(11, s + 1, 0, 13, s + 3, 8);
        b.fill(10, s + 4, 0, 14, s + 4, 0, Blocks.CHISELED_STONE_BRICKS);
        b.air(8, s + 1, 8, 16, s + 6, 16);
        b.fill(8, s, 8, 16, s, 16, Blocks.POLISHED_DEEPSLATE);
        b.fill(10, s, 10, 14, s, 14, Masonry.crystalBricks());
        b.set(12, s + 1, 12, Blocks.CHISELED_DEEPSLATE);
        b.set(12, s + 2, 12, Blocks.POLISHED_DEEPSLATE);
        b.set(12, s + 3, 12, Masonry.chiseledCrystal());
        b.set(12, s + 4, 12, Masonry.starsteel());
        b.set(11, s + 4, 12, Masonry.phlogistonLamp()); // its eyes
        b.set(13, s + 4, 12, Masonry.phlogistonLamp());
        for (int[] p : new int[][] {{9, 9}, {15, 9}, {9, 15}, {15, 15}}) {
            b.pot(p[0], s + 1, p[1], Direction.NORTH, p[0] == 15 && p[1] == 15 ? ModChestLoot.KINDLED_OFFERING : null);
            b.set(p[0], s + 6, p[1], Blocks.COBWEB);
        }
        b.chest(12, s + 1, 15, Direction.NORTH, ModChestLoot.ZIGGURAT);
        b.suspicious(9, s, 12, Blocks.SUSPICIOUS_SAND, ModChestLoot.DIG_ZIGGURAT); // sand drifted into the corners (28)
        b.suspicious(15, s, 12, Blocks.SUSPICIOUS_SAND, ModChestLoot.DIG_ZIGGURAT);
        b.set(10, s + 1, 12, Blocks.CANDLE.defaultBlockState().setValue(CandleBlock.CANDLES, 4).setValue(CandleBlock.LIT, true));
        b.set(14, s + 1, 12, Blocks.CANDLE.defaultBlockState().setValue(CandleBlock.CANDLES, 4).setValue(CandleBlock.LIT, true));
        // The jungle taking it back: leaves on the ledges, vines down the faces, moss on the floor
        for (int i = 0; i < 70; i++) {
            int x = r.nextInt(25), z = r.nextInt(25), y = Masonry.top(b, x, z, s + 20);
            boolean stair = x >= 11 && x <= 13 && z >= 13;
            if (y >= s && !stair && !(x >= 9 && x <= 15 && z >= 9 && z <= 15)) {
                b.set(x, y + 1, z, jungleLeaves());
                if (r.nextBoolean()) {
                    b.set(x, y + 2, z, jungleLeaves());
                }
            }
        }
        Masonry.vines(b, r, 0, s + 1, 0, 24, s + 14, 24, 0.12);
        Masonry.carpet(b, r, 8, 0, 16, 16, s + 1, Blocks.MOSS_CARPET, 0.15);
        return b;
    }

    /**
     * 31 x 31: the tar pit (centre (14, 15), black and glossy in the grass), the skeletons sunk in it (a mammoth's spine
     * and ribs with a skull and tusks clear of the tar, the ribs of another just showing, a terror bird at the edge), and on
     * its south-east bank the prospector's camp: a tent fallen in, a cold fire, his barrels and pick, and his notes on a
     * lectern with the chest beside it.
     */
    static Blueprint tarPit() {
        Blueprint b = new Blueprint(TAR_PIT);
        Random r = new Random(2702);
        int s = HollowBlueprints.FLOOR, cx = 14, cz = 15;
        b.air(0, s + 1, 0, 30, s + 10, 30);
        for (int x = 0; x <= 30; x++) {
            for (int z = 0; z <= 30; z++) {
                double d = Math.hypot((x - cx) / 1.2, z - cz);
                if (d <= 9) {
                    b.set(x, s, z, r.nextInt(5) == 0 ? Blocks.MUD.defaultBlockState() : Blocks.CONCRETE.black().defaultBlockState());
                } else if (d <= 10.5 && r.nextInt(3) != 0) {
                    b.set(x, s, z, r.nextBoolean() ? Blocks.MUD.defaultBlockState() : Blocks.COARSE_DIRT.defaultBlockState());
                }
            }
        }
        // The mammoth: a spine east-west, ribs arching up either side, its skull and tusks at the west end
        BlockState spine = Blocks.BONE_BLOCK.defaultBlockState().setValue(RotatedPillarBlock.AXIS, Direction.Axis.X);
        b.fill(10, s + 2, cz, 18, s + 2, cz, spine);
        for (int x = 11; x <= 17; x += 2) {
            for (int side : new int[] {-1, 1}) {
                b.set(x, s + 2, cz + side, Blocks.BONE_BLOCK);
                b.set(x, s + 1, cz + side * 2, Blocks.BONE_BLOCK);
            }
            b.set(x, s + 1, cz, Blocks.BONE_BLOCK);
        }
        b.fill(7, s + 1, cz - 1, 9, s + 3, cz + 1, Blocks.BONE_BLOCK);
        b.set(6, s + 2, cz, Blocks.SKELETON_SKULL);
        for (int side : new int[] {-1, 1}) { // the tusks, curling up and back
            b.set(6, s + 1, cz + side * 2, Blocks.CALCITE);
            b.set(5, s + 2, cz + side * 2, Blocks.CALCITE);
            b.set(5, s + 3, cz + side * 3, Blocks.CALCITE);
        }
        // Another, sunk all but its ribs; a terror bird at the edge
        for (int z = cz + 4; z <= cz + 7; z++) {
            b.set(19, s + 1, z, Blocks.BONE_BLOCK);
            b.set(21, s + 1, z, Blocks.BONE_BLOCK);
            if (z % 2 == 0) {
                b.set(20, s + 2, z, Blocks.BONE_BLOCK);
            }
        }
        b.fill(14, s + 1, cz - 8, 14, s + 2, cz - 8, Blocks.BONE_BLOCK);
        b.set(14, s + 3, cz - 8, Blocks.SKELETON_SKULL);
        b.set(15, s + 1, cz - 8, Blocks.BONE_BLOCK);
        // The prospector's camp on the south-east bank
        int px = 23, pz = 25;
        b.fill(px - 3, s, pz - 3, px + 4, s, pz + 3, Blocks.COARSE_DIRT);
        for (int x = px; x <= px + 4; x++) { // the tent, fallen in
            b.set(x, s + 1, pz + 2, Blocks.WOOL.brown());
            if (x % 2 == 0) {
                b.set(x, s + 2, pz + 2, Blocks.WOOL.brown());
            }
        }
        b.fill(px, s + 1, pz + 3, px, s + 3, pz + 3, Blocks.SPRUCE_FENCE);
        b.set(px - 2, s + 1, pz, Blocks.CAMPFIRE.defaultBlockState().setValue(CampfireBlock.LIT, false));
        b.set(px + 3, s + 1, pz - 2, Blocks.BARREL);
        b.set(px + 4, s + 1, pz - 2, Blocks.BARREL);
        b.set(px + 4, s + 2, pz - 2, Blocks.LANTERN);
        b.blockEntity(px + 1, s + 1, pz - 1, Blocks.LECTERN.defaultBlockState().setValue(LecternBlock.FACING, Direction.NORTH).setValue(LecternBlock.HAS_BOOK, true),
                lectern(book("book.hollowfrontier.prospector_notes.", NOTES_PAGES, "Notes", "J. Calloway")));
        b.chest(px + 2, s + 1, pz - 1, Direction.NORTH, ModChestLoot.TAR_PIT);
        b.armorStand(px - 2, s + 1, pz + 2, 45, ModItems.COWBOY_HAT.get(), null, null, null);
        // Gravel on the banks that the tar spat bones into (28: for a brush)
        for (int[] p : new int[][] {{3, 15}, {13, 25}, {22, 7}, {25, 15}}) {
            b.suspicious(p[0], s, p[1], Blocks.SUSPICIOUS_GRAVEL, ModChestLoot.DIG_TAR_PIT);
        }
        // Reeds and ferns round the bank
        for (int i = 0; i < 30; i++) {
            int x = r.nextInt(31), z = r.nextInt(31);
            double d = Math.hypot((x - cx) / 1.2, z - cz);
            if (d > 10 && d < 14 && !b.has(x, s + 1, z)) {
                b.set(x, s + 1, z, r.nextBoolean() ? Blocks.FERN : Blocks.SHORT_GRASS);
            }
        }
        return b;
    }

    // ---------------------------------------------------------------- the Crystal Steppes

    /**
     * 25 x 25: the headframe over the shaft (x 10..14, z 10..14): four timber legs eleven high braced at two heights, the
     * wheel up top, a ladder down the shaft's north side (the cavern, {@link #mineCavern}, hangs from the connector at its
     * foot). West, the bunkhouse with its bunks, stove and table; east, rails from the shaft's lip to the spoil heap, the
     * ore cart on them; the company's sign; a rack of picks and a heap of phlogiston ore by the door.
     */
    static Blueprint phlogistonMine() {
        Blueprint b = new Blueprint(PHLOGISTON_MINE);
        Random r = new Random(2703);
        int s = HollowBlueprints.FLOOR;
        b.air(0, s + 1, 0, 24, s + 16, 24);
        // The shaft's collar of timber, and the ladder down its north side
        b.fill(10, s, 10, 14, s, 14, Blocks.SPRUCE_PLANKS);
        b.air(11, s, 11, 13, s, 13);
        b.set(12, s, 11, Blocks.LADDER.defaultBlockState().setValue(LadderBlock.FACING, Direction.SOUTH));
        b.jigsaw(12, s, 11, FrontAndTop.fromFrontAndTop(Direction.DOWN, Direction.SOUTH), "hollowfrontier:mine_down", "hollowfrontier:mine_shaft",
                "hollowfrontier:phlogiston_mine/cavern", Blocks.LADDER.defaultBlockState().setValue(LadderBlock.FACING, Direction.SOUTH), 20);
        // The headframe
        BlockState log = Blocks.STRIPPED_SPRUCE_LOG.defaultBlockState();
        for (int[] leg : new int[][] {{10, 10}, {14, 10}, {10, 14}, {14, 14}}) {
            b.fill(leg[0], s + 1, leg[1], leg[0], s + 11, leg[1], log);
        }
        for (int y : new int[] {s + 5, s + 10}) {
            b.fill(10, y, 10, 14, y, 10, log.setValue(RotatedPillarBlock.AXIS, Direction.Axis.X));
            b.fill(10, y, 14, 14, y, 14, log.setValue(RotatedPillarBlock.AXIS, Direction.Axis.X));
            b.fill(10, y, 11, 10, y, 13, log.setValue(RotatedPillarBlock.AXIS, Direction.Axis.Z));
            b.fill(14, y, 11, 14, y, 13, log.setValue(RotatedPillarBlock.AXIS, Direction.Axis.Z));
        }
        b.fill(10, s + 12, 12, 14, s + 12, 12, log.setValue(RotatedPillarBlock.AXIS, Direction.Axis.X));
        for (int dy = -2; dy <= 2; dy++) { // the wheel
            for (int dz = -2; dz <= 2; dz++) {
                if (Math.abs(dy) == 2 || Math.abs(dz) == 2) {
                    b.set(12, s + 14 + dy, 12 + dz, Blocks.SPRUCE_FENCE);
                }
            }
        }
        b.set(12, s + 14, 12, Blocks.IRON_BLOCK);
        b.fill(12, s + 6, 12, 12, s + 13, 12, Blocks.IRON_CHAIN);
        // The bunkhouse (x 1..7, z 6..16)
        b.fill(1, s, 6, 7, s, 16, Blocks.SPRUCE_PLANKS);
        for (int y = s + 1; y <= s + 4; y++) {
            for (int x = 1; x <= 7; x++) {
                for (int z = 6; z <= 16; z++) {
                    boolean wall = x == 1 || x == 7 || z == 6 || z == 16, corner = (x == 1 || x == 7) && (z == 6 || z == 16);
                    if (wall) {
                        b.set(x, y, z, corner ? log : y == s + 2 && (x == 1 || x == 7) && z % 3 == 0 && z != 6 ? Blocks.GLASS_PANE.defaultBlockState() : Blocks.SPRUCE_PLANKS.defaultBlockState());
                    }
                }
            }
        }
        // A pitched roof (ridge north-south over x 4), its gables boarded
        for (int x = 0; x <= 8; x++) {
            for (int z = 5; z <= 17; z++) {
                int y = s + 5 + Math.min(x - 1, 7 - x); // the eaves at x 0 and 8 hang a block below the walls' tops
                b.set(x, y, z, x == 4 ? Blocks.SPRUCE_PLANKS.defaultBlockState()
                        : Masonry.stair(Blocks.SPRUCE_STAIRS, x < 4 ? Direction.EAST : Direction.WEST));
            }
        }
        for (int z : new int[] {6, 16}) {
            for (int x = 2; x <= 6; x++) {
                for (int y = s + 5; y < s + 5 + Math.min(x - 1, 7 - x); y++) {
                    b.set(x, y, z, Blocks.SPRUCE_PLANKS);
                }
            }
        }
        b.air(7, s + 1, 11, 7, s + 2, 11);
        b.door(7, s + 1, 11, (net.minecraft.world.level.block.DoorBlock) Blocks.SPRUCE_DOOR, Direction.EAST,
                net.minecraft.world.level.block.state.properties.DoorHingeSide.LEFT);
        for (int z : new int[] {7, 9, 13}) {
            b.bed(3, s + 1, z, Blocks.BED.brown(), Direction.WEST); // head against the west wall
        }
        b.set(2, s + 1, 15, Blocks.FURNACE);
        b.set(5, s + 1, 15, Blocks.CRAFTING_TABLE);
        b.chest(6, s + 1, 7, Direction.WEST, ModChestLoot.PHLOGISTON_MINE);
        b.set(5, s + 3, 11, Blocks.LANTERN.defaultBlockState().setValue(LanternBlock.HANGING, true));
        b.wallSign(4, s + 3, 5, Blocks.SPRUCE_WALL_SIGN, Direction.NORTH, DyeColor.BLACK, false,
                "sign.hollowfrontier.phlogiston_mine.1", "sign.hollowfrontier.phlogiston_mine.2", "sign.hollowfrontier.phlogiston_mine.3");
        // The rails east from the shaft to the spoil heap, the ore cart on them
        for (int x = 15; x <= 21; x++) {
            b.set(x, s + 1, 12, Blocks.RAIL.defaultBlockState().setValue(RailBlock.SHAPE, RailShape.EAST_WEST));
        }
        CompoundTag cart = new CompoundTag();
        b.entity(EntityTypes.MINECART, 18, s + 1, 12, cart);
        for (int x = 20; x <= 24; x++) {
            for (int z = 9; z <= 16; z++) {
                double d = Math.hypot(x - 23, (z - 12.5) / 1.5);
                int h = (int) Math.round(3 - d);
                for (int y = s + 1; y <= s + h; y++) {
                    b.set(x, y, z, r.nextInt(6) == 0 ? ModBlocks.PHLOGISTON_ORE.get().defaultBlockState() : r.nextBoolean() ? Blocks.GRAVEL.defaultBlockState() : Blocks.COBBLESTONE.defaultBlockState());
                }
            }
        }
        // The rack of picks by the door, the ore by it
        b.shelf(8, s + 2, 9, Blocks.SPRUCE_SHELF, Direction.EAST, Items.IRON_PICKAXE, Items.STONE_PICKAXE, Items.TORCH);
        b.set(8, s + 1, 9, Blocks.SPRUCE_PLANKS);
        b.set(8, s + 1, 13, ModBlocks.PHLOGISTON_ORE.get());
        b.hangLanterns(); // the bunkhouse lantern on a chain from the ridge
        b.set(9, s + 1, 13, Blocks.AMETHYST_CLUSTER.defaultBlockState().setValue(AmethystClusterBlock.FACING, Direction.UP));
        return b;
    }

    /**
     * Under the mine (a buried piece): the shaft, timbered, its ladder on the north side ({@link #MINE_SHAFT} deep), opening
     * into the cavern (21 x 21): a dome of stone glittering with phlogiston ore and crystal, rails along its floor, the steam
     * drill that broke against the crystal (boiler, pipes, a bit of iron in the wall), lanterns on posts, a chest.
     */
    static Blueprint mineCavern() {
        Blueprint b = new Blueprint(MINE_CAVERN);
        Random r = new Random(2704);
        int c = 10, h = 9, top = h + MINE_SHAFT;
        for (int x = 0; x <= 20; x++) {
            for (int z = 0; z <= 20; z++) {
                for (int y = 0; y <= h; y++) {
                    double e = Math.hypot(x - c, z - c) / 10.0 + Math.max(0, y - 2) / (double) h;
                    boolean inside = Math.hypot(x - c, z - c) <= 9.2 && y >= 1 && e < 1.0;
                    if (inside) {
                        b.set(x, y, z, Blocks.AIR);
                    } else {
                        int n = r.nextInt(30);
                        b.set(x, y, z, n == 0 ? ModBlocks.PHLOGISTON_ORE.get().defaultBlockState() : n == 1 ? ModBlocks.CONCAVIAN_CRYSTAL.get().defaultBlockState()
                                : n < 4 ? Blocks.AMETHYST_BLOCK.defaultBlockState() : r.nextBoolean() ? Blocks.STONE.defaultBlockState() : Blocks.TUFF.defaultBlockState());
                    }
                }
            }
        }
        // The shaft up from the cavern's roof (x 9..11 inside, z 9..11), timbered at the corners
        for (int y = h - 2; y < top; y++) {
            for (int x = 8; x <= 12; x++) {
                for (int z = 8; z <= 12; z++) {
                    boolean inside = x >= 9 && x <= 11 && z >= 9 && z <= 11;
                    boolean corner = (x == 8 || x == 12) && (z == 8 || z == 12);
                    b.set(x, y, z, inside && y < top - 0 ? Blocks.AIR.defaultBlockState() : corner ? Blocks.STRIPPED_SPRUCE_LOG.defaultBlockState()
                            : r.nextBoolean() ? Blocks.STONE.defaultBlockState() : Blocks.TUFF.defaultBlockState());
                }
            }
        }
        BlockState rung = Blocks.LADDER.defaultBlockState().setValue(LadderBlock.FACING, Direction.SOUTH);
        b.fill(10, 1, 9, 10, top - 2, 9, rung);
        b.fill(10, 1, 8, 10, h - 3, 8, Blocks.STRIPPED_SPRUCE_LOG); // a post for the ladder through the open cavern
        b.jigsaw(10, top - 1, 9, FrontAndTop.fromFrontAndTop(Direction.UP, Direction.SOUTH), "hollowfrontier:mine_shaft", "hollowfrontier:mine_down",
                "minecraft:empty", rung);
        b.set(10, top - 1, 10, Blocks.STONE);
        b.set(10, top - 1, 11, Blocks.STONE);
        b.set(9, top - 1, 9, Blocks.STONE);
        b.set(11, top - 1, 9, Blocks.STONE);
        b.fill(9, top - 1, 10, 11, top - 1, 11, Blocks.STONE);
        // Crystal grown from the walls, clusters on it
        for (int i = 0; i < 40; i++) {
            int x = 1 + r.nextInt(19), z = 1 + r.nextInt(19), y = 1;
            if (Math.hypot(x - c, z - c) > 7.5 && Math.hypot(x - c, z - c) <= 9 && b.get(new net.minecraft.core.BlockPos(x, y, z)) != null
                    && b.get(new net.minecraft.core.BlockPos(x, y, z)).isAir()) {
                int tall = 1 + r.nextInt(3);
                b.fill(x, y, z, x, y + tall - 1, z, ModBlocks.CONCAVIAN_CRYSTAL.get());
                b.set(x, y + tall, z, Blocks.AMETHYST_CLUSTER.defaultBlockState().setValue(AmethystClusterBlock.FACING, Direction.UP));
            }
        }
        // The rails, the drill broken against the crystal, lanterns, the chest
        for (int x = 3; x <= 17; x++) {
            b.set(x, 1, 14, Blocks.RAIL.defaultBlockState().setValue(RailBlock.SHAPE, RailShape.EAST_WEST));
        }
        b.set(16, 1, 6, ModBlocks.BOILER.get());
        b.fill(15, 1, 6, 14, 1, 6, ModBlocks.COPPER_PIPE.get().defaultBlockState());
        b.set(17, 1, 6, Blocks.IRON_BLOCK);
        b.set(18, 1, 6, Blocks.ANVIL); // the bit, jammed in the crystal
        b.fill(18, 1, 5, 18, 3, 5, ModBlocks.CONCAVIAN_CRYSTAL.get());
        for (int[] p : new int[][] {{5, 5}, {15, 15}, {5, 15}}) {
            b.fill(p[0], 1, p[1], p[0], 2, p[1], Blocks.SPRUCE_FENCE);
            b.set(p[0], 3, p[1], Blocks.LANTERN);
        }
        b.chest(6, 1, 12, Direction.SOUTH, ModChestLoot.PHLOGISTON_MINE_CAVERN);
        b.set(7, 1, 12, Blocks.BARREL);
        b.set(13, 1, 4, Blocks.TNT); // left behind, unlit
        return b;
    }

    /**
     * 21 x 21: a round tower (radius 5 at (10, 10)) of tuff on a deepslate course, crystal windows up it, three floors
     * joined by a ladder: the star chart laid in the ground floor (the inner sun at its middle, the shell's stars round it in
     * lapis and white), the study above (lecterns, shelves, the chest), and the lens room; on top, under the open sky, the
     * great lens: a ring of starsteel on four posts round a disc of glass and crystal, turned up at the sun.
     */
    static Blueprint observatory() {
        Blueprint b = new Blueprint(OBSERVATORY);
        Random r = new Random(2705);
        int s = HollowBlueprints.FLOOR, c = 10, height = 19;
        b.air(0, s + 1, 0, 20, s + 26, 20);
        for (int x = 0; x <= 20; x++) {
            for (int z = 0; z <= 20; z++) {
                double d = Math.hypot(x - c, z - c);
                if (d <= 9.6) {
                    b.set(x, s, z, d > 8.5 ? Masonry.kindledBase(r) : Masonry.kindledPaving(r));
                }
                if (d > 5.6) {
                    continue;
                }
                for (int y = s + 1; y <= s + height; y++) {
                    if (d > 4.5) {
                        boolean window = (y % 6 == 3 || y % 6 == 4) && (Math.abs(x - c) <= 1 || Math.abs(z - c) <= 1);
                        b.set(x, y, z, y <= s + 2 ? Masonry.kindledBase(r) : window ? ModBlocks.CONCAVIAN_CRYSTAL.get().defaultBlockState() : Masonry.kindledWall(r));
                    } else if (y == s + 6 || y == s + 12) {
                        b.set(x, y, z, Masonry.kindledPaving(r));
                    } else {
                        b.set(x, y, z, Blocks.AIR);
                    }
                }
                if (d <= 4.5) { // the star chart on the ground floor
                    double a = Math.toDegrees(Math.atan2(z - c, x - c)) + 360;
                    boolean star = (x * 7 + z * 13) % 11 == 0;
                    Block cell = d <= 1.0 ? Blocks.SHROOMLIGHT : star ? Blocks.CONCRETE.white() : ((int) (a / 45)) % 2 == 0 ? Blocks.LAPIS_BLOCK : Blocks.CONCRETE.blue();
                    b.set(x, s, z, cell);
                }
            }
        }
        // The door, south, under a crystal lintel
        b.air(c, s + 1, c + 5, c, s + 3, c + 5);
        b.air(c, s + 1, c + 4, c, s + 3, c + 4);
        b.set(c, s + 4, c + 5, Masonry.chiseledCrystal());
        // The ladder up the north side, through hatches in both floors
        BlockState rung = Blocks.LADDER.defaultBlockState().setValue(LadderBlock.FACING, Direction.SOUTH);
        b.fill(c, s + 1, c - 4, c, s + height, c - 4, rung);
        // The study (floor 2): lecterns, shelves, the chest; the lens room (floor 3): a spyglass on a shelf, a bench
        b.set(c + 2, s + 7, c - 2, Blocks.LECTERN.defaultBlockState().setValue(LecternBlock.FACING, Direction.WEST));
        b.fill(c - 3, s + 7, c - 1, c - 3, s + 8, c + 1, Blocks.BOOKSHELF);
        b.chest(c + 3, s + 7, c + 1, Direction.WEST, ModChestLoot.OBSERVATORY);
        b.set(c, s + 11, c, Masonry.phlogistonLamp()); // hung under floor 3
        b.shelf(c + 3, s + 14, c, Blocks.PALE_OAK_SHELF, Direction.WEST, Items.SPYGLASS, Items.COMPASS, Items.CLOCK);
        b.set(c + 3, s + 13, c, Blocks.POLISHED_TUFF);
        b.fill(c - 2, s + 13, c + 2, c + 2, s + 13, c + 2, Masonry.stair(Blocks.TUFF_BRICK_STAIRS, Direction.SOUTH));
        // The top: a parapet, and the lens on its posts
        int roof = s + height;
        for (int x = 0; x <= 20; x++) {
            for (int z = 0; z <= 20; z++) {
                double d = Math.hypot(x - c, z - c);
                if (d <= 5.6) {
                    b.set(x, roof, z, d > 4.5 ? Blocks.CHISELED_TUFF_BRICKS.defaultBlockState() : Masonry.kindledPaving(r));
                }
                if (d > 4.5 && d <= 5.6 && (x + z) % 2 == 0) {
                    b.set(x, roof + 1, z, Blocks.TUFF_BRICK_WALL);
                }
            }
        }
        b.set(c, roof, c - 4, rung);
        for (int[] p : new int[][] {{c - 2, c - 2}, {c + 2, c - 2}, {c - 2, c + 2}, {c + 2, c + 2}}) {
            b.fill(p[0], roof + 1, p[1], p[0], roof + 2, p[1], Blocks.POLISHED_DEEPSLATE);
            b.set(p[0], roof + 3, p[1], Masonry.starsteel());
        }
        for (int x = c - 3; x <= c + 3; x++) {
            for (int z = c - 3; z <= c + 3; z++) {
                double d = Math.hypot(x - c, z - c);
                if (d > 2.4 && d <= 3.4) {
                    b.set(x, roof + 4, z, Blocks.CUT_COPPER.waxed().weathered());
                } else if (d <= 2.4) {
                    b.set(x, roof + 4, z, d <= 1.0 ? ModBlocks.CONCAVIAN_CRYSTAL.get().defaultBlockState() : Blocks.GLASS.defaultBlockState());
                }
            }
        }
        // Weather and wear
        Masonry.crumble(b, r, 0, 0, 20, 20, 2, 0.15);
        Masonry.carpet(b, r, 0, 0, 20, 20, s + 1, Blocks.MOSS_CARPET, 0.04);
        return b;
    }

    // ---------------------------------------------------------------- the Ember Badlands

    /**
     * 29 x 25: the foundry hall (x 2..26, z 2..18) of blackstone brick, its roof gone but for beams, two crucibles on
     * plinths still holding lava (lava cauldrons, chains down to them), the channel the metal ran along (magma under
     * basalt) to the molds (anvils and a smithing table), blast furnaces along the north wall; the workshop at the east end
     * with the starsteel anvil and the chest; a chimney stack in the corner; slag heaped outside to the south.
     */
    static Blueprint foundry() {
        Blueprint b = new Blueprint(FOUNDRY);
        Random r = new Random(2706);
        int s = HollowBlueprints.FLOOR;
        b.air(0, s + 1, 0, 28, s + 18, 24);
        b.fill(2, s, 2, 26, s, 18, Blocks.POLISHED_BLACKSTONE);
        for (int y = s + 1; y <= s + 7; y++) {
            for (int x = 2; x <= 26; x++) {
                for (int z = 2; z <= 18; z++) {
                    boolean wall = x == 2 || x == 26 || z == 2 || z == 18;
                    if (wall) {
                        boolean window = y >= s + 3 && y <= s + 5 && (x % 4 == 0 || z % 4 == 0) && !(x == 2 || x == 26) == (z == 2 || z == 18);
                        b.set(x, y, z, window ? Blocks.AIR.defaultBlockState() : r.nextInt(5) == 0 ? Blocks.CRACKED_POLISHED_BLACKSTONE_BRICKS.defaultBlockState()
                                : Blocks.POLISHED_BLACKSTONE_BRICKS.defaultBlockState());
                    }
                }
            }
        }
        for (int x = 2; x <= 26; x += 4) { // the roof beams, what's left of the roof
            b.fill(x, s + 8, 2, x, s + 8, 18, Blocks.DARK_OAK_LOG.defaultBlockState().setValue(RotatedPillarBlock.AXIS, Direction.Axis.Z));
        }
        Masonry.crumble(b, r, 2, 2, 26, 18, 3, 0.35);
        b.air(13, s + 1, 18, 15, s + 4, 18); // the great door, south
        // The crucibles on their plinths, lava in them, chains down from the beams
        for (int cx : new int[] {7, 13}) {
            b.fill(cx - 1, s + 1, 6, cx + 1, s + 1, 8, Blocks.POLISHED_BLACKSTONE);
            b.set(cx, s + 2, 7, Blocks.LAVA_CAULDRON);
            b.fill(cx, s + 3, 7, cx, s + 7, 7, Blocks.IRON_CHAIN);
            b.set(cx, s + 8, 7, Blocks.DARK_OAK_LOG);
        }
        // The channel to the molds: magma under a skin of basalt, steps of it cracked open
        for (int x = 7; x <= 20; x++) {
            b.set(x, s, 10, x % 3 == 0 ? Blocks.MAGMA_BLOCK : Blocks.SMOOTH_BASALT);
        }
        for (int x = 17; x <= 21; x += 2) {
            b.set(x, s + 1, 11, Blocks.ANVIL);
        }
        b.set(19, s + 1, 9, Blocks.SMITHING_TABLE);
        for (int x = 4; x <= 22; x += 3) {
            b.set(x, s + 1, 3, Blocks.BLAST_FURNACE);
        }
        // The workshop (east, x 22..25, z 13..17): the starsteel anvil, the chest, a grindstone, tools on a shelf
        b.fill(21, s + 1, 12, 21, s + 6, 17, Blocks.POLISHED_BLACKSTONE_BRICKS);
        b.fill(21, s + 1, 12, 25, s + 6, 12, Blocks.POLISHED_BLACKSTONE_BRICKS);
        b.air(21, s + 1, 15, 21, s + 3, 15);
        b.set(23, s + 1, 15, Masonry.starsteel());
        b.set(23, s + 2, 15, Blocks.ANVIL);
        b.chest(25, s + 1, 16, Direction.WEST, ModChestLoot.FOUNDRY);
        b.set(25, s + 1, 13, Blocks.GRINDSTONE);
        b.shelf(25, s + 2, 14, Blocks.CRIMSON_SHELF, Direction.WEST, ModItems.STARSTEEL_NUGGET.get(), Items.IRON_PICKAXE, ModItems.RAW_STARSTEEL.get());
        b.set(25, s + 1, 14, Blocks.POLISHED_BLACKSTONE_BRICKS);
        // The chimney stack in the north-west corner
        b.fill(0, s, 0, 2, s + 15, 2, Blocks.BRICKS);
        b.air(1, s + 1, 1, 1, s + 15, 1);
        b.set(1, s, 1, Blocks.CAMPFIRE.defaultBlockState().setValue(CampfireBlock.LIT, true).setValue(CampfireBlock.SIGNAL_FIRE, true));
        // The slag heaped south of the door
        for (int x = 3; x <= 25; x++) {
            for (int z = 20; z <= 24; z++) {
                double d = Math.min(Math.hypot(x - 7, (z - 22) * 1.5), Math.hypot(x - 21, (z - 22) * 1.5));
                int h = (int) Math.round(3 - d * 0.8);
                for (int y = s + 1; y <= s + h; y++) {
                    int n = r.nextInt(6);
                    b.set(x, y, z, (n < 2 ? Blocks.BLACKSTONE : n < 4 ? Blocks.BASALT : n == 4 ? Blocks.GRAVEL : Blocks.COAL_BLOCK).defaultBlockState());
                }
            }
        }
        return b;
    }

    // ---------------------------------------------------------------- on the islands, and under the crust

    /**
     * 17 x 17, on a floating island's top: a round floor of crystal brick, a dais in its middle with the waystone on it
     * (arrivals step off two blocks south), six crystal pillars round it under a broken ring of lintels, offering pots, and
     * a bridge running east off the island's edge that stops in the air where its far end fell.
     */
    static Blueprint skyShrine() {
        Blueprint b = new Blueprint(SKY_SHRINE);
        Random r = new Random(2707);
        int s = HollowBlueprints.FLOOR, c = 8;
        b.air(0, s + 1, 0, 16, s + 10, 16);
        for (int x = 0; x <= 16; x++) {
            for (int z = 0; z <= 16; z++) {
                double d = Math.hypot(x - c, z - c);
                if (d <= 6.5) {
                    b.set(x, s, z, d > 5.5 ? Masonry.chiseledCrystal() : r.nextInt(6) == 0 ? Blocks.CALCITE.defaultBlockState() : Masonry.crystalBricks());
                }
            }
        }
        b.fill(c - 1, s + 1, c - 1, c + 1, s + 1, c + 1, Masonry.chiseledCrystal());
        b.set(c, s + 2, c, ModBlocks.WAYSTONE.get());
        b.air(c, s + 1, c + 2, c, s + 3, c + 2);
        for (int k = 0; k < 6; k++) {
            double a = k * Math.PI / 3;
            int x = c + (int) Math.round(Math.cos(a) * 5), z = c + (int) Math.round(Math.sin(a) * 5);
            int h = k == 4 ? 2 : 5;
            b.fill(x, s + 1, z, x, s + h, z, ModBlocks.CRYSTAL_BRICK_WALL.get());
            if (h == 5) {
                b.set(x, s + 6, z, Masonry.chiseledCrystal());
                b.set(x, s + 7, z, Blocks.AMETHYST_CLUSTER.defaultBlockState().setValue(AmethystClusterBlock.FACING, Direction.UP));
            }
        }
        b.pot(c - 3, s + 1, c - 3, Direction.SOUTH, ModChestLoot.KINDLED_OFFERING);
        b.pot(c + 3, s + 1, c - 3, Direction.SOUTH, ModChestLoot.SKY_SHRINE);
        b.chest(c - 3, s + 1, c + 3, Direction.NORTH, ModChestLoot.SKY_SHRINE);
        // The bridge east, its far end fallen
        for (int x = c + 6; x <= 16; x++) {
            b.fill(x, s, c - 1, x, s, c + 1, Masonry.crystalBricks());
            if (x % 2 == 0) {
                b.set(x, s + 1, c - 1, ModBlocks.CRYSTAL_BRICK_WALL.get());
                b.set(x, s + 1, c + 1, ModBlocks.CRYSTAL_BRICK_WALL.get());
            }
        }
        b.set(16, s, c + 1, Blocks.AIR);
        return b;
    }

    /**
     * 15 x 15 x 16, hung under the crust (its top layer set into the rock): an iron pylon down from the anchor to the deck
     * (a disc of quartz rimmed in white, lit, railed with bars), the relay's cache on it, and under the deck a mast down to
     * a dish pointing at the ground. A probe keeps watch.
     */
    static Blueprint grayRelay() {
        Blueprint b = new Blueprint(GRAY_RELAY);
        int c = 7, deck = 7;
        // The anchor in the crust, and the pylon down
        b.fill(c - 2, 14, c - 2, c + 2, 15, c + 2, Blocks.IRON_BLOCK);
        b.fill(c - 1, deck + 1, c - 1, c + 1, 13, c + 1, Blocks.IRON_BARS);
        b.fill(c, deck + 1, c, c, 13, c, Blocks.IRON_BLOCK);
        for (int y = deck + 2; y <= 13; y += 3) {
            b.set(c, y, c - 1, Blocks.SEA_LANTERN);
        }
        // The deck
        for (int x = 0; x <= 14; x++) {
            for (int z = 0; z <= 14; z++) {
                double d = Math.hypot(x - c, z - c);
                if (d > 6.5) {
                    continue;
                }
                b.set(x, deck, z, d > 5.6 ? Blocks.CONCRETE.white() : ((int) d) % 2 == 0 ? Blocks.SMOOTH_QUARTZ : Blocks.CONCRETE.lightGray());
                b.set(x, deck - 1, z, d > 5.6 ? Blocks.SMOOTH_QUARTZ_SLAB.defaultBlockState().setValue(SlabBlock.TYPE, SlabType.TOP) : Blocks.SMOOTH_QUARTZ.defaultBlockState());
                if (d > 5.6) {
                    b.set(x, deck + 1, z, Blocks.IRON_BARS);
                }
            }
        }
        for (int[] p : new int[][] {{c - 4, c}, {c + 4, c}, {c, c - 4}, {c, c + 4}}) {
            b.set(p[0], deck, p[1], Blocks.SEA_LANTERN);
        }
        b.chest(c + 2, deck + 1, c + 2, Direction.WEST, ModChestLoot.GRAY_RELAY);
        b.set(c - 2, deck + 1, c + 2, Blocks.COPPER_BULB.waxed().unaffected().defaultBlockState().setValue(net.minecraft.world.level.block.CopperBulbBlock.LIT, true));
        // The mast down and its dish
        b.fill(c, 2, c, c, deck - 2, c, Blocks.IRON_BARS);
        for (int x = c - 2; x <= c + 2; x++) {
            for (int z = c - 2; z <= c + 2; z++) {
                if (Math.abs(x - c) + Math.abs(z - c) < 4) {
                    b.set(x, 1, z, Math.abs(x - c) + Math.abs(z - c) <= 1 ? Blocks.SMOOTH_QUARTZ.defaultBlockState()
                            : Blocks.SMOOTH_QUARTZ_SLAB.defaultBlockState().setValue(SlabBlock.TYPE, SlabType.TOP));
                }
            }
        }
        b.set(c, 1, c, Blocks.SEA_LANTERN);
        b.set(c, 0, c, Blocks.END_ROD.defaultBlockState().setValue(net.minecraft.world.level.block.EndRodBlock.FACING, Direction.DOWN));
        b.entity(ModEntities.GRAY_PROBE.get(), c + 3, deck + 2, c - 3);
        return b;
    }

    // ---------------------------------------------------------------- books

    /** A written book with its pages from the lang file ({@code prefix + n}), saved as an item stack is (cf. the journal). */
    static CompoundTag book(String prefix, int pages, String title, String author) {
        List<Filterable<Component>> list = new ArrayList<>();
        for (int i = 1; i <= pages; i++) {
            list.add(Filterable.passThrough(Component.translatable(prefix + i)));
        }
        WrittenBookContent content = new WrittenBookContent(Filterable.passThrough(title), author, 0, list, true);
        CompoundTag components = new CompoundTag();
        components.put("minecraft:written_book_content", WrittenBookContent.CODEC.encodeStart(NbtOps.INSTANCE, content).getOrThrow());
        CompoundTag book = new CompoundTag();
        book.putString("id", "minecraft:written_book");
        book.putInt("count", 1);
        book.put("components", components);
        return book;
    }

    static CompoundTag lectern(CompoundTag book) {
        CompoundTag nbt = new CompoundTag();
        nbt.putString("id", "minecraft:lectern");
        nbt.put("Book", book);
        nbt.putInt("Page", 0);
        return nbt;
    }
}
