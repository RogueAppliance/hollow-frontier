package com.hollowfrontier.worldgen.structure;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Random;
import java.util.Set;

import com.hollowfrontier.HollowFrontier;
import com.hollowfrontier.content.hollow.gray.Captive;
import com.hollowfrontier.content.hollow.gray.Captives;
import com.hollowfrontier.content.hollow.gray.Specimens;
import com.hollowfrontier.registry.ModBlocks;
import com.hollowfrontier.registry.ModEntities;
import com.hollowfrontier.registry.ModItems;

import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.network.chat.Component;
import net.minecraft.server.network.Filterable;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.WrittenBookContent;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.ButtonBlock;
import net.minecraft.world.level.block.DoorBlock;
import net.minecraft.world.level.block.CampfireBlock;
import net.minecraft.world.level.block.CandleBlock;
import net.minecraft.world.level.block.ChainBlock;
import net.minecraft.world.level.block.EndRodBlock;
import net.minecraft.world.level.block.LadderBlock;
import net.minecraft.world.level.block.LanternBlock;
import net.minecraft.world.level.block.LecternBlock;
import net.minecraft.world.level.block.RotatedPillarBlock;
import net.minecraft.world.level.block.SeaPickleBlock;
import net.minecraft.world.level.block.SlabBlock;
import net.minecraft.world.level.block.StairBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.AttachFace;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.DoorHingeSide;
import net.minecraft.world.level.block.state.properties.SlabType;

/**
 * The Hollow Earth's lore sites (9b3), each one piece of a {@link HollowSiteStructure}. Every blueprint has its floor
 * ({@link #FLOOR}, its bottom layer) set on the ground, clears the air over its footprint, and builds from there.
 * <ul>
 * <li><b>Kindled sites</b> (Concavian Highlands), one of three: a <b>henge</b>, a ring of trilithons round a stepped
 *     dais and an obelisk with a lodestone heart and a blue fire on top, paved avenues out to four gateways; a
 *     <b>gate</b>, a stepped quartz front cut into a snow-capped hill between two spear-bearing guardians, its great iron
 *     door shut for good on a hall of columns, a tomb and a vault; and a ruined <b>temple</b>, a colonnade (some of it
 *     fallen) round a chamber open to the sky whose back wall holds the mural of the world as its builders knew it: a
 *     shell with a sun inside and an opening at each pole. Phase 11 tells who the builders were.</li>
 * <li>An abandoned <b>Gray field lab</b>: a dome with a band of dark windows over four restraint tables under a ring of
 *     lamps, specimen tanks round its wall (one cracked and dry); a scorched landing pad; an antenna mast with a dish; a
 *     block of cages, one holding an ear-tagged Glassback Hare; and two Gray Probes watching over it all.</li>
 * <li>A live <b>Gray field station</b> (10b), lit and working: tagged megafauna in glass pens, a lab with a hare on the
 *     operating table and glow squid in the tanks, a phlogiston reactor, a mast; sentinels on watch and two specimens loose.</li>
 * <li>A lost <b>Symmes expedition camp</b>: the expedition's airship, crashed, its envelope collapsed over the north side
 *     of the camp and its gondola broken off (boiler, propeller and all); a palisade of stakes, smashed where the ship
 *     came down and burned where something landed; a command tent with the expedition's journal on a lectern
 *     ({@link #journal}); two small tents (one torn down), a cold fire, stores, a watchtower, and three graves, the last
 *     of them open and empty.</li>
 * </ul>
 */
public final class HollowBlueprints {
    private HollowBlueprints() {}

    public static final String KINDLED_HENGE = "kindled_henge";
    public static final String KINDLED_GATE = "kindled_gate";
    public static final String KINDLED_TEMPLE = "kindled_temple";
    public static final String GRAY_LAB = "gray_lab";
    public static final String GRAY_STATION = "gray_station";
    public static final String SYMMES_CAMP = "symmes_camp";
    public static final String GRAY_PENS = "gray_pens";
    public static final int PENS_VARIANTS = 3;
    /**
     * Each site's floor is its bottom layer, set level with the ground: the terrain blend (BEARD_THIN) lines the land up
     * with a piece's bottom layer, so a foundation below the floor would only be dug out round the site and left bare.
     */
    public static final int FLOOR = 0;
    /** The journal's pages, {@code book.hollowfrontier.symmes_journal.<n>} in the lang file. */
    public static final int JOURNAL_PAGES = 7;

    public static List<Blueprint> all() {
        return List.of(henge(), gate(), temple(), grayLab(), grayLabDeep(), grayStation(), symmesCamp(), grayPens(1), grayPens(2), grayPens(3));
    }

    /** The lab with the buried complex under it (26): the same lab, a hatch in the bunks' corner and a ladder down. */
    public static final String GRAY_LAB_DEEP = "gray_lab_deep";

    static Blueprint grayLabDeep() {
        Blueprint b = grayLab().restyled(GRAY_LAB_DEEP, state -> state);
        b.set(5, FLOOR + 1, 2, Blocks.AIR);
        b.set(5, FLOOR + 3, 2, Blocks.REDSTONE_LAMP); // a warning lamp over it
        DungeonBlueprints.shaftEntry(b, DungeonBlueprints.GRAY_COMPLEX, 5, FLOOR, 2, Direction.SOUTH);
        return b;
    }

    // ---------------------------------------------------------------- the Kindled sites

    /**
     * 31 x 31, centred on (15, 15): paved avenues out along the four axes to a gateway each (the west one fallen), a paved
     * ring walk, an inner ring of eight posts (soul lanterns on four), an outer ring of twelve great stones paired by
     * lintels into trilithons (two of them toppled inward), and in the middle a three-step dais with the obelisk: 3 x 3,
     * bands of lapis, a lodestone in each face, a carved cap and a spire with a soul fire burning on it, its smoke going
     * up high.
     */
    static Blueprint henge() {
        Blueprint b = new Blueprint(KINDLED_HENGE);
        Random r = new Random(1818);
        int c = 15, s = FLOOR;
        BlockState crystal = Masonry.crystalBricks(), chiseled = Masonry.chiseledCrystal(), starsteel = Masonry.starsteel();
        for (int x = 0; x <= 30; x++) {
            for (int z = 0; z <= 30; z++) {
                double d = Math.hypot(x - c, z - c);
                if (d > 15.4) {
                    continue;
                }
                b.air(x, s + 1, z, x, s + 20, z);
                boolean avenue = (Math.abs(x - c) <= 1 || Math.abs(z - c) <= 1) && d >= 8.5;
                boolean walk = d >= 9.6 && d <= 10.6;
                if (d <= 8.5) { // the ritual floor: a sun laid in the paving, rays of crystal out from the dais
                    double a = Math.toDegrees(Math.atan2(z - c, x - c)) + 360;
                    boolean ray = d > 4.4 && Math.abs(((a + 22.5) % 45) - 22.5) < 5.5;
                    b.set(x, s, z, ray ? crystal : d > 7.4 ? Blocks.POLISHED_DEEPSLATE.defaultBlockState() : Masonry.kindledBase(r));
                } else if ((avenue || walk) && r.nextInt(6) != 0) {
                    b.set(x, s, z, Masonry.kindledPaving(r));
                }
            }
        }
        // The seats round the ritual floor, a ring of steps facing in (open where the avenues come through)
        for (int x = 0; x <= 30; x++) {
            for (int z = 0; z <= 30; z++) {
                double d = Math.hypot(x - c, z - c);
                if (d <= 8.5 || d > 9.5 || Math.abs(x - c) <= 1 || Math.abs(z - c) <= 1) {
                    continue;
                }
                Direction out = Math.abs(x - c) > Math.abs(z - c) ? (x > c ? Direction.EAST : Direction.WEST) : (z > c ? Direction.SOUTH : Direction.NORTH);
                b.set(x, s + 1, z, r.nextInt(9) == 0 ? Blocks.AIR.defaultBlockState() : Masonry.stair(Blocks.TUFF_BRICK_STAIRS, out));
            }
        }
        // The dais, two steps up: deepslate, then crystal with starsteel at the quarters
        disc(b, c, c, 4.5, s + 1, Blocks.DEEPSLATE_TILES.defaultBlockState());
        disc(b, c, c, 3.2, s + 2, crystal);
        for (int[] q : new int[][] {{0, -3}, {3, 0}, {0, 3}, {-3, 0}}) {
            b.set(c + q[0], s + 2, c + q[1], starsteel);
        }
        // The obelisk: dark stone banded in crystal and starsteel, a lodestone in each face, a carved cap, a crystal spire
        // and a soul fire on it, its smoke going up high
        for (int y = s + 3; y <= s + 12; y++) {
            for (int dx = -1; dx <= 1; dx++) {
                for (int dz = -1; dz <= 1; dz++) {
                    boolean corner = dx != 0 && dz != 0, face = (dx == 0) != (dz == 0);
                    BlockState block = corner ? Blocks.POLISHED_DEEPSLATE.defaultBlockState() : Blocks.DEEPSLATE_TILES.defaultBlockState();
                    if (face && (y == s + 5 || y == s + 10)) {
                        block = ModBlocks.CONCAVIAN_CRYSTAL.get().defaultBlockState();
                    } else if (face && y == s + 7) {
                        block = Blocks.LODESTONE.defaultBlockState();
                    } else if (y == s + 12) {
                        block = starsteel;
                    }
                    b.set(c + dx, y, c + dz, block);
                }
            }
        }
        b.fill(c - 1, s + 13, c - 1, c + 1, s + 13, c + 1, Blocks.CHISELED_DEEPSLATE);
        b.set(c, s + 14, c, chiseled);
        b.fill(c, s + 15, c, c, s + 16, c, ModBlocks.CONCAVIAN_CRYSTAL.get());
        b.set(c, s + 17, c, Blocks.SOUL_CAMPFIRE.defaultBlockState().setValue(CampfireBlock.LIT, true).setValue(CampfireBlock.SIGNAL_FIRE, true));
        // The inner ring: eight posts of polished tuff at the floor's edge, soul lanterns on four
        for (int k = 0; k < 8; k++) {
            double a = k * Math.PI / 4 + Math.PI / 8;
            int x = c + (int) Math.round(Math.cos(a) * 6.8), z = c + (int) Math.round(Math.sin(a) * 6.8);
            int top = s + 3 + (k % 3 == 0 ? 1 : 0);
            b.fill(x, s + 1, z, x, top, z, Blocks.POLISHED_TUFF);
            b.set(x, top + 1, z, Blocks.CHISELED_TUFF);
            if (k % 2 == 0) {
                b.set(x, top + 2, z, Blocks.SOUL_LANTERN.defaultBlockState().setValue(LanternBlock.HANGING, false));
            }
        }
        // The outer ring: twelve great stones, 2 x 2 and eight high, on deepslate footings, paired by lintels into trilithons;
        // two have toppled. Each standing stone has a niche cut in its inner face, an offering pot or candles in it.
        int[][] stones = new int[12][];
        for (int i = 0; i < 12; i++) {
            double a = i * Math.PI / 6 + Math.PI / 12;
            int x = c + (int) Math.round(Math.cos(a) * 12.5), z = c + (int) Math.round(Math.sin(a) * 12.5);
            boolean fallen = i == 4 || i == 9;
            int height = fallen ? 3 : 8;
            for (int y = s + 1; y <= s + height; y++) {
                for (int[] p : new int[][] {{x, z}, {x + 1, z}, {x, z + 1}, {x + 1, z + 1}}) {
                    b.set(p[0], y, p[1], y == s + 1 ? Masonry.kindledBase(r) : r.nextInt(7) == 0 ? Blocks.TUFF.defaultBlockState()
                            : (y % 3 == 0 ? Blocks.POLISHED_TUFF : Blocks.TUFF_BRICKS).defaultBlockState());
                }
            }
            if (fallen) { // toppled inward, across the ring walk
                int ox = -(int) Math.round(Math.cos(a)), oz = -(int) Math.round(Math.sin(a));
                for (int step = 2; step <= 5; step++) {
                    b.set(x + ox * step, s + 1, z + oz * step, Blocks.TUFF_BRICKS);
                    b.set(x + 1 + ox * step, s + 1, z + 1 + oz * step, Blocks.TUFF_BRICKS);
                }
            } else {
                // The niche: the stone's block nearest the middle, cut back at shoulder height
                int nx = x + (x < c ? 1 : 0), nz = z + (z < c ? 1 : 0);
                if (i % 2 == 0) {
                    b.pot(nx, s + 3, nz, Direction.NORTH, ModChestLoot.KINDLED_OFFERING);
                } else {
                    b.set(nx, s + 3, nz, Blocks.CANDLE.defaultBlockState().setValue(CandleBlock.CANDLES, 3).setValue(CandleBlock.LIT, true));
                }
                b.set(nx, s + 4, nz, chiseled);
            }
            stones[i] = new int[] {x, z, fallen ? 1 : 0};
        }
        for (int i = 0; i < 12; i += 2) {
            int[] p = stones[i], q = stones[i + 1];
            if (p[2] == 0 && q[2] == 0) {
                line(b, p[0], p[1], q[0] + 1, q[1] + 1, s + 9, Blocks.DEEPSLATE_TILES.defaultBlockState());
                line(b, p[0] + 1, p[1] + 1, q[0], q[1], s + 9, Blocks.DEEPSLATE_TILES.defaultBlockState());
                b.set((p[0] + q[0] + 1) / 2, s + 10, (p[1] + q[1] + 1) / 2, chiseled); // the keystone
            }
        }
        // The gateways where the avenues meet the edge, the west one fallen, its starsteel sun disc lying in the grass
        for (int[] dir : new int[][] {{0, -1}, {1, 0}, {0, 1}, {-1, 0}}) {
            int px = c + dir[0] * 14, pz = c + dir[1] * 14, ax = dir[1], az = dir[0];
            boolean fallen = dir[0] == -1;
            for (int side : new int[] {-2, 2}) {
                int top = s + (fallen ? (side < 0 ? 2 : 4) : 6);
                b.set(px + ax * side, s + 1, pz + az * side, Blocks.POLISHED_DEEPSLATE);
                b.fill(px + ax * side, s + 2, pz + az * side, px + ax * side, top, pz + az * side, Blocks.TUFF_BRICKS);
            }
            if (fallen) {
                b.fill(px, s + 1, pz + 4, px + 2, s + 1, pz + 4, Blocks.TUFF_BRICKS);
                for (int dx = -1; dx <= 1; dx++) {
                    for (int dz = -1; dz <= 1; dz++) {
                        if (Math.abs(dx) + Math.abs(dz) < 2) {
                            b.set(px + 2 + dx, s + 1, pz - 4 + dz, dx == 0 && dz == 0 ? chiseled : starsteel);
                        }
                    }
                }
                continue;
            }
            for (int k = -2; k <= 2; k++) {
                BlockState block = Math.abs(k) == 2 ? Blocks.CHISELED_TUFF_BRICKS.defaultBlockState() : k == 0 ? starsteel : crystal;
                b.set(px + ax * k, s + 7, pz + az * k, block);
            }
        }
        // Weather: snow over it all, pale moss in the cracks, a little rubble by the fallen stones
        Masonry.rubble(b, r, c - 12, s + 1, c + 4, 3, 5, Blocks.TUFF_BRICKS.defaultBlockState(), Blocks.TUFF_BRICK_SLAB.defaultBlockState());
        Masonry.rubble(b, r, c + 5, s + 1, c - 11, 3, 5, Blocks.TUFF_BRICKS.defaultBlockState(), Blocks.TUFF_BRICK_STAIRS.defaultBlockState());
        Masonry.carpet(b, r, 0, 0, 30, 30, s + 1, Blocks.PALE_MOSS_CARPET, 0.05);
        Masonry.snow(b, r, 0, 0, 30, 30, 0.55);
        return b;
    }

