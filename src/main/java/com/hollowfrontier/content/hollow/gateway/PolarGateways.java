package com.hollowfrontier.content.hollow.gateway;

import java.util.List;
import java.util.Optional;

import com.hollowfrontier.network.GatewayCrossingPayload;
import com.hollowfrontier.registry.ModAttachments;
import com.hollowfrontier.registry.ModBlocks;
import com.hollowfrontier.registry.ModSounds;
import com.hollowfrontier.worldgen.HollowEarth;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ClientboundSetSubtitleTextPacket;
import net.minecraft.network.protocol.game.ClientboundSetTitleTextPacket;
import net.minecraft.network.protocol.game.ClientboundSetTitlesAnimationPacket;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.neoforged.neoforge.network.PacketDistributor;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.Relative;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.LanternBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.portal.TeleportTransition;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

/**
 * Polar Gateways: a ring of twelve {@link GatewayFrameBlock}s round a 3x3 well, like an end portal's, lying flat in the
 * floor of an ice crater (PolarBlueprints). A Polar Resonator wakes the ring stone by stone and then opens the well
 * (GatewayEffects, then {@link #open}); stepping in carries you to the Hollow Earth at the same x/z. The first crossing builds a small return gateway on the ground there and remembers the
 * pair ({@link GatewayLinks}), so later trips land at the same spot and the return gateway leads home.
 */
public final class PolarGateways {
    private PolarGateways() {}

    /** The ring's stones, relative to the centre of the well. */
    public static final List<BlockPos> RING;
    static {
        java.util.ArrayList<BlockPos> ring = new java.util.ArrayList<>();
        for (int i = -1; i <= 1; i++) {
            ring.add(new BlockPos(i, 0, -2));
            ring.add(new BlockPos(i, 0, 2));
            ring.add(new BlockPos(-2, 0, i));
            ring.add(new BlockPos(2, 0, i));
        }
        RING = List.copyOf(ring);
    }
    /** The same stones in order round the ring (clockwise seen from above, from the north side), for waking it. */
    public static final List<BlockPos> RING_ORDER = RING.stream()
            .sorted(java.util.Comparator.comparingDouble(p -> Math.atan2(p.getZ(), p.getX()))).toList();
    /** How far above the return gateway a traveller comes out going in, room allowing, to drift down to it. */
    public static final int DESCENT_HEIGHT = 18;

    private static ResourceKey<Level> inside = HollowEarth.LEVEL;

    /** The dimension the gateways lead into: the Hollow Earth. */
    public static ResourceKey<Level> inside() {
        return inside;
    }

    /**
     * Game tests only: the game test server loads no datapack dimensions, so there is no Hollow Earth there, and the tests
     * stand the Nether in for it. Null puts the Hollow Earth back.
     */
    public static void standInForHollowEarth(ResourceKey<Level> level) {
        inside = level == null ? HollowEarth.LEVEL : level;
    }

    // ---------------------------------------------------------------- the ring

    /** The centre of the complete ring this frame stone belongs to, if the ring is whole. */
    public static Optional<BlockPos> ringCenter(Level level, BlockPos frame) {
        for (BlockPos offset : RING) {
            BlockPos center = frame.subtract(offset);
            if (isRing(level, center)) {
                return Optional.of(center);
            }
        }
        return Optional.empty();
    }

    public static boolean isRing(Level level, BlockPos center) {
        return RING.stream().allMatch(offset -> level.getBlockState(center.offset(offset)).getBlock() instanceof GatewayFrameBlock);
    }

    public static boolean isOpen(Level level, BlockPos center) {
        return level.getBlockState(center).is(ModBlocks.POLAR_PORTAL.get());
    }

    /** Whether the well is clear to open: air, snow or plants only (not someone's cobblestone). */
    public static boolean wellClear(Level level, BlockPos center) {
        for (int dx = -1; dx <= 1; dx++) {
            for (int dz = -1; dz <= 1; dz++) {
                BlockState state = level.getBlockState(center.offset(dx, 0, dz));
                if (!state.canBeReplaced() && !state.is(ModBlocks.POLAR_PORTAL.get())) {
                    return false;
                }
            }
        }
        return true;
    }

