package com.hollowfrontier.content.ufo.taken;

import java.util.function.Consumer;

import com.hollowfrontier.content.frontier.mystery.ManInBlack;
import com.hollowfrontier.content.frontier.outlaw.AbstractOutlaw;
import com.hollowfrontier.content.hollow.creature.GrayProbe;
import com.hollowfrontier.content.hollow.gray.Specimens;
import com.hollowfrontier.content.ufo.GrayDrone;
import com.hollowfrontier.content.ufo.GrayWarden;
import com.hollowfrontier.content.ufo.PhlogistonCellItem;
import com.hollowfrontier.registry.ModDamageTypes;

import net.minecraft.ChatFormatting;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.TooltipDisplay;

/**
 * The Shock Rod (50), off the surgeons' tray: what the Grays keep their subjects quiet with. A blow from it costs one
 * charge from the Phlogiston Cells its holder carries. It does {@link #DAMAGE} more, and whatever it lands on that is a
 * Gray, is one of their machines, or carries one of their implants (an outlaw, a man in a duster, a tagged beast) is
 * {@link #STUN_TICKS} out of the fight: a Warden drops whoever it was after and stands where it is. With no charge it's a
 * short metal stick.
 */
public class ShockRodItem extends Item {
    public static final int STUN_TICKS = 60;
    public static final float DAMAGE = 4.0F;

    public ShockRodItem(Properties properties) {
        super(properties);
    }

    /** Whether the rod's charge does more to this than hurt: Grays, their machines, and anything they've put an implant in. */
    public static boolean stuns(LivingEntity target) {
        return target instanceof GrayWarden || target instanceof GrayProbe || target instanceof GrayDrone || target instanceof AbstractOutlaw
                || target instanceof ManInBlack || Specimens.isTagged(target);
    }

    @Override
    public void hurtEnemy(ItemStack stack, LivingEntity target, LivingEntity attacker) {
        if (!(attacker.level() instanceof ServerLevel level) || !(attacker instanceof Player player)) {
            return;
        }
        if (!PhlogistonCellItem.draw(player, 1)) {
            player.sendOverlayMessage(Component.translatable("message.hollowfrontier.no_charge").withStyle(ChatFormatting.GRAY));
            return;
        }
        shock(level, player, target);
    }

    /** The charge lands (no cell is drawn here). Whether it stunned. */
    public static boolean shock(ServerLevel level, LivingEntity attacker, LivingEntity target) {
        target.hurtServer(level, ModDamageTypes.phlogistonBeam(attacker), DAMAGE);
        level.sendParticles(ParticleTypes.ELECTRIC_SPARK, target.getX(), target.getY(0.6), target.getZ(), 14, 0.3, 0.5, 0.3, 0.15);
        level.playSound(null, target.blockPosition(), SoundEvents.BEE_STING, SoundSource.PLAYERS, 1.0F, 0.6F);
        if (!target.isAlive() || !stuns(target)) {
            return false;
        }
        if (target instanceof GrayWarden warden) {
            warden.stun(STUN_TICKS);
        } else {
            target.addEffect(new MobEffectInstance(MobEffects.SLOWNESS, STUN_TICKS, 6), attacker);
            target.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, STUN_TICKS, 3), attacker);
            if (target instanceof Mob mob) {
                mob.setTarget(null);
                mob.getNavigation().stop();
            }
        }
        return true;
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, TooltipDisplay display, Consumer<Component> builder, TooltipFlag flag) {
        builder.accept(Component.translatable("tooltip.hollowfrontier.shock_rod").withStyle(ChatFormatting.GRAY));
        builder.accept(Component.translatable("tooltip.hollowfrontier.shock_rod.charge").withStyle(ChatFormatting.DARK_GRAY));
    }
}
