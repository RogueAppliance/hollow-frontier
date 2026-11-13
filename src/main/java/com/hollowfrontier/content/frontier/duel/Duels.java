package com.hollowfrontier.content.frontier.duel;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import org.jspecify.annotations.Nullable;

import com.hollowfrontier.HollowFrontier;
import com.hollowfrontier.content.frontier.bounty.BoardPosting;
import com.hollowfrontier.content.frontier.bounty.BoardUse;
import com.hollowfrontier.content.frontier.bounty.BountyBoards;
import com.hollowfrontier.content.frontier.bounty.BountyData;
import com.hollowfrontier.content.frontier.bounty.Purse;
import com.hollowfrontier.content.frontier.gun.GunItem;
import com.hollowfrontier.content.frontier.gun.GunMechanics;
import com.hollowfrontier.content.frontier.gun.GunTypes;
import com.hollowfrontier.content.frontier.gun.GunWielder;
import com.hollowfrontier.content.frontier.outlaw.OutlawBoss;
import com.hollowfrontier.content.frontier.stake.Standing;
import com.hollowfrontier.content.frontier.town.Towns;
import com.hollowfrontier.content.frontier.townsfolk.Lawman;
import com.hollowfrontier.content.frontier.townsfolk.Townsfolk;
import com.hollowfrontier.network.DuelHudPayload;
import com.hollowfrontier.registry.ModCriteria;
import com.hollowfrontier.registry.ModDamageTypes;
import com.hollowfrontier.registry.ModDataComponents;
import com.hollowfrontier.registry.ModEntities;
import com.hollowfrontier.registry.ModItems;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.level.levelgen.structure.PoolElementStructurePiece;
import net.minecraft.world.level.levelgen.structure.StructurePiece;
import net.minecraft.world.level.levelgen.structure.StructureStart;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.server.ServerStoppingEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;
import net.neoforged.neoforge.network.PacketDistributor;

/**
 * High noon (51). One Wanted Poster in three ({@link #callsOut}) names a boss who would sooner settle it in the street:
 * near noon, with its holder within reach of the town whose board it hangs on, he asks ({@link #lookForCall}), over the
 * hotbar, with a key for yes and one for no. Yes puts the player on a mark in the street with him twelve paces off.
 * <p>
 * A {@link Duel} runs by its own count of ticks: the walk to the marks, three tolls of the bell, a wait nobody can time,
 * the draw. Until the draw the player's trigger is locked ({@link #holstered}); a pull before it is a flinch and locks it
 * a little longer. After it the gun is the player's own and fires as it always does, without spending anything
 * ({@link #freeFire}), and the first hit decides it: theirs on him ({@link #hit}), or his on them, which comes after a
 * reaction time set by his bounty ({@link #reactionTicks}) and lands five times in six.
 * <p>
 * Nothing of a duel is saved. A duel in hand when the player leaves or the server stops is called off: the boss goes,
 * a lent gun is taken back, and the poster is as it was.
 */
@EventBusSubscriber(modid = HollowFrontier.MODID)
public final class Duels {
    private Duels() {}

    /** The boss asks between these hours of the day (noon is 6000), and waits this long for an answer. */
    public static final int ASK_FROM = 5400, ASK_UNTIL = 6100, ASK_TICKS = 600;
    /** Ticks from standing on the mark to the first toll, between tolls, and the least and most from the third to the draw. */
    public static final int WALK = 40, TOLL_GAP = 30, WAIT_MIN = 30, WAIT_MAX = 80;
    /** A pull before the draw: the gun stays locked this much longer for each, up to so many; a held trigger is one pull. */
    public static final int FLINCH_TICKS = 10, MAX_FLINCHES = 4, FLINCH_GAP = 6;
    /** His reaction: this slow at a bounty of {@link #CHEAP} or less, this fast at {@link #DEAR} or more. */
    public static final int SLOWEST = 18, FASTEST = 9, CHEAP = 3, DEAR = 7;
    /** Ticks between his shots, and how many of them in this many miss. */
    public static final int BOSS_COOLDOWN = 24, HIT_IN = 6;
    /** How long both stand there once it's decided, and how long a duel nobody is winning goes on. */
    public static final int AFTER = 60, TIMEOUT = 600;
    public static final int PACES = 12;
    public static final int WON_STANDING = 4, LOST_STANDING = 3;
    /** The loser is left with this much health, and is this many parts in a hundred of their coin lighter. */
    public static final float LOSER_HEALTH = 2.0F;
    public static final int PURSE_PERCENT = 10;
    private static final int DAZED = 200;
    private static final Identifier ROOTED = HollowFrontier.id("duel_rooted");

    /** Client side only: whether this client's own trigger is locked (DuelClient keeps it). */
    public static volatile boolean clientHolstered;

