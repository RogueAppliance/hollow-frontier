package com.hollowfrontier.worldgen.structure;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.jspecify.annotations.Nullable;

import com.hollowfrontier.HollowFrontier;

import net.minecraft.commands.arguments.blocks.BlockStateParser;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.FrontAndTop;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.DoubleTag;
import net.minecraft.nbt.IntTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.level.EmptyBlockGetter;
import net.minecraft.world.level.block.BedBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.ChestBlock;
import net.minecraft.world.level.block.CrossCollisionBlock;
import net.minecraft.world.level.block.DoorBlock;
import net.minecraft.world.level.block.FenceBlock;
import net.minecraft.world.level.block.FenceGateBlock;
import net.minecraft.world.level.block.IronBarsBlock;
import net.minecraft.world.level.block.JigsawBlock;
import net.minecraft.world.level.block.WallBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BedPart;
import net.minecraft.world.level.block.state.properties.DoorHingeSide;
import net.minecraft.world.level.block.state.properties.DoubleBlockHalf;
import net.minecraft.world.level.block.state.properties.WallSide;
import net.minecraft.world.level.storage.loot.LootTable;

/**
 * Code-defined structure template. Blueprints are built in datagen and written to structure .nbt
 * (StructureTemplate format) by StructureNbtProvider, so no client is needed to author structures.
 * Coordinates are template-local; y = 0 is the foundation layer that sits on the terrain surface.
 * Positions never set are left untouched in the world (void); use air() to clear space.
 */
public final class Blueprint {
    private final String name;
    private final Map<BlockPos, BlockState> blocks = new LinkedHashMap<>();
    private final Map<BlockPos, CompoundTag> blockEntityData = new HashMap<>();
    private final List<CompoundTag> entities = new ArrayList<>();
    private int sizeX;
    private int sizeY;
    private int sizeZ;

    public Blueprint(String name) {
        this.name = name;
    }

    public String name() {
        return name;
    }

    public Blueprint set(int x, int y, int z, BlockState state) {
        BlockPos pos = new BlockPos(x, y, z);
        blocks.put(pos, state);
        blockEntityData.remove(pos);
        sizeX = Math.max(sizeX, x + 1);
        sizeY = Math.max(sizeY, y + 1);
        sizeZ = Math.max(sizeZ, z + 1);
        return this;
    }

    /** The width (x) and depth (z) drawn so far. */
    public int sizeX() {
        return sizeX;
    }

    public int sizeZ() {
        return sizeZ;
    }

    /** The height drawn so far. */
    public int sizeY() {
        return sizeY;
    }

    /** Redraws every spot drawn through {@code change} (weathering: a crack here, moss there); block entities stay. */
    public Blueprint map(java.util.function.BiFunction<BlockPos, BlockState, BlockState> change) {
        blocks.replaceAll(change);
        return this;
    }

    /**
     * A decorated pot, its loot rolled from {@code lootTable} when first broken or opened (null for an empty one), and up to
     * four sherds (back, left, right, front; null for a plain side).
     */
    public Blueprint pot(int x, int y, int z, Direction facing, @Nullable ResourceKey<LootTable> lootTable, net.minecraft.world.level.ItemLike... sherds) {
        CompoundTag nbt = new CompoundTag();
        nbt.putString("id", "minecraft:decorated_pot");
        if (lootTable != null) {
            nbt.putString("LootTable", lootTable.identifier().toString());
        }
        if (sherds.length > 0) {
            CompoundTag sides = new CompoundTag();
            String[] names = {"back", "left", "right", "front"};
            for (int i = 0; i < Math.min(4, sherds.length); i++) {
                if (sherds[i] != null) {
                    sides.putString(names[i], BuiltInRegistries.ITEM.getKey(sherds[i].asItem()).toString());
                }
            }
            nbt.put("sherds", sides);
        }
        return blockEntity(x, y, z, Blocks.DECORATED_POT.defaultBlockState().setValue(net.minecraft.world.level.block.DecoratedPotBlock.HORIZONTAL_FACING, facing), nbt);
    }

    /** Suspicious sand or gravel ({@code block}) holding a find from {@code lootTable}, for a brush (28). */
    public Blueprint suspicious(int x, int y, int z, Block block, ResourceKey<LootTable> lootTable) {
        CompoundTag nbt = new CompoundTag();
        nbt.putString("id", "minecraft:brushable_block");
        nbt.putString("LootTable", lootTable.identifier().toString());
        return blockEntity(x, y, z, block.defaultBlockState(), nbt);
    }

