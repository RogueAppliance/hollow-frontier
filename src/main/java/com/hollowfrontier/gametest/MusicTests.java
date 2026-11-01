package com.hollowfrontier.gametest;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.HashSet;
import java.util.Set;

import com.hollowfrontier.HollowFrontier;
import com.hollowfrontier.content.frontier.music.SaloonPianoBlock;
import com.hollowfrontier.content.frontier.music.SaloonPianoBlockEntity;
import com.hollowfrontier.content.frontier.town.TownPresence;
import com.hollowfrontier.registry.ModAttachments;
import com.hollowfrontier.registry.ModBlocks;
import com.hollowfrontier.registry.ModSounds;

import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerPlayer;

// Music and sound: sound events, the saloon piano, town detection.
final class MusicTests {
    private MusicTests() {}

    static void soundEventsRegistered(GameTestHelper h) {
        for (var sound : ModSounds.SOUNDS.getEntries()) {
            h.assertTrue(BuiltInRegistries.SOUND_EVENT.containsKey(sound.getId()), "sound event missing: " + sound.getId());
        }
        h.succeed();
    }

    /**
     * The piano's repertoire: every tune is a registered event whose sounds.json entry streams an .ogg that's in the jar, and a
     * piano works through all of them in turn.
     */
    static void pianoRepertoireResolves(GameTestHelper h) {
        h.assertTrue(ModSounds.PIANO_REPERTOIRE.size() >= 2, "the piano should have a repertoire");
        String soundsJson;
        try (var in = HollowFrontier.class.getResourceAsStream("/assets/" + HollowFrontier.MODID + "/sounds.json")) {
            h.assertTrue(in != null, "sounds.json missing");
            soundsJson = new String(in.readAllBytes(), StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new IllegalStateException(e);
        }
        for (var tune : ModSounds.PIANO_REPERTOIRE) {
            h.assertTrue(BuiltInRegistries.SOUND_EVENT.containsKey(tune.getId()), "tune not registered: " + tune.getId());
            String name = tune.getId().getPath().substring("block.saloon_piano.".length());
            String file = HollowFrontier.MODID + ":block/saloon_piano/" + name;
            h.assertTrue(soundsJson.contains("\"" + tune.getId().getPath() + "\"") && soundsJson.contains("\"" + file + "\""),
                    "sounds.json doesn't stream " + file + " for " + tune.getId());
            h.assertTrue(HollowFrontier.class.getResource("/assets/" + HollowFrontier.MODID + "/sounds/block/saloon_piano/" + name + ".ogg") != null,
                    "recording missing: " + name + ".ogg");
        }
        h.assertTrue(HollowFrontier.class.getResource("/assets/" + HollowFrontier.MODID + "/sounds/block/saloon_piano/pleasant_moments.ogg") == null,
                "the old piano roll recording is still shipped");
        BlockPos pos = new BlockPos(5, 1, 5);
        h.setBlock(pos, ModBlocks.SALOON_PIANO.get());
        SaloonPianoBlockEntity piano = (SaloonPianoBlockEntity) h.getLevel().getBlockEntity(h.absolutePos(pos));
        Set<Object> heard = new HashSet<>();
        for (int i = 0; i < ModSounds.PIANO_REPERTOIRE.size(); i++) heard.add(piano.nextTune());
        h.assertTrue(heard.size() == ModSounds.PIANO_REPERTOIRE.size(), "a round of the repertoire repeated a tune: " + heard);
        h.succeed();
    }

    /** Right-click toggles the piano; a new piano starts out playing. */
    static void pianoControls(GameTestHelper h) {
        BlockPos pos = new BlockPos(5, 1, 5);
        h.setBlock(pos, ModBlocks.SALOON_PIANO.get());
        h.assertTrue(h.getBlockState(pos).getValue(SaloonPianoBlock.PLAYING), "a new piano should be playing");
        ServerPlayer player = TestPlayers.survival(h, new BlockPos(5, 1, 3));
        h.useBlock(pos, player);
        h.assertFalse(h.getBlockState(pos).getValue(SaloonPianoBlock.PLAYING), "right-click didn't stop it");
        h.useBlock(pos, player);
        h.assertTrue(h.getBlockState(pos).getValue(SaloonPianoBlock.PLAYING), "right-click didn't start it again");

        // The test platform is no town.
        TownPresence.update(player);
        h.assertFalse(player.getData(ModAttachments.IN_TOWN.get()), "test platform counted as a town");
        h.succeed();
    }
}
