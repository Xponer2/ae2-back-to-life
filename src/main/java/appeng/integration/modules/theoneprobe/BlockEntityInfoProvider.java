/*
 * This file is part of Applied Energistics 2.
 * Copyright (c) 2013 - 2014, AlgorithmX2, All rights reserved.
 *
 * Applied Energistics 2 is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Applied Energistics 2 is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with Applied Energistics 2.  If not, see <http://www.gnu.org/licenses/lgpl>.
 */

package appeng.integration.modules.theoneprobe;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

import net.minecraft.block.Block;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.world.World;

import mcjty.theoneprobe.api.IBlockDisplayOverride;
import mcjty.theoneprobe.api.IProbeHitData;
import mcjty.theoneprobe.api.IProbeInfo;
import mcjty.theoneprobe.api.IProbeInfoProvider;
import mcjty.theoneprobe.api.ProbeMode;
import mcjty.theoneprobe.api.TextStyleClass;

import appeng.api.integrations.igtooltip.ClientRegistration;
import appeng.api.integrations.igtooltip.CommonRegistration;
import appeng.api.integrations.igtooltip.TooltipBuilder;
import appeng.api.integrations.igtooltip.TooltipContext;
import appeng.api.integrations.igtooltip.providers.BodyProvider;
import appeng.api.integrations.igtooltip.providers.IconProvider;
import appeng.api.integrations.igtooltip.providers.ModNameProvider;
import appeng.api.integrations.igtooltip.providers.NameProvider;
import appeng.api.integrations.igtooltip.providers.ServerDataProvider;
import appeng.core.AppEng;
import appeng.integration.modules.igtooltip.TooltipProviders;
import appeng.util.Platform;

/**
 * Delegation provider for tiles through {@link mcjty.theoneprobe.api.IProbeInfoProvider}
 */
public final class BlockEntityInfoProvider implements IProbeInfoProvider, IBlockDisplayOverride {

    private final List<ServerDataCollector> dataCollectors = new ArrayList<>();

    private final List<BodyCustomizer<?>> bodyCustomizers = new ArrayList<>();
    private final List<NameCustomizer<?>> nameCustomizers = new ArrayList<>();
    private final List<ModNameCustomizer<?>> modNameCustomizers = new ArrayList<>();
    private final List<IconCustomizer<?>> iconCustomizers = new ArrayList<>();

    public BlockEntityInfoProvider() {
        TooltipProviders.loadCommon(new CommonRegistration() {
            @Override
            public <T extends TileEntity> void addBlockEntityData(ResourceLocation id, Class<T> blockEntityClass,
                    ServerDataProvider<? super T> provider) {
                dataCollectors.add((blockEntity, player, serverData) -> {
                    if (blockEntityClass.isInstance(blockEntity)) {
                        var obj = blockEntityClass.cast(blockEntity);
                        provider.provideServerData(player, obj, serverData);
                    }
                });
            }
        });
        TooltipProviders.loadClient(new ClientRegistration() {
            @Override
            public <T extends TileEntity> void addBlockEntityBody(Class<T> blockEntityClass,
                    Class<? extends Block> blockClass, ResourceLocation id, BodyProvider<? super T> provider,
                    int priority) {
                bodyCustomizers.add(new BodyCustomizer<>(blockEntityClass, provider, priority));
            }

            @Override
            public <T extends TileEntity> void addBlockEntityIcon(Class<T> blockEntityClass,
                    Class<? extends Block> blockClass, ResourceLocation id, IconProvider<? super T> provider,
                    int priority) {
                iconCustomizers.add(new IconCustomizer<>(blockEntityClass, provider, priority));
            }

            @Override
            public <T extends TileEntity> void addBlockEntityName(Class<T> blockEntityClass,
                    Class<? extends Block> blockClass, ResourceLocation id, NameProvider<? super T> provider,
                    int priority) {
                nameCustomizers.add(new NameCustomizer<>(blockEntityClass, provider, priority));
            }

            @Override
            public <T extends TileEntity> void addBlockEntityModName(Class<T> blockEntityClass,
                    Class<? extends Block> blockClass, ResourceLocation id, ModNameProvider<? super T> provider,
                    int priority) {
                modNameCustomizers.add(new ModNameCustomizer<>(blockEntityClass, provider, priority));
            }
        });

        nameCustomizers.sort(Comparator.comparingInt(NameCustomizer::priority));
        iconCustomizers.sort(Comparator.comparingInt(IconCustomizer::priority));
        modNameCustomizers.sort(Comparator.comparingInt(ModNameCustomizer::priority));
        bodyCustomizers.sort(Comparator.comparingInt(BodyCustomizer::priority));
    }

    @Override
    public String getID() {
        return AppEng.makeId("block-entity").toString();
    }

    @Override
    public void addProbeInfo(ProbeMode mode, IProbeInfo probeInfo, EntityPlayer player, World level,
            IBlockState blockState, IProbeHitData data) {
        var blockEntity = level.getTileEntity(data.getPos());
        if (blockEntity != null) {
            var serverData = getServerData(player, blockEntity);

            // Then allow all providers to modify the probe info
            var context = getContext(player, data, serverData);
            var tooltipBuilder = new TopTooltipBuilder(probeInfo);
            for (var customizer : bodyCustomizers) {
                customizer.buildTooltip(blockEntity, context, tooltipBuilder);
            }
        }
    }

