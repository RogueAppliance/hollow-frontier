package com.hollowfrontier.content.concavia;

import java.util.ArrayList;
import java.util.List;

import org.jspecify.annotations.Nullable;

import com.hollowfrontier.registry.ModBlocks;

import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.redstone.Orientation;

/**
 * A socket in a Kindled vault's antechamber (26): a carved pedestal, {@link #MARKED} if the mural over it shows a lamp. The
 * vault's {@link ModBlocks#KINDLED_SEAL seal} opens when every marked socket within {@link #REACH} has a phlogiston lamp standing
 * on it and no unmarked one does. Whenever what stands on a socket changes, the sockets round it are checked
 * ({@link #solved}), and if they're right every seal block near them crumbles away.
 * <p>
 * Nothing about the puzzle is stored: the sockets and seals are found round the socket that changed, so it works however
 * the vault was turned when it was placed.
 */
public class SealSocketBlock extends Block {
    public static final BooleanProperty MARKED = BooleanProperty.create("marked");
    /** How far round a socket the others, and the seal, are looked for. */
    public static final int REACH = 8;

    public SealSocketBlock(BlockBehaviour.Properties properties) {
        super(properties);
        registerDefaultState(stateDefinition.any().setValue(MARKED, false));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(MARKED);
    }

    @Override
    protected void neighborChanged(BlockState state, Level level, BlockPos pos, Block block, @Nullable Orientation orientation, boolean movedByPiston) {
        if (level instanceof ServerLevel server && solved(server, pos)) {
            open(server, pos);
        }
    }

    /** Whether the sockets round {@code pos} are all right: a phlogiston lamp on each marked one, none on the others. */
    public static boolean solved(Level level, BlockPos pos) {
        List<BlockPos> sockets = new ArrayList<>();
        for (BlockPos at : BlockPos.betweenClosed(pos.offset(-REACH, -2, -REACH), pos.offset(REACH, 2, REACH))) {
            if (level.getBlockState(at).is(ModBlocks.SEAL_SOCKET.get())) {
                sockets.add(at.immutable());
            }
        }
        if (sockets.isEmpty()) {
            return false;
        }
        for (BlockPos socket : sockets) {
            boolean lamp = level.getBlockState(socket.above()).is(ModBlocks.PHLOGISTON_LAMP.get());
            if (lamp != level.getBlockState(socket).getValue(MARKED)) {
                return false;
            }
        }
        return true;
    }

    /** Every seal block round {@code pos} crumbles away, with a rumble and dust; returns how many went. */
    public static int open(ServerLevel level, BlockPos pos) {
        int opened = 0;
        for (BlockPos at : BlockPos.betweenClosed(pos.offset(-REACH, -2, -REACH), pos.offset(REACH, 6, REACH))) {
            if (level.getBlockState(at).is(ModBlocks.KINDLED_SEAL.get())) {
                level.setBlockAndUpdate(at, Blocks.AIR.defaultBlockState());
                level.sendParticles(ParticleTypes.END_ROD, at.getX() + 0.5, at.getY() + 0.5, at.getZ() + 0.5, 6, 0.3, 0.3, 0.3, 0.02);
                opened++;
            }
        }
        if (opened > 0) {
            level.playSound(null, pos, SoundEvents.BEACON_ACTIVATE, SoundSource.BLOCKS, 1.5F, 0.6F);
            level.playSound(null, pos, SoundEvents.DEEPSLATE_BREAK, SoundSource.BLOCKS, 1.5F, 0.5F);
        }
        return opened;
    }
}