    /** Whether anything is drawn at a spot. */
    public boolean has(int x, int y, int z) {
        return blocks.containsKey(new BlockPos(x, y, z));
    }

    /** What's drawn at a spot, or null if nothing is (left as the world has it). */
    public @Nullable BlockState get(BlockPos pos) {
        return blocks.get(pos);
    }

    /** Every spot drawn, in the order first set. */
    public java.util.Set<BlockPos> positions() {
        return java.util.Collections.unmodifiableSet(blocks.keySet());
    }

    public Blueprint set(int x, int y, int z, Block block) {
        return set(x, y, z, block.defaultBlockState());
    }

    /** Fills the inclusive box (corners in any order). */
    public Blueprint fill(int x1, int y1, int z1, int x2, int y2, int z2, BlockState state) {
        for (int x = Math.min(x1, x2); x <= Math.max(x1, x2); x++)
            for (int y = Math.min(y1, y2); y <= Math.max(y1, y2); y++)
                for (int z = Math.min(z1, z2); z <= Math.max(z1, z2); z++)
                    set(x, y, z, state);
        return this;
    }

    public Blueprint fill(int x1, int y1, int z1, int x2, int y2, int z2, Block block) {
        return fill(x1, y1, z1, x2, y2, z2, block.defaultBlockState());
    }

    public Blueprint air(int x1, int y1, int z1, int x2, int y2, int z2) {
        return fill(x1, y1, z1, x2, y2, z2, Blocks.AIR);
    }

    /** Four walls of the box (no floor or ceiling). */
    public Blueprint walls(int x1, int y1, int z1, int x2, int y2, int z2, BlockState state) {
        fill(x1, y1, z1, x2, y2, z1, state);
        fill(x1, y1, z2, x2, y2, z2, state);
        fill(x1, y1, z1, x1, y2, z2, state);
        fill(x2, y1, z1, x2, y2, z2, state);
        return this;
    }

    public Blueprint door(int x, int y, int z, DoorBlock door, Direction facing, DoorHingeSide hinge) {
        BlockState base = door.defaultBlockState().setValue(DoorBlock.FACING, facing).setValue(DoorBlock.HINGE, hinge);
        set(x, y, z, base.setValue(DoorBlock.HALF, DoubleBlockHalf.LOWER));
        return set(x, y + 1, z, base.setValue(DoorBlock.HALF, DoubleBlockHalf.UPPER));
    }

    public Blueprint chest(int x, int y, int z, Direction facing, ResourceKey<LootTable> lootTable) {
        set(x, y, z, Blocks.CHEST.defaultBlockState().setValue(ChestBlock.FACING, facing));
        CompoundTag nbt = new CompoundTag();
        nbt.putString("id", "minecraft:chest");
        nbt.putString("LootTable", lootTable.identifier().toString());
        blockEntityData.put(new BlockPos(x, y, z), nbt);
        return this;
    }

    /**
     * Jigsaw connector facing {@code front}: pieces join where a connector's {@code target} equals the other
     * connector's {@code name} and the two face each other. {@code pool} is what this connector spawns
     * ("minecraft:empty" for a child's own connector); it turns into {@code finalState} once placed.
     */
    public Blueprint jigsaw(int x, int y, int z, FrontAndTop front, String name, String target, String pool, BlockState finalState) {
        return jigsaw(x, y, z, front, name, target, pool, finalState, 0);
    }

    /**
     * As {@link #jigsaw}, with a placement priority: connectors with a higher one have their pieces placed first (a
     * dungeon's way to its end room, before the side rooms can take the space).
     */
    public Blueprint jigsaw(int x, int y, int z, FrontAndTop front, String name, String target, String pool, BlockState finalState, int priority) {
        set(x, y, z, Blocks.JIGSAW.defaultBlockState().setValue(JigsawBlock.ORIENTATION, front));
        CompoundTag nbt = new CompoundTag();
        nbt.putString("id", "minecraft:jigsaw");
        nbt.putString("name", name);
        nbt.putString("target", target);
        nbt.putString("pool", pool);
        nbt.putString("final_state", BlockStateParser.serialize(finalState));
        nbt.putString("joint", "aligned");
        nbt.putInt("placement_priority", priority);
        nbt.putInt("selection_priority", 0);
        blockEntityData.put(new BlockPos(x, y, z), nbt);
        return this;
    }

