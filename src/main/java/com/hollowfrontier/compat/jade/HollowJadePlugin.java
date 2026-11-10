package com.hollowfrontier.compat.jade;

import com.hollowfrontier.HollowFrontier;
import com.hollowfrontier.content.steam.BoilerBlock;
import com.hollowfrontier.content.steam.BoilerBlockEntity;
import com.hollowfrontier.content.steam.CrusherBlock;
import com.hollowfrontier.content.steam.CrusherBlockEntity;
import com.hollowfrontier.content.steam.PressureGaugeBlock;
import com.hollowfrontier.content.steam.PressureGaugeBlockEntity;
import com.hollowfrontier.content.steam.SteamHandcar;
import com.hollowfrontier.content.steam.SteamNetwork;
import com.hollowfrontier.content.steam.airship.Airship;
import com.hollowfrontier.content.steam.submersible.SteamSubmersible;
import com.hollowfrontier.content.ufo.PrototypeSaucer;
import com.hollowfrontier.content.ufo.SaucerFittings;

import net.minecraft.ChatFormatting;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.Entity;
import snownee.jade.api.BlockAccessor;
import snownee.jade.api.EntityAccessor;
import snownee.jade.api.IBlockComponentProvider;
import snownee.jade.api.IEntityComponentProvider;
import snownee.jade.api.IServerDataProvider;
import snownee.jade.api.ITooltip;
import snownee.jade.api.IWailaClientRegistration;
import snownee.jade.api.IWailaCommonRegistration;
import snownee.jade.api.IWailaPlugin;
import snownee.jade.api.WailaPlugin;
import snownee.jade.api.config.IPluginConfig;

/**
 * Jade (optional): what's going on inside the steam machines (the boiler's fire, the crusher's progress, the gauge's
 * reading) and the vehicles (the airship's steam and coal, the handcar's steam, the saucer's phlogiston and fittings). Jade
 * already shows the tanks and slots it finds through capabilities. Only loaded by Jade.
 */
@WailaPlugin(HollowFrontier.MODID)
public class HollowJadePlugin implements IWailaPlugin {
    @Override
    public void register(IWailaCommonRegistration registration) {
        registration.registerBlockDataProvider(MachineData.INSTANCE, BoilerBlockEntity.class);
        registration.registerBlockDataProvider(MachineData.INSTANCE, CrusherBlockEntity.class);
        registration.registerBlockDataProvider(MachineData.INSTANCE, PressureGaugeBlockEntity.class);
        registration.registerEntityDataProvider(VehicleData.INSTANCE, Airship.class);
        registration.registerEntityDataProvider(VehicleData.INSTANCE, SteamSubmersible.class);
        registration.registerEntityDataProvider(VehicleData.INSTANCE, SteamHandcar.class);
        registration.registerEntityDataProvider(VehicleData.INSTANCE, PrototypeSaucer.class);
    }

    @Override
    public void registerClient(IWailaClientRegistration registration) {
        registration.registerBlockComponent(Machines.INSTANCE, BoilerBlock.class);
        registration.registerBlockComponent(Machines.INSTANCE, CrusherBlock.class);
        registration.registerBlockComponent(Machines.INSTANCE, PressureGaugeBlock.class);
        registration.registerEntityComponent(Vehicles.INSTANCE, Airship.class);
        registration.registerEntityComponent(Vehicles.INSTANCE, SteamSubmersible.class);
        registration.registerEntityComponent(Vehicles.INSTANCE, SteamHandcar.class);
        registration.registerEntityComponent(Vehicles.INSTANCE, PrototypeSaucer.class);
    }

    private static MutableComponent line(String key, Object... args) {
        return Component.translatable("jade." + HollowFrontier.MODID + "." + key, args);
    }

    // Since 1.21.6 Jade wants the server's data and the client's tooltip from separate providers

    /** Server side: what the machines don't sync to clients. */
    enum MachineData implements IServerDataProvider<BlockAccessor> {
        INSTANCE;

        @Override
        public Identifier getUid() {
            return HollowFrontier.id("steam_machine_data");
        }

