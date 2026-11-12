package com.hollowfrontier.content.ufo.taken;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import org.jspecify.annotations.Nullable;

import com.hollowfrontier.content.frontier.outlaw.AbstractOutlaw;
import com.hollowfrontier.content.frontier.townsfolk.Folk;
import com.hollowfrontier.content.frontier.townsfolk.Townsfolk;
import com.hollowfrontier.content.hollow.gray.Captive;
import com.hollowfrontier.content.hollow.gray.Captives;
import com.hollowfrontier.content.hollow.gray.Specimen;
import com.hollowfrontier.content.ufo.GrayWarden;
import com.hollowfrontier.content.ufo.taken.ShipLayout.Side;
import com.hollowfrontier.registry.ModAttachments;
import com.hollowfrontier.registry.ModBlocks;
import com.hollowfrontier.registry.ModEntities;
import com.hollowfrontier.registry.ModSounds;
import com.hollowfrontier.worldgen.structure.Blueprint;
import com.hollowfrontier.worldgen.structure.ShipBlueprints;

import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerBossEvent;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.BossEvent;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

/**
 * One ship with someone taken aboard it (50): what's on it and what state it's in. Nothing of a ship is saved: it lasts as
 * long as somebody is aboard and is cleared when the last of them is off ({@link Taken}). Where things are is
 * {@link ShipLayout}'s business; this is what happens.
 * <ul>
 * <li><b>The pen.</b> The taken wake behind a field. About {@link #CALL_AT} ticks in, a surgeon comes for the newest:
 *     the field drops when it reaches the pen (by {@link #PEN_OPENS_BY} at the latest), and for {@link #DISTRACTED} ticks
 *     it's busy at the wall and sees nothing. Then it looks in the pen, and goes back to its work.</li>
 * <li><b>The crew</b> are two surgeons at the rig (Wardens with rods, no pistols), a Warden walking the ring, and one
 *     standing in the engine room. What they {@link #sees see}: someone walking within {@link #WALK_SIGHT}, someone
 *     sneaking within {@link #SNEAK_SIGHT}, in plain view; nobody in the vents; anyone who has just hurt them.</li>
 * <li><b>The alarm.</b> A Gray that has had someone in its sights for {@link #CHASE_TO_ALARM} ticks, without being put
 *     down or stunned, raises it. The doors shut and open only while one of the crew is at them, the crew see half as
 *     far again, and it stands down {@link #ALARM_TICKS} after the last of them lost sight of anyone.</li>
 * <li><b>The fall.</b> With the core's three cells out (or anything blown up beside it) the ship has {@link #FALL_TICKS}
 *     left. The doors open, the crew run for their own way off, the lights go out from the middle, and whoever is still
 *     aboard at the end goes down with it.</li>
 * </ul>
 */
public final class Ship {
    public static final double WALK_SIGHT = 12.0, SNEAK_SIGHT = 4.0, ALARM_SIGHT = 1.5;
    public static final int CALL_AT = 100, PEN_OPENS_BY = 900, DISTRACTED = 240;
    public static final int CHASE_TO_ALARM = 140, ALARM_TICKS = 900, FALL_TICKS = 1800, LAUNCH_DELAY = 50;
    /** Keeping low, the taken can tell where the crew are: any of them this near a crouching player shows through the walls. */
    public static final double LISTEN_RANGE = 20.0;
    /** The freed pitch in: how near a Gray that's after somebody has to be, what a blow of theirs does, and how often. */
    public static final double BRAWL_RANGE = 12.0;
    public static final float BRAWL_DAMAGE = 3.0F;
    public static final int BRAWL_EVERY = 30;
    /** What each of the held had saved from supper, handed over when they're let out. */
    public static final int BREAD_EACH = 2;
    public static final int CELLS = 3;
    /** How near the pod a freed captive has to be to get away in it. */
    public static final double POD_REACH = 24.0;
    public static final String CREW = "hollowfrontier.ship_crew";

    public enum Stage {
        QUIET(1), ALARM(2), FALLING(3);

        /** What the client is told (the {@code ABOARD} attachment); 0 is not aboard. */
        public final int code;

        Stage(int code) {
            this.code = code;
        }
    }

    private final int index;
    private final ResourceKey<Level> dimension;
    private final BlockPos origin;
    private final Set<UUID> players = new LinkedHashSet<>();
    private final List<GrayWarden> crew = new ArrayList<>();
    private final List<Mob> held = new ArrayList<>(), freed = new ArrayList<>();
    private final Map<UUID, Integer> chase = new HashMap<>();
    private final int[] launchAt = {-1, -1};
    private @Nullable GrayWarden caller;
    private @Nullable Mob beast;
    private @Nullable ServerBossEvent bar, callBar;
    private double callFrom = -1;
    private boolean listenNoted;
    private int age, captivesAtStart, lost;
    private boolean manned, penOpen, pensOpen, holdOpen, everSeen, everAlarm;
    private int callerBusyUntil = -1, alarmUntil = -1, fallAt = -1, pulled, lightsOut;

