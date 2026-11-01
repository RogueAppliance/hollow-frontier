package com.hollowfrontier.content.frontier.furniture;

import org.jspecify.annotations.Nullable;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.LanternBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;

/**
 * A town lamp (24c): a lantern the lamplighter sees to. It's dark by day and lit by night, and changes over on a random
 * tick, so a town's lamps come on one by one through the dusk and go out one by one in the morning. The towns' street
 * lamps, porch lamps and the welcome arches' lamps are these. Light 15 when lit, as a lantern; none when not.
 */
public class TownLampBlock extends LanternBlock {
    public static final BooleanProperty LIT = BlockStateProperties.LIT;

    public TownLampBlock(Properties properties) {
        super(properties);
        registerDefaultState(defaultBlockState().setValue(LIT, false));
    }

    /** Light by state: a lantern's when lit. */
    public static int light(BlockState state) {
        return state.getValue(LIT) ? 15 : 0;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        super.createBlockStateDefinition(builder);
        builder.add(LIT);
    }

    @Override
    public @Nullable BlockState getStateForPlacement(BlockPlaceContext context) {
        BlockState state = super.getStateForPlacement(context);
        return state == null ? null : state.setValue(LIT, shouldBeLit(context.getLevel()));
    }

    /** Lit after dusk, in a world with a day (the Hollow Earth's sun never sets, so they stay dark there). */
    public static boolean shouldBeLit(Level level) {
        return level.isDarkOutside();
    }

    @Override
    protected boolean isRandomlyTicking(BlockState state) {
        return true;
    }

    @Override
    protected void randomTick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
        boolean lit = shouldBeLit(level);
        if (state.getValue(LIT) != lit) {
            level.setBlock(pos, state.setValue(LIT, lit), Block.UPDATE_ALL);
        }
    }
}
