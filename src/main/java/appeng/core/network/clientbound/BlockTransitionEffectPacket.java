
package appeng.core.network.clientbound;

import net.minecraft.block.Block;
import net.minecraft.block.SoundType;
import net.minecraft.block.material.Material;
import net.minecraft.block.state.IBlockState;
import net.minecraft.client.Minecraft;
import net.minecraft.client.audio.PositionedSoundRecord;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.Blocks;
import net.minecraft.init.SoundEvents;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.SoundCategory;
import net.minecraft.util.SoundEvent;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraftforge.fluids.Fluid;
import net.minecraftforge.fluids.FluidRegistry;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

import appeng.client.render.effects.AEParticles;
import appeng.client.render.effects.EnergyParticleData;
import appeng.core.AELog;
import appeng.core.AppEngClient;
import appeng.core.network.ClientboundPacket;
import appeng.core.network.CustomAppEngPayload;
import appeng.core.network.RegistryFriendlyByteBuf;
import appeng.core.network.codec.StreamCodec;

/**
 * Plays the block breaking or fluid pickup sound and a transition particle effect into the supplied direction. Used
 * primarily by annihilation planes.
 */
public final class BlockTransitionEffectPacket implements ClientboundPacket {
    private final BlockPos pos;
    private final IBlockState blockState;
    private final EnumFacing direction;
    private final SoundMode soundMode;

    public BlockTransitionEffectPacket(BlockPos pos, IBlockState blockState, EnumFacing direction, SoundMode soundMode) {
        this.pos = pos;
        this.blockState = blockState;
        this.direction = direction;
        this.soundMode = soundMode;
    }

    public BlockPos pos() {
        return pos;
    }

    public IBlockState blockState() {
        return blockState;
    }

    public EnumFacing direction() {
        return direction;
    }

    public SoundMode soundMode() {
        return soundMode;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof BlockTransitionEffectPacket)) return false;
        BlockTransitionEffectPacket that = (BlockTransitionEffectPacket) o;
        return java.util.Objects.equals(pos, that.pos)
                && java.util.Objects.equals(blockState, that.blockState)
                && java.util.Objects.equals(direction, that.direction)
                && java.util.Objects.equals(soundMode, that.soundMode);
    }

    @Override
    public int hashCode() {
        return java.util.Objects.hash(pos, blockState, direction, soundMode);
    }

    @Override
    public String toString() {
        return "BlockTransitionEffectPacket[" + "pos=" + pos + ", " + "blockState=" + blockState + ", " + "direction=" + direction + ", " + "soundMode=" + soundMode + "]";
    }

    public static final StreamCodec<RegistryFriendlyByteBuf, BlockTransitionEffectPacket> STREAM_CODEC = StreamCodec
            .ofMember(
                    BlockTransitionEffectPacket::write,
                    BlockTransitionEffectPacket::decode);

    public static final Type<BlockTransitionEffectPacket> TYPE = CustomAppEngPayload
            .createType("block_transition_effect");

    @Override
    public Type<BlockTransitionEffectPacket> type() {
        return TYPE;
    }

    public enum SoundMode {
        BLOCK, FLUID, NONE
    }

    public void write(RegistryFriendlyByteBuf data) {
        data.writeBlockPos(pos);
        // 1.12.2 state ids are block id + meta, properties that only exist in the actual state are lost
        data.writeInt(Block.getStateId(blockState));
        data.writeEnum(direction);
        data.writeEnum(soundMode);
    }

    public static BlockTransitionEffectPacket decode(RegistryFriendlyByteBuf data) {

        var pos = data.readBlockPos();
        int blockStateId = data.readInt();
        IBlockState blockState = Block.getStateById(blockStateId);
        if (blockState == null || blockState.getBlock() == Blocks.AIR && blockStateId != 0) {
            AELog.warn("Received invalid blockstate id %d from server", blockStateId);
            blockState = Blocks.AIR.getDefaultState();
        }
        var direction = data.readEnum(EnumFacing.class);
        var soundMode = data.readEnum(SoundMode.class);
        return new BlockTransitionEffectPacket(pos, blockState, direction, soundMode);
    }

    @Override
    @SideOnly(Side.CLIENT)
    public void handleOnClient(EntityPlayer player) {
        spawnParticles(player.world);

        playBreakOrPickupSound(player.world);
    }

    @SideOnly(Side.CLIENT)
    private void spawnParticles(World level) {

        EnergyParticleData data = new EnergyParticleData(false, direction);
        for (int zz = 0; zz < 32; zz++) {
            if (AppEngClient.instance().shouldAddParticles(level.rand)) {
                // Distribute the spawn point across the entire block's area
                double x = pos.getX() + level.rand.nextFloat();
                double y = pos.getY() + level.rand.nextFloat();
                double z = pos.getZ() + level.rand.nextFloat();
                double speedX = 0.1f * this.direction.getXOffset();
                double speedY = 0.1f * this.direction.getYOffset();
                double speedZ = 0.1f * this.direction.getZOffset();

                AEParticles.create(data, x, y, z, speedX, speedY, speedZ);
            }
        }
    }

    @SideOnly(Side.CLIENT)
    private void playBreakOrPickupSound(World level) {

        SoundEvent soundEvent;
        float volume;
        float pitch;
        if (soundMode == SoundMode.FLUID) {
            // This code is based on what BucketItem does
            Fluid fluid = FluidRegistry.lookupFluidForBlock(blockState.getBlock());
            soundEvent = fluid != null ? fluid.getFillSound() : null;
            if (soundEvent == null) {
                if (blockState.getMaterial() == Material.LAVA) {
                    soundEvent = SoundEvents.ITEM_BUCKET_FILL_LAVA;
                } else {
                    soundEvent = SoundEvents.ITEM_BUCKET_FILL;
                }
            }
            volume = 1;
            pitch = 1;
        } else if (soundMode == SoundMode.BLOCK) {
            SoundType soundType = blockState.getBlock().getSoundType(blockState, level, pos, null);
            soundEvent = soundType.getBreakSound();
            volume = soundType.getVolume();
            pitch = soundType.getPitch();
        } else {
            return;
        }

        var sound = new PositionedSoundRecord(soundEvent, SoundCategory.BLOCKS, (volume + 1.0F) / 2.0F,
                pitch * 0.8F, pos);
        Minecraft.getMinecraft().getSoundHandler().playSound(sound);
    }

}
