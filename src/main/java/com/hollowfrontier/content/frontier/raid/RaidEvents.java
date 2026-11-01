package com.hollowfrontier.content.frontier.raid;

import com.hollowfrontier.HollowFrontier;
import com.hollowfrontier.content.frontier.outlaw.AbstractOutlaw;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.Level;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.RegisterCommandsEvent;
import net.neoforged.neoforge.event.entity.EntityJoinLevelEvent;
import net.neoforged.neoforge.event.entity.living.LivingDamageEvent;
import net.neoforged.neoforge.event.entity.living.LivingDeathEvent;
import net.neoforged.neoforge.event.tick.LevelTickEvent;

/** Hooks raids into the game: ticking and rolling (Overworld), raiders joining the world, their deaths, the command. */
@EventBusSubscriber(modid = HollowFrontier.MODID)
public final class RaidEvents {
    private RaidEvents() {}

    @SubscribeEvent
    static void onLevelTick(LevelTickEvent.Post event) {
        if (!(event.getLevel() instanceof ServerLevel level) || level.dimension() != Level.OVERWORLD) {
            return;
        }
        TownRaids data = TownRaids.get(level);
        data.tick(level);
        if (level.getGameTime() % RaidConfig.checkInterval() == 0) {
            data.rollForRaids(level);
        }
    }

    // A raider whose raid is over (the server stopped mid-raid, or its chunk unloaded after the gang was called off)
    // never comes back; live ones get their raid goals.
    @SubscribeEvent
    static void onJoin(EntityJoinLevelEvent event) {
        if (!(event.getLevel() instanceof ServerLevel level)) {
            return;
        }
        RaidMember member = RaidMember.of(event.getEntity());
        if (member == null) {
            return;
        }
        if (level.dimension() != Level.OVERWORLD || TownRaids.get(level).byId(member.raid()) == null) {
            event.setCanceled(true);
            return;
        }
        if (event.getEntity() instanceof AbstractOutlaw outlaw) {
            RaidGoals.equip(outlaw);
        }
    }

    @SubscribeEvent
    static void onDeath(LivingDeathEvent event) {
        RaidMember member = RaidMember.of(event.getEntity());
        if (member != null && event.getEntity().level() instanceof ServerLevel level) {
            TownRaids data = TownRaids.get(level);
            TownRaid raid = data.byId(member.raid());
            if (raid != null) {
                raid.onMemberDeath(level, data, event.getEntity(), member, event.getSource());
            }
        }
    }

    @SubscribeEvent
    static void onDamage(LivingDamageEvent.Post event) {
        RaidMember member = RaidMember.of(event.getEntity());
        if (member != null && member.role() != RaidMember.Role.HORSE && event.getSource().getEntity() instanceof ServerPlayer player
                && event.getEntity().level() instanceof ServerLevel level) {
            TownRaid raid = TownRaids.get(level).byId(member.raid());
            if (raid != null) {
                raid.onMemberHurt(player, event.getInflictedDamage());
                TownRaids.get(level).setDirty();
            }
        }
    }

    @SubscribeEvent
    static void onRegisterCommands(RegisterCommandsEvent event) {
        RaidCommand.register(event.getDispatcher());
    }
}
