package com.hollowfrontier.content.frontier.duel;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.jspecify.annotations.Nullable;

import com.hollowfrontier.registry.ModAttachments;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import net.minecraft.core.UUIDUtil;
import net.minecraft.world.entity.player.Player;

/**
 * What high noon (51) remembers of a player, kept through death: which bosses have asked them to the street, how often
 * and on what day (twice at most, a day apart), and the duels they've fought, by town and in all. {@code last} is how the
 * latest went (0 none yet, {@link #WON}, {@link #LOST}): the town talks about that one.
 */
public record DuelData(List<Ask> asks, Map<Long, Integer> won, Map<Long, Integer> lost, int wins, int losses, int last) {
    public static final DuelData NONE = new DuelData(List.of(), Map.of(), Map.of(), 0, 0, 0);
    public static final int WON = 1, LOST = 2;
    /** A boss asks this often, then leaves it. */
    public static final int MAX_ASKS = 2;
    private static final int KEPT = 12;

    public record Ask(UUID bounty, int times, long day) {
        static final Codec<Ask> CODEC = RecordCodecBuilder.create(i -> i.group(
                UUIDUtil.CODEC.fieldOf("bounty").forGetter(Ask::bounty),
                Codec.INT.fieldOf("times").forGetter(Ask::times),
                Codec.LONG.fieldOf("day").forGetter(Ask::day)).apply(i, Ask::new));
    }

    private static final Codec<Map<Long, Integer>> BY_TOWN = Codec.unboundedMap(Codec.STRING.xmap(Long::parseLong, String::valueOf), Codec.INT);
    public static final Codec<DuelData> CODEC = RecordCodecBuilder.create(i -> i.group(
            Ask.CODEC.listOf().optionalFieldOf("asks", List.of()).forGetter(DuelData::asks),
            BY_TOWN.optionalFieldOf("won", Map.of()).forGetter(DuelData::won),
            BY_TOWN.optionalFieldOf("lost", Map.of()).forGetter(DuelData::lost),
            Codec.INT.optionalFieldOf("wins", 0).forGetter(DuelData::wins),
            Codec.INT.optionalFieldOf("losses", 0).forGetter(DuelData::losses),
            Codec.INT.optionalFieldOf("last", 0).forGetter(DuelData::last)).apply(i, DuelData::new));

    public static DuelData of(Player player) {
        return player.getData(ModAttachments.DUELS.get());
    }

    public static void set(Player player, DuelData data) {
        player.setData(ModAttachments.DUELS.get(), data);
    }

    public int times(UUID bounty) {
        return asks.stream().filter(a -> a.bounty().equals(bounty)).mapToInt(Ask::times).findFirst().orElse(0);
    }

    public long askedOn(UUID bounty) {
        return asks.stream().filter(a -> a.bounty().equals(bounty)).mapToLong(Ask::day).findFirst().orElse(Long.MIN_VALUE);
    }

    /** Asked once more, on this day. */
    public DuelData asked(UUID bounty, long day) {
        return withAsk(new Ask(bounty, times(bounty) + 1, day));
    }

    /** That boss won't ask again (it was fought, one way or the other). */
    public DuelData closed(UUID bounty) {
        return withAsk(new Ask(bounty, MAX_ASKS, askedOn(bounty)));
    }

    private DuelData withAsk(Ask ask) {
        List<Ask> next = new ArrayList<>(asks);
        next.removeIf(a -> a.bounty().equals(ask.bounty()));
        next.add(ask);
        while (next.size() > KEPT) {
            next.removeFirst();
        }
        return new DuelData(List.copyOf(next), won, lost, wins, losses, last);
    }

    public int won(long town) {
        return won.getOrDefault(town, 0);
    }

    public int lost(long town) {
        return lost.getOrDefault(town, 0);
    }

    /** One more fought and won ({@code town}: where, or null out of any town). */
    public DuelData wonAt(@Nullable Long town) {
        return new DuelData(asks, plus(won, town), lost, wins + 1, losses, WON);
    }

    public DuelData lostAt(@Nullable Long town) {
        return new DuelData(asks, won, plus(lost, town), wins, losses + 1, LOST);
    }

    private static Map<Long, Integer> plus(Map<Long, Integer> map, @Nullable Long town) {
        if (town == null) {
            return map;
        }
        Map<Long, Integer> next = new HashMap<>(map);
        next.merge(town, 1, Integer::sum);
        return Map.copyOf(next);
    }
}
