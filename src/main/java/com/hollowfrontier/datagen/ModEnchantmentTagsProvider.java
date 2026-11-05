package com.hollowfrontier.datagen;

import java.util.concurrent.CompletableFuture;

import com.hollowfrontier.HollowFrontier;
import com.hollowfrontier.registry.ModEnchantments;
import com.hollowfrontier.registry.ModTags;

import net.minecraft.core.HolderLookup;
import net.minecraft.core.registries.Registries;
import net.minecraft.data.PackOutput;
import net.minecraft.data.tags.TagsProvider;
import net.minecraft.tags.EnchantmentTags;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.Enchantments;

public class ModEnchantmentTagsProvider extends TagsProvider<Enchantment> {
    public ModEnchantmentTagsProvider(PackOutput output, CompletableFuture<HolderLookup.Provider> lookupProvider) {
        super(output, Registries.ENCHANTMENT, lookupProvider, HollowFrontier.MODID);
    }

    @Override
    protected void addTags(HolderLookup.Provider registries) {
        // Ordinary enchantments: vanilla's non_treasure puts them on the table, in librarians' trades and in random loot.
        tag(EnchantmentTags.NON_TREASURE).add(ModEnchantments.RIFLING, ModEnchantments.QUICK_LOAD, ModEnchantments.DEADEYE,
                ModEnchantments.CHOKE, ModEnchantments.DRY_POWDER, ModEnchantments.FRUGAL, ModEnchantments.FOCUS);
        // Vanilla Infinity is a gun enchantment too (GunItem lets the black powder guns take it), so camp loot and the blacksmith can turn it up.
        // Focus stays out: this tag is what the frontier knows, and no frontier smith has seen a phlogiston gun.
        tag(ModTags.Enchantments.GUN).add(ModEnchantments.RIFLING, ModEnchantments.QUICK_LOAD, ModEnchantments.DEADEYE,
                ModEnchantments.CHOKE, ModEnchantments.DRY_POWDER, ModEnchantments.FRUGAL, Enchantments.INFINITY);
        tag(ModTags.Enchantments.EXCLUSIVE_SET_AMMO).add(Enchantments.INFINITY);
    }
}
