package com.hollowfrontier.registry;

import java.util.List;
import java.util.function.Consumer;
import java.util.function.Supplier;

import com.hollowfrontier.HollowFrontier;
import com.hollowfrontier.content.frontier.cat.TownCatOdds;
import com.hollowfrontier.content.frontier.gun.GunItem;
import com.hollowfrontier.content.steam.SteamCanisterItem;

import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.ItemLike;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.BuildCreativeModeTabContentsEvent;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

/**
 * The creative menu. This file is the one place that decides where every mod item shows up:
 * <ul>
 * <li><b>The Hollow Frontier tab</b>, after the vanilla ones (one tab since 32a; it was six). It lists the mod section by
 *     section, the way a game goes: the frontier, building a town, steam, the Hollow Earth, Concavia, the Grays, and every
 *     spawn egg at the end. Within a section, things come in the order players look for them: materials, tools and
 *     weapons, ammo, armor, gear, consumables, blocks. Useful variants (loaded guns, a full canister, every bandana
 *     colour) are entries of their own.</li>
 * <li><b>Vanilla tabs</b> ({@link #addToVanillaTabs}): the same items again next to their vanilla relatives.</li>
 * </ul>
 * A new item has to be added to a section here; the {@code every_item_in_a_tab} game test fails until it is.
 */
@EventBusSubscriber(modid = HollowFrontier.MODID)
public final class ModTabs {
    private ModTabs() {}

    public static final DeferredRegister<CreativeModeTab> TABS =
            DeferredRegister.create(Registries.CREATIVE_MODE_TAB, HollowFrontier.MODID);

    public static final DeferredHolder<CreativeModeTab, CreativeModeTab> HOLLOW_FRONTIER =
            tab("hollow_frontier", CreativeModeTabs.SPAWN_EGGS, ModItems.WANTED_POSTER, ModTabs::hollowFrontier);

    /** The mod's own tabs, in menu order (the coverage test walks these). */
    public static List<DeferredHolder<CreativeModeTab, CreativeModeTab>> modTabs() {
        return List.of(HOLLOW_FRONTIER);
    }

    /** Every spawn egg, in the order the tab and the vanilla Spawn Eggs tab list them: the frontier's folk and wildlife, the Hollow's creatures, Concavia, the Grays. */
    private static List<Supplier<? extends ItemLike>> spawnEggs() {
        return List.of(ModItems.OUTLAW_SPAWN_EGG, ModItems.OUTLAW_GUNSLINGER_SPAWN_EGG, ModItems.OUTLAW_BOSS_SPAWN_EGG,
                ModItems.LAWMAN_SPAWN_EGG, ModItems.TOWNSFOLK_SPAWN_EGG, ModItems.BARMAID_SPAWN_EGG, ModItems.RATTLESNAKE_SPAWN_EGG,
                ModItems.MAN_IN_BLACK_SPAWN_EGG,
                ModItems.FERN_SKITTER_SPAWN_EGG, ModItems.GLASSBACK_HARE_SPAWN_EGG, ModItems.ASH_SALAMANDER_SPAWN_EGG, ModItems.PALE_RAVEN_SPAWN_EGG,
                ModItems.GRAY_PROBE_SPAWN_EGG, ModItems.WOOLLY_MAMMOTH_SPAWN_EGG, ModItems.TERROR_BIRD_SPAWN_EGG,
                ModItems.MEGATHERIUM_SPAWN_EGG, ModItems.GLYPTODON_SPAWN_EGG, ModItems.GRAY_SENTINEL_SPAWN_EGG,
                ModItems.KINDLED_SPAWN_EGG, ModItems.CRYSTAL_GUARDIAN_SPAWN_EGG, ModItems.SUN_ARCHON_SPAWN_EGG,
                ModItems.GRAY_WARDEN_SPAWN_EGG, ModItems.DISGUISED_GRAY_SPAWN_EGG);
    }

