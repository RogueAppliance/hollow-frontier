package com.hollowfrontier.gametest;

import java.util.List;
import java.util.UUID;

import com.hollowfrontier.content.frontier.bank.BankBox;
import com.hollowfrontier.content.frontier.bank.BankNotice;
import com.hollowfrontier.content.frontier.bank.TownBanks;
import com.hollowfrontier.content.frontier.gear.DynamiteEntity;
import com.hollowfrontier.content.frontier.law.Wanted;
import com.hollowfrontier.content.frontier.outlaw.AbstractOutlaw;
import com.hollowfrontier.content.frontier.outlaw.Outlaw;
import com.hollowfrontier.content.frontier.outlaw.OutlawBoss;
import com.hollowfrontier.content.frontier.raid.RaidConfig;
import com.hollowfrontier.content.frontier.raid.RaidGoals;
import com.hollowfrontier.content.frontier.raid.RaidMember;
import com.hollowfrontier.content.frontier.raid.RaidTimings;
import com.hollowfrontier.content.frontier.raid.TownRaid;
import com.hollowfrontier.content.frontier.raid.TownRaids;
import com.hollowfrontier.content.frontier.raid.TownRecord;
import com.hollowfrontier.content.frontier.raid.TownSite;
import com.hollowfrontier.content.frontier.talk.Chatter;
import com.hollowfrontier.content.frontier.talk.ChatterLines;
import com.hollowfrontier.content.frontier.talk.Context;
import com.hollowfrontier.content.frontier.talk.Fact;
import com.hollowfrontier.content.frontier.talk.Moment;
import com.hollowfrontier.content.frontier.townsfolk.Lawman;
import com.hollowfrontier.content.frontier.townsfolk.Townsfolk;
import com.hollowfrontier.registry.ModAttachments;
import com.hollowfrontier.registry.ModDataComponents;
import com.hollowfrontier.registry.ModEntities;
import com.hollowfrontier.registry.ModItems;

import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.NbtOps;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.RegistryOps;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.Difficulty;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.ChestBlockEntity;
import net.minecraft.world.level.levelgen.structure.BoundingBox;

/**
 * Town raids, on the test_raid_town structure (TestBlueprints). Raiders are mostly frozen (NoAI) once they ride in so
 * they can't wander into neighbouring tests; the director (TownRaid) works off where they stand, not their AI. Every
 * test calls the raid off at the end so nothing lingers.
 */
final class RaidTests {
    private RaidTests() {}

    // Street points in the test town
    private static final BlockPos RALLY = new BlockPos(12, 1, 6);
    private static final BlockPos EDGE = new BlockPos(20, 1, 6);
    private static final BlockPos APPROACH = new BlockPos(22, 1, 6);
    private static final BlockPos RIDE_IN = new BlockPos(20, 1, 5);
    private static final BlockPos VAULT_CHEST = new BlockPos(17, 1, 20);
    private static final BlockPos WORK_SPOT = new BlockPos(17, 1, 17);
    private static final BlockPos BOARD = new BlockPos(10, 1, 2);
    private static final RaidTimings QUICK = new RaidTimings(10, 40, 2000, 20);

    private static TownSite site(GameTestHelper h) {
        ServerLevel level = h.getLevel();
        BoundingBox bounds = BoundingBox.fromCorners(h.absolutePos(BlockPos.ZERO), h.absolutePos(new BlockPos(23, 7, 23)));
        return TownSite.scan(level, h.absolutePos(BlockPos.ZERO).asLong(), bounds, h.absolutePos(RALLY), h.absolutePos(EDGE),
                h.absolutePos(APPROACH), 9);
    }

    private static TownRaid start(GameTestHelper h, int gang, RaidTimings timings) {
        h.getLevel().getServer().setDifficulty(Difficulty.NORMAL, true);
        return TownRaids.get(h.getLevel()).start(h.getLevel(), site(h), timings, gang, false).orElseThrow();
    }

    // Skips the warning: the gang appears in the street, frozen.
    private static List<AbstractOutlaw> rideIn(GameTestHelper h, TownRaid raid) {
        raid.rideIn(h.getLevel(), TownRaids.get(h.getLevel()), h.absolutePos(RIDE_IN));
        List<AbstractOutlaw> gang = gang(h, raid);
        gang.forEach(o -> o.setNoAi(true));
        return gang;
    }

