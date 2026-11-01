package com.hollowfrontier.datagen;

import java.util.List;

import com.hollowfrontier.HollowFrontier;
import com.hollowfrontier.content.frontier.brass.BrassAlloyingCondition;
import com.hollowfrontier.content.steam.CrushingRecipe;
import com.hollowfrontier.registry.ModItems;
import com.hollowfrontier.registry.ModTags;

import net.minecraft.advancements.Advancement;
import net.minecraft.advancements.predicates.MinMaxBounds;
import net.minecraft.core.registries.Registries;
import net.minecraft.data.recipes.CustomCraftingRecipeBuilder;
import net.minecraft.data.recipes.RecipeBuilder;
import net.minecraft.resources.ResourceKey;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStackTemplate;
import net.minecraft.world.item.crafting.DyeRecipe;
import net.minecraft.data.recipes.RecipeCategory;
import net.minecraft.data.recipes.RecipeProvider;
import net.minecraft.data.recipes.SimpleCookingRecipeBuilder;
import net.minecraft.data.recipes.SingleItemRecipeBuilder;
import net.minecraft.data.recipes.TransmuteRecipeBuilder;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.data.worldgen.BootstrapContext;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.CookingBookCategory;
import net.minecraft.world.item.crafting.CampfireCookingRecipe;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.SmeltingRecipe;
import net.minecraft.world.item.crafting.SmokingRecipe;
import net.minecraft.world.level.ItemLike;
import net.neoforged.neoforge.common.Tags;

// Recipe ids are plain paths; the default mod id passed to super() puts them in our namespace.
public class ModRecipeProvider extends RecipeProvider {
    public ModRecipeProvider(BootstrapContext<Recipe<?>> recipes, BootstrapContext<Advancement> advancements) {
        super(recipes, advancements, HollowFrontier.MODID);
    }

    @Override
    protected void buildRecipes() {
        materials();
        guns();
        cowboyGear();
        bounties();
        furniture();
        steam();
        crushing();
        expedition();
        hollowEarth();
        concavia();
        megafauna();
        gray();
    }

    // 10b: the Signal Locator, Gray fragments round a phlogiston shard in a compass
    private void gray() {
        shaped(RecipeCategory.TOOLS, ModItems.SIGNAL_LOCATOR.get())
                .define('F', ModItems.GRAY_TECH_FRAGMENT.get()).define('C', Items.COMPASS).define('V', ModTags.Items.GEMS_PHLOGISTON)
                .pattern(" F ").pattern("FCF").pattern(" V ")
                .unlockedBy("has_gray_tech_fragment", has(ModItems.GRAY_TECH_FRAGMENT.get())).save(output);
    }

    // 10a: what the megafauna's drops make
    private void megafauna() {
        // The Oliphant: a mammoth's tusk, a leather sling and a brass mouthpiece. A spare tusk grinds to bone meal.
        shapeless(RecipeCategory.TOOLS, ModItems.OLIPHANT.get())
                .requires(ModItems.MAMMOTH_TUSK.get()).requires(Tags.Items.LEATHERS).requires(ModTags.Items.NUGGETS_BRASS)
                .unlockedBy("has_mammoth_tusk", has(ModItems.MAMMOTH_TUSK.get())).save(output);
        shapeless(RecipeCategory.MISC, Items.BONE_MEAL, 6)
                .requires(ModItems.MAMMOTH_TUSK.get())
                .unlockedBy("has_mammoth_tusk", has(ModItems.MAMMOTH_TUSK.get()))
                .save(output, HollowFrontier.id("bone_meal_from_mammoth_tusk").toString());
        simpleCookingRecipe("smelting", SmeltingRecipe::new, 200, ModItems.RAW_MAMMOTH.get(), ModItems.MAMMOTH_STEAK.get(), 0.35F);
        simpleCookingRecipe("smoking", SmokingRecipe::new, 100, ModItems.RAW_MAMMOTH.get(), ModItems.MAMMOTH_STEAK.get(), 0.35F);
        simpleCookingRecipe("campfire_cooking", CampfireCookingRecipe::new, 600, ModItems.RAW_MAMMOTH.get(), ModItems.MAMMOTH_STEAK.get(), 0.35F);
        // The Talon Knife: the talon bound on a stick with a leather grip
        shaped(RecipeCategory.COMBAT, ModItems.TALON_KNIFE.get())
                .define('T', ModItems.TERROR_BIRD_TALON.get()).define('L', Tags.Items.LEATHERS).define('S', Items.STICK)
                .pattern("T").pattern("L").pattern("S")
                .unlockedBy("has_terror_bird_talon", has(ModItems.TERROR_BIRD_TALON.get())).save(output);
        // Hide Barding: five thick hides with brass buckles. A hide splits into leather too.
        shaped(RecipeCategory.COMBAT, ModItems.HIDE_BARDING.get())
                .define('H', ModTags.Items.HIDES_THICK).define('N', ModTags.Items.NUGGETS_BRASS)
                .pattern("H H").pattern("HHH").pattern("N N")
                .unlockedBy("has_thick_hide", has(ModTags.Items.HIDES_THICK)).save(output);
        shapeless(RecipeCategory.MISC, Items.LEATHER, 2)
                .requires(ModItems.THICK_HIDE.get())
                .unlockedBy("has_thick_hide", has(ModItems.THICK_HIDE.get()))
                .save(output, HollowFrontier.id("leather_from_thick_hide").toString());
        // Glyptodon shell armor, scutes in the usual shapes
        TagKey<Item> scute = ModTags.Items.SCUTES_GLYPTODON;
        String has = "has_glyptodon_scute";
        shaped(RecipeCategory.COMBAT, ModItems.GLYPTODON_HELMET.get()).define('X', scute)
                .pattern("XXX").pattern("X X").unlockedBy(has, has(scute)).save(output);
        shaped(RecipeCategory.COMBAT, ModItems.GLYPTODON_CHESTPLATE.get()).define('X', scute)
                .pattern("X X").pattern("XXX").pattern("XXX").unlockedBy(has, has(scute)).save(output);
        shaped(RecipeCategory.COMBAT, ModItems.GLYPTODON_LEGGINGS.get()).define('X', scute)
                .pattern("XXX").pattern("X X").pattern("X X").unlockedBy(has, has(scute)).save(output);
        shaped(RecipeCategory.COMBAT, ModItems.GLYPTODON_BOOTS.get()).define('X', scute)
                .pattern("X X").pattern("X X").unlockedBy(has, has(scute)).save(output);
    }

