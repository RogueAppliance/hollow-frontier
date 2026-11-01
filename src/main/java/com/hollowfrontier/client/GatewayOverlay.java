package com.hollowfrontier.client;

import com.hollowfrontier.HollowFrontier;
import com.hollowfrontier.content.hollow.gateway.PolarPortalBlock;
import com.hollowfrontier.network.GatewayCrossingPayload;
import com.hollowfrontier.registry.ModBlocks;
import com.hollowfrontier.registry.ModSounds;

import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.client.resources.sounds.AbstractTickableSoundInstance;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.resources.Identifier;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.ARGB;
import net.minecraft.util.Mth;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.RegisterGuiLayersEvent;
import net.neoforged.neoforge.client.gui.VanillaGuiLayers;
import net.neoforged.neoforge.client.network.event.RegisterClientPayloadHandlersEvent;

/**
 * What the player sees and hears of a Polar Gateway crossing, in place of the nether portal's swirl:
 * <ul>
 * <li><b>Charge</b>: standing in the well, frost creeps in from the edges of the screen (textures/misc/polar_frost.png),
 *     snow swirls up round them and the ring's hum rises in pitch, filling over the crossing time.</li>
 * <li><b>Crossing</b> (GatewayCrossingPayload from the server): the screen whites out and clears over a second and a
 *     half while the frost melts off more slowly. The title card on a first visit is a vanilla title, drawn over this.</li>
 * </ul>
 */
@EventBusSubscriber(modid = HollowFrontier.MODID, value = Dist.CLIENT)
public final class GatewayOverlay {
    private GatewayOverlay() {}

    static final Identifier FROST = HollowFrontier.id("textures/misc/polar_frost.png");
    static final int FLASH_TICKS = 30;

    private static float charge;
    private static float oCharge;
    private static float flash;
    private static float oFlash;
    private static Hum hum;

    @SubscribeEvent
    static void registerLayers(RegisterGuiLayersEvent event) {
        event.registerBelow(VanillaGuiLayers.TITLE, HollowFrontier.id("polar_gateway"), GatewayOverlay::render);
    }

    @SubscribeEvent
    static void registerPayloads(RegisterClientPayloadHandlersEvent event) {
        event.register(GatewayCrossingPayload.TYPE, (payload, context) -> {
            crossed();
            HollowDescent.start(payload);
        });
    }

    /** Just through: full white, and the frost (full) left to melt. */
    static void crossed() {
        flash = oFlash = 1.0F;
        charge = oCharge = 1.0F;
    }

    /** How charged the crossing is, 0..1 (also the frost's strength). */
    public static float charge() {
        return charge;
    }

    @SubscribeEvent
    static void onClientTick(ClientTickEvent.Post event) {
        Minecraft mc = Minecraft.getInstance();
        LocalPlayer player = mc.player;
        oCharge = charge;
        oFlash = flash;
        if (player == null) {
            charge = flash = 0.0F;
            return;
        }
        if (mc.isPaused()) {
            return;
        }
        boolean inWell = player.level().getBlockState(player.blockPosition()).is(ModBlocks.POLAR_PORTAL.get());
        if (inWell) {
            charge = Math.min(1.0F, charge + 1.0F / PolarPortalBlock.CROSSING_TICKS);
            swirlSnow(player);
            if (hum == null || hum.isStopped()) {
                hum = new Hum(player);
                mc.getSoundManager().play(hum);
            }
        } else {
            charge = Math.max(0.0F, charge - (flash > 0.0F ? 0.015F : 0.08F)); // after a crossing the frost melts off slowly
        }
        flash = Math.max(0.0F, flash - 1.0F / FLASH_TICKS);
    }

    // Snow drawn up round the player in a widening spiral, more of it as the charge builds
    private static void swirlSnow(LocalPlayer player) {
        int count = 1 + (int) (charge * 2);
        for (int i = 0; i < count; i++) {
            double a = player.getRandom().nextDouble() * Math.PI * 2;
            double r = 1.6 + player.getRandom().nextDouble() * 0.8; // well clear of the camera (closer, they blot out the view)
            player.level().addParticle(ParticleTypes.SNOWFLAKE, player.getX() + Math.cos(a) * r, player.getY() + player.getRandom().nextDouble() * 0.5,
                    player.getZ() + Math.sin(a) * r, -Math.sin(a) * 0.12, 0.06 + charge * 0.14, Math.cos(a) * 0.12);
        }
    }

    private static void render(GuiGraphicsExtractor g, DeltaTracker delta) {
        float partial = delta.getGameTimeDeltaPartialTick(false);
        float frost = Mth.lerp(partial, oCharge, charge);
        if (frost > 0.0F) {
            g.blit(RenderPipelines.GUI_TEXTURED, FROST, 0, 0, 0.0F, 0.0F, g.guiWidth(), g.guiHeight(), g.guiWidth(), g.guiHeight(),
                    ARGB.white((float) Math.pow(frost, 0.8)));
        }
        float white = Mth.lerp(partial, oFlash, flash);
        if (white > 0.0F) {
            g.fill(0, 0, g.guiWidth(), g.guiHeight(), ARGB.color((int) (255 * white * white), 0xF4FBFF));
        }
    }

    /** The ring's hum while you stand in the well: louder and higher as the charge builds, gone when you step out or cross. */
    static final class Hum extends AbstractTickableSoundInstance {
        private final LocalPlayer player;

        Hum(LocalPlayer player) {
            super(ModSounds.GATEWAY_CHARGE.get(), SoundSource.BLOCKS, player.getRandom());
            this.player = player;
            this.looping = true;
            this.delay = 0;
            this.volume = 0.2F;
            this.pitch = 0.6F;
            this.x = player.getX();
            this.y = player.getY();
            this.z = player.getZ();
        }

        @Override
        public void tick() {
            if (player.isRemoved() || flash > 0.0F || charge <= 0.0F
                    || !player.level().getBlockState(player.blockPosition()).is(ModBlocks.POLAR_PORTAL.get())) {
                stop();
                return;
            }
            volume = 0.2F + charge * 0.8F;
            pitch = 0.6F + charge * 1.0F;
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
