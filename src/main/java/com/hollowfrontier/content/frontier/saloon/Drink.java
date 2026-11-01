package com.hollowfrontier.content.frontier.saloon;

import java.util.List;

import com.hollowfrontier.registry.ModCriteria;
import com.hollowfrontier.registry.ModMobEffects;

import net.minecraft.ChatFormatting;
import net.minecraft.core.Holder;
import net.minecraft.network.chat.Component;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;

/**
 * A saloon drink: fixed effects, plus Tipsy levels for liquor (negative = sobers you up by that many levels).
 * A gamble drink (snake oil) also applies one random outcome from GAMBLE.
 */
public record Drink(List<Dose> effects, int tipsy, boolean gamble) {
    /** One status effect a drink gives. */
    public record Dose(Holder<MobEffect> effect, int seconds, int amplifier) {
        MobEffectInstance instance() {
            return new MobEffectInstance(effect, seconds * 20, amplifier);
        }
    }

    private static Dose dose(Holder<MobEffect> effect, int seconds, int amplifier) {
        return new Dose(effect, seconds, amplifier);
    }

    public static final Drink FRONTIER_BEER = new Drink(List.of(dose(MobEffects.REGENERATION, 10, 0)), 1, false);
    public static final Drink SARSAPARILLA = new Drink(List.of(dose(MobEffects.SPEED, 45, 0)), -TipsyEffect.MAX_AMPLIFIER - 1, false);
    public static final Drink COWBOY_COFFEE = new Drink(List.of(dose(MobEffects.HASTE, 90, 0)), -1, false);
    public static final Drink RYE_WHISKEY = new Drink(List.of(dose(MobEffects.STRENGTH, 60, 0), dose(MobEffects.RESISTANCE, 60, 0)), 1, false);
    public static final Drink MEZCAL = new Drink(List.of(dose(MobEffects.NIGHT_VISION, 120, 0), dose(MobEffects.JUMP_BOOST, 60, 0)), 2, false);
    public static final Drink MOONSHINE = new Drink(List.of(dose(MobEffects.FIRE_RESISTANCE, 90, 0), dose(MobEffects.STRENGTH, 30, 1)), 3, false);
    public static final Drink SNAKE_OIL = new Drink(List.of(), 0, true);

    // Doc's miracle tonic: five ways it helps, three ways it doesn't.
    public static final List<Dose> GAMBLE = List.of(
            dose(MobEffects.REGENERATION, 15, 1), dose(MobEffects.ABSORPTION, 60, 1), dose(MobEffects.SPEED, 30, 1),
            dose(MobEffects.HEALTH_BOOST, 120, 0), dose(MobEffects.LUCK, 120, 0),
            dose(MobEffects.POISON, 8, 0), dose(MobEffects.SLOWNESS, 20, 1), dose(MobEffects.LEVITATION, 3, 0));

    /** Applies everything (server side). */
    public void drink(LivingEntity drinker) {
        effects.forEach(d -> drinker.addEffect(d.instance()));
        if (gamble) {
            gamble(drinker, drinker.getRandom().nextInt(GAMBLE.size()));
        }
        if (tipsy > 0) {
            addTipsy(drinker, tipsy);
        } else if (tipsy < 0) {
            sober(drinker, -tipsy);
        }
    }

    /**
     * One of the tonic's outcomes ({@code outcome}, an index into GAMBLE), and Doc's promise for it on the drinker's action
     * bar (21b), which never matches what happens: {@code message.hollowfrontier.snake_oil.<outcome>}. The key said.
     */
    public static String gamble(LivingEntity drinker, int outcome) {
        drinker.addEffect(GAMBLE.get(outcome).instance());
        String claim = "message.hollowfrontier.snake_oil." + outcome;
        if (drinker instanceof Player player) {
            player.sendOverlayMessage(Component.translatable("message.hollowfrontier.snake_oil", Component.translatable(claim))
                    .withStyle(ChatFormatting.GOLD, ChatFormatting.ITALIC));
        }
        return claim;
    }

    /** Tipsy level (1-4), 0 when sober. */
    public static int tipsyLevel(LivingEntity entity) {
        MobEffectInstance tipsy = entity.getEffect(ModMobEffects.TIPSY);
        return tipsy == null ? 0 : tipsy.getAmplifier() + 1;
    }

    public static void addTipsy(LivingEntity drinker, int levels) {
        int level = tipsyLevel(drinker) + levels;
        if (level > TipsyEffect.MAX_AMPLIFIER + 1) {
            // One too many: black out for a few seconds and come round at Tipsy III.
            drinker.addEffect(new MobEffectInstance(MobEffects.BLINDNESS, 100, 0));
            drinker.addEffect(new MobEffectInstance(MobEffects.SLOWNESS, 100, 3));
            drinker.addEffect(new MobEffectInstance(MobEffects.NAUSEA, 200, 0));
            if (drinker instanceof Player player) {
                player.sendOverlayMessage(Component.translatable("message.hollowfrontier.blackout").withStyle(ChatFormatting.GOLD));
            }
            ModCriteria.fire(drinker, ModCriteria.BLACKED_OUT);
            level = TipsyEffect.MAX_AMPLIFIER;
        }
        setTipsy(drinker, level);
    }

    public static void sober(LivingEntity drinker, int levels) {
        drinker.removeEffect(MobEffects.NAUSEA);
        setTipsy(drinker, Math.max(0, tipsyLevel(drinker) - levels));
    }

    // addEffect never lowers an amplifier, so replace the effect outright.
    private static void setTipsy(LivingEntity entity, int level) {
        entity.removeEffect(ModMobEffects.TIPSY);
        if (level > 0) {
            entity.addEffect(new MobEffectInstance(ModMobEffects.TIPSY, level * TipsyEffect.TICKS_PER_LEVEL, level - 1));
        }
    }
}
