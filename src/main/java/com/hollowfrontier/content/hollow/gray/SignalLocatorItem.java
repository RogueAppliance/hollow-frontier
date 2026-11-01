package com.hollowfrontier.content.hollow.gray;

import java.util.Optional;
import java.util.function.Consumer;

import com.hollowfrontier.content.frontier.mystery.Suspicion;
import com.hollowfrontier.registry.ModCriteria;
import com.hollowfrontier.registry.ModSounds;
import com.hollowfrontier.worldgen.structure.ModStructures;
import com.mojang.datafixers.util.Pair;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.GlobalPos;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderSet;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.LodestoneTracker;
import net.minecraft.world.item.component.TooltipDisplay;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.levelgen.structure.Structure;

/**
 * The Signal Locator (10b): Gray tech fragments wired round a phlogiston shard in a compass case. Use it and it picks up the
 * nearest live Gray field station's carrier signal (up to {@link #SEARCH_CHUNKS} chunks out) and points to it, as the
 * Polar Compass does to a gateway, and says roughly how far. Outside the Hollow Earth there's nothing to hear and the
 * needle spins.
 */
public class SignalLocatorItem extends Item {
    public static final int SEARCH_CHUNKS = 100;
    static final int COOLDOWN = 60;

    public SignalLocatorItem(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResult use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (level instanceof ServerLevel server) {
            Optional<BlockPos> target = bearing(server, player.blockPosition());
            if (target.isPresent()) {
                BlockPos pos = target.get();
                stack.set(DataComponents.LODESTONE_TRACKER, new LodestoneTracker(Optional.of(GlobalPos.of(level.dimension(), pos)), false));
                int distance = (int) Math.round(Math.sqrt(pos.distToCenterSqr(player.getX(), pos.getY(), player.getZ())) / 10) * 10;
                player.sendOverlayMessage(Component.translatable("message.hollowfrontier.signal_locator.bearing",
                        Math.max(10, distance), Component.translatable("message.hollowfrontier.direction." + direction(player.blockPosition(), pos))));
                level.playSound(null, player.blockPosition(), ModSounds.SIGNAL_LOCATOR_PING.get(), SoundSource.PLAYERS, 1.0F, 1.0F);
                ModCriteria.fire(player, ModCriteria.SIGNAL_FOUND);
                if (player instanceof ServerPlayer serverPlayer) {
                    Suspicion.add(serverPlayer, Suspicion.TAKE_BEARING); // they hear who's listening (11b)
                }
            } else {
                stack.remove(DataComponents.LODESTONE_TRACKER);
                player.sendOverlayMessage(Component.translatable("message.hollowfrontier.signal_locator.lost"));
            }
            player.getCooldowns().addCooldown(stack, COOLDOWN);
        }
        return InteractionResult.SUCCESS;
    }

    /** The nearest live Gray field station from here, if any is in range. */
    public static Optional<BlockPos> bearing(ServerLevel level, BlockPos from) {
        Optional<Holder.Reference<Structure>> station = level.registryAccess().lookupOrThrow(Registries.STRUCTURE).get(ModStructures.GRAY_STATION);
        if (station.isEmpty()) {
            return Optional.empty();
        }
        Pair<BlockPos, Holder<Structure>> found = level.getChunkSource().getGenerator()
                .findNearestMapStructure(level, HolderSet.direct(station.get()), from, SEARCH_CHUNKS, false);
        return found == null ? Optional.empty() : Optional.of(found.getFirst());
    }

    /** One of eight compass points from one spot to another (north is -z). */
    static String direction(BlockPos from, BlockPos to) {
        double angle = Mth.atan2(to.getX() - from.getX(), -(to.getZ() - from.getZ())) * Mth.RAD_TO_DEG;
        String[] points = {"north", "northeast", "east", "southeast", "south", "southwest", "west", "northwest"};
        return points[Math.floorMod((int) Math.round(angle / 45.0), 8)];
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, TooltipDisplay display, Consumer<Component> builder, TooltipFlag flag) {
        LodestoneTracker tracker = stack.get(DataComponents.LODESTONE_TRACKER);
        builder.accept(Component.translatable(tracker != null && tracker.target().isPresent()
                ? "tooltip.hollowfrontier.signal_locator.set" : "tooltip.hollowfrontier.signal_locator.unset").withStyle(ChatFormatting.GRAY));
    }
}
