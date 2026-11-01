package com.hollowfrontier.content.frontier.raid;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import org.jspecify.annotations.Nullable;

import com.hollowfrontier.content.frontier.bank.Banking;
import com.hollowfrontier.content.frontier.bank.TownBanks;
import com.hollowfrontier.content.frontier.bounty.BountyData;
import com.hollowfrontier.content.frontier.bounty.OutlawNames;
import com.hollowfrontier.content.frontier.law.Wanted;
import com.hollowfrontier.content.frontier.outlaw.AbstractOutlaw;
import com.hollowfrontier.content.frontier.outlaw.OutlawBoss;
import com.hollowfrontier.content.frontier.raid.RaidMember.Role;
import com.hollowfrontier.content.frontier.townsfolk.Lawman;
import com.hollowfrontier.registry.ModAttachments;
import com.hollowfrontier.registry.ModCriteria;
import com.hollowfrontier.registry.ModEntities;
import com.hollowfrontier.registry.ModItems;
import com.hollowfrontier.registry.ModSounds;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.UUIDUtil;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ClientboundSetSubtitleTextPacket;
import net.minecraft.network.protocol.game.ClientboundSetTitleTextPacket;
import net.minecraft.network.protocol.game.ClientboundSetTitlesAnimationPacket;
import net.minecraft.server.level.ServerBossEvent;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.util.Prediction;
import net.minecraft.util.RandomSource;
import net.minecraft.util.StringRepresentable;
import net.minecraft.world.BossEvent;
import net.minecraft.world.Difficulty;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.animal.equine.AbstractHorse;
import net.minecraft.world.entity.animal.equine.Horse;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.SwingAnimation;
import net.minecraft.world.level.block.BellBlock;
import net.minecraft.world.level.block.DoorBlock;
import net.minecraft.world.level.block.entity.ChestBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

/**
 * One raid on one town, from the first toll of the bell to the dust settling. Saved with {@link TownRaids}, so a raid
 * picks up where it left off after a restart; the boss bar is rebuilt.
 * <ol>
 * <li><b>Warning</b> (timings.warning): the bell rings, hoofbeats and shots come closer from out of town, a title names
 *     the gang. Townsfolk hide, lawmen form up in the street (RaidGoals).</li>
 * <li><b>Assault:</b> the gang rides in (on horses, dismounting at the end of the street) led by a named boss. The
 *     safecracker heads for the vault and works it (crack overlay, sparks, a vault bar); killing him starts the job over
 *     (once, someone takes his place). The gang breaks when the leader dies or enough of them do (moraleBreak); the
 *     vault opening or the timeout also ends the fight.</li>
 * <li><b>Retreat</b> (timings.retreat): the gang mounts up and rides out. If they cracked the vault the safecracker
 *     carries the loot: kill him before he's gone and it goes back in the vault. Whoever is left at the end escapes.</li>
 * </ol>
 * Defenders are paid on the spot by the sheriff and get their heat cooled; three-star outlaws who kept out of the way
 * get a cut of a robbery. An escaped leader turns up on the town's Bounty Board.
 */
public class TownRaid {
    public enum Phase implements StringRepresentable {
        WARNING, ASSAULT, RETREAT;

        public static final Codec<Phase> CODEC = StringRepresentable.fromEnum(Phase::values);

        @Override
        public String getSerializedName() {
            return name().toLowerCase(Locale.ROOT);
        }
    }

    public enum Outcome implements StringRepresentable {
        /** Still being fought over. */
        NONE,
        /** The leader died or the gang broke. */
        DEFENDED,
        /** The vault was cracked, but the safecracker died before he got the loot out. */
        RECOVERED,
        /** The vault was cracked (and, once the raid is over, the loot got away). */
        ROBBED,
        /** The gang ran out of time and rode off empty-handed. */
        GAVE_UP;

        public static final Codec<Outcome> CODEC = StringRepresentable.fromEnum(Outcome::values);

        @Override
        public String getSerializedName() {
            return name().toLowerCase(Locale.ROOT);
        }

        public boolean townHeld() {
            return this == DEFENDED || this == RECOVERED || this == GAVE_UP;
        }
    }

    /** What one player did against the gang. */
    public record Contribution(int kills, boolean leader, boolean safecracker, float damage) {
        public static final Contribution NONE = new Contribution(0, false, false, 0.0F);
        public static final Codec<Contribution> CODEC = RecordCodecBuilder.create(i -> i.group(
                Codec.INT.fieldOf("kills").forGetter(Contribution::kills),
                Codec.BOOL.fieldOf("leader").forGetter(Contribution::leader),
                Codec.BOOL.fieldOf("safecracker").forGetter(Contribution::safecracker),
                Codec.FLOAT.fieldOf("damage").forGetter(Contribution::damage)).apply(i, Contribution::new));

        public boolean any() {
            return kills > 0 || damage > 0.0F;
        }
    }

    /** Running counts (mutable, saved). */
    public static final class Tally {
        static final Codec<Tally> CODEC = RecordCodecBuilder.create(i -> i.group(
                Codec.INT.fieldOf("planned").forGetter(t -> t.planned),
                Codec.BOOL.fieldOf("mounted").forGetter(t -> t.mounted),
                Codec.INT.fieldOf("gang_size").forGetter(t -> t.gangSize),
                Codec.INT.fieldOf("lost").forGetter(t -> t.lost),
                Codec.INT.fieldOf("lawmen").forGetter(t -> t.lawmenAtStart),
                Codec.BOOL.fieldOf("replaced").forGetter(t -> t.replaced),
                Codec.LONG.fieldOf("safecracker_lost").forGetter(t -> t.safecrackerLost),
                Codec.BOOL.fieldOf("leader_killed").forGetter(t -> t.leaderKilled),
                Codec.BOOL.fieldOf("cracked").forGetter(t -> t.cracked)).apply(i, Tally::new));

        int planned;
        boolean mounted;
        int gangSize;
        int lost;
        int lawmenAtStart;
        boolean replaced;
        long safecrackerLost;
        boolean leaderKilled;
        boolean cracked;

