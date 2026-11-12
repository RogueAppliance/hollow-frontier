package com.hollowfrontier.content;

import net.minecraft.tags.BlockTags;
import net.minecraft.util.Mth;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.common.Tags;

/**
 * The numbers behind what riding, flying and standing near something very large feel like (46b, 46c). The client
 * classes that play it ({@code client/CameraFeel}, {@code client/WorldFeelClient}) read everything from here, so game
 * tests can hold it. The guns' numbers are in {@code content/frontier/gun/GunFeel}.
 */
public final class WorldFeel {
    private WorldFeel() {}

    // ---- the camera ----

    /** The most a shake can be, whatever is going on at once. */
    public static final float MAX_SHAKE = 1.0F;
    /** Degrees the view moves at a full shake. */
    public static final float SHAKE_DEGREES = 1.6F;
    /** A shake fades by this much a tick: a full one is gone in a second. */
    public static final float SHAKE_FADE = 0.05F;

    /** How hard something {@code distance} blocks off shakes the view: {@code base} beside it, nothing at {@code range} and beyond. */
    public static float shake(float base, double distance, double range) {
        if (distance >= range) {
            return 0.0F;
        }
        float near = (float) (1.0 - Math.max(0.0, distance) / range);
        return base * near * near;
    }

    /** Two shakes at once: they add, up to {@link #MAX_SHAKE}. */
    public static float add(float shake, float more) {
        return Mth.clamp(shake + more, 0.0F, MAX_SHAKE);
    }

    // ---- hooves ----

    /** Blocks a tick, over the ground, at which a horse is galloping. */
    public static final double GALLOP = 0.22;
    /** Blocks between one fall of the hooves and the next. */
    public static final double STRIDE = 1.7;

    public enum Ground {
        /** Sand, gravel, dry dirt, a path, terracotta: a cloud. */
        DUSTY(5, false),
        /** Earth: some. */
        EARTH(2, false),
        /** Grass: a little. */
        GRASS(1, false),
        /** Snow: a puff of it. */
        SNOW(3, false),
        /** Stone, brick, metal: no dust, and a shod hoof rings. */
        HARD(0, true),
        /** Boards and anything else: nothing. */
        OTHER(0, false);

        private final int dust;
        private final boolean rings;

        Ground(int dust, boolean rings) {
            this.dust = dust;
            this.rings = rings;
        }

        /** Whether a horseshoe rings on it. */
        public boolean rings() {
            return rings;
        }
    }

    public static Ground ground(BlockState state) {
        if (state.is(Blocks.GRASS_BLOCK)) {
            return Ground.GRASS;
        } else if (state.is(BlockTags.SNOW)) {
            return Ground.SNOW;
        } else if (state.is(BlockTags.SAND) || state.is(Tags.BlockItems.GRAVELS.block()) || state.is(BlockTags.TERRACOTTA)
                || state.is(Blocks.COARSE_DIRT) || state.is(Blocks.DIRT_PATH)) {
            return Ground.DUSTY;
        } else if (state.is(BlockTags.DIRT)) {
            return Ground.EARTH;
        } else if (state.is(BlockTags.MINEABLE_WITH_PICKAXE)) {
            return Ground.HARD;
        }
        return Ground.OTHER;
    }

    /** Particles a stride kicks up from this ground. Rain lays the dust: none from grass or earth, half from the rest. */
    public static int dust(BlockState ground, boolean raining) {
        Ground kind = ground(ground);
        if (raining) {
            return kind == Ground.DUSTY ? kind.dust / 2 : 0;
        }
        return kind.dust;
    }

    // ---- the airship ----

