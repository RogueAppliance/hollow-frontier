package com.hollowfrontier.content.frontier.weather;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import com.hollowfrontier.HollowFrontier;
import com.hollowfrontier.content.hollow.creature.HollowCreatures;
import com.hollowfrontier.registry.ModEntities;
import com.hollowfrontier.worldgen.HollowEarth;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.TagKey;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.level.saveddata.SavedDataType;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.level.storage.loot.LootTable;
import net.minecraft.world.level.storage.loot.parameters.LootContextParamSets;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.RegisterCommandsEvent;
import net.neoforged.neoforge.event.tick.LevelTickEvent;

/**
 * Where and when {@link DustDevil}s spin up (29), and where they've been. Every {@link DustDevilConfig#rollInterval}
 * ticks each player gets a {@link DustDevilConfig#chance} roll for one {@link #NEAR_MIN}-{@link #NEAR_MAX} blocks off: in
 * the Overworld by day, in clear weather, in {@link #HAS_DUST_DEVILS} country; in the Hollow Earth an Ash Devil, at any
 * hour, in {@link #HAS_ASH_DEVILS} country. Not more than {@link #CROWD} near one player at once. Their own spawner, so
 * they take nobody's place in the mob caps.
 * <p>
 * Each Overworld devil is logged where it starts and where it dies down (the last {@link Passes#KEPT}, saved with the
 * Overworld), so the townsfolk can talk about the one that came through today ({@link #cameThrough}).
 * {@code /hollowfrontier dustdevil [ash]} spins one up in front of the runner (ops).
 */
@EventBusSubscriber(modid = HollowFrontier.MODID)
public final class DustDevils {
    private DustDevils() {}

    public static final TagKey<Biome> HAS_DUST_DEVILS = TagKey.create(Registries.BIOME, HollowFrontier.id("has_dust_devils"));
    public static final TagKey<Biome> HAS_ASH_DEVILS = TagKey.create(Registries.BIOME, HollowFrontier.id("has_ash_devils"));
    public static final ResourceKey<LootTable> LOOT = ResourceKey.create(Registries.LOOT_TABLE, HollowFrontier.id("gameplay/dust_devil"));
    public static final int NEAR_MIN = 24, NEAR_MAX = 48, CROWD = 2;
    /** How near, and how lately, a devil has to have passed for the townsfolk to talk about it. */
    public static final double TALK_RANGE = 96;
    public static final long TALK_WINDOW = 24000L;

    @SubscribeEvent
    static void onLevelTick(LevelTickEvent.Post event) {
        if (!(event.getLevel() instanceof ServerLevel level) || !DustDevilConfig.enabled() || level.getGameTime() % DustDevilConfig.rollInterval() != 0) {
            return;
        }
        boolean ash = level.dimension() == HollowEarth.LEVEL;
        if (!ash && (level.dimension() != Level.OVERWORLD || level.isDarkOutside() || level.isRaining())) {
            return;
        }
        RandomSource random = level.getRandom();
        for (ServerPlayer player : level.players()) {
            if (player.isSpectator() || random.nextDouble() >= DustDevilConfig.chance()
                    || level.getEntitiesOfClass(DustDevil.class, player.getBoundingBox().inflate(NEAR_MAX * 2)).size() >= CROWD) {
                continue;
            }
            double angle = random.nextDouble() * Math.PI * 2, distance = NEAR_MIN + random.nextDouble() * (NEAR_MAX - NEAR_MIN);
            int x = Mth.floor(player.getX() + Math.cos(angle) * distance), z = Mth.floor(player.getZ() + Math.sin(angle) * distance);
            if (!level.hasChunk(x >> 4, z >> 4)) {
                continue;
            }
            ground(level, x, z, Mth.floor(player.getY()), ash)
                    .filter(foot -> canBlowAt(level, foot, ash))
                    .ifPresent(foot -> spinUp(level, Vec3.atBottomCenterOf(foot), ash, random));
        }
    }