        Tally(int planned, boolean mounted, int gangSize, int lost, int lawmenAtStart, boolean replaced, long safecrackerLost,
                boolean leaderKilled, boolean cracked) {
            this.planned = planned;
            this.mounted = mounted;
            this.gangSize = gangSize;
            this.lost = lost;
            this.lawmenAtStart = lawmenAtStart;
            this.replaced = replaced;
            this.safecrackerLost = safecrackerLost;
            this.leaderKilled = leaderKilled;
            this.cracked = cracked;
        }
    }

    public static final Codec<TownRaid> CODEC = RecordCodecBuilder.create(i -> i.group(
            UUIDUtil.CODEC.fieldOf("id").forGetter(r -> r.id),
            TownSite.CODEC.fieldOf("site").forGetter(r -> r.site),
            RaidTimings.CODEC.fieldOf("timings").forGetter(r -> r.timings),
            BountyData.CODEC.fieldOf("bounty").forGetter(r -> r.bounty),
            Phase.CODEC.fieldOf("phase").forGetter(r -> r.phase),
            Codec.LONG.fieldOf("phase_start").forGetter(r -> r.phaseStart),
            Outcome.CODEC.fieldOf("outcome").forGetter(r -> r.outcome),
            UUIDUtil.CODEC.optionalFieldOf("leader").forGetter(r -> Optional.ofNullable(r.leader)),
            UUIDUtil.CODEC.listOf().fieldOf("members").forGetter(r -> r.members),
            UUIDUtil.CODEC.listOf().fieldOf("horses").forGetter(r -> r.horses),
            UUIDUtil.CODEC.optionalFieldOf("safecracker").forGetter(r -> Optional.ofNullable(r.safecracker)),
            Codec.INT.fieldOf("vault_progress").forGetter(r -> r.vaultProgress),
            ItemStack.CODEC.listOf().fieldOf("loot").forGetter(r -> r.loot),
            Codec.unboundedMap(UUIDUtil.STRING_CODEC, Contribution.CODEC).fieldOf("contributions").forGetter(r -> r.contributions),
            Tally.CODEC.fieldOf("tally").forGetter(r -> r.tally),
            Codec.unboundedMap(UUIDUtil.STRING_CODEC, Codec.INT).optionalFieldOf("box_take", Map.of()).forGetter(r -> r.boxTake)).apply(i, TownRaid::new));

    // Nobody about for this long and the raid is called off (the gang is removed).
    static final int ABANDON_TICKS = 600;
    // Players this far outside the town still see the bar and hear the bell.
    static final double WATCH_RANGE = 32.0;
    // A safecracker this close (horizontally) to his spot is working.
    private static final double WORK_REACH = 1.8;
    // How long after the safecracker falls someone else takes over (once).
    private static final int REPLACE_DELAY = 200;
    // Riders get off at the end of the street, when shot at, or after this long in the saddle.
    private static final int MAX_RIDE = 400;
    private static final double DISMOUNT_REACH = 5.0;
    private static final double ESCAPE_REACH = 6.0;

    final UUID id;
    final TownSite site;
    final RaidTimings timings;
    final BountyData bounty;
    Phase phase;
    long phaseStart;
    Outcome outcome;
    @Nullable UUID leader;
    final List<UUID> members;
    final List<UUID> horses;
    @Nullable UUID safecracker;
    int vaultProgress;
    final List<ItemStack> loot;
    final Map<UUID, Contribution> contributions;
    final Tally tally;
    // What the gang took from each safe deposit box when it cracked the vault (owner -> copper).
    final Map<UUID, Integer> boxTake;

    private @Nullable ServerBossEvent bar;
    private long lastWatched = Long.MIN_VALUE;

    private TownRaid(UUID id, TownSite site, RaidTimings timings, BountyData bounty, Phase phase, long phaseStart, Outcome outcome,
            Optional<UUID> leader, List<UUID> members, List<UUID> horses, Optional<UUID> safecracker, int vaultProgress, List<ItemStack> loot,
            Map<UUID, Contribution> contributions, Tally tally, Map<UUID, Integer> boxTake) {
        this.id = id;
        this.site = site;
        this.timings = timings;
        this.bounty = bounty;
        this.phase = phase;
        this.phaseStart = phaseStart;
        this.outcome = outcome;
        this.leader = leader.orElse(null);
        this.members = new ArrayList<>(members);
        this.horses = new ArrayList<>(horses);
        this.safecracker = safecracker.orElse(null);
        this.vaultProgress = vaultProgress;
        this.loot = new ArrayList<>(loot);
        this.contributions = new HashMap<>(contributions);
        this.tally = tally;
        this.boxTake = new HashMap<>(boxTake);
    }

    /** A fresh raid in its warning phase. The leader's name and hideout (if he escapes) are picked now. */
    static TownRaid create(ServerLevel level, TownSite site, RaidTimings timings, int gangSize, boolean mounted) {
        RandomSource random = level.getRandom();
        float angle = random.nextFloat() * Mth.TWO_PI;
        int distance = 200 + random.nextInt(300);
        BountyData bounty = new BountyData(UUID.randomUUID(), OutlawNames.random(random),
                site.rally().getX() + Math.round(Mth.cos(angle) * distance), site.rally().getZ() + Math.round(Mth.sin(angle) * distance),
                4 + random.nextInt(4), false);
        return new TownRaid(UUID.randomUUID(), site, timings, bounty, Phase.WARNING, level.getGameTime(), Outcome.NONE, Optional.empty(),
                List.of(), List.of(), Optional.empty(), 0, List.of(), Map.of(), new Tally(gangSize, mounted, 0, 0, 0, false, -1, false, false), Map.of());
    }

