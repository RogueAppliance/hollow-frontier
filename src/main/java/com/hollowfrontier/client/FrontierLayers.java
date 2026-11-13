package com.hollowfrontier.client;

import com.google.common.reflect.TypeToken;
import com.hollowfrontier.HollowFrontier;
import com.hollowfrontier.content.frontier.gear.LassoItem;
import com.hollowfrontier.registry.ModAttachments;
import com.mojang.blaze3d.vertex.PoseStack;

import net.minecraft.client.entity.ClientAvatarEntity;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.model.player.PlayerModel;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.AbstractHorseRenderer;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.LivingEntityRenderer;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.client.renderer.entity.player.AvatarRenderer;
import net.minecraft.client.renderer.entity.state.AvatarRenderState;
import net.minecraft.client.renderer.entity.state.EquineRenderState;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.resources.Identifier;
import net.minecraft.util.context.ContextKey;
import net.minecraft.world.entity.Avatar;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.entity.animal.equine.AbstractHorse;
import net.minecraft.world.entity.player.PlayerModelType;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;
import net.neoforged.neoforge.client.renderstate.AvatarRenderStateModifier;
import net.neoforged.neoforge.client.renderstate.RegisterRenderStateModifiersEvent;

/**
 * Two things drawn onto vanilla's own renderers (46b, asked for by the user after their first look):
 * <ul>
 * <li><b>Horseshoes</b> on a shod horse, donkey or mule: a band of the shoe's metal round the foot of each leg, and the
 *     shoe itself underneath. One texture a tier, laid over the vanilla horse's leg on its own sheet.</li>
 * <li><b>The lasso's wind-up</b>: while a player holds a lasso drawn back, its loop whirls over their head on a line from
 *     their hand. Not drawn in first person (a player's layers never are); there the loop is seen once it's thrown.</li>
 * </ul>
 */
@EventBusSubscriber(modid = HollowFrontier.MODID, value = Dist.CLIENT)
public final class FrontierLayers {
    private FrontierLayers() {}

    /** A horse's shoes (0 none, then iron, brass, starsteel), and whether a player is winding up a lasso. */
    static final ContextKey<Integer> SHOD = new ContextKey<>(HollowFrontier.id("horseshoe_tier"));
    static final ContextKey<HumanoidArm> WINDING = new ContextKey<>(HollowFrontier.id("lasso_winding"));

    private static final Identifier[] SHOES = {
            HollowFrontier.id("textures/entity/horseshoes_iron.png"),
            HollowFrontier.id("textures/entity/horseshoes_brass.png"),
            HollowFrontier.id("textures/entity/horseshoes_starsteel.png")};

    @SubscribeEvent
    static void onRenderStates(RegisterRenderStateModifiersEvent event) {
        event.registerEntityModifier(new TypeToken<AbstractHorseRenderer<AbstractHorse, EquineRenderState, ?>>() {},
                (horse, state) -> state.setRenderData(SHOD, horse.getData(ModAttachments.HORSESHOE_TIER.get())));
        event.registerAvatarEntityModifier(new AvatarRenderStateModifier() {
            @Override
            public <T extends Avatar & ClientAvatarEntity> void accept(T avatar, AvatarRenderState state) {
                boolean winding = avatar.isUsingItem() && avatar.getUseItem().getItem() instanceof LassoItem;
                HumanoidArm arm = avatar.getUsedItemHand() == net.minecraft.world.InteractionHand.MAIN_HAND ? avatar.getMainArm() : avatar.getMainArm().getOpposite();
                state.setRenderData(WINDING, winding ? arm : null);
            }
        });
    }

    @SubscribeEvent
    @SuppressWarnings({"unchecked", "rawtypes"})
    static void onAddLayers(EntityRenderersEvent.AddLayers event) {
        for (EntityType<?> type : event.getEntityTypes()) {
            EntityRenderer<?, ?> renderer = event.getRenderer((EntityType) type);
            if (renderer instanceof AbstractHorseRenderer horses) {
                horses.addLayer(new Horseshoes(horses));
            }
        }
        for (PlayerModelType skin : event.getSkins()) {
            AvatarRenderer<?> players = event.getPlayerRenderer(skin);
            if (players != null) {
                ((LivingEntityRenderer) players).addLayer(new LassoWhirl((RenderLayerParent) players));
            }
        }
    }

    /** The shoes: the horse's own model drawn again in a sheet that is clear but for the feet. Foals aren't shod. */
    static final class Horseshoes<S extends EquineRenderState, M extends EntityModel<? super S>> extends RenderLayer<S, M> {
        Horseshoes(RenderLayerParent<S, M> renderer) {
            super(renderer);
        }

        @Override
        public void submit(PoseStack poseStack, SubmitNodeCollector collector, int light, S state, float yRot, float xRot) {
            Integer tier = state.getRenderData(SHOD);
            if (tier == null || tier <= 0 || state.isBaby || state.isInvisible) {
                return;
            }
            Identifier shoes = SHOES[Math.min(tier, SHOES.length) - 1];
            collector.order(2).submitModel(getParentModel(), state, poseStack, RenderTypes.entityCutoutZOffset(shoes), light,
                    LivingEntityRenderer.getOverlayCoords(state, 0.0F), state.outlineColor);
        }
    }

    /** The loop over a player's head while they wind up. Model space: a block is 1, up is -y, the head's top is at -0.5. */
    static final class LassoWhirl extends RenderLayer<AvatarRenderState, PlayerModel> {
        LassoWhirl(RenderLayerParent<AvatarRenderState, PlayerModel> renderer) {
            super(renderer);
        }

        @Override
        public void submit(PoseStack poseStack, SubmitNodeCollector collector, int light, AvatarRenderState state, float yRot, float xRot) {
            HumanoidArm arm = state.getRenderData(WINDING);
            if (arm == null || state.isInvisible) {
                return;
            }
            double turn = -state.ageInTicks * LassoRenderer.SPIN;
            Vec3 centre = new Vec3(0, -1.25, 0);
            // The raised hand: beside the head, on the winding arm's side (the model's right arm is at -x)
            Vec3 hand = new Vec3(arm == HumanoidArm.RIGHT ? -0.42 : 0.42, -0.72, -0.05);
            collector.submitCustomGeometry(poseStack, RenderTypes.entityCutout(RopeDraw.ROPE), (pose, buffer) -> {
                RopeDraw.loop(pose, buffer, light, centre, 0.62, 0.32, turn);
                RopeDraw.line(pose, buffer, light, hand, RopeDraw.knot(centre, 0.62, 0.32, turn), -0.03);
            });
        }
    }
}
