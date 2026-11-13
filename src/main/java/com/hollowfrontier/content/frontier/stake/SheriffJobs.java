package com.hollowfrontier.content.frontier.stake;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.jspecify.annotations.Nullable;

import com.hollowfrontier.content.frontier.bounty.BoardUse;
import com.hollowfrontier.content.frontier.bounty.BountyBoards;
import com.hollowfrontier.content.frontier.bounty.BountyData;
import com.hollowfrontier.content.frontier.bounty.OutlawNames;
import com.hollowfrontier.content.frontier.bounty.Purse;
import com.hollowfrontier.content.frontier.outlaw.AbstractOutlaw;
import com.hollowfrontier.content.frontier.raid.TownSite;
import com.hollowfrontier.content.frontier.town.Towns;
import com.hollowfrontier.content.frontier.townsfolk.Lawman;
import com.hollowfrontier.content.hollow.gray.Captive;
import com.hollowfrontier.registry.ModAttachments;
import com.hollowfrontier.registry.ModCriteria;
import com.hollowfrontier.registry.ModDataComponents;
import com.hollowfrontier.registry.ModEntities;
import com.hollowfrontier.registry.ModItems;
import com.hollowfrontier.registry.ModSounds;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.levelgen.Heightmap;

/**
 * The sheriff's jobs (47a), one a game day and one at a time: after dark, <b>the rounds</b> (walk the town's stops in
 * order, then report; now and then somebody is watching the bank); by day, <b>bring one in</b> (a drifter is out past the
 * edge of town: shoot him down to half, rope him with a lasso and walk him back alive) or <b>a warrant</b> to carry to
 * the next town's sheriff. Each pays coin and {@link Standing#JOB}.
 */
public final class SheriffJobs {
    private SheriffJobs() {}

    public static final int ROUNDS_PAY = 8;
    public static final int BRING_IN_PAY = 6;
    /** What a roped drifter is worth at the board or the sheriff, in gold, on top of the job's pay. */
    public static final int DRIFTER_REWARD = 2;
    public static final int WARRANT_BASE = 6;
    public static final int WARRANT_PER = 100;
    /** How near a stop counts as walked, and how near the drifter's spot he turns up. */
    public static final double STOP_RANGE = 5.0, QUARRY_RANGE = 40.0;
    public static final float SCOUT_CHANCE = 0.35F;
    /** Off only in game tests, where a scout would turn up on some other test's ground. */
    public static boolean scouts = true;
    private static final String[] STOPS = {StakeText.POINT_SQUARE, StakeText.POINT_EDGE, StakeText.POINT_TRAIL, StakeText.POINT_BANK};

    public static @Nullable SheriffJob of(Player player) {
        return player.hasData(ModAttachments.SHERIFF_JOB.get()) ? player.getData(ModAttachments.SHERIFF_JOB.get()) : null;
    }

    private static void set(ServerPlayer player, SheriffJob job) {
        player.setData(ModAttachments.SHERIFF_JOB.get(), job);
    }

    // ---------------------------------------------------------------- at the sheriff

