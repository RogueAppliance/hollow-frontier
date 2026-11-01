package com.hollowfrontier.worldgen.structure;

import static com.hollowfrontier.worldgen.structure.TownBlueprints.candles;
import static com.hollowfrontier.worldgen.structure.TownBlueprints.facing;
import static com.hollowfrontier.worldgen.structure.TownBlueprints.front;
import static com.hollowfrontier.worldgen.structure.TownBlueprints.gableRoof;
import static com.hollowfrontier.worldgen.structure.TownBlueprints.hangingLantern;
import static com.hollowfrontier.worldgen.structure.TownBlueprints.log;
import static com.hollowfrontier.worldgen.structure.TownBlueprints.openBarrel;
import static com.hollowfrontier.worldgen.structure.TownBlueprints.pigeonholes;
import static com.hollowfrontier.worldgen.structure.TownBlueprints.s;
import static com.hollowfrontier.worldgen.structure.TownBlueprints.shopfront;
import static com.hollowfrontier.worldgen.structure.TownBlueprints.slab;
import static com.hollowfrontier.worldgen.structure.TownBlueprints.stairs;
import static com.hollowfrontier.worldgen.structure.TownBlueprints.tag;
import static com.hollowfrontier.worldgen.structure.TownBlueprints.water;
import static com.hollowfrontier.worldgen.structure.TownDecor.barrel;
import static com.hollowfrontier.worldgen.structure.TownDecor.chair;
import static com.hollowfrontier.worldgen.structure.TownDecor.stool;

import java.util.List;

import com.hollowfrontier.registry.ModBlocks;
import com.hollowfrontier.registry.ModEntities;
import com.hollowfrontier.registry.ModItems;

import net.minecraft.core.Direction;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.BellBlock;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.DoorBlock;
import net.minecraft.world.level.block.FenceGateBlock;
import net.minecraft.world.level.block.LadderBlock;
import net.minecraft.world.level.block.LeverBlock;
import net.minecraft.world.level.block.TrapDoorBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.AttachFace;
import net.minecraft.world.level.block.state.properties.BellAttachType;
import net.minecraft.world.level.block.state.properties.DoorHingeSide;
import net.minecraft.world.level.block.state.properties.Half;
import net.minecraft.world.level.block.state.properties.SlabType;

/**
 * The town's newer trades and homes: the gunsmith, the doctor, the post and telegraph office (shop lots), the two-storey
 * homestead (home and lane lots) and the schoolhouse (at the end of a street). Same rules as TownBlueprints: drawn facing
 * north, front connector mid z 0, floor y 0, at most 12 deep on a street lot.
 */
public final class TownShops {
    private TownShops() {}

    static List<Blueprint> all() {
        return List.of(TownLots.inLot(gunsmith()), TownLots.inLot(doctor()), TownLots.inLot(postOffice()), TownLots.inLot(homestead()), schoolhouse(),
                TownLots.inLot(bathhouse()), TownLots.inLot(gazetteOffice()));
    }

