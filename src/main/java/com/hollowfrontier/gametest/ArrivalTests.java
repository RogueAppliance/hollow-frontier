package com.hollowfrontier.gametest;

import java.util.List;

import com.hollowfrontier.content.frontier.outlaw.FreedMen;
import com.hollowfrontier.content.frontier.outlaw.Outlaw;
import com.hollowfrontier.content.frontier.saloon.Barmaid;
import com.hollowfrontier.content.frontier.town.TownVacancies;
import com.hollowfrontier.content.frontier.townsfolk.Folk;
import com.hollowfrontier.content.frontier.townsfolk.Lawman;
import com.hollowfrontier.content.frontier.townsfolk.Townsfolk;
import com.hollowfrontier.registry.ModEntities;
import com.hollowfrontier.registry.ModItems;

import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.NbtOps;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.RegistryOps;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

// Towns replacing their dead (TownVacancies), in the raid test town. Posts are in its closed hut, out of sight of other tests' players.
final class ArrivalTests {
    private ArrivalTests() {}

    private static final BlockPos POST = new BlockPos(5, 1, 16);
    private static final BlockPos OTHER_POST = new BlockPos(4, 1, 15);
    private static final BlockPos STREET = new BlockPos(10, 1, 6);
    private static final BlockPos HUT_DOOR = new BlockPos(5, 1, 12);

    private static long key(GameTestHelper h) {
        return h.absolutePos(BlockPos.ZERO).asLong();
    }

    private static BoundingBox bounds(GameTestHelper h) {
        return BoundingBox.fromCorners(h.absolutePos(BlockPos.ZERO), h.absolutePos(new BlockPos(23, 7, 23)));
    }

    private static TownVacancies settle(GameTestHelper h) {
        TownVacancies data = TownVacancies.get(h.getLevel());
        data.forget(key(h)); // a failed run's leftovers
        data.settle(key(h), bounds(h));
        return data;
    }

    private static Townsfolk folk(GameTestHelper h, BlockPos pos, EntitySpawnReason reason) {
        Townsfolk folk = ModEntities.TOWNSFOLK.get().spawn(h.getLevel(), h.absolutePos(pos), reason);
        folk.setNoAi(true);
        return folk;
    }

    private static Lawman lawman(GameTestHelper h, BlockPos pos, EntitySpawnReason reason) {
        Lawman lawman = ModEntities.LAWMAN.get().spawn(h.getLevel(), h.absolutePos(pos), reason);
        lawman.setNoAi(true);
        return lawman;
    }

    private static List<Townsfolk> livingFolk(GameTestHelper h) {
        return h.getLevel().getEntitiesOfClass(Townsfolk.class, AABB.of(bounds(h)), Townsfolk::isAlive);
    }

    private static List<Lawman> livingLawmen(GameTestHelper h) {
        return h.getLevel().getEntitiesOfClass(Lawman.class, AABB.of(bounds(h)), Lawman::isAlive);
    }

    private static void moveTo(GameTestHelper h, net.minecraft.world.entity.Entity entity, BlockPos pos) {
        Vec3 at = Vec3.atBottomCenterOf(h.absolutePos(pos));
        entity.setPos(at.x, at.y, at.z);
    }

