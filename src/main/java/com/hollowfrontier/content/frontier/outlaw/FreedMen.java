package com.hollowfrontier.content.frontier.outlaw;

import org.jspecify.annotations.Nullable;

import com.hollowfrontier.content.frontier.gazette.Aftermath;
import com.hollowfrontier.content.frontier.law.Wanted;
import com.hollowfrontier.content.frontier.raid.TownSite;
import com.hollowfrontier.content.frontier.townsfolk.Folk;
import com.hollowfrontier.content.frontier.townsfolk.Lawman;
import com.hollowfrontier.content.frontier.townsfolk.Townsfolk;
import com.hollowfrontier.registry.ModCriteria;
import com.hollowfrontier.registry.ModEntities;
import com.hollowfrontier.registry.ModItems;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.ConversionParams;
import net.minecraft.world.item.ItemStack;

/**
 * What the frontier makes of a freed man (15c). An outlaw the broken leash let go ({@link AbstractOutlaw#isFreed}) has
 * nowhere to be; talk to him in a town and he'll take a new life there:
 * <ul>
 * <li><b>A deputy's star</b>, for {@link #WAGE} gold coin (use him holding it): he becomes a Deputy of that town, with a
 *     shotgun and a post where he stands, and stands with it in raids like any other lawman. Nobody with two stars or
 *     more can hire him.</li>
 * <li><b>Home</b> (sneak and use him): he settles there as a Prospector and trades like any townsperson.</li>
 * </ul>
 * Plain use tells you both; out of town, he'll say a town is where he'd start. Either earns A Second Chance, and the
 * Gazette hears of it ({@link Aftermath}).
 */
public final class FreedMen {
    private FreedMen() {}

    public static final int WAGE = 1;

    /** {@code player} uses the freed {@code man}: hired, sent home, or told how. What happened ({@code hired}, {@code home}, or why not). */
    public static String talk(ServerLevel level, ServerPlayer player, AbstractOutlaw man, InteractionHand hand) {
        if (TownSite.townAt(level, man.blockPosition()).isEmpty()) {
            say(player, man, "no_town");
            return "no_town";
        }
        ItemStack held = player.getItemInHand(hand);
        if (held.is(ModItems.GOLD_COIN.get())) {
            return hire(level, player, man, held) != null ? "hired" : "wanted";
        }
        if (player.isSecondaryUseActive()) {
            sendHome(level, player, man);
            return "home";
        }
        say(player, man, "offer");
        player.sendSystemMessage(Component.translatable("message.hollowfrontier.freed.hint", WAGE).withStyle(ChatFormatting.GRAY, ChatFormatting.ITALIC));
        return "offer";
    }

    /** Swears him in as a Deputy where he stands, for a coin from {@code coins} (none in creative). Null if he won't take the job from them. */
    public static @Nullable Lawman hire(ServerLevel level, ServerPlayer player, AbstractOutlaw man, ItemStack coins) {
        if (Wanted.stars(player) >= 2) {
            say(player, man, "wanted");
            return null;
        }
        BlockPos post = man.blockPosition();
        say(player, man, "hired");
        Lawman deputy = man.convertTo(ModEntities.LAWMAN.get(), ConversionParams.single(man, false, false), lawman -> lawman.swearIn(post));
        if (deputy == null) {
            return null;
        }
        if (!player.getAbilities().instabuild) {
            coins.shrink(WAGE);
        }
        level.playSound(null, post, SoundEvents.ARMOR_EQUIP_IRON.value(), SoundSource.NEUTRAL, 1.0F, 1.0F);
        level.sendParticles(ParticleTypes.HAPPY_VILLAGER, deputy.getX(), deputy.getY(1.0), deputy.getZ(), 8, 0.3, 0.4, 0.3, 0);
        ModCriteria.fire(player, ModCriteria.SECOND_CHANCE);
        Aftermath.get(level).record(level, Aftermath.Kind.DEPUTY, player.getName().getString(), post);
        return deputy;
    }

    /** He settles in the town as a Prospector. */
    public static @Nullable Townsfolk sendHome(ServerLevel level, ServerPlayer player, AbstractOutlaw man) {
        BlockPos home = man.blockPosition();
        say(player, man, "home");
        Townsfolk prospector = man.convertTo(ModEntities.TOWNSFOLK.get(), ConversionParams.single(man, false, false),
                folk -> folk.setFolk(Folk.PROSPECTOR));
        if (prospector == null) {
            return null;
        }
        level.sendParticles(ParticleTypes.HAPPY_VILLAGER, prospector.getX(), prospector.getY(1.0), prospector.getZ(), 8, 0.3, 0.4, 0.3, 0);
        ModCriteria.fire(player, ModCriteria.SECOND_CHANCE);
        Aftermath.get(level).record(level, Aftermath.Kind.HOMECOMING, player.getName().getString(), home);
        return prospector;
    }

    private static void say(ServerPlayer player, AbstractOutlaw man, String what) {
        player.sendSystemMessage(Component.translatable("message.hollowfrontier.npc.say", man.getDisplayName(),
                Component.translatable("message.hollowfrontier.freed." + what)));
    }
}
