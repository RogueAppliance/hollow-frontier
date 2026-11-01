package com.hollowfrontier.registry;

import java.util.function.UnaryOperator;

import com.hollowfrontier.HollowFrontier;
import com.hollowfrontier.content.frontier.bounty.BountyBoardItem;
import com.hollowfrontier.content.frontier.bounty.BountyItem;
import com.hollowfrontier.content.frontier.cat.CatAccessoryItem;
import com.hollowfrontier.content.frontier.gazette.GazetteItem;
import com.hollowfrontier.content.frontier.gear.DynamiteItem;
import com.hollowfrontier.content.frontier.gear.LassoItem;
import com.hollowfrontier.content.frontier.gear.ModArmorMaterials;
import com.hollowfrontier.content.frontier.gear.SnakeskinBootsItem;
import com.hollowfrontier.content.frontier.gun.GunItem;
import com.hollowfrontier.content.frontier.gun.GunTypes;
import com.hollowfrontier.content.frontier.mystery.StrangeSkullItem;
import com.hollowfrontier.content.frontier.horse.HorseshoeItem;
import com.hollowfrontier.content.frontier.saloon.Drink;
import com.hollowfrontier.content.frontier.saloon.DrinkItem;
import com.hollowfrontier.content.hollow.gear.StarsteelGear;
import com.hollowfrontier.content.hollow.gear.PaxelItem;
import com.hollowfrontier.content.hollow.gateway.PolarCompassItem;
import com.hollowfrontier.content.hollow.gateway.PolarResonatorItem;
import com.hollowfrontier.content.hollow.gray.SignalLocatorItem;
import com.hollowfrontier.content.hollow.megafauna.MegafaunaGear;
import com.hollowfrontier.content.hollow.megafauna.OliphantItem;
import com.hollowfrontier.content.concavia.Artifacts;
import com.hollowfrontier.content.concavia.ConcavianCodexItem;
import com.hollowfrontier.content.concavia.KindledSigilItem;
import com.hollowfrontier.content.concavia.SunstoneLensItem;
import com.hollowfrontier.content.concavia.PhlogistonCoreItem;
import com.hollowfrontier.content.steam.SteamCanisterItem;
import com.hollowfrontier.content.ufo.PrototypeSaucerItem;
import com.hollowfrontier.content.ufo.SalvageItem;
import com.hollowfrontier.content.ufo.PhlogistonCellItem;
import com.hollowfrontier.content.ufo.PhlogistonGunItem;
import com.hollowfrontier.content.steam.airship.AirshipItem;
import com.hollowfrontier.content.steam.SteamDrillItem;

import net.minecraft.core.component.DataComponents;
import net.minecraft.world.food.FoodProperties;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.DoubleHighBlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.MinecartItem;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.item.SpawnEggItem;
import net.minecraft.world.item.component.BlockTransformers;
import net.minecraft.world.item.component.Consumables;
import net.minecraft.world.level.storage.loot.providers.number.ints.ContextIntProviders;
import net.minecraft.world.item.equipment.ArmorType;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;

// Registration order is creative tab order.
public final class ModItems {
    private ModItems() {}

    public static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(HollowFrontier.MODID);

    // Materials
    public static final DeferredItem<BlockItem> BRASS_BLOCK = ITEMS.registerSimpleBlockItem(ModBlocks.BRASS_BLOCK);
    public static final DeferredItem<Item> BRASS_INGOT = ITEMS.registerSimpleItem("brass_ingot");
    public static final DeferredItem<Item> BRASS_NUGGET = ITEMS.registerSimpleItem("brass_nugget");
    public static final DeferredItem<Item> BRASS_PLATE = ITEMS.registerSimpleItem("brass_plate");
    public static final DeferredItem<Item> BRASS_GEAR = ITEMS.registerSimpleItem("brass_gear");
    public static final DeferredItem<Item> PRESSURE_VALVE = ITEMS.registerSimpleItem("pressure_valve");

    // Frontier black powder guns & ammo. Every load also uses one percussion cap.
    public static final DeferredItem<Item> PERCUSSION_CAP = ITEMS.registerSimpleItem("percussion_cap");
    public static final DeferredItem<Item> REVOLVER_CARTRIDGE = ITEMS.registerSimpleItem("revolver_cartridge");
    public static final DeferredItem<Item> RIFLE_CARTRIDGE = ITEMS.registerSimpleItem("rifle_cartridge");
    public static final DeferredItem<Item> BUCKSHOT_CARTRIDGE = ITEMS.registerSimpleItem("buckshot_cartridge");
    // How well the guns take enchantments at the table (22): brass between a bow (1) and iron tools (14); phlogiston as gold tools do
    private static final int GUN_ENCHANTABILITY = 10;
    private static final int PHLOGISTON_GUN_ENCHANTABILITY = 22;
    public static final DeferredItem<GunItem> REVOLVER = ITEMS.registerItem("revolver",
            p -> new GunItem(GunTypes.REVOLVER, p), p -> p.durability(600).repairable(ModTags.Items.INGOTS_BRASS).enchantable(GUN_ENCHANTABILITY));
    public static final DeferredItem<GunItem> PLAINS_RIFLE = ITEMS.registerItem("plains_rifle",
            p -> new GunItem(GunTypes.RIFLE, p), p -> p.durability(450).repairable(ModTags.Items.INGOTS_BRASS).enchantable(GUN_ENCHANTABILITY));
    public static final DeferredItem<GunItem> SHOTGUN = ITEMS.registerItem("shotgun",
            p -> new GunItem(GunTypes.SHOTGUN, p), p -> p.durability(400).repairable(ModTags.Items.INGOTS_BRASS).enchantable(GUN_ENCHANTABILITY));

