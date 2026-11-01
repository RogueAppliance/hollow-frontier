package com.hollowfrontier.content.ufo;

import java.util.function.Consumer;

import com.hollowfrontier.registry.ModItems;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.item.ItemStack;

/**
 * The parts fitted to a Prototype Saucer (13c): the beam emitter (the tractor beam), the lens array (the heat laser) and
 * the navigation core (bearings); and from the Mothership's rematches (15b), the cloaking device (unseen at night) and
 * the cargo hold (nine slots). A crafted saucer comes with the first two ({@link #STANDARD}, since the playtest); the
 * rest are fitted by using them on the craft (as are the first two, on a saucer that somehow lacks them). They go with
 * the saucer when it's folded back into an item.
 * <p>
 * {@link #mask} packs it into an int for the renderer, which shows each part once fitted.
 */
public record SaucerFittings(boolean emitter, boolean lens, boolean nav, boolean cloak, boolean hold) {
    public static final SaucerFittings NONE = new SaucerFittings(false, false, false);
    public static final SaucerFittings STANDARD = new SaucerFittings(true, true, false);
    public static final SaucerFittings ALL = new SaucerFittings(true, true, true, true, true);
    public static final int EMITTER = 1, LENS = 2, NAV = 4, CLOAK = 8, HOLD = 16;

    public static final Codec<SaucerFittings> CODEC = RecordCodecBuilder.create(i -> i.group(
            Codec.BOOL.optionalFieldOf("emitter", false).forGetter(SaucerFittings::emitter),
            Codec.BOOL.optionalFieldOf("lens", false).forGetter(SaucerFittings::lens),
            Codec.BOOL.optionalFieldOf("nav", false).forGetter(SaucerFittings::nav),
            Codec.BOOL.optionalFieldOf("cloak", false).forGetter(SaucerFittings::cloak),
            Codec.BOOL.optionalFieldOf("hold", false).forGetter(SaucerFittings::hold)).apply(i, SaucerFittings::new));
    public static final StreamCodec<RegistryFriendlyByteBuf, SaucerFittings> STREAM_CODEC = ByteBufCodecs.VAR_INT.map(
            SaucerFittings::fromMask, SaucerFittings::mask).cast();

    /** The three 13c parts, without the rematch fittings. */
    public SaucerFittings(boolean emitter, boolean lens, boolean nav) {
        this(emitter, lens, nav, false, false);
    }

    /** Whether {@code stack} is one of the parts at all. */
    public static boolean isPart(ItemStack stack) {
        return stack.is(ModItems.BEAM_EMITTER.get()) || stack.is(ModItems.LENS_ARRAY.get()) || stack.is(ModItems.NAVIGATION_CORE.get())
                || stack.is(ModItems.CLOAKING_DEVICE.get()) || stack.is(ModItems.CARGO_HOLD.get());
    }

    /** Fits {@code part} if it's one not fitted yet: the fittings after, or this unchanged. */
    public SaucerFittings fit(ItemStack part) {
        if (part.is(ModItems.BEAM_EMITTER.get()) && !emitter) {
            return fromMask(mask() | EMITTER);
        }
        if (part.is(ModItems.LENS_ARRAY.get()) && !lens) {
            return fromMask(mask() | LENS);
        }
        if (part.is(ModItems.NAVIGATION_CORE.get()) && !nav) {
            return fromMask(mask() | NAV);
        }
        if (part.is(ModItems.CLOAKING_DEVICE.get()) && !cloak) {
            return fromMask(mask() | CLOAK);
        }
        if (part.is(ModItems.CARGO_HOLD.get()) && !hold) {
            return fromMask(mask() | HOLD);
        }
        return this;
    }

    /** Whether every fitting there is has been fitted. */
    public boolean full() {
        return mask() == ALL.mask();
    }

    public static SaucerFittings fromMask(int mask) {
        return new SaucerFittings((mask & EMITTER) != 0, (mask & LENS) != 0, (mask & NAV) != 0, (mask & CLOAK) != 0, (mask & HOLD) != 0);
    }

    public int mask() {
        return (emitter ? EMITTER : 0) | (lens ? LENS : 0) | (nav ? NAV : 0) | (cloak ? CLOAK : 0) | (hold ? HOLD : 0);
    }

    /** A line per part fitted, or one saying none are (tooltips). */
    public void describe(Consumer<Component> out) {
        if (mask() == 0) {
            out.accept(Component.translatable("tooltip.hollowfrontier.prototype_saucer.no_fittings"));
            return;
        }
        if (emitter) out.accept(fitted(ModItems.BEAM_EMITTER.get().getDefaultInstance()));
        if (lens) out.accept(fitted(ModItems.LENS_ARRAY.get().getDefaultInstance()));
        if (nav) out.accept(fitted(ModItems.NAVIGATION_CORE.get().getDefaultInstance()));
        if (cloak) out.accept(fitted(ModItems.CLOAKING_DEVICE.get().getDefaultInstance()));
        if (hold) out.accept(fitted(ModItems.CARGO_HOLD.get().getDefaultInstance()));
    }

    private static Component fitted(ItemStack part) {
        return Component.translatable("tooltip.hollowfrontier.prototype_saucer.fitted", part.getHoverName());
    }
}
