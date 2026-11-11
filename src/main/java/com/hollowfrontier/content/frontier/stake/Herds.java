package com.hollowfrontier.content.frontier.stake;

import java.util.EnumSet;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.jspecify.annotations.Nullable;

import com.hollowfrontier.content.frontier.town.Towns;
import com.hollowfrontier.registry.ModAttachments;
import com.hollowfrontier.registry.ModCriteria;
import com.hollowfrontier.registry.ModEntities;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import net.minecraft.core.BlockPos;
import net.minecraft.core.UUIDUtil;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.Leashable;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.entity.animal.cow.AbstractCow;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

/**
 * Cattle (47b, 47c): vanilla cows with something attached. A <b>brand</b> ({@link Brand}) says whose a cow is and where it
 * was branded; a <b>herd</b> mark ({@link Herd}) says it's on a drive. Herd cattle keep together and can be driven
 * ({@link DriveGoal}); gunfire stampedes them. Rustlers come for branded stock on a claim at night.
 */
public final class Herds {
    private Herds() {}

    /** A cow's brand: its owner, the town their place is at, and where the iron went on (a sale pays by how far it has come since). */
    public record Brand(UUID owner, String ownerName, long town, int x, int z) {
        public static final Codec<Brand> CODEC = RecordCodecBuilder.create(i -> i.group(
                UUIDUtil.CODEC.fieldOf("owner").forGetter(Brand::owner),
                Codec.STRING.fieldOf("owner_name").forGetter(Brand::ownerName),
                Codec.LONG.fieldOf("town").forGetter(Brand::town),
                Codec.INT.fieldOf("x").forGetter(Brand::x),
                Codec.INT.fieldOf("z").forGetter(Brand::z)).apply(i, Brand::new));
        public static final Brand NONE = new Brand(new UUID(0, 0), "", 0, 0, 0);
    }

    /** A cow on a drive: which drive, and who is driving it. */
    public record Herd(UUID drive, UUID drover) {
        public static final Codec<Herd> CODEC = RecordCodecBuilder.create(i -> i.group(
                UUIDUtil.CODEC.fieldOf("drive").forGetter(Herd::drive),
                UUIDUtil.CODEC.fieldOf("drover").forGetter(Herd::drover)).apply(i, Herd::new));
        public static final Herd NONE = new Herd(new UUID(0, 0), new UUID(0, 0));
    }

    /** A herd cow running: until when, and from where. Not saved: a stampede doesn't outlast the world being shut. */
    public record Stampede(long until, double x, double z) {
        public static final Stampede NONE = new Stampede(0, 0, 0);
    }

    public static final double RUSTLER_HERD_RANGE = 48.0;
    public static final int RUSTLE_AT = 3;
    /** How far past where he rode in a rustler has to lead a cow before it's lost. */
    public static final int RUSTLE_RUN = 40;
    public static final float RUSTLE_CHANCE = 0.06F;
    public static final int STAMPEDE_TICKS = 120;
    public static final double SPOOK_RANGE = 24.0;

    public static boolean isCattle(Entity entity) {
        return entity instanceof AbstractCow;
    }

    public static @Nullable Brand brandOf(Entity entity) {
        return entity.hasData(ModAttachments.BRAND.get()) ? entity.getData(ModAttachments.BRAND.get()) : null;
    }

    public static @Nullable Herd herdOf(Entity entity) {
        return entity.hasData(ModAttachments.HERD.get()) ? entity.getData(ModAttachments.HERD.get()) : null;
    }

    // ---------------------------------------------------------------- the brand

