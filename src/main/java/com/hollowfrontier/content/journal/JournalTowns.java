package com.hollowfrontier.content.journal;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import com.hollowfrontier.HollowFrontier;
import com.hollowfrontier.content.frontier.bank.BankBox;
import com.hollowfrontier.content.frontier.bank.TownBanks;
import com.hollowfrontier.content.frontier.bounty.BountyData;
import com.hollowfrontier.content.frontier.raid.TownRaids;
import com.hollowfrontier.content.frontier.raid.TownRecord;
import com.hollowfrontier.content.frontier.stake.Drives;
import com.hollowfrontier.content.frontier.stake.Herds;
import com.hollowfrontier.content.frontier.stake.SheriffJob;
import com.hollowfrontier.content.frontier.stake.StakeData;
import com.hollowfrontier.content.frontier.stake.Standing;
import com.hollowfrontier.content.frontier.town.Towns;
import com.hollowfrontier.content.hollow.gateway.PolarCompassItem;
import com.hollowfrontier.network.JournalNotePayload;
import com.hollowfrontier.registry.ModAttachments;

import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.Level;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;
import net.neoforged.neoforge.network.PacketDistributor;

/**
 * The journal's town pages (48c): one for every town the player has walked into. The journal keeps only which towns and
 * the day they first came ({@link JournalData#towns}); everything else on a page is read from the town's own records
 * when the book opens: standing, rank and the star ({@link Standing}), the sheriff's job in hand, the claim and the
 * branded stock on it, the box at the bank, the poster waiting on the board and the last raid ({@link TownRecord}), and
 * a herd out on a drive from there.
 */
@EventBusSubscriber(modid = HollowFrontier.MODID)
public final class JournalTowns {
    private JournalTowns() {}

    private static final int LOOK_EVERY = 40;

    /**
     * A town's page as sent. {@code star}: 0 none, 1 worn, 2 in the sheriff's drawer. {@code job}: the kind in hand, or "".
     * {@code head}: branded stock on the claim, -1 with no claim (and only what's loaded can be counted). {@code bank}:
     * copper's worth in the box, -1 with no box. {@code poster}: who's wanted on the board ("" for nobody) and for how
     * much gold. {@code raidDay}: the day of the last raid, 0 if none is on record. {@code driveHead}: head near the
     * player of a herd out from here, -1 with no drive; and how far and which way it's bound.
     */
    public record Town(String name, long firstDay, int points, String rank, int star, String job, int head, int bank, String poster, int reward,
            long raidDay, int driveHead, int driveBlocks, String driveWay) {
        public static final StreamCodec<ByteBuf, Town> STREAM_CODEC = StreamCodec.of((buf, town) -> {
            ByteBufCodecs.STRING_UTF8.encode(buf, town.name);
            ByteBufCodecs.VAR_LONG.encode(buf, town.firstDay);
            ByteBufCodecs.VAR_INT.encode(buf, town.points);
            ByteBufCodecs.STRING_UTF8.encode(buf, town.rank);
            ByteBufCodecs.VAR_INT.encode(buf, town.star);
            ByteBufCodecs.STRING_UTF8.encode(buf, town.job);
            ByteBufCodecs.VAR_INT.encode(buf, town.head);
            ByteBufCodecs.VAR_INT.encode(buf, town.bank);
            ByteBufCodecs.STRING_UTF8.encode(buf, town.poster);
            ByteBufCodecs.VAR_INT.encode(buf, town.reward);
            ByteBufCodecs.VAR_LONG.encode(buf, town.raidDay);
            ByteBufCodecs.VAR_INT.encode(buf, town.driveHead);
            ByteBufCodecs.VAR_INT.encode(buf, town.driveBlocks);
            ByteBufCodecs.STRING_UTF8.encode(buf, town.driveWay);
        }, buf -> new Town(ByteBufCodecs.STRING_UTF8.decode(buf), ByteBufCodecs.VAR_LONG.decode(buf), ByteBufCodecs.VAR_INT.decode(buf),
                ByteBufCodecs.STRING_UTF8.decode(buf), ByteBufCodecs.VAR_INT.decode(buf), ByteBufCodecs.STRING_UTF8.decode(buf),
                ByteBufCodecs.VAR_INT.decode(buf), ByteBufCodecs.VAR_INT.decode(buf), ByteBufCodecs.STRING_UTF8.decode(buf),
                ByteBufCodecs.VAR_INT.decode(buf), ByteBufCodecs.VAR_LONG.decode(buf), ByteBufCodecs.VAR_INT.decode(buf),
                ByteBufCodecs.VAR_INT.decode(buf), ByteBufCodecs.STRING_UTF8.decode(buf)));
    }

