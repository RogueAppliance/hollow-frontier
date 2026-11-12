package com.hollowfrontier.registry;

import java.util.function.Supplier;

import com.hollowfrontier.HollowFrontier;
import com.hollowfrontier.content.frontier.cat.TownCat;
import com.hollowfrontier.content.frontier.law.Wanted;
import com.hollowfrontier.content.frontier.mystery.Suspicion;
import com.hollowfrontier.content.frontier.raid.RaidMember;
import com.hollowfrontier.content.concavia.KindledStanding;
import com.mojang.serialization.Codec;

import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.world.item.component.ItemContainerContents;
import net.neoforged.neoforge.attachment.AttachmentType;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.NeoForgeRegistries;

public final class ModAttachments {
    private ModAttachments() {}

    public static final DeferredRegister<AttachmentType<?>> ATTACHMENTS =
            DeferredRegister.create(NeoForgeRegistries.Keys.ATTACHMENT_TYPES, HollowFrontier.MODID);

    // When a dust devil last took the player's hat (29), for the townsfolk to remark on. Not saved.
    public static final Supplier<AttachmentType<Long>> HAT_LOST = ATTACHMENTS.register("hat_lost",
            () -> AttachmentType.builder(() -> -1L).build());

    // Horse gear. Horseshoe tier: 0 = none, otherwise HorseshoeItem.tier().
    public static final Supplier<AttachmentType<Integer>> HORSESHOE_TIER = ATTACHMENTS.register("horseshoe_tier",
            () -> AttachmentType.builder(() -> 0).serialize(Codec.INT.fieldOf("tier")).sync(ByteBufCodecs.VAR_INT).build()); // synced since 46b: shod hooves ring

    // A player's standing with the law. Reset on death; the heat is synced to its player for the HUD.
    public static final Supplier<AttachmentType<Wanted>> WANTED = ATTACHMENTS.register("wanted",
            () -> AttachmentType.builder(() -> Wanted.NONE).serialize(Wanted.CODEC.fieldOf("wanted")).sync(Wanted.STREAM_CODEC).build());

    // How much the men in black mind the player (11b); they don't forget a face, so it's kept through death
    public static final Supplier<AttachmentType<Suspicion>> SUSPICION = ATTACHMENTS.register("suspicion",
            () -> AttachmentType.builder(() -> Suspicion.NONE).serialize(Suspicion.CODEC.fieldOf("suspicion")).copyOnDeath().build());

    // How the Kindled think of the player (12a): favour won by trade, lost by harm; kept through death, like suspicion
    public static final Supplier<AttachmentType<KindledStanding>> KINDLED_STANDING = ATTACHMENTS.register("kindled_standing",
            () -> AttachmentType.builder(() -> KindledStanding.NONE).serialize(KindledStanding.CODEC.fieldOf("standing")).copyOnDeath().build());

    // Whether the player is inside a frontier town (TownPresence, server side); synced so the client plays town sounds.
    public static final Supplier<AttachmentType<Boolean>> IN_TOWN = ATTACHMENTS.register("in_town",
            () -> AttachmentType.builder(() -> false).sync(ByteBufCodecs.BOOL).build());

    // A cat's frontier data: town role, what it wears, ghostliness (TownCat). Town blueprints write it into the template NBT.
    public static final Supplier<AttachmentType<TownCat>> TOWN_CAT = ATTACHMENTS.register("town_cat",
            () -> AttachmentType.builder(() -> TownCat.STRAY).serialize(TownCat.MAP_CODEC).sync(TownCat.STREAM_CODEC).build());

    // A chest a raid gang goes for (set by the town blueprint): 2 = bank vault, 1 = the sheriff's strongbox. 0 = any other chest.
    public static final Supplier<AttachmentType<Integer>> TOWN_VAULT = ATTACHMENTS.register("town_vault",
            () -> AttachmentType.builder(() -> 0).serialize(Codec.INT.fieldOf("kind"), kind -> kind != 0).build());