    /** Gang size for a town: bigger streets and harder difficulties draw bigger gangs (leader included). */
    public static int gangSizeFor(ServerLevel level, TownSite site) {
        Difficulty difficulty = level.getDifficulty();
        int size = RaidConfig.minGang() + (site.size() >= 11 ? 1 : 0) + level.getRandom().nextInt(2)
                + (difficulty == Difficulty.HARD ? 2 : difficulty == Difficulty.NORMAL ? 1 : 0);
        return Mth.clamp(size, RaidConfig.minGang(), RaidConfig.maxGang());
    }

    // ---------------------------------------------------------------- queries

    public UUID id() { return id; }
    public TownSite site() { return site; }
    public RaidTimings timings() { return timings; }
    public Phase phase() { return phase; }
    public Outcome outcome() { return outcome; }
    public BountyData bounty() { return bounty; }
    public String leaderName() { return bounty.target(); }
    public @Nullable UUID leaderId() { return leader; }
    public @Nullable UUID safecrackerId() { return safecracker; }
    public List<UUID> members() { return List.copyOf(members); }
    public int vaultProgress() { return vaultProgress; }
    public boolean cracked() { return tally.cracked; }
    public List<ItemStack> loot() { return List.copyOf(loot); }
    public Map<UUID, Integer> boxTake() { return Map.copyOf(boxTake); }
    public int gangSize() { return tally.gangSize; }
    public Contribution contribution(UUID player) { return contributions.getOrDefault(player, Contribution.NONE); }

    /** The gang's name, from the leader's surname: "the McGraw Gang". */
    public Component gangName() {
        String name = bounty.target();
        return Component.translatable("raid.hollowfrontier.gang", name.substring(name.lastIndexOf(' ') + 1));
    }

    /** Area the raid is felt in: the town plus WATCH_RANGE around it. */
    public AABB watchArea() {
        return AABB.of(site.bounds()).inflate(WATCH_RANGE, 64.0, WATCH_RANGE);
    }

    public List<ServerPlayer> watchers(ServerLevel level) {
        AABB area = watchArea();
        return level.players().stream().filter(p -> area.contains(p.position())).toList();
    }

    /** Where this member holds the street: lawmen face the gang across it from the far side of the middle. */
    BlockPos post(Entity entity, boolean lawman) {
        Vec3 rally = Vec3.atBottomCenterOf(site.rally());
        Vec3 toEdge = Vec3.atBottomCenterOf(site.edge()).subtract(rally).normalize();
        Vec3 across = new Vec3(-toEdge.z, 0, toEdge.x);
        int slot = Math.floorMod(entity.getUUID().hashCode(), 5) - 2;
        Vec3 spot = lawman ? rally.subtract(toEdge.scale(6.0)) : rally.add(toEdge.scale(RaidMember.is(entity, Role.LEADER) ? 8.0 : 2.0));
        return BlockPos.containing(spot.add(across.scale(slot * 1.5)));
    }

    // ---------------------------------------------------------------- ticking

    /** One tick. Returns true when the raid is over (TownRaids then calls finish). */
    boolean tick(ServerLevel level, TownRaids data) {
        long now = level.getGameTime();
        long age = now - phaseStart;
        List<ServerPlayer> watchers = watchers(level);
        if (lastWatched == Long.MIN_VALUE || !watchers.isEmpty()) {
            lastWatched = now;
        }
        if (now - lastWatched > ABANDON_TICKS || !level.isLoaded(site.rally())) {
            return true; // TownRaids sees outcome NONE and calls the whole thing off
        }
        if (now % 10 == 0) {
            updateBar(level, watchers, age);
        }
        switch (phase) {
            case WARNING -> {
                if (age % 30 == 0) {
                    ringAlarm(level);
                }
                if (age % 20 == 10) {
                    // Hoofbeats from out past the edge of town, coming closer.
                    Vec3 from = Vec3.atCenterOf(site.approach());
                    Vec3 to = Vec3.atCenterOf(site.edge());
                    Vec3 at = from.lerp(to, 0.6 * age / Math.max(1, timings.warning()));
                    level.playSound(null, at.x, at.y + 2, at.z, ModSounds.RAID_HOOVES.get(), SoundSource.HOSTILE, 4.0F, 0.9F + level.getRandom().nextFloat() * 0.2F);
                }
                if (level.getRandom().nextInt(60) == 0) {
                    BlockPos at = site.approach();
                    level.playSound(null, at.getX(), at.getY() + 3, at.getZ(), ModSounds.RAID_GUNFIRE.get(), SoundSource.HOSTILE, 5.0F, 0.9F + level.getRandom().nextFloat() * 0.2F);
                }
                if (age >= timings.warning()) {
                    rideIn(level, data, null);
                }
            }
            case ASSAULT -> tickAssault(level, data, now, age);
            case RETREAT -> {
                return tickRetreat(level, now, age);
            }
        }
        return false;
    }

    private void ringAlarm(ServerLevel level) {
        if (site.bell().isPresent() && level.isLoaded(site.bell().get())) {
            BlockPos pos = site.bell().get();
            BlockState state = level.getBlockState(pos);
            if (state.getBlock() instanceof BellBlock bell && bell.attemptToRing(level, pos, null)) {
                return;
            }
        }
        // No chapel (a water tower town): somebody beats the alarm triangle in the street.
        level.playSound(null, site.rally(), ModSounds.RAID_ALARM.get(), SoundSource.BLOCKS, 4.0F, 1.0F);
    }