    // 9 x 12: guns in a glass case, ammunition on shelves behind the counter, the bench with its vise and grindstone, powder
    // kegs, a target to sight on. The gunsmith is a blacksmith by trade (he sells the gun books).
    static Blueprint gunsmith() {
        Blueprint b = new Blueprint("town/gunsmith");
        int w = 9, h = 4, cx = 4;
        BlockState post = log(Blocks.STRIPPED_DARK_OAK_LOG, Direction.Axis.Y);
        shopfront(b, w, 12, h, 3, 4, s(Blocks.ACACIA_PLANKS), post, s(Blocks.DARK_OAK_PLANKS), Blocks.SPRUCE_SLAB, Blocks.DARK_OAK_SLAB,
                (DoorBlock) Blocks.DARK_OAK_DOOR);
        TownDecor.cornice(b, w, h, 3, post, Blocks.DARK_OAK_STAIRS);
        TownDecor.shutters(b, w, Blocks.SPRUCE_TRAPDOOR);
        TownDecor.shopSign(b, cx, 3, "gunsmith");
        // The glass case down the west wall: a revolver, a rifle and a shotgun on shelves behind glass, on a cabinet
        b.fill(1, 1, 4, 2, 1, 6, Blocks.DARK_OAK_PLANKS);
        b.shelf(1, 2, 4, Blocks.DARK_OAK_SHELF, Direction.EAST, ModItems.REVOLVER.get());
        b.shelf(1, 2, 5, Blocks.DARK_OAK_SHELF, Direction.EAST, ModItems.PLAINS_RIFLE.get());
        b.shelf(1, 2, 6, Blocks.DARK_OAK_SHELF, Direction.EAST, ModItems.SHOTGUN.get());
        b.fill(2, 2, 4, 2, 2, 6, Blocks.GLASS_PANE);
        b.fill(1, 3, 4, 2, 3, 6, slab(Blocks.DARK_OAK_SLAB, SlabType.BOTTOM));
        // The counter, and behind it ammunition on the back wall over the powder kegs and the bench
        b.fill(3, 1, 7, 6, 1, 7, ModBlocks.BAR_COUNTER.get());
        b.set(3, 2, 7, Blocks.LANTERN);
        b.fill(1, 1, 10, 3, 1, 10, barrel(Direction.NORTH));
        b.set(5, 1, 10, Blocks.SMITHING_TABLE);
        b.set(6, 1, 10, Blocks.GRINDSTONE.defaultBlockState().setValue(net.minecraft.world.level.block.GrindstoneBlock.FACE, AttachFace.FLOOR));
        b.set(7, 1, 10, openBarrel());
        b.shelf(1, 2, 10, Blocks.SPRUCE_SHELF, Direction.NORTH, ModItems.REVOLVER_CARTRIDGE.get(), ModItems.REVOLVER_CARTRIDGE.get(), ModItems.REVOLVER_CARTRIDGE.get());
        b.shelf(2, 2, 10, Blocks.SPRUCE_SHELF, Direction.NORTH, ModItems.RIFLE_CARTRIDGE.get(), ModItems.RIFLE_CARTRIDGE.get(), ModItems.RIFLE_CARTRIDGE.get());
        b.shelf(3, 2, 10, Blocks.SPRUCE_SHELF, Direction.NORTH, ModItems.BUCKSHOT_CARTRIDGE.get(), ModItems.BUCKSHOT_CARTRIDGE.get(), ModItems.BUCKSHOT_CARTRIDGE.get());
        b.set(4, 2, 10, Blocks.TARGET);
        b.shelf(5, 2, 10, Blocks.SPRUCE_SHELF, Direction.NORTH, ModItems.PERCUSSION_CAP.get(), ModItems.PERCUSSION_CAP.get(), Items.FLINT);
        b.shelf(6, 2, 10, Blocks.SPRUCE_SHELF, Direction.NORTH, ModItems.BRASS_NUGGET.get(), ModItems.BRASS_GEAR.get(), Items.IRON_NUGGET);
        b.shelf(7, 2, 10, Blocks.SPRUCE_SHELF, Direction.NORTH, Items.GUNPOWDER, Items.GUNPOWDER, Items.PAPER);
        b.chest(1, 1, 9, Direction.EAST, ModChestLoot.GUNSMITH);
        // On the shop floor: a keg of powder, a stool to try a grip from, a rug
        b.set(7, 1, 3, barrel(Direction.UP));
        b.set(7, 1, 5, stool());
        TownDecor.rug(b, 3, 1, 4, 6, 6, DyeColor.GREEN, DyeColor.BROWN);
        b.set(4, 4, 5, hangingLantern());
        b.set(4, 4, 9, hangingLantern());
        b.set(1, 1, 0, Blocks.BARREL);
        b.fill(7, 1, 0, 7, 1, 1, Blocks.SPRUCE_FENCE);
        b.entity(ModEntities.TOWNSFOLK.get(), 5, 1, 8, tag("folk", "gunsmith"));
        return b;
    }

