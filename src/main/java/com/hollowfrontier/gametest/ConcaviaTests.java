package com.hollowfrontier.gametest;

import java.util.List;

import com.hollowfrontier.HollowFrontier;
import com.hollowfrontier.content.frontier.outlaw.Outlaw;
import com.hollowfrontier.content.frontier.townsfolk.Townsfolk;
import com.hollowfrontier.content.hollow.creature.GrayProbe;
import com.hollowfrontier.content.concavia.CrystalGuardian;
import com.hollowfrontier.content.concavia.Kindled;
import com.hollowfrontier.content.concavia.KindledSigilItem;
import com.hollowfrontier.content.concavia.KindledStanding;
import com.hollowfrontier.content.concavia.SunArchon;
import com.hollowfrontier.registry.ModBlocks;
import com.hollowfrontier.registry.ModEntities;
import com.hollowfrontier.registry.ModItems;
import com.hollowfrontier.worldgen.structure.ConcaviaBlueprints;
import com.hollowfrontier.worldgen.structure.ModChestLoot;

import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.Difficulty;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.trading.MerchantOffer;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.ChestBlockEntity;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructurePlaceSettings;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplate;
import net.minecraft.world.phys.AABB;

/**
 * Phase 12a: the Kindled. The crystal ruins' template and who lives in it; trading with a Kindled, the favour it wins and
 * the alliance (the sigil, once, and the ally's own trades); their story told in order as far as the player has earned;
 * hurting one, and the grudge easing; the guardians' choice of targets and their beam; the sigil showing the implanted.
 */
final class ConcaviaTests {
    private ConcaviaTests() {}

    /** The ruins: three Kindled at home in the sanctum, three guardians, the stores, the fountain and the lamps. */
    static void crystalRuinsPlace(GameTestHelper h) {
        StructureTemplate ruins = h.getLevel().getStructureTemplateManager().get(HollowFrontier.id(ConcaviaBlueprints.CRYSTAL_RUINS)).orElseThrow();
        BlockPos origin = h.absolutePos(BlockPos.ZERO);
        ruins.placeInWorld(h.getLevel(), origin, origin, new StructurePlaceSettings().setFinalizeEntities(true), h.getLevel().getRandom(), Block.UPDATE_CLIENTS);
        List<Entity> placed = h.getLevel().getEntities((Entity) null, new AABB(origin).expandTowards(46, 26, 46), e -> !(e instanceof ServerPlayer));
        try {
            List<Kindled> kindledFolk = placed.stream().filter(e -> e instanceof Kindled).map(e -> (Kindled) e).toList();
            h.assertValueEqual(kindledFolk.size(), 3, "Kindled");
            h.assertValueEqual(placed.stream().filter(e -> e instanceof CrystalGuardian).count(), 3L, "guardians");
            BlockPos sanctum0 = origin.offset(ConcaviaBlueprints.SANCTUM_X0, 0, ConcaviaBlueprints.SANCTUM_Z0);
            BlockPos sanctum1 = origin.offset(ConcaviaBlueprints.SANCTUM_X1, ConcaviaBlueprints.SANCTUM_HEIGHT, ConcaviaBlueprints.SANCTUM_Z1);
            for (Kindled kindled : kindledFolk) {
                BlockPos at = kindled.blockPosition();
                h.assertTrue(at.getX() > sanctum0.getX() && at.getX() < sanctum1.getX() && at.getZ() > sanctum0.getZ() && at.getZ() < sanctum1.getZ(),
                        "a Kindled outside the sanctum at " + at);
                h.assertTrue(kindled.hasHome(), "a Kindled with no home to keep to");
            }
            h.assertValueEqual(LoreSiteTests.chestsRolling(ruins, ModChestLoot.CRYSTAL_RUINS), 1L, "the stores' chest");
            h.assertValueEqual(LoreSiteTests.chestsRolling(ruins, ModChestLoot.CRYSTAL_HOME) + LoreSiteTests.chestsRolling(ruins, ModChestLoot.CRYSTAL_LIBRARY)
                    + LoreSiteTests.chestsRolling(ruins, ModChestLoot.CRYSTAL_MARKET), 3L, "the city's chests (a house, the library, a stall)");
            h.assertTrue(count(ruins, Blocks.BOOKSHELF) >= 20 && count(ruins, Blocks.LADDER) >= 10, "the library tower");
            h.assertTrue(count(ruins, ModBlocks.PHLOGISTON_LAMP.get()) >= 9, "the phlogiston lamps");
            h.assertTrue(count(ruins, ModBlocks.CONCAVIAN_CRYSTAL.get()) >= 40, "the crystal");
            h.assertTrue(count(ruins, Blocks.WATER) >= 12, "the fountain's water");
            h.assertTrue(count(ruins, ModBlocks.CRYSTAL_BRICK_WALL.get()) >= 4, "the lamp posts");
        } finally {
            placed.forEach(Entity::discard);
        }
        h.succeed();
    }

