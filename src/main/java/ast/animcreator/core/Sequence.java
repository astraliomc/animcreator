package ast.animcreator.core;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

public class Sequence {

    private static class SequenceAnim {
        public Animation animation;
        public Integer tick;
        public boolean loop;

        public SequenceAnim(Animation animation, Integer tick, boolean loop) {
            this.animation = animation;
            this.tick = tick;
            this.loop = loop;
        }
    }

    public String name;
    private List<SequenceAnim> seq;
    //NOTE duration of -1 means infinite (all anims are looping)
    private int durationInTicks = -1;

    public boolean canPlay;
    public boolean loopSeq;
    private boolean forceRestart = false;

    private int nextTickIdx;
    private int tickCounter;

    public Sequence(String name) {
        this.name = name;
        this.seq = new ArrayList<>();
        loopSeq = false;
        nextTickIdx = 0;
        tickCounter = 0;
        canPlay = false;
    }

    public boolean addAnimToSequence(Animation animation, Integer tick, boolean loop) {
        if (animation == null || animation.getFrames().size() < 2) {
            return false;
        }
        seq.add(new SequenceAnim(animation, tick, loop));
        seq.sort(Comparator.comparingInt(s2 -> s2.tick));
        for (int seqIdx = seq.size() - 1 ; seqIdx > -1 ; --seqIdx) {
            SequenceAnim seqAnim = seq.get(seqIdx);
            if (!seqAnim.loop) {
                durationInTicks = seqAnim.tick + (seqAnim.animation.getFrames().get(seqAnim.animation.getFrames().size() - 1).tick);
                System.out.println("NEW DURATION " + durationInTicks);
            }
        }
        return true;
    }

    private void restartAndClearAllAnimationBlocks() {
        for (SequenceAnim seqAnim : seq) {
            seqAnim.animation.clearAllAnimationBlocks();
        }
        restart();
    }

    private void restart() {
        tickCounter = 0;
        nextTickIdx = 0;
        canPlay = true;
    }

    public void forceRestart() {
        forceRestart = true;
    }

    public void playSequence() {
        if (forceRestart) {
            forceRestart = false;
            restartAndClearAllAnimationBlocks();
        }
        if (!canPlay) {
            return;
        }

        //System.out.println("TICK COUNTER " + tickCounter);
        //System.out.println("nextTickIdx " + nextTickIdx);

        if (nextTickIdx >= seq.size()) {
            if (tickCounter <= durationInTicks) {
                ++tickCounter;
                return;
            }
            restart();
            if (!loopSeq) {
                canPlay = false;
                for (SequenceAnim seqAnim : seq) {
                    if (seqAnim.loop) {
                        seqAnim.animation.pauseAnimation();
                    }
                }
                return;
            }
        }

        SequenceAnim seqAnim = seq.get(nextTickIdx);
        if (tickCounter == seqAnim.tick) {
            playAnim(seqAnim.animation, seqAnim.loop);
            ++nextTickIdx;
        }
        ++tickCounter;
    }

    private void playAnim(Animation animation, boolean loopAnim) {
        AnimPlayer.addAnimationToPlay(animation, loopAnim);
    }

    public void stopSequence() {
        for (SequenceAnim seqAnim : seq) {
            seqAnim.animation.stopAnimation();
        }
    }

    public void pauseSequence() {
        canPlay = false;
        for (SequenceAnim seqAnim : seq) {
            seqAnim.animation.pauseAnimation();
        }
    }

    public void resumeSequence() {
        canPlay = true;
        for (SequenceAnim seqAnim : seq) {
            seqAnim.animation.resumeAnimation();
        }
    }

    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append(name);
        for (SequenceAnim seqAnim : seq) {
            sb.append(seqAnim.animation.name).append(" ").append(seqAnim.tick).append(" ").append(seqAnim.loop ? "LOOP" : "NOLOOP");
            sb.append("\n");
        }
        return sb.toString();
    }
}
