package com.hollowfrontier.datagen;

import java.util.concurrent.CompletableFuture;

import com.hollowfrontier.HollowFrontier;
import com.hollowfrontier.registry.ModItems;
import com.hollowfrontier.registry.ModTags;

import net.minecraft.core.HolderLookup;
import net.minecraft.data.PackOutput;
import net.minecraft.data.tags.TagsProvider;
import net.minecraft.tags.BlockItemTags;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Block;
import net.neoforged.neoforge.common.Tags;
import net.neoforged.neoforge.common.data.BlockTagCopyingItemTagProvider;

// Item tags; use copy(blockTag, itemTag) to mirror block tags (ores, storage blocks) onto their items.
public class ModItemTagsProvider extends BlockTagCopyingItemTagProvider {
    public ModItemTagsProvider(PackOutput output, CompletableFuture<HolderLookup.Provider> lookupProvider,
            CompletableFuture<TagsProvider.TagLookup<Block>> blockTags) {
        super(output, lookupProvider, blockTags, HollowFrontier.MODID);
    }

    @Override
    protected void addTags(HolderLookup.Provider registries) {
        copy(Tags.BlockItems.ORES);
        copy(Tags.BlockItems.ORES_IN_GROUND_STONE);
        copy(Tags.BlockItems.ORES_IN_GROUND_DEEPSLATE);
        copy(Tags.BlockItems.ORE_RATES_SINGULAR);
        copy(ModTags.BlockItems.STORAGE_BLOCKS_BRASS);
        copy(Tags.BlockItems.STORAGE_BLOCKS);
        copy(BlockItemTags.LEAVES);
        copy(BlockItemTags.STAIRS);
        copy(BlockItemTags.SLABS);
        copy(BlockItemTags.WALLS);

        // Worn in Curios slots when Curios is installed (CuriosCompat)
        tag(ModTags.Items.CURIOS_CHARM).add(ModItems.SUNSTONE_LENS.key());
        tag(ModTags.Items.CURIOS_NECKLACE).add(ModItems.KINDLED_SIGIL.key());

        // Crust rock works as cobblestone does: stone tools, furnaces
        tag(ItemTags.STONE_TOOL_MATERIALS).add(ModItems.CRUST_ROCK.key());
        tag(ItemTags.STONE_CRAFTING_MATERIALS).add(ModItems.CRUST_ROCK.key());

        tag(ModTags.Items.INGOTS_BRASS).add(ModItems.BRASS_INGOT.key());
        tag(Tags.Items.INGOTS).addTag(ModTags.Items.INGOTS_BRASS);
        tag(ModTags.Items.NUGGETS_BRASS).add(ModItems.BRASS_NUGGET.key());
        tag(Tags.Items.NUGGETS).addTag(ModTags.Items.NUGGETS_BRASS);
        tag(ModTags.Items.PLATES_BRASS).add(ModItems.BRASS_PLATE.key());
        tag(ModTags.Items.PLATES).addTag(ModTags.Items.PLATES_BRASS);
        tag(ModTags.Items.GEARS_BRASS).add(ModItems.BRASS_GEAR.key());
        tag(ModTags.Items.GEARS).addTag(ModTags.Items.GEARS_BRASS);

        // Armor slot tags drive enchantability (enchantable/* tags include these).
        tag(ItemTags.HEAD_ARMOR).add(ModItems.COWBOY_HAT.key());
        tag(ItemTags.CHEST_ARMOR).add(ModItems.DUSTER.key());
        tag(ItemTags.LEG_ARMOR).add(ModItems.CHAPS.key());
        tag(ItemTags.FOOT_ARMOR).add(ModItems.COWBOY_BOOTS.key(), ModItems.SNAKESKIN_BOOTS.key());
        tag(ItemTags.WOODEN_DOORS).add(ModItems.SALOON_DOORS.key());
        // The guns (22): their own enchantments, and vanilla's Unbreaking, Mending and Curse of Vanishing
        tag(ModTags.Items.ENCHANTABLE_SHOTGUN).add(ModItems.SHOTGUN.key());
        tag(ModTags.Items.ENCHANTABLE_MUZZLELOADER).add(ModItems.REVOLVER.key(), ModItems.PLAINS_RIFLE.key()).addTag(ModTags.Items.ENCHANTABLE_SHOTGUN);
        tag(ModTags.Items.ENCHANTABLE_PHLOGISTON_GUN).add(ModItems.RAY_PISTOL.key(), ModItems.PHLOGISTON_RIFLE.key());
        tag(ModTags.Items.ENCHANTABLE_GUN).addTag(ModTags.Items.ENCHANTABLE_MUZZLELOADER).addTag(ModTags.Items.ENCHANTABLE_PHLOGISTON_GUN);
        tag(ItemTags.DURABILITY_ENCHANTABLE).addTag(ModTags.Items.ENCHANTABLE_GUN);
        tag(ItemTags.VANISHING_ENCHANTABLE).addTag(ModTags.Items.ENCHANTABLE_GUN);

        tag(ModTags.Items.DUSTS_IRON).add(ModItems.IRON_DUST.key());
        tag(ModTags.Items.DUSTS_GOLD).add(ModItems.GOLD_DUST.key());
        tag(ModTags.Items.DUSTS_COPPER).add(ModItems.COPPER_DUST.key());
        tag(ModTags.Items.DUSTS_STARSTEEL).add(ModItems.STARSTEEL_DUST.key());
        tag(ModTags.Items.DUSTS).addTag(ModTags.Items.DUSTS_IRON).addTag(ModTags.Items.DUSTS_GOLD)
                .addTag(ModTags.Items.DUSTS_COPPER).addTag(ModTags.Items.DUSTS_STARSTEEL);

        // The Hollow Earth's materials and gear (9c)
        copy(ModTags.BlockItems.ORES_STARSTEEL);
        copy(ModTags.BlockItems.ORES_PHLOGISTON);
        copy(ModTags.BlockItems.STORAGE_BLOCKS_RAW_STARSTEEL);
        copy(ModTags.BlockItems.STORAGE_BLOCKS_STARSTEEL);
        tag(ModTags.Items.RAW_MATERIALS_STARSTEEL).add(ModItems.RAW_STARSTEEL.key());
        tag(Tags.Items.RAW_MATERIALS).addTag(ModTags.Items.RAW_MATERIALS_STARSTEEL);
        tag(ModTags.Items.INGOTS_STARSTEEL).add(ModItems.STARSTEEL_INGOT.key());
        tag(Tags.Items.INGOTS).addTag(ModTags.Items.INGOTS_STARSTEEL);
        tag(ModTags.Items.NUGGETS_STARSTEEL).add(ModItems.STARSTEEL_NUGGET.key());
        tag(Tags.Items.NUGGETS).addTag(ModTags.Items.NUGGETS_STARSTEEL);
        tag(ModTags.Items.GEMS_PHLOGISTON).add(ModItems.PHLOGISTON_SHARD.key());
        tag(Tags.Items.GEMS).addTag(ModTags.Items.GEMS_PHLOGISTON);
        tag(ModTags.Items.REPAIRS_STARSTEEL).addTag(ModTags.Items.INGOTS_STARSTEEL);
        tag(ItemTags.SWORDS).add(ModItems.STARSTEEL_SWORD.key());
        // The paxel counts as all three tools, so it takes each one's enchantments
        tag(ItemTags.PICKAXES).add(ModItems.STARSTEEL_PAXEL.key());
        tag(ItemTags.AXES).add(ModItems.STARSTEEL_PAXEL.key());
        tag(ItemTags.SHOVELS).add(ModItems.STARSTEEL_PAXEL.key());
        tag(ItemTags.HEAD_ARMOR).add(ModItems.STARSTEEL_HELMET.key());
        tag(ItemTags.CHEST_ARMOR).add(ModItems.STARSTEEL_CHESTPLATE.key());
        tag(ItemTags.LEG_ARMOR).add(ModItems.STARSTEEL_LEGGINGS.key());
        tag(ItemTags.FOOT_ARMOR).add(ModItems.STARSTEEL_BOOTS.key());
        tag(ModTags.Items.STARSTEEL_GEAR).add(ModItems.STARSTEEL_SWORD.key(), ModItems.STARSTEEL_PAXEL.key(), ModItems.STARSTEEL_HELMET.key(), ModItems.STARSTEEL_CHESTPLATE.key(),
                ModItems.STARSTEEL_LEGGINGS.key(), ModItems.STARSTEEL_BOOTS.key(), ModItems.STARSTEEL_CROWN.key());
        // The Archon's crown (12b) is a helmet as any other, and sun-forged with the rest
        tag(ItemTags.HEAD_ARMOR).add(ModItems.STARSTEEL_CROWN.key());
        // The megafauna's drops and gear (10a)
        tag(ModTags.Items.TALONS).add(ModItems.TERROR_BIRD_TALON.key());
        tag(ModTags.Items.SCUTES_GLYPTODON).add(ModItems.GLYPTODON_SCUTE.key());
        tag(ModTags.Items.HIDES_THICK).add(ModItems.THICK_HIDE.key());
        // What a Talon Knife skins one more of: hides and leather, tusks, talons, scutes, snakeskin
        tag(ModTags.Items.SKINNINGS).add(ModItems.THICK_HIDE.key(), ModItems.MAMMOTH_TUSK.key(), ModItems.TERROR_BIRD_TALON.key(),
                ModItems.GLYPTODON_SCUTE.key(), ModItems.SNAKESKIN.key()).addTag(Tags.Items.LEATHERS).add(Items.RABBIT_HIDE.builtInRegistryHolder().key(), Items.ARMADILLO_SCUTE.builtInRegistryHolder().key());
        tag(ModTags.Items.GLYPTODON_SHELL_ARMOR).add(ModItems.GLYPTODON_HELMET.key(), ModItems.GLYPTODON_CHESTPLATE.key(),
                ModItems.GLYPTODON_LEGGINGS.key(), ModItems.GLYPTODON_BOOTS.key());
        // The Grays' things the men in black buy back (11b)
        tag(ModTags.Items.STRANGE_GOODS).add(ModItems.SHOCK_ROD.key()); // 50
        tag(ModTags.Items.STRANGE_GOODS).add(ModItems.GRAY_TECH_FRAGMENT.key(), ModItems.STRANGE_SKULL.key(), ModItems.SIGNAL_LOCATOR.key(),
                ModItems.FLATTENED_WHEAT.key());
        tag(ItemTags.SWORDS).add(ModItems.TALON_KNIFE.key());
        tag(ItemTags.HEAD_ARMOR).add(ModItems.GLYPTODON_HELMET.key());
        tag(ItemTags.CHEST_ARMOR).add(ModItems.GLYPTODON_CHESTPLATE.key());
        tag(ItemTags.LEG_ARMOR).add(ModItems.GLYPTODON_LEGGINGS.key());
        tag(ItemTags.FOOT_ARMOR).add(ModItems.GLYPTODON_BOOTS.key());
        tag(ItemTags.MEAT).add(ModItems.RAW_MAMMOTH.key(), ModItems.MAMMOTH_STEAK.key());
        tag(Tags.Items.FOODS_RAW_MEAT).add(ModItems.RAW_MAMMOTH.key());
        tag(Tags.Items.FOODS_COOKED_MEAT).add(ModItems.MAMMOTH_STEAK.key());
    }
}
