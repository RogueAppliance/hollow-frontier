import java.io.File;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Writes the GeckoLib geo models for the saloon barmaids: one shared figure (slim arms, cinched corset waist, bodice,
 * puffed sleeves, a three-tier skirt over the legs) plus each hostess's own hairstyle and accessories.
 * Run from the project root:  java tools/models/BarmaidModels.java src/main/resources/assets/hollowfrontier/geckolib/models/entity
 *
 * Bedrock/Blockbench model space in pixels, feet at y 0, facing north (-Z). GeckoLib mirrors X, so the barmaid's RIGHT
 * side is NEGATIVE x (same as the shared humanoid model). Bone names match humanoid (body/head/arms/legs/hand items), so
 * geckolib/animations/entity/barmaid.animation.json (hand-written) drives them.
 *
 * Every cube uses Bedrock box UV on a 128x64 texture. The UV origins below must match TextureArt.Barmaids.
 */
public class BarmaidModels {
    // Box UV origins {u, v}. Footprint of a w*h*d box is 2(w+d) x (d+h).
    static final int[] HEAD = {0, 0}, HAIR_CAP = {32, 0}, CHEST = {64, 0}, BODICE = {88, 0}, WAIST = {64, 10};
    static final int[] SKIRT_UPPER = {0, 16}, SKIRT_LOWER = {30, 16}, HEM = {66, 17};
    static final int[] R_ARM = {0, 30}, L_ARM = {12, 30}, R_LEG = {24, 30}, L_LEG = {36, 30}, R_PUFF = {48, 30}, L_PUFF = {64, 30};
    static final int[] HAIR_LONG = {80, 26}, BUN = {102, 26}, TAIL = {102, 33}, CURL_R = {112, 33}, CURL_L = {112, 41};
    static final int[] HAT_CROWN = {48, 38}, HAT_BRIM = {0, 45}, FEATHER = {28, 45}, FLOWER = {34, 45}, BOW = {40, 45};

    record Cube(double x, double y, double z, int w, int h, int d, int[] uv, double inflate) {
        Cube(double x, double y, double z, int w, int h, int d, int[] uv) {
            this(x, y, z, w, h, d, uv, 0);
        }
    }

    record Bone(String name, String parent, double[] pivot, double[] rotation, List<Cube> cubes) {
        Bone(String name, String parent, double[] pivot, Cube... cubes) {
            this(name, parent, pivot, null, List.of(cubes));
        }
    }

    enum Hair { LONG, PONYTAIL, CURLS, BUN, BRAID }

    enum Hostess {
        RUBY(Hair.LONG, false, true, false),
        CLEMENTINE(Hair.PONYTAIL, false, false, true),
        JOSIE(Hair.CURLS, true, false, false),
        BELLE(Hair.BUN, false, false, true),
        LUPE(Hair.BRAID, false, true, false);

        final Hair hair;
        final boolean hat, flower, bow;

        Hostess(Hair hair, boolean hat, boolean flower, boolean bow) {
            this.hair = hair; this.hat = hat; this.flower = flower; this.bow = bow;
        }
    }

    public static void main(String[] args) throws Exception {
        File dir = new File(args.length > 0 ? args[0] : "src/main/resources/assets/hollowfrontier/geckolib/models/entity");
        dir.mkdirs();
        for (Hostess h : Hostess.values()) {
            String name = "barmaid_" + h.name().toLowerCase(Locale.ROOT);
            Files.writeString(new File(dir, name + ".geo.json").toPath(), json(name, bones(h)));
        }
        System.out.println("BarmaidModels: wrote " + Hostess.values().length + " models to " + dir);
    }

    static List<Bone> bones(Hostess h) {
        List<Bone> bones = new ArrayList<>();
        // Corset bodice: shoulders 8 wide, waist cinched to 6, a little shaped bodice front
        bones.add(new Bone("body", null, new double[] {0, 24, 0},
                new Cube(-4, 18, -2, 8, 6, 4, CHEST),
                new Cube(-3, 14, -1.5, 6, 4, 3, WAIST),
                new Cube(-3.5, 19, -2.6, 7, 3, 1, BODICE)));
        // Skirt flares in three tiers down to a lace hem just above the boots
        bones.add(new Bone("skirt", "body", new double[] {0, 14, 0},
                new Cube(-4.5, 8, -3, 9, 6, 6, SKIRT_UPPER),
                new Cube(-5.5, 1, -3.5, 11, 7, 7, SKIRT_LOWER),
                new Cube(-6, 0.5, -4, 12, 1, 8, HEM)));
        bones.add(new Bone("head", "body", new double[] {0, 24, 0},
                new Cube(-4, 24, -4, 8, 8, 8, HEAD),
                new Cube(-4, 24, -4, 8, 8, 8, HAIR_CAP, 0.5)));
        bones.add(new Bone("hair", "head", new double[] {0, 24, 0}, hair(h).toArray(Cube[]::new)));
        if (h.hat) {
            // Small feathered hat worn at an angle
            bones.add(new Bone("hat", "head", new double[] {1.5, 32, 0}, new double[] {0, 0, -14}, List.of(
                    new Cube(-2, 32, -3.5, 7, 1, 7, HAT_BRIM),
                    new Cube(-1, 33, -2.5, 5, 2, 5, HAT_CROWN),
                    new Cube(3, 33.5, 0, 1, 5, 2, FEATHER))));
        }
        bones.add(new Bone("right_arm", "body", new double[] {-5, 22, 0},
                new Cube(-7, 12, -1.5, 3, 12, 3, R_ARM),
                new Cube(-7.5, 19, -2, 4, 4, 4, R_PUFF)));
        bones.add(new Bone("RightHandItem", "right_arm", new double[] {-5.5, 13, -1}));
        bones.add(new Bone("left_arm", "body", new double[] {5, 22, 0},
                new Cube(4, 12, -1.5, 3, 12, 3, L_ARM),
                new Cube(3.5, 19, -2, 4, 4, 4, L_PUFF)));
        bones.add(new Bone("LeftHandItem", "left_arm", new double[] {5.5, 13, -1}));
        bones.add(new Bone("right_leg", "body", new double[] {-1.9, 12, 0}, new Cube(-3.4, 0, -1.5, 3, 12, 3, R_LEG)));
        bones.add(new Bone("left_leg", "body", new double[] {1.9, 12, 0}, new Cube(0.4, 0, -1.5, 3, 12, 3, L_LEG)));
        return bones;
    }

