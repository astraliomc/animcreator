package ast.animcreator.core;

import ast.animcreator.utils.Utils;
import net.minecraft.block.Blocks;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;


public class Animation {
    public String name;
    private List<Frame> frames = new ArrayList<>();
    public ServerWorld world;

    public boolean loopAnim;
    private boolean canPlay;
    public boolean forceRestart = false;

    private int nextTickIdx;
    private int tickNextFrame;
    private int tickCounter;

    public boolean editedUnsaved = false;

    public Animation(String name, ServerWorld world) {
        this.name = name;
        this.world = world;
        this.loopAnim = false;
        restart();
    }

    public void addFrame(Frame f) {
        if (f.region == null) {
            f.deduceRegionFromBlocks();
        }
        frames.add(f);
        frames.sort(Comparator.comparingInt(f2 -> f2.tick));
        editedUnsaved = true;
        if (frames.size() == 1) {
            return;
        }
        for (int frameIdx = 0 ; frameIdx < frames.size() ; ++frameIdx) {
            Frame frame = frames.get(frameIdx);
            if (frame == f) {
                System.out.println("update blocks to remove for frame " + frameIdx);
                Frame prevFrame = (frameIdx != 0 ? frames.get(frameIdx - 1) : frames.get(frames.size() - 1));
                Frame nextFrame = frames.get((frameIdx + 1) % frames.size());
                System.out.println("update cur with prev");
                frame.updateBlocksToRemove(prevFrame);
                System.out.println("update next with cur");
                nextFrame.updateBlocksToRemove(frame);
                break;
            }
        }
    }

    public List<Frame> getFrames() {
        return frames;
    }

    public boolean frameTickAlreadyExists(Integer tick) {
        for (Frame frame : frames) {
            if (frame.tick.equals(tick)) {
                return true;
            }
        }
        return false;
    }

    public void restart() {
        tickCounter = 0;
        nextTickIdx = 0;
        tickNextFrame = 0;
        canPlay = true;
    }

    public void restartAndClearAllAnimationBlocks() {
        clearAllAnimationBlocks();
        restart();
    }

    public void clearAllAnimationBlocks() {
        List<BlockPos> blocksToRemove = new ArrayList<>();
        for (Frame frame : frames) {
            for (FrameBlock frameBlock : frame.blocks) {
                boolean alreadyInList = false;
                for (BlockPos blockToRemove : blocksToRemove) {
                    if (blockToRemove.equals(frameBlock.blockPos)) {
                        alreadyInList = true;
                        break;
                    }
                }
                if (!alreadyInList) {
                    blocksToRemove.add(frameBlock.blockPos);
                }
            }
        }
        for (BlockPos blockToRemove : blocksToRemove) {
            world.setBlockState(blockToRemove, Blocks.AIR.getDefaultState());
        }
    }

    public void playAnimation() {
        if (forceRestart) {
            forceRestart = false;
            restartAndClearAllAnimationBlocks();
        }
        //TODO je pense que je peux remplacer cet enum par un bool 'isPlaying' en tout cas pour le moment c'est le cas
        if (!canPlay) {
            return;
        }

        if (tickCounter == tickNextFrame) {
            if (nextTickIdx >= frames.size()) {
                restart();
                if (!loopAnim) {
                    canPlay = false;
                    return;
                }
            }

            showFrame(nextTickIdx);

            tickNextFrame = frames.get(nextTickIdx).tick;
            ++nextTickIdx;
        }
        ++tickCounter;
    }

    private void showFrame(int frameIdx) {
        System.out.println("frame idx " + frameIdx);
        Frame newFrame = frames.get(frameIdx);
        System.out.println("nb blocks to remove " + newFrame.blocksToRemove.size());
        for (FrameBlock frame : newFrame.blocksToRemove) {
            System.out.println("removing block " + frame.blockPos.getX() + ";" + frame.blockPos.getY() + ";" + frame.blockPos.getZ());
            world.setBlockState(frame.blockPos, Blocks.AIR.getDefaultState());
        }
        System.out.println("nb blocks to set " + newFrame.blocks.size());
        for (FrameBlock frame : newFrame.blocks) {
            System.out.println("set block " + frame.blockPos.getX() + ";" + frame.blockPos.getY() + ";" + frame.blockPos.getZ());
            world.setBlockState(frame.blockPos, frame.blockState);
        }
    }

    public void togglePauseAnimation() {
        canPlay = !canPlay;
    }

    public void stopAnimation() {
        restartAndClearAllAnimationBlocks();
        showFrame(0);
        canPlay = false;
    }

    @Override
    public String toString() {
        StringBuilder str = new StringBuilder("Summary of animation " + name + "[" + world.getDimension().effects().toString() + "]\n");
        for (int idxFrame = 0 ; idxFrame < frames.size() ; ++idxFrame) {
            Frame frame = frames.get(idxFrame);
            str.append("Frame ").append(idxFrame).append(" : tick ").append(frame.tick);
            if (frame.region != null) {
                str.append(" [").append(Utils.blockPosStr(frame.region.cornerMax)).append(" -> ").append(Utils.blockPosStr(frame.region.cornerMin)).append("]\n");
            }
            else {
                str.append('\n');
            }
        }
        return str.toString();
    }
}
