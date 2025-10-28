package ast.animcreator.core;

import net.minecraft.server.MinecraftServer;

import java.util.ArrayList;
import java.util.List;

public class AnimPlayer {

    private static List<Animation> animationPlayingList = new ArrayList<>();

    public static void addAnimationToPlay(Animation animation, boolean loopAnim) {
        animation.loopAnim = loopAnim;
        //TODO si l'anim existe déjà et qu'elle loop, il ne faut pas la forceRestart si ?? (tester avec les séquences)
        animation.forceRestart = true;

        boolean alreadyExists = false;
        for (Animation animPlaying : animationPlayingList) {
            if (animPlaying.name.equals(animation.name)) {
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

    public static void removeAnimationPlaying(Animation animation) {
        animationPlayingList.remove(animation);
    }
}
