package com.hollowfrontier.gametest;

import java.util.UUID;

import com.hollowfrontier.content.frontier.bounty.BountyData;
import com.hollowfrontier.content.frontier.bounty.BountyEvents;
import com.hollowfrontier.content.frontier.gear.CowboySetBonus;
import com.hollowfrontier.content.frontier.gear.LassoEntity;
import com.hollowfrontier.content.frontier.gun.GunItem;
import com.hollowfrontier.content.frontier.gun.GunMechanics;
import com.hollowfrontier.content.frontier.gun.GunTypes;
import com.hollowfrontier.content.frontier.horse.SaddlebagContainer;
import com.hollowfrontier.content.frontier.gun.GunWielder;
import com.hollowfrontier.content.frontier.outlaw.AbstractOutlaw;
import com.hollowfrontier.content.frontier.outlaw.Outlaw;
import com.hollowfrontier.content.frontier.outlaw.OutlawBoss;
import com.hollowfrontier.content.frontier.saloon.Barmaid;
import com.hollowfrontier.content.frontier.saloon.Drink;
import com.hollowfrontier.content.frontier.saloon.Hostess;
import com.hollowfrontier.content.frontier.saloon.TipsyEffect;
import com.hollowfrontier.content.frontier.townsfolk.Folk;
import com.hollowfrontier.content.frontier.townsfolk.Townsfolk;
import com.hollowfrontier.registry.ModAttachments;
import com.hollowfrontier.registry.ModBlocks;
import com.hollowfrontier.registry.ModDataComponents;
import com.hollowfrontier.registry.ModEntities;
import com.hollowfrontier.registry.ModItems;

import net.minecraft.commands.arguments.EntityAnchorArgument;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.Difficulty;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.animal.cow.Cow;
import net.minecraft.world.entity.animal.golem.IronGolem;
import net.minecraft.world.entity.animal.equine.Horse;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

// Phases 3-5: guns, cowboy gear, horse gear, outlaws, bounties, townsfolk.
final class FrontierTests {
    private FrontierTests() {}

    private static void aimAt(Player player, Entity target) {
        player.lookAt(EntityAnchorArgument.Anchor.EYES, target.getBoundingBox().getCenter());
    }

    private static int count(Player player, net.minecraft.world.item.Item item) {
        int n = 0;
        for (int i = 0; i < player.getInventory().getContainerSize(); i++) {
            ItemStack s = player.getInventory().getItem(i);
            if (s.is(item)) n += s.getCount();
        }
        return n;
    }

    static void revolverHitsAndUsesAmmo(GameTestHelper h) {
        ServerPlayer player = TestPlayers.survival(h, new BlockPos(2, 1, 2));
        Cow cow = h.spawnWithNoFreeWill(EntityTypes.COW, new BlockPos(2, 1, 7));
        aimAt(player, cow);
        ItemStack gun = new ItemStack(ModItems.REVOLVER.get());
        GunItem.setAmmo(gun, 6);
        player.setItemInHand(InteractionHand.MAIN_HAND, gun);
        gun.getItem().use(h.getLevel(), player, InteractionHand.MAIN_HAND);
        h.assertTrue(cow.getHealth() < cow.getMaxHealth(), "revolver did not damage the cow");
        h.assertTrue(GunItem.getAmmo(gun) == 5, "ammo not decremented: " + GunItem.getAmmo(gun));
        h.assertTrue(player.getCooldowns().isOnCooldown(gun), "no fire cooldown");
        h.assertTrue(gun.getDamageValue() == 1, "durability not used");
        h.succeed();
    }

    // Runs the reload clock forward one chamber (fake players don't tick their inventory).
    private static void finishChamber(GameTestHelper h, Player player, ItemStack gun) {
        GunItem item = (GunItem) gun.getItem();
        GunItem.setReloadStart(gun, h.getLevel().getGameTime() - item.stats().loadTicks());
        item.tickReload(player, gun);
    }