    Ship(int index, ResourceKey<Level> dimension, BlockPos origin) {
        this.index = index;
        this.dimension = dimension;
        this.origin = origin;
    }

    // ---------------------------------------------------------------- what it is

    public int index() {
        return index;
    }

    public ResourceKey<Level> dimension() {
        return dimension;
    }

    public BlockPos origin() {
        return origin;
    }

    public BlockPos world(BlockPos local) {
        return origin.offset(local);
    }

    public BlockPos local(BlockPos world) {
        return world.subtract(origin);
    }

    public boolean contains(BlockPos world) {
        BlockPos at = local(world);
        return at.getY() >= -8 && at.getY() < ShipLayout.HEIGHT + 4 && at.getX() >= -2 && at.getZ() >= -2
                && at.getX() < ShipLayout.SIZE + 2 && at.getZ() < ShipLayout.SIZE + 2;
    }

    public AABB box() {
        return new AABB(origin.getX() - 2, origin.getY() - 8, origin.getZ() - 2,
                origin.getX() + ShipLayout.SIZE + 2, origin.getY() + ShipLayout.HEIGHT + 4, origin.getZ() + ShipLayout.SIZE + 2);
    }

    public Set<UUID> players() {
        return players;
    }

    public int age() {
        return age;
    }

    public Stage stage() {
        return fallAt >= 0 ? Stage.FALLING : alarmUntil >= 0 ? Stage.ALARM : Stage.QUIET;
    }

    public boolean falling() {
        return fallAt >= 0;
    }

    public int fallLeft() {
        return fallAt < 0 ? -1 : Math.max(0, FALL_TICKS - (age - fallAt));
    }

    public boolean everSeen() {
        return everSeen || everAlarm;
    }

    public List<GrayWarden> crew() {
        return crew;
    }

    public List<Mob> held() {
        return held;
    }

    public List<Mob> freed() {
        return freed;
    }

    public @Nullable Mob beast() {
        return beast;
    }

    public boolean penOpen() {
        return penOpen;
    }

    public int pulled() {
        return pulled;
    }

    /** Nobody the ship held is still held, and none was lost (to the rig, or to anything else). */
    public boolean nobodyLeft() {
        return captivesAtStart > 0 && held.isEmpty() && lost == 0;
    }

    void add(ServerPlayer player) {
        players.add(player.getUUID());
    }

    private boolean kept;

    /** For tests and the photo rig: the ship stays though nobody is aboard it. */
    public void keep(boolean keep) {
        kept = keep;
    }

    public boolean kept() {
        return kept;
    }

    void remove(ServerPlayer player) {
        players.remove(player.getUUID());
        if (bar != null) {
            bar.removePlayer(player);
        }
        if (callBar != null) {
            callBar.removePlayer(player);
        }
    }

    public List<ServerPlayer> aboard(ServerLevel level) {
        List<ServerPlayer> list = new ArrayList<>();
        for (UUID id : players) {
            ServerPlayer player = level.getServer().getPlayerList().getPlayer(id);
            if (player != null && player.level() == level && player.isAlive()) {
                list.add(player);
            }
        }
        return list;
    }

    // ---------------------------------------------------------------- raising and clearing

    /** The ship's blocks set down at its origin, over whatever was left there. No crew yet: {@link #man} brings them. */
    void raise(ServerLevel level) {
        for (int cx = origin.getX() >> 4; cx <= (origin.getX() + ShipLayout.SIZE) >> 4; cx++) {
            for (int cz = origin.getZ() >> 4; cz <= (origin.getZ() + ShipLayout.SIZE) >> 4; cz++) {
                level.getChunk(cx, cz);
                level.setChunkForced(cx, cz, true);
            }
        }
        empty(level);
        Blueprint ship = plan();
        for (BlockPos at : ship.positions()) {
            BlockState state = ship.get(at);
            if (!state.isAir()) {
                level.setBlock(world(at), state, Block.UPDATE_CLIENTS | Block.UPDATE_KNOWN_SHAPE);
            }
        }
    }

    private static @Nullable Blueprint plan;
    private static @Nullable List<BlockPos> lights;

    private static Blueprint plan() {
        if (plan == null) {
            plan = ShipBlueprints.ship();
        }
        return plan;
    }

    /** The ship's lights, from the middle out: the order they fail in. */
    private static List<BlockPos> lights() {
        if (lights == null) {
            Blueprint ship = plan();
            List<BlockPos> found = new ArrayList<>();
            for (BlockPos at : ship.positions()) {
                if (ship.get(at).is(Blocks.SEA_LANTERN) && ShipLayout.radius(at.getX(), at.getZ()) <= ShipLayout.HULL_R) {
                    found.add(at);
                }
            }
            found.sort(Comparator.comparingDouble(at -> ShipLayout.radius(at.getX(), at.getZ())));
            lights = List.copyOf(found);
        }
        return lights;
    }

