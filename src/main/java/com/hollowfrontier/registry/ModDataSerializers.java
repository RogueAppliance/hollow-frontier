package com.hollowfrontier.registry;

import com.hollowfrontier.HollowFrontier;
import com.hollowfrontier.content.frontier.bounty.BoardFace;

import net.minecraft.network.syncher.EntityDataSerializer;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.NeoForgeRegistries;

/** Entity data types of our own, for synced values vanilla has no serializer for. */
public final class ModDataSerializers {
    private ModDataSerializers() {}

    public static final DeferredRegister<EntityDataSerializer<?>> SERIALIZERS =
            DeferredRegister.create(NeoForgeRegistries.Keys.ENTITY_DATA_SERIALIZERS, HollowFrontier.MODID);

    /** What a hanging Bounty Board shows (18). */
    public static final DeferredHolder<EntityDataSerializer<?>, EntityDataSerializer<BoardFace>> BOARD_FACE = SERIALIZERS.register("board_face",
            () -> EntityDataSerializer.forValueType(BoardFace.STREAM_CODEC));
}
