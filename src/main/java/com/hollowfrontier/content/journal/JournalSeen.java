package com.hollowfrontier.content.journal;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Predicate;

import com.hollowfrontier.HollowFrontier;
import com.hollowfrontier.content.concavia.ConcavianCodexItem;
import com.hollowfrontier.content.concavia.Kindled;
import com.hollowfrontier.content.frontier.bounty.Purse;
import com.hollowfrontier.content.frontier.mystery.ManInBlack;
import com.hollowfrontier.content.frontier.mystery.MysteryLog;
import com.hollowfrontier.content.frontier.outlaw.AbstractOutlaw;
import com.hollowfrontier.content.frontier.town.Towns;
import com.hollowfrontier.content.hollow.creature.GlassbackHare;
import com.hollowfrontier.content.hollow.creature.GrayProbe;
import com.hollowfrontier.content.hollow.gray.GraySentinel;
import com.hollowfrontier.content.hollow.gray.Specimens;
import com.hollowfrontier.content.ufo.FieldNotes;
import com.hollowfrontier.content.ufo.GrayDrone;
import com.hollowfrontier.content.ufo.GraySaucer;
import com.hollowfrontier.content.ufo.GrayWarden;
import com.hollowfrontier.network.JournalNotePayload;
import com.hollowfrontier.registry.ModAttachments;

import io.netty.buffer.ByteBuf;
import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.component.WrittenBookContent;
import net.minecraft.world.level.Level;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.AnimalTameEvent;
import net.neoforged.neoforge.event.entity.living.BabyEntitySpawnEvent;
import net.neoforged.neoforge.event.entity.living.LivingDeathEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;
import net.neoforged.neoforge.network.PacketDistributor;

/**
 * What the journal's owner has seen (48b), written down as it happens whether or not the book is on them (it's what they
 * know; the "noted" line is only said when it's carried).
 * <ul>
 * <li><b>Creatures</b> ({@link #CREATURES}, ours only): a page on first sight within {@link #SIGHT} blocks and in plain
 * view, or on the first kill; more on it once one has been tamed, bred, fed or killed ({@link #learn}); and a line
 * about the Grays' tag once a tagged one has been met.
 * <li><b>Strange doings</b>: each sighting, mutilation, crop circle and landing the {@link MysteryLog} records within
 * {@link #WITNESS} blocks of the player, and each visit from the men in black, with the day and the place. At
 * {@link #TALLY} the Gazette's editor pays for the lot, once ({@link #sellTally}).
 * <li><b>People</b> ({@link #PEOPLE}): a page once one of them has been met, and a paragraph for each thing learned.
 * Most of those are advancements; meeting them and finding the Grays' field notes are kept here.
 * </ul>
 */
@EventBusSubscriber(modid = HollowFrontier.MODID)
public final class JournalSeen {
    private JournalSeen() {}

    public static final double SIGHT = 6.0, WITNESS = 128.0;
    public static final int TALLY = 10, TALLY_PAY = 5 * Purse.GOLD;
    private static final int LOOK_EVERY = 20, STRANGE_EVERY = 100;

    /** The creatures that get a page (entity ids in our namespace), in the order the book lists them. */
    public static final List<String> CREATURES = List.of("rattlesnake", "fern_skitter", "glassback_hare", "ash_salamander", "pale_raven",
            "woolly_mammoth", "terror_bird", "megatherium", "glyptodon", "great_sea_cow", "basilosaurus", "plesiosaur", "lampjelly");
    /** Those of them the Grays tag. */
    public static final List<String> TAGGABLE = List.of("glassback_hare", "woolly_mammoth", "terror_bird", "megatherium", "glyptodon");

    /** A paragraph of a person's page, and when the player has it: by something kept in the journal, or by an advancement. */
    public record Fact(String name, Predicate<ServerPlayer> known) {}

    private static Fact kept(String person, String name) {
        return new Fact(name, player -> player.getData(ModAttachments.JOURNAL.get()).people().contains(person + "." + name));
    }

    private static Fact reached(String name, String advancement) {
        return new Fact(name, player -> ConcavianCodexItem.reached(player, advancement));
    }

    /** Who gets a page, and each one's paragraphs in the order they're written. A page is there once any of them is. */
    public static final Map<String, List<Fact>> PEOPLE = new LinkedHashMap<>();

