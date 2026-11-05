package com.hollowfrontier.gametest;

import com.hollowfrontier.content.frontier.gun.GunEnchantments;
import com.hollowfrontier.content.frontier.gun.GunItem;
import com.hollowfrontier.content.frontier.gun.GunStats;
import com.hollowfrontier.content.frontier.gun.GunTypes;
import com.hollowfrontier.content.frontier.townsfolk.Folk;
import com.hollowfrontier.content.frontier.townsfolk.Townsfolk;
import com.hollowfrontier.registry.ModEnchantments;
import com.hollowfrontier.registry.ModEntities;
import com.hollowfrontier.registry.ModItems;
import com.hollowfrontier.registry.ModTags;

import net.minecraft.commands.arguments.EntityAnchorArgument;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.contents.TranslatableContents;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.EnchantmentTags;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.animal.golem.IronGolem;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.level.Level;

// The gun enchantments (22): which guns take which, what each one does, and the blacksmith's book.
final class GunEnchantmentTests {
    private GunEnchantmentTests() {}

    private static Holder<Enchantment> holder(Level level, ResourceKey<Enchantment> key) {
        return level.registryAccess().lookupOrThrow(Registries.ENCHANTMENT).getOrThrow(key);
    }

    private static ItemStack enchanted(GameTestHelper h, ItemStack gun, ResourceKey<Enchantment> key, int level) {
        gun.enchant(holder(h.getLevel(), key), level);
        return gun;
    }

    private static boolean near(float a, float b) {
        return Math.abs(a - b) < 0.001F;
    }

    /** Each enchantment goes on the guns it's meant for and nothing else; all seven are on the table; vanilla's durability ones fit every gun. */
    static void enchantmentsFitTheRightGuns(GameTestHelper h) {
        Level level = h.getLevel();
        ItemStack revolver = new ItemStack(ModItems.REVOLVER.get());
        ItemStack shotgun = new ItemStack(ModItems.SHOTGUN.get());
        ItemStack phlogistonRifle = new ItemStack(ModItems.PHLOGISTON_RIFLE.get());
        ItemStack sword = new ItemStack(Items.DIAMOND_SWORD);
        for (ItemStack gun : new ItemStack[] {revolver, new ItemStack(ModItems.PLAINS_RIFLE.get()), shotgun, phlogistonRifle,
                new ItemStack(ModItems.RAY_PISTOL.get())}) {
            h.assertTrue(gun.has(DataComponents.ENCHANTABLE), gun + " can't go on the enchanting table");
            h.assertTrue(holder(level, Enchantments.UNBREAKING).value().canEnchant(gun), gun + " won't take Unbreaking");
            h.assertTrue(holder(level, Enchantments.MENDING).value().canEnchant(gun), gun + " won't take Mending");
            h.assertTrue(holder(level, ModEnchantments.DEADEYE).value().canEnchant(gun), gun + " won't take Deadeye");
        }
        for (ResourceKey<Enchantment> key : new ResourceKey[] {ModEnchantments.RIFLING, ModEnchantments.QUICK_LOAD, ModEnchantments.DRY_POWDER,
                ModEnchantments.FRUGAL}) {
            Enchantment enchantment = holder(level, key).value();
            h.assertTrue(enchantment.canEnchant(revolver) && enchantment.canEnchant(shotgun), key + " won't go on a muzzleloader");
            h.assertFalse(enchantment.canEnchant(phlogistonRifle), key + " goes on a phlogiston gun");
        }
        Enchantment choke = holder(level, ModEnchantments.CHOKE).value();
        h.assertTrue(choke.canEnchant(shotgun) && !choke.canEnchant(revolver), "Choke isn't the shotgun's alone");
        // Focus is the phlogiston guns' alone, and it's on the table but not in the frontier's book trade (the blacksmith, camp loot)
        Holder<Enchantment> focus = holder(level, ModEnchantments.FOCUS);
        h.assertTrue(focus.value().canEnchant(phlogistonRifle) && focus.value().canEnchant(new ItemStack(ModItems.RAY_PISTOL.get())), "Focus won't go on a phlogiston gun");
        h.assertFalse(focus.value().canEnchant(revolver) || focus.value().canEnchant(shotgun) || focus.value().canEnchant(sword), "Focus goes on something else");
        h.assertTrue(focus.is(EnchantmentTags.IN_ENCHANTING_TABLE), "Focus isn't on the table");
        h.assertFalse(focus.is(ModTags.Enchantments.GUN), "Focus is in the frontier's books");
        for (ResourceKey<Enchantment> key : new ResourceKey[] {ModEnchantments.RIFLING, ModEnchantments.QUICK_LOAD, ModEnchantments.DEADEYE,
                ModEnchantments.CHOKE, ModEnchantments.DRY_POWDER, ModEnchantments.FRUGAL}) {
            Holder<Enchantment> enchantment = holder(level, key);
            h.assertTrue(enchantment.is(EnchantmentTags.IN_ENCHANTING_TABLE) && enchantment.is(ModTags.Enchantments.GUN), key + " missing a tag");
            h.assertFalse(enchantment.value().canEnchant(sword), key + " goes on a sword");
        }
        h.succeed();
    }

