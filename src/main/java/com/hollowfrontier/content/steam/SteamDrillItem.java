package com.hollowfrontier.content.steam;

import java.util.List;
import java.util.function.Consumer;

import org.jspecify.annotations.Nullable;

import com.geckolib.animatable.GeoItem;
import com.geckolib.animatable.client.GeoRenderProvider;
import com.geckolib.animatable.instance.AnimatableInstanceCache;
import com.geckolib.animatable.manager.AnimatableManager;
import com.geckolib.animation.AnimationController;
import com.geckolib.animation.RawAnimation;
import com.geckolib.constant.DataTickets;
import com.geckolib.constant.dataticket.DataTicket;
import com.geckolib.util.GeckoLibUtil;
import com.hollowfrontier.HollowFrontier;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderGetter;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ToolMaterial;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.Tool;
import net.minecraft.world.item.component.TooltipDisplay;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;

/**
 * Pickaxe + shovel at iron tier. With steam (from canisters in the inventory) it mines at DRILL_SPEED and uses
 * STEAM_PER_BLOCK mB per block; without steam it still mines (and drops) but at bare-hand speed. Held, it's a GeckoLib model
 * whose bit spins while it drills (DrillRenderer); its icon is flat.
 */
@EventBusSubscriber(modid = HollowFrontier.MODID)
public class SteamDrillItem extends Item implements GeoItem {
    public static final int STEAM_PER_BLOCK = 25;
    private static final float DRILL_SPEED = 9.0F;
    // Render-state data the client renderer fills in: whether whoever holds it is drilling (with steam to do it).
    public static final DataTicket<Boolean> DRILLING = DataTickets.create("hollowfrontier_drilling", Boolean.class);

    private static final RawAnimation IDLE = RawAnimation.begin().thenLoop("idle");
    private static final RawAnimation SPIN = RawAnimation.begin().thenLoop("spin");
    private final AnimatableInstanceCache geoCache = GeckoLibUtil.createInstanceCache(this);

    public SteamDrillItem(Properties properties) {
        super(properties);
    }

    /** Iron-tier pickaxe attributes, with a tool rule set that also covers shovel blocks. */
    public static Properties drillProperties(Properties properties) {
        HolderGetter<Block> blocks = BuiltInRegistries.acquireBootstrapRegistrationLookup(BuiltInRegistries.BLOCK);
        return properties.pickaxe(ToolMaterial.IRON, 1.0F, -2.8F)
                .durability(750)
                .component(DataComponents.TOOL, new Tool(List.of(
                        Tool.Rule.deniesDrops(blocks.getOrThrow(ToolMaterial.IRON.incorrectBlocksForDrops())),
                        Tool.Rule.minesAndDrops(blocks.getOrThrow(BlockTags.MINEABLE_WITH_PICKAXE), DRILL_SPEED),
                        Tool.Rule.minesAndDrops(blocks.getOrThrow(BlockTags.MINEABLE_WITH_SHOVEL), DRILL_SPEED)),
                        1.0F, 1, true));
    }

    /** Unpowered drills mine at bare-hand speed (per player, so it can't live on the shared Item). */
    @SubscribeEvent
    static void onBreakSpeed(PlayerEvent.BreakSpeed event) {
        Player player = event.getEntity();
        if (player.getMainHandItem().getItem() instanceof SteamDrillItem && !SteamCanisterItem.hasSteam(player, STEAM_PER_BLOCK)) {
            event.setNewSpeed(Math.min(event.getNewSpeed(), 1.0F));
        }
    }

    @Override
    public boolean mineBlock(ItemStack stack, Level level, BlockState state, BlockPos pos, LivingEntity owner) {
        if (!level.isClientSide() && state.getDestroySpeed(level, pos) != 0.0F && owner instanceof Player player) {
            SteamCanisterItem.drawFromInventory(player, STEAM_PER_BLOCK);
        }
        return super.mineBlock(stack, level, state, pos, owner);
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, TooltipDisplay display, Consumer<Component> builder, TooltipFlag flag) {
        builder.accept(Component.translatable("tooltip.hollowfrontier.steam_drill", STEAM_PER_BLOCK).withStyle(ChatFormatting.GRAY));
    }

    // ---------------------------------------------------------------- GeckoLib (held, it's a 3D model; its icon is flat)

    @Override
    public void inventoryTick(ItemStack stack, ServerLevel level, Entity owner, @Nullable EquipmentSlot slot) {
        GeoItem.getOrAssignId(stack, level);
    }

    // Wear from each block changes the stack; that shouldn't drop it out of the hand and back, as a new item would
    @Override
    public boolean shouldCauseReequipAnimation(ItemStack oldStack, ItemStack newStack, boolean slotChanged) {
        return slotChanged || !ItemStack.isSameItem(oldStack, newStack);
    }

    @Override
    public void createGeoRenderer(Consumer<GeoRenderProvider> consumer) {
        consumer.accept(new GeoRenderProvider() {
            private com.hollowfrontier.client.DrillRenderer renderer;

            @Override
            public com.geckolib.renderer.GeoItemRenderer<?> getGeoItemRenderer() {
                if (renderer == null) {
                    renderer = new com.hollowfrontier.client.DrillRenderer(SteamDrillItem.this);
                }
                return renderer;
            }
        });
    }

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
        controllers.add(new AnimationController<SteamDrillItem>("drill", 2,
                test -> test.setAndContinue(test.getDataOrDefault(DRILLING, false) ? SPIN : IDLE)));
    }

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return geoCache;
    }
}
