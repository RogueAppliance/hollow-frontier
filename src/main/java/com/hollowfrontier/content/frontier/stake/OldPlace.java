package com.hollowfrontier.content.frontier.stake;

import java.util.List;
import java.util.Locale;
import java.util.Optional;

import com.hollowfrontier.HollowFrontier;
import com.hollowfrontier.content.frontier.raid.TownSite;
import com.hollowfrontier.content.frontier.town.Towns;
import com.hollowfrontier.registry.ModBlocks;
import com.hollowfrontier.registry.ModCriteria;
import com.hollowfrontier.worldgen.structure.OldPlaceBlueprint;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.entity.SignBlockEntity;
import net.minecraft.world.level.block.entity.SignText;
import net.minecraft.world.level.block.entity.SignTextSlot;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.level.levelgen.structure.PoolElementStructurePiece;
import net.minecraft.world.level.levelgen.structure.StructureStart;
import net.minecraft.world.level.levelgen.structure.templatesystem.JigsawReplacementProcessor;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructurePlaceSettings;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplate;

/**
 * Taking up the Old Place (47b). A deed is for one town. Used on the Claim Stake of an Old Place standing empty at that
 * town, it makes the place the player's: their name goes on the board by the gate and in {@link StakeData}. Where the
 * town has none standing (an old world's town, or one whose outskirts drew something else), the deed is used on open,
 * level ground outside the town's bounds and within reach of them, and the same house is raised there, front to the
 * player. One place a town for each player. Nothing here keeps anyone else out: that's a server's own business.
 */
public final class OldPlace {
    private OldPlace() {}

    /** How far the ground under a raised place may vary from where the deed was used. */
    /**
     * What ground a deed will raise a place on: half the lot within two blocks of level, most of it within {@link #MAX_STEP}, none of it more than
     * {@link #WORST_STEP} off, and next to none of it under water. Trees don't count: the hammers clear them. (Played:
     * with every sampled point held to three blocks and a tree counting as a hill, 48 spots round one town were refused
     * before one was taken.)
     */
    public static final int MAX_STEP = 4, WORST_STEP = 8;
    public static final int STANDING_FOR_HOME = 5;

    /** The Old Places a town was built with: the bounds of each. */
    public static List<BoundingBox> standing(StructureStart start) {
        return start.getPieces().stream()
                .filter(p -> p instanceof PoolElementStructurePiece piece && piece.getElement().toString().contains(OldPlaceBlueprint.NAME))
                .map(p -> p.getBoundingBox()).toList();
    }

    /** An Old Place of this town's that nobody has taken, if it has one: its middle. */
    public static Optional<BlockPos> forSale(ServerLevel level, long town) {
        StakeData data = StakeData.get(level);
        List<StakeData.Claim> taken = data.in(town);
        return Towns.start(level, town).flatMap(start -> standing(start).stream()
                .filter(box -> taken.stream().noneMatch(claim -> box.isInside(claim.stake().atY(box.minY()))))
                .map(box -> box.getCenter()).findFirst());
    }

    private static boolean mayTake(ServerPlayer player, TownRef deed) {
        if (!StakeConfig.claims()) {
            return false;
        }
        if (!player.isCreative() && !Standing.rank(player, deed.key()).atLeast(Standing.Rank.NEIGHBOUR)) {
            player.sendOverlayMessage(Component.translatable(StakeText.DEED_STANDING));
            return false;
        }
        if (StakeData.get(player.level()).of(player.getUUID(), deed.key()).isPresent()) {
            player.sendOverlayMessage(Component.translatable(StakeText.DEED_HAVE_ONE, deed.name()));
            return false;
        }
        return true;
    }

    /** A deed used on a Claim Stake. Whether the place was taken. */
    public static boolean claim(ServerLevel level, ServerPlayer player, BlockPos stake, ItemStack deedStack, TownRef deed) {
        StakeData data = StakeData.get(level);
        Optional<StakeData.Claim> owned = data.at(stake);
        if (owned.isPresent()) {
            player.sendOverlayMessage(Component.translatable(StakeText.DEED_OWNED, owned.get().ownerName()));
            return false;
        }
        Optional<Long> town = Towns.near(level, stake, StakeConfig.claimReach()).map(TownSite::keyOf);
        if (town.isEmpty() || town.get() != deed.key()) {
            player.sendOverlayMessage(Component.translatable(StakeText.DEED_WRONG_TOWN, deed.name()));
            return false;
        }
        if (!mayTake(player, deed)) {
            return false;
        }
        record(level, player, stake, deed);
        deedStack.consume(1, player);
        return true;
    }