    /** What each enchantment does to a gun's tuning, at its top level; an unenchanted gun is untouched. */
    static void enchantmentsChangeTheTuning(GameTestHelper h) {
        Level level = h.getLevel();
        GunStats plain = GunEnchantments.apply(GunTypes.REVOLVER, new ItemStack(ModItems.REVOLVER.get()), level);
        h.assertTrue(plain == GunTypes.REVOLVER, "an unenchanted revolver's tuning changed");

        ItemStack revolver = new ItemStack(ModItems.REVOLVER.get());
        enchanted(h, revolver, ModEnchantments.RIFLING, 4);
        enchanted(h, revolver, ModEnchantments.QUICK_LOAD, 3);
        enchanted(h, revolver, ModEnchantments.DEADEYE, 3);
        GunStats stats = GunEnchantments.apply(GunTypes.REVOLVER, revolver, level);
        h.assertTrue(near(stats.damage(), 7.0F * 1.6F), "Rifling IV: " + stats.damage());
        h.assertValueEqual(stats.loadTicks(), 6, "Quick Load III load ticks (15 plain)");
        h.assertTrue(near(stats.headshot(), 2.25F), "Deadeye III headshot: " + stats.headshot());

        ItemStack shotgun = enchanted(h, new ItemStack(ModItems.SHOTGUN.get()), ModEnchantments.CHOKE, 3);
        float spread = GunEnchantments.apply(GunTypes.SHOTGUN, shotgun, level).spreadDegrees();
        h.assertTrue(near(spread, GunTypes.SHOTGUN.spreadDegrees() * 0.25F), "Choke III spread: " + spread);

        ItemStack dry = enchanted(h, new ItemStack(ModItems.PLAINS_RIFLE.get()), ModEnchantments.DRY_POWDER, 1);
        h.assertTrue(GunEnchantments.keepsPowderDry(dry, level), "Dry Powder didn't keep the powder dry");
        h.assertFalse(GunEnchantments.keepsPowderDry(new ItemStack(ModItems.PLAINS_RIFLE.get()), level), "a plain rifle kept its powder dry");
        h.succeed();
    }

    /** A Rifling revolver's shot really does hit harder (the enchantment reaches the shot, not just the numbers). */
    static void riflingHitsHarder(GameTestHelper h) {
        ServerPlayer player = TestPlayers.survival(h, new BlockPos(2, 1, 2));
        IronGolem golem = h.spawnWithNoFreeWill(EntityTypes.IRON_GOLEM, new BlockPos(2, 1, 8));
        player.lookAt(EntityAnchorArgument.Anchor.EYES, golem.getBoundingBox().getCenter());
        ItemStack gun = enchanted(h, new ItemStack(ModItems.REVOLVER.get()), ModEnchantments.RIFLING, 4);
        GunItem.setAmmo(gun, 1);
        player.setItemInHand(InteractionHand.MAIN_HAND, gun);
        gun.getItem().use(h.getLevel(), player, InteractionHand.MAIN_HAND);
        float lost = golem.getMaxHealth() - golem.getHealth();
        h.assertTrue(near(lost, 7.0F * 1.6F), "a Rifling IV revolver did " + lost);
        h.succeed();
    }