    private static final Map<UUID, Duel> ACTIVE = new HashMap<>();
    private static final Map<UUID, Pending> PENDING = new HashMap<>();

    /** Where the two stand (feet, block centres). */
    public record Ground(Vec3 player, Vec3 boss) {
        public Vec3 centre() {
            return player.add(boss).scale(0.5);
        }
    }

    private record Pending(BountyData bounty, long town, Ground ground, long expires) {}

    public enum Outcome { NONE, WON, LOST }

    // ---------------------------------------------------------------- the rule

    /** Whether this poster's boss is one who calls his hunter out: fixed by the bounty itself, so the poster can say so. */
    public static boolean callsOut(BountyData bounty) {
        return DuelConfig.duels() && callsOut(bounty.id(), DuelConfig.callOutChance());
    }

    public static boolean callsOut(UUID id, double chance) {
        long mixed = (id.getMostSignificantBits() ^ id.getLeastSignificantBits()) * 0x9E3779B97F4A7C15L;
        return (mixed >>> 40) / (double) (1L << 24) < chance;
    }

    /** How long after the draw he fires, in ticks. */
    public static int reactionTicks(int reward) {
        float along = (reward - CHEAP) / (float) (DEAR - CHEAP);
        return Mth.clamp(Math.round(SLOWEST - along * (SLOWEST - FASTEST)), FASTEST, SLOWEST);
    }

    /** What's on his head once he has put a hunter down, and what his badge pays when he's beaten in the street. */
    public static int raised(int reward) {
        return (reward * 3 + 1) / 2;
    }

    /** Whether this boss would ask now: he's one who does, he hasn't been found at his camp, it's near noon, and he hasn't asked today or twice already. */
    public static boolean mayAsk(DuelData data, BountyData bounty, long timeOfDay, long day) {
        return !bounty.spawned() && !bounty.captive() && callsOut(bounty)
                && timeOfDay >= ASK_FROM && timeOfDay < ASK_UNTIL
                && data.times(bounty.id()) < DuelData.MAX_ASKS && data.askedOn(bounty.id()) != day;
    }

    /** A poster just taken off a board: if its boss is one who calls out, the hunter is told. */
    public static void posterTaken(ServerPlayer player, BountyData bounty) {
        if (callsOut(bounty)) {
            player.sendSystemMessage(Component.translatable(DuelText.WORD_SENT, bounty.target()).withStyle(ChatFormatting.GOLD));
        }
    }

    // ---------------------------------------------------------------- the street

    /**
     * Two marks {@link #PACES} apart along one of the town's streets, the player's nearer the square: level with each
     * other, dry, and with nothing between them at eye height. Empty where the town isn't loaded or has no such run.
     */
    public static Optional<Ground> ground(ServerLevel level, long town) {
        Optional<StructureStart> found = Towns.start(level, town);
        if (found.isEmpty()) {
            return Optional.empty();
        }
        StructureStart start = found.get();
        BoundingBox square = start.getPieces().getFirst().getBoundingBox();
        BlockPos rally = new BlockPos(square.getCenter().getX(), square.minY() + 1, square.getCenter().getZ());
        for (StructurePiece piece : start.getPieces()) {
            if (!(piece instanceof PoolElementStructurePiece pool) || !pool.getElement().toString().contains("street_")) {
                continue;
            }
            BoundingBox street = piece.getBoundingBox();
            BlockPos mid = street.getCenter();
            Direction out = Direction.getApproximateNearest(mid.getX() - rally.getX(), 0, mid.getZ() - rally.getZ());
            int length = switch (out) {
                case EAST -> street.maxX() - rally.getX();
                case WEST -> rally.getX() - street.minX();
                case SOUTH -> street.maxZ() - rally.getZ();
                default -> rally.getZ() - street.minZ();
            };
            for (int from = 2; from + PACES <= length; from += 3) {
                Optional<Ground> lane = lane(level, rally.relative(out, from), rally.relative(out, from + PACES), rally.getY());
                if (lane.isPresent()) {
                    return lane;
                }
            }
        }
        // No street will do: across the square itself, one way or the other
        for (Direction across : new Direction[] {Direction.EAST, Direction.SOUTH}) {
            Optional<Ground> lane = lane(level, rally.relative(across, -PACES / 2), rally.relative(across, PACES / 2), rally.getY());
            if (lane.isPresent()) {
                return lane;
            }
        }
        return Optional.empty();
    }

