package ast.animcreator.core;

import net.minecraft.util.math.BlockPos;

public class Region {
    public BlockPos cornerMax;
    public BlockPos cornerMin;

    public Region(BlockPos corner1, BlockPos corner2) {
        this.cornerMax = new BlockPos(
                Math.max(corner1.getX(), corner2.getX()),
                Math.max(corner1.getY(), corner2.getY()),
                Math.max(corner1.getZ(), corner2.getZ()));
        this.cornerMin = new BlockPos(
                Math.min(corner1.getX(), corner2.getX()),
                Math.min(corner1.getY(), corner2.getY()),
                Math.min(corner1.getZ(), corner2.getZ()));;
    }

    public int computeRegionSize() {
        return ((Math.abs(cornerMax.getX() - cornerMin.getX()) + 1) *
                (Math.abs(cornerMax.getY() - cornerMin.getY()) + 1) *
                (Math.abs(cornerMax.getZ() - cornerMin.getZ()) + 1));
    }

    public boolean equals(Region region) {
        return (cornerMax.getX() == region.cornerMax.getX() &&
                cornerMax.getY() == region.cornerMax.getY() &&
                cornerMax.getZ() == region.cornerMax.getZ() &&
                cornerMin.getX() == region.cornerMin.getX() &&
                cornerMin.getY() == region.cornerMin.getY() &&
                cornerMin.getZ() == region.cornerMin.getZ());
    }
    //XXYY
    //XXYY
    public boolean isIntersectionEmpty(Region region) {
        return ((cornerMax.getX() < region.cornerMin.getX()) ||
                (cornerMax.getY() < region.cornerMin.getY()) ||
                (cornerMax.getZ() < region.cornerMin.getZ()));
    }
}