    // Phase 9: what's found in the Hollow Earth. Ash grinds to a grey dye (its use until 9c gives it more).
    private void hollowEarth() {
        shapeless(RecipeCategory.MISC, Items.DYE.gray())
                .requires(ModItems.ASH.get())
                .unlockedBy("has_ash", has(ModItems.ASH.get()))
                .save(output, HollowFrontier.id("gray_dye_from_ash").toString());

        // 9c: starsteel smelts (or crushes and smelts) to ingots; tools and armor in the usual shapes; phlogiston lights lamps
        List<ItemLike> starsteel = List.of(ModItems.STARSTEEL_ORE.get(), ModItems.DEEPSLATE_STARSTEEL_ORE.get(), ModItems.RAW_STARSTEEL.get());
        oreSmelting(starsteel, RecipeCategory.MISC, CookingBookCategory.MISC, ModItems.STARSTEEL_INGOT.get(), 1.0F, 200, "starsteel_ingot");
        oreBlasting(starsteel, RecipeCategory.MISC, CookingBookCategory.MISC, ModItems.STARSTEEL_INGOT.get(), 1.0F, 100, "starsteel_ingot");
        List<ItemLike> phlogiston = List.of(ModItems.PHLOGISTON_ORE.get(), ModItems.DEEPSLATE_PHLOGISTON_ORE.get());
        oreSmelting(phlogiston, RecipeCategory.MISC, CookingBookCategory.MISC, ModItems.PHLOGISTON_SHARD.get(), 1.0F, 200, "phlogiston_shard");
        oreBlasting(phlogiston, RecipeCategory.MISC, CookingBookCategory.MISC, ModItems.PHLOGISTON_SHARD.get(), 1.0F, 100, "phlogiston_shard");
        dustSmelting(ModTags.Items.DUSTS_STARSTEEL, ModItems.STARSTEEL_DUST.get(), ModItems.STARSTEEL_INGOT.get());
        storage(ModItems.RAW_STARSTEEL.get(), ModItems.RAW_STARSTEEL_BLOCK.get(), "raw_starsteel", "raw_starsteel_block");
        storage(ModItems.STARSTEEL_INGOT.get(), ModItems.STARSTEEL_BLOCK.get(), "starsteel_ingot", "starsteel_block");
        storage(ModItems.STARSTEEL_NUGGET.get(), ModItems.STARSTEEL_INGOT.get(), "starsteel_nugget", "starsteel_ingot_from_nuggets");
        TagKey<Item> ingot = ModTags.Items.INGOTS_STARSTEEL;
        String has = "has_starsteel_ingot";
        shaped(RecipeCategory.COMBAT, ModItems.STARSTEEL_SWORD.get()).define('O', ingot).define('S', Items.STICK)
                .pattern("O").pattern("O").pattern("S").unlockedBy(has, has(ingot)).save(output);
        // The paxel: a pickaxe's head with an axe's blade worked into it, on two sticks
        shaped(RecipeCategory.TOOLS, ModItems.STARSTEEL_PAXEL.get()).define('O', ingot).define('S', Items.STICK)
                .pattern("OOO").pattern("OS ").pattern(" S ").unlockedBy(has, has(ingot)).save(output);
        shaped(RecipeCategory.COMBAT, ModItems.STARSTEEL_HELMET.get()).define('O', ingot)
                .pattern("OOO").pattern("O O").unlockedBy(has, has(ingot)).save(output);
        shaped(RecipeCategory.COMBAT, ModItems.STARSTEEL_CHESTPLATE.get()).define('O', ingot)
                .pattern("O O").pattern("OOO").pattern("OOO").unlockedBy(has, has(ingot)).save(output);
        shaped(RecipeCategory.COMBAT, ModItems.STARSTEEL_LEGGINGS.get()).define('O', ingot)
                .pattern("OOO").pattern("O O").pattern("O O").unlockedBy(has, has(ingot)).save(output);
        shaped(RecipeCategory.COMBAT, ModItems.STARSTEEL_BOOTS.get()).define('O', ingot)
                .pattern("O O").pattern("O O").unlockedBy(has, has(ingot)).save(output);
        shaped(RecipeCategory.TOOLS, ModItems.STARSTEEL_HORSESHOES.get()).define('O', ingot).define('N', ModTags.Items.NUGGETS_STARSTEEL)
                .pattern("ONO").pattern("O O").unlockedBy(has, has(ingot)).save(output);
        shaped(RecipeCategory.DECORATIONS, ModItems.PHLOGISTON_LAMP.get())
                .define('B', ModTags.Items.INGOTS_BRASS).define('G', Tags.BlockItems.GLASS_BLOCKS.item()).define('V', ModTags.Items.GEMS_PHLOGISTON)
                .pattern(" B ").pattern("GVG").pattern(" B ")
                .unlockedBy("has_phlogiston_shard", has(ModTags.Items.GEMS_PHLOGISTON)).save(output);
    }

