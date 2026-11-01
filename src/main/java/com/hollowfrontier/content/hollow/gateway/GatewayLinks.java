package com.hollowfrontier.content.hollow.gateway;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;

import com.hollowfrontier.HollowFrontier;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.level.saveddata.SavedDataType;

/**
 * Which Overworld gateway leads to which return gateway in the Hollow Earth (the centres of their wells). Made the first
 * time anything crosses an Overworld gateway, so every later trip through it lands at the same place inside, and the
 * return gateway leads back to it. Kept with the Overworld's saved data.
 */
public class GatewayLinks extends SavedData {
    public record Link(BlockPos overworld, BlockPos hollow) {
        static final Codec<Link> CODEC = RecordCodecBuilder.create(i -> i.group(
                BlockPos.CODEC.fieldOf("overworld").forGetter(Link::overworld),
                BlockPos.CODEC.fieldOf("hollow").forGetter(Link::hollow)).apply(i, Link::new));
    }

    public static final Codec<GatewayLinks> CODEC = Link.CODEC.listOf().fieldOf("links").xmap(GatewayLinks::new, d -> List.copyOf(d.links)).codec();
    public static final SavedDataType<GatewayLinks> TYPE = new SavedDataType<>(HollowFrontier.id("gateway_links"), GatewayLinks::new, CODEC);

    private final List<Link> links = new ArrayList<>();

    public GatewayLinks() {}

    private GatewayLinks(List<Link> links) {
        this.links.addAll(links);
    }

    public static GatewayLinks get(ServerLevel level) {
        return level.getServer().overworld().getDataStorage().computeIfAbsent(TYPE);
    }

    public Optional<Link> fromOverworld(BlockPos center) {
        return links.stream().filter(link -> link.overworld().equals(center)).findFirst();
    }

    public Optional<Link> fromHollow(BlockPos center) {
        return links.stream().filter(link -> link.hollow().equals(center)).findFirst();
    }

    /** The return gateway nearest a spot in the Hollow Earth, within range blocks (the Polar Compass points to it). */
    public Optional<Link> nearestHollow(BlockPos pos, double range) {
        return links.stream().filter(link -> link.hollow().distSqr(pos) <= range * range)
                .min(Comparator.comparingDouble(link -> link.hollow().distSqr(pos)));
    }

    public void add(Link link) {
        links.add(link);
        setDirty();
    }

    public List<Link> all() {
        return List.copyOf(links);
    }
}
