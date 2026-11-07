package com.hollowfrontier.gametest;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import com.hollowfrontier.HollowFrontier;
import com.hollowfrontier.content.frontier.gazette.GazetteBackPage;
import com.hollowfrontier.content.frontier.gazette.GazetteBackPage.Kind;
import com.hollowfrontier.content.frontier.gazette.GazetteBackPage.Piece;
import com.hollowfrontier.content.frontier.gazette.GazetteItem;
import com.hollowfrontier.content.frontier.outlaw.AbstractOutlaw;
import com.hollowfrontier.content.frontier.outlaw.Outlaw;
import com.hollowfrontier.content.frontier.talk.Act;
import com.hollowfrontier.content.frontier.talk.Chatter;
import com.hollowfrontier.content.frontier.talk.Context;
import com.hollowfrontier.content.frontier.talk.Fact;
import com.hollowfrontier.content.ufo.FieldNotes;
import com.hollowfrontier.registry.ModEntities;
import com.hollowfrontier.worldgen.structure.ModChestLoot;
import com.hollowfrontier.worldgen.structure.TownLandmarks;

import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.contents.TranslatableContents;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.WrittenBookContent;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructurePlaceSettings;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplate;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.level.storage.loot.LootTable;
import net.minecraft.world.level.storage.loot.parameters.LootContextParamSets;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;
import net.minecraft.world.phys.Vec3;

/**
 * Phase 21a's words: the Gazette's back page follows the story and fits its page, the Gray field notes turn up in their
 * caches in more than one copy, Boot Hill carries its epitaphs, and outlaws jeer at a player who stops to reload. (The
 * new Chatter lines, the men in black's cover stories among them, are checked with the rest by ChatterTests.)
 */
final class HumourTests {
    private HumourTests() {}

    /** Every piece fits its own condition and its page, keys are their own, and every column has pieces for anyone. */
    static void backPageTable(GameTestHelper h) {
        Set<String> keys = new HashSet<>();
        for (Piece piece : GazetteBackPage.pieces()) {
            h.assertTrue(keys.add(piece.key()), "two pieces share " + piece.key());
            int max = piece.kind() == Kind.CLASSIFIED ? GazetteBackPage.MAX_CLASSIFIED : GazetteBackPage.MAX_PIECE;
            h.assertTrue(piece.text().length() <= max, piece.key() + " runs off its page: " + piece.text().length());
            h.assertTrue(!piece.text().contains("%"), piece.key() + " has a format argument nobody fills");
            h.assertTrue(GazetteBackPage.candidates(piece.kind(), forced(piece)).contains(piece), piece.key() + " never fits its own condition");
            if (piece.cond().from() != Act.FRONTIER) {
                h.assertFalse(GazetteBackPage.candidates(piece.kind(), Context.QUIET).contains(piece), piece.key() + " printed too early");
            }
            if (piece.cond().to() != Act.AFTER) {
                h.assertFalse(GazetteBackPage.candidates(piece.kind(), Context.of(Act.AFTER)).contains(piece), piece.key() + " printed too late");
            }
        }
        for (Kind kind : Kind.values()) {
            h.assertTrue(GazetteBackPage.candidates(kind, Context.QUIET).size() >= 4, "too few " + kind + " for a new player");
            h.assertTrue(GazetteBackPage.pieces().stream().filter(p -> p.kind() == kind).count() >= 10, "too few " + kind);
        }
        h.succeed();
    }

    /** A back page holds its column's title and pieces that fit; the later acts' jokes stay out of an early paper. */
    static void backPageFollowsTheStory(GameTestHelper h) {
        RandomSource random = RandomSource.create(21);
        Context early = Context.QUIET;
        for (int i = 0; i < 60; i++) {
            Kind kind = Kind.values()[i % Kind.values().length];
            List<String> printed = keys(GazetteBackPage.page(kind, early, random));
            h.assertTrue(printed.getFirst().equals(kind.titleKey()), "the column's title first: " + printed);
            h.assertValueEqual(printed.size(), 1 + kind.perPage, "pieces on a " + kind + " page");
            for (String key : printed.subList(1, printed.size())) {
                Piece piece = GazetteBackPage.pieces().stream().filter(p -> p.key().equals(key)).findFirst().orElseThrow();
                h.assertTrue(piece.kind() == kind && piece.cond().test(early), key + " doesn't belong in an early " + kind + " column");
            }
        }
        // With the sightings fresh, the letters lean on them
        Context lights = Context.of(Act.FRONTIER, Fact.SIGHTINGS);
        int widow = 0;
        for (int i = 0; i < 200; i++) {
            if (keys(GazetteBackPage.page(Kind.LETTER, lights, random)).stream().anyMatch(k -> GazetteBackPage.pieces().stream()
                    .anyMatch(p -> p.key().equals(k) && p.cond().all().contains(Fact.SIGHTINGS)))) {
                widow++;
            }
        }
        h.assertTrue(widow > 60, "the letter about the airship's lights came up only " + widow + " times in 200");

        ServerPlayer reader = TestPlayers.survival(h, new BlockPos(5, 1, 5));
        WrittenBookContent paper = GazetteItem.print(h.getLevel(), reader);
        Component back = paper.pages().getLast().raw();
        h.assertTrue(List.of(Kind.values()).stream().anyMatch(k -> keys(back).contains(k.titleKey())), "the back page isn't last: " + back);
        TestPlayers.leave(reader);
        h.succeed();
    }

