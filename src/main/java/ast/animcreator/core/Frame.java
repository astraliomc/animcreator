package ast.animcreator.core;

import net.minecraft.block.BlockState;
import net.minecraft.util.math.BlockPos;

import java.util.ArrayList;
import java.util.List;

public class Frame {
    public Animation animation;
    public Integer tick;
    public Region region;
    public List<FrameBlock> blocks;
    public List<FrameBlock> blocksToRemove;

    public Frame(Animation animation, Integer tick, Region region) {
        this.animation = animation;
        this.tick = tick;
        this.region = region;
        this.blocks = new ArrayList<>();
        this.blocksToRemove = new ArrayList<>();
        saveBlocksInRegion();
    }

    public Frame(Animation animation, Integer tick) {
        this.animation = animation;
        this.tick = tick;
        this.blocks = new ArrayList<>();
        this.blocksToRemove = new ArrayList<>();
    }

    public void deduceRegionFromBlocks() {
        int minX = Integer.MAX_VALUE, minY = Integer.MAX_VALUE, minZ = Integer.MAX_VALUE;
        int maxX = Integer.MIN_VALUE, maxY = Integer.MIN_VALUE, maxZ = Integer.MIN_VALUE;

        for (FrameBlock frameBlock : blocks) {
            minX = Math.min(minX, frameBlock.blockPos.getX());
            minY = Math.min(minY, frameBlock.blockPos.getY());
            minZ = Math.min(minZ, frameBlock.blockPos.getZ());

            maxX = Math.max(maxX, frameBlock.blockPos.getX());
            maxY = Math.max(maxY, frameBlock.blockPos.getY());
            maxZ = Math.max(maxZ, frameBlock.blockPos.getZ());
        }

        region = new Region(new BlockPos(minX, minY, minZ), new BlockPos(maxX, maxY, maxZ));
    }

    public void saveBlocksInRegion() {

        for (int x = region.cornerMin.getX() ; x < region.cornerMax.getX() ; ++x) {
            for (int y = region.cornerMin.getY() ; y < region.cornerMax.getY() ; ++y) {
                for (int z = region.cornerMin.getZ() ; z < region.cornerMax.getZ() ; ++z) {
                    BlockPos pos = new BlockPos(x,y,z);
                    BlockState state = animation.world.getBlockState(pos);
                    //TODO regarder s'il y a moyen d'éviter la comparaison de string
                    if (!state.getRegistryEntry().getIdAsString().equals("minecraft:air")) {
                        blocks.add(new FrameBlock(state, pos));
                    }
                }
            }
        }
        System.out.println("saveblock end");
    }

    private Region computeRegionsIntersection(Region region1, Region region2) {
        if (region1.equals(region2)) {
            return region1;
        }
        if (region1.isIntersectionEmpty(region2)) {
            return null;
        }

        int intersectMinX = Integer.MAX_VALUE, intersectMinY = Integer.MAX_VALUE, intersectMinZ = Integer.MAX_VALUE;
        int intersectMaxX = Integer.MIN_VALUE, intersectMaxY = Integer.MIN_VALUE, intersectMaxZ = Integer.MIN_VALUE;

        for (int xTest = region1.cornerMin.getX() ; xTest < region1.cornerMax.getX() ; ++xTest) {
            for (int yTest = region1.cornerMin.getY() ; yTest < region1.cornerMax.getY() ; ++yTest) {
                for (int zTest = region1.cornerMin.getZ() ; zTest < region1.cornerMax.getZ() ; ++zTest) {
                    if (xTest >= region2.cornerMin.getX() && yTest >= region2.cornerMin.getY() && zTest >= region2.cornerMin.getZ() &&
                        xTest <= region2.cornerMax.getX() && yTest <= region2.cornerMax.getY() && zTest <= region2.cornerMax.getZ()) {
                        intersectMinX = Math.min(intersectMinX, xTest);
                        intersectMinY = Math.min(intersectMinY, yTest);
                        intersectMinZ = Math.min(intersectMinZ, zTest);
                        intersectMaxX = Math.max(intersectMaxX, xTest);
                        intersectMaxY = Math.max(intersectMaxY, yTest);
                        intersectMaxZ = Math.max(intersectMaxZ, zTest);
                    }
                }
            }
        }
        return new Region(new BlockPos(intersectMinX, intersectMinY, intersectMinZ), new BlockPos(intersectMaxX, intersectMaxY, intersectMaxZ));
    }

    public void updateBlocksToRemove(Frame prevFrame) {
        blocksToRemove.clear();
        for (FrameBlock prevFrameBlock : prevFrame.blocks) {
            System.out.println("prevFrameBlock " + prevFrameBlock.toString());
            // if prevFrameBlock is not in region of curFrame, it needs to be removed
            if ((prevFrameBlock.blockPos.getX() < region.cornerMin.getX() || prevFrameBlock.blockPos.getX() > region.cornerMax.getX()) &&
                (prevFrameBlock.blockPos.getY() < region.cornerMin.getY() || prevFrameBlock.blockPos.getY() > region.cornerMax.getY()) &&
                (prevFrameBlock.blockPos.getZ() < region.cornerMin.getZ() || prevFrameBlock.blockPos.getZ() > region.cornerMax.getZ())) {
                System.out.println("remove block 1");
                blocksToRemove.add(prevFrameBlock);
                continue;
            }
            // otherwise check if there is a block with the same coords in the curFrame
            boolean removeBlock = true;
            for (FrameBlock curFrameBlock : blocks) {
                System.out.println("curFrameBlock " + curFrameBlock.toString());
                if (curFrameBlock.blockPos.equals(prevFrameBlock.blockPos)) {
                    removeBlock = false;
                    break;
                }
            }
            if (removeBlock) {
                System.out.println("remove block 2");
                blocksToRemove.add(prevFrameBlock);
            }
        }
    }
}
