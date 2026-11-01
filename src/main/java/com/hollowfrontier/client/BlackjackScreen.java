package com.hollowfrontier.client;

import java.util.ArrayList;
import java.util.List;

import com.hollowfrontier.HollowFrontier;
import com.hollowfrontier.content.frontier.saloon.Blackjack;
import com.hollowfrontier.content.frontier.saloon.BlackjackMenu;
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
 * The card table (17): the dealer's hand across the top of the felt, the player's below, the bet and the buttons along the
 * bottom. Like the steam machine screens it's drawn with flat fills and the font (cards included: a white face with its
 * rank and suit, a red lattice back), so it needs no GUI texture.
 */
@EventBusSubscriber(modid = HollowFrontier.MODID, value = Dist.CLIENT)
public class BlackjackScreen extends AbstractContainerScreen<BlackjackMenu> {
    private static final int WIDTH = 208, HEIGHT = 164;
    private static final int CARD_W = 20, CARD_H = 28;
    private static final int WOOD = 0xFF4E3018, WOOD_EDGE = 0xFF2A180A, FELT = 0xFF1F5A34, FELT_LINE = 0xFF2C7444;
    private static final int CARD_FACE = 0xFFF4EFE2, CARD_EDGE = 0xFF5A5246, RED = 0xFFB0202A, BLACK = 0xFF1A1A1A;
    private static final int BACK = 0xFF8E1C22, BACK_LINE = 0xFFC8A040, TEXT = 0xFFF0E6C8, DIM = 0xFFB8C8B0;
    private static final String[] RANKS = {"A", "2", "3", "4", "5", "6", "7", "8", "9", "10", "J", "Q", "K"};
    private static final String[] SUITS = {"♠", "♥", "♦", "♣"};

    private final List<Button> buttons = new ArrayList<>();
    private final List<Integer> buttonIds = new ArrayList<>();