    private static void hollowFrontier(Consumer<ItemStack> out) {
        frontier(out);
        frontierBuilding(out);
        steam(out);
        hollowEarth(out);
        concavia(out);
        grays(out);
        spawnEggs().forEach(egg -> out.accept(new ItemStack(egg.get())));
    }

    // ---------------------------------------------------------------- Frontier: the cowboy's kit

    private static void frontier(Consumer<ItemStack> out) {
        // Weapons, each empty then fully loaded, and the thrown ones
        gun(out, ModItems.REVOLVER.get());
        gun(out, ModItems.PLAINS_RIFLE.get());
        gun(out, ModItems.SHOTGUN.get());
        add(out, ModItems.DYNAMITE, ModItems.LASSO);
        // Ammo
        add(out, ModItems.PERCUSSION_CAP, ModItems.REVOLVER_CARTRIDGE, ModItems.RIFLE_CARTRIDGE, ModItems.BUCKSHOT_CARTRIDGE);
        // Armor
        add(out, ModItems.COWBOY_HAT, ModItems.DUSTER, ModItems.CHAPS, ModItems.COWBOY_BOOTS, ModItems.SNAKESKIN_BOOTS);
        // Horse gear
        add(out, ModItems.IRON_HORSESHOES, ModItems.BRASS_HORSESHOES, ModItems.SADDLEBAGS);
        // Money, the law and the news
        add(out, ModItems.COPPER_COIN, ModItems.GOLD_COIN, ModItems.WANTED_POSTER, ModItems.OUTLAW_BADGE, ModItems.FRONTIER_GAZETTE, ModItems.SURVEY_LETTER);
        // The frontier's mysteries (11a)
        add(out, ModItems.FLATTENED_WHEAT, ModItems.STRANGE_SKULL);
        // Drinks, soft to hard, then the doctor's
        add(out, ModItems.SARSAPARILLA, ModItems.COWBOY_COFFEE, ModItems.FRONTIER_BEER, ModItems.RYE_WHISKEY, ModItems.MEZCAL,
                ModItems.MOONSHINE, ModItems.SNAKE_OIL);
        // Cat accessories: the bandana as crafted (red), then every other colour, and the star
        bandanas(out);
        add(out, ModItems.BRASS_STAR);
        // Critter drops
        add(out, ModItems.SNAKESKIN);
    }

    // ---------------------------------------------------------------- Frontier Building: furnishing a town

    private static void frontierBuilding(Consumer<ItemStack> out) {
        // The saloon
        add(out, ModItems.SALOON_DOORS, ModItems.BAR_COUNTER, ModItems.BAR_STOOL, ModItems.SALOON_PIANO, ModItems.CARD_TABLE);
        // Chairs and tables (24c)
        add(out, ModItems.OAK_CHAIR, ModItems.SPRUCE_CHAIR, ModItems.DARK_OAK_CHAIR, ModItems.OAK_TABLE, ModItems.SPRUCE_TABLE, ModItems.DARK_OAK_TABLE,
                ModItems.TOWN_LAMP);
        // The law office
        add(out, ModItems.HANGING_BOUNTY_BOARD, ModItems.BOUNTY_BOARD);
        // Metal blocks and the ore they come from
        add(out, ModItems.BRASS_BLOCK);
    }

    // ---------------------------------------------------------------- Steam: brass and what it builds

    private static void steam(Consumer<ItemStack> out) {
        // Materials: brass, parts, then crusher dust
        add(out, ModItems.BRASS_INGOT, ModItems.BRASS_NUGGET,
                ModItems.BRASS_PLATE, ModItems.BRASS_GEAR, ModItems.PRESSURE_VALVE);
        add(out, ModItems.IRON_DUST, ModItems.COPPER_DUST, ModItems.GOLD_DUST);
        // Tools: canisters empty and full, the drill, the handcar
        add(out, ModItems.STEAM_CANISTER);
        out.accept(fullCanister());
        add(out, ModItems.STEAM_DRILL, ModItems.STEAM_HANDCAR);
        // The airship: its two parts, then the ship
        add(out, ModItems.AIRSHIP_ENVELOPE, ModItems.AIRSHIP_ENGINE, ModItems.AIRSHIP);
        // The polar expedition: find a gateway, open it (the frame is for builders in creative)
        add(out, ModItems.POLAR_COMPASS, ModItems.POLAR_RESONATOR);
        // Machines, in the order a steam line is built
        add(out, ModItems.BOILER, ModItems.COPPER_PIPE, ModItems.STEAM_TANK, ModItems.PRESSURE_GAUGE, ModItems.CRUSHER);
        add(out, ModItems.GATEWAY_FRAME);
    }

