package com.hollowfrontier.content.frontier.mystery;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import com.hollowfrontier.HollowFrontier;
import com.hollowfrontier.content.hollow.creature.GrayProbe;
import com.hollowfrontier.registry.ModCriteria;
import com.hollowfrontier.registry.ModItems;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.context.CommandContext;

import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.animal.cow.Cow;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.RegisterCommandsEvent;
import net.neoforged.neoforge.event.entity.living.LivingDropsEvent;
import net.neoforged.neoforge.event.level.SleepFinishedTimeEvent;
import net.neoforged.neoforge.event.tick.LevelTickEvent;

/**
 * Hooks the frontier's mysteries (11a) into the game: the nightly roll (Overworld), the morning after a night slept
 * through, a probe brought down mid-job, and {@code /hollowfrontier mystery} (ops) to make one happen where the command
 * runs: {@code sighting}, {@code cattle} or {@code circle} (done at once, to the nearest steer or ripe field), or
 * {@code visit cattle|circle} (a probe sent to do it); {@code sighting hover} brings one over to hang there, as the skull
 * does; {@code visitors} brings two men in black (11b). {@code /hollowfrontier suspicion get|set <n>} reads or sets the
 * runner's {@link Suspicion} (set works in survival only, as suspicion does).
 */
@EventBusSubscriber(modid = HollowFrontier.MODID)
public final class MysteryEvents {
    private MysteryEvents() {}

    /** What a probe brought down mid-job gives up. */
    public static final int CAUGHT_FRAGMENTS = 2;

    @SubscribeEvent
    static void onLevelTick(LevelTickEvent.Post event) {
        if (!(event.getLevel() instanceof ServerLevel level) || level.dimension() != Level.OVERWORLD
                || level.getGameTime() % Mysteries.ROLL_INTERVAL != 0 || !level.isDarkOutside()) {
            return;
        }
        MysteryLog log = MysteryLog.get(level);
        if (!MysteryConfig.enabled() || log.happenedTonight(level.getGameTime()) || level.getRandom().nextDouble() >= MysteryConfig.chance()) {
            return;
        }
        List<ServerPlayer> players = level.players().stream().filter(p -> !p.isSpectator()).toList();
        if (!players.isEmpty()) {
            ServerPlayer player = players.get(level.getRandom().nextInt(players.size()));
            MysteryLog.Kind kind = Mysteries.pick(level.getRandom());
            if (!MysteryConfig.allows(kind)) {
                kind = MysteryLog.Kind.SIGHTING;
            }
            if (!Mysteries.happen(level, player, kind, level.getRandom(), !player.isSleeping()) && kind != MysteryLog.Kind.SIGHTING) {
                Mysteries.happen(level, player, MysteryLog.Kind.SIGHTING, level.getRandom(), true);
            }
        }
    }

    /** A night slept through may turn up a cut steer or a circle in the wheat by morning. */
    @SubscribeEvent
    static void onSleptThrough(SleepFinishedTimeEvent event) {
        if (!(event.getLevel() instanceof ServerLevel level) || level.dimension() != Level.OVERWORLD) {
            return;
        }
        if (!MysteryConfig.enabled() || MysteryLog.get(level).happenedTonight(level.getGameTime())
                || level.getRandom().nextDouble() >= MysteryConfig.overnightChance()) {
            return;
        }
        List<MysteryLog.Kind> kinds = new ArrayList<>(List.of(MysteryLog.Kind.MUTILATION, MysteryLog.Kind.CROP_CIRCLE));
        kinds.removeIf(kind -> !MysteryConfig.allows(kind));
        List<ServerPlayer> players = level.players().stream().filter(p -> !p.isSpectator()).toList();
        if (!players.isEmpty() && !kinds.isEmpty()) {
            ServerPlayer player = players.get(level.getRandom().nextInt(players.size()));
            if (level.getRandom().nextBoolean()) {
                Collections.reverse(kinds);
            }
            for (MysteryLog.Kind kind : kinds) {
                if (Mysteries.happen(level, player, kind, level.getRandom(), false)) {
                    break;
                }
            }
        }
    }

