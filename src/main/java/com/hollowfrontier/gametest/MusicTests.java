package com.hollowfrontier.gametest;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.HashSet;
import java.util.Set;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.hollowfrontier.HollowFrontier;
import com.hollowfrontier.content.frontier.music.SaloonPianoBlock;
import com.hollowfrontier.content.frontier.music.SaloonPianoBlockEntity;
import com.hollowfrontier.content.frontier.town.TownPresence;
import com.hollowfrontier.content.concavia.SunArchon;
import com.hollowfrontier.content.hollow.HollowAmbience;
import com.hollowfrontier.content.hollow.Moments;
import com.hollowfrontier.content.ufo.Mothership;
import com.hollowfrontier.registry.ModEntities;
import com.hollowfrontier.content.hollow.sea.SeaLife;
import com.hollowfrontier.worldgen.HollowBiomes;
import com.hollowfrontier.registry.ModAttachments;
import com.hollowfrontier.registry.ModBlocks;
import com.hollowfrontier.registry.ModSounds;

import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.Biomes;
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

    /**
     * 45a: every sound event the mod registers has an entry in sounds.json, and every file of ours an entry names is in
     * the jar. A missing entry plays nothing and says nothing, so it would never be noticed by ear.
     */
    static void everySoundEventIsDefined(GameTestHelper h) {
        JsonObject sounds = soundsJson(h);
        for (var event : ModSounds.SOUNDS.getEntries()) {
            String name = event.getId().getPath();
            h.assertTrue(sounds.has(name), "sounds.json has no entry for " + name);
            for (JsonElement sound : sounds.getAsJsonObject(name).getAsJsonArray("sounds")) {
                boolean file = !sound.isJsonObject() || !sound.getAsJsonObject().has("type")
                        || sound.getAsJsonObject().get("type").getAsString().equals("file");
                String path = sound.isJsonObject() ? sound.getAsJsonObject().get("name").getAsString() : sound.getAsString();
                if (file && path.startsWith(HollowFrontier.MODID + ":")) {
                    String ogg = "/assets/" + HollowFrontier.MODID + "/sounds/" + path.substring(HollowFrontier.MODID.length() + 1) + ".ogg";
                    h.assertTrue(HollowFrontier.class.getResource(ogg) != null, name + " names a recording that isn't shipped: " + ogg);
                }
            }
        }
        h.succeed();
    }

    /**
     * 45a: each Hollow biome's background music is its own piece of the score, streamed from a recording in the jar, and
     * none still names vanilla's music.
     */
    static void hollowBiomesPlayTheHollowScore(GameTestHelper h) {
        JsonObject sounds = soundsJson(h);
        h.assertTrue(ModSounds.HOLLOW_SCORE.size() == 5, "the score should have a piece for each of the five biomes");
        for (var piece : ModSounds.HOLLOW_SCORE) {
            String biome = piece.getId().getPath().substring("music.hollow.".length());
            String json;
            try (var in = HollowFrontier.class.getResourceAsStream("/data/" + HollowFrontier.MODID + "/worldgen/biome/" + biome + ".json")) {
                h.assertTrue(in != null, "no biome for the piece " + piece.getId());
                json = new String(in.readAllBytes(), StandardCharsets.UTF_8);
            } catch (IOException e) {
                throw new IllegalStateException(e);
            }
            h.assertTrue(json.contains("\"" + piece.getId() + "\""), biome + " doesn't play " + piece.getId());
            h.assertFalse(json.contains("\"minecraft:music."), biome + " still plays vanilla's music");
            JsonElement sound = sounds.getAsJsonObject(piece.getId().getPath()).getAsJsonArray("sounds").get(0);
            h.assertTrue(sound.isJsonObject() && sound.getAsJsonObject().get("stream").getAsBoolean(), piece.getId() + " should be streamed");
        }
        h.succeed();
    }

    /** 45b: each Hollow biome names its own loop as its ambient bed, and the loop is a streamed recording. */
    static void hollowBiomesHaveBeds(GameTestHelper h) {
        JsonObject sounds = soundsJson(h);
        for (var bed : ModSounds.HOLLOW_BEDS) {
            JsonElement sound = sounds.getAsJsonObject(bed.getId().getPath()).getAsJsonArray("sounds").get(0);
            h.assertTrue(sound.isJsonObject() && sound.getAsJsonObject().get("stream").getAsBoolean(), bed.getId() + " should be streamed");
            if (bed == ModSounds.SEA_DEEP) {
                continue; // played by the client under water, not a biome's bed
            }
            String biome = bed.getId().getPath().substring("ambient.".length()).replace(".loop", "");
            String json;
            try (var in = HollowFrontier.class.getResourceAsStream("/data/" + HollowFrontier.MODID + "/worldgen/biome/" + biome + ".json")) {
                h.assertTrue(in != null, "no biome for the bed " + bed.getId());
                json = new String(in.readAllBytes(), StandardCharsets.UTF_8);
            } catch (IOException e) {
                throw new IllegalStateException(e);
            }
            h.assertTrue(json.replace(" ", "").contains("\"loop\":\"" + bed.getId() + "\""), biome + " doesn't loop " + bed.getId());
        }
        h.succeed();
    }

    /** 45b: a far-off call is only ever a creature whose biome tag (or the sea) puts it there, and nothing calls outside the Hollow. */
    static void farCallsComeFromWhatLivesThere(GameTestHelper h) {
        var biomes = h.getLevel().registryAccess().lookupOrThrow(Registries.BIOME);
        record Lives(net.minecraft.core.Holder<net.minecraft.sounds.SoundEvent> call, net.minecraft.tags.TagKey<Biome> where) {}
        var lives = java.util.List.of(new Lives(ModSounds.MAMMOTH_TRUMPET, HollowBiomes.HAS_MAMMOTHS),
                new Lives(ModSounds.TERROR_BIRD_SCREECH, HollowBiomes.HAS_TERROR_BIRDS),
                new Lives(ModSounds.MEGATHERIUM_AMBIENT, HollowBiomes.HAS_MEGATHERIUM),
                new Lives(ModSounds.BASILOSAURUS_AMBIENT, SeaLife.HAS_SEA_LIFE),
                new Lives(ModSounds.PLESIOSAUR_AMBIENT, SeaLife.HAS_SEA_LIFE),
                new Lives(ModSounds.SEA_COW_AMBIENT, SeaLife.HAS_SEA_LIFE));
        for (var key : java.util.List.of(HollowBiomes.PRIMORDIAL_JUNGLE, HollowBiomes.CRYSTAL_STEPPES, HollowBiomes.EMBER_BADLANDS,
                HollowBiomes.CONCAVIAN_HIGHLANDS, HollowBiomes.STILL_SEA)) {
            var biome = biomes.getOrThrow(key);
            var calls = HollowAmbience.farCalls(biome);
            h.assertFalse(calls.isEmpty(), key.identifier() + " has nothing calling in the distance");
            for (var call : calls) {
                for (Lives l : lives) {
                    h.assertTrue(!l.call().equals(call) || biome.is(l.where()), key.identifier() + " hears a creature that doesn't live there: " + call);
                }
            }
        }
        h.assertTrue(HollowAmbience.farCalls(biomes.getOrThrow(Biomes.PLAINS)).isEmpty(), "something calls in the Overworld");
        h.succeed();
    }

    /** 45b: the deeper, the louder and lower the sea's bed and the more often a hull creaks; awash, a hull never does. */
    static void depthDarkensTheSeaAndWorksTheHull(GameTestHelper h) {
        h.assertTrue(HollowAmbience.creakTicks(0) == Integer.MAX_VALUE, "a hull that's awash shouldn't creak");
        for (int depth = 1; depth < HollowAmbience.FULL_DEPTH; depth++) {
            h.assertTrue(HollowAmbience.deepVolume(depth + 1) > HollowAmbience.deepVolume(depth), "the sea should get louder with depth at " + depth);
            h.assertTrue(HollowAmbience.deepPitch(depth + 1) < HollowAmbience.deepPitch(depth), "the sea should get lower with depth at " + depth);
            h.assertTrue(HollowAmbience.creakTicks(depth + 1) <= HollowAmbience.creakTicks(depth), "a hull should creak more with depth at " + depth);
        }
        h.assertTrue(HollowAmbience.creakTicks(1) > HollowAmbience.creakTicks(HollowAmbience.FULL_DEPTH) * 4, "depth should make a real difference to the hull");
        h.assertTrue(HollowAmbience.deepVolume(200) == HollowAmbience.deepVolume(HollowAmbience.FULL_DEPTH) && HollowAmbience.deepVolume(200) <= 1.0F,
                "past full depth nothing changes");
        h.assertTrue(HollowAmbience.deepPitch(200) >= 0.5F, "the pitch can't go under what the sound engine allows");
        h.succeed();
    }

    /**
     * 45c: each of the five stings is tied to an advancement the mod really has (a renamed one would silence its sting
     * without a word), no two share one, and each is a streamed recording.
     */
    static void stingsRideOnRealAdvancements(GameTestHelper h) {
        JsonObject sounds = soundsJson(h);
        var stings = Moments.stings();
        h.assertTrue(stings.size() == ModSounds.STINGS.size() && new HashSet<>(stings.values()).size() == stings.size(),
                "every sting should have an advancement of its own");
        var advancements = h.getLevel().getServer().getAdvancements();
        for (var entry : stings.entrySet()) {
            h.assertTrue(advancements.get(entry.getKey()) != null, "no such advancement for a sting: " + entry.getKey());
            h.assertTrue(ModSounds.STINGS.contains(entry.getValue()), entry.getKey() + " plays something that isn't a sting");
        }
        for (var sound : java.util.stream.Stream.concat(ModSounds.STINGS.stream(), ModSounds.BOSS_THEMES.stream()).toList()) {
            JsonElement first = sounds.getAsJsonObject(sound.getId().getPath()).getAsJsonArray("sounds").get(0);
            h.assertTrue(first.isJsonObject() && first.getAsJsonObject().get("stream").getAsBoolean(), sound.getId() + " should be streamed");
        }
        h.succeed();
    }

    /** 45c: both bosses' bars carry the "play boss music" flag, which is all the client has to go on. */
    static void bossBarsAskForTheirThemes(GameTestHelper h) {
        SunArchon archon = ModEntities.SUN_ARCHON.get().create(h.getLevel(), EntitySpawnReason.TRIGGERED);
        Mothership ship = ModEntities.MOTHERSHIP.get().create(h.getLevel(), EntitySpawnReason.TRIGGERED);
        h.assertTrue(archon != null && archon.bossEvent().shouldPlayBossMusic(), "the Sun Archon's bar doesn't ask for music");
        h.assertTrue(ship != null && ship.bossEvent().shouldPlayBossMusic(), "the Mothership's bar doesn't ask for music");
        h.succeed();
    }

    private static JsonObject soundsJson(GameTestHelper h) {
        try (var in = HollowFrontier.class.getResourceAsStream("/assets/" + HollowFrontier.MODID + "/sounds.json")) {
            h.assertTrue(in != null, "sounds.json missing");
            return JsonParser.parseString(new String(in.readAllBytes(), StandardCharsets.UTF_8)).getAsJsonObject();
        } catch (IOException e) {
            throw new IllegalStateException(e);
        }
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
