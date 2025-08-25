package ast.animcreator.core;

import net.minecraft.server.MinecraftServer;

import java.util.ArrayList;
import java.util.List;

public class AnimPlayer {

    private static List<Animation> animationPlayingList = new ArrayList<>();

    public static void addAnimationToPlay(Animation animation, boolean loopAnim) {
        animation.loopAnim = loopAnim;
        animation.forceRestart = true;

        boolean alreadyExists = false;
        for (Animation animPlaying : animationPlayingList) {
            if (animPlaying.name.equals(animation.name)) {
                System.out.println("already exists!");
                alreadyExists = true;
                break;
            }
        }
        if (!alreadyExists) {
            System.out.println("don't already exist!");
            animationPlayingList.add(animation);
        }
    }

    public static void onEndTick(MinecraftServer server) {
        for (Animation animationPlaying : animationPlayingList) {
            animationPlaying.playAnimation();
        }
    }

    public static void stopAllAnimations() {
        animationPlayingList.clear();
    }

    public static boolean isAnimationPlaying(String animName) {
        for (Animation animationPlaying : animationPlayingList) {
            if (animationPlaying.name.equals(animName) && animationPlaying.canPlay()) {
                return true;
            }
        }
        return false;
    }

    public static void removeAnimtionPlaying(Animation animation) {
        animationPlayingList.remove(animation);
    }
}
