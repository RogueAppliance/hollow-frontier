package com.hollowfrontier.content.frontier.stake;

import java.util.Optional;

import com.hollowfrontier.HollowFrontier;
import com.hollowfrontier.content.frontier.outlaw.AbstractOutlaw;
import com.hollowfrontier.content.frontier.town.Towns;
import com.hollowfrontier.content.frontier.townsfolk.Folk;
import com.hollowfrontier.content.frontier.townsfolk.Lawman;
import com.hollowfrontier.content.frontier.townsfolk.Townsfolk;
import com.hollowfrontier.registry.ModDataComponents;
import com.hollowfrontier.registry.ModEntities;
import com.hollowfrontier.registry.ModItems;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.trading.ItemCost;
import net.minecraft.world.item.trading.MerchantOffer;
import net.minecraft.world.level.Level;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.EntityJoinLevelEvent;
import net.neoforged.neoforge.event.entity.living.LivingDamageEvent;
import net.neoforged.neoforge.event.entity.living.LivingDeathEvent;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;
import net.neoforged.neoforge.event.tick.LevelTickEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;

/** Where phase 47 meets the game: the sheriff and the wrangler asked, jobs and drives ticked, herds made to behave, the mail brought. */
@EventBusSubscriber(modid = HollowFrontier.MODID)
public final class StakeEvents {
    private StakeEvents() {}

    /** How often the mail and the rustlers are looked at. */
    private static final int WORLD_CHECK = 600;

    @SubscribeEvent
    static void onInteract(PlayerInteractEvent.EntityInteract event) {
        if (event.getHand() != InteractionHand.MAIN_HAND || !(event.getLevel() instanceof ServerLevel level) || !(event.getEntity() instanceof ServerPlayer player)) {
            return;
        }
        Entity target = event.getTarget();
        boolean handled = false;
        if (target instanceof Lawman lawman && !lawman.isPosse()) {
            Optional<Long> town = Towns.keyAt(level, lawman.blockPosition());
            handled = town.isPresent() && theLaw(level, lawman, town.get()) && Deputies.atSheriff(level, player, lawman, player.getItemInHand(event.getHand()), town.get());
        } else if (target instanceof Townsfolk folk && folk.getType() == ModEntities.TOWNSFOLK.get() && folk.getFolk() == Folk.WRANGLER
                && player.isSecondaryUseActive() && folk.isAlive()) {
            handled = Drives.atWrangler(level, player, folk);
        }
        if (handled) {
            event.setCancellationResult(InteractionResult.SUCCESS);
            event.setCanceled(true);
        }
    }

    /**
     * Whether this lawman speaks for the town's law: its sheriff does, and where a town has no sheriff (no sheriff's office
     * was built there, or he's dead) any of its lawmen will swear a deputy in and hand out the day's job.
     */
    public static boolean theLaw(ServerLevel level, Lawman lawman, long town) {
        if (lawman.getRank() == Lawman.Rank.SHERIFF) {
            return true;
        }
        return Towns.start(level, town).map(start -> level.getEntitiesOfClass(Lawman.class, net.minecraft.world.phys.AABB.of(start.getBoundingBox()).inflate(16),
                other -> other.isAlive() && other.getRank() == Lawman.Rank.SHERIFF && !other.isPosse()).isEmpty()).orElse(false);
    }

    // A deputy set on in their own town: the town's lawmen take it personally
    @SubscribeEvent
    static void onDamage(LivingDamageEvent.Post event) {
        if (event.getEntity() instanceof ServerPlayer player && event.getSource().getEntity() instanceof LivingEntity attacker
                && !(attacker instanceof Player) && player.level() instanceof ServerLevel level) {
            Optional<Long> badge = Standing.of(player).badge();
            if (badge.isPresent() && Towns.keyAt(level, player.blockPosition()).filter(badge.get()::equals).isPresent() && Deputies.deputy(player, badge.get())) {
                Lawman.callForHelp(level, player, attacker);
            }
        }
    }

    @SubscribeEvent
    static void onDeath(LivingDeathEvent event) {
        if (event.getEntity() instanceof AbstractOutlaw outlaw && outlaw.level() instanceof ServerLevel level && !outlaw.isJailed()) {
            SheriffJobs.quarryDied(level, outlaw);
        }
    }

    @SubscribeEvent
    static void onPlayerTick(PlayerTickEvent.Post event) {
        if (event.getEntity() instanceof ServerPlayer player && player.tickCount % 20 == 0) {
            SheriffJobs.tick(player.level(), player);
            Drives.tick(player.level(), player);
            if (StakeConfig.standing() || StakeConfig.drives()) {
                Towns.keyAt(player.level(), player.blockPosition()).ifPresent(town -> Neighbours.scout(player.level(), town));
            }
        }
    }

    @SubscribeEvent
    static void onLevelTick(LevelTickEvent.Post event) {
        if (event.getLevel() instanceof ServerLevel level && level.dimension() == Level.OVERWORLD && level.getGameTime() % WORLD_CHECK == 0) {
            MailboxBlock.deliver(level);
            Herds.rustlers(level);
        }
    }

    // Herd cattle get their herding back each time they come into the world (a goal isn't saved)
    @SubscribeEvent
    static void onJoin(EntityJoinLevelEvent event) {
        if (!event.getLevel().isClientSide() && event.getEntity() instanceof PathfinderMob mob && Herds.herdOf(mob) != null) {
            mob.goalSelector.addGoal(2, new Herds.DriveGoal(mob));
        }
    }

    /**
     * The deed on a land seller's list (the banker, or the postmaster, who keeps the land office where there's no bank),
     * put right for whoever is about to trade with him: there if the town thinks them a neighbour and they've no place
     * here yet, gone otherwise.
     */
    public static void offerDeed(ServerLevel level, Townsfolk seller, Player player) {
        if (seller.getFolk() != Folk.BANKER && seller.getFolk() != Folk.POSTMASTER) {
            return;
        }
        seller.getOffers().removeIf(offer -> offer.getResult().is(ModItems.DEED.get()));
        Optional<Long> town = Towns.keyAt(level, seller.blockPosition());
        if (town.isEmpty() || !StakeConfig.claims() || !Standing.rank(player, town.get()).atLeast(Standing.Rank.NEIGHBOUR)
                || StakeData.get(level).of(player.getUUID(), town.get()).isPresent()) {
            return;
        }
        ItemStack deed = new ItemStack(ModItems.DEED.get());
        deed.set(ModDataComponents.TOWN.get(), new TownRef(town.get(), Towns.name(level, town.get())));
        seller.getOffers().add(new MerchantOffer(new ItemCost(ModItems.GOLD_COIN.get(), StakeConfig.deedPrice()), Optional.empty(), deed, 1, 1, 0.0F));
    }

    /** A raid has started on a town: anyone with a place there is told, wherever they are. */
    public static void raidStarted(ServerLevel level, long town) {
        for (StakeData.Claim claim : StakeData.get(level).in(town)) {
            ServerPlayer owner = level.getServer().getPlayerList().getPlayer(claim.owner());
            if (owner != null) {
                owner.sendSystemMessage(Component.translatable(StakeText.BELL, Towns.name(level, town)).withStyle(ChatFormatting.DARK_RED));
            }
        }
    }
}