    /**
     * The gang rides in: spawned at the site's approach (or at spawnAt, for tests and the photo booth), mounted if the
     * raid is, then everyone in town gets the news. The town's lawmen are counted now, to see how many make it.
     */
    public void rideIn(ServerLevel level, TownRaids data, @Nullable BlockPos spawnAt) {
        BlockPos gather = site.approach();
        // A big town can put the gathering ground out where nothing is loaded: come in toward the square until it is.
        Vec3 inward = Vec3.atCenterOf(site.rally()).subtract(Vec3.atCenterOf(gather)).normalize().scale(8.0);
        for (int i = 0; i < 16 && !level.isLoaded(gather); i++) {
            gather = BlockPos.containing(Vec3.atCenterOf(gather).add(inward));
        }
        BlockPos ground = spawnAt != null ? spawnAt : level.getHeightmapPos(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, gather);
        spawnGang(level, ground);
        phase = Phase.ASSAULT;
        phaseStart = level.getGameTime();
        tally.lawmenAtStart = lawmenStanding(level);
        Vec3 dir = Vec3.atCenterOf(site.approach()).subtract(Vec3.atCenterOf(site.rally()));
        Direction from = Direction.getApproximateNearest(dir.x, 0, dir.z);
        for (ServerPlayer player : watchers(level)) {
            player.sendSystemMessage(Component.translatable("message.hollowfrontier.raid.ride_in", gangName(),
                    Component.translatable("raid.hollowfrontier.from." + from.getName())).withStyle(ChatFormatting.RED));
        }
        say(level, "arrive");
        data.setDirty();
    }

    void announceWarning(ServerLevel level) {
        for (ServerPlayer player : watchers(level)) {
            title(player, Component.translatable("title.hollowfrontier.raid.warning").withStyle(ChatFormatting.DARK_RED, ChatFormatting.BOLD),
                    Component.translatable("title.hollowfrontier.raid.warning.sub", gangName()));
            int stars = Wanted.stars(player);
            if (stars >= 3) {
                player.sendSystemMessage(Component.translatable("message.hollowfrontier.raid.partner").withStyle(ChatFormatting.GRAY));
            } else if (stars > 0) {
                player.sendSystemMessage(Component.translatable("message.hollowfrontier.raid.truce").withStyle(ChatFormatting.GOLD));
            }
        }
    }

    private void spawnGang(ServerLevel level, BlockPos ground) {
        List<Role> roles = new ArrayList<>();
        roles.add(Role.LEADER);
        if (site.hasVault()) {
            roles.add(Role.SAFECRACKER);
        }
        roles.add(Role.DYNAMITER);
        if (tally.planned >= 7) {
            roles.add(Role.DYNAMITER);
        }
        while (roles.size() < tally.planned) {
            roles.add(Role.GUNSLINGER);
        }
        RandomSource random = level.getRandom();
        Vec3 toTown = Vec3.atCenterOf(site.rally()).subtract(Vec3.atCenterOf(ground)).normalize();
        float yaw = (float) (Mth.atan2(toTown.z, toTown.x) * Mth.RAD_TO_DEG) - 90.0F;
        for (int i = 0; i < roles.size(); i++) {
            // A loose bunch, leader at the back.
            double angle = i * 2.4;
            double spread = i == 0 ? 0.0 : 1.5 + i * 0.5;
            BlockPos column = BlockPos.containing(ground.getX() + 0.5 + Math.cos(angle) * spread - toTown.x * (i == 0 ? 3 : 0), ground.getY(),
                    ground.getZ() + 0.5 + Math.sin(angle) * spread - toTown.z * (i == 0 ? 3 : 0));
            BlockPos at = level.isLoaded(column) && Math.abs(level.getHeightmapPos(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, column).getY() - ground.getY()) < 4
                    ? level.getHeightmapPos(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, column) : ground;
            spawnMember(level, roles.get(i), i, at, yaw, random);
        }
    }

    private void spawnMember(ServerLevel level, Role role, int index, BlockPos at, float yaw, RandomSource random) {
        EntityType<? extends AbstractOutlaw> type = switch (role) {
            case LEADER -> ModEntities.OUTLAW_BOSS.get();
            case GUNSLINGER -> index % 2 == 0 ? ModEntities.OUTLAW_GUNSLINGER.get() : ModEntities.OUTLAW.get();
            default -> ModEntities.OUTLAW.get();
        };
        AbstractOutlaw outlaw = type.create(level, EntitySpawnReason.EVENT);
        if (outlaw == null) {
            return;
        }
        outlaw.snapTo(at.getX() + 0.5, at.getY(), at.getZ() + 0.5, yaw, 0.0F);
        outlaw.finalizeSpawn(level, level.getCurrentDifficultyAt(at), EntitySpawnReason.EVENT, null);
        outlaw.setPersistenceRequired();
        if (outlaw instanceof OutlawBoss boss) {
            boss.setBounty(bounty);
        }
        dress(outlaw, role);
        Horse horse = tally.mounted ? EntityTypes.HORSE.create(level, EntitySpawnReason.EVENT) : null;
        Optional<UUID> mount = Optional.empty();
        if (horse != null) {
            horse.snapTo(at.getX() + 0.5, at.getY(), at.getZ() + 0.5, yaw, 0.0F);
            horse.finalizeSpawn(level, level.getCurrentDifficultyAt(at), EntitySpawnReason.EVENT, null);
            horse.setTamed(true);
            horse.setItemSlot(EquipmentSlot.SADDLE, new ItemStack(Items.SADDLE));
            horse.setData(ModAttachments.RAID_MEMBER.get(), new RaidMember(id, Role.HORSE, Optional.empty()));
            mount = Optional.of(horse.getUUID());
        }
        outlaw.setData(ModAttachments.RAID_MEMBER.get(), new RaidMember(id, role, mount));
        if (horse != null) {
            outlaw.startRiding(horse, true, false);
            level.addFreshEntityWithPassengers(horse);
            horses.add(horse.getUUID());
        } else {
            level.addFreshEntity(outlaw);
        }
        members.add(outlaw.getUUID());
        tally.gangSize++;
        if (role == Role.LEADER) {
            leader = outlaw.getUUID();
        } else if (role == Role.SAFECRACKER) {
            safecracker = outlaw.getUUID();
        }
    }

