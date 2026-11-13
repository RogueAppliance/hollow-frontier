package com.hollowfrontier.content.frontier.outlaw;

import java.util.Optional;
import java.util.UUID;

import com.hollowfrontier.content.frontier.bounty.BountyData;
import com.hollowfrontier.content.frontier.gun.GunStats;
import com.hollowfrontier.content.frontier.gun.GunTypes;
import com.hollowfrontier.registry.ModDataComponents;
import com.hollowfrontier.registry.ModItems;

import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerBossEvent;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.BossEvent;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;

/**
 * Named gang leader spawned by a Wanted Poster (see BountyEvents). Never despawns, shows a boss bar,
 * and drops an Outlaw Badge carrying its bounty, which pays out at a Bounty Board.
 */
public class OutlawBoss extends AbstractOutlaw {
    private final ServerBossEvent bossEvent = new ServerBossEvent(UUID.randomUUID(), Component.empty(),
            BossEvent.BossBarColor.YELLOW, BossEvent.BossBarOverlay.PROGRESS);
    private BountyData bounty;
    private boolean duelling;

    public OutlawBoss(EntityType<? extends OutlawBoss> type, Level level) {
        super(type, level);
        setPersistenceRequired();
        xpReward = 30;
    }

    public static AttributeSupplier.Builder createAttributes() {
        return createOutlawAttributes()
                .add(Attributes.MAX_HEALTH, 80.0)
                .add(Attributes.ARMOR, 6.0)
                .add(Attributes.KNOCKBACK_RESISTANCE, 0.5)
                .add(Attributes.MOVEMENT_SPEED, 0.32);
    }

    public Optional<BountyData> bounty() {
        return Optional.ofNullable(bounty);
    }

    public void setBounty(BountyData bounty) {
        this.bounty = bounty;
        setCustomName(Component.literal(bounty.target()));
        setCustomNameVisible(true);
    }

    public boolean isDuelling() {
        return duelling;
    }

    /**
     * High noon (51): in the street he stands still with a revolver, and the duel moves him ({@code Duels}). Set false
     * (or loaded from a save that caught him mid-duel), he's any boss again, shotgun and all.
     */
    public void setDuelling(boolean inTheStreet) {
        duelling = inTheStreet;
        setNoAi(inTheStreet);
        setItemSlot(EquipmentSlot.MAINHAND, inTheStreet ? new ItemStack(ModItems.REVOLVER.get()) : createGun());
        setDropChance(EquipmentSlot.MAINHAND, 0.0F);
        setGunPose(GunPose.LOWERED);
    }

    @Override
    public GunStats gunStats() {
        return duelling ? GunTypes.DUEL_REVOLVER : GunTypes.BOSS_SHOTGUN;
    }

    @Override
    protected ItemStack createGun() {
        return new ItemStack(ModItems.SHOTGUN.get());
    }

    @Override
    public boolean removeWhenFarAway(double distSqr) {
        return false;
    }

    @Override
    protected void customServerAiStep(ServerLevel level) {
        super.customServerAiStep(level);
        bossEvent.setName(getDisplayName());
        bossEvent.setProgress(getHealth() / getMaxHealth());
    }

    @Override
    public void startSeenByPlayer(ServerPlayer player) {
        super.startSeenByPlayer(player);
        bossEvent.setName(getDisplayName()); // also kept up to date by the AI, which a NoAI boss never runs
        bossEvent.addPlayer(player);
    }

    @Override
    public void stopSeenByPlayer(ServerPlayer player) {
        super.stopSeenByPlayer(player);
        bossEvent.removePlayer(player);
    }

    @Override
    protected void dropCustomDeathLoot(ServerLevel level, DamageSource source, boolean killedByPlayer) {
        super.dropCustomDeathLoot(level, source, killedByPlayer);
        if (bounty != null) {
            ItemStack badge = new ItemStack(ModItems.OUTLAW_BADGE.get());
            badge.set(ModDataComponents.BOUNTY.get(), bounty);
            spawnAtLocation(level, badge);
        }
    }

    @Override
    protected void addAdditionalSaveData(ValueOutput output) {
        super.addAdditionalSaveData(output);
        output.storeNullable("bounty", BountyData.CODEC, bounty);
        if (duelling) {
            output.putBoolean("duelling", true);
        }
    }

    @Override
    protected void readAdditionalSaveData(ValueInput input) {
        super.readAdditionalSaveData(input);
        input.read("bounty", BountyData.CODEC).ifPresent(this::setBounty);
        if (input.getBooleanOr("duelling", false)) {
            setDuelling(false); // the duel he was in went with the server
        }
    }

    public boolean hasBounty(UUID id) {
        return bounty != null && bounty.id().equals(id);
    }
}
