package ast.animcreator.core;

import ast.animcreator.utils.Utils;
import net.minecraft.block.Blocks;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.Text;
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
    private int tickCounter;

    public boolean editedUnsaved = false;

    public Animation(String name, ServerWorld world) {
        this.name = name;
        this.world = world;
        this.loopAnim = false;
        this.tickCounter = 0;
        this.nextTickIdx = 0;
        this.canPlay = false;
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
        // Looping over all the sorted frames because we need to edit the one before and the one after
        // the one that was just added
        for (int frameIdx = 0 ; frameIdx < frames.size() ; ++frameIdx) {
            Frame frame = frames.get(frameIdx);
            if (frame == f) {
                Frame prevFrame = (frameIdx != 0 ? frames.get(frameIdx - 1) : frames.get(frames.size() - 1));
                Frame nextFrame = frames.get((frameIdx + 1) % frames.size());
                // set blocks to remove for newly added frame using previous frame
                frame.updateBlocksToRemove(prevFrame);
                // update blocks to remove of next frame because its previous frame is now the newly added one
                nextFrame.updateBlocksToRemove(frame);
                break;
            }
        }
    }

    public boolean removeFrame(int tick) {
        Frame frameToRemove = GlobalManager.curAnimation.getFrameForTick(tick);
        if (frameToRemove == null) {
            return false;
        }
        int frameIdx = frames.indexOf(frameToRemove);
        if (frameIdx == -1) {
            return false;
        }

        editedUnsaved = true;
        if (frames.size() < 3) {
            frames.remove(frameToRemove);
            // No need to update blocks to remove if there was only 1 or 2 frames
            return true;
        }

        // Update blocks to remove of the next frame using the previous frame of the deleted one
        Frame prevFrame = (frameIdx != 0 ? frames.get(frameIdx - 1) : frames.get(frames.size() - 1));
        Frame nextFrame = frames.get((frameIdx + 1) % frames.size());
        nextFrame.updateBlocksToRemove(prevFrame);

        frames.remove(frameToRemove);
        return true;
    }

    public List<Frame> getFrames() {
        return frames;
    }

    public Frame getFrameForTick(Integer tick) {
        for (Frame frame : frames) {
            if (frame.tick.equals(tick)) {
                return frame;
            }
        }
        return null;
    }

    public void restart() {
        tickCounter = 0;
        nextTickIdx = 0;
        canPlay = true;
    }

    public void restartAndClearAllAnimationBlocks() {
        clearAllAnimationBlocks();
        restart();
    }

    public void clearAllAnimationBlocks() {
        List<BlockPos> allBlocksToRemove = new ArrayList<>();
        /// Getting all unique block positions of all the frames
        for (Frame frame : frames) {
            for (FrameBlock frameBlock : frame.blocks) {
                /// Checking if current block position was already added in the list
                boolean alreadyInList = false;
                for (BlockPos blockToRemove : allBlocksToRemove) {
                    if (blockToRemove.equals(frameBlock.blockPos)) {
                        alreadyInList = true;
                        break;
                    }
                }
                if (!alreadyInList) {
                    allBlocksToRemove.add(frameBlock.blockPos);
                }
            }
        }
        /// Removing all blocks
        for (BlockPos blockToRemove : allBlocksToRemove) {
            world.setBlockState(blockToRemove, Blocks.AIR.getDefaultState());
        }
    }

    public void playAnimation() {
        if (forceRestart) {
            forceRestart = false;
            restartAndClearAllAnimationBlocks();
        }
        if (!canPlay) {
            return;
        }

        /// At tick 0 or if we reached a new frame
        if (nextTickIdx == 0 || tickCounter == frames.get(nextTickIdx - 1).tick) {
            /// If we reached the last frame, reset internal values and check if anim needs to loop
            if (nextTickIdx >= frames.size()) {
                restart();
                if (!loopAnim) {
                    canPlay = false;
                    return;
                }
            }

            showFrame(nextTickIdx);

            ++nextTickIdx;
        }
        ++tickCounter;
    }

    private void showFrame(int frameIdx) {
        if (frames.isEmpty()) {
            return;
        }
        Frame newFrame = frames.get(frameIdx);
        /// Step 1 : remove the blocks which were in the previous frame but not in the new one
        for (FrameBlock frame : newFrame.blocksToRemove) {
            world.setBlockState(frame.blockPos, Blocks.AIR.getDefaultState());
        }
        /// Step 2 : set the new blocks of the new frame
        for (FrameBlock frame : newFrame.blocks) {
            world.setBlockState(frame.blockPos, frame.blockState);
        }
    }

    public void pauseAnimation() {
        canPlay = false;
    }

    public void resumeAnimation() {
        canPlay = true;
    }

    public void stopAnimation() {
        restartAndClearAllAnimationBlocks();
        showFrame(0);
        canPlay = false;
    }

    public void forceAdvanceOneFrame() {
        if (frames.isEmpty()) {
            return;
        }
        //NOTE clearAllAnimationBlocks is slow but this method is not real-time
        // just check if it is not TOO slow with a large animation
        clearAllAnimationBlocks();
        ++nextTickIdx;
        if (nextTickIdx < frames.size()) {
            tickCounter = frames.get(nextTickIdx - 1).tick;
        }
        else {
            tickCounter = 0;
            nextTickIdx = 0;
        }
        showFrame(nextTickIdx);
    }

    public void forceGoBackOneFrame() {
        if (frames.isEmpty()) {
            return;
        }
        clearAllAnimationBlocks();
        nextTickIdx = (nextTickIdx == 0 ? frames.size() - 1 : nextTickIdx - 1);
        if (nextTickIdx > 0) {
            tickCounter = frames.get(nextTickIdx - 1).tick;
        }
        else {
            tickCounter = frames.get(nextTickIdx).tick;
        }
        showFrame(nextTickIdx);
    }

    public void slowDown(float factor) {
        for (Frame f : frames) {
            f.tick = (int)((float)f.tick * factor);
        }
    }

    public boolean speedUp(float factor) {
        if (frames.isEmpty()) {
            return false;
        }

        List<Integer> newTicks = new ArrayList<>(frames.size());
        int firstTick = frames.get(0).tick;
        boolean isFirst = true;
        for (Frame f : frames) {
            int newTick = (int)((float)f.tick / factor);
            if ((isFirst && newTick <= 0) || (!isFirst && newTick <= firstTick)) {
                return false;
            }
            newTicks.add(newTick);
            firstTick = newTick;
            isFirst = false;
        }

        for (int newTickIdx = 0 ; newTickIdx < newTicks.size() ; ++newTickIdx) {
            frames.get(newTickIdx).tick = newTicks.get(newTickIdx);
        }
        return true;
    }

    public boolean moveFrame(Integer frameNumber, Integer newTick) {
        if (frameNumber < 1 || frameNumber > frames.size() + 1) {
            return false;
        }
        Frame oldFrame = frames.get(frameNumber - 1);
        Frame newFrame = new Frame(oldFrame.animation, newTick, oldFrame.region);
        newFrame.blocks = oldFrame.blocks;

        removeFrame(oldFrame.tick);
        addFrame(newFrame);
        return true;
    }

    public boolean canPlay() {
        return canPlay;
    }

    public String getCurFrameInfo() {
        return "Frame " + (nextTickIdx + 1) + " | Tick " + frames.get(nextTickIdx).tick;
    }

    @Override
    public String toString() {
        StringBuilder str = new StringBuilder("Summary of animation " + name + "[" + world.getDimension().effects().toString() + "]\n");
        for (int idxFrame = 0 ; idxFrame < frames.size() ; ++idxFrame) {
            Frame frame = frames.get(idxFrame);
            str.append("Frame ").append(idxFrame + 1).append(" : tick ").append(frame.tick);
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
