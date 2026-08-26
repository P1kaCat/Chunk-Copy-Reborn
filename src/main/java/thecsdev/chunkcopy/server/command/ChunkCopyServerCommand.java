package thecsdev.chunkcopy.server.command;

import java.util.ArrayList;

import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.network.chat.Component;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.server.permissions.Permissions;
import thecsdev.chunkcopy.api.AutoChunkCopy;
import thecsdev.chunkcopy.api.AutoChunkCopy.ACCMode;
import thecsdev.chunkcopy.api.ChunkCopyAPI;
import thecsdev.chunkcopy.api.ChunkCopyUtils;
import thecsdev.chunkcopy.command.ChunkCopyCommand;

public final class ChunkCopyServerCommand extends ChunkCopyCommand<CommandSourceStack>
{
	@Override public String getCommandName() { return "chunkcopysrv"; }
	@Override protected boolean canChunkCopy(CommandSourceStack cs) { return cs.permissions().hasPermission(Permissions.COMMANDS_OWNER); }
	@Override protected boolean canCopy(CommandSourceStack cs) { return isOpAndHuman(cs); }
	@Override protected boolean canPaste(CommandSourceStack cs) { return isOpAndHuman(cs); }
	@Override protected boolean canConfig(CommandSourceStack cs) { return cs.permissions().hasPermission(Permissions.COMMANDS_OWNER); }

	@Override protected void execMain(CommandSourceStack cs)
	{ cs.sendSuccess(() -> Component.literal("[Chunk Copy] Only operator players can execute this command."), false); }

	@Override protected void copy(CommandSourceStack commandSource, String fileName, int chunkDistance)
	{
		try {
			ServerLevel world = commandSource.getLevel();
			ChunkPos chunkPos = commandSource.getPlayer().chunkPosition();
			ArrayList<ChunkPos> loadedChunks = ChunkCopyUtils.getNearbyLoadedChunks(world, chunkPos, chunkDistance);
			for (ChunkPos cp : loadedChunks) { ChunkCopyAPI.saveChunkDataIO(world, cp, fileName); }
			final int affectedChunks = loadedChunks.size();
			final String msg = String.format("[Chunk Copy] Copied %d chunks to '%s'.", affectedChunks, fileName);
			commandSource.sendSuccess(() -> Component.literal(msg), true);
		} catch (Exception e) { handleException(commandSource, e); }
	}

	@Override protected void paste(CommandSourceStack commandSource, String fileName, int chunkDistance)
	{
		if(!ChunkCopyAPI.getSaveFileDirectory(fileName).exists())
		{ commandSource.sendSuccess(() -> Component.literal(String.format("[Chunk Copy] Unable to paste chunks from '%s', file not found.", fileName)), true); return; }
		try {
			ServerLevel world = commandSource.getLevel();
			ChunkPos chunkPos = commandSource.getPlayer().chunkPosition();
			ArrayList<ChunkPos> loadedChunks = ChunkCopyUtils.getNearbyLoadedChunks(world, chunkPos, chunkDistance);
			int affectedChunks = 0;
			for (ChunkPos cp : loadedChunks) { if(ChunkCopyAPI.loadChunkDataIO(world, cp, fileName)) affectedChunks++; }
			final int finalCount = affectedChunks;
			final String msg = String.format("[Chunk Copy] Pasted %d chunks from '%s'.", finalCount, fileName);
			commandSource.sendSuccess(() -> Component.literal(msg), true);
		} catch (Exception e) { handleException(commandSource, e); }
	}

	@Override protected void fill(CommandSourceStack commandSource, int chunkDistance, BlockState block)
	{
		try {
			ServerLevel world = commandSource.getLevel();
			ChunkPos chunkPos = commandSource.getPlayer().chunkPosition();
			ArrayList<ChunkPos> loadedChunks = ChunkCopyUtils.getNearbyLoadedChunks(world, chunkPos, chunkDistance);
			for (ChunkPos cp : loadedChunks) { ChunkCopyAPI.fillChunkBlocks(world, cp, block); }
			final int affectedChunks = loadedChunks.size();
			final String bn = block.getBlock().getName().getString();
			final String msg = String.format("[Chunk Copy] Filled %d chunks with '%s'.", affectedChunks, bn);
			commandSource.sendSuccess(() -> Component.literal(msg), true);
		} catch (Exception e) { handleException(commandSource, e); }
	}

	@Override protected void autoChunkCopyStart(CommandSourceStack commandSource, String fileName, ACCMode accMode) { autoChunkCopyStop(commandSource); }
	@Override protected void autoChunkCopyStop(CommandSourceStack commandSource)
	{ commandSource.sendSuccess(() -> Component.literal("[Chunk Copy] AutoChunkCopy is not available server-side."), false); AutoChunkCopy.stop(); }

	private static boolean isOpAndHuman(CommandSourceStack src)
	{ try { return src.getPlayer() != null && src.permissions().hasPermission(Permissions.COMMANDS_OWNER); } catch (Exception e) { return false; } }

	private void handleException(CommandSourceStack source, Exception e)
	{ source.sendSuccess(() -> Component.literal("[Chunk Copy] An exception was thrown while executing the command: " + "\n" + getExceptionMessage(e)), true); }
}
