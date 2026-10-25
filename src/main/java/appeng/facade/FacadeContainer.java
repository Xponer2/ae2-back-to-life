/*
 * This file is part of Applied Energistics 2.
 * Copyright (c) 2013 - 2015, AlgorithmX2, All rights reserved.
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

package appeng.facade;

import java.util.Arrays;
import java.util.function.Consumer;

import io.netty.buffer.ByteBuf;
import org.apache.commons.lang3.StringUtils;

import net.minecraft.block.Block;
import net.minecraft.block.state.IBlockState;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.EnumFacing;

import appeng.api.parts.IFacadeContainer;
import appeng.api.parts.IFacadePart;
import appeng.api.parts.IPartHost;
import appeng.core.network.RegistryFriendlyByteBuf;
import appeng.core.network.codec.ByteBufCodecs;
import appeng.core.network.codec.StreamCodec;
import appeng.parts.CableBusStorage;
import appeng.util.AECodecs;
import appeng.util.DataResults;
import appeng.util.NbtOps;

public class FacadeContainer implements IFacadeContainer {
    private static final StreamCodec<ByteBuf, IBlockState> BLOCK_STATE_STREAM_CODEC = ByteBufCodecs
            .idMapper(Block::getStateById, Block::getStateId);

    /**
     * Key names to store facades
     */
    private static final String[] NBT_KEY_NAMES = Arrays.stream(EnumFacing.VALUES)
            .map(d -> "facade" + StringUtils.capitalize(d.getName()))
            .toArray(String[]::new);

    private final CableBusStorage storage;
    private final Consumer<EnumFacing> changeCallback;

    public FacadeContainer(CableBusStorage cbs, Consumer<EnumFacing> changeCallback) {
        this.storage = cbs;
        this.changeCallback = changeCallback;
    }

    @Override
    public boolean canAddFacade(IFacadePart a) {
        return this.getFacade(a.getSide()) == null;
    }

    @Override
    public boolean addFacade(IFacadePart a) {
        if (canAddFacade(a)) {
            this.storage.setFacade(a.getSide(), a);
            this.notifyChange(a.getSide());
            return true;
        }
        return false;
    }

    @Override
    public void removeFacade(IPartHost host, EnumFacing side) {
        if (side != null && this.storage.getFacade(side) != null) {
            this.storage.removeFacade(side);
            this.notifyChange(side);
            if (host != null) {
                host.markForUpdate();
            }
        }
    }

    @Override
    public IFacadePart getFacade(EnumFacing side) {
        return this.storage.getFacade(side);
    }

    @Override
    public void readFromNBT(NBTTagCompound c) {
        for (var side : EnumFacing.VALUES) {
            this.storage.removeFacade(side);

            var tag = c.getTag(NBT_KEY_NAMES[side.ordinal()]);
            var result = AECodecs.BLOCK_STATE.decode(NbtOps.INSTANCE, tag).result();
            if (result.isPresent()) {
                var blockState = result.get().getFirst();
                this.storage.setFacade(side, new FacadePart(blockState, side));
            }
        }
    }

    @Override
    public void writeToNBT(NBTTagCompound c) {
        for (var side : EnumFacing.VALUES) {
            if (this.storage.getFacade(side) != null) {
                var data = DataResults.getOrThrow(AECodecs.BLOCK_STATE.encodeStart(NbtOps.INSTANCE,
                        this.storage.getFacade(side).getBlockState()));
                c.setTag(NBT_KEY_NAMES[side.ordinal()], data);
            }
        }
    }

    @Override
    public boolean readFromStream(RegistryFriendlyByteBuf out) {
        final int facadeSides = out.readByte();

        boolean changed = false;

        for (var side : EnumFacing.VALUES) {
            final int ix = 1 << side.ordinal();
            if ((facadeSides & ix) == ix) {
                var facade = BLOCK_STATE_STREAM_CODEC.decode(out);
                changed = changed || this.storage.getFacade(side) == null;
                this.storage.setFacade(side, new FacadePart(facade, side));
            } else {
                changed = changed || this.storage.getFacade(side) != null;
                this.storage.removeFacade(side);
            }
        }

        return changed;
    }

    @Override
    public void writeToStream(RegistryFriendlyByteBuf out) {
        int facadeSides = 0;
        for (var side : EnumFacing.VALUES) {
            if (this.getFacade(side) != null) {
                facadeSides |= 1 << side.ordinal();
            }
        }
        out.writeByte((byte) facadeSides);

        for (var side : EnumFacing.VALUES) {
            final IFacadePart part = this.getFacade(side);
            if (part != null) {
                BLOCK_STATE_STREAM_CODEC.encode(out, part.getBlockState());
            }
        }
    }

    @Override
    public boolean isEmpty() {
        for (var side : EnumFacing.VALUES) {
            if (this.storage.getFacade(side) != null) {
                return false;
            }
        }
        return true;
    }

    private void notifyChange(EnumFacing side) {
        this.changeCallback.accept(side);
    }

}