    // Cowboy gear
    public static final DeferredItem<Item> COWBOY_HAT = ITEMS.registerItem("cowboy_hat",
            Item::new, p -> p.humanoidArmor(ModArmorMaterials.COWBOY, ArmorType.HELMET));
    public static final DeferredItem<Item> DUSTER = ITEMS.registerItem("duster",
            Item::new, p -> p.humanoidArmor(ModArmorMaterials.COWBOY, ArmorType.CHESTPLATE));
    public static final DeferredItem<Item> CHAPS = ITEMS.registerItem("chaps",
            Item::new, p -> p.humanoidArmor(ModArmorMaterials.COWBOY, ArmorType.LEGGINGS));
    public static final DeferredItem<Item> COWBOY_BOOTS = ITEMS.registerItem("cowboy_boots",
            Item::new, p -> p.humanoidArmor(ModArmorMaterials.COWBOY, ArmorType.BOOTS));
    public static final DeferredItem<SnakeskinBootsItem> SNAKESKIN_BOOTS = ITEMS.registerItem("snakeskin_boots",
            SnakeskinBootsItem::new, p -> p.humanoidArmor(ModArmorMaterials.SNAKESKIN, ArmorType.BOOTS));
    public static final DeferredItem<LassoItem> LASSO = ITEMS.registerItem("lasso", LassoItem::new, p -> p.stacksTo(16));
    public static final DeferredItem<DynamiteItem> DYNAMITE = ITEMS.registerItem("dynamite", DynamiteItem::new, p -> p.stacksTo(16));

    // Horse gear (starsteel horseshoes, tier 3, are with the Hollow Earth's gear below)
    public static final DeferredItem<HorseshoeItem> IRON_HORSESHOES = ITEMS.registerItem("iron_horseshoes",
            p -> new HorseshoeItem(1, 0.10, 0.05, p), p -> p.stacksTo(16));
    public static final DeferredItem<HorseshoeItem> BRASS_HORSESHOES = ITEMS.registerItem("brass_horseshoes",
            p -> new HorseshoeItem(2, 0.20, 0.10, p), p -> p.stacksTo(16));
    public static final DeferredItem<Item> SADDLEBAGS = ITEMS.registerSimpleItem("saddlebags", p -> p.stacksTo(1));

