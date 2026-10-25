package appeng.blockentity.storage;


import net.minecraftforge.fluids.IFluidTank;
import net.minecraftforge.fluids.FluidUtil;
import net.minecraftforge.fluids.FluidTank;
import net.minecraftforge.fluids.Fluid;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.EnumHand;
import net.minecraft.util.math.BlockPos;
import net.minecraftforge.fluids.capability.IFluidHandler;

import appeng.blockentity.AEBaseBlockEntity;
import appeng.core.definitions.BlockEntityType;
import appeng.core.network.RegistryFriendlyByteBuf;

public class SkyStoneTankBlockEntity extends AEBaseBlockEntity {

    public static final int BUCKET_CAPACITY = 16;

    protected FluidTank tank = new FluidTank(Fluid.BUCKET_VOLUME * BUCKET_CAPACITY) {
        @Override
        protected void onContentsChanged() {
            SkyStoneTankBlockEntity.this.markForUpdate();
            SkyStoneTankBlockEntity.this.markDirty();
        }
    };

    public SkyStoneTankBlockEntity() {
        super();
    }

    @Override
    public void saveAdditional(NBTTagCompound data) {
        super.saveAdditional(data);
        var tankNbt = new NBTTagCompound();
        tank.writeToNBT(tankNbt);
        if (!tankNbt.isEmpty()) {
            data.setTag("tank", tankNbt);
        }
    }

    @Override
    public void loadTag(NBTTagCompound data) {
        super.loadTag(data);
        tank.readFromNBT(data.getCompoundTag("tank"));
    }

    public boolean onPlayerUse(EntityPlayer player, EnumHand hand) {
        return FluidUtil.interactWithFluidHandler(player, hand, tank);
    }

    public IFluidTank getTank() {
        return tank;
    }

    public IFluidHandler getFluidHandler() {
        return tank;
    }

    protected boolean readFromStream(RegistryFriendlyByteBuf data) {
        boolean ret = super.readFromStream(data);
        var tankTag = data.readNbt();
        tank.readFromNBT(tankTag != null ? tankTag : new NBTTagCompound());
        return ret;
    }

    protected void writeToStream(RegistryFriendlyByteBuf data) {
        super.writeToStream(data);
        var tag = new NBTTagCompound();
        tank.writeToNBT(tag);
        data.writeNbt(tag);
    }
}