    /** A branding iron used on a cow: branded for the holder's place, if the iron is hot and they have a place. */
    public static boolean brand(ServerLevel level, ServerPlayer player, LivingEntity cow, ItemStack iron, InteractionHand hand) {
        if (!StakeItems.BrandingIron.hot(iron, level)) {
            player.sendOverlayMessage(Component.translatable(StakeText.IRON_COLD));
            return false;
        }
        Brand old = brandOf(cow);
        if (old != null) {
            player.sendOverlayMessage(Component.translatable(StakeText.BRAND_THEIRS, old.ownerName()));
            return false;
        }
        // Their place nearest the cow
        Optional<StakeData.Claim> place = StakeData.get(level).of(player.getUUID()).stream()
                .min((a, b) -> Double.compare(a.stake().distSqr(cow.blockPosition()), b.stake().distSqr(cow.blockPosition())));
        if (place.isEmpty()) {
            player.sendOverlayMessage(Component.translatable(StakeText.IRON_NO_PLACE));
            return false;
        }
        mark(cow, player, place.get().town());
        level.playSound(null, cow.blockPosition(), SoundEvents.FIRE_EXTINGUISH, SoundSource.NEUTRAL, 0.7F, 1.4F);
        level.sendParticles(ParticleTypes.SMOKE, cow.getX(), cow.getY(0.6), cow.getZ(), 8, 0.2, 0.2, 0.2, 0.01);
        iron.hurtAndBreak(1, player, hand);
        player.sendOverlayMessage(Component.translatable(StakeText.BRANDED, place.get().ownerName(), Towns.name(level, place.get().town())));
        ModCriteria.fire(player, ModCriteria.BRANDED);
        return true;
    }

    /** Puts {@code owner}'s brand on a cow where it stands (it takes the brand's name if it has none of its own). */
    public static void mark(LivingEntity cow, Player owner, long town) {
        String name = owner.getGameProfile().name();
        cow.setData(ModAttachments.BRAND.get(), new Brand(owner.getUUID(), name, town, cow.getBlockX(), cow.getBlockZ()));
        if (!cow.hasCustomName()) {
            cow.setCustomName(Component.translatable(StakeText.BRAND_NAME, name));
        }
    }

    /** The branded stock of a claim's owner standing on the claim. */
    public static List<Animal> on(ServerLevel level, StakeData.Claim claim) {
        if (!level.isLoaded(claim.stake())) {
            return List.of();
        }
        return level.getEntitiesOfClass(Animal.class, new AABB(claim.stake()).inflate(StakeData.RADIUS, 16, StakeData.RADIUS), animal -> {
            Brand brand = brandOf(animal);
            return animal.isAlive() && brand != null && brand.owner().equals(claim.owner());
        });
    }

    public static int count(ServerLevel level, StakeData.Claim claim) {
        return on(level, claim).size();
    }

    // ---------------------------------------------------------------- rustlers

    /** Once in a while at night: rustlers come for the stock of an owner who's home (a herd with nobody near isn't loaded to rustle). */
    public static void rustlers(ServerLevel level) {
        if (!StakeConfig.claims() || !level.isDarkOutside()) {
            return;
        }
        for (StakeData.Claim claim : StakeData.get(level).all()) {
            ServerPlayer owner = level.getServer().getPlayerList().getPlayer(claim.owner());
            if (owner == null || owner.level() != level || !owner.blockPosition().closerThan(claim.stake(), RUSTLER_HERD_RANGE)
                    || level.getRandom().nextFloat() >= RUSTLE_CHANCE) {
                continue;
            }
            List<Animal> stock = on(level, claim);
            if (stock.size() >= RUSTLE_AT && rustle(level, claim, stock.subList(0, Math.min(2, stock.size()))) > 0) {
                owner.sendSystemMessage(Component.translatable(StakeText.RUSTLERS).withStyle(net.minecraft.ChatFormatting.DARK_RED));
            }
        }
    }

