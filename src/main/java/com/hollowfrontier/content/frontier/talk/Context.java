package com.hollowfrontier.content.frontier.talk;

import java.util.EnumSet;
import java.util.Set;

import org.jspecify.annotations.Nullable;

import com.hollowfrontier.content.frontier.gear.CowboySetBonus;
import com.hollowfrontier.content.frontier.law.Wanted;
import com.hollowfrontier.content.frontier.mystery.Mysteries;
import com.hollowfrontier.content.frontier.mystery.MysteryLog;
import com.hollowfrontier.content.frontier.mystery.Suspicion;
import com.hollowfrontier.content.frontier.raid.TownRaids;
import com.hollowfrontier.content.frontier.saloon.Moonshine;
import com.hollowfrontier.content.frontier.weather.DustDevils;
import com.hollowfrontier.content.concavia.Artifacts;
import com.hollowfrontier.content.ufo.DisguisedGray;
import com.hollowfrontier.registry.ModAttachments;
import com.hollowfrontier.registry.ModDataComponents;
import com.hollowfrontier.registry.ModItems;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.AABB;

/**
 * The facts of one moment of talk, and the listener's act. {@code poster} is the name on a Wanted Poster the listener
 * carries (empty if none), passed to every line as its first argument so the poster lines can name him. {@code beenDown} is
 * whether the listener has been to the Hollow Earth, which their act alone can't say ({@link Act#beenDown}).
 */
public record Context(Set<Fact> facts, Act act, String poster, boolean beenDown) {
    public static final Context QUIET = new Context(EnumSet.noneOf(Fact.class), Act.FRONTIER, "", false);
    /** How near a Gray in disguise has to be for the townsfolk to remark on it (21c). */
    public static final double STRANGER_RANGE = 24.0;
    /** How long after a dust devil took their hat a bareheaded listener gets remarked on (29). */
    public static final long HAT_WINDOW = 3600L;

    /** A made-up moment, for tests and for picking lines without a world. */
    public static Context of(Act act, Fact... facts) {
        EnumSet<Fact> set = EnumSet.noneOf(Fact.class);
        set.addAll(Set.of(facts));
        return new Context(set, act, set.contains(Fact.POSTER) ? "Black Jack Ketchum" : "", act.atLeast(Act.GATEWAY));
    }

    public boolean has(Fact fact) {
        return facts.contains(fact);
    }

    /** What's true for a speaker standing at {@code where} and talking to {@code player} (or to nobody in particular). */
    public static Context of(ServerLevel level, BlockPos where, @Nullable Player player) {
        EnumSet<Fact> facts = EnumSet.noneOf(Fact.class);
        long now = level.getGameTime();
        if (level.isDarkOutside()) {
            facts.add(Fact.NIGHT);
        }
        if (level.isRaining()) {
            facts.add(Fact.RAIN);
        }
        if (level.isThundering()) {
            facts.add(Fact.STORM);
        }
        if (TownRaids.isRobbed(level, where)) {
            facts.add(Fact.ROBBED);
        }
        if (TownRaids.raidAt(level, where) != null) {
            facts.add(Fact.RAID);
        }
        MysteryLog log = MysteryLog.get(level);
        recent(log, MysteryLog.Kind.MUTILATION, Fact.MUTILATIONS, where, now, facts);
        recent(log, MysteryLog.Kind.CROP_CIRCLE, Fact.CROP_CIRCLES, where, now, facts);
        recent(log, MysteryLog.Kind.SIGHTING, Fact.SIGHTINGS, where, now, facts);
        recent(log, MysteryLog.Kind.LANDING, Fact.LANDINGS, where, now, facts);
        if (!level.getEntitiesOfClass(DisguisedGray.class, new AABB(where).inflate(STRANGER_RANGE), g -> g.isAlive() && !g.isExposed()).isEmpty()) {
            facts.add(Fact.STRANGER);
        }
        if (DustDevils.cameThrough(level, where)) {
            facts.add(Fact.DUST_DEVIL);
        }
        String poster = "";
        if (player != null) {
            int stars = Wanted.stars(player);
            if (stars >= 1) {
                facts.add(Fact.WANTED);
            }
            if (stars >= 2) {
                facts.add(Fact.OUTLAW);
            }
            if (Suspicion.hunted(player)) {
                facts.add(Fact.HUNTED);
            } else if (Suspicion.warned(player)) {
                facts.add(Fact.SUSPECT);
            }
            for (ItemStack stack : player.getInventory()) {
                if (stack.is(ModItems.WANTED_POSTER.get()) && stack.get(ModDataComponents.BOUNTY.get()) != null && poster.isEmpty()) {
                    poster = stack.get(ModDataComponents.BOUNTY.get()).target();
                    facts.add(Fact.POSTER);
                } else if (stack.is(ModItems.KINDLED_SIGIL.get())) {
                    facts.add(Fact.SIGIL);
                }
            }
            if (CowboySetBonus.hasFullSet(player)) {
                facts.add(Fact.OUTFIT);
            }
            if (Artifacts.crowned(player)) {
                facts.add(Fact.CROWN);
            }
            if (Moonshine.seeingThings(player)) {
                facts.add(Fact.SEEING_THINGS);
            }
            if (player instanceof net.minecraft.server.level.ServerPlayer serverPlayer
                    && com.hollowfrontier.content.concavia.ConcavianCodexItem.reached(serverPlayer, "steam/airship")
                    && !com.hollowfrontier.content.concavia.ConcavianCodexItem.reached(serverPlayer, "steam/open_sesame")) {
                facts.add(Fact.AIRSHIP);
            }
            if (player.getData(ModAttachments.SURVEY_LETTER.get())) {
                facts.add(Fact.SURVEY_LETTER);
            }
            long lost = player.getData(ModAttachments.HAT_LOST.get());
            if (lost >= 0 && now - lost <= HAT_WINDOW && !player.getItemBySlot(EquipmentSlot.HEAD).is(ModItems.COWBOY_HAT.get())) {
                facts.add(Fact.LOST_HAT);
            }
        }
        return new Context(facts, Act.of(level, player), poster, Act.beenDown(level, player));
    }

    private static void recent(MysteryLog log, MysteryLog.Kind kind, Fact fact, BlockPos where, long now, Set<Fact> facts) {
        if (log.latest(kind, where, Mysteries.GRUMBLE_RANGE, now, Mysteries.GRUMBLE_WINDOW).isPresent()) {
            facts.add(fact);
        }
    }
}
