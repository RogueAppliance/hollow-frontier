package com.hollowfrontier.client;

import java.util.function.ToIntFunction;

import com.geckolib.animatable.GeoAnimatable;
import com.geckolib.constant.dataticket.DataTicket;
import com.geckolib.model.DefaultedEntityGeoModel;
import com.geckolib.model.GeoModel;
import com.geckolib.renderer.base.GeoRenderState;
import com.geckolib.renderer.layer.builtin.ItemInHandGeoLayer;
import com.hollowfrontier.HollowFrontier;

import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.state.LivingEntityRenderState;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.Mob;

/**
 * Shared GeckoLib renderer for the mod's humanoids: one "humanoid" geo model + animation file,
 * a per-entity texture (textures/entity/<texture>.png), and held items on the RightHandItem/LeftHandItem bones.
 * Mobs with several looks (townsfolk folk, lawman ranks) pass a list of textures and pick one per entity; the index
 * travels to the model through the render state.
 * The render state is GeckoLib-mixed into LivingEntityRenderState at runtime; the intersection bound satisfies the layer.
 */
public class HumanoidGeoRenderer<T extends Mob & GeoAnimatable, R extends LivingEntityRenderState & GeoRenderState> extends GlowingGeoRenderer<T, R> {
    static final DataTicket<Integer> VARIANT = DataTicket.create("hollowfrontier_variant", Integer.class);

    private final ToIntFunction<T> variant;

    public HumanoidGeoRenderer(EntityRendererProvider.Context context, String texture, float scale) {
        this(context, new String[] {texture}, entity -> 0, scale);
    }

    public HumanoidGeoRenderer(EntityRendererProvider.Context context, String[] textures, ToIntFunction<T> variant, float scale) {
        super(context, new Model<>(textures));
        this.variant = variant;
        withRenderLayer(new ItemInHandGeoLayer<>(context, this));
        withScale(scale);
    }

    @Override
    public void captureDefaultRenderState(T entity, Void relatedObject, R state, float partialTick) {
        super.captureDefaultRenderState(entity, relatedObject, state, partialTick);
        state.addGeckolibData(VARIANT, variant.applyAsInt(entity));
    }

    /** The shared humanoid geo model and animations, with the texture picked by the render state's variant. */
    static class Model<T extends GeoAnimatable> extends GeoModel<T> {
        private final DefaultedEntityGeoModel<T>[] looks;

        @SuppressWarnings("unchecked")
        Model(String[] textures) {
            looks = new DefaultedEntityGeoModel[textures.length];
            for (int i = 0; i < textures.length; i++) {
                looks[i] = new DefaultedEntityGeoModel<T>(HollowFrontier.id("humanoid")).withAltTexture(HollowFrontier.id(textures[i]));
            }
        }

        private DefaultedEntityGeoModel<T> look(GeoRenderState state) {
            int i = state.getOrDefaultGeckolibData(VARIANT, 0);
            return looks[i >= 0 && i < looks.length ? i : 0];
        }

        @Override
        public Identifier getModelResource(GeoRenderState state) {
            return look(state).getModelResource(state);
        }

        @Override
        public Identifier getTextureResource(GeoRenderState state) {
            return look(state).getTextureResource(state);
        }

        @Override
        public Identifier getAnimationResource(T animatable) {
            return looks[0].getAnimationResource(animatable);
        }
    }
}