    /** Sends a rustler for each of {@code marks}, riding in from past the fence. How many came. */
    public static int rustle(ServerLevel level, StakeData.Claim claim, List<Animal> marks) {
        int sent = 0;
        float angle = level.getRandom().nextFloat() * Mth.TWO_PI;
        for (Animal mark : marks) {
            int distance = StakeData.RADIUS + 4 + level.getRandom().nextInt(6);
            BlockPos column = claim.stake().offset(Math.round(Mth.cos(angle) * distance), 0, Math.round(Mth.sin(angle) * distance));
            angle += 0.4F;
            if (!level.isLoaded(column)) {
                continue;
            }
            BlockPos ground = level.getHeightmapPos(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, column);
            var outlaw = ModEntities.OUTLAW.get().create(level, EntitySpawnReason.EVENT);
            if (outlaw == null) {
                continue;
            }
            outlaw.snapTo(ground.getX() + 0.5, ground.getY(), ground.getZ() + 0.5, 0.0F, 0.0F);
            outlaw.finalizeSpawn(level, level.getCurrentDifficultyAt(ground), EntitySpawnReason.EVENT, null);
            // He has to get the cow a good way off before it's gone (played: with his own starting place as the finish he
            // was away twelve seconds after the warning)
            BlockPos off = claim.stake().offset(Math.round(Mth.cos(angle - 0.4F) * (distance + RUSTLE_RUN)), 0, Math.round(Mth.sin(angle - 0.4F) * (distance + RUSTLE_RUN)));
            BlockPos away = level.isLoaded(off) ? level.getHeightmapPos(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, off) : ground;
            outlaw.goalSelector.addGoal(1, new RustleGoal(outlaw, mark, claim, away));
            if (level.addFreshEntity(outlaw)) {
                sent++;
            }
        }
        return sent;
    }

    /** A rustler's whole plan: get a rope on the cow, lead it back the way he came, and be gone with it past the fence. */
    public static final class RustleGoal extends Goal {
        private final PathfinderMob rustler;
        private final Animal mark;
        private final StakeData.Claim claim;
        private final BlockPos away;

        public static final int GIVE_UP_TICKS = 2400;
        private int ticks;

        // The rails, gates and low walls between the rustler and his cow come down (and lie there to be put back).
        private void cutFence() {
            if (!(mark.level() instanceof ServerLevel level)) {
                return;
            }
            BlockPos from = rustler.blockPosition(), to = mark.blockPosition();
            boolean cut = false;
            for (BlockPos pos : BlockPos.betweenClosed(Math.min(from.getX(), to.getX()) - 1, Math.min(from.getY(), to.getY()), Math.min(from.getZ(), to.getZ()) - 1,
                    Math.max(from.getX(), to.getX()) + 1, Math.max(from.getY(), to.getY()) + 1, Math.max(from.getZ(), to.getZ()) + 1)) {
                BlockState state = level.getBlockState(pos);
                if (state.is(net.minecraft.tags.BlockTags.FENCES) || state.is(net.minecraft.tags.BlockTags.FENCE_GATES) || state.is(net.minecraft.tags.BlockTags.WALLS)) {
                    level.destroyBlock(pos, true);
                    cut = true;
                }
            }
            if (cut) {
                ServerPlayer owner = level.getServer().getPlayerList().getPlayer(claim.owner());
                if (owner != null) {
                    owner.sendSystemMessage(Component.translatable(StakeText.FENCE_CUT).withStyle(net.minecraft.ChatFormatting.RED));
                }
            }
        }

        public RustleGoal(PathfinderMob rustler, Animal mark, StakeData.Claim claim, BlockPos away) {
            this.rustler = rustler;
            this.mark = mark;
            this.claim = claim;
            this.away = away;
            setFlags(EnumSet.of(Flag.MOVE));
        }

        @Override
        public boolean canUse() {
            return mark.isAlive() && rustler.isAlive();
        }

        @Override
        public boolean requiresUpdateEveryTick() {
            return true;
        }