    public BlackjackScreen(BlackjackMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title, WIDTH, HEIGHT);
    }

    @SubscribeEvent
    static void registerScreens(RegisterMenuScreensEvent event) {
        event.register(ModMenus.BLACKJACK.get(), BlackjackScreen::new);
    }

    @Override
    protected void init() {
        super.init();
        buttons.clear();
        buttonIds.clear();
        int x = leftPos, y = topPos;
        button(BlackjackMenu.BET_LESS_5, "-5", x + 8, y + 120, 22);
        button(BlackjackMenu.BET_LESS, "-1", x + 32, y + 120, 22);
        button(BlackjackMenu.BET_MORE, "+1", x + WIDTH - 54, y + 120, 22);
        button(BlackjackMenu.BET_MORE_5, "+5", x + WIDTH - 30, y + 120, 22);
        String[] actions = {"deal", "hit", "stand", "double"};
        int[] ids = {BlackjackMenu.DEAL, BlackjackMenu.HIT, BlackjackMenu.STAND, BlackjackMenu.DOUBLE};
        for (int i = 0; i < actions.length; i++) {
            button(ids[i], Component.translatable("gui.hollowfrontier.blackjack." + actions[i]), x + 8 + i * 49, y + 140, 45);
        }
    }

    private void button(int id, String label, int x, int y, int width) {
        button(id, Component.literal(label), x, y, width);
    }

    private void button(int id, Component label, int x, int y, int width) {
        Button button = Button.builder(label, b -> minecraft.gameMode.handleInventoryButtonClick(menu.containerId, id)).bounds(x, y, width, 16).build();
        buttons.add(addRenderableWidget(button));
        buttonIds.add(id);
    }

    @Override
    protected void containerTick() {
        super.containerTick();
        for (int i = 0; i < buttons.size(); i++) {
            buttons.get(i).active = menu.allowed(buttonIds.get(i));
        }
    }

    @Override
    public void extractBackground(GuiGraphicsExtractor g, int mouseX, int mouseY, float a) {
        super.extractBackground(g, mouseX, mouseY, a);
        int x = leftPos, y = topPos;
        g.fill(x, y, x + WIDTH, y + HEIGHT, WOOD_EDGE);
        g.fill(x + 1, y + 1, x + WIDTH - 1, y + HEIGHT - 1, WOOD);
        g.fill(x + 4, y + 16, x + WIDTH - 4, y + 114, FELT);
        g.outline(x + 6, y + 18, WIDTH - 12, 94, FELT_LINE);

        boolean settled = menu.phase() == BlackjackMenu.SETTLED;
        List<Integer> dealer = cards(BlackjackMenu.DEALER_CARDS), mine = cards(BlackjackMenu.PLAYER_CARDS);
        g.text(font, Component.translatable("gui.hollowfrontier.blackjack.dealer"), x + 10, y + 22, DIM, false);
        hand(g, dealer, x + 10, y + 32);
        if (!dealer.isEmpty()) {
            g.text(font, total(dealer, settled), x + WIDTH - 10 - font.width(total(dealer, settled)), y + 22, TEXT, false);
        }
        g.text(font, Component.translatable("gui.hollowfrontier.blackjack.you"), x + 10, y + 64, DIM, false);
        hand(g, mine, x + 10, y + 74);
        if (!mine.isEmpty()) {
            g.text(font, total(mine, true), x + WIDTH - 10 - font.width(total(mine, true)), y + 64, TEXT, false);
        }
        Component status = status();
        g.centeredText(font, status, x + WIDTH / 2, y + 104, TEXT);

        g.centeredText(font, Component.translatable("gui.hollowfrontier.blackjack.bet", menu.bet()), x + WIDTH / 2, y + 124, TEXT);
    }

    @Override
    protected void extractLabels(GuiGraphicsExtractor g, int mouseX, int mouseY) {
        Component purse = Component.translatable("gui.hollowfrontier.blackjack.purse", menu.purse());
        g.text(font, purse, WIDTH - 8 - font.width(purse), 5, DIM, false);
        // A long dealer's name ("Miss Belle", "Clementine") ran the title into the purse: squeezed to fit the room left
        float room = WIDTH - 16 - font.width(purse) - 6, scale = Math.min(1.0F, room / font.width(title));
        g.pose().pushMatrix();
        g.pose().translate(8, 5 + (1 - scale) * font.lineHeight / 2);
        g.pose().scale(scale, scale);
        g.text(font, title, 0, 0, TEXT, false);
        g.pose().popMatrix();
    }

    private Component status() {
        return switch (menu.phase()) {
            case BlackjackMenu.PLAYING -> Component.translatable("gui.hollowfrontier.blackjack.your_move");
            case BlackjackMenu.SETTLED -> Component.translatable("gui.hollowfrontier.blackjack.outcome." + menu.outcome().name().toLowerCase(java.util.Locale.ROOT),
                    menu.payout());
            default -> Component.translatable(menu.hitSoft17() ? "gui.hollowfrontier.blackjack.rules_sharp" : "gui.hollowfrontier.blackjack.rules",
                    menu.maxBet());
        };
    }

    // The cards in the data slots from {@code from}, as sent (1 + id, or HIDDEN)
    private List<Integer> cards(int from) {
        List<Integer> out = new ArrayList<>();
        for (int i = 0; i < BlackjackMenu.MAX_CARDS && menu.card(from + i) != 0; i++) {
            out.add(menu.card(from + i));
        }
        return out;
    }

    // A hand's total as far as can be seen ("soft 17" when an ace counts 11); a hidden hole card isn't counted
    private String total(List<Integer> sent, boolean all) {
        List<Integer> seen = new ArrayList<>();
        for (int card : sent) {
            if (card != BlackjackMenu.HIDDEN) {
                seen.add(card - 1);
            }
        }
        int total = Blackjack.total(seen);
        String shown = Blackjack.soft(seen) && total < 21 ? Component.translatable("gui.hollowfrontier.blackjack.soft", total).getString() : String.valueOf(total);
        return all || seen.size() == sent.size() ? shown : Component.translatable("gui.hollowfrontier.blackjack.shows", shown).getString();
    }

    private void hand(GuiGraphicsExtractor g, List<Integer> sent, int x, int y) {
        int room = WIDTH - 20 - CARD_W;
        int step = sent.size() > 1 ? Math.min(CARD_W + 3, room / (sent.size() - 1)) : 0;
        for (int i = 0; i < sent.size(); i++) {
            card(g, sent.get(i), x + i * step, y);
        }
    }

    private void card(GuiGraphicsExtractor g, int sent, int x, int y) {
        g.fill(x, y, x + CARD_W, y + CARD_H, CARD_EDGE);
        if (sent == BlackjackMenu.HIDDEN) {
            g.fill(x + 1, y + 1, x + CARD_W - 1, y + CARD_H - 1, BACK);
            for (int i = 3; i < CARD_W - 3; i += 3) {
                g.verticalLine(x + i, y + 3, y + CARD_H - 4, BACK_LINE);
            }
            g.outline(x + 2, y + 2, CARD_W - 4, CARD_H - 4, BACK_LINE);
            return;
        }
        int card = sent - 1;
        int colour = Blackjack.suit(card) == 1 || Blackjack.suit(card) == 2 ? RED : BLACK;
        g.fill(x + 1, y + 1, x + CARD_W - 1, y + CARD_H - 1, CARD_FACE);
        g.text(font, RANKS[Blackjack.rank(card)], x + 3, y + 3, colour, false);
        g.text(font, SUITS[Blackjack.suit(card)], x + CARD_W - 9, y + CARD_H - 11, colour, false);
    }
}
