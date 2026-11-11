import java.io.File;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Writes the GeckoLib geo model for a town's December tree (35b; a code "blueprint", like the airship and the saucer).
 * Run from the project root:  java tools/models/TreeModels.java src/main/resources/assets/hollowfrontier/geckolib/models/entity
 *
 * Model space is Bedrock/Blockbench pixels, the foot of the tree at the origin, +Y up, 16 px = 1 block. A tub and a short
 * trunk, then eight tiers of boughs, each a little smaller than the one below and every other one turned 45 degrees, so
 * the corners of each stand out past the sides of the next and the tree reads as layered spruce rather than a pyramid.
 * A candle stands on each corner of the five lowest tiers, a tin star hangs on each face of them, and a bigger star
 * stands on top: about six and three quarter blocks in all.
 *
 * Every face is mapped per face onto the 96x64 texture painted by tools/textures/TextureArt.java (December there; the
 * rectangles must match). The candles' flames are all in the "flames" bone, which the renderer draws only after dark, and
 * are the only thing in the glow mask.
 */
public class TreeModels {
    static final int TEX_W = 96, TEX_H = 64;
    // Texture rectangles (u, v); must match TextureArt.December.
    static final int[] BOUGH_SIDE = {0, 0}, BOUGH_TOP = {0, 16}, BARK = {48, 0}, TUB_SIDE = {56, 0}, TUB_TOP = {56, 8},
            CANDLE = {72, 0}, FLAME = {76, 0}, TIN_STAR = {80, 0}, TOP_STAR = {84, 0};
    static final int BOUGH_STRIP = 14; // the bough strip's height: a tier's side shows its bottom rows, ragged edge and all

    /** Tiers of boughs: {foot y, half width, height}. Each overlaps the one below by two rows (the ragged fringe). */
    static final int[][] TIERS = {{18, 22, 14}, {30, 19, 14}, {42, 16, 14}, {54, 13, 12}, {64, 10, 12}, {74, 7, 12}, {84, 4, 10}, {92, 2, 8}};
    static final int DRESSED = 5; // how many tiers, from the bottom, carry candles and tin stars

    public static void main(String[] args) throws Exception {
        File dir = new File(args.length > 0 ? args[0] : "src/main/resources/assets/hollowfrontier/geckolib/models/entity");
        List<String> bones = new ArrayList<>();
        bones.add(bone("root", null, 0, 0, 0, 0, List.of()));
        bones.add(bone("tub", "root", 0, 0, 0, 0, List.of(
                box(-6, 0, -6, 12, 8, 12, TUB_SIDE, 12, 8, TUB_TOP, 12, 12),
                box(-2, 8, -2, 4, 12, 4, BARK, 4, 12, BARK, 4, 4))));
        List<String> flames = new ArrayList<>();
        for (int t = 0; t < TIERS.length; t++) {
            int y = TIERS[t][0], half = TIERS[t][1], h = TIERS[t][2], w = half * 2;
            boolean turned = t % 2 == 1;
            List<String> cubes = new ArrayList<>();
            cubes.add("{\"origin\": " + vec(-half, y, -half) + ", \"size\": " + vec(w, h, w) + ", \"uv\": {"
                    + side("north", BOUGH_SIDE[0], BOUGH_SIDE[1] + BOUGH_STRIP - h, w, h) + ", "
                    + side("south", BOUGH_SIDE[0], BOUGH_SIDE[1] + BOUGH_STRIP - h, w, h) + ", "
                    + side("east", BOUGH_SIDE[0], BOUGH_SIDE[1] + BOUGH_STRIP - h, w, h) + ", "
                    + side("west", BOUGH_SIDE[0], BOUGH_SIDE[1] + BOUGH_STRIP - h, w, h) + ", "
                    + side("up", BOUGH_TOP[0], BOUGH_TOP[1], w, w) + ", "
                    + side("down", BOUGH_TOP[0], BOUGH_TOP[1], w, w) + "}}");
            if (t < DRESSED) {
                double top = y + h;
                double at = half - 2.5; // a candle's corner, in from the tier's own
                for (int sx = -1; sx <= 1; sx += 2) {
                    for (int sz = -1; sz <= 1; sz += 2) {
                        double cx = sx * at, cz = sz * at;
                        cubes.add(box(cx - 0.5, top, cz - 0.5, 1, 3, 1, CANDLE, 1, 3, CANDLE, 1, 1));
                        // The flame goes in the flames bone, which isn't turned: put it where the turned candle ends up
                        double fx = cx, fz = cz;
                        if (turned) {
                            double r = Math.sqrt(2) * at;
                            fx = sx == sz ? sx * r : 0;
                            fz = sx == sz ? 0 : sz * r;
                        }
                        flames.add(box(fx - 0.5, top + 3, fz - 0.5, 1, 2, 1, FLAME, 1, 2, FLAME, 1, 1));
                    }
                }
                // A tin star on each face, hung a little way down it
                double sy = y + h - 7, out = half + 0.2;
                cubes.add(plane(-1.5, sy, -out, 3, 3, 0, TIN_STAR, "north", "south"));
                cubes.add(plane(-1.5, sy, out, 3, 3, 0, TIN_STAR, "north", "south"));
                cubes.add(plane(-out, sy, -1.5, 0, 3, 3, TIN_STAR, "east", "west"));
                cubes.add(plane(out, sy, -1.5, 0, 3, 3, TIN_STAR, "east", "west"));
            }
            bones.add(bone("tier_" + t, "root", 0, y, 0, turned ? 45 : 0, cubes));
        }
        int crown = TIERS[TIERS.length - 1][0] + TIERS[TIERS.length - 1][2];
        bones.add(bone("star", "root", 0, crown, 0, 0, List.of(
                "{\"origin\": " + vec(-4, crown - 1, 0) + ", \"size\": " + vec(8, 8, 0) + ", \"uv\": {"
                        + side("north", TOP_STAR[0], TOP_STAR[1], 8, 8) + ", " + side("south", TOP_STAR[0], TOP_STAR[1], 8, 8) + "}}",
                "{\"origin\": " + vec(0, crown - 1, -4) + ", \"size\": " + vec(0, 8, 8) + ", \"uv\": {"
                        + side("east", TOP_STAR[0], TOP_STAR[1], 8, 8) + ", " + side("west", TOP_STAR[0], TOP_STAR[1], 8, 8) + "}}")));
        bones.add(bone("flames", "root", 0, 0, 0, 0, flames));

        StringBuilder json = new StringBuilder();
        json.append("{\n\t\"format_version\": \"1.12.0\",\n\t\"minecraft:geometry\": [\n\t\t{\n\t\t\t\"description\": {\n")
                .append("\t\t\t\t\"identifier\": \"geometry.hollowfrontier.town_tree\",\n")
                .append("\t\t\t\t\"texture_width\": ").append(TEX_W).append(",\n\t\t\t\t\"texture_height\": ").append(TEX_H).append(",\n")
                .append("\t\t\t\t\"visible_bounds_width\": 5,\n\t\t\t\t\"visible_bounds_height\": 8,\n\t\t\t\t\"visible_bounds_offset\": [0, 3.5, 0]\n")
                .append("\t\t\t},\n\t\t\t\"bones\": [\n");
        json.append(String.join(",\n", bones)).append("\n\t\t\t]\n\t\t}\n\t]\n}\n");
        File out = new File(dir, "town_tree.geo.json");
        Files.writeString(out.toPath(), json.toString());
        System.out.println("TreeModels: " + bones.size() + " bones, " + flames.size() + " candles -> " + out);
    }

