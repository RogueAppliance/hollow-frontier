package com.hollowfrontier.client;

import com.geckolib.animatable.GeoAnimatable;
import com.geckolib.constant.DataTickets;
import com.geckolib.model.DefaultedEntityGeoModel;
import com.geckolib.renderer.base.BoneSnapshots;
import com.geckolib.renderer.base.GeoRenderState;
import com.geckolib.renderer.base.RenderPassInfo;
import com.geckolib.renderer.layer.builtin.AutoGlowingGeoLayer;
import com.hollowfrontier.HollowFrontier;

import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.state.LivingEntityRenderState;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.LivingEntity;

/**
 * The Still Sea's creatures (43b): a GeckoLib model named after the entity and a shadow to match. The whole body pitches
 * with the way it's swimming (the {@code body} bone, by the entity's pitch: a swimmer's look is its heading), which a land
 * beast's renderer never needs. Young ones are drawn at half size where the kind breeds (the Great Sea Cow), and
 * {@link #glowing} adds the {@code _glowmask} layer (the Lampjelly's light).
 */
public class SeaCreatureRenderer<T extends LivingEntity & GeoAnimatable, R extends LivingEntityRenderState & GeoRenderState> extends GlowingGeoRenderer<T, R> {
    private final boolean hasYoung;

    public SeaCreatureRenderer(EntityRendererProvider.Context context, String name, float shadow, boolean hasYoung) {
        super(context, new DefaultedEntityGeoModel<>(HollowFrontier.id(name)));
        this.shadowRadius = shadow;
        this.hasYoung = hasYoung;
    }

    public SeaCreatureRenderer<T, R> glowing() {
        withRenderLayer(AutoGlowingGeoLayer::new);
        return this;
    }

    @Override
    public void adjustModelBonesForRender(RenderPassInfo<R> info, BoneSnapshots snapshots) {
        super.adjustModelBonesForRender(info, snapshots);
        float pitch = info.getOrDefaultGeckolibData(DataTickets.ENTITY_PITCH, 0.0F) * Mth.DEG_TO_RAD;
        snapshots.ifPresent("body", bone -> bone.setRotX(bone.getRotX() - pitch));
    }

    @Override
    public void scaleModelForRender(RenderPassInfo<R> info, float widthScale, float heightScale) {
        float young = hasYoung && info.renderState().isBaby ? 0.5F : 1.0F;
        super.scaleModelForRender(info, widthScale * young, heightScale * young);
    }
}
