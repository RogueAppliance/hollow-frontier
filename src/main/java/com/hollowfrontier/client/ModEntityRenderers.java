package com.hollowfrontier.client;

import java.util.Arrays;

import com.geckolib.model.DefaultedEntityGeoModel;
import com.geckolib.renderer.layer.builtin.AutoGlowingGeoLayer;
import com.hollowfrontier.HollowFrontier;
import com.hollowfrontier.content.frontier.mystery.AirshipSighting;
import com.hollowfrontier.content.frontier.mystery.CattleCarcass;
import com.hollowfrontier.content.frontier.townsfolk.Folk;
import com.hollowfrontier.content.frontier.townsfolk.Lawman;
import com.hollowfrontier.content.frontier.townsfolk.Townsfolk;
import com.hollowfrontier.content.frontier.wildlife.Rattlesnake;
import com.hollowfrontier.content.hollow.creature.AshSalamander;
import com.hollowfrontier.content.hollow.creature.FernSkitter;
import com.hollowfrontier.content.hollow.creature.GrayProbe;
import com.hollowfrontier.content.hollow.creature.PaleRaven;
import com.hollowfrontier.content.concavia.CrystalGuardian;
import com.hollowfrontier.content.concavia.SunArchon;
import com.hollowfrontier.content.ufo.GraySaucer;
import com.hollowfrontier.content.ufo.GrayDrone;
import com.hollowfrontier.content.ufo.GrayWarden;
import com.hollowfrontier.content.hollow.gray.GraySentinel;
import com.hollowfrontier.registry.ModBlockEntities;
import com.hollowfrontier.registry.ModEntities;
import com.hollowfrontier.registry.ModParticles;

import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.renderer.entity.MinecartRenderer;
import net.minecraft.client.renderer.entity.NoopRenderer;
import net.minecraft.client.renderer.entity.ThrownItemRenderer;
import net.minecraft.world.entity.EntityTypes;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;
import net.neoforged.neoforge.client.event.RegisterParticleProvidersEvent;

@EventBusSubscriber(modid = HollowFrontier.MODID, value = Dist.CLIENT)
public final class ModEntityRenderers {
    private ModEntityRenderers() {}

    // 29: the motes a dust devil is drawn with
    @SubscribeEvent
    static void registerParticles(RegisterParticleProvidersEvent event) {
        event.registerSpriteSet(ModParticles.DUST_DEVIL_MOTE.get(), DustDevilMote.Provider::new);
    }

