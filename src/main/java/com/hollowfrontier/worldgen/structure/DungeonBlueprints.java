package com.hollowfrontier.worldgen.structure;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.function.Function;

import com.hollowfrontier.content.concavia.SealSocketBlock;
import com.hollowfrontier.registry.ModBlocks;
import com.hollowfrontier.registry.ModEntities;

import net.minecraft.core.Direction;
import net.minecraft.core.FrontAndTop;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.ButtonBlock;
import net.minecraft.world.level.block.CampfireBlock;
import net.minecraft.world.level.block.CandleBlock;
import net.minecraft.world.level.block.DispenserBlock;
import net.minecraft.world.level.block.DoorBlock;
import net.minecraft.world.level.block.LadderBlock;
import net.minecraft.world.level.block.SlabBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.AttachFace;
import net.minecraft.world.level.block.state.properties.DoorHingeSide;
import net.minecraft.world.level.block.state.properties.SlabType;

/**
 * The dungeons under three of the Hollow sites (26), each a kit of jigsaw pieces grown down from a connector in its site's
 * bottom layer ({@link #shaftEntry}). Every kit has the same shape:
 * <ol>
 * <li>a <b>shaft</b> with a ladder down from the site, {@link #SHAFT} deep (less under the Sun Temple);</li>
 * <li>a <b>hub</b> at its foot, a door each way: back to the shaft, forward on the path, and a side room either side;</li>
 * <li>the <b>path</b> forward to the <b>end room</b>, its connectors placed first (a higher priority) so the side rooms
 *     can't take its space: the end room is always there;</li>
 * <li><b>side rooms</b>, picked at random from the kit's pool, each with more doors to more rooms until the jigsaw runs
 *     out of depth or room, and a <b>cap</b> of rubble where a door leads nowhere (the pool's fallback).</li>
 * </ol>
 * Every room's floor is at its y 0 and its doors' connectors at y 1, three wide and three high, so the rooms all lie at
 * one level. The kits:
 * <ul>
 * <li><b>Kindled Catacombs</b> (under the gate): an ossuary hall, a guardian hall, collapsed galleries, burial niches; the
 *     end room is the sealed vault, its door a {@link ModBlocks#KINDLED_SEAL seal} that opens when phlogiston lamps stand on the
 *     sockets the mural over them shows ({@link SealSocketBlock}).</li>
 * <li><b>Buried Gray Complex</b> (under one Gray lab in three): cell blocks with captives in them (20), cold storage, a
 *     reactor with a Gray spawner pad, a security corridor; the end room is the data core.</li>
 * <li><b>Sun Temple Undercroft</b> (under the throne hall): a lava-lit treasury, trap corridors (pressure plates working
 *     dispensers of arrows and fire charges), offering rooms; the end room is the hoard, moved down here from the throne.</li>
 * </ul>
 */
public final class DungeonBlueprints {
    private DungeonBlueprints() {}

    /** How far down a dungeon's shaft runs from its site's bottom layer to the rooms' floor. */
    public static final int SHAFT = 14;
    /** Under the Sun Temple the shaft starts low in the ziggurat already. */
    public static final int UNDERCROFT_SHAFT = 9;
    /** The vault's puzzle variants (which sockets the mural marks). */
    public static final int VAULT_VARIANTS = 3;
    /** Each vault variant's marked sockets, left to right as you face the seal (four sockets). */
    static final boolean[][] VAULT_MARKS = {{true, false, true, false}, {false, true, true, false}, {true, false, false, true}};

    /** A dungeon's kit: its id (templates and pools under it), its stone, and the names its connectors go by. */
    public record Kit(String id, Function<Random, BlockState> wall, Function<Random, BlockState> floor, Function<Random, BlockState> ceiling, BlockState light) {
        public String name(String role) {
            return "hollowfrontier:" + id + "_" + role;
        }

        public String pool(String name) {
            return "hollowfrontier:" + id + "/" + name;
        }

        public String template(String name) {
            return id + "/" + name;
        }
    }

    public static final Kit CATACOMBS = new Kit("catacombs", Masonry::kindledWall, Masonry::kindledBase,
            r -> r.nextInt(5) == 0 ? Blocks.CHISELED_TUFF_BRICKS.defaultBlockState() : Blocks.TUFF_BRICKS.defaultBlockState(), Masonry.phlogistonLamp());
    public static final Kit GRAY_COMPLEX = new Kit("gray_complex",
            r -> (r.nextInt(9) == 0 ? Blocks.CONCRETE.lightGray() : Blocks.CONCRETE.white()).defaultBlockState(),
            r -> (r.nextInt(4) == 0 ? Blocks.SMOOTH_STONE : Blocks.CONCRETE.lightGray()).defaultBlockState(),
            r -> Blocks.SMOOTH_QUARTZ.defaultBlockState(), Blocks.SEA_LANTERN.defaultBlockState());
    public static final Kit UNDERCROFT = new Kit("undercroft",
            r -> (r.nextInt(5) == 0 ? Blocks.RESIN_BRICKS : ModBlocks.CRYSTAL_BRICKS.get()).defaultBlockState(),
            r -> (r.nextInt(3) == 0 ? Blocks.CUT_SANDSTONE : Blocks.SMOOTH_SANDSTONE).defaultBlockState(),
            r -> ModBlocks.CRYSTAL_BRICKS.get().defaultBlockState(), Blocks.SHROOMLIGHT.defaultBlockState());
    public static final List<Kit> KITS = List.of(CATACOMBS, GRAY_COMPLEX, UNDERCROFT);

    /** The templates in each kit's pools: the pool's name and the templates in it with their weights. */
    public record PoolSpec(String pool, List<String> templates, List<Integer> weights, String fallback) {}

