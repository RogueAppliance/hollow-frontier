package com.hollowfrontier.client;

import java.util.ArrayList;
import java.util.List;

import com.hollowfrontier.HollowFrontier;
import com.hollowfrontier.content.journal.Journal;
import com.hollowfrontier.content.journal.JournalData;
import com.hollowfrontier.content.journal.JournalSeen;
import com.hollowfrontier.content.journal.JournalText;
import com.hollowfrontier.content.journal.JournalTowns;
import com.hollowfrontier.datagen.ModAdvancements;
import com.hollowfrontier.network.JournalPagesPayload;
import com.hollowfrontier.network.JournalRequestPayload;
import com.mojang.blaze3d.platform.InputConstants;

import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.Style;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.client.network.ClientPacketDistributor;

/**
 * The Prospector's Journal open (48a, 48b): a leather book lying flat. Along the top of the left page are the book's
 * parts, each there once it has anything in it: the road, the creatures met, the frontier's strange doings, and people.
 * Under them, the part's contents (for the road, the pinned thread's next step first); the right page is the entry
 * picked. Flat fills and the font, like the waystone and the card table, so it needs no GUI texture and a shader pack
 * has nothing to get hold of; a creature's sketch is its spawn egg, drawn large. Every control is a button (Tab and
 * Enter work), Page Up and Page Down turn the right page, and the narrator reads the page.
 */
public class JournalScreen extends Screen {
    private static final int WIDTH = 296, HEIGHT = 184, PAGE = 124, LINE = 9, LINES = 14, ROW = 9, SKETCH = 3;
    private static final int LEATHER = 0xFF5B3A21, LEATHER_EDGE = 0xFF2E1C0F, PAPER = 0xFFEADFC2, PAPER_EDGE = 0xFFCDBE98, GUTTER = 0xFFB9A982,
            INK = 0xFF2B2118, FAINT = 0xFF756650, TICK = 0xFF3C6A2C, RULE = 0xFFBFAF88;
    private static final String ROAD = "road", SEEN = "seen", DOINGS = "doings", PEOPLE = "people", TOWNS = "towns";
    private static final String[][] TABS = {{ROAD, JournalText.ROAD_TAB}, {SEEN, JournalText.SEEN_TAB}, {DOINGS, JournalText.DOINGS_TAB},
            {PEOPLE, JournalText.PEOPLE_TAB}, {TOWNS, JournalText.TOWNS_TAB}};

    private record Line(FormattedCharSequence text, int colour, boolean head) {}

    private JournalPagesPayload pages;
    private String part = ROAD;
    // The entry picked in each part (a thread, a creature, a person)
    private String thread = "", creature = "", person = "", town = "";
    private int leaf;
    private List<Line> lines = List.of();
    private ItemStack sketch = ItemStack.EMPTY;
    private final List<Component> spoken = new ArrayList<>();

    public JournalScreen(JournalPagesPayload pages) {
        super(Component.translatable(JournalText.TITLE));
        this.pages = pages;
        this.thread = pages.pinned().isEmpty() && !pages.threads().isEmpty() ? pages.threads().getLast().name() : pages.pinned();
    }

    /** The server's answer to a pin or a fresh look, while the book is open. */
    public void update(JournalPagesPayload pages) {
        this.pages = pages;
        rebuildWidgets();
    }

    /** Turns to a part ("road", "seen", "doings", "people") and an entry in it (the contents' buttons, and the photo booth). */
    void show(String part, String entry) {
        this.part = part;
        switch (part) {
            case ROAD -> thread = entry;
            case SEEN -> creature = entry;
            case PEOPLE -> person = entry;
            case TOWNS -> town = entry;
            default -> {}
        }
        leaf = 0;
        rebuildWidgets();
    }

    private int left() {
        return (width - WIDTH) / 2;
    }

    private int top() {
        return Math.max(4, (height - HEIGHT) / 2 - 8);
    }

    private Journal.Thread thread(String name) {
        return pages.threads().stream().filter(t -> t.name().equals(name)).findFirst().orElse(null);
    }

    private static Component name(String thread) {
        return Component.translatable(JournalText.THREAD.getOrDefault(thread, thread));
    }