    private static int count(StructureTemplate template, Block block) {
        return template.filterBlocks(BlockPos.ZERO, new StructurePlaceSettings(), block).size();
    }

    private static MerchantOffer offerFor(Kindled kindled, net.minecraft.world.item.Item cost) {
        return kindled.getOffers().stream().filter(o -> o.getCostA().is(cost)).findFirst().orElseThrow();
    }

    /**
     * Talking opens the trade and tells the first of the story; trades win favour (Gray tech three times as much); at the
     * ally's line the sigil is given, once, and the ally's trades show for them and not for a stranger.
     */
    static void tradingWinsAnAlly(GameTestHelper h) {
        Kindled kindled = h.spawn(ModEntities.KINDLED.get(), new BlockPos(20, 1, 22));
        ServerPlayer player = TestPlayers.survival(h, new BlockPos(20, 1, 20));
        kindled.mobInteract(player, InteractionHand.MAIN_HAND);
        h.assertTrue(kindled.getTradingPlayer() == player, "talking didn't open the trade");
        h.assertValueEqual(KindledStanding.of(player).heard(), 1, "lines heard on meeting");
        h.assertFalse(kindled.getOffers().stream().anyMatch(Kindled::isAlliedOffer), "a stranger sees the ally's trades");
        kindled.notifyTrade(offerFor(kindled, ModItems.MAMMOTH_TUSK.get()));
        h.assertValueEqual(KindledStanding.favour(player), KindledStanding.TRADE, "favour for a plain trade");
        MerchantOffer fragments = offerFor(kindled, ModItems.GRAY_TECH_FRAGMENT.get());
        for (int i = 0; i < 4; i++) {
            kindled.notifyTrade(fragments);
        }
        h.assertTrue(KindledStanding.allied(player), "four fragment trades and a tusk should make an ally: " + KindledStanding.favour(player));
        h.assertValueEqual(player.getInventory().countItem(ModItems.KINDLED_SIGIL.get()), 1, "sigils given");
        // Back down and up again: no second sigil
        KindledStanding.add(player, -5);
        KindledStanding.add(player, 5);
        h.assertValueEqual(player.getInventory().countItem(ModItems.KINDLED_SIGIL.get()), 1, "sigils after crossing the line twice");
        kindled.setTradingPlayer(null);
        kindled.mobInteract(player, InteractionHand.MAIN_HAND);
        h.assertTrue(kindled.getOffers().stream().anyMatch(Kindled::isAlliedOffer), "the ally doesn't see their trades");
        kindled.setTradingPlayer(null);
        ServerPlayer stranger = TestPlayers.survival(h, new BlockPos(22, 1, 20));
        kindled.mobInteract(stranger, InteractionHand.MAIN_HAND);
        h.assertFalse(kindled.getOffers().stream().anyMatch(Kindled::isAlliedOffer), "the ally's trades still showing to a stranger");
        h.succeed();
    }

