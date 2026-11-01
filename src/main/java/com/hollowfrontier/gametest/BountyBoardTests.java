package com.hollowfrontier.gametest;

import java.util.HashSet;
import java.util.List;

import com.hollowfrontier.content.frontier.bounty.BoardFace;
import com.hollowfrontier.content.frontier.bounty.BoardPosting;
import com.hollowfrontier.content.frontier.bounty.BountyBoardBlockEntity;
import com.hollowfrontier.content.frontier.bounty.BountyBoardEntity;
import com.hollowfrontier.content.frontier.bounty.BountyBoards;
import com.hollowfrontier.content.frontier.bounty.BountyData;
import com.hollowfrontier.content.frontier.law.Wanted;
import com.hollowfrontier.registry.ModBlocks;
import com.hollowfrontier.registry.ModDataComponents;
import com.hollowfrontier.registry.ModItems;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;

// The saloon bounty board (18): what it posts, taking and turning in, the dawn round, shared town lists, wanted players.
final class BountyBoardTests {
    private BountyBoardTests() {}

    private static BountyBoardBlockEntity board(GameTestHelper h, BlockPos relative) {
        h.setBlock(relative, ModBlocks.BOUNTY_BOARD.get());
        BountyBoardBlockEntity board = (BountyBoardBlockEntity) h.getLevel().getBlockEntity(h.absolutePos(relative));
        board.sync(h.getLevel());
        return board;
    }

    private static BountyData carried(ServerPlayer player) {
        for (int i = 0; i < player.getInventory().getContainerSize(); i++) {
            ItemStack stack = player.getInventory().getItem(i);
            if (stack.is(ModItems.WANTED_POSTER.get())) {
                return stack.get(ModDataComponents.BOUNTY.get());
            }
        }
        return null;
    }

    // A use on the north face at (u, v) across and down it
    private static void useAt(GameTestHelper h, BlockPos relative, ServerPlayer player, float u, float v) {
        BlockPos abs = h.absolutePos(relative);
        Vec3 at = new Vec3(abs.getX() + 1.0 - u, abs.getY() + 1.0 - v, abs.getZ());
        h.useBlock(relative, player, new BlockHitResult(at, Direction.NORTH, abs, false));
    }

    /** A fresh board puts up three different outlaws, 200-500 blocks off. */
    static void postsBounties(GameTestHelper h) {
        BountyBoardBlockEntity board = board(h, new BlockPos(5, 1, 5));
        List<BoardPosting> shown = board.face().shown();
        h.assertTrue(shown.size() == BountyBoards.OUTLAWS, "expected 3 posters, got " + shown.size());
        h.assertTrue(new HashSet<>(shown.stream().map(p -> p.bounty().id()).toList()).size() == 3, "the same bounty twice");
        BlockPos at = board.getBlockPos();
        for (BoardPosting posting : shown) {
            double d = Math.hypot(posting.bounty().x() - at.getX(), posting.bounty().z() - at.getZ());
            h.assertTrue(posting.status() == BoardPosting.Status.OPEN && d >= 195 && d <= 505, "bad posting " + posting + " at " + d);
        }
        List<BoardFace.Slot> slots = board.face().slots(2, 2);
        h.assertTrue(slots.stream().filter(s -> s.kind() == BoardFace.Kind.OUTLAW).count() == 3, "not every poster has a place on the face");
        h.assertTrue(slots.size() == 4 && slots.get(3).kind() == BoardFace.Kind.SURVEY, "the Polar Survey's notice (32d) should fill the last cell: " + slots);
        h.succeed();
    }

