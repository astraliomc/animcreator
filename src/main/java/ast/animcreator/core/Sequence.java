package ast.animcreator.core;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

public class Sequence {

    private class SequenceAnim {
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

    public Sequence() {
        this.seq = new ArrayList<>();
    }

    public void addAnimToSequence(Animation animation, Integer tick, boolean loop) {
        seq.add(new SequenceAnim(animation, tick, loop));
        seq.sort(Comparator.comparingInt(s2 -> s2.tick));
    }
}
