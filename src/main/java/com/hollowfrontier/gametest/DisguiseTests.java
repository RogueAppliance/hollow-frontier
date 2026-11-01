package com.hollowfrontier.gametest;

import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

import com.hollowfrontier.content.frontier.law.Wanted;
import com.hollowfrontier.content.frontier.talk.Chatter;
import com.hollowfrontier.content.frontier.talk.Context;
import com.hollowfrontier.content.frontier.talk.Fact;
import com.hollowfrontier.content.ufo.DisguisedGray;
import com.hollowfrontier.content.ufo.GrayVisitors;
import com.hollowfrontier.content.ufo.Leash;
import com.hollowfrontier.registry.ModBlocks;
import com.hollowfrontier.registry.ModItems;
import com.mojang.serialization.JsonOps;

import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.phys.AABB;

/**
 * Phase 21c: Grays in disguise. Strangers come only once the leash is broken; one speaks as a stranger and the townsfolk
 * near notice; a Sunstone Lens exposes it (hat off, a fragment dropped, the count reset) and it runs and is gone; three
 * visits left alone make the third stranger the regular (named, on a stool, kept in the town's saved count), who trades a
 * Phlogiston Cell for a Sarsaparilla once a day; and hitting the regular exposes him at a cost in heat. Each test has a town key
 * of its own, so the counts never meet. The world's leash is only broken and mended again inside one tick.
 */
final class DisguiseTests {
    private DisguiseTests() {}

    private static long townKey() {
        return ThreadLocalRandom.current().nextLong(1L << 40, 1L << 50);
    }

    private static AABB around(GameTestHelper h) {
        return new AABB(h.absolutePos(BlockPos.ZERO)).inflate(40);
    }

    /** No strangers while the leash holds; one visiting speaks as a stranger, and the townsfolk can tell. */
    static void strangersComeAfterTheLeash(GameTestHelper h) {
        ServerLevel level = h.getLevel();
        boolean was = Leash.isBroken(level);
        try {
            Leash.set(level, false);
            h.assertFalse(GrayVisitors.mayCome(level), "a stranger while the leash holds");
            Leash.set(level, true);
            h.assertTrue(GrayVisitors.mayCome(level), "no strangers after the leash");
        } finally {
            Leash.set(level, was);
        }
        DisguisedGray gray = GrayVisitors.visit(level, townKey(), h.absolutePos(new BlockPos(5, 1, 5)));
        h.assertTrue(gray != null && !gray.isExposed() && !gray.isRegular() && gray.visitLeft() == DisguisedGray.VISIT_TICKS, "no stranger came");
        h.assertTrue(gray.getMainHandItem().is(ModItems.FRONTIER_BEER.get()), "no beer in hand");
        h.assertValueEqual(Chatter.voices(gray), List.of("disguised_gray"), "voice");
        h.assertTrue(Context.of(level, gray.blockPosition(), null).has(Fact.STRANGER), "nobody notices the stranger");
        gray.discard();
        h.succeed();
    }

