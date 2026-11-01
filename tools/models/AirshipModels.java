import java.io.File;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Writes the GeckoLib geo model for the airship (a code "blueprint", like the guns and structures).
 * Run from the project root:  java tools/models/AirshipModels.java src/main/resources/assets/hollowfrontier/geckolib/models/entity
 *
 * Model space is Bedrock/Blockbench pixels with the entity's feet at the origin, +Y up and the bow toward -Z (north), the
 * way a mob faces, so the ship heads where the entity faces. 16 px = 1 block: a 4 x 2 block gondola for two hung under a
 * 9-block canvas envelope, the boiler and its stack at the stern, the propeller behind it. The hit box (2.5 x 3 blocks)
 * covers the gondola and the bottom of the envelope.
 *
 * Every face is mapped onto a material swatch of the 128x64 texture painted by tools/textures/TextureArt.java (Airship
 * there; the swatch rectangles must match), stretched to the face at 1 texel per pixel at most. The animation
 * (geckolib/animations/entity/airship.animation.json) spins the "propeller" bone about Z.
 */
public class AirshipModels {
    static final int TEX_W = 128, TEX_H = 64;

    // Swatch rectangles {u, v, w, h}; must match TextureArt.Airship.
    enum Mat {
        CANVAS(0, 0, 32, 32),
        PANELS(32, 0, 64, 32),
        RED(96, 0, 16, 16),
        BRASS(112, 0, 16, 16),
        WOOD(96, 16, 32, 16),
        IRON(0, 32, 16, 16),
        ROPE(16, 32, 16, 16),
        WICKER(32, 32, 32, 16),
        PROP(64, 32, 16, 16),
        FIREBOX(80, 32, 16, 16);

        final int u, v, w, h;

        Mat(int u, int v, int w, int h) {
            this.u = u; this.v = v; this.w = w; this.h = h;
        }
    }

    record Cube(double x0, double y0, double z0, double x1, double y1, double z1, Mat mat, Mat front) {}

    static final class Bone {
        final String name, parent;
        final double[] pivot;
        final List<Cube> cubes = new ArrayList<>();

        Bone(String name, String parent, double... pivot) {
            this.name = name;
            this.parent = parent;
            this.pivot = pivot;
        }

        Bone box(Mat mat, double x0, double y0, double z0, double x1, double y1, double z1) {
            return box(mat, mat, x0, y0, z0, x1, y1, z1);
        }

        // A box whose stern-facing (south, +Z) face uses another material, e.g. the firebox door.
        Bone box(Mat mat, Mat front, double x0, double y0, double z0, double x1, double y1, double z1) {
            cubes.add(new Cube(x0, y0, z0, x1, y1, z1, mat, front));
            return this;
        }
    }

    public static void main(String[] args) throws Exception {
        File dir = new File(args.length > 0 ? args[0] : "src/main/resources/assets/hollowfrontier/geckolib/models/entity");
        dir.mkdirs();
        write(new File(dir, "airship.geo.json"), "airship", airship());
        System.out.println("AirshipModels: wrote the airship model to " + dir);
    }