    /**
     * Marks a chest placed with {@link #chest} as what a raid gang goes for (the town_vault attachment):
     * kind 2 = a bank vault, 1 = the sheriff's strongbox (TownSite).
     */
    public Blueprint vault(int x, int y, int z, int kind) {
        CompoundTag townVault = new CompoundTag();
        townVault.putInt("kind", kind); // saved like NeoForge saves the TOWN_VAULT attachment
        CompoundTag attachments = new CompoundTag();
        attachments.put(HollowFrontier.MODID + ":town_vault", townVault);
        blockEntityData.get(new BlockPos(x, y, z)).put("neoforge:attachments", attachments);
        return this;
    }

    /** A block with its block entity's saved data (e.g. a boiler with coal in it); nbt needs the block entity "id". */
    public Blueprint blockEntity(int x, int y, int z, BlockState state, CompoundTag nbt) {
        set(x, y, z, state);
        blockEntityData.put(new BlockPos(x, y, z), nbt.copy());
        return this;
    }

    /**
     * A shelf facing {@code facing} with up to three items on it, left to right as you face it (a store's goods, a gun
     * rack, the bottles behind a bar). Any shelf block (oak_shelf, spruce_shelf, ...).
     */
    public Blueprint shelf(int x, int y, int z, Block shelf, Direction facing, net.minecraft.world.level.ItemLike... items) {
        CompoundTag nbt = new CompoundTag();
        nbt.putString("id", "minecraft:shelf");
        ListTag list = new ListTag();
        for (int slot = 0; slot < Math.min(3, items.length); slot++) {
            if (items[slot] == null) {
                continue;
            }
            CompoundTag item = new CompoundTag();
            item.putByte("Slot", (byte) slot);
            item.putString("id", BuiltInRegistries.ITEM.getKey(items[slot].asItem()).toString());
            item.putInt("count", 1);
            list.add(item);
        }
        nbt.put("Items", list);
        nbt.putBoolean("align_items_to_bottom", true);
        return blockEntity(x, y, z, shelf.defaultBlockState().setValue(net.minecraft.world.level.block.ShelfBlock.FACING, facing), nbt);
    }

    /**
     * A wall sign hung on the block behind it, its text facing {@code facing}: up to four lines of translation keys
     * (lang from datagen), in {@code color}, glowing if asked (a shop's name over its door).
     */
    public Blueprint wallSign(int x, int y, int z, Block wallSign, Direction facing, net.minecraft.world.item.DyeColor color, boolean glowing, String... keys) {
        java.util.List<net.minecraft.network.chat.Component> lines = new ArrayList<>();
        for (int i = 0; i < 4; i++) {
            lines.add(i < keys.length && !keys[i].isEmpty() ? net.minecraft.network.chat.Component.translatable(keys[i]) : net.minecraft.network.chat.Component.empty());
        }
        return blockEntity(x, y, z, wallSign.defaultBlockState().setValue(net.minecraft.world.level.block.WallSignBlock.FACING, facing), signNbt(lines, color, glowing));
    }

    /** Sign block entity data with the same text front and back. */
    public static CompoundTag signNbt(java.util.List<net.minecraft.network.chat.Component> lines, net.minecraft.world.item.DyeColor color, boolean glowing) {
        CompoundTag sign = new CompoundTag();
        sign.putString("id", "minecraft:sign");
        var text = new net.minecraft.world.level.block.entity.SignText(lines, lines, color, glowing);
        sign.put("front_text", net.minecraft.world.level.block.entity.SignText.CODEC.encodeStart(net.minecraft.nbt.NbtOps.INSTANCE, text).getOrThrow());
        sign.putBoolean("is_waxed", true); // nobody repaints the town's signs
        return sign;
    }

    /** A bed with its foot at (x, y, z) and its head one block toward {@code facing}. */
    public Blueprint bed(int x, int y, int z, Block bed, Direction facing) {
        BlockState base = bed.defaultBlockState().setValue(BedBlock.FACING, facing);
        set(x, y, z, base.setValue(BedBlock.PART, BedPart.FOOT));
        return set(x + facing.getStepX(), y, z + facing.getStepZ(), base.setValue(BedBlock.PART, BedPart.HEAD));
    }