    /** What each job carries: the safecracker a pick for the vault, the dynamiter his sticks (thrown, not fired). */
    static void dress(AbstractOutlaw outlaw, Role role) {
        switch (role) {
            case SAFECRACKER -> {
                outlaw.setItemSlot(EquipmentSlot.MAINHAND, new ItemStack(Items.IRON_PICKAXE));
                outlaw.setCustomName(Component.translatable("entity.hollowfrontier.outlaw.safecracker"));
                outlaw.setCustomNameVisible(true);
            }
            case DYNAMITER -> {
                outlaw.setItemSlot(EquipmentSlot.MAINHAND, new ItemStack(ModItems.DYNAMITE.get()));
                outlaw.setCustomName(Component.translatable("entity.hollowfrontier.outlaw.dynamiter"));
            }
            default -> {}
        }
        outlaw.setDropChance(EquipmentSlot.MAINHAND, 0.0F);
    }

    private void tickAssault(ServerLevel level, TownRaids data, long now, long age) {
        if (now % 5 == 0) {
            steerRiders(level, site.edge(), age, true);
        }
        if (age % 100 == 0) {
            ringAlarm(level);
        }
        // The safecracker at work.
        AbstractOutlaw cracker = safecracker == null ? null : level.getEntity(safecracker) instanceof AbstractOutlaw o && o.isAlive() ? o : null;
        if (cracker != null && site.workAt().isPresent() && atWork(cracker)) {
            vaultProgress++;
            BlockPos crack = site.crackAt().orElseThrow();
            if (vaultProgress % 10 == 0) {
                Vec3 face = Vec3.atCenterOf(crack).lerp(Vec3.atCenterOf(site.workAt().get()), 0.5);
                level.sendParticles(ParticleTypes.ELECTRIC_SPARK, face.x, face.y, face.z, 4, 0.2, 0.3, 0.2, 0.05);
                level.sendParticles(ParticleTypes.SMOKE, face.x, face.y + 0.3, face.z, 2, 0.1, 0.1, 0.1, 0.01);
                level.playSound(null, crack, ModSounds.VAULT_WORK.get(), SoundSource.HOSTILE, 1.2F, 0.8F + level.getRandom().nextFloat() * 0.4F);
                cracker.swing(net.minecraft.world.InteractionHand.MAIN_HAND, SwingAnimation.DEFAULT, false);
            }
            level.destroyBlockProgress(crackerId(), crack, Math.min(9, vaultProgress * 10 / Math.max(1, timings.vault())));
            if (vaultProgress >= timings.vault()) {
                crack(level, data, cracker);
                return;
            }
        }
        // Someone takes up the dead safecracker's tools (once).
        if (safecracker == null && site.hasVault() && !tally.replaced && tally.safecrackerLost >= 0 && now - tally.safecrackerLost >= REPLACE_DELAY) {
            tally.replaced = true;
            for (UUID uuid : members) {
                if (level.getEntity(uuid) instanceof AbstractOutlaw outlaw && outlaw.isAlive() && RaidMember.is(outlaw, Role.GUNSLINGER)) {
                    outlaw.setData(ModAttachments.RAID_MEMBER.get(), RaidMember.of(outlaw).withRole(Role.SAFECRACKER));
                    dress(outlaw, Role.SAFECRACKER);
                    outlaw.setTarget(null);
                    safecracker = uuid;
                    say(level, "replace");
                    break;
                }
            }
        }
        int men = tally.gangSize - 1;
        if (men > 0 && tally.lost >= Math.ceil(RaidConfig.moraleBreak() * men)) {
            say(level, "break");
            decide(level, data, Outcome.DEFENDED);
        } else if (age >= timings.timeout()) {
            say(level, "give_up");
            decide(level, data, Outcome.GAVE_UP);
        }
        data.setDirty();
    }

    public boolean atWork(AbstractOutlaw cracker) {
        Vec3 spot = Vec3.atBottomCenterOf(site.workAt().orElseThrow());
        return !cracker.isPassenger() && cracker.position().subtract(spot).horizontalDistanceSqr() <= WORK_REACH * WORK_REACH
                && Math.abs(cracker.getY() - spot.y) < 1.5;
    }

    private int crackerId() {
        return id.hashCode();
    }

    // Drives the gang's horses (mobs don't steer them themselves) and lets riders off near the target or when shot at.
    private void steerRiders(ServerLevel level, BlockPos target, long age, boolean dismountAtEnd) {
        for (UUID uuid : members) {
            if (!(level.getEntity(uuid) instanceof AbstractOutlaw rider) || !(rider.getVehicle() instanceof AbstractHorse horse)) {
                continue;
            }
            if (dismountAtEnd && (horse.distanceToSqr(Vec3.atBottomCenterOf(target)) < DISMOUNT_REACH * DISMOUNT_REACH
                    || rider.hurtTime > 0 || horse.hurtTime > 0 || age > MAX_RIDE)) {
                rider.stopRiding();
                horse.getNavigation().stop();
                horse.setHomeTo(horse.blockPosition(), 4); // hitched where he left it
                continue;
            }
            if (age % 20 == 0 || horse.getNavigation().isDone()) {
                Vec3 spot = Vec3.atBottomCenterOf(post(rider, false)).lerp(Vec3.atBottomCenterOf(target), dismountAtEnd ? 0.85 : 1.0);
                horse.getNavigation().moveTo(spot.x, spot.y, spot.z, 2.2);
            }
        }
    }

