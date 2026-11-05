package com.hollowfrontier.gametest;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import com.hollowfrontier.HollowFrontier;
import com.hollowfrontier.content.frontier.mystery.MysteryLog;
import com.hollowfrontier.content.frontier.mystery.Suspicion;
import com.hollowfrontier.content.ufo.Abductions;
import com.hollowfrontier.content.frontier.talk.Context;
import com.hollowfrontier.content.frontier.talk.Fact;
import com.hollowfrontier.content.ufo.GrayDrone;
import com.hollowfrontier.content.ufo.GrayLandings;
import com.hollowfrontier.content.ufo.GraySaucer;
import com.hollowfrontier.content.ufo.GrayWarden;
import com.hollowfrontier.content.ufo.PrototypeSaucer;
import com.hollowfrontier.content.ufo.PrototypeSaucerItem;
import com.hollowfrontier.content.ufo.SaucerFittings;
import com.hollowfrontier.content.ufo.PhlogistonCellItem;
import com.hollowfrontier.content.ufo.PhlogistonGunItem;
import com.hollowfrontier.registry.ModBlocks;
import com.hollowfrontier.registry.ModEntities;
import com.hollowfrontier.registry.ModItems;
import com.hollowfrontier.worldgen.structure.UfoBlueprints;

import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.Difficulty;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.animal.cow.Cow;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Input;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructurePlaceSettings;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplate;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

/**
 * Phase 13a: the Grays arrive. A flyover logged as a sighting; a saucer's beam drawing up an animal and taking it; the
 * beam passing over a sneaking player and losing one who starts sneaking; missing time; the Warden's ray and its blink;
 * the crash site and the salvage in its wreckage. On the 40-block field (the crash site on the 48-block yard).
 */
final class UfoTests {
    private UfoTests() {}

    private static GraySaucer beamingSaucer(GameTestHelper h, BlockPos under) {
        GraySaucer saucer = ModEntities.GRAY_SAUCER.get().create(h.getLevel(), net.minecraft.world.entity.EntitySpawnReason.EVENT);
        Vec3 at = Vec3.atBottomCenterOf(h.absolutePos(under)).add(0, GraySaucer.BEAM_HEIGHT, 0);
        saucer.setPos(at);
        h.getLevel().addFreshEntity(saucer);
        saucer.beamHere();
        return saucer;
    }

    /** A flyover crosses the sky, and it's logged as a sighting (so the men in black come round). */
    static void flyoverIsASighting(GameTestHelper h) {
        Vec3 over = Vec3.atCenterOf(h.absolutePos(new BlockPos(20, 1, 20)));
        long before = h.getLevel().getGameTime();
        GraySaucer saucer = Abductions.flyover(h.getLevel(), over, h.getLevel().getRandom());
        try {
            h.assertTrue(saucer.run() == GraySaucer.Run.CROSS, "it isn't crossing: " + saucer.run());
            h.assertTrue(MysteryLog.get(h.getLevel()).latest(MysteryLog.Kind.SIGHTING, BlockPos.containing(over), 64, before, 10).isPresent(),
                    "no sighting logged");
        } finally {
            saucer.discard();
        }
        h.succeed();
    }

    /** Its beam draws a cow up into it, and the cow is gone. */
    static void beamTakesAnAnimal(GameTestHelper h) {
        Cow cow = h.spawn(EntityTypes.COW, new BlockPos(20, 1, 20));
        GraySaucer saucer = beamingSaucer(h, new BlockPos(20, 1, 20));
        h.succeedWhen(() -> {
            h.assertTrue(saucer.taken() == cow && cow.isRemoved(), "the cow wasn't taken (caught " + saucer.caught() + ", y " + cow.getY() + ")");
            saucer.discard();
        });
    }

    /** The beam passes over a player keeping low, takes hold of one who isn't, and lets them go when they duck. */
    static void beamAndTheSneakingPlayer(GameTestHelper h) {
        ServerPlayer low = TestPlayers.survival(h, new BlockPos(20, 1, 20));
        low.setShiftKeyDown(true);
        ServerPlayer upright = TestPlayers.survival(h, new BlockPos(21, 1, 21));
        GraySaucer saucer = beamingSaucer(h, new BlockPos(20, 1, 20));
        h.runAfterDelay(5, () -> {
            h.assertTrue(saucer.caught() == upright, "the beam caught " + saucer.caught() + ", not the upright player");
            upright.setShiftKeyDown(true);
        });
        h.runAfterDelay(10, () -> {
            h.assertTrue(saucer.caught() == null, "the beam kept hold of a ducking player");
            h.assertTrue(upright.hasEffect(MobEffects.SLOW_FALLING), "no gentle letting down");
            saucer.discard();
            h.succeed();
        });
    }

