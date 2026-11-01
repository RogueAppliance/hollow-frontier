package com.hollowfrontier.content.concavia;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import com.hollowfrontier.HollowFrontier;
import com.hollowfrontier.content.hollow.gateway.GatewayLinks;
import com.hollowfrontier.registry.ModCriteria;
import com.hollowfrontier.registry.ModTags;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Relative;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.portal.TeleportTransition;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.level.saveddata.SavedDataType;
import net.minecraft.world.phys.Vec3;

/**
 * The waystone network (15c): Concavia's old roads, woken one ruin at a time. A Crystal Ruins restored to its last stage
 * ({@link Restoration}) wakes its waystone, and every woken waystone links to every other and to every Polar Gateway that
 * has been crossed ({@link GatewayLinks}: an opened one, found from both ends). From a waystone, or from a crossed
 * gateway's ring with a phlogiston shard in hand, a player picks where to go ({@link WaystoneMenu}) and is carried there for
 * {@link #FARE} phlogiston shard (nothing in creative). The first fast travel there is, and a sink for phlogiston.
 * <p>
 * Each waystone takes the next of {@link #NAMES} Concavian names in the order they woke. Kept with the Overworld's
 * saved data, since the stones and the gateways are in two worlds.
 */
public class Waystones extends SavedData {
    public static final int FARE = 1;
    public static final int NAMES = 12;
    /** How far south of a gateway's well a traveller comes down: past its ring of stones (2 out) and onto the rim. */
    public static final int GATEWAY_STANDOFF = 6;

    /** A woken waystone: where it stands, where a traveller arriving by it stands, and its name's number. */
    public record Stone(ResourceKey<Level> dimension, BlockPos pos, BlockPos arrival, int name) {
        static final Codec<Stone> CODEC = RecordCodecBuilder.create(i -> i.group(
                ResourceKey.codec(Registries.DIMENSION).fieldOf("dimension").forGetter(Stone::dimension),
                BlockPos.CODEC.fieldOf("pos").forGetter(Stone::pos),
                BlockPos.CODEC.fieldOf("arrival").forGetter(Stone::arrival),
                Codec.INT.fieldOf("name").forGetter(Stone::name)).apply(i, Stone::new));

        public Component label() {
            Component base = Component.translatable("waystone.hollowfrontier.name." + name % NAMES);
            return name < NAMES ? base : Component.translatable("waystone.hollowfrontier.name.numbered", base, name / NAMES + 1);
        }
    }

    /** Somewhere a traveller can go: what it's called, and where they come out (for a gateway, its well: they come down beside it). */
    public record Destination(Component label, ResourceKey<Level> dimension, BlockPos at, boolean gateway) {}

    public static final Codec<Waystones> CODEC = Stone.CODEC.listOf().fieldOf("stones").xmap(Waystones::new, w -> List.copyOf(w.stones)).codec();
    public static final SavedDataType<Waystones> TYPE = new SavedDataType<>(HollowFrontier.id("waystones"), Waystones::new, CODEC);

    private final List<Stone> stones = new ArrayList<>();

    public Waystones() {}

    private Waystones(List<Stone> stones) {
        this.stones.addAll(stones);
    }

    public static Waystones get(ServerLevel level) {
        return level.getServer().overworld().getDataStorage().computeIfAbsent(TYPE);
    }

    public List<Stone> stones() {
        return List.copyOf(stones);
    }

    public Optional<Stone> at(ResourceKey<Level> dimension, BlockPos pos) {
        return stones.stream().filter(s -> s.dimension().equals(dimension) && s.pos().equals(pos)).findFirst();
    }

    /** Wakes the waystone at {@code pos} (if it isn't already): it joins the network under the next name. */
    public Stone wake(ResourceKey<Level> dimension, BlockPos pos, BlockPos arrival) {
        Optional<Stone> known = at(dimension, pos);
        if (known.isPresent()) {
            return known.get();
        }
        Stone stone = new Stone(dimension, pos.immutable(), arrival.immutable(), stones.size());
        stones.add(stone);
        setDirty();
        return stone;
    }