    private static Component creatureName(String id) {
        return Component.translatable("entity." + HollowFrontier.MODID + "." + id);
    }

    private static Component nextLine(String step) {
        return Component.translatable(JournalText.NEXT.getOrDefault(step, ModAdvancements.key(step) + ".description"));
    }

    private Component tab(String[] tab) {
        return Component.translatable(tab[1]).withStyle(tab[0].equals(part) ? Style.EMPTY.withBold(true) : Style.EMPTY);
    }

    // How far the left page's contents are pushed down by the parts running to a second row
    private int drop() {
        int x = 0, rows = 1;
        for (String[] tab : TABS) {
            if (has(tab[0])) {
                int wide = font.width(tab(tab));
                if (x > 0 && x + wide > PAGE) {
                    rows++;
                    x = 0;
                }
                x += wide + 4;
            }
        }
        return (rows - 1) * 10;
    }

    // Where the part's list starts on the left page: for the road, under the pinned thread's lines if one is pinned
    private int listTop() {
        Journal.Thread pinned = thread(pages.pinned());
        if (!part.equals(ROAD) || pinned == null) {
            return top() + 42 + drop();
        }
        return top() + 54 + drop() + Math.min(4, pinnedLines(pinned).size()) * LINE + 4;
    }

    private List<FormattedCharSequence> pinnedLines(Journal.Thread pinned) {
        return font.split(pinned.next().isEmpty() ? Component.translatable(JournalText.DONE_ALL) : nextLine(pinned.next()), PAGE);
    }

    private boolean has(String part) {
        return switch (part) {
            case SEEN -> !pages.creatures().isEmpty();
            case DOINGS -> pages.strangeCount() > 0;
            case PEOPLE -> !pages.people().isEmpty();
            case TOWNS -> !pages.towns().isEmpty();
            default -> true;
        };
    }

    @Override
    protected void init() {
        if (!has(part)) {
            part = ROAD;
        }
        if (thread(thread) == null) {
            thread = pages.threads().isEmpty() ? "" : pages.threads().getFirst().name();
        }
        if (pages.creatures().stream().noneMatch(c -> c.id().equals(creature))) {
            creature = pages.creatures().isEmpty() ? "" : pages.creatures().getFirst().id();
        }
        if (pages.people().stream().noneMatch(p -> p.id().equals(person))) {
            person = pages.people().isEmpty() ? "" : pages.people().getFirst().id();
        }
        if (pages.towns().stream().noneMatch(t -> t.name().equals(town))) {
            town = pages.towns().isEmpty() ? "" : pages.towns().getLast().name();
        }
        int x = left() + 14, y = top() + 26;
        // The book's parts, along the top (on two rows once there are too many for one)
        int tabX = x;
        for (String[] tab : TABS) {
            if (has(tab[0])) {
                Component label = tab(tab);
                if (tabX > x && tabX - x + font.width(label) > PAGE) {
                    tabX = x;
                    y += 10;
                }
                String to = tab[0];
                addRenderableWidget(new InkButton(tabX, y, font.width(label), 10, label, b -> part(to), font));
                tabX += font.width(label) + 4;
            }
        }
        y = listTop();
        switch (part) {
            case ROAD -> {
                for (Journal.Thread entry : pages.threads()) {
                    y = entry(x, y, name(entry.name()), entry.name().equals(thread), () -> show(ROAD, entry.name()));
                }
            }
            case SEEN -> {
                for (JournalSeen.Creature entry : pages.creatures()) {
                    y = entry(x, y, creatureName(entry.id()), entry.id().equals(creature), () -> show(SEEN, entry.id()));
                }
            }
            case PEOPLE -> {
                for (JournalSeen.Person entry : pages.people()) {
                    y = entry(x, y, Component.translatable(JournalText.PERSON.getOrDefault(entry.id(), entry.id())), entry.id().equals(person), () -> show(PEOPLE, entry.id()));
                }
            }
            case TOWNS -> {
                for (JournalTowns.Town entry : pages.towns()) {
                    y = entry(x, y, Component.literal(entry.name()), entry.name().equals(town), () -> show(TOWNS, entry.name()));
                }
            }
            default -> {}
        }
        write();
        int right = left() + WIDTH / 2 + 10, foot = top() + HEIGHT - 24;
        Journal.Thread shown = thread(thread);
        if (part.equals(ROAD) && shown != null) {
            boolean pinned = shown.name().equals(pages.pinned());
            addRenderableWidget(new InkButton(right, foot - 12, PAGE, 11, Component.translatable(pinned ? JournalText.UNPIN : JournalText.PIN)
                    .withStyle(ChatFormatting.ITALIC), b -> ClientPacketDistributor.sendToServer(new JournalRequestPayload(pinned ? "" : shown.name())), font));
        }
        if (leaves() > 1) {
            InkButton back = addRenderableWidget(new InkButton(right, foot, 12, 11, Component.literal("<"), b -> turn(-1), font));
            InkButton on = addRenderableWidget(new InkButton(right + PAGE - 8, foot, 12, 11, Component.literal(">"), b -> turn(1), font));
            back.setTooltip(Tooltip.create(Component.translatable(JournalText.TURN_BACK)));
            on.setTooltip(Tooltip.create(Component.translatable(JournalText.TURN_ON)));
            back.active = leaf > 0;
            on.active = leaf < leaves() - 1;
        }
        addRenderableWidget(Button.builder(CommonComponents.GUI_DONE, b -> onClose()).bounds(width / 2 - 50, top() + HEIGHT + 4, 100, 20).build());
    }

