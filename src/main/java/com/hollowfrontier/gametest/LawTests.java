package com.hollowfrontier.gametest;

import java.util.List;

import com.hollowfrontier.content.frontier.gazette.GazetteItem;
import com.hollowfrontier.content.frontier.law.Wanted;
import com.hollowfrontier.content.frontier.talk.Chatter;
import com.hollowfrontier.content.frontier.townsfolk.Folk;
import com.hollowfrontier.content.frontier.townsfolk.Lawman;
import com.hollowfrontier.content.frontier.townsfolk.Townsfolk;
import com.hollowfrontier.registry.ModAttachments;
import com.hollowfrontier.registry.ModBlocks;
import com.hollowfrontier.registry.ModEntities;
import com.hollowfrontier.registry.ModItems;

import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.Difficulty;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.WrittenBookContent;
import net.minecraft.world.phys.AABB;

// The wanted level, posses, fines, folk trades and the Frontier Gazette.
final class LawTests {
    private LawTests() {}

    /** Crimes add heat, two stars shut the shops, and the fine at a Bounty Board clears it (with change). */
    static void crimesMakeYouWanted(GameTestHelper h) {
        ServerLevel level = h.getLevel();
        level.getServer().setDifficulty(Difficulty.NORMAL, true);
        ServerPlayer player = TestPlayers.survival(h, new BlockPos(2, 1, 2));
        Townsfolk folk = h.spawnWithNoFreeWill(ModEntities.TOWNSFOLK.get(), new BlockPos(4, 1, 4));
        folk.hurtServer(level, level.damageSources().playerAttack(player), 1.0F);
        h.assertTrue(Wanted.of(player).heat() == Wanted.HURT_TOWNSFOLK && Wanted.stars(player) == 1, "hurting townsfolk: " + Wanted.of(player));

        Lawman lawman = h.spawnWithNoFreeWill(ModEntities.LAWMAN.get(), new BlockPos(8, 1, 8));
        lawman.hurtServer(level, level.damageSources().playerAttack(player), 1000.0F);
        int heat = Wanted.HURT_TOWNSFOLK + Wanted.KILL_LAWMAN;
        h.assertTrue(Wanted.of(player).heat() == heat && Wanted.stars(player) == 2, "killing a lawman: " + Wanted.of(player));

        folk.mobInteract(player, InteractionHand.MAIN_HAND);
        h.assertFalse(folk.isTrading(), "townsfolk traded with a wanted player");

        // Fine of heat * 4 copper, paid with 2 gold (18) + 5 copper = 23: 3 copper back.
        int fine = heat * Wanted.FINE_PER_HEAT;
        h.assertTrue(fine == 20, "fine changed, update this test: " + fine);
        player.getInventory().add(new ItemStack(ModItems.GOLD_COIN.get(), 2));
        player.getInventory().add(new ItemStack(ModItems.COPPER_COIN.get(), 5));
        BlockPos board = new BlockPos(2, 1, 4);
        h.setBlock(board, ModBlocks.BOUNTY_BOARD.get());
        h.useBlock(board, player);
        h.assertTrue(Wanted.stars(player) == 0, "fine didn't clear the player: " + Wanted.of(player));
        h.assertTrue(player.getInventory().countItem(ModItems.COPPER_COIN.get()) == 3 && player.getInventory().countItem(ModItems.GOLD_COIN.get()) == 0,
                "wrong change from the fine");
        h.succeed();
    }

    static void wantedCoolsOff(GameTestHelper h) {
        ServerPlayer player = TestPlayers.survival(h, new BlockPos(2, 1, 2));
        Wanted.addHeat(player, 2);
        Wanted.tick(player);
        h.assertTrue(Wanted.of(player).heat() == 2, "cooled off too soon");
        long now = h.getLevel().getGameTime();
        player.setData(ModAttachments.WANTED.get(), new Wanted(2, now - Wanted.COOL_TICKS, now + 100000));
        Wanted.tick(player);
        h.assertTrue(Wanted.of(player).heat() == 1, "didn't cool off: " + Wanted.of(player));
        h.succeed();
    }