    static void gunReloadTakesInventoryAmmo(GameTestHelper h) {
        ServerPlayer player = TestPlayers.survival(h, new BlockPos(2, 1, 2));
        ItemStack gun = new ItemStack(ModItems.REVOLVER.get());
        player.setItemInHand(InteractionHand.MAIN_HAND, gun);
        player.getInventory().add(new ItemStack(ModItems.REVOLVER_CARTRIDGE.get(), 10));
        player.getInventory().add(new ItemStack(ModItems.PERCUSSION_CAP.get(), 10));
        h.assertTrue(((GunItem) gun.getItem()).startReload(player, gun), "reload did not start");
        h.assertTrue(GunItem.getAmmo(gun) == 0, "loaded before any time passed");
        finishChamber(h, player, gun);
        h.assertTrue(GunItem.getAmmo(gun) == 1 && GunItem.isReloading(gun), "first chamber not loaded or reload stopped early");
        for (int i = 0; i < 5; i++) {
            finishChamber(h, player, gun);
        }
        h.assertTrue(GunItem.getAmmo(gun) == 6, "cylinder not full: " + GunItem.getAmmo(gun));
        h.assertTrue(!GunItem.isReloading(gun), "still reloading when full");
        h.assertTrue(count(player, ModItems.REVOLVER_CARTRIDGE.get()) == 4, "wrong cartridges left: " + count(player, ModItems.REVOLVER_CARTRIDGE.get()));
        h.assertTrue(count(player, ModItems.PERCUSSION_CAP.get()) == 4, "wrong caps left: " + count(player, ModItems.PERCUSSION_CAP.get()));
        h.succeed();
    }

    static void gunReloadNeedsCaps(GameTestHelper h) {
        ServerPlayer player = TestPlayers.survival(h, new BlockPos(2, 1, 2));
        ItemStack gun = new ItemStack(ModItems.SHOTGUN.get());
        player.setItemInHand(InteractionHand.MAIN_HAND, gun);
        GunItem item = (GunItem) gun.getItem();
        player.getInventory().add(new ItemStack(ModItems.BUCKSHOT_CARTRIDGE.get(), 5));
        h.assertTrue(!item.startReload(player, gun), "reloaded without percussion caps");
        player.getInventory().add(new ItemStack(ModItems.PERCUSSION_CAP.get(), 1));
        h.assertTrue(item.startReload(player, gun), "reload did not start with a cap");
        finishChamber(h, player, gun);
        h.assertTrue(GunItem.getAmmo(gun) == 1, "barrel not loaded");
        h.assertTrue(!GunItem.isReloading(gun), "kept loading after the last cap");
        h.succeed();
    }

    static void firingStopsReload(GameTestHelper h) {
        ServerPlayer player = TestPlayers.survival(h, new BlockPos(2, 1, 2));
        ItemStack gun = new ItemStack(ModItems.REVOLVER.get());
        player.setItemInHand(InteractionHand.MAIN_HAND, gun);
        player.getInventory().add(new ItemStack(ModItems.REVOLVER_CARTRIDGE.get(), 5));
        player.getInventory().add(new ItemStack(ModItems.PERCUSSION_CAP.get(), 5));
        GunItem.setAmmo(gun, 2);
        h.assertTrue(((GunItem) gun.getItem()).startReload(player, gun), "top-up reload did not start");
        gun.getItem().use(h.getLevel(), player, InteractionHand.MAIN_HAND);
        h.assertTrue(!GunItem.isReloading(gun), "firing did not stop the reload");
        h.assertTrue(GunItem.getAmmo(gun) == 1, "shot not taken: " + GunItem.getAmmo(gun));
        h.assertTrue(gun.has(ModDataComponents.LAST_FIRED.get()), "fire animation state not set");
        h.succeed();
    }

    static void shotgunPelletsStackDamage(GameTestHelper h) {
        ServerPlayer player = TestPlayers.survival(h, new BlockPos(2, 1, 2));
        Cow cow = h.spawnWithNoFreeWill(EntityTypes.COW, new BlockPos(2, 1, 5));
        aimAt(player, cow);
        ItemStack gun = new ItemStack(ModItems.SHOTGUN.get());
        GunItem.setAmmo(gun, 2);
        player.setItemInHand(InteractionHand.MAIN_HAND, gun);
        gun.getItem().use(h.getLevel(), player, InteractionHand.MAIN_HAND);
        float dealt = cow.getMaxHealth() - cow.getHealth();
        h.assertTrue(dealt >= 7.0F, "expected several pellets to land, dealt " + dealt);
        h.succeed();
    }

