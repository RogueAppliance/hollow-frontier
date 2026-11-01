package com.hollowfrontier.content.frontier.cat;

import java.util.List;
import java.util.Map;
import java.util.WeakHashMap;

import org.jspecify.annotations.Nullable;

import com.hollowfrontier.HollowFrontier;
import com.hollowfrontier.content.frontier.cat.TownCat.Accessory;
import com.hollowfrontier.content.frontier.cat.TownCat.Role;
import com.hollowfrontier.content.frontier.law.Wanted;
import com.hollowfrontier.content.frontier.music.SaloonPianoBlock;
import com.hollowfrontier.content.frontier.outlaw.AbstractOutlaw;
import com.hollowfrontier.content.frontier.wildlife.Rattlesnake;
import com.hollowfrontier.registry.ModAttachments;
import com.hollowfrontier.registry.ModBlocks;
import com.hollowfrontier.registry.ModCriteria;
import com.hollowfrontier.registry.ModSounds;
import com.hollowfrontier.worldgen.ModSpawns;

import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.util.Prediction;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.goal.AvoidEntityGoal;
import net.minecraft.world.entity.ai.goal.MoveTowardsRestrictionGoal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.animal.feline.Cat;
import net.minecraft.world.entity.animal.feline.CatVariants;
import net.minecraft.world.entity.monster.Phantom;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.JukeboxBlock;
import net.minecraft.world.level.block.entity.JukeboxBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.EventHooks;
import net.neoforged.neoforge.event.entity.EntityJoinLevelEvent;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;
import net.neoforged.neoforge.event.tick.EntityTickEvent;

/**
 * Town cats are vanilla cats (taming, sitting, breeding, gifts and creeper/phantom scaring all still work) with a
 * {@link TownCat} attachment and a few extra goals:
 * <ul>
 * <li><b>Settling:</b> a blueprint cat rolls its presence, coat and accessory (TownCatOdds) the first time it joins the
 *     world, and gets a home it drifts back to. Town cats are used to people and don't run from players.</li>
 * <li><b>Goals</b> (added to every cat, but they only act for untamed town cats): follow the Sheriff (his cat), gather
 *     at a fire at dusk, lounge on perches, return home, and stalk rattlesnakes (livery and house cats).</li>
 * <li><b>Brass star:</b> a cat wearing it (the sheriff's, or any you pin it on) hisses at free outlaws and two-star
 *     wanted players nearby and makes them glow for a few seconds. It never attacks.</li>
 * <li><b>Ghost cat</b> (the undertaker's, rare): fades out by day and when a player sprints at it; tamed only with
 *     cooked fish while music plays nearby. Tamed, it stays pale and keeps phantoms away from a wide area.</li>
 * <li><b>Dressing:</b> right-click your own grown cat with a bandana, cowboy hat or brass star to put it on (swapping
 *     out what it wore); sneak-right-click with an empty hand to take it back.</li>
 * </ul>
 */
@EventBusSubscriber(modid = HollowFrontier.MODID)
public final class TownCats {
    private TownCats() {}

    public static final int HOME_RADIUS = 16;
    public static final double STAR_RANGE = 8.0;
    public static final int GLOW_TICKS = 120;
    public static final double PHANTOM_RANGE = 48.0;
    public static final int MUSIC_RANGE = 16;
    private static final double SHY_RANGE = 12.0;
    private static final int SHY_TICKS = 200;

    // Game time until which a ghost cat stays faded after being rushed (server side, not saved).
    private static final Map<Cat, Long> SHY_UNTIL = new WeakHashMap<>();

    // ---------------------------------------------------------------- spawning

    @SubscribeEvent
    static void onJoin(EntityJoinLevelEvent event) {
        if (!(event.getEntity() instanceof Cat cat) || !(event.getLevel() instanceof ServerLevel level)) {
            return;
        }
        if (cat.hasData(ModAttachments.TOWN_CAT.get()) && !TownCat.of(cat).settled() && !settle(cat, level)) {
            event.setCanceled(true); // this spot stays empty
            return;
        }
        addGoals(cat);
    }