    // Phase 12a: Concavia's crystal stone. Glass and quartz round a phlogiston shard make the crystal; the bricks are cut from it
    // (at the table, or one for one at the stonecutter, with the stairs, slabs, walls and chiseled bricks).
    private void concavia() {
        Item crystal = ModItems.CONCAVIAN_CRYSTAL.get(), bricks = ModItems.CRYSTAL_BRICKS.get();
        shaped(RecipeCategory.BUILDING_BLOCKS, crystal, 8)
                .define('G', Tags.BlockItems.GLASS_BLOCKS_COLORLESS.item()).define('Q', Tags.Items.GEMS_QUARTZ).define('V', ModTags.Items.GEMS_PHLOGISTON)
                .pattern("GQG").pattern("QVQ").pattern("GQG")
                .unlockedBy("has_phlogiston_shard", has(ModTags.Items.GEMS_PHLOGISTON)).save(output);
        shaped(RecipeCategory.BUILDING_BLOCKS, bricks, 4).define('C', crystal).pattern("CC").pattern("CC")
                .unlockedBy("has_concavian_crystal", has(crystal)).save(output);
        stairBuilder(ModItems.CRYSTAL_BRICK_STAIRS.get(), Ingredient.of(bricks)).unlockedBy("has_crystal_bricks", has(bricks)).save(output);
        slab(RecipeCategory.BUILDING_BLOCKS, ModItems.CRYSTAL_BRICK_SLAB.get(), bricks);
        wall(RecipeCategory.DECORATIONS, ModItems.CRYSTAL_BRICK_WALL.get(), bricks);
        chiseled(RecipeCategory.BUILDING_BLOCKS, ModItems.CHISELED_CRYSTAL_BRICKS.get(), ModItems.CRYSTAL_BRICK_SLAB.get());
        stonecutterResultFromBase(RecipeCategory.BUILDING_BLOCKS, bricks, crystal);
        for (var cut : List.of(ModItems.CRYSTAL_BRICK_STAIRS.get(), ModItems.CRYSTAL_BRICK_WALL.get(), ModItems.CHISELED_CRYSTAL_BRICKS.get())) {
            stonecutterResultFromBase(RecipeCategory.BUILDING_BLOCKS, cut, bricks);
        }
        stonecutterResultFromBase(RecipeCategory.BUILDING_BLOCKS, ModItems.CRYSTAL_BRICK_SLAB.get(), bricks, 2);
        // 28: the Concavian trim: applied at the smithing table like vanilla's, copied with seven diamonds and crystal brick
        Item template = ModItems.CONCAVIAN_ARMOR_TRIM_SMITHING_TEMPLATE.get();
        copySmithingTemplate(template, bricks);
        trimSmithing(template, com.hollowfrontier.content.hollow.HollowPatterns.CONCAVIAN,
                net.minecraft.resources.ResourceKey.create(Registries.RECIPE, HollowFrontier.id("concavian_armor_trim_smithing_template_smithing_trim")));
        // 12b: the Sunstone Lens (sunstone ground and set in a starsteel ring on a handle), and the Codex (a book and a phlogiston shard)
        shaped(RecipeCategory.TOOLS, ModItems.SUNSTONE_LENS.get())
                .define('S', ModItems.SUNSTONE_SHARD.get()).define('H', ModTags.Items.INGOTS_STARSTEEL).define('G', Tags.BlockItems.GLASS_PANES_COLORLESS.item())
                .pattern(" HS").pattern("HGH").pattern("SH ")
                .unlockedBy("has_sunstone_shard", has(ModItems.SUNSTONE_SHARD.get())).save(output);
        shapeless(RecipeCategory.MISC, ModItems.CONCAVIAN_CODEX.get()).requires(Items.BOOK).requires(ModTags.Items.GEMS_PHLOGISTON)
                .unlockedBy("has_phlogiston_shard", has(ModTags.Items.GEMS_PHLOGISTON)).save(output);
        // 13b: phlogiston cells (brass caps on a glass cell with a starsteel contact, charged with a phlogiston shard), the phlogiston guns
        // (Gray work rebuilt in starsteel), and the gravity drive mended with phlogiston and starsteel
        shaped(RecipeCategory.MISC, ModItems.EMPTY_PHLOGISTON_CELL.get(), 2)
                .define('B', ModTags.Items.INGOTS_BRASS).define('G', Tags.BlockItems.GLASS_PANES_COLORLESS.item()).define('N', ModTags.Items.NUGGETS_STARSTEEL)
                .pattern(" B ").pattern("GNG").pattern(" B ")
                .unlockedBy("has_phlogiston_shard", has(ModTags.Items.GEMS_PHLOGISTON)).save(output);
        shapeless(RecipeCategory.MISC, ModItems.PHLOGISTON_CELL.get()).requires(ModItems.EMPTY_PHLOGISTON_CELL.get()).requires(ModTags.Items.GEMS_PHLOGISTON)
                .unlockedBy("has_empty_phlogiston_cell", has(ModItems.EMPTY_PHLOGISTON_CELL.get())).save(output);
        shaped(RecipeCategory.COMBAT, ModItems.RAY_PISTOL.get())
                .define('F', ModItems.GRAY_TECH_FRAGMENT.get()).define('V', ModTags.Items.GEMS_PHLOGISTON).define('H', ModTags.Items.INGOTS_STARSTEEL)
                .define('B', ModTags.Items.INGOTS_BRASS)
                .pattern(" FV").pattern("HHB").pattern(" B ")
                .unlockedBy("has_gray_tech_fragment", has(ModItems.GRAY_TECH_FRAGMENT.get())).save(output);
        shaped(RecipeCategory.COMBAT, ModItems.PHLOGISTON_RIFLE.get())
                .define('F', ModItems.GRAY_TECH_FRAGMENT.get()).define('V', ModTags.Items.GEMS_PHLOGISTON).define('H', ModTags.Items.INGOTS_STARSTEEL)
                .define('B', ModTags.Items.INGOTS_BRASS)
                .pattern("FVF").pattern("HHH").pattern("BB ")
                .unlockedBy("has_gray_tech_fragment", has(ModItems.GRAY_TECH_FRAGMENT.get())).save(output);
        shaped(RecipeCategory.MISC, ModItems.GRAVITY_DRIVE.get())
                .define('D', ModItems.DAMAGED_GRAVITY_DRIVE.get()).define('V', ModTags.Items.GEMS_PHLOGISTON).define('H', ModTags.Items.INGOTS_STARSTEEL)
                .pattern("HVH").pattern("VDV").pattern("HVH")
                .unlockedBy("has_damaged_gravity_drive", has(ModItems.DAMAGED_GRAVITY_DRIVE.get())).save(output);
        // 13d: the Phlogiston Beacon: the Sun Archon's Phlogiston Core in starsteel, with the Grays' own tech to call them, over a phlogiston lamp
        shaped(RecipeCategory.MISC, ModItems.PHLOGISTON_BEACON.get())
                .define('H', ModTags.Items.INGOTS_STARSTEEL).define('F', ModItems.GRAY_TECH_FRAGMENT.get())
                .define('C', ModItems.PHLOGISTON_CORE.get()).define('L', ModItems.PHLOGISTON_LAMP.get())
                .pattern("HFH").pattern("FCF").pattern("HLH")
                .unlockedBy("has_phlogiston_core", has(ModItems.PHLOGISTON_CORE.get())).save(output);
        // 13c: the Prototype Saucer, whole: eight alien alloy plates round the mended gravity drive (it comes with the beam and laser; a navigation core goes on after)
        shaped(RecipeCategory.TRANSPORTATION, ModItems.PROTOTYPE_SAUCER.get())
                .define('P', ModItems.ALIEN_ALLOY_PLATING.get()).define('D', ModItems.GRAVITY_DRIVE.get())
                .pattern("PPP").pattern("PDP").pattern("PPP")
                .unlockedBy("has_gravity_drive", has(ModItems.GRAVITY_DRIVE.get())).save(output);
    }

