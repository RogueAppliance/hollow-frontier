package com.hollowfrontier.content.frontier.mystery;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import com.hollowfrontier.content.ufo.Leash;
import com.hollowfrontier.HollowFrontier;
import com.hollowfrontier.content.frontier.raid.TownSite;
import com.hollowfrontier.content.hollow.creature.GrayProbe;
import com.hollowfrontier.content.hollow.gray.GraySentinel;
import com.hollowfrontier.content.hollow.gray.Specimens;
import com.hollowfrontier.content.ufo.GrayWarden;
import com.hollowfrontier.registry.ModAttachments;
import com.hollowfrontier.registry.ModEntities;
import com.hollowfrontier.registry.ModItems;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.levelgen.Heightmap;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.LivingDeathEvent;
import net.neoforged.neoforge.event.tick.LevelTickEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;
import org.jspecify.annotations.Nullable;

/**
 * The men in black dusters' comings and goings (11b), and what makes them mind a player ({@link Suspicion}).
 * <ul>
 * <li><b>Visits:</b> every mystery books one, about half a day on ({@link #VISIT_DELAY}), where it happened. When it's
 *     due and a player is within {@link #VISIT_RANGE}, one or two turn up: in the nearest town's street if there's one
 *     within reach (they lean on the townsfolk and the Gazette's editor), otherwise near the spot. Until the next mystery,
 *     the Gazette prints a correction instead of the report (swamp gas, and so on).</li>
 * <li><b>Hunts:</b> a player marked ({@link Suspicion#HOSTILE}) is hunted at night in the Overworld, by two at a time,
 *     every {@link Suspicion#HUNT_INTERVAL}.</li>
 * <li><b>What they mind:</b> killing a probe, a sentinel, a tagged specimen or one of their own; the rest is raised where
 *     it happens (the skull, the locator, being seen with their things).</li>
 * </ul>
 */
@EventBusSubscriber(modid = HollowFrontier.MODID)
public final class MenInBlack {
    private MenInBlack() {}

    public static final long VISIT_DELAY = 12000;
    public static final double VISIT_RANGE = 128;
    /** How long a visit keeps the Gazette quiet about the mystery it followed. */
    public static final long CORRECTION_WINDOW = 3 * 24000L;

    /** What a find fetches from them, each, in copper coins (a gold coin is 9). */
    public static int price(ItemStack stack) {
        if (stack.is(ModItems.STRANGE_SKULL.get())) {
            return 27;
        }
        if (stack.is(ModItems.SIGNAL_LOCATOR.get())) {
            return 18;
        }
        if (stack.is(ModItems.GRAY_TECH_FRAGMENT.get())) {
            return 6;
        }
        return 1; // flattened wheat: evidence, bought up all the same
    }

    /** Pays {@code copper} coins' worth, in gold where it can. */
    public static void pay(ServerPlayer player, int copper) {
        for (ItemStack coins : List.of(new ItemStack(ModItems.GOLD_COIN.get(), copper / 9), new ItemStack(ModItems.COPPER_COIN.get(), copper % 9))) {
            if (!coins.isEmpty() && !player.getInventory().add(coins)) {
                player.spawnAtLocation(player.level(), coins);
            }
        }
    }

    /** Puts one or two of them on the ground near {@code around} (8-16 blocks out, in loaded chunks); returns who came. */
    public static List<ManInBlack> arrive(ServerLevel level, BlockPos around, int count, @Nullable Player hunted) {
        List<ManInBlack> came = new ArrayList<>();
        RandomSource random = level.getRandom();
        int min = hunted != null ? 20 : 6, spread = hunted != null ? 10 : 8;
        for (int attempt = 0; attempt < 10 && came.size() < count; attempt++) {
            float angle = random.nextFloat() * Mth.TWO_PI;
            int distance = min + random.nextInt(spread);
            BlockPos column = around.offset(Math.round(Mth.cos(angle) * distance), 0, Math.round(Mth.sin(angle) * distance));
            if (!level.isLoaded(column)) {
                continue;
            }
            BlockPos ground = level.getHeightmapPos(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, column);
            ManInBlack man = ModEntities.MAN_IN_BLACK.get().create(level, EntitySpawnReason.EVENT);
            if (man == null || !level.getWorldBorder().isWithinBounds(ground)) {
                continue;
            }
            man.snapTo(ground.getX() + 0.5, ground.getY(), ground.getZ() + 0.5, angle * Mth.RAD_TO_DEG, 0.0F);
            man.finalizeSpawn(level, level.getCurrentDifficultyAt(ground), EntitySpawnReason.EVENT, null);
            if (hunted != null) {
                man.hunt(hunted);
            }
            if (level.addFreshEntity(man)) {
                came.add(man);
            }
        }
        return came;
    }

