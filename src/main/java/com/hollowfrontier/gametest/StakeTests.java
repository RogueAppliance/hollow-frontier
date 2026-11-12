package com.hollowfrontier.gametest;

import java.util.List;
import java.util.Optional;

import com.hollowfrontier.HollowFrontier;
import com.hollowfrontier.content.frontier.bounty.Purse;
import com.hollowfrontier.content.frontier.law.Wanted;
import com.hollowfrontier.content.frontier.outlaw.AbstractOutlaw;
import com.hollowfrontier.content.frontier.stake.Deputies;
import com.hollowfrontier.content.frontier.stake.Drives;
import com.hollowfrontier.content.frontier.stake.Herds;
import com.hollowfrontier.content.frontier.stake.MailboxBlock;
import com.hollowfrontier.content.frontier.stake.Neighbours;
import com.hollowfrontier.content.frontier.stake.OldPlace;
import com.hollowfrontier.content.frontier.stake.SheriffJob;
import com.hollowfrontier.content.frontier.stake.SheriffJobs;
import com.hollowfrontier.content.frontier.stake.StakeData;
import com.hollowfrontier.content.frontier.stake.StakeItems;
import com.hollowfrontier.content.frontier.stake.Standing;
import com.hollowfrontier.content.frontier.stake.TownRef;
import com.hollowfrontier.content.frontier.townsfolk.Lawman;
import com.hollowfrontier.content.hollow.gray.Captives;
import com.hollowfrontier.registry.ModAttachments;
import com.hollowfrontier.registry.ModBlocks;
import com.hollowfrontier.registry.ModDataComponents;
import com.hollowfrontier.registry.ModEntities;
import com.hollowfrontier.registry.ModItems;
import com.hollowfrontier.worldgen.structure.ModChestLoot;
import com.hollowfrontier.worldgen.structure.OldPlaceBlueprint;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.Container;
import net.minecraft.world.Difficulty;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.entity.animal.cow.Cow;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.entity.ChestBlockEntity;
import net.minecraft.world.level.block.entity.SignBlockEntity;
import net.minecraft.world.level.levelgen.structure.templatesystem.JigsawReplacementProcessor;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructurePlaceSettings;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplate;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

// 47, a stake in town: standing, the badge and the sheriff's jobs, the Old Place, the brand, and cattle drives. The test
// world has no towns, so each test names its own (a key nothing else uses).
final class StakeTests {
    private StakeTests() {}

    private static final long TOWN = 470_001L, OTHER = 470_002L;

    private static Lawman sheriff(GameTestHelper h, BlockPos pos) {
        Lawman lawman = h.spawnWithNoFreeWill(ModEntities.LAWMAN.get(), pos);
        lawman.setRank(Lawman.Rank.SHERIFF);
        return lawman;
    }

    /** Points make ranks at 10, 30 and 70, stop at 100, come off for crimes and halve for a killing. */
    static void standingByDeed(GameTestHelper h) {
        ServerPlayer player = TestPlayers.survival(h, new BlockPos(2, 1, 2));
        h.assertTrue(Standing.rank(player, TOWN) == Standing.Rank.STRANGER, "a newcomer isn't a stranger");
        Standing.add(player, TOWN, 9);
        h.assertTrue(Standing.rank(player, TOWN) == Standing.Rank.STRANGER, "9 points made a rank");
        Standing.add(player, TOWN, Standing.BOUNTY);
        h.assertTrue(Standing.rank(player, TOWN) == Standing.Rank.REGULAR, "15 points: " + Standing.rank(player, TOWN));
        h.assertTrue(Standing.rank(player, OTHER) == Standing.Rank.STRANGER, "one town's standing reached another");
        Standing.add(player, TOWN, 15);
        h.assertTrue(Standing.rank(player, TOWN) == Standing.Rank.NEIGHBOUR, "30 points: " + Standing.rank(player, TOWN));
        Standing.add(player, TOWN, 500);
        h.assertTrue(Standing.of(player).points(TOWN) == Standing.MAX && Standing.rank(player, TOWN) == Standing.Rank.PILLAR, "standing isn't capped");
        Standing.halve(player, TOWN);
        h.assertTrue(Standing.of(player).points(TOWN) == Standing.MAX / 2, "a killing didn't halve it");
        Standing.add(player, TOWN, -3 * Standing.PER_HEAT);
        h.assertTrue(Standing.of(player).points(TOWN) == Standing.MAX / 2 - 6, "a crime didn't cost");
        Standing.add(player, TOWN, -500);
        h.assertTrue(Standing.of(player).points(TOWN) == 0, "standing went below nothing");
        h.assertTrue(Standing.town("town:" + TOWN).orElse(-1L) == TOWN && Standing.town("board:x").isEmpty(), "board keys misread");
        h.succeed();
    }