    private static List<AbstractOutlaw> gang(GameTestHelper h, TownRaid raid) {
        return raid.members().stream().map(id -> h.getLevel().getEntity(id)).filter(e -> e instanceof AbstractOutlaw)
                .map(e -> (AbstractOutlaw) e).toList();
    }

    private static void callOff(GameTestHelper h, TownRaid raid) {
        TownRaids.get(h.getLevel()).stop(h.getLevel(), raid.site().key());
    }

    private static ChestBlockEntity vaultWith(GameTestHelper h, ItemStack... stacks) {
        ChestBlockEntity chest = h.getBlockEntity(VAULT_CHEST, ChestBlockEntity.class);
        chest.unpackLootTable(null);
        chest.clearContent();
        for (int i = 0; i < stacks.length; i++) {
            chest.setItem(i, stacks[i]);
        }
        return chest;
    }

    private static void kill(GameTestHelper h, Entity victim, ServerPlayer by) {
        victim.hurtServer(h.getLevel(), h.getLevel().damageSources().playerAttack(by), 10000.0F);
    }

    /** The site reads the bank vault (door, work spot, chest) and bell off the test town. */
    static void siteFindsVault(GameTestHelper h) {
        TownSite site = site(h);
        h.assertTrue(site.vault().equals(List.of(h.absolutePos(VAULT_CHEST))), "vault chests: " + site.vault());
        h.assertTrue(site.crackAt().orElseThrow().equals(h.absolutePos(new BlockPos(17, 1, 18))), "vault door: " + site.crackAt());
        h.assertTrue(site.workAt().orElseThrow().equals(h.absolutePos(WORK_SPOT)), "work spot (should be the lobby side): " + site.workAt());
        h.assertTrue(site.bell().orElseThrow().equals(h.absolutePos(new BlockPos(2, 1, 2))), "bell: " + site.bell());
        h.succeed();
    }

    /** A raid starts with a warning, then a named leader and the rest of the gang ride in, every one marked as a raider. */
    static void raidSpawnsLeaderAndGang(GameTestHelper h) {
        TestPlayers.survival(h, new BlockPos(6, 1, 4));
        TownRaid raid = start(h, 5, QUICK);
        h.assertTrue(raid.phase() == TownRaid.Phase.WARNING && raid.members().isEmpty(), "gang arrived before the warning");
        List<AbstractOutlaw> gang = rideIn(h, raid);
        try {
            h.assertTrue(raid.phase() == TownRaid.Phase.ASSAULT, "not riding in: " + raid.phase());
            h.assertTrue(gang.size() == 5 && raid.gangSize() == 5, "gang of " + gang.size());
            OutlawBoss leader = (OutlawBoss) h.getLevel().getEntity(raid.leaderId());
            h.assertTrue(leader != null && leader.bounty().isPresent() && leader.bounty().get().target().equals(raid.leaderName()),
                    "leader isn't the named boss");
            h.assertTrue(gang.stream().allMatch(o -> RaidMember.of(o) != null && RaidMember.of(o).raid().equals(raid.id())), "unmarked gang member");
            h.assertTrue(gang.stream().filter(o -> RaidMember.is(o, RaidMember.Role.SAFECRACKER)).count() == 1, "no safecracker (the town has a vault)");
            AbstractOutlaw dynamiter = gang.stream().filter(o -> RaidMember.is(o, RaidMember.Role.DYNAMITER)).findFirst().orElse(null);
            h.assertTrue(dynamiter != null && dynamiter.getMainHandItem().is(ModItems.DYNAMITE.get()), "no dynamiter with his sticks");
        } finally {
            callOff(h, raid);
        }
        h.assertTrue(gang.stream().noneMatch(Entity::isAlive), "calling it off left raiders behind");
        h.succeed();
    }

    /** Only one raid per town at a time. */
    static void oneRaidPerTown(GameTestHelper h) {
        TownRaid raid = start(h, 4, QUICK);
        boolean second = TownRaids.get(h.getLevel()).start(h.getLevel(), site(h), QUICK, 4, false).isPresent();
        callOff(h, raid);
        h.assertFalse(second, "a second raid started in the same town");
        h.succeed();
    }

