package com.hollowfrontier.client;

import com.hollowfrontier.HollowFrontier;
import com.hollowfrontier.content.concavia.SunArchon;
import com.hollowfrontier.content.ufo.Mothership;
import com.hollowfrontier.registry.ModSounds;

import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.sounds.AbstractTickableSoundInstance;
import net.minecraft.client.resources.sounds.SoundInstance;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.SelectMusicEvent;

/**
 * The boss themes (45c): the Sun Archon's and the Mothership's, each a loop that plays while that boss's bar is on
 * screen and fades out over four seconds when it goes (won, lost or walked away from).
 * <p>
 * How the client knows: both bosses' bars carry vanilla's "play boss music" flag, which the game itself only acts on in
 * the End. A bar with the flag up and one of our two bosses in sight is a fight. The theme is a sound of our own on the
 * Music slider, not the music manager's, because the manager can only stop a piece dead; while it plays, the manager is
 * told to play nothing. With {@code hollowScore} off there are no themes.
 */
@EventBusSubscriber(modid = HollowFrontier.MODID, value = Dist.CLIENT)
public final class BossMusic {
    private BossMusic() {}

    private static Theme theme;

    @SubscribeEvent
    static void onClientTick(ClientTickEvent.Post event) {
        Minecraft mc = Minecraft.getInstance();
        if (theme != null && theme.isStopped()) {
            theme = null;
        }
        SoundEvent wanted = mc.player == null || mc.level == null ? null : wanted(mc);
        if (theme != null && theme.event != wanted) {
            theme.wanted = false; // it fades and stops by itself
            if (wanted != null) {
                theme = null;
            }
        }
        if (wanted != null && theme == null) {
            theme = new Theme(wanted);
            mc.getSoundManager().play(theme);
        } else if (wanted != null) {
            theme.wanted = true; // back in range before it had faded out
        }
    }

    /** The theme for the fight the player is in, or null. */
    private static SoundEvent wanted(Minecraft mc) {
        if (!ClientConfig.hollowScore() || !mc.gui.hud.getBossOverlay().shouldPlayMusic()) {
            return null;
        }
        SoundEvent nearest = null;
        double best = Double.MAX_VALUE;
        for (Entity entity : mc.level.entitiesForRendering()) {
            SoundEvent its = entity instanceof SunArchon ? ModSounds.MUSIC_SUN_ARCHON.get()
                    : entity instanceof Mothership ? ModSounds.MUSIC_MOTHERSHIP.get() : null;
            if (its != null && entity.isAlive() && entity.distanceToSqr(mc.player) < best) {
                best = entity.distanceToSqr(mc.player);
                nearest = its;
            }
        }
        return nearest;
    }

    // Nothing else plays as music over a theme, or over its fade
    @SubscribeEvent(priority = EventPriority.LOWEST, receiveCanceled = true)
    static void onSelectMusic(SelectMusicEvent event) {
        if (theme != null && !theme.isStopped()) {
            event.setMusic(null);
        }
    }

    static final class Theme extends AbstractTickableSoundInstance {
        static final float IN = 1.0F / 20.0F;  // up in a second
        static final float OUT = 1.0F / 80.0F; // out over four
        final SoundEvent event;
        boolean wanted = true;

        Theme(SoundEvent event) {
            super(event, SoundSource.MUSIC, SoundInstance.createUnseededRandom());
            this.event = event;
            this.looping = true;
            this.delay = 0;
            this.volume = 0.05F;
            this.relative = true;
        }

        @Override
        public void tick() {
            if (Minecraft.getInstance().player == null) {
                stop();
                return;
            }
            volume = Mth.approach(volume, wanted ? 1.0F : 0.0F, wanted ? IN : OUT);
            if (!wanted && volume <= 0.0F) {
                stop();
            }
        }
    }
}
