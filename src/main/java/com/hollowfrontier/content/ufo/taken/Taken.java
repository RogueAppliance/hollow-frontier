package com.hollowfrontier.content.ufo.taken;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import org.jspecify.annotations.Nullable;

import com.hollowfrontier.HollowFrontier;
import com.hollowfrontier.content.frontier.bounty.BoardUse;
import com.hollowfrontier.content.frontier.mystery.MysteryLog;
import com.hollowfrontier.content.frontier.mystery.Suspicion;
import com.hollowfrontier.content.frontier.stake.StakeText;
import com.hollowfrontier.content.hollow.gray.Specimens;
import com.hollowfrontier.content.ufo.Abductions;
import com.hollowfrontier.content.ufo.CargoPodBlock;
import com.hollowfrontier.content.ufo.GrayWarden;
import com.hollowfrontier.content.ufo.Leash;
import com.hollowfrontier.content.ufo.UfoConfig;
import com.hollowfrontier.registry.ModAttachments;
import com.hollowfrontier.registry.ModBlocks;
import com.hollowfrontier.registry.ModCriteria;
import com.hollowfrontier.registry.ModItems;
import com.hollowfrontier.registry.ModSounds;
import com.hollowfrontier.registry.ModTags;
import com.hollowfrontier.worldgen.Aboard;
import com.hollowfrontier.worldgen.structure.ShipBlueprints;
import com.hollowfrontier.worldgen.structure.UfoBlueprints;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.GlobalPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructurePlaceSettings;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplate;
import net.minecraft.world.level.portal.TeleportTransition;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.LivingChangeTargetEvent;
import net.neoforged.neoforge.event.entity.living.LivingDeathEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;
import net.neoforged.neoforge.event.level.BlockEvent;
import net.neoforged.neoforge.event.level.ExplosionEvent;
import net.neoforged.neoforge.event.server.ServerStoppedEvent;
import net.neoforged.neoforge.event.tick.LevelTickEvent;

/**
 * Taken (50). The Grays' beam used to cost a player an hour and a walk home. Now, once they have reason to keep somebody
 * ({@link #keeps}), that player wakes aboard: in a pen, with nothing on them, on a {@link Ship} of the Grays' in a place of
 * its own ({@link Aboard}), and has to get off it.
 * <ul>
 * <li><b>Who's kept.</b> The first time, a player the men in black already mind ({@link UfoConfig#takenSuspicion}); after
 *     that, one taking in {@link UfoConfig#takenAgain}. Never in creative, never once the leash is broken, never with
 *     {@code taken} or {@code abductPlayers} off. Everyone else gets missing time, as before.</li>
 * <li><b>The kit.</b> What the player had on them is held in {@link TakenData} and is found aboard in the hold. It is
 *     always given back: by the pod in the hold, or by {@link #finish} on every way off.</li>
 * <li><b>The ways off</b> ({@link Way}): a pod; a fall out of the hull; death (which isn't one: the player is set down
 *     alive); the ship going down with them aboard; leaving the game (set down when they come back); being moved out by
 *     something else. All but the pod end as missing time does, near where they were taken.</li>
 * <li><b>The ship can't be dug through.</b> Only what's tagged {@code taken_breakable} breaks, nothing is placed, and
 *     a blast breaks nothing (but one beside the core finishes the engine).</li>
 * </ul>
 */
@EventBusSubscriber(modid = HollowFrontier.MODID)
public final class Taken {
    private Taken() {}

    public enum Way { POD, FELL, DIED, CRASHED, LEFT }

    /** How far apart ships are set, and how high. */
    public static final int GRID = 256, SHIP_Y = 100;
    /** How long a ship still takes on others caught in the same beam, and how near they must have been taken. */
    public static final int JOIN_TICKS = 200;
    public static final double JOIN_RANGE = 48.0;
    public static final int SUSPICION_OUT = 15, SUSPICION_DOWN = 25;
    /** How far from where the pod comes down a wrecked ship does. */
    public static final int CRASH_MIN = 160, CRASH_MAX = 420;

