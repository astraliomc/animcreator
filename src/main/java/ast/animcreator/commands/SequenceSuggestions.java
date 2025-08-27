package ast.animcreator.commands;

import ast.animcreator.core.Animation;
import ast.animcreator.core.GlobalManager;
import ast.animcreator.core.SeqPlayer;
import ast.animcreator.core.Sequence;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.suggestion.SuggestionProvider;
import com.mojang.brigadier.suggestion.Suggestions;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;
import net.minecraft.server.command.ServerCommandSource;

import java.util.concurrent.CompletableFuture;

public class SequenceSuggestions implements SuggestionProvider<ServerCommandSource> {

    @Override
    public CompletableFuture<Suggestions> getSuggestions(CommandContext<ServerCommandSource> context, SuggestionsBuilder builder) {
        for (Sequence sequence : SeqPlayer.sequencestoPlay) {
            builder.suggest(sequence.name);
        }
        return builder.buildFuture();
    }
}
