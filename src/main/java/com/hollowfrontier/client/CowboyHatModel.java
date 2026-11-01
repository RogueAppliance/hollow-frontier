package com.hollowfrontier.client;

import java.util.Map;
import java.util.WeakHashMap;

import com.hollowfrontier.HollowFrontier;
import com.hollowfrontier.registry.ModItems;

import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.Model;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeDeformation;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.model.geom.builders.PartDefinition;
import net.minecraft.client.renderer.entity.state.HumanoidRenderState;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.resources.model.EquipmentClientInfo;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.ItemStack;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.extensions.common.IClientItemExtensions;
import net.neoforged.neoforge.client.extensions.common.RegisterClientExtensionsEvent;

/**
 * The worn Cowboy Hat as real geometry instead of the vanilla helmet box: a tall crown pinched down the middle (two lobes
 * with a 1px crease between them), a dark band, and a wide brim whose sides curl up. It stands in for whichever humanoid
 * armor model would have drawn the helmet (player, armor stand, zombie...) and copies that model's head pose each frame,
 * so it follows head turns, sneaking and armor stand poses. Only the adult layer is replaced; babies keep the helmet box.
 * <p>
 * Head space: the head is x -4..4, y -8..0, z -4..4 (front at -z). Texture (64x64, its own file so the duster/boots layer
 * stays vanilla-shaped): crown 8x4x8 @0,0 | crown lobe 3x1x6 @32,0 | brim 10x1x16 @0,12 | curled brim side 3x1x14 @0,30.
 */
public class CowboyHatModel<S extends HumanoidRenderState> extends Model<S> {
    public static final Identifier TEXTURE = HollowFrontier.id("textures/entity/equipment/humanoid/cowboy_hat.png");

    private static ModelPart baked;
    // One wrapper per armor model it stands in for; renderers (and their models) are rebuilt on resource reload.
    private static final Map<HumanoidModel<?>, CowboyHatModel<?>> WRAPPERS = new WeakHashMap<>();

    private final HumanoidModel<S> wearer;
    private final ModelPart head;

    private CowboyHatModel(ModelPart root, HumanoidModel<S> wearer) {
        super(root, RenderTypes::armorCutoutNoCull);
        this.wearer = wearer;
        this.head = root.getChild("head");
    }

    @Override
    public void setupAnim(S state) {
        // Rendering is deferred, so pose the wearer's model for this state now and take its head from it.
        wearer.setupAnim(state);
        root.loadPose(wearer.root().storePose());
        head.loadPose(wearer.head.storePose());
        head.visible = wearer.head.visible;
    }

    public static LayerDefinition createLayer() {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition head = mesh.getRoot().addOrReplaceChild("head", CubeListBuilder.create(), PartPose.ZERO);
        // Crown x/z ±4.75, y -10.75..-5.25 (clears the skin's hat layer); the lobes rise 0.75 above it either side of the crease.
        head.addOrReplaceChild("crown", CubeListBuilder.create()
                .texOffs(0, 0).addBox(-4.0F, -10.0F, -4.0F, 8.0F, 4.0F, 8.0F, new CubeDeformation(0.75F))
                .texOffs(32, 0).addBox(-4.0F, -11.0F, -3.0F, 3.0F, 1.0F, 6.0F, new CubeDeformation(0.5F))
                .texOffs(32, 0).mirror().addBox(1.0F, -11.0F, -3.0F, 3.0F, 1.0F, 6.0F, new CubeDeformation(0.5F)), PartPose.ZERO);
        // Brim at forehead height, just above the eyes: flat front to back, 0.6 thick, x -5..5, z -8..8.
        CubeDeformation thin = new CubeDeformation(0.0F, -0.2F, 0.0F);
        head.addOrReplaceChild("brim", CubeListBuilder.create()
                .texOffs(0, 12).addBox(-5.0F, -6.0F, -8.0F, 10.0F, 1.0F, 16.0F, thin), PartPose.ZERO);
        // The sides hinge on the brim's edges and curl up about 30 degrees.
        head.addOrReplaceChild("brim_right", CubeListBuilder.create()
                .texOffs(0, 30).addBox(-3.0F, -0.5F, -7.0F, 3.0F, 1.0F, 14.0F, thin), PartPose.offsetAndRotation(-5.0F, -5.5F, 0.0F, 0.0F, 0.0F, 0.55F));
        head.addOrReplaceChild("brim_left", CubeListBuilder.create()
                .texOffs(0, 30).mirror().addBox(0.0F, -0.5F, -7.0F, 3.0F, 1.0F, 14.0F, thin), PartPose.offsetAndRotation(5.0F, -5.5F, 0.0F, 0.0F, 0.0F, -0.55F));
        return LayerDefinition.create(mesh, 64, 64);
    }

    @SuppressWarnings("unchecked")
    private static Model<?> wrap(HumanoidModel<?> wearer) {
        if (baked == null) baked = createLayer().bakeRoot();
        return WRAPPERS.computeIfAbsent(wearer, w -> new CowboyHatModel<>(baked, (HumanoidModel<HumanoidRenderState>) w));
    }

    private static boolean replaced(EquipmentClientInfo.LayerType layerType) {
        return layerType == EquipmentClientInfo.LayerType.HUMANOID;
    }

    private static final IClientItemExtensions HAT = new IClientItemExtensions() {
        @Override
        public Model getGenericArmorModel(ItemStack stack, EquipmentClientInfo.LayerType layerType, Model original) {
            return replaced(layerType) && original instanceof HumanoidModel<?> humanoid ? wrap(humanoid) : original;
        }

        @Override
        public Identifier getArmorTexture(ItemStack stack, EquipmentClientInfo.LayerType type, EquipmentClientInfo.Layer layer, Identifier _default) {
            return replaced(type) ? TEXTURE : null;
        }
    };

    @EventBusSubscriber(modid = HollowFrontier.MODID, value = Dist.CLIENT)
    static final class Extensions {
        private Extensions() {}

        @SubscribeEvent
        static void register(RegisterClientExtensionsEvent event) {
            event.registerItem(HAT, ModItems.COWBOY_HAT.get());
        }
    }
}
