package com.hollowfrontier.content.frontier.saloon;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import org.jspecify.annotations.Nullable;

import com.hollowfrontier.HollowFrontier;
import com.hollowfrontier.content.frontier.bounty.BoardUse;
import com.hollowfrontier.content.frontier.raid.TownSite;
import com.hollowfrontier.registry.ModCriteria;
import com.hollowfrontier.registry.ModItems;
import com.hollowfrontier.registry.ModMobEffects;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.BlockTags;
import net.minecraft.util.RandomSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.ItemLore;
import net.minecraft.world.level.block.BedBlock;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BedPart;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.level.levelgen.structure.StructureStart;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.MobEffectEvent;

/**
 * Missing Time? (21b), the saloon's answer to the abductions. Now and then ({@link SaloonConfig#missingTimeChance}),
 * when a player's Tipsy III or worse wears off in a town, they come to somewhere else in it: in a bed, on a hay bale
 * (the sheriff's cells have one), or sitting in a water trough. They've lost an hour, they're wearing a hat that isn't
 * theirs (a worn Cowboy Hat), and they earn the hidden "Missing Time?".
 * <p>
 * It's safe on purpose: only within {@link #REACH} blocks and inside the same town, only a spot with room to stand and
 * no hostile mob within {@link #CLEAR_OF} blocks, no fall (the player is set down on the spot), nothing taken. Nowhere
 * safe, nothing happens. Servers can turn it off ({@link SaloonConfig#missingTime}).
 */
@EventBusSubscriber(modid = HollowFrontier.MODID)
public final class MissingTime {
    private MissingTime() {}

    public static final int REACH = 32, HEIGHT = 6;
    public static final double CLEAR_OF = 12.0;
    /** A Tipsy this strong or stronger wearing off can do it (III: amplifier 2). */
    public static final int FROM_AMPLIFIER = 2;

    /** Where a player can come to, and what they wake up in. */
    public record Spot(Vec3 at, String kind) {}

    @SubscribeEvent
    static void onExpired(MobEffectEvent.Expired event) {
        MobEffectInstance effect = event.getEffectInstance();
        if (effect == null || !effect.is(ModMobEffects.TIPSY) || effect.getAmplifier() < FROM_AMPLIFIER
                || !(event.getEntity() instanceof ServerPlayer player) || !SaloonConfig.missingTime()
                || player.getRandom().nextDouble() >= SaloonConfig.missingTimeChance()) {
            return;
        }
        wakeUp((ServerLevel) player.level(), player, player.getRandom());
    }

    /** The player comes to somewhere safe nearby in their town, if there's anywhere. Where, or null (nothing happened). */
    public static @Nullable Spot wakeUp(ServerLevel level, ServerPlayer player, RandomSource random) {
        if (player.isSpectator() || player.isPassenger()) {
            return null;
        }
        Optional<StructureStart> town = TownSite.townAt(level, player.blockPosition());
        return town.isEmpty() ? null : wakeUp(level, player, random, spots(level, player.blockPosition(), town.get()));
    }

    /** As above, at one of these spots (none: nothing happens). Public so game tests can supply a town's. */
    public static @Nullable Spot wakeUp(ServerLevel level, ServerPlayer player, RandomSource random, List<Spot> spots) {
        if (spots.isEmpty()) {
            return null;
        }
        Spot spot = spots.get(random.nextInt(spots.size()));
        player.stopRiding();
        player.teleportTo(spot.at().x, spot.at().y, spot.at().z);
        player.resetFallDistance();
        player.addEffect(new MobEffectInstance(MobEffects.BLINDNESS, 60, 0, false, false));
        player.addEffect(new MobEffectInstance(MobEffects.NAUSEA, 120, 0, false, false));
        player.sendSystemMessage(Component.translatable("message.hollowfrontier.missing_time." + spot.kind()).withStyle(ChatFormatting.GOLD, ChatFormatting.ITALIC));
        BoardUse.give(player, wornHat(random));
        ModCriteria.fire(player, ModCriteria.MISSING_TIME_DRUNK);
        return spot;
    }

