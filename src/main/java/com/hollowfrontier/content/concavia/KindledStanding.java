package com.hollowfrontier.content.concavia;

import com.hollowfrontier.registry.ModAttachments;
import com.hollowfrontier.registry.ModCriteria;
import com.hollowfrontier.registry.ModItems;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

/**
 * How the Kindled (12a) think of a player, from {@link #MIN} to {@link #MAX}, and how much of their story they've told
 * them ({@code heard}, the lines of {@link Kindled#LINES} so far). Every trade wins a point of favour, and a trade in Gray
 * tech wins {@link #GRAY_TRADE}; at {@link #FRIEND} the guardians let the player carry Gray things through the ruins and
 * the Kindled tell them more, and at {@link #ALLIED} the player is their ally: they give them a {@link KindledSigilItem}
 * and open their last trades. Hurting one of them costs {@link #WRONG}, and killing one drops the player to {@link #MIN}:
 * below zero, the guardians shoot on sight and the Kindled won't trade. A grudge eases by a point every
 * {@link #FORGIVE_TICKS}. Kept through death; the {@code sigil} flag stops a second gift.
 */
public record KindledStanding(int favour, int heard, boolean sigil, long lastChange) {
    public static final KindledStanding NONE = new KindledStanding(0, 0, false, 0);
    public static final int MIN = -20;
    public static final int MAX = 40;
    public static final int FRIEND = 4;
    public static final int ALLIED = 12;
    public static final int TRADE = 1;
    public static final int GRAY_TRADE = 3;
    public static final int WRONG = -8;
    public static final int FORGIVE_TICKS = 2400;

    public static final Codec<KindledStanding> CODEC = RecordCodecBuilder.create(i -> i.group(
            Codec.INT.fieldOf("favour").forGetter(KindledStanding::favour),
            Codec.INT.optionalFieldOf("heard", 0).forGetter(KindledStanding::heard),
            Codec.BOOL.optionalFieldOf("sigil", false).forGetter(KindledStanding::sigil),
            Codec.LONG.optionalFieldOf("last_change", 0L).forGetter(KindledStanding::lastChange)).apply(i, KindledStanding::new));

    public static KindledStanding of(Player player) {
        return player.getData(ModAttachments.KINDLED_STANDING.get());
    }

    public static int favour(Player player) {
        return of(player).favour;
    }

    public static boolean friend(Player player) {
        return favour(player) >= FRIEND;
    }

    public static boolean allied(Player player) {
        return favour(player) >= ALLIED;
    }

    public static boolean wronged(Player player) {
        return favour(player) < 0;
    }

    /** Wins (or with a negative amount, loses) favour, telling the player when they cross a line. */
    public static void add(ServerPlayer player, int amount) {
        KindledStanding old = of(player);
        set(player, old.withFavour(Mth.clamp(old.favour + amount, MIN, MAX), player.level().getGameTime()));
    }

    public static void set(ServerPlayer player, KindledStanding standing) {
        KindledStanding old = of(player);
        player.setData(ModAttachments.KINDLED_STANDING.get(), standing);
        if (standing.favour >= ALLIED && old.favour < ALLIED) {
            player.sendSystemMessage(Component.translatable("message.hollowfrontier.kindled.allied").withStyle(ChatFormatting.AQUA));
            ModCriteria.fire(player, ModCriteria.KINDLED_ALLY);
            if (!standing.sigil) {
                giveSigil(player);
            }
        } else if (standing.favour >= FRIEND && old.favour < FRIEND) {
            player.sendOverlayMessage(Component.translatable("message.hollowfrontier.kindled.friend").withStyle(ChatFormatting.AQUA));
        } else if (standing.favour < 0 && old.favour >= 0) {
            player.sendOverlayMessage(Component.translatable("message.hollowfrontier.kindled.wronged").withStyle(ChatFormatting.DARK_AQUA));
        }
    }

    /** The ally's gift, once: into their inventory, or dropped at their feet if it's full. */
    private static void giveSigil(ServerPlayer player) {
        ItemStack sigil = new ItemStack(ModItems.KINDLED_SIGIL.get());
        if (!player.getInventory().add(sigil)) {
            player.spawnAtLocation(player.level(), sigil);
        }
        player.setData(ModAttachments.KINDLED_STANDING.get(), new KindledStanding(of(player).favour, of(player).heard, true, of(player).lastChange));
        player.sendSystemMessage(Component.translatable("message.hollowfrontier.kindled.sigil").withStyle(ChatFormatting.AQUA));
    }

    /** The next line of their story told. */
    public static void heardOne(ServerPlayer player) {
        KindledStanding s = of(player);
        player.setData(ModAttachments.KINDLED_STANDING.get(), new KindledStanding(s.favour, s.heard + 1, s.sigil, s.lastChange));
    }

    /** Once a second per player: a grudge eases a point every {@link #FORGIVE_TICKS}. */
    public static void tick(ServerPlayer player) {
        KindledStanding s = of(player);
        long now = player.level().getGameTime();
        if (s.favour < 0 && now - s.lastChange >= FORGIVE_TICKS) {
            set(player, s.withFavour(s.favour + 1, now));
        }
    }

    private KindledStanding withFavour(int favour, long time) {
        return new KindledStanding(favour, heard, sigil, time);
    }
}
