package com.hollowfrontier.content.ufo;

import java.util.function.Consumer;

import org.jspecify.annotations.Nullable;

import com.hollowfrontier.content.hollow.RelicItem;
import com.hollowfrontier.worldgen.structure.ModChestLoot;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.TooltipDisplay;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.level.storage.loot.parameters.LootContextParamSets;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.common.Tags;

/**
 * The Grays' odds and ends (32c), found in a crash site's cargo pod: a relic the prospector buys like the Hollow's, each
 * with one small trick nobody can explain. The frontier has opinions about them all.
 * <ul>
 * <li><b>Humming Doohickey:</b> use it and it hums, louder the nearer ore lies (within {@link #HUM_RANGE}).</li>
 * <li><b>Self-Warming Rock:</b> carried, it keeps the frost off: powder snow never freezes you.</li>
 * <li><b>Backward Watch:</b> runs backward, but tells you how long until dusk (or dawn).</li>
 * <li><b>Restless Marble:</b> use it and it would rather be up: a hop, and slow falling to come down after it.</li>
 * <li><b>Unopenable Box:</b> doesn't open. One try in {@link #BOX_ODDS} it does, and gives up a Gray Landing's salvage.</li>
 * </ul>
 */
public class GrayGadgetItem extends RelicItem {
    public enum Kind { DOOHICKEY, WARMING_ROCK, BACKWARD_WATCH, RESTLESS_MARBLE, UNOPENABLE_BOX }

    public static final int HUM_RANGE = 6;
    public static final int BOX_ODDS = 20;

    private final Kind kind;

    public GrayGadgetItem(Properties properties, int price, Kind kind) {
        super(properties, price);
        this.kind = kind;
    }

    public Kind kind() {
        return kind;
    }

    @Override
    public InteractionResult use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        int cooldown = switch (kind) {
            case DOOHICKEY -> 40;
            case BACKWARD_WATCH -> 20;
            case RESTLESS_MARBLE -> 60;
            case UNOPENABLE_BOX -> 20;
            case WARMING_ROCK -> 0;
        };
        if (cooldown == 0) {
            return InteractionResult.PASS; // the rock does its work in your pocket
        }
        if (kind == Kind.RESTLESS_MARBLE) {
            hop(player); // on both sides, so the hop isn't fought by the client
        }
        if (level instanceof ServerLevel server) {
            switch (kind) {
                case DOOHICKEY -> hum(server, player);
                case BACKWARD_WATCH -> tellTime(server, player);
                case RESTLESS_MARBLE -> {
                    player.addEffect(new net.minecraft.world.effect.MobEffectInstance(net.minecraft.world.effect.MobEffects.SLOW_FALLING, 50));
                    level.playSound(null, player.blockPosition(), SoundEvents.AMETHYST_BLOCK_CHIME, SoundSource.PLAYERS, 1.0F, 1.8F);
                }
                case UNOPENABLE_BOX -> tryTheBox(server, player, stack);
                default -> {}
            }
            player.getCooldowns().addCooldown(stack, cooldown);
        }
        return InteractionResult.SUCCESS;
    }

    // The rock: wherever it's carried, the cold can't get a grip
    @Override
    public void inventoryTick(ItemStack stack, ServerLevel level, Entity owner, @Nullable EquipmentSlot slot) {
        if (kind == Kind.WARMING_ROCK && owner.getTicksFrozen() > 0) {
            owner.setTicksFrozen(0);
        }
    }

    /** How near the nearest ore lies (blocks), or -1 with none within {@link #HUM_RANGE}. */
    public static int nearestOre(Level level, BlockPos from) {
        double best = Double.MAX_VALUE;
        for (BlockPos pos : BlockPos.betweenClosed(from.offset(-HUM_RANGE, -HUM_RANGE, -HUM_RANGE), from.offset(HUM_RANGE, HUM_RANGE, HUM_RANGE))) {
            if (level.getBlockState(pos).is(Tags.Blocks.ORES)) {
                best = Math.min(best, pos.distSqr(from));
            }
        }
        return best == Double.MAX_VALUE ? -1 : (int) Math.round(Math.sqrt(best));
    }

    private static void hum(ServerLevel level, Player player) {
        int distance = nearestOre(level, player.blockPosition());
        String how = distance < 0 ? "quiet" : distance <= 2 ? "loud" : distance <= 4 ? "sure" : "faint";
        player.sendOverlayMessage(Component.translatable("message.hollowfrontier.humming_doohickey." + how));
        if (distance >= 0) {
            level.playSound(null, player.blockPosition(), SoundEvents.NOTE_BLOCK_CHIME.value(), SoundSource.PLAYERS,
                    0.4F + 0.1F * (HUM_RANGE - distance), 0.6F + 0.2F * (HUM_RANGE - distance));
        }
    }

    private static void tellTime(ServerLevel level, Player player) {
        long t = Math.floorMod(level.getOverworldClockTime(), 24000L);
        boolean day = t < 13000;
        long until = day ? 13000 - t : 24000 - t; // to dusk by day, to dawn (time 0) by night
        int minutes = Math.max(1, (int) Math.round(until / 1200.0));
        player.sendOverlayMessage(Component.translatable("message.hollowfrontier.backward_watch." + (day ? "dusk" : "dawn"), minutes));
        level.playSound(null, player.blockPosition(), SoundEvents.UI_BUTTON_CLICK.value(), SoundSource.PLAYERS, 0.3F, 0.6F);
    }

    private static void hop(Player player) {
        player.push(0.0, 0.75, 0.0);
        player.resetFallDistance();
        player.needsSync = true;
    }

    private static void tryTheBox(ServerLevel level, Player player, ItemStack box) {
        if (level.getRandom().nextInt(BOX_ODDS) != 0) {
            player.sendOverlayMessage(Component.translatable("message.hollowfrontier.unopenable_box.shut"));
            level.playSound(null, player.blockPosition(), SoundEvents.CHAIN_HIT, SoundSource.PLAYERS, 0.6F, 1.6F);
            return;
        }
        openTheBox(level, player, box);
    }

    /** It opens after all: the box is used up and gives up what a Gray Landing's cache would. */
    public static void openTheBox(ServerLevel level, Player player, ItemStack box) {
        box.consume(1, player);
        LootParams params = new LootParams.Builder(level)
                .withParameter(LootContextParams.ORIGIN, Vec3.atCenterOf(player.blockPosition()))
                .withParameter(LootContextParams.THIS_ENTITY, player)
                .create(LootContextParamSets.CHEST);
        for (ItemStack stack : level.getServer().reloadableRegistries().getLootTable(ModChestLoot.GRAY_LANDING).getRandomItems(params)) {
            if (!player.addItem(stack)) {
                Block.popResource(level, player.blockPosition(), stack);
            }
        }
        player.sendOverlayMessage(Component.translatable("message.hollowfrontier.unopenable_box.open"));
        level.playSound(null, player.blockPosition(), SoundEvents.IRON_TRAPDOOR_OPEN, SoundSource.PLAYERS, 1.0F, 1.2F);
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, TooltipDisplay display, Consumer<Component> builder, TooltipFlag flag) {
        builder.accept(Component.translatable(getDescriptionId() + ".use").withStyle(ChatFormatting.DARK_AQUA));
        super.appendHoverText(stack, context, display, builder, flag);
    }
}
