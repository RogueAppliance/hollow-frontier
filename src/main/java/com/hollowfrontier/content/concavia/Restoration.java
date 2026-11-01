package com.hollowfrontier.content.concavia;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import com.hollowfrontier.HollowFrontier;
import com.hollowfrontier.content.frontier.gazette.Aftermath;
import com.hollowfrontier.content.ufo.Leash;
import com.hollowfrontier.registry.ModCriteria;
import com.hollowfrontier.registry.ModItems;
import com.hollowfrontier.registry.ModTags;
import com.hollowfrontier.worldgen.structure.ConcaviaBlueprints;
import com.hollowfrontier.worldgen.structure.ModStructures;
import com.mojang.serialization.Codec;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.TagKey;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.trading.MerchantOffer;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.levelgen.structure.PoolElementStructurePiece;
import net.minecraft.world.level.levelgen.structure.StructureStart;
import net.minecraft.world.level.levelgen.structure.templatesystem.LiquidSettings;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructurePlaceSettings;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplate;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.level.saveddata.SavedDataType;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;

/**
 * Restoring Concavia (15c). Once the leash is off the frontier, a Kindled ally can bring phlogiston shards and starsteel to
 * a Crystal Ruins' fountain and mend the ruin, in {@link ConcaviaBlueprints#STAGES} stages: use either on the fountain
 * with the next stage's {@link #PHLOGISTON} shards and {@link #STARSTEEL} ingots in hand or pack, and they're spent and the stage
 * is laid over the ruin ({@link ConcaviaBlueprints#stageName}, placed as the ruin's own piece was, turned the same way).
 * 1 raises a tower, 2 raises another and lights the sanctum, 3 raises the last, mends the paving and wakes the ruin's
 * waystone ({@link Waystones}). Each stage opens more of the Kindled's trades there ({@link #stageOffers}).
 * <p>
 * The stage each ruin has reached is kept with that world's saved data, by the ruin's structure start.
 */
@EventBusSubscriber(modid = HollowFrontier.MODID)
public class Restoration extends SavedData {
    /** What each stage costs, by stage (index 0 unused). */
    public static final int[] PHLOGISTON = {0, 16, 24, 32}, STARSTEEL = {0, 4, 8, 12};
    /** How near the fountain's middle (across, and up or down) a use counts as on it. */
    public static final double FOUNTAIN_REACH = 4.5;

    public static final Codec<Restoration> CODEC = Codec.unboundedMap(Codec.STRING, Codec.INT).fieldOf("stages")
            .xmap(Restoration::new, r -> r.stages).codec();
    public static final SavedDataType<Restoration> TYPE = new SavedDataType<>(HollowFrontier.id("ruin_restoration"), Restoration::new, CODEC);

    private final Map<String, Integer> stages = new HashMap<>();

    public Restoration() {}

    private Restoration(Map<String, Integer> stages) {
        this.stages.putAll(stages);
    }

    public static Restoration get(ServerLevel level) {
        return level.getDataStorage().computeIfAbsent(TYPE);
    }

    /** A Crystal Ruins: its key (its structure start's chunk), and where and which way its one piece was placed. */
    public record Ruin(long key, BlockPos origin, Rotation rotation) {
        /** A spot in the ruin's template, in the world. */
        public BlockPos toWorld(BlockPos local) {
            return StructureTemplate.calculateRelativePosition(new StructurePlaceSettings().setRotation(rotation), local).offset(origin);
        }
    }

    /** The Crystal Ruins {@code pos} is in, if any. */
    public static Optional<Ruin> at(ServerLevel level, BlockPos pos) {
        StructureStart start = level.structureManager().getStructureWithPieceAt(pos, holder -> holder.is(ModStructures.CRYSTAL_RUINS));
        if (!start.isValid() || !(start.getPieces().getFirst() instanceof PoolElementStructurePiece piece)) {
            return Optional.empty();
        }
        return Optional.of(new Ruin(start.getChunkPos().pack(), piece.getPosition(), piece.getRotation()));
    }

