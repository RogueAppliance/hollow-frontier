package com.hollowfrontier.client;

import java.util.ArrayList;
import java.util.List;

import org.jspecify.annotations.Nullable;

import com.hollowfrontier.content.frontier.bounty.BoardFace;
import com.hollowfrontier.content.frontier.bounty.BoardPosting;
import com.hollowfrontier.content.frontier.bounty.BountyData;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;

import net.minecraft.client.gui.Font;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;

/**
 * Wanted posters (18), for both boards: aged paper with WANTED across the top, a sketch of the face (drawn from a few hats,
 * bandanas and beards, picked by the bounty), the name, the reward and which way the hideout lies. TAKEN (with the
 * hunter's name) and DEAD go on as rubber stamps. All of it is drawn flat, the way sign text is, so it needs no texture
 * and reads in any language.
 * <p>
 * {@link #paint} draws a face of columns by rows cells, 64 units to a cell, from its top left corner (y down); the caller
 * scales that onto the board.
 */
final class PosterPainter {
    static final int CELL = 64;
    // The name's scale on a poster, and on a raid leader's (which starts its text WIDE_TEXT units in, past the face)
    private static final float NAME = 0.75F, WIDE_NAME = 0.9F;
    private static final int WIDE_TEXT = 36;
    private static final int PAPER = 0xFFE6D6AC, PAPER_RAIDER = 0xFFEBCF94, PAPER_EDGE = 0xFF8A7044, INK = 0xFF2B1D0E,
            HEADER = 0xFF7A1E12, REWARD = 0xFF2E4A1A, FADED = 0xFF5E4A30, TAKEN = 0xFF1E3A8A, DEAD = 0xFFB0101A;
    private static final int[] HATS = {0xFF3A2614, 0xFF1C1C1C, 0xFF6E5236, 0xFF4A3A2A};
    private static final int SKIN = 0xFFC9A27A, SHADOW = 0xFF8C6A48, BANDANA = 0xFF9E1E1E, BEARD = 0xFF4A3220;

    /** One poster, worded and laid out (u, v: 0..1 on the face). */
    record Poster(BoardFace.Kind kind, float u0, float v0, float u1, float v1, String header, List<String> name,
                  String reward, String bearing, int seed, @Nullable String stamp, @Nullable String by) {}

    private final Font font;

    PosterPainter(Font font) {
        this.font = font;
    }

    /** The posters a face shows, worded for this client, with bearings from origin. */
    List<Poster> posters(BoardFace face, int columns, int rows, BlockPos origin) {
        List<Poster> posters = new ArrayList<>();
        for (BoardFace.Slot slot : face.slots(columns, rows)) {
            if (slot.kind() == BoardFace.Kind.WANTED) {
                var wanted = face.wanted().get(slot.index());
                posters.add(new Poster(slot.kind(), slot.u0(), slot.v0(), slot.u1(), slot.v1(), tr("wanted"), wrap(wanted.name(), false),
                        tr("fine", wanted.fine()), tr("dead_or_alive"), wanted.name().hashCode(), null, null));
                continue;
            }
            if (slot.kind() == BoardFace.Kind.SURVEY) { // 32d: news of the Polar Survey, a bearded explorer for its likeness
                posters.add(new Poster(slot.kind(), slot.u0(), slot.v0(), slot.u1(), slot.v1(), tr("wanted"), wrap(tr("survey"), false),
                        tr("reward", com.hollowfrontier.content.frontier.survey.SurveyBounty.REWARD), tr("survey_ask"), 1908 << 5, null, null));
                continue;
            }
            BoardPosting posting = face.shown().get(slot.index());
            BountyData bounty = posting.bounty();
            String stamp = switch (posting.status()) {
                case TAKEN -> tr("taken");
                case DEAD -> tr("dead");
                case CAPTURED -> tr("captured");
                default -> null;
            };
            posters.add(new Poster(slot.kind(), slot.u0(), slot.v0(), slot.u1(), slot.v1(), tr(posting.raider() ? "raid_leader" : "wanted"),
                    wrap(bounty.target(), slot.kind() == BoardFace.Kind.RAIDER), tr("reward", bounty.reward()), bearing(origin, bounty), bounty.id().hashCode(), stamp,
                    posting.status() == BoardPosting.Status.TAKEN ? posting.takenBy() : null));
        }
        return posters;
    }

