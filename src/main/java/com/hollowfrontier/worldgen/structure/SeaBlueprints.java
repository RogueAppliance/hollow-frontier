package com.hollowfrontier.worldgen.structure;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import com.hollowfrontier.registry.ModBlocks;
import com.hollowfrontier.registry.ModEntities;
import com.hollowfrontier.registry.ModItems;

import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.network.Filterable;
import net.minecraft.world.item.component.WrittenBookContent;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.CampfireBlock;
import net.minecraft.world.level.block.ChestBlock;
import net.minecraft.world.level.block.LecternBlock;
import net.minecraft.world.level.block.SlabBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.SlabType;
import net.minecraft.world.level.storage.loot.LootTable;

/**
 * The Still Sea's sites (43d).
 * <ul>
 * <li><b>The Drowned Quarter</b>: Concavia's river port, the lowest ground in the country, on the bed of the sea that was
 *     let in over it. A {@link HollowSiteStructure} in seabed mode, its floor ({@code y 0}) on the bed. A quay along the
 *     north side and a colonnaded street south from it, paved and broken, lit by Jelly Lamps that never went out; two
 *     roofless houses with what was left in them; and three <b>domes</b> of glass ribbed in crystal that still hold their
 *     air. Each dome stands on a stone drum with a flooded passage through it to a pool in the dome's floor, so a diver
 *     swims in under the wall and comes up inside. The great dome is the port's counting house: its vault, the Kindled's
 *     own record of the day the water came, and two Crystal Guardians still on watch. One of the small ones has a third.</li>
 * <li><b>The Symmes Landing</b>: on a beach of the Still Sea, the expedition's boat drawn up and never launched again: a
 *     tent, a cold fire, a crate, and the boat's log on a lectern, which says what they saw out in the water.</li>
 * </ul>
 * Everything set outside the domes is a full block or is waterlogged ({@link #wet}): the template is placed ignoring
 * waterlogging, and a dry stair or chest under the sea would stand in a bubble of its own.
 */
public final class SeaBlueprints {
    private SeaBlueprints() {}

    public static final String DROWNED_QUARTER = "drowned_quarter";
    public static final String SYMMES_LANDING = "symmes_landing";
    /** The Kindled record in the great dome: {@code book.hollowfrontier.drowned_record.<n>}. */
    public static final int RECORD_PAGES = 4;
    /** The boat's log at the landing: {@code book.hollowfrontier.symmes_log.<n>}. */
    public static final int LOG_PAGES = 4;
    /** The Quarter's footprint, and the domes: centre x, centre z, radius. */
    public static final int SIZE = 40;
    public static final int[] GREAT_DOME = {20, 25, 7}, WEST_DOME = {8, 12, 4}, EAST_DOME = {33, 31, 4};
    /** The top of a dome's drum, which is its floor; its air starts on the layer above. */
    public static final int DRUM = 3;

    public static List<Blueprint> all() {
        return List.of(drownedQuarter(), symmesLanding());
    }

    /** {@code state} with water in it, if it's a block that can hold water. */
    static BlockState wet(BlockState state) {
        return state.hasProperty(BlockStateProperties.WATERLOGGED) ? state.setValue(BlockStateProperties.WATERLOGGED, true) : state;
    }

