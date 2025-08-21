package ast.animcreator.core;

import ast.animcreator.core.enums.State;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;

import java.util.ArrayList;
import java.util.List;

public class GlobalManager {
    public static MinecraftServer server;

    //NOTE si je veux supporter le multijoueur il faudrait faire une association 1 joueur -> 1 anim
    public static ServerPlayerEntity player;
    public static State state = State.NONE;
    public static Region curRegion = null;

    //NOTE pour le moment on ne peut charger qu'une seule animation à la fois
    public static Animation curAnimation = null;

    public final static Integer MAX_REGION_SIZE = 65536;

    public static List<Animation> animations = new ArrayList<>();



}