    /** A stranger hears the first three lines and then only greetings; a friend the next three; an ally the rest. */
    static void storyToldInOrder(GameTestHelper h) {
        Kindled kindled = h.spawn(ModEntities.KINDLED.get(), new BlockPos(20, 1, 22));
        ServerPlayer player = TestPlayers.survival(h, new BlockPos(20, 1, 20));
        for (int i = 0; i < 6; i++) {
            kindled.talk(player);
        }
        h.assertValueEqual(KindledStanding.of(player).heard(), 3, "lines a stranger heard");
        KindledStanding.add(player, KindledStanding.FRIEND);
        for (int i = 0; i < 6; i++) {
            kindled.talk(player);
        }
        h.assertValueEqual(KindledStanding.of(player).heard(), 6, "lines a friend heard");
        KindledStanding.add(player, KindledStanding.ALLIED);
        for (int i = 0; i < 6; i++) {
            kindled.talk(player);
        }
        h.assertValueEqual(KindledStanding.of(player).heard(), Kindled.LINES, "lines an ally heard");
        h.succeed();
    }

    /** Hurting a Kindled costs favour, rouses the guardians and ends the trading; the grudge eases with time. */
    static void wrongingTheKindled(GameTestHelper h) {
        Kindled kindled = h.spawn(ModEntities.KINDLED.get(), new BlockPos(20, 1, 22));
        CrystalGuardian guardian = h.spawn(ModEntities.CRYSTAL_GUARDIAN.get(), new BlockPos(14, 1, 22));
        ServerPlayer player = TestPlayers.survival(h, new BlockPos(20, 1, 20));
        kindled.hurtServer(h.getLevel(), h.getLevel().damageSources().playerAttack(player), 2.0F);
        h.assertValueEqual(KindledStanding.favour(player), KindledStanding.WRONG, "favour after a blow");
        h.assertTrue(KindledStanding.wronged(player), "not counted wronged");
        h.assertTrue(guardian.getTarget() == player, "the guardian wasn't roused");
        h.assertTrue(CrystalGuardian.guardsAgainst(player), "the guardians don't count the player an enemy");
        kindled.mobInteract(player, InteractionHand.MAIN_HAND);
        h.assertTrue(kindled.getTradingPlayer() == null, "it still trades with the player who hurt it");
        KindledStanding s = KindledStanding.of(player);
        player.setData(com.hollowfrontier.registry.ModAttachments.KINDLED_STANDING.get(),
                new KindledStanding(s.favour(), s.heard(), s.sigil(), h.getLevel().getGameTime() - KindledStanding.FORGIVE_TICKS));
        KindledStanding.tick(player);
        h.assertValueEqual(KindledStanding.favour(player), KindledStanding.WRONG + 1, "the grudge didn't ease");
        h.succeed();
    }

    /** A guardian leaves a stranger be, but not a stranger carrying the Grays' things; a friend may carry them. */
    static void guardianSmellsTheGrays(GameTestHelper h) {
        ServerPlayer clean = TestPlayers.survival(h, new BlockPos(20, 1, 16));
        ServerPlayer carrier = TestPlayers.survival(h, new BlockPos(20, 1, 26));
        carrier.getInventory().add(new ItemStack(ModItems.GRAY_TECH_FRAGMENT.get()));
        ServerPlayer friend = TestPlayers.survival(h, new BlockPos(24, 1, 20));
        friend.getInventory().add(new ItemStack(ModItems.GRAY_TECH_FRAGMENT.get()));
        KindledStanding.add(friend, KindledStanding.FRIEND);
        h.assertFalse(CrystalGuardian.guardsAgainst(clean), "a clean stranger counted an enemy");
        h.assertTrue(CrystalGuardian.guardsAgainst(carrier), "a stranger with a fragment let by");
        h.assertFalse(CrystalGuardian.guardsAgainst(friend), "a friend with a fragment counted an enemy");
        CrystalGuardian guardian = h.spawn(ModEntities.CRYSTAL_GUARDIAN.get(), new BlockPos(20, 1, 20));
        h.succeedWhen(() -> h.assertTrue(guardian.getTarget() == carrier, "target " + guardian.getTarget()));
    }

