package com.hollowfrontier.content.hollow.gray;

import java.util.List;

import com.hollowfrontier.HollowFrontier;
import com.hollowfrontier.content.hollow.megafauna.TerrorBird;
import com.hollowfrontier.registry.ModCriteria;
import com.hollowfrontier.registry.ModItems;

import net.minecraft.core.Holder;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.EntitySelector;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.LivingDropsEvent;

/**
 * The Grays' tagged megafauna (10b). The implant makes a beast half again as hardy, armours it in plating (+6), quickens it
 * (a fifth; a quarter for the terror bird) and hits harder (a quarter), and it drives the beast too: tagged mammoths and
 * megatheria go for players on sight, a tagged terror bird no longer keeps off mammoths, a tagged glyptodon never
 * hunkers, and none of them heed the Oliphant. They come from the live Gray field stations: penned there, a few loose
 * on trial (kept near the station, {@link #TETHER}), and now and then one that got away, anywhere a station could be
 * ({@link #ESCAPE_CHANCE} of the megafauna spawning wild). Killed, a tagged beast gives up 1-3 Gray tech fragments.
 */
@EventBusSubscriber(modid = HollowFrontier.MODID)
public final class Specimens {
    private Specimens() {}

    /** Of the megafauna spawning wild (natural or chunk-generation spawns), the share that escaped a station. */
    public static final float ESCAPE_CHANCE = 0.03F;
    /** How far a specimen loose at its station strays from where it was set down. */
    public static final int TETHER = 24;
    public static final String SAVE_KEY = "GrayTagged";

    private record Boost(Holder<Attribute> attribute, String name, double amount, AttributeModifier.Operation operation) {}

    private static final List<Boost> BOOSTS = List.of(
            new Boost(Attributes.MAX_HEALTH, "gray_implant_health", 0.5, AttributeModifier.Operation.ADD_MULTIPLIED_BASE),
            new Boost(Attributes.ARMOR, "gray_implant_plating", 6.0, AttributeModifier.Operation.ADD_VALUE),
            new Boost(Attributes.ATTACK_DAMAGE, "gray_implant_strength", 0.25, AttributeModifier.Operation.ADD_MULTIPLIED_BASE));
    private static final Identifier SPEED = HollowFrontier.id("gray_implant_speed");

    /** Puts the implant's boosts on (or takes them off); heals a beast that's newly tagged. */
    public static void applyBoosts(Mob mob, boolean tagged) {
        boolean wasTagged = mob.getAttribute(Attributes.MAX_HEALTH).hasModifier(HollowFrontier.id("gray_implant_health"));
        for (Boost boost : BOOSTS) {
            AttributeInstance instance = mob.getAttribute(boost.attribute());
            if (instance == null) {
                continue;
            }
            Identifier id = HollowFrontier.id(boost.name());
            if (tagged) {
                instance.addOrReplacePermanentModifier(new AttributeModifier(id, boost.amount(), boost.operation()));
            } else {
                instance.removeModifier(id);
            }
        }
        AttributeInstance speed = mob.getAttribute(Attributes.MOVEMENT_SPEED);
        if (tagged) {
            speed.addOrReplacePermanentModifier(new AttributeModifier(SPEED, mob instanceof TerrorBird ? 0.25 : 0.2, AttributeModifier.Operation.ADD_MULTIPLIED_BASE));
        } else {
            speed.removeModifier(SPEED);
        }
        if (tagged && !wasTagged) {
            mob.setHealth(mob.getMaxHealth());
        } else if (mob.getHealth() > mob.getMaxHealth()) {
            mob.setHealth(mob.getMaxHealth());
        }
    }

    /** For a specimen's target selector: once tagged, it goes for players on sight (not creative or spectating ones). */
    public static <T extends Mob & Specimen> NearestAttackableTargetGoal<Player> huntWhenTagged(T mob) {
        return new NearestAttackableTargetGoal<>(mob, Player.class, 10, true, false,
                (player, level) -> mob.isTagged() && EntitySelector.NO_CREATIVE_OR_SPECTATOR.test(player));
    }

    /** Whether an entity is a tagged specimen. */
    public static boolean isTagged(LivingEntity entity) {
        return entity instanceof Specimen specimen && specimen.isTagged();
    }

    /**
     * On spawning: a wild beast may turn out to be one that escaped a station; one a station's template sets down tagged
     * is kept near where it was put.
     */
    public static void onSpawn(Mob mob, EntitySpawnReason reason, RandomSource random) {
        if (!(mob instanceof Specimen specimen)) {
            return;
        }
        if ((reason == EntitySpawnReason.NATURAL || reason == EntitySpawnReason.CHUNK_GENERATION) && random.nextFloat() < ESCAPE_CHANCE) {
            specimen.setTagged(true);
        } else if (reason == EntitySpawnReason.STRUCTURE && specimen.isTagged()) {
            mob.setHomeTo(mob.blockPosition(), TETHER);
        }
    }

    /** Sets every tagged specimen within {@code range} of {@code at} on {@code player} (a sentinel's alarm); returns how many. */
    public static int alarm(ServerLevel level, Player player, Vec3 at, double range) {
        int roused = 0;
        for (Mob mob : level.getEntitiesOfClass(Mob.class, new AABB(at, at).inflate(range), m -> m.isAlive() && isTagged(m))) {
            mob.setTarget(player);
            roused++;
        }
        return roused;
    }

    /** A tagged beast gives up 1-3 fragments of its implant and plating. */
    @SubscribeEvent
    static void onDrops(LivingDropsEvent event) {
        LivingEntity entity = event.getEntity();
        if (isTagged(entity) && entity.level() instanceof ServerLevel level) {
            int count = 1 + level.getRandom().nextInt(3);
            event.getDrops().add(new ItemEntity(level, entity.getX(), entity.getY(), entity.getZ(), new ItemStack(ModItems.GRAY_TECH_FRAGMENT.get(), count)));
            if (event.getSource().getEntity() instanceof Player killer) {
                ModCriteria.fire(killer, ModCriteria.SPECIMEN_KILLED);
            }
        }
    }
}
