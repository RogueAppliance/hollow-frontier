package com.hollowfrontier.client;

import com.geckolib.model.DefaultedEntityGeoModel;
import com.geckolib.renderer.GeoEntityRenderer;
import com.geckolib.renderer.base.GeoRenderState;
import com.hollowfrontier.HollowFrontier;
import com.hollowfrontier.content.steam.submersible.SteamSubmersible;

import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.state.EntityRenderState;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.AABB;

/**
 * The Steam Submersible (43c; GeckoLib model "steam_submersible", bow toward -Z like a mob's face). Turns smoothly between
 * ticks, as the airship does, and isn't culled while its bow and screw, longer than its hit box, could be on screen.
 */
public class SubmersibleRenderer<R extends EntityRenderState & GeoRenderState> extends GeoEntityRenderer<SteamSubmersible, R> {
    public SubmersibleRenderer(EntityRendererProvider.Context context) {
        super(context, new DefaultedEntityGeoModel<SteamSubmersible>(HollowFrontier.id("steam_submersible")));
        this.shadowRadius = 1.2F;
        withRenderLayer(com.geckolib.renderer.layer.builtin.AutoGlowingGeoLayer::new); // the lamp and the ports
    }

    // Its glass is left out for the crew looking out through it in first person. Translucent glass is drawn before the
    // water and writes depth, so from inside a dome awash the sea's surface vanished behind every pane (the 2026-11-09
    // playtest); the saucer hides its canopy from its crew the same way.
    static final com.geckolib.constant.dataticket.DataTicket<Boolean> CREW_VIEW =
            com.geckolib.constant.dataticket.DataTicket.create("hollowfrontier_submersible_crew_view", Boolean.class);

    @Override
    public void captureDefaultRenderState(SteamSubmersible sub, Void relatedObject, R state, float partialTick) {
        super.captureDefaultRenderState(sub, relatedObject, state, partialTick);
        net.minecraft.client.Minecraft mc = net.minecraft.client.Minecraft.getInstance();
        state.addGeckolibData(CREW_VIEW, mc.options.getCameraType().isFirstPerson() && mc.getCameraEntity() != null && sub.hasPassenger(mc.getCameraEntity()));
    }

    @Override
    public void adjustModelBonesForRender(com.geckolib.renderer.base.RenderPassInfo<R> info, com.geckolib.renderer.base.BoneSnapshots snapshots) {
        super.adjustModelBonesForRender(info, snapshots);
        boolean crew = info.renderState().getOrDefaultGeckolibData(CREW_VIEW, false);
        snapshots.ifPresent("glass", bone -> bone.skipRender(crew));
    }

    @Override
    protected float calculateYRot(SteamSubmersible sub, float yHeadRot, float partialTick) {
        return Mth.rotLerp(partialTick, sub.yRotO, sub.getYRot());
    }

    @Override
    // The dome's panes are tinted glass, so the skin is drawn translucent (vanilla's type, as the saucer's canopy is), and
    // one order after its crew, who would otherwise be hidden behind the glass's depth.
    public net.minecraft.client.renderer.rendertype.RenderType getRenderType(R state, net.minecraft.resources.Identifier texture) {
        return net.minecraft.client.renderer.rendertype.RenderTypes.entityTranslucent(texture);
    }

    @Override
    public void submitRenderTasks(com.geckolib.renderer.base.RenderPassInfo<R> info, net.minecraft.client.renderer.OrderedSubmitNodeCollector collector,
            net.minecraft.client.renderer.rendertype.RenderType renderType) {
        super.submitRenderTasks(info, collector instanceof net.minecraft.client.renderer.SubmitNodeCollector root ? root.order(1) : collector, renderType);
    }

    @Override
    protected AABB getBoundingBoxForCulling(SteamSubmersible sub, float partialTicks) {
        return super.getBoundingBoxForCulling(sub, partialTicks).inflate(1.5, 0.5, 1.5);
    }
}
