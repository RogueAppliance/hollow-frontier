package com.hollowfrontier.client;

import java.util.Map;
import java.util.WeakHashMap;

import com.hollowfrontier.HollowFrontier;
import com.hollowfrontier.content.WorldFeel;
import com.hollowfrontier.content.frontier.gun.GunFeel;
import com.hollowfrontier.content.frontier.gun.GunItem;
import com.hollowfrontier.content.hollow.megafauna.Glyptodon;
import com.hollowfrontier.content.hollow.megafauna.Megatherium;
import com.hollowfrontier.content.hollow.megafauna.WoollyMammoth;
import com.hollowfrontier.content.steam.SteamHandcar;
import com.hollowfrontier.content.steam.airship.Airship;
import com.hollowfrontier.content.steam.submersible.SteamSubmersible;
import com.hollowfrontier.content.ufo.Mothership;
import com.hollowfrontier.registry.ModAttachments;
import com.hollowfrontier.registry.ModDataComponents;
import com.hollowfrontier.registry.ModSounds;

import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.resources.sounds.AbstractTickableSoundInstance;
import net.minecraft.client.resources.sounds.SoundInstance;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.resources.Identifier;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.Leashable;
import net.minecraft.world.entity.animal.equine.AbstractHorse;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.sound.PlaySoundEvent;

/**
 * What acting in the world feels like (46), worked out on each client from what it can already see, so nothing new
 * crosses the wire and everyone near sees the same dust:
 * <ul>
 * <li><b>Your own shot</b> kicks the view ({@link CameraFeel}).</li>
 * <li><b>Horses</b> at a gallop kick up the ground they're on, and a shod one rings on stone.</li>
 * <li><b>A line</b> with something pulling on it creaks.</li>
 * <li><b>The airship</b>: wind by speed and height, a creaking envelope, cloud past the deck, a deck that leans.</li>
 * <li><b>The handcar</b>: rail joints, and sparks when it brakes.</li>
 * <li><b>The big things</b>: megafauna's footfalls, blasts, a rammed hull, the Mothership coming in.</li>
 * </ul>
 * The numbers are in {@link WorldFeel} and {@link GunFeel}.
 */
@EventBusSubscriber(modid = HollowFrontier.MODID, value = Dist.CLIENT)
public final class WorldFeelClient {
    private WorldFeelClient() {}

    /** Nothing further off than this is looked at (the Mothership apart, which is felt from further). */
    private static final double NEAR = 48.0;

    private static final RandomSource RANDOM = RandomSource.create();
    // How far each thing has gone since its last stride, joint or footfall, and how fast a handcar was going last tick
    private static final Map<Entity, Double> TRAVEL = new WeakHashMap<>();
    private static final Map<Entity, Double> SPEED_BEFORE = new WeakHashMap<>();
    private static final long[] SHOT_SEEN = {Long.MIN_VALUE, Long.MIN_VALUE};
    private static Wind wind;
    private static int creakWait, rumbleWait;
    private static SteamSubmersible ridden;
    private static float hullBefore;

    @SubscribeEvent
    static void onClientTick(ClientTickEvent.Post event) {
        Minecraft mc = Minecraft.getInstance();
        LocalPlayer player = mc.player;
        ClientLevel level = mc.level;
        if (player == null || level == null || mc.isPaused()) {
            return;
        }
        ownShot(player, level);
        for (Entity entity : level.entitiesForRendering()) {
            double distance = entity.distanceTo(player);
            if (entity instanceof Mothership ship) {
                arriving(level, player, ship);
            }
            if (distance > NEAR) {
                continue;
            }
            if (entity instanceof AbstractHorse horse) {
                hooves(level, horse);
            } else if (entity instanceof SteamHandcar) {
                rails(level, entity);
            } else if (entity instanceof WoollyMammoth) {
                tread(level, entity, WorldFeel.MAMMOTH, distance);
            } else if (entity instanceof Megatherium) {
                tread(level, entity, WorldFeel.MEGATHERIUM, distance);
            } else if (entity instanceof Glyptodon) {
                tread(level, entity, WorldFeel.GLYPTODON, distance);
            }
            if (entity instanceof Leashable leashed && leashed.getLeashHolder() == player && distance > 4.5
                    && (player.tickCount + entity.getId()) % 30 == 0) {
                Vec3 mid = entity.position().add(player.position()).scale(0.5);
                level.playLocalSound(mid.x, mid.y + 1.0, mid.z, ModSounds.LINE_CREAK.get(), SoundSource.NEUTRAL, 0.5F, 0.8F + RANDOM.nextFloat() * 0.3F, false);
            }
        }
        aloft(mc, level, player);
        rammed(player);
    }

