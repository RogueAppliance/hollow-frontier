package com.hollowfrontier.content.frontier.season;

import java.util.function.Consumer;

import com.hollowfrontier.content.frontier.bounty.BoardUse;
import com.hollowfrontier.content.frontier.townsfolk.Folk;
import com.hollowfrontier.content.frontier.townsfolk.Townsfolk;
import com.hollowfrontier.registry.ModAttachments;
import com.hollowfrontier.registry.ModItems;
import com.hollowfrontier.worldgen.structure.ModChestLoot;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.TooltipDisplay;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.level.storage.loot.parameters.LootContextParamSets;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;

/**
 * A Christmas Parcel (35b): in December the first talk with a postmaster hands one over, one per player per calendar
 * year (the year is kept in an attachment that survives death). A town from before the post offices has no postmaster,
 * so a shopkeeper hands it over there. Use it to open it, like the Gray Cargo Pod: it spills
 * {@link ModChestLoot#CHRISTMAS_PARCEL}, which always holds a Fruitcake.
 */
public class ChristmasParcelItem extends Item {
    /** How far a shopkeeper looks for a postmaster before handing the parcel over himself. */
    public static final double POST_OFFICE_RANGE = 96.0;

    public ChristmasParcelItem(Properties properties) {
        super(properties);
    }

    /** Whether this player is due this year's parcel. */
    public static boolean due(ServerLevel level, ServerPlayer player) {
        return Season.isDecember(level) && player.getData(ModAttachments.PARCEL_YEAR.get()) != Season.year();
    }

    /** Whether this townsperson hands parcels over here: the postmaster, or a shopkeeper in a town without one. */
    public static boolean handsOver(ServerLevel level, Townsfolk folk) {
        if (folk.getFolk() == Folk.POSTMASTER) {
            return true;
        }
        return folk.getFolk() == Folk.SHOPKEEPER && level.getEntitiesOfClass(Townsfolk.class, folk.getBoundingBox().inflate(POST_OFFICE_RANGE),
                other -> other.isAlive() && other.getFolk() == Folk.POSTMASTER).isEmpty();
    }

    /** Hands this year's parcel over, if the player is due it. */
    public static boolean handOver(ServerLevel level, ServerPlayer player) {
        if (!due(level, player)) {
            return false;
        }
        player.setData(ModAttachments.PARCEL_YEAR.get(), Season.year());
        BoardUse.give(player, new ItemStack(ModItems.CHRISTMAS_PARCEL.get()));
        level.playSound(null, player.blockPosition(), SoundEvents.BUNDLE_DROP_CONTENTS, SoundSource.PLAYERS, 1.0F, 0.8F);
        return true;
    }

    @Override
    public InteractionResult use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (level instanceof ServerLevel server) {
            LootParams params = new LootParams.Builder(server)
                    .withParameter(LootContextParams.ORIGIN, player.position())
                    .withParameter(LootContextParams.THIS_ENTITY, player)
                    .withLuck(player.getLuck())
                    .create(LootContextParamSets.CHEST);
            stack.consume(1, player);
            for (ItemStack item : server.getServer().reloadableRegistries().getLootTable(ModChestLoot.CHRISTMAS_PARCEL).getRandomItems(params)) {
                BoardUse.give(player, item);
            }
            level.playSound(null, player.blockPosition(), SoundEvents.BUNDLE_DROP_CONTENTS, SoundSource.PLAYERS, 1.0F, 1.0F);
            player.sendOverlayMessage(Component.translatable("message.hollowfrontier.parcel.opened").withStyle(ChatFormatting.GRAY));
        }
        return InteractionResult.SUCCESS;
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, TooltipDisplay display, Consumer<Component> builder, TooltipFlag flag) {
        builder.accept(Component.translatable("tooltip.hollowfrontier.christmas_parcel").withStyle(ChatFormatting.GRAY));
    }
}
