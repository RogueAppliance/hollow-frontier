package com.hollowfrontier.content.frontier.bounty;

import java.util.Optional;

import org.jspecify.annotations.Nullable;

import com.hollowfrontier.content.hollow.gray.Captives;
import com.hollowfrontier.registry.ModDataComponents;
import com.hollowfrontier.registry.ModDataSerializers;
import com.hollowfrontier.registry.ModEntities;
import com.hollowfrontier.registry.ModItems;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundAddEntityPacket;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerEntity;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.decoration.HangingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.gamerules.GameRules;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

/**
 * The Bounty Board (18): a cork board in a wooden frame, hung on a wall like a painting, two blocks high and three across
 * where there's room (six posters), or two across (four). One poster to a block. It shows the same lists as the
 * one-block Small Bounty Board ({@link BountyBoards}; a town's boards all share one), and is used the same way
 * ({@link BoardUse}): click a poster to take it.
 * <p>
 * Like a painting, {@code pos} is the block in front of the wall at the bottom left (as you face it) of an even width, or
 * at the bottom middle of an odd one.
 */
public class BountyBoardEntity extends HangingEntity implements PostingBoard {
    public static final int ROWS = 2, WIDE = 3, NARROW = 2;
    public static final int CHECK_TICKS = 40;
    public static final double DEPTH = 0.0625;
    private static final EntityDataAccessor<Integer> DATA_COLUMNS = SynchedEntityData.defineId(BountyBoardEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<BoardFace> DATA_FACE = SynchedEntityData.defineId(BountyBoardEntity.class, ModDataSerializers.BOARD_FACE.get());

    private String key = "";

    public BountyBoardEntity(EntityType<? extends BountyBoardEntity> type, Level level) {
        super(type, level);
    }

    private BountyBoardEntity(Level level, BlockPos pos) {
        super(ModEntities.HANGING_BOUNTY_BOARD.get(), level, pos);
    }

    /** A board hung from pos facing out that way: three across if it fits there, else two; empty if neither does. */
    public static Optional<BountyBoardEntity> create(Level level, BlockPos pos, Direction facing) {
        BountyBoardEntity board = new BountyBoardEntity(level, pos);
        board.setDirection(facing);
        for (int columns : new int[] {WIDE, NARROW}) {
            board.setColumns(columns);
            if (board.survives()) {
                return Optional.of(board);
            }
        }
        return Optional.empty();
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
        builder.define(DATA_COLUMNS, NARROW);
        builder.define(DATA_FACE, BoardFace.EMPTY);
    }

    @Override
    public void onSyncedDataUpdated(EntityDataAccessor<?> accessor) {
        super.onSyncedDataUpdated(accessor);
        if (DATA_COLUMNS.equals(accessor)) {
            recalculateBoundingBox();
        }
    }

    public void setColumns(int columns) {
        entityData.set(DATA_COLUMNS, columns == WIDE ? WIDE : NARROW);
        recalculateBoundingBox();
    }

    @Override
    public int columns() {
        return entityData.get(DATA_COLUMNS);
    }

    @Override
    public int rows() {
        return ROWS;
    }

    @Override
    public BoardFace face() {
        return entityData.get(DATA_FACE);
    }

    @Override
    public String key(ServerLevel level) {
        if (key.isEmpty()) {
            key = BountyBoards.keyFor(level, pos);
        }
        return key;
    }

    /** For tests: shares another board's list. */
    public void setKey(String key) {
        this.key = key;
    }

    @Override
    public BlockPos origin() {
        return pos;
    }

    @Override
    protected AABB calculateBoundingBox(BlockPos pos, Direction direction) {
        Vec3 onWall = Vec3.atCenterOf(pos).relative(direction, -0.46875);
        Direction right = direction.getCounterClockWise();
        int columns = columns();
        Vec3 centre = onWall.relative(right, columns % 2 == 0 ? 0.5 : 0.0).relative(Direction.UP, 0.5);
        Direction.Axis axis = direction.getAxis();
        return AABB.ofSize(centre, axis == Direction.Axis.X ? DEPTH : columns, ROWS, axis == Direction.Axis.Z ? DEPTH : columns);
    }

    // ---------------------------------------------------------------- keeping it current, and using it

    @Override
    public void tick() {
        super.tick();
        if (level() instanceof ServerLevel level && (tickCount + getId()) % CHECK_TICKS == 0 && !isRemoved()) {
            sync(level);
        }
    }

    @Override
    public void sync(ServerLevel level) {
        BoardFace now = BoardFace.of(level, key(level), pos);
        if (!now.equals(face())) {
            entityData.set(DATA_FACE, now);
        }
    }

    /** Where on the face (0..1 across as you look at it, 0..1 down) a spot relative to the board's middle falls. */
    public float[] faceSpot(Vec3 location) {
        Direction right = getDirection().getCounterClockWise();
        float u = (float) (location.x * right.getStepX() + location.z * right.getStepZ()) / columns() + 0.5F;
        float v = 0.5F - (float) location.y / ROWS;
        return new float[] {Math.min(1.0F, Math.max(0.0F, u)), Math.min(1.0F, Math.max(0.0F, v))};
    }

    @Override
    public InteractionResult interact(Player player, InteractionHand hand, Vec3 location) {
        ItemStack held = player.getItemInHand(hand);
        if (held.is(ModItems.LETTER_HOME.get())) {
            if (level() instanceof ServerLevel level && player instanceof ServerPlayer serverPlayer) {
                Captives.deliverLetter(level, serverPlayer, held, blockPosition());
            }
            return InteractionResult.SUCCESS;
        }
        BountyData bounty = held.is(ModItems.OUTLAW_BADGE.get()) ? held.get(ModDataComponents.BOUNTY.get()) : null;
        if (bounty == null && !held.isEmpty()) {
            return InteractionResult.PASS;
        }
        if (level() instanceof ServerLevel level && player instanceof ServerPlayer serverPlayer) {
            if (bounty != null) {
                BoardUse.turnIn(level, player, held, bounty, this, position());
            } else {
                BoardUse.emptyHand(level, serverPlayer, this, faceSpot(location), position());
            }
        }
        return InteractionResult.SUCCESS;
    }

    // ---------------------------------------------------------------- hanging like a painting

    @Override
    public void dropItem(ServerLevel level, @Nullable Entity causedBy) {
        if (level.getGameRules().get(GameRules.ENTITY_DROPS)) {
            playSound(SoundEvents.PAINTING_BREAK, 1.0F, 1.0F);
            if (!(causedBy instanceof Player player && player.hasInfiniteMaterials())) {
                spawnAtLocation(level, new ItemStack(ModItems.HANGING_BOUNTY_BOARD.get()));
            }
        }
    }

    @Override
    public void playPlacementSound() {
        playSound(SoundEvents.PAINTING_PLACE, 1.0F, 1.0F);
    }

    @Override
    protected void addAdditionalSaveData(ValueOutput output) {
        output.store("facing", Direction.LEGACY_ID_CODEC_2D, getDirection());
        super.addAdditionalSaveData(output);
        output.putInt("columns", columns());
        output.putString("list", key);
        output.store("face", BoardFace.MAP_CODEC.codec(), face());
    }

    @Override
    protected void readAdditionalSaveData(ValueInput input) {
        Direction direction = input.read("facing", Direction.LEGACY_ID_CODEC_2D).orElse(Direction.SOUTH);
        super.readAdditionalSaveData(input);
        entityData.set(DATA_COLUMNS, input.getIntOr("columns", NARROW) == WIDE ? WIDE : NARROW);
        key = input.getStringOr("list", "");
        entityData.set(DATA_FACE, input.read("face", BoardFace.MAP_CODEC.codec()).orElse(BoardFace.EMPTY));
        setDirection(direction);
    }

    @Override
    public void snapTo(double x, double y, double z, float yRot, float xRot) {
        setPos(x, y, z);
    }

    @Override
    public Vec3 trackingPosition() {
        return Vec3.atLowerCornerOf(pos);
    }

    @Override
    public Packet<ClientGamePacketListener> getAddEntityPacket(ServerEntity serverEntity) {
        return new ClientboundAddEntityPacket(this, getDirection().get3DDataValue(), getPos());
    }

    @Override
    public void recreateFromPacket(ClientboundAddEntityPacket packet) {
        super.recreateFromPacket(packet);
        setDirection(Direction.from3DDataValue(packet.getData()));
    }

    @Override
    public ItemStack getPickResult() {
        return new ItemStack(ModItems.HANGING_BOUNTY_BOARD.get());
    }
}
