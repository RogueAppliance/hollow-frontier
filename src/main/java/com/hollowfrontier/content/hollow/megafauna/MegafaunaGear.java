package com.hollowfrontier.content.hollow.megafauna;

import java.util.Collection;
import java.util.EnumMap;
import java.util.Map;
import java.util.Optional;

import com.hollowfrontier.HollowFrontier;
import com.hollowfrontier.registry.ModItems;
import com.hollowfrontier.registry.ModTags;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ToolMaterial;
import net.minecraft.world.item.equipment.ArmorMaterial;
import net.minecraft.world.item.equipment.ArmorType;
import net.minecraft.world.item.equipment.EquipmentAsset;
import net.minecraft.world.item.equipment.EquipmentAssets;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.LivingDropsEvent;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;
import net.neoforged.neoforge.event.entity.player.ItemTooltipEvent;

/**
 * What the megafauna's drops make (10a), beyond the Oliphant ({@link OliphantItem}) and mammoth steaks.
 * <ul>
 * <li><b>Talon Knife</b> (a terror bird's talon on a stick): a quick, light blade, and a skinning knife: whatever you kill
 *     with it drops one more of its hide, tusk, talon, scute or snakeskin (anything in {@code hollowfrontier:skinnings}).</li>
 * <li><b>Glyptodon shell armor</b> (scutes): between iron and diamond, a little tough, a little knockback-proof. In the
 *     full set, sneaking shells you up: arrows, bullets and other projectiles do a quarter of their damage, explosions half.</li>
 * <li><b>Hide Barding</b> (megatherium hide): horse armor worth 9 (between gold and diamond), heavy enough to steady a horse
 *     against knockback.</li>
 * </ul>
 */
@EventBusSubscriber(modid = HollowFrontier.MODID)
public final class MegafaunaGear {
    private MegafaunaGear() {}

    public static final ToolMaterial TALON = new ToolMaterial(BlockTags.INCORRECT_FOR_IRON_TOOL, 320, 6.0F, 1.0F, 16, ModTags.Items.TALONS);

    public static final ResourceKey<EquipmentAsset> SHELL_ASSET = ResourceKey.create(EquipmentAssets.ROOT_ID, HollowFrontier.id("glyptodon_shell"));
    public static final ArmorMaterial SHELL = new ArmorMaterial(
            28, defense(3, 6, 7, 3, 0), 12, SoundEvents.ARMOR_EQUIP_TURTLE, 1.0F, 0.05F, ModTags.Items.SCUTES_GLYPTODON, SHELL_ASSET);

    public static final ResourceKey<EquipmentAsset> BARDING_ASSET = ResourceKey.create(EquipmentAssets.ROOT_ID, HollowFrontier.id("hide_barding"));
    public static final ArmorMaterial BARDING = new ArmorMaterial(
            16, defense(0, 0, 0, 0, 9), 10, SoundEvents.HORSE_ARMOR, 0.0F, 0.2F, ModTags.Items.HIDES_THICK, BARDING_ASSET);

    /** Projectile and explosion damage taken by a shelled-up wearer. */
    public static final float SHELL_PROJECTILE = 0.25F, SHELL_EXPLOSION = 0.5F;

    private static Map<ArmorType, Integer> defense(int boots, int legs, int chest, int helmet, int body) {
        Map<ArmorType, Integer> map = new EnumMap<>(ArmorType.class);
        map.put(ArmorType.BOOTS, boots);
        map.put(ArmorType.LEGGINGS, legs);
        map.put(ArmorType.CHESTPLATE, chest);
        map.put(ArmorType.HELMET, helmet);
        map.put(ArmorType.BODY, body);
        return map;
    }

    /** All four pieces of glyptodon shell armor. */
    public static boolean wearsFullShell(LivingEntity entity) {
        return entity.getItemBySlot(EquipmentSlot.HEAD).is(ModItems.GLYPTODON_HELMET.get())
                && entity.getItemBySlot(EquipmentSlot.CHEST).is(ModItems.GLYPTODON_CHESTPLATE.get())
                && entity.getItemBySlot(EquipmentSlot.LEGS).is(ModItems.GLYPTODON_LEGGINGS.get())
                && entity.getItemBySlot(EquipmentSlot.FEET).is(ModItems.GLYPTODON_BOOTS.get());
    }

    /** The share of {@code source}'s damage a shelled-up wearer takes (1 if the shell doesn't apply). */
    public static float shellFactor(LivingEntity entity, DamageSource source) {
        if (!entity.isCrouching() || !wearsFullShell(entity)) {
            return 1.0F;
        }
        if (source.is(DamageTypeTags.IS_PROJECTILE)) {
            return SHELL_PROJECTILE;
        }
        return source.is(DamageTypeTags.IS_EXPLOSION) ? SHELL_EXPLOSION : 1.0F;
    }

    @SubscribeEvent
    static void onDamage(LivingIncomingDamageEvent event) {
        float factor = shellFactor(event.getEntity(), event.getSource());
        if (factor < 1.0F) {
            event.setAmount(event.getAmount() * factor);
        }
    }

    /** Skinning: one more of the first skinnings-tagged drop, when the killer holds a Talon Knife. */
    @SubscribeEvent
    static void onDrops(LivingDropsEvent event) {
        if (event.getSource().getEntity() instanceof Player player) {
            skin(player, event.getDrops());
        }
    }

    /** Adds the Talon Knife's extra skinning to {@code drops} if {@code killer} holds one; returns whether it did. */
    public static boolean skin(Player killer, Collection<ItemEntity> drops) {
        if (!killer.getMainHandItem().is(ModItems.TALON_KNIFE.get())) {
            return false;
        }
        Optional<ItemStack> skinning = drops.stream().map(ItemEntity::getItem).filter(stack -> stack.is(ModTags.Items.SKINNINGS)).findFirst();
        skinning.ifPresent(stack -> stack.grow(1));
        return skinning.isPresent();
    }

    @SubscribeEvent
    static void onTooltip(ItemTooltipEvent event) {
        ItemStack stack = event.getItemStack();
        String key = null;
        if (stack.is(ModItems.TALON_KNIFE.get())) {
            key = "talon_knife";
        } else if (stack.is(ModItems.OLIPHANT.get())) {
            key = "oliphant";
        } else if (stack.is(ModItems.HIDE_BARDING.get())) {
            key = "hide_barding";
        } else if (stack.is(ModTags.Items.GLYPTODON_SHELL_ARMOR)) {
            key = "glyptodon_shell";
        }
        if (key != null) {
            event.getToolTip().add(Component.translatable("tooltip.hollowfrontier." + key).withStyle(ChatFormatting.GRAY));
        }
    }
}