    public static List<PoolSpec> pools(Kit kit) {
        List<PoolSpec> pools = new ArrayList<>();
        pools.add(new PoolSpec("shaft", List.of(kit.template("shaft")), List.of(1), "minecraft:empty"));
        pools.add(new PoolSpec("hub", List.of(kit.template("hub")), List.of(1), "minecraft:empty"));
        pools.add(new PoolSpec("path", List.of(kit.template("path")), List.of(1), "minecraft:empty"));
        List<String> ends = new ArrayList<>();
        if (kit == CATACOMBS) {
            for (int v = 0; v < VAULT_VARIANTS; v++) {
                ends.add(kit.template("end_" + v));
            }
        } else {
            ends.add(kit.template("end"));
        }
        pools.add(new PoolSpec("end", ends, ends.stream().map(e -> 1).toList(), "minecraft:empty"));
        List<String> rooms = roomNames(kit).stream().map(kit::template).toList();
        List<Integer> weights = roomNames(kit).stream().map(n -> n.startsWith("corridor") ? 3 : 2).toList();
        pools.add(new PoolSpec("rooms", rooms, weights, kit.pool("caps")));
        pools.add(new PoolSpec("caps", List.of(kit.template("cap")), List.of(1), "minecraft:empty"));
        return pools;
    }

    private static List<String> roomNames(Kit kit) {
        if (kit == CATACOMBS) {
            return List.of("corridor", "corner", "ossuary", "guardian_hall", "niche", "gallery");
        } else if (kit == GRAY_COMPLEX) {
            return List.of("corridor", "corner", "cells", "cold_storage", "reactor");
        }
        return List.of("corridor", "corner", "lava_gallery", "offering", "trap_hall");
    }

    public static List<Blueprint> all() {
        List<Blueprint> all = new ArrayList<>();
        // Catacombs
        all.add(shaft(CATACOMBS, SHAFT));
        all.add(catacombHub());
        all.add(catacombPath());
        for (int v = 0; v < VAULT_VARIANTS; v++) {
            all.add(catacombVault(v));
        }
        all.add(corridor(CATACOMBS, new Random(2601)));
        all.add(corner(CATACOMBS, new Random(2602)));
        all.add(ossuary());
        all.add(guardianHall());
        all.add(niche());
        all.add(gallery());
        all.add(cap(CATACOMBS, Blocks.TUFF_BRICKS.defaultBlockState(), Blocks.COBWEB.defaultBlockState()));
        // The Gray complex
        all.add(shaft(GRAY_COMPLEX, SHAFT));
        all.add(grayHub());
        all.add(grayPath());
        all.add(dataCore());
        all.add(corridor(GRAY_COMPLEX, new Random(2611)));
        all.add(corner(GRAY_COMPLEX, new Random(2612)));
        all.add(grayCells());
        all.add(coldStorage());
        all.add(grayReactor());
        all.add(cap(GRAY_COMPLEX, Blocks.CONCRETE.white().defaultBlockState(), Blocks.IRON_BARS.defaultBlockState()));
        // The undercroft
        all.add(shaft(UNDERCROFT, UNDERCROFT_SHAFT));
        all.add(treasury());
        all.add(trapCorridor("path", true, new Random(2621)));
        all.add(hoard());
        all.add(corridor(UNDERCROFT, new Random(2622)));
        all.add(corner(UNDERCROFT, new Random(2623)));
        all.add(lavaGallery());
        all.add(offering());
        all.add(trapCorridor("trap_hall", false, new Random(2624)));
        all.add(cap(UNDERCROFT, ModBlocks.CRYSTAL_BRICKS.get().defaultBlockState(), Blocks.SAND.defaultBlockState()));
        return all;
    }

    // ---------------------------------------------------------------- the frame of every kit

    /**
     * The connector in a site's bottom layer that the kit's shaft hangs from, its ladder facing {@code ladder} (so its top
     * points that way and the shaft's ladder lines up with the site's).
     */
    static void shaftEntry(Blueprint b, Kit kit, int x, int y, int z, Direction ladder) {
        BlockState rung = Blocks.LADDER.defaultBlockState().setValue(LadderBlock.FACING, ladder);
        b.jigsaw(x, y, z, FrontAndTop.fromFrontAndTop(Direction.DOWN, ladder), kit.name("down"), kit.name("shaft"), kit.pool("shaft"), rung, 20);
    }

    /** A room {@code w} x {@code d} outside (both odd), its floor at y 0 and {@code h} high inside under a ceiling. */
    static Blueprint room(String name, Kit kit, Random r, int w, int d, int h) {
        Blueprint b = new Blueprint(name);
        for (int x = 0; x < w; x++) {
            for (int z = 0; z < d; z++) {
                for (int y = 0; y <= h + 1; y++) {
                    boolean side = x == 0 || z == 0 || x == w - 1 || z == d - 1;
                    BlockState state = y == 0 ? kit.floor().apply(r) : y == h + 1 ? kit.ceiling().apply(r) : side ? kit.wall().apply(r) : Blocks.AIR.defaultBlockState();
                    b.set(x, y, z, state);
                }
            }
        }
        return b;
    }

    /** What a door's connector is called, what it joins, and the pool it opens onto. */
    private record Joint(String name, String target, String pool, int priority) {}

    private static Joint toRooms(Kit kit) {
        return new Joint(kit.name("door"), kit.name("door"), kit.pool("rooms"), 0);
    }

    private static Joint toPath(Kit kit, String pool) {
        return new Joint(kit.name("any"), kit.name("path_in"), kit.pool(pool), 10);
    }

    private static Joint pathIn(Kit kit) {
        return new Joint(kit.name("path_in"), kit.name("any"), "minecraft:empty", 0);
    }

    private static Joint hubIn(Kit kit) {
        return new Joint(kit.name("hub_in"), kit.name("any"), "minecraft:empty", 0);
    }

