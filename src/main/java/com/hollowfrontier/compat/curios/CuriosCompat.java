package com.hollowfrontier.compat.curios;

import java.util.function.BiConsumer;

import com.hollowfrontier.content.concavia.KindledSigilItem;
import com.hollowfrontier.content.concavia.SunstoneLensItem;
import com.hollowfrontier.registry.ModItems;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.capabilities.RegisterCapabilitiesEvent;
import top.theillusivec4.curios.api.CuriosCapability;
import top.theillusivec4.curios.api.SlotContext;
import top.theillusivec4.curios.api.type.capability.ICurio;

/**
 * Curios (optional): the Sunstone Lens worn as a charm lends night vision, and the Kindled Sigil worn as a necklace
 * shows the implanted, just as they do held. The beam still needs the lens in hand. Which slot takes which item, and
 * that players have those slots, is data ({@code curios:charm}/{@code curios:necklace} item tags and
 * {@code data/hollowfrontier/curios/entities/player.json}, both from datagen). Only loaded when Curios is.
 */
public final class CuriosCompat {
    private CuriosCompat() {}

    public static void registerCapabilities(RegisterCapabilitiesEvent event) {
        worn(event, ModItems.SUNSTONE_LENS.get(), (wearer, level) -> SunstoneLensItem.lendSight(wearer));
        worn(event, ModItems.KINDLED_SIGIL.get(), (wearer, level) -> KindledSigilItem.reveal(level, wearer));
    }

    /** Makes {@code item} a curio that does {@code effect} for its wearer once a second, server side. */
    private static void worn(RegisterCapabilitiesEvent event, net.minecraft.world.item.Item item, BiConsumer<LivingEntity, ServerLevel> effect) {
        event.registerItem(CuriosCapability.ITEM, (stack, context) -> new ICurio() {
            @Override
            public ItemStack getStack() {
                return stack;
            }

            @Override
            public void curioTick(SlotContext slot) {
                LivingEntity wearer = slot.entity();
                if (wearer.tickCount % 20 == 0 && wearer.level() instanceof ServerLevel level) {
                    effect.accept(wearer, level);
                }
            }
        }, item);
    }
}