    /** The field notes turn up in the Gray caches, a title page and four entries, and not always the same copy. */
    static void fieldNotesInTheCaches(GameTestHelper h) {
        for (int v = 0; v < FieldNotes.VARIANTS; v++) {
            List<Integer> entries = FieldNotes.entries(v);
            h.assertValueEqual(new HashSet<>(entries).size(), FieldNotes.PER_BOOK, "copy " + v + " repeats an entry");
            h.assertTrue(entries.stream().allMatch(i -> i >= 0 && i < FieldNotes.ENTRIES.size()), "copy " + v + " has no such entry: " + entries);
        }
        Set<Integer> used = new HashSet<>();
        for (int v = 0; v < FieldNotes.VARIANTS; v++) {
            used.addAll(FieldNotes.entries(v));
        }
        h.assertValueEqual(used.size(), FieldNotes.ENTRIES.size(), "entries no copy holds");

        for (ResourceKey<LootTable> key : List.of(ModChestLoot.GRAY_LAB, ModChestLoot.GRAY_STATION, ModChestLoot.GRAY_LANDING)) {
            LootTable table = h.getLevel().getServer().reloadableRegistries().getLootTable(key);
            Set<List<String>> copies = new HashSet<>();
            for (int i = 0; i < 200; i++) {
                for (ItemStack stack : table.getRandomItems(new LootParams.Builder(h.getLevel())
                        .withParameter(LootContextParams.ORIGIN, h.absoluteVec(Vec3.ZERO)).create(LootContextParamSets.CHEST))) {
                    if (!stack.is(Items.WRITTEN_BOOK)) {
                        continue;
                    }
                    WrittenBookContent book = stack.get(DataComponents.WRITTEN_BOOK_CONTENT);
                    h.assertTrue(book != null && book.title().raw().equals(FieldNotes.TITLE) && book.author().equals(FieldNotes.AUTHOR), "not the field notes");
                    h.assertValueEqual(book.pages().size(), 1 + FieldNotes.PER_BOOK, "pages in the field notes");
                    List<String> pages = new ArrayList<>();
                    book.pages().forEach(p -> pages.addAll(keys(p.raw())));
                    h.assertTrue(pages.getFirst().equals(FieldNotes.titleKey()), "the title page first: " + pages);
                    copies.add(pages);
                }
            }
            h.assertTrue(copies.size() >= 2, key.identifier() + " gave " + copies.size() + " copies of the notes in 200 chests");
        }
        h.succeed();
    }

    /** Each Boot Hill has three joke epitaphs on standing signs, no two hills the same, and every epitaph is used. */
    static void epitaphsOnBootHill(GameTestHelper h) {
        Set<String> seen = new HashSet<>();
        for (String name : List.of("town/boot_hill", "town/boot_hill_b", "town/boot_hill_c", "town/boot_hill_strange")) {
            StructureTemplate template = h.getLevel().getStructureTemplateManager().get(HollowFrontier.id(name)).orElse(null);
            h.assertTrue(template != null, name + " is missing");
            var signs = template.filterBlocks(BlockPos.ZERO, new StructurePlaceSettings(), Blocks.SPRUCE_SIGN);
            h.assertValueEqual(signs.size(), 3, "epitaph signs on " + name);
            for (var sign : signs) {
                String nbt = sign.nbt() == null ? "" : sign.nbt().toString();
                int at = nbt.indexOf("sign.hollowfrontier.epitaph.");
                h.assertTrue(at >= 0, "a sign on " + name + " without an epitaph");
                String key = nbt.substring(at, nbt.indexOf('.', at + "sign.hollowfrontier.epitaph.".length()));
                h.assertTrue(seen.add(key), key + " is on two hills");
            }
        }
        h.assertValueEqual(seen.size(), TownLandmarks.EPITAPHS.size(), "epitaphs on no hill");
        h.succeed();
    }

    /** An outlaw fighting a player jeers when they reload; one who isn't after them keeps quiet. */
    static void outlawsTauntReloading(GameTestHelper h) {
        ServerPlayer player = TestPlayers.survival(h, new BlockPos(5, 1, 5));
        Outlaw idle = h.spawnWithNoFreeWill(ModEntities.OUTLAW.get(), new BlockPos(7, 1, 5));
        h.assertTrue(AbstractOutlaw.taunt(player) == null, "an outlaw jeered at a player he isn't fighting");
        Outlaw outlaw = h.spawnWithNoFreeWill(ModEntities.OUTLAW.get(), new BlockPos(9, 1, 9));
        outlaw.setTarget(player);
        h.runAfterDelay(Chatter.LISTENER_BARK_COOLDOWN + 1, () -> {
            String key = AbstractOutlaw.taunt(player);
            h.assertTrue(key != null && key.startsWith("chatter.hollowfrontier.outlaw.taunt."), "no jeer at the reload: " + key);
            h.assertTrue(idle.isAlive(), "the idle outlaw is gone");
            TestPlayers.leave(player);
            h.succeed();
        });
    }

    private static Context forced(Piece piece) {
        Set<Fact> facts = piece.cond().all().isEmpty() ? EnumSet.noneOf(Fact.class) : EnumSet.copyOf(piece.cond().all());
        return new Context(facts, piece.cond().from(), "", piece.cond().from().atLeast(Act.GATEWAY));
    }

    // The lang keys in a page, in order.
    private static List<String> keys(Component component) {
        List<String> out = new ArrayList<>();
        if (component.getContents() instanceof TranslatableContents t) {
            out.add(t.getKey());
        }
        component.getSiblings().forEach(c -> out.addAll(keys(c)));
        return out;
    }
}