    /**
     * 27 x 30, facing south (+z): a hill of stone turfed with pale moss and capped with snow, cut square at z 17 for a front
     * of tuff two thick on a deepslate course, stepping in twice to a carved crown 21 high, banded in crystal. Pilasters of
     * polished deepslate stand proud of it, relief panels either side of the door, a terrace before it with stairs down, a
     * soul fire at each corner and on two braziers by the stair, and a guardian on a plinth at each side (a robed figure with
     * a copper spear). The door (x 10..16, ten high) is a slab of iron with a starsteel sun on it, and no way to open it.
     * <p>
     * Behind it, in the hill ({@link #GATE_FLOOR} the floor): an antechamber with benches and offering pots; a hall of
     * columns lit from a ceiling of phlogiston lamps; the tomb, a sarcophagus under the mural of the world, a chest at its foot;
     * and either side of the hall an alcove: the vault (west) and an ossuary (east).
     */
    static Blueprint gate() {
        Blueprint b = new Blueprint(KINDLED_GATE);
        Random r = new Random(1823);
        int s = FLOOR, t = s + 3, f = GATE_FLOOR, cx = 13;
        BlockState crystal = Masonry.crystalBricks(), chiseled = Masonry.chiseledCrystal(), starsteel = Masonry.starsteel();
        b.air(0, s + 1, 0, 26, s + 22, 29);
        // The hill, behind the front
        for (int x = 0; x <= 26; x++) {
            for (int z = 0; z <= 16; z++) {
                int h = hill(x, z);
                if (h < 1) {
                    continue;
                }
                for (int y = s; y < s + h; y++) {
                    b.set(x, y, z, r.nextInt(12) == 0 ? Blocks.TUFF : r.nextInt(16) == 0 ? Blocks.ANDESITE : Blocks.STONE);
                }
                b.set(x, s + h, z, h > 12 ? Blocks.SNOW_BLOCK : r.nextInt(4) == 0 ? Blocks.COARSE_DIRT : Blocks.PALE_MOSS_BLOCK);
                if (h <= 12 && r.nextInt(5) == 0) {
                    b.set(x, s + h + 1, z, Blocks.PALE_MOSS_CARPET);
                }
            }
        }
        // The rooms in the hill: walls of Kindled stone round each, then hollowed out
        int[][] rooms = {
                {9, 14, 17, 16, 6},  // antechamber
                {7, 8, 19, 12, 8},   // the hall of columns
                {10, 3, 16, 6, 7},   // the tomb
                {3, 9, 5, 12, 5},    // the vault
                {21, 9, 23, 12, 5}}; // the ossuary
        for (int[] room : rooms) {
            Masonry.kindledFill(b, r, room[0] - 1, f, room[1] - 1, room[2] + 1, f + room[4] + 1, room[3] + 1);
        }
        for (int[] room : rooms) {
            b.air(room[0], f + 1, room[1], room[2], f + room[4], room[3]);
            for (int x = room[0]; x <= room[2]; x++) {
                for (int z = room[1]; z <= room[3]; z++) {
                    b.set(x, f, z, (x + z) % 2 == 0 ? Blocks.DEEPSLATE_TILES.defaultBlockState() : Blocks.POLISHED_DEEPSLATE.defaultBlockState());
                }
            }
        }
        // Doorways between them, framed in chiseled tuff
        doorway(b, 12, 14, f + 1, 13, 13, 4);       // antechamber - hall
        doorway(b, 12, 14, f + 1, 7, 7, 4);         // hall - tomb
        b.air(6, f + 1, 10, 6, f + 3, 11);          // hall - vault
        b.air(20, f + 1, 10, 20, f + 3, 11);        // hall - ossuary
        // The front: two thick (z 17..18) on a deepslate course, stepping in at 13 and 16 high, a carved crown at 20
        for (int y = s + 1; y <= s + 20; y++) {
            int half = y <= s + 13 ? 10 : y <= s + 16 ? 7 : 4;
            for (int x = cx - half; x <= cx + half; x++) {
                for (int z = 17; z <= 18; z++) {
                    b.set(x, y, z, y <= s + 3 ? Masonry.kindledBase(r) : y == s + 13 || y == s + 16 ? crystal : Masonry.kindledWall(r));
                }
            }
        }
        b.fill(cx - 4, s + 20, 18, cx + 4, s + 20, 18, Blocks.CHISELED_TUFF_BRICKS);
        b.set(cx, s + 20, 18, starsteel);
        b.set(cx - 4, s + 21, 18, Blocks.SOUL_LANTERN.defaultBlockState().setValue(LanternBlock.HANGING, false));
        b.set(cx + 4, s + 21, 18, Blocks.SOUL_LANTERN.defaultBlockState().setValue(LanternBlock.HANGING, false));
        b.set(cx, s + 21, 17, Blocks.SOUL_CAMPFIRE.defaultBlockState().setValue(CampfireBlock.LIT, true));
        for (int px : new int[] {4, 8, 18, 22}) {
            b.fill(px, t, 19, px, s + 12, 19, Blocks.POLISHED_DEEPSLATE);
            b.set(px, s + 13, 19, chiseled);
        }
        // The relief panels between the pilasters: a robed figure in chiseled tuff, a crystal eye, a sun over it
        for (int px : new int[] {6, 20}) {
            b.fill(px, t + 1, 18, px, t + 5, 18, Blocks.CHISELED_TUFF_BRICKS);
            b.set(px - 1, t + 4, 18, Blocks.CHISELED_TUFF_BRICKS);
            b.set(px + 1, t + 4, 18, Blocks.CHISELED_TUFF_BRICKS);
            b.set(px, t + 6, 18, chiseled);
            b.set(px, t + 8, 18, starsteel);
            b.set(px - 1, t + 8, 18, crystal);
            b.set(px + 1, t + 8, 18, crystal);
        }
        // The door: iron, set back a block, a starsteel sun on it and a dark seam down the middle
        b.air(10, t, 18, 16, t + 9, 18);
        b.fill(10, t, 17, 16, t + 9, 17, Blocks.IRON_BLOCK);
        b.fill(cx, t, 17, cx, t + 9, 17, Blocks.POLISHED_DEEPSLATE);
        for (int dx = -2; dx <= 2; dx++) {
            for (int dy = -2; dy <= 2; dy++) {
                int ring = Math.max(Math.abs(dx), Math.abs(dy));
                if (ring == 2 && Math.abs(dx) + Math.abs(dy) < 4) {
                    b.set(cx + dx, t + 5 + dy, 17, starsteel);
                } else if (ring == 1 && Math.abs(dx) + Math.abs(dy) == 1) {
                    b.set(cx + dx, t + 5 + dy, 17, Blocks.HONEYCOMB_BLOCK);
                }
            }
        }
        b.set(cx, t + 5, 17, Blocks.GLOWSTONE);
        b.fill(9, t, 18, 9, t + 10, 18, chiseled);
        b.fill(17, t, 18, 17, t + 10, 18, chiseled);
        b.fill(9, t + 10, 18, 17, t + 10, 18, chiseled);
        for (int x = 6; x <= 20; x++) {
            b.set(x, s + 14, 18, x % 2 == 0 ? starsteel : Blocks.CHISELED_TUFF_BRICKS.defaultBlockState());
        }
        // The terrace, and the stairs down from it
        for (int x = 2; x <= 24; x++) {
            for (int z = 19; z <= 26; z++) {
                b.set(x, s + 1, z, Masonry.kindledBase(r));
                b.set(x, s + 2, z, x == 2 || x == 24 || z == 26 ? Blocks.POLISHED_TUFF.defaultBlockState() : Masonry.kindledPaving(r));
            }
        }
        for (int x = 8; x <= 18; x++) {
            b.set(x, s + 2, 27, Masonry.stair(Blocks.DEEPSLATE_TILE_STAIRS, Direction.NORTH));
            b.set(x, s + 1, 27, Blocks.DEEPSLATE_TILES);
            b.set(x, s + 1, 28, Masonry.stair(Blocks.DEEPSLATE_TILE_STAIRS, Direction.NORTH));
            b.set(x, s, 29, Masonry.stair(Blocks.DEEPSLATE_TILE_STAIRS, Direction.NORTH));
        }
        // Guardians, fires on the terrace's front corners and braziers either side of the stair
        guardian(b, 5, t, 22, -1);
        guardian(b, 21, t, 22, 1);
        b.set(3, t, 25, Blocks.SOUL_CAMPFIRE.defaultBlockState().setValue(CampfireBlock.LIT, true));
        b.set(23, t, 25, Blocks.SOUL_CAMPFIRE.defaultBlockState().setValue(CampfireBlock.LIT, true));
        for (int bx : new int[] {7, 19}) {
            b.set(bx, t, 26, Blocks.POLISHED_DEEPSLATE_WALL);
            b.set(bx, t + 1, 26, Blocks.SOUL_CAMPFIRE.defaultBlockState().setValue(CampfireBlock.LIT, true));
        }
        BlockState soulLantern = Blocks.SOUL_LANTERN.defaultBlockState().setValue(LanternBlock.HANGING, false);
        // The antechamber (x 9..17, z 14..16): benches along the walls, offering pots, lanterns
        for (int x : new int[] {9, 10, 16, 17}) {
            b.set(x, f + 1, 14, Masonry.stair(Blocks.TUFF_BRICK_STAIRS, Direction.NORTH));
        }
        b.pot(9, f + 1, 16, Direction.EAST, ModChestLoot.KINDLED_OFFERING);
        b.pot(17, f + 1, 16, Direction.WEST, ModChestLoot.KINDLED_OFFERING);
        b.set(11, f + 1, 16, soulLantern);
        b.set(15, f + 1, 16, soulLantern);
        // The hall of columns (x 7..19, z 8..12): two rows, dark bases and crystal capitals; phlogiston lamps in the ceiling
        for (int x : new int[] {9, 17}) {
            for (int z : new int[] {9, 11}) {
                b.set(x, f + 1, z, Blocks.POLISHED_DEEPSLATE);
                b.fill(x, f + 2, z, x, f + 7, z, Blocks.QUARTZ_PILLAR);
                b.set(x, f + 8, z, chiseled);
            }
        }
        for (int x = 8; x <= 18; x += 5) {
            b.set(x, f + 9, 10, Masonry.phlogistonLamp());
        }
        b.set(cx, f + 9, 8, Masonry.phlogistonLamp());
        b.set(cx, f + 9, 12, Masonry.phlogistonLamp());
        b.fill(11, f, 8, 15, f, 12, crystal); // a runner of crystal up the middle
        // The tomb (x 10..16, z 3..6): the sarcophagus under the mural, candles at its corners, a chest at its foot
        b.fill(12, f + 1, 4, 14, f + 1, 5, Blocks.CHISELED_DEEPSLATE);
        b.fill(12, f + 2, 4, 14, f + 2, 5, ModBlocks.CRYSTAL_BRICK_SLAB.get().defaultBlockState().setValue(SlabBlock.TYPE, SlabType.BOTTOM));
        b.set(cx, f + 2, 4, Blocks.SKELETON_SKULL);
        for (int[] p : new int[][] {{11, 3}, {15, 3}, {11, 6}, {15, 6}}) {
            b.set(p[0], f + 1, p[1], Blocks.CANDLE.defaultBlockState().setValue(CandleBlock.CANDLES, 2).setValue(CandleBlock.LIT, true));
        }
        b.chest(15, f + 1, 4, Direction.WEST, ModChestLoot.KINDLED_TOMB);
        b.set(11, f + 8, 5, Masonry.phlogistonLamp());
        b.set(15, f + 8, 5, Masonry.phlogistonLamp());
        muralOn(b, new String[] {".RR.RR.", "R.....R", "R..H..R", "R.HSH.R", "R..H..R", "R.....R", ".RR.RR."}, 10, f + 7, 2);
        // Behind the sarcophagus, under the mural, a ladder down through the floor to the catacombs (26)
        b.fill(cx, s + 1, 3, cx, f, 3, Blocks.LADDER.defaultBlockState().setValue(LadderBlock.FACING, Direction.SOUTH));
        DungeonBlueprints.shaftEntry(b, DungeonBlueprints.CATACOMBS, cx, s, 3, Direction.SOUTH);
        // The vault (west): the vault chest on a starsteel plinth, pots, a lantern
        b.set(3, f + 1, 10, starsteel);
        b.chest(3, f + 2, 10, Direction.EAST, ModChestLoot.KINDLED_VAULT);
        b.pot(3, f + 1, 12, Direction.EAST, ModChestLoot.KINDLED_OFFERING);
        b.pot(3, f + 1, 9, Direction.EAST, null);
        b.set(5, f + 1, 9, soulLantern);
        // The ossuary (east): bones stacked in the walls, skulls in rows, candles
        for (int z = 9; z <= 12; z++) {
            for (int y = f + 1; y <= f + 4; y++) {
                b.set(24, y, z, (y + z) % 2 == 0 ? Blocks.BONE_BLOCK : Blocks.SKELETON_SKULL);
            }
        }
        b.fill(21, f + 1, 12, 23, f + 1, 12, Blocks.BONE_BLOCK);
        b.set(22, f + 2, 12, Blocks.CANDLE.defaultBlockState().setValue(CandleBlock.CANDLES, 4).setValue(CandleBlock.LIT, true));
        b.set(21, f + 1, 9, Blocks.SKELETON_SKULL);
        // Cobwebs high in the corners
        for (int[] p : new int[][] {{7, 8}, {19, 8}, {7, 12}, {19, 12}, {10, 3}, {16, 3}}) {
            if (r.nextBoolean()) {
                b.set(p[0], f + 7, p[1], Blocks.COBWEB);
            }
        }
        Masonry.snow(b, r, 0, 17, 26, 29, 0.5);
        return b;
    }

