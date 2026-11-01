package com.hollowfrontier.datagen;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Path;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Stream;

import com.google.common.hash.Hashing;
import com.hollowfrontier.HollowFrontier;
import com.hollowfrontier.gametest.TestBlueprints;
import com.hollowfrontier.worldgen.structure.Blueprint;
import com.hollowfrontier.worldgen.structure.FrontierBlueprints;
import com.hollowfrontier.worldgen.structure.HollowBlueprints;
import com.hollowfrontier.worldgen.structure.ConcaviaBlueprints;
import com.hollowfrontier.worldgen.structure.UfoBlueprints;

import net.minecraft.data.CachedOutput;
import net.minecraft.data.DataProvider;
import net.minecraft.data.PackOutput;
import net.minecraft.nbt.NbtIo;

// Writes every FrontierBlueprints, HollowBlueprints and TestBlueprints blueprint to data/hollowfrontier/structure/<name>.nbt (gzipped NBT).
public class StructureNbtProvider implements DataProvider {
    private final PackOutput.PathProvider pathProvider;

    public StructureNbtProvider(PackOutput output) {
        this.pathProvider = output.createPathProvider(PackOutput.Target.DATA_PACK, "structure");
    }

    @Override
    @SuppressWarnings("deprecation") // Hashing.sha1 is what vanilla's DataProvider uses for the cache
    public CompletableFuture<?> run(CachedOutput cache) {
        return CompletableFuture.runAsync(() -> {
            for (Blueprint blueprint : Stream.of(FrontierBlueprints.all(), HollowBlueprints.all(), ConcaviaBlueprints.all(), com.hollowfrontier.worldgen.structure.DungeonBlueprints.all(), com.hollowfrontier.worldgen.structure.WildBlueprints.all(), UfoBlueprints.all(), TestBlueprints.all()).flatMap(List::stream).toList()) {
                try {
                    ByteArrayOutputStream bytes = new ByteArrayOutputStream();
                    NbtIo.writeCompressed(blueprint.toNbt(), bytes);
                    byte[] data = bytes.toByteArray();
                    Path path = pathProvider.file(HollowFrontier.id(blueprint.name()), "nbt");
                    cache.writeIfNeeded(path, data, Hashing.sha1().hashBytes(data));
                } catch (IOException e) {
                    throw new UncheckedIOException("Failed to write structure " + blueprint.name(), e);
                }
            }
        });
    }

    @Override
    public String getName() {
        return "Structure Blueprints - " + HollowFrontier.MODID;
    }
}
