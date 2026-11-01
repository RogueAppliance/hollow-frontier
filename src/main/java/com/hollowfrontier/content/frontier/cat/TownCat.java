package com.hollowfrontier.content.frontier.cat;

import java.util.Locale;

import com.hollowfrontier.registry.ModAttachments;
import com.hollowfrontier.registry.ModItems;
import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.util.StringRepresentable;
import net.minecraft.world.entity.animal.feline.Cat;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.DyedItemColor;

/**
 * A vanilla cat's frontier data (the TOWN_CAT attachment, saved and synced): where in town it lives, what it wears and
 * whether it's the undertaker's ghost. Town blueprints place cats with only a role; {@link TownCats} rolls the rest the
 * first time the cat joins the world ({@code settled} false until then). Player-dressed cats elsewhere are STRAYs.
 *
 * @param worn  the accessory item itself (bandana, cowboy hat or brass star), returned as is when taken off; empty for none
 * @param faded an untamed ghost cat that has faded out (daytime, or shied away from a sprinting player); server-set, synced
 */
public record TownCat(Role role, ItemStack worn, boolean ghost, boolean settled, boolean faded) {
    public static final TownCat STRAY = new TownCat(Role.STRAY, ItemStack.EMPTY, false, true, false);

    public static final MapCodec<TownCat> MAP_CODEC = RecordCodecBuilder.mapCodec(i -> i.group(
            Role.CODEC.optionalFieldOf("role", Role.STRAY).forGetter(TownCat::role),
            ItemStack.OPTIONAL_CODEC.optionalFieldOf("worn", ItemStack.EMPTY).forGetter(TownCat::worn),
            Codec.BOOL.optionalFieldOf("ghost", false).forGetter(TownCat::ghost),
            Codec.BOOL.optionalFieldOf("settled", false).forGetter(TownCat::settled),
            Codec.BOOL.optionalFieldOf("faded", false).forGetter(TownCat::faded)
    ).apply(i, TownCat::new));

    public static final StreamCodec<RegistryFriendlyByteBuf, TownCat> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.idMapper(i -> Role.values()[i], Role::ordinal), TownCat::role,
            ItemStack.OPTIONAL_STREAM_CODEC, TownCat::worn,
            ByteBufCodecs.BOOL, TownCat::ghost,
            ByteBufCodecs.BOOL, TownCat::settled,
            ByteBufCodecs.BOOL, TownCat::faded,
            TownCat::new);

    /** The cat's data, or STRAY for a plain cat (without creating the attachment). */
    public static TownCat of(Cat cat) {
        return cat.hasData(ModAttachments.TOWN_CAT.get()) ? cat.getData(ModAttachments.TOWN_CAT.get()) : STRAY;
    }

    public static void set(Cat cat, TownCat data) {
        cat.setData(ModAttachments.TOWN_CAT.get(), data);
    }

    /** Lives in a town (placed by a blueprint) and hasn't been taken home by a player. */
    public static boolean isTownCat(Cat cat) {
        return !cat.isTame() && of(cat).role() != Role.STRAY;
    }

    public TownCat wearing(ItemStack stack) {
        return new TownCat(role, stack, ghost, settled, faded);
    }

    public TownCat withFaded(boolean value) {
        return new TownCat(role, worn, ghost, settled, value);
    }

    public Accessory accessory() {
        return Accessory.of(worn);
    }

    /** Bandana dye (an undyed bandana is red, like the item). */
    public int bandanaColor() {
        return DyedItemColor.getOrDefault(worn, Accessory.DEFAULT_BANDANA);
    }

    /**
     * Where a town cat lives. Each blueprint spot has a role: the chance the spot has a cat at all, and what it favours
     * (see TownCatOdds). STRAY is any other cat.
     */
    public enum Role implements StringRepresentable {
        STRAY, SALOON, PORCH, STORE, HOME, LIVERY, SMITHY, SHERIFF, GHOST;

        public static final Codec<Role> CODEC = StringRepresentable.fromEnum(Role::values);

        @Override
        public String getSerializedName() {
            return name().toLowerCase(Locale.ROOT);
        }
    }

    /** What a cat can wear: one thing at a time, drawn by the client's accessory layer. */
    public enum Accessory {
        NONE, BANDANA, COWBOY_HAT, SHERIFF_STAR;

        public static final int DEFAULT_BANDANA = 0xFF000000 | DyeColor.RED.getTextureDiffuseColor();

        public static Accessory of(ItemStack stack) {
            if (stack.is(ModItems.BANDANA.get())) return BANDANA;
            if (stack.is(ModItems.COWBOY_HAT.get())) return COWBOY_HAT;
            if (stack.is(ModItems.BRASS_STAR.get())) return SHERIFF_STAR;
            return NONE;
        }
    }
}
