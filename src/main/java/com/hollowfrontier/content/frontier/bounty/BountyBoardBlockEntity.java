package com.hollowfrontier.content.frontier.bounty;

import java.util.Optional;

import org.jspecify.annotations.Nullable;

import com.hollowfrontier.registry.ModBlockEntities;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;

/**
 * The Small Bounty Board's face (18): the list it shows ({@link BountyBoards}), and a copy of what's on it ({@link BoardFace})
 * for clients to draw, four posters in a 2x2 grid on every side with open air in front. It looks again every
 * {@link #CHECK_TICKS} and sends itself out only when something changed.
 */
public class BountyBoardBlockEntity extends BlockEntity implements PostingBoard {
    public static final int CHECK_TICKS = 40;

    private String key = "";
    private BoardFace face = BoardFace.EMPTY;

    public BountyBoardBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.BOUNTY_BOARD.get(), pos, state);
    }

    @Override
    public BoardFace face() {
        return face;
    }

    @Override
    public String key(ServerLevel level) {
        if (key.isEmpty()) {
            key = BountyBoards.keyFor(level, worldPosition);
            setChanged();
        }
        return key;
    }

    /** For tests: shares another board's list. */
    public void setKey(String key) {
        this.key = key;
        setChanged();
    }

    @Override
    public BlockPos origin() {
        return worldPosition;
    }

    @Override
    public int columns() {
        return 2;
    }

    @Override
    public int rows() {
        return 2;
    }

    public static void serverTick(Level level, BlockPos pos, BlockState state, BountyBoardBlockEntity board) {
        if (level instanceof ServerLevel server && (level.getGameTime() + pos.asLong()) % CHECK_TICKS == 0) {
            board.sync(server);
        }
    }

    @Override
    public void sync(ServerLevel level) {
        BoardFace now = BoardFace.of(level, key(level), worldPosition);
        if (!now.equals(face)) {
            face = now;
            setChanged();
            level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), Block.UPDATE_CLIENTS);
        }
    }

    /**
     * Where on a side's face (0..1 across as you look at it, 0..1 down) a hit landed; empty for the top and bottom. The middle
     * of the face counts as the top row.
     */
    public static Optional<float[]> faceSpot(BlockHitResult hit) {
        Direction side = hit.getDirection();
        if (side.getAxis() == Direction.Axis.Y) {
            return Optional.empty();
        }
        BlockPos pos = hit.getBlockPos();
        Vec3 local = hit.getLocation().subtract(pos.getX() + 0.5, pos.getY(), pos.getZ() + 0.5);
        Direction right = side.getCounterClockWise();
        float u = (float) (local.x * right.getStepX() + local.z * right.getStepZ()) + 0.5F;
        float v = 1.0F - (float) local.y;
        return Optional.of(new float[] {Math.min(1.0F, Math.max(0.0F, u)), Math.min(1.0F, Math.max(0.0F, v - 0.0001F))});
    }

    // ---------------------------------------------------------------- saving and sending

    @Override
    protected void saveAdditional(ValueOutput output) {
        super.saveAdditional(output);
        output.putString("list", key);
        output.store(BoardFace.MAP_CODEC, face);
    }

    @Override
    protected void loadAdditional(ValueInput input) {
        super.loadAdditional(input);
        key = input.getStringOr("list", "");
        face = input.read(BoardFace.MAP_CODEC).orElse(BoardFace.EMPTY);
    }

    @Override
    public CompoundTag getUpdateTag(HolderLookup.Provider registries) {
        return saveCustomOnly(registries);
    }

    @Override
    public @Nullable Packet<ClientGamePacketListener> getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }
}