    /**
     * A doorway in the room's wall on {@code side} (w x d its outside), three wide and three high, its connector in the
     * bottom middle; the lintel over it in the kit's light, so every door is lit.
     */
    static void door(Blueprint b, Kit kit, int w, int d, Direction side, Joint joint) {
        int x = switch (side) {
            case WEST -> 0;
            case EAST -> w - 1;
            default -> w / 2;
        };
        int z = switch (side) {
            case NORTH -> 0;
            case SOUTH -> d - 1;
            default -> d / 2;
        };
        int ax = side.getAxis() == Direction.Axis.Z ? 1 : 0, az = 1 - ax;
        for (int k = -1; k <= 1; k++) {
            b.air(x + ax * k, 1, z + az * k, x + ax * k, 3, z + az * k);
        }
        b.set(x, 4, z, kit.light());
        b.jigsaw(x, 1, z, FrontAndTop.fromFrontAndTop(side, Direction.UP), joint.name(), joint.target(), joint.pool(), Blocks.AIR.defaultBlockState(), joint.priority());
    }

    /**
     * The shaft, 5 x 5 and {@code height} deep: a ladder down its north wall from the connector at its top (under the
     * site's) to its foot, where a door south opens onto the hub.
     */
    static Blueprint shaft(Kit kit, int height) {
        Random r = new Random(kit.id().hashCode());
        Blueprint b = new Blueprint(kit.template("shaft"));
        for (int x = 0; x < 5; x++) {
            for (int z = 0; z < 5; z++) {
                for (int y = 0; y < height; y++) {
                    boolean inside = x >= 1 && x <= 3 && z >= 1 && z <= 3 && y >= 1 && y <= height - 2;
                    b.set(x, y, z, y == 0 ? kit.floor().apply(r) : inside ? Blocks.AIR.defaultBlockState() : kit.wall().apply(r));
                }
            }
        }
        BlockState rung = Blocks.LADDER.defaultBlockState().setValue(LadderBlock.FACING, Direction.SOUTH);
        b.fill(2, 1, 1, 2, height - 2, 1, rung);
        for (int y = 4; y < height - 2; y += 4) {
            b.set(3, y, 0, kit.light());
        }
        b.jigsaw(2, height - 1, 1, FrontAndTop.fromFrontAndTop(Direction.UP, Direction.SOUTH), kit.name("shaft"), kit.name("down"), "minecraft:empty", rung);
        door(b, kit, 5, 5, Direction.SOUTH, new Joint(kit.name("any"), kit.name("hub_in"), kit.pool("hub"), 20));
        return b;
    }

    /** A plug of rubble 3 wide and 4 high where a door leads nowhere: the pool's fallback. */
    static Blueprint cap(Kit kit, BlockState rubble, BlockState odd) {
        Random r = new Random(kit.id().hashCode() + 7);
        Blueprint b = new Blueprint(kit.template("cap"));
        for (int x = 0; x < 3; x++) {
            for (int y = 0; y < 5; y++) {
                b.set(x, y, 0, r.nextInt(6) == 0 && y > 0 && y < 4 ? odd : y == 0 ? kit.floor().apply(r) : r.nextInt(3) == 0 ? rubble : kit.wall().apply(r));
            }
        }
        b.jigsaw(1, 1, 0, FrontAndTop.NORTH_UP, kit.name("door"), kit.name("door"), "minecraft:empty", kit.wall().apply(r));
        return b;
    }

    /** A plain corridor, 5 x 11, doors north and south, lit at its middle. */
    static Blueprint corridor(Kit kit, Random r) {
        Blueprint b = room(kit.template("corridor"), kit, r, 5, 11, 4);
        door(b, kit, 5, 11, Direction.NORTH, toRooms(kit));
        door(b, kit, 5, 11, Direction.SOUTH, toRooms(kit));
        b.set(2, 5, 5, kit.light());
        dress(b, kit, r, 1, 1, 3, 9);
        return b;
    }

    /** A corner, 7 x 7, doors north and east. */
    static Blueprint corner(Kit kit, Random r) {
        Blueprint b = room(kit.template("corner"), kit, r, 7, 7, 4);
        door(b, kit, 7, 7, Direction.NORTH, toRooms(kit));
        door(b, kit, 7, 7, Direction.EAST, toRooms(kit));
        b.set(3, 5, 3, kit.light());
        dress(b, kit, r, 1, 1, 5, 5);
        return b;
    }

    /** A little of each kit's mess along a room's walls: webs and bones, papers and glass, sand and sherds. */
    private static void dress(Blueprint b, Kit kit, Random r, int x0, int z0, int x1, int z1) {
        for (int i = 0; i < 4; i++) {
            int x = r.nextBoolean() ? x0 : x1, z = z0 + r.nextInt(z1 - z0 + 1);
            if (b.get(new net.minecraft.core.BlockPos(x, 1, z)) == null || !b.get(new net.minecraft.core.BlockPos(x, 1, z)).isAir()) {
                continue;
            }
            BlockState bit = kit == CATACOMBS ? (r.nextBoolean() ? Blocks.SKELETON_SKULL.defaultBlockState() : Blocks.CANDLE.defaultBlockState().setValue(CandleBlock.CANDLES, 2).setValue(CandleBlock.LIT, true))
                    : kit == GRAY_COMPLEX ? (r.nextBoolean() ? Blocks.CARPET.lightGray().defaultBlockState() : Blocks.GLASS_PANE.defaultBlockState())
                    : (r.nextBoolean() ? Blocks.SAND.defaultBlockState() : Blocks.CANDLE.defaultBlockState().setValue(CandleBlock.CANDLES, 3).setValue(CandleBlock.LIT, true));
            b.set(x, 1, z, bit);
        }
        if (kit == CATACOMBS) {
            b.set(x0, 4, z0, Blocks.COBWEB);
            b.set(x1, 4, z1, Blocks.COBWEB);
        }
    }

    // ---------------------------------------------------------------- Kindled Catacombs

