package com.hollowfrontier.content.frontier.raid;

import java.util.Optional;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.context.CommandContext;

import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;

/**
 * /hollowfrontier raid start [size] | stop | status (ops). Works on the town the command runs in (where you stand).
 * start skips the night, the dice and the cooldown; stop calls a raid off (the gang just goes); status reports the
 * town's raid state.
 */
public final class RaidCommand {
    private RaidCommand() {}

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal("hollowfrontier")
                .requires(Commands.hasPermission(Commands.LEVEL_GAMEMASTERS))
                .then(Commands.literal("raid")
                        .then(Commands.literal("start")
                                .executes(ctx -> start(ctx, -1))
                                .then(Commands.argument("size", IntegerArgumentType.integer(2, 16))
                                        .executes(ctx -> start(ctx, IntegerArgumentType.getInteger(ctx, "size")))))
                        .then(Commands.literal("stop").executes(RaidCommand::stop))
                        .then(Commands.literal("status").executes(RaidCommand::status))));
    }

    private static Optional<TownSite> town(CommandContext<CommandSourceStack> ctx) {
        ServerLevel level = ctx.getSource().getLevel();
        Optional<TownSite> site = level.dimension() == ServerLevel.OVERWORLD
                ? TownSite.find(level, BlockPos.containing(ctx.getSource().getPosition()), level.getRandom()) : Optional.empty();
        if (site.isEmpty()) {
            ctx.getSource().sendFailure(Component.translatable("commands.hollowfrontier.raid.no_town"));
        }
        return site;
    }

    private static int start(CommandContext<CommandSourceStack> ctx, int size) {
        Optional<TownSite> site = town(ctx);
        if (site.isEmpty()) {
            return 0;
        }
        ServerLevel level = ctx.getSource().getLevel();
        int gang = size > 0 ? size : TownRaid.gangSizeFor(level, site.get());
        Optional<TownRaid> raid = TownRaids.get(level).start(level, site.get(), RaidConfig.timings(), gang, true);
        if (raid.isEmpty()) {
            ctx.getSource().sendFailure(Component.translatable("commands.hollowfrontier.raid.already"));
            return 0;
        }
        ctx.getSource().sendSuccess(() -> Component.translatable("commands.hollowfrontier.raid.started", raid.get().gangName(), gang,
                site.get().hasVault() ? Component.translatable(site.get().vault().size() > 1 ? "commands.hollowfrontier.raid.bank" : "commands.hollowfrontier.raid.strongbox")
                        : Component.translatable("commands.hollowfrontier.raid.no_vault")), true);
        return 1;
    }

    private static int stop(CommandContext<CommandSourceStack> ctx) {
        Optional<TownSite> site = town(ctx);
        if (site.isEmpty()) {
            return 0;
        }
        if (!TownRaids.get(ctx.getSource().getLevel()).stop(ctx.getSource().getLevel(), site.get().key())) {
            ctx.getSource().sendFailure(Component.translatable("commands.hollowfrontier.raid.none"));
            return 0;
        }
        ctx.getSource().sendSuccess(() -> Component.translatable("commands.hollowfrontier.raid.stopped"), true);
        return 1;
    }

    private static int status(CommandContext<CommandSourceStack> ctx) {
        Optional<TownSite> site = town(ctx);
        if (site.isEmpty()) {
            return 0;
        }
        ServerLevel level = ctx.getSource().getLevel();
        TownRaids data = TownRaids.get(level);
        long now = level.getGameTime();
        TownRecord record = data.record(site.get().key(), now);
        TownRaid raid = data.raidIn(site.get().key());
        CommandSourceStack source = ctx.getSource();
        source.sendSuccess(() -> Component.translatable("commands.hollowfrontier.raid.status.town", site.get().size(),
                Math.max(0, (record.nextRaid - now) / 20), record.robbed(now) ? (record.robbedUntil - now) / 20 : 0,
                record.poster.map(p -> Component.literal(p.target())).orElse(Component.translatable("commands.hollowfrontier.raid.status.no_poster"))), false);
        if (raid == null) {
            source.sendSuccess(() -> Component.translatable("commands.hollowfrontier.raid.status.quiet"), false);
        } else {
            long alive = raid.members().stream().map(level::getEntity).filter(e -> e != null && e.isAlive()).count();
            Entity leader = raid.leaderId() == null ? null : level.getEntity(raid.leaderId());
            source.sendSuccess(() -> Component.translatable("commands.hollowfrontier.raid.status.raid", raid.gangName(), raid.leaderName(),
                    raid.phase().getSerializedName(), alive, raid.gangSize(), raid.vaultProgress(), raid.timings().vault(),
                    leader != null && leader.isAlive() ? leader.blockPosition().toShortString() : "-"), false);
        }
        return 1;
    }
}
