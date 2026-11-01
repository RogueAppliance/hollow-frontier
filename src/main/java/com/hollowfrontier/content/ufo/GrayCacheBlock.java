package com.hollowfrontier.content.ufo;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;

/**
 * A Gray Landing's salvage cache (15a): an alloy crate the Grays set down by their saucer. It can't be broken or blown up.
 * While its saucer is down beside it (until every guard is down) it stays sealed; after that, using it spills out what's inside
 * ({@link GrayLandings#open}) and it's gone. Left at dawn, the saucer takes it back aboard.
 */
public class GrayCacheBlock extends Block {
    public GrayCacheBlock(Properties properties) {
        super(properties);
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        if (level instanceof ServerLevel server) {
            if (GrayLandings.sealed(server, pos)) {
                player.sendOverlayMessage(Component.translatable("message.hollowfrontier.landing.sealed").withStyle(ChatFormatting.GRAY));
                level.playSound(null, pos, SoundEvents.IRON_TRAPDOOR_CLOSE, SoundSource.BLOCKS, 0.8F, 0.5F);
            } else {
                GrayLandings.open(server, pos, player);
            }
        }
        return InteractionResult.SUCCESS;
    }
}