    @Override
    public boolean overrideStandardInfo(ProbeMode probeMode, IProbeInfo probeInfo, EntityPlayer player, World level,
            IBlockState blockState, IProbeHitData probeHitData) {
        var blockEntity = level.getTileEntity(probeHitData.getPos());
        if (blockEntity == null) {
            return false;
        }

        var serverData = getServerData(player, blockEntity);
        var context = getContext(player, probeHitData, serverData);

        // If any one of our providers customizes the info, we have to override it all
        ITextComponent name = null;
        String modName = null;
        ItemStack icon = null;
        for (var customizer : nameCustomizers) {
            name = customizer.getName(blockEntity, context);
            if (name != null) {
                break;
            }
        }
        for (var customizer : modNameCustomizers) {
            modName = customizer.getModName(blockEntity, context);
            if (modName != null) {
                break;
            }
        }
        for (var customizer : iconCustomizers) {
            icon = customizer.getIcon(blockEntity, context);
            if (icon != null) {
                break;
            }
        }

        if (name != null || modName != null || icon != null) {
            // Fill out defaults
            var pickBlock = probeHitData.getPickBlock();
            if (icon == null) {
                icon = pickBlock;
            }
            if (modName == null) {
                modName = Platform.getModName(pickBlock.getItem().getRegistryName().getNamespace());
            }

            // TOP itself checks a config here to enable/disable the mod name, but I don't know how to get access to it
            var lines = probeInfo.horizontal().item(icon).vertical();
            if (name != null) {
                new TopTooltipBuilder(lines).addLine(name);
            } else {
                // translated on the client, unlike the name of the stack on the server
                lines.itemLabel(pickBlock);
            }
            lines.text(TextStyleClass.MODNAME + modName);
            return true;
        }

        return false;
    }

    @FunctionalInterface
    private interface ServerDataCollector {
        void collect(TileEntity blockEntity, EntityPlayerMP player, NBTTagCompound serverData);
    }

    private NBTTagCompound getServerData(EntityPlayer player, TileEntity blockEntity) {
        var serverData = new NBTTagCompound();
        // Emulate Jade/WTHIT/Waila model by collecting the server-data they would send to the client
        // into a temporary compound tag.
        if (player instanceof EntityPlayerMP serverPlayer) {
            for (var dataCollector : dataCollectors) {
                dataCollector.collect(blockEntity, serverPlayer, serverData);
            }
        }
        return serverData;
    }

    private static TooltipContext getContext(EntityPlayer player, IProbeHitData data, NBTTagCompound serverData) {
        return new TooltipContext(
                serverData,
                data.getHitVec(),
                player);
    }

    private static final class NameCustomizer<T> {
        private final Class<T> beClass;
        private final NameProvider<? super T> provider;
        private final int priority;

        NameCustomizer(Class<T> beClass, NameProvider<? super T> provider, int priority) {
            this.beClass = beClass;
            this.provider = provider;
            this.priority = priority;
        }

        int priority() {
            return priority;
        }

        ITextComponent getName(TileEntity blockEntity, TooltipContext context) {
            if (beClass.isInstance(blockEntity)) {
                return provider.getName(beClass.cast(blockEntity), context);
            }
            return null;
        }
    }

    private static final class IconCustomizer<T> {
        private final Class<T> beClass;
        private final IconProvider<? super T> provider;
        private final int priority;

        IconCustomizer(Class<T> beClass, IconProvider<? super T> provider, int priority) {
            this.beClass = beClass;
            this.provider = provider;
            this.priority = priority;
        }

        int priority() {
            return priority;
        }

        ItemStack getIcon(TileEntity blockEntity, TooltipContext context) {
            if (beClass.isInstance(blockEntity)) {
                return provider.getIcon(beClass.cast(blockEntity), context);
            }
            return null;
        }
    }

    private static final class ModNameCustomizer<T> {
        private final Class<T> beClass;
        private final ModNameProvider<? super T> provider;
        private final int priority;

        ModNameCustomizer(Class<T> beClass, ModNameProvider<? super T> provider, int priority) {
            this.beClass = beClass;
            this.provider = provider;
            this.priority = priority;
        }

        int priority() {
            return priority;
        }

        String getModName(TileEntity blockEntity, TooltipContext context) {
            if (beClass.isInstance(blockEntity)) {
                return provider.getModName(beClass.cast(blockEntity), context);
            }
            return null;
        }
    }

    private static final class BodyCustomizer<T> {
        private final Class<T> beClass;
        private final BodyProvider<? super T> provider;
        private final int priority;

        BodyCustomizer(Class<T> beClass, BodyProvider<? super T> provider, int priority) {
            this.beClass = beClass;
            this.provider = provider;
            this.priority = priority;
        }

        int priority() {
            return priority;
        }

        void buildTooltip(TileEntity blockEntity, TooltipContext context, TooltipBuilder tooltipBuilder) {
            if (beClass.isInstance(blockEntity)) {
                provider.buildTooltip(beClass.cast(blockEntity), context, tooltipBuilder);
            }
        }
    }
}
