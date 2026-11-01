package com.hollowfrontier.content.concavia;

import com.hollowfrontier.HollowFrontier;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.IntegerArgumentType;

import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.RegisterCommandsEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;

/**
 * Concavia's game events (12a): a grudge the Kindled hold easing with time, and {@code /hollowfrontier kindled get|set <n>}
 * (ops) to read or set the runner's {@link KindledStanding} favour (setting it past the ally's line gives the sigil, as
 * trading there does).
 */
@EventBusSubscriber(modid = HollowFrontier.MODID)
public final class ConcaviaEvents {
    private ConcaviaEvents() {}

    @SubscribeEvent
    static void onPlayerTick(PlayerTickEvent.Post event) {
        if (event.getEntity() instanceof ServerPlayer player && player.tickCount % 20 == 0) {
            KindledStanding.tick(player);
        }
    }

    @SubscribeEvent
    static void onRegisterCommands(RegisterCommandsEvent event) {
        register(event.getDispatcher());
    }

    static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal("hollowfrontier")
                .requires(Commands.hasPermission(Commands.LEVEL_GAMEMASTERS))
                .then(Commands.literal("kindled")
                        .then(Commands.literal("get").executes(ctx -> {
                            ServerPlayer player = ctx.getSource().getPlayerOrException();
                            ctx.getSource().sendSuccess(() -> Component.translatable("commands.hollowfrontier.kindled.get", KindledStanding.favour(player)), false);
                            return KindledStanding.favour(player);
                        }))
                        .then(Commands.literal("set").then(Commands.argument("favour", IntegerArgumentType.integer(KindledStanding.MIN, KindledStanding.MAX))
                                .executes(ctx -> {
                                    ServerPlayer player = ctx.getSource().getPlayerOrException();
                                    int favour = IntegerArgumentType.getInteger(ctx, "favour");
                                    KindledStanding.add(player, favour - KindledStanding.favour(player));
                                    ctx.getSource().sendSuccess(() -> Component.translatable("commands.hollowfrontier.kindled.get", KindledStanding.favour(player)), true);
                                    return favour;
                                })))));
    }
}