        @Override
        public void tick() {
            // Played, a rustler stood at the corral rail for good with his rope on a cow the other side of it (opening the
            // gates for him wasn't enough: the gate is the long way round). So he ropes only what he can see, cuts the
            // fence between them as he does, and leads it off on the rope; and a rustler who can't
            // get a cow clear in two minutes rides off without it. Walls keep stock in; rails only slow him down.
            if (++ticks > GIVE_UP_TICKS) {
                if (mark instanceof Leashable leashable && leashable.getLeashHolder() == rustler) {
                    leashable.removeLeash();
                }
                rustler.discard();
                return;
            }
            boolean roped = mark instanceof Leashable leashable && leashable.getLeashHolder() == rustler;
            if (!roped) {
                rustler.getNavigation().moveTo(mark, 1.1);
                if (rustler.distanceToSqr(mark) < 6.25 && rustler.hasLineOfSight(mark) && mark instanceof Leashable leashable && !leashable.isLeashed()) {
                    leashable.setLeashedTo(rustler, true);
                    cutFence();
                }
                return;
            }
            rustler.getNavigation().moveTo(away.getX() + 0.5, away.getY(), away.getZ() + 0.5, 1.0);
            if (rustler.blockPosition().closerThan(away, 3.0) && rustler.level() instanceof ServerLevel level) {
                ServerPlayer owner = level.getServer().getPlayerList().getPlayer(claim.owner());
                if (owner != null) {
                    owner.sendSystemMessage(Component.translatable(StakeText.RUSTLED).withStyle(net.minecraft.ChatFormatting.RED));
                }
                ((Leashable) mark).removeLeash();
                mark.discard();
                rustler.discard();
            }
        }
    }

    // ---------------------------------------------------------------- a herd on the move

    /** Something loud at {@code pos}: herd cattle in earshot run from it. Whether any did. */
    public static boolean spook(ServerLevel level, Vec3 pos, double range) {
        List<Animal> herd = level.getEntitiesOfClass(Animal.class, AABB.ofSize(pos, range * 2, 16, range * 2), animal -> herdOf(animal) != null);
        long until = level.getGameTime() + STAMPEDE_TICKS;
        herd.forEach(cow -> cow.setData(ModAttachments.STAMPEDE.get(), new Stampede(until, pos.x, pos.z)));
        return !herd.isEmpty();
    }

    public static boolean stampeding(Entity cow) {
        return cow.getData(ModAttachments.STAMPEDE.get()).until() > cow.level().getGameTime();
    }

    /**
     * What a herd cow does besides being a cow: it runs from whatever spooked it; it follows a herd-mate on a rope; it walks
     * on with the rest of the bunch toward where the drive is bound while its drover is close; and it comes back to the
     * bunch when it has strayed. Otherwise it
     * grazes, follows wheat and panics as cows do.
     */
    public static final class DriveGoal extends Goal {
        public static final double PUSH_RANGE = 10.0, HERD_RANGE = 20.0, BUNCH = 10.0, AHEAD = 20.0, LEAD_GAP = 4.0, STRAY = 9.0, MATES = 24.0;
        private final PathfinderMob cow;
        private @Nullable Vec3 to;
        private double speed;

        public DriveGoal(PathfinderMob cow) {
            this.cow = cow;
            setFlags(EnumSet.of(Flag.MOVE));
        }

        @Override
        public boolean canUse() {
            Herd herd = herdOf(cow);
            if (herd == null || cow.isPassenger() || cow instanceof Leashable leashable && leashable.isLeashed()) {
                return false;
            }
            return (to = plan(herd)) != null;
        }

        @Override
        public boolean canContinueToUse() {
            return to != null && !cow.getNavigation().isDone() && herdOf(cow) != null && cow.tickCount % 20 != 0;
        }

        @Override
        public void start() {
            if (to != null) {
                cow.getNavigation().moveTo(to.x, to.y, to.z, speed);
            }
        }

        @Override
        public void stop() {
            to = null;
        }

