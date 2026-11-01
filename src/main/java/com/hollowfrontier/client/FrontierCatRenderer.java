package com.hollowfrontier.client;

import java.util.EnumMap;
import java.util.Map;
import java.util.WeakHashMap;

import com.mojang.blaze3d.vertex.PoseStack;
import com.hollowfrontier.HollowFrontier;
import com.hollowfrontier.content.frontier.cat.TownCat;
import com.hollowfrontier.content.frontier.cat.TownCat.Accessory;

import net.minecraft.client.model.animal.feline.AbstractFelineModel;
import net.minecraft.client.model.geom.EntityModelSet;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.CatRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.client.renderer.entity.state.CatRenderState;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.resources.Identifier;
import net.minecraft.util.ARGB;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.animal.feline.Cat;

/**
 * The vanilla cat renderer plus the frontier: an accessory layer (bandana, cowboy hat, brass star; grown cats only) and
 * the ghost cat, drawn pale and see-through with vanilla's entityTranslucent render type (shader-safe), fading softly in
 * and out as the server marks it faded. Ghosts cast no shadow.
 */
public class FrontierCatRenderer extends CatRenderer {
    private static final Identifier GHOST = HollowFrontier.id("textures/entity/cat/ghost.png");
    private static final int GHOST_ALPHA = 150;
    // Fade per frame, so a fade takes about half a second at 60 fps.
    private static final float FADE_STEP = 0.035F;

    private final Map<Cat, Float> fades = new WeakHashMap<>();

    public FrontierCatRenderer(EntityRendererProvider.Context context) {
        super(context);
        addLayer(new AccessoryLayer(this, context.getModelSet()));
    }

    public static class State extends CatRenderState {
        public Accessory accessory = Accessory.NONE;
        public int bandanaColor = Accessory.DEFAULT_BANDANA;
        public boolean ghost;
        /** 1 = fully shown, 0 = gone. */
        public float ghostFade = 1.0F;
    }

    @Override
    public CatRenderState createRenderState() {
        return new State();
    }

    @Override
    public void extractRenderState(Cat cat, CatRenderState state, float partialTicks) {
        super.extractRenderState(cat, state, partialTicks);
        if (!(state instanceof State s)) {
            return;
        }
        TownCat data = TownCat.of(cat);
        s.accessory = state.isBaby ? Accessory.NONE : data.accessory();
        s.bandanaColor = data.bandanaColor();
        s.ghost = data.ghost();
        if (s.ghost) {
            float target = data.faded() ? 0.0F : 1.0F;
            float fade = fades.getOrDefault(cat, target);
            fade += Mth.clamp(target - fade, -FADE_STEP, FADE_STEP);
            fades.put(cat, fade);
            s.ghostFade = fade;
            if (!state.isBaby) {
                state.texture = GHOST;
            }
            state.shadowRadius = 0.0F;
            state.shadowPieces.clear();
        } else {
            s.ghostFade = 1.0F;
        }
    }

    @Override
    protected RenderType getRenderType(CatRenderState state, boolean isBodyVisible, boolean forceTransparent, boolean appearGlowing) {
        if (state instanceof State s && s.ghost && isBodyVisible) {
            return s.ghostFade < 0.02F ? null : RenderTypes.entityTranslucent(getTextureLocation(state));
        }
        return super.getRenderType(state, isBodyVisible, forceTransparent, appearGlowing);
    }

    @Override
    protected int getModelTint(CatRenderState state) {
        if (state instanceof State s && s.ghost) {
            return ARGB.color(Math.round(GHOST_ALPHA * s.ghostFade), 235, 242, 255);
        }
        return super.getModelTint(state);
    }

    // A fading ghost's collar fades with it (a tamed ghost never fades).
    @Override
    protected boolean shouldRenderLayers(CatRenderState state) {
        return super.shouldRenderLayers(state) && !(state instanceof State s && s.ghostFade < 0.99F);
    }

    /** Draws a grown cat's accessory, the bandana tinted with its dye. */
    static class AccessoryLayer extends RenderLayer<CatRenderState, AbstractFelineModel<CatRenderState>> {
        private final Map<Accessory, CatAccessoryModel> models = new EnumMap<>(Accessory.class);
        private final Map<Accessory, Identifier> textures = Map.of(
                Accessory.BANDANA, HollowFrontier.id("textures/entity/cat/cat_bandana.png"),
                Accessory.COWBOY_HAT, HollowFrontier.id("textures/entity/cat/cat_cowboy_hat.png"),
                Accessory.SHERIFF_STAR, HollowFrontier.id("textures/entity/cat/cat_sheriff_star.png"));

        AccessoryLayer(RenderLayerParent<CatRenderState, AbstractFelineModel<CatRenderState>> renderer, EntityModelSet modelSet) {
            super(renderer);
            for (Accessory accessory : textures.keySet()) {
                models.put(accessory, new CatAccessoryModel(modelSet.bakeLayer(CatAccessoryModel.LAYER), accessory));
            }
        }

        @Override
        public void submit(PoseStack poseStack, SubmitNodeCollector collector, int lightCoords, CatRenderState state, float yRot, float xRot) {
            if (state instanceof State s && s.accessory != Accessory.NONE && !state.isBaby) {
                int color = s.accessory == Accessory.BANDANA ? s.bandanaColor : -1;
                coloredCutoutModelCopyLayerRender(models.get(s.accessory), textures.get(s.accessory), poseStack, collector, lightCoords, state, color, 1);
            }
        }
    }
}
