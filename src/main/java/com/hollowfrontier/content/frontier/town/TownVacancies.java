package com.hollowfrontier.content.frontier.town;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.jspecify.annotations.Nullable;

import com.hollowfrontier.HollowFrontier;
import com.hollowfrontier.content.frontier.raid.TownRaids;
import com.hollowfrontier.content.frontier.raid.TownSite;
import com.hollowfrontier.content.frontier.townsfolk.Folk;
import com.hollowfrontier.content.frontier.townsfolk.Lawman;
import com.hollowfrontier.content.frontier.townsfolk.Townsfolk;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.level.levelgen.structure.StructureStart;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.level.saveddata.SavedDataType;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.LivingDeathEvent;
import net.neoforged.neoforge.event.tick.LevelTickEvent;

/**
 * The places towns have lost people from, saved with the Overworld. When a resident dies (a townsperson or lawman the
 * town started with, or a replacement for one) their job and post go on their town's list, and someone new arrives at
 * that post after a while: one at a time, never during a raid, and only where no player would see them appear. The
 * list only ever holds who died, so a town can't grow past what it started with. The spawn egg's folk, freed men and
 * posse riders aren't residents and leave no place behind.
 */
@EventBusSubscriber(modid = HollowFrontier.MODID)
public class TownVacancies extends SavedData {
    private static final int CHECK_EVERY = 100;
    // How far off a post a replacement may stand if it's blocked or in view.
    private static final int NEAR_POST = 3;
    // Nobody appears this close to a player, seen or not, or anywhere a player within SIGHT can see.
    private static final double PRIVACY = 8.0, SIGHT = 96.0;

    /** Someone a town has lost: what they were (a Folk or Rank id, by type) and where they kept. */
    public record Vacancy(EntityType<?> type, BlockPos post, String role) {
        static final Codec<Vacancy> CODEC = RecordCodecBuilder.create(i -> i.group(
                EntityType.CODEC.fieldOf("type").forGetter(Vacancy::type),
                BlockPos.CODEC.fieldOf("post").forGetter(Vacancy::post),
                Codec.STRING.fieldOf("role").forGetter(Vacancy::role)).apply(i, Vacancy::new));
    }

    /** One town's list, and since when it has been waiting: its latest arrival, or the death that started the list. */
    private static final class Town {
        static final Codec<Town> CODEC = RecordCodecBuilder.create(i -> i.group(
                BoundingBox.CODEC.fieldOf("bounds").forGetter(t -> t.bounds),
                Codec.LONG.fieldOf("since").forGetter(t -> t.since),
                Vacancy.CODEC.listOf().fieldOf("open").forGetter(t -> t.open)).apply(i, Town::new));

        final BoundingBox bounds;
        long since;
        final List<Vacancy> open;

        Town(BoundingBox bounds, long since, List<Vacancy> open) {
            this.bounds = bounds;
            this.since = since;
            this.open = new ArrayList<>(open);
        }
    }

    public static final Codec<TownVacancies> CODEC = RecordCodecBuilder.create(i -> i.group(
            Codec.unboundedMap(Codec.STRING, Town.CODEC).fieldOf("towns").forGetter(TownVacancies::townsByName)).apply(i, TownVacancies::new));
    public static final SavedDataType<TownVacancies> TYPE = new SavedDataType<>(HollowFrontier.id("town_vacancies"), TownVacancies::new, CODEC);

    private final Map<Long, Town> towns = new HashMap<>();

    public TownVacancies() {}

    private TownVacancies(Map<String, Town> towns) {
        towns.forEach((key, town) -> this.towns.put(Long.parseLong(key), town));
    }

    private Map<String, Town> townsByName() {
        Map<String, Town> out = new HashMap<>();
        towns.forEach((key, town) -> out.put(Long.toString(key), town));
        return out;
    }

    public static TownVacancies get(ServerLevel level) {
        return level.getServer().overworld().getDataStorage().computeIfAbsent(TYPE);
    }

    /** The town's open places, the longest waiting first. */
    public List<Vacancy> open(long key) {
        Town town = towns.get(key);
        return town == null ? List.of() : List.copyOf(town.open);
    }

    /** Starts keeping a list for a town (real towns get theirs at their first death; game tests name their own). */
    public void settle(long key, BoundingBox bounds) {
        towns.computeIfAbsent(key, k -> new Town(bounds, 0, List.of()));
        setDirty();
    }

    /** Stops keeping a town's list (for game tests, whose towns go away). */
    public void forget(long key) {
        towns.remove(key);
        setDirty();
    }

    // ---------------------------------------------------------------- losing someone

    // Last, so anything that saves them from dying gets its say first.
    @SubscribeEvent(priority = EventPriority.LOWEST)
    static void onDeath(LivingDeathEvent event) {
        if (event.getEntity().level() instanceof ServerLevel level && level.dimension() == Level.OVERWORLD) {
            vacancyOf(event.getEntity()).ifPresent(vacancy -> get(level).lose(level, vacancy));
        }
    }

    private static Optional<Vacancy> vacancyOf(LivingEntity entity) {
        if (entity instanceof Townsfolk folk && folk.isResident()) {
            BlockPos post = folk.getWorkplace() != null ? folk.getWorkplace() : folk.blockPosition();
            return Optional.of(new Vacancy(folk.getType(), post, folk.getFolk().id()));
        }
        if (entity instanceof Lawman lawman && lawman.isResident()) {
            BlockPos post = lawman.hasHome() ? lawman.getHomePosition() : lawman.blockPosition();
            return Optional.of(new Vacancy(lawman.getType(), post, lawman.getRank().id()));
        }
        return Optional.empty();
    }