    /** The vault gives: its door swings open, the gang takes its share of every stack, and the safecracker carries it out. */
    void crack(ServerLevel level, TownRaids data, AbstractOutlaw cracker) {
        tally.cracked = true;
        level.destroyBlockProgress(crackerId(), site.crackAt().orElseThrow(), -1);
        BlockPos crack = site.crackAt().orElseThrow();
        BlockState state = level.getBlockState(crack);
        if (state.getBlock() instanceof DoorBlock door && !door.isOpen(state)) {
            door.setOpen(cracker, level, state, crack, true);
        }
        level.sendParticles(ParticleTypes.EXPLOSION, crack.getX() + 0.5, crack.getY() + 1.0, crack.getZ() + 0.5, 1, 0, 0, 0, 0);
        level.sendParticles(ParticleTypes.LARGE_SMOKE, crack.getX() + 0.5, crack.getY() + 1.0, crack.getZ() + 0.5, 12, 0.4, 0.6, 0.4, 0.02);
        level.playSound(null, crack, ModSounds.VAULT_BLOWN.get(), SoundSource.HOSTILE, 3.0F, 1.0F);
        double share = RaidConfig.stolenShare();
        for (BlockPos pos : site.vault()) {
            if (level.getBlockEntity(pos) instanceof ChestBlockEntity chest) {
                chest.unpackLootTable(null);
                for (int slot = 0; slot < chest.getContainerSize(); slot++) {
                    ItemStack stack = chest.getItem(slot);
                    int take = (int) Math.ceil(stack.getCount() * share);
                    if (!stack.isEmpty() && take > 0) {
                        loot.add(stack.split(take));
                    }
                }
                chest.setChanged();
            }
        }
        // ...and a share of every safe deposit box in the town's bank, whoever's they are.
        boxTake.putAll(TownBanks.get(level).rob(site.key(), RaidConfig.depositsAtRisk()));
        cracker.setItemSlot(EquipmentSlot.MAINHAND, new ItemStack(Items.BUNDLE)); // the loot sack
        cracker.setCustomName(Component.translatable("entity.hollowfrontier.outlaw.bagman"));
        say(level, "cracked");
        decide(level, data, Outcome.ROBBED);
    }

    /** The fight is over one way or another: pay out (or not), tell the town, and the gang makes for the hills. */
    void decide(ServerLevel level, TownRaids data, Outcome result) {
        outcome = result;
        phase = Phase.RETREAT;
        phaseStart = level.getGameTime();
        safecrackerDone(level);
        List<ServerPlayer> watchers = watchers(level);
        String key = switch (result) {
            case ROBBED -> "robbed";
            case RECOVERED -> "recovered";
            case GAVE_UP -> "gave_up";
            default -> tally.leaderKilled ? "leader_down" : "broke";
        };
        for (ServerPlayer player : watchers) {
            title(player, Component.translatable("title.hollowfrontier.raid." + key).withStyle(result == Outcome.ROBBED ? ChatFormatting.RED : ChatFormatting.GOLD),
                    Component.translatable("title.hollowfrontier.raid." + key + ".sub", gangName(), bounty.target()));
        }
        level.playSound(null, site.rally(), result == Outcome.ROBBED ? ModSounds.RAID_LOST.get() : ModSounds.RAID_WON.get(), SoundSource.AMBIENT, 4.0F, 1.0F);
        if (result == Outcome.ROBBED) {
            payOutlaws(level, watchers);
        } else {
            payDefenders(level);
        }
        data.record(site.key(), level.getGameTime()).nextRaid = level.getGameTime() + RaidConfig.cooldown();
        data.setDirty();
    }

    private void safecrackerDone(ServerLevel level) {
        if (site.crackAt().isPresent()) {
            level.destroyBlockProgress(crackerId(), site.crackAt().get(), -1);
        }
    }

    // Every player who hurt the gang gets paid, by the sheriff if he's standing, and the law forgets a good deal.
    private void payDefenders(ServerLevel level) {
        int saved = Math.min(tally.lawmenAtStart, lawmenStanding(level));
        Lawman sheriff = level.getEntitiesOfClass(Lawman.class, AABB.of(site.bounds()), l -> l.isAlive() && !l.isPosse())
                .stream().min((a, b) -> Integer.compare(a.getRank().ordinal(), b.getRank().ordinal())).orElse(null);
        Component payer = sheriff != null ? sheriff.getDisplayName().copy().withStyle(ChatFormatting.GOLD) : Component.translatable("raid.hollowfrontier.townsfolk");
        for (Map.Entry<UUID, Contribution> entry : contributions.entrySet()) {
            Contribution c = entry.getValue();
            ServerPlayer player = level.getServer().getPlayerList().getPlayer(entry.getKey());
            if (player == null || !c.any()) {
                continue;
            }
            int copper = RaidConfig.payTakingPart() + c.kills() * RaidConfig.payKill() + (c.leader() ? RaidConfig.payLeader() : 0)
                    + (c.safecracker() ? RaidConfig.paySafecracker() : 0) + saved * RaidConfig.payLawmanSaved();
            payCoins(player, copper);
            ModCriteria.fire(player, ModCriteria.RAID_DEFENDED, c.kills());
            player.sendSystemMessage(Component.translatable("message.hollowfrontier.npc.say", payer,
                    Component.translatable("message.hollowfrontier.raid.reward", copper / 9, copper % 9)));
            if (Wanted.of(player).heat() > 0 && RaidConfig.heatReduction() > 0) {
                Wanted.addHeat(player, -RaidConfig.heatReduction());
                player.sendSystemMessage(Component.translatable("message.hollowfrontier.raid.pardon").withStyle(ChatFormatting.GREEN));
            }
        }
    }

    // A three-star outlaw in town who never fired on the gang: one of their own. He gets a cut, and the law hears of it.
    private void payOutlaws(ServerLevel level, List<ServerPlayer> watchers) {
        for (ServerPlayer player : watchers) {
            if (player.isCreative() || player.isSpectator() || Wanted.stars(player) < 3 || contribution(player.getUUID()).any()) {
                continue;
            }
            payCoins(player, RaidConfig.outlawCut());
            Wanted.addHeat(player, RaidConfig.outlawCutHeat());
            ModCriteria.fire(player, ModCriteria.RAID_CUT);
            player.sendSystemMessage(Component.translatable("message.hollowfrontier.npc.say", leaderLabel(),
                    Component.translatable("message.hollowfrontier.raid.leader.cut")));
        }
    }

    static void payCoins(ServerPlayer player, int copper) {
        give(player, new ItemStack(ModItems.GOLD_COIN.get(), copper / 9));
        give(player, new ItemStack(ModItems.COPPER_COIN.get(), copper % 9));
        player.level().playSound(null, player.blockPosition(), ModSounds.COIN_CLINK.get(), SoundSource.PLAYERS, 1.0F, 1.0F);
    }

