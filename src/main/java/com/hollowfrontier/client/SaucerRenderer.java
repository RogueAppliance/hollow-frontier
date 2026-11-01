package com.hollowfrontier.client;

import com.geckolib.constant.dataticket.DataTicket;
import com.geckolib.model.DefaultedEntityGeoModel;
import com.geckolib.renderer.base.BoneSnapshots;
import com.geckolib.renderer.base.GeoRenderState;
import com.geckolib.renderer.base.RenderPassInfo;
import com.geckolib.renderer.layer.builtin.AutoGlowingGeoLayer;
import com.hollowfrontier.HollowFrontier;
import com.hollowfrontier.content.ufo.PrototypeSaucer;
import com.hollowfrontier.content.ufo.SaucerFittings;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.OrderedSubmitNodeCollector;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.culling.Frustum;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.state.EntityRenderState;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.resources.Identifier;

/**
 * The Prototype Saucer (13c, {@code prototype_saucer}, full size, made by tools/models/SaucerModels.java): a stepped,
 * near-round alloy disc with a spinning band of rim lights, and a brass-ribbed glass dome the crew show through. It draws
 * translucent for the glass. The beam emitter, lens array and navigation core each show once fitted
 * ({@link SaucerFittings#mask}); the rim lights, drive, emitter, lens eye, nav ball and console glow. The cloak and the
 * cargo hold don't show; a cloaked saucer isn't drawn at all, but for its own crew.
 */
public class SaucerRenderer<R extends EntityRenderState & GeoRenderState> extends GlowingGeoRenderer<PrototypeSaucer, R> {
    static final DataTicket<Integer> FITTINGS = DataTicket.create("hollowfrontier_saucer_fittings", Integer.class);
    static final String[] BONES = {"emitter", "lens", "nav"};
    static final int[] BITS = {SaucerFittings.EMITTER, SaucerFittings.LENS, SaucerFittings.NAV};
    // From inside in first person, the ribs, the glass's tint and the console filled the view (playtest): the crew's own
    // camera skips the dome, its cap and mast and the console, and sees out clear over the hull
    static final DataTicket<Boolean> CREW_VIEW = DataTicket.create("hollowfrontier_saucer_crew_view", Boolean.class);
    static final String[] DOME_BONES;
    static {
        java.util.List<String> bones = new java.util.ArrayList<>(java.util.List.of("dome", "nav", "cockpit"));
        for (int k = 0; k < 16; k++) {
            bones.add("rib_" + k);
            bones.add("pane_" + k);
        }
        DOME_BONES = bones.toArray(String[]::new);
    }

    public SaucerRenderer(EntityRendererProvider.Context context) {
        super(context, new DefaultedEntityGeoModel<>(HollowFrontier.id("prototype_saucer")));
        withRenderLayer(AutoGlowingGeoLayer::new);
        this.shadowRadius = 1.8F;
    }

    // Cloaked (15b), it's seen only by its own crew
    @Override
    public boolean shouldRender(PrototypeSaucer saucer, Frustum culler, double camX, double camY, double camZ, float partialTicks) {
        Minecraft mc = Minecraft.getInstance();
        if (saucer.isCloaked() && (mc.getCameraEntity() == null || !saucer.hasPassenger(mc.getCameraEntity()))) {
            return false;
        }
        return super.shouldRender(saucer, culler, camX, camY, camZ, partialTicks);
    }

    @Override
    public RenderType getRenderType(R state, Identifier texture) {
        return RenderTypes.entityTranslucent(texture);
    }

    // Drawn one order after everything else this frame, its crew included: players are translucent too, and drawn first,
    // the glass's depth hid the pilot behind it (runPhoto). After them it lays over them, the way glass should.
    @Override
    public void submitRenderTasks(RenderPassInfo<R> info, OrderedSubmitNodeCollector collector, RenderType renderType) {
        super.submitRenderTasks(info, collector instanceof SubmitNodeCollector root ? root.order(1) : collector, renderType);
    }

    @Override
    public void captureDefaultRenderState(PrototypeSaucer saucer, Void relatedObject, R state, float partialTick) {
        super.captureDefaultRenderState(saucer, relatedObject, state, partialTick);
        state.addGeckolibData(FITTINGS, saucer.fittingsMask());
        Minecraft mc = Minecraft.getInstance();
        state.addGeckolibData(CREW_VIEW, mc.options.getCameraType().isFirstPerson() && mc.getCameraEntity() != null
                && saucer.hasPassenger(mc.getCameraEntity()));
    }

    @Override
    public void adjustModelBonesForRender(RenderPassInfo<R> info, BoneSnapshots snapshots) {
        super.adjustModelBonesForRender(info, snapshots);
        int mask = info.renderState().getOrDefaultGeckolibData(FITTINGS, 0);
        for (int i = 0; i < BONES.length; i++) {
            boolean shown = (mask & BITS[i]) != 0;
            snapshots.ifPresent(BONES[i], bone -> bone.skipRender(!shown));
        }
        if (info.renderState().getOrDefaultGeckolibData(CREW_VIEW, false)) {
            for (String name : DOME_BONES) {
                snapshots.ifPresent(name, bone -> bone.skipRender(true));
            }
        }
    }
}
