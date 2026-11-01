package com.hollowfrontier.content.ufo;

import java.util.List;
import java.util.Optional;

import org.jspecify.annotations.Nullable;

import com.hollowfrontier.HollowFrontier;
import com.hollowfrontier.content.frontier.mystery.MysteryLog;
import com.hollowfrontier.content.frontier.raid.TownSite;
import com.hollowfrontier.content.hollow.gateway.HollowLanding;
import com.hollowfrontier.registry.ModBlocks;
import com.hollowfrontier.registry.ModCriteria;
import com.hollowfrontier.registry.ModEntities;
import com.hollowfrontier.registry.ModSounds;
import com.hollowfrontier.worldgen.HollowEarth;
import com.hollowfrontier.worldgen.structure.ModChestLoot;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.BlockTags;
import net.minecraft.util.RandomSource;
import net.minecraft.world.Difficulty;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.level.saveddata.SavedDataType;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.level.storage.loot.parameters.LootContextParamSets;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.tick.LevelTickEvent;

/**
 * Gray Landings (15a): with the leash broken the Grays take nobody, but they haven't gone. Every few nights, in the
 * Overworld and the Hollow Earth alike, one of their saucers sets down {@link #NEAR}-{@link #FAR} blocks from a player,
 * in a clearing it scorches ({@link #CLEARING} across) on level, open ground away from towns. Out step Gray Wardens and
 * drones ({@link #GUARD}) round a sealed {@link GrayCacheBlock salvage cache}.
 * <p>
 * The nights are the Overworld's, down below too (the inner sun never sets): from {@link #NIGHTFALL} to
 * {@link #LAST_CALL}, once a night per world, after {@link UfoConfig#landingInterval} nights, at
 * {@link UfoConfig#landingChance}. A landing lasts till dawn ({@link #day} turning over, sleeping included):
 * <ul>
 * <li>Every guard down before then ({@link #watch}): the saucer lifts off in a hurry and the cache is left unsealed, to
 *     be opened for salvage, phlogiston cells and now and then a whole part ({@link ModChestLoot#GRAY_LANDING}). Everyone near
 *     earns Trespassers.</li>
 * <li>Dawn first: the guards go back aboard and the cache with them, and it's gone.</li>
 * </ul>
 * The scorched ring stays either way. Overworld landings are logged with the frontier's mysteries, so the Gazette reports
 * them and the townsfolk talk.
 */
@EventBusSubscriber(modid = HollowFrontier.MODID)
public class GrayLandings extends SavedData {
    /** The tag every landing's guards carry. */
    public static final String GUARD = "hollowfrontier.landing_guard";

    /** The tag that ties a guard to its own saucer (two landings may come down within reach of each other). */
    public static String guardOf(GraySaucer saucer) {
        return GUARD + "." + saucer.getUUID();
    }
    public static final int NEAR = 40, FAR = 72;
    /** The scorched clearing's radius, and the open ground a landing needs (radius, and headroom over it). */
    public static final int CLEARING = 6, ROOM = 5, HEADROOM = 6;
    /** Most the ground may rise or fall across the clearing. */
    public static final int MAX_STEP = 2;
    /** How far from its saucer a guard still counts as keeping it. */
    public static final double GUARD_RANGE = 48.0;
    public static final long NIGHTFALL = 13000, LAST_CALL = 21000;
    public static final int WARDENS = 2, DRONES = 2;
    public static final double WITNESS_RANGE = 96.0;

    public static final Codec<GrayLandings> CODEC = RecordCodecBuilder.create(i -> i.group(
            Codec.LONG.optionalFieldOf("last_rolled", -1L).forGetter(l -> l.lastRolled),
            Codec.LONG.optionalFieldOf("last_landing", Long.MIN_VALUE / 2).forGetter(l -> l.lastLanding)).apply(i, GrayLandings::new));
    public static final SavedDataType<GrayLandings> TYPE = new SavedDataType<>(HollowFrontier.id("gray_landings"), GrayLandings::new, CODEC);

