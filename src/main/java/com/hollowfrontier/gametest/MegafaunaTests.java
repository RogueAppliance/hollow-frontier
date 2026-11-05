package com.hollowfrontier.gametest;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import com.hollowfrontier.HollowFrontier;
import com.hollowfrontier.content.hollow.megafauna.Glyptodon;
import com.hollowfrontier.content.hollow.megafauna.MegafaunaGear;
import com.hollowfrontier.content.hollow.megafauna.Megatherium;
import com.hollowfrontier.content.hollow.megafauna.OliphantItem;
import com.hollowfrontier.content.hollow.megafauna.TerrorBird;
import com.hollowfrontier.content.hollow.megafauna.WoollyMammoth;
import com.hollowfrontier.registry.ModEntities;
import com.hollowfrontier.registry.ModItems;
import com.hollowfrontier.worldgen.HollowBiomes;

import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.attribute.EnvironmentAttributes;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.entity.animal.equine.Horse;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.MobSpawnSettings;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.LeavesBlock;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

/**
 * Phase 10a: the wild megafauna. Their spawns, drops and recipes, then each beast's behaviour (the mammoth herd closing
 * ranks, the terror bird's hunting, its fear of mammoths and how few it spawns, the megatherium browsing, the glyptodon
 * shelling up and being brushed) and what their drops make (the Oliphant, the Talon Knife's skinning, the shell armor's
 * sneak, the Hide Barding). The behaviour tests run on the 40-block field (grass at y 0).
 */
final class MegafaunaTests {
    private MegafaunaTests() {}

    /** Each spawns where its biome tag says (through its biome modifier), in its own category. */
    static void megafaunaSpawnInTheirBiomes(GameTestHelper h) {
        var biomes = h.getLevel().registryAccess().lookupOrThrow(Registries.BIOME);
        Map<EntityType<?>, List<ResourceKey<Biome>>> expected = Map.of(
                ModEntities.WOOLLY_MAMMOTH.get(), List.of(HollowBiomes.CONCAVIAN_HIGHLANDS, HollowBiomes.CRYSTAL_STEPPES),
                ModEntities.TERROR_BIRD.get(), List.of(HollowBiomes.CRYSTAL_STEPPES, HollowBiomes.EMBER_BADLANDS),
                ModEntities.MEGATHERIUM.get(), List.of(HollowBiomes.PRIMORDIAL_JUNGLE),
                ModEntities.GLYPTODON.get(), List.of(HollowBiomes.PRIMORDIAL_JUNGLE, HollowBiomes.CRYSTAL_STEPPES));
        expected.forEach((type, keys) -> {
            for (ResourceKey<Biome> key : keys) {
                MobSpawnSettings spawns = biomes.getOrThrow(key).value().getAttributes().applyModifier(EnvironmentAttributes.NATURAL_MOB_SPAWNS, MobSpawnSettings.EMPTY);
                h.assertTrue(spawns.getMobsToSpawn(type.getCategory()).unwrap().stream().anyMatch(w -> w.value().type() == type),
                        key.identifier() + " doesn't spawn " + type);
            }
        });
        h.assertTrue(ModEntities.TERROR_BIRD.get().getCategory() == net.minecraft.world.entity.MobCategory.MONSTER, "terror birds should be monsters");
        h.succeed();
    }

    /** Their recipes load: the Oliphant, the knife, the barding, the shell armor, the steak three ways, the spares. */
    static void megafaunaRecipesLoad(GameTestHelper h) {
        for (String id : List.of("oliphant", "bone_meal_from_mammoth_tusk", "mammoth_steak_from_smelting", "mammoth_steak_from_smoking",
                "mammoth_steak_from_campfire_cooking", "talon_knife", "hide_barding", "leather_from_thick_hide", "glyptodon_helmet",
                "glyptodon_chestplate", "glyptodon_leggings", "glyptodon_boots")) {
            ResourceKey<Recipe<?>> key = ResourceKey.create(Registries.RECIPE, HollowFrontier.id(id));
            h.assertTrue(h.getLevel().recipeAccess().byKey(key).isPresent(), "missing recipe " + id);
        }
        h.succeed();
    }

