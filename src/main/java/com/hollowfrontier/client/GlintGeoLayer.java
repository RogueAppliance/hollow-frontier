package com.hollowfrontier.client;

import com.geckolib.animatable.GeoAnimatable;
import com.geckolib.constant.DataTickets;
import com.geckolib.renderer.base.GeoRenderState;
import com.geckolib.renderer.base.GeoRenderer;
import com.geckolib.renderer.base.RenderPassInfo;
import com.geckolib.renderer.layer.builtin.TextureLayerGeoLayer;

import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.feature.ItemFeatureRenderer;
import net.minecraft.client.renderer.rendertype.RenderTypes;

/**
 * The enchantment glint on a GeckoLib item model (22). GeckoLib passes the item's foil flag along (HAS_GLINT) but draws
 * nothing with it, so an enchanted gun looked plain. This redraws the model in vanilla's glint-only pass, the one a
 * banner shield's glint uses: it lands only where the model was already drawn, at the entity glint's scale.
 */
public class GlintGeoLayer<T extends GeoAnimatable, O, R extends GeoRenderState> extends TextureLayerGeoLayer<T, O, R> {
    public GlintGeoLayer(GeoRenderer<T, O, R> renderer) {
        super(renderer, ItemFeatureRenderer.ENCHANTED_GLINT_ITEM, texture -> RenderTypes.patternedShieldGlint());
    }

    @Override
    public void submitRenderTask(RenderPassInfo<R> renderPassInfo, SubmitNodeCollector collector) {
        if (renderPassInfo.renderState().getOrDefaultGeckolibData(DataTickets.HAS_GLINT, false)) {
            super.submitRenderTask(renderPassInfo, collector);
        }
    }
}