    /** Rolls a blueprint cat's presence, coat and accessory and gives it a home. False if the spot gets no cat. */
    public static boolean settle(Cat cat, ServerLevel level) {
        Role role = TownCat.of(cat).role();
        RandomSource random = level.getRandom();
        if (random.nextFloat() >= TownCatOdds.presence(role)) {
            return false;
        }
        boolean ghost = role == Role.GHOST;
        boolean desertTown = level.getBiome(cat.blockPosition()).is(ModSpawns.HAS_RATTLESNAKES);
        // The ghost is drawn pale by the client whatever its coat; white is what its kittens would inherit.
        level.registryAccess().lookupOrThrow(Registries.CAT_VARIANT).get(ghost ? CatVariants.WHITE : TownCatOdds.coat(role, desertTown, random))
                .ifPresent(coat -> cat.setComponent(DataComponents.CAT_VARIANT, coat));
        TownCat.set(cat, new TownCat(role, TownCatOdds.accessory(role, random), ghost, true, false));
        cat.setHomeTo(cat.blockPosition(), HOME_RADIUS);
        cat.setPersistenceRequired();
        return true;
    }

    private static void addGoals(Cat cat) {
        if (cat.goalSelector.getAvailableGoals().stream().anyMatch(g -> g.getGoal() instanceof CatLoungeGoal)) {
            return;
        }
        if (TownCat.isTownCat(cat)) {
            cat.goalSelector.removeAllGoals(goal -> goal instanceof AvoidEntityGoal); // used to people
        }
        cat.goalSelector.addGoal(5, new SheriffCatGoal(cat));
        cat.goalSelector.addGoal(5, new CatHearthGoal(cat));
        cat.goalSelector.addGoal(7, new CatLoungeGoal(cat));
        cat.goalSelector.addGoal(11, new MoveTowardsRestrictionGoal(cat, 0.8));
        // Now and then (a long random interval) a barn or house cat goes after a snake near home.
        cat.targetSelector.addGoal(1, new NearestAttackableTargetGoal<>(cat, Rattlesnake.class, 300, true, false,
                (snake, level) -> isSnakeHunter(cat) && (!cat.hasHome() || cat.isWithinHome(snake.blockPosition()))));
    }

    private static boolean isSnakeHunter(Cat cat) {
        Role role = TownCat.of(cat).role();
        return TownCat.isTownCat(cat) && (role == Role.LIVERY || role == Role.HOME);
    }

    // ---------------------------------------------------------------- ticking

    @SubscribeEvent
    static void onTick(EntityTickEvent.Post event) {
        if (event.getEntity() instanceof Cat cat && cat.level() instanceof ServerLevel level && cat.tickCount % 10 == 0
                && cat.hasData(ModAttachments.TOWN_CAT.get())) {
            update(cat, level);
        }
    }

    /** Every 10 ticks for cats with frontier data. Public so game tests can drive it. */
    public static void update(Cat cat, ServerLevel level) {
        TownCat data = TownCat.of(cat);
        if (cat.isTame() && cat.hasHome()) {
            cat.clearHome(); // taken home by a player: it follows them now
        }
        if (data.accessory() == Accessory.SHERIFF_STAR && cat.tickCount % 40 == 0) {
            spotOutlaws(cat, level);
        }
        if (data.ghost()) {
            haunt(cat, level, data);
        }
    }