    private void empty(ServerLevel level) {
        for (Entity entity : level.getEntities((Entity) null, box(), e -> !(e instanceof Player))) {
            entity.discard();
        }
        BlockState air = Blocks.AIR.defaultBlockState();
        for (BlockPos at : BlockPos.betweenClosed(origin, origin.offset(ShipLayout.SIZE - 1, ShipLayout.HEIGHT - 1, ShipLayout.SIZE - 1))) {
            if (!level.getBlockState(at).isAir()) {
                level.setBlock(at, air, Block.UPDATE_CLIENTS | Block.UPDATE_KNOWN_SHAPE);
            }
        }
    }

    /** Gone: everything aboard with it, and its place in the sky left as it was found. */
    void clear(ServerLevel level) {
        if (bar != null) {
            bar.removeAllPlayers();
            bar = null;
        }
        endCall();
        empty(level);
        for (int cx = origin.getX() >> 4; cx <= (origin.getX() + ShipLayout.SIZE) >> 4; cx++) {
            for (int cz = origin.getZ() >> 4; cz <= (origin.getZ() + ShipLayout.SIZE) >> 4; cz++) {
                level.setChunkForced(cx, cz, false);
            }
        }
    }

    /** The crew, the captives and the stock come aboard. */
    public void man(ServerLevel level) {
        manned = true;
        // The held: an outlaw, somebody from a town and a gunslinger in the pens, and one more already on the table
        EntityType<?>[] types = {ModEntities.OUTLAW.get(), ModEntities.TOWNSFOLK.get(), ModEntities.OUTLAW_GUNSLINGER.get()};
        for (int i = 0; i < ShipLayout.CAPTIVE_PENS.length; i++) {
            hold(level, types[i], ShipLayout.penMiddle(ShipLayout.CAPTIVE_PENS[i]), Captive.HELD, Folk.WRANGLER);
        }
        hold(level, ModEntities.TOWNSFOLK.get(), ShipLayout.PATIENT, Captive.ON_TABLE, Folk.BLACKSMITH);
        captivesAtStart = held.size();
        // The crew: the surgeons, the Warden on its round, and the one that keeps the engine
        for (BlockPos at : ShipLayout.SURGEONS) {
            GrayWarden surgeon = crew(level, at, Captives.SURGEON, Captives.GUARD, GrayWarden.UNARMED);
            if (surgeon != null && caller == null) {
                caller = surgeon;
            }
        }
        List<BlockPos> round = ShipLayout.round().stream().map(this::world).toList();
        GrayWarden walker = crew(level, ShipLayout.round().get(4), Captives.GUARD);
        if (walker != null) {
            walker.setRound(round, true);
        }
        crew(level, new BlockPos(ShipLayout.C + 4, ShipLayout.DECK, ShipLayout.C + 3), Captives.GUARD);
        // The hold: something they've been at with their implants, and somebody's cattle
        if (ModEntities.TERROR_BIRD.get().create(level, EntitySpawnReason.STRUCTURE) instanceof Mob bird) {
            place(bird, ShipLayout.BEAST);
            if (bird instanceof Specimen specimen) {
                specimen.setTagged(true);
            }
            bird.setPersistenceRequired();
            level.addFreshEntity(bird);
            beast = bird;
        }
        for (BlockPos at : ShipLayout.CATTLE) {
            Mob cow = EntityTypes.COW.create(level, EntitySpawnReason.STRUCTURE);
            if (cow != null) {
                place(cow, at);
                cow.setPersistenceRequired();
                level.addFreshEntity(cow);
            }
        }
    }

    private void place(Entity entity, BlockPos local) {
        Vec3 at = ShipLayout.feet(origin, local);
        entity.snapTo(at.x, at.y, at.z, entity.getRandom().nextFloat() * 360.0F, 0.0F);
    }

    private void hold(ServerLevel level, EntityType<?> type, BlockPos local, Captive how, Folk folk) {
        if (!(type.create(level, EntitySpawnReason.STRUCTURE) instanceof Mob mob)) {
            return;
        }
        place(mob, local);
        mob.finalizeSpawn(level, level.getCurrentDifficultyAt(world(local)), EntitySpawnReason.STRUCTURE, null);
        if (mob instanceof Townsfolk townsfolk) {
            townsfolk.setFolk(folk);
        }
        mob.setData(ModAttachments.CAPTIVE.get(), how);
        Captives.restrain(mob);
        level.addFreshEntity(mob);
        held.add(mob);
    }