    static void lawmenShootWantedOnSight(GameTestHelper h) {
        h.getLevel().getServer().setDifficulty(Difficulty.NORMAL, true);
        ServerPlayer player = TestPlayers.survival(h, new BlockPos(2, 1, 2));
        Wanted.addHeat(player, 5);
        Lawman lawman = ModEntities.LAWMAN.get().spawn(h.getLevel(), h.absolutePos(new BlockPos(8, 1, 8)), EntitySpawnReason.SPAWN_ITEM_USE);
        h.succeedWhen(() -> h.assertTrue(lawman.getTarget() == player, "lawman hasn't gone for a 2-star player"));
    }

    static void posseRidesOut(GameTestHelper h) {
        ServerPlayer player = TestPlayers.survival(h, new BlockPos(5, 1, 5));
        Wanted.addHeat(player, Wanted.MAX_HEAT);
        Wanted.sendPosse(h.getLevel(), player);
        List<Lawman> posse = h.getLevel().getEntitiesOfClass(Lawman.class, new AABB(player.blockPosition()).inflate(40, 128, 40), Lawman::isPosse);
        try {
            h.assertTrue(posse.size() == 2, "expected a posse of 2, got " + posse.size());
            h.assertTrue(posse.stream().allMatch(l -> l.getTarget() == player && !l.getMainHandItem().isEmpty()), "posse not armed and hunting");
            h.assertTrue(Chatter.heard(player).stream().anyMatch(k -> k.startsWith("chatter.hollowfrontier.lawman.posse.")),
                    "the posse rode in without a word: " + Chatter.heard(player));
        } finally {
            posse.forEach(Entity::discard);
        }
        h.succeed();
    }

    static void folkHaveOwnTrades(GameTestHelper h) {
        Townsfolk prospector = h.spawnWithNoFreeWill(ModEntities.TOWNSFOLK.get(), new BlockPos(3, 1, 3));
        prospector.setFolk(Folk.PROSPECTOR);
        h.assertTrue(prospector.getOffers().stream().anyMatch(o -> o.getResult().is(ModItems.DYNAMITE.get())), "prospector sells no dynamite");
        h.assertTrue(prospector.getOffers().stream().anyMatch(o -> o.getCostA().is(ModItems.SNAKESKIN.get())), "prospector buys no snakeskin");
        Townsfolk shopkeeper = h.spawnWithNoFreeWill(ModEntities.TOWNSFOLK.get(), new BlockPos(7, 1, 7));
        shopkeeper.setFolk(Folk.SHOPKEEPER);
        h.assertTrue(shopkeeper.getOffers().stream().anyMatch(o -> o.getResult().is(ModItems.FRONTIER_GAZETTE.get())), "shopkeeper sells no gazette");
        h.succeed();
    }

    static void gazettePrintsOnRead(GameTestHelper h) {
        ServerPlayer player = TestPlayers.survival(h, new BlockPos(5, 1, 5));
        ItemStack gazette = new ItemStack(ModItems.FRONTIER_GAZETTE.get());
        player.setItemInHand(InteractionHand.MAIN_HAND, gazette);
        gazette.use(h.getLevel(), player, InteractionHand.MAIN_HAND);
        WrittenBookContent content = player.getMainHandItem().get(DataComponents.WRITTEN_BOOK_CONTENT);
        // Four pages, five when a town was raided this week (raid tests may have run first).
        h.assertTrue(content != null && content.pages().size() >= 4, "gazette not printed: " + content);
        Wanted.addHeat(player, 2);
        WrittenBookContent wanted = GazetteItem.print(h.getLevel(), player);
        h.assertTrue(wanted.pages().get(2).raw().getString().contains(player.getName().getString())
                        || wanted.pages().get(2).raw().toString().contains("wanted_you"),
                "the wanted column doesn't name the wanted reader");
        h.succeed();
    }
}