    // Outlaws & bounties
    public static final DeferredItem<BlockItem> BOUNTY_BOARD = ITEMS.registerSimpleBlockItem(ModBlocks.BOUNTY_BOARD);
    public static final DeferredItem<BountyBoardItem> HANGING_BOUNTY_BOARD = ITEMS.registerItem("hanging_bounty_board", BountyBoardItem::new);
    public static final DeferredItem<BlockItem> BAR_COUNTER = ITEMS.registerSimpleBlockItem(ModBlocks.BAR_COUNTER);
    public static final DeferredItem<BlockItem> BAR_STOOL = ITEMS.registerSimpleBlockItem(ModBlocks.BAR_STOOL);
    public static final DeferredItem<BlockItem> SALOON_PIANO = ITEMS.registerSimpleBlockItem(ModBlocks.SALOON_PIANO);
    public static final DeferredItem<BlockItem> CARD_TABLE = ITEMS.registerSimpleBlockItem(ModBlocks.CARD_TABLE);
    // 24c
    public static final DeferredItem<BlockItem> OAK_CHAIR = ITEMS.registerSimpleBlockItem(ModBlocks.OAK_CHAIR);
    public static final DeferredItem<BlockItem> SPRUCE_CHAIR = ITEMS.registerSimpleBlockItem(ModBlocks.SPRUCE_CHAIR);
    public static final DeferredItem<BlockItem> DARK_OAK_CHAIR = ITEMS.registerSimpleBlockItem(ModBlocks.DARK_OAK_CHAIR);
    public static final DeferredItem<BlockItem> OAK_TABLE = ITEMS.registerSimpleBlockItem(ModBlocks.OAK_TABLE);
    public static final DeferredItem<BlockItem> SPRUCE_TABLE = ITEMS.registerSimpleBlockItem(ModBlocks.SPRUCE_TABLE);
    public static final DeferredItem<BlockItem> DARK_OAK_TABLE = ITEMS.registerSimpleBlockItem(ModBlocks.DARK_OAK_TABLE);
    public static final DeferredItem<BlockItem> TOWN_LAMP = ITEMS.registerSimpleBlockItem(ModBlocks.TOWN_LAMP);
    public static final DeferredItem<DoubleHighBlockItem> SALOON_DOORS = ITEMS.registerItem("saloon_doors",
            p -> new DoubleHighBlockItem(ModBlocks.SALOON_DOORS.get(), p));
    public static final DeferredItem<Item> COPPER_COIN = ITEMS.registerSimpleItem("copper_coin");
    public static final DeferredItem<Item> GOLD_COIN = ITEMS.registerSimpleItem("gold_coin");
    public static final DeferredItem<GazetteItem> FRONTIER_GAZETTE = ITEMS.registerItem("frontier_gazette", GazetteItem::new, p -> p.stacksTo(1));
    public static final DeferredItem<Item> SNAKESKIN = ITEMS.registerSimpleItem("snakeskin");
    // Cat accessories (the cowboy hat fits cats too). The star is never crafted: each town's sheriff's cat wears the only one.
    public static final DeferredItem<CatAccessoryItem> BANDANA = ITEMS.registerItem("bandana", p -> new CatAccessoryItem("bandana", p));
    public static final DeferredItem<CatAccessoryItem> BRASS_STAR = ITEMS.registerItem("brass_star",
            p -> new CatAccessoryItem("brass_star", p), p -> p.stacksTo(1).rarity(Rarity.UNCOMMON));
    public static final DeferredItem<BountyItem> WANTED_POSTER = ITEMS.registerItem("wanted_poster",
            p -> new BountyItem(true, p), p -> p.stacksTo(1));
    public static final DeferredItem<BountyItem> OUTLAW_BADGE = ITEMS.registerItem("outlaw_badge",
            p -> new BountyItem(false, p), p -> p.stacksTo(1).rarity(Rarity.UNCOMMON));
    public static final DeferredItem<SpawnEggItem> OUTLAW_SPAWN_EGG = ITEMS.registerItem("outlaw_spawn_egg",
            SpawnEggItem::new, p -> p.spawnEgg(ModEntities.OUTLAW.get()));
    public static final DeferredItem<SpawnEggItem> OUTLAW_GUNSLINGER_SPAWN_EGG = ITEMS.registerItem("outlaw_gunslinger_spawn_egg",
            SpawnEggItem::new, p -> p.spawnEgg(ModEntities.OUTLAW_GUNSLINGER.get()));
    public static final DeferredItem<SpawnEggItem> OUTLAW_BOSS_SPAWN_EGG = ITEMS.registerItem("outlaw_boss_spawn_egg",
            SpawnEggItem::new, p -> p.spawnEgg(ModEntities.OUTLAW_BOSS.get()));
    public static final DeferredItem<SpawnEggItem> TOWNSFOLK_SPAWN_EGG = ITEMS.registerItem("townsfolk_spawn_egg",
            SpawnEggItem::new, p -> p.spawnEgg(ModEntities.TOWNSFOLK.get()));
    public static final DeferredItem<SpawnEggItem> BARMAID_SPAWN_EGG = ITEMS.registerItem("barmaid_spawn_egg",
            SpawnEggItem::new, p -> p.spawnEgg(ModEntities.BARMAID.get()));
    public static final DeferredItem<SpawnEggItem> LAWMAN_SPAWN_EGG = ITEMS.registerItem("lawman_spawn_egg",
            SpawnEggItem::new, p -> p.spawnEgg(ModEntities.LAWMAN.get()));
    public static final DeferredItem<SpawnEggItem> RATTLESNAKE_SPAWN_EGG = ITEMS.registerItem("rattlesnake_spawn_egg",
            SpawnEggItem::new, p -> p.spawnEgg(ModEntities.RATTLESNAKE.get()));
    // The Hollow Earth's creatures (9b2)
    public static final DeferredItem<SpawnEggItem> FERN_SKITTER_SPAWN_EGG = ITEMS.registerItem("fern_skitter_spawn_egg",
            SpawnEggItem::new, p -> p.spawnEgg(ModEntities.FERN_SKITTER.get()));
    public static final DeferredItem<SpawnEggItem> GLASSBACK_HARE_SPAWN_EGG = ITEMS.registerItem("glassback_hare_spawn_egg",
            SpawnEggItem::new, p -> p.spawnEgg(ModEntities.GLASSBACK_HARE.get()));
    public static final DeferredItem<SpawnEggItem> ASH_SALAMANDER_SPAWN_EGG = ITEMS.registerItem("ash_salamander_spawn_egg",
            SpawnEggItem::new, p -> p.spawnEgg(ModEntities.ASH_SALAMANDER.get()));
    public static final DeferredItem<SpawnEggItem> PALE_RAVEN_SPAWN_EGG = ITEMS.registerItem("pale_raven_spawn_egg",
            SpawnEggItem::new, p -> p.spawnEgg(ModEntities.PALE_RAVEN.get()));
    public static final DeferredItem<SpawnEggItem> GRAY_PROBE_SPAWN_EGG = ITEMS.registerItem("gray_probe_spawn_egg",
            SpawnEggItem::new, p -> p.spawnEgg(ModEntities.GRAY_PROBE.get()));
    // The wild megafauna (10a)
    public static final DeferredItem<SpawnEggItem> WOOLLY_MAMMOTH_SPAWN_EGG = ITEMS.registerItem("woolly_mammoth_spawn_egg",
            SpawnEggItem::new, p -> p.spawnEgg(ModEntities.WOOLLY_MAMMOTH.get()));
    public static final DeferredItem<SpawnEggItem> TERROR_BIRD_SPAWN_EGG = ITEMS.registerItem("terror_bird_spawn_egg",
            SpawnEggItem::new, p -> p.spawnEgg(ModEntities.TERROR_BIRD.get()));
    public static final DeferredItem<SpawnEggItem> MEGATHERIUM_SPAWN_EGG = ITEMS.registerItem("megatherium_spawn_egg",
            SpawnEggItem::new, p -> p.spawnEgg(ModEntities.MEGATHERIUM.get()));
    public static final DeferredItem<SpawnEggItem> GLYPTODON_SPAWN_EGG = ITEMS.registerItem("glyptodon_spawn_egg",
            SpawnEggItem::new, p -> p.spawnEgg(ModEntities.GLYPTODON.get()));
    // The Gray experiments (10b)
    public static final DeferredItem<SpawnEggItem> GRAY_SENTINEL_SPAWN_EGG = ITEMS.registerItem("gray_sentinel_spawn_egg",
            SpawnEggItem::new, p -> p.spawnEgg(ModEntities.GRAY_SENTINEL.get()));

    // Saloon drinks (sold by the barmaid). Liquor stacks Tipsy; coffee and sarsaparilla sober you up.
    // Beer is the one drink that also feeds you.
    private static final FoodProperties BEER = new FoodProperties.Builder().nutrition(4).saturationModifier(0.3F).alwaysEdible().build();
    public static final DeferredItem<DrinkItem> FRONTIER_BEER = drink("frontier_beer", Drink.FRONTIER_BEER, p -> p.food(BEER, Consumables.DEFAULT_DRINK));
    public static final DeferredItem<DrinkItem> SARSAPARILLA = drink("sarsaparilla", Drink.SARSAPARILLA, p -> p);
    public static final DeferredItem<DrinkItem> COWBOY_COFFEE = drink("cowboy_coffee", Drink.COWBOY_COFFEE, p -> p);
    public static final DeferredItem<DrinkItem> RYE_WHISKEY = drink("rye_whiskey", Drink.RYE_WHISKEY, p -> p);
    public static final DeferredItem<DrinkItem> MEZCAL = drink("mezcal", Drink.MEZCAL, p -> p);
    public static final DeferredItem<DrinkItem> MOONSHINE = drink("moonshine", Drink.MOONSHINE, p -> p.rarity(Rarity.UNCOMMON));
    public static final DeferredItem<DrinkItem> SNAKE_OIL = drink("snake_oil", Drink.SNAKE_OIL, p -> p.rarity(Rarity.UNCOMMON));

