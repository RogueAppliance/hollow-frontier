package com.hollowfrontier.content.ufo;

import com.hollowfrontier.worldgen.structure.ModChestLoot;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.level.storage.loot.parameters.LootContextParamSets;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;

/**
 * A Gray Cargo Pod (32c): a sealed alloy locker in a crash site's cockpit. Using it breaks the seal with a hiss and it
 * spills what the Grays were carrying ({@link ModChestLoot#CRASH_SITE}: salvage and the odd gadget the prospector buys),
 * and stays open and empty. Sealed, it can't be broken (nobody has found where to get a pry bar in); open, it's scrap
 * that comes away like iron and leaves nothing.
 */
public class CargoPodBlock extends Block {
    public static final BooleanProperty OPEN = BlockStateProperties.OPEN;

    public CargoPodBlock(Properties properties) {
        super(properties);
        registerDefaultState(stateDefinition.any().setValue(OPEN, false));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(OPEN);
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        if (level instanceof ServerLevel server) {
            if (state.getValue(OPEN)) {
                player.sendOverlayMessage(Component.translatable("message.hollowfrontier.cargo_pod.empty").withStyle(ChatFormatting.GRAY));
            } else {
                open(server, pos, player);
            }
        }
        return InteractionResult.SUCCESS;
    }

    /** Breaks the seal: what's inside comes out of the top, and the pod stays, open and empty. */
    public static void open(ServerLevel level, BlockPos pos, Player player) {
        LootParams params = new LootParams.Builder(level)
                .withParameter(LootContextParams.ORIGIN, Vec3.atCenterOf(pos))
                .withParameter(LootContextParams.THIS_ENTITY, player)
                .withLuck(player.getLuck())
                .create(LootContextParamSets.CHEST);
        for (ItemStack stack : level.getServer().reloadableRegistries().getLootTable(ModChestLoot.CRASH_SITE).getRandomItems(params)) {
            Block.popResourceFromFace(level, pos, Direction.UP, stack);
        }
        level.setBlock(pos, level.getBlockState(pos).setValue(OPEN, true), Block.UPDATE_ALL);
        level.sendParticles(ParticleTypes.CLOUD, pos.getX() + 0.5, pos.getY() + 1.0, pos.getZ() + 0.5, 8, 0.25, 0.1, 0.25, 0.02);
        level.playSound(null, pos, SoundEvents.FIRE_EXTINGUISH, SoundSource.BLOCKS, 0.8F, 1.4F);
        level.playSound(null, pos, SoundEvents.IRON_TRAPDOOR_OPEN, SoundSource.BLOCKS, 1.0F, 0.7F);
        player.sendOverlayMessage(Component.translatable("message.hollowfrontier.cargo_pod.opened").withStyle(ChatFormatting.GRAY));
    }

    // Sealed, it won't come away at all; open, it breaks like iron
    @Override
    protected float getDestroyProgress(BlockState state, Player player, BlockGetter level, BlockPos pos) {
        return state.getValue(OPEN) ? super.getDestroyProgress(state, player, level, pos) : 0.0F;
    }
}