    /** Townsfolk caught in the street go indoors through the nearest door during the warning. */
    static void townsfolkShelterIndoors(GameTestHelper h) {
        TestPlayers.survival(h, new BlockPos(11, 1, 2));
        Townsfolk folk = ModEntities.TOWNSFOLK.get().spawn(h.getLevel(), h.absolutePos(new BlockPos(5, 1, 7)), EntitySpawnReason.SPAWN_ITEM_USE);
        h.assertFalse(RaidGoals.Shelter.sheltered(h.getLevel(), folk.blockPosition()), "townsfolk start out indoors");
        TownRaid raid = start(h, 4, new RaidTimings(4000, 40, 4000, 20));
        h.succeedWhen(() -> {
            h.assertTrue(RaidGoals.Shelter.sheltered(h.getLevel(), folk.blockPosition()), "townsfolk still outside at " + folk.blockPosition());
            callOff(h, raid);
        });
    }

    /** Lawmen go after raiders. */
    static void lawmenEngageRaiders(GameTestHelper h) {
        TestPlayers.survival(h, new BlockPos(2, 1, 9));
        Lawman lawman = ModEntities.LAWMAN.get().spawn(h.getLevel(), h.absolutePos(new BlockPos(8, 1, 5)), EntitySpawnReason.SPAWN_ITEM_USE);
        TownRaid raid = start(h, 3, QUICK);
        List<AbstractOutlaw> gang = rideIn(h, raid);
        h.succeedWhen(() -> {
            h.assertTrue(gang.stream().anyMatch(o -> !o.isAlive() || o.getHealth() < o.getMaxHealth()),
                    "no raider hit yet (lawman's target " + lawman.getTarget() + ")");
            callOff(h, raid);
        });
    }

    /** The safecracker at the vault works it open: the iron door opens and the gang takes its share of the chest. */
    static void vaultTimerRobsChest(GameTestHelper h) {
        TestPlayers.survival(h, new BlockPos(6, 1, 4));
        ChestBlockEntity chest = vaultWith(h, new ItemStack(ModItems.GOLD_COIN.get(), 10));
        TownRaid raid = start(h, 3, QUICK);
        List<AbstractOutlaw> gang = rideIn(h, raid);
        Entity cracker = h.getLevel().getEntity(raid.safecrackerId());
        BlockPos work = h.absolutePos(WORK_SPOT);
        cracker.snapTo(work.getX() + 0.5, work.getY(), work.getZ() + 0.5, 0, 0);
        h.succeedWhen(() -> {
            h.assertTrue(raid.cracked(), "vault not cracked yet: " + raid.vaultProgress() + "/" + QUICK.vault());
            int left = chest.getItem(0).getCount();
            int share = (int) Math.ceil(10 * RaidConfig.stolenShare());
            h.assertTrue(left == 10 - share, "chest still holds " + left);
            h.assertTrue(raid.loot().stream().mapToInt(ItemStack::getCount).sum() == share, "loot: " + raid.loot());
            h.assertTrue(raid.outcome() == TownRaid.Outcome.ROBBED && raid.phase() == TownRaid.Phase.RETREAT, "outcome " + raid.outcome());
            h.assertTrue(h.getBlockState(new BlockPos(17, 1, 18)).getValue(net.minecraft.world.level.block.DoorBlock.OPEN), "vault door still shut");
            h.assertTrue(((AbstractOutlaw) cracker).getMainHandItem().is(net.minecraft.world.item.Items.BUNDLE), "the safecracker isn't carrying the loot");
            callOff(h, raid);
        });
    }

    /** Killing the safecracker mid-job starts the job over; the vault keeps its contents. */
    static void killingSafecrackerInterrupts(GameTestHelper h) {
        ServerPlayer player = TestPlayers.survival(h, new BlockPos(6, 1, 4));
        ChestBlockEntity chest = vaultWith(h, new ItemStack(ModItems.GOLD_COIN.get(), 10));
        TownRaid raid = start(h, 4, new RaidTimings(10, 400, 2000, 20));
        rideIn(h, raid);
        Entity cracker = h.getLevel().getEntity(raid.safecrackerId());
        BlockPos work = h.absolutePos(WORK_SPOT);
        cracker.snapTo(work.getX() + 0.5, work.getY(), work.getZ() + 0.5, 0, 0);
        h.runAfterDelay(30, () -> {
            h.assertTrue(raid.vaultProgress() > 0, "safecracker never got to work");
            kill(h, cracker, player);
            try {
                h.assertTrue(raid.vaultProgress() == 0 && raid.safecrackerId() == null, "job not interrupted: " + raid.vaultProgress());
                h.assertFalse(raid.cracked(), "vault cracked anyway");
                h.assertTrue(chest.getItem(0).getCount() == 10, "vault lost coins");
                h.assertTrue(raid.contribution(player.getUUID()).safecracker(), "no credit for stopping the safecracker");
            } finally {
                callOff(h, raid);
            }
            h.succeed();
        });
    }

