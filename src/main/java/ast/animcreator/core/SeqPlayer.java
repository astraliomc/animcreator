package ast.animcreator.core;

import net.minecraft.server.MinecraftServer;

import java.util.ArrayList;
import java.util.List;

public class SeqPlayer {
    public static List<Sequence> sequencestoPlay = new ArrayList<>();

    public static void onEndTick(MinecraftServer server) {
        for (Sequence sequencePlaying : sequencestoPlay) {
            sequencePlaying.playSequence();
        }
    }
}