    /** Puts the place at {@code stake} in the player's name (no checks: claim and raise make them, and tests name the town). */
    public static void record(ServerLevel level, ServerPlayer player, BlockPos stake, TownRef town) {
        String name = player.getGameProfile().name();
        StakeData.get(level).put(new StakeData.Claim(stake.immutable(), town.key(), player.getUUID(), name, -1));
        nameBoard(level, stake, name);
        Standing.add(player, town.key(), STANDING_FOR_HOME);
        level.playSound(null, stake, SoundEvents.PLAYER_LEVELUP, SoundSource.PLAYERS, 0.8F, 1.0F);
        player.sendSystemMessage(Component.translatable(StakeText.CLAIMED, name, town.name()).withStyle(ChatFormatting.GOLD));
        ModCriteria.fire(player, ModCriteria.HOMESTEADER);
    }

    // The for-sale board by the stake takes the owner's name, both sides
    private static void nameBoard(ServerLevel level, BlockPos stake, String name) {
        for (BlockPos pos : BlockPos.betweenClosed(stake.offset(-2, -1, -2), stake.offset(2, 1, 2))) {
            if (level.getBlockEntity(pos) instanceof SignBlockEntity sign) {
                for (SignTextSlot slot : SignTextSlot.values()) {
                    sign.updateText(old -> old.asMutable().setLine(0, Component.empty()).setLine(1, Component.translatable(StakeText.SIGN_THE))
                            .setLine(2, Component.literal(name.toUpperCase(Locale.ROOT))).setLine(3, Component.translatable(StakeText.SIGN_PLACE)).asImmutable(), slot);
                }
                level.sendBlockUpdated(pos, sign.getBlockState(), sign.getBlockState(), Block.UPDATE_CLIENTS);
                return;
            }
        }
    }

    /** A deed used on the ground: the place is raised there, its gate where the deed was used and its front to the player. */
    public static boolean raise(ServerLevel level, ServerPlayer player, BlockPos ground, Direction playerFacing, ItemStack deedStack, TownRef deed) {
        if (!mayTake(player, deed)) {
            return false;
        }
        Optional<StructureStart> town = Towns.start(level, deed.key());
        BlockPos here = player.blockPosition();
        Optional<BlockPos> standing = forSale(level, deed.key());
        if (standing.isPresent()) {
            player.sendOverlayMessage(Component.translatable(StakeText.DEED_STANDS, deed.name(), StakeText.paces(here, standing.get()), StakeText.direction(here, standing.get())));
            return false;
        }
        if (town.isEmpty() || away(town.get().getBoundingBox(), ground) > StakeConfig.claimReach()) {
            player.sendOverlayMessage(Component.translatable(StakeText.DEED_TOO_FAR, deed.name()));
            return false;
        }
        var style = Towns.style(level, town.get());
        StructureTemplate template = level.getStructureTemplateManager().get(HollowFrontier.id(style.template(OldPlaceBlueprint.NAME))).orElse(null);
        if (template == null) {
            return false;
        }
        StructurePlaceSettings settings = new StructurePlaceSettings().setRotation(rotation(playerFacing.getOpposite()))
                .addProcessor(JigsawReplacementProcessor.INSTANCE).setFinalizeEntities(true);
        BlockPos origin = ground.subtract(StructureTemplate.calculateRelativePosition(settings, new BlockPos(OldPlaceBlueprint.GATE_X, 0, 0)));
        BoundingBox box = template.getBoundingBox(settings, origin);
        String problem = problem(level, box, ground.getY());
        if (problem != null) {
            player.sendOverlayMessage(Component.translatable(problem, deed.name()));
            return false;
        }
        footing(level, box, ground.getY());
        template.placeInWorld(level, origin, origin, settings, level.getRandom(), Block.UPDATE_CLIENTS);
        BlockPos stake = origin.offset(StructureTemplate.calculateRelativePosition(settings, OldPlaceBlueprint.STAKE));
        if (!level.getBlockState(stake).is(ModBlocks.CLAIM_STAKE.get())) {
            level.setBlockAndUpdate(stake, ModBlocks.CLAIM_STAKE.get().defaultBlockState());
        }
        player.sendSystemMessage(Component.translatable(StakeText.DEED_RAISED).withStyle(ChatFormatting.GRAY, ChatFormatting.ITALIC));
        record(level, player, stake, deed);
        deedStack.consume(1, player);
        return true;
    }