    /** Persistent mob standing on (x, y, z); mobs are finalized (equipment etc.) when the structure places. */
    public Blueprint entity(EntityType<?> type, int x, int y, int z) {
        return entity(type, x, y, z, new CompoundTag());
    }

    /** As entity(), with extra entity NBT (e.g. a townsfolk's "folk" or a lawman's "rank") read before it is finalized. */
    public Blueprint entity(EntityType<?> type, int x, int y, int z, CompoundTag extra) {
        CompoundTag nbt = extra.copy();
        nbt.putString("id", BuiltInRegistries.ENTITY_TYPE.getKey(type).toString());
        nbt.putBoolean("PersistenceRequired", true);
        CompoundTag entry = new CompoundTag();
        entry.put("pos", doubles(x + 0.5, y, z + 0.5));
        entry.put("blockPos", ints(x, y, z));
        entry.put("nbt", nbt);
        entities.add(entry);
        return this;
    }

    /**
     * A hanging Bounty Board (18) on the wall behind (x, y, z), facing out from it. Like a painting, (x, y, z) is the bottom
     * left (as you face it) of a board two across, or the bottom middle of one three across.
     */
    public Blueprint bountyBoard(int x, int y, int z, Direction facing, int columns) {
        CompoundTag nbt = new CompoundTag();
        nbt.putByte("facing", (byte) facing.get2DDataValue());
        nbt.putInt("columns", columns);
        nbt.putIntArray("block_pos", new int[] {x, y, z}); // rewritten where the template is placed
        return entity(com.hollowfrontier.registry.ModEntities.HANGING_BOUNTY_BOARD.get(), x, y, z, nbt);
    }

    /**
     * An armor stand with arms (a shop's dummy, a coat rack) turned to {@code yaw} (0 faces south, 90 west), wearing
     * whatever's given for head, chest, legs and feet (null for nothing). It turns with the structure like any entity.
     */
    public Blueprint armorStand(int x, int y, int z, float yaw, net.minecraft.world.level.ItemLike... worn) {
        CompoundTag nbt = new CompoundTag();
        String[] slots = {"head", "chest", "legs", "feet"};
        CompoundTag equipment = new CompoundTag();
        for (int i = 0; i < Math.min(slots.length, worn.length); i++) {
            if (worn[i] != null) {
                CompoundTag item = new CompoundTag();
                item.putString("id", BuiltInRegistries.ITEM.getKey(worn[i].asItem()).toString());
                item.putInt("count", 1);
                equipment.put(slots[i], item);
            }
        }
        nbt.put("equipment", equipment);
        nbt.putBoolean("ShowArms", true);
        ListTag rotation = new ListTag();
        rotation.add(net.minecraft.nbt.FloatTag.valueOf(yaw));
        rotation.add(net.minecraft.nbt.FloatTag.valueOf(0));
        nbt.put("Rotation", rotation);
        return entity(EntityTypes.ARMOR_STAND, x, y, z, nbt);
    }

    /**
     * A town cat spot: a vanilla cat with only its TOWN_CAT role set (saved like NeoForge saves the attachment). Whether
     * the spot gets a cat, and its coat and accessory, are rolled when it first joins the world (TownCats).
     */
    public Blueprint cat(int x, int y, int z, String role) {
        CompoundTag townCat = new CompoundTag();
        townCat.putString("role", role);
        CompoundTag attachments = new CompoundTag();
        attachments.put(HollowFrontier.MODID + ":town_cat", townCat);
        CompoundTag extra = new CompoundTag();
        extra.put("neoforge:attachments", attachments);
        return entity(EntityTypes.CAT, x, y, z, extra);
    }

    /** Copies another blueprint in at an offset (no rotation). */
    /**
     * A copy under another name with every block passed through {@code restyle} (a palette or town style); block
     * entity data (loot tables, vault marks, connectors) and entities come along unchanged.
     */
    public Blueprint restyled(String newName, java.util.function.UnaryOperator<BlockState> restyle) {
        Blueprint copy = new Blueprint(newName);
        blocks.forEach((pos, state) -> copy.blocks.put(pos, restyle.apply(state)));
        blockEntityData.forEach((pos, nbt) -> copy.blockEntityData.put(pos, nbt.copy()));
        entities.forEach(entity -> copy.entities.add(entity.copy()));
        copy.sizeX = sizeX;
        copy.sizeY = sizeY;
        copy.sizeZ = sizeZ;
        return copy;
    }

