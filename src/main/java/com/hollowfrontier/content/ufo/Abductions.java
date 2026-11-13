package com.hollowfrontier.content.ufo;

import java.util.List;

import com.hollowfrontier.HollowFrontier;
import com.hollowfrontier.content.frontier.mystery.MysteryLog;
import com.hollowfrontier.content.frontier.mystery.Suspicion;
import com.hollowfrontier.registry.ModCriteria;
import com.hollowfrontier.registry.ModEntities;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.IntegerArgumentType;

import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.RegisterCommandsEvent;
import net.neoforged.neoforge.event.tick.LevelTickEvent;

/**
 * The Grays' saucers over the Overworld (13a). At night, every {@link UfoConfig#rollInterval} ticks, a
 * {@link UfoConfig#chance} roll sends a {@link GraySaucer} over one of the players; {@link UfoConfig#abductionChance} of
 * them stop to beam something up: an animal near the player if there is one ({@link #ANIMAL_SEARCH}), otherwise the
 * player's own spot. Every one is logged as a sighting, so the men in black come round after (11b).
 * <p>
 * What's taken ({@link #take}): an animal is simply gone. A player comes to on the ground {@link #MISSING_MIN} to
 * {@link #MISSING_MAX} blocks away, blinded, sick and slow, with no memory of how they got there, and the men in black
 * mind them a little more (they've been looked at). Off the Overworld they're set down where they were taken.
 * <p>
 * {@code /hollowfrontier ufo flyover} sends one over the runner; {@code ufo abduction} sends one to stop and beam where the
 * runner stands (ops). {@code ufo rematch [fallen]} reads or sets how many Motherships have fallen (15b).
 */
@EventBusSubscriber(modid = HollowFrontier.MODID)
public final class Abductions {
    private Abductions() {}

    public static final double ANIMAL_SEARCH = 40.0;
    public static final int MISSING_MIN = 300, MISSING_MAX = 700;
    public static final int SUSPICION = 10;

    @SubscribeEvent
    static void onLevelTick(LevelTickEvent.Post event) {
        if (!(event.getLevel() instanceof ServerLevel level) || level.dimension() != Level.OVERWORLD || !UfoConfig.flyovers()
                || level.getGameTime() % UfoConfig.rollInterval() != 0 || !level.isDarkOutside()) {
            return;
        }
        RandomSource random = level.getRandom();
        List<ServerPlayer> players = level.players().stream().filter(p -> !p.isSpectator()).toList();
        if (players.isEmpty() || random.nextDouble() >= UfoConfig.chance()) {
            return;
        }
        ServerPlayer player = players.get(random.nextInt(players.size()));
        // 13d: with the leash broken they no longer take anyone, only cross the sky
        if (random.nextDouble() < UfoConfig.abductionChance() && !Leash.isBroken(level)) {
            Animal animal = level.getEntitiesOfClass(Animal.class, player.getBoundingBox().inflate(ANIMAL_SEARCH), a -> a.isAlive() && !a.isPassenger())
                    .stream().findAny().orElse(null);
            abduction(level, animal != null ? animal.position() : player.position(), random);
        } else {
            flyover(level, player.position(), random);
        }
    }

    /** A saucer across the sky over {@code over}, 40-60 up. */
    public static GraySaucer flyover(ServerLevel level, Vec3 over, RandomSource random) {
        double angle = random.nextDouble() * Math.PI * 2;
        Vec3 direction = new Vec3(Math.cos(angle), 0, Math.sin(angle));
        double height = Math.min(over.y + 40 + random.nextInt(20), level.getMaxY() - 8);
        Vec3 side = new Vec3(-direction.z, 0, direction.x).scale(random.nextInt(41) - 20);
        GraySaucer saucer = spawn(level, new Vec3(over.x, height, over.z).add(side).subtract(direction.scale(GraySaucer.HALF_RUN)));
        saucer.flyover(direction);
        MysteryLog.get(level).record(MysteryLog.Kind.SIGHTING, level.getGameTime(), BlockPos.containing(over));
        return saucer;
    }