    static String bone(String name, String parent, double px, double py, double pz, double turn, List<String> cubes) {
        StringBuilder s = new StringBuilder("\t\t\t\t{\n\t\t\t\t\t\"name\": \"" + name + "\",\n");
        if (parent != null) s.append("\t\t\t\t\t\"parent\": \"").append(parent).append("\",\n");
        s.append("\t\t\t\t\t\"pivot\": ").append(vec(px, py, pz));
        if (turn != 0) s.append(",\n\t\t\t\t\t\"rotation\": ").append(vec(0, turn, 0));
        if (!cubes.isEmpty()) {
            s.append(",\n\t\t\t\t\t\"cubes\": [\n");
            for (int i = 0; i < cubes.size(); i++) {
                s.append("\t\t\t\t\t\t").append(cubes.get(i)).append(i + 1 < cubes.size() ? ",\n" : "\n");
            }
            s.append("\t\t\t\t\t]");
        }
        return s.append("\n\t\t\t\t}").toString();
    }

    /** A box with one rectangle for its four sides and another for its top and bottom. */
    static String box(double x, double y, double z, double w, double h, double d, int[] sides, int sw, int sh, int[] ends, int ew, int eh) {
        return "{\"origin\": " + vec(x, y, z) + ", \"size\": " + vec(w, h, d) + ", \"uv\": {"
                + side("north", sides[0], sides[1], sw, sh) + ", " + side("south", sides[0], sides[1], sw, sh) + ", "
                + side("east", sides[0], sides[1], sw, sh) + ", " + side("west", sides[0], sides[1], sw, sh) + ", "
                + side("up", ends[0], ends[1], ew, eh) + ", " + side("down", ends[0], ends[1], ew, eh) + "}}";
    }

    /** A flat, two-sided card three pixels square (a tin star). */
    static String plane(double x, double y, double z, double w, double h, double d, int[] uv, String front, String back) {
        return "{\"origin\": " + vec(x, y, z) + ", \"size\": " + vec(w, h, d) + ", \"uv\": {"
                + side(front, uv[0], uv[1], 3, 3) + ", " + side(back, uv[0], uv[1], 3, 3) + "}}";
    }

    static String side(String face, int u, int v, int w, int h) {
        return "\"" + face + "\": {\"uv\": " + vec(u, v) + ", \"uv_size\": " + vec(w, h) + "}";
    }

    static String vec(double... v) {
        StringBuilder s = new StringBuilder("[");
        for (int i = 0; i < v.length; i++) {
            if (i > 0) s.append(", ");
            s.append(v[i] == Math.rint(v[i]) ? String.valueOf((long) v[i]) : String.format(Locale.ROOT, "%.3f", v[i]));
        }
        return s.append("]").toString();
    }
}