    /** Killed by a player, each drops its own: a tusk, a talon, thick hide, scutes. */
    static void megafaunaDropTheirOwn(GameTestHelper h) {
        ServerPlayer player = TestPlayers.survival(h, new BlockPos(20, 1, 2));
        Map<LivingEntity, Item> beasts = Map.of(
                h.spawnWithNoFreeWill(ModEntities.WOOLLY_MAMMOTH.get(), new BlockPos(6, 1, 8)), ModItems.MAMMOTH_TUSK.get(),
                h.spawnWithNoFreeWill(ModEntities.TERROR_BIRD.get(), new BlockPos(30, 1, 8)), ModItems.TERROR_BIRD_TALON.get(),
                h.spawnWithNoFreeWill(ModEntities.MEGATHERIUM.get(), new BlockPos(6, 1, 30)), ModItems.THICK_HIDE.get(),
                h.spawnWithNoFreeWill(ModEntities.GLYPTODON.get(), new BlockPos(30, 1, 30)), ModItems.GLYPTODON_SCUTE.get());
        Map<Item, Vec3> where = new java.util.HashMap<>();
        beasts.forEach((beast, item) -> {
            where.put(item, beast.position());
            beast.hurtServer(h.getLevel(), h.getLevel().damageSources().playerAttack(player), 10_000.0F);
        });
        h.succeedWhen(() -> where.forEach((item, pos) -> h.assertTrue(!h.getLevel().getEntitiesOfClass(ItemEntity.class, new AABB(pos, pos).inflate(4),
                drop -> drop.getItem().is(item)).isEmpty(), "no " + item + " dropped")));
    }

    /** Hurt one mammoth and the rest of the herd turns on you too, trumpeting. */
    static void mammothHerdDefends(GameTestHelper h) {
        WoollyMammoth hurt = h.spawn(ModEntities.WOOLLY_MAMMOTH.get(), new BlockPos(12, 1, 20));
        WoollyMammoth herd = h.spawn(ModEntities.WOOLLY_MAMMOTH.get(), new BlockPos(24, 1, 20));
        ServerPlayer player = TestPlayers.survival(h, new BlockPos(12, 1, 32));
        // Not on its first tick: HurtByTargetGoal ignores a hurt stamped tick 0, its own starting stamp
        h.runAfterDelay(5, () -> hurt.hurtServer(h.getLevel(), h.getLevel().damageSources().playerAttack(player), 1.0F));
        h.succeedWhen(() -> h.assertTrue(herd.getTarget() == player && hurt.getTarget() == player,
                "the herd didn't take up the quarrel (" + hurt.getTarget() + ", " + herd.getTarget() + "; hurt by " + hurt.getLastHurtByMob() + ", health " + hurt.getHealth() + ", player alive " + player.isAlive() + ")"));
    }

    /** The Oliphant calms an angry mammoth and puts a terror bird to flight, which then runs off. */
    static void oliphantCalmsAndScatters(GameTestHelper h) {
        ServerPlayer player = TestPlayers.survival(h, new BlockPos(20, 1, 20));
        WoollyMammoth mammoth = h.spawn(ModEntities.WOOLLY_MAMMOTH.get(), new BlockPos(8, 1, 20));
        TerrorBird bird = h.spawn(ModEntities.TERROR_BIRD.get(), new BlockPos(26, 1, 20));
        mammoth.setTarget(player);
        bird.setTarget(player);
        h.assertTrue(OliphantItem.blow(h.getLevel(), player) == 2, "the Oliphant should reach both");
        h.assertTrue(mammoth.getTarget() == null, "the mammoth is still angry");
        h.assertTrue(bird.isScared() && bird.getTarget() == null, "the terror bird isn't scared");
        double before = bird.distanceTo(player);
        h.runAfterDelay(80, () -> {
            h.assertTrue(bird.distanceTo(player) > before + 4, "the bird didn't run off: " + before + " -> " + bird.distanceTo(player));
            h.assertTrue(mammoth.getTarget() == null, "the mammoth took up its quarrel again");
            h.succeed();
        });
    }

