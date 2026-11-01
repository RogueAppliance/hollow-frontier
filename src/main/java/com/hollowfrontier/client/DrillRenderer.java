package com.hollowfrontier.client;

import com.geckolib.model.DefaultedItemGeoModel;
import com.geckolib.renderer.GeoItemRenderer;
import com.geckolib.renderer.base.GeoRenderState;
import com.hollowfrontier.content.steam.SteamCanisterItem;
import com.hollowfrontier.content.steam.SteamDrillItem;

import net.minecraft.client.Minecraft;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.entity.LivingEntity;

/**
 * GeckoLib renderer for the Steam Drill held: geckolib/models/item/steam_drill.geo.json, its animation, and the
 * textures/item/steam_drill sheet. Tells the item's animation controller whether to spin the bit: for the player at this
 * screen, while they're breaking a block with steam in their canisters; for anyone else, while their arm is swinging (all a
 * client can see of someone else's mining). Its inventory icon is flat (steam_drill_icon).
 */
public class DrillRenderer extends GeoItemRenderer<SteamDrillItem> {
    public DrillRenderer(SteamDrillItem item) {
        super(new DefaultedItemGeoModel<SteamDrillItem>(BuiltInRegistries.ITEM.getKey(item)));
    }

    @Override
    public void captureDefaultRenderState(SteamDrillItem drill, RenderData data, GeoRenderState state, float partialTick) {
        super.captureDefaultRenderState(drill, data, state, partialTick);
        Minecraft mc = Minecraft.getInstance();
        boolean drilling = false;
        if (data.itemOwner() instanceof LivingEntity holder && holder.getMainHandItem().getItem() instanceof SteamDrillItem) {
            drilling = holder == mc.player
                    ? mc.gameMode != null && mc.gameMode.isDestroying() && SteamCanisterItem.hasSteam(mc.player, SteamDrillItem.STEAM_PER_BLOCK)
                    : holder.isSwinging();
        }
        state.addGeckolibData(SteamDrillItem.DRILLING, drilling);
    }
}
