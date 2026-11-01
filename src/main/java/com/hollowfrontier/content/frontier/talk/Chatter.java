package com.hollowfrontier.content.frontier.talk;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Deque;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.jspecify.annotations.Nullable;

import com.hollowfrontier.HollowFrontier;
import com.hollowfrontier.content.frontier.law.Wanted;
import com.hollowfrontier.content.frontier.mystery.ManInBlack;
import com.hollowfrontier.content.frontier.outlaw.AbstractOutlaw;
import com.hollowfrontier.content.frontier.outlaw.OutlawBoss;
import com.hollowfrontier.content.frontier.saloon.Barmaid;
import com.hollowfrontier.content.frontier.talk.ChatterLines.Exchange;
import com.hollowfrontier.content.frontier.talk.ChatterLines.Line;
import com.hollowfrontier.content.frontier.townsfolk.Lawman;
import com.hollowfrontier.content.frontier.townsfolk.Townsfolk;
import com.hollowfrontier.content.hollow.gray.Captives;
import com.hollowfrontier.content.concavia.Kindled;
import com.hollowfrontier.content.ufo.DisguisedGray;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.npc.villager.AbstractVillager;
import net.minecraft.world.level.Level;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.LivingDeathEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.server.ServerStoppedEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;

/**
 * How the frontier talks (phase 19). Every speaking NPC goes through here: a line is picked from the speaker's voices
 * ({@link #voices}) for the moment, among the lines whose {@link Cond} fits the {@link Context}, weighted towards the
 * ones that ask for the most, and away from what the listener heard lately. An urgent fact (a raid, a hunt) crowds out
 * the rest. Lines arrive as {@code <Name> line} in chat.
 * <ul>
 * <li><b>Talk</b> ({@link #talk}): using a speaker (a trade screen, or a word). Once a minute per speaker and listener.</li>
 * <li><b>Greetings</b> ({@link #lookAround}): coming near a speaker you haven't met today. A listener hears at most one
 *     greeting or bit of gossip every half minute.</li>
 * <li><b>Overheard</b> ({@link #overhear}): now and then two townsfolk near a player trade two or three lines.</li>
 * <li><b>Barks</b> ({@link #bark}): outlaws and lawmen shouting in a fight, to everyone near. Each speaker barks each kind
 *     of thing every ten seconds at most, and a listener hears one every two, so a gang doesn't flood the chat.</li>
 * <li><b>One-offs</b> ({@link #say}): said once, when something happens (a man in black's warning, a jailbird's jeer).</li>
 * </ul>
 * Cooldowns and what each listener heard live here, not saved: they only need to last a play session.
 */
@EventBusSubscriber(modid = HollowFrontier.MODID)
public final class Chatter {
    private Chatter() {}

    /** Ticks between two lines from one speaker (other than talk, which the player asked for). */
    public static final int SPEAKER_COOLDOWN = 100;
    /** Ticks before the same speaker says something again when the same player uses them. */
    public static final int TALK_AGAIN = 1200;
    /** Ticks before a speaker greets the same player again. */
    public static final int GREET_AGAIN = 24000;
    /** Ticks between greetings or gossip one listener hears. */
    public static final int LISTENER_COOLDOWN = 600;
    /** Ticks between barks of one kind from one speaker, and between barks one listener hears. */
    public static final int BARK_COOLDOWN = 200, LISTENER_BARK_COOLDOWN = 40;
    /** Ticks between bits of gossip one listener overhears, how often a listener is checked, and between lines of it. */
    public static final int GOSSIP_INTERVAL = 3600, GOSSIP_CHECK = 200, GOSSIP_GAP = 50;
    public static final double GREET_RANGE = 6.0, BARK_RANGE = 24.0, GOSSIP_RANGE = 10.0, GOSSIP_PAIR = 5.0;
    /** How many of the lines a listener heard last are kept from being picked again. */
    public static final int RECENT = 12;

    private static final Map<UUID, Ear> EARS = new HashMap<>();
    private static final Map<UUID, Long> SPEAKER_NEXT = new HashMap<>();
    private static final Map<String, Long> BARK_NEXT = new HashMap<>();
    private static final List<Pending> PENDING = new ArrayList<>();
    private static int exchanges;

