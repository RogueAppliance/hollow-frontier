package com.hollowfrontier.worldgen.structure;

import java.util.List;
import java.util.Random;

import com.hollowfrontier.registry.ModBlocks;
import com.hollowfrontier.registry.ModEntities;

import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.CampfireBlock;
import net.minecraft.world.level.block.state.BlockState;

/**
 * The Grays' wrecks (13a), one piece each with its floor ({@link HollowBlueprints#FLOOR}) on the ground: the same crash
 * site stands in the Overworld (a rigid jigsaw piece on the surface) and in the Hollow Earth (a {@link HollowSiteStructure}).
 * <ul>
 * <li>A <b>crash site</b>, 21 x 46: a scorched furrow some thirty blocks long, widening as it goes, its earth thrown up on
 *     either side and still smouldering, hull plates strewn along it; and at its end the saucer, fifteen across, nose down
 *     and half buried, a rim of phlogiston lights (some dead), its glass dome stove in, a breach torn in its flank. Salvage is in
 *     its wreckage ({@link ModBlocks#SAUCER_WRECKAGE}): two blocks in the cockpit under the dome, one at the breach, two
 *     thrown clear along the furrow. A sealed cargo pod ({@link ModBlocks#GRAY_CARGO_POD}, 32c) stands in the cockpit. Two Gray
 *     Wardens keep it.</li>
 * </ul>
 */
public final class UfoBlueprints {
    private UfoBlueprints() {}

    public static final String CRASH_SITE = "crash_site";
    /** The saucer's middle, and its radius. */
    public static final int SAUCER_X = 10, SAUCER_Z = 36;
    public static final double SAUCER_R = 7.5;
    /** How far its nose is pitched down into the ground. */
    private static final double TILT = Math.toRadians(20);

    public static List<Blueprint> all() {
        return List.of(crashSite());
    }

    static Blueprint crashSite() {
        Blueprint b = new Blueprint(CRASH_SITE);
        Random r = new Random(1947);
        int s = HollowBlueprints.FLOOR, c = SAUCER_X;
        b.air(0, s + 1, 0, 20, s + 14, 45);
        // The furrow: scorched earth down the middle, thrown up in berms either side, smouldering here and there
        for (int z = 0; z <= 30; z++) {
            int half = 1 + z / 10;
            for (int dx = -half - 2; dx <= half + 2; dx++) {
                int x = c + dx;
                if (Math.abs(dx) <= half) {
                    b.set(x, s, z, scorched(r));
                } else if (r.nextInt(3) != 0) {
                    b.set(x, s + 1, z, r.nextInt(3) == 0 ? Blocks.COARSE_DIRT : Blocks.DIRT);
                    if (Math.abs(dx) == half + 1 && z > 12 && r.nextInt(2) == 0) {
                        b.set(x, s + 2, z, Blocks.COARSE_DIRT);
                    }
                }
            }
        }
        for (int z : new int[] {6, 15, 24}) {
            b.set(c + (z % 2 == 0 ? 1 : -1), s, z, Blocks.CAMPFIRE.defaultBlockState().setValue(CampfireBlock.LIT, true));
        }
        // Plates torn off on the way in
        for (int i = 0; i < 7; i++) {
            int z = 4 + r.nextInt(24), x = c + r.nextInt(5) - 2;
            b.set(x, s + 1, z, r.nextInt(3) == 0 ? Blocks.IRON_BARS.defaultBlockState() : ModBlocks.SAUCER_HULL.get().defaultBlockState());
        }
        saucer(b, r, s);
        // Salvage: in the cockpit, at the breach, and thrown clear along the furrow
        BlockState wreckage = ModBlocks.SAUCER_WRECKAGE.get().defaultBlockState();
        b.set(c - 1, s + 3, SAUCER_Z, wreckage);
        b.set(c + 1, s + 3, SAUCER_Z + 1, wreckage);
        b.set(c + 6, s + 3, SAUCER_Z + 1, wreckage);
        b.set(c - 2, s + 1, 20, wreckage);
        b.set(c + 2, s + 1, 27, wreckage);
        // The cargo pod (32c), sealed, beside the pilot's seat
        b.set(c + 1, s + 3, SAUCER_Z - 1, ModBlocks.GRAY_CARGO_POD.get().defaultBlockState());
        // Its keepers
        b.entity(ModEntities.GRAY_WARDEN.get(), c - 4, s + 1, 25);
        b.entity(ModEntities.GRAY_WARDEN.get(), c + 5, s + 1, 22);
        return b;
    }

