package com.hollowfrontier.content.frontier.outlaw;

import com.hollowfrontier.content.frontier.gun.GunStats;
import com.hollowfrontier.content.frontier.gun.GunTypes;
import com.hollowfrontier.registry.ModItems;

import net.minecraft.world.entity.EntityType;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

// Common revolver-wielding outlaw.
public class Outlaw extends AbstractOutlaw {
    public Outlaw(EntityType<? extends Outlaw> type, Level level) {
        super(type, level);
    }

    @Override
    public GunStats gunStats() {
        return GunTypes.OUTLAW_REVOLVER;
    }

    @Override
    protected ItemStack createGun() {
        return new ItemStack(ModItems.REVOLVER.get());
    }
}
