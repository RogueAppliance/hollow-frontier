package com.hollowfrontier.content.frontier.cat;

import java.util.ArrayList;
import java.util.List;

import com.hollowfrontier.content.frontier.cat.TownCat.Role;
import com.hollowfrontier.registry.ModItems;

import net.minecraft.core.component.DataComponents;
import net.minecraft.resources.ResourceKey;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.animal.feline.CatVariant;
import net.minecraft.world.entity.animal.feline.CatVariants;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.DyedItemColor;

/**
 * Every town cat weight in one place. A blueprint spot keeps its cat with the role's PRESENCE chance (so some lots have
 * none); coat and accessory then roll independently. Roughly: a long street ends up with 3-5 cats, a short one 2-4.
 * Mostly plain cats, plenty of bandanas, a few hats; the sheriff's cat always wears the town's one star; the undertaker's
 * ghost turns up in about one town in fifteen (the undertaker is an uncommon lot).
 */
public final class TownCatOdds {
    private TownCatOdds() {}

    /** Chance a blueprint spot keeps its cat. */
    public static float presence(Role role) {
        return switch (role) {
            case SALOON, SHERIFF -> 1.0F;
            case LIVERY -> 0.6F;
            case STORE -> 0.45F;
            case PORCH, SMITHY -> 0.3F;
            case HOME -> 0.25F;
            case GHOST -> 0.15F;
            case STRAY -> 0.0F;
        };
    }

    // Coat weights: every vanilla coat 6, the two frontier coats 6 each, plus a bonus where they belong.
    private static final List<ResourceKey<CatVariant>> VANILLA_COATS = List.of(CatVariants.TABBY, CatVariants.BLACK, CatVariants.RED,
            CatVariants.SIAMESE, CatVariants.BRITISH_SHORTHAIR, CatVariants.CALICO, CatVariants.PERSIAN, CatVariants.RAGDOLL,
            CatVariants.WHITE, CatVariants.JELLIE, CatVariants.ALL_BLACK);
    private static final int COAT_WEIGHT = 6;
    private static final int DESERT_TOWN_BONUS = 30; // desert, badlands and savanna towns
    private static final int SALOON_BONUS = 30;      // the saloon's own cat

    // Accessory weights: none / bandana (red or blue) / cowboy hat. The store cat favours a bandana.
    private static final int PLAIN = 60, BANDANA = 30, HAT = 10, STORE_BANDANA_BONUS = 30;

    public static ResourceKey<CatVariant> coat(Role role, boolean desertTown, RandomSource random) {
        List<ResourceKey<CatVariant>> coats = new ArrayList<>(VANILLA_COATS);
        List<Integer> weights = new ArrayList<>(VANILLA_COATS.stream().map(c -> COAT_WEIGHT).toList());
        coats.add(CatCoats.DESERT);
        weights.add(COAT_WEIGHT + (desertTown ? DESERT_TOWN_BONUS : 0));
        coats.add(CatCoats.SALOON);
        weights.add(COAT_WEIGHT + (role == Role.SALOON ? SALOON_BONUS : 0));
        int roll = random.nextInt(weights.stream().mapToInt(Integer::intValue).sum());
        for (int i = 0; i < coats.size(); i++) {
            roll -= weights.get(i);
            if (roll < 0) {
                return coats.get(i);
            }
        }
        return CatVariants.TABBY;
    }

    /** What the cat wears; empty for none. The ghost wears nothing, the sheriff's cat the brass star. */
    public static ItemStack accessory(Role role, RandomSource random) {
        if (role == Role.SHERIFF) {
            return new ItemStack(ModItems.BRASS_STAR.get());
        }
        if (role == Role.GHOST) {
            return ItemStack.EMPTY;
        }
        int bandana = BANDANA + (role == Role.STORE ? STORE_BANDANA_BONUS : 0);
        int roll = random.nextInt(PLAIN + bandana + HAT);
        if (roll < PLAIN) {
            return ItemStack.EMPTY;
        }
        if (roll < PLAIN + bandana) {
            return bandana(random.nextBoolean() ? DyeColor.RED : DyeColor.BLUE);
        }
        return new ItemStack(ModItems.COWBOY_HAT.get());
    }

    public static ItemStack bandana(DyeColor color) {
        ItemStack stack = new ItemStack(ModItems.BANDANA.get());
        stack.set(DataComponents.DYED_COLOR, new DyedItemColor(color.getTextureDiffuseColor() & 0xFFFFFF));
        return stack;
    }
}
