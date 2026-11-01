package com.hollowfrontier.gametest;

import java.util.ArrayList;
import java.util.List;

import com.hollowfrontier.HollowFrontier;
import com.hollowfrontier.content.frontier.gazette.GazetteItem;
import com.hollowfrontier.content.frontier.mystery.AirshipSighting;
import com.hollowfrontier.content.frontier.mystery.CattleCarcass;
import com.hollowfrontier.content.frontier.mystery.MysteryLog;
import com.hollowfrontier.content.frontier.mystery.Mysteries;
import com.hollowfrontier.content.frontier.talk.Chatter;
import com.hollowfrontier.content.frontier.talk.ChatterLines;
import com.hollowfrontier.content.frontier.talk.Context;
import com.hollowfrontier.content.frontier.talk.Fact;
import com.hollowfrontier.content.frontier.talk.Moment;
import com.hollowfrontier.content.frontier.townsfolk.Folk;
import com.hollowfrontier.content.hollow.creature.GrayProbe;
import com.hollowfrontier.registry.ModBlocks;
import com.hollowfrontier.registry.ModEntities;
import com.hollowfrontier.registry.ModItems;
import com.hollowfrontier.worldgen.structure.ModChestLoot;
import com.hollowfrontier.worldgen.structure.TownLandmarks;

import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.contents.TranslatableContents;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.animal.cow.Cow;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.WrittenBookContent;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.CropBlock;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructurePlaceSettings;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.level.storage.loot.parameters.LootContextParamSets;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

/**
 * Phase 11a: the frontier's mysteries. A sighting crossing the sky (and one hovering, as for the Strange Skull); a crop
 * circle pressed into a wheat field; a mutilated steer, its carcass and scorched ring; a probe sent to do the job, done,
 * spooked off, and brought down in the act; the log the Gazette prints from and the townsfolk grumble from; and the Boot
 * Hill grave's payoff. On the 40-block field (grass at y 0).
 */
final class MysteryTests {
    private MysteryTests() {}

    /** A sighting heads off in a straight line, untouchable, and is logged; one sent for the skull hangs in place first. */
    static void sightingCrossesTheSky(GameTestHelper h) {
        Vec3 over = h.absoluteVec(new Vec3(20, 1, 20));
        AirshipSighting crossing = Mysteries.sighting(h.getLevel(), over, h.getLevel().getRandom(), 0);
        crossing.setPos(over.add(0, 10, 0)); // brought in where the test's chunks tick (it would start 96 blocks off)
        AirshipSighting hovering = Mysteries.sighting(h.getLevel(), over, h.getLevel().getRandom(), 60);
        Vec3 start = crossing.position(), still = hovering.position();
        h.assertFalse(crossing.hurtServer(h.getLevel(), h.getLevel().damageSources().generic(), 100.0F), "a sighting shouldn't take hurt");
        h.assertTrue(hovering.isHovering() && !crossing.isHovering(), "only the one sent for the skull should hover");
        h.assertTrue(MysteryLog.get(h.getLevel()).latest(MysteryLog.Kind.SIGHTING, BlockPos.containing(over), 1, h.getLevel().getGameTime(), 10).isPresent(),
                "the sighting wasn't logged");
        h.runAfterDelay(40, () -> {
            double moved = crossing.position().subtract(start).horizontalDistance();
            h.assertTrue(Math.abs(moved - 40 * AirshipSighting.SPEED) < 1.0, "it should have flown 20 blocks: " + moved);
            h.assertTrue(hovering.position().distanceTo(still) < 1.0, "the hovering one drifted: " + hovering.position().distanceTo(still));
            crossing.discard();
            hovering.discard();
            h.succeed();
        });
    }