    // ---------------------------------------------------------------- Hollow Earth: what's found beneath the ice

    // 9a has the crust, 9b1 the flora and ash, 9b2 the creatures' eggs, 9c the ores, starsteel and its gear, and phlogiston (the tab's icon).
    private static void hollowEarth(Consumer<ItemStack> out) {
        // Natural blocks
        add(out, ModItems.CRUST_ROCK, ModItems.ASH, ModItems.ASH_VENT, ModItems.FERN_FROND);
        // Ores and materials (9c)
        add(out, ModItems.STARSTEEL_ORE, ModItems.DEEPSLATE_STARSTEEL_ORE, ModItems.PHLOGISTON_ORE, ModItems.DEEPSLATE_PHLOGISTON_ORE,
                ModItems.RAW_STARSTEEL_BLOCK, ModItems.STARSTEEL_BLOCK, ModItems.PHLOGISTON_LAMP);
        add(out, ModItems.RAW_STARSTEEL, ModItems.STARSTEEL_DUST, ModItems.STARSTEEL_INGOT, ModItems.STARSTEEL_NUGGET, ModItems.PHLOGISTON_SHARD);
        // Starsteel gear
        add(out, ModItems.STARSTEEL_SWORD, ModItems.STARSTEEL_PAXEL,
                ModItems.STARSTEEL_HELMET, ModItems.STARSTEEL_CHESTPLATE, ModItems.STARSTEEL_LEGGINGS, ModItems.STARSTEEL_BOOTS,
                ModItems.STARSTEEL_HORSESHOES);
        // The megafauna's drops and what they make (10a), beast by beast
        add(out, ModItems.MAMMOTH_TUSK, ModItems.OLIPHANT, ModItems.RAW_MAMMOTH, ModItems.MAMMOTH_STEAK, ModItems.TERROR_BIRD_TALON, ModItems.TALON_KNIFE,
                ModItems.THICK_HIDE, ModItems.HIDE_BARDING, ModItems.GLYPTODON_SCUTE,
                ModItems.GLYPTODON_HELMET, ModItems.GLYPTODON_CHESTPLATE, ModItems.GLYPTODON_LEGGINGS, ModItems.GLYPTODON_BOOTS);
        // The Gray experiments (10b)
        add(out, ModItems.GRAY_TECH_FRAGMENT, ModItems.SIGNAL_LOCATOR);
    }

    // ---------------------------------------------------------------- Concavia: the crystal city and its last people (12a)

    private static void concavia(Consumer<ItemStack> out) {
        add(out, ModItems.CONCAVIAN_CRYSTAL, ModItems.CRYSTAL_BRICKS, ModItems.CRYSTAL_BRICK_STAIRS, ModItems.CRYSTAL_BRICK_SLAB,
                ModItems.CRYSTAL_BRICK_WALL, ModItems.CHISELED_CRYSTAL_BRICKS);
        add(out, ModItems.KINDLED_SIGIL, ModItems.CONCAVIAN_CODEX);
        add(out, ModItems.WAYSTONE); // 15c, for builders in creative
        // The Sun Archon (12b): the temple's pylons, and what the Archon leaves
        add(out, ModItems.SUN_PYLON, ModItems.SUNSTONE_SHARD, ModItems.PHLOGISTON_CORE, ModItems.STARSTEEL_CROWN, ModItems.SUNSTONE_LENS);
    }

