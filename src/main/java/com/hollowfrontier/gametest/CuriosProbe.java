package com.hollowfrontier.gametest;

import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import top.theillusivec4.curios.api.CuriosApi;

// Curios calls for CompatTests, kept apart so its classes load only when Curios is there.
final class CuriosProbe {
    private CuriosProbe() {}

    /** Puts {@code stack} in the first {@code slot} slot; false if the wearer has no such slot. */
    static boolean wear(LivingEntity wearer, String slot, ItemStack stack) {
        var curios = CuriosApi.getCuriosInventory(wearer).orElse(null);
        if (curios == null || !curios.getCurios().containsKey(slot)) {
            return false;
        }
        curios.setEquippedCurio(slot, 0, stack);
        return true;
    }

    /** What Curios makes of the wearer's slots, for failure messages. */
    static String describe(LivingEntity wearer, ItemStack probe) {
        var curios = CuriosApi.getCuriosInventory(wearer).orElse(null);
        return "curio cap on " + probe.getItem() + ": " + CuriosApi.getCurio(probe).isPresent()
                + ", slots: " + (curios == null ? "none" : curios.getCurios().keySet())
                + ", equipped: " + (curios != null && curios.isEquipped(probe.getItem()));
    }

    /**
     * Ticks what the wearer has on, as Curios does each tick in a real game. Curios skips its own tick on a
     * non-dedicated server whose client hasn't sent slot data, which the headless test server always is.
     */
    static void tickWorn(LivingEntity wearer) {
        CuriosApi.getCuriosInventory(wearer).ifPresent(curios -> curios.findCurios(stack -> true)
                .forEach(worn -> CuriosApi.getCurio(worn.stack()).ifPresent(curio -> curio.curioTick(worn.slotContext()))));
    }
}
