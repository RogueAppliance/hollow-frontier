package com.hollowfrontier.content.hollow.gear;

import java.util.EnumMap;
import java.util.Map;

import com.hollowfrontier.HollowFrontier;
import com.hollowfrontier.registry.ModAttachments;
import com.hollowfrontier.registry.ModItems;
import com.hollowfrontier.registry.ModTags;
import com.hollowfrontier.worldgen.HollowEarth;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.animal.equine.AbstractHorse;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ToolMaterial;
import net.minecraft.world.item.equipment.ArmorMaterial;
import net.minecraft.world.item.equipment.ArmorType;
import net.minecraft.world.item.equipment.EquipmentAsset;
import net.minecraft.world.item.equipment.EquipmentAssets;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LightLayer;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;
import net.neoforged.neoforge.event.entity.player.ItemTooltipEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;

/**
 * Starsteel (9c), the Concavians' crimson-and-gold metal from deep under the Hollow Earth's floor (their own word for
 * it was Plato's orichalcum). Its gear is a sword, a paxel (PaxelItem) and the four pieces of armor.
 * <ul>
 * <li><b>Tier:</b> between diamond and netherite. It mines what diamond mines, lasts a little longer than diamond (netherite
 *     still outlasts it), is the fastest metal to dig with and takes enchantments as gold does. Its armor guards as well
 *     as diamond with more toughness; netherite stays the toughest and the only one that shrugs off knockback.</li>
 * <li><b>Perk, sun-forged:</b> starsteel gear mends itself in sunlight. Every five seconds each starsteel piece a player
 *     wears or holds gets a point back, while they stand in full sky light by day; in the Hollow Earth the inner sun
 *     never sets, so there it mends whenever the crust is all that's overhead.</li>
 * <li><b>Horseshoes</b> (tier 3): the fastest shoes, and a horse shod in starsteel takes no fall damage, nor does its rider.</li>
 * </ul>
 */
@EventBusSubscriber(modid = HollowFrontier.MODID)
public final class StarsteelGear {
    private StarsteelGear() {}

    public static final ToolMaterial TOOLS = new ToolMaterial(BlockTags.INCORRECT_FOR_DIAMOND_TOOL, 1800, 10.0F, 3.5F, 22, ModTags.Items.REPAIRS_STARSTEEL);

    public static final ResourceKey<EquipmentAsset> ASSET = ResourceKey.create(EquipmentAssets.ROOT_ID, HollowFrontier.id("starsteel"));
    public static final ArmorMaterial ARMOR = new ArmorMaterial(
            35, defense(3, 6, 8, 3), 22, SoundEvents.ARMOR_EQUIP_GOLD, 2.5F, 0.0F, ModTags.Items.REPAIRS_STARSTEEL, ASSET);

    /** How often sun-forged gear mends, and by how much. */
    public static final int MEND_TICKS = 100;
    public static final int MEND_AMOUNT = 1;

    private static Map<ArmorType, Integer> defense(int boots, int legs, int chest, int helmet) {
        Map<ArmorType, Integer> map = new EnumMap<>(ArmorType.class);
        map.put(ArmorType.BOOTS, boots);
        map.put(ArmorType.LEGGINGS, legs);
        map.put(ArmorType.CHESTPLATE, chest);
        map.put(ArmorType.HELMET, helmet);
        map.put(ArmorType.BODY, chest);
        return map;
    }

    /** Whether a player stands in the sun: full sky light, and day (always day under the inner sun). */
    public static boolean inSunlight(Player player) {
        Level level = player.level();
        boolean day = level.dimension() == HollowEarth.LEVEL || level.isBrightOutside();
        return day && level.getBrightness(LightLayer.SKY, player.blockPosition().above()) >= 15;
    }

    /** Mends each damaged starsteel piece the player wears or holds by {@link #MEND_AMOUNT}; returns how many it mended. */
    public static int mend(Player player) {
        int mended = 0;
        for (EquipmentSlot slot : EquipmentSlot.values()) {
            ItemStack stack = player.getItemBySlot(slot);
            if (stack.is(ModTags.Items.STARSTEEL_GEAR) && stack.isDamaged()) {
                stack.setDamageValue(Math.max(0, stack.getDamageValue() - MEND_AMOUNT));
                mended++;
            }
        }
        return mended;
    }

    @SubscribeEvent
    static void onPlayerTick(PlayerTickEvent.Post event) {
        Player player = event.getEntity();
        if (!player.level().isClientSide() && player.tickCount % MEND_TICKS == 0 && inSunlight(player)) {
            mend(player);
        }
    }

    @SubscribeEvent
    static void onTooltip(ItemTooltipEvent event) {
        ItemStack stack = event.getItemStack();
        if (stack.is(ModTags.Items.STARSTEEL_GEAR)) {
            if (stack.is(ModItems.STARSTEEL_PAXEL.get())) {
                event.getToolTip().add(Component.translatable("tooltip.hollowfrontier.paxel").withStyle(ChatFormatting.GRAY));
            }
            event.getToolTip().add(Component.translatable("tooltip.hollowfrontier.sun_forged").withStyle(ChatFormatting.GOLD));
        } else if (stack.is(ModItems.STARSTEEL_HORSESHOES.get())) {
            event.getToolTip().add(Component.translatable("tooltip.hollowfrontier.starsteel_horseshoes").withStyle(ChatFormatting.GOLD));
        }
    }

    /** Whether an entity is a horse shod in starsteel, or rides one. */
    public static boolean fallProof(Entity entity) {
        Entity mount = entity instanceof AbstractHorse ? entity : entity.getVehicle();
        return mount instanceof AbstractHorse horse && horse.getData(ModAttachments.HORSESHOE_TIER.get()) >= 3;
    }

    // Horses work out their own fall damage (no fall event), so the fall damage itself is turned away, for the horse and
    // whoever's in the saddle.
    @SubscribeEvent
    static void onDamage(LivingIncomingDamageEvent event) {
        if (event.getSource().is(DamageTypeTags.IS_FALL) && fallProof(event.getEntity())) {
            event.setCanceled(true);
        }
    }
}
