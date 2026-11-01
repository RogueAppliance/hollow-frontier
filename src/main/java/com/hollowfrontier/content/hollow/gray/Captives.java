package com.hollowfrontier.content.hollow.gray;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.jspecify.annotations.Nullable;

import com.hollowfrontier.HollowFrontier;
import com.hollowfrontier.content.frontier.bounty.BoardPosting;
import com.hollowfrontier.content.frontier.bounty.BoardUse;
import com.hollowfrontier.content.frontier.bounty.BountyBoards;
import com.hollowfrontier.content.frontier.bounty.BountyData;
import com.hollowfrontier.content.frontier.law.Wanted;
import com.hollowfrontier.content.frontier.outlaw.AbstractOutlaw;
import com.hollowfrontier.content.frontier.raid.TownRaids;
import com.hollowfrontier.content.frontier.raid.TownSite;
import com.hollowfrontier.content.frontier.talk.Chatter;
import com.hollowfrontier.content.frontier.talk.Moment;
import com.hollowfrontier.content.frontier.townsfolk.Lawman;
import com.hollowfrontier.content.ufo.GrayWarden;
import com.hollowfrontier.registry.ModAttachments;
import com.hollowfrontier.registry.ModCriteria;
import com.hollowfrontier.registry.ModDataComponents;
import com.hollowfrontier.registry.ModEntities;
import com.hollowfrontier.registry.ModItems;
import com.hollowfrontier.worldgen.structure.ModStructures;
import com.mojang.datafixers.util.Pair;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderSet;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.ConversionParams;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.Relative;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.entity.EntityTypeTest;
import net.minecraft.world.level.levelgen.structure.Structure;
import net.minecraft.world.level.portal.TeleportTransition;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.EntityJoinLevelEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;

/**
 * The captives (20): the frontier folk the Grays hold in their pens, deep in the Hollow Earth, before they're fitted with
 * implants and sent back up as outlaws. A captive is one of the frontier's own mobs (an outlaw, now and then a
 * townsperson or a lawman) carrying a {@link Captive} attachment; while held they don't move or fight (no AI) and have no
 * gun. The pens' blueprint writes the attachment, and the {@link ImplantRigBlockEntity rig} runs the pen.
 * <ul>
 * <li><b>Freeing one</b> ({@link #free}): open the cell and use them. They tell their story (Chatter, {@link Moment#RESCUED}),
 *     hand over a {@link LetterHome Letter Home} for the town they were taken from, and walk off (an outlaw as a freed
 *     man, 13d). Jailbreak. The one on the table can't be freed while the rig's at work on him.</li>
 * <li><b>Letters</b> ({@link #deliverLetter}): used on that town's Bounty Board or sheriff, a letter pays
 *     {@link #LETTER_REWARD} gold and wipes the bringer's heat, if any. Special Delivery.</li>
 * <li><b>Brought in alive</b>: a hunter carrying an unclaimed Wanted Poster may find its man in a cell
 *     ({@link #recognise}). Freed by that hunter, he goes with them as a prisoner ({@link FollowCaptorGoal}, across
 *     worlds too), and at the board or sheriff of the town that posted him ({@link #turnIn}) the poster pays in full and
 *     the board's is stamped CAPTURED. ...Or Alive.</li>
 * </ul>
 */
@EventBusSubscriber(modid = HollowFrontier.MODID)
public final class Captives {
    private Captives() {}

    /** The Wardens working the rig: every one down stops the procedure. */
    public static final String SURGEON = "hollowfrontier.surgeon";
    /** Every Warden keeping a pen (surgeons too): after the leash, every one down clears it. */
    public static final String GUARD = "hollowfrontier.pens_guard";
    public static final int LETTER_REWARD = 3;
    /** How near the board or sheriff a prisoner has to be brought. */
    public static final double ESCORT_RANGE = 12.0;
    /** How far a prisoner lags before he's caught up (he was walked, not left). */
    public static final double CATCH_UP = 20.0;

    // ---------------------------------------------------------------- who's held

    public static @Nullable Captive of(Entity entity) {
        return entity.hasData(ModAttachments.CAPTIVE.get()) ? entity.getData(ModAttachments.CAPTIVE.get()) : null;
    }

    /** Held in a pen (not a prisoner being walked home). */
    public static boolean isHeld(Entity entity) {
        Captive captive = of(entity);
        return captive != null && captive.held();
    }