    /** The marks over {@code a} and {@code b} on the ground as it stands, if they'll do for a duel (see {@link #ground}). */
    public static Optional<Ground> lane(ServerLevel level, BlockPos a, BlockPos b, int streetY) {
        if (!level.isLoaded(a) || !level.isLoaded(b)) {
            return Optional.empty();
        }
        BlockPos pa = level.getHeightmapPos(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, a);
        BlockPos pb = level.getHeightmapPos(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, b);
        if (Math.abs(pa.getY() - pb.getY()) > 2 || Math.abs(pa.getY() - streetY) > 3 || Math.abs(pb.getY() - streetY) > 3
                || !level.getFluidState(pa).isEmpty() || !level.getFluidState(pb).isEmpty()
                || !level.getFluidState(pa.below()).isEmpty() || !level.getFluidState(pb.below()).isEmpty()) {
            return Optional.empty();
        }
        Vec3 fa = Vec3.atBottomCenterOf(pa), fb = Vec3.atBottomCenterOf(pb);
        for (double height : new double[] {0.6, 1.62}) {
            HitResult between = level.clip(new ClipContext(fa.add(0, height, 0), fb.add(0, height, 0), ClipContext.Block.COLLIDER,
                    ClipContext.Fluid.ANY, CollisionContext.empty()));
            if (between.getType() != HitResult.Type.MISS) {
                return Optional.empty();
            }
        }
        return Optional.of(new Ground(fa, fb));
    }

    // ---------------------------------------------------------------- the question

    @SubscribeEvent
    static void onPlayerTick(PlayerTickEvent.Post event) {
        if (event.getEntity() instanceof ServerPlayer player && player.tickCount % 20 == 0) {
            if (!ACTIVE.containsKey(player.getUUID())) {
                takeLentBack(player, false);
                lookForCall(player);
            }
        }
    }

    /** Asks the player to the street if a poster they carry has a boss who would, and the hour and the place are right. */
    public static void lookForCall(ServerPlayer player) {
        ServerLevel level = player.level();
        if (!DuelConfig.duels() || level.dimension() != Level.OVERWORLD || level.getDifficulty() == net.minecraft.world.Difficulty.PEACEFUL || player.isSpectator() || !player.isAlive()
                || PENDING.containsKey(player.getUUID()) || ACTIVE.containsKey(player.getUUID())) {
            return;
        }
        long time = level.getDefaultClockTime();
        long timeOfDay = time % 24000L, day = time / 24000L;
        if (timeOfDay < ASK_FROM || timeOfDay >= ASK_UNTIL) {
            return;
        }
        DuelData data = DuelData.of(player);
        Inventory inventory = player.getInventory();
        int reach = DuelConfig.duelReach();
        for (int slot = 0; slot < inventory.getContainerSize(); slot++) {
            ItemStack stack = inventory.getItem(slot);
            BountyData bounty = stack.get(ModDataComponents.BOUNTY.get());
            if (!stack.is(ModItems.WANTED_POSTER.get()) || bounty == null || !mayAsk(data, bounty, timeOfDay, day)) {
                continue;
            }
            Optional<Long> town = BountyBoards.get(level).keyHolding(bounty.id()).flatMap(Standing::town);
            if (town.isEmpty() || !Towns.middle(town.get()).closerThan(player.blockPosition().atY(64), reach + 160)
                    || ACTIVE.values().stream().anyMatch(d -> town.get().equals(d.town))) {
                continue;
            }
            Optional<Ground> ground = ground(level, town.get());
            if (ground.isEmpty() || ground.get().player().distanceTo(player.position()) > reach) {
                continue;
            }
            ask(player, bounty, town.get(), ground.get(), day);
            return;
        }
    }

