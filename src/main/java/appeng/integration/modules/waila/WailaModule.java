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

package appeng.integration.modules.waila;

import java.util.List;

import org.jetbrains.annotations.Nullable;

import net.minecraft.block.Block;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.text.TextFormatting;
import net.minecraft.world.World;

import mcp.mobius.waila.api.IWailaConfigHandler;
import mcp.mobius.waila.api.IWailaDataAccessor;
import mcp.mobius.waila.api.IWailaDataProvider;
import mcp.mobius.waila.api.IWailaPlugin;
import mcp.mobius.waila.api.IWailaRegistrar;
import mcp.mobius.waila.api.WailaPlugin;

import appeng.api.integrations.igtooltip.ClientRegistration;
import appeng.api.integrations.igtooltip.CommonRegistration;
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
 * 1.12.2 only: the in-game tooltips of AE2 for Hwyla, the 1.12.2 successor of Waila (the 1.21 version has the same
 * for Jade and WTHIT). Hwyla finds the plugin by its annotation and only loads it if Hwyla is installed. Server data
 * is collected by NBT providers on the server, everything else runs on the client.
 */
@WailaPlugin(AppEng.MOD_ID)
public class WailaModule implements IWailaPlugin {

    @Override
    public void register(IWailaRegistrar registrar) {
        TooltipProviders.loadCommon(new CommonRegistration() {
            @Override
            public <T extends TileEntity> void addBlockEntityData(ResourceLocation id, Class<T> blockEntityClass,
                    ServerDataProvider<? super T> provider) {
                registrar.registerNBTProvider(new IWailaDataProvider() {
                    @Override
                    public NBTTagCompound getNBTData(EntityPlayerMP player, TileEntity te, NBTTagCompound tag,
                            World world, BlockPos pos) {
                        if (blockEntityClass.isInstance(te)) {
                            provider.provideServerData(player, blockEntityClass.cast(te), tag);
                        }
                        return tag;
                    }
                }, blockEntityClass);
            }
        });

        TooltipProviders.loadClient(new ClientRegistration() {
            @Override
            public <T extends TileEntity> void addBlockEntityBody(Class<T> blockEntityClass,
                    Class<? extends Block> blockClass, ResourceLocation id, BodyProvider<? super T> provider,
                    int priority) {
                registrar.registerBodyProvider(new IWailaDataProvider() {
                    @Override
                    public List<String> getWailaBody(ItemStack itemStack, List<String> tooltip,
                            IWailaDataAccessor accessor, IWailaConfigHandler config) {
                        var be = accessor.getTileEntity();
                        if (blockEntityClass.isInstance(be)) {
                            provider.buildTooltip(blockEntityClass.cast(be), getContext(accessor),
                                    new WailaTooltipBuilder(tooltip));
                        }
                        return tooltip;
                    }
                }, blockEntityClass);
            }

            @Override
            public <T extends TileEntity> void addBlockEntityIcon(Class<T> blockEntityClass,
                    Class<? extends Block> blockClass, ResourceLocation id, IconProvider<? super T> provider,
                    int priority) {
                registrar.registerStackProvider(new IWailaDataProvider() {
                    @Override
                    public @Nullable ItemStack getWailaStack(IWailaDataAccessor accessor,
                            IWailaConfigHandler config) {
                        var be = accessor.getTileEntity();
                        if (blockEntityClass.isInstance(be)) {
                            // Hwyla uses the next provider (or the picked block) if this is null
                            return provider.getIcon(blockEntityClass.cast(be), getContext(accessor));
                        }
                        return null;
                    }
                    // Hwyla looks up stack providers by block only (all other kinds also by block entity)
                }, blockClass);
            }

            @Override
            public <T extends TileEntity> void addBlockEntityName(Class<T> blockEntityClass,
                    Class<? extends Block> blockClass, ResourceLocation id, NameProvider<? super T> provider,
                    int priority) {
                registrar.registerHeadProvider(new IWailaDataProvider() {
                    @Override
                    public List<String> getWailaHead(ItemStack itemStack, List<String> tooltip,
                            IWailaDataAccessor accessor, IWailaConfigHandler config) {
                        var be = accessor.getTileEntity();
                        if (!blockEntityClass.isInstance(be)) {
                            return tooltip;
                        }
                        var name = provider.getName(blockEntityClass.cast(be), getContext(accessor));
                        // Replace the object name
                        if (name != null) {
                            var text = name.getFormattedText();
                            // Don't overwrite a text color if one is present
                            if (name.getStyle().getColor() == null) {
                                text = TextFormatting.WHITE + text;
                            }
                            if (tooltip.isEmpty()) {
                                tooltip.add(text);
                            } else {
                                tooltip.set(0, text);
                            }
                        }
                        return tooltip;
                    }
                }, blockEntityClass);
            }

            @Override
            public <T extends TileEntity> void addBlockEntityModName(Class<T> blockEntityClass,
                    Class<? extends Block> blockClass, ResourceLocation id, ModNameProvider<? super T> provider,
                    int priority) {
                registrar.registerTailProvider(new IWailaDataProvider() {
                    @Override
                    public List<String> getWailaTail(ItemStack itemStack, List<String> tooltip,
                            IWailaDataAccessor accessor, IWailaConfigHandler config) {
                        var be = accessor.getTileEntity();
                        if (!blockEntityClass.isInstance(be)) {
                            return tooltip;
                        }
                        var modName = provider.getModName(blockEntityClass.cast(be), getContext(accessor));
                        if (modName != null && !itemStack.isEmpty()) {
                            // Only replace the mod name if it's already there (it can be turned off in Hwyla)
                            var shownModName = Platform.getModName(
                                    itemStack.getItem().getRegistryName().getNamespace());
                            for (int i = 0; i < tooltip.size(); i++) {
                                if (shownModName.equals(TextFormatting.getTextWithoutFormattingCodes(tooltip.get(i)))) {
                                    tooltip.set(i, TextFormatting.BLUE + "" + TextFormatting.ITALIC + modName);
                                    break;
                                }
                            }
                        }
                        return tooltip;
                    }
                }, blockEntityClass);
            }
        });
    }

    private static TooltipContext getContext(IWailaDataAccessor accessor) {
        var serverData = accessor.getNBTData();
        return new TooltipContext(
                serverData != null ? serverData : new NBTTagCompound(),
                accessor.getMOP().hitVec,
                accessor.getPlayer());
    }
}
