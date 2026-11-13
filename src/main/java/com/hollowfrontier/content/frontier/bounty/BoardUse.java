package com.hollowfrontier.content.frontier.bounty;

import java.util.UUID;

import org.jspecify.annotations.Nullable;

import com.hollowfrontier.content.frontier.law.Wanted;
import com.hollowfrontier.content.hollow.gray.Captives;
import com.hollowfrontier.registry.ModCriteria;
import com.hollowfrontier.registry.ModDataComponents;
import com.hollowfrontier.registry.ModItems;
import com.hollowfrontier.registry.ModSounds;

import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Prediction;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;

/**
 * What using a board does, for either kind (18):
 * <ul>
 * <li>Empty hand while wanted: pay the fine (see Wanted.payFine) to clear your name, and your poster comes down.</li>
 * <li>Empty hand otherwise: take the poster you're looking at (or, from anywhere else on the board, the first one up for
 *     grabs), one at a time. It's marked TAKEN with your name.</li>
 * <li>Holding an Outlaw Badge: turn it in for its gold-coin reward; your poster is torn up and the board's is stamped DEAD.</li>
 * </ul>
 */
public final class BoardUse {
    private BoardUse() {}

    /** An empty-handed use at {@code spot} on the face (0..1 across, 0..1 down), or nowhere in particular (null). */
    public static void emptyHand(ServerLevel level, ServerPlayer player, PostingBoard board, float @Nullable [] spot, Vec3 at) {
        // A prisoner walked in from a Gray pen (20) comes first
        if (Captives.turnIn(level, player, BlockPos.containing(at), board.key(level)) > 0) {
            board.sync(level);
            return;
        }
        if (Wanted.of(player).heat() > 0) {
            int fine = Wanted.fine(player);
            if (Wanted.payFine(player)) {
                player.sendSystemMessage(Component.translatable("message.hollowfrontier.wanted.paid", fine));
                level.playSound(null, at.x, at.y, at.z, ModSounds.COIN_CLINK.get(), SoundSource.BLOCKS, 1.0F, 1.0F);
                board.sync(level);
            } else {
                player.sendOverlayMessage(Component.translatable("message.hollowfrontier.wanted.fine", fine));
            }
            return;
        }
        board.sync(level);
        // 32d: the Polar Survey's notice, when that's the poster in front of them (on the face as it is now: a new board's
        // is empty until it syncs, and the notice would seem to be in the first cell)
        BoardFace.Slot looked = spot == null ? null : board.face().slotAt(board.columns(), board.rows(), spot[0], spot[1]);
        if (looked != null && looked.kind() == BoardFace.Kind.SURVEY) {
            com.hollowfrontier.content.frontier.survey.SurveyBounty.use(level, player, at);
            return;
        }
        if (player.getInventory().contains(stack -> stack.is(ModItems.WANTED_POSTER.get()))) {
            player.sendOverlayMessage(Component.translatable("message.hollowfrontier.bounty.already_have"));
            return;
        }
        BoardPosting posting = pick(board, spot, player);
        if (posting == null) {
            player.sendOverlayMessage(Component.translatable("message.hollowfrontier.bounty.none_up"));
            return;
        }
        if (posting.status().closed()) {
            String why = posting.status() == BoardPosting.Status.CAPTURED ? "already_captured" : "already_dead";
            player.sendOverlayMessage(Component.translatable("message.hollowfrontier.bounty." + why, posting.bounty().target()));
            return;
        }
        String name = player.getGameProfile().name();
        if (posting.status() == BoardPosting.Status.TAKEN && !posting.takenBy().equals(name)) {
            player.sendOverlayMessage(Component.translatable("message.hollowfrontier.bounty.taken_by", posting.bounty().target(), posting.takenBy()));
            return;
        }
        BountyBoards.get(level).update(board.key(level), posting.taken(name));
        board.sync(level);
        ItemStack poster = new ItemStack(ModItems.WANTED_POSTER.get());
        poster.set(ModDataComponents.BOUNTY.get(), posting.bounty());
        if (posting.raider()) {
            player.sendSystemMessage(Component.translatable("message.hollowfrontier.bounty.raider", posting.bounty().target()));
        }
        give(player, poster);
        com.hollowfrontier.content.frontier.duel.Duels.posterTaken(player, posting.bounty()); // 51
        level.playSound(null, at.x, at.y, at.z, SoundEvents.BOOK_PAGE_TURN, SoundSource.BLOCKS, 1.0F, 1.0F);
    }