        @Override
        public void appendServerData(CompoundTag data, BlockAccessor accessor) {
            switch (accessor.getBlockEntity()) {
                case BoilerBlockEntity boiler -> data.putInt("burn", boiler.burnTime());
                case CrusherBlockEntity crusher -> data.putInt("progress", crusher.progress());
                case PressureGaugeBlockEntity gauge -> {
                    SteamNetwork.Pressure reading = PressureGaugeBlockEntity.read(accessor.getLevel(), accessor.getPosition());
                    data.putInt("pressure", reading.capacity() <= 0 ? -1 : (int) Math.round(reading.fraction() * 100));
                }
                default -> {}
            }
        }
    }

    enum Machines implements IBlockComponentProvider {
        INSTANCE;

        @Override
        public Identifier getUid() {
            return HollowFrontier.id("steam_machine");
        }

        @Override
        public void appendTooltip(ITooltip tooltip, BlockAccessor accessor, IPluginConfig config) {
            CompoundTag data = accessor.getServerData();
            if (data.contains("burn")) {
                int burn = data.getIntOr("burn", 0);
                tooltip.add(burn > 0 ? line("boiler.burning", (burn + 19) / 20) : line("boiler.cold").withStyle(ChatFormatting.GRAY));
            } else if (data.contains("progress")) {
                int progress = data.getIntOr("progress", 0);
                if (progress > 0) {
                    tooltip.add(line("crusher.progress", progress * 100 / CrusherBlockEntity.CRUSH_TIME));
                }
            } else if (data.contains("pressure")) {
                int pressure = data.getIntOr("pressure", -1);
                tooltip.add(pressure < 0 ? Component.translatable("message.hollowfrontier.gauge.none").withStyle(ChatFormatting.GRAY)
                        : line("gauge", pressure));
            }
        }
    }

    /** Server side: the airship's coal and the saucer's charge and fittings aren't synced to clients. */
    enum VehicleData implements IServerDataProvider<EntityAccessor> {
        INSTANCE;

        @Override
        public Identifier getUid() {
            return HollowFrontier.id("vehicle_data");
        }

        @Override
        public void appendServerData(CompoundTag data, EntityAccessor accessor) {
            Entity entity = accessor.getEntity();
            switch (entity) {
                case Airship ship -> {
                    data.putInt("steam", ship.steam());
                    data.putInt("capacity", Airship.CAPACITY);
                    data.putInt("fuel", ship.fuel());
                }
                case SteamSubmersible sub -> {
                    data.putInt("steam", sub.steam());
                    data.putInt("capacity", SteamSubmersible.CAPACITY);
                    data.putInt("hull", Math.round(sub.hull() * 100 / SteamSubmersible.HULL));
                }
                case SteamHandcar car -> {
                    data.putInt("steam", car.steam());
                    data.putInt("capacity", SteamHandcar.CAPACITY);
                }
                case PrototypeSaucer saucer -> {
                    data.putInt("phlogiston", saucer.charge());
                    data.putInt("fittings", saucer.fittingsMask());
                }
                default -> {}
            }
        }
    }

    enum Vehicles implements IEntityComponentProvider {
        INSTANCE;

        @Override
        public Identifier getUid() {
            return HollowFrontier.id("vehicle");
        }

        @Override
        public void appendTooltip(ITooltip tooltip, EntityAccessor accessor, IPluginConfig config) {
            CompoundTag data = accessor.getServerData();
            if (data.contains("steam")) {
                tooltip.add(Component.translatable("tooltip.hollowfrontier.steam", data.getIntOr("steam", 0), data.getIntOr("capacity", 0)));
            }
            if (data.contains("hull")) {
                tooltip.add(line("submersible.hull", data.getIntOr("hull", 0)));
            }
            if (data.contains("fuel")) {
                tooltip.add(line("airship.coal", String.format(java.util.Locale.ROOT, "%.1f", data.getIntOr("fuel", 0) / (double) Airship.COAL_TICKS)));
            }
            if (data.contains("phlogiston")) {
                tooltip.add(Component.translatable("tooltip.hollowfrontier.prototype_saucer.charge", data.getIntOr("phlogiston", 0), PrototypeSaucer.MAX_CHARGE)
                        .withStyle(ChatFormatting.GOLD));
                SaucerFittings.fromMask(data.getIntOr("fittings", 0)).describe(tooltip::add);
            }
        }
    }
}
