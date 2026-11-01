package com.hollowfrontier.worldgen.structure;

import java.util.List;
import java.util.stream.Stream;

import com.hollowfrontier.content.frontier.music.SaloonPianoBlock;
import com.hollowfrontier.registry.ModBlocks;
import com.hollowfrontier.registry.ModEntities;
import com.hollowfrontier.registry.ModItems;
import net.minecraft.world.item.Items;

import net.minecraft.core.Direction;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.CampfireBlock;
import net.minecraft.world.level.block.LanternBlock;
import net.minecraft.world.level.block.RotatedPillarBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.DoorHingeSide;

/**
 * Wild West structures as code. Called during datagen only (blocks/entities are registered by then).
 * Saloon fronts face north (-z). The frontier town is built from jigsaw pieces in TownBlueprints (which reuses the saloon).
 */
public final class FrontierBlueprints {
    private FrontierBlueprints() {}

    public static List<Blueprint> all() {
        return Stream.concat(Stream.of(saloon(), outlawCamp(), PolarBlueprints.gateway()), TownBlueprints.all().stream()).toList();
    }

    private static BlockState s(Block block) {
        return block.defaultBlockState();
    }

    private static BlockState hangingLantern() {
        return Blocks.LANTERN.defaultBlockState().setValue(LanternBlock.HANGING, true);
    }

    // 13 x 10 x 13. Porch z 0..2, hall z 3..12, false front up to y 9.
    public static Blueprint saloon() {
        Blueprint b = new Blueprint("saloon");
        BlockState wall = s(Blocks.SPRUCE_PLANKS);
        BlockState post = s(Blocks.STRIPPED_SPRUCE_LOG);

        b.air(0, 1, 0, 12, 9, 12);
        b.fill(0, 0, 0, 12, 0, 12, Blocks.SPRUCE_PLANKS);
        b.fill(0, 0, 0, 12, 0, 2, Blocks.OAK_PLANKS);

        // Hall
        b.walls(0, 1, 3, 12, 5, 12, wall);
        for (int[] c : new int[][] {{0, 3}, {12, 3}, {0, 12}, {12, 12}}) {
            b.fill(c[0], 1, c[1], c[0], 5, c[1], post);
        }
        b.fill(0, 6, 3, 12, 6, 12, Blocks.DARK_OAK_PLANKS);
        // False front facade
        b.fill(1, 6, 3, 11, 8, 3, wall);
        b.fill(0, 9, 3, 12, 9, 3, Blocks.DARK_OAK_SLAB);
        b.fill(4, 7, 2, 8, 7, 2, Blocks.DARK_OAK_TRAPDOOR);
        // Doors and windows
        b.door(5, 1, 3, ModBlocks.SALOON_DOORS.get(), Direction.NORTH, DoorHingeSide.LEFT);
        b.door(6, 1, 3, ModBlocks.SALOON_DOORS.get(), Direction.NORTH, DoorHingeSide.RIGHT);
        b.fill(2, 2, 3, 3, 3, 3, Blocks.GLASS_PANE);
        b.fill(9, 2, 3, 10, 3, 3, Blocks.GLASS_PANE);
        b.fill(0, 2, 6, 0, 3, 8, Blocks.GLASS_PANE);
        b.fill(12, 2, 6, 12, 3, 8, Blocks.GLASS_PANE);

        // Porch
        for (int x : new int[] {0, 4, 8, 12}) {
            b.fill(x, 1, 0, x, 3, 0, Blocks.SPRUCE_FENCE);
        }
        b.fill(0, 4, 0, 12, 4, 2, Blocks.SPRUCE_SLAB);
        b.set(1, 1, 0, Blocks.HAY_BLOCK);
        b.set(11, 1, 1, Blocks.BARREL);

        // Bar: counter along z 9, shelves of barrels at the back wall
        b.fill(2, 1, 9, 9, 1, 9, ModBlocks.BAR_COUNTER.get());
        b.fill(1, 1, 11, 9, 2, 11, Blocks.BARREL);
        b.chest(10, 1, 11, Direction.NORTH, ModChestLoot.SALOON);
        for (int x : new int[] {3, 5, 7}) {
            b.set(x, 1, 8, ModBlocks.BAR_STOOL.get());
        }
        // Tables: a card table on the right, in reach of the barmaid who deals (17)
        for (int[] t : new int[][] {{3, 5}, {9, 5}}) {
            if (t[0] == 9) {
                b.set(t[0], 1, t[1], ModBlocks.CARD_TABLE.get());
            } else {
                b.set(t[0], 1, t[1], ModBlocks.DARK_OAK_TABLE.get());
            }
            b.set(t[0] - 1, 1, t[1], ModBlocks.BAR_STOOL.get());
            b.set(t[0] + 1, 1, t[1], ModBlocks.BAR_STOOL.get());
        }
        b.set(3, 5, 6, hangingLantern());
        b.set(9, 5, 6, hangingLantern());
        b.set(6, 5, 10, hangingLantern());
        b.bountyBoard(11, 1, 4, Direction.WEST, 2); // on the east wall, between the porch and the window
        b.set(11, 1, 8, ModBlocks.SALOON_PIANO.get().defaultBlockState().setValue(SaloonPianoBlock.FACING, Direction.WEST)); // against the east wall

        // Back bar dressing: a still on the barrels, bottles on shelves between, lanterns at each end of the counter
        b.set(3, 3, 11, Blocks.BREWING_STAND);
        b.set(8, 3, 11, Blocks.BREWING_STAND);
        backBar(b, 11, new int[] {1, 2, 4, 5, 6, 7, 9});
        // Its name over the swinging doors
        b.wallSign(5, 3, 2, Blocks.DARK_OAK_WALL_SIGN, Direction.NORTH, net.minecraft.world.item.DyeColor.WHITE, false, "",
                TownDecor.signKey("saloon"), TownDecor.signKey("saloon") + ".sub");
        b.wallSign(6, 3, 2, Blocks.DARK_OAK_WALL_SIGN, Direction.NORTH, net.minecraft.world.item.DyeColor.WHITE, false, "",
                TownDecor.signKey("saloon_cards"), TownDecor.signKey("saloon_cards") + ".sub");
        b.set(2, 2, 9, Blocks.LANTERN);
        b.set(9, 2, 9, Blocks.LANTERN);

        b.entity(ModEntities.BARMAID.get(), 5, 1, 10);
        b.cat(3, 2, 9, "saloon");   // the saloon cat, up on the bar
        b.cat(1, 2, 0, "porch");    // sometimes a second one, on the hay bale out front
        return b;
    }