    private @Nullable GrayWarden crew(ServerLevel level, BlockPos local, String... tags) {
        GrayWarden warden = ModEntities.GRAY_WARDEN.get().create(level, EntitySpawnReason.STRUCTURE);
        if (warden == null) {
            return null;
        }
        place(warden, local);
        warden.addTag(CREW);
        for (String tag : tags) {
            warden.addTag(tag);
        }
        warden.setPersistenceRequired();
        level.addFreshEntity(warden);
        if (warden.isRemoved()) { // peaceful: there's nobody aboard but the held
            return null;
        }
        crew.add(warden);
        return warden;
    }

    // ---------------------------------------------------------------- every tick

    void tick(ServerLevel level) {
        age++;
        List<ServerPlayer> aboard = aboard(level);
        if (!manned && !aboard.isEmpty()) {
            man(level);
        }
        for (ServerPlayer player : aboard) {
            if (player.getY() < origin.getY() - 6) {
                Taken.leave(player, Taken.Way.FELL);
            }
        }
        script(level, aboard);
        coming(aboard);
        if (age % 10 == 0) {
            listen(aboard);
            brawl(level);
        }
        if (age % 5 == 0) {
            crew.removeIf(w -> !w.isAlive());
            watch(level, aboard);
            doors(level);
        }
        if (age % 20 == 0) {
            captives(level, aboard);
            Stage stage = stage();
            for (ServerPlayer player : aboard) {
                int was = player.getData(ModAttachments.ABOARD.get()), noted = was & Taken.VENT_NOTED;
                if ((was & ~Taken.VENT_NOTED) != stage.code) {
                    player.setData(ModAttachments.ABOARD.get(), stage.code | noted);
                }
            }
            if (stage == Stage.ALARM && age % 40 == 0) {
                sound(level, aboard, ModSounds.ABOARD_ALARM.get(), 0.7F, 1.0F);
            }
        }
        for (int pod = 0; pod < launchAt.length; pod++) {
            if (launchAt[pod] >= 0 && age >= launchAt[pod]) {
                launch(level, pod);
            }
        }
        if (fallAt >= 0) {
            fall(level, aboard);
        }
    }

    private static void sound(ServerLevel level, List<ServerPlayer> to, SoundEvent sound, float volume, float pitch) {
        for (ServerPlayer player : to) {
            level.playSound(null, player.getX(), player.getY(), player.getZ(), sound, SoundSource.AMBIENT, volume, pitch);
        }
    }

    private void script(ServerLevel level, List<ServerPlayer> aboard) {
        if (penOpen) {
            // The caller has finished at the wall: it looks in the pen, then goes back to the table
            if (callerBusyUntil >= 0 && age == callerBusyUntil && caller != null && caller.isAlive()) {
                List<BlockPos> way = new ArrayList<>(List.of(ShipLayout.WAKE));
                way.addAll(ShipLayout.along(Side.NORTH, Side.SOUTH));
                way.add(ShipLayout.SURGEONS[0]);
                caller.setRound(way.stream().map(this::world).toList(), false);
            }
            return;
        }
        Mob neighbour = held.stream().filter(m -> !Captives.onTable(m)).findFirst().orElse(null);
        switch (age) {
            case 50 -> aboard.forEach(p -> Taken.note(p, TakenText.NOTE_WAKE));
            case 120 -> aboard.forEach(p -> Taken.note(p, TakenText.NOTE_EMPTY_HANDS));
            case 190 -> aboard.forEach(p -> Taken.says(p, neighbour, TakenText.SAY_NEW));
            case 260 -> aboard.forEach(p -> Taken.says(p, neighbour, TakenText.SAY_WAIT));
            default -> { }
        }
        if (age == CALL_AT && caller != null && caller.isAlive()) {
            // Half way round the ship: out of the surgery, along the ring, in at the pens
            List<BlockPos> way = new ArrayList<>(ShipLayout.along(Side.SOUTH, Side.NORTH));
            way.add(ShipLayout.PEN_CALL);
            caller.setRound(way.stream().map(this::world).toList(), false, 0.85);
        }
        boolean arrived = caller != null && caller.isAlive() && age > CALL_AT && caller.roundDone();
        if (arrived || age >= PEN_OPENS_BY) {
            openPen(level);
            callerBusyUntil = age + DISTRACTED;
            aboard.forEach(p -> Taken.note(p, TakenText.NOTE_PEN_OPEN));
            aboard.forEach(p -> Taken.says(p, neighbour, TakenText.SAY_GO));
        }
    }