    /** A taken player comes to far off, dazed, and the men in black mind them more. */
    static void missingTime(GameTestHelper h) {
        ServerPlayer player = TestPlayers.survival(h, new BlockPos(20, 1, 20));
        Vec3 from = player.position();
        Abductions.missingTime(h.getLevel(), player);
        double moved = Math.hypot(player.getX() - from.x, player.getZ() - from.z);
        h.assertTrue(moved >= Abductions.MISSING_MIN - 1 && moved <= Abductions.MISSING_MAX + 1, "set down " + moved + " away");
        h.assertTrue(player.hasEffect(MobEffects.BLINDNESS) && player.hasEffect(MobEffects.NAUSEA), "not dazed");
        h.assertValueEqual(Suspicion.level(player), Abductions.SUSPICION, "suspicion after being taken");
        h.succeed();
    }

    /** A Warden draws on a player and its ray burns them. */
    static void wardenFires(GameTestHelper h) {
        h.getLevel().getServer().setDifficulty(Difficulty.NORMAL, true);
        GrayWarden warden = h.spawn(ModEntities.GRAY_WARDEN.get(), new BlockPos(20, 1, 20));
        ServerPlayer player = TestPlayers.survival(h, new BlockPos(20, 1, 30));
        warden.setTarget(player);
        h.succeedWhen(() -> h.assertTrue(player.getHealth() < player.getMaxHealth(), "the ray hasn't hit (aiming " + warden.isAiming() + ")"));
    }

    /** Hurt, a Warden can blink a few blocks off. */
    static void wardenBlinks(GameTestHelper h) {
        GrayWarden warden = h.spawn(ModEntities.GRAY_WARDEN.get(), new BlockPos(20, 1, 20));
        Vec3 from = warden.position();
        h.assertTrue(warden.blink(), "it found nowhere to blink to");
        h.assertTrue(warden.position().distanceTo(from) >= 3.0, "it only moved " + warden.position().distanceTo(from));
        h.succeed();
    }

    /** The crash site: the saucer's hull and lights, five pieces of wreckage, two Wardens. */
    static void crashSitePlaces(GameTestHelper h) {
        StructureTemplate site = h.getLevel().getStructureTemplateManager().get(HollowFrontier.id(UfoBlueprints.CRASH_SITE)).orElseThrow();
        BlockPos origin = h.absolutePos(BlockPos.ZERO);
        site.placeInWorld(h.getLevel(), origin, origin, new StructurePlaceSettings(), h.getLevel().getRandom(), Block.UPDATE_CLIENTS);
        List<Entity> placed = h.getLevel().getEntities((Entity) null, new AABB(origin).expandTowards(46, 20, 46), e -> !(e instanceof ServerPlayer));
        try {
            h.assertValueEqual(placed.stream().filter(e -> e instanceof GrayWarden).count(), 2L, "Wardens");
            h.assertValueEqual(count(site, ModBlocks.SAUCER_WRECKAGE.get()), 5, "wreckage");
            h.assertTrue(count(site, ModBlocks.SAUCER_HULL.get()) >= 100, "too little hull: " + count(site, ModBlocks.SAUCER_HULL.get()));
            h.assertTrue(count(site, ModBlocks.PHLOGISTON_LAMP.get()) >= 4, "the rim's lights");
            h.assertTrue(count(site, Blocks.TINTED_GLASS) >= 6, "the dome");
        } finally {
            placed.forEach(Entity::discard);
        }
        h.succeed();
    }

    private static int count(StructureTemplate template, Block block) {
        return template.filterBlocks(BlockPos.ZERO, new StructurePlaceSettings(), block).size();
    }

    /** Broken, wreckage always gives plating, and across a few wrecks' worth every working part turns up. */
    static void wreckageGivesSalvage(GameTestHelper h) {
        BlockPos at = new BlockPos(20, 1, 20);
        for (int i = 0; i < 80; i++) {
            h.setBlock(at, ModBlocks.SAUCER_WRECKAGE.get());
            h.getLevel().destroyBlock(h.absolutePos(at), true);
        }
        Set<Item> found = h.getLevel().getEntitiesOfClass(ItemEntity.class, new AABB(h.absolutePos(at)).inflate(4)).stream()
                .map(e -> e.getItem().getItem()).collect(Collectors.toSet());
        for (Item part : List.of(ModItems.ALIEN_ALLOY_PLATING.get(), ModItems.DAMAGED_GRAVITY_DRIVE.get(), ModItems.BEAM_EMITTER.get(),
                ModItems.LENS_ARRAY.get(), ModItems.NAVIGATION_CORE.get())) {
            h.assertTrue(found.contains(part), "no " + part + " in eighty wreckage blocks");
        }
        h.succeed();
    }

