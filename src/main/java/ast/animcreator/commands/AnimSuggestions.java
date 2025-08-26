package ast.animcreator.commands;

import ast.animcreator.core.Animation;
import ast.animcreator.core.GlobalManager;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.suggestion.SuggestionProvider;
import com.mojang.brigadier.suggestion.Suggestions;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;
import net.minecraft.server.command.ServerCommandSource;
import java.util.concurrent.CompletableFuture;

public class AnimSuggestions implements SuggestionProvider<ServerCommandSource> {

    @Override
    public CompletableFuture<Suggestions> getSuggestions(CommandContext<ServerCommandSource> context, SuggestionsBuilder builder) {
        for (Animation animation : GlobalManager.animations) {
            builder.suggest(animation.name);
        }
        return builder.buildFuture();
    }
}