    static Blueprint drownedQuarter() {
        Blueprint b = new Blueprint(DROWNED_QUARTER);
        Random r = new Random(4343);
        BlockState water = Blocks.WATER.defaultBlockState();
        BlockState lamp = ModBlocks.JELLY_LAMP.get().defaultBlockState();
        // The pavement: a rough round of Kindled paving on the bed, one stone in seven gone
        for (int x = 0; x < SIZE; x++) {
            for (int z = 0; z < SIZE; z++) {
                double d = Math.hypot(x - 20, z - 20);
                if (d < 20.5 && (d < 17 ? r.nextInt(7) != 0 : r.nextInt(3) == 0)) {
                    b.set(x, 0, z, Masonry.kindledPaving(r));
                }
            }
        }
        // The quay along the north side: a wall two courses high with a mooring stone every six blocks
        for (int x = 6; x <= 34; x++) {
            for (int z = 0; z <= 2; z++) {
                b.set(x, 0, z, Masonry.kindledBase(r));
                b.set(x, 1, z, Masonry.kindledBase(r));
                if (z < 2 || r.nextInt(5) != 0) {
                    b.set(x, 2, z, Masonry.kindledBase(r));
                }
            }
            if (x % 6 == 2) {
                b.set(x, 3, 1, Blocks.POLISHED_BLACKSTONE);
                b.set(x, 4, 1, Blocks.CHISELED_POLISHED_BLACKSTONE);
            }
        }
        // The street south from the quay to the great dome: a crystal line down its middle, columns either side, some standing
        for (int z = 3; z <= 15; z++) {
            for (int x = 18; x <= 22; x++) {
                b.set(x, 0, z, x == 20 ? Masonry.crystalBricks() : Masonry.kindledPaving(r));
            }
        }
        for (int z = 4; z <= 14; z += 5) {
            for (int x : new int[] {16, 24}) {
                int height = 2 + r.nextInt(5);
                b.set(x, 0, z, Blocks.POLISHED_DEEPSLATE);
                b.fill(x, 1, z, x, height, z, Blocks.QUARTZ_PILLAR);
                if (height >= 5) {
                    b.set(x, height + 1, z, lamp); // a whole column still carries its lamp
                } else {
                    Masonry.rubble(b, r, x + (x < 20 ? -2 : 2), 1, z + 1, 2, 3, Blocks.QUARTZ_BLOCK.defaultBlockState(), Blocks.QUARTZ_PILLAR.defaultBlockState());
                }
            }
        }
        // Lamps low along the street and round the square
        for (int[] at : new int[][] {{18, 6}, {22, 10}, {12, 20}, {28, 20}, {12, 33}, {28, 12}}) {
            b.set(at[0], 1, at[1], Blocks.POLISHED_DEEPSLATE);
            b.set(at[0], 2, at[1], lamp);
        }
        // Two houses, roofless: tuff walls with gaps, a doorway, pots, a chest
        house(b, r, 27, 4, 34, 10, Direction.SOUTH);
        house(b, r, 4, 26, 11, 33, Direction.NORTH);
        Masonry.rubble(b, r, 14, 1, 30, 3, 6, Blocks.TUFF_BRICKS.defaultBlockState(), Blocks.TUFF.defaultBlockState());
        Masonry.rubble(b, r, 30, 1, 22, 3, 5, Blocks.TUFF_BRICKS.defaultBlockState(), Blocks.POLISHED_TUFF.defaultBlockState());

        // The domes, each entered from under: the great one from the north (the street), the small ones from the square
        dome(b, r, GREAT_DOME, Direction.NORTH, 2);
        dome(b, r, WEST_DOME, Direction.EAST, 1);
        dome(b, r, EAST_DOME, Direction.WEST, 1);
        int f = DRUM + 1;
        // The great dome, the counting house: the vault at the back, the record on its lectern, pots, two guardians
        int gx = GREAT_DOME[0], gz = GREAT_DOME[1];
        b.set(gx, f, gz + 5, Masonry.chiseledCrystal());
        b.chest(gx, f + 1, gz + 5, Direction.NORTH, ModChestLoot.DROWNED_VAULT);
        b.blockEntity(gx - 4, f, gz + 2, Blocks.LECTERN.defaultBlockState().setValue(LecternBlock.FACING, Direction.EAST).setValue(LecternBlock.HAS_BOOK, true),
                lectern(record()));
        b.pot(gx + 4, f, gz + 2, Direction.WEST, ModChestLoot.DROWNED_HOME, ModItems.WAVE_POTTERY_SHERD.get(), null, null, ModItems.WAVE_POTTERY_SHERD.get());
        b.pot(gx - 3, f, gz + 4, Direction.NORTH, null, null, ModItems.CRYSTAL_POTTERY_SHERD.get(), null, ModItems.WAVE_POTTERY_SHERD.get());
        b.set(gx + 3, f, gz + 4, lamp);
        b.set(gx - 5, f, gz - 1, Masonry.crystalBricks());
        b.set(gx + 5, f, gz - 1, Masonry.crystalBricks());
        b.entity(ModEntities.CRYSTAL_GUARDIAN.get(), gx - 3, f, gz - 3);
        b.entity(ModEntities.CRYSTAL_GUARDIAN.get(), gx + 3, f, gz - 3);
        // The west dome, a home: a chest, a pot, a lamp
        b.chest(WEST_DOME[0], f, WEST_DOME[1] + 2, Direction.NORTH, ModChestLoot.DROWNED_HOME);
        b.pot(WEST_DOME[0] - 2, f, WEST_DOME[1], Direction.EAST, null, ModItems.WAVE_POTTERY_SHERD.get(), null, null, ModItems.SUN_POTTERY_SHERD.get());
        b.set(WEST_DOME[0], f, WEST_DOME[1] - 2, lamp);
        // The east dome, a shop: a chest, a pot with something in it, and the third guardian
        b.chest(EAST_DOME[0], f, EAST_DOME[1] - 2, Direction.SOUTH, ModChestLoot.DROWNED_HOME);
        b.pot(EAST_DOME[0] + 2, f, EAST_DOME[1], Direction.WEST, ModChestLoot.DROWNED_STREET, ModItems.WAVE_POTTERY_SHERD.get());
        b.set(EAST_DOME[0], f, EAST_DOME[1] + 2, lamp);
        b.entity(ModEntities.CRYSTAL_GUARDIAN.get(), EAST_DOME[0] + 1, f, EAST_DOME[1] + 1);
        return b;
    }

