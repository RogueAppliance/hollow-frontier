package com.hollowfrontier.content.ufo.taken;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.StringRepresentable;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.phys.BlockHitResult;

/**
 * A console aboard the Grays' ship (50): a slab of glass and light with one thing it does, by its {@link Kind}. The pens'
 * drops every pen's field; the hold's lets out what's kept there; a pod's launches it. Used once, it goes dark
 * ({@link #SPENT}). Away from a ship it does nothing: there's nothing for it to speak to.
 */
public class ShipConsoleBlock extends Block {
    public enum Kind implements StringRepresentable {
        PENS, HOLD, POD;

        @Override
        public String getSerializedName() {
            return name().toLowerCase(java.util.Locale.ROOT);
        }
    }

    public static final EnumProperty<Kind> KIND = EnumProperty.create("kind", Kind.class);
    public static final BooleanProperty SPENT = BooleanProperty.create("spent");

    public ShipConsoleBlock(Properties properties) {
        super(properties);
        registerDefaultState(stateDefinition.any().setValue(KIND, Kind.PENS).setValue(SPENT, false));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(KIND, SPENT);
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        if (level instanceof ServerLevel server && player instanceof ServerPlayer serverPlayer) {
            Ship ship = Taken.shipAt(server, pos);
            if (ship == null || state.getValue(SPENT)) {
                player.sendOverlayMessage(Component.translatable(TakenText.CONSOLE_DEAD).withStyle(ChatFormatting.GRAY));
            } else if (ship.console(server, serverPlayer, pos, state.getValue(KIND))) {
                level.setBlock(pos, state.setValue(SPENT, true), Block.UPDATE_ALL);
            }
        }
        return InteractionResult.SUCCESS;
    }
}
