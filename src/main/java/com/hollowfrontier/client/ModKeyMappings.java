package com.hollowfrontier.client;

import java.util.Optional;

import com.hollowfrontier.HollowFrontier;
import com.hollowfrontier.content.frontier.gun.GunItem;
import com.hollowfrontier.network.ReloadGunPayload;
import com.hollowfrontier.network.SaucerControlPayload;
import com.hollowfrontier.network.SaucerHoldPayload;
import com.hollowfrontier.content.ufo.PrototypeSaucer;
import com.mojang.blaze3d.platform.InputConstants;

import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.RegisterKeyMappingsEvent;
import net.neoforged.neoforge.client.network.ClientPacketDistributor;
import net.neoforged.neoforge.client.settings.KeyConflictContext;

@EventBusSubscriber(modid = HollowFrontier.MODID, value = Dist.CLIENT)
public final class ModKeyMappings {
    private ModKeyMappings() {}

    public static final KeyMapping.Category CATEGORY = new KeyMapping.Category(HollowFrontier.id("main"));

    public static final KeyMapping RELOAD = new KeyMapping("key.hollowfrontier.reload",
            KeyConflictContext.IN_GAME, InputConstants.Type.KEYBOARD, InputConstants.KEY_R, CATEGORY);
    // 13c: a saucer pilot's heat laser and tractor beam, each held
    public static final KeyMapping SAUCER_LASER = new KeyMapping("key.hollowfrontier.saucer_laser",
            KeyConflictContext.IN_GAME, InputConstants.Type.KEYBOARD, InputConstants.KEY_V, CATEGORY);
    public static final KeyMapping SAUCER_BEAM = new KeyMapping("key.hollowfrontier.saucer_beam",
            KeyConflictContext.IN_GAME, InputConstants.Type.KEYBOARD, InputConstants.KEY_B, CATEGORY);
    // 15b: opens the cargo hold, aboard a saucer that has one
    public static final KeyMapping SAUCER_HOLD = new KeyMapping("key.hollowfrontier.saucer_hold",
            KeyConflictContext.IN_GAME, InputConstants.Type.KEYBOARD, InputConstants.KEY_H, CATEGORY);

    // What the server was last told of the saucer keys, so the payload goes only when they change
    private static boolean sentLaser, sentBeam;

    @SubscribeEvent
    static void registerKeys(RegisterKeyMappingsEvent event) {
        event.registerCategory(CATEGORY);
        event.register(RELOAD);
        event.register(SAUCER_LASER);
        event.register(SAUCER_BEAM);
        event.register(SAUCER_HOLD);
    }

    @SubscribeEvent
    static void onClientTick(ClientTickEvent.Post event) {
        Player player = Minecraft.getInstance().player;
        while (RELOAD.consumeClick()) {
            if (player != null && (player.getMainHandItem().getItem() instanceof GunItem || player.getOffhandItem().getItem() instanceof GunItem)) {
                ClientPacketDistributor.sendToServer(ReloadGunPayload.INSTANCE);
            }
        }
        while (SAUCER_HOLD.consumeClick()) {
            if (player != null && player.getVehicle() instanceof PrototypeSaucer) {
                ClientPacketDistributor.sendToServer(SaucerHoldPayload.INSTANCE);
            }
        }
        PrototypeSaucer piloted = player != null && player.getVehicle() instanceof PrototypeSaucer saucer && saucer.getFirstPassenger() == player
                ? saucer : null;
        boolean laser = piloted != null && SAUCER_LASER.isDown(), beam = piloted != null && SAUCER_BEAM.isDown();
        // While the laser's held, every tick, with where the sight points
        if (laser != sentLaser || beam != sentBeam || laser) {
            sentLaser = laser;
            sentBeam = beam;
            if (player != null) {
                Optional<Vec3> aim = laser ? Optional.of(SaucerAim.target(Minecraft.getInstance(), piloted)) : Optional.empty();
                ClientPacketDistributor.sendToServer(new SaucerControlPayload(laser, beam, aim));
            }
        }
    }
}