    /** Somebody else's hat: a Cowboy Hat, well worn, that fits anyway. */
    public static ItemStack wornHat(RandomSource random) {
        ItemStack hat = new ItemStack(ModItems.COWBOY_HAT.get());
        hat.set(DataComponents.CUSTOM_NAME, Component.translatable("item.hollowfrontier.worn_cowboy_hat").withStyle(s -> s.withItalic(false)));
        hat.set(DataComponents.LORE, new ItemLore(List.of(Component.translatable("lore.hollowfrontier.worn_cowboy_hat").withStyle(ChatFormatting.GRAY))));
        if (hat.isDamageableItem()) {
            hat.setDamageValue(hat.getMaxDamage() / 2 + random.nextInt(Math.max(1, hat.getMaxDamage() / 4)));
        }
        return hat;
    }

    /**
     * The safe spots near {@code around} in its town: a bed, a hay bale or a water trough, inside the same town, with
     * room over it, and no hostile mob near.
     */
    public static List<Spot> spots(ServerLevel level, BlockPos around, StructureStart town) {
        // Being in the town is being in its bounds (as TownSite.townAt has it), so only the part of the reach inside them
        // is scanned, with no structure lookup per spot
        BoundingBox bounds = town.getBoundingBox();
        BlockPos from = new BlockPos(Math.max(around.getX() - REACH, bounds.minX()), Math.max(around.getY() - HEIGHT, bounds.minY()),
                Math.max(around.getZ() - REACH, bounds.minZ()));
        BlockPos to = new BlockPos(Math.min(around.getX() + REACH, bounds.maxX()), Math.min(around.getY() + HEIGHT, bounds.maxY()),
                Math.min(around.getZ() + REACH, bounds.maxZ()));
        return from.getX() > to.getX() || from.getY() > to.getY() || from.getZ() > to.getZ() ? List.of() : scan(level, from, to, pos -> true);
    }

    /** As above, with what counts as the same town given (game tests have no real town). */
    public static List<Spot> spots(ServerLevel level, BlockPos around, java.util.function.Predicate<BlockPos> inTown) {
        return scan(level, around.offset(-REACH, -HEIGHT, -REACH), around.offset(REACH, HEIGHT, REACH), inTown);
    }

    private static List<Spot> scan(ServerLevel level, BlockPos from, BlockPos to, java.util.function.Predicate<BlockPos> inTown) {
        List<Spot> spots = new ArrayList<>();
        for (BlockPos pos : BlockPos.betweenClosed(from, to)) {
            BlockState state = level.getBlockState(pos);
            Spot spot = null;
            if (state.is(BlockTags.BEDS) && state.getValue(BedBlock.PART) == BedPart.HEAD) {
                spot = new Spot(new Vec3(pos.getX() + 0.5, pos.getY() + 0.5625, pos.getZ() + 0.5), "bed");
            } else if (state.is(Blocks.HAY_BLOCK)) {
                spot = new Spot(Vec3.atBottomCenterOf(pos.above()), "hay");
            } else if (state.is(Blocks.WATER_CAULDRON)) {
                spot = new Spot(new Vec3(pos.getX() + 0.5, pos.getY() + 0.25, pos.getZ() + 0.5), "trough");
            }
            if (spot != null && inTown.test(pos) && roomAbove(level, BlockPos.containing(spot.at())) && clear(level, spot.at())) {
                spots.add(spot);
            }
        }
        return spots;
    }

    private static boolean roomAbove(ServerLevel level, BlockPos feet) {
        for (BlockPos pos : List.of(feet.above(), feet.above(2))) {
            if (!level.getBlockState(pos).getCollisionShape(level, pos).isEmpty() || !level.getFluidState(pos).isEmpty()) {
                return false;
            }
        }
        return true;
    }

    private static boolean clear(ServerLevel level, Vec3 at) {
        return level.getEntitiesOfClass(net.minecraft.world.entity.Mob.class, new AABB(at, at).inflate(CLEAR_OF), m -> m instanceof Enemy && m.isAlive()
                && !(m instanceof com.hollowfrontier.content.frontier.outlaw.AbstractOutlaw outlaw && (outlaw.isJailed() || outlaw.isFreed()))).isEmpty();
    }
}
