package com.hollowfrontier.datagen;

import com.geckolib.renderer.internal.GeckolibItemSpecialRenderer;
import com.hollowfrontier.HollowFrontier;
import com.hollowfrontier.content.ufo.PhlogistonBeaconBlock;
import com.hollowfrontier.content.frontier.cat.TownCat;
import com.hollowfrontier.content.hollow.gateway.GatewayFrameBlock;
import com.hollowfrontier.content.steam.BoilerBlock;
import com.hollowfrontier.content.steam.HandcarEngineBlock;
import com.hollowfrontier.registry.ModBlocks;
import com.hollowfrontier.registry.ModItems;

import net.minecraft.client.color.item.Dye;
import net.minecraft.client.renderer.item.properties.numeric.CompassAngle;
import net.minecraft.client.renderer.item.properties.numeric.CompassAngleState;
import net.minecraft.client.data.models.BlockModelGenerators;
import net.minecraft.client.data.models.ItemModelGenerators;
import net.minecraft.client.data.models.ModelProvider;
import net.minecraft.client.data.models.MultiVariant;
import net.minecraft.client.data.models.blockstates.MultiPartGenerator;
import net.minecraft.client.data.models.blockstates.MultiVariantGenerator;
import net.minecraft.client.data.models.model.ItemModelUtils;
import net.minecraft.client.data.models.model.ModelLocationUtils;
import net.minecraft.client.data.models.model.ModelTemplate;
import net.minecraft.client.data.models.model.ModelTemplates;
import net.minecraft.client.data.models.model.TextureMapping;
import net.minecraft.client.data.models.model.TextureSlot;
import net.minecraft.client.data.models.model.TexturedModel;
import net.minecraft.core.Direction;
import net.minecraft.data.PackOutput;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.neoforged.neoforge.client.model.generators.template.ExtendedModelTemplateBuilder;

// Generates blockstates, block/item models and client item definitions. Every mod block and item must be covered.
public class ModModelProvider extends ModelProvider {
    // Bar stool: leather cushion, seat, post and foot (matches BarStoolBlock's shape).
    private static final TextureSlot CUSHION = TextureSlot.create("cushion");
    private static final ModelTemplate STOOL = ExtendedModelTemplateBuilder.builder()
            .parent(Identifier.withDefaultNamespace("block/block"))
            .requiredTextureSlot(TextureSlot.ALL)
            .requiredTextureSlot(CUSHION)
            .requiredTextureSlot(TextureSlot.PARTICLE)
            .element(e -> e.from(4, 11, 4).to(12, 13, 12).textureAll(CUSHION))
            .element(e -> e.from(3, 9, 3).to(13, 11, 13).textureAll(TextureSlot.ALL))
            .element(e -> e.from(6.5F, 0, 6.5F).to(9.5F, 9, 9.5F).textureAll(TextureSlot.ALL))
            .element(e -> e.from(4, 0, 4).to(12, 1, 12).textureAll(TextureSlot.ALL))
            .build();
    // 24c: a chair facing north (four legs, a seat, the ladder back to the south) and a table (a top on four legs), in any
    // planks (matching ChairBlock's and TableBlock's shapes)
    private static final ModelTemplate CHAIR = ExtendedModelTemplateBuilder.builder()
            .parent(Identifier.withDefaultNamespace("block/block"))
            .requiredTextureSlot(TextureSlot.ALL)
            .requiredTextureSlot(TextureSlot.PARTICLE)
            .element(e -> e.from(2, 7, 2).to(14, 9, 14).textureAll(TextureSlot.ALL))
            .element(e -> e.from(2, 9, 12).to(14, 16, 14).textureAll(TextureSlot.ALL))
            .element(e -> e.from(2, 0, 2).to(4, 7, 4).textureAll(TextureSlot.ALL))
            .element(e -> e.from(12, 0, 2).to(14, 7, 4).textureAll(TextureSlot.ALL))
            .element(e -> e.from(2, 0, 12).to(4, 7, 14).textureAll(TextureSlot.ALL))
            .element(e -> e.from(12, 0, 12).to(14, 7, 14).textureAll(TextureSlot.ALL))
            .build();
    private static final ModelTemplate TABLE = ExtendedModelTemplateBuilder.builder()
            .parent(Identifier.withDefaultNamespace("block/block"))
            .requiredTextureSlot(TextureSlot.ALL)
            .requiredTextureSlot(TextureSlot.PARTICLE)
            .element(e -> e.from(0, 13, 0).to(16, 16, 16).textureAll(TextureSlot.ALL))
            .element(e -> e.from(1, 0, 1).to(3, 13, 3).textureAll(TextureSlot.ALL))
            .element(e -> e.from(13, 0, 1).to(15, 13, 3).textureAll(TextureSlot.ALL))
            .element(e -> e.from(1, 0, 13).to(3, 13, 15).textureAll(TextureSlot.ALL))
            .element(e -> e.from(13, 0, 13).to(15, 13, 15).textureAll(TextureSlot.ALL))
            .build();
    // Saloon piano (facing north): case with a panelled front, keybed with the keys on top, two legs, lid.
    private static final TextureSlot KEYS = TextureSlot.create("keys");
    private static final ModelTemplate PIANO = ExtendedModelTemplateBuilder.builder()
            .parent(Identifier.withDefaultNamespace("block/block"))
            .requiredTextureSlot(TextureSlot.SIDE)
            .requiredTextureSlot(TextureSlot.FRONT)
            .requiredTextureSlot(KEYS)
            .requiredTextureSlot(TextureSlot.PARTICLE)
            .element(e -> e.from(1, 0, 7).to(15, 15, 15).textureAll(TextureSlot.SIDE).face(Direction.NORTH, f -> f.texture(TextureSlot.FRONT)))
            .element(e -> e.from(1, 8, 3).to(15, 10, 7).textureAll(TextureSlot.SIDE).face(Direction.UP, f -> f.texture(KEYS)))
            .element(e -> e.from(1.5F, 0, 3).to(3, 8, 4.5F).textureAll(TextureSlot.SIDE))
            .element(e -> e.from(13, 0, 3).to(14.5F, 8, 4.5F).textureAll(TextureSlot.SIDE))
            .element(e -> e.from(0.5F, 15, 6.5F).to(15.5F, 16, 15.5F).textureAll(TextureSlot.SIDE))
            .build();
    // Card table: a felt top on a pedestal and foot (matches CardTableBlock's shape).
    private static final ModelTemplate CARD_TABLE = ExtendedModelTemplateBuilder.builder()
            .parent(Identifier.withDefaultNamespace("block/block"))
            .requiredTextureSlot(TextureSlot.TOP)
            .requiredTextureSlot(TextureSlot.SIDE)
            .requiredTextureSlot(TextureSlot.PARTICLE)
            .element(e -> e.from(0, 13, 0).to(16, 16, 16).textureAll(TextureSlot.SIDE).face(Direction.UP, f -> f.texture(TextureSlot.TOP)))
            .element(e -> e.from(6, 2, 6).to(10, 13, 10).textureAll(TextureSlot.SIDE))
            .element(e -> e.from(3, 0, 3).to(13, 2, 13).textureAll(TextureSlot.SIDE))
            .build();
    // Copper pipe: 4x4 core plus one north-facing arm, rotated per connected side (matches PipeBlock(6) shapes).
    private static final ModelTemplate PIPE_CORE = ExtendedModelTemplateBuilder.builder()
            .parent(Identifier.withDefaultNamespace("block/block"))
            .suffix("_core")
            .requiredTextureSlot(TextureSlot.ALL)
            .requiredTextureSlot(TextureSlot.PARTICLE)
            .element(e -> e.from(5, 5, 5).to(11, 11, 11).textureAll(TextureSlot.ALL))
            .build();
    private static final ModelTemplate PIPE_SIDE = ExtendedModelTemplateBuilder.builder()
            .parent(Identifier.withDefaultNamespace("block/block"))
            .suffix("_side")
            .requiredTextureSlot(TextureSlot.ALL)
            .requiredTextureSlot(TextureSlot.PARTICLE)
            .element(e -> e.from(5, 5, 0).to(11, 11, 5).textureAll(TextureSlot.ALL))
            .build();