    /** The catacombs' crossing, 11 x 11 and six high: four piers, burial shelves in the walls, a sarcophagus. */
    static Blueprint catacombHub() {
        Random r = new Random(2600);
        Kit k = CATACOMBS;
        Blueprint b = room(k.template("hub"), k, r, 11, 11, 6);
        door(b, k, 11, 11, Direction.NORTH, hubIn(k));
        door(b, k, 11, 11, Direction.SOUTH, toPath(k, "path"));
        door(b, k, 11, 11, Direction.EAST, toRooms(k));
        door(b, k, 11, 11, Direction.WEST, toRooms(k));
        for (int[] p : new int[][] {{3, 3}, {7, 3}, {3, 7}, {7, 7}}) {
            b.set(p[0], 1, p[1], Blocks.POLISHED_DEEPSLATE);
            b.fill(p[0], 2, p[1], p[0], 5, p[1], Blocks.QUARTZ_PILLAR);
            b.set(p[0], 6, p[1], Masonry.chiseledCrystal());
        }
        b.set(5, 7, 5, k.light());
        b.set(3, 7, 5, k.light());
        b.set(7, 7, 5, k.light());
        // The shelves: bones and skulls in the corners' walls
        for (int[] p : new int[][] {{1, 1}, {9, 1}, {1, 9}, {9, 9}}) {
            b.set(p[0], 2, p[1], Blocks.BONE_BLOCK);
            b.set(p[0], 3, p[1], Blocks.SKELETON_SKULL);
            b.set(p[0], 1, p[1], Blocks.CANDLE.defaultBlockState().setValue(CandleBlock.CANDLES, 3).setValue(CandleBlock.LIT, true));
        }
        // The sarcophagus between the piers, an offering pot at its head
        b.fill(4, 1, 5, 6, 1, 5, Blocks.CHISELED_DEEPSLATE);
        b.fill(4, 2, 5, 6, 2, 5, ModBlocks.CRYSTAL_BRICK_SLAB.get().defaultBlockState().setValue(SlabBlock.TYPE, SlabType.BOTTOM));
        b.pot(5, 1, 4, Direction.NORTH, ModChestLoot.KINDLED_OFFERING);
        return b;
    }

    /** The collapsed gallery on the way to the vault, 7 x 17: rubble heaped along it, a guardian on watch at its middle. */
    static Blueprint catacombPath() {
        Random r = new Random(2603);
        Kit k = CATACOMBS;
        Blueprint b = room(k.template("path"), k, r, 7, 17, 5);
        door(b, k, 7, 17, Direction.NORTH, pathIn(k));
        door(b, k, 7, 17, Direction.SOUTH, toPath(k, "end"));
        for (int z = 3; z <= 13; z += 5) {
            b.set(1, 6, z, k.light());
            b.set(5, 6, z, k.light());
        }
        Masonry.rubble(b, r, 1, 1, 8, 2.5, 8, Blocks.TUFF_BRICKS.defaultBlockState(), Blocks.TUFF_BRICK_SLAB.defaultBlockState(), Blocks.TUFF_BRICK_STAIRS.defaultBlockState());
        Masonry.rubble(b, r, 5, 1, 11, 2.5, 8, Blocks.TUFF_BRICKS.defaultBlockState(), Blocks.TUFF_BRICK_SLAB.defaultBlockState(), Blocks.COBWEB.defaultBlockState());
        b.fill(1, 4, 6, 2, 5, 7, Blocks.TUFF); // where the roof came down
        b.entity(ModEntities.CRYSTAL_GUARDIAN.get(), 3, 1, 8);
        return b;
    }

    /**
     * The vault (variant {@code variant}), 13 x 13: an antechamber, four sockets along its south side and over each, in
     * the wall, the mural's mark (a phlogiston lamp where a lamp must stand, dark tuff where none may); the seal in the wall's
     * middle, three wide and three high; behind it the vault itself, its chest on a starsteel plinth, pots, gold.
     */
    static Blueprint catacombVault(int variant) {
        Random r = new Random(2604 + variant);
        Kit k = CATACOMBS;
        Blueprint b = room(k.template("end_" + variant), k, r, 13, 13, 5);
        door(b, k, 13, 13, Direction.NORTH, pathIn(k));
        // The wall between the antechamber (z 1..5) and the vault (z 7..11), and the seal in it
        Masonry.kindledFill(b, r, 1, 1, 6, 11, 5, 6);
        b.fill(5, 1, 6, 7, 3, 6, ModBlocks.KINDLED_SEAL.get());
        b.set(6, 4, 6, Masonry.chiseledCrystal());
        int[] sockets = {2, 4, 8, 10};
        for (int i = 0; i < sockets.length; i++) {
            boolean marked = VAULT_MARKS[variant][i];
            b.set(sockets[i], 1, 5, ModBlocks.SEAL_SOCKET.get().defaultBlockState().setValue(SealSocketBlock.MARKED, marked));
            b.set(sockets[i], 3, 6, marked ? Masonry.phlogistonLamp() : Blocks.CHISELED_TUFF.defaultBlockState());
            b.set(sockets[i], 4, 6, marked ? Masonry.starsteel() : Blocks.POLISHED_TUFF.defaultBlockState());
        }
        b.wallSign(11, 2, 3, Blocks.SPRUCE_WALL_SIGN, Direction.WEST, DyeColor.CYAN, true,
                "sign.hollowfrontier.seal.1", "sign.hollowfrontier.seal.2", "sign.hollowfrontier.seal.3");
        b.set(1, 6, 3, k.light());
        b.set(11, 6, 3, k.light());
        // The vault behind the seal
        b.fill(1, 0, 7, 11, 0, 11, Masonry.crystalBricks());
        b.set(6, 1, 10, Masonry.starsteel());
        b.chest(6, 2, 10, Direction.NORTH, ModChestLoot.CATACOMB_VAULT);
        b.pot(2, 1, 11, Direction.EAST, ModChestLoot.KINDLED_OFFERING);
        b.pot(10, 1, 11, Direction.WEST, ModChestLoot.KINDLED_OFFERING);
        b.set(2, 1, 8, Blocks.GOLD_BLOCK);
        b.set(10, 1, 8, Blocks.RAW_GOLD_BLOCK);
        b.set(6, 6, 9, k.light());
        b.set(4, 1, 10, Blocks.CANDLE.defaultBlockState().setValue(CandleBlock.CANDLES, 4).setValue(CandleBlock.LIT, true));
        b.set(8, 1, 10, Blocks.CANDLE.defaultBlockState().setValue(CandleBlock.CANDLES, 4).setValue(CandleBlock.LIT, true));
        return b;
    }