    /** A saucer coming in to stop {@link GraySaucer#BEAM_HEIGHT} over {@code spot} and beam. */
    public static GraySaucer abduction(ServerLevel level, Vec3 spot, RandomSource random) {
        double angle = random.nextDouble() * Math.PI * 2;
        Vec3 direction = new Vec3(Math.cos(angle), 0, Math.sin(angle));
        double height = Math.min(spot.y + GraySaucer.BEAM_HEIGHT, level.getMaxY() - 8);
        GraySaucer saucer = spawn(level, new Vec3(spot.x, height, spot.z).subtract(direction.scale(GraySaucer.HALF_RUN)));
        saucer.abduction(direction, new Vec3(spot.x, height, spot.z));
        MysteryLog.get(level).record(MysteryLog.Kind.SIGHTING, level.getGameTime(), BlockPos.containing(spot));
        return saucer;
    }

    private static GraySaucer spawn(ServerLevel level, Vec3 at) {
        GraySaucer saucer = ModEntities.GRAY_SAUCER.get().create(level, EntitySpawnReason.EVENT);
        saucer.setPos(at);
        level.addFreshEntity(saucer);
        return saucer;
    }

    /** What a saucer's beam has drawn up into it: gone, if an animal; set down far away, if a player. */
    public static void take(ServerLevel level, GraySaucer saucer, LivingEntity taken) {
        level.playSound(null, saucer.getX(), saucer.getY(), saucer.getZ(), SoundEvents.ENDERMAN_TELEPORT, SoundSource.HOSTILE, 4.0F, 0.5F);
        if (taken instanceof ServerPlayer player) {
            // 50: one the Grays have reason to keep wakes aboard; anyone else loses an hour, as before
            if (!com.hollowfrontier.content.ufo.taken.Taken.keeps(level, player) || com.hollowfrontier.content.ufo.taken.Taken.take(level, player) == null) {
                missingTime(level, player);
            }
        } else {
            taken.discard();
        }
    }

