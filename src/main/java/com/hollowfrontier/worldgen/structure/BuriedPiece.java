package com.hollowfrontier.worldgen.structure;

import com.hollowfrontier.registry.ModFeatures;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.level.levelgen.structure.PoolElementStructurePiece;
import net.minecraft.world.level.levelgen.structure.TerrainAdjustment;
import net.minecraft.world.level.levelgen.structure.pieces.StructurePieceSerializationContext;
import net.minecraft.world.level.levelgen.structure.pieces.StructurePieceType;
import net.minecraft.world.level.levelgen.structure.pools.JigsawJunction;
import net.minecraft.world.level.levelgen.structure.templatesystem.LiquidSettings;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplateManager;
import net.neoforged.neoforge.common.world.PieceBeardifierModifier;

/**
 * A dungeon room under a Hollow site (26): a jigsaw piece like any other, except that the terrain is left alone round it.
 * The site's terrain blend (BEARD_THIN) works on every rigid piece by its own floor, so without this each room buried
 * under a site would have the land dug away over its floor and heaped under it, leaving pits and caves round the dungeon.
 * The site's own piece, on the surface, keeps the blend.
 * <p>
 * Saved under its own piece type ({@link ModFeatures#BURIED_PIECE}), so a structure start read back from disk keeps
 * leaving the land alone for the chunks round it that generate later.
 */
public class BuriedPiece extends PoolElementStructurePiece implements PieceBeardifierModifier {
    /** The surface piece's room, copied as it was placed. */
    public BuriedPiece(StructureTemplateManager templates, PoolElementStructurePiece piece) {
        super(templates, piece.getElement(), piece.getPosition(), piece.getGroundLevelDelta(), piece.getRotation(), piece.getBoundingBox(),
                LiquidSettings.IGNORE_WATERLOGGING);
        for (JigsawJunction junction : piece.getJunctions()) {
            addJunction(junction);
        }
    }

    public BuriedPiece(StructurePieceSerializationContext context, CompoundTag tag) {
        super(context, tag);
    }

    @Override
    public StructurePieceType getType() {
        return ModFeatures.BURIED_PIECE.get();
    }

    @Override
    public BoundingBox getBeardifierBox() {
        return getBoundingBox();
    }

    @Override
    public TerrainAdjustment getTerrainAdjustment() {
        return TerrainAdjustment.NONE;
    }

    @Override
    public int getGroundLevelDelta() {
        return 0;
    }
}