    /** The gate's inside floor: the terrace's level, so the rooms run on from it behind the door. */
    public static final int GATE_FLOOR = 2;

    /** The gate's hill: how high (over its floor) it rises at (x, z), 0 off it. */
    private static int hill(int x, int z) {
        double e = sq((x - 13) / 13.5) + sq((z - 9) / 10.5);
        return e >= 1 ? 0 : (int) Math.round(Math.sqrt(1 - e) * 17);
    }

    /** A doorway {@code height} high through the wall at z {@code z0..z1}, from x {@code x0} to {@code x1}, chiseled tuff over it. */
    private static void doorway(Blueprint b, int x0, int x1, int y, int z0, int z1, int height) {
        b.air(x0, y, z0, x1, y + height - 1, z1);
        b.fill(x0 - 1, y + height, z0, x1 + 1, y + height, z1, Blocks.CHISELED_TUFF_BRICKS);
    }

    /** A robed guardian 14 high on a plinth, facing south, a copper spear on its {@code side} (-1 west, 1 east). */
    private static void guardian(Blueprint b, int x, int y, int z, int side) {
        b.fill(x - 2, y, z - 1, x + 2, y + 1, z + 1, Blocks.POLISHED_DEEPSLATE);
        b.fill(x - 1, y + 2, z - 1, x + 1, y + 5, z, Blocks.POLISHED_TUFF); // the robe
        b.fill(x - 1, y + 6, z - 1, x + 1, y + 8, z, Blocks.TUFF_BRICKS); // the body
        b.fill(x - 1, y + 9, z - 1, x + 1, y + 9, z, Blocks.POLISHED_TUFF); // shoulders
        b.fill(x - 2, y + 6, z, x - 2, y + 8, z, Blocks.POLISHED_TUFF); // arms
        b.fill(x + 2, y + 6, z, x + 2, y + 8, z, Blocks.POLISHED_TUFF);
        b.set(x, y + 3, z + 1, Masonry.starsteel()); // the belt's clasp
        b.set(x, y + 10, z, ModBlocks.CHISELED_CRYSTAL_BRICKS.get()); // the face
        b.set(x, y + 10, z - 1, Blocks.POLISHED_TUFF);
        b.fill(x, y + 11, z - 1, x, y + 11, z, Masonry.starsteel()); // the helm
        b.fill(x + 2 * side, y + 2, z + 1, x + 2 * side, y + 12, z + 1, Blocks.COPPER_BARS.waxed().oxidized()); // the spear
        b.set(x + 2 * side, y + 13, z + 1, Blocks.LIGHTNING_ROD.waxed().oxidized());
    }

    /**
     * 25 x 33, facing south (+z): a platform two steps high on a deepslate course, stairs at the front, a colonnade round it
     * (quartz columns on dark bases under crystal capitals, a crystal entablature over each stretch where both columns
     * stand; about one in five broken off, its drums lying by it). Inside, the chamber (x 6..18, z 8..25) of tuff, a band of
     * crystal windows down its sides, open to the sky where its roof fell in, the mural (11 x 7) set into its back wall, an
     * altar with soul lanterns, two soul fires by its door. Behind it, roofed and between the colonnade and the chamber's
     * back wall: the scriptorium (west: lecterns, shelves of books, a chest) and the vestry (east: robes on stands, pots).
     */
    static Blueprint temple() {
        Blueprint b = new Blueprint(KINDLED_TEMPLE);
        Random r = new Random(1826);
        int s = FLOOR, t = s + 3;
        BlockState crystal = Masonry.crystalBricks(), chiseled = Masonry.chiseledCrystal(), starsteel = Masonry.starsteel();
        b.air(0, s + 1, 0, 24, s + 14, 32);
        for (int x = 0; x <= 24; x++) {
            for (int z = 0; z <= 32; z++) {
                b.set(x, s + 1, z, Masonry.kindledBase(r));
            }
        }
        for (int x = 1; x <= 23; x++) {
            for (int z = 1; z <= 31; z++) {
                boolean axis = x == 12 && z > 25;
                b.set(x, s + 2, z, axis ? crystal : r.nextInt(16) == 0 ? Blocks.PALE_MOSS_BLOCK.defaultBlockState() : Masonry.kindledPaving(r));
            }
        }
        for (int x = 8; x <= 16; x++) {
            b.set(x, s + 2, 32, Masonry.stair(Blocks.DEEPSLATE_TILE_STAIRS, Direction.NORTH));
        }
        // The colonnade
        List<int[]> columns = new ArrayList<>();
        for (int z = 2; z <= 29; z += 3) {
            columns.add(new int[] {2, z});
            columns.add(new int[] {22, z});
        }
        for (int x = 6; x <= 18; x += 4) {
            columns.add(new int[] {x, 2});
            columns.add(new int[] {x, 29});
        }
        Set<Long> placed = new HashSet<>(), standing = new HashSet<>();
        for (int[] col : columns) {
            int x = col[0], z = col[1];
            placed.add(key(x, z));
            b.set(x, t, z, Blocks.POLISHED_DEEPSLATE);
            if (r.nextInt(5) == 0) {
                b.fill(x, t + 1, z, x, t + 1 + r.nextInt(4), z, Blocks.QUARTZ_PILLAR);
                // Its fallen drums, lying on the walk inside the colonnade
                Direction.Axis axis = x == 2 || x == 22 ? Direction.Axis.Z : Direction.Axis.X;
                int ox = axis == Direction.Axis.X ? 0 : (x == 2 ? 2 : -2), oz = axis == Direction.Axis.Z ? 0 : (z == 2 ? 2 : -2);
                int along = z >= 27 || x >= 16 ? -1 : 1;
                for (int k = 0; k < 3; k++) {
                    int fx = x + ox + (axis == Direction.Axis.X ? k * along : 0), fz = z + oz + (axis == Direction.Axis.Z ? k * along : 0);
                    b.set(fx, t, fz, Blocks.QUARTZ_PILLAR.defaultBlockState().setValue(RotatedPillarBlock.AXIS, axis));
                    placed.add(key(fx, fz));
                }
            } else {
                b.fill(x, t + 1, z, x, t + 7, z, Blocks.QUARTZ_PILLAR);
                b.set(x, t + 8, z, chiseled);
                standing.add(key(x, z));
            }
        }
        for (int x : new int[] {2, 22}) {
            for (int z = 2; z + 3 <= 29; z += 3) {
                if (standing.contains(key(x, z)) && standing.contains(key(x, z + 3))) {
                    b.fill(x, t + 9, z, x, t + 9, z + 3, crystal);
                }
            }
        }
        for (int z : new int[] {2, 29}) {
            for (int x = 2; x < 22; ) {
                int next = x == 2 ? 6 : x == 18 ? 22 : x + 4;
                if (standing.contains(key(x, z)) && standing.contains(key(next, z))) {
                    b.fill(x, t + 9, z, next, t + 9, z, crystal);
                }
                x = next;
            }
        }
        // The chamber: tuff walls on a deepslate course, a band of crystal windows, its roof mostly fallen in
        for (int y = t; y <= t + 8; y++) {
            for (int x = 6; x <= 18; x++) {
                for (int z = 8; z <= 25; z++) {
                    if (x == 6 || x == 18 || z == 8 || z == 25) {
                        b.set(x, y, z, y == t ? Masonry.kindledBase(r) : y == t + 8 ? Blocks.CHISELED_TUFF_BRICKS.defaultBlockState() : Masonry.kindledWall(r));
                    }
                }
            }
        }
        for (int z = 11; z <= 22; z += 3) {
            for (int x : new int[] {6, 18}) {
                b.fill(x, t + 4, z, x, t + 5, z, ModBlocks.CONCAVIAN_CRYSTAL.get());
            }
        }
        b.air(11, t, 25, 13, t + 5, 25);
        b.fill(10, t + 6, 25, 14, t + 6, 25, chiseled);
        b.set(12, t + 7, 25, starsteel);
        for (int x = 6; x <= 18; x++) {
            for (int z = 8; z <= 25; z++) {
                boolean hole = x >= 9 && x <= 16 && z >= 12 && z <= 22;
                if (!hole && r.nextInt(8) != 0) {
                    b.set(x, t + 9, z, Masonry.kindledWall(r));
                }
            }
        }
        muralOn(b, new String[] {"...RR.RR...", "..R.....R..", ".R...H...R.", ".R..HSH..R.", ".R...H...R.", "..R.....R..", "...RR.RR..."},
                7, t + 7, 8);
        // The altar under the open roof, soul lanterns on it (a fire's smoke would hide the mural); the fires by the door
        b.fill(11, t, 17, 13, t, 17, Blocks.POLISHED_DEEPSLATE);
        b.fill(11, t + 1, 17, 13, t + 1, 17, chiseled);
        b.set(12, t + 1, 17, starsteel);
        b.set(11, t + 2, 17, Blocks.SOUL_LANTERN.defaultBlockState().setValue(LanternBlock.HANGING, false));
        b.set(13, t + 2, 17, Blocks.SOUL_LANTERN.defaultBlockState().setValue(LanternBlock.HANGING, false));
        b.set(8, t, 22, Blocks.SOUL_CAMPFIRE.defaultBlockState().setValue(CampfireBlock.LIT, true));
        b.set(16, t, 22, Blocks.SOUL_CAMPFIRE.defaultBlockState().setValue(CampfireBlock.LIT, true));
        // Benches down the chamber
        for (int z = 13; z <= 21; z += 4) {
            b.fill(8, t, z, 9, t, z, Masonry.stair(Blocks.TUFF_BRICK_STAIRS, Direction.SOUTH));
            b.fill(15, t, z, 16, t, z, Masonry.stair(Blocks.TUFF_BRICK_STAIRS, Direction.SOUTH));
        }
        // The back rooms: walls between the colonnade's back row and the chamber, a roof, a door from each side walk
        for (int y = t; y <= t + 5; y++) {
            for (int x = 6; x <= 18; x++) {
                for (int z = 3; z <= 7; z++) {
                    if (x == 6 || x == 18 || z == 3 || x == 12) {
                        b.set(x, y, z, y == t ? Masonry.kindledBase(r) : Masonry.kindledWall(r));
                    } else {
                        b.set(x, y, z, Blocks.AIR);
                    }
                }
            }
        }
        b.fill(6, t + 6, 3, 18, t + 6, 7, Blocks.DEEPSLATE_TILE_SLAB.defaultBlockState().setValue(SlabBlock.TYPE, SlabType.BOTTOM));
        for (int x = 6; x <= 18; x++) {
            for (int z = 3; z <= 7; z++) {
                if (x != 6 && x != 18 && z != 3) {
                    b.set(x, t - 1, z, (x + z) % 2 == 0 ? Blocks.DEEPSLATE_TILES : Blocks.POLISHED_TUFF);
                }
            }
        }
        b.air(6, t + 1, 5, 6, t + 2, 6);
        b.air(18, t + 1, 5, 18, t + 2, 6);
        b.set(9, t + 5, 5, Masonry.phlogistonLamp());
        b.set(15, t + 5, 5, Masonry.phlogistonLamp());
        // The scriptorium (west): lecterns, shelves of books under a band of crystal, the chest
        b.set(8, t, 6, Blocks.LECTERN.defaultBlockState().setValue(LecternBlock.FACING, Direction.NORTH));
        b.set(10, t, 6, Blocks.LECTERN.defaultBlockState().setValue(LecternBlock.FACING, Direction.NORTH));
        b.fill(7, t, 4, 11, t + 1, 4, Blocks.CHISELED_BOOKSHELF.defaultBlockState().setValue(BlockStateProperties.HORIZONTAL_FACING, Direction.SOUTH));
        b.set(9, t, 4, Blocks.BOOKSHELF);
        b.fill(7, t + 2, 4, 11, t + 2, 4, ModBlocks.CONCAVIAN_CRYSTAL.get());
        b.chest(11, t, 7, Direction.WEST, ModChestLoot.KINDLED_SCRIPTORIUM);
        b.set(7, t, 7, Blocks.CANDLE.defaultBlockState().setValue(CandleBlock.CANDLES, 3).setValue(CandleBlock.LIT, true));
        // The vestry (east): robes on their stands, offering pots, a chest of vestments
        b.armorStand(14, t, 4, 0, Items.GOLDEN_HELMET, Items.LEATHER_CHESTPLATE, Items.LEATHER_LEGGINGS, null);
        b.armorStand(16, t, 4, 0, null, Items.LEATHER_CHESTPLATE, null, null);
        b.pot(17, t, 7, Direction.WEST, ModChestLoot.KINDLED_OFFERING);
        b.pot(13, t, 7, Direction.EAST, ModChestLoot.KINDLED_OFFERING);
        b.chest(17, t, 4, Direction.WEST, ModChestLoot.KINDLED_SCRIPTORIUM);
        // Moss, snow and rubble on the walks and in the open chamber
        for (int i = 0; i < 14; i++) {
            int x = 1 + r.nextInt(23), z = 1 + r.nextInt(31);
            boolean inside = x > 6 && x < 18 && z > 8 && z < 25, wall = x >= 6 && x <= 18 && z >= 3 && z <= 25 && !inside;
            if (!placed.contains(key(x, z)) && !wall && !b.has(x, t, z)) {
                b.set(x, t, z, r.nextInt(3) == 0 ? Blocks.TUFF_BRICK_SLAB.defaultBlockState() : Blocks.PALE_MOSS_CARPET.defaultBlockState());
            }
        }
        Masonry.snow(b, r, 0, 0, 24, 32, 0.5);
        return b;
    }

