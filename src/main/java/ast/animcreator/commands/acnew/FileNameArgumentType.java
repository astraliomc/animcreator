package ast.animcreator.commands.acnew;

import com.mojang.brigadier.StringReader;
import com.mojang.brigadier.arguments.ArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.SimpleCommandExceptionType;
import com.mojang.brigadier.suggestion.Suggestions;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.text.Text;

import java.util.Arrays;
import java.util.Collection;
import java.util.concurrent.CompletableFuture;

public class FileNameArgumentType implements ArgumentType<String> {
    private static final Collection<String> EXAMPLES = Arrays.asList("my_cool_anim", "snake", "parkour1");
    public static final SimpleCommandExceptionType INVALID_FILENAME_EXCEPTION =
            new SimpleCommandExceptionType(Text.translatable("argument.animcreator.filename.invalid"));
    private static final char[] ILLEGAL_CHARACTERS = { '/', '\n', '\r', '\t', '\0', '\f', '`', '?', '*', '\\', '<', '>', '|', '\"', ':' };

    public static FileNameArgumentType filename() {
        return new FileNameArgumentType();
    }

    @Override
    public String parse(StringReader reader) throws CommandSyntaxException {
        String arg = reader.getString();
        for (char illegalCharacter : ILLEGAL_CHARACTERS) {
            if (arg.contains(String.valueOf(illegalCharacter))) {
                throw INVALID_FILENAME_EXCEPTION.createWithContext(reader);
            }
        }
        return reader.getString();
    }

    @Override
    public <S> CompletableFuture<Suggestions> listSuggestions(CommandContext<S> context, SuggestionsBuilder builder) {
        return ArgumentType.super.listSuggestions(context, builder);
    }

    @Override
    public Collection<String> getExamples() {
        return EXAMPLES;
    }

    public static String getFilename(CommandContext<ServerCommandSource> context) {
        return context.getArgument("filename", String.class);
    }
}
