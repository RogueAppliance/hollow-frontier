package com.hollowfrontier.content.journal;

import java.util.ArrayList;
import java.util.List;

import com.hollowfrontier.HollowFrontier;
import com.hollowfrontier.content.concavia.ConcavianCodexItem;
import com.hollowfrontier.content.frontier.bounty.BoardUse;
import com.hollowfrontier.content.frontier.survey.SurveyLetterItem;
import com.hollowfrontier.content.frontier.town.Towns;
import com.hollowfrontier.content.hollow.gateway.PolarCompassItem;
import com.hollowfrontier.network.JournalNotePayload;
import com.hollowfrontier.network.JournalPagesPayload;
import com.hollowfrontier.registry.ModAttachments;
import com.hollowfrontier.registry.ModDataComponents;
import com.hollowfrontier.registry.ModItems;

import io.netty.buffer.ByteBuf;
import net.minecraft.core.BlockPos;
import net.minecraft.core.GlobalPos;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.AdvancementEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.network.PacketDistributor;

/**
 * The Prospector's Journal (48a): the player's own notebook. The server works out what's in it and the client draws
 * it; nothing is sent while the book is shut.
 *
 * <p><b>The Road</b> is one {@link Thread} for each chapter of the Concavian Codex ({@link ConcavianCodexItem#CHAPTERS},
 * the one list of the road's milestones), and under steam the Polar Survey's own list ({@link SurveyLetterItem#STEPS})
 * for as long as the gateway is shut. A thread is in the book only once its subject has been met ({@link #open}), and it
 * holds what's done and the one next step: what lies past that is never sent.
 */
@EventBusSubscriber(modid = HollowFrontier.MODID)
public final class Journal {
    private Journal() {}

    private static final String GATEWAY_OPEN = "steam/open_sesame";

    /**
     * One thread of the road as its reader has it: the milestones reached (advancement paths, in the road's order), the
     * first not reached ("" when there's none), the same for the Survey's list (step names; empty unless this is the
     * thread the letter belongs to and the letter's business isn't finished), and how far and which way the crater is
     * (0 blocks when the journal doesn't know).
     */
    public record Thread(String name, List<String> done, String next, List<String> surveyDone, String surveyNext, int craterBlocks, String craterWay) {
        public static final StreamCodec<ByteBuf, Thread> STREAM_CODEC = StreamCodec.composite(
                ByteBufCodecs.STRING_UTF8, Thread::name,
                ByteBufCodecs.STRING_UTF8.apply(ByteBufCodecs.list()), Thread::done,
                ByteBufCodecs.STRING_UTF8, Thread::next,
                ByteBufCodecs.STRING_UTF8.apply(ByteBufCodecs.list()), Thread::surveyDone,
                ByteBufCodecs.STRING_UTF8, Thread::surveyNext,
                ByteBufCodecs.VAR_INT, Thread::craterBlocks,
                ByteBufCodecs.STRING_UTF8, Thread::craterWay,
                Thread::new);
    }

    public static boolean carried(Player player) {
        return player.getInventory().contains(stack -> stack.is(ModItems.PROSPECTORS_JOURNAL.get()));
    }

    /** The road as this player's journal has it now: the threads they've met, in the Codex's order. */
    public static List<Thread> threads(ServerPlayer player) {
        List<Thread> threads = new ArrayList<>();
        for (ConcavianCodexItem.Chapter chapter : ConcavianCodexItem.CHAPTERS) {
            List<String> done = new ArrayList<>();
            String next = "";
            for (String step : chapter.steps()) {
                if (ConcavianCodexItem.reached(player, step)) {
                    done.add(step);
                } else if (next.isEmpty()) {
                    next = step;
                }
            }
            if (!open(player, chapter.name(), !done.isEmpty())) {
                continue;
            }
            List<String> surveyDone = new ArrayList<>();
            String surveyNext = "";
            int craterBlocks = 0;
            String craterWay = "";
            if (chapter.name().equals("steam") && !ConcavianCodexItem.reached(player, GATEWAY_OPEN)) {
                if (player.getData(ModAttachments.SURVEY_LETTER.get())) {
                    for (SurveyLetterItem.Step step : SurveyLetterItem.STEPS) {
                        if (step.done().test(player)) {
                            surveyDone.add(step.name());
                        } else if (surveyNext.isEmpty()) {
                            surveyNext = step.name();
                        }
                    }
                }
                BlockPos crater = crater(player);
                if (crater != null) {
                    craterBlocks = Math.max(100, (int) Math.round(Math.hypot(crater.getX() - player.getX(), crater.getZ() - player.getZ()) / 100) * 100);
                    craterWay = PolarCompassItem.direction(player.blockPosition(), crater);
                }
            }
            threads.add(new Thread(chapter.name(), List.copyOf(done), next, List.copyOf(surveyDone), surveyNext, craterBlocks, craterWay));
        }
        return threads;
    }

