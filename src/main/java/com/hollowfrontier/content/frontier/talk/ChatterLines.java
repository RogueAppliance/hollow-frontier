package com.hollowfrontier.content.frontier.talk;

import static com.hollowfrontier.content.frontier.talk.Cond.act;
import static com.hollowfrontier.content.frontier.talk.Cond.only;
import static com.hollowfrontier.content.frontier.talk.Cond.when;
import static com.hollowfrontier.content.frontier.talk.Fact.*;
import static com.hollowfrontier.content.frontier.talk.Moment.*;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Everything the frontier says, by voice and moment, with the English (the lang provider reads it from here, so a line
 * and its text can't drift apart). A speaker has a few voices, most particular first ({@link Chatter#voices}): a
 * wrangler speaks as "wrangler" and as "townsfolk", Josie as "josie" and as "barmaid". Lines from before phase 19 keep
 * their old lang keys; new ones are {@code chatter.hollowfrontier.<voice>.<moment>.<n>}. Overheard talk is a list of
 * {@link Exchange}s, said turn about by two townsfolk.
 */
public final class ChatterLines {
    private ChatterLines() {}

    public record Line(String voice, Moment moment, String key, String text, Cond cond) {}

    /** Two or three lines of gossip, said by one townsperson, then the other, then the first. */
    public record Exchange(String key, Cond cond, List<String> texts) {
        public String key(int line) {
            return key + "." + (line + 1);
        }
    }

    private static final List<Line> LINES = new ArrayList<>();
    private static final List<Exchange> EXCHANGES = new ArrayList<>();
    private static final Map<String, Integer> COUNTS = new HashMap<>();
    private static String voice = "";

    public static List<Line> lines() {
        return LINES;
    }

    public static List<Exchange> exchanges() {
        return EXCHANGES;
    }

    /** How many lines there are in all, counting each line of gossip. */
    public static int count() {
        return LINES.size() + EXCHANGES.stream().mapToInt(e -> e.texts().size()).sum();
    }

    // ---------------------------------------------------------------- building the table

    private static void voice(String name) {
        voice = name;
    }

    private static void say(Moment moment, String text) {
        say(moment, Cond.ANY, text);
    }

    private static void say(Moment moment, Cond cond, String text) {
        String pool = voice + "." + moment.id();
        int n = COUNTS.merge(pool, 1, Integer::sum);
        LINES.add(new Line(voice, moment, "chatter.hollowfrontier." + pool + "." + n, text, cond));
    }

    /** A line from before phase 19, under its old lang key. */
    private static void old(Moment moment, Cond cond, String key, String text) {
        LINES.add(new Line(voice, moment, "message.hollowfrontier." + key, text, cond));
    }

    private static void gossip(Cond cond, String... texts) {
        EXCHANGES.add(new Exchange("chatter.hollowfrontier.gossip." + (EXCHANGES.size() + 1), cond, List.of(texts)));
    }

    static {
        townsfolk();
        folk();
        barmaids();
        lawmen();
        outlaws();
        others();
        gossip();
        december();
    }

    // ---------------------------------------------------------------- town

    private static void townsfolk() {
        voice("townsfolk");
        say(GREET, Cond.ANY.unless(NIGHT), "Afternoon, stranger.");
        say(GREET, "Howdy.");
        say(GREET, Cond.ANY.unless(NIGHT, RAIN), "Hot one today. Keep your hat on.");
        say(GREET, when(NIGHT), "Evening. Mind the dark past the last lamp.");
        say(GREET, when(RAIN), "Wet enough for you?");
        say(GREET, when(STORM), "Hear that thunder? Horses'll be skittish all night.");
        say(GREET, when(WANTED).unless(OUTLAW), "...I didn't see you. I don't see anybody.");
        say(GREET, when(OUTLAW), "That's the face off the board! Somebody fetch the sheriff!");
        say(GREET, when(RAID), "Get inside! Get off the street!");
        say(GREET, when(RAID), "They're shooting up the bank! Run!");
        say(GREET, when(ROBBED), "Quiet since the robbery. Folks are counting their pennies.");
        // 21b: what nobody else saw, and coming to blows
        say(GREET, when(SEEING_THINGS), "Saw what? Nobody saw nothing. Go and sleep it off.");
        say(BRAWL, "You take that back about my mule!");
        say(BRAWL, "Hold my beer! No, really, hold it, I paid for it!");
        // 21c: the stranger at the bar, half-noticed, and found out
        say(GREET, when(STRANGER), "Don't stare, but the tall fella in the big hat ain't blinked since Tuesday.");
        say(GREET, when(STRANGER), "New fella in town. Says he's from Elsewhere. Never heard of it.");
        say(EXPOSED, "It's one of THEM!");
        say(EXPOSED, "I knew nobody tips that well!");
        say(EXPOSED, "Lock up the cattle!");
        say(GREET, when(POSTER), "Out after %s, are you? Good luck to you.");
        say(GREET, when(OUTFIT), "Now there's somebody who looks like they belong out here.");
        say(GREET, when(CROWN), "Lord above, is that a crown? Real gold?");
        // 29: the dust devils, and what they take
        say(GREET, when(DUST_DEVIL), "Dust devil came right down the street today. Took the washing clean off the line.");
        say(GREET, when(LOST_HAT), "Lost your hat? Dust devil, was it? It'll turn up in Kansas.");
        say(GREET, when(LOST_HAT), "Nobody out here goes bareheaded on purpose, friend. Go find your hat.");
        say(TALK, when(DUST_DEVIL), "Sweep the boardwalk in the morning, and a dust devil puts it all back by noon.");
        say(TALK, when(LOST_HAT), "There's hats sold at the store. You'll want a chin strap this time.");
        say(GREET, act(Act.GATEWAY), "You've got a look about you. Like you've been somewhere nobody's been.");
        say(GREET, act(Act.AFTER), "Feels lighter out here lately. Can't say why.");

        say(TALK, "Prices are fair. That's more than you'll say for the next town.");
        say(TALK, "Coin only. Paper money's for kindling out here.");
        say(TALK, "Take your time. Nobody hurries on the frontier but the undertaker.");
        say(TALK, "Heading out? Buy more caps than you think you'll need.");
        say(TALK, when(NIGHT), "Burning the lamp late for you. Don't make a habit of it.");
        say(TALK, when(RAIN), "Rain's good for the wheat and bad for business.");
        say(TALK, when(ROBBED), "Everything's dearer since the gang hit the bank. Can't be helped.");
        say(TALK, when(ROBBED), "They took the payroll and half our nerve. Prices are up a coin.");
        say(TALK, when(RAID), "Not now! Can't you hear the shooting?");
        say(TALK, when(WANTED), "I'll sell to you. Once. Then you'd best ride on.");
        say(TALK, when(SUSPECT), "Fella in a black coat was asking after you. Polite. Too polite.");
        say(TALK, when(HUNTED), "Those men in black are looking for you. Don't bring 'em in here!");
        say(TALK, when(POSTER), "%s? I'd know that face. Rode through last spring and never paid for his beans.");
        say(TALK, when(POSTER), "That poster's worth more than my whole stock. Bring him in, would you?");
        say(TALK, when(OUTFIT), "Fine duster. Chaps too. You ride for an outfit, or for yourself?");
        say(TALK, when(SIGIL), "That pendant you carry hums, doesn't it? I can hear it from here.");
        say(TALK, when(CROWN), "Wear that crown into the saloon and you won't keep it long.");
        say(TALK, act(Act.GATEWAY), "They say there's a hole at the top of the world. Fella at the bar swears he fell down it.");
        say(TALK, act(Act.GATEWAY), "You smell of ferns. Where'd you find ferns out here?");
        say(TALK, only(Act.GRAYS), "Folks are locking their doors at night. Not for outlaws. For the lights.");
        say(TALK, only(Act.GRAYS), "My cousin lost a whole night. Went to bed, woke up in the corn. Ain't been right since.");
        say(TALK, act(Act.AFTER), "No lights in the sky for weeks. I almost miss 'em. Almost.");
        say(TALK, act(Act.AFTER), "Some of the drifters came back to themselves. One's mending my roof. Good worker.");
        old(TALK, when(SIGHTINGS), "grumble.sighting.1", "You see that airship go over the other night? Lights all down its length, and quiet as a cloud.");
        old(TALK, when(SIGHTINGS), "grumble.sighting.2", "Half the town was out watching the sky last night. A great cigar of lights, they say, going east.");
        old(TALK, when(SIGHTINGS), "grumble.sighting.3", "The preacher says those lights are a sign. The sheriff says they're a drunk. I don't know what I saw.");
        say(TALK, when(LANDINGS), "One of them saucers set down outside town. Sat there all night, bold as brass, with its little grey men walking round it.");
        say(TALK, when(LANDINGS), "There's a ring of burnt ground past the creek. Saucer sat in it all night. Gone by sunup.");
        say(TALK, when(LANDINGS), "They don't take folks no more. Now they just land where they please, like they own the place.");
        // 32d: the road north, for anyone who hasn't taken it
        say(TALK, only(Act.FRONTIER).unless(SURVEY_LETTER), "The Polar Survey went north two summers back. The postmaster's still holding their last letter.");
        say(TALK, when(AIRSHIP), "That airship of yours could make the pole. The Survey went on foot, and look where it got them.");
        say(TALK, when(AIRSHIP), "If you're flying north, take steam. The Survey wrote home for steam, and coal, and a compass. In that order.");
        // 50: taken, and back. The town has heard, and has settled on what happened
        say(TALK, when(TAKEN), "Found asleep in a field, the paper says. The hotel's beds are hard, I'll grant you.");
        say(TALK, when(TAKEN), "You were gone four days. Your horse came back without you. It wouldn't say where it had been, either.");
        say(TALK, when(TAKEN), "Little grey doctors. And did they send a bill?");
        say(TALK, when(TAKEN), "Carried up into the sky. My cousin says the same every payday.");
        say(TALK, when(TAKEN), "If they kept your hat, I'd let it go.");
        say(TALK, when(TAKEN), "You don't blink as much as you used to. Has anybody told you?");
        say(TALK, when(TAKEN), "Whatever you saw up there, don't tell the children. They'll want to go.");
        // 51: high noon. The whole town was at its windows
        say(TALK, when(DUEL_WON), "I watched it from the hotel window. So did everybody. The hotel charged.");
        say(TALK, when(DUEL_WON), "Twelve paces and one shot. The undertaker had the box measured before the smoke cleared.");
        say(TALK, when(DUEL_WON), "The bell rang three times and then once more, by itself. The preacher says that last one was the wind.");
        say(TALK, when(DUEL_WON), "My boy's been standing in the street all morning with a stick in his belt. I blame you, and I thank you.");
        say(TALK, when(DUEL_LOST), "You stood up. Then you lay down. Nobody's saying otherwise.");
        say(TALK, when(DUEL_LOST), "You were left your boots. From that sort, it's a compliment.");
        say(TALK, when(DUEL_LOST), "The doctor says you'll mend. He says it about everyone, mind.");
        // 43d: the Still Sea, which nobody up here believes in
        say(TALK, when(SEA_SEEN), "A sea under the ground. And I suppose the fish carry lanterns.");
        say(TALK, when(SEA_SEEN), "A sea with no waves on it? That's not a sea. That's a pond with ideas.");
        say(TALK, when(SEA_SEEN), "No tide, you say. Then what does it do all day?");
        say(TALK, when(SEA_SEEN), "You smell of kelp. We're nine hundred miles from kelp.");
        say(TALK, when(SEA_SEEN), "A town under glass at the bottom of it. Sure. And who washes the windows?");
        say(TALK, when(SEA_SEEN), "My brother sailed twenty years. Says a sea with no wind is just a place to drown slowly.");
        say(TALK, when(SEA_SEEN), "They say something long went under a fella's boat down there. He doesn't row any more.");
        say(TALK, when(SEA_SEEN), "If there's a whale down there I don't want to hear about it. I've enough to worry on with the cattle.");
    }

    private static void folk() {
        voice("shopkeeper");
        say(GREET, "Come in, come in! Everything a body needs, and some things it don't.");
        say(TALK, "Gazette's a copper. All the news, and some of it's even true.");
        say(TALK, "Got discs fresh off the train. Nothing livens up a saloon like a good rag.");
        say(TALK, "Lasso, cartridges, a hat that fits? You're in the right store.");
        say(TALK, "Credit? Friend, I don't give credit to my own brother.");
        say(TALK, when(ROBBED), "Can't restock till the payroll comes. Take what's on the shelf.");
        say(TALK, act(Act.GATEWAY), "Fella tried to sell me a crystal that glowed. I bought it. Don't tell my wife.");

        voice("banker");
        say(GREET, "Good day. Your money's safer with us than in your boots.");
        say(TALK, "Sneak a word to me and I'll open your box. A gold coin the first time, free after.");
        say(TALK, "Coins in a box earn a little every day. Come back and collect it.");
        say(TALK, "Keep your coin in the vault, not under the mattress. Mattresses catch fire.");
        old(TALK, when(ROBBED), "banker.robbed", "Cleaned us out, they did. Prices are up till the next payroll comes in.");
        say(TALK, when(ROBBED), "We'll make good on the boxes where we can. The vault is another story.");
        say(TALK, when(RAID), "Not the vault! Not again!");
        say(TALK, when(WANTED), "I'll take your deposit. I won't ask where it came from. Today.");

        voice("blacksmith");
        say(GREET, "Mind the sparks.");
        say(TALK, "Iron shoes keep a horse sound on any trail. The starsteel kind, I hear, keep it from falling at all.");
        say(TALK, "Dynamite's for mining. I don't want to hear it was for anything else.");
        say(TALK, "Good iron's honest. Can't say the same for most men.");
        say(TALK, "Bring me a broken gun and I'll tell you what you did wrong.");
        say(TALK, when(RAIN), "Damp gets in the coals. Everything comes out brittle on a wet day.");
        say(TALK, act(Act.GATEWAY), "Somebody brought me a red metal last month. Wouldn't so much as soften in my forge.");

        voice("wrangler");
        say(GREET, "Watch where you step. The herd's been through.");
        say(TALK, "I'll buy snakeskins. Rattlers keep clear of a man in snakeskin boots.");
        say(TALK, "Shoe your horse before a long ride. Don't skimp on it.");
        say(TALK, when(STORM), "Cattle smell weather before we do. They've been bunched up all day.");
        say(TALK, when(DUST_DEVIL), "A dust devil went through the corral and every horse in it stood on its hind legs. Shod ones didn't. Shoe your horse.");
        say(TALK, only(Act.GRAYS), "Herd won't graze the east pasture no more. Something burned a ring in it.");
        say(TALK, act(Act.AFTER), "Cattle are calm again. They always know before we do.");
        say(TALK, when(LANDINGS), "Something came down in the far pasture last night. The herd wouldn't go near it, and I didn't blame 'em.");
        old(TALK, when(MUTILATIONS), "grumble.mutilation.1", "Lost another steer last night. Cut up neat as you please, and no wolf did that.");
        old(TALK, when(MUTILATIONS), "grumble.mutilation.2", "Found one of the herd dead in a burned ring. Not a drop of blood. The others won't go near the spot.");
        old(TALK, when(MUTILATIONS), "grumble.mutilation.3", "Somebody's cutting up cattle out there, and it ain't rustlers. Rustlers take the meat.");
        old(TALK, when(CROP_CIRCLES), "grumble.crop_circle.1", "Wheat's been pressed flat in rings out past the edge of town. Every stalk laid the same way.");
        old(TALK, when(CROP_CIRCLES), "grumble.crop_circle.2", "There's circles in the wheat again. Perfect ones. Who walks out at night with a compass and a roller?");
        old(TALK, when(CROP_CIRCLES), "grumble.crop_circle.3", "I'd blame the kids for the rings in the wheat, if the kids could fly.");

        voice("prospector");
        say(GREET, "Gold in them hills! Or there was, till a fella from back east took it.");
        say(TALK, "Fifty years I've panned these creeks. Got me a bad back and a good story.");
        say(TALK, "Dynamite and snakeskins, that's my trade. Both bite if you're careless.");
        say(TALK, Cond.ANY.until(Act.FRONTIER), "Ever hear of Symmes? Holes at the poles, he said, and a world inside. I believe him.");
        say(TALK, when(NIGHT).until(Act.GRAYS), "I count the stars, nights. Some nights there's one too many, and it moves.");
        say(TALK, act(Act.GATEWAY), "You've been down there, ain't you? Down the hole! I knew Symmes was right!");
        say(TALK, act(Act.GATEWAY), "Fella back from the pole swears there's folk down there who glow like a lamp. Lantern Men, he calls 'em.");
        say(TALK, act(Act.AFTER), "Funny thing. Ever since the sky went quiet, I sleep like a babe.");
        say(TALK, when(LANDINGS), "Saw the saucer's lights through the trees. I went for a look, and one of the grey fellas looked back.");
        say(TALK, when(AIRSHIP), "Flying north? I've a compass that points at the pole. Not the one on the map. The other one.");
        say(TALK, when(SEA_SEEN), "Bring me anything off that sea floor. Bells, clocks, I don't care. Wet's fine. I'll dry it."); // 43d
        say(TALK, when(TAKEN), "You were up there and came back with your pockets full? Sit down. Start at the beginning. Leave nothing out."); // 50

        voice("engineer");
        say(GREET, "Mind the rails. The four-fifteen don't stop for nobody.");
        say(TALK, "I'll take your coal and your crusher dust. The smelter down the line eats it by the ton.");
        say(TALK, "Pressure valves are a devil to make. I've a spare, for a price.");
        say(TALK, "Steam's the future, friend. Horses are for folks who like waiting.");
        say(TALK, when(RAIN), "Wet rails and a heavy load. That's how a man meets his maker.");
        say(TALK, only(Act.GRAYS), "Saw a light keep pace with the night train. Forty miles an hour, and it never fell behind.");

        // 24c: the town's newer folk
        voice("doctor");
        say(GREET, "Say \"ah\". No? Suit yourself. Next time you're shot, you'll come see me.");
        say(TALK, "I can set a bone, pull a tooth or cut out a bullet. Not all at once, mind.");
        say(TALK, "That snake oil at the store is mostly whiskey. So's half my medicine.");
        say(TALK, when(WANTED), "Took three slugs out of the last fella who was wanted. Don't make it four.");

        voice("teacher");
        say(GREET, "Good day. Hat off indoors, if you please.");
        say(TALK, "Spelling bee on Friday. The prospector's boy has spelled \"gold\" with an E four weeks running.");
        say(TALK, "Reading, writing, arithmetic, and not shooting at the school bell. That last one needs saying more than you'd think.");
        say(TALK, when(NIGHT), "Marking slates by lamplight. Half of them have drawn the sheriff's cat.");

        voice("preacher");
        say(GREET, "Bless you, stranger. Wipe your boots.");
        say(TALK, "Service is Sunday at nine. The saloon closes, and so do the eyes of half my flock.");
        say(TALK, "I preach against drink, cards and dancing. The barmaid says I'm bad for business.");
        say(TALK, only(Act.GRAYS), "There are lights in the heavens that aren't in my Book. I'm writing a new sermon.");

        voice("postmaster");
        say(GREET, "Mail goes out with the stage, Mondays and Thursdays. Rain, shine or outlaws.");
        say(TALK, "A letter back east takes three weeks. A telegram takes three minutes and costs a week's wages.");
        say(TALK, "I don't read the postcards. Don't need to. Folks tell me what's in 'em anyway.");
        say(TALK, only(Act.FRONTIER).unless(SURVEY_LETTER), "There's a letter here from the Polar Survey. Addressed to anybody. Nobody's been anybody yet.");
        say(TALK, when(SURVEY_LETTER).until(Act.FRONTIER), "Read that letter yet? Don't tell me what's in it. I'll hear soon enough.");

        voice("gunsmith");
        say(GREET, "Mind the muzzle, friend. Every gun's loaded till it ain't.");
        say(TALK, "Bring me a fouled barrel and I'll make it sing. Bring me coin and it'll sing louder.");
        say(TALK, "Caps, cartridges, buckshot. Powder dry, prices fair, no credit.");
        say(TALK, when(RAID), "Raiders! Take a box of cartridges and get to a window!");

        voice("editor");
        say(GREET, "Got news? I'll print it. Got none? I'll print something.");
        say(TALK, "The Gazette goes to press at dawn. Ink on my fingers, ink in my coffee.");
        say(TALK, "Sightings, raids, bounties. If it's strange, it's front page.");
        say(TALK, when(DUST_DEVIL), "A dust devil went through the press room. Half this week's paper is somewhere over the next county.");
        say(TALK, only(Act.GRAYS), "\"Lights Over the Mesa\". Sold out by noon. Everybody's seen 'em; nobody'll say so.");
    }

    private static void barmaids() {
        voice("barmaid");
        say(GREET, "Come on in out of the dust, darlin'.");
        say(GREET, "Bar's open. Belly up.");
        say(GREET, when(NIGHT), "Late one, sugar? Bar's still open.");
        say(GREET, when(RAIN), "Come in, come in, you're drippin' on my floor.");
        say(GREET, when(DUST_DEVIL), "Shut that door, darlin', there's a dust devil out there and I just swept.");
        say(GREET, when(LOST_HAT), "Where's your hat, sugar? Don't tell me. The wind's wearin' it.");
        say(GREET, when(RAID), "Get down behind the bar and keep your head low!");
        say(TALK, when(RAID), "Not now, honey, they're shootin' up the street!");
        say(TALK, when(ROBBED), "Since the robbery folks drink more and tip less. Ain't that always the way.");
        say(TALK, when(WANTED), "I'll pour for you, but the sheriff drinks here too. Drink fast.");
        say(TALK, when(POSTER), "%s? Drank here Saturdays, till his face went up on the board.");
        say(TALK, when(OUTFIT), "Well, ain't you a picture. Full cowboy rig, and boots to match.");
        say(TALK, when(CROWN), "Honey, nobody wears a crown in a saloon. Not twice.");
        say(TALK, when(SUSPECT), "Two men in black coats sat in the corner all night. Drank water. Asked about you.");
        say(TALK, act(Act.GATEWAY), "Somebody tipped me a glowing rock. I keep it by the till. Best lamp I ever had.");
        say(TALK, only(Act.GRAYS), "Every drunk in town's seen a saucer this week. Some of 'em even sober.");
        say(TALK, act(Act.AFTER), "The drifters who come in now order coffee and say thank you. Strangest thing.");
        say(TALK, when(LANDINGS), "A fella came in with a crate of saucer metal. Says the grey ones ran off and left it. Drinks are on him.");
        // 21b: the moonshine's sightings, and the fights
        say(TALK, when(SEEING_THINGS), "A ship in the sky, all lit up? Sugar, that was the moonshine. It does that. Have a coffee.");
        say(GREET, when(SEEING_THINGS), "Back again? You saw it again, didn't you. Nobody else did. They never do.");
        say(BRAWL, "Not in my saloon! Take it outside!");
        say(BRAWL, "Somebody's paying for that chair!");
        say(BRAWL, "Every Saturday. Every blessed Saturday.");
        say(TALK, when(STRANGER), "Odd fella. Tips well, though. Tips in rocks, but they glow, so I keep 'em.");
        say(TALK, when(STRANGER), "Orders a beer and just holds it. Hour after hour. Holds it real nice.");
        say(EXPOSED, "And he never paid his tab!");

        voice("ruby");
        old(TALK, Cond.ANY, "barmaid.ruby.1", "Well now, look what the wind blew in. What'll it be, darlin'?");
        old(TALK, Cond.ANY, "barmaid.ruby.2", "Pull up a stool, stranger. The smile's free. The whiskey ain't.");
        old(TALK, Cond.ANY, "barmaid.ruby.3", "Easy on the moonshine, sugar. I'd hate for you to swoon before I do.");
        say(TALK, "The card table's in the corner. I deal fair. Mostly.");
        voice("clementine");
        old(TALK, Cond.ANY, "barmaid.clementine.1", "Howdy, sugar! Sarsaparilla's ice cold today!");
        old(TALK, Cond.ANY, "barmaid.clementine.2", "Ooh, a new face! You stayin' in town long?");
        old(TALK, Cond.ANY, "barmaid.clementine.3", "Coffee's strong enough to float a horseshoe. Want a cup, sweetie?");
        say(TALK, when(STORM), "Ooh, I hate thunder! Stay a while, would you?");
        voice("josie");
        old(TALK, Cond.ANY, "barmaid.josie.1", "You buyin', or just admirin' the scenery?");
        old(TALK, Cond.ANY, "barmaid.josie.2", "I'd deal you in, darlin', but you look like you'd lose your boots.");
        old(TALK, Cond.ANY, "barmaid.josie.3", "Snake oil cures everything but a broken heart, honey. Want some?");
        say(TALK, "My table hits soft seventeen and pays double. Sit down if you've got the nerve.");
        voice("belle");
        old(TALK, Cond.ANY, "barmaid.belle.1", "Why, welcome, stranger. Do make yourself comfortable.");
        old(TALK, Cond.ANY, "barmaid.belle.2", "My, such fine manners. Something refined for you this evening?");
        old(TALK, Cond.ANY, "barmaid.belle.3", "Mind the dust on those boots, darlin'. This is a respectable establishment.");
        say(TALK, when(WANTED), "I don't believe a word of what's on that board, darlin'. Not one word.");
        voice("lupe");
        old(TALK, Cond.ANY, "barmaid.lupe.1", "¡Hola, cariño! Mezcal, straight from my abuela's agave.");
        old(TALK, Cond.ANY, "barmaid.lupe.2", "Find the worm in your mezcal and you get a kiss... on the cheek.");
        old(TALK, Cond.ANY, "barmaid.lupe.3", "Coffee with cinnamon, the way my mamá made it. Sit, sit!");
        say(TALK, when(NIGHT), "My abuela said the lights in the sky are the dead, riding home. Have a drink, it's late.");
    }

    // ---------------------------------------------------------------- the law

    private static void lawmen() {
        voice("lawman");
        say(GREET, Cond.ANY.unless(NIGHT, WANTED), "Afternoon.");
        say(GREET, Cond.ANY.unless(WANTED), "Stranger.");
        say(GREET, when(NIGHT).unless(WANTED), "Evening. Keep to the lit streets.");
        say(GREET, when(WANTED), "I know your face. Don't give me a reason.");
        say(GREET, when(RAID), "Off the street, unless you're here to shoot!");
        say(BRAWL, "Alright, break it up! ...After this round.");
        old(TALK, when(WANTED), "lawman.warning", "I've got my eye on you, stranger. Step out of line again and you'll hang.");
        say(TALK, when(WANTED), "Pay your fine at the board and we'll call it square. Until then, I'm watching.");
        say(TALK, when(ROBBED), "They got clean away with the payroll. I'll see that gang hang if it takes all year.");
        say(TALK, when(ROBBED), "The leader's poster is on the board. Bring him in and the whole town will thank you.");
        say(TALK, when(POSTER), "%s, eh? Bring him in breathing if you can. Dead pays the same.");
        say(TALK, when(POSTER), "Got a poster on you. Good. The board pays out on his badge, mind.");
        say(TALK, when(RAID), "No time to jaw! Grab a gun or get indoors!");
        say(TALK, when(OUTFIT), "You dress like a lawman. Ever think of wearing the star?");
        say(TALK, when(SUSPECT), "Men in black dusters came by with federal papers. I didn't like them, and they didn't like you.");
        say(TALK, act(Act.GATEWAY), "Federal men keep asking about the polar country. I tell 'em what I know. Nothing.");
        say(TALK, only(Act.GRAYS), "Outlaws I can shoot. Lights in the sky I just write down.");
        say(TALK, only(Act.GRAYS), "The outlaws we jail can't remember their own mothers. Something's wrong with 'em.");
        say(TALK, act(Act.AFTER), "Half the outlaws in my cells this month just gave up. Asked for a preacher.");
        say(TALK, when(LANDINGS), "There was a saucer down near here last night. Nobody's hurt, and I mean to keep it that way. You go near it, you go armed.");
        say(TALK, when(LANDINGS), "Can't arrest a saucer. Lord knows I'd like to.");
        say(SPOT, "Hold it right there!");
        say(SPOT, "That's far enough!");
        say(SPOT, when(OUTLAW), "In the name of the law, drop that iron!");
        say(ARREST, "That's one less outlaw on my roads.");
        say(ARREST, "Somebody fetch the undertaker.");
        say(ARREST, "Should've come quietly.");
        say(POSSE, "There! That's the one on the warrant!");
        say(POSSE, "By order of the territory, you're coming in. Dead or alive.");
        say(POSSE, "Ride 'em down, boys!");
        say(MUSTER, "Raiders! Everybody to the street!");
        say(MUSTER, "Form up! Nobody touches that bank!");
        say(MUSTER, "Here they come. Steady, now.");

        voice("sheriff");
        old(TALK, Cond.ANY.unless(WANTED), "lawman.sheriff.1", "Keep your iron holstered in my town, and we'll get along fine.");
        old(TALK, Cond.ANY.unless(WANTED), "lawman.sheriff.2", "Bounties are on the board. Bring 'em in breathin' if you can.");
        say(TALK, Cond.ANY.unless(WANTED), "Jail's got three cells and a cat. The cat's the toughest thing in there.");
        say(TALK, "I've hung better men than the ones still riding.");
        // 21b: the likeness on a wanted player's poster
        say(TALK, when(WANTED), "Your poster's up. Artist never laid eyes on you, but folks say he caught something.");
        say(GREET, when(WANTED), "Didn't recognise you from the poster. That's the artist's fault, not yours.");
        voice("deputy");
        old(TALK, Cond.ANY.unless(WANTED), "lawman.deputy.1", "Sheriff says I'm to keep an eye on strangers. So... howdy.");
        old(TALK, Cond.ANY, "lawman.deputy.2", "Two barrels of buckshot says this town stays quiet.");
        say(TALK, "Sheriff lets me carry the shotgun. Says I can't miss with it. I've missed with it.");
        say(GREET, Cond.ANY.unless(WANTED), "Howdy! I mean, halt! I mean... howdy.");
        voice("marshal");
        old(TALK, Cond.ANY, "lawman.marshal.1", "Federal business. Seen any riders with a red bandana?");
        old(TALK, Cond.ANY, "lawman.marshal.2", "Law runs as far as my rifle reaches. That's a fair piece.");
        say(TALK, "I've a warrant from Washington and a horse that hasn't eaten. One of us is in a hurry.");
        say(TALK, act(Act.GRAYS), "Washington wants reports on the lights. Washington can come and look for itself.");
        voice("ranger");
        old(TALK, Cond.ANY, "lawman.ranger.1", "One riot, one ranger. Mind your manners.");
        old(TALK, Cond.ANY, "lawman.ranger.2", "Trail's been hot with outlaws. Ride careful out there.");
        say(TALK, "Thirty years I've ridden this country. Never seen it so strange.");
        say(TALK, when(STORM), "Storm like this, the outlaws hole up. So do I.");
    }

    // ---------------------------------------------------------------- outlaws

    private static void outlaws() {
        voice("outlaw");
        say(SPOT, "There's one! Light 'em up!");
        say(SPOT, "Well, well. Look who came to die.");
        say(SPOT, "You picked the wrong trail, friend.");
        say(SPOT, "Get 'im, boys!");
        say(SPOT, when(POSTER), "That a poster in your pocket? Come to collect, have you?");
        say(SPOT, only(Act.GRAYS), "It's in my head. It says you're the one.");
        say(SPOT, act(Act.AFTER), "No more voices in my head. I'm doin' this 'cause I want to.");
        say(RELOAD, "Cover me, I'm loading!");
        say(RELOAD, "Hold on, hold on, I'm dry!");
        say(RELOAD, "Powder, ball, cap... hurry, dang it!");
        say(RELOAD, "Keep 'em busy while I load!");
        say(RELOAD, "Nobody move! ...I mean it, nobody move till I'm loaded!");
        say(RELOAD, "Hold your fire a spell, would you? Much obliged.");
        say(RELOAD, when(RAIN), "Wet powder! Who loads a gun in the rain? Me, that's who.");
        say(TAUNT, "Take your time, partner. I'll wait.");
        say(TAUNT, "Powder, then the ball. You're doin' it backwards.");
        say(TAUNT, "Want me to hold your hat while you load?");
        say(TAUNT, "My grandma loads faster, and she's dead.");
        say(TAUNT, "No hurry. I'm only shootin' at you.");
        say(TAUNT, when(RAIN), "Load all you like. Your caps are wet as mine.");
        say(TAUNT, when(OUTFIT), "Fancy duster. Shame about the reloading.");
        say(HURT, "I'm hit! I'm hit bad!");
        say(HURT, "Ain't gonna end like this. Ain't gonna...");
        say(HURT, "You'll pay for that!");
        say(HURT, "Somebody... help me...");
        say(RUN, "Ride! Ride for the hills!");
        say(RUN, "That's it, boys, we're done here!");
        say(RUN, "Scatter! Meet at the old mine!");
        say(RUN, "Every man for himself!");

        voice("boss");
        say(SPOT, "Read my poster? Then you know what's coming.");
        say(SPOT, "There's a price on my head. Come and claim it.");
        say(SPOT, "Nobody brings me in. Nobody.");
        say(SPOT, when(POSTER), "That's my face on that paper. Hope you said your goodbyes.");
        say(HURT, "That all you got? I've been shot by better!");
        say(HURT, "First blood to you. Last blood's mine.");
        say(TAUNT, "Go on, load up. I'd hate to beat you unarmed.");

        voice("jailbird");
        old(JEER, Cond.ANY, "outlaw.jeer.1", "Hey! Fetch me them keys and I'll cut you in on the loot.");
        old(JEER, Cond.ANY, "outlaw.jeer.2", "My gang'll bust me outta here by sundown. You'll see.");
        old(JEER, Cond.ANY, "outlaw.jeer.3", "Beans again? A man could starve in this jail.");
        old(JEER, Cond.ANY, "outlaw.jeer.4", "Whatcha lookin' at? Ain't you never seen a desperado?");
        old(JEER, Cond.ANY, "outlaw.jeer.5", "I was framed, I tell ya! Framed!");
        old(JEER, Cond.ANY, "outlaw.jeer.6", "That cat's been starin' at me all day. Somebody make it quit.");
        old(JEER, Cond.ANY, "outlaw.jeer.7", "Sheriff's cat is the only deputy in this town worth a lick. Hissed me right into this cell.");
        say(JEER, when(WANTED), "Hey, you're on the board too! Come on in, plenty of room.");
        say(JEER, act(Act.AFTER), "It's quiet in my head now. Best sleep I've had in years, in here.");

        voice("freed");
        old(FREED, Cond.ANY, "outlaw.freed.1", "...where am I? What have I been doing?");
        old(FREED, Cond.ANY, "outlaw.freed.2", "It's quiet. It's never been quiet before.");
        old(FREED, Cond.ANY, "outlaw.freed.3", "I don't want this gun. I never did.");
        say(GREET, "Do you know what town this is? I used to know.");
        say(GREET, "I had a wife. I think I had a wife.");
        say(GREET, "I don't know what you did. But thank you.");

        // 20: held in a Gray pen. Through the bars, then once they're out.
        voice("captive");
        say(GREET, "Hey! Hey, you're no Gray. Get me out of here.");
        say(GREET, "Keep your voice down. They hear everything. Then get me out.");
        say(GREET, "Is it day up there? I can't tell any more.");
        say(GREET, "The button's on your side of the bars, friend. I've been looking at it for a month.");
        say(GREET, only(Act.GRAYS), "They took the fella in the next cell to the table. He came back wrong.");
        say(GREET, act(Act.AFTER), "They stopped coming for the table. Just stopped. Something's got 'em spooked.");
        say(RESCUED, "There was a light over the barn. Then it was morning, and I was here. My wife'll think I ran off.");
        say(RESCUED, "I lost one night out on the trail. Just the one. Turned out to be most of a year.");
        say(RESCUED, "Two men in black coats came round the week before. Asked my neighbours if I slept sound.");
        say(RESCUED, "A man in a black coat paid my bar tab. I should've known. Nobody pays a bar tab.");
        say(RESCUED, "They put me on a cold table. Cold as a creek in January. Then the humming started.");
        say(RESCUED, "They don't talk. They look at you and you know what they mean. Mostly it's 'hold still'.");
        say(RESCUED, "Six weeks in that cell and not one cup of coffee. Write that down. That's the real crime.");
        say(RESCUED, act(Act.AFTER), "They left us down here when the sky went quiet. Forgot us, I reckon. I'm glad somebody didn't.");
    }

    // ---------------------------------------------------------------- the men in black, the Kindled

    private static void others() {
        voice("man_in_black");
        old(TALK, Cond.ANY, "man_in_black.greeting.1", "Fine evening. You haven't seen anything strange in the sky, have you? No. Of course not.");
        old(TALK, Cond.ANY, "man_in_black.greeting.2", "We're with the government. Of a sort.");
        old(TALK, Cond.ANY, "man_in_black.greeting.3", "Weather balloon. That's all it ever is. A weather balloon.");
        say(TALK, act(Act.GATEWAY), "You've been north. Further north than there is. We'd know.");
        say(TALK, when(CROWN), "That's not yours. Nothing down there is yours.");
        say(TALK, when(SIGIL), "The Lantern Men gave you that, didn't they. They aren't your friends.");
        // 50: somebody who was kept, and is standing here anyway
        say(TALK, when(TAKEN), "You fell asleep in a field. You were tired. You'd been working hard.");
        say(TALK, when(TAKEN), "Nobody leaves the way you say you left. So you didn't.");
        say(TALK, when(TAKEN), "You've an itch behind the ear, I expect. Leave it be.");
        // 21a: cover stories for what's been seen near here, each a little worse than the last
        say(TALK, when(SIGHTINGS), "Those lights? The planet Venus. She's very bright this time of year.");
        say(TALK, when(SIGHTINGS), "Geese. A flock of geese, lit from below. By the moon.");
        say(TALK, when(SIGHTINGS), "Venus again. She gets about.");
        say(TALK, when(MUTILATIONS), "Coyotes. Very tidy coyotes.");
        say(TALK, when(MUTILATIONS), "The steer had a weak heart. Then it fell on a scalpel.");
        say(TALK, when(CROP_CIRCLES), "Wind. A very orderly wind.");
        say(TALK, when(CROP_CIRCLES), "Ground squirrels. They're organised out here.");
        say(TALK, when(SIGHTINGS, MUTILATIONS), "Venus, swamp gas, coyotes. It's all been explained. Please stop asking.");
        // 29: the dust devil, an explanation for everything
        say(TALK, when(CROP_CIRCLES), "A dust devil. They turn whichever way they please.");
        say(TALK, when(SIGHTINGS), "A dust devil picked up a lantern and carried it off. Happens all the time.");
        say(TALK, when(MUTILATIONS), "Dust devil. A very sharp one.");
        old(WARN, Cond.ANY, "man_in_black.warning.1", "You've been asking about things that don't concern you. Stop.");
        old(WARN, Cond.ANY, "man_in_black.warning.2", "There's a lot of empty country out there. Folks get lost in it. Folks who go looking.");
        old(WARN, Cond.ANY, "man_in_black.warning.3", "Whatever you think you saw, you didn't. We'd like you to remember that.");
        old(BOUGHT, Cond.ANY, "man_in_black.bought.1", "Obliged. Best forget where you found it.");
        old(BOUGHT, Cond.ANY, "man_in_black.bought.2", "That belongs to some friends of ours. They'll be glad of it.");
        old(BOUGHT, Cond.ANY, "man_in_black.bought.3", "A fair price for something you never had.");
        old(FREED, Cond.ANY, "man_in_black.freed", "...I was promised a long life. Funny. I think I'd rather have my own.");

        // 21c: a stranded Gray doing its best
        voice("disguised_gray");
        say(GREET, "Howdy, fellow human. I, too, enjoy... beans.");
        say(GREET, "Greetings, cow-person. I am also a cow-person.");
        say(GREET, when(NIGHT), "Good evening. I will now drink this beverage. Later.");
        say(TALK, "One beverage of fermented grain, please. For drinking. With my mouth.");
        say(TALK, "I am from the town of... Elsewhere. It is a normal town. We have horses there.");
        say(TALK, "This is a fine hat. It is my hat. I have always had this hat.");
        say(TALK, "Yee. And, if I may, haw.");
        say(TALK, when(SIGIL), "That sign you carry. I do not know it. I do not know what a Lantern Man is. Good day.");
        voice("mr_gray");
        say(GREET, "Ah. A fellow regular. Pull up a stool. I have pulled up mine.");
        say(TALK, "The usual, barkeep. I will look at it for an hour, as is our custom.");
        say(TALK, "I have studied your species for a long time. The coffee is still a mystery to me.");
        say(TALK, "You have a Sarsaparilla? I will give you something shiny for it. Please do not ask what it does.");
        say(TALK, "The cards. The piano. The fighting on Saturdays. I find I would rather be here than up there.");
        say(TALK, when(WANTED), "Your face is on the board. The artist has not captured your eyes. I know something about eyes.");

        voice("kindled");
        old(TALK, Cond.ANY, "kindled.greet.0", "Walk softly here. The stones remember.");
        old(TALK, Cond.ANY, "kindled.greet.1", "The guardians know you. They will not trouble you while you keep faith.");
        old(TALK, Cond.ANY, "kindled.greet.2", "Bring us what the grey ones leave behind. Better in our hands than theirs.");
        old(TALK, Cond.ANY, "kindled.greet.3", "There were thousands of us once. Now there are the few you see.");
        say(TALK, when(CROWN), "The crown of the Sun's keeper. Wear it well. Many died for it.");
        say(TALK, when(SIGIL), "You carry our sign. Our guardians know you by it.");
        say(TALK, only(Act.GRAYS), "The grey ones grow bold above. We feel their ships pass through the crust.");
        say(TALK, act(Act.AFTER), "The leash is broken. For the first time in an age, the grey ones are afraid.");
    }

    // ---------------------------------------------------------------- a frontier December (35d)

    /**
     * What the frontier says in December ({@link Fact#DECEMBER}) and over Christmas itself ({@link Fact#CHRISTMAS}), voice
     * by voice. Three threads run through it: the lights in the sky become a sleigh (the children and old-timers decide it,
     * the Gazette corrects them, the men in black adopt it); the Gray at the church social; and the Kindled, who live under
     * a sun that never sets and have the month explained to them. Nobody names the driver of the sleigh.
     */
    private static void december() {
        voice("townsfolk");
        say(GREET, when(DECEMBER), "Merry Christmas, stranger. Mind the tree. It's the only one for forty miles.");
        say(GREET, when(DECEMBER).unless(RAIN), "No snow again this year. We hung the washing out to look festive.");
        say(GREET, when(CHRISTMAS), "Merry Christmas! Even the gang's stayed home. Don't ask how I know.");
        say(TALK, when(DECEMBER), "Tree came in on the railroad. Cost more than the church bell. Smells better.");
        say(TALK, when(DECEMBER), "My boy asked for snow. I gave him a bucket of sand and told him to use his imagination.");
        say(TALK, when(DECEMBER, SIGHTINGS), "Them lights last night? The children say it was a sleigh. I've decided they're right. I sleep better.");
        say(TALK, when(CHRISTMAS, STRANGER), "The tall fella sang every carol at the social. Half a beat behind. Every one.");
        say(TALK, when(CHRISTMAS, STRANGER), "New fella brought a gift to the social. It hums. Nobody's opened it.");

        voice("shopkeeper");
        say(TALK, when(DECEMBER), "Fruitcake? No. I don't buy them. Nobody buys them. They just arrive.");
        voice("banker");
        say(TALK, when(DECEMBER), "The bank shuts Christmas Day. The vault isn't festive. Leave it be.");
        voice("wrangler");
        say(TALK, when(DECEMBER), "Horses don't know it's Christmas. I told 'em. Made no difference.");
        voice("prospector");
        say(TALK, when(DECEMBER, SIGHTINGS), "Lights over the ridge, fast, no sound. That's a sleigh. Saw one in '49. Didn't stop then neither.");
        voice("doctor");
        say(TALK, when(DECEMBER), "Eggnog is not medicine. I'm asked every year.");
        voice("teacher");
        say(TALK, when(DECEMBER, SIGHTINGS), "The children drew what they saw in the sky. Eleven sleighs and one very round plate. I've kept the plate.");
        voice("preacher");
        say(TALK, when(DECEMBER), "Carol service is Sunday. Come and sing. We need somebody who knows the second verse.");
        voice("postmaster");
        say(TALK, when(DECEMBER), "Parcels up to the rafters. Half of 'em are fruitcake. You can tell by the lifting.");
        voice("editor");
        say(TALK, when(DECEMBER, SIGHTINGS), "The Gazette's position is that it was not a sleigh. Sales are down.");

        voice("barmaid");
        say(GREET, when(DECEMBER), "Come in, darlin'. Mind the mistletoe. Nobody's stood under it on purpose yet.");
        say(TALK, when(DECEMBER), "Eggnog or hot cider, sugar? December only. Come January I pour it out.");
        say(TALK, when(CHRISTMAS, STRANGER), "The quiet fella ordered an eggnog. Held it an hour. Then he asked me what a nog was.");

        voice("lawman");
        say(TALK, when(DECEMBER).unless(WANTED), "Quiet month. Even outlaws have mothers.");
        voice("sheriff");
        say(TALK, when(CHRISTMAS).unless(WANTED), "No raids over Christmas. Nobody agreed to it. It just happens.");
        voice("jailbird");
        say(JEER, when(DECEMBER), "They brought me a plate. Turkey, they said. I've met that turkey. It was a crow.");
        say(JEER, when(CHRISTMAS), "Christmas dinner on a tin plate. Gravy was warm, mind. Don't tell the sheriff I said so.");

        // The sleigh, as a cover story: worse each time
        voice("man_in_black");
        say(TALK, when(DECEMBER, SIGHTINGS), "It was a sleigh.");
        say(TALK, when(DECEMBER, SIGHTINGS), "It was a sleigh. We have looked into it.");
        say(TALK, when(DECEMBER, SIGHTINGS), "It was a sleigh. We have looked into it. You may stop asking.");
        say(TALK, when(DECEMBER, SIGHTINGS), "The sleigh has been accounted for. So have the reindeer. There were the usual number of reindeer.");

        voice("disguised_gray");
        say(GREET, when(DECEMBER), "Merry December, fellow human. I have brought a gift. It is a rock. It is wrapped.");
        say(TALK, when(CHRISTMAS), "I have sung the carol. All of it. I began when you had finished, to be polite.");

        voice("kindled");
        say(TALK, when(DECEMBER), "A shortest day. We have no word for it. Our sun does not leave. I am sorry that yours does.");
        say(TALK, when(DECEMBER), "Your sun goes a little further each day, and so you light candles and sing. That is sad. It is also brave.");

        gossip(when(DECEMBER), "Tree's up in the square.", "Came by rail. Dropped half its needles at the depot.", "Depot looks lovely.");
        gossip(when(DECEMBER).unless(RAIN), "Think it'll snow for Christmas?", "It hasn't snowed here since the town was founded.", "So it's due.");
        gossip(when(DECEMBER, SIGHTINGS), "The little ones say the lights were a sleigh.", "And what do you say?", "I say it's cheaper than the truth.");
        gossip(when(CHRISTMAS), "No sign of the gang all week.", "Christmas. Even they keep it.", "That's what worries me.");
        gossip(when(CHRISTMAS, STRANGER), "That stranger sang at the social.", "Sang well?", "Sang late. Half a beat. Like an echo with a hat on.");
    }

    // ---------------------------------------------------------------- overheard

    private static void gossip() {
        gossip(Cond.ANY, "Did you hear the Hendersons' girl is sweet on the new deputy?", "The one who shot his own hat? Lord help her.");
        gossip(Cond.ANY, "Prices at the store went up again.", "Everything goes up out here but the rain.");
        gossip(Cond.ANY, "My boy wants to be an outlaw when he grows up.", "Better than a lawyer.", "We don't have a lawyer.");
        gossip(Cond.ANY.unless(RAIN), "Not a cloud in a week.", "The well's getting low. Pray for rain.");
        gossip(when(RAIN), "Rain at last.", "Mud for a week, then.", "You'd complain if it rained gold.");
        gossip(when(STORM), "Hear that thunder?", "Better thunder than gunfire.");
        gossip(when(NIGHT), "Lamp's still on at the saloon.", "Lamp's always on at the saloon. That's the point of a saloon.");
        gossip(when(ROBBED), "They say the gang took every coin in the vault.", "Not every coin. I keep mine in my boot.", "Then I know where they'll look next time.");
        gossip(when(RAID), "Is it over? Are they gone?", "Hush! Not yet!");
        gossip(when(WANTED), "Don't look now. That one's wanted.", "Wanted for what?", "Does it matter? Don't look.");
        gossip(when(POSTER), "That stranger's carrying a poster for %s.", "Good. That one stole my mule.");
        gossip(when(OUTFIT), "Now that's a proper cowboy.", "Proper cowboys smell worse.");
        gossip(when(SUSPECT), "Men in black coats were at the hotel asking questions.", "About what?", "About that one there. Keep walking.");
        gossip(when(SIGHTINGS), "You see the lights last night?", "I ain't saying I saw 'em. But I saw 'em.");
        gossip(when(DUST_DEVIL), "See that dust devil this morning?", "Took my hat, my hen and my good mood.", "Hen came back. Hat didn't.");
        gossip(when(LOST_HAT), "That one's lost a hat.", "Dust devil?", "Or a saucer. Either way, it ain't coming back.");
        gossip(when(MUTILATIONS), "Another steer, cut up neat. No blood.", "Wolves.", "Wolves don't carry a scalpel.");
        gossip(when(CROP_CIRCLES), "Rings in the wheat again.", "Kids.", "Kids can't fly.");
        gossip(when(LANDINGS), "Hear about the saucer that set down?", "Set down? I thought they only flew over.", "Not any more, they don't.");
        gossip(when(LANDINGS), "They say whoever runs the grey ones off gets what they leave behind.", "What do they leave?", "Ask the fella buying rounds at the saloon.");
        gossip(act(Act.GATEWAY), "Heard somebody went down the hole at the pole and came back.", "Came back from where? There's nothing up there but ice.", "That's what I said.");
        gossip(only(Act.GRAYS), "Martha says she was taken up in a light and lost a whole night.", "Martha says a lot of things.", "She's got a mark behind her ear, though.");
        gossip(act(Act.AFTER), "Sky's been quiet.", "Too quiet?", "No. Just quiet. It's nice.");
        gossip(when(STRANGER), "That stranger asked me where the humans drink.", "What'd you tell him?", "The saloon. Where else would humans drink?");
        gossip(when(STRANGER), "Ever notice the new fella's hat's three sizes too big?", "Maybe his head's small.", "His head ain't small.");
        gossip(only(Act.FRONTIER).unless(SURVEY_LETTER), "Any word from the Polar Survey?", "Not since the letter.", "What letter?"); // 32d
        gossip(when(SEA_SEEN), "That one says there's a sea under the ground.", "Under whose ground?", "Everybody's, the way they tell it."); // 43d
        gossip(when(TAKEN), "That one says they were carried off into the sky.", "Carried off by what?", "Doctors, to hear it told."); // 50
        gossip(when(TAKEN), "Gone four days, that one.", "Gone where?", "Up.");
        gossip(when(DUEL_WON), "That's the one from the street at noon.", "Quick?", "The other one thought so too."); // 51
        gossip(when(DUEL_LOST), "That's the one who got shot at noon.", "Shot, and walking?", "It was only the purse that was wanted.");
        gossip(when(SEA_SEEN), "They say you can ride a brass kettle to the bottom of it.", "And back up?", "They didn't say.");
    }
}