        /** Where this cow should be heading, and how fast (in {@link #speed}); null to be left to itself. */
        public @Nullable Vec3 plan(Herd herd) {
            Vec3 at = cow.position();
            if (stampeding(cow)) {
                Stampede from = cow.getData(ModAttachments.STAMPEDE.get());
                speed = 2.2;
                return at.add(flat(at.x - from.x(), at.z - from.z()).scale(12.0));
            }
            List<PathfinderMob> mates = cow.level().getEntitiesOfClass(PathfinderMob.class, cow.getBoundingBox().inflate(MATES, 8, MATES), other -> {
                Herd theirs = herdOf(other);
                return other != cow && other.isAlive() && theirs != null && theirs.drive().equals(herd.drive());
            });
            Optional<PathfinderMob> lead = mates.stream().filter(m -> m instanceof Leashable leashable && leashable.getLeashHolder() instanceof Player)
                    .min((a, b) -> Double.compare(a.distanceToSqr(cow), b.distanceToSqr(cow)));
            if (lead.isPresent()) {
                double gap = Math.sqrt(lead.get().distanceToSqr(cow));
                if (gap <= LEAD_GAP) {
                    return null;
                }
                speed = gap > 10.0 ? 2.2 : 1.6;
                return lead.get().position();
            }
            Player drover = cow.level().getPlayerByUUID(herd.drover());
            if (drover != null && drover.isAlive()) {
                // The herd knows where it's bound and walks there as one, so long as its drover is close: a cow out of
                // the bunch comes back to it first, and one that has got ahead of the drover waits. (Played twice in real
                // country: cows that moved only for a drover at their heels made 17 blocks in three minutes, and a herd
                // pushed away from wherever the drover stood fanned out along the first river and lost eight of twelve.)
                List<PathfinderMob> all = new java.util.ArrayList<>(mates);
                all.add(cow);
                Vec3 middle = centre(all);
                double toMiddle = Math.hypot(middle.x - drover.getX(), middle.z - drover.getZ());
                double toCow = Math.sqrt(drover.distanceToSqr(cow));
                if (toMiddle <= HERD_RANGE || toCow <= PUSH_RANGE) {
                    if (Math.hypot(middle.x - at.x, middle.z - at.z) > BUNCH) {
                        speed = 1.8;
                        return new Vec3(middle.x, at.y, middle.z);
                    }
                    Drives.Drive drive = Drives.of(drover);
                    Vec3 way = drive != null && drive.id().equals(herd.drive())
                            ? flat(drive.dest().getX() + 0.5 - middle.x, drive.dest().getZ() + 0.5 - middle.z)
                            : toMiddle > 2.5 ? flat(middle.x - drover.getX(), middle.z - drover.getZ()) : flat(at.x - drover.getX(), at.z - drover.getZ());
                    if (toCow > AHEAD && (at.x - drover.getX()) * way.x + (at.z - drover.getZ()) * way.z > 0) {
                        return null;
                    }
                    Vec3 close = new Vec3(middle.x - at.x, 0, middle.z - at.z).scale(0.5);
                    speed = 1.5;
                    return dry(at, way.scale(8.0).add(close));
                }
            }
            Vec3 bunch = centre(mates);
            if (bunch != null && bunch.distanceToSqr(at) > STRAY * STRAY) {
                speed = 1.2;
                return bunch;
            }
            return null;
        }

        // Where a step of {@code by} from {@code at} comes down, turned aside (a little, then more) if that's in water
        // and somewhere to one side isn't: a herd goes round a pond, and through a river only when there's no way round.
        private Vec3 dry(Vec3 at, Vec3 by) {
            for (float turn : new float[] {0.0F, 0.6F, -0.6F, 1.2F, -1.2F}) {
                Vec3 to = at.add(by.yRot(turn));
                BlockPos ground = cow.level().getHeightmapPos(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, BlockPos.containing(to));
                if (cow.level().getFluidState(ground.below()).isEmpty()) {
                    return new Vec3(to.x, ground.getY(), to.z);
                }
            }
            return at.add(by);
        }

        private static Vec3 flat(double x, double z) {
            Vec3 v = new Vec3(x, 0, z);
            return v.lengthSqr() < 1.0E-4 ? new Vec3(1, 0, 0) : v.normalize();
        }

        private static @Nullable Vec3 centre(List<PathfinderMob> mates) {
            if (mates.isEmpty()) {
                return null;
            }
            Vec3 sum = Vec3.ZERO;
            for (PathfinderMob mate : mates) {
                sum = sum.add(mate.position());
            }
            return sum.scale(1.0 / mates.size());
        }
    }
}
