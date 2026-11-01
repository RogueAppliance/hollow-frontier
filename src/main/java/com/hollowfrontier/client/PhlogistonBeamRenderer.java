package com.hollowfrontier.client;

import org.joml.Vector3f;

import com.hollowfrontier.content.ufo.PhlogistonBeam;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;

import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.blockentity.BeaconRenderer;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.state.EntityRenderState;
import net.minecraft.client.renderer.state.level.CameraRenderState;

/**
 * Draws a {@link PhlogistonBeam}: vanilla's beacon beam (a bright core in a soft glow), laid along the shot instead of standing
 * up, as thick as a finger and thinning to nothing over the beam's short life.
 */
public class PhlogistonBeamRenderer extends EntityRenderer<PhlogistonBeam, PhlogistonBeamRenderer.State> {
    static final float CORE = 0.035F, GLOW = PhlogistonBeam.GLOW_WIDTH;

    public static class State extends EntityRenderState {
        final Vector3f reach = new Vector3f();
        int colour;
        float age;
        float width = 1.0F;
        int life = PhlogistonBeam.LIFETIME;
        boolean soft;
    }

    public PhlogistonBeamRenderer(EntityRendererProvider.Context context) {
        super(context);
    }

    @Override
    public State createRenderState() {
        return new State();
    }

    @Override
    public void extractRenderState(PhlogistonBeam beam, State state, float partialTick) {
        super.extractRenderState(beam, state, partialTick);
        state.reach.set(beam.reach());
        state.colour = beam.colour();
        state.age = beam.tickCount + partialTick;
        state.width = beam.width();
        state.life = Math.max(1, beam.life());
        state.soft = beam.soft();
    }

    @Override
    protected boolean affectedByCulling(PhlogistonBeam beam) {
        return false; // the entity is a point at the muzzle; the beam runs on far past it
    }

    @Override
    public void submit(State state, PoseStack poseStack, SubmitNodeCollector collector, CameraRenderState camera) {
        float length = state.reach.length();
        float fade = 1.0F - Math.min(1.0F, state.age / state.life);
        if (length < 0.1F || fade <= 0.0F) {
            return;
        }
        poseStack.pushPose();
        // Turn the beacon beam's up (+Y) onto the shot: pitch it over from the vertical, then swing it round to the heading
        poseStack.rotate(Axis.YP, (float) Math.atan2(state.reach.x, state.reach.z));
        poseStack.rotate(Axis.XP, (float) Math.acos(Math.max(-1.0F, Math.min(1.0F, state.reach.y / length))));
        poseStack.translate(-0.5F, 0.0F, -0.5F); // the beam code centres itself on a block
        BeaconRenderer.submitBeaconBeam(poseStack, collector, BeaconRenderer.BEAM_LOCATION, 1.0F, state.age * 4.0F, 0,
                Math.max(1, Math.round(length)), state.colour, state.soft ? 0.0F : CORE * state.width * fade, GLOW * state.width * fade);
        poseStack.popPose();
        super.submit(state, poseStack, collector, camera);
    }
}