    /** Lights the ring and fills the well with the portal. */
    public static void open(ServerLevel level, BlockPos center) {
        setActive(level, center, true);
        for (int dx = -1; dx <= 1; dx++) {
            for (int dz = -1; dz <= 1; dz++) {
                level.setBlock(center.offset(dx, 0, dz), ModBlocks.POLAR_PORTAL.get().defaultBlockState(), Block.UPDATE_CLIENTS);
            }
        }
        level.playSound(null, center, ModSounds.GATEWAY_OPEN.get(), SoundSource.BLOCKS, 1.5F, 1.0F);
        level.sendParticles(ParticleTypes.END_ROD, center.getX() + 0.5, center.getY() + 1.0, center.getZ() + 0.5, 60, 1.2, 0.6, 1.2, 0.05);
    }

    /** Puts the light out: the well empties and the ring goes dark. */
    public static void close(ServerLevel level, BlockPos center) {
        setActive(level, center, false);
        for (int dx = -1; dx <= 1; dx++) {
            for (int dz = -1; dz <= 1; dz++) {
                BlockPos pos = center.offset(dx, 0, dz);
                if (level.getBlockState(pos).is(ModBlocks.POLAR_PORTAL.get())) {
                    level.setBlock(pos, Blocks.AIR.defaultBlockState(), Block.UPDATE_ALL);
                }
            }
        }
        level.playSound(null, center, ModSounds.GATEWAY_CLOSE.get(), SoundSource.BLOCKS, 1.2F, 1.0F);
    }

    private static void setActive(ServerLevel level, BlockPos center, boolean active) {
        for (BlockPos offset : RING) {
            BlockPos pos = center.offset(offset);
            BlockState state = level.getBlockState(pos);
            if (state.getBlock() instanceof GatewayFrameBlock) {
                level.setBlock(pos, state.setValue(GatewayFrameBlock.ACTIVE, active), Block.UPDATE_ALL);
            }
        }
    }

    /** The centre of the well a portal block belongs to: the one of it and its neighbours that has portal all round. */
    public static BlockPos wellCenter(Level level, BlockPos portal) {
        for (int dx = -1; dx <= 1; dx++) {
            for (int dz = -1; dz <= 1; dz++) {
                BlockPos center = portal.offset(dx, 0, dz);
                if (isRing(level, center)) {
                    return center;
                }
            }
        }
        return portal;
    }

    // ---------------------------------------------------------------- crossing

    /** Where an entity stepping into the well at portalPos comes out, building the far side on a first crossing. */
    public static TeleportTransition destination(ServerLevel from, Entity entity, BlockPos portalPos) {
        BlockPos center = wellCenter(from, portalPos);
        GatewayLinks links = GatewayLinks.get(from);
        boolean fromInside = from.dimension() == inside();
        ServerLevel to = from.getServer().getLevel(fromInside ? Level.OVERWORLD : inside());
        if (to == null) {
            return null;
        }
        BlockPos far;
        if (fromInside) {
            Optional<GatewayLinks.Link> link = links.fromHollow(center);
            if (link.isEmpty()) {
                // A gateway inside that leads nowhere (built by hand in creative): out at the world spawn.
                BlockPos spawn = to.getRespawnData().pos();
                return arrive(to, Vec3.atBottomCenterOf(to.getHeightmapPos(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, spawn)), entity, false, null);
            }
            far = link.get().overworld();
        } else {
            far = links.fromOverworld(center).map(GatewayLinks.Link::hollow).orElseGet(() -> {
                BlockPos hollow = landing(to, center.getX(), center.getZ());
                buildReturnGateway(to, hollow);
                SymmesShaft.carve(to, hollow);
                links.add(new GatewayLinks.Link(center, hollow));
                return hollow;
            });
            to.getChunk(far); // load (or generate) the far side before standing anyone on it
            if (!isRing(to, far)) {
                buildReturnGateway(to, far); // someone tore it down in creative: build it again
            }
        }
        to.getChunk(far);
        // Out over the south stone of the far ring, facing the well, clear of the portal (the cooldown would hold anyway).
        Vec3 stone = new Vec3(far.getX() + 0.5, far.getY() + 1.0, far.getZ() + 2.5);
        boolean intoHollow = !fromInside;
        if (intoHollow && entity instanceof ServerPlayer) {
            // Players come out at the top of Symmes' shaft, looking down it, and fall through the crust (GatewayEffects, HollowDescent)
            SymmesShaft.Mouth mouth = SymmesShaft.mouth(to, far);
            GatewayEffects.Fall fall = new GatewayEffects.Fall(stone, mouth.undersideY());
            return arrive(to, new Vec3(stone.x, mouth.arrivalY(), stone.z), entity, true, fall).withRotation(180.0F, 80.0F);
        }
        if (intoHollow && entity instanceof LivingEntity) {
            // Other living things come out a little above the gateway and drift down to it
            return arrive(to, stone.add(0, dropHeight(to, stone), 0), entity, true, null).withRotation(180.0F, 55.0F);
        }
        return arrive(to, stone, entity, intoHollow, null).withRotation(180.0F, 20.0F);
    }

