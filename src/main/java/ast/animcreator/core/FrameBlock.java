package ast.animcreator.core;

import net.minecraft.block.BlockState;
import net.minecraft.util.math.BlockPos;

public class FrameBlock {
    BlockState blockState;
    BlockPos blockPos;

    public FrameBlock(BlockState blockState, BlockPos blockPos) {
        this.blockState = blockState;
        this.blockPos = blockPos;
    }

    public boolean equals(FrameBlock frameBlock) {
        return frameBlock.blockPos.getX() == blockPos.getX() &&
                frameBlock.blockPos.getY() == blockPos.getY() &&
                frameBlock.blockPos.getZ() == blockPos.getZ();
    }

    public String toString() {
        return "[" + blockPos.getX() + ";" + blockPos.getY() + ";" + blockPos.getZ() + "]";
    }
}