    /** A player's shot to the head pays the gun's headshot multiplier and one to the body doesn't, from a rifle ball or a phlogiston beam. */
    static void headshotsHitHarder(GameTestHelper h) {
        ServerPlayer player = TestPlayers.survival(h, new BlockPos(2, 1, 2));
        IronGolem head = h.spawnWithNoFreeWill(EntityTypes.IRON_GOLEM, new BlockPos(2, 1, 8));
        IronGolem body = h.spawnWithNoFreeWill(EntityTypes.IRON_GOLEM, new BlockPos(8, 1, 8));
        shootPair(h, player, head, body, "plains rifle");
        // The phlogiston rifle once the golems' hurt cooldown has run out
        h.runAfterDelay(25, () -> {
            shootPair(h, player, head, body, "phlogiston rifle");
            h.succeed();
        });
    }

    // One shot at the head golem's eyes and one at the body golem's middle; the head must lose about twice as much.
    private static void shootPair(GameTestHelper h, Player player, IronGolem head, IronGolem body, String gun) {
        head.setHealth(head.getMaxHealth());
        body.setHealth(body.getMaxHealth());
        player.lookAt(EntityAnchorArgument.Anchor.EYES, new Vec3(head.getX(), head.getEyeY(), head.getZ()));
        shoot(h, player, gun);
        aimAt(player, body);
        shoot(h, player, gun);
        float headLost = head.getMaxHealth() - head.getHealth();
        float bodyLost = body.getMaxHealth() - body.getHealth();
        h.assertTrue(bodyLost > 0 && headLost > bodyLost * 1.9F, gun + ": head lost " + headLost + ", body " + bodyLost);
    }

    private static void shoot(GameTestHelper h, Player player, String gun) {
        if (gun.equals("plains rifle")) {
            GunMechanics.fire(h.getLevel(), player, GunTypes.RIFLE);
        } else {
            ModItems.PHLOGISTON_RIFLE.get().beam(h.getLevel(), player);
        }
    }

    static void lassoLeashesCow(GameTestHelper h) {
        ServerPlayer player = TestPlayers.survival(h, new BlockPos(2, 1, 2));
        Cow cow = h.spawnWithNoFreeWill(EntityTypes.COW, new BlockPos(2, 1, 6));
        aimAt(player, cow);
        LassoEntity lasso = new LassoEntity(h.getLevel(), player, new ItemStack(ModItems.LASSO.get()));
        lasso.shootFromRotation(player, player.getXRot(), player.getYRot(), 0.0F, 1.6F, 0.0F);
        h.getLevel().addFreshEntity(lasso);
        h.succeedWhen(() -> {
            h.assertTrue(cow.isLeashed() && cow.getLeashHolder() == player, "cow not leashed to the thrower");
            h.assertTrue(AdvancementTests.earned(player, "frontier/rope_em_in"), "lassoing earned nothing");
        });
    }

    /**
     * 46b: using a lasso winds it up and throws nothing; letting go throws one, with the cooldown, however short the
     * wind-up was (a click is a press and a release).
     */
    static void lassoWindsUpThenThrows(GameTestHelper h) {
        ServerPlayer player = TestPlayers.survival(h, new BlockPos(2, 1, 2));
        ItemStack stack = new ItemStack(ModItems.LASSO.get(), 3);
        player.setItemInHand(InteractionHand.MAIN_HAND, stack);
        var box = new net.minecraft.world.phys.AABB(h.absolutePos(new BlockPos(2, 1, 2))).inflate(8);
        int before = h.getLevel().getEntitiesOfClass(LassoEntity.class, box).size();
        ModItems.LASSO.get().use(h.getLevel(), player, InteractionHand.MAIN_HAND);
        h.assertTrue(player.isUsingItem(), "using a lasso should wind it up");
        h.assertTrue(h.getLevel().getEntitiesOfClass(LassoEntity.class, box).size() == before && stack.getCount() == 3, "winding up shouldn't throw");
        player.releaseUsingItem();
        h.assertTrue(h.getLevel().getEntitiesOfClass(LassoEntity.class, box).size() == before + 1, "letting go should throw one lasso");
        h.assertTrue(stack.getCount() == 2 && player.getCooldowns().isOnCooldown(stack), "a throw should cost a lasso and start the cooldown");
        h.succeed();
    }

