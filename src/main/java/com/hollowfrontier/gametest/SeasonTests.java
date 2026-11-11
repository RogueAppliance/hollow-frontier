package com.hollowfrontier.gametest;

import java.time.LocalDate;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import com.hollowfrontier.HollowFrontier;
import com.hollowfrontier.content.frontier.furniture.TownLampBlock;
import com.hollowfrontier.content.frontier.gazette.GazetteBackPage;
import com.hollowfrontier.content.frontier.law.Wanted;
import com.hollowfrontier.content.frontier.music.SaloonPianoBlock;
import com.hollowfrontier.content.frontier.music.SaloonPianoBlockEntity;
import com.hollowfrontier.content.frontier.raid.TownRaids;
import com.hollowfrontier.content.frontier.saloon.Barmaid;
import com.hollowfrontier.content.frontier.saloon.Drink;
import com.hollowfrontier.content.frontier.season.ChristmasParcelItem;
import com.hollowfrontier.content.frontier.season.Season;
import com.hollowfrontier.content.frontier.season.TownTree;
import com.hollowfrontier.content.frontier.talk.Act;
import com.hollowfrontier.content.frontier.talk.Chatter;
import com.hollowfrontier.content.frontier.talk.ChatterLines;
import com.hollowfrontier.content.frontier.talk.Context;
import com.hollowfrontier.content.frontier.talk.Fact;
import com.hollowfrontier.content.frontier.talk.Moment;
import com.hollowfrontier.content.frontier.townsfolk.Folk;
import com.hollowfrontier.content.frontier.townsfolk.Lawman;
import com.hollowfrontier.content.frontier.townsfolk.Townsfolk;
import com.hollowfrontier.registry.ModAttachments;
import com.hollowfrontier.registry.ModBlocks;
import com.hollowfrontier.registry.ModEntities;
import com.hollowfrontier.registry.ModItems;
import com.hollowfrontier.registry.ModMobEffects;
import com.hollowfrontier.registry.ModSounds;

import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;

/**
 * A frontier December (35): the season by the date and by its mode, the lamps, the town tree, the parcel, the drinks,
 * the carols, the words and the truce. Each test sets the date through the clock seam, does what it has to in the same
 * tick and puts the clock back, so no other test ever sees December (the game test server's own date is held in July).
 */
final class SeasonTests {
    private SeasonTests() {}

    private static final LocalDate JULY = LocalDate.of(2026, 7, 4), DECEMBER = LocalDate.of(2026, 12, 10),
            CHRISTMAS = LocalDate.of(2026, 12, 25), JANUARY = LocalDate.of(2027, 1, 2);
    /** The longest a December line may run and still read in chat as one remark. */
    private static final int MAX_LINE = 130;

    private static void on(LocalDate date, Runnable body) {
        Season.setClock(() -> date);
        try {
            body.run();
        } finally {
            Season.setClock(null);
            Season.setMode(null);
        }
    }

    /** 35a: December is the month, Christmas the 24th to the 26th, and the mode holds the month on or off. */
    static void seasonFollowsTheDate(GameTestHelper h) {
        h.assertTrue(Season.december(Season.Mode.AUTO, LocalDate.of(2026, 12, 1)) && Season.december(Season.Mode.AUTO, LocalDate.of(2026, 12, 31)),
                "December should run the whole month");
        h.assertFalse(Season.december(Season.Mode.AUTO, LocalDate.of(2026, 11, 30)) || Season.december(Season.Mode.AUTO, JANUARY),
                "December shouldn't run past the month");
        h.assertTrue(Season.december(Season.Mode.ALWAYS, JULY), "always should hold it on in July");
        h.assertFalse(Season.december(Season.Mode.NEVER, DECEMBER), "never should hold it off in December");
        for (int day = 22; day <= 28; day++) {
            boolean christmas = day >= 24 && day <= 26;
            h.assertTrue(Season.christmas(Season.Mode.AUTO, LocalDate.of(2026, 12, day)) == christmas, "Christmas on the " + day + "th: should be " + christmas);
        }
        h.assertFalse(Season.christmas(Season.Mode.ALWAYS, JULY), "always shouldn't make it Christmas all year (the truce would never end)");
        h.assertFalse(Season.christmas(Season.Mode.NEVER, CHRISTMAS), "never should hold Christmas off too");
        ServerLevel level = h.getLevel();
        h.assertFalse(Season.isDecember(level), "the test server's own date should be out of season");
        on(DECEMBER, () -> {
            h.assertTrue(Season.isDecember(level) && !Season.isChristmas(level), "the clock seam should make it December");
            Season.setMode(Season.Mode.NEVER);
            h.assertFalse(Season.isDecember(level), "never should override the date");
        });
        on(JULY, () -> {
            Season.setMode(Season.Mode.ALWAYS);
            h.assertTrue(Season.isDecember(level), "always should override the date");
        });
        on(CHRISTMAS, () -> h.assertTrue(Season.isChristmas(level) && Season.truce(level) && Season.year() == 2026, "Christmas Day, 2026, and the truce"));
        h.assertFalse(Season.isDecember(level), "the clock should be put back");
        h.succeed();
    }