    /** Blocks over the ground this entity moved in the last tick. */
    private static double speed(Entity entity) {
        double dx = entity.getX() - entity.xo;
        double dz = entity.getZ() - entity.zo;
        return Math.sqrt(dx * dx + dz * dz);
    }

    /** Adds a tick's travel; true each time {@code every} blocks have gone by. */
    private static boolean covered(Entity entity, double speed, double every) {
        double travel = TRAVEL.getOrDefault(entity, 0.0) + speed;
        boolean stride = travel >= every;
        TRAVEL.put(entity, stride ? travel - every : travel);
        return stride;
    }

    // ---- 46a: your own shot ----

    /**
     * A gun in either hand whose "last fired" has just changed to now is a shot of ours. A gun seen for the first time,
     * or drawn again with an old shot on it, isn't.
     */
    private static void ownShot(LocalPlayer player, ClientLevel level) {
        long now = level.getGameTime();
        for (InteractionHand hand : InteractionHand.values()) {
            ItemStack stack = player.getItemInHand(hand);
            Long fired = stack.getItem() instanceof GunItem ? stack.get(ModDataComponents.LAST_FIRED.get()) : null;
            if (fired == null) {
                continue;
            }
            int slot = hand.ordinal();
            if (fired != SHOT_SEEN[slot] && Math.abs(now - fired) <= 3) {
                CameraFeel.kick(GunFeel.kick(((GunItem) stack.getItem()).stats()), RANDOM.nextBoolean());
            }
            SHOT_SEEN[slot] = fired;
        }
    }

    // ---- 46b: riding and flying ----

    private static void hooves(ClientLevel level, AbstractHorse horse) {
        double speed = speed(horse);
        if (!horse.onGround() || speed < WorldFeel.GALLOP || !covered(horse, speed, WorldFeel.STRIDE)) {
            return;
        }
        BlockState ground = horse.getBlockStateOn();
        BlockPos under = horse.getOnPos();
        int dust = WorldFeel.dust(ground, level.isRainingAt(under.above()));
        // Thrown up and back from behind the hooves
        Vec3 back = new Vec3(horse.xo - horse.getX(), 0, horse.zo - horse.getZ()).normalize();
        for (int i = 0; i < dust; i++) {
            level.addParticle(new BlockParticleOption(ParticleTypes.BLOCK, ground),
                    horse.getX() + back.x * 0.7 + (RANDOM.nextDouble() - 0.5) * 0.8, horse.getY() + 0.1,
                    horse.getZ() + back.z * 0.7 + (RANDOM.nextDouble() - 0.5) * 0.8,
                    back.x * 1.5 + (RANDOM.nextDouble() - 0.5), 1.5 + RANDOM.nextDouble(), back.z * 1.5 + (RANDOM.nextDouble() - 0.5));
        }
        if (WorldFeel.ground(ground).rings() && horse.getData(ModAttachments.HORSESHOE_TIER.get()) > 0) {
            level.playLocalSound(horse.getX(), horse.getY(), horse.getZ(), ModSounds.HORSESHOE_RING.get(), SoundSource.NEUTRAL,
                    0.45F, 0.9F + RANDOM.nextFloat() * 0.3F, false);
        }
    }