    private int entry(int x, int y, Component name, boolean here, Runnable pick) {
        addRenderableWidget(new InkButton(x, y, PAGE, ROW, Component.empty().append(here ? "» " : "  ").append(name), b -> pick.run(), font));
        return y + ROW;
    }

    private void part(String to) {
        part = to;
        leaf = 0;
        rebuildWidgets();
    }

    private void turn(int by) {
        leaf = Math.clamp(leaf + by, 0, leaves() - 1);
        rebuildWidgets();
    }

    private int leaves() {
        return Math.max(1, (lines.size() + LINES - 1) / LINES);
    }

    // The right page, as lines of ink
    private void write() {
        List<Line> out = new ArrayList<>();
        spoken.clear();
        sketch = ItemStack.EMPTY;
        switch (part) {
            case SEEN -> writeCreature(out);
            case DOINGS -> writeDoings(out);
            case PEOPLE -> writePerson(out);
            case TOWNS -> writeTown(out);
            default -> writeThread(out);
        }
        List<Line> set = new ArrayList<>();
        for (Line line : out) {
            if (line != null && line.head() && set.size() % LINES == LINES - 1) {
                set.add(null);   // a heading isn't left alone at the foot of a leaf
            }
            if (line != null || set.size() % LINES != 0) {   // and a leaf doesn't open on a blank line
                set.add(line);
            }
        }
        lines = set;
        leaf = Math.clamp(leaf, 0, leaves() - 1);
    }

    private void writeThread(List<Line> out) {
        Journal.Thread entry = thread(thread);
        if (entry == null) {
            return;
        }
        put(out, name(entry.name()).copy().withStyle(ChatFormatting.BOLD), INK);
        out.add(null);
        for (String step : entry.done()) {
            put(out, Component.literal("✔ ").append(Component.translatable(ModAdvancements.key(step) + ".title")), TICK);
        }
        if (!entry.done().isEmpty()) {
            out.add(null);
        }
        if (entry.next().isEmpty()) {
            put(out, Component.translatable(JournalText.DONE_ALL).withStyle(ChatFormatting.ITALIC), FAINT);
        } else {
            head(out, JournalText.NEXT_HEAD);
            put(out, nextLine(entry.next()), INK);
        }
        if (!entry.surveyDone().isEmpty() || !entry.surveyNext().isEmpty()) {
            out.add(null);
            head(out, JournalText.SURVEY);
            for (String step : entry.surveyDone()) {
                put(out, Component.literal("✔ ").append(Component.translatable("book.hollowfrontier.survey_letter.step." + step)), TICK);
            }
            if (!entry.surveyNext().isEmpty()) {
                String key = "book.hollowfrontier.survey_letter.step." + entry.surveyNext();
                put(out, Component.literal("○ ").append(Component.translatable(key)), INK);
                put(out, Component.translatable(key + ".how").withStyle(ChatFormatting.ITALIC), FAINT);
            }
        }
        if (entry.craterBlocks() > 0) {
            out.add(null);
            put(out, Component.translatable(JournalText.HEADING, entry.craterBlocks(),
                    Component.translatable("message.hollowfrontier.direction." + entry.craterWay())), INK);
        }
    }

