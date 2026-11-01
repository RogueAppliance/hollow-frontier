package com.hollowfrontier.content.hollow.gateway;

import java.util.Optional;
import java.util.function.Consumer;

import org.jspecify.annotations.Nullable;

import com.hollowfrontier.content.steam.airship.Airship;
import com.hollowfrontier.registry.ModCriteria;
import com.hollowfrontier.registry.ModDataComponents;
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
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.LodestoneTracker;
import net.minecraft.world.item.component.TooltipDisplay;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.chunk.status.ChunkStatus;
import net.minecraft.world.level.levelgen.structure.Structure;
import net.minecraft.world.level.levelgen.structure.StructureStart;

/**
 * A compass whose needle hunts for the north pole that isn't there. It finds the nearest Polar Gateway (up to
 * {@link #SEARCH_CHUNKS} chunks out) and points at the middle of its ring, like a lodestone compass. In the Hollow Earth
 * it points to the nearest return gateway instead. The bearing is kept on the item (a lodestone tracker that doesn't
 * need a lodestone), so the needle follows the player as they travel.
 * <p>
 * Since 32b it takes its own bearings: carried, it searches when it has none and again every {@link #RESEARCH_DISTANCE}
 * blocks travelled (or in another world), so a nearer gateway takes over. Held, it says how far and which way on the
 * overlay; a pilot reads it in the airship's gauge line instead. Using it forces a fresh bearing, as before.
 */
public class PolarCompassItem extends Item {
    public static final int SEARCH_CHUNKS = 100;
    /** How far a carried compass goes on its last bearing before searching again (blocks). */
    public static final int RESEARCH_DISTANCE = 256;
    /** Within this many blocks of the ring's middle the reading says you're there. */
    static final int ARRIVED = 4;
    static final int COOLDOWN = 60;

    public PolarCompassItem(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResult use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (level instanceof ServerLevel server) {
            takeBearing(server, player, stack, "message.hollowfrontier.polar_compass.bearing");
            player.getCooldowns().addCooldown(stack, COOLDOWN);
        }
        return InteractionResult.SUCCESS;
    }

    @Override
    public void inventoryTick(ItemStack stack, ServerLevel level, Entity owner, @Nullable EquipmentSlot slot) {
        if (owner instanceof ServerPlayer player && (level.getGameTime() + player.getId()) % 20 == 0) {
            update(level, player, stack);
        }
    }

    /** Once a second for a carried compass: a bearing if it has none or has come a long way, and the reading if held. */
    public static void update(ServerLevel level, ServerPlayer player, ItemStack stack) {
        GlobalPos from = stack.get(ModDataComponents.BEARING_FROM.get());
        if (from == null) {
            takeBearing(level, player, stack, "message.hollowfrontier.polar_compass.first"); // a new compass: say how to read it
        } else if (from.dimension() != level.dimension()
                || from.pos().distToCenterSqr(player.getX(), from.pos().getY(), player.getZ()) > (double) RESEARCH_DISTANCE * RESEARCH_DISTANCE) {
            takeBearing(level, player, stack, null);
        }
        boolean held = player.getMainHandItem() == stack || player.getOffhandItem() == stack;
        boolean piloting = player.getVehicle() instanceof Airship ship && ship.getFirstPassenger() == player;
        if (held && !piloting) {
            reading(player, stack).ifPresent(player::sendOverlayMessage);
        }
    }

    /**
     * Searches from where the player stands and sets the needle; says so (with {@code messageKey}, or quietly if null,
     * though it always says when it can't find one on a click). Remembers where it searched from.
     */
    static void takeBearing(ServerLevel level, Player player, ItemStack stack, @Nullable String messageKey) {
        stack.set(ModDataComponents.BEARING_FROM.get(), GlobalPos.of(level.dimension(), player.blockPosition()));
        Optional<BlockPos> target = bearing(level, player.blockPosition());
        if (target.isPresent()) {
            BlockPos pos = target.get();
            LodestoneTracker before = stack.get(DataComponents.LODESTONE_TRACKER);
            GlobalPos aim = GlobalPos.of(level.dimension(), pos);
            stack.set(DataComponents.LODESTONE_TRACKER, new LodestoneTracker(Optional.of(aim), false));
            boolean moved = before == null || !before.target().equals(Optional.of(aim));
            if (messageKey != null) {
                player.sendOverlayMessage(Component.translatable(messageKey, distance(player, pos), heading(player, pos)));
            }
            if (messageKey != null || moved) {
                level.playSound(null, player.blockPosition(), SoundEvents.LODESTONE_COMPASS_LOCK, SoundSource.PLAYERS, 1.0F, 1.2F);
            }
            ModCriteria.fire(player, ModCriteria.POLAR_BEARING);
        } else {
            stack.remove(DataComponents.LODESTONE_TRACKER);
            if (messageKey != null) {
                player.sendOverlayMessage(Component.translatable("message.hollowfrontier.polar_compass.lost"));
            }
        }
    }

    /** What a held compass says: how far and which way to its gateway, or that you're there. Empty without a bearing here. */
    public static Optional<Component> reading(Player player, ItemStack stack) {
        LodestoneTracker tracker = stack.get(DataComponents.LODESTONE_TRACKER);
        if (tracker == null || tracker.target().isEmpty() || tracker.target().get().dimension() != player.level().dimension()) {
            return Optional.empty();
        }
        BlockPos pos = tracker.target().get().pos();
        if (pos.distToCenterSqr(player.getX(), pos.getY(), player.getZ()) <= ARRIVED * ARRIVED) {
            return Optional.of(Component.translatable("message.hollowfrontier.polar_compass.here"));
        }
        return Optional.of(Component.translatable("message.hollowfrontier.polar_compass.reading", distance(player, pos), heading(player, pos)));
    }

    /** The reading of the first Polar Compass in the player's hands, if they hold one with a bearing. */
    public static Optional<Component> heldReading(Player player) {
        for (InteractionHand hand : InteractionHand.values()) {
            ItemStack stack = player.getItemInHand(hand);
            if (stack.getItem() instanceof PolarCompassItem) {
                return reading(player, stack);
            }
        }
        return Optional.empty();
    }

    // Rounded to tens, as a needle can't do better
    private static int distance(Player player, BlockPos pos) {
        return Math.max(10, (int) Math.round(Math.sqrt(pos.distToCenterSqr(player.getX(), pos.getY(), player.getZ())) / 10) * 10);
    }

    private static Component heading(Player player, BlockPos pos) {
        return Component.translatable("message.hollowfrontier.direction." + direction(player.blockPosition(), pos));
    }

    /**
     * The nearest gateway from here, at the middle of its ring: Overworld (or any other dimension) gateways by structure
     * search, return gateways inside. A structure search only finds the corner of the chunk a gateway starts in, so the
     * ring's middle is taken from the gateway's bounds (its crater is drawn round the ring).
     */
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
        if (found == null) {
            return Optional.empty();
        }
        ChunkPos chunk = ChunkPos.containing(found.getFirst());
        StructureStart start = level.structureManager().getStartForStructure(gateway.get().value(),
                level.getChunk(chunk.x(), chunk.z(), ChunkStatus.STRUCTURE_STARTS));
        return Optional.of(start != null && start.isValid() ? start.getBoundingBox().getCenter() : found.getFirst());
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