    // The poster at the spot if it's an outlaw's or a raider's; otherwise the first one up for grabs (or theirs again)
    private static @Nullable BoardPosting pick(PostingBoard board, float @Nullable [] spot, Player player) {
        BoardFace face = board.face();
        BoardFace.Slot slot = spot == null ? null : face.slotAt(board.columns(), board.rows(), spot[0], spot[1]);
        if (slot != null && slot.kind() != BoardFace.Kind.WANTED && slot.kind() != BoardFace.Kind.SURVEY) {
            return face.shown().get(slot.index());
        }
        String name = player.getGameProfile().name();
        BoardPosting fallback = null;
        for (BoardFace.Slot s : face.slots(board.columns(), board.rows())) {
            if (s.kind() == BoardFace.Kind.WANTED || s.kind() == BoardFace.Kind.SURVEY) {
                continue;
            }
            BoardPosting posting = face.shown().get(s.index());
            if (posting.status() == BoardPosting.Status.OPEN || posting.status() == BoardPosting.Status.TAKEN && posting.takenBy().equals(name)) {
                return posting;
            }
            fallback = fallback == null ? posting : fallback;
        }
        return fallback;
    }

    /** An Outlaw Badge brought to the board: the reward paid, the poster torn up, the board's stamped DEAD. */
    public static void turnIn(ServerLevel level, Player player, ItemStack badge, BountyData bounty, @Nullable PostingBoard board, Vec3 at) {
        removePoster(player.getInventory(), bounty.id());
        badge.shrink(1);
        give(player, new ItemStack(ModItems.GOLD_COIN.get(), bounty.reward()));
        player.sendSystemMessage(Component.translatable("message.hollowfrontier.bounty.paid", bounty.reward(), bounty.target()));
        ModCriteria.fire(player, ModCriteria.BOUNTY_CLAIMED);
        // 47a: the town whose board it is thinks the better of them, and pays its own deputy a coin over
        if (player instanceof ServerPlayer hunter) {
            String key = board != null ? board.key(level) : BountyBoards.keyFor(level, BlockPos.containing(at));
            com.hollowfrontier.content.frontier.stake.Standing.atBoard(hunter, key, com.hollowfrontier.content.frontier.stake.Standing.BOUNTY);
            com.hollowfrontier.content.frontier.stake.Standing.town(key).filter(town -> com.hollowfrontier.content.frontier.stake.Deputies.deputy(hunter, town)).ifPresent(town -> {
                give(hunter, new ItemStack(ModItems.GOLD_COIN.get()));
                hunter.sendSystemMessage(Component.translatable(com.hollowfrontier.content.frontier.stake.StakeText.BOUNTY_BONUS));
            });
        }
        level.playSound(null, at.x, at.y, at.z, SoundEvents.PLAYER_LEVELUP, SoundSource.BLOCKS, 0.8F, 1.2F);
        BountyBoards.get(level).markDead(bounty.id());
        if (board != null) {
            board.sync(level);
        }
    }

    public static void give(Player player, ItemStack stack) {
        if (!player.getInventory().add(stack)) {
            player.drop(stack, false, Prediction.SERVER_ONLY);
        }
    }

    public static void removePoster(Inventory inventory, UUID id) {
        for (int slot = 0; slot < inventory.getContainerSize(); slot++) {
            ItemStack candidate = inventory.getItem(slot);
            BountyData data = candidate.get(ModDataComponents.BOUNTY.get());
            if (candidate.is(ModItems.WANTED_POSTER.get()) && data != null && data.id().equals(id)) {
                inventory.removeItemNoUpdate(slot);
                return;
            }
        }
    }
}
