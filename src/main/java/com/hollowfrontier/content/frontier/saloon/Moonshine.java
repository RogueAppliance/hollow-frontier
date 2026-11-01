package com.hollowfrontier.content.frontier.saloon;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import com.hollowfrontier.HollowFrontier;
import com.hollowfrontier.network.PhantomSightingPayload;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;
import net.neoforged.neoforge.network.PacketDistributor;

/**
 * What the moonshine shows you (21b). A glass of it that leaves the drinker at Tipsy III or more may ({@link #CHANCE})
 * have them see a sighting: the next time, within {@link #PENDING_TICKS}, that they're under the open night sky in the
 * Overworld and still that drunk, an airship crosses it for them alone ({@link PhantomSightingPayload}). It's never
 * logged and the Gazette never hears of it, but for {@link #TALK_TICKS} after, the barmaids and townsfolk have something
 * to say about it (Chatter's SEEING_THINGS). Not saved: it only needs to last the evening.
 */
@EventBusSubscriber(modid = HollowFrontier.MODID)
public final class Moonshine {
    private Moonshine() {}

    public static final float CHANCE = 0.35F;
    public static final int PENDING_TICKS = 2400, TALK_TICKS = 12000;
    public static final int FROM_LEVEL = 3;

    private static final Map<UUID, Long> PENDING = new HashMap<>();
    private static final Map<UUID, Long> SEEN = new HashMap<>();

    /** A glass of moonshine just went down: maybe something will be up there tonight. Whether it will. */
    public static boolean drunk(ServerPlayer player) {
        if (Drink.tipsyLevel(player) < FROM_LEVEL || player.getRandom().nextFloat() >= CHANCE) {
            return false;
        }
        PENDING.put(player.getUUID(), player.level().getGameTime() + PENDING_TICKS);
        return true;
    }

    @SubscribeEvent
    static void onPlayerTick(PlayerTickEvent.Post event) {
        if (!(event.getEntity() instanceof ServerPlayer player) || player.tickCount % 20 != 0) {
            return;
        }
        Long until = PENDING.get(player.getUUID());
        if (until == null) {
            return;
        }
        long now = player.level().getGameTime();
        if (now > until || Drink.tipsyLevel(player) < FROM_LEVEL) {
            PENDING.remove(player.getUUID());
        } else if (player.level().dimension() == Level.OVERWORLD && player.level().isDarkOutside()
                && player.level().canSeeSky(player.blockPosition().above())) {
            see(player);
        }
    }

    /** The sighting, now, for this player only. */
    public static void see(ServerPlayer player) {
        noteSeen(player);
        PacketDistributor.sendToPlayer(player, new PhantomSightingPayload(player.getRandom().nextFloat() * (float) (Math.PI * 2)));
    }

    /** Marks that this player has seen it (and has no more coming tonight). */
    public static void noteSeen(Player player) {
        PENDING.remove(player.getUUID());
        SEEN.put(player.getUUID(), player.level().getGameTime());
    }

    /** For tests: as if they never saw a thing. */
    public static void forget(Player player) {
        PENDING.remove(player.getUUID());
        SEEN.remove(player.getUUID());
    }

    /** Whether this player saw something lately that nobody else did. */
    public static boolean seeingThings(Player player) {
        Long when = SEEN.get(player.getUUID());
        return when != null && player.level().getGameTime() - when < TALK_TICKS;
    }
}
