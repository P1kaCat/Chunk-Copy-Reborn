package thecsdev.chunkcopy.command.argument;

import java.util.concurrent.CompletableFuture;

import com.mojang.brigadier.StringReader;
import com.mojang.brigadier.arguments.ArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.suggestion.Suggestions;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;

import net.minecraft.commands.arguments.IdentifierArgument;
import net.minecraft.resources.Identifier;
import thecsdev.chunkcopy.api.config.ChunkCopyConfig;
import thecsdev.chunkcopy.api.config.ConfigKey;

@Deprecated
public final class ConfigKeyArgumentType implements ArgumentType<Identifier>
{
	private static final IdentifierArgument IAT = IdentifierArgument.id();

	protected ConfigKeyArgumentType() {}
	public static ConfigKeyArgumentType configKeyId() { return new ConfigKeyArgumentType(); }

	@Override
	public Identifier parse(StringReader reader) throws CommandSyntaxException { return IAT.parse(reader); }

	@Override
	public <S> CompletableFuture<Suggestions> listSuggestions(CommandContext<S> context, SuggestionsBuilder builder)
	{
		for (ConfigKey<?> configKey : ChunkCopyConfig.KEYS) builder.suggest(configKey.keyName.toString());
		return builder.buildFuture();
	}
}