    /** The foot of a devil at x, z: on the ground, out in the open (the Hollow Earth's ground found under the crust). */
    public static Optional<BlockPos> ground(ServerLevel level, int x, int z, int nearY, boolean hollow) {
        int y = hollow ? HollowCreatures.groundBelow(level, x, z, nearY + 16) : level.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, x, z) - 1;
        if (y < nearY - 40) {
            return Optional.empty();
        }
        BlockPos foot = new BlockPos(x, y + 1, z);
        return level.getBlockState(foot).isAir() && level.getBlockState(foot.above()).isAir() ? Optional.of(foot) : Optional.empty();
    }

    /** Whether a devil can blow here: dry ground in its own country (and, over the Overworld, a clear sky by day). */
    public static boolean canBlowAt(ServerLevel level, BlockPos foot, boolean ash) {
        if (!level.getFluidState(foot).isEmpty() || !level.getFluidState(foot.below()).isEmpty() && !ash) {
            return false;
        }
        if (ash) {
            return level.getBiome(foot).is(HAS_ASH_DEVILS);
        }
        return level.getBiome(foot).is(HAS_DUST_DEVILS) && !level.isRaining() && !level.isDarkOutside();
    }

    public static DustDevil spinUp(ServerLevel level, Vec3 at, boolean ash, RandomSource random) {
        EntityType<DustDevil> type = ash ? ModEntities.ASH_DEVIL.get() : ModEntities.DUST_DEVIL.get();
        DustDevil devil = DustDevil.spawn(level, type, at, DustDevil.MIN_LIFE + random.nextInt(DustDevil.MAX_LIFE - DustDevil.MIN_LIFE + 1));
        if (!ash) {
            record(level, BlockPos.containing(at));
        }
        return devil;
    }

    /** What a dust devil leaves where it dies down. */
    public static List<ItemStack> loot(ServerLevel level, Vec3 at) {
        LootParams params = new LootParams.Builder(level).withParameter(LootContextParams.ORIGIN, at).create(LootContextParamSets.CHEST);
        return level.getServer().reloadableRegistries().getLootTable(LOOT).getRandomItems(params);
    }

    // ---------------------------------------------------------------- where they've been

    public static void record(ServerLevel level, BlockPos pos) {
        if (level.dimension() == Level.OVERWORLD) {
            Passes.get(level).add(level.getGameTime(), pos);
        }
    }

    /** Whether a dust devil passed within {@link #TALK_RANGE} of {@code pos} in the last day. */
    public static boolean cameThrough(ServerLevel level, BlockPos pos) {
        long now = level.getGameTime();
        return Passes.get(level).passes.stream().anyMatch(p -> now - p.time() <= TALK_WINDOW && Math.hypot(pos.getX() - p.x(), pos.getZ() - p.z()) <= TALK_RANGE);
    }

    public record Pass(long time, int x, int z) {
        public static final Codec<Pass> CODEC = RecordCodecBuilder.create(i -> i.group(
                Codec.LONG.fieldOf("time").forGetter(Pass::time),
                Codec.INT.fieldOf("x").forGetter(Pass::x),
                Codec.INT.fieldOf("z").forGetter(Pass::z)).apply(i, Pass::new));
    }

    /** The last few places dust devils were, saved with the Overworld. */
    public static class Passes extends SavedData {
        public static final int KEPT = 16;
        public static final Codec<Passes> CODEC = Pass.CODEC.listOf().xmap(Passes::new, p -> List.copyOf(p.passes)).fieldOf("passes").codec();
        public static final SavedDataType<Passes> TYPE = new SavedDataType<>(HollowFrontier.id("dust_devils"), Passes::new, CODEC);

        private final List<Pass> passes = new ArrayList<>();

        public Passes() {}

        private Passes(List<Pass> passes) {
            this.passes.addAll(passes);
        }

        public static Passes get(ServerLevel level) {
            return level.getServer().overworld().getDataStorage().computeIfAbsent(TYPE);
        }

        void add(long time, BlockPos pos) {
            passes.add(new Pass(time, pos.getX(), pos.getZ()));
            while (passes.size() > KEPT) {
                passes.removeFirst();
            }
            setDirty();
        }
    }

    // ---------------------------------------------------------------- the command

    @SubscribeEvent
    static void onRegisterCommands(RegisterCommandsEvent event) {
        register(event.getDispatcher());
    }

    static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal("hollowfrontier")
                .requires(Commands.hasPermission(Commands.LEVEL_GAMEMASTERS))
                .then(Commands.literal("dustdevil")
                        .executes(ctx -> command(ctx.getSource(), false))
                        .then(Commands.literal("ash").executes(ctx -> command(ctx.getSource(), true)))));
    }

    private static int command(CommandSourceStack source, boolean ash) {
        ServerLevel level = source.getLevel();
        Vec3 at = source.getPlayer() != null ? DustDevil.inFront(source.getPlayer(), 8) : source.getPosition();
        Optional<BlockPos> foot = ground(level, Mth.floor(at.x), Mth.floor(at.z), Mth.floor(at.y), level.dimension() == HollowEarth.LEVEL);
        spinUp(level, foot.map(Vec3::atBottomCenterOf).orElse(at), ash, level.getRandom()).tame();
        source.sendSuccess(() -> Component.translatable("commands.hollowfrontier.dustdevil" + (ash ? ".ash" : "")), true);
        return 1;
    }
}
