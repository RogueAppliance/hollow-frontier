package com.hollowfrontier.client;

import java.util.HashMap;
import java.util.Map;

import com.hollowfrontier.HollowFrontier;
import com.hollowfrontier.content.frontier.music.SaloonPianoBlockEntity;
import com.hollowfrontier.registry.ModAttachments;
import com.hollowfrontier.registry.ModSounds;

import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.client.resources.sounds.SoundInstance;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.SelectMusicEvent;
import net.neoforged.neoforge.client.event.sound.PlaySoundEvent;
import net.neoforged.neoforge.registries.DeferredHolder;

/**
 * What a frontier town sounds like, for a player inside one (the server-synced IN_TOWN flag): now and then a sound from
 * somewhere down the street (a hammer on the anvil, a horse, spurs, a creaking door, footsteps on the boardwalk, the
 * chapel bell, glasses near the saloon, a cat's meow or purr). There's no background music of its own: music comes from saloon pianos (and any
 * jukebox a player loads), so you hear it where it plays. Also gives saloon pianos their sound output, and
 * anywhere in the world keeps vanilla's background music from playing over a record or piano within RECORD_RANGE.
 */
@EventBusSubscriber(modid = HollowFrontier.MODID, value = Dist.CLIENT)
public final class TownSoundscape {
    private TownSoundscape() {}

    // Mean ticks between ambient sounds.
    private static final int AMBIENCE_CHANCE = 120;

    private static final RandomSource RANDOM = RandomSource.create();
    private static final Map<BlockPos, SoundInstance> PIANOS = new HashMap<>();
    // Records and pianos (Records category) we've heard start, with the game time they started.
    private static final Map<SoundInstance, Long> RECORDS = new HashMap<>();
    private static final double RECORD_RANGE = 32.0;
    // A just-started sound may not be active in the engine yet; don't drop it for this many ticks.
    private static final int RECORD_GRACE = 20;

    @SubscribeEvent
    static void onClientSetup(FMLClientSetupEvent event) {
        SaloonPianoBlockEntity.output = new SaloonPianoBlockEntity.Output() {
            @Override
            public void start(BlockPos pos, SoundEvent tune) {
                stop(pos);
                SoundInstance sound = new SimpleSoundInstance(tune, SoundSource.RECORDS, 1.0F, 1.0F, RANDOM,
                        pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5);
                PIANOS.put(pos.immutable(), sound);
                Minecraft mc = Minecraft.getInstance();
                mc.getSoundManager().play(sound);
                // Like a jukebox: name the tune for whoever is standing by the piano.
                if (mc.player != null && mc.player.blockPosition().closerThan(pos, 10)) {
                    String name = BuiltInRegistries.SOUND_EVENT.getKey(tune).getPath().substring("block.saloon_piano.".length());
                    mc.gui.hud.setNowPlaying(Component.translatable("track." + HollowFrontier.MODID + "." + name));
                }
            }

            @Override
            public void stop(BlockPos pos) {
                SoundInstance sound = PIANOS.remove(pos);
                if (sound != null) {
                    Minecraft.getInstance().getSoundManager().stop(sound);
                }
            }

            @Override
            public boolean isPlaying(BlockPos pos) {
                SoundInstance sound = PIANOS.get(pos);
                return sound != null && Minecraft.getInstance().getSoundManager().isActive(sound);
            }

            @Override
            public boolean listenerWithin(BlockPos pos, double range) {
                LocalPlayer player = Minecraft.getInstance().player;
                return player != null && player.blockPosition().closerThan(pos, range);
            }
        };
    }

    @SubscribeEvent
    static void onPlaySound(PlaySoundEvent event) {
        SoundInstance sound = event.getSound();
        Minecraft mc = Minecraft.getInstance();
        if (sound != null && sound.getSource() == SoundSource.RECORDS && mc.level != null) {
            RECORDS.put(sound, mc.level.getGameTime());
        }
    }

    // A record or piano is playing within earshot: hold vanilla's music (it resumes on its usual schedule afterwards).
    @SubscribeEvent
    static void onSelectMusic(SelectMusicEvent event) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.level == null || RECORDS.isEmpty()) {
            return;
        }
        long now = mc.level.getGameTime();
        RECORDS.entrySet().removeIf(e -> now - e.getValue() > RECORD_GRACE && !mc.getSoundManager().isActive(e.getKey()));
        for (SoundInstance record : RECORDS.keySet()) {
            if (mc.player.distanceToSqr(record.getX(), record.getY(), record.getZ()) < RECORD_RANGE * RECORD_RANGE) {
                event.setMusic(null);
                return;
            }
        }
    }

    public static boolean inTown(LocalPlayer player) {
        return player != null && player.getData(ModAttachments.IN_TOWN.get());
    }

    @SubscribeEvent
    static void onClientTick(ClientTickEvent.Post event) {
        Minecraft mc = Minecraft.getInstance();
        // The street goes quiet while a raid is on.
        if (mc.player == null || mc.level == null || mc.isPaused() || !inTown(mc.player) || mc.player.getData(ModAttachments.RAID_ALERT.get()) > 0) {
            return;
        }
        if (RANDOM.nextInt(AMBIENCE_CHANCE) == 0) {
            ambience(mc, mc.player);
        }
    }

    // One sound from somewhere 6-20 blocks off. The bell is rare; glasses only clink near a saloon piano; now and then a cat.
    private static void ambience(Minecraft mc, LocalPlayer player) {
        int roll = RANDOM.nextInt(21);
        DeferredHolder<SoundEvent, SoundEvent> sound;
        if (roll < 5) sound = ModSounds.TOWN_BLACKSMITH;
        else if (roll < 9) sound = ModSounds.TOWN_HORSE;
        else if (roll < 12) sound = ModSounds.TOWN_BOARDWALK;
        else if (roll < 15) sound = ModSounds.TOWN_SPURS;
        else if (roll < 18) sound = ModSounds.TOWN_DOOR;
        else if (roll < 19) sound = ModSounds.TOWN_BELL;
        else if (roll == 20) sound = ModSounds.TOWN_CAT;
        else sound = SaloonPianoBlockEntity.heardRecently(mc.level.getGameTime()) ? ModSounds.SALOON_GLASSES : ModSounds.TOWN_BOARDWALK;
        float angle = RANDOM.nextFloat() * Mth.TWO_PI;
        double distance = 6.0 + RANDOM.nextDouble() * 14.0;
        double x = player.getX() + Mth.cos(angle) * distance;
        double z = player.getZ() + Mth.sin(angle) * distance;
        double y = player.getY() + RANDOM.nextInt(5) - 2;
        mc.getSoundManager().play(new SimpleSoundInstance(sound.get(), SoundSource.AMBIENT, 1.0F, 1.0F, RANDOM, x, y, z));
    }
}