    /** The sheriff swears in a Neighbour and nobody less; two stars take the star, and a clear name gets it back. */
    static void badgeOfferedAndTakenBack(GameTestHelper h) {
        ServerLevel level = h.getLevel();
        ServerPlayer player = TestPlayers.survival(h, new BlockPos(2, 1, 2));
        Lawman sheriff = sheriff(h, new BlockPos(4, 1, 4));
        Standing.add(player, TOWN, Standing.Rank.REGULAR.from);
        h.assertFalse(Deputies.atSheriff(level, player, sheriff, ItemStack.EMPTY, TOWN), "a Regular was sworn in");
        Standing.add(player, TOWN, 30);
        h.assertTrue(Deputies.atSheriff(level, player, sheriff, ItemStack.EMPTY, TOWN), "a Neighbour wasn't sworn in");
        h.assertTrue(Deputies.deputy(player, TOWN) && player.getInventory().countItem(ModItems.DEPUTY_BADGE.get()) == 1, "no star: " + Standing.of(player));
        h.assertTrue(AdvancementTests.earned(player, "frontier/the_star"), "the_star not earned");
        h.assertFalse(Deputies.deputy(player, OTHER), "one town's star is another's");

        Wanted.addHeat(player, 5);
        h.assertTrue(Wanted.stars(player) == 2 && Standing.of(player).suspended() && player.getInventory().countItem(ModItems.DEPUTY_BADGE.get()) == 0
                && !Deputies.deputy(player, TOWN), "two stars didn't take the star");
        Wanted.clear(player);
        h.assertTrue(Deputies.atSheriff(level, player, sheriff, ItemStack.EMPTY, TOWN) && Deputies.deputy(player, TOWN), "the star wasn't given back");
        h.succeed();
    }

    /** The rounds: each stop walked in order, then the sheriff pays, and has nothing more that day. */
    static void roundsWalkedAndPaid(GameTestHelper h) {
        ServerLevel level = h.getLevel();
        ServerPlayer player = TestPlayers.survival(h, new BlockPos(2, 1, 2));
        Lawman sheriff = sheriff(h, new BlockPos(4, 1, 4));
        Standing.add(player, TOWN, 30);
        Deputies.swearIn(level, player, TOWN);
        BlockPos a = h.absolutePos(new BlockPos(2, 1, 8)), b = h.absolutePos(new BlockPos(9, 1, 9));
        SheriffJobs.giveRounds(player, TOWN, List.of(a, b));
        SheriffJobs.scouts = false; // nobody casing the bank from the next test's platform
        SheriffJobs.tick(level, player);
        h.assertTrue(SheriffJobs.of(player).progress() == 0, "a stop counted from across the yard");
        player.snapTo(b.getX() + 0.5, b.getY(), b.getZ() + 0.5, 0, 0);
        SheriffJobs.tick(level, player);
        h.assertTrue(SheriffJobs.of(player).progress() == 0, "the second stop counted before the first");
        player.snapTo(a.getX() + 0.5, a.getY(), a.getZ() + 0.5, 0, 0);
        SheriffJobs.tick(level, player);
        player.snapTo(b.getX() + 0.5, b.getY(), b.getZ() + 0.5, 0, 0);
        SheriffJobs.tick(level, player);
        SheriffJobs.scouts = true;
        h.assertTrue(SheriffJobs.of(player).walked(), "rounds not walked: " + SheriffJobs.of(player));
        int before = Standing.of(player).points(TOWN);
        Deputies.atSheriff(level, player, sheriff, ItemStack.EMPTY, TOWN);
        h.assertTrue(SheriffJobs.of(player) == null && Purse.count(player) == SheriffJobs.ROUNDS_PAY, "rounds not paid: " + Purse.count(player));
        h.assertTrue(Standing.of(player).points(TOWN) == before + Standing.JOB, "a job didn't raise standing");
        h.assertTrue(AdvancementTests.earned(player, "frontier/an_honest_days_work"), "an_honest_days_work not earned");
        Deputies.atSheriff(level, player, sheriff, ItemStack.EMPTY, TOWN);
        h.assertTrue(SheriffJobs.of(player) == null, "a second job the same day");
        h.succeed();
    }