    /** Drop the leader and the town is saved: the defender is paid and the law forgets a two-star past. */
    static void defendedRaidPaysAndPardons(GameTestHelper h) {
        ServerPlayer player = TestPlayers.survival(h, new BlockPos(6, 1, 4));
        Wanted.addHeat(player, 6);
        h.assertTrue(Wanted.stars(player) == 2, "setup: not two stars");
        TownRaid raid = start(h, 4, QUICK);
        rideIn(h, raid);
        AbstractOutlaw grunt = gang(h, raid).stream().filter(o -> !(o instanceof OutlawBoss)).findFirst().orElseThrow();
        kill(h, grunt, player);
        kill(h, h.getLevel().getEntity(raid.leaderId()), player);
        try {
            h.assertTrue(raid.outcome() == TownRaid.Outcome.DEFENDED, "outcome " + raid.outcome());
            int purse = player.getInventory().countItem(ModItems.COPPER_COIN.get()) + 9 * player.getInventory().countItem(ModItems.GOLD_COIN.get());
            int expected = RaidConfig.payTakingPart() + 2 * RaidConfig.payKill() + RaidConfig.payLeader();
            h.assertTrue(purse >= expected, "paid " + purse + " copper, expected at least " + expected);
            h.assertTrue(Wanted.stars(player) == 0, "still wanted: " + Wanted.of(player));
            h.assertTrue(AdvancementTests.earned(player, "frontier/hold_the_line"), "defending the town earned nothing");
            h.assertTrue(player.getInventory().contains(s -> s.is(ModItems.OUTLAW_BADGE.get()))
                    || !h.getLevel().getEntitiesOfClass(net.minecraft.world.entity.item.ItemEntity.class, raid.watchArea(),
                            e -> e.getItem().is(ModItems.OUTLAW_BADGE.get())).isEmpty(), "the leader dropped no badge");
        } finally {
            callOff(h, raid);
        }
        h.succeed();
    }

    /** Shoot the bagman before he gets out of town: the money goes back, and that's Not So Fast, Partner. */
    static void recoveredLootEarnsAdvancement(GameTestHelper h) {
        ServerPlayer player = TestPlayers.survival(h, new BlockPos(6, 1, 4));
        vaultWith(h, new ItemStack(ModItems.GOLD_COIN.get(), 10));
        TownRaid raid = start(h, 3, QUICK);
        rideIn(h, raid);
        Entity cracker = h.getLevel().getEntity(raid.safecrackerId());
        BlockPos work = h.absolutePos(WORK_SPOT);
        cracker.snapTo(work.getX() + 0.5, work.getY(), work.getZ() + 0.5, 0, 0);
        boolean[] shot = new boolean[1];
        h.succeedWhen(() -> {
            if (!shot[0]) {
                h.assertTrue(raid.outcome() == TownRaid.Outcome.ROBBED && raid.phase() == TownRaid.Phase.RETREAT, "not robbed yet");
                kill(h, h.getLevel().getEntity(raid.safecrackerId()), player);
                shot[0] = true;
            }
            try {
                h.assertTrue(raid.outcome() == TownRaid.Outcome.RECOVERED, "outcome " + raid.outcome());
                h.assertTrue(AdvancementTests.earned(player, "frontier/not_so_fast"), "recovering the loot earned nothing");
                h.assertTrue(AdvancementTests.earned(player, "frontier/hold_the_line"), "a recovered raid counts as defended");
            } finally {
                callOff(h, raid);
            }
        });
    }