    /** You take the poster you click; it's marked yours, and nobody else can have it. */
    static void takeMarksTaken(GameTestHelper h) {
        BlockPos at = new BlockPos(5, 1, 5);
        BountyBoardBlockEntity board = board(h, at);
        ServerPlayer ann = TestPlayers.survival(h, new BlockPos(5, 1, 3), "Ann");
        useAt(h, at, ann, 0.75F, 0.25F); // the top right poster: the second outlaw
        BountyData taken = carried(ann);
        h.assertTrue(taken != null && taken.id().equals(board.face().shown().get(1).bounty().id()), "took the wrong poster: " + taken);
        h.assertTrue(board.face().shown().get(1).status() == BoardPosting.Status.TAKEN && board.face().shown().get(1).takenBy().equals("Ann"),
                "not marked taken: " + board.face().shown().get(1));
        ServerPlayer bob = TestPlayers.survival(h, new BlockPos(3, 1, 3), "Bob");
        useAt(h, at, bob, 0.75F, 0.25F);
        h.assertTrue(carried(bob) == null, "Bob took Ann's poster");
        useAt(h, at, bob, 0.25F, 0.25F);
        h.assertTrue(carried(bob) != null && !carried(bob).id().equals(taken.id()), "Bob couldn't take an open poster");
        TestPlayers.leave(ann, bob);
        h.succeed();
    }

    /** A badge brought back stamps the poster DEAD; the next dawn takes it down and puts a new outlaw up. */
    static void turnInAndDawn(GameTestHelper h) {
        BlockPos at = new BlockPos(5, 1, 5);
        BountyBoardBlockEntity board = board(h, at);
        ServerPlayer player = TestPlayers.survival(h, new BlockPos(5, 1, 3));
        h.useBlock(at, player);
        BountyData bounty = carried(player);
        ItemStack badge = new ItemStack(ModItems.OUTLAW_BADGE.get());
        badge.set(ModDataComponents.BOUNTY.get(), bounty);
        player.setItemInHand(InteractionHand.MAIN_HAND, badge);
        h.useBlock(at, player);
        BoardPosting dead = board.face().shown().stream().filter(p -> p.bounty().id().equals(bounty.id())).findFirst().orElseThrow();
        h.assertTrue(dead.status() == BoardPosting.Status.DEAD, "not stamped dead: " + dead);
        ServerLevel level = h.getLevel();
        BountyBoards.Board list = BountyBoards.get(level).peek(board.key(h.getLevel())).orElseThrow();
        BountyBoards.refresh(list, list.day() + 1, board.getBlockPos(), level.getRandom());
        h.assertTrue(list.postings().size() == 3 && list.postings().stream().noneMatch(p -> p.bounty().id().equals(bounty.id())),
                "the dawn round left " + list.postings());
        h.assertTrue(list.postings().stream().anyMatch(p -> p.status() == BoardPosting.Status.OPEN && p.postedDay() == list.day()), "no fresh poster");
        TestPlayers.leave(player);
        h.succeed();
    }

    /** Open posters go stale after three days, taken ones after a week. */
    static void stalePostersComeDown(GameTestHelper h) {
        BlockPos origin = h.absolutePos(new BlockPos(5, 1, 5));
        String key = "test:stale:" + origin.asLong();
        BountyData open = BountyBoards.newBounty(origin, h.getLevel().getRandom()), taken = BountyBoards.newBounty(origin, h.getLevel().getRandom());
        BountyBoards boards = BountyBoards.get(h.getLevel());
        boards.put(key, List.of(BoardPosting.open(open, false, 10), BoardPosting.open(taken, false, 10).taken("Ann")), 12);
        BountyBoards.Board list = boards.peek(key).orElseThrow();
        BountyBoards.refresh(list, 13, origin, h.getLevel().getRandom());
        h.assertTrue(list.postings().stream().noneMatch(p -> p.bounty().equals(open)), "a three-day-old poster stayed up");
        h.assertTrue(list.postings().stream().anyMatch(p -> p.bounty().equals(taken)), "a taken poster came down too soon");
        BountyBoards.refresh(list, 17, origin, h.getLevel().getRandom());
        h.assertTrue(list.postings().stream().noneMatch(p -> p.bounty().equals(taken)), "a week-old taken poster stayed up");
        h.succeed();
    }