    // A raid gang member (outlaw or horse): which raid it rides with and its role. Present only on raiders (check hasData).
    public static final Supplier<AttachmentType<RaidMember>> RAID_MEMBER = ATTACHMENTS.register("raid_member",
            () -> AttachmentType.builder(() -> RaidMember.NONE).serialize(RaidMember.CODEC.fieldOf("member")).build());

    // Whether a raid is on where the player stands (0 none, 1 the warning, 2 the fight); synced so town ambience goes quiet.
    public static final Supplier<AttachmentType<Integer>> RAID_ALERT = ATTACHMENTS.register("raid_alert",
            () -> AttachmentType.builder(() -> 0).sync(ByteBufCodecs.VAR_INT).build());

    // Whether the player has been to the Hollow Earth: the title card shows on the first arrival only. Kept through death.
    public static final Supplier<AttachmentType<Boolean>> VISITED_HOLLOW_EARTH = ATTACHMENTS.register("visited_hollow_earth",
            () -> AttachmentType.builder(() -> false).serialize(Codec.BOOL.fieldOf("visited"), visited -> visited).copyOnDeath().build());

    // 32d: the Polar Survey's letter: whether this player has been handed it, and whether the board has paid them for news of the Survey
    public static final Supplier<AttachmentType<Boolean>> SURVEY_LETTER = ATTACHMENTS.register("survey_letter",
            () -> AttachmentType.builder(() -> false).serialize(Codec.BOOL.fieldOf("handed"), handed -> handed).copyOnDeath().build());
    public static final Supplier<AttachmentType<Boolean>> SURVEY_PAID = ATTACHMENTS.register("survey_paid",
            () -> AttachmentType.builder(() -> false).serialize(Codec.BOOL.fieldOf("paid"), paid -> paid).copyOnDeath().build());

    // 35: the year a postmaster last handed the player a Christmas Parcel, and the year a sheriff last let a fine go. Kept through death
    public static final Supplier<AttachmentType<Integer>> PARCEL_YEAR = ATTACHMENTS.register("parcel_year",
            () -> AttachmentType.builder(() -> 0).serialize(Codec.INT.fieldOf("year"), year -> year != 0).copyOnDeath().build());
    public static final Supplier<AttachmentType<Integer>> FINE_WAIVED_YEAR = ATTACHMENTS.register("fine_waived_year",
            () -> AttachmentType.builder(() -> 0).serialize(Codec.INT.fieldOf("year"), year -> year != 0).copyOnDeath().build());