    /** Bring one in: the drifter turns up as the deputy nears, won't be roped till he's hurt, then walks in and is paid for. */
    static void drifterBroughtInAlive(GameTestHelper h) {
        ServerLevel level = h.getLevel();
        level.getServer().setDifficulty(Difficulty.NORMAL, true);
        ServerPlayer player = TestPlayers.survival(h, new BlockPos(2, 1, 2));
        Standing.add(player, TOWN, 30);
        Deputies.swearIn(level, player, TOWN);
        BlockPos spot = h.absolutePos(new BlockPos(8, 1, 8));
        SheriffJobs.giveBringIn(player, TOWN, spot);
        SheriffJobs.tick(level, player);
        SheriffJob job = SheriffJobs.of(player);
        h.assertTrue(job.quarry().isPresent(), "the drifter didn't turn up");
        AbstractOutlaw drifter = (AbstractOutlaw) level.getEntity(job.quarry().get());
        try {
            drifter.setNoAi(true);
            h.assertTrue(drifter.hasCustomName() && SheriffJobs.quarryOf(drifter).orElseThrow().equals(player.getUUID()), "the drifter isn't this deputy's");
            h.assertTrue(SheriffJobs.rope(level, player, drifter) && !drifter.isJailed(), "an unhurt drifter gave up");
            drifter.setHealth(drifter.getMaxHealth() / 2.0F);
            h.assertTrue(SheriffJobs.rope(level, player, drifter) && drifter.isJailed() && Captives.isPrisonerOf(drifter, player), "a hurt drifter didn't give up");
            h.assertTrue(drifter.getMainHandItem().isEmpty(), "he kept his gun");
            int before = Standing.of(player).points(TOWN);
            h.assertTrue(Captives.turnIn(level, player, drifter.blockPosition(), "town:" + TOWN) == 1, "the drifter wasn't turned in");
            h.assertTrue(SheriffJobs.of(player) == null, "the job is still open");
            h.assertTrue(Purse.count(player) == SheriffJobs.DRIFTER_REWARD * Purse.GOLD + SheriffJobs.BRING_IN_PAY, "wrong pay: " + Purse.count(player));
            h.assertTrue(Standing.of(player).points(TOWN) == before + Standing.CAPTIVE + Standing.JOB, "wrong standing for bringing him in");
            h.assertFalse(AdvancementTests.earned(player, "frontier/or_alive"), "a drifter counted as a man from a Gray pen");
        } finally {
            drifter.discard();
        }
        h.succeed();
    }