    /** Boards sharing a list (as a town's do) show the same posters, and a take on one shows on the other. */
    static void boardsShareAList(GameTestHelper h) {
        BountyBoardBlockEntity first = board(h, new BlockPos(3, 1, 5));
        BountyBoardBlockEntity second = board(h, new BlockPos(8, 1, 5));
        second.setKey(first.key(h.getLevel()));
        second.sync(h.getLevel());
        h.assertTrue(first.face().shown().equals(second.face().shown()), "the boards differ");
        ServerPlayer player = TestPlayers.survival(h, new BlockPos(3, 1, 3));
        h.useBlock(new BlockPos(3, 1, 5), player);
        second.sync(h.getLevel());
        h.assertTrue(second.face().shown().stream().anyMatch(p -> p.status() == BoardPosting.Status.TAKEN), "the take didn't show on the other board");
        TestPlayers.leave(player);
        h.succeed();
    }

    /** A player with two stars gets a poster of their own nearby, and paying the fine takes it down. */
    static void wantedPlayersPosted(GameTestHelper h) {
        BlockPos at = new BlockPos(5, 1, 5);
        BountyBoardBlockEntity board = board(h, at);
        ServerPlayer player = TestPlayers.survival(h, new BlockPos(5, 1, 3), "Desperado");
        Wanted.addHeat(player, 5);
        board.sync(h.getLevel());
        h.assertTrue(board.face().wanted().stream().anyMatch(f -> f.name().equals("Desperado")), "no poster for a two-star player: " + board.face().wanted());
        player.getInventory().add(new ItemStack(ModItems.GOLD_COIN.get(), 9));
        h.useBlock(at, player);
        h.assertTrue(board.face().wanted().stream().noneMatch(f -> f.name().equals("Desperado")), "the poster stayed up after the fine");
        TestPlayers.leave(player);
        h.succeed();
    }

    // A stone wall along z = 8, from x0 to x1 and two high, to hang boards on (facing north, out of it)
    private static void wall(GameTestHelper h, int x0, int x1) {
        for (int x = x0; x <= x1; x++) {
            for (int y = 1; y <= 2; y++) {
                h.setBlock(new BlockPos(x, y, 8), net.minecraft.world.level.block.Blocks.STONE);
            }
        }
    }

    /** A hanging board takes three across where the wall allows, else two, and won't hang on one block of wall. */
    static void hangingBoardSizes(GameTestHelper h) {
        ServerLevel level = h.getLevel();
        wall(h, 1, 3);
        wall(h, 6, 7);
        wall(h, 10, 10);
        var wide = BountyBoardEntity.create(level, h.absolutePos(new BlockPos(2, 1, 7)), Direction.NORTH);
        h.assertTrue(wide.isPresent() && wide.get().columns() == BountyBoardEntity.WIDE, "no three-across board on a wall three wide");
        // Two across hangs from its bottom left as you face it: facing north, that's the east end (x 7, then 6)
        var narrow = BountyBoardEntity.create(level, h.absolutePos(new BlockPos(7, 1, 7)), Direction.NORTH);
        h.assertTrue(narrow.isPresent() && narrow.get().columns() == BountyBoardEntity.NARROW, "no two-across board on a wall two wide");
        h.assertTrue(BountyBoardEntity.create(level, h.absolutePos(new BlockPos(10, 1, 7)), Direction.NORTH).isEmpty(), "a board hung on one block of wall");
        h.succeed();
    }

