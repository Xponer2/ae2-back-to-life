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

package appeng.debug;

import appeng.util.ItemProperties;

import appeng.core.CreativeTabOutput;

import net.minecraft.core.component.DataComponents;
import net.minecraft.creativetab.CreativeTabs;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.ActionResult;
import net.minecraft.util.EnumActionResult;
import net.minecraft.util.EnumHand;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.TextComponentString;
import net.minecraft.world.World;
import net.minecraft.world.WorldServer;
import net.minecraft.world.chunk.Chunk;
import net.minecraft.world.gen.structure.StructureBoundingBox;

import appeng.core.AEConfig;
import appeng.items.AEBaseItem;
import appeng.util.InteractionUtil;
import appeng.util.Platform;
import appeng.util.UseOnContext;
import appeng.worldgen.meteorite.CraterType;
import appeng.worldgen.meteorite.MeteoritePlacer;
import appeng.worldgen.meteorite.PlacedMeteoriteSettings;
import appeng.worldgen.meteorite.debug.MeteoriteSpawner;

public class MeteoritePlacerItem extends AEBaseItem {

    private static final String MODE_TAG = "mode";

    public MeteoritePlacerItem(ItemProperties properties) {
        super(properties);
    }

    @Override
    public ActionResult<ItemStack> use(World level, EntityPlayer player, EnumHand hand) {
        if (level.isRemote) {
            return new ActionResult<>(EnumActionResult.PASS, player.getHeldItem(hand));
        }

        if (InteractionUtil.isInAlternateUseMode(player)) {
            final ItemStack itemStack = player.getHeldItem(hand);
            NBTTagCompound.update(DataComponents.CUSTOM_DATA, itemStack, tag -> {
                if (tag.contains(MODE_TAG)) {
                    final byte mode = tag.getByte("mode");
                    tag.setByte(MODE_TAG, (byte) ((mode + 1) % CraterType.values().length));
                } else {
                    tag.setByte(MODE_TAG, (byte) CraterType.NORMAL.ordinal());
                }
            });

            var craterType = getCraterType(itemStack);

            player.sendMessage(new TextComponentString(craterType.name()));

            return new ActionResult<>(EnumActionResult.SUCCESS, itemStack);
        }

        return super.use(level, player, hand);
    }

    private CraterType getCraterType(ItemStack stack) {
        var customData = stack.getOrDefault(DataComponents.CUSTOM_DATA, NBTTagCompound.EMPTY);
        var tag = customData.copy();
        return CraterType.values()[tag.getByte(MODE_TAG)];
    }

    @Override
    public EnumActionResult onItemUseFirst(ItemStack stack, UseOnContext context) {
        if (context.getLevel().isRemote) {
            return EnumActionResult.PASS;
        }

        EntityPlayerMP player = (EntityPlayerMP) context.getPlayer();
        WorldServer level = (WorldServer) context.getLevel();
        BlockPos pos = context.getClickedPos();

        if (player == null) {
            return EnumActionResult.PASS;
        }

        // See MeteoriteStructure for original code
        float coreRadius = level.rand.nextFloat() * 6.0f + 2;
        boolean pureCrater = level.rand.nextFloat() > 0.5f;
        CraterType craterType = getCraterType(stack);

        MeteoriteSpawner spawner = new MeteoriteSpawner();
        PlacedMeteoriteSettings spawned = spawner.trySpawnMeteoriteAtSuitableHeight(level, pos, coreRadius, craterType,
                pureCrater);

        if (spawned == null) {
            player.sendMessage(new TextComponentString("Un-suitable Location."));
            return EnumActionResult.FAIL;
        }

        // Since we don't know yet if the meteorite will be underground or not,
        // we have to assume maximum size
        int range = (int) Math.ceil((coreRadius * 2 + 5) * 5f);

        StructureBoundingBox boundingBox = new StructureBoundingBox(pos.getX() - range, pos.getY() - 10, pos.getZ() - range,
                pos.getX() + range, pos.getY() + 10, pos.getZ() + range);

        MeteoritePlacer.place(level, spawned, boundingBox, level.rand);

        player.sendMessage(new TextComponentString("Spawned at y=" + spawned.getPos().getY() + " range=" + range));

        // The placer will not send chunks to the player since it's used as part
        // of world-gen normally, so we'll have to do it ourselves. Since this
        // is a debug tool, we'll not care about being terribly efficient here
        ChunkPos.rangeClosed(new ChunkPos(spawned.getPos()), 1).forEach(cp -> {
            Chunk c = level.getChunk(cp.x, cp.z);
            player.connection.send(Platform.getFullChunkPacket(c));
        });

        return EnumActionResult.SUCCESS;
    }

    @Override
    public void addToMainCreativeTab(CreativeTabOutput.DisplayParameters parameters, CreativeTabOutput output) {
        if (AEConfig.instance().isDebugToolsEnabled()) {
            output.accept(this);
        }
    }
}