    /** The ossuary, 11 x 9: walls of stacked bone and rows of skulls, candles, offering pots, a chest of grave goods. */
    static Blueprint ossuary() {
        Random r = new Random(2605);
        Kit k = CATACOMBS;
        Blueprint b = room(k.template("ossuary"), k, r, 11, 9, 4);
        for (int x = 1; x <= 9; x++) {
            for (int y = 1; y <= 3; y++) {
                if (Math.abs(x - 5) > 1) {
                    b.set(x, y, 1, (x + y) % 2 == 0 ? Blocks.BONE_BLOCK : Blocks.SKELETON_SKULL);
                    b.set(x, y, 7, (x + y) % 2 == 1 ? Blocks.BONE_BLOCK : Blocks.SKELETON_SKULL);
                }
            }
        }
        door(b, k, 11, 9, Direction.NORTH, toRooms(k));
        door(b, k, 11, 9, Direction.EAST, toRooms(k));
        door(b, k, 11, 9, Direction.WEST, toRooms(k));
        b.set(5, 5, 4, k.light());
        b.pot(2, 1, 6, Direction.NORTH, ModChestLoot.KINDLED_OFFERING);
        b.chest(8, 1, 6, Direction.NORTH, ModChestLoot.KINDLED_TOMB);
        b.set(5, 1, 6, Blocks.CANDLE.defaultBlockState().setValue(CandleBlock.CANDLES, 4).setValue(CandleBlock.LIT, true));
        return b;
    }

    /** The guardian hall, 9 x 13: two rows of piers, a Crystal Guardian standing between them, lamps over it. */
    static Blueprint guardianHall() {
        Random r = new Random(2606);
        Kit k = CATACOMBS;
        Blueprint b = room(k.template("guardian_hall"), k, r, 9, 13, 6);
        door(b, k, 9, 13, Direction.NORTH, toRooms(k));
        door(b, k, 9, 13, Direction.SOUTH, toRooms(k));
        for (int z = 3; z <= 9; z += 3) {
            for (int x : new int[] {2, 6}) {
                b.set(x, 1, z, Blocks.POLISHED_DEEPSLATE);
                b.fill(x, 2, z, x, 6, z, Blocks.POLISHED_TUFF);
            }
            b.set(4, 7, z, k.light());
        }
        b.entity(ModEntities.CRYSTAL_GUARDIAN.get(), 4, 1, 6);
        return b;
    }

    /** A burial niche (a dead end), 7 x 7: a sarcophagus under a crystal lid, a chest at its foot. */
    static Blueprint niche() {
        Random r = new Random(2607);
        Kit k = CATACOMBS;
        Blueprint b = room(k.template("niche"), k, r, 7, 7, 4);
        door(b, k, 7, 7, Direction.NORTH, toRooms(k));
        b.fill(2, 1, 4, 4, 1, 5, Blocks.CHISELED_DEEPSLATE);
        b.fill(2, 2, 4, 4, 2, 5, ModBlocks.CRYSTAL_BRICK_SLAB.get().defaultBlockState().setValue(SlabBlock.TYPE, SlabType.BOTTOM));
        b.chest(5, 1, 3, Direction.WEST, ModChestLoot.KINDLED_TOMB);
        b.set(1, 1, 5, Blocks.SKELETON_SKULL);
        b.set(3, 5, 3, k.light());
        return b;
    }

    /** A gallery half fallen in, 9 x 9, doors north and south: a ramp of rubble along one side, webs, a lamp still lit. */
    static Blueprint gallery() {
        Random r = new Random(2608);
        Kit k = CATACOMBS;
        Blueprint b = room(k.template("gallery"), k, r, 9, 9, 5);
        door(b, k, 9, 9, Direction.NORTH, toRooms(k));
        door(b, k, 9, 9, Direction.SOUTH, toRooms(k));
        for (int z = 1; z <= 7; z++) {
            for (int y = 1; y <= 1 + Math.abs(z - 4) / 2 + (z % 3 == 0 ? 1 : 0); y++) {
                b.set(1, y, z, Blocks.TUFF);
                b.set(2, y, z, r.nextBoolean() ? Blocks.TUFF_BRICKS : Blocks.GRAVEL);
            }
        }
        Masonry.rubble(b, r, 6, 1, 4, 2, 6, Blocks.TUFF_BRICK_SLAB.defaultBlockState(), Blocks.COBWEB.defaultBlockState());
        b.set(6, 6, 4, k.light());
        return b;
    }

    // ---------------------------------------------------------------- the buried Gray complex

    /** The lobby at the lift's foot, 11 x 11: a ring of lights in the floor, a console, a Warden on watch. */
    static Blueprint grayHub() {
        Random r = new Random(2610);
        Kit k = GRAY_COMPLEX;
        Blueprint b = room(k.template("hub"), k, r, 11, 11, 5);
        door(b, k, 11, 11, Direction.NORTH, hubIn(k));
        door(b, k, 11, 11, Direction.SOUTH, toPath(k, "path"));
        door(b, k, 11, 11, Direction.EAST, toRooms(k));
        door(b, k, 11, 11, Direction.WEST, toRooms(k));
        for (int x = 2; x <= 8; x++) {
            for (int z = 2; z <= 8; z++) {
                if (x == 2 || x == 8 || z == 2 || z == 8) {
                    b.set(x, 0, z, (x + z) % 2 == 0 ? Blocks.SEA_LANTERN : Blocks.CONCRETE.white());
                }
            }
        }
        b.set(5, 1, 5, Blocks.SMOOTH_QUARTZ);
        b.set(5, 2, 5, Blocks.TINTED_GLASS);
        b.set(5, 6, 5, k.light());
        b.entity(ModEntities.GRAY_WARDEN.get(), 4, 1, 7);
        return b;
    }

