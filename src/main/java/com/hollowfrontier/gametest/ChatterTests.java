package com.hollowfrontier.gametest;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import com.hollowfrontier.HollowFrontier;
import com.hollowfrontier.content.frontier.bounty.BountyData;
import com.hollowfrontier.content.frontier.law.Wanted;
import com.hollowfrontier.content.frontier.mystery.Suspicion;
import com.hollowfrontier.content.frontier.outlaw.Outlaw;
import com.hollowfrontier.content.frontier.saloon.Hostess;
import com.hollowfrontier.content.frontier.talk.Act;
import com.hollowfrontier.content.frontier.talk.Chatter;
import com.hollowfrontier.content.frontier.talk.ChatterLines;
import com.hollowfrontier.content.frontier.talk.ChatterLines.Exchange;
import com.hollowfrontier.content.frontier.talk.ChatterLines.Line;
import com.hollowfrontier.content.frontier.talk.Cond;
import com.hollowfrontier.content.frontier.talk.Context;
import com.hollowfrontier.content.frontier.talk.Fact;
import com.hollowfrontier.content.frontier.talk.Moment;
import com.hollowfrontier.content.frontier.townsfolk.Folk;
import com.hollowfrontier.content.frontier.townsfolk.Lawman;
import com.hollowfrontier.content.frontier.townsfolk.Townsfolk;
import com.hollowfrontier.content.ufo.Leash;
import com.hollowfrontier.registry.ModDataComponents;
import com.hollowfrontier.registry.ModEntities;
import com.hollowfrontier.registry.ModItems;

import net.minecraft.advancements.AdvancementHolder;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.ItemStack;

/**
 * Phase 19's talk: the line table is whole, each condition lets its lines in (and keeps them out), picks avoid repeats
 * and lean towards what fits, the world's facts are read right, the cooldowns hold, outlaws and lawmen bark, and two
 * townsfolk gossip where a player can hear.
 */
final class ChatterTests {
    private ChatterTests() {}

    private static final String[] VOICES = {"townsfolk", "shopkeeper", "banker", "blacksmith", "wrangler", "prospector", "engineer",
            "doctor", "teacher", "preacher", "postmaster", "gunsmith", "editor",
            "barmaid", "ruby", "clementine", "josie", "belle", "lupe", "lawman", "sheriff", "deputy", "marshal", "ranger",
            "outlaw", "boss", "jailbird", "freed", "captive", "man_in_black", "disguised_gray", "mr_gray", "kindled"};

    /** Every key is its own, every voice and moment has lines, every fact is asked for, %s only where there's a name. */
    static void tableComplete(GameTestHelper h) {
        Set<String> keys = new HashSet<>();
        for (Line line : ChatterLines.lines()) {
            h.assertTrue(keys.add(line.key()), "two lines share " + line.key());
            h.assertTrue(!line.text().isBlank(), "no text for " + line.key());
            h.assertTrue(!line.text().contains("%s") || line.cond().all().contains(Fact.POSTER), line.key() + " names someone it can't know");
            h.assertTrue(List.of(VOICES).contains(line.voice()), "nobody speaks as " + line.voice());
        }
        for (Exchange exchange : ChatterLines.exchanges()) {
            h.assertTrue(exchange.texts().size() >= 2 && exchange.texts().size() <= 3, exchange.key() + " should be two or three lines");
            for (int i = 0; i < exchange.texts().size(); i++) {
                h.assertTrue(keys.add(exchange.key(i)), "two lines share " + exchange.key(i));
            }
        }
        for (String voice : VOICES) {
            h.assertTrue(ChatterLines.lines().stream().anyMatch(l -> l.voice().equals(voice)), voice + " has nothing to say");
        }
        for (Folk folk : Folk.values()) {
            h.assertTrue(List.of(VOICES).contains(folk.id()), folk + " has no voice");
        }
        for (Hostess hostess : Hostess.values()) {
            h.assertTrue(List.of(VOICES).contains(hostess.id()), hostess + " has no voice");
        }
        for (Lawman.Rank rank : Lawman.Rank.values()) {
            h.assertTrue(List.of(VOICES).contains(rank.id()), rank + " has no voice");
        }
        for (Moment moment : Moment.values()) {
            h.assertTrue(moment == Moment.GOSSIP || ChatterLines.lines().stream().anyMatch(l -> l.moment() == moment), "no lines for " + moment);
        }
        for (Fact fact : Fact.values()) {
            h.assertTrue(ChatterLines.lines().stream().anyMatch(l -> l.cond().all().contains(fact)), "no line asks for " + fact);
        }
        h.assertTrue(ChatterLines.count() >= 150, "only " + ChatterLines.count() + " lines");
        HollowFrontier.LOGGER.info("Chatter: {} lines", ChatterLines.count());
        h.succeed();
    }