    // Phase 8: the airship (envelope + engine + a boat for the gondola) and the polar kit.
    private void expedition() {
        shaped(RecipeCategory.TRANSPORTATION, ModItems.AIRSHIP_ENVELOPE.get())
                .define('L', Items.LEATHER)
                .define('M', Items.PHANTOM_MEMBRANE)
                .define('S', Items.STRING)
                .pattern("LML")
                .pattern("LSL")
                .pattern("LML")
                .unlockedBy("has_phantom_membrane", has(Items.PHANTOM_MEMBRANE))
                .save(output);
        shaped(RecipeCategory.TRANSPORTATION, ModItems.AIRSHIP_ENGINE.get())
                .define('G', ModTags.Items.GEARS_BRASS)
                .define('P', ModTags.Items.PLATES_BRASS)
                .define('C', ModItems.COPPER_PIPE.get())
                .define('F', Items.FURNACE)
                .define('V', ModItems.PRESSURE_VALVE.get())
                .pattern("GPG")
                .pattern("CFC")
                .pattern("GVG")
                .unlockedBy("has_pressure_valve", has(ModItems.PRESSURE_VALVE.get()))
                .save(output);
        shaped(RecipeCategory.TRANSPORTATION, ModItems.AIRSHIP.get())
                .define('L', Items.LEAD)
                .define('E', ModItems.AIRSHIP_ENVELOPE.get())
                .define('N', ModItems.AIRSHIP_ENGINE.get())
                .define('B', ItemTags.BOATS)
                .pattern("LEL")
                .pattern(" N ")
                .pattern(" B ")
                .unlockedBy("has_airship_envelope", has(ModItems.AIRSHIP_ENVELOPE.get()))
                .save(output);
        shaped(RecipeCategory.TOOLS, ModItems.POLAR_COMPASS.get())
                .define('P', ModTags.Items.PLATES_BRASS)
                .define('C', Items.COMPASS)
                .define('I', Items.PACKED_ICE)
                .pattern(" P ")
                .pattern("PCP")
                .pattern(" I ")
                .unlockedBy("has_compass", has(Items.COMPASS))
                .save(output);
        shaped(RecipeCategory.TOOLS, ModItems.POLAR_RESONATOR.get())
                .define('A', Items.AMETHYST_SHARD)
                .define('G', ModTags.Items.GEARS_BRASS)
                .define('V', ModItems.PRESSURE_VALVE.get())
                .define('P', ModTags.Items.PLATES_BRASS)
                .define('C', ModItems.COPPER_PIPE.get())
                .pattern(" A ")
                .pattern("GVG")
                .pattern("PCP")
                .unlockedBy("has_amethyst_shard", has(Items.AMETHYST_SHARD))
                .save(output);
    }