    // 9 x 12: a waiting room with a bench and the doctor's desk, remedies on shelves; through a door the surgery, with a
    // sickbed, a washstand, the instruments and the skeleton every doctor keeps. The sign promises teeth pulled.
    static Blueprint doctor() {
        Blueprint b = new Blueprint("town/doctor");
        int w = 9, h = 4, cx = 4;
        BlockState post = log(Blocks.STRIPPED_BIRCH_LOG, Direction.Axis.Y);
        shopfront(b, w, 12, h, 2, 4, s(Blocks.BIRCH_PLANKS), post, s(Blocks.BIRCH_PLANKS), Blocks.DARK_OAK_SLAB, Blocks.SPRUCE_SLAB,
                (DoorBlock) Blocks.SPRUCE_DOOR);
        TownDecor.cornice(b, w, h, 2, post, Blocks.DARK_OAK_STAIRS);
        TownDecor.shutters(b, w, Blocks.DARK_OAK_TRAPDOOR);
        TownDecor.shopSign(b, cx, 3, "doctor");
        // The waiting room: a bench for the patients, the doctor's desk with a brewing stand on it, remedies on the wall
        b.fill(1, 1, 3, 1, 1, 5, stairs(Blocks.SPRUCE_STAIRS, Direction.WEST, Half.BOTTOM));
        b.fill(6, 1, 5, 7, 1, 5, ModBlocks.BAR_COUNTER.get());
        b.set(7, 2, 5, Blocks.BREWING_STAND);
        b.set(6, 2, 5, candles(1));
        chair(b, 6, 1, 6, Blocks.SPRUCE_STAIRS, Direction.SOUTH);
        b.shelf(7, 2, 3, Blocks.SPRUCE_SHELF, Direction.WEST, Items.GLASS_BOTTLE, Items.HONEY_BOTTLE, Items.GLASS_BOTTLE);
        b.shelf(7, 2, 4, Blocks.SPRUCE_SHELF, Direction.WEST, ModItems.SNAKE_OIL.get(), Items.SUGAR, Items.SPIDER_EYE);
        b.set(1, 1, 6, Blocks.POTTED_FERN);
        TownDecor.rug(b, 2, 1, 3, 4, 5, DyeColor.CYAN, DyeColor.BLUE);
        // The surgery behind a partition
        b.fill(1, 1, 7, 7, 4, 7, Blocks.BIRCH_PLANKS);
        b.door(4, 1, 7, (DoorBlock) Blocks.SPRUCE_DOOR, Direction.NORTH, DoorHingeSide.LEFT);
        b.bed(2, 1, 9, Blocks.BED.white(), Direction.WEST);
        b.bed(2, 1, 10, Blocks.BED.white(), Direction.WEST);
        b.set(3, 1, 8, water(2));
        b.chest(7, 1, 9, Direction.WEST, ModChestLoot.DOCTOR);
        b.set(7, 1, 8, Blocks.CAULDRON);
        b.shelf(5, 2, 10, Blocks.SPRUCE_SHELF, Direction.NORTH, Items.GLISTERING_MELON_SLICE, Items.PAPER, Items.GLASS_BOTTLE);
        b.shelf(6, 2, 10, Blocks.SPRUCE_SHELF, Direction.NORTH, Items.BONE, Items.STRING, Items.SHEARS);
        b.shelf(7, 2, 10, Blocks.SPRUCE_SHELF, Direction.NORTH, Items.BOOK, Items.BOOK, Items.INK_SAC);
        b.armorStand(5, 1, 10, 180, Items.SKELETON_SKULL);
        b.set(4, 4, 4, hangingLantern());
        b.set(4, 4, 9, hangingLantern());
        b.set(7, 1, 0, Blocks.POTTED_FERN);
        b.entity(ModEntities.TOWNSFOLK.get(), 2, 1, 4);                  // a patient, waiting
        b.entity(ModEntities.TOWNSFOLK.get(), 7, 1, 6, tag("folk", "doctor"));  // the doctor at his desk
        return b;
    }

    // 9 x 12: the counter with its barred window, the sorting case of pigeonholes along the back wall, mail sacks, the
    // telegraph key on its desk, the Gazette on a shelf. The clerk is a banker by look.
    static Blueprint postOffice() {
        Blueprint b = new Blueprint("town/post_office");
        int w = 9, h = 5, cx = 4;
        BlockState post = log(Blocks.DARK_OAK_LOG, Direction.Axis.Y);
        shopfront(b, w, 12, h, 2, 4, s(Blocks.SPRUCE_PLANKS), post, s(Blocks.PALE_OAK_PLANKS), Blocks.DARK_OAK_SLAB, Blocks.SPRUCE_SLAB,
                (DoorBlock) Blocks.SPRUCE_DOOR);
        TownDecor.cornice(b, w, h, 2, post, Blocks.DARK_OAK_STAIRS);
        TownDecor.shutters(b, w, Blocks.SPRUCE_TRAPDOOR);
        TownDecor.shopSign(b, cx, 3, "post_office");
        // The counter across the room, a window of bars over it, the gate at its east end
        b.fill(1, 1, 6, 6, 1, 6, ModBlocks.BAR_COUNTER.get());
        b.fill(1, 2, 6, 2, 3, 6, Blocks.IRON_BARS);
        b.fill(5, 2, 6, 6, 3, 6, Blocks.IRON_BARS);
        b.fill(1, 4, 6, 6, 4, 6, Blocks.SPRUCE_PLANKS);
        b.door(7, 1, 6, (DoorBlock) Blocks.SPRUCE_DOOR, Direction.NORTH, DoorHingeSide.RIGHT);
        b.set(3, 2, 6, candles(2));
        // The public side: the Gazette and stationery on shelves, a writing desk with a stool
        b.shelf(7, 2, 3, Blocks.SPRUCE_SHELF, Direction.WEST, ModItems.FRONTIER_GAZETTE.get(), ModItems.FRONTIER_GAZETTE.get(), ModItems.FRONTIER_GAZETTE.get());
        b.shelf(7, 2, 4, Blocks.SPRUCE_SHELF, Direction.WEST, Items.PAPER, Items.FEATHER, Items.INK_SAC);
        TownDecor.table(b, 2, 1, 4, Blocks.SPRUCE_FENCE, Blocks.SPRUCE_PRESSURE_PLATE);
        b.set(1, 1, 4, stool());
        b.set(1, 1, 3, Blocks.POTTED_BAMBOO);
        // Behind the counter: the sorting case, mail sacks, the telegraph desk and key
        b.fill(1, 1, 10, 7, 3, 10, pigeonholes(Direction.NORTH));
        b.fill(1, 1, 8, 1, 1, 9, Blocks.WOOL.pick(DyeColor.BROWN));
        b.set(1, 2, 9, Blocks.WOOL.pick(DyeColor.BROWN));
        b.set(2, 1, 9, Blocks.WOOL_SLAB.pick(DyeColor.BROWN));
        b.fill(6, 1, 8, 7, 1, 8, ModBlocks.BAR_COUNTER.get());
        b.set(7, 2, 8, Blocks.LEVER.defaultBlockState().setValue(LeverBlock.FACE, AttachFace.FLOOR).setValue(LeverBlock.FACING, Direction.WEST));
        b.set(6, 2, 8, Blocks.LANTERN);
        b.set(7, 1, 9, stool());
        b.chest(5, 1, 9, Direction.NORTH, ModChestLoot.POST_OFFICE);
        b.set(cx, 5, 4, hangingLantern());
        b.set(cx, 5, 8, hangingLantern());
        // A wire strung down from the roof to the street, the line in
        b.fill(7, h + 2, 9, 7, h + 5, 9, Blocks.SPRUCE_FENCE);
        b.set(6, h + 5, 9, Blocks.SPRUCE_FENCE);
        b.set(8, h + 5, 9, Blocks.SPRUCE_FENCE);
        b.set(7, h + 6, 9, Blocks.LIGHTNING_ROD.waxed().unaffected());
        b.set(1, 1, 0, barrel(Direction.UP));
        b.entity(ModEntities.TOWNSFOLK.get(), 4, 1, 7, tag("folk", "postmaster"));
        return b;
    }

