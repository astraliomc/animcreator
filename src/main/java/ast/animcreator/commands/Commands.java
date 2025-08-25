package ast.animcreator.commands;

import ast.animcreator.core.*;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.BoolArgumentType;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import net.minecraft.command.CommandRegistryAccess;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.text.Text;
import net.minecraft.util.ActionResult;

import java.util.ArrayList;
import java.util.List;

public class Commands {

    public static void registerCommands(CommandDispatcher<ServerCommandSource> dispatcher,
                                        CommandRegistryAccess registryAccess,
                                        CommandManager.RegistrationEnvironment environment)
    {
        if (environment.integrated) {
            dispatcher.register(CommandManager.literal("ac_new")
            .requires(source -> source.hasPermissionLevel(2))
            .then(CommandManager.argument("name", StringArgumentType.string())
                .executes(context -> acNewCommand(context, StringArgumentType.getString(context, "name")))));

            dispatcher.register(CommandManager.literal("ac_edit")
                    .requires(source -> source.hasPermissionLevel(2))
                    .then(CommandManager.argument("name", StringArgumentType.string())
                            .executes(context -> acEditCommand(context, StringArgumentType.getString(context, "name")))));

            dispatcher.register(CommandManager.literal("ac_rename")
                    .requires(source -> source.hasPermissionLevel(2))
                    .then(CommandManager.argument("old_name", StringArgumentType.string())
                    .then(CommandManager.argument("new_name", StringArgumentType.string())
                            .executes(context -> acRenameCommand(context, StringArgumentType.getString(context, "old_name"), StringArgumentType.getString(context, "new_name"))))));


            dispatcher.register(CommandManager.literal("ac_delete")
                    .requires(source -> source.hasPermissionLevel(2))
                    .then(CommandManager.argument("name", StringArgumentType.string())
                            .executes(context -> acDeleteCommand(context, StringArgumentType.getString(context, "name")))));

            dispatcher.register(CommandManager.literal("ac_discard")
                    .requires(source -> source.hasPermissionLevel(2))
                    .executes(Commands::acDiscardCommand));

            dispatcher.register(CommandManager.literal("ac_frame")
                    .requires(source -> source.hasPermissionLevel(2))
                    .then(CommandManager.argument("tick", IntegerArgumentType.integer())
                            .executes(context -> acFrameCommand(context, IntegerArgumentType.getInteger(context, "tick")))));

            dispatcher.register(CommandManager.literal("ac_rframe")
                    .requires(source -> source.hasPermissionLevel(2))
                    .then(CommandManager.argument("tick", IntegerArgumentType.integer())
                            .executes(context -> acRframeCommand(context, IntegerArgumentType.getInteger(context, "tick")))));

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

            dispatcher.register(CommandManager.literal("ac_resume")
                    .requires(source -> source.hasPermissionLevel(2))
                    .executes(context -> acResumeCommand(context, (GlobalManager.curAnimation == null ? "" : GlobalManager.curAnimation.name)))
                    .then(CommandManager.argument("name", StringArgumentType.string())
                            .executes(context -> acResumeCommand(context, StringArgumentType.getString(context, "name")))));

            dispatcher.register(CommandManager.literal("ac_stop")
                    .requires(source -> source.hasPermissionLevel(2))
                    .executes(context -> acStopCommand(context, (GlobalManager.curAnimation == null ? "" : GlobalManager.curAnimation.name)))
                    .then(CommandManager.argument("name", StringArgumentType.string())
                            .executes(context -> acStopCommand(context, StringArgumentType.getString(context, "name")))));

            dispatcher.register(CommandManager.literal("ac_list")
                    .requires(source -> source.hasPermissionLevel(2))
                    .executes(Commands::acListCommand));
        }
    }

    private static int acNewCommand(CommandContext<ServerCommandSource> context, String animName)
    {
        final ServerCommandSource source = context.getSource();
        if (checkEditedUnsaved(source) != 0) {
            return -1;
        }
        if (FileStorage.animExistsOnDisk(animName)) {
            source.sendFeedback(() -> Text.literal("An animation already exists with name " + animName), false);
            return -1;
        }

        GlobalManager.player = source.getPlayer();
        GlobalManager.curRegion = null;

        GlobalManager.curAnimation = new Animation(animName, GlobalManager.player.getWorld());

        source.sendFeedback(() -> Text.literal("Anim " + animName + " initialized."), false);

        return 0;
    }