    // ---------------------------------------------------------------- The Grays: their wrecks and what's in them (13a)

    private static void grays(Consumer<ItemStack> out) {
        add(out, ModItems.SAUCER_HULL, ModItems.SAUCER_WRECKAGE, ModItems.GRAY_CACHE, ModItems.GRAY_CARGO_POD);
        add(out, ModItems.ALIEN_ALLOY_PLATING, ModItems.DAMAGED_GRAVITY_DRIVE, ModItems.BEAM_EMITTER, ModItems.LENS_ARRAY, ModItems.NAVIGATION_CORE);
        // Phlogiston tech (13b)
        add(out, ModItems.EMPTY_PHLOGISTON_CELL, ModItems.PHLOGISTON_CELL, ModItems.RAY_PISTOL, ModItems.PHLOGISTON_RIFLE, ModItems.GRAVITY_DRIVE);
        add(out, ModItems.PROTOTYPE_SAUCER, ModItems.CLOAKING_DEVICE, ModItems.CARGO_HOLD); // 13c, 15b
        add(out, ModItems.PHLOGISTON_BEACON); // 13d
        add(out, ModItems.IMPLANT_RIG, ModItems.LETTER_HOME); // 20
        add(out, ModItems.KINDLED_SEAL, ModItems.SEAL_SOCKET); // 26
        ModItems.RELICS.forEach(relic -> add(out, relic)); // 28
        ModItems.GADGETS.forEach(gadget -> add(out, gadget)); // 32c
        ModItems.SHERDS.forEach(sherd -> add(out, sherd));
        add(out, ModItems.CONCAVIAN_ARMOR_TRIM_SMITHING_TEMPLATE);
    }

    // ---------------------------------------------------------------- vanilla tabs

