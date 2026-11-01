package com.hollowfrontier.datagen;

import java.util.function.BiConsumer;

import com.hollowfrontier.HollowFrontier;
import com.hollowfrontier.content.frontier.gear.ModArmorMaterials;
import com.hollowfrontier.content.hollow.gear.StarsteelGear;
import com.hollowfrontier.content.concavia.Artifacts;
import com.hollowfrontier.content.hollow.megafauna.MegafaunaGear;

import net.minecraft.client.data.models.EquipmentAssetProvider;
import net.minecraft.client.resources.model.EquipmentClientInfo;
import net.minecraft.data.PackOutput;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.equipment.EquipmentAsset;

// Worn-armor layers (assets/<ns>/equipment/*.json). Textures: textures/entity/equipment/<layer>/<name>.png.
public class ModEquipmentAssetProvider extends EquipmentAssetProvider {
    public ModEquipmentAssetProvider(PackOutput output) {
        super(output);
    }

    @Override
    protected void registerModels(BiConsumer<ResourceKey<EquipmentAsset>, EquipmentClientInfo> output) {
        output.accept(ModArmorMaterials.COWBOY_ASSET, EquipmentClientInfo.builder().addHumanoidLayers(HollowFrontier.id("cowboy")).build());
        output.accept(ModArmorMaterials.SNAKESKIN_ASSET, EquipmentClientInfo.builder().addHumanoidLayers(HollowFrontier.id("snakeskin")).build());
        output.accept(StarsteelGear.ASSET, EquipmentClientInfo.builder().addHumanoidLayers(HollowFrontier.id("starsteel")).build());
        output.accept(Artifacts.CROWN_ASSET, EquipmentClientInfo.builder().addHumanoidLayers(HollowFrontier.id("starsteel_crown")).build());
        output.accept(MegafaunaGear.SHELL_ASSET, EquipmentClientInfo.builder().addHumanoidLayers(HollowFrontier.id("glyptodon_shell")).build());
        output.accept(MegafaunaGear.BARDING_ASSET, EquipmentClientInfo.builder()
                .addLayers(EquipmentClientInfo.LayerType.HORSE_BODY, new EquipmentClientInfo.Layer(HollowFrontier.id("hide_barding"))).build());
    }
}
