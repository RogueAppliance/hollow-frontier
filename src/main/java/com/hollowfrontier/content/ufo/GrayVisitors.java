package com.hollowfrontier.content.ufo;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import org.jspecify.annotations.Nullable;

import com.hollowfrontier.HollowFrontier;
import com.hollowfrontier.content.frontier.raid.TownSite;
import com.hollowfrontier.content.frontier.saloon.Barmaid;
import com.hollowfrontier.registry.ModEntities;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import net.minecraft.core.BlockPos;
import net.minecraft.core.UUIDUtil;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.level.saveddata.SavedDataType;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.tick.ServerTickEvent;

/**
 * Grays in disguise (21c): with the leash broken, now and then a stranded Gray in a duster and a hat too big for it
 * ({@link DisguisedGray}) turns up in a frontier town of an evening, mostly at the saloon, and stays a while. Each town
 * keeps count (saved with the Overworld): a visit that ends with the stranger unexposed counts, and after
 * {@link #VISITS_TO_STAY} of them it stays for good, the saloon's regular. Exposing one sets the count back to nothing;
 * exposing the regular loses it. A town with a regular gets no more strangers.
 * <p>
 * The roll: every {@link #CHECK_EVERY} ticks, for each player in a town in the Overworld, in the evening hours
 * ({@link #EVENING}-{@link #LATE}), at most one visit a day per town, at {@link #CHANCE}.
 */
@EventBusSubscriber(modid = HollowFrontier.MODID)
public class GrayVisitors extends SavedData {
    public static final int VISITS_TO_STAY = 3;
    public static final int CHECK_EVERY = 600;
    public static final float CHANCE = 0.15F;
    public static final long EVENING = 11500, LATE = 22000;
    public static final double SALOON_REACH = 48.0;

    /** One town's count, its regular, and the day a stranger last came. */
    public static final class Town {
        static final Codec<Town> CODEC = RecordCodecBuilder.create(i -> i.group(
                Codec.INT.optionalFieldOf("visits", 0).forGetter(t -> t.visits),
                UUIDUtil.CODEC.optionalFieldOf("regular").forGetter(t -> t.regular),
                Codec.LONG.optionalFieldOf("last_day", -1L).forGetter(t -> t.lastDay)).apply(i, Town::new));

        int visits;
        Optional<UUID> regular;
        long lastDay;

        Town(int visits, Optional<UUID> regular, long lastDay) {
            this.visits = visits;
            this.regular = regular;
            this.lastDay = lastDay;
        }

        public int visits() {
            return visits;
        }

        public Optional<UUID> regular() {
            return regular;
        }
    }

    public static final Codec<GrayVisitors> CODEC = Codec.unboundedMap(Codec.STRING, Town.CODEC).xmap(GrayVisitors::new, v -> v.byName());
    public static final SavedDataType<GrayVisitors> TYPE = new SavedDataType<>(HollowFrontier.id("gray_visitors"), GrayVisitors::new, CODEC);

    private final Map<Long, Town> towns = new HashMap<>();

    public GrayVisitors() {}

    private GrayVisitors(Map<String, Town> saved) {
        saved.forEach((key, town) -> towns.put(Long.parseLong(key), town));
    }

    private Map<String, Town> byName() {
        Map<String, Town> out = new HashMap<>();
        towns.forEach((key, town) -> out.put(Long.toString(key), town));
        return out;
    }

    public static GrayVisitors get(ServerLevel level) {
        return level.getServer().overworld().getDataStorage().computeIfAbsent(TYPE);
    }

    public Town town(long key) {
        return towns.computeIfAbsent(key, k -> new Town(0, Optional.empty(), -1));
    }

    /** A visit ended with the stranger unexposed: one more counted. Whether this one now stays as the regular. */
    public boolean visitEnded(long key, UUID stranger) {
        Town town = town(key);
        town.visits++;
        boolean stays = town.regular.isEmpty() && town.visits >= VISITS_TO_STAY;
        if (stays) {
            town.regular = Optional.of(stranger);
        }
        setDirty();
        return stays;
    }

    /** A stranger exposed in this town: the count starts over, and if it was the regular, the town has none now. */
    public void exposed(long key, UUID stranger) {
        Town town = town(key);
        town.visits = 0;
        town.regular = town.regular.filter(r -> !r.equals(stranger));
        setDirty();
    }

    /** The regular is gone some other way (killed): the town has none. */
    public void regularGone(long key, UUID stranger) {
        Town town = town(key);
        if (town.regular.filter(stranger::equals).isPresent()) {
            town.regular = Optional.empty();
            setDirty();
        }
    }

    @SubscribeEvent
    static void onServerTick(ServerTickEvent.Post event) {
        if (event.getServer().getTickCount() % CHECK_EVERY != 0) {
            return;
        }
        ServerLevel level = event.getServer().overworld();
        if (!mayCome(level)) {
            return;
        }
        long time = level.getDefaultClockTime() % 24000L;
        if (time < EVENING || time >= LATE) {
            return;
        }
        for (ServerPlayer player : level.players()) {
            if (player.isSpectator() || level.getRandom().nextFloat() >= CHANCE) {
                continue;
            }
            TownSite.townAt(level, player.blockPosition()).ifPresent(start -> {
                long key = TownSite.keyOf(start);
                GrayVisitors visitors = get(level);
                Town town = visitors.town(key);
                if (town.regular.isPresent() || town.lastDay == GrayLandings.day(level)
                        || !level.getEntitiesOfClass(DisguisedGray.class, player.getBoundingBox().inflate(128), g -> g.town() == key).isEmpty()) {
                    return;
                }
                BlockPos at = arrival(level, player);
                if (at != null) {
                    visit(level, key, at);
                }
            });
        }
    }

    /** Whether strangers come at all: only once the leash is broken, with nothing left holding the Grays together. */
    public static boolean mayCome(ServerLevel level) {
        return Leash.isBroken(level);
    }

    /** Where a stranger walks in: by the saloon's barmaid near the player if there is one, else a little way off the player. */
    static @Nullable BlockPos arrival(ServerLevel level, ServerPlayer player) {
        var barmaid = level.getEntitiesOfClass(Barmaid.class, player.getBoundingBox().inflate(SALOON_REACH), Entity::isAlive).stream().findFirst();
        if (barmaid.isPresent()) {
            return barmaid.get().blockPosition().offset(level.getRandom().nextInt(5) - 2, 0, level.getRandom().nextInt(5) - 2);
        }
        double angle = level.getRandom().nextDouble() * Math.PI * 2;
        int x = player.getBlockX() + (int) (Math.cos(angle) * 16), z = player.getBlockZ() + (int) (Math.sin(angle) * 16);
        BlockPos ground = level.getHeightmapPos(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, new BlockPos(x, 0, z));
        return TownSite.townAt(level, ground).isPresent() ? ground : null;
    }

    /** A stranger walks into the town under {@code key}, at {@code at}, and stays a while. */
    public static @Nullable DisguisedGray visit(ServerLevel level, long key, BlockPos at) {
        DisguisedGray gray = ModEntities.DISGUISED_GRAY.get().create(level, EntitySpawnReason.EVENT);
        if (gray == null) {
            return null;
        }
        gray.snapTo(at.getX() + 0.5, at.getY(), at.getZ() + 0.5, level.getRandom().nextFloat() * 360.0F, 0.0F);
        gray.startVisit(key, at);
        level.addFreshEntity(gray);
        GrayVisitors visitors = get(level);
        visitors.town(key).lastDay = GrayLandings.day(level);
        visitors.setDirty();
        return gray;
    }
}