    /**
     * Forcing each line's condition lets it in; taking away any fact it asks for, adding any it can't stand, or moving the
     * act outside its range keeps it out. An urgent fact (a raid) crowds out every everyday line.
     */
    static void conditionsLetLinesIn(GameTestHelper h) {
        for (Line line : ChatterLines.lines()) {
            Cond cond = line.cond();
            List<String> voices = List.of(line.voice());
            h.assertTrue(keys(voices, line.moment(), forced(cond.all(), cond.from())).contains(line.key()), line.key() + " never fits its own condition");
            for (Fact fact : cond.all()) {
                Set<Fact> without = EnumSet.copyOf(cond.all());
                without.remove(fact);
                h.assertFalse(keys(voices, line.moment(), forced(without, cond.from())).contains(line.key()), line.key() + " said without " + fact);
            }
            for (Fact fact : cond.none()) {
                Set<Fact> with = cond.all().isEmpty() ? EnumSet.noneOf(Fact.class) : EnumSet.copyOf(cond.all());
                with.add(fact);
                h.assertFalse(keys(voices, line.moment(), forced(with, cond.from())).contains(line.key()), line.key() + " said with " + fact);
            }
            if (cond.from() != Act.FRONTIER) {
                h.assertFalse(keys(voices, line.moment(), forced(cond.all(), Act.FRONTIER)).contains(line.key()), line.key() + " said too early");
            }
            if (cond.to() != Act.AFTER) {
                h.assertFalse(keys(voices, line.moment(), forced(cond.all(), Act.AFTER)).contains(line.key()), line.key() + " said too late");
            }
        }
        for (Exchange exchange : ChatterLines.exchanges()) {
            h.assertTrue(Chatter.exchanges(forced(exchange.cond().all(), exchange.cond().from())).contains(exchange),
                    exchange.key() + " never fits its own condition");
        }
        Context raid = Context.of(Act.FRONTIER, Fact.RAID, Fact.NIGHT);
        for (List<String> voices : List.of(List.of("wrangler", "townsfolk"), List.of("banker", "townsfolk"), List.of("josie", "barmaid"),
                List.of("sheriff", "lawman"))) {
            for (Moment moment : List.of(Moment.GREET, Moment.TALK)) {
                List<Line> lines = Chatter.candidates(voices, moment, raid);
                h.assertTrue(lines.stream().allMatch(l -> l.cond().all().contains(Fact.RAID)), voices + " " + moment + " chatting through a raid: " + lines);
            }
        }
        h.assertTrue(Chatter.exchanges(raid).stream().allMatch(e -> e.cond().all().contains(Fact.RAID)), "gossip through a raid");
        h.assertTrue(!Chatter.candidates(List.of("banker", "townsfolk"), Moment.TALK, Context.of(Act.FRONTIER, Fact.ROBBED)).stream()
                .filter(l -> l.key().equals("message.hollowfrontier.banker.robbed")).toList().isEmpty(), "the banker won't talk of the robbery");
        h.succeed();
    }

