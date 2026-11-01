package com.hollowfrontier.content.frontier.town;

import com.hollowfrontier.HollowFrontier;
import com.hollowfrontier.registry.ModAttachments;
import com.hollowfrontier.worldgen.structure.ModStructures;

import net.minecraft.core.registries.Registries;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;

/**
 * Keeps each player's IN_TOWN flag current: true while they stand inside a frontier town's bounds (checked every two
 * seconds on the server, where the structure data lives). The client reads it for the town soundscape.
 */
@EventBusSubscriber(modid = HollowFrontier.MODID)
public final class TownPresence {
    private TownPresence() {}

    @SubscribeEvent
    static void onPlayerTick(PlayerTickEvent.Post event) {
        if (event.getEntity() instanceof ServerPlayer player && player.tickCount % 40 == 0) {
            update(player);
        }
    }

    /** Public so game tests can drive it (fake players don't tick). */
    public static void update(ServerPlayer player) {
        boolean inTown = isInTown(player.level(), player);
        if (player.getData(ModAttachments.IN_TOWN.get()) != inTown) {
            player.setData(ModAttachments.IN_TOWN.get(), inTown); // only on change: every set is a sync packet
        }
    }

    public static boolean isInTown(ServerLevel level, ServerPlayer player) {
        return com.hollowfrontier.content.frontier.raid.TownSite.townAt(level, player.blockPosition()).isPresent(); // any style of town
    }
}