    /** The last day each world rolled for a landing, and the last it had one. */
    private long lastRolled = -1;
    private long lastLanding = Long.MIN_VALUE / 2;

    public GrayLandings() {}

    private GrayLandings(long lastRolled, long lastLanding) {
        this.lastRolled = lastRolled;
        this.lastLanding = lastLanding;
    }

    /** Each world keeps its own. */
    public static GrayLandings get(ServerLevel level) {
        return level.getDataStorage().computeIfAbsent(TYPE);
    }

    /** Days since the world began by the Overworld's clock, turning over at dawn. */
    public static long day(ServerLevel level) {
        return level.getServer().overworld().getDefaultClockTime() / 24000L;
    }

    /** Whether it's the part of the night a saucer may come down in. */
    public static boolean landingHours(ServerLevel level) {
        long time = level.getServer().overworld().getDefaultClockTime() % 24000L;
        return time >= NIGHTFALL && time < LAST_CALL;
    }

    @SubscribeEvent
    static void onLevelTick(LevelTickEvent.Post event) {
        if (!(event.getLevel() instanceof ServerLevel level) || level.getGameTime() % 100 != 0
                || (level.dimension() != Level.OVERWORLD && level.dimension() != HollowEarth.LEVEL)
                || !UfoConfig.landings() || !landingHours(level) || !Leash.isBroken(level)
                || level.getDifficulty() == Difficulty.PEACEFUL) { // its Wardens couldn't stay, and the cache would be free
            return;
        }
        List<ServerPlayer> players = level.players().stream().filter(p -> !p.isSpectator()).toList();
        if (players.isEmpty()) {
            return;
        }
        GrayLandings landings = get(level);
        long day = day(level);
        if (landings.lastRolled == day) {
            return;
        }
        landings.lastRolled = day;
        landings.setDirty();
        RandomSource random = level.getRandom();
        if (day - landings.lastLanding < UfoConfig.landingInterval() || random.nextDouble() >= UfoConfig.landingChance()) {
            return;
        }
        if (land(level, players.get(random.nextInt(players.size())).blockPosition(), random).isPresent()) {
            landings.lastLanding = day;
        }
    }

    /** A saucer coming down somewhere {@link #NEAR}-{@link #FAR} from {@code near}, if there's room for one. */
    public static Optional<GraySaucer> land(ServerLevel level, BlockPos near, RandomSource random) {
        return findClearing(level, near, NEAR, FAR, random).map(ground -> landAt(level, ground, day(level)));
    }

    /** Level, open, dry ground {@code min}-{@code max} from {@code near}, in loaded chunks and out of town. */
    public static Optional<BlockPos> findClearing(ServerLevel level, BlockPos near, int min, int max, RandomSource random) {
        for (int attempt = 0; attempt < 32; attempt++) {
            double angle = random.nextDouble() * Math.PI * 2, distance = min + random.nextDouble() * (max - min);
            int x = near.getX() + (int) Math.round(Math.cos(angle) * distance), z = near.getZ() + (int) Math.round(Math.sin(angle) * distance);
            BlockPos ground = floor(level, x, z, near.getY());
            if (ground != null && clear(level, ground)) {
                return Optional.of(ground);
            }
        }
        return Optional.empty();
    }

