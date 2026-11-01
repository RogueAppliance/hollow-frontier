package com.hollowfrontier.content.frontier.saloon;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import com.hollowfrontier.HollowFrontier;
import com.hollowfrontier.content.frontier.raid.TownSite;
import com.hollowfrontier.registry.ModMobEffects;

import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.LayeredCauldronBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;

/**
 * A hot bath (24b). Sit in a tub of water (a water cauldron) in a town for {@link #SOAK} seconds and you climb out sober
 * and fresh: Tipsy and Nausea gone, a little Regeneration, and the tub a little lower. The bathhouse has two tubs, but
 * a horse trough does it too, for the brave. How long a player has been in is kept here only (a bath is short).
 */
@EventBusSubscriber(modid = HollowFrontier.MODID)
public final class Baths {
    private Baths() {}

    /** Seconds in the tub for it to take. */
    public static final int SOAK = 3;
    public static final int REGENERATION_TICKS = 200;
    private static final Map<UUID, Integer> SOAKING = new HashMap<>();

    @SubscribeEvent
    static void onPlayerTick(PlayerTickEvent.Post event) {
        if (!(event.getEntity() instanceof ServerPlayer player) || player.tickCount % 20 != 0) {
            return;
        }
        if (!inTub(player)) {
            SOAKING.remove(player.getUUID());
            return;
        }
        int seconds = SOAKING.merge(player.getUUID(), 1, Integer::sum);
        if (seconds >= SOAK) {
            SOAKING.remove(player.getUUID());
            bathe(player);
        }
    }

    /** In a water cauldron, in a town. */
    public static boolean inTub(ServerPlayer player) {
        BlockState state = player.level().getBlockState(player.blockPosition());
        return state.is(Blocks.WATER_CAULDRON) && player.level().dimension() == Level.OVERWORLD
                && TownSite.townAt((ServerLevel) player.level(), player.blockPosition()).isPresent();
    }

    /** The bath takes: sober, fresh, and the tub lower. */
    public static void bathe(ServerPlayer player) {
        boolean wasTipsy = player.hasEffect(ModMobEffects.TIPSY);
        player.removeEffect(ModMobEffects.TIPSY);
        player.removeEffect(MobEffects.NAUSEA);
        player.addEffect(new MobEffectInstance(MobEffects.REGENERATION, REGENERATION_TICKS, 0));
        player.clearFire();
        BlockPos tub = player.blockPosition();
        BlockState state = player.level().getBlockState(tub);
        if (state.is(Blocks.WATER_CAULDRON)) {
            LayeredCauldronBlock.lowerFillLevel(state, player.level(), tub);
        }
        player.level().playSound(null, tub, SoundEvents.PLAYER_SPLASH, SoundSource.PLAYERS, 0.6F, 1.1F);
        player.sendSystemMessage(Component.translatable(wasTipsy ? "message.hollowfrontier.bath.sober" : "message.hollowfrontier.bath.fresh"), true);
    }
}