    private static String tr(String key, Object... args) {
        return Component.translatable("gui.hollowfrontier.bounty_board." + key, args).getString();
    }

    // The name in at most two lines of what fits on the poster at the name's scale
    private List<String> wrap(String name, boolean wide) {
        List<String> lines = new ArrayList<>();
        int room = (int) (wide ? (2 * CELL - 6 - WIDE_TEXT - 4) / WIDE_NAME : (CELL - 6 - 4) / NAME);
        for (var line : font.split(Component.literal(name), room)) {
            StringBuilder out = new StringBuilder();
            line.accept((index, style, codePoint) -> {
                out.appendCodePoint(codePoint);
                return true;
            });
            lines.add(out.toString());
            if (lines.size() == 2) {
                break;
            }
        }
        return lines;
    }

    private static String bearing(BlockPos from, BountyData bounty) {
        double dx = bounty.x() - from.getX(), dz = bounty.z() - from.getZ();
        int distance = (int) Math.round(Math.hypot(dx, dz) / 10.0) * 10;
        String[] points = {"north", "northeast", "east", "southeast", "south", "southwest", "west", "northwest"};
        String point = points[Math.floorMod((int) Math.round(Math.toDegrees(Math.atan2(dx, -dz)) / 45.0), 8)];
        return tr("bearing", distance, tr("compass." + point));
    }

    // ---------------------------------------------------------------- drawing

    /** The posters on a face of columns by rows cells, from its top left. */
    void paint(PoseStack pose, SubmitNodeCollector collector, List<Poster> posters, int columns, int rows, int light) {
        for (Poster poster : posters) {
            poster(pose, collector, poster, columns * CELL, rows * CELL, light);
        }
    }

    private void poster(PoseStack pose, SubmitNodeCollector collector, Poster p, float width, float height, int light) {
        float x0 = p.u0() * width + 3, y0 = p.v0() * height + 3, x1 = p.u1() * width - 3, y1 = p.v1() * height - 3;
        boolean wide = p.kind() == BoardFace.Kind.RAIDER;
        // The paper, a shade behind the ink
        pose.pushPose();
        pose.translate(0.0F, 0.0F, -0.8F);
        rect(pose, collector, x0 - 0.6F, y0 - 0.6F, x1 + 0.6F, y1 + 0.6F, PAPER_EDGE, light);
        pose.translate(0.0F, 0.0F, 0.2F);
        rect(pose, collector, x0, y0, x1, y1, wide ? PAPER_RAIDER : PAPER, light);
        pose.popPose();

        // Laid out to fill the 58 units square (or 122 by 58) with as big a hand as fits: the header, the face, the name in
        // up to two lines, the reward, and the bearing in compass short form
        float mid = (x0 + x1) / 2;
        text(pose, collector, p.header(), mid, y0 + 2, 1.0F, HEADER, light, true);
        float stampY;
        if (wide) {
            sketch(pose, collector, x0 + 6, y0 + 15, 1.6F, p.seed(), true, light);
            float tx = x0 + WIDE_TEXT;
            for (int i = 0; i < p.name().size(); i++) {
                text(pose, collector, p.name().get(i), tx, y0 + 13 + i * 9, WIDE_NAME, INK, light, false);
            }
            text(pose, collector, p.reward(), tx, y0 + 33, 1.0F, REWARD, light, false);
            text(pose, collector, p.bearing(), tx, y0 + 45, 0.75F, FADED, light, false);
            stampY = y0 + 30;
        } else {
            if (p.kind() == BoardFace.Kind.WANTED) {
                badLikeness(pose, collector, mid - 7, y0 + 11, p.seed(), light);
            } else {
                sketch(pose, collector, mid - 7, y0 + 11, 1.0F, p.seed(), p.kind() == BoardFace.Kind.OUTLAW, light);
            }
            float nameY = y0 + 27 + (p.name().size() == 1 ? 3.5F : 0); // one line sits in the middle of the room for two
            for (int i = 0; i < p.name().size(); i++) {
                text(pose, collector, p.name().get(i), mid, nameY + i * 7, NAME, INK, light, true);
            }
            text(pose, collector, p.reward(), mid, y0 + 42, 0.85F, REWARD, light, true);
            text(pose, collector, p.bearing(), mid, y0 + 51, 0.65F, FADED, light, true);
            stampY = y0 + 18;
        }
        // Stamped over the face, not the name
        if (p.stamp() != null) {
            pose.pushPose();
            pose.translate(mid, stampY, 0.4F);
            pose.rotateDegrees(Axis.ZP, -15.0F);
            boolean dead = p.by() == null;
            text(pose, collector, p.stamp(), 0, dead ? -6 : -9, dead ? 1.6F : 1.2F, dead ? DEAD : TAKEN, light, true);
            if (p.by() != null) {
                text(pose, collector, p.by(), 0, 1.5F, 0.7F, TAKEN, light, true); // under the stamp, still above the name
            }
            pose.popPose();
        }
    }

