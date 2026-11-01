package com.hollowfrontier.gametest;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;

import com.hollowfrontier.content.frontier.gazette.GazetteBackPage;
import com.hollowfrontier.content.frontier.talk.ChatterLines;
import com.hollowfrontier.content.frontier.talk.Context;
import com.hollowfrontier.content.frontier.talk.Fact;
import com.hollowfrontier.content.frontier.weather.DustDevil;
import com.hollowfrontier.content.frontier.weather.DustDevils;
import com.hollowfrontier.content.hollow.creature.AshSalamander;
import com.hollowfrontier.registry.ModAttachments;
import com.hollowfrontier.registry.ModEntities;
import com.hollowfrontier.registry.ModItems;
import com.hollowfrontier.worldgen.HollowBiomes;

import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.animal.chicken.Chicken;
import net.minecraft.world.entity.animal.equine.Horse;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.Biomes;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.gamerules.GameRules;
import net.minecraft.world.phys.Vec3;

/**
 * Phase 29's dust devils: where they blow, what they lift and fling (players let down gently), the hats they take, the
 * horses they spook, what they leave, the Ash Devil's fire, and what the frontier says about them.
 */
final class DustDevilTests {
    private DustDevilTests() {}

    static final BlockPos CENTRE = new BlockPos(20, 1, 20);

    private static DustDevil devil(GameTestHelper h, boolean ash) {
        Vec3 at = h.absoluteVec(Vec3.atBottomCenterOf(CENTRE));
        EntityType<DustDevil> type = ash ? ModEntities.ASH_DEVIL.get() : ModEntities.DUST_DEVIL.get();
        return DustDevil.spawn(h.getLevel(), type, at, DustDevil.MAX_LIFE).fixed();
    }

    /** Desert, badlands and savanna have dust devils, the Ember Badlands Ash Devils; nowhere else either. */
    static void theirCountry(GameTestHelper h) {
        var biomes = h.getLevel().registryAccess().lookupOrThrow(Registries.BIOME);
        for (ResourceKey<Biome> dry : List.of(Biomes.DESERT, Biomes.BADLANDS, Biomes.ERODED_BADLANDS, Biomes.SAVANNA)) {
            h.assertTrue(biomes.getOrThrow(dry).is(DustDevils.HAS_DUST_DEVILS), dry.identifier() + " has no dust devils");
        }
        for (ResourceKey<Biome> wet : List.of(Biomes.PLAINS, Biomes.FOREST, Biomes.OCEAN, Biomes.SWAMP, Biomes.SNOWY_PLAINS)) {
            h.assertFalse(biomes.getOrThrow(wet).is(DustDevils.HAS_DUST_DEVILS), wet.identifier() + " has dust devils");
        }
        h.assertTrue(biomes.getOrThrow(HollowBiomes.EMBER_BADLANDS).is(DustDevils.HAS_ASH_DEVILS), "no Ash Devils in the Ember Badlands");
        h.assertFalse(biomes.getOrThrow(HollowBiomes.PRIMORDIAL_JUNGLE).is(DustDevils.HAS_ASH_DEVILS), "Ash Devils in the jungle");
        h.assertFalse(biomes.getOrThrow(HollowBiomes.EMBER_BADLANDS).is(DustDevils.HAS_DUST_DEVILS), "dust devils underground");
        // on the test field a devil blows just as its biome and the sky allow; over water, never
        BlockPos foot = h.absolutePos(CENTRE);
        boolean allowed = h.getLevel().getBiome(foot).is(DustDevils.HAS_DUST_DEVILS) && !h.getLevel().isRaining() && !h.getLevel().isDarkOutside();
        h.assertValueEqual(DustDevils.canBlowAt(h.getLevel(), foot, false), allowed, "blowing on the test field");
        h.assertFalse(DustDevils.canBlowAt(h.getLevel(), foot, true), "an Ash Devil blows in the Overworld");
        h.setBlock(CENTRE, Blocks.WATER);
        h.assertFalse(DustDevils.canBlowAt(h.getLevel(), foot, true), "a devil blows over water");
        h.succeed();
    }

