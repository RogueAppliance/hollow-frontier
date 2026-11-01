package com.hollowfrontier.content.concavia;

import java.util.function.Consumer;

import org.jspecify.annotations.Nullable;

import com.hollowfrontier.content.frontier.mystery.ManInBlack;
import com.hollowfrontier.content.frontier.outlaw.AbstractOutlaw;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.TooltipDisplay;
import net.minecraft.world.phys.AABB;

/**
 * The Kindled Sigil (12a): what the Kindled give an ally. Held in either hand, it shows the Grays' work: every outlaw and
 * man in black within {@link #RANGE} glows (the implant behind the ear), seen through walls, for as long as it's held.
 * The Kindled will make another for an ally who loses theirs.
 */
public class KindledSigilItem extends Item {
    public static final double RANGE = 32.0;
    private static final int GLOW_TICKS = 30;

    public KindledSigilItem(Properties properties) {
        super(properties);
    }

    @Override
    public void inventoryTick(ItemStack stack, ServerLevel level, Entity entity, @Nullable EquipmentSlot slot) {
        if ((slot == EquipmentSlot.MAINHAND || slot == EquipmentSlot.OFFHAND) && entity.tickCount % 20 == 0) {
            reveal(level, entity);
        }
    }

    /** Lights up the implanted round {@code holder}; how many it found. */
    public static int reveal(ServerLevel level, Entity holder) {
        int found = 0;
        for (LivingEntity implanted : level.getEntitiesOfClass(LivingEntity.class, holder.getBoundingBox().inflate(RANGE),
                e -> e.isAlive() && isImplanted(e))) {
            implanted.addEffect(new MobEffectInstance(MobEffects.GLOWING, GLOW_TICKS, 0, false, false));
            found++;
        }
        return found;
    }

    public static boolean isImplanted(Entity entity) {
        return entity instanceof AbstractOutlaw outlaw && !outlaw.isFreed() || entity instanceof ManInBlack; // a freed outlaw's implant is dark (13d)
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, TooltipDisplay display, Consumer<Component> builder, TooltipFlag flag) {
        builder.accept(Component.translatable("tooltip.hollowfrontier.kindled_sigil").withStyle(ChatFormatting.GRAY));
    }
}