    // A face 14 units square at scale 1: hat brim and crown, the face, eyes, and a bandana or a beard
    private void sketch(PoseStack pose, SubmitNodeCollector collector, float atX, float atY, float scale, int seed, boolean outlaw, int light) {
        int hat = HATS[Math.floorMod(seed, HATS.length)];
        boolean bandana = outlaw && Math.floorMod(seed >> 3, 3) != 0;
        boolean beard = !bandana && Math.floorMod(seed >> 5, 2) == 0;
        float x = 0, y = 0;
        pose.pushPose();
        pose.translate(atX, atY, -0.4F);
        pose.scale(scale, scale, 1.0F);
        rect(pose, collector, x + 3, y, x + 11, y + 3, hat, light);
        rect(pose, collector, x, y + 3, x + 14, y + 4, hat, light);
        rect(pose, collector, x + 3, y + 4, x + 11, y + 13, SKIN, light);
        detail(pose, collector, x + 3, y + 4, x + 11, y + 5, SHADOW, light);
        detail(pose, collector, x + 4.5F, y + 6.5F, x + 6, y + 7.5F, INK, light);
        detail(pose, collector, x + 8, y + 6.5F, x + 9.5F, y + 7.5F, INK, light);
        if (bandana) {
            detail(pose, collector, x + 3, y + 9, x + 11, y + 13, BANDANA, light);
        } else if (beard) {
            detail(pose, collector, x + 3, y + 10, x + 11, y + 13, BEARD, light);
        } else {
            detail(pose, collector, x + 5.5F, y + 10, x + 8.5F, y + 10.6F, SHADOW, light);
        }
        rect(pose, collector, x + 4, y + 13, x + 10, y + 14, SHADOW, light);
        pose.popPose();
    }

