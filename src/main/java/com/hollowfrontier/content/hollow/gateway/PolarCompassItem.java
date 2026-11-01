package com.hollowfrontier.content.hollow.gateway;

import java.util.Optional;
import java.util.function.Consumer;

import com.hollowfrontier.registry.ModCriteria;
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
import net.minecraft.sounds.SoundEvents;
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
 * A compass whose needle hunts for the north pole that isn't there. Use it to take a bearing: it finds the nearest Polar
 * Gateway (up to {@link #SEARCH_CHUNKS} chunks out) and points there, like a lodestone compass, and says roughly how far.
 * In the Hollow Earth it points to the nearest return gateway instead. The bearing is kept on the item (a lodestone
 * tracker that doesn't need a lodestone), so the needle holds steady until the next bearing.
 */
public class PolarCompassItem extends Item {
    public static final int SEARCH_CHUNKS = 100;
    static final int COOLDOWN = 60;

    public PolarCompassItem(Properties properties) {
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
                player.sendOverlayMessage(Component.translatable("message.hollowfrontier.polar_compass.bearing",
                        Math.max(10, distance), Component.translatable("message.hollowfrontier.direction." + direction(player.blockPosition(), pos))));
                level.playSound(null, player.blockPosition(), SoundEvents.LODESTONE_COMPASS_LOCK, SoundSource.PLAYERS, 1.0F, 1.2F);
                ModCriteria.fire(player, ModCriteria.POLAR_BEARING);
            } else {
                stack.remove(DataComponents.LODESTONE_TRACKER);
                player.sendOverlayMessage(Component.translatable("message.hollowfrontier.polar_compass.lost"));
            }
            player.getCooldowns().addCooldown(stack, COOLDOWN);
        }
        return InteractionResult.SUCCESS;
    }

    /** The nearest gateway from here: Overworld (or any other dimension) gateways by structure search, return gateways inside. */
    public static Optional<BlockPos> bearing(ServerLevel level, BlockPos from) {
        if (level.dimension() == PolarGateways.inside()) {
            return GatewayLinks.get(level).nearestHollow(from, SEARCH_CHUNKS * 16).map(GatewayLinks.Link::hollow);
        }
        Optional<Holder.Reference<Structure>> gateway = level.registryAccess().lookupOrThrow(Registries.STRUCTURE).get(ModStructures.POLAR_GATEWAY);
        if (gateway.isEmpty()) {
            return Optional.empty();
        }
        Pair<BlockPos, Holder<Structure>> found = level.getChunkSource().getGenerator()
                .findNearestMapStructure(level, HolderSet.direct(gateway.get()), from, SEARCH_CHUNKS, false);
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
                ? "tooltip.hollowfrontier.polar_compass.set" : "tooltip.hollowfrontier.polar_compass.unset").withStyle(ChatFormatting.GRAY));
    }
}