    /** A mural: rows from the top (at y {@code top}) down, starting at x {@code x0}, in the wall at z {@code z}. */
    private static void muralOn(Blueprint b, String[] rows, int x0, int top, int z) {
        for (int row = 0; row < rows.length; row++) {
            for (int i = 0; i < rows[row].length(); i++) {
                Block block = switch (rows[row].charAt(i)) {
                    case 'R' -> Blocks.LAPIS_BLOCK;
                    case 'S' -> Blocks.GLOWSTONE;
                    case 'H' -> Blocks.HONEYCOMB_BLOCK;
                    default -> null;
                };
                if (block != null) {
                    b.set(x0 + i, top - row, z, block);
                }
            }
        }
    }

    private static BlockState paving(Random r) {
        return switch (r.nextInt(6)) {
            case 0, 1, 2 -> Blocks.CALCITE.defaultBlockState();
            case 3, 4 -> Blocks.POLISHED_DIORITE.defaultBlockState();
            default -> Blocks.DIORITE.defaultBlockState();
        };
    }

    // ---------------------------------------------------------------- the Gray field lab

    /**
     * 37 x 37. The dome (centre (11, 24), radius 9, eleven high): concrete walls with a band of tinted windows, a shell
     * of smooth quartz with holes in it and a glass eye at the top; inside, four restraint tables in a cross under a
     * hanging ring of lamps, chains down from the ring onto each table and a skull left on one; four specimen tanks round
     * the wall (glowing pickles, a brain coral, kelp, one cracked and dry); a bench with a cauldron, a brewing stand and a
     * chest; a blown-open doorway east. The landing pad (centre (27, 10), radius 7): grey, rimmed white, lights round it
     * and a black scorch ring. Walkways join them to the cage block (x 23..35, z 27..33: three cells, the tagged hare in
     * one, bones in the next, the last broken open) and the mast (x 33, z 20, a dish 17 up). One probe hangs over the
     * pad, another over the dome.
     */
    static Blueprint grayLab() {
        Blueprint b = new Blueprint(GRAY_LAB);
        Random r = new Random(1947);
        int s = FLOOR, dcx = 11, dcz = 24;
        b.air(0, s + 1, 0, 36, s + 22, 36);
        // The dome
        for (int x = 1; x <= 21; x++) {
            for (int z = 14; z <= 34; z++) {
                double d = Math.hypot(x - dcx, z - dcz);
                if (d > 9.5) {
                    continue;
                }
                b.set(x, s, z, ((int) d) % 3 == 0 ? Blocks.CONCRETE.lightGray() : Blocks.CONCRETE.white());
                for (int y = s + 1; y <= s + 11; y++) {
                    double e = y <= s + 4 ? d : Math.sqrt(d * d + sq((y - s - 4) * 9.0 / 7.0));
                    if (e <= 8.4 || e > 9.5) {
                        continue;
                    }
                    if (y == s + 3) {
                        b.set(x, y, z, Blocks.TINTED_GLASS);
                    } else if (y <= s + 4) {
                        b.set(x, y, z, Blocks.CONCRETE.white());
                    } else if (d <= 2.2) {
                        b.set(x, y, z, Blocks.GLASS);
                    } else if (r.nextInt(14) != 0) {
                        b.set(x, y, z, Blocks.SMOOTH_QUARTZ);
                    }
                }
            }
        }
        // The doorway, blown open, on the east side
        b.fill(19, s + 1, 22, 21, s + 4, 26, Blocks.IRON_BLOCK);
        b.air(19, s + 1, 23, 21, s + 3, 25);
        b.set(18, s + 1, 22, Blocks.COBWEB);
        // The tables in a cross round the middle, chains down onto them from the lamp ring
        BlockState chain = Blocks.IRON_CHAIN.defaultBlockState().setValue(ChainBlock.AXIS, Direction.Axis.Y);
        for (int[] dir : new int[][] {{0, -1}, {0, 1}, {-1, 0}, {1, 0}}) {
            for (int k = 3; k <= 5; k++) {
                b.set(dcx + dir[0] * k, s + 1, dcz + dir[1] * k, Blocks.SMOOTH_QUARTZ);
            }
            b.fill(dcx + dir[0] * 4, s + 2, dcz + dir[1] * 4, dcx + dir[0] * 4, s + 5, dcz + dir[1] * 4, chain);
            b.fill(dcx + dir[0] * 4, s + 7, dcz + dir[1] * 4, dcx + dir[0] * 4, s + 9, dcz + dir[1] * 4, chain);
        }
        b.set(dcx, s + 2, dcz + 3, Blocks.SKELETON_SKULL);
        for (int x = dcx - 5; x <= dcx + 5; x++) {
            for (int z = dcz - 5; z <= dcz + 5; z++) {
                double d = Math.hypot(x - dcx, z - dcz);
                if (d > 3.5 && d <= 4.5) {
                    b.set(x, s + 6, z, x == dcx || z == dcz ? Blocks.SEA_LANTERN : Blocks.CONCRETE.lightGray());
                }
            }
        }
        // Specimen tanks round the wall
        int[][] tanks = {{dcx + 3, dcz + 6}, {dcx - 3, dcz + 6}, {dcx - 3, dcz - 6}, {dcx + 3, dcz - 6}};
        for (int i = 0; i < tanks.length; i++) {
            int tx = tanks[i][0], tz = tanks[i][1];
            b.fill(tx - 1, s, tz - 1, tx + 1, s, tz + 1, Blocks.IRON_BLOCK);
            b.fill(tx - 1, s + 1, tz - 1, tx + 1, s + 3, tz + 1, Blocks.GLASS);
            b.fill(tx - 1, s + 4, tz - 1, tx + 1, s + 4, tz + 1, Blocks.IRON_BLOCK);
            b.fill(tx, s + 1, tz, tx, s + 3, tz, Blocks.WATER);
            switch (i) {
                case 0 -> b.set(tx, s + 1, tz, Blocks.SEA_PICKLE.defaultBlockState().setValue(SeaPickleBlock.PICKLES, 4).setValue(SeaPickleBlock.WATERLOGGED, true));
                case 1 -> b.set(tx, s + 1, tz, Blocks.BRAIN_CORAL_BLOCK);
                case 2 -> {
                    b.set(tx, s + 1, tz, Blocks.KELP_PLANT);
                    b.set(tx, s + 2, tz, Blocks.KELP);
                }
                default -> { // cracked and dry
                    b.fill(tx, s + 1, tz, tx, s + 3, tz, Blocks.AIR);
                    b.set(tx, s + 1, tz, Blocks.COBWEB);
                    b.air(tx - 1, s + 2, tz + 1, tx, s + 3, tz + 1);
                }
            }
        }
        // The bench on the west side
        b.set(dcx - 7, s + 1, dcz - 1, Blocks.CAULDRON);
        b.set(dcx - 7, s + 1, dcz, Blocks.BREWING_STAND);
        b.chest(dcx - 7, s + 1, dcz + 1, Direction.EAST, ModChestLoot.GRAY_LAB);
        for (int i = 0; i < 6; i++) {
            b.set(dcx - 6 + r.nextInt(13), s + 7 + r.nextInt(2), dcz - 6 + r.nextInt(13), Blocks.COBWEB);
        }
        // The landing pad
        int pcx = 27, pcz = 10;
        for (int x = pcx - 8; x <= pcx + 8; x++) {
            for (int z = pcz - 8; z <= pcz + 8; z++) {
                double d = Math.hypot(x - pcx, z - pcz);
                if (d > 7.4) {
                    continue;
                }
                Block floor = d > 6.5 ? Blocks.CONCRETE.white() : d > 2.0 && d <= 3.2 ? Blocks.CONCRETE.black() : d <= 1.0 ? Blocks.IRON_BLOCK : Blocks.CONCRETE.lightGray();
                b.set(x, s, z, floor);
            }
        }
        for (int k = 0; k < 8; k++) {
            double a = k * Math.PI / 4;
            b.set(pcx + (int) Math.round(Math.cos(a) * 6), s, pcz + (int) Math.round(Math.sin(a) * 6), Blocks.SEA_LANTERN);
        }
        // Walkways: dome door to the cages, pad down to the walkway
        b.fill(22, s, 23, 34, s, 25, Blocks.SMOOTH_STONE);
        b.fill(26, s, 17, 28, s, 22, Blocks.SMOOTH_STONE);
        // The cage block: three cells of iron bars under a concrete roof, the last one broken open
        for (int x = 23; x <= 35; x++) {
            for (int z = 27; z <= 33; z++) {
                b.set(x, s, z, Blocks.COARSE_DIRT);
                boolean wall = x == 23 || x == 27 || x == 31 || x == 35 || z == 27 || z == 33;
                boolean broken = z == 27 && x >= 32 && x <= 34;
                if (wall && !broken) {
                    b.fill(x, s + 1, z, x, s + 3, z, Blocks.IRON_BARS);
                }
            }
        }
        b.fill(23, s + 4, 27, 35, s + 4, 33, Blocks.CONCRETE.lightGray());
        b.set(24, s + 1, 32, Blocks.HAY_BLOCK);
        b.set(26, s + 1, 32, Blocks.CAULDRON);
        b.set(29, s + 1, 31, Blocks.BONE_BLOCK);
        b.set(28, s + 1, 30, Blocks.SKELETON_SKULL);
        b.set(30, s + 3, 28, Blocks.COBWEB);
        b.set(33, s + 1, 30, Blocks.DEAD_BUSH);
        // The mast and its dish
        b.fill(32, s, 19, 34, s, 21, Blocks.IRON_BLOCK);
        b.fill(33, s + 1, 20, 33, s + 16, 20, Blocks.IRON_BARS);
        for (int y : new int[] {s + 6, s + 11}) {
            b.fill(32, y, 20, 34, y, 20, Blocks.IRON_BARS);
            b.fill(33, y, 19, 33, y, 21, Blocks.IRON_BARS);
        }
        BlockState slab = Blocks.SMOOTH_QUARTZ_SLAB.defaultBlockState().setValue(SlabBlock.TYPE, SlabType.BOTTOM);
        for (int dx = -2; dx <= 2; dx++) {
            for (int dz = -2; dz <= 2; dz++) {
                if (Math.abs(dx) + Math.abs(dz) < 4) {
                    b.set(33 + dx, s + 17, 20 + dz, Math.abs(dx) + Math.abs(dz) <= 1 ? Blocks.SMOOTH_QUARTZ.defaultBlockState() : slab);
                }
            }
        }
        b.set(33, s + 17, 20, Blocks.SEA_LANTERN);
        b.set(33, s + 18, 20, Blocks.LIGHTNING_ROD.waxed().weathered());
        labAnnex(b, r, s);
        // Gone to seed: vines down the walls, moss creeping over the concrete, webs inside
        Masonry.vines(b, r, 0, s + 1, 0, 36, s + 8, 36, 0.05);
        Masonry.carpet(b, r, 0, 0, 36, 36, s + 1, Blocks.MOSS_CARPET, 0.04);
        // The hare in its cell, and the watchers
        CompoundTag tagged = new CompoundTag();
        tagged.putBoolean("EarTag", true);
        b.entity(ModEntities.GLASSBACK_HARE.get(), 25, s + 1, 30, tagged);
        b.entity(ModEntities.GRAY_PROBE.get(), pcx, s + 8, pcz);
        b.entity(ModEntities.GRAY_PROBE.get(), dcx + 4, s + 15, dcz - 6);
        return b;
    }

