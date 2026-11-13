package com.hollowfrontier.registry;

import com.hollowfrontier.HollowFrontier;
import com.hollowfrontier.content.frontier.bounty.BountyBoardBlock;
import com.hollowfrontier.content.frontier.furniture.BarStoolBlock;
import com.hollowfrontier.content.frontier.furniture.ChairBlock;
import com.hollowfrontier.content.frontier.furniture.TableBlock;
import com.hollowfrontier.content.frontier.mystery.FlattenedWheatBlock;
import com.hollowfrontier.content.frontier.music.SaloonPianoBlock;
import com.hollowfrontier.content.frontier.saloon.CardTableBlock;
import com.hollowfrontier.content.hollow.AshVentBlock;
import com.hollowfrontier.content.hollow.CrustRockBlock;
import com.hollowfrontier.content.hollow.gateway.GatewayFrameBlock;
import com.hollowfrontier.content.hollow.gateway.PolarPortalBlock;
import com.hollowfrontier.content.hollow.gray.ImplantRigBlock;
import com.hollowfrontier.content.steam.BoilerBlock;
import com.hollowfrontier.content.steam.CopperPipeBlock;
import com.hollowfrontier.content.steam.CrusherBlock;
import com.hollowfrontier.content.steam.HandcarEngineBlock;
import com.hollowfrontier.content.steam.PressureGaugeBlock;
import com.hollowfrontier.content.steam.SteamTankBlock;
import com.hollowfrontier.content.concavia.WaystoneBlock;
import com.hollowfrontier.content.ufo.GrayCacheBlock;
import com.hollowfrontier.content.ufo.PhlogistonBeaconBlock;