    /**
     * The saucer, a disc thick in the middle and thin at the rim, pitched {@link #TILT} nose-down toward the furrow (what
     * would be below the floor is in the ground), with its dome, cockpit, breach and the earth heaped against its nose.
     */
    private static void saucer(Blueprint b, Random r, int s) {
        double cy = s + 3, cos = Math.cos(TILT), sin = Math.sin(TILT);
        BlockState hull = ModBlocks.SAUCER_HULL.get().defaultBlockState();
        for (int x = 1; x <= 19; x++) {
            for (int y = s; y <= s + 10; y++) {
                for (int z = 26; z <= 45; z++) {
                    double dx = x - SAUCER_X, dy = y - cy, dz = z - SAUCER_Z;
                    double ly = dy * cos - dz * sin, lz = dz * cos + dy * sin;
                    double rad = Math.hypot(dx, lz);
                    if (rad > SAUCER_R + 0.3) {
                        continue;
                    }
                    double t = 0.6 + 1.6 * (1 - Math.pow(Math.min(1, rad / SAUCER_R), 2));
                    boolean cockpit = rad <= 2.8 && ly > -0.6 && ly <= t + 1.8;
                    boolean breach = dx >= 3.5 && Math.abs(lz) <= 1.6 && Math.abs(ly) <= 1.2;
                    if (cockpit || breach) {
                        b.set(x, y, z, Blocks.AIR);
                        continue;
                    }
                    if (Math.abs(ly) <= t) {
                        boolean rim = rad > SAUCER_R - 1.0 && Math.abs(ly) <= 0.6;
                        if (rim) {
                            double angle = Math.atan2(lz, dx);
                            boolean light = ((int) Math.round(angle / (Math.PI / 8)) & 1) == 0;
                            b.set(x, y, z, light ? (r.nextInt(4) == 0 ? Blocks.TINTED_GLASS.defaultBlockState() : ModBlocks.PHLOGISTON_LAMP.get().defaultBlockState()) : hull);
                        } else {
                            b.set(x, y, z, hull);
                        }
                    } else if (rad <= 3.3 && ly > t && ly <= t + 2.4 * Math.sqrt(Math.max(0, 1 - Math.pow(rad / 3.3, 2)))) {
                        // The dome over the cockpit, stove in
                        b.set(x, y, z, r.nextInt(3) == 0 ? Blocks.AIR.defaultBlockState() : Blocks.TINTED_GLASS.defaultBlockState());
                    }
                }
            }
        }
        // The cockpit's floor and the pilot's seat (empty)
        b.fill(SAUCER_X - 2, s + 2, SAUCER_Z - 1, SAUCER_X + 2, s + 2, SAUCER_Z + 2, hull);
        b.set(SAUCER_X, s + 3, SAUCER_Z + 2, Blocks.SMOOTH_QUARTZ_STAIRS);
        // Earth heaped against its buried nose
        for (int x = 1; x <= 19; x++) {
            for (int z = 24; z <= 32; z++) {
                double rad = Math.hypot(x - SAUCER_X, z - SAUCER_Z);
                if (rad > SAUCER_R - 0.5 && rad <= SAUCER_R + 2.5 && z < SAUCER_Z - 3) {
                    b.set(x, s + 1, z, r.nextInt(3) == 0 ? Blocks.COARSE_DIRT : Blocks.DIRT);
                    if (rad <= SAUCER_R + 1.2) {
                        b.set(x, s + 2, z, Blocks.COARSE_DIRT);
                    }
                }
            }
        }
    }

    private static Block scorched(Random r) {
        return switch (r.nextInt(5)) {
            case 0 -> Blocks.BASALT;
            case 1 -> Blocks.BLACKSTONE;
            case 2 -> Blocks.DYED_TERRACOTTA.black();
            default -> Blocks.COARSE_DIRT;
        };
    }
}