    /** Focus reaches the beam: a Focus IV ray pistol does 60% more, by the same road as Rifling. */
    static void focusHitsHarder(GameTestHelper h) {
        ServerPlayer player = TestPlayers.survival(h, new BlockPos(2, 1, 2));
        IronGolem golem = h.spawnWithNoFreeWill(EntityTypes.IRON_GOLEM, new BlockPos(2, 1, 8));
        player.lookAt(EntityAnchorArgument.Anchor.EYES, golem.getBoundingBox().getCenter());
        ItemStack gun = enchanted(h, new ItemStack(ModItems.RAY_PISTOL.get()), ModEnchantments.FOCUS, 4);
        ModItems.RAY_PISTOL.get().beam(h.getLevel(), player, gun);
        float lost = golem.getMaxHealth() - golem.getHealth();
        h.assertTrue(near(lost, GunTypes.RAY_PISTOL.damage() * 1.6F), "a Focus IV ray pistol did " + lost);
        h.succeed();
    }

    /**
     * The damage curve (33): each phlogiston gun out-hits and outlasts the black powder gun in its role, plain against plain and
     * enchanted against enchanted, and even a plain one keeps up more damage than that gun at its best.
     */
    static void phlogistonGunsStayAhead(GameTestHelper h) {
        Level level = h.getLevel();
        GunStats bestRevolver = powderAtBest(h, new ItemStack(ModItems.REVOLVER.get()), GunTypes.REVOLVER);
        GunStats bestRifle = powderAtBest(h, new ItemStack(ModItems.PLAINS_RIFLE.get()), GunTypes.RIFLE);
        GunStats bestPistol = GunEnchantments.apply(GunTypes.RAY_PISTOL, enchanted(h, new ItemStack(ModItems.RAY_PISTOL.get()), ModEnchantments.FOCUS, 4), level);
        GunStats bestPhlogiston = GunEnchantments.apply(GunTypes.PHLOGISTON_RIFLE,
                enchanted(h, new ItemStack(ModItems.PHLOGISTON_RIFLE.get()), ModEnchantments.FOCUS, 4), level);
        ahead(h, GunTypes.RAY_PISTOL, GunTypes.REVOLVER, "plain ray pistol");
        ahead(h, GunTypes.PHLOGISTON_RIFLE, GunTypes.RIFLE, "plain phlogiston rifle");
        ahead(h, bestPistol, bestRevolver, "best ray pistol");
        ahead(h, bestPhlogiston, bestRifle, "best phlogiston rifle");
        h.assertTrue(GunTypes.RAY_PISTOL.damagePerSecond() > bestRevolver.damagePerSecond(), "a plain ray pistol trails the best revolver");
        h.assertTrue(GunTypes.PHLOGISTON_RIFLE.damagePerSecond() > bestRifle.damagePerSecond(), "a plain phlogiston rifle trails the best plains rifle");
        h.succeed();
    }

    private static GunStats powderAtBest(GameTestHelper h, ItemStack gun, GunStats base) {
        enchanted(h, gun, ModEnchantments.RIFLING, 4);
        enchanted(h, gun, ModEnchantments.QUICK_LOAD, 3);
        return GunEnchantments.apply(base, gun, h.getLevel());
    }

    private static void ahead(GameTestHelper h, GunStats phlogiston, GunStats powder, String gun) {
        h.assertTrue(phlogiston.shotDamage() > powder.shotDamage(), gun + " hits for " + phlogiston.shotDamage() + " against " + powder.shotDamage());
        h.assertTrue(phlogiston.damagePerSecond() > powder.damagePerSecond(),
                gun + " keeps up " + phlogiston.damagePerSecond() + " a second against " + powder.damagePerSecond());
    }