    // 11 x 12: the barber's chair and mirror at the front, the barber pole out on the porch; through a door the bathhouse,
    // two tubs (water cauldrons) either side of a curtain, the stove that heats the water, towels. A soak sobers you (Baths).
    static Blueprint bathhouse() {
        Blueprint b = new Blueprint("town/bathhouse");
        int w = 11, h = 4, cx = 5;
        BlockState post = log(Blocks.STRIPPED_BIRCH_LOG, Direction.Axis.Y);
        shopfront(b, w, 12, h, 2, 4, s(Blocks.PALE_OAK_PLANKS), post, s(Blocks.BIRCH_PLANKS), Blocks.DARK_OAK_SLAB, Blocks.SPRUCE_SLAB,
                (DoorBlock) Blocks.SPRUCE_DOOR);
        TownDecor.cornice(b, w, h, 2, post, Blocks.DARK_OAK_STAIRS);
        TownDecor.shutters(b, w, Blocks.DARK_OAK_TRAPDOOR);
        TownDecor.shopSign(b, cx, 3, "bathhouse");
        // The barber pole on the porch, red and white
        b.set(9, 1, 0, Blocks.WOOL.red());
        b.set(9, 2, 0, Blocks.WOOL.white());
        b.set(9, 3, 0, Blocks.WOOL.red());
        // The shop: the barber's chair facing his mirror and shelf, a bench for whoever's next, a stool and a strop
        b.set(1, 1, 4, ModBlocks.BAR_COUNTER.get());
        b.shelf(1, 2, 4, Blocks.SPRUCE_SHELF, Direction.EAST, Items.SHEARS, Items.HONEY_BOTTLE, Items.BOWL);
        b.set(1, 3, 4, Blocks.STAINED_GLASS_PANE.white());                // the mirror
        b.set(2, 1, 4, stairs(Blocks.WOOL_STAIRS.pick(DyeColor.RED), Direction.EAST, Half.BOTTOM));
        b.fill(9, 1, 3, 9, 1, 4, stairs(Blocks.OAK_STAIRS, Direction.EAST, Half.BOTTOM));
        b.set(9, 1, 6, Blocks.POTTED_FERN);
        b.shelf(9, 2, 5, Blocks.SPRUCE_SHELF, Direction.WEST, Items.LEATHER, Items.CANDLE, Items.BOOK);
        TownDecor.rug(b, 3, 1, 3, 7, 5, DyeColor.WHITE, DyeColor.BLACK);
        // The partition, and the baths behind it: two tubs, a curtain between, the stove, towels on the shelf
        b.fill(1, 1, 7, 9, h, 7, Blocks.PALE_OAK_PLANKS);
        b.door(cx, 1, 7, (DoorBlock) Blocks.SPRUCE_DOOR, Direction.NORTH, DoorHingeSide.LEFT);
        b.fill(1, 0, 8, 9, 0, 10, Blocks.SMOOTH_STONE);                   // a tiled floor for the wet room
        for (int x : new int[] {2, 7}) {
            b.fill(x, 1, 9, x + 1, 1, 9, water(3));
            b.fill(x, 1, 10, x + 1, 1, 10, Blocks.SPRUCE_PLANKS);          // the tub's back
        }
        b.fill(cx, 1, 9, cx, 3, 10, Blocks.WOOL.white());                 // the curtain
        b.shelf(1, 2, 8, Blocks.SPRUCE_SHELF, Direction.EAST, Items.WOOL.pick(DyeColor.WHITE), Items.WOOL.pick(DyeColor.WHITE), Items.WOOL.pick(DyeColor.WHITE));
        TownDecor.stove(b, 9, 1, 8, Direction.WEST, h + 2);
        b.chest(9, 1, 10, Direction.WEST, ModChestLoot.BATHHOUSE);
        b.set(cx, h, 5, hangingLantern());
        b.set(cx, h, 9, hangingLantern());
        b.entity(ModEntities.TOWNSFOLK.get(), 4, 1, 5);                 // the barber
        return b;
    }

