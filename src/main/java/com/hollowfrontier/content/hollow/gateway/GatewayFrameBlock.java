package com.hollowfrontier.content.hollow.gateway;

import com.hollowfrontier.content.concavia.WaystoneBlock;
import com.hollowfrontier.registry.ModTags;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.phys.BlockHitResult;

/**
 * A stone of a Polar Gateway's ring: twelve of them round a 3x3 well (PolarGateways.RING). Unbreakable like an end portal
 * frame, and only found in gateways (the item is creative-only). ACTIVE while the gateway is open: the runes glow.
 * A phlogiston shard used on the ring of a gateway that's been crossed shows where the woken waystones can take you (15c).
 */
public class GatewayFrameBlock extends Block {
    public static final BooleanProperty ACTIVE = BooleanProperty.create("active");

    public GatewayFrameBlock(Properties properties) {
        super(properties);
        registerDefaultState(stateDefinition.any().setValue(ACTIVE, false));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(ACTIVE);
    }

    @Override
    protected InteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hit) {
        if (!stack.is(ModTags.Items.GEMS_PHLOGISTON)) {
            return InteractionResult.PASS; // the Polar Resonator's own use
        }
        if (level instanceof ServerLevel server && player instanceof ServerPlayer traveller && !WaystoneBlock.fromGateway(server, traveller, pos)) {
            return InteractionResult.PASS;
        }
        return InteractionResult.SUCCESS;
    }
}