    /** 35a: a Town Lamp's random tick dresses it in December and undresses it in January, with its light, in one change. */
    static void lampsDressByDate(GameTestHelper h) {
        BlockPos lamp = new BlockPos(3, 1, 3);
        h.setBlock(new BlockPos(3, 0, 3), Blocks.STONE);
        ServerLevel level = h.getLevel();
        boolean night = TownLampBlock.shouldBeLit(level);
        h.setBlock(lamp, ModBlocks.TOWN_LAMP.get().defaultBlockState().setValue(TownLampBlock.LIT, !night));
        BlockPos abs = h.absolutePos(lamp);
        h.assertFalse(h.getBlockState(lamp).getValue(TownLampBlock.FESTIVE), "a lamp should start out plain");
        on(DECEMBER, () -> {
            level.getBlockState(abs).randomTick(level, abs, level.getRandom());
            BlockState dressed = level.getBlockState(abs);
            h.assertTrue(dressed.getValue(TownLampBlock.FESTIVE), "the lamp didn't dress in December");
            h.assertTrue(dressed.getValue(TownLampBlock.LIT) == night && dressed.getLightEmission() == (night ? 15 : 0), "dressing should set its light too");
        });
        on(JANUARY, () -> {
            level.getBlockState(abs).randomTick(level, abs, level.getRandom());
            h.assertFalse(level.getBlockState(abs).getValue(TownLampBlock.FESTIVE), "the lamp didn't undress in January");
        });
        on(JULY, () -> {
            Season.setMode(Season.Mode.ALWAYS);
            level.getBlockState(abs).randomTick(level, abs, level.getRandom());
            h.assertTrue(level.getBlockState(abs).getValue(TownLampBlock.FESTIVE), "always should dress the lamp in July");
        });
        on(DECEMBER, () -> {
            Season.setMode(Season.Mode.NEVER);
            level.getBlockState(abs).randomTick(level, abs, level.getRandom());
            h.assertFalse(level.getBlockState(abs).getValue(TownLampBlock.FESTIVE), "never should undress the lamp in December");
        });
        h.succeed();
    }

