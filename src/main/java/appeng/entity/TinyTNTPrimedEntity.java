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

package appeng.entity;

import appeng.util.Platform;

import java.util.List;

import net.neoforged.neoforge.entity.IEntityWithComplexSpawn;
import net.neoforged.neoforge.event.EventHooks;
import org.jetbrains.annotations.Nullable;

import net.minecraft.block.Block;
import net.minecraft.block.state.IBlockState;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.item.EntityItem;
import net.minecraft.entity.item.EntityTNTPrimed;
import net.minecraft.init.Blocks;
import net.minecraft.init.SoundEvents;
import net.minecraft.item.ItemStack;
import net.minecraft.util.SoundCategory;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.Explosion;
import net.minecraft.world.World;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.level.Explosion.BlockInteraction;

import appeng.core.AEConfig;
import appeng.core.AppEng;
import appeng.core.definitions.AEBlocks;
import appeng.core.definitions.AEEntities;
import appeng.core.definitions.EntityType;
import appeng.core.network.RegistryFriendlyByteBuf;
import appeng.core.network.clientbound.MockExplosionPacket;

public final class TinyTNTPrimedEntity extends EntityTNTPrimed implements IEntityWithComplexSpawn {

    private EntityLivingBase placedBy;

    public TinyTNTPrimedEntity(EntityType<? extends TinyTNTPrimedEntity> type, World level) {
        super(type, level);
        this.blocksBuilding = true;
    }

    public TinyTNTPrimedEntity(World level, double x, double y, double z,
            @Nullable EntityLivingBase igniter) {
        super(AEEntities.TINY_TNT_PRIMED.get(), level);
        this.setPos(x, y, z);
        double d0 = level.rand.nextDouble() * ((float) Math.PI * 2F);
        this.setDeltaMovement(-Math.sin(d0) * 0.02D, 0.2F, -Math.cos(d0) * 0.02D);
        this.setFuse(80);
        this.xo = x;
        this.yo = y;
        this.zo = z;
        this.placedBy = igniter;
    }

    @Nullable
    @Override
    public EntityLivingBase getOwner() {
        return this.placedBy;
    }

    /**
     * Called to update the entity's position/logic.
     */
    @Override
    public void tick() {
        this.updateInWaterStateAndDoFluidPushing();

        this.xo = this.getX();
        this.yo = this.getY();
        this.zo = this.getZ();
        this.setDeltaMovement(this.getDeltaMovement().subtract(0, 0.03999999910593033D, 0));
        this.move(MoverType.SELF, this.getDeltaMovement());
        this.setDeltaMovement(
                this.getDeltaMovement().multiply(0.9800000190734863D, 0.9800000190734863D, 0.9800000190734863D));

        if (this.onGround()) {
            this.setDeltaMovement(this.getDeltaMovement().multiply(0.699999988079071D, 0.699999988079071D, -0.5D));
        }

        if (this.isInWater() && !this.level().isRemote) // put out the fuse.
        {
            ItemStack tntStack = AEBlocks.TINY_TNT.stack();

            final EntityItem item = new EntityItem(this.level(), this.getX(), this.getY(), this.getZ(),
                    tntStack);

            item.setDeltaMovement(this.getDeltaMovement());
            item.prevPosX = this.prevPosX;
            item.prevPosY = this.prevPosY;
            item.prevPosZ = this.prevPosZ;

            this.level().spawnEntity(item);
            this.discard();
        }

        if (this.getFuse() <= 0) {
            this.discard();

            if (!this.level().isRemote) {
                this.explode();
            }
        } else {
            this.level().addParticle(ParticleTypes.SMOKE, this.getX(), this.getY(), this.getZ(), 0.0D, 0.0D,
                    0.0D);
        }
        this.setFuse(this.getFuse() - 1);
    }

    // override :P
    @Override
    protected void explode() {
        this.level().playSound(null, this.getX(), this.getY(), this.getZ(), SoundEvents.GENERIC_EXPLODE.value(),
                SoundCategory.BLOCKS, 4.0F,
                (1.0F + (this.level().random.nextFloat() - this.level().random.nextFloat()) * 0.2F) * 32.9F);

        if (this.isInWater()) {
            return;
        }

        final Explosion ex = new Explosion(this.level(), this, this.getX(), this.getY(), this.getZ(),
                0.2f, false, AEConfig.instance().isTinyTntBlockDamageEnabled() ? BlockInteraction.DESTROY_WITH_DECAY
                        : BlockInteraction.KEEP);
        final AxisAlignedBB area = new AxisAlignedBB(this.getX() - 1.5, this.getY() - 1.5f, this.getZ() - 1.5,
                this.getX() + 1.5, this.getY() + 1.5, this.getZ() + 1.5);
        final List<Entity> list = this.level().getEntities(this, area);

        EventHooks.onExplosionDetonate(this.level(), ex, list, 0.2f * 2d);

        for (Entity e : list) {
            e.hurt(level().damageSources().explosion(ex), 6);
        }

        if (AEConfig.instance().isTinyTntBlockDamageEnabled()) {
            this.setPos(this.getX(), this.getY() - 0.25, this.getZ());

            for (int x = (int) (this.getX() - 2); x <= this.getX() + 2; x++) {
                for (int y = (int) (this.getY() - 2); y <= this.getY() + 2; y++) {
                    for (int z = (int) (this.getZ() - 2); z <= this.getZ() + 2; z++) {
                        final BlockPos point = new BlockPos(x, y, z);
                        final IBlockState state = this.level().getBlockState(point);
                        final Block block = state.getBlock();

                        if (!Platform.isAir(state)) {
                            float strength = (float) (2.3f
                                    - ((x + 0.5f - this.getX()) * (x + 0.5f - this.getX())
                                            + (y + 0.5f - this.getY()) * (y + 0.5f - this.getY())
                                            + (z + 0.5f - this.getZ()) * (z + 0.5f - this.getZ())));

                            final float fluidResistance = !state.getFluidState().isEmpty()
                                    ? state.getFluidState().getExplosionResistance()
                                    : 0f;
                            final float resistance = Math
                                    .max(block.getExplosionResistance(state, this.level(), point, ex), fluidResistance);
                            strength -= (resistance + 0.3F) * 0.11f;

                            if (strength > 0.01 && !Platform.isAir(state)) {
                                if (state.canDropFromExplosion(this.level(), point, ex)) {
                                    Block.dropResources(state, this.level(), point, this.level().getTileEntity(point));
                                }

                                level().setBlock(point, Blocks.AIR.getDefaultState(), 3);
                                state.onBlockExploded(this.level(), point, ex);
                            }
                        }
                    }
                }
            }
        }

        AppEng.instance().sendToAllNearExcept(null, this.getX(), this.getY(), this.getZ(), 64, this.level(),
                new MockExplosionPacket(this.getX(), this.getY(), this.getZ()));
    }

    @Override
    public void writeSpawnData(RegistryFriendlyByteBuf buffer) {
        buffer.writeByte(this.getFuse());
    }

    @Override
    public void readSpawnData(RegistryFriendlyByteBuf additionalData) {
        this.setFuse(additionalData.readByte());
    }
}