    /** Every gun's tooltip says what it hits for, enchantments counted, a shotgun by the pellet. */
    static void gunTooltipsShowDamage(GameTestHelper h) {
        Level level = h.getLevel();
        h.assertValueEqual(shownDamage(level, new ItemStack(ModItems.PHLOGISTON_RIFLE.get())), "30", "phlogiston rifle's tooltip");
        h.assertValueEqual(shownDamage(level, new ItemStack(ModItems.SHOTGUN.get())), "3.5", "shotgun's tooltip");
        h.assertValueEqual(shownDamage(level, enchanted(h, new ItemStack(ModItems.REVOLVER.get()), ModEnchantments.RIFLING, 4)), "11.2", "Rifling IV revolver's tooltip");
        h.assertValueEqual(shownDamage(level, enchanted(h, new ItemStack(ModItems.RAY_PISTOL.get()), ModEnchantments.FOCUS, 4)), "16", "Focus IV ray pistol's tooltip");
        h.succeed();
    }

    private static String shownDamage(Level level, ItemStack gun) {
        for (Component line : gun.getTooltipLines(net.minecraft.world.item.Item.TooltipContext.of(level), null, TooltipFlag.NORMAL)) {
            if (line.getContents() instanceof TranslatableContents text && text.getKey().startsWith("tooltip.hollowfrontier.gun_damage")) {
                return String.valueOf(text.getArgs()[0]);
            }
        }
        return "no damage line";
    }

    /** Quick Load III loads a revolver chamber in 6 ticks, not 15. */
    static void quickLoadLoadsSooner(GameTestHelper h) {
        ServerPlayer player = TestPlayers.survival(h, new BlockPos(2, 1, 2));
        ItemStack gun = enchanted(h, new ItemStack(ModItems.REVOLVER.get()), ModEnchantments.QUICK_LOAD, 3);
        player.setItemInHand(InteractionHand.MAIN_HAND, gun);
        player.getInventory().add(new ItemStack(ModItems.REVOLVER_CARTRIDGE.get(), 4));
        player.getInventory().add(new ItemStack(ModItems.PERCUSSION_CAP.get(), 4));
        GunItem item = (GunItem) gun.getItem();
        h.assertTrue(item.startReload(player, gun), "reload did not start");
        long now = h.getLevel().getGameTime();
        GunItem.setReloadStart(gun, now - 5);
        item.tickReload(player, gun);
        h.assertValueEqual(GunItem.getAmmo(gun), 0, "chambers loaded after 5 ticks");
        GunItem.setReloadStart(gun, now - 6);
        item.tickReload(player, gun);
        h.assertValueEqual(GunItem.getAmmo(gun), 1, "chambers loaded after 6 ticks");
        h.succeed();
    }

    /** Frugal III saves a good share of rounds over three cylinders' loading (all 18 spent: about 1 in 50,000). */
    static void frugalSavesRounds(GameTestHelper h) {
        ServerPlayer player = TestPlayers.survival(h, new BlockPos(2, 1, 2));
        ItemStack gun = enchanted(h, new ItemStack(ModItems.REVOLVER.get()), ModEnchantments.FRUGAL, 3);
        player.setItemInHand(InteractionHand.MAIN_HAND, gun);
        player.getInventory().add(new ItemStack(ModItems.REVOLVER_CARTRIDGE.get(), 32));
        player.getInventory().add(new ItemStack(ModItems.PERCUSSION_CAP.get(), 32));
        GunItem item = (GunItem) gun.getItem();
        for (int chamber = 0; chamber < 18; chamber++) {
            if (!GunItem.isReloading(gun)) {
                if (GunItem.getAmmo(gun) >= GunTypes.REVOLVER.magazine()) {
                    GunItem.setAmmo(gun, 0);
                }
                h.assertTrue(item.startReload(player, gun), "reload did not start");
            }
            GunItem.setReloadStart(gun, h.getLevel().getGameTime() - GunTypes.REVOLVER.loadTicks());
            item.tickReload(player, gun);
        }
        int spent = 32 - count(player, ModItems.REVOLVER_CARTRIDGE.get());
        int capsSpent = 32 - count(player, ModItems.PERCUSSION_CAP.get());
        h.assertTrue(spent < 18 && spent == capsSpent, "18 chambers cost " + spent + " cartridges and " + capsSpent + " caps");
        h.succeed();
    }