    private void writeCreature(List<Line> out) {
        JournalSeen.Creature entry = pages.creatures().stream().filter(c -> c.id().equals(creature)).findFirst().orElse(null);
        if (entry == null) {
            return;
        }
        // Its sketch stands at the head of the page: the spawn egg's art, large, with the name beside it
        sketch = BuiltInRegistries.ITEM.getOptional(HollowFrontier.id(entry.id() + "_spawn_egg")).map(ItemStack::new).orElse(ItemStack.EMPTY);
        int rows = (16 * SKETCH + LINE - 1) / LINE;
        spoken.add(creatureName(entry.id()));
        for (int row = 0; row < rows; row++) {
            out.add(new Line(FormattedCharSequence.EMPTY, INK, false));
        }
        String key = JournalText.key("creature." + entry.id());
        put(out, Component.translatable(key + ".met"), INK);
        if ((entry.flags() & JournalData.KNOWN) != 0) {
            out.add(null);
            put(out, Component.translatable(key + ".known"), INK);
        }
        if ((entry.flags() & JournalData.TAGGED) != 0 && JournalSeen.TAGGABLE.contains(entry.id())) {
            out.add(null);
            head(out, JournalText.TAGGED);
            put(out, Component.translatable(key + ".tagged"), INK);
        }
    }

    private void writeDoings(List<Line> out) {
        put(out, Component.translatable(JournalText.DOINGS).withStyle(ChatFormatting.BOLD), INK);
        out.add(null);
        // the latest first
        for (JournalData.Strange sign : pages.strange().reversed()) {
            Component what = Component.translatable(JournalText.STRANGE.getOrDefault(sign.kind(), sign.kind()));
            put(out, sign.town().isEmpty() ? Component.translatable(JournalText.STRANGE_AT, sign.day(), sign.x(), sign.z(), what)
                    : Component.translatable(JournalText.STRANGE_NEAR, sign.day(), sign.town(), what), INK);
            out.add(null);
        }
    }

    private void writePerson(List<Line> out) {
        JournalSeen.Person entry = pages.people().stream().filter(p -> p.id().equals(person)).findFirst().orElse(null);
        if (entry == null) {
            return;
        }
        put(out, Component.translatable(JournalText.PERSON.getOrDefault(entry.id(), entry.id())).withStyle(ChatFormatting.BOLD), INK);
        for (String fact : entry.facts()) {
            out.add(null);
            put(out, Component.translatable(JournalText.key("people." + entry.id() + "." + fact)), INK);
        }
    }

