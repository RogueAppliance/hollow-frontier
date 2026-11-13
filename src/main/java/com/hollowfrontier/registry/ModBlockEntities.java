package com.hollowfrontier.registry;

import com.hollowfrontier.HollowFrontier;
import com.hollowfrontier.content.frontier.bounty.BountyBoardBlockEntity;
import com.hollowfrontier.content.ufo.PhlogistonBeaconBlockEntity;
import com.hollowfrontier.content.frontier.music.SaloonPianoBlockEntity;
import com.hollowfrontier.content.hollow.gateway.PolarPortalBlockEntity;
import com.hollowfrontier.content.steam.BoilerBlockEntity;
import com.hollowfrontier.content.steam.CrusherBlockEntity;
import com.hollowfrontier.content.steam.PressureGaugeBlockEntity;
import com.hollowfrontier.content.steam.SteamStorageBlockEntity;

import net.minecraft.core.registries.Registries;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.capabilities.RegisterCapabilitiesEvent;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.transfer.item.WorldlyContainerWrapper;

@EventBusSubscriber(modid = HollowFrontier.MODID)
public final class ModBlockEntities {
    private ModBlockEntities() {}

    public static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITIES =
            DeferredRegister.create(Registries.BLOCK_ENTITY_TYPE, HollowFrontier.MODID);

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<BoilerBlockEntity>> BOILER = BLOCK_ENTITIES.register("boiler",
            () -> new BlockEntityType<>(BoilerBlockEntity::new, ModBlocks.BOILER.get()));
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<SteamStorageBlockEntity>> COPPER_PIPE = BLOCK_ENTITIES.register("copper_pipe",
            () -> new BlockEntityType<>(SteamStorageBlockEntity::pipe, ModBlocks.COPPER_PIPE.get()));
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<SteamStorageBlockEntity>> STEAM_TANK = BLOCK_ENTITIES.register("steam_tank",
            () -> new BlockEntityType<>(SteamStorageBlockEntity::tank, ModBlocks.STEAM_TANK.get()));
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<PressureGaugeBlockEntity>> PRESSURE_GAUGE = BLOCK_ENTITIES.register("pressure_gauge",
            () -> new BlockEntityType<>(PressureGaugeBlockEntity::new, ModBlocks.PRESSURE_GAUGE.get()));
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<CrusherBlockEntity>> CRUSHER = BLOCK_ENTITIES.register("crusher",
            () -> new BlockEntityType<>(CrusherBlockEntity::new, ModBlocks.CRUSHER.get()));

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<BountyBoardBlockEntity>> BOUNTY_BOARD = BLOCK_ENTITIES.register("bounty_board",
            () -> new BlockEntityType<>(BountyBoardBlockEntity::new, ModBlocks.BOUNTY_BOARD.get()));
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<SaloonPianoBlockEntity>> SALOON_PIANO = BLOCK_ENTITIES.register("saloon_piano",
            () -> new BlockEntityType<>(SaloonPianoBlockEntity::new, ModBlocks.SALOON_PIANO.get()));
    // No data: it's there so the open well can draw its beam of light (PolarPortalRenderer).
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<PolarPortalBlockEntity>> POLAR_PORTAL = BLOCK_ENTITIES.register("polar_portal",
            () -> new BlockEntityType<>(PolarPortalBlockEntity::new, ModBlocks.POLAR_PORTAL.get()));

    // 13d: the Phlogiston Beacon's count down to the Mothership
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<PhlogistonBeaconBlockEntity>> PHLOGISTON_BEACON = BLOCK_ENTITIES.register("phlogiston_beacon",
            () -> new BlockEntityType<>(PhlogistonBeaconBlockEntity::new, ModBlocks.PHLOGISTON_BEACON.get()));
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<com.hollowfrontier.content.hollow.gray.ImplantRigBlockEntity>> IMPLANT_RIG =
            BLOCK_ENTITIES.register("implant_rig", () -> new BlockEntityType<>(com.hollowfrontier.content.hollow.gray.ImplantRigBlockEntity::new, ModBlocks.IMPLANT_RIG.get()));

    // 47b: the mailbox keeps a barrel's block entity
    @SubscribeEvent
    static void addBlocks(net.neoforged.neoforge.event.BlockEntityTypeAddBlocksEvent event) {
        event.modify(net.minecraft.world.level.block.entity.BlockEntityTypes.BARREL, ModBlocks.MAILBOX.get());
    }

    // Steam moves through the standard fluid capability, so other mods' pipes and tanks interoperate.
    @SubscribeEvent
    static void registerCapabilities(RegisterCapabilitiesEvent event) {
        event.registerBlockEntity(Capabilities.Fluid.BLOCK, BOILER.get(), (be, side) -> be.fluidHandler());
        event.registerBlockEntity(Capabilities.Fluid.BLOCK, COPPER_PIPE.get(), (be, side) -> be.tank());
        event.registerBlockEntity(Capabilities.Fluid.BLOCK, STEAM_TANK.get(), (be, side) -> be.tank());
        event.registerBlockEntity(Capabilities.Fluid.BLOCK, CRUSHER.get(), (be, side) -> be.steamTank());
        // 30: their slots too, with the same faces hoppers get, for other mods' pipes and storage (NeoForge only wraps vanilla's containers)
        event.registerBlockEntity(Capabilities.Item.BLOCK, BOILER.get(), WorldlyContainerWrapper::new);
        event.registerBlockEntity(Capabilities.Item.BLOCK, CRUSHER.get(), WorldlyContainerWrapper::new);
    }
}