    /** A deputy with their star asks the sheriff: paid for rounds walked, reminded of a job in hand, or given today's. */
    public static void atSheriff(ServerLevel level, ServerPlayer player, Lawman sheriff, long town) {
        // A prisoner at the deputy's heels comes first (played: the sheriff only reminded his deputy of the job, and
        // the man stood there; a sheriff takes anyone else's prisoner through his own board)
        if (com.hollowfrontier.content.hollow.gray.Captives.turnIn(level, player, sheriff.blockPosition(), "town:" + town) > 0) {
            return;
        }
        SheriffJob job = of(player);
        if (job != null && job.walked()) {
            StakeText.say(player, sheriff, StakeText.ROUNDS_DONE);
            complete(player, job, ROUNDS_PAY);
            return;
        }
        if (job != null) {
            StakeText.say(player, sheriff, StakeText.JOB_REMIND);
            remind(player, job);
            return;
        }
        if (player.getData(ModAttachments.LAST_JOB_DAY.get()) == BountyBoards.day(level)) {
            StakeText.say(player, sheriff, StakeText.JOB_NONE);
            return;
        }
        RandomSource random = level.getRandom();
        BlockPos at = sheriff.blockPosition();
        if (level.isDarkOutside()) {
            List<BlockPos> stops = TownSite.find(level, at, random).map(SheriffJobs::stops).orElseGet(() -> List.of(at.north(12), at.east(12), at.south(12)));
            giveRounds(player, town, stops);
            StakeText.say(player, sheriff, StakeText.JOB_ROUNDS, Component.translatable(STOPS[0]));
            remind(player, of(player));
            return;
        }
        Optional<Neighbours.Neighbour> next = random.nextBoolean() ? Neighbours.of(level, town) : Optional.empty();
        if (next.isPresent()) {
            giveWarrant(level, player, town, next.get());
            StakeText.say(player, sheriff, StakeText.JOB_WARRANT, next.get().name(), StakeText.paces(at, next.get().pos()), StakeText.direction(at, next.get().pos()));
            return;
        }
        float angle = random.nextFloat() * Mth.TWO_PI;
        int distance = 70 + random.nextInt(41);
        BlockPos spot = at.offset(Math.round(Mth.cos(angle) * distance), 0, Math.round(Mth.sin(angle) * distance));
        giveBringIn(player, town, spot);
        StakeText.say(player, sheriff, StakeText.JOB_BRING_IN, distance, StakeText.direction(at, spot));
    }

    /** The rounds' stops in a town, in the order {@link #STOPS} names them: the square, the street's end, the trail, and the strongroom if there is one. */
    public static List<BlockPos> stops(TownSite site) {
        List<BlockPos> stops = new ArrayList<>(List.of(site.rally(), site.edge(), site.approach()));
        site.workAt().ifPresent(stops::add);
        return stops;
    }

    public static void giveRounds(ServerPlayer player, long town, List<BlockPos> stops) {
        set(player, new SheriffJob(SheriffJob.Kind.ROUNDS, town, 0, List.copyOf(stops), Optional.empty()));
    }

    public static void giveBringIn(ServerPlayer player, long town, BlockPos spot) {
        set(player, new SheriffJob(SheriffJob.Kind.BRING_IN, town, 0, List.of(spot), Optional.empty()));
    }

    public static void giveWarrant(ServerLevel level, ServerPlayer player, long town, Neighbours.Neighbour to) {
        set(player, new SheriffJob(SheriffJob.Kind.WARRANT, town, 0, List.of(to.pos()), Optional.empty()));
        ItemStack warrant = new ItemStack(ModItems.WARRANT.get());
        warrant.set(ModDataComponents.TOWN.get(), new TownRef(to.key(), to.name()));
        BoardUse.give(player, warrant);
    }

    private static void remind(ServerPlayer player, @Nullable SheriffJob job) {
        if (job == null || job.points().isEmpty()) {
            return;
        }
        BlockPos here = player.blockPosition();
        if (job.kind() == SheriffJob.Kind.ROUNDS && !job.walked()) {
            BlockPos next = job.points().get(job.progress());
            player.sendOverlayMessage(Component.translatable(StakeText.ROUNDS_NEXT, job.progress(), job.points().size(),
                    Component.translatable(STOPS[Math.min(job.progress(), STOPS.length - 1)]), StakeText.paces(here, next), StakeText.direction(here, next)));
        }
    }

    private static void complete(ServerPlayer player, SheriffJob job, int pay) {
        Purse.give(player, pay);
        player.sendSystemMessage(Component.translatable(StakeText.PAID, pay).withStyle(ChatFormatting.GOLD));
        player.level().playSound(null, player.blockPosition(), ModSounds.COIN_CLINK.get(), SoundSource.PLAYERS, 1.0F, 1.0F);
        Standing.add(player, job.town(), Standing.JOB);
        player.setData(ModAttachments.LAST_JOB_DAY.get(), BountyBoards.day(player.level()));
        player.removeData(ModAttachments.SHERIFF_JOB.get());
        ModCriteria.fire(player, ModCriteria.JOB_DONE);
    }

    // ---------------------------------------------------------------- once a second

