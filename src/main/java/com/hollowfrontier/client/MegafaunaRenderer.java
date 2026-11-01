package com.hollowfrontier.client;

import com.geckolib.animatable.GeoAnimatable;
import com.geckolib.constant.DataTickets;
import com.geckolib.constant.dataticket.DataTicket;
import com.geckolib.model.DefaultedEntityGeoModel;
import com.geckolib.renderer.base.BoneSnapshots;
import com.geckolib.renderer.base.GeoRenderState;
import com.geckolib.renderer.base.RenderPassInfo;
import com.geckolib.renderer.layer.builtin.AutoGlowingGeoLayer;
import com.hollowfrontier.HollowFrontier;
import com.hollowfrontier.content.hollow.gray.Specimens;

import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.state.LivingEntityRenderState;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.LivingEntity;

/**
 * The wild megafauna (10a): a GeckoLib model named after the entity, a shadow to match its bulk, and the {@code neck}
 * bone turned to where the beast is looking (GeckoLib leaves that to the renderer; the head under it stays free to animate).
 * A beast the Grays have tagged (10b) shows its plating and implant (bones {@link #TAGGED_BONES}, hidden otherwise); the
 * implant glows (the {@code _glowmask} texture, which has nothing else on it).
 */
public class MegafaunaRenderer<T extends LivingEntity & GeoAnimatable, R extends LivingEntityRenderState & GeoRenderState> extends GlowingGeoRenderer<T, R> {
    static final DataTicket<Boolean> TAGGED = DataTicket.create("hollowfrontier_gray_tagged", Boolean.class);
    static final String[] TAGGED_BONES = {"plating", "plating_head", "implant"};

    public MegafaunaRenderer(EntityRendererProvider.Context context, String name, float shadow) {
        super(context, new DefaultedEntityGeoModel<>(HollowFrontier.id(name)));
        this.shadowRadius = shadow;
        withRenderLayer(AutoGlowingGeoLayer::new);
    }

    @Override
    public void captureDefaultRenderState(T animatable, Void relatedObject, R state, float partialTick) {
        super.captureDefaultRenderState(animatable, relatedObject, state, partialTick);
        state.addGeckolibData(TAGGED, Specimens.isTagged(animatable));
    }

    @Override
    public void adjustModelBonesForRender(RenderPassInfo<R> info, BoneSnapshots snapshots) {
        super.adjustModelBonesForRender(info, snapshots);
        // Added to the animated rotation (GeckoLib's own hardcodedHeadRotation replaces it, which would undo a neck's animation)
        float pitch = info.getOrDefaultGeckolibData(DataTickets.ENTITY_PITCH, 0.0F) * Mth.DEG_TO_RAD;
        float yaw = info.getOrDefaultGeckolibData(DataTickets.ENTITY_YAW, 0.0F) * Mth.DEG_TO_RAD;
        snapshots.ifPresent("neck", bone -> bone.setRotX(bone.getRotX() - pitch).setRotY(bone.getRotY() - yaw));
        boolean tagged = info.renderState().getOrDefaultGeckolibData(TAGGED, false);
        for (String name : TAGGED_BONES) {
            snapshots.ifPresent(name, bone -> bone.skipRender(!tagged));
        }
    }
}