    /**
     * Whether a thread has a page yet. Any milestone of its own opens it. Otherwise: the frontier is where everybody
     * starts; steam opens with the Survey's letter; the Hollow on arriving; what comes after, once the leash is broken.
     * Concavia, the Grays and the Still Sea wait until the player has met them.
     */
    static boolean open(ServerPlayer player, String thread, boolean any) {
        if (any) {
            return true;
        }
        return switch (thread) {
            case "frontier" -> true;
            case "steam" -> player.getData(ModAttachments.SURVEY_LETTER.get());
            case "hollow" -> ConcavianCodexItem.reached(player, "steam/hollow_earth");
            case "after" -> ConcavianCodexItem.reached(player, "ufo/cut_loose");
            default -> false;
        };
    }

    // Where a letter the player carries says the crater is, if they're in the same world as it
    private static BlockPos crater(ServerPlayer player) {
        Inventory inventory = player.getInventory();
        for (int slot = 0; slot < inventory.getContainerSize(); slot++) {
            GlobalPos gateway = inventory.getItem(slot).get(ModDataComponents.SURVEY_GATEWAY.get());
            if (gateway != null && gateway.dimension() == player.level().dimension()) {
                return gateway.pos();
            }
        }
        return null;
    }

    /** What the screen is sent. */
    public static JournalPagesPayload pages(ServerPlayer player) {
        List<Thread> threads = threads(player);
        String pinned = player.getData(ModAttachments.JOURNAL.get()).pinned();
        JournalData data = player.getData(ModAttachments.JOURNAL.get());
        return new JournalPagesPayload(threads, threads.stream().anyMatch(thread -> thread.name().equals(pinned)) ? pinned : "",
                JournalSeen.creatures(player), data.strange(), data.strangeCount(), data.tallyPaid(), JournalSeen.people(player), JournalTowns.towns(player));
    }

    /** Opens the journal for its reader (or brings an open one up to date). */
    public static void open(ServerPlayer player) {
        PacketDistributor.sendToPlayer(player, pages(player));
    }

    /** Pins a thread the player has to the first page; "" unpins. */
    public static void pin(ServerPlayer player, String thread) {
        if (thread.isEmpty() || threads(player).stream().anyMatch(t -> t.name().equals(thread))) {
            player.setData(ModAttachments.JOURNAL.get(), player.getData(ModAttachments.JOURNAL.get()).pin(thread));
        }
    }

    /** The thread a milestone belongs to, or null if it isn't on the road. */
    public static String threadOf(String step) {
        for (ConcavianCodexItem.Chapter chapter : ConcavianCodexItem.CHAPTERS) {
            if (chapter.steps().contains(step)) {
                return chapter.name();
            }
        }
        return null;
    }

    // A milestone reached with the journal on you is noted there and then (the client decides whether to say so)
    @SubscribeEvent
    static void onAdvancementEarned(AdvancementEvent.AdvancementEarnEvent event) {
        if (event.getEntity() instanceof ServerPlayer player && event.getAdvancement().id().getNamespace().equals(HollowFrontier.MODID)) {
            String step = event.getAdvancement().id().getPath();
            if (threadOf(step) != null && carried(player)) {
                PacketDistributor.sendToPlayer(player, new JournalNotePayload(com.hollowfrontier.datagen.ModAdvancements.key(step) + ".title"));
            }
        }
    }

    // Anyone who starts out in a town (42) has a journal in their pack. Once: a lost one is made again at a bench
    @SubscribeEvent
    static void onLogin(PlayerEvent.PlayerLoggedInEvent event) {
        if (event.getEntity() instanceof ServerPlayer player && player.level() instanceof ServerLevel level) {
            handOut(level, player);
        }
    }

    public static boolean handOut(ServerLevel level, ServerPlayer player) {
        JournalData data = player.getData(ModAttachments.JOURNAL.get());
        if (data.given()) {
            return false;
        }
        player.setData(ModAttachments.JOURNAL.get(), data.handed());
        if (Towns.keyAt(level, player.blockPosition()).isEmpty() || carried(player)) {
            return false;
        }
        BoardUse.give(player, new ItemStack(ModItems.PROSPECTORS_JOURNAL.get()));
        return true;
    }
}