    /** As high above pos as there's clear air for a traveller, up to {@link #DESCENT_HEIGHT}. */
    static int dropHeight(ServerLevel level, Vec3 pos) {
        BlockPos base = BlockPos.containing(pos);
        for (int h = 1; h <= DESCENT_HEIGHT + 2; h++) {
            if (!level.getBlockState(base.above(h)).getCollisionShape(level, base.above(h)).isEmpty()) {
                return Math.max(0, h - 3);
            }
        }
        return DESCENT_HEIGHT;
    }

    private static TeleportTransition arrive(ServerLevel to, Vec3 pos, Entity entity, boolean intoHollow, GatewayEffects.@Nullable Fall fall) {
        return new TeleportTransition(to, pos, Vec3.ZERO, entity.getYRot(), entity.getXRot(), Relative.DELTA,
                TeleportTransition.PLACE_PORTAL_TICKET.then(traveller -> arrived(traveller, intoHollow, fall)));
    }

    /**
     * After the crossing (instead of the nether portal's travel sound): a rush of air and a burst of snow and light where
     * they come out; a living traveller drifts down (or is lifted out of the well coming home); a player's screen whites
     * out and clears (GatewayCrossingPayload). A player going in falls down Symmes' shaft ({@code fall}), and the first time
     * the title card comes up as they drop out of the crust (GatewayEffects).
     */
    static void arrived(Entity traveller, boolean intoHollow, GatewayEffects.@Nullable Fall fall) {
        if (!(traveller.level() instanceof ServerLevel level)) {
            return;
        }
        level.playSound(null, traveller.getX(), traveller.getY(), traveller.getZ(), ModSounds.GATEWAY_CROSS.get(), SoundSource.PLAYERS, 1.0F, 1.0F);
        level.sendParticles(ParticleTypes.SNOWFLAKE, traveller.getX(), traveller.getY() + 1.0, traveller.getZ(), 40, 0.6, 0.8, 0.6, 0.08);
        level.sendParticles(ParticleTypes.END_ROD, traveller.getX(), traveller.getY() + 1.0, traveller.getZ(), 20, 0.4, 0.6, 0.4, 0.05);
        boolean firstVisit = false;
        if (traveller instanceof ServerPlayer player && intoHollow && !player.getData(ModAttachments.VISITED_HOLLOW_EARTH)) {
            player.setData(ModAttachments.VISITED_HOLLOW_EARTH, true);
            firstVisit = true;
        }
        if (traveller instanceof LivingEntity living) {
            GatewayEffects.descend(living, intoHollow, fall, firstVisit);
        }
        if (traveller instanceof ServerPlayer player && player.connection.hasChannel(GatewayCrossingPayload.TYPE)) {
            // not on a client without the mod (or a test's fake player)
            PacketDistributor.sendToPlayer(player, fall != null
                    ? new GatewayCrossingPayload(true, fall.target().x, fall.target().y, fall.target().z, fall.undersideY())
                    : GatewayCrossingPayload.plain(intoHollow));
        }
    }

    /** The first-visit title card: THE HOLLOW EARTH, beneath the ice, another sun. */
    static void showTitle(ServerPlayer player) {
        player.connection.send(new ClientboundSetTitlesAnimationPacket(30, 90, 40));
        player.connection.send(new ClientboundSetSubtitleTextPacket(Component.translatable("title.hollowfrontier.hollow_earth.subtitle")
                .withStyle(ChatFormatting.GRAY, ChatFormatting.ITALIC)));
        player.connection.send(new ClientboundSetTitleTextPacket(Component.translatable("title.hollowfrontier.hollow_earth")
                .withStyle(ChatFormatting.GOLD)));
    }

