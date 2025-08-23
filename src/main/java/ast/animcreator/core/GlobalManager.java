package ast.animcreator.core;

import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;

import java.util.ArrayList;
import java.util.List;

public class GlobalManager {
    public static MinecraftServer server;

    //NOTE si je veux supporter le multijoueur il faudrait faire une association 1 joueur -> 1 anim
    public static ServerPlayerEntity player;
    public static Region curRegion = null;

    public static Animation curAnimation = null;

    public final static Integer MAX_REGION_SIZE = 65536;

    //NOTE don't call add on this variable but the method addAnimation
    public static List<Animation> animations = new ArrayList<>();

    public static boolean waitingDiscardConfirmation = false;

    public static void addAnimation(Animation animation) {
        animation.editedUnsaved = false;
        animations.add(animation);
    }

}
