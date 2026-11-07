package com.hollowfrontier.content.frontier.talk;

import org.jspecify.annotations.Nullable;

import com.hollowfrontier.HollowFrontier;
import com.hollowfrontier.content.ufo.Leash;

import net.minecraft.advancements.AdvancementHolder;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;

/**
 * How far the story has got for a listener, so the frontier talks about what they've lived through. Each act is reached
 * by the listener's own advancement, except the last, which is the world's: once the leash is broken it's broken for
 * everyone.
 */
public enum Act {
    /** Guns, outlaws, the odd light in the sky. */
    FRONTIER(null),
    /** Through a polar gateway and down into the Hollow Earth (hollow/root). */
    GATEWAY("hollow/root"),
    /** A Gray saucer seen in the night sky (ufo/root). */
    GRAYS("ufo/root"),
    /** The leash broken (13d, {@link Leash}). */
    AFTER(null);

    private final String advancement;

    Act(String advancement) {
        this.advancement = advancement;
    }

    public boolean atLeast(Act act) {
        return compareTo(act) >= 0;
    }

    /** The furthest act this listener has reached (the world's, if there's no listener). */
    public static Act of(ServerLevel level, @Nullable Player player) {
        if (Leash.isBroken(level)) {
            return AFTER;
        }
        for (int i = AFTER.ordinal() - 1; i > 0; i--) {
            if (player instanceof ServerPlayer serverPlayer && values()[i].reached(level, serverPlayer)) {
                return values()[i];
            }
        }
        return FRONTIER;
    }

    /**
     * Whether this listener has been down to the Hollow Earth. The acts aren't always reached in order: a saucer can turn up
     * first, and that puts a listener past {@link #GATEWAY} without their ever having gone through one.
     */
    public static boolean beenDown(ServerLevel level, @Nullable Player player) {
        return player instanceof ServerPlayer serverPlayer ? GATEWAY.reached(level, serverPlayer) : of(level, null).atLeast(GATEWAY);
    }

    private boolean reached(ServerLevel level, ServerPlayer player) {
        AdvancementHolder holder = level.getServer().getAdvancements().get(HollowFrontier.id(advancement));
        return holder != null && player.getAdvancements().getOrStartProgress(holder).isDone();
    }
}