    private static final Map<Integer, Ship> SHIPS = new HashMap<>();
    /** Players this class is moving off a ship right now (so their change of world isn't taken for somebody else's doing). */
    private static final Set<UUID> MOVING = new HashSet<>();
    private static ResourceKey<Level> where = Aboard.LEVEL;
    private static int setDownMin = Abductions.MISSING_MIN, setDownMax = Abductions.MISSING_MAX;

    /**
     * Game tests only: the game test server loads no datapack dimensions, so the tests raise their ships in another world
     * and set players down {@code near} it. Null puts things back.
     */
    public static void standIn(@Nullable ResourceKey<Level> level, int near) {
        where = level == null ? Aboard.LEVEL : level;
        setDownMin = level == null ? Abductions.MISSING_MIN : 0;
        setDownMax = level == null ? Abductions.MISSING_MAX : near;
    }

    public static @Nullable ServerLevel aboard(MinecraftServer server) {
        return server.getLevel(where);
    }

    // ---------------------------------------------------------------- who's kept

    /** Whether the Grays keep a player their beam has drawn up, rather than setting them down an hour later. */
    public static boolean keeps(ServerLevel level, ServerPlayer player) {
        if (!UfoConfig.taken() || !UfoConfig.abductPlayers() || Leash.isBroken(level) || player.isCreative() || player.isSpectator()
                || aboard(level.getServer()) == null || shipOf(player) != null) {
            return false;
        }
        TakenData data = TakenData.of(player);
        return data.times() == 0 ? Suspicion.level(player) >= UfoConfig.takenSuspicion() : level.getRandom().nextDouble() < UfoConfig.takenAgain();
    }