    /** 35b: one tree to a town in December, none out of season or off the Overworld, and it takes itself away in January. */
    static void townTreeStandsInDecember(GameTestHelper h) {
        ServerLevel level = h.getLevel();
        BlockPos square = h.absolutePos(new BlockPos(3, 1, 3));
        on(JULY, () -> h.assertTrue(TownTree.ensure(level, square).isEmpty(), "a tree went up in July"));
        TownTree[] tree = new TownTree[1];
        on(DECEMBER, () -> {
            tree[0] = TownTree.ensure(level, square).orElse(null);
            h.assertTrue(tree[0] != null, "no tree went up in December");
            h.assertTrue(tree[0].position().distanceTo(net.minecraft.world.phys.Vec3.atBottomCenterOf(square)) <= TownTree.SEARCH * 1.5 + 2, "the tree should stand by the middle of the square");
            h.assertTrue(TownTree.ensure(level, square).isEmpty(), "a second tree went up in the same town");
            tree[0].keep(level);
            h.assertTrue(tree[0].isAlive() && tree[0].isLit() == level.isDarkOutside(), "the tree should stand through December, lit after dark");
            h.assertFalse(tree[0].isPickable() || tree[0].isPushable() || tree[0].isAttackable(), "the tree shouldn't be something to hit or push");
            h.assertFalse(tree[0].hurtServer(level, level.damageSources().generic(), 100.0F), "the tree shouldn't take damage");
            ServerLevel nether = level.getServer().getLevel(Level.NETHER);
            h.assertTrue(nether == null || TownTree.outOfSeason(nether), "no tree outside the Overworld");
        });
        on(JANUARY, () -> {
            tree[0].keep(level);
            h.assertFalse(tree[0].isAlive(), "the tree should take itself away once the month is over");
        });
        tree[0].discard();
        h.assertTrue(ModEntities.TOWN_TREE.is(net.neoforged.neoforge.common.Tags.EntityTypes.CAPTURING_NOT_SUPPORTED)
                && ModEntities.TOWN_TREE.is(net.neoforged.neoforge.common.Tags.EntityTypes.TELEPORTING_NOT_SUPPORTED),
                "the tree should be tagged against capture and teleporting");
        h.succeed();
    }

