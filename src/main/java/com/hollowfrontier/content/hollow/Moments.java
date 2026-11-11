package com.hollowfrontier.content.hollow;

import java.util.LinkedHashMap;
import java.util.Map;

import com.hollowfrontier.HollowFrontier;
import com.hollowfrontier.registry.ModSounds;

import net.minecraft.core.Holder;
import net.minecraft.network.protocol.game.ClientboundSoundPacket;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.AdvancementEvent;

/**
 * The stings (45c): a few seconds of music at five turns of the story, each heard once by each player in a world. Each
 * rides on an advancement that already marks the moment, so "once" is the advancement's own: it can't be earned twice.
 * Only the player who earned it hears it.
 */
@EventBusSubscriber(modid = HollowFrontier.MODID)
public final class Moments {
    private Moments() {}

    /** Advancement to sting. */
    public static Map<Identifier, Holder<SoundEvent>> stings() {
        Map<Identifier, Holder<SoundEvent>> stings = new LinkedHashMap<>();
        stings.put(HollowFrontier.id("concavia/well_met"), ModSounds.STING_WELL_MET);             // the first Kindled met
        stings.put(HollowFrontier.id("concavia/temple_of_the_sun"), ModSounds.STING_SUN_TEMPLE);  // the Sun Temple
        stings.put(HollowFrontier.id("hollow/still_running"), ModSounds.STING_STILL_RUNNING);     // the first Gray station
        stings.put(HollowFrontier.id("hollow/drowned_quarter"), ModSounds.STING_DROWNED_QUARTER); // the Drowned Quarter
        stings.put(HollowFrontier.id("ufo/cut_loose"), ModSounds.STING_CUT_LOOSE);                // the leash broken
        return stings;
    }

    @SubscribeEvent
    static void onAdvancementEarned(AdvancementEvent.AdvancementEarnEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            Holder<SoundEvent> sting = stings().get(event.getAdvancement().id());
            if (sting != null) {
                play(player, sting);
            }
        }
    }

    /** To this player alone, where they stand, on the Ambient slider. */
    public static void play(ServerPlayer player, Holder<SoundEvent> sting) {
        player.connection.send(new ClientboundSoundPacket(sting, SoundSource.AMBIENT, player.getX(), player.getEyeY(), player.getZ(),
                1.0F, 1.0F, player.level().getRandom().nextLong()));
    }
}
