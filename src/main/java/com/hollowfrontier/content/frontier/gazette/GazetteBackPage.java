package com.hollowfrontier.content.frontier.gazette;

import static com.hollowfrontier.content.frontier.talk.Cond.act;
import static com.hollowfrontier.content.frontier.talk.Cond.only;
import static com.hollowfrontier.content.frontier.talk.Cond.when;
import static com.hollowfrontier.content.frontier.talk.Fact.*;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import org.jspecify.annotations.Nullable;

import com.hollowfrontier.content.frontier.talk.Act;
import com.hollowfrontier.content.frontier.talk.Cond;
import com.hollowfrontier.content.frontier.talk.Context;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.util.RandomSource;

/**
 * The Gazette's back page (phase 21a): one kind of column a week, classifieds, letters to the editor or corrections,
 * picked from the pieces whose {@link Cond} fits the reader (the same facts and acts as {@link
 * com.hollowfrontier.content.frontier.talk.Chatter}), weighted the same way, so the jokes follow the story. The English
 * lives here and the lang provider reads it, like {@link com.hollowfrontier.content.frontier.talk.ChatterLines}.
 */
public final class GazetteBackPage {
    private GazetteBackPage() {}

    /** A kind of column, and how many of its pieces fit on a page. */
    public enum Kind {
        CLASSIFIED(2, "CLASSIFIEDS"),
        LETTER(1, "LETTERS TO THE EDITOR"),
        CORRECTION(1, "CORRECTIONS");

        public final int perPage;
        public final String title;

        Kind(int perPage, String title) {
            this.perPage = perPage;
            this.title = title;
        }

        public String id() {
            return name().toLowerCase(Locale.ROOT);
        }

        public String titleKey() {
            return "gazette.hollowfrontier.back." + id() + ".title";
        }
    }

    public record Piece(Kind kind, String key, String text, Cond cond) {}

    /** The longest a piece may run in English and still fit its page (checked by a game test). */
    public static final int MAX_CLASSIFIED = 95, MAX_PIECE = 200;

    private static final List<Piece> PIECES = new ArrayList<>();
    private static final Map<Kind, Integer> COUNTS = new HashMap<>();

    public static List<Piece> pieces() {
        return PIECES;
    }

    private static void add(Kind kind, Cond cond, String text) {
        int n = COUNTS.merge(kind, 1, Integer::sum);
        PIECES.add(new Piece(kind, "gazette.hollowfrontier.back." + kind.id() + "." + n, text, cond));
    }

    static {
        classifieds();
        letters();
        corrections();
    }

    private static void classifieds() {
        Kind k = Kind.CLASSIFIED;
        add(k, Cond.ANY, "FOR SALE: one mule, stubborn. Also one mule, very stubborn. Ask at the livery.");
        add(k, Cond.ANY, "WANTED: piano player who keeps playing through a gunfight. Steady work.");
        add(k, Cond.ANY, "LOST: one left boot, near the creek. The right boot is offered as a reward.");
        add(k, Cond.ANY, "FOR SALE: Doc's Snake Oil, by the crate. Cures all ills. Doc has left town.");
        add(k, Cond.ANY, "WANTED: a deputy. Must own a hat. Must not shoot it.");
        add(k, Cond.ANY, "NOTICE: the sheriff's cat is not for sale, and has asked us to say so.");
        add(k, Cond.ANY, "FOR SALE: percussion caps. Will not fire in the rain. Neither will yours.");
        add(k, act(Act.GATEWAY), "FOR SALE: warm socks, one pair. The seller has been to the pole and is not going back.");
        add(k, act(Act.GATEWAY), "WANTED: anyone who has seen a large bird with teeth. My friend is up a tree.");
        add(k, only(Act.GRAYS), "FOR SALE: saucer part, still humming. No questions asked. Or answered.");
        add(k, when(SIGHTINGS), "FOR HIRE: a man with a telescope and no fear of the dark. Watches skies, a dollar a night.");
        add(k, when(ROBBED), "FOR SALE: one strongbox. Never opened by its owner. Lightly opened by others.");
        add(k, act(Act.AFTER), "FOR SALE: lead-lined nightcap. The sky's quiet now, and the owner wants his hair back.");
        add(k, when(DUST_DEVIL), "LOST: one good hat, last seen heading east at a gallop. Reward.");
        add(k, when(LOST_HAT), "FOR SALE: hats, with chin straps. Ask the reader who has just lost one.");
        // 35d: December
        add(k, when(DECEMBER), "FOR SALE: fruitcake, unopened, some years old. Will trade for anything that is not fruitcake.");
        add(k, when(DECEMBER), "WANTED: snow, any quantity, by Christmas. Will also accept flour. Ask at the schoolhouse.");
        add(k, when(DECEMBER, SIGHTINGS), "FOUND: one sleigh bell, large, in a pasture. Still warm. Owner may call at this office.");
        add(k, when(SEA_SEEN), "FOR SALE: rowing boat, used once, on a sea that is on no map. Ask no questions."); // 43d
    }

