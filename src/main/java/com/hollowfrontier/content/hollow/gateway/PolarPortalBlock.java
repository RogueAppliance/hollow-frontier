package com.hollowfrontier.content.hollow.gateway;

import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.InsideBlockEffectApplier;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.Portal;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.portal.TeleportTransition;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jspecify.annotations.Nullable;

/**
 * The open well of a Polar Gateway: a sheet of shimmering light lying flat in the ring, with a beam of light standing up
 * out of it (PolarPortalRenderer). Step in, frost creeps over your sight while the ring hums (GatewayOverlay), and you come
 * out at the other end (PolarGateways.destination): from the Overworld into the Hollow Earth, or back. No collision, so
 * you drop into it off the frame. Vanilla's portal cooldown keeps you from bouncing straight back.
 */
public class PolarPortalBlock extends Block implements Portal, EntityBlock {
    private static final VoxelShape SHAPE = Block.column(16.0, 0.0, 12.0);
    /** Ticks a survival player stands in it before crossing (creative players cross at once, as with nether portals). */
    public static final int CROSSING_TICKS = 40;

    public PolarPortalBlock(Properties properties) {
        super(properties);
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return SHAPE;
    }

    @Override
    protected VoxelShape getCollisionShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return net.minecraft.world.phys.shapes.Shapes.empty();
    }

    @Override
    protected VoxelShape getEntityInsideCollisionShape(BlockState state, BlockGetter level, BlockPos pos, Entity entity) {
        return state.getShape(level, pos);
    }

    @Override
    protected void entityInside(BlockState state, Level level, BlockPos pos, Entity entity, InsideBlockEffectApplier effectApplier, boolean isPrecise) {
        if (entity.canUsePortal(false)) {
            entity.setAsInsidePortal(this, pos);
        }
    }

    @Override
    public int getPortalTransitionTime(ServerLevel level, Entity entity) {
        return entity instanceof net.minecraft.world.entity.player.Player player && !player.getAbilities().invulnerable ? CROSSING_TICKS : 0;
    }

    @Override
    public @Nullable TeleportTransition getPortalDestination(ServerLevel currentLevel, Entity entity, BlockPos portalEntryPos) {
        return PolarGateways.destination(currentLevel, entity, portalEntryPos);
    }

    // No nether-portal swirl or sounds: the frost, the hum and the snow come from GatewayOverlay on the client instead.
    @Override
    public Transition getLocalTransition() {
        return Transition.NONE;
    }

    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new PolarPortalBlockEntity(pos, state);
    }

    @Override
    public void animateTick(BlockState state, Level level, BlockPos pos, RandomSource random) {
        if (random.nextInt(3) == 0) {
            double x = pos.getX() + random.nextDouble();
            double z = pos.getZ() + random.nextDouble();
            level.addParticle(ParticleTypes.END_ROD, x, pos.getY() + 0.8, z, 0.0, 0.03 + random.nextDouble() * 0.04, 0.0);
        }
        if (random.nextInt(6) == 0) {
            level.addParticle(ParticleTypes.SNOWFLAKE, pos.getX() + random.nextDouble(), pos.getY() + 0.9, pos.getZ() + random.nextDouble(), 0.0, 0.08, 0.0);
        }
    }

    @Override
    protected ItemStack getCloneItemStack(LevelReader level, BlockPos pos, BlockState state, boolean includeData) {
        return ItemStack.EMPTY;
    }

    @Override
    protected boolean canBeReplaced(BlockState state, Fluid fluid) {
        return false;
    }
}