    /** Hisses at free outlaws and two-star wanted players in sight and makes them glow, so the law can spot them. */
    public static void spotOutlaws(Cat cat, ServerLevel level) {
        List<LivingEntity> suspects = level.getEntitiesOfClass(LivingEntity.class, cat.getBoundingBox().inflate(STAR_RANGE), e -> e.isAlive()
                && (e instanceof AbstractOutlaw outlaw && !outlaw.isJailed() || e instanceof Player p && !p.isSpectator() && !p.isCreative() && Wanted.stars(p) >= 2)
                && !e.hasEffect(MobEffects.GLOWING) && cat.hasLineOfSight(e));
        if (suspects.isEmpty()) {
            return;
        }
        cat.hiss();
        cat.getLookControl().setLookAt(suspects.getFirst());
        for (LivingEntity suspect : suspects) {
            suspect.addEffect(new MobEffectInstance(MobEffects.GLOWING, GLOW_TICKS, 0, false, false), cat); // outline only, no swirl
        }
    }

    private static void haunt(Cat cat, ServerLevel level, TownCat data) {
        if (cat.isTame()) {
            if (data.faded()) {
                TownCat.set(cat, data.withFaded(false));
            }
            if (cat.tickCount % 20 == 0) {
                for (Phantom phantom : level.getEntitiesOfClass(Phantom.class, cat.getBoundingBox().inflate(PHANTOM_RANGE), p -> p.getTarget() != null)) {
                    phantom.setTarget(null);
                }
            }
            return;
        }
        long now = level.getGameTime();
        if (SHY_UNTIL.getOrDefault(cat, 0L) <= now && rushedBySprinter(cat, level)) {
            SHY_UNTIL.put(cat, now + SHY_TICKS);
        }
        // Staged (NoAI) ghosts, like the photo booth's, always show.
        boolean fade = !cat.isNoAi() && (level.isBrightOutside() || SHY_UNTIL.getOrDefault(cat, 0L) > now);
        if (fade != data.faded()) {
            TownCat.set(cat, data.withFaded(fade));
            level.sendParticles(ParticleTypes.SOUL, cat.getX(), cat.getY() + 0.4, cat.getZ(), 6, 0.25, 0.3, 0.25, 0.02);
            cat.playSound(ModSounds.GHOST_CAT_FADE.get(), 0.8F, fade ? 1.0F : 1.3F);
        }
        if (fade) {
            cat.getNavigation().stop();
            cat.setInSittingPose(true);
        } else if (data.faded()) {
            cat.setInSittingPose(false);
        }
    }

    // A player sprinting this way, looking at the cat.
    private static boolean rushedBySprinter(Cat cat, ServerLevel level) {
        for (Player player : level.getEntitiesOfClass(Player.class, cat.getBoundingBox().inflate(SHY_RANGE), p -> p.isSprinting() && !p.isSpectator())) {
            Vec3 toCat = cat.position().subtract(player.position()).normalize();
            if (player.getLookAngle().dot(toCat) > 0.7) {
                return true;
            }
        }
        return false;
    }

    /** A playing Saloon Piano or jukebox within MUSIC_RANGE: the ghost cat's taming ritual needs one. */
    public static boolean musicNearby(Level level, BlockPos pos) {
        for (BlockPos at : BlockPos.betweenClosed(pos.offset(-MUSIC_RANGE, -6, -MUSIC_RANGE), pos.offset(MUSIC_RANGE, 6, MUSIC_RANGE))) {
            BlockState state = level.getBlockState(at);
            if (state.is(ModBlocks.SALOON_PIANO.get()) && state.getValue(SaloonPianoBlock.PLAYING)) {
                return true;
            }
            if (state.getBlock() instanceof JukeboxBlock && state.getValue(JukeboxBlock.HAS_RECORD)
                    && level.getBlockEntity(at) instanceof JukeboxBlockEntity jukebox && jukebox.getSongPlayer().isPlaying()) {
                return true;
            }
        }
        return false;
    }

    // A faded ghost can't be hurt (it isn't really there), short of /kill.
    @SubscribeEvent
    static void onDamage(LivingIncomingDamageEvent event) {
        if (event.getEntity() instanceof Cat cat && TownCat.of(cat).faded() && !event.getSource().is(DamageTypeTags.BYPASSES_INVULNERABILITY)) {
            event.setCanceled(true);
        }
    }

    // ---------------------------------------------------------------- interaction

