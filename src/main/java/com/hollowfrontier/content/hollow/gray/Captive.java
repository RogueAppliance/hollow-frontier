package com.hollowfrontier.content.hollow.gray;

import java.util.Optional;
import java.util.UUID;

import com.hollowfrontier.content.frontier.bounty.BountyData;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import net.minecraft.core.UUIDUtil;

/**
 * Someone the Grays hold in their pens (20), as an attachment on the mob ({@link Captives}). {@code table}: strapped to the
 * operating table, next for the rig. {@code bounty}: the Wanted Poster he's the man on, once a hunter carrying it has seen
 * him. {@code captor}: the hunter walking him back to town alive; while it's set he's no longer held, only a prisoner.
 */
public record Captive(boolean table, Optional<BountyData> bounty, Optional<UUID> captor) {
    public static final Captive HELD = new Captive(false, Optional.empty(), Optional.empty());
    public static final Captive ON_TABLE = new Captive(true, Optional.empty(), Optional.empty());

    public static final Codec<Captive> CODEC = RecordCodecBuilder.create(i -> i.group(
            Codec.BOOL.optionalFieldOf("table", false).forGetter(Captive::table),
            BountyData.CODEC.optionalFieldOf("bounty").forGetter(Captive::bounty),
            UUIDUtil.CODEC.optionalFieldOf("captor").forGetter(Captive::captor)).apply(i, Captive::new));

    public boolean held() {
        return captor.isEmpty();
    }

    public Captive withBounty(BountyData data) {
        return new Captive(table, Optional.of(data), captor);
    }

    public Captive escortedBy(UUID hunter) {
        return new Captive(false, bounty, Optional.of(hunter));
    }
}