    @SubscribeEvent
    static void registerRenderers(EntityRenderersEvent.RegisterRenderers event) {
        event.registerEntityRenderer(ModEntities.LASSO.get(), ThrownItemRenderer::new);
        event.registerEntityRenderer(ModEntities.DYNAMITE.get(), ThrownItemRenderer::new);
        event.registerEntityRenderer(ModEntities.SEAT.get(), NoopRenderer::new);
        // 29: a dust devil draws itself in particles
        event.registerEntityRenderer(ModEntities.DUST_DEVIL.get(), NoopRenderer::new);
        event.registerEntityRenderer(ModEntities.ASH_DEVIL.get(), NoopRenderer::new);
        // A plain minecart body; the engine is its display block (HandcarEngineBlock).
        event.registerEntityRenderer(ModEntities.STEAM_HANDCAR.get(), ctx -> new MinecartRenderer(ctx, ModelLayers.MINECART));
        event.registerEntityRenderer(ModEntities.AIRSHIP.get(), AirshipRenderer::new);
        event.registerBlockEntityRenderer(ModBlockEntities.POLAR_PORTAL.get(), PolarPortalRenderer::new);
        event.registerEntityRenderer(ModEntities.RATTLESNAKE.get(), ctx -> new GlowingGeoRenderer<>(ctx, new DefaultedEntityGeoModel<Rattlesnake>(HollowFrontier.id("rattlesnake"))));
        // The Hollow Earth's creatures (9b2). The hare's crystals and the salamander's spots glow (their *_glowmask textures);
        // the hare's renderer also draws the lab hare's ear tag (9b3).
        event.registerEntityRenderer(ModEntities.FERN_SKITTER.get(), ctx -> new GlowingGeoRenderer<>(ctx, new DefaultedEntityGeoModel<FernSkitter>(HollowFrontier.id("fern_skitter"))));
        event.registerEntityRenderer(ModEntities.GLASSBACK_HARE.get(), GlassbackHareRenderer::new);
        event.registerEntityRenderer(ModEntities.ASH_SALAMANDER.get(), ctx -> new GlowingGeoRenderer<>(ctx,
                new DefaultedEntityGeoModel<AshSalamander>(HollowFrontier.id("ash_salamander"))).withRenderLayer(AutoGlowingGeoLayer::new));
        event.registerEntityRenderer(ModEntities.PALE_RAVEN.get(), ctx -> new GlowingGeoRenderer<>(ctx, new DefaultedEntityGeoModel<PaleRaven>(HollowFrontier.id("pale_raven"))));
        // The Gray probe over the abandoned lab (9b3); its band glows
        event.registerEntityRenderer(ModEntities.GRAY_PROBE.get(), ctx -> new GlowingGeoRenderer<>(ctx,
                new DefaultedEntityGeoModel<GrayProbe>(HollowFrontier.id("gray_probe"))).withRenderLayer(AutoGlowingGeoLayer::new));
        // The wild megafauna (10a)
        event.registerEntityRenderer(ModEntities.WOOLLY_MAMMOTH.get(), ctx -> new MegafaunaRenderer<>(ctx, "woolly_mammoth", 1.6F));
        event.registerEntityRenderer(ModEntities.TERROR_BIRD.get(), ctx -> new MegafaunaRenderer<>(ctx, "terror_bird", 0.7F));
        event.registerEntityRenderer(ModEntities.MEGATHERIUM.get(), ctx -> new MegafaunaRenderer<>(ctx, "megatherium", 1.3F));
        event.registerEntityRenderer(ModEntities.GLYPTODON.get(), ctx -> new MegafaunaRenderer<>(ctx, "glyptodon", 1.2F));
        // The frontier's mysteries (11a): the sighting (its model a quarter size, drawn four times over; its lights glow), and the
        // carcass (its cuts glow faintly)
        event.registerEntityRenderer(ModEntities.AIRSHIP_SIGHTING.get(), ctx -> new GlowingGeoRenderer<>(ctx,
                new DefaultedEntityGeoModel<AirshipSighting>(HollowFrontier.id("airship_sighting"))).withScale(4.0F).withRenderLayer(AutoGlowingGeoLayer::new));
        event.registerEntityRenderer(ModEntities.CATTLE_CARCASS.get(), ctx -> new GlowingGeoRenderer<>(ctx,
                new DefaultedEntityGeoModel<CattleCarcass>(HollowFrontier.id("cattle_carcass"))).withRenderLayer(AutoGlowingGeoLayer::new));
        // The live station's sentinel (10b): the probe's model in its own skin, its lights red
        event.registerEntityRenderer(ModEntities.GRAY_SENTINEL.get(), ctx -> new GlowingGeoRenderer<>(ctx,
                new DefaultedEntityGeoModel<GraySentinel>(HollowFrontier.id("gray_probe")).withAltTexture(HollowFrontier.id("gray_sentinel")))
                .withRenderLayer(AutoGlowingGeoLayer::new));
        // The men in black (11b): the humanoid model, their own skin, the implant behind the ear glowing
        event.registerEntityRenderer(ModEntities.MAN_IN_BLACK.get(), ctx -> new HumanoidGeoRenderer<>(ctx, "man_in_black", 1.0F).withRenderLayer(AutoGlowingGeoLayer::new));
        // Concavia (12a): the Kindled, tall on the humanoid model with their eyes and circlets glowing, and the crystal guardian,
        // its eye, core and crystals glowing
        event.registerEntityRenderer(ModEntities.KINDLED.get(), ctx -> new HumanoidGeoRenderer<>(ctx, "kindled", 1.2F).withRenderLayer(AutoGlowingGeoLayer::new));
        event.registerEntityRenderer(ModEntities.CRYSTAL_GUARDIAN.get(), ctx -> new GlowingGeoRenderer<>(ctx,
                new DefaultedEntityGeoModel<CrystalGuardian>(HollowFrontier.id("crystal_guardian"))).withRenderLayer(AutoGlowingGeoLayer::new));
        // The Sun Archon (12b): its crown, halo, eyes and staff's orb glow
        event.registerEntityRenderer(ModEntities.SUN_ARCHON.get(), ctx -> new GlowingGeoRenderer<>(ctx,
                new DefaultedEntityGeoModel<SunArchon>(HollowFrontier.id("sun_archon"))).withRenderLayer(AutoGlowingGeoLayer::new));
        // The Grays (13a): the saucer, a quarter-size model drawn four times over (its rim lights and emitter glow), and the Warden
        // (its eyes' sheen and the pistol's muzzle glow)
        event.registerEntityRenderer(ModEntities.GRAY_SAUCER.get(), ctx -> new GlowingGeoRenderer<>(ctx,
                new DefaultedEntityGeoModel<GraySaucer>(HollowFrontier.id("gray_saucer"))).withScale(4.0F).withRenderLayer(AutoGlowingGeoLayer::new));
        event.registerEntityRenderer(ModEntities.PHLOGISTON_BEAM.get(), PhlogistonBeamRenderer::new); // 13b: the phlogiston guns' beam
        // 13c: the player's own saucer, under a glass dome
        event.registerEntityRenderer(ModEntities.PROTOTYPE_SAUCER.get(), SaucerRenderer::new);
        // 13d: the Mothership; its drones (the probe's model in a skin of their own, drawn a third larger, their lights violet);
        // the Phlogiston Beacon's beam
        event.registerEntityRenderer(ModEntities.MOTHERSHIP.get(), MothershipRenderer::new);
        event.registerEntityRenderer(ModEntities.GRAY_DRONE.get(), ctx -> new GlowingGeoRenderer<>(ctx,
                new DefaultedEntityGeoModel<GrayDrone>(HollowFrontier.id("gray_probe")).withAltTexture(HollowFrontier.id("gray_drone")))
                .withScale(1.35F).withRenderLayer(AutoGlowingGeoLayer::new));
        event.registerBlockEntityRenderer(ModBlockEntities.PHLOGISTON_BEACON.get(), PhlogistonBeaconRenderer::new);
        event.registerBlockEntityRenderer(ModBlockEntities.BOUNTY_BOARD.get(), BountyBoardRenderer::new);
        event.registerEntityRenderer(ModEntities.HANGING_BOUNTY_BOARD.get(), HangingBountyBoardRenderer::new);
        event.registerEntityRenderer(ModEntities.GRAY_WARDEN.get(), ctx -> new GlowingGeoRenderer<>(ctx,
                new DefaultedEntityGeoModel<GrayWarden>(HollowFrontier.id("gray_warden"))).withRenderLayer(AutoGlowingGeoLayer::new));
        event.registerEntityRenderer(ModEntities.DISGUISED_GRAY.get(), DisguisedGrayRenderer::new); // 21c
        event.registerEntityRenderer(ModEntities.OUTLAW.get(), ctx -> new HumanoidGeoRenderer<>(ctx, "outlaw", 1.0F));
        event.registerEntityRenderer(ModEntities.OUTLAW_GUNSLINGER.get(), ctx -> new HumanoidGeoRenderer<>(ctx, "outlaw_gunslinger", 1.0F));
        event.registerEntityRenderer(ModEntities.OUTLAW_BOSS.get(), ctx -> new HumanoidGeoRenderer<>(ctx, "outlaw_boss", 1.15F));
        String[] folk = Arrays.stream(Folk.values()).map(Folk::texture).toArray(String[]::new);
        event.registerEntityRenderer(ModEntities.TOWNSFOLK.get(), ctx -> new HumanoidGeoRenderer<>(ctx, folk, (Townsfolk t) -> t.getFolk().ordinal(), 1.0F));
        String[] ranks = Arrays.stream(Lawman.Rank.values()).map(Lawman.Rank::texture).toArray(String[]::new);
        event.registerEntityRenderer(ModEntities.LAWMAN.get(), ctx -> new HumanoidGeoRenderer<>(ctx, ranks, (Lawman l) -> l.getRank().ordinal(), 1.0F));
        event.registerEntityRenderer(ModEntities.BARMAID.get(), BarmaidRenderer::new);
        // Vanilla cats, plus town cat accessories and the ghost cat.
        event.registerEntityRenderer(EntityTypes.CAT, FrontierCatRenderer::new);
    }

    @SubscribeEvent
    static void registerLayers(EntityRenderersEvent.RegisterLayerDefinitions event) {
        event.registerLayerDefinition(CatAccessoryModel.LAYER, CatAccessoryModel::createLayer);
    }
}
