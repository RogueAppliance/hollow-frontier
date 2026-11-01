package com.hollowfrontier.gametest;

import java.util.List;

import com.hollowfrontier.HollowFrontier;
import com.hollowfrontier.content.hollow.creature.GlassbackHare;
import com.hollowfrontier.content.hollow.gray.GraySentinel;
import com.hollowfrontier.content.hollow.gray.SignalLocatorItem;
import com.hollowfrontier.content.hollow.gray.Specimen;
import com.hollowfrontier.content.hollow.gray.Specimens;
import com.hollowfrontier.content.hollow.megafauna.Glyptodon;
import com.hollowfrontier.content.hollow.megafauna.OliphantItem;
import com.hollowfrontier.content.hollow.megafauna.TerrorBird;
import com.hollowfrontier.content.hollow.megafauna.WoollyMammoth;
import com.hollowfrontier.registry.ModBlocks;
import com.hollowfrontier.registry.ModEntities;
import com.hollowfrontier.registry.ModItems;
import com.hollowfrontier.worldgen.structure.HollowBlueprints;
import com.hollowfrontier.worldgen.structure.ModChestLoot;
import com.hollowfrontier.worldgen.structure.ModStructures;

import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.RandomSource;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.ChestBlockEntity;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructurePlaceSettings;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplate;
import net.minecraft.world.level.storage.loot.LootTable;
import net.minecraft.world.phys.AABB;

/**
 * Phase 10b: the Gray experiments. The tagged megafauna (tougher, quicker, driven: hunting players, deaf to the Oliphant,
 * no fear of mammoths, no hunkering; fragments when killed; a few wild ones escaped), the station's sentinel (it stuns and
 * raises the alarm, and holds its post), the live station's contents, the Signal Locator and the data. The behaviour tests
 * run on the 40-block field.
 */
final class GrayTests {
    private GrayTests() {}

    private static <T extends Mob & Specimen> T tagged(GameTestHelper h, EntityType<T> type, BlockPos pos, boolean freeWill) {
        T mob = freeWill ? h.spawn(type, pos) : h.spawnWithNoFreeWill(type, pos);
        mob.setTagged(true);
        return mob;
    }

    /** Tagged: half again the health (and healed to it), six more armor, a fifth quicker (the bird a quarter); untagged, back. */
    static void taggedBeastsAreTougher(GameTestHelper h) {
        List<EntityType<? extends Mob>> types = List.of(ModEntities.WOOLLY_MAMMOTH.get(), ModEntities.TERROR_BIRD.get(),
                ModEntities.MEGATHERIUM.get(), ModEntities.GLYPTODON.get());
        int x = 4;
        for (EntityType<? extends Mob> type : types) {
            Mob mob = h.spawnWithNoFreeWill(type, new BlockPos(x, 1, 20));
            x += 9;
            double health = mob.getMaxHealth(), armor = mob.getAttributeValue(Attributes.ARMOR), speed = mob.getAttributeValue(Attributes.MOVEMENT_SPEED);
            Specimen specimen = (Specimen) mob;
            h.assertFalse(specimen.isTagged(), type + " shouldn't start tagged");
            specimen.setTagged(true);
            h.assertTrue(Math.abs(mob.getMaxHealth() - health * 1.5) < 0.01 && mob.getHealth() == mob.getMaxHealth(),
                    type + " health " + mob.getHealth() + "/" + mob.getMaxHealth() + " (was " + health + ")");
            h.assertTrue(Math.abs(mob.getAttributeValue(Attributes.ARMOR) - armor - 6) < 0.01, type + " armor " + mob.getAttributeValue(Attributes.ARMOR));
            double quicker = mob instanceof TerrorBird ? 1.25 : 1.2;
            h.assertTrue(Math.abs(mob.getAttributeValue(Attributes.MOVEMENT_SPEED) - speed * quicker) < 0.001, type + " speed " + mob.getAttributeValue(Attributes.MOVEMENT_SPEED));
            specimen.setTagged(false);
            h.assertTrue(Math.abs(mob.getMaxHealth() - health) < 0.01 && Math.abs(mob.getAttributeValue(Attributes.ARMOR) - armor) < 0.01,
                    type + " kept its boosts untagged");
        }
        h.succeed();
    }

    /** A tagged mammoth goes for a player on sight (an untagged one wouldn't). */
    static void taggedMammothHunts(GameTestHelper h) {
        WoollyMammoth mammoth = tagged(h, ModEntities.WOOLLY_MAMMOTH.get(), new BlockPos(10, 1, 20), true);
        ServerPlayer player = TestPlayers.survival(h, new BlockPos(24, 1, 20));
        h.succeedWhen(() -> h.assertTrue(mammoth.getTarget() == player, "the tagged mammoth didn't go for the player"));
    }

