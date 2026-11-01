package com.hollowfrontier.client;

import com.geckolib.animatable.GeoAnimatable;
import com.geckolib.model.GeoModel;
import com.geckolib.renderer.GeoEntityRenderer;
import com.geckolib.renderer.base.GeoRenderState;
import com.geckolib.renderer.base.RenderPassInfo;

import net.minecraft.client.renderer.OrderedSubmitNodeCollector;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.state.EntityRenderState;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.world.entity.Entity;

/**
 * A GeckoLib entity renderer that draws the Glowing outline. GeckoLib 5.5.7 submits a visible model once, with its normal
 * render type, and never uses the entity's outline colour, so glowing GeckoLib mobs showed no outline. When the entity
 * appears glowing this submits the posed model a second time with vanilla's outline render type, which 26.3 routes into
 * the entity-outline pass like any vanilla mob's. The outline is white (the vanilla colour without a team).
 */
public class GlowingGeoRenderer<T extends Entity & GeoAnimatable, R extends EntityRenderState & GeoRenderState> extends GeoEntityRenderer<T, R> {
    public GlowingGeoRenderer(EntityRendererProvider.Context context, GeoModel<T> model) {
        super(context, model);
    }

    @Override
    public void submitRenderTasks(RenderPassInfo<R> info, OrderedSubmitNodeCollector collector, RenderType renderType) {
        super.submitRenderTasks(info, collector, renderType);
        R state = info.renderState();
        // An invisible glowing mob already gets the outline type as its only render type.
        if (renderType != null && !renderType.isOutline() && state.appearsGlowing()) {
            super.submitRenderTasks(info, collector, RenderTypes.outline(getTextureLocation(state)));
        }
    }
}
