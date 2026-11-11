package com.hollowfrontier.datagen;

import java.util.stream.Stream;

import com.hollowfrontier.registry.ModEntities;
import com.hollowfrontier.registry.ModItems;

import net.minecraft.data.loot.EntityLootSubProvider;
import net.minecraft.data.loot.LootTableSubProvider;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.flag.FeatureFlags;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.ItemLike;
import net.minecraft.world.level.storage.loot.LootPool;
import net.minecraft.world.level.storage.loot.LootTable;
import net.minecraft.world.level.storage.loot.entries.LootItem;
import net.minecraft.world.level.storage.loot.functions.EnchantedCountIncreaseFunction;
import net.minecraft.world.level.storage.loot.functions.SetItemCountFunction;
import net.minecraft.world.level.storage.loot.functions.SmeltItemFunction;
import net.minecraft.world.level.storage.loot.predicates.LootItemKilledByPlayerCondition;
import net.minecraft.world.level.storage.loot.predicates.LootItemRandomChanceCondition;
import net.minecraft.world.level.storage.loot.providers.number.floats.ContextFloatProviders;
import net.minecraft.world.level.storage.loot.providers.number.ints.ContextIntProviders;

// Outlaw Boss badges are added in code (they carry the bounty); everything else is here.
public class ModEntityLootProvider extends EntityLootSubProvider {
    public ModEntityLootProvider(LootTableSubProvider.Context context) {
        super(FeatureFlags.REGISTRY.allFlags(), context);
    }