    // Steam
    public static final DeferredItem<BlockItem> BOILER = ITEMS.registerSimpleBlockItem(ModBlocks.BOILER);
    public static final DeferredItem<BlockItem> COPPER_PIPE = ITEMS.registerSimpleBlockItem(ModBlocks.COPPER_PIPE);
    public static final DeferredItem<BlockItem> STEAM_TANK = ITEMS.registerSimpleBlockItem(ModBlocks.STEAM_TANK);
    public static final DeferredItem<BlockItem> PRESSURE_GAUGE = ITEMS.registerSimpleBlockItem(ModBlocks.PRESSURE_GAUGE);
    public static final DeferredItem<BlockItem> CRUSHER = ITEMS.registerSimpleBlockItem(ModBlocks.CRUSHER);
    public static final DeferredItem<SteamCanisterItem> STEAM_CANISTER = ITEMS.registerItem("steam_canister",
            SteamCanisterItem::new, p -> p.stacksTo(1));
    public static final DeferredItem<SteamDrillItem> STEAM_DRILL = ITEMS.registerItem("steam_drill",
            SteamDrillItem::new, SteamDrillItem::drillProperties);
    public static final DeferredItem<MinecartItem> STEAM_HANDCAR = ITEMS.registerItem("steam_handcar",
            p -> new MinecartItem(ModEntities.STEAM_HANDCAR.get(), p), p -> p.stacksTo(1));
    // Airship and the polar expedition (phase 8). The gateway frame is creative-only: gateways are found, not built.
    public static final DeferredItem<Item> AIRSHIP_ENVELOPE = ITEMS.registerSimpleItem("airship_envelope");
    public static final DeferredItem<Item> AIRSHIP_ENGINE = ITEMS.registerSimpleItem("airship_engine");
    public static final DeferredItem<AirshipItem> AIRSHIP = ITEMS.registerItem("airship", AirshipItem::new, p -> p.stacksTo(1).rarity(Rarity.UNCOMMON));
    public static final DeferredItem<PolarCompassItem> POLAR_COMPASS = ITEMS.registerItem("polar_compass", PolarCompassItem::new, p -> p.stacksTo(1));
    public static final DeferredItem<PolarResonatorItem> POLAR_RESONATOR = ITEMS.registerItem("polar_resonator",
            PolarResonatorItem::new, p -> p.stacksTo(1).rarity(Rarity.UNCOMMON));
    public static final DeferredItem<BlockItem> GATEWAY_FRAME = ITEMS.registerSimpleBlockItem(ModBlocks.GATEWAY_FRAME, p -> p.rarity(Rarity.EPIC));
    public static final DeferredItem<Item> IRON_DUST = ITEMS.registerSimpleItem("iron_dust");
    public static final DeferredItem<Item> GOLD_DUST = ITEMS.registerSimpleItem("gold_dust");
    public static final DeferredItem<Item> COPPER_DUST = ITEMS.registerSimpleItem("copper_dust");

    // The Hollow Earth (phase 9)
    public static final DeferredItem<BlockItem> CRUST_ROCK = ITEMS.registerSimpleBlockItem(ModBlocks.CRUST_ROCK);
    public static final DeferredItem<BlockItem> FERN_FROND = ITEMS.registerSimpleBlockItem(ModBlocks.FERN_FROND);
    public static final DeferredItem<BlockItem> ASH = ITEMS.registerSimpleBlockItem(ModBlocks.ASH);
    public static final DeferredItem<BlockItem> ASH_VENT = ITEMS.registerSimpleBlockItem(ModBlocks.ASH_VENT);
    // 9c: starsteel and phlogiston
    public static final DeferredItem<BlockItem> STARSTEEL_ORE = ITEMS.registerSimpleBlockItem(ModBlocks.STARSTEEL_ORE);
    public static final DeferredItem<BlockItem> DEEPSLATE_STARSTEEL_ORE = ITEMS.registerSimpleBlockItem(ModBlocks.DEEPSLATE_STARSTEEL_ORE);
    public static final DeferredItem<BlockItem> RAW_STARSTEEL_BLOCK = ITEMS.registerSimpleBlockItem(ModBlocks.RAW_STARSTEEL_BLOCK);
    public static final DeferredItem<BlockItem> STARSTEEL_BLOCK = ITEMS.registerSimpleBlockItem(ModBlocks.STARSTEEL_BLOCK);
    public static final DeferredItem<BlockItem> PHLOGISTON_ORE = ITEMS.registerSimpleBlockItem(ModBlocks.PHLOGISTON_ORE);
    public static final DeferredItem<BlockItem> DEEPSLATE_PHLOGISTON_ORE = ITEMS.registerSimpleBlockItem(ModBlocks.DEEPSLATE_PHLOGISTON_ORE);
    public static final DeferredItem<BlockItem> PHLOGISTON_LAMP = ITEMS.registerSimpleBlockItem(ModBlocks.PHLOGISTON_LAMP);
    public static final DeferredItem<Item> RAW_STARSTEEL = ITEMS.registerSimpleItem("raw_starsteel");
    public static final DeferredItem<Item> STARSTEEL_INGOT = ITEMS.registerSimpleItem("starsteel_ingot", p -> p.rarity(Rarity.UNCOMMON));
    public static final DeferredItem<Item> STARSTEEL_NUGGET = ITEMS.registerSimpleItem("starsteel_nugget");
    public static final DeferredItem<Item> STARSTEEL_DUST = ITEMS.registerSimpleItem("starsteel_dust");
    // Burns as long as a block of coal, in a furnace or a boiler
    public static final DeferredItem<Item> PHLOGISTON_SHARD = ITEMS.registerSimpleItem("phlogiston_shard",
            p -> p.rarity(Rarity.UNCOMMON).cookingFuel(ContextIntProviders.COOKING_TIME_COAL_BLOCK));
    public static final DeferredItem<Item> STARSTEEL_SWORD = ITEMS.registerItem("starsteel_sword", Item::new, p -> p.sword(StarsteelGear.TOOLS, 3.0F, -2.4F));
    // The paxel: pickaxe, axe and shovel in one (attacks like a light axe, and disables shields as one does)
    public static final DeferredItem<PaxelItem> STARSTEEL_PAXEL = ITEMS.registerItem("starsteel_paxel", PaxelItem::new,
            p -> p.tool(StarsteelGear.TOOLS, ModTags.Blocks.MINEABLE_WITH_PAXEL, 4.0F, -2.9F, 5.0F)
                    .delayedComponent(DataComponents.BLOCK_TRANSFORMER, context -> context.getOrThrow(BlockTransformers.AXE)));
    public static final DeferredItem<Item> STARSTEEL_HELMET = ITEMS.registerItem("starsteel_helmet", Item::new, p -> p.humanoidArmor(StarsteelGear.ARMOR, ArmorType.HELMET));
    public static final DeferredItem<Item> STARSTEEL_CHESTPLATE = ITEMS.registerItem("starsteel_chestplate", Item::new, p -> p.humanoidArmor(StarsteelGear.ARMOR, ArmorType.CHESTPLATE));
    public static final DeferredItem<Item> STARSTEEL_LEGGINGS = ITEMS.registerItem("starsteel_leggings", Item::new, p -> p.humanoidArmor(StarsteelGear.ARMOR, ArmorType.LEGGINGS));
    public static final DeferredItem<Item> STARSTEEL_BOOTS = ITEMS.registerItem("starsteel_boots", Item::new, p -> p.humanoidArmor(StarsteelGear.ARMOR, ArmorType.BOOTS));
    public static final DeferredItem<HorseshoeItem> STARSTEEL_HORSESHOES = ITEMS.registerItem("starsteel_horseshoes",
            p -> new HorseshoeItem(3, 0.30, 0.15, p), p -> p.stacksTo(16));