    private static void plantWheat(GameTestHelper h, int from, int to) {
        for (int x = from; x <= to; x++) {
            for (int z = from; z <= to; z++) {
                h.setBlock(new BlockPos(x, 0, z), Blocks.FARMLAND);
                h.setBlock(new BlockPos(x, 1, z), Blocks.WHEAT.defaultBlockState().setValue(CropBlock.AGE, CropBlock.MAX_AGE));
            }
        }
    }

    /** A ripe field is found, and a circle pressed into it: the pattern laid flat, the rest left standing, the farmland kept. */
    static void cropCirclePressed(GameTestHelper h) {
        plantWheat(h, 10, 30);
        BlockPos middle = h.absolutePos(new BlockPos(20, 1, 20));
        h.assertTrue(Mysteries.findWheat(h.getLevel(), middle, 0, 12).isPresent(), "the field wasn't found");
        int laid = Mysteries.cropCircle(h.getLevel(), middle);
        h.assertTrue(laid >= 60, "only " + laid + " stalks laid");
        h.assertBlockPresent(ModBlocks.FLATTENED_WHEAT.get(), new BlockPos(20, 1, 20));
        h.assertBlockPresent(ModBlocks.FLATTENED_WHEAT.get(), new BlockPos(24, 1, 20));
        h.assertBlockPresent(Blocks.WHEAT, new BlockPos(22, 1, 22)); // between the rings
        h.runAfterDelay(20, () -> {
            h.assertBlockPresent(Blocks.FARMLAND, new BlockPos(20, 0, 20));
            h.assertTrue(MysteryLog.get(h.getLevel()).latest(MysteryLog.Kind.CROP_CIRCLE, middle, 1, h.getLevel().getGameTime(), 40).isPresent(),
                    "the circle wasn't logged");
            h.succeed();
        });
    }

    /**
     * A town's wheat field, as built, takes a whole crop circle: found from its gate, the circle goes in the middle of
     * the wheat, and every spot of the pattern is wheat to lay flat (none of it falls on a water hole or off the field).
     */
    static void townWheatFieldsTakeCircles(GameTestHelper h) {
        BlockPos at = new BlockPos(1, 0, 1);
        BlockPos origin = h.absolutePos(at);
        h.getLevel().getStructureTemplateManager().get(HollowFrontier.id("town/wheat_field")).orElseThrow().placeInWorld(h.getLevel(),
                origin, origin, new StructurePlaceSettings(), h.getLevel().getRandom(), Block.UPDATE_CLIENTS);
        BlockPos gate = h.absolutePos(at.offset(9, 1, 0));
        BlockPos middle = h.absolutePos(at.offset(TownLandmarks.FIELD_MIDDLE_X, 1, TownLandmarks.FIELD_MIDDLE_Z));
        var found = Mysteries.findWheat(h.getLevel(), gate, 0, Mysteries.SEARCH);
        h.assertTrue(found.isPresent(), "the field wasn't found");
        h.assertValueEqual(found.get(), middle, "where the circle goes");
        int pattern = 0;
        for (int dx = -Mysteries.CIRCLE_REACH; dx <= Mysteries.CIRCLE_REACH; dx++) {
            for (int dz = -Mysteries.CIRCLE_REACH; dz <= Mysteries.CIRCLE_REACH; dz++) {
                pattern += Mysteries.inPattern(dx, dz) ? 1 : 0;
            }
        }
        h.assertValueEqual(Mysteries.cropCircle(h.getLevel(), found.get()), pattern, "stalks laid (the whole pattern)");
        h.succeed();
    }