    private void steam() {
        shaped(RecipeCategory.MISC, ModItems.COPPER_PIPE.get(), 6)
                .define('C', Tags.Items.INGOTS_COPPER)
                .pattern("CCC")
                .unlockedBy("has_copper_ingot", has(Tags.Items.INGOTS_COPPER))
                .save(output);
        shaped(RecipeCategory.MISC, ModItems.BOILER.get())
                .define('C', Tags.Items.INGOTS_COPPER)
                .define('V', ModItems.PRESSURE_VALVE.get())
                .define('F', Items.FURNACE)
                .define('B', Items.BRICKS)
                .pattern("CVC")
                .pattern("CFC")
                .pattern("BBB")
                .unlockedBy("has_pressure_valve", has(ModItems.PRESSURE_VALVE.get()))
                .save(output);
        shaped(RecipeCategory.MISC, ModItems.STEAM_TANK.get())
                .define('P', ModTags.Items.PLATES_BRASS)
                .define('V', ModItems.PRESSURE_VALVE.get())
                .pattern("PPP")
                .pattern("PVP")
                .pattern("PPP")
                .unlockedBy("has_pressure_valve", has(ModItems.PRESSURE_VALVE.get()))
                .save(output);
        shaped(RecipeCategory.REDSTONE, ModItems.PRESSURE_GAUGE.get())
                .define('P', ModTags.Items.PLATES_BRASS)
                .define('G', Tags.BlockItems.GLASS_PANES.item())
                .define('V', ModItems.PRESSURE_VALVE.get())
                .pattern("PGP")
                .pattern(" V ")
                .unlockedBy("has_pressure_valve", has(ModItems.PRESSURE_VALVE.get()))
                .save(output);
        shaped(RecipeCategory.MISC, ModItems.CRUSHER.get())
                .define('I', Tags.Items.INGOTS_IRON)
                .define('G', ModTags.Items.GEARS_BRASS)
                .define('F', Items.FLINT)
                .define('V', ModItems.PRESSURE_VALVE.get())
                .pattern("IGI")
                .pattern("FVF")
                .pattern("III")
                .unlockedBy("has_pressure_valve", has(ModItems.PRESSURE_VALVE.get()))
                .save(output);
        shaped(RecipeCategory.TOOLS, ModItems.STEAM_CANISTER.get())
                .define('P', ModTags.Items.PLATES_BRASS)
                .define('V', ModItems.PRESSURE_VALVE.get())
                .pattern(" V ")
                .pattern("P P")
                .pattern("PPP")
                .unlockedBy("has_pressure_valve", has(ModItems.PRESSURE_VALVE.get()))
                .save(output);
        // The handcar is where brass parts and the pressure valve come together: a minecart with an engine on it.
        shaped(RecipeCategory.TRANSPORTATION, ModItems.STEAM_HANDCAR.get())
                .define('V', ModItems.PRESSURE_VALVE.get())
                .define('G', ModTags.Items.GEARS_BRASS)
                .define('P', ModTags.Items.PLATES_BRASS)
                .define('M', Items.MINECART)
                .define('C', ModItems.COPPER_PIPE.get())
                .pattern(" V ")
                .pattern("GMG")
                .pattern("PCP")
                .unlockedBy("has_pressure_valve", has(ModItems.PRESSURE_VALVE.get()))
                .save(output);
        shaped(RecipeCategory.TOOLS, ModItems.STEAM_DRILL.get())
                .define('I', Tags.Items.INGOTS_IRON)
                .define('G', ModTags.Items.GEARS_BRASS)
                .define('P', ModTags.Items.PLATES_BRASS)
                .define('V', ModItems.PRESSURE_VALVE.get())
                .pattern(" II")
                .pattern("PGI")
                .pattern("VP ")
                .unlockedBy("has_pressure_valve", has(ModItems.PRESSURE_VALVE.get()))
                .save(output);

        // Dusts smelt back into ingots: any mod's dust (the c: tag), under our dust's recipe name so datapacks that override it keep working.
        dustSmelting(ModTags.Items.DUSTS_IRON, ModItems.IRON_DUST.get(), Items.IRON_INGOT);
        dustSmelting(ModTags.Items.DUSTS_GOLD, ModItems.GOLD_DUST.get(), Items.GOLD_INGOT);
        dustSmelting(ModTags.Items.DUSTS_COPPER, ModItems.COPPER_DUST.get(), Items.COPPER_INGOT);
    }

    private void dustSmelting(TagKey<Item> dusts, ItemLike dust, ItemLike ingot) {
        String name = getItemName(ingot);
        SimpleCookingRecipeBuilder.smelting(tag(dusts), RecipeCategory.MISC, CookingBookCategory.MISC, ingot, 0.35F, 200)
                .group(name).unlockedBy(getHasName(dust), has(dusts)).save(output, name + "_from_smelting_" + getItemName(dust));
        SimpleCookingRecipeBuilder.blasting(tag(dusts), RecipeCategory.MISC, CookingBookCategory.MISC, ingot, 0.35F, 100)
                .group(name).unlockedBy(getHasName(dust), has(dusts)).save(output, name + "_from_blasting_" + getItemName(dust));
    }

    // Crusher: raw ore or ore block -> 2 dust (ore doubling), plus a cobblestone -> gravel -> sand chain.
    private void crushing() {
        crush("iron_dust_from_raw_iron", tag(Tags.Items.RAW_MATERIALS_IRON), ModItems.IRON_DUST.get(), 2);
        crush("iron_dust_from_ore", tag(Tags.BlockItems.ORES_IRON.item()), ModItems.IRON_DUST.get(), 2);
        crush("gold_dust_from_raw_gold", tag(Tags.Items.RAW_MATERIALS_GOLD), ModItems.GOLD_DUST.get(), 2);
        crush("gold_dust_from_ore", tag(Tags.BlockItems.ORES_GOLD.item()), ModItems.GOLD_DUST.get(), 2);
        crush("copper_dust_from_raw_copper", tag(Tags.Items.RAW_MATERIALS_COPPER), ModItems.COPPER_DUST.get(), 2);
        crush("copper_dust_from_ore", tag(Tags.BlockItems.ORES_COPPER.item()), ModItems.COPPER_DUST.get(), 2);
        crush("starsteel_dust_from_raw_starsteel", tag(ModTags.Items.RAW_MATERIALS_STARSTEEL), ModItems.STARSTEEL_DUST.get(), 2);
        crush("starsteel_dust_from_ore", tag(ModTags.BlockItems.ORES_STARSTEEL.item()), ModItems.STARSTEEL_DUST.get(), 2);
        crush("phlogiston_shards_from_ore", tag(ModTags.BlockItems.ORES_PHLOGISTON.item()), ModItems.PHLOGISTON_SHARD.get(), 3);
        crush("gravel_from_cobblestone", tag(Tags.BlockItems.COBBLESTONES.item()), Items.GRAVEL, 1);
        crush("sand_from_gravel", tag(Tags.BlockItems.GRAVELS.item()), Items.SAND, 1);
    }

    private void crush(String name, Ingredient input, ItemLike result, int count) {
        new SingleItemRecipeBuilder(RecipeCategory.MISC, CrushingRecipe::new, input, result, count)
                .unlockedBy("has_crusher", has(ModItems.CRUSHER.get()))
                .save(output, "crushing/" + name);
    }