    /** Lines a listener heard lately aren't picked again while there are others; the lines that fit best come up most. */
    static void picksAvoidRepeats(GameTestHelper h) {
        RandomSource random = RandomSource.create(19);
        List<String> banker = List.of("banker", "townsfolk");
        ArrayDeque<String> recent = new ArrayDeque<>();
        int fresh = Chatter.candidates(banker, Moment.TALK, Context.QUIET).size();
        for (int i = 0; i < 60; i++) {
            Line line = Chatter.pick(banker, Moment.TALK, Context.QUIET, random, recent);
            h.assertTrue(line != null, "the banker had nothing to say");
            h.assertFalse(recent.contains(line.key()) && recent.size() < fresh, "repeated " + line.key() + " after " + recent);
            recent.addLast(line.key());
            while (recent.size() > Math.min(Chatter.RECENT, fresh - 1)) {
                recent.removeFirst();
            }
        }
        Context robbed = Context.of(Act.FRONTIER, Fact.ROBBED);
        Map<Boolean, Integer> counts = new HashMap<>();
        for (int i = 0; i < 400; i++) {
            Line line = Chatter.pick(banker, Moment.TALK, robbed, random, List.of());
            counts.merge(line.cond().all().contains(Fact.ROBBED), 1, Integer::sum);
        }
        h.assertTrue(counts.getOrDefault(true, 0) > 200, "the robbery should be most of the banker's talk: " + counts);
        h.succeed();
    }

    /** The world's facts: the stars and suspicion on you, the poster in your pack, what you wear, how far the story's gone. */
    static void contextReadsTheWorld(GameTestHelper h) {
        ServerPlayer player = TestPlayers.survival(h, new BlockPos(5, 1, 5));
        BlockPos here = player.blockPosition();
        Context plain = Context.of(h.getLevel(), here, player);
        for (Fact fact : List.of(Fact.WANTED, Fact.OUTLAW, Fact.SUSPECT, Fact.HUNTED, Fact.POSTER, Fact.OUTFIT, Fact.CROWN, Fact.SIGIL, Fact.ROBBED, Fact.RAID)) {
            h.assertFalse(plain.has(fact), "a plain stranger shouldn't be " + fact);
        }
        h.assertValueEqual(plain.act(), Act.FRONTIER, "a new player's act");

        Wanted.addHeat(player, 1);
        h.assertTrue(Context.of(h.getLevel(), here, player).has(Fact.WANTED) && !Context.of(h.getLevel(), here, player).has(Fact.OUTLAW), "one star");
        Wanted.addHeat(player, 5);
        h.assertTrue(Context.of(h.getLevel(), here, player).has(Fact.OUTLAW), "two stars and up");
        Wanted.clear(player);
        Suspicion.add(player, Suspicion.WARN + 1);
        h.assertTrue(Context.of(h.getLevel(), here, player).has(Fact.SUSPECT), "warned off");
        Suspicion.add(player, Suspicion.HOSTILE);
        Context hunted = Context.of(h.getLevel(), here, player);
        h.assertTrue(hunted.has(Fact.HUNTED) && !hunted.has(Fact.SUSPECT), "hunted, not merely warned");

        ItemStack poster = new ItemStack(ModItems.WANTED_POSTER.get());
        poster.set(ModDataComponents.BOUNTY.get(), new BountyData(UUID.randomUUID(), "Dutch Calloway", 0, 0, 3, false));
        player.getInventory().add(poster);
        player.getInventory().add(new ItemStack(ModItems.KINDLED_SIGIL.get()));
        player.setItemSlot(EquipmentSlot.CHEST, new ItemStack(ModItems.DUSTER.get()));
        player.setItemSlot(EquipmentSlot.LEGS, new ItemStack(ModItems.CHAPS.get()));
        player.setItemSlot(EquipmentSlot.FEET, new ItemStack(ModItems.COWBOY_BOOTS.get()));
        player.setItemSlot(EquipmentSlot.HEAD, new ItemStack(ModItems.STARSTEEL_CROWN.get()));
        Context dressed = Context.of(h.getLevel(), here, player);
        h.assertTrue(dressed.has(Fact.POSTER) && dressed.poster().equals("Dutch Calloway"), "the poster and its name: " + dressed.poster());
        h.assertTrue(dressed.has(Fact.SIGIL) && dressed.has(Fact.CROWN) && !dressed.has(Fact.OUTFIT), "crowned, not in the full outfit");
        player.setItemSlot(EquipmentSlot.HEAD, new ItemStack(ModItems.COWBOY_HAT.get()));
        h.assertTrue(Context.of(h.getLevel(), here, player).has(Fact.OUTFIT), "the whole cowboy outfit");

        // A saucer can come before the Hollow Earth: that listener mustn't hear they've been down there
        ServerPlayer watcher = TestPlayers.survival(h, new BlockPos(6, 1, 5));
        award(watcher, "ufo/root");
        Context sawOne = Context.of(h.getLevel(), watcher.blockPosition(), watcher);
        h.assertValueEqual(sawOne.act(), Act.GRAYS, "a saucer seen first");
        h.assertFalse(Cond.act(Act.GATEWAY).test(sawOne), "Hollow Earth lines for someone who hasn't been");
        h.assertTrue(Cond.only(Act.GRAYS).test(sawOne), "saucer lines still reach them");
        TestPlayers.leave(watcher);

        award(player, "hollow/root");
        h.assertValueEqual(Context.of(h.getLevel(), here, player).act(), Act.GATEWAY, "through a gateway");
        h.assertTrue(Cond.act(Act.GATEWAY).test(Context.of(h.getLevel(), here, player)), "Hollow Earth lines for someone who has");
        award(player, "ufo/root");
        h.assertValueEqual(Context.of(h.getLevel(), here, player).act(), Act.GRAYS, "a saucer seen");
        boolean wasBroken = Leash.isBroken(h.getLevel());
        Leash.set(h.getLevel(), true);
        try {
            h.assertValueEqual(Context.of(h.getLevel(), here, player).act(), Act.AFTER, "the leash broken");
        } finally {
            Leash.set(h.getLevel(), wasBroken);
        }
        Suspicion.set(player, Suspicion.NONE, Suspicion.level(player));
        TestPlayers.leave(player);
        h.succeed();
    }

