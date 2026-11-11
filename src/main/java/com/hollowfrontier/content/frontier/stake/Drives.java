package com.hollowfrontier.content.frontier.stake;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.jspecify.annotations.Nullable;

import com.hollowfrontier.content.frontier.bounty.Purse;
import com.hollowfrontier.content.frontier.raid.TownSite;
import com.hollowfrontier.content.frontier.town.Towns;
import com.hollowfrontier.registry.ModAttachments;
import com.hollowfrontier.registry.ModCriteria;
import com.hollowfrontier.registry.ModEntities;
import com.hollowfrontier.registry.ModSounds;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.UUIDUtil;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.entity.animal.cow.Cow;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.phys.AABB;

/**
 * Cattle drives (47c). A town's wrangler (sneak and use him) turns a herd of his out to a player the town knows
 * ({@link Standing.Rank#REGULAR}): 8 to 16 head to take to the next town, or, where no town is in reach, to a buyer
 * waiting a way down the trail. The pay is per head that arrives with the drover, by how far the drive was. He also buys
 * a player's own branded cattle brought to him, paying more the farther they've come from where they were branded.
 */
public final class Drives {
    private Drives() {}

    public static final int MIN_HEAD = 8, MAX_HEAD = 16;
    /** Copper per head for each this many blocks of a drive, and the least a head pays. */
    public static final int BLOCKS_PER_COPPER = 60, MIN_PER_HEAD = 3;
    /** Selling branded stock: what a head is worth at home, and the blocks that add a copper to it. */
    public static final int SALE_BASE = 4, SALE_BLOCKS_PER_COPPER = 80;
    /** How near the drover a cow has to be to count as arrived, or to be sold. */
    public static final double ARRIVE_RANGE = 48.0, SALE_RANGE = 10.0;
    /** How near a trail's end counts as there, and how far off a trail's end is put. */
    public static final double TRAIL_END_RANGE = 24.0;
    public static final int TRAIL_LENGTH = 900;
    public static final int AMBUSH_EVERY = 2400;
    public static final float AMBUSH_CHANCE = 0.25F;
    public static final int LOST_STANDING = 3;

    /** A drive in hand, on the player: the herd's id, where it left, where it's bound ({@code to} is the town's key if it's a town). */
    public record Drive(UUID id, long from, Optional<Long> to, BlockPos dest, int head) {
        public static final Codec<Drive> CODEC = RecordCodecBuilder.create(i -> i.group(
                UUIDUtil.CODEC.fieldOf("id").forGetter(Drive::id),
                Codec.LONG.fieldOf("from").forGetter(Drive::from),
                Codec.LONG.optionalFieldOf("to").forGetter(Drive::to),
                BlockPos.CODEC.fieldOf("dest").forGetter(Drive::dest),
                Codec.INT.fieldOf("head").forGetter(Drive::head)).apply(i, Drive::new));
        public static final Drive NONE = new Drive(new UUID(0, 0), 0, Optional.empty(), BlockPos.ZERO, 0);
    }

    public static @Nullable Drive of(Player player) {
        return player.hasData(ModAttachments.DRIVE.get()) ? player.getData(ModAttachments.DRIVE.get()) : null;
    }

    /** What a head pays for a drive of this many blocks. */
    public static int perHead(int blocks) {
        return Math.max(MIN_PER_HEAD, blocks / BLOCKS_PER_COPPER);
    }

    /** What a branded head sells for, this many blocks from where it was branded. */
    public static int salePrice(int blocks) {
        return SALE_BASE + blocks / SALE_BLOCKS_PER_COPPER;
    }

    // ---------------------------------------------------------------- at the wrangler