    // 47a: what each town thinks of the player, and whose star they wear. Kept through death (the law's heat isn't)
    public static final Supplier<AttachmentType<com.hollowfrontier.content.frontier.stake.Standing>> STANDING = ATTACHMENTS.register("standing",
            () -> AttachmentType.builder(() -> com.hollowfrontier.content.frontier.stake.Standing.NONE).serialize(com.hollowfrontier.content.frontier.stake.Standing.CODEC.fieldOf("standing")).copyOnDeath().build());
    // 47a: the sheriff's job in hand (present only while there is one: check hasData), and the game day the last one ended
    public static final Supplier<AttachmentType<com.hollowfrontier.content.frontier.stake.SheriffJob>> SHERIFF_JOB = ATTACHMENTS.register("sheriff_job",
            () -> AttachmentType.builder(() -> com.hollowfrontier.content.frontier.stake.SheriffJob.NONE).serialize(com.hollowfrontier.content.frontier.stake.SheriffJob.CODEC.fieldOf("job")).copyOnDeath().build());
    public static final Supplier<AttachmentType<Long>> LAST_JOB_DAY = ATTACHMENTS.register("last_job_day",
            () -> AttachmentType.builder(() -> -1L).serialize(Codec.LONG.fieldOf("day"), day -> day >= 0).copyOnDeath().build());
    // 47a: on a drifter, the deputy sent to bring him in (present only on one: check hasData)
    public static final Supplier<AttachmentType<java.util.UUID>> QUARRY = ATTACHMENTS.register("quarry",
            () -> AttachmentType.builder(() -> new java.util.UUID(0, 0)).serialize(net.minecraft.core.UUIDUtil.CODEC.fieldOf("deputy")).build());
    // 47b, 47c: a cow's brand and its herd mark (each present only on a cow that has one), and a herd cow's fright (not saved)
    public static final Supplier<AttachmentType<com.hollowfrontier.content.frontier.stake.Herds.Brand>> BRAND = ATTACHMENTS.register("brand",
            () -> AttachmentType.builder(() -> com.hollowfrontier.content.frontier.stake.Herds.Brand.NONE).serialize(com.hollowfrontier.content.frontier.stake.Herds.Brand.CODEC.fieldOf("brand")).build());
    public static final Supplier<AttachmentType<com.hollowfrontier.content.frontier.stake.Herds.Herd>> HERD = ATTACHMENTS.register("herd",
            () -> AttachmentType.builder(() -> com.hollowfrontier.content.frontier.stake.Herds.Herd.NONE).serialize(com.hollowfrontier.content.frontier.stake.Herds.Herd.CODEC.fieldOf("herd")).build());
    public static final Supplier<AttachmentType<com.hollowfrontier.content.frontier.stake.Herds.Stampede>> STAMPEDE = ATTACHMENTS.register("stampede",
            () -> AttachmentType.builder(() -> com.hollowfrontier.content.frontier.stake.Herds.Stampede.NONE).build());
    // 47c: the drive a player has out (present only while there is one). Kept through death: the herd is still out there
    public static final Supplier<AttachmentType<com.hollowfrontier.content.frontier.stake.Drives.Drive>> DRIVE = ATTACHMENTS.register("drive",
            () -> AttachmentType.builder(() -> com.hollowfrontier.content.frontier.stake.Drives.Drive.NONE).serialize(com.hollowfrontier.content.frontier.stake.Drives.Drive.CODEC.fieldOf("drive")).copyOnDeath().build());

    // 48a: the journal's pinned thread, and whether one was handed out for starting in town. Kept through death
    public static final Supplier<AttachmentType<com.hollowfrontier.content.journal.JournalData>> JOURNAL = ATTACHMENTS.register("journal",
            () -> AttachmentType.builder(() -> com.hollowfrontier.content.journal.JournalData.NONE).serialize(com.hollowfrontier.content.journal.JournalData.CODEC.fieldOf("journal")).copyOnDeath().build());

    // 50: what being taken has done to a player (how often, whether they're aboard, where from, and the kit held for them).
    // Kept through death and through leaving the game; and the ship's state, synced so the client knows what to play
    public static final Supplier<AttachmentType<com.hollowfrontier.content.ufo.taken.TakenData>> TAKEN = ATTACHMENTS.register("taken",
            () -> AttachmentType.builder(() -> com.hollowfrontier.content.ufo.taken.TakenData.NONE).serialize(com.hollowfrontier.content.ufo.taken.TakenData.CODEC.fieldOf("taken")).copyOnDeath().build());
    public static final Supplier<AttachmentType<Integer>> ABOARD = ATTACHMENTS.register("aboard",
            () -> AttachmentType.builder(() -> 0).sync(ByteBufCodecs.VAR_INT).build());

    // Present only while the horse wears saddlebags (check hasData before getData, which would create it).
    public static final Supplier<AttachmentType<ItemContainerContents>> SADDLEBAGS = ATTACHMENTS.register("saddlebags",
            () -> AttachmentType.builder(() -> ItemContainerContents.EMPTY).serialize(ItemContainerContents.CODEC.fieldOf("items")).build());

    // Someone held in a Gray pen (20), or a prisoner walked back from one. Present only on them (check hasData); the pens
    // blueprint writes it into the template NBT.
    public static final Supplier<AttachmentType<com.hollowfrontier.content.hollow.gray.Captive>> CAPTIVE = ATTACHMENTS.register("captive",
            () -> AttachmentType.builder(() -> com.hollowfrontier.content.hollow.gray.Captive.HELD)
                    .serialize(com.hollowfrontier.content.hollow.gray.Captive.CODEC.fieldOf("captive")).build());
}