    private static void give(ServerPlayer player, ItemStack stack) {
        if (!stack.isEmpty() && !player.getInventory().add(stack)) {
            player.drop(stack, false, Prediction.SERVER_ONLY);
        }
    }

    private int lawmenStanding(ServerLevel level) {
        return level.getEntitiesOfClass(Lawman.class, AABB.of(site.bounds()), l -> l.isAlive() && !l.isPosse()).size();
    }

    private boolean tickRetreat(ServerLevel level, long now, long age) {
        if (now % 5 == 0) {
            steerRiders(level, site.approach(), age, false);
        }
        // Whoever reaches open ground past the edge of town is gone.
        Vec3 out = Vec3.atBottomCenterOf(site.approach());
        for (UUID uuid : List.copyOf(members)) {
            if (level.getEntity(uuid) instanceof AbstractOutlaw outlaw && outlaw.position().subtract(out).horizontalDistanceSqr() < ESCAPE_REACH * ESCAPE_REACH) {
                escape(outlaw);
            }
        }
        return age >= timings.retreat() || members.isEmpty();
    }

    private void escape(AbstractOutlaw outlaw) {
        members.remove(outlaw.getUUID());
        if (outlaw.getVehicle() instanceof AbstractHorse horse) {
            horses.remove(horse.getUUID());
            horse.discard();
        }
        outlaw.discard();
    }

    // ---------------------------------------------------------------- the end

    /** Over: stragglers ride off, riderless horses are left for the taking, and the town remembers how it went. */
    void finish(ServerLevel level, TownRaids data) {
        long now = level.getGameTime();
        for (UUID uuid : List.copyOf(members)) {
            if (level.getEntity(uuid) instanceof AbstractOutlaw outlaw) {
                escape(outlaw);
            }
        }
        // Horses whose riders fell stay behind: ordinary saddled horses now (tame them to keep them).
        for (UUID uuid : horses) {
            if (level.getEntity(uuid) instanceof AbstractHorse horse && horse.isAlive()) {
                horse.removeData(ModAttachments.RAID_MEMBER.get());
                horse.setTamed(false);
                horse.clearHome();
            }
        }
        horses.clear();
        TownRecord record = data.record(site.key(), now);
        boolean escaped = !tally.leaderKilled;
        if (outcome == Outcome.ROBBED) {
            record.robbedUntil = now + RaidConfig.robbedTime();
        }
        if (escaped) {
            record.poster = Optional.of(bounty);
        }
        String story = switch (outcome) {
            case ROBBED -> "robbed";
            case RECOVERED -> "recovered";
            case GAVE_UP -> "gave_up";
            default -> contributions.values().stream().anyMatch(Contribution::any) ? "defended" : "held";
        };
        int bankLost = outcome == Outcome.ROBBED ? boxTake.values().stream().mapToInt(Integer::intValue).sum() : 0;
        String town = com.hollowfrontier.worldgen.structure.TownNames.name(level, site.key());
        data.lastReport = Optional.of(new RaidReport(story, bounty.target(), site.rally().getX(), site.rally().getZ(), now, bankLost, town));
        if (!boxTake.isEmpty()) {
            String gang = bounty.target().substring(bounty.target().lastIndexOf(' ') + 1);
            Banking.notify(level.getServer(), boxTake, gang, town, site.rally().getX(), site.rally().getZ(), outcome == Outcome.RECOVERED);
        }
        for (ServerPlayer player : watchers(level)) {
            player.sendSystemMessage(Component.translatable("message.hollowfrontier.raid.over." + story, gangName(), bounty.target())
                    .withStyle(outcome == Outcome.ROBBED ? ChatFormatting.RED : ChatFormatting.GOLD));
            if (escaped) {
                player.sendSystemMessage(Component.translatable("message.hollowfrontier.raid.poster", bounty.target()).withStyle(ChatFormatting.YELLOW));
            }
        }
        removeBar();
        safecrackerDone(level);
        data.setDirty();
    }

    /** Called off (nobody about, the town unloaded, or a command): the gang is just gone, nothing is decided. */
    void abandon(ServerLevel level, TownRaids data) {
        for (UUID uuid : List.copyOf(members)) {
            if (level.getEntity(uuid) instanceof AbstractOutlaw outlaw) {
                escape(outlaw);
            }
        }
        for (UUID uuid : horses) {
            Entity horse = level.getEntity(uuid);
            if (horse != null) {
                horse.discard();
            }
        }
        horses.clear();
        // Called off with the bagman still in town: the boxes get their coins back (the vault's loot is just gone, as ever).
        if (outcome == Outcome.ROBBED && !boxTake.isEmpty()) {
            TownBanks.get(level).refund(site.key(), boxTake);
            boxTake.clear();
        }
        removeBar();
        safecrackerDone(level);
        TownRecord record = data.record(site.key(), level.getGameTime());
        record.nextRaid = Math.max(record.nextRaid, level.getGameTime() + RaidConfig.cooldown() / 4);
        data.setDirty();
    }

    // ---------------------------------------------------------------- deaths and damage

    void onMemberHurt(ServerPlayer player, float amount) {
        Contribution c = contribution(player.getUUID());
        contributions.put(player.getUUID(), new Contribution(c.kills(), c.leader(), c.safecracker(), c.damage() + amount));
    }