    /** Moves the job in hand along: a stop walked, the drifter turning up as the deputy nears his spot. */
    public static void tick(ServerLevel level, ServerPlayer player) {
        SheriffJob job = of(player);
        if (job == null || job.points().isEmpty()) {
            return;
        }
        BlockPos here = player.blockPosition();
        if (job.kind() == SheriffJob.Kind.ROUNDS && !job.walked()) {
            BlockPos stop = job.points().get(job.progress());
            if (Math.hypot(stop.getX() - here.getX(), stop.getZ() - here.getZ()) <= STOP_RANGE) {
                SheriffJob next = job.advanced();
                set(player, next);
                level.playSound(null, here, SoundEvents.NOTE_BLOCK_HAT.value(), SoundSource.PLAYERS, 0.6F, 1.2F);
                if (next.walked()) {
                    player.sendOverlayMessage(Component.translatable(StakeText.ROUNDS_REPORT));
                    if (scouts && level.getRandom().nextFloat() < SCOUT_CHANCE && scout(level, player, stop)) {
                        player.sendSystemMessage(Component.translatable(StakeText.ROUNDS_SCOUT).withStyle(ChatFormatting.DARK_RED));
                    }
                } else {
                    remind(player, next);
                }
            } else if (player.tickCount % 100 == 0) {
                remind(player, job);
            }
        } else if (job.kind() == SheriffJob.Kind.BRING_IN && job.quarry().isEmpty()) {
            BlockPos spot = job.points().getFirst();
            if (Math.hypot(spot.getX() - here.getX(), spot.getZ() - here.getZ()) <= QUARRY_RANGE && level.isLoaded(spot)) {
                AbstractOutlaw drifter = drifter(level, player, spot);
                if (drifter != null) {
                    set(player, job.withQuarry(drifter.getUUID()));
                    player.sendOverlayMessage(Component.translatable(StakeText.QUARRY_NEAR, drifter.getDisplayName()));
                }
            }
        }
    }

    /** The drifter a bring-in job is after, put down at {@code spot} and marked as this deputy's. */
    public static @Nullable AbstractOutlaw drifter(ServerLevel level, ServerPlayer player, BlockPos spot) {
        AbstractOutlaw outlaw = spawn(level, level.getHeightmapPos(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, spot));
        if (outlaw != null) {
            outlaw.setCustomName(Component.literal(OutlawNames.random(level.getRandom())));
            outlaw.setData(ModAttachments.QUARRY.get(), player.getUUID());
        }
        return outlaw;
    }

    // Somebody casing the bank: an outlaw a little way off in the dark
    private static boolean scout(ServerLevel level, ServerPlayer player, BlockPos near) {
        float angle = level.getRandom().nextFloat() * Mth.TWO_PI;
        BlockPos column = near.offset(Math.round(Mth.cos(angle) * 16), 0, Math.round(Mth.sin(angle) * 16));
        return level.isLoaded(column) && spawn(level, level.getHeightmapPos(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, column)) != null;
    }

    private static @Nullable AbstractOutlaw spawn(ServerLevel level, BlockPos ground) {
        AbstractOutlaw outlaw = ModEntities.OUTLAW.get().create(level, EntitySpawnReason.EVENT);
        if (outlaw == null || !level.getWorldBorder().isWithinBounds(ground)) {
            return null;
        }
        outlaw.snapTo(ground.getX() + 0.5, ground.getY(), ground.getZ() + 0.5, level.getRandom().nextFloat() * 360.0F, 0.0F);
        outlaw.finalizeSpawn(level, level.getCurrentDifficultyAt(ground), EntitySpawnReason.EVENT, null);
        outlaw.setPersistenceRequired();
        return level.addFreshEntity(outlaw) ? outlaw : null;
    }

    // ---------------------------------------------------------------- the drifter

    /** Whose drifter this is, if it's one. */
    public static Optional<UUID> quarryOf(Entity entity) {
        return entity.hasData(ModAttachments.QUARRY.get()) ? Optional.of(entity.getData(ModAttachments.QUARRY.get())) : Optional.empty();
    }