    static List<Bone> airship() {
        Bone root = new Bone("root", null, 0, 0, 0);

        // Gondola: plank floor, wicker sides, brass rails, an iron keel, the helm at the bow.
        Bone gondola = new Bone("gondola", "root", 0, 0, 0)
                .box(Mat.IRON, -3, -2, -32, 3, 0, 32)
                .box(Mat.WOOD, -14, 0, -30, 14, 3, 30)
                .box(Mat.WICKER, -16, 0, -30, -14, 13, 30)
                .box(Mat.WICKER, 14, 0, -30, 16, 13, 30)
                .box(Mat.WICKER, -16, 0, -34, 16, 13, -30)
                .box(Mat.WICKER, -16, 0, 30, 16, 13, 34)
                .box(Mat.BRASS, -16.5, 13, -34.5, -14, 14, 34.5)
                .box(Mat.BRASS, 14, 13, -34.5, 16.5, 14, 34.5)
                .box(Mat.BRASS, -14, 13, -34.5, 14, 14, -32)
                .box(Mat.BRASS, -14, 13, 32, 14, 14, 34.5)
                .box(Mat.WOOD, -1, 3, -28, 1, 12, -26.5)          // helm post
                // The wheel: an open brass ring with a cross of spokes (a solid plate filled the pilot's view; runPhoto)
                .box(Mat.BRASS, -4, 18, -26.5, 4, 19, -25.5)
                .box(Mat.BRASS, -4, 11, -26.5, 4, 12, -25.5)
                .box(Mat.BRASS, -5, 12, -26.5, -4, 18, -25.5)
                .box(Mat.BRASS, 4, 12, -26.5, 5, 18, -25.5)
                .box(Mat.WOOD, -4, 14.5, -26.2, 4, 15.5, -25.8)
                .box(Mat.WOOD, -0.5, 12, -26.2, 0.5, 18, -25.8);

        // Boiler at the stern: a brass drum with an iron band, the firebox door facing aft, and the stack.
        Bone boiler = new Bone("boiler", "root", 0, 0, 0)
                .box(Mat.BRASS, -6, 3, 18, 6, 22, 28)
                .box(Mat.IRON, -6.5, 10, 17.5, 6.5, 12, 28.5)
                .box(Mat.IRON, Mat.FIREBOX, -4, 4, 28, 4, 10, 28.5)
                .box(Mat.IRON, -2, 22, 21, 2, 40, 25)
                .box(Mat.IRON, -3, 38, 20, 3, 40, 26);

        // Rigging: six ropes from the rails up to the envelope.
        Bone rigging = new Bone("rigging", "root", 0, 0, 0);
        for (double z : new double[] {-28, 0, 28}) {
            rigging.box(Mat.ROPE, -15.5, 14, z - 0.5, -14.5, 37, z + 0.5);
            rigging.box(Mat.ROPE, 14.5, 14, z - 0.5, 15.5, 37, z + 0.5);
        }

        // The envelope: a cigar of stacked boxes, two brass straps and a red stripe along each side.
        Bone envelope = new Bone("envelope", "root", 0, 54, 0)
                .box(Mat.PANELS, -20, 36, -50, 20, 72, 50)
                .box(Mat.PANELS, -22, 40, -46, 22, 68, 46)
                .box(Mat.PANELS, -16, 34, -46, 16, 74, 46)
                .box(Mat.CANVAS, -16, 38, -60, 16, 70, -50)
                .box(Mat.CANVAS, -16, 38, 50, 16, 70, 60)
                .box(Mat.CANVAS, -10, 44, -68, 10, 64, -60)
                .box(Mat.CANVAS, -10, 44, 60, 10, 64, 68)
                .box(Mat.BRASS, -5, 49, -72, 5, 59, -68)
                .box(Mat.CANVAS, -5, 49, 68, 5, 59, 72)
                .box(Mat.BRASS, -22.5, 33.5, -21, 22.5, 74.5, -19)
                .box(Mat.BRASS, -22.5, 33.5, 19, 22.5, 74.5, 21)
                .box(Mat.RED, -22.3, 52, -46, 22.3, 56, 46);

        // Fins at the tail.
        Bone fins = new Bone("fins", "envelope", 0, 54, 58)
                .box(Mat.RED, -1, 72, 48, 1, 88, 66)
                .box(Mat.RED, -1, 22, 50, 1, 36, 64)
                .box(Mat.RED, -36, 53, 50, -20, 55, 66)
                .box(Mat.RED, 20, 53, 50, 36, 55, 66);

        // The screw, behind the stern on a shaft; the animation spins this bone about Z.
        // Blades reach a pixel above the keel's bottom, so a parked ship's screw clears the ground.
        Bone propeller = new Bone("propeller", "root", 0, 9, 37)
                .box(Mat.IRON, -1, 8, 34, 1, 10, 37)
                .box(Mat.BRASS, -2, 7, 36, 2, 11, 38.5)
                .box(Mat.PROP, -1.5, -1, 36.5, 1.5, 19, 37.5)
                .box(Mat.PROP, -10, 7.5, 36.5, 10, 10.5, 37.5);

        return List.of(root, gondola, boiler, rigging, envelope, fins, propeller);
    }

    // ---------------------------------------------------------------- writing

    static void write(File file, String name, List<Bone> bones) throws Exception {
        StringBuilder sb = new StringBuilder();
        sb.append("{\n\t\"format_version\": \"1.12.0\",\n\t\"minecraft:geometry\": [\n\t\t{\n");
        sb.append("\t\t\t\"description\": {\n");
        sb.append("\t\t\t\t\"identifier\": \"geometry.hollowfrontier.").append(name).append("\",\n");
        sb.append("\t\t\t\t\"texture_width\": ").append(TEX_W).append(",\n");
        sb.append("\t\t\t\t\"texture_height\": ").append(TEX_H).append(",\n");
        sb.append("\t\t\t\t\"visible_bounds_width\": 10,\n\t\t\t\t\"visible_bounds_height\": 7,\n");
        sb.append("\t\t\t\t\"visible_bounds_offset\": [0, 3, 0]\n\t\t\t},\n");
        sb.append("\t\t\t\"bones\": [\n");
        for (int b = 0; b < bones.size(); b++) {
            Bone bone = bones.get(b);
            sb.append("\t\t\t\t{\n\t\t\t\t\t\"name\": \"").append(bone.name).append("\",\n");
            if (bone.parent != null) {
                sb.append("\t\t\t\t\t\"parent\": \"").append(bone.parent).append("\",\n");
            }
            sb.append("\t\t\t\t\t\"pivot\": ").append(vec(bone.pivot));
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
        double w = c.x1 - c.x0, h = c.y1 - c.y0, d = c.z1 - c.z0;
        StringBuilder sb = new StringBuilder("{\"origin\": ").append(vec(c.x0, c.y0, c.z0)).append(", \"size\": ").append(vec(w, h, d));
        sb.append(", \"uv\": {");
        sb.append("\"north\": ").append(face(c.mat, w, h)).append(", ");
        sb.append("\"south\": ").append(face(c.front, w, h)).append(", ");
        sb.append("\"east\": ").append(face(c.mat, d, h)).append(", ");
        sb.append("\"west\": ").append(face(c.mat, d, h)).append(", ");
        sb.append("\"up\": ").append(face(c.mat, w, d)).append(", ");
        sb.append("\"down\": ").append(face(c.mat, w, d));
        return sb.append("}}").toString();
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
            double x = Math.round(v[i] * 1000) / 1000.0;
            sb.append(x == Math.rint(x) ? String.valueOf((long) x) : String.format(Locale.ROOT, "%s", x));
        }
        return sb.append("]").toString();
    }
}
