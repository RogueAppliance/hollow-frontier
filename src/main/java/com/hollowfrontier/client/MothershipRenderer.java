package com.hollowfrontier.client;

import com.geckolib.constant.dataticket.DataTicket;
import com.geckolib.model.DefaultedEntityGeoModel;
import com.geckolib.renderer.base.BoneSnapshots;
import com.geckolib.renderer.base.GeoRenderState;
import com.geckolib.renderer.base.RenderPassInfo;
import com.geckolib.renderer.layer.builtin.AutoGlowingGeoLayer;
import com.hollowfrontier.HollowFrontier;
import com.hollowfrontier.content.ufo.Mothership;

import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.state.EntityRenderState;

/**
 * The Mothership (13d, {@code mothership}, made by tools/models/SaucerModels.java): a 12-block stepped disc of dark Gray
 * alloy, its turning rim strung with violet lights, a dark dome with lit windows, the beam emitter underneath. Its belly
 * doors are shut till the last phase; then they're gone and its glowing core shows ({@link Mothership#isExposed}). The
 * lights, windows, emitter and core glow. Falling, it tips a little rather than rolling onto its side like a mob.
 */
public class MothershipRenderer<R extends EntityRenderState & GeoRenderState> extends GlowingGeoRenderer<Mothership, R> {
    static final DataTicket<Boolean> EXPOSED = DataTicket.create("hollowfrontier_mothership_exposed", Boolean.class);

    public MothershipRenderer(EntityRendererProvider.Context context) {
        super(context, new DefaultedEntityGeoModel<>(HollowFrontier.id("mothership")));
        withRenderLayer(AutoGlowingGeoLayer::new);
        this.shadowRadius = 0.0F; // it hangs twenty blocks up; a shadow that size only looked odd
    }

    @Override
    public void captureDefaultRenderState(Mothership ship, Void relatedObject, R state, float partialTick) {
        super.captureDefaultRenderState(ship, relatedObject, state, partialTick);
        state.addGeckolibData(EXPOSED, ship.isExposed());
    }

    @Override
    public void adjustModelBonesForRender(RenderPassInfo<R> info, BoneSnapshots snapshots) {
        super.adjustModelBonesForRender(info, snapshots);
        boolean exposed = info.renderState().getOrDefaultGeckolibData(EXPOSED, false);
        snapshots.ifPresent("doors", bone -> bone.skipRender(exposed));
        snapshots.ifPresent("core", bone -> bone.skipRender(!exposed));
    }

    @Override
    protected float getDeathMaxRotation(GeoRenderState state) {
        return 12.0F;
    }
}