    /** The template's north front turned to face {@code front}. */
    public static Rotation rotation(Direction front) {
        return switch (front) {
            case EAST -> Rotation.CLOCKWISE_90;
            case SOUTH -> Rotation.CLOCKWISE_180;
            case WEST -> Rotation.COUNTERCLOCKWISE_90;
            default -> Rotation.NONE;
        };
    }

    // How far pos lies outside a town's bounds along x or z (0 inside)
    private static int away(BoundingBox bounds, BlockPos pos) {
        int dx = Math.max(Math.max(bounds.minX() - pos.getX(), pos.getX() - bounds.maxX()), 0);
        int dz = Math.max(Math.max(bounds.minZ() - pos.getZ(), pos.getZ() - bounds.maxZ()), 0);
        return Math.max(dx, dz);
    }

    /** Why a place can't go in {@code box} with its floor at {@code floorY} (a StakeText key), or null if it can. */
    public static String problem(ServerLevel level, BoundingBox box, int floorY) {
        if (!level.hasChunksAt(box.minX(), box.minZ(), box.maxX(), box.maxZ())) {
            return StakeText.DEED_TOO_FAR;
        }
        for (int x : new int[] {box.minX(), box.getCenter().getX(), box.maxX()}) {
            for (int z : new int[] {box.minZ(), box.getCenter().getZ(), box.maxZ()}) {
                if (TownSite.townAt(level, new BlockPos(x, floorY, z)).isPresent()) {
                    return StakeText.DEED_IN_TOWN;
                }
            }
        }
        for (int cx = box.minX() >> 4; cx <= box.maxX() >> 4; cx++) {
            for (int cz = box.minZ() >> 4; cz <= box.maxZ() >> 4; cz++) {
                LevelChunk chunk = level.getChunk(cx, cz);
                if (chunk.getBlockEntities().keySet().stream().anyMatch(pos -> box.isInside(pos.atY(box.minY())) && pos.getY() >= floorY - 2)) {
                    return StakeText.DEED_BUILT;
                }
            }
        }
        int points = 0, rough = 0, wet = 0, level2 = 0;
        for (int x = box.minX(); x <= box.maxX(); x += 3) {
            for (int z = box.minZ(); z <= box.maxZ(); z += 3) {
                int top = soil(level, x, z);
                points++;
                if (!level.getFluidState(new BlockPos(x, top, z)).isEmpty()) {
                    wet++;
                }
                if (Math.abs(top - floorY) > WORST_STEP) {
                    return StakeText.DEED_ROUGH;
                }
                if (Math.abs(top - floorY) > MAX_STEP) {
                    rough++;
                }
                if (Math.abs(top - floorY) <= 2) {
                    level2++;
                }
            }
        }
        return wet * 8 > points ? StakeText.DEED_WET : rough * 4 > points || level2 * 2 < points ? StakeText.DEED_ROUGH : null;
    }

    // Packs earth under the floor wherever the ground falls away, so no corner hangs in the air
    // The top of the ground in a column: under any tree, bush or snow, at the water's surface where it's wet.
    private static int soil(ServerLevel level, int x, int z) {
        BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos(x, level.getHeight(Heightmap.Types.WORLD_SURFACE, x, z) - 1, z);
        while (pos.getY() > level.getMinY()) {
            BlockState state = level.getBlockState(pos);
            boolean growth = state.is(net.minecraft.tags.BlockTags.LOGS) || state.is(net.minecraft.tags.BlockTags.LEAVES) || state.canBeReplaced();
            if (!state.getFluidState().isEmpty() || !growth) {
                break;
            }
            pos.move(Direction.DOWN);
        }
        return pos.getY();
    }

    private static void footing(ServerLevel level, BoundingBox box, int floorY) {
        BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();
        for (int x = box.minX(); x <= box.maxX(); x++) {
            for (int z = box.minZ(); z <= box.maxZ(); z++) {
                for (int y = floorY - 1; y >= floorY - WORST_STEP - 2; y--) {
                    BlockState state = level.getBlockState(pos.set(x, y, z));
                    if (!state.canBeReplaced() && state.getFluidState().isEmpty()) {
                        break;
                    }
                    level.setBlock(pos, Blocks.COARSE_DIRT.defaultBlockState(), Block.UPDATE_CLIENTS);
                }
            }
        }
    }
}