    private void writeTown(List<Line> out) {
        JournalTowns.Town entry = pages.towns().stream().filter(t -> t.name().equals(town)).findFirst().orElse(null);
        if (entry == null) {
            return;
        }
        put(out, Component.literal(entry.name()).withStyle(ChatFormatting.BOLD), INK);
        out.add(null);
        put(out, Component.translatable(JournalText.TOWN_FIRST, entry.firstDay()), FAINT);
        put(out, Component.translatable(JournalText.TOWN_STANDING, Component.translatable("standing.hollowfrontier." + entry.rank()), entry.points()), INK);
        if (entry.star() > 0) {
            put(out, Component.translatable(entry.star() == 1 ? JournalText.TOWN_STAR : JournalText.TOWN_STAR_HELD), INK);
        }
        if (!entry.job().isEmpty()) {
            put(out, Component.translatable(JournalText.key("town.job." + entry.job())), INK);
        }
        if (entry.head() >= 0) {
            out.add(null);
            put(out, Component.translatable(JournalText.TOWN_CLAIM, entry.head()), INK);
        }
        if (entry.driveHead() >= 0) {
            out.add(null);
            put(out, entry.driveWay().isEmpty() ? Component.translatable(JournalText.TOWN_DRIVE_AWAY) : Component.translatable(JournalText.TOWN_DRIVE,
                    entry.driveHead(), entry.driveBlocks(), Component.translatable("message.hollowfrontier.direction." + entry.driveWay())), INK);
        }
        boolean duelled = entry.duelsWon() + entry.duelsLost() > 0;
        if (entry.bank() >= 0 || !entry.poster().isEmpty() || entry.raidDay() > 0 || duelled) {
            out.add(null);
        }
        if (entry.bank() >= 0) {
            int gold = entry.bank() / com.hollowfrontier.content.frontier.bounty.Purse.GOLD, copper = entry.bank() % com.hollowfrontier.content.frontier.bounty.Purse.GOLD;
            put(out, entry.bank() == 0 ? Component.translatable(JournalText.TOWN_BANK_EMPTY)
                    : copper == 0 ? Component.translatable(JournalText.TOWN_BANK_GOLD, gold)
                    : gold == 0 ? Component.translatable(JournalText.TOWN_BANK_COPPER, copper)
                    : Component.translatable(JournalText.TOWN_BANK, gold, copper), INK);
        }
        if (!entry.poster().isEmpty()) {
            put(out, Component.translatable(JournalText.TOWN_POSTER, entry.poster(), entry.reward()), INK);
        }
        if (entry.raidDay() > 0) {
            put(out, Component.translatable(JournalText.TOWN_RAID, entry.raidDay()), INK);
        }
        if (duelled) {
            put(out, Component.translatable(JournalText.TOWN_DUELS, entry.duelsWon(), entry.duelsLost()), INK);
        }
    }

    private void head(List<Line> out, String key) {
        Component text = Component.translatable(key).withStyle(Style.EMPTY.withUnderlined(true));
        spoken.add(text);
        for (FormattedCharSequence row : font.split(text, PAGE)) {
            out.add(new Line(row, FAINT, true));
        }
    }

    private void put(List<Line> out, Component text, int colour) {
        spoken.add(text);
        List<FormattedCharSequence> rows = font.split(text, PAGE);
        // A paragraph that would run over the foot of a leaf starts the next one instead
        if (rows.size() <= LINES && out.size() % LINES + rows.size() > LINES) {
            while (out.size() % LINES != 0) {
                out.add(new Line(FormattedCharSequence.EMPTY, colour, false));
            }
        }
        for (FormattedCharSequence row : rows) {
            out.add(new Line(row, colour, false));
        }
    }

    @Override
    public Component getNarrationMessage() {
        List<Component> all = new ArrayList<>();
        all.add(super.getNarrationMessage());
        all.addAll(spoken);
        return CommonComponents.joinLines(all);
    }