    /**
     * The lab's annex (25), north of the dome (x 1..17, z 1..12): a low white block, a band of dark windows, a flat roof
     * fallen in over the bunks. A corridor (z 6..7) runs from the bunks (west: sleeping pods of glass, lit from below) past
     * the control room (north: consoles under dead screens, a map of the Hollow Earth set in the floor, the control chest)
     * to the decon airlock (south-east: copper grates over and under, its outer door blown in). The specimen store
     * (south: shelves of jars, barrels, a cauldron, the store's chest) opens on a passage down into the dome.
     */
    private static void labAnnex(Blueprint b, Random r, int s) {
        // The shell: floor, white walls with a band of tinted glass, a quartz roof with holes over the bunks
        b.fill(1, s, 1, 17, s, 12, Blocks.CONCRETE.lightGray());
        b.walls(1, s + 1, 1, 17, s + 4, 12, Blocks.CONCRETE.white().defaultBlockState());
        for (int x = 2; x <= 16; x += 2) {
            b.set(x, s + 3, 1, Blocks.TINTED_GLASS);
            b.set(x, s + 3, 12, Blocks.TINTED_GLASS);
        }
        for (int z = 3; z <= 10; z += 2) {
            b.set(1, s + 3, z, Blocks.TINTED_GLASS);
            b.set(17, s + 3, z, Blocks.TINTED_GLASS);
        }
        for (int x = 1; x <= 17; x++) {
            for (int z = 1; z <= 12; z++) {
                boolean fallen = x <= 6 && r.nextInt(3) == 0;
                b.set(x, s + 5, z, fallen ? Blocks.AIR : (x + z) % 7 == 0 ? Blocks.SEA_LANTERN : Blocks.SMOOTH_QUARTZ);
            }
        }
        // The walls inside: bunks | corridor | control (north), store and airlock (south)
        b.fill(6, s + 1, 2, 6, s + 4, 11, Blocks.CONCRETE.white());   // bunks' wall
        b.fill(7, s + 1, 5, 16, s + 4, 5, Blocks.CONCRETE.white());   // control's wall
        b.fill(7, s + 1, 8, 16, s + 4, 8, Blocks.CONCRETE.white());   // store and airlock's wall
        b.fill(13, s + 1, 9, 13, s + 4, 11, Blocks.CONCRETE.white()); // between them
        b.air(6, s + 1, 6, 6, s + 3, 7);
        b.air(7, s + 1, 6, 16, s + 4, 7);
        b.air(10, s + 1, 5, 11, s + 3, 5);
        b.air(9, s + 1, 8, 10, s + 3, 8);
        b.air(15, s + 1, 8, 15, s + 3, 8);
        for (int x = 7; x <= 16; x += 3) {
            b.set(x, s + 4, 6, Blocks.END_ROD.defaultBlockState().setValue(EndRodBlock.FACING, Direction.DOWN));
        }
        // The bunks: three pods of glass along the west wall, each on a lit plinth, one broken open
        for (int i = 0; i < 3; i++) {
            int z = 3 + i * 3;
            b.set(2, s + 1, z, Blocks.SEA_LANTERN);
            b.set(3, s + 1, z, Blocks.CONCRETE.white());
            b.fill(2, s + 2, z, 3, s + 2, z, Blocks.CARPET.white());
            if (i == 1) {
                b.set(4, s + 1, z, Blocks.GLASS_PANE);
                b.set(4, s + 2, z, Blocks.COBWEB);
            } else {
                b.fill(4, s + 1, z, 4, s + 3, z, Blocks.GLASS_PANE);
            }
            b.fill(2, s + 3, z, 3, s + 3, z, Blocks.TINTED_GLASS);
        }
        b.set(5, s + 1, 10, Blocks.COBWEB);
        // The control room: consoles along the north wall, screens dark over them, the chest, the map in the floor
        for (int x = 7; x <= 16; x++) {
            b.set(x, s + 1, 2, x % 3 == 0 ? Blocks.COPPER_BULB.waxed().oxidized().defaultBlockState() : Blocks.SMOOTH_QUARTZ_SLAB.defaultBlockState().setValue(SlabBlock.TYPE, SlabType.TOP));
            b.set(x, s + 2, 1, Blocks.TINTED_GLASS);
        }
        b.set(8, s + 2, 2, Blocks.LEVER.defaultBlockState().setValue(net.minecraft.world.level.block.LeverBlock.FACE, AttachFace.FLOOR));
        b.set(14, s + 2, 2, Blocks.REDSTONE_LAMP);
        b.chest(16, s + 1, 4, Direction.WEST, ModChestLoot.GRAY_LAB_CONTROL);
        String[] map = {"wwgggw", "wgjjgw", "wgjsgg", "wggbgw"}; // the shell (white), the land, the jungle, the sun, a sea
        for (int row = 0; row < map.length; row++) {
            for (int i = 0; i < map[row].length(); i++) {
                Block cell = switch (map[row].charAt(i)) {
                    case 'g' -> Blocks.CONCRETE.lime();
                    case 'j' -> Blocks.CONCRETE.green();
                    case 's' -> Blocks.CONCRETE.yellow();
                    case 'b' -> Blocks.CONCRETE.lightBlue();
                    default -> Blocks.CONCRETE.white();
                };
                b.set(9 + i, s, 3 + row, cell);
            }
        }
        // The specimen store: shelves of jars, barrels, a cauldron, the chest; the passage south to the dome
        b.shelf(7, s + 2, 9, Blocks.PALE_OAK_SHELF, Direction.EAST, Items.GLASS_BOTTLE, Items.SLIME_BALL, Items.GLASS_BOTTLE);
        b.shelf(7, s + 2, 10, Blocks.PALE_OAK_SHELF, Direction.EAST, Items.GLOW_INK_SAC, null, Items.FERMENTED_SPIDER_EYE);
        b.fill(7, s + 1, 9, 7, s + 1, 11, Blocks.BARREL);
        b.set(8, s + 1, 11, Blocks.CAULDRON);
        b.set(12, s + 1, 9, Blocks.GLASS);
        b.set(12, s + 2, 9, Blocks.SEA_PICKLE.defaultBlockState().setValue(SeaPickleBlock.PICKLES, 2).setValue(SeaPickleBlock.WATERLOGGED, false));
        b.chest(12, s + 1, 11, Direction.WEST, ModChestLoot.GRAY_LAB_STORE);
        b.air(10, s + 1, 12, 11, s + 3, 12);
        b.fill(10, s, 12, 11, s, 16, Blocks.CONCRETE.lightGray());
        b.walls(9, s + 1, 13, 12, s + 4, 15, Blocks.CONCRETE.white().defaultBlockState());
        b.air(10, s + 1, 12, 11, s + 3, 16);
        b.fill(9, s + 4, 13, 12, s + 4, 15, Blocks.SMOOTH_QUARTZ);
        // The airlock: grates over and under, the shower's lights, the outer door blown in
        b.fill(14, s, 9, 16, s, 11, Blocks.COPPER_GRATE.waxed().weathered());
        b.fill(14, s + 4, 9, 16, s + 4, 11, Blocks.COPPER_GRATE.waxed().weathered());
        b.set(15, s + 3, 10, Blocks.END_ROD.defaultBlockState().setValue(EndRodBlock.FACING, Direction.DOWN));
        b.air(17, s + 1, 9, 17, s + 3, 10);
        b.set(18, s, 9, Blocks.CONCRETE.black());
        b.set(18, s, 10, Blocks.CONCRETE.black());
        b.set(16, s + 1, 11, Blocks.IRON_BLOCK); // the door, lying where it landed
        for (int i = 0; i < 8; i++) {
            b.set(2 + r.nextInt(15), s + 4, 2 + r.nextInt(10), Blocks.COBWEB);
        }
    }

    // ---------------------------------------------------------------- the live Gray field station (10b)

    /**
     * 45 x 45, on a round apron of concrete (centre (22, 22), radius 21) ringed with lights. North, the containment yard:
     * four quartz-rimmed glass pens (x 2..10, 12..20, 24..32, 34..42; z 3..11), a scanner lamp in a beam over each, each
     * holding a tagged specimen (a mammoth, a megatherium, a terror bird, a glyptodon), a lit walkway along their fronts. South-west, the lab (a dome,
     * centre (12, 32), radius 9, intact and lit): an operating table with a tagged hare strapped to it under surgical arms,
     * three tanks with glow squid in them, a tagging bench (anvil, brewing stands) and two chests. South-east, the reactor
     * (centre (33, 32)): a glass column round a core of phlogiston lamps in an iron frame, with a chest. In the far corner a
     * mast with a lit dish. Three sentinels keep watch (over the pens, the dome and the reactor), and two specimens are
     * loose on trial: a terror bird on the south lawn and a glyptodon by the mast.
     */
    static Blueprint grayStation() {
        Blueprint b = new Blueprint(GRAY_STATION);
        Random r = new Random(1952);
        int s = FLOOR, c = 22;
        b.air(0, s + 1, 0, 44, s + 26, 44);
        // The apron, ringed with lights
        for (int x = 0; x <= 44; x++) {
            for (int z = 0; z <= 44; z++) {
                double d = Math.hypot(x - c, z - c);
                if (d > 21.5) {
                    continue;
                }
                Block floor = d > 20.5 ? Blocks.CONCRETE.white() : ((int) d) % 5 == 0 ? Blocks.CONCRETE.lightGray() : Blocks.SMOOTH_STONE;
                b.set(x, s, z, floor);
            }
        }
        for (int k = 0; k < 24; k++) {
            double a = k * Math.PI / 12;
            b.set(c + (int) Math.round(Math.cos(a) * 20), s, c + (int) Math.round(Math.sin(a) * 20), Blocks.SEA_LANTERN);
        }
        // The containment yard: four glass pens, iron posts, a scanner lamp hung over each
        CompoundTag tagged = new CompoundTag();
        tagged.putBoolean(Specimens.SAVE_KEY, true);
        int[] pens = {2, 12, 24, 34};
        EntityType<?>[] specimens = {ModEntities.WOOLLY_MAMMOTH.get(), ModEntities.MEGATHERIUM.get(),
                ModEntities.TERROR_BIRD.get(), ModEntities.GLYPTODON.get()};
        for (int i = 0; i < pens.length; i++) {
            int x0 = pens[i], x1 = x0 + 8, z0 = 3, z1 = 11;
            b.fill(x0 + 1, s, z0 + 1, x1 - 1, s, z1 - 1, Blocks.COARSE_DIRT);
            b.walls(x0, s + 1, z0, x1, s + 4, z1, Blocks.GLASS.defaultBlockState());
            for (int[] post : new int[][] {{x0, z0}, {x1, z0}, {x0, z1}, {x1, z1}}) {
                b.fill(post[0], s + 1, post[1], post[0], s + 5, post[1], Blocks.IRON_BLOCK);
            }
            // A white rim along the glass, a beam across the middle with the scanner lamp in it
            BlockState rim = Blocks.SMOOTH_QUARTZ_SLAB.defaultBlockState().setValue(SlabBlock.TYPE, SlabType.BOTTOM);
            int mx = (x0 + x1) / 2, mz = (z0 + z1) / 2;
            for (int x = x0 + 1; x < x1; x++) {
                b.set(x, s + 5, z0, rim);
                b.set(x, s + 5, z1, rim);
            }
            for (int z = z0 + 1; z < z1; z++) {
                b.set(x0, s + 5, z, rim);
                b.set(x1, s + 5, z, rim);
                b.set(mx, s + 5, z, rim);
            }
            b.set(mx, s + 5, mz, Blocks.SEA_LANTERN);
            b.set(x0 + 2, s + 1, z0 + 2, Blocks.HAY_BLOCK);
            b.set(x1 - 2, s + 1, z0 + 2, Blocks.CAULDRON);
            b.entity(specimens[i], mx, s + 1, mz + 1, tagged);
        }
        // The walkway along the pens' fronts, lit
        b.fill(1, s, 13, 43, s, 15, Blocks.CONCRETE.white());
        for (int x = 3; x <= 41; x += 4) {
            b.set(x, s, 14, Blocks.SEA_LANTERN);
        }
        // The lab: an intact dome, a band of tinted windows, a lit eye on top, a doorway on the walkway
        int dcx = 12, dcz = 32;
        for (int x = dcx - 10; x <= dcx + 10; x++) {
            for (int z = dcz - 10; z <= dcz + 10; z++) {
                double d = Math.hypot(x - dcx, z - dcz);
                if (d > 9.5) {
                    continue;
                }
                b.set(x, s, z, ((int) d) % 3 == 0 ? Blocks.CONCRETE.lightGray() : Blocks.CONCRETE.white());
                for (int y = s + 1; y <= s + 11; y++) {
                    double e = y <= s + 4 ? d : Math.sqrt(d * d + sq((y - s - 4) * 9.0 / 7.0));
                    if (e <= 8.4 || e > 9.5) {
                        continue;
                    }
                    b.set(x, y, z, y == s + 3 ? Blocks.TINTED_GLASS : y <= s + 4 ? Blocks.CONCRETE.white() : d <= 2.2 ? Blocks.SEA_LANTERN : Blocks.SMOOTH_QUARTZ);
                }
            }
        }
        b.air(dcx - 1, s + 1, dcz - 10, dcx + 1, s + 3, dcz - 8);
        b.fill(dcx - 1, s, dcz - 12, dcx + 1, s, dcz - 8, Blocks.CONCRETE.white());
        // A ring of lamps inside, and the operating table under surgical arms in the middle
        for (int x = dcx - 6; x <= dcx + 6; x++) {
            for (int z = dcz - 6; z <= dcz + 6; z++) {
                double d = Math.hypot(x - dcx, z - dcz);
                if (d > 5.5 && d <= 6.5) {
                    b.set(x, s + 7, z, (x + z) % 3 == 0 ? Blocks.SEA_LANTERN : Blocks.CONCRETE.lightGray());
                }
            }
        }
        BlockState slab = Blocks.SMOOTH_QUARTZ_SLAB.defaultBlockState().setValue(SlabBlock.TYPE, SlabType.TOP);
        b.fill(dcx - 1, s + 1, dcz, dcx + 1, s + 1, dcz, slab);
        b.set(dcx, s + 1, dcz - 1, Blocks.IRON_BLOCK);
        b.set(dcx, s + 1, dcz + 1, Blocks.IRON_BLOCK);
        BlockState arm = Blocks.END_ROD.defaultBlockState().setValue(EndRodBlock.FACING, Direction.DOWN);
        BlockState chain = Blocks.IRON_CHAIN.defaultBlockState().setValue(ChainBlock.AXIS, Direction.Axis.Y);
        for (int dx : new int[] {-1, 1}) {
            b.fill(dcx + dx, s + 4, dcz, dcx + dx, s + 6, dcz, chain);
            b.set(dcx + dx, s + 3, dcz, arm);
        }
        CompoundTag strapped = new CompoundTag();
        strapped.putBoolean("EarTag", true);
        strapped.putBoolean("NoAI", true);
        strapped.putBoolean("PersistenceRequired", true);
        b.entity(ModEntities.GLASSBACK_HARE.get(), dcx, s + 2, dcz, strapped);
        // Three tanks round the wall, each with a glow squid in it
        int[][] tanks = {{dcx - 5, dcz + 3}, {dcx, dcz + 6}, {dcx + 5, dcz + 3}};
        for (int[] tank : tanks) {
            int tx = tank[0], tz = tank[1];
            b.fill(tx - 1, s, tz - 1, tx + 1, s, tz + 1, Blocks.IRON_BLOCK);
            b.fill(tx - 1, s + 1, tz - 1, tx + 1, s + 4, tz + 1, Blocks.GLASS);
            b.fill(tx - 1, s + 5, tz - 1, tx + 1, s + 5, tz + 1, Blocks.IRON_BLOCK);
            b.fill(tx, s + 1, tz, tx, s + 4, tz, Blocks.WATER);
            b.entity(EntityTypes.GLOW_SQUID, tx, s + 2, tz);
        }
        // The tagging bench and the chests, on the west side
        b.set(dcx - 7, s + 1, dcz - 2, Blocks.ANVIL);
        b.set(dcx - 7, s + 1, dcz - 1, Blocks.BREWING_STAND);
        b.set(dcx - 7, s + 1, dcz, Blocks.BREWING_STAND);
        b.chest(dcx - 7, s + 1, dcz + 1, Direction.EAST, ModChestLoot.GRAY_STATION);
        b.chest(dcx + 7, s + 1, dcz - 1, Direction.WEST, ModChestLoot.GRAY_STATION);
        // The reactor: a glass column round a core of phlogiston lamps, in an iron frame
        int rcx = 33, rcz = 32;
        for (int x = rcx - 4; x <= rcx + 4; x++) {
            for (int z = rcz - 4; z <= rcz + 4; z++) {
                double d = Math.hypot(x - rcx, z - rcz);
                if (d <= 4.5) {
                    b.set(x, s, z, d <= 1.5 ? Blocks.IRON_BLOCK : Blocks.CONCRETE.black());
                }
                if (d > 1.5 && d <= 2.5) {
                    b.fill(x, s + 1, z, x, s + 8, z, Blocks.GLASS);
                }
                if (d <= 1.5) {
                    b.fill(x, s + 1, z, x, s + 8, z, x == rcx && z == rcz ? ModBlocks.PHLOGISTON_LAMP.get() : Blocks.SEA_LANTERN);
                }
            }
        }
        for (int[] post : new int[][] {{rcx - 3, rcz - 3}, {rcx + 3, rcz - 3}, {rcx - 3, rcz + 3}, {rcx + 3, rcz + 3}}) {
            b.fill(post[0], s + 1, post[1], post[0], s + 9, post[1], Blocks.IRON_BLOCK);
        }
        b.fill(rcx - 3, s + 9, rcz - 3, rcx + 3, s + 9, rcz + 3, Blocks.SMOOTH_QUARTZ_SLAB.defaultBlockState().setValue(SlabBlock.TYPE, SlabType.BOTTOM));
        b.fill(rcx - 1, s + 9, rcz - 1, rcx + 1, s + 9, rcz + 1, ModBlocks.PHLOGISTON_LAMP.get());
        b.chest(rcx - 4, s + 1, rcz + 3, Direction.EAST, ModChestLoot.GRAY_STATION);
        // The mast and its dish, in the far corner
        int mx = 38, mz = 41;
        b.fill(mx - 1, s, mz - 1, mx + 1, s, mz + 1, Blocks.IRON_BLOCK);
        b.fill(mx, s + 1, mz, mx, s + 20, mz, Blocks.IRON_BARS);
        for (int y : new int[] {s + 7, s + 14}) {
            b.fill(mx - 1, y, mz, mx + 1, y, mz, Blocks.IRON_BARS);
            b.fill(mx, y, mz - 1, mx, y, mz + 1, Blocks.IRON_BARS);
        }
        BlockState dish = Blocks.SMOOTH_QUARTZ_SLAB.defaultBlockState().setValue(SlabBlock.TYPE, SlabType.BOTTOM);
        for (int dx = -2; dx <= 2; dx++) {
            for (int dz = -2; dz <= 2; dz++) {
                if (Math.abs(dx) + Math.abs(dz) < 4) {
                    b.set(mx + dx, s + 21, mz + dz, Math.abs(dx) + Math.abs(dz) <= 1 ? Blocks.SMOOTH_QUARTZ.defaultBlockState() : dish);
                }
            }
        }
        b.set(mx, s + 21, mz, Blocks.SEA_LANTERN);
        b.set(mx, s + 22, mz, Blocks.END_ROD);
        stationOps(b, r, s, c);
        // The watch, and the two on trial
        b.entity(ModEntities.GRAY_SENTINEL.get(), c, s + 9, 9);
        b.entity(ModEntities.GRAY_SENTINEL.get(), dcx, s + 15, dcz);
        b.entity(ModEntities.GRAY_SENTINEL.get(), rcx, s + 13, rcz);
        b.entity(ModEntities.TERROR_BIRD.get(), c, s + 1, 41, tagged);
        b.entity(ModEntities.GLYPTODON.get(), 30, s + 1, 40, tagged);
        b.entity(ModEntities.GRAY_PROBE.get(), STATION_PAD_X, s + 3, STATION_PAD_Z); // parked over its pad
        return b;
    }