    // Copper pipe in the inventory: a straight run end to end (the bare 6px core read as a dot in the creative tab).
    private static final ModelTemplate PIPE_ITEM = ExtendedModelTemplateBuilder.builder()
            .parent(Identifier.withDefaultNamespace("block/block"))
            .suffix("_inventory")
            .requiredTextureSlot(TextureSlot.ALL)
            .requiredTextureSlot(TextureSlot.PARTICLE)
            .element(e -> e.from(5, 5, 0).to(11, 11, 16).textureAll(TextureSlot.ALL))
            .element(e -> e.from(4, 4, 7).to(12, 12, 9).textureAll(TextureSlot.ALL)) // flange
            .build();

    // Handcar engine (a minecart display block, drawn at 0.75 scale in the cart): a firebox at one end with an upright brass
    // boiler on it, a smokestack and a gauge, leaving the rest of the cart for the rider.
    private static final TextureSlot BRASS = TextureSlot.create("brass");
    private static final TextureSlot FIREBOX = TextureSlot.create("firebox");
    private static final ModelTemplate HANDCAR_ENGINE = ExtendedModelTemplateBuilder.builder()
            .parent(Identifier.withDefaultNamespace("block/block"))
            .requiredTextureSlot(TextureSlot.SIDE)
            .requiredTextureSlot(BRASS)
            .requiredTextureSlot(FIREBOX)
            .requiredTextureSlot(TextureSlot.PARTICLE)
            // Tall enough to stand well clear of the cart's rim (the cart sinks the display block and draws it at 0.75).
            .element(e -> e.from(2, 0, 10).to(14, 8, 16).textureAll(TextureSlot.SIDE).face(Direction.NORTH, f -> f.texture(FIREBOX)))
            .element(e -> e.from(3, 8, 10.5F).to(13, 20, 15.5F).textureAll(BRASS))
            .element(e -> e.from(6.5F, 20, 11.5F).to(9.5F, 29, 14.5F).textureAll(TextureSlot.SIDE))
            .element(e -> e.from(5.5F, 28, 10.5F).to(10.5F, 31, 15.5F).textureAll(TextureSlot.SIDE))
            .element(e -> e.from(10.5F, 14, 9.5F).to(12.5F, 16, 10.5F).textureAll(TextureSlot.SIDE).face(Direction.NORTH, f -> f.texture(BRASS)))
            .build();

    // Polar Gateway well: a sheet of light lying in the bottom of the block (no sides; its pale texture is see-through).
    private static final ModelTemplate PORTAL_SHEET = ExtendedModelTemplateBuilder.builder()
            .parent(Identifier.withDefaultNamespace("block/block"))
            .requiredTextureSlot(TextureSlot.ALL)
            .requiredTextureSlot(TextureSlot.PARTICLE)
            .element(e -> e.from(0, 10, 0).to(16, 10.5F, 16)
                    .face(Direction.UP, f -> f.texture(TextureSlot.ALL))
                    .face(Direction.DOWN, f -> f.texture(TextureSlot.ALL)))
            .build();

    public ModModelProvider(PackOutput output) {
        super(output, HollowFrontier.MODID);
    }

