package com.hollowfrontier.content.concavia;

import java.util.EnumMap;
import java.util.Map;

import com.hollowfrontier.HollowFrontier;
import com.hollowfrontier.registry.ModDamageTypes;
import com.hollowfrontier.registry.ModItems;
import com.hollowfrontier.registry.ModTags;

import net.minecraft.resources.ResourceKey;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.equipment.ArmorMaterial;
import net.minecraft.world.item.equipment.ArmorType;
import net.minecraft.world.item.equipment.EquipmentAsset;
import net.minecraft.world.item.equipment.EquipmentAssets;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;
import net.neoforged.neoforge.event.entity.living.MobEffectEvent;

/**
 * The Sun Archon's artifacts (12b). The <b>Starsteel Crown</b> is the last king's: a helmet a little better than
 * starsteel's (and sun-forged like it), whose wearer can't be withered and takes half the harm of any phlogiston beam. The
 * Sunstone Lens ({@link SunstoneLensItem}), the Concavian Codex ({@link ConcavianCodexItem}) and the Phlogiston Core
 * ({@link PhlogistonCoreItem}) are items of their own.
 */
@EventBusSubscriber(modid = HollowFrontier.MODID)
public final class Artifacts {
    private Artifacts() {}

    public static final ResourceKey<EquipmentAsset> CROWN_ASSET = ResourceKey.create(EquipmentAssets.ROOT_ID, HollowFrontier.id("starsteel_crown"));
    public static final ArmorMaterial CROWN = new ArmorMaterial(45, helmet(4), 25, SoundEvents.ARMOR_EQUIP_GOLD, 3.0F, 0.1F,
            ModTags.Items.REPAIRS_STARSTEEL, CROWN_ASSET);
    /** How much of a phlogiston beam's harm reaches the crown's wearer. */
    public static final float CROWN_BEAM_FACTOR = 0.5F;

    private static Map<ArmorType, Integer> helmet(int defense) {
        Map<ArmorType, Integer> map = new EnumMap<>(ArmorType.class);
        for (ArmorType type : ArmorType.values()) {
            map.put(type, type == ArmorType.HELMET ? defense : 0);
        }
        return map;
    }

    public static boolean crowned(LivingEntity entity) {
        return entity.getItemBySlot(EquipmentSlot.HEAD).is(ModItems.STARSTEEL_CROWN.get());
    }

    @SubscribeEvent
    static void onEffect(MobEffectEvent.Applicable event) {
        if (event.getEffectInstance().is(MobEffects.WITHER) && crowned(event.getEntity())) {
            event.setResult(MobEffectEvent.Applicable.Result.DO_NOT_APPLY);
        }
    }

    @SubscribeEvent
    static void onDamage(LivingIncomingDamageEvent event) {
        if (event.getSource().is(ModDamageTypes.PHLOGISTON_BEAM) && crowned(event.getEntity())) {
            event.setAmount(event.getAmount() * CROWN_BEAM_FACTOR);
        }
    }
}