    /** Mod items beside their vanilla relatives (parent tab only: the search tab already has them from the mod tabs). */
    @SubscribeEvent
    static void addToVanillaTabs(BuildCreativeModeTabContentsEvent event) {
        ResourceKey<CreativeModeTab> tab = event.getTabKey();
        if (tab == CreativeModeTabs.BUILDING_BLOCKS) {
            after(event, Items.GOLD_BLOCK, ModItems.BRASS_BLOCK, ModItems.STARSTEEL_BLOCK);
            after(event, Items.CHISELED_QUARTZ_BLOCK, ModItems.CONCAVIAN_CRYSTAL, ModItems.CRYSTAL_BRICKS, ModItems.CRYSTAL_BRICK_STAIRS,
                    ModItems.CRYSTAL_BRICK_SLAB, ModItems.CRYSTAL_BRICK_WALL, ModItems.CHISELED_CRYSTAL_BRICKS);
            after(event, Items.OAK_DOOR, ModItems.SALOON_DOORS);
        } else if (tab == CreativeModeTabs.NATURAL_BLOCKS) {
            after(event, Items.DEEPSLATE_COPPER_ORE, ModItems.STARSTEEL_ORE, ModItems.DEEPSLATE_STARSTEEL_ORE,
                    ModItems.PHLOGISTON_ORE, ModItems.DEEPSLATE_PHLOGISTON_ORE);
            after(event, Items.RAW_GOLD_BLOCK, ModItems.RAW_STARSTEEL_BLOCK);
            after(event, Items.CALCITE, ModItems.CRUST_ROCK);
            after(event, Items.JUNGLE_LEAVES, ModItems.FERN_FROND);
            after(event, Items.RED_SAND, ModItems.ASH);
            after(event, Items.MAGMA_BLOCK, ModItems.ASH_VENT);
        } else if (tab == CreativeModeTabs.FUNCTIONAL_BLOCKS) {
            after(event, Items.BLAST_FURNACE, ModItems.BOILER, ModItems.CRUSHER, ModItems.STEAM_TANK, ModItems.COPPER_PIPE, ModItems.PRESSURE_GAUGE);
            after(event, Items.JUKEBOX, ModItems.SALOON_PIANO);
            after(event, Items.SEA_LANTERN, ModItems.PHLOGISTON_LAMP);
            after(event, Items.LECTERN, ModItems.BOUNTY_BOARD, ModItems.BAR_COUNTER, ModItems.BAR_STOOL, ModItems.CARD_TABLE);
            after(event, Items.END_PORTAL_FRAME, ModItems.GATEWAY_FRAME);
        } else if (tab == CreativeModeTabs.TOOLS_AND_UTILITIES) {
            after(event, Items.BUCKET, ModItems.STEAM_CANISTER);
            after(event, Items.LEAD, ModItems.LASSO);
            after(event, Items.SADDLE, ModItems.IRON_HORSESHOES, ModItems.BRASS_HORSESHOES, ModItems.STARSTEEL_HORSESHOES, ModItems.SADDLEBAGS);
            after(event, Items.GOAT_HORN, ModItems.OLIPHANT);
            after(event, Items.TNT_MINECART, ModItems.STEAM_HANDCAR, ModItems.AIRSHIP);
            after(event, Items.RECOVERY_COMPASS, ModItems.POLAR_COMPASS, ModItems.POLAR_RESONATOR, ModItems.SIGNAL_LOCATOR);
            event.accept(ModItems.STEAM_DRILL.get().getDefaultInstance(), CreativeModeTab.TabVisibility.PARENT_TAB_ONLY);
            after(event, Items.DIAMOND_HOE, ModItems.STARSTEEL_PAXEL);
        } else if (tab == CreativeModeTabs.COMBAT) {
            after(event, Items.CROSSBOW, ModItems.REVOLVER, ModItems.PLAINS_RIFLE, ModItems.SHOTGUN, ModItems.RAY_PISTOL, ModItems.PHLOGISTON_RIFLE);
            after(event, Items.ARROW, ModItems.PERCUSSION_CAP, ModItems.REVOLVER_CARTRIDGE, ModItems.RIFLE_CARTRIDGE, ModItems.BUCKSHOT_CARTRIDGE);
            after(event, Items.TNT, ModItems.DYNAMITE);
            after(event, Items.DIAMOND_SWORD, ModItems.STARSTEEL_SWORD);
            after(event, Items.IRON_SWORD, ModItems.TALON_KNIFE);
            after(event, Items.IRON_BOOTS, ModItems.GLYPTODON_HELMET, ModItems.GLYPTODON_CHESTPLATE, ModItems.GLYPTODON_LEGGINGS, ModItems.GLYPTODON_BOOTS);
            after(event, Items.GOLDEN_HORSE_ARMOR, ModItems.HIDE_BARDING);
            after(event, Items.DIAMOND_BOOTS, ModItems.STARSTEEL_HELMET, ModItems.STARSTEEL_CHESTPLATE, ModItems.STARSTEEL_LEGGINGS, ModItems.STARSTEEL_BOOTS);
            after(event, Items.LEATHER_BOOTS, ModItems.COWBOY_HAT, ModItems.DUSTER, ModItems.CHAPS, ModItems.COWBOY_BOOTS, ModItems.SNAKESKIN_BOOTS);
        } else if (tab == CreativeModeTabs.FOOD_AND_DRINKS) {
            after(event, Items.COOKED_BEEF, ModItems.RAW_MAMMOTH, ModItems.MAMMOTH_STEAK);
            after(event, Items.HONEY_BOTTLE, ModItems.SARSAPARILLA, ModItems.COWBOY_COFFEE, ModItems.FRONTIER_BEER, ModItems.RYE_WHISKEY,
                    ModItems.MEZCAL, ModItems.MOONSHINE, ModItems.SNAKE_OIL);
        } else if (tab == CreativeModeTabs.INGREDIENTS) {
            after(event, Items.RAW_GOLD, ModItems.RAW_STARSTEEL);
            after(event, Items.DIAMOND, ModItems.PHLOGISTON_SHARD);
            after(event, Items.GOLD_INGOT, ModItems.STARSTEEL_INGOT, ModItems.STARSTEEL_NUGGET, ModItems.STARSTEEL_DUST);
            after(event, Items.GOLD_NUGGET, ModItems.BRASS_NUGGET);
            after(event, Items.COPPER_INGOT, ModItems.BRASS_INGOT, ModItems.BRASS_PLATE, ModItems.BRASS_GEAR,
                    ModItems.PRESSURE_VALVE, ModItems.IRON_DUST, ModItems.COPPER_DUST, ModItems.GOLD_DUST, ModItems.SNAKESKIN);
            after(event, Items.PHANTOM_MEMBRANE, ModItems.AIRSHIP_ENVELOPE, ModItems.AIRSHIP_ENGINE);
            after(event, Items.ARMADILLO_SCUTE, ModItems.GLYPTODON_SCUTE, ModItems.THICK_HIDE, ModItems.MAMMOTH_TUSK, ModItems.TERROR_BIRD_TALON, ModItems.GRAY_TECH_FRAGMENT);
        } else if (tab == CreativeModeTabs.SPAWN_EGGS) {
            for (var egg : spawnEggs()) {
                event.accept(new ItemStack(egg.get()), CreativeModeTab.TabVisibility.PARENT_TAB_ONLY);
            }
        }
    }

