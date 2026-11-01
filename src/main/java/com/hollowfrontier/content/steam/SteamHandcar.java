package com.hollowfrontier.content.steam;

import com.hollowfrontier.registry.ModBlocks;
import com.hollowfrontier.registry.ModCriteria;
import com.hollowfrontier.registry.ModFluids;
import com.hollowfrontier.registry.ModItems;
import com.hollowfrontier.registry.ModSounds;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.player.Input;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.vehicle.minecart.Minecart;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.transfer.ResourceHandler;
import net.neoforged.neoforge.transfer.fluid.FluidResource;
import net.neoforged.neoforge.transfer.transaction.Transaction;

/**
 * A steam handcar: a minecart with a little vertical boiler at one end, driven by its rider. Hold forward to open the
 * throttle (it burns {@link #STEAM_PER_TICK} from its own tank and runs up to {@link #SPEED_FACTOR} times a minecart's top
 * speed), back to brake, jump to blow the whistle. It's a "furnace" to vanilla, so it shoves other carts along ahead of it.
 * Fill it by using a steam canister on it, or stop beside any steam holder (a tank, pipe or boiler next to the rail or under
 * it) and it draws steam through the transfer API. Railroad depots have such a standpipe.
 */
public class SteamHandcar extends Minecart {
    public static final int CAPACITY = 8_000;
    public static final int STEAM_PER_TICK = 3;
    public static final int WHISTLE_STEAM = 20;
    public static final double SPEED_FACTOR = 1.75;
    /** Throttle: speed gained per tick at full steam. */
    static final double ACCELERATION = 0.03;
    /** Refuelling: steam drawn per tick from holders around the rail, while (nearly) stopped. */
    static final int REFILL_RATE = 100;
    private static final double STOPPED = 0.05;

