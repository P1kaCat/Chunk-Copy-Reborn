package thecsdev.chunkcopy.api;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;

import org.apache.commons.io.FileUtils;

import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.chunk.EmptyLevelChunk;
import net.minecraft.world.level.chunk.LevelChunk;
import thecsdev.chunkcopy.ChunkCopy;
import thecsdev.chunkcopy.api.data.ChunkData;
import thecsdev.chunkcopy.api.data.block.CDBFillBlocks;
import thecsdev.chunkcopy.api.io.IOUtils;

public final class ChunkCopyAPI
{
	public static final String FILE_EXTENSION = ".bin";

	public static File getSaveFilesDirectory()
	{ return new File(ChunkCopy.getModSavesDirectory().getAbsolutePath() + "/savedChunks/"); }

	public static File getSaveFileDirectory(String fileName)
	{
		fileName = fileName.replaceFirst("\\/.*", "");
		return new File(getSaveFilesDirectory().getAbsolutePath() + "/" + fileName + "/");
	}

	public static File getChunkSaveFile(Level world, ChunkPos chunkPos, String fileName) throws IOException
	{
		String worldIdNamespace = null;
		String worldIdPath = null;
		IOException e = new IOException("Invalid fileName syntax.");
		if(!fileName.matches("[a-zA-Z0-9_\\/]*")) throw e;
		if(fileName.contains("/"))
		{
			if(!fileName.matches("[a-zA-Z0-9_]{1,}\\/[a-zA-Z0-9_]{1,}\\/[a-zA-Z0-9_]{1,}")) throw e;
			String[] fnS = fileName.split("\\/");
			worldIdNamespace = fnS[1];
			worldIdPath = fnS[2];
		}
		else
		{
			worldIdNamespace = world.dimension().identifier().getNamespace();
			worldIdPath = world.dimension().identifier().getPath();
		}
		String a = getSaveFileDirectory(fileName).getAbsolutePath() + "/";
		String b = worldIdNamespace + "/" + worldIdPath + "/";
		String c = chunkPos.getX() + "_" + chunkPos.getZ() + FILE_EXTENSION;
		return new File(a + b + c);
	}

	public static void saveChunkDataIO(Level world, ChunkPos chunkPos, String fileName) throws IOException
	{
		LevelChunk chunk = world.getChunk(chunkPos.getX(), chunkPos.getZ());
		if(chunk == null || (chunk instanceof EmptyLevelChunk)) return;
		File file = getChunkSaveFile(world, chunkPos, fileName);
		file.getParentFile().mkdirs();
		byte[] chunkData = copyChunkData(world, chunkPos, true);
		FileUtils.writeByteArrayToFile(file, chunkData);
	}

	public static boolean loadChunkDataIO(ServerLevel world, ChunkPos chunkPos, String fileName) throws IOException
	{
		File file = getChunkSaveFile(world, chunkPos, fileName);
		if(!file.exists()) return false;
		byte[] chunkData = FileUtils.readFileToByteArray(file);
		pasteChunkData(chunkData, world, chunkPos, true);
		return true;
	}

	public static byte[] copyChunkData(Level world, ChunkPos chunkPos, boolean useCompression) throws IOException
	{
		ChunkData chunkData = new ChunkData();
		chunkData.copyData(world, chunkPos);
		byte[] bytes = chunkData.toByteArray();
		if(useCompression) bytes = IOUtils.gzipCompressBytes(bytes);
		return bytes;
	}

	public static void pasteChunkData(byte[] data, ServerLevel world, ChunkPos chunkPos, boolean isCompressed) throws IOException
	{
		if(isCompressed) data = IOUtils.gzipDecompressBytes(data);
		ByteArrayInputStream stream = new ByteArrayInputStream(data);
		ChunkData chunkData = new ChunkData();
		chunkData.readData(stream);
		stream.close();
		chunkData.pasteData(world, chunkPos);
	}

	public static void fillChunkBlocks(ServerLevel world, ChunkPos chunkPos, BlockState block)
	{
		CDBFillBlocks f = new CDBFillBlocks();
		f.state = block;
		f.pasteData(world, chunkPos);
		f.updateClients(world, chunkPos);
	}

	public static void clearChunkBlocks(ServerLevel world, ChunkPos chunkPos)
	{ fillChunkBlocks(world, chunkPos, Blocks.AIR.defaultBlockState()); }
}