    /**
     * Cracking the vault takes a share of every safe deposit box in town, an absent owner's too; shoot the bagman and
     * every coin goes back to its box (Saved the Savings for a defender whose own box was hit), and both owners hear so.
     */
    static void bagmanReturnsBoxCoins(GameTestHelper h) {
        ServerPlayer player = TestPlayers.survival(h, new BlockPos(6, 1, 4));
        vaultWith(h, new ItemStack(ModItems.GOLD_COIN.get(), 4));
        long town = site(h).key();
        TownBanks bank = TownBanks.get(h.getLevel());
        UUID away = UUID.randomUUID();
        BankBox mine = bank.open(town, player.getUUID(), 0);
        mine.setItem(0, new ItemStack(ModItems.GOLD_COIN.get(), 8)); // 72 c
        BankBox theirs = bank.open(town, away, 0);
        theirs.setItem(5, new ItemStack(ModItems.COPPER_COIN.get(), 40));
        int takeMine = (int) Math.floor(72 * RaidConfig.depositsAtRisk());
        int takeTheirs = (int) Math.floor(40 * RaidConfig.depositsAtRisk());
        TownRaid raid = start(h, 3, QUICK);
        rideIn(h, raid);
        Entity cracker = h.getLevel().getEntity(raid.safecrackerId());
        BlockPos work = h.absolutePos(WORK_SPOT);
        cracker.snapTo(work.getX() + 0.5, work.getY(), work.getZ() + 0.5, 0, 0);
        boolean[] shot = new boolean[1];
        h.succeedWhen(() -> {
            if (!shot[0]) {
                h.assertTrue(raid.outcome() == TownRaid.Outcome.ROBBED, "not robbed yet");
                h.assertTrue(mine.worth() == 72 - takeMine && theirs.worth() == 40 - takeTheirs,
                        "boxes after the crack: " + mine.worth() + ", " + theirs.worth() + " (took " + raid.boxTake() + ")");
                kill(h, h.getLevel().getEntity(raid.safecrackerId()), player);
                shot[0] = true;
            }
            h.assertTrue(raid.outcome() == TownRaid.Outcome.RECOVERED, "outcome " + raid.outcome());
            h.assertTrue(mine.worth() == 72 && theirs.worth() == 40, "coins not back: " + mine.worth() + ", " + theirs.worth());
            h.assertTrue(AdvancementTests.earned(player, "frontier/saved_the_savings"), "saving your own savings earned nothing");
            // The owner who was away hears it once the raid is over (the one who was there was told on the spot).
            h.assertTrue(TownRaids.get(h.getLevel()).raidIn(town) == null, "raid not over yet: " + raid.phase());
            List<BankNotice> news = bank.collect(away);
            h.assertTrue(news.size() == 1 && news.getFirst().returned() && news.getFirst().copper() == takeTheirs, "news: " + news);
            h.assertTrue(bank.collect(player.getUUID()).isEmpty(), "news queued for an owner who was online");
        });
    }

    /**
     * A bagman who gets away keeps the boxes' share: the absent owner hears what was lost on their next visit, and
     * the Gazette prints what the bank lost.
     */
    static void robbedBankTellsOwners(GameTestHelper h) {
        ServerPlayer player = TestPlayers.survival(h, new BlockPos(9, 1, 3));
        vaultWith(h, new ItemStack(ModItems.GOLD_COIN.get(), 4));
        long town = site(h).key();
        UUID away = UUID.randomUUID();
        BankBox theirs = TownBanks.get(h.getLevel()).open(town, away, 0);
        theirs.setItem(0, new ItemStack(ModItems.GOLD_COIN.get(), 16)); // 144 c
        int take = (int) Math.floor(144 * RaidConfig.depositsAtRisk());
        TownRaid raid = start(h, 3, QUICK);
        rideIn(h, raid);
        Entity cracker = h.getLevel().getEntity(raid.safecrackerId());
        BlockPos work = h.absolutePos(WORK_SPOT);
        cracker.snapTo(work.getX() + 0.5, work.getY(), work.getZ() + 0.5, 0, 0);
        h.succeedWhen(() -> {
            h.assertTrue(TownRaids.get(h.getLevel()).raidIn(town) == null, "raid not over yet: " + raid.phase());
            h.assertTrue(theirs.worth() == 144 - take, "box worth " + theirs.worth());
            List<BankNotice> news = TownBanks.get(h.getLevel()).collect(away);
            h.assertTrue(news.size() == 1 && news.getFirst().copper() == take && !news.getFirst().returned(), "news: " + news);
            h.assertTrue(TownRaids.get(h.getLevel()).lastReport().map(r -> r.bankLost() == take).orElse(false),
                    "report: " + TownRaids.get(h.getLevel()).lastReport());
            String paper = com.hollowfrontier.content.frontier.gazette.GazetteItem.print(h.getLevel(), player).pages().toString();
            h.assertTrue(paper.contains("gazette.hollowfrontier.raid.bank_lost"), "the Gazette missed the bank's loss");
        });
    }

