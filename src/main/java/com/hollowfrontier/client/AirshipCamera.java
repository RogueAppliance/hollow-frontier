package com.hollowfrontier.client;

import com.hollowfrontier.HollowFrontier;
import com.hollowfrontier.content.steam.airship.Airship;
import com.hollowfrontier.content.ufo.PrototypeSaucer;

import net.minecraft.world.entity.Entity;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.CalculateDetachedCameraDistanceEvent;

/**
 * Third person aboard an airship: pull the camera back far enough to clear the envelope (at the usual 4 blocks it sat
 * inside it; runPhoto). Block collision still pulls it in where there's no room.
 * <p>
 * Aboard a Prototype Saucer (13c), the camera also rides {@link #SAUCER_RISE} above the pilot's line of sight (along the
 * camera's own up, so it holds at any pitch), so the craft sits under the crosshair instead of behind it (playtest). The
 * laser still lands in the sight: the pilot's client sends where the camera's middle points ({@link SaucerAim}). With the
 * tractor beam on it eases further back and higher, to show what's being lifted, and eases in again when it's off.
 * The rise uses {@code Camera.move}, opened by the access transformer; it's applied just before vanilla pulls the camera
 * back, as vanilla nudges a sleeping player's camera.
 */
@EventBusSubscriber(modid = HollowFrontier.MODID, value = Dist.CLIENT)
public final class AirshipCamera {
    private AirshipCamera() {}

    static final float DISTANCE = 12.0F;
    static final float SAUCER_DISTANCE = 7.0F, SAUCER_RISE = 2.5F;
    static final float BEAM_DISTANCE = 13.0F, BEAM_RISE = 4.0F;
    // How far the saucer camera has eased toward its beam framing, 0 to 1
    private static float beamFraming;

    @SubscribeEvent
    static void onCameraDistance(CalculateDetachedCameraDistanceEvent event) {
        Entity vehicle = event.getCamera().entity() != null ? event.getCamera().entity().getVehicle() : null;
        if (vehicle instanceof Airship) {
            event.setDistance(Math.max(event.getDistance(), DISTANCE));
        } else if (vehicle instanceof PrototypeSaucer saucer) {
            beamFraming += ((saucer.isBeaming() ? 1.0F : 0.0F) - beamFraming) * 0.04F;
            float distance = SAUCER_DISTANCE + (BEAM_DISTANCE - SAUCER_DISTANCE) * beamFraming;
            float rise = SAUCER_RISE + (BEAM_RISE - SAUCER_RISE) * beamFraming;
            event.getCamera().move(0.0F, rise, 0.0F);
            event.setDistance(Math.max(event.getDistance(), distance));
        } else {
            beamFraming = 0.0F;
        }
    }
}
