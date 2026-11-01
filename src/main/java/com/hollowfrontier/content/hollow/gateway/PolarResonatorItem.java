package com.hollowfrontier.content.hollow.gateway;

import java.util.List;
import java.util.Optional;
import java.util.function.Consumer;

import com.hollowfrontier.content.steam.SteamCanisterItem;
import com.hollowfrontier.content.steam.airship.Airship;
import com.hollowfrontier.registry.ModCriteria;
import com.hollowfrontier.registry.ModDataComponents;
import com.hollowfrontier.registry.ModFluids;

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

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.TooltipDisplay;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.transfer.ResourceHandler;
import net.neoforged.neoforge.transfer.fluid.FluidResource;
import net.neoforged.neoforge.transfer.transaction.Transaction;

/**
 * A steam-driven tuning fork the size of a coffee grinder. Use it on any stone of a Polar Gateway's ring: it spends
 * {@link #OPEN_STEAM} of steam, and the ring wakes stone by stone and opens the well a couple of seconds later
 * (GatewayEffects). Sneak-use on an open gateway to shut it again (free). This is what keeps the Hollow Earth behind the
 * steam age.
 * <p>
 * Since 32c it carries one opening's worth of steam itself, charged like a canister (use it on a boiler, tank or pipe),
 * and the steam for an opening comes from its own charge first, then canisters you carry, then your airship if it's
 * within {@link #AIRSHIP_REACH} blocks (a playtester reached a gateway with an airship full of steam and no canister).
 */
public class PolarResonatorItem extends Item implements GeoItem {
    public static final int OPEN_STEAM = 2_000;
    /** It holds one opening's worth. */
    public static final int CAPACITY = OPEN_STEAM;
    public static final int AIRSHIP_REACH = 16;
    /** How long it rings after waking a ring (ticks): as long as the ring takes to wake. */
    public static final int RING_TICKS = GatewayEffects.WAKE_TICKS;
    // Render-state data the client renderer fills in from the stack: ticks since it last woke a ring, or -1.
    public static final DataTicket<Double> SINCE_RESONATED = DataTickets.create("hollowfrontier_since_resonated", Double.class);

    private static final RawAnimation IDLE = RawAnimation.begin().thenLoop("idle");
    private static final RawAnimation RING = RawAnimation.begin().thenPlay("strike").thenLoop("ring");
    private final AnimatableInstanceCache geoCache = GeckoLibUtil.createInstanceCache(this);

    public PolarResonatorItem(Properties properties) {
        super(properties);
    }

    public static int charge(ItemStack stack) {
        return stack.getOrDefault(ModDataComponents.STEAM.get(), 0);
    }

    public static void setCharge(ItemStack stack, int amount) {
        stack.set(ModDataComponents.STEAM.get(), Math.max(0, Math.min(CAPACITY, amount)));
    }

    @Override
    public InteractionResult useOn(UseOnContext context) {
        Level level = context.getLevel();
        if (!(level.getBlockState(context.getClickedPos()).getBlock() instanceof GatewayFrameBlock)) {
            return charge(context);
        }
        Player player = context.getPlayer();
        if (!(level instanceof ServerLevel server) || player == null) {
            return InteractionResult.SUCCESS;
        }
        Optional<BlockPos> ring = PolarGateways.ringCenter(level, context.getClickedPos());
        if (ring.isEmpty()) {
            player.sendOverlayMessage(Component.translatable("message.hollowfrontier.polar_resonator.broken_ring"));
            return InteractionResult.FAIL;
        }
        BlockPos center = ring.get();
        if (GatewayEffects.isWaking(level, center)) {
            return InteractionResult.FAIL; // already ringing
        }
        if (PolarGateways.isOpen(level, center)) {
            if (player.isSecondaryUseActive()) {
                PolarGateways.close(server, center);
                player.sendOverlayMessage(Component.translatable("message.hollowfrontier.polar_resonator.closed"));
                return InteractionResult.SUCCESS;
            }
            player.sendOverlayMessage(Component.translatable("message.hollowfrontier.polar_resonator.already_open"));
            return InteractionResult.FAIL;
        }
        if (!PolarGateways.wellClear(level, center)) {
            player.sendOverlayMessage(Component.translatable("message.hollowfrontier.polar_resonator.blocked"));
            return InteractionResult.FAIL;
        }
        if (!spendSteam(server, player, context.getItemInHand(), OPEN_STEAM)) {
            player.sendOverlayMessage(Component.translatable("message.hollowfrontier.polar_resonator.no_steam", OPEN_STEAM, AIRSHIP_REACH));
            return InteractionResult.FAIL;
        }
        GatewayEffects.awaken(server, center);
        context.getItemInHand().set(ModDataComponents.RESONATED.get(), level.getGameTime()); // it rings while the ring wakes
        player.sendOverlayMessage(Component.translatable("message.hollowfrontier.polar_resonator.opened"));
        ModCriteria.fire(player, ModCriteria.GATEWAY_OPENED);
        return InteractionResult.SUCCESS;
    }

