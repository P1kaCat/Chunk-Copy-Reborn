package thecsdev.chunkcopy.client.command;

import java.util.ArrayList;

import net.fabricmc.fabric.api.client.command.v2.FabricClientCommandSource;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.network.chat.Component;
import net.minecraft.world.level.ChunkPos;
import thecsdev.chunkcopy.api.AutoChunkCopy;
import thecsdev.chunkcopy.api.AutoChunkCopy.ACCMode;
import thecsdev.chunkcopy.api.ChunkCopyAPI;
import thecsdev.chunkcopy.api.ChunkCopyUtils;
import thecsdev.chunkcopy.command.ChunkCopyCommand;

public final class ChunkCopyClientCommand extends ChunkCopyCommand<FabricClientCommandSource>
{
	@Override public String getCommandName() { return "chunkcopy"; }
	@Override protected boolean canChunkCopy(FabricClientCommandSource cs) { return true; }
	@Override protected boolean canCopy(FabricClientCommandSource cs) { return true; }
	@Override protected boolean canPaste(FabricClientCommandSource cs) { return Minecraft.getInstance().isLocalServer(); }
	@Override protected boolean canConfig(FabricClientCommandSource cs) { return true; }

	@Override protected void execMain(FabricClientCommandSource cs)
	{ cs.sendFeedback(Component.translatable("chunkcopy.feedback.syntax.copypaste")); }

	@Override protected void copy(FabricClientCommandSource commandSource, String fileName, int chunkDistance)
	{ copy(commandSource, fileName, chunkDistance, true); }

	public void copy(FabricClientCommandSource commandSource, String fileName, int chunkDistance, boolean sendFeedback)
	{
		ClientLevel world = commandSource.getLevel();
		ChunkPos chunkPos = commandSource.getPlayer().chunkPosition();
		ArrayList<ChunkPos> loadedChunks = ChunkCopyUtils.getNearbyLoadedChunks(world, chunkPos, chunkDistance);
		int affectedChunks = 0;
		try { for (ChunkPos cp : loadedChunks) { ChunkCopyAPI.saveChunkDataIO(world, cp, fileName); affectedChunks++; } }
		catch (Exception e) { handleException(commandSource, e); return; }
		if(sendFeedback) commandSource.sendFeedback(Component.translatable("chunkcopy.feedback.copied", affectedChunks, fileName));
	}

	@Override protected void paste(FabricClientCommandSource commandSource, String fileName, int chunkDistance)
	{ paste(commandSource, fileName, chunkDistance, true); }

	protected void paste(FabricClientCommandSource commandSource, String fileName, int chunkDistance, boolean sendFeedback)
	{
		Minecraft mc = Minecraft.getInstance();
		if(!requireSingleplayer(commandSource)) return;
		if(!ChunkCopyAPI.getSaveFileDirectory(fileName).exists())
		{ commandSource.sendFeedback(Component.translatable("chunkcopy.feedback.paste_file_not_found", new Object[] { fileName })); return; }
		ServerLevel world = mc.getSingleplayerServer().getLevel(mc.level.dimension());
		ChunkPos chunkPos = commandSource.getPlayer().chunkPosition();
		ArrayList<ChunkPos> loadedChunks = ChunkCopyUtils.getNearbyLoadedChunks(world, chunkPos, chunkDistance);
		int affectedChunks = 0;
		try { for (ChunkPos cp : loadedChunks) { if(ChunkCopyAPI.loadChunkDataIO(world, cp, fileName)) affectedChunks++; } }
		catch (Exception e) { handleException(commandSource, e); return; }
		if(sendFeedback) commandSource.sendFeedback(Component.translatable("chunkcopy.feedback.pasted", affectedChunks, fileName));
	}

	@Override protected void fill(FabricClientCommandSource commandSource, int chunkDistance, BlockState block)
	{
		Minecraft mc = Minecraft.getInstance();
		if(!requireSingleplayer(commandSource)) return;
		ServerLevel world = mc.getSingleplayerServer().getLevel(mc.level.dimension());
		ChunkPos chunkPos = commandSource.getPlayer().chunkPosition();
		ArrayList<ChunkPos> loadedChunks = ChunkCopyUtils.getNearbyLoadedChunks(world, chunkPos, chunkDistance);
		int affectedChunks = 0;
		try { for (ChunkPos cp : loadedChunks) { ChunkCopyAPI.fillChunkBlocks(world, cp, block); affectedChunks++; } }
		catch (Exception e) { handleException(commandSource, e); return; }
		String bn = block.getBlock().getName().getString();
		commandSource.sendFeedback(Component.translatable("chunkcopy.feedback.filled", affectedChunks, bn));
	}

	@Override protected void autoChunkCopyStart(FabricClientCommandSource commandSource, String fileName, ACCMode accMode)
	{
		AutoChunkCopy.start(fileName, accMode);
		if(accMode == ACCMode.Copying) { copy(commandSource, fileName, 8, false); commandSource.sendFeedback(Component.translatable("chunkcopy.feedback.autochunkcopy.start_copying", fileName)); }
		else if(accMode == ACCMode.Pasting) { paste(commandSource, fileName, 8, false); commandSource.sendFeedback(Component.translatable("chunkcopy.feedback.autochunkcopy.start_pasting", fileName)); }
	}

	@Override protected void autoChunkCopyStop(FabricClientCommandSource commandSource)
	{ AutoChunkCopy.stop(); commandSource.sendFeedback(Component.translatable("chunkcopy.feedback.autochunkcopy.stop")); }

	private boolean requireSingleplayer(FabricClientCommandSource cmdSrc)
	{
		Minecraft mc = Minecraft.getInstance();
		if(!mc.isLocalServer()) { cmdSrc.sendFeedback(Component.translatable("chunkcopy.feedback.require_singleplayer")); return false; }
		return true;
	}

	private void handleException(FabricClientCommandSource source, Exception e)
	{ source.sendFeedback(Component.translatable("chunkcopy.feedback.command_exception", new Object[] { "\n" + getExceptionMessage(e) })); }
}