    /**
     * Packs bare dirt hard (packed mud), so nothing grows on it: worldgen's grass, flowers and trees come after the
     * structures and take root on any dirt. Dirt under a plant (a tumbleweed, a cactus, crops) is left as it is.
     */
    public Blueprint hardenGround() {
        blocks.replaceAll((pos, state) -> {
            boolean dirt = state.is(Blocks.COARSE_DIRT) || state.is(Blocks.DIRT);
            BlockState above = blocks.getOrDefault(pos.above(), Blocks.AIR.defaultBlockState());
            boolean rooted = above.getBlock() instanceof net.minecraft.world.level.block.VegetationBlock || above.is(Blocks.CACTUS);
            return dirt && !rooted ? Blocks.PACKED_MUD.defaultBlockState() : state;
        });
        return this;
    }

    /**
     * Takes up any carpet on the tile either side of a door, so a rug or runner stops short of the threshold: mobs walking
     * through the door stick on a carpet there (a playtest saw townsfolk stuck, working the door open and shut).
     */
    public Blueprint bareThresholds() {
        for (BlockPos pos : new ArrayList<>(blocks.keySet())) {
            BlockState state = blocks.get(pos);
            if (!(state.getBlock() instanceof DoorBlock) || state.getValue(DoorBlock.HALF) != DoubleBlockHalf.LOWER) {
                continue;
            }
            Direction facing = state.getValue(DoorBlock.FACING);
            for (BlockPos threshold : List.of(pos.relative(facing), pos.relative(facing.getOpposite()))) {
                // By class, not the carpet tag: blueprints are drawn in datagen, where tags aren't bound yet
                if (blocks.getOrDefault(threshold, Blocks.AIR.defaultBlockState()).getBlock() instanceof net.minecraft.world.level.block.CarpetBlock) {
                    blocks.put(threshold, Blocks.AIR.defaultBlockState());
                }
            }
        }
        return this;
    }

    /** How far up {@link #hangLanterns} looks for something to hang a lantern from. */
    public static final int CHAIN_REACH = 8;

    /**
     * Every hanging lantern (and town lamp) with nothing over it gets a chain up to the first thing that is there within
     * {@link #CHAIN_REACH} (a beam, the floor above, the roof), so none floats under an open roof. A lantern with a block
     * over it is left as drawn.
     */
    public Blueprint hangLanterns() {
        for (BlockPos pos : new ArrayList<>(blocks.keySet())) {
            BlockState state = blocks.get(pos);
            if (!(state.getBlock() instanceof net.minecraft.world.level.block.LanternBlock) || !state.getValue(net.minecraft.world.level.block.LanternBlock.HANGING)) {
                continue;
            }
            int top = 0;
            for (int up = 1; up <= CHAIN_REACH && top == 0; up++) {
                BlockState over = blocks.get(pos.above(up));
                if (over != null && !over.isAir()) {
                    top = up;
                }
            }
            for (int up = 1; up < top; up++) {
                set(pos.getX(), pos.getY() + up, pos.getZ(), Blocks.IRON_CHAIN);
            }
        }
        return this;
    }

    public Blueprint paste(Blueprint other, int dx, int dy, int dz) {
        other.blocks.forEach((pos, state) -> set(pos.getX() + dx, pos.getY() + dy, pos.getZ() + dz, state));
        other.blockEntityData.forEach((pos, nbt) -> blockEntityData.put(pos.offset(dx, dy, dz), nbt.copy()));
        for (CompoundTag entity : other.entities) {
            CompoundTag copy = entity.copy();
            ListTag pos = copy.getListOrEmpty("pos");
            ListTag blockPos = copy.getListOrEmpty("blockPos");
            copy.put("pos", doubles(pos.getDoubleOr(0, 0) + dx, pos.getDoubleOr(1, 0) + dy, pos.getDoubleOr(2, 0) + dz));
            copy.put("blockPos", ints(blockPos.getIntOr(0, 0) + dx, blockPos.getIntOr(1, 0) + dy, blockPos.getIntOr(2, 0) + dz));
            entities.add(copy);
        }
        return this;
    }