    /**
     * What the taken are shown of the one coming for them: a bar that fills as it nears the pen, then one that runs down
     * while its back is turned. It's gone once the caller has looked in, or is dead.
     */
    private void coming(List<ServerPlayer> aboard) {
        boolean onItsWay = caller != null && caller.isAlive() && age > CALL_AT && (!penOpen || age < callerBusyUntil) && !pensOpen && fallAt < 0;
        if (!onItsWay) {
            endCall();
            return;
        }
        BlockPos pen = world(ShipLayout.PEN_CALL);
        double distance = Math.sqrt(caller.blockPosition().distSqr(pen));
        if (callFrom < 0) {
            callFrom = Math.max(1.0, distance);
        }
        boolean turned = penOpen;
        if (callBar == null) {
            callBar = new ServerBossEvent(UUID.randomUUID(), Component.translatable(TakenText.BAR_COMING), BossEvent.BossBarColor.GREEN, BossEvent.BossBarOverlay.PROGRESS);
        }
        callBar.setName(Component.translatable(turned ? TakenText.BAR_TURNED : TakenText.BAR_COMING));
        callBar.setColor(turned ? BossEvent.BossBarColor.YELLOW : BossEvent.BossBarColor.GREEN);
        callBar.setProgress((float) Math.max(0.0, Math.min(1.0, turned ? (callerBusyUntil - age) / (double) DISTRACTED : 1.0 - distance / callFrom)));
        aboard.forEach(callBar::addPlayer);
    }

    private void endCall() {
        if (callBar != null) {
            callBar.removeAllPlayers();
            callBar = null;
        }
    }

    /** The crew near anyone keeping low, and the one on its way to the pen, show through the walls. */
    private void listen(List<ServerPlayer> aboard) {
        for (GrayWarden warden : crew) {
            boolean heard = warden == caller && callBar != null;
            for (ServerPlayer player : aboard) {
                if (player.isCrouching() && warden.distanceTo(player) <= LISTEN_RANGE) {
                    heard = true;
                    if (!listenNoted) {
                        listenNoted = true;
                        Taken.note(player, TakenText.NOTE_LISTEN);
                    }
                }
            }
            if (warden.isCurrentlyGlowing() != heard) {
                warden.setGlowingTag(heard);
            }
        }
    }

    /** The freed pitch in: any of them near a Gray that's after somebody goes for it with whatever comes to hand. */
    private void brawl(ServerLevel level) {
        for (Mob mob : freed) {
            GrayWarden foe = crew.stream().filter(w -> w.getTarget() != null && !w.isStunned() && w.distanceTo(mob) <= BRAWL_RANGE)
                    .min(Comparator.comparingDouble(mob::distanceToSqr)).orElse(null);
            if (foe == null) {
                continue;
            }
            mob.getLookControl().setLookAt(foe, 30.0F, 30.0F);
            if (mob.distanceToSqr(foe) > 5.0) {
                mob.getNavigation().moveTo(foe, 1.3);
            } else if ((age + mob.getId() * 7) % BRAWL_EVERY < 10) {
                level.playSound(null, foe.blockPosition(), SoundEvents.PLAYER_ATTACK_STRONG, SoundSource.NEUTRAL, 0.8F, 0.9F);
                foe.hurtServer(level, level.damageSources().mobAttack(mob), BRAWL_DAMAGE);
            }
        }
    }

    /** The field across the pens the taken are in comes down. */
    public void openPen(ServerLevel level) {
        penOpen = true;
        drop(level, ShipLayout.penField(ShipLayout.PLAYER_PEN));
        drop(level, ShipLayout.penField(ShipLayout.PENS.length - 1));
    }

    private void drop(ServerLevel level, List<BlockPos> field) {
        boolean any = false;
        for (BlockPos at : field) {
            BlockPos pos = world(at);
            if (level.getBlockState(pos).is(ModBlocks.GRAY_FIELD.get())) {
                level.setBlock(pos, Blocks.AIR.defaultBlockState(), Block.UPDATE_ALL);
                any = true;
            }
        }
        if (any) {
            BlockPos middle = world(field.get(field.size() / 2));
            level.playSound(null, middle, ModSounds.GRAY_FIELD_DROP.get(), SoundSource.BLOCKS, 1.0F, 1.0F);
            level.sendParticles(ParticleTypes.ELECTRIC_SPARK, middle.getX() + 0.5, middle.getY() + 0.5, middle.getZ() + 0.5, 12, 0.8, 0.8, 0.8, 0.02);
        }
    }

    private void raise(ServerLevel level, List<BlockPos> field) {
        for (BlockPos at : field) {
            BlockPos pos = world(at);
            if (level.getBlockState(pos).isAir()) {
                level.setBlock(pos, ModBlocks.GRAY_FIELD.get().defaultBlockState(), Block.UPDATE_ALL);
            }
        }
    }

    // ---------------------------------------------------------------- the crew's eyes, and the alarm

