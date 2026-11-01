import java.io.File;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Writes the GeckoLib geo model for the Prototype Saucer (13c; a code "blueprint", like the airship and the guns).
 * Run from the project root:  java tools/models/SaucerModels.java src/main/resources/assets/hollowfrontier/geckolib/models/entity
 *
 * Model space is Bedrock/Blockbench pixels with the entity's feet at the origin, +Y up and the front toward -Z (north).
 * 16 px = 1 block, drawn at full size: a 60 px (3.75 block) disc a block deep, under a 34 px glass dome.
 * <ul>
 * <li><b>The disc</b> is seven stacked layers, each a regular 24-gon made of twelve thin bars turned 15 degrees apart,
 *     each bar's ends on the circle ({@link Bone#disc}). (Squares turned the same way make a star: their corners stick
 *     out, as a runPhoto showed.) A bar's two end faces are two whole edges of the polygon, so they carry the side detail;
 *     its long sides are inside the disc and aren't drawn. The bars share their top and bottom faces' plane, so those are
 *     mapped to flat-colour swatches (whichever bar wins a pixel, it's the same colour). The rim layer is its own bone, so
 *     the animation can spin its lights.</li>
 * <li><b>The dome</b> is a 16-sided faceted half-ellipsoid (radius 17, height 20): in each of 16 bones (one per facet,
 *     turned 22.5 degrees apart) six flat glass panes tilted inward, one per tier, and a brass rib over each seam (16
 *     more bones, turned half a step) to hide where the panes meet. A brass cap on top; the nav mast and its ball stand on
 *     it. The panes come last in the file so they're drawn over what's inside.</li>
 * <li><b>Underneath</b>, the gravity drive (a glowing ring round a dark hub), the beam emitter in the hub, and the lens
 *     array's turret at the front with its orange eye. In the cockpit, a console with a glowing top.</li>
 * </ul>
 * Every face maps onto a swatch of the 64x64 texture painted by tools/textures/TextureArt.java (Ufo.prototype; the
 * swatch rectangles must match).
 */
public class SaucerModels {
    static final int TEX_W = 64, TEX_H = 64;

    // Swatch rectangles {u, v, w, h}; must match TextureArt.Ufo.prototype.
    enum Mat {
        ALLOY_HI(0, 0, 4, 4), ALLOY(4, 0, 4, 4), ALLOY_DK(8, 0, 4, 4), UNDER(12, 0, 4, 4), BRASS(16, 0, 4, 4), BRASS_DK(20, 0, 4, 4),
        DRIVE(24, 0, 4, 4), EMITTER(28, 0, 4, 4), LENS(32, 0, 4, 4), EYE(36, 0, 4, 4), NAV(40, 0, 4, 4), CONSOLE(44, 0, 4, 4),
        SCREEN(48, 0, 4, 4), UNDERGLOW(52, 0, 4, 4), // the last: the Mothership's dim underside glow (13d)
        // Side strips, one per disc layer (a face uses as much of its strip as it's wide: under 8 px for every layer)
        S_BELLY(0, 8, 8, 2), S_L2(8, 8, 8, 2), S_L3(16, 8, 8, 2), S_RIM(24, 8, 8, 4), S_U1(32, 8, 8, 2), S_U2(40, 8, 8, 2),
        S_DECK(48, 8, 8, 2),
        S_DOME(56, 8, 8, 4), // the Mothership's dome, its windows lit (13d)
        GLASS(0, 32, 8, 12);

        final int u, v, w, h;

        Mat(int u, int v, int w, int h) {
            this.u = u; this.v = v; this.w = w; this.h = h;
        }
    }

    enum Face { NORTH, SOUTH, EAST, WEST, UP, DOWN }

    record Cube(double[] origin, double[] size, double[] pivot, double[] rotation, Map<Face, Mat> faces) {}

    static final class Bone {
        final String name, parent;
        final double[] pivot;
        double[] rotation;
        final List<Cube> cubes = new ArrayList<>();

        Bone(String name, String parent, double... pivot) {
            this.name = name;
            this.parent = parent;
            this.pivot = pivot;
        }

        Bone turned(double yaw) {
            rotation = new double[] {0, yaw, 0};
            return this;
        }

        /** A box with one material all round. */
        Bone box(Mat mat, double x0, double y0, double z0, double x1, double y1, double z1) {
            return box(all(mat), x0, y0, z0, x1, y1, z1);
        }

        Bone box(Map<Face, Mat> faces, double x0, double y0, double z0, double x1, double y1, double z1) {
            cubes.add(new Cube(new double[] {x0, y0, z0}, new double[] {x1 - x0, y1 - y0, z1 - z0}, null, null, faces));
            return this;
        }

        /**
         * A round layer from y0 to y1: a regular polygon of {@code sides} sides with its corners on a circle {@code d}
         * across, made of sides/2 bars turned 360/sides degrees apart. Each bar's ends span one side's angle (360/sides
         * degrees) and sit on the circle, so the bars' ends are the polygon's sides, edge to edge, and the bars fill it.
         */
        Bone disc(double d, double y0, double y1, int sides, Mat side, Mat top, Mat bottom) {
            Map<Face, Mat> faces = new EnumMap<>(Face.class);
            faces.put(Face.NORTH, side);
            faces.put(Face.SOUTH, side);
            faces.put(Face.UP, top);
            faces.put(Face.DOWN, bottom);
            double half = Math.PI / sides, along = d / 2 * Math.cos(half), across = d / 2 * Math.sin(half);
            for (int i = 0; i < sides / 2; i++) {
                double yaw = 360.0 * i / sides;
                cubes.add(new Cube(new double[] {-across, y0, -along}, new double[] {2 * across, y1 - y0, 2 * along}, new double[] {0, y0, 0},
                        yaw == 0 ? null : new double[] {0, yaw, 0}, faces));
            }
            return this;
        }

        /**
         * A flat piece on this bone's +Z side running from (z0, y0) up to (z1, y1) (z the distance out from the middle), w
         * wide and t thick, tilted inward about its foot.
         */
        Bone slat(Map<Face, Mat> faces, double w, double t, double z0, double y0, double z1, double y1) {
            double len = Math.hypot(z0 - z1, y1 - y0);
            double tilt = Math.toDegrees(Math.atan2(z0 - z1, y1 - y0));
            cubes.add(new Cube(new double[] {-w / 2, y0, z0 - t / 2}, new double[] {w, len, t}, new double[] {0, y0, z0},
                    new double[] {tilt, 0, 0}, faces));
            return this;
        }
    }

    static Map<Face, Mat> all(Mat m) {
        Map<Face, Mat> faces = new EnumMap<>(Face.class);
        for (Face f : Face.values()) faces.put(f, m);
        return faces;
    }

    public static void main(String[] args) throws Exception {
        File dir = new File(args.length > 0 ? args[0] : "src/main/resources/assets/hollowfrontier/geckolib/models/entity");
        dir.mkdirs();
        write(new File(dir, "prototype_saucer.geo.json"), "prototype_saucer", saucer(), 5, 4, 1.5);
        write(new File(dir, "mothership.geo.json"), "mothership", mothership(), 14, 4, 1);
        System.out.println("SaucerModels: wrote the Prototype Saucer and the Mothership to " + dir);
    }

    static final double DECK = 16, DOME_R = 17, DOME_H = 20;
    static final int FACETS = 16;
    // The dome's profile: elevations (degrees) where its six tiers of panes meet. A pane is a rectangle on a tapering facet,
    // as wide as the facet halfway up, so it overhangs its neighbours a little at the top; with three tiers the overhang
    // showed from the cockpit as pale bands (runPhoto), with six it's under half a rib's width each side.
    static final double[] TIERS = {0, 15, 30, 45, 60, 72, 84};
    static final double RIB = 0.7;

    static List<Bone> saucer() {
        List<Bone> bones = new ArrayList<>();
        Bone root = new Bone("root", null, 0, 0, 0);
        bones.add(root);

        // The disc: a belly, two skirts flaring to the rim, two narrowing above it, and the brass deck the dome sits on
        bones.add(new Bone("hull", "root", 0, 0, 0)
                .disc(28, 0, 2, 24, Mat.S_BELLY, Mat.UNDER, Mat.UNDER)
                .disc(44, 2, 4, 24, Mat.S_L2, Mat.ALLOY_DK, Mat.ALLOY_DK)
                .disc(54, 4, 6, 24, Mat.S_L3, Mat.ALLOY, Mat.ALLOY)
                .disc(54, 10, 12, 24, Mat.S_U1, Mat.ALLOY_HI, Mat.ALLOY_DK)
                .disc(46, 12, 14, 24, Mat.S_U2, Mat.ALLOY, Mat.ALLOY)
                .disc(38, 14, DECK, 24, Mat.S_DECK, Mat.BRASS_DK, Mat.BRASS_DK)); // dark underfoot, so the glass reads clear
        bones.add(new Bone("rim", "root", 0, 8, 0).disc(60, 6, 10, 24, Mat.S_RIM, Mat.BRASS, Mat.BRASS_DK));

        // Underneath: the drive's glowing ring round a dark hub; the emitter in the hub; the lens turret at the front
        bones.add(new Bone("drive", "root", 0, 0, 0)
                .disc(20, -0.5, 0, 12, Mat.DRIVE, Mat.DRIVE, Mat.DRIVE)
                .disc(12, -0.8, -0.5, 12, Mat.UNDER, Mat.UNDER, Mat.UNDER));
        bones.add(new Bone("emitter", "root", 0, -1, 0).disc(8, -1.6, -0.8, 12, Mat.EMITTER, Mat.EMITTER, Mat.EMITTER));
        Map<Face, Mat> barrel = all(Mat.LENS);
        barrel.put(Face.NORTH, Mat.EYE);
        bones.add(new Bone("lens", "root", 0, -1, -10)
                .box(Mat.LENS, -2, -2.5, -12, 2, 0, -8)
                .box(barrel, -0.75, -2, -15, 0.75, -0.5, -12));

        // The cockpit: a console before the pilot, its top aglow
        Map<Face, Mat> console = all(Mat.CONSOLE);
        console.put(Face.UP, Mat.SCREEN);
        bones.add(new Bone("cockpit", "root", 0, DECK, 0).box(console, -4, DECK, -14, 4, DECK + 3, -11));

        // The dome's profile points (distance out, height above the deck)
        double[][] p = new double[TIERS.length][];
        for (int i = 0; i < TIERS.length; i++) {
            double a = Math.toRadians(TIERS[i]);
            p[i] = new double[] {DOME_R * Math.cos(a), DOME_H * Math.sin(a)};
        }
        double half = Math.tan(Math.PI / FACETS); // half a facet's width per unit out
        double top = DECK + p[TIERS.length - 1][1];

        // A brass cap on top, and the nav mast and its ball on that
        Bone dome = new Bone("dome", "root", 0, DECK, 0).disc(2 * p[TIERS.length - 1][0] + 0.6, top - 0.4, top + 0.4, 8, Mat.BRASS, Mat.BRASS, Mat.BRASS);
        bones.add(dome);
        bones.add(new Bone("nav", "root", 0, top, 0)
                .box(Mat.BRASS, -0.5, top + 0.4, -0.5, 0.5, top + 4, 0.5)
                .box(Mat.NAV, -1.25, top + 4, -1.25, 1.25, top + 6.5, 1.25));

        // A brass rib over each seam (the seams lie half a facet round, a little further out)
        double seam = 1.0 / Math.cos(Math.PI / FACETS);
        for (int k = 0; k < FACETS; k++) {
            Bone rib = new Bone("rib_" + k, "dome", 0, DECK, 0).turned(360.0 * k / FACETS + 180.0 / FACETS);
            for (int i = 0; i + 1 < TIERS.length; i++) {
                rib.slat(all(Mat.BRASS), RIB, 0.6, p[i][0] * seam, DECK + p[i][1], p[i + 1][0] * seam, DECK + p[i + 1][1]);
            }
            bones.add(rib);
        }
        // The glass: each pane as wide as its facet at mid-height, only its outward face drawn (the render type is no-cull)
        Map<Face, Mat> glass = new EnumMap<>(Face.class);
        glass.put(Face.SOUTH, Mat.GLASS);
        for (int k = 0; k < FACETS; k++) {
            Bone pane = new Bone("pane_" + k, "dome", 0, DECK, 0).turned(360.0 * k / FACETS);
            for (int i = 0; i + 1 < TIERS.length; i++) {
                double w = (p[i][0] + p[i + 1][0]) * half;
                pane.slat(glass, w, 0, p[i][0], DECK + p[i][1], p[i + 1][0], DECK + p[i + 1][1]);
            }
            bones.add(pane);
        }
        return bones;
    }

    /**
     * The Mothership (13d): the same stepped 24-gon build as the Prototype Saucer, twelve blocks across (192 px) and dark,
     * in the same swatch layout under a texture of its own (TextureArt.Ufo.mothership). A belly, a skirt flaring to a deep
     * rim (its own bone, so it turns, strung with violet lights), two tiers narrowing above, a deck and a three-tier dome
     * with lit windows and a red light on top. Underneath, the beam emitter, and belly doors over the core: the renderer
     * shows the doors, or the glowing core once it's exposed.
     */
    static List<Bone> mothership() {
        List<Bone> bones = new ArrayList<>();
        bones.add(new Bone("root", null, 0, 0, 0));
        bones.add(new Bone("hull", "root", 0, 0, 0)
                .disc(96, 0, 4, 24, Mat.S_BELLY, Mat.UNDER, Mat.UNDER)
                .disc(150, 4, 8, 24, Mat.S_L2, Mat.ALLOY_DK, Mat.UNDERGLOW)
                .disc(164, 14, 18, 24, Mat.S_U1, Mat.ALLOY, Mat.ALLOY_DK)
                .disc(124, 18, 22, 24, Mat.S_U2, Mat.ALLOY_DK, Mat.ALLOY_DK)
                .disc(84, 22, 25, 24, Mat.S_DECK, Mat.BRASS, Mat.BRASS_DK));
        bones.add(new Bone("rim", "root", 0, 11, 0).disc(192, 8, 14, 24, Mat.S_RIM, Mat.ALLOY_HI, Mat.UNDER));
        bones.add(new Bone("dome", "root", 0, 25, 0)
                .disc(64, 25, 31, 24, Mat.S_DOME, Mat.ALLOY_DK, Mat.ALLOY_DK)
                .disc(46, 31, 35, 24, Mat.S_U2, Mat.ALLOY_DK, Mat.ALLOY_DK)
                .disc(26, 35, 37, 12, Mat.S_U2, Mat.ALLOY_DK, Mat.ALLOY_DK)
                .disc(5, 37, 39, 8, Mat.NAV, Mat.NAV, Mat.NAV));
        bones.add(new Bone("doors", "root", 0, 0, 0).disc(56, -1, 0, 24, Mat.UNDER, Mat.UNDER, Mat.ALLOY_DK));
        bones.add(new Bone("core", "root", 0, -1, 0).disc(48, -3, 0, 24, Mat.EMITTER, Mat.EMITTER, Mat.EMITTER));
        bones.add(new Bone("emitter", "root", 0, -1, 0)
                .disc(20, -1.6, -1, 12, Mat.DRIVE, Mat.DRIVE, Mat.DRIVE)
                .disc(10, -3.4, -1.6, 12, Mat.EMITTER, Mat.EMITTER, Mat.EMITTER));
        return bones;
    }

    // ---------------------------------------------------------------- writing

    static void write(File file, String name, List<Bone> bones, double boundsWidth, double boundsHeight, double boundsOffset) throws Exception {
        StringBuilder sb = new StringBuilder();
        sb.append("{\n\t\"format_version\": \"1.12.0\",\n\t\"minecraft:geometry\": [\n\t\t{\n");
        sb.append("\t\t\t\"description\": {\n");
        sb.append("\t\t\t\t\"identifier\": \"geometry.hollowfrontier.").append(name).append("\",\n");
        sb.append("\t\t\t\t\"texture_width\": ").append(TEX_W).append(",\n");
        sb.append("\t\t\t\t\"texture_height\": ").append(TEX_H).append(",\n");
        sb.append(String.format(Locale.ROOT, "\t\t\t\t\"visible_bounds_width\": %s,\n\t\t\t\t\"visible_bounds_height\": %s,\n",
                vec(boundsWidth).replace("[", "").replace("]", ""), vec(boundsHeight).replace("[", "").replace("]", "")));
        sb.append("\t\t\t\t\"visible_bounds_offset\": ").append(vec(0, boundsOffset, 0)).append("\n\t\t\t},\n");
        sb.append("\t\t\t\"bones\": [\n");
        for (int b = 0; b < bones.size(); b++) {
            Bone bone = bones.get(b);
            sb.append("\t\t\t\t{\n\t\t\t\t\t\"name\": \"").append(bone.name).append("\",\n");
            if (bone.parent != null) {
                sb.append("\t\t\t\t\t\"parent\": \"").append(bone.parent).append("\",\n");
            }
            sb.append("\t\t\t\t\t\"pivot\": ").append(vec(bone.pivot));
            if (bone.rotation != null) {
                sb.append(",\n\t\t\t\t\t\"rotation\": ").append(vec(bone.rotation));
            }
            if (!bone.cubes.isEmpty()) {
                sb.append(",\n\t\t\t\t\t\"cubes\": [\n");
                for (int c = 0; c < bone.cubes.size(); c++) {
                    sb.append("\t\t\t\t\t\t").append(cube(bone.cubes.get(c)));
                    sb.append(c + 1 < bone.cubes.size() ? ",\n" : "\n");
                }
                sb.append("\t\t\t\t\t]");
            }
            sb.append("\n\t\t\t\t}").append(b + 1 < bones.size() ? ",\n" : "\n");
        }
        sb.append("\t\t\t]\n\t\t}\n\t]\n}\n");
        Files.writeString(file.toPath(), sb.toString());
    }

    static String cube(Cube c) {
        double w = c.size[0], h = c.size[1], d = c.size[2];
        StringBuilder sb = new StringBuilder("{\"origin\": ").append(vec(c.origin)).append(", \"size\": ").append(vec(c.size));
        if (c.rotation != null) {
            sb.append(", \"pivot\": ").append(vec(c.pivot)).append(", \"rotation\": ").append(vec(c.rotation));
        }
        sb.append(", \"uv\": {");
        List<String> faces = new ArrayList<>();
        for (Map.Entry<Face, Mat> e : c.faces.entrySet()) {
            double fu = switch (e.getKey()) { case EAST, WEST -> d; default -> w; };
            double fv = switch (e.getKey()) { case UP, DOWN -> d; default -> h; };
            faces.add("\"" + e.getKey().name().toLowerCase(Locale.ROOT) + "\": " + face(e.getValue(), fu, fv));
        }
        return sb.append(String.join(", ", faces)).append("}}").toString();
    }

    // The swatch, or as much of it as the face has pixels (so a small face isn't a blur of the whole swatch).
    static String face(Mat m, double fu, double fv) {
        double uw = Math.min(m.w, Math.max(1, fu));
        double vh = Math.min(m.h, Math.max(1, fv));
        return "{\"uv\": " + vec(m.u, m.v) + ", \"uv_size\": " + vec(uw, vh) + "}";
    }

    static String vec(double... v) {
        StringBuilder sb = new StringBuilder("[");
        for (int i = 0; i < v.length; i++) {
            if (i > 0) sb.append(", ");
            double r = Math.round(v[i] * 1000.0) / 1000.0;
            sb.append(r == Math.rint(r) ? String.valueOf((long) r) : String.format(Locale.ROOT, "%.3f", r).replaceAll("0+$", ""));
        }
        return sb.append("]").toString();
    }
}
