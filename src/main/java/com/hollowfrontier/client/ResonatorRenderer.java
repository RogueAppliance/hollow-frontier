package com.hollowfrontier.client;

import com.geckolib.model.DefaultedItemGeoModel;
import com.geckolib.renderer.GeoItemRenderer;
import com.geckolib.renderer.base.GeoRenderState;
import com.geckolib.renderer.layer.builtin.AutoGlowingGeoLayer;
import com.hollowfrontier.content.hollow.gateway.PolarResonatorItem;
import com.hollowfrontier.registry.ModDataComponents;

import net.minecraft.client.Minecraft;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

/**
 * GeckoLib renderer for the Polar Resonator held: geckolib/models/item/polar_resonator.geo.json, its animation, and the
 * textures/item/polar_resonator sheet with its fork tips drawn full-bright from the glowmask. Feeds how long ago it last
 * woke a ring into the render state for the item's animation controller. Its inventory icon is flat (polar_resonator_icon).
 */
public class ResonatorRenderer extends GeoItemRenderer<PolarResonatorItem> {
    public ResonatorRenderer(PolarResonatorItem item) {
        super(new DefaultedItemGeoModel<PolarResonatorItem>(BuiltInRegistries.ITEM.getKey(item)));
        withRenderLayer(AutoGlowingGeoLayer::new);
    }

    @Override
    public void captureDefaultRenderState(PolarResonatorItem resonator, RenderData data, GeoRenderState state, float partialTick) {
        super.captureDefaultRenderState(resonator, data, state, partialTick);
        ItemStack stack = data.itemStack();
        Level level = data.level() != null ? data.level() : Minecraft.getInstance().level;
        Long rang = stack.get(ModDataComponents.RESONATED.get());
        state.addGeckolibData(PolarResonatorItem.SINCE_RESONATED, level == null || rang == null ? -1.0 : Math.max(0.0, level.getGameTime() + partialTick - rang));
    }
}
