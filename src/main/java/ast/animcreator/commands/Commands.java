package ast.animcreator.commands;

import ast.animcreator.core.*;
import ast.animcreator.core.enums.State;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.BoolArgumentType;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import net.minecraft.command.CommandRegistryAccess;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.text.Text;

import java.nio.file.Path;

public class Commands {

    public static void registerCommands(CommandDispatcher<ServerCommandSource> dispatcher,
                                        CommandRegistryAccess registryAccess,
                                        CommandManager.RegistrationEnvironment environment)
    {
        //TODO ac_rename
        //     ac_reload
        if (environment.integrated) {
            dispatcher.register(CommandManager.literal("ac_new")
            .requires(source -> source.hasPermissionLevel(2))
            .then(CommandManager.argument("name", StringArgumentType.string())
                .executes(context -> acNewCommand(context, StringArgumentType.getString(context, "name")))));

            dispatcher.register(CommandManager.literal("ac_frame")
                    .requires(source -> source.hasPermissionLevel(2))
                    .then(CommandManager.argument("tick", IntegerArgumentType.integer())
                            .executes(context -> acFrameCommand(context, IntegerArgumentType.getInteger(context, "tick")))));

            dispatcher.register(CommandManager.literal("ac_save")
                    .requires(source -> source.hasPermissionLevel(2))
                    .executes(Commands::acSaveCommand));

            dispatcher.register(CommandManager.literal("ac_show")
                    .requires(source -> source.hasPermissionLevel(2))
                    .executes(context -> acShowCommand(context, (GlobalManager.curAnimation == null ? "" : GlobalManager.curAnimation.name)))
                    .then(CommandManager.argument("name", StringArgumentType.string())
                            .executes(context -> acShowCommand(context, StringArgumentType.getString(context, "name")))));

            dispatcher.register(CommandManager.literal("ac_play")
                    .requires(source -> source.hasPermissionLevel(2))
                    .executes(context -> acPlayCommand(context, (GlobalManager.curAnimation == null ? "" : GlobalManager.curAnimation.name), false))
                    .then(CommandManager.argument("name", StringArgumentType.string())
                            .executes(context -> acPlayCommand(context, StringArgumentType.getString(context, "name"), false))
                            .then(CommandManager.argument("loop", BoolArgumentType.bool())
                                    .executes(context -> acPlayCommand(context, StringArgumentType.getString(context, "name"), BoolArgumentType.getBool(context, "loop"))))));

            dispatcher.register(CommandManager.literal("ac_pause")
                    .requires(source -> source.hasPermissionLevel(2))
                    .executes(context -> acPauseCommand(context, (GlobalManager.curAnimation == null ? "" : GlobalManager.curAnimation.name)))
                    .then(CommandManager.argument("name", StringArgumentType.string())
                            .executes(context -> acPauseCommand(context, StringArgumentType.getString(context, "name")))));

            dispatcher.register(CommandManager.literal("ac_stop")
                    .requires(source -> source.hasPermissionLevel(2))
                    .executes(context -> acStopCommand(context, (GlobalManager.curAnimation == null ? "" : GlobalManager.curAnimation.name)))
                    .then(CommandManager.argument("name", StringArgumentType.string())
                            .executes(context -> acStopCommand(context, StringArgumentType.getString(context, "name")))));
        }
    }

    private static int acNewCommand(CommandContext<ServerCommandSource> context, String animName)
    {
        final ServerCommandSource source = context.getSource();
        String pathString = source.getServer().getRunDirectory().toString();
        source.sendFeedback(() -> Text.literal("Run directory : " + pathString), false);

        if (GlobalManager.state == State.CREATING) {
            source.sendFeedback(() -> Text.literal("Can't create new anim, currently creating one."), false);
            return -1;
        }

        if (FileStorage.animAlreadyExists(animName)) {
            source.sendFeedback(() -> Text.literal(animName + " already exists."), false);
            return -1;
        }

        GlobalManager.player = source.getPlayer();

        GlobalManager.curAnimation = new Animation(animName, GlobalManager.player.getWorld());
        GlobalManager.state = State.CREATING;

        source.sendFeedback(() -> Text.literal("Anim " + animName + " initialized."), false);

        return 0;
    }

    private static int acFrameCommand(CommandContext<ServerCommandSource> context, Integer tick) {
        final ServerCommandSource source = context.getSource();

        if (GlobalManager.curAnimation == null) {
            source.sendFeedback(() -> Text.literal("Not currently creating or modifying an animation."), false);
            return -1;
        }

        if (GlobalManager.curRegion == null) {
            source.sendFeedback(() -> Text.literal("No region defined for current frame."), false);
            return -1;
        }

        if (GlobalManager.curAnimation.frameTickAlreadyExists(tick)) {
            source.sendFeedback(() -> Text.literal("A frame already exists for tick " + tick), false);
            return -1;
        }

        Frame frame = new Frame(GlobalManager.curAnimation, tick, GlobalManager.curRegion);
        GlobalManager.curAnimation.addFrame(frame);

        return 0;
    }

    private static int acSaveCommand(CommandContext<ServerCommandSource> context) {
        final ServerCommandSource source = context.getSource();
        Animation animation = GlobalManager.curAnimation;
        if (animation == null) {
            return -1;
        }
        GlobalManager.state = State.NONE;
        //TODO check que l'anim ait au moins 2 frames
        if (FileStorage.saveAnimToFile(animation, source) != 0) {
            source.sendFeedback(() -> Text.literal("Can not save animation " + animation.name), false);
        }

        return 0;
    }

    private static int acPlayCommand(CommandContext<ServerCommandSource> context, String animName, boolean loop) {
        final ServerCommandSource source = context.getSource();
        Animation animationToPlay = getAnimationFromName(animName, source);
        if (animationToPlay != null) {
            GlobalManager.state = State.PLAYING;
            AnimPlayer.addAnimationToPlay(animationToPlay, loop);
        }
        else {
            return -1;
        }

        return 0;
    }

    private static int acPauseCommand(CommandContext<ServerCommandSource> context, String animName) {
        final ServerCommandSource source = context.getSource();
        Animation anim = getAnimationFromName(animName, source);
        if (anim != null) {
            anim.togglePauseAnimation();
        }

        return 0;
    }

    private static int acStopCommand(CommandContext<ServerCommandSource> context, String animName) {
        //TODO booléen optionnel pour clear la zone au lieu d'afficher la 1e frame ?
        final ServerCommandSource source = context.getSource();
        Animation anim = getAnimationFromName(animName, source);
        if (anim != null) {
            anim.stopAnimation(true);
        }

        return 0;
    }

    private static int acShowCommand(CommandContext<ServerCommandSource> context, String animName) {
        final ServerCommandSource source = context.getSource();
        Animation anim = getAnimationFromName(animName, source);
        if (anim != null) {
            String animStr = anim.toString();
            source.sendFeedback(() -> Text.literal(animStr), false);
        }

        return 0;
    }

    public static Animation getAnimationFromName(String animName, ServerCommandSource source) {
        if (animName.isEmpty()) {
            source.sendFeedback(() -> Text.literal("Not currently creating or modifying an animation."), false);
            return null;
        }
        if (GlobalManager.curAnimation != null && animName.equals(GlobalManager.curAnimation.name)) {
            return GlobalManager.curAnimation;
        }
        else {
            for (Animation animation : GlobalManager.animations) {
                if (animation.name.equals(animName)) {
                    return animation;
                }
            }
        }
        source.sendFeedback(() -> Text.literal("Unknown animation " + animName), false);
        return null;
    }
}

