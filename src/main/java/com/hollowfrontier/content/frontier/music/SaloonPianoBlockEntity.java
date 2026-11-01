package com.hollowfrontier.content.frontier.music;

import java.util.List;

import com.hollowfrontier.content.frontier.outlaw.AbstractOutlaw;
import com.hollowfrontier.content.frontier.raid.TownRaids;
import com.hollowfrontier.content.frontier.saloon.Brawls;
import com.hollowfrontier.content.frontier.townsfolk.Lawman;
import com.hollowfrontier.registry.ModBlockEntities;
import com.hollowfrontier.registry.ModSounds;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.registries.DeferredHolder;

/**
 * Client side of a {@link SaloonPianoBlock}: while the piano is PLAYING and the local player is in earshot it plays a tune
 * from its repertoire ({@link ModSounds#PIANO_REPERTOIRE}) from the piano (positional, Records volume), and after a short
 * pause moves on to the next. Each piano starts at a different tune. Sounds go
 * through {@link #output}, which the client sets at startup, so this class never touches client code and is safe on a
 * dedicated server.
 */
public class SaloonPianoBlockEntity extends BlockEntity {
    /** Start playing when the listener comes this close; the recording itself fades out over 24 blocks. */
    public static final double AUDIBLE_RANGE = 32.0;
    // Ticks a just-started recording counts as playing before we trust the sound engine's answer.
    private static final int START_GRACE = 20;

    /** The client's sound handling for pianos, keyed by position. Null on dedicated servers. */
    public interface Output {
        void start(BlockPos pos, SoundEvent tune);

        void stop(BlockPos pos);

        boolean isPlaying(BlockPos pos);

        boolean listenerWithin(BlockPos pos, double range);
    }

    public static Output output;
    // Game time a piano last played in earshot; town music keeps quiet while one does.
    private static long lastHeard = Long.MIN_VALUE / 2;

    private boolean sounding;
    private int sinceStart;
    private int pause;
    private int tune = -1;

    public SaloonPianoBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.SALOON_PIANO.get(), pos, state);
    }

    public static boolean heardRecently(long gameTime) {
        return gameTime - lastHeard < 40;
    }

    static void clientTick(Level level, BlockPos pos, BlockState state, SaloonPianoBlockEntity piano) {
        if (output == null) {
            return;
        }
        if (!state.getValue(SaloonPianoBlock.PLAYING) || !output.listenerWithin(pos, AUDIBLE_RANGE)) {
            piano.silence();
            piano.pause = 0; // start straight away next time
            return;
        }
        if (state.getValue(SaloonPianoBlock.HUSHED)) {
            piano.silence(); // stopped dead mid-tune; a breath once it's quiet, then the next one
            piano.pause = 40;
            return;
        }
        lastHeard = level.getGameTime();
        piano.sinceStart++;
        if (piano.sounding && (piano.sinceStart < START_GRACE || output.isPlaying(pos))) {
            return;
        }
        if (piano.sounding) { // the tune ended: a breather, then the next one
            piano.sounding = false;
            piano.pause = 100 + level.getRandom().nextInt(100);
        }
        if (piano.pause-- > 0) {
            return;
        }
        output.start(pos, piano.nextTune().value());
        piano.sounding = true;
        piano.sinceStart = 0;
    }

    /** Once a second, on the server: hushed while there's trouble within {@link #TROUBLE_RANGE} (21b). */
    public static void serverTick(Level level, BlockPos pos, BlockState state, SaloonPianoBlockEntity piano) {
        if ((level.getGameTime() + pos.asLong()) % 20 != 0) {
            return;
        }
        boolean trouble = trouble(level, pos);
        if (state.getValue(SaloonPianoBlock.HUSHED) != trouble) {
            level.setBlock(pos, state.setValue(SaloonPianoBlock.HUSHED, trouble), Block.UPDATE_CLIENTS);
        }
    }

    public static final double TROUBLE_RANGE = 24.0;

    /** A brawl, a raid, or a gunfight (an outlaw or a lawman with somebody in his sights) near the piano. */
    public static boolean trouble(Level level, BlockPos pos) {
        if (Brawls.near(level, pos, TROUBLE_RANGE) || level instanceof ServerLevel server && TownRaids.raidAt(server, pos) != null) {
            return true;
        }
        return !level.getEntitiesOfClass(Mob.class, new AABB(pos).inflate(TROUBLE_RANGE),
                m -> (m instanceof AbstractOutlaw || m instanceof Lawman) && m.isAlive() && m.getTarget() != null && m.getTarget().isAlive()).isEmpty();
    }

    /** The tune to play next: each piano starts somewhere in the repertoire, then plays it in order. */
    public Holder<SoundEvent> nextTune() {
        List<DeferredHolder<SoundEvent, SoundEvent>> tunes = ModSounds.PIANO_REPERTOIRE;
        tune = tune < 0 ? Math.floorMod(worldPosition.hashCode(), tunes.size()) : (tune + 1) % tunes.size();
        return tunes.get(tune);
    }

    private void silence() {
        if (sounding) {
            output.stop(worldPosition);
            sounding = false;
        }
    }

    @Override
    public void setRemoved() {
        super.setRemoved();
        if (level != null && level.isClientSide() && output != null) {
            silence();
        }
    }
}
