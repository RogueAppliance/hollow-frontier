package com.hollowfrontier.gametest;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import com.hollowfrontier.content.frontier.gun.GunItem;
import com.hollowfrontier.content.steam.SteamCanisterItem;
import com.hollowfrontier.registry.ModItems;
import com.hollowfrontier.registry.ModTabs;

import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.SpawnEggItem;

// The creative menu (ModTabs): every item is somewhere in a mod tab, variants are there, vanilla tabs get their share.
final class TabTests {
    private TabTests() {}

    private static void build(GameTestHelper h) {
        ServerLevel level = h.getLevel();
        CreativeModeTabs.tryRebuildTabContents(level.enabledFeatures(), true, level.registryAccess());
    }

    private static List<ItemStack> contents(ResourceKey<CreativeModeTab> tab) {
        return new ArrayList<>(BuiltInRegistries.CREATIVE_MODE_TAB.getValueOrThrow(tab).getDisplayItems());
    }

    /** Fails for any registered mod item that no mod tab lists: add it to its section in ModTabs. */
    static void everyItemInATab(GameTestHelper h) {
        build(h);
        Set<Item> listed = new HashSet<>();
        for (var tab : ModTabs.modTabs()) {
            List<ItemStack> items = contents(tab.key());
            h.assertTrue(!items.isEmpty(), "empty tab " + tab.getId());
            items.forEach(stack -> listed.add(stack.getItem()));
        }
        List<String> missing = new ArrayList<>();
        for (var item : ModItems.ITEMS.getEntries()) {
            if (!listed.contains(item.get())) {
                missing.add(item.getId().toString());
            }
        }
        h.assertTrue(missing.isEmpty(), "in no mod tab (add them to ModTabs): " + missing);
        h.succeed();
    }

    /**
     * Loaded guns, a full canister and every bandana colour are entries of their own; the one tab (32a) runs frontier,
     * town building, steam, Hollow Earth, Concavia, the Grays, then every spawn egg.
     */
    static void tabVariantsAndOrder(GameTestHelper h) {
        build(h);
        List<ItemStack> tab = contents(ModTabs.HOLLOW_FRONTIER.key());
        h.assertTrue(tab.get(0).is(ModItems.REVOLVER.get()) && GunItem.getAmmo(tab.get(0)) == 0
                && tab.get(1).is(ModItems.REVOLVER.get()) && GunItem.getAmmo(tab.get(1)) == 6, "the tab should open with an empty and a loaded revolver");
        long bandanaColours = tab.stream().filter(s -> s.is(ModItems.BANDANA.get()))
                .map(s -> s.has(DataComponents.DYED_COLOR) ? s.get(DataComponents.DYED_COLOR).rgb() : -1).distinct().count();
        h.assertTrue(bandanaColours == 16, "expected 16 bandana colours, got " + bandanaColours);
        int lastEgg = -1, firstGun = Integer.MAX_VALUE;
        for (int i = 0; i < tab.size(); i++) {
            if (tab.get(i).getItem() instanceof SpawnEggItem) lastEgg = i;
            if (tab.get(i).getItem() instanceof GunItem) firstGun = Math.min(firstGun, i);
        }
        h.assertTrue(lastEgg == tab.size() - 1 && firstGun == 0, "spawn eggs should close the tab and guns open it");
        int firstEgg = 0;
        while (!(tab.get(firstEgg).getItem() instanceof SpawnEggItem)) {
            firstEgg++;
        }
        h.assertTrue(tab.stream().skip(firstEgg).allMatch(s -> s.getItem() instanceof SpawnEggItem), "the spawn eggs should be together at the end");
        List<Item> sections = List.of(ModItems.REVOLVER.get(), ModItems.SALOON_DOORS.get(), ModItems.BOILER.get(), ModItems.CRUST_ROCK.get(),
                ModItems.CONCAVIAN_CRYSTAL.get(), ModItems.SAUCER_HULL.get(), ModItems.OUTLAW_SPAWN_EGG.get());
        for (int i = 1; i < sections.size(); i++) {
            h.assertTrue(indexOf(tab, sections.get(i - 1)) < indexOf(tab, sections.get(i)), "sections out of order at " + sections.get(i));
        }
        h.assertTrue(tab.stream().anyMatch(s -> s.is(ModItems.STEAM_CANISTER.get()) && SteamCanisterItem.steam(s) == SteamCanisterItem.CAPACITY)
                && tab.stream().anyMatch(s -> s.is(ModItems.STEAM_CANISTER.get()) && SteamCanisterItem.steam(s) == 0), "need an empty and a full canister");
        h.succeed();
    }

    /** Mod items also turn up in vanilla tabs, next to their relatives. */
    static void vanillaTabsGetModItems(GameTestHelper h) {
        build(h);
        List<ItemStack> combat = contents(CreativeModeTabs.COMBAT);
        int crossbow = indexOf(combat, Items.CROSSBOW);
        h.assertTrue(crossbow >= 0 && combat.get(crossbow + 1).is(ModItems.REVOLVER.get()), "the revolver should follow the crossbow in Combat");
        h.assertTrue(indexOf(contents(CreativeModeTabs.SPAWN_EGGS), ModItems.OUTLAW_SPAWN_EGG.get()) >= 0, "no outlaw egg in Spawn Eggs");
        h.assertTrue(indexOf(contents(CreativeModeTabs.FOOD_AND_DRINKS), ModItems.RYE_WHISKEY.get()) >= 0, "no whiskey in Food & Drinks");
        h.assertTrue(indexOf(contents(CreativeModeTabs.FUNCTIONAL_BLOCKS), ModItems.BOILER.get()) >= 0, "no boiler in Functional Blocks");
        h.succeed();
    }

    private static int indexOf(List<ItemStack> stacks, Item item) {
        for (int i = 0; i < stacks.size(); i++) {
            if (stacks.get(i).is(item)) return i;
        }
        return -1;
    }
}
