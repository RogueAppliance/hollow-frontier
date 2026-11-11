package com.hollowfrontier.content.frontier.town;

import java.util.Optional;

import com.hollowfrontier.content.frontier.raid.TownSite;
import com.hollowfrontier.worldgen.structure.ModStructures;
import com.hollowfrontier.worldgen.structure.TownNames;
import com.hollowfrontier.worldgen.structure.TownStyle;

import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.chunk.status.ChunkStatus;
import net.minecraft.world.level.levelgen.structure.StructureStart;

/** Looking towns up: by where something stands, by key, and what stands near one. Towns are Overworld only. */
public final class Towns {
    private Towns() {}

    /** The key of the town at pos (TownSite.keyOf), if pos is inside one's bounds. */
    public static Optional<Long> keyAt(ServerLevel level, BlockPos pos) {
        if (level.dimension() != Level.OVERWORLD) {
            return Optional.empty();
        }
        return TownSite.townAt(level, pos).map(TownSite::keyOf);
    }

    /** The town at pos or, failing that, one whose bounds lie within {@code reach} of it (looked for at sixteen points round pos). */
    public static Optional<StructureStart> near(ServerLevel level, BlockPos pos, int reach) {
        if (level.dimension() != Level.OVERWORLD) {
            return Optional.empty();
        }
        Optional<StructureStart> here = TownSite.townAt(level, pos);
        for (int ring = 1; ring <= 2 && here.isEmpty(); ring++) {
            for (int i = 0; i < 8 && here.isEmpty(); i++) {
                double a = Math.PI * 2 * i / 8;
                BlockPos probe = pos.offset((int) Math.round(Math.cos(a) * reach * ring / 2.0), 0, (int) Math.round(Math.sin(a) * reach * ring / 2.0));
                if (level.isLoaded(probe)) {
                    here = TownSite.townAt(level, probe);
                }
            }
        }
        return here;
    }

    public static String name(ServerLevel level, long key) {
        return TownNames.name(level, key);
    }

    /** The middle of a town's start chunk: near enough its square for a heading. */
    public static BlockPos middle(long key) {
        return ChunkPos.unpack(key).getMiddleBlockPosition(64);
    }

    /** The town's structure start, read from its start chunk (which is loaded as far as its structure starts if need be). */
    public static Optional<StructureStart> start(ServerLevel level, long key) {
        ChunkPos pos = ChunkPos.unpack(key);
        ChunkAccess chunk = level.getServer().overworld().getChunk(pos.x(), pos.z(), ChunkStatus.STRUCTURE_STARTS);
        var structures = level.registryAccess().lookupOrThrow(Registries.STRUCTURE);
        return ModStructures.TOWNS.stream().map(k -> chunk.getStartForStructure(structures.getValueOrThrow(k)))
                .filter(found -> found != null && found.isValid()).findFirst();
    }

    /** Which style a town start is built in (its buildings' templates are named for it). */
    public static TownStyle style(ServerLevel level, StructureStart start) {
        var structures = level.registryAccess().lookupOrThrow(Registries.STRUCTURE);
        for (TownStyle style : TownStyle.values()) {
            if (structures.getValue(style.structure()) == start.getStructure()) {
                return style;
            }
        }
        return TownStyle.FRONTIER;
    }
}
