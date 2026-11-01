package com.hollowfrontier.content.frontier.mystery;

import com.hollowfrontier.content.ufo.Leash;
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
 * How much the men in black dusters (11b) mind a player: 0 to {@link #MAX}. Poking at the Grays' business raises it
 * (killing their probes, sentinels and specimens, calling a sighting with the pilot's skull, taking bearings on their
 * stations, being seen carrying their things, killing their men); selling them what you've found lowers it, and it fades
 * by a point every {@link #COOL_TICKS}. At {@link #WARN} they warn you off; at {@link #HOSTILE} they shoot on sight and
 * come hunting at night. They don't forget a face: it outlasts death. Creative and spectator players don't interest them.
 */
public record Suspicion(int level, long lastChange, long nextHunt) {
    public static final Suspicion NONE = new Suspicion(0, 0, 0);
    public static final int MAX = 100;
    public static final int WARN = 25;
    public static final int HOSTILE = 60;
    public static final int COOL_TICKS = 1200;
    public static final int HUNT_INTERVAL = 6000;

    // What raises it, and what selling them a find takes off
    public static final int KILL_PROBE = 20;
    public static final int KILL_SENTINEL = 15;
    public static final int KILL_SPECIMEN = 8;
    public static final int KILL_MAN_IN_BLACK = 20;
    public static final int KILL_WARDEN = 15;
    public static final int CALL_SIGHTING = 15;
    public static final int TAKE_BEARING = 5;
    public static final int SEEN_CARRYING = 2;
    public static final int SELL = -10;

    public static final Codec<Suspicion> CODEC = RecordCodecBuilder.create(i -> i.group(
            Codec.INT.fieldOf("level").forGetter(Suspicion::level),
            Codec.LONG.fieldOf("last_change").forGetter(Suspicion::lastChange),
            Codec.LONG.fieldOf("next_hunt").forGetter(Suspicion::nextHunt)).apply(i, Suspicion::new));

    public static Suspicion of(Player player) {
        return player.getData(ModAttachments.SUSPICION.get());
    }

    public static int level(Player player) {
        return of(player).level;
    }

    public static boolean warned(Player player) {
        return level(player) >= WARN;
    }

    public static boolean hunted(Player player) {
        return level(player) >= HOSTILE && !player.isCreative() && !player.isSpectator();
    }

    /** Raises (or with a negative amount, lowers) it, telling the player when they cross a line. */
    public static void add(ServerPlayer player, int amount) {
        if (player.isCreative() || player.isSpectator() || Leash.isBroken(player.level())) { // 13d: no one is watching any more
            return;
        }
        Suspicion old = of(player);
        int level = Mth.clamp(old.level + amount, 0, MAX);
        long now = player.level().getGameTime();
        // The first time they turn hostile, a few minutes before the first hunt
        long nextHunt = level >= HOSTILE && old.level < HOSTILE ? now + 2400 : old.nextHunt;
        set(player, new Suspicion(level, now, nextHunt), old.level);
    }

    public static void set(ServerPlayer player, Suspicion suspicion, int oldLevel) {
        player.setData(ModAttachments.SUSPICION.get(), suspicion);
        int level = suspicion.level();
        if (level >= HOSTILE && oldLevel < HOSTILE) {
            player.sendOverlayMessage(Component.translatable("message.hollowfrontier.suspicion.hostile").withStyle(ChatFormatting.DARK_RED));
            ModCriteria.fire(player, ModCriteria.MARKED);
        } else if (level >= WARN && oldLevel < WARN) {
            player.sendOverlayMessage(Component.translatable("message.hollowfrontier.suspicion.watched").withStyle(ChatFormatting.GRAY));
        } else if (level < WARN && oldLevel >= WARN) {
            player.sendOverlayMessage(Component.translatable("message.hollowfrontier.suspicion.eased").withStyle(ChatFormatting.GRAY));
        }
    }

    /** Once a second per player (MenInBlack): fade a point every {@link #COOL_TICKS} without a new reason. */
    public static void tick(ServerPlayer player) {
        Suspicion suspicion = of(player);
        long now = player.level().getGameTime();
        if (suspicion.level > 0 && now - suspicion.lastChange >= COOL_TICKS) {
            set(player, new Suspicion(suspicion.level - 1, now, suspicion.nextHunt), suspicion.level);
        }
    }
}