    @Override
    protected void registerModels(BlockModelGenerators blockModels, ItemModelGenerators itemModels) {
        blockModels.createTrivialCube(ModBlocks.BRASS_BLOCK.get());
        // The Hollow Earth's crust and its unbreakable shell
        blockModels.createTrivialCube(ModBlocks.CRUST_ROCK.get());
        blockModels.createTrivialCube(ModBlocks.JELLY_LAMP.get()); // 43b
        blockModels.createTrivialCube(ModBlocks.SHELL_BEDROCK.get());
        // Hollow Earth flora and ground (9b1): fronds as leaves, ash like sand, the vent a cracked top over basalt sides
        blockModels.createTrivialBlock(ModBlocks.FERN_FROND.get(), TexturedModel.LEAVES);
        blockModels.createTrivialCube(ModBlocks.ASH.get());
        blockModels.createTrivialBlock(ModBlocks.ASH_VENT.get(), TexturedModel.CUBE_BOTTOM_TOP);
        // 9c: starsteel and phlogiston
        // 11a: a crop circle's flattened wheat, a carpet of laid stalks
        blockModels.createTrivialBlock(ModBlocks.FLATTENED_WHEAT.get(), TexturedModel.CARPET);
        for (var block : java.util.List.of(ModBlocks.STARSTEEL_ORE, ModBlocks.DEEPSLATE_STARSTEEL_ORE, ModBlocks.RAW_STARSTEEL_BLOCK,
                ModBlocks.STARSTEEL_BLOCK, ModBlocks.PHLOGISTON_ORE, ModBlocks.DEEPSLATE_PHLOGISTON_ORE, ModBlocks.PHLOGISTON_LAMP)) {
            blockModels.createTrivialCube(block.get());
        }
        // 12a: Concavia's crystal stone. The crystal's texture is see-through, so it draws translucent like stained glass.
        blockModels.createTrivialCube(ModBlocks.CONCAVIAN_CRYSTAL.get());
        blockModels.family(ModBlocks.CRYSTAL_BRICKS.get()).stairs(ModBlocks.CRYSTAL_BRICK_STAIRS.get()).slab(ModBlocks.CRYSTAL_BRICK_SLAB.get())
                .wall(ModBlocks.CRYSTAL_BRICK_WALL.get());
        blockModels.createTrivialCube(ModBlocks.CHISELED_CRYSTAL_BRICKS.get());
        blockModels.createTrivialCube(ModBlocks.SUN_PYLON.get()); // 12b
        blockModels.createTrivialCube(ModBlocks.SAUCER_HULL.get()); // 13a
        blockModels.createTrivialCube(ModBlocks.SAUCER_WRECKAGE.get());
        blockModels.createTrivialCube(ModBlocks.GRAY_CACHE.get()); // 15a
        blockModels.createTrivialBlock(ModBlocks.WAYSTONE.get(), TexturedModel.COLUMN); // 15c
        blockModels.createTrivialBlock(ModBlocks.IMPLANT_RIG.get(), TexturedModel.COLUMN); // 20
        // 26: the vault's seal, and its sockets (a lit top where the mural marks one)
        blockModels.createTrivialCube(ModBlocks.KINDLED_SEAL.get());
        Block socket = ModBlocks.SEAL_SOCKET.get();
        TextureMapping socketTextures = TextureMapping.cubeBottomTop(socket);
        Identifier socketPlain = ModelTemplates.CUBE_BOTTOM_TOP.create(socket, socketTextures, blockModels.modelOutput);
        Identifier socketMarked = ModelTemplates.CUBE_BOTTOM_TOP.createWithSuffix(socket, "_marked",
                socketTextures.copyAndUpdate(TextureSlot.TOP, TextureMapping.getBlockTexture(socket, "_top_marked")), blockModels.modelOutput);
        blockModels.blockStateOutput.accept(MultiVariantGenerator.dispatch(socket).with(BlockModelGenerators.createBooleanModelDispatch(
                com.hollowfrontier.content.concavia.SealSocketBlock.MARKED, BlockModelGenerators.plainVariant(socketMarked), BlockModelGenerators.plainVariant(socketPlain))));

        itemModels.generateFlatItem(ModItems.BRASS_INGOT.get(), ModelTemplates.FLAT_ITEM);
        itemModels.generateFlatItem(ModItems.BRASS_NUGGET.get(), ModelTemplates.FLAT_ITEM);
        itemModels.generateFlatItem(ModItems.BRASS_PLATE.get(), ModelTemplates.FLAT_ITEM);
        itemModels.generateFlatItem(ModItems.BRASS_GEAR.get(), ModelTemplates.FLAT_ITEM);
        itemModels.generateFlatItem(ModItems.PRESSURE_VALVE.get(), ModelTemplates.FLAT_ITEM);

        itemModels.generateFlatItem(ModItems.PERCUSSION_CAP.get(), ModelTemplates.FLAT_ITEM);
        itemModels.generateFlatItem(ModItems.REVOLVER_CARTRIDGE.get(), ModelTemplates.FLAT_ITEM);
        itemModels.generateFlatItem(ModItems.RIFLE_CARTRIDGE.get(), ModelTemplates.FLAT_ITEM);
        itemModels.generateFlatItem(ModItems.BUCKSHOT_CARTRIDGE.get(), ModelTemplates.FLAT_ITEM);
        // Guns are GeckoLib models; see gun() for how the display transforms were worked out.
        //               item                         fp scale  fp translation     tp scale  gui scale tilt  gui translation
        gun(itemModels, ModItems.REVOLVER.get(),     0.60F, -2.2F, 2.6F, -2.1F,  0.45F,    0.95F, 15.0F, -3.6F, -2.15F);
        gun(itemModels, ModItems.PLAINS_RIFLE.get(), 0.45F, -1.8F, 1.6F, -4.5F,  0.46F,    0.40F, 45.0F, -1.65F, -2.5F);
        gun(itemModels, ModItems.SHOTGUN.get(),      0.48F, -1.8F, 1.6F, -4.5F,  0.45F,    0.45F, 45.0F, -0.95F, -2.1F);
        // The phlogiston guns (13b) are built to the revolver's and the plains rifle's lengths, so they take near enough the same
        gun(itemModels, ModItems.RAY_PISTOL.get(),   0.60F, -2.2F, 3.8F, -2.1F,  0.45F,    0.95F, 15.0F, -3.6F, -1.6F); // its barrel sits lower
        gun(itemModels, ModItems.PHLOGISTON_RIFLE.get(),   0.45F, -1.8F, 1.6F, -4.5F,  0.46F,    0.42F, 45.0F, -1.4F, -2.5F);

        itemModels.generateFlatItem(ModItems.COWBOY_HAT.get(), ModelTemplates.FLAT_ITEM);
        itemModels.generateFlatItem(ModItems.DUSTER.get(), ModelTemplates.FLAT_ITEM);
        itemModels.generateFlatItem(ModItems.CHAPS.get(), ModelTemplates.FLAT_ITEM);
        itemModels.generateFlatItem(ModItems.COWBOY_BOOTS.get(), ModelTemplates.FLAT_ITEM);
        itemModels.generateFlatItem(ModItems.SNAKESKIN_BOOTS.get(), ModelTemplates.FLAT_ITEM);
        itemModels.generateFlatItem(ModItems.LASSO.get(), ModelTemplates.FLAT_ITEM);
        itemModels.generateFlatItem(ModItems.IRON_HORSESHOES.get(), ModelTemplates.FLAT_ITEM);
        itemModels.generateFlatItem(ModItems.BRASS_HORSESHOES.get(), ModelTemplates.FLAT_ITEM);
        itemModels.generateFlatItem(ModItems.SADDLEBAGS.get(), ModelTemplates.FLAT_ITEM);

        blockModels.createTrivialCube(ModBlocks.BOUNTY_BOARD.get());
        blockModels.createTrivialBlock(ModBlocks.BAR_COUNTER.get(), TexturedModel.CUBE_BOTTOM_TOP);
        blockModels.createDoor(ModBlocks.SALOON_DOORS.get());
        Block stool = ModBlocks.BAR_STOOL.get();
        Identifier stoolModel = STOOL.create(stool, TextureMapping.cube(stool).copySlot(TextureSlot.ALL, TextureSlot.PARTICLE)
                .put(CUSHION, TextureMapping.getBlockTexture(stool, "_cushion")), blockModels.modelOutput);
        blockModels.blockStateOutput.accept(BlockModelGenerators.createSimpleBlock(stool, BlockModelGenerators.plainVariant(stoolModel)));
        // 24c: chairs (turned to face the way they're placed) and tables, textured with the vanilla planks of their wood
        Object[][] furniture = {{ModBlocks.OAK_CHAIR.get(), ModBlocks.OAK_TABLE.get(), net.minecraft.world.level.block.Blocks.OAK_PLANKS},
                {ModBlocks.SPRUCE_CHAIR.get(), ModBlocks.SPRUCE_TABLE.get(), net.minecraft.world.level.block.Blocks.SPRUCE_PLANKS},
                {ModBlocks.DARK_OAK_CHAIR.get(), ModBlocks.DARK_OAK_TABLE.get(), net.minecraft.world.level.block.Blocks.DARK_OAK_PLANKS}};
        for (Object[] wood : furniture) {
            TextureMapping planks = new TextureMapping().put(TextureSlot.ALL, TextureMapping.getBlockTexture((Block) wood[2]))
                    .put(TextureSlot.PARTICLE, TextureMapping.getBlockTexture((Block) wood[2]));
            Block chair = (Block) wood[0], table = (Block) wood[1];
            blockModels.blockStateOutput.accept(MultiVariantGenerator.dispatch(chair,
                    BlockModelGenerators.plainVariant(CHAIR.create(chair, planks, blockModels.modelOutput))).with(BlockModelGenerators.ROTATION_HORIZONTAL_FACING));
            blockModels.blockStateOutput.accept(BlockModelGenerators.createSimpleBlock(table,
                    BlockModelGenerators.plainVariant(TABLE.create(table, planks, blockModels.modelOutput))));
        }
        // The town lamp: vanilla's lantern when lit, a cold lantern (town_lamp_unlit, reworked from vanilla's) when not; the
        // item is vanilla's lantern sprite
        Block lamp = ModBlocks.TOWN_LAMP.get();
        TextureMapping cold = new TextureMapping().put(TextureSlot.LANTERN, TextureMapping.getBlockTexture(lamp, "_unlit"));
        Identifier coldStanding = net.minecraft.client.data.models.model.ModelTemplates.LANTERN.createWithSuffix(lamp, "_unlit", cold, blockModels.modelOutput);
        Identifier coldHanging = net.minecraft.client.data.models.model.ModelTemplates.HANGING_LANTERN.createWithSuffix(lamp, "_unlit", cold, blockModels.modelOutput);
        // 35a: dressed for December, lit and unlit: the same two models with a sprig and a red ribbon drawn on the texture
        TextureMapping festiveLit = new TextureMapping().put(TextureSlot.LANTERN, TextureMapping.getBlockTexture(lamp, "_festive"));
        TextureMapping festiveCold = new TextureMapping().put(TextureSlot.LANTERN, TextureMapping.getBlockTexture(lamp, "_festive_unlit"));
        Identifier festiveStanding = ModelTemplates.LANTERN.createWithSuffix(lamp, "_festive", festiveLit, blockModels.modelOutput);
        Identifier festiveHanging = ModelTemplates.HANGING_LANTERN.createWithSuffix(lamp, "_festive", festiveLit, blockModels.modelOutput);
        Identifier festiveColdStanding = ModelTemplates.LANTERN.createWithSuffix(lamp, "_festive_unlit", festiveCold, blockModels.modelOutput);
        Identifier festiveColdHanging = ModelTemplates.HANGING_LANTERN.createWithSuffix(lamp, "_festive_unlit", festiveCold, blockModels.modelOutput);
        blockModels.blockStateOutput.accept(MultiVariantGenerator.dispatch(lamp).with(
                net.minecraft.client.data.models.blockstates.PropertyDispatch.initial(BlockStateProperties.HANGING, BlockStateProperties.LIT,
                                com.hollowfrontier.content.frontier.furniture.TownLampBlock.FESTIVE)
                        .select(false, true, false, BlockModelGenerators.plainVariant(Identifier.withDefaultNamespace("block/lantern")))
                        .select(true, true, false, BlockModelGenerators.plainVariant(Identifier.withDefaultNamespace("block/lantern_hanging")))
                        .select(false, false, false, BlockModelGenerators.plainVariant(coldStanding))
                        .select(true, false, false, BlockModelGenerators.plainVariant(coldHanging))
                        .select(false, true, true, BlockModelGenerators.plainVariant(festiveStanding))
                        .select(true, true, true, BlockModelGenerators.plainVariant(festiveHanging))
                        .select(false, false, true, BlockModelGenerators.plainVariant(festiveColdStanding))
                        .select(true, false, true, BlockModelGenerators.plainVariant(festiveColdHanging))));
        itemModels.itemModelOutput.accept(lamp.asItem(), net.minecraft.client.data.models.model.ItemModelUtils.plainModel(
                ModelTemplates.FLAT_ITEM.create(lamp.asItem(), TextureMapping.layer0(net.minecraft.world.item.Items.LANTERN), itemModels.modelOutput)));
        Block piano = ModBlocks.SALOON_PIANO.get();
        TextureMapping pianoTextures = new TextureMapping()
                .put(TextureSlot.SIDE, TextureMapping.getBlockTexture(piano, "_wood"))
                .put(TextureSlot.FRONT, TextureMapping.getBlockTexture(piano, "_front"))
                .put(KEYS, TextureMapping.getBlockTexture(piano, "_keys"))
                .put(TextureSlot.PARTICLE, TextureMapping.getBlockTexture(piano, "_wood"));
        blockModels.blockStateOutput.accept(MultiVariantGenerator.dispatch(piano,
                BlockModelGenerators.plainVariant(PIANO.create(piano, pianoTextures, blockModels.modelOutput))).with(BlockModelGenerators.ROTATION_HORIZONTAL_FACING));

        Block cardTable = ModBlocks.CARD_TABLE.get();
        TextureMapping cardTableTextures = new TextureMapping()
                .put(TextureSlot.TOP, TextureMapping.getBlockTexture(cardTable, "_felt"))
                .put(TextureSlot.SIDE, TextureMapping.getBlockTexture(cardTable, "_wood"))
                .put(TextureSlot.PARTICLE, TextureMapping.getBlockTexture(cardTable, "_wood"));
        blockModels.blockStateOutput.accept(BlockModelGenerators.createSimpleBlock(cardTable,
                BlockModelGenerators.plainVariant(CARD_TABLE.create(cardTable, cardTableTextures, blockModels.modelOutput))));

        // Steam machines
        Block boiler = ModBlocks.BOILER.get();
        TextureMapping boilerTextures = TextureMapping.cubeBottomTop(boiler);
        Identifier boilerOff = ModelTemplates.CUBE_BOTTOM_TOP.create(boiler, boilerTextures, blockModels.modelOutput);
        Identifier boilerOn = ModelTemplates.CUBE_BOTTOM_TOP.createWithSuffix(boiler, "_on",
                boilerTextures.copyAndUpdate(TextureSlot.SIDE, TextureMapping.getBlockTexture(boiler, "_side_on")), blockModels.modelOutput);
        blockModels.blockStateOutput.accept(MultiVariantGenerator.dispatch(boiler).with(BlockModelGenerators.createBooleanModelDispatch(
                BoilerBlock.LIT, BlockModelGenerators.plainVariant(boilerOn), BlockModelGenerators.plainVariant(boilerOff))));
        blockModels.createTrivialBlock(ModBlocks.CRUSHER.get(), TexturedModel.CUBE_BOTTOM_TOP);
        blockModels.createTrivialBlock(ModBlocks.STEAM_TANK.get(), TexturedModel.CUBE_BOTTOM_TOP);
        blockModels.createTrivialCube(ModBlocks.PRESSURE_GAUGE.get());

        Block pipe = ModBlocks.COPPER_PIPE.get();
        TextureMapping pipeTextures = TextureMapping.cube(pipe).copySlot(TextureSlot.ALL, TextureSlot.PARTICLE);
        MultiVariant core = BlockModelGenerators.plainVariant(PIPE_CORE.create(pipe, pipeTextures, blockModels.modelOutput));
        MultiVariant side = BlockModelGenerators.plainVariant(PIPE_SIDE.create(pipe, pipeTextures, blockModels.modelOutput));
        blockModels.blockStateOutput.accept(MultiPartGenerator.multiPart(pipe)
                .with(core)
                .with(BlockModelGenerators.condition().term(BlockStateProperties.NORTH, true), side)
                .with(BlockModelGenerators.condition().term(BlockStateProperties.EAST, true), side.with(BlockModelGenerators.Y_ROT_90))
                .with(BlockModelGenerators.condition().term(BlockStateProperties.SOUTH, true), side.with(BlockModelGenerators.Y_ROT_180))
                .with(BlockModelGenerators.condition().term(BlockStateProperties.WEST, true), side.with(BlockModelGenerators.Y_ROT_270))
                .with(BlockModelGenerators.condition().term(BlockStateProperties.UP, true), side.with(BlockModelGenerators.X_ROT_270))
                .with(BlockModelGenerators.condition().term(BlockStateProperties.DOWN, true), side.with(BlockModelGenerators.X_ROT_90)));
        blockModels.registerSimpleItemModel(pipe, PIPE_ITEM.create(pipe, pipeTextures, blockModels.modelOutput));

        itemModels.generateFlatItem(ModItems.STEAM_CANISTER.get(), ModelTemplates.FLAT_ITEM);
        itemModels.generateFlatItem(ModItems.STEAM_HANDCAR.get(), ModelTemplates.FLAT_ITEM);
        Block engine = ModBlocks.HANDCAR_ENGINE.get();
        TextureMapping engineTextures = new TextureMapping()
                .put(TextureSlot.SIDE, TextureMapping.getBlockTexture(engine, "_iron"))
                .put(BRASS, TextureMapping.getBlockTexture(engine, "_brass"))
                .put(FIREBOX, TextureMapping.getBlockTexture(engine, "_firebox"))
                .put(TextureSlot.PARTICLE, TextureMapping.getBlockTexture(engine, "_iron"));
        Identifier engineOff = HANDCAR_ENGINE.create(engine, engineTextures, blockModels.modelOutput);
        Identifier engineOn = HANDCAR_ENGINE.createWithSuffix(engine, "_on",
                engineTextures.copyAndUpdate(FIREBOX, TextureMapping.getBlockTexture(engine, "_firebox_on")), blockModels.modelOutput);
        blockModels.blockStateOutput.accept(MultiVariantGenerator.dispatch(engine).with(BlockModelGenerators.createBooleanModelDispatch(
                HandcarEngineBlock.LIT, BlockModelGenerators.plainVariant(engineOn), BlockModelGenerators.plainVariant(engineOff))));
        // Airship and the polar expedition
        itemModels.generateFlatItem(ModItems.AIRSHIP_ENVELOPE.get(), ModelTemplates.FLAT_ITEM);
        itemModels.generateFlatItem(ModItems.AIRSHIP_ENGINE.get(), ModelTemplates.FLAT_ITEM);
        itemModels.generateFlatItem(ModItems.AIRSHIP.get(), ModelTemplates.FLAT_ITEM);
        itemModels.generateFlatItem(ModItems.STEAM_SUBMERSIBLE.get(), ModelTemplates.FLAT_ITEM); // 43c
        heldModels(itemModels); // the resonator, the drill and the lens: flat icons, and held GeckoLib models
        // The polar compass points like a lodestone compass at its bearing (polar_compass_00..31), and spins without one.
        Item compass = ModItems.POLAR_COMPASS.get();
        itemModels.itemModelOutput.accept(compass, ItemModelUtils.rangeSelect(
                new CompassAngle(true, CompassAngleState.CompassTarget.LODESTONE), 32.0F, itemModels.createCompassModels(compass)));
        // 32c: the crash site's cargo pod, sealed (its band lit) and open
        Block pod = ModBlocks.GRAY_CARGO_POD.get();
        TextureMapping podTextures = TextureMapping.cubeBottomTop(pod);
        Identifier podSealed = ModelTemplates.CUBE_BOTTOM_TOP.create(pod, podTextures, blockModels.modelOutput);
        Identifier podOpen = ModelTemplates.CUBE_BOTTOM_TOP.createWithSuffix(pod, "_open", podTextures
                .copyAndUpdate(TextureSlot.TOP, TextureMapping.getBlockTexture(pod, "_top_open"))
                .copyAndUpdate(TextureSlot.SIDE, TextureMapping.getBlockTexture(pod, "_side_open")), blockModels.modelOutput);
        blockModels.blockStateOutput.accept(MultiVariantGenerator.dispatch(pod).with(BlockModelGenerators.createBooleanModelDispatch(
                com.hollowfrontier.content.ufo.CargoPodBlock.OPEN, BlockModelGenerators.plainVariant(podOpen), BlockModelGenerators.plainVariant(podSealed))));
        blockModels.registerSimpleItemModel(pod, podSealed);
        Block frame = ModBlocks.GATEWAY_FRAME.get();
        TextureMapping frameTextures = TextureMapping.cubeBottomTop(frame);
        Identifier frameDark = ModelTemplates.CUBE_BOTTOM_TOP.create(frame, frameTextures, blockModels.modelOutput);
        Identifier frameLit = ModelTemplates.CUBE_BOTTOM_TOP.createWithSuffix(frame, "_active", frameTextures
                .copyAndUpdate(TextureSlot.TOP, TextureMapping.getBlockTexture(frame, "_top_active"))
                .copyAndUpdate(TextureSlot.SIDE, TextureMapping.getBlockTexture(frame, "_side_active")), blockModels.modelOutput);
        blockModels.blockStateOutput.accept(MultiVariantGenerator.dispatch(frame).with(BlockModelGenerators.createBooleanModelDispatch(
                GatewayFrameBlock.ACTIVE, BlockModelGenerators.plainVariant(frameLit), BlockModelGenerators.plainVariant(frameDark))));
        blockModels.registerSimpleItemModel(frame, frameDark);
        // 13d: the Phlogiston Beacon, dark and lit
        Block beacon = ModBlocks.PHLOGISTON_BEACON.get();
        TextureMapping beaconTextures = TextureMapping.cubeBottomTop(beacon);
        Identifier beaconDark = ModelTemplates.CUBE_BOTTOM_TOP.create(beacon, beaconTextures, blockModels.modelOutput);
        Identifier beaconLit = ModelTemplates.CUBE_BOTTOM_TOP.createWithSuffix(beacon, "_lit", beaconTextures
                .copyAndUpdate(TextureSlot.TOP, TextureMapping.getBlockTexture(beacon, "_top_lit"))
                .copyAndUpdate(TextureSlot.SIDE, TextureMapping.getBlockTexture(beacon, "_side_lit")), blockModels.modelOutput);
        blockModels.blockStateOutput.accept(MultiVariantGenerator.dispatch(beacon).with(BlockModelGenerators.createBooleanModelDispatch(
                PhlogistonBeaconBlock.LIT, BlockModelGenerators.plainVariant(beaconLit), BlockModelGenerators.plainVariant(beaconDark))));
        blockModels.registerSimpleItemModel(beacon, beaconDark);
        Block portal = ModBlocks.POLAR_PORTAL.get();
        Identifier sheet = PORTAL_SHEET.create(portal, TextureMapping.cube(portal).copySlot(TextureSlot.ALL, TextureSlot.PARTICLE), blockModels.modelOutput);
        blockModels.blockStateOutput.accept(BlockModelGenerators.createSimpleBlock(portal, BlockModelGenerators.plainVariant(sheet)));

        itemModels.generateFlatItem(ModItems.IRON_DUST.get(), ModelTemplates.FLAT_ITEM);
        itemModels.generateFlatItem(ModItems.GOLD_DUST.get(), ModelTemplates.FLAT_ITEM);
        itemModels.generateFlatItem(ModItems.COPPER_DUST.get(), ModelTemplates.FLAT_ITEM);
        itemModels.generateFlatItem(ModItems.COPPER_COIN.get(), ModelTemplates.FLAT_ITEM);
        itemModels.generateFlatItem(ModItems.GOLD_COIN.get(), ModelTemplates.FLAT_ITEM);
        itemModels.generateFlatItem(ModItems.WANTED_POSTER.get(), ModelTemplates.FLAT_ITEM);
        itemModels.generateFlatItem(ModItems.HANGING_BOUNTY_BOARD.get(), ModelTemplates.FLAT_ITEM);
        itemModels.generateFlatItem(ModItems.OUTLAW_BADGE.get(), ModelTemplates.FLAT_ITEM);
        itemModels.generateFlatItem(ModItems.OUTLAW_SPAWN_EGG.get(), ModelTemplates.FLAT_ITEM);
        itemModels.generateFlatItem(ModItems.OUTLAW_GUNSLINGER_SPAWN_EGG.get(), ModelTemplates.FLAT_ITEM);
        itemModels.generateFlatItem(ModItems.OUTLAW_BOSS_SPAWN_EGG.get(), ModelTemplates.FLAT_ITEM);
        itemModels.generateFlatItem(ModItems.TOWNSFOLK_SPAWN_EGG.get(), ModelTemplates.FLAT_ITEM);
        itemModels.generateFlatItem(ModItems.BARMAID_SPAWN_EGG.get(), ModelTemplates.FLAT_ITEM);
        itemModels.generateFlatItem(ModItems.LAWMAN_SPAWN_EGG.get(), ModelTemplates.FLAT_ITEM);
        itemModels.generateFlatItem(ModItems.RATTLESNAKE_SPAWN_EGG.get(), ModelTemplates.FLAT_ITEM);
        itemModels.generateFlatItem(ModItems.FERN_SKITTER_SPAWN_EGG.get(), ModelTemplates.FLAT_ITEM);
        itemModels.generateFlatItem(ModItems.GLASSBACK_HARE_SPAWN_EGG.get(), ModelTemplates.FLAT_ITEM);
        itemModels.generateFlatItem(ModItems.ASH_SALAMANDER_SPAWN_EGG.get(), ModelTemplates.FLAT_ITEM);
        itemModels.generateFlatItem(ModItems.PALE_RAVEN_SPAWN_EGG.get(), ModelTemplates.FLAT_ITEM);
        itemModels.generateFlatItem(ModItems.GRAY_PROBE_SPAWN_EGG.get(), ModelTemplates.FLAT_ITEM);
        for (var item : java.util.List.of(ModItems.RAW_STARSTEEL, ModItems.STARSTEEL_INGOT, ModItems.STARSTEEL_NUGGET, ModItems.STARSTEEL_DUST,
                ModItems.PHLOGISTON_SHARD, ModItems.STARSTEEL_HELMET, ModItems.STARSTEEL_CHESTPLATE, ModItems.STARSTEEL_LEGGINGS, ModItems.STARSTEEL_BOOTS,
                ModItems.STARSTEEL_HORSESHOES)) {
            itemModels.generateFlatItem(item.get(), ModelTemplates.FLAT_ITEM);
        }
        // The Signal Locator points like a lodestone compass at its bearing (signal_locator_00..31), and spins without one (10b)
        Item locator = ModItems.SIGNAL_LOCATOR.get();
        itemModels.itemModelOutput.accept(locator, ItemModelUtils.rangeSelect(
                new CompassAngle(true, CompassAngleState.CompassTarget.LODESTONE), 32.0F, itemModels.createCompassModels(locator)));
        itemModels.generateFlatItem(ModItems.GRAY_TECH_FRAGMENT.get(), ModelTemplates.FLAT_ITEM);
        itemModels.generateFlatItem(ModItems.STRANGE_SKULL.get(), ModelTemplates.FLAT_ITEM); // 11a
        itemModels.generateFlatItem(ModItems.MAN_IN_BLACK_SPAWN_EGG.get(), ModelTemplates.FLAT_ITEM); // 11b
        itemModels.generateFlatItem(ModItems.GRAY_SENTINEL_SPAWN_EGG.get(), ModelTemplates.FLAT_ITEM);
        itemModels.generateFlatItem(ModItems.KINDLED_SIGIL.get(), ModelTemplates.FLAT_ITEM); // 12a
        itemModels.generateFlatItem(ModItems.KINDLED_SPAWN_EGG.get(), ModelTemplates.FLAT_ITEM);
        itemModels.generateFlatItem(ModItems.CRYSTAL_GUARDIAN_SPAWN_EGG.get(), ModelTemplates.FLAT_ITEM);
        for (var item : java.util.List.of(ModItems.SUNSTONE_SHARD, ModItems.PHLOGISTON_CORE, ModItems.STARSTEEL_CROWN,
                ModItems.CONCAVIAN_CODEX, ModItems.SUN_ARCHON_SPAWN_EGG, // 12b
                ModItems.ALIEN_ALLOY_PLATING, ModItems.DAMAGED_GRAVITY_DRIVE, ModItems.BEAM_EMITTER, ModItems.LENS_ARRAY, ModItems.NAVIGATION_CORE,
                ModItems.GRAY_WARDEN_SPAWN_EGG, ModItems.DISGUISED_GRAY_SPAWN_EGG, // 13a, 21c
                ModItems.EMPTY_PHLOGISTON_CELL, ModItems.PHLOGISTON_CELL, ModItems.GRAVITY_DRIVE, // 13b
                ModItems.PROTOTYPE_SAUCER, // 13c
                ModItems.CLOAKING_DEVICE, ModItems.CARGO_HOLD, ModItems.LETTER_HOME, ModItems.SEALED_HULL)) { // 15b, 20, 43d
            itemModels.generateFlatItem(item.get(), ModelTemplates.FLAT_ITEM);
        }
        // 28: the relics, the sherds and the trim template
        ModItems.RELICS.forEach(relic -> itemModels.generateFlatItem(relic.get(), ModelTemplates.FLAT_ITEM));
        ModItems.GADGETS.forEach(gadget -> itemModels.generateFlatItem(gadget.get(), ModelTemplates.FLAT_ITEM)); // 32c
        ModItems.SHERDS.forEach(sherd -> itemModels.generateFlatItem(sherd.get(), ModelTemplates.FLAT_ITEM));
        itemModels.generateFlatItem(ModItems.CONCAVIAN_ARMOR_TRIM_SMITHING_TEMPLATE.get(), ModelTemplates.FLAT_ITEM);
        // The Still Sea (43b): eggs and drops flat, the harpoons held like tools
        for (var item : java.util.List.of(ModItems.GREAT_SEA_COW_SPAWN_EGG, ModItems.BASILOSAURUS_SPAWN_EGG, ModItems.PLESIOSAUR_SPAWN_EGG,
                ModItems.LAMPJELLY_SPAWN_EGG, ModItems.BLUBBER, ModItems.TALLOW, ModItems.BASILOSAUR_TOOTH, ModItems.LAMP_GEL)) {
            itemModels.generateFlatItem(item.get(), ModelTemplates.FLAT_ITEM);
        }
        // The megafauna (10a): eggs, drops and gear
        for (var item : java.util.List.of(ModItems.WOOLLY_MAMMOTH_SPAWN_EGG, ModItems.TERROR_BIRD_SPAWN_EGG, ModItems.MEGATHERIUM_SPAWN_EGG, ModItems.GLYPTODON_SPAWN_EGG,
                ModItems.MAMMOTH_TUSK, ModItems.RAW_MAMMOTH, ModItems.MAMMOTH_STEAK, ModItems.OLIPHANT, ModItems.TERROR_BIRD_TALON, ModItems.THICK_HIDE,
                ModItems.HIDE_BARDING, ModItems.GLYPTODON_SCUTE, ModItems.GLYPTODON_HELMET, ModItems.GLYPTODON_CHESTPLATE, ModItems.GLYPTODON_LEGGINGS,
                ModItems.GLYPTODON_BOOTS)) {
            itemModels.generateFlatItem(item.get(), ModelTemplates.FLAT_ITEM);
        }
        for (var tool : java.util.List.of(ModItems.STARSTEEL_SWORD, ModItems.STARSTEEL_PAXEL, ModItems.TALON_KNIFE)) {
            itemModels.generateFlatItem(tool.get(), ModelTemplates.FLAT_HANDHELD_ITEM);
        }
        itemModels.generateFlatItem(ModItems.DYNAMITE.get(), ModelTemplates.FLAT_ITEM);
        itemModels.generateFlatItem(ModItems.SNAKESKIN.get(), ModelTemplates.FLAT_ITEM);
        // The bandana texture is pale cloth, tinted with its dye (red when undyed).
        Item bandana = ModItems.BANDANA.get();
        itemModels.itemModelOutput.accept(bandana, ItemModelUtils.tintedModel(
                ModelTemplates.FLAT_ITEM.create(bandana, TextureMapping.layer0(bandana), itemModels.modelOutput), new Dye(TownCat.Accessory.DEFAULT_BANDANA)));
        itemModels.generateFlatItem(ModItems.BRASS_STAR.get(), ModelTemplates.FLAT_ITEM);
        itemModels.generateFlatItem(ModItems.FRONTIER_GAZETTE.get(), ModelTemplates.FLAT_ITEM);
        itemModels.generateFlatItem(ModItems.SURVEY_LETTER.get(), ModelTemplates.FLAT_ITEM); // 32d
        itemModels.generateFlatItem(ModItems.CHRISTMAS_PARCEL.get(), ModelTemplates.FLAT_ITEM); // 35b
        itemModels.generateFlatItem(ModItems.FRUITCAKE.get(), ModelTemplates.FLAT_ITEM);
        for (var drink : java.util.List.of(ModItems.FRONTIER_BEER, ModItems.SARSAPARILLA, ModItems.COWBOY_COFFEE, ModItems.RYE_WHISKEY,
                ModItems.MEZCAL, ModItems.MOONSHINE, ModItems.SNAKE_OIL, ModItems.EGGNOG, ModItems.HOT_CIDER)) {
            itemModels.generateFlatItem(drink.get(), ModelTemplates.FLAT_ITEM);
        }
    }