    public static boolean onTable(Entity entity) {
        Captive captive = of(entity);
        return captive != null && captive.held() && captive.table();
    }

    /** A prisoner this player is walking back to town. */
    public static boolean isPrisonerOf(Entity entity, Player player) {
        Captive captive = of(entity);
        return captive != null && captive.captor().filter(player.getUUID()::equals).isPresent();
    }

    public static List<Mob> heldNear(ServerLevel level, BlockPos pos, double range) {
        return level.getEntitiesOfClass(Mob.class, new AABB(pos).inflate(range), m -> m.isAlive() && isHeld(m));
    }

    public static List<GrayWarden> surgeons(ServerLevel level, BlockPos pos, double range) {
        return level.getEntitiesOfClass(GrayWarden.class, new AABB(pos).inflate(range), w -> w.isAlive() && w.entityTags().contains(SURGEON));
    }

    public static List<GrayWarden> guards(ServerLevel level, BlockPos pos, double range) {
        return level.getEntitiesOfClass(GrayWarden.class, new AABB(pos).inflate(range), w -> w.isAlive() && w.entityTags().contains(GUARD));
    }

    /** Held: still, unarmed, kept. (Run each time one joins a world: the pens' outlaws come armed from their spawn.) */
    public static void restrain(Mob mob) {
        mob.setNoAi(true);
        mob.setTarget(null);
        mob.setPersistenceRequired();
        if (mob instanceof AbstractOutlaw || mob instanceof Lawman) {
            mob.setItemSlot(EquipmentSlot.MAINHAND, ItemStack.EMPTY);
        }
    }

    @SubscribeEvent
    static void onJoin(EntityJoinLevelEvent event) {
        if (!event.getLevel().isClientSide() && event.getEntity() instanceof Mob mob && isHeld(mob)) {
            restrain(mob);
        }
    }

    // ---------------------------------------------------------------- using one

    @SubscribeEvent(priority = EventPriority.HIGH)
    static void onInteract(PlayerInteractEvent.EntityInteract event) {
        // Server only: the attachment isn't synced, so the client can't tell a captive from anyone else
        if (!(event.getLevel() instanceof ServerLevel level) || !(event.getEntity() instanceof ServerPlayer player)) {
            return;
        }
        Entity target = event.getTarget();
        boolean handled = false;
        if (target instanceof Mob mob && isHeld(mob)) {
            free(level, player, mob);
            handled = true;
        } else if (target instanceof Lawman lawman && lawman.getRank() == Lawman.Rank.SHERIFF) {
            handled = atTown(level, player, player.getItemInHand(event.getHand()), lawman.blockPosition(),
                    BountyBoards.keyFor(level, lawman.blockPosition()));
        }
        if (handled) {
            event.setCancellationResult(InteractionResult.SUCCESS);
            event.setCanceled(true);
        }
    }

    /**
     * A player at a town's board or sheriff (at {@code pos}, the board list {@code key}) with a letter in hand, or with a
     * prisoner of theirs near: delivered, or told why not. Whether there was anything of the captives' to do.
     */
    public static boolean atTown(ServerLevel level, ServerPlayer player, ItemStack held, BlockPos pos, String key) {
        if (held.is(ModItems.LETTER_HOME.get())) {
            deliverLetter(level, player, held, pos);
            return true;
        }
        return turnIn(level, player, pos, key) != 0;
    }

    /** {@code player} uses a held captive: what came of it ({@code strapped}, {@code escorted} or {@code freed}). */
    public static String free(ServerLevel level, ServerPlayer player, Mob mob) {
        Captive captive = of(mob);
        if (captive == null) {
            return "none";
        }
        if (captive.table() && ImplantRigBlockEntity.operatingOn(level, mob)) {
            say(player, mob, Component.translatable("message.hollowfrontier.captive.strapped"));
            return "strapped";
        }
        if (mob instanceof AbstractOutlaw outlaw && captive.bounty().isPresent() && posterFor(player, captive.bounty().get().id()) != null) {
            escort(level, player, outlaw, captive);
            return "escorted";
        }
        Chatter.say(mob, player, Moment.RESCUED);
        mob.removeData(ModAttachments.CAPTIVE.get());
        mob.setNoAi(false);
        if (mob instanceof AbstractOutlaw outlaw) {
            outlaw.setFreed();
        }
        ItemStack letter = new ItemStack(ModItems.LETTER_HOME.get());
        letter.set(ModDataComponents.LETTER_HOME.get(), homeTown(level, mob.getRandom()));
        say(player, mob, Component.translatable("message.hollowfrontier.captive.letter"));
        BoardUse.give(player, letter);
        level.playSound(null, mob.blockPosition(), SoundEvents.CHAIN_BREAK, SoundSource.NEUTRAL, 1.0F, 1.0F);
        level.sendParticles(ParticleTypes.HAPPY_VILLAGER, mob.getX(), mob.getY(1.0), mob.getZ(), 8, 0.3, 0.4, 0.3, 0);
        ModCriteria.fire(player, ModCriteria.CAPTIVE_FREED);
        return "freed";
    }