    private static void letters() {
        Kind k = Kind.LETTER;
        add(k, Cond.ANY, "Sir,\nThe new bandstand is a disgrace. A band played on it Sunday, and I could hear it.\n\n— A Resident");
        add(k, Cond.ANY, "Sir,\nMy husband says he was at the church social on Tuesday. Your paper says it was Thursday. I should like a word with him, then with you.\n\n— Mrs. E. Tolliver");
        add(k, Cond.ANY, "Sir,\nThe steam carriage in your paper was mine, and the horses started it.\n\n— An Inventor");
        add(k, Cond.ANY, "Sir,\nI have read the Gazette for nine years, and it has never once got the price of beans right. Keep it up.\n\n— A Grocer");
        add(k, act(Act.GATEWAY), "Sir,\nThe Earth is not hollow. I have begun a hole behind my house to prove it, and will write again when I reach the bottom.\n\n— A Skeptic");
        add(k, act(Act.GATEWAY), "Sir,\nA stranger paid for flour with a crystal that glows. My wife reads by it now. Is it legal tender?\n\n— A Shopkeeper");
        add(k, when(SIGHTINGS), "Sir,\nI would thank the owners of the airship to put their lights out after ten. Some of us work in the morning.\n\n— A Rancher's Widow");
        add(k, when(MUTILATIONS), "Sir,\nTo whoever took the insides out of my best steer: I want him back. Or the liver, at least.\n\n— A Rancher");
        add(k, when(DUST_DEVIL), "Sir,\nA dust devil took my laundry on Tuesday. If the gentleman two towns over is wearing my Sunday shirt, I should like it pressed.\n\n— A Widow");
        add(k, when(CROP_CIRCLES), "Sir,\nI did not make the rings in my wheat, and I resent my neighbours' suggestion that I have taken up dancing.\n\n— A Farmer");
        add(k, only(Act.GRAYS), "Sir,\nOn Saturday last I was taken up into a light and shown many wonders. I should like to know who will pay for my fence.\n\n— A Citizen");
        add(k, act(Act.AFTER), "Sir,\nThe lights have stopped. I have nothing left to write to you about.\n\nP.S. The bandstand is still a disgrace.\n— A Resident");
        add(k, when(DECEMBER, SIGHTINGS), "Sir,\nMy children say the lights over the mesa were a sleigh. Kindly do not print otherwise until January.\n\n— A Mother");
        add(k, when(DECEMBER), "Sir,\nThe town tree has more candles on it than the church. I do not say this is wrong. I say it has been noticed.\n\n— A Deacon");
        add(k, when(SEA_SEEN), "Sir,\nYour paper says there is a sea beneath us. I have dug wells for thirty years and never once struck a fish.\n\n— A Well-Digger"); // 43d
    }

