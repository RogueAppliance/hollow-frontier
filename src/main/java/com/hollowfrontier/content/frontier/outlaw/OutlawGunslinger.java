package com.hollowfrontier.content.frontier.outlaw;

import com.hollowfrontier.content.frontier.gun.GunStats;
import com.hollowfrontier.content.frontier.gun.GunTypes;
import com.hollowfrontier.registry.ModItems;

import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

// Long-range rifleman: hangs back and snipes.
public class OutlawGunslinger extends AbstractOutlaw {
    public OutlawGunslinger(EntityType<? extends OutlawGunslinger> type, Level level) {
        super(type, level);
    }

    public static AttributeSupplier.Builder createAttributes() {
        return createOutlawAttributes()
                .add(Attributes.MAX_HEALTH, 24.0)
                .add(Attributes.FOLLOW_RANGE, 56.0);
    }

    @Override
    public GunStats gunStats() {
        return GunTypes.OUTLAW_RIFLE;
    }

    @Override
    protected ItemStack createGun() {
        return new ItemStack(ModItems.PLAINS_RIFLE.get());
    }
}