    private static int acEditCommand(CommandContext<ServerCommandSource> context, String animName)
    {
        final ServerCommandSource source = context.getSource();
        if (checkEditedUnsaved(source) != 0) {
            return -1;
        }
        Animation animationToEdit = getAnimationFromName(animName, source);
        if (animationToEdit != null) {
            GlobalManager.curAnimation = animationToEdit;
        }
        else {
            return -1;
        }
        source.sendFeedback(() -> Text.literal("Start editing animation " + animName), false);
        return 0;
    }

    private static int acRenameCommand(CommandContext<ServerCommandSource> context, String oldName, String newName) {
        final ServerCommandSource source = context.getSource();
        Animation animationToRename = getAnimationFromName(oldName, source);
        if (animationToRename != null) {
            List<String> errors = new ArrayList<>();
            if (FileStorage.renameAnimFile(oldName, newName, errors)) {
                source.sendFeedback(() -> Text.literal("Successfully renamed animation from " + oldName + " to " + newName), false);
                animationToRename.name = newName;
            }
            else {
                for (String error : errors) {
                    source.sendFeedback(() -> Text.literal(error), false);
                }
                return -1;
            }
        }
        else {
            return -1;
        }
        return 0;
    }

    private static int acDeleteCommand(CommandContext<ServerCommandSource> context, String animName)
    {
        //NOTE maybe force player to type the command twice as a confirmation ?
        final ServerCommandSource source = context.getSource();
        Animation animationToDelete = getAnimationFromName(animName, source);
        if (animationToDelete != null) {
            GlobalManager.removeAnimation(animationToDelete);
            List<String> errors = new ArrayList<>();
            if (FileStorage.deleteAnimFile(animName, errors)) {
                source.sendFeedback(() -> Text.literal("Successfully deleted animation " + animName), false);
            }
            else {
                for (String error : errors) {
                    source.sendFeedback(() -> Text.literal(error), false);
                }
                return -1;
            }
        }
        else {
            return -1;
        }
        return 0;
    }

    private static int acDiscardCommand(CommandContext<ServerCommandSource> context) {
        final ServerCommandSource source = context.getSource();

        if (GlobalManager.curAnimation == null) {
            source.sendFeedback(() -> Text.literal("Not currently creating or modifying an animation."), false);
            return -1;
        }

        if (AnimPlayer.isAnimationPlaying(GlobalManager.curAnimation.name)) {
            source.sendFeedback(() -> Text.literal("Current animation is playing. Pause it or stop it before using this command."), false);
            return -1;
        }

        return discardChanges(source);
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

        if (GlobalManager.curRegion.computeRegionSize() < 2) {
            source.sendFeedback(() -> Text.literal("Current region is empty."), false);
            return -1;
        }

        if (GlobalManager.curAnimation.getFrameForTick(tick) != null) {
            source.sendFeedback(() -> Text.literal("A frame already exists for tick " + tick), false);
            return -1;
        }

        Frame frame = new Frame(GlobalManager.curAnimation, tick, GlobalManager.curRegion);
        GlobalManager.curAnimation.addFrame(frame);
        source.sendFeedback(() -> Text.literal("Frame set for tick " + tick), false);

        return 0;
    }

    private static int acRframeCommand(CommandContext<ServerCommandSource> context, Integer tick) {
        final ServerCommandSource source = context.getSource();

        if (GlobalManager.curAnimation == null) {
            source.sendFeedback(() -> Text.literal("Not currently creating or modifying an animation."), false);
            return -1;
        }

        if (!GlobalManager.curAnimation.removeFrame(tick)) {
            source.sendFeedback(() -> Text.literal("There is no frame for tick " + tick), false);
            return -1;
        }

        source.sendFeedback(() -> Text.literal("Removed frame for tick " + tick), false);
        return 0;
    }