    /**
     * A dome on its drum: a drum of deepslate tile {@link #DRUM} high and a block wider than the dome all round; a
     * hemisphere of glass over it with four ribs of crystal brick and a Jelly Lamp for a keystone, air inside; a flooded
     * passage {@code width} wide through the drum from its {@code door} side to its middle, and a pool of the same width
     * in the floor there. Water doesn't climb, so the pool lies level with the floor and the dome stays dry.
     */
    private static void dome(Blueprint b, Random r, int[] dome, Direction door, int width) {
        int cx = dome[0], cz = dome[1], radius = dome[2];
        BlockState water = Blocks.WATER.defaultBlockState();
        for (int x = cx - radius - 1; x <= cx + radius + 1; x++) {
            for (int z = cz - radius - 1; z <= cz + radius + 1; z++) {
                double d = Math.hypot(x - cx, z - cz);
                if (d > radius + 1.3) {
                    continue;
                }
                for (int y = 0; y <= DRUM; y++) {
                    b.set(x, y, z, Masonry.kindledBase(r));
                }
                for (int y = DRUM + 1; y <= DRUM + radius + 1; y++) {
                    double shell = Math.sqrt(sq(x - cx) + sq(y - DRUM - 0.5) + sq(z - cz));
                    if (shell > radius + 0.5) {
                        continue;
                    }
                    if (shell > radius - 0.6) {
                        boolean rib = x == cx || z == cz;
                        b.set(x, y, z, rib ? Masonry.crystalBricks() : Blocks.GLASS.defaultBlockState());
                    } else {
                        b.set(x, y, z, Blocks.AIR.defaultBlockState());
                    }
                }
            }
        }
        b.set(cx, DRUM + radius, cz, ModBlocks.JELLY_LAMP.get().defaultBlockState());
        // The passage: from the drum's edge on the door side in to the middle, two blocks high, and the pool over its end
        for (int step = 0; step <= radius + 1; step++) {
            for (int w = 0; w < width; w++) {
                int x = cx + door.getStepX() * step + (door.getAxis() == Direction.Axis.Z ? w : 0);
                int z = cz + door.getStepZ() * step + (door.getAxis() == Direction.Axis.X ? w : 0);
                b.set(x, 1, z, water);
                b.set(x, 2, z, water);
                if (step == 0) {
                    b.set(x, DRUM, z, water);
                }
            }
        }
    }