    void onMemberDeath(ServerLevel level, TownRaids data, LivingEntity entity, RaidMember member, DamageSource source) {
        if (member.role() == Role.HORSE) {
            horses.remove(entity.getUUID());
            return;
        }
        if (!members.remove(entity.getUUID())) {
            return;
        }
        boolean wasCracker = entity.getUUID().equals(safecracker);
        if (source.getEntity() instanceof ServerPlayer player) {
            Contribution c = contribution(player.getUUID());
            contributions.put(player.getUUID(), new Contribution(c.kills() + 1, c.leader() || member.role() == Role.LEADER,
                    c.safecracker() || wasCracker, c.damage()));
        }
        if (member.role() == Role.LEADER) {
            leader = null;
            tally.leaderKilled = true;
            for (ServerPlayer player : watchers(level)) {
                player.sendSystemMessage(Component.translatable("message.hollowfrontier.raid.leader_down", bounty.target()).withStyle(ChatFormatting.GOLD));
            }
            if (phase != Phase.RETREAT) {
                decide(level, data, Outcome.DEFENDED);
            }
        } else {
            tally.lost++;
        }
        if (wasCracker) {
            safecracker = null;
            tally.safecrackerLost = level.getGameTime();
            boolean working = vaultProgress > 0;
            vaultProgress = 0;
            safecrackerDone(level);
            if (phase == Phase.RETREAT && outcome == Outcome.ROBBED) {
                returnLoot(level);
                ModCriteria.fire(source.getEntity(), ModCriteria.LOOT_RECOVERED);
                if (!boxTake.isEmpty()) {
                    TownBanks.get(level).refund(site.key(), boxTake);
                    if (source.getEntity() != null && boxTake.containsKey(source.getEntity().getUUID())) {
                        ModCriteria.fire(source.getEntity(), ModCriteria.BANK_SAVED);
                    }
                }
                decide(level, data, Outcome.RECOVERED);
            } else if (working && phase == Phase.ASSAULT) {
                for (ServerPlayer player : watchers(level)) {
                    player.sendOverlayMessage(Component.translatable("message.hollowfrontier.raid.safecracker_down").withStyle(ChatFormatting.GOLD));
                }
            }
        }
        data.setDirty();
    }

    // The bagman is down: what he carried goes back where it came from (whatever won't fit drops by the vault).
    private void returnLoot(ServerLevel level) {
        for (ItemStack stack : loot) {
            for (BlockPos pos : site.vault()) {
                if (!stack.isEmpty() && level.getBlockEntity(pos) instanceof ChestBlockEntity chest) {
                    for (int slot = 0; slot < chest.getContainerSize() && !stack.isEmpty(); slot++) {
                        ItemStack in = chest.getItem(slot);
                        if (in.isEmpty()) {
                            chest.setItem(slot, stack.copyAndClear());
                        } else if (ItemStack.isSameItemSameComponents(in, stack) && in.getCount() < in.getMaxStackSize()) {
                            int move = Math.min(stack.getCount(), in.getMaxStackSize() - in.getCount());
                            in.grow(move);
                            stack.shrink(move);
                        }
                    }
                    chest.setChanged();
                }
            }
            if (!stack.isEmpty() && site.workAt().isPresent()) {
                BlockPos at = site.workAt().get();
                net.minecraft.world.Containers.dropItemStack(level, at.getX() + 0.5, at.getY() + 0.5, at.getZ() + 0.5, stack);
            }
        }
        loot.clear();
    }

    // ---------------------------------------------------------------- talk, titles, bar

    private Component leaderLabel() {
        return Component.literal(bounty.target()).withStyle(ChatFormatting.DARK_RED);
    }

    /** The leader shouts (message.hollowfrontier.raid.leader.KEY) to everyone around, if he's still alive. */
    void say(ServerLevel level, String key) {
        if (leader == null) {
            return;
        }
        Component line = Component.translatable("message.hollowfrontier.npc.say", leaderLabel(),
                Component.translatable("message.hollowfrontier.raid.leader." + key));
        for (ServerPlayer player : watchers(level)) {
            player.sendSystemMessage(line);
        }
    }

    private static void title(ServerPlayer player, Component title, Component subtitle) {
        player.connection.send(new ClientboundSetTitlesAnimationPacket(10, 70, 20));
        player.connection.send(new ClientboundSetSubtitleTextPacket(subtitle));
        player.connection.send(new ClientboundSetTitleTextPacket(title));
    }

    private void updateBar(ServerLevel level, List<ServerPlayer> watchers, long age) {
        if (bar == null) {
            bar = new ServerBossEvent(id, gangName(), BossEvent.BossBarColor.RED, BossEvent.BossBarOverlay.NOTCHED_10);
            bar.setDarkenScreen(false);
        }
        int alive = members.size();
        switch (phase) {
            case WARNING -> {
                bar.setName(Component.translatable("bossbar.hollowfrontier.raid.warning", gangName()));
                bar.setColor(BossEvent.BossBarColor.RED);
                bar.setProgress(Mth.clamp(1.0F - (float) age / Math.max(1, timings.warning()), 0.0F, 1.0F));
            }
            case ASSAULT -> {
                if (vaultProgress > 0) {
                    bar.setName(Component.translatable("bossbar.hollowfrontier.raid.vault", gangName()));
                    bar.setColor(BossEvent.BossBarColor.YELLOW);
                    bar.setProgress(Mth.clamp((float) vaultProgress / Math.max(1, timings.vault()), 0.0F, 1.0F));
                } else {
                    bar.setName(Component.translatable("bossbar.hollowfrontier.raid.gang", gangName(), alive));
                    bar.setColor(BossEvent.BossBarColor.RED);
                    bar.setProgress(Mth.clamp((float) alive / Math.max(1, tally.gangSize), 0.0F, 1.0F));
                }
            }
            case RETREAT -> {
                bar.setName(Component.translatable(outcome == Outcome.ROBBED ? "bossbar.hollowfrontier.raid.getaway" : "bossbar.hollowfrontier.raid.retreat", gangName()));
                bar.setColor(outcome == Outcome.ROBBED ? BossEvent.BossBarColor.YELLOW : BossEvent.BossBarColor.WHITE);
                bar.setProgress(Mth.clamp(1.0F - (float) age / Math.max(1, timings.retreat()), 0.0F, 1.0F));
            }
        }
        for (ServerPlayer player : List.copyOf(bar.getPlayers())) {
            if (!watchers.contains(player)) {
                bar.removePlayer(player);
            }
        }
        for (ServerPlayer player : watchers) {
            bar.addPlayer(player);
        }
    }

    private void removeBar() {
        if (bar != null) {
            bar.removeAllPlayers();
            bar = null;
        }
    }
}
