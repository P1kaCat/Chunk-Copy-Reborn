package thecsdev.chunkcopy;

import java.io.File;

import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import net.fabricmc.api.EnvType;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerChunkEvents;
import net.minecraft.world.level.ChunkPos;
import thecsdev.chunkcopy.api.AutoChunkCopy;
import thecsdev.chunkcopy.api.ChunkCopyAPI;
import thecsdev.chunkcopy.api.config.ChunkCopyConfig;
import thecsdev.chunkcopy.api.data.ChunkData;
import thecsdev.chunkcopy.api.data.block.CDBChunkSections;
import thecsdev.chunkcopy.api.data.block.CDBEntitiesLegacy;
import thecsdev.chunkcopy.api.data.block.CDBEntityBlocksLegacy;
import thecsdev.chunkcopy.client.ChunkCopyClient;
import thecsdev.chunkcopy.command.ChunkCopyCommand;
import thecsdev.chunkcopy.server.ChunkCopyServer;

public abstract class ChunkCopy
{
	private static ChunkCopy Instance = null;
	public static final Logger LOGGER = LoggerFactory.getLogger(getModID());
	public static final String ModName = "Chunk Copy";
	public static final String ModID = "chunkcopy";
	public static final int FileVersion = 759;

	public ChunkCopy()
	{
		if(validateInstance()) throw new RuntimeException("An instance of ChunkCopy already exists.");
		Instance = this;
		LOGGER.info("Initializing '" + getModName() + "' as '" + getClass().getSimpleName() + "'.");
		ChunkCopyConfig.KEYS.add(ChunkCopyConfig.PASTE_ENTITIES);
		ChunkData.registerChunkDataBlockType(CDBChunkSections.class);
		ChunkData.registerChunkDataBlockType(CDBEntityBlocksLegacy.class);
		ChunkData.registerChunkDataBlockType(CDBEntitiesLegacy.class);

		ServerChunkEvents.CHUNK_LOAD.register((sWorld, sChunk, generated) ->
		{
			if(!AutoChunkCopy.isPasting()) return;
			final ChunkPos scPos = sChunk.getPos();
			final Runnable task = () -> { try { ChunkCopyAPI.loadChunkDataIO(sWorld, scPos, AutoChunkCopy.getFileName()); } catch(Exception exc) {} };
			new Thread(() -> { try { Thread.sleep(500); while(!sWorld.hasChunk(scPos.getX(), scPos.getZ())) Thread.sleep(100); task.run(); } catch(Exception e) {} }).start();
		});
	}

	public static String getModName() { return ModName; }
	public static String getModID() { return ModID; }
	@Nullable public static ChunkCopy getInstance() { return Instance; }
	public static boolean validateInstance() { return Instance != null && (Instance instanceof ChunkCopyClient || Instance instanceof ChunkCopyServer); }
	public static EnvType getEnviroment()
	{
		if(!validateInstance()) throw new RuntimeException("Uninitialized mod.");
		if(Instance instanceof ChunkCopyClient) return EnvType.CLIENT;
		if(Instance instanceof ChunkCopyServer) return EnvType.SERVER;
		throw new RuntimeException("If you are reading this, something went terribly wrong.");
	}
	public static File getModSavesDirectory() { return new File(getRunDirectory().getAbsolutePath() + "/mods/" + ModID + "/"); }
	public static File getRunDirectory() { return new File(System.getProperty("user.dir")); }
	public static boolean isServer() { return getEnviroment() == EnvType.SERVER; }
	public static boolean isClient() { return getEnviroment() == EnvType.CLIENT; }
	@Nullable public abstract ChunkCopyCommand<?> getCommand();
}
