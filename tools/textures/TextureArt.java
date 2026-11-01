import java.awt.image.BufferedImage;
import java.io.File;
import java.util.Random;

import javax.imageio.ImageIO;

/**
 * Procedural pixel art for Hollow Frontier (v1, replaces flat placeholders).
 * Run from the project root:  java tools/textures/TextureArt.java src/main/resources/assets/hollowfrontier/textures
 *
 * Outlaw skins use the shared GeckoLib "humanoid" model's box-UV layout (64x64):
 *   head 8x8x8 @0,0 | hat crown 8x3x8 @32,0 | brim 12x1x12 @0,32 | body 8x12x4 @16,16
 *   right arm 4x12x4 @40,16 | left arm 4x12x4 @32,48 | right leg 4x12x4 @0,16 | left leg 4x12x4 @16,48
 * Guns are 128x64 material atlases for their GeckoLib models (layout in Gun, matching tools/models/GunModels.java).
 */
public class TextureArt {
    static final int CLEAR = 0x00000000;

    public static void main(String[] args) throws Exception {
        File root = new File(args.length > 0 ? args[0] : "src/main/resources/assets/hollowfrontier/textures");
        write(new File(root, "entity/outlaw.png"), skin(Character.OUTLAW, 11));
        write(new File(root, "entity/outlaw_gunslinger.png"), skin(Character.GUNSLINGER, 23));
        write(new File(root, "entity/outlaw_boss.png"), skin(Character.BOSS, 37));
        // The man in black (11b), and his implant: a teal light behind the ear, on each side of the head (its only glow)
        BufferedImage black = skin(Character.MAN_IN_BLACK, 71);
        BufferedImage implant = new BufferedImage(SKIN_W, SKIN_H, BufferedImage.TYPE_INT_ARGB);
        for (int[] px : new int[][] {{1, 13}, {22, 13}}) {
            black.setRGB(px[0], px[1], 0xFF000000 | Creatures.PROBE_GLOW);
            implant.setRGB(px[0], px[1], 0xFF000000 | Creatures.PROBE_GLOW);
        }
        write(new File(root, "entity/man_in_black.png"), black);
        write(new File(root, "entity/man_in_black_glowmask.png"), implant);
        String[] folk = {"shopkeeper", "banker", "blacksmith", "wrangler", "prospector", "engineer", "doctor", "teacher", "preacher", "postmaster",
                "gunsmith", "editor"};
        Character[] folkLooks = {Character.TOWNSFOLK, Character.BANKER, Character.BLACKSMITH, Character.WRANGLER, Character.PROSPECTOR, Character.ENGINEER,
                Character.DOCTOR, Character.TEACHER, Character.PREACHER, Character.POSTMASTER, Character.GUNSMITH, Character.EDITOR};
        for (int i = 0; i < folk.length; i++) write(new File(root, "entity/townsfolk_" + folk[i] + ".png"), skin(folkLooks[i], 41 + i));
        String[] ranks = {"sheriff", "deputy", "marshal", "ranger"};
        Character[] lawLooks = {Character.SHERIFF, Character.DEPUTY, Character.MARSHAL, Character.RANGER};
        for (int i = 0; i < ranks.length; i++) write(new File(root, "entity/lawman_" + ranks[i] + ".png"), skin(lawLooks[i], 61 + i));
        int barmaids = Barmaids.writeAll(root);
        write(new File(root, "entity/rattlesnake.png"), Snake.skin());
        int creatures = Creatures.writeAll(root);
        int megafauna = Megafauna.writeAll(root);
        int mystery = Mystery.writeAll(root);
        int cats = Cats.writeAll(root);
        write(new File(root, "mob_effect/tipsy.png"), tipsyIcon());
        int guns = Gun.writeAll(root);
        int items = Items.writeAll(root);
        int blocks = Tiles.writeAll(root);
        int armor = Armor.writeAll(root);
        int expedition = Expedition.writeAll(root);
        int hollow = Hollow.writeAll(root);
        int starsteel = Starsteel.writeAll(root);
        int concavia = Concavia.writeAll(root);
        int ufo = Ufo.writeAll(root);
        int relics = Relics.writeAll(root); // 26-28: the Hollow sites' finds, the seal and the trim
        System.out.println("TextureArt: " + relics + " Hollow relic, sherd, trim and seal textures");
        int reworked = Reworked.writeAll(root); // last: it overwrites hand-drawn versions with ones reworked from vanilla
        System.out.println("TextureArt: wrote " + (3 + folk.length + ranks.length) + " skins, " + barmaids + " barmaid skins, " + creatures + " Hollow Earth creature textures, " + megafauna + " megafauna textures, " + mystery + " frontier mystery textures, " + cats + " cat textures, 1 effect icon, " + guns + " gun textures, " + items + " items, " + blocks + " block tiles, " + armor + " armor layers, " + expedition + " airship and polar gateway textures, " + hollow + " Hollow Earth textures, " + starsteel + " starsteel and phlogiston textures, " + concavia + " Concavia textures, " + ufo + " Gray textures, " + reworked + " reworked from vanilla to " + root);
    }

    static void write(File file, BufferedImage img) throws Exception {
        file.getParentFile().mkdirs();
        ImageIO.write(img, "png", file);
    }

    // ---------------------------------------------------------------- characters

    /**
     * bandana < 0 means none; sleeve is the arm color (the coat for outlaws, shirtsleeves for townsfolk).
     * star is a lawman's badge on the left breast, apron a work apron over the front (both < 0 for none).
     */
    record Character(int hat, int hatBand, int hair, int skin, int skinShade, int eye, int bandana, boolean maskUp,
                     int coat, int lapel, int shirt, int sleeve, int belt, int buckle, int pants, int boots, boolean mustache, boolean scar,
                     int star, int apron) {
        Character(int hat, int hatBand, int hair, int skin, int skinShade, int eye, int bandana, boolean maskUp,
                  int coat, int lapel, int shirt, int sleeve, int belt, int buckle, int pants, int boots, boolean mustache, boolean scar) {
            this(hat, hatBand, hair, skin, skinShade, eye, bandana, maskUp, coat, lapel, shirt, sleeve, belt, buckle, pants, boots, mustache, scar, -1, -1);
        }
        // Red-bandana bandit in a brown duster.
        static final Character OUTLAW = new Character(0x3B2A1E, 0x8B1A1A, 0x2E2018, 0xC68863, 0xA86E4F, 0x2B1D14,
                0x9E1B1B, true, 0x6A4A2E, 0x8A6A44, 0xB8A888, 0x6A4A2E, 0x2A1E14, 0xC9A227, 0x3E3A33, 0x2A1C12, false, false);
        // Grey-duster sharpshooter, bandana pulled down, stubble.
        static final Character GUNSLINGER = new Character(0x1E1C1A, 0x6E6A64, 0x1A1410, 0xB57A55, 0x97623F, 0x24323C,
                0x4A4F57, false, 0x5E5A55, 0x7A766F, 0x8C7B62, 0x5E5A55, 0x241A12, 0xB0B4B8, 0x2F2B26, 0x1E1610, false, false);
        // Gang leader: cream hat, black coat, red vest, gold buckle, mustache and scar.
        static final Character BOSS = new Character(0xD8CCB0, 0x1A1A1A, 0x2A1A10, 0xC08560, 0x9E6A48, 0x1C1410,
                0x1A1A1A, false, 0x1F1C1C, 0x3A3434, 0x7A1414, 0x1F1C1C, 0x141010, 0xE0B83A, 0x2A2626, 0x120C08, true, true);
        // Shopkeeper: tan hat, white shirtsleeves, green vest, brown trousers, no bandana.
        static final Character TOWNSFOLK = new Character(0x8A6440, 0x4A3220, 0x5A3A22, 0xD9A07A, 0xBE8762, 0x3A5A7A,
                -1, false, 0x2F5A3A, 0x3E7048, 0xE8E2D4, 0xE0D8C8, 0x3A2818, 0x9A9AA0, 0x6A4A30, 0x3A2616, false, false);
        // Banker: black bowler, charcoal suit, grey hair and mustache, gold watch chain.
        static final Character BANKER = new Character(0x2A2A2E, 0x6A1A1A, 0x7A7A78, 0xE0B090, 0xC49470, 0x3A4A5A,
                -1, false, 0x2A2A34, 0x3A3A48, 0xF0ECE0, 0x2A2A34, 0x1A1A1A, 0xE0B83A, 0x2A2A30, 0x141414, true, false);
        // Blacksmith: sooty hat, grey work shirt under a heavy leather apron, big mustache.
        static final Character BLACKSMITH = new Character(0x4A3A2A, 0x2A1E14, 0x1A1410, 0xB07A55, 0x93603E, 0x2B1D14,
                -1, false, 0x5E5850, 0x6E6860, 0xA89880, 0x8A8478, 0x2A1E14, 0x6A6A70, 0x3E3A33, 0x2A1C12, true, false, -1, 0x5A3A22);
        // Wrangler: straw hat, blue neckerchief, red work shirt, leather vest, denim.
        static final Character WRANGLER = new Character(0xC8A86A, 0x7A1A1A, 0x8A5A2A, 0xD8A07A, 0xBA845E, 0x3A5A7A,
                0x3A5A9A, false, 0x8A6A44, 0xA88A5A, 0xB04A3A, 0xB04A3A, 0x3A2818, 0xB0B4B8, 0x3A4A6A, 0x5A3A22, false, false);
        // Prospector: battered hat, white whiskers, red long johns under suspenders, patched trousers.
        static final Character PROSPECTOR = new Character(0x6A5A44, 0x3A2E20, 0xD8D2C4, 0xC8906A, 0xA87452, 0x3A4A3A,
                -1, false, 0x9A3A2A, 0x5A3A22, 0xB04A3A, 0x9A3A2A, 0x5A3A22, 0x8A8A80, 0x6A5A40, 0x3A2A1A, true, false);

        // Railroad engineer: pale hickory-stripe cap, red neckerchief, chambray shirt, denim overalls (the bib as an apron), mustache.
        static final Character ENGINEER = new Character(0x6A7A92, 0xE8E8E8, 0x3A2618, 0xD09A72, 0xB07E58, 0x3A4A5A,
                0x9E1B1B, false, 0x4A5A7A, 0x5A6A8A, 0x8A9AB0, 0x8A9AB0, 0x2A1E14, 0x8A8A80, 0x3A4A6A, 0x2A1C12, true, false, -1, 0x3A4A6A);

        // 24c. Doctor: black bowler and frock coat, grey hair and whiskers.
        static final Character DOCTOR = new Character(0x1E1E22, 0x3A3A40, 0x9A9A96, 0xE2B492, 0xC69676, 0x3A4A5A,
                -1, false, 0x24242A, 0x34343C, 0xF2EEE4, 0x24242A, 0x18181A, 0xB0B4B8, 0x2E2E34, 0x141414, true, false);
        // Schoolteacher: straw boater with a blue ribbon, blue dress over a white blouse.
        static final Character TEACHER = new Character(0xE0C890, 0x2A3A6A, 0x6A4426, 0xEBC0A0, 0xD0A282, 0x4A6A3A,
                -1, false, 0x2E4A7A, 0x3E5A8A, 0xF4F0E8, 0xF4F0E8, 0x22304E, 0xB0B4B8, 0x2E4A7A, 0x2A1C12, false, false);
        // Preacher: flat black hat, black coat, white collar.
        static final Character PREACHER = new Character(0x101012, 0x101012, 0x2A1A10, 0xD8A47E, 0xBC8862, 0x2B1D14,
                -1, false, 0x141416, 0x1E1E22, 0xF6F6F0, 0x141416, 0x0E0E10, 0x8A8A90, 0x18181C, 0x0E0C0A, false, false);
        // Postmaster: green eyeshade, grey vest over white shirtsleeves, mustache.
        static final Character POSTMASTER = new Character(0x2E7A3E, 0x1E5A2E, 0x5A3A22, 0xDCA684, 0xC08A66, 0x3A4A5A,
                -1, false, 0x6E6E72, 0x808086, 0xEDE8DA, 0xEDE8DA, 0x2A1E14, 0xB0B4B8, 0x3A3A40, 0x1E1610, true, false);
        // Gunsmith: brown hat, red shirt under a leather apron, mustache.
        static final Character GUNSMITH = new Character(0x5A3E26, 0x2A1E14, 0x3A2618, 0xC88E66, 0xAA7450, 0x2B1D14,
                -1, false, 0x8E3428, 0xA84434, 0xB04A3A, 0xB04A3A, 0x2A1E14, 0xC9A227, 0x3E3A33, 0x2A1C12, true, false, -1, 0x6A4A2E);
        // Editor: green eyeshade, black vest, white shirtsleeves, an ink-stained apron.
        static final Character EDITOR = new Character(0x2E7A3E, 0x1E5A2E, 0x1A1410, 0xE0AE8A, 0xC4926E, 0x24323C,
                -1, false, 0x1A1A1E, 0x2A2A30, 0xF0ECE0, 0xF0ECE0, 0x1A1A1A, 0x8A8A90, 0x2A2A30, 0x141414, false, false, -1, 0xC8C2B4);

        // A man in a black duster (11b): black hat and duster over a white shirt, pale and clean-shaven, dark eyes.
        static final Character MAN_IN_BLACK = new Character(0x141416, 0x2A2A2E, 0x121214, 0xD8CEC4, 0xBCB0A6, 0x0C0C10,
                -1, false, 0x18181C, 0x2A2A30, 0xEAEAEA, 0x18181C, 0x101012, 0xB0B4B8, 0x26262A, 0x0E0E10, false, false);

        // A Kindled (12a): white-blond and pale, ice-blue eyes, in a white robe trimmed pale blue, a phlogiston clasp (ember) at the sash.
        // The hat boxes are cleared to a circlet afterwards (Concavia.kindled).
        static final Character KINDLED = new Character(0xF0EEE6, 0xD8C070, 0xF2EAC8, 0xF4E8DE, 0xDCCCC0, 0x6FD8F2,
                -1, false, 0xECF2F6, 0xA8CCE0, 0xDCE8F0, 0xECF2F6, 0x8FB8D0, 0xF58A2C, 0xE2ECF2, 0xB8C8D4, false, false);

        // Lawmen all wear a badge. Sheriff: dark hat, black vest over white shirtsleeves, gold star, mustache.
        static final Character SHERIFF = new Character(0x3A2A1E, 0x1A1A1A, 0x4A3A2A, 0xD09A72, 0xB07E58, 0x3A5A7A,
                -1, false, 0x2A2420, 0x3A322C, 0xE8E2D4, 0xE0D8C8, 0x3A2818, 0xC9A227, 0x5A4A3A, 0x2A1C12, true, false, 0xE8C840, -1);
        // Deputy: tan hat, ginger hair, brown vest over a blue chambray shirt, silver star.
        static final Character DEPUTY = new Character(0x8A6A44, 0x4A3220, 0xA0602A, 0xE0AA80, 0xC28E64, 0x4A6A3A,
                -1, false, 0x6A4A2E, 0x8A6A44, 0x7A8AA0, 0x7A8AA0, 0x3A2818, 0xB0B4B8, 0x4A3A2A, 0x2A1C12, false, false, 0xD0D4D8, -1);
        // U.S. Marshal: black hat and long black coat, mustache, silver star.
        static final Character MARSHAL = new Character(0x1A1A1A, 0x4A4A4A, 0x2A1A10, 0xB57A55, 0x97623F, 0x24323C,
                -1, false, 0x26262A, 0x3E3E44, 0xD8D0C0, 0x26262A, 0x141010, 0xB0B4B8, 0x3A3434, 0x120C08, true, false, 0xD0D4D8, -1);
        // Ranger: cream hat, red neckerchief, buckskin jacket, silver star-in-a-circle.
        static final Character RANGER = new Character(0xD8CCB0, 0x5A3A22, 0x3A2618, 0xC68863, 0xA86E4F, 0x2B1D14,
                0x9E1B1B, false, 0x9A7A4E, 0xB89A6A, 0xC8B8A0, 0x9A7A4E, 0x3A2818, 0xB0B4B8, 0x5A4A3A, 0x3A2616, false, false, 0xC0C4C8, -1);
    }

    /** The humanoid skin's size: the 64x64 player-style layout, with the coat tails, bandana and brim curls below it (31c). */
    static final int SKIN_W = 64, SKIN_H = 96;

    /**
     * What hangs below the belt: a dress or a robe (none: -1, the box left clear). Duster and frock-coat tails were tried
     * on the coat wearers and held off on request; the painting below still handles them.
     */
    static int tails(Character c) {
        if (c == Character.TEACHER || c == Character.KINDLED) {
            return c.coat;
        }
        return -1;
    }

    static BufferedImage skin(Character c, long seed) {
        BufferedImage img = new BufferedImage(SKIN_W, SKIN_H, BufferedImage.TYPE_INT_ARGB);
        Random rnd = new Random(seed);
        Painter p = new Painter(img, rnd);

        // Head 8x8x8 @0,0
        Box head = new Box(0, 0, 8, 8, 8);
        p.face(head.top(), c.hair, 10);
        p.face(head.bottom(), c.skinShade, 6);
        paintFace(p, head.front(), c);
        paintHeadSide(p, head.east(), c, false);
        paintHeadSide(p, head.west(), c, true);
        paintHeadBack(p, head.back(), c);

        // Hat crown 8x3x8 @32,0 and brim 12x1x12 @0,32: felt, the crown pinched along the middle, the brim lit at its edge
        Box crown = new Box(32, 0, 8, 3, 8);
        p.face(crown.top(), shade(c.hat, 0.9), 4);
        for (int y = 1; y < 7; y++) p.set(crown.top().x + 3 + (y > 3 ? 1 : 0), crown.top().y + y, shade(c.hat, 0.68));
        for (Rect side : new Rect[] {crown.front(), crown.back(), crown.east(), crown.west()}) {
            cloth(p, side, c.hat, side.equals(crown.front()) ? 1.0 : 0.9);
            p.row(side, side.h - 1, c.hatBand);
        }
        // 31c: the brim is a flat middle 8x1x12 @0,32 and a curl 2x1x12 @0,76 each side (mirrored), turned up
        Box brim = new Box(0, 32, 8, 1, 12);
        p.face(brim.top(), c.hat, 4);
        p.row(brim.top(), 0, shade(c.hat, 1.15)); // the front and back edges lit; the sides run on into the curls
        p.row(brim.top(), brim.top().h() - 1, shade(c.hat, 1.15));
        p.face(brim.bottom(), shade(c.hat, 0.65), 3);
        for (Rect side : new Rect[] {brim.front(), brim.back(), brim.east(), brim.west()}) {
            p.face(side, shade(c.hat, 0.75), 2);
        }
        Box curl = new Box(0, 76, 2, 1, 12);
        p.face(curl.top(), shade(c.hat, 0.95), 4);
        for (int y = 0; y < curl.top().h(); y++) p.set(curl.top().x(), curl.top().y() + y, shade(c.hat, 1.15)); // the rolled edge (outer, mirrored the other side)
        p.row(curl.top(), 0, shade(c.hat, 1.15));
        p.row(curl.top(), curl.top().h() - 1, shade(c.hat, 1.15));
        p.face(curl.bottom(), shade(c.hat, 0.6), 3);
        for (Rect side : new Rect[] {curl.front(), curl.back(), curl.east(), curl.west()}) {
            p.face(side, shade(c.hat, 0.75), 2);
        }

        // 31c: the bandana 8x3x8 @24,64 round the lower face (or just the neck, pulled down) and its hanging tip 3x2 @56,64
        if (c.bandana >= 0) {
            Box band = new Box(24, 64, 8, 3, 8);
            int from = c.maskUp ? 0 : 2;
            for (Rect side : new Rect[] {band.front(), band.east(), band.west(), band.back()}) {
                for (int y = from; y < 3; y++) {
                    for (int x = 0; x < side.w(); x++) p.set(side.x() + x, side.y() + y, Tiles.jitter(shade(c.bandana, 1.0 - 0.06 * y), rnd, 2));
                }
                if (c.maskUp) p.set(side.x() + side.w() / 2, side.y() + 1, shade(c.bandana, 0.72)); // a fold
            }
            if (c.maskUp) { // the knot at the back
                p.set(band.back().x() + 3, band.back().y() + 1, shade(c.bandana, 0.62));
                p.set(band.back().x() + 4, band.back().y() + 1, shade(c.bandana, 0.62));
            }
            p.face(band.bottom(), shade(c.bandana, 0.7), 3);
            for (Rect side : new Rect[] {new Box(56, 64, 3, 2, 0).front(), new Box(56, 64, 3, 2, 0).back()}) {
                p.row(side, 0, shade(c.bandana, 0.9));
                p.set(side.x() + 1, side.y() + 1, shade(c.bandana, 0.8));
            }
        }

        // 31c: coat tails, a dress or a robe 8x8x4 @0,64: open in front (the legs show through), round the sides and back
        int tail = tails(c);
        if (tail >= 0) {
            Box tails = new Box(0, 64, 8, 8, 4);
            boolean robe = c == Character.KINDLED, dress = c == Character.TEACHER;
            for (Rect side : new Rect[] {tails.east(), tails.west(), tails.back()}) {
                cloth(p, side, tail, side.equals(tails.back()) ? 0.9 : 0.84);
                p.row(side, side.h() - 1, robe ? c.lapel : dress ? shade(c.shirt, 0.95) : shade(tail, 0.68)); // the hem
            }
            if (!robe && !dress) { // the vent up the back of a coat
                for (int y = 0; y < tails.back().h() - 1; y++) p.set(tails.back().x() + 3, tails.back().y() + y, shade(tail, 0.66));
            }
            for (Rect front : new Rect[] {tails.east(), tails.west()}) { // the open front edges, where the coat folds back
                p.set(front.x() + (front.equals(tails.east()) ? front.w() - 1 : 0), front.y(), c.lapel);
            }
        }

        // Body 8x12x4 @16,16: woven cloth, its sides in shadow
        Box body = new Box(16, 16, 8, 12, 4);
        paintBodyFront(p, body.front(), c);
        cloth(p, body.back(), c.coat, 0.94);
        p.row(body.back(), 8, c.belt);
        for (int y = 9; y < 12; y++) p.set(body.back().x + 3, body.back().y + y, shade(c.coat, 0.7)); // coat vent
        for (Rect side : new Rect[] {body.east(), body.west()}) {
            cloth(p, side, c.coat, 0.86);
            p.row(side, 8, c.belt);
        }
        p.face(body.top(), shade(c.coat, 1.1), 3);
        p.face(body.bottom(), shade(c.coat, 0.7), 3);

        // Arms 4x12x4 @40,16 and @32,48: lit in front, darker behind, a folded cuff, the hands
        for (Box arm : new Box[] {new Box(40, 16, 4, 12, 4), new Box(32, 48, 4, 12, 4)}) {
            Rect[] sides = {arm.front(), arm.east(), arm.west(), arm.back()};
            double[] light = {1.0, 0.92, 0.92, 0.84};
            for (int i = 0; i < sides.length; i++) {
                Rect side = sides[i];
                cloth(p, side, c.sleeve, light[i]);
                p.row(side, 4, shade(c.sleeve, 0.86 * light[i])); // the crook of the elbow
                p.row(side, 9, shade(c.sleeve, 0.72));
                p.face(new Rect(side.x, side.y + 10, side.w, 2), shade(c.skin, light[i]), 3);
                p.row(side, 11, shade(c.skinShade, light[i]));
            }
            p.face(arm.top(), shade(c.sleeve, 1.1), 3);
            p.face(arm.bottom(), c.skinShade, 2);
        }

        // Legs 4x12x4 @0,16 and @16,48: trousers creased at the knee, boots with a turned-down top, a lit toe and a dark sole
        for (Box leg : new Box[] {new Box(0, 16, 4, 12, 4), new Box(16, 48, 4, 12, 4)}) {
            Rect[] sides = {leg.front(), leg.east(), leg.west(), leg.back()};
            double[] light = {1.0, 0.9, 0.9, 0.84};
            for (int i = 0; i < sides.length; i++) {
                Rect side = sides[i];
                cloth(p, side, c.pants, light[i]);
                p.set(side.x + 1, side.y + 5, shade(c.pants, 0.82 * light[i])); // the knee
                p.set(side.x + 2, side.y + 5, shade(c.pants, 0.82 * light[i]));
                p.face(new Rect(side.x, side.y + 8, side.w, 4), shade(c.boots, light[i]), 3);
                p.row(side, 8, shade(c.boots, 1.45 * light[i]));
                p.row(side, 11, shade(c.boots, 0.6));
            }
            p.set(leg.front().x + 1, leg.front().y + 10, shade(c.boots, 1.3)); // the toe catching the light
            p.set(leg.front().x + 2, leg.front().y + 10, shade(c.boots, 1.2));
            p.set(leg.back().x + 1, leg.back().y + 11, 0xB0B4B8); // spur
            p.face(leg.top(), c.pants, 3);
            p.face(leg.bottom(), shade(c.boots, 0.6), 2);
        }
        return img;
    }

    /**
     * Woven cloth: brightest at the top of the face and a shade darker toward the hem, every other column a touch darker
     * for the weave, with only a little noise; {@code light} dims a face turned from the light (sides, backs).
     */
    static void cloth(Painter p, Rect r, int base, double light) {
        for (int x = 0; x < r.w; x++) {
            for (int y = 0; y < r.h; y++) {
                double t = light * (1.06 - 0.14 * y / Math.max(1, r.h - 1)) * (x % 2 == 0 ? 1.0 : 0.96);
                p.set(r.x + x, r.y + y, Tiles.jitter(shade(base, t), p.rnd, 2));
            }
        }
    }

    static void paintFace(Painter p, Rect f, Character c) {
        p.face(f, c.skin, 6);
        p.row(f, 0, c.hair);
        p.set(f.x, f.y + 1, c.hair);
        p.set(f.x + 7, f.y + 1, c.hair);
        // brows
        p.set(f.x + 1, f.y + 2, c.hair);
        p.set(f.x + 2, f.y + 2, c.hair);
        p.set(f.x + 5, f.y + 2, c.hair);
        p.set(f.x + 6, f.y + 2, c.hair);
        // eyes: white + iris, squinting slightly
        p.set(f.x + 1, f.y + 3, 0xE8E0D0);
        p.set(f.x + 2, f.y + 3, c.eye);
        p.set(f.x + 5, f.y + 3, c.eye);
        p.set(f.x + 6, f.y + 3, 0xE8E0D0);
        // nose
        p.set(f.x + 3, f.y + 4, c.skinShade);
        p.set(f.x + 4, f.y + 4, c.skinShade);
        if (c.maskUp) {
            for (int y = 5; y < 8; y++) p.row(f, y, c.bandana);
            p.set(f.x + 3, f.y + 7, shade(c.bandana, 0.7)); // fold
            p.set(f.x + 4, f.y + 6, shade(c.bandana, 0.7));
        } else if (c.bandana >= 0) {
            // mouth + stubble, bandana around the neck on the bottom row
            p.set(f.x + 3, f.y + 6, shade(c.skin, 0.55));
            p.set(f.x + 4, f.y + 6, shade(c.skin, 0.55));
            for (int x = 1; x < 7; x++) if ((x + 5) % 2 == 0) p.set(f.x + x, f.y + 5, c.skinShade);
            p.row(f, 7, c.bandana);
        } else if (c == Character.MAN_IN_BLACK) {
            // clean-shaven and giving nothing away: a straight, closed mouth
            p.set(f.x + 3, f.y + 6, shade(c.skin, 0.6));
            p.set(f.x + 4, f.y + 6, shade(c.skin, 0.6));
        } else {
            // clean-shaven, friendly smile
            p.set(f.x + 2, f.y + 5, shade(c.skin, 0.6));
            p.set(f.x + 3, f.y + 6, shade(c.skin, 0.6));
            p.set(f.x + 4, f.y + 6, shade(c.skin, 0.6));
            p.set(f.x + 5, f.y + 5, shade(c.skin, 0.6));
            p.set(f.x + 1, f.y + 4, shade(c.skin, 1.08)); // cheeks
            p.set(f.x + 6, f.y + 4, shade(c.skin, 1.08));
        }
        if (c.mustache) {
            p.set(f.x + 2, f.y + 5, c.hair);
            p.set(f.x + 3, f.y + 5, c.hair);
            p.set(f.x + 4, f.y + 5, c.hair);
            p.set(f.x + 5, f.y + 5, c.hair);
            p.set(f.x + 2, f.y + 6, c.hair);
            p.set(f.x + 5, f.y + 6, c.hair);
        }
        if (c.scar) {
            p.set(f.x + 6, f.y + 2, 0xD9A08A);
            p.set(f.x + 6, f.y + 4, 0xD9A08A);
            p.set(f.x + 7, f.y + 5, 0xD9A08A);
        }
    }

    /**
     * A side of the head. In box UV the east strip's right edge meets the face and the west strip's right edge meets
     * the back of the head, so the hair at the back (and the ear) is mirrored on the east side; otherwise it reads as
     * a long sideburn down the cheek.
     */
    static void paintHeadSide(Painter p, Rect f, Character c, boolean backOnRight) {
        p.face(f, c.skin, 6);
        p.row(f, 0, c.hair);
        p.row(f, 1, c.hair);
        int nape = backOnRight ? 7 : 0, napeIn = backOnRight ? 6 : 1, ear = backOnRight ? 3 : 4;
        for (int y = 2; y < 6; y++) p.set(f.x + napeIn, f.y + y, c.hair);
        for (int y = 2; y < 7; y++) p.set(f.x + nape, f.y + y, c.hair);
        p.set(f.x + ear, f.y + 3, c.skinShade);
        p.set(f.x + ear, f.y + 4, c.skinShade);
        if (c.maskUp) {
            for (int y = 5; y < 8; y++) p.row(f, y, c.bandana);
        } else if (c.bandana >= 0) {
            p.row(f, 7, c.bandana);
        }
    }

    static void paintHeadBack(Painter p, Rect f, Character c) {
        p.face(f, c.hair, 10);
        if (c.bandana >= 0) {
            p.row(f, 7, c.bandana);
        }
        if (c.maskUp) {
            p.row(f, 5, c.bandana);
            p.set(f.x + 3, f.y + 6, c.bandana); // knot
            p.set(f.x + 4, f.y + 6, shade(c.bandana, 0.7));
        }
    }

    static void paintBodyFront(Painter p, Rect f, Character c) {
        cloth(p, f, c.coat, 1.0);
        for (int y = 0; y < 8; y++) {
            p.set(f.x + 3, f.y + y, c.shirt);
            p.set(f.x + 4, f.y + y, c.shirt);
        }
        for (int y = 0; y < 5; y++) { // lapels
            p.set(f.x + 2, f.y + y, c.lapel);
            p.set(f.x + 5, f.y + y, c.lapel);
        }
        int neck = c.bandana >= 0 ? c.bandana : c.hatBand; // neckerchief, or a string tie
        p.set(f.x + 3, f.y, neck);
        p.set(f.x + 4, f.y, neck);
        if (c.bandana < 0) {
            p.set(f.x + 3, f.y + 1, neck);
            p.set(f.x + 4, f.y + 2, neck);
        }
        p.set(f.x + 3, f.y + 3, shade(c.shirt, 0.7)); // buttons
        p.set(f.x + 3, f.y + 5, shade(c.shirt, 0.7));
        p.row(f, 8, c.belt);
        p.set(f.x + 3, f.y + 8, c.buckle);
        p.set(f.x + 4, f.y + 8, c.buckle);
        for (int y = 9; y < 12; y++) { // duster tails open over the trousers
            p.set(f.x + 3, f.y + y, c.pants);
            p.set(f.x + 4, f.y + y, c.pants);
        }
        p.set(f.x + 1, f.y + 7, shade(c.coat, 0.7)); // pocket
        p.set(f.x + 6, f.y + 7, shade(c.coat, 0.7));
        if (c.apron >= 0) { // bib apron from the chest to the knees, on neck straps
            for (int y = 2; y < 12; y++) for (int x = 1; x < 7; x++) p.set(f.x + x, f.y + y, shade(c.apron, 0.92 + ((x + y) % 3) * 0.04));
            p.set(f.x + 1, f.y + 1, c.apron);
            p.set(f.x + 6, f.y + 1, c.apron);
            for (int x : new int[] {1, 2, 5, 6}) p.set(f.x + x, f.y + 7, shade(c.apron, 0.7)); // pocket seams
        }
        if (c.star >= 0) { // badge on the left breast (the viewer's right)
            p.set(f.x + 6, f.y + 2, c.star);
            p.set(f.x + 5, f.y + 3, c.star);
            p.set(f.x + 6, f.y + 3, shade(c.star, 1.2));
            p.set(f.x + 7, f.y + 3, c.star);
            p.set(f.x + 5, f.y + 4, shade(c.star, 0.75));
            p.set(f.x + 7, f.y + 4, shade(c.star, 0.75));
        }
    }

    // ---------------------------------------------------------------- barmaids
    // 128x64 skins for tools/models/BarmaidModels.java (box UV origins below must match its constants).

    static final class Barmaids {
        static final int NONE = -1;
        static final int PLAIN = 0, GINGHAM = 1, STRIPES = 2, SATIN = 3, BANDS = 4;

        /**
         * One hostess's look. lace trims the neckline and hem; apron/choker/gloves/wing are optional (NONE/false).
         * stockings2 is a second stripe color (NONE = plain). accent colors her flower, bow or hat feather.
         */
        record Look(String id, long seed, int skin, int eye, int lips, int hair, int brow,
                    int dress, int pattern, int corset, int lace, int sleeve, int apron,
                    int stockings, int stockings2, int boots, int accent, int hat,
                    boolean freckles, boolean beautyMark, boolean wing, int choker, int gloves, int earrings, boolean longHairSides) {}

        static final Look[] LOOKS = {
                // Miss Ruby: crimson satin, black lace corset and gloves, raven waves, a red rose, beauty mark
                new Look("ruby", 71, 0xF0C8A8, 0x3A7A4A, 0xB0182A, 0x1E1418, 0x140E10,
                        0xA8182A, SATIN, 0x1A1416, 0x161012, 0x1A1416, NONE,
                        0x141014, NONE, 0x1A1010, 0xC81830, NONE,
                        false, true, true, 0x101010, 0x161214, NONE, true),
                // Clementine: yellow gingham, white blouse and frilled apron, strawberry-blonde ponytail, freckles, pink bow
                new Look("clementine", 72, 0xF4CDB0, 0x4A7AB8, 0xD86070, 0xD8783A, 0xA8562A,
                        0xE8C040, GINGHAM, 0xC8862A, 0xFAF6EC, 0xFAF6EC, 0xFAF6EC,
                        0xF0ECE0, NONE, 0x6A4426, 0xE85A8A, NONE,
                        true, false, false, NONE, NONE, NONE, false),
                // Josie: emerald corset over a black lace skirt, striped stockings, chestnut ringlets, feathered hat
                new Look("josie", 73, 0xD9A27A, 0x5A3A1E, 0x9A2440, 0x6A3A1E, 0x3A2010,
                        0x1C1C22, STRIPES, 0x1E7A4A, 0xD8D0C0, 0x1E7A4A, NONE,
                        0x1A1A1E, 0x1E7A4A, 0x141414, 0x6A2A7A, 0x1E5A3A,
                        false, false, true, NONE, NONE, NONE, false),
                // Miss Belle: lavender pinstripe, white lace sleeves, pearl choker, platinum bun with a violet ribbon
                new Look("belle", 74, 0xF6DCCB, 0x6A8AC8, 0xC86A7A, 0xEADBA0, 0xB8A070,
                        0xB8A0D8, STRIPES, 0x8A6AB8, 0xFAF8F4, 0xFAF8F4, NONE,
                        0xF4F0F4, NONE, 0x3A2A3A, 0x8A6AB8, NONE,
                        false, false, false, 0xF0EEE8, NONE, NONE, false),
                // Lupe: white peasant blouse, black corset, tiered turquoise skirt with bright bands, braid, marigold, gold hoops
                new Look("lupe", 75, 0xB07A52, 0x3A2214, 0xA82A3A, 0x16100E, 0x100A08,
                        0x2AA8A0, BANDS, 0x221A18, 0xF0A030, 0xFAF6EC, NONE,
                        0xB07A52, NONE, 0x5A3A22, 0xF09A1E, NONE,
                        false, false, true, NONE, NONE, 0xE0B83A, true),
        };

        static int writeAll(File root) throws Exception {
            for (Look look : LOOKS) write(new File(root, "entity/barmaid_" + look.id() + ".png"), paint(look));
            return LOOKS.length;
        }

        static BufferedImage paint(Look l) {
            BufferedImage img = new BufferedImage(128, 64, BufferedImage.TYPE_INT_ARGB);
            Painter p = new Painter(img, new Random(l.seed()));
            int skinShade = shade(l.skin(), 0.88), hairShade = shade(l.hair(), 0.78);

            // Head 8x8x8 @0,0: skin, hair on top and back
            Box head = new Box(0, 0, 8, 8, 8);
            p.face(head.top(), l.hair(), 6);
            p.face(head.bottom(), skinShade, 3);
            p.face(head.back(), l.hair(), 6);
            for (Rect side : new Rect[] {head.east(), head.west()}) {
                p.face(side, l.skin(), 3);
                p.row(side, 0, l.hair());
                for (int y = 0; y < 8; y++) for (int x = 5; x < 8; x++) p.set(side.x() + x, side.y() + y, l.hair());
                p.set(side.x() + 3, side.y() + 4, skinShade); // ear
                if (l.earrings() != NONE) {
                    p.set(side.x() + 3, side.y() + 5, l.earrings());
                    p.set(side.x() + 3, side.y() + 6, shade(l.earrings(), 0.8));
                }
            }
            paintFace(p, head.front(), l, skinShade);

            // Hair shell (head inflated by 0.5) @32,0: only the hair is painted, the rest is see-through
            Box cap = new Box(32, 0, 8, 8, 8);
            p.face(cap.top(), l.hair(), 8);
            p.face(cap.back(), l.hair(), 8);
            for (int x = 0; x < 8; x += 2) p.set(cap.back().x() + x, cap.back().y() + 7, hairShade);
            for (Rect side : new Rect[] {cap.east(), cap.west()}) {
                int rows = l.longHairSides() ? 8 : 4;
                for (int y = 0; y < rows; y++) for (int x = 4; x < 8; x++) p.set(side.x() + x, side.y() + y, y % 3 == 2 ? hairShade : l.hair());
                for (int x = 0; x < 4; x++) p.set(side.x() + x, side.y(), l.hair());
                for (int x = 1; x < 4; x++) p.set(side.x() + x, side.y() + 1, l.hair());
            }
            Rect fringe = cap.front();
            for (int x = 0; x < 8; x++) p.set(fringe.x() + x, fringe.y(), l.hair());
            for (int x : new int[] {0, 1, 2, 5, 6, 7}) p.set(fringe.x() + x, fringe.y() + 1, l.hair()); // center part
            p.set(fringe.x(), fringe.y() + 2, l.hair());
            p.set(fringe.x() + 7, fringe.y() + 2, l.hair());
            if (l.longHairSides()) { // locks framing the face
                for (int y = 3; y < 7; y++) {
                    p.set(fringe.x(), fringe.y() + y, y % 2 == 0 ? hairShade : l.hair());
                    p.set(fringe.x() + 7, fringe.y() + y, y % 2 == 0 ? hairShade : l.hair());
                }
            }

            // Corset bodice: sweetheart neckline edged in lace, laced up the front
            Box chest = new Box(64, 0, 8, 6, 4);
            Rect cf = chest.front();
            p.face(cf, l.corset(), 6);
            for (int x = 0; x < 8; x++) p.set(cf.x() + x, cf.y(), l.skin());
            for (int x = 2; x < 6; x++) p.set(cf.x() + x, cf.y() + 1, l.skin());
            for (int x : new int[] {0, 1, 6, 7}) p.set(cf.x() + x, cf.y() + 1, l.sleeve());
            for (int x = 0; x < 8; x++) p.set(cf.x() + x, cf.y() + 2, (x == 3 || x == 4) ? l.skin() : l.lace());
            p.set(cf.x() + 3, cf.y() + 3, l.lace());
            p.set(cf.x() + 4, cf.y() + 3, l.lace());
            lacing(p, cf.x() + 3, cf.y() + 4, 2, l);
            if (l.choker() != NONE) {
                p.set(cf.x() + 3, cf.y(), l.choker());
                p.set(cf.x() + 4, cf.y(), l.choker());
            }
            for (Rect side : new Rect[] {chest.east(), chest.west()}) {
                p.face(side, l.corset(), 6);
                p.row(side, 0, l.sleeve());
                p.row(side, 1, l.lace());
            }
            p.face(chest.back(), l.corset(), 6);
            p.row(chest.back(), 0, l.skin());
            p.row(chest.back(), 1, l.lace());
            lacing(p, chest.back().x() + 3, chest.back().y() + 2, 4, l);
            p.face(chest.top(), l.skin(), 3);
            for (int y = 0; y < 4; y++) {
                p.set(chest.top().x(), chest.top().y() + y, l.sleeve());
                p.set(chest.top().x() + 7, chest.top().y() + y, l.sleeve());
            }
            p.face(chest.bottom(), l.corset(), 4);

            // Shaped bodice front 7x3x1 @88,0
            Box bodice = new Box(88, 0, 7, 3, 1);
            for (Rect r : new Rect[] {bodice.front(), bodice.east(), bodice.west(), bodice.back(), bodice.bottom()}) p.face(r, l.corset(), 5);
            p.row(bodice.front(), 0, l.lace());
            p.set(bodice.front().x() + 3, bodice.front().y(), l.skin());
            lacing(p, bodice.front().x() + 3, bodice.front().y() + 1, 2, l);
            p.face(bodice.top(), l.lace(), 3);
            if (l.choker() != NONE) p.set(bodice.front().x() + 3, bodice.front().y(), l.choker()); // pendant

            // Cinched waist 6x4x3 @64,10
            Box waist = new Box(64, 10, 6, 4, 3);
            for (Rect r : new Rect[] {waist.front(), waist.east(), waist.west(), waist.back(), waist.top()}) p.face(r, l.corset(), 5);
            lacing(p, waist.front().x() + 2, waist.front().y(), 4, l);
            lacing(p, waist.back().x() + 2, waist.back().y(), 4, l);
            p.face(waist.bottom(), l.dress(), 4);

            // Skirt: upper 9x6x6 @0,16, lower 11x7x7 @30,16, lace hem 12x1x8 @66,17
            Box upper = new Box(0, 16, 9, 6, 6), lower = new Box(30, 16, 11, 7, 7), hem = new Box(66, 17, 12, 1, 8);
            for (Rect r : new Rect[] {upper.front(), upper.east(), upper.west(), upper.back()}) {
                cloth(p, r, l.dress(), l.pattern(), l);
                p.row(r, 0, l.corset()); // waistband
            }
            p.face(upper.top(), l.corset(), 4);
            p.face(upper.bottom(), shade(l.dress(), 0.6), 3);
            for (Rect r : new Rect[] {lower.front(), lower.east(), lower.west(), lower.back()}) {
                cloth(p, r, l.dress(), l.pattern(), l);
                for (int x = 1; x < r.w(); x += 3) for (int y = 0; y < r.h(); y++) { // fold shadows
                    p.set(r.x() + x, r.y() + y, shade(img.getRGB(r.x() + x, r.y() + y) & 0xFFFFFF, 0.82));
                }
            }
            p.face(lower.top(), l.dress(), 4);
            p.face(lower.bottom(), shade(l.dress(), 0.55), 3);
            if (l.apron() != NONE) {
                p.face(new Rect(upper.front().x() + 2, upper.front().y() + 1, 5, 5), l.apron(), 3);
                Rect lf = lower.front();
                p.face(new Rect(lf.x() + 2, lf.y(), 7, 5), l.apron(), 3);
                for (int x = 2; x < 9; x += 2) p.set(lf.x() + x, lf.y() + 5, l.apron()); // frill
                p.row(upper.back(), 0, l.apron()); // apron strings
                p.set(upper.back().x() + 4, upper.back().y() + 1, l.apron());
            }
            for (Rect r : new Rect[] {hem.front(), hem.east(), hem.west(), hem.back()}) {
                for (int x = 0; x < r.w(); x++) p.set(r.x() + x, r.y(), x % 2 == 0 ? l.lace() : shade(l.lace(), 0.85));
            }
            p.face(hem.top(), l.lace(), 3);
            p.face(hem.bottom(), shade(l.lace(), 0.75), 3);

            // Slim arms 3x12x3 @0,30 / @12,30 under puffed sleeves 4x4x4 @48,30 / @64,30
            for (Box arm : new Box[] {new Box(0, 30, 3, 12, 3), new Box(12, 30, 3, 12, 3)}) {
                for (Rect side : new Rect[] {arm.front(), arm.back(), arm.east(), arm.west()}) {
                    p.face(side, l.skin(), 3);
                    if (l.gloves() != NONE) {
                        p.face(new Rect(side.x(), side.y() + 6, side.w(), 6), l.gloves(), 4);
                        for (int x = 0; x < side.w(); x++) p.set(side.x() + x, side.y() + 6, shade(l.gloves(), 1.6)); // lace cuff
                    }
                    if (l.earrings() != NONE) p.row(side, 9, l.earrings()); // bangle
                }
                p.face(arm.top(), l.sleeve(), 3);
                p.face(arm.bottom(), l.gloves() != NONE ? l.gloves() : skinShade, 3);
            }
            for (Box puff : new Box[] {new Box(48, 30, 4, 4, 4), new Box(64, 30, 4, 4, 4)}) {
                for (Rect side : new Rect[] {puff.front(), puff.back(), puff.east(), puff.west()}) {
                    p.face(side, l.sleeve(), 5);
                    p.row(side, 0, shade(l.sleeve(), 1.08));
                    p.row(side, 3, l.lace());
                }
                p.face(puff.top(), l.sleeve(), 4);
                p.face(puff.bottom(), l.lace(), 3);
            }

            // Legs 3x12x3 @24,30 / @36,30: stockings (striped for some) and buttoned boots
            for (Box leg : new Box[] {new Box(24, 30, 3, 12, 3), new Box(36, 30, 3, 12, 3)}) {
                for (Rect side : new Rect[] {leg.front(), leg.back(), leg.east(), leg.west()}) {
                    for (int y = 0; y < 9; y++) {
                        int c = l.stockings2() != NONE && y % 2 == 1 ? l.stockings2() : l.stockings();
                        for (int x = 0; x < side.w(); x++) p.set(side.x() + x, side.y() + y, c);
                    }
                    p.face(new Rect(side.x(), side.y() + 9, side.w(), 3), l.boots(), 4);
                    p.set(side.x() + 1, side.y() + 9, shade(l.boots(), 1.5)); // shine
                }
                p.face(leg.top(), l.stockings(), 2);
                p.face(leg.bottom(), shade(l.boots(), 0.7), 2);
            }

            // Hair pieces: long back 9x16x2 @80,26, bun 4x3x4 @102,26, tail/braid 3x10x2 @102,33, ringlets 2x6x2 @112,33/41
            Box longHair = new Box(80, 26, 9, 16, 2);
            for (Rect r : new Rect[] {longHair.front(), longHair.back(), longHair.east(), longHair.west(), longHair.top()}) {
                p.face(r, l.hair(), 6);
                for (int x = 1; x < r.w(); x += 2) for (int y = 0; y < r.h(); y++) if ((y + x) % 4 != 0) p.set(r.x() + x, r.y() + y, hairShade);
            }
            for (int x = 0; x < 9; x += 2) { // wavy ends
                p.set(longHair.front().x() + x, longHair.front().y() + 15, CLEAR);
                p.set(longHair.back().x() + x, longHair.back().y() + 15, CLEAR);
            }
            p.face(longHair.bottom(), hairShade, 4);
            Box bun = new Box(102, 26, 4, 3, 4);
            for (Rect r : new Rect[] {bun.front(), bun.back(), bun.east(), bun.west(), bun.top(), bun.bottom()}) {
                p.face(r, l.hair(), 6);
                p.set(r.x() + 1, r.y() + 1, hairShade);
                if (r.w() > 2) p.set(r.x() + 2, r.y() + Math.min(2, r.h() - 1), hairShade);
            }
            Box tail = new Box(102, 33, 3, 10, 2);
            for (Rect r : new Rect[] {tail.front(), tail.back(), tail.east(), tail.west()}) {
                for (int y = 0; y < r.h(); y++) for (int x = 0; x < r.w(); x++) {
                    p.set(r.x() + x, r.y() + y, (x + y) % 2 == 0 ? l.hair() : hairShade); // braided/twisted
                }
                p.row(r, 0, l.accent()); // tie
                p.row(r, r.h() - 1, hairShade);
            }
            p.face(tail.top(), l.accent(), 2);
            p.face(tail.bottom(), hairShade, 3);
            for (Box curl : new Box[] {new Box(112, 33, 2, 6, 2), new Box(112, 41, 2, 6, 2)}) {
                for (Rect r : new Rect[] {curl.front(), curl.back(), curl.east(), curl.west(), curl.top(), curl.bottom()}) {
                    p.face(r, l.hair(), 6);
                    for (int y = 1; y < r.h(); y += 2) p.row(r, y, hairShade); // ringlet coils
                }
            }

            // Hat (crown 5x2x5 @48,38, brim 7x1x7 @0,45), feather 1x5x2 @28,45, flower 1x2x2 @34,45, bow 4x2x1 @40,45
            int hat = l.hat() != NONE ? l.hat() : l.accent();
            Box crown = new Box(48, 38, 5, 2, 5), brim = new Box(0, 45, 7, 1, 7);
            for (Rect r : new Rect[] {crown.front(), crown.back(), crown.east(), crown.west()}) {
                p.face(r, hat, 5);
                p.row(r, 1, 0x141414); // band
            }
            p.face(crown.top(), shade(hat, 1.1), 4);
            p.face(crown.bottom(), shade(hat, 0.7), 2);
            for (Rect r : new Rect[] {brim.front(), brim.back(), brim.east(), brim.west()}) p.face(r, shade(hat, 0.8), 3);
            p.face(brim.top(), hat, 5);
            p.face(brim.bottom(), shade(hat, 0.6), 3);
            Box feather = new Box(28, 45, 1, 5, 2);
            for (Rect r : new Rect[] {feather.front(), feather.back(), feather.east(), feather.west(), feather.top(), feather.bottom()}) {
                for (int y = 0; y < r.h(); y++) for (int x = 0; x < r.w(); x++) p.set(r.x() + x, r.y() + y, shade(l.accent(), 1.3 - 0.1 * y));
            }
            Box flower = new Box(34, 45, 1, 2, 2);
            for (Rect r : new Rect[] {flower.front(), flower.back(), flower.east(), flower.west(), flower.top(), flower.bottom()}) {
                p.face(r, l.accent(), 6);
            }
            p.set(flower.east().x(), flower.east().y(), shade(l.accent(), 0.7));
            p.set(flower.west().x() + 1, flower.west().y() + 1, 0x3E7048); // leaf
            p.set(flower.west().x(), flower.west().y(), 0xF0D040);         // heart of the flower
            Box bow = new Box(40, 45, 4, 2, 1);
            for (Rect r : new Rect[] {bow.front(), bow.back(), bow.east(), bow.west(), bow.top(), bow.bottom()}) p.face(r, l.accent(), 5);
            for (Rect r : new Rect[] {bow.front(), bow.back()}) {
                p.set(r.x() + 1, r.y(), shade(l.accent(), 0.7)); // knot
                p.set(r.x() + 2, r.y() + 1, shade(l.accent(), 0.7));
            }
            return img;
        }

        // Eyes with lashes (and a flick of liner for the flirts), blush, painted lips.
        static void paintFace(Painter p, Rect f, Look l, int skinShade) {
            p.face(f, l.skin(), 3);
            p.row(f, 0, l.hair());
            // Soft brows (one pixel each, lifted at the outer end)
            int brow = blend(l.brow(), l.skin(), 0.25);
            p.set(f.x() + 1, f.y() + 2, brow);
            p.set(f.x() + 6, f.y() + 2, brow);
            // Big 2px-tall eyes: lashes on the outer corners, a darker upper iris, white below to the outside
            int lash = 0x1A1210;
            p.set(f.x() + 1, f.y() + 3, lash);
            p.set(f.x() + 2, f.y() + 3, shade(l.eye(), 0.6));
            p.set(f.x() + 5, f.y() + 3, shade(l.eye(), 0.6));
            p.set(f.x() + 6, f.y() + 3, lash);
            if (l.wing()) {
                p.set(f.x(), f.y() + 3, lash);
                p.set(f.x() + 7, f.y() + 3, lash);
            }
            p.set(f.x() + 1, f.y() + 4, 0xF4F0EA);
            p.set(f.x() + 2, f.y() + 4, l.eye());
            p.set(f.x() + 5, f.y() + 4, l.eye());
            p.set(f.x() + 6, f.y() + 4, 0xF4F0EA);
            int blush = blend(l.skin(), 0xE86A7A, 0.3);
            p.set(f.x() + 1, f.y() + 5, blush);
            p.set(f.x() + 6, f.y() + 5, blush);
            p.set(f.x() + 4, f.y() + 5, skinShade); // nose
            // Small painted lips with a darker lower edge
            p.set(f.x() + 3, f.y() + 6, l.lips());
            p.set(f.x() + 4, f.y() + 6, l.lips());
            p.set(f.x() + 3, f.y() + 7, blend(l.skin(), l.lips(), 0.25));
            p.set(f.x() + 4, f.y() + 7, blend(l.skin(), l.lips(), 0.25));
            if (l.freckles()) {
                p.set(f.x() + 2, f.y() + 5, shade(l.skin(), 0.8));
                p.set(f.x() + 5, f.y() + 5, shade(l.skin(), 0.8));
                p.set(f.x() + 3, f.y() + 5, shade(l.skin(), 0.86));
            }
            if (l.beautyMark()) p.set(f.x() + 6, f.y() + 6, 0x2A1A14);
        }

        // Corset lacing: crossed cord down two columns.
        static void lacing(Painter p, int x, int y, int rows, Look l) {
            for (int i = 0; i < rows; i++) {
                p.set(x + (i % 2), y + i, l.lace());
                p.set(x + 1 - (i % 2), y + i, shade(l.corset(), 0.7));
            }
        }

        static void cloth(Painter p, Rect r, int base, int pattern, Look l) {
            for (int x = 0; x < r.w(); x++)
                for (int y = 0; y < r.h(); y++) {
                    int c = switch (pattern) {
                        case GINGHAM -> (x % 2 == 0 && y % 2 == 0) ? 0xFAF6EC : (x % 2 == 0 || y % 2 == 0) ? blend(base, 0xFAF6EC, 0.45) : base;
                        case STRIPES -> x % 3 == 0 ? shade(base, 1.25) : base;
                        case SATIN -> x % 4 == 1 ? shade(base, 1.22) : x % 4 == 3 ? shade(base, 0.88) : base;
                        case BANDS -> y == r.h() - 2 ? l.lace() : y == r.h() - 4 ? 0xD8307A : base;
                        default -> base;
                    };
                    p.set(r.x() + x, r.y() + y, c);
                }
        }

        static int blend(int a, int b, double t) {
            int r = (int) (((a >> 16) & 0xFF) * (1 - t) + ((b >> 16) & 0xFF) * t);
            int g = (int) (((a >> 8) & 0xFF) * (1 - t) + ((b >> 8) & 0xFF) * t);
            int bl = (int) ((a & 0xFF) * (1 - t) + (b & 0xFF) * t);
            return (r << 16) | (g << 8) | bl;
        }
    }

    /** 18x18 effect icon: a tipped shot glass spilling whiskey. */
    static BufferedImage tipsyIcon() {
        BufferedImage img = new BufferedImage(18, 18, BufferedImage.TYPE_INT_ARGB);
        draw(img, 0, 0, 0xC8862A, pal('g', 0x9AB4B4, 'G', 0xE0F0F0, 'b', 0xF0D890),
                "",
                "",
                "   gGGGg",
                "   g555Gg",
                "   g5665Gg",
                "    g5665Gg",
                "    g55555Gg",
                "     g4555bg",
                "     g44444Gg",
                "      g4433gg",
                "      gggggg  5",
                "           4  5",
                "         5   4",
                "          4  3 4",
                "        3   4  3",
                "         4 3  5",
                "           3",
                "");
        return img;
    }

    // ---------------------------------------------------------------- guns
    // Each GeckoLib gun model (tools/models/GunModels.java) maps its faces onto material swatches of a 128x64 texture.
    // The layout below must match GunModels.Mat; only the palette differs per gun.

    static final class Gun {
        static final Rect WOOD_H = new Rect(0, 0, 64, 16), WOOD_V = new Rect(64, 0, 16, 64);
        static final Rect STEEL_H = new Rect(0, 16, 64, 8), STEEL_V = new Rect(96, 0, 8, 64);
        static final Rect CASE = new Rect(0, 24, 32, 16), BRASS = new Rect(32, 24, 32, 16);
        static final Rect BRIGHT = new Rect(0, 40, 16, 16), BLACK = new Rect(16, 40, 16, 16);
        static final Rect HICKORY_H = new Rect(32, 40, 32, 8), HICKORY_V = new Rect(104, 0, 8, 64);
        static final Rect BORE = new Rect(0, 56, 8, 8), CHAMBERS = new Rect(8, 56, 8, 8), COPPER = new Rect(16, 56, 8, 8);

        record Palette(int wood, int steel, int seed) {}

        static int writeAll(File root) throws Exception {
            // Revolver: oiled walnut, deep blued barrel and cylinder.
            write(new File(root, "item/revolver.png"), paint(new Palette(0x6E4024, 0x3A465A, 51)));
            // Plains rifle: lighter stock, browned barrel.
            write(new File(root, "item/plains_rifle.png"), paint(new Palette(0x7A4A26, 0x54402E, 52)));
            // Shotgun: dark walnut, blued barrels.
            write(new File(root, "item/shotgun.png"), paint(new Palette(0x553018, 0x353E4E, 53)));
            // The phlogiston guns (13b): green Gray light in the pistol, ember in the rifle
            for (String[] g : new String[][] {{"ray_pistol", "70FFA0", "61"}, {"phlogiston_rifle", "FF9A3A", "62"}}) {
                BufferedImage[] sheet = paintPhlogiston(Integer.parseInt(g[1], 16), Integer.parseInt(g[2]));
                write(new File(root, "item/" + g[0] + ".png"), sheet[0]);
                write(new File(root, "item/" + g[0] + "_glowmask.png"), sheet[1]);
            }
            return 7;
        }

        static BufferedImage paint(Palette pal) {
            BufferedImage img = new BufferedImage(128, 64, BufferedImage.TYPE_INT_ARGB);
            Random rnd = new Random(pal.seed());
            grain(img, WOOD_H, pal.wood(), false, rnd, 0.22);
            grain(img, WOOD_V, pal.wood(), true, rnd, 0.22);
            grain(img, HICKORY_H, 0xA88250, false, rnd, 0.12);
            grain(img, HICKORY_V, 0xA88250, true, rnd, 0.12);
            polish(img, STEEL_H, pal.steel(), false, rnd);
            polish(img, STEEL_V, pal.steel(), true, rnd);
            caseColors(img, CASE, rnd);
            metal(img, BRASS, 0xBC9440, 9, rnd);
            metal(img, BRIGHT, 0xB4BAC2, 8, rnd);
            metal(img, BLACK, 0x2A2C30, 5, rnd);
            metal(img, COPPER, 0xC07040, 10, rnd);
            bore(img, pal.steel());
            chambers(img, pal.steel());
            return img;
        }

        /**
         * The phlogiston guns' sheet (13b), on the same swatches: a dark composite where the wood goes, pale alien alloy for the
         * steel, starsteel for the case colours, brass, and the phlogiston's light (BRIGHT) and the emitter's face (BORE) in
         * {@code glow}; the glow mask is those two swatches alone.
         */
        static BufferedImage[] paintPhlogiston(int glow, int seed) {
            BufferedImage img = new BufferedImage(128, 64, BufferedImage.TYPE_INT_ARGB);
            BufferedImage mask = new BufferedImage(128, 64, BufferedImage.TYPE_INT_ARGB);
            Random rnd = new Random(seed);
            grain(img, WOOD_H, 0x2E3238, false, rnd, 0.08);
            grain(img, WOOD_V, 0x2E3238, true, rnd, 0.08);
            grain(img, HICKORY_H, 0x3A3E48, false, rnd, 0.08);
            grain(img, HICKORY_V, 0x3A3E48, true, rnd, 0.08);
            polish(img, STEEL_H, 0xB8BEC6, false, rnd);
            polish(img, STEEL_V, 0xB8BEC6, true, rnd);
            metal(img, CASE, 0xA8402E, 9, rnd);
            metal(img, BRASS, 0xBC9440, 9, rnd);
            metal(img, BLACK, 0x2A2C30, 5, rnd);
            metal(img, COPPER, 0xC07040, 10, rnd);
            chambers(img, 0xB8BEC6);
            for (int i = 0; i < BRIGHT.w(); i++) {
                for (int j = 0; j < BRIGHT.h(); j++) {
                    int c = (i + j) % 5 == 0 ? 0xF0FFF6 : (i * 3 + j) % 7 == 0 ? shade(glow, 0.8) : glow;
                    img.setRGB(BRIGHT.x() + i, BRIGHT.y() + j, 0xFF000000 | c);
                    mask.setRGB(BRIGHT.x() + i, BRIGHT.y() + j, 0xFF000000 | c);
                }
            }
            for (int i = 0; i < 8; i++) {
                for (int j = 0; j < 8; j++) {
                    double d = Math.hypot(i + 0.5 - 4, j + 0.5 - 4);
                    int c = d < 1.6 ? 0xFFFFFF : d < 3.0 ? glow : shade(glow, 0.6);
                    img.setRGB(BORE.x() + i, BORE.y() + j, 0xFF000000 | c);
                    mask.setRGB(BORE.x() + i, BORE.y() + j, 0xFF000000 | c);
                }
            }
            return new BufferedImage[] {img, mask};
        }

        // Wood: wavy dark grain lines running along the swatch's long axis.
        static void grain(BufferedImage img, Rect r, int base, boolean vertical, Random rnd, double lines) {
            double phase = rnd.nextDouble() * 10;
            for (int i = 0; i < r.w(); i++)
                for (int j = 0; j < r.h(); j++) {
                    double along = vertical ? j : i, across = vertical ? i : j;
                    double v = across + 1.4 * Math.sin(along * 0.11 + phase) + 0.6 * Math.sin(along * 0.37 + across * 0.5);
                    double band = 0.5 + 0.5 * Math.sin(v * 1.1);
                    double f = 0.86 + 0.16 * band + (rnd.nextDouble() - 0.5) * 0.06;
                    if ((v * 0.5 - Math.floor(v * 0.5)) < lines * 0.5) f -= 0.18;
                    img.setRGB(r.x() + i, r.y() + j, 0xFF000000 | shade(base, f));
                }
        }

        // Blued/browned steel: faint polish streaks along the long axis, a lighter top edge.
        static void polish(BufferedImage img, Rect r, int base, boolean vertical, Random rnd) {
            int lanes = vertical ? r.w() : r.h();
            double[] lane = new double[lanes];
            for (int k = 0; k < lanes; k++) lane[k] = (rnd.nextDouble() - 0.5) * 0.12;
            for (int i = 0; i < r.w(); i++)
                for (int j = 0; j < r.h(); j++) {
                    int k = vertical ? i : j;
                    double f = 1.0 + lane[k] + (rnd.nextDouble() - 0.5) * 0.05;
                    if (k == 0) f += 0.18;
                    img.setRGB(r.x() + i, r.y() + j, 0xFF000000 | shade(base, f));
                }
        }

        // Case hardening: soft mottles of blue, straw, purple and grey.
        static void caseColors(BufferedImage img, Rect r, Random rnd) {
            int[] colors = {0x45536E, 0x9C8456, 0x5E4866, 0x6E747C, 0x3A4458};
            int seed = rnd.nextInt();
            for (int i = 0; i < r.w(); i++)
                for (int j = 0; j < r.h(); j++) {
                    double n = noise(i * 0.22, j * 0.22, seed);
                    int c = colors[Math.min(colors.length - 1, (int) (n * colors.length))];
                    img.setRGB(r.x() + i, r.y() + j, 0xFF000000 | shade(c, 0.95 + (rnd.nextDouble() - 0.5) * 0.1));
                }
        }

        static void metal(BufferedImage img, Rect r, int base, int noise, Random rnd) {
            for (int i = 0; i < r.w(); i++)
                for (int j = 0; j < r.h(); j++) {
                    int n = rnd.nextInt(noise * 2 + 1) - noise;
                    int c = (clamp(((base >> 16) & 0xFF) + n) << 16) | (clamp(((base >> 8) & 0xFF) + n) << 8) | clamp((base & 0xFF) + n);
                    img.setRGB(r.x() + i, r.y() + j, 0xFF000000 | c);
                }
        }

        // Muzzle: steel face with a dark bore.
        static void bore(BufferedImage img, int steel) {
            for (int i = 0; i < 8; i++)
                for (int j = 0; j < 8; j++) {
                    double d = Math.hypot(i + 0.5 - 4, j + 0.5 - 4);
                    int c = d < 2.2 ? 0x0C0C0E : d < 3.0 ? shade(steel, 0.7) : shade(steel, 1.15);
                    img.setRGB(BORE.x() + i, BORE.y() + j, 0xFF000000 | c);
                }
        }

        // Front of the revolver cylinder: six chamber mouths around the arbor.
        static void chambers(BufferedImage img, int steel) {
            for (int i = 0; i < 8; i++)
                for (int j = 0; j < 8; j++) {
                    double x = i + 0.5 - 4, y = j + 0.5 - 4;
                    int c = shade(steel, 1.1);
                    for (int k = 0; k < 6; k++) {
                        double a = k * Math.PI / 3 + Math.PI / 2;
                        if (Math.hypot(x - 2.4 * Math.cos(a), y - 2.4 * Math.sin(a)) < 0.95) c = 0x141416;
                    }
                    if (Math.hypot(x, y) < 0.8) c = shade(steel, 0.6);
                    img.setRGB(CHAMBERS.x() + i, CHAMBERS.y() + j, 0xFF000000 | c);
                }
        }

        // Smooth value noise in [0, 1).
        static double noise(double x, double y, int seed) {
            int x0 = (int) Math.floor(x), y0 = (int) Math.floor(y);
            double fx = x - x0, fy = y - y0;
            fx = fx * fx * (3 - 2 * fx);
            fy = fy * fy * (3 - 2 * fy);
            double top = hash(x0, y0, seed) + (hash(x0 + 1, y0, seed) - hash(x0, y0, seed)) * fx;
            double bottom = hash(x0, y0 + 1, seed) + (hash(x0 + 1, y0 + 1, seed) - hash(x0, y0 + 1, seed)) * fx;
            return Math.min(0.999, top + (bottom - top) * fy);
        }

        static double hash(int x, int y, int seed) {
            int h = x * 374761393 + y * 668265263 + seed * 1442695041;
            h = (h ^ (h >>> 13)) * 1274126177;
            return ((h ^ (h >>> 16)) & 0xFFFFFF) / (double) 0x1000000;
        }
    }

    // ---------------------------------------------------------------- rattlesnake
    // 64x32 box-UV skin for geckolib/models/entity/rattlesnake.geo.json (uv origins and sizes below must match it).

    /**
     * The Hollow Earth's small creatures (9b2), 64x32 box-UV skins matching their hand-written GeckoLib models
     * (assets/hollowfrontier/geckolib/models/entity/*.geo.json; the boxes below repeat each cube's uv and size). The hare
     * and the salamander also get a {@code _glowmask}: only its opaque pixels (crystals; spots and eyes) are drawn
     * emissive, by GeckoLib's AutoGlowingGeoLayer.
     */
    static final class Creatures {
        static final int SKITTER = 0x5E7A3A, SKITTER_STRIPE = 0x34461E, SKITTER_BELLY = 0xD8CFA0, SKITTER_LEG = 0x8A7A50;
        static final int HARE = 0xB8A88A, HARE_DARK = 0x8E7E62, HARE_BELLY = 0xEDE6D6, EAR_IN = 0xD8A8A0;
        static final int CRYSTAL = 0xB8A0E8, CRYSTAL_HI = 0xEEE6FF, CRYSTAL_DARK = 0x7A60C0;
        static final int SALAMANDER = 0x2A2426, SALAMANDER_HI = 0x3E3436, SPOT = 0xFF7A1A, SPOT_HOT = 0xFFC040, SALAMANDER_BELLY = 0xB0461A;
        static final int RAVEN = 0xE8E8E4, RAVEN_GREY = 0xC4C6C6, RAVEN_DARK = 0x7A7E84, BEAK = 0x55575C;
        static final int EYE = 0x121014;
        static final int EAR_TAG = 0xF2C42A, EAR_TAG_DARK = 0x8A6A10;
        static final int PROBE = 0x8E9298, PROBE_DARK = 0x5C6068, PROBE_HI = 0xB8BCC2, PROBE_GLOW = 0x86F2E4, PROBE_GLOW_HI = 0xE0FFF8;
        // The live station's sentinel (10b): darker metal, red lights
        static final int SENTINEL = 0x5E6268, SENTINEL_DARK = 0x34373C, SENTINEL_HI = 0x8A8E94, SENTINEL_GLOW = 0xFF3A30, SENTINEL_GLOW_HI = 0xFFC8B8;

        static int writeAll(File root) throws Exception {
            write(new File(root, "entity/fern_skitter.png"), skitter());
            BufferedImage[] hare = hare();
            write(new File(root, "entity/glassback_hare.png"), hare[0]);
            write(new File(root, "entity/glassback_hare_glowmask.png"), hare[1]);
            BufferedImage[] salamander = salamander();
            write(new File(root, "entity/ash_salamander.png"), salamander[0]);
            write(new File(root, "entity/ash_salamander_glowmask.png"), salamander[1]);
            write(new File(root, "entity/pale_raven.png"), raven());
            BufferedImage[] probe = probe();
            write(new File(root, "entity/gray_probe.png"), probe[0]);
            write(new File(root, "entity/gray_probe_glowmask.png"), probe[1]);
            BufferedImage[] sentinel = probe(SENTINEL, SENTINEL_DARK, SENTINEL_HI, SENTINEL_GLOW, SENTINEL_GLOW_HI);
            write(new File(root, "entity/gray_sentinel.png"), sentinel[0]);
            write(new File(root, "entity/gray_sentinel_glowmask.png"), sentinel[1]);
            return 10;
        }

        /** Every face of a box in one colour (with a little noise). */
        static void all(Painter p, Box b, int rgb, int noise) {
            for (Rect r : new Rect[] {b.top(), b.bottom(), b.east(), b.front(), b.west(), b.back()}) p.face(r, rgb, noise);
        }

        /** Eyes on a head box's flanks, {@code back} pixels behind its front edge, {@code down} below its top. */
        static void eyes(Painter p, Box head, int back, int down, int rgb) {
            p.set(head.east().x() + head.east().w() - 1 - back, head.east().y() + down, rgb);
            p.set(head.west().x() + back, head.west().y() + down, rgb);
        }

        /** Fern Skitter: olive green with dark stripes across the back and tail, a cream belly and throat, dark eyes. */
        static BufferedImage skitter() {
            BufferedImage img = new BufferedImage(64, 32, BufferedImage.TYPE_INT_ARGB);
            Painter p = new Painter(img, new Random(9201));
            Box body = new Box(0, 0, 3, 3, 6), neck = new Box(18, 0, 2, 4, 2), head = new Box(26, 0, 3, 2, 4);
            Box leg = new Box(40, 0, 1, 5, 2), foot = new Box(46, 0, 2, 1, 3), arm = new Box(56, 0, 1, 2, 1);
            Box tail1 = new Box(0, 9, 2, 2, 5), tail2 = new Box(14, 9, 1, 1, 6);
            for (Box b : new Box[] {body, neck, head, tail1, tail2}) {
                all(p, b, SKITTER, 8);
                p.face(b.bottom(), SKITTER_BELLY, 5);
            }
            // Stripes across the back and down the flanks, on and on along the tail
            for (Box b : new Box[] {body, tail1, tail2}) {
                for (int y = 1; y < b.top().h(); y += 2) p.row(b.top(), y, SKITTER_STRIPE);
                for (Rect side : new Rect[] {b.east(), b.west()}) {
                    for (int x = 1; x < side.w(); x += 2) p.set(side.x() + x, side.y(), SKITTER_STRIPE);
                    p.row(side, side.h() - 1, SKITTER_BELLY);
                }
            }
            p.face(neck.front(), SKITTER_BELLY, 4);
            p.face(head.front(), shade(SKITTER, 0.85), 4);
            p.set(head.front().x(), head.front().y(), 0x1A1410);
            p.set(head.front().x() + 2, head.front().y(), 0x1A1410);
            for (Rect side : new Rect[] {head.east(), head.west()}) p.row(side, 1, SKITTER_BELLY); // pale jaw
            eyes(p, head, 2, 0, EYE);
            all(p, leg, SKITTER_LEG, 6);
            all(p, foot, shade(SKITTER_LEG, 0.75), 4);
            all(p, arm, SKITTER_LEG, 6);
            return img;
        }

        /** Glassback Hare: buff fur, darker on the back, white belly and tail, pink inner ears, and violet crystals. */
        static BufferedImage[] hare() {
            BufferedImage img = new BufferedImage(64, 32, BufferedImage.TYPE_INT_ARGB);
            BufferedImage glow = new BufferedImage(64, 32, BufferedImage.TYPE_INT_ARGB);
            Painter p = new Painter(img, new Random(9202));
            Painter g = new Painter(glow, new Random(9202));
            Box body = new Box(0, 0, 4, 4, 7), head = new Box(22, 0, 3, 3, 4), ear = new Box(36, 0, 1, 4, 1);
            Box frontLeg = new Box(22, 7, 1, 2, 1), backLeg = new Box(26, 7, 1, 3, 3), tail = new Box(34, 7, 2, 2, 1);
            all(p, body, HARE, 8);
            p.face(body.top(), HARE_DARK, 8);
            p.face(body.bottom(), HARE_BELLY, 4);
            all(p, head, HARE, 8);
            p.face(head.front(), shade(HARE, 1.08), 4);
            p.set(head.front().x() + 1, head.front().y() + 2, 0x6A4A44); // nose
            eyes(p, head, 1, 0, EYE);
            all(p, ear, HARE, 6);
            p.face(ear.front(), EAR_IN, 4);
            all(p, frontLeg, HARE, 6);
            all(p, backLeg, HARE, 6);
            all(p, tail, HARE_BELLY, 3);
            for (Box c : new Box[] {new Box(40, 0, 1, 3, 1), new Box(44, 0, 1, 2, 1), new Box(48, 0, 1, 2, 1), new Box(52, 0, 1, 2, 1)}) {
                for (Painter q : new Painter[] {p, g}) {
                    all(q, c, CRYSTAL, 10);
                    q.face(c.top(), CRYSTAL_HI, 4);
                    q.set(c.east().x(), c.east().y() + c.east().h() - 1, CRYSTAL_DARK);
                    q.set(c.west().x(), c.west().y() + c.west().h() - 1, CRYSTAL_DARK);
                }
            }
            // The ear tag (9b3): a yellow tab, only drawn on the Gray lab's hare (GlassbackHareRenderer)
            Box tag = new Box(56, 0, 1, 1, 1);
            all(p, tag, EAR_TAG, 4);
            p.face(tag.bottom(), EAR_TAG_DARK, 2);
            return new BufferedImage[] {img, glow};
        }

        /**
         * Gray Probe (9b3): a dull gunmetal saucer with panel seams, a darker dome, and a band of teal lights round its rim,
         * an eye on the dome's front, a lens underneath and a tip on its antenna, all of which glow.
         */
        static BufferedImage[] probe() {
            return probe(PROBE, PROBE_DARK, PROBE_HI, PROBE_GLOW, PROBE_GLOW_HI);
        }

        /** The probe's skin in the given metal and lights (the sentinel's is darker, its lights red). */
        static BufferedImage[] probe(int metal, int dark, int hi, int light, int lightHi) {
            BufferedImage img = new BufferedImage(64, 32, BufferedImage.TYPE_INT_ARGB);
            BufferedImage glow = new BufferedImage(64, 32, BufferedImage.TYPE_INT_ARGB);
            Painter p = new Painter(img, new Random(9205));
            Painter g = new Painter(glow, new Random(9205));
            Box body = new Box(0, 0, 8, 2, 8), lens = new Box(48, 0, 2, 1, 2), band = new Box(0, 10, 10, 1, 10);
            Box dome = new Box(32, 0, 4, 2, 4), eye = new Box(48, 4, 2, 1, 1), antenna = new Box(56, 0, 1, 2, 1);
            all(p, body, metal, 3);
            // The top: a lit rim round its edge, short seams in from the middle of each side, a rivet in each corner
            Rect top = body.top();
            for (int x = 0; x < top.w(); x++) {
                for (int y = 0; y < top.h(); y++) {
                    int c = x == 0 || y == 0 ? shade(hi, 1.1) : x == top.w() - 1 || y == top.h() - 1 ? metal : hi;
                    boolean seam = (x == top.w() / 2 && (y < 2 || y > top.h() - 3)) || (y == top.h() / 2 && (x < 2 || x > top.w() - 3));
                    p.set(top.x() + x, top.y() + y, seam ? dark : c);
                }
            }
            for (int[] rv : new int[][] {{1, 1}, {top.w() - 2, 1}, {1, top.h() - 2}, {top.w() - 2, top.h() - 2}}) p.set(top.x() + rv[0], top.y() + rv[1], dark);
            // The rim's edge rounds over: lit along its top row, in shadow along its foot
            for (Rect side : new Rect[] {body.front(), body.back(), body.east(), body.west()}) {
                p.row(side, 0, hi);
                p.row(side, side.h() - 1, dark);
            }
            // Underneath: a dark inner ring round the lens, vent slits between
            Rect under = body.bottom();
            p.face(under, dark, 3);
            for (int x = 1; x < under.w() - 1; x++) {
                for (int y = 1; y < under.h() - 1; y++) {
                    boolean ring = x == 1 || y == 1 || x == under.w() - 2 || y == under.h() - 2;
                    if (ring) p.set(under.x() + x, under.y() + y, shade(dark, 0.7));
                    else if ((x + y) % 3 == 0) p.set(under.x() + x, under.y() + y, metal);
                }
            }
            all(p, dome, dark, 3);
            p.face(dome.top(), shade(dark, 1.25), 3);
            p.set(dome.top().x(), dome.top().y(), shade(dark, 1.6)); // a glint on the dome
            for (Rect side : new Rect[] {dome.front(), dome.back(), dome.east(), dome.west()}) p.row(side, 0, shade(dark, 1.15));
            all(p, antenna, dark, 3);
            // The band: dark metal with a light every other pixel round the rim
            all(p, band, dark, 3);
            for (Rect side : new Rect[] {band.front(), band.back(), band.east(), band.west()}) {
                for (int x = 0; x < side.w(); x += 2) {
                    for (Painter q : new Painter[] {p, g}) q.set(side.x() + x, side.y(), x % 4 == 0 ? lightHi : light);
                }
            }
            for (Painter q : new Painter[] {p, g}) {
                all(q, lens, light, 6);
                q.face(lens.bottom(), lightHi, 3);
                q.face(eye.front(), lightHi, 2);
                q.set(antenna.top().x(), antenna.top().y(), lightHi);
            }
            return new BufferedImage[] {img, glow};
        }

        /** Ash Salamander: charcoal black with glowing orange spots, an ember-orange belly, yellow eyes (which glow too). */
        static BufferedImage[] salamander() {
            BufferedImage img = new BufferedImage(64, 32, BufferedImage.TYPE_INT_ARGB);
            BufferedImage glow = new BufferedImage(64, 32, BufferedImage.TYPE_INT_ARGB);
            Painter p = new Painter(img, new Random(9203));
            Painter g = new Painter(glow, new Random(9203));
            Random r = new Random(9213);
            Box body = new Box(0, 0, 4, 3, 8), head = new Box(24, 0, 4, 2, 4), tail1 = new Box(0, 11, 3, 2, 5), tail2 = new Box(16, 11, 2, 1, 6);
            Box leg = new Box(40, 0, 2, 2, 1);
            for (Box b : new Box[] {body, head, tail1, tail2}) {
                all(p, b, SALAMANDER, 6);
                p.face(b.top(), SALAMANDER_HI, 6);
                p.face(b.bottom(), SALAMANDER_BELLY, 8);
                // Spots on the back and flanks
                for (Rect face : new Rect[] {b.top(), b.east(), b.west()}) {
                    int spots = Math.max(1, face.w() * face.h() / 6);
                    for (int i = 0; i < spots; i++) {
                        int x = face.x() + r.nextInt(face.w()), y = face.y() + r.nextInt(face.h());
                        int c = r.nextInt(3) == 0 ? SPOT_HOT : SPOT;
                        p.set(x, y, c);
                        g.set(x, y, c);
                    }
                }
            }
            all(p, leg, SALAMANDER, 6);
            p.face(leg.bottom(), SALAMANDER_BELLY, 6);
            int eyeEast = head.east().x() + head.east().w() - 2, eyeWest = head.west().x() + 1, eyeY = head.east().y();
            for (Painter q : new Painter[] {p, g}) {
                q.set(eyeEast, eyeY, SPOT_HOT);
                q.set(eyeWest, head.west().y(), SPOT_HOT);
            }
            return new BufferedImage[] {img, glow};
        }

        /** Pale Raven: white plumage greying toward the wings' and tail's ends, a dark grey bill and legs, black eyes. */
        static BufferedImage raven() {
            BufferedImage img = new BufferedImage(64, 32, BufferedImage.TYPE_INT_ARGB);
            Painter p = new Painter(img, new Random(9204));
            Box body = new Box(0, 0, 4, 4, 6), head = new Box(20, 0, 3, 3, 3), beak = new Box(32, 0, 1, 1, 2);
            Box wing = new Box(0, 10, 1, 3, 6), tail = new Box(14, 10, 3, 1, 4), leg = new Box(28, 10, 1, 3, 1);
            // Scalloped feathers over the body and wings, the back a shade greyer, the breast plain
            for (Rect r : new Rect[] {body.east(), body.west(), body.back()}) Megafauna.feathers(p, r, RAVEN, shade(RAVEN_GREY, 0.9), new Random(9206));
            Megafauna.feathers(p, body.top(), shade(RAVEN, 0.95), shade(RAVEN_GREY, 0.9), new Random(9207));
            p.face(body.front(), RAVEN, 2);
            p.face(body.bottom(), RAVEN_GREY, 3);
            all(p, head, RAVEN, 2);
            p.face(head.top(), shade(RAVEN, 0.95), 2);
            eyes(p, head, 1, 1, EYE);
            eyes(p, head, 1, 0, RAVEN_GREY); // a grey brow over each eye
            all(p, beak, BEAK, 2);
            p.face(beak.top(), shade(BEAK, 1.35), 2);
            all(p, wing, RAVEN, 2);
            for (Rect r : new Rect[] {wing.east(), wing.west(), wing.top()}) Megafauna.feathers(p, r, RAVEN, shade(RAVEN_GREY, 0.9), new Random(9208));
            // Primaries: the wing's trailing rows and back end grey to dark
            // (on the east strip the back end is its left edge, on the west strip its right)
            for (Rect side : new Rect[] {wing.east(), wing.west()}) {
                p.row(side, side.h() - 1, RAVEN_DARK);
                int backEdge = side.equals(wing.east()) ? side.x() : side.x() + side.w() - 1;
                for (int y = 0; y < side.h() - 1; y++) p.set(backEdge, side.y() + y, RAVEN_GREY);
            }
            p.face(wing.back(), RAVEN_DARK, 4);
            all(p, tail, RAVEN_GREY, 5);
            p.row(tail.top(), tail.top().h() - 1, RAVEN_DARK);
            all(p, leg, 0x4A4A4E, 4);
            return img;
        }
    }

    // ---------------------------------------------------------------- phase 10a: the wild megafauna

    /**
     * The Hollow Earth's wild megafauna (10a), each on its model's box UVs (geckolib/models/entity/*.geo.json): the woolly
     * mammoth, terror bird, megatherium and glyptodon; and the talon and Talon Knife icons. Their other items are reworked
     * from vanilla ({@link Reworked}).
     */
    static final class Megafauna {
        static final int MAMMOTH = 0x6A4428, MAMMOTH_DARK = 0x45291A, MAMMOTH_LIGHT = 0x93653C, MAMMOTH_SKIN = 0x4E423C;
        static final int IVORY = 0xEDE4C8, IVORY_DARK = 0xC4B48A, IVORY_HI = 0xFFFBEA;
        static final int BIRD = 0x5A5048, BIRD_DARK = 0x332C26, BIRD_LIGHT = 0x857766, BIRD_CHEST = 0xCDBD9C, CREST = 0xB04E1E;
        static final int BEAK = 0xD8BE6C, BEAK_DARK = 0xA88E44, BEAK_TIP = 0x2A2420, BIRD_LEG = 0x8E845E, BIRD_EYE = 0xF2C21E;
        static final int SLOTH = 0x7E5434, SLOTH_DARK = 0x58381F, SLOTH_LIGHT = 0xA27248, SLOTH_SKIN = 0x5C4C44, CLAW = 0xDCD0B2;
        static final int SHELL = 0xA28C5C, SHELL_DARK = 0x6A5A38, SHELL_LIGHT = 0xC8B27C, GLYPT = 0x62584A, GLYPT_DARK = 0x3E362E;
        static final int EYE = 0x141010;
        // The Grays' plating and implants on a tagged beast (10b), the probe's gunmetal and teal
        static final int PLATE = 0x7E848C, PLATE_HI = 0xA8AEB6, PLATE_DARK = 0x4A4E56;
        static final int GLOW = Creatures.PROBE_GLOW, GLOW_HI = Creatures.PROBE_GLOW_HI;

        static int writeAll(File root) throws Exception {
            String[] beasts = {"woolly_mammoth", "terror_bird", "megatherium", "glyptodon"};
            BufferedImage[][] skins = {mammoth(), terrorBird(), megatherium(), glyptodon()};
            for (int i = 0; i < beasts.length; i++) {
                write(new File(root, "entity/" + beasts[i] + ".png"), skins[i][0]);
                write(new File(root, "entity/" + beasts[i] + "_glowmask.png"), skins[i][1]);
            }
            Items.put(root, "terror_bird_talon", talon());
            Items.put(root, "talon_knife", talonKnife());
            Items.put(root, "mammoth_tusk", tusk());
            // The Gray experiments (10b): the fragment, and the Signal Locator's needle in 32 bearings
            Items.put(root, "gray_tech_fragment", fragment());
            for (int i = 0; i < 32; i++) {
                Items.put(root, String.format(java.util.Locale.ROOT, "signal_locator_%02d", i), locator(i));
            }
            return 44;
        }

        /** A Gray tech fragment: a jagged shard of gunmetal with a teal circuit trace across it. */
        static BufferedImage fragment() {
            return sprite(PLATE, pal('k', 0x24272C, 'g', GLOW, 'G', GLOW_HI),
                    "",
                    "",
                    "       kk",
                    "      k65k",
                    "     k6554k",
                    "    k65G44k",
                    "    k5gg433k",
                    "   k654g4332k",
                    "   k5543g332k",
                    "    k5433g22k",
                    "     k433322k",
                    "      k3322k",
                    "       kk2k",
                    "         k");
        }

        /**
         * The Signal Locator, frame {@code frame} of 32: a gunmetal case with a dark teal screen, a ring of dim marks round
         * it, and a bright trace from the middle out toward the bearing (360 * frame / 32 degrees clockwise from up).
         */
        static BufferedImage locator(int frame) {
            BufferedImage img = new BufferedImage(16, 16, BufferedImage.TYPE_INT_ARGB);
            double cx = 8, cy = 8;
            int screen = 0x123A3A;
            for (int x = 0; x < 16; x++) {
                for (int y = 0; y < 16; y++) {
                    double dx = x + 0.5 - cx, dy = y + 0.5 - cy, d = Math.hypot(dx, dy);
                    int c;
                    if (d <= 5.0) {
                        c = (x + y) % 5 == 0 ? shade(screen, 1.25) : screen;
                    } else if (d <= 6.4) {
                        c = dx + dy < -2 ? PLATE_HI : dx + dy > 2 ? PLATE_DARK : PLATE;
                    } else if (d <= 7.2) {
                        c = 0x24272C;
                    } else {
                        continue;
                    }
                    img.setRGB(x, y, 0xFF000000 | c);
                }
            }
            for (int[] t : new int[][] {{7, 4}, {8, 4}, {11, 7}, {11, 8}, {7, 11}, {8, 11}, {4, 7}, {4, 8}}) img.setRGB(t[0], t[1], 0xFF000000 | shade(GLOW, 0.55));
            needle(img, frame, 4, 1, GLOW, GLOW_HI, 0x0A1E1E);
            for (int[] h : new int[][] {{7, 7}, {8, 7}, {7, 8}, {8, 8}}) img.setRGB(h[0], h[1], 0xFF000000 | (h[0] + h[1] == 14 ? PLATE_HI : PLATE_DARK));
            return img;
        }

        /**
         * A tagged beast's extras (10b), on their own UV boxes: riveted gunmetal plates, and an implant of dark metal with a
         * teal light in each face, which is all the glow mask holds.
         */
        static BufferedImage[] tagged(BufferedImage img, Box[] plates, Box implant) {
            BufferedImage glow = new BufferedImage(img.getWidth(), img.getHeight(), BufferedImage.TYPE_INT_ARGB);
            Painter p = new Painter(img, new Random(10501));
            Painter g = new Painter(glow, new Random(10501));
            for (Box plate : plates) {
                for (Rect r : sides(plate)) {
                    p.face(r, PLATE, 4);
                    rivets(p, r);
                }
                p.face(plate.top(), PLATE_HI, 4);
                rivets(p, plate.top());
                p.face(plate.bottom(), PLATE_DARK, 3);
            }
            Creatures.all(p, implant, PLATE_DARK, 3);
            for (Rect r : new Rect[] {implant.top(), implant.bottom(), implant.east(), implant.front(), implant.west(), implant.back()}) {
                for (Painter q : new Painter[] {p, g}) {
                    q.set(r.x() + r.w() / 2, r.y() + r.h() / 2, GLOW_HI);
                    if (r.w() > 1) q.set(r.x() + (r.w() - 1) / 2, r.y() + r.h() / 2, GLOW);
                    if (r.h() > 2) q.set(r.x() + r.w() / 2, r.y() + r.h() / 2 - 1, GLOW);
                }
            }
            return new BufferedImage[] {img, glow};
        }

        /** Rivets every three pixels, a pixel in from the edges. */
        static void rivets(Painter p, Rect r) {
            for (int x = 1; x < r.w() - 1; x += 3) {
                for (int y = 1; y < r.h() - 1; y += 3) p.set(r.x() + x, r.y() + y, PLATE_DARK);
            }
        }

        static Rect[] sides(Box b) {
            return new Rect[] {b.east(), b.front(), b.west(), b.back()};
        }

        /**
         * Shaggy fur in long hanging strands: each column is a run of strands 5-11 pixels long, every strand lit at its root
         * and darkening to its tip, so the face streaks downward like hair. Columns get their own shade, now and then a dark
         * parting between them; the whole face darkens toward its foot, and a few pale strands catch the light.
         */
        static void fur(Painter p, Rect r, int base, int dark, int light, Random rnd) {
            for (int x = r.x(); x < r.x() + r.w(); x++) {
                double column = 0.9 + rnd.nextInt(6) * 0.04;
                boolean parting = rnd.nextInt(5) == 0;
                int len = 5 + rnd.nextInt(7), t = rnd.nextInt(len);
                boolean pale = false;
                for (int y = r.y(); y < r.y() + r.h(); y++, t++) {
                    if (t >= len) { // the next strand starts
                        len = 5 + rnd.nextInt(7);
                        t = 0;
                        pale = rnd.nextInt(10) == 0;
                    }
                    double fall = 1.06 - 0.2 * (y - r.y()) / Math.max(1, r.h() - 1);
                    double strand = 1.16 - 0.3 * t / Math.max(1, len - 1);
                    int c = parting && t > 1 ? dark : pale && t < 3 ? light : shade(base, strand * column * fall);
                    p.set(x, y, c);
                }
            }
        }

        /** A box in fur: the sides shaggy with a ragged dark fringe on the lowest row, the top a shade lighter, the bottom dark. */
        static void furBox(Painter p, Box b, int base, int dark, int light, Random rnd) {
            for (Rect r : sides(b)) {
                fur(p, r, base, dark, light, rnd);
                for (int x = r.x(); x < r.x() + r.w(); x++) {
                    if (rnd.nextInt(3) != 0) p.set(x, r.y() + r.h() - 1, dark);
                }
            }
            fur(p, b.top(), shade(base, 1.08), base, light, rnd);
            p.face(b.bottom(), dark, 6);
        }

        /** Rows of overlapping feathers: every other row a pixel darker in turn, so the face reads as scalloped. */
        static void feathers(Painter p, Rect r, int base, int dark, Random rnd) {
            p.face(r, base, 6);
            for (int y = 0; y < r.h(); y += 2) {
                for (int x = (y / 2) % 2; x < r.w(); x += 2) p.set(r.x() + x, r.y() + y, dark);
            }
        }

        /** Bony scutes in a brick pattern: dark seams, a light boss in each. */
        static void scutes(Painter p, Rect r, int base, int dark, int light, Random rnd) {
            for (int x = 0; x < r.w(); x++) {
                for (int y = 0; y < r.h(); y++) {
                    int sx = x + ((y / 4) % 2) * 2;
                    int c = sx % 4 == 0 || y % 4 == 0 ? dark : sx % 4 == 2 && y % 4 == 2 ? light : base;
                    int n = rnd.nextInt(9) - 4;
                    p.set(r.x() + x, r.y() + y, (clamp(((c >> 16) & 255) + n) << 16) | (clamp(((c >> 8) & 255) + n) << 8) | clamp((c & 255) + n));
                }
            }
        }

        static void scuteBox(Painter p, Box b, int base, int dark, int light, Random rnd) {
            for (Rect r : sides(b)) scutes(p, r, base, dark, light, rnd);
            scutes(p, b.top(), shade(base, 1.06), dark, light, rnd);
            p.face(b.bottom(), GLYPT_DARK, 5);
        }

        /** Woolly Mammoth: shaggy chestnut fur, darker down the spine and on the hump, a grey-skinned trunk, ivory tusks. */
        static BufferedImage[] mammoth() {
            BufferedImage img = new BufferedImage(256, 128, BufferedImage.TYPE_INT_ARGB);
            Painter p = new Painter(img, new Random(10101));
            Random r = new Random(10111);
            Box body = new Box(0, 0, 24, 24, 34), hump = new Box(0, 58, 18, 6, 14), head = new Box(64, 58, 16, 16, 14);
            Box ear = new Box(64, 88, 2, 8, 6), tail = new Box(80, 88, 2, 10, 2), leg = new Box(0, 78, 8, 20, 8);
            Box trunk = new Box(32, 78, 6, 8, 6), trunkMid = new Box(32, 92, 5, 8, 5), trunkTip = new Box(88, 88, 4, 8, 4);
            Box tuskBase = new Box(104, 88, 3, 10, 3), tuskFore = new Box(56, 106, 3, 3, 12), tuskTip = new Box(42, 105, 2, 6, 2);
            furBox(p, body, MAMMOTH, MAMMOTH_DARK, MAMMOTH_LIGHT, r);
            // A dark stripe down the spine
            Rect top = body.top();
            for (int y = 0; y < top.h(); y++) {
                for (int dx = -2; dx <= 1; dx++) if (r.nextInt(5) != 0) p.set(top.x() + top.w() / 2 + dx, top.y() + y, MAMMOTH_DARK);
            }
            // Longer, darker fringe hanging along the flanks (the skirt of a woolly coat)
            for (Rect side : new Rect[] {body.east(), body.west(), body.front(), body.back()}) {
                for (int x = side.x(); x < side.x() + side.w(); x++) {
                    int len = 2 + r.nextInt(4);
                    for (int y = side.y() + side.h() - len; y < side.y() + side.h(); y++) p.set(x, y, r.nextInt(3) == 0 ? MAMMOTH : MAMMOTH_DARK);
                }
            }
            furBox(p, hump, MAMMOTH_DARK, shade(MAMMOTH_DARK, 0.8), MAMMOTH, r);
            furBox(p, head, MAMMOTH, MAMMOTH_DARK, MAMMOTH_LIGHT, r);
            // The face: fur over the brow, bare grey skin below it where the trunk grows
            Rect face = head.front();
            for (int y = face.h() / 2; y < face.h(); y++) {
                for (int x = 3; x < face.w() - 3; x++) p.set(face.x() + x, face.y() + y, shade(MAMMOTH_SKIN, 0.95 + r.nextInt(10) / 100.0));
            }
            Creatures.eyes(p, head, 3, 8, EYE);
            furBox(p, ear, MAMMOTH_DARK, shade(MAMMOTH_DARK, 0.8), MAMMOTH, r);
            furBox(p, tail, MAMMOTH_DARK, shade(MAMMOTH_DARK, 0.75), MAMMOTH, r);
            for (Rect side : sides(tail)) p.row(side, side.h() - 1, shade(MAMMOTH_DARK, 0.6));
            furBox(p, leg, MAMMOTH, MAMMOTH_DARK, MAMMOTH_LIGHT, r);
            for (Rect side : sides(leg)) { // grey feet with pale nails
                p.row(side, side.h() - 1, MAMMOTH_SKIN);
                p.row(side, side.h() - 2, shade(MAMMOTH_SKIN, 1.1));
            }
            Rect toes = leg.front();
            for (int x = 1; x < toes.w(); x += 3) p.set(toes.x() + x, toes.y() + toes.h() - 1, IVORY_DARK);
            p.face(leg.bottom(), shade(MAMMOTH_SKIN, 0.7), 4);
            // The trunk: furred where it leaves the head, wrinkled grey skin toward the tip
            furBox(p, trunk, MAMMOTH, MAMMOTH_DARK, MAMMOTH_LIGHT, r);
            for (Box b : new Box[] {trunkMid, trunkTip}) {
                Creatures.all(p, b, MAMMOTH_SKIN, 6);
                for (Rect side : sides(b)) for (int y = 1; y < side.h(); y += 2) p.row(side, y, shade(MAMMOTH_SKIN, 0.82));
            }
            for (Rect side : sides(trunkMid)) for (int x = side.x(); x < side.x() + side.w(); x++) if (r.nextBoolean()) p.set(x, side.y(), MAMMOTH_DARK);
            p.face(trunkTip.bottom(), shade(MAMMOTH_SKIN, 0.5), 2);
            // Tusks: ivory, yellowed toward the root, brightest at the tips
            Creatures.all(p, tuskBase, IVORY, 5);
            for (Rect side : sides(tuskBase)) { p.row(side, 0, IVORY_DARK); p.row(side, 1, IVORY_DARK); }
            Creatures.all(p, tuskFore, IVORY, 5);
            p.face(tuskFore.bottom(), IVORY_DARK, 4);
            Creatures.all(p, tuskTip, IVORY_HI, 3);
            return tagged(img, new Box[] {new Box(128, 0, 25, 2, 22), new Box(128, 24, 1, 11, 16), new Box(128, 52, 14, 9, 1)}, new Box(128, 64, 2, 3, 3));
        }

        /** Terror Bird: dusky grey-brown, scalloped feathers, a barred pale chest, a russet crest, a horn-yellow hooked beak. */
        static BufferedImage[] terrorBird() {
            BufferedImage img = new BufferedImage(128, 64, BufferedImage.TYPE_INT_ARGB);
            Painter p = new Painter(img, new Random(10201));
            Random r = new Random(10211);
            Box body = new Box(0, 0, 10, 10, 16), tail = new Box(52, 0, 8, 2, 9), wing = new Box(52, 11, 1, 6, 10);
            Box neck = new Box(0, 26, 5, 12, 5), head = new Box(20, 26, 6, 6, 8), crest = new Box(96, 0, 2, 3, 4);
            Box beak = new Box(48, 27, 4, 4, 8), hook = new Box(86, 0, 3, 3, 2), jaw = new Box(74, 11, 3, 2, 6);
            Box thigh = new Box(72, 27, 4, 8, 5), shin = new Box(108, 0, 2, 10, 2), foot = new Box(90, 27, 4, 2, 5);
            for (Rect side : sides(body)) feathers(p, side, BIRD, BIRD_DARK, r);
            feathers(p, body.top(), shade(BIRD, 0.9), BIRD_DARK, r);
            // Pale chest and belly, barred
            for (Rect pale : new Rect[] {body.front(), body.bottom()}) {
                p.face(pale, BIRD_CHEST, 6);
                for (int y = 1; y < pale.h(); y += 3) p.row(pale, y, shade(BIRD_CHEST, 0.78));
            }
            for (Rect side : new Rect[] {body.east(), body.west()}) p.row(side, side.h() - 1, BIRD_CHEST);
            for (Rect side : sides(tail)) feathers(p, side, BIRD_DARK, shade(BIRD_DARK, 0.7), r);
            feathers(p, tail.top(), BIRD_DARK, BIRD, r);
            p.row(tail.top(), tail.top().h() - 1, BIRD_LIGHT); // pale tips
            p.face(tail.bottom(), BIRD, 4);
            for (Rect side : sides(wing)) feathers(p, side, BIRD_DARK, BIRD, r);
            for (Rect side : new Rect[] {wing.east(), wing.west()}) p.row(side, side.h() - 1, BIRD_LIGHT);
            p.face(wing.top(), BIRD, 4);
            for (Rect side : sides(neck)) feathers(p, side, BIRD, BIRD_DARK, r);
            p.face(neck.front(), BIRD_CHEST, 5); // the throat
            for (int y = 1; y < neck.front().h(); y += 3) p.row(neck.front(), y, shade(BIRD_CHEST, 0.8));
            Creatures.all(p, head, BIRD, 6);
            p.face(head.top(), BIRD_DARK, 5);
            p.face(head.bottom(), BIRD_CHEST, 4);
            // Fierce yellow eyes under a dark brow, a pale cheek stripe
            for (Rect side : new Rect[] {head.east(), head.west()}) p.row(side, 0, BIRD_DARK);
            p.set(head.east().x() + head.east().w() - 3, head.east().y() + 2, BIRD_EYE);
            p.set(head.east().x() + head.east().w() - 2, head.east().y() + 2, EYE);
            p.set(head.west().x() + 2, head.west().y() + 2, BIRD_EYE);
            p.set(head.west().x() + 1, head.west().y() + 2, EYE);
            p.row(head.east(), 4, BIRD_CHEST);
            p.row(head.west(), 4, BIRD_CHEST);
            Creatures.all(p, crest, CREST, 8);
            p.face(crest.top(), shade(CREST, 1.2), 4);
            // The beak: horn yellow, darker below, the hook dark
            Creatures.all(p, beak, BEAK, 6);
            p.face(beak.bottom(), BEAK_DARK, 4);
            for (Rect side : new Rect[] {beak.east(), beak.west()}) p.row(side, side.h() - 1, BEAK_DARK);
            p.set(beak.top().x() + 1, beak.top().y() + beak.top().h() - 2, BEAK_TIP); // nostrils, near the base
            p.set(beak.top().x() + beak.top().w() - 2, beak.top().y() + beak.top().h() - 2, BEAK_TIP);
            p.face(beak.front(), shade(BEAK_TIP, 1.6), 3);
            Creatures.all(p, hook, BEAK_TIP, 4);
            Creatures.all(p, jaw, BEAK_DARK, 5);
            p.face(jaw.top(), shade(CREST, 0.8), 4); // a red mouth inside
            // Legs: feathered thighs, scaly yellow-grey shins and feet, dark claws
            for (Rect side : sides(thigh)) feathers(p, side, BIRD, BIRD_DARK, r);
            p.face(thigh.top(), BIRD, 4);
            p.face(thigh.bottom(), BIRD_DARK, 4);
            Creatures.all(p, shin, BIRD_LEG, 5);
            for (Rect side : sides(shin)) for (int y = 1; y < side.h(); y += 2) p.row(side, y, shade(BIRD_LEG, 0.8));
            Creatures.all(p, foot, BIRD_LEG, 5);
            for (int x = 0; x < foot.front().w(); x += 2) {
                p.set(foot.front().x() + x, foot.front().y() + 1, BEAK_TIP);
                p.set(foot.top().x() + x, foot.top().y(), shade(BEAK_TIP, 1.5));
            }
            return tagged(img, new Box[] {new Box(0, 43, 11, 1, 12), new Box(46, 43, 7, 1, 6)}, new Box(72, 43, 1, 3, 3));
        }

        /** Megatherium: shaggy russet-brown, darker along the back, a bare grey-brown snout, long cream claws. */
        static BufferedImage[] megatherium() {
            BufferedImage img = new BufferedImage(256, 128, BufferedImage.TYPE_INT_ARGB);
            Painter p = new Painter(img, new Random(10301));
            Random r = new Random(10311);
            Box body = new Box(0, 0, 22, 20, 30), tail = new Box(0, 50, 8, 10, 10), head = new Box(36, 50, 10, 10, 11);
            Box snout = new Box(28, 71, 7, 6, 4), arm = new Box(0, 71, 7, 16, 7), claw = new Box(50, 71, 6, 2, 3), leg = new Box(78, 50, 9, 18, 10);
            furBox(p, body, SLOTH, SLOTH_DARK, SLOTH_LIGHT, r);
            Rect top = body.top();
            for (int y = 0; y < top.h(); y++) {
                for (int dx = -4; dx <= 3; dx++) if (r.nextInt(3) != 0) p.set(top.x() + top.w() / 2 + dx, top.y() + y, SLOTH_DARK);
            }
            // Paler belly fur, ragged down the flanks
            for (Rect side : new Rect[] {body.east(), body.west()}) {
                for (int x = side.x(); x < side.x() + side.w(); x++) {
                    int len = 2 + r.nextInt(3);
                    for (int y = side.y() + side.h() - len; y < side.y() + side.h(); y++) p.set(x, y, r.nextInt(3) == 0 ? SLOTH : SLOTH_LIGHT);
                }
            }
            furBox(p, tail, SLOTH, SLOTH_DARK, SLOTH_LIGHT, r);
            furBox(p, head, SLOTH, SLOTH_DARK, SLOTH_LIGHT, r);
            p.face(head.front(), SLOTH_DARK, 6);
            Creatures.eyes(p, head, 3, 4, EYE);
            Creatures.all(p, snout, SLOTH_SKIN, 6);
            p.face(snout.top(), shade(SLOTH_SKIN, 1.1), 4);
            p.set(snout.front().x() + 1, snout.front().y() + 1, EYE);
            p.set(snout.front().x() + snout.front().w() - 2, snout.front().y() + 1, EYE);
            p.row(snout.front(), snout.front().h() - 1, shade(SLOTH_SKIN, 0.7)); // the mouth
            furBox(p, arm, SLOTH, SLOTH_DARK, SLOTH_LIGHT, r);
            furBox(p, leg, SLOTH, SLOTH_DARK, SLOTH_LIGHT, r);
            for (Rect side : sides(leg)) p.row(side, side.h() - 1, SLOTH_SKIN);
            p.face(leg.bottom(), shade(SLOTH_SKIN, 0.7), 4);
            // Three long claws on each forefoot
            Creatures.all(p, claw, CLAW, 4);
            for (Rect face : new Rect[] {claw.front(), claw.top(), claw.bottom()}) {
                for (int x = 2; x < face.w(); x += 2) for (int y = 0; y < face.h(); y++) p.set(face.x() + x, face.y() + y, shade(CLAW, 0.6));
            }
            return tagged(img, new Box[] {new Box(128, 0, 23, 2, 24), new Box(128, 26, 11, 1, 8)}, new Box(128, 40, 2, 3, 3));
        }

        /** Glyptodon: a domed tan shell of bony scutes, a darker armoured cap and tail, dark grey-brown skin, cream spikes. */
        static BufferedImage[] glyptodon() {
            BufferedImage img = new BufferedImage(256, 128, BufferedImage.TYPE_INT_ARGB);
            Painter p = new Painter(img, new Random(10401));
            Random r = new Random(10411);
            Box shell = new Box(0, 0, 24, 14, 30), dome = new Box(0, 44, 20, 3, 24), head = new Box(88, 44, 8, 7, 7);
            Box cap = new Box(88, 72, 9, 2, 6), leg = new Box(88, 58, 6, 7, 6), tail = new Box(0, 71, 6, 6, 7), club = new Box(26, 71, 7, 7, 6);
            Box spike = new Box(52, 71, 2, 2, 2);
            scuteBox(p, shell, SHELL, SHELL_DARK, SHELL_LIGHT, r);
            // A dark band where the shell's rim meets the ground
            for (Rect side : sides(shell)) {
                p.row(side, side.h() - 1, SHELL_DARK);
                p.row(side, side.h() - 2, shade(SHELL, 0.85));
            }
            scuteBox(p, dome, shade(SHELL, 1.04), SHELL_DARK, SHELL_LIGHT, r);
            Creatures.all(p, head, GLYPT, 6);
            p.face(head.front(), GLYPT_DARK, 4);
            p.set(head.front().x() + 2, head.front().y() + head.front().h() - 2, EYE); // nostrils
            p.set(head.front().x() + head.front().w() - 3, head.front().y() + head.front().h() - 2, EYE);
            Creatures.eyes(p, head, 2, 2, EYE);
            scuteBox(p, cap, SHELL_DARK, shade(SHELL_DARK, 0.7), SHELL, r);
            Creatures.all(p, leg, GLYPT_DARK, 6);
            for (Rect side : sides(leg)) for (int y = 0; y < side.h() - 1; y += 2) for (int x = (y / 2) % 2; x < side.w(); x += 2) p.set(side.x() + x, side.y() + y, GLYPT);
            for (int x = 0; x < leg.front().w(); x += 2) p.set(leg.front().x() + x, leg.front().y() + leg.front().h() - 1, CLAW);
            scuteBox(p, tail, SHELL, SHELL_DARK, SHELL_LIGHT, r);
            scuteBox(p, club, SHELL_DARK, shade(SHELL_DARK, 0.7), SHELL, r);
            Creatures.all(p, spike, CLAW, 4);
            p.face(spike.top(), shade(CLAW, 1.1), 2);
            return tagged(img, new Box[] {new Box(128, 0, 21, 1, 5), new Box(128, 6, 25, 12, 5), new Box(128, 23, 9, 1, 6)}, new Box(128, 30, 1, 3, 3));
        }

        static final java.util.Map<java.lang.Character, Integer> CLAW_PAL = pal('k', 0x1A1614, 'b', 0xDCD0B2, 'B', 0xAFA282,
                'L', 0x5E3A1E, 'l', 0x8A5A32, 'W', 0x9C6E40, 'w', 0x6E4A28);

        /** A terror bird's talon: a dark keratin hook, thick at its pale root, curling over to a needle point. */
        static BufferedImage talon() {
            java.util.Map<java.lang.Character, Integer> palette = new java.util.HashMap<>(CLAW_PAL);
            palette.put('H', 0xA89A8E); // the shine along the outer curve
            return sprite(0x6E625A, palette,
                    "",
                    "       kkkk",
                    "     kkHH65kk",
                    "    kH6655544k",
                    "   kH655kkk443k",
                    "  kH654k   k43k",
                    "  kH65k     k3k",
                    " kH654k     k2k",
                    " kH654k      k",
                    " kbB54k",
                    " kbbBBk",
                    " kbbBBk",
                    "  kbBk",
                    "   kk");
        }

        /** A mammoth's tusk: an ivory crescent, yellowed at its thick root (bottom left), sweeping up to a pale tip. */
        static BufferedImage tusk() {
            return sprite(0xE0D4B0, pal('k', 0x5A4A30, 'y', 0xB8A274, 'Y', 0x9A8458),
                    "",
                    "             k",
                    "            k6k",
                    "            k5k",
                    "           k55k",
                    "          k554k",
                    "         k5544k",
                    "        k55443k",
                    "      kk55443k",
                    "    kk555443k",
                    "  kk5554433k",
                    " kyy554433kk",
                    " kyyY4433k",
                    "  kYYY3kk",
                    "   kkkk");
        }

        /** The Talon Knife: a talon for a blade, bound to a wooden grip with a leather wrap (held like a sword, blade up-right). */
        static BufferedImage talonKnife() {
            return sprite(0x5A524C, CLAW_PAL,
                    "",
                    "             kk",
                    "            k5k",
                    "           k54k",
                    "          k543k",
                    "         k5432k",
                    "        k5432k",
                    "       kb432k",
                    "      kbB2kk",
                    "     kLlkk",
                    "    kLlLk",
                    "   kWLk",
                    "  kWwk",
                    " kWwk",
                    " kkk");
        }
    }

    // ---------------------------------------------------------------- phase 11a: the frontier's mysteries

    /**
     * The frontier's mysteries (11a), on their models' box UVs: the airship sighting (128x64: a dark hull in silvered
     * panels, a gondola with lit windows, a searchlight at the nose and running lights, all of which glow) and the Strange
     * Skull's icon. The cattle carcass and the flattened wheat are reworked from vanilla's ({@link Reworked}).
     */
    static final class Mystery {
        static final int HULL = 0x3C4048, HULL_DARK = 0x262A30, HULL_HI = 0x5A6068, LIGHT = 0xFFE8A0, LIGHT_HI = 0xFFFFF0, LAMP = 0xFFF8D8;
        static final int FLESH = 0xD88A84, CUT = 0x7A1E1E, TRACE = 0x2E7A74; // a carcass's cuts (Reworked)

        static int writeAll(File root) throws Exception {
            BufferedImage[] sighting = sighting();
            write(new File(root, "entity/airship_sighting.png"), sighting[0]);
            write(new File(root, "entity/airship_sighting_glowmask.png"), sighting[1]);
            Items.put(root, "strange_skull", skull());
            Tiles.put(root, "flattened_wheat", flattenedWheat());
            return 4; // the carcass is reworked from vanilla's cow (Reworked)
        }

        /**
         * A crop circle's flattened wheat (11a), seen from above: ripe stalks all laid the same way, four broad bands sloping
         * across the block (each lit along its top, shaded below, a dark line of stubble under it), each stalk ending in a pale
         * seed head, so a circle reads as a paler swirl against the standing crop. The bands run 2 across for 1 down, so the
         * tile repeats.
         */
        static BufferedImage flattenedWheat() {
            BufferedImage img = Tiles.blank();
            Random r = new Random(1102);
            double[] lit = {1.18, 1.0, 0.84};
            int[] start = {r.nextInt(16), r.nextInt(16)}; // bands two apart share a start: the stair shifts 2 bands across a tile
            for (int x = 0; x < 16; x++) {
                for (int y = 0; y < 16; y++) {
                    int line = Math.floorMod(y - Math.floorDiv(x, 2), 16), s = line / 4, row = line % 4;
                    int t = Math.floorMod(x - start[s % 2], 16);
                    int c;
                    if (row == 3) c = t < 4 ? 0x9A7A38 : 0x5E4A26;               // stubble showing under the stalk
                    else if (t < 3) c = row == 0 ? 0xF4DE96 : 0xB89038;          // the seed head
                    else c = shade(s % 2 == 0 ? 0xC8A456 : 0xBC984C, lit[row]);
                    Tiles.px(img, x, y, Tiles.jitter(c, r, 3));
                }
            }
            return img;
        }

        static BufferedImage[] sighting() {
            BufferedImage img = new BufferedImage(128, 64, BufferedImage.TYPE_INT_ARGB);
            BufferedImage glow = new BufferedImage(128, 64, BufferedImage.TYPE_INT_ARGB);
            Painter p = new Painter(img, new Random(11101));
            Painter g = new Painter(glow, new Random(11101));
            Box hull = new Box(0, 0, 8, 8, 32), fore = new Box(0, 40, 6, 6, 6), aft = new Box(24, 40, 6, 6, 6);
            Box nose = new Box(48, 40, 4, 4, 3), tail = new Box(62, 40, 4, 4, 2), fin = new Box(80, 0, 1, 4, 7), wing = new Box(80, 11, 4, 1, 5);
            Box gondola = new Box(80, 17, 4, 3, 14), windows = new Box(80, 34, 1, 1, 4), light = new Box(112, 0, 1, 1, 1);
            for (Box b : new Box[] {hull, fore, aft, tail}) {
                Creatures.all(p, b, HULL, 4);
                p.face(b.top(), HULL_HI, 4);
                p.face(b.bottom(), HULL_DARK, 3);
                // Panel seams round the hull
                for (Rect r : new Rect[] {b.top(), b.east(), b.west(), b.bottom()}) {
                    for (int i = 3; i < Math.max(r.w(), r.h()); i += 4) {
                        if (r.w() > r.h()) for (int y = 0; y < r.h(); y++) p.set(r.x() + i, r.y() + y, HULL_DARK);
                        else for (int x = 0; x < r.w(); x++) p.set(r.x() + x, r.y() + i, HULL_DARK);
                    }
                }
            }
            Creatures.all(p, nose, HULL_DARK, 3);
            for (Painter q : new Painter[] {p, g}) q.face(nose.front(), LAMP, 2); // the searchlight
            Creatures.all(p, fin, HULL, 4);
            Creatures.all(p, wing, HULL, 4);
            Creatures.all(p, gondola, HULL_DARK, 3);
            for (Rect side : new Rect[] {gondola.east(), gondola.west()}) {
                for (int x = 1; x < side.w() - 1; x += 2) {
                    for (Painter q : new Painter[] {p, g}) q.set(side.x() + x, side.y() + 1, LIGHT);
                }
            }
            for (Painter q : new Painter[] {p, g}) {
                Creatures.all(q, windows, LIGHT, 3);
                Creatures.all(q, light, LIGHT_HI, 2);
            }
            return new BufferedImage[] {img, glow};
        }

        /** The Strange Skull: a grey cranium too big for its little jaw, with great dark almond eyes. */
        static BufferedImage skull() {
            return sprite(0xB8B4AA, pal('k', 0x3A3832, 'e', 0x0E0C10, 'E', 0x2A2838),
                    "",
                    "     kkkkkk",
                    "   kk666655kk",
                    "  k6666555544k",
                    " k66665555444k",
                    " k66555544443k",
                    " k65eeE44eeE3k",
                    " k5eeeE44eeeEk",
                    " k54eeE33eeE3k",
                    "  k444333333k",
                    "   k4333332k",
                    "    k43332k",
                    "     k3k3k",
                    "     k333k",
                    "      kkk");
        }
    }

    static final class Snake {
        static final int TAN = 0xB89A6A, DARK = 0x5A4030, EDGE = 0xE8D8A8, BELLY = 0xE8DCC0, HEAD = 0x8A6A4A;

        static BufferedImage skin() {
            BufferedImage img = new BufferedImage(64, 32, BufferedImage.TYPE_INT_ARGB);
            Painter p = new Painter(img, new Random(71));
            // Body segments {u, v, w, h, d, z of their front}: diamonds run on along the whole back
            for (int[] s : new int[][] {{16, 0, 2, 2, 4, -11}, {0, 8, 3, 2, 5, -7}, {16, 8, 3, 2, 5, -2}, {32, 8, 2, 2, 5, 3}}) {
                segment(p, new Box(s[0], s[1], s[2], s[3], s[4]), s[5]);
            }
            // Head: darker crown, pale jaw, eyes near the snout (the east strip meets the front on its right edge)
            Box head = new Box(0, 0, 3, 2, 4);
            p.face(head.top(), HEAD, 6);
            p.set(head.top().x() + 1, head.top().y() + 1, DARK);
            p.face(head.bottom(), BELLY, 4);
            p.face(head.front(), shade(HEAD, 0.85), 4);
            p.set(head.front().x() + 1, head.front().y() + 1, 0x1A1410); // nostril pit
            p.face(head.back(), TAN, 4);
            p.face(head.east(), TAN, 5);
            p.face(head.west(), TAN, 5);
            p.set(head.east().x() + 2, head.east().y(), 0x1A1410);
            p.set(head.east().x() + 3, head.east().y() + 1, EDGE);
            p.set(head.west().x() + 1, head.west().y(), 0x1A1410);
            p.set(head.west().x(), head.west().y() + 1, EDGE);
            // Coontail: black and white rings
            Box tail = new Box(32, 0, 2, 1, 4);
            for (Rect r : new Rect[] {tail.top(), tail.bottom()}) {
                for (int y = 0; y < r.h(); y++) p.row(r, y, y % 2 == 0 ? 0x1E1A16 : 0xE8E4DA);
            }
            for (Rect r : new Rect[] {tail.east(), tail.west()}) {
                for (int x = 0; x < r.w(); x++) p.set(r.x() + x, r.y(), x % 2 == 0 ? 0x1E1A16 : 0xE8E4DA);
            }
            p.face(tail.front(), 0x1E1A16, 2);
            p.face(tail.back(), 0xE8E4DA, 2);
            // Rattle: horny buttons
            Box rattle = new Box(46, 0, 1, 1, 2);
            for (Rect r : new Rect[] {rattle.top(), rattle.bottom(), rattle.east(), rattle.west(), rattle.front(), rattle.back()}) {
                p.face(r, 0xD8C8A0, 4);
            }
            p.set(rattle.top().x(), rattle.top().y(), 0x9A8A68);
            return img;
        }

        private static void segment(Painter p, Box b, int z0) {
            Rect top = b.top();
            p.face(top, TAN, 8);
            int mid = top.w() / 2;
            for (int y = 0; y < top.h(); y++) {
                int k = Math.floorMod(z0 + y, 4);
                if (k == 0) {
                    p.set(top.x() + mid, top.y() + y, DARK);
                } else if (k == 2) {
                    p.set(top.x() + mid, top.y() + y, EDGE);
                } else {
                    if (mid - 1 >= 0) p.set(top.x() + mid - 1, top.y() + y, DARK);
                    if (mid + 1 < top.w()) p.set(top.x() + mid + 1, top.y() + y, DARK);
                }
            }
            for (Rect side : new Rect[] {b.east(), b.west()}) {
                p.face(side, shade(TAN, 1.06), 8);
                for (int x = 0; x < side.w(); x++) {
                    if (Math.floorMod(z0 + x, 4) == 2) p.set(side.x() + x, side.y(), DARK);
                }
            }
            p.face(b.bottom(), BELLY, 4);
            p.face(b.front(), TAN, 4);
            p.face(b.back(), TAN, 4);
        }
    }

    // ---------------------------------------------------------------- cats
    // Coats use the vanilla cat UV layouts: adult 64x32 (AdultFelineModel) and baby 32x32 (BabyFelineModel). The
    // accessory textures (32x32) follow hollowfrontier's CatAccessoryModel; everything outside their boxes is clear.

    static final class Cats {
        /** eye/nose are single pixels; stripe < 0 = none; patchA/patchB >= 0 = scruffy patches; notch = a nicked ear. */
        record Coat(int base, int stripe, int belly, int eye, int nose, int patchA, int patchB, int noise, boolean notch, long seed) {}

        // Desert: sandy, faint tabby stripes, cream belly, amber eyes.
        static final Coat DESERT = new Coat(0xD6B27A, 0xBE9660, 0xF0E2C0, 0xC8A020, 0xC88A7A, -1, -1, 6, false, 81);
        // Saloon: dusty grey-brown, ginger and white patches, scruffy, a notched ear, green eyes.
        static final Coat SALOON = new Coat(0x7E7062, 0x5A4E44, 0xE2DACC, 0x7AB040, 0xB07A70, 0xB06A34, 0xE2DACC, 16, true, 82);
        // Ghost: pale blue-white, the faintest stripes, glowing pale eyes (drawn see-through by the renderer).
        static final Coat GHOST = new Coat(0xE6ECF4, 0xCCD6E4, 0xF6F9FF, 0x8EE6FF, 0xD8C8E0, -1, -1, 4, false, 83);

        static int writeAll(File root) throws Exception {
            write(new File(root, "entity/cat/desert.png"), adult(DESERT));
            write(new File(root, "entity/cat/desert_baby.png"), baby(DESERT));
            write(new File(root, "entity/cat/saloon.png"), adult(SALOON));
            write(new File(root, "entity/cat/saloon_baby.png"), baby(SALOON));
            write(new File(root, "entity/cat/ghost.png"), haunt(adult(GHOST)));
            write(new File(root, "entity/cat/cat_cowboy_hat.png"), hat());
            write(new File(root, "entity/cat/cat_bandana.png"), bandana());
            write(new File(root, "entity/cat/cat_sheriff_star.png"), star());
            return 8;
        }

        /**
         * The ghost cat's coat drifts like smoke: wavy bands of brighter white and cooler blue-grey curl across the fur
         * (the eyes, nose and inner ears are left as they are).
         */
        static BufferedImage haunt(BufferedImage img) {
            for (int x = 0; x < img.getWidth(); x++) {
                for (int y = 0; y < img.getHeight(); y++) {
                    int p = img.getRGB(x, y);
                    if ((p >>> 24) == 0 || p == (0xFF000000 | GHOST.eye()) || p == (0xFF000000 | GHOST.nose())) continue;
                    double v = Math.sin(x * 0.55 + Math.sin(y * 0.5) * 1.6 + y * 0.2);
                    if (v > 0.8) img.setRGB(x, y, 0xFF000000 | 0xFAFCFF);
                    else if (v < -0.85) img.setRGB(x, y, 0xFF000000 | 0xB8C6DC);
                    else if (v < -0.6) img.setRGB(x, y, 0xFF000000 | 0xD2DAE8);
                }
            }
            return img;
        }

        static BufferedImage adult(Coat c) {
            BufferedImage img = new BufferedImage(64, 32, BufferedImage.TYPE_INT_ARGB);
            Painter p = new Painter(img, new Random(c.seed()));
            Box head = new Box(0, 0, 5, 4, 5), nose = new Box(0, 24, 3, 2, 2), body = new Box(20, 0, 4, 16, 6);
            fur(p, c, head, true);
            // Tabby "M" on the brow, cheeks and muzzle pale, two eyes above the snout
            if (c.stripe() >= 0) {
                Rect top = head.top();
                for (int x = 0; x < top.w(); x += 2) p.set(top.x() + x, top.y() + 1, c.stripe());
                p.set(top.x() + 2, top.y() + 3, c.stripe());
            }
            Rect face = head.front();
            p.row(face, 3, c.belly());
            p.set(face.x() + 1, face.y() + 1, c.eye());
            p.set(face.x() + 3, face.y() + 1, c.eye());
            p.set(face.x() + 1, face.y(), shade(c.base(), 0.8));
            p.set(face.x() + 3, face.y(), shade(c.base(), 0.8));
            fur(p, c, nose, false);
            p.face(nose.top(), c.belly(), 3);
            p.face(nose.front(), c.belly(), 3);
            p.set(nose.front().x() + 1, nose.front().y(), c.nose());
            for (Box ear : new Box[] {new Box(0, 10, 1, 1, 2), new Box(6, 10, 1, 1, 2)}) {
                fur(p, c, ear, false);
                p.set(ear.front().x(), ear.front().y(), c.nose()); // pink inner ear
            }
            if (c.notch()) { // a bite out of the right ear, from the front and above
                Box ear = new Box(6, 10, 1, 1, 2);
                p.set(ear.front().x(), ear.front().y(), CLEAR);
                p.set(ear.top().x(), ear.top().y(), CLEAR);
            }
            fur(p, c, body, true);
            p.face(body.front(), c.belly(), 4); // the belly
            fur(p, c, new Box(0, 15, 1, 8, 1), true);
            fur(p, c, new Box(4, 15, 1, 8, 1), true);
            tailTip(p, c, new Box(4, 15, 1, 8, 1));
            for (Box leg : new Box[] {new Box(8, 13, 2, 6, 2), new Box(40, 0, 2, 10, 2)}) {
                fur(p, c, leg, false);
                paws(p, c, leg);
            }
            return img;
        }

        static BufferedImage baby(Coat c) {
            BufferedImage img = new BufferedImage(32, 32, BufferedImage.TYPE_INT_ARGB);
            Painter p = new Painter(img, new Random(c.seed() + 100));
            Box head = new Box(0, 0, 5, 4, 4), nose = new Box(18, 3, 3, 2, 1), body = new Box(0, 8, 4, 3, 7);
            fur(p, c, head, true);
            Rect face = head.front();
            p.row(face, 3, c.belly());
            p.set(face.x() + 1, face.y() + 1, c.eye());
            p.set(face.x() + 3, face.y() + 1, c.eye());
            fur(p, c, nose, false);
            p.face(nose.front(), c.belly(), 3);
            p.set(nose.front().x() + 1, nose.front().y(), c.nose());
            for (Box ear : new Box[] {new Box(18, 0, 1, 1, 2), new Box(24, 0, 1, 1, 2)}) {
                fur(p, c, ear, false);
                p.set(ear.front().x(), ear.front().y(), c.nose());
            }
            fur(p, c, body, true);
            p.face(body.bottom(), c.belly(), 4);
            fur(p, c, new Box(0, 18, 1, 1, 5), true);
            for (int[] uv : new int[][] {{18, 18}, {12, 18}, {18, 22}, {12, 22}}) {
                Box leg = new Box(uv[0], uv[1], 1, 2, 2);
                fur(p, c, leg, false);
                paws(p, c, leg);
            }
            return img;
        }

        // All six faces in the coat's fur: noise, faint stripes every few rows, scruffy patches.
        static void fur(Painter p, Coat c, Box b, boolean striped) {
            for (Rect r : new Rect[] {b.top(), b.bottom(), b.east(), b.front(), b.west(), b.back()}) {
                p.face(r, c.base(), c.noise());
                for (int x = r.x(); x < r.x() + r.w(); x++) {
                    for (int y = r.y(); y < r.y() + r.h(); y++) {
                        if (striped && c.stripe() >= 0 && Math.floorMod(y - r.y(), 4) == 1 && Gun.hash(x, y, (int) c.seed()) < 0.8) {
                            p.set(x, y, c.stripe());
                        }
                        if (c.patchA() >= 0) {
                            double blob = Gun.hash(x / 3, y / 3, (int) c.seed());
                            if (blob < 0.22) p.set(x, y, shade(c.patchA(), 0.9 + 0.2 * Gun.hash(x, y, 7)));
                            else if (blob > 0.82) p.set(x, y, shade(c.patchB(), 0.94 + 0.1 * Gun.hash(x, y, 9)));
                            else if (Gun.hash(x, y, (int) c.seed() + 1) < 0.08) p.set(x, y, c.stripe()); // scruff
                        }
                    }
                }
            }
        }

        // Pale paws: the bottom row of each side and the sole.
        static void paws(Painter p, Coat c, Box leg) {
            for (Rect r : new Rect[] {leg.east(), leg.front(), leg.west(), leg.back()}) p.row(r, r.h() - 1, c.belly());
            p.face(leg.bottom(), c.belly(), 2);
        }

        static void tailTip(Painter p, Coat c, Box tail) {
            int tip = c.patchA() >= 0 ? c.stripe() : c.stripe() >= 0 ? c.stripe() : shade(c.base(), 0.85);
            for (Rect r : new Rect[] {tail.east(), tail.front(), tail.west(), tail.back()}) {
                p.row(r, r.h() - 1, tip);
                p.row(r, r.h() - 2, tip);
            }
            p.face(tail.bottom(), tip, 2);
        }

        // Tiny cowboy hat: felt crown with a dented top and dark band, a wide flat brim a shade lighter at the edge, with a
        // hole for each ear. On the brim's top and underside, column 0 is the -x edge and row 0 the back (+z) edge; the ears
        // sit in columns 2 and 5, rows 1-2 (CatAccessoryModel puts the brim at x -4..4, z -4..3).
        static BufferedImage hat() {
            BufferedImage img = new BufferedImage(32, 32, BufferedImage.TYPE_INT_ARGB);
            Painter p = new Painter(img, new Random(84));
            int felt = 0x6E4C2E, band = 0x2E2014;
            Box crown = new Box(0, 0, 2, 2, 3);
            for (Rect r : new Rect[] {crown.top(), crown.bottom(), crown.east(), crown.front(), crown.west(), crown.back()}) p.face(r, felt, 6);
            p.row(crown.top(), 1, shade(felt, 0.7)); // the pinch
            for (Rect r : new Rect[] {crown.east(), crown.front(), crown.west(), crown.back()}) p.row(r, 1, band);
            Box brim = new Box(0, 6, 8, 1, 7);
            for (Rect r : new Rect[] {brim.top(), brim.bottom(), brim.east(), brim.front(), brim.west(), brim.back()}) p.face(r, felt, 6);
            for (Rect r : new Rect[] {brim.top(), brim.bottom()}) p.outline(r, shade(felt, 1.18));
            p.face(brim.bottom(), shade(felt, 0.82), 4); // underside in shadow
            for (Rect r : new Rect[] {brim.top(), brim.bottom()}) {
                for (int col : new int[] {2, 5}) {
                    for (int row : new int[] {1, 2}) p.set(r.x() + col, r.y() + row, CLEAR);
                }
            }
            return img;
        }

        // Bandana: pale cloth with paisley dots (the renderer tints it with the dye), on the band and the kerchief.
        static BufferedImage bandana() {
            BufferedImage img = new BufferedImage(32, 32, BufferedImage.TYPE_INT_ARGB);
            Painter p = new Painter(img, new Random(85));
            int cloth = 0xF0F0F0, dot = 0xB4B4B4;
            for (Box b : new Box[] {new Box(0, 14, 4, 2, 6), new Box(20, 14, 3, 1, 3)}) {
                for (Rect r : new Rect[] {b.top(), b.bottom(), b.east(), b.front(), b.west(), b.back()}) {
                    p.face(r, cloth, 5);
                    for (int x = r.x(); x < r.x() + r.w(); x++)
                        for (int y = r.y(); y < r.y() + r.h(); y++)
                            if ((x + 2 * y) % 4 == 0) p.set(x, y, dot);
                }
            }
            Box kerchief = new Box(20, 14, 3, 1, 3);
            for (Rect r : new Rect[] {kerchief.top(), kerchief.bottom()}) p.set(r.x() + 1, r.y() + 1, 0x8C8C8C); // the knot
            return img;
        }

        // Brass star on a dark leather collar. The star plate (row 0 = top): a point, the arms, a bright middle, two feet;
        // the same on both faces. The collar has stitched edges and a brass ring under the chin.
        static BufferedImage star() {
            BufferedImage img = new BufferedImage(32, 32, BufferedImage.TYPE_INT_ARGB);
            Painter p = new Painter(img, new Random(86));
            int brass = 0xE0B440, leather = 0x4A2E1A;
            Box collar = new Box(0, 14, 4, 2, 6);
            for (Rect r : new Rect[] {collar.top(), collar.bottom(), collar.east(), collar.front(), collar.west(), collar.back()}) p.face(r, leather, 5);
            for (Rect r : new Rect[] {collar.east(), collar.front(), collar.west(), collar.back()}) p.row(r, 0, shade(leather, 1.3));
            p.set(collar.front().x() + 1, collar.front().y() + 1, brass);
            p.set(collar.front().x() + 2, collar.front().y() + 1, shade(brass, 0.8));
            Box star = new Box(0, 24, 4, 1, 4);
            String[] shape = {".hh.", "bwmb", ".mm.", "d..d"};
            for (Rect r : new Rect[] {star.top(), star.bottom()}) {
                for (int y = 0; y < 4; y++)
                    for (int x = 0; x < 4; x++) {
                        char c = shape[y].charAt(x);
                        if (c == '.') continue;
                        p.set(r.x() + x, r.y() + y, switch (c) {
                            case 'h' -> shade(brass, 1.2);
                            case 'w' -> 0xFFF4C8;
                            case 'm' -> brass;
                            case 'd' -> shade(brass, 0.72);
                            default -> shade(brass, 0.9);
                        });
                    }
            }
            for (Rect r : new Rect[] {star.east(), star.front(), star.west(), star.back()}) p.row(r, 0, shade(brass, 0.65));
            return img;
        }
    }

    // ---------------------------------------------------------------- ASCII sprites
    // '.' or ' ' = transparent, '1'..'6' = shades of the sprite's base color (dark..highlight), letters = palette entries.

    static final double[] RAMP = {0, 0.42, 0.6, 0.8, 1.0, 1.2, 1.42};

    static int tone(int base, int level) {
        return shade(base, RAMP[level]);
    }

    static BufferedImage sprite(int base, java.util.Map<java.lang.Character, Integer> palette, String... rows) {
        BufferedImage img = new BufferedImage(16, 16, BufferedImage.TYPE_INT_ARGB);
        draw(img, 0, 0, base, palette, rows);
        return img;
    }

    static void draw(BufferedImage img, int ox, int oy, int base, java.util.Map<java.lang.Character, Integer> palette, String... rows) {
        for (int y = 0; y < rows.length; y++) {
            String row = rows[y];
            for (int x = 0; x < row.length(); x++) {
                char ch = row.charAt(x);
                Integer rgb = null;
                if (ch >= '1' && ch <= '6') rgb = tone(base, ch - '0');
                else if (palette != null && palette.containsKey(ch)) rgb = palette.get(ch);
                if (rgb != null && ox + x < img.getWidth() && oy + y < img.getHeight()) {
                    img.setRGB(ox + x, oy + y, 0xFF000000 | rgb);
                }
            }
        }
    }

    /**
     * A dial's needle for frame {@code frame} of 32 (360 * frame / 32 degrees clockwise from up), pivoting on the 2x2 hub at
     * (7..8, 7..8): a clean one-pixel line {@code headLen} long toward the bearing, ending in {@code tip}, and a tail
     * {@code tailLen} long the other way. The caller draws the hub over the pivot afterwards.
     */
    static void needle(BufferedImage img, int frame, int headLen, int tailLen, int head, int tip, int tail) {
        double a = Math.PI * 2 * frame / 32.0, ux = Math.sin(a), uy = -Math.cos(a);
        needleLine(img, ux, uy, headLen, head, tip);
        needleLine(img, -ux, -uy, tailLen, tail, tail);
    }

    /** One arm of a needle: a Bresenham line from the hub pixel on that side out {@code len} pixels along (ux, uy). */
    static void needleLine(BufferedImage img, double ux, double uy, int len, int rgb, int tip) {
        int x0 = ux > 1e-9 ? 8 : 7, y0 = uy > 1e-9 ? 8 : 7;
        int x1 = x0 + (int) Math.round(ux * len), y1 = y0 + (int) Math.round(uy * len);
        int dx = Math.abs(x1 - x0), dy = -Math.abs(y1 - y0), sx = x0 < x1 ? 1 : -1, sy = y0 < y1 ? 1 : -1, err = dx + dy;
        for (int x = x0, y = y0; ; ) {
            img.setRGB(x, y, 0xFF000000 | (x == x1 && y == y1 ? tip : rgb));
            if (x == x1 && y == y1) break;
            int e2 = 2 * err;
            if (e2 >= dy) { err += dy; x += sx; }
            if (e2 <= dx) { err += dx; y += sy; }
        }
    }

    static java.util.Map<java.lang.Character, Integer> pal(Object... kv) {
        java.util.Map<java.lang.Character, Integer> map = new java.util.HashMap<>();
        for (int i = 0; i < kv.length; i += 2) map.put((java.lang.Character) kv[i], (Integer) kv[i + 1]);
        return map;
    }

    /** Filled disc with 3D shading (light from top-left); returns nothing, draws onto img. */
    static void disc(BufferedImage img, double cx, double cy, double r, int base) {
        for (int x = 0; x < img.getWidth(); x++)
            for (int y = 0; y < img.getHeight(); y++) {
                double dx = x + 0.5 - cx, dy = y + 0.5 - cy, d = Math.sqrt(dx * dx + dy * dy);
                if (d <= r) {
                    int level = d > r - 1 ? 2 : (dx + dy < -r * 0.6 ? 6 : dx + dy > r * 0.6 ? 3 : 4);
                    img.setRGB(x, y, 0xFF000000 | tone(base, level));
                }
            }
    }

    static final class Items {
        static final int BRASS = 0xD4A640, IRON = 0xB8BCC2, GOLD = 0xF0C838, COPPER = 0xD07A4A;
        static final int LEATHER = 0x8A5A32, DARK_LEATHER = 0x5A3A20, ROPE = 0xC8A870, PAPER = 0xE8D8A8, INK = 0x2A1E14, RED = 0xA82020;

        static final String[] INGOT = {
                "", "", "", "", "",
                "    6666666666",
                "   56555555556",
                "  4555555555554",
                "  44444444444443",
                "  33333333333332",
                "  22222222222221",
                "   111111111111"};
        static final String[] NUGGET = {
                "", "", "", "", "", "", "", "",
                "      66",
                "     6554",
                "    655443   55",
                "    4443322 5543",
                "     22211  4332",
                "             211"};
        static final String[] RAW = {
                "", "", "",
                "     556",
                "    55665  5",
                "   4556655566",
                "  445555555665",
                "  344555544556",
                " 3344455443455",
                " 2333444333444",
                " 22233333223331",
                "  1122222112221",
                "   11111  1111"};
        static final String[] DUST = {
                "", "", "", "", "", "", "", "",
                "       6",
                "      656  5",
                "     55555",
                "    5544455",
                "   554444445",
                "  44443333444",
                " 3333322223333",
                "  222211112222"};
        // A brass plate: outlined, its rolled edge lit along the top and left, a rivet hole in each corner
        static final String[] PLATE = {
                "",
                "",
                "  111111111111",
                "  166666666651",
                "  165555555441",
                "  165155551441",
                "  165555555441",
                "  165555555441",
                "  165555555441",
                "  165555555441",
                "  165555555441",
                "  165155551441",
                "  164444444431",
                "  163333333331",
                "  111111111111"};

        // A horseshoe, toe up, heels down: lit along its outer left edge, four nail holes down each branch, square heel calks
        static final String[] HORSESHOE = {
                "",
                "    11111111",
                "  116665555411",
                "  165555555441",
                " 16555111144431",
                " 16521    12431",
                " 1651      1431",
                "16551      14431",
                "16521      12431",
                "16551      14431",
                "16521      12431",
                "16551      14431",
                "15521      12331",
                "15331      13331",
                "11111      11111"};

        static final String[] EGG = {
                "",
                "      1111",
                "     166541",
                "    16665441",
                "    16655441",
                "   1665554431",
                "   1655554431",
                "   1655544431",
                "   1555444331",
                "   1554444331",
                "   1544443321",
                "    14443321",
                "    14433221",
                "     133221",
                "      1111"};

        static int writeAll(File root) throws Exception {
            int n = 0;
            n += put(root, "brass_ingot", sprite(BRASS, null, INGOT));
            n += put(root, "brass_nugget", sprite(BRASS, null, NUGGET));
            n += put(root, "brass_plate", sprite(BRASS, null, PLATE));
            n += put(root, "brass_gear", gear());
            // A red handwheel (four spokes round a dark hub) on a brass bonnet, over a flanged length of pipe
            n += put(root, "pressure_valve", sprite(BRASS, pal('k', 0x3A2608, 'R', 0xD84040, 'r', 0x8E1E1E, 'h', 0x2A2A2E),
                    "    kkkkkkkk",
                    "  kkRRRRRRRRkk",
                    " kRrkkkRRkkkrRk",
                    " kRRRRRhhRRRRRk",
                    " krrkkkrrkkkrrk",
                    "  kkrrrrrrrrkk",
                    "    kkk65kkk",
                    "      k65k",
                    "     k6554k",
                    " kkkkk6554kkkkk",
                    "kk666665555444kk",
                    "k6k5555554444k3k",
                    "k5k4444443333k2k",
                    "kk333333322222kk",
                    " kkkkkkkkkkkkkk"));
            n += put(root, "iron_dust", sprite(0xD8CFC7, null, DUST));
            n += put(root, "gold_dust", sprite(GOLD, null, DUST));
            n += put(root, "copper_dust", sprite(COPPER, null, DUST));

            // Black powder ammo: paper cartridges (lead ball on top, twisted tail) and a tin of copper percussion caps
            n += put(root, "percussion_cap", sprite(0xA4A8AE, pal('k', 0x2A2C30, 'R', 0xC03434, 'r', 0x8E1E1E, 'W', 0xF0E8D8,
                            'c', 0xB86636, 'C', 0xF0A878, 'd', 0x6E3A1A),
                    "", "",
                    "    kkkkkkkk",
                    "   k66666655k",
                    "   k65555544k",
                    "   kkkkkkkkkk",
                    "   kRRRRRRrrk",
                    "   kRWkWWkWrk",
                    "   kRRRRRRrrk",
                    "   k55444332k",
                    "    kkkkkkkk",
                    "",
                    "  kk   kk   kk",
                    " kCck kCck kCck",
                    " kcdk kcdk kcdk",
                    "  kk   kk   kk"));
            java.util.Map<java.lang.Character, Integer> cartridge = pal('k', 0x3E3222, 'W', 0xD8DEE6, 'L', 0x9098A2, 'l', 0x5E646E, 't', 0x9A6E3A);
            n += put(root, "revolver_cartridge", sprite(PAPER, cartridge,
                    "", "",
                    "   kkkk",
                    "  kWLLlk",
                    "  kLLllk  kkkk",
                    "  kttttk kWLLlk",
                    "  k6654k kLLllk",
                    "  k6554k kttttk",
                    "  k5544k k6654k",
                    "  k5443k k6554k",
                    "  k4433k k5544k",
                    "  k4332k k5443k",
                    "   k32k  k4433k",
                    "    kk   k4332k",
                    "          k32k",
                    "           kk"));
            n += put(root, "rifle_cartridge", sprite(PAPER, cartridge,
                    "       kk",
                    "      kWlk",
                    "     kWLLlk",
                    "     kLLLlk",
                    "     kllllk",
                    "     kLLllk",
                    "     kttttk",
                    "     k6654k",
                    "     k6554k",
                    "     k6554k",
                    "     k5544k",
                    "     k5443k",
                    "     k4433k",
                    "     kttttk",
                    "      k32k",
                    "       kk"));
            n += put(root, "buckshot_cartridge", sprite(PAPER, cartridge,
                    "", "",
                    "     kk  kk",
                    "    kWLkkWLk",
                    "   kkLlkkLlkk",
                    "   k66555544k",
                    "   k65555443k",
                    "   k65554443k",
                    "   kttttttttk",
                    "   k65554433k",
                    "   k55544332k",
                    "   k55443332k",
                    "   k54433322k",
                    "    k443322k",
                    "     kk32kk",
                    "       kk"));

            // Cowboy gear icons
            n += put(root, "cowboy_hat", sprite(LEATHER, pal('b', 0x3A2416),
                    "", "", "", "",
                    "      5665",
                    "     566665",
                    "     555555",
                    "     444444",
                    "     bbbbbb",
                    "  4455555555544",
                    " 3444444444444443",
                    "  22333333333322",
                    "    2222222222"));
            // The duster: a long coat hanging open over a shirt and belt, lapels turned back, sleeves at its sides, the tails split
            n += put(root, "duster", garment(LEATHER, pal('s', 0xC8B898, 'L', tone(LEATHER, 6), 'y', 0xC9A227, 'c', tone(LEATHER, 2)),
                    "",
                    "    ###  ###",
                    "  #####ss#####",
                    " #####LssL#####",
                    " ### #LssL# ###",
                    " ### #LssL# ###",
                    " ### ##ss## ###",
                    " ### ##ss## ###",
                    " ### ##yy## ###",
                    " ccc ##  ## ccc",
                    "     ##  ##",
                    "     ##  ##",
                    "    ###  ###",
                    "    ###  ###"));
            // Chaps: two leather legs on a belt, silver conchos down the outer seams, fringe along the edges and the cuffs
            n += put(root, "chaps", garment(LEATHER, pal('b', 0x2A1E14, 'y', 0xC9A227, 'o', 0xC8CCD2, 'f', tone(LEATHER, 6)),
                    "",
                    "  bbbbbyybbbbb",
                    "  #####  #####",
                    " f#o###  ###o#f",
                    "  #####  #####",
                    " f#####  #####f",
                    "  #####  #####",
                    " f#o###  ###o#f",
                    "  #####  #####",
                    " f#####  #####f",
                    "  #####  #####",
                    " f#o###  ###o#f",
                    "  #####  #####",
                    "  #####  #####",
                    "  f f f  f f f"));
            n += put(root, "cowboy_boots", sprite(DARK_LEATHER, pal('s', 0xC8CCD2),
                    "", "", "",
                    "  5555    5555",
                    "  5665    5665",
                    "  5555    5555",
                    "  4554    4554",
                    "  4554    4554",
                    "  4554    4554",
                    "  44544   44544",
                    "  4444444 4444444",
                    "  33333333333333",
                    " s2222222 2222222s",
                    "  1   1    1   1"));
            // Snakeskin Boots: the cowboy boot cut in diamondback hide, dark leather soles and heels
            n += put(root, "snakeskin_boots", sprite(0xB89A6A, pal('s', 0xC8CCD2, 'd', 0x5A4030, 'k', shade(DARK_LEATHER, 0.8), 'h', shade(DARK_LEATHER, 0.55)),
                    "", "", "",
                    "  dddd    dddd",
                    "  5d65    5d65",
                    "  5555    5555",
                    "  4d54    4d54",
                    "  45d4    45d4",
                    "  4d54    4d54",
                    "  45d44   45d44",
                    "  4d444d4 4d444d4",
                    "  kkkkkkkkkkkkkk",
                    " shhhhhhh hhhhhhhs",
                    "  h   h    h   h"));
            n += put(root, "lasso", sprite(ROPE, null,
                    "",
                    "     5555555",
                    "   55       55",
                    "  5           5",
                    " 5             5",
                    " 4             4",
                    " 4             4",
                    "  4           4",
                    "   44       44",
                    "     4444444",
                    "       43",
                    "      43",
                    "     43",
                    "    433",
                    "   4332",
                    "    22"));
            n += put(root, "iron_horseshoes", sprite(IRON, null, HORSESHOE));
            n += put(root, "brass_horseshoes", sprite(BRASS, null, HORSESHOE));
            n += put(root, "saddlebags", sprite(LEATHER, pal('y', 0xC9A227, 'b', 0x3A2416),
                    "", "",
                    "   bbbbbbbbbb",
                    "  bb        bb",
                    " 5555      5555",
                    " 5665      5665",
                    " 5y55      55y5",
                    " 5555      5555",
                    " 5555      5555",
                    " 4444      4444",
                    " 4444      4444",
                    " 4444      4444",
                    " 3333      3333",
                    "  22        22"));

            // Bounty items
            n += put(root, "copper_coin", coin(0xC0703E, HORSESHOE_EMBLEM));
            n += put(root, "gold_coin", coin(0xC0922A, STAR_EMBLEM));
            n += put(root, "wanted_poster", sprite(PAPER, pal('d', INK, 'r', RED, 'f', 0xC68863),
                    " 55555555555555",
                    " 5ddddddddddd45",
                    " 55555555555545",
                    " 5555ddddd55545",
                    " 555ddddddd5545",
                    " 5555fffff55545",
                    " 5555fdfdf55545",
                    " 5555rrrrr55545",
                    " 555ddddddd5545",
                    " 55ddddddddd545",
                    " 55555555555545",
                    " 5d5d5dd5d5d545",
                    " 55555555555545",
                    " 55rrrrrrrr5545",
                    " 54444444444445",
                    "  3333333333333"));
            n += put(root, "outlaw_badge", badge());

            // Spawn eggs: the outlined egg in the mob's main colour, with its likeness drawn on (vanilla gives every mob its own egg)
            n += put(root, "outlaw_spawn_egg", egg(0x6A4A2E, pal('h', 0x3B2A1E, 'H', 0x5A4430, 'f', 0xC68863, 'e', 0x2B1D14, 'r', 0x9E1B1B, 'R', 0xC83A3A),
                    "", "",
                    "      hHHh",
                    "     hHHHhh",
                    "   hhhhhhhhhh",
                    "    ffffffff",
                    "    ffeffeff",
                    "    rrrrrrrr",
                    "    rRrrrrRr",
                    "     rrrrrr",
                    "      rrrr",
                    "       rr"));
            n += put(root, "outlaw_gunslinger_spawn_egg", egg(0x5E5A55, pal('h', 0x1E1C1A, 'H', 0x3A3632, 'f', 0xB57A55, 'e', 0x24323C, 's', 0x97623F, 'n', 0x4A4F57),
                    "", "",
                    "      hHHh",
                    "     hHHHhh",
                    "   hhhhhhhhhh",
                    "    ffffffff",
                    "    ffeffeff",
                    "    fssssssf",
                    "    nnnnnnnn"));
            n += put(root, "outlaw_boss_spawn_egg", egg(0x2A2626, pal('h', 0xD8CCB0, 'H', 0xF0E8D4, 'b', 0x1A1A1A, 'f', 0xC08560, 'e', 0x1C1410,
                            'm', 0x2A1A10, 'v', 0x7A1414, 'g', 0xE0B83A),
                    "", "",
                    "      hHHh",
                    "     hHHHhh",
                    "     bbbbbb",
                    "   hhhhhhhhhh",
                    "    ffeffeff",
                    "    fmmmmmmf",
                    "    ffmffmff",
                    "     vvvvvv",
                    "     vvggvv"));
            n += put(root, "townsfolk_spawn_egg", egg(0x2F5A3A, pal('h', 0x8A6440, 'H', 0xA88058, 'f', 0xD9A07A, 'e', 0x3A5A7A, 'm', 0xA86E4F, 'w', 0xE8E2D4),
                    "", "",
                    "      hHHh",
                    "     hHHHhh",
                    "   hhhhhhhhhh",
                    "    ffffffff",
                    "    ffeffeff",
                    "    fffmmfff",
                    "     wwwwww",
                    "     4wwww4",
                    "      4ww4"));
            n += put(root, "barmaid_spawn_egg", egg(0x7A1E2E, pal('a', 0x5A3020, 'A', 0x7A4630, 'f', 0xE0B090, 'e', 0x3A2A20, 'r', 0xC04050, 'l', 0xF0EAD8),
                    "", "",
                    "      aAAa",
                    "     aAAAAa",
                    "    aAffffAa",
                    "    afeffefa",
                    "    affrrffa",
                    "    aaffffaa",
                    "    llllllll",
                    "     l l  l l"));
            n += put(root, "lawman_spawn_egg", egg(0x2A2420, pal('h', 0x3A2A1E, 'H', 0x5A4430, 'f', 0xD09A72, 'e', 0x3A5A7A, 'm', 0x4A3A2A, 'w', 0xE8E2D4,
                            's', 0xE8C840, 'S', 0xB08A20),
                    "", "",
                    "      hHHh",
                    "     hHHHhh",
                    "   hhhhhhhhhh",
                    "    ffffffff",
                    "    ffeffeff",
                    "    ffmmmmff",
                    "      wwww",
                    "      ww s",
                    "        sSs",
                    "        s s"));
            n += put(root, "rattlesnake_spawn_egg", egg(0xB89A6A, pal('d', 0x5A4030, 'l', 0xE0C898),
                    "", "",
                    "       dd",
                    "      dlld",
                    "       dd",
                    "    d      d",
                    "    ld dd dl",
                    "    d dlld d",
                    "       dd",
                    "    d      d",
                    "    ld dd dl",
                    "      dlld",
                    "       dd"));
            // The Hollow Earth's small creatures (9b2)
            n += put(root, "fern_skitter_spawn_egg", egg(Creatures.SKITTER, pal('S', Creatures.SKITTER_STRIPE, 'e', 0x1A1A10, 'w', 0xF2E6A0, 'b', Creatures.SKITTER_BELLY),
                    "", "", "", "", "",
                    "    we    ew",
                    "",
                    "     S    S",
                    "      S  S",
                    "       SS",
                    "     S    S",
                    "      S  S",
                    "       SS",
                    "      bbbb"));
            n += put(root, "glassback_hare_spawn_egg", egg(Creatures.HARE, pal('k', 0x4A4436, 'p', Creatures.EAR_IN, 'e', 0x2A2420,
                            'c', Creatures.CRYSTAL, 'C', Creatures.CRYSTAL_HI, 'd', Creatures.CRYSTAL_DARK),
                    "     kk  kk",
                    "     kpkkpk",
                    "      p  p",
                    "", "", "",
                    "     e    e",
                    "       pp",
                    "",
                    "        C",
                    "       cCd",
                    "     C cd",
                    "     cd"));
            n += put(root, "ash_salamander_spawn_egg", egg(Creatures.SALAMANDER, pal('o', Creatures.SPOT, 'O', Creatures.SPOT_HOT),
                    "", "", "", "",
                    "      O",
                    "         o",
                    "     O    O",
                    "     o",
                    "          O",
                    "       o",
                    "    o",
                    "         O",
                    "      o"));
            n += put(root, "pale_raven_spawn_egg", egg(Creatures.RAVEN, pal('e', 0x1A1A1E, 'b', Creatures.BEAK, 'B', Creatures.RAVEN_DARK, 'f', Creatures.RAVEN_GREY),
                    "", "", "", "", "",
                    "     e    e",
                    "      bBBb",
                    "       bb",
                    "",
                    "    f  ff  f",
                    "     ff  ff",
                    "      f  f"));
            n += put(root, "gray_probe_spawn_egg", egg(Creatures.PROBE, pal('k', Creatures.PROBE_DARK, 'g', Creatures.PROBE_GLOW, 'G', Creatures.PROBE_GLOW_HI, 'e', 0x101418),
                    "", "", "", "",
                    "     kkkkkk",
                    "",
                    "      gggg",
                    "     gGGGGg",
                    "     gGeeGg",
                    "      gggg",
                    "",
                    "     kkkkkk"));
            n += put(root, "woolly_mammoth_spawn_egg", egg(Megafauna.MAMMOTH, pal('F', Megafauna.MAMMOTH_LIGHT, 'd', Megafauna.MAMMOTH_DARK, 'e', 0x1A100A, 'i', Megafauna.IVORY),
                    "", "", "",
                    "      F  F",
                    "     F  F",
                    "",
                    "     e    e",
                    "      dddd",
                    "       dd",
                    "    i  dd  i",
                    "    ii dd ii",
                    "     i dd i",
                    "       dd"));
            n += put(root, "terror_bird_spawn_egg", egg(Megafauna.BIRD, pal('c', Megafauna.CREST, 'e', Megafauna.BIRD_EYE, 'B', Megafauna.BEAK, 'b', Megafauna.BEAK_DARK,
                            't', Megafauna.BEAK_TIP, 'w', Megafauna.BIRD_CHEST),
                    "", "",
                    "       cc",
                    "      cc",
                    "",
                    "     e    e",
                    "      BBBB",
                    "     BBBBBb",
                    "       BBbt",
                    "         t",
                    "",
                    "     wwwwww",
                    "      wwww"));
            n += put(root, "megatherium_spawn_egg", egg(Megafauna.SLOTH, pal('L', Megafauna.SLOTH_LIGHT, 'S', Megafauna.SLOTH_SKIN, 'e', 0x1A100A, 'n', 0x2A1E18, 'c', Megafauna.CLAW),
                    "", "", "", "",
                    "     SSSSSS",
                    "     SeSSeS",
                    "     SSnnSS",
                    "      SSSS",
                    "",
                    "     L    L",
                    "     c c c",
                    "     c c c",
                    "      c c c"));
            n += put(root, "glyptodon_spawn_egg", egg(Megafauna.SHELL, pal('d', Megafauna.SHELL_DARK, 'l', Megafauna.SHELL_LIGHT, 'g', Megafauna.GLYPT, 'e', 0x1A140E),
                    "", "", "",
                    "      d  d",
                    "     dddddd",
                    "    dl dd ld",
                    "    d  dd  d",
                    "    dddddddd",
                    "      d  d",
                    "     ld  dl",
                    "    dddddddd",
                    "      gggg",
                    "      egge"));
            n += put(root, "gray_sentinel_spawn_egg", egg(Creatures.SENTINEL, pal('k', Creatures.SENTINEL_DARK, 'g', Creatures.SENTINEL_GLOW, 'G', Creatures.SENTINEL_GLOW_HI,
                            'h', Creatures.SENTINEL_HI),
                    "       g",
                    "       k",
                    "", "", "",
                    "    kkkkkkkk",
                    "    gGGGGGGg",
                    "    kkkkkkkk",
                    "",
                    "     h    h",
                    "",
                    "     kkkkkk"));
            n += put(root, "man_in_black_spawn_egg", egg(0x18181C, pal('h', 0x0E0E10, 'H', 0x2A2A2E, 'f', 0xD8CEC4, 'e', 0x0C0C10, 'w', 0xEAEAEA, 't', 0x0E0E10),
                    "", "",
                    "      hHHh",
                    "     hHHHhh",
                    "   hhhhhhhhhh",
                    "    ffffffff",
                    "    feeffeef",
                    "    ffffffff",
                    "     fwwwwf",
                    "      wttw",
                    "       tt"));

            // Dynamite: red paper stick, tan end caps, black fuse with a spark
            n += put(root, "dynamite", sprite(RED, pal('c', PAPER, 'f', INK, 'y', 0xFFE060, 'o', 0xFF8020),
                    "             y",
                    "            yoy",
                    "             f",
                    "            f",
                    "          cf",
                    "         c4c",
                    "        4565c",
                    "       45654",
                    "      45654",
                    "     45654",
                    "    45654",
                    "   45654",
                    "  c5654",
                    "  c654",
                    "   cc"));
            // Snakeskin: a curled shed skin, tan with dark diamonds
            n += put(root, "snakeskin", sprite(0xB89A6A, pal('d', 0x5A4030, 'b', 0xE8DCC0),
                    "",
                    "         4444",
                    "       44d55d4",
                    "      4d5bb5d54",
                    "      45b44b54",
                    "      4d5b4",
                    "       45d5",
                    "        4d54",
                    "    444  45d4",
                    "   4d554  4d54",
                    "  45bb5d4 45b4",
                    "  4d5445d5d5d4",
                    "   4d4 455d54",
                    "    44  4444",
                    "",
                    ""));
            // Bandana: a folded kerchief, point down, knotted at the top. Pale cloth with a paisley dot; the item model tints it.
            n += put(root, "bandana", sprite(0xD0D0D0, pal('p', 0x8C8C8C, 'k', 0x4E4E4E),
                    " kk          kk",
                    " k5k        k4k",
                    "  k5k  kk  k4k",
                    "   kkkk65kkkk",
                    "  k6666666655k",
                    "  k5p5555p554k",
                    "  kp5p55p5p54k",
                    "   k55p55p54k",
                    "   k5p5p5p44k",
                    "    k55p544k",
                    "     k5p54k",
                    "     kp5p4k",
                    "      k54k",
                    "       kk"));
            // Brass Star: a five-pointed tin star with ball tips and an engraved centre
            n += put(root, "brass_star", sprite(BRASS, pal('k', 0x6A4A18, 'w', 0xFFF4C8),
                    "",
                    "       6",
                    "      6w5",
                    "      654",
                    "     65544",
                    " 6w66655544443",
                    "  6655555554k",
                    "   65555k5543",
                    "    555k5k54",
                    "    5555k554",
                    "   5554 4554",
                    "   554   553",
                    "  6w4     4k3",
                    "   4       k",
                    "",
                    ""));
            // The Frontier Gazette: folded broadsheet, bold masthead, columns of type
            n += put(root, "frontier_gazette", sprite(PAPER, pal('k', INK, 'g', 0x8A7E68),
                    "",
                    "  555555555555",
                    "  5kkkkkkkkkk5",
                    "  5kkkkkkkkkk5",
                    "  555555555555",
                    "  5gggg5gggg53",
                    "  5555555555 3",
                    "  5gggg5gggg53",
                    "  5ggg55gggg53",
                    "  555555555553",
                    "  5gggg5gggg53",
                    "  5gggg5ggg553",
                    "  555555555553",
                    "  4444444444443",
                    "   33333333333",
                    ""));

            // Saloon drinks: g glass edge, c cork, L label, k ink
            java.util.Map<java.lang.Character, Integer> bottle = pal('g', 0x9AB4B4, 'G', 0xD8ECEC, 'c', 0x9A6A3A, 'L', PAPER, 'k', INK, 'r', RED);
            n += put(root, "rye_whiskey", sprite(0xB86A1E, bottle,
                    "",
                    "      cc",
                    "      cc",
                    "      gG",
                    "      gG",
                    "     g55G",
                    "    g5665G",
                    "   g566655G",
                    "   g555555G",
                    "   gLLLLLLg",
                    "   gLkkkkLg",
                    "   gLLrrLLg",
                    "   g444443g",
                    "   g333332g",
                    "    g2222g",
                    "     gggg"));
            n += put(root, "sarsaparilla", sprite(0x5E2E16, bottle,
                    "      cc",
                    "      gG",
                    "      gG",
                    "      gG",
                    "     g55G",
                    "     g56G",
                    "    g5555G",
                    "    grrrrg",
                    "    gLkkLg",
                    "    gLLLLg",
                    "    grrrrg",
                    "    g4443g",
                    "    g4433g",
                    "    g3332g",
                    "    g3322g",
                    "     gggg"));
            n += put(root, "mezcal", sprite(0xD8C468, pal('g', 0x9AB4B4, 'G', 0xD8ECEC, 'c', 0x9A6A3A, 'v', 0x3E7048, 'V', 0x6EA060, 'w', 0xC88A60),
                    "",
                    "      cc",
                    "      gG",
                    "     g55G",
                    "     g56G",
                    "    g5665G",
                    "    g5555G",
                    "   gvvVvvvg",
                    "   gvVVVVvg",
                    "   gvvVvvvg",
                    "   g444444g",
                    "   g44w4443g",
                    "   g4ww4433g",
                    "   g3333332g",
                    "    g22222g",
                    "     ggggg"));
            n += put(root, "snake_oil", sprite(0x3A62A8, pal('g', 0x5E86C8, 'G', 0xB8D0F0, 'c', 0x2A2A2E, 'L', PAPER, 'k', INK, 's', 0x3E7048),
                    "", "",
                    "       cc",
                    "      cccc",
                    "       gG",
                    "       gG",
                    "    gg55GG",
                    "    g5665G",
                    "    gLLLLg",
                    "    gLsksg",
                    "    gLksLg",
                    "    gLLLLg",
                    "    g4443g",
                    "    g3332g",
                    "    gggggg"));
            n += put(root, "moonshine", sprite(0xD2C2A0, pal('b', 0x7A4A26, 'B', 0xA06A3A, 'c', 0x9A6A3A, 'k', INK),
                    "", "",
                    "       cc",
                    "      bBBb   ",
                    "      bbbb 33",
                    "     566665  3",
                    "    56666665 3",
                    "   5666666665 3",
                    "   566k6k6k65 3",
                    "   5666k6k6653",
                    "   566k6k6k65",
                    "   5555555554",
                    "   4444444443",
                    "   3444444432",
                    "    33333322",
                    "     222222"));
            n += put(root, "frontier_beer", sprite(0x8A5A32, pal('f', 0xF4EEDC, 'F', 0xD8D0BC, 'a', 0xD89A30, 'i', 0x6A6E74, 'I', 0x9AA0A6),
                    "", "",
                    "    fFfffFf",
                    "   fffffffff",
                    "   FfffFffff",
                    "   6aaaaaaa5 44",
                    "   iIIIIIIIi  4",
                    "   656565654  4",
                    "   656565654  4",
                    "   545454543  4",
                    "   545454543 44",
                    "   iIIIIIIIi",
                    "   434343432",
                    "   323232321",
                    "    2222222"));
            n += put(root, "cowboy_coffee", sprite(0x3A5A8A, pal('w', 0xE8ECF0, 'k', 0x3A2416, 's', 0xC8CCD0),
                    "",
                    "     s   s",
                    "      s   s",
                    "     s   s",
                    "      s   s",
                    "",
                    "   wwwwwwwww",
                    "   6kkkkkkk5",
                    "   65w55555544",
                    "   5555w5554  4",
                    "   55555555 4 4",
                    "   4w4444444  4",
                    "   444444w4 44",
                    "   33333333",
                    "    222222"));

            // Furniture / steam items
            n += put(root, "saloon_doors", sprite(0x8A5A32, pal('k', 0x3A2416),
                    "",
                    " kkkkkk  kkkkkk",
                    " k5555k  k5555k",
                    " k4444k  k4444k",
                    " k5555k  k5555k",
                    " k4444k  k4444k",
                    " k5555k  k5555k",
                    " k4444k  k4444k",
                    " k5555k  k5555k",
                    " k4444k  k4444k",
                    " k5555k  k5555k",
                    " kkkkkk  kkkkkk"));
            // A brass bottle banded in iron: a red valve knob on top, a pressure gauge on its flank
            n += put(root, "steam_canister", sprite(BRASS, pal('k', 0x3A2608, 'R', 0xD84040, 'r', 0x8E1E1E, 's', 0x2A2A2E,
                            'I', 0xA0A4AA, 'i', 0x5E6268, 'g', 0xF0E8D8, 'n', 0xA82020),
                    "      kkkk",
                    "     kRRrrk",
                    "      kssk",
                    "     kk55kk",
                    "   kk666655kk",
                    "   k66555544k",
                    "   kIIIIIiiik",
                    "   k6kkkk443k",
                    "   k6kgnk443k",
                    "   k6kggk433k",
                    "   k5kkkk433k",
                    "   kIIIIIiiik",
                    "   k55544332k",
                    "   k54443322k",
                    "   kk443322kk",
                    "     kkkkkk"));
            n += put(root, "steam_drill", drill());
            n += put(root, "steam_handcar", sprite(0x6A6E74, pal('k', 0x1E1E22, 'y', BRASS, 'Y', shade(BRASS, 0.7), 'f', 0xF0A030),
                    "          kk    ",
                    "         kkkk   ",
                    "          kk    ",
                    "          kk    ",
                    "        yyyyyy  ",
                    "        yYYYYy  ",
                    "        yyyyyy  ",
                    "        kffffk  ",
                    " 6555555555555k ",
                    " 5444444444444k ",
                    " 5444444444444k ",
                    " 4333333333333k ",
                    "  43333333333k  ",
                    "   kk     kk    ",
                    "  k22k   k22k   ",
                    "   kk     kk    "));
            return n;
        }

        static int put(File root, String name, BufferedImage img) throws Exception {
            write(new File(root, "item/" + name + ".png"), img);
            return 1;
        }

        /**
         * A garment icon from a mask: '#' is the cloth, lit from the left (lighter toward the left, darker toward the right)
         * and a shade lighter along its top rows; every empty pixel touching the cloth becomes the dark outline, so narrow
         * sleeves and legs keep their colour. Other letters come from {@code palette}. Leave a free row and column round
         * the mask for the outline.
         */
        static BufferedImage garment(int base, java.util.Map<java.lang.Character, Integer> palette, String... mask) {
            BufferedImage img = new BufferedImage(16, 16, BufferedImage.TYPE_INT_ARGB);
            for (int y = 0; y < 16; y++) {
                for (int x = 0; x < 16; x++) {
                    char ch = filled(mask, x, y);
                    if (ch == '#') {
                        img.setRGB(x, y, 0xFF000000 | tone(base, Math.min(6, (x <= 4 ? 5 : x <= 10 ? 4 : 3) + (y <= 2 ? 1 : 0))));
                    } else if (ch != ' ') {
                        img.setRGB(x, y, 0xFF000000 | palette.get(ch));
                    } else if (filled(mask, x - 1, y) == '#' || filled(mask, x + 1, y) == '#' || filled(mask, x, y - 1) == '#' || filled(mask, x, y + 1) == '#') {
                        img.setRGB(x, y, 0xFF000000 | tone(base, 1));
                    }
                }
            }
            return img;
        }

        /** The mask character at (x, y), or ' ' outside it. */
        static char filled(String[] mask, int x, int y) {
            return y < 0 || y >= mask.length || x < 0 || x >= mask[y].length() ? ' ' : mask[y].charAt(x);
        }

        static BufferedImage gear() {
            BufferedImage img = new BufferedImage(16, 16, BufferedImage.TYPE_INT_ARGB);
            gearOnto(img, 8, 8, 7.4, 5.6, 2.1, BRASS);
            return img;
        }

        /** 8-tooth gear: teeth reach toothR, body reaches bodyR, axle hole of holeR; lit from the top-left. */
        static void gearOnto(BufferedImage img, double cx, double cy, double toothR, double bodyR, double holeR, int base) {
            for (int x = 0; x < img.getWidth(); x++)
                for (int y = 0; y < img.getHeight(); y++) {
                    double dx = x + 0.5 - cx, dy = y + 0.5 - cy, d = Math.sqrt(dx * dx + dy * dy);
                    boolean tooth = Math.cos(Math.atan2(dy, dx) * 8) > 0.3;
                    double outer = tooth ? toothR : bodyR;
                    if (d <= outer && d > holeR) {
                        int level = d > outer - 1 ? 3 : (dx + dy < -2 ? 6 : dx + dy > 2 ? 3 : 5);
                        if (d < holeR + 1) level = 2;
                        img.setRGB(x, y, 0xFF000000 | tone(base, level));
                    }
                }
        }

        static final String[] HORSESHOE_EMBLEM = {
                "", "", "", "", "",
                "     x    x",
                "     x    x",
                "     x    x",
                "     xx  xx",
                "      xxxx"};
        static final String[] STAR_EMBLEM = {
                "", "", "", "", "",
                "       xx",
                "     xxxxxx",
                "      xxxx",
                "     xx  xx"};

        /** A struck coin: dark outline, a raised rim lit from the top-left, and a raised emblem casting a shadow down-right. */
        static BufferedImage coin(int base, String[] emblem) {
            BufferedImage img = new BufferedImage(16, 16, BufferedImage.TYPE_INT_ARGB);
            for (int x = 0; x < 16; x++)
                for (int y = 0; y < 16; y++) {
                    double dx = x + 0.5 - 8, dy = y + 0.5 - 8, d = Math.sqrt(dx * dx + dy * dy);
                    if (d > 6.9) continue;
                    int level = d > 6.0 ? 1 : d > 4.6 ? (dx + dy < 0 ? 5 : 3) : 4;
                    img.setRGB(x, y, 0xFF000000 | tone(base, level));
                }
            for (int y = 0; y < emblem.length; y++)
                for (int x = 0; x < emblem[y].length(); x++)
                    if (emblem[y].charAt(x) == 'x') {
                        img.setRGB(x, y, 0xFF000000 | tone(base, 6));
                        boolean shadowFree = y + 1 >= emblem.length || x + 1 >= emblem[y + 1].length() || emblem[y + 1].charAt(x + 1) != 'x';
                        if (shadowFree) img.setRGB(x + 1, y + 1, 0xFF000000 | tone(base, 3));
                    }
            return img;
        }

        static BufferedImage badge() {
            BufferedImage img = new BufferedImage(16, 16, BufferedImage.TYPE_INT_ARGB);
            int iron = 0x9EA4AC;
            for (int x = 0; x < 16; x++)
                for (int y = 0; y < 16; y++) {
                    double dx = x + 0.5 - 8, dy = y + 0.5 - 8.4;
                    double r = Math.sqrt(dx * dx + dy * dy), a = Math.atan2(dy, dx) + Math.PI / 2;
                    double star = 3.2 + 4.2 * Math.pow(Math.abs(Math.cos(a * 2.5)), 6);
                    if (r <= star) {
                        int level = r > star - 0.9 ? 2 : (dx + dy < -1.5 ? 6 : dx + dy > 1.5 ? 3 : 4);
                        img.setRGB(x, y, 0xFF000000 | tone(iron, level));
                    }
                }
            disc(img, 8, 8.4, 2.2, RED);
            return img;
        }

        /** A spawn egg: the outlined egg in {@code base} with the mob's likeness drawn over it (digits in the overlay are base tones). */
        static BufferedImage egg(int base, java.util.Map<java.lang.Character, Integer> palette, String... overlay) {
            BufferedImage img = sprite(base, null, EGG);
            draw(img, 0, 0, base, palette, overlay);
            return img;
        }

        // Steam drill: walnut grip bottom-left, a fat brass boiler housing, an iron collar, and a fluted steel bit to the top-right.
        static BufferedImage drill() {
            int steel = 0xB8BEC6;
            BufferedImage img = lathe(new double[][] {
                    {0.6, 1.6, 1.2, 1.2, 0x4A4E54},  // pommel
                    {1.6, 7.4, 1.0, 1.2, 0x7A4A26},  // grip
                    {7.4, 8.4, 2.0, 2.0, 0x6A6E74},  // guard
                    {8.4, 14.2, 2.7, 2.5, BRASS},   // housing
                    {14.2, 15.4, 1.7, 1.7, 0x8A9098}, // collar
                    {15.4, 22.4, 1.6, 0.2, steel}});  // bit
            for (int x = 0; x < 16; x++)
                for (int y = 0; y < 16; y++) {
                    double u = (x - y + 16) / Math.sqrt(2), v = (x + y - 15) / Math.sqrt(2);
                    int p = img.getRGB(x, y);
                    if ((p >>> 24) == 0 || u < 15.4) continue;
                    if (Math.floorMod((int) Math.round(u * Math.sqrt(2) + v * Math.sqrt(2)), 3) == 0 && Math.abs(v) < 1.0) {
                        img.setRGB(x, y, 0xFF000000 | tone(steel, 2)); // spiral flutes
                    }
                }
            img.setRGB(7, 9, 0xFFF0E8D8); // pressure gauge on the housing
            img.setRGB(8, 8, 0xFFF0E8D8);
            img.setRGB(8, 9, 0xFFA82020);
            img.setRGB(4, 11, 0xFFA82020); // valve knob
            return img;
        }

        /**
         * A turned shape along the item diagonal, from the bottom-left corner (u = 0) to the top-right (u ~ 22.6). Each segment
         * is {from u, to u, radius at from, radius at to, colour}; lit from the top-left, outlined in its own darkest tone.
         */
        static BufferedImage lathe(double[][] segments) {
            BufferedImage img = new BufferedImage(16, 16, BufferedImage.TYPE_INT_ARGB);
            int[][] base = new int[16][16];
            for (int x = 0; x < 16; x++)
                for (int y = 0; y < 16; y++) {
                    double u = (x - y + 16) / Math.sqrt(2), v = (x + y - 15) / Math.sqrt(2) - 0.35;
                    for (double[] s : segments) {
                        if (u < s[0] || u >= s[1]) continue;
                        double r = s[2] + (s[3] - s[2]) * (u - s[0]) / (s[1] - s[0]);
                        if (Math.abs(v) > r + 0.36) break;
                        int c = (int) s[4];
                        double t = v / Math.max(r + 0.36, 0.5);
                        base[x][y] = 0xFF000000 | c;
                        img.setRGB(x, y, 0xFF000000 | tone(c, t < -0.45 ? 6 : t < 0.05 ? 5 : t < 0.5 ? 4 : 3));
                        break;
                    }
                }
            BufferedImage out = new BufferedImage(16, 16, BufferedImage.TYPE_INT_ARGB);
            for (int x = 0; x < 16; x++)
                for (int y = 0; y < 16; y++) {
                    if (base[x][y] == 0) continue;
                    boolean edge = x == 0 || y == 0 || x == 15 || y == 15
                            || base[x - 1][y] == 0 || base[x + 1][y] == 0 || base[x][y - 1] == 0 || base[x][y + 1] == 0;
                    out.setRGB(x, y, edge ? 0xFF000000 | tone(base[x][y] & 0xFFFFFF, 1) : img.getRGB(x, y));
                }
            return out;
        }
    }

    // ---------------------------------------------------------------- block tiles (16x16, opaque)

    static final class Tiles {
        static final int STONE = 0x7E7E7E, DEEPSLATE = 0x4A4A52;
        static final int BRASS = Items.BRASS, COPPER = 0xB8693A, IRON = 0x8C9096, DARK_IRON = 0x3A3C40;
        static final int WOOD = 0x8A5A32, DARK_WOOD = 0x4E3018, BRICK = 0x8E4434;

        static int writeAll(File root) throws Exception {
            int n = 0;
            n += put(root, "brass_block", metalBlock(BRASS, 7));
            n += put(root, "bounty_board", bountyBoard());
            n += put(root, "bar_counter_top", planks(0x5A3418, true, 8, true));
            n += put(root, "bar_counter_side", counterSide());
            n += put(root, "bar_counter_bottom", planks(0x3E2410, true, 9, false));
            n += put(root, "bar_stool", planks(DARK_WOOD, false, 10, true));
            n += put(root, "bar_stool_cushion", cushion());
            n += put(root, "saloon_piano_wood", planks(PIANO_WOOD, true, 21, true));
            n += put(root, "saloon_piano_front", pianoFront());
            n += put(root, "saloon_piano_keys", pianoKeys());
            n += put(root, "card_table_felt", felt());
            n += put(root, "card_table_wood", planks(DARK_WOOD, true, 24, true));
            write(new File(root, "entity/bounty_board.png"), hangingBoard());
            write(new File(root, "item/hanging_bounty_board.png"), hangingBoardIcon());
            n += 2;
            n += put(root, "saloon_doors_top", doorHalf(true));
            n += put(root, "saloon_doors_bottom", doorHalf(false));
            n += put(root, "boiler_side", boilerSide(false));
            n += put(root, "boiler_side_on", boilerSide(true));
            n += put(root, "boiler_top", boilerTop());
            n += put(root, "boiler_bottom", bricks());
            n += put(root, "copper_pipe", pipe());
            n += put(root, "steam_tank_side", tankSide());
            n += put(root, "steam_tank_top", hatch(BRASS, 12));
            n += put(root, "steam_tank_bottom", noise(shade(BRASS, 0.55), 6, 13));
            n += put(root, "crusher_side", crusherSide());
            n += put(root, "crusher_top", crusherTop());
            n += put(root, "crusher_bottom", noise(DARK_IRON, 6, 14));
            n += put(root, "pressure_gauge", gauge());
            n += put(root, "handcar_engine_iron", engineIron());
            n += put(root, "handcar_engine_brass", engineBrass());
            n += put(root, "handcar_engine_firebox", engineFirebox(false));
            n += put(root, "handcar_engine_firebox_on", engineFirebox(true));
            return n;
        }

        static int put(File root, String name, BufferedImage img) throws Exception {
            write(new File(root, "block/" + name + ".png"), img);
            return 1;
        }

        static BufferedImage blank() {
            return new BufferedImage(16, 16, BufferedImage.TYPE_INT_ARGB);
        }

        static void px(BufferedImage img, int x, int y, int rgb) {
            if (x >= 0 && y >= 0 && x < 16 && y < 16) img.setRGB(x, y, 0xFF000000 | rgb);
        }

        static int jitter(int rgb, Random r, int amount) {
            int n = r.nextInt(amount * 2 + 1) - amount;
            return (clamp(((rgb >> 16) & 0xFF) + n) << 16) | (clamp(((rgb >> 8) & 0xFF) + n) << 8) | clamp((rgb & 0xFF) + n);
        }

        static BufferedImage noise(int base, int amount, long seed) {
            BufferedImage img = blank();
            Random r = new Random(seed);
            for (int x = 0; x < 16; x++) for (int y = 0; y < 16; y++) px(img, x, y, jitter(base, r, amount));
            return img;
        }

        /** Mottled stone: noise plus a few darker and lighter blotches. */
        static BufferedImage stone(long seed) {
            BufferedImage img = noise(STONE, 7, seed);
            Random r = new Random(seed * 31);
            for (int b = 0; b < 7; b++) {
                int cx = r.nextInt(16), cy = r.nextInt(16), len = 2 + r.nextInt(3);
                int c = r.nextBoolean() ? shade(STONE, 0.82) : shade(STONE, 1.12);
                for (int i = 0; i < len; i++) px(img, cx + i, cy + (r.nextInt(3) == 0 ? 1 : 0), jitter(c, r, 4));
            }
            return img;
        }

        static BufferedImage deepslate(long seed) {
            BufferedImage img = noise(DEEPSLATE, 5, seed);
            Random r = new Random(seed * 17);
            for (int y = 0; y < 16; y += 3 + r.nextInt(2)) {
                for (int x = 0; x < 16; x++) if (r.nextInt(4) != 0) px(img, x, y, jitter(shade(DEEPSLATE, 0.8), r, 3));
            }
            return img;
        }

        /** Scatter 4 ore clusters with a bright core and darker rim. */
        static BufferedImage ore(BufferedImage base, int ore, long seed) {
            Random r = new Random(seed);
            int[][] centers = {{3, 3}, {11, 4}, {5, 11}, {12, 12}};
            for (int[] c : centers) {
                int cx = c[0] + r.nextInt(2), cy = c[1] + r.nextInt(2);
                // dark rim first so the bright crystal pops against stone
                for (int[] o : new int[][] {{-1, 0}, {0, -1}, {2, 0}, {2, 1}, {0, 2}, {1, 2}, {-1, 1}, {1, -1}}) {
                    px(base, cx + o[0], cy + o[1], 0x2E3A42);
                }
                px(base, cx, cy, 0xE4F4FF);
                px(base, cx + 1, cy, shade(ore, 1.1));
                px(base, cx, cy + 1, ore);
                px(base, cx + 1, cy + 1, shade(ore, 0.7));
                if (r.nextBoolean()) { px(base, cx + 3, cy + 1, ore); px(base, cx + 3, cy + 2, 0x2E3A42); }
            }
            return base;
        }

        static BufferedImage rawBlock(int base, long seed) {
            BufferedImage img = noise(tone(base, 3), 8, seed);
            Random r = new Random(seed);
            for (int b = 0; b < 10; b++) {
                int cx = r.nextInt(14), cy = r.nextInt(14);
                px(img, cx, cy, tone(base, 6));
                px(img, cx + 1, cy, tone(base, 5));
                px(img, cx, cy + 1, tone(base, 4));
                px(img, cx + 1, cy + 2, tone(base, 2));
                px(img, cx + 2, cy + 1, tone(base, 2));
            }
            return img;
        }

        /** Riveted metal block: dark seam border, bevelled panel, diagonal sheen, corner rivets. */
        static BufferedImage metalBlock(int base, long seed) {
            BufferedImage img = noise(tone(base, 4), 4, seed);
            for (int i = 0; i < 16; i++) {
                px(img, i, 0, tone(base, 2));
                px(img, i, 15, tone(base, 2));
                px(img, 0, i, tone(base, 2));
                px(img, 15, i, tone(base, 2));
                if (i > 0 && i < 15) {
                    px(img, i, 1, tone(base, 6));
                    px(img, 1, i, tone(base, 5));
                    px(img, i, 14, tone(base, 3));
                    px(img, 14, i, tone(base, 3));
                }
            }
            for (int i = 3; i < 9; i++) px(img, i, 12 - i, tone(base, 5));
            for (int[] c : new int[][] {{3, 3}, {12, 3}, {3, 12}, {12, 12}}) {
                px(img, c[0], c[1], tone(base, 6));
                px(img, c[0] + 1, c[1] + 1, tone(base, 2));
            }
            return img;
        }

        /** Planks; horizontal boards (4 px) or vertical; optional polish highlight. */
        static BufferedImage planks(int base, boolean horizontal, long seed, boolean polished) {
            BufferedImage img = blank();
            Random r = new Random(seed);
            for (int a = 0; a < 16; a++)
                for (int b = 0; b < 16; b++) {
                    int board = b / 4, inBoard = b % 4;
                    int c = jitter(shade(base, 1 + (board % 2) * 0.06), r, 5);
                    if (inBoard == 3) c = shade(base, 0.6);              // seam
                    else if (polished && inBoard == 0) c = shade(base, 1.18);
                    if ((a + board * 5) % 9 == 0 && inBoard == 1) c = shade(base, 0.82); // grain
                    if (horizontal) px(img, a, b, c); else px(img, b, a, c);
                }
            return img;
        }

        static final int PIANO_WOOD = 0x3A1E10;

        /** Upright piano front: two raised panels in a frame, a brass candle sconce each side, a carved rail. */
        static BufferedImage pianoFront() {
            BufferedImage img = planks(PIANO_WOOD, false, 22, true);
            int frame = shade(PIANO_WOOD, 0.65), panel = shade(PIANO_WOOD, 1.25);
            for (int[] r : new int[][] {{2, 2, 6, 6}, {9, 2, 13, 6}, {2, 10, 13, 14}}) { // upper panels, kick board
                for (int x = r[0]; x <= r[2]; x++) for (int y = r[1]; y <= r[3]; y++) {
                    boolean edge = x == r[0] || x == r[2] || y == r[1] || y == r[3];
                    px(img, x, y, edge ? frame : shade(panel, 0.95 + ((x + y) % 3) * 0.03));
                }
            }
            for (int x = 1; x < 15; x++) px(img, x, 8, shade(PIANO_WOOD, 1.45)); // carved rail
            for (int[] s : new int[][] {{1, 3}, {14, 3}}) { // sconces with a candle
                px(img, s[0], s[1], BRASS);
                px(img, s[0], s[1] - 1, 0xF0EAD8);
                px(img, s[0], s[1] - 2, 0xFFD060);
            }
            return img;
        }

        /** Keys seen from above on the keybed (rows 3-6 = the keys element's top face): ivory, with black sharps behind. */
        static BufferedImage pianoKeys() {
            BufferedImage img = planks(PIANO_WOOD, true, 23, true);
            for (int x = 1; x < 15; x++) {
                for (int y = 3; y < 7; y++) px(img, x, y, (x % 2 == 0) ? 0xF0EAD8 : 0xE2DAC4);
                px(img, x, 3, 0xFAF6EA); // front lip
                int inOctave = Math.floorMod(x - 1, 7);
                if (inOctave != 2 && inOctave != 6) { // no sharp after E and B
                    px(img, x, 5, 0x1A1410);
                    px(img, x, 6, 0x2A2018);
                }
            }
            return img;
        }

        /** Tufted oxblood leather seat: brass tacks round the edge, buttoned dimples with creases fanning out. */
        /**
         * The hanging Bounty Board (18), 64x48: a 3x3 grid of 16x16 front tiles (corners, edges, middle) of cork in a dark
         * wooden frame with nails at the corners, picked per block by where it sits on the board; and a plank tile at
         * 48..64 x 0..16 for the back and edges (the rest is planks too).
         */
        static BufferedImage hangingBoard() {
            BufferedImage img = new BufferedImage(64, 48, BufferedImage.TYPE_INT_ARGB);
            int cork = 0xA9824F;
            Random r = new Random(26);
            BufferedImage wood = planks(DARK_WOOD, true, 27, true);
            for (int x = 0; x < 64; x++) for (int y = 0; y < 48; y++) img.setRGB(x, y, wood.getRGB(x % 16, y % 16));
            for (int tx = 0; tx < 3; tx++) for (int ty = 0; ty < 3; ty++) {
                for (int x = 0; x < 16; x++) for (int y = 0; y < 16; y++) {
                    int c = jitter(cork, r, 10);
                    if (r.nextInt(9) == 0) c = shade(cork, 0.72); // the cork's grain
                    boolean frame = tx == 0 && x < 2 || tx == 2 && x > 13 || ty == 0 && y < 2 || ty == 2 && y > 13;
                    boolean inner = tx == 0 && x == 2 || tx == 2 && x == 13 || ty == 0 && y == 2 || ty == 2 && y == 13;
                    if (frame) c = shade(DARK_WOOD, (tx == 0 && x == 0 || ty == 0 && y == 0) ? 1.25 : 1.0);
                    else if (inner) c = shade(cork, 0.6); // the cork's shadow under the frame
                    img.setRGB(tx * 16 + x, ty * 16 + y, 0xFF000000 | c);
                }
                if ((tx == 0 || tx == 2) && (ty == 0 || ty == 2)) { // a nail in each corner
                    int nx = tx * 16 + (tx == 0 ? 1 : 14), ny = ty * 16 + (ty == 0 ? 1 : 14);
                    img.setRGB(nx, ny, 0xFF000000 | IRON);
                }
            }
            return img;
        }

        /** Its item: a framed cork board with two posters pinned up. */
        static BufferedImage hangingBoardIcon() {
            BufferedImage img = blank();
            for (int x = 1; x < 15; x++) for (int y = 3; y < 13; y++) {
                boolean frame = x == 1 || x == 14 || y == 3 || y == 12;
                px(img, x, y, frame ? DARK_WOOD : jitter(0xA9824F, new Random(x * 31 + y), 8));
            }
            for (int[] p : new int[][] {{3, 5}, {9, 5}}) {
                for (int x = 0; x < 4; x++) for (int y = 0; y < 6; y++) px(img, p[0] + x, p[1] + y, y == 0 ? 0x7A1E12 : 0xE6D6AC);
                px(img, p[0] + 1, p[1] + 2, 0x2B1D0E);
                px(img, p[0] + 2, p[1] + 2, 0x2B1D0E);
                px(img, p[0] + 1, p[1] + 4, 0x2E4A1A);
                px(img, p[0] + 2, p[1] + 4, 0x2E4A1A);
            }
            return img;
        }

        /** Card table top: green baize with a faint weave, a polished wooden rim, and a gold pinstripe inside it. */
        static BufferedImage felt() {
            int baize = 0x1F5A34;
            BufferedImage img = noise(baize, 4, 25);
            for (int x = 1; x < 15; x++) for (int y = 1; y < 15; y++) if ((x + y) % 4 == 0) px(img, x, y, shade(baize, 0.9));
            for (int i = 0; i < 16; i++) {
                int rim = shade(DARK_WOOD, i % 5 == 0 ? 0.8 : 1.1);
                px(img, i, 0, rim); px(img, i, 15, rim); px(img, 0, i, rim); px(img, 15, i, rim);
            }
            for (int i = 2; i < 14; i++) {
                px(img, i, 2, 0xB08D57); px(img, i, 13, 0xB08D57); px(img, 2, i, 0xB08D57); px(img, 13, i, 0xB08D57);
            }
            return img;
        }

        static BufferedImage cushion() {
            int leather = 0x7E1C1C;
            BufferedImage img = noise(leather, 5, 16);
            for (int i = 0; i < 16; i++) {
                int edge = shade(leather, 0.62);
                px(img, i, 0, edge); px(img, i, 15, edge); px(img, 0, i, edge); px(img, 15, i, edge);
                if (i > 0 && i < 15) px(img, i, 1, shade(leather, 1.18)); // top-lit roll
            }
            for (int i = 1; i < 15; i += 3) { // tacks
                px(img, i, 0, BRASS); px(img, i, 15, shade(BRASS, 0.8)); px(img, 0, i, BRASS); px(img, 15, i, shade(BRASS, 0.8));
            }
            for (int[] b : new int[][] {{4, 4}, {11, 4}, {4, 11}, {11, 11}, {8, 8}}) {
                for (int d = 1; d < 3; d++) {
                    px(img, b[0] - d, b[1] - d, shade(leather, 0.82));
                    px(img, b[0] + d, b[1] + d, shade(leather, 0.82));
                }
                px(img, b[0], b[1], shade(leather, 0.45));
                px(img, b[0] - 1, b[1], shade(leather, 0.7));
                px(img, b[0] + 1, b[1] - 1, shade(leather, 1.25));
            }
            return img;
        }

        static BufferedImage counterSide() {
            BufferedImage img = planks(0x5A3418, false, 15, false);
            int frame = 0x3A2010;
            for (int i = 0; i < 16; i++) {
                px(img, i, 0, frame); px(img, i, 1, frame);
                px(img, i, 11, frame); px(img, 0, i, frame); px(img, 15, i, frame);
            }
            for (int x = 3; x < 13; x++) for (int y = 3; y < 9; y++) if (x == 3 || x == 12 || y == 3 || y == 8) px(img, x, y, shade(0x5A3418, 1.2));
            for (int x = 0; x < 16; x++) { px(img, x, 13, tone(BRASS, 5)); px(img, x, 14, tone(BRASS, 3)); } // foot rail
            px(img, 2, 12, tone(BRASS, 3)); px(img, 13, 12, tone(BRASS, 3));
            return img;
        }

        static BufferedImage doorHalf(boolean top) {
            BufferedImage img = blank();
            Random r = new Random(top ? 16 : 17);
            int frame = 0x4A2C16;
            for (int x = 0; x < 16; x++)
                for (int y = 0; y < 16; y++) {
                    boolean edge = x < 2 || x > 13 || (top ? y < 2 : y > 13);
                    int c = edge ? frame : (y % 3 == 0 ? shade(WOOD, 0.7) : jitter(shade(WOOD, y % 3 == 1 ? 1.15 : 1.0), r, 4));
                    px(img, x, y, c);
                }
            if (!top) { px(img, 12, 1, tone(BRASS, 6)); px(img, 12, 2, tone(BRASS, 3)); } // hinge plate
            return img;
        }

        static BufferedImage boilerSide(boolean lit) {
            BufferedImage img = noise(COPPER, 6, lit ? 18 : 19);
            for (int x = 0; x < 16; x++) {
                px(img, x, 0, tone(COPPER, 2));
                px(img, x, 5, tone(COPPER, 2));
                px(img, x, 6, tone(COPPER, 5));
                if (x % 3 == 1) { px(img, x, 4, tone(COPPER, 6)); px(img, x, 7, tone(COPPER, 6)); } // rivet rows
            }
            for (int y = 0; y < 5; y++) px(img, 8, y, tone(COPPER, 2));
            // Firebox door: iron frame with grate slits (glowing when lit)
            for (int x = 3; x < 13; x++)
                for (int y = 9; y < 15; y++) {
                    boolean frame = x == 3 || x == 12 || y == 9 || y == 14;
                    int c = frame ? DARK_IRON : (x % 2 == 0 ? (lit ? (y < 12 ? 0xFFC040 : 0xE06010) : 0x161616) : 0x2C2E32);
                    px(img, x, y, c);
                }
            px(img, 11, 11, tone(BRASS, 6)); // latch
            for (int x = 0; x < 16; x++) px(img, x, 15, BRICK);
            return img;
        }

        static BufferedImage boilerTop() {
            BufferedImage img = noise(COPPER, 6, 20);
            for (int i = 0; i < 16; i++) { px(img, i, 0, tone(COPPER, 2)); px(img, 0, i, tone(COPPER, 2)); px(img, i, 15, tone(COPPER, 2)); px(img, 15, i, tone(COPPER, 2)); }
            for (int x = 0; x < 16; x++)
                for (int y = 0; y < 16; y++) {
                    double d = Math.hypot(x + 0.5 - 8, y + 0.5 - 8);
                    if (d < 4.5 && d >= 3.5) px(img, x, y, tone(BRASS, 5));
                    else if (d < 3.5) px(img, x, y, 0x1A1A1A); // chimney flue
                }
            return img;
        }

        static BufferedImage bricks() {
            BufferedImage img = blank();
            Random r = new Random(21);
            for (int y = 0; y < 16; y++)
                for (int x = 0; x < 16; x++) {
                    boolean mortar = y % 4 == 3 || (x + (y / 4 % 2) * 4) % 8 == 7;
                    px(img, x, y, mortar ? 0x9A9086 : jitter(BRICK, r, 8));
                }
            return img;
        }

        static BufferedImage pipe() {
            BufferedImage img = noise(COPPER, 5, 22);
            for (int x = 0; x < 16; x++) {
                px(img, x, 5, tone(COPPER, 3));
                px(img, x, 6, tone(COPPER, 6));
                px(img, x, 7, tone(COPPER, 5));
                px(img, x, 10, tone(COPPER, 3));
            }
            for (int y = 0; y < 16; y++) { px(img, 7, y, tone(COPPER, 2)); px(img, 8, y, tone(BRASS, 4)); } // flange
            return img;
        }

        /** The tank's side: riveted brass bands top and bottom, seamed edges, and a framed sight glass with the water line in it. */
        static BufferedImage tankSide() {
            BufferedImage img = noise(tone(BRASS, 4), 3, 23);
            for (int x = 0; x < 16; x++) {
                px(img, x, 0, tone(BRASS, 2)); px(img, x, 1, tone(BRASS, 6)); px(img, x, 2, tone(BRASS, 5)); px(img, x, 3, tone(BRASS, 3));
                px(img, x, 12, tone(BRASS, 6)); px(img, x, 13, tone(BRASS, 5)); px(img, x, 14, tone(BRASS, 3)); px(img, x, 15, tone(BRASS, 2));
                if (x % 4 == 2) { px(img, x, 2, tone(BRASS, 2)); px(img, x, 13, tone(BRASS, 2)); px(img, x - 1, 1, 0xFFF4C8); px(img, x - 1, 12, 0xFFF4C8); }
            }
            for (int y = 4; y < 12; y++) { px(img, 0, y, tone(BRASS, 6)); px(img, 15, y, tone(BRASS, 3)); }
            for (int y = 4; y < 12; y++) { // the sight glass in a dark iron frame, steam above the water line
                px(img, 6, y, DARK_IRON); px(img, 9, y, DARK_IRON);
                boolean water = y >= 8;
                px(img, 7, y, y == 4 || y == 5 ? 0xF4FAFF : water ? 0x5A8AB0 : 0xC8D8E2);
                px(img, 8, y, water ? (y == 8 ? 0x8ABCDC : 0x3E6A90) : 0xA8BCC8);
            }
            for (int x = 6; x <= 9; x++) { px(img, x, 4, DARK_IRON); px(img, x, 11, DARK_IRON); }
            return img;
        }

        /** The tank's top: a riveted brass lid, a round hatch bevelled into it, and a red handwheel with four spokes on the hatch. */
        static BufferedImage hatch(int base, long seed) {
            BufferedImage img = noise(tone(base, 4), 3, seed);
            for (int i = 0; i < 16; i++) {
                px(img, i, 0, tone(base, 6)); px(img, 0, i, tone(base, 6)); px(img, i, 15, tone(base, 2)); px(img, 15, i, tone(base, 2));
                if (i % 4 == 2) for (int[] p : new int[][] {{i, 1}, {1, i}, {i, 14}, {14, i}}) px(img, p[0], p[1], tone(base, 2));
            }
            int red = 0xC03030;
            for (int x = 0; x < 16; x++)
                for (int y = 0; y < 16; y++) {
                    double dx = x + 0.5 - 8, dy = y + 0.5 - 8, d = Math.hypot(dx, dy);
                    if (d < 6.4 && d >= 5.6) px(img, x, y, tone(base, 2));
                    else if (d < 5.6 && d >= 4.8) px(img, x, y, tone(base, dx + dy < 0 ? 6 : 3));
                    else if (d < 3.9 && d >= 2.9) px(img, x, y, dx + dy < 0 ? shade(red, 1.25) : shade(red, 0.75));
                    else if (d < 2.9 && (x == 7 || x == 8 || y == 7 || y == 8)) px(img, x, y, shade(red, 0.9));
                }
            for (int x = 7; x <= 8; x++) for (int y = 7; y <= 8; y++) px(img, x, y, DARK_IRON);
            return img;
        }

        // Handcar engine: riveted black iron (firebox, stack), a banded brass boiler drum, and the firebox door.
        static BufferedImage engineIron() {
            BufferedImage img = noise(DARK_IRON, 5, 40);
            for (int i = 0; i < 16; i++) {
                if (i % 4 == 1) { px(img, i, 1, tone(IRON, 5)); px(img, i, 14, tone(IRON, 5)); px(img, 1, i, tone(IRON, 5)); px(img, 14, i, tone(IRON, 5)); }
                px(img, i, 0, shade(DARK_IRON, 0.7)); px(img, i, 15, shade(DARK_IRON, 0.7));
            }
            return img;
        }

        static BufferedImage engineBrass() {
            BufferedImage img = noise(tone(BRASS, 4), 5, 41);
            for (int y = 0; y < 16; y++) {
                px(img, 0, y, tone(BRASS, 2)); px(img, 1, y, tone(BRASS, 6)); px(img, 15, y, tone(BRASS, 2));
            }
            for (int x = 0; x < 16; x++) {
                for (int y : new int[] {2, 8, 13}) { px(img, x, y, tone(BRASS, 2)); px(img, x, y + 1, tone(BRASS, 6)); } // bands
                if (x % 3 == 1) px(img, x, 9, tone(BRASS, 6));
            }
            return img;
        }

        static BufferedImage engineFirebox(boolean lit) {
            BufferedImage img = engineIron();
            for (int x = 3; x < 13; x++)
                for (int y = 4; y < 13; y++) {
                    boolean frame = x == 3 || x == 12 || y == 4 || y == 12;
                    int c = frame ? tone(IRON, 3) : (x % 2 == 0 ? (lit ? (y < 8 ? 0xFFC040 : 0xE06010) : 0x161616) : 0x2C2E32);
                    px(img, x, y, c);
                }
            px(img, 11, 8, tone(BRASS, 6)); // latch
            return img;
        }

        static BufferedImage crusherSide() {
            BufferedImage img = metalBlock(IRON, 24);
            Items.gearOnto(img, 8, 8, 5.2, 3.9, 1.6, BRASS);
            return img;
        }

        static BufferedImage crusherTop() {
            BufferedImage img = metalBlock(IRON, 25);
            for (int x = 2; x < 14; x++)
                for (int y = 4; y < 12; y++) {
                    boolean tooth = (x + (y < 8 ? 0 : 1)) % 2 == 0;
                    px(img, x, y, y == 7 || y == 8 ? 0x101010 : tooth ? 0xC8CCD2 : 0x5A5E64); // two toothed rollers
                }
            return img;
        }

        static BufferedImage gauge() {
            BufferedImage img = metalBlock(BRASS, 26);
            for (int x = 0; x < 16; x++)
                for (int y = 0; y < 16; y++) {
                    double d = Math.hypot(x + 0.5 - 8, y + 0.5 - 8);
                    if (d < 6 && d >= 5) px(img, x, y, tone(BRASS, 2));
                    else if (d < 5) px(img, x, y, 0xEDE4CC);
                }
            for (int[] t : new int[][] {{4, 8}, {5, 5}, {8, 4}, {11, 5}, {12, 8}}) px(img, t[0], t[1], 0x2A2A2A); // ticks
            px(img, 8, 8, 0x2A2A2A);
            px(img, 9, 7, 0xC02020); px(img, 10, 6, 0xC02020); // needle
            return img;
        }

        static BufferedImage bountyBoard() {
            BufferedImage img = planks(0x7A5030, false, 27, false);
            int paper = 0xE8D8A8, ink = 0x3A2A1A;
            int[][] notes = {{1, 2, 6, 7}, {8, 1, 6, 6}, {3, 10, 7, 5}, {11, 8, 4, 5}};
            for (int[] n : notes) {
                for (int x = n[0]; x < n[0] + n[2]; x++)
                    for (int y = n[1]; y < n[1] + n[3]; y++) px(img, x, y, (y - n[1]) % 2 == 1 && x > n[0] && x < n[0] + n[2] - 1 ? ink : paper);
                px(img, n[0] + n[2] / 2, n[1], 0xC02020); // pin
            }
            for (int x = 2; x < 6; x++) px(img, x, 3, 0x6A1414); // "WANTED" band
            return img;
        }
    }

    // ---------------------------------------------------------------- starsteel and phlogiston (9c)

    /**
     * The Hollow Earth's metal and crystal (9c). Starsteel is crimson with pale-gold edges, so it reads apart from copper's
     * salmon and gold's yellow, and every piece of its gear carries one ember phlogiston stone. The gear sprites are drawn as
     * vanilla's are: a near-black outline round everything, two-pixel handles, big heads on the diagonal, a five-tone
     * shade. Ores (crimson flecks with a gold glint), storage blocks, the Phlogiston Lamp; the materials; the sword, paxel, armor
     * icons and horseshoes; and the worn armor layers (vanilla humanoid UV).
     */
    static final class Starsteel {
        static final int OUTLINE = 0x2B0710, DARK = 0x6A0F1E, MID = 0x9A1A2B, LIGHT = 0xC42B3A, GOLD = 0xE6A93A, PALE_GOLD = 0xFFE39A;
        static final int PHLOGISTON = 0xFF9A3A, PHLOGISTON_HI = 0xFFEAC0;
        static final java.util.Map<java.lang.Character, Integer> PAL = pal('o', OUTLINE, 'c', DARK, 'C', MID, 'r', LIGHT, 'g', GOLD,
                'G', PALE_GOLD, 'v', PHLOGISTON, 'V', PHLOGISTON_HI, 'w', 0x9C6E40, 'W', 0x6E4A28, 'd', 0x3A2414);

        static final String[] SWORD = {
                ".............ooo",
                "............oGGo",
                "...........oGrco",
                "..........oGrco.",
                ".........oGrco..",
                "........oGrco...",
                ".......oGrco....",
                "..oo..oGrco.....",
                "..oGooGrco......",
                "...ogGrco.......",
                "....ogvVoo......",
                "...owoogGo......",
                "..owWo.oGo......",
                ".owWo...oo......",
                "oGgo............",
                "ooo............."};
        static final String[] PAXEL = {
                "...oooo.........",
                "..oGGGGoooooo...",
                ".oGrrrrGGGGGGo..",
                ".oGrrCCrrrrrrGo.",
                ".ocrrcoooovVrrGo",
                "..occo..owvorcGo",
                "...oo..owo..ocro",
                "......owWo..ocro",
                ".....owWo....oco",
                "....owWo.....oco",
                "...owWo.......oo",
                "..owWo..........",
                ".owWo...........",
                "owWo............",
                "oWo.............",
                "oo.............."};
        static final String[] HELMET = {
                "", "",
                "....oooooooo....",
                "...oGGGGGGGGo...",
                "..oGGrrvVrrrro..",
                "..oGrrrrrrrrco..",
                "..oGrcccccccco..",
                "..oGco....oCco..",
                "..oGco....oCco..",
                "..oco......oco..",
                "..ooo......ooo.."};
        static final String[] CHESTPLATE = {
                "..ooo......ooo..",
                ".oGGGo....oGGco.",
                ".oGrrGoooooGrrco",
                ".oGrrrGGGGGrrrco",
                ".occoGrrvVrrcoco",
                "..ooGrrrvvrrrco.",
                "...oGrrrrrrrrco.",
                "...oGrCrrrrCrco.",
                "...oGrrrrrrrrco.",
                "...oGrCCCCCCrco.",
                "...ocCCCCCCCCco.",
                "...ooooooooooo.."};
        static final String[] LEGGINGS = {
                "",
                "...oooooooooo...",
                "...oGGGgvgGGGo..",
                "...oGrrrrrrrco..",
                "...oGrcoooGrco..",
                "...oGco..oGrco..",
                "...oGco..oGrco..",
                "...oGco..oGrco..",
                "...oGco..oGrco..",
                "...oGco..oGrco..",
                "...occo..occco..",
                "...oooo..ooooo.."};
        static final String[] BOOTS = {
                "", "", "", "", "",
                "...oooo...oooo..",
                "...oGvo...oGvo..",
                "...oGro...oGro..",
                "...oGco...oGco..",
                "...oGco...oGco..",
                "..ooGco..ooGco..",
                ".oGGrco.oGGrco..",
                ".ocCCco.ocCCco..",
                ".oooooo.oooooo.."};
        static final String[] SHARD = {
                "",
                ".......oo.......",
                "......oVvo......",
                "......oVvo......",
                ".....oVvvvo.....",
                ".....oVvvvo.....",
                "....oVvvVvvo....",
                "....oVvvvVvo....",
                ".....oVvvvo.....",
                ".....ovvvvo.....",
                "......ovvo......",
                "......ovvo......",
                ".......oo......."};

        /** Recolours one of the frontier's ramp sprites (tones 1-6) into starsteel: outline, crimson, then gold on top. */
        static String[] crimson(String[] rows) {
            String[] out = new String[rows.length];
            for (int i = 0; i < rows.length; i++) {
                out[i] = rows[i].replace('1', 'o').replace('2', 'c').replace('3', 'C').replace('4', 'r').replace('5', 'g').replace('6', 'G');
            }
            return out;
        }

        static BufferedImage gear(String[] rows) {
            return sprite(LIGHT, PAL, rows);
        }

        static int writeAll(File root) throws Exception {
            int n = 0;
            n += Tiles.put(root, "starsteel_ore", ore(Tiles.stone(91), 92));
            n += Tiles.put(root, "deepslate_starsteel_ore", ore(Tiles.deepslate(93), 94));
            n += Tiles.put(root, "raw_starsteel_block", Tiles.rawBlock(MID, 95));
            n += Tiles.put(root, "starsteel_block", block());
            n += Tiles.put(root, "phlogiston_ore", Tiles.ore(Tiles.stone(97), PHLOGISTON, 98));
            n += Tiles.put(root, "deepslate_phlogiston_ore", Tiles.ore(Tiles.deepslate(99), PHLOGISTON, 100));
            n += Tiles.put(root, "phlogiston_lamp", lamp());
            n += Items.put(root, "raw_starsteel", gear(crimson(Items.RAW)));
            n += Items.put(root, "starsteel_ingot", gear(crimson(Items.INGOT)));
            n += Items.put(root, "starsteel_nugget", gear(crimson(Items.NUGGET)));
            n += Items.put(root, "starsteel_dust", gear(crimson(Items.DUST)));
            n += Items.put(root, "starsteel_horseshoes", gear(crimson(Items.HORSESHOE)));
            n += Items.put(root, "phlogiston_shard", gear(SHARD));
            n += Items.put(root, "starsteel_sword", gear(SWORD));
            n += Items.put(root, "starsteel_paxel", gear(PAXEL));
            n += Items.put(root, "starsteel_helmet", gear(HELMET));
            n += Items.put(root, "starsteel_chestplate", gear(CHESTPLATE));
            n += Items.put(root, "starsteel_leggings", gear(LEGGINGS));
            n += Items.put(root, "starsteel_boots", gear(BOOTS));
            BufferedImage armor = armor(false);
            write(new File(root, "entity/equipment/humanoid/starsteel.png"), armor);
            write(new File(root, "entity/equipment/humanoid_baby/starsteel.png"), armor);
            write(new File(root, "entity/equipment/humanoid_leggings/starsteel.png"), armor(true));
            return n + 3;
        }

        /** Starsteel ore: crimson flecks in dark rims, each with a pale-gold glint (nothing like redstone's even red). */
        static BufferedImage ore(BufferedImage base, long seed) {
            Random r = new Random(seed);
            int[][] centers = {{3, 3}, {11, 4}, {5, 11}, {12, 12}};
            for (int[] c : centers) {
                int cx = c[0] + r.nextInt(2), cy = c[1] + r.nextInt(2);
                for (int[] o : new int[][] {{-1, 0}, {0, -1}, {2, 0}, {2, 1}, {0, 2}, {1, 2}, {-1, 1}, {1, -1}}) {
                    Tiles.px(base, cx + o[0], cy + o[1], OUTLINE);
                }
                Tiles.px(base, cx, cy, PALE_GOLD);
                Tiles.px(base, cx + 1, cy, LIGHT);
                Tiles.px(base, cx, cy + 1, MID);
                Tiles.px(base, cx + 1, cy + 1, DARK);
            }
            return base;
        }

        /** Block of Starsteel: a crimson panel framed in gold, a phlogiston stone at its centre. */
        static BufferedImage block() {
            BufferedImage img = Tiles.noise(MID, 4, 96);
            for (int i = 0; i < 16; i++) {
                for (int j = 0; j < 16; j++) {
                    boolean edge = i == 0 || j == 0 || i == 15 || j == 15, inner = i == 1 || j == 1 || i == 14 || j == 14;
                    if (edge) {
                        Tiles.px(img, i, j, OUTLINE);
                    } else if (inner) {
                        Tiles.px(img, i, j, i == 1 || j == 1 ? PALE_GOLD : GOLD);
                    } else if (i + j < 12) {
                        Tiles.px(img, i, j, LIGHT);
                    }
                }
            }
            for (int[] p : new int[][] {{7, 7}, {8, 7}, {7, 8}, {8, 8}}) {
                Tiles.px(img, p[0], p[1], p[0] == 7 && p[1] == 7 ? PHLOGISTON_HI : PHLOGISTON);
            }
            return img;
        }

        /** The Phlogiston Lamp: a brass frame round glass, a phlogiston crystal glowing at its heart. */
        static BufferedImage lamp() {
            BufferedImage img = Tiles.noise(shade(PHLOGISTON, 0.55), 3, 101);
            for (int x = 0; x < 16; x++) {
                for (int y = 0; y < 16; y++) {
                    double d = Math.hypot(x - 7.5, y - 7.5);
                    if (d < 5.5) {
                        Tiles.px(img, x, y, d < 2.2 ? 0xFFF6E0 : d < 3.6 ? PHLOGISTON : shade(PHLOGISTON, 0.8));
                    }
                    if (x <= 1 || y <= 1 || x >= 14 || y >= 14) {
                        Tiles.px(img, x, y, tone(Items.BRASS, x == 0 || y == 0 ? 6 : x == 15 || y == 15 ? 2 : 4));
                    }
                }
            }
            return img;
        }

        /** Worn armor: crimson plates edged in gold, a visor slit, phlogiston stones in the helm's brow and the breastplate. */
        static BufferedImage armor(boolean leggings) {
            BufferedImage img = new BufferedImage(64, 32, BufferedImage.TYPE_INT_ARGB);
            Painter p = new Painter(img, new Random(leggings ? 102 : 103));
            if (leggings) {
                Box body = new Box(16, 16, 8, 12, 4);
                for (Rect side : new Rect[] {body.front(), body.back(), body.east(), body.west()}) {
                    plate(p, new Rect(side.x(), side.y() + 7, side.w(), 5));
                }
                p.set(body.front().x() + 3, body.front().y() + 7, PHLOGISTON);
                p.set(body.front().x() + 4, body.front().y() + 7, PHLOGISTON);
                Box leg = new Box(0, 16, 4, 12, 4);
                for (Rect side : new Rect[] {leg.front(), leg.back(), leg.east(), leg.west()}) {
                    plate(p, new Rect(side.x(), side.y(), side.w(), 9));
                }
                return img;
            }
            Box head = new Box(0, 0, 8, 8, 8);
            plate(p, head.top());
            for (Rect side : new Rect[] {head.front(), head.back(), head.east(), head.west()}) {
                plate(p, side);
            }
            Rect face = head.front();
            p.face(new Rect(face.x() + 1, face.y() + 3, face.w() - 2, 2), OUTLINE, 0); // the visor slit
            p.set(face.x() + 3, face.y() + 1, PHLOGISTON);
            p.set(face.x() + 4, face.y() + 1, PHLOGISTON_HI);
            Box body = new Box(16, 16, 8, 12, 4);
            for (Rect side : new Rect[] {body.front(), body.back(), body.east(), body.west()}) {
                plate(p, side);
            }
            Rect chest = body.front();
            p.row(chest, 5, GOLD); // a gold band under the breast
            for (int dy = 0; dy < 2; dy++) {
                p.set(chest.x() + 3, chest.y() + 2 + dy, PHLOGISTON);
                p.set(chest.x() + 4, chest.y() + 2 + dy, dy == 0 ? PHLOGISTON_HI : PHLOGISTON);
            }
            Box arm = new Box(40, 16, 4, 12, 4);
            plate(p, arm.top());
            for (Rect side : new Rect[] {arm.front(), arm.back(), arm.east(), arm.west()}) {
                plate(p, new Rect(side.x(), side.y(), side.w(), 5));
            }
            Box leg = new Box(0, 16, 4, 12, 4);
            for (Rect side : new Rect[] {leg.front(), leg.back(), leg.east(), leg.west()}) {
                plate(p, new Rect(side.x(), side.y() + 8, side.w(), 4));
            }
            return img;
        }

        private static void plate(Painter p, Rect r) {
            p.face(r, MID, 6);
            p.row(r, 0, PALE_GOLD);
            if (r.h() > 2) {
                p.row(r, 1, GOLD);
            }
            p.row(r, r.h() - 1, DARK);
        }
    }

    // ---------------------------------------------------------------- reworked from vanilla

    /**
     * The mod's metals drawn after vanilla's own textures (CLAUDE.md allows reworked vanilla art; ASSETS.md marks each with
     * its source), read straight from the patched game jar the build makes (build/moddev/artifacts). Each overwrites the
     * hand-drawn version written earlier; without the jar (no build yet) the hand-drawn ones stay.
     * <ul>
     * <li><b>Starsteel</b> (9c), from iron: shading mapped onto a crimson ramp, a gold rim along each icon's top-left
     *     edges, gold glints on the ore flecks and the raw ore, phlogiston stones on the armor, the block, the sword's guard and
     *     the paxel's head; tools take gold on the blade's highlight instead of a rim (their blades are too thin for one). The
     *     paxel is vanilla's iron axe with the pickaxe's spike worked into the gaps round its head.</li>
     * <li><b>Phlogiston</b> ore, from emerald (its gem-shaped flecks, so it can't pass for diamond), ember.</li>
     * <li><b>Brass</b>, from gold: deeper and browner than gold, with rivets on the block.</li>
     * <li>The crusher's <b>dusts</b>, from redstone dust, in each metal's colour.</li>
     * </ul>
     * Handles keep vanilla's wood: only grey (metal) pixels are recoloured on tools.
     */
    // ---------------------------------------------------------------- Concavia (12a)

    /** The Kindled's skin and the crystal guardian's, the sigil and the two spawn eggs (the crystal blocks are in Reworked). */
    static final class Concavia {
        static final int PHLOGISTON = 0xF58A2C, PHLOGISTON_HI = 0xFFE6B0, GOLD = 0xD8C070, GOLD_DK = 0x9A8040;
        /** The cold crystal light that isn't phlogiston: the guardians' eyes, chiselled crystal, and the saucer's Gray nav ball. */
        static final int GLOW = 0x7FE8FF, GLOW_HI = 0xE0FCFF;
        static final int CRYSTAL = 0x9FD8EC, CRYSTAL_DK = 0x4E8CA8, CRYSTAL_HI = 0xDAF4FC;
        static final int STONE = 0xD6DCDE, STONE_DK = 0x9AA4AA;

        static int writeAll(File root) throws Exception {
            BufferedImage[] kindled = kindled();
            write(new File(root, "entity/kindled.png"), kindled[0]);
            write(new File(root, "entity/kindled_glowmask.png"), kindled[1]);
            BufferedImage[] guardian = guardian();
            write(new File(root, "entity/crystal_guardian.png"), guardian[0]);
            write(new File(root, "entity/crystal_guardian_glowmask.png"), guardian[1]);
            int n = 4;
            n += Items.put(root, "kindled_sigil", sigil());
            n += Tiles.put(root, "waystone_side", waystoneSide()); // 15c
            n += Tiles.put(root, "waystone_top", waystoneTop());
            n += Items.put(root, "kindled_spawn_egg", Items.egg(0xE8F0F6, pal('a', 0xF2EAC8, 'A', 0xD8C88A, 'g', GOLD, 'v', PHLOGISTON, 'f', 0xF4E8DE, 't', 0xA8CCE0),
                    "", "",
                    "      aAAa",
                    "     aAAAAa",
                    "    aggvvgga",
                    "    afvffvfa",
                    "    affffffa",
                    "    aaffffaa",
                    "    a      a",
                    "    a      a",
                    "    tttttttt",
                    "       vv"));
            n += Items.put(root, "crystal_guardian_spawn_egg", Items.egg(CRYSTAL, pal('d', CRYSTAL_DK, 'h', CRYSTAL_HI, 'v', PHLOGISTON_HI, 'V', PHLOGISTON),
                    "", "", "",
                    "      hh",
                    "     hh",
                    "",
                    "     vV  Vv",
                    "",
                    "    dd    dd",
                    "      dddd",
                    "",
                    "     h    h",
                    "      dddd"));
            // The Sun Archon (12b): its skin, the crown it wears and drops (worn and as an icon), the lens, the egg
            BufferedImage[] archon = archon();
            write(new File(root, "entity/sun_archon.png"), archon[0]);
            write(new File(root, "entity/sun_archon_glowmask.png"), archon[1]);
            write(new File(root, "entity/equipment/humanoid/starsteel_crown.png"), crownLayer());
            n += 3;
            n += Items.put(root, "starsteel_crown", crownIcon());
            n += Tiles.put(root, "sun_pylon", pylon());
            n += Items.put(root, "sunstone_lens", lens());
            n += Items.put(root, "sun_archon_spawn_egg", Items.egg(0x8A1E1E, pal('y', 0xFFC040, 'Y', 0xFFE070, 'W', 0xFFFBE0),
                    "", "", "",
                    "       yy",
                    "     y    y",
                    "      yyyy",
                    "     yYYYYy",
                    "    yyYWWYyy",
                    "     yYYYYy",
                    "      yyyy",
                    "     y    y",
                    "       yy"));
            return n;
        }

        /**
         * The humanoid skin in the Kindled colours, with the hat cleared away but for a gold circlet (the crown box's
         * bottom row) set with a phlogiston stone over the brow; long hair down the back to the shoulders. The eyes and the
         * stone glow.
         */
        static BufferedImage[] kindled() {
            BufferedImage img = skin(Character.KINDLED, 1912);
            BufferedImage glow = new BufferedImage(SKIN_W, SKIN_H, BufferedImage.TYPE_INT_ARGB);
            Box crown = new Box(32, 0, 8, 3, 8), brim = new Box(0, 32, 8, 1, 12), curl = new Box(0, 76, 2, 1, 12);
            for (Box hat : new Box[] {crown, brim, curl}) {
                for (int x = hat.u(); x < hat.u() + 2 * (hat.w() + hat.d()); x++) {
                    for (int y = hat.v(); y < hat.v() + hat.d() + hat.h(); y++) {
                        img.setRGB(x, y, CLEAR);
                    }
                }
            }
            for (Rect side : new Rect[] {crown.front(), crown.back(), crown.east(), crown.west()}) {
                for (int x = 0; x < side.w(); x++) {
                    img.setRGB(side.x() + x, side.y() + side.h() - 1, 0xFF000000 | (x % 2 == 0 ? GOLD : GOLD_DK));
                }
            }
            Rect brow = crown.front();
            for (int x : new int[] {3, 4}) {
                img.setRGB(brow.x() + x, brow.y() + brow.h() - 1, 0xFF000000 | PHLOGISTON);
                glow.setRGB(brow.x() + x, brow.y() + brow.h() - 1, 0xFF000000 | PHLOGISTON);
            }
            // The eyes: the irises glow, and the whites are faintly blue
            Rect face = new Box(0, 0, 8, 8, 8).front();
            for (int x : new int[] {2, 5}) {
                img.setRGB(face.x() + x, face.y() + 3, 0xFF000000 | Character.KINDLED.eye());
                glow.setRGB(face.x() + x, face.y() + 3, 0xFF000000 | Character.KINDLED.eye());
            }
            for (int x : new int[] {1, 6}) img.setRGB(face.x() + x, face.y() + 3, 0xFFE8F4F8);
            // Hair down the back of the robe to the shoulders
            Rect back = new Box(16, 16, 8, 12, 4).back();
            Random rnd = new Random(7);
            for (int y = 0; y < 4; y++) {
                for (int x = 1; x < 7; x++) {
                    if (y < 3 || rnd.nextBoolean()) img.setRGB(back.x() + x, back.y() + y, 0xFF000000 | shade(Character.KINDLED.hair(), 0.94 + rnd.nextInt(3) * 0.03));
                }
            }
            // A pale blue hem round the robe's skirts, where the legs' boots begin
            for (Box leg : new Box[] {new Box(0, 16, 4, 12, 4), new Box(16, 48, 4, 12, 4)}) {
                for (Rect side : new Rect[] {leg.front(), leg.back(), leg.east(), leg.west()}) {
                    for (int x = 0; x < side.w(); x++) img.setRGB(side.x() + x, side.y() + 7, 0xFF000000 | Character.KINDLED.lapel());
                }
            }
            return new BufferedImage[] {img, glow};
        }

        /**
         * The guardian (crystal_guardian.geo.json's box UVs): a frosted crystal-brick torso with a phlogiston core in its chest,
         * a crystal head with a slot of an eye and a spike of crystal on top, two crystal shards floating at its shoulders,
         * a chiseled band at the hips and legs of pale stone. The core, the eye, the spike and the shards' edges glow.
         */
        static BufferedImage[] guardian() {
            BufferedImage img = new BufferedImage(64, 64, BufferedImage.TYPE_INT_ARGB);
            BufferedImage glow = new BufferedImage(64, 64, BufferedImage.TYPE_INT_ARGB);
            Painter p = new Painter(img, new Random(1912));
            // Torso: frosted crystal brick, coursed
            Box torso = new Box(0, 0, 16, 14, 9);
            for (Rect side : faces(torso)) {
                p.face(side, 0xC4DCE8, 8);
                for (int y = 0; y < side.h(); y++) {
                    if (y % 4 == 3) p.row(side, y, 0x8EB0C4);
                    else for (int x = (y / 4 % 2 == 0 ? 2 : 0); x < side.w(); x += 5) p.set(side.x() + x, side.y() + y, 0x8EB0C4);
                }
            }
            Rect chest = torso.front();
            for (int x = 5; x <= 10; x++) for (int y = 4; y <= 9; y++) p.set(chest.x() + x, chest.y() + y, 0x5A7E92); // the core's setting
            // Core: phlogiston
            Box core = new Box(50, 0, 4, 4, 1);
            for (Rect side : faces(core)) fillGlow(img, glow, side, PHLOGISTON);
            Rect cf = core.front();
            for (int x = 1; x <= 2; x++) for (int y = 1; y <= 2; y++) {
                img.setRGB(cf.x() + x, cf.y() + y, 0xFF000000 | PHLOGISTON_HI);
                glow.setRGB(cf.x() + x, cf.y() + y, 0xFF000000 | PHLOGISTON_HI);
            }
            // Head: faceted crystal
            Box head = new Box(0, 23, 8, 8, 8);
            for (Rect side : faces(head)) crystal(p, side);
            // Eye: a bright slot
            Box eye = new Box(50, 6, 4, 2, 1);
            for (Rect side : faces(eye)) fillGlow(img, glow, side, GLOW_HI);
            // Crown spike and the floating shards
            for (Rect side : faces(new Box(48, 23, 2, 6, 2))) {
                crystal(p, side);
                edgeGlow(img, glow, side);
            }
            for (Rect side : faces(new Box(32, 23, 4, 10, 4))) {
                crystal(p, side);
                edgeGlow(img, glow, side);
            }
            // Hips: chiseled crystal brick with a gold band
            for (Rect side : faces(new Box(20, 39, 12, 4, 6))) {
                p.face(side, 0xD2E6EE, 6);
                p.row(side, 0, GOLD);
                p.row(side, side.h() - 1, 0x8EB0C4);
            }
            // Legs: pale stone, a joint at the knee and a darker foot
            for (Rect side : faces(new Box(0, 39, 5, 14, 5))) {
                p.face(side, STONE, 10);
                if (side.h() == 14) {
                    p.row(side, 6, STONE_DK);
                    for (int y = 11; y < 14; y++) p.row(side, y, shade(STONE, 0.82));
                }
            }
            return new BufferedImage[] {img, glow};
        }

        /**
         * The Sun Archon (sun_archon.geo.json's box UVs, 128x64): a gold mask of a face with burning eyes under white hair,
         * a starsteel crown set with phlogiston, a halo of sunlight, an ivory-and-gold tunic and a crimson robe both cracked
         * through with a dark, ember-lit corruption, gold-cuffed crimson sleeves, a starsteel staff with a sun orb, and an
         * eclipse-black cape edged in gold. The eyes, gems, halo, orb and the cracks' embers glow.
         */
        static BufferedImage[] archon() {
            BufferedImage img = new BufferedImage(128, 64, BufferedImage.TYPE_INT_ARGB);
            BufferedImage glow = new BufferedImage(128, 64, BufferedImage.TYPE_INT_ARGB);
            Painter p = new Painter(img, new Random(1208));
            int ivory = 0xE8D8A8, gold = 0xD8B040, goldDk = 0x8A6A20, crimson = 0x8A1E1E, crimsonDk = 0x5A1010;
            int sun = 0xFFE070, sunHi = 0xFFF8D0, ember = 0xFF8A30, crack = 0x2A1410, hair = 0xF4F0E8, cape = 0x2A1430;
            // Tunic
            Box body = new Box(0, 0, 12, 16, 7);
            for (Rect side : faces(body)) {
                p.face(side, ivory, 8);
                p.row(side, 0, gold);
                if (side.h() == 16) {
                    p.row(side, side.h() - 1, gold);
                    p.row(side, side.h() - 2, goldDk);
                }
            }
            Rect chest = body.front();
            for (int y = 2; y < 12; y++) p.set(chest.x() + 5 + (y % 3 == 0 ? 1 : 0), chest.y() + y, gold); // the sash
            sunburst(img, glow, chest.x() + 6, chest.y() + 5, sun); // the sun on the breast
            cracks(img, glow, body.front(), crack, ember, 91);
            cracks(img, glow, body.back(), crack, ember, 92);
            // Head: white hair, a gold mask with burning eyes
            Box head = new Box(38, 0, 8, 10, 8);
            for (Rect side : faces(head)) p.face(side, hair, 6);
            Rect face = head.front();
            p.face(new Rect(face.x(), face.y() + 2, face.w(), face.h() - 2), gold, 8);
            for (int x = 1; x < 7; x++) p.set(face.x() + x, face.y() + 9, hair); // the beard's edge
            p.row(face, 8, hair);
            for (int x : new int[] {2, 5}) {
                for (int y : new int[] {4, 5}) {
                    img.setRGB(face.x() + x, face.y() + y, 0xFF000000 | sunHi);
                    glow.setRGB(face.x() + x, face.y() + y, 0xFF000000 | sunHi);
                }
            }
            p.set(face.x() + 3, face.y() + 6, goldDk);
            p.set(face.x() + 4, face.y() + 6, goldDk);
            // Crown: starsteel, phlogiston set in it
            Box crown = new Box(70, 0, 9, 3, 9);
            for (Rect side : faces(crown)) {
                p.face(side, Reworked.CRIMSON[2], 6);
                p.row(side, 0, Reworked.CRIMSON[3]);
            }
            for (Rect side : new Rect[] {crown.front(), crown.back(), crown.east(), crown.west()}) {
                for (int x = 1; x < side.w(); x += 3) {
                    img.setRGB(side.x() + x, side.y() + 1, 0xFF000000 | PHLOGISTON);
                    glow.setRGB(side.x() + x, side.y() + 1, 0xFF000000 | PHLOGISTON);
                }
            }
            // The halo: sunlight, all of it aglow
            for (Box bar : new Box[] {new Box(70, 12, 14, 2, 1), new Box(100, 12, 2, 10, 1)}) {
                for (Rect side : faces(bar)) {
                    for (int x = 0; x < side.w(); x++) for (int y = 0; y < side.h(); y++) {
                        int rgb = (x + y) % 3 == 0 ? sunHi : sun;
                        img.setRGB(side.x() + x, side.y() + y, 0xFF000000 | rgb);
                        glow.setRGB(side.x() + x, side.y() + y, 0xFF000000 | rgb);
                    }
                }
            }
            // Robe: crimson, a gold hem, cracked through
            Box robe = new Box(0, 32, 12, 20, 8);
            for (Rect side : faces(robe)) {
                p.face(side, crimson, 10);
                if (side.h() == 20) {
                    p.row(side, side.h() - 1, gold);
                    p.row(side, side.h() - 2, goldDk);
                    for (int y = 0; y < side.h() - 2; y += 4) p.row(side, y, crimsonDk);
                }
            }
            cracks(img, glow, robe.front(), crack, ember, 93);
            cracks(img, glow, robe.east(), crack, ember, 94);
            cracks(img, glow, robe.west(), crack, ember, 95);
            // Sleeves, cuffs and hands
            Box arm = new Box(40, 32, 4, 15, 4);
            for (Rect side : faces(arm)) {
                p.face(side, crimson, 8);
                if (side.h() == 15) {
                    p.row(side, 0, gold);
                    p.face(new Rect(side.x(), side.y() + 11, side.w(), 1), gold, 0);
                    p.face(new Rect(side.x(), side.y() + 12, side.w(), 3), gold, 6);
                }
            }
            // The staff and its sun orb
            for (Rect side : faces(new Box(56, 32, 2, 30, 2))) {
                p.face(side, Reworked.CRIMSON[1], 6);
                for (int y = 0; y < side.h(); y += 5) p.row(side, y, gold);
            }
            for (Rect side : faces(new Box(64, 32, 4, 4, 4))) {
                for (int x = 0; x < side.w(); x++) for (int y = 0; y < side.h(); y++) {
                    int rgb = (x == 1 || x == 2) && (y == 1 || y == 2) ? sunHi : sun;
                    img.setRGB(side.x() + x, side.y() + y, 0xFF000000 | rgb);
                    glow.setRGB(side.x() + x, side.y() + y, 0xFF000000 | rgb);
                }
            }
            // The cape: eclipse-black, edged in gold
            for (Rect side : faces(new Box(80, 24, 12, 29, 1))) {
                p.face(side, cape, 8);
                p.outline(side, gold);
            }
            return new BufferedImage[] {img, glow};
        }

        /** A little sun: a bright centre and four rays, in the image and its glow. */
        static void sunburst(BufferedImage img, BufferedImage glow, int cx, int cy, int rgb) {
            int[][] px = {{0, 0}, {-1, 0}, {0, -1}, {-1, -1}, {-2, 0}, {1, -1}, {-1, 1}, {0, -2}, {-3, -1}, {2, 0}, {0, 2}, {-1, -3}};
            for (int[] d : px) {
                img.setRGB(cx + d[0], cy + d[1], 0xFF000000 | rgb);
                glow.setRGB(cx + d[0], cy + d[1], 0xFF000000 | rgb);
            }
        }

        /** Dark cracks wandering down a face, an ember glowing at every other step. */
        static void cracks(BufferedImage img, BufferedImage glow, Rect r, int crack, int ember, long seed) {
            Random rnd = new Random(seed);
            for (int n = 0; n < 2; n++) {
                int x = rnd.nextInt(r.w()), y = rnd.nextInt(Math.max(1, r.h() / 3));
                for (int step = 0; step < r.h() / 2 && y < r.h(); step++) {
                    boolean lit = step % 2 == 1;
                    img.setRGB(r.x() + x, r.y() + y, 0xFF000000 | (lit ? ember : crack));
                    if (lit) glow.setRGB(r.x() + x, r.y() + y, 0xFF000000 | ember);
                    y++;
                    x = Math.max(0, Math.min(r.w() - 1, x + rnd.nextInt(3) - 1));
                }
            }
        }

        /** The Starsteel Crown as worn: a band round the brow (the helmet's sides, top three rows) with points, a phlogiston stone in front. */
        static BufferedImage crownLayer() {
            BufferedImage img = new BufferedImage(64, 32, BufferedImage.TYPE_INT_ARGB);
            Box head = new Box(0, 0, 8, 8, 8);
            for (Rect side : new Rect[] {head.front(), head.back(), head.east(), head.west()}) {
                for (int x = 0; x < side.w(); x++) {
                    img.setRGB(side.x() + x, side.y() + 2, 0xFF000000 | Reworked.CRIMSON[1]);
                    img.setRGB(side.x() + x, side.y() + 1, 0xFF000000 | (x % 2 == 0 ? GOLD : Reworked.CRIMSON[2]));
                    if (x % 2 == 0) img.setRGB(side.x() + x, side.y(), 0xFF000000 | Reworked.CRIMSON[3]);
                }
            }
            Rect front = head.front();
            img.setRGB(front.x() + 3, front.y() + 1, 0xFF000000 | PHLOGISTON);
            img.setRGB(front.x() + 4, front.y() + 1, 0xFF000000 | PHLOGISTON_HI);
            return img;
        }

        /**
         * The Sun Pylon (12b): a captured sun inside a frame of crystal brick (studs at the corners); white-hot at the heart,
         * cooling to gold and amber, with eight rays reaching for the frame.
         */
        static BufferedImage pylon() {
            int[] sun = Reworked.SUN, frame = Reworked.CRYSTAL_STONE;
            BufferedImage img = Tiles.blank();
            for (int x = 0; x < 16; x++)
                for (int y = 0; y < 16; y++) {
                    int ring = Math.min(Math.min(x, y), Math.min(15 - x, 15 - y));
                    int c;
                    if (ring == 0) c = (x == 0 || y == 0) && x + y < 15 ? frame[2] : frame[1];
                    else if (ring == 1) c = (x == 1 || y == 1) && x + y < 15 ? frame[4] : frame[3];
                    else {
                        double dx = x + 0.5 - 8, dy = y + 0.5 - 8, d = Math.hypot(dx, dy);
                        double ray = Math.pow(Math.abs(Math.cos(Math.atan2(dy, dx) * 4)), 8);
                        double heat = 1 - d / 7.5 + ray * 0.28;
                        c = heat > 0.78 ? sun[4] : heat > 0.6 ? sun[3] : heat > 0.38 ? sun[2] : heat > 0.2 ? sun[1] : sun[0];
                    }
                    Tiles.px(img, x, y, c);
                }
            for (int[] s : new int[][] {{1, 1}, {14, 1}, {1, 14}, {14, 14}}) Tiles.px(img, s[0], s[1], frame[0]);
            return img;
        }

        /** The crown's icon: three gold-tipped starsteel points over a band rimmed in gold, set with three phlogiston stones. */
        static BufferedImage crownIcon() {
            int dk = Reworked.CRIMSON[1], md = Reworked.CRIMSON[2], lt = Reworked.CRIMSON[3];
            return sprite(md, pal('d', dk, 'm', md, 'l', lt, 'g', GOLD, 'G', GOLD_DK, 'P', 0xFFF0B0, 'v', PHLOGISTON, 'V', PHLOGISTON_HI, 'o', Reworked.OUTLINE),
                    "",
                    "",
                    "",
                    "       PP",
                    "   P  oggo  P",
                    "  ogo olmo ogo",
                    "  oloollmmoomo",
                    " ollmollmmolmmo",
                    " oPggggggggggGo",
                    " ollvllVvmmvmdo",
                    " olmvmmvvmmvddo",
                    " oggggggggggGGo",
                    "  oooooooooooo");
        }

        /** The Sunstone Lens: a starsteel ring round a disc of sunstone, on a short handle. */
        static BufferedImage lens() {
            BufferedImage img = new BufferedImage(16, 16, BufferedImage.TYPE_INT_ARGB);
            for (int x = 0; x < 16; x++) {
                for (int y = 0; y < 16; y++) {
                    double d = Math.hypot(x - 9.5, y - 6.0);
                    if (d <= 3.2) img.setRGB(x, y, 0xFF000000 | (x + y < 15 ? 0xFFF0A0 : 0xFFC040));
                    else if (d <= 4.6) img.setRGB(x, y, 0xFF000000 | (x + y < 15 ? Reworked.CRIMSON[3] : Reworked.CRIMSON[1]));
                }
            }
            img.setRGB(8, 4, 0xFFFFFFFF);
            img.setRGB(9, 4, 0xFFFFFFFF);
            for (int i = 0; i < 5; i++) {
                img.setRGB(5 - i, 10 + i, 0xFF000000 | GOLD);
                img.setRGB(6 - i, 10 + i, 0xFF000000 | GOLD_DK);
            }
            return img;
        }
        static Rect[] faces(Box b) {
            return new Rect[] {b.top(), b.bottom(), b.front(), b.back(), b.east(), b.west()};
        }

        /** Crystal: a pale blue face with diagonal facets, light along one way and dark along the other. */
        static void crystal(Painter p, Rect r) {
            p.face(r, CRYSTAL, 6);
            for (int x = 0; x < r.w(); x++) {
                for (int y = 0; y < r.h(); y++) {
                    if ((x + y) % 5 == 0) p.set(r.x() + x, r.y() + y, CRYSTAL_HI);
                    else if ((x - y + 40) % 7 == 0) p.set(r.x() + x, r.y() + y, CRYSTAL_DK);
                }
            }
        }

        static void fillGlow(BufferedImage img, BufferedImage glow, Rect r, int rgb) {
            for (int x = 0; x < r.w(); x++) for (int y = 0; y < r.h(); y++) {
                img.setRGB(r.x() + x, r.y() + y, 0xFF000000 | rgb);
                glow.setRGB(r.x() + x, r.y() + y, 0xFF000000 | rgb);
            }
        }

        /** The face's edge lit, in the image and its glow mask. */
        static void edgeGlow(BufferedImage img, BufferedImage glow, Rect r) {
            for (int x = 0; x < r.w(); x++) for (int y = 0; y < r.h(); y++) {
                if (x == 0 || y == 0 || x == r.w() - 1 || y == r.h() - 1) {
                    img.setRGB(r.x() + x, r.y() + y, 0xFF000000 | CRYSTAL_HI);
                    glow.setRGB(r.x() + x, r.y() + y, 0xFF000000 | 0x9FEFFF);
                }
            }
        }

        /**
         * The waystone (15c), a pale standing stone: its side dressed in rough courses, darker at the edges, with a column
         * of phlogiston runes glowing down its middle; its top a dressed face with a ring of runes round a phlogiston eye.
         */
        static BufferedImage waystoneSide() {
            BufferedImage img = new BufferedImage(16, 16, BufferedImage.TYPE_INT_ARGB);
            java.util.Random r = new java.util.Random(1715);
            for (int x = 0; x < 16; x++) {
                for (int y = 0; y < 16; y++) {
                    boolean seam = y % 5 == 4 || (x == (y / 5 % 2 == 0 ? 3 : 12));
                    int c = seam ? STONE_DK : x == 0 || x == 15 ? 0xB8C0C6 : r.nextInt(5) == 0 ? 0xE4E8EA : STONE;
                    img.setRGB(x, y, 0xFF000000 | c);
                }
            }
            int[][] runes = {{7, 1}, {8, 1}, {7, 2}, {6, 3}, {8, 3}, {7, 5}, {7, 6}, {8, 6}, {6, 7}, {7, 8}, {8, 9}, {7, 10},
                    {6, 11}, {8, 11}, {7, 12}, {7, 13}, {6, 14}, {8, 14}};
            for (int[] p : runes) {
                img.setRGB(p[0], p[1], 0xFF000000 | (p[1] % 4 == 1 ? PHLOGISTON_HI : PHLOGISTON));
            }
            return img;
        }

        static BufferedImage waystoneTop() {
            BufferedImage img = new BufferedImage(16, 16, BufferedImage.TYPE_INT_ARGB);
            for (int x = 0; x < 16; x++) {
                for (int y = 0; y < 16; y++) {
                    double d = Math.hypot(x + 0.5 - 8, y + 0.5 - 8);
                    int c = x == 0 || y == 0 || x == 15 || y == 15 ? STONE_DK : STONE;
                    if (d < 2.0) c = d < 1.0 ? PHLOGISTON_HI : PHLOGISTON;
                    else if (Math.abs(d - 5.0) < 0.6 && (x * 3 + y) % 4 != 0) c = PHLOGISTON;
                    else if (Math.abs(d - 5.0) < 1.2) c = STONE_DK;
                    img.setRGB(x, y, 0xFF000000 | c);
                }
            }
            return img;
        }

        /** The sigil: a gold ring round a pale crystal face, a four-pointed phlogiston star in the middle. */
        static BufferedImage sigil() {
            BufferedImage img = new BufferedImage(16, 16, BufferedImage.TYPE_INT_ARGB);
            for (int x = 0; x < 16; x++) {
                for (int y = 0; y < 16; y++) {
                    double d = Math.hypot(x - 7.5, y - 7.5);
                    int rgb;
                    if (d > 7.0) continue;
                    else if (d > 6.0) rgb = x + y < 15 ? 0xF0DC90 : GOLD_DK;
                    else if (d > 5.2) rgb = GOLD;
                    else rgb = (x + y) % 4 == 0 ? CRYSTAL_HI : CRYSTAL;
                    img.setRGB(x, y, 0xFF000000 | rgb);
                }
            }
            int[][] star = {{7, 3}, {8, 3}, {7, 4}, {8, 4}, {7, 5}, {8, 5}, {7, 10}, {8, 10}, {7, 11}, {8, 11}, {7, 12}, {8, 12},
                    {3, 7}, {3, 8}, {4, 7}, {4, 8}, {5, 7}, {5, 8}, {10, 7}, {10, 8}, {11, 7}, {11, 8}, {12, 7}, {12, 8}};
            for (int[] s : star) img.setRGB(s[0], s[1], 0xFF000000 | PHLOGISTON);
            for (int x = 6; x <= 9; x++) for (int y = 6; y <= 9; y++) img.setRGB(x, y, 0xFF000000 | ((x == 6 || x == 9) && (y == 6 || y == 9) ? PHLOGISTON : PHLOGISTON_HI));
            return img;
        }
    }

    // ---------------------------------------------------------------- the Grays (13a)

    /** The saucer, the Warden, the salvage and the spawn egg (the hull and wreckage blocks are in Reworked). */
    static final class Ufo {
        static final int HULL = 0xB8BEC6, HULL_DK = 0x7A828C, HULL_HI = 0xE2E6EA, GLOW = 0x9CFFB8, GLOW_HI = 0xE0FFE8;
        static final int SKIN = 0x9A9EA4, SKIN_DK = 0x72767C, SUIT = 0x3A3E48, SUIT_HI = 0x585E6C, EYE = 0x0A0C10, EYE_HI = 0x46566A;

        static int writeAll(File root) throws Exception {
            BufferedImage[] saucer = saucer();
            write(new File(root, "entity/gray_saucer.png"), saucer[0]);
            write(new File(root, "entity/gray_saucer_glowmask.png"), saucer[1]);
            BufferedImage[] warden = warden();
            write(new File(root, "entity/gray_warden.png"), warden[0]);
            write(new File(root, "entity/gray_warden_glowmask.png"), warden[1]);
            // 21c: a Gray in a duster and a hat too big for it, and the same Gray with the hat knocked off
            write(new File(root, "entity/disguised_gray.png"), disguised(false));
            write(new File(root, "entity/disguised_gray_exposed.png"), disguised(true));
            int n = 6;
            n += Items.put(root, "disguised_gray_spawn_egg", Items.egg(0x6A4A2C, pal('h', 0x3A2818, 'e', EYE, 's', SKIN),
                    "", "",
                    "   hhhhhhhhhh",
                    "     hhhhhh",
                    "     ssssss",
                    "    se ss es",
                    "     ssssss",
                    "", "",
                    "       ss"));
            n += Items.put(root, "damaged_gravity_drive", drive());
            n += Items.put(root, "beam_emitter", emitter());
            n += Items.put(root, "lens_array", lensArray());
            n += Items.put(root, "navigation_core", navCore());
            n += Items.put(root, "alien_alloy_plating", plating());
            n += Tiles.put(root, "saucer_hull", hull());
            n += Items.put(root, "gray_warden_spawn_egg", Items.egg(SKIN, pal('e', EYE, 'E', EYE_HI, 'm', SKIN_DK),
                    "", "", "", "", "",
                    "    eE    Ee",
                    "    eee  eee",
                    "     ee  ee",
                    "", "",
                    "       mm"));
            // Phlogiston tech (13b): the cells, full and empty, and the drive mended
            n += Items.put(root, "phlogiston_cell", cell(true));
            n += Items.put(root, "empty_phlogiston_cell", cell(false));
            n += Items.put(root, "gravity_drive", mendedDrive());
            // The Prototype Saucer (13c): the craft and its item
            BufferedImage[] proto = prototype();
            write(new File(root, "entity/prototype_saucer.png"), proto[0]);
            write(new File(root, "entity/prototype_saucer_glowmask.png"), proto[1]);
            n += 2;
            n += Items.put(root, "prototype_saucer", saucerItem());
            // The rematch fittings (15b)
            n += Items.put(root, "cloaking_device", cloakingDevice());
            n += Items.put(root, "cargo_hold", cargoHold());
            // The captives (20): the implant rig, and the letters the freed hand over
            n += Tiles.put(root, "implant_rig_side", rigSide());
            n += Tiles.put(root, "implant_rig_top", rigEnd());
            n += Items.put(root, "letter_home", letterHome());
            // The Mothership (13d): the ship, its drones (the probe's layout, violet), and the Phlogiston Beacon
            BufferedImage[] mother = mothership();
            write(new File(root, "entity/mothership.png"), mother[0]);
            write(new File(root, "entity/mothership_glowmask.png"), mother[1]);
            BufferedImage[] drone = Creatures.probe(0x4A4E58, 0x2C2F36, 0x70757F, 0xB070FF, 0xE8D8FF);
            write(new File(root, "entity/gray_drone.png"), drone[0]);
            write(new File(root, "entity/gray_drone_glowmask.png"), drone[1]);
            n += 4;
            n += Tiles.put(root, "phlogiston_beacon_side", beaconSide(false));
            n += Tiles.put(root, "phlogiston_beacon_side_lit", beaconSide(true));
            n += Tiles.put(root, "phlogiston_beacon_top", beaconTop(false));
            n += Tiles.put(root, "phlogiston_beacon_top_lit", beaconTop(true));
            n += Tiles.put(root, "phlogiston_beacon_bottom", beaconBottom());
            return n;
        }

        /** gray_saucer.geo.json's box UVs: silver panels, a rim strung with green lights, a dark glass dome, a glowing emitter. */
        static BufferedImage[] saucer() {
            BufferedImage img = new BufferedImage(128, 64, BufferedImage.TYPE_INT_ARGB);
            BufferedImage glow = new BufferedImage(128, 64, BufferedImage.TYPE_INT_ARGB);
            Painter p = new Painter(img, new Random(1947));
            Box rim = new Box(0, 0, 28, 2, 28);
            for (Rect face : Concavia.faces(rim)) {
                p.face(face, HULL, 6);
                if (face.h() == 2) {
                    for (int x = 1; x < face.w(); x += 3) {
                        img.setRGB(face.x() + x, face.y(), 0xFF000000 | GLOW);
                        glow.setRGB(face.x() + x, face.y(), 0xFF000000 | GLOW);
                    }
                    p.row(face, 1, HULL_DK);
                } else {
                    rings(p, face, HULL_DK, 4);
                }
            }
            Box hull = new Box(0, 30, 20, 2, 20);
            for (Rect face : Concavia.faces(hull)) {
                p.face(face, face.equals(hull.bottom()) ? HULL_DK : HULL, 6);
                if (face.h() == 20) {
                    rings(p, face, face.equals(hull.bottom()) ? 0x5A626C : HULL_DK, 3);
                } else {
                    p.row(face, 0, HULL_HI);
                }
            }
            Box dome = new Box(80, 30, 10, 4, 10);
            for (Rect face : Concavia.faces(dome)) {
                p.face(face, 0x1E3A30, 6);
                p.set(face.x() + 2, face.y() + 1, 0x7AA090);
                p.set(face.x() + 3, face.y() + 1, 0x5A8070);
                for (int x = 0; x < face.w(); x++) {
                    for (int y = 0; y < face.h(); y++) {
                        if ((x * 7 + y * 3) % 11 == 0) {
                            glow.setRGB(face.x() + x, face.y() + y, 0xFF000000 | 0x3A7A5A);
                        }
                    }
                }
            }
            for (Rect face : Concavia.faces(new Box(80, 44, 6, 1, 6))) {
                Concavia.fillGlow(img, glow, face, GLOW);
            }
            Rect under = new Box(80, 44, 6, 1, 6).bottom();
            for (int x = 1; x < 5; x++) for (int y = 1; y < 5; y++) {
                img.setRGB(under.x() + x, under.y() + y, 0xFF000000 | GLOW_HI);
                glow.setRGB(under.x() + x, under.y() + y, 0xFF000000 | GLOW_HI);
            }
            return new BufferedImage[] {img, glow};
        }

        /** Concentric panel seams on a square face, every {@code step} pixels in. */
        static void rings(Painter p, Rect r, int seam, int step) {
            double cx = r.w() / 2.0 - 0.5, cy = r.h() / 2.0 - 0.5;
            for (int x = 0; x < r.w(); x++) {
                for (int y = 0; y < r.h(); y++) {
                    int d = (int) Math.round(Math.hypot(x - cx, y - cy));
                    if (d > 0 && d % step == 0) p.set(r.x() + x, r.y() + y, seam);
                }
            }
        }

        /**
         * gray_warden.geo.json's box UVs: smooth grey skin, a great head with two black almond eyes slanting up and out, a
         * slit of a mouth; a close dark suit with a silver collar and a green sigil on the chest; a silver ray pistol with a
         * green muzzle. The sigil and the muzzle glow, and the eyes have a faint sheen.
         */
        static BufferedImage[] warden() {
            BufferedImage img = new BufferedImage(64, 64, BufferedImage.TYPE_INT_ARGB);
            BufferedImage glow = new BufferedImage(64, 64, BufferedImage.TYPE_INT_ARGB);
            Painter p = new Painter(img, new Random(1896));
            Box head = new Box(0, 0, 10, 9, 9);
            for (Rect face : Concavia.faces(head)) p.face(face, SKIN, 5);
            Rect f = head.front();
            int[][] eye = {{1, 3}, {2, 3}, {1, 4}, {2, 4}, {3, 4}, {2, 5}, {3, 5}, {3, 6}};
            for (int[] e : eye) {
                img.setRGB(f.x() + e[0], f.y() + e[1], 0xFF000000 | EYE);
                img.setRGB(f.x() + 9 - e[0], f.y() + e[1], 0xFF000000 | EYE);
            }
            for (int[] e : new int[][] {{2, 4}, {7, 4}}) {
                img.setRGB(f.x() + e[0], f.y() + e[1], 0xFF000000 | EYE_HI);
                glow.setRGB(f.x() + e[0], f.y() + e[1], 0xFF000000 | 0x203040);
            }
            p.set(f.x() + 4, f.y() + 7, SKIN_DK);
            p.set(f.x() + 5, f.y() + 7, SKIN_DK);
            p.set(f.x() + 4, f.y() + 6, SKIN_DK); // the faintest nose
            for (Rect face : Concavia.faces(new Box(56, 32, 2, 2, 2))) p.face(face, SKIN_DK, 4);
            for (Rect face : Concavia.faces(new Box(0, 32, 2, 14, 2))) {
                p.face(face, SUIT, 6);
                if (face.h() == 14) p.row(face, 13, SUIT_HI);
            }
            Box body = new Box(16, 32, 6, 10, 3);
            for (Rect face : Concavia.faces(body)) {
                p.face(face, SUIT, 6);
                if (face.h() == 10) p.row(face, 0, HULL);
            }
            Rect chest = body.front();
            for (int[] s : new int[][] {{2, 3}, {3, 3}, {2, 4}, {3, 4}, {1, 2}, {4, 2}}) {
                img.setRGB(chest.x() + s[0], chest.y() + s[1], 0xFF000000 | GLOW);
                glow.setRGB(chest.x() + s[0], chest.y() + s[1], 0xFF000000 | GLOW);
            }
            for (Rect face : Concavia.faces(new Box(36, 32, 2, 12, 2))) {
                p.face(face, SUIT, 6);
                if (face.h() == 12) {
                    p.row(face, 10, SKIN);
                    p.row(face, 11, SKIN);
                }
            }
            Box pistol = new Box(44, 32, 2, 2, 4);
            for (Rect face : Concavia.faces(pistol)) p.face(face, HULL, 4);
            Concavia.fillGlow(img, glow, pistol.front(), GLOW);
            return new BufferedImage[] {img, glow};
        }


        /**
         * The Disguised Gray (21c, the geo model disguised_gray): the Warden's head, limbs and suit (no pistol), under a
         * dusty brown duster with sleeves, and a wide-brimmed hat with a band, far too big for it. Exposed, the hat's pixels
         * are left clear, so the hat's gone and the great grey head shows.
         */
        static BufferedImage disguised(boolean exposed) {
            BufferedImage img = new BufferedImage(64, 64, BufferedImage.TYPE_INT_ARGB);
            Painter p = new Painter(img, new Random(1897));
            int coat = 0x7A5A38, coatDk = 0x5A4028, coatHi = 0x94704A, hat = 0x4A3420, hatHi = 0x5E4430, band = 0x2A1A10;
            Box head = new Box(0, 0, 10, 9, 9);
            for (Rect face : Concavia.faces(head)) p.face(face, SKIN, 5);
            Rect f = head.front();
            int[][] eye = {{1, 3}, {2, 3}, {1, 4}, {2, 4}, {3, 4}, {2, 5}, {3, 5}, {3, 6}};
            for (int[] e : eye) {
                img.setRGB(f.x() + e[0], f.y() + e[1], 0xFF000000 | EYE);
                img.setRGB(f.x() + 9 - e[0], f.y() + e[1], 0xFF000000 | EYE);
            }
            for (int[] e : new int[][] {{2, 4}, {7, 4}}) img.setRGB(f.x() + e[0], f.y() + e[1], 0xFF000000 | EYE_HI);
            p.set(f.x() + 4, f.y() + 7, SKIN_DK);
            p.set(f.x() + 5, f.y() + 7, SKIN_DK);
            for (Rect face : Concavia.faces(new Box(56, 32, 2, 2, 2))) p.face(face, SKIN_DK, 4);
            for (Rect face : Concavia.faces(new Box(0, 32, 2, 14, 2))) {
                p.face(face, SUIT, 6);
                if (face.h() == 14) p.row(face, 13, 0x2A1A10); // boots, of a sort
            }
            for (Rect face : Concavia.faces(new Box(16, 32, 6, 10, 3))) p.face(face, SUIT, 6);
            for (Rect face : Concavia.faces(new Box(36, 32, 2, 12, 2))) {
                p.face(face, SUIT, 6);
                if (face.h() == 12) {
                    p.row(face, 10, SKIN);
                    p.row(face, 11, SKIN);
                }
            }
            // The duster: dusty brown, lighter at the shoulders, a darker hem, a seam down the front
            Box duster = new Box(20, 46, 7, 14, 4);
            for (Rect face : Concavia.faces(duster)) {
                p.face(face, coat, 8);
                if (face.h() == 14) {
                    p.row(face, 0, coatHi);
                    p.row(face, 13, coatDk);
                }
            }
            Rect front = duster.front();
            for (int y = 1; y < 13; y++) img.setRGB(front.x() + 3, front.y() + y, 0xFF000000 | coatDk);
            for (Rect face : Concavia.faces(new Box(42, 46, 3, 12, 3))) {
                p.face(face, coat, 8);
                if (face.h() == 12) p.row(face, 11, coatDk);
            }
            if (!exposed) {
                // The hat: a broad brim, lit along its top, and a low crown with a dark band
                for (Rect face : Concavia.faces(new Box(0, 18, 14, 1, 13))) p.face(face, hat, 5);
                Rect brimTop = new Box(0, 18, 14, 1, 13).top();
                for (int x = 0; x < brimTop.w(); x++) img.setRGB(brimTop.x() + x, brimTop.y() + brimTop.h() - 1, 0xFF000000 | hatHi);
                for (Rect face : Concavia.faces(new Box(38, 0, 6, 5, 6))) {
                    p.face(face, hat, 5);
                    if (face.h() == 5) p.row(face, 4, band);
                }
            }
            return img;
        }

        /**
         * The Prototype Saucer (13c): the 64x64 swatch texture of tools/models/SaucerModels.java (these rectangles must match
         * its Mat). Row 0 holds flat colours for the disc's tops and bottoms and the small parts (flat, because the disc's
         * overlapping copies share those planes); then a side strip per disc layer, its detail in the middle (the only part
         * of a face that shows), the rim's strip with a phlogiston light, and the dome's glass, a pale blue at a third opacity with
         * a highlight. The rim light, drive, emitter, lens eye, nav ball and console screen glow.
         */
        static BufferedImage[] prototype() {
            BufferedImage img = new BufferedImage(64, 64, BufferedImage.TYPE_INT_ARGB);
            BufferedImage glow = new BufferedImage(64, 64, BufferedImage.TYPE_INT_ARGB);
            Painter p = new Painter(img, new Random(1913));
            int brass = 0xC8A048, brassDk = 0x8A6A28, under = 0x5A6068;
            int[] flat = {HULL_HI, HULL, HULL_DK, under, brass, brassDk};
            for (int i = 0; i < flat.length; i++) p.face(new Rect(4 * i, 0, 4, 4), flat[i], 0);
            Concavia.fillGlow(img, glow, new Rect(24, 0, 4, 4), GLOW);             // drive
            Concavia.fillGlow(img, glow, new Rect(28, 0, 4, 4), GLOW_HI);          // emitter
            p.face(new Rect(32, 0, 4, 4), 0x3A3E48, 0);                              // lens turret
            Concavia.fillGlow(img, glow, new Rect(36, 0, 4, 4), 0xFF8030);         // lens eye
            Concavia.fillGlow(img, glow, new Rect(40, 0, 4, 4), Concavia.GLOW);  // nav ball
            p.face(new Rect(44, 0, 4, 4), 0x4A3424, 0);                              // console
            // its screen: dark glass with a ring of glowing readouts round a lit dot
            for (int sx = 0; sx < 4; sx++) for (int sy = 0; sy < 4; sy++) {
                boolean lit = (sx == 0 || sx == 3) != (sy == 0 || sy == 3) || (sx == 1 && sy == 1);
                if (lit) Concavia.fillGlow(img, glow, new Rect(48 + sx, sy, 1, 1), sx == 1 && sy == 1 ? GLOW_HI : GLOW);
                else p.set(48 + sx, sy, 0x14201A);
            }
            // Side strips {u, layer diameter, colour}, row 8: a face of a 24-gon layer is d sin 7.5 degrees wide and uses that
            // much of its strip, so each gets a brass rivet at its own middle
            int[][] strips = {{0, 28, under}, {8, 44, HULL_DK}, {16, 54, HULL}, {32, 54, HULL_HI}, {40, 46, HULL}, {48, 38, brass}};
            for (int[] s : strips) {
                p.face(new Rect(s[0], 8, 8, 2), s[2], 3);
                int mid = (int) (s[1] * Math.sin(Math.toRadians(7.5)) / 2);
                p.set(s[0] + mid, 8, s[2] == brass ? 0xE6C676 : brass);
            }
            // The rim (60 across, so 7.8 px faces): brass bands top and bottom, alloy between, a phlogiston light in the middle
            Rect rim = new Rect(24, 8, 8, 4);
            p.face(rim, HULL_DK, 3);
            p.row(rim, 0, brassDk);
            p.row(rim, 3, brassDk);
            for (int x = 26; x < 30; x++) {
                for (int y = 9; y < 11; y++) {
                    int c = x == 26 || x == 29 ? GLOW : GLOW_HI;
                    img.setRGB(x, y, 0xFF000000 | c);
                    glow.setRGB(x, y, 0xFF000000 | c);
                }
            }
            // The glass: pale blue, a little brighter at the top, a white streak down one side
            for (int x = 0; x < 8; x++) {
                for (int y = 0; y < 12; y++) {
                    boolean streak = x == 1 || x == 2;
                    int a = streak ? 0x90 : y < 2 ? 0x70 : 0x50;
                    img.setRGB(x, 32 + y, (a << 24) | (streak ? 0xEAFBFF : 0xA8E4F4));
                }
            }
            return new BufferedImage[] {img, glow};
        }

        /** The Prototype Saucer's item: the craft side on, its glass dome, the brass rim with its lights, the drive aglow beneath. */
        static BufferedImage saucerItem() {
            BufferedImage img = new BufferedImage(16, 16, BufferedImage.TYPE_INT_ARGB);
            for (int x = 0; x < 16; x++) {
                for (int y = 2; y < 8; y++) {
                    double dx = (x - 7.5) / 4.6, dy = (y - 7.9) / 5.6, d = dx * dx + dy * dy;
                    if (d <= 1.0) {
                        boolean shine = (x == 5 || x == 6) && y >= 4 && y <= 5;
                        img.setRGB(x, y, 0xFF000000 | (d > 0.7 ? 0x5E9AB0 : shine ? 0xF4FFFF : 0xA8E4F4));
                    }
                }
            }
            int brass = 0xC8A048, brassDk = 0x8A6A28;
            int[][] rows = {{8, 3, 12, brassDk}, {9, 1, 14, HULL_HI}, {10, 0, 15, brass}, {11, 0, 15, brassDk}, {12, 1, 14, HULL},
                    {13, 3, 12, HULL_DK}, {14, 5, 10, GLOW}};
            for (int[] r : rows) {
                for (int x = r[1]; x <= r[2]; x++) img.setRGB(x, r[0], 0xFF000000 | r[3]);
            }
            for (int x = 1; x < 16; x += 3) img.setRGB(x, 10, 0xFF000000 | GLOW_HI);
            img.setRGB(7, 14, 0xFF000000 | GLOW_HI);
            img.setRGB(8, 14, 0xFF000000 | GLOW_HI);
            return img;
        }
        /**
         * The Mothership (13d): its 64x64 texture in tools/models/SaucerModels.java's swatch layout (the Prototype
         * Saucer's), painted dark: graphite and gunmetal, a rim of violet lights, a dome with pale windows, a violet beam
         * emitter, a core of hot violet light, and a red light on the dome. The lights, windows, emitter, core and red light
         * glow.
         */
        static BufferedImage[] mothership() {
            BufferedImage img = new BufferedImage(64, 64, BufferedImage.TYPE_INT_ARGB);
            BufferedImage glow = new BufferedImage(64, 64, BufferedImage.TYPE_INT_ARGB);
            Painter p = new Painter(img, new Random(1947));
            // Lighter than it looks: drawn at night, the first, near-black version showed only its rim lights (runPhoto)
            int hi = 0x9298A2, mid = 0x727882, dark = 0x575C66, under = 0x3E424A, gun = 0x7A7E88, gunDk = 0x50545C;
            int violet = 0xB070FF, violetHi = 0xE8D8FF;
            int[] flat = {hi, mid, dark, under, gun, gunDk};
            for (int i = 0; i < flat.length; i++) p.face(new Rect(4 * i, 0, 4, 4), flat[i], 0);
            Concavia.fillGlow(img, glow, new Rect(24, 0, 4, 4), violet);     // the emitter's ring
            Concavia.fillGlow(img, glow, new Rect(28, 0, 4, 4), violetHi);   // the emitter's eye, and the core
            Concavia.fillGlow(img, glow, new Rect(40, 0, 4, 4), 0xFF4030);   // the red light atop the dome
            Concavia.fillGlow(img, glow, new Rect(52, 0, 4, 4), 0x4A2E78);   // the dim violet glow under the skirt
            // Side strips (row 8), each a dark panel with a seam at its foot
            int[][] strips = {{0, under}, {8, dark}, {16, mid}, {32, mid}, {40, dark}, {48, gun}};
            for (int[] s : strips) {
                p.face(new Rect(s[0], 8, 8, 2), s[1], 3);
                p.row(new Rect(s[0], 8, 8, 2), 1, shade(s[1], 0.7));
            }
            // The rim: gunmetal bands top and bottom, graphite between, a violet light in the middle of each face
            Rect rim = new Rect(24, 8, 8, 4);
            p.face(rim, dark, 3);
            p.row(rim, 0, gun);
            p.row(rim, 3, gunDk);
            for (int x = 25; x < 31; x++) { // wide: from the ground, twenty blocks down, narrower ones were specks
                for (int y = 9; y < 11; y++) {
                    int c = x == 25 || x == 30 ? violet : violetHi;
                    img.setRGB(x, y, 0xFF000000 | c);
                    glow.setRGB(x, y, 0xFF000000 | c);
                }
            }
            // The dome's lower tier: dark, with a lit window in the middle of each face
            Rect dome = new Rect(56, 8, 8, 4);
            p.face(dome, dark, 3);
            for (int x = 59; x < 61; x++) {
                for (int y = 9; y < 11; y++) {
                    img.setRGB(x, y, 0xFF000000 | 0xC8F0E0);
                    glow.setRGB(x, y, 0xFF000000 | 0xC8F0E0);
                }
            }
            return new BufferedImage[] {img, glow};
        }

        /** The Phlogiston Beacon's side: a starsteel frame round a column of violet glass, dark or lit from within. */
        static BufferedImage beaconSide(boolean lit) {
            BufferedImage img = new BufferedImage(16, 16, BufferedImage.TYPE_INT_ARGB);
            for (int x = 0; x < 16; x++) {
                for (int y = 0; y < 16; y++) {
                    boolean frame = x < 2 || x > 13 || y < 2 || y > 13;
                    boolean glass = x >= 5 && x <= 10 && y >= 3 && y <= 12;
                    int c = frame ? ((x + y) % 3 == 0 ? Reworked.CRIMSON[3] : Reworked.CRIMSON[2])
                            : glass ? (lit ? ((x == 7 || x == 8) ? 0xF0E4FF : 0xB070FF) : ((x == 7 || x == 8) ? 0x5A3A80 : 0x3A2458))
                            : (x + y) % 4 == 0 ? 0x2E3036 : 0x3A3D44;
                    if (!frame && !glass && (x == 3 || x == 12) && (y == 3 || y == 12)) c = 0xE6C676; // brass rivets
                    img.setRGB(x, y, 0xFF000000 | c);
                }
            }
            return img;
        }

        /** Its top: the Phlogiston Core in a ring of Gray tech, dark or blazing. */
        static BufferedImage beaconTop(boolean lit) {
            BufferedImage img = new BufferedImage(16, 16, BufferedImage.TYPE_INT_ARGB);
            for (int x = 0; x < 16; x++) {
                for (int y = 0; y < 16; y++) {
                    double d = Math.hypot(x - 7.5, y - 7.5);
                    int c = d < 2.0 ? (lit ? 0xFFFFFF : 0x9FE8F4)
                            : d < 3.2 ? (lit ? 0xE0C8FF : Concavia.PHLOGISTON)
                            : d < 4.2 ? 0xC8A048
                            : d < 6.2 ? ((int) (Math.atan2(y - 7.5, x - 7.5) * 4 / Math.PI + 8) % 2 == 0 ? 0x3A3E48 : (lit ? 0xB070FF : 0x5A3A80))
                            : (x + y) % 3 == 0 ? Reworked.CRIMSON[3] : Reworked.CRIMSON[2];
                    img.setRGB(x, y, 0xFF000000 | c);
                }
            }
            return img;
        }

        /** Its foot: a plain starsteel plate, riveted at the corners. */
        static BufferedImage beaconBottom() {
            BufferedImage img = new BufferedImage(16, 16, BufferedImage.TYPE_INT_ARGB);
            for (int x = 0; x < 16; x++) {
                for (int y = 0; y < 16; y++) {
                    int c = (x == 2 || x == 13) && (y == 2 || y == 13) ? 0xE6C676 : (x + y) % 3 == 0 ? Reworked.CRIMSON[2] : Reworked.CRIMSON[1];
                    img.setRGB(x, y, 0xFF000000 | c);
                }
            }
            return img;
        }

        /** A Phlogiston Cell: a glass cylinder between brass caps, with a phlogiston shard glowing in it, or nothing if it's empty. */
        static BufferedImage cell(boolean charged) {
            BufferedImage img = new BufferedImage(16, 16, BufferedImage.TYPE_INT_ARGB);
            int brass = 0xC8A048, brassDk = 0x8A6A28;
            for (int x = 5; x <= 10; x++) {
                for (int y : new int[] {2, 3, 12, 13}) img.setRGB(x, y, 0xFF000000 | (x <= 6 ? 0xE6C676 : x >= 9 ? brassDk : brass));
            }
            for (int y = 4; y <= 11; y++) {
                for (int x = 5; x <= 10; x++) {
                    boolean edge = x == 5 || x == 10;
                    int c = edge ? 0xA8C8D0 : 0xD8EEF2;
                    if (charged && x >= 7 && x <= 8 && y >= 5 && y <= 10) c = (y + x) % 3 == 0 ? 0xFFF4DC : Concavia.PHLOGISTON;
                    img.setRGB(x, y, (edge || (charged && x >= 7 && x <= 8 && y >= 5 && y <= 10) ? 0xFF000000 : 0x90000000) | c);
                }
            }
            img.setRGB(7, 1, 0xFF000000 | brassDk);
            img.setRGB(8, 1, 0xFF000000 | brassDk);
            return img;
        }

        /** The gravity drive, mended: whole again, banded in starsteel, its core bright. */
        static BufferedImage mendedDrive() {
            BufferedImage img = new BufferedImage(16, 16, BufferedImage.TYPE_INT_ARGB);
            for (int x = 0; x < 16; x++) {
                for (int y = 0; y < 16; y++) {
                    double d = Math.hypot(x - 7.5, y - 7.5);
                    if (d <= 2.4) img.setRGB(x, y, 0xFF000000 | (d < 1.3 ? 0xFFFFFF : GLOW_HI));
                    else if (d > 3.8 && d <= 6.8) {
                        boolean band = Math.abs(x - 7.5) < 1 || Math.abs(y - 7.5) < 1;
                        img.setRGB(x, y, 0xFF000000 | (band ? Reworked.CRIMSON[2] : d > 6.0 ? HULL_DK : x + y < 15 ? HULL_HI : HULL));
                    } else if (d > 2.4 && d <= 3.8) img.setRGB(x, y, 0xFF000000 | ((x + y) % 2 == 0 ? GLOW : 0x4A8A60));
                }
            }
            return img;
        }
        /** A ring of alloy round a green core, cracked through on one side. */
        static BufferedImage drive() {
            BufferedImage img = new BufferedImage(16, 16, BufferedImage.TYPE_INT_ARGB);
            for (int x = 0; x < 16; x++) {
                for (int y = 0; y < 16; y++) {
                    double d = Math.hypot(x - 7.5, y - 7.5);
                    if (d <= 2.2) img.setRGB(x, y, 0xFF000000 | (d < 1.2 ? GLOW_HI : GLOW));
                    else if (d > 3.8 && d <= 6.8) img.setRGB(x, y, 0xFF000000 | (d > 6.0 ? HULL_DK : x + y < 15 ? HULL_HI : HULL));
                    else if (d > 2.2 && d <= 3.8 && (x + y) % 3 == 0) img.setRGB(x, y, 0xFF000000 | HULL_DK);
                }
            }
            for (int[] c : new int[][] {{11, 3}, {12, 4}, {11, 5}, {12, 6}, {13, 5}}) img.setRGB(c[0], c[1], 0x00000000); // the crack
            return img;
        }

        /** A beam emitter: an alloy dish on a stem and foot, a dark rod rising from its middle to a green-glowing focus. */
        static BufferedImage emitter() {
            return sprite(HULL, pal('k', 0x2A2E33, 'G', GLOW_HI, 'g', GLOW, 'd', 0x3A9A5A),
                    "       kk",
                    "      kGgk",
                    "      kgdk",
                    "  kkkkkkkkkkkk",
                    " k62221kk12224k",
                    " k66222kk22243k",
                    "  k6665555443k",
                    "   k66555443k",
                    "    kk5544kk",
                    "      k54k",
                    "      k54k",
                    "    kk6543kk",
                    "   k66554433k",
                    "   k55444332k",
                    "   kddgGgdddk",
                    "    kkkkkkkk");
        }

        /** Seven amber lenses in a honeycomb, each in a dark socket, set in a bevelled alloy plate. */
        static BufferedImage lensArray() {
            BufferedImage img = new BufferedImage(16, 16, BufferedImage.TYPE_INT_ARGB);
            for (int x = 1; x < 15; x++)
                for (int y = 2; y < 14; y++) {
                    if ((x == 1 || x == 14) && (y == 2 || y == 13)) continue;
                    boolean edge = x == 1 || x == 14 || y == 2 || y == 13;
                    int level = edge ? 1 : x == 2 || y == 3 ? 6 : x == 13 || y == 12 ? 3 : 4;
                    img.setRGB(x, y, 0xFF000000 | tone(HULL, level));
                }
            int[][] lenses = {{5, 4}, {9, 4}, {3, 7}, {7, 7}, {11, 7}, {5, 10}, {9, 10}};
            for (int[] l : lenses) {
                for (int dx = -1; dx <= 2; dx++)
                    for (int dy = -1; dy <= 2; dy++) {
                        boolean rim = dx == -1 || dx == 2 || dy == -1 || dy == 2;
                        if (rim && (dx == -1 || dx == 2) && (dy == -1 || dy == 2)) continue;
                        int c = rim ? 0x2A2E33 : dx == 0 && dy == 0 ? 0xFFE0A0 : dx == 1 && dy == 1 ? 0xC05010 : 0xFF9030;
                        img.setRGB(l[0] + dx, l[1] + dy, 0xFF000000 | c);
                    }
            }
            return img;
        }

        /** A gyroscope: a green-glowing core inside two crossed alloy rings, the near half of each ring passing in front. */
        static BufferedImage navCore() {
            BufferedImage img = new BufferedImage(16, 16, BufferedImage.TYPE_INT_ARGB);
            for (int pass = 0; pass < 3; pass++) { // far halves of the rings, then the core, then the near halves
                for (int x = 0; x < 16; x++)
                    for (int y = 0; y < 16; y++) {
                        double dx = x + 0.5 - 8, dy = y + 0.5 - 8;
                        if (pass == 1) {
                            double d = Math.hypot(dx, dy);
                            if (d <= 3.4) img.setRGB(x, y, 0xFF000000 | (d > 2.6 ? 0x1E6A3A : dx + dy < -1.2 ? 0xFFFFFF : dx + dy < 1 ? GLOW_HI : GLOW));
                            continue;
                        }
                        for (double a : new double[] {0, Math.PI / 2}) {
                            double rx = dx * Math.cos(a) + dy * Math.sin(a), ry = -dx * Math.sin(a) + dy * Math.cos(a);
                            double e = Math.hypot(rx / 6.8, ry / 3.0);
                            if (Math.abs(e - 1) > 0.17 || (ry > 0) != (pass == 2)) continue;
                            int c = e > 1.06 ? 0x2A2E33 : e < 0.92 ? HULL_DK : dy < 0 ? HULL_HI : HULL;
                            img.setRGB(x, y, 0xFF000000 | c);
                        }
                    }
            }
            return img;
        }

        /**
         * The cloaking device: an alloy ring, lit from the top left, round a lens that's half not there: a shimmer of cyan
         * and violet motes with the background showing between them, and a bright glint at the top left.
         */
        static BufferedImage cloakingDevice() {
            BufferedImage img = new BufferedImage(16, 16, BufferedImage.TYPE_INT_ARGB);
            for (int x = 0; x < 16; x++)
                for (int y = 0; y < 16; y++) {
                    double dx = x + 0.5 - 8, dy = y + 0.5 - 8, d = Math.hypot(dx, dy);
                    if (d > 7.2) continue;
                    int c;
                    if (d > 6.4) c = 0x2A2E33;
                    else if (d > 4.6) c = dx + dy < -2 ? HULL_HI : dx + dy < 2 ? HULL : HULL_DK;
                    else if (d > 4.0) c = 0x2A2E33;
                    else if ((x * 3 + y * 5) % 7 < 3) c = (x + y) % 3 == 0 ? 0xB090F0 : 0x70D8F0;
                    else continue; // the lens is mostly not there
                    img.setRGB(x, y, 0xFF000000 | c);
                }
            img.setRGB(6, 6, 0xFFE8FCFF);
            img.setRGB(7, 6, 0xFFB8F0FF);
            img.setRGB(6, 7, 0xFFB8F0FF);
            return img;
        }

        /** The cargo hold: a squat alloy crate, its hatch framed in dark seams, with a lit green latch across it. */
        static BufferedImage cargoHold() {
            return sprite(HULL, pal('k', 0x2A2E33, 'G', GLOW_HI, 'g', GLOW),
                    "",
                    "",
                    " kkkkkkkkkkkkkk",
                    " k666666666665k",
                    " k655555555543k",
                    " k5kkkkkkkkkk3k",
                    " k5k44444443k3k",
                    " k5k4gGGGg43k3k",
                    " k5k44444443k3k",
                    " k5k33333332k3k",
                    " k5kkkkkkkkkk3k",
                    " k544444444433k",
                    " k333333333322k",
                    " kkkkkkkkkkkkkk");
        }

        /**
         * The implant rig (20), its side: a pale alloy housing in two panels, dark seams between, a band of lit vents across
         * the middle and a row of rivets above and below.
         */
        static BufferedImage rigSide() {
            BufferedImage img = new BufferedImage(16, 16, BufferedImage.TYPE_INT_ARGB);
            for (int x = 0; x < 16; x++) {
                for (int y = 0; y < 16; y++) {
                    int c;
                    if (x == 0 || x == 15 || y == 0 || y == 15 || x == 7) c = 0x2A2E33;
                    else if (y >= 6 && y <= 9) c = y == 6 || y == 9 ? HULL_DK : (x % 2 == 0 ? (y == 7 ? GLOW_HI : GLOW) : 0x2A2E33);
                    else if ((y == 2 || y == 13) && (x == 2 || x == 5 || x == 9 || x == 12)) c = HULL_DK;
                    else c = x == 1 || x == 8 || y == 1 || y == 10 ? HULL_HI : x == 6 || x == 14 || y == 5 || y == 14 ? HULL_DK : HULL;
                    img.setRGB(x, y, 0xFF000000 | c);
                }
            }
            return img;
        }

        /** Its ends: the underside it works from, a ring of needle ports round a green eye in a dark collar. */
        static BufferedImage rigEnd() {
            BufferedImage img = new BufferedImage(16, 16, BufferedImage.TYPE_INT_ARGB);
            for (int x = 0; x < 16; x++) {
                for (int y = 0; y < 16; y++) {
                    double d = Math.hypot(x + 0.5 - 8, y + 0.5 - 8);
                    int c = x == 0 || y == 0 || x == 15 || y == 15 ? 0x2A2E33 : HULL;
                    if (d < 1.6) c = d < 0.8 ? GLOW_HI : GLOW;
                    else if (d < 3.0) c = 0x2A2E33;
                    else if (Math.abs(d - 5.0) < 0.7) c = (x + y) % 3 == 0 ? 0x1A1C20 : HULL_DK;
                    else if (d > 6.2 && x > 0 && y > 0 && x < 15 && y < 15 && (x + y) % 2 == 0) c = HULL_HI;
                    img.setRGB(x, y, 0xFF000000 | c);
                }
            }
            return img;
        }

        /** A Letter Home: a creased, grubby envelope, tied with a twist of string, the address scrawled in pencil. */
        static BufferedImage letterHome() {
            return sprite(0xE0D2AE, pal('k', 0x5A4A34, 's', 0xA88A5A, 'p', 0x6A6460),
                    "",
                    "",
                    "",
                    " kkkkkkkkkkkkkk",
                    " k6k44444444k5k",
                    " k56k444444k54k",
                    " k555k4444k544k",
                    " k5555kssk5444k",
                    " kssssssssssssk",
                    " k55555ss54444k",
                    " k5555pppp5444k",
                    " k55ppp5pp5443k",
                    " k444444444433k",
                    " kkkkkkkkkkkkkk");
        }

        /** Alien alloy plating: two bevelled plates, one overlapping the other, flush rivets at the corners, a lit trace on the near one. */
        static BufferedImage plating() {
            BufferedImage img = new BufferedImage(16, 16, BufferedImage.TYPE_INT_ARGB);
            plate(img, 5, 1, 14, 9);
            plate(img, 1, 5, 11, 14);
            for (int x = 4; x <= 8; x++) img.setRGB(x, 9, 0xFF000000 | (x == 8 ? GLOW_HI : GLOW));
            for (int y = 10; y <= 11; y++) img.setRGB(8, y, 0xFF000000 | GLOW);
            img.setRGB(4, 8, 0xFF000000 | 0x3A9A5A);
            return img;
        }

        static void plate(BufferedImage img, int x0, int y0, int x1, int y1) {
            int[] ramp = Reworked.ALLOY;
            for (int x = x0; x <= x1; x++)
                for (int y = y0; y <= y1; y++) {
                    if ((x == x0 || x == x1) && (y == y0 || y == y1)) continue;
                    boolean edge = x == x0 || x == x1 || y == y0 || y == y1;
                    int c = edge ? 0x2A2E33 : x == x0 + 1 || y == y0 + 1 ? ramp[4] : x == x1 - 1 || y == y1 - 1 ? ramp[1] : (x - y) % 6 == 0 ? ramp[3] : ramp[2];
                    img.setRGB(x, y, 0xFF000000 | c);
                }
            for (int[] r : new int[][] {{x0 + 2, y0 + 2}, {x1 - 2, y0 + 2}, {x0 + 2, y1 - 2}, {x1 - 2, y1 - 2}}) {
                img.setRGB(r[0], r[1], 0xFF000000 | ramp[0]);
            }
        }

        /**
         * Saucer hull: two broad plates of pale alloy, each the block's width and half its height, their joints staggered; each
         * plate is brightest along its top and darkens down its face, with a soft sheen line, and a row of flush rivets runs
         * along every seam.
         */
        static BufferedImage hull() {
            int[] ramp = Reworked.ALLOY;
            BufferedImage img = Tiles.blank();
            for (int x = 0; x < 16; x++)
                for (int y = 0; y < 16; y++) {
                    int py = y % 8, px = (x + (y / 8) * 8) % 16;
                    int c = py == 7 || px == 15 ? ramp[1] : py == 0 ? ramp[4] : py == 2 && px > 1 && px < 13 ? ramp[4] : py < 4 ? ramp[3] : ramp[2];
                    if (py == 6 && x % 3 == 1) c = ramp[1]; // rivets along the seam
                    Tiles.px(img, x, y, c);
                }
            return img;
        }
    }

    static final class Reworked {
        static final int OUTLINE = Starsteel.OUTLINE, GOLD = Starsteel.GOLD, PALE_GOLD = Starsteel.PALE_GOLD;
        static final int PHLOGISTON = Starsteel.PHLOGISTON, PHLOGISTON_HI = Starsteel.PHLOGISTON_HI;
        static final int[] CRIMSON = {OUTLINE, Starsteel.DARK, Starsteel.MID, Starsteel.LIGHT, 0xE04A52};
        static final int[] EMBER = {0x3A1004, 0x7A2A08, 0xD8601A, 0xF6A04A, 0xFFF0D0};
        static final int[] BRASS = {0x3A2608, 0x6E4A14, 0xA07A28, 0xC89E40, 0xE6C676};
        static final int[] IRON_DUST = {0x3A3A3E, 0x6E6E74, 0xA4A4AA, 0xD0D0D4, 0xF2F2F2};
        static final int[] GOLD_DUST = {0x5A3A08, 0xA0701A, 0xE0B030, 0xF4D460, 0xFFF2B0};
        static final int[] COPPER_DUST = {0x4A2010, 0x8A4426, 0xC0683E, 0xE08A5A, 0xF6B894};
        static final int[] IVORY = {0x5A4A30, 0xA8966A, 0xD8CCA8, 0xEDE4C8, 0xFFFBEA};
        static final int[] RAW_MEAT = {0x3A0E0E, 0x6E1E1C, 0x9A3430, 0xBC5048, 0xE8B4A0};
        static final int[] COOKED_MEAT = {0x2A160C, 0x5A3018, 0x7E4A26, 0xA0683A, 0xC89060};
        static final int[] HIDE = {0x231A14, 0x45362A, 0x66523E, 0x86705A, 0xA89078};
        static final int[] SHELL = {0x2A2216, 0x5A4A2E, 0x8A7648, 0xAA9460, 0xC8B27C};
        static final int[] BARDING = {0x3A2A1E, 0x6A5440, 0x8E7658, 0xAE9474, 0xCDB494};
        // Wider ramps for the icons, so they read at 16 px: shell plates, shaggy hide, dark leather strapping
        static final int[] SCUTE = {0x2A1C10, 0x5A4022, 0x8A683C, 0xB08C56, 0xD8B880};
        static final int[] HIDE_WIDE = {0x24180E, 0x4E3824, 0x7A5A3A, 0xA6845E, 0xD8BC94};
        static final int[] STRAP = {0x1A0E08, 0x3A2214, 0x5A3820, 0x7A5030, 0x9A6A40};
        static final int[] CRYSTAL_STONE = {0x3E6C82, 0x86B2C6, 0xB8D8E6, 0xDCEEF6, 0xF6FCFF};
        static final int[] SUN = {0x6A3A08, 0xB86A10, 0xF0A020, 0xFFD050, 0xFFF4B0};
        static final int[] CODEX = {0x0E2A44, 0x1E5A80, 0x3A8CB8, 0x7FC4E4, 0xC8ECF8};
        static final int[] ALLOY = {0x5A626C, 0x8A929C, 0xB8BEC6, 0xD6DADE, 0xF2F4F6};
        static final int[] ALLOY_DK = {0x2E3238, 0x4E545C, 0x6E767E, 0x8E969E, 0xAEB4BA};
        /** A pixel this colourful (channel spread) is an ore's fleck or a tool's wooden handle rather than stone or metal. */
        static final int CHROMA = 20;

        enum Part { ALL, FLECKS, METAL }

        static int writeAll(File root) throws Exception {
            File jar = gameJar();
            if (jar == null) {
                System.out.println("TextureArt: no game jar under build/moddev/artifacts, keeping the hand-drawn metals");
                return 0;
            }
            int n = 0;
            try (java.util.zip.ZipFile zip = new java.util.zip.ZipFile(jar)) {
                // Starsteel and phlogiston (9c)
                n += Tiles.put(root, "starsteel_ore", glint(recolor(vanilla(zip, "block/iron_ore"), CRIMSON, Part.FLECKS, false), true));
                n += Tiles.put(root, "deepslate_starsteel_ore", glint(recolor(vanilla(zip, "block/deepslate_iron_ore"), CRIMSON, Part.FLECKS, false), true));
                n += Tiles.put(root, "phlogiston_ore", recolor(vanilla(zip, "block/emerald_ore"), EMBER, Part.FLECKS, false));
                n += Tiles.put(root, "deepslate_phlogiston_ore", recolor(vanilla(zip, "block/deepslate_emerald_ore"), EMBER, Part.FLECKS, false));
                n += Tiles.put(root, "raw_starsteel_block", glint(recolor(vanilla(zip, "block/raw_iron_block"), CRIMSON, Part.ALL, false), false));
                BufferedImage block = recolor(vanilla(zip, "block/iron_block"), CRIMSON, Part.ALL, false);
                for (int i = 0; i < 16; i++) { // a gold frame round the panel
                    for (int j : new int[] {0, 15}) {
                        block.setRGB(i, j, 0xFF000000 | (j == 0 ? PALE_GOLD : GOLD));
                        block.setRGB(j, i, 0xFF000000 | (j == 0 ? PALE_GOLD : GOLD));
                    }
                }
                n += Tiles.put(root, "starsteel_block", gem(block, 7, 7, 8, 7, 7, 8, 8, 8));
                n += Items.put(root, "starsteel_ingot", recolor(vanilla(zip, "item/iron_ingot"), CRIMSON, Part.ALL, true));
                n += Items.put(root, "starsteel_nugget", recolor(vanilla(zip, "item/iron_nugget"), CRIMSON, Part.ALL, true));
                n += Items.put(root, "raw_starsteel", glint(recolor(vanilla(zip, "item/raw_iron"), CRIMSON, Part.ALL, false), false));
                n += Items.put(root, "starsteel_dust", glint(recolor(vanilla(zip, "item/redstone"), CRIMSON, Part.ALL, false), false));
                n += Items.put(root, "starsteel_helmet", gem(recolor(vanilla(zip, "item/iron_helmet"), CRIMSON, Part.ALL, true), 7, 5, 8, 5));
                n += Items.put(root, "starsteel_chestplate", gem(recolor(vanilla(zip, "item/iron_chestplate"), CRIMSON, Part.ALL, true), 7, 8, 8, 8, 7, 9, 8, 9));
                n += Items.put(root, "starsteel_leggings", gem(recolor(vanilla(zip, "item/iron_leggings"), CRIMSON, Part.ALL, true), 7, 3, 8, 3));
                n += Items.put(root, "starsteel_boots", gem(recolor(vanilla(zip, "item/iron_boots"), CRIMSON, Part.ALL, true), 5, 5, 11, 5));
                n += Items.put(root, "starsteel_sword", gem(glint(recolor(vanilla(zip, "item/iron_sword"), CRIMSON, Part.METAL, false), true), 4, 8, 4, 9));
                BufferedImage paxel = under(vanilla(zip, "item/iron_axe"), vanilla(zip, "item/iron_pickaxe"));
                n += Items.put(root, "starsteel_paxel", gem(glint(recolor(paxel, CRIMSON, Part.METAL, false), true), 9, 4, 9, 5));
                BufferedImage worn = gem(recolor(vanilla(zip, "entity/equipment/humanoid/iron"), CRIMSON, Part.ALL, false), 11, 9, 12, 9, 23, 22, 24, 22, 23, 23, 24, 23);
                write(new File(root, "entity/equipment/humanoid/starsteel.png"), worn);
                write(new File(root, "entity/equipment/humanoid_baby/starsteel.png"), worn);
                write(new File(root, "entity/equipment/humanoid_leggings/starsteel.png"),
                        recolor(vanilla(zip, "entity/equipment/humanoid_leggings/iron"), CRIMSON, Part.ALL, false));
                n += 3;
                // Brass, from gold
                n += Items.put(root, "brass_ingot", recolor(vanilla(zip, "item/gold_ingot"), BRASS, Part.ALL, false));
                n += Items.put(root, "brass_nugget", recolor(vanilla(zip, "item/gold_nugget"), BRASS, Part.ALL, false));
                BufferedImage brass = recolor(vanilla(zip, "block/gold_block"), BRASS, Part.ALL, false);
                for (int[] rivet : new int[][] {{2, 2}, {13, 2}, {2, 13}, {13, 13}}) {
                    brass.setRGB(rivet[0], rivet[1], 0xFF000000 | BRASS[0]);
                    brass.setRGB(rivet[0] - 1, rivet[1] - 1, 0xFF000000 | BRASS[4]);
                }
                n += Tiles.put(root, "brass_block", brass);
                // The crusher's dusts, from redstone dust
                n += Items.put(root, "iron_dust", recolor(vanilla(zip, "item/redstone"), IRON_DUST, Part.ALL, false));
                n += Items.put(root, "gold_dust", recolor(vanilla(zip, "item/redstone"), GOLD_DUST, Part.ALL, false));
                n += Items.put(root, "copper_dust", recolor(vanilla(zip, "item/redstone"), COPPER_DUST, Part.ALL, false));
                // A mutilated steer (11a): vanilla's temperate cow (its model's UVs, see cattle_carcass.geo.json), a neat square
                // cut from each flank showing raw flesh in a dark rim, the right eye cut out clean; the rims glow faintly
                BufferedImage cow = vanilla(zip, "entity/cow/cow_temperate");
                BufferedImage trace = new BufferedImage(cow.getWidth(), cow.getHeight(), BufferedImage.TYPE_INT_ARGB);
                for (int fx : new int[] {20, 42}) { // the flanks: the body box's side faces, x 18..27 and 40..49, y 14..31
                    for (int x = 0; x < 6; x++) {
                        for (int y = 0; y < 5; y++) {
                            boolean rim = x == 0 || y == 0 || x == 5 || y == 4;
                            cow.setRGB(fx + x, 19 + y, 0xFF000000 | (rim ? Mystery.CUT : (x + y) % 3 == 0 ? shade(Mystery.FLESH, 0.85) : Mystery.FLESH));
                            if (rim) trace.setRGB(fx + x, 19 + y, 0xFF000000 | Mystery.TRACE);
                        }
                    }
                }
                for (int[] p : new int[][] {{12, 8}, {13, 8}, {12, 9}, {13, 9}}) cow.setRGB(p[0], p[1], 0xFF1A0E0E); // the eye, gone
                for (int[] p : new int[][] {{11, 8}, {11, 9}, {12, 7}, {13, 7}, {12, 10}, {13, 10}}) {
                    cow.setRGB(p[0], p[1], 0xFF000000 | Mystery.CUT);
                    trace.setRGB(p[0], p[1], 0xFF000000 | Mystery.TRACE);
                }
                write(new File(root, "entity/cattle_carcass.png"), cow);
                write(new File(root, "entity/cattle_carcass_glowmask.png"), trace);
                n += 2;
                // Concavia's crystal stone (12a): quartz bricks and chiseled quartz frosted ice-blue, the chiseled block's
                // grooves running with phlogiston; the crystal itself from glass, its frame kept and its pane filled see-through blue
                n += Tiles.put(root, "crystal_bricks", recolor(vanilla(zip, "block/quartz_bricks"), CRYSTAL_STONE, Part.ALL, false));
                BufferedImage chiseled = recolor(vanilla(zip, "block/chiseled_quartz_block"), CRYSTAL_STONE, Part.ALL, false);
                for (int x = 0; x < 16; x++) {
                    for (int y = 0; y < 16; y++) {
                        int rgb = chiseled.getRGB(x, y) & 0xFFFFFF;
                        if (rgb == CRYSTAL_STONE[0]) chiseled.setRGB(x, y, 0xFF000000 | Concavia.GLOW);
                        else if (rgb == CRYSTAL_STONE[1]) chiseled.setRGB(x, y, 0xFF000000 | 0x4FB8D8);
                    }
                }
                n += Tiles.put(root, "chiseled_crystal_bricks", chiseled);
                BufferedImage glass = vanilla(zip, "block/glass");
                BufferedImage crystal = new BufferedImage(16, 16, BufferedImage.TYPE_INT_ARGB);
                for (int x = 0; x < 16; x++) {
                    for (int y = 0; y < 16; y++) {
                        int p = glass.getRGB(x, y);
                        if (!clear(p)) {
                            crystal.setRGB(x, y, 0xE0000000 | (lum(p) > 180 ? Concavia.CRYSTAL_HI : Concavia.CRYSTAL));
                        } else if ((x + y) % 6 == 0) {
                            crystal.setRGB(x, y, 0xB0000000 | 0xC8F0FA);
                        } else if ((x - y + 32) % 9 == 0) {
                            crystal.setRGB(x, y, 0xA0000000 | 0x5AA8C8);
                        } else {
                            crystal.setRGB(x, y, 0x88000000 | 0x8FD4EA);
                        }
                    }
                }
                n += Tiles.put(root, "concavian_crystal", crystal);
                // The Sun Archon's things (12b): sunstone from the amethyst shard; the Phlogiston Core from the heart of the sea in phlogiston
                // blue; the Codex from the written book, bound in crystal blue with its gold kept (the pylon is Concavia.pylon)
                n += Items.put(root, "sunstone_shard", recolor(vanilla(zip, "item/amethyst_shard"), SUN, Part.ALL, false));
                n += Items.put(root, "phlogiston_core", recolor(vanilla(zip, "item/heart_of_the_sea"), EMBER, Part.ALL, true));
                n += Items.put(root, "concavian_codex", recolor(vanilla(zip, "item/written_book"), CODEX, Part.FLECKS, false));
                // The Grays' wrecks (13a): wreckage from the iron block in dark alloy, torn, scorched and with a green light still
                // in it (the whole hull and the alloy plating are Ufo.hull and Ufo.plating)
                BufferedImage wreck = recolor(vanilla(zip, "block/iron_block"), ALLOY_DK, Part.ALL, false);
                java.util.Random tear = new java.util.Random(1947);
                for (int i = 0; i < 26; i++) {
                    int x = tear.nextInt(16), y = tear.nextInt(16);
                    wreck.setRGB(x, y, 0xFF000000 | (i < 18 ? 0x1A1C1E : i < 23 ? 0x3A2A20 : Ufo.GLOW));
                }
                n += Tiles.put(root, "saucer_wreckage", wreck);
                // A Gray Landing's cache (15a): the hull's alloy framed in dark seams, a lit green band across it and a lock at its heart
                BufferedImage cache = recolor(vanilla(zip, "block/iron_block"), ALLOY, Part.ALL, false);
                for (int i = 0; i < 16; i++) {
                    for (int[] p : new int[][] {{i, 0}, {i, 15}, {0, i}, {15, i}}) {
                        cache.setRGB(p[0], p[1], 0xFF000000 | 0x2A2E33);
                    }
                    if (i > 0 && i < 15) {
                        cache.setRGB(i, 7, 0xFF000000 | (i % 3 == 0 ? 0x2A2E33 : Ufo.GLOW));
                        cache.setRGB(i, 8, 0xFF000000 | 0x2A2E33);
                    }
                }
                for (int x = 6; x <= 9; x++) {
                    for (int y = 5; y <= 10; y++) {
                        boolean edge = x == 6 || x == 9 || y == 5 || y == 10;
                        cache.setRGB(x, y, 0xFF000000 | (edge ? 0x2A2E33 : Ufo.GLOW));
                    }
                }
                n += Tiles.put(root, "gray_cache", cache);
                // The megafauna's drops and gear (10a)
                n += Items.put(root, "oliphant", brassFittings(recolor(vanilla(zip, "item/goat_horn"), IVORY, Part.ALL, false)));
                n += Items.put(root, "raw_mammoth", marble(recolor(vanilla(zip, "item/beef"), RAW_MEAT, Part.ALL, false), RAW_MEAT[4]));
                n += Items.put(root, "mammoth_steak", grill(recolor(vanilla(zip, "item/cooked_beef"), COOKED_MEAT, Part.ALL, false), COOKED_MEAT[0]));
                n += Items.put(root, "thick_hide", fuzz(recolor(vanilla(zip, "item/leather"), HIDE, Part.ALL, false), HIDE[4]));
                // The barding's icon: the horse armour's plates in shaggy hide (pale hair along the top edges), its red cloth
                // turned to dark leather straps, brass buckles on them
                BufferedImage barding = fuzz(recolor(vanilla(zip, "item/iron_horse_armor"), HIDE_WIDE, Part.METAL, false), HIDE_WIDE[4]);
                barding = recolor(barding, STRAP, Part.FLECKS, false);
                for (int x = 0; x < 16; x++) {
                    for (int y = 0; y < 16; y++) {
                        if ((barding.getRGB(x, y) & 0xFFFFFF) == STRAP[3] && (x + y) % 4 == 0) barding.setRGB(x, y, 0xFF000000 | BRASS[4]);
                    }
                }
                n += Items.put(root, "hide_barding", barding);
                write(new File(root, "entity/equipment/horse_body/hide_barding.png"),
                        strands(recolor(vanilla(zip, "entity/equipment/horse_body/iron"), BARDING, Part.ALL, false), BARDING[1]));
                n++;
                n += Items.put(root, "glyptodon_scute", shellPlates(recolor(vanilla(zip, "item/turtle_scute"), SCUTE, Part.ALL, false), SCUTE, 4));
                for (String piece : new String[] {"helmet", "chestplate", "leggings", "boots"}) {
                    n += Items.put(root, "glyptodon_" + piece, shellPlates(recolor(vanilla(zip, "item/iron_" + piece), SCUTE, Part.ALL, false), SCUTE, 4));
                }
                // The steam handcar: vanilla's minecart with the brass engine standing up out of it, a black stack on top
                BufferedImage handcar = vanilla(zip, "item/minecart");
                draw(handcar, 0, 0, Items.BRASS, pal('k', 0x1E1E22, 's', 0x3A3C40, 'S', 0x6A6E74, 'i', 0x4A4E54),
                        "      kkkk",
                        "      kSsk",
                        "     kkkkkk",
                        "     k6554k",
                        "     kiiiik",
                        "     k6544k",
                        "     k6543k",
                        "     k5433k");
                n += Items.put(root, "steam_handcar", handcar);
                BufferedImage shell = seams(recolor(vanilla(zip, "entity/equipment/humanoid/iron"), SHELL, Part.ALL, false), SHELL, 4);
                write(new File(root, "entity/equipment/humanoid/glyptodon_shell.png"), shell);
                write(new File(root, "entity/equipment/humanoid_baby/glyptodon_shell.png"), shell);
                write(new File(root, "entity/equipment/humanoid_leggings/glyptodon_shell.png"),
                        seams(recolor(vanilla(zip, "entity/equipment/humanoid_leggings/iron"), SHELL, Part.ALL, false), SHELL, 4));
                n += 3;
                // 24c: the town lamp by day: vanilla's lantern with its flame and glow put out (sooty glass, a dead wick)
                BufferedImage strip = vanilla(zip, "block/lantern");
                // Its first frame only: vanilla's is a strip of flame frames (an animation), which as one image the model stretched
                BufferedImage cold = new BufferedImage(strip.getWidth(), strip.getWidth(), BufferedImage.TYPE_INT_ARGB);
                cold.getGraphics().drawImage(strip.getSubimage(0, 0, strip.getWidth(), strip.getWidth()), 0, 0, null);
                for (int x = 0; x < cold.getWidth(); x++) {
                    for (int y = 0; y < cold.getHeight(); y++) {
                        int argb = cold.getRGB(x, y), r = argb >> 16 & 0xFF, g = argb >> 8 & 0xFF, b = argb & 0xFF;
                        if ((argb >>> 24) != 0 && r > 170 && g > 100 && b < 160) {
                            int grey = (r + g + b) / 3 * 30 / 100;
                            cold.setRGB(x, y, argb & 0xFF000000 | Math.min(255, grey + 14) << 16 | Math.min(255, grey + 10) << 8 | grey);
                        }
                    }
                }
                n += Tiles.put(root, "town_lamp_unlit", cold);
            }
            return n;
        }

        /** The patched game jar the build makes (holds vanilla's assets), or null before the first build. */
        static File gameJar() {
            File[] jars = new File("build/moddev/artifacts").listFiles((dir, name) -> name.startsWith("minecraft-patched-")
                    && name.endsWith(".jar") && !name.contains("sources") && !name.contains("merged"));
            return jars == null || jars.length == 0 ? null : jars[0];
        }

        static BufferedImage vanilla(java.util.zip.ZipFile zip, String texture) throws Exception {
            var entry = zip.getEntry("assets/minecraft/textures/" + texture + ".png");
            if (entry == null) {
                throw new IllegalStateException("vanilla texture missing from the game jar: " + texture);
            }
            try (var in = zip.getInputStream(entry)) {
                BufferedImage read = ImageIO.read(in);
                BufferedImage img = new BufferedImage(read.getWidth(), read.getHeight(), BufferedImage.TYPE_INT_ARGB);
                img.getGraphics().drawImage(read, 0, 0, null);
                return img;
            }
        }

        /** The Oliphant (the goat horn in ivory): a brass rim round the bell, a brass band, a brass mouthpiece. */
        static BufferedImage brassFittings(BufferedImage img) {
            for (int x = 0; x < img.getWidth(); x++) {
                for (int y = 0; y < img.getHeight(); y++) {
                    int p = img.getRGB(x, y);
                    if (!clear(p) && (p & 0xFFFFFF) != IVORY[0] && (x >= 13 || x == 9 || y <= 4)) {
                        img.setRGB(x, y, 0xFF000000 | (y <= 4 || x == 13 ? BRASS[4] : BRASS[3]));
                    }
                }
            }
            return img;
        }

        /** Streaks of fat through raw meat. */
        static BufferedImage marble(BufferedImage img, int fat) {
            for (int x = 1; x < img.getWidth() - 1; x++) {
                for (int y = 1; y < img.getHeight() - 1; y++) {
                    if (interior(img, x, y) && (x * 3 + y * 7) % 11 == 0) img.setRGB(x, y, 0xFF000000 | fat);
                }
            }
            return img;
        }

        /** Dark grill marks across a steak. */
        static BufferedImage grill(BufferedImage img, int mark) {
            for (int x = 1; x < img.getWidth() - 1; x++) {
                for (int y = 1; y < img.getHeight() - 1; y++) {
                    if (interior(img, x, y) && (x - y == -2 || x - y == 3)) img.setRGB(x, y, 0xFF000000 | mark);
                }
            }
            return img;
        }

        /** Pale hair standing along a hide's top edges. */
        static BufferedImage fuzz(BufferedImage img, int hair) {
            for (int x = 0; x < img.getWidth(); x += 2) {
                for (int y = 1; y < img.getHeight(); y++) {
                    if (!clear(img.getRGB(x, y)) && clear(img.getRGB(x, y - 1))) img.setRGB(x, y, 0xFF000000 | hair);
                }
            }
            return img;
        }

        /** Dark strands scattered through a worn hide layer. */
        static BufferedImage strands(BufferedImage img, int dark) {
            for (int x = 0; x < img.getWidth(); x++) {
                for (int y = 0; y < img.getHeight(); y++) {
                    if (!clear(img.getRGB(x, y)) && ((x * 7 + y * 13) % 9 == 0 || (x * 5 + y * 3) % 17 == 0)) img.setRGB(x, y, 0xFF000000 | dark);
                }
            }
            return img;
        }

        /** Scute seams in a brick pattern every {@code cell} pixels, on the lit pixels (not the outline or the shadows). */
        static BufferedImage seams(BufferedImage img, int[] ramp, int cell) {
            for (int x = 0; x < img.getWidth(); x++) {
                for (int y = 0; y < img.getHeight(); y++) {
                    int p = img.getRGB(x, y) & 0xFFFFFF;
                    if (clear(img.getRGB(x, y)) || p == ramp[0] || p == ramp[1]) {
                        continue;
                    }
                    int sx = x + ((y / cell) % 2) * (cell / 2);
                    if (sx % cell == 0 || y % cell == 0) {
                        img.setRGB(x, y, 0xFF000000 | ramp[1]);
                    }
                }
            }
            return img;
        }

        /**
         * Raised shell plates for an icon, in staggered rows {@code cell} pixels apart: each plate is bevelled, lit along its
         * top edge and cut off by a dark seam along its bottom and right. Outline pixels are left alone.
         */
        static BufferedImage shellPlates(BufferedImage img, int[] ramp, int cell) {
            for (int x = 0; x < img.getWidth(); x++) {
                for (int y = 0; y < img.getHeight(); y++) {
                    if (clear(img.getRGB(x, y)) || (img.getRGB(x, y) & 0xFFFFFF) == ramp[0]) {
                        continue;
                    }
                    int sx = (x + ((y / cell) % 2) * (cell / 2)) % cell, sy = y % cell;
                    if (sx == cell - 1 || sy == cell - 1) {
                        img.setRGB(x, y, 0xFF000000 | ramp[1]);
                    } else if (sy == 0 && (img.getRGB(x, y) & 0xFFFFFF) != ramp[1]) {
                        img.setRGB(x, y, 0xFF000000 | ramp[4]);
                    }
                }
            }
            return img;
        }

        /** Opaque, with opaque pixels on all four sides. */
        static boolean interior(BufferedImage img, int x, int y) {
            return !clear(img.getRGB(x, y)) && !clear(img.getRGB(x - 1, y)) && !clear(img.getRGB(x + 1, y))
                    && !clear(img.getRGB(x, y - 1)) && !clear(img.getRGB(x, y + 1));
        }

        /** {@code top} with {@code below}'s metal pixels filled in wherever {@code top} is empty (the paxel: axe over pick). */
        static BufferedImage under(BufferedImage top, BufferedImage below) {
            for (int x = 0; x < top.getWidth(); x++) {
                for (int y = 0; y < top.getHeight(); y++) {
                    int p = below.getRGB(x, y);
                    if (clear(top.getRGB(x, y)) && target(p, Part.METAL)) {
                        top.setRGB(x, y, p);
                    }
                }
            }
            return top;
        }

        /**
         * Maps the brightness of each pixel in {@code part} onto {@code ramp}, spread over the brightness range those pixels
         * span; with {@code goldRim}, the recoloured pixels along the shape's top-left edges (an empty or outline pixel above
         * or to the left) turn gold, pale gold at the corners.
         */
        static BufferedImage recolor(BufferedImage src, int[] ramp, Part part, boolean goldRim) {
            int w = src.getWidth(), h = src.getHeight();
            double lo = Double.MAX_VALUE, hi = -Double.MAX_VALUE;
            for (int x = 0; x < w; x++) {
                for (int y = 0; y < h; y++) {
                    if (target(src.getRGB(x, y), part)) {
                        double l = lum(src.getRGB(x, y));
                        lo = Math.min(lo, l);
                        hi = Math.max(hi, l);
                    }
                }
            }
            BufferedImage out = new BufferedImage(w, h, BufferedImage.TYPE_INT_ARGB);
            boolean[][] recoloured = new boolean[w][h];
            double[] steps = {0.15, 0.4, 0.65, 0.85};
            for (int x = 0; x < w; x++) {
                for (int y = 0; y < h; y++) {
                    int p = src.getRGB(x, y);
                    if (!target(p, part)) {
                        out.setRGB(x, y, p);
                        continue;
                    }
                    double t = hi > lo ? (lum(p) - lo) / (hi - lo) : 1;
                    int i = 0;
                    while (i < steps.length && t >= steps[i]) {
                        i++;
                    }
                    out.setRGB(x, y, (p & 0xFF000000) | ramp[Math.min(i, ramp.length - 1)]);
                    recoloured[x][y] = true;
                }
            }
            if (goldRim) {
                for (int x = 0; x < w; x++) {
                    for (int y = 0; y < h; y++) {
                        if (!recoloured[x][y] || (out.getRGB(x, y) & 0xFFFFFF) == ramp[0]) {
                            continue;
                        }
                        boolean left = x == 0 || edge(out, recoloured, x - 1, y, ramp), up = y == 0 || edge(out, recoloured, x, y - 1, ramp);
                        if (left || up) {
                            out.setRGB(x, y, 0xFF000000 | (left && up ? PALE_GOLD : GOLD));
                        }
                    }
                }
            }
            return out;
        }

        /** The brightest pixels turn to gold glints: every one on an ore's flecks, a scatter elsewhere. */
        static BufferedImage glint(BufferedImage img, boolean ore) {
            int bright = CRIMSON[CRIMSON.length - 1];
            for (int x = 0; x < img.getWidth(); x++) {
                for (int y = 0; y < img.getHeight(); y++) {
                    if ((img.getRGB(x, y) & 0xFFFFFF) == bright && (ore || (x * 7 + y * 3) % 5 == 0)) {
                        img.setRGB(x, y, 0xFF000000 | (ore ? PALE_GOLD : GOLD));
                    }
                }
            }
            return img;
        }

        /** Sets a phlogiston stone at each (x, y) pair given, the first pixel lit; only where the texture is opaque. */
        static BufferedImage gem(BufferedImage img, int... xy) {
            for (int i = 0; i < xy.length; i += 2) {
                if (!clear(img.getRGB(xy[i], xy[i + 1]))) {
                    img.setRGB(xy[i], xy[i + 1], 0xFF000000 | (i == 0 ? PHLOGISTON_HI : PHLOGISTON));
                }
            }
            return img;
        }

        static boolean target(int p, Part part) {
            if (clear(p)) {
                return false;
            }
            int r = (p >> 16) & 255, g = (p >> 8) & 255, b = p & 255;
            boolean colourful = Math.max(r, Math.max(g, b)) - Math.min(r, Math.min(g, b)) >= CHROMA;
            return part == Part.ALL || (part == Part.FLECKS) == colourful;
        }

        /** Empty, the ramp's outline tone, or a pixel left as it was (a handle): the edge a gold rim runs along. */
        static boolean edge(BufferedImage out, boolean[][] recoloured, int x, int y, int[] ramp) {
            int p = out.getRGB(x, y);
            return clear(p) || !recoloured[x][y] || (p & 0xFFFFFF) == ramp[0];
        }

        static boolean clear(int p) {
            return (p >>> 24) < 128;
        }

        static double lum(int p) {
            return 0.3 * ((p >> 16) & 255) + 0.59 * ((p >> 8) & 255) + 0.11 * (p & 255);
        }
    }

    // ---------------------------------------------------------------- armor layers (64x32, vanilla humanoid armor UV)

    static final class Armor {
        static int writeAll(File root) throws Exception {
            BufferedImage main = humanoid();
            write(new File(root, "entity/equipment/humanoid/cowboy.png"), main);
            write(new File(root, "entity/equipment/humanoid_baby/cowboy.png"), main);
            write(new File(root, "entity/equipment/humanoid_leggings/cowboy.png"), leggings());
            write(new File(root, "entity/equipment/humanoid/cowboy_hat.png"), cowboyHat());
            BufferedImage snake = snakeskinBoots();
            write(new File(root, "entity/equipment/humanoid/snakeskin.png"), snake);
            write(new File(root, "entity/equipment/humanoid_baby/snakeskin.png"), snake);
            // The asset declares a leggings layer too; boots never use it, so it stays empty.
            write(new File(root, "entity/equipment/humanoid_leggings/snakeskin.png"), new BufferedImage(64, 32, BufferedImage.TYPE_INT_ARGB));
            return 7;
        }

        // The worn Cowboy Hat (CowboyHatModel, 64x64): felt crown with a dark band and a brass buckle at the front, a shadow
        // down the crease between the lobes, and a brim (flat middle, curled sides) lighter at the edge, darker underneath.
        static BufferedImage cowboyHat() {
            BufferedImage img = new BufferedImage(64, 64, BufferedImage.TYPE_INT_ARGB);
            Painter p = new Painter(img, new Random(34));
            int felt = 0x6E4C2E, band = 0x2E2014, buckle = 0xC9A227;
            Box crown = new Box(0, 0, 8, 4, 8);
            for (Rect r : new Rect[] {crown.top(), crown.bottom(), crown.east(), crown.front(), crown.west(), crown.back()}) p.face(r, felt, 6);
            for (Rect r : new Rect[] {crown.east(), crown.front(), crown.west(), crown.back()}) p.row(r, 3, band);
            p.set(crown.front().x() + 3, crown.front().y() + 3, buckle);
            p.set(crown.front().x() + 4, crown.front().y() + 3, buckle);
            Rect top = crown.top();
            for (int y = 1; y < top.h() - 1; y++) {
                p.set(top.x() + 3, top.y() + y, shade(felt, 0.7));
                p.set(top.x() + 4, top.y() + y, shade(felt, 0.7));
            }
            Box lobe = new Box(32, 0, 3, 1, 6);
            for (Rect r : new Rect[] {lobe.bottom(), lobe.east(), lobe.front(), lobe.west(), lobe.back()}) p.face(r, felt, 6);
            p.face(lobe.top(), shade(felt, 1.08), 5);
            for (Box brim : new Box[] {new Box(0, 12, 10, 1, 16), new Box(0, 30, 3, 1, 14)}) {
                p.face(brim.top(), felt, 6);
                p.outline(brim.top(), shade(felt, 1.18));
                p.face(brim.bottom(), shade(felt, 0.8), 4);
                for (Rect r : new Rect[] {brim.east(), brim.front(), brim.west(), brim.back()}) p.face(r, shade(felt, 1.1), 4);
            }
            return img;
        }

        // Snakeskin Boots: only the boot part of the leg box, diamondback hide over a dark leather cuff and sole.
        static BufferedImage snakeskinBoots() {
            BufferedImage img = new BufferedImage(64, 32, BufferedImage.TYPE_INT_ARGB);
            Painter p = new Painter(img, new Random(33));
            int hide = Snake.TAN, diamond = Snake.DARK, edge = Snake.EDGE, sole = shade(Items.DARK_LEATHER, 0.7);
            Box leg = new Box(0, 16, 4, 12, 4);
            for (Rect side : new Rect[] {leg.front(), leg.back(), leg.east(), leg.west()}) {
                p.face(new Rect(side.x, side.y + 7, side.w, 5), hide, 8);
                p.row(side, 7, shade(Items.DARK_LEATHER, 1.2)); // cuff
                for (int y = 8; y < 11; y++) {
                    for (int x = 0; x < side.w; x++) {
                        int k = Math.floorMod(x + y, 4);
                        if (k == 0) p.set(side.x + x, side.y + y, diamond);
                        else if (k == 2 && (x + y) % 8 == 2) p.set(side.x + x, side.y + y, edge);
                    }
                }
                p.row(side, 11, sole);
            }
            p.set(leg.back().x + 1, leg.back().y + 11, 0xC8CCD2); // spur
            p.face(leg.bottom(), shade(sole, 0.8), 4);
            return img;
        }

        // Helmet = hat crown + band on the head box (lower face left clear); chest = duster; boots on the legs.
        static BufferedImage humanoid() {
            BufferedImage img = new BufferedImage(64, 32, BufferedImage.TYPE_INT_ARGB);
            Painter p = new Painter(img, new Random(31));
            int hat = 0x5A3A22, band = 0x2A1A10, coat = Items.LEATHER, lapel = 0xA87850, belt = 0x2A1E14, buckle = 0xC9A227;
            int boot = Items.DARK_LEATHER;
            Box head = new Box(0, 0, 8, 8, 8);
            p.face(head.top(), hat, 6);
            for (Rect side : new Rect[] {head.front(), head.back(), head.east(), head.west()}) {
                p.face(new Rect(side.x, side.y, side.w, 3), hat, 6);
                p.row(side, 3, band);
            }
            Box hatLayer = new Box(32, 0, 8, 8, 8); // outer layer: brim edge
            for (Rect side : new Rect[] {hatLayer.front(), hatLayer.back(), hatLayer.east(), hatLayer.west()}) {
                p.row(side, 4, shade(hat, 0.85));
            }
            Box body = new Box(16, 16, 8, 12, 4);
            for (Rect side : new Rect[] {body.front(), body.back(), body.east(), body.west()}) p.face(side, coat, 8);
            Rect f = body.front();
            for (int y = 0; y < 6; y++) { p.set(f.x + 3, f.y + y, CLEAR); p.set(f.x + 4, f.y + y, CLEAR); }
            for (int y = 0; y < 5; y++) { p.set(f.x + 2, f.y + y, lapel); p.set(f.x + 5, f.y + y, lapel); }
            for (Rect side : new Rect[] {body.front(), body.back(), body.east(), body.west()}) p.row(side, 8, belt);
            p.set(f.x + 3, f.y + 8, buckle); p.set(f.x + 4, f.y + 8, buckle);
            p.face(body.top(), shade(coat, 1.1), 4);
            Box arm = new Box(40, 16, 4, 12, 4);
            for (Rect side : new Rect[] {arm.front(), arm.back(), arm.east(), arm.west()}) {
                p.face(new Rect(side.x, side.y, side.w, 10), coat, 8);
                p.row(side, 9, shade(coat, 0.7));
            }
            p.face(arm.top(), shade(coat, 1.1), 4);
            Box leg = new Box(0, 16, 4, 12, 4);
            for (Rect side : new Rect[] {leg.front(), leg.back(), leg.east(), leg.west()}) {
                p.face(new Rect(side.x, side.y + 7, side.w, 5), boot, 6);
                p.row(side, 7, shade(boot, 1.4));
            }
            p.set(leg.back().x + 1, leg.back().y + 11, 0xC8CCD2); // spur
            p.face(leg.bottom(), shade(boot, 0.7), 4);
            return img;
        }

        // Chaps: leather legs with fringe on the outer edge, belt on the waist.
        static BufferedImage leggings() {
            BufferedImage img = new BufferedImage(64, 32, BufferedImage.TYPE_INT_ARGB);
            Painter p = new Painter(img, new Random(32));
            int leather = Items.LEATHER, belt = 0x2A1E14, buckle = 0xC9A227;
            Box body = new Box(16, 16, 8, 12, 4);
            for (Rect side : new Rect[] {body.front(), body.back(), body.east(), body.west()}) {
                p.face(new Rect(side.x, side.y + 8, side.w, 4), leather, 6);
                p.row(side, 8, belt);
            }
            p.set(body.front().x + 3, body.front().y + 8, buckle);
            p.set(body.front().x + 4, body.front().y + 8, buckle);
            Box leg = new Box(0, 16, 4, 12, 4);
            for (Rect side : new Rect[] {leg.front(), leg.back(), leg.east(), leg.west()}) {
                p.face(new Rect(side.x, side.y, side.w, 10), leather, 8);
            }
            for (int y = 0; y < 10; y += 2) p.set(leg.east().x, leg.east().y + y, shade(leather, 1.35)); // fringe
            for (int y = 1; y < 10; y += 2) p.set(leg.east().x + 1, leg.east().y + y, shade(leather, 1.2));
            p.face(leg.top(), leather, 4);
            return img;
        }
    }

    // ---------------------------------------------------------------- helpers

    record Rect(int x, int y, int w, int h) {}

    /** Java/Bedrock box UV: top, bottom, then east|north(front)|west|south(back) strip. */
    record Box(int u, int v, int w, int h, int d) {
        Rect top() { return new Rect(u + d, v, w, d); }
        Rect bottom() { return new Rect(u + d + w, v, w, d); }
        Rect east() { return new Rect(u, v + d, d, h); }
        Rect front() { return new Rect(u + d, v + d, w, h); }
        Rect west() { return new Rect(u + d + w, v + d, d, h); }
        Rect back() { return new Rect(u + 2 * d + w, v + d, w, h); }
    }

    static int shade(int rgb, double f) {
        int r = clamp((int) (((rgb >> 16) & 0xFF) * f));
        int g = clamp((int) (((rgb >> 8) & 0xFF) * f));
        int b = clamp((int) ((rgb & 0xFF) * f));
        return (r << 16) | (g << 8) | b;
    }

    static int clamp(int v) {
        return Math.max(0, Math.min(255, v));
    }

    static final class Painter {
        final BufferedImage img;
        final Random rnd;

        Painter(BufferedImage img, Random rnd) {
            this.img = img;
            this.rnd = rnd;
        }

        void set(int x, int y, int rgb) {
            img.setRGB(x, y, rgb == CLEAR ? CLEAR : 0xFF000000 | rgb);
        }

        /** Fill with per-pixel brightness noise of +/- noise for a hand-drawn look. */
        void face(Rect r, int rgb, int noise) {
            for (int x = r.x; x < r.x + r.w; x++)
                for (int y = r.y; y < r.y + r.h; y++) {
                    int n = noise == 0 ? 0 : rnd.nextInt(noise * 2 + 1) - noise;
                    int c = (clamp(((rgb >> 16) & 0xFF) + n) << 16) | (clamp(((rgb >> 8) & 0xFF) + n) << 8) | clamp((rgb & 0xFF) + n);
                    set(x, y, c);
                }
        }

        void row(Rect r, int y, int rgb) {
            for (int x = r.x; x < r.x + r.w; x++) set(x, r.y + y, rgb);
        }

        void outline(Rect r, int rgb) {
            for (int x = r.x; x < r.x + r.w; x++) { set(x, r.y, rgb); set(x, r.y + r.h - 1, rgb); }
            for (int y = r.y; y < r.y + r.h; y++) { set(r.x, y, rgb); set(r.x + r.w - 1, y, rgb); }
        }
    }

    // ---------------------------------------------------------------- phase 8: the airship and the polar expedition

    /**
     * The airship's 128x64 material atlas (swatches must match tools/models/AirshipModels.java), its parts and item, the
     * Polar Compass (32 needle frames like a vanilla compass: frame i points 360*i/32 degrees clockwise from straight up),
     * the Polar Resonator, and the Polar Gateway's frame stones (dark and lit) and portal sheet.
     */
    /** The Hollow Earth: the crust, its shell and the inner sun (9a); fern fronds, ash and ash vents (9b1). */
    static final class Hollow {
        static final int CRUST = 0xB89A62, SHELL = 0x5E4A36, SUN_CORE = 0xFFF8E0, SUN_RIM = 0xFFD678;
        // 9b1: fern fronds (matching the Jungle's foliage), ash and the vents
        static final int FROND = 0x3E8E32, FROND_RIB = 0x245A1E, FROND_TIP = 0x6CB04A;
        static final int ASH = 0x6E6A66, BASALT = 0x3A3638, EMBER = 0xD8561A, EMBER_HOT = 0xFFB040;

        static int writeAll(File root) throws Exception {
            int n = 0;
            n += Tiles.put(root, "crust_rock", crust(9101));
            n += Tiles.put(root, "shell_bedrock", shell(9102));
            n += Tiles.put(root, "fern_frond", frond(9111));
            n += Tiles.put(root, "ash", ash(9112));
            n += Tiles.put(root, "ash_vent_top", ventTop(9113));
            n += Tiles.put(root, "ash_vent_side", ventSide(9114));
            n += Tiles.put(root, "ash_vent_bottom", Tiles.noise(BASALT, 8, 9115));
            write(new File(root, "environment/inner_sun.png"), sun());
            write(new File(root, "environment/inner_sun_halo.png"), halo());
            return n + 2;
        }

        /**
         * Crust rock: warm sandstone gold, layered like the strata of a cliff (the crust was laid down in sheets). The beds
         * are grainy (two tones dithered together), parted by broken dark bedding lines, and they sag and rise a pixel as they
         * cross the block; a few pale pebbles and dark pits sit in them. The beds sum to 16 rows, so the tile repeats.
         */
        static BufferedImage crust(long seed) {
            BufferedImage img = Tiles.blank();
            Random r = new Random(seed);
            int[] beds = {5, 4, 7};
            double[] tones = {1.0, 0.92, 1.05};
            int[] bed = new int[16], row = new int[16];
            for (int b = 0, y = 0; b < beds.length; b++) {
                for (int i = 0; i < beds[b]; i++, y++) {
                    bed[y] = b;
                    row[y] = i;
                }
            }
            for (int x = 0; x < 16; x++) {
                int sag = (int) Math.round(Math.sin(2 * Math.PI * x / 16));
                for (int y = 0; y < 16; y++) {
                    int sy = Math.floorMod(y - sag, 16), b = bed[sy];
                    double t = tones[b] * (r.nextInt(3) == 0 ? 0.9 : 1.0);
                    if (row[sy] == 0 && r.nextInt(5) != 0) t = 0.78; // the bedding line
                    else if (row[sy] == 1) t *= 1.06;
                    Tiles.px(img, x, y, Tiles.jitter(shade(CRUST, t), r, 3));
                }
            }
            for (int i = 0; i < 6; i++) Tiles.px(img, r.nextInt(16), r.nextInt(16), Tiles.jitter(0xF0E2B8, r, 5));
            for (int i = 0; i < 4; i++) Tiles.px(img, r.nextInt(16), r.nextInt(16), shade(CRUST, 0.62));
            return img;
        }

        /**
         * Shell bedrock: the crust's dark, fused cap, broken into chunky shards like vanilla bedrock but brown-gold. The
         * shards are cells round scattered points (wrapping, so the tile repeats), each its own shade, lit along its top-left
         * edges and cracked dark along its bottom-right; a few flecks of fused gold.
         */
        static BufferedImage shell(long seed) {
            Random r = new Random(seed);
            int cells = 12;
            double[][] pts = new double[cells][2];
            double[] tones = new double[cells];
            double[] palette = {0.5, 0.7, 0.9, 1.15, 1.4};
            for (int i = 0; i < cells; i++) {
                pts[i] = new double[] {r.nextDouble() * 16, r.nextDouble() * 16};
                tones[i] = palette[r.nextInt(palette.length)];
            }
            int[][] id = new int[16][16];
            for (int x = 0; x < 16; x++) {
                for (int y = 0; y < 16; y++) {
                    double best = Double.MAX_VALUE;
                    for (int i = 0; i < cells; i++) {
                        double dx = Math.abs(x + 0.5 - pts[i][0]), dy = Math.abs(y + 0.5 - pts[i][1]);
                        dx = Math.min(dx, 16 - dx);
                        dy = Math.min(dy, 16 - dy);
                        if (dx * dx + dy * dy < best) {
                            best = dx * dx + dy * dy;
                            id[x][y] = i;
                        }
                    }
                }
            }
            BufferedImage img = Tiles.blank();
            for (int x = 0; x < 16; x++) {
                for (int y = 0; y < 16; y++) {
                    int c = id[x][y];
                    boolean lit = id[(x + 15) % 16][y] != c || id[x][(y + 15) % 16] != c;
                    boolean crack = id[(x + 1) % 16][y] != c || id[x][(y + 1) % 16] != c;
                    double t = crack ? 0.38 : lit ? tones[c] * 1.25 : tones[c];
                    Tiles.px(img, x, y, Tiles.jitter(shade(SHELL, t), r, 4));
                }
            }
            for (int i = 0; i < 3; i++) Tiles.px(img, r.nextInt(16), r.nextInt(16), 0xC8A050);
            return img;
        }

        /**
         * Fern frond (9b1, a leaves texture: the gaps are see-through): fronds crossing the block, each a dark curved midrib
         * with paired leaflets that shorten toward its tip, lit on one side and shaded on the other; a darker frond lies
         * underneath to fill the gaps a little. Drawn wrapping, so the tile repeats.
         */
        static BufferedImage frond(long seed) {
            BufferedImage img = Tiles.blank();
            Random r = new Random(seed);
            double[][] fronds = {{9, -1, -0.35, 1, 0.6}, {7, 6, 1, 0.5, 0.6}, {-2, 2, 1, 0.45, 1}, {4, 9, 1, -0.4, 1}};
            for (double[] fr : fronds) {
                double len = Math.hypot(fr[2], fr[3]), ux = fr[2] / len, uy = fr[3] / len, shade = fr[4];
                for (int s = 0; s < 15; s++) {
                    double bend = s * s * 0.012;
                    double mx = fr[0] + ux * s - uy * bend, my = fr[1] + uy * s + ux * bend;
                    Tiles.px(img, Math.floorMod((int) Math.round(mx), 16), Math.floorMod((int) Math.round(my), 16), shade(FROND_RIB, shade));
                    if (s % 2 != 0) continue;
                    int reach = (int) Math.round(4 - s * 0.2);
                    for (int side = -1; side <= 1; side += 2) {
                        for (int k = 1; k <= reach; k++) {
                            double lx = mx - uy * side * k + ux * k * 0.4, ly = my + ux * side * k + uy * k * 0.4;
                            int c = k == reach ? FROND_TIP : side < 0 ? FROND : shade(FROND, 0.8);
                            Tiles.px(img, Math.floorMod((int) Math.round(lx), 16), Math.floorMod((int) Math.round(ly), 16), Tiles.jitter(shade(c, shade), r, 2));
                        }
                    }
                }
            }
            return img;
        }

        /**
         * Ash: soft mid-grey laid down in drifts (pale crests and shadowed troughs running across the block), with a few
         * black cinders and pale flakes. The drifts' period divides 16, so the tile repeats.
         */
        static BufferedImage ash(long seed) {
            BufferedImage img = Tiles.blank();
            Random r = new Random(seed * 5);
            for (int x = 0; x < 16; x++) {
                for (int y = 0; y < 16; y++) {
                    double w = Math.sin(2 * Math.PI * (x * 0.5 + y) / 8);
                    double t = w > 0.7 ? 1.09 : w > 0.2 ? 1.03 : w < -0.7 ? 0.9 : 0.97;
                    Tiles.px(img, x, y, Tiles.jitter(shade(ASH, t), r, 4));
                }
            }
            for (int i = 0; i < 6; i++) Tiles.px(img, r.nextInt(16), r.nextInt(16), Tiles.jitter(0x2A2624, r, 4));
            for (int i = 0; i < 4; i++) Tiles.px(img, r.nextInt(16), r.nextInt(16), Tiles.jitter(0xB8B2AC, r, 4));
            return img;
        }

        /** The vent's top: dark basalt round a ragged crack glowing orange from below, dusted with ash at the edges. */
        static BufferedImage ventTop(long seed) {
            BufferedImage img = Tiles.noise(BASALT, 8, seed);
            Random r = new Random(seed * 3);
            for (int x = 0; x < 16; x++) {
                for (int y = 0; y < 16; y++) {
                    double d = Math.hypot(x - 7.5, y - 7.5);
                    if (d < 3.2 + r.nextDouble() * 0.8) {
                        Tiles.px(img, x, y, d < 1.8 ? Tiles.jitter(EMBER_HOT, r, 12) : Tiles.jitter(EMBER, r, 14));
                    } else if (d < 4.6) {
                        Tiles.px(img, x, y, Tiles.jitter(0x1A1616, r, 4));
                    } else if (d > 6.5 && r.nextInt(3) == 0) {
                        Tiles.px(img, x, y, Tiles.jitter(ASH, r, 8));
                    }
                }
            }
            // Cracks running out from the hole
            for (int c = 0; c < 4; c++) {
                double angle = c * Math.PI / 2 + r.nextDouble() * 0.8;
                for (int s = 4; s < 8; s++) Tiles.px(img, 8 + (int) Math.round(Math.cos(angle) * s), 8 + (int) Math.round(Math.sin(angle) * s), Tiles.jitter(EMBER, r, 20));
            }
            return img;
        }

        /** Its sides: basalt in rough vertical streaks, ash settled along the top, a glint of heat in the seams. */
        static BufferedImage ventSide(long seed) {
            BufferedImage img = Tiles.noise(BASALT, 8, seed);
            Random r = new Random(seed * 11);
            for (int x = 0; x < 16; x += 2 + r.nextInt(3)) {
                for (int y = 0; y < 16; y++) if (r.nextInt(4) != 0) Tiles.px(img, x, y, Tiles.jitter(shade(BASALT, 0.7), r, 4));
            }
            for (int x = 0; x < 16; x++) {
                Tiles.px(img, x, 0, Tiles.jitter(ASH, r, 8));
                if (r.nextBoolean()) Tiles.px(img, x, 1, Tiles.jitter(ASH, r, 10));
            }
            for (int i = 0; i < 3; i++) Tiles.px(img, r.nextInt(16), 3 + r.nextInt(12), Tiles.jitter(EMBER, r, 16));
            return img;
        }

        /** The inner sun: a white-gold disc with a soft, slightly ragged rim (drawn emissive, faded by the haze). */
        static BufferedImage sun() {
            int size = 64;
            BufferedImage img = new BufferedImage(size, size, BufferedImage.TYPE_INT_ARGB);
            Random r = new Random(9103);
            for (int x = 0; x < size; x++) {
                for (int y = 0; y < size; y++) {
                    double d = Math.hypot(x + 0.5 - size / 2.0, y + 0.5 - size / 2.0) / (size / 2.0);
                    if (d > 1.0) continue;
                    double edge = d < 0.72 ? 1.0 : Math.max(0.0, 1.0 - (d - 0.72) / 0.28);
                    edge = edge * edge * (3 - 2 * edge);
                    int rgb = lerp(SUN_CORE, SUN_RIM, Math.min(1.0, Math.max(0.0, (d - 0.35) / 0.6)));
                    int a = (int) Math.round(255 * edge * (0.96 + r.nextDouble() * 0.04));
                    img.setRGB(x, y, (a << 24) | rgb);
                }
            }
            return img;
        }

        /** Its halo: a broad, faint glow round the disc, fading to nothing well out from it. */
        static BufferedImage halo() {
            int size = 128;
            BufferedImage img = new BufferedImage(size, size, BufferedImage.TYPE_INT_ARGB);
            for (int x = 0; x < size; x++) {
                for (int y = 0; y < size; y++) {
                    double d = Math.hypot(x + 0.5 - size / 2.0, y + 0.5 - size / 2.0) / (size / 2.0);
                    if (d >= 1.0) continue;
                    double glow = Math.pow(1.0 - d, 2.2);
                    img.setRGB(x, y, ((int) Math.round(150 * glow) << 24) | lerp(0xFFF0C0, 0xFFC860, d));
                }
            }
            return img;
        }

        static int lerp(int a, int b, double t) {
            int r = (int) Math.round(((a >> 16) & 0xFF) * (1 - t) + ((b >> 16) & 0xFF) * t);
            int g = (int) Math.round(((a >> 8) & 0xFF) * (1 - t) + ((b >> 8) & 0xFF) * t);
            int bl = (int) Math.round((a & 0xFF) * (1 - t) + (b & 0xFF) * t);
            return (r << 16) | (g << 8) | bl;
        }
    }

    static final class Expedition {
        static final int CANVAS = 0xE6DAC0, RED = 0xA8322A, BRASS = Items.BRASS, IRON = 0x4A4E54, ROPE = 0xC8A870;
        static final int WOOD = 0x8A5A32, WICKER = 0xB08850, VARNISH = 0x5A3418;
        static final int STONE = 0x3E4A5A, RUNE = 0x26303C, GLOW = 0x7FF0FF, ICE = 0xD8EEF4, AMETHYST = 0x9A5CC6;

        static int writeAll(File root) throws Exception {
            int n = 0;
            write(new File(root, "entity/airship.png"), atlas());
            n++;
            n += Items.put(root, "airship_envelope", envelopeItem());
            n += Items.put(root, "airship_engine", engineItem());
            n += Items.put(root, "airship", airshipItem());
            n += Items.put(root, "polar_resonator", resonatorItem());
            for (int i = 0; i < 32; i++) {
                n += Items.put(root, String.format(java.util.Locale.ROOT, "polar_compass_%02d", i), compass(i));
            }
            n += Tiles.put(root, "gateway_frame_side", frameSide(false));
            n += Tiles.put(root, "gateway_frame_side_active", frameSide(true));
            n += Tiles.put(root, "gateway_frame_top", frameTop(false));
            n += Tiles.put(root, "gateway_frame_top_active", frameTop(true));
            n += Tiles.put(root, "gateway_frame_bottom", Tiles.noise(shade(STONE, 0.85), 5, 81));
            n += Tiles.put(root, "polar_portal", portal());
            java.nio.file.Files.writeString(new File(root, "block/polar_portal.png.mcmeta").toPath(), "{\"animation\": {\"frametime\": 3, \"interpolate\": true}}\n");
            write(new File(root, "misc/polar_frost.png"), frost());
            n++;
            return n;
        }

        /**
         * The screen overlay while a gateway charges (stretched over the whole screen, faded in by the charge): frost
         * thick at the edges and clear in the middle, with feathery ice crystals growing in from the edges.
         */
        static BufferedImage frost() {
            int size = 256;
            BufferedImage img = new BufferedImage(size, size, BufferedImage.TYPE_INT_ARGB);
            Random r = new Random(1818);
            for (int x = 0; x < size; x++) {
                for (int y = 0; y < size; y++) {
                    double edge = Math.min(Math.min(x, size - 1 - x), Math.min(y, size - 1 - y)) / (size / 2.0);
                    double fog = edge < 0.4 ? Math.pow(1 - edge / 0.4, 1.8) : 0;
                    int alpha = (int) (fog * 190) + (fog > 0 ? r.nextInt(18) : 0);
                    if (alpha > 0) {
                        img.setRGB(x, y, (Math.min(255, alpha) << 24) | Tiles.jitter(0xDDF2FF, r, 8));
                    }
                }
            }
            // Crystals: branching walkers from the edges toward the middle
            for (int c = 0; c < 70; c++) {
                int side = r.nextInt(4);
                double px = side == 0 ? 0 : side == 1 ? size - 1 : r.nextInt(size);
                double py = side == 2 ? 0 : side == 3 ? size - 1 : r.nextInt(size);
                double angle = Math.atan2(size / 2.0 - py, size / 2.0 - px) + (r.nextDouble() - 0.5) * 1.2;
                crystal(img, r, px, py, angle, 20 + r.nextInt(45), 3);
            }
            return img;
        }

        private static void crystal(BufferedImage img, Random r, double x, double y, double angle, int length, int depth) {
            for (int i = 0; i < length; i++) {
                x += Math.cos(angle);
                y += Math.sin(angle);
                int px = (int) x, py = (int) y;
                if (px < 0 || py < 0 || px >= img.getWidth() || py >= img.getHeight()) {
                    return;
                }
                int alpha = 235 - i * 140 / length;
                img.setRGB(px, py, (alpha << 24) | 0xF8FCFF);
                if (depth > 0 && i > 3 && r.nextInt(7) == 0) {
                    crystal(img, r, x, y, angle + (r.nextBoolean() ? 0.8 : -0.8), length / 3, depth - 1);
                }
            }
        }

        // ------------------------------------------------ the airship atlas

        static BufferedImage atlas() {
            BufferedImage img = new BufferedImage(128, 64, BufferedImage.TYPE_INT_ARGB);
            Random r = new Random(1903);
            Painter p = new Painter(img, r);
            p.face(new Rect(0, 0, 128, 64), shade(IRON, 0.7), 4);  // unused corners: dark, so a stray UV shows as soot
            // CANVAS 0,0 32x32: doped canvas, vertical seams every 8, a few weather stains
            p.face(new Rect(0, 0, 32, 32), CANVAS, 5);
            for (int x = 7; x < 32; x += 8) for (int y = 0; y < 32; y++) p.set(x, y, shade(CANVAS, 0.86));
            stains(p, r, 0, 0, 32, 32, 3);
            // PANELS 32,0 64x32: the envelope's gores run along the ship (horizontal seams), with stitching
            p.face(new Rect(32, 0, 64, 32), CANVAS, 5);
            for (int y = 7; y < 32; y += 8) {
                for (int x = 32; x < 96; x++) {
                    p.set(x, y, shade(CANVAS, 0.84));
                    if (x % 3 == 0) p.set(x, y - 1, shade(CANVAS, 0.93));
                }
            }
            for (int y = 0; y < 32; y++) p.set(32 + 31, y, shade(CANVAS, 0.9)); // a cross seam halfway
            stains(p, r, 32, 0, 64, 32, 5);
            // RED 96,0: painted fin canvas
            p.face(new Rect(96, 0, 16, 16), RED, 6);
            for (int x = 96; x < 112; x++) p.set(x, 15, shade(RED, 0.7));
            // BRASS 112,0: riveted sheet
            p.face(new Rect(112, 0, 16, 16), tone(BRASS, 4), 5);
            for (int i = 0; i < 16; i++) { p.set(112 + i, 0, tone(BRASS, 6)); p.set(112 + i, 15, tone(BRASS, 2)); }
            for (int[] c : new int[][] {{114, 3}, {125, 3}, {114, 12}, {125, 12}}) { p.set(c[0], c[1], tone(BRASS, 6)); p.set(c[0] + 1, c[1] + 1, tone(BRASS, 2)); }
            // WOOD 96,16 32x16: deck planks along the ship
            p.face(new Rect(96, 16, 32, 16), WOOD, 6);
            for (int y = 16; y < 32; y += 4) for (int x = 96; x < 128; x++) p.set(x, y, shade(WOOD, 0.7));
            for (int k = 0; k < 6; k++) { int x = 96 + r.nextInt(32), y = 17 + r.nextInt(14); p.set(x, y, shade(WOOD, 0.8)); }
            // IRON 0,32: dark iron with rivets
            p.face(new Rect(0, 32, 16, 16), IRON, 5);
            for (int[] c : new int[][] {{2, 34}, {13, 34}, {2, 45}, {13, 45}}) p.set(c[0], c[1], shade(IRON, 1.5));
            // ROPE 16,32: twisted hemp
            p.face(new Rect(16, 32, 16, 16), ROPE, 4);
            for (int y = 32; y < 48; y++) for (int x = 16; x < 32; x++) if ((x + y) % 4 == 0) p.set(x, y, shade(ROPE, 0.72));
            // WICKER 32,32 32x16: woven cane, 2x2 over-and-under
            for (int y = 32; y < 48; y++) {
                for (int x = 32; x < 64; x++) {
                    boolean over = ((x / 2) + (y / 2)) % 2 == 0;
                    int c = over ? WICKER : shade(WICKER, 0.72);
                    p.set(x, y, (x + y) % 5 == 0 ? shade(c, 1.12) : c);
                }
            }
            for (int x = 32; x < 64; x++) { p.set(x, 32, shade(WICKER, 0.55)); p.set(x, 47, shade(WICKER, 0.55)); }
            // PROP 64,32: varnished wood
            p.face(new Rect(64, 32, 16, 16), VARNISH, 5);
            for (int y = 33; y < 48; y += 3) for (int x = 64; x < 80; x++) if (r.nextInt(3) != 0) p.set(x, y, shade(VARNISH, 1.25));
            // FIREBOX 80,32: an iron door with the fire showing through the grille
            p.face(new Rect(80, 32, 16, 16), IRON, 4);
            for (int y = 37; y < 44; y++) for (int x = 83; x < 93; x++) p.set(x, y, (x % 2 == 0) ? 0xF0A030 : 0xC85020);
            p.outline(new Rect(82, 36, 12, 9), shade(IRON, 0.6));
            return img;
        }

        static void stains(Painter p, Random r, int x0, int y0, int w, int h, int count) {
            for (int s = 0; s < count; s++) {
                int cx = x0 + 2 + r.nextInt(w - 4), cy = y0 + 2 + r.nextInt(h - 4);
                for (int dx = -1; dx <= 1; dx++) for (int dy = -1; dy <= 1; dy++) if (r.nextInt(3) != 0) p.set(cx + dx, cy + dy, shade(CANVAS, 0.9));
            }
        }

        // ------------------------------------------------ items

        /** The envelope: a canvas cigar sewn in gores, a red belly band, a brass nose cap, red tail fins, rigging gathered below. */
        static BufferedImage envelopeItem() {
            BufferedImage img = new BufferedImage(16, 16, BufferedImage.TYPE_INT_ARGB);
            int outline = 0x4A3C2C;
            boolean[][] body = new boolean[16][16];
            // tail fins first, so the envelope overlaps their roots
            draw(img, 0, 0, RED, pal('k', outline, 'y', BRASS),
                    "",
                    "             kk",
                    "            kyk",
                    "           k45k",
                    "", "", "", "", "", "",
                    "           k43k",
                    "            kyk",
                    "             kk");
            for (int x = 0; x < 16; x++)
                for (int y = 0; y < 16; y++) {
                    double dx = x + 0.5 - 7.5, dy = y + 0.5 - 7.0;
                    body[x][y] = (dx / 6.9) * (dx / 6.9) + (dy / 4.0) * (dy / 4.0) <= 1;
                }
            for (int x = 0; x < 16; x++)
                for (int y = 0; y < 16; y++) {
                    if (!body[x][y]) continue;
                    boolean edge = x == 0 || x == 15 || !body[x - 1][y] || !body[x + 1][y] || !body[x][y - 1] || !body[x][y + 1];
                    int level = y <= 4 ? 6 : y <= 6 ? 5 : y <= 8 ? 4 : 3;
                    int c = edge ? outline : x <= 1 ? tone(BRASS, level) : y == 7 ? tone(RED, level == 4 ? 4 : 5) : tone(CANVAS, x % 3 == 2 ? level - 1 : level);
                    img.setRGB(x, y, 0xFF000000 | c);
                }
            draw(img, 0, 0, ROPE, pal('k', outline, 'y', BRASS),
                    "", "", "", "", "", "", "", "", "", "", "", "",
                    "     k    k",
                    "      k  k",
                    "       yy",
                    "       kk");
            return img;
        }

        static BufferedImage engineItem() {
            BufferedImage img = sprite(BRASS, pal('k', 0x2A2A2E, 'c', Items.COPPER, 'f', 0xF0A030, 'F', 0xC85020, 'i', IRON),
                    "      ii",
                    "      ii",
                    "    666655",
                    "   65555554",
                    "   5iiiiii4",
                    "   5kfFfFk4",
                    "   5kFfFfk4",
                    "   5iiiiii3",
                    " cc44444443cc",
                    " cc44444443cc",
                    "   43333332",
                    "   33333322",
                    "    222222");
            Items.gearOnto(img, 12.5, 12.5, 3.4, 2.6, 1.0, BRASS);
            return img;
        }

        static BufferedImage airshipItem() {
            return sprite(CANVAS, pal('y', BRASS, 'r', RED, 'w', WICKER, 'W', shade(WICKER, 0.7), 'k', 0x2A2A2E, 'v', VARNISH),
                    "",
                    "     55555   r",
                    "   5566666655rr",
                    "  556666666655r",
                    " 5y555555555y55",
                    " 4rrrrrrrrrrrr4r",
                    " 4y444444444y4",
                    "  33333333333r",
                    "    2222222",
                    "    k  k  k",
                    "    k  k  k",
                    "   wwwwwwwwwk v",
                    "   WWWWWWWWWkvv",
                    "    WWWWWWW   v",
                    "",
                    "");
        }

        static BufferedImage resonatorItem() {
            BufferedImage img = sprite(BRASS, pal('a', AMETHYST, 'A', shade(AMETHYST, 1.35), 'm', shade(AMETHYST, 0.65), 'k', 0x2A2A2E, 'c', Items.COPPER, 'g', 0xE8E0D0),
                    "    A     A",
                    "    a     a",
                    "    aA   Aa",
                    "    ma   am",
                    "     ma am",
                    "      mam",
                    "       a",
                    "      kkk",
                    "    6665555",
                    "   655g55554",
                    "  c55gkg5554c",
                    "  c555g55554c",
                    "   44444444",
                    "   33333332",
                    "    222222");
            return img;
        }

        static BufferedImage compass(int frame) {
            BufferedImage img = new BufferedImage(16, 16, BufferedImage.TYPE_INT_ARGB);
            double cx = 8, cy = 8;
            for (int x = 0; x < 16; x++) {
                for (int y = 0; y < 16; y++) {
                    double dx = x + 0.5 - cx, dy = y + 0.5 - cy, d = Math.hypot(dx, dy);
                    int c;
                    if (d <= 5.2) {
                        c = (x + y) % 7 == 0 ? shade(ICE, 0.94) : ICE;                       // icy dial
                    } else if (d <= 6.4) {
                        c = dx + dy < -2 ? tone(BRASS, 6) : dx + dy > 2 ? tone(BRASS, 3) : tone(BRASS, 4); // brass bezel
                    } else if (d <= 7.2) {
                        c = tone(BRASS, 2);                                                  // rim
                    } else {
                        continue;
                    }
                    img.setRGB(x, y, 0xFF000000 | c);
                }
            }
            // N/E/S/W ticks
            for (int[] t : new int[][] {{7, 3}, {8, 3}, {12, 7}, {12, 8}, {7, 12}, {8, 12}, {3, 7}, {3, 8}}) img.setRGB(t[0], t[1], 0xFF000000 | 0x8AA8B8);
            needle(img, frame, 4, 3, 0xC02828, 0xFF5A4A, 0x2A3440);
            for (int[] h : new int[][] {{7, 7}, {8, 7}, {7, 8}, {8, 8}}) img.setRGB(h[0], h[1], 0xFF000000 | tone(BRASS, h[0] + h[1] == 14 ? 6 : h[0] + h[1] == 16 ? 3 : 4));
            return img;
        }

        // ------------------------------------------------ the gateway

        /** Ancient blue-grey stone with frost at the edges and a band of carved runes, glowing when the gateway is open. */
        static BufferedImage frameSide(boolean active) {
            BufferedImage img = Tiles.noise(STONE, 5, 77);
            Random r = new Random(78);
            for (int x = 0; x < 16; x++) {
                if (r.nextInt(2) == 0) Tiles.px(img, x, 0, 0xC8DCE8);
                Tiles.px(img, x, 5, shade(STONE, 0.7));
                Tiles.px(img, x, 10, shade(STONE, 0.7));
                Tiles.px(img, x, 15, shade(STONE, 0.75));
            }
            int rune = active ? GLOW : RUNE;
            String[] glyphs = {
                    " x  xx   x x  x ",
                    " xx  x  xxx  xx ",
                    " x  xx   x  x x "};
            for (int y = 0; y < 3; y++) for (int x = 0; x < 16; x++) if (glyphs[y].charAt(x) == 'x') Tiles.px(img, x, 6 + y, rune);
            if (active) {
                for (int y = 6; y < 9; y++) for (int x = 0; x < 16; x++) if (glyphs[y - 6].charAt(x) != 'x' && r.nextInt(5) == 0) Tiles.px(img, x, y, shade(GLOW, 0.55));
            }
            return img;
        }

        /** The top face: a dressed stone with a ring-and-star sigil cut in it. */
        static BufferedImage frameTop(boolean active) {
            BufferedImage img = Tiles.noise(shade(STONE, 1.05), 5, 79);
            int line = active ? GLOW : RUNE;
            for (int x = 0; x < 16; x++) {
                for (int y = 0; y < 16; y++) {
                    double d = Math.hypot(x + 0.5 - 8, y + 0.5 - 8);
                    if (d > 4.6 && d < 5.6) Tiles.px(img, x, y, line);
                }
            }
            for (int i = 2; i <= 13; i++) {
                Tiles.px(img, i, 8, line);
                Tiles.px(img, 8, i, line);
            }
            for (int i = 0; i < 16; i++) {
                Tiles.px(img, i, 0, shade(STONE, 0.7));
                Tiles.px(img, 0, i, shade(STONE, 0.7));
            }
            return img;
        }

        /**
         * The open well: pale, half see-through light (picked up as translucent by the renderer), animated over 32 frames
         * (polar_portal.png.mcmeta): two interfering ripples drift across it and a few sparks of frost wink in and out.
         * The ripples' wavelengths divide 16, so the tile repeats seamlessly across the well.
         */
        static BufferedImage portal() {
            int frames = 32;
            BufferedImage img = new BufferedImage(16, 16 * frames, BufferedImage.TYPE_INT_ARGB);
            Random r = new Random(1908);
            int[][] sparks = new int[6][3];
            for (int i = 0; i < sparks.length; i++) sparks[i] = new int[] {r.nextInt(16), r.nextInt(16), r.nextInt(frames)};
            for (int f = 0; f < frames; f++) {
                double p = 2 * Math.PI * f / frames;
                for (int x = 0; x < 16; x++) {
                    for (int y = 0; y < 16; y++) {
                        double v = 0.5 + 0.3 * Math.sin(2 * Math.PI * (x + 2 * y) / 16 + p) + 0.2 * Math.sin(2 * Math.PI * (2 * x - y) / 16 - 2 * p);
                        int c = v > 0.82 ? 0xF4FDFF : v > 0.6 ? 0xC8F2FF : v > 0.38 ? 0x9CE2F6 : 0x74C8EA;
                        int alpha = Math.min(255, 140 + (int) (v * 90));
                        img.setRGB(x, f * 16 + y, (alpha << 24) | c);
                    }
                }
                for (int[] s : sparks) {
                    int age = Math.floorMod(f - s[2], frames);
                    if (age < 4) img.setRGB(s[0], f * 16 + s[1], (age == 1 ? 0xFF000000 : 0xC8000000) | 0xFFFFFF);
                }
            }
            return img;
        }
    }

    // ---------------------------------------------------------------- 26-28: the Hollow sites' finds

    /**
     * The Hollow overhaul's art (26-28), drawn from character grids (one letter a colour, '.' clear): the eight relics,
     * the three Hollow sherds and their pot patterns, the Concavian trim (its template, and the worn trim in vanilla's
     * trim greys, which the game recolours per material), and the Kindled vault's seal and sockets.
     */
    static final class Relics {
        // Shared colours
        static final java.util.Map<java.lang.Character, Integer> INK = java.util.Map.ofEntries(
                java.util.Map.entry('k', 0x1A1414), java.util.Map.entry('d', 0x3A3434), java.util.Map.entry('s', 0x6E6A66),
                java.util.Map.entry('w', 0xF2EEE4), java.util.Map.entry('i', 0xE6DCC0), java.util.Map.entry('I', 0xC8BC98),
                java.util.Map.entry('c', 0x9EE8F0), java.util.Map.entry('C', 0x4AB8C8), java.util.Map.entry('v', 0x2A7A8A),
                java.util.Map.entry('h', 0xC8402E), java.util.Map.entry('H', 0x8A2418), java.util.Map.entry('g', 0xE8C060),
                java.util.Map.entry('G', 0xB08A2A), java.util.Map.entry('o', 0xFFB040), java.util.Map.entry('O', 0xD86A1A),
                java.util.Map.entry('b', 0x8A6A44), java.util.Map.entry('B', 0x5A3E22), java.util.Map.entry('n', 0x2A3A6A),
                java.util.Map.entry('N', 0x1A2448), java.util.Map.entry('e', 0x7ACB5A), java.util.Map.entry('E', 0x3E8A3A),
                java.util.Map.entry('t', 0xB06A48), java.util.Map.entry('T', 0x7A4430), java.util.Map.entry('r', 0x5A2E1E),
                java.util.Map.entry('q', 0xD0D0D8), java.util.Map.entry('Q', 0x9A9AA4), java.util.Map.entry('y', 0x9EE070));

        static BufferedImage grid(String... rows) {
            BufferedImage img = new BufferedImage(rows[0].length(), rows.length, BufferedImage.TYPE_INT_ARGB);
            for (int y = 0; y < rows.length; y++) {
                for (int x = 0; x < rows[y].length(); x++) {
                    char ch = rows[y].charAt(x);
                    img.setRGB(x, y, ch == '.' ? CLEAR : 0xFF000000 | INK.get(ch));
                }
            }
            return img;
        }

        /**
         * A face of the Gray Cargo Pod (32c), in the hull's pale alloy with dark seams. The sides have a band that's lit
         * green while it's sealed and dead once open, and an open side shows its hatch swung away on a dark hold; the top
         * has a round seal, lit while sealed, a dark port once open. The bottom is plain alloy.
         */
        static BufferedImage pod(boolean open, Boolean top) {
            BufferedImage img = new BufferedImage(16, 16, BufferedImage.TYPE_INT_ARGB);
            Random rnd = new Random(open ? 3202 : 3201);
            for (int y = 0; y < 16; y++) {
                for (int x = 0; x < 16; x++) {
                    int shade = rnd.nextInt(7) - 3;
                    int rgb = (0xB4 + shade) << 16 | (0xB8 + shade) << 8 | (0xC0 + shade);
                    if (x == 0 || y == 0 || x == 15 || y == 15) {
                        rgb = 0x7A7E86;
                    } else if (x == 1 || y == 1) {
                        rgb = 0xD0D4DA;
                    }
                    img.setRGB(x, y, 0xFF000000 | rgb);
                }
            }
            if (top == null) {
                return img; // the bottom: plain alloy
            }
            if (top) {
                for (int y = 4; y <= 11; y++) {
                    for (int x = 4; x <= 11; x++) {
                        double d = Math.hypot(x - 7.5, y - 7.5);
                        if (d <= 3.9) {
                            int rgb = d > 2.9 ? 0x6E727A : open ? 0x1A1414 : d < 1.5 ? 0x9EE070 : 0x7ACB5A;
                            img.setRGB(x, y, 0xFF000000 | rgb);
                        }
                    }
                }
                return img;
            }
            for (int x = 1; x < 15; x++) {
                img.setRGB(x, 4, 0xFF6E727A);
                img.setRGB(x, 11, 0xFF6E727A);
                img.setRGB(x, 7, 0xFF000000 | (open ? 0x3A3434 : 0x7ACB5A));
                img.setRGB(x, 8, 0xFF000000 | (open ? 0x2A2626 : x % 3 == 0 ? 0x9EE070 : 0x7ACB5A));
            }
            if (open) {
                for (int y = 2; y <= 13; y++) {
                    for (int x = 3; x <= 12; x++) {
                        boolean edge = x == 3 || x == 12 || y == 2 || y == 13;
                        img.setRGB(x, y, 0xFF000000 | (edge ? 0x5A5E66 : y > 10 ? 0x2A2626 : 0x1A1414));
                    }
                }
            }
            return img;
        }

        static int writeAll(File root) throws Exception {
            int n = 0;
            // The relics
            write(new File(root, "item/kindled_idol.png"), grid(
                    "......cc........", ".....cwwc.......", "....ccCCcc......", "..c.cCvvCc.c....", "..cc.cCCc.cc....", "...ccCCCCcc.....",
                    ".....cCCc.......", ".....cCCc.......", "....cCvvCc......", "....cCvvCc......", "....cCCCCc......", ".....cCCc.......",
                    "....ddddddd.....", "...dsssssssd....", "...ddddddddd....", "................"));
            write(new File(root, "item/crystal_tablet.png"), grid(
                    "................", "...cccccccccc...", "..cwwCCCCCCCvc..", "..cwC.C.CC.Cvc..", "..cCCCCCCCCCvc..", "..cC.CC.C.CCvc..",
                    "..cCCCCCCCCCvc..", "..cCC.C.CC.Cvc..", "..cCCCCCCCCCvc..", "..cC.CCC.C.Cvc..", "..cCCCCCCCCCvc..", "..cvvvvvvvvvvc..",
                    "...cccccccccc...", "................", "................", "................"));
            write(new File(root, "item/sunstone_circlet.png"), grid(
                    "................", "................", "......oooo......", "....ooOOOOoo....", "...oO..gg..Oo...", "..oO..gooG..Oo..",
                    "..o...gooG...o..", "..oG...GG...Go..", "..GoG......GoG..", "...GoGG..GGoG...", ".....GGGGGG.....", "................",
                    "................", "................", "................", "................"));
            write(new File(root, "item/starsteel_mask.png"), grid(
                    "......hhhh......", ".....hhhhhh.....", "....hhgggghh....", "....hhhhhhhh....", "....hkkhhkkh....", "....hcvhhcvh....",
                    "....hhhhhhhh....", ".....hhGGhh.....", ".....hhhhhh.....", ".....hHhhHh.....", ".....hhkkhh.....", "......hhhh......",
                    "......HhhH......", ".......HH.......", "................", "................").getSubimage(0, 0, 16, 16));
            write(new File(root, "item/gray_specimen_jar.png"), grid(
                    "................", ".....qqqqqq.....", ".....QQQQQQ.....", "....c......c....", "...c.yyyyyy.c...", "...cyyyyyyyyc...",
                    "...cyyy.ddyyc...", "...cyyd..dyyc...", "...cyy.d.yyyc...", "...cyyy.dyyyc...", "...cyyyyyyyyc...", "...cyyyyyyyyc...",
                    "....cyyyyyyc....", ".....QQQQQQ.....", "................", "................"));
            write(new File(root, "item/expedition_sextant.png"), grid(
                    "................", ".......GG.......", "......GggG......", ".......GG.......", ".......bb.......", "......bGGb......",
                    ".....bG..Gb.....", "....bG.cc.Gb....", "...bG..cc..Gb...", "..bG........Gb..", ".bGggggggggggGb.", ".bGGGGGGGGGGGGb.",
                    "................", "................", "................", "................"));
            write(new File(root, "item/ivory_carving.png"), grid(
                    "................", "................", "....iiiiii......", "...iiiiiiiii....", "..iiiiiiiiiiii..", ".Ikiiiiiiiiiii..",
                    ".IIiiiiiiiiiii..", "..IiIiiiiiiiii..", ".I.IIiiiiiiIi...", ".I..iI.iI..iI...", "..I.iI.iI..iI...", "....II.II..II...",
                    "...bbbbbbbbbbb..", "...BBBBBBBBBBB..", "................", "................"));
            write(new File(root, "item/star_chart.png"), grid(
                    "................", "..iiiiiiiiiiii..", ".iIiiiiiiiiiiIi.", ".iInnnnnnnnnnIi.", ".iInwnnnngnnnIi.", ".iInnnwnnnnnwIi.",
                    ".iInnnnnoOnnnIi.", ".iInngnnOOnnnIi.", ".iInnnnnnnnwnIi.", ".iInwnnnnnnnnIi.", ".iInnnnngnnnnIi.", ".iInnnnnnnnnnIi.",
                    ".iIiiiiiiiiiiIi.", "..iiiiiiiiiiii..", "................", "................"));
            n += 8;
            // 32c: the Grays' gadgets from a crash site's cargo pod, and the pod
            write(new File(root, "item/humming_doohickey.png"), grid(
                    "................", ".......k........", ".......c........", "......kQk.......", ".......Q........", ".....qqqqq......",
                    "....qwqqqqQ.....", "...qqqcCcqqQ....", "...qqcyyycqQ....", "...qqqcCcqqQ....", "....qqqqqqQ.....", "....QQQQQQQ.....",
                    "...ddddddddd....", "...dsssssssd....", "....ddddddd.....", "................"));
            write(new File(root, "item/self_warming_rock.png"), grid(
                    "................", "................", "................", "......ddddd.....", "....ddsssssd....", "...dssssoossd...",
                    "..dsssoOOosssd..", "..dssoOooOossd..", "..dsssoOOossssd.", "..dsssssoosssd..", "...dssssssssd...", "....dddddddd....",
                    "................", "................", "................", "................"));
            write(new File(root, "item/backward_watch.png"), grid(
                    "......GG........", ".....G..G.......", "......GG........", "....gggggg......", "...gwwwwwwg.....", "..gwwwkwwwwg....",
                    "..gwwwkwwwwg....", "..gwwwkkkwwg....", "..gwwwwwwwwg....", "..gwwwwwwwwg....", "...gwwwwwwg.....", "....gggggg......",
                    "................", "................", "................", "................"));
            write(new File(root, "item/restless_marble.png"), grid(
                    "................", "................", "................", "................", ".....cccc.......", "....cwcCCc......",
                    "...cwcCvCCc.....", "...cCCvvCCc.....", "...cCvCCvCc.....", "...cCCCvCCc.....", "....cCCCCc......", ".....cccc.......",
                    "................", "................", "......dddd......", "................"));
            write(new File(root, "item/unopenable_box.png"), grid(
                    "................", "................", "..QQQQQQQQQQQQ..", "..QqwqqqqqqqqQ..", "..QqqqqqqqqqqQ..", "..QqqqqqqqqqqQ..",
                    "..QqqqqqqqqqqQ..", "..QqqqqcCqqqqQ..", "..QqqqqqqqqqqQ..", "..QqqqqqqqqqqQ..", "..QqqqqqqqqqqQ..", "..QqqqqqqqqqqQ..",
                    "..QQQQQQQQQQQQ..", "...dddddddddddd.", "................", "................"));
            // 32d: the Polar Survey's last letter, an old envelope with its flap and a red wax seal
            write(new File(root, "item/survey_letter.png"), grid(
                    "................", "................", "................", ".IIIIIIIIIIIIII.", ".IIiiiiiiiiiiII.", ".IiIiiiiiiiiIiI.",
                    ".IiiIiiiiiiIiiI.", ".IiiiIiiiiIiiiI.", ".IiiiiIhhIiiiiI.", ".IiiiiihHhiiiiI.", ".IiiiiiihiiiiiI.", ".IiiiiiiiiiiiiI.",
                    ".IIIIIIIIIIIIII.", "..dddddddddddd..", "................", "................"));
            n++;
            for (boolean open : new boolean[] {false, true}) {
                String suffix = open ? "_open" : "";
                write(new File(root, "block/gray_cargo_pod_side" + suffix + ".png"), pod(open, false));
                write(new File(root, "block/gray_cargo_pod_top" + suffix + ".png"), pod(open, true));
            }
            write(new File(root, "block/gray_cargo_pod_bottom.png"), pod(true, null));
            n += 10;
            // The sherds (shards of terracotta with the motif on) and the pot patterns themselves
            String[] sherd = {
                    "................", "................", "....tttt........", "...tttttttt.....", "..tttttttttt....", "..tttttttttttt..",
                    "..ttttttttttttt.", "...tttttttttttt.", "...tttttttttttT.", "....ttttttttttT.", ".....tttttttTT..", ".......tttTT....",
                    "................", "................", "................", "................"};
            String[][] motifs = {
                    {"......r.....", "....r.r.r...", ".....rrr....", "...rrrrrrr..", ".....rrr....", "....r.r.r...", "......r....."},
                    {"......r.....", ".....rrr....", "....r.r.r...", "....r.r.r...", "....r.r.r...", ".....rrr....", "......r....."},
                    {"....rrrrr...", "...r.....r..", "..r.rr.rr.r.", "..r.......r.", "...r..r..r..", "....rr.rr...", "............"}};
            String[] names = {"sun", "crystal", "watcher"};
            for (int k = 0; k < names.length; k++) {
                BufferedImage item = grid(sherd);
                for (int y = 0; y < motifs[k].length; y++) {
                    for (int x = 0; x < motifs[k][y].length(); x++) {
                        if (motifs[k][y].charAt(x) == 'r') {
                            item.setRGB(x + 2, y + 3, 0xFF000000 | INK.get('r'));
                        }
                    }
                }
                write(new File(root, "item/" + names[k] + "_pottery_sherd.png"), item);
                BufferedImage pattern = new BufferedImage(16, 16, BufferedImage.TYPE_INT_ARGB);
                Random rnd = new Random(2800 + k);
                for (int y = 0; y < 16; y++) {
                    for (int x = 0; x < 16; x++) {
                        int base = rnd.nextInt(5) == 0 ? 0x9A5434 : 0xA45E3C;
                        pattern.setRGB(x, y, 0xFF000000 | base);
                    }
                }
                for (int y = 0; y < motifs[k].length; y++) {
                    for (int x = 0; x < motifs[k][y].length(); x++) {
                        if (motifs[k][y].charAt(x) == 'r') {
                            pattern.setRGB(x + 2, y + 4, 0xFF000000 | 0x4E2416);
                        }
                    }
                }
                write(new File(root, "entity/decorated_pot/" + names[k] + "_pottery_pattern.png"), pattern);
                n += 2;
            }
            // The trim's template (a plate of netherite-dark stone with the crystal sun on it), and the worn trim
            write(new File(root, "item/concavian_armor_trim_smithing_template.png"), grid(
                    "................", "..dddddddddddd..", ".dsssssssssssd..", ".dssscccccsssd..", ".dsscCwwwCcssd..", ".dsscwCvvCwcsd..",
                    ".dssCwvcvwCssd..", ".dsscwCvvCwcsd..", ".dsscCwwwCcssd..", ".dssscccccsssd..", ".dsssssssssssd..", ".dsssssssssssd..",
                    "..ddddddddddd...", "................", "................", "................"));
            write(new File(root, "trims/entity/humanoid/concavian.png"), trim(false));
            write(new File(root, "trims/entity/humanoid_leggings/concavian.png"), trim(true));
            for (String layer : new String[] {"humanoid", "humanoid_leggings"}) {
                java.nio.file.Files.writeString(new File(root, "trims/entity/" + layer + "/concavian.png.mcmeta").toPath(),
                        "{\n  \"palette\" : {\n    \"base_palette\" : \"trim_base\"\n  }\n}\n");
            }
            n += 3;
            // The seal: dark stone veined with crystal round a carved sun; the sockets: dressed stone, a ring of runes on the
            // side, a hollow in the top (lit in a marked one)
            write(new File(root, "block/kindled_seal.png"), grid(
                    "kdddkddddkdddddk", "dvsddsdcsddsdsvd", "dsCsdsdcdssdsCsd", "ddsCdssccsdsCsdd", "kdddCsCwwCsCdddk", "dsdsdCwvvwCdsdsd",
                    "ddsscCvccvCcssdd", "cccccwvcCvwccccc", "ddsscCvccvCcssdd", "dsdsdCwvvwCdsdsd", "kdddCsCwwCsCdddk", "ddsCdssccsdsCsdd",
                    "dsCsdsdcdssdsCsd", "dvsddsdcsddsdsvd", "dsddsddcddsddsdd", "kdddkddddkdddddk"));
            write(new File(root, "block/seal_socket_side.png"), grid(
                    "ssssssssssssssss", "sQQQQQQQQQQQQQQs", "sddddddddddddds.", "sdvdvCvdvCvdvdds", "sdCdcdCdcdCdcdds", "sdvdvCvdvCvdvdds",
                    "sddddddddddddds.", "sQQQQQQQQQQQQQQs", "sdsdsdsdsdsdsdss", "sddddddddddddds.", "sdsdsdsdsdsdsdss", "sddddddddddddds.",
                    "sdsdsdsdsdsdsdss", "sddddddddddddds.", "sQQQQQQQQQQQQQQs", "ssssssssssssssss").getSubimage(0, 0, 16, 16));
            String[] top = {
                    "ssssssssssssssss", "sQQQQQQQQQQQQQQs", "sQddddddddddddQs", "sQdssssssssssdQs", "sQdsddddddddsdQs", "sQdsdkkkkkkdsdQs",
                    "sQdsdk....kdsdQs", "sQdsdk....kdsdQs", "sQdsdk....kdsdQs", "sQdsdk....kdsdQs", "sQdsdkkkkkkdsdQs", "sQdsddddddddsdQs",
                    "sQdssssssssssdQs", "sQddddddddddddQs", "sQQQQQQQQQQQQQQs", "ssssssssssssssss"};
            write(new File(root, "block/seal_socket_top.png"), grid(top).getSubimage(0, 0, 16, 16));
            String[] marked = top.clone();
            for (int y = 6; y <= 9; y++) {
                marked[y] = marked[y].replace("....", y == 6 || y == 9 ? "cCCc" : "CwwC");
            }
            write(new File(root, "block/seal_socket_top_marked.png"), grid(marked));
            write(new File(root, "block/seal_socket_bottom.png"), grid(
                    "ssssssssssssssss", "sddddddddddddds.", "sdsdsdsdsdsdsdss", "sddddddddddddds.", "sdsdsdsdsdsdsdss", "sddddddddddddds.",
                    "sdsdsdsdsdsdsdss", "sddddddddddddds.", "sdsdsdsdsdsdsdss", "sddddddddddddds.", "sdsdsdsdsdsdsdss", "sddddddddddddds.",
                    "sdsdsdsdsdsdsdss", "sddddddddddddds.", "sdsdsdsdsdsdsdss", "ssssssssssssssss"));
            n += 5;
            // Fill the odd clear pixels the grids leave at a tile's edge
            for (String tile : new String[] {"seal_socket_side", "seal_socket_bottom", "seal_socket_top"}) {
                File f = new File(root, "block/" + tile + ".png");
                BufferedImage img = ImageIO.read(f);
                for (int y = 0; y < 16; y++) {
                    for (int x = 0; x < 16; x++) {
                        if ((img.getRGB(x, y) >>> 24) == 0 && !(tile.endsWith("top") && x >= 6 && x <= 9 && y >= 6 && y <= 9)) {
                            img.setRGB(x, y, 0xFF000000 | INK.get('s'));
                        } else if ((img.getRGB(x, y) >>> 24) == 0) {
                            img.setRGB(x, y, 0xFF000000 | INK.get('k'));
                        }
                    }
                }
                write(f, img);
            }
            return n;
        }

        /**
         * The worn trim, 64 x 32 in vanilla's trim greys (light to dark: E0, C0, A0, 80, 60), recoloured by the game per
         * trim material: a circlet round the helmet with a crystal at the brow, a sun on the breast between collar lines,
         * cuffs on the arms, bands at the boots' tops; on the leggings, a belt and a band at each knee.
         */
        static BufferedImage trim(boolean leggings) {
            BufferedImage img = new BufferedImage(64, 32, BufferedImage.TYPE_INT_ARGB);
            int hi = 0xFFE0E0E0, mid = 0xFFC0C0C0, lo = 0xFFA0A0A0, sh = 0xFF808080, dk = 0xFF606060;
            if (leggings) {
                for (int x = 16; x < 40; x++) { // the belt round the body's lower edge
                    img.setRGB(x, 30, x % 4 == 0 ? hi : mid);
                    img.setRGB(x, 31, sh);
                }
                for (int x = 0; x < 16; x++) { // knee bands, both legs share the texture
                    img.setRGB(x, 24, mid);
                    img.setRGB(x, 25, x % 4 == 2 ? hi : lo);
                }
                img.setRGB(5, 23, hi); // the knee's crystal
                img.setRGB(6, 23, mid);
                return img;
            }
            for (int x = 0; x < 32; x++) { // the circlet round the head's four sides
                img.setRGB(x, 10, x % 8 == 4 ? hi : mid);
                img.setRGB(x, 11, lo);
            }
            img.setRGB(11, 9, hi); // the brow's crystal
            img.setRGB(12, 9, hi);
            img.setRGB(11, 12, sh);
            img.setRGB(12, 12, sh);
            // The breast: collar lines down to a sun
            for (int k = 0; k < 4; k++) {
                img.setRGB(20 + k, 20 + k, mid);
                img.setRGB(27 - k, 20 + k, mid);
            }
            int[][] sun = {{23, 25}, {24, 25}, {23, 26}, {24, 26}};
            for (int[] p : sun) {
                img.setRGB(p[0], p[1], hi);
            }
            for (int[] p : new int[][] {{22, 25}, {25, 25}, {22, 26}, {25, 26}, {23, 24}, {24, 24}, {23, 27}, {24, 27}}) {
                img.setRGB(p[0], p[1], lo);
            }
            for (int[] p : new int[][] {{21, 24}, {26, 24}, {21, 27}, {26, 27}}) {
                img.setRGB(p[0], p[1], dk);
            }
            for (int x = 40; x < 56; x++) { // arm cuffs
                img.setRGB(x, 29, mid);
                img.setRGB(x, 30, x % 4 == 1 ? hi : lo);
            }
            for (int x = 0; x < 16; x++) { // boot bands
                img.setRGB(x, 26, mid);
                img.setRGB(x, 27, sh);
            }
            return img;
        }
    }
}
