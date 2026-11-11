package com.hollowfrontier.content.frontier.stake;

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;

import com.hollowfrontier.content.frontier.town.Towns;
import com.hollowfrontier.registry.ModAttachments;
import com.hollowfrontier.registry.ModCriteria;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Player;

/**
 * What each town thinks of a player (47a): points by town key, read as a {@link Rank}. A town's own business raises it
 * (a bounty brought to its board, a raid on it seen off, one of its people brought home, the sheriff's jobs, a herd
 * delivered) and crimes inside its bounds lower it. Unlike {@link com.hollowfrontier.content.frontier.law.Wanted}, which
 * is one figure everywhere and is wiped by dying, this is local and kept. {@code badge} is the town the player is a deputy
 * of, if any, and {@code suspended} whether its sheriff has taken the star back until their name is clear.
 */
public record Standing(Map<Long, Integer> points, Optional<Long> badge, boolean suspended) {
    public static final Standing NONE = new Standing(Map.of(), Optional.empty(), false);
    public static final int MAX = 100;

    // What each deed is worth.
    public static final int BOUNTY = 6;
    public static final int RAID_DEFENDED = 4;
    public static final int RAID_KILL = 2;
    public static final int CAPTIVE = 5;
    public static final int LETTER = 3;
    public static final int JOB = 5;
    public static final int WARRANT_SERVED = 3;
    /** Lost for each point of heat earned inside the town. */
    public static final int PER_HEAT = 2;

    public enum Rank {
        STRANGER(0), REGULAR(10), NEIGHBOUR(30), PILLAR(70);

        public final int from;

        Rank(int from) {
            this.from = from;
        }

        public String id() {
            return name().toLowerCase(Locale.ROOT);
        }

        public boolean atLeast(Rank other) {
            return ordinal() >= other.ordinal();
        }

        public Component title() {
            return Component.translatable("standing.hollowfrontier." + id());
        }

        public static Rank of(int points) {
            Rank rank = STRANGER;
            for (Rank r : values()) {
                if (points >= r.from) {
                    rank = r;
                }
            }
            return rank;
        }
    }

    private static final Codec<Map<Long, Integer>> POINTS = Codec.unboundedMap(Codec.STRING.xmap(Long::parseLong, String::valueOf), Codec.INT);
    public static final Codec<Standing> CODEC = RecordCodecBuilder.create(i -> i.group(
            POINTS.fieldOf("points").forGetter(Standing::points),
            Codec.LONG.optionalFieldOf("badge").forGetter(Standing::badge),
            Codec.BOOL.optionalFieldOf("suspended", false).forGetter(Standing::suspended)).apply(i, Standing::new));

    public int points(long town) {
        return points.getOrDefault(town, 0);
    }

    public Rank rank(long town) {
        return Rank.of(points(town));
    }

    public boolean deputyOf(long town) {
        return !suspended && badge.isPresent() && badge.get() == town;
    }

    public static Standing of(Player player) {
        return player.getData(ModAttachments.STANDING.get());
    }

    public static Rank rank(Player player, long town) {
        return of(player).rank(town);
    }

    private Standing with(long town, int value) {
        Map<Long, Integer> next = new HashMap<>(points);
        if (value <= 0) {
            next.remove(town);
        } else {
            next.put(town, value);
        }
        return new Standing(Map.copyOf(next), badge, suspended);
    }

    public Standing withBadge(Optional<Long> town, boolean isSuspended) {
        return new Standing(points, town, isSuspended);
    }

    /** Adds to (or takes from) the player's standing in a town, and tells them when their rank there changes. */
    public static void add(ServerPlayer player, long town, int amount) {
        if (amount == 0 || !StakeConfig.standing() || player.isCreative() || player.isSpectator()) {
            return;
        }
        Standing old = of(player);
        int value = Mth.clamp(old.points(town) + amount, 0, MAX);
        set(player, old, old.with(town, value), town);
    }

    /** Killing one of a town's own: half of what the player had there is gone. */
    public static void halve(ServerPlayer player, long town) {
        Standing old = of(player);
        if (old.points(town) > 0) {
            set(player, old, old.with(town, old.points(town) / 2), town);
        }
    }

    private static void set(ServerPlayer player, Standing old, Standing next, long town) {
        player.setData(ModAttachments.STANDING.get(), next);
        Rank before = old.rank(town), after = next.rank(town);
        if (before == after) {
            return;
        }
        String name = Towns.name(player.level(), town);
        if (after.ordinal() > before.ordinal()) {
            player.sendSystemMessage(Component.translatable("message.hollowfrontier.standing.up", name, after.title()).withStyle(ChatFormatting.GOLD));
            ModCriteria.fire(player, ModCriteria.STANDING_RANK, after.ordinal());
        } else {
            player.sendSystemMessage(Component.translatable("message.hollowfrontier.standing.down", name, after.title()).withStyle(ChatFormatting.RED));
        }
    }

    /** Standing for something done at a board or a sheriff whose list is {@code boardKey} ("town:&lt;key&gt;"); nothing for a board out of town. */
    public static void atBoard(ServerPlayer player, String boardKey, int amount) {
        town(boardKey).ifPresent(town -> add(player, town, amount));
    }

    public static Optional<Long> town(String boardKey) {
        if (!boardKey.startsWith("town:")) {
            return Optional.empty();
        }
        try {
            return Optional.of(Long.parseLong(boardKey.substring(5)));
        } catch (NumberFormatException e) {
            return Optional.empty();
        }
    }
}
