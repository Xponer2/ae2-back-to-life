package appeng.block.orientation;

import net.minecraft.util.EnumFacing;

/**
 * Defines the final rotation around the axis the block is facing. (a.k.a. spin).
 */
public final class SpinMapping {
    private SpinMapping() {
    }

    // When looking at the block from the given direction
    // the list contains the rotation in clockwise orientation
    // For UP/DOWN we consider NORTH to be up
    private static final EnumFacing[][] SPIN_DIRECTIONS = new EnumFacing[][] {
            // DOWN
            { EnumFacing.NORTH, EnumFacing.WEST, EnumFacing.SOUTH, EnumFacing.EAST },
            // UP
            { EnumFacing.NORTH, EnumFacing.EAST, EnumFacing.SOUTH, EnumFacing.WEST },
            // NORTH
            { EnumFacing.UP, EnumFacing.WEST, EnumFacing.DOWN, EnumFacing.EAST },
            // SOUTH
            { EnumFacing.UP, EnumFacing.EAST, EnumFacing.DOWN, EnumFacing.WEST },
            // WEST
            { EnumFacing.UP, EnumFacing.SOUTH, EnumFacing.DOWN, EnumFacing.NORTH },
            // EAST
            { EnumFacing.UP, EnumFacing.NORTH, EnumFacing.DOWN, EnumFacing.SOUTH },
    };

    public static int getSpinFromUp(EnumFacing facing, EnumFacing up) {
        var spinDirs = SPIN_DIRECTIONS[facing.ordinal()];
        for (int i = 0; i < spinDirs.length; i++) {
            if (spinDirs[i] == up) {
                return i;
            }
        }
        return 0; // Degenerated up direction just falls back to no spin
    }

    public static EnumFacing getUpFromSpin(EnumFacing facing, int spin) {
        return SPIN_DIRECTIONS[facing.ordinal()][spin];
    }
}