    /** Whether one of the crew can make out a player (see the class notes). */
    public boolean sees(GrayWarden warden, Player player) {
        if (fallAt >= 0 || warden.isStunned() || (warden == caller && age < callerBusyUntil)) {
            return false;
        }
        if (ShipLayout.inVent(player.getX() - origin.getX(), player.getZ() - origin.getZ())) {
            return false;
        }
        if (warden.getLastHurtByMob() == player && warden.tickCount - warden.getLastHurtByMobTimestamp() < 100) {
            return true;
        }
        double range = (player.isCrouching() ? SNEAK_SIGHT : WALK_SIGHT) * (alarmUntil >= 0 ? ALARM_SIGHT : 1.0);
        return warden.distanceTo(player) <= range && warden.hasLineOfSight(player);
    }

    private void watch(ServerLevel level, List<ServerPlayer> aboard) {
        boolean chasing = false;
        for (GrayWarden warden : crew) {
            LivingEntity target = warden.getTarget();
            if (target instanceof ServerPlayer player && players.contains(player.getUUID())) {
                if (fallAt >= 0 || ShipLayout.inVent(player.getX() - origin.getX(), player.getZ() - origin.getZ())) {
                    warden.setTarget(null);
                    chase.remove(warden.getUUID());
                    continue;
                }
                if (!everSeen) {
                    everSeen = true;
                    Taken.note(player, TakenText.NOTE_SEEN);
                }
                chasing = true;
                if (chase.merge(warden.getUUID(), 5, Integer::sum) >= CHASE_TO_ALARM && alarmUntil < 0) {
                    alarm(level, aboard);
                }
            } else {
                chase.remove(warden.getUUID());
            }
        }
        if (alarmUntil >= 0 && fallAt < 0) {
            if (chasing) {
                alarmUntil = age + ALARM_TICKS;
            } else if (age >= alarmUntil) {
                alarmUntil = -1;
                aboard.forEach(p -> Taken.note(p, TakenText.NOTE_ALARM_OVER));
            }
        }
        // Let out, the beast goes for whichever of the crew is nearest, and never for the held or the taken
        if (holdOpen && beast != null && beast.isAlive() && !(beast.getTarget() instanceof GrayWarden w && w.isAlive())) {
            crew.stream().min(Comparator.comparingDouble(beast::distanceToSqr)).ifPresent(beast::setTarget);
        }
    }

    /** The ship knows somebody is loose. */
    public void alarm(ServerLevel level, List<ServerPlayer> aboard) {
        if (fallAt >= 0) {
            return;
        }
        boolean fresh = alarmUntil < 0;
        alarmUntil = age + ALARM_TICKS;
        everAlarm = true;
        if (fresh) {
            sound(level, aboard, ModSounds.ABOARD_ALARM.get(), 1.0F, 1.0F);
            aboard.forEach(p -> Taken.note(p, TakenText.NOTE_ALARM));
        }
    }

    private void doors(ServerLevel level) {
        boolean shut = alarmUntil >= 0 && fallAt < 0;
        for (List<BlockPos> door : ShipLayout.doors()) {
            BlockPos middle = world(door.get(door.size() / 2));
            boolean want = shut && crew.stream().noneMatch(w -> w.blockPosition().closerThan(middle, 4.5));
            boolean is = level.getBlockState(world(door.get(0))).is(ModBlocks.GRAY_FIELD.get());
            if (want && !is) {
                raise(level, door);
                level.playSound(null, middle, ModSounds.GRAY_FIELD_DROP.get(), SoundSource.BLOCKS, 0.8F, 1.4F);
            } else if (!want && is) {
                drop(level, door);
            }
        }
    }

    // ---------------------------------------------------------------- the held

    private void captives(ServerLevel level, List<ServerPlayer> aboard) {
        for (Mob mob : List.copyOf(held)) {
            if (!mob.isAlive()) {
                held.remove(mob);
                lost++;
            } else if (!Captives.isHeld(mob)) {
                held.remove(mob);
                // Off the table the wrong way he's an outlaw with a gun again, and nobody's to walk out
                if (mob instanceof AbstractOutlaw outlaw && !outlaw.isFreed()) {
                    lost++;
                } else {
                    freed.add(mob);
                    // What they'd kept back from supper: the taken haven't eaten since the ground
                    aboard.stream().min(Comparator.comparingDouble(mob::distanceToSqr)).ifPresent(player -> {
                        com.hollowfrontier.content.frontier.bounty.BoardUse.give(player, new net.minecraft.world.item.ItemStack(net.minecraft.world.item.Items.BREAD, BREAD_EACH));
                        Taken.says(player, mob, TakenText.SAY_BREAD);
                    });
                }
            }
        }
        freed.removeIf(m -> !m.isAlive());
        if (aboard.isEmpty()) {
            return;
        }
        // The held call out to whoever passes, as they do in the pens below (the rig is too far off here to do it for them)
        for (Mob mob : held) {
            com.hollowfrontier.content.frontier.talk.Chatter.lookAround(mob);
        }
        for (Mob mob : freed) {
            ServerPlayer nearest = aboard.stream().min(Comparator.comparingDouble(mob::distanceToSqr)).orElseThrow();
            boolean fighting = crew.stream().anyMatch(w -> w.getTarget() != null && !w.isStunned() && w.distanceTo(mob) <= BRAWL_RANGE);
            if (!fighting && mob.distanceToSqr(nearest) > 9.0) {
                mob.getNavigation().moveTo(nearest, 1.25);
            }
        }
    }