    /**
     * Where the return gateway goes in the Hollow Earth: level, dry land under the crust near x/z (HollowLanding), or a
     * platform at the surface of the water if there's nothing else. The heightmap can't be used: its top is the crust.
     */
    static BlockPos landing(ServerLevel level, int x, int z) {
        for (int cx = (x - HollowLanding.SEARCH_RADIUS - 3) >> 4; cx <= (x + HollowLanding.SEARCH_RADIUS + 3) >> 4; cx++) {
            for (int cz = (z - HollowLanding.SEARCH_RADIUS - 3) >> 4; cz <= (z + HollowLanding.SEARCH_RADIUS + 3) >> 4; cz++) {
                level.getChunk(cx, cz); // generate the search area first (trees and all)
            }
        }
        BlockPos pos = HollowLanding.find(HollowLanding.of(level), x, z).pos();
        return new BlockPos(pos.getX(), Math.max(level.getMinY() + 6, Math.min(level.getMaxY() - 8, pos.getY())), pos.getZ());
    }

    /**
     * The return gateway: a 7x7 floor of old mossy stonework set into the ground, the ring and open well in its middle,
     * a lantern on a post at each corner, and headroom cleared above. The well is at {@code center}.
     */
    public static void buildReturnGateway(ServerLevel level, BlockPos center) {
        // A clearing first: no tree stands over the gateway or overhangs its floor
        BlockPos.MutableBlockPos cut = new BlockPos.MutableBlockPos();
        for (int dx = -6; dx <= 6; dx++) {
            for (int dz = -6; dz <= 6; dz++) {
                for (int dy = 1; dy <= 24; dy++) {
                    BlockState state = level.getBlockState(cut.set(center.getX() + dx, center.getY() + dy, center.getZ() + dz));
                    if (state.is(BlockTags.LOGS) || state.is(BlockTags.LEAVES) || state.is(Blocks.VINE) || state.is(Blocks.BAMBOO)) {
                        level.setBlock(cut, Blocks.AIR.defaultBlockState(), Block.UPDATE_CLIENTS);
                    }
                }
            }
        }
        BlockState floor = Blocks.MOSSY_STONE_BRICKS.defaultBlockState();
        for (int dx = -3; dx <= 3; dx++) {
            for (int dz = -3; dz <= 3; dz++) {
                BlockPos column = center.offset(dx, 0, dz);
                level.setBlock(column.below(), Blocks.STONE_BRICKS.defaultBlockState(), Block.UPDATE_CLIENTS);
                level.setBlock(column, (dx + dz) % 3 == 0 ? Blocks.CRACKED_STONE_BRICKS.defaultBlockState() : floor, Block.UPDATE_CLIENTS);
                for (int dy = 1; dy <= 4; dy++) {
                    level.setBlock(column.above(dy), Blocks.AIR.defaultBlockState(), Block.UPDATE_CLIENTS);
                }
            }
        }
        for (BlockPos offset : RING) {
            level.setBlock(center.offset(offset), ModBlocks.GATEWAY_FRAME.get().defaultBlockState().setValue(GatewayFrameBlock.ACTIVE, true), Block.UPDATE_CLIENTS);
        }
        for (int dx = -1; dx <= 1; dx++) {
            for (int dz = -1; dz <= 1; dz++) {
                level.setBlock(center.offset(dx, 0, dz), ModBlocks.POLAR_PORTAL.get().defaultBlockState(), Block.UPDATE_CLIENTS);
            }
        }
        for (Direction a : new Direction[] {Direction.NORTH, Direction.SOUTH}) {
            for (Direction b : new Direction[] {Direction.EAST, Direction.WEST}) {
                BlockPos post = center.relative(a, 3).relative(b, 3);
                level.setBlock(post.above(), Blocks.MOSSY_STONE_BRICK_WALL.defaultBlockState(), Block.UPDATE_ALL);
                level.setBlock(post.above(2), Blocks.LANTERN.defaultBlockState().setValue(LanternBlock.HANGING, false), Block.UPDATE_ALL);
            }
        }
    }
}