    static void cowboySetBonusSpeedsHorse(GameTestHelper h) {
        ServerPlayer player = TestPlayers.survival(h, new BlockPos(4, 1, 4));
        Horse horse = h.spawnWithNoFreeWill(EntityTypes.HORSE, new BlockPos(4, 1, 4));
        horse.setTamed(true);
        player.startRiding(horse, true, false);
        player.setItemSlot(EquipmentSlot.HEAD, new ItemStack(ModItems.COWBOY_HAT.get()));
        player.setItemSlot(EquipmentSlot.CHEST, new ItemStack(ModItems.DUSTER.get()));
        player.setItemSlot(EquipmentSlot.LEGS, new ItemStack(ModItems.CHAPS.get()));
        player.setItemSlot(EquipmentSlot.FEET, new ItemStack(ModItems.COWBOY_BOOTS.get()));
        AttributeInstance speed = horse.getAttribute(Attributes.MOVEMENT_SPEED);
        double base = speed.getValue();
        CowboySetBonus.update(player);
        h.assertTrue(speed.getValue() > base * 1.15, "set bonus not applied: " + base + " -> " + speed.getValue());
        player.setItemSlot(EquipmentSlot.HEAD, ItemStack.EMPTY);
        CowboySetBonus.update(player);
        h.assertTrue(Math.abs(speed.getValue() - base) < 1e-6, "set bonus not removed");
        h.succeed();
    }

    static void horseGearFitAndStrip(GameTestHelper h) {
        ServerPlayer player = TestPlayers.survival(h, new BlockPos(2, 1, 4));
        Horse horse = h.spawnWithNoFreeWill(EntityTypes.HORSE, new BlockPos(4, 1, 4));
        horse.setTamed(true);
        AttributeInstance speed = horse.getAttribute(Attributes.MOVEMENT_SPEED);
        double base = speed.getValue();

        player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(ModItems.IRON_HORSESHOES.get()));
        player.interactOn(horse, InteractionHand.MAIN_HAND, Vec3.ZERO);
        h.assertTrue(horse.getData(ModAttachments.HORSESHOE_TIER.get()) == 1, "horseshoes not fitted");
        h.assertTrue(speed.getValue() > base, "horseshoes gave no speed");
        h.assertTrue(player.getMainHandItem().isEmpty(), "horseshoes not consumed");