    /** A roofless house: walls three high, more of them gone the higher they go, a doorway in {@code door}'s side, and what's left inside. */
    private static void house(Blueprint b, Random r, int x0, int z0, int x1, int z1, Direction door) {
        for (int x = x0; x <= x1; x++) {
            for (int z = z0; z <= z1; z++) {
                boolean wall = x == x0 || x == x1 || z == z0 || z == z1;
                b.set(x, 0, z, wall ? Masonry.kindledBase(r) : Masonry.kindledPaving(r));
                if (!wall) {
                    continue;
                }
                int height = 1 + r.nextInt(3);
                for (int y = 1; y <= height; y++) {
                    b.set(x, y, z, Masonry.kindledWall(r));
                }
            }
        }
        int mx = (x0 + x1) / 2, mz = (z0 + z1) / 2;
        int dx = door.getAxis() == Direction.Axis.X ? (door == Direction.EAST ? x1 : x0) : mx;
        int dz = door.getAxis() == Direction.Axis.Z ? (door == Direction.SOUTH ? z1 : z0) : mz;
        for (int y = 1; y <= 3; y++) {
            b.set(dx, y, dz, Blocks.WATER.defaultBlockState());
        }
        chest(b, x0 + 1, 1, z0 + 1, door, ModChestLoot.DROWNED_STREET);
        CompoundTag pot = potNbt();
        pot.putString("LootTable", ModChestLoot.DROWNED_STREET.identifier().toString());
        b.blockEntity(x1 - 1, 1, z0 + 1, wet(Blocks.DECORATED_POT.defaultBlockState()), pot);
        // Silt in two corners, for a brush
        b.suspicious(x0 + 1, 1, z1 - 1, Blocks.SUSPICIOUS_SAND, ModChestLoot.DIG_DROWNED_QUARTER);
        b.suspicious(mx + 1, 1, mz + 1, Blocks.SUSPICIOUS_SAND, ModChestLoot.DIG_DROWNED_QUARTER);
        b.blockEntity(x1 - 1, 1, z1 - 1, wet(Blocks.DECORATED_POT.defaultBlockState()), potNbt());
        b.set(mx, 1, mz, wet(Blocks.TUFF_BRICK_SLAB.defaultBlockState().setValue(SlabBlock.TYPE, SlabType.BOTTOM)));
    }

    /** A chest under water: waterlogged, so it doesn't stand in a bubble. */
    private static void chest(Blueprint b, int x, int y, int z, Direction facing, ResourceKey<LootTable> table) {
        CompoundTag nbt = new CompoundTag();
        nbt.putString("id", "minecraft:chest");
        nbt.putString("LootTable", table.identifier().toString());
        b.blockEntity(x, y, z, wet(Blocks.CHEST.defaultBlockState().setValue(ChestBlock.FACING, facing)), nbt);
    }

    private static CompoundTag potNbt() {
        CompoundTag nbt = new CompoundTag();
        nbt.putString("id", "minecraft:decorated_pot");
        return nbt;
    }