    // Coins are minted from four vanilla nuggets (2x2, fits the inventory grid), and 9 copper = 1 gold both ways.
    private void bounties() {
        mint(ModItems.COPPER_COIN.get(), Items.COPPER_NUGGET);
        mint(ModItems.GOLD_COIN.get(), Items.GOLD_NUGGET);
        storage(ModItems.COPPER_COIN.get(), ModItems.GOLD_COIN.get(), "copper_coin_from_gold_coin", "gold_coin_from_copper_coins");
        shaped(RecipeCategory.DECORATIONS, ModItems.BOUNTY_BOARD.get())
                .define('W', ItemTags.PLANKS)
                .define('P', Items.PAPER)
                .define('S', Tags.Items.RODS_WOODEN)
                .pattern("WWW")
                .pattern("WPW")
                .pattern("S S")
                .unlockedBy("has_paper", has(Items.PAPER))
                .save(output);
        // The hanging Bounty Board (18): a frame of sticks round paper on planks, or two small boards side by side
        shaped(RecipeCategory.DECORATIONS, ModItems.HANGING_BOUNTY_BOARD.get())
                .define('W', ItemTags.PLANKS)
                .define('P', Items.PAPER)
                .define('S', Tags.Items.RODS_WOODEN)
                .pattern("SSS")
                .pattern("PPP")
                .pattern("WWW")
                .unlockedBy("has_paper", has(Items.PAPER))
                .save(output);
        shapeless(RecipeCategory.DECORATIONS, ModItems.HANGING_BOUNTY_BOARD.get())
                .requires(ModItems.BOUNTY_BOARD.get(), 2)
                .unlockedBy("has_bounty_board", has(ModItems.BOUNTY_BOARD.get()))
                .save(output, "hanging_bounty_board_from_small");
        // Saloon piano: a wooden case, a note block for the works, bone for the ivory keys.
        shaped(RecipeCategory.DECORATIONS, ModItems.SALOON_PIANO.get())
                .define('W', ItemTags.PLANKS)
                .define('N', Items.NOTE_BLOCK)
                .define('B', Items.BONE)
                .pattern("WWW")
                .pattern("BNB")
                .pattern("W W")
                .unlockedBy("has_note_block", has(Items.NOTE_BLOCK))
                .save(output);
    }

    private void furniture() {
        shaped(RecipeCategory.DECORATIONS, ModItems.BAR_COUNTER.get(), 3)
                .define('S', Items.DARK_OAK_SLAB)
                .define('W', ItemTags.PLANKS)
                .pattern("SSS")
                .pattern("WWW")
                .pattern("WWW")
                .unlockedBy("has_dark_oak_slab", has(Items.DARK_OAK_SLAB))
                .save(output);
        shaped(RecipeCategory.DECORATIONS, ModItems.BAR_STOOL.get(), 2)
                .define('S', ItemTags.WOODEN_SLABS)
                .define('R', Tags.Items.RODS_WOODEN)
                .pattern("SSS")
                .pattern(" R ")
                .pattern(" R ")
                .unlockedBy("has_stick", has(Tags.Items.RODS_WOODEN))
                .save(output);
        shaped(RecipeCategory.REDSTONE, ModItems.SALOON_DOORS.get(), 2)
                .define('W', ItemTags.PLANKS)
                .define('R', Tags.Items.RODS_WOODEN)
                .pattern("RR")
                .pattern("WW")
                .pattern("RR")
                .unlockedBy("has_stick", has(Tags.Items.RODS_WOODEN))
                .save(output);
        // Card table: green felt (carpet) with a deck of cards (paper) on a pedestal
        shaped(RecipeCategory.DECORATIONS, ModItems.CARD_TABLE.get())
                .define('G', Items.CARPET.green())
                .define('P', Items.PAPER)
                .define('W', ItemTags.PLANKS)
                .pattern("GPG")
                .pattern(" W ")
                .pattern("WWW")
                .unlockedBy("has_green_carpet", has(Items.CARPET.green()))
                .save(output);
        // 24c: town lamps, lanterns on a daylight switch
        shapeless(RecipeCategory.DECORATIONS, ModItems.TOWN_LAMP.get(), 2)
                .requires(Items.LANTERN, 2)
                .requires(Items.DAYLIGHT_DETECTOR)
                .unlockedBy("has_daylight_detector", has(Items.DAYLIGHT_DETECTOR))
                .save(output);
        // 24c: chairs (a ladder back on a seat on legs, two at a time) and tables (a slab top on legs), in each wood
        Object[][] woods = {{ModItems.OAK_CHAIR, ModItems.OAK_TABLE, Items.OAK_PLANKS, Items.OAK_SLAB},
                {ModItems.SPRUCE_CHAIR, ModItems.SPRUCE_TABLE, Items.SPRUCE_PLANKS, Items.SPRUCE_SLAB},
                {ModItems.DARK_OAK_CHAIR, ModItems.DARK_OAK_TABLE, Items.DARK_OAK_PLANKS, Items.DARK_OAK_SLAB}};
        for (Object[] wood : woods) {
            net.minecraft.world.item.Item planks = (net.minecraft.world.item.Item) wood[2], slab = (net.minecraft.world.item.Item) wood[3];
            shaped(RecipeCategory.DECORATIONS, ((net.neoforged.neoforge.registries.DeferredItem<?>) wood[0]).get(), 2)
                    .define('P', planks)
                    .define('R', Tags.Items.RODS_WOODEN)
                    .pattern("P  ")
                    .pattern("PPP")
                    .pattern("R R")
                    .unlockedBy("has_planks", has(planks))
                    .save(output);
            shaped(RecipeCategory.DECORATIONS, ((net.neoforged.neoforge.registries.DeferredItem<?>) wood[1]).get())
                    .define('S', slab)
                    .define('R', Tags.Items.RODS_WOODEN)
                    .pattern("SSS")
                    .pattern("R R")
                    .pattern("R R")
                    .unlockedBy("has_slab", has(slab))
                    .save(output);
        }
    }