    /** For tests: forgets a waystone. */
    public void forget(ResourceKey<Level> dimension, BlockPos pos) {
        if (stones.removeIf(s -> s.dimension().equals(dimension) && s.pos().equals(pos))) {
            setDirty();
        }
    }

    /** Where a player can go from the waystone {@code from}: every other woken waystone, then every crossed gateway. */
    public List<Destination> fromStone(ServerLevel level, Stone from) {
        List<Destination> out = new ArrayList<>();
        for (Stone stone : stones) {
            if (!stone.equals(from)) {
                out.add(new Destination(stone.label(), stone.dimension(), stone.arrival(), false));
            }
        }
        for (GatewayLinks.Link link : GatewayLinks.get(level).all()) {
            out.add(new Destination(Component.translatable("waystone.hollowfrontier.gateway", link.overworld().getX(), link.overworld().getZ()),
                    Level.OVERWORLD, link.overworld(), true));
        }
        return out;
    }

    /** Where a player can go from a crossed gateway's ring: every woken waystone. */
    public List<Destination> fromGateway() {
        return stones.stream().map(s -> new Destination(s.label(), s.dimension(), s.arrival(), false)).toList();
    }

    /**
     * Carries {@code player} to {@code to} for the fare: false (and why, on their screen) if they can't pay or the far end
     * can't be reached.
     */
    public static boolean travel(ServerPlayer player, Destination to) {
        ServerLevel there = player.level().getServer().getLevel(to.dimension());
        if (there == null) {
            return false;
        }
        if (!player.getAbilities().instabuild && !payFare(player)) {
            player.sendOverlayMessage(Component.translatable("message.hollowfrontier.waystone.fare", FARE).withStyle(ChatFormatting.RED));
            return false;
        }
        BlockPos arrival = to.gateway() ? besideGateway(there, to.at()) : to.at();
        ServerLevel here = player.level();
        here.sendParticles(ParticleTypes.END_ROD, player.getX(), player.getY(1.0), player.getZ(), 30, 0.3, 0.8, 0.3, 0.05);
        here.playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.AMETHYST_BLOCK_RESONATE, SoundSource.PLAYERS, 1.0F, 0.7F);
        player.stopRiding();
        player.teleport(new TeleportTransition(there, Vec3.atBottomCenterOf(arrival), Vec3.ZERO, player.getYRot(), player.getXRot(),
                Relative.DELTA, TeleportTransition.PLACE_PORTAL_TICKET));
        player.resetFallDistance();
        there.sendParticles(ParticleTypes.END_ROD, player.getX(), player.getY(1.0), player.getZ(), 30, 0.3, 0.8, 0.3, 0.05);
        there.playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.AMETHYST_BLOCK_CHIME, SoundSource.PLAYERS, 1.0F, 1.2F);
        player.sendOverlayMessage(Component.translatable("message.hollowfrontier.waystone.arrived", to.label()).withStyle(ChatFormatting.AQUA));
        ModCriteria.fire(player, ModCriteria.WAYSTONE_TRAVELLED);
        return true;
    }

    private static boolean payFare(ServerPlayer player) {
        var inventory = player.getInventory();
        int have = 0;
        for (int slot = 0; slot < inventory.getContainerSize(); slot++) {
            if (inventory.getItem(slot).is(ModTags.Items.GEMS_PHLOGISTON)) {
                have += inventory.getItem(slot).getCount();
            }
        }
        if (have < FARE) {
            return false;
        }
        int owed = FARE;
        for (int slot = 0; slot < inventory.getContainerSize() && owed > 0; slot++) {
            ItemStack stack = inventory.getItem(slot);
            if (stack.is(ModTags.Items.GEMS_PHLOGISTON)) {
                int taken = Math.min(owed, stack.getCount());
                stack.shrink(taken);
                owed -= taken;
            }
        }
        return true;
    }

    /** The ground just outside a gateway's ring, south of its well. */
    static BlockPos besideGateway(ServerLevel level, BlockPos well) {
        int x = well.getX(), z = well.getZ() + GATEWAY_STANDOFF;
        level.getChunk(x >> 4, z >> 4); // generated, so the heightmap's true
        return new BlockPos(x, level.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, x, z), z);
    }
}