    /**
     * A lasso thrown by {@code thrower} lands on {@code target}. If it's the thrower's drifter he gives up, once he's hurt
     * enough (half his health or less), and becomes their prisoner; the walk back and the turning in are the captives'
     * (20). Whether the lasso was the job's business (it then drops, as from any miss).
     */
    public static boolean rope(ServerLevel level, LivingEntity thrower, Entity target) {
        if (!(thrower instanceof ServerPlayer player) || !(target instanceof AbstractOutlaw outlaw)
                || quarryOf(outlaw).filter(player.getUUID()::equals).isEmpty() || outlaw.isJailed()) {
            return false;
        }
        if (outlaw.getHealth() > outlaw.getMaxHealth() / 2.0F) {
            player.sendOverlayMessage(Component.translatable(StakeText.QUARRY_FIGHT));
            return true;
        }
        String name = outlaw.getDisplayName().getString();
        BountyData bounty = new BountyData(UUID.randomUUID(), name, outlaw.getBlockX(), outlaw.getBlockZ(), DRIFTER_REWARD, true);
        outlaw.setData(ModAttachments.CAPTIVE.get(), new Captive(false, Optional.of(bounty), Optional.of(player.getUUID())));
        outlaw.setJailed();
        outlaw.setTarget(null);
        outlaw.setItemSlot(EquipmentSlot.MAINHAND, ItemStack.EMPTY);
        level.playSound(null, outlaw.blockPosition(), SoundEvents.CHAIN_PLACE, SoundSource.NEUTRAL, 1.0F, 1.0F);
        player.sendSystemMessage(Component.translatable(StakeText.QUARRY_ROPED, outlaw.getDisplayName()).withStyle(ChatFormatting.GRAY, ChatFormatting.ITALIC));
        SheriffJob job = of(player);
        if (job != null && job.kind() == SheriffJob.Kind.BRING_IN && job.progress() == 0) {
            set(player, job.advanced());
        }
        return true;
    }

    /** A prisoner turned in at a board or a sheriff (Captives.turnIn): if he was this deputy's drifter, the job is done. */
    public static void broughtIn(ServerPlayer player, AbstractOutlaw prisoner) {
        SheriffJob job = of(player);
        if (job != null && job.kind() == SheriffJob.Kind.BRING_IN && quarryOf(prisoner).filter(player.getUUID()::equals).isPresent()) {
            player.sendSystemMessage(Component.translatable(StakeText.BROUGHT_IN).withStyle(ChatFormatting.GOLD));
            complete(player, job, BRING_IN_PAY);
        }
    }

    /** The drifter died: no pay, and the job is over. */
    public static void quarryDied(ServerLevel level, AbstractOutlaw outlaw) {
        quarryOf(outlaw).map(level.getServer().getPlayerList()::getPlayer).ifPresent(player -> {
            SheriffJob job = of(player);
            if (job != null && job.kind() == SheriffJob.Kind.BRING_IN && job.quarry().filter(outlaw.getUUID()::equals).isPresent()) {
                player.removeData(ModAttachments.SHERIFF_JOB.get());
                player.setData(ModAttachments.LAST_JOB_DAY.get(), BountyBoards.day(level));
                player.sendSystemMessage(Component.translatable(StakeText.QUARRY_DEAD).withStyle(ChatFormatting.RED));
            }
        });
    }

    // ---------------------------------------------------------------- the warrant

    /** A warrant handed to the sheriff of {@code town}: served if it's for his town, with pay by how far it came. */
    public static boolean serve(ServerLevel level, ServerPlayer player, Lawman sheriff, ItemStack warrant, long town) {
        TownRef to = warrant.get(ModDataComponents.TOWN.get());
        if (to == null || to.key() != town) {
            StakeText.say(player, sheriff, StakeText.WARRANT_OWN, to == null ? "?" : to.name());
            return true;
        }
        SheriffJob job = of(player);
        boolean theirs = job != null && job.kind() == SheriffJob.Kind.WARRANT;
        int pay = WARRANT_BASE + (theirs ? StakeText.paces(Towns.middle(job.town()), sheriff.blockPosition()) / WARRANT_PER : 0);
        warrant.shrink(1);
        Standing.add(player, town, Standing.WARRANT_SERVED);
        StakeText.say(player, sheriff, StakeText.WARRANT_SERVED, theirs ? Towns.name(level, job.town()) : "?", pay);
        if (theirs) {
            complete(player, job, pay);
        } else {
            Purse.give(player, pay);
        }
        return true;
    }
}