    /** 19: while the gang's in town nobody talks of anything else; once they've robbed it, the town talks of the robbery. */
    static void townTalksOfRaidAndRobbery(GameTestHelper h) {
        ServerPlayer player = TestPlayers.survival(h, new BlockPos(9, 1, 3));
        BlockPos street = h.absolutePos(RALLY);
        TownRaid raid = start(h, 3, QUICK);
        try {
            Context during = Context.of(h.getLevel(), street, player);
            h.assertTrue(during.has(Fact.RAID) && !during.has(Fact.ROBBED), "a raid on: " + during.facts());
            List<ChatterLines.Line> greetings = Chatter.candidates(List.of("wrangler", "townsfolk"), Moment.GREET, during);
            h.assertTrue(!greetings.isEmpty() && greetings.stream().allMatch(l -> l.cond().all().contains(Fact.RAID)), "small talk in a raid: " + greetings);
        } finally {
            callOff(h, raid);
        }
        TownRecord record = TownRaids.get(h.getLevel()).record(raid.site().key(), h.getLevel().getGameTime());
        record.robbedUntil = h.getLevel().getGameTime() + 24000;
        Context after = Context.of(h.getLevel(), street, player);
        h.assertTrue(after.has(Fact.ROBBED) && !after.has(Fact.RAID), "robbed, and the gang gone: " + after.facts());
        h.assertTrue(Chatter.candidates(List.of("banker", "townsfolk"), Moment.TALK, after).stream()
                .anyMatch(l -> l.key().equals("message.hollowfrontier.banker.robbed")), "the banker won't talk of the robbery");
        record.robbedUntil = 0;
        TestPlayers.leave(player);
        h.succeed();
    }

    /** A three-star outlaw who keeps out of it gets a cut of the robbery, and Honor Among Thieves. */
    static void outlawCutEarnsAdvancement(GameTestHelper h) {
        ServerPlayer player = TestPlayers.survival(h, new BlockPos(9, 1, 3));
        Wanted.addHeat(player, 8);
        vaultWith(h, new ItemStack(ModItems.GOLD_COIN.get(), 4));
        TownRaid raid = start(h, 3, QUICK);
        rideIn(h, raid);
        Entity cracker = h.getLevel().getEntity(raid.safecrackerId());
        BlockPos work = h.absolutePos(WORK_SPOT);
        cracker.snapTo(work.getX() + 0.5, work.getY(), work.getZ() + 0.5, 0, 0);
        h.succeedWhen(() -> {
            h.assertTrue(raid.outcome() == TownRaid.Outcome.ROBBED, "not robbed yet");
            try {
                h.assertTrue(AdvancementTests.earned(player, "frontier/honor_among_thieves"), "the outlaw's cut earned nothing");
                h.assertFalse(AdvancementTests.earned(player, "frontier/hold_the_line"), "an outlaw was thanked for defending the town");
            } finally {
                callOff(h, raid);
            }
        });
    }