    /** A player sneaking up to a wrangler. Whether it was cattle business (otherwise he trades as usual). */
    public static boolean atWrangler(ServerLevel level, ServerPlayer player, Entity wrangler) {
        if (!StakeConfig.drives()) {
            return false;
        }
        BlockPos here = wrangler.blockPosition();
        Optional<Long> town = Towns.near(level, here, 32).map(TownSite::keyOf);
        Drive drive = of(player);
        if (drive != null) {
            if (town.isPresent() && drive.to().isPresent() && drive.to().get().equals(town.get())) {
                arrive(level, player, drive);
            } else {
                StakeText.say(player, wrangler, StakeText.DRIVE_BUSY, StakeText.paces(here, drive.dest()), StakeText.direction(here, drive.dest()));
            }
            return true;
        }
        if (sell(level, player, wrangler, town)) {
            return true;
        }
        if (town.isEmpty()) {
            StakeText.say(player, wrangler, StakeText.WRANGLER_NOTHING);
            return true;
        }
        if (StakeConfig.standing() && !Standing.rank(player, town.get()).atLeast(Standing.Rank.REGULAR)) {
            StakeText.say(player, wrangler, StakeText.DRIVE_STANDING);
            return true;
        }
        Optional<Neighbours.Neighbour> next = Neighbours.of(level, town.get()).filter(n -> StakeText.paces(Towns.middle(town.get()), n.pos()) <= StakeConfig.driveReach());
        int head = MIN_HEAD + level.getRandom().nextInt(MAX_HEAD - MIN_HEAD + 1);
        BlockPos dest = next.map(Neighbours.Neighbour::pos).orElseGet(() -> trailEnd(level, town.get()));
        BlockPos held = turnOut(level, here, town.get(), dest);
        Drive given = start(level, player, held, town.get(), next.map(Neighbours.Neighbour::key), dest, head);
        if (given == null) {
            StakeText.say(player, wrangler, StakeText.DRIVE_NO_ROOM);
        } else if (next.isPresent()) {
            StakeText.say(player, wrangler, StakeText.DRIVE_OFFER, given.head(), next.get().name(), StakeText.paces(here, dest), StakeText.direction(here, dest));
        } else {
            StakeText.say(player, wrangler, StakeText.DRIVE_OFFER_TRAIL, given.head(), StakeText.paces(here, dest), StakeText.direction(here, dest));
        }
        if (given != null && !held.equals(here)) {
            StakeText.say(player, wrangler, StakeText.DRIVE_HELD, StakeText.paces(here, held), StakeText.direction(here, held));
        }
        return true;
    }

    // Where the buyer waits when no town is in reach: always the same way out of a given town, the first of eight ways
    // round from its own that doesn't end in a sea or a river (the map is asked what biome is there; nothing is loaded)
    private static BlockPos trailEnd(ServerLevel level, long town) {
        double angle = (Long.hashCode(town * 0x9E3779B97F4A7C15L) & 0xFFFF) / 65536.0 * Math.PI * 2;
        BlockPos middle = Towns.middle(town);
        for (int turn = 0; turn < 8; turn++) {
            double a = angle + turn * Math.PI / 4;
            BlockPos end = middle.offset((int) Math.round(Math.cos(a) * TRAIL_LENGTH), 0, (int) Math.round(Math.sin(a) * TRAIL_LENGTH));
            var biome = level.getUncachedNoiseBiome(net.minecraft.core.QuartPos.fromBlock(end.getX()), net.minecraft.core.QuartPos.fromBlock(64), net.minecraft.core.QuartPos.fromBlock(end.getZ()));
            if (!biome.is(net.minecraft.tags.BiomeTags.IS_OCEAN) && !biome.is(net.minecraft.tags.BiomeTags.IS_DEEP_OCEAN) && !biome.is(net.minecraft.tags.BiomeTags.IS_RIVER)) {
                return end;
            }
        }
        return middle.offset((int) Math.round(Math.cos(angle) * TRAIL_LENGTH), 0, (int) Math.round(Math.sin(angle) * TRAIL_LENGTH));
    }

    /**
     * Where a herd is held for its drover: outside the town on the side it's bound for, on dry ground. Played, a herd
     * turned out round the wrangler in the middle of town spent its first minute getting past the buildings. Falls back
     * to where the wrangler stands if that ground isn't loaded.
     */
    private static BlockPos turnOut(ServerLevel level, BlockPos wrangler, long town, BlockPos dest) {
        Optional<net.minecraft.world.level.levelgen.structure.BoundingBox> bounds = Towns.start(level, town).map(s -> s.getBoundingBox());
        if (bounds.isEmpty()) {
            return wrangler;
        }
        var box = bounds.get();
        double cx = box.getCenter().getX(), cz = box.getCenter().getZ();
        double dx = dest.getX() - cx, dz = dest.getZ() - cz, far = Math.max(1.0, Math.hypot(dx, dz));
        dx /= far;
        dz /= far;
        double out = 0;
        while (out < 400 && cx + dx * out >= box.minX() && cx + dx * out <= box.maxX() && cz + dz * out >= box.minZ() && cz + dz * out <= box.maxZ()) {
            out += 4;
        }
        for (int more = 14; more <= 44; more += 10) {
            BlockPos column = BlockPos.containing(cx + dx * (out + more), 0, cz + dz * (out + more));
            if (!level.isLoaded(column)) {
                break;
            }
            BlockPos ground = level.getHeightmapPos(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, column);
            if (level.getFluidState(ground.below()).isEmpty()) {
                return ground;
            }
        }
        return wrangler;
    }

