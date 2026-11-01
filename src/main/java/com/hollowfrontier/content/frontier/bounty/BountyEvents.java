package com.hollowfrontier.content.frontier.bounty;

import com.hollowfrontier.HollowFrontier;
import com.hollowfrontier.content.frontier.outlaw.AbstractOutlaw;
import com.hollowfrontier.content.frontier.outlaw.OutlawBoss;
import com.hollowfrontier.registry.ModDataComponents;
import com.hollowfrontier.registry.ModEntities;
import com.hollowfrontier.registry.ModItems;

import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.levelgen.Heightmap;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;

/**
 * Spawns a poster's gang lazily: once a player carrying the poster comes within SPAWN_RADIUS of the
 * hideout (so the chunks are loaded), the boss plus a few henchmen appear on the surface there and
 * the poster is marked spawned so it never spawns twice. Overworld only.
 */
@EventBusSubscriber(modid = HollowFrontier.MODID)
public final class BountyEvents {
    private BountyEvents() {}

    private static final int CHECK_INTERVAL = 40;
    private static final double SPAWN_RADIUS = 64.0;

    @SubscribeEvent
    static void onPlayerTick(PlayerTickEvent.Post event) {
        Player player = event.getEntity();
        if (player.tickCount % CHECK_INTERVAL == 0) {
            checkPosters(player);
        }
    }

    /** Spawns the gang for any carried, unspawned poster whose hideout is within SPAWN_RADIUS. */
    public static void checkPosters(Player player) {
        if (!(player.level() instanceof ServerLevel level) || level.dimension() != Level.OVERWORLD) {
            return;
        }
        Inventory inventory = player.getInventory();
        for (int slot = 0; slot < inventory.getContainerSize(); slot++) {
            ItemStack stack = inventory.getItem(slot);
            BountyData bounty = stack.get(ModDataComponents.BOUNTY.get());
            if (!stack.is(ModItems.WANTED_POSTER.get()) || bounty == null || bounty.spawned()) {
                continue;
            }
            double dx = player.getX() - bounty.x();
            double dz = player.getZ() - bounty.z();
            if (dx * dx + dz * dz <= SPAWN_RADIUS * SPAWN_RADIUS) {
                spawnGang(level, bounty);
                stack.set(ModDataComponents.BOUNTY.get(), bounty.markSpawned());
                player.sendSystemMessage(Component.translatable("message.hollowfrontier.bounty.spotted", bounty.target()));
            }
        }
    }

    private static void spawnGang(ServerLevel level, BountyData bounty) {
        BlockPos center = surface(level, bounty.x(), bounty.z());
        OutlawBoss boss = ModEntities.OUTLAW_BOSS.get().create(level, EntitySpawnReason.EVENT);
        if (boss != null) {
            boss.setBounty(bounty);
            place(level, boss, center);
        }
        spawnHenchman(level, ModEntities.OUTLAW.get(), center.offset(3, 0, 1));
        spawnHenchman(level, ModEntities.OUTLAW.get(), center.offset(-2, 0, 3));
        spawnHenchman(level, ModEntities.OUTLAW_GUNSLINGER.get(), center.offset(1, 0, -4));
    }

    private static void spawnHenchman(ServerLevel level, EntityType<? extends AbstractOutlaw> type, BlockPos near) {
        AbstractOutlaw outlaw = type.create(level, EntitySpawnReason.EVENT);
        if (outlaw != null) {
            outlaw.setPersistenceRequired();
            place(level, outlaw, surface(level, near.getX(), near.getZ()));
        }
    }

    private static void place(ServerLevel level, AbstractOutlaw outlaw, BlockPos pos) {
        outlaw.snapTo(pos.getX() + 0.5, pos.getY(), pos.getZ() + 0.5, level.getRandom().nextFloat() * 360.0F, 0.0F);
        outlaw.finalizeSpawn(level, level.getCurrentDifficultyAt(pos), EntitySpawnReason.EVENT, null);
        level.addFreshEntityWithPassengers(outlaw);
    }

    private static BlockPos surface(ServerLevel level, int x, int z) {
        return level.getHeightmapPos(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, new BlockPos(x, 0, z));
    }
}