    /** A robbed town is marked robbed and its Bounty Board hands out a poster for the leader who got away. */
    static void robbedRaidPostsPoster(GameTestHelper h) {
        ServerPlayer player = TestPlayers.survival(h, new BlockPos(9, 1, 3));
        vaultWith(h, new ItemStack(ModItems.GOLD_COIN.get(), 4));
        TownRaid raid = start(h, 3, QUICK);
        rideIn(h, raid);
        Entity cracker = h.getLevel().getEntity(raid.safecrackerId());
        BlockPos work = h.absolutePos(WORK_SPOT);
        cracker.snapTo(work.getX() + 0.5, work.getY(), work.getZ() + 0.5, 0, 0);
        String leader = raid.leaderName();
        h.succeedWhen(() -> {
            h.assertTrue(TownRaids.get(h.getLevel()).raidIn(raid.site().key()) == null, "raid not over yet: " + raid.phase());
            h.assertTrue(TownRaids.isRobbed(h.getLevel(), h.absolutePos(RALLY)), "town not marked robbed");
            h.useBlock(BOARD, player);
            ItemStack poster = ItemStack.EMPTY;
            for (int i = 0; i < player.getInventory().getContainerSize(); i++) {
                if (player.getInventory().getItem(i).is(ModItems.WANTED_POSTER.get())) {
                    poster = player.getInventory().getItem(i);
                }
            }
            h.assertTrue(poster.has(ModDataComponents.BOUNTY.get()) && poster.get(ModDataComponents.BOUNTY.get()).target().equals(leader),
                    "no poster for " + leader + ": " + poster);
            // And the Gazette carries the story: the world's latest raid, which may be another test's robbery that ended since
            // (robbed_bank_tells_owners runs alongside), so it's checked against the report rather than this raid's leader.
            var report = TownRaids.get(h.getLevel()).lastReport().orElseThrow();
            String paper = com.hollowfrontier.content.frontier.gazette.GazetteItem.print(h.getLevel(), player).pages().toString();
            h.assertTrue(paper.contains("gazette.hollowfrontier.raid." + report.story()) && paper.contains(report.leader()),
                    "the Gazette missed the raid report " + report);
        });
    }

    /** Raider dynamite hurts lawmen but leaves blocks and townsfolk alone. */
    static void raiderDynamiteSparesTown(GameTestHelper h) {
        h.getLevel().getServer().setDifficulty(Difficulty.NORMAL, true);
        BlockPos blast = new BlockPos(12, 1, 8);
        BlockPos[] around = {blast.east(), blast.west(), blast.north(), blast.below(), blast.above().above()};
        Block[] blocks = {Blocks.OAK_PLANKS, Blocks.GLASS, Blocks.SPRUCE_FENCE, Blocks.STONE, Blocks.OAK_PLANKS};
        for (int i = 0; i < around.length; i++) {
            h.setBlock(around[i], blocks[i]);
        }
        Outlaw thrower = h.spawnWithNoFreeWill(ModEntities.OUTLAW.get(), new BlockPos(12, 1, 2));
        Townsfolk folk = h.spawnWithNoFreeWill(ModEntities.TOWNSFOLK.get(), new BlockPos(13, 1, 9));
        Lawman lawman = h.spawnWithNoFreeWill(ModEntities.LAWMAN.get(), new BlockPos(11, 1, 9));
        DynamiteEntity stick = new DynamiteEntity(h.getLevel(), thrower, new ItemStack(ModItems.DYNAMITE.get()));
        stick.setRaider(true);
        BlockPos at = h.absolutePos(blast);
        stick.setPos(at.getX() + 0.5, at.getY() + 0.1, at.getZ() + 0.5);
        stick.setDeltaMovement(0, 0, 0);
        h.getLevel().addFreshEntity(stick);
        h.runAfterDelay(DynamiteEntity.FUSE_TICKS + 10, () -> {
            h.assertFalse(stick.isAlive(), "dynamite didn't go off");
            for (int i = 0; i < around.length; i++) {
                h.assertTrue(h.getBlockState(around[i]).is(blocks[i]), "raider dynamite broke " + blocks[i] + " at " + around[i]);
            }
            h.assertTrue(folk.getHealth() == folk.getMaxHealth(), "raider dynamite hurt a townsfolk");
            h.assertTrue(lawman.getHealth() < lawman.getMaxHealth(), "raider dynamite didn't touch the lawman");
            h.succeed();
        });
    }