    /** Its first shot at a stranger with Gray things goes wide; the next one doesn't. One who's wronged them gets no warning. */
    static void guardianWarnsFirst(GameTestHelper h) {
        h.getLevel().getServer().setDifficulty(Difficulty.NORMAL, true); // peaceful would spare the players
        CrystalGuardian guardian = h.spawn(ModEntities.CRYSTAL_GUARDIAN.get(), new BlockPos(20, 1, 20));
        guardian.setNoAi(true); // it fires only when told to here
        ServerPlayer carrier = TestPlayers.survival(h, new BlockPos(20, 1, 26));
        carrier.getInventory().add(new ItemStack(ModItems.GRAY_TECH_FRAGMENT.get()));
        ServerPlayer wrongdoer = TestPlayers.survival(h, new BlockPos(14, 1, 20));
        KindledStanding.add(wrongdoer, KindledStanding.WRONG);
        // A new player can't be hurt until the server counts its client as loaded, a few seconds on
        h.runAfterDelay(80, () -> {
            CrystalGuardian.fire(h.getLevel(), guardian, carrier);
            h.assertTrue(guardian.hasWarned(carrier) && carrier.getHealth() == carrier.getMaxHealth(), "the first shot wasn't a warning");
            CrystalGuardian.fire(h.getLevel(), guardian, carrier);
            h.assertTrue(carrier.getHealth() < carrier.getMaxHealth(), "the second shot did no harm");
            CrystalGuardian.fire(h.getLevel(), guardian, wrongdoer);
            h.assertTrue(wrongdoer.getHealth() < wrongdoer.getMaxHealth() && !guardian.hasWarned(wrongdoer), "a wrongdoer was warned");
            h.succeed();
        });
    }

    /** A guardian beams a Gray probe in sight. */
    static void guardianBeamsAProbe(GameTestHelper h) {
        CrystalGuardian guardian = h.spawn(ModEntities.CRYSTAL_GUARDIAN.get(), new BlockPos(20, 1, 20));
        GrayProbe probe = h.spawn(ModEntities.GRAY_PROBE.get(), new BlockPos(20, 3, 28));
        probe.setNoAi(true);
        probe.setNoGravity(true);
        h.succeedWhen(() -> h.assertTrue(!probe.isAlive() || probe.getHealth() < probe.getMaxHealth(),
                "the probe wasn't hit (target " + guardian.getTarget() + ", charging " + guardian.isCharging() + ")"));
    }