    /**
     * Fences, iron bars, glass panes and walls keep their connections in the block state. In the world they work them
     * out when a neighbour changes, but structure placement copies the saved state as is, so settle them here from the
     * blueprint's own blocks (vanilla's own checks use block tags, which aren't loaded during datagen).
     */
    private BlockState connected(BlockPos pos, BlockState state) {
        Block self = state.getBlock();
        if (self instanceof FenceBlock || self instanceof IronBarsBlock) {
            for (Direction d : Direction.Plane.HORIZONTAL) {
                state = state.setValue(CrossCollisionBlock.PROPERTY_BY_DIRECTION.get(d), attaches(self, neighbour(pos, d), d));
            }
        } else if (self instanceof WallBlock) {
            boolean north = attaches(self, neighbour(pos, Direction.NORTH), Direction.NORTH);
            boolean south = attaches(self, neighbour(pos, Direction.SOUTH), Direction.SOUTH);
            boolean east = attaches(self, neighbour(pos, Direction.EAST), Direction.EAST);
            boolean west = attaches(self, neighbour(pos, Direction.WEST), Direction.WEST);
            state = state.setValue(WallBlock.NORTH, north ? WallSide.LOW : WallSide.NONE)
                    .setValue(WallBlock.SOUTH, south ? WallSide.LOW : WallSide.NONE)
                    .setValue(WallBlock.EAST, east ? WallSide.LOW : WallSide.NONE)
                    .setValue(WallBlock.WEST, west ? WallSide.LOW : WallSide.NONE)
                    // A straight run is a flat wall; corners, ends and posts get the raised post
                    .setValue(WallBlock.UP, !((north && south && !east && !west) || (east && west && !north && !south)));
        }
        return state;
    }

    private BlockState neighbour(BlockPos pos, Direction direction) {
        return blocks.getOrDefault(pos.relative(direction), Blocks.AIR.defaultBlockState());
    }

    // Same rules as FenceBlock/IronBarsBlock/WallBlock.connectsTo, minus the tags.
    private static boolean attaches(Block self, BlockState other, Direction direction) {
        Block block = other.getBlock();
        boolean solid = !Block.isExceptionForConnection(other) && other.isFaceSturdy(EmptyBlockGetter.INSTANCE, BlockPos.ZERO, direction.getOpposite());
        boolean gate = block instanceof FenceGateBlock && FenceGateBlock.connectsToDirection(other, direction);
        if (self instanceof FenceBlock) {
            boolean sameFence = block instanceof FenceBlock && (block == Blocks.NETHER_BRICK_FENCE) == (self == Blocks.NETHER_BRICK_FENCE);
            return solid || sameFence || gate;
        }
        if (self instanceof IronBarsBlock) {
            return solid || block instanceof IronBarsBlock || block instanceof WallBlock;
        }
        return solid || block instanceof WallBlock || block instanceof IronBarsBlock || gate;
    }

    public CompoundTag toNbt() {
        Map<BlockState, Integer> paletteIndex = new LinkedHashMap<>();
        ListTag palette = new ListTag();
        ListTag blockList = new ListTag();
        blocks.forEach((pos, placed) -> {
            BlockState state = connected(pos, placed);
            int index = paletteIndex.computeIfAbsent(state, s -> {
                palette.add(NbtUtils.writeBlockState(s));
                return paletteIndex.size();
            });
            CompoundTag entry = new CompoundTag();
            entry.put("pos", ints(pos.getX(), pos.getY(), pos.getZ()));
            entry.putInt("state", index);
            CompoundTag nbt = blockEntityData.get(pos);
            if (nbt != null) {
                entry.put("nbt", nbt);
            }
            blockList.add(entry);
        });
        ListTag entityList = new ListTag();
        entityList.addAll(entities);

        CompoundTag root = new CompoundTag();
        root.put("size", ints(sizeX, sizeY, sizeZ));
        root.put("palette", palette);
        root.put("blocks", blockList);
        root.put("entities", entityList);
        return NbtUtils.addCurrentDataVersion(root);
    }

    private static ListTag ints(int... values) {
        ListTag list = new ListTag();
        for (int v : values) {
            list.add(IntTag.valueOf(v));
        }
        return list;
    }

    private static ListTag doubles(double... values) {
        ListTag list = new ListTag();
        for (double v : values) {
            list.add(DoubleTag.valueOf(v));
        }
        return list;
    }
}