    // 9 x 12: the Frontier Gazette's office: the counter where the paper's sold, today's edition on the shelf; behind it the
    // press (a piston over the platen in an oak frame), the type cases, a stack of newsprint and the ink.
    static Blueprint gazetteOffice() {
        Blueprint b = new Blueprint("town/gazette_office");
        int w = 9, h = 5, cx = 4;
        BlockState post = log(Blocks.DARK_OAK_LOG, Direction.Axis.Y);
        shopfront(b, w, 12, h, 3, 4, s(Blocks.OAK_PLANKS), post, s(Blocks.DARK_OAK_PLANKS), Blocks.SPRUCE_SLAB, Blocks.DARK_OAK_SLAB,
                (DoorBlock) Blocks.DARK_OAK_DOOR);
        TownDecor.cornice(b, w, h, 3, post, Blocks.DARK_OAK_STAIRS);
        TownDecor.shutters(b, w, Blocks.SPRUCE_TRAPDOOR);
        TownDecor.shopSign(b, cx, 3, "gazette_office");
        // The counter, the papers on the shelves, a bench to read them on
        b.fill(1, 1, 5, 5, 1, 5, ModBlocks.BAR_COUNTER.get());
        b.set(2, 2, 5, candles(2));
        b.shelf(7, 2, 3, Blocks.SPRUCE_SHELF, Direction.WEST, ModItems.FRONTIER_GAZETTE.get(), ModItems.FRONTIER_GAZETTE.get(), ModItems.FRONTIER_GAZETTE.get());
        b.shelf(7, 2, 4, Blocks.SPRUCE_SHELF, Direction.WEST, ModItems.FRONTIER_GAZETTE.get(), ModItems.WANTED_POSTER.get(), ModItems.FRONTIER_GAZETTE.get());
        b.fill(7, 1, 3, 7, 1, 4, stairs(Blocks.OAK_STAIRS, Direction.EAST, Half.BOTTOM));
        // The press: an oak frame, the platen, the screw (a piston face down)
        b.fill(2, 1, 8, 4, 1, 8, Blocks.SMOOTH_STONE_SLAB);
        b.fill(2, 2, 8, 2, 3, 8, Blocks.DARK_OAK_FENCE);
        b.fill(4, 2, 8, 4, 3, 8, Blocks.DARK_OAK_FENCE);
        b.fill(2, 4, 8, 4, 4, 8, Blocks.DARK_OAK_PLANKS);
        b.set(3, 3, 8, Blocks.PISTON.defaultBlockState().setValue(net.minecraft.world.level.block.DirectionalBlock.FACING, Direction.DOWN));
        b.set(2, 1, 7, Blocks.WOOL.white());                              // a bale of newsprint
        b.set(2, 2, 7, Blocks.WOOL_SLAB.pick(DyeColor.WHITE));
        // The type cases along the back, the ink, the editor's desk
        b.fill(1, 1, 10, 4, 2, 10, pigeonholes(Direction.NORTH));
        b.set(5, 1, 10, barrel(Direction.UP));
        b.set(6, 1, 10, barrel(Direction.NORTH));
        b.chest(7, 1, 10, Direction.WEST, ModChestLoot.GAZETTE_OFFICE);
        b.set(7, 1, 7, ModBlocks.BAR_COUNTER.get());
        b.set(7, 2, 7, Blocks.LECTERN.defaultBlockState().setValue(net.minecraft.world.level.block.HorizontalDirectionalBlock.FACING, Direction.WEST));
        b.set(6, 1, 7, stool());
        b.set(cx, h, 4, hangingLantern());
        b.set(cx, h, 9, hangingLantern());
        b.set(1, 1, 0, barrel(Direction.UP));
        b.entity(ModEntities.TOWNSFOLK.get(), 3, 1, 6, tag("folk", "editor")); // the editor, selling the paper
        return b;
    }

    /** The homestead's floors: the ground floor, and the upper floor's boards at y FLOOR (its rooms from FLOOR + 1). */
    static final int FLOOR = 5;

