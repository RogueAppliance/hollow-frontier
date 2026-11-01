package com.hollowfrontier.client;

import com.geckolib.model.DefaultedEntityGeoModel;
import com.geckolib.renderer.GeoEntityRenderer;
import com.geckolib.renderer.base.GeoRenderState;
import com.hollowfrontier.HollowFrontier;
import com.hollowfrontier.content.steam.airship.Airship;

import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.state.EntityRenderState;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.AABB;

/**
 * The airship (GeckoLib model "airship", bow toward -Z like a mob's face, so it heads the way the entity faces). Turns
 * smoothly between ticks, and isn't culled while the envelope, much bigger than the gondola's hit box, could be on screen.
 */
public class AirshipRenderer<R extends EntityRenderState & GeoRenderState> extends GeoEntityRenderer<Airship, R> {
    public AirshipRenderer(EntityRendererProvider.Context context) {
        super(context, new DefaultedEntityGeoModel<Airship>(HollowFrontier.id("airship")));
        this.shadowRadius = 1.6F;
    }

    @Override
    protected float calculateYRot(Airship ship, float yHeadRot, float partialTick) {
        return Mth.rotLerp(partialTick, ship.yRotO, ship.getYRot());
    }

    @Override
    protected AABB getBoundingBoxForCulling(Airship ship, float partialTicks) {
        return super.getBoundingBoxForCulling(ship, partialTicks).inflate(4.5, 2.5, 4.5);
    }
}