    /** Things dropped at its foot and small creatures go up it; a cow is too big; the Ash Devil leaves salamanders be. */
    static void liftsItemsAndCritters(GameTestHelper h) {
        DustDevil devil = devil(h, false);
        ItemEntity stick = new ItemEntity(h.getLevel(), devil.getX() + 1.0, devil.getY() + 0.1, devil.getZ(), new ItemStack(Items.STICK));
        h.getLevel().addFreshEntity(stick);
        Chicken hen = h.spawn(EntityTypes.CHICKEN, CENTRE.east());
        var cow = h.spawn(EntityTypes.COW, CENTRE.west());
        AtomicBoolean cowHeld = new AtomicBoolean();
        h.onEachTick(() -> cowHeld.compareAndSet(false, devil.holds(cow)));
        h.succeedWhen(() -> {
            h.assertTrue(stick.getY() > devil.getY() + 2.5, "the stick only got to " + (stick.getY() - devil.getY()));
            h.assertTrue(hen.getY() > devil.getY() + 2.5, "the hen only got to " + (hen.getY() - devil.getY()));
            h.assertFalse(cowHeld.get(), "a cow went up the whirl");
            devil.discard();
        });
    }

    /** What reaches the top is flung out, the living with slow falling; it lets go of everything when it dies down. */
    static void flingsAndLetsGo(GameTestHelper h) {
        DustDevil devil = devil(h, false);
        Chicken hen = h.spawn(EntityTypes.CHICKEN, CENTRE.east());
        AtomicBoolean flung = new AtomicBoolean();
        h.onEachTick(() -> {
            if (hen.hasEffect(MobEffects.SLOW_FALLING) && !devil.holds(hen)) {
                flung.set(true);
            }
        });
        h.succeedWhen(() -> {
            h.assertTrue(flung.get(), "the hen was never flung out (held " + devil.holds(hen) + ", up " + (hen.getY() - devil.getY()) + ")");
            devil.finish(h.getLevel());
            h.assertTrue(devil.isRemoved(), "still there after it finished");
        });
    }

    /** A player who isn't sneaking is lifted and let down gently, and loses their hat downwind. */
    static void liftsPlayerAndTakesHat(GameTestHelper h) {
        DustDevil devil = devil(h, false);
        ServerPlayer player = TestPlayers.survival(h, CENTRE.north());
        player.setItemSlot(EquipmentSlot.HEAD, new ItemStack(ModItems.COWBOY_HAT.get()));
        h.succeedWhen(() -> {
            h.assertTrue(player.hasEffect(MobEffects.LEVITATION), "not lifted");
            h.assertTrue(player.hasEffect(MobEffects.SLOW_FALLING), "no soft landing to come");
            h.assertTrue(player.getItemBySlot(EquipmentSlot.HEAD).isEmpty(), "the hat stayed on");
            List<ItemEntity> hats = h.getLevel().getEntitiesOfClass(ItemEntity.class, player.getBoundingBox().inflate(6),
                    i -> i.getItem().is(ModItems.COWBOY_HAT.get()));
            h.assertTrue(!hats.isEmpty(), "the hat went nowhere");
            h.assertTrue(player.getData(ModAttachments.HAT_LOST.get()) >= 0, "the lost hat wasn't noted");
            h.assertTrue(Context.of(h.getLevel(), player.blockPosition(), player).has(Fact.LOST_HAT), "nobody would remark on it");
            devil.discard();
        });
    }