    private void lose(ServerLevel level, Vacancy vacancy) {
        Town town = townAt(level, vacancy.post());
        if (town != null) {
            if (town.open.isEmpty()) {
                town.since = level.getGameTime();
            }
            town.open.add(vacancy);
            setDirty();
        }
    }

    // Null if the post isn't in a town (the folk held in Hollow Earth camps, say).
    private @Nullable Town townAt(ServerLevel level, BlockPos post) {
        for (Town town : towns.values()) {
            if (town.bounds.isInside(post)) {
                return town;
            }
        }
        Optional<StructureStart> start = TownSite.townAt(level, post);
        if (start.isEmpty()) {
            return null;
        }
        return towns.computeIfAbsent(TownSite.keyOf(start.get()), key -> new Town(start.get().getBoundingBox(), 0, List.of()));
    }

    // ---------------------------------------------------------------- arrivals

    @SubscribeEvent
    static void onLevelTick(LevelTickEvent.Post event) {
        if (event.getLevel() instanceof ServerLevel level && level.dimension() == Level.OVERWORLD
                && level.getGameTime() % CHECK_EVERY == 0 && TownConfig.arrivals()) {
            get(level).check(level, TownConfig.arrivalTime());
        }
    }

    /** One arrival, for each town that has waited this long, has no raid on and has a post it can fill unseen. */
    public void check(ServerLevel level, int wait) {
        long now = level.getGameTime();
        towns.forEach((key, town) -> {
            if (!town.open.isEmpty() && now - town.since >= wait && TownRaids.get(level).raidIn(key) == null) {
                arrive(level, town);
            }
        });
    }

    private void arrive(ServerLevel level, Town town) {
        for (Vacancy vacancy : town.open) {
            Optional<BlockPos> spot = spotFor(level, vacancy);
            if (spot.isPresent() && spawn(level, vacancy, spot.get())) {
                town.open.remove(vacancy);
                town.since = level.getGameTime();
                setDirty();
                return;
            }
        }
    }

    // The post or near it, unseen. Only if there's no room there at all (the building's gone) the ground over it, so a
    // player in the shop keeps the new shopkeeper waiting rather than sending them up onto the roof. An unloaded post
    // waits for a player to come by.
    private static Optional<BlockPos> spotFor(ServerLevel level, Vacancy vacancy) {
        BlockPos post = vacancy.post();
        if (!level.isPositionEntityTicking(post)) {
            return Optional.empty();
        }
        List<BlockPos> room = roomAround(level, vacancy.type(), post);
        if (room.isEmpty()) {
            room = roomAround(level, vacancy.type(), level.getHeightmapPos(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, post));
        }
        return room.stream().filter(pos -> unseen(level, pos)).findFirst();
    }

    // The spots near center that someone fits, the nearest first.
    private static List<BlockPos> roomAround(ServerLevel level, EntityType<?> type, BlockPos center) {
        List<BlockPos> room = new ArrayList<>();
        for (BlockPos pos : BlockPos.withinManhattan(center, NEAR_POST)) {
            if (fits(level, type, pos)) {
                room.add(pos.immutable());
            }
        }
        return room;
    }

    private static boolean fits(ServerLevel level, EntityType<?> type, BlockPos pos) {
        AABB box = type.getSpawnAABB(pos.getX() + 0.5, pos.getY(), pos.getZ() + 0.5);
        return level.noCollision(box) && !level.containsAnyLiquid(box) && !level.getBlockState(pos.below()).getCollisionShape(level, pos.below()).isEmpty();
    }

    private static boolean unseen(ServerLevel level, BlockPos pos) {
        Vec3 feet = Vec3.atCenterOf(pos);
        Vec3 head = Vec3.atCenterOf(pos.above());
        for (ServerPlayer player : level.players()) {
            if (!player.isSpectator() && (player.position().closerThan(feet, PRIVACY) || sees(level, player, feet) || sees(level, player, head))) {
                return false;
            }
        }
        return true;
    }

    // Glass and bars don't hide anyone, so this goes by what blocks the view, not what blocks a walker.
    private static boolean sees(ServerLevel level, ServerPlayer player, Vec3 target) {
        Vec3 eye = player.getEyePosition();
        return eye.closerThan(target, SIGHT)
                && level.clip(new ClipContext(eye, target, ClipContext.Block.VISUAL, ClipContext.Fluid.NONE, player)).getType() == HitResult.Type.MISS;
    }

    private static boolean spawn(ServerLevel level, Vacancy vacancy, BlockPos spot) {
        Entity entity = vacancy.type().create(level, EntitySpawnReason.EVENT);
        if (!(entity instanceof Mob mob)) {
            return false;
        }
        mob.snapTo(spot.getX() + 0.5, spot.getY(), spot.getZ() + 0.5, level.getRandom().nextFloat() * 360.0F, 0.0F);
        if (mob instanceof Townsfolk folk) {
            folk.setFolk(Folk.byId(vacancy.role()));
            folk.setWorkplace(vacancy.post());
        } else if (mob instanceof Lawman lawman) {
            lawman.setRank(Lawman.Rank.byId(vacancy.role()));
        }
        mob.finalizeSpawn(level, level.getCurrentDifficultyAt(spot), EntitySpawnReason.EVENT, null);
        if (mob instanceof Lawman lawman) {
            lawman.setHomeTo(vacancy.post(), lawman.getHomeRadius());
        }
        return level.addFreshEntity(mob);
    }
}