    /** What one listener heard and when: their own cooldowns, and who they talked to. */
    private static final class Ear {
        long nextAmbient = Long.MIN_VALUE, nextBark = Long.MIN_VALUE, nextGossip = Long.MIN_VALUE;
        final Deque<String> recent = new ArrayDeque<>();
        final Map<UUID, Long> talked = new HashMap<>();
        final Map<UUID, Long> greeted = new HashMap<>();

        void heard(String key) {
            recent.remove(key);
            recent.addLast(key);
            while (recent.size() > RECENT) {
                recent.removeFirst();
            }
        }
    }

    /** A line of gossip waiting its turn. */
    private record Pending(int exchange, ResourceKey<Level> dimension, UUID speaker, UUID partner, List<UUID> listeners, String key,
                           Context context, long at) {}

    // ---------------------------------------------------------------- who speaks how

    /** A speaker's voices, most particular first (which lines in {@link ChatterLines} they can say). */
    public static List<String> voices(Entity speaker) {
        return switch (speaker) {
            case LivingEntity held when Captives.isHeld(held) -> List.of("captive");
            case Barmaid barmaid -> List.of(barmaid.getHostess().id(), "barmaid");
            case Townsfolk folk -> List.of(folk.getFolk().id(), "townsfolk");
            case Lawman lawman -> List.of(lawman.getRank().id(), "lawman");
            case AbstractOutlaw outlaw when outlaw.isJailed() -> List.of("jailbird");
            case AbstractOutlaw outlaw when outlaw.isFreed() -> List.of("freed");
            case OutlawBoss boss -> List.of("boss", "outlaw");
            case AbstractOutlaw outlaw -> List.of("outlaw");
            case ManInBlack man -> List.of("man_in_black");
            case DisguisedGray gray when gray.isExposed() -> List.of();
            case DisguisedGray gray when gray.isRegular() -> List.of("mr_gray");
            case DisguisedGray gray -> List.of("disguised_gray");
            case Kindled kindled -> List.of("kindled");
            default -> List.of();
        };
    }

    private static Component name(Entity speaker) {
        ChatFormatting style = switch (speaker) {
            case Barmaid barmaid -> ChatFormatting.GOLD;
            case Lawman lawman -> ChatFormatting.GOLD;
            case AbstractOutlaw outlaw when outlaw.isFreed() -> ChatFormatting.GRAY;
            case AbstractOutlaw outlaw -> ChatFormatting.DARK_RED;
            case ManInBlack man -> ChatFormatting.DARK_GRAY;
            default -> null;
        };
        return style == null ? speaker.getDisplayName() : speaker.getDisplayName().copy().withStyle(style);
    }

    // ---------------------------------------------------------------- picking

    /** The lines a speaker with these voices may say at this moment, in this context (urgent ones only, if any fit). */
    public static List<Line> candidates(List<String> voices, Moment moment, Context context) {
        List<Line> fit = new ArrayList<>();
        for (Line line : ChatterLines.lines()) {
            if (line.moment() == moment && voices.contains(line.voice()) && line.cond().test(context)) {
                fit.add(line);
            }
        }
        if (fit.stream().anyMatch(l -> l.cond().urgent())) {
            fit.removeIf(l -> !l.cond().urgent());
        }
        return fit;
    }

    /** Picks one of the {@link #candidates}, by weight, leaving out the ones in {@code recent} unless nothing else fits. */
    public static @Nullable Line pick(List<String> voices, Moment moment, Context context, RandomSource random, Collection<String> recent) {
        List<Line> fit = candidates(voices, moment, context);
        List<Line> fresh = fit.stream().filter(l -> !recent.contains(l.key())).toList();
        return weighted(fresh.isEmpty() ? fit : fresh, Line::cond, random);
    }

    /** The bits of gossip that fit this context (urgent ones only, if any fit). */
    public static List<Exchange> exchanges(Context context) {
        List<Exchange> fit = new ArrayList<>(ChatterLines.exchanges().stream().filter(e -> e.cond().test(context)).toList());
        if (fit.stream().anyMatch(e -> e.cond().urgent())) {
            fit.removeIf(e -> !e.cond().urgent());
        }
        return fit;
    }

