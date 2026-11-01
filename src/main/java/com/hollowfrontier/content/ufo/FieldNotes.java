package com.hollowfrontier.content.ufo;

import java.util.List;

/**
 * A Gray observer's field notes on the frontier (phase 21a), left in the caches of field labs, stations and landings:
 * a written book of a title page and {@link #PER_BOOK} entries. There are {@link #VARIANTS} copies, each with its own
 * entries, so the loot tables roll one at random (no loot function needed). The English lives here and the lang provider
 * reads it: the title page is {@code book.hollowfrontier.field_notes.title}, the entries {@code ....field_notes.<n>}.
 */
public final class FieldNotes {
    private FieldNotes() {}

    public static final int VARIANTS = 8, PER_BOOK = 4;
    public static final String TITLE = "Field Notes", AUTHOR = "Observer 7";
    public static final String TITLE_PAGE = "FIELD NOTES\n\nSpecimen group: cowpokes.\nSurface, western range.\n\nObserver's copy. Do not leave behind.";

    public static final List<String> ENTRIES = List.of(
            "Subject drinks fermented grain, then challenges a cactus to a duel. The cactus wins. Subject apologises to it.",
            "The specimens greet each other by touching their hats. Removing a specimen's hat causes great distress. Do not remove the hats again.",
            "Specimens trade small metal discs for beans. Value of the discs: unknown. Value of the beans: also unknown.",
            "Observed a specimen load its weapon. This took forty heartbeats. The weapon then failed to fire because of rain. They hold this whole continent somehow.",
            "Took specimen 2231 for study and returned it after one night. It told the others. Nobody believed it. Very convenient.",
            "The specimens keep a small four-legged animal, the \"cat\", in the house of law. The cat does not appear to serve them. They do not seem to mind.",
            "Each seventh day the specimens sit in rows and sing at the sky. We went to look. They were not singing at us. Disappointing.",
            "A specimen pointed at our craft and shouted \"airship\". There is no air in our ship. Observer corrected it. It fainted.",
            "Specimens put their dead in the ground and carve a joke above them. Purpose unclear. Observer 7 laughed. Observer 7 has been reprimanded.",
            "Specimen 4471 will not stop saying \"yeehaw\". Procedure abandoned. Specimen returned with tag. Specimen still saying it.",
            "The large horned food-animals do not like us. We cannot think why. We have only taken a few of their organs.",
            "In the drinking house one specimen strikes a box of wires and hammers. When weapons come out, it stops. When they are put away, it starts again. The most orderly thing on the planet.",
            "We pressed a perfect circle into a wheat field to test their mathematics. They blamed \"kids\". Tested again. They blamed kids again. Their young must be remarkable.",
            "The specimen in the dark coat asked for long life in return for his silence. He is very bad at silence. He tells the barmaid everything.",
            "Specimens crave a bitter black liquid called coffee. One drank eleven cups and spotted us from a mile off. Recommend this planet be kept away from coffee.",
            "Attempted to blend in at a drinking house. Wore a hat. Ordered \"one beverage\". The barmaid called us \"sugar\". Unsure if discovered.");

    public static String titleKey() {
        return "book.hollowfrontier.field_notes.title";
    }

    /** The lang key of entry {@code i} (0-based). */
    public static String entryKey(int i) {
        return "book.hollowfrontier.field_notes." + (i + 1);
    }

    /**
     * Which entries copy {@code variant} holds, in order: {@link #PER_BOOK} different ones. Between them the copies hold
     * every entry twice, never twice in one copy.
     */
    public static List<Integer> entries(int variant) {
        int half = ENTRIES.size() / 2;
        return List.of(variant, (variant + 3) % half + half, (variant + 5) % half, variant + half);
    }
}
