package com.hollowfrontier.content.frontier.law;

import com.hollowfrontier.content.frontier.bounty.Purse;
import com.hollowfrontier.content.frontier.talk.Chatter;
import com.hollowfrontier.content.frontier.talk.Moment;
import com.hollowfrontier.content.frontier.townsfolk.Lawman;
import com.hollowfrontier.registry.ModAttachments;
import com.hollowfrontier.registry.ModCriteria;
import com.hollowfrontier.registry.ModEntities;
import com.hollowfrontier.registry.ModSounds;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.levelgen.Heightmap;

/**
 * A player's standing with the law. Crimes against townsfolk and lawmen add heat; heat shows as 0-3 stars:
 * 1 star and the law keeps an eye on you, 2 and lawmen shoot on sight and townsfolk won't trade, 3 and a posse rides
 * out after you. Heat cools by one point every COOL_TICKS without a new crime, gunning down outlaws takes a point off,
 * paying the fine at a Bounty Board clears it, and dying wipes the slate (the attachment isn't kept on death).
 * Creative and spectator players are never wanted.
 */
public record Wanted(int heat, long lastChange, long nextPosse) {
    public static final Wanted NONE = new Wanted(0, 0, 0);
    public static final int MAX_HEAT = 12;
    public static final int COOL_TICKS = 2400;
    public static final int POSSE_INTERVAL = 3600;
    /** Fine per point of heat, in copper coins (a gold coin is worth 9). */
    public static final int FINE_PER_HEAT = 4;

    // Heat for each crime.
    public static final int HURT_TOWNSFOLK = 1;
    public static final int KILL_TOWNSFOLK = 3;
    public static final int HURT_LAWMAN = 2;
    public static final int KILL_LAWMAN = 4;
    public static final int KILL_PRISONER = 2;

    public static final Codec<Wanted> CODEC = RecordCodecBuilder.create(i -> i.group(
            Codec.INT.fieldOf("heat").forGetter(Wanted::heat),
            Codec.LONG.fieldOf("last_change").forGetter(Wanted::lastChange),
            Codec.LONG.fieldOf("next_posse").forGetter(Wanted::nextPosse)).apply(i, Wanted::new));
    // The client only needs the heat for the HUD.
    public static final StreamCodec<RegistryFriendlyByteBuf, Wanted> STREAM_CODEC = ByteBufCodecs.VAR_INT
            .map(heat -> new Wanted(heat, 0, 0), Wanted::heat).cast();

    public int stars() {
        return starsFor(heat);
    }

    public static int starsFor(int heat) {
        return heat <= 0 ? 0 : heat < 4 ? 1 : heat < 8 ? 2 : 3;
    }

    public static Wanted of(Player player) {
        return player.getData(ModAttachments.WANTED.get());
    }

    public static int stars(Player player) {
        return of(player).stars();
    }

    /** Adds (or with a negative amount, removes) heat for a crime or a good deed. */
    public static void addHeat(ServerPlayer player, int amount) {
        if (player.isCreative() || player.isSpectator()) {
            return;
        }
        Wanted old = of(player);
        int heat = Mth.clamp(old.heat + amount, 0, MAX_HEAT);
        long now = player.level().getGameTime();
        // The first time the posse threshold is crossed, give the player a minute's head start.
        long nextPosse = starsFor(heat) >= 3 && old.stars() < 3 ? now + 1200 : old.nextPosse;
        set(player, new Wanted(heat, now, nextPosse), old.stars());
    }

    public static void clear(ServerPlayer player) {
        set(player, NONE, stars(player));
    }

    private static void set(ServerPlayer player, Wanted wanted, int oldStars) {
        player.setData(ModAttachments.WANTED.get(), wanted);
        int stars = wanted.stars();
        if (stars != oldStars) {
            Component message = stars == 0
                    ? Component.translatable("message.hollowfrontier.wanted.clear").withStyle(ChatFormatting.GREEN)
                    : Component.translatable("message.hollowfrontier.wanted." + stars).withStyle(ChatFormatting.RED);
            player.sendOverlayMessage(message);
            if (stars >= 2 && oldStars < 2) {
                com.hollowfrontier.content.frontier.stake.Deputies.disgrace(player); // 47a: the star comes off
            }
            if (stars > oldStars) {
                player.level().playSound(null, player.blockPosition(), ModSounds.WANTED_RAISED.get(), SoundSource.PLAYERS, 1.0F, 1.0F);
                ModCriteria.fire(player, ModCriteria.WANTED_LEVEL, stars);
            }
        }
    }

    /** Once a second per player (from LawEvents; public so game tests can drive it): cool off, send the posse. */
    public static void tick(ServerPlayer player) {
        Wanted wanted = of(player);
        if (wanted.heat <= 0) {
            return;
        }
        long now = player.level().getGameTime();
        if (now - wanted.lastChange >= COOL_TICKS) {
            int oldStars = wanted.stars();
            wanted = new Wanted(wanted.heat - 1, now, wanted.nextPosse);
            set(player, wanted, oldStars);
        }
        if (wanted.stars() >= 3 && now >= wanted.nextPosse && player.level() instanceof ServerLevel level && level.dimension() == Level.OVERWORLD) {
            player.setData(ModAttachments.WANTED.get(), new Wanted(wanted.heat, wanted.lastChange, now + POSSE_INTERVAL));
            sendPosse(level, player);
        }
    }

    /** Two marshals or rangers ride in from 24-32 blocks out, hunting the player (up to 8 tries for spots in loaded chunks). */
    public static void sendPosse(ServerLevel level, ServerPlayer player) {
        int sent = 0;
        for (int attempt = 0; attempt < 8 && sent < 2; attempt++) {
            float angle = level.getRandom().nextFloat() * Mth.TWO_PI;
            int distance = 24 + level.getRandom().nextInt(9);
            BlockPos column = player.blockPosition().offset(Math.round(Mth.cos(angle) * distance), 0, Math.round(Mth.sin(angle) * distance));
            BlockPos ground = level.getHeightmapPos(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, column);
            Lawman lawman = ModEntities.LAWMAN.get().create(level, EntitySpawnReason.EVENT);
            if (lawman == null || !level.isLoaded(ground) || !level.getWorldBorder().isWithinBounds(ground)) {
                continue;
            }
            lawman.setRank(level.getRandom().nextBoolean() ? Lawman.Rank.MARSHAL : Lawman.Rank.RANGER);
            lawman.snapTo(ground.getX() + 0.5, ground.getY(), ground.getZ() + 0.5, angle, 0.0F);
            lawman.finalizeSpawn(level, level.getCurrentDifficultyAt(ground), EntitySpawnReason.EVENT, null);
            lawman.joinPosse(player);
            if (level.addFreshEntity(lawman) && sent++ == 0) {
                Chatter.say(lawman, player, Moment.POSSE);
            }
        }
        if (sent > 0) {
            player.sendSystemMessage(Component.translatable("message.hollowfrontier.wanted.posse").withStyle(ChatFormatting.DARK_RED));
        }
    }

    /** Pays the fine from the player's coins (gold counts as 9 copper, change comes back). False if they can't cover it. */
    public static boolean payFine(ServerPlayer player) {
        if (!Purse.pay(player, fine(player))) {
            return false;
        }
        clear(player);
        ModCriteria.fire(player, ModCriteria.WANTED_CLEARED);
        return true;
    }

    public static int fine(Player player) {
        return of(player).heat * FINE_PER_HEAT;
    }
}
