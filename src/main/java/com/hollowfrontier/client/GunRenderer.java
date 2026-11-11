package com.hollowfrontier.client;

import com.geckolib.model.DefaultedItemGeoModel;
import com.geckolib.renderer.GeoItemRenderer;
import com.geckolib.renderer.base.GeoRenderState;
import com.geckolib.renderer.layer.builtin.AutoGlowingGeoLayer;
import com.hollowfrontier.content.frontier.gun.GunItem;
import com.hollowfrontier.registry.ModDataComponents;

import net.minecraft.client.Minecraft;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

/**
 * GeckoLib renderer for the muzzleloaders: geckolib/models/item/<gun>.geo.json, geckolib/animations/item/<gun>.animation.json
 * and textures/item/<gun>.png. Feeds the stack's reload/fire state into the render state for GunItem's animation controller.
 */
public class GunRenderer extends GeoItemRenderer<GunItem> {
    /** As the muzzleloaders', with the gun's <gun>_glowmask texture drawn full-bright: the phlogiston guns' coils and emitters (13b). */
    public static GunRenderer glowing(GunItem item) {
        GunRenderer renderer = new GunRenderer(item);
        renderer.withRenderLayer(AutoGlowingGeoLayer::new);
        return renderer;
    }

    public GunRenderer(GunItem item) {
        super(new DefaultedItemGeoModel<GunItem>(BuiltInRegistries.ITEM.getKey(item)));
        withRenderLayer(GlintGeoLayer::new);
    }

    @Override
    public void captureDefaultRenderState(GunItem gun, RenderData data, GeoRenderState state, float partialTick) {
        super.captureDefaultRenderState(gun, data, state, partialTick);
        ItemStack stack = data.itemStack();
        Level level = data.level() != null ? data.level() : Minecraft.getInstance().level;
        if (level == null) {
            return;
        }
        double now = level.getGameTime() + partialTick;
        Long reloadStart = stack.get(ModDataComponents.RELOAD_START.get());
        state.addGeckolibData(GunItem.RELOAD_TICKS, reloadStart == null ? -1.0 : Math.max(0.0, now - reloadStart));
        state.addGeckolibData(GunItem.RELOAD_SPEED, reloadStart == null ? 1.0 : gun.reloadSpeed(stack, level));
        Long fired = stack.get(ModDataComponents.LAST_FIRED.get());
        state.addGeckolibData(GunItem.LAST_FIRED_TICK, fired == null ? Long.MIN_VALUE : fired);
        state.addGeckolibData(GunItem.SINCE_FIRED, fired == null ? 1000.0 : now - fired);
    }
}