    /** A booked visit, if it's due and anyone's about: they come, to the nearest town's street or the spot itself. */
    public static List<ManInBlack> visitIfDue(ServerLevel level) {
        if (Leash.isBroken(level)) {
            return List.of(); // 13d: the leash is broken; no one sends them any more
        }
        MysteryLog log = MysteryLog.get(level);
        Optional<MysteryLog.Visit> due = log.dueVisit();
        if (due.isEmpty() || level.getGameTime() < due.get().time()) {
            return List.of();
        }
        BlockPos spot = due.get().pos();
        Player witness = level.getNearestPlayer(spot.getX(), spot.getY(), spot.getZ(), VISIT_RANGE, p -> !p.isSpectator());
        if (witness == null) {
            return List.of();
        }
        BlockPos where = TownSite.townAt(level, witness.blockPosition()).map(start -> start.getBoundingBox().getCenter()).orElse(spot);
        List<ManInBlack> came = arrive(level, where, 1 + level.getRandom().nextInt(2), null);
        if (!came.isEmpty()) {
            log.visited(level.getGameTime(), where);
        }
        return came;
    }

    /** Sends two after a marked player (at night, in the Overworld, when the hunt is due); returns who came. */
    public static List<ManInBlack> huntIfDue(ServerPlayer player) {
        if (Leash.isBroken(player.level())) {
            return List.of();
        }
        Suspicion suspicion = Suspicion.of(player);
        ServerLevel level = player.level();
        long now = level.getGameTime();
        if (!Suspicion.hunted(player) || now < suspicion.nextHunt() || level.dimension() != Level.OVERWORLD || !level.isDarkOutside()) {
            return List.of();
        }
        player.setData(ModAttachments.SUSPICION.get(), new Suspicion(suspicion.level(), suspicion.lastChange(), now + Suspicion.HUNT_INTERVAL));
        List<ManInBlack> came = arrive(level, player.blockPosition(), 2, player);
        if (!came.isEmpty()) {
            player.sendSystemMessage(Component.translatable("message.hollowfrontier.suspicion.hunted").withStyle(ChatFormatting.DARK_GRAY, ChatFormatting.ITALIC));
        }
        return came;
    }

    /** Whether the men in black have been round {@code pos} since the newest mystery there (the Gazette prints a correction). */
    public static boolean corrected(ServerLevel level, MysteryLog.Sign sign) {
        return MysteryLog.get(level).lastVisit().filter(visit -> visit.time() >= sign.time()
                && level.getGameTime() - visit.time() <= CORRECTION_WINDOW
                && Math.hypot(visit.pos().getX() - sign.x(), visit.pos().getZ() - sign.z()) <= Mysteries.NEWS_RANGE).isPresent();
    }

    @SubscribeEvent
    static void onLevelTick(LevelTickEvent.Post event) {
        if (event.getLevel() instanceof ServerLevel level && level.dimension() == Level.OVERWORLD && level.getGameTime() % 200 == 0 && MysteryConfig.visits()) {
            visitIfDue(level);
        }
    }

    @SubscribeEvent
    static void onPlayerTick(PlayerTickEvent.Post event) {
        if (event.getEntity() instanceof ServerPlayer player && player.tickCount % 20 == 0) {
            Suspicion.tick(player);
            if (MysteryConfig.hunts()) {
                huntIfDue(player);
            }
        }
    }

    /** What they mind: the Grays' things and people killed. */
    @SubscribeEvent
    static void onDeath(LivingDeathEvent event) {
        if (!(event.getSource().getEntity() instanceof ServerPlayer player)) {
            return;
        }
        int amount = switch (event.getEntity()) {
            case GraySentinel sentinel -> Suspicion.KILL_SENTINEL;
            case GrayProbe probe -> Suspicion.KILL_PROBE;
            case ManInBlack man -> Suspicion.KILL_MAN_IN_BLACK;
            case GrayWarden warden -> Suspicion.KILL_WARDEN;
            default -> Specimens.isTagged(event.getEntity()) ? Suspicion.KILL_SPECIMEN : 0;
        };
        if (amount > 0) {
            Suspicion.add(player, amount);
        }
    }
}
