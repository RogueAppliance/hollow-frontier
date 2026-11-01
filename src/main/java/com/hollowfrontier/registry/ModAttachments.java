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
            () -> AttachmentType.builder(() -> 0).serialize(Codec.INT.fieldOf("tier")).build());

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

    // Present only while the horse wears saddlebags (check hasData before getData, which would create it).
    public static final Supplier<AttachmentType<ItemContainerContents>> SADDLEBAGS = ATTACHMENTS.register("saddlebags",
            () -> AttachmentType.builder(() -> ItemContainerContents.EMPTY).serialize(ItemContainerContents.CODEC.fieldOf("items")).build());

    // Someone held in a Gray pen (20), or a prisoner walked back from one. Present only on them (check hasData); the pens
    // blueprint writes it into the template NBT.
    public static final Supplier<AttachmentType<com.hollowfrontier.content.hollow.gray.Captive>> CAPTIVE = ATTACHMENTS.register("captive",
            () -> AttachmentType.builder(() -> com.hollowfrontier.content.hollow.gray.Captive.HELD)
                    .serialize(com.hollowfrontier.content.hollow.gray.Captive.CODEC.fieldOf("captive")).build());
}
