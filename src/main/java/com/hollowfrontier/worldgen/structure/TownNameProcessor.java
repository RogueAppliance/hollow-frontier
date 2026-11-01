package com.hollowfrontier.worldgen.structure;

import java.util.List;
import java.util.OptionalLong;

import org.jspecify.annotations.Nullable;

import com.hollowfrontier.registry.ModFeatures;
import com.mojang.serialization.MapCodec;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.contents.TranslatableContents;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.entity.SignText;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructurePlaceSettings;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureProcessor;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplate;

/**
 * Writes the town's name on its signs as the town is built (24a). A template sign line that is the translation key
 * {@link #NAME} becomes the town's name ({@link TownNames}, from the world seed and the town's structure start, found
 * among the structures the chunk being built belongs to); a line {@link #POPULATION} becomes "POP. n". Every other block
 * passes through. On every town pool element; anything placed outside a town (a /place template) says "THIS TOWN".
 */
public final class TownNameProcessor implements StructureProcessor {
    public static final TownNameProcessor INSTANCE = new TownNameProcessor();
    public static final MapCodec<TownNameProcessor> CODEC = MapCodec.unit(INSTANCE);
    /** The sign lines replaced. */
    public static final String NAME = "sign.hollowfrontier.town_name";
    public static final String POPULATION = "sign.hollowfrontier.town_population";

    private TownNameProcessor() {}

    @Override
    public StructureTemplate.@Nullable StructureBlockInfo process(LevelReader level, BlockPos target, BlockPos reference,
            StructureTemplate.StructureBlockInfo original, StructureTemplate.StructureBlockInfo processed, StructurePlaceSettings settings,
            @Nullable StructureTemplate template) {
        CompoundTag nbt = processed.nbt();
        if (nbt == null || !nbt.contains("front_text") || !nbt.toString().contains(NAME) && !nbt.toString().contains(POPULATION)) {
            return processed;
        }
        OptionalLong key = townKey(level, processed.pos());
        long seed = level instanceof WorldGenLevel world ? world.getSeed() : 0;
        Component name = key.isPresent() ? Component.literal(TownNames.signName(seed, key.getAsLong()))
                : Component.translatable("sign.hollowfrontier.this_town");
        Component population = Component.translatable("sign.hollowfrontier.population", key.isPresent() ? TownNames.population(seed, key.getAsLong()) : 100);
        CompoundTag out = nbt.copy();
        for (String side : List.of("front_text", "back_text")) {
            if (!out.contains(side)) {
                continue;
            }
            SignText.CODEC.parse(NbtOps.INSTANCE, out.get(side)).ifSuccess(text -> {
                List<Component> lines = new java.util.ArrayList<>(text.getMessages(false));
                for (int i = 0; i < lines.size(); i++) {
                    if (lines.get(i).getContents() instanceof TranslatableContents line) {
                        if (line.getKey().equals(NAME)) {
                            lines.set(i, name);
                        } else if (line.getKey().equals(POPULATION)) {
                            lines.set(i, population);
                        }
                    }
                }
                SignText named = new SignText(lines, lines, text.getColor(), text.hasGlowingText());
                out.put(side, SignText.CODEC.encodeStart(NbtOps.INSTANCE, named).getOrThrow());
            });
        }
        return new StructureTemplate.StructureBlockInfo(processed.pos(), processed.state(), out);
    }

    /** The key (structure start chunk) of the town the chunk at pos belongs to, if it's in one. */
    static OptionalLong townKey(LevelReader level, BlockPos pos) {
        ChunkAccess chunk = level.getChunk(pos);
        for (var entry : chunk.getAllReferences().entrySet()) {
            if (entry.getKey() instanceof FrontierTownStructure && !entry.getValue().isEmpty()) {
                return OptionalLong.of(entry.getValue().iterator().nextLong());
            }
        }
        return OptionalLong.empty();
    }

    @Override
    public MapCodec<? extends StructureProcessor> codec() {
        return ModFeatures.TOWN_NAME_PROCESSOR.get();
    }
}