    /** Where the station's landing pad is, and its probe parked over it. */
    public static final int STATION_PAD_X = 39, STATION_PAD_Z = 25;

    /**
     * The station's working parts (25): the ops block between the pens' walkway and the reactor (x 24..36, z 17..21; the
     * monitoring room west, its windows on the pens, and the feed store east), a landing pad by the reactor with a probe
     * parked over it and scorch round it, and a lit fence of bars round the apron, open at the four quarters.
     */
    private static void stationOps(Blueprint b, Random r, int s, int c) {
        b.fill(24, s, 17, 36, s, 21, Blocks.CONCRETE.lightGray());
        b.walls(24, s + 1, 17, 36, s + 4, 21, Blocks.CONCRETE.white().defaultBlockState());
        b.fill(24, s + 5, 17, 36, s + 5, 21, Blocks.SMOOTH_QUARTZ);
        b.air(25, s + 1, 18, 35, s + 4, 20);
        b.fill(30, s + 1, 18, 30, s + 4, 20, Blocks.CONCRETE.white());
        b.air(30, s + 1, 19, 30, s + 3, 19);
        for (int x = 25; x <= 29; x++) {
            b.set(x, s + 2, 17, Blocks.TINTED_GLASS);
            b.set(x, s + 3, 17, Blocks.TINTED_GLASS);
        }
        b.set(27, s + 5, 19, Blocks.SEA_LANTERN);
        b.set(33, s + 5, 19, Blocks.SEA_LANTERN);
        b.air(27, s + 1, 21, 27, s + 3, 21);
        b.air(33, s + 1, 21, 33, s + 3, 21);
        // The monitoring room: a desk of consoles under the windows, a lamp lit on each, a stool
        for (int x = 25; x <= 29; x++) {
            b.set(x, s + 1, 18, x % 2 == 1 ? Blocks.COPPER_BULB.waxed().unaffected().defaultBlockState().setValue(net.minecraft.world.level.block.CopperBulbBlock.LIT, true)
                    : Blocks.SMOOTH_QUARTZ_SLAB.defaultBlockState().setValue(SlabBlock.TYPE, SlabType.TOP));
        }
        b.set(27, s + 1, 19, Blocks.SMOOTH_QUARTZ_SLAB.defaultBlockState().setValue(SlabBlock.TYPE, SlabType.BOTTOM));
        b.set(25, s + 1, 20, Blocks.CARTOGRAPHY_TABLE);
        // The feed store: hay and barrels, troughs, the tagging gear, a chest
        b.fill(31, s + 1, 18, 32, s + 2, 18, Blocks.HAY_BLOCK);
        b.fill(34, s + 1, 18, 35, s + 1, 18, Blocks.BARREL);
        b.set(35, s + 1, 19, Blocks.CAULDRON);
        b.set(31, s + 1, 20, Blocks.ANVIL);
        b.shelf(35, s + 2, 20, Blocks.PALE_OAK_SHELF, Direction.WEST, Items.LEAD, Items.NAME_TAG, Items.LEAD);
        b.chest(34, s + 1, 20, Direction.NORTH, ModChestLoot.GRAY_STATION_STORE);
        // The landing pad: dark, rimmed white, lights at its points, scorched round about
        int px = STATION_PAD_X, pz = STATION_PAD_Z;
        for (int x = px - 4; x <= px + 4; x++) {
            for (int z = pz - 4; z <= pz + 4; z++) {
                double d = Math.hypot(x - px, z - pz);
                if (d <= 3.4) {
                    b.set(x, s, z, d > 2.5 ? Blocks.CONCRETE.white() : d <= 0.9 ? Blocks.IRON_BLOCK : Blocks.CONCRETE.gray());
                } else if (d <= 4.4 && r.nextInt(3) == 0 && Math.hypot(x - c, z - c) <= 20.5) {
                    b.set(x, s, z, Blocks.CONCRETE.black());
                }
            }
        }
        for (int[] p : new int[][] {{px - 3, pz}, {px + 3, pz}, {px, pz - 3}, {px, pz + 3}}) {
            b.set(p[0], s, p[1], Blocks.SEA_LANTERN);
        }
        // The fence round the apron, its posts lit, open at the quarters
        for (int x = 0; x <= 44; x++) {
            for (int z = 0; z <= 44; z++) {
                double d = Math.hypot(x - c, z - c);
                boolean clear = z <= 15 || Math.hypot(x - 12, z - 32) <= 10.5; // the pens and their walkway, the dome
                if (d <= 20.8 || d > 21.5 || Math.abs(x - c) <= 2 || Math.abs(z - c) <= 2 || clear) {
                    continue;
                }
                b.fill(x, s + 1, z, x, s + 2, z, Blocks.IRON_BARS);
                if ((x + z) % 7 == 0) {
                    b.set(x, s + 3, z, Blocks.END_ROD);
                }
            }
        }
    }

    // ---------------------------------------------------------------- the Gray pens (20)

    /** The pens' three versions, which differ only in who's held: {@code gray_pens_1..3}. */
    public static String pensTemplate(int variant) {
        return GRAY_PENS + "_" + variant;
    }

    /** Where the rig hangs, and the table under it (the patient lies on its middle). */
    public static final int RIG_X = 24, RIG_Y = 5, RIG_Z = 12;
    /** The cells' middles along the north wall (x), all at z {@link #CELL_Z}; each door is at x + 1 on z {@link #CELL_FRONT}. */
    public static final int[] CELLS = {4, 8, 12, 16};
    public static final int CELL_Z = 4, CELL_FRONT = 6;

    /**
     * 33 x 27 on a pale apron: a low white block (x 2..30, z 2..20) with a lit roof and a mast. West, the cell block: four
     * cells along the north wall behind bars, each with an iron door and a button beside it out in the corridor, and a
     * Warden on watch. East, through a doorway, the operating room: a restraint table under the {@link ModBlocks#IMPLANT_RIG
     * implant rig}, surgical arms either side, a captive strapped down, two Wardens at work (the surgeons), and a cabinet
     * with a chest. A Warden at the door and two sentinels over the roof. Who's held changes with the variant (two, three
     * or four in the cells; mostly outlaws, now and then a townsperson or a lawman):
     * <ol>
     * <li>two outlaws; an outlaw on the table;</li>
     * <li>an outlaw, a gunslinger and a prospector; an outlaw on the table;</li>
     * <li>an outlaw, a deputy, an outlaw and a wrangler; a blacksmith on the table.</li>
     * </ol>
     */
    static Blueprint grayPens(int variant) {
        Blueprint b = new Blueprint(pensTemplate(variant));
        int s = FLOOR;
        b.air(0, s + 1, 0, 32, s + 14, 26);
        // The apron, with a lit edge
        for (int x = 0; x <= 32; x++) {
            for (int z = 0; z <= 26; z++) {
                boolean edge = x == 0 || z == 0 || x == 32 || z == 26;
                if (edge && (x == 0 || x == 32) && (z == 0 || z == 26)) {
                    continue;
                }
                b.set(x, s, z, edge ? Blocks.CONCRETE.white() : (x + z) % 6 == 0 ? Blocks.CONCRETE.lightGray() : Blocks.SMOOTH_STONE);
            }
        }
        for (int x = 4; x <= 28; x += 6) {
            b.set(x, s, 0, Blocks.SEA_LANTERN);
            b.set(x, s, 26, Blocks.SEA_LANTERN);
        }
        // The block: white walls, a band of tinted windows, a flat roof with lights let into it
        b.fill(2, s, 2, 30, s, 20, Blocks.CONCRETE.lightGray());
        b.walls(2, s + 1, 2, 30, s + 5, 20, Blocks.CONCRETE.white().defaultBlockState());
        for (int x = 4; x <= 28; x += 3) {
            b.set(x, s + 3, 2, Blocks.TINTED_GLASS);
            b.set(x, s + 3, 20, Blocks.TINTED_GLASS);
        }
        b.fill(2, s + 6, 2, 30, s + 6, 20, Blocks.SMOOTH_QUARTZ);
        for (int x = 5; x <= 27; x += 4) {
            for (int z = 9; z <= 17; z += 4) {
                b.set(x, s + 6, z, Blocks.SEA_LANTERN);
            }
        }
        // The door, south, with a lit step
        b.air(15, s + 1, 20, 17, s + 3, 20);
        b.fill(14, s, 21, 18, s, 22, Blocks.CONCRETE.white());
        b.set(14, s + 1, 21, Blocks.END_ROD);
        b.set(18, s + 1, 21, Blocks.END_ROD);
        // The cell block (west): a wall between it and the operating room, with a doorway
        b.fill(18, s + 1, 3, 18, s + 5, 19, Blocks.CONCRETE.white());
        b.air(18, s + 1, 11, 18, s + 3, 13);
        // Four cells: dividers (x 2, 6, 10, 14, 18 bound them), bars along the front, an iron door at each one's east end
        // and a button out in the corridor on the divider beside it
        BlockState bars = Blocks.IRON_BARS.defaultBlockState();
        BlockState button = Blocks.POLISHED_BLACKSTONE_BUTTON.defaultBlockState()
                .setValue(ButtonBlock.FACE, AttachFace.WALL).setValue(ButtonBlock.FACING, Direction.SOUTH);
        for (int cx : CELLS) {
            int west = cx - 2, east = cx + 2;
            b.fill(east, s + 1, 3, east, s + 5, CELL_FRONT, Blocks.CONCRETE.white());
            b.fill(west + 1, s + 1, CELL_FRONT, cx, s + 3, CELL_FRONT, bars);
            b.door(cx + 1, s + 1, CELL_FRONT, (DoorBlock) Blocks.IRON_DOOR, Direction.SOUTH, DoorHingeSide.RIGHT);
            b.fill(west + 1, s + 4, CELL_FRONT, cx + 1, s + 5, CELL_FRONT, Blocks.CONCRETE.white());
            b.set(cx + 1, s + 3, CELL_FRONT, Blocks.CONCRETE.white());
            b.set(east, s + 2, CELL_FRONT + 1, button);
            b.fill(west + 1, s, 3, east - 1, s, CELL_FRONT - 1, Blocks.CONCRETE.gray());
            b.set(cx - 1, s + 1, 3, Blocks.CARPET.white());
            b.set(cx, s + 1, 3, Blocks.CARPET.white());
        }
        // The corridor: a lit strip down the floor
        for (int x = 4; x <= 16; x += 3) {
            b.set(x, s, 9, Blocks.SEA_LANTERN);
        }
        // The operating room (east): the restraint table, iron at the ends, under the rig and its arms
        BlockState slab = Blocks.SMOOTH_QUARTZ_SLAB.defaultBlockState().setValue(SlabBlock.TYPE, SlabType.TOP);
        b.fill(RIG_X - 1, s + 1, RIG_Z, RIG_X + 1, s + 1, RIG_Z, slab);
        b.set(RIG_X, s + 1, RIG_Z - 1, Blocks.IRON_BLOCK);
        b.set(RIG_X, s + 1, RIG_Z + 1, Blocks.IRON_BLOCK);
        b.set(RIG_X, s + RIG_Y, RIG_Z, ModBlocks.IMPLANT_RIG.get());
        BlockState arm = Blocks.END_ROD.defaultBlockState().setValue(EndRodBlock.FACING, Direction.DOWN);
        BlockState chain = Blocks.IRON_CHAIN.defaultBlockState().setValue(ChainBlock.AXIS, Direction.Axis.Y);
        for (int dx : new int[] {-2, 2}) {
            b.fill(RIG_X + dx, s + 4, RIG_Z, RIG_X + dx, s + 5, RIG_Z, chain);
            b.set(RIG_X + dx, s + 3, RIG_Z, arm);
        }
        // A ring of floor lights round the table, a cabinet and a chest along the east wall
        for (int dx = -3; dx <= 3; dx++) {
            for (int dz = -3; dz <= 3; dz++) {
                if (Math.abs(dx) == 3 || Math.abs(dz) == 3) {
                    b.set(RIG_X + dx, s, RIG_Z + dz, (dx + dz) % 2 == 0 ? Blocks.SEA_LANTERN : Blocks.CONCRETE.white());
                }
            }
        }
        b.set(29, s + 1, 5, Blocks.BREWING_STAND);
        b.set(29, s + 1, 6, Blocks.ANVIL);
        b.set(29, s + 1, 7, Blocks.BREWING_STAND);
        b.chest(29, s + 1, 18, Direction.WEST, ModChestLoot.GRAY_STATION);
        // The evidence locker (25), south-west: what the held had on them, on shelves and a stand, and the rest in a chest
        b.fill(9, s + 1, 14, 9, s + 5, 19, Blocks.CONCRETE.white());
        b.fill(3, s + 1, 14, 9, s + 5, 14, Blocks.CONCRETE.white());
        b.air(6, s + 1, 14, 6, s + 3, 14);
        b.set(6, s + 4, 14, Blocks.SEA_LANTERN);
        b.shelf(3, s + 2, 16, Blocks.PALE_OAK_SHELF, Direction.EAST, ModItems.COWBOY_HAT.get(), ModItems.REVOLVER.get(), ModItems.BANDANA.get());
        b.shelf(3, s + 2, 18, Blocks.PALE_OAK_SHELF, Direction.EAST, ModItems.OUTLAW_BADGE.get(), ModItems.FRONTIER_GAZETTE.get(), Items.CLOCK);
        b.fill(3, s + 1, 15, 3, s + 1, 19, Blocks.BARREL);
        b.armorStand(8, s + 1, 18, 90, ModItems.COWBOY_HAT.get(), ModItems.DUSTER.get(), null, ModItems.COWBOY_BOOTS.get());
        b.chest(8, s + 1, 15, Direction.WEST, ModChestLoot.GRAY_PENS_BELONGINGS);
        b.set(5, s + 1, 19, Blocks.CARPET.lightGray());
        // The watch desk by the door: a console, its lamp lit
        b.set(12, s + 1, 18, Blocks.SMOOTH_QUARTZ_SLAB.defaultBlockState().setValue(SlabBlock.TYPE, SlabType.TOP));
        b.set(13, s + 1, 18, Blocks.COPPER_BULB.waxed().unaffected().defaultBlockState().setValue(net.minecraft.world.level.block.CopperBulbBlock.LIT, true));
        b.set(12, s + 2, 20, Blocks.TINTED_GLASS);
        b.set(14, s + 2, 20, Blocks.TINTED_GLASS);
        // A mast on the roof, its dish lit
        b.fill(28, s + 7, 4, 28, s + 12, 4, Blocks.IRON_BARS);
        b.set(28, s + 13, 4, Blocks.SEA_LANTERN);
        b.set(28, s + 14, 4, Blocks.END_ROD);
        // The held: in the cells, and on the table
        EntityType<?>[][] cells = switch (variant) {
            case 1 -> new EntityType<?>[][] {{ModEntities.OUTLAW.get()}, {}, {ModEntities.OUTLAW.get()}, {}};
            case 2 -> new EntityType<?>[][] {{ModEntities.OUTLAW.get()}, {ModEntities.OUTLAW_GUNSLINGER.get()}, {ModEntities.TOWNSFOLK.get()}, {}};
            default -> new EntityType<?>[][] {{ModEntities.OUTLAW.get()}, {ModEntities.LAWMAN.get()}, {ModEntities.OUTLAW.get()}, {ModEntities.TOWNSFOLK.get()}};
        };
        String[] folk = variant == 2 ? new String[] {"", "", "prospector", ""} : new String[] {"", "", "", "wrangler"};
        for (int i = 0; i < CELLS.length; i++) {
            if (cells[i].length == 0) {
                continue;
            }
            CompoundTag held = captive(false);
            if (!folk[i].isEmpty()) {
                held.putString("folk", folk[i]);
            }
            if (cells[i][0] == ModEntities.LAWMAN.get()) {
                held.putString("rank", "deputy");
            }
            b.entity(cells[i][0], CELLS[i], s + 1, CELL_Z, held);
        }
        CompoundTag strapped = captive(true);
        if (variant == 3) {
            strapped.putString("folk", "blacksmith");
        }
        b.entity(variant == 3 ? ModEntities.TOWNSFOLK.get() : ModEntities.OUTLAW.get(), RIG_X, s + 2, RIG_Z, strapped);
        // The Grays: two surgeons at the table, one on watch in the corridor, one at the door, two sentinels over the roof
        b.entity(ModEntities.GRAY_WARDEN.get(), RIG_X - 2, s + 1, RIG_Z - 2, grayTags(Captives.SURGEON, Captives.GUARD));
        b.entity(ModEntities.GRAY_WARDEN.get(), RIG_X + 2, s + 1, RIG_Z + 2, grayTags(Captives.SURGEON, Captives.GUARD));
        b.entity(ModEntities.GRAY_WARDEN.get(), 10, s + 1, 12, grayTags(Captives.GUARD));
        b.entity(ModEntities.GRAY_WARDEN.get(), 16, s + 1, 23, grayTags(Captives.GUARD));
        b.entity(ModEntities.GRAY_SENTINEL.get(), 8, s + 10, 11);
        b.entity(ModEntities.GRAY_SENTINEL.get(), 24, s + 10, 11);
        return b;
    }

