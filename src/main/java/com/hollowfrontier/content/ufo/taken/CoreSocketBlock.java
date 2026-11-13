package com.hollowfrontier.content.ufo.taken;

import com.hollowfrontier.content.frontier.bounty.BoardUse;
import com.hollowfrontier.registry.ModItems;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
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
 * A socket in the ship's core (50), with one of the three Phlogiston Cells the engine runs on seated in it ({@link #CELL}).
 * Using it pulls the cell: the player keeps it, the socket goes dark, and the ship is told ({@link Ship#cellPulled}). With
 * all three out there's nothing holding the ship up. Away from a ship a cell still comes out; nothing else happens.
 */
public class CoreSocketBlock extends Block {
    public static final BooleanProperty CELL = BooleanProperty.create("cell");

    public CoreSocketBlock(Properties properties) {
        super(properties);
        registerDefaultState(stateDefinition.any().setValue(CELL, true));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(CELL);
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        if (level instanceof ServerLevel server && player instanceof ServerPlayer serverPlayer) {
            if (!state.getValue(CELL)) {
                player.sendOverlayMessage(Component.translatable(TakenText.SOCKET_EMPTY).withStyle(ChatFormatting.GRAY));
            } else {
                pull(server, serverPlayer, pos, state);
            }
        }
        return InteractionResult.SUCCESS;
    }

    /** The cell comes out into the player's hands. */
    public static void pull(ServerLevel level, ServerPlayer player, BlockPos pos, BlockState state) {
        level.setBlock(pos, state.setValue(CELL, false), Block.UPDATE_ALL);
        BoardUse.give(player, new ItemStack(ModItems.PHLOGISTON_CELL.get()));
        level.sendParticles(ParticleTypes.ELECTRIC_SPARK, pos.getX() + 0.5, pos.getY() + 1.0, pos.getZ() + 0.5, 16, 0.3, 0.3, 0.3, 0.1);
        level.playSound(null, pos, SoundEvents.BEACON_DEACTIVATE, SoundSource.BLOCKS, 1.5F, 0.7F);
        level.playSound(null, pos, SoundEvents.RESPAWN_ANCHOR_DEPLETE.value(), SoundSource.BLOCKS, 1.0F, 0.8F);
        Ship ship = Taken.shipAt(level, pos);
        if (ship != null) {
            ship.cellPulled(level, player);
        }
    }
}