    /** A dead drifter ends the job unpaid. */
    static void deadDrifterPaysNothing(GameTestHelper h) {
        ServerLevel level = h.getLevel();
        level.getServer().setDifficulty(Difficulty.NORMAL, true);
        ServerPlayer player = TestPlayers.survival(h, new BlockPos(2, 1, 2));
        Standing.add(player, TOWN, 30);
        Deputies.swearIn(level, player, TOWN);
        SheriffJobs.giveBringIn(player, TOWN, h.absolutePos(new BlockPos(8, 1, 8)));
        SheriffJobs.tick(level, player);
        AbstractOutlaw drifter = (AbstractOutlaw) level.getEntity(SheriffJobs.of(player).quarry().orElseThrow());
        drifter.hurtServer(level, level.damageSources().playerAttack(player), 1000.0F);
        h.assertTrue(SheriffJobs.of(player) == null && Purse.count(player) == 0, "a dead drifter was paid for");
        h.succeed();
    }

    /** A warrant is served on the sheriff it's for, not its own, and pays; standing rises in both towns. */
    static void warrantServed(GameTestHelper h) {
        ServerLevel level = h.getLevel();
        ServerPlayer player = TestPlayers.survival(h, new BlockPos(2, 1, 2));
        Lawman sheriff = sheriff(h, new BlockPos(4, 1, 4));
        Standing.add(player, TOWN, 30);
        Deputies.swearIn(level, player, TOWN);
        SheriffJobs.giveWarrant(level, player, TOWN, new Neighbours.Neighbour(OTHER, "Elsewhere", sheriff.blockPosition()));
        ItemStack warrant = ItemStack.EMPTY;
        for (ItemStack stack : player.getInventory()) {
            if (stack.is(ModItems.WARRANT.get())) {
                warrant = stack;
            }
        }
        h.assertTrue(warrant.is(ModItems.WARRANT.get()) && warrant.get(ModDataComponents.TOWN.get()).key() == OTHER, "no warrant in hand");
        Deputies.atSheriff(level, player, sheriff, warrant, TOWN);
        h.assertTrue(!warrant.isEmpty() && SheriffJobs.of(player) != null, "a warrant was served on the sheriff who wrote it");
        int before = Standing.of(player).points(TOWN);
        Deputies.atSheriff(level, player, sheriff, warrant, OTHER);
        h.assertTrue(warrant.isEmpty() && SheriffJobs.of(player) == null, "the warrant wasn't served");
        h.assertTrue(Purse.count(player) >= SheriffJobs.WARRANT_BASE, "no pay for the ride: " + Purse.count(player));
        h.assertTrue(Standing.of(player).points(OTHER) == Standing.WARRANT_SERVED && Standing.of(player).points(TOWN) == before + Standing.JOB,
                "wrong standing for a warrant: " + Standing.of(player));
        h.succeed();
    }