    /** The player wakes aboard (no checks: {@link #keeps} makes them). The ship, or null if there was nowhere to put one. */
    public static @Nullable Ship take(ServerLevel level, ServerPlayer player) {
        ServerLevel aboard = aboard(level.getServer());
        if (aboard == null) {
            return null;
        }
        Ship on = shipOf(player);
        if (on != null) {
            return on; // already aboard: nobody is taken off a ship
        }
        int x = player.getBlockX(), z = player.getBlockZ();
        GlobalPos here = GlobalPos.of(level.dimension(), new BlockPos(x, level.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, x, z), z));
        GlobalPos from = level.dimension() != Aboard.LEVEL ? here
                // Taken from the ship's own world (a command run there): there's no ground under it to be put back on
                : GlobalPos.of(Level.OVERWORLD, level.getServer().overworld().getRespawnData().pos());
        Ship ship = SHIPS.values().stream().filter(s -> s.age() <= JOIN_TICKS && !s.players().isEmpty() && takenNear(level, s, from)).findFirst().orElse(null);
        boolean first = ship == null;
        if (first) {
            ship = raise(aboard);
        }
        player.stopRiding();
        player.removeEffect(MobEffects.LEVITATION);
        TakenData.board(player, from);
        TakenData.stash(player);
        ship.add(player);
        Vec3 pen = ShipLayout.feet(ship.origin(), first ? ShipLayout.WAKE : ShipLayout.penMiddle(ShipLayout.PENS.length - 1));
        move(player, aboard, pen, 0.0F); // facing the pen's front, and whatever comes to it
        player.resetFallDistance();
        player.setHealth(player.getMaxHealth());
        // The Grays keep their subjects fed and mended: nobody wakes hungry or hurt
        player.getFoodData().setFoodLevel(20);
        player.getFoodData().setSaturation(10.0F);
        player.addEffect(new MobEffectInstance(MobEffects.BLINDNESS, 110, 0, false, false));
        player.addEffect(new MobEffectInstance(MobEffects.NAUSEA, 200, 0, false, false));
        player.addEffect(new MobEffectInstance(MobEffects.SLOWNESS, 240, 1, false, false));
        player.setData(ModAttachments.ABOARD.get(), Ship.Stage.QUIET.code);
        Suspicion.add(player, Abductions.SUSPICION);
        ModCriteria.fire(player, ModCriteria.TAKEN_ABOARD);
        return ship;
    }

    private static boolean takenNear(ServerLevel level, Ship ship, GlobalPos from) {
        for (UUID id : ship.players()) {
            ServerPlayer other = level.getServer().getPlayerList().getPlayer(id);
            Optional<GlobalPos> theirs = other == null ? Optional.empty() : TakenData.of(other).from();
            if (theirs.isPresent() && theirs.get().dimension() == from.dimension() && theirs.get().pos().closerThan(from.pos(), JOIN_RANGE)) {
                return true;
            }
        }
        return false;
    }

    /** A ship with nobody on it, set down in the first free place of the grid. */
    public static Ship raise(ServerLevel aboard) {
        int index = 0;
        while (SHIPS.containsKey(index)) {
            index++;
        }
        // Away from a world's own ground where the ship is standing in for the real place (the game tests)
        boolean real = aboard.dimension() == Aboard.LEVEL;
        BlockPos origin = new BlockPos((real ? 0 : 30000) + (index % 32) * GRID, real ? SHIP_Y : 200, (real ? 0 : 30000) + (index / 32) * GRID);
        Ship ship = new Ship(index, aboard.dimension(), origin);
        ship.raise(aboard);
        SHIPS.put(index, ship);
        return ship;
    }

    public static @Nullable Ship shipOf(Player player) {
        for (Ship ship : SHIPS.values()) {
            if (ship.players().contains(player.getUUID())) {
                return ship;
            }
        }
        return null;
    }

    public static @Nullable Ship shipAt(Level level, BlockPos pos) {
        for (Ship ship : SHIPS.values()) {
            if (ship.dimension() == level.dimension() && ship.contains(pos)) {
                return ship;
            }
        }
        return null;
    }

    public static List<Ship> ships() {
        return List.copyOf(SHIPS.values());
    }

    // ---------------------------------------------------------------- the two voices

    /** A line of the player's own, as their journal would have it. */
    public static void note(ServerPlayer player, String key, Object... args) {
        player.sendSystemMessage(Component.translatable(key, args).withStyle(ChatFormatting.GRAY, ChatFormatting.ITALIC));
    }

    /** Something one of the held says through the field. */
    public static void says(ServerPlayer player, @Nullable Entity speaker, String key) {
        if (speaker != null && speaker.isAlive()) {
            player.sendSystemMessage(Component.translatable("message.hollowfrontier.npc.say", speaker.getDisplayName(), Component.translatable(key)));
        }
    }

    // ---------------------------------------------------------------- the ways off

    private static void move(ServerPlayer player, ServerLevel to, Vec3 at, float yaw) {
        MOVING.add(player.getUUID());
        try {
            player.teleport(new TeleportTransition(to, at, Vec3.ZERO, yaw, 0.0F, TeleportTransition.DO_NOTHING));
        } finally {
            MOVING.remove(player.getUUID());
        }
    }

    /** Off the ship's books, with the kit back in hand. How many stacks of it came back. */
    public static int finish(ServerPlayer player) {
        Ship ship = shipOf(player);
        if (ship != null) {
            ship.remove(player);
        }
        player.setData(ModAttachments.ABOARD.get(), 0);
        int back = TakenData.restore(player);
        if (back > 0) {
            note(player, TakenText.NOTE_KIT_BACK);
        }
        return back;
    }

    /** Off the ship any way but a pod: set down near where they were taken, as from missing time. */
    public static void leave(ServerPlayer player, Way way) {
        finish(player);
        setDown(player, way);
    }

    private static ServerLevel groundFor(ServerPlayer player) {
        MinecraftServer server = player.level().getServer();
        ServerLevel ground = TakenData.of(player).from().map(from -> server.getLevel(from.dimension())).orElse(null);
        return ground == null || ground.dimension() == Aboard.LEVEL ? server.overworld() : ground;
    }

    private static BlockPos takenFrom(ServerPlayer player, ServerLevel ground) {
        return TakenData.of(player).from().filter(from -> from.dimension() == ground.dimension()).map(GlobalPos::pos).orElse(ground.getRespawnData().pos());
    }

    /**
     * Somewhere to come to: {@link Abductions#MISSING_MIN} to {@link Abductions#MISSING_MAX} blocks from {@code from}, on
     * dry ground if eight tries find any (and at {@code from} itself if they find no ground at all).
     */
    public static BlockPos setDownSpot(ServerLevel level, BlockPos from, RandomSource random) {
        BlockPos wet = null;
        for (int i = 0; i < 8; i++) {
            double angle = random.nextDouble() * Math.PI * 2, distance = setDownMin + random.nextInt(Math.max(1, setDownMax - setDownMin + 1));
            int x = (int) (from.getX() + Math.cos(angle) * distance), z = (int) (from.getZ() + Math.sin(angle) * distance);
            level.getChunk(x >> 4, z >> 4); // generated, so the heightmap's true
            int y = level.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, x, z);
            if (y <= level.getMinY() + 1) {
                continue;
            }
            BlockPos at = new BlockPos(x, y, z);
            if (level.getFluidState(at.below()).isEmpty()) {
                return at;
            }
            wet = at;
        }
        return wet != null ? wet : from;
    }

    private static void setDown(ServerPlayer player, Way way) {
        ServerLevel ground = groundFor(player);
        BlockPos spot = setDownSpot(ground, takenFrom(player, ground), ground.getRandom());
        move(player, ground, Vec3.atBottomCenterOf(spot), player.getYRot());
        TakenData.landed(player);
        player.resetFallDistance();
        player.clearFire();
        player.addEffect(new MobEffectInstance(MobEffects.BLINDNESS, 100, 0));
        player.addEffect(new MobEffectInstance(MobEffects.NAUSEA, 200, 0));
        player.addEffect(new MobEffectInstance(MobEffects.SLOWNESS, 300, 1));
        note(player, switch (way) {
            case FELL -> TakenText.LANDED_FELL;
            case DIED -> TakenText.LANDED_DIED;
            case CRASHED -> TakenText.LANDED_CRASHED;
            default -> TakenText.LANDED_LEFT;
        });
        ModCriteria.fire(player, ModCriteria.ABDUCTED);
    }

    /**
     * A pod comes down: its riders and the freed who made it are set in its wreck near where the first rider was taken.
     * If the ship was falling when they left it, its own wreck is raised a walk away.
     */
    static void podLanding(ServerLevel aboard, Ship ship, List<ServerPlayer> riders, List<Mob> captives, boolean everyone) {
        ServerPlayer first = riders.get(0);
        ServerLevel ground = groundFor(first);
        RandomSource random = ground.getRandom();
        BlockPos spot = setDownSpot(ground, takenFrom(first, ground), random);
        BlockPos origin = spot.offset(-3, -1, -3);
        StructureTemplate wreck = ground.getStructureTemplateManager().get(HollowFrontier.id(ShipBlueprints.POD_WRECK)).orElse(null);
        if (wreck != null) {
            wreck.placeInWorld(ground, origin, origin, new StructurePlaceSettings(), random, Block.UPDATE_CLIENTS);
        }
        Vec3 seat = Vec3.atBottomCenterOf(origin.offset(ShipBlueprints.WRECK_SEAT));
        boolean down = ship.falling(), unseen = !ship.everSeen();
        Optional<BlockPos> crash = down ? raiseCrashSite(ground, spot, random) : Optional.empty();
        for (ServerPlayer rider : riders) {
            finish(rider);
            move(rider, ground, seat, rider.getYRot());
            TakenData.landed(rider);
            rider.resetFallDistance();
            rider.clearFire();
            rider.addEffect(new MobEffectInstance(MobEffects.BLINDNESS, 60, 0, false, false));
            rider.addEffect(new MobEffectInstance(MobEffects.NAUSEA, 140, 0, false, false));
            note(rider, TakenText.LANDED_POD);
            if (!everyone && !ship.held().isEmpty()) {
                note(rider, TakenText.NOTE_LEFT_BEHIND);
            }
            crash.ifPresent(site -> note(rider, TakenText.NOTE_CRASH_SITE, StakeText.paces(spot, site), StakeText.direction(spot, site)));
            Suspicion.add(rider, down ? SUSPICION_DOWN : SUSPICION_OUT);
            ModCriteria.fire(rider, ModCriteria.TAKEN_OUT);
            if (down) {
                ModCriteria.fire(rider, ModCriteria.TAKEN_BROUGHT_DOWN);
            }
            if (everyone) {
                ModCriteria.fire(rider, ModCriteria.TAKEN_NOBODY_LEFT);
            }
            if (unseen) {
                ModCriteria.fire(rider, ModCriteria.TAKEN_UNSEEN);
            }
            com.hollowfrontier.content.journal.JournalSeen.takenAndBack(rider, down);
        }
        for (Mob captive : captives) {
            double angle = random.nextDouble() * Math.PI * 2;
            int x = spot.getX() + (int) Math.round(Math.cos(angle) * 4), z = spot.getZ() + (int) Math.round(Math.sin(angle) * 4);
            Vec3 to = new Vec3(x + 0.5, ground.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, x, z), z + 0.5);
            captive.teleport(new TeleportTransition(ground, to, Vec3.ZERO, captive.getYRot(), 0.0F, TeleportTransition.DO_NOTHING));
        }
        ground.playSound(null, spot, ModSounds.POD_CRASH.get(), SoundSource.BLOCKS, 4.0F, 1.0F);
        ground.sendParticles(ParticleTypes.EXPLOSION_EMITTER, spot.getX() + 0.5, spot.getY() + 1.0, spot.getZ() + 0.5, 1, 0, 0, 0, 0);
        ground.sendParticles(ParticleTypes.CAMPFIRE_SIGNAL_SMOKE, spot.getX() + 0.5, spot.getY() + 1.5, spot.getZ() + 0.5, 30, 1.5, 1.0, 1.5, 0.02);
        // Something came down in the night: the men in dusters will want a word, and the Gazette a paragraph
        MysteryLog.get(ground).record(MysteryLog.Kind.SIGHTING, ground.getGameTime(), spot);
    }

    /** The ship's time is up with these still aboard: they come to in what's left of it. */
    static void wentDown(ServerLevel aboard, Ship ship, List<ServerPlayer> still) {
        for (ServerPlayer player : still) {
            ServerLevel ground = groundFor(player);
            BlockPos from = takenFrom(player, ground);
            Optional<BlockPos> crash = raiseCrashSite(ground, setDownSpot(ground, from, ground.getRandom()), ground.getRandom());
            finish(player);
            if (crash.isPresent()) {
                BlockPos at = crash.get().offset(8, 0, -18);
                at = at.atY(ground.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, at.getX(), at.getZ()));
                move(player, ground, Vec3.atBottomCenterOf(at), player.getYRot());
                TakenData.landed(player);
                player.resetFallDistance();
                player.clearFire();
                player.addEffect(new MobEffectInstance(MobEffects.BLINDNESS, 100, 0));
                player.addEffect(new MobEffectInstance(MobEffects.NAUSEA, 200, 0));
                note(player, TakenText.LANDED_CRASHED);
                ModCriteria.fire(player, ModCriteria.ABDUCTED);
                MysteryLog.get(ground).record(MysteryLog.Kind.SIGHTING, ground.getGameTime(), at);
            } else {
                setDown(player, Way.CRASHED);
            }
            player.setHealth(Math.min(player.getHealth(), 6.0F));
            Suspicion.add(player, SUSPICION_DOWN);
        }
    }

    /**
     * A wrecked ship, as the crash sites of the world are: raised on level, dry ground {@link #CRASH_MIN} to
     * {@link #CRASH_MAX} blocks from {@code near}, its Wardens with it. Where it stands, if anywhere would take it.
     */
    public static Optional<BlockPos> raiseCrashSite(ServerLevel level, BlockPos near, RandomSource random) {
        StructureTemplate site = level.getStructureTemplateManager().get(HollowFrontier.id(UfoBlueprints.CRASH_SITE)).orElse(null);
        if (site == null) {
            return Optional.empty();
        }
        int sizeX = site.getSize().getX(), sizeZ = site.getSize().getZ();
        // (Nearer in the game tests, which set everything down close by)
        int nearest = Math.min(CRASH_MIN, setDownMax), farthest = Math.max(nearest, Math.min(CRASH_MAX, setDownMax));
        for (int attempt = 0; attempt < 12; attempt++) {
            double angle = random.nextDouble() * Math.PI * 2, distance = nearest + random.nextInt(farthest - nearest + 1);
            int x = (int) (near.getX() + Math.cos(angle) * distance), z = (int) (near.getZ() + Math.sin(angle) * distance);
            int low = Integer.MAX_VALUE, high = Integer.MIN_VALUE;
            boolean wet = false;
            for (int dx = 0; dx <= sizeX; dx += sizeX / 2) {
                for (int dz = 0; dz <= sizeZ; dz += sizeZ / 3) {
                    level.getChunk((x + dx) >> 4, (z + dz) >> 4);
                    int y = level.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, x + dx, z + dz);
                    low = Math.min(low, y);
                    high = Math.max(high, y);
                    wet |= !level.getFluidState(new BlockPos(x + dx, y - 1, z + dz)).isEmpty();
                }
            }
            if (wet || high - low > 5 || low <= level.getMinY() + 1) {
                continue;
            }
            // Its floor row is the ground's top block, as it is where worldgen sets one
            BlockPos origin = new BlockPos(x, (low + high) / 2 - 1, z);
            site.placeInWorld(level, origin, origin, new StructurePlaceSettings().setFinalizeEntities(true), random, Block.UPDATE_CLIENTS);
            BlockPos middle = origin.offset(UfoBlueprints.SAUCER_X, 1, UfoBlueprints.SAUCER_Z);
            level.playSound(null, middle, SoundEvents.GENERIC_EXPLODE.value(), SoundSource.BLOCKS, 8.0F, 0.5F);
            return Optional.of(middle);
        }
        return Optional.empty();
    }

    // ---------------------------------------------------------------- ticking

    @SubscribeEvent
    static void onLevelTick(LevelTickEvent.Post event) {
        if (!(event.getLevel() instanceof ServerLevel level) || SHIPS.isEmpty()) {
            return;
        }
        for (Ship ship : List.copyOf(SHIPS.values())) {
            if (ship.dimension() != level.dimension()) {
                continue;
            }
            ship.tick(level);
            if (ship.players().isEmpty() && !ship.kept()) {
                ship.clear(level);
                SHIPS.remove(ship.index());
            }
        }
    }

    /**
     * Nobody stays in the ship's world without a ship: a survival player found there on no ship's books (come by a
     * command or another mod's doing, or left there by a server that stopped mid-flight) is set down on the ground.
     */
    @SubscribeEvent
    static void onStrayTick(LevelTickEvent.Post event) {
        if (event.getLevel() instanceof ServerLevel level && level.dimension() == Aboard.LEVEL && level.getGameTime() % 20 == 0) {
            for (ServerPlayer player : List.copyOf(level.players())) {
                if (!player.isCreative() && !player.isSpectator() && player.isAlive() && shipOf(player) == null && !MOVING.contains(player.getUUID())) {
                    leave(player, Way.LEFT);
                }
            }
        }
    }

    @SubscribeEvent
    static void onServerStopped(ServerStoppedEvent event) {
        SHIPS.clear();
        MOVING.clear();
    }

    // ---------------------------------------------------------------- leaving any other way

    /** Death aboard isn't one: the Grays set their subject down, as they do the ones they've finished with. */
    @SubscribeEvent(priority = EventPriority.HIGHEST)
    static void onDeath(LivingDeathEvent event) {
        if (event.getEntity() instanceof ServerPlayer player && shipOf(player) != null) {
            event.setCanceled(true);
            player.setHealth(Math.min(player.getMaxHealth(), 8.0F));
            player.removeAllEffects();
            leave(player, Way.DIED);
        }
    }

    /** Leaving the game aboard: the kit goes back now, and the player is set down when they come back. */
    @SubscribeEvent
    static void onLoggedOut(PlayerEvent.PlayerLoggedOutEvent event) {
        if (event.getEntity() instanceof ServerPlayer player && shipOf(player) != null) {
            finish(player);
        }
    }

    @SubscribeEvent
    static void onLoggedIn(PlayerEvent.PlayerLoggedInEvent event) {
        if (event.getEntity() instanceof ServerPlayer player && TakenData.of(player).aboard()) {
            leave(player, Way.LEFT);
        }
    }

    /** Moved out of the ship's world by something that isn't us (a command, another mod): the books are closed here. */
    @SubscribeEvent
    static void onChangedDimension(PlayerEvent.PlayerChangedDimensionEvent event) {
        if (event.getEntity() instanceof ServerPlayer player && !MOVING.contains(player.getUUID()) && event.getFrom() == where
                && TakenData.of(player).aboard()) {
            finish(player);
            TakenData.landed(player);
        }
    }

    // ---------------------------------------------------------------- the hull holds

    /** Added to the synced ship state once the player has had the vents' one line (the client reads the state below it). */
    public static final int VENT_NOTED = 8;

    private static boolean bound(Player player) {
        return !player.isCreative() && shipOf(player) != null;
    }

    @SubscribeEvent
    static void onBreakSpeed(PlayerEvent.BreakSpeed event) {
        if (!bound(event.getEntity())) {
            return;
        }
        BlockState state = event.getState();
        if (!state.is(ModTags.Blocks.TAKEN_BREAKABLE)) {
            event.setCanceled(true);
        } else {
            // By hand: a grille in a second and a half, the rig in six
            event.setNewSpeed(Math.max(event.getOriginalSpeed(), state.is(ModBlocks.IMPLANT_RIG.get()) ? 3.4F : 7.0F));
        }
    }

    @SubscribeEvent
    static void onBreak(net.neoforged.neoforge.event.level.block.BreakBlockEvent event) {
        if (!bound(event.getPlayer())) {
            return;
        }
        if (!event.getState().is(ModTags.Blocks.TAKEN_BREAKABLE)) {
            event.setCanceled(true);
            event.getPlayer().sendOverlayMessage(Component.translatable(TakenText.CANT_BREAK).withStyle(ChatFormatting.GRAY));
        } else if (event.getPlayer() instanceof ServerPlayer player && !event.getState().is(ModBlocks.IMPLANT_RIG.get())
                && player.getData(ModAttachments.ABOARD.get()) < VENT_NOTED) {
            // The first grille off: what the vents are good for, said once
            note(player, TakenText.NOTE_VENT);
            player.setData(ModAttachments.ABOARD.get(), player.getData(ModAttachments.ABOARD.get()) + VENT_NOTED);
        }
    }

    @SubscribeEvent
    static void onPlace(BlockEvent.EntityPlaceEvent event) {
        if (event.getEntity() instanceof Player player && bound(player)) {
            event.setCanceled(true);
        }
    }

    @SubscribeEvent
    static void onDetonate(ExplosionEvent.Detonate event) {
        if (!(event.getLevel() instanceof ServerLevel level)) {
            return;
        }
        Vec3 centre = event.getExplosion().center();
        Ship ship = shipAt(level, BlockPos.containing(centre));
        if (ship == null) {
            return;
        }
        event.getAffectedBlocks().clear();
        if (centre.distanceTo(Vec3.atCenterOf(ship.world(ShipLayout.CORE))) <= ShipLayout.ENGINE_R + 1) {
            ship.startFall(level);
        }
    }

    // ---------------------------------------------------------------- what the crew make out

    @SubscribeEvent
    static void onChangeTarget(LivingChangeTargetEvent event) {
        LivingEntity entity = event.getEntity();
        if (SHIPS.isEmpty() || !(event.getNewAboutToBeSetTarget() instanceof Player player) || entity.level().isClientSide()) {
            return;
        }
        Ship ship = shipAt(entity.level(), entity.blockPosition());
        if (ship == null) {
            return;
        }
        if (entity instanceof GrayWarden warden ? !ship.sees(warden, player) : Specimens.isTagged(entity)) {
            event.setCanceled(true);
        }
    }

    // ---------------------------------------------------------------- what's used aboard

    @SubscribeEvent(priority = EventPriority.HIGH)
    static void onUseBlock(PlayerInteractEvent.RightClickBlock event) {
        if (!(event.getLevel() instanceof ServerLevel level) || !(event.getEntity() instanceof ServerPlayer player) || SHIPS.isEmpty()) {
            return;
        }
        BlockPos pos = event.getPos();
        Ship ship = shipAt(level, pos);
        if (ship == null) {
            return;
        }
        BlockState state = level.getBlockState(pos);
        BlockPos local = ship.local(pos);
        boolean handled = false;
        if (state.is(ModBlocks.GRAY_CARGO_POD.get()) && local.equals(ShipLayout.KIT_POD)) {
            // What was on them: handed back, to each of the taken who asks
            if (TakenData.of(player).hasKit()) {
                TakenData.restore(player);
                note(player, TakenText.NOTE_KIT);
                unseal(level, pos, state);
            } else {
                player.sendOverlayMessage(Component.translatable(TakenText.NOTE_KIT_NONE).withStyle(ChatFormatting.GRAY));
            }
            handled = true;
        } else if (state.is(ModBlocks.GRAY_CARGO_POD.get()) && local.equals(ShipLayout.TRAY_POD)) {
            if (!state.getValue(CargoPodBlock.OPEN)) {
                BoardUse.give(player, new ItemStack(ModItems.SHOCK_ROD.get()));
                BoardUse.give(player, new ItemStack(ModItems.PHLOGISTON_CELL.get()));
                note(player, TakenText.NOTE_TRAY);
                unseal(level, pos, state);
            } else {
                player.sendOverlayMessage(Component.translatable("message.hollowfrontier.cargo_pod.empty").withStyle(ChatFormatting.GRAY));
            }
            handled = true;
        } else if (state.is(ModBlocks.IMPLANT_RIG.get()) && event.getItemStack().is(ModItems.SHOCK_ROD.get())) {
            // The rod, put where it will do the most harm: the rig shorts out, and whatever it was doing stops
            level.sendParticles(ParticleTypes.ELECTRIC_SPARK, pos.getX() + 0.5, pos.getY(), pos.getZ() + 0.5, 30, 0.4, 0.4, 0.4, 0.2);
            level.destroyBlock(pos, false, player);
            note(player, TakenText.NOTE_RIG_SHORTED);
            handled = true;
        }
        if (handled) {
            event.setCancellationResult(InteractionResult.SUCCESS);
            event.setCanceled(true);
        }
    }

    private static void unseal(ServerLevel level, BlockPos pos, BlockState state) {
        if (!state.getValue(CargoPodBlock.OPEN)) {
            level.setBlock(pos, state.setValue(CargoPodBlock.OPEN, true), Block.UPDATE_ALL);
            level.sendParticles(ParticleTypes.CLOUD, pos.getX() + 0.5, pos.getY() + 1.0, pos.getZ() + 0.5, 8, 0.25, 0.1, 0.25, 0.02);
            level.playSound(null, pos, SoundEvents.FIRE_EXTINGUISH, SoundSource.BLOCKS, 0.8F, 1.4F);
            level.playSound(null, pos, SoundEvents.IRON_TRAPDOOR_OPEN, SoundSource.BLOCKS, 1.0F, 0.7F);
        }
    }

    // ---------------------------------------------------------------- the commands

    /** {@code /hollowfrontier ufo taken}: the runner is kept, now, whatever the rules say. */
    public static int commandTake(ServerPlayer player) {
        if (shipOf(player) != null) {
            return 0;
        }
        Ship ship = take((ServerLevel) player.level(), player);
        if (ship == null) {
            player.sendSystemMessage(Component.translatable(TakenText.COMMAND_NO_SHIP));
            return 0;
        }
        return 1;
    }

    /** {@code /hollowfrontier ufo taken leave}: the runner is set down, as if they'd left the game and come back. */
    public static int commandLeave(ServerPlayer player) {
        if (shipOf(player) == null) {
            player.sendSystemMessage(Component.translatable(TakenText.COMMAND_NOT_ABOARD));
            return 0;
        }
        leave(player, Way.LEFT);
        return 1;
    }

    /** For tests: every ship gone, with nobody set down anywhere. */
    public static void reset(MinecraftServer server) {
        for (Ship ship : List.copyOf(SHIPS.values())) {
            ServerLevel level = server.getLevel(ship.dimension());
            if (level != null) {
                for (ServerPlayer player : new ArrayList<>(ship.aboard(level))) {
                    ship.remove(player);
                    player.setData(ModAttachments.ABOARD.get(), 0);
                }
                ship.clear(level);
            }
        }
        SHIPS.clear();
    }
}