    /** The land at x/z: under the crust in the Hollow Earth (never on it), the surface elsewhere. */
    private static @Nullable BlockPos floor(ServerLevel level, int x, int z, int nearY) {
        if (!level.isLoaded(new BlockPos(x, nearY, z))) {
            return null;
        }
        if (level.dimension() == HollowEarth.LEVEL) {
            var column = HollowLanding.column(HollowLanding.of(level), x, z);
            return column.kind() == HollowLanding.Kind.LAND ? new BlockPos(x, column.floorY(), z) : null;
        }
        return new BlockPos(x, level.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, x, z) - 1, z);
    }

    /** Whether a saucer can set down on {@code ground}: solid, level and open all round, and no town about. */
    public static boolean clear(ServerLevel level, BlockPos ground) {
        if (TownSite.townAt(level, ground).isPresent()) {
            return false;
        }
        for (int dx = -ROOM; dx <= ROOM; dx++) {
            for (int dz = -ROOM; dz <= ROOM; dz++) {
                if (dx * dx + dz * dz > ROOM * ROOM) {
                    continue;
                }
                BlockPos top = top(level, ground.offset(dx, 0, dz));
                if (top == null || Math.abs(top.getY() - ground.getY()) > MAX_STEP) {
                    return false;
                }
                BlockState state = level.getBlockState(top);
                if (!state.getFluidState().isEmpty() || !state.isFaceSturdy(level, top, Direction.UP)) {
                    return false;
                }
                for (int y = 1; y <= HEADROOM; y++) {
                    if (!open(level.getBlockState(top.above(y)))) {
                        return false;
                    }
                }
            }
        }
        return true;
    }

    /** The top solid block in a column, within {@link #MAX_STEP} + 1 of {@code near}'s height. */
    private static @Nullable BlockPos top(ServerLevel level, BlockPos near) {
        for (int dy = MAX_STEP + 1; dy >= -MAX_STEP - 1; dy--) {
            BlockPos at = near.above(dy);
            BlockState state = level.getBlockState(at);
            if (!open(state)) {
                // a stump or trunk is no floor
                return !state.is(BlockTags.LOGS) && open(level.getBlockState(at.above())) ? at : null;
            }
        }
        return null;
    }

    /** Air, or what a landing flattens: grass, flowers, snow. Not leaves, logs or water. */
    private static boolean open(BlockState state) {
        return state.isAir() || state.canBeReplaced() && state.getFluidState().isEmpty() && !state.is(BlockTags.LEAVES);
    }

    /** A saucer coming down onto {@code ground}, from {@link GraySaucer#DESCENT_HEIGHT} up, to land on {@code day}. */
    public static GraySaucer landAt(ServerLevel level, BlockPos ground, long day) {
        GraySaucer saucer = ModEntities.GRAY_SAUCER.get().create(level, EntitySpawnReason.EVENT);
        double y = Math.min(ground.getY() + 1 + GraySaucer.HOVER + GraySaucer.DESCENT_HEIGHT, level.getMaxY() - 8);
        saucer.snapTo(ground.getX() + 0.5, Math.max(y, ground.getY() + 1 + GraySaucer.HOVER), ground.getZ() + 0.5, level.getRandom().nextFloat() * 360, 0);
        saucer.land(ground, day);
        level.addFreshEntity(saucer);
        level.playSound(null, saucer.getX(), saucer.getY(), saucer.getZ(), ModSounds.SIGHTING_HUM.get(), SoundSource.HOSTILE, 8.0F, 0.7F);
        if (level.dimension() == Level.OVERWORLD) {
            MysteryLog.get(level).record(MysteryLog.Kind.LANDING, level.getGameTime(), ground);
        }
        return saucer;
    }

    /** It's down: the clearing scorched, its guards out and its cache set beside it. */
    public static void touchdown(ServerLevel level, GraySaucer saucer) {
        BlockPos ground = saucer.ground();
        RandomSource random = level.getRandom();
        scorch(level, ground, random);
        Vec3 at = saucer.position();
        level.sendParticles(ParticleTypes.CAMPFIRE_COSY_SMOKE, at.x, ground.getY() + 1.2, at.z, 30, 3.0, 0.2, 3.0, 0.01);
        level.playSound(null, at.x, at.y, at.z, SoundEvents.BEACON_ACTIVATE, SoundSource.HOSTILE, 6.0F, 0.6F);
        level.playSound(null, at.x, at.y, at.z, SoundEvents.FIRE_EXTINGUISH, SoundSource.BLOCKS, 3.0F, 0.6F);

        double facing = random.nextDouble() * Math.PI * 2;
        BlockPos cache = surface(level, ground.offset((int) Math.round(Math.cos(facing) * 4.5), 0, (int) Math.round(Math.sin(facing) * 4.5)));
        level.setBlock(cache, ModBlocks.GRAY_CACHE.get().defaultBlockState(), Block.UPDATE_ALL);
        saucer.setCache(cache);

        int wardens = WARDENS + random.nextInt(2) + (level.getDifficulty() == Difficulty.HARD ? 1 : 0);
        for (int i = 0; i < wardens; i++) {
            double a = facing + Math.PI + (i - (wardens - 1) / 2.0) * 0.7;
            BlockPos spot = surface(level, ground.offset((int) Math.round(Math.cos(a) * 6), 0, (int) Math.round(Math.sin(a) * 6)));
            GrayWarden warden = ModEntities.GRAY_WARDEN.get().create(level, EntitySpawnReason.EVENT);
            warden.snapTo(spot.getX() + 0.5, spot.getY(), spot.getZ() + 0.5, (float) Math.toDegrees(a) - 90, 0);
            warden.setPersistenceRequired();
            warden.setHomeTo(ground, 16);
            warden.addTag(GUARD);
            warden.addTag(guardOf(saucer));
            level.addFreshEntity(warden);
        }
        for (int i = 0; i < DRONES; i++) {
            GrayDrone drone = GrayDrone.guard(level, saucer);
            drone.addTag(GUARD);
            drone.addTag(guardOf(saucer));
        }
    }

    /** The open block over the ground at x/z near {@code near}'s height (where something can stand). */
    private static BlockPos surface(ServerLevel level, BlockPos near) {
        BlockPos top = top(level, near);
        return top != null ? top.above() : near.above();
    }

    /** Burns the clearing: black glass-hard earth in the middle, coarse dirt round it, the grass and flowers gone. */
    private static void scorch(ServerLevel level, BlockPos ground, RandomSource random) {
        for (int dx = -CLEARING; dx <= CLEARING; dx++) {
            for (int dz = -CLEARING; dz <= CLEARING; dz++) {
                double r = Math.sqrt(dx * dx + dz * dz);
                if (r > CLEARING + 0.4 || (r > CLEARING - 1.5 && random.nextInt(3) == 0)) {
                    continue;
                }
                BlockPos top = top(level, ground.offset(dx, 0, dz));
                if (top == null) {
                    continue;
                }
                BlockPos over = top.above();
                if (!level.getBlockState(over).isAir() && open(level.getBlockState(over))) {
                    level.setBlock(over, Blocks.AIR.defaultBlockState(), Block.UPDATE_ALL);
                }
                BlockState state = level.getBlockState(top);
                if (state.is(BlockTags.SUBSTRATE_OVERWORLD) || state.is(BlockTags.DIRT) || state.is(Blocks.GRASS_BLOCK) || state.is(BlockTags.SAND)
                        || state.is(Blocks.GRAVEL) || state.is(ModBlocks.ASH.get())) {
                    Block burnt = r < 2.5 ? (random.nextBoolean() ? Blocks.BASALT : Blocks.BLACKSTONE)
                            : r < 4.5 && random.nextInt(3) == 0 ? Blocks.DYED_TERRACOTTA.black() : Blocks.COARSE_DIRT;
                    level.setBlock(top, burnt.defaultBlockState(), Block.UPDATE_ALL);
                }
            }
        }
    }

    /** A landed saucer's check, every second: dawn come, or every guard down. */
    public static void watch(ServerLevel level, GraySaucer saucer) {
        if (saucer.tickCount < 40) {
            return; // give its guards' chunks a moment to load along with it
        }
        List<LivingEntity> guards = guards(level, saucer);
        if (day(level) > saucer.landedDay()) {
            dawn(level, saucer, guards);
        } else if (guards.isEmpty()) {
            cleared(level, saucer);
        }
    }

    /** Its guards still up. */
    public static List<LivingEntity> guards(ServerLevel level, GraySaucer saucer) {
        return level.getEntitiesOfClass(LivingEntity.class, saucer.getBoundingBox().inflate(GUARD_RANGE),
                e -> e.isAlive() && e.entityTags().contains(guardOf(saucer)));
    }

    /** Whether a cache is still kept: its saucer is still down beside it (it stays till every guard is down, or dawn). */
    public static boolean sealed(ServerLevel level, BlockPos cache) {
        return !level.getEntitiesOfClass(GraySaucer.class, new AABB(cache).inflate(16), s -> s.isLanding() && cache.equals(s.cache())).isEmpty();
    }

    /** Dawn: everyone back aboard, the cache too, and away. */
    private static void dawn(ServerLevel level, GraySaucer saucer, List<LivingEntity> guards) {
        for (LivingEntity guard : guards) {
            level.sendParticles(ParticleTypes.END_ROD, guard.getX(), guard.getY(0.5), guard.getZ(), 12, 0.3, 0.6, 0.3, 0.02);
            guard.discard();
        }
        BlockPos cache = saucer.cache();
        if (cache != null && level.getBlockState(cache).is(ModBlocks.GRAY_CACHE.get())) {
            level.sendParticles(ParticleTypes.END_ROD, cache.getX() + 0.5, cache.getY() + 0.5, cache.getZ() + 0.5, 12, 0.3, 0.3, 0.3, 0.02);
            level.removeBlock(cache, false);
        }
        tell(level, saucer, "message.hollowfrontier.landing.dawn");
        saucer.liftOff(level, randomHeading(level.getRandom()));
    }

    /** Every guard down: off it goes in a hurry, leaving the cache. */
    private static void cleared(ServerLevel level, GraySaucer saucer) {
        tell(level, saucer, "message.hollowfrontier.landing.cleared");
        for (ServerPlayer player : level.getEntitiesOfClass(ServerPlayer.class, saucer.getBoundingBox().inflate(WITNESS_RANGE), p -> !p.isSpectator())) {
            ModCriteria.fire(player, ModCriteria.LANDING_CLEARED);
        }
        saucer.liftOff(level, randomHeading(level.getRandom()));
    }

    private static void tell(ServerLevel level, GraySaucer saucer, String key) {
        for (ServerPlayer player : level.getEntitiesOfClass(ServerPlayer.class, saucer.getBoundingBox().inflate(WITNESS_RANGE), p -> !p.isSpectator())) {
            player.sendSystemMessage(Component.translatable(key).withStyle(ChatFormatting.GRAY, ChatFormatting.ITALIC));
        }
    }

    private static Vec3 randomHeading(RandomSource random) {
        double a = random.nextDouble() * Math.PI * 2;
        return new Vec3(Math.cos(a), 0, Math.sin(a));
    }

    /** Opens an unsealed cache: its salvage spills out and it's gone. */
    public static void open(ServerLevel level, BlockPos cache, Player player) {
        LootParams params = new LootParams.Builder(level)
                .withParameter(LootContextParams.ORIGIN, Vec3.atCenterOf(cache))
                .withParameter(LootContextParams.THIS_ENTITY, player)
                .create(LootContextParamSets.CHEST);
        for (ItemStack stack : level.getServer().reloadableRegistries().getLootTable(ModChestLoot.GRAY_LANDING).getRandomItems(params)) {
            Block.popResource(level, cache, stack);
        }
        level.removeBlock(cache, false);
        level.sendParticles(ParticleTypes.END_ROD, cache.getX() + 0.5, cache.getY() + 0.6, cache.getZ() + 0.5, 10, 0.3, 0.3, 0.3, 0.03);
        level.playSound(null, cache, SoundEvents.IRON_TRAPDOOR_OPEN, SoundSource.BLOCKS, 1.0F, 0.6F);
        level.playSound(null, cache, SoundEvents.BEACON_DEACTIVATE, SoundSource.BLOCKS, 0.8F, 1.6F);
    }
}
