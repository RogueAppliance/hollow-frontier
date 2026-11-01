package com.hollowfrontier.content.ufo;

import com.hollowfrontier.HollowFrontier;
import com.hollowfrontier.content.frontier.mystery.ManInBlack;
import com.hollowfrontier.content.frontier.mystery.Suspicion;
import com.hollowfrontier.content.frontier.outlaw.AbstractOutlaw;
import com.hollowfrontier.content.frontier.talk.Chatter;
import com.hollowfrontier.content.frontier.talk.Moment;
import com.hollowfrontier.registry.ModCriteria;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import net.minecraft.ChatFormatting;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ClientboundSetSubtitleTextPacket;
import net.minecraft.network.protocol.game.ClientboundSetTitleTextPacket;
import net.minecraft.network.protocol.game.ClientboundSetTitlesAnimationPacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.level.saveddata.SavedDataType;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.entity.EntitySpawnReason;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.FinalizeSpawnEvent;

/**
 * The story's end (13d): the Grays' leash on the men they implanted. The Mothership is what holds it; the first time one
 * falls in a world ({@link #breakIt}), it breaks, for good (saved with the Overworld):
 * <ul>
 * <li>Everyone online sees "The leash is broken" and is told what it means; every one of them earns Cut Loose.</li>
 * <li><b>Freed</b> ({@link #free}): within {@link #FREE_RANGE} of the wreck, every outlaw throws down his gun and walks
 *     off, harmless ({@link AbstractOutlaw#setFreed}), and every man in black comes to himself and is gone.</li>
 * <li><b>After:</b> the men in black stop coming (no visits, no hunts), nobody's suspicion rises again and everyone's is
 *     wiped; the Grays stop abducting (their saucers still cross the sky now and then); and with no more drifters being
 *     made, only {@link #OUTLAW_SPAWNS_KEPT} of the outlaws that would spawn in the wild still do. The ones who ride now
 *     ride by their own choice: raids and the jail go on as before.</li>
 * </ul>
 * Later Motherships can still be called and fought; the leash only breaks once. Each one that falls is counted
 * ({@link #fallen}, 15b), and each rematch comes a step harder ({@link #rematch}), up to {@link #MAX_REMATCH}.
 */
@EventBusSubscriber(modid = HollowFrontier.MODID)
public class Leash extends SavedData {
    public static final double FREE_RANGE = 256.0;
    public static final float OUTLAW_SPAWNS_KEPT = 1.0F / 3.0F;
    public static final int MAX_REMATCH = 4;

    public static final Codec<Leash> CODEC = RecordCodecBuilder.create(i -> i.group(
            Codec.BOOL.optionalFieldOf("broken", false).forGetter(leash -> leash.broken),
            Codec.LONG.optionalFieldOf("when", 0L).forGetter(leash -> leash.when),
            Codec.INT.optionalFieldOf("motherships_fallen", 0).forGetter(leash -> leash.fallen)).apply(i, Leash::new));
    public static final SavedDataType<Leash> TYPE = new SavedDataType<>(HollowFrontier.id("the_leash"), Leash::new, CODEC);

    private boolean broken;
    private long when;
    private int fallen;

    public Leash() {}

    private Leash(boolean broken, long when, int fallen) {
        this.broken = broken;
        this.when = when;
        this.fallen = fallen;
    }

    public static Leash get(ServerLevel level) {
        return level.getServer().overworld().getDataStorage().computeIfAbsent(TYPE);
    }

    public static boolean isBroken(Level level) {
        return level instanceof ServerLevel server && get(server).broken;
    }

    /** For tests and the command: sets it broken or whole, with none of the ending's effects. */
    public static void set(ServerLevel level, boolean broken) {
        Leash leash = get(level);
        leash.broken = broken;
        leash.when = broken ? level.getGameTime() : 0;
        leash.setDirty();
    }

    /** How many Motherships have fallen in this world. A world whose leash broke before the count was kept reads as one. */
    public static int fallen(ServerLevel level) {
        Leash leash = get(level);
        return Math.max(leash.fallen, leash.broken ? 1 : 0);
    }

    /** How much harder the next Mothership comes: 0 for the first, then a step a rematch, up to {@link #MAX_REMATCH}. */
    public static int rematch(ServerLevel level) {
        return Math.min(fallen(level), MAX_REMATCH);
    }

    /** Another Mothership down: how many have fallen now. */
    public static int countFallen(ServerLevel level) {
        return setFallen(level, fallen(level) + 1);
    }

    /** For tests and the command: sets how many Motherships have fallen. */
    public static int setFallen(ServerLevel level, int count) {
        Leash leash = get(level);
        leash.fallen = Math.max(0, count);
        leash.setDirty();
        return leash.fallen;
    }

    /**
     * The Mothership has fallen at {@code where}: break the leash, if it isn't already. Whether this was the time it broke.
     */
    public static boolean breakIt(ServerLevel level, Vec3 where) {
        if (get(level).broken) {
            return false;
        }
        set(level, true);
        free(level, where, FREE_RANGE);
        for (ServerPlayer player : level.getServer().getPlayerList().getPlayers()) {
            player.connection.send(new ClientboundSetTitlesAnimationPacket(20, 100, 40));
            player.connection.send(new ClientboundSetTitleTextPacket(Component.translatable("title.hollowfrontier.leash_broken").withStyle(ChatFormatting.AQUA)));
            player.connection.send(new ClientboundSetSubtitleTextPacket(Component.translatable("title.hollowfrontier.leash_broken.subtitle")));
            for (int line = 1; line <= 3; line++) {
                player.sendSystemMessage(Component.translatable("message.hollowfrontier.leash_broken." + line).withStyle(ChatFormatting.GRAY, ChatFormatting.ITALIC));
            }
            player.level().playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.BEACON_DEACTIVATE, SoundSource.MASTER, 1.0F, 0.5F);
            Suspicion.set(player, Suspicion.NONE, Suspicion.level(player));
            ModCriteria.fire(player, ModCriteria.LEASH_BROKEN);
        }
        return true;
    }

    /** Frees every implanted man within {@code range} of {@code where}; how many. */
    public static int free(ServerLevel level, Vec3 where, double range) {
        int freed = 0;
        AABB around = new AABB(where, where).inflate(range);
        for (AbstractOutlaw outlaw : level.getEntitiesOfClass(AbstractOutlaw.class, around, o -> o.isAlive() && !o.isJailed() && !o.isFreed())) {
            outlaw.setFreed();
            freed++;
        }
        for (ManInBlack man : level.getEntitiesOfClass(ManInBlack.class, around, Entity::isAlive)) {
            if (level.getNearestPlayer(man, 24.0) instanceof ServerPlayer near) {
                Chatter.say(man, near, Moment.FREED);
            }
            level.sendParticles(ParticleTypes.SMOKE, man.getX(), man.getY(1.0), man.getZ(), 8, 0.2, 0.4, 0.2, 0.01);
            man.discard();
            freed++;
        }
        return freed;
    }

    /** With the leash broken, the Grays make no more drifters into outlaws: most of the wild spawns don't happen. */
    @SubscribeEvent
    static void onFinalizeSpawn(FinalizeSpawnEvent event) {
        if (event.getEntity() instanceof AbstractOutlaw && event.getSpawnType() == EntitySpawnReason.NATURAL
                && isBroken(event.getLevel().getLevel()) && event.getLevel().getRandom().nextFloat() >= OUTLAW_SPAWNS_KEPT) {
            event.setSpawnCancelled(true);
        }
    }
}
