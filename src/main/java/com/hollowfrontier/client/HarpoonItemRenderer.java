package com.hollowfrontier.client;

import com.geckolib.model.DefaultedItemGeoModel;
import com.geckolib.renderer.GeoItemRenderer;
import com.hollowfrontier.content.hollow.sea.HarpoonItem;

import net.minecraft.core.registries.BuiltInRegistries;

/**
 * GeckoLib renderer for a Harpoon held: geckolib/models/item/harpoon.geo.json (or toothed_harpoon, by the item) and the
 * textures/item sheet of the same name. Its inventory icon is flat (harpoon_icon).
 */
public class HarpoonItemRenderer extends GeoItemRenderer<HarpoonItem> {
    public HarpoonItemRenderer(HarpoonItem item) {
        super(new DefaultedItemGeoModel<HarpoonItem>(BuiltInRegistries.ITEM.getKey(item)));
    }
}