    // The megafauna's drops and what they make (10a; MegafaunaGear)
    public static final DeferredItem<Item> MAMMOTH_TUSK = ITEMS.registerSimpleItem("mammoth_tusk", p -> p.stacksTo(16));
    public static final DeferredItem<Item> RAW_MAMMOTH = ITEMS.registerSimpleItem("raw_mammoth",
            p -> p.food(new FoodProperties.Builder().nutrition(4).saturationModifier(0.3F).build()));
    public static final DeferredItem<Item> MAMMOTH_STEAK = ITEMS.registerSimpleItem("mammoth_steak",
            p -> p.food(new FoodProperties.Builder().nutrition(10).saturationModifier(0.9F).build()));
    public static final DeferredItem<OliphantItem> OLIPHANT = ITEMS.registerItem("oliphant", OliphantItem::new, p -> p.stacksTo(1).rarity(Rarity.UNCOMMON));
    public static final DeferredItem<Item> TERROR_BIRD_TALON = ITEMS.registerSimpleItem("terror_bird_talon");
    // A quick, light blade: 4.5 damage at 2.2 attacks a second (an iron sword is 6 at 1.6), and it skins
    public static final DeferredItem<Item> TALON_KNIFE = ITEMS.registerItem("talon_knife", Item::new, p -> p.sword(MegafaunaGear.TALON, 2.5F, -1.8F));
    public static final DeferredItem<Item> THICK_HIDE = ITEMS.registerSimpleItem("thick_hide");
    public static final DeferredItem<Item> HIDE_BARDING = ITEMS.registerItem("hide_barding", Item::new, p -> p.horseArmor(MegafaunaGear.BARDING));
    public static final DeferredItem<Item> GLYPTODON_SCUTE = ITEMS.registerSimpleItem("glyptodon_scute");
    public static final DeferredItem<Item> GLYPTODON_HELMET = ITEMS.registerItem("glyptodon_helmet", Item::new, p -> p.humanoidArmor(MegafaunaGear.SHELL, ArmorType.HELMET));
    public static final DeferredItem<Item> GLYPTODON_CHESTPLATE = ITEMS.registerItem("glyptodon_chestplate", Item::new, p -> p.humanoidArmor(MegafaunaGear.SHELL, ArmorType.CHESTPLATE));
    public static final DeferredItem<Item> GLYPTODON_LEGGINGS = ITEMS.registerItem("glyptodon_leggings", Item::new, p -> p.humanoidArmor(MegafaunaGear.SHELL, ArmorType.LEGGINGS));
    public static final DeferredItem<Item> GLYPTODON_BOOTS = ITEMS.registerItem("glyptodon_boots", Item::new, p -> p.humanoidArmor(MegafaunaGear.SHELL, ArmorType.BOOTS));

    // The Gray experiments (10b): what the tagged beasts and the stations give up, and the locator it makes
    public static final DeferredItem<Item> GRAY_TECH_FRAGMENT = ITEMS.registerSimpleItem("gray_tech_fragment", p -> p.rarity(Rarity.UNCOMMON));
    public static final DeferredItem<SignalLocatorItem> SIGNAL_LOCATOR = ITEMS.registerItem("signal_locator", SignalLocatorItem::new,
            p -> p.stacksTo(1).rarity(Rarity.UNCOMMON));