    static Blueprint symmesLanding() {
        Blueprint b = new Blueprint(SYMMES_LANDING);
        Random r = new Random(1890);
        // The beach under it, and the air over it
        for (int x = 0; x <= 15; x++) {
            for (int z = 0; z <= 11; z++) {
                if (sq((x - 7.5) / 8.0) + sq((z - 5.5) / 6.0) < 1) {
                    b.set(x, 0, z, r.nextInt(9) == 0 ? Blocks.GRAVEL : Blocks.SAND);
                }
            }
        }
        b.air(0, 1, 0, 15, 5, 11);
        // The boat, drawn up bow to the land: a keel of planks, slab gunwales, a stair at the bow and stern
        BlockState gunwale = Blocks.SPRUCE_SLAB.defaultBlockState();
        for (int x = 2; x <= 8; x++) {
            b.set(x, 0, 4, Blocks.SPRUCE_PLANKS);
            b.set(x, 1, 3, gunwale);
            b.set(x, 1, 5, gunwale);
        }
        b.set(1, 1, 4, Masonry.stair(Blocks.SPRUCE_STAIRS, Direction.EAST));
        b.set(9, 1, 4, Masonry.stair(Blocks.SPRUCE_STAIRS, Direction.WEST));
        b.set(5, 1, 4, Blocks.SPRUCE_SLAB.defaultBlockState()); // the thwart
        b.set(8, 1, 4, Blocks.LANTERN);
        // The tent: an A-frame of canvas with a bedroll
        for (int z = 1; z <= 3; z++) {
            b.set(11, 1, z, Blocks.WOOL.white());
            b.set(13, 1, z, Blocks.WOOL.white());
            b.set(12, 2, z, r.nextInt(4) == 0 ? Blocks.WOOL.lightGray() : Blocks.WOOL.white());
        }
        b.bed(12, 1, 2, Blocks.BED.brown(), Direction.NORTH);
        // The fire, long cold, with a log to sit on; the crate and a barrel; the log on its lectern
        b.set(11, 1, 7, Blocks.CAMPFIRE.defaultBlockState().setValue(CampfireBlock.LIT, false));
        b.set(13, 1, 7, Blocks.SPRUCE_LOG.defaultBlockState().setValue(BlockStateProperties.AXIS, Direction.Axis.Z));
        b.chest(6, 1, 8, Direction.NORTH, ModChestLoot.SYMMES_LANDING);
        b.set(7, 1, 8, Blocks.BARREL);
        b.blockEntity(9, 1, 8, Blocks.LECTERN.defaultBlockState().setValue(LecternBlock.FACING, Direction.NORTH).setValue(LecternBlock.HAS_BOOK, true),
                lectern(log()));
        return b;
    }

    // ---------------------------------------------------------------- the books

    /** The Kindled's record of the day the water came, in the great dome. */
    public static CompoundTag record() {
        return book("book.hollowfrontier.drowned_record.", RECORD_PAGES, "The Day the Water Came", "A keeper of the port");
    }

    /** The boat's log at the Symmes Landing. */
    public static CompoundTag log() {
        return book("book.hollowfrontier.symmes_log.", LOG_PAGES, "Boat's Log", "T. Avery");
    }

    // Built as NBT by hand, as HollowBlueprints.journal is: items have no components bound during datagen.
    private static CompoundTag book(String key, int pageCount, String title, String author) {
        List<Filterable<Component>> pages = new ArrayList<>();
        for (int i = 1; i <= pageCount; i++) {
            pages.add(Filterable.passThrough(Component.translatable(key + i)));
        }
        WrittenBookContent content = new WrittenBookContent(Filterable.passThrough(title), author, 0, pages, true);
        CompoundTag components = new CompoundTag();
        components.put("minecraft:written_book_content", WrittenBookContent.CODEC.encodeStart(NbtOps.INSTANCE, content).getOrThrow());
        CompoundTag book = new CompoundTag();
        book.putString("id", "minecraft:written_book");
        book.putInt("count", 1);
        book.put("components", components);
        return book;
    }

    private static CompoundTag lectern(CompoundTag book) {
        CompoundTag nbt = new CompoundTag();
        nbt.putString("id", "minecraft:lectern");
        nbt.put("Book", book);
        nbt.putInt("Page", 0);
        return nbt;
    }

    private static double sq(double v) {
        return v * v;
    }
}