    /** A steer is cut: gone, a carcass in its place in a scorched ring; the carcass breaks up into leather. */
    static void steerMutilated(GameTestHelper h) {
        Cow cow = h.spawnWithNoFreeWill(EntityTypes.COW, new BlockPos(20, 1, 20));
        ServerPlayer player = TestPlayers.survival(h, new BlockPos(20, 1, 2));
        h.assertTrue(Mysteries.happen(h.getLevel(), player, MysteryLog.Kind.MUTILATION, h.getLevel().getRandom(), false), "nothing happened");
        h.assertFalse(cow.isAlive(), "the steer is still there");
        List<CattleCarcass> carcasses = h.getEntities(ModEntities.CATTLE_CARCASS.get());
        h.assertValueEqual(carcasses.size(), 1, "carcasses");
        int scorched = 0;
        for (int dx = -4; dx <= 4; dx++) {
            for (int dz = -4; dz <= 4; dz++) {
                scorched += h.getBlockState(new BlockPos(20 + dx, 0, 20 + dz)).is(Blocks.COARSE_DIRT) ? 1 : 0;
            }
        }
        h.assertTrue(scorched >= 12, "the ring isn't scorched: " + scorched);
        CattleCarcass carcass = carcasses.getFirst();
        AABB around = carcass.getBoundingBox().inflate(3);
        carcass.hurtServer(h.getLevel(), h.getLevel().damageSources().playerAttack(player), 1.0F);
        h.succeedWhen(() -> h.assertTrue(carcass.isRemoved() && !h.getLevel().getEntitiesOfClass(ItemEntity.class, around,
                item -> item.getItem().is(Items.LEATHER)).isEmpty(), "the carcass didn't break up into leather"));
    }

    /** A probe sent to a steer comes down over it, works a while and leaves a carcass. */
    static void probeDoesTheJob(GameTestHelper h) {
        Cow cow = h.spawnWithNoFreeWill(EntityTypes.COW, new BlockPos(20, 1, 20));
        GrayProbe probe = Mysteries.visit(h.getLevel(), MysteryLog.Kind.MUTILATION, cow.blockPosition(), cow);
        lower(h, probe);
        h.assertTrue(probe.hasJob(), "the probe has no job");
        h.succeedWhen(() -> h.assertTrue(!cow.isAlive() && h.getEntities(ModEntities.CATTLE_CARCASS.get()).size() == 1 && !probe.hasJob(),
                "the job isn't done (job ticks " + probe.jobTicks() + ", at " + h.relativeVec(probe.position()) + ")"));
    }

    /** Someone walking up on it (not sneaking) and it gives up and goes; the steer lives. */
    static void probeSpookedOff(GameTestHelper h) {
        Cow cow = h.spawnWithNoFreeWill(EntityTypes.COW, new BlockPos(20, 1, 20));
        GrayProbe probe = Mysteries.visit(h.getLevel(), MysteryLog.Kind.MUTILATION, cow.blockPosition(), cow);
        lower(h, probe);
        TestPlayers.survival(h, new BlockPos(24, 1, 20));
        h.succeedWhen(() -> h.assertTrue(probe.isFleeing() && !probe.hasJob() && cow.isAlive(), "the probe kept at it"));
    }

    // A probe comes down from 28 up, over the test plot's roof: start it under it
    private static void lower(GameTestHelper h, GrayProbe probe) {
        probe.setPos(h.absoluteVec(new Vec3(20.5, 12, 20.5)));
        // A probe despawns like any mob when the nearest player is far off, and other tests' fake players can be
        probe.setPersistenceRequired();
    }

    /** Brought down mid-job, a probe drops fragments. */
    static void probeCaughtInTheAct(GameTestHelper h) {
        Cow cow = h.spawnWithNoFreeWill(EntityTypes.COW, new BlockPos(20, 1, 20));
        GrayProbe probe = Mysteries.visit(h.getLevel(), MysteryLog.Kind.MUTILATION, cow.blockPosition(), cow);
        ServerPlayer player = TestPlayers.survival(h, new BlockPos(2, 1, 2));
        Vec3 at = probe.position();
        probe.hurtServer(h.getLevel(), h.getLevel().damageSources().playerAttack(player), 100.0F);
        h.succeedWhen(() -> h.assertTrue(h.getLevel().getEntitiesOfClass(ItemEntity.class, new AABB(at, at).inflate(4),
                item -> item.getItem().is(ModItems.GRAY_TECH_FRAGMENT.get())).stream().mapToInt(i -> i.getItem().getCount()).sum() >= 2,
                "no fragments from the probe"));
    }

