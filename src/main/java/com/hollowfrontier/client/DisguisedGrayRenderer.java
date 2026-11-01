package com.hollowfrontier.client;

import com.geckolib.constant.dataticket.DataTicket;
import com.geckolib.model.DefaultedEntityGeoModel;
import com.geckolib.model.GeoModel;
import com.geckolib.renderer.base.GeoRenderState;
import com.hollowfrontier.HollowFrontier;
import com.hollowfrontier.content.ufo.DisguisedGray;

import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.state.LivingEntityRenderState;
import net.minecraft.resources.Identifier;

/**
 * The Disguised Gray (21c): its own geo model (the Warden's, under a duster and a hat too big for it), and its texture by
 * whether it's been found out: exposed, the hat's pixels are clear, so the hat's gone.
 */
public class DisguisedGrayRenderer<R extends LivingEntityRenderState & GeoRenderState> extends GlowingGeoRenderer<DisguisedGray, R> {
    static final DataTicket<Boolean> EXPOSED = DataTicket.create("hollowfrontier_exposed", Boolean.class);

    public DisguisedGrayRenderer(EntityRendererProvider.Context context) {
        super(context, new Model());
    }

    @Override
    public void captureDefaultRenderState(DisguisedGray entity, Void relatedObject, R state, float partialTick) {
        super.captureDefaultRenderState(entity, relatedObject, state, partialTick);
        state.addGeckolibData(EXPOSED, entity.isExposed());
    }

    static class Model extends GeoModel<DisguisedGray> {
        private final DefaultedEntityGeoModel<DisguisedGray> disguised = new DefaultedEntityGeoModel<>(HollowFrontier.id("disguised_gray"));
        private final DefaultedEntityGeoModel<DisguisedGray> exposed = new DefaultedEntityGeoModel<DisguisedGray>(HollowFrontier.id("disguised_gray"))
                .withAltTexture(HollowFrontier.id("disguised_gray_exposed"));

        private DefaultedEntityGeoModel<DisguisedGray> look(GeoRenderState state) {
            return state.getOrDefaultGeckolibData(EXPOSED, false) ? exposed : disguised;
        }

        @Override
        public Identifier getModelResource(GeoRenderState state) {
            return look(state).getModelResource(state);
        }

        @Override
        public Identifier getTextureResource(GeoRenderState state) {
            return look(state).getTextureResource(state);
        }

        @Override
        public Identifier getAnimationResource(DisguisedGray animatable) {
            return disguised.getAnimationResource(animatable);
        }
    }
}