    /** Caught in the act: a probe brought down at work drops fragments of itself. */
    @SubscribeEvent
    static void onDrops(LivingDropsEvent event) {
        if (event.getEntity() instanceof GrayProbe probe && probe.hasJob() && probe.level() instanceof ServerLevel level) {
            event.getDrops().add(new ItemEntity(level, probe.getX(), probe.getY(), probe.getZ(), new ItemStack(ModItems.GRAY_TECH_FRAGMENT.get(), CAUGHT_FRAGMENTS)));
            if (event.getSource().getEntity() instanceof Player killer) {
                ModCriteria.fire(killer, ModCriteria.CAUGHT_IN_THE_ACT);
            }
        }
    }

    @SubscribeEvent
    static void onRegisterCommands(RegisterCommandsEvent event) {
        register(event.getDispatcher());
    }

    static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal("hollowfrontier")
                .requires(Commands.hasPermission(Commands.LEVEL_GAMEMASTERS))
                .then(Commands.literal("mystery")
                        .then(Commands.literal("sighting").executes(ctx -> sighting(ctx, 0))
                                .then(Commands.literal("hover").executes(ctx -> sighting(ctx, StrangeSkullItem.HOVER_TICKS))))
                        .then(Commands.literal("cattle").executes(ctx -> cattle(ctx, false)))
                        .then(Commands.literal("circle").executes(ctx -> circle(ctx, false)))
                        .then(Commands.literal("visit")
                                .then(Commands.literal("cattle").executes(ctx -> cattle(ctx, true)))
                                .then(Commands.literal("circle").executes(ctx -> circle(ctx, true))))
                        .then(Commands.literal("visitors").executes(ctx -> {
                            MenInBlack.arrive(ctx.getSource().getLevel(), BlockPos.containing(ctx.getSource().getPosition()), 2, null);
                            return done(ctx, "visitors");
                        })))
                .then(Commands.literal("suspicion")
                        .then(Commands.literal("get").executes(ctx -> {
                            ServerPlayer player = ctx.getSource().getPlayerOrException();
                            ctx.getSource().sendSuccess(() -> Component.translatable("commands.hollowfrontier.suspicion.get", Suspicion.level(player)), false);
                            return Suspicion.level(player);
                        }))
                        .then(Commands.literal("set").then(Commands.argument("level", IntegerArgumentType.integer(0, Suspicion.MAX)).executes(ctx -> {
                            ServerPlayer player = ctx.getSource().getPlayerOrException();
                            int level = IntegerArgumentType.getInteger(ctx, "level");
                            Suspicion.add(player, level - Suspicion.level(player));
                            ctx.getSource().sendSuccess(() -> Component.translatable("commands.hollowfrontier.suspicion.get", Suspicion.level(player)), true);
                            return level;
                        })))));
    }

    private static int sighting(CommandContext<CommandSourceStack> ctx, int hover) {
        Mysteries.sighting(ctx.getSource().getLevel(), ctx.getSource().getPosition(), ctx.getSource().getLevel().getRandom(), hover);
        return done(ctx, "sighting");
    }

    private static int cattle(CommandContext<CommandSourceStack> ctx, boolean visit) {
        ServerLevel level = ctx.getSource().getLevel();
        Optional<Cow> cow = Mysteries.findCattle(level, BlockPos.containing(ctx.getSource().getPosition()), 0, Mysteries.SEARCH);
        if (cow.isEmpty()) {
            ctx.getSource().sendFailure(Component.translatable("commands.hollowfrontier.mystery.no_cattle"));
            return 0;
        }
        if (visit) {
            Mysteries.visit(level, MysteryLog.Kind.MUTILATION, cow.get().blockPosition(), cow.get());
        } else {
            Mysteries.mutilate(level, cow.get());
        }
        return done(ctx, visit ? "visit" : "cattle");
    }

    private static int circle(CommandContext<CommandSourceStack> ctx, boolean visit) {
        ServerLevel level = ctx.getSource().getLevel();
        Optional<BlockPos> field = Mysteries.findWheat(level, BlockPos.containing(ctx.getSource().getPosition()), 0, Mysteries.SEARCH);
        if (field.isEmpty()) {
            ctx.getSource().sendFailure(Component.translatable("commands.hollowfrontier.mystery.no_wheat"));
            return 0;
        }
        if (visit) {
            Mysteries.visit(level, MysteryLog.Kind.CROP_CIRCLE, field.get(), null);
        } else {
            Mysteries.cropCircle(level, field.get());
        }
        return done(ctx, visit ? "visit" : "circle");
    }

    private static int done(CommandContext<CommandSourceStack> ctx, String what) {
        ctx.getSource().sendSuccess(() -> Component.translatable("commands.hollowfrontier.mystery." + what), true);
        return 1;
    }
}