    /** The security corridor, 7 x 15: three scanner arches of tinted glass, end rods down from them, a Warden. */
    static Blueprint grayPath() {
        Random r = new Random(2613);
        Kit k = GRAY_COMPLEX;
        Blueprint b = room(k.template("path"), k, r, 7, 15, 4);
        door(b, k, 7, 15, Direction.NORTH, pathIn(k));
        door(b, k, 7, 15, Direction.SOUTH, toPath(k, "end"));
        for (int z = 4; z <= 10; z += 3) {
            b.fill(1, 4, z, 5, 4, z, Blocks.TINTED_GLASS);
            b.set(1, 3, z, Blocks.END_ROD.defaultBlockState().setValue(net.minecraft.world.level.block.EndRodBlock.FACING, Direction.DOWN));
            b.set(5, 3, z, Blocks.END_ROD.defaultBlockState().setValue(net.minecraft.world.level.block.EndRodBlock.FACING, Direction.DOWN));
            b.set(3, 0, z, Blocks.SEA_LANTERN);
        }
        b.entity(ModEntities.GRAY_WARDEN.get(), 3, 1, 7);
        return b;
    }

    /**
     * The data core, 13 x 13 and seven high: a column of glass round a core of phlogiston lamps in the middle, consoles round
     * the walls, the core's chest, two Wardens and a probe keeping it.
     */
    static Blueprint dataCore() {
        Random r = new Random(2614);
        Kit k = GRAY_COMPLEX;
        Blueprint b = room(k.template("end"), k, r, 13, 13, 7);
        door(b, k, 13, 13, Direction.NORTH, pathIn(k));
        for (int x = 4; x <= 8; x++) {
            for (int z = 4; z <= 8; z++) {
                double d = Math.hypot(x - 6, z - 6);
                if (d <= 2.5) {
                    b.set(x, 0, z, Blocks.IRON_BLOCK);
                    b.fill(x, 1, z, x, 7, z, d <= 1.0 ? Masonry.phlogistonLamp() : Blocks.GLASS.defaultBlockState());
                }
            }
        }
        for (int x = 2; x <= 10; x++) {
            b.set(x, 1, 11, x % 2 == 0 ? Blocks.COPPER_BULB.waxed().unaffected().defaultBlockState().setValue(net.minecraft.world.level.block.CopperBulbBlock.LIT, true)
                    : Blocks.SMOOTH_QUARTZ_SLAB.defaultBlockState().setValue(SlabBlock.TYPE, SlabType.TOP));
            b.set(x, 2, 12, Blocks.TINTED_GLASS);
        }
        b.chest(1, 1, 10, Direction.EAST, ModChestLoot.GRAY_COMPLEX_CORE);
        b.chest(11, 1, 10, Direction.WEST, ModChestLoot.GRAY_LAB_CONTROL);
        b.set(2, 8, 2, k.light());
        b.set(10, 8, 2, k.light());
        b.entity(ModEntities.GRAY_WARDEN.get(), 3, 1, 6);
        b.entity(ModEntities.GRAY_WARDEN.get(), 9, 1, 6);
        b.entity(ModEntities.GRAY_PROBE.get(), 6, 5, 2);
        return b;
    }

    /**
     * A cell block, 11 x 9: three cells behind bars on the south side, each with an iron door and a button out in the
     * corridor, two of them holding someone taken from the frontier (captives, as in the pens: free them and they talk).
     */
    static Blueprint grayCells() {
        Random r = new Random(2615);
        Kit k = GRAY_COMPLEX;
        Blueprint b = room(k.template("cells"), k, r, 11, 9, 4);
        door(b, k, 11, 9, Direction.NORTH, toRooms(k));
        door(b, k, 11, 9, Direction.EAST, toRooms(k));
        BlockState button = Blocks.POLISHED_BLACKSTONE_BUTTON.defaultBlockState().setValue(ButtonBlock.FACE, AttachFace.WALL).setValue(ButtonBlock.FACING, Direction.NORTH);
        for (int i = 0; i < 3; i++) {
            int x0 = 1 + i * 3;
            if (i > 0) {
                b.fill(x0 - 1, 1, 5, x0 - 1, 4, 7, Blocks.CONCRETE.white());
            }
            b.fill(x0, 1, 5, x0 + 1, 3, 5, Blocks.IRON_BARS);
            b.door(x0 + 1, 1, 5, (DoorBlock) Blocks.IRON_DOOR, Direction.NORTH, DoorHingeSide.LEFT);
            b.set(x0 + 2, 2, 4, button);
            b.set(x0 + 2, 1, 5, Blocks.CONCRETE.white());
            b.set(x0 + 2, 2, 5, Blocks.CONCRETE.white());
            b.set(x0 + 2, 3, 5, Blocks.CONCRETE.white());
            b.set(x0, 1, 7, Blocks.CARPET.white());
        }
        b.fill(10, 1, 5, 10, 4, 7, Blocks.CONCRETE.white());
        b.entity(ModEntities.OUTLAW.get(), 1, 1, 7, HollowBlueprints.captive(false));
        CompoundTag folk = HollowBlueprints.captive(false);
        folk.putString("folk", "prospector");
        b.entity(ModEntities.TOWNSFOLK.get(), 7, 1, 7, folk);
        b.set(5, 5, 2, k.light());
        b.entity(ModEntities.GRAY_WARDEN.get(), 5, 1, 2);
        return b;
    }