    private static boolean mentions(Component component, String key) {
        if (component.getContents() instanceof TranslatableContents t && t.getKey().equals(key)) {
            return true;
        }
        return component.getSiblings().stream().anyMatch(c -> mentions(c, key));
    }

    /** The Gazette prints the week's strange doings; the wrangler grumbles about cattle, anyone about lights in the sky. */
    static void gazetteAndGrumbles(GameTestHelper h) {
        ServerPlayer reader = TestPlayers.survival(h, new BlockPos(20, 1, 20));
        BlockPos here = reader.blockPosition();
        MysteryLog log = MysteryLog.get(h.getLevel());
        // Stamped a little ahead, so these are the newest signs about whatever the tests round this one log
        log.record(MysteryLog.Kind.MUTILATION, h.getLevel().getGameTime() + 1000, here.offset(40, 0, 0));
        WrittenBookContent paper = GazetteItem.print(h.getLevel(), reader);
        h.assertTrue(paper.pages().stream().anyMatch(page -> mentions(page.raw(), "gazette.hollowfrontier.mystery.mutilation.title")),
                "the Gazette didn't report the mutilation");
        Context talk = Context.of(h.getLevel(), here, reader);
        h.assertTrue(talk.has(Fact.MUTILATIONS), "the mutilation isn't the talk of the town");
        h.assertTrue(grumbles(Folk.WRANGLER, talk).contains("message.hollowfrontier.grumble.mutilation.1"), "the wrangler's grumble");
        h.assertFalse(grumbles(Folk.BANKER, talk).stream().anyMatch(k -> k.contains("grumble.mutilation")), "the banker shouldn't talk cattle");
        log.record(MysteryLog.Kind.SIGHTING, h.getLevel().getGameTime() + 2000, here);
        h.assertTrue(grumbles(Folk.BANKER, Context.of(h.getLevel(), here, reader)).contains("message.hollowfrontier.grumble.sighting.2"),
                "the banker on the lights");
        TestPlayers.leave(reader);
        h.succeed();
    }

    // What a townsperson of this folk might say when you open their trades.
    private static List<String> grumbles(Folk folk, Context context) {
        return Chatter.candidates(List.of(folk.id(), "townsfolk"), Moment.TALK, context).stream().map(ChatterLines.Line::key).toList();
    }

    /** Boot Hill's strange grave holds the pilot: his skull, his logbook in glyphs, bits of his craft. */
    static void graveHoldsThePilot(GameTestHelper h) {
        var table = h.getLevel().getServer().reloadableRegistries().getLootTable(ModChestLoot.STRANGE_GRAVE);
        List<ItemStack> loot = new ArrayList<>(table.getRandomItems(new LootParams.Builder(h.getLevel())
                .withParameter(LootContextParams.ORIGIN, h.absoluteVec(Vec3.ZERO)).create(LootContextParamSets.CHEST)));
        h.assertTrue(loot.stream().anyMatch(s -> s.is(ModItems.STRANGE_SKULL.get())), "no skull in the grave: " + loot);
        h.assertTrue(loot.stream().anyMatch(s -> s.is(ModItems.GRAY_TECH_FRAGMENT.get())), "no fragments in the grave");
        ItemStack book = loot.stream().filter(s -> s.is(Items.WRITTEN_BOOK)).findFirst().orElseThrow(() -> new IllegalStateException("no logbook"));
        WrittenBookContent log = book.get(DataComponents.WRITTEN_BOOK_CONTENT);
        h.assertTrue(log != null && log.pages().size() == 4, "the logbook should have four pages");
        h.assertTrue(log.pages().getFirst().raw().getStyle().getFont().toString().contains("alt"), "the logbook should be in glyphs");
        h.succeed();
    }
}