    /**
     * Vanilla Infinity goes on the black powder guns (table and anvil) but not the phlogiston guns, never with Frugal; loading a
     * cylinder then spends nothing, though like a bow the gun still wants a cartridge and a cap to hand.
     */
    static void infinityLoadsForFree(GameTestHelper h) {
        Level level = h.getLevel();
        Holder<Enchantment> infinity = holder(level, Enchantments.INFINITY);
        for (ItemStack gun : new ItemStack[] {new ItemStack(ModItems.REVOLVER.get()), new ItemStack(ModItems.PLAINS_RIFLE.get()),
                new ItemStack(ModItems.SHOTGUN.get())}) {
            h.assertTrue(gun.supportsEnchantment(infinity) && gun.isPrimaryItemFor(infinity), gun + " won't take Infinity");
        }
        h.assertFalse(new ItemStack(ModItems.PHLOGISTON_RIFLE.get()).supportsEnchantment(infinity), "a phlogiston rifle takes Infinity");
        h.assertFalse(Enchantment.areCompatible(infinity, holder(level, ModEnchantments.FRUGAL)), "Infinity and Frugal go together");

        ServerPlayer player = TestPlayers.survival(h, new BlockPos(2, 1, 2));
        ItemStack gun = enchanted(h, new ItemStack(ModItems.REVOLVER.get()), Enchantments.INFINITY, 1);
        player.setItemInHand(InteractionHand.MAIN_HAND, gun);
        GunItem item = (GunItem) gun.getItem();
        h.assertFalse(item.startReload(player, gun), "an Infinity revolver loaded with nothing to hand");
        player.getInventory().add(new ItemStack(ModItems.REVOLVER_CARTRIDGE.get(), 1));
        player.getInventory().add(new ItemStack(ModItems.PERCUSSION_CAP.get(), 1));
        h.assertTrue(item.startReload(player, gun), "reload did not start");
        for (int chamber = 0; chamber < GunTypes.REVOLVER.magazine(); chamber++) {
            GunItem.setReloadStart(gun, h.getLevel().getGameTime() - GunTypes.REVOLVER.loadTicks());
            item.tickReload(player, gun);
        }
        h.assertValueEqual(GunItem.getAmmo(gun), GunTypes.REVOLVER.magazine(), "chambers loaded");
        h.assertTrue(count(player, ModItems.REVOLVER_CARTRIDGE.get()) == 1 && count(player, ModItems.PERCUSSION_CAP.get()) == 1,
                "an Infinity revolver spent ammo");
        h.succeed();
    }

    /** A town blacksmith sells one gun enchantment book, at I or II, for gold. */
    static void blacksmithSellsAGunBook(GameTestHelper h) {
        Townsfolk smith = h.spawnWithNoFreeWill(ModEntities.TOWNSFOLK.get(), new BlockPos(5, 1, 5));
        smith.setFolk(Folk.BLACKSMITH);
        long books = smith.getOffers().stream().filter(offer -> offer.getResult().is(Items.ENCHANTED_BOOK)
                && offer.getCostA().is(ModItems.GOLD_COIN.get())
                && EnchantmentHelper.getEnchantmentsForCrafting(offer.getResult()).entrySet().stream()
                        .allMatch(e -> e.getKey().is(ModTags.Enchantments.GUN) && e.getIntValue() <= 2)).count();
        h.assertValueEqual((int) books, 1, "gun books the blacksmith sells");
        h.succeed();
    }

    private static int count(Player player, net.minecraft.world.item.Item item) {
        int n = 0;
        for (int i = 0; i < player.getInventory().getContainerSize(); i++) {
            ItemStack s = player.getInventory().getItem(i);
            if (s.is(item)) n += s.getCount();
        }
        return n;
    }
}