    /** The bottles behind a bar: a shelf of the house's drinks (three of a kind each) at each x, on the barrels along z at y 3. */
    static void backBar(Blueprint b, int z, int[] xs) {
        net.minecraft.world.item.Item[] drinks = {ModItems.RYE_WHISKEY.get(), ModItems.FRONTIER_BEER.get(), ModItems.MEZCAL.get(),
                ModItems.SARSAPARILLA.get(), ModItems.RYE_WHISKEY.get(), ModItems.MOONSHINE.get(), Items.GLASS_BOTTLE, ModItems.FRONTIER_BEER.get()};
        for (int i = 0; i < xs.length; i++) {
            net.minecraft.world.item.Item drink = drinks[i % drinks.length];
            b.shelf(xs[i], 3, z, Blocks.DARK_OAK_SHELF, Direction.NORTH, drink, drink, drink);
        }
    }

    // 15 x 5 x 15. Three wool tents around a campfire, loot chest, gang of three.
    public static Blueprint outlawCamp() {
        Blueprint b = new Blueprint("outlaw_camp");
        b.air(0, 1, 0, 14, 4, 14);
        b.fill(0, 0, 0, 14, 0, 14, Blocks.COARSE_DIRT);
        b.fill(4, 0, 4, 10, 0, 10, Blocks.DIRT);
        b.set(7, 0, 7, Blocks.COBBLESTONE);
        b.set(7, 1, 7, Blocks.CAMPFIRE.defaultBlockState().setValue(CampfireBlock.LIT, true));

        tent(b, 1, 1, Blocks.WOOL.red());
        tent(b, 11, 1, Blocks.WOOL.brown());
        tent(b, 1, 10, Blocks.WOOL.brown());

        BlockState log = Blocks.STRIPPED_OAK_LOG.defaultBlockState().setValue(RotatedPillarBlock.AXIS, Direction.Axis.X);
        b.fill(6, 1, 5, 8, 1, 5, log);
        b.fill(6, 1, 9, 8, 1, 9, log);
        b.chest(12, 1, 12, Direction.WEST, ModChestLoot.OUTLAW_CAMP);
        b.set(12, 1, 10, Blocks.BARREL);
        b.set(11, 1, 12, Blocks.HAY_BLOCK);
        b.fill(13, 1, 6, 13, 1, 9, Blocks.OAK_FENCE);

        b.entity(ModEntities.OUTLAW.get(), 5, 1, 7);
        b.entity(ModEntities.OUTLAW.get(), 9, 1, 7);
        b.entity(ModEntities.OUTLAW_GUNSLINGER.get(), 7, 1, 11);
        return b;
    }

    // 3 x 2 x 4 A-frame, open at both ends.
    private static void tent(Blueprint b, int x, int z, Block wool) {
        b.fill(x, 1, z, x, 1, z + 3, wool);
        b.fill(x + 2, 1, z, x + 2, 1, z + 3, wool);
        b.fill(x + 1, 2, z, x + 1, 2, z + 3, wool);
    }
}