    /** Both come back in their old jobs at their old posts, a wait apart, and a town never holds more than it started with. */
    static void replacedOneAtATime(GameTestHelper h) {
        ServerLevel level = h.getLevel();
        TownVacancies data = settle(h);
        Townsfolk banker = folk(h, POST, EntitySpawnReason.STRUCTURE);
        banker.setFolk(Folk.BANKER);
        Lawman marshal = lawman(h, OTHER_POST, EntitySpawnReason.STRUCTURE);
        marshal.setRank(Lawman.Rank.MARSHAL);
        h.runAfterDelay(2, () -> {
            // They die out in the street, but their posts are where they kept.
            moveTo(h, banker, STREET);
            moveTo(h, marshal, STREET);
            banker.kill(level);
            marshal.kill(level);
            List<TownVacancies.Vacancy> open = data.open(key(h));
            h.assertTrue(open.size() == 2, "expected two open places: " + open);
            h.assertTrue(open.get(0).post().equals(h.absolutePos(POST)) && open.get(0).role().equals("banker"), "wrong first place: " + open);
            h.assertTrue(open.get(1).post().equals(h.absolutePos(OTHER_POST)) && open.get(1).role().equals("marshal"), "wrong second place: " + open);

            data.check(level, 1000);
            h.assertTrue(livingFolk(h).isEmpty(), "someone arrived before the wait was over");

            data.check(level, 0);
            h.assertTrue(livingFolk(h).size() == 1 && livingLawmen(h).isEmpty(), "first arrival wrong: " + livingFolk(h) + livingLawmen(h));
            Townsfolk newBanker = livingFolk(h).getFirst();
            h.assertTrue(newBanker.getFolk() == Folk.BANKER, "the banker's place went to a " + newBanker.getFolk());
            h.assertTrue(newBanker.blockPosition().equals(h.absolutePos(POST)), "banker arrived away from the post: " + newBanker.blockPosition());
            h.assertTrue(h.absolutePos(POST).equals(newBanker.getWorkplace()), "banker's workplace: " + newBanker.getWorkplace());

            data.check(level, 1);
            h.assertTrue(livingLawmen(h).isEmpty(), "two arrivals without a wait between");

            data.check(level, 0);
            h.assertTrue(livingLawmen(h).size() == 1, "the marshal wasn't replaced");
            Lawman newMarshal = livingLawmen(h).getFirst();
            h.assertTrue(newMarshal.getRank() == Lawman.Rank.MARSHAL, "the marshal's place went to a " + newMarshal.getRank());
            h.assertTrue(newMarshal.getMainHandItem().is(ModItems.PLAINS_RIFLE.get()), "new marshal unarmed: " + newMarshal.getMainHandItem());
            h.assertTrue(newMarshal.getHomePosition().equals(h.absolutePos(OTHER_POST)), "new marshal's post: " + newMarshal.getHomePosition());
            h.assertTrue(data.open(key(h)).isEmpty(), "places still open: " + data.open(key(h)));

            data.check(level, 0);
            h.assertTrue(livingFolk(h).size() == 1 && livingLawmen(h).size() == 1, "the town grew past what it started with");

            // A replacement is a resident in turn.
            newBanker.setNoAi(true);
            newBanker.kill(level);
            h.assertTrue(data.open(key(h)).size() == 1, "the new banker wasn't missed");
            data.check(level, 0);
            h.assertTrue(livingFolk(h).size() == 1, "the new banker wasn't replaced");
            data.forget(key(h));
            h.succeed();
        });
    }

    static void barmaidReplacedAsBarmaid(GameTestHelper h) {
        ServerLevel level = h.getLevel();
        TownVacancies data = settle(h);
        Barmaid barmaid = ModEntities.BARMAID.get().spawn(level, h.absolutePos(POST), EntitySpawnReason.STRUCTURE);
        barmaid.kill(level);
        data.check(level, 0);
        h.assertTrue(livingFolk(h).size() == 1 && livingFolk(h).getFirst() instanceof Barmaid, "not replaced by a barmaid: " + livingFolk(h));
        data.forget(key(h));
        h.succeed();
    }

    /** Only the town's own leave a place behind: not the spawn egg's, the sworn-in or settled freed men, or a posse. */
    static void strangersNotReplaced(GameTestHelper h) {
        ServerLevel level = h.getLevel();
        TownVacancies data = settle(h);
        ServerPlayer player = TestPlayers.survival(h, STREET);
        folk(h, POST, EntitySpawnReason.SPAWN_ITEM_USE).kill(level);
        lawman(h, POST, EntitySpawnReason.SPAWN_ITEM_USE).kill(level);
        Lawman rider = lawman(h, POST, EntitySpawnReason.EVENT);
        rider.joinPosse(player);
        rider.kill(level);
        Outlaw settler = ModEntities.OUTLAW.get().spawn(level, h.absolutePos(POST), EntitySpawnReason.SPAWN_ITEM_USE);
        Townsfolk prospector = FreedMen.sendHome(level, player, settler);
        prospector.kill(level);
        Outlaw recruit = ModEntities.OUTLAW.get().spawn(level, h.absolutePos(POST), EntitySpawnReason.SPAWN_ITEM_USE);
        Lawman deputy = FreedMen.hire(level, player, recruit, new ItemStack(ModItems.GOLD_COIN.get(), 9));
        deputy.kill(level);
        h.assertTrue(data.open(key(h)).isEmpty(), "strangers left places behind: " + data.open(key(h)));
        data.forget(key(h));
        h.succeed();
    }