    /** While a raid is on, hurting townsfolk or a lawman costs no heat; killing one still does. */
    static void raidForgivesStrayShots(GameTestHelper h) {
        ServerLevel level = h.getLevel();
        ServerPlayer player = TestPlayers.survival(h, new BlockPos(6, 1, 4));
        TownRaid raid = start(h, 4, QUICK);
        Townsfolk folk = h.spawnWithNoFreeWill(ModEntities.TOWNSFOLK.get(), new BlockPos(13, 1, 9));
        Lawman lawman = h.spawnWithNoFreeWill(ModEntities.LAWMAN.get(), new BlockPos(11, 1, 9));
        folk.hurtServer(level, level.damageSources().playerAttack(player), 1.0F);
        lawman.hurtServer(level, level.damageSources().playerAttack(player), 1.0F);
        h.assertValueEqual(Wanted.of(player).heat(), 0, "heat for stray hits in a raid");
        Lawman other = h.spawnWithNoFreeWill(ModEntities.LAWMAN.get(), new BlockPos(9, 1, 9));
        other.hurtServer(level, level.damageSources().playerAttack(player), 1000.0F);
        h.assertValueEqual(Wanted.of(player).heat(), Wanted.KILL_LAWMAN, "heat for killing a lawman in a raid");
        callOff(h, raid);
        h.succeed();
    }

    /** Raid state goes through the save codec and back intact (what a server restart does). */
    static void raidStateSurvivesSave(GameTestHelper h) {
        ServerLevel level = h.getLevel();
        TestPlayers.survival(h, new BlockPos(6, 1, 4));
        TownRaid raid = start(h, 4, QUICK);
        rideIn(h, raid);
        TownRaids data = TownRaids.get(level);
        TownRecord record = data.record(raid.site().key(), level.getGameTime());
        record.robbedUntil = 12345L;
        RegistryOps<Tag> ops = level.registryAccess().createSerializationContext(NbtOps.INSTANCE);
        try {
            Tag saved = TownRaids.CODEC.encodeStart(ops, data).getOrThrow();
            TownRaids loaded = TownRaids.CODEC.parse(ops, saved).getOrThrow();
            TownRaid back = loaded.byId(raid.id());
            h.assertTrue(back != null, "raid lost in the save");
            h.assertTrue(back.phase() == raid.phase() && back.leaderName().equals(raid.leaderName())
                    && back.leaderId().equals(raid.leaderId()) && back.members().equals(raid.members())
                    && back.safecrackerId().equals(raid.safecrackerId()) && back.site().equals(raid.site()),
                    "raid changed in the save");
            h.assertTrue(loaded.recordIfKnown(raid.site().key()).map(r -> r.robbedUntil == 12345L && r.bounds.isPresent()).orElse(false),
                    "town record changed in the save");
        } finally {
            callOff(h, raid);
        }
        h.succeed();
    }

    /** A raider whose raid is over (say the server stopped mid-raid and it was called off) is gone when it loads again. */
    static void orphanRaidersRemoved(GameTestHelper h) {
        Outlaw outlaw = ModEntities.OUTLAW.get().create(h.getLevel(), EntitySpawnReason.EVENT);
        BlockPos at = h.absolutePos(RALLY);
        outlaw.snapTo(at.getX() + 0.5, at.getY(), at.getZ() + 0.5, 0, 0);
        outlaw.setData(ModAttachments.RAID_MEMBER.get(), new RaidMember(UUID.randomUUID(), RaidMember.Role.GUNSLINGER, java.util.Optional.empty()));
        boolean added = h.getLevel().addFreshEntity(outlaw);
        h.assertFalse(added, "an orphaned raider joined the world");
        h.succeed();
    }

    /**
     * A three-star outlaw is one of the gang's own: raiders pick the honest player beside them instead. (Their target
     * goals are run by hand, so the frozen gang never goes after anyone in a neighbouring test.)
     */
    static void raidersLeaveOutlawPlayersBe(GameTestHelper h) {
        ServerPlayer outlawPlayer = TestPlayers.survival(h, new BlockPos(18, 1, 5));
        Wanted.addHeat(outlawPlayer, Wanted.MAX_HEAT);
        ServerPlayer honest = TestPlayers.survival(h, new BlockPos(16, 1, 5));
        TownRaid raid = start(h, 3, QUICK);
        List<AbstractOutlaw> gang = rideIn(h, raid);
        try {
            for (int i = 0; i < 200; i++) {
                gang.forEach(o -> o.targetSelector.tick());
            }
            h.assertTrue(gang.stream().noneMatch(o -> o.getTarget() == outlawPlayer), "a raider went for a three-star outlaw");
            h.assertTrue(gang.stream().anyMatch(o -> o.getTarget() == honest), "no raider picked the honest player either (test is broken)");
        } finally {
            callOff(h, raid);
        }
        h.succeed();
    }
}
