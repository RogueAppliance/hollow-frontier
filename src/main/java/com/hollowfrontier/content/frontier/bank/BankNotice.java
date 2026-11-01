package com.hollowfrontier.content.frontier.bank;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;

/**
 * News for a box owner: the gang (by surname) that cracked the bank in the town near x, z took copper's worth from
 * their box, and whether it came back (the bagman was shot).
 */
public record BankNotice(String gang, int x, int z, int copper, boolean returned, String town) {
    public static final Codec<BankNotice> CODEC = RecordCodecBuilder.create(i -> i.group(
            Codec.STRING.fieldOf("gang").forGetter(BankNotice::gang),
            Codec.INT.fieldOf("x").forGetter(BankNotice::x),
            Codec.INT.fieldOf("z").forGetter(BankNotice::z),
            Codec.INT.fieldOf("copper").forGetter(BankNotice::copper),
            Codec.BOOL.fieldOf("returned").forGetter(BankNotice::returned),
            Codec.STRING.optionalFieldOf("town", "").forGetter(BankNotice::town)).apply(i, BankNotice::new));

    public Component message() {
        return Component.translatable("message.hollowfrontier.bank.notice." + (returned ? "returned" : "lost"), gang, x, z, copper / 9, copper % 9, com.hollowfrontier.worldgen.structure.TownNames.phrase(town))
                .withStyle(returned ? ChatFormatting.GOLD : ChatFormatting.RED);
    }
}