    /** Puts the question (public for tests, which have no town to stand near). */
    public static void ask(ServerPlayer player, BountyData bounty, long town, Ground ground, long day) {
        PENDING.put(player.getUUID(), new Pending(bounty, town, ground, player.level().getGameTime() + ASK_TICKS));
        DuelData.set(player, DuelData.of(player).asked(bounty.id(), day));
        hud(player, DuelHudPayload.ASK, bounty.target(), ASK_TICKS);
        player.level().playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.BELL_BLOCK, SoundSource.NEUTRAL, 0.5F, 0.7F);
    }

    public static boolean asked(Player player) {
        return PENDING.containsKey(player.getUUID());
    }

    /** The answer, from the key (or from nobody: time ran out, which is no). */
    public static @Nullable Duel answer(ServerPlayer player, boolean yes) {
        Pending pending = PENDING.remove(player.getUUID());
        if (pending == null) {
            return null;
        }
        hud(player, DuelHudPayload.CLEAR, "", 0);
        if (!yes) {
            boolean last = DuelData.of(player).times(pending.bounty().id()) >= DuelData.MAX_ASKS;
            player.sendSystemMessage(Component.translatable(last ? DuelText.DECLINED_LAST : DuelText.DECLINED, pending.bounty().target()));
            return null;
        }
        ServerLevel level = player.level();
        if (level.dimension() != Level.OVERWORLD || !player.isAlive() || findPoster(player, pending.bounty().id()) == null
                || ACTIVE.values().stream().anyMatch(d -> d.town != null && d.town == pending.town())) {
            player.sendSystemMessage(Component.translatable(DuelText.TOO_LATE, pending.bounty().target()));
            return null;
        }
        return begin(level, player, pending.bounty(), pending.town(), pending.ground());
    }

    /** Starts a duel on this ground ({@code town}: whose street it is, or null for none). */
    public static Duel begin(ServerLevel level, ServerPlayer player, BountyData bounty, @Nullable Long town, Ground ground) {
        Duel duel = new Duel(level, player, bounty, town, ground);
        ACTIVE.put(player.getUUID(), duel);
        duel.start();
        return duel;
    }

    public static @Nullable Duel of(Player player) {
        return ACTIVE.get(player.getUUID());
    }

    private static @Nullable Duel duelOf(Entity entity) {
        if (ACTIVE.isEmpty()) {
            return null;
        }
        for (Duel duel : ACTIVE.values()) {
            if (duel.player == entity || duel.boss == entity) {
                return duel;
            }
        }
        return null;
    }

    // ---------------------------------------------------------------- the gun's side of it

    /**
     * Whether the player's trigger is locked (asked by a gun about to fire, on either side). On the server a pull while
     * it's locked is counted: before the draw it's a flinch.
     */
    public static boolean holstered(Player player) {
        if (player.level().isClientSide()) {
            return clientHolstered;
        }
        Duel duel = ACTIVE.get(player.getUUID());
        if (duel == null || !duel.locked()) {
            return false;
        }
        duel.pull();
        return true;
    }

    /** Whether the player's shots cost nothing just now: the draw has come and nothing is decided. */
    public static boolean freeFire(LivingEntity shooter) {
        Duel duel = shooter instanceof Player ? ACTIVE.get(shooter.getUUID()) : null;
        return duel != null && duel.live();
    }

    /** A gun went off (GunMechanics, PhlogistonGunItem). */
    public static void shot(LivingEntity shooter) {
        Duel duel = ACTIVE.isEmpty() || !(shooter instanceof Player) ? null : ACTIVE.get(shooter.getUUID());
        if (duel != null && duel.live()) {
            duel.shots++;
        }
    }

    /** That shot struck {@code target}: if it's a duellist's on his man, that's the duel. */
    public static void hit(ServerLevel level, LivingEntity shooter, Entity target, boolean head) {
        Duel duel = ACTIVE.isEmpty() || !(shooter instanceof Player) ? null : ACTIVE.get(shooter.getUUID());
        if (duel != null && duel.live() && target == duel.boss) {
            duel.win(head && duel.shots == 1);
        }
    }

    /** Nobody outside a duel can hurt either of the two, and neither of them can hurt anybody else, until it's over. */
    @SubscribeEvent
    static void onDamage(LivingIncomingDamageEvent event) {
        if (ACTIVE.isEmpty()) {
            return;
        }
        Duel mine = duelOf(event.getEntity());
        if (mine != null) {
            if (!mine.resolving) {
                event.setCanceled(true);
            }
            return;
        }
        Entity attacker = event.getSource().getEntity();
        if (attacker != null && duelOf(attacker) != null) {
            event.setCanceled(true);
        }
    }

    // ---------------------------------------------------------------- comings and goings

    @SubscribeEvent
    static void onServerTick(ServerTickEvent.Post event) {
        if (!PENDING.isEmpty()) {
            for (Map.Entry<UUID, Pending> entry : new ArrayList<>(PENDING.entrySet())) {
                ServerPlayer player = event.getServer().getPlayerList().getPlayer(entry.getKey());
                if (player == null) {
                    PENDING.remove(entry.getKey());
                } else if (player.level().getGameTime() >= entry.getValue().expires()) {
                    answer(player, false);
                }
            }
        }
        if (!ACTIVE.isEmpty()) {
            for (Duel duel : new ArrayList<>(ACTIVE.values())) {
                duel.tick();
            }
        }
    }

    @SubscribeEvent
    static void onLoggedOut(PlayerEvent.PlayerLoggedOutEvent event) {
        PENDING.remove(event.getEntity().getUUID());
        Duel duel = ACTIVE.get(event.getEntity().getUUID());
        if (duel != null) {
            duel.leave();
        }
    }

    @SubscribeEvent
    static void onChangedDimension(PlayerEvent.PlayerChangedDimensionEvent event) {
        Duel duel = ACTIVE.get(event.getEntity().getUUID());
        if (duel != null) {
            duel.leave();
        }
    }

    @SubscribeEvent
    static void onServerStopping(ServerStoppingEvent event) {
        for (Duel duel : new ArrayList<>(ACTIVE.values())) {
            duel.leave();
        }
        ACTIVE.clear();
        PENDING.clear();
    }

    private static void hud(ServerPlayer player, int kind, String name, int ticks) {
        PacketDistributor.sendToPlayer(player, new DuelHudPayload(kind, name, ticks));
    }

    private static @Nullable ItemStack findPoster(Player player, UUID bounty) {
        Inventory inventory = player.getInventory();
        for (int slot = 0; slot < inventory.getContainerSize(); slot++) {
            ItemStack stack = inventory.getItem(slot);
            BountyData data = stack.get(ModDataComponents.BOUNTY.get());
            if (stack.is(ModItems.WANTED_POSTER.get()) && data != null && data.id().equals(bounty)) {
                return stack;
            }
        }
        return null;
    }

    /** Whether a stack is the revolver a town lends for a duel. */
    public static boolean lent(ItemStack stack) {
        return stack.getOrDefault(ModDataComponents.LENT.get(), false);
    }

    /** Takes any lent revolver off the player (it's the town's). Whether there was one. */
    public static boolean takeLentBack(Player player, boolean say) {
        Inventory inventory = player.getInventory();
        boolean found = false;
        for (int slot = 0; slot < inventory.getContainerSize(); slot++) {
            if (lent(inventory.getItem(slot))) {
                inventory.setItem(slot, ItemStack.EMPTY);
                found = true;
            }
        }
        if (found && say) {
            player.sendSystemMessage(Component.translatable(DuelText.LENT_BACK));
        }
        return found;
    }

    private static boolean handgun(ItemStack stack) {
        return stack.is(ModItems.REVOLVER.get()) || stack.is(ModItems.RAY_PISTOL.get());
    }

    // ---------------------------------------------------------------- one duel

    public static final class Duel {
        private final ServerLevel level;
        private final ServerPlayer player;
        private final BountyData bounty;
        private final @Nullable Long town;
        private final Ground ground;
        private final int drawAt;
        private final int reaction;

        private @Nullable OutlawBoss boss;
        private int t;
        private int flinches;
        private int lastPull = Integer.MIN_VALUE / 2;
        private int nextShot;
        private int shots;
        private int endAt;
        private Outcome outcome = Outcome.NONE;
        private boolean resolving;
        private boolean over;
        private boolean cleanKill;
        /** For tests: whether his next shots land, and whether losing kills, whatever the dice and the config say. */
        public @Nullable Boolean landsNext, lethal;

        // The player's own gun, what was loaded in it before, and the slot it was fetched from (-1: it was in hand)
        private @Nullable ItemStack ownGun;
        private int savedAmmo = -1;
        private int fetchedFrom = -1;
        private int handSlot;

        private Duel(ServerLevel level, ServerPlayer player, BountyData bounty, @Nullable Long town, Ground ground) {
            this.level = level;
            this.player = player;
            this.bounty = bounty;
            this.town = town;
            this.ground = ground;
            this.drawAt = WALK + 2 * TOLL_GAP + WAIT_MIN + level.getRandom().nextInt(WAIT_MAX - WAIT_MIN + 1);
            this.reaction = reactionTicks(bounty.reward());
            this.nextShot = drawAt + reaction;
        }

        public int ticks() { return t; }
        public int drawAt() { return drawAt; }
        public int reaction() { return reaction; }
        public int flinches() { return flinches; }
        public int unlockAt() { return drawAt + flinches * FLINCH_TICKS; }
        public Outcome outcome() { return outcome; }
        public @Nullable OutlawBoss boss() { return boss; }
        public boolean over() { return over; }
        public int endAt() { return endAt; }
        /** Won with the first shot, to the head. */
        public boolean cleanKill() { return cleanKill; }

        /** The trigger is locked: the draw hasn't come, or it has and a flinch is being paid for. */
        public boolean locked() {
            return outcome == Outcome.NONE && t < unlockAt();
        }

        /** The draw has come, the gun is free, and nothing is decided. */
        public boolean live() {
            return outcome == Outcome.NONE && t >= unlockAt();
        }

        private void start() {
            player.stopRiding();
            Vec3 at = ground.player(), to = ground.boss();
            float yaw = (float) (Mth.atan2(to.z - at.z, to.x - at.x) * Mth.RAD_TO_DEG) - 90.0F;
            player.teleportTo(level, at.x, at.y, at.z, Set.of(), yaw, 20.0F, false);
            markPoster(true);
            arm();
            root(true);
            hud(player, DuelHudPayload.MARK, bounty.target(), 0);
        }

        // Their best handgun to hand and loaded for nothing, or the town's own revolver if they haven't one
        private void arm() {
            Inventory inventory = player.getInventory();
            handSlot = inventory.getSelectedSlot();
            int best = handgun(inventory.getItem(handSlot)) ? handSlot : -1;
            for (int slot = 0; slot < inventory.getContainerSize() && best != handSlot; slot++) {
                ItemStack stack = inventory.getItem(slot);
                if (handgun(stack) && (best < 0 || stack.is(ModItems.RAY_PISTOL.get()) && !inventory.getItem(best).is(ModItems.RAY_PISTOL.get()))) {
                    best = slot;
                }
            }
            if (best < 0) {
                ItemStack revolver = new ItemStack(ModItems.REVOLVER.get());
                GunItem.setAmmo(revolver, GunTypes.REVOLVER.magazine());
                revolver.set(ModDataComponents.LENT.get(), true);
                ItemStack inHand = inventory.getItem(handSlot);
                inventory.setItem(handSlot, revolver);
                if (!inHand.isEmpty()) {
                    BoardUse.give(player, inHand);
                }
                player.sendSystemMessage(Component.translatable(DuelText.LENT));
                return;
            }
            if (best != handSlot) {
                ItemStack gun = inventory.getItem(best);
                inventory.setItem(best, inventory.getItem(handSlot));
                inventory.setItem(handSlot, gun);
                fetchedFrom = best;
            }
            ownGun = inventory.getItem(handSlot);
            if (ownGun.getItem() instanceof GunItem gun && gun.stats().magazine() > 0) {
                savedAmmo = GunItem.getAmmo(ownGun);
                gun.stopReload(ownGun);
                GunItem.setAmmo(ownGun, gun.stats().magazine());
            }
        }

        // What was loaded is what's loaded again, and the gun goes back where it was fetched from
        private void disarm() {
            takeLentBack(player, true);
            if (ownGun == null) {
                return;
            }
            if (savedAmmo >= 0) {
                GunItem.setAmmo(ownGun, savedAmmo);
            }
            Inventory inventory = player.getInventory();
            if (fetchedFrom >= 0 && inventory.getItem(handSlot) == ownGun) {
                inventory.setItem(handSlot, inventory.getItem(fetchedFrom));
                inventory.setItem(fetchedFrom, ownGun);
            }
            ownGun = null;
        }

        private void root(boolean rooted) {
            for (var attribute : List.of(Attributes.MOVEMENT_SPEED, Attributes.JUMP_STRENGTH)) {
                AttributeInstance instance = player.getAttribute(attribute);
                if (instance == null) {
                    continue;
                }
                instance.removeModifier(ROOTED);
                if (rooted) {
                    instance.addTransientModifier(new AttributeModifier(ROOTED, -1.0, AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL));
                }
            }
        }

        // The poster reads "spotted" while he's in the street, so his camp stays empty; called off, it's as it was
        private void markPoster(boolean spotted) {
            ItemStack poster = findPoster(player, bounty.id());
            if (poster != null) {
                poster.set(ModDataComponents.BOUNTY.get(), spotted ? bounty.markSpawned() : bounty);
            }
        }

        /** One tick (the server's; public so a test can run a duel on to the moment it wants). */
        public void tick() {
            if (over) {
                return;
            }
            t++;
            if (!player.isAlive() || player.hasDisconnected() || player.level() != level || player.position().distanceToSqr(ground.player()) > 36.0) {
                leave();
                return;
            }
            if (outcome != Outcome.NONE) {
                if (t >= endAt) {
                    finish();
                }
                return;
            }
            if (t == 1 || t % 10 == 0) {
                townWatches();
            }
            if (t == 5) {
                spawnBoss();
            }
            if (boss != null) {
                if (!boss.isAlive()) {
                    leave();
                    return;
                }
                face(boss, player.getEyePosition());
            }
            if (t == 20 && boss != null) {
                says(DuelText.GREETING);
            }
            for (int toll = 0; toll < 3; toll++) {
                if (t == WALK + toll * TOLL_GAP) {
                    Vec3 c = ground.centre();
                    level.playSound(null, c.x, c.y + 6, c.z, SoundEvents.BELL_BLOCK, SoundSource.BLOCKS, 3.0F, 0.6F);
                    hud(player, DuelHudPayload.TOLL, bounty.target(), toll + 1);
                }
            }
            if (t == drawAt) {
                draw();
            }
            if (t >= drawAt && boss != null) {
                if (t >= nextShot) {
                    bossFires();
                }
                if (outcome == Outcome.NONE && t > drawAt + TIMEOUT) {
                    player.sendSystemMessage(Component.translatable(DuelText.STOOD_DOWN, bounty.target()));
                    leave();
                }
            }
        }

        /** Runs the duel on until its tick count is {@code tick} (for tests). */
        public void runTo(int tick) {
            while (t < tick && !over) {
                tick();
            }
        }

        private void spawnBoss() {
            OutlawBoss made = ModEntities.OUTLAW_BOSS.get().create(level, EntitySpawnReason.EVENT);
            if (made == null) {
                leave();
                return;
            }
            Vec3 at = ground.boss();
            made.setBounty(new BountyData(bounty.id(), bounty.target(), bounty.x(), bounty.z(), raised(bounty.reward()), true, false));
            made.snapTo(at.x, at.y, at.z, 0.0F, 0.0F);
            made.finalizeSpawn(level, level.getCurrentDifficultyAt(BlockPos.containing(at)), EntitySpawnReason.EVENT, null);
            made.setDuelling(true);
            face(made, player.getEyePosition());
            level.addFreshEntityWithPassengers(made);
            boss = made;
        }

        private void draw() {
            Vec3 c = ground.centre();
            level.playSound(null, c.x, c.y + 2, c.z, SoundEvents.BELL_BLOCK, SoundSource.BLOCKS, 3.0F, 1.7F);
            hud(player, DuelHudPayload.DRAW, bounty.target(), flinches * FLINCH_TICKS);
            if (boss != null) {
                boss.setGunPose(GunWielder.GunPose.AIMING);
            }
        }

        // A pull on a locked trigger. Before the draw it's a flinch (a held button is one pull, not one every repeat)
        private void pull() {
            if (t < drawAt && t - lastPull >= FLINCH_GAP && flinches < MAX_FLINCHES) {
                flinches++;
                player.sendOverlayMessage(Component.translatable(DuelText.FLINCH).withStyle(ChatFormatting.RED));
            }
            lastPull = t;
        }

        private void bossFires() {
            nextShot = t + BOSS_COOLDOWN;
            boolean lands = landsNext != null ? landsNext : level.getRandom().nextInt(HIT_IN) != 0;
            Vec3 at = player.position().add(0, 1.1, 0);
            if (!lands) {
                Vec3 along = ground.player().subtract(ground.boss()).normalize();
                Vec3 aside = new Vec3(-along.z, 0, along.x).scale(level.getRandom().nextBoolean() ? 1.5 : -1.5);
                at = at.add(aside).add(0, 0.5, 0);
            }
            face(boss, at);
            boss.onShot();
            // Seen and heard as any shot; what it does is settled here, so nothing in the way of it changes anything
            GunMechanics.fire(level, boss, GunTypes.DUEL_REVOLVER, anything -> true);
            if (lands) {
                lose();
            }
        }

        private DamageSource bullet(LivingEntity from) {
            return new DamageSource(ModDamageTypes.holder(level, ModDamageTypes.BULLET), from, from);
        }

        private void win(boolean firstShotToTheHead) {
            outcome = Outcome.WON;
            cleanKill = firstShotToTheHead;
            endAt = t + AFTER;
            if (boss != null) {
                resolving = true;
                boss.hurtServer(level, bullet(player), 100_000.0F);
                resolving = false;
                if (boss.isAlive()) {
                    boss.kill(level);
                }
            }
            DuelData.set(player, DuelData.of(player).closed(bounty.id()).wonAt(town));
            if (town != null) {
                Standing.add(player, town, WON_STANDING);
            }
            ModCriteria.fire(player, ModCriteria.DUEL_WON);
            if (reaction <= FASTEST) {
                ModCriteria.fire(player, ModCriteria.DUEL_WON_DEAREST);
            }
            if (firstShotToTheHead) {
                ModCriteria.fire(player, ModCriteria.DUEL_HEADSHOT);
            }
            hud(player, DuelHudPayload.WON, bounty.target(), 0);
            player.sendSystemMessage(Component.translatable(firstShotToTheHead ? DuelText.WON_HEAD : DuelText.WON_LINE, bounty.target()));
            int cheers = 0;
            for (Townsfolk folk : level.getEntitiesOfClass(Townsfolk.class, new AABB(BlockPos.containing(ground.centre())).inflate(24, 6, 24))) {
                if (cheers++ < 4) {
                    folk.playCelebrateSound();
                }
            }
        }

        private void lose() {
            outcome = Outcome.LOST;
            endAt = t + AFTER;
            // He's worth more for it, on the poster and on the board, and he's back at his camp for whoever comes
            BountyData dearer = new BountyData(bounty.id(), bounty.target(), bounty.x(), bounty.z(), raised(bounty.reward()), false, false);
            ItemStack poster = findPoster(player, bounty.id());
            if (poster != null) {
                poster.set(ModDataComponents.BOUNTY.get(), dearer);
            }
            BountyBoards boards = BountyBoards.get(level);
            boards.keyHolding(bounty.id()).ifPresent(key -> boards.peek(key).flatMap(board -> board.postings().stream()
                    .filter(p -> p.bounty().id().equals(bounty.id())).findFirst())
                    .ifPresent(p -> boards.update(key, new BoardPosting(dearer, p.raider(), p.status(), p.takenBy(), p.postedDay()))));
            DuelData.set(player, DuelData.of(player).closed(bounty.id()).lostAt(town));
            if (town != null) {
                Standing.add(player, town, -LOST_STANDING);
            }
            hud(player, DuelHudPayload.LOST, bounty.target(), 0);
            if (boss != null) {
                boss.setGunPose(GunWielder.GunPose.LOWERED);
            }
            if (lethal != null ? lethal : DuelConfig.lethalDuels()) {
                root(false);
                disarm(); // the town's revolver doesn't fall with the rest of what they carried
                resolving = true;
                // His by name, but with nobody behind it: a mob's bullet goes by the difficulty, and this one doesn't
                player.hurtServer(level, new DamageSource(ModDamageTypes.holder(level, ModDamageTypes.BULLET), boss, null), Float.MAX_VALUE);
                resolving = false;
                return;
            }
            int purse = Math.min(Purse.count(player) * PURSE_PERCENT / 100, bounty.reward() * Purse.GOLD);
            Purse.pay(player, purse);
            if (player.getHealth() > LOSER_HEALTH + 1.0F && boss != null) {
                resolving = true;
                player.hurtServer(level, bullet(boss), 1.0F);
                resolving = false;
            }
            player.setHealth(Math.min(player.getHealth(), LOSER_HEALTH));
            player.addEffect(new MobEffectInstance(MobEffects.SLOWNESS, DAZED, 3));
            player.addEffect(new MobEffectInstance(MobEffects.NAUSEA, DAZED, 0));
            player.sendSystemMessage(Component.translatable(DuelText.LOST_LINE, bounty.target()));
            if (purse > 0) {
                player.sendSystemMessage(Component.translatable(DuelText.LOST_PURSE, purse / Purse.GOLD, purse % Purse.GOLD));
            }
            if (boss != null) {
                says(DuelText.LOST_SAYS);
            }
        }

        private void says(String key) {
            player.sendSystemMessage(Component.translatable(DuelText.SAYS, boss.getDisplayName(), Component.translatable(key)));
        }

        // It's decided and they've stood there long enough
        private void finish() {
            bossGoes();
            close();
        }

        /** Called off with nothing decided (or the player gone): he goes, and the poster is as it was. */
        public void leave() {
            if (over) {
                return;
            }
            if (outcome == Outcome.NONE) {
                markPoster(false);
                if (!player.hasDisconnected() && player.isAlive()) {
                    hud(player, DuelHudPayload.CLEAR, "", 0);
                    player.sendSystemMessage(Component.translatable(DuelText.CALLED_OFF, bounty.target()));
                }
            }
            bossGoes();
            close();
        }

        private void bossGoes() {
            if (boss != null && boss.isAlive()) {
                level.sendParticles(ParticleTypes.POOF, boss.getX(), boss.getY() + 1.0, boss.getZ(), 12, 0.3, 0.6, 0.3, 0.02);
                boss.discard();
            }
            boss = null;
        }

        private void close() {
            over = true;
            root(false);
            disarm();
            ACTIVE.remove(player.getUUID(), this);
        }

        // Townsfolk and the law get out of the line between the two, and watch
        private void townWatches() {
            Vec3 a = ground.player(), b = ground.boss(), c = ground.centre();
            Vec3 along = b.subtract(a);
            double length = along.length();
            Vec3 unit = along.scale(1.0 / length);
            Vec3 aside = new Vec3(-unit.z, 0, unit.x);
            for (Mob mob : level.getEntitiesOfClass(Mob.class, new AABB(BlockPos.containing(c)).inflate(24, 6, 24),
                    m -> m instanceof Townsfolk || m instanceof Lawman)) {
                Vec3 rel = mob.position().subtract(a);
                double forward = rel.dot(unit), sideways = rel.dot(aside);
                if (forward > -4.0 && forward < length + 4.0 && Math.abs(sideways) < 4.0) {
                    Vec3 to = a.add(unit.scale(forward)).add(aside.scale(sideways < 0 ? -6.5 : 6.5));
                    mob.getNavigation().moveTo(to.x, mob.getY(), to.z, 1.15);
                } else {
                    mob.getLookControl().setLookAt(c.x, c.y + 1.2, c.z);
                }
            }
        }

        private static void face(Mob mob, Vec3 at) {
            Vec3 eye = mob.getEyePosition();
            double dx = at.x - eye.x, dy = at.y - eye.y, dz = at.z - eye.z;
            float yaw = (float) (Mth.atan2(dz, dx) * Mth.RAD_TO_DEG) - 90.0F;
            float pitch = (float) -(Mth.atan2(dy, Math.sqrt(dx * dx + dz * dz)) * Mth.RAD_TO_DEG);
            mob.setYRot(yaw);
            mob.setXRot(pitch);
            mob.yHeadRot = yaw;
            mob.yBodyRot = yaw;
        }
    }
}
