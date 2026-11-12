package com.hollowfrontier.client;

import com.hollowfrontier.HollowFrontier;
import com.hollowfrontier.content.WorldFeel;
import com.hollowfrontier.content.ufo.taken.Ship;
import com.hollowfrontier.content.ufo.taken.Taken;
import com.hollowfrontier.registry.ModAttachments;

import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;

/**
 * What a Gray ship going down feels like to someone aboard it (50). The server says only what state the ship is in (the
 * synced {@code ABOARD} attachment); while that's "falling" the deck rolls slowly under the view, more as the time runs
 * out, over a tremor that doesn't stop. Both go through {@link CameraFeel} and its {@code cameraShake} slider. The hull's
 * groans and the pod's landing shake the view by their sounds, as blasts do ({@link WorldFeelClient}).
 */
@EventBusSubscriber(modid = HollowFrontier.MODID, value = Dist.CLIENT)
public final class AboardFeel {
    private AboardFeel() {}

    private static int falling;

    @SubscribeEvent
    static void onClientTick(ClientTickEvent.Post event) {
        Minecraft minecraft = Minecraft.getInstance();
        LocalPlayer player = minecraft.player;
        if (player == null || minecraft.isPaused()) {
            return;
        }
        int state = player.getData(ModAttachments.ABOARD.get()) & ~Taken.VENT_NOTED;
        if (state != Ship.Stage.FALLING.code) {
            falling = 0;
            return;
        }
        falling++;
        CameraFeel.sway(WorldFeel.fallLean(falling, falling / (float) Ship.FALL_TICKS));
        CameraFeel.tremor(WorldFeel.FALL_TREMOR);
    }
}