    /**
     * The tools held as GeckoLib models (the Polar Resonator, the Steam Drill, the Sunstone Lens): their flat icons
     * ({@code item/<name>_icon}) in the inventory, in frames, on shelves and on the ground, as vanilla does the spyglass;
     * held, the GeckoLib model under a base model carrying only the hand transforms. Placed in hand by eye in runPhoto.
     * <ul>
     * <li>Upright ones (the resonator and the lens: grip at the origin, up +Y, their face toward the holder +Z) stand in
     *     first person toed in a little; in third person, where an item's +Y points ahead of a hanging arm, a quarter turn
     *     about X stands them up, and {@code tpLift} raises the grip into the fist.</li>
     * <li>The drill is held like a pistol (grip at the origin, bit along -Z), as the guns are in first person; in third
     *     person the same quarter turn points the bit ahead.</li>
     * </ul>
     */
    private static void heldModels(ItemModelGenerators itemModels) {
        held(itemModels, ModItems.POLAR_RESONATOR.get(), t -> t.rotation(0, -12, 0).translation(-2.0F, 2.5F, -2.5F).scale(0.6F), 3.2F);
        held(itemModels, ModItems.SUNSTONE_LENS.get(), t -> t.rotation(0, -12, 0).translation(-2.0F, 2.0F, -2.5F).scale(0.6F), 2.4F);
        held(itemModels, ModItems.STEAM_DRILL.get(), t -> t.rotation(0, 6, 0).translation(-1.6F, 2.2F, -3.5F).scale(0.45F), 1.9F);
        // 43b: the harpoons, held like the drill (grip at the origin, head along -Z), a little bigger
        held(itemModels, ModItems.HARPOON.get(), t -> t.rotation(0, 4, 0).translation(-1.2F, 2.6F, -2.0F).scale(0.6F), 1.9F);
        held(itemModels, ModItems.TOOTHED_HARPOON.get(), t -> t.rotation(0, 4, 0).translation(-1.2F, 2.6F, -2.0F).scale(0.6F), 1.9F);
    }