    /** 35b: a postmaster hands over one parcel a year, a shopkeeper does where there's no postmaster, and it always holds a Fruitcake. */
    static void parcelOncePerYear(GameTestHelper h) {
        ServerLevel level = h.getLevel();
        ServerPlayer player = TestPlayers.survival(h, new BlockPos(3, 1, 3));
        player.setData(ModAttachments.SURVEY_LETTER.get(), true); // so the Survey's letter doesn't come first
        Townsfolk postmaster = h.spawnWithNoFreeWill(ModEntities.TOWNSFOLK.get(), new BlockPos(4, 1, 4));
        postmaster.setFolk(Folk.POSTMASTER);
        Townsfolk shopkeeper = h.spawnWithNoFreeWill(ModEntities.TOWNSFOLK.get(), new BlockPos(2, 1, 4));
        shopkeeper.setFolk(Folk.SHOPKEEPER);
        on(JULY, () -> h.assertFalse(ChristmasParcelItem.due(level, player), "a parcel was due in July"));
        on(DECEMBER, () -> {
            h.assertTrue(ChristmasParcelItem.due(level, player), "no parcel due in December");
            h.assertTrue(ChristmasParcelItem.handsOver(level, postmaster), "the postmaster should hand parcels over");
            h.assertFalse(ChristmasParcelItem.handsOver(level, shopkeeper), "the shopkeeper shouldn't, in a town with a postmaster");
            postmaster.mobInteract(player, InteractionHand.MAIN_HAND);
            h.assertTrue(player.getInventory().countItem(ModItems.CHRISTMAS_PARCEL.get()) == 1, "the postmaster's first word should hand over the parcel");
            h.assertFalse(ChristmasParcelItem.due(level, player) || ChristmasParcelItem.handOver(level, player), "one parcel a year");
        });
        on(LocalDate.of(2027, 12, 3), () -> h.assertTrue(ChristmasParcelItem.due(level, player), "next December should bring another"));
        postmaster.discard();
        on(DECEMBER, () -> h.assertTrue(ChristmasParcelItem.handsOver(level, shopkeeper), "with no postmaster, the shopkeeper hands it over"));
        // Opening it
        int slot = player.getInventory().findSlotMatchingItem(new ItemStack(ModItems.CHRISTMAS_PARCEL.get()));
        player.getInventory().setItem(slot, ItemStack.EMPTY);
        player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(ModItems.CHRISTMAS_PARCEL.get()));
        player.getMainHandItem().use(level, player, InteractionHand.MAIN_HAND);
        h.assertTrue(player.getInventory().countItem(ModItems.CHRISTMAS_PARCEL.get()) == 0, "opening the parcel should use it up");
        h.assertTrue(player.getInventory().countItem(ModItems.FRUITCAKE.get()) == 1, "a parcel always holds a Fruitcake");
        h.assertTrue(player.getInventory().countItem(ModItems.COPPER_COIN.get()) >= 6, "a parcel holds some coin");
        // Nobody will buy one
        for (Folk folk : Folk.values()) {
            Townsfolk trader = h.spawnWithNoFreeWill(ModEntities.TOWNSFOLK.get(), new BlockPos(1, 1, 1));
            trader.setFolk(folk);
            h.assertTrue(trader.getOffers().stream().noneMatch(o -> o.getItemCostA().itemStack().is(ModItems.FRUITCAKE.get())), folk + " buys Fruitcake");
            trader.discard();
        }
        h.succeed();
    }

    /** 35c: the barmaid's December drinks come onto her list and go off it, and do what they say. */
    static void decemberDrinksComeAndGo(GameTestHelper h) {
        Barmaid barmaid = h.spawnWithNoFreeWill(ModEntities.BARMAID.get(), new BlockPos(3, 1, 3));
        int usual = barmaid.getOffers().size();
        h.assertTrue(pours(barmaid, ModItems.EGGNOG.get()) == 0 && pours(barmaid, ModItems.HOT_CIDER.get()) == 0, "December's drinks on the list out of season");
        barmaid.seasonal(true);
        barmaid.seasonal(true);
        h.assertTrue(pours(barmaid, ModItems.EGGNOG.get()) == 1 && pours(barmaid, ModItems.HOT_CIDER.get()) == 1, "December should add each drink once");
        barmaid.seasonal(false);
        h.assertTrue(barmaid.getOffers().size() == usual && pours(barmaid, ModItems.EGGNOG.get()) == 0, "January should take them off again and leave the rest");

        ServerPlayer player = TestPlayers.survival(h, new BlockPos(5, 1, 5));
        player.setTicksFrozen(100);
        new ItemStack(ModItems.HOT_CIDER.get()).finishUsingItem(h.getLevel(), player);
        h.assertTrue(player.getTicksFrozen() == 0 && player.hasEffect(MobEffects.REGENERATION) && Drink.tipsyLevel(player) == 0,
                "Hot Cider should thaw and mend, and not make you Tipsy");
        new ItemStack(ModItems.EGGNOG.get()).finishUsingItem(h.getLevel(), player);
        h.assertTrue(player.hasEffect(MobEffects.ABSORPTION) && Drink.tipsyLevel(player) == 1, "Eggnog should give Absorption and one Tipsy");
        player.removeEffect(ModMobEffects.TIPSY);
        h.succeed();
    }

    private static long pours(Barmaid barmaid, net.minecraft.world.item.Item drink) {
        return barmaid.getOffers().stream().filter(o -> o.getResult().is(drink)).count();
    }

    /** 35c: in December the piano works a carol in every other tune; the carols are real recordings; the server sets it by date. */
    static void pianoPlaysCarols(GameTestHelper h) {
        String soundsJson;
        try (var in = HollowFrontier.class.getResourceAsStream("/assets/" + HollowFrontier.MODID + "/sounds.json")) {
            soundsJson = new String(in.readAllBytes(), java.nio.charset.StandardCharsets.UTF_8);
        } catch (java.io.IOException e) {
            throw new IllegalStateException(e);
        }
        h.assertTrue(ModSounds.PIANO_CAROLS.size() == 3, "three carols");
        String lang;
        try (var in = HollowFrontier.class.getResourceAsStream("/assets/" + HollowFrontier.MODID + "/lang/en_us.json")) {
            lang = new String(in.readAllBytes(), java.nio.charset.StandardCharsets.UTF_8);
        } catch (java.io.IOException e) {
            throw new IllegalStateException(e);
        }
        // Every tune the piano plays has a name for "Now Playing" (the carols first shipped without, and showed their keys)
        for (var tune : java.util.stream.Stream.concat(ModSounds.PIANO_REPERTOIRE.stream(), ModSounds.PIANO_CAROLS.stream()).toList()) {
            String name = tune.getId().getPath().substring("block.saloon_piano.".length());
            h.assertTrue(lang.contains("\"track." + HollowFrontier.MODID + "." + name + "\""), "no Now Playing name for " + name);
        }
        for (var carol : ModSounds.PIANO_CAROLS) {
            String name = carol.getId().getPath().substring("block.saloon_piano.".length());
            h.assertTrue(BuiltInRegistries.SOUND_EVENT.containsKey(carol.getId()) && soundsJson.contains("\"" + carol.getId().getPath() + "\""),
                    "carol not registered or not in sounds.json: " + carol.getId());
            h.assertTrue(HollowFrontier.class.getResource("/assets/" + HollowFrontier.MODID + "/sounds/block/saloon_piano/" + name + ".ogg") != null,
                    "recording missing: " + name + ".ogg");
        }
        BlockPos pos = new BlockPos(3, 1, 3);
        h.setBlock(pos, ModBlocks.SALOON_PIANO.get());
        BlockPos abs = h.absolutePos(pos);
        ServerLevel level = h.getLevel();
        SaloonPianoBlockEntity piano = (SaloonPianoBlockEntity) level.getBlockEntity(abs);
        Set<Object> carols = new HashSet<>(ModSounds.PIANO_CAROLS);
        for (int i = 0; i < 8; i++) {
            h.assertFalse(carols.contains(piano.nextTune(false)), "a carol out of season");
        }
        Set<Object> heard = new HashSet<>();
        for (int i = 0; i < 6; i++) {
            Object tune = piano.nextTune(true);
            h.assertTrue(carols.contains(tune) == (i % 2 == 0), "December should be a carol every other tune (tune " + i + ")");
            heard.add(tune);
        }
        h.assertTrue(heard.containsAll(carols) && heard.size() == 6, "three carols and three rags in six tunes: " + heard);
        on(DECEMBER, () -> {
            SaloonPianoBlockEntity.watch(level, abs, level.getBlockState(abs));
            h.assertTrue(level.getBlockState(abs).getValue(SaloonPianoBlock.CAROLS), "the server should give the piano carols in December");
        });
        on(JANUARY, () -> {
            SaloonPianoBlockEntity.watch(level, abs, level.getBlockState(abs));
            h.assertFalse(level.getBlockState(abs).getValue(SaloonPianoBlock.CAROLS), "and take them away in January");
        });
        h.succeed();
    }

    /** 35d: December's lines and Gazette pieces are said only in season, there are enough of them, and they fit. */
    static void decemberWordsKeepToTheSeason(GameTestHelper h) {
        List<ChatterLines.Line> lines = ChatterLines.lines().stream()
                .filter(l -> l.cond().all().contains(Fact.DECEMBER) || l.cond().all().contains(Fact.CHRISTMAS)).toList();
        h.assertTrue(lines.size() >= 30, "about thirty December lines, found " + lines.size());
        for (ChatterLines.Line line : lines) {
            h.assertTrue(line.text().length() <= MAX_LINE, line.key() + " runs long: " + line.text().length());
            h.assertFalse(line.cond().test(Context.of(Act.FRONTIER)) || line.cond().test(Context.of(Act.FRONTIER, Fact.SIGHTINGS, Fact.STRANGER)),
                    line.key() + " can be said out of season");
        }
        h.assertTrue(lines.stream().map(ChatterLines.Line::voice).distinct().count() >= 12, "the season should run across the voices");
        long exchanges = ChatterLines.exchanges().stream()
                .filter(e -> e.cond().all().contains(Fact.DECEMBER) || e.cond().all().contains(Fact.CHRISTMAS)).count();
        h.assertTrue(exchanges >= 3, "a few December exchanges, found " + exchanges);
        h.assertTrue(Chatter.exchanges(Context.of(Act.FRONTIER)).stream().noneMatch(e -> e.cond().all().contains(Fact.DECEMBER)), "December gossip out of season");
        // The sleigh: the men in black take it up once there have been lights in a December sky
        h.assertTrue(Chatter.candidates(List.of("man_in_black"), Moment.TALK, Context.of(Act.FRONTIER, Fact.DECEMBER, Fact.SIGHTINGS)).stream()
                .anyMatch(l -> l.text().startsWith("It was a sleigh.")), "the men in black should say it was a sleigh");
        h.assertTrue(Chatter.candidates(List.of("kindled"), Moment.TALK, Context.of(Act.GATEWAY, Fact.DECEMBER)).stream()
                .anyMatch(l -> l.cond().all().contains(Fact.DECEMBER)), "the Kindled should have the month explained to them");
        List<GazetteBackPage.Piece> pieces = GazetteBackPage.pieces().stream()
                .filter(p -> p.cond().all().contains(Fact.DECEMBER) || p.cond().all().contains(Fact.CHRISTMAS)).toList();
        h.assertTrue(pieces.size() >= 6, "a few December back-page pieces, found " + pieces.size());
        for (GazetteBackPage.Piece piece : pieces) {
            int max = piece.kind() == GazetteBackPage.Kind.CLASSIFIED ? GazetteBackPage.MAX_CLASSIFIED : GazetteBackPage.MAX_PIECE;
            h.assertTrue(piece.text().length() <= max, piece.key() + " runs off its page: " + piece.text().length());
            h.assertFalse(piece.cond().test(Context.of(Act.FRONTIER, Fact.SIGHTINGS)), piece.key() + " can be printed out of season");
        }
        // And the world's own facts follow the date
        ServerLevel level = h.getLevel();
        BlockPos here = h.absolutePos(new BlockPos(3, 1, 3));
        h.assertFalse(Context.of(level, here, null).has(Fact.DECEMBER), "December in the test server's July");
        on(DECEMBER, () -> h.assertTrue(Context.of(level, here, null).has(Fact.DECEMBER) && !Context.of(level, here, null).has(Fact.CHRISTMAS), "the 10th"));
        on(CHRISTMAS, () -> h.assertTrue(Context.of(level, here, null).has(Fact.DECEMBER) && Context.of(level, here, null).has(Fact.CHRISTMAS), "the 25th"));
        h.succeed();
    }

    /** 35d: over Christmas no raid starts, and a sheriff lets one one-star fine go, once a year. */
    static void christmasTruce(GameTestHelper h) {
        ServerLevel level = h.getLevel();
        TownRaids raids = TownRaids.get(level);
        on(CHRISTMAS, () -> h.assertFalse(raids.raidsMayStart(level), "a raid could start during the truce"));
        boolean usually = raids.raidsMayStart(level);
        on(LocalDate.of(2026, 12, 23), () -> h.assertTrue(raids.raidsMayStart(level) == usually, "the truce shouldn't hold on the 23rd"));
        on(CHRISTMAS, () -> {
            Season.setMode(Season.Mode.NEVER);
            h.assertTrue(raids.raidsMayStart(level) == usually && !Season.truce(level), "with the season off there's no truce");
        });

        ServerPlayer player = TestPlayers.survival(h, new BlockPos(3, 1, 3));
        Lawman sheriff = h.spawnWithNoFreeWill(ModEntities.LAWMAN.get(), new BlockPos(4, 1, 4));
        sheriff.setRank(Lawman.Rank.SHERIFF);
        Lawman deputy = h.spawnWithNoFreeWill(ModEntities.LAWMAN.get(), new BlockPos(2, 1, 4));
        deputy.setRank(Lawman.Rank.DEPUTY);
        Wanted.addHeat(player, 1);
        on(LocalDate.of(2026, 12, 23), () -> h.assertFalse(sheriff.waiveFine(player), "no fine waived before Christmas"));
        on(CHRISTMAS, () -> {
            h.assertFalse(deputy.waiveFine(player), "only a sheriff lets a fine go");
            h.assertTrue(sheriff.waiveFine(player) && Wanted.stars(player) == 0, "the sheriff should let a one-star fine go at Christmas");
            Wanted.addHeat(player, 1);
            h.assertFalse(sheriff.waiveFine(player) || Wanted.stars(player) != 1, "one fine a Christmas");
        });
        on(LocalDate.of(2027, 12, 25), () -> {
            Wanted.addHeat(player, 5);
            h.assertTrue(Wanted.stars(player) == 2, "two stars");
            h.assertFalse(sheriff.waiveFine(player), "at two stars the law still shoots");
        });
        Wanted.clear(player);
        h.succeed();
    }
}
