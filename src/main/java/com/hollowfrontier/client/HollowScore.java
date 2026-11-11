package com.hollowfrontier.client;

import java.util.Map;

import com.hollowfrontier.HollowFrontier;
import com.hollowfrontier.registry.ModSounds;

import net.minecraft.core.Holder;
import net.minecraft.resources.Identifier;
import net.minecraft.sounds.Music;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.SelectMusicEvent;

/**
 * The Hollow Earth's score (45a). The biomes name our pieces as their background music, so the game plays them on its
 * own schedule. This class is only the way out: with {@code hollowScore} off in the client config, each piece is
 * swapped for the vanilla biome music that biome had before, with the same gaps. It runs ahead of
 * {@link TownSoundscape}, which may then hold the music for a piano or a record in earshot.
 */
@EventBusSubscriber(modid = HollowFrontier.MODID, value = Dist.CLIENT)
public final class HollowScore {
    private HollowScore() {}

    private static Map<Identifier, Holder<SoundEvent>> fallbacks;

    /** What a piece stands in for: the vanilla music its biome played up to 1.3.0. Null for anything that isn't ours. */
    static Holder<SoundEvent> vanillaFor(Identifier piece) {
        if (fallbacks == null) {
            fallbacks = Map.of(
                    ModSounds.MUSIC_JUNGLE.getId(), SoundEvents.MUSIC_BIOME_JUNGLE,
                    ModSounds.MUSIC_STEPPES.getId(), SoundEvents.MUSIC_BIOME_MEADOW,
                    ModSounds.MUSIC_BADLANDS.getId(), SoundEvents.MUSIC_BIOME_BADLANDS,
                    ModSounds.MUSIC_HIGHLANDS.getId(), SoundEvents.MUSIC_BIOME_GROVE,
                    ModSounds.MUSIC_SEA.getId(), SoundEvents.MUSIC_BIOME_LUSH_CAVES);
        }
        return fallbacks.get(piece);
    }

    @SubscribeEvent(priority = EventPriority.HIGH)
    static void onSelectMusic(SelectMusicEvent event) {
        Music music = event.getMusic();
        if (music == null || ClientConfig.hollowScore()) {
            return;
        }
        Holder<SoundEvent> vanilla = vanillaFor(music.sound().value().location());
        if (vanilla != null) {
            event.setMusic(new Music(vanilla, music.minDelay(), music.maxDelay(), music.replaceCurrentMusic()));
        }
    }
}