    /** Keeping low gets you a shove, not a ride, and keeps your hat on; so does Curse of Binding, and so do the creative. */
    static void sneakingAndBinding(GameTestHelper h) {
        DustDevil devil = devil(h, false);
        ServerPlayer low = TestPlayers.survival(h, CENTRE.north());
        low.setShiftKeyDown(true);
        low.setItemSlot(EquipmentSlot.HEAD, new ItemStack(ModItems.COWBOY_HAT.get()));
        ServerPlayer bound = TestPlayers.survival(h, CENTRE.south());
        ItemStack cursed = new ItemStack(ModItems.COWBOY_HAT.get());
        cursed.enchant(h.getLevel().registryAccess().lookupOrThrow(Registries.ENCHANTMENT).getOrThrow(Enchantments.BINDING_CURSE), 1);
        bound.setItemSlot(EquipmentSlot.HEAD, cursed);
        ServerPlayer creative = TestPlayers.creative(h, CENTRE.east());
        h.runAfterDelay(DustDevil.SPIN_UP + 20, () -> {
            h.assertFalse(low.hasEffect(MobEffects.LEVITATION), "a sneaking player was lifted");
            h.assertTrue(low.getItemBySlot(EquipmentSlot.HEAD).is(ModItems.COWBOY_HAT.get()), "a sneaking player lost their hat");
            h.assertTrue(bound.hasEffect(MobEffects.LEVITATION), "the bound player wasn't lifted");
            h.assertTrue(bound.getItemBySlot(EquipmentSlot.HEAD).is(ModItems.COWBOY_HAT.get()), "a bound hat blew off");
            h.assertFalse(creative.hasEffect(MobEffects.LEVITATION), "a creative player was lifted");
            devil.discard();
            h.succeed();
        });
    }

    /** An unshod horse rears in it; a shod one keeps its feet. */
    static void horseshoesSteady(GameTestHelper h) {
        DustDevil devil = devil(h, false);
        Horse bare = h.spawn(EntityTypes.HORSE, CENTRE.north());
        Horse shod = h.spawn(EntityTypes.HORSE, CENTRE.south());
        shod.setData(ModAttachments.HORSESHOE_TIER.get(), 1);
        AtomicBoolean bareReared = new AtomicBoolean(), shodReared = new AtomicBoolean();
        h.onEachTick(() -> {
            bareReared.compareAndSet(false, bare.isStanding());
            shodReared.compareAndSet(false, shod.isStanding());
        });
        h.runAfterDelay(DustDevil.SPIN_UP + 30, () -> {
            h.assertTrue(bareReared.get(), "the unshod horse never reared");
            h.assertFalse(shodReared.get(), "the shod horse reared");
            devil.discard();
            h.succeed();
        });
    }

    /** It leaves a few odds and ends from its own table, and nothing else. */
    static void leavesItsLoot(GameTestHelper h) {
        Set<Item> allowed = Set.of(Items.STICK, Items.DEAD_BUSH, Items.FEATHER, Items.RABBIT_HIDE, ModItems.COPPER_COIN.get(),
                ModItems.SNAKESKIN.get(), ModItems.FRONTIER_GAZETTE.get(), ModItems.GOLD_COIN.get(), ModItems.BANDANA.get());
        Set<Item> seen = new HashSet<>();
        int empty = 0;
        for (int i = 0; i < 300; i++) {
            List<ItemStack> loot = DustDevils.loot(h.getLevel(), h.absoluteVec(Vec3.atCenterOf(CENTRE)));
            if (loot.isEmpty()) {
                empty++;
            }
            loot.forEach(s -> seen.add(s.getItem()));
        }
        h.assertTrue(allowed.containsAll(seen), "unexpected loot: " + seen);
        h.assertTrue(seen.size() >= 5, "too little variety: " + seen);
        h.assertTrue(empty > 0 && empty < 250, "empty " + empty + " of 300");
        h.succeed();
    }