    /**
     * The Old Place stands as drawn (the stake, the mailbox, the board, every chest with its table, the stock), goes into a
     * player's name, has its board repainted, gets the morning paper once a day, and keeps its mailbox for its owner.
     */
    static void oldPlaceStandsAndIsTaken(GameTestHelper h) {
        ServerLevel level = h.getLevel();
        StructureTemplate place = level.getStructureTemplateManager().get(HollowFrontier.id("town/" + OldPlaceBlueprint.NAME)).orElseThrow();
        h.assertTrue(place.getSize().getX() == OldPlaceBlueprint.WIDTH && place.getSize().getZ() == OldPlaceBlueprint.DEPTH, "the lot isn't the size OldPlace thinks: " + place.getSize());
        BlockPos origin = h.absolutePos(new BlockPos(0, 120, 0));
        StructurePlaceSettings settings = new StructurePlaceSettings().addProcessor(JigsawReplacementProcessor.INSTANCE).setFinalizeEntities(true);
        place.placeInWorld(level, origin, origin, settings, level.getRandom(), Block.UPDATE_CLIENTS);
        AABB lot = AABB.encapsulatingFullBlocks(origin, origin.offset(OldPlaceBlueprint.WIDTH, 22, OldPlaceBlueprint.DEPTH));
        try {
            BlockPos stake = origin.offset(OldPlaceBlueprint.STAKE);
            h.assertTrue(level.getBlockState(stake).is(ModBlocks.CLAIM_STAKE.get()), "no Claim Stake by the gate");
            h.assertTrue(level.getBlockEntity(stake.east()) instanceof SignBlockEntity, "no for-sale board by the stake");
            h.assertFalse(level.getBlockState(origin.offset(OldPlaceBlueprint.GATE_X, 0, 0)).is(net.minecraft.world.level.block.Blocks.JIGSAW), "the connector was left in the gateway");
            int chests = 0;
            for (BlockPos pos : BlockPos.betweenClosed(origin, origin.offset(OldPlaceBlueprint.WIDTH - 1, 12, OldPlaceBlueprint.DEPTH - 1))) {
                if (level.getBlockEntity(pos) instanceof ChestBlockEntity chest) {
                    chests++;
                    h.assertTrue(chest.getLootTable() != null && chest.getLootTable().identifier().getPath().contains("old_place"), "a chest without the Old Place's loot at " + pos);
                }
            }
            h.assertTrue(chests == 4, "expected the pantry, trunk, desk and barn chests, found " + chests);
            h.assertTrue(level.getEntitiesOfClass(Cow.class, lot).size() == OldPlaceBlueprint.CATTLE, "the corral is short of cattle");
            h.assertTrue(level.getBlockState(origin.offset(OldPlaceBlueprint.PIANO)).is(ModBlocks.SALOON_PIANO.get()), "no piano in the parlour");

            ServerPlayer owner = TestPlayers.survival(h, new BlockPos(2, 1, 2), "hf-homesteader");
            ServerPlayer visitor = TestPlayers.survival(h, new BlockPos(4, 1, 2), "hf-visitor");
            StakeData data = StakeData.get(level);
            h.assertTrue(data.at(stake).isEmpty(), "an Old Place nobody has taken has an owner");
            OldPlace.record(level, owner, stake, new TownRef(TOWN, "Testville"));
            StakeData.Claim claim = data.at(stake).orElseThrow();
            h.assertTrue(claim.owner().equals(owner.getUUID()) && data.of(owner.getUUID(), TOWN).isPresent() && claim.contains(origin.offset(1, 5, 20)) && claim.contains(origin.offset(0, 5, OldPlaceBlueprint.DEPTH - 1)),
                    "the claim isn't on record");
            h.assertTrue(AdvancementTests.earned(owner, "frontier/the_old_place"), "the_old_place not earned");
            String board = ((SignBlockEntity) level.getBlockEntity(stake.east())).getText(net.minecraft.world.level.block.entity.SignTextSlot.FRONT).getMessages(false).get(2).getString();
            h.assertTrue(board.equals("HF-HOMESTEADER"), "the board wasn't repainted: " + board);

            Container mailbox = MailboxBlock.of(level, claim).orElseThrow(() -> new IllegalStateException("no mailbox at the gate"));
            MailboxBlock.deliver(level);
            MailboxBlock.deliver(level);
            h.assertTrue(mailbox.countItem(ModItems.FRONTIER_GAZETTE.get()) == 1, "the mailbox should have one Gazette, has " + mailbox.countItem(ModItems.FRONTIER_GAZETTE.get()));
            BlockPos box = origin.offset(OldPlaceBlueprint.GATE_X - 3, 2, 0);
            h.assertTrue(level.getBlockState(box).is(ModBlocks.MAILBOX.get()), "no mailbox where the blueprint puts it");
            level.getBlockState(box).useWithoutItem(level, visitor, new net.minecraft.world.phys.BlockHitResult(Vec3.atCenterOf(box), Direction.NORTH, box, false));
            h.assertFalse(visitor.containerMenu != visitor.inventoryMenu, "a visitor opened the owner's mail");

            // The brand: cold iron does nothing, a hot one marks a cow for its holder's place, and nobody brands it twice
            Cow cow = level.getEntitiesOfClass(Cow.class, lot).getFirst();
            ItemStack iron = new ItemStack(ModItems.BRANDING_IRON.get());
            owner.setItemInHand(InteractionHand.MAIN_HAND, iron);
            h.assertFalse(Herds.brand(level, owner, cow, iron, InteractionHand.MAIN_HAND), "a cold iron branded");
            iron.set(ModDataComponents.HOT_UNTIL.get(), level.getGameTime() + StakeItems.BrandingIron.HOT_TICKS);
            h.assertTrue(Herds.brand(level, owner, cow, iron, InteractionHand.MAIN_HAND) && Herds.brandOf(cow).owner().equals(owner.getUUID())
                    && Herds.brandOf(cow).town() == TOWN && iron.getDamageValue() == 1, "a hot iron didn't brand");
            h.assertFalse(Herds.brand(level, owner, cow, iron, InteractionHand.MAIN_HAND), "a branded cow was branded again");
            h.assertTrue(Herds.count(level, claim) == 1, "the stake should count one head, counts " + Herds.count(level, claim));
            h.assertTrue(AdvancementTests.earned(owner, "frontier/riding_for_the_brand"), "riding_for_the_brand not earned");
            ItemStack visitorsIron = new ItemStack(ModItems.BRANDING_IRON.get());
            visitorsIron.set(ModDataComponents.HOT_UNTIL.get(), level.getGameTime() + 100);
            Cow second = level.getEntitiesOfClass(Cow.class, lot).get(1);
            h.assertFalse(Herds.brand(level, visitor, second, visitorsIron, InteractionHand.MAIN_HAND), "someone with no place branded a cow");

            data.remove(stake);
        } finally {
            level.getEntitiesOfClass(net.minecraft.world.entity.Entity.class, lot.inflate(2), e -> !(e instanceof ServerPlayer)).forEach(net.minecraft.world.entity.Entity::discard);
            BlockPos.betweenClosed(origin, origin.offset(OldPlaceBlueprint.WIDTH - 1, 21, OldPlaceBlueprint.DEPTH - 1))
                    .forEach(pos -> level.setBlock(pos, net.minecraft.world.level.block.Blocks.AIR.defaultBlockState(), Block.UPDATE_CLIENTS | Block.UPDATE_SUPPRESS_DROPS));
        }
        h.assertTrue(OldPlace.rotation(Direction.NORTH) == Rotation.NONE && OldPlace.rotation(Direction.EAST) == Rotation.CLOCKWISE_90
                && OldPlace.rotation(Direction.SOUTH) == Rotation.CLOCKWISE_180 && OldPlace.rotation(Direction.WEST) == Rotation.COUNTERCLOCKWISE_90,
                "the front doesn't turn to face the buyer");
        h.succeed();
    }