    // 9 x 12, two storeys under a steep gable: a porch with a railing; a parlour with a stove and armchair, a dining table
    // for four and the kitchen at the back; a ladder up to the bedroom under the roof; a yard with a woodpile and chickens.
    static Blueprint homestead() {
        Blueprint b = new Blueprint("town/homestead");
        int w = 9, d = 10, cx = 4;
        b.air(0, 1, 0, w - 1, 14, d + 1);
        b.fill(0, 0, 0, w - 1, 0, d - 1, Blocks.SPRUCE_PLANKS);
        b.fill(0, 0, d, w - 1, 0, d + 1, Blocks.COARSE_DIRT);
        b.walls(0, 1, 2, w - 1, 8, d - 1, s(Blocks.BIRCH_PLANKS));
        for (int[] c : new int[][] {{0, 2}, {w - 1, 2}, {0, d - 1}, {w - 1, d - 1}}) {
            b.fill(c[0], 1, c[1], c[0], 8, c[1], log(Blocks.STRIPPED_BIRCH_LOG, Direction.Axis.Y));
        }
        b.walls(0, FLOOR, 2, w - 1, FLOOR, d - 1, log(Blocks.STRIPPED_BIRCH_LOG, Direction.Axis.X)); // the beam between storeys
        for (int z : new int[] {2, d - 1}) {
            b.fill(1, FLOOR, z, w - 2, FLOOR, z, log(Blocks.STRIPPED_BIRCH_LOG, Direction.Axis.X));
        }
        for (int x : new int[] {0, w - 1}) {
            b.fill(x, FLOOR, 3, x, FLOOR, d - 2, log(Blocks.STRIPPED_BIRCH_LOG, Direction.Axis.Z));
        }
        gableRoof(b, w, 2, d - 1, 9, Blocks.SPRUCE_STAIRS, slab(Blocks.SPRUCE_SLAB, SlabType.BOTTOM), s(Blocks.BIRCH_PLANKS));
        // Front: the door, windows up and down, a round window in the gable
        b.door(cx, 1, 2, (DoorBlock) Blocks.SPRUCE_DOOR, Direction.NORTH, DoorHingeSide.LEFT);
        for (int x : new int[] {2, 6}) {
            b.fill(x, 2, 2, x, 3, 2, Blocks.GLASS_PANE);
            b.fill(x, 7, 2, x, 8, 2, Blocks.GLASS_PANE);
        }
        b.set(cx, 10, 2, Blocks.GLASS_PANE);
        for (int z : new int[] {4, 7}) {
            b.set(0, 2, z, Blocks.GLASS_PANE);
            b.set(w - 1, 2, z, Blocks.GLASS_PANE);
            b.set(0, 7, z, Blocks.GLASS_PANE);
            b.set(w - 1, 7, z, Blocks.GLASS_PANE);
        }
        // The porch: posts, a rail with a gap at the steps, a slab roof, a swing seat and a pot plant
        for (int x : new int[] {0, cx - 1, cx + 1, w - 1}) {
            b.fill(x, 1, 0, x, 3, 0, Blocks.SPRUCE_FENCE);
        }
        b.fill(1, 1, 0, cx - 2, 1, 0, Blocks.SPRUCE_FENCE);
        b.fill(cx + 2, 1, 0, w - 2, 1, 0, Blocks.SPRUCE_FENCE);
        b.fill(0, 4, 0, w - 1, 4, 1, slab(Blocks.SPRUCE_SLAB, SlabType.BOTTOM));
        b.set(6, 1, 1, stairs(Blocks.OAK_STAIRS, Direction.SOUTH, Half.BOTTOM));
        b.set(7, 1, 1, stairs(Blocks.OAK_STAIRS, Direction.SOUTH, Half.BOTTOM));
        b.set(1, 1, 1, Blocks.POTTED_CACTUS);
        b.set(cx, 3, 1, hangingLantern());
        // The parlour: the stove (its pipe up through the bedroom), an armchair and a rug, a clock on the shelf
        TownDecor.stove(b, 1, 1, 5, Direction.EAST, 11);
        b.set(1, 1, 3, stairs(Blocks.WOOL_STAIRS.pick(DyeColor.GREEN), Direction.WEST, Half.BOTTOM));
        b.shelf(1, 2, 4, Blocks.SPRUCE_SHELF, Direction.EAST, Items.CLOCK, Items.CANDLE, Items.BOOK);
        TownDecor.rug(b, 2, 1, 3, 3, 5, DyeColor.RED, DyeColor.BROWN);
        // The dining table for four
        TownDecor.table(b, 6, 1, 4, Blocks.OAK_FENCE, Blocks.OAK_PRESSURE_PLATE);
        TownDecor.table(b, 6, 1, 5, Blocks.OAK_FENCE, Blocks.OAK_PRESSURE_PLATE);
        chair(b, 5, 1, 4, Blocks.OAK_STAIRS, Direction.WEST);
        chair(b, 5, 1, 5, Blocks.OAK_STAIRS, Direction.WEST);
        chair(b, 7, 1, 4, Blocks.OAK_STAIRS, Direction.EAST);
        chair(b, 7, 1, 5, Blocks.OAK_STAIRS, Direction.EAST);
        // The kitchen along the back: a range, the dresser with the crockery, a sink, the back door
        b.set(7, 1, 8, TownBlueprints.facing(Blocks.SMOKER, Direction.NORTH));
        b.set(6, 1, 8, ModBlocks.BAR_COUNTER.get());
        b.set(5, 1, 8, water(1));
        b.shelf(6, 2, 8, Blocks.SPRUCE_SHELF, Direction.NORTH, Items.BOWL, Items.BOWL, Items.BREAD);
        b.shelf(5, 2, 8, Blocks.SPRUCE_SHELF, Direction.NORTH, Items.EGG, Items.SUGAR, Items.WHEAT);
        b.set(7, 1, 7, barrel(Direction.WEST));
        TownDecor.backDoor(b, cx, d, (DoorBlock) Blocks.SPRUCE_DOOR);
        b.set(cx, 4, 5, hangingLantern());
        // The ladder up, in the north-west corner of the back
        b.fill(1, FLOOR, 3, w - 2, FLOOR, d - 2, Blocks.SPRUCE_PLANKS);
        for (int y = 1; y <= FLOOR + 1; y++) {
            b.set(1, y, d - 2, Blocks.LADDER.defaultBlockState().setValue(LadderBlock.FACING, Direction.EAST));
        }
        // The bedroom under the roof: two beds with a chest between, a wardrobe, a rug, the stovepipe
        int up = FLOOR + 1;
        b.air(1, up, 3, w - 2, up + 3, d - 2);
        b.set(1, FLOOR, d - 2, Blocks.LADDER.defaultBlockState().setValue(LadderBlock.FACING, Direction.EAST));
        b.set(1, up, d - 2, Blocks.LADDER.defaultBlockState().setValue(LadderBlock.FACING, Direction.EAST));
        b.fill(1, FLOOR, 5, 1, 10, 5, Blocks.POLISHED_DEEPSLATE_WALL);     // the stovepipe, on up through the floor
        b.bed(3, up, 4, Blocks.BED.blue(), Direction.NORTH);
        b.bed(6, up, 4, Blocks.BED.red(), Direction.NORTH);
        b.chest(4, up, 3, Direction.SOUTH, ModChestLoot.TOWN_HOUSE);
        b.set(5, up, 3, Blocks.POTTED_POPPY);
        b.shelf(7, up, 8, Blocks.SPRUCE_SHELF, Direction.WEST, Items.LEATHER_BOOTS, ModItems.BANDANA.get(), Items.LEATHER_HELMET);
        b.shelf(7, up + 1, 8, Blocks.SPRUCE_SHELF, Direction.WEST, Items.BOOK, Items.WRITABLE_BOOK, Items.CANDLE);
        TownDecor.rug(b, 3, up, 6, 6, 7, DyeColor.LIGHT_BLUE, DyeColor.BLUE);
        b.set(cx, 12, 5, hangingLantern());
        // The yard: the woodpile, a rain barrel, a chicken run against the back wall
        TownDecor.woodpile(b, 0, d, 3);
        TownDecor.rainBarrel(b, 3, d + 1);
        b.fill(5, 1, d, 5, 1, d + 1, Blocks.SPRUCE_FENCE);
        b.fill(5, 1, d + 1, 8, 1, d + 1, Blocks.SPRUCE_FENCE);
        b.set(8, 1, d, Blocks.SPRUCE_FENCE);
        b.set(5, 1, d, Blocks.SPRUCE_FENCE_GATE.defaultBlockState().setValue(FenceGateBlock.FACING, Direction.WEST));
        b.entity(EntityTypes.CHICKEN, 6, 1, d);
        b.entity(EntityTypes.CHICKEN, 7, 1, d);
        b.cat(2, 1, 1, "home");                                           // on the porch
        front(b, cx, s(Blocks.SPRUCE_PLANKS));
        return b;
    }

