package com.hollowfrontier.content.frontier.bank;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.jspecify.annotations.Nullable;

import com.hollowfrontier.HollowFrontier;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import net.minecraft.core.UUIDUtil;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.level.saveddata.SavedDataType;

/**
 * Every town's bank, saved with the Overworld: safe deposit boxes keyed by the town's structure key (the same key as
 * TownRaids) and then by owner, plus the news each owner hasn't heard yet (what a raid took from their boxes).
 */
public class TownBanks extends SavedData {
    private static final Codec<Map<UUID, BankBox>> BOXES = Codec.unboundedMap(UUIDUtil.STRING_CODEC, BankBox.CODEC);
    public static final Codec<TownBanks> CODEC = RecordCodecBuilder.create(i -> i.group(
            Codec.unboundedMap(Codec.STRING, BOXES).fieldOf("towns").forGetter(TownBanks::townsByName),
            Codec.unboundedMap(UUIDUtil.STRING_CODEC, BankNotice.CODEC.listOf()).fieldOf("notices").forGetter(b -> b.notices))
            .apply(i, TownBanks::new));
    public static final SavedDataType<TownBanks> TYPE = new SavedDataType<>(HollowFrontier.id("town_banks"), TownBanks::new, CODEC);

    private final Map<Long, Map<UUID, BankBox>> towns = new HashMap<>();
    private final Map<UUID, List<BankNotice>> notices = new HashMap<>();

    public TownBanks() {}

    private TownBanks(Map<String, Map<UUID, BankBox>> towns, Map<UUID, List<BankNotice>> notices) {
        towns.forEach((key, boxes) -> {
            Map<UUID, BankBox> town = new HashMap<>(boxes);
            town.values().forEach(box -> box.bank = this);
            this.towns.put(Long.parseLong(key), town);
        });
        notices.forEach((owner, list) -> this.notices.put(owner, new ArrayList<>(list)));
    }

    private Map<String, Map<UUID, BankBox>> townsByName() {
        Map<String, Map<UUID, BankBox>> out = new HashMap<>();
        towns.forEach((key, boxes) -> out.put(Long.toString(key), boxes));
        return out;
    }

    public static TownBanks get(ServerLevel level) {
        return level.getServer().overworld().getDataStorage().computeIfAbsent(TYPE);
    }

    /** The owner's box in that town, if they've opened one. */
    public @Nullable BankBox box(long town, UUID owner) {
        Map<UUID, BankBox> boxes = towns.get(town);
        return boxes == null ? null : boxes.get(owner);
    }

    /** Opens a new (empty) box for the owner in that town, last visited today. */
    public BankBox open(long town, UUID owner, long today) {
        BankBox box = new BankBox(today);
        box.bank = this;
        towns.computeIfAbsent(town, k -> new HashMap<>()).put(owner, box);
        setDirty();
        return box;
    }

    /** The gang cracked the town's vault: it takes share of the coins in every box there. What each owner lost, in copper. */
    public Map<UUID, Integer> rob(long town, double share) {
        Map<UUID, Integer> taken = new HashMap<>();
        Map<UUID, BankBox> boxes = towns.get(town);
        if (boxes == null || share <= 0) {
            return taken;
        }
        boxes.forEach((owner, box) -> {
            int copper = box.rob(share);
            if (copper > 0) {
                taken.put(owner, copper);
            }
        });
        setDirty();
        return taken;
    }

    /** The bagman is down: every coin goes back to the box it came from (anything that won't fit waits for the owner's next visit). */
    public void refund(long town, Map<UUID, Integer> taken) {
        taken.forEach((owner, copper) -> {
            BankBox box = box(town, owner);
            if (box != null) {
                box.owed += box.deposit(copper);
            }
        });
        setDirty();
    }

    public void post(UUID owner, BankNotice notice) {
        notices.computeIfAbsent(owner, k -> new ArrayList<>()).add(notice);
        setDirty();
    }

    /** Hands over (and forgets) the owner's unheard news. */
    public List<BankNotice> collect(UUID owner) {
        List<BankNotice> news = notices.remove(owner);
        if (news == null) {
            return List.of();
        }
        setDirty();
        return news;
    }
}
