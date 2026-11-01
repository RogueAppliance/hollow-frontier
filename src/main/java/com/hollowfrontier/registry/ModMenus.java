package com.hollowfrontier.registry;

import com.hollowfrontier.HollowFrontier;
import com.hollowfrontier.content.steam.BoilerMenu;
import com.hollowfrontier.content.steam.CrusherMenu;
import com.hollowfrontier.content.frontier.saloon.BlackjackMenu;
import com.hollowfrontier.content.concavia.WaystoneMenu;

import net.minecraft.core.registries.Registries;
import net.minecraft.world.flag.FeatureFlags;
import net.minecraft.world.inventory.MenuType;
import net.neoforged.neoforge.common.extensions.IMenuTypeExtension;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class ModMenus {
    private ModMenus() {}

    public static final DeferredRegister<MenuType<?>> MENUS =
            DeferredRegister.create(Registries.MENU, HollowFrontier.MODID);

    public static final DeferredHolder<MenuType<?>, MenuType<BoilerMenu>> BOILER = MENUS.register("boiler",
            () -> new MenuType<>(BoilerMenu::new, FeatureFlags.DEFAULT_FLAGS));
    public static final DeferredHolder<MenuType<?>, MenuType<CrusherMenu>> CRUSHER = MENUS.register("crusher",
            () -> new MenuType<>(CrusherMenu::new, FeatureFlags.DEFAULT_FLAGS));
    public static final DeferredHolder<MenuType<?>, MenuType<BlackjackMenu>> BLACKJACK = MENUS.register("blackjack",
            () -> new MenuType<>(BlackjackMenu::new, FeatureFlags.DEFAULT_FLAGS));
    // 15c: the waystone's destinations, their names sent along with it
    public static final DeferredHolder<MenuType<?>, MenuType<WaystoneMenu>> WAYSTONE = MENUS.register("waystone",
            () -> IMenuTypeExtension.create(WaystoneMenu::new));
}