        player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(ModItems.SADDLEBAGS.get()));
        player.interactOn(horse, InteractionHand.MAIN_HAND, Vec3.ZERO);
        h.assertTrue(horse.hasData(ModAttachments.SADDLEBAGS.get()), "saddlebags not attached");
        SaddlebagContainer.of(horse).setItem(0, new ItemStack(Items.DIAMOND, 3));
        h.assertTrue(horse.getData(ModAttachments.SADDLEBAGS.get()).nonEmptyItemCopyStream().count() == 1, "saddlebag contents not saved");

        player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(Items.SHEARS));
        player.setShiftKeyDown(true);
        player.interactOn(horse, InteractionHand.MAIN_HAND, Vec3.ZERO);
        h.assertTrue(horse.getData(ModAttachments.HORSESHOE_TIER.get()) == 0 && !horse.hasData(ModAttachments.SADDLEBAGS.get()), "gear not stripped");
        h.assertTrue(Math.abs(speed.getValue() - base) < 1e-6, "horseshoe modifier not removed");
        h.assertItemEntityPresent(ModItems.IRON_HORSESHOES.get());
        h.assertItemEntityPresent(ModItems.SADDLEBAGS.get());
        h.assertItemEntityPresent(Items.DIAMOND);
        h.succeed();
    }

    static void outlawSpawnsArmed(GameTestHelper h) {
        h.getLevel().getServer().setDifficulty(Difficulty.NORMAL, true);
        Outlaw outlaw = ModEntities.OUTLAW.get().spawn(h.getLevel(), h.absolutePos(new BlockPos(5, 1, 5)), EntitySpawnReason.SPAWN_ITEM_USE);
        h.assertTrue(outlaw != null, "outlaw failed to spawn");
        h.assertTrue(outlaw.getMainHandItem().is(ModItems.REVOLVER.get()), "outlaw has no revolver: " + outlaw.getMainHandItem());
        h.succeed();
    }

    static void outlawShootsPlayer(GameTestHelper h) {
        h.getLevel().getServer().setDifficulty(Difficulty.NORMAL, true);
        ServerPlayer player = TestPlayers.survival(h, new BlockPos(2, 1, 2));
        Outlaw outlaw = ModEntities.OUTLAW.get().spawn(h.getLevel(), h.absolutePos(new BlockPos(2, 1, 9)), EntitySpawnReason.SPAWN_ITEM_USE);
        outlaw.setTarget(player);
        h.succeedWhen(() -> h.assertTrue(player.getHealth() < player.getMaxHealth(), "outlaw has not hit the player yet"));
    }

    static void outlawStopsToAim(GameTestHelper h) {
        h.getLevel().getServer().setDifficulty(Difficulty.NORMAL, true);
        ServerPlayer player = TestPlayers.survival(h, new BlockPos(2, 1, 2));
        Outlaw outlaw = ModEntities.OUTLAW.get().spawn(h.getLevel(), h.absolutePos(new BlockPos(2, 1, 9)), EntitySpawnReason.SPAWN_ITEM_USE);
        outlaw.setTarget(player);
        // The first hit must come from a standing outlaw with its gun raised.
        h.succeedWhen(() -> {
            h.assertTrue(player.getHealth() < player.getMaxHealth(), "outlaw has not hit the player yet");
            h.assertTrue(outlaw.getGunPose() == GunWielder.GunPose.AIMING, "outlaw shot without aiming: " + outlaw.getGunPose());
            h.assertTrue(outlaw.getNavigation().isDone(), "outlaw shot while walking");
        });
    }

    static void bossDropsBadge(GameTestHelper h) {
        h.getLevel().getServer().setDifficulty(Difficulty.NORMAL, true);
        OutlawBoss boss = h.spawnWithNoFreeWill(ModEntities.OUTLAW_BOSS.get(), new BlockPos(5, 1, 5));
        BountyData bounty = new BountyData(UUID.randomUUID(), "Test 'Gametest' McGraw", 0, 0, 4, true);
        boss.setBounty(bounty);
        boss.kill(h.getLevel());
        h.succeedWhen(() -> {
            h.assertItemEntityPresent(ModItems.OUTLAW_BADGE.get());
            boolean match = h.getLevel().getEntitiesOfClass(net.minecraft.world.entity.item.ItemEntity.class, new AABB(h.absolutePos(BlockPos.ZERO)).inflate(16))
                    .stream().anyMatch(e -> bounty.equals(e.getItem().get(ModDataComponents.BOUNTY.get())));
            h.assertTrue(match, "badge does not carry the boss's bounty");
        });
    }

    static void bountyBoardLoop(GameTestHelper h) {
        BlockPos board = new BlockPos(5, 1, 5);
        h.setBlock(board, ModBlocks.BOUNTY_BOARD.get());
        ServerPlayer player = TestPlayers.survival(h, new BlockPos(5, 1, 3));
        h.useBlock(board, player);
        ItemStack poster = findItem(player, ModItems.WANTED_POSTER.get());
        h.assertTrue(poster != null, "board gave no poster");
        BountyData bounty = poster.get(ModDataComponents.BOUNTY.get());
        h.assertTrue(bounty != null && !bounty.spawned() && bounty.reward() >= 3, "bad poster bounty " + bounty);

        h.useBlock(board, player);
        h.assertTrue(count(player, ModItems.WANTED_POSTER.get()) == 1, "board gave a second poster");

        ItemStack badge = new ItemStack(ModItems.OUTLAW_BADGE.get());
        badge.set(ModDataComponents.BOUNTY.get(), bounty);
        player.setItemInHand(InteractionHand.MAIN_HAND, badge);
        h.useBlock(board, player);
        h.assertTrue(count(player, ModItems.GOLD_COIN.get()) == bounty.reward(), "reward not paid: " + count(player, ModItems.GOLD_COIN.get()));
        h.assertTrue(count(player, ModItems.WANTED_POSTER.get()) == 0, "poster not torn up");
        h.assertTrue(count(player, ModItems.OUTLAW_BADGE.get()) == 0, "badge not consumed");
        h.succeed();
    }

    static void bountyGangSpawnsNearHideout(GameTestHelper h) {
        h.getLevel().getServer().setDifficulty(Difficulty.NORMAL, true);
        ServerPlayer player = TestPlayers.survival(h, new BlockPos(5, 1, 5));
        ItemStack poster = new ItemStack(ModItems.WANTED_POSTER.get());
        poster.set(ModDataComponents.BOUNTY.get(), new BountyData(UUID.randomUUID(), "Test Gang", player.getBlockX(), player.getBlockZ(), 3, false));
        player.getInventory().add(poster);
        BountyEvents.checkPosters(player);
        ItemStack carried = findItem(player, ModItems.WANTED_POSTER.get());
        h.assertTrue(carried != null && carried.get(ModDataComponents.BOUNTY.get()).spawned(), "poster not marked spawned");
        h.assertTrue(!h.getLevel().getEntitiesOfClass(OutlawBoss.class, player.getBoundingBox().inflate(32)).isEmpty(), "no boss near the hideout");
        int henchmen = h.getLevel().getEntitiesOfClass(com.hollowfrontier.content.frontier.outlaw.AbstractOutlaw.class, player.getBoundingBox().inflate(32)).size();
        h.assertTrue(henchmen >= 4, "expected boss + 3 henchmen, found " + henchmen);
        h.succeed();
    }

    static void townsfolkTrades(GameTestHelper h) {
        // Each trade sells its own wares (24c review): the banker changes coin, only the gunsmith sells guns
        int i = 0;
        for (Folk folk : Folk.values()) {
            Townsfolk townsfolk = h.spawnWithNoFreeWill(ModEntities.TOWNSFOLK.get(), new BlockPos(1 + i % 8, 1, 1 + i / 8));
            i++;
            townsfolk.setFolk(folk);
            boolean guns = townsfolk.getOffers().stream().anyMatch(o -> o.getResult().is(ModItems.REVOLVER.get()) || o.getResult().is(ModItems.SHOTGUN.get()));
            h.assertTrue(guns == (folk == Folk.GUNSMITH), folk + (guns ? " sells guns" : " sells no guns"));
            h.assertFalse(townsfolk.getOffers().isEmpty(), folk + " has nothing to trade");
            if (folk == Folk.BANKER) {
                h.assertTrue(townsfolk.getOffers().stream().anyMatch(o -> o.getCostA().is(ModItems.COPPER_COIN.get()) && o.getResult().is(ModItems.GOLD_COIN.get())),
                        "the banker doesn't change copper for gold");
            }
            townsfolk.discard();
        }
        h.succeed();
    }

    static void barmaidSellsDrinks(GameTestHelper h) {
        Barmaid barmaid = h.spawnWithNoFreeWill(ModEntities.BARMAID.get(), new BlockPos(5, 1, 5));
        boolean sellsSarsaparilla = barmaid.getOffers().stream().anyMatch(o -> o.getResult().is(ModItems.SARSAPARILLA.get()));
        h.assertTrue(sellsSarsaparilla, "barmaid does not sell sarsaparilla");
        h.assertTrue(barmaid.getOffers().size() == 12, "expected 12 trades, got " + barmaid.getOffers().size());
        h.succeed();
    }

    // Drinks are applied through the item, as eating/drinking does (fake players don't tick item use).
    private static void drink(GameTestHelper h, Player player, net.minecraft.world.item.Item item) {
        ItemStack stack = new ItemStack(item);
        stack.getItem().finishUsingItem(stack, h.getLevel(), player);
    }

    static void barmaidSpecialties(GameTestHelper h) {
        Barmaid ruby = h.spawnWithNoFreeWill(ModEntities.BARMAID.get(), new BlockPos(3, 1, 3));
        ruby.setHostess(Hostess.RUBY);
        int moonshine = ruby.getOffers().stream().filter(o -> o.getResult().is(ModItems.MOONSHINE.get()))
                .findFirst().orElseThrow().getBaseCostA().getCount();
        h.assertTrue(moonshine == 3, "Ruby's moonshine should cost 3, got " + moonshine);
        Barmaid clem = h.spawnWithNoFreeWill(ModEntities.BARMAID.get(), new BlockPos(6, 1, 6));
        clem.setHostess(Hostess.CLEMENTINE);
        int sarsaparilla = clem.getOffers().stream().filter(o -> o.getResult().is(ModItems.SARSAPARILLA.get()))
                .findFirst().orElseThrow().getResult().getCount();
        h.assertTrue(sarsaparilla == 2, "Clementine should pour two sarsaparillas a coin, got " + sarsaparilla);
        h.assertTrue(clem.getName().getString().equals("Clementine"), "hostess name not shown: " + clem.getName().getString());
        h.succeed();
    }

    static void whiskeyMakesTipsy(GameTestHelper h) {
        ServerPlayer player = TestPlayers.survival(h, new BlockPos(2, 1, 2));
        drink(h, player, ModItems.RYE_WHISKEY.get());
        h.assertTrue(player.hasEffect(MobEffects.STRENGTH), "whiskey gave no strength");
        h.assertTrue(Drink.tipsyLevel(player) == 1, "expected Tipsy I, got " + Drink.tipsyLevel(player));
        h.assertTrue(TipsyEffect.extraSpread(player) > 0, "tipsy does not spoil aim");
        drink(h, player, ModItems.MEZCAL.get());
        h.assertTrue(Drink.tipsyLevel(player) == 3, "tipsy does not stack, got " + Drink.tipsyLevel(player));
        h.succeed();
    }

    static void tooMuchLiquorBlacksOut(GameTestHelper h) {
        ServerPlayer player = TestPlayers.survival(h, new BlockPos(2, 1, 2));
        drink(h, player, ModItems.MOONSHINE.get());
        drink(h, player, ModItems.MOONSHINE.get());
        h.assertTrue(player.hasEffect(MobEffects.BLINDNESS), "no blackout past Tipsy IV");
        h.assertTrue(Drink.tipsyLevel(player) == 3, "should come round at Tipsy III, got " + Drink.tipsyLevel(player));
        h.succeed();
    }

    static void sobering(GameTestHelper h) {
        ServerPlayer player = TestPlayers.survival(h, new BlockPos(2, 1, 2));
        drink(h, player, ModItems.MOONSHINE.get());
        drink(h, player, ModItems.COWBOY_COFFEE.get());
        h.assertTrue(Drink.tipsyLevel(player) == 2, "coffee should take off one level, got " + Drink.tipsyLevel(player));
        drink(h, player, ModItems.SARSAPARILLA.get());
        h.assertTrue(Drink.tipsyLevel(player) == 0, "sarsaparilla should sober you right up");
        h.assertTrue(player.hasEffect(MobEffects.SPEED), "sarsaparilla gave no speed");
        h.succeed();
    }

    private static ItemStack findItem(Player player, net.minecraft.world.item.Item item) {
        for (int i = 0; i < player.getInventory().getContainerSize(); i++) {
            ItemStack s = player.getInventory().getItem(i);
            if (s.is(item)) return s;
        }
        return null;
    }
}