    /** A terror bird picks out a player and hunts them. */
    static void terrorBirdHunts(GameTestHelper h) {
        TerrorBird bird = h.spawn(ModEntities.TERROR_BIRD.get(), new BlockPos(8, 1, 20));
        ServerPlayer player = TestPlayers.survival(h, new BlockPos(28, 1, 20));
        h.succeedWhen(() -> h.assertTrue(bird.getTarget() == player && bird.distanceTo(player) < 8,
                "the bird isn't hunting the player (target " + bird.getTarget() + ", " + bird.distanceTo(player) + " away)"));
    }

    /** It won't stay near a mammoth. */
    static void terrorBirdShunsMammoths(GameTestHelper h) {
        h.spawnWithNoFreeWill(ModEntities.WOOLLY_MAMMOTH.get(), new BlockPos(20, 1, 20));
        TerrorBird bird = h.spawn(ModEntities.TERROR_BIRD.get(), new BlockPos(24, 1, 20));
        WoollyMammoth mammoth = h.getLevel().getEntitiesOfClass(WoollyMammoth.class, bird.getBoundingBox().inflate(12)).getFirst();
        h.succeedWhen(() -> h.assertTrue(bird.distanceTo(mammoth) > TerrorBird.MAMMOTH_BERTH - 1,
                "the bird stayed by the mammoth: " + bird.distanceTo(mammoth)));
    }

    /** Natural spawning keeps them few: two in range is a crowd (checked over a few blocks, as other tests have birds about). */
    static void terrorBirdsStayFew(GameTestHelper h) {
        BlockPos pos = h.absolutePos(new BlockPos(20, 1, 20));
        h.spawnWithNoFreeWill(ModEntities.TERROR_BIRD.get(), new BlockPos(18, 1, 20));
        h.assertFalse(TerrorBird.crowded(h.getLevel(), pos, 4.0), "one bird shouldn't be a crowd");
        h.spawnWithNoFreeWill(ModEntities.TERROR_BIRD.get(), new BlockPos(22, 1, 20));
        h.assertTrue(TerrorBird.crowded(h.getLevel(), pos, 4.0), "two birds should be a crowd");
        h.succeed();
    }

    /** A megatherium finds a tree's leaves within reach, rears up, browses and strips them. */
    static void megatheriumBrowses(GameTestHelper h) {
        BlockPos leaves = new BlockPos(20, 4, 20);
        for (int y = 1; y < 4; y++) {
            h.setBlock(new BlockPos(20, y, 20), Blocks.JUNGLE_LOG);
        }
        h.setBlock(leaves, Blocks.JUNGLE_LEAVES.defaultBlockState().setValue(LeavesBlock.DISTANCE, 1));
        h.assertTrue(Megatherium.isBrowse(h.getBlockState(leaves)), "a tree's leaves should be browse");
        h.assertFalse(Megatherium.isBrowse(Blocks.JUNGLE_LEAVES.defaultBlockState().setValue(LeavesBlock.PERSISTENT, true)),
                "a player's leaves shouldn't be browse");
        Megatherium sloth = h.spawn(ModEntities.MEGATHERIUM.get(), new BlockPos(14, 1, 20));
        sloth.setHomeTo(h.absolutePos(new BlockPos(20, 1, 20)), 6); // keeps it from strolling out of reach of the one tree
        boolean[] reared = {false};
        h.onEachTick(() -> reared[0] |= sloth.isRearing());
        h.succeedWhen(() -> h.assertTrue(reared[0] && h.getBlockState(leaves).isAir(),
                "it didn't browse the leaves (reared " + reared[0] + ", leaves " + h.getBlockState(leaves) + ")"));
    }