    private static void rails(ClientLevel level, Entity cart) {
        double speed = speed(cart);
        double before = SPEED_BEFORE.getOrDefault(cart, speed);
        SPEED_BEFORE.put(cart, speed);
        if (speed > 0.02 && covered(cart, speed, WorldFeel.RAIL_JOINT)) {
            level.playLocalSound(cart.getX(), cart.getY(), cart.getZ(), ModSounds.HANDCAR_CLACK.get(), SoundSource.NEUTRAL,
                    0.5F, 0.9F + RANDOM.nextFloat() * 0.15F, false);
        }
        if (speed > 0.08 && before - speed >= WorldFeel.BRAKING) {
            for (int i = 0; i < 2; i++) {
                level.addParticle(ParticleTypes.ELECTRIC_SPARK, cart.getX() + (RANDOM.nextDouble() - 0.5) * 0.9, cart.getY() + 0.05,
                        cart.getZ() + (RANDOM.nextDouble() - 0.5) * 0.9, (RANDOM.nextDouble() - 0.5) * 0.3, 0.15, (RANDOM.nextDouble() - 0.5) * 0.3);
            }
        }
    }

    /** Aboard an airship: the wind, the envelope, the cloud and the lean of the deck. */
    private static void aloft(Minecraft mc, ClientLevel level, LocalPlayer player) {
        if (creakWait > 0) {
            creakWait--;
        }
        if (!(player.getVehicle() instanceof Airship ship)) {
            return;
        }
        if (wind == null || wind.isStopped() || wind.ship != ship) {
            wind = new Wind(player, ship);
            mc.getSoundManager().play(wind);
        }
        double speed = speed(ship);
        float turn = Mth.wrapDegrees(ship.getYRot() - ship.yRotO);
        double climb = ship.getY() - ship.yo;
        boolean flying = !ship.onGround();
        // The deck leans into a turn, and is never quite still in the air
        CameraFeel.sway(flying ? WorldFeel.sway(turn) + Mth.sin(player.tickCount * 0.05F) * 0.35F : 0.0F);
        if (flying && creakWait == 0 && (Math.abs(turn) >= WorldFeel.CREAK_TURN || Math.abs(climb) >= WorldFeel.CREAK_CLIMB)) {
            creakWait = 50 + RANDOM.nextInt(40);
            level.playLocalSound(ship.getX(), ship.getY() + 3.0, ship.getZ(), ModSounds.AIRSHIP_CREAK.get(), SoundSource.NEUTRAL,
                    0.6F, 0.8F + RANDOM.nextFloat() * 0.3F, false);
        }
        if (level.dimension() == Level.OVERWORLD && WorldFeel.inCloud(ship.getY()) && speed > 0.05) {
            Vec3 going = new Vec3(ship.getX() - ship.xo, 0, ship.getZ() - ship.zo);
            for (int i = 0; i < 2; i++) {
                level.addParticle(ParticleTypes.CLOUD, ship.getX() + going.x * 12 + (RANDOM.nextDouble() - 0.5) * 12,
                        ship.getY() + 0.5 + RANDOM.nextDouble() * 4.0, ship.getZ() + going.z * 12 + (RANDOM.nextDouble() - 0.5) * 12,
                        -going.x * 0.4, 0.0, -going.z * 0.4);
            }
        }
    }

    // ---- 46c: the big things ----

    private static void tread(ClientLevel level, Entity beast, WorldFeel.Tread tread, double distance) {
        double speed = speed(beast);
        if (!beast.onGround() || speed < 0.01 || !covered(beast, speed, tread.stride())) {
            return;
        }
        boolean charging = speed >= WorldFeel.CHARGE;
        level.playLocalSound(beast.getX(), beast.getY(), beast.getZ(), ModSounds.BEAST_FOOTFALL.get(), SoundSource.NEUTRAL,
                (charging ? 1.3F : 0.9F) * (float) (tread.range() / 12.0), (charging ? 0.95F : 0.8F) + RANDOM.nextFloat() * 0.15F, false);
        BlockState ground = beast.getBlockStateOn();
        if (!ground.isAir()) {
            for (int i = 0; i < (charging ? 6 : 3); i++) {
                level.addParticle(new BlockParticleOption(ParticleTypes.BLOCK, ground),
                        beast.getX() + (RANDOM.nextDouble() - 0.5) * beast.getBbWidth(), beast.getY() + 0.1,
                        beast.getZ() + (RANDOM.nextDouble() - 0.5) * beast.getBbWidth(),
                        (RANDOM.nextDouble() - 0.5) * 1.5, 1.0 + RANDOM.nextDouble(), (RANDOM.nextDouble() - 0.5) * 1.5);
            }
        }
        CameraFeel.shake(WorldFeel.footfall(tread, speed, distance));
    }