    private static <T> @Nullable T weighted(List<T> options, java.util.function.Function<T, Cond> cond, RandomSource random) {
        int total = options.stream().mapToInt(o -> cond.apply(o).weight()).sum();
        if (total <= 0) {
            return null;
        }
        int roll = random.nextInt(total);
        for (T option : options) {
            roll -= cond.apply(option).weight();
            if (roll < 0) {
                return option;
            }
        }
        return null;
    }

    // ---------------------------------------------------------------- speaking

    /** A speaker used by a player (a trade screen opened, or a word): a line, unless they spoke to them a minute ago. */
    public static @Nullable String talk(LivingEntity speaker, ServerPlayer player) {
        long now = player.level().getGameTime();
        Ear ear = ear(player);
        if (ear.talked.getOrDefault(speaker.getUUID(), Long.MIN_VALUE / 2) + TALK_AGAIN > now) {
            return null;
        }
        String key = speak(speaker, player, Moment.TALK);
        if (key != null) {
            ear.talked.put(speaker.getUUID(), now);
            prune(ear.talked, now - TALK_AGAIN);
        }
        return key;
    }

    /** A speaker greets a player who came near, if neither has spoken lately and they haven't met today. */
    public static @Nullable String greet(LivingEntity speaker, ServerPlayer player) {
        long now = player.level().getGameTime();
        Ear ear = ear(player);
        if (ear.nextAmbient > now || !speakerReady(speaker, now)
                || ear.greeted.getOrDefault(speaker.getUUID(), Long.MIN_VALUE / 2) + GREET_AGAIN > now) {
            return null;
        }
        String key = speak(speaker, player, Moment.GREET);
        if (key != null) {
            ear.greeted.put(speaker.getUUID(), now);
            prune(ear.greeted, now - GREET_AGAIN);
            ear.nextAmbient = now + LISTENER_COOLDOWN;
            SPEAKER_NEXT.put(speaker.getUUID(), now + SPEAKER_COOLDOWN);
        }
        return key;
    }

    /** Something said once, when it happens, with no cooldown: the key said, or null if the speaker has no line for it. */
    public static @Nullable String say(LivingEntity speaker, ServerPlayer player, Moment moment) {
        return speak(speaker, player, moment);
    }

    /**
     * A speaker shouts at this moment (spotting someone, reloading, hurt...) to every player in range who hasn't heard a
     * bark just now. The key shouted, or null (on cooldown, nobody to hear, or no line for it).
     */
    public static @Nullable String bark(LivingEntity speaker, Moment moment) {
        if (!(speaker.level() instanceof ServerLevel level)) {
            return null;
        }
        long now = level.getGameTime();
        String barkId = speaker.getUUID() + "/" + moment.id();
        if (BARK_NEXT.getOrDefault(barkId, Long.MIN_VALUE) > now) {
            return null;
        }
        List<ServerPlayer> listeners = level.getPlayers(p -> !p.isSpectator() && p.distanceToSqr(speaker) <= BARK_RANGE * BARK_RANGE
                && ear(p).nextBark <= now);
        if (listeners.isEmpty()) {
            return null;
        }
        ServerPlayer nearest = listeners.stream().min((a, b) -> Double.compare(a.distanceToSqr(speaker), b.distanceToSqr(speaker))).orElseThrow();
        Context context = Context.of(level, speaker.blockPosition(), nearest);
        Line line = pick(voices(speaker), moment, context, speaker.getRandom(), ear(nearest).recent);
        if (line == null) {
            return null;
        }
        BARK_NEXT.put(barkId, now + BARK_COOLDOWN);
        if (BARK_NEXT.size() > 1024) {
            BARK_NEXT.values().removeIf(t -> t <= now);
        }
        for (ServerPlayer listener : listeners) {
            deliver(speaker, listener, line.key(), context);
            ear(listener).nextBark = now + LISTENER_BARK_COOLDOWN;
        }
        return line.key();
    }