    private static void corrections() {
        Kind k = Kind.CORRECTION;
        add(k, Cond.ANY, "Our report of the church social said that nobody was shot. One man was shot, slightly, by himself. We regret the error.");
        add(k, Cond.ANY, "Beans are three coppers a can, not two. The Editor blames the railroad again.");
        add(k, Cond.ANY, "Mrs. Henderson's prize pie was peach, not \"peacheous\". The typesetter has been spoken to.");
        add(k, Cond.ANY, "The outlaw in last week's Wanted column has written to say that he is taller than that.");
        add(k, Cond.ANY, "We said the new deputy shot an outlaw's hat clean off. It was his own hat. He was wearing it.");
        add(k, act(Act.GATEWAY), "Last week's paper said the mayor was eaten by a mammoth. He was only sat on. He is resting comfortably, and so is the mammoth.");
        add(k, act(Act.GATEWAY), "We reported that a prospector fell down a hole at the top of the world. He has written to say that he jumped.");
        add(k, only(Act.GRAYS), "Gentlemen from out of town have asked us to correct last week's correction. It was not the planet Venus after all. It was a different planet. They did not say which.");
        add(k, only(Act.GRAYS), "The Gazette printed nothing about lights in the sky last week. Readers who remember reading it are mistaken.");
        add(k, Cond.ANY, "We reported a twister through the stockyard. It was a dust devil, and it took only the stockman's hat. He would like it back.");
        add(k, only(Act.GRAYS), "Gentlemen from out of town have asked us to say that last week's lights were a dust devil carrying a lantern. We have said it.");
        add(k, act(Act.AFTER), "We reported that the sky has been quiet for a month. A goose flew over on Thursday. We regret the alarm.");
        add(k, when(DECEMBER, SIGHTINGS), "We reported a sleigh over the mesa on Tuesday. It had no horses, no runners and no driver. It was not a sleigh. We regret the excitement.");
        add(k, when(DECEMBER, SIGHTINGS), "Gentlemen from out of town have asked us to correct our correction. It was a sleigh. They were very firm about it. Merry Christmas.");
        add(k, when(CHRISTMAS), "The punch at the Christmas social was not \"spiked\". It was eggnog, which arrives that way. The Editor has apologised to the punch.");
        add(k, when(SEA_SEEN), "We reported a sea under the earth with no tide. A reader asks what, then, it has. We have asked. Lamps, we are told."); // 43d
    }

    // ---------------------------------------------------------------- printing

    /** The pieces of this kind that fit the reader. */
    public static List<Piece> candidates(Kind kind, Context context) {
        return PIECES.stream().filter(p -> p.kind() == kind && p.cond().test(context)).toList();
    }

    /** This week's back page: a random kind of column, with as many fitting pieces as fit on it. */
    public static MutableComponent page(Context context, RandomSource random) {
        return page(Kind.values()[random.nextInt(Kind.values().length)], context, random);
    }

    public static MutableComponent page(Kind kind, Context context, RandomSource random) {
        MutableComponent page = Component.empty().append(Component.translatable(kind.titleKey()).withStyle(ChatFormatting.BOLD));
        List<Piece> left = new ArrayList<>(candidates(kind, context));
        for (int i = 0; i < kind.perPage; i++) {
            Piece piece = pick(left, random);
            if (piece == null) {
                break;
            }
            left.remove(piece);
            page.append("\n\n").append(Component.translatable(piece.key()));
        }
        return page;
    }

    // By weight, the more particular pieces first, as Chatter picks its lines.
    private static @Nullable Piece pick(List<Piece> options, RandomSource random) {
        int total = options.stream().mapToInt(p -> p.cond().weight()).sum();
        if (total <= 0) {
            return null;
        }
        int roll = random.nextInt(total);
        for (Piece piece : options) {
            roll -= piece.cond().weight();
            if (roll < 0) {
                return piece;
            }
        }
        return null;
    }
}
