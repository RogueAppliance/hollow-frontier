package com.hollowfrontier.content.ufo;

import org.jspecify.annotations.Nullable;

import com.hollowfrontier.registry.ModBlockEntities;
import com.hollowfrontier.registry.ModCriteria;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;

/**
 * The Phlogiston Beacon (13d): a Phlogiston Core set in starsteel with the Grays' own tech round it, which calls the Mothership
 * down. Use it in the Overworld, at night, under the open sky: it lights, a violet beam goes up from it, and
 * {@link PhlogistonBeaconBlockEntity#SUMMON_TICKS} later the Mothership comes down out of the sky over it. It stays lit while
 * that Mothership lives (it goes dark when the ship falls or leaves), and it can call one again, any night.
 */
public class PhlogistonBeaconBlock extends BaseEntityBlock {
    public static final BooleanProperty LIT = BooleanProperty.create("lit");
    public static final double ONE_AT_A_TIME = 512.0;

    public PhlogistonBeaconBlock(Properties properties) {
        super(properties);
        registerDefaultState(stateDefinition.any().setValue(LIT, false));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(LIT);
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hitResult) {
        if (level instanceof ServerLevel server) {
            String refused = call(server, pos);
            if (refused != null) {
                player.sendOverlayMessage(Component.translatable("message.hollowfrontier.phlogiston_beacon." + refused).withStyle(ChatFormatting.GRAY));
            } else {
                ModCriteria.fire(player, ModCriteria.MOTHERSHIP_SUMMONED);
            }
        }
        return InteractionResult.SUCCESS;
    }

    /**
     * Calls the Mothership to the beacon at {@code pos}: null if it's coming, or why not ({@code not_here} off the
     * Overworld, {@code no_sky}, {@code already} if one's on its way or about, {@code not_night}).
     */
    public static @Nullable String call(ServerLevel level, BlockPos pos) {
        String refused = refusal(level, pos);
        if (refused == null && !level.isDarkOutside()) {
            refused = "not_night";
        }
        if (refused == null) {
            light(level, pos);
        }
        return refused;
    }

    /** Why the beacon at {@code pos} can't call, whatever the hour; null if it can. */
    public static @Nullable String refusal(ServerLevel level, BlockPos pos) {
        if (level.dimension() != Level.OVERWORLD || !(level.getBlockEntity(pos) instanceof PhlogistonBeaconBlockEntity)) {
            return "not_here";
        }
        if (!openSky(level, pos)) {
            return "no_sky";
        }
        if (level.getBlockState(pos).getValue(LIT) || !level.getEntitiesOfClass(Mothership.class, new AABB(pos).inflate(ONE_AT_A_TIME)).isEmpty()) {
            return "already";
        }
        return null;
    }

    /**
     * Whether nothing over the beacon shuts out the sky: every block up the column lets sky light through (glass and
     * barriers do). Read from the blocks, not the sky light itself, which lags a block just placed overhead.
     */
    static boolean openSky(ServerLevel level, BlockPos pos) {
        BlockPos.MutableBlockPos up = pos.mutable();
        for (int y = pos.getY() + 1; y < level.getMaxY(); y++) {
            up.setY(y);
            if (!level.getBlockState(up).propagatesSkylightDown()) {
                return false;
            }
        }
        return true;
    }

    /** Lights it: the count down to the Mothership begins. */
    public static void light(ServerLevel level, BlockPos pos) {
        BlockState state = level.getBlockState(pos);
        if (!(level.getBlockEntity(pos) instanceof PhlogistonBeaconBlockEntity beacon)) {
            return;
        }
        level.setBlock(pos, state.setValue(LIT, true), Block.UPDATE_ALL);
        beacon.startCountdown();
        level.playSound(null, pos, SoundEvents.BEACON_ACTIVATE, SoundSource.BLOCKS, 2.0F, 0.6F);
        for (Player near : level.getEntitiesOfClass(Player.class, new AABB(pos).inflate(64))) {
            near.sendSystemMessage(Component.translatable("message.hollowfrontier.phlogiston_beacon.called").withStyle(ChatFormatting.DARK_PURPLE, ChatFormatting.ITALIC));
        }
    }

    /** Puts the beacon at {@code pos} out, if it's still there and lit (its Mothership is gone). */
    public static void putOut(ServerLevel level, BlockPos pos) {
        BlockState state = level.getBlockState(pos);
        if (state.getBlock() instanceof PhlogistonBeaconBlock && state.getValue(LIT)) {
            level.setBlock(pos, state.setValue(LIT, false), Block.UPDATE_ALL);
            level.playSound(null, pos, SoundEvents.BEACON_DEACTIVATE, SoundSource.BLOCKS, 1.5F, 0.6F);
        }
    }

    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new PhlogistonBeaconBlockEntity(pos, state);
    }

    @Override
    public <T extends BlockEntity> @Nullable BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        return level.isClientSide() ? null : createTickerHelper(type, ModBlockEntities.PHLOGISTON_BEACON.get(), PhlogistonBeaconBlockEntity::serverTick);
    }
}
