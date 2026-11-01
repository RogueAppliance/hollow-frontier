package com.hollowfrontier.gametest;

import java.util.List;

import com.hollowfrontier.content.frontier.music.SaloonPianoBlock;
import com.hollowfrontier.content.frontier.music.SaloonPianoBlockEntity;
import com.hollowfrontier.content.frontier.saloon.Barmaid;
import com.hollowfrontier.content.frontier.saloon.Brawls;
import com.hollowfrontier.content.frontier.saloon.Drink;
import com.hollowfrontier.content.frontier.saloon.MissingTime;
import com.hollowfrontier.content.frontier.saloon.Moonshine;
import com.hollowfrontier.content.frontier.saloon.Slur;
import com.hollowfrontier.content.frontier.talk.Chatter;
import com.hollowfrontier.content.frontier.talk.Context;
import com.hollowfrontier.content.frontier.talk.Fact;
import com.hollowfrontier.content.frontier.talk.Moment;
import com.hollowfrontier.content.frontier.townsfolk.Townsfolk;
import com.hollowfrontier.registry.ModBlocks;
import com.hollowfrontier.registry.ModEntities;
import com.hollowfrontier.registry.ModItems;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.component.DataComponents;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.DoorBlock;
import net.minecraft.world.level.block.LayeredCauldronBlock;
import net.minecraft.world.level.block.state.properties.DoubleBlockHalf;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

/**
 * Phase 21b: a night at the saloon. Slurring changes words only from Tipsy II, the same way each time, and keeps the
 * text's style; Doc's tonic says one promise per outcome; Missing Time picks only safe spots in town and leaves a worn
 * hat; a brawl hushes the piano, ends with the loser thrown out the door, dazed but alive, and both sobered; the
 * moonshine's sighting is something the barmaid has words for.
 */
final class SaloonNightTests {
    private SaloonNightTests() {}

    private static <T extends Mob> T spawn(GameTestHelper h, EntityType<T> type, BlockPos at) {
        T mob = type.create(h.getLevel(), EntitySpawnReason.MOB_SUMMONED);
        mob.snapTo(Vec3.atBottomCenterOf(h.absolutePos(at)));
        h.getLevel().addFreshEntity(mob);
        return mob;
    }

    /** Sober and Tipsy I read straight; from Tipsy II words slur, the same way every time, keeping punctuation and style. */
    static void slurringIsSteady(GameTestHelper h) {
        String line = "Whatever you think you saw out there, partner, you didn't. Have another coffee and forget it.";
        h.assertValueEqual(Slur.slur(line, 0), line, "sober");
        h.assertValueEqual(Slur.slur(line, 1), line, "Tipsy I");
        String slurred = Slur.slur(line, 4);
        h.assertTrue(!slurred.equals(line), "Tipsy IV didn't slur: " + slurred);
        h.assertValueEqual(Slur.slur(line, 4), slurred, "slurred twice the same line");
        h.assertValueEqual(slurred.replaceAll("[\\p{L}]", ""), line.replaceAll("[\\p{L}]", ""), "spaces and punctuation");
        h.assertTrue(slurred.length() - line.length() <= line.split(" ").length, "too much added: " + slurred);
        Component styled = Component.literal(line).withStyle(ChatFormatting.AQUA);
        Component out = Slur.slur(styled, 4);
        h.assertValueEqual(out.getString(), slurred, "the component's text");
        h.assertTrue(out.getSiblings().stream().allMatch(c -> c.getStyle().getColor() != null), "the style was lost");
        h.succeed();
    }

    /** Each of Doc's outcomes comes with its own promise, and the outcome itself. */
    static void snakeOilPromises(GameTestHelper h) {
        ServerPlayer player = TestPlayers.survival(h, new BlockPos(2, 1, 2));
        try {
            for (int i = 0; i < Drink.GAMBLE.size(); i++) {
                player.removeAllEffects();
                String claim = Drink.gamble(player, i);
                h.assertValueEqual(claim, "message.hollowfrontier.snake_oil." + i, "promise " + i);
                h.assertTrue(player.hasEffect(Drink.GAMBLE.get(i).effect()), "outcome " + i + " didn't happen");
            }
            player.removeAllEffects();
        } finally {
            TestPlayers.leave(player);
        }
        h.succeed();
    }