    /** Blocks a tick at which the wind is as loud as speed makes it, and the height at which height does. */
    public static final double WIND_FULL_SPEED = 0.6;
    public static final double WIND_FROM_Y = 80.0, WIND_FULL_Y = 220.0;
    /** Where the Overworld's clouds are, and how far either side of them wisps go past the deck. */
    public static final double CLOUD_Y = 192.0, CLOUD_BAND = 6.0;
    /** A turn of this many degrees a tick, or a climb of this many blocks a tick, makes the envelope creak. */
    public static final float CREAK_TURN = 2.5F;
    public static final double CREAK_CLIMB = 0.12;
    /** The most the deck leans the view, in degrees. */
    public static final float MAX_SWAY = 2.5F;

    /** How loud the wind is at this speed and height, 0 to 1: mostly speed, some height, and never quite nothing aloft. */
    public static float wind(double speed, double y) {
        float fromSpeed = (float) Mth.clamp(speed / WIND_FULL_SPEED, 0.0, 1.0);
        float fromHeight = (float) Mth.clamp((y - WIND_FROM_Y) / (WIND_FULL_Y - WIND_FROM_Y), 0.0, 1.0);
        return Mth.clamp(0.65F * fromSpeed + 0.35F * fromHeight, 0.0F, 1.0F);
    }

    /** How far the deck leans the view for a turn of {@code degreesPerTick}: into the turn, up to {@link #MAX_SWAY}. */
    public static float sway(float degreesPerTick) {
        return Mth.clamp(degreesPerTick * 0.6F, -MAX_SWAY, MAX_SWAY);
    }

    public static boolean inCloud(double y) {
        return Math.abs(y - CLOUD_Y) <= CLOUD_BAND;
    }

    // ---- the handcar ----

    /** Blocks between rail joints, and the loss of speed in a tick that counts as braking. */
    public static final double RAIL_JOINT = 2.5;
    public static final double BRAKING = 0.012;

    // ---- the big things ----

    /** A beast's tread: blocks between footfalls, the shake beside it, and how far off it's felt. */
    public record Tread(double stride, float shake, double range) {}

    public static final Tread MAMMOTH = new Tread(2.2, 0.26F, 14.0);
    public static final Tread MEGATHERIUM = new Tread(1.9, 0.22F, 12.0);
    public static final Tread GLYPTODON = new Tread(1.4, 0.14F, 9.0);
    /** At a charge (this many blocks a tick or more) a footfall is this much harder. */
    public static final double CHARGE = 0.2;
    public static final float CHARGE_HARDER = 1.6F;

    public static float footfall(Tread tread, double speed, double distance) {
        return shake(tread.shake() * (speed >= CHARGE ? CHARGE_HARDER : 1.0F), distance, tread.range());
    }

    /** Blasts: the shake beside them and how far off they're felt. */
    public record Blast(float shake, double range) {}

    public static final Blast DYNAMITE = new Blast(0.8F, 24.0);
    public static final Blast VAULT = new Blast(0.9F, 32.0);
    public static final Blast DEATH_RAY = new Blast(1.0F, 48.0);
    // 50: aboard a Gray ship. Its hull giving way, and a pod meeting the ground with you in it
    public static final Blast HULL_GROAN = new Blast(0.45F, 16.0);
    public static final Blast POD_CRASH = new Blast(1.0F, 40.0);
    /** A falling ship's deck: how far it leans the view, how slowly it rolls, and the tremor held under it. */
    public static final float FALL_LEAN = 2.2F, FALL_ROLL = 0.045F, FALL_TREMOR = 0.12F;

    /** The lean of a falling ship's deck at {@code tick}, growing as the time left ({@code 0..1} of it gone) runs out. */
    public static float fallLean(int tick, float gone) {
        return Mth.sin(tick * FALL_ROLL) * FALL_LEAN * (0.4F + 0.6F * Mth.clamp(gone, 0.0F, 1.0F));
    }

    /** A Basilosaurus ramming the submersible you're in. */
    public static final float RAM = 0.7F;
    /** The Mothership coming in: a tremor held at this level under it, felt out to this range. */
    public static final float TREMOR = 0.3F;
    public static final double TREMOR_RANGE = 96.0;

    public static float blast(Blast blast, double distance) {
        return shake(blast.shake(), distance, blast.range());
    }
}
