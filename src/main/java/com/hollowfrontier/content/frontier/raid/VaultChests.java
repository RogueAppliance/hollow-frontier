package com.hollowfrontier.content.frontier.raid;

import java.util.List;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;

import com.hollowfrontier.HollowFrontier;
import com.hollowfrontier.registry.ModAttachments;

import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.ChestBlockEntity;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.level.storage.loot.LootTable;
import net.minecraft.world.level.storage.loot.parameters.LootContextParamSets;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.level.ChunkEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;

/**
 * Keeps a town's vault chests plain chests. Lootr turns any chest with a loot table into a per-player one, which would
 * lose the town_vault mark raids find them by and leave nothing to rob. So each marked chest rolls its table as soon as
 * its chunk loads, at the start of the next tick. Lootr converts at the end of a tick, the tick after a chest loads at the
 * earliest, and skips a chest with no table left. It works the same with or without Lootr, and no Lootr code is touched.
 */
@EventBusSubscriber(modid = HollowFrontier.MODID)
public final class VaultChests {
    private VaultChests() {}

    private static final Queue<LevelChunk> LOADED = new ConcurrentLinkedQueue<>();

    @SubscribeEvent
    static void onChunkLoad(ChunkEvent.Load event) {
        // The chunk itself, not its position: a chunk still on its way in isn't findable by position next tick
        if (event.getLevel() instanceof ServerLevel level && level.dimension() == Level.OVERWORLD && event.getChunk() instanceof LevelChunk chunk) {
            LOADED.add(chunk);
        }
    }

    // Not in the load event itself: filling a chest updates comparators around it, which can reach for chunks still loading.
    @SubscribeEvent(priority = EventPriority.HIGHEST)
    static void onServerTick(ServerTickEvent.Pre event) {
        ServerLevel level = event.getServer().overworld();
        for (LevelChunk chunk = LOADED.poll(); chunk != null; chunk = LOADED.poll()) {
            stock(level, chunk);
        }
    }

    /** Fills the chunk's vault chests that haven't rolled their loot yet. */
    public static void stock(ServerLevel level, LevelChunk chunk) {
        // By position, and made now: a newly generated chunk's block entities may still be pending, and getBlockEntities leaves those out
        for (BlockPos pos : List.copyOf(chunk.getBlockEntitiesPos())) {
            BlockEntity entity = chunk.getBlockEntity(pos, LevelChunk.EntityCreationType.IMMEDIATE);
            if (entity instanceof ChestBlockEntity chest && chest.hasData(ModAttachments.TOWN_VAULT.get()) && chest.getLootTable() != null) {
                fill(level, chest);
            }
        }
    }

    // RandomizableContainer.unpackLootTable would do, but Lootr cancels that for any chest it could convert.
    private static void fill(ServerLevel level, ChestBlockEntity chest) {
        ResourceKey<LootTable> table = chest.getLootTable();
        long seed = chest.getLootTableSeed();
        chest.setLootTable(null);
        LootParams params = new LootParams.Builder(level).withParameter(LootContextParams.ORIGIN, Vec3.atCenterOf(chest.getBlockPos()))
                .create(LootContextParamSets.CHEST);
        level.getServer().reloadableRegistries().getLootTable(table).fill(chest, params, seed);
        chest.setChanged();
    }
}