    /** The man on the hunter's poster: he goes with them, in irons. */
    private static void escort(ServerLevel level, ServerPlayer player, AbstractOutlaw outlaw, Captive captive) {
        outlaw.setData(ModAttachments.CAPTIVE.get(), captive.escortedBy(player.getUUID()));
        outlaw.setNoAi(false);
        outlaw.setJailed();
        say(player, outlaw, Component.translatable("message.hollowfrontier.captive.escort"));
        player.sendSystemMessage(Component.translatable("message.hollowfrontier.captive.escort_hint", outlaw.getDisplayName())
                .withStyle(ChatFormatting.GRAY, ChatFormatting.ITALIC));
        level.playSound(null, outlaw.blockPosition(), SoundEvents.CHAIN_PLACE, SoundSource.NEUTRAL, 1.0F, 1.0F);
        ModCriteria.fire(player, ModCriteria.CAPTIVE_FREED);
    }

    /**
     * A hunter near a pen: if they carry a poster no gang has turned up for yet, its man turns out to be in one of the
     * cells (an outlaw not yet claimed by another poster). One poster a call. The name on the poster, if one was matched.
     */
    public static @Nullable String recognise(ServerLevel level, ServerPlayer player, BlockPos pen) {
        for (ItemStack stack : player.getInventory()) {
            BountyData bounty = stack.is(ModItems.WANTED_POSTER.get()) ? stack.get(ModDataComponents.BOUNTY.get()) : null;
            if (bounty == null || bounty.spawned()) {
                continue;
            }
            Optional<AbstractOutlaw> man = level.getEntitiesOfClass(AbstractOutlaw.class, new AABB(pen).inflate(ImplantRigBlockEntity.CAPTIVE_RANGE),
                    o -> o.isAlive() && isHeld(o) && !of(o).table() && of(o).bounty().isEmpty()).stream().findFirst();
            if (man.isEmpty()) {
                return null;
            }
            BountyData found = bounty.markCaptive();
            man.get().setData(ModAttachments.CAPTIVE.get(), of(man.get()).withBounty(found));
            man.get().setCustomName(Component.literal(found.target()));
            stack.set(ModDataComponents.BOUNTY.get(), found);
            player.sendSystemMessage(Component.translatable("message.hollowfrontier.captive.recognised", found.target()).withStyle(ChatFormatting.GOLD));
            return found.target();
        }
        return null;
    }

    /** The procedure ran its course: the captive gets up implanted, an outlaw with a gun, and hostile. */
    public static @Nullable AbstractOutlaw implant(ServerLevel level, Mob mob) {
        mob.removeData(ModAttachments.CAPTIVE.get());
        mob.setNoAi(false);
        AbstractOutlaw outlaw = mob instanceof AbstractOutlaw o ? o
                : mob.convertTo(ModEntities.OUTLAW.get(), ConversionParams.single(mob, false, false), o -> {});
        if (outlaw == null) {
            return null;
        }
        outlaw.arm();
        outlaw.setPersistenceRequired();
        level.sendParticles(ParticleTypes.ELECTRIC_SPARK, outlaw.getX(), outlaw.getY(1.0), outlaw.getZ(), 20, 0.3, 0.3, 0.3, 0.1);
        level.playSound(null, outlaw.blockPosition(), SoundEvents.ZOMBIE_VILLAGER_CURE, SoundSource.HOSTILE, 1.0F, 0.6F);
        if (level.getNearestPlayer(outlaw, 32.0) instanceof ServerPlayer near && !near.isCreative() && !near.isSpectator()) {
            outlaw.setTarget(near);
        }
        return outlaw;
    }

