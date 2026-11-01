import java.io.File;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Writes the GeckoLib geo models for the guns, the black powder ones and the phlogiston ones (13b): a code "blueprint" per gun,
 * like the structures.
 * Run from the project root:  java tools/models/GunModels.java src/main/resources/assets/hollowfrontier/geckolib/models/item
 *
 * Coordinates are Bedrock/Blockbench model space in pixels: grip at the origin, barrel toward -Z (north), +Y up.
 * GeckoLib mirrors X when it loads, so the gun's RIGHT side (lock, hammer) is NEGATIVE x here.
 * Animation JSON (geckolib/animations/item/*.animation.json) is hand-written and uses the bone names and pivots below.
 *
 * Every face is UV-mapped onto a material swatch of the gun's 128x64 texture (painted by tools/textures/TextureArt.java,
 * see Gun.Swatch there) at 2 texels per model pixel. Materials have a horizontal and a vertical swatch so wood grain
 * and polish streaks follow the long axis of each face.
 */
public class GunModels {
    static final int TEX_W = 128, TEX_H = 64;
    static final double DENSITY = 2.0;

    // Swatch rectangles {u, v, w, h}; must match TextureArt.Gun.
    enum Mat {
        WOOD(0, 0, 64, 16, 64, 0, 16, 64),
        STEEL(0, 16, 64, 8, 96, 0, 8, 64),
        CASE(0, 24, 32, 16, 0, 24, 32, 16),
        BRASS(32, 24, 32, 16, 32, 24, 32, 16),
        BRIGHT(0, 40, 16, 16, 0, 40, 16, 16),
        BLACK(16, 40, 16, 16, 16, 40, 16, 16),
        HICKORY(32, 40, 32, 8, 104, 0, 8, 64),
        BORE(0, 56, 8, 8, 0, 56, 8, 8),
        CHAMBERS(8, 56, 8, 8, 8, 56, 8, 8),
        COPPER(16, 56, 8, 8, 16, 56, 8, 8);

        final int hu, hv, hw, hh, vu, vv, vw, vh;

        Mat(int hu, int hv, int hw, int hh, int vu, int vv, int vw, int vh) {
            this.hu = hu; this.hv = hv; this.hw = hw; this.hh = hh;
            this.vu = vu; this.vv = vv; this.vw = vw; this.vh = vh;
        }
    }

    record Cube(double x0, double y0, double z0, double x1, double y1, double z1, Mat mat, Mat front,
                double[] rotation, double[] pivot) {}

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
            cubes.add(new Cube(x0, y0, z0, x1, y1, z1, mat, mat, null, null));
            return this;
        }

        // A box whose north (muzzle-facing) face uses a different material, e.g. a bore.
        Bone box(Mat mat, Mat front, double x0, double y0, double z0, double x1, double y1, double z1) {
            cubes.add(new Cube(x0, y0, z0, x1, y1, z1, mat, front, null, null));
            return this;
        }

        Bone rotated(Mat mat, double x0, double y0, double z0, double x1, double y1, double z1, double rx, double ry, double rz,
                     double px, double py, double pz) {
            cubes.add(new Cube(x0, y0, z0, x1, y1, z1, mat, mat, new double[] {rx, ry, rz}, new double[] {px, py, pz}));
            return this;
        }

        /**
         * Octagonal bar along Z with apothem a (half the width across flats): the union of four side*2a rectangles at
         * 0/45/90/135 degrees. Ends are staggered by a hair so the coplanar ends don't z-fight; a cap() carries the front picture.
         */
        Bone octagon(Mat mat, Mat front, double cx, double cy, double a, double z0, double z1) {
            double s = a * 0.8284; // side length = 2a tan(22.5)
            double zc = (z0 + z1) / 2;
            cubes.add(new Cube(cx - s / 2, cy - a, z0, cx + s / 2, cy + a, z1, mat, mat, null, null));
            cap(mat, front, cx, cy, a * 0.8, z0);
            cubes.add(new Cube(cx - a, cy - s / 2, z0 + 0.02, cx + a, cy + s / 2, z1 - 0.02, mat, mat, null, null));
            rotated(mat, cx - s / 2, cy - a, z0 + 0.04, cx + s / 2, cy + a, z1 - 0.04, 0, 0, 45, cx, cy, zc);
            rotated(mat, cx - a, cy - s / 2, z0 + 0.06, cx + a, cy + s / 2, z1 - 0.06, 0, 0, 45, cx, cy, zc);
            return this;
        }

        // A paper-thin square just proud of a bar's front end that carries the muzzle/chamber picture, so it isn't squashed
        // onto one of the narrow rectangles the bar is built from.
        private void cap(Mat mat, Mat front, double cx, double cy, double half, double z0) {
            cubes.add(new Cube(cx - half, cy - half, z0 - 0.01, cx + half, cy + half, z0 + 0.05, mat, front, null, null));
        }

        // Hexagonal prism along Z with circumradius r: three r x sqrt(3)r rectangles at 0/60/120 degrees.
        Bone hexagon(Mat mat, Mat front, double cx, double cy, double r, double z0, double z1) {
            double h = Math.sqrt(3) * r;
            double zc = (z0 + z1) / 2;
            cubes.add(new Cube(cx - r / 2, cy - h / 2, z0, cx + r / 2, cy + h / 2, z1, mat, mat, null, null));
            cap(mat, front, cx, cy, r * 0.8, z0);
            rotated(mat, cx - r / 2, cy - h / 2, z0 + 0.02, cx + r / 2, cy + h / 2, z1 - 0.02, 0, 0, 60, cx, cy, zc);
            rotated(mat, cx - r / 2, cy - h / 2, z0 + 0.04, cx + r / 2, cy + h / 2, z1 - 0.04, 0, 0, 120, cx, cy, zc);
            return this;
        }
    }

    public static void main(String[] args) throws Exception {
        File dir = new File(args.length > 0 ? args[0] : "src/main/resources/assets/hollowfrontier/geckolib/models/item");
        dir.mkdirs();
        write(new File(dir, "revolver.geo.json"), "revolver", revolver());
        write(new File(dir, "plains_rifle.geo.json"), "plains_rifle", plainsRifle());
        write(new File(dir, "shotgun.geo.json"), "shotgun", shotgun());
        write(new File(dir, "ray_pistol.geo.json"), "ray_pistol", rayPistol());
        write(new File(dir, "phlogiston_rifle.geo.json"), "phlogiston_rifle", phlogistonRifle());
        write(new File(dir, "polar_resonator.geo.json"), "polar_resonator", resonator());
        write(new File(dir, "steam_drill.geo.json"), "steam_drill", steamDrill());
        write(new File(dir, "sunstone_lens.geo.json"), "sunstone_lens", sunstoneLens());
        System.out.println("GunModels: wrote 5 gun models, the Polar Resonator, the Steam Drill and the Sunstone Lens to " + dir);
    }

    // ---------------------------------------------------------------- the Steam Drill and the Sunstone Lens

    /**
     * The Steam Drill, held like a pistol (TextureArt.Gun.paintDrill): a walnut grip at the origin, an iron guard, a fat
     * brass housing running forward (-Z) with a gauge on its back toward the holder, a red valve wheel on the right (-x) and a
     * copper exhaust on the left, an iron collar, and the fluted steel bit. The bit is its own bone on the drill's axis so the
     * "spin" animation turns it; dark flutes wound round it at staggered angles make the turning show.
     */
    static List<Bone> steamDrill() {
        double ay = 5.8; // the bit's axis
        Bone drill = new Bone("drill", null, 0, 0, 0)
                .rotated(Mat.WOOD, -0.8, -1.5, -0.9, 0.8, 4.2, 0.9, 12, 0, 0, 0, 4.2, 0)  // the grip, raked back
                .box(Mat.STEEL, -0.9, 3.6, -2.4, 0.9, 4.1, 0.9)              // its guard
                .box(Mat.BLACK, -0.15, 2.6, -1.9, 0.15, 3.6, -1.5)            // the trigger
                .box(Mat.BRASS, -1.8, 4.0, -6.2, 1.8, 7.6, 1.4)               // the housing
                .box(Mat.STEEL, -1.9, 3.9, 1.4, 1.9, 7.7, 1.8)                // its back plate
                .box(Mat.BRASS, -0.9, 7.6, -0.6, 0.9, 9.2, 0.9)               // the gauge on top, its dial toward the holder
                .box(Mat.CHAMBERS, -0.75, 7.75, 0.9, 0.75, 9.05, 0.95)
                .box(Mat.CASE, -2.15, 5.0, -3.0, -1.8, 6.8, -1.2)             // the valve wheel, on the right
                .box(Mat.COPPER, 1.8, 6.3, -5.5, 2.4, 6.9, 0.5)               // the exhaust, on the left
                .box(Mat.COPPER, 1.8, 6.9, -5.5, 2.4, 7.9, -4.9)              //   and its stack
                .box(Mat.STEEL, -1.3, 4.5, -7.3, 1.3, 7.1, -6.2);             // the collar
        Bone bit = new Bone("bit", "drill", 0, ay, -7.3)
                .octagon(Mat.STEEL, Mat.STEEL, 0, ay, 0.85, -12.6, -7.3)
                .octagon(Mat.STEEL, Mat.STEEL, 0, ay, 0.45, -14.0, -12.6);
        for (int k = 0; k < 5; k++) { // the flutes, a turn of the spiral per segment
            double z0 = -12.4 + k, z1 = z0 + 0.45;
            bit.rotated(Mat.BLACK, -0.95, ay - 0.15, z0, 0.95, ay + 0.15, z1, 0, 0, k * 40, 0, ay, (z0 + z1) / 2);
        }
        return List.of(drill, bit);
    }

    /**
     * The Sunstone Lens, held up like a reading glass (TextureArt.Gun.paintLens): a gold handle with a leather wrap at the
     * origin, up (+Y) to a crimson starsteel ring round a disc of sunstone facing the holder (+Z), lit full-bright from the
     * glowmask. The disc is its own bone so the "flare" animation can swell it when a beam is thrown.
     */
    static List<Bone> sunstoneLens() {
        double cy = 9.0, r = 3.4; // the ring's centre and radius
        Bone lens = new Bone("lens", null, 0, 0, 0)
                .box(Mat.BRASS, -0.7, -3.6, -0.7, 0.7, -3.0, 0.7)             // the handle's foot
                .box(Mat.WOOD, -0.6, -3.0, -0.6, 0.6, 3.0, 0.6)               // its leather wrap
                .box(Mat.BRASS, -0.75, 3.0, -0.75, 0.75, 5.0, 0.75)           // the gold neck
                .box(Mat.BRASS, -1.0, 5.0, -0.8, 1.0, 5.6, 0.8);              //   flaring to the ring
        for (int k = 0; k < 8; k++) { // the ring: eight bars round the disc
            double side = r * 0.8284 + 0.2;
            lens.rotated(Mat.STEEL, -side / 2, cy - r - 0.45, -0.55, side / 2, cy - r + 0.45, 0.55, 0, 0, k * 45 + 22.5, 0, cy, 0);
        }
        Bone glass = new Bone("glass", "lens", 0, cy, 0)
                .octagon(Mat.BRIGHT, Mat.BRIGHT, 0, cy, r - 0.3, -0.25, 0.25);
        return List.of(lens, glass);
    }

    // ---------------------------------------------------------------- the Polar Resonator

    /**
     * The Polar Resonator, held (it's not a gun, but it's the same kind of model on the same atlas, painted by
     * TextureArt.Gun.paintResonator): a brass tuning fork on a steam gauge, held by a short walnut grip. Grip at the
     * origin with the fork up (+Y) and the gauge's dial toward the holder (+Z). Bones: the body; the two tines, each pivoting
     * at its root so the "ring" animation can shiver them; and the gauge's needle, pivoting at the dial's centre. The tines'
     * tips are the BRIGHT swatch, lit full-bright from the glowmask (the gateway's cyan). The CHAMBERS swatch holds the dial,
     * CASE the red of the valve wheel and the needle.
     */
    static List<Bone> resonator() {
        Bone body = new Bone("resonator", null, 0, 0, 0)
                .box(Mat.WOOD, -0.8, -4.0, -0.8, 0.8, 1.0, 0.8)          // the grip
                .box(Mat.BRASS, -1.1, -4.4, -1.1, 1.1, -3.9, 1.1)         // its end cap
                .box(Mat.BRASS, -1.2, 1.0, -1.2, 1.2, 1.6, 1.2)           // the collar
                .box(Mat.BRASS, -2.2, 1.6, -1.3, 2.2, 5.8, 1.3)           // the gauge's case
                .box(Mat.STEEL, -1.9, 1.9, 1.3, 1.9, 2.2, 1.42)           // its bezel, bottom and top
                .box(Mat.STEEL, -1.9, 5.2, 1.3, 1.9, 5.5, 1.42)
                .box(Mat.STEEL, -1.9, 2.2, 1.3, -1.6, 5.2, 1.42)          // and sides
                .box(Mat.STEEL, 1.6, 2.2, 1.3, 1.9, 5.2, 1.42)
                .box(Mat.CHAMBERS, -1.6, 2.2, 1.3, 1.6, 5.2, 1.34)        // the dial
                .box(Mat.COPPER, -3.0, 3.0, -0.35, -2.2, 3.7, 0.35)       // a steam pipe out of the right side (-x)
                .box(Mat.COPPER, -3.0, 0.2, -0.35, -2.3, 3.0, 0.35)       //   and down
                .box(Mat.CASE, 2.2, 2.6, -0.9, 2.5, 4.4, 0.9)             // the valve wheel, on the left
                .box(Mat.BRASS, -0.6, 5.8, -0.6, 0.6, 8.0, 0.6)           // the fork's stem
                .box(Mat.BRASS, -2.6, 8.0, -0.6, 2.6, 9.0, 0.6);          // and its yoke
        Bone left = new Bone("tine_left", "resonator", 2.05, 9.0, 0)
                .box(Mat.BRASS, 1.5, 9.0, -0.5, 2.6, 16.0, 0.5)
                .box(Mat.BRIGHT, 1.5, 16.0, -0.5, 2.6, 17.0, 0.5);       // its lit tip
        Bone right = new Bone("tine_right", "resonator", -2.05, 9.0, 0)
                .box(Mat.BRASS, -2.6, 9.0, -0.5, -1.5, 16.0, 0.5)
                .box(Mat.BRIGHT, -2.6, 16.0, -0.5, -1.5, 17.0, 0.5);
        Bone needle = new Bone("needle", "resonator", 0, 3.7, 1.4)
                .box(Mat.CASE, -0.12, 3.6, 1.35, 0.12, 5.0, 1.45);
        return List.of(body, left, right, needle);
    }

    // ---------------------------------------------------------------- guns

    /**
     * Cap & ball revolver after the 1851 Navy: open-top case-hardened frame, octagonal 7.5" barrel, hexagonal-ish
     * six-shot cylinder, loading lever + rammer under the barrel, brass trigger guard and backstrap, one-piece walnut grip.
     * Bones: gun > cylinder (turns about the bore-parallel axis), lever (hinged at the lug), rammer, hammer.
     */
    static List<Bone> revolver() {
        Bone gun = new Bone("gun", null, 0, 0, 0)
                .octagon(Mat.STEEL, Mat.BORE, 0, 5.0, 0.6, -11.0, -3.0)
                .box(Mat.BRASS, -0.15, 5.55, -10.75, 0.15, 6.0, -10.35)       // front sight (brass cone)
                .box(Mat.STEEL, -0.45, 3.75, -6.6, 0.45, 4.45, -3.0)          // barrel lug
                .box(Mat.BRIGHT, -0.15, 3.9, -3.45, 0.15, 4.2, -2.8)          // arbor pin
                .box(Mat.CASE, -0.6, 2.2, -3.45, 0.6, 4.45, -2.9)             // frame front, under the barrel breech
                .box(Mat.CASE, -0.75, 2.2, -3.1, 0.75, 2.95, 0.4)             // frame belly under the cylinder
                .box(Mat.CASE, -0.95, 2.2, 0.0, 0.95, 5.3, 0.7)               // recoil shield
                .box(Mat.CASE, -0.6, 1.2, 0.6, 0.6, 4.6, 1.5)                 // frame rear / hammer housing
                .box(Mat.BRASS, -0.25, 0.3, -1.8, 0.25, 0.6, 0.3)             // trigger guard bow
                .box(Mat.BRASS, -0.25, 0.3, -2.1, 0.25, 2.2, -1.8)            // guard front
                .box(Mat.BRASS, -0.25, 0.3, 0.3, 0.25, 1.3, 0.6)              // guard rear
                .rotated(Mat.BLACK, -0.12, 0.9, -1.0, 0.12, 2.2, -0.7, 10, 0, 0, 0, 2.2, -0.85) // trigger
                .rotated(Mat.WOOD, -0.65, -3.6, 0.3, 0.65, 1.6, 2.1, 15, 0, 0, 0, 1.6, 1.2)      // grip
                .rotated(Mat.BRASS, -0.45, -3.6, 2.1, 0.45, 1.2, 2.35, 15, 0, 0, 0, 1.6, 1.2)    // backstrap
                .rotated(Mat.BRASS, -0.4, -3.6, 0.1, 0.4, 0.3, 0.3, 15, 0, 0, 0, 1.6, 1.2)       // front strap
                .rotated(Mat.BRASS, -0.7, -3.85, 0.3, 0.7, -3.6, 2.35, 15, 0, 0, 0, 1.6, 1.2);   // butt
        Bone cylinder = new Bone("cylinder", "gun", 0, 4.05, -1.45)
                .hexagon(Mat.STEEL, Mat.CHAMBERS, 0, 4.05, 1.25, -2.85, -0.05);
        Bone lever = new Bone("lever", "gun", 0, 3.75, -3.4)
                .box(Mat.CASE, -0.25, 3.3, -9.6, 0.25, 3.75, -3.3)            // lever arm under the barrel
                .box(Mat.CASE, -0.2, 3.75, -9.6, 0.2, 4.4, -9.2);             // latch at the muzzle end
        Bone rammer = new Bone("rammer", "gun", 0, 3.45, -3.2)
                .box(Mat.BRIGHT, -0.22, 3.25, -3.4, 0.22, 3.65, -2.9);
        Bone hammer = new Bone("hammer", "gun", 0, 3.4, 1.0)
                .box(Mat.CASE, -0.25, 3.6, 0.7, 0.25, 5.1, 1.2)
                .box(Mat.CASE, -0.2, 4.9, 1.0, 0.2, 5.3, 2.1)                 // spur
                .box(Mat.BRIGHT, -0.15, 4.6, 0.4, 0.15, 5.0, 0.7);            // nose
        return List.of(gun, cylinder, lever, rammer, hammer);
    }

    /**
     * Hawken-style half-stock plains rifle: heavy browned octagon barrel, brass blade front sight and buckhorn rear,
     * hickory ramrod in brass thimbles under a rib, case-hardened percussion sidelock on the right, double set triggers,
     * brass guard and nose cap, walnut stock with cheek piece and an iron crescent butt plate.
     * Bones: gun > hammer (right side), ramrod.
     */
    static List<Bone> plainsRifle() {
        double drop = -7, sx = 0, sy = 3.4, sz = 5.5; // stock drop at the heel, rotated about the comb's front
        Bone gun = new Bone("gun", null, 0, 0, 0)
                .octagon(Mat.STEEL, Mat.BORE, 0, 4.0, 0.8, -30.0, 1.5)
                .box(Mat.STEEL, -0.4, 4.0, 1.5, 0.4, 4.55, 3.6)               // breech plug tang
                .box(Mat.BRASS, -0.12, 4.8, -29.4, 0.12, 5.25, -28.9)         // front sight blade
                .box(Mat.BLACK, -0.55, 4.8, -19.2, 0.55, 5.3, -18.8)          // buckhorn rear sight
                .box(Mat.STEEL, -0.35, 2.7, -27.0, 0.35, 3.25, -11.5)         // under-rib
                .box(Mat.BRASS, -0.45, 1.85, -25.5, 0.45, 2.75, -24.7)        // thimbles
                .box(Mat.BRASS, -0.45, 1.85, -19.5, 0.45, 2.75, -18.7)
                .box(Mat.WOOD, -1.0, 1.3, -12.0, 1.0, 3.7, 1.5)               // half-stock forend
                .box(Mat.BRASS, -1.05, 1.25, -12.4, 1.05, 3.75, -12.0)        // nose cap
                .box(Mat.CASE, -1.2, 2.2, -0.8, -1.0, 3.6, 3.4)               // lock plate (right)
                .box(Mat.BLACK, 1.0, 2.5, 0.2, 1.1, 3.3, 2.6)                 // side plate (left)
                .box(Mat.STEEL, -1.35, 3.6, 0.2, -0.8, 4.3, 1.0)              // drum / bolster
                .box(Mat.COPPER, -1.25, 4.3, 0.4, -0.95, 4.75, 0.8)           // percussion cap on the nipple
                .box(Mat.WOOD, -0.75, 0.7, 1.5, 0.75, 3.7, 6.0)               // wrist
                .rotated(Mat.WOOD, -0.85, 0.0, 4.0, 0.85, 3.6, 6.5, -4, 0, 0, sx, sy, sz)        // swell into the butt
                .rotated(Mat.WOOD, -1.0, -1.6, 5.5, 1.0, 3.4, 15.0, drop, 0, 0, sx, sy, sz)      // buttstock
                .rotated(Mat.WOOD, 1.0, 0.8, 7.5, 1.3, 2.6, 11.5, drop, 0, 0, sx, sy, sz)        // cheek piece (left)
                .rotated(Mat.BLACK, -1.05, -1.7, 15.0, 1.05, 3.45, 15.4, drop, 0, 0, sx, sy, sz) // crescent butt plate
                .box(Mat.BRASS, -0.22, -0.2, -1.8, 0.22, 0.1, 3.0)            // trigger guard bow
                .box(Mat.BRASS, -0.22, -0.2, -2.1, 0.22, 1.3, -1.8)           // guard front
                .box(Mat.BRASS, -0.22, -0.2, 2.7, 0.22, 0.7, 3.0)             // guard rear
                .box(Mat.BRASS, -0.22, 0.45, 3.0, 0.22, 0.7, 5.5)             // guard tang under the wrist
                .box(Mat.BLACK, -0.1, 0.2, -0.7, 0.1, 1.3, -0.45)             // set triggers
                .box(Mat.BLACK, -0.1, 0.1, 0.5, 0.1, 1.3, 0.75);
        Bone hammer = new Bone("hammer", "gun", -1.35, 3.2, 2.2)
                .box(Mat.CASE, -1.55, 3.0, 1.8, -1.2, 4.6, 2.6)
                .box(Mat.CASE, -1.5, 4.4, 2.4, -1.25, 4.8, 3.4)               // spur
                .box(Mat.CASE, -1.5, 4.55, 0.4, -1.15, 5.0, 1.9);             // nose, resting on the cap
        Bone ramrod = new Bone("ramrod", "gun", 0, 2.3, -16.0)
                .box(Mat.HICKORY, -0.28, 2.02, -27.2, 0.28, 2.58, -4.8)
                .box(Mat.BRASS, -0.32, 1.98, -27.5, 0.32, 2.62, -27.2);       // brass tip
        return List.of(gun, hammer, ramrod);
    }

    /**
     * Side-by-side percussion shotgun (coach gun length): two blued barrels with a top rib and brass bead, short
     * walnut forend, ramrod under the barrels, case-hardened locks and twin hammers, straight English wrist.
     * Bones: gun > hammer_r, hammer_l, ramrod.
     */
    static List<Bone> shotgun() {
        double drop = -6, sx = 0, sy = 3.5, sz = 6.0;
        Bone gun = new Bone("gun", null, 0, 0, 0)
                .octagon(Mat.STEEL, Mat.BORE, -0.68, 4.0, 0.66, -24.0, 1.5)   // right barrel
                .octagon(Mat.STEEL, Mat.BORE, 0.68, 4.0, 0.66, -24.0, 1.5)    // left barrel
                .box(Mat.STEEL, -0.3, 4.35, -23.9, 0.3, 4.85, 1.5)            // top rib
                .box(Mat.BRASS, -0.12, 4.85, -23.8, 0.12, 5.15, -23.5)        // bead
                .box(Mat.STEEL, -0.3, 3.0, -23.5, 0.3, 3.4, -9.0)             // bottom rib
                .box(Mat.BRASS, -0.4, 2.25, -19.0, 0.4, 3.15, -18.3)          // ramrod thimble
                .box(Mat.WOOD, -1.35, 2.0, -9.0, 1.35, 3.6, 0.5)              // forend
                .box(Mat.CASE, -0.9, 1.4, -2.0, 0.9, 2.4, 1.5)                // action bar
                .box(Mat.CASE, -1.6, 2.0, 0.8, -1.35, 3.6, 4.8)               // right lock plate
                .box(Mat.CASE, 1.35, 2.0, 0.8, 1.6, 3.6, 4.8)                 // left lock plate
                .box(Mat.STEEL, -1.55, 3.6, 0.3, -1.0, 4.3, 1.1)              // bolsters
                .box(Mat.STEEL, 1.0, 3.6, 0.3, 1.55, 4.3, 1.1)
                .box(Mat.COPPER, -1.45, 4.3, 0.5, -1.15, 4.7, 0.9)            // caps
                .box(Mat.COPPER, 1.15, 4.3, 0.5, 1.45, 4.7, 0.9)
                .box(Mat.WOOD, -0.75, 0.9, 1.5, 0.75, 3.7, 6.5)               // straight wrist
                .box(Mat.STEEL, -0.35, 3.7, 1.5, 0.35, 3.95, 4.0)             // top tang
                .rotated(Mat.WOOD, -0.9, 0.3, 4.5, 0.9, 3.6, 7.0, -3, 0, 0, sx, sy, sz)          // swell into the butt
                .rotated(Mat.WOOD, -1.05, -1.2, 6.0, 1.05, 3.5, 14.0, drop, 0, 0, sx, sy, sz)    // buttstock
                .rotated(Mat.BLACK, -1.1, -1.3, 14.0, 1.1, 3.55, 14.35, drop, 0, 0, sx, sy, sz)  // butt plate
                .box(Mat.BLACK, -0.22, 0.0, -1.6, 0.22, 0.3, 2.6)             // trigger guard
                .box(Mat.BLACK, -0.22, 0.0, -1.9, 0.22, 1.6, -1.6)
                .box(Mat.BLACK, -0.22, 0.0, 2.4, 0.22, 1.0, 2.7)
                .box(Mat.BLACK, -0.1, 0.4, -0.8, 0.1, 1.6, -0.55)             // triggers
                .box(Mat.BLACK, -0.1, 0.4, 0.4, 0.1, 1.6, 0.65);
        Bone hammerR = new Bone("hammer_r", "gun", -1.55, 3.0, 2.4)
                .box(Mat.CASE, -1.85, 3.0, 2.0, -1.5, 4.6, 2.8)
                .box(Mat.CASE, -1.8, 4.4, 2.6, -1.55, 4.8, 3.6)
                .box(Mat.CASE, -1.8, 4.5, 0.5, -1.4, 5.0, 2.1);
        Bone hammerL = new Bone("hammer_l", "gun", 1.55, 3.0, 2.4)
                .box(Mat.CASE, 1.5, 3.0, 2.0, 1.85, 4.6, 2.8)
                .box(Mat.CASE, 1.55, 4.4, 2.6, 1.8, 4.8, 3.6)
                .box(Mat.CASE, 1.4, 4.5, 0.5, 1.8, 5.0, 2.1);
        Bone ramrod = new Bone("ramrod", "gun", 0, 2.7, -14.0)
                .box(Mat.HICKORY, -0.25, 2.45, -23.0, 0.25, 2.95, -5.0)
                .box(Mat.BRASS, -0.29, 2.41, -23.3, 0.29, 2.99, -23.0);
        return List.of(gun, hammerR, hammerL, ramrod);
    }


    // ---------------------------------------------------------------- the phlogiston guns (13b)
    // Same material swatches, painted for them (TextureArt.Gun.paintPhlogiston): WOOD is a dark composite, STEEL pale alien alloy,
    // CASE starsteel, BRIGHT the phlogiston's light (glowing), and BORE the emitter's face (glowing).

    /**
     * Ray Pistol: a pale alloy receiver on a dark grip, a glowing phlogiston tube along its top in brass caps, a slim barrel
     * through three starsteel fins of shrinking size, and a glowing emitter at the muzzle.
     * Bones: gun > coil (the tube, which flares when it fires).
     */
    static List<Bone> rayPistol() {
        Bone gun = new Bone("gun", null, 0, 0, 0)
                .box(Mat.STEEL, -0.8, 1.2, -4.2, 0.8, 4.2, 1.6)               // receiver
                .box(Mat.CASE, -0.55, 3.4, 1.6, 0.55, 4.4, 2.3)               // rear cap
                .octagon(Mat.STEEL, Mat.BORE, 0, 3.0, 0.5, -10.0, -4.2)       // barrel
                .octagon(Mat.CASE, Mat.CASE, 0, 3.0, 1.35, -6.0, -5.6)        // fins, shrinking toward the muzzle
                .octagon(Mat.CASE, Mat.CASE, 0, 3.0, 1.1, -7.7, -7.3)
                .octagon(Mat.CASE, Mat.CASE, 0, 3.0, 0.9, -9.3, -8.9)
                .octagon(Mat.BRIGHT, Mat.BORE, 0, 3.0, 0.65, -11.0, -10.0)    // emitter
                .box(Mat.BRASS, -0.25, 0.3, -1.8, 0.25, 0.6, 0.3)             // trigger guard
                .box(Mat.BRASS, -0.25, 0.3, -2.1, 0.25, 1.2, -1.8)
                .box(Mat.BRASS, -0.25, 0.3, 0.3, 0.25, 1.2, 0.6)
                .rotated(Mat.BLACK, -0.12, 0.6, -1.0, 0.12, 1.2, -0.7, 10, 0, 0, 0, 1.2, -0.85) // trigger
                .rotated(Mat.WOOD, -0.7, -3.4, 0.2, 0.7, 1.4, 2.0, 15, 0, 0, 0, 1.4, 1.1)       // grip
                .rotated(Mat.CASE, -0.75, -3.65, 0.2, 0.75, -3.4, 2.05, 15, 0, 0, 0, 1.4, 1.1); // pommel
        Bone coil = new Bone("coil", "gun", 0, 5.0, -1.5)
                .hexagon(Mat.BRIGHT, Mat.BRIGHT, 0, 5.0, 0.6, -3.6, 0.6)      // the phlogiston tube
                .box(Mat.BRASS, -0.55, 4.45, -3.9, 0.55, 5.55, -3.55)          // its caps
                .box(Mat.BRASS, -0.55, 4.45, 0.55, 0.55, 5.55, 0.9);
        return List.of(gun, coil);
    }

    /**
     * Phlogiston Rifle: a long pale alloy barrel through five starsteel fins to a broad glowing emitter, a heavy receiver with
     * a phlogiston tube along its top, a dark composite forend and stock (with the plains rifle's drop) and a black butt plate.
     * Bones: gun > coil.
     */
    static List<Bone> phlogistonRifle() {
        double drop = -7, sx = 0, sy = 3.4, sz = 5.5;
        Bone gun = new Bone("gun", null, 0, 0, 0)
                .box(Mat.STEEL, -1.0, 1.0, -6.5, 1.0, 4.0, 3.0)               // receiver
                .octagon(Mat.STEEL, Mat.BORE, 0, 3.0, 0.7, -26.0, -6.5)       // barrel
                .octagon(Mat.CASE, Mat.CASE, 0, 3.0, 1.6, -10.0, -9.5)        // fins
                .octagon(Mat.CASE, Mat.CASE, 0, 3.0, 1.5, -13.5, -13.0)
                .octagon(Mat.CASE, Mat.CASE, 0, 3.0, 1.4, -17.0, -16.5)
                .octagon(Mat.CASE, Mat.CASE, 0, 3.0, 1.3, -20.5, -20.0)
                .octagon(Mat.CASE, Mat.CASE, 0, 3.0, 1.2, -24.0, -23.5)
                .octagon(Mat.BRIGHT, Mat.BORE, 0, 3.0, 1.0, -28.0, -26.0)     // emitter
                .box(Mat.WOOD, -0.8, -0.1, -11.5, 0.8, 1.9, -7.0)              // forend grip
                .box(Mat.BRASS, -0.22, -0.2, -1.8, 0.22, 0.1, 2.6)            // trigger guard
                .box(Mat.BRASS, -0.22, -0.2, -2.1, 0.22, 1.0, -1.8)
                .box(Mat.BRASS, -0.22, -0.2, 2.3, 0.22, 1.0, 2.6)
                .box(Mat.BLACK, -0.1, 0.2, -0.4, 0.1, 1.0, -0.15)             // trigger
                .box(Mat.WOOD, -0.75, 0.7, 3.0, 0.75, 3.7, 6.5)               // wrist
                .rotated(Mat.WOOD, -1.0, -1.6, 5.5, 1.0, 3.4, 14.5, drop, 0, 0, sx, sy, sz)      // stock
                .rotated(Mat.CASE, -1.05, 2.6, 6.0, 1.05, 3.45, 12.0, drop, 0, 0, sx, sy, sz)    // starsteel comb
                .rotated(Mat.BLACK, -1.05, -1.7, 14.5, 1.05, 3.45, 14.9, drop, 0, 0, sx, sy, sz); // butt plate
        Bone coil = new Bone("coil", "gun", 0, 4.9, -5.0)
                .hexagon(Mat.BRIGHT, Mat.BRIGHT, 0, 4.9, 0.75, -12.0, 1.5)    // the phlogiston tube
                .box(Mat.BRASS, -0.7, 4.2, -12.4, 0.7, 5.6, -12.0)             // its caps
                .box(Mat.BRASS, -0.7, 4.2, 1.5, 0.7, 5.6, 1.9)
                .box(Mat.CASE, -0.3, 4.0, -8.0, 0.3, 4.3, -7.5)                // straps to the barrel
                .box(Mat.CASE, -0.3, 4.0, -3.0, 0.3, 4.3, -2.5);
        return List.of(gun, coil);
    }
    // ---------------------------------------------------------------- JSON

    static void write(File file, String name, List<Bone> bones) throws Exception {
        StringBuilder sb = new StringBuilder();
        sb.append("{\n\t\"format_version\": \"1.12.0\",\n\t\"minecraft:geometry\": [\n\t\t{\n");
        sb.append("\t\t\t\"description\": {\n");
        sb.append("\t\t\t\t\"identifier\": \"geometry.hollowfrontier.").append(name).append("\",\n");
        sb.append("\t\t\t\t\"texture_width\": ").append(TEX_W).append(",\n");
        sb.append("\t\t\t\t\"texture_height\": ").append(TEX_H).append(",\n");
        sb.append("\t\t\t\t\"visible_bounds_width\": 4,\n\t\t\t\t\"visible_bounds_height\": 3,\n");
        sb.append("\t\t\t\t\"visible_bounds_offset\": [0, 0.5, 0]\n\t\t\t},\n");
        sb.append("\t\t\t\"bones\": [\n");
        int seed = name.hashCode();
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
                    sb.append("\t\t\t\t\t\t").append(cube(bone.cubes.get(c), seed++));
                    sb.append(c + 1 < bone.cubes.size() ? ",\n" : "\n");
                }
                sb.append("\t\t\t\t\t]");
            }
            sb.append("\n\t\t\t\t}").append(b + 1 < bones.size() ? ",\n" : "\n");
        }
        sb.append("\t\t\t]\n\t\t}\n\t]\n}\n");
        Files.writeString(file.toPath(), sb.toString());
    }

    static String cube(Cube c, int seed) {
        double w = c.x1 - c.x0, h = c.y1 - c.y0, d = c.z1 - c.z0;
        StringBuilder sb = new StringBuilder("{\"origin\": ").append(vec(c.x0, c.y0, c.z0)).append(", \"size\": ").append(vec(w, h, d));
        if (c.rotation != null) {
            sb.append(", \"pivot\": ").append(vec(c.pivot)).append(", \"rotation\": ").append(vec(c.rotation));
        }
        sb.append(", \"uv\": {");
        sb.append("\"north\": ").append(face(c.front, w, h, seed)).append(", ");
        sb.append("\"south\": ").append(face(c.mat, w, h, seed + 1)).append(", ");
        sb.append("\"east\": ").append(face(c.mat, d, h, seed + 2)).append(", ");
        sb.append("\"west\": ").append(face(c.mat, d, h, seed + 3)).append(", ");
        sb.append("\"up\": ").append(face(c.mat, w, d, seed + 4)).append(", ");
        sb.append("\"down\": ").append(face(c.mat, w, d, seed + 5));
        return sb.append("}}").toString();
    }

    // Maps a face of fu x fv pixels onto its material: the swatch whose grain runs along the face's long side,
    // at DENSITY texels per pixel (stretched if the swatch is smaller), at a seed-dependent offset inside it.
    static String face(Mat m, double fu, double fv, int seed) {
        if (m == Mat.BORE || m == Mat.CHAMBERS) {
            // Muzzle / cylinder face: the whole picture, centred on the face.
            return "{\"uv\": " + vec(m.hu, m.hv) + ", \"uv_size\": " + vec(m.hw, m.hh) + "}";
        }
        boolean vertical = fv > fu;
        int su = vertical ? m.vu : m.hu, sv = vertical ? m.vv : m.hv, sw = vertical ? m.vw : m.hw, sh = vertical ? m.vh : m.hh;
        double uw = Math.min(sw, Math.max(0.5, fu * DENSITY));
        double vh = Math.min(sh, Math.max(0.5, fv * DENSITY));
        java.util.Random r = new java.util.Random(seed * 31L + 7);
        double u = su + Math.floor(r.nextDouble() * (sw - uw + 1e-9) * 2) / 2;
        double v = sv + Math.floor(r.nextDouble() * (sh - vh + 1e-9) * 2) / 2;
        return "{\"uv\": " + vec(u, v) + ", \"uv_size\": " + vec(uw, vh) + "}";
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