    private static int acSaveCommand(CommandContext<ServerCommandSource> context) {
        final ServerCommandSource source = context.getSource();
        Animation animation = GlobalManager.curAnimation;
        if (animation == null) {
            source.sendFeedback(() -> Text.literal("Not currently creating or modifying an animation."), false);
            return -1;
        }
        //TODO check que l'anim ait au moins 2 frames
        List<String> errors = new ArrayList<>();
        if (FileStorage.saveAnimToFile(animation, false, errors) != 0) {
            source.sendFeedback(() -> Text.literal("Can not save animation " + animation.name), false);
            for (String error : errors) {
                source.sendFeedback(() -> Text.literal(error), false);
            }
            return -1;
        }
        source.sendFeedback(() -> Text.literal("Successfully saved animation " + animation.name), false);
        GlobalManager.addAnimation(animation);

        return 0;
    }

    private static int acPlayCommand(CommandContext<ServerCommandSource> context, String animName, boolean loop) {
        final ServerCommandSource source = context.getSource();
        Animation animationToPlay = getAnimationFromName(animName, source);
        if (animationToPlay != null) {
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
            anim.pauseAnimation();
        }

        return 0;
    }

    private static int acResumeCommand(CommandContext<ServerCommandSource> context, String animName) {
        final ServerCommandSource source = context.getSource();
        Animation anim = getAnimationFromName(animName, source);
        if (anim != null) {
            anim.resumeAnimation();
        }

        return 0;
    }

    private static int acStopCommand(CommandContext<ServerCommandSource> context, String animName) {
        //TODO booléen optionnel pour clear la zone au lieu d'afficher la 1e frame ?
        final ServerCommandSource source = context.getSource();
        Animation anim = getAnimationFromName(animName, source);
        if (anim != null) {
            anim.stopAnimation();
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

    private static int acListCommand(CommandContext<ServerCommandSource> context) {
        final ServerCommandSource source = context.getSource();
        StringBuilder animList = new StringBuilder();
        animList.append("Loaded animations :\n");
        for (Animation animation : GlobalManager.animations) {
            animList.append(animation.name);
            if (AnimPlayer.isAnimationPlaying(animation.name)) {
                animList.append(" (playing) ");
            }
            if (GlobalManager.curAnimation != null && animation.name.equals(GlobalManager.curAnimation.name)) {
                animList.append(" (current) ");
            }
            animList.append("\n");
        }
        source.sendFeedback(() -> Text.literal(animList.toString()), false);
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

    private static int checkEditedUnsaved(ServerCommandSource source) {
        //NOTE le fait que waitingDiscardConfirmation soit un global fait qu'il n'y a pas de distinction
        // entre les différentes commandes qui appellent cette méthode. Par ex si on fait ac_new, qu'on a ce
        // message de warning puis qu'on fait ac_edit, les changements seront perdus.
        // Voir si il faudrait un autre comportement, pour le moment ça me semble ok.
        if (GlobalManager.curAnimation != null && GlobalManager.curAnimation.editedUnsaved) {
            if (!GlobalManager.waitingDiscardConfirmation) {
                source.sendFeedback(() -> Text.literal("Animation " + GlobalManager.curAnimation.name + " was edited but not saved. " +
                        "Retype the command to discard changes, or run '/ac_save' to validate changes."), false);
                GlobalManager.waitingDiscardConfirmation = true;
                return -1;
            }
            else {
                discardChanges(source);
            }
        }
        GlobalManager.waitingDiscardConfirmation = false;
        return 0;
    }

    private static int discardChanges(ServerCommandSource source) {
        String animName = GlobalManager.curAnimation.name;
        GlobalManager.removeAnimation(GlobalManager.curAnimation);
        if (!FileStorage.animExistsOnDisk(animName)) {
            source.sendFeedback(() -> Text.literal("Discarded animation " + animName), false);
        }
        else {
            List<String> errors = new ArrayList<>();
            if (!FileStorage.loadAnimFile(animName, errors)) {
                source.sendFeedback(() -> Text.literal("Failed to load original state of animation " + animName), false);
                for (String err : errors) {
                    source.sendFeedback(() -> Text.literal(err), false);
                }
                return -1;
            }
            source.sendFeedback(() -> Text.literal("Discarded changes made to animation " + animName), false);
            // Get the reloaded animation from GlobalManager.animations from name
            Animation reloadedAnimation = getAnimationFromName(animName, source);
            if (reloadedAnimation != null) {
                GlobalManager.curAnimation = reloadedAnimation;
            }
            else {
                source.sendFeedback(() -> Text.literal("Failed to load original state of animation " + animName), false);
                return -1;
            }
        }
        return 0;
    }
}