    // The frontier's mysteries (11a): a circle's flattened wheat, and the Boot Hill pilot's skull
    public static final DeferredItem<BlockItem> FLATTENED_WHEAT = ITEMS.registerSimpleBlockItem(ModBlocks.FLATTENED_WHEAT);
    public static final DeferredItem<StrangeSkullItem> STRANGE_SKULL = ITEMS.registerItem("strange_skull", StrangeSkullItem::new,
            p -> p.stacksTo(1).rarity(Rarity.RARE));
    public static final DeferredItem<SpawnEggItem> MAN_IN_BLACK_SPAWN_EGG = ITEMS.registerItem("man_in_black_spawn_egg",
            SpawnEggItem::new, p -> p.spawnEgg(ModEntities.MAN_IN_BLACK.get()));

    // Concavia (12a): the crystal set, the sigil the Kindled give an ally, and the Kindled and their guardian
    public static final DeferredItem<BlockItem> CONCAVIAN_CRYSTAL = ITEMS.registerSimpleBlockItem(ModBlocks.CONCAVIAN_CRYSTAL);
    public static final DeferredItem<BlockItem> CRYSTAL_BRICKS = ITEMS.registerSimpleBlockItem(ModBlocks.CRYSTAL_BRICKS);
    public static final DeferredItem<BlockItem> CRYSTAL_BRICK_STAIRS = ITEMS.registerSimpleBlockItem(ModBlocks.CRYSTAL_BRICK_STAIRS);
    public static final DeferredItem<BlockItem> CRYSTAL_BRICK_SLAB = ITEMS.registerSimpleBlockItem(ModBlocks.CRYSTAL_BRICK_SLAB);
    public static final DeferredItem<BlockItem> CRYSTAL_BRICK_WALL = ITEMS.registerSimpleBlockItem(ModBlocks.CRYSTAL_BRICK_WALL);
    public static final DeferredItem<BlockItem> CHISELED_CRYSTAL_BRICKS = ITEMS.registerSimpleBlockItem(ModBlocks.CHISELED_CRYSTAL_BRICKS);
    public static final DeferredItem<KindledSigilItem> KINDLED_SIGIL = ITEMS.registerItem("kindled_sigil", KindledSigilItem::new,
            p -> p.stacksTo(1).rarity(Rarity.RARE).component(DataComponents.ENCHANTMENT_GLINT_OVERRIDE, true));
    public static final DeferredItem<SpawnEggItem> KINDLED_SPAWN_EGG = ITEMS.registerItem("kindled_spawn_egg",
            SpawnEggItem::new, p -> p.spawnEgg(ModEntities.KINDLED.get()));
    public static final DeferredItem<SpawnEggItem> CRYSTAL_GUARDIAN_SPAWN_EGG = ITEMS.registerItem("crystal_guardian_spawn_egg",
            SpawnEggItem::new, p -> p.spawnEgg(ModEntities.CRYSTAL_GUARDIAN.get()));

    // The Sun Archon (12b): the temple's pylons, what the Archon leaves, and what the Kindled's learning makes of it
    public static final DeferredItem<BlockItem> SUN_PYLON = ITEMS.registerSimpleBlockItem(ModBlocks.SUN_PYLON);
    public static final DeferredItem<Item> SUNSTONE_SHARD = ITEMS.registerSimpleItem("sunstone_shard", p -> p.rarity(Rarity.UNCOMMON).fireResistant());
    public static final DeferredItem<Item> PHLOGISTON_CORE = ITEMS.registerItem("phlogiston_core", PhlogistonCoreItem::new,
            p -> p.stacksTo(1).rarity(Rarity.EPIC).fireResistant().component(DataComponents.ENCHANTMENT_GLINT_OVERRIDE, true));
    public static final DeferredItem<Item> STARSTEEL_CROWN = ITEMS.registerItem("starsteel_crown", Item::new,
            p -> p.humanoidArmor(Artifacts.CROWN, ArmorType.HELMET).rarity(Rarity.EPIC).fireResistant());
    public static final DeferredItem<SunstoneLensItem> SUNSTONE_LENS = ITEMS.registerItem("sunstone_lens", SunstoneLensItem::new,
            p -> p.durability(SunstoneLensItem.DURABILITY).rarity(Rarity.EPIC).fireResistant());
    public static final DeferredItem<ConcavianCodexItem> CONCAVIAN_CODEX = ITEMS.registerItem("concavian_codex", ConcavianCodexItem::new,
            p -> p.stacksTo(1).rarity(Rarity.UNCOMMON));
    public static final DeferredItem<SpawnEggItem> SUN_ARCHON_SPAWN_EGG = ITEMS.registerItem("sun_archon_spawn_egg",
            SpawnEggItem::new, p -> p.spawnEgg(ModEntities.SUN_ARCHON.get()));

    // The Grays arrive (13a): a crashed saucer's hull and wreckage, the salvage, and the Warden
    public static final DeferredItem<BlockItem> SAUCER_HULL = ITEMS.registerSimpleBlockItem(ModBlocks.SAUCER_HULL);
    public static final DeferredItem<BlockItem> SAUCER_WRECKAGE = ITEMS.registerSimpleBlockItem(ModBlocks.SAUCER_WRECKAGE);
    public static final DeferredItem<BlockItem> GRAY_CACHE = ITEMS.registerSimpleBlockItem(ModBlocks.GRAY_CACHE); // 15a
    public static final DeferredItem<BlockItem> GRAY_CARGO_POD = ITEMS.registerSimpleBlockItem(ModBlocks.GRAY_CARGO_POD, p -> p.rarity(Rarity.UNCOMMON)); // 32c
    public static final DeferredItem<SalvageItem> ALIEN_ALLOY_PLATING = ITEMS.registerItem("alien_alloy_plating", SalvageItem::new,
            p -> p.rarity(Rarity.UNCOMMON));
    public static final DeferredItem<SalvageItem> DAMAGED_GRAVITY_DRIVE = ITEMS.registerItem("damaged_gravity_drive", SalvageItem::new,
            p -> p.stacksTo(1).rarity(Rarity.RARE));
    public static final DeferredItem<SalvageItem> BEAM_EMITTER = ITEMS.registerItem("beam_emitter", SalvageItem::new,
            p -> p.stacksTo(1).rarity(Rarity.RARE));
    public static final DeferredItem<SalvageItem> LENS_ARRAY = ITEMS.registerItem("lens_array", SalvageItem::new,
            p -> p.stacksTo(1).rarity(Rarity.RARE));
    public static final DeferredItem<SalvageItem> NAVIGATION_CORE = ITEMS.registerItem("navigation_core", SalvageItem::new,
            p -> p.stacksTo(1).rarity(Rarity.RARE));
    public static final DeferredItem<SpawnEggItem> GRAY_WARDEN_SPAWN_EGG = ITEMS.registerItem("gray_warden_spawn_egg",
            SpawnEggItem::new, p -> p.spawnEgg(ModEntities.GRAY_WARDEN.get()));
    public static final DeferredItem<SpawnEggItem> DISGUISED_GRAY_SPAWN_EGG = ITEMS.registerItem("disguised_gray_spawn_egg",
            SpawnEggItem::new, p -> p.spawnEgg(ModEntities.DISGUISED_GRAY.get())); // 21c

