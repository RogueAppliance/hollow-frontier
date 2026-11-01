package com.hollowfrontier.client;

import java.util.List;

import com.hollowfrontier.HollowFrontier;
import com.hollowfrontier.content.concavia.WaystoneMenu;
import com.hollowfrontier.content.concavia.Waystones;
import com.hollowfrontier.registry.ModMenus;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RegisterMenuScreensEvent;

/**
 * Where a waystone can take you (15c): a pale stone panel, a button for each destination ({@link #PER_PAGE} a page, with
 * paging when there are more), and the fare. Flat fills and the font, like the card table, so it needs no GUI texture.
 */
@EventBusSubscriber(modid = HollowFrontier.MODID, value = Dist.CLIENT)
public class WaystoneScreen extends AbstractContainerScreen<WaystoneMenu> {
    private static final int WIDTH = 196, HEIGHT = 176, PER_PAGE = 7;
    private static final int STONE = 0xFFD8DEE4, STONE_EDGE = 0xFF5A6A78, INSET = 0xFFB8C4CE, TEXT = 0xFF203040, DIM = 0xFF4A6070;

    private int page;

    public WaystoneScreen(WaystoneMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title, WIDTH, HEIGHT);
    }

    @SubscribeEvent
    static void registerScreens(RegisterMenuScreensEvent event) {
        event.register(ModMenus.WAYSTONE.get(), WaystoneScreen::new);
    }

    private int pages() {
        return Math.max(1, (menu.labels().size() + PER_PAGE - 1) / PER_PAGE);
    }

    @Override
    protected void init() {
        super.init();
        clearWidgets();
        List<Component> labels = menu.labels();
        int x = leftPos + 10, y = topPos + 22;
        for (int i = page * PER_PAGE; i < Math.min(labels.size(), (page + 1) * PER_PAGE); i++) {
            int id = i;
            addRenderableWidget(Button.builder(labels.get(i), b -> minecraft.gameMode.handleInventoryButtonClick(menu.containerId, id))
                    .bounds(x, y, WIDTH - 20, 18).build());
            y += 20;
        }
        if (pages() > 1) {
            Button back = addRenderableWidget(Button.builder(Component.literal("<"), b -> turn(-1)).bounds(leftPos + 10, topPos + HEIGHT - 26, 20, 16).build());
            Button on = addRenderableWidget(Button.builder(Component.literal(">"), b -> turn(1)).bounds(leftPos + WIDTH - 30, topPos + HEIGHT - 26, 20, 16).build());
            back.active = page > 0;
            on.active = page < pages() - 1;
        }
    }

    private void turn(int by) {
        page = Math.clamp(page + by, 0, pages() - 1);
        init();
    }

    @Override
    public void extractBackground(GuiGraphicsExtractor g, int mouseX, int mouseY, float a) {
        super.extractBackground(g, mouseX, mouseY, a);
        int x = leftPos, y = topPos;
        g.fill(x, y, x + WIDTH, y + HEIGHT, STONE_EDGE);
        g.fill(x + 1, y + 1, x + WIDTH - 1, y + HEIGHT - 1, STONE);
        g.fill(x + 6, y + 18, x + WIDTH - 6, y + HEIGHT - 32, INSET);
        Component fare = Component.translatable("gui.hollowfrontier.waystone.fare", Waystones.FARE);
        g.centeredText(font, fare, x + WIDTH / 2, y + HEIGHT - 22, DIM);
    }

    @Override
    protected void extractLabels(GuiGraphicsExtractor g, int mouseX, int mouseY) {
        g.text(font, title, 8, 6, TEXT, false);
        if (pages() > 1) {
            String count = (page + 1) + "/" + pages();
            g.text(font, count, WIDTH - 8 - font.width(count), 6, DIM, false);
        }
    }
}
