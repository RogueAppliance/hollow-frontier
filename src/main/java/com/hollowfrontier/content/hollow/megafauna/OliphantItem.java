package com.hollowfrontier.content.hollow.megafauna;

import com.hollowfrontier.content.hollow.gray.Specimens;
import com.hollowfrontier.registry.ModSounds;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundSource;
import net.minecraft.stats.Stats;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.gameevent.GameEvent;

/**
 * The Oliphant (10a): a horn carved from a mammoth's tusk, with a brass mouthpiece. One long blast calms the angry
 * mammoths and megatheria within {@link #RANGE} blocks (they drop their quarrel) and puts the terror birds there to
 * flight for {@link #SCARE_TICKS}; beasts the Grays have tagged (10b) pay it no heed. Then it needs {@link #COOLDOWN}
 * ticks before it can be blown again.
 */
public class OliphantItem extends Item {
    public static final double RANGE = 24.0;
    public static final int SCARE_TICKS = 200;
    public static final int COOLDOWN = 600;

    public OliphantItem(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResult use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        level.playSound(null, player.getX(), player.getY(), player.getZ(), ModSounds.OLIPHANT_BLOW.get(), SoundSource.PLAYERS, 4.0F, 1.0F);
        if (level instanceof ServerLevel server) {
            blow(server, player);
            player.getCooldowns().addCooldown(stack, COOLDOWN);
            player.awardStat(Stats.ITEM_USED.get(this));
            level.gameEvent(GameEvent.INSTRUMENT_PLAY, player.position(), GameEvent.Context.of(player));
        }
        return InteractionResult.CONSUME;
    }

    /** Calms the neutral beasts around {@code player} and scares off the terror birds; returns how many it reached. */
    public static int blow(ServerLevel level, Player player) {
        int reached = 0;
        for (Mob mob : level.getEntitiesOfClass(Mob.class, player.getBoundingBox().inflate(RANGE), Mob::isAlive)) {
            if (Specimens.isTagged(mob)) {
                continue; // the implant drowns it out
            }
            if (mob instanceof TerrorBird bird) {
                bird.scare(player.position(), SCARE_TICKS);
                reached++;
            } else if ((mob instanceof WoollyMammoth || mob instanceof Megatherium) && Megafauna.isAngry(mob)) {
                Megafauna.calm(mob);
                reached++;
            }
        }
        return reached;
    }
}
