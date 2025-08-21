package ast.animcreator.utils;

import ast.animcreator.core.Animation;
import ast.animcreator.core.enums.Dimension;
import net.minecraft.registry.RegistryKey;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

import java.util.HashMap;
import java.util.Map;

public class Utils {
    private static final Map<String, Dimension> dimensionStrToEnum = new HashMap<String, Dimension>() {
        {
            put("overworld", Dimension.OVERWORLD);
            put("the_nether", Dimension.NETHER);
            put("the_end", Dimension.END);
        }
    };

    public static Dimension dimensionStrToEnum(String dimensionStr) throws NullPointerException {
        return dimensionStrToEnum.get(dimensionStr);
    }

    public static String blockPosStr(BlockPos blockPos) {
        return blockPos.getX() + ";" + blockPos.getY() + ";" + blockPos.getZ();
    }
}