    static {
        PEOPLE.put("outlaws", List.of(kept("outlaws", "met"), reached("bounty", "frontier/dead_or_alive"), reached("freed", "ufo/second_chance"),
                reached("truth", "concavia/the_truth"), reached("loose", "ufo/cut_loose"),
                // 51: met one in the street at noon, and how it went
                new Fact("duel", player -> com.hollowfrontier.content.frontier.duel.DuelData.of(player).wins() > 0),
                new Fact("shot_down", player -> com.hollowfrontier.content.frontier.duel.DuelData.of(player).losses() > 0)));
        PEOPLE.put("dusters", List.of(kept("dusters", "met"), reached("warned", "frontier/word_to_the_wise"), reached("sold", "frontier/nothing_to_see"),
                reached("marked", "frontier/marked_man"), reached("truth", "concavia/the_truth"), reached("loose", "ufo/cut_loose")));
        PEOPLE.put("kindled", List.of(new Fact("met", player -> player.getData(ModAttachments.JOURNAL.get()).people().contains("kindled.met")
                        || ConcavianCodexItem.reached(player, "concavia/well_met")),
                reached("ally", "concavia/an_ally_below"), reached("truth", "concavia/the_truth"), reached("archon", "concavia/eclipse")));
        PEOPLE.put("grays", List.of(new Fact("met", player -> player.getData(ModAttachments.JOURNAL.get()).people().contains("grays.met")
                        || ConcavianCodexItem.reached(player, "ufo/root")),
                reached("caught", "frontier/caught_in_the_act"), kept("grays", "notes"), reached("pens", "hollow/jailbreak"),
                reached("aboard", "ufo/taken"), reached("down", "ufo/what_goes_up"), // 50
                reached("truth", "concavia/the_truth"), reached("fallen", "ufo/the_mothership_falls"), reached("loose", "ufo/cut_loose")));
    }

    /** A creature's page as sent: its id and what's known ({@link JournalData#MET} and the rest). */
    public record Creature(String id, int flags) {
        public static final StreamCodec<ByteBuf, Creature> STREAM_CODEC = StreamCodec.composite(
                ByteBufCodecs.STRING_UTF8, Creature::id, ByteBufCodecs.VAR_INT, Creature::flags, Creature::new);
    }

    /** A person's page as sent: who, and the paragraphs the player has. */
    public record Person(String id, List<String> facts) {
        public static final StreamCodec<ByteBuf, Person> STREAM_CODEC = StreamCodec.composite(
                ByteBufCodecs.STRING_UTF8, Person::id, ByteBufCodecs.STRING_UTF8.apply(ByteBufCodecs.list()), Person::facts, Person::new);
    }

    public static List<Creature> creatures(ServerPlayer player) {
        JournalData data = player.getData(ModAttachments.JOURNAL.get());
        List<Creature> pages = new ArrayList<>();
        for (String id : CREATURES) {
            if (data.creature(id) != 0) {
                pages.add(new Creature(id, data.creature(id)));
            }
        }
        return pages;
    }

    public static List<Person> people(ServerPlayer player) {
        List<Person> pages = new ArrayList<>();
        PEOPLE.forEach((id, facts) -> {
            List<String> known = facts.stream().filter(fact -> fact.known().test(player)).map(Fact::name).toList();
            if (!known.isEmpty()) {
                pages.add(new Person(id, known));
            }
        });
        return pages;
    }

    // Which creature page an entity belongs on, or null
    private static String creature(Entity entity) {
        Identifier id = BuiltInRegistries.ENTITY_TYPE.getKey(entity.getType());
        return id.getNamespace().equals(HollowFrontier.MODID) && CREATURES.contains(id.getPath()) ? id.getPath() : null;
    }

    // Whose page an entity belongs on, or null. A Gray wearing a man's face isn't known for one
    private static String person(Entity entity) {
        if (entity instanceof AbstractOutlaw) {
            return "outlaws";
        }
        if (entity instanceof ManInBlack) {
            return "dusters";
        }
        if (entity instanceof Kindled) {
            return "kindled";
        }
        if (entity instanceof GrayProbe || entity instanceof GraySentinel || entity instanceof GrayWarden || entity instanceof GrayDrone || entity instanceof GraySaucer) {
            return "grays";
        }
        return null;
    }

    private static boolean tagged(Entity entity) {
        return entity instanceof LivingEntity living && Specimens.isTagged(living) || entity instanceof GlassbackHare hare && hare.hasEarTag();
    }

    private static void note(ServerPlayer player, String key) {
        if (Journal.carried(player)) {
            PacketDistributor.sendToPlayer(player, new JournalNotePayload(key));
        }
    }

    private static void creature(ServerPlayer player, Entity entity, int flags) {
        String id = creature(entity);
        if (id == null) {
            return;
        }
        if (tagged(entity)) {
            flags |= JournalData.TAGGED;
        }
        JournalData data = player.getData(ModAttachments.JOURNAL.get());
        if ((data.creature(id) | flags) != data.creature(id)) {
            player.setData(ModAttachments.JOURNAL.get(), data.creature(id, flags));
            note(player, entity.getType().getDescriptionId());
        }
    }

    private static void person(ServerPlayer player, String fact, String person) {
        JournalData data = player.getData(ModAttachments.JOURNAL.get());
        if (!data.people().contains(fact)) {
            boolean had = people(player).stream().anyMatch(page -> page.id().equals(person) && page.facts().contains(fact.substring(person.length() + 1)));
            player.setData(ModAttachments.JOURNAL.get(), data.person(fact));
            if (!had) {
                note(player, JournalText.PERSON.get(person));
            }
        }
    }

    /** Something was learned of a creature at first hand: it was tamed, bred, fed or killed. */
    public static void learn(ServerPlayer player, Entity entity) {
        creature(player, entity, JournalData.MET | JournalData.KNOWN);
    }

