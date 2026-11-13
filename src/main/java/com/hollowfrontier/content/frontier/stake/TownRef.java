package com.hollowfrontier.content.frontier.stake;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;

/** The town a badge, a deed or a warrant belongs to: its key (TownSite.keyOf) and its name, kept so the item can say it. */
public record TownRef(long key, String name) {
    public static final Codec<TownRef> CODEC = RecordCodecBuilder.create(i -> i.group(
            Codec.LONG.fieldOf("key").forGetter(TownRef::key),
            Codec.STRING.fieldOf("name").forGetter(TownRef::name)).apply(i, TownRef::new));
    public static final StreamCodec<RegistryFriendlyByteBuf, TownRef> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.VAR_LONG, TownRef::key, ByteBufCodecs.STRING_UTF8, TownRef::name, TownRef::new);
}