    /** Cold storage, 9 x 9: packed ice in the walls, specimens frozen in blocks of ice, a chest of the store's things. */
    static Blueprint coldStorage() {
        Random r = new Random(2616);
        Kit k = GRAY_COMPLEX;
        Blueprint b = room(k.template("cold_storage"), k, r, 9, 9, 4);
        b.map((pos, state) -> state.is(Blocks.CONCRETE.white()) && r.nextInt(3) == 0 ? Blocks.PACKED_ICE.defaultBlockState() : state);
        door(b, k, 9, 9, Direction.NORTH, toRooms(k));
        door(b, k, 9, 9, Direction.EAST, toRooms(k));
        for (int[] p : new int[][] {{1, 7}, {3, 7}, {5, 7}, {1, 5}}) {
            b.set(p[0], 1, p[1], Blocks.BLUE_ICE);
            b.set(p[0], 2, p[1], Blocks.ICE);
            b.set(p[0], 3, p[1], Blocks.PACKED_ICE);
        }
        b.set(3, 2, 7, Blocks.SKELETON_SKULL); // something in the ice
        b.set(1, 2, 5, Blocks.BONE_BLOCK);
        b.fill(2, 0, 2, 6, 0, 6, Blocks.PACKED_ICE);
        b.set(4, 1, 4, Blocks.POWDER_SNOW);
        b.chest(7, 1, 7, Direction.WEST, ModChestLoot.GRAY_LAB_STORE);
        b.set(4, 5, 4, k.light());
        return b;
    }

    /**
     * The reactor, 11 x 11 and six high: a glass column round phlogiston lamps in an iron frame, and by it the spawner pad the
     * complex's Wardens come from (the one spawner in it), ringed with lights.
     */
    static Blueprint grayReactor() {
        Random r = new Random(2617);
        Kit k = GRAY_COMPLEX;
        Blueprint b = room(k.template("reactor"), k, r, 11, 11, 6);
        door(b, k, 11, 11, Direction.NORTH, toRooms(k));
        door(b, k, 11, 11, Direction.SOUTH, toRooms(k));
        door(b, k, 11, 11, Direction.EAST, toRooms(k));
        for (int y = 1; y <= 6; y++) {
            b.fill(3, y, 3, 5, y, 5, Blocks.GLASS);
            b.set(4, y, 4, Masonry.phlogistonLamp());
        }
        for (int[] p : new int[][] {{2, 2}, {6, 2}, {2, 6}, {6, 6}}) {
            b.fill(p[0], 1, p[1], p[0], 6, p[1], Blocks.IRON_BLOCK);
        }
        CompoundTag spawner = new CompoundTag();
        spawner.putString("id", "minecraft:mob_spawner");
        CompoundTag entity = new CompoundTag();
        entity.putString("id", net.minecraft.core.registries.BuiltInRegistries.ENTITY_TYPE.getKey(ModEntities.GRAY_WARDEN.get()).toString());
        CompoundTag data = new CompoundTag();
        data.put("entity", entity);
        spawner.put("SpawnData", data);
        spawner.putShort("MinSpawnDelay", (short) 400);
        spawner.putShort("MaxSpawnDelay", (short) 900);
        spawner.putShort("SpawnCount", (short) 1);
        spawner.putShort("MaxNearbyEntities", (short) 3);
        b.blockEntity(8, 1, 8, Blocks.SPAWNER.defaultBlockState(), spawner);
        for (int[] p : new int[][] {{7, 7}, {9, 7}, {7, 9}, {9, 9}}) {
            b.set(p[0], 0, p[1], Blocks.SEA_LANTERN);
        }
        b.set(8, 0, 8, Blocks.IRON_BLOCK);
        return b;
    }

    // ---------------------------------------------------------------- the Sun Temple's undercroft

    /** The treasury, 11 x 15 and six high: lava behind glass in both long walls, piers, braziers, offering pots. */
    static Blueprint treasury() {
        Random r = new Random(2620);
        Kit k = UNDERCROFT;
        Blueprint b = room(k.template("hub"), k, r, 11, 15, 6);
        // The lava channels along the long walls (x 1 and 9), behind orange glass, stopped at each end so they can't
        // run out into the side doorways (z 6..8)
        for (int z = 2; z <= 12; z++) {
            boolean end = z == 2 || z == 5 || z == 9 || z == 12;
            if (z >= 6 && z <= 8) {
                continue;
            }
            for (int x : new int[] {1, 9}) {
                int glass = x == 1 ? 2 : 8;
                for (int y = 1; y <= 2; y++) {
                    b.set(x, y, z, end ? Blocks.RESIN_BRICKS.defaultBlockState() : Blocks.LAVA.defaultBlockState());
                    b.set(glass, y, z, end ? Blocks.RESIN_BRICKS.defaultBlockState() : Blocks.STAINED_GLASS.orange().defaultBlockState());
                }
                b.set(x, 3, z, Blocks.RESIN_BRICKS);
                b.set(glass, 3, z, Blocks.CHISELED_RESIN_BRICKS);
            }
        }
        door(b, k, 11, 15, Direction.NORTH, hubIn(k));
        door(b, k, 11, 15, Direction.SOUTH, toPath(k, "path"));
        door(b, k, 11, 15, Direction.EAST, toRooms(k));
        door(b, k, 11, 15, Direction.WEST, toRooms(k));
        for (int z = 4; z <= 10; z += 6) {
            for (int x : new int[] {3, 7}) {
                b.set(x, 1, z, Blocks.CHISELED_RESIN_BRICKS);
                b.fill(x, 2, z, x, 6, z, Blocks.RESIN_BRICKS);
            }
        }
        b.set(5, 1, 7, Blocks.CHISELED_RESIN_BRICKS);
        b.set(5, 2, 7, Blocks.CAMPFIRE.defaultBlockState().setValue(CampfireBlock.LIT, true));
        b.pot(3, 1, 12, Direction.NORTH, ModChestLoot.SUN_TEMPLE_OFFERING);
        b.pot(7, 1, 12, Direction.NORTH, ModChestLoot.SUN_TEMPLE_OFFERING);
        b.set(5, 7, 3, k.light());
        b.set(5, 7, 11, k.light());
        return b;
    }