    public int stage(Ruin ruin) {
        return stages.getOrDefault(Long.toString(ruin.key()), 0);
    }

    /** The stage of the ruin at {@code pos}: 0 if it's untouched, or not in a ruin. */
    public static int stageAt(ServerLevel level, BlockPos pos) {
        return at(level, pos).map(ruin -> get(level).stage(ruin)).orElse(0);
    }

    /** Whether {@code pos} is on the ruin's fountain. */
    public static boolean onFountain(Ruin ruin, BlockPos pos) {
        Vec3 fountain = Vec3.atBottomCenterOf(ruin.toWorld(ConcaviaBlueprints.FOUNTAIN));
        Vec3 at = Vec3.atBottomCenterOf(pos);
        return at.subtract(fountain).horizontalDistance() <= FOUNTAIN_REACH && Math.abs(at.y - fountain.y) <= FOUNTAIN_REACH;
    }

    /**
     * {@code player} brings the next stage's cost to the ruin's fountain: if they're the Kindled's ally and carry it all,
     * it's spent and the stage is laid. Null if it was, or why not ({@code not_after}, {@code not_ally}, {@code done},
     * {@code short}).
     */
    public static String restore(ServerLevel level, ServerPlayer player, Ruin ruin) {
        Restoration restoration = get(level);
        int next = restoration.stage(ruin) + 1;
        if (!Leash.isBroken(level)) {
            return "not_after";
        }
        if (!KindledStanding.allied(player)) {
            return "not_ally";
        }
        if (next > ConcaviaBlueprints.STAGES) {
            return "done";
        }
        if (!player.getAbilities().instabuild) {
            if (count(player, ModTags.Items.GEMS_PHLOGISTON) < PHLOGISTON[next] || count(player, ModTags.Items.INGOTS_STARSTEEL) < STARSTEEL[next]) {
                return "short";
            }
            take(player, ModTags.Items.GEMS_PHLOGISTON, PHLOGISTON[next]);
            take(player, ModTags.Items.INGOTS_STARSTEEL, STARSTEEL[next]);
        }
        advance(level, ruin);
        player.sendSystemMessage(Component.translatable("message.hollowfrontier.restoration.stage." + next).withStyle(ChatFormatting.AQUA, ChatFormatting.ITALIC));
        ModCriteria.fire(player, ModCriteria.RUIN_RESTORED, next);
        BlockPos fountain = ruin.toWorld(ConcaviaBlueprints.FOUNTAIN);
        Aftermath.get(level).record(level, next == ConcaviaBlueprints.STAGES ? Aftermath.Kind.WAYSTONE : Aftermath.Kind.RUIN, "", fountain);
        return null;
    }

    /** Lays the ruin's next stage (whoever pays): the blocks, the light and sound, and at the last, its waystone woken. The stage now. */
    public static int advance(ServerLevel level, Ruin ruin) {
        Restoration restoration = get(level);
        int next = restoration.stage(ruin) + 1;
        if (next > ConcaviaBlueprints.STAGES) {
            return next - 1;
        }
        StructureTemplate stage = level.getStructureTemplateManager().get(HollowFrontier.id(ConcaviaBlueprints.stageName(next))).orElseThrow();
        StructurePlaceSettings settings = new StructurePlaceSettings().setRotation(ruin.rotation()).setIgnoreEntities(true)
                .setLiquidSettings(LiquidSettings.IGNORE_WATERLOGGING);
        stage.placeInWorld(level, ruin.origin(), ruin.origin(), settings, level.getRandom(), Block.UPDATE_ALL);
        restoration.stages.put(Long.toString(ruin.key()), next);
        restoration.setDirty();
        Vec3 fountain = Vec3.atBottomCenterOf(ruin.toWorld(ConcaviaBlueprints.FOUNTAIN));
        level.sendParticles(ParticleTypes.END_ROD, fountain.x, fountain.y + 2, fountain.z, 80, 3, 2, 3, 0.05);
        level.playSound(null, fountain.x, fountain.y, fountain.z, SoundEvents.BEACON_POWER_SELECT, SoundSource.BLOCKS, 2.0F, 0.8F);
        level.playSound(null, fountain.x, fountain.y, fountain.z, SoundEvents.AMETHYST_BLOCK_RESONATE, SoundSource.BLOCKS, 2.0F, 0.6F);
        if (next == ConcaviaBlueprints.STAGES) {
            Waystones.get(level).wake(level.dimension(), ruin.toWorld(ConcaviaBlueprints.WAYSTONE), ruin.toWorld(ConcaviaBlueprints.WAYSTONE_ARRIVAL));
        }
        return next;
    }