    private void cowboyGear() {
        // L = leather, B = brass nugget (buckles/buttons), N = iron nugget (spurs)
        shaped(RecipeCategory.COMBAT, ModItems.COWBOY_HAT.get())
                .define('L', Tags.Items.LEATHERS)
                .pattern(" L ")
                .pattern("LLL")
                .unlockedBy("has_leather", has(Tags.Items.LEATHERS))
                .save(output);
        shaped(RecipeCategory.COMBAT, ModItems.DUSTER.get())
                .define('L', Tags.Items.LEATHERS)
                .define('B', ModTags.Items.NUGGETS_BRASS)
                .pattern("L L")
                .pattern("LBL")
                .pattern("LLL")
                .unlockedBy("has_leather", has(Tags.Items.LEATHERS))
                .save(output);
        shaped(RecipeCategory.COMBAT, ModItems.CHAPS.get())
                .define('L', Tags.Items.LEATHERS)
                .define('B', ModTags.Items.NUGGETS_BRASS)
                .pattern("LBL")
                .pattern("L L")
                .pattern("L L")
                .unlockedBy("has_leather", has(Tags.Items.LEATHERS))
                .save(output);
        shaped(RecipeCategory.COMBAT, ModItems.COWBOY_BOOTS.get())
                .define('L', Tags.Items.LEATHERS)
                .define('N', Tags.Items.NUGGETS_IRON)
                .pattern("L L")
                .pattern("L L")
                .pattern("N N")
                .unlockedBy("has_leather", has(Tags.Items.LEATHERS))
                .save(output);
        // Snakeskin Boots: cowboy boots + 2 snakeskin; the boots keep their wear, enchantments and name.
        TransmuteRecipeBuilder.transmute(RecipeCategory.COMBAT, Ingredient.of(ModItems.COWBOY_BOOTS.get()), Ingredient.of(ModItems.SNAKESKIN.get()),
                        ModItems.SNAKESKIN_BOOTS.get())
                .setMaterialCount(MinMaxBounds.Ints.exactly(2))
                .unlockedBy("has_snakeskin", has(ModItems.SNAKESKIN.get()))
                .save(output);

        // Dynamite: a paper stick of blasting powder with a string fuse, two to a batch.
        shapeless(RecipeCategory.COMBAT, ModItems.DYNAMITE.get(), 2)
                .requires(Items.PAPER)
                .requires(Items.GUNPOWDER)
                .requires(Items.GUNPOWDER)
                .requires(Tags.Items.STRINGS)
                .unlockedBy("has_gunpowder", has(Items.GUNPOWDER))
                .save(output);

        shapeless(RecipeCategory.TOOLS, ModItems.LASSO.get())
                .requires(Items.LEAD)
                .requires(Tags.Items.STRINGS)
                .requires(Tags.Items.STRINGS)
                .unlockedBy("has_lead", has(Items.LEAD))
                .save(output);

        // Bandana (for your cat): a scrap of wool and a string tie makes two; dye it any colour like leather.
        shapeless(RecipeCategory.MISC, ModItems.BANDANA.get(), 2)
                .requires(ItemTags.WOOL)
                .requires(Tags.Items.STRINGS)
                .unlockedBy("has_string", has(Tags.Items.STRINGS))
                .save(output);
        // Like vanilla's dyedItem(), which would write a minecraft: id.
        Item bandana = ModItems.BANDANA.get();
        CustomCraftingRecipeBuilder.customCrafting(RecipeCategory.MISC,
                        (common, book) -> new DyeRecipe(common, book, Ingredient.of(bandana), tag(ItemTags.DYES), new ItemStackTemplate(bandana)))
                .unlockedBy(getHasName(bandana), has(bandana))
                .group("bandana")
                .save(output, ResourceKey.create(Registries.RECIPE, HollowFrontier.id("bandana_dyed")));

        shaped(RecipeCategory.TOOLS, ModItems.IRON_HORSESHOES.get())
                .define('I', Tags.Items.INGOTS_IRON)
                .define('N', Tags.Items.NUGGETS_IRON)
                .pattern("INI")
                .pattern("I I")
                .unlockedBy("has_iron_ingot", has(Tags.Items.INGOTS_IRON))
                .save(output);
        shaped(RecipeCategory.TOOLS, ModItems.BRASS_HORSESHOES.get())
                .define('B', ModTags.Items.INGOTS_BRASS)
                .define('N', Tags.Items.NUGGETS_IRON)
                .pattern("BNB")
                .pattern("B B")
                .unlockedBy("has_brass_ingot", has(ModTags.Items.INGOTS_BRASS))
                .save(output);
        shaped(RecipeCategory.TOOLS, ModItems.SADDLEBAGS.get())
                .define('L', Tags.Items.LEATHERS)
                .define('S', Tags.Items.STRINGS)
                .define('C', Tags.BlockItems.CHESTS_WOODEN.item())
                .pattern("SLS")
                .pattern("LCL")
                .unlockedBy("has_leather", has(Tags.Items.LEATHERS))
                .save(output);
    }