    private static void award(ServerPlayer player, String name) {
        AdvancementHolder holder = player.level().getServer().getAdvancements().get(HollowFrontier.id(name));
        for (String criterion : holder.value().criteria().keySet()) {
            player.getAdvancements().award(holder, criterion);
        }
    }

    /**
     * Using a speaker again within a minute gets nothing; another speaker still talks. One greeting per half minute per
     * listener. A bark of one kind waits for its cooldown, and a listener hears one bark every couple of seconds.
     */
    static void cooldownsHold(GameTestHelper h) {
        ServerPlayer player = TestPlayers.survival(h, new BlockPos(5, 1, 5));
        Townsfolk first = h.spawnWithNoFreeWill(ModEntities.TOWNSFOLK.get(), new BlockPos(5, 1, 7));
        first.setFolk(Folk.SHOPKEEPER);
        Townsfolk second = h.spawnWithNoFreeWill(ModEntities.TOWNSFOLK.get(), new BlockPos(7, 1, 5));
        second.setFolk(Folk.BLACKSMITH);
        Outlaw outlaw = h.spawnWithNoFreeWill(ModEntities.OUTLAW.get(), new BlockPos(9, 1, 9));

        h.assertTrue(Chatter.talk(first, player) != null, "the shopkeeper said nothing");
        h.assertTrue(Chatter.talk(first, player) == null, "the shopkeeper talked twice in a row");
        h.assertTrue(Chatter.talk(second, player) != null, "the blacksmith kept quiet because the shopkeeper spoke");
        h.assertTrue(Chatter.greet(first, player) != null, "no greeting");
        h.assertTrue(Chatter.greet(second, player) == null, "two greetings at once");
        h.assertTrue(Chatter.bark(outlaw, Moment.SPOT) != null, "the outlaw didn't call out");
        h.assertTrue(Chatter.bark(outlaw, Moment.SPOT) == null, "the same bark twice");
        h.assertTrue(Chatter.bark(outlaw, Moment.RELOAD) == null, "two barks heard at once");
        h.runAfterDelay(Chatter.LISTENER_BARK_COOLDOWN + 1, () -> {
            h.assertTrue(Chatter.bark(outlaw, Moment.RELOAD) != null, "the listener's bark cooldown never ran out");
            TestPlayers.leave(player);
            h.succeed();
        });
    }

