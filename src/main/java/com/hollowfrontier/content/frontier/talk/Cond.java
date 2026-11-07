package com.hollowfrontier.content.frontier.talk;

import java.util.EnumSet;
import java.util.Set;

/**
 * When a line may be said: every fact in {@code all} holds, none in {@code none} does, and the listener's act is between
 * {@code from} and {@code to}. The more a line asks for, the likelier it is to be picked when it fits ({@link #weight}).
 */
public record Cond(Set<Fact> all, Set<Fact> none, Act from, Act to) {
    public static final Cond ANY = new Cond(EnumSet.noneOf(Fact.class), EnumSet.noneOf(Fact.class), Act.FRONTIER, Act.AFTER);

    public static Cond when(Fact first, Fact... rest) {
        return ANY.and(first, rest);
    }

    /** From this act on. */
    public static Cond act(Act act) {
        return ANY.from(act);
    }

    /** In this act only. */
    public static Cond only(Act act) {
        return new Cond(ANY.all, ANY.none, act, act);
    }

    public Cond and(Fact first, Fact... rest) {
        EnumSet<Fact> more = copy(all);
        more.add(first);
        more.addAll(Set.of(rest));
        return new Cond(more, none, from, to);
    }

    public Cond unless(Fact first, Fact... rest) {
        EnumSet<Fact> more = copy(none);
        more.add(first);
        more.addAll(Set.of(rest));
        return new Cond(all, more, from, to);
    }

    public Cond from(Act act) {
        return new Cond(all, none, act, to);
    }

    /** Up to and including this act. */
    public Cond until(Act act) {
        return new Cond(all, none, from, act);
    }

    public boolean test(Context context) {
        return context.facts().containsAll(all) && none.stream().noneMatch(context::has)
                && context.act().atLeast(from) && to.atLeast(context.act())
                && (from != Act.GATEWAY || context.beenDown());
    }

    /** Whether this asks for an urgent fact ({@link Fact#urgent}). */
    public boolean urgent() {
        return all.stream().anyMatch(f -> f.urgent);
    }

    /** One for an everyday line, five more for each thing it asks for (a fact, or a later act). */
    public int weight() {
        return 1 + 5 * (all.size() + (from != Act.FRONTIER ? 1 : 0));
    }

    private static EnumSet<Fact> copy(Set<Fact> facts) {
        return facts.isEmpty() ? EnumSet.noneOf(Fact.class) : EnumSet.copyOf(facts);
    }
}
