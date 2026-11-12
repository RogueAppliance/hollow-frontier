package com.hollowfrontier.content.journal;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;

/**
 * What a player's journal keeps that the world doesn't already know. Kept through death.
 * <ul>
 * <li>48a: the thread pinned to its first page, and whether they've been handed a journal for starting in town. The
 * road's pages aren't here: they're read off the player's advancements each time the book is opened.
 * <li>48b: the creatures met (id to {@link #MET}, {@link #KNOWN}, {@link #TAGGED}), the strange doings they were near
 * (the latest {@link #STRANGE_KEPT}, and how many in all), the people met and what was learned of them that no
 * advancement records, and whether the Gazette's editor has paid for the tally.
 * <li>48c: the towns walked into (town key to the day they first came). The rest of a town's page is read from the
 * town's own records when the book is opened.
 * </ul>
 */
public record JournalData(String pinned, boolean given, Map<String, Integer> creatures, List<Strange> strange, int strangeCount, Set<String> people,
        boolean tallyPaid, Map<Long, Long> towns) {
    /** A creature has a page; it has been tamed, bred or harvested; one the Grays have tagged has been met. */
    public static final int MET = 1, KNOWN = 2, TAGGED = 4;
    public static final int STRANGE_KEPT = 40;

    /** One of the frontier's strange doings as its witness has it: what, the day, where, and the town it was by ("" for none). */
    public record Strange(String kind, long day, int x, int z, String town) {
        public static final Codec<Strange> CODEC = RecordCodecBuilder.create(i -> i.group(
                Codec.STRING.fieldOf("kind").forGetter(Strange::kind),
                Codec.LONG.fieldOf("day").forGetter(Strange::day),
                Codec.INT.fieldOf("x").forGetter(Strange::x),
                Codec.INT.fieldOf("z").forGetter(Strange::z),
                Codec.STRING.optionalFieldOf("town", "").forGetter(Strange::town)).apply(i, Strange::new));
        public static final StreamCodec<ByteBuf, Strange> STREAM_CODEC = StreamCodec.composite(
                ByteBufCodecs.STRING_UTF8, Strange::kind,
                ByteBufCodecs.VAR_LONG, Strange::day,
                ByteBufCodecs.VAR_INT, Strange::x,
                ByteBufCodecs.VAR_INT, Strange::z,
                ByteBufCodecs.STRING_UTF8, Strange::town,
                Strange::new);
    }

    public static final JournalData NONE = new JournalData("", false);

    public static final Codec<JournalData> CODEC = RecordCodecBuilder.create(i -> i.group(
            Codec.STRING.optionalFieldOf("pinned", "").forGetter(JournalData::pinned),
            Codec.BOOL.optionalFieldOf("given", false).forGetter(JournalData::given),
            Codec.unboundedMap(Codec.STRING, Codec.INT).optionalFieldOf("creatures", Map.of()).forGetter(JournalData::creatures),
            Strange.CODEC.listOf().optionalFieldOf("strange", List.of()).forGetter(JournalData::strange),
            Codec.INT.optionalFieldOf("strange_count", 0).forGetter(JournalData::strangeCount),
            Codec.STRING.listOf().xmap(list -> (Set<String>) new HashSet<>(list), set -> set.stream().sorted().toList())
                    .optionalFieldOf("people", Set.of()).forGetter(JournalData::people),
            Codec.BOOL.optionalFieldOf("tally_paid", false).forGetter(JournalData::tallyPaid),
            Codec.unboundedMap(Codec.STRING.xmap(Long::parseLong, String::valueOf), Codec.LONG).optionalFieldOf("towns", Map.of()).forGetter(JournalData::towns))
            .apply(i, JournalData::new));

    public JournalData {
        creatures = Map.copyOf(creatures);
        strange = List.copyOf(strange);
        people = Set.copyOf(people);
        towns = Map.copyOf(towns);
    }

    public JournalData(String pinned, boolean given) {
        this(pinned, given, Map.of(), List.of(), 0, Set.of(), false, Map.of());
    }

    public JournalData pin(String thread) {
        return new JournalData(thread, given, creatures, strange, strangeCount, people, tallyPaid, towns);
    }

    public JournalData handed() {
        return new JournalData(pinned, true, creatures, strange, strangeCount, people, tallyPaid, towns);
    }

    public int creature(String id) {
        return creatures.getOrDefault(id, 0);
    }

    public JournalData creature(String id, int flags) {
        Map<String, Integer> all = new HashMap<>(creatures);
        all.put(id, creature(id) | flags);
        return new JournalData(pinned, given, all, strange, strangeCount, people, tallyPaid, towns);
    }

    public JournalData witnessed(Strange sign) {
        List<Strange> all = new ArrayList<>(strange);
        all.add(sign);
        while (all.size() > STRANGE_KEPT) {
            all.removeFirst();
        }
        return new JournalData(pinned, given, creatures, all, strangeCount + 1, people, tallyPaid, towns);
    }

    public JournalData person(String fact) {
        Set<String> all = new HashSet<>(people);
        all.add(fact);
        return new JournalData(pinned, given, creatures, strange, strangeCount, all, tallyPaid, towns);
    }

    public JournalData paid() {
        return new JournalData(pinned, given, creatures, strange, strangeCount, people, true, towns);
    }

    public JournalData town(long key, long day) {
        Map<Long, Long> all = new HashMap<>(towns);
        all.putIfAbsent(key, day);
        return new JournalData(pinned, given, creatures, strange, strangeCount, people, tallyPaid, all);
    }
}