    private static void held(ItemModelGenerators itemModels, Item item,
                             java.util.function.Consumer<net.neoforged.neoforge.client.model.generators.template.TransformVecBuilder> firstPerson,
                             float tpLift) {
        net.minecraft.client.resources.model.sprite.Material iconTexture = TextureMapping.getItemTexture(item, "_icon");
        Identifier icon = ModelTemplates.FLAT_ITEM.create(ModelLocationUtils.getModelLocation(item, "_icon"), TextureMapping.layer0(iconTexture),
                itemModels.modelOutput);
        ModelTemplate held = ExtendedModelTemplateBuilder.builder()
                .requiredTextureSlot(TextureSlot.PARTICLE)
                .transform(ItemDisplayContext.FIRST_PERSON_RIGHT_HAND, firstPerson)
                .transform(ItemDisplayContext.FIRST_PERSON_LEFT_HAND, firstPerson)
                .transform(ItemDisplayContext.THIRD_PERSON_RIGHT_HAND, t -> t.rotation(90, 0, 0).translation(0, 1.5F, tpLift).scale(0.45F))
                .transform(ItemDisplayContext.THIRD_PERSON_LEFT_HAND, t -> t.rotation(90, 0, 0).translation(0, 1.5F, tpLift).scale(0.45F))
                .transform(ItemDisplayContext.HEAD, t -> t.translation(0, 8, 0).scale(0.6F))
                .build();
        Identifier base = held.create(item, new TextureMapping().put(TextureSlot.PARTICLE, iconTexture), itemModels.modelOutput);
        itemModels.itemModelOutput.accept(item, ItemModelGenerators.createFlatModelDispatch(ItemModelUtils.plainModel(icon),
                ItemModelUtils.specialModel(base, new GeckolibItemSpecialRenderer.Unbaked<>())));
    }