    private void guns() {
        // Black powder ammo: a twist of paper holding the powder charge and lead (iron) shot, plus a copper cap per load.
        shapeless(RecipeCategory.COMBAT, ModItems.PERCUSSION_CAP.get(), 16)
                .requires(Tags.Items.INGOTS_COPPER)
                .requires(Items.GUNPOWDER)
                .unlockedBy("has_gunpowder", has(Items.GUNPOWDER))
                .save(output);
        shapeless(RecipeCategory.COMBAT, ModItems.REVOLVER_CARTRIDGE.get(), 6)
                .requires(Items.PAPER)
                .requires(Items.GUNPOWDER)
                .requires(Tags.Items.NUGGETS_IRON)
                .unlockedBy("has_gunpowder", has(Items.GUNPOWDER))
                .save(output);
        shapeless(RecipeCategory.COMBAT, ModItems.RIFLE_CARTRIDGE.get(), 4)
                .requires(Items.PAPER)
                .requires(Items.GUNPOWDER)
                .requires(Tags.Items.NUGGETS_IRON)
                .requires(Tags.Items.NUGGETS_IRON)
                .unlockedBy("has_gunpowder", has(Items.GUNPOWDER))
                .save(output);
        shapeless(RecipeCategory.COMBAT, ModItems.BUCKSHOT_CARTRIDGE.get(), 4)
                .requires(Items.PAPER)
                .requires(Items.GUNPOWDER)
                .requires(Tags.Items.NUGGETS_IRON)
                .requires(Tags.Items.NUGGETS_IRON)
                .requires(Tags.Items.NUGGETS_IRON)
                .unlockedBy("has_gunpowder", has(Items.GUNPOWDER))
                .save(output);

        // I = iron barrel, G = brass gear (action), P = brass plate (frame), W = planks (grip/stock)
        shaped(RecipeCategory.COMBAT, ModItems.REVOLVER.get())
                .define('I', Tags.Items.INGOTS_IRON)
                .define('G', ModTags.Items.GEARS_BRASS)
                .define('P', ModTags.Items.PLATES_BRASS)
                .define('W', ItemTags.PLANKS)
                .pattern("IIP")
                .pattern(" GW")
                .unlockedBy("has_brass_gear", has(ModTags.Items.GEARS_BRASS))
                .save(output);
        shaped(RecipeCategory.COMBAT, ModItems.PLAINS_RIFLE.get())
                .define('I', Tags.Items.INGOTS_IRON)
                .define('G', ModTags.Items.GEARS_BRASS)
                .define('P', ModTags.Items.PLATES_BRASS)
                .define('W', ItemTags.PLANKS)
                .pattern("III")
                .pattern("PGW")
                .pattern("  W")
                .unlockedBy("has_brass_gear", has(ModTags.Items.GEARS_BRASS))
                .save(output);
        shaped(RecipeCategory.COMBAT, ModItems.SHOTGUN.get())
                .define('I', Tags.Items.INGOTS_IRON)
                .define('G', ModTags.Items.GEARS_BRASS)
                .define('P', ModTags.Items.PLATES_BRASS)
                .define('W', ItemTags.PLANKS)
                .pattern("II ")
                .pattern("IGP")
                .pattern(" WW")
                .unlockedBy("has_brass_gear", has(ModTags.Items.GEARS_BRASS))
                .save(output);
    }

    private void materials() {
        storage(ModItems.BRASS_INGOT.get(), ModItems.BRASS_BLOCK.get(), "brass_ingot_from_block", "brass_block");
        storage(ModItems.BRASS_NUGGET.get(), ModItems.BRASS_INGOT.get(), "brass_nugget", "brass_ingot_from_nuggets");

        // Brass alloy: 3 copper and a gold nugget make 4 (copper is plentiful and vanilla barely uses it). Tag inputs so
        // other mods' copper and gold work too. Gated by hollowfrontier:brass_alloying: off by default when another mod adds brass
        // (BrassConfig), so we don't undercut that mod's way of making it.
        shapeless(RecipeCategory.MISC, ModItems.BRASS_INGOT.get(), 4)
                .requires(Tags.Items.INGOTS_COPPER)
                .requires(Tags.Items.INGOTS_COPPER)
                .requires(Tags.Items.INGOTS_COPPER)
                .requires(Tags.Items.NUGGETS_GOLD)
                .unlockedBy("has_copper_ingot", has(Tags.Items.INGOTS_COPPER))
                .save(output.withConditions(BrassAlloyingCondition.INSTANCE), "brass_ingot_alloying");

        shaped(RecipeCategory.MISC, ModItems.BRASS_PLATE.get(), 2)
                .define('B', ModTags.Items.INGOTS_BRASS)
                .pattern("BB")
                .pattern("BB")
                .unlockedBy("has_brass_ingot", has(ModTags.Items.INGOTS_BRASS))
                .save(output);
        shaped(RecipeCategory.MISC, ModItems.BRASS_GEAR.get(), 2)
                .define('B', ModTags.Items.INGOTS_BRASS)
                .define('N', Tags.Items.NUGGETS_IRON)
                .pattern(" B ")
                .pattern("BNB")
                .pattern(" B ")
                .unlockedBy("has_brass_ingot", has(ModTags.Items.INGOTS_BRASS))
                .save(output);
        shaped(RecipeCategory.MISC, ModItems.PRESSURE_VALVE.get())
                .define('G', ModTags.Items.GEARS_BRASS)
                .define('P', ModTags.Items.PLATES_BRASS)
                .define('C', Tags.Items.INGOTS_COPPER)
                .define('R', Items.REDSTONE)
                .pattern(" G ")
                .pattern("PCP")
                .pattern(" R ")
                .unlockedBy("has_brass_gear", has(ModTags.Items.GEARS_BRASS))
                .save(output);
    }

    // 9 small <-> 1 big, saved under our namespace (vanilla's helper writes minecraft: ids).
    private void storage(ItemLike small, ItemLike big, String unpackId, String packId) {
        save(shapeless(RecipeCategory.MISC, small, 9)
                .requires(big)
                .unlockedBy(getHasName(big), has(big)), small, unpackId);
        save(shaped(RecipeCategory.MISC, big)
                .define('#', small)
                .pattern("###")
                .pattern("###")
                .pattern("###")
                .unlockedBy(getHasName(small), has(small)), big, packId);
    }

    private void mint(ItemLike coin, ItemLike nugget) {
        shaped(RecipeCategory.MISC, coin)
                .define('#', nugget)
                .pattern("##")
                .pattern("##")
                .unlockedBy(getHasName(nugget), has(nugget))
                .save(output);
    }

    // RecipeBuilder.save(output, id) throws if id equals the default (the result's item id).
    private void save(RecipeBuilder builder, ItemLike result, String id) {
        if (id.equals(getSimpleRecipeName(result))) {
            builder.save(output);
        } else {
            builder.save(output, id);
        }
    }
}