    @SubscribeEvent
    static void onInteract(PlayerInteractEvent.EntityInteract event) {
        if (event.getTarget() instanceof Cat cat) {
            InteractionResult result = interact(cat, event.getEntity(), event.getHand(), event.getItemStack());
            if (result != null) {
                event.setCanceled(true);
                event.setCancellationResult(result);
            }
        }
    }

    /** Runs on both sides (only the server changes anything). Null leaves the click to vanilla. */
    public static @Nullable InteractionResult interact(Cat cat, Player player, InteractionHand hand, ItemStack stack) {
        TownCat data = TownCat.of(cat);
        boolean server = !player.level().isClientSide();
        if (data.ghost()) {
            if (data.faded()) {
                return InteractionResult.FAIL;
            }
            if (!cat.isTame()) {
                if (cat.isFood(stack)) {
                    if (server) say(player, "ghost_cat.raw");
                    return InteractionResult.SUCCESS;
                }
                if (stack.is(Items.COOKED_COD) || stack.is(Items.COOKED_SALMON)) {
                    if (server) offerGhost(cat, player, stack, data);
                    return InteractionResult.SUCCESS;
                }
                return null;
            }
            if (Accessory.of(stack) != Accessory.NONE && cat.isOwnedBy(player)) {
                if (server) say(player, "ghost_cat.no_dress");
                return InteractionResult.SUCCESS;
            }
            return null;
        }
        if (!cat.isTame() && data.role() == Role.SHERIFF && cat.isFood(stack) && SheriffCatGoal.findSheriff(cat) != null) {
            if (server) say(player, "sheriff_cat.loyal");
            return InteractionResult.SUCCESS;
        }
        if (!cat.isTame() || !cat.isOwnedBy(player) || cat.isBaby() || hand != InteractionHand.MAIN_HAND) {
            return null;
        }
        if (Accessory.of(stack) != Accessory.NONE) {
            if (server) {
                ItemStack old = data.worn();
                TownCat.set(cat, data.wearing(stack.copyWithCount(1)));
                stack.consume(1, player);
                player.getInventory().placeItemBackInInventory(old, Prediction.SERVER_ONLY);
                cat.playSound(SoundEvents.ARMOR_EQUIP_LEATHER.value(), 1.0F, 1.2F);
                ModCriteria.fire(player, ModCriteria.CAT_DRESSED);
            }
            return InteractionResult.SUCCESS;
        }
        if (player.isSecondaryUseActive() && stack.isEmpty() && !data.worn().isEmpty()) {
            if (server) {
                player.getInventory().placeItemBackInInventory(data.worn().copy(), Prediction.SERVER_ONLY);
                TownCat.set(cat, data.wearing(ItemStack.EMPTY));
                cat.playSound(SoundEvents.ARMOR_EQUIP_LEATHER.value(), 1.0F, 0.9F);
            }
            return InteractionResult.SUCCESS;
        }
        return null;
    }

    // Cooked fish with music playing nearby: the usual one-in-three taming chance. Without music it's ignored.
    private static void offerGhost(Cat cat, Player player, ItemStack stack, TownCat data) {
        if (!musicNearby(player.level(), cat.blockPosition())) {
            say(player, "ghost_cat.no_music");
            return;
        }
        stack.consume(1, player);
        cat.setPersistenceRequired();
        if (cat.getRandom().nextInt(3) == 0 && !EventHooks.onAnimalTame(cat, player)) {
            cat.tame(player);
            cat.setOrderedToSit(true);
            cat.clearHome();
            TownCat.set(cat, data.withFaded(false));
            cat.level().broadcastEntityEvent(cat, (byte) 7); // hearts
            ModCriteria.fire(player, ModCriteria.GHOST_CAT_TAMED);
        } else {
            cat.level().broadcastEntityEvent(cat, (byte) 6); // smoke
        }
    }

    private static void say(Player player, String key) {
        player.sendOverlayMessage(Component.translatable("message.hollowfrontier." + key));
    }
}