    @Override
    public void generate() {
        add(ModEntities.OUTLAW.get(), LootTable.lootTable()
                .withPool(drop(ModItems.REVOLVER_CARTRIDGE.get(), 0, 3))
                .withPool(drop(ModItems.PERCUSSION_CAP.get(), 0, 3))
                .withPool(drop(ModItems.COPPER_COIN.get(), 0, 2).when(LootItemKilledByPlayerCondition.killedByPlayer()))
                .withPool(rare(ModItems.REVOLVER.get(), 0.03F)));
        add(ModEntities.OUTLAW_GUNSLINGER.get(), LootTable.lootTable()
                .withPool(drop(ModItems.RIFLE_CARTRIDGE.get(), 0, 3))
                .withPool(drop(ModItems.PERCUSSION_CAP.get(), 0, 3))
                .withPool(drop(ModItems.COPPER_COIN.get(), 1, 3).when(LootItemKilledByPlayerCondition.killedByPlayer()))
                .withPool(rare(ModItems.PLAINS_RIFLE.get(), 0.03F)));
        add(ModEntities.OUTLAW_BOSS.get(), LootTable.lootTable()
                .withPool(drop(ModItems.BUCKSHOT_CARTRIDGE.get(), 2, 6))
                .withPool(drop(ModItems.PERCUSSION_CAP.get(), 2, 6))
                .withPool(drop(ModItems.COPPER_COIN.get(), 4, 8))
                .withPool(drop(ModItems.SHOTGUN.get(), 1, 1)));
        add(ModEntities.TOWNSFOLK.get(), LootTable.lootTable());
        add(ModEntities.BARMAID.get(), LootTable.lootTable());
        add(ModEntities.LAWMAN.get(), LootTable.lootTable());
        add(ModEntities.RATTLESNAKE.get(), LootTable.lootTable()
                .withPool(drop(ModItems.SNAKESKIN.get(), 0, 1)));
        // The Hollow Earth's creatures (9b2): vanilla drops only until 9c and later give them uses of their own
        add(ModEntities.FERN_SKITTER.get(), LootTable.lootTable()
                .withPool(drop(Items.FEATHER, 0, 1)));
        add(ModEntities.GLASSBACK_HARE.get(), LootTable.lootTable()
                .withPool(drop(Items.RABBIT_HIDE, 0, 1))
                .withPool(rare(Items.AMETHYST_SHARD, 0.35F)));
        add(ModEntities.ASH_SALAMANDER.get(), LootTable.lootTable()
                .withPool(rare(Items.MAGMA_CREAM, 0.4F)));
        add(ModEntities.PALE_RAVEN.get(), LootTable.lootTable()
                .withPool(drop(Items.FEATHER, 0, 2)));
        add(ModEntities.GRAY_PROBE.get(), LootTable.lootTable()); // nothing to salvage yet (10b's Gray tech fragments)
        // The wild megafauna (10a): each one's own drop (a player's kill for the tusk and talon), and meat or leather
        add(ModEntities.WOOLLY_MAMMOTH.get(), LootTable.lootTable()
                .withPool(drop(ModItems.MAMMOTH_TUSK.get(), 1, 2).when(LootItemKilledByPlayerCondition.killedByPlayer()))
                .withPool(meat(ModItems.RAW_MAMMOTH.get(), 3, 5))
                .withPool(drop(Items.LEATHER, 1, 3)));
        add(ModEntities.TERROR_BIRD.get(), LootTable.lootTable()
                .withPool(drop(ModItems.TERROR_BIRD_TALON.get(), 1, 1).when(LootItemKilledByPlayerCondition.killedByPlayer()))
                .withPool(drop(Items.FEATHER, 1, 3))
                .withPool(meat(Items.CHICKEN, 1, 2)));
        add(ModEntities.MEGATHERIUM.get(), LootTable.lootTable()
                .withPool(drop(ModItems.THICK_HIDE.get(), 1, 3))
                .withPool(drop(Items.LEATHER, 0, 2)));
        add(ModEntities.GLYPTODON.get(), LootTable.lootTable()
                .withPool(drop(ModItems.GLYPTODON_SCUTE.get(), 2, 4)));
        // The Still Sea (43b): blubber from the sea cow and the whale, the whale's teeth to a hunter, bones and fish from the
        // plesiosaur (what it's for is the diving), gel from the jelly
        add(ModEntities.GREAT_SEA_COW.get(), LootTable.lootTable()
                .withPool(drop(ModItems.BLUBBER.get(), 2, 3))
                .withPool(drop(Items.LEATHER, 0, 2)));
        add(ModEntities.BASILOSAURUS.get(), LootTable.lootTable()
                .withPool(drop(ModItems.BLUBBER.get(), 3, 5))
                .withPool(drop(ModItems.BASILOSAUR_TOOTH.get(), 1, 2).when(LootItemKilledByPlayerCondition.killedByPlayer())));
        add(ModEntities.PLESIOSAUR.get(), LootTable.lootTable()
                .withPool(drop(Items.BONE, 1, 3))
                .withPool(meat(Items.COD, 0, 2)));
        add(ModEntities.LAMPJELLY.get(), LootTable.lootTable()
                .withPool(drop(ModItems.LAMP_GEL.get(), 0, 2)));
        // The frontier's mysteries (11a): a sighting drops nothing; a carcass breaks up in code (CattleCarcass)
        add(ModEntities.AIRSHIP_SIGHTING.get(), LootTable.lootTable());
        add(ModEntities.CATTLE_CARCASS.get(), LootTable.lootTable());
        // A man in black (11b): a little of the Grays' money, now and then a piece of their tech
        add(ModEntities.MAN_IN_BLACK.get(), LootTable.lootTable()
                .withPool(drop(ModItems.GOLD_COIN.get(), 0, 2).when(LootItemKilledByPlayerCondition.killedByPlayer()))
                .withPool(rare(ModItems.GRAY_TECH_FRAGMENT.get(), 0.3F)));
        // Concavia (12a): a Kindled leaves nothing worth the taking; a guardian falls to crystal and the phlogiston that ran it
        add(ModEntities.KINDLED.get(), LootTable.lootTable());
        add(ModEntities.CRYSTAL_GUARDIAN.get(), LootTable.lootTable()
                .withPool(drop(ModItems.PHLOGISTON_SHARD.get(), 1, 3))
                .withPool(drop(ModItems.CONCAVIAN_CRYSTAL.get(), 2, 4)));
        // The Sun Archon (12b): its heart and its crown (one each, whatever the looting), and the sunstone it burned with
        add(ModEntities.SUN_ARCHON.get(), LootTable.lootTable()
                .withPool(LootPool.lootPool().setRolls(ContextIntProviders.exactly(1)).add(LootItem.lootTableItem(ModItems.PHLOGISTON_CORE.get())))
                .withPool(LootPool.lootPool().setRolls(ContextIntProviders.exactly(1)).add(LootItem.lootTableItem(ModItems.STARSTEEL_CROWN.get())))
                .withPool(drop(ModItems.SUNSTONE_SHARD.get(), 2, 3)));
        // 13a: a Warden gives up Gray tech, and now and then a strip of its craft's plating; the saucer itself leaves nothing
        add(ModEntities.GRAY_WARDEN.get(), LootTable.lootTable()
                .withPool(drop(ModItems.GRAY_TECH_FRAGMENT.get(), 0, 2).when(LootItemKilledByPlayerCondition.killedByPlayer()))
                .withPool(rare(ModItems.ALIEN_ALLOY_PLATING.get(), 0.15F)));
        add(ModEntities.GRAY_SAUCER.get(), LootTable.lootTable());
        add(ModEntities.DISGUISED_GRAY.get(), LootTable.lootTable()); // 21c: it gives up its fragment when found out, not when killed
        // 13d: the Mothership's wreck gives up salvage enough for a saucer of one's own, and its phlogiston; a drone, a fragment now and then
        add(ModEntities.MOTHERSHIP.get(), LootTable.lootTable()
                .withPool(drop(ModItems.ALIEN_ALLOY_PLATING.get(), 8, 12))
                .withPool(LootPool.lootPool().setRolls(ContextIntProviders.exactly(1)).add(LootItem.lootTableItem(ModItems.GRAVITY_DRIVE.get())))
                .withPool(drop(ModItems.PHLOGISTON_SHARD.get(), 6, 12))
                .withPool(drop(ModItems.GRAY_TECH_FRAGMENT.get(), 4, 8)));
        add(ModEntities.GRAY_DRONE.get(), LootTable.lootTable()
                .withPool(rare(ModItems.GRAY_TECH_FRAGMENT.get(), 0.35F)));
        add(ModEntities.PHLOGISTON_BEAM.get(), LootTable.lootTable());
        add(ModEntities.PROTOTYPE_SAUCER.get(), LootTable.lootTable());
        // 29: a dust devil drops its own table when it dies down (DustDevils.LOOT), not as a kill
        add(ModEntities.DUST_DEVIL.get(), LootTable.lootTable());
        add(ModEntities.ASH_DEVIL.get(), LootTable.lootTable());
        // The station's sentinel (10b); the tagged beasts' fragments are added in code (Specimens)
        add(ModEntities.GRAY_SENTINEL.get(), LootTable.lootTable()
                .withPool(drop(ModItems.GRAY_TECH_FRAGMENT.get(), 1, 2).when(LootItemKilledByPlayerCondition.killedByPlayer())));
    }

