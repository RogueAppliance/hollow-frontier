package com.hollowfrontier.content.frontier.saloon;

import java.util.List;
import java.util.Locale;
import java.util.function.Supplier;

import com.hollowfrontier.registry.ModItems;

import net.minecraft.world.item.Item;

/**
 * Who's tending bar. Each hostess has her own look (geckolib/models/entity/barmaid_<id>.geo.json and
 * textures/entity/barmaid_<id>.png), idle pose ("idle.<id>"), lines (her Chatter voice, named by id)
 * and two house specialties she pours for a coin less.
 */
public enum Hostess {
    /** Miss Ruby: smoky charm, crimson satin and black lace. Whiskey and moonshine. */
    RUBY(ModItems.RYE_WHISKEY, ModItems.MOONSHINE),
    /** Clementine: sunny and bubbly, yellow gingham. Sarsaparilla and coffee. */
    CLEMENTINE(ModItems.SARSAPARILLA, ModItems.COWBOY_COFFEE),
    /** Josie: sharp-tongued card sharp in emerald, feathered hat. Snake oil and mezcal. */
    JOSIE(ModItems.SNAKE_OIL, ModItems.MEZCAL),
    /** Miss Belle: genteel Southern belle in lavender and pearls. Beer and whiskey. */
    BELLE(ModItems.FRONTIER_BEER, ModItems.RYE_WHISKEY),
    /** Lupe: warm and teasing, turquoise skirt, marigold in her braid. Mezcal and coffee. */
    LUPE(ModItems.MEZCAL, ModItems.COWBOY_COFFEE);

    private final List<Supplier<? extends Item>> specialties;

    @SafeVarargs
    Hostess(Supplier<? extends Item>... specialties) {
        this.specialties = List.of(specialties);
    }

    public String id() {
        return name().toLowerCase(Locale.ROOT);
    }

    public String nameKey() {
        return "entity.hollowfrontier.barmaid." + id();
    }

    public boolean isSpecialty(Item item) {
        return specialties.stream().anyMatch(s -> s.get() == item);
    }

    public static Hostess byId(String id) {
        for (Hostess h : values()) {
            if (h.id().equals(id)) {
                return h;
            }
        }
        return RUBY;
    }
}