    // Charging, like a canister: from any steam holder (a boiler, tank or pipe) clicked on
    private static InteractionResult charge(UseOnContext context) {
        Level level = context.getLevel();
        ItemStack stack = context.getItemInHand();
        int space = CAPACITY - charge(stack);
        ResourceHandler<FluidResource> handler = level.getCapability(Capabilities.Fluid.BLOCK, context.getClickedPos(), context.getClickedFace());
        if (handler == null || space <= 0) {
            return InteractionResult.PASS;
        }
        if (!level.isClientSide()) {
            try (Transaction tx = Transaction.openRoot()) {
                int drained = ModFluids.extractSteam(handler, space, tx);
                if (drained > 0) {
                    tx.commit();
                    setCharge(stack, charge(stack) + drained);
                    level.playSound(null, context.getClickedPos(), SoundEvents.BUCKET_FILL, SoundSource.BLOCKS, 0.6F, 1.6F);
                }
            }
        }
        return InteractionResult.SUCCESS;
    }

    /** The player's airships within reach (theirs, or nobody's yet), the fullest first. */
    static List<Airship> airshipsNear(ServerLevel level, Player player) {
        return level.getEntitiesOfClass(Airship.class, new AABB(player.blockPosition()).inflate(AIRSHIP_REACH),
                        ship -> ship.isAlive() && (ship.owner() == null || ship.owner().equals(player.getUUID())))
                .stream().sorted(java.util.Comparator.comparingInt(Airship::steam).reversed()).toList();
    }

    /**
     * Takes {@code amount} of steam for an opening, if it can all be had: from the resonator's own charge, then canisters
     * carried, then the player's airships within reach. Takes nothing if there isn't enough.
     */
    public static boolean spendSteam(ServerLevel level, Player player, ItemStack resonator, int amount) {
        if (player.hasInfiniteMaterials()) {
            return true;
        }
        int own = charge(resonator);
        int carried = 0;
        for (int slot = 0; slot < player.getInventory().getContainerSize(); slot++) {
            ItemStack stack = player.getInventory().getItem(slot);
            if (stack.getItem() instanceof SteamCanisterItem) {
                carried += SteamCanisterItem.steam(stack);
            }
        }
        List<Airship> ships = airshipsNear(level, player);
        int aboard = ships.stream().mapToInt(Airship::steam).sum();
        if (own + carried + aboard < amount) {
            return false;
        }
        int left = amount;
        int fromCharge = Math.min(own, left);
        setCharge(resonator, own - fromCharge);
        left -= fromCharge;
        left -= SteamCanisterItem.drawFromInventory(player, left);
        for (Airship ship : ships) {
            int take = Math.min(ship.steam(), left);
            ship.setSteam(ship.steam() - take);
            left -= take;
        }
        return true;
    }

    @Override
    public boolean isBarVisible(ItemStack stack) {
        return true;
    }

    @Override
    public int getBarWidth(ItemStack stack) {
        return Math.round(13.0F * charge(stack) / CAPACITY);
    }

    @Override
    public int getBarColor(ItemStack stack) {
        return 0xD8D8E0;
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, TooltipDisplay display, Consumer<Component> builder, TooltipFlag flag) {
        builder.accept(Component.translatable("tooltip.hollowfrontier.steam", charge(stack), CAPACITY).withStyle(ChatFormatting.GRAY));
        builder.accept(Component.translatable("tooltip.hollowfrontier.polar_resonator", OPEN_STEAM).withStyle(ChatFormatting.GRAY));
        builder.accept(Component.translatable("tooltip.hollowfrontier.polar_resonator.steam", AIRSHIP_REACH).withStyle(ChatFormatting.GRAY));
    }

    // ---------------------------------------------------------------- GeckoLib (held, it's a 3D model; its icon is flat)

    @Override
    public void inventoryTick(ItemStack stack, ServerLevel level, Entity owner, @Nullable EquipmentSlot slot) {
        GeoItem.getOrAssignId(stack, level);
    }

    // Ringing, charging or spending steam changes the stack; none of that should drop it out of the hand and back, as a
    // new item would
    @Override
    public boolean shouldCauseReequipAnimation(ItemStack oldStack, ItemStack newStack, boolean slotChanged) {
        return slotChanged || !ItemStack.isSameItem(oldStack, newStack);
    }

    @Override
    public void createGeoRenderer(Consumer<GeoRenderProvider> consumer) {
        consumer.accept(new GeoRenderProvider() {
            private com.hollowfrontier.client.ResonatorRenderer renderer;

            @Override
            public com.geckolib.renderer.GeoItemRenderer<?> getGeoItemRenderer() {
                if (renderer == null) {
                    renderer = new com.hollowfrontier.client.ResonatorRenderer(PolarResonatorItem.this);
                }
                return renderer;
            }
        });
    }

    // It strikes and rings for as long as the ring takes to wake, then idles
    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
        controllers.add(new AnimationController<PolarResonatorItem>("resonator", 2, test -> {
            double since = test.getDataOrDefault(SINCE_RESONATED, -1.0);
            return test.setAndContinue(since >= 0 && since < RING_TICKS ? RING : IDLE);
        }));
    }

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return geoCache;
    }
}
