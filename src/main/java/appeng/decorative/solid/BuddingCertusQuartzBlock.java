package appeng.decorative.solid;

import appeng.util.Platform;

import appeng.block.BlockProperties;

import java.util.Random;

import net.minecraft.block.Block;
import net.minecraft.block.state.IBlockState;
import net.minecraft.init.Blocks;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.WorldServer;
import net.minecraft.world.level.block.AmethystClusterBlock;
import net.minecraft.world.level.material.PushReaction;
import net.minecraftforge.fluids.FluidRegistry;

import appeng.block.AEBaseBlock;
import appeng.core.definitions.AEBlocks;
import appeng.util.Util;

public class BuddingCertusQuartzBlock extends AEBaseBlock {
    public static final int GROWTH_CHANCE = 5;
    public static final int DECAY_CHANCE = 12;
    private static final EnumFacing[] DIRECTIONS = EnumFacing.VALUES;

    public BuddingCertusQuartzBlock(BlockProperties properties) {
        super(properties);
    }

    public PushReaction getPistonPushReaction(IBlockState state) {
        return PushReaction.DESTROY;
    }

    public void randomTick(IBlockState state, WorldServer level, BlockPos pos, Random randomSource) {
        if (randomSource.nextInt(GROWTH_CHANCE) != 0) {
            return;
        }

        // Try to grow cluster
        EnumFacing direction = Util.getRandom(DIRECTIONS, randomSource);
        BlockPos targetPos = pos.offset(direction);
        IBlockState targetState = level.getBlockState(targetPos);
        Block newCluster = null;
        if (canClusterGrowAtState(targetState)) {
            newCluster = AEBlocks.SMALL_QUARTZ_BUD.block();
        } else if (Platform.is(targetState, AEBlocks.SMALL_QUARTZ_BUD.block())
                && targetState.getValue(AmethystClusterBlock.FACING) == direction) {
            newCluster = AEBlocks.MEDIUM_QUARTZ_BUD.block();
        } else if (Platform.is(targetState, AEBlocks.MEDIUM_QUARTZ_BUD.block())
                && targetState.getValue(AmethystClusterBlock.FACING) == direction) {
            newCluster = AEBlocks.LARGE_QUARTZ_BUD.block();
        } else if (Platform.is(targetState, AEBlocks.LARGE_QUARTZ_BUD.block())
                && targetState.getValue(AmethystClusterBlock.FACING) == direction) {
            newCluster = AEBlocks.QUARTZ_CLUSTER.block();
        }

        if (newCluster == null) {
            return;
        }

        // Grow certus crystal
        IBlockState newClusterState = newCluster.getDefaultState()
                .setValue(AmethystClusterBlock.FACING, direction)
                .setValue(AmethystClusterBlock.WATERLOGGED, targetState.getFluidState().getType() == FluidRegistry.WATER);
        level.setBlockState(targetPos, newClusterState);

        // Damage the budding certus block after a successful growth
        if (this == AEBlocks.FLAWLESS_BUDDING_QUARTZ.block() || randomSource.nextInt(DECAY_CHANCE) != 0) {
            return;
        }
        Block newBlock;
        if (this == AEBlocks.FLAWED_BUDDING_QUARTZ.block()) {
            newBlock = AEBlocks.CHIPPED_BUDDING_QUARTZ.block();
        } else if (this == AEBlocks.CHIPPED_BUDDING_QUARTZ.block()) {
            newBlock = AEBlocks.DAMAGED_BUDDING_QUARTZ.block();
        } else if (this == AEBlocks.DAMAGED_BUDDING_QUARTZ.block()) {
            newBlock = AEBlocks.QUARTZ_BLOCK.block();
        } else {
            throw new IllegalStateException("Unexpected block: " + this);
        }
        level.setBlockState(pos, newBlock.getDefaultState());
    }

    public static boolean canClusterGrowAtState(IBlockState state) {
        return Platform.isAir(state) || Platform.is(state, Blocks.WATER) && state.getFluidState().getAmount() == 8;
    }
}
