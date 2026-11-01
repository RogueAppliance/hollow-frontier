package com.hollowfrontier.client;

import com.hollowfrontier.HollowFrontier;
import com.hollowfrontier.network.GatewayCrossingPayload;
import com.hollowfrontier.registry.ModSounds;

import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.resources.sounds.AbstractTickableSoundInstance;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;

/**
 * The fall down Symmes' shaft into the Hollow Earth, steered on the client (the player's own movement is the client's to
 * make, so this is smooth where server pushes would stutter):
 * <ol>
 * <li><b>The shaft</b>: down the dark tube at speed, rings of light rushing past, the wind roaring.</li>
 * <li><b>The reveal</b>: out of the crust's underside into the open, the fall all but stops for a few seconds, the swell
 *     plays and (the first time) the title card comes up, with the land spread out below and the inner sun overhead.</li>
 * <li><b>The drift</b>: down again, faster at first, slowing to a gentle touch on the return gateway's south stone,
 *     drawn a little toward it on the way.</li>
 * </ol>
 * The server keeps the player safe whatever happens here (hidden Slow Falling and no fall damage, GatewayEffects), so a
 * missed packet only means a plainer, slower fall.
 */
@EventBusSubscriber(modid = HollowFrontier.MODID, value = Dist.CLIENT)
public final class HollowDescent {
    private HollowDescent() {}

    static final double SHAFT_SPEED = 1.5;
    static final int HANG_TICKS = 60;
    static final double HANG_SPEED = 0.12;
    static final double DRIFT_SPEED = 0.8;
    static final int MAX_TICKS = 900;

    private static boolean active;
    private static Vec3 target = Vec3.ZERO;
    private static int underside;
    private static int ticks;
    private static int revealedAt = -1;
    private static Wind wind;

    static void start(GatewayCrossingPayload payload) {
        if (!payload.falling()) {
            active = false;
            return;
        }
        active = true;
        target = new Vec3(payload.targetX(), payload.targetY(), payload.targetZ());
        underside = payload.undersideY();
        ticks = 0;
        revealedAt = -1;
    }

    public static boolean active() {
        return active;
    }

    /** Ticks since the player fell out of the crust (-1 while still in the shaft or not falling). */
    public static int sinceReveal() {
        return active && revealedAt >= 0 ? ticks - revealedAt : -1;
    }

    @SubscribeEvent
    static void onClientTick(ClientTickEvent.Pre event) {
        Minecraft mc = Minecraft.getInstance();
        LocalPlayer player = mc.player;
        if (!active || player == null || mc.isPaused()) {
            if (player == null) {
                active = false;
            }
            return;
        }
        ticks++;
        if (player.onGround() && ticks > 5 || player.getY() < target.y - 2 || ticks > MAX_TICKS || player.isInWater()) {
            active = false;
            return;
        }
        boolean inShaft = player.getY() > underside + 1;
        if (!inShaft && revealedAt < 0) {
            revealedAt = ticks;
            mc.getSoundManager().play(SimpleSoundInstance.forUI(ModSounds.HOLLOW_REVEAL.get(), 1.0F, 0.9F));
        }
        double height = player.getY() - target.y;
        double want;
        if (inShaft) {
            want = -SHAFT_SPEED;
        } else if (ticks - revealedAt < HANG_TICKS || ticks - revealedAt < HANG_TICKS * 4 && !landAround(player)) {
            want = -HANG_SPEED; // the breath: hanging in the air over it all (longer if the land below is still arriving)
        } else {
            want = -Mth.clamp(height / 45.0, 0.12, DRIFT_SPEED);
        }
        Vec3 motion = player.getDeltaMovement();
        double vy = Mth.lerp(inShaft ? 0.35 : 0.08, motion.y, want);
        // A gentle pull toward the stone, stronger as the ground comes up
        double pull = inShaft ? 0.0 : Mth.clamp(1.0 - height / 120.0, 0.15, 1.0) * 0.04;
        double vx = Mth.clamp((target.x - player.getX()) * pull, -0.25, 0.25);
        double vz = Mth.clamp((target.z - player.getZ()) * pull, -0.25, 0.25);
        player.setDeltaMovement(vx, vy, vz);
        player.resetFallDistance();
        if (wind == null || wind.isStopped()) {
            wind = new Wind(player);
            mc.getSoundManager().play(wind);
        }
    }

    /** Whether the land is in for some way round (eight chunks out each way), so the hang shows it rather than the void. */
    static boolean landAround(LocalPlayer player) {
        int cx = player.getBlockX() >> 4;
        int cz = player.getBlockZ() >> 4;
        var chunks = player.level().getChunkSource();
        return chunks.hasChunk(cx + 8, cz) && chunks.hasChunk(cx - 8, cz) && chunks.hasChunk(cx, cz + 8) && chunks.hasChunk(cx, cz - 8);
    }

    /** The rush of air on the way down: loud in the shaft, a whisper in the hang, louder as the drift picks up. */
    static final class Wind extends AbstractTickableSoundInstance {
        private final LocalPlayer player;

        Wind(LocalPlayer player) {
            super(SoundEvents.ELYTRA_FLYING, SoundSource.PLAYERS, player.getRandom());
            this.player = player;
            this.looping = true;
            this.delay = 0;
            this.volume = 0.0F;
            this.x = player.getX();
            this.y = player.getY();
            this.z = player.getZ();
        }

        @Override
        public void tick() {
            if (!active || player.isRemoved()) {
                volume = Math.max(0.0F, volume - 0.08F);
                if (volume <= 0.0F) {
                    stop();
                }
                return;
            }
            float speed = (float) Math.abs(player.getDeltaMovement().y);
            volume = Mth.lerp(0.2F, volume, Mth.clamp(speed * 0.7F, 0.04F, 1.0F));
            pitch = 0.8F + speed * 0.25F;
            x = player.getX();
            y = player.getY();
            z = player.getZ();
        }

        @Override
        public boolean canStartSilent() {
            return true;
        }
    }
}