    // Phlogiston tech (13b): the cells the phlogiston guns draw on, the guns, and the mended gravity drive
    public static final DeferredItem<Item> EMPTY_PHLOGISTON_CELL = ITEMS.registerSimpleItem("empty_phlogiston_cell");
    public static final DeferredItem<PhlogistonCellItem> PHLOGISTON_CELL = ITEMS.registerItem("phlogiston_cell", PhlogistonCellItem::new,
            p -> p.durability(PhlogistonCellItem.CHARGES).rarity(Rarity.UNCOMMON));
    public static final DeferredItem<PhlogistonGunItem> RAY_PISTOL = ITEMS.registerItem("ray_pistol",
            p -> new PhlogistonGunItem(GunTypes.RAY_PISTOL, 1, 1, 0x70FFA0, new Vec3(1.0, 0.33, 0.12), p), p -> p.durability(500).repairable(ModTags.Items.INGOTS_STARSTEEL).rarity(Rarity.RARE).enchantable(PHLOGISTON_GUN_ENCHANTABILITY));
    public static final DeferredItem<PhlogistonGunItem> PHLOGISTON_RIFLE = ITEMS.registerItem("phlogiston_rifle",
            p -> new PhlogistonGunItem(GunTypes.PHLOGISTON_RIFLE, 3, 3, 0xFF8A2A, new Vec3(1.5, 0.34, 0.3), p), p -> p.durability(500).repairable(ModTags.Items.INGOTS_STARSTEEL).rarity(Rarity.RARE).enchantable(PHLOGISTON_GUN_ENCHANTABILITY));
    public static final DeferredItem<SalvageItem> GRAVITY_DRIVE = ITEMS.registerItem("gravity_drive", SalvageItem::new,
            p -> p.stacksTo(1).rarity(Rarity.EPIC).fireResistant());
    // 13d: the Phlogiston Beacon
    public static final DeferredItem<BlockItem> PHLOGISTON_BEACON = ITEMS.registerSimpleBlockItem(ModBlocks.PHLOGISTON_BEACON, p -> p.rarity(Rarity.EPIC));
    // 13c: the Prototype Saucer, crafted whole from salvage and set down like the airship
    public static final DeferredItem<PrototypeSaucerItem> PROTOTYPE_SAUCER = ITEMS.registerItem("prototype_saucer", PrototypeSaucerItem::new,
            p -> p.stacksTo(1).rarity(Rarity.EPIC).fireResistant());
    // 15c: the waystone (creative-only, like the gateway frame: restored ruins have theirs)
    public static final DeferredItem<BlockItem> WAYSTONE = ITEMS.registerSimpleBlockItem(ModBlocks.WAYSTONE, p -> p.rarity(Rarity.EPIC));
    // 20: the captives: the rig (creative-only, like the waystone) and the letters freed captives hand over
    public static final DeferredItem<BlockItem> IMPLANT_RIG = ITEMS.registerSimpleBlockItem(ModBlocks.IMPLANT_RIG, p -> p.rarity(Rarity.UNCOMMON));
    // 26: the Kindled vault's puzzle
    public static final DeferredItem<BlockItem> KINDLED_SEAL = ITEMS.registerSimpleBlockItem(ModBlocks.KINDLED_SEAL, p -> p.rarity(Rarity.UNCOMMON));
    public static final DeferredItem<BlockItem> SEAL_SOCKET = ITEMS.registerSimpleBlockItem(ModBlocks.SEAL_SOCKET, p -> p.rarity(Rarity.UNCOMMON));
    // 28: the Hollow Earth's relics (a town's prospector buys them, for the gold coins after each name), its sherds and its trim
    public static final DeferredItem<com.hollowfrontier.content.hollow.RelicItem> KINDLED_IDOL = relic("kindled_idol", 5, Rarity.RARE);
    public static final DeferredItem<com.hollowfrontier.content.hollow.RelicItem> CRYSTAL_TABLET = relic("crystal_tablet", 4, Rarity.UNCOMMON);
    public static final DeferredItem<com.hollowfrontier.content.hollow.RelicItem> SUNSTONE_CIRCLET = relic("sunstone_circlet", 8, Rarity.EPIC);
    public static final DeferredItem<com.hollowfrontier.content.hollow.RelicItem> STARSTEEL_MASK = relic("starsteel_mask", 6, Rarity.RARE);
    public static final DeferredItem<com.hollowfrontier.content.hollow.RelicItem> GRAY_SPECIMEN_JAR = relic("gray_specimen_jar", 5, Rarity.RARE);
    public static final DeferredItem<com.hollowfrontier.content.hollow.RelicItem> EXPEDITION_SEXTANT = relic("expedition_sextant", 3, Rarity.UNCOMMON);
    public static final DeferredItem<com.hollowfrontier.content.hollow.RelicItem> IVORY_CARVING = relic("ivory_carving", 3, Rarity.UNCOMMON);
    public static final DeferredItem<com.hollowfrontier.content.hollow.RelicItem> STAR_CHART = relic("star_chart", 4, Rarity.UNCOMMON);
    public static final java.util.List<DeferredItem<com.hollowfrontier.content.hollow.RelicItem>> RELICS = java.util.List.of(KINDLED_IDOL, CRYSTAL_TABLET,
            SUNSTONE_CIRCLET, STARSTEEL_MASK, GRAY_SPECIMEN_JAR, EXPEDITION_SEXTANT, IVORY_CARVING, STAR_CHART);
    // 32c: the Grays' odds and ends from a crash site's cargo pod (the prospector buys them too, for the gold coins after each name)
    public static final DeferredItem<com.hollowfrontier.content.ufo.GrayGadgetItem> HUMMING_DOOHICKEY = gadget("humming_doohickey", 3, com.hollowfrontier.content.ufo.GrayGadgetItem.Kind.DOOHICKEY);
    public static final DeferredItem<com.hollowfrontier.content.ufo.GrayGadgetItem> SELF_WARMING_ROCK = gadget("self_warming_rock", 3, com.hollowfrontier.content.ufo.GrayGadgetItem.Kind.WARMING_ROCK);
    public static final DeferredItem<com.hollowfrontier.content.ufo.GrayGadgetItem> BACKWARD_WATCH = gadget("backward_watch", 2, com.hollowfrontier.content.ufo.GrayGadgetItem.Kind.BACKWARD_WATCH);
    public static final DeferredItem<com.hollowfrontier.content.ufo.GrayGadgetItem> RESTLESS_MARBLE = gadget("restless_marble", 2, com.hollowfrontier.content.ufo.GrayGadgetItem.Kind.RESTLESS_MARBLE);
    public static final DeferredItem<com.hollowfrontier.content.ufo.GrayGadgetItem> UNOPENABLE_BOX = gadget("unopenable_box", 4, com.hollowfrontier.content.ufo.GrayGadgetItem.Kind.UNOPENABLE_BOX);
    public static final java.util.List<DeferredItem<com.hollowfrontier.content.ufo.GrayGadgetItem>> GADGETS = java.util.List.of(HUMMING_DOOHICKEY,
            SELF_WARMING_ROCK, BACKWARD_WATCH, RESTLESS_MARBLE, UNOPENABLE_BOX);
    public static final DeferredItem<Item> SUN_POTTERY_SHERD = ITEMS.registerItem("sun_pottery_sherd", Item::new,
            p -> p.rarity(Rarity.UNCOMMON).potPattern(com.hollowfrontier.content.hollow.HollowPatterns.SUN));
    public static final DeferredItem<Item> CRYSTAL_POTTERY_SHERD = ITEMS.registerItem("crystal_pottery_sherd", Item::new,
            p -> p.rarity(Rarity.UNCOMMON).potPattern(com.hollowfrontier.content.hollow.HollowPatterns.CRYSTAL));
    public static final DeferredItem<Item> WATCHER_POTTERY_SHERD = ITEMS.registerItem("watcher_pottery_sherd", Item::new,
            p -> p.rarity(Rarity.UNCOMMON).potPattern(com.hollowfrontier.content.hollow.HollowPatterns.WATCHER));
    public static final java.util.List<DeferredItem<Item>> SHERDS = java.util.List.of(SUN_POTTERY_SHERD, CRYSTAL_POTTERY_SHERD, WATCHER_POTTERY_SHERD);
    public static final DeferredItem<net.minecraft.world.item.SmithingTemplateItem> CONCAVIAN_ARMOR_TRIM_SMITHING_TEMPLATE = ITEMS.registerItem(
            "concavian_armor_trim_smithing_template", net.minecraft.world.item.SmithingTemplateItem::createArmorTrimTemplate, p -> p.rarity(Rarity.RARE));