    // 9 x 15 little red schoolhouse at the end of a street: a belfry, rows of desks facing the blackboard, the teacher's
    // desk, a stove, books and slates.
    static Blueprint schoolhouse() {
        Blueprint b = new Blueprint("town/schoolhouse");
        int w = 9, d = 15, cx = 4;
        b.air(0, 1, 0, w - 1, 15, d - 1);
        b.fill(0, 0, 0, w - 1, 0, d - 1, Blocks.SPRUCE_PLANKS);
        b.fill(cx - 1, 0, 0, cx + 1, 0, 0, Blocks.COBBLESTONE);
        b.walls(0, 1, 1, w - 1, 5, d - 1, s(Blocks.MANGROVE_PLANKS));
        for (int[] c : new int[][] {{0, 1}, {w - 1, 1}, {0, d - 1}, {w - 1, d - 1}}) {
            b.fill(c[0], 1, c[1], c[0], 5, c[1], log(Blocks.STRIPPED_BIRCH_LOG, Direction.Axis.Y));
        }
        gableRoof(b, w, 1, d - 1, 6, Blocks.DARK_OAK_STAIRS, s(Blocks.DARK_OAK_PLANKS), s(Blocks.MANGROVE_PLANKS));
        b.door(cx, 1, 1, (DoorBlock) Blocks.SPRUCE_DOOR, Direction.NORTH, DoorHingeSide.LEFT);
        b.set(cx, 4, 1, Blocks.GLASS_PANE);
        b.wallSign(cx, 3, 0, Blocks.DARK_OAK_WALL_SIGN, Direction.NORTH, DyeColor.WHITE, false, "", TownDecor.signKey("schoolhouse"),
                TownDecor.signKey("schoolhouse") + ".sub");
        for (int z = 3; z < d - 2; z += 3) {
            b.fill(0, 2, z, 0, 4, z, Blocks.GLASS_PANE);
            b.fill(w - 1, 2, z, w - 1, 4, z, Blocks.GLASS_PANE);
        }
        // The belfry on the ridge over the door: four posts, the bell hung from its little roof
        b.fill(cx - 1, 10, 1, cx + 1, 10, 3, Blocks.DARK_OAK_PLANKS);
        for (int[] c : new int[][] {{cx - 1, 1}, {cx + 1, 1}, {cx - 1, 3}, {cx + 1, 3}}) {
            b.fill(c[0], 11, c[1], c[0], 12, c[1], Blocks.DARK_OAK_FENCE);
        }
        b.fill(cx - 1, 13, 1, cx + 1, 13, 3, Blocks.DARK_OAK_PLANKS);
        b.fill(cx - 1, 14, 1, cx + 1, 14, 3, slab(Blocks.DARK_OAK_SLAB, SlabType.BOTTOM));
        b.set(cx, 12, 2, Blocks.BELL.defaultBlockState().setValue(BellBlock.ATTACHMENT, BellAttachType.CEILING).setValue(BellBlock.FACING, Direction.NORTH));
        // Rows of desks either side of the aisle, the pupils facing the blackboard at the back
        BlockState desk = Blocks.SPRUCE_TRAPDOOR.defaultBlockState().setValue(TrapDoorBlock.HALF, Half.TOP);
        for (int z : new int[] {4, 7, 10}) {
            for (int x : new int[] {1, 2, 3, 5, 6, 7}) {
                b.set(x, 1, z, stairs(Blocks.OAK_STAIRS, Direction.NORTH, Half.BOTTOM));
                b.set(x, 1, z + 1, desk);
            }
        }
        b.fill(cx, 1, 2, cx, 1, 11, Blocks.CARPET.brown());
        // The teacher's desk, the blackboard behind it, books either side, the stove
        b.fill(cx - 1, 1, 12, cx + 1, 1, 12, ModBlocks.BAR_COUNTER.get());
        chair(b, cx, 1, 13, Blocks.SPRUCE_STAIRS, Direction.SOUTH);
        b.set(cx - 1, 2, 12, Blocks.LANTERN);
        b.set(cx + 1, 2, 12, Blocks.POTTED_RED_TULIP);                   // a flower for the teacher's desk
        b.fill(cx - 2, 2, d - 1, cx + 2, 3, d - 1, Blocks.POLISHED_BLACKSTONE);
        b.fill(cx - 2, 4, d - 1, cx + 2, 4, d - 1, log(Blocks.STRIPPED_BIRCH_LOG, Direction.Axis.X));
        b.fill(1, 1, 13, 1, 2, 13, Blocks.BOOKSHELF);
        b.shelf(7, 2, 12, Blocks.SPRUCE_SHELF, Direction.WEST, Items.BOOK, Items.APPLE, Items.BOOK);
        b.chest(7, 1, 13, Direction.WEST, ModChestLoot.SCHOOLHOUSE);
        TownDecor.stove(b, 7, 1, 2, Direction.WEST, 8);
        b.set(1, 1, 2, barrel(Direction.EAST));
        b.set(cx, 5, 6, hangingLantern());
        b.set(cx, 5, 10, hangingLantern());
        // Benches either side of the door
        TownDecor.bench(b, 2, 1, 0, Direction.SOUTH);
        b.entity(ModEntities.TOWNSFOLK.get(), 5, 1, 13, tag("folk", "teacher"));
        TownDecor.bench(b, 7, 1, 0, Direction.SOUTH);
        front(b, cx, s(Blocks.COBBLESTONE));
        return b;
    }
}