    // ---------------------------------------------------------------- the consoles

    /** A console used; whether it did anything (and so goes dark). */
    public boolean console(ServerLevel level, ServerPlayer player, BlockPos pos, ShipConsoleBlock.Kind kind) {
        switch (kind) {
            case PENS -> {
                if (pensOpen) {
                    return false;
                }
                pensOpen = true;
                penOpen = true;
                for (int pen = 0; pen < ShipLayout.PENS.length; pen++) {
                    drop(level, ShipLayout.penField(pen));
                }
                Taken.note(player, TakenText.NOTE_PENS);
                Mob neighbour = held.stream().filter(m -> !Captives.onTable(m)).findFirst().orElse(null);
                Taken.says(player, neighbour, TakenText.SAY_PENS_OPEN);
                return true;
            }
            case HOLD -> {
                if (holdOpen) {
                    return false;
                }
                holdOpen = true;
                drop(level, ShipLayout.beastField());
                List<BlockPos> cattle = new ArrayList<>();
                for (int u = 3; u <= 9; u++) {
                    for (int y = ShipLayout.DECK; y <= ShipLayout.DECK + 2; y++) {
                        cattle.add(Side.EAST.at(u, 22, y));
                    }
                }
                drop(level, cattle);
                Taken.note(player, TakenText.NOTE_HOLD);
                return true;
            }
            case POD -> {
                int pod = ShipLayout.podAt(local(pos));
                if (pod < 0 || launchAt[pod] >= 0) {
                    return false;
                }
                launchAt[pod] = age + LAUNCH_DELAY;
                raise(level, hatch(pod));
                level.playSound(null, pos, ModSounds.POD_LAUNCH.get(), SoundSource.BLOCKS, 2.0F, 1.0F);
                Taken.note(player, TakenText.NOTE_POD);
                return false; // it goes dark when the pod has gone, not before
            }
            default -> {
                return false;
            }
        }
    }

    private static List<BlockPos> hatch(int pod) {
        return List.of(Side.WEST.at(ShipLayout.PODS[pod], 22, ShipLayout.DECK), Side.WEST.at(ShipLayout.PODS[pod], 22, ShipLayout.DECK + 1));
    }

    private void launch(ServerLevel level, int pod) {
        launchAt[pod] = -1;
        AABB cabin = ShipLayout.podCabin(pod).move(origin).inflate(0.3);
        List<ServerPlayer> riders = aboard(level).stream().filter(p -> cabin.intersects(p.getBoundingBox())).toList();
        if (riders.isEmpty()) {
            drop(level, hatch(pod)); // nobody in it: the hatch opens again
            return;
        }
        BlockPos console = world(ShipLayout.podConsole(pod));
        BlockState state = level.getBlockState(console);
        if (state.is(ModBlocks.SHIP_CONSOLE.get())) {
            level.setBlock(console, state.setValue(ShipConsoleBlock.SPENT, true), Block.UPDATE_ALL);
        }
        captives(level, riders);
        List<Mob> away = freed.stream().filter(m -> m.blockPosition().closerThan(console, POD_REACH)).toList();
        boolean everyone = nobodyLeft() && away.size() == freed.size() && freed.size() == captivesAtStart;
        freed.removeAll(away);
        Taken.podLanding(level, this, riders, away, everyone);
    }

    // ---------------------------------------------------------------- the core, and the fall

    /** One of the core's cells has been pulled. */
    public void cellPulled(ServerLevel level, ServerPlayer player) {
        pulled++;
        // The pillar goes dark from the top, a third for each cell
        int height = ShipLayout.CORE_TOP - ShipLayout.DECK + 1;
        int dark = Math.min(height, (int) Math.ceil(height * pulled / (double) CELLS));
        for (int i = 0; i < dark; i++) {
            BlockPos at = world(ShipLayout.CORE.atY(ShipLayout.CORE_TOP - i));
            if (level.getBlockState(at).is(ModBlocks.PHLOGISTON_LAMP.get())) {
                level.setBlock(at, Blocks.TINTED_GLASS.defaultBlockState(), Block.UPDATE_ALL);
            }
        }
        List<ServerPlayer> aboard = aboard(level);
        sound(level, aboard, ModSounds.ABOARD_GROAN.get(), 0.6F + 0.2F * pulled, 1.1F - 0.1F * pulled);
        if (pulled >= CELLS) {
            startFall(level);
        } else {
            Taken.note(player, pulled == 1 ? TakenText.NOTE_CELL_1 : TakenText.NOTE_CELL_2);
            if (pulled == 2) {
                alarm(level, aboard);
            }
        }
    }