    /**
     * A GeckoLib-rendered gun: the client item is a special model ("geckolib:geckolib") whose base model only carries the
     * particle texture and display transforms. The geo models put the grip at the origin with the barrel along -Z, +Y up.
     * <ul>
     * <li>First person: the hand space already looks down -Z, so the gun just sits there, toed in a few degrees. The grip
     *     starts at (0.56, -0.52, -0.72) from the eye; the long guns are pushed forward so the stock stays clear of the
     *     camera, and the revolver is lifted so its grip shows (checked with runPhoto).</li>
     * <li>Third person: GunClientExtensions raises the arm (crossbow hold); in that pose item -Z is forward and +Y up,
     *     so no rotation is needed either; it drops 1px so the grip sits in the fist rather than on top of it.</li>
     * <li>GUI/frame: turn the barrel to +X with the lock side (+X) facing the viewer (yaw -90), then tilt the muzzle up by
     *     guiTilt about the view axis. As one XYZ Euler rotation that is [90, guiTilt - 90, 90] (right_rotation would be
     *     simpler, but NeoForge datagen writes it beside a "left_rotation" key the model loader ignores). Translation
     *     re-centres the gun (its centre, rotated and scaled, negated).</li>
     * </ul>
     */
    private static void gun(ItemModelGenerators itemModels, Item item, float fpScale, float fpX, float fpY, float fpZ, float tpScale,
                            float guiScale, float guiTilt, float guiX, float guiY) {
        ModelTemplate template = ExtendedModelTemplateBuilder.builder()
                .requiredTextureSlot(TextureSlot.PARTICLE)
                .transform(ItemDisplayContext.FIRST_PERSON_RIGHT_HAND, t -> t.rotation(0, 4, 0).translation(fpX, fpY, fpZ).scale(fpScale))
                .transform(ItemDisplayContext.FIRST_PERSON_LEFT_HAND, t -> t.rotation(0, 4, 0).translation(fpX, fpY, fpZ).scale(fpScale))
                .transform(ItemDisplayContext.THIRD_PERSON_RIGHT_HAND, t -> t.translation(0, -1, 0).scale(tpScale))
                .transform(ItemDisplayContext.THIRD_PERSON_LEFT_HAND, t -> t.translation(0, -1, 0).scale(tpScale))
                .transform(ItemDisplayContext.GUI, t -> t.rotation(90, guiTilt - 90, 90).translation(guiX, guiY, 0).scale(guiScale))
                .transform(ItemDisplayContext.FIXED, t -> t.rotation(90, guiTilt - 90, 90).translation(guiX, guiY, 0).scale(guiScale))
                .transform(ItemDisplayContext.GROUND, t -> t.translation(0, 2, 0).scale(tpScale * 0.8F))
                .build();
        Identifier base = template.create(item, TextureMapping.particleFromItem(item), itemModels.modelOutput);
        itemModels.itemModelOutput.accept(item, ItemModelUtils.specialModel(base, new GeckolibItemSpecialRenderer.Unbaked<>()));
    }
}