    @Override
    public boolean keyPressed(KeyEvent event) {
        if (super.keyPressed(event)) {
            return true;
        }
        if (event.key() == InputConstants.KEY_PAGEUP || event.key() == InputConstants.KEY_PAGEDOWN) {
            turn(event.key() == InputConstants.KEY_PAGEUP ? -1 : 1);
            return true;
        }
        if (ModKeyMappings.JOURNAL.matches(event)) {
            onClose();
            return true;
        }
        return false;
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    @Override
    public void extractBackground(GuiGraphicsExtractor g, int mouseX, int mouseY, float a) {
        super.extractBackground(g, mouseX, mouseY, a);
        int x = left(), y = top(), mid = x + WIDTH / 2;
        // The cover, the two pages, and the gutter between them
        g.fill(x, y, x + WIDTH, y + HEIGHT, LEATHER_EDGE);
        g.fill(x + 1, y + 1, x + WIDTH - 1, y + HEIGHT - 1, LEATHER);
        g.fill(x + 5, y + 5, x + WIDTH - 5, y + HEIGHT - 5, PAPER_EDGE);
        g.fill(x + 6, y + 5, mid - 1, y + HEIGHT - 6, PAPER);
        g.fill(mid + 1, y + 5, x + WIDTH - 6, y + HEIGHT - 6, PAPER);
        g.fill(mid - 3, y + 5, mid - 1, y + HEIGHT - 6, PAPER_EDGE);
        g.fill(mid + 1, y + 5, mid + 3, y + HEIGHT - 6, PAPER_EDGE);
        g.fill(mid - 1, y + 5, mid + 1, y + HEIGHT - 5, GUTTER);
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor g, int mouseX, int mouseY, float a) {
        super.extractRenderState(g, mouseX, mouseY, a);
        int x = left() + 14, y = top() + 12;
        g.text(font, title.copy().withStyle(ChatFormatting.BOLD), x, y, INK, false);
        g.horizontalLine(x, x + PAGE - 1, y + 11, RULE);
        int drop = drop();
        g.horizontalLine(x, x + PAGE - 1, y + 26 + drop, RULE);
        if (part.equals(ROAD)) {
            Journal.Thread pinned = thread(pages.pinned());
            if (pinned != null) {
                g.text(font, Component.translatable(JournalText.PINNED).append(": ").append(name(pinned.name())), x, y + 30 + drop, FAINT, false);
                List<FormattedCharSequence> rows = pinnedLines(pinned);
                for (int i = 0; i < Math.min(4, rows.size()); i++) {
                    g.text(font, rows.get(i), x, y + 42 + drop + i * LINE, INK, false);
                }
            }
        } else if (part.equals(DOINGS)) {
            // The tally, and what the editor makes of it
            List<FormattedCharSequence> rows = new ArrayList<>(font.split(Component.translatable(JournalText.TALLY, pages.strangeCount()), PAGE));
            rows.add(FormattedCharSequence.EMPTY);
            rows.addAll(font.split(Component.translatable(pages.tallyPaid() ? JournalText.TALLY_PAID
                    : pages.strangeCount() >= JournalSeen.TALLY ? JournalText.TALLY_WANTED : JournalText.TALLY_SHORT).withStyle(ChatFormatting.ITALIC), PAGE));
            for (int i = 0; i < rows.size(); i++) {
                g.text(font, rows.get(i), x, y + 32 + drop + i * LINE, i == 0 ? INK : FAINT, false);
            }
        }
        int right = left() + WIDTH / 2 + 10;
        if (!sketch.isEmpty() && leaf == 0) {
            g.pose().pushMatrix();
            g.pose().translate(right, y - 2);
            g.pose().scale(SKETCH, SKETCH);
            g.item(sketch, 0, 0);
            g.pose().popMatrix();
            List<FormattedCharSequence> name = font.split(creatureName(creature).copy().withStyle(ChatFormatting.BOLD), PAGE - 16 * SKETCH - 6);
            for (int i = 0; i < name.size(); i++) {
                g.text(font, name.get(i), right + 16 * SKETCH + 6, y + 14 + i * LINE, INK, false);
            }
        }
        for (int i = leaf * LINES, row = 0; i < Math.min(lines.size(), (leaf + 1) * LINES); i++, row++) {
            Line line = lines.get(i);
            if (line != null) {
                g.text(font, line.text(), right, y + row * LINE, line.colour(), false);
            }
        }
        if (leaves() > 1) {
            g.centeredText(font, Component.translatable(JournalText.PAGE, leaf + 1, leaves()), right + PAGE / 2, top() + HEIGHT - 22, FAINT);
        }
    }

    /** A button that is only its words in ink: underlined under the pointer or the keyboard's focus, faint when it can't be pressed. */
    private static class InkButton extends Button {
        private final Font font;

        InkButton(int x, int y, int width, int height, Component message, OnPress onPress, Font font) {
            super(x, y, width, height, message, onPress, DEFAULT_NARRATION);
            this.font = font;
        }

        @Override
        public void extractContents(GuiGraphicsExtractor g, int mouseX, int mouseY, float a) {
            Component text = isHoveredOrFocused() && active ? getMessage().copy().withStyle(getMessage().getStyle().withUnderlined(true)) : getMessage();
            g.text(font, text, getX(), getY() + 1, active ? INK : PAPER_EDGE, false);
        }
    }
}
