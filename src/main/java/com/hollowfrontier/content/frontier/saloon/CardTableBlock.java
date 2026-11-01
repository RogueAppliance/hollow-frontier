package com.hollowfrontier.content.frontier.saloon;

import java.util.Comparator;
import java.util.Optional;

import com.hollowfrontier.content.frontier.law.Wanted;
import com.hollowfrontier.registry.ModMobEffects;

import net.minecraft.network.chat.Component;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

/**
 * A saloon card table (17): green felt on a pedestal. Blackjack against the house, and the house is the saloon's barmaid,
 * so a table deals only with one within {@link #DEALER_RANGE}. Josie, the card sharp, runs a harder table: the dealer hits
 * a soft 17 and the limit is doubled. A player Tipsy II or worse may bet double too. Like townsfolk, no barmaid deals to
 * anyone with two stars on their head. Shape matches the datagen model (ModModelProvider.CARD_TABLE).
 */
public class CardTableBlock extends Block {
    public static final double DEALER_RANGE = 8.0;
    public static final int MAX_BET = 18, SHARP_MAX_BET = 36;
    private static final VoxelShape SHAPE = Shapes.or(
            Block.box(0, 13, 0, 16, 16, 16),
            Block.box(6, 2, 6, 10, 13, 10),
            Block.box(3, 0, 3, 13, 2, 13));

    public CardTableBlock(Properties properties) {
        super(properties);
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return SHAPE;
    }

    /** The nearest barmaid in reach of the table, who deals. */
    public static Optional<Barmaid> dealer(Level level, BlockPos pos) {
        return level.getEntitiesOfClass(Barmaid.class, new AABB(pos).inflate(DEALER_RANGE), Barmaid::isAlive).stream()
                .min(Comparator.comparingDouble(b -> b.distanceToSqr(Vec3.atCenterOf(pos))));
    }

    /** Her house rules, and the player's limit at them. */
    public static Blackjack.Rules rules(Barmaid dealer, Player player) {
        boolean sharp = dealer.getHostess() == Hostess.JOSIE;
        MobEffectInstance tipsy = player.getEffect(ModMobEffects.TIPSY);
        int limit = (sharp ? SHARP_MAX_BET : MAX_BET) * (tipsy != null && tipsy.getAmplifier() >= 1 ? 2 : 1);
        return new Blackjack.Rules(sharp, limit);
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        if (!(level instanceof ServerLevel server)) {
            return InteractionResult.SUCCESS;
        }
        Optional<Barmaid> dealer = dealer(level, pos);
        if (dealer.isEmpty()) {
            player.sendOverlayMessage(Component.translatable("message.hollowfrontier.card_table.no_dealer"));
            return InteractionResult.CONSUME;
        }
        if (Wanted.stars(player) >= 2) {
            player.sendOverlayMessage(Component.translatable("message.hollowfrontier.townsfolk.refuse"));
            return InteractionResult.CONSUME;
        }
        Blackjack.Rules rules = rules(dealer.get(), player);
        player.openMenu(new SimpleMenuProvider((id, inventory, p) -> new BlackjackMenu(id, inventory, ContainerLevelAccess.create(level, pos),
                rules, () -> Blackjack.shuffled(server.getRandom())),
                Component.translatable("container.hollowfrontier.blackjack", dealer.get().getDisplayName())));
        return InteractionResult.CONSUME;
    }
}