    // ---------------------------------------------------------------- letters and prisoners in town

    /** A Letter Home brought to a board or sheriff at {@code pos}. Whether it was taken. */
    public static boolean deliverLetter(ServerLevel level, ServerPlayer player, ItemStack letter, BlockPos pos) {
        return deliverLetter(level, player, letter, pos, TownSite.townAt(level, pos).map(TownSite::keyOf));
    }

    /** As above, in the town under {@code town} (if any). Public so game tests can name the town. */
    public static boolean deliverLetter(ServerLevel level, ServerPlayer player, ItemStack letter, BlockPos pos, Optional<Long> town) {
        LetterHome home = letter.getOrDefault(ModDataComponents.LETTER_HOME.get(), LetterHome.ANY);
        if (town.isEmpty()) {
            player.sendOverlayMessage(Component.translatable("message.hollowfrontier.letter_home.no_town"));
            return false;
        }
        if (!home.isFor(town.get())) {
            player.sendOverlayMessage(Component.translatable("message.hollowfrontier.letter_home.wrong_town", home.x(), home.z()));
            return false;
        }
        letter.shrink(1);
        BoardUse.give(player, new ItemStack(ModItems.GOLD_COIN.get(), LETTER_REWARD));
        player.sendSystemMessage(Component.translatable("message.hollowfrontier.letter_home.delivered", LETTER_REWARD).withStyle(ChatFormatting.GOLD));
        if (Wanted.of(player).heat() > 0) {
            Wanted.clear(player);
            player.sendSystemMessage(Component.translatable("message.hollowfrontier.letter_home.pardoned").withStyle(ChatFormatting.GREEN));
        }
        level.playSound(null, pos, SoundEvents.VILLAGER_CELEBRATE, SoundSource.NEUTRAL, 1.0F, 1.0F);
        ModCriteria.fire(player, ModCriteria.LETTER_DELIVERED);
        return true;
    }

    /**
     * Prisoners this player walked to a board or sheriff at {@code pos} (board list {@code key}): each one posted here (or
     * on no board any more) is paid for in full, locked up, and his poster stamped CAPTURED. How many were near (paid or not).
     */
    public static int turnIn(ServerLevel level, ServerPlayer player, BlockPos pos, String key) {
        List<AbstractOutlaw> prisoners = level.getEntitiesOfClass(AbstractOutlaw.class, new AABB(pos).inflate(ESCORT_RANGE),
                o -> o.isAlive() && isPrisonerOf(o, player));
        for (AbstractOutlaw prisoner : prisoners) {
            BountyData bounty = of(prisoner).bounty().orElse(null);
            if (bounty == null) {
                continue;
            }
            BountyBoards boards = BountyBoards.get(level);
            Optional<String> postedAt = boards.keyHolding(bounty.id());
            if (postedAt.isPresent() && !postedAt.get().equals(key)) {
                player.sendOverlayMessage(Component.translatable("message.hollowfrontier.captive.wrong_town", bounty.target()));
                continue;
            }
            BoardUse.removePoster(player.getInventory(), bounty.id());
            BoardUse.give(player, new ItemStack(ModItems.GOLD_COIN.get(), bounty.reward()));
            boards.close(bounty.id(), BoardPosting.Status.CAPTURED);
            player.sendSystemMessage(Component.translatable("message.hollowfrontier.captive.turned_in", bounty.target(), bounty.reward()).withStyle(ChatFormatting.GOLD));
            level.playSound(null, pos, SoundEvents.PLAYER_LEVELUP, SoundSource.NEUTRAL, 0.8F, 1.2F);
            level.sendParticles(ParticleTypes.POOF, prisoner.getX(), prisoner.getY(0.5), prisoner.getZ(), 8, 0.3, 0.5, 0.3, 0.02);
            prisoner.discard();
            ModCriteria.fire(player, ModCriteria.BOUNTY_CLAIMED);
            ModCriteria.fire(player, ModCriteria.BROUGHT_IN_ALIVE);
        }
        return prisoners.size();
    }

    // ---------------------------------------------------------------- where they came from