    static List<Cube> hair(Hostess h) {
        List<Cube> cubes = new ArrayList<>();
        switch (h.hair) {
            case LONG -> cubes.add(new Cube(-4.5, 15, 3.6, 9, 16, 2, HAIR_LONG));          // down to the waist
            case PONYTAIL -> cubes.add(new Cube(-1.5, 20, 4.2, 3, 10, 2, TAIL));            // high tail
            case CURLS -> {                                                                  // ringlets framing the face
                cubes.add(new Cube(-5.2, 19, -2.5, 2, 6, 2, CURL_R));
                cubes.add(new Cube(3.2, 19, -2.5, 2, 6, 2, CURL_L));
            }
            case BUN -> cubes.add(new Cube(-2, 28, 3.5, 4, 3, 4, BUN));                     // pinned-up bun
            case BRAID -> cubes.add(new Cube(-1.5, 15, 4.2, 3, 10, 2, TAIL));               // one long braid
        }
        if (h.flower) {
            cubes.add(new Cube(3.6, 27.5, -1.5, 1, 2, 2, FLOWER));                          // tucked over the left ear
        }
        if (h.bow) {
            cubes.add(h.hair == Hair.BUN ? new Cube(-2, 26.5, 4.5, 4, 2, 1, BOW) : new Cube(-2, 28, 5.9, 4, 2, 1, BOW));
        }
        return cubes;
    }

    static String json(String name, List<Bone> bones) {
        StringBuilder sb = new StringBuilder();
        sb.append("{\n\t\"format_version\": \"1.12.0\",\n\t\"minecraft:geometry\": [\n\t\t{\n");
        sb.append("\t\t\t\"description\": {\n");
        sb.append("\t\t\t\t\"identifier\": \"geometry.hollowfrontier.").append(name).append("\",\n");
        sb.append("\t\t\t\t\"texture_width\": 128,\n\t\t\t\t\"texture_height\": 64,\n");
        sb.append("\t\t\t\t\"visible_bounds_width\": 2,\n\t\t\t\t\"visible_bounds_height\": 3,\n\t\t\t\t\"visible_bounds_offset\": [0, 1.5, 0]\n");
        sb.append("\t\t\t},\n\t\t\t\"bones\": [\n");
        for (int i = 0; i < bones.size(); i++) {
            Bone b = bones.get(i);
            sb.append("\t\t\t\t{\n\t\t\t\t\t\"name\": \"").append(b.name).append("\",\n");
            if (b.parent != null) sb.append("\t\t\t\t\t\"parent\": \"").append(b.parent).append("\",\n");
            sb.append("\t\t\t\t\t\"pivot\": ").append(vec(b.pivot));
            if (b.rotation != null) sb.append(",\n\t\t\t\t\t\"rotation\": ").append(vec(b.rotation));
            if (!b.cubes.isEmpty()) {
                sb.append(",\n\t\t\t\t\t\"cubes\": [\n");
                for (int c = 0; c < b.cubes.size(); c++) {
                    Cube cube = b.cubes.get(c);
                    sb.append("\t\t\t\t\t\t{\"origin\": ").append(vec(cube.x, cube.y, cube.z))
                            .append(", \"size\": ").append(vec(cube.w, cube.h, cube.d))
                            .append(", \"uv\": ").append(vec(cube.uv[0], cube.uv[1]));
                    if (cube.inflate != 0) sb.append(", \"inflate\": ").append(num(cube.inflate));
                    sb.append("}").append(c < b.cubes.size() - 1 ? ",\n" : "\n");
                }
                sb.append("\t\t\t\t\t]");
            }
            sb.append("\n\t\t\t\t}").append(i < bones.size() - 1 ? ",\n" : "\n");
        }
        sb.append("\t\t\t]\n\t\t}\n\t]\n}\n");
        return sb.toString();
    }

    static String vec(double... v) {
        StringBuilder sb = new StringBuilder("[");
        for (int i = 0; i < v.length; i++) sb.append(i > 0 ? ", " : "").append(num(v[i]));
        return sb.append("]").toString();
    }

    static String num(double d) {
        return d == Math.rint(d) ? Long.toString((long) d) : Double.toString(d);
    }
}
