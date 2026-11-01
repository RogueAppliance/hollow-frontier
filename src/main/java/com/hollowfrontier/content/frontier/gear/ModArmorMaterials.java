package com.hollowfrontier.content.frontier.gear;

import java.util.EnumMap;
import java.util.Map;

import com.hollowfrontier.HollowFrontier;

import net.minecraft.resources.ResourceKey;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.item.equipment.ArmorMaterial;
import net.minecraft.world.item.equipment.ArmorType;
import net.minecraft.world.item.equipment.EquipmentAsset;
import net.minecraft.world.item.equipment.EquipmentAssets;

public final class ModArmorMaterials {
    private ModArmorMaterials() {}

    public static final ResourceKey<EquipmentAsset> COWBOY_ASSET = ResourceKey.create(EquipmentAssets.ROOT_ID, HollowFrontier.id("cowboy"));

    // Between leather (1/2/3/1) and chainmail (1/4/5/2); leather-repairable, very enchantable.
    public static final ArmorMaterial COWBOY = new ArmorMaterial(
            10, defense(2, 3, 4, 2), 15, SoundEvents.ARMOR_EQUIP_LEATHER, 0.0F, 0.0F, ItemTags.REPAIRS_LEATHER_ARMOR, COWBOY_ASSET);

    // Snakeskin Boots: cowboy stats, a little tougher, their own look. Leather-repairable like the rest of the set.
    public static final ResourceKey<EquipmentAsset> SNAKESKIN_ASSET = ResourceKey.create(EquipmentAssets.ROOT_ID, HollowFrontier.id("snakeskin"));
    public static final ArmorMaterial SNAKESKIN = new ArmorMaterial(
            13, defense(2, 3, 4, 2), 15, SoundEvents.ARMOR_EQUIP_LEATHER, 0.0F, 0.0F, ItemTags.REPAIRS_LEATHER_ARMOR, SNAKESKIN_ASSET);

    private static Map<ArmorType, Integer> defense(int boots, int legs, int chest, int helmet) {
        Map<ArmorType, Integer> map = new EnumMap<>(ArmorType.class);
        map.put(ArmorType.BOOTS, boots);
        map.put(ArmorType.LEGGINGS, legs);
        map.put(ArmorType.CHESTPLATE, chest);
        map.put(ArmorType.HELMET, helmet);
        map.put(ArmorType.BODY, chest);
        return map;
    }
}