    /**
     * A trap corridor, 5 x 15: three pressure plates down its middle, a dispenser in the wall beside each (arrows,
     * fire charges, arrows), the floor scorched where they've gone off. As the path ({@code path}) it leads on to the hoard.
     */
    static Blueprint trapCorridor(String name, boolean path, Random r) {
        Kit k = UNDERCROFT;
        Blueprint b = room(k.template(name), k, r, 5, 15, 4);
        door(b, k, 5, 15, Direction.NORTH, path ? pathIn(k) : toRooms(k));
        door(b, k, 5, 15, Direction.SOUTH, path ? toPath(k, "end") : toRooms(k));
        int[] plates = {4, 7, 10};
        for (int i = 0; i < plates.length; i++) {
            int z = plates[i];
            b.set(2, 1, z, Blocks.POLISHED_BLACKSTONE_PRESSURE_PLATE);
            boolean west = i % 2 == 0;
            int x = west ? 0 : 4;
            b.set(x, 1, z, ModBlocks.CRYSTAL_BRICKS.get()); // the wall stays whole round the dispenser
            dispenser(b, west ? 1 : 3, 1, z, west ? Direction.EAST : Direction.WEST, i == 1 ? net.minecraft.world.item.Items.FIRE_CHARGE : net.minecraft.world.item.Items.ARROW);
            b.set(west ? 1 : 3, 0, z + 1, Blocks.DYED_TERRACOTTA.black());
        }
        b.set(2, 5, 2, k.light());
        b.set(2, 5, 12, k.light());
        return b;
    }

    /** A dispenser facing {@code facing} with a stack of {@code ammo} in it. */
    private static void dispenser(Blueprint b, int x, int y, int z, Direction facing, net.minecraft.world.item.Item ammo) {
        CompoundTag nbt = new CompoundTag();
        nbt.putString("id", "minecraft:dispenser");
        ListTag items = new ListTag();
        CompoundTag stack = new CompoundTag();
        stack.putByte("Slot", (byte) 0);
        stack.putString("id", net.minecraft.core.registries.BuiltInRegistries.ITEM.getKey(ammo).toString());
        stack.putInt("count", 16);
        items.add(stack);
        nbt.put("Items", items);
        b.blockEntity(x, y, z, Blocks.DISPENSER.defaultBlockState().setValue(DispenserBlock.FACING, facing), nbt);
    }

    /**
     * The hoard, 11 x 11 and six high: the Archon's hoard (moved down here from beside its throne in 26) on a dais of
     * gold, a sun of shroomlight in the back wall, braziers, and the offerings either side.
     */
    static Blueprint hoard() {
        Random r = new Random(2625);
        Kit k = UNDERCROFT;
        Blueprint b = room(k.template("end"), k, r, 11, 11, 6);
        door(b, k, 11, 11, Direction.NORTH, pathIn(k));
        b.fill(3, 1, 7, 7, 1, 9, Blocks.CHISELED_RESIN_BRICKS);
        b.fill(4, 2, 8, 6, 2, 9, Blocks.GOLD_BLOCK);
        b.chest(5, 3, 8, Direction.NORTH, ModChestLoot.SUN_TEMPLE);
        b.chest(2, 1, 9, Direction.EAST, ModChestLoot.SUN_TEMPLE_OFFERING);
        b.pot(8, 1, 9, Direction.WEST, ModChestLoot.SUN_TEMPLE_OFFERING);
        for (int x = 3; x <= 7; x++) {
            for (int y = 2; y <= 6; y++) {
                double d = Math.hypot(x - 5, y - 4);
                if (d <= 2.3) {
                    b.set(x, y, 10, d <= 1.1 ? Blocks.GLOWSTONE : Blocks.SHROOMLIGHT);
                }
            }
        }
        for (int[] p : new int[][] {{1, 1}, {9, 1}}) {
            b.set(p[0], 1, p[1], Blocks.CHISELED_RESIN_BRICKS);
            b.set(p[0], 2, p[1], Blocks.CAMPFIRE.defaultBlockState().setValue(CampfireBlock.LIT, true));
        }
        return b;
    }

    /** The lava gallery, 9 x 9: a pool of lava behind bars across its middle, a walk round it, doors north and south. */
    static Blueprint lavaGallery() {
        Random r = new Random(2626);
        Kit k = UNDERCROFT;
        Blueprint b = room(k.template("lava_gallery"), k, r, 9, 9, 5);
        door(b, k, 9, 9, Direction.NORTH, toRooms(k));
        door(b, k, 9, 9, Direction.SOUTH, toRooms(k));
        // Two troughs of lava either side of the walk (x 4), ringed with bars, so it lies still in them
        for (int x0 : new int[] {1, 5}) {
            for (int x = x0; x <= x0 + 2; x++) {
                for (int z = 2; z <= 6; z++) {
                    boolean ring = x == x0 || x == x0 + 2 || z == 2 || z == 6;
                    b.set(x, 1, z, ring ? Blocks.IRON_BARS.defaultBlockState() : Blocks.LAVA.defaultBlockState());
                }
            }
        }
        b.set(4, 6, 4, k.light());
        return b;
    }

    /** An offering room (a dead end), 7 x 7: pots on shelves of stone, a chest, a brazier. */
    static Blueprint offering() {
        Random r = new Random(2627);
        Kit k = UNDERCROFT;
        Blueprint b = room(k.template("offering"), k, r, 7, 7, 4);
        door(b, k, 7, 7, Direction.NORTH, toRooms(k));
        for (int x = 1; x <= 5; x += 2) {
            b.pot(x, 1, 5, Direction.NORTH, ModChestLoot.SUN_TEMPLE_OFFERING);
        }
        b.chest(5, 1, 3, Direction.WEST, ModChestLoot.SUN_TEMPLE_OFFERING);
        b.set(1, 1, 2, Blocks.CHISELED_RESIN_BRICKS);
        b.set(1, 2, 2, Blocks.CAMPFIRE.defaultBlockState().setValue(CampfireBlock.LIT, true));
        b.set(3, 5, 3, k.light());
        return b;
    }
}
