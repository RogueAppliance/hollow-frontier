package com.hollowfrontier.client;

import com.hollowfrontier.HollowFrontier;
import com.hollowfrontier.registry.ModItems;

import net.minecraft.client.model.HumanoidModel;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.extensions.common.IClientItemExtensions;
import net.neoforged.neoforge.client.extensions.common.RegisterClientExtensionsEvent;

/**
 * Players hold the muzzleloaders up in both hands (the crossbow aiming pose); the guns' third-person display
 * transforms in ModModelProvider assume that raised arm.
 */
@EventBusSubscriber(modid = HollowFrontier.MODID, value = Dist.CLIENT)
public final class GunClientExtensions {
    private GunClientExtensions() {}

    private static final IClientItemExtensions AIMED = new IClientItemExtensions() {
        @Override
        public HumanoidModel.ArmPose getArmPose(LivingEntity entity, InteractionHand hand, ItemStack stack) {
            return HumanoidModel.ArmPose.CROSSBOW_HOLD;
        }
    };

    @SubscribeEvent
    static void register(RegisterClientExtensionsEvent event) {
        event.registerItem(AIMED, ModItems.REVOLVER.get(), ModItems.PLAINS_RIFLE.get(), ModItems.SHOTGUN.get(),
                ModItems.RAY_PISTOL.get(), ModItems.PHLOGISTON_RIFLE.get());
    }
}