    // ---------------------------------------------------------------- 13b: phlogiston tech

    private static net.minecraft.world.entity.monster.zombie.Zombie dummy(GameTestHelper h, BlockPos at) {
        net.minecraft.world.entity.monster.zombie.Zombie zombie = h.spawn(EntityTypes.ZOMBIE, at);
        zombie.setNoAi(true);
        return zombie;
    }

    /** The Ray Pistol burns what it's aimed at and draws a charge from a cell; with no charge it won't fire. */
    static void rayPistolFires(GameTestHelper h) {
        ServerPlayer player = TestPlayers.survival(h, new BlockPos(20, 1, 14));
        ItemStack pistol = new ItemStack(ModItems.RAY_PISTOL.get());
        player.setItemInHand(InteractionHand.MAIN_HAND, pistol);
        ItemStack cell = new ItemStack(ModItems.PHLOGISTON_CELL.get());
        player.getInventory().add(cell);
        var zombie = dummy(h, new BlockPos(20, 1, 22));
        player.lookAt(net.minecraft.commands.arguments.EntityAnchorArgument.Anchor.EYES, zombie.position().add(0, 1.0, 0));
        PhlogistonGunItem gun = ModItems.RAY_PISTOL.get();
        h.assertTrue(gun.shoot(h.getLevel(), player, pistol, InteractionHand.MAIN_HAND), "it didn't fire with a full cell");
        h.assertTrue(zombie.getHealth() < zombie.getMaxHealth(), "the beam missed");
        h.assertValueEqual(PhlogistonCellItem.charges(player), PhlogistonCellItem.CHARGES - gun.cost(), "charge left");
        player.getInventory().clearContent();
        player.setItemInHand(InteractionHand.MAIN_HAND, pistol);
        h.assertFalse(gun.shoot(h.getLevel(), player, pistol, InteractionHand.MAIN_HAND), "it fired with no cell");
        h.succeed();
    }

    /** The Phlogiston Rifle's beam goes through three in a line, and no more. */
    static void phlogistonRiflePierces(GameTestHelper h) {
        ServerPlayer player = TestPlayers.survival(h, new BlockPos(20, 1, 6));
        List<net.minecraft.world.entity.monster.zombie.Zombie> row = List.of(dummy(h, new BlockPos(20, 1, 12)), dummy(h, new BlockPos(20, 1, 16)),
                dummy(h, new BlockPos(20, 1, 20)), dummy(h, new BlockPos(20, 1, 24)));
        player.lookAt(net.minecraft.commands.arguments.EntityAnchorArgument.Anchor.EYES, row.get(3).position().add(0, 1.0, 0));
        PhlogistonGunItem rifle = ModItems.PHLOGISTON_RIFLE.get();
        List<LivingEntity> struck = rifle.beam(h.getLevel(), player);
        h.assertValueEqual(struck.size(), rifle.pierce(), "creatures the beam went through");
        h.assertTrue(struck.containsAll(row.subList(0, 3)) && !struck.contains(row.get(3)), "it struck the wrong ones");
        h.assertTrue(row.get(3).getHealth() == row.get(3).getMaxHealth(), "the fourth was hurt");
        h.succeed();
    }

    /** A beam sets what it harms alight (33), the rifle's for longer. */
    static void beamsSetAlight(GameTestHelper h) {
        ServerPlayer player = TestPlayers.survival(h, new BlockPos(20, 1, 6));
        var zombie = dummy(h, new BlockPos(20, 1, 12));
        var golem = h.spawnWithNoFreeWill(EntityTypes.IRON_GOLEM, new BlockPos(28, 1, 6));
        player.lookAt(net.minecraft.commands.arguments.EntityAnchorArgument.Anchor.EYES, zombie.getBoundingBox().getCenter());
        ModItems.RAY_PISTOL.get().beam(h.getLevel(), player);
        player.lookAt(net.minecraft.commands.arguments.EntityAnchorArgument.Anchor.EYES, golem.getBoundingBox().getCenter());
        ModItems.PHLOGISTON_RIFLE.get().beam(h.getLevel(), player);
        h.assertTrue(zombie.getRemainingFireTicks() > 0, "the ray pistol didn't set it alight");
        h.assertTrue(golem.getRemainingFireTicks() > zombie.getRemainingFireTicks(), "the rifle's fire isn't the longer: "
                + golem.getRemainingFireTicks() + " against " + zombie.getRemainingFireTicks());
        h.succeed();
    }