    /** What's in plain view within a few blocks goes in the book, and so does a copy of the Grays' field notes in the pack. */
    public static void look(ServerPlayer player) {
        for (Entity entity : player.level().getEntities(player, player.getBoundingBox().inflate(SIGHT), entity -> entity.isAlive() && !entity.isInvisible())) {
            String person = person(entity);
            if ((person != null || creature(entity) != null) && player.hasLineOfSight(entity)) {
                creature(player, entity, JournalData.MET);
                if (person != null) {
                    person(player, person + ".met", person);
                }
            }
        }
        Inventory inventory = player.getInventory();
        for (int slot = 0; slot < inventory.getContainerSize(); slot++) {
            WrittenBookContent book = inventory.getItem(slot).get(DataComponents.WRITTEN_BOOK_CONTENT);
            if (book != null && book.title().raw().equals(FieldNotes.TITLE) && book.author().equals(FieldNotes.AUTHOR)) {
                person(player, "grays.notes", "grays");
                break;
            }
        }
    }

    /** The log's doings of the last {@code window} ticks that happened within {@link #WITNESS} blocks of the player go in the book. */
    public static void witness(ServerLevel level, ServerPlayer player, long window) {
        if (level.dimension() != Level.OVERWORLD) {
            return;
        }
        MysteryLog log = MysteryLog.get(level);
        long now = level.getGameTime();
        for (MysteryLog.Sign sign : log.signs()) {
            if (now - sign.time() <= window && sign.distanceTo(player.blockPosition()) <= WITNESS) {
                strange(level, player, sign.kind().getSerializedName(), sign.time(), sign.x(), sign.z());
            }
        }
        log.lastVisit().ifPresent(visit -> {
            if (now - visit.time() <= window && Math.sqrt(visit.pos().distSqr(player.blockPosition())) <= WITNESS) {
                strange(level, player, "visit", visit.time(), visit.pos().getX(), visit.pos().getZ());
            }
        });
    }

    /** Off one of the Grays' ships by pod (50): it goes in the book as a strange doing of the player's own, where they came down. */
    public static void takenAndBack(ServerPlayer player, boolean broughtDown) {
        if (player.level() instanceof ServerLevel level) {
            strange(level, player, broughtDown ? "brought_down" : "taken", level.getGameTime(), player.getBlockX(), player.getBlockZ());
        }
    }

    private static void strange(ServerLevel level, ServerPlayer player, String kind, long time, int x, int z) {
        JournalData data = player.getData(ModAttachments.JOURNAL.get());
        long day = time / 24000 + 1;
        if (data.strange().stream().anyMatch(sign -> sign.kind().equals(kind) && sign.x() == x && sign.z() == z && sign.day() == day)) {
            return;
        }
        BlockPos pos = new BlockPos(x, player.getBlockY(), z);
        String town = Towns.near(level, pos, 160).map(start -> Towns.name(level, start.getChunkPos().pack())).orElse("");
        player.setData(ModAttachments.JOURNAL.get(), data.witnessed(new JournalData.Strange(kind, day, x, z, town)));
        note(player, JournalText.STRANGE.getOrDefault(kind, kind));
    }

    /**
     * The Gazette's editor buys the tally: with the journal on the player and {@link #TALLY} strange doings in it, once.
     * True if he paid (and so that was his business with the player this time).
     */
    public static boolean sellTally(ServerPlayer player) {
        JournalData data = player.getData(ModAttachments.JOURNAL.get());
        if (data.tallyPaid() || data.strangeCount() < TALLY || !Journal.carried(player)) {
            return false;
        }
        player.setData(ModAttachments.JOURNAL.get(), data.paid());
        Purse.give(player, TALLY_PAY);
        player.sendSystemMessage(Component.translatable(JournalText.TALLY_SOLD, TALLY_PAY / Purse.GOLD));
        return true;
    }

    @SubscribeEvent
    static void onPlayerTick(PlayerTickEvent.Post event) {
        if (event.getEntity() instanceof ServerPlayer player && player.level() instanceof ServerLevel level && !player.isSpectator()) {
            if ((player.tickCount + player.getId()) % LOOK_EVERY == 0) {
                look(player);
            }
            if ((player.tickCount + player.getId()) % STRANGE_EVERY == 0) {
                witness(level, player, STRANGE_EVERY * 2L);
            }
        }
    }

    @SubscribeEvent
    static void onDeath(LivingDeathEvent event) {
        if (event.getSource().getEntity() instanceof ServerPlayer player) {
            learn(player, event.getEntity());
        }
    }

    @SubscribeEvent
    static void onTame(AnimalTameEvent event) {
        if (event.getTamer() instanceof ServerPlayer player) {
            learn(player, event.getAnimal());
        }
    }

    @SubscribeEvent
    static void onBaby(BabyEntitySpawnEvent event) {
        if (event.getCausedByPlayer() instanceof ServerPlayer player) {
            learn(player, event.getParentA());
        }
    }
}
