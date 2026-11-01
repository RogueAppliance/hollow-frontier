package com.hollowfrontier.client;

import com.geckolib.model.DefaultedItemGeoModel;
import com.geckolib.renderer.GeoItemRenderer;
import com.geckolib.renderer.base.GeoRenderState;
import com.geckolib.renderer.layer.builtin.AutoGlowingGeoLayer;
import com.hollowfrontier.content.concavia.SunstoneLensItem;
import com.hollowfrontier.registry.ModDataComponents;

import net.minecraft.client.Minecraft;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.level.Level;

/**
 * GeckoLib renderer for the Sunstone Lens held: geckolib/models/item/sunstone_lens.geo.json, its animation, and the
 * textures/item/sunstone_lens sheet with the sunstone drawn full-bright from the glowmask. Feeds its last beam (the
 * LAST_FIRED component) into the render state, as GunRenderer does a gun's last shot. Its inventory icon is flat
 * (sunstone_lens_icon).
 */
public class LensRenderer extends GeoItemRenderer<SunstoneLensItem> {
    public LensRenderer(SunstoneLensItem item) {
        super(new DefaultedItemGeoModel<SunstoneLensItem>(BuiltInRegistries.ITEM.getKey(item)));
        withRenderLayer(AutoGlowingGeoLayer::new);
    }

    @Override
    public void captureDefaultRenderState(SunstoneLensItem lens, RenderData data, GeoRenderState state, float partialTick) {
        super.captureDefaultRenderState(lens, data, state, partialTick);
        Level level = data.level() != null ? data.level() : Minecraft.getInstance().level;
        Long beam = data.itemStack().get(ModDataComponents.LAST_FIRED.get());
        state.addGeckolibData(SunstoneLensItem.LAST_BEAM_TICK, beam == null ? Long.MIN_VALUE : beam);
        state.addGeckolibData(SunstoneLensItem.SINCE_BEAM, beam == null || level == null ? 1000.0 : level.getGameTime() + partialTick - beam);
    }
}