    /** A beam fired from the saddle goes through your own horse, which would otherwise take the shot and the fire (33). */
    static void beamsSpareTheShootersMount(GameTestHelper h) {
        ServerPlayer player = TestPlayers.survival(h, new BlockPos(20, 1, 14));
        var horse = h.spawnWithNoFreeWill(EntityTypes.HORSE, new BlockPos(20, 1, 14));
        player.startRiding(horse, true, false);
        horse.positionRider(player);
        player.setXRot(90.0F);
        Vec3 eye = player.getEyePosition();
        h.assertTrue(horse.getBoundingBox().inflate(0.3).clip(eye, eye.add(0, -3.0, 0)).isPresent(), "the beam wouldn't have crossed the horse");
        List<LivingEntity> struck = ModItems.RAY_PISTOL.get().beam(h.getLevel(), player);
        h.assertTrue(struck.isEmpty(), "the beam struck " + struck);
        h.assertTrue(horse.getHealth() == horse.getMaxHealth() && horse.getRemainingFireTicks() <= 0, "the horse was hurt");
        h.succeed();
    }

    /** A cell drawn dry turns into an empty one; a draw larger than what's left takes nothing. */
    static void cellsDrainAndEmpty(GameTestHelper h) {
        ServerPlayer player = TestPlayers.survival(h, new BlockPos(20, 1, 20));
        ItemStack cell = new ItemStack(ModItems.PHLOGISTON_CELL.get());
        cell.setDamageValue(PhlogistonCellItem.CHARGES - 2);
        player.getInventory().setItem(0, cell);
        h.assertFalse(PhlogistonCellItem.draw(player, 3), "drew three from a cell with two");
        h.assertValueEqual(PhlogistonCellItem.charges(player), 2, "charge after a failed draw");
        h.assertTrue(PhlogistonCellItem.draw(player, 2), "couldn't draw the last two");
        h.assertTrue(player.getInventory().getItem(0).is(ModItems.EMPTY_PHLOGISTON_CELL.get()), "the drained cell didn't empty");
        for (String recipe : List.of("phlogiston_cell", "empty_phlogiston_cell", "ray_pistol", "phlogiston_rifle", "gravity_drive")) {
            var key = net.minecraft.resources.ResourceKey.create(net.minecraft.core.registries.Registries.RECIPE, HollowFrontier.id(recipe));
            h.assertTrue(h.getLevel().getServer().getRecipeManager().byKey(key).isPresent(), "no recipe " + recipe);
        }
        h.succeed();
    }

    // ---------------------------------------------------------------- 13c: the Prototype Saucer

    /** A saucer set down from its item at {@code at} (it must have room), with that item's fittings and charge. */
    private static PrototypeSaucer placed(GameTestHelper h, BlockPos at, ItemStack item) {
        PrototypeSaucer saucer = PrototypeSaucerItem.place(h.getLevel(), h.absolutePos(at), 0.0F, item);
        h.assertTrue(saucer != null, "no room to set the saucer down");
        h.getLevel().addFreshEntity(saucer);
        return saucer;
    }

    private static PrototypeSaucer saucer(GameTestHelper h, BlockPos at, boolean emitter, boolean lens, int charge) {
        return placed(h, at, PrototypeSaucerItem.of(new SaucerFittings(emitter, lens, false), charge));
    }

    private static Input keys(boolean forward, boolean jump) {
        return new Input(forward, false, false, false, jump, false, false);
    }