    private static DeferredItem<com.hollowfrontier.content.hollow.RelicItem> relic(String name, int price, Rarity rarity) {
        return ITEMS.registerItem(name, p -> new com.hollowfrontier.content.hollow.RelicItem(p, price), p -> p.stacksTo(16).rarity(rarity));
    }

    private static DeferredItem<com.hollowfrontier.content.ufo.GrayGadgetItem> gadget(String name, int price, com.hollowfrontier.content.ufo.GrayGadgetItem.Kind kind) {
        return ITEMS.registerItem(name, p -> new com.hollowfrontier.content.ufo.GrayGadgetItem(p, price, kind), p -> p.stacksTo(16).rarity(Rarity.UNCOMMON));
    }
    public static final DeferredItem<com.hollowfrontier.content.hollow.gray.LetterHomeItem> LETTER_HOME = ITEMS.registerItem("letter_home",
            com.hollowfrontier.content.hollow.gray.LetterHomeItem::new, p -> p.stacksTo(1).rarity(Rarity.UNCOMMON));
    // 15b: the fittings only a Mothership rematch gives up
    public static final DeferredItem<SalvageItem> CLOAKING_DEVICE = ITEMS.registerItem("cloaking_device", SalvageItem::new,
            p -> p.stacksTo(1).rarity(Rarity.EPIC).fireResistant());
    public static final DeferredItem<SalvageItem> CARGO_HOLD = ITEMS.registerItem("cargo_hold", SalvageItem::new,
            p -> p.stacksTo(1).rarity(Rarity.EPIC).fireResistant());

    // 16 to a stack, drunk like a potion, leaves the glass bottle.
    private static DeferredItem<DrinkItem> drink(String name, Drink drink, UnaryOperator<Item.Properties> extra) {
        return ITEMS.registerItem(name, p -> new DrinkItem(drink, p), p -> extra.apply(p.stacksTo(16).usingConvertsTo(Items.GLASS_BOTTLE)
                .component(DataComponents.CONSUMABLE, Consumables.DEFAULT_DRINK)));
    }
}