    /**
     * A wanted player's face, as the town's artist drew it without ever seeing them (21b): one of four, by name. A lumpy
     * potato with eyes at different heights; a stick figure in a hat; a hat and a moustache and nothing else; or a round
     * smiling face, cross-eyed.
     */
    private void badLikeness(PoseStack pose, SubmitNodeCollector collector, float atX, float atY, int seed, int light) {
        int hat = HATS[Math.floorMod(seed, HATS.length)];
        pose.pushPose();
        pose.translate(atX, atY, -0.4F);
        switch (Math.floorMod(seed >> 4, 4)) {
            case 0 -> { // the potato
                rect(pose, collector, 2, 3, 12, 13, SKIN, light);
                rect(pose, collector, 1, 5, 2, 11, SKIN, light);
                rect(pose, collector, 12, 4, 13, 9, SKIN, light);
                rect(pose, collector, 4, 2, 9, 3, SKIN, light);
                detail(pose, collector, 4, 5, 5.5F, 6.5F, INK, light);
                detail(pose, collector, 8.5F, 7.5F, 10, 9, INK, light);
                detail(pose, collector, 5, 11, 9, 11.6F, INK, light);
            }
            case 1 -> { // the stick figure
                rect(pose, collector, 4, 0, 10, 2, hat, light);
                rect(pose, collector, 2, 2, 12, 2.8F, hat, light);
                rect(pose, collector, 5, 2.8F, 9, 6, SKIN, light);
                rect(pose, collector, 6.6F, 6, 7.4F, 10.5F, INK, light);
                detail(pose, collector, 3.5F, 7.5F, 10.5F, 8.2F, INK, light);
                rect(pose, collector, 4.5F, 10.5F, 6.8F, 11.2F, INK, light);
                rect(pose, collector, 7.2F, 10.5F, 9.5F, 11.2F, INK, light);
                rect(pose, collector, 4.5F, 11.2F, 5.2F, 14, INK, light);
                rect(pose, collector, 8.8F, 11.2F, 9.5F, 14, INK, light);
            }
            case 2 -> { // all hat, and a moustache
                rect(pose, collector, 2, 0, 12, 7, hat, light);
                rect(pose, collector, 0, 7, 14, 8.5F, hat, light);
                rect(pose, collector, 3, 10, 11, 11.5F, BEARD, light);
                rect(pose, collector, 2, 11.5F, 4, 12.5F, BEARD, light);
                rect(pose, collector, 10, 11.5F, 12, 12.5F, BEARD, light);
            }
            default -> { // round, smiling, cross-eyed
                rect(pose, collector, 3, 2, 11, 13, SKIN, light);
                rect(pose, collector, 2, 4, 12, 11, SKIN, light);
                detail(pose, collector, 5.5F, 6, 6.5F, 7, INK, light);
                detail(pose, collector, 7.5F, 6, 8.5F, 7, INK, light);
                detail(pose, collector, 4, 9.5F, 10, 10.2F, INK, light);
                detail(pose, collector, 3.5F, 8.5F, 4.2F, 9.5F, INK, light);
                detail(pose, collector, 9.8F, 8.5F, 10.5F, 9.5F, INK, light);
            }
        }
        pose.popPose();
    }

    private static void rect(PoseStack pose, SubmitNodeCollector collector, float x0, float y0, float x1, float y1, int colour, int light) {
        collector.submitTextBackground(pose, x0, y0, x1, y1, colour, Font.DisplayMode.NORMAL, light);
    }

    /**
     * A rect drawn over another one in a sketch (eyes, a bandana, a shadow): a hair in front of it. Drawn in the same plane,
     * the two fought for depth and the faces flickered as the camera moved.
     */
    private static void detail(PoseStack pose, SubmitNodeCollector collector, float x0, float y0, float x1, float y1, int colour, int light) {
        pose.pushPose();
        pose.translate(0.0F, 0.0F, 0.1F);
        rect(pose, collector, x0, y0, x1, y1, colour, light);
        pose.popPose();
    }

    private void text(PoseStack pose, SubmitNodeCollector collector, String line, float x, float y, float scale, int colour, int light, boolean centred) {
        pose.pushPose();
        pose.translate(x, y, 0.0F);
        pose.scale(scale, scale, 1.0F);
        float left = centred ? -font.width(line) / 2.0F : 0.0F;
        collector.submitText(pose, left, 0, Component.literal(line).getVisualOrderText(), false, Font.DisplayMode.NORMAL, light, colour, 0, 0);
        pose.popPose();
    }
}
