package com.hollowfrontier.content.frontier.gear;

import com.hollowfrontier.registry.ModCriteria;
import com.hollowfrontier.registry.ModEntities;
import com.hollowfrontier.registry.ModItems;

import net.minecraft.network.protocol.game.ClientboundSetEntityMotionPacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Leashable;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.throwableitemprojectile.ThrowableItemProjectile;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

/**
 * Thrown lasso. On hitting a living entity it yanks it toward the thrower; if the target is leashable
 * and ends up within leash range, it is leashed to the thrower (vanilla leash logic) and the lasso is used up.
 * Otherwise the lasso drops as an item.
 */
public class LassoEntity extends ThrowableItemProjectile {
    private static final double MAX_PULL = 1.8;
    private static final double PULL_PER_BLOCK = 0.12;

    public LassoEntity(EntityType<? extends LassoEntity> type, Level level) {
        super(type, level);
    }

    public LassoEntity(Level level, LivingEntity owner, net.minecraft.world.item.ItemStack stack) {
        super(ModEntities.LASSO.get(), owner, level, stack);
    }

    @Override
    protected Item getDefaultItem() {
        return ModItems.LASSO.get();
    }

    @Override
    protected void onHitEntity(EntityHitResult result) {
        super.onHitEntity(result);
        if (!(level() instanceof ServerLevel level) || !(getOwner() instanceof LivingEntity owner)) {
            return;
        }
        Entity target = result.getEntity();
        if (target == owner || !(target instanceof LivingEntity living)) {
            dropLasso(level);
            return;
        }

        // 47a: a deputy's drifter, roped: he gives up or he doesn't, and either way the lasso drops
        if (com.hollowfrontier.content.frontier.stake.SheriffJobs.rope(level, owner, target)) {
            dropLasso(level);
            return;
        }
        Vec3 toOwner = owner.position().subtract(target.position());
        double distance = toOwner.length();
        // Big animals barely budge.
        double weight = Math.max(1.0, living.getBbWidth() * living.getBbHeight());
        double strength = Math.min(distance * PULL_PER_BLOCK, MAX_PULL) / weight;
        target.push(toOwner.normalize().scale(strength).add(0, 0.3, 0));
        // push() syncs velocity to watchers, but a player's own client needs the motion packet.
        if (target instanceof ServerPlayer pulledPlayer) {
            pulledPlayer.connection.send(new ClientboundSetEntityMotionPacket(pulledPlayer));
        }
        level.playSound(null, target.getX(), target.getY(), target.getZ(), SoundEvents.LEAD_TIED, getSoundSource(), 1.0F, 1.0F);

        if (target instanceof Leashable leashable && leashable.canBeLeashed() && !leashable.isLeashed()
                && owner instanceof Player && distance <= leashable.leashSnapDistance()) {
            leashable.setLeashedTo(owner, true);
            ModCriteria.fire(owner, ModCriteria.LASSOED);
        } else {
            dropLasso(level);
        }
    }

    @Override
    protected void onHitBlock(BlockHitResult result) {
        super.onHitBlock(result);
        if (level() instanceof ServerLevel level) {
            dropLasso(level);
        }
    }

    @Override
    protected void onHit(HitResult result) {
        super.onHit(result);
        if (!level().isClientSide()) {
            discard();
        }
    }

    private void dropLasso(ServerLevel level) {
        if (!(getOwner() instanceof Player player && player.hasInfiniteMaterials())) {
            spawnAtLocation(level, getItem());
        }
    }
}
