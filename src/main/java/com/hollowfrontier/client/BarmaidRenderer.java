package com.hollowfrontier.client;

import com.geckolib.constant.dataticket.DataTicket;
import com.geckolib.model.DefaultedEntityGeoModel;
import com.geckolib.model.GeoModel;
import com.geckolib.renderer.base.GeoRenderState;
import com.geckolib.renderer.layer.builtin.ItemInHandGeoLayer;
import com.hollowfrontier.HollowFrontier;
import com.hollowfrontier.content.frontier.saloon.Barmaid;
import com.hollowfrontier.content.frontier.saloon.Hostess;

import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.state.LivingEntityRenderState;
import net.minecraft.resources.Identifier;

/**
 * Barmaids: each {@link Hostess} has her own geo model (hairstyle, hat, flowers) and texture, all sharing one
 * animation file. The hostess travels to the model through the render state.
 */
public class BarmaidRenderer<R extends LivingEntityRenderState & GeoRenderState> extends GlowingGeoRenderer<Barmaid, R> {
    static final DataTicket<Integer> HOSTESS = DataTicket.create("hollowfrontier_hostess", Integer.class);

    public BarmaidRenderer(EntityRendererProvider.Context context) {
        super(context, new Model());
        withRenderLayer(new ItemInHandGeoLayer<>(context, this));
        withScale(0.95F);
    }

    @Override
    public void captureDefaultRenderState(Barmaid barmaid, Void relatedObject, R state, float partialTick) {
        super.captureDefaultRenderState(barmaid, relatedObject, state, partialTick);
        state.addGeckolibData(HOSTESS, barmaid.getHostess().ordinal());
    }

    /** Picks barmaid_<hostess> model and texture; animations are always geckolib/animations/entity/barmaid. */
    static class Model extends GeoModel<Barmaid> {
        private final DefaultedEntityGeoModel<Barmaid>[] looks;

        @SuppressWarnings("unchecked")
        Model() {
            Hostess[] all = Hostess.values();
            looks = new DefaultedEntityGeoModel[all.length];
            for (Hostess h : all) {
                Identifier id = HollowFrontier.id("barmaid_" + h.id());
                looks[h.ordinal()] = new DefaultedEntityGeoModel<Barmaid>(id).withAltAnimations(HollowFrontier.id("barmaid"));
            }
        }

        private DefaultedEntityGeoModel<Barmaid> look(GeoRenderState state) {
            return looks[state.getOrDefaultGeckolibData(HOSTESS, 0)];
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
        public Identifier getAnimationResource(Barmaid barmaid) {
            return looks[0].getAnimationResource(barmaid);
        }
    }
}