    /** Turns a herd out round {@code at} and gives the player the drive. Null if no cow could be put down. */
    public static @Nullable Drive start(ServerLevel level, ServerPlayer player, BlockPos at, long from, Optional<Long> to, BlockPos dest, int head) {
        UUID id = UUID.randomUUID();
        int out = 0;
        for (int i = 0; i < head * 3 && out < head; i++) {
            float angle = level.getRandom().nextFloat() * Mth.TWO_PI;
            int distance = 3 + level.getRandom().nextInt(6);
            BlockPos column = at.offset(Math.round(Mth.cos(angle) * distance), 0, Math.round(Mth.sin(angle) * distance));
            if (!level.isLoaded(column)) {
                continue;
            }
            BlockPos ground = level.getHeightmapPos(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, column);
            if (Math.abs(ground.getY() - at.getY()) > 4) {
                continue; // a roof, or a hole
            }
            Cow cow = EntityTypes.COW.create(level, EntitySpawnReason.EVENT);
            if (cow == null) {
                break;
            }
            cow.snapTo(ground.getX() + 0.5, ground.getY(), ground.getZ() + 0.5, level.getRandom().nextFloat() * 360.0F, 0.0F);
            cow.finalizeSpawn(level, level.getCurrentDifficultyAt(ground), EntitySpawnReason.EVENT, null);
            cow.setPersistenceRequired();
            cow.setData(ModAttachments.HERD.get(), new Herds.Herd(id, player.getUUID()));
            if (level.addFreshEntity(cow)) {
                out++;
            }
        }
        if (out == 0) {
            return null;
        }
        Drive drive = new Drive(id, from, to, dest, out);
        player.setData(ModAttachments.DRIVE.get(), drive);
        return drive;
    }

    /** The herd cattle of this drive near its drover. */
    public static List<Animal> herd(ServerLevel level, ServerPlayer player, Drive drive) {
        return level.getEntitiesOfClass(Animal.class, new AABB(player.blockPosition()).inflate(ARRIVE_RANGE, 24, ARRIVE_RANGE), animal -> {
            Herds.Herd herd = Herds.herdOf(animal);
            return animal.isAlive() && herd != null && herd.drive().equals(drive.id());
        });
    }

    /** The drive is in: paid for every head that came with the drover, and the herd goes on to the pens. What was paid. */
    public static int arrive(ServerLevel level, ServerPlayer player, Drive drive) {
        List<Animal> herd = herd(level, player, drive);
        int in = herd.size();
        int pay = in * perHead(StakeText.paces(Towns.middle(drive.from()), drive.dest()));
        for (Animal cow : herd) {
            level.sendParticles(ParticleTypes.POOF, cow.getX(), cow.getY(0.5), cow.getZ(), 4, 0.3, 0.3, 0.3, 0.02);
            cow.discard();
        }
        player.removeData(ModAttachments.DRIVE.get());
        if (in == 0) {
            player.sendSystemMessage(Component.translatable(StakeText.DRIVE_LOST).withStyle(ChatFormatting.RED));
            Standing.add(player, drive.from(), -LOST_STANDING);
            return 0;
        }
        Purse.give(player, pay);
        level.playSound(null, player.blockPosition(), ModSounds.COIN_CLINK.get(), SoundSource.PLAYERS, 1.0F, 1.0F);
        player.sendSystemMessage(Component.translatable(StakeText.DRIVE_IN, in, drive.head(), pay).withStyle(ChatFormatting.GOLD));
        int standing = Math.max(1, in / 2);
        Standing.add(player, drive.from(), standing);
        drive.to().ifPresent(town -> Standing.add(player, town, standing));
        ModCriteria.fire(player, ModCriteria.DRIVE_DONE, in);
        return pay;
    }

