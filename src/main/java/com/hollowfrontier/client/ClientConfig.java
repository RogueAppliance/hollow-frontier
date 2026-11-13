package com.hollowfrontier.client;

import net.neoforged.neoforge.common.ModConfigSpec;

/**
 * The player's own settings (ModConfig.Type.CLIENT, hollowfrontier-client.toml in the config folder): nothing a server
 * has a say in. Before the config loads, the defaults apply.
 */
public final class ClientConfig {
    private ClientConfig() {}

    public static final ModConfigSpec SPEC;

    private static final ModConfigSpec.BooleanValue HOLLOW_SCORE;
    private static final ModConfigSpec.DoubleValue CAMERA_KICK;
    private static final ModConfigSpec.DoubleValue CAMERA_SHAKE;
    private static final ModConfigSpec.BooleanValue JOURNAL_NOTES;

    static {
        ModConfigSpec.Builder b = new ModConfigSpec.Builder();
        b.comment("The Hollow Earth's score").push("score");
        HOLLOW_SCORE = b.comment("Whether the Hollow Earth plays its own music. Off puts back the vanilla biome music it had before 1.4.0 "
                + "(for players and packs with music of their own)").define("hollowScore", true);
        b.pop();
        b.comment("Camera movement (46). If a moving view makes you unwell, set both to 0: nothing else changes").push("camera");
        CAMERA_KICK = b.comment("How hard firing a gun kicks the view, from 0 (not at all) to 1. It never moves your aim: the view comes back to where it was")
                .defineInRange("cameraKick", 1.0, 0.0, 1.0);
        CAMERA_SHAKE = b.comment("How much the view shakes for heavy footfalls, blasts and the Mothership, and leans on an airship's deck, from 0 (not at all) to 1")
                .defineInRange("cameraShake", 1.0, 0.0, 1.0);
        b.pop();
        b.comment("The Prospector's Journal (48)").push("journal");
        JOURNAL_NOTES = b.comment("Whether a line over the hotbar and a pencil scratch say so when something is written into the journal you carry")
                .define("journalNotes", true);
        b.pop();
        SPEC = b.build();
    }

    public static double cameraKick() { return SPEC.isLoaded() ? CAMERA_KICK.getAsDouble() : CAMERA_KICK.getDefault(); }
    public static double cameraShake() { return SPEC.isLoaded() ? CAMERA_SHAKE.getAsDouble() : CAMERA_SHAKE.getDefault(); }
    public static boolean journalNotes() { return SPEC.isLoaded() ? JOURNAL_NOTES.getAsBoolean() : JOURNAL_NOTES.getDefault(); }
    public static boolean hollowScore() { return SPEC.isLoaded() ? HOLLOW_SCORE.getAsBoolean() : HOLLOW_SCORE.getDefault(); }
}
