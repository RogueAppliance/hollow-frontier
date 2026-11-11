package com.hollowfrontier.client;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import com.hollowfrontier.HollowFrontier;
import com.hollowfrontier.registry.ModSounds;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;

import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.client.resources.sounds.SoundInstance;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvent;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.RegisterClientCommandsEvent;
import net.neoforged.neoforge.client.event.SelectMusicEvent;
import net.neoforged.neoforge.registries.DeferredHolder;

/**
 * The listening pass (45): a dev-only client command that plays the phase's sounds one after another, naming each in
 * chat, so they can be checked by ear in {@code runClient} without walking to each biome.
 * <ul>
 * <li>{@code /hfaudition}: every sound of every set, 45 seconds of each</li>
 * <li>{@code /hfaudition <set> [seconds]}: one set ("score"); 0 seconds plays each sound to its end</li>
 * <li>{@code /hfaudition stop}</li>
 * </ul>
 * The game's own music is held while it runs. A root of its own, so the server's {@code /hollowfrontier} is left alone.
 */
@EventBusSubscriber(modid = HollowFrontier.MODID, value = Dist.CLIENT)
public final class Audition {
    private Audition() {}

    private static final int DEFAULT_SECONDS = 45;
    // A streamed sound isn't "active" the tick it's asked for; don't take it for finished until this many ticks in
    private static final int GRACE = 40;

    private static final Deque<DeferredHolder<SoundEvent, SoundEvent>> QUEUE = new ArrayDeque<>();
    private static SoundInstance playing;
    private static int ticks;
    private static int limit;

    private static Map<String, List<DeferredHolder<SoundEvent, SoundEvent>>> sets() {
        Map<String, List<DeferredHolder<SoundEvent, SoundEvent>>> sets = new LinkedHashMap<>();
        sets.put("score", ModSounds.HOLLOW_SCORE);
        sets.put("beds", ModSounds.HOLLOW_BEDS); // loops: give a number of seconds, or they never end
        sets.put("far", List.of(ModSounds.MAMMOTH_TRUMPET, ModSounds.TERROR_BIRD_SCREECH, ModSounds.MEGATHERIUM_AMBIENT, ModSounds.PALE_RAVEN_AMBIENT,
                ModSounds.BASILOSAURUS_AMBIENT, ModSounds.PLESIOSAUR_AMBIENT, ModSounds.SEA_COW_AMBIENT, ModSounds.CRUST_RUMBLE));
        sets.put("themes", ModSounds.BOSS_THEMES); // loops too
        sets.put("stings", ModSounds.STINGS);
        sets.put("carols", ModSounds.PIANO_CAROLS); // 35c, never checked by ear
        sets.put("submersible", List.of(ModSounds.SUBMERSIBLE_THRUM, ModSounds.SUBMERSIBLE_CREAK));
        return sets;
    }

    @SubscribeEvent
    static void onRegisterClientCommands(RegisterClientCommandsEvent event) {
        event.getDispatcher().register(Commands.literal("hfaudition").executes(ctx -> start(null, DEFAULT_SECONDS))
                .then(Commands.literal("stop").executes(ctx -> {
                    stop();
                    return 1;
                }))
                .then(Commands.argument("set", StringArgumentType.word())
                        .suggests((ctx, builder) -> {
                            sets().keySet().forEach(builder::suggest);
                            return builder.buildFuture();
                        })
                        .executes(ctx -> start(StringArgumentType.getString(ctx, "set"), DEFAULT_SECONDS))
                        .then(Commands.argument("seconds", IntegerArgumentType.integer(0, 600))
                                .executes(ctx -> start(StringArgumentType.getString(ctx, "set"), IntegerArgumentType.getInteger(ctx, "seconds"))))));
    }

    private static int start(String set, int seconds) {
        stop();
        if (set == null) {
            sets().values().forEach(QUEUE::addAll);
        } else if (sets().containsKey(set)) {
            QUEUE.addAll(sets().get(set));
        } else {
            say("No such set: " + set + " (there's " + String.join(", ", sets().keySet()) + ")");
            return 0;
        }
        limit = seconds * 20;
        say("Audition: " + QUEUE.size() + " sounds" + (seconds > 0 ? ", " + seconds + " s of each" : ", each to its end"));
        return QUEUE.size();
    }

    private static void stop() {
        QUEUE.clear();
        if (playing != null) {
            Minecraft.getInstance().getSoundManager().stop(playing);
            playing = null;
        }
    }

    @SubscribeEvent
    static void onClientTick(ClientTickEvent.Post event) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null) {
            stop();
            return;
        }
        if (playing != null) {
            ticks++;
            boolean over = limit > 0 && ticks >= limit;
            boolean ended = ticks > GRACE && !mc.getSoundManager().isActive(playing);
            if (!over && !ended) {
                return;
            }
            mc.getSoundManager().stop(playing);
            playing = null;
            if (QUEUE.isEmpty()) {
                say("Audition over");
            }
        }
        DeferredHolder<SoundEvent, SoundEvent> next = QUEUE.poll();
        if (next != null) {
            playing = SimpleSoundInstance.forMusic(next.get());
            ticks = 0;
            mc.getSoundManager().play(playing);
            say("Now: " + next.getId().getPath());
        }
    }

    // Nothing else plays as music while a sound is being listened to
    @SubscribeEvent(priority = EventPriority.LOWEST, receiveCanceled = true)
    static void onSelectMusic(SelectMusicEvent event) {
        if (playing != null || !QUEUE.isEmpty()) {
            event.setMusic(null);
        }
    }

    private static void say(String line) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player != null) {
            mc.player.sendSystemMessage(Component.literal(line));
        }
    }
}