    /** Nobody appears with a player near, even through a wall, or at a post a player can see; a shut door is enough to hide them. */
    static void arrivalsKeepOutOfSight(GameTestHelper h) {
        ServerLevel level = h.getLevel();
        TownVacancies data = settle(h);
        folk(h, POST, EntitySpawnReason.STRUCTURE).kill(level);
        ServerPlayer player = TestPlayers.survival(h, new BlockPos(5, 1, 14));
        data.check(level, 0);
        h.assertTrue(data.open(key(h)).size() == 1, "arrived with a player in the room, at " + livingFolk(h));

        // In the street with the hut's door off, looking in at the post: they arrive where the doorway doesn't show.
        h.setBlock(HUT_DOOR.above(), Blocks.AIR);
        h.setBlock(HUT_DOOR, Blocks.AIR);
        moveTo(h, player, new BlockPos(5, 1, 7));
        data.check(level, 0);
        h.assertTrue(livingFolk(h).size() == 1, "no arrival with a hidden spot beside the post");
        Townsfolk first = livingFolk(h).getFirst();
        h.assertFalse(first.blockPosition().equals(h.absolutePos(POST)), "arrived at a post a player was looking at");
        h.assertTrue(h.absolutePos(POST).equals(first.getWorkplace()), "kept out of sight, but lost the post: " + first.getWorkplace());

        // The doorway shut again, the post itself will do.
        first.kill(level);
        h.setBlock(HUT_DOOR, Blocks.STONE);
        h.setBlock(HUT_DOOR.above(), Blocks.STONE);
        data.check(level, 0);
        h.assertTrue(livingFolk(h).size() == 1 && livingFolk(h).getFirst().blockPosition().equals(h.absolutePos(POST)),
                "a wall didn't hide the post: " + livingFolk(h));
        data.forget(key(h));
        h.succeed();
    }

    /** A post with no room left (its building's gone or filled in): they arrive on the ground over it. */
    static void arrivalsFindGroundOverLostPost(GameTestHelper h) {
        ServerLevel level = h.getLevel();
        TownVacancies data = settle(h);
        folk(h, POST, EntitySpawnReason.STRUCTURE).kill(level);
        for (BlockPos pos : BlockPos.betweenClosed(new BlockPos(3, 1, 13), new BlockPos(7, 3, 19))) {
            h.setBlock(pos.immutable(), Blocks.STONE);
        }
        data.check(level, 0);
        h.assertTrue(data.open(key(h)).isEmpty(), "nobody came to a filled-in post");
        h.assertTrue(!h.getLevel().getEntitiesOfClass(Townsfolk.class,
                new AABB(h.absolutePos(POST)).inflate(4.0, 24.0, 4.0), Townsfolk::isAlive).isEmpty(), "arrived nowhere near the post");
        data.forget(key(h));
        h.succeed();
    }

    static void vacanciesSurviveSave(GameTestHelper h) {
        ServerLevel level = h.getLevel();
        TownVacancies data = settle(h);
        folk(h, POST, EntitySpawnReason.STRUCTURE).kill(level);
        RegistryOps<Tag> ops = level.registryAccess().createSerializationContext(NbtOps.INSTANCE);
        Tag saved = TownVacancies.CODEC.encodeStart(ops, data).getOrThrow();
        TownVacancies loaded = TownVacancies.CODEC.parse(ops, saved).getOrThrow();
        h.assertTrue(data.open(key(h)).size() == 1 && loaded.open(key(h)).equals(data.open(key(h))), "places changed in the save: " + loaded.open(key(h)));
        data.forget(key(h));
        h.succeed();
    }
}