    /**
     * Spots: a hay bale with room over it, a full trough and a bed's head count; a bale under a slab, one with a zombie
     * by it, and one outside the town don't. Woken at one: the player's on it, with a worn hat and Missing Time?. Out of
     * any town, nothing happens.
     */
    static void missingTimeIsSafe(GameTestHelper h) {
        ServerLevel level = h.getLevel();
        BlockPos hay = h.absolutePos(new BlockPos(4, 1, 4)), roofed = h.absolutePos(new BlockPos(8, 1, 4)),
                trough = h.absolutePos(new BlockPos(12, 1, 4)), bed = h.absolutePos(new BlockPos(16, 1, 4)),
                guarded = h.absolutePos(new BlockPos(4, 1, 30)), outside = h.absolutePos(new BlockPos(40, 1, 4));
        level.setBlock(hay, Blocks.HAY_BLOCK.defaultBlockState(), Block.UPDATE_ALL);
        level.setBlock(roofed, Blocks.HAY_BLOCK.defaultBlockState(), Block.UPDATE_ALL);
        level.setBlock(roofed.above(2), Blocks.STONE.defaultBlockState(), Block.UPDATE_ALL);
        level.setBlock(trough, Blocks.WATER_CAULDRON.defaultBlockState().setValue(LayeredCauldronBlock.LEVEL, 3), Block.UPDATE_ALL);
        level.setBlock(bed, Blocks.BED.red().defaultBlockState().setValue(net.minecraft.world.level.block.BedBlock.PART, net.minecraft.world.level.block.state.properties.BedPart.HEAD), Block.UPDATE_ALL);
        level.setBlock(guarded, Blocks.HAY_BLOCK.defaultBlockState(), Block.UPDATE_ALL);
        level.setBlock(outside, Blocks.HAY_BLOCK.defaultBlockState(), Block.UPDATE_ALL);
        Mob zombie = spawn(h, net.minecraft.world.entity.EntityTypes.ZOMBIE, new BlockPos(6, 1, 30));
        ServerPlayer player = TestPlayers.survival(h, new BlockPos(10, 1, 12));
        try {
            BlockPos origin = h.absolutePos(BlockPos.ZERO);
            List<MissingTime.Spot> spots = MissingTime.spots(level, player.blockPosition(), pos -> pos.getX() - origin.getX() < 30);
            List<String> kinds = spots.stream().map(MissingTime.Spot::kind).sorted().toList();
            h.assertValueEqual(kinds, List.of("bed", "hay", "trough"), "the safe spots");
            h.assertTrue(spots.stream().noneMatch(s -> BlockPos.containing(s.at()).below().equals(roofed)), "the roofed bale counted");
            h.assertTrue(MissingTime.wakeUp(level, player, level.getRandom()) == null, "woke somewhere out of town");
            MissingTime.Spot spot = MissingTime.wakeUp(level, player, level.getRandom(), spots);
            h.assertTrue(spot != null && player.position().distanceTo(spot.at()) < 0.01, "not set down on the spot");
            boolean hat = false;
            for (ItemStack stack : player.getInventory()) {
                hat |= stack.is(ModItems.COWBOY_HAT.get()) && stack.has(DataComponents.CUSTOM_NAME) && stack.isDamaged();
            }
            h.assertTrue(hat, "no worn hat");
            h.assertTrue(AdvancementTests.earned(player, "frontier/missing_time_question"), "Missing Time? not earned");
        } finally {
            TestPlayers.leave(player);
            zombie.discard();
        }
        h.succeed();
    }