    /** The implant drowns out the Oliphant, the terror bird's fear of mammoths and the glyptodon's urge to hunker. */
    static void implantOverridesInstinct(GameTestHelper h) {
        ServerPlayer player = TestPlayers.survival(h, new BlockPos(20, 1, 20));
        WoollyMammoth mammoth = tagged(h, ModEntities.WOOLLY_MAMMOTH.get(), new BlockPos(8, 1, 20), false);
        TerrorBird bird = tagged(h, ModEntities.TERROR_BIRD.get(), new BlockPos(30, 1, 20), false);
        mammoth.setTarget(player);
        bird.setTarget(player);
        h.assertTrue(OliphantItem.blow(h.getLevel(), player) == 0, "the Oliphant reached a tagged beast");
        h.assertTrue(mammoth.getTarget() == player && !bird.isScared(), "a tagged beast heeded the Oliphant");
        bird.scare(player.position(), 100);
        h.assertFalse(bird.isScared(), "a tagged bird shouldn't scare");
        Glyptodon glyptodon = tagged(h, ModEntities.GLYPTODON.get(), new BlockPos(20, 1, 32), false);
        glyptodon.hurtServer(h.getLevel(), h.getLevel().damageSources().magic(), 2.0F);
        h.assertFalse(glyptodon.isHunkered(), "a tagged glyptodon shouldn't hunker");
        h.succeed();
    }

    /** A tagged terror bird keeps no distance from a mammoth. */
    static void taggedBirdFearsNoMammoth(GameTestHelper h) {
        WoollyMammoth mammoth = h.spawnWithNoFreeWill(ModEntities.WOOLLY_MAMMOTH.get(), new BlockPos(20, 1, 20));
        TerrorBird bird = tagged(h, ModEntities.TERROR_BIRD.get(), new BlockPos(24, 1, 20), true);
        h.runAfterDelay(120, () -> {
            h.assertTrue(bird.distanceTo(mammoth) < TerrorBird.MAMMOTH_BERTH + 4, "the tagged bird fled the mammoth: " + bird.distanceTo(mammoth));
            h.succeed();
        });
    }

    /** Killed by a player, a tagged beast gives up Gray tech fragments. */
    static void taggedBeastsDropFragments(GameTestHelper h) {
        ServerPlayer player = TestPlayers.survival(h, new BlockPos(20, 1, 30));
        Glyptodon glyptodon = tagged(h, ModEntities.GLYPTODON.get(), new BlockPos(20, 1, 20), false);
        AABB around = glyptodon.getBoundingBox().inflate(4);
        glyptodon.hurtServer(h.getLevel(), h.getLevel().damageSources().playerAttack(player), 10_000.0F);
        h.succeedWhen(() -> h.assertTrue(!h.getLevel().getEntitiesOfClass(ItemEntity.class, around,
                item -> item.getItem().is(ModItems.GRAY_TECH_FRAGMENT.get())).isEmpty(), "no fragments dropped"));
    }

    /** About 3 in 100 wild megafauna turn out to have escaped a station; a spawn egg's never do; a station's are tethered. */
    static void escapeesAreRare(GameTestHelper h) {
        Glyptodon glyptodon = h.spawnWithNoFreeWill(ModEntities.GLYPTODON.get(), new BlockPos(20, 1, 20));
        RandomSource random = RandomSource.create(1952);
        int escaped = 0;
        for (int i = 0; i < 2000; i++) {
            glyptodon.setTagged(false);
            Specimens.onSpawn(glyptodon, EntitySpawnReason.NATURAL, random);
            escaped += glyptodon.isTagged() ? 1 : 0;
        }
        h.assertTrue(escaped > 25 && escaped < 110, "escapees in 2000 wild spawns: " + escaped);
        for (int i = 0; i < 200; i++) {
            glyptodon.setTagged(false);
            Specimens.onSpawn(glyptodon, EntitySpawnReason.SPAWN_ITEM_USE, random);
            h.assertFalse(glyptodon.isTagged(), "a spawn egg made an escapee");
        }
        glyptodon.setTagged(true);
        Specimens.onSpawn(glyptodon, EntitySpawnReason.STRUCTURE, random);
        h.assertTrue(glyptodon.hasHome() && glyptodon.getHomeRadius() == Specimens.TETHER, "a station's specimen should be tethered");
        h.succeed();
    }