import net.minecraft.util.ColorRGBA;
import net.minecraft.util.valueproviders.ConstantInt;
import net.minecraft.util.valueproviders.UniformInt;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.ColoredFallingBlock;
import net.minecraft.world.level.block.LeavesBlock;
import net.minecraft.world.level.block.sounds.AmbientLeavesBlockSoundPlayer;
import net.minecraft.world.level.block.DoorBlock;
import net.minecraft.world.level.block.DropExperienceBlock;
import net.minecraft.world.level.block.SlabBlock;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.StairBlock;
import net.minecraft.world.level.block.TransparentBlock;
import net.minecraft.world.level.block.WallBlock;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.properties.BlockSetType;
import net.minecraft.world.level.material.MapColor;
import net.minecraft.world.level.material.PushReaction;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class ModBlocks {
    private ModBlocks() {}

    public static final DeferredRegister.Blocks BLOCKS = DeferredRegister.createBlocks(HollowFrontier.MODID);

    // Brass: alloyed from vanilla copper and a little gold
    public static final DeferredBlock<Block> BRASS_BLOCK = BLOCKS.registerSimpleBlock("brass_block",
            () -> BlockBehaviour.Properties.ofLegacyCopy(Blocks.IRON_BLOCK).mapColor(MapColor.GOLD));

    // Frontier
    public static final DeferredBlock<BountyBoardBlock> BOUNTY_BOARD = BLOCKS.registerBlock("bounty_board",
            BountyBoardBlock::new, () -> BlockBehaviour.Properties.ofLegacyCopy(Blocks.OAK_PLANKS));
    public static final DeferredBlock<Block> BAR_COUNTER = BLOCKS.registerSimpleBlock("bar_counter",
            () -> BlockBehaviour.Properties.ofLegacyCopy(Blocks.DARK_OAK_PLANKS));
    public static final DeferredBlock<BarStoolBlock> BAR_STOOL = BLOCKS.registerBlock("bar_stool",
            BarStoolBlock::new, () -> BlockBehaviour.Properties.ofLegacyCopy(Blocks.DARK_OAK_PLANKS).noOcclusion());
    public static final DeferredBlock<SaloonPianoBlock> SALOON_PIANO = BLOCKS.registerBlock("saloon_piano",
            SaloonPianoBlock::new, () -> BlockBehaviour.Properties.ofLegacyCopy(Blocks.DARK_OAK_PLANKS).noOcclusion());
    public static final DeferredBlock<CardTableBlock> CARD_TABLE = BLOCKS.registerBlock("card_table",
            CardTableBlock::new, () -> BlockBehaviour.Properties.ofLegacyCopy(Blocks.DARK_OAK_PLANKS).noOcclusion());
    public static final DeferredBlock<DoorBlock> SALOON_DOORS = BLOCKS.registerBlock("saloon_doors",
            p -> new DoorBlock(BlockSetType.OAK, p), () -> BlockBehaviour.Properties.ofLegacyCopy(Blocks.OAK_DOOR).noOcclusion());
    // 24c: the towns' chairs and tables, in three woods
    public static final DeferredBlock<ChairBlock> OAK_CHAIR = chair("oak_chair", Blocks.OAK_PLANKS);
    public static final DeferredBlock<ChairBlock> SPRUCE_CHAIR = chair("spruce_chair", Blocks.SPRUCE_PLANKS);
    public static final DeferredBlock<ChairBlock> DARK_OAK_CHAIR = chair("dark_oak_chair", Blocks.DARK_OAK_PLANKS);
    public static final DeferredBlock<TableBlock> OAK_TABLE = table("oak_table", Blocks.OAK_PLANKS);
    public static final DeferredBlock<TableBlock> SPRUCE_TABLE = table("spruce_table", Blocks.SPRUCE_PLANKS);
    public static final DeferredBlock<TableBlock> DARK_OAK_TABLE = table("dark_oak_table", Blocks.DARK_OAK_PLANKS);

    /** A lantern that lights at dusk and goes out at dawn (24c). */
    public static final DeferredBlock<com.hollowfrontier.content.frontier.furniture.TownLampBlock> TOWN_LAMP = BLOCKS.registerBlock("town_lamp",
            com.hollowfrontier.content.frontier.furniture.TownLampBlock::new,
            () -> BlockBehaviour.Properties.ofLegacyCopy(Blocks.LANTERN).lightLevel(com.hollowfrontier.content.frontier.furniture.TownLampBlock::light));

    // 47b: the stake by an Old Place's gate (no item: it comes with the place) and the mailbox
    public static final DeferredBlock<com.hollowfrontier.content.frontier.stake.ClaimStakeBlock> CLAIM_STAKE = BLOCKS.registerBlock("claim_stake",
            com.hollowfrontier.content.frontier.stake.ClaimStakeBlock::new, () -> BlockBehaviour.Properties.ofLegacyCopy(Blocks.SPRUCE_FENCE).strength(2.0F, 1200.0F).noLootTable()
                    .pushReaction(PushReaction.IMMOVEABLE));
    public static final DeferredBlock<com.hollowfrontier.content.frontier.stake.MailboxBlock> MAILBOX = BLOCKS.registerBlock("mailbox",
            com.hollowfrontier.content.frontier.stake.MailboxBlock::new, () -> BlockBehaviour.Properties.ofLegacyCopy(Blocks.IRON_BARS).strength(2.0F).noOcclusion());

    private static DeferredBlock<ChairBlock> chair(String name, Block wood) {
        return BLOCKS.registerBlock(name, ChairBlock::new, () -> BlockBehaviour.Properties.ofLegacyCopy(wood).noOcclusion());
    }

    private static DeferredBlock<TableBlock> table(String name, Block wood) {
        return BLOCKS.registerBlock(name, TableBlock::new, () -> BlockBehaviour.Properties.ofLegacyCopy(wood).noOcclusion());
    }

    // Steam
    public static final DeferredBlock<BoilerBlock> BOILER = BLOCKS.registerBlock("boiler",
            BoilerBlock::new, () -> BlockBehaviour.Properties.ofLegacyCopy(Blocks.COPPER_BLOCK.weathering().unaffected())
                    .lightLevel(state -> state.getValue(BoilerBlock.LIT) ? 12 : 0));
    public static final DeferredBlock<CopperPipeBlock> COPPER_PIPE = BLOCKS.registerBlock("copper_pipe",
            CopperPipeBlock::new, () -> BlockBehaviour.Properties.ofLegacyCopy(Blocks.COPPER_BLOCK.weathering().unaffected()).strength(1.5F).noOcclusion());
    public static final DeferredBlock<SteamTankBlock> STEAM_TANK = BLOCKS.registerBlock("steam_tank",
            SteamTankBlock::new, () -> BlockBehaviour.Properties.ofLegacyCopy(Blocks.COPPER_BLOCK.weathering().unaffected()));
    public static final DeferredBlock<PressureGaugeBlock> PRESSURE_GAUGE = BLOCKS.registerBlock("pressure_gauge",
            PressureGaugeBlock::new, () -> BlockBehaviour.Properties.ofLegacyCopy(Blocks.COPPER_BLOCK.weathering().unaffected()).strength(1.5F));
    public static final DeferredBlock<CrusherBlock> CRUSHER = BLOCKS.registerBlock("crusher",
            CrusherBlock::new, () -> BlockBehaviour.Properties.ofLegacyCopy(Blocks.IRON_BLOCK));
    // Display only: the engine a steam handcar carries (no item; never placed).
    public static final DeferredBlock<HandcarEngineBlock> HANDCAR_ENGINE = BLOCKS.registerBlock("handcar_engine",
            HandcarEngineBlock::new, () -> BlockBehaviour.Properties.ofLegacyCopy(Blocks.IRON_BLOCK).noOcclusion().noLootTable());

    // Polar Gateways (the way into the Hollow Earth). Both unbreakable, like the end portal and its frame.
    public static final DeferredBlock<GatewayFrameBlock> GATEWAY_FRAME = BLOCKS.registerBlock("gateway_frame",
            GatewayFrameBlock::new, () -> BlockBehaviour.Properties.ofLegacyCopy(Blocks.END_PORTAL_FRAME).mapColor(MapColor.ICE)
                    .lightLevel(state -> state.getValue(GatewayFrameBlock.ACTIVE) ? 10 : 3));
    public static final DeferredBlock<PolarPortalBlock> POLAR_PORTAL = BLOCKS.registerBlock("polar_portal",
            PolarPortalBlock::new, () -> BlockBehaviour.Properties.of().mapColor(MapColor.ICE).noCollision().strength(-1.0F, 3_600_000.0F)
                    .lightLevel(state -> 12).noLootTable().noOcclusion().pushReaction(PushReaction.IMMOVEABLE));

    // The Hollow Earth's crust: stone that lets sky light through, so the land under the ceiling is in daylight.
    public static final DeferredBlock<CrustRockBlock> CRUST_ROCK = BLOCKS.registerBlock("crust_rock",
            CrustRockBlock::new, () -> BlockBehaviour.Properties.ofLegacyCopy(Blocks.STONE).mapColor(MapColor.TERRACOTTA_YELLOW));
    // Its cap, the top five layers of the world: unbreakable like bedrock (no item; never dropped).
    public static final DeferredBlock<CrustRockBlock> SHELL_BEDROCK = BLOCKS.registerBlock("shell_bedrock",
            CrustRockBlock::new, () -> BlockBehaviour.Properties.ofLegacyCopy(Blocks.BEDROCK).mapColor(MapColor.TERRACOTTA_BROWN).noLootTable());

    // Hollow Earth flora and ground (9b1). Fern fronds crown the jungle's tree ferns and cycads (placed persistent: no decay).
    public static final DeferredBlock<LeavesBlock> FERN_FROND = BLOCKS.registerBlock("fern_frond",
            p -> new LeavesBlock(AmbientLeavesBlockSoundPlayer.noAmbientSound(), p),
            () -> BlockBehaviour.Properties.ofLegacyCopy(Blocks.JUNGLE_LEAVES).mapColor(MapColor.PLANT));
    // Ash: the Ember Badlands' grey drifts round the vents. Falls like sand.
    public static final DeferredBlock<ColoredFallingBlock> ASH = BLOCKS.registerBlock("ash",
            p -> new ColoredFallingBlock(new ColorRGBA(0xFF6E6A66), p),
            () -> BlockBehaviour.Properties.ofLegacyCopy(Blocks.SAND).mapColor(MapColor.COLOR_GRAY).strength(0.4F));
    public static final DeferredBlock<AshVentBlock> ASH_VENT = BLOCKS.registerBlock("ash_vent",
            AshVentBlock::new, () -> BlockBehaviour.Properties.ofLegacyCopy(Blocks.MAGMA_BLOCK).mapColor(MapColor.COLOR_BLACK)
                    .lightLevel(state -> 6));
    // 43b: Lamp Gel set in glass. As bright as a sea lantern, and it doesn't care whether it's under water.
    public static final DeferredBlock<Block> JELLY_LAMP = BLOCKS.registerSimpleBlock("jelly_lamp",
            () -> BlockBehaviour.Properties.ofLegacyCopy(Blocks.SEA_LANTERN).mapColor(MapColor.COLOR_LIGHT_GREEN).lightLevel(state -> 15));
    // 9c: the Hollow Earth's ores. Starsteel is diamond-tier (an iron pickaxe or better); phlogiston glows in the rock.
    public static final DeferredBlock<DropExperienceBlock> STARSTEEL_ORE = BLOCKS.registerBlock("starsteel_ore",
            p -> new DropExperienceBlock(UniformInt.of(2, 5), p),
            () -> BlockBehaviour.Properties.ofLegacyCopy(Blocks.DIAMOND_ORE).mapColor(MapColor.TERRACOTTA_ORANGE));
    public static final DeferredBlock<DropExperienceBlock> DEEPSLATE_STARSTEEL_ORE = BLOCKS.registerBlock("deepslate_starsteel_ore",
            p -> new DropExperienceBlock(UniformInt.of(2, 5), p),
            () -> BlockBehaviour.Properties.ofLegacyCopy(Blocks.DEEPSLATE_DIAMOND_ORE).mapColor(MapColor.TERRACOTTA_ORANGE));
    public static final DeferredBlock<Block> RAW_STARSTEEL_BLOCK = BLOCKS.registerSimpleBlock("raw_starsteel_block",
            () -> BlockBehaviour.Properties.ofLegacyCopy(Blocks.RAW_GOLD_BLOCK).mapColor(MapColor.TERRACOTTA_ORANGE));
    public static final DeferredBlock<Block> STARSTEEL_BLOCK = BLOCKS.registerSimpleBlock("starsteel_block",
            () -> BlockBehaviour.Properties.ofLegacyCopy(Blocks.GOLD_BLOCK).mapColor(MapColor.COLOR_ORANGE).strength(5.0F, 6.0F));
    public static final DeferredBlock<DropExperienceBlock> PHLOGISTON_ORE = BLOCKS.registerBlock("phlogiston_ore",
            p -> new DropExperienceBlock(UniformInt.of(3, 7), p),
            () -> BlockBehaviour.Properties.ofLegacyCopy(Blocks.DIAMOND_ORE).lightLevel(state -> 7));
    public static final DeferredBlock<DropExperienceBlock> DEEPSLATE_PHLOGISTON_ORE = BLOCKS.registerBlock("deepslate_phlogiston_ore",
            p -> new DropExperienceBlock(UniformInt.of(3, 7), p),
            () -> BlockBehaviour.Properties.ofLegacyCopy(Blocks.DEEPSLATE_DIAMOND_ORE).lightLevel(state -> 7));
    // A phlogiston shard in a brass-capped glass case: the brightest light, and it never needs fuel.
    public static final DeferredBlock<Block> PHLOGISTON_LAMP = BLOCKS.registerSimpleBlock("phlogiston_lamp",
            () -> BlockBehaviour.Properties.ofLegacyCopy(Blocks.SEA_LANTERN).mapColor(MapColor.COLOR_LIGHT_BLUE).lightLevel(state -> 15));
    // 11a: wheat laid flat in a crop circle
    public static final DeferredBlock<FlattenedWheatBlock> FLATTENED_WHEAT = BLOCKS.registerBlock("flattened_wheat", FlattenedWheatBlock::new,
            () -> BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_YELLOW).noCollision().instabreak().sound(SoundType.CROP).forceSolidOff()
                    .pushReaction(PushReaction.POPPED));
    // 12a: the Concavians' building stone. The crystal is see-through and glows like their phlogiston; the bricks are cut from it
    // and frosted, and the chiseled ones have a phlogiston seam that glows a little.
    public static final DeferredBlock<TransparentBlock> CONCAVIAN_CRYSTAL = BLOCKS.registerBlock("concavian_crystal", TransparentBlock::new,
            () -> BlockBehaviour.Properties.ofLegacyCopy(Blocks.GLASS).mapColor(MapColor.COLOR_LIGHT_BLUE).strength(1.5F, 6.0F)
                    .sound(SoundType.AMETHYST).lightLevel(state -> 7));
    public static final DeferredBlock<Block> CRYSTAL_BRICKS = BLOCKS.registerSimpleBlock("crystal_bricks",
            () -> BlockBehaviour.Properties.ofLegacyCopy(Blocks.QUARTZ_BRICKS).mapColor(MapColor.ICE).strength(1.5F, 6.0F).sound(SoundType.AMETHYST));
    public static final DeferredBlock<StairBlock> CRYSTAL_BRICK_STAIRS = BLOCKS.registerBlock("crystal_brick_stairs",
            p -> new StairBlock(CRYSTAL_BRICKS.get().defaultBlockState(), p), () -> BlockBehaviour.Properties.ofLegacyCopy(CRYSTAL_BRICKS.get()));
    public static final DeferredBlock<SlabBlock> CRYSTAL_BRICK_SLAB = BLOCKS.registerBlock("crystal_brick_slab",
            SlabBlock::new, () -> BlockBehaviour.Properties.ofLegacyCopy(CRYSTAL_BRICKS.get()));
    public static final DeferredBlock<WallBlock> CRYSTAL_BRICK_WALL = BLOCKS.registerBlock("crystal_brick_wall",
            WallBlock::new, () -> BlockBehaviour.Properties.ofLegacyCopy(CRYSTAL_BRICKS.get()).forceSolidOn());
    public static final DeferredBlock<Block> CHISELED_CRYSTAL_BRICKS = BLOCKS.registerSimpleBlock("chiseled_crystal_bricks",
            () -> BlockBehaviour.Properties.ofLegacyCopy(CRYSTAL_BRICKS.get()).lightLevel(state -> 5));
    // 12b: the Sun Temple's pylons, which shield the Archon while they stand; broken, each gives up a sunstone shard
    public static final DeferredBlock<Block> SUN_PYLON = BLOCKS.registerSimpleBlock("sun_pylon",
            () -> BlockBehaviour.Properties.ofLegacyCopy(Blocks.GLOWSTONE).mapColor(MapColor.GOLD).strength(2.0F, 6.0F)
                    .sound(SoundType.AMETHYST).lightLevel(state -> 15));
    // 13a: a crashed saucer's hull (a building block in its own right), and the wreckage salvage is pried from
    public static final DeferredBlock<Block> SAUCER_HULL = BLOCKS.registerSimpleBlock("saucer_hull",
            () -> BlockBehaviour.Properties.ofLegacyCopy(Blocks.IRON_BLOCK).mapColor(MapColor.COLOR_LIGHT_GRAY).strength(5.0F, 9.0F));
    // 13d: the Phlogiston Beacon, which calls the Mothership down
    public static final DeferredBlock<PhlogistonBeaconBlock> PHLOGISTON_BEACON = BLOCKS.registerBlock("phlogiston_beacon", PhlogistonBeaconBlock::new,
            () -> BlockBehaviour.Properties.ofLegacyCopy(Blocks.BEACON).mapColor(MapColor.COLOR_PURPLE).strength(5.0F, 1200.0F)
                    .lightLevel(state -> state.getValue(PhlogistonBeaconBlock.LIT) ? 15 : 7));
    public static final DeferredBlock<Block> SAUCER_WRECKAGE = BLOCKS.registerSimpleBlock("saucer_wreckage",
            () -> BlockBehaviour.Properties.ofLegacyCopy(Blocks.IRON_BLOCK).mapColor(MapColor.COLOR_GRAY).strength(4.0F, 9.0F).lightLevel(state -> 4));
    // 15a: a Gray Landing's salvage cache (unbreakable; it opens, or leaves with the saucer)
    public static final DeferredBlock<GrayCacheBlock> GRAY_CACHE = BLOCKS.registerBlock("gray_cache", GrayCacheBlock::new,
            () -> BlockBehaviour.Properties.ofLegacyCopy(Blocks.IRON_BLOCK).mapColor(MapColor.COLOR_LIGHT_GRAY).strength(-1.0F, 3600000.0F)
                    .noLootTable().pushReaction(PushReaction.IMMOVEABLE).lightLevel(state -> 7));
    // 32c: a crash site's cargo pod (sealed until used, then open, empty scrap)
    public static final DeferredBlock<com.hollowfrontier.content.ufo.CargoPodBlock> GRAY_CARGO_POD = BLOCKS.registerBlock("gray_cargo_pod",
            com.hollowfrontier.content.ufo.CargoPodBlock::new, () -> BlockBehaviour.Properties.ofLegacyCopy(Blocks.IRON_BLOCK).mapColor(MapColor.COLOR_LIGHT_GRAY)
                    .strength(5.0F, 3600000.0F).noLootTable().pushReaction(PushReaction.IMMOVEABLE)
                    .lightLevel(state -> state.getValue(com.hollowfrontier.content.ufo.CargoPodBlock.OPEN) ? 0 : 5));
    // 15c: a Concavian waystone (unbreakable, like the gateways' stones; the item is creative-only)
    public static final DeferredBlock<WaystoneBlock> WAYSTONE = BLOCKS.registerBlock("waystone", WaystoneBlock::new,
            () -> BlockBehaviour.Properties.ofLegacyCopy(Blocks.END_PORTAL_FRAME).mapColor(MapColor.QUARTZ).lightLevel(state -> 10)
                    .noLootTable().pushReaction(PushReaction.IMMOVEABLE));
    // 20: the implant rig over a Gray pen's operating table (breaking it stops the procedure)
    public static final DeferredBlock<ImplantRigBlock> IMPLANT_RIG = BLOCKS.registerBlock("implant_rig", ImplantRigBlock::new,
            () -> BlockBehaviour.Properties.ofLegacyCopy(Blocks.IRON_BLOCK).mapColor(MapColor.COLOR_LIGHT_GRAY).strength(4.0F, 6.0F)
                    .requiresCorrectToolForDrops().lightLevel(state -> 9));
    // 50: aboard the Grays' ship. The field can't be broken (the ship lets it go); the console and the core's sockets are
    // worked by hand. All three have items for builders in creative only.
    public static final DeferredBlock<com.hollowfrontier.content.ufo.taken.GrayFieldBlock> GRAY_FIELD = BLOCKS.registerBlock("gray_field",
            com.hollowfrontier.content.ufo.taken.GrayFieldBlock::new, () -> BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_CYAN)
                    .strength(-1.0F, 3600000.0F).noLootTable().noOcclusion().sound(SoundType.GLASS).lightLevel(state -> 9)
                    .isValidSpawn(Blocks::never).isRedstoneConductor((s, l, p) -> false).isSuffocating((s, l, p) -> false)
                    .isViewBlocking((s, l, p, box) -> false).pushReaction(PushReaction.IMMOVEABLE));
    public static final DeferredBlock<com.hollowfrontier.content.ufo.taken.ShipConsoleBlock> SHIP_CONSOLE = BLOCKS.registerBlock("ship_console",
            com.hollowfrontier.content.ufo.taken.ShipConsoleBlock::new, () -> BlockBehaviour.Properties.ofLegacyCopy(Blocks.IRON_BLOCK)
                    .mapColor(MapColor.COLOR_LIGHT_GRAY).strength(-1.0F, 3600000.0F).noLootTable().pushReaction(PushReaction.IMMOVEABLE)
                    .lightLevel(state -> state.getValue(com.hollowfrontier.content.ufo.taken.ShipConsoleBlock.SPENT) ? 0 : 8));
    public static final DeferredBlock<com.hollowfrontier.content.ufo.taken.CoreSocketBlock> CORE_SOCKET = BLOCKS.registerBlock("core_socket",
            com.hollowfrontier.content.ufo.taken.CoreSocketBlock::new, () -> BlockBehaviour.Properties.ofLegacyCopy(Blocks.IRON_BLOCK)
                    .mapColor(MapColor.COLOR_LIGHT_GRAY).strength(-1.0F, 3600000.0F).noLootTable().pushReaction(PushReaction.IMMOVEABLE)
                    .lightLevel(state -> state.getValue(com.hollowfrontier.content.ufo.taken.CoreSocketBlock.CELL) ? 12 : 0));
    // 26: the Kindled vault's seal (opens when its sockets are right) and the sockets, marked or not
    public static final DeferredBlock<Block> KINDLED_SEAL = BLOCKS.registerSimpleBlock("kindled_seal",
            () -> BlockBehaviour.Properties.ofLegacyCopy(Blocks.REINFORCED_DEEPSLATE).mapColor(MapColor.COLOR_CYAN).strength(-1.0F, 3600000.0F)
                    .noLootTable().pushReaction(PushReaction.IMMOVEABLE).lightLevel(state -> 4));
    public static final DeferredBlock<com.hollowfrontier.content.concavia.SealSocketBlock> SEAL_SOCKET = BLOCKS.registerBlock("seal_socket",
            com.hollowfrontier.content.concavia.SealSocketBlock::new, () -> BlockBehaviour.Properties.ofLegacyCopy(Blocks.REINFORCED_DEEPSLATE)
                    .mapColor(MapColor.COLOR_GRAY).strength(-1.0F, 3600000.0F).noLootTable().pushReaction(PushReaction.IMMOVEABLE));
}
