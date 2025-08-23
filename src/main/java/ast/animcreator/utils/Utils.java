package ast.animcreator.utils;

import ast.animcreator.core.Animation;
import net.minecraft.registry.RegistryKey;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

import java.util.HashMap;
import java.util.Map;

public class Utils {
    public static String blockPosStr(BlockPos blockPos) {
        return blockPos.getX() + ";" + blockPos.getY() + ";" + blockPos.getZ();
    }
}