    /** An Ash Devil sets light to what it carries and to the ground it crosses (where fire may spread). */
    static void ashDevilBurns(GameTestHelper h) {
        GameRules rules = h.getLevel().getGameRules();
        int radius = rules.get(GameRules.FIRE_SPREAD_RADIUS_AROUND_PLAYER);
        rules.set(GameRules.FIRE_SPREAD_RADIUS_AROUND_PLAYER, -1, h.getLevel().getServer());
        for (BlockPos pos : BlockPos.betweenClosed(CENTRE.offset(-3, -1, -3), CENTRE.offset(3, -1, 3))) {
            h.setBlock(pos, Blocks.OAK_PLANKS);
        }
        DustDevil devil = devil(h, true);
        Chicken hen = h.spawn(EntityTypes.CHICKEN, CENTRE.east());
        AshSalamander salamander = h.spawn(ModEntities.ASH_SALAMANDER.get(), CENTRE.west());
        AtomicBoolean henBurned = new AtomicBoolean(), salamanderHeld = new AtomicBoolean();
        h.onEachTick(() -> {
            henBurned.compareAndSet(false, hen.isOnFire());
            salamanderHeld.compareAndSet(false, devil.holds(salamander));
        });
        h.succeedWhen(() -> {
            h.assertTrue(henBurned.get(), "the hen didn't catch");
            h.assertFalse(salamanderHeld.get(), "the Ash Devil took the salamander");
            boolean fire = false;
            for (BlockPos pos : BlockPos.betweenClosed(CENTRE.offset(-4, 0, -4), CENTRE.offset(4, 1, 4))) {
                fire |= h.getBlockState(pos).is(Blocks.FIRE);
            }
            h.assertTrue(fire, "nothing caught alight");
            devil.discard();
            rules.set(GameRules.FIRE_SPREAD_RADIUS_AROUND_PLAYER, radius, h.getLevel().getServer());
            for (BlockPos pos : BlockPos.betweenClosed(CENTRE.offset(-6, 0, -6), CENTRE.offset(6, 1, 6))) {
                if (h.getBlockState(pos).is(Blocks.FIRE)) {
                    h.setBlock(pos, Blocks.AIR);
                }
            }
        });
    }

    /** The townsfolk talk about one that came through today, and not about one far off; there are lines for both new facts. */
    static void theFrontierTalks(GameTestHelper h) {
        BlockPos here = h.absolutePos(CENTRE);
        BlockPos far = here.offset(20000, 0, 20000);
        h.assertFalse(Context.of(h.getLevel(), far, null).has(Fact.DUST_DEVIL), "talk of a dust devil nobody saw");
        DustDevils.record(h.getLevel(), far);
        h.assertTrue(Context.of(h.getLevel(), far, null).has(Fact.DUST_DEVIL), "nobody talks of the dust devil");
        h.assertFalse(Context.of(h.getLevel(), far.offset(1000, 0, 0), null).has(Fact.DUST_DEVIL), "talk of one a thousand blocks off");
        for (Fact fact : List.of(Fact.DUST_DEVIL, Fact.LOST_HAT)) {
            h.assertTrue(ChatterLines.lines().stream().filter(l -> l.cond().all().contains(fact)).count() >= 3, "too few lines for " + fact);
            h.assertTrue(ChatterLines.exchanges().stream().anyMatch(e -> e.cond().all().contains(fact)), "no gossip about " + fact);
        }
        h.assertTrue(ChatterLines.lines().stream().anyMatch(l -> l.voice().equals("man_in_black") && l.text().contains("ust devil")),
                "the men in black don't blame the dust devils");
        h.assertTrue(GazetteBackPage.pieces().stream().filter(p -> p.text().contains("dust devil") || p.cond().all().contains(Fact.DUST_DEVIL)).count() >= 3,
                "the Gazette hasn't heard of dust devils");
        ServerPlayer player = TestPlayers.survival(h, CENTRE);
        h.assertFalse(Context.of(h.getLevel(), here, player).has(Fact.LOST_HAT), "a hatless stranger who never had one");
        player.setData(ModAttachments.HAT_LOST.get(), h.getLevel().getGameTime());
        h.assertTrue(Context.of(h.getLevel(), here, player).has(Fact.LOST_HAT), "the lost hat goes unremarked");
        player.setItemSlot(EquipmentSlot.HEAD, new ItemStack(ModItems.COWBOY_HAT.get()));
        h.assertFalse(Context.of(h.getLevel(), here, player).has(Fact.LOST_HAT), "remarked on with a hat back on");
        h.succeed();
    }
}
