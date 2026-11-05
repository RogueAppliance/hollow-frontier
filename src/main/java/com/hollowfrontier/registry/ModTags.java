package com.hollowfrontier.registry;

import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.tags.BlockItemTagId;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.material.Fluid;

// Convention (c:) tags for our materials, so other mods' recipes accept them.
public final class ModTags {
    private ModTags() {}

    public static final class BlockItems {
        public static final BlockItemTagId STORAGE_BLOCKS_BRASS = common("storage_blocks/brass");
        // The Hollow Earth's ores (9c)
        public static final BlockItemTagId ORES_STARSTEEL = common("ores/starsteel");
        public static final BlockItemTagId ORES_PHLOGISTON = common("ores/phlogiston");
        public static final BlockItemTagId STORAGE_BLOCKS_RAW_STARSTEEL = common("storage_blocks/raw_starsteel");
        public static final BlockItemTagId STORAGE_BLOCKS_STARSTEEL = common("storage_blocks/starsteel");

        private static BlockItemTagId common(String path) {
            Identifier id = Identifier.fromNamespaceAndPath("c", path);
            return BlockItemTagId.create(id, id);
        }
    }

    public static final class Blocks {
        /** What the starsteel paxel mines: anything a pickaxe, an axe or a shovel does. */
        public static final TagKey<Block> MINEABLE_WITH_PAXEL = TagKey.create(Registries.BLOCK, com.hollowfrontier.HollowFrontier.id("mineable/paxel"));
    }

    public static final class Items {
        public static final TagKey<Item> INGOTS_BRASS = common("ingots/brass");
        public static final TagKey<Item> NUGGETS_BRASS = common("nuggets/brass");
        public static final TagKey<Item> PLATES = common("plates");
        public static final TagKey<Item> PLATES_BRASS = common("plates/brass");
        public static final TagKey<Item> GEARS = common("gears");
        public static final TagKey<Item> GEARS_BRASS = common("gears/brass");
        public static final TagKey<Item> DUSTS = common("dusts");
        public static final TagKey<Item> DUSTS_IRON = common("dusts/iron");
        public static final TagKey<Item> DUSTS_GOLD = common("dusts/gold");
        public static final TagKey<Item> DUSTS_COPPER = common("dusts/copper");
        public static final TagKey<Item> RAW_MATERIALS_STARSTEEL = common("raw_materials/starsteel");
        public static final TagKey<Item> INGOTS_STARSTEEL = common("ingots/starsteel");
        public static final TagKey<Item> NUGGETS_STARSTEEL = common("nuggets/starsteel");
        public static final TagKey<Item> DUSTS_STARSTEEL = common("dusts/starsteel");
        public static final TagKey<Item> GEMS_PHLOGISTON = common("gems/phlogiston");
        /** What repairs starsteel tools and armor at an anvil (our own tag, as vanilla has one per material). */
        public static final TagKey<Item> REPAIRS_STARSTEEL = TagKey.create(Registries.ITEM, com.hollowfrontier.HollowFrontier.id("repairs_starsteel"));
        /** Starsteel gear, which mends itself in the sun (StarsteelGear). */
        public static final TagKey<Item> STARSTEEL_GEAR = TagKey.create(Registries.ITEM, com.hollowfrontier.HollowFrontier.id("starsteel_gear"));
        // The megafauna's drops (10a): what repairs their gear, and what a Talon Knife skins an extra one of
        public static final TagKey<Item> TALONS = TagKey.create(Registries.ITEM, com.hollowfrontier.HollowFrontier.id("talons"));
        public static final TagKey<Item> SCUTES_GLYPTODON = TagKey.create(Registries.ITEM, com.hollowfrontier.HollowFrontier.id("glyptodon_scutes"));
        public static final TagKey<Item> HIDES_THICK = TagKey.create(Registries.ITEM, com.hollowfrontier.HollowFrontier.id("thick_hides"));
        public static final TagKey<Item> SKINNINGS = TagKey.create(Registries.ITEM, com.hollowfrontier.HollowFrontier.id("skinnings"));
        public static final TagKey<Item> GLYPTODON_SHELL_ARMOR = TagKey.create(Registries.ITEM, com.hollowfrontier.HollowFrontier.id("glyptodon_shell_armor"));
        /** The Grays' things, which the men in black buy back and mind you carrying (11b). */
        public static final TagKey<Item> STRANGE_GOODS = TagKey.create(Registries.ITEM, com.hollowfrontier.HollowFrontier.id("strange_goods"));
        // What takes the gun enchantments (22): every gun (Deadeye), the black powder guns (the rest), the shotgun (Choke), the phlogiston guns (Focus, 33)
        public static final TagKey<Item> ENCHANTABLE_GUN = TagKey.create(Registries.ITEM, com.hollowfrontier.HollowFrontier.id("enchantable/gun"));
        public static final TagKey<Item> ENCHANTABLE_MUZZLELOADER = TagKey.create(Registries.ITEM, com.hollowfrontier.HollowFrontier.id("enchantable/muzzleloader"));
        public static final TagKey<Item> ENCHANTABLE_SHOTGUN = TagKey.create(Registries.ITEM, com.hollowfrontier.HollowFrontier.id("enchantable/shotgun"));
        public static final TagKey<Item> ENCHANTABLE_PHLOGISTON_GUN = TagKey.create(Registries.ITEM, com.hollowfrontier.HollowFrontier.id("enchantable/phlogiston_gun"));
        // Curios slots (optional compat, phase 14): plain tags, harmless without Curios
        public static final TagKey<Item> CURIOS_CHARM = TagKey.create(Registries.ITEM, Identifier.fromNamespaceAndPath("curios", "charm"));
        public static final TagKey<Item> CURIOS_NECKLACE = TagKey.create(Registries.ITEM, Identifier.fromNamespaceAndPath("curios", "necklace"));

        private static TagKey<Item> common(String path) {
            return TagKey.create(Registries.ITEM, Identifier.fromNamespaceAndPath("c", path));
        }
    }

    public static final class Fluids {
        /** Any mod's steam (30): our tanks take it in as our own, and our pulls draw it from other mods' tanks. */
        public static final TagKey<Fluid> STEAM = TagKey.create(Registries.FLUID, Identifier.fromNamespaceAndPath("c", "steam"));
    }

    public static final class Enchantments {
        /** The gun enchantments (22): what outlaw camps, blacksmiths' chests and the town blacksmith's book trade draw from. */
        public static final TagKey<Enchantment> GUN = TagKey.create(Registries.ENCHANTMENT, com.hollowfrontier.HollowFrontier.id("gun"));
        /** What Frugal won't go with: Infinity, which already spends no ammo. */
        public static final TagKey<Enchantment> EXCLUSIVE_SET_AMMO = TagKey.create(Registries.ENCHANTMENT, com.hollowfrontier.HollowFrontier.id("exclusive_set/ammo"));
    }
}