    /** Nothing is holding the ship up. */
    public void startFall(ServerLevel level) {
        if (fallAt >= 0) {
            return;
        }
        fallAt = age;
        alarmUntil = age;
        everAlarm = true;
        List<ServerPlayer> aboard = aboard(level);
        aboard.forEach(p -> Taken.note(p, TakenText.NOTE_FALLING));
        sound(level, aboard, ModSounds.ABOARD_GROAN.get(), 1.2F, 0.7F);
        // The crew have their own way off, and no more time for anybody
        BlockPos door = world(Side.WEST.deck(0, 14)), bay = world(Side.WEST.deck(0, 20));
        for (GrayWarden warden : crew) {
            warden.setTarget(null);
            // Round the ring to the bay's door from whichever side they're nearest, then in
            Side nearest = Side.NORTH;
            for (Side side : Side.values()) {
                if (warden.blockPosition().distSqr(world(side.deck(0, 14))) < warden.blockPosition().distSqr(world(nearest.deck(0, 14)))) {
                    nearest = side;
                }
            }
            List<BlockPos> way = new ArrayList<>(ShipLayout.along(nearest, Side.WEST).stream().map(this::world).toList());
            way.add(door);
            way.add(bay);
            warden.setRound(way, false, 1.15);
        }
    }

    private void fall(ServerLevel level, List<ServerPlayer> aboard) {
        int left = fallLeft();
        if (bar == null) {
            bar = new ServerBossEvent(UUID.randomUUID(), Component.translatable(TakenText.BAR_FALLING), BossEvent.BossBarColor.RED, BossEvent.BossBarOverlay.NOTCHED_6);
        }
        bar.setProgress(left / (float) FALL_TICKS);
        aboard.forEach(bar::addPlayer);
        // The lights go out from the middle: all of them by two thirds of the way down
        int gone = Math.min(lights().size(), (int) ((age - fallAt) / (FALL_TICKS * 0.66) * lights().size()));
        for (; lightsOut < gone; lightsOut++) {
            BlockPos at = world(lights().get(lightsOut));
            if (level.getBlockState(at).is(Blocks.SEA_LANTERN)) {
                level.setBlock(at, Blocks.CONCRETE.gray().defaultBlockState(), Block.UPDATE_CLIENTS);
                level.sendParticles(ParticleTypes.ELECTRIC_SPARK, at.getX() + 0.5, at.getY() - 0.2, at.getZ() + 0.5, 4, 0.2, 0.1, 0.2, 0.05);
            }
        }
        // The crew go as they reach the bay
        for (GrayWarden warden : List.copyOf(crew)) {
            if (warden.roundDone()) {
                level.sendParticles(ParticleTypes.REVERSE_PORTAL, warden.getX(), warden.getY(1.0), warden.getZ(), 20, 0.3, 0.6, 0.3, 0.05);
                warden.playSound(SoundEvents.ENDERMAN_TELEPORT, 1.0F, 1.6F);
                warden.discard();
                crew.remove(warden);
            }
        }
        // The hull lets everyone know how long it has: a groan, and something giving way near whoever's aboard
        int every = left > 1200 ? 120 : left > 600 ? 70 : 35;
        if (left > 0 && left % every == 0 && !aboard.isEmpty()) {
            sound(level, aboard, ModSounds.ABOARD_GROAN.get(), 1.0F, 0.6F + level.getRandom().nextFloat() * 0.4F);
            ServerPlayer near = aboard.get(level.getRandom().nextInt(aboard.size()));
            double x = near.getX() + level.getRandom().nextInt(13) - 6, z = near.getZ() + level.getRandom().nextInt(13) - 6;
            double y = origin.getY() + ShipLayout.CEILING - 0.5;
            level.sendParticles(ParticleTypes.EXPLOSION, x, y, z, 1, 0, 0, 0, 0);
            level.sendParticles(ParticleTypes.LARGE_SMOKE, x, y, z, 12, 0.6, 0.3, 0.6, 0.02);
            level.sendParticles(ParticleTypes.LAVA, x, y, z, 6, 0.4, 0.1, 0.4, 0);
            level.playSound(null, x, y, z, SoundEvents.GENERIC_EXPLODE.value(), SoundSource.BLOCKS, 0.7F, 0.7F);
        }
        if (left == 400) {
            aboard.forEach(p -> Taken.note(p, TakenText.NOTE_FALL_LATE));
        }
        if (left <= 0) {
            Taken.wentDown(level, this, aboard);
        }
    }
}