    /** Called every second or so by speakers who greet: the nearest player in sight, if any, may get a hello. */
    public static void lookAround(Mob speaker) {
        if (!(speaker.level() instanceof ServerLevel level) || !speaker.isAlive() || speaker.getTarget() != null
                || speaker instanceof AbstractVillager villager && (villager.isTrading() || villager.isBaby())) {
            return;
        }
        if (!(level.getNearestPlayer(speaker, GREET_RANGE) instanceof ServerPlayer player) || player.isSpectator()
                || !speaker.hasLineOfSight(player) || speaker.getRandom().nextInt(2) != 0) {
            return;
        }
        if (greet(speaker, player) != null) {
            speaker.getLookControl().setLookAt(player);
        }
    }

    private static @Nullable String speak(LivingEntity speaker, ServerPlayer player, Moment moment) {
        if (!(speaker.level() instanceof ServerLevel level)) {
            return null;
        }
        Context context = Context.of(level, speaker.blockPosition(), player);
        Line line = pick(voices(speaker), moment, context, speaker.getRandom(), ear(player).recent);
        if (line == null) {
            return null;
        }
        deliver(speaker, player, line.key(), context);
        return line.key();
    }

    private static void deliver(Entity speaker, ServerPlayer player, String key, Context context) {
        MutableComponent text = Component.translatable(key, context.poster());
        if (speaker instanceof Kindled) {
            text.withStyle(ChatFormatting.AQUA);
        }
        player.sendSystemMessage(Component.translatable("message.hollowfrontier.npc.say", name(speaker), text));
        ear(player).heard(key);
    }

    // ---------------------------------------------------------------- overheard talk

    /**
     * Two townsfolk near this player, close together and not busy, start a bit of gossip that fits (said a line at a time,
     * {@link #GOSSIP_GAP} ticks apart, to everyone near). The gossip started, or null if there's nobody to start it.
     */
    public static @Nullable Exchange overhear(ServerPlayer player) {
        ServerLevel level = player.level();
        long now = level.getGameTime();
        List<Townsfolk> near = level.getEntitiesOfClass(Townsfolk.class, player.getBoundingBox().inflate(GOSSIP_RANGE),
                t -> t.isAlive() && !t.isTrading() && !t.isBaby() && speakerReady(t, now));
        for (Townsfolk first : near) {
            for (Townsfolk second : near) {
                if (second == first || second.distanceToSqr(first) > GOSSIP_PAIR * GOSSIP_PAIR) {
                    continue;
                }
                Context context = Context.of(level, first.blockPosition(), player);
                List<Exchange> fit = exchanges(context);
                Exchange exchange = weighted(fit.stream().filter(e -> !ear(player).recent.contains(e.key(0))).toList(), Exchange::cond, first.getRandom());
                if (exchange == null) {
                    exchange = weighted(fit, Exchange::cond, first.getRandom());
                }
                if (exchange == null) {
                    return null;
                }
                start(level, exchange, first, second, context, now);
                return exchange;
            }
        }
        return null;
    }

    private static void start(ServerLevel level, Exchange exchange, Townsfolk first, Townsfolk second, Context context, long now) {
        int id = ++exchanges;
        List<UUID> listeners = level.getPlayers(p -> !p.isSpectator() && p.distanceToSqr(first) <= (GOSSIP_RANGE + 6) * (GOSSIP_RANGE + 6))
                .stream().map(Entity::getUUID).toList();
        int lines = exchange.texts().size();
        for (int i = 0; i < lines; i++) {
            Townsfolk speaker = i % 2 == 0 ? first : second;
            Townsfolk partner = i % 2 == 0 ? second : first;
            PENDING.add(new Pending(id, level.dimension(), speaker.getUUID(), partner.getUUID(), listeners, exchange.key(i), context,
                    now + (long) i * GOSSIP_GAP));
        }
        long done = now + (long) lines * GOSSIP_GAP;
        SPEAKER_NEXT.put(first.getUUID(), done + SPEAKER_COOLDOWN);
        SPEAKER_NEXT.put(second.getUUID(), done + SPEAKER_COOLDOWN);
        for (UUID listener : listeners) {
            Ear ear = EARS.computeIfAbsent(listener, u -> new Ear());
            ear.nextGossip = now + GOSSIP_INTERVAL;
            ear.nextAmbient = Math.max(ear.nextAmbient, done + LISTENER_COOLDOWN);
        }
    }

