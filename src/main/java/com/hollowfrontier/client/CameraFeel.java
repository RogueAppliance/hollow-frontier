package com.hollowfrontier.client;

import com.hollowfrontier.HollowFrontier;
import com.hollowfrontier.content.WorldFeel;
import com.hollowfrontier.content.frontier.gun.GunFeel;

import net.minecraft.client.Minecraft;
import net.minecraft.util.Mth;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.ViewportEvent;

/**
 * Everything of ours that moves the player's view (46), and the only place it's moved:
 * <ul>
 * <li><b>The kick</b> ({@code cameraKick}): a gun throws the view up and a little aside at once, and it eases back in
 *     about a quarter of a second to exactly where it was. The aim itself never moves.</li>
 * <li><b>The shake</b> ({@code cameraShake}): footfalls, blasts and tremors add to one figure from 0 to 1 that fades by
 *     itself, and the view trembles in step with it. (It went by the figure's square at first; measured in the client, a
 *     mammoth six blocks off then moved the view four thousandths of a degree.)</li>
 * <li><b>The sway</b> ({@code cameraShake} too): the lean of an airship's deck in a turn.</li>
 * </ul>
 * Both sliders are in {@link ClientConfig}; at 0 an effect is gone entirely.
 */
@EventBusSubscriber(modid = HollowFrontier.MODID, value = Dist.CLIENT)
public final class CameraFeel {
    private CameraFeel() {}

    // Each is kept as this tick's value and the last's, and drawn between them
    private static float kickUp, kickUpBefore, kickAside, kickAsideBefore;
    private static float shake, shakeBefore;
    private static float sway, swayBefore, swayWanted;
    private static int ticks;

    /** A shot: {@code degrees} up, a share of it to one side. */
    public static void kick(float degrees, boolean toTheRight) {
        kickUp = Math.min(GunFeel.MAX_KICK, kickUp + degrees);
        kickAside = Mth.clamp(kickAside + degrees * GunFeel.KICK_SIDEWAYS * (toTheRight ? 1.0F : -1.0F), -GunFeel.MAX_KICK, GunFeel.MAX_KICK);
    }

    /** Something heavy happened: add this much shake (0 to 1). */
    public static void shake(float amount) {
        shake = WorldFeel.add(shake, amount);
    }

    /** Holds the shake at no less than {@code level} this tick (a tremor that lasts), without piling up. */
    public static void tremor(float level) {
        shake = Math.max(shake, Math.min(level, WorldFeel.MAX_SHAKE));
    }

    /** The lean the deck wants this tick, in degrees; it's eased toward, and goes back to level by itself. */
    public static void sway(float degrees) {
        swayWanted = degrees;
    }

    /** How far up the view is kicked right now, in degrees before the slider (for the dev photo rig, which logs it). */
    public static float kicked() {
        return kickUp;
    }

    /** What the shake stands at (for the dev photo rig, which logs it). */
    public static float shaking() {
        return shake;
    }

    @SubscribeEvent
    static void onClientTick(ClientTickEvent.Post event) {
        if (Minecraft.getInstance().isPaused()) {
            return;
        }
        ticks++;
        kickUpBefore = kickUp;
        kickAsideBefore = kickAside;
        shakeBefore = shake;
        swayBefore = sway;
        kickUp *= 0.55F;
        kickAside *= 0.55F;
        if (kickUp < 0.01F) {
            kickUp = 0.0F;
        }
        if (Math.abs(kickAside) < 0.01F) {
            kickAside = 0.0F;
        }
        shake = Math.max(0.0F, shake - WorldFeel.SHAKE_FADE);
        sway += (swayWanted - sway) * 0.12F;
        swayWanted = 0.0F;
        if (Math.abs(sway) < 0.005F) {
            sway = 0.0F;
        }
    }

    @SubscribeEvent
    static void onCameraAngles(ViewportEvent.ComputeCameraAngles event) {
        float partial = event.getPartialTick();
        float kickScale = (float) ClientConfig.cameraKick();
        float shakeScale = (float) ClientConfig.cameraShake();
        float up = Mth.lerp(partial, kickUpBefore, kickUp) * kickScale;
        float aside = Mth.lerp(partial, kickAsideBefore, kickAside) * kickScale;
        float pitch = -up; // pitch grows downward
        float yaw = aside;
        float roll = Mth.lerp(partial, swayBefore, sway) * shakeScale;
        float trauma = Mth.lerp(partial, shakeBefore, shake);
        if (trauma > 0.0F && shakeScale > 0.0F) {
            // Three unrelated wobbles, so it never reads as a pattern
            float t = ticks + partial;
            float size = trauma * WorldFeel.SHAKE_DEGREES * shakeScale;
            pitch += size * (Mth.sin(t * 2.9F) * 0.6F + Mth.sin(t * 5.3F + 1.3F) * 0.4F);
            yaw += size * (Mth.sin(t * 3.7F + 2.1F) * 0.6F + Mth.sin(t * 6.1F + 0.4F) * 0.4F);
            roll += size * 0.7F * (Mth.sin(t * 3.1F + 4.0F) * 0.6F + Mth.sin(t * 4.7F + 2.6F) * 0.4F);
        }
        if (pitch != 0.0F) {
            event.setPitch(event.getPitch() + pitch);
        }
        if (yaw != 0.0F) {
            event.setYaw(event.getYaw() + yaw);
        }
        if (roll != 0.0F) {
            event.setRoll(event.getRoll() + roll);
        }
    }
}