    /** A drive: the herd is turned out marked, moves off from its drover, runs from a shot, and pays per head that arrives. */
    static void herdDrivenAndPaid(GameTestHelper h) {
        ServerLevel level = h.getLevel();
        ServerPlayer player = TestPlayers.survival(h, new BlockPos(5, 1, 5));
        h.assertTrue(Drives.perHead(600) == 10 && Drives.perHead(10) == Drives.MIN_PER_HEAD, "pay per head doesn't go by distance");
        h.assertTrue(Drives.salePrice(0) == Drives.SALE_BASE && Drives.salePrice(160) == Drives.SALE_BASE + 2, "branded stock doesn't sell by distance");
        BlockPos at = h.absolutePos(new BlockPos(5, 1, 5));
        BlockPos dest = com.hollowfrontier.content.frontier.town.Towns.middle(TOWN).offset(600, 0, 0); // pay goes by how far the town it left is
        // The test's cage: the framework roofs a test in barrier while it sets it up, and now and then the roof is still on
        // at the first tick. A herd is turned out on open ground, so a roof six blocks up reads as a building: take it off.
        for (BlockPos over : BlockPos.betweenClosed(at.offset(-9, 2, -9), at.offset(9, 12, 9))) {
            if (level.getBlockState(over).is(net.minecraft.world.level.block.Blocks.BARRIER)) {
                level.setBlock(over, net.minecraft.world.level.block.Blocks.AIR.defaultBlockState(), 2);
            }
        }
        Drives.Drive drive = Drives.start(level, player, at, TOWN, Optional.empty(), dest, 4);
        h.assertTrue(drive != null && drive.head() >= 1 && Drives.of(player) != null, "no herd turned out");
        List<Animal> herd = Drives.herd(level, player, drive);
        try {
            h.assertTrue(herd.size() == drive.head() && herd.stream().allMatch(cow -> Herds.herdOf(cow).drover().equals(player.getUUID())), "the herd isn't marked");
            Animal cow = herd.getFirst();
            cow.snapTo(at.getX() + 3.5, at.getY(), at.getZ() + 0.5, 0, 0);
            Vec3 to = new Herds.DriveGoal(cow).plan(Herds.herdOf(cow));
            h.assertTrue(to != null && to.distanceToSqr(Vec3.atCenterOf(dest)) < cow.position().distanceToSqr(Vec3.atCenterOf(dest)), "a cow near its drover doesn't walk on toward where the drive is bound: " + to);
            h.assertFalse(Herds.stampeding(cow), "a quiet herd is stampeding");
            h.assertTrue(Herds.spook(level, player.position(), Herds.SPOOK_RANGE) && Herds.stampeding(cow), "a shot didn't stampede the herd");
            int in = herd.size();
            int pay = Drives.arrive(level, player, drive);
            h.assertTrue(pay == in * Drives.perHead(600) && Purse.count(player) == pay, "wrong pay for " + in + " head: " + pay);
            h.assertTrue(Drives.of(player) == null && herd.stream().noneMatch(Animal::isAlive), "the drive isn't over");
            h.assertTrue(Standing.of(player).points(TOWN) == Math.max(1, in / 2), "a drive didn't raise standing");
            h.assertTrue(AdvancementTests.earned(player, "frontier/head_em_up"), "head_em_up not earned");
        } finally {
            herd.forEach(Animal::discard);
        }
        // A herd that never arrives costs standing where it came from
        Standing.add(player, TOWN, 10);
        int before = Standing.of(player).points(TOWN);
        Drives.Drive lost = new Drives.Drive(java.util.UUID.randomUUID(), TOWN, Optional.empty(), dest, 8);
        player.setData(ModAttachments.DRIVE.get(), lost);
        h.assertTrue(Drives.arrive(level, player, lost) == 0 && Standing.of(player).points(TOWN) == before - Drives.LOST_STANDING, "a lost herd cost nothing");
        h.succeed();
    }

    /** The Old Place's loot tables load, and its template is in every town style. */
    static void oldPlaceData(GameTestHelper h) {
        ServerLevel level = h.getLevel();
        for (var key : List.of(ModChestLoot.OLD_PLACE_PANTRY, ModChestLoot.OLD_PLACE_TRUNK, ModChestLoot.OLD_PLACE_DESK, ModChestLoot.OLD_PLACE_BARN)) {
            h.assertTrue(level.getServer().reloadableRegistries().getLootTable(key) != net.minecraft.world.level.storage.loot.LootTable.EMPTY, "no loot table " + key.identifier());
        }
        for (var style : com.hollowfrontier.worldgen.structure.TownStyle.values()) {
            h.assertTrue(level.getStructureTemplateManager().get(HollowFrontier.id(style.template(OldPlaceBlueprint.NAME))).isPresent(), "no Old Place for " + style);
        }
        h.succeed();
    }
}
