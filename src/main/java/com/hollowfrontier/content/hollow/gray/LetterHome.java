package com.hollowfrontier.content.hollow.gray;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;

/**
 * Where a Letter Home is going (20): the frontier town a freed captive was taken from, by its structure start (the key
 * TownSite saves a town under), the middle of that chunk and the town's name (24a), for the address. {@link #ANY} for a captive who couldn't say:
 * any town's board or sheriff will take it.
 */
public record LetterHome(long town, int x, int z, String name) {
    public static final long NOWHERE = Long.MIN_VALUE;
    public static final LetterHome ANY = new LetterHome(NOWHERE, 0, 0, "");

    public static final Codec<LetterHome> CODEC = RecordCodecBuilder.create(i -> i.group(
            Codec.LONG.fieldOf("town").forGetter(LetterHome::town),
            Codec.INT.fieldOf("x").forGetter(LetterHome::x),
            Codec.INT.fieldOf("z").forGetter(LetterHome::z),
            Codec.STRING.optionalFieldOf("name", "").forGetter(LetterHome::name)).apply(i, LetterHome::new));

    public static final StreamCodec<RegistryFriendlyByteBuf, LetterHome> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.VAR_LONG, LetterHome::town,
            ByteBufCodecs.VAR_INT, LetterHome::x,
            ByteBufCodecs.VAR_INT, LetterHome::z,
            ByteBufCodecs.STRING_UTF8, LetterHome::name,
            LetterHome::new);

    public boolean anyTown() {
        return town == NOWHERE;
    }

    /** Whether the town under this key is where it's going. */
    public boolean isFor(long key) {
        return anyTown() || town == key;
    }
}