    /**
     * The tick after a browse ends. Vanilla only asks {@code canContinueToUse} on every other tick, so a goal that wants every tick
     * gets one more {@code tick} after letting go of its leaves; that one crashed the server, from either way of letting go.
     */
    static void browseSurvivesTheTickAfter(GameTestHelper h) {
        BlockPos leaves = new BlockPos(20, 4, 20);
        for (int y = 1; y < 4; y++) {
            h.setBlock(new BlockPos(20, y, 20), Blocks.JUNGLE_LOG);
        }
        h.setBlock(leaves, Blocks.JUNGLE_LEAVES.defaultBlockState().setValue(LeavesBlock.DISTANCE, 1));

        // Couldn't get there: its path ends with the leaves still out of reach
        Megatherium stuck = h.spawnWithNoFreeWill(ModEntities.MEGATHERIUM.get(), new BlockPos(26, 1, 20));
        Megatherium.BrowseGoal giveUp = browsing(h, stuck);
        stuck.getNavigation().stop();
        giveUp.tick();
        h.assertFalse(giveUp.canContinueToUse(), "it should give up on leaves it can't reach");
        giveUp.tick();

        // Ate them: rears beside the tree, browses, strips the leaves
        Megatherium fed = h.spawnWithNoFreeWill(ModEntities.MEGATHERIUM.get(), new BlockPos(20, 1, 22));
        Megatherium.BrowseGoal eat = browsing(h, fed);
        for (int i = 0; i < 1000 && eat.canContinueToUse(); i++) {
            eat.tick();
        }
        h.assertTrue(h.getBlockState(leaves).isAir(), "it didn't strip the leaves");
        eat.tick();
        h.succeed();
    }

    // canUse is a chance per tick, so asks until it comes up; the goal is then under way on the leaves it found
    private static Megatherium.BrowseGoal browsing(GameTestHelper h, Megatherium sloth) {
        Megatherium.BrowseGoal goal = new Megatherium.BrowseGoal(sloth);
        for (int i = 0; i < 10000; i++) {
            if (goal.canUse()) {
                goal.start();
                return goal;
            }
        }
        throw h.assertionException("the megatherium never found the leaves");
    }

    /** Hurt, a glyptodon hunkers down, and then lets only a quarter of the damage through. */
    static void glyptodonHunkers(GameTestHelper h) {
        Glyptodon glyptodon = h.spawn(ModEntities.GLYPTODON.get(), new BlockPos(20, 1, 20));
        ServerLevel level = h.getLevel();
        h.assertFalse(glyptodon.isHunkered(), "it shouldn't start hunkered");
        glyptodon.hurtServer(level, level.damageSources().magic(), 8.0F);
        h.assertTrue(glyptodon.isHunkered(), "it didn't hunker when hurt");
        float afterFirst = glyptodon.getHealth();
        h.assertTrue(Math.abs(glyptodon.getMaxHealth() - afterFirst - 8.0F) < 0.01F, "the first hit should land in full: " + afterFirst);
        h.runAfterDelay(25, () -> {
            glyptodon.hurtServer(level, level.damageSources().magic(), 8.0F);
            float taken = afterFirst - glyptodon.getHealth();
            h.assertTrue(Math.abs(taken - 8.0F * Glyptodon.HUNKERED_DAMAGE) < 0.01F, "hunkered, it took " + taken);
            h.assertTrue(glyptodon.getDeltaMovement().horizontalDistance() < 0.01, "it should hold still hunkered");
            h.succeed();
        });
    }