    // Puts items one after another behind a vanilla anchor; at the end of the tab if the anchor isn't there (feature flags).
    @SafeVarargs
    private static void after(BuildCreativeModeTabContentsEvent event, Item anchor, Supplier<? extends ItemLike>... items) {
        ItemStack previous = new ItemStack(anchor);
        boolean anchored = event.getParentEntries().contains(previous);
        for (Supplier<? extends ItemLike> item : items) {
            ItemStack stack = new ItemStack(item.get());
            if (anchored) {
                event.insertAfter(previous, stack, CreativeModeTab.TabVisibility.PARENT_TAB_ONLY);
                previous = stack;
            } else {
                event.accept(stack, CreativeModeTab.TabVisibility.PARENT_TAB_ONLY);
            }
        }
    }

    // ---------------------------------------------------------------- helpers

    @SafeVarargs
    private static void add(Consumer<ItemStack> out, Supplier<? extends ItemLike>... items) {
        for (Supplier<? extends ItemLike> item : items) {
            out.accept(new ItemStack(item.get()));
        }
    }

    private static void gun(Consumer<ItemStack> out, GunItem gun) {
        out.accept(new ItemStack(gun));
        ItemStack loaded = new ItemStack(gun);
        GunItem.setAmmo(loaded, gun.stats().magazine());
        out.accept(loaded);
    }

    private static void bandanas(Consumer<ItemStack> out) {
        out.accept(new ItemStack(ModItems.BANDANA.get())); // undyed bandanas are red
        for (DyeColor color : DyeColor.values()) {
            if (color != DyeColor.RED) {
                out.accept(TownCatOdds.bandana(color));
            }
        }
    }

    public static ItemStack fullCanister() {
        ItemStack canister = new ItemStack(ModItems.STEAM_CANISTER.get());
        SteamCanisterItem.setSteam(canister, SteamCanisterItem.CAPACITY);
        return canister;
    }

    public static String titleKey(String name) {
        return "itemGroup." + HollowFrontier.MODID + "." + name;
    }

    private static DeferredHolder<CreativeModeTab, CreativeModeTab> tab(String name, ResourceKey<CreativeModeTab> after,
            Supplier<? extends ItemLike> icon, Consumer<Consumer<ItemStack>> contents) {
        return TABS.register(name, () -> CreativeModeTab.builder()
                .title(Component.translatable(titleKey(name)))
                .withTabsBefore(after)
                .icon(() -> new ItemStack(icon.get()))
                .displayItems((parameters, output) -> contents.accept(output::accept))
                .build());
    }
}