    /** Held, the sigil makes outlaws glow, and leaves townsfolk be. */
    static void sigilShowsTheImplanted(GameTestHelper h) {
        ServerPlayer player = TestPlayers.survival(h, new BlockPos(20, 1, 20));
        player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(ModItems.KINDLED_SIGIL.get()));
        Outlaw outlaw = h.spawn(ModEntities.OUTLAW.get(), new BlockPos(30, 1, 20));
        outlaw.setNoAi(true);
        Townsfolk folk = h.spawn(ModEntities.TOWNSFOLK.get(), new BlockPos(10, 1, 20));
        folk.setNoAi(true);
        // A test player isn't ticked by a connection, so its held item's tick is run here (in the pocket, then in hand)
        new ItemStack(ModItems.KINDLED_SIGIL.get()).inventoryTick(h.getLevel(), player, null);
        h.assertFalse(outlaw.hasEffect(MobEffects.GLOWING), "a sigil in the pocket lit the outlaw");
        player.getMainHandItem().inventoryTick(h.getLevel(), player, EquipmentSlot.MAINHAND);
        h.assertTrue(outlaw.hasEffect(MobEffects.GLOWING), "the outlaw isn't glowing");
        h.assertFalse(folk.hasEffect(MobEffects.GLOWING), "the townsfolk glows");
        h.assertValueEqual(KindledSigilItem.reveal(h.getLevel(), player), 1, "implanted found");
        h.succeed();
    }

    // ---------------------------------------------------------------- 12b: the Sun Archon

    /** The temple: the Archon before its throne, four pylons on the arena, the hoard. */
    static void sunTemplePlaces(GameTestHelper h) {
        StructureTemplate temple = h.getLevel().getStructureTemplateManager().get(HollowFrontier.id(ConcaviaBlueprints.SUN_TEMPLE)).orElseThrow();
        BlockPos origin = h.absolutePos(BlockPos.ZERO);
        temple.placeInWorld(h.getLevel(), origin, origin, new StructurePlaceSettings().setFinalizeEntities(true), h.getLevel().getRandom(), Block.UPDATE_CLIENTS);
        List<Entity> placed = h.getLevel().getEntities((Entity) null, new AABB(origin).expandTowards(46, 32, 64), e -> !(e instanceof ServerPlayer));
        try {
            List<SunArchon> archons = placed.stream().filter(e -> e instanceof SunArchon).map(e -> (SunArchon) e).toList();
            h.assertValueEqual(archons.size(), 1, "Archons");
            SunArchon archon = archons.getFirst();
            h.assertFalse(archon.isAwake(), "the Archon should wait on its throne");
            h.assertTrue(archon.throne() != null, "the Archon doesn't know its throne");
            h.assertValueEqual(count(temple, ModBlocks.SUN_PYLON.get()), 4, "pylons");
            h.assertValueEqual(LoreSiteTests.chestsRolling(temple, ModChestLoot.SUN_TEMPLE), 0L, "the hoard by the throne (26: moved down to the undercroft)");
            h.assertValueEqual(LoreSiteTests.chestsRolling(temple, ModChestLoot.SUN_TEMPLE_OFFERING), 1L, "the throne hall's offering chest");
            h.assertTrue(count(temple, Blocks.HONEYCOMB_BLOCK) >= 6 && count(temple, Blocks.RESIN_BRICKS) >= 60, "the priests' discs, the resin banding");
            h.assertTrue(archon.getY() - origin.getY() > ConcaviaBlueprints.ARENA_Y, "the Archon isn't up on the arena");
        } finally {
            placed.forEach(Entity::discard);
        }
        h.succeed();
    }

    private static SunArchon archon(GameTestHelper h, BlockPos at) {
        SunArchon archon = h.spawn(ModEntities.SUN_ARCHON.get(), at);
        archon.setThrone(h.absolutePos(at));
        return archon;
    }

    /** A player coming near wakes it, and its boss bar shows to them. */
    static void archonWakes(GameTestHelper h) {
        SunArchon archon = archon(h, new BlockPos(20, 1, 20));
        h.assertFalse(archon.isAwake(), "it woke with no one near");
        ServerPlayer player = TestPlayers.survival(h, new BlockPos(20, 1, 14));
        h.succeedWhen(() -> {
            h.assertTrue(archon.isAwake() && archon.phase() == SunArchon.KING, "it didn't wake: phase " + archon.phase());
            h.assertTrue(archon.bossEvent().getPlayers().contains(player), "no boss bar for the player");
            TestPlayers.leave(player); // gone once it passes, so it can't keep archon_resets' Archon awake
        });
    }

    /**
     * Its phases go by its health; in the second, a standing pylon shields it (blows turned aside, it hovers), and with the
     * pylon broken the shield fails; at a third of its health, the eclipse.
     */
    static void archonPhasesAndShield(GameTestHelper h) {
        SunArchon archon = archon(h, new BlockPos(20, 1, 20));
        BlockPos pylon = new BlockPos(26, 1, 20);
        h.setBlock(pylon, ModBlocks.SUN_PYLON.get());
        archon.addPylon(h.absolutePos(pylon));
        archon.wake(null);
        archon.setHealth(archon.getMaxHealth() * 0.6F);
        h.runAfterDelay(2, () -> {
            h.assertTrue(archon.phase() == SunArchon.CORONA && archon.isShielded(), "no shielded corona: phase " + archon.phase());
            float before = archon.getHealth();
            archon.hurtServer(h.getLevel(), h.getLevel().damageSources().magic(), 20.0F);
            h.assertValueEqual(archon.getHealth(), before, "health through the shield");
            h.setBlock(pylon, Blocks.AIR);
        });
        h.runAfterDelay(5, () -> {
            h.assertFalse(archon.isShielded(), "the shield didn't fail with the pylon gone");
            archon.setHealth(archon.getMaxHealth() * 0.3F);
        });
        h.runAfterDelay(8, () -> {
            h.assertValueEqual(archon.phase(), SunArchon.ECLIPSE, "phase at a third");
            h.succeed();
        });
    }

    /** With no one left to fight, it goes back to its throne, healed, and the broken pylons are whole again. */
    static void archonResets(GameTestHelper h) {
        SunArchon archon = archon(h, new BlockPos(20, 1, 20));
        BlockPos pylon = new BlockPos(26, 1, 20);
        archon.addPylon(h.absolutePos(pylon));
        archon.wake(null);
        archon.setHealth(100.0F);
        archon.teleportTo(archon.getX() + 6, archon.getY(), archon.getZ());
        h.succeedWhen(() -> {
            h.assertFalse(archon.isAwake(), "still awake; players near: " + playersNear(h, archon));
            h.assertValueEqual(archon.getHealth(), archon.getMaxHealth(), "health after the reset");
            h.assertTrue(archon.blockPosition().equals(archon.throne()), "not back on its throne: " + archon.blockPosition());
            h.assertTrue(h.getBlockState(pylon).is(ModBlocks.SUN_PYLON.get()), "the pylon wasn't made whole");
            h.assertFalse(archon.bossEvent().isVisible(), "the boss bar still shows");
        });
    }

    // Who could be keeping a boss awake (a fake player another test left standing nearby, say)
    private static List<String> playersNear(GameTestHelper h, SunArchon archon) {
        return h.getLevel().getEntitiesOfClass(net.minecraft.world.entity.player.Player.class, archon.getBoundingBox().inflate(SunArchon.BOSS_RANGE))
                .stream().map(p -> p.getGameProfile().name() + "@" + p.blockPosition() + " " + p.gameMode()).toList();
    }

    /** Its sunburst burns and throws what's near it. */
    static void archonSunburst(GameTestHelper h) {
        SunArchon archon = archon(h, new BlockPos(20, 1, 20));
        archon.setNoAi(true);
        net.minecraft.world.entity.animal.cow.Cow cow = h.spawn(net.minecraft.world.entity.EntityTypes.COW, new BlockPos(23, 1, 20));
        h.assertValueEqual(archon.burst(h.getLevel()), 1, "caught by the burst");
        h.assertTrue(cow.getHealth() < cow.getMaxHealth() && cow.isOnFire(), "the cow wasn't burned");
        h.succeed();
    }

    /** It drops the Phlogiston Core, the crown and sunstone; the Kindled thank whoever stood against it. */
    static void archonFalls(GameTestHelper h) {
        SunArchon archon = archon(h, new BlockPos(20, 1, 20));
        ServerPlayer player = TestPlayers.survival(h, new BlockPos(20, 1, 30));
        archon.kill(h.getLevel());
        h.assertValueEqual(KindledStanding.favour(player), SunArchon.KINDLED_THANKS, "the Kindled's thanks");
        h.succeedWhen(() -> {
            List<net.minecraft.world.entity.item.ItemEntity> drops = h.getLevel().getEntitiesOfClass(net.minecraft.world.entity.item.ItemEntity.class,
                    new AABB(h.absolutePos(new BlockPos(20, 1, 20))).inflate(6));
            h.assertTrue(drops.stream().anyMatch(d -> d.getItem().is(ModItems.PHLOGISTON_CORE.get())), "no Phlogiston Core");
            h.assertTrue(drops.stream().anyMatch(d -> d.getItem().is(ModItems.STARSTEEL_CROWN.get())), "no crown");
            h.assertTrue(drops.stream().anyMatch(d -> d.getItem().is(ModItems.SUNSTONE_SHARD.get())), "no sunstone");
            TestPlayers.leave(player);
        });
    }

    /** The crown's wearer can't be withered, and takes half a phlogiston beam. */
    static void crownWards(GameTestHelper h) {
        Townsfolk crowned = h.spawn(ModEntities.TOWNSFOLK.get(), new BlockPos(16, 1, 20));
        Townsfolk bare = h.spawn(ModEntities.TOWNSFOLK.get(), new BlockPos(24, 1, 20));
        crowned.setNoAi(true);
        bare.setNoAi(true);
        crowned.setItemSlot(EquipmentSlot.HEAD, new ItemStack(ModItems.STARSTEEL_CROWN.get()));
        bare.setItemSlot(EquipmentSlot.HEAD, new ItemStack(ModItems.STARSTEEL_HELMET.get()));
        crowned.addEffect(new net.minecraft.world.effect.MobEffectInstance(MobEffects.WITHER, 100));
        bare.addEffect(new net.minecraft.world.effect.MobEffectInstance(MobEffects.WITHER, 100));
        h.assertFalse(crowned.hasEffect(MobEffects.WITHER), "the crowned one was withered");
        h.assertTrue(bare.hasEffect(MobEffects.WITHER), "wither didn't take on the bare one");
        bare.removeAllEffects();
        CrystalGuardian guardian = h.spawn(ModEntities.CRYSTAL_GUARDIAN.get(), new BlockPos(20, 1, 26));
        guardian.setNoAi(true);
        crowned.hurtServer(h.getLevel(), com.hollowfrontier.registry.ModDamageTypes.phlogistonBeam(guardian), 8.0F);
        bare.hurtServer(h.getLevel(), com.hollowfrontier.registry.ModDamageTypes.phlogistonBeam(guardian), 8.0F);
        float crownedLoss = crowned.getMaxHealth() - crowned.getHealth(), bareLoss = bare.getMaxHealth() - bare.getHealth();
        h.assertTrue(crownedLoss > 0 && crownedLoss < bareLoss * 0.75F, "the crown didn't halve the beam: " + crownedLoss + " vs " + bareLoss);
        h.succeed();
    }

    /** Held, the lens gives night vision; used, its beam burns a zombie (twice as hard: it's undead). */
    static void lensShines(GameTestHelper h) {
        ServerPlayer player = TestPlayers.survival(h, new BlockPos(20, 1, 14));
        player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(ModItems.SUNSTONE_LENS.get()));
        player.getMainHandItem().inventoryTick(h.getLevel(), player, EquipmentSlot.MAINHAND);
        h.assertTrue(player.hasEffect(MobEffects.NIGHT_VISION), "no night vision from the lens");
        net.minecraft.world.entity.monster.zombie.Zombie zombie = h.spawn(net.minecraft.world.entity.EntityTypes.ZOMBIE, new BlockPos(20, 1, 22));
        zombie.setNoAi(true);
        player.lookAt(net.minecraft.commands.arguments.EntityAnchorArgument.Anchor.EYES, zombie.position().add(0, 1.0, 0)); // whichever way the plot is turned
        h.assertTrue(com.hollowfrontier.content.concavia.SunstoneLensItem.shine(h.getLevel(), player) == zombie, "the beam missed the zombie");
        h.assertTrue(zombie.getMaxHealth() - zombie.getHealth() > com.hollowfrontier.content.concavia.SunstoneLensItem.DAMAGE && zombie.isOnFire(),
                "the zombie wasn't burned hard: " + zombie.getHealth());
        // Used, it marks the beam on the stack, which flares the glass of its held model
        player.getMainHandItem().use(h.getLevel(), player, InteractionHand.MAIN_HAND);
        h.assertTrue(player.getMainHandItem().has(com.hollowfrontier.registry.ModDataComponents.LAST_FIRED.get()), "using it didn't mark the beam");
        TestPlayers.leave(player);
        h.succeed();
    }

    /** The Codex: every milestone it walks through is a real advancement, and it writes a page or more a chapter. */
    static void codexWrites(GameTestHelper h) {
        ServerPlayer player = TestPlayers.survival(h, new BlockPos(20, 1, 20));
        for (var chapter : com.hollowfrontier.content.concavia.ConcavianCodexItem.CHAPTERS) {
            for (String step : chapter.steps()) {
                h.assertTrue(h.getLevel().getServer().getAdvancements().get(HollowFrontier.id(step)) != null, "the Codex names no advancement " + step);
            }
        }
        var book = com.hollowfrontier.content.concavia.ConcavianCodexItem.write(player);
        h.assertTrue(book.pages().size() > com.hollowfrontier.content.concavia.ConcavianCodexItem.CHAPTERS.size(), "too few pages: " + book.pages().size());
        h.assertFalse(com.hollowfrontier.content.concavia.ConcavianCodexItem.reached(player, "concavia/eclipse"), "a new player has beaten the Archon");
        TestPlayers.leave(player);
        h.succeed();
    }
}
