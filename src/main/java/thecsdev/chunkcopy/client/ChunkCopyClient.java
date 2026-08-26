package thecsdev.chunkcopy.client;

import org.jetbrains.annotations.Nullable;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.command.v2.ClientCommands;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback;
import thecsdev.chunkcopy.ChunkCopy;
import thecsdev.chunkcopy.client.command.ChunkCopyClientCommand;
import thecsdev.chunkcopy.command.ChunkCopyCommand;

public final class ChunkCopyClient extends ChunkCopy implements ClientModInitializer
{
	private ChunkCopyClientCommand Command = null;
	@Override public void onInitializeClient()
	{
		ClientCommandRegistrationCallback.EVENT.register((dispatcher, buildContext) ->
		{
			Command = new ChunkCopyClientCommand();
			Command.register(dispatcher, buildContext);
		});
	}
	@Override public @Nullable ChunkCopyCommand<?> getCommand() { return Command; }
}
