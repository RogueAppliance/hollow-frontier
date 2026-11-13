package com.hollowfrontier.content.frontier.stake;

import java.util.Optional;

import com.hollowfrontier.content.frontier.bounty.BoardUse;
import com.hollowfrontier.content.frontier.law.Wanted;
import com.hollowfrontier.content.frontier.season.Season;
import com.hollowfrontier.content.frontier.town.Towns;
import com.hollowfrontier.content.frontier.townsfolk.Lawman;
import com.hollowfrontier.registry.ModAttachments;
import com.hollowfrontier.registry.ModCriteria;
import com.hollowfrontier.registry.ModDataComponents;
import com.hollowfrontier.registry.ModItems;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

/**
 * The Deputy's Badge (47a). A town's sheriff swears in a {@link Standing.Rank#NEIGHBOUR}: one town's star at a time. A
 * deputy carrying the star gets the sheriff's jobs ({@link SheriffJobs}), a gold coin on top of any bounty brought to
 * that town's board, and the town's lawmen at their back when something goes for them. Two stars of heat anywhere and the
 * star is taken back; the sheriff returns it once the name is clear.
 */
public final class Deputies {
    private Deputies() {}

    /** A player using the sheriff of {@code town} with {@code held} in hand. Whether it was the badge's business. */
    public static boolean atSheriff(ServerLevel level, ServerPlayer player, Lawman sheriff, ItemStack held, long town) {
        if (!StakeConfig.standing()) {
            return false;
        }
        if (held.is(ModItems.WARRANT.get())) {
            return SheriffJobs.serve(level, player, sheriff, held, town);
        }
        Standing standing = Standing.of(player);
        if (Wanted.of(player).heat() > 0) {
            return waive(player, sheriff, standing, town);
        }
        if (standing.badge().isPresent() && standing.badge().get() == town) {
            if (standing.suspended()) {
                player.setData(ModAttachments.STANDING.get(), standing.withBadge(standing.badge(), false));
                BoardUse.give(player, badge(level, town));
                StakeText.say(player, sheriff, StakeText.REINSTATED);
            } else if (!carries(player, town)) {
                BoardUse.give(player, badge(level, town));
                StakeText.say(player, sheriff, StakeText.NEW_STAR);
            } else {
                SheriffJobs.atSheriff(level, player, sheriff, town);
            }
            return true;
        }
        if (!standing.rank(town).atLeast(Standing.Rank.NEIGHBOUR)) {
            return false;
        }
        if (standing.badge().isPresent() && !player.isSecondaryUseActive()) {
            StakeText.say(player, sheriff, StakeText.OTHER_STAR, Towns.name(level, standing.badge().get()));
            return true;
        }
        swearIn(level, player, town);
        StakeText.say(player, sheriff, StakeText.SWORN_IN, Towns.name(level, town));
        return true;
    }

    /** Makes the player a deputy of {@code town}: any other town's star is handed in, and any job from it dropped. */
    public static void swearIn(ServerLevel level, ServerPlayer player, long town) {
        removeBadges(player.getInventory());
        player.removeData(ModAttachments.SHERIFF_JOB.get());
        player.setData(ModAttachments.STANDING.get(), Standing.of(player).withBadge(Optional.of(town), false));
        BoardUse.give(player, badge(level, town));
        level.playSound(null, player.blockPosition(), SoundEvents.ARMOR_EQUIP_GOLD.value(), SoundSource.PLAYERS, 1.0F, 1.0F);
        ModCriteria.fire(player, ModCriteria.DEPUTISED);
    }

    // A sheriff lets a one-star fine go for someone the town knows, once a year (the Christmas truce's waiver, 35d, all year)
    private static boolean waive(ServerPlayer player, Lawman sheriff, Standing standing, long town) {
        int year = Season.year();
        if (Wanted.stars(player) != 1 || !standing.rank(town).atLeast(Standing.Rank.REGULAR)
                || player.getData(ModAttachments.FINE_WAIVED_YEAR.get()) == year) {
            return false;
        }
        player.setData(ModAttachments.FINE_WAIVED_YEAR.get(), year);
        Wanted.clear(player);
        StakeText.say(player, sheriff, StakeText.WAIVED);
        return true;
    }

    public static ItemStack badge(ServerLevel level, long town) {
        ItemStack badge = new ItemStack(ModItems.DEPUTY_BADGE.get());
        badge.set(ModDataComponents.TOWN.get(), new TownRef(town, Towns.name(level, town)));
        return badge;
    }

    /** Whether the player has {@code town}'s star on them. */
    public static boolean carries(Player player, long town) {
        for (ItemStack stack : player.getInventory()) {
            TownRef ref = stack.get(ModDataComponents.TOWN.get());
            if (stack.is(ModItems.DEPUTY_BADGE.get()) && ref != null && ref.key() == town) {
                return true;
            }
        }
        return false;
    }

    /** A deputy of {@code town} in good standing, with the star on them. */
    public static boolean deputy(Player player, long town) {
        return Standing.of(player).deputyOf(town) && carries(player, town);
    }

    private static void removeBadges(Inventory inventory) {
        for (int slot = 0; slot < inventory.getContainerSize(); slot++) {
            if (inventory.getItem(slot).is(ModItems.DEPUTY_BADGE.get())) {
                inventory.setItem(slot, ItemStack.EMPTY);
            }
        }
    }

    /** Two stars: the star comes off, and the job in hand goes with it. Called as the player's heat crosses the line. */
    public static void disgrace(ServerPlayer player) {
        Standing standing = Standing.of(player);
        if (standing.badge().isEmpty() || standing.suspended()) {
            return;
        }
        player.setData(ModAttachments.STANDING.get(), standing.withBadge(standing.badge(), true));
        removeBadges(player.getInventory());
        player.removeData(ModAttachments.SHERIFF_JOB.get());
        player.sendSystemMessage(Component.translatable(StakeText.STAR_TAKEN, Towns.name(player.level(), standing.badge().get())).withStyle(ChatFormatting.RED));
    }
}
