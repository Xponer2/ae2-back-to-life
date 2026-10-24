package appeng.decorative.solid;

import appeng.block.BlockProperties;

import java.util.Collections;
import java.util.List;

import org.jetbrains.annotations.Nullable;

import net.minecraft.block.Block;
import net.minecraft.block.properties.PropertyBool;
import net.minecraft.block.properties.PropertyDirection;
import net.minecraft.block.state.BlockStateContainer;
import net.minecraft.block.state.IBlockState;
import net.minecraft.init.Blocks;
import net.minecraft.item.ItemStack;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.Mirror;
import net.minecraft.util.Rotation;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.IBlockAccess;
import net.minecraft.world.World;
import net.minecraft.world.level.block.SimpleWaterloggedBlock;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.level.material.PushReaction;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;
import net.minecraftforge.fluids.FluidRegistry;

import appeng.block.AEBaseBlock;
import appeng.block.BlockStateProperties;
import appeng.util.BlockPlaceContext;
import appeng.util.shape.CollisionContext;
import appeng.util.shape.Shapes;
import appeng.util.shape.VoxelShape;

public class CertusQuartzClusterBlock extends AEBaseBlock implements SimpleWaterloggedBlock {
    public static final PropertyBool WATERLOGGED = BlockStateProperties.WATERLOGGED;
    public static final PropertyDirection FACING = BlockStateProperties.FACING;
    protected final VoxelShape northAabb;
    protected final VoxelShape southAabb;
    protected final VoxelShape eastAabb;
    protected final VoxelShape westAabb;
    protected final VoxelShape upAabb;
    protected final VoxelShape downAabb;

    public CertusQuartzClusterBlock(int i, int j, BlockProperties properties) {
        super(properties);
        this.setDefaultState(this.getDefaultState().withProperty(WATERLOGGED, false).withProperty(FACING, EnumFacing.UP));
        this.upAabb = Shapes.box16(j, 0.0, j, (16 - j), i, (16 - j));
        this.downAabb = Shapes.box16(j, (16 - i), j, (16 - j), 16.0, (16 - j));
        this.northAabb = Shapes.box16(j, j, (16 - i), (16 - j), (16 - j), 16.0);
        this.southAabb = Shapes.box16(j, j, 0.0, (16 - j), (16 - j), i);
        this.eastAabb = Shapes.box16(0.0, j, j, i, (16 - j), (16 - j));
        this.westAabb = Shapes.box16((16 - i), j, j, 16.0, (16 - j), (16 - j));
    }

    public VoxelShape getShape(IBlockState state, IBlockAccess level, BlockPos pos, CollisionContext context) {
        return switch (state.getValue(FACING)) {
            case NORTH -> this.northAabb;
            case SOUTH -> this.southAabb;
            case EAST -> this.eastAabb;
            case WEST -> this.westAabb;
            case DOWN -> this.downAabb;
            case UP -> this.upAabb;
        };
    }

    @Override
    public List<ItemStack> getDrops(IBlockState state, LootParams.Builder builder) {
        if (builder.getOptionalParameter(LootContextParams.THIS_ENTITY) == null) {
            // Prevent dropping anything if there is no player to avoid certus dust and crystals floating around when
            // the bud or cluster's supporting block is destroyed.
            return Collections.emptyList();
        }
        return super.getDrops(state, builder);
    }

    public boolean canSurvive(IBlockState state, World level, BlockPos pos) {
        EnumFacing direction = state.getValue(FACING);
        BlockPos blockPos = pos.offset(direction.getOpposite());
        return level.getBlockState(blockPos).isFaceSturdy(level, blockPos, direction);
    }

    public IBlockState updateShape(IBlockState state, EnumFacing direction, IBlockState neighborState, World level,
            BlockPos currentPos, BlockPos neighborPos) {
        if (state.getValue(WATERLOGGED)) {
            level.scheduleTick(currentPos, FluidRegistry.WATER, FluidRegistry.WATER.getTickDelay(level));
        }

        return direction == state.getValue(FACING).getOpposite() && !state.canSurvive(level, currentPos)
                ? Blocks.AIR.getDefaultState()
                : super.updateShape(state, direction, neighborState, level, currentPos, neighborPos);
    }

    @Nullable
    public IBlockState getStateForPlacement(BlockPlaceContext context) {
        var fluidState = context.getLevel().getFluidState(context.getClickedPos());
        return super.getStateForPlacement(context)
                .setValue(WATERLOGGED, fluidState.getType() == FluidRegistry.WATER)
                .setValue(FACING, context.getClickedFace());
    }

    public IBlockState rotate(IBlockState state, Rotation rotation) {
        return state.withProperty(FACING, rotation.rotate(state.getValue(FACING)));
    }

    public IBlockState mirror(IBlockState state, Mirror mirror) {
        return state.rotate(mirror.getRotation(state.getValue(FACING)));
    }

    public FluidState getFluidState(IBlockState state) {
        return state.getValue(WATERLOGGED) ? FluidRegistry.WATER.getSource(false) : super.getFluidState(state);
    }

    protected void createBlockStateDefinition(BlockStateContainer.Builder builder) {
        builder.add(WATERLOGGED, FACING);
    }

    public PushReaction getPistonPushReaction(IBlockState state) {
        return PushReaction.DESTROY;
    }
}