    /** An outlaw calls out when he sets his sights on a player, and cries out when badly hurt; a lawman says so when he gets one. */
    static void outlawsAndLawmenBark(GameTestHelper h) {
        ServerPlayer player = TestPlayers.survival(h, new BlockPos(5, 1, 5));
        Outlaw outlaw = h.spawnWithNoFreeWill(ModEntities.OUTLAW.get(), new BlockPos(9, 1, 5));
        Lawman lawman = h.spawnWithNoFreeWill(ModEntities.LAWMAN.get(), new BlockPos(5, 1, 9));
        outlaw.setTarget(player);
        h.assertTrue(heard(player, "chatter.hollowfrontier.outlaw.spot."), "no word on spotting the player: " + Chatter.heard(player));
        h.runAfterDelay(Chatter.LISTENER_BARK_COOLDOWN + 1, () -> {
            outlaw.hurtServer(h.getLevel(), h.getLevel().damageSources().playerAttack(player), outlaw.getMaxHealth() * 0.75F);
            h.assertTrue(heard(player, "chatter.hollowfrontier.outlaw.hurt."), "no cry when badly hurt: " + Chatter.heard(player));
        });
        h.runAfterDelay(2 * Chatter.LISTENER_BARK_COOLDOWN + 2, () -> {
            outlaw.hurtServer(h.getLevel(), h.getLevel().damageSources().mobAttack(lawman), 1000.0F);
            h.assertTrue(heard(player, "chatter.hollowfrontier.lawman.arrest."), "the lawman got his man without a word: " + Chatter.heard(player));
            TestPlayers.leave(player);
            h.succeed();
        });
    }

    /** Two townsfolk close together near a player trade the lines of one bit of gossip, in turn. */
    static void townsfolkGossip(GameTestHelper h) {
        ServerPlayer player = TestPlayers.survival(h, new BlockPos(5, 1, 5));
        Townsfolk first = h.spawnWithNoFreeWill(ModEntities.TOWNSFOLK.get(), new BlockPos(8, 1, 5));
        Townsfolk second = h.spawnWithNoFreeWill(ModEntities.TOWNSFOLK.get(), new BlockPos(9, 1, 7));
        Exchange exchange = Chatter.overhear(player);
        h.assertTrue(exchange != null, "nobody gossiped");
        List<String> keys = new ArrayList<>();
        for (int i = 0; i < exchange.texts().size(); i++) {
            keys.add(exchange.key(i));
        }
        h.succeedWhen(() -> {
            h.assertTrue(Chatter.heard(player).containsAll(keys), "overheard " + Chatter.heard(player) + ", expected " + keys);
            h.assertTrue(first.isAlive() && second.isAlive(), "a gossip is gone");
            TestPlayers.leave(player);
        });
    }

    private static boolean heard(ServerPlayer player, String prefix) {
        return Chatter.heard(player).stream().anyMatch(k -> k.startsWith(prefix));
    }

    private static List<String> keys(List<String> voices, Moment moment, Context context) {
        return Chatter.candidates(voices, moment, context).stream().map(Line::key).toList();
    }

    private static Context forced(Set<Fact> facts, Act act) {
        return new Context(facts.isEmpty() ? EnumSet.noneOf(Fact.class) : EnumSet.copyOf(facts), act, "Black Jack Ketchum", act.atLeast(Act.GATEWAY));
    }
}