    // Says the gossip that's due. A speaker who's gone (or a partner) ends the conversation.
    private static void tickGossip(MinecraftServer server) {
        long now = server.overworld().getGameTime();
        for (Iterator<Pending> it = PENDING.iterator(); it.hasNext();) {
            Pending line = it.next();
            if (line.at() > now) {
                continue;
            }
            it.remove();
            ServerLevel level = server.getLevel(line.dimension());
            Entity speaker = level == null ? null : level.getEntity(line.speaker());
            Entity partner = level == null ? null : level.getEntity(line.partner());
            if (speaker == null || !speaker.isAlive() || partner == null || !partner.isAlive()) {
                PENDING.removeIf(p -> p.exchange() == line.exchange());
                return; // the iterator's spent; the rest wait for the next tick
            }
            if (speaker instanceof Mob mob) {
                mob.getNavigation().stop();
                mob.getLookControl().setLookAt(partner);
            }
            for (UUID id : line.listeners()) {
                if (level.getPlayerByUUID(id) instanceof ServerPlayer player && player.distanceToSqr(speaker) <= 24 * 24) {
                    deliver(speaker, player, line.key(), line.context());
                }
            }
        }
    }

    // ---------------------------------------------------------------- bookkeeping

    /** The keys of the lines this player heard last, oldest first (for tests). */
    public static List<String> heard(ServerPlayer player) {
        return List.copyOf(ear(player).recent);
    }

    /** Whether a bit of gossip is still being said. */
    public static boolean gossiping() {
        return !PENDING.isEmpty();
    }

    private static Ear ear(ServerPlayer player) {
        return EARS.computeIfAbsent(player.getUUID(), u -> new Ear());
    }

    private static boolean speakerReady(Entity speaker, long now) {
        return SPEAKER_NEXT.getOrDefault(speaker.getUUID(), Long.MIN_VALUE) <= now;
    }

    private static void prune(Map<UUID, Long> times, long before) {
        if (times.size() > 64) {
            times.values().removeIf(t -> t < before);
        }
    }

    @SubscribeEvent
    static void onPlayerTick(PlayerTickEvent.Post event) {
        if (!(event.getEntity() instanceof ServerPlayer player) || player.tickCount % GOSSIP_CHECK != 0 || player.isSpectator()) {
            return;
        }
        long now = player.level().getGameTime();
        Ear ear = ear(player);
        if (ear.nextGossip <= now && ear.nextAmbient <= now && player.getRandom().nextInt(3) == 0) {
            overhear(player);
        }
    }

    // A lawman who brings down an outlaw, or a wanted player, says so.
    @SubscribeEvent
    static void onDeath(LivingDeathEvent event) {
        if (event.getSource().getEntity() instanceof Lawman lawman && (event.getEntity() instanceof AbstractOutlaw
                || event.getEntity() instanceof ServerPlayer player && Wanted.stars(player) > 0)) {
            bark(lawman, Moment.ARREST);
        }
    }

    @SubscribeEvent
    static void onServerTick(ServerTickEvent.Post event) {
        if (!PENDING.isEmpty()) {
            tickGossip(event.getServer());
        }
        if (event.getServer().getTickCount() % 6000 == 0) {
            long now = event.getServer().overworld().getGameTime();
            SPEAKER_NEXT.values().removeIf(t -> t <= now);
            BARK_NEXT.values().removeIf(t -> t <= now);
        }
    }

    @SubscribeEvent
    static void onLogout(PlayerEvent.PlayerLoggedOutEvent event) {
        EARS.remove(event.getEntity().getUUID());
    }

    // Game time starts over in another world: none of this carries across.
    @SubscribeEvent
    static void onServerStopped(ServerStoppedEvent event) {
        EARS.clear();
        SPEAKER_NEXT.clear();
        BARK_NEXT.clear();
        PENDING.clear();
    }
}
