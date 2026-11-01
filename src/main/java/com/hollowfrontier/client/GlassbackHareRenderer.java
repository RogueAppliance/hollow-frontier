package com.hollowfrontier.client;

import com.geckolib.constant.dataticket.DataTicket;
import com.geckolib.model.DefaultedEntityGeoModel;
import com.geckolib.renderer.base.BoneSnapshots;
import com.geckolib.renderer.base.GeoRenderState;
import com.geckolib.renderer.base.RenderPassInfo;
import com.geckolib.renderer.layer.builtin.AutoGlowingGeoLayer;
import com.hollowfrontier.HollowFrontier;
import com.hollowfrontier.content.hollow.creature.GlassbackHare;

import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.state.LivingEntityRenderState;

/**
 * The Glassback Hare, with its crystals glowing (the {@code _glowmask} texture), and the ear tag on its left ear drawn only
 * on a tagged hare (the Gray lab's, 9b3). The flag travels to the model through the render state.
 */
public class GlassbackHareRenderer<R extends LivingEntityRenderState & GeoRenderState> extends GlowingGeoRenderer<GlassbackHare, R> {
    static final DataTicket<Boolean> EAR_TAG = DataTicket.create("hollowfrontier_ear_tag", Boolean.class);

    public GlassbackHareRenderer(EntityRendererProvider.Context context) {
        super(context, new DefaultedEntityGeoModel<>(HollowFrontier.id("glassback_hare")));
        withRenderLayer(AutoGlowingGeoLayer::new);
    }

    @Override
    public void captureDefaultRenderState(GlassbackHare hare, Void relatedObject, R state, float partialTick) {
        super.captureDefaultRenderState(hare, relatedObject, state, partialTick);
        state.addGeckolibData(EAR_TAG, hare.hasEarTag());
    }

    @Override
    public void adjustModelBonesForRender(RenderPassInfo<R> info, BoneSnapshots snapshots) {
        super.adjustModelBonesForRender(info, snapshots);
        boolean tagged = info.renderState().getOrDefaultGeckolibData(EAR_TAG, false);
        snapshots.ifPresent("ear_tag", bone -> bone.skipRender(!tagged));
    }
}
