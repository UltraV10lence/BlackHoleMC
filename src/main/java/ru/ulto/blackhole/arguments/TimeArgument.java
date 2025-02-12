package ru.ulto.blackhole.arguments;

import com.mojang.brigadier.StringReader;
import com.mojang.brigadier.arguments.ArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.SimpleCommandExceptionType;
import com.mojang.brigadier.suggestion.Suggestions;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;
import net.minecraft.text.Text;

import java.util.Collection;
import java.util.List;
import java.util.concurrent.CompletableFuture;

public class TimeArgument implements ArgumentType<Integer> {
    @Override
    public Integer parse(StringReader stringReader) throws CommandSyntaxException {
        var time = stringReader.readInt();

        char unit = '-';
        if (stringReader.canRead()) {
            var c = stringReader.read();
            if (Character.isLetter(c)) unit = c;
        }

        switch (unit) {
            case 't':
                break;
            case 's':
                time *= 20;
                break;
            case 'm':
                time *= 20 * 60;
                break;
            case 'h':
                time *= 20 * 60 * 60;
                break;
            default:
                var msg = Text.literal("Cannot define time unit " + unit + ". Expected: t(ick), s(econd), m(inute), h(our)");
                throw new CommandSyntaxException(new SimpleCommandExceptionType(msg), msg);
        }

        if (time < 0) {
            var msg = Text.literal("Number is too big");
            throw new CommandSyntaxException(new SimpleCommandExceptionType(msg), msg);
        }
        return time;
    }

    @Override
    public <S> CompletableFuture<Suggestions> listSuggestions(CommandContext<S> context, SuggestionsBuilder builder) {
        if (isNumber(builder.getRemainingLowerCase())) {
            builder.suggest(builder.getRemainingLowerCase() + 'h');
            builder.suggest(builder.getRemainingLowerCase() + 'm');
            builder.suggest(builder.getRemainingLowerCase() + 's');
            builder.suggest(builder.getRemainingLowerCase() + 't');
        }

        return builder.buildFuture();
    }

    public boolean isNumber(String str) {
        if (str.isEmpty()) return false;
        for (var c : str.toCharArray()) {
            if (!Character.isDigit(c)) return false;
        }

        return true;
    }

    @Override
    public Collection<String> getExamples() {
        return List.of("1h", "60m", "3600s", "72000t");
    }
}
