package com.hollowfrontier.content.hollow.gray;

import java.util.Comparator;
import java.util.List;
import java.util.UUID;

import org.jspecify.annotations.Nullable;

import com.hollowfrontier.content.frontier.talk.Chatter;
import com.hollowfrontier.content.ufo.Leash;
import com.hollowfrontier.registry.ModBlockEntities;
import com.hollowfrontier.registry.ModCriteria;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerBossEvent;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.BossEvent;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.AABB;

/**
 * What a Gray pen's implant rig is doing (20). It keeps the pen: once a second its captives may call out to a player near
 * ({@link Chatter#lookAround}), and a hunter's poster may turn out to name one of them ({@link Captives#recognise}).
 * <ul>
 * <li><b>The procedure:</b> the first time a player (not in creative) comes within {@link #SPOT_RANGE}, the Grays start on
 *     the captive strapped to the table under it, if there is one and a surgeon (a Warden tagged {@link Captives#SURGEON})
 *     still stands. A "Procedure" bar fills over {@link #DURATION} ticks for everyone within {@link #BAR_RANGE}. Every
 *     surgeon down, or the rig broken, stops it: everyone near earns Not on My Watch, and the captive stays on the table
 *     to be freed. If it fills, the captive gets up implanted and hostile ({@link Captives#implant}). It runs once.</li>
 * <li><b>After the leash</b> there's nothing to implant into: no procedure. The pen is a holdout like a landing (15a): once
 *     every Warden keeping it is down, everyone near earns Trespassers.</li>
 * </ul>
 */
public class ImplantRigBlockEntity extends BlockEntity {
    public static final int DURATION = 1200;
    public static final double SPOT_RANGE = 20.0, BAR_RANGE = 48.0, CREW_RANGE = 64.0, CAPTIVE_RANGE = 24.0, WITNESS_RANGE = 96.0;
    /** How far below the rig the table can be. */
    public static final int TABLE_REACH = 4;

    public enum Stage {
        /** Nobody's come yet. */
        WAITING,
        RUNNING,
        /** Stopped, finished, or never to be: the rig is done. */
        OVER
    }

    private Stage stage = Stage.WAITING;
    private int progress;
    private boolean cleared;
    private @Nullable ServerBossEvent bar;