    /** The lens: hat off, a fragment dropped, the count back to nothing, Not From Around Here; it runs and is gone. */
    static void lensExposesAStranger(GameTestHelper h) {
        ServerLevel level = h.getLevel();
        long key = townKey();
        GrayVisitors.get(level).visitEnded(key, java.util.UUID.randomUUID()); // one visit already counted
        DisguisedGray gray = GrayVisitors.visit(level, key, h.absolutePos(new BlockPos(5, 1, 5)));
        ServerPlayer player = TestPlayers.survival(h, new BlockPos(3, 1, 5));
        try {
            player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(ModItems.SUNSTONE_LENS.get()));
            gray.mobInteract(player, InteractionHand.MAIN_HAND);
            h.assertTrue(gray.isExposed(), "the lens didn't find it out");
            h.assertFalse(gray.expose(level, player), "exposed twice");
            h.assertTrue(!level.getEntitiesOfClass(ItemEntity.class, around(h), i -> i.getItem().is(ModItems.GRAY_TECH_FRAGMENT.get())).isEmpty(), "no fragment dropped");
            h.assertValueEqual(GrayVisitors.get(level).town(key).visits(), 0, "the count after");
            h.assertTrue(AdvancementTests.earned(player, "ufo/not_from_around_here"), "Not From Around Here not earned");
            h.assertValueEqual(Chatter.voices(gray), List.of(), "an exposed Gray still talking");
            for (int i = 0; i <= DisguisedGray.FLEE_TICKS && !gray.isRemoved(); i++) {
                gray.aiStep();
            }
            h.assertTrue(gray.isRemoved(), "it didn't blink away");
        } finally {
            TestPlayers.leave(player);
            level.getEntitiesOfClass(Entity.class, around(h), e -> !(e instanceof ServerPlayer)).forEach(Entity::discard);
        }
        h.succeed();
    }

    /**
     * Three visits left alone: the first two leave, the third stays as Mr. Gray, on the stool by the bar, the town's
     * regular in its saved count. He trades a Phlogiston Cell for a Sarsaparilla, once a day (Regular Customer).
     */
    static void thirdVisitStays(GameTestHelper h) {
        ServerLevel level = h.getLevel();
        long key = townKey();
        BlockPos stool = h.absolutePos(new BlockPos(7, 1, 5));
        level.setBlock(stool, ModBlocks.BAR_STOOL.get().defaultBlockState(), Block.UPDATE_ALL);
        ServerPlayer player = TestPlayers.survival(h, new BlockPos(3, 1, 5));
        try {
            for (int visit = 1; visit <= 2; visit++) {
                DisguisedGray gray = GrayVisitors.visit(level, key, h.absolutePos(new BlockPos(5, 1, 5)));
                h.assertFalse(gray.endVisit(level), "visit " + visit + " stayed");
                h.assertTrue(gray.isRemoved(), "visit " + visit + " didn't leave");
            }
            DisguisedGray regular = GrayVisitors.visit(level, key, h.absolutePos(new BlockPos(5, 1, 5)));
            h.assertTrue(regular.endVisit(level), "the third didn't stay");
            h.assertTrue(regular.isRegular() && !regular.isRemoved() && regular.isPassenger() && regular.isPersistenceRequired(),
                    "not a regular on a stool");
            h.assertTrue(regular.getCustomName() != null && regular.getCustomName().getContents() instanceof net.minecraft.network.chat.contents.TranslatableContents name
                    && name.getKey().equals("entity.hollowfrontier.disguised_gray.regular"), "he isn't Mr. Gray");
            h.assertValueEqual(Chatter.voices(regular), List.of("mr_gray"), "voice");
            GrayVisitors.Town town = GrayVisitors.get(level).town(key);
            h.assertTrue(town.regular().filter(regular.getUUID()::equals).isPresent(), "the town doesn't know its regular");
            // Kept with the world: the count survives saving
            var saved = GrayVisitors.CODEC.encodeStart(JsonOps.INSTANCE, GrayVisitors.get(level)).getOrThrow();
            GrayVisitors loaded = GrayVisitors.CODEC.parse(JsonOps.INSTANCE, saved).getOrThrow();
            h.assertTrue(loaded.town(key).regular().filter(regular.getUUID()::equals).isPresent(), "the regular wasn't saved");
            ItemStack sarsaparilla = new ItemStack(ModItems.SARSAPARILLA.get(), 2);
            h.assertTrue(regular.trade(level, player, sarsaparilla), "no trade");
            h.assertTrue(player.getInventory().contains(s -> s.is(ModItems.PHLOGISTON_CELL.get())) && sarsaparilla.getCount() == 1, "the trade wasn't made");
            h.assertFalse(regular.trade(level, player, sarsaparilla), "traded twice in a day");
            h.assertTrue(AdvancementTests.earned(player, "ufo/regular_customer"), "Regular Customer not earned");
        } finally {
            TestPlayers.leave(player);
            level.getEntitiesOfClass(Entity.class, around(h), e -> !(e instanceof ServerPlayer)).forEach(Entity::discard);
        }
        h.succeed();
    }

    /** A punch exposes the regular: the town loses him, and the puncher's heat goes up. */
    static void exposingTheRegularCosts(GameTestHelper h) {
        ServerLevel level = h.getLevel();
        long key = townKey();
        ServerPlayer player = TestPlayers.survival(h, new BlockPos(3, 1, 5), "hf-gray-puncher");
        try {
            DisguisedGray regular = GrayVisitors.visit(level, key, h.absolutePos(new BlockPos(5, 1, 5)));
            for (int i = 0; i < GrayVisitors.VISITS_TO_STAY - 1; i++) {
                GrayVisitors.get(level).visitEnded(key, java.util.UUID.randomUUID());
            }
            h.assertTrue(regular.endVisit(level) && regular.isRegular(), "no regular");
            Wanted.clear(player);
            regular.hurtServer(level, player.damageSources().playerAttack(player), 1.0F);
            h.assertTrue(regular.isExposed() && !regular.isRegular(), "the punch didn't give him away");
            h.assertTrue(GrayVisitors.get(level).town(key).regular().isEmpty(), "the town still has a regular");
            h.assertValueEqual(Wanted.of(player).heat(), DisguisedGray.REGULAR_HEAT, "heat for exposing the regular");
            Wanted.clear(player);
        } finally {
            TestPlayers.leave(player);
            level.getEntitiesOfClass(Entity.class, around(h), e -> !(e instanceof ServerPlayer)).forEach(Entity::discard);
        }
        h.succeed();
    }
}
