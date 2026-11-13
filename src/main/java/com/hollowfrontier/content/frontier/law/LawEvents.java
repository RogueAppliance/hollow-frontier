package com.hollowfrontier.content.frontier.law;

import com.hollowfrontier.HollowFrontier;
import com.hollowfrontier.content.frontier.outlaw.AbstractOutlaw;
import com.hollowfrontier.content.frontier.raid.TownRaids;
import com.hollowfrontier.content.frontier.townsfolk.Lawman;
import com.hollowfrontier.content.frontier.townsfolk.Townsfolk;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.LivingDamageEvent;
import net.neoforged.neoforge.event.entity.living.LivingDeathEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;

/** Turns what players do in town into heat (see {@link Wanted}) and ticks it down. */
@EventBusSubscriber(modid = HollowFrontier.MODID)
public final class LawEvents {
    private LawEvents() {}

    // Hurting a townsfolk or lawman (directly, with a bullet, or with dynamite: the source entity is the player).
    // While a gang is raiding the town, a stray shot into the crowd is forgiven: defending a town shouldn't make you
    // wanted. Killing someone still counts.
    @SubscribeEvent
    static void onDamage(LivingDamageEvent.Post event) {
        if (event.getInflictedDamage() <= 0 || !(event.getSource().getEntity() instanceof ServerPlayer player) || event.getEntity().isDeadOrDying()) {
            return;
        }
        int heat = crimeHeat(event.getEntity(), false);
        if (heat > 0 && TownRaids.raidAt(player.level(), event.getEntity().blockPosition()) == null) {
            Wanted.addHeat(player, heat);
            thoughtLessOf(player, event.getEntity(), heat, false);
        }
    }

    @SubscribeEvent
    static void onDeath(LivingDeathEvent event) {
        if (!(event.getSource().getEntity() instanceof ServerPlayer player)) {
            return;
        }
        LivingEntity victim = event.getEntity();
        int heat = crimeHeat(victim, true);
        if (heat > 0) {
            Wanted.addHeat(player, heat);
            thoughtLessOf(player, victim, heat, victim instanceof Townsfolk || victim instanceof Lawman);
        } else if (victim instanceof AbstractOutlaw outlaw && !outlaw.isJailed()) {
            Wanted.addHeat(player, -1); // a good deed
        }
    }

    // 47a: a crime inside a town costs the player their standing there (half of it for killing one of the town's own)
    private static void thoughtLessOf(ServerPlayer player, LivingEntity victim, int heat, boolean killedOneOfTheirs) {
        com.hollowfrontier.content.frontier.town.Towns.keyAt(player.level(), victim.blockPosition()).ifPresent(town -> {
            if (killedOneOfTheirs) {
                com.hollowfrontier.content.frontier.stake.Standing.halve(player, town);
            }
            com.hollowfrontier.content.frontier.stake.Standing.add(player, town, -heat * com.hollowfrontier.content.frontier.stake.Standing.PER_HEAT);
        });
    }

    /** Heat for hurting or killing this entity; 0 if it's no crime. */
    public static int crimeHeat(LivingEntity victim, boolean killed) {
        if (victim instanceof Lawman) {
            return killed ? Wanted.KILL_LAWMAN : Wanted.HURT_LAWMAN;
        }
        if (victim instanceof Townsfolk) {
            return killed ? Wanted.KILL_TOWNSFOLK : Wanted.HURT_TOWNSFOLK;
        }
        if (victim instanceof AbstractOutlaw outlaw && outlaw.isJailed() && killed) {
            return Wanted.KILL_PRISONER;
        }
        return 0;
    }

    @SubscribeEvent
    static void onPlayerTick(PlayerTickEvent.Post event) {
        if (event.getEntity() instanceof ServerPlayer player && player.tickCount % 20 == 0) {
            Wanted.tick(player);
        }
    }
}