    private static final EntityDataAccessor<Integer> DATA_STEAM = SynchedEntityData.defineId(SteamHandcar.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Boolean> DATA_RUNNING = SynchedEntityData.defineId(SteamHandcar.class, EntityDataSerializers.BOOLEAN);

    // Server: what the rider asked for this tick (+1 throttle, -1 brake), whether jump was already down, whistle cooldown.
    private int throttle;
    private boolean jumpHeld;
    private int whistleCooldown;
    private int chuff;
    // Distance driven under steam on this ride (reset when the rider gets off), and how much of it we've reported.
    private double driven;
    private int reported;
    // Throttle for a driverless handcar (tests); a rider's input replaces it.
    private int standingThrottle;

    public SteamHandcar(EntityType<?> type, Level level) {
        super(type, level);
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
        builder.define(DATA_STEAM, 0);
        builder.define(DATA_RUNNING, false);
    }

    public int steam() {
        return entityData.get(DATA_STEAM);
    }

    public void setSteam(int amount) {
        entityData.set(DATA_STEAM, Mth.clamp(amount, 0, CAPACITY));
    }

    public boolean isRunning() {
        return entityData.get(DATA_RUNNING);
    }

    /** Pushes steam in from outside (canister, standpipe); returns how much it took. */
    public int fill(int amount) {
        int taken = Math.min(amount, CAPACITY - steam());
        setSteam(steam() + taken);
        return taken;
    }

    @Override
    public boolean isFurnace() {
        return true; // pushes carts ahead of it instead of being pushed by them
    }

    @Override
    public void tick() {
        if (level() instanceof ServerLevel level) {
            readRider();
        }
        Vec3 before = position();
        super.tick();
        if (level() instanceof ServerLevel level) {
            if (isRunning()) {
                driven += position().subtract(before).horizontalDistance();
            }
            serverTick(level);
        } else if (isRunning() && random.nextInt(2) == 0) {
            Vec3 stack = stackPosition();
            level().addParticle(ParticleTypes.LARGE_SMOKE, stack.x, stack.y, stack.z, 0, 0.04, 0);
        }
    }

    private void readRider() {
        throttle = standingThrottle;
        if (getFirstPassenger() instanceof ServerPlayer player) {
            Input input = player.getLastClientInput();
            throttle = input.forward() ? 1 : input.backward() ? -1 : 0;
            boolean jump = input.jump();
            if (jump && !jumpHeld) {
                whistle(player);
            }
            jumpHeld = jump;
        } else {
            jumpHeld = false;
            driven = 0;
            reported = 0;
        }
    }

    private void serverTick(ServerLevel level) {
        if (whistleCooldown > 0) {
            whistleCooldown--;
        }
        boolean running = throttle > 0 && steam() >= STEAM_PER_TICK;
        entityData.set(DATA_RUNNING, running);
        double speed = getDeltaMovement().horizontalDistance();
        if (running) {
            setSteam(steam() - STEAM_PER_TICK);
            // The engine beats faster as it picks up speed.
            if (--chuff <= 0) {
                level.playSound(null, getX(), getY(), getZ(), ModSounds.HANDCAR_CHUFF.get(), SoundSource.NEUTRAL, 0.6F, 0.9F + random.nextFloat() * 0.2F);
                chuff = Mth.clamp((int) Math.round(12 - speed * 14), 3, 12);
            }
        } else if (speed < STOPPED) {
            drawFromStandpipe(level);
        }
        if (getFirstPassenger() instanceof ServerPlayer player) {
            if (tickCount % 10 == 0) {
                player.sendOverlayMessage(gauge());
            }
            if (driven >= reported + 10) {
                reported = (int) driven;
                ModCriteria.HANDCAR_DRIVEN.get().trigger(player, reported);
            }
        }
    }

    /** Draws steam from every steam holder touching the rail (sides and below), the way a locomotive takes on water. */
    void drawFromStandpipe(ServerLevel level) {
        if (steam() >= CAPACITY) {
            return;
        }
        BlockPos rail = blockPosition();
        int before = steam();
        for (Direction dir : new Direction[] {Direction.DOWN, Direction.NORTH, Direction.SOUTH, Direction.EAST, Direction.WEST}) {
            int want = Math.min(REFILL_RATE - (steam() - before), CAPACITY - steam());
            if (want <= 0) {
                break;
            }
            ResourceHandler<FluidResource> handler = level.getCapability(Capabilities.Fluid.BLOCK, rail.relative(dir), dir.getOpposite());
            if (handler == null) {
                continue;
            }
            try (Transaction tx = Transaction.openRoot()) {
                int got = ModFluids.extractSteam(handler, want, tx);
                if (got > 0) {
                    tx.commit();
                    fill(got);
                }
            }
        }
        if (steam() > before && tickCount % 20 == 0) {
            level.playSound(null, getX(), getY(), getZ(), SoundEvents.BUCKET_FILL, SoundSource.NEUTRAL, 0.4F, 1.6F);
            if (steam() >= CAPACITY && getFirstPassenger() instanceof ServerPlayer player) {
                ModCriteria.HANDCAR_REFUELLED.get().trigger(player);
            }
        }
    }

    private void whistle(ServerPlayer player) {
        if (whistleCooldown > 0 || steam() < WHISTLE_STEAM) {
            return;
        }
        setSteam(steam() - WHISTLE_STEAM);
        whistleCooldown = 30;
        ServerLevel level = (ServerLevel) level();
        level.playSound(null, getX(), getY(), getZ(), ModSounds.HANDCAR_WHISTLE.get(), SoundSource.NEUTRAL, 2.0F, 1.0F);
        Vec3 stack = stackPosition();
        level.sendParticles(ParticleTypes.CLOUD, stack.x, stack.y + 0.2, stack.z, 6, 0.1, 0.2, 0.1, 0.02);
        ModCriteria.HANDCAR_WHISTLE.get().trigger(player);
    }

    private Component gauge() {
        int bars = (int) Math.ceil(10.0 * steam() / CAPACITY);
        return Component.translatable("message.hollowfrontier.handcar.gauge", "▮".repeat(bars) + "▯".repeat(10 - bars), steam() * 100 / CAPACITY);
    }

    // Where the smokestack is: the engine sits at one end of the cart (yRot points along it; checked with runPhoto).
    private Vec3 stackPosition() {
        float yaw = getYRot() * Mth.DEG_TO_RAD;
        return new Vec3(getX() + Mth.cos(yaw) * 0.3, getY() + 1.75, getZ() + Mth.sin(yaw) * 0.3);
    }

    @Override
    protected Vec3 applyNaturalSlowdown(Vec3 movement) {
        if (throttle > 0 && isRunning()) {
            Vec3 along = movement.horizontalDistanceSqr() > 1.0E-4 ? movement.horizontal().normalize() : riderFacing();
            movement = movement.add(along.scale(ACCELERATION));
            // Vanilla moves a ridden cart only 0.75 of its speed per tick, so let a ridden one carry enough to reach top speed.
            double max = (level() instanceof ServerLevel level ? getMaxSpeed(level) : 0.7) / (isVehicle() ? 0.75 : 1.0);
            if (movement.horizontalDistance() > max) {
                Vec3 h = movement.horizontal().normalize().scale(max);
                movement = new Vec3(h.x, movement.y, h.z);
            }
        } else if (throttle < 0) {
            movement = movement.multiply(0.8, 1.0, 0.8);
        }
        return super.applyNaturalSlowdown(movement);
    }

    private Vec3 riderFacing() {
        if (getFirstPassenger() instanceof Player player) {
            float yaw = player.getYRot() * Mth.DEG_TO_RAD;
            return new Vec3(-Mth.sin(yaw), 0, Mth.cos(yaw));
        }
        return Vec3.ZERO;
    }

    @Override
    protected double getMaxSpeed(ServerLevel level) {
        return steam() > 0 ? super.getMaxSpeed(level) * SPEED_FACTOR : super.getMaxSpeed(level);
    }

    @Override
    public InteractionResult interact(Player player, InteractionHand hand, Vec3 location) {
        ItemStack held = player.getItemInHand(hand);
        if (held.getItem() instanceof SteamCanisterItem) {
            if (!level().isClientSide()) {
                int poured = fill(SteamCanisterItem.steam(held));
                if (poured > 0) {
                    SteamCanisterItem.setSteam(held, SteamCanisterItem.steam(held) - poured);
                    level().playSound(null, getX(), getY(), getZ(), SoundEvents.BUCKET_EMPTY, SoundSource.NEUTRAL, 0.6F, 1.5F);
                }
                player.sendOverlayMessage(gauge());
            }
            return InteractionResult.SUCCESS;
        }
        return super.interact(player, hand, location);
    }

    @Override
    public BlockState getDefaultDisplayBlockState() {
        return ModBlocks.HANDCAR_ENGINE.get().defaultBlockState().setValue(HandcarEngineBlock.LIT, steam() > 0);
    }

    @Override
    protected Item getDropItem() {
        return ModItems.STEAM_HANDCAR.get();
    }

    @Override
    public ItemStack getPickResult() {
        return new ItemStack(ModItems.STEAM_HANDCAR.get());
    }

    @Override
    protected void addAdditionalSaveData(ValueOutput output) {
        super.addAdditionalSaveData(output);
        output.putInt("steam", steam());
    }

    @Override
    protected void readAdditionalSaveData(ValueInput input) {
        super.readAdditionalSaveData(input);
        setSteam(input.getIntOr("steam", 0));
    }

    /** For tests: a standing throttle (+1 open, -1 brake, 0 shut) used while nobody is aboard. */
    public void setThrottle(int throttle) {
        this.standingThrottle = throttle;
    }
}
