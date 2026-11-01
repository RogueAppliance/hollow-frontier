package com.hollowfrontier.content.frontier.bank;

import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import org.jspecify.annotations.Nullable;

import com.hollowfrontier.HollowFrontier;
import com.hollowfrontier.content.frontier.bounty.BountyBoards;
import com.hollowfrontier.content.frontier.raid.TownSite;
import com.hollowfrontier.registry.ModCriteria;
import com.hollowfrontier.registry.ModItems;
import com.hollowfrontier.registry.ModSounds;

import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.entity.Entity;
import net.minecraft.util.Prediction;
import net.minecraft.world.inventory.ChestMenu;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.levelgen.structure.StructureStart;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;

/**
 * The bank's counter: sneak-use a town's Banker to open your box there. The first visit costs a gold coin; every
 * visit pays what's owed and the interest since the last one, then tells you any news (a raid on one of your boxes).
 * News also comes on login, or at once to an owner who's online when it happens.
 */
@EventBusSubscriber(modid = HollowFrontier.MODID)
public final class Banking {
    private Banking() {}

    /** Interest: 1 copper a day for each full RATE copper in the box (about 3%)... */
    public static final int RATE = 32;
    /** ...at most this much a day per box... */
    public static final int DAILY_CAP = 8;
    /** ...for at most this many days between visits. */
    public static final int MAX_DAYS = 7;

    /** Sneak-use on a Banker: opens the player's box in the town he works in. */
    public static void visit(ServerLevel level, ServerPlayer player, Entity banker) {
        Optional<StructureStart> town = level.dimension() == Level.OVERWORLD ? TownSite.townAt(level, banker.blockPosition()) : Optional.empty();
        if (town.isEmpty()) {
            say(player, banker, "no_bank");
            return;
        }
        open(level, player, banker, TownSite.keyOf(town.get()));
    }

    /** Opens the player's box in the town (charging for a new one), settling interest and news first. False if refused. */
    public static boolean open(ServerLevel level, ServerPlayer player, @Nullable Entity banker, long town) {
        TownBanks bank = TownBanks.get(level);
        long today = BountyBoards.day(level);
        BankBox box = bank.box(town, player.getUUID());
        if (box == null) {
            if (!player.isCreative() && !takeGold(player)) {
                say(player, banker, "fee");
                return false;
            }
            box = bank.open(town, player.getUUID(), today);
            say(player, banker, "opened");
        }
        tellNews(player, bank);
        settle(player, box, today);
        box.banker = banker;
        BankBox shown = box;
        Component title = Component.translatable("container.hollowfrontier.bank_box");
        player.openMenu(new SimpleMenuProvider((id, inventory, p) -> new ChestMenu(MenuType.GENERIC_9x2, id, inventory, shown, 2), title));
        ModCriteria.fire(player, ModCriteria.BANK_OPENED);
        return true;
    }

    // Coins owed back go in first, then the interest for the days since the last visit. What won't fit goes to the owner.
    static void settle(ServerPlayer player, BankBox box, long today) {
        if (box.owed > 0) {
            int owed = box.owed;
            box.owed = 0;
            pay(player, box.deposit(owed));
            player.sendSystemMessage(Component.translatable("message.hollowfrontier.bank.owed", owed / 9, owed % 9));
        }
        long days = today - box.lastDay;
        box.lastDay = today;
        box.setChanged();
        int interest = interest(box.worth(), days);
        if (interest > 0) {
            pay(player, box.deposit(interest));
            player.level().playSound(null, player.blockPosition(), ModSounds.COIN_CLINK.get(), SoundSource.PLAYERS, 1.0F, 1.0F);
            player.sendSystemMessage(Component.translatable("message.hollowfrontier.bank.interest", interest, Math.min(days, MAX_DAYS)));
            ModCriteria.fire(player, ModCriteria.BANK_INTEREST);
        }
    }

    /** Interest on coins worth copper over days away: 1 c per full RATE c a day, compounding, capped daily and at MAX_DAYS. */
    public static int interest(int worth, long days) {
        int total = 0;
        for (long day = 0; day < Math.min(days, MAX_DAYS); day++) {
            total += Math.min(DAILY_CAP, (worth + total) / RATE);
        }
        return total;
    }

    private static boolean takeGold(ServerPlayer player) {
        var inventory = player.getInventory();
        for (int slot = 0; slot < inventory.getContainerSize(); slot++) {
            if (inventory.getItem(slot).is(ModItems.GOLD_COIN.get())) {
                inventory.getItem(slot).shrink(1);
                return true;
            }
        }
        return false;
    }

    private static void pay(ServerPlayer player, int copper) {
        give(player, new ItemStack(ModItems.GOLD_COIN.get(), copper / 9));
        give(player, new ItemStack(ModItems.COPPER_COIN.get(), copper % 9));
    }

    private static void give(ServerPlayer player, ItemStack stack) {
        if (!stack.isEmpty() && !player.getInventory().add(stack)) {
            player.drop(stack, false, Prediction.SERVER_ONLY);
        }
    }

    private static void say(ServerPlayer player, @Nullable Entity banker, String line) {
        Component text = Component.translatable("message.hollowfrontier.bank." + line);
        player.sendSystemMessage(banker == null ? text : Component.translatable("message.hollowfrontier.npc.say", banker.getDisplayName(), text));
    }

    // ---------------------------------------------------------------- news of raids

    /** Tells each owner what a raid did to their box: now if they're online, otherwise on their next visit or login. */
    public static void notify(MinecraftServer server, Map<UUID, Integer> taken, String gang, String town, int x, int z, boolean returned) {
        TownBanks bank = TownBanks.get(server.overworld());
        taken.forEach((owner, copper) -> {
            BankNotice notice = new BankNotice(gang, x, z, copper, returned, town);
            ServerPlayer player = server.getPlayerList().getPlayer(owner);
            if (player != null) {
                player.sendSystemMessage(notice.message());
            } else {
                bank.post(owner, notice);
            }
        });
    }

    private static void tellNews(ServerPlayer player, TownBanks bank) {
        for (BankNotice notice : bank.collect(player.getUUID())) {
            player.sendSystemMessage(notice.message());
        }
    }

    @SubscribeEvent
    static void onLogin(PlayerEvent.PlayerLoggedInEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            tellNews(player, TownBanks.get((ServerLevel) player.level()));
        }
    }
}
