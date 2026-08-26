package thecsdev.chunkcopy.api;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

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
		String c = chunkPos.x() + "_" + chunkPos.z() + FILE_EXTENSION;
		return new File(a + b + c);
	}

	public static void saveChunkDataIO(Level world, ChunkPos chunkPos, String fileName) throws IOException
	{
		LevelChunk chunk = world.getChunk(chunkPos.x(), chunkPos.z());
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

	/**
	 * Recursively lists all saved chunk files (.bin) under the given directory.
	 */
	private static List<File> listChunkFiles(File dir)
	{
		List<File> files = new ArrayList<>();
		File[] children = dir.listFiles();
		if(children == null) return files;
		for (File child : children)
		{
			if(child.isDirectory()) files.addAll(listChunkFiles(child));
			else if(child.getName().endsWith(FILE_EXTENSION)) files.add(child);
		}
		return files;
	}

	/**
	 * Returns all saved chunk positions for a given fileName, scanning
	 * across all dimensions/subdirectories.
	 */
	public static List<ChunkPos> getAllSavedChunkPositions(String fileName)
	{
		List<ChunkPos> positions = new ArrayList<>();
		File saveDir = getSaveFileDirectory(fileName);
		if(!saveDir.exists()) return positions;
		for (File chunkFile : listChunkFiles(saveDir))
		{
			String name = chunkFile.getName().replace(FILE_EXTENSION, "");
			String[] parts = name.split("_");
			if(parts.length != 2) continue;
			try
			{
				int x = Integer.parseInt(parts[0]);
				int z = Integer.parseInt(parts[1]);
				positions.add(new ChunkPos(x, z));
			}
			catch(NumberFormatException e) {}
		}
		return positions;
	}

	/**
	 * Pastes ALL saved chunks from the given fileName into the target world at once.
	 * Each chunk file is read directly and pasted into the world, regardless of
	 * which dimension the chunks were originally saved from.
	 * @return The number of chunks successfully pasted.
	 */
	public static int pasteAllChunks(ServerLevel world, String fileName) throws IOException
	{
		File saveDir = getSaveFileDirectory(fileName);
		if(!saveDir.exists()) return 0;
		int count = 0;
		for (File chunkFile : listChunkFiles(saveDir))
		{
			String name = chunkFile.getName().replace(FILE_EXTENSION, "");
			String[] parts = name.split("_");
			if(parts.length != 2) continue;
			try
			{
				int x = Integer.parseInt(parts[0]);
				int z = Integer.parseInt(parts[1]);
				ChunkPos chunkPos = new ChunkPos(x, z);
				byte[] chunkData = FileUtils.readFileToByteArray(chunkFile);
				pasteChunkData(chunkData, world, chunkPos, true);
				count++;
			}
			catch(Exception e) {}
		}
		return count;
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