    /** A captive's NBT: the {@link Captive} attachment, as the template carries it. */
    static CompoundTag captive(boolean table) {
        CompoundTag captive = new CompoundTag();
        captive.putBoolean("table", table);
        CompoundTag wrapped = new CompoundTag();
        wrapped.put("captive", captive);
        CompoundTag attachments = new CompoundTag();
        attachments.put(HollowFrontier.MODID + ":captive", wrapped);
        CompoundTag nbt = new CompoundTag();
        nbt.put("neoforge:attachments", attachments);
        return nbt;
    }

    private static CompoundTag grayTags(String... tags) {
        ListTag list = new ListTag();
        for (String tag : tags) {
            list.add(StringTag.valueOf(tag));
        }
        CompoundTag nbt = new CompoundTag();
        nbt.put("Tags", list);
        return nbt;
    }

    // ---------------------------------------------------------------- the Symmes camp

    /**
     * 35 x 35, on the ground itself. Along the north side the expedition's airship lies where it came down: its envelope
     * of canvas (wool), 27 long, sagging between its ribs (dark oak poking through), its gondola broken off beside it
     * (x 11..23, z 11..14) with the boiler still aboard and the propeller at the stern. Round the camp (centre (17, 19)) a
     * palisade of stakes, crushed where the ship fell and burned to stumps by the ring to the south-east; a gate in the
     * south. Inside: the command tent (x 3..11, z 16..24, open to the east) with bedrolls, a map table and the journal
     * on its lectern; two small tents, one torn down; a cold fire ringed with logs; stores; a watchtower with a ladder;
     * three graves, the last of them open and empty; trodden paths; and the ring burned into the grass (centre (27, 29)).
     */
    static Blueprint symmesCamp() {
        Blueprint b = new Blueprint(SYMMES_CAMP);
        Random r = new Random(1889);
        int s = FLOOR;
        b.air(0, s + 1, 0, 34, s + 12, 34);
        // The envelope
        for (int x = 3; x <= 31; x++) {
            for (int z = 0; z <= 12; z++) {
                double e = sq((x - 17) / 13.5) + sq((z - 6) / 5.5);
                if (e >= 1) {
                    continue;
                }
                boolean rib = (x - 5) % 5 == 0;
                int h = 1 + (int) Math.round(Math.sqrt(1 - e) * 5);
                int top = rib ? h : Math.max(1, h - 1);
                Block canvas = r.nextInt(7) == 0 ? Blocks.WOOL.lightGray() : Blocks.WOOL.white();
                if (e > 0.55) {
                    b.fill(x, s + 1, z, x, s + top, z, canvas);
                } else if (r.nextInt(10) != 0) {
                    b.set(x, s + top, z, canvas);
                }
                if (rib && e < 0.8 && r.nextInt(3) != 0) {
                    b.set(x, s + top + 1, z, Blocks.DARK_OAK_FENCE);
                }
            }
        }
        gondola(b, r, s);
        // The palisade
        for (int x = 0; x <= 34; x++) {
            for (int z = 0; z <= 34; z++) {
                double d = Math.hypot(x - 17, z - 19);
                if (d <= 14.5 || d > 15.5 || z < 12 || (z > 30 && Math.abs(x - 17) <= 2)) {
                    continue;
                }
                if (Math.hypot(x - 27, z - 29) < 6) {
                    if (r.nextBoolean()) {
                        b.set(x, s + 1, z, Blocks.DARK_OAK_LOG); // burned to stumps
                    }
                    continue;
                }
                if (r.nextInt(9) == 0) {
                    continue;
                }
                int height = r.nextInt(7) == 0 ? 1 : 3;
                b.fill(x, s + 1, z, x, s + height, z, Blocks.STRIPPED_SPRUCE_LOG);
                if (height == 3 && r.nextBoolean()) {
                    b.set(x, s + 4, z, Blocks.SPRUCE_FENCE);
                }
            }
        }
        for (int gx : new int[] {14, 20}) {
            b.fill(gx, s + 1, 33, gx, s + 4, 33, Blocks.SPRUCE_LOG);
            b.set(gx, s + 5, 33, Blocks.LANTERN);
        }
        BlockState fallenStake = Blocks.STRIPPED_SPRUCE_LOG.defaultBlockState().setValue(RotatedPillarBlock.AXIS, Direction.Axis.X);
        b.fill(2, s + 1, 13, 5, s + 1, 13, fallenStake);
        b.fill(29, s + 1, 11, 32, s + 1, 11, fallenStake);
        // The command tent, ridge east-west, open to the east
        for (int x = 3; x <= 11; x++) {
            for (int dz = -4; dz <= 4; dz++) {
                int y = s + 5 - Math.abs(dz);
                b.set(x, y, 20 + dz, Blocks.WOOL.white());
                if (x == 3) {
                    b.fill(x, s + 1, 20 + dz, x, y, 20 + dz, Blocks.WOOL.white());
                }
            }
        }
        b.fill(2, s + 1, 20, 2, s + 5, 20, Blocks.SPRUCE_FENCE);
        b.fill(12, s + 1, 20, 12, s + 5, 20, Blocks.SPRUCE_FENCE);
        b.fill(4, s + 1, 17, 5, s + 1, 17, Blocks.CARPET.brown());
        b.fill(4, s + 1, 23, 5, s + 1, 23, Blocks.CARPET.brown());
        b.set(5, s + 1, 20, Blocks.CARTOGRAPHY_TABLE);
        b.blockEntity(7, s + 1, 20, Blocks.LECTERN.defaultBlockState().setValue(LecternBlock.FACING, Direction.EAST).setValue(LecternBlock.HAS_BOOK, true),
                lectern(journal()));
        b.set(9, s + 1, 23, Blocks.BARREL);
        b.set(9, s + 2, 23, Blocks.LANTERN);
        // Two small tents on the east side, the second torn down
        smallTent(b, 22, 15);
        for (int x = 22; x <= 26; x++) {
            for (int z = 20; z <= 23; z++) {
                if (r.nextInt(4) != 0) {
                    b.set(x, s + 1, z, Blocks.CARPET.white());
                }
            }
        }
        b.set(24, s + 1, 21, Blocks.WOOL.white());
        b.set(26, s + 1, 23, Blocks.SPRUCE_FENCE);
        // The fire and its benches
        b.set(17, s + 1, 21, Blocks.CAMPFIRE.defaultBlockState().setValue(CampfireBlock.LIT, false));
        BlockState logX = Blocks.SPRUCE_LOG.defaultBlockState().setValue(RotatedPillarBlock.AXIS, Direction.Axis.X);
        BlockState logZ = Blocks.SPRUCE_LOG.defaultBlockState().setValue(RotatedPillarBlock.AXIS, Direction.Axis.Z);
        b.fill(16, s + 1, 18, 18, s + 1, 18, logX);
        b.fill(16, s + 1, 24, 18, s + 1, 24, logX);
        b.fill(14, s + 1, 20, 14, s + 1, 22, logZ);
        b.fill(20, s + 1, 20, 20, s + 1, 22, logZ);
        // Stores
        for (int[] p : new int[][] {{18, 27}, {19, 27}, {18, 28}, {20, 28}, {21, 29}}) {
            b.set(p[0], s + 1, p[1], Blocks.BARREL);
        }
        b.set(18, s + 2, 27, Blocks.BARREL);
        b.set(19, s + 2, 27, Blocks.LANTERN);
        b.chest(19, s + 1, 29, Direction.NORTH, ModChestLoot.SYMMES_CAMP);
        b.set(21, s + 1, 27, Blocks.HAY_BLOCK);
        // The watchtower: four legs, a ladder up a middle post, a railed platform eight up under a roof
        for (int[] leg : new int[][] {{28, 13}, {30, 13}, {28, 15}, {30, 15}}) {
            b.fill(leg[0], s + 1, leg[1], leg[0], s + 10, leg[1], Blocks.SPRUCE_LOG);
        }
        b.fill(27, s + 8, 12, 31, s + 8, 16, Blocks.SPRUCE_PLANKS);
        b.fill(29, s + 1, 14, 29, s + 9, 14, Blocks.SPRUCE_LOG);
        for (int x = 27; x <= 31; x++) {
            for (int z = 12; z <= 16; z++) {
                boolean edge = x == 27 || x == 31 || z == 12 || z == 16;
                if (edge && !(x == 29 && z == 16)) {
                    b.set(x, s + 9, z, Blocks.SPRUCE_FENCE);
                }
            }
        }
        b.fill(27, s + 11, 12, 31, s + 11, 16, Blocks.SPRUCE_SLAB.defaultBlockState().setValue(SlabBlock.TYPE, SlabType.BOTTOM));
        b.fill(29, s + 1, 15, 29, s + 8, 15, Blocks.LADDER.defaultBlockState().setValue(LadderBlock.FACING, Direction.SOUTH));
        b.set(29, s + 10, 14, Blocks.LANTERN.defaultBlockState().setValue(LanternBlock.HANGING, true));
        // Three graves south of the big tent, the last open and empty
        for (int i = 0; i < 3; i++) {
            int gx = 6 + i * 3;
            if (i < 2) {
                b.fill(gx, s, 27, gx, s, 28, Blocks.COARSE_DIRT);
                b.fill(gx, s + 1, 26, gx, s + 2, 26, Blocks.SPRUCE_FENCE);
                b.set(gx - 1, s + 2, 26, Blocks.SPRUCE_FENCE);
                b.set(gx + 1, s + 2, 26, Blocks.SPRUCE_FENCE);
            } else {
                b.air(gx, s, 27, gx, s, 28);
                b.set(gx + 1, s + 1, 28, Blocks.COARSE_DIRT);
                b.set(gx, s + 1, 26, Blocks.SPRUCE_FENCE);
            }
        }
        // Trodden paths: from the gate up to the fire, across to the big tent and the stores
        for (int z = 25; z <= 34; z++) {
            if (r.nextInt(4) != 0) {
                b.set(17, s, z, Blocks.COARSE_DIRT);
            }
        }
        for (int x = 12; x <= 15; x++) {
            b.set(x, s, 21, Blocks.COARSE_DIRT);
        }
        b.set(18, s, 26, Blocks.COARSE_DIRT);
        // The burned ring
        for (int x = 22; x <= 32; x++) {
            for (int z = 24; z <= 34; z++) {
                double d = Math.hypot(x - 27, z - 29);
                if (d > 2.6 && d <= 3.6) {
                    b.set(x, s, z, Blocks.COARSE_DIRT);
                } else if (d <= 2.6) {
                    b.set(x, s, z, Blocks.PODZOL);
                    if (r.nextInt(3) == 0) {
                        b.set(x, s + 1, z, Blocks.DEAD_BUSH);
                    }
                }
            }
        }
        digSite(b, r, s);
        return b;
    }