    /** A taken player comes to somewhere else, with hours they can't account for. */
    public static void missingTime(ServerLevel level, ServerPlayer player) {
        player.removeEffect(MobEffects.LEVITATION);
        player.resetFallDistance();
        if (level.dimension() == Level.OVERWORLD) {
            RandomSource random = level.getRandom();
            double angle = random.nextDouble() * Math.PI * 2, distance = MISSING_MIN + random.nextInt(MISSING_MAX - MISSING_MIN + 1);
            int x = (int) (player.getX() + Math.cos(angle) * distance), z = (int) (player.getZ() + Math.sin(angle) * distance);
            level.getChunk(x >> 4, z >> 4); // generated, so the heightmap's true
            int y = level.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, x, z);
            player.teleportTo(x + 0.5, y, z + 0.5);
        }
        player.addEffect(new MobEffectInstance(MobEffects.BLINDNESS, 100, 0));
        player.addEffect(new MobEffectInstance(MobEffects.NAUSEA, 200, 0));
        player.addEffect(new MobEffectInstance(MobEffects.SLOWNESS, 300, 1));
        player.sendSystemMessage(Component.translatable("message.hollowfrontier.abduction.missing_time").withStyle(ChatFormatting.GRAY, ChatFormatting.ITALIC));
        Suspicion.add(player, SUSPICION);
        ModCriteria.fire(player, ModCriteria.ABDUCTED);
    }

    @SubscribeEvent
    static void onRegisterCommands(RegisterCommandsEvent event) {
        register(event.getDispatcher());
    }

    static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal("hollowfrontier")
                .requires(Commands.hasPermission(Commands.LEVEL_GAMEMASTERS))
                .then(Commands.literal("ufo")
                        .then(Commands.literal("flyover").executes(ctx -> {
                            flyover(ctx.getSource().getLevel(), ctx.getSource().getPosition(), ctx.getSource().getLevel().getRandom());
                            ctx.getSource().sendSuccess(() -> Component.translatable("commands.hollowfrontier.ufo.flyover"), true);
                            return 1;
                        }))
                        .then(Commands.literal("abduction").executes(ctx -> {
                            abduction(ctx.getSource().getLevel(), ctx.getSource().getPosition(), ctx.getSource().getLevel().getRandom());
                            ctx.getSource().sendSuccess(() -> Component.translatable("commands.hollowfrontier.ufo.abduction"), true);
                            return 1;
                        }))
                        // 50: the runner is kept, whatever the rules say; "leave" sets them down again
                        .then(Commands.literal("taken")
                                .executes(ctx -> com.hollowfrontier.content.ufo.taken.Taken.commandTake(ctx.getSource().getPlayerOrException()))
                                .then(Commands.literal("leave").executes(ctx -> com.hollowfrontier.content.ufo.taken.Taken.commandLeave(ctx.getSource().getPlayerOrException()))))
                        // 15a: a Gray Landing nearby (closer than one would come on its own, and whether or not the leash holds)
                        .then(Commands.literal("landing").executes(ctx -> {
                            ServerLevel level = ctx.getSource().getLevel();
                            var ground = GrayLandings.findClearing(level, BlockPos.containing(ctx.getSource().getPosition()), 16, 40, level.getRandom());
                            if (ground.isEmpty()) {
                                ctx.getSource().sendFailure(Component.translatable("commands.hollowfrontier.ufo.landing.no_room"));
                                return 0;
                            }
                            GrayLandings.landAt(level, ground.get(), GrayLandings.day(level));
                            ctx.getSource().sendSuccess(() -> Component.translatable("commands.hollowfrontier.ufo.landing"), true);
                            return 1;
                        }))
                        // 13d: a Mothership over the runner, as if a beacon there had called it; the leash, read or set
                        .then(Commands.literal("mothership").executes(ctx -> {
                            Mothership.summon(ctx.getSource().getLevel(), net.minecraft.core.BlockPos.containing(ctx.getSource().getPosition()));
                            ctx.getSource().sendSuccess(() -> Component.translatable("commands.hollowfrontier.ufo.mothership"), true);
                            return 1;
                        }))
                        .then(Commands.literal("leash")
                                .executes(ctx -> {
                                    boolean broken = Leash.isBroken(ctx.getSource().getLevel());
                                    ctx.getSource().sendSuccess(() -> Component.translatable("commands.hollowfrontier.ufo.leash." + (broken ? "broken" : "whole")), false);
                                    return broken ? 1 : 0;
                                })
                                .then(Commands.literal("break").executes(ctx -> {
                                    Leash.set(ctx.getSource().getLevel(), false);
                                    Leash.breakIt(ctx.getSource().getLevel(), ctx.getSource().getPosition());
                                    return 1;
                                }))
                                .then(Commands.literal("mend").executes(ctx -> {
                                    Leash.set(ctx.getSource().getLevel(), false);
                                    ctx.getSource().sendSuccess(() -> Component.translatable("commands.hollowfrontier.ufo.leash.whole"), true);
                                    return 1;
                                })))
                        // 15b: how many Motherships have fallen (what the next one's rematch is), read or set
                        .then(Commands.literal("rematch")
                                .executes(ctx -> {
                                    ServerLevel level = ctx.getSource().getLevel();
                                    int fallen = Leash.fallen(level);
                                    ctx.getSource().sendSuccess(() -> Component.translatable("commands.hollowfrontier.ufo.rematch", fallen, Leash.rematch(level)), false);
                                    return fallen;
                                })
                                .then(Commands.argument("fallen", IntegerArgumentType.integer(0)).executes(ctx -> {
                                    ServerLevel level = ctx.getSource().getLevel();
                                    int fallen = Leash.setFallen(level, IntegerArgumentType.getInteger(ctx, "fallen"));
                                    ctx.getSource().sendSuccess(() -> Component.translatable("commands.hollowfrontier.ufo.rematch", Leash.fallen(level), Leash.rematch(level)), true);
                                    return fallen;
                                })))));
    }
}
