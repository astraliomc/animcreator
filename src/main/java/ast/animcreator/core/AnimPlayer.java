package ast.animcreator.core;

import net.minecraft.server.MinecraftServer;

import java.util.ArrayList;
import java.util.List;

public class AnimPlayer {

    private static List<Animation> animationPlayingList = new ArrayList<>();

    public static void addAnimationToPlay(Animation animation, boolean loopAnim) {
        animation.loopAnim = loopAnim;
        boolean alreadyExists = false;
        for (Animation animPlaying : animationPlayingList) {
            if (animPlaying.name.equals(animation.name)) {
                // Animation was already playing, paused or stopped so force restart from the beginning
                animPlaying.forceRestart = true;
                alreadyExists = true;
                break;
            }
        }
        if (!alreadyExists) {
            animationPlayingList.add(animation);
        }
    }

    public static void onEndTick(MinecraftServer server) {
        for (Animation animationPlaying : animationPlayingList) {
            animationPlaying.playAnimation();
        }
    }
}
