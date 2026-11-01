package com.hollowfrontier.content.ufo;

import com.hollowfrontier.registry.ModBlockEntities;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.AABB;

/**
 * A Phlogiston Beacon's count down to the Mothership's coming (13d): {@link #SUMMON_TICKS} after it's lit, a rising hum and
 * sparks up the beam, then the Mothership comes down over it. The beam itself is drawn from the block's LIT state
 * (PhlogistonBeaconRenderer).
 */
public class PhlogistonBeaconBlockEntity extends BlockEntity {
    public static final int SUMMON_TICKS = 100;
    private int countdown;

    public PhlogistonBeaconBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.PHLOGISTON_BEACON.get(), pos, state);
    }

    public void startCountdown() {
        countdown = SUMMON_TICKS;
        setChanged();
    }

    public int countdown() {
        return countdown;
    }

    public static void serverTick(Level level, BlockPos pos, BlockState state, PhlogistonBeaconBlockEntity beacon) {
        if (beacon.countdown <= 0 || !(level instanceof ServerLevel server)) {
            return;
        }
        beacon.countdown--;
        beacon.setChanged();
        if (beacon.countdown % 5 == 0) {
            for (int i = 0; i < 6; i++) {
                double y = pos.getY() + 1 + server.getRandom().nextDouble() * 24;
                server.sendParticles(ParticleTypes.REVERSE_PORTAL, pos.getX() + 0.5, y, pos.getZ() + 0.5, 1, 0.2, 0.2, 0.2, 0.02);
            }
        }
        if (beacon.countdown % 20 == 0) {
            server.playSound(null, pos, SoundEvents.BEACON_AMBIENT, SoundSource.BLOCKS, 2.0F, 0.5F + (SUMMON_TICKS - beacon.countdown) / (float) SUMMON_TICKS);
        }
        if (beacon.countdown == 0) {
            Mothership.summon(server, pos);
            for (Player near : server.getEntitiesOfClass(Player.class, new AABB(pos).inflate(96))) {
                near.sendSystemMessage(Component.translatable("message.hollowfrontier.mothership.coming").withStyle(ChatFormatting.DARK_PURPLE));
            }
        }
    }

    @Override
    protected void saveAdditional(ValueOutput output) {
        super.saveAdditional(output);
        output.putInt("countdown", countdown);
    }

    @Override
    protected void loadAdditional(ValueInput input) {
        super.loadAdditional(input);
        countdown = input.getIntOr("countdown", 0);
    }
}