    /** The Mothership coming in: a tremor that holds while it settles, by how nearly you're under it, and its rumble. */
    private static void arriving(ClientLevel level, LocalPlayer player, Mothership ship) {
        if (ship.phase() != Mothership.ARRIVING) {
            return;
        }
        double dx = ship.getX() - player.getX();
        double dz = ship.getZ() - player.getZ();
        float tremor = WorldFeel.shake(WorldFeel.TREMOR, Math.sqrt(dx * dx + dz * dz), WorldFeel.TREMOR_RANGE);
        if (tremor <= 0.0F) {
            return;
        }
        CameraFeel.tremor(tremor);
        if (rumbleWait-- <= 0) {
            rumbleWait = 45;
            level.playLocalSound(player.getX(), player.getY() + 24.0, player.getZ(), ModSounds.CRUST_RUMBLE.get(), SoundSource.HOSTILE,
                    2.5F, 0.7F, false);
        }
    }

    /** A hull that has just lost a piece of itself, with us inside. */
    private static void rammed(LocalPlayer player) {
        if (player.getVehicle() instanceof SteamSubmersible sub) {
            if (sub == ridden && hullBefore - sub.hull() >= 0.5F) {
                CameraFeel.shake(WorldFeel.RAM);
            }
            ridden = sub;
            hullBefore = sub.hull();
        } else {
            ridden = null;
        }
    }

    /** Blasts are told by their sounds: ours only, so nothing of vanilla's or another mod's shakes the view. */
    @SubscribeEvent
    static void onPlaySound(PlaySoundEvent event) {
        SoundInstance sound = event.getSound();
        LocalPlayer player = Minecraft.getInstance().player;
        if (sound == null || player == null) {
            return;
        }
        Identifier id = sound.getIdentifier();
        WorldFeel.Blast blast = id.equals(ModSounds.DYNAMITE_BLAST.getId()) ? WorldFeel.DYNAMITE
                : id.equals(ModSounds.VAULT_BLOWN.getId()) ? WorldFeel.VAULT
                : id.equals(ModSounds.DEATH_RAY_FIRE.getId()) ? WorldFeel.DEATH_RAY
                : id.equals(ModSounds.ABOARD_GROAN.getId()) ? WorldFeel.HULL_GROAN
                : id.equals(ModSounds.POD_CRASH.getId()) ? WorldFeel.POD_CRASH
                : id.equals(ModSounds.POD_LAUNCH.getId()) ? WorldFeel.HULL_GROAN : null;
        if (blast != null) {
            CameraFeel.shake(WorldFeel.blast(blast, Math.sqrt(player.distanceToSqr(sound.getX(), sound.getY(), sound.getZ()))));
        }
    }

    /** The wind over an airship's deck, for whoever is aboard: up with speed and height, gone on the ground. */
    static final class Wind extends AbstractTickableSoundInstance {
        private final LocalPlayer player;
        final Airship ship;

        Wind(LocalPlayer player, Airship ship) {
            super(ModSounds.AIRSHIP_WIND.get(), SoundSource.AMBIENT, SoundInstance.createUnseededRandom());
            this.player = player;
            this.ship = ship;
            this.looping = true;
            this.delay = 0;
            this.volume = 0.01F;
            this.relative = true;
        }

        @Override
        public void tick() {
            boolean aboard = !player.isRemoved() && !ship.isRemoved() && player.getVehicle() == ship;
            double speed = speed(ship);
            float target = aboard && !ship.onGround() ? 0.08F + 0.6F * WorldFeel.wind(speed, ship.getY()) : 0.0F;
            volume = Mth.approach(volume, target, 0.02F);
            pitch = Mth.approach(pitch, 0.8F + 0.4F * (float) Mth.clamp(speed / WorldFeel.WIND_FULL_SPEED, 0.0, 1.0), 0.01F);
            if (!aboard && volume <= 0.0F) {
                stop();
            }
        }
    }
}