    /** Clicking a poster on a hanging board takes that one (the top left of three across, here). */
    static void hangingBoardTakeByClick(GameTestHelper h) {
        ServerLevel level = h.getLevel();
        wall(h, 1, 3);
        BountyBoardEntity board = BountyBoardEntity.create(level, h.absolutePos(new BlockPos(2, 1, 7)), Direction.NORTH).orElseThrow();
        level.addFreshEntity(board);
        board.sync(level);
        h.assertTrue(board.face().shown().size() == BountyBoards.OUTLAWS, "the board put nothing up");
        ServerPlayer player = TestPlayers.survival(h, new BlockPos(2, 1, 4));
        // Facing north, "right" is west (-x): the left column is +1 from the middle, the top row +0.5 up
        board.interact(player, InteractionHand.MAIN_HAND, new Vec3(1.0, 0.5, 0.0));
        BountyData taken = carried(player);
        h.assertTrue(taken != null && taken.id().equals(board.face().shown().getFirst().bounty().id()), "took the wrong poster: " + taken);
        h.assertTrue(board.face().shown().getFirst().status() == BoardPosting.Status.TAKEN, "not marked taken on the board");
        board.discard();
        TestPlayers.leave(player);
        h.succeed();
    }

    /** Every structure with a hanging board holds it up: the wall behind it is there, and nothing's in the way. */
    static void structureBoardsHang(GameTestHelper h) {
        ServerLevel level = h.getLevel();
        var manager = level.getStructureTemplateManager();
        String id = com.hollowfrontier.registry.ModEntities.HANGING_BOUNTY_BOARD.getId().toString();
        List<net.minecraft.resources.Identifier> names = manager.listTemplates()
                .filter(n -> n.getNamespace().equals(com.hollowfrontier.HollowFrontier.MODID)).sorted().toList();
        int placed = 0;
        for (var name : names) {
            var template = manager.get(name).orElseThrow();
            if (!template.save(new net.minecraft.nbt.CompoundTag()).toString().contains(id)) {
                continue;
            }
            BlockPos origin = h.absolutePos(new BlockPos(0, 120, 0)); // each in the same spot, cleared after: further out, entities aren't loaded
            template.placeInWorld(level, origin, origin, new net.minecraft.world.level.levelgen.structure.templatesystem.StructurePlaceSettings(),
                    level.getRandom(), net.minecraft.world.level.block.Block.UPDATE_CLIENTS);
            var size = template.getSize();
            List<BountyBoardEntity> boards = level.getEntitiesOfClass(BountyBoardEntity.class,
                    new net.minecraft.world.phys.AABB(Vec3.atCenterOf(origin), Vec3.atCenterOf(origin.offset(size))).inflate(1));
            h.assertFalse(boards.isEmpty(), name + ": no board hung");
            for (BountyBoardEntity board : boards) {
                h.assertTrue(board.survives(), name + ": the board at " + board.getPos().subtract(origin) + " can't hang there");
                board.discard();
            }
            for (BlockPos pos : BlockPos.betweenClosed(origin, origin.offset(size))) {
                level.setBlock(pos, net.minecraft.world.level.block.Blocks.AIR.defaultBlockState(), net.minecraft.world.level.block.Block.UPDATE_CLIENTS | net.minecraft.world.level.block.Block.UPDATE_SUPPRESS_DROPS);
            }
            level.getEntitiesOfClass(net.minecraft.world.entity.Entity.class, new net.minecraft.world.phys.AABB(Vec3.atCenterOf(origin), Vec3.atCenterOf(origin.offset(size))).inflate(2),
                    e -> !(e instanceof ServerPlayer)).forEach(net.minecraft.world.entity.Entity::discard);
            placed++;
        }
        h.assertTrue(placed >= 3, "only " + placed + " structures have a hanging board");
        h.succeed();
    }

    /** A board from before 18 (no block entity) gets one, posters and all, on a random tick. */
    static void oldBoardsCatchUp(GameTestHelper h) {
        BlockPos at = new BlockPos(5, 1, 5);
        h.setBlock(at, ModBlocks.BOUNTY_BOARD.get());
        BlockPos abs = h.absolutePos(at);
        ServerLevel level = h.getLevel();
        level.removeBlockEntity(abs);
        level.getBlockState(abs).randomTick(level, abs, level.getRandom());
        h.assertTrue(level.getBlockEntity(abs) instanceof BountyBoardBlockEntity board && board.face().shown().size() == BountyBoards.OUTLAWS,
                "the old board never put posters up");
        h.succeed();
    }
}
