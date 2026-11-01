package com.hollowfrontier.content.frontier.raid;

import java.util.Locale;
import java.util.Optional;
import java.util.UUID;

import org.jspecify.annotations.Nullable;

import com.hollowfrontier.registry.ModAttachments;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import net.minecraft.core.UUIDUtil;
import net.minecraft.util.StringRepresentable;
import net.minecraft.world.entity.Entity;

/**
 * Marks an outlaw or horse as riding with a raid (the RAID_MEMBER attachment, saved with the entity): which raid, what
 * job it has, and for riders the horse they came on. A member whose raid is over is removed when it next loads
 * (TownRaids), so a gang never outlives its raid.
 */
public record RaidMember(UUID raid, Role role, Optional<UUID> mount) {
    public static final RaidMember NONE = new RaidMember(new UUID(0, 0), Role.GUNSLINGER, Optional.empty());

    public enum Role implements StringRepresentable {
        /** The named boss (an OutlawBoss): rides at the back, calls the shots. Kill him and the gang breaks. */
        LEADER,
        /** Pins down the street with revolver or rifle. */
        GUNSLINGER,
        /** Lobs dynamite at lawmen and whoever's shooting. */
        DYNAMITER,
        /** Heads straight for the vault and works the lock. Carries the loot out once it's open. */
        SAFECRACKER,
        /** A gang horse. */
        HORSE;

        public static final Codec<Role> CODEC = StringRepresentable.fromEnum(Role::values);

        @Override
        public String getSerializedName() {
            return name().toLowerCase(Locale.ROOT);
        }
    }

    public static final Codec<RaidMember> CODEC = RecordCodecBuilder.create(i -> i.group(
            UUIDUtil.CODEC.fieldOf("raid").forGetter(RaidMember::raid),
            Role.CODEC.fieldOf("role").forGetter(RaidMember::role),
            UUIDUtil.CODEC.optionalFieldOf("mount").forGetter(RaidMember::mount)).apply(i, RaidMember::new));

    public static @Nullable RaidMember of(Entity entity) {
        return entity.hasData(ModAttachments.RAID_MEMBER.get()) ? entity.getData(ModAttachments.RAID_MEMBER.get()) : null;
    }

    public static boolean is(Entity entity, Role role) {
        RaidMember member = of(entity);
        return member != null && member.role == role;
    }

    public RaidMember withRole(Role newRole) {
        return new RaidMember(raid, newRole, mount);
    }
}