    /**
     * A crafted saucer sets down primed with a cell's worth of phlogiston and with its beam and laser; a navigation core used on
     * it fits it (once); landed and empty it folds back into its item, fittings and charge kept, but not while crewed.
     */
    static void saucerPlacesFitsAndFolds(GameTestHelper h) {
        var saucer = placed(h, new BlockPos(20, 1, 20), new ItemStack(ModItems.PROTOTYPE_SAUCER.get()));
        h.assertValueEqual(saucer.charge(), PrototypeSaucerItem.PRIMED_CHARGE, "charge of a new saucer");
        h.assertValueEqual(saucer.fittingsMask(), SaucerFittings.EMITTER | SaucerFittings.LENS, "a new saucer's fittings");
        ServerPlayer player = TestPlayers.survival(h, new BlockPos(16, 1, 20));
        player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(ModItems.NAVIGATION_CORE.get(), 2));
        saucer.interact(player, InteractionHand.MAIN_HAND, saucer.position());
        h.assertTrue(saucer.fittings().nav(), "the navigation core wasn't fitted");
        h.assertValueEqual(player.getMainHandItem().getCount(), 1, "cores left after fitting one");
        saucer.interact(player, InteractionHand.MAIN_HAND, saucer.position());
        h.assertValueEqual(player.getMainHandItem().getCount(), 1, "cores left after fitting a second");
        player.setItemInHand(InteractionHand.MAIN_HAND, ItemStack.EMPTY);
        h.runAfterDelay(5, () -> {
            ServerPlayer rider = TestPlayers.survival(h, new BlockPos(24, 1, 20));
            rider.startRiding(saucer, true, true);
            h.assertValueEqual(saucer.fold(player), "fold_crewed", "folding a crewed saucer");
            rider.stopRiding();
            saucer.setCharge(40);
            h.assertTrue(saucer.fold(player) == null, "it didn't fold");
            h.assertTrue(saucer.isRemoved(), "the saucer is still about");
            ItemStack folded = ItemStack.EMPTY;
            for (int slot = 0; slot < player.getInventory().getContainerSize(); slot++) {
                if (player.getInventory().getItem(slot).is(ModItems.PROTOTYPE_SAUCER.get())) folded = player.getInventory().getItem(slot);
            }
            h.assertTrue(PrototypeSaucerItem.fittings(folded).nav() && PrototypeSaucerItem.charge(folded) == 40,
                    "the item lost its fittings or charge");
            h.succeed();
        });
    }

    /** With a real pilot's keys (no standing helm): jump lifts it off the ground, then W drives it forward. */
    static void saucerFliesWithPilot(GameTestHelper h) {
        var saucer = saucer(h, new BlockPos(20, 1, 20), false, false, 64);
        ServerPlayer pilot = TestPlayers.survival(h, new BlockPos(17, 1, 20));
        pilot.startRiding(saucer, true, true);
        pilot.setLastClientInput(keys(false, true));
        double y0 = saucer.getY();
        Vec3[] aloft = new Vec3[1];
        h.runAfterDelay(20, () -> {
            h.assertTrue(saucer.getY() > y0 + 3, "it didn't lift off: " + (saucer.getY() - y0));
            aloft[0] = saucer.position();
            pilot.setLastClientInput(keys(true, false));
        });
        h.runAfterDelay(35, () -> {
            double run = saucer.position().subtract(aloft[0]).horizontalDistance();
            h.assertTrue(run > 6, "it didn't drive forward: " + run);
            h.assertTrue(Math.abs(saucer.getY() - aloft[0].y) < 1, "it didn't hold its height with no lift keys");
            pilot.stopRiding();
            saucer.discard();
            h.succeed();
        });
    }

    /** Run dry, it draws a cell's worth from its pilot's Phlogiston Cells and flies on; with nothing to draw it stays down. */
    static void saucerDrawsPilotsCells(GameTestHelper h) {
        var saucer = saucer(h, new BlockPos(12, 1, 20), false, false, 0);
        var stranded = saucer(h, new BlockPos(28, 1, 20), false, false, 0);
        ServerPlayer pilot = TestPlayers.survival(h, new BlockPos(9, 1, 20));
        ServerPlayer castaway = TestPlayers.survival(h, new BlockPos(31, 1, 20));
        pilot.getInventory().add(new ItemStack(ModItems.PHLOGISTON_CELL.get()));
        pilot.startRiding(saucer, true, true);
        castaway.startRiding(stranded, true, true);
        pilot.setLastClientInput(keys(false, true));
        castaway.setLastClientInput(keys(false, true));
        double y0 = saucer.getY();
        h.runAfterDelay(20, () -> {
            h.assertTrue(saucer.charge() > PhlogistonCellItem.CHARGES - 5, "it didn't draw the cell: " + saucer.charge());
            h.assertTrue(pilot.getInventory().findSlotMatchingItem(new ItemStack(ModItems.EMPTY_PHLOGISTON_CELL.get())) >= 0, "the cell wasn't emptied");
            h.assertTrue(saucer.getY() > y0 + 3, "it didn't lift off on the drawn charge");
            h.assertTrue(stranded.charge() == 0 && stranded.getY() < y0 + 0.5, "it flew with nothing to draw");
            h.assertTrue(stranded.gauge().getString().startsWith("No phlogiston"), "the gauge didn't say why: " + stranded.gauge().getString());
            pilot.stopRiding();
            castaway.stopRiding();
            saucer.discard();
            stranded.discard();
            h.succeed();
        });
    }

    /** With charge it climbs (standing helm); with none it sinks; a Phlogiston Cell pours its charge in. */
    static void saucerFliesOnCharge(GameTestHelper h) {
        var saucer = saucer(h, new BlockPos(20, 5, 20), false, false, 20);
        saucer.setHelm(0, 1);
        double start = saucer.getY();
        ServerPlayer player = TestPlayers.survival(h, new BlockPos(16, 1, 20));
        player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(ModItems.PHLOGISTON_CELL.get()));
        h.runAfterDelay(20, () -> {
            h.assertTrue(saucer.getY() > start + 2, "it didn't climb: " + (saucer.getY() - start));
            saucer.setCharge(0);
            saucer.setHelm(0, 0);
        });
        h.runAfterDelay(40, () -> {
            double high = saucer.getY();
            h.runAfterDelay(10, () -> {
                h.assertTrue(saucer.getY() < high, "with no charge it didn't sink");
                saucer.interact(player, InteractionHand.MAIN_HAND, saucer.position());
                h.assertValueEqual(saucer.charge(), PhlogistonCellItem.CHARGES, "charge poured from a cell");
                h.assertTrue(player.getMainHandItem().is(ModItems.EMPTY_PHLOGISTON_CELL.get()), "the cell wasn't emptied");
                saucer.discard();
                h.succeed();
            });
        });
    }

    /** Its tractor beam draws a cow up under it; with the gamerule off, it passes over a player. */
    static void tractorBeamLifts(GameTestHelper h) {
        Cow cow = h.spawn(EntityTypes.COW, new BlockPos(20, 1, 20));
        ServerPlayer under = TestPlayers.survival(h, new BlockPos(21, 1, 21));
        var rule = com.hollowfrontier.registry.ModGameRules.SAUCER_BEAMS_PLAYERS.get();
        h.getLevel().getGameRules().set(rule, false, h.getLevel().getServer());
        var saucer = saucer(h, new BlockPos(20, 14, 20), true, false, 50);
        ServerPlayer pilot = TestPlayers.survival(h, new BlockPos(20, 14, 20));
        pilot.startRiding(saucer, true, true);
        saucer.setControls(false, true);
        double start = cow.getY();
        h.runAfterDelay(30, () -> {
            try {
                h.assertTrue(saucer.isBeaming(), "the beam isn't on");
                h.assertTrue(saucer.carried().contains(cow.getUUID()) && cow.getY() > start + 3, "the cow wasn't drawn up: " + (cow.getY() - start));
                h.assertFalse(saucer.carried().contains(under.getUUID()), "it took a player with the gamerule off");
            } finally {
                h.getLevel().getGameRules().set(rule, true, h.getLevel().getServer());
                pilot.stopRiding();
                saucer.discard();
            }
            h.succeed();
        });
    }

    /** Its heat laser burns a zombie, and it overheats if fired too long. */
    static void heatLaserBurnsAndOverheats(GameTestHelper h) {
        var zombie = dummy(h, new BlockPos(20, 1, 20));
        var saucer = saucer(h, new BlockPos(20, 10, 20), false, true, 50);
        Vec3 down = zombie.position().add(0, 1, 0).subtract(saucer.position().add(0, -0.3, 0));
        h.assertTrue(saucer.bolt(h.getLevel(), null, down) == zombie, "the bolt missed");
        h.assertTrue(zombie.getHealth() < zombie.getMaxHealth() && zombie.isOnFire(), "the zombie wasn't burned");
        for (int i = 0; i < 20 && !saucer.isOverheated(); i++) {
            saucer.bolt(h.getLevel(), null, down);
        }
        h.assertTrue(saucer.isOverheated(), "it never overheated: " + saucer.heat());
        saucer.discard();
        h.succeed();
    }

    /**
     * The laser lands where the pilot's crosshair is: aimed off their line of sight, a bolt from the underside strikes the
     * zombie they're looking at, 16 blocks off and well below (fired along their look it went a couple of blocks under).
     */
    static void laserHitsWhatThePilotSees(GameTestHelper h) {
        var zombie = dummy(h, new BlockPos(24, 1, 20));
        var saucer = saucer(h, new BlockPos(8, 7, 20), false, true, 50);
        ServerPlayer pilot = TestPlayers.survival(h, new BlockPos(8, 7, 20));
        pilot.startRiding(saucer, true, true);
        h.runAfterDelay(2, () -> {
            pilot.lookAt(net.minecraft.commands.arguments.EntityAnchorArgument.Anchor.EYES, zombie.getEyePosition());
            h.assertTrue(saucer.bolt(h.getLevel(), pilot, saucer.aim(h.getLevel(), pilot)) == zombie, "the aimed bolt missed");
            pilot.stopRiding();
            saucer.discard();
            h.succeed();
        });
    }

    /**
     * Held, the laser fires at the sight the pilot's client reports (in third person the camera's middle, off their eye
     * line), even with the pilot looking elsewhere; a sight further off than it could reach is ignored.
     */
    static void laserFiresAtTheReportedSight(GameTestHelper h) {
        var zombie = dummy(h, new BlockPos(24, 1, 20));
        var saucer = saucer(h, new BlockPos(8, 7, 20), false, true, 50);
        ServerPlayer pilot = TestPlayers.survival(h, new BlockPos(8, 7, 20));
        pilot.startRiding(saucer, true, true);
        h.runAfterDelay(2, () -> {
            pilot.lookAt(net.minecraft.commands.arguments.EntityAnchorArgument.Anchor.EYES, pilot.getEyePosition().add(0, 10, -10));
            saucer.sight(saucer.position().add(0, 500, 0));
            saucer.sight(zombie.position().add(0, 1, 0));
            saucer.setControls(true, false);
        });
        h.runAfterDelay(8, () -> {
            h.assertTrue(zombie.getHealth() < zombie.getMaxHealth(), "the bolt didn't go to the sight");
            saucer.setControls(false, false);
            pilot.stopRiding();
            saucer.discard();
            h.succeed();
        });
    }

    /** Aboard, using what you hold uses it (bread is eaten, not the saucer clicked); from outside, the same use boards. */
    static void crewUseItemsAboard(GameTestHelper h) {
        var saucer = saucer(h, new BlockPos(20, 1, 20), false, false, 64);
        ServerPlayer crew = TestPlayers.survival(h, new BlockPos(17, 1, 20));
        ServerPlayer outside = TestPlayers.survival(h, new BlockPos(24, 1, 20));
        crew.startRiding(saucer, true, true);
        crew.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(Items.BREAD));
        outside.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(Items.BREAD));
        h.assertTrue(saucer.interact(crew, InteractionHand.MAIN_HAND, saucer.position()) == net.minecraft.world.InteractionResult.PASS,
                "the saucer took its own crew's use");
        h.assertTrue(saucer.interact(outside, InteractionHand.MAIN_HAND, saucer.position()).consumesAction() && outside.getVehicle() == saucer,
                "the one outside didn't board");
        crew.stopRiding();
        outside.stopRiding();
        saucer.discard();
        h.succeed();
    }

    // ---------------------------------------------------------------- 15a: Gray Landings

    /** A landing coming down in the middle of the field, a few blocks short of its resting place so the test is quick. */
    private static GraySaucer landing(GameTestHelper h) {
        h.getLevel().getServer().setDifficulty(Difficulty.NORMAL, true); // peaceful would send its Wardens away
        BlockPos ground = h.absolutePos(new BlockPos(20, 0, 20));
        GraySaucer saucer = GrayLandings.landAt(h.getLevel(), ground, GrayLandings.day(h.getLevel()));
        saucer.setPos(saucer.restingPlace().add(0, 1, 0));
        return saucer;
    }

    /** Whatever a landing left about (its guards, the saucer, the cache) cleared away. */
    private static void clearLanding(GameTestHelper h, GraySaucer saucer) {
        h.getLevel().getEntitiesOfClass(LivingEntity.class, new AABB(h.absolutePos(new BlockPos(20, 0, 20))).inflate(GrayLandings.GUARD_RANGE),
                e -> e.entityTags().contains(GrayLandings.guardOf(saucer))).forEach(Entity::discard);
        if (saucer.cache() != null) {
            h.getLevel().removeBlock(saucer.cache(), false);
        }
        saucer.discard();
    }

    /** Open, level ground will take a saucer; a tree, or water, in the way won't; the search finds the field. */
    static void landingNeedsOpenGround(GameTestHelper h) {
        BlockPos ground = h.absolutePos(new BlockPos(20, 0, 20));
        h.assertTrue(GrayLandings.clear(h.getLevel(), ground), "the open field won't do");
        h.setBlock(new BlockPos(22, 1, 20), Blocks.OAK_LOG);
        h.setBlock(new BlockPos(22, 2, 20), Blocks.OAK_LOG);
        h.assertFalse(GrayLandings.clear(h.getLevel(), ground), "it set down on a tree");
        h.setBlock(new BlockPos(22, 1, 20), Blocks.AIR);
        h.setBlock(new BlockPos(22, 2, 20), Blocks.AIR);
        h.setBlock(new BlockPos(18, 0, 21), Blocks.WATER);
        h.assertFalse(GrayLandings.clear(h.getLevel(), ground), "it set down in a pond");
        h.setBlock(new BlockPos(18, 0, 21), Blocks.GRASS_BLOCK);
        h.assertTrue(GrayLandings.findClearing(h.getLevel(), ground, 2, 6, h.getLevel().getRandom()).isPresent(), "nowhere in the field to land");
        h.succeed();
    }

    /**
     * Down: the clearing scorched, Wardens and drones about it, a sealed cache beside it, and (in the Overworld) the landing
     * logged where the townsfolk will talk of it.
     */
    static void landingComesDown(GameTestHelper h) {
        GraySaucer saucer = landing(h);
        h.succeedWhen(() -> {
            h.assertTrue(saucer.run() == GraySaucer.Run.LANDED, "still coming down (" + saucer.run() + ")");
            ServerLevel level = h.getLevel();
            h.assertTrue(saucer.cache() != null && level.getBlockState(saucer.cache()).is(ModBlocks.GRAY_CACHE.get()), "no cache");
            h.assertTrue(GrayLandings.sealed(level, saucer.cache()), "the cache isn't sealed");
            List<LivingEntity> guards = GrayLandings.guards(level, saucer);
            h.assertTrue(guards.stream().filter(e -> e instanceof GrayWarden).count() >= GrayLandings.WARDENS, "too few Wardens: " + guards);
            h.assertValueEqual(guards.stream().filter(e -> e instanceof GrayDrone).count(), (long) GrayLandings.DRONES, "drones");
            int burnt = 0;
            for (BlockPos pos : BlockPos.betweenClosed(saucer.ground().offset(-6, 0, -6), saucer.ground().offset(6, 0, 6))) {
                BlockState state = level.getBlockState(pos);
                if (state.is(Blocks.COARSE_DIRT) || state.is(Blocks.BASALT) || state.is(Blocks.BLACKSTONE) || state.is(Blocks.DYED_TERRACOTTA.black())) {
                    burnt++;
                }
            }
            h.assertTrue(burnt >= 60, "only " + burnt + " blocks scorched");
            h.assertTrue(Context.of(level, saucer.ground(), null).has(Fact.LANDINGS), "the townsfolk haven't heard of it");
            clearLanding(h, saucer);
        });
    }

    /** Dawn before it's cleared: its guards go back aboard, the cache with them, and it lifts off. */
    static void landingLeavesAtDawn(GameTestHelper h) {
        GraySaucer saucer = landing(h);
        h.runAfterDelay(4, () -> saucer.setLandedDay(GrayLandings.day(h.getLevel()) - 1));
        h.succeedWhen(() -> {
            h.assertTrue(saucer.run() == GraySaucer.Run.LEAVE, "still down (" + saucer.run() + ")");
            h.assertTrue(GrayLandings.guards(h.getLevel(), saucer).isEmpty(), "guards left behind");
            h.assertFalse(h.getLevel().getBlockState(saucer.cache()).is(ModBlocks.GRAY_CACHE.get()), "the cache was left behind");
            clearLanding(h, saucer);
        });
    }

    /** Sealed while a guard's up; with every guard down the saucer flees, and the cache opens on plating and a cell. */
    static void clearedLandingPaysOut(GameTestHelper h) {
        GraySaucer saucer = landing(h);
        ServerPlayer player = TestPlayers.survival(h, new BlockPos(20, 1, 30));
        player.addEffect(new net.minecraft.world.effect.MobEffectInstance(MobEffects.RESISTANCE, 400, 4));
        boolean[] killed = {false}, openedEarly = {false};
        h.succeedWhen(() -> {
            h.assertTrue(saucer.cache() != null, "not down yet");
            BlockPos cache = h.relativePos(saucer.cache());
            if (!killed[0]) {
                h.useBlock(cache, player);
                openedEarly[0] = !h.getBlockState(cache).is(ModBlocks.GRAY_CACHE.get());
                GrayLandings.guards(h.getLevel(), saucer).forEach(guard -> guard.kill(h.getLevel()));
                killed[0] = true;
            }
            h.assertFalse(openedEarly[0], "it opened with its keepers about");
            h.assertTrue(saucer.run() == GraySaucer.Run.LEAVE, "still down (" + saucer.run() + ")");
            h.useBlock(cache, player);
            h.assertTrue(h.getBlockState(cache).isAir(), "the cache didn't open");
            Set<Item> found = h.getLevel().getEntitiesOfClass(ItemEntity.class, new AABB(saucer.cache()).inflate(1.5)).stream()
                    .map(e -> e.getItem().getItem()).collect(Collectors.toSet());
            h.assertTrue(found.contains(ModItems.ALIEN_ALLOY_PLATING.get()) && found.contains(ModItems.PHLOGISTON_CELL.get()), "the cache held only " + found);
            TestPlayers.leave(player);
            clearLanding(h, saucer);
        });
    }
}
