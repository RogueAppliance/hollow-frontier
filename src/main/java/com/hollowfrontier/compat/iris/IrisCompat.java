package com.hollowfrontier.compat.iris;

import com.geckolib.animatable.GeoAnimatable;
import com.geckolib.renderer.base.GeoRenderState;
import com.geckolib.renderer.layer.builtin.AutoGlowingGeoLayer;
import com.mojang.renderpearl.api.pipeline.RenderPipeline;

import net.irisshaders.iris.api.v0.IrisApi;
import net.irisshaders.iris.api.v0.IrisProgram;

/**
 * Iris (optional, client only): GeckoLib draws every glowing texture ({@code _glowmask}) through a pipeline of its own,
 * which Iris doesn't know, so a shader pack lit the saucers' lights, the Grays' eyes and the phlogiston glow like any other
 * surface, dim at night. Assigned to the pack's emissive-entity program, they glow under shaders as vanilla's eyes do.
 * Only loaded when Iris is.
 */
public final class IrisCompat {
    private IrisCompat() {}

    public static void assignPipelines() {
        IrisApi.getInstance().assignPipeline(GlowPipeline.get(), IrisProgram.EMISSIVE_ENTITIES);
    }

    /** Never made: only reaches GeckoLib's protected glow pipeline. */
    private static final class GlowPipeline extends AutoGlowingGeoLayer<GeoAnimatable, Object, GeoRenderState> {
        private GlowPipeline() {
            super(null);
        }

        static RenderPipeline get() {
            return RENDER_PIPELINE;
        }
    }
}
