package com.hollowfrontier.content.hollow;

import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;

/**
 * The Hollow Earth's crust: solid stone to walk on and look at, but sky light passes straight through it, as through glass.
 * That's what lets the land under a world-wide ceiling lie in full daylight (sky light comes down from the top of the world
 * and any ordinary stone would stop it). Shell bedrock, the crust's unbreakable cap, is the same block class.
 */
public class CrustRockBlock extends Block {
    public CrustRockBlock(Properties properties) {
        super(properties);
    }

    @Override
    protected int getLightDampening(BlockState state) {
        return 0;
    }

    @Override
    protected boolean propagatesSkylightDown(BlockState state) {
        return true;
    }
}