    /** What the Kindled of a ruin at {@code stage} sell besides their usual: building stone, then cells and sunstone, then a core. */
    public static List<MerchantOffer> stageOffers(int stage) {
        Item phlogiston = ModItems.PHLOGISTON_SHARD.get();
        var items = new ArrayList<MerchantOffer>();
        if (stage >= 1) {
            items.add(Kindled.buy(phlogiston, 1, ModItems.CRYSTAL_BRICK_STAIRS.get(), 8));
            items.add(Kindled.buy(phlogiston, 1, ModItems.CRYSTAL_BRICK_WALL.get(), 8));
        }
        if (stage >= 2) {
            items.add(Kindled.buy(phlogiston, 4, ModItems.PHLOGISTON_CELL.get(), 1));
            items.add(Kindled.buy(phlogiston, 6, ModItems.SUNSTONE_SHARD.get(), 1));
        }
        if (stage >= 3) {
            items.add(Kindled.buy(phlogiston, 32, ModItems.PHLOGISTON_CORE.get(), 1));
        }
        return items;
    }

    private static int count(Player player, TagKey<Item> tag) {
        int have = 0;
        for (int slot = 0; slot < player.getInventory().getContainerSize(); slot++) {
            ItemStack stack = player.getInventory().getItem(slot);
            if (stack.is(tag)) {
                have += stack.getCount();
            }
        }
        return have;
    }

    private static void take(Player player, TagKey<Item> tag, int amount) {
        var inventory = player.getInventory();
        for (int slot = 0; slot < inventory.getContainerSize() && amount > 0; slot++) {
            ItemStack stack = inventory.getItem(slot);
            if (stack.is(tag)) {
                int taken = Math.min(amount, stack.getCount());
                stack.shrink(taken);
                amount -= taken;
            }
        }
    }

    /** A phlogiston shard or starsteel ingot used on a ruin's fountain: the next stage, or why not. */
    @SubscribeEvent
    static void onUseBlock(PlayerInteractEvent.RightClickBlock event) {
        ItemStack held = event.getItemStack();
        if (!(held.is(ModTags.Items.GEMS_PHLOGISTON) || held.is(ModTags.Items.INGOTS_STARSTEEL))
                || !(event.getLevel() instanceof ServerLevel level) || !(event.getEntity() instanceof ServerPlayer player)) {
            return;
        }
        Optional<Ruin> ruin = at(level, event.getPos()).filter(r -> onFountain(r, event.getPos()));
        if (ruin.isEmpty()) {
            return;
        }
        int next = get(level).stage(ruin.get()) + 1;
        String refused = restore(level, player, ruin.get());
        if (refused != null) {
            int stage = Math.min(next, ConcaviaBlueprints.STAGES);
            player.sendOverlayMessage(Component.translatable("message.hollowfrontier.restoration." + refused, PHLOGISTON[stage], STARSTEEL[stage], next)
                    .withStyle(ChatFormatting.GRAY));
        }
        event.setCanceled(true);
        event.setCancellationResult(InteractionResult.SUCCESS);
    }
}
