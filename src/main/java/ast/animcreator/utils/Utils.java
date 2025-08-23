package ast.animcreator.utils;

import net.minecraft.util.math.BlockPos;

public class Utils {
    public static String blockPosStr(BlockPos blockPos) {
        return blockPos.getX() + ";" + blockPos.getY() + ";" + blockPos.getZ();
    }
}