    /** A sentinel that sees a player charges, then stuns them and sets the tagged beasts round it on them; hit, it holds. */
    static void sentinelStunsAndAlarms(GameTestHelper h) {
        GraySentinel sentinel = h.spawn(ModEntities.GRAY_SENTINEL.get(), new BlockPos(20, 5, 20));
        WoollyMammoth mammoth = tagged(h, ModEntities.WOOLLY_MAMMOTH.get(), new BlockPos(34, 1, 34), false);
        ServerPlayer player = TestPlayers.survival(h, new BlockPos(20, 1, 30));
        sentinel.hurtServer(h.getLevel(), h.getLevel().damageSources().magic(), 1.0F);
        h.assertFalse(sentinel.isFleeing(), "a sentinel shouldn't flee");
        h.succeedWhen(() -> h.assertTrue(player.hasEffect(MobEffects.SLOWNESS) && mammoth.getTarget() == player,
                "no stun or no alarm (slowed " + player.hasEffect(MobEffects.SLOWNESS) + ", mammoth after " + mammoth.getTarget() + ", charge " + sentinel.charge() + ")"));
    }

    /** No station in reach (there are none in the test world), the locator hears only static. */
    static void signalLocatorListens(GameTestHelper h) {
        h.assertTrue(SignalLocatorItem.bearing(h.getLevel(), h.absolutePos(BlockPos.ZERO)).isEmpty(), "a station in the test world?");
        var structures = h.getLevel().registryAccess().lookupOrThrow(Registries.STRUCTURE);
        h.assertTrue(structures.get(ModStructures.GRAY_STATION).isPresent(), "the station structure isn't registered");
        ResourceKey<Recipe<?>> recipe = ResourceKey.create(Registries.RECIPE, HollowFrontier.id("signal_locator"));
        h.assertTrue(h.getLevel().recipeAccess().byKey(recipe).isPresent(), "no Signal Locator recipe");
        var loot = h.getLevel().getServer().reloadableRegistries();
        h.assertTrue(loot.getLootTable(ModChestLoot.GRAY_STATION) != LootTable.EMPTY, "no station loot");
        h.assertTrue(loot.getLootTable(ModEntities.GRAY_SENTINEL.get().getDefaultLootTable().orElseThrow()) != LootTable.EMPTY, "no sentinel loot");
        h.succeed();
    }

    private static int count(StructureTemplate template, Block block) {
        return template.filterBlocks(BlockPos.ZERO, new StructurePlaceSettings(), block).size();
    }

    /**
     * The station built: three sentinels, six tagged specimens (four penned, two loose), the strapped hare, glow squid in
     * the tanks, three chests of station loot, the phlogiston reactor. (Everything it spawned is cleared away straight after.)
     */
    static void grayStationPlaces(GameTestHelper h) {
        StructureTemplate station = h.getLevel().getStructureTemplateManager().get(HollowFrontier.id(HollowBlueprints.GRAY_STATION)).orElseThrow();
        BlockPos origin = h.absolutePos(BlockPos.ZERO);
        station.placeInWorld(h.getLevel(), origin, origin, new StructurePlaceSettings(), h.getLevel().getRandom(), Block.UPDATE_CLIENTS);
        AABB area = new AABB(origin).expandTowards(46, 26, 46);
        List<Entity> placed = h.getLevel().getEntities((Entity) null, area, e -> !(e instanceof ServerPlayer));
        try {
            h.assertValueEqual(placed.stream().filter(e -> e instanceof GraySentinel).count(), 3L, "sentinels");
            long specimens = placed.stream().filter(e -> e instanceof Mob mob && Specimens.isTagged(mob)).count();
            h.assertValueEqual(specimens, 6L, "tagged specimens");
            h.assertTrue(placed.stream().anyMatch(e -> e instanceof GlassbackHare hare && hare.hasEarTag() && hare.isNoAi()), "the hare on the table");
            h.assertValueEqual(placed.stream().filter(e -> e.getType() == EntityTypes.GLOW_SQUID).count(), 3L, "glow squid in the tanks");
            h.assertValueEqual(LoreSiteTests.chestsRolling(station, ModChestLoot.GRAY_STATION), 3L, "chests of station loot");
            h.assertValueEqual(LoreSiteTests.chestsRolling(station, ModChestLoot.GRAY_STATION_STORE), 1L, "the feed store's chest");
            h.assertTrue(placed.stream().anyMatch(e -> e instanceof com.hollowfrontier.content.hollow.creature.GrayProbe), "the probe parked over its pad");
            h.assertTrue(count(station, ModBlocks.PHLOGISTON_LAMP.get()) >= 9, "the reactor's phlogiston lamps");
        } finally {
            placed.forEach(Entity::discard);
        }
        h.succeed();
    }
}