    @Override
    protected Stream<EntityType<?>> getKnownEntityTypes() {
        return ModEntities.ENTITIES.getEntries().stream().map(holder -> holder.get());
    }

    private LootPool.Builder drop(ItemLike item, int min, int max) {
        return LootPool.lootPool()
                .setRolls(ContextIntProviders.exactly(1))
                .add(LootItem.lootTableItem(item)
                        .apply(SetItemCountFunction.setCount(ContextIntProviders.between(min, max)))
                        .apply(EnchantedCountIncreaseFunction.lootingMultiplier(this.enchantments, ContextFloatProviders.between(0.0F, 1.0F))));
    }

    // Raw meat, cooked (by its smelting recipe) if the beast died burning; looting adds one
    private LootPool.Builder meat(ItemLike raw, int min, int max) {
        return LootPool.lootPool()
                .setRolls(ContextIntProviders.exactly(1))
                .add(LootItem.lootTableItem(raw)
                        .apply(SetItemCountFunction.setCount(ContextIntProviders.between(min, max)))
                        .apply(SmeltItemFunction.smelted().when(shouldSmeltLoot()))
                        .apply(EnchantedCountIncreaseFunction.lootingMultiplier(this.enchantments, ContextFloatProviders.between(0.0F, 1.0F))));
    }

    private static LootPool.Builder rare(ItemLike item, float chance) {
        return LootPool.lootPool()
                .setRolls(ContextIntProviders.exactly(1))
                .add(LootItem.lootTableItem(item))
                .when(LootItemKilledByPlayerCondition.killedByPlayer())
                .when(LootItemRandomChanceCondition.randomChance(chance));
    }
}