    public ImplantRigBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.IMPLANT_RIG.get(), pos, state);
    }

    public Stage stage() {
        return stage;
    }

    public int progress() {
        return progress;
    }

    public boolean cleared() {
        return cleared;
    }

    /** For tests: how far along the procedure is. */
    public void setProgress(int ticks) {
        progress = ticks;
        setChanged();
    }

    public static void serverTick(Level level, BlockPos pos, BlockState state, ImplantRigBlockEntity rig) {
        if (!(level instanceof ServerLevel server)) {
            return;
        }
        if (rig.stage == Stage.RUNNING) {
            rig.run(server, pos);
        } else if ((server.getGameTime() + pos.asLong()) % 20 == 0) {
            rig.watch(server, pos);
        }
    }

    /** Once a second while it isn't operating: the captives, the posters, and whether to start (after the leash: whether the pen's cleared). */
    public void watch(ServerLevel level, BlockPos pos) {
        for (Mob captive : Captives.heldNear(level, pos, CAPTIVE_RANGE)) {
            Chatter.lookAround(captive);
        }
        ServerPlayer spotted = spotter(level, pos);
        if (spotted != null) {
            Captives.recognise(level, spotted, pos);
        }
        if (Leash.isBroken(level)) {
            if (!cleared && spotted != null && Captives.guards(level, pos, CREW_RANGE).isEmpty()) {
                cleared = true;
                setChanged();
                for (ServerPlayer near : players(level, pos, WITNESS_RANGE)) {
                    near.sendSystemMessage(Component.translatable("message.hollowfrontier.pens.cleared").withStyle(ChatFormatting.GRAY, ChatFormatting.ITALIC));
                    ModCriteria.fire(near, ModCriteria.LANDING_CLEARED);
                }
            }
            return;
        }
        if (stage == Stage.WAITING && spotted != null) {
            start(level, pos);
        }
    }

    /** The Grays start on the one on the table, if there's one and a surgeon to do it. Whether they did. */
    public boolean start(ServerLevel level, BlockPos pos) {
        Mob patient = patient(level, pos);
        if (patient == null || Captives.surgeons(level, pos, CREW_RANGE).isEmpty()) {
            stage = Stage.OVER;
            setChanged();
            return false;
        }
        stage = Stage.RUNNING;
        progress = 0;
        setChanged();
        for (ServerPlayer near : players(level, pos, BAR_RANGE)) {
            near.sendSystemMessage(Component.translatable("message.hollowfrontier.procedure.started", patient.getDisplayName()).withStyle(ChatFormatting.DARK_AQUA));
        }
        level.playSound(null, pos, SoundEvents.BEACON_ACTIVATE, SoundSource.HOSTILE, 1.5F, 1.6F);
        return true;
    }

    private void run(ServerLevel level, BlockPos pos) {
        progress++;
        setChanged();
        ServerBossEvent bar = bar();
        bar.setProgress(Math.min(1.0F, progress / (float) DURATION));
        if (progress % 10 == 0) {
            level.sendParticles(ParticleTypes.ELECTRIC_SPARK, pos.getX() + 0.5, pos.getY() - 0.2, pos.getZ() + 0.5, 6, 0.3, 0.1, 0.3, 0.05);
        }
        if (progress % 40 == 0) {
            level.playSound(null, pos, SoundEvents.BEACON_AMBIENT, SoundSource.HOSTILE, 1.5F, 1.4F + progress / (float) DURATION);
        }
        if (progress % 20 != 0 && progress < DURATION) {
            return;
        }
        List<ServerPlayer> near = players(level, pos, BAR_RANGE);
        for (ServerPlayer player : List.copyOf(bar.getPlayers())) {
            if (!near.contains(player)) {
                bar.removePlayer(player);
            }
        }
        near.forEach(bar::addPlayer);
        Mob patient = patient(level, pos);
        if (patient == null) {
            end();
        } else if (Captives.surgeons(level, pos, CREW_RANGE).isEmpty()) {
            stop(level, pos);
        } else if (progress >= DURATION) {
            for (ServerPlayer player : near) {
                player.sendSystemMessage(Component.translatable("message.hollowfrontier.procedure.finished", patient.getDisplayName()).withStyle(ChatFormatting.DARK_RED));
            }
            end();
            Captives.implant(level, patient);
        }
    }

    /** The procedure stopped (its surgeons down, or the rig broken): everyone near earns Not on My Watch. */
    private void stop(ServerLevel level, BlockPos pos) {
        for (ServerPlayer player : players(level, pos, BAR_RANGE)) {
            player.sendSystemMessage(Component.translatable("message.hollowfrontier.procedure.stopped").withStyle(ChatFormatting.GREEN));
            ModCriteria.fire(player, ModCriteria.PROCEDURE_STOPPED);
        }
        level.playSound(null, pos, SoundEvents.BEACON_DEACTIVATE, SoundSource.HOSTILE, 1.5F, 1.2F);
        end();
    }

    private void end() {
        stage = Stage.OVER;
        setChanged();
        if (bar != null) {
            bar.removeAllPlayers();
            bar = null;
        }
    }

    private ServerBossEvent bar() {
        if (bar == null) {
            bar = new ServerBossEvent(UUID.randomUUID(), Component.translatable("bossbar.hollowfrontier.procedure"),
                    BossEvent.BossBarColor.GREEN, BossEvent.BossBarOverlay.NOTCHED_10);
        }
        return bar;
    }

    /** The captive on the table under the rig, if one's still there and still held. */
    public @Nullable Mob patient(ServerLevel level, BlockPos pos) {
        AABB under = new AABB(pos).inflate(1.5, 0, 1.5).expandTowards(0, -TABLE_REACH, 0);
        return level.getEntitiesOfClass(Mob.class, under, m -> m.isAlive() && Captives.onTable(m)).stream()
                .min(Comparator.comparingDouble(m -> m.distanceToSqr(net.minecraft.world.phys.Vec3.atCenterOf(pos)))).orElse(null);
    }

    /** Whether a rig over this captive is operating on them now (they can't be freed off the table till it stops). */
    public static boolean operatingOn(ServerLevel level, Mob captive) {
        for (BlockPos at : BlockPos.betweenClosed(captive.blockPosition().offset(-1, 0, -1), captive.blockPosition().offset(1, TABLE_REACH + 1, 1))) {
            if (level.getBlockEntity(at) instanceof ImplantRigBlockEntity rig && rig.stage == Stage.RUNNING) {
                return true;
            }
        }
        return false;
    }

    private static @Nullable ServerPlayer spotter(ServerLevel level, BlockPos pos) {
        return players(level, pos, SPOT_RANGE).stream().filter(p -> !p.isCreative()).findFirst().orElse(null);
    }

    private static List<ServerPlayer> players(ServerLevel level, BlockPos pos, double range) {
        return level.getPlayers(p -> !p.isSpectator() && p.distanceToSqr(net.minecraft.world.phys.Vec3.atCenterOf(pos)) <= range * range);
    }

    // Broken: a procedure under way stops (not on chunk unload, which only takes the bar down)
    @Override
    public void preRemoveSideEffects(BlockPos pos, BlockState state) {
        if (stage == Stage.RUNNING && level instanceof ServerLevel server) {
            stop(server, pos);
        }
        super.preRemoveSideEffects(pos, state);
    }

    @Override
    public void setRemoved() {
        super.setRemoved();
        if (bar != null) {
            bar.removeAllPlayers();
            bar = null;
        }
    }

    @Override
    protected void saveAdditional(ValueOutput output) {
        super.saveAdditional(output);
        output.putString("stage", stage.name());
        output.putInt("progress", progress);
        output.putBoolean("cleared", cleared);
    }

    @Override
    protected void loadAdditional(ValueInput input) {
        super.loadAdditional(input);
        stage = input.getString("stage").map(Stage::valueOf).orElse(Stage.WAITING);
        progress = input.getIntOr("progress", 0);
        cleared = input.getBooleanOr("cleared", false);
    }
}
