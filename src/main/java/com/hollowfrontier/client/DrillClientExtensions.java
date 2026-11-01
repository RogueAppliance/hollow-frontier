package com.hollowfrontier.client;

import com.hollowfrontier.HollowFrontier;
import com.hollowfrontier.registry.ModItems;
import com.mojang.blaze3d.vertex.PoseStack;

import net.minecraft.client.renderer.state.level.PlayerRenderState;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.item.ItemStack;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.extensions.common.IClientItemExtensions;
import net.neoforged.neoforge.client.extensions.common.RegisterClientExtensionsEvent;

/**
 * The Steam Drill in first person holds steady while it mines: only its bit turns (DrillRenderer). Vanilla would swing
 * the hand up and down with every stroke, as for a pickaxe; this puts the drill where the hand rests (raised in as it's
 * equipped, as usual) and skips the swing.
 */
@EventBusSubscriber(modid = HollowFrontier.MODID, value = Dist.CLIENT)
public final class DrillClientExtensions {
    private DrillClientExtensions() {}

    private static final IClientItemExtensions STEADY = new IClientItemExtensions() {
        @Override
        public boolean applyForgeHandTransform(PoseStack poseStack, PlayerRenderState player, HumanoidArm arm, ItemStack itemInHand,
                                               float partialTick, float equipProcess, float swingProcess) {
            int side = arm == HumanoidArm.RIGHT ? 1 : -1;
            poseStack.translate(side * 0.56F, -0.52F + equipProcess * -0.6F, -0.72F); // vanilla's resting hand, without the swing
            return true;
        }
    };

    @SubscribeEvent
    static void register(RegisterClientExtensionsEvent event) {
        event.registerItem(STEADY, ModItems.STEAM_DRILL.get());
    }
}
