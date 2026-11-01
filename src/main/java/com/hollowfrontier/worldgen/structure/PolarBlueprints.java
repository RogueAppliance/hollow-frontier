package com.hollowfrontier.worldgen.structure;

import java.util.Random;

import com.hollowfrontier.content.hollow.gateway.GatewayFrameBlock;
import com.hollowfrontier.content.hollow.gateway.PolarGateways;
import com.hollowfrontier.registry.ModBlocks;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.CampfireBlock;
import net.minecraft.world.level.block.LanternBlock;
import net.minecraft.world.level.block.SlabBlock;
import net.minecraft.world.level.block.SnowLayerBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.SlabType;

/**
 * The Polar Gateway: an ice crater in frozen country with the gateway's ring of stones lying flat at the bottom, the well
 * dark until a Polar Resonator wakes it. Quartz posts with soul lanterns stand at the ring's corners (Concavian work,
 * phase 11 tells whose), and a frozen expedition's last camp sits on the slope with a chest.
 * <p>
 * 23 x {@link #HEIGHT} x 23, the well's centre at ({@link #CENTER}, {@link #RING_Y}, {@link #CENTER}). The template is
 * sunk {@link #SINK} blocks so the rim sits at ground level (ModStructures); the bowl is cleared up to the top of the
 * template, so it cuts into rising ground too.
 */
public final class PolarBlueprints {
    private PolarBlueprints() {}

    public static final int CENTER = 11;
    public static final int RING_Y = 2;
    public static final int HEIGHT = 20;
    /** How far below the surface the template starts: the rim (floor height 11) ends up a block above the ground. */
    public static final int SINK = 10;
    private static final double RADIUS = 11.5;

    public static Blueprint gateway() {
        Blueprint b = new Blueprint("polar_gateway");
        Random r = new Random(1908); // the year the Hollow Earth was "discovered", for a fixed look
        for (int x = 0; x < 23; x++) {
            for (int z = 0; z < 23; z++) {
                double d = Math.hypot(x - CENTER, z - CENTER);
                if (d > RADIUS) {
                    continue;
                }
                int floor = floor(d);
                for (int y = 0; y < floor; y++) {
                    BlockState ice = r.nextInt(9) == 0 ? Blocks.BLUE_ICE.defaultBlockState() : Blocks.PACKED_ICE.defaultBlockState();
                    b.set(x, y, z, y == floor - 1 && d > 3.5 ? Blocks.SNOW_BLOCK.defaultBlockState() : ice);
                }
                b.air(x, floor, z, x, HEIGHT - 1, z);
                if (d > 4.5 && floor < 11 && r.nextInt(3) == 0) {
                    b.set(x, floor, z, Blocks.SNOW.defaultBlockState().setValue(SnowLayerBlock.LAYERS, 1 + r.nextInt(3)));
                }
            }
        }
        // The floor round the ring: dark, old dressed stone
        for (int x = CENTER - 3; x <= CENTER + 3; x++) {
            for (int z = CENTER - 3; z <= CENTER + 3; z++) {
                if (Math.hypot(x - CENTER, z - CENTER) <= 3.5) {
                    b.set(x, RING_Y - 1, z, (x + z) % 2 == 0 ? Blocks.POLISHED_DEEPSLATE : Blocks.DEEPSLATE_TILES);
                }
            }
        }
        // The ring, dark (the well is left empty: air)
        for (BlockPos offset : PolarGateways.RING) {
            b.set(CENTER + offset.getX(), RING_Y, CENTER + offset.getZ(), ModBlocks.GATEWAY_FRAME.get().defaultBlockState().setValue(GatewayFrameBlock.ACTIVE, false));
        }
        b.fill(CENTER - 1, RING_Y - 1, CENTER - 1, CENTER + 1, RING_Y - 1, CENTER + 1, Blocks.CHISELED_DEEPSLATE);
        // Posts at the ring's corners
        for (int[] c : new int[][] {{-2, -2}, {2, -2}, {-2, 2}, {2, 2}}) {
            int x = CENTER + c[0], z = CENTER + c[1];
            b.fill(x, RING_Y, z, x, RING_Y + 2, z, Blocks.QUARTZ_PILLAR);
            b.set(x, RING_Y + 3, z, Blocks.CHISELED_QUARTZ_BLOCK);
            b.set(x, RING_Y + 4, z, Blocks.SOUL_LANTERN.defaultBlockState().setValue(LanternBlock.HANGING, false));
        }
        // Ice spikes round the rim
        for (int i = 0; i < 7; i++) {
            double angle = i * (Math.PI * 2 / 7) + 0.3;
            int x = CENTER + (int) Math.round(Math.cos(angle) * 10);
            int z = CENTER + (int) Math.round(Math.sin(angle) * 10);
            int base = floor(Math.hypot(x - CENTER, z - CENTER));
            b.fill(x, base, z, x, base + 1 + r.nextInt(4), z, Blocks.PACKED_ICE);
        }
        // The expedition's last camp, on the south slope
        camp(b, CENTER, CENTER + 8);
        return b;
    }

    /** Floor height at distance d from the centre: flat round the ring, then a slope up to the rim. */
    static int floor(double d) {
        return d <= 3.5 ? RING_Y : Math.min(11, RING_Y + (int) Math.round((d - 3.5) * 1.3));
    }

    private static int floorAt(int x, int z) {
        return floor(Math.hypot(x - CENTER, z - CENTER));
    }

    private static void camp(Blueprint b, int x, int z) {
        int y = floorAt(x, z);
        b.chest(x, y, z, Direction.NORTH, ModChestLoot.POLAR_GATEWAY);
        int by = floorAt(x + 1, z);
        b.set(x + 1, by, z, Blocks.BARREL);
        b.set(x + 1, by + 1, z, Blocks.LANTERN);
        int fy = floorAt(x - 2, z - 1);
        b.set(x - 2, fy, z - 1, Blocks.CAMPFIRE.defaultBlockState().setValue(CampfireBlock.LIT, false));
        // An abandoned sled: two spruce runners
        int sy = floorAt(x + 3, z - 1);
        b.set(x + 3, sy, z - 1, Blocks.SPRUCE_SLAB.defaultBlockState().setValue(SlabBlock.TYPE, SlabType.BOTTOM));
        b.set(x + 3, floorAt(x + 3, z - 2), z - 2, Blocks.SPRUCE_SLAB.defaultBlockState().setValue(SlabBlock.TYPE, SlabType.BOTTOM));
    }
}
