package com.hollowfrontier.content.frontier.survey;

import com.hollowfrontier.content.concavia.ConcavianCodexItem;
import com.hollowfrontier.content.frontier.bounty.BoardUse;
import com.hollowfrontier.registry.ModAttachments;
import com.hollowfrontier.registry.ModItems;
import com.hollowfrontier.registry.ModSounds;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;

/**
 * "Wanted: news of the Polar Survey" (32d), a standing notice on every bounty board with room for it (BoardFace), the
 * same for everyone. Each player is paid {@link #REWARD} gold coins for it once, when they come back to a board having
 * opened a Polar Gateway; before then it points them to the post office, and hands them the Survey's letter if they
 * somehow never had it.
 */
public final class SurveyBounty {
    private SurveyBounty() {}

    public static final int REWARD = 8;

    /** The notice used by an empty hand. */
    public static void use(ServerLevel level, ServerPlayer player, Vec3 at) {
        if (player.getData(ModAttachments.SURVEY_PAID.get())) {
            player.sendOverlayMessage(Component.translatable("message.hollowfrontier.survey.paid_already").withStyle(ChatFormatting.GRAY));
            return;
        }
        if (ConcavianCodexItem.reached(player, "steam/open_sesame")) {
            player.setData(ModAttachments.SURVEY_PAID.get(), true);
            BoardUse.give(player, new ItemStack(ModItems.GOLD_COIN.get(), REWARD));
            player.sendSystemMessage(Component.translatable("message.hollowfrontier.survey.paid", REWARD));
            level.playSound(null, at.x, at.y, at.z, ModSounds.COIN_CLINK.get(), SoundSource.BLOCKS, 1.0F, 1.0F);
            return;
        }
        if (SurveyLetterItem.handOver(level, player, player.blockPosition())) {
            player.sendSystemMessage(Component.translatable("message.hollowfrontier.survey.letter_pinned"));
            return;
        }
        player.sendOverlayMessage(Component.translatable("message.hollowfrontier.survey.notice"));
    }
}
