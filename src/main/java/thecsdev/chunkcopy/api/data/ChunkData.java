package thecsdev.chunkcopy.api.data;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Arrays;
import java.util.HashSet;

import org.jetbrains.annotations.Nullable;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import thecsdev.chunkcopy.ChunkCopy;
import thecsdev.chunkcopy.api.data.block.CDBBlocksLegacy;
import thecsdev.chunkcopy.api.data.block.CDBEntitiesLegacy;
import thecsdev.chunkcopy.api.data.block.CDBEntityBlocksLegacy;
import thecsdev.chunkcopy.api.io.IOUtils;

public final class ChunkData implements ChunkDataIO
{
	public final HashSet<ChunkDataBlock> ChunkDataBlocks = new HashSet<>();

	@Override
	public void copyData(Level world, ChunkPos chunkPos)
	{
		ChunkDataBlocks.clear();
		for (Class<? extends ChunkDataBlock> cdbType : ChunkDataBlockTypes)
			try
			{
				ChunkDataBlock cdb = cdbType.getConstructor().newInstance();
				cdb.copyData(world, chunkPos);
				ChunkDataBlocks.add(cdb);
			}
			catch (Exception e) { e.printStackTrace(); }
	}

	@Override
	public void pasteData(ServerLevel world, ChunkPos chunkPos)
	{
		world.getServer().execute(() ->
		{
			for (ChunkDataBlock chunkDataBlock : ChunkDataBlocks)
				try
				{
					chunkDataBlock.pasteData(world, chunkPos);
					chunkDataBlock.updateClients(world, chunkPos);
				}
				catch(Exception e) {}
		});
	}

	@Override
	public void readData(InputStream stream) throws IOException
	{
		ChunkDataBlocks.clear();
		byte[] modIdBytes = ChunkCopy.ModID.getBytes("ASCII");
		if(Arrays.compare(modIdBytes, stream.readNBytes(modIdBytes.length)) != 0)
		{
			stream.reset();
			readData_legacy(stream);
			return;
		}
		int fileVersion = IOUtils.readVarInt(stream);
		if(fileVersion != ChunkCopy.FileVersion)
		{
			throw new IOException("Unable to read and paste chunk data because it was saved using a "
					+ "different file or game version. Please use that version to paste the chunk data.");
		}
		ByteArrayInputStream chunkDataStream = new ByteArrayInputStream(IOUtils.readByteArray(stream));
		while(chunkDataStream.available() > 0)
		{
			byte[] cdbBytes = IOUtils.readByteArray(chunkDataStream);
			ByteArrayInputStream cdbStream = new ByteArrayInputStream(cdbBytes);
			String cdbId = IOUtils.readString(cdbStream);
			ChunkDataBlock cdb = ChunkDataBlock.fromId(cdbId);
			if(cdb == null) { cdbStream.close(); continue; }
			try { cdb.readData(cdbStream); } catch(IOException e) {}
			ChunkDataBlocks.add(cdb);
			cdbStream.close();
		}
		chunkDataStream.close();
	}

	private void readData_legacy(InputStream stream) throws IOException
	{
		while(stream.available() > 0)
		{
			int chunkId = IOUtils.readVarInt(stream);
			byte[] chunkData = IOUtils.readByteArray(stream);
			ChunkDataBlock cdb = null;
			if(chunkId == 1) cdb = new CDBBlocksLegacy();
			else if(chunkId == 2) cdb = new CDBEntityBlocksLegacy();
			else if(chunkId == 3) cdb = new CDBEntitiesLegacy();
			else continue;
			ByteArrayInputStream chunkStream = new ByteArrayInputStream(chunkData);
			cdb.readData(chunkStream);
			chunkStream.close();
			ChunkDataBlocks.add(cdb);
		}
	}

	@Override
	public void writeData(OutputStream stream) throws IOException
	{
		stream.write(ChunkCopy.ModID.getBytes("ASCII"));
		IOUtils.writeVarInt(stream, ChunkCopy.FileVersion);
		ByteArrayOutputStream chunkDataStream = new ByteArrayOutputStream();
		for (ChunkDataBlock cdb : ChunkDataBlocks)
		{
			byte[] cdbIdBytes = cdb.getIdentifierByteArray();
			byte[] cdbBytes = new byte[0];
			try { cdbBytes = cdb.toByteArray(); } catch (IOException e) {}
			IOUtils.writeVarInt(chunkDataStream, cdbIdBytes.length + cdbBytes.length);
			chunkDataStream.write(cdbIdBytes);
			chunkDataStream.write(cdbBytes);
		}
		IOUtils.writeByteArray(stream, chunkDataStream.toByteArray());
		chunkDataStream.close();
	}

	protected static final HashSet<Class<? extends ChunkDataBlock>> ChunkDataBlockTypes = new HashSet<>();

	public static <T extends ChunkDataBlock> boolean registerChunkDataBlockType(Class<T> type)
	{
		String log = "Registering chunk data block '" + type.getSimpleName() + "': %s, %s.";
		boolean a = type.isAnnotationPresent(ChunkDataBlockID.class);
		boolean b = IOUtils.classHasParameterlessConstructor(type);
		boolean result = a && b;
		if(result) ChunkDataBlockTypes.add(type);
		ChunkCopy.LOGGER.info(String.format(log, Boolean.toString(a), Boolean.toString(b)));
		return result;
	}

	public static <T extends ChunkDataBlock> boolean unregisterChunkDataBlockType(Class<T> type)
	{ return ChunkDataBlockTypes.remove(type); }

	@Nullable
	public static Class<? extends ChunkDataBlock> getChunkDataBlockType(String identifier)
	{
		try
		{
			return ChunkData.ChunkDataBlockTypes.stream().filter(i ->
			{
				ChunkDataBlockID iId = i.getAnnotation(ChunkDataBlockID.class);
				if(iId == null) return false;
				return (iId.namespace() + ":" + iId.path()).equals(identifier);
			}).findFirst().get();
		}
		catch (Exception e) { return null; }
	}
}
