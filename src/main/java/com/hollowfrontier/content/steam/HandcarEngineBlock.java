package com.hollowfrontier.content.steam;

import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BooleanProperty;

/**
 * The little upright boiler and smokestack a {@link SteamHandcar} carries. Only ever shown as the cart's display block (the
 * way a furnace minecart shows a furnace); it has no item and is never placed. LIT when the handcar has steam up.
 */
public class HandcarEngineBlock extends Block {
    public static final BooleanProperty LIT = BooleanProperty.create("lit");

    public HandcarEngineBlock(Properties properties) {
        super(properties);
        registerDefaultState(stateDefinition.any().setValue(LIT, false));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(LIT);
    }
}