    /**
     * Two tipsy townsfolk by a barmaid: the brawl hushes the piano; at its end the one with fewer punches is out through
     * the door (on the far side from the bar), dazed and alive, both sober, and the piano's free to play.
     */
    static void brawlThrowsOneOut(GameTestHelper h) {
        ServerLevel level = h.getLevel();
        BlockPos door = h.absolutePos(new BlockPos(10, 1, 4));
        DoorBlock oak = (DoorBlock) Blocks.OAK_DOOR;
        level.setBlock(door, oak.defaultBlockState().setValue(DoorBlock.HALF, DoubleBlockHalf.LOWER), Block.UPDATE_CLIENTS);
        level.setBlock(door.above(), oak.defaultBlockState().setValue(DoorBlock.HALF, DoubleBlockHalf.UPPER), Block.UPDATE_CLIENTS);
        BlockPos piano = h.absolutePos(new BlockPos(14, 1, 12));
        level.setBlock(piano, ModBlocks.SALOON_PIANO.get().defaultBlockState(), Block.UPDATE_ALL);
        Barmaid barmaid = spawn(h, ModEntities.BARMAID.get(), new BlockPos(10, 1, 12));
        Townsfolk a = spawn(h, ModEntities.TOWNSFOLK.get(), new BlockPos(9, 1, 9));
        Townsfolk b = spawn(h, ModEntities.TOWNSFOLK.get(), new BlockPos(11, 1, 9));
        try {
            h.assertFalse(Brawls.pair(a, b), "sober townsfolk came to blows");
            Drink.addTipsy(a, 1);
            Drink.addTipsy(b, 2);
            h.assertTrue(Brawls.pair(a, b), "tipsy townsfolk wouldn't fight");
            h.assertFalse(SaloonPianoBlockEntity.trouble(level, piano), "trouble before the fight");
            Brawls.Brawl brawl = Brawls.start(level, a, b, barmaid);
            h.assertTrue(brawl != null && Brawls.fighting(a) && Brawls.fighting(b), "no brawl");
            h.assertTrue(Brawls.start(level, a, b, barmaid) == null, "a second brawl between the same two");
            h.assertTrue(SaloonPianoBlockEntity.trouble(level, piano), "the piano plays on through a brawl");
            for (int i = 0; i < 30; i++) {
                Brawls.tick(level, brawl);
            }
            Brawls.hurry(brawl);
            h.assertTrue(Brawls.tick(level, brawl), "the brawl didn't end");
            Brawls.clear();
            Mob loser = List.of(a, b).stream().filter(m -> m.hasEffect(MobEffects.SLOWNESS)).findFirst().orElse(null);
            h.assertTrue(loser != null && loser.isAlive() && a.isAlive() && b.isAlive(), "nobody was thrown out, or somebody died");
            h.assertTrue(loser.getZ() < door.getZ(), "the loser isn't out the door: " + loser.position() + " door " + door);
            h.assertTrue(Drink.tipsyLevel(a) == 0 && Drink.tipsyLevel(b) == 0, "still drunk after the fight");
            h.assertFalse(SaloonPianoBlockEntity.trouble(level, piano), "still trouble after the fight");
            h.assertTrue(level.getBlockState(piano).hasProperty(SaloonPianoBlock.HUSHED), "the piano can't hush");
        } finally {
            Brawls.clear();
            level.getEntitiesOfClass(Entity.class, new AABB(h.absolutePos(BlockPos.ZERO)).expandTowards(48, 28, 48),
                    e -> !(e instanceof ServerPlayer)).forEach(Entity::discard);
        }
        h.succeed();
    }

    /** Moonshine only shows things at Tipsy III; once it has, the barmaid's heard it all before. */
    static void moonshineShowsThings(GameTestHelper h) {
        ServerPlayer player = TestPlayers.survival(h, new BlockPos(2, 1, 2));
        try {
            h.assertFalse(Moonshine.drunk(player), "a sighting while sober");
            h.assertFalse(Moonshine.seeingThings(player), "seeing things already");
            Moonshine.noteSeen(player);
            h.assertTrue(Moonshine.seeingThings(player), "didn't see it");
            Context context = Context.of(h.getLevel(), player.blockPosition(), player);
            h.assertTrue(context.has(Fact.SEEING_THINGS), "the fact isn't known");
            h.assertTrue(Chatter.candidates(List.of("josie", "barmaid"), Moment.TALK, context).stream().anyMatch(l -> l.cond().all().contains(Fact.SEEING_THINGS)),
                    "the barmaid has nothing to say about it");
        } finally {
            Moonshine.forget(player);
            TestPlayers.leave(player);
        }
        h.succeed();
    }
}