    /** A brush takes a scute off it, then not again for a while. */
    static void glyptodonBrushedForScute(GameTestHelper h) {
        Glyptodon glyptodon = h.spawnWithNoFreeWill(ModEntities.GLYPTODON.get(), new BlockPos(20, 1, 20));
        ServerPlayer player = TestPlayers.survival(h, new BlockPos(20, 1, 17));
        player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(Items.BRUSH));
        h.assertTrue(glyptodon.mobInteract(player, InteractionHand.MAIN_HAND) == InteractionResult.SUCCESS, "brushing failed");
        h.assertFalse(glyptodon.canBeBrushed(), "it should need time before the next brushing");
        h.assertTrue(glyptodon.mobInteract(player, InteractionHand.MAIN_HAND) == InteractionResult.FAIL, "it gave a second scute at once");
        h.succeedWhen(() -> h.assertTrue(h.getLevel().getEntitiesOfClass(ItemEntity.class, glyptodon.getBoundingBox().inflate(3),
                item -> item.getItem().is(ModItems.GLYPTODON_SCUTE.get())).size() == 1, "there should be one scute"));
    }

    /** The Talon Knife skins one more of the first hide-like drop; any other weapon doesn't. */
    static void talonKnifeSkins(GameTestHelper h) {
        ServerPlayer player = TestPlayers.survival(h, new BlockPos(20, 1, 20));
        List<ItemEntity> drops = new ArrayList<>();
        Vec3 at = h.absoluteVec(new Vec3(20, 1, 22));
        drops.add(new ItemEntity(h.getLevel(), at.x, at.y, at.z, new ItemStack(Items.FEATHER, 2)));
        drops.add(new ItemEntity(h.getLevel(), at.x, at.y, at.z, new ItemStack(ModItems.THICK_HIDE.get(), 2)));
        player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(Items.IRON_SWORD));
        h.assertFalse(MegafaunaGear.skin(player, drops), "an iron sword shouldn't skin");
        player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(ModItems.TALON_KNIFE.get()));
        h.assertTrue(MegafaunaGear.skin(player, drops), "the knife didn't skin");
        h.assertTrue(drops.get(0).getItem().getCount() == 2 && drops.get(1).getItem().getCount() == 3,
                "wrong drops: " + drops.get(0).getItem() + ", " + drops.get(1).getItem());
        h.succeed();
    }

    /** In the full shell, sneaking: arrows do a quarter, explosions half; standing, or a piece short, the full amount. */
    static void shellArmorTurnsArrows(GameTestHelper h) {
        ServerPlayer player = TestPlayers.survival(h, new BlockPos(20, 1, 20));
        player.setItemSlot(EquipmentSlot.HEAD, new ItemStack(ModItems.GLYPTODON_HELMET.get()));
        player.setItemSlot(EquipmentSlot.CHEST, new ItemStack(ModItems.GLYPTODON_CHESTPLATE.get()));
        player.setItemSlot(EquipmentSlot.LEGS, new ItemStack(ModItems.GLYPTODON_LEGGINGS.get()));
        player.setItemSlot(EquipmentSlot.FEET, new ItemStack(ModItems.GLYPTODON_BOOTS.get()));
        var types = h.getLevel().registryAccess().lookupOrThrow(Registries.DAMAGE_TYPE);
        DamageSource arrow = new DamageSource(types.getOrThrow(DamageTypes.ARROW));
        DamageSource blast = h.getLevel().damageSources().explosion(null, null);
        h.assertTrue(MegafaunaGear.shellFactor(player, arrow) == 1.0F, "standing, the shell shouldn't apply");
        player.setPose(Pose.CROUCHING);
        h.assertTrue(MegafaunaGear.shellFactor(player, arrow) == MegafaunaGear.SHELL_PROJECTILE, "arrows: " + MegafaunaGear.shellFactor(player, arrow));
        h.assertTrue(MegafaunaGear.shellFactor(player, blast) == MegafaunaGear.SHELL_EXPLOSION, "explosions: " + MegafaunaGear.shellFactor(player, blast));
        h.assertTrue(MegafaunaGear.shellFactor(player, h.getLevel().damageSources().generic()) == 1.0F, "other damage shouldn't be cut");
        player.setItemSlot(EquipmentSlot.FEET, ItemStack.EMPTY);
        h.assertTrue(MegafaunaGear.shellFactor(player, arrow) == 1.0F, "a piece short, the shell shouldn't apply");
        h.succeed();
    }

    /** Hide Barding is horse armor, worth 9. */
    static void hideBardingArmorsHorse(GameTestHelper h) {
        Horse horse = h.spawnWithNoFreeWill(EntityTypes.HORSE, new BlockPos(20, 1, 20));
        horse.setTamed(true);
        horse.setItemSlot(EquipmentSlot.BODY, new ItemStack(ModItems.HIDE_BARDING.get()));
        h.succeedWhen(() -> h.assertTrue(horse.getArmorValue() >= 9, "the barding gives " + horse.getArmorValue() + " armor"));
    }
}