    public static long day(long gameTime) {
        return gameTime / 24000 + 1;
    }

    /** The player has walked into a town: it gets a page, dated today, if it hasn't one. */
    public static boolean enter(ServerLevel level, ServerPlayer player, long town) {
        JournalData data = player.getData(ModAttachments.JOURNAL.get());
        if (data.towns().containsKey(town)) {
            return false;
        }
        player.setData(ModAttachments.JOURNAL.get(), data.town(town, day(level.getGameTime())));
        if (Journal.carried(player)) {
            PacketDistributor.sendToPlayer(player, new JournalNotePayload(Towns.name(level, town)));
        }
        return true;
    }

    /** The towns the player has pages for, in the order they first came to them. */
    public static List<Town> towns(ServerPlayer player) {
        ServerLevel level = player.level().getServer().overworld();
        JournalData data = player.getData(ModAttachments.JOURNAL.get());
        Standing standing = Standing.of(player);
        SheriffJob job = player.hasData(ModAttachments.SHERIFF_JOB.get()) ? player.getData(ModAttachments.SHERIFF_JOB.get()) : null;
        Drives.Drive drive = Drives.of(player);
        TownRaids raids = TownRaids.get(level);
        List<Map.Entry<Long, Long>> known = new ArrayList<>(data.towns().entrySet());
        known.sort(Map.Entry.<Long, Long>comparingByValue().thenComparing(Map.Entry.comparingByKey(Comparator.naturalOrder())));
        List<Town> pages = new ArrayList<>();
        for (Map.Entry<Long, Long> entry : known) {
            long key = entry.getKey();
            Optional<StakeData.Claim> claim = StakeData.get(level).of(player.getUUID(), key);
            BankBox box = TownBanks.get(level).box(key, player.getUUID());
            Optional<TownRecord> record = raids.recordIfKnown(key);
            Optional<BountyData> poster = record.flatMap(r -> r.poster);
            boolean driving = drive != null && drive.from() == key;
            boolean here = driving && player.level().dimension() == Level.OVERWORLD;
            pages.add(new Town(Towns.name(level, key), entry.getValue(), standing.points(key), standing.rank(key).id(),
                    standing.deputyOf(key) ? (standing.suspended() ? 2 : 1) : 0,
                    job != null && job.town() == key ? job.kind().getSerializedName() : "",
                    claim.map(c -> Herds.count(level, c)).orElse(-1),
                    box == null ? -1 : box.worth(),
                    poster.map(BountyData::target).orElse(""), poster.map(BountyData::reward).orElse(0),
                    record.filter(r -> r.lastRaid >= 0).map(r -> day(r.lastRaid)).orElse(0L),
                    driving ? (here ? Drives.herd(level, player, drive).size() : 0) : -1,
                    here ? (int) Math.round(Math.hypot(drive.dest().getX() - player.getX(), drive.dest().getZ() - player.getZ())) : 0,
                    here ? PolarCompassItem.direction(player.blockPosition(), drive.dest()) : ""));
        }
        return pages;
    }

    @SubscribeEvent
    static void onPlayerTick(PlayerTickEvent.Post event) {
        if (event.getEntity() instanceof ServerPlayer player && player.level() instanceof ServerLevel level && !player.isSpectator()
                && (player.tickCount + player.getId()) % LOOK_EVERY == 0 && player.getData(ModAttachments.IN_TOWN.get())) {
            Towns.keyAt(level, player.blockPosition()).ifPresent(town -> enter(level, player, town));
        }
    }
}