    /**
     * The gondola (25), broken off the airship and lying on its side's keel at the envelope's south edge (x 11..23, z
     * 10..15): a cabin you can walk into by its door on the south side, portholes down both sides, its roof stove in, its
     * bow torn open to the east. Inside: the boiler with its pipes, the chart table, a bunk, the captain's chest, stores.
     * Out of the bow, the cargo it was carrying spilled east across the grass.
     */
    private static void gondola(Blueprint b, Random r, int s) {
        b.fill(11, s, 10, 23, s, 15, Blocks.SPRUCE_PLANKS);
        b.fill(11, s + 1, 10, 23, s + 1, 15, Blocks.SPRUCE_PLANKS);
        for (int y = s + 2; y <= s + 4; y++) {
            for (int x = 11; x <= 22; x++) {
                b.set(x, y, 10, y == s + 3 && x % 3 == 0 ? Blocks.GLASS_PANE.defaultBlockState() : Blocks.SPRUCE_PLANKS.defaultBlockState());
                b.set(x, y, 15, y == s + 3 && x % 3 == 0 ? Blocks.GLASS_PANE.defaultBlockState() : Blocks.SPRUCE_PLANKS.defaultBlockState());
            }
            b.fill(11, y, 10, 11, y, 15, Blocks.SPRUCE_PLANKS);
        }
        b.air(12, s + 2, 11, 22, s + 4, 14);
        b.air(23, s + 2, 10, 23, s + 4, 15); // the bow, torn open
        b.set(23, s + 2, 10, Blocks.SPRUCE_PLANKS);
        b.set(22, s + 4, 10, Blocks.AIR);
        b.set(22, s + 4, 15, Blocks.AIR);
        b.fill(11, s + 5, 10, 23, s + 5, 15, Blocks.SPRUCE_SLAB.defaultBlockState().setValue(SlabBlock.TYPE, SlabType.BOTTOM));
        b.air(10, s + 6, 10, 23, s + 10, 15); // nothing of the envelope (its ribs, its canvas) left standing on the roof
        b.air(16, s + 5, 12, 19, s + 5, 13); // stove in where the envelope's frame came down on it
        b.set(17, s + 5, 12, Blocks.DARK_OAK_LOG.defaultBlockState().setValue(RotatedPillarBlock.AXIS, Direction.Axis.X)); // the spar that did it
        b.door(17, s + 2, 15, (DoorBlock) Blocks.SPRUCE_DOOR, Direction.SOUTH, DoorHingeSide.LEFT);
        b.set(17, s + 1, 16, Blocks.SPRUCE_STAIRS.defaultBlockState().setValue(StairBlock.FACING, Direction.NORTH));
        // The engine room aft: the boiler, its pipes, the coal
        b.set(13, s + 2, 12, ModBlocks.BOILER.get());
        b.fill(14, s + 2, 12, 16, s + 2, 12, ModBlocks.COPPER_PIPE.get().defaultBlockState());
        b.set(12, s + 2, 14, Blocks.COAL_BLOCK);
        b.set(12, s + 2, 11, Blocks.BARREL);
        // The cabin forward: chart table, bunk, the captain's chest, a lantern swinging
        b.set(19, s + 2, 11, Blocks.CARTOGRAPHY_TABLE);
        b.set(20, s + 2, 11, Blocks.BARREL);
        b.bed(21, s + 2, 14, Blocks.BED.red(), Direction.WEST);
        b.chest(22, s + 2, 11, Direction.WEST, ModChestLoot.SYMMES_WRECK);
        b.set(13, s + 4, 13, Blocks.LANTERN.defaultBlockState().setValue(LanternBlock.HANGING, true)); // hung from the roof
        // The propeller at the stern
        b.fill(10, s + 2, 12, 10, s + 4, 12, Blocks.SPRUCE_FENCE);
        b.set(10, s + 3, 11, Blocks.SPRUCE_FENCE);
        b.set(10, s + 3, 13, Blocks.SPRUCE_FENCE);
        // The cargo, spilled out of the bow: barrels, a burst crate of coal, planks, rope
        int[][] spill = {{24, 12}, {25, 14}, {26, 11}, {27, 15}, {25, 16}, {28, 13}};
        for (int[] p : spill) {
            b.set(p[0], s + 1, p[1], r.nextBoolean() ? Blocks.BARREL : Blocks.SPRUCE_PLANKS);
        }
        b.set(26, s + 1, 13, Blocks.COAL_BLOCK);
        b.set(27, s + 1, 12, Blocks.SPRUCE_SLAB.defaultBlockState().setValue(SlabBlock.TYPE, SlabType.BOTTOM));
        b.set(29, s + 1, 14, Blocks.SPRUCE_TRAPDOOR);
        b.set(24, s + 1, 15, ModBlocks.COPPER_PIPE.get());
    }

    /**
     * The expedition's dig (25), east of the palisade (x 34..44, z 12..30): a mound of earth they were cutting back when
     * whatever happened happened. Its west face is dug away to show a Kindled carving in the rock: a robed figure in tuff
     * with a crystal eye, a starsteel sun over it. Scaffolding up the face, a sifting screen, the spoil heaped to one
     * side, survey stakes strung with lanterns, and a work tent with the dig's chest and a table of finds.
     */
    private static void digSite(Blueprint b, Random r, int s) {
        int mx = 40, mz = 20;
        b.air(34, s + 1, 10, 44, s + 12, 32);
        for (int x = 34; x <= 44; x++) {
            for (int z = 12; z <= 30; z++) {
                double e = sq((x - mx) / 5.0) + sq((z - mz) / 8.0);
                if (e >= 1) {
                    continue;
                }
                int h = (int) Math.round(Math.sqrt(1 - e) * 6);
                boolean cut = x <= 38; // the west face, dug back
                int top = cut ? Math.min(h, 1) : h;
                for (int y = s + 1; y <= s + top; y++) {
                    b.set(x, y, z, r.nextInt(5) == 0 ? Blocks.GRAVEL : r.nextInt(4) == 0 ? Blocks.ROOTED_DIRT : Blocks.COARSE_DIRT);
                }
                if (!cut && h > 0) {
                    b.set(x, s + h, z, r.nextInt(3) == 0 ? Blocks.COARSE_DIRT : Blocks.GRASS_BLOCK);
                }
            }
        }
        // The carving, in the face at x 39: a figure in tuff, its crystal eye, the sun over it
        for (int z = 17; z <= 23; z++) {
            for (int y = s + 1; y <= s + 5; y++) {
                b.set(39, y, z, Masonry.kindledWall(r));
            }
        }
        b.fill(39, s + 1, 20, 39, s + 3, 20, Blocks.CHISELED_TUFF_BRICKS);
        b.fill(39, s + 3, 19, 39, s + 3, 21, Blocks.CHISELED_TUFF_BRICKS);
        b.set(39, s + 4, 20, Masonry.chiseledCrystal());
        b.set(39, s + 5, 20, Masonry.starsteel());
        b.set(39, s + 5, 19, ModBlocks.CRYSTAL_BRICKS.get());
        b.set(39, s + 5, 21, ModBlocks.CRYSTAL_BRICKS.get());
        // Scaffolding up the face, the sifting screen, the spoil
        b.fill(37, s + 1, 17, 37, s + 4, 17, Blocks.SCAFFOLDING);
        b.fill(37, s + 1, 23, 37, s + 3, 23, Blocks.SCAFFOLDING);
        b.set(36, s + 1, 20, Blocks.SPRUCE_FENCE);
        b.set(35, s + 1, 20, Blocks.SPRUCE_FENCE);
        b.fill(35, s + 2, 20, 36, s + 2, 20, Blocks.SPRUCE_TRAPDOOR.defaultBlockState()
                .setValue(net.minecraft.world.level.block.TrapDoorBlock.HALF, net.minecraft.world.level.block.state.properties.Half.TOP));
        for (int[] p : new int[][] {{35, 26}, {36, 26}, {35, 27}, {36, 27}, {35, 25}, {37, 27}}) {
            b.set(p[0], s + 1, p[1], Blocks.GRAVEL);
        }
        b.set(35, s + 2, 26, Blocks.GRAVEL);
        // What the diggers hadn't sifted yet (28): suspicious gravel in the spoil, sand at the foot of the face
        for (int[] p : new int[][] {{35, 1, 26}, {36, 1, 27}, {35, 1, 25}, {38, 1, 19}, {38, 1, 21}}) {
            b.suspicious(p[0], s + p[1], p[2], p[0] == 38 ? Blocks.SUSPICIOUS_SAND : Blocks.SUSPICIOUS_GRAVEL, ModChestLoot.DIG_SYMMES);
        }
        // Survey stakes along the face, lanterns on two
        for (int z = 14; z <= 26; z += 4) {
            b.fill(35, s + 1, z, 35, s + 2, z, Blocks.SPRUCE_FENCE);
            if (z % 8 == 2) {
                b.set(35, s + 3, z, Blocks.LANTERN);
            }
        }
        // The work tent, north: a table of finds, the chest
        smallTent(b, 34, 10);
        b.set(36, s + 1, 12, Blocks.CRAFTING_TABLE);
        b.chest(35, s + 1, 12, Direction.EAST, ModChestLoot.SYMMES_DIG);
        // The path back to the camp
        for (int x = 29; x <= 35; x++) {
            b.set(x, s, 19 + (x % 2), Blocks.COARSE_DIRT);
        }
    }

    /** A canvas A-frame 5 wide (x0..x0+4) and 4 long (z0..z0+3), closed at the north end, a bedroll inside. */
    private static void smallTent(Blueprint b, int x0, int z0) {
        int s = FLOOR;
        for (int z = z0; z <= z0 + 3; z++) {
            b.set(x0, s + 1, z, Blocks.WOOL.white());
            b.set(x0 + 4, s + 1, z, Blocks.WOOL.white());
            b.set(x0 + 1, s + 2, z, Blocks.WOOL.white());
            b.set(x0 + 3, s + 2, z, Blocks.WOOL.white());
            b.set(x0 + 2, s + 3, z, Blocks.WOOL.white());
        }
        b.fill(x0 + 1, s + 1, z0, x0 + 3, s + 1, z0, Blocks.WOOL.white());
        b.set(x0 + 2, s + 2, z0, Blocks.WOOL.white());
        b.fill(x0 + 2, s + 1, z0 + 1, x0 + 2, s + 1, z0 + 2, Blocks.CARPET.brown());
        b.set(x0 + 2, s + 4, z0 + 3, Blocks.SPRUCE_FENCE);
    }

    // ---------------------------------------------------------------- the journal

    /**
     * The Symmes expedition's journal, a written book with its pages from the lang file, saved as an item stack is. Built
     * as NBT by hand: during datagen items have no components bound yet, so an ItemStack can't be made (or encoded) there.
     */
    public static CompoundTag journal() {
        List<Filterable<Component>> pages = new ArrayList<>();
        for (int i = 1; i <= JOURNAL_PAGES; i++) {
            pages.add(Filterable.passThrough(Component.translatable("book.hollowfrontier.symmes_journal." + i)));
        }
        WrittenBookContent content = new WrittenBookContent(Filterable.passThrough("Expedition Journal"), "E. Hartwell", 0, pages, true);
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

    // ---------------------------------------------------------------- shapes

    private static void disc(Blueprint b, int cx, int cz, double radius, int y, BlockState state) {
        int reach = (int) Math.ceil(radius);
        for (int x = cx - reach; x <= cx + reach; x++) {
            for (int z = cz - reach; z <= cz + reach; z++) {
                if (Math.hypot(x - cx, z - cz) <= radius) {
                    b.set(x, y, z, state);
                }
            }
        }
    }

    /** A straight run of blocks from (x0, z0) to (x1, z1) at height y (Bresenham). */
    private static void line(Blueprint b, int x0, int z0, int x1, int z1, int y, BlockState state) {
        int dx = Math.abs(x1 - x0), dz = -Math.abs(z1 - z0), sx = x0 < x1 ? 1 : -1, sz = z0 < z1 ? 1 : -1, err = dx + dz;
        while (true) {
            b.set(x0, y, z0, state);
            if (x0 == x1 && z0 == z1) {
                return;
            }
            int e2 = 2 * err;
            if (e2 >= dz) {
                err += dz;
                x0 += sx;
            }
            if (e2 <= dx) {
                err += dx;
                z0 += sz;
            }
        }
    }

    private static double sq(double v) {
        return v * v;
    }

    private static long key(int x, int z) {
        return ((long) x << 32) | (z & 0xFFFFFFFFL);
    }
}