    // The player's own branded cattle standing by the wrangler: bought, by how far each has come
    private static boolean sell(ServerLevel level, ServerPlayer player, Entity wrangler, Optional<Long> town) {
        List<Animal> stock = level.getEntitiesOfClass(Animal.class, wrangler.getBoundingBox().inflate(SALE_RANGE, 6, SALE_RANGE), animal -> {
            Herds.Brand brand = Herds.brandOf(animal);
            return animal.isAlive() && brand != null && brand.owner().equals(player.getUUID());
        });
        if (stock.isEmpty()) {
            return false;
        }
        int pay = 0, farthest = 0;
        for (Animal cow : stock) {
            Herds.Brand brand = Herds.brandOf(cow);
            int came = StakeText.paces(new BlockPos(brand.x(), 0, brand.z()), cow.blockPosition());
            farthest = Math.max(farthest, came);
            pay += salePrice(came);
            level.sendParticles(ParticleTypes.POOF, cow.getX(), cow.getY(0.5), cow.getZ(), 4, 0.3, 0.3, 0.3, 0.02);
            cow.discard();
        }
        Purse.give(player, pay);
        level.playSound(null, player.blockPosition(), ModSounds.COIN_CLINK.get(), SoundSource.PLAYERS, 1.0F, 1.0F);
        StakeText.say(player, wrangler, StakeText.SOLD, stock.size(), farthest, pay);
        int sold = stock.size();
        town.ifPresent(key -> Standing.add(player, key, Math.max(1, sold / 2)));
        return true;
    }

    // ---------------------------------------------------------------- on the trail

    /** Once a second for a drover: the trail's end reached, a reminder of the heading, and now and then riders on the back trail. */
    public static void tick(ServerLevel level, ServerPlayer player) {
        Drive drive = of(player);
        if (drive == null) {
            return;
        }
        BlockPos here = player.blockPosition();
        boolean atTown = drive.to().isPresent() && Towns.keyAt(level, here).filter(drive.to().get()::equals).isPresent();
        boolean atTrailEnd = drive.to().isEmpty() && StakeText.paces(here, drive.dest()) <= TRAIL_END_RANGE;
        if (atTown || atTrailEnd) {
            arrive(level, player, drive);
            return;
        }
        if (player.tickCount % 200 == 0) {
            player.sendOverlayMessage(Component.translatable(StakeText.DRIVE_HEADING, herd(level, player, drive).size(), StakeText.paces(here, drive.dest()),
                    StakeText.direction(here, drive.dest())));
        }
        if (player.tickCount % AMBUSH_EVERY == 0 && level.getRandom().nextFloat() < AMBUSH_CHANCE && Towns.keyAt(level, here).isEmpty()
                && !herd(level, player, drive).isEmpty() && ambush(level, player) > 0) {
            player.sendSystemMessage(Component.translatable(StakeText.DRIVE_AMBUSH).withStyle(ChatFormatting.DARK_RED));
        }
    }

    /** Two riders come up on the herd from a way off (their first shot does the rest). How many came. */
    public static int ambush(ServerLevel level, ServerPlayer player) {
        int sent = 0;
        float angle = level.getRandom().nextFloat() * Mth.TWO_PI;
        for (int i = 0; i < 2; i++) {
            int distance = 26 + level.getRandom().nextInt(7);
            BlockPos column = player.blockPosition().offset(Math.round(Mth.cos(angle + i * 0.3F) * distance), 0, Math.round(Mth.sin(angle + i * 0.3F) * distance));
            if (!level.isLoaded(column)) {
                continue;
            }
            BlockPos ground = level.getHeightmapPos(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, column);
            var outlaw = ModEntities.OUTLAW.get().create(level, EntitySpawnReason.EVENT);
            if (outlaw == null) {
                continue;
            }
            outlaw.snapTo(ground.getX() + 0.5, ground.getY(), ground.getZ() + 0.5, 0.0F, 0.0F);
            outlaw.finalizeSpawn(level, level.getCurrentDifficultyAt(ground), EntitySpawnReason.EVENT, null);
            outlaw.setTarget(player);
            if (level.addFreshEntity(outlaw)) {
                sent++;
            }
        }
        return sent;
    }
}