    /**
     * The town a captive was taken from: one of the towns the world knows (a board used there, or a raid), else the town
     * nearest the world's spawn, else any town.
     */
    public static LetterHome homeTown(ServerLevel level, RandomSource random) {
        ServerLevel overworld = level.getServer().overworld();
        List<Long> known = new ArrayList<>(BountyBoards.get(overworld).towns());
        TownRaids.get(overworld).knownTowns().stream().filter(k -> !known.contains(k)).forEach(known::add);
        if (!known.isEmpty()) {
            return letterTo(overworld, known.get(random.nextInt(known.size())));
        }
        var structures = overworld.registryAccess().lookupOrThrow(Registries.STRUCTURE);
        List<Holder<Structure>> towns = ModStructures.TOWNS.stream().flatMap(k -> structures.get(k).stream()).map(h -> (Holder<Structure>) h).toList();
        Pair<BlockPos, Holder<Structure>> nearest = towns.isEmpty() ? null : overworld.getChunkSource().getGenerator()
                .findNearestMapStructure(overworld, HolderSet.direct(towns), overworld.getRespawnData().pos(), 64, false);
        return nearest == null ? LetterHome.ANY : letterTo(overworld, ChunkPos.containing(nearest.getFirst()).pack());
    }

    private static LetterHome letterTo(ServerLevel overworld, long town) {
        ChunkPos chunk = ChunkPos.unpack(town);
        return new LetterHome(town, chunk.getMiddleBlockX(), chunk.getMiddleBlockZ(), com.hollowfrontier.worldgen.structure.TownNames.name(overworld, town));
    }

    // ---------------------------------------------------------------- walking a prisoner home

    /** A prisoner keeps up with the hunter walking him in. */
    public static final class FollowCaptorGoal extends Goal {
        private final AbstractOutlaw prisoner;
        private @Nullable Player captor;

        public FollowCaptorGoal(AbstractOutlaw prisoner) {
            this.prisoner = prisoner;
            setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK));
        }

        private @Nullable Player captor() {
            Captive captive = of(prisoner);
            if (captive == null || captive.captor().isEmpty()) {
                return null;
            }
            Player player = prisoner.level().getPlayerByUUID(captive.captor().get());
            return player != null && player.isAlive() && !player.isSpectator() ? player : null;
        }

        @Override
        public boolean canUse() {
            captor = captor();
            return captor != null && prisoner.distanceToSqr(captor) > 9.0;
        }

        @Override
        public boolean canContinueToUse() {
            return captor != null && captor.isAlive() && prisoner.distanceToSqr(captor) > 4.0;
        }

        @Override
        public void stop() {
            captor = null;
            prisoner.getNavigation().stop();
        }

        @Override
        public void tick() {
            if (captor == null) {
                return;
            }
            prisoner.getLookControl().setLookAt(captor, 10.0F, prisoner.getMaxHeadXRot());
            if (prisoner.distanceToSqr(captor) > CATCH_UP * CATCH_UP) {
                prisoner.snapTo(captor.getX(), captor.getY(), captor.getZ(), prisoner.getYRot(), prisoner.getXRot());
                prisoner.getNavigation().stop();
            } else {
                prisoner.getNavigation().moveTo(captor, 1.1);
            }
        }
    }

    /** A hunter changing worlds (a gateway, a waystone): their prisoners go with them. */
    @SubscribeEvent
    static void onChangedDimension(PlayerEvent.PlayerChangedDimensionEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player) || !(player.level() instanceof ServerLevel there)) {
            return;
        }
        ServerLevel from = there.getServer().getLevel(event.getFrom());
        if (from == null) {
            return;
        }
        for (AbstractOutlaw prisoner : from.getEntities(EntityTypeTest.forClass(AbstractOutlaw.class), o -> o.isAlive() && isPrisonerOf(o, player))) {
            prisoner.teleport(new TeleportTransition(there, player.position(), Vec3.ZERO, prisoner.getYRot(), prisoner.getXRot(),
                    Relative.DELTA, TeleportTransition.DO_NOTHING));
        }
    }

    private static @Nullable ItemStack posterFor(Player player, UUID bounty) {
        for (ItemStack stack : player.getInventory()) {
            BountyData data = stack.get(ModDataComponents.BOUNTY.get());
            if (stack.is(ModItems.WANTED_POSTER.get()) && data != null && data.id().equals(bounty)) {
                return stack;
            }
        }
        return null;
    }

    private static void say(ServerPlayer player, Entity speaker, Component line) {
        player.sendSystemMessage(Component.translatable("message.hollowfrontier.npc.say", speaker.getDisplayName(), line));
    }
}
