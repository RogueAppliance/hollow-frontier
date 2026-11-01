package com.hollowfrontier.content.frontier.bounty;

import java.util.UUID;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import net.minecraft.core.UUIDUtil;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;

/**
 * One bounty, stored on both the Wanted Poster and the Outlaw Badge the boss drops.
 * (x, z) is where the gang hides out; spawned flips once the gang has been placed in the world.
 * Reward is in gold coins. captive (20): the hunter found him in a Gray pen instead, so no gang will ever be at the hideout.
 */
public record BountyData(UUID id, String target, int x, int z, int reward, boolean spawned, boolean captive) {
    public static final Codec<BountyData> CODEC = RecordCodecBuilder.create(i -> i.group(
            UUIDUtil.CODEC.fieldOf("id").forGetter(BountyData::id),
            Codec.STRING.fieldOf("target").forGetter(BountyData::target),
            Codec.INT.fieldOf("x").forGetter(BountyData::x),
            Codec.INT.fieldOf("z").forGetter(BountyData::z),
            Codec.INT.fieldOf("reward").forGetter(BountyData::reward),
            Codec.BOOL.optionalFieldOf("spawned", false).forGetter(BountyData::spawned),
            Codec.BOOL.optionalFieldOf("captive", false).forGetter(BountyData::captive)
    ).apply(i, BountyData::new));

    public static final StreamCodec<RegistryFriendlyByteBuf, BountyData> STREAM_CODEC = StreamCodec.composite(
            UUIDUtil.STREAM_CODEC, BountyData::id,
            ByteBufCodecs.STRING_UTF8, BountyData::target,
            ByteBufCodecs.VAR_INT, BountyData::x,
            ByteBufCodecs.VAR_INT, BountyData::z,
            ByteBufCodecs.VAR_INT, BountyData::reward,
            ByteBufCodecs.BOOL, BountyData::spawned,
            ByteBufCodecs.BOOL, BountyData::captive,
            BountyData::new);

    public BountyData(UUID id, String target, int x, int z, int reward, boolean spawned) {
        this(id, target, x, z, reward, spawned, false);
    }

    public BountyData markSpawned() {
        return new BountyData(id, target, x, z, reward, true, captive);
    }

    /** Found in a Gray pen: spawned too, so the hideout stays empty. */
    public BountyData markCaptive() {
        return new BountyData(id, target, x, z, reward, true, true);
    }
}
