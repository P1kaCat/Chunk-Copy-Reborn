package thecsdev.chunkcopy.api.data;

import java.io.ByteArrayOutputStream;
import java.io.IOException;

import org.jetbrains.annotations.Nullable;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.resources.Identifier;
import net.minecraft.world.level.ChunkPos;
import thecsdev.chunkcopy.api.io.IOUtils;

public abstract class ChunkDataBlock implements ChunkDataIO
{
	@Nullable
	public static ChunkDataBlock fromId(String cdbId)
	{
		try { return ChunkData.getChunkDataBlockType(cdbId).getConstructor().newInstance(); }
		catch (Exception e) { return null; }
	}

	public final String getIdentifier()
	{
		ChunkDataBlockID id = getClass().getAnnotation(ChunkDataBlockID.class);
		if(id == null) return "null:null";
		return id.namespace() + ":" + id.path();
	}

	public final byte[] getIdentifierByteArray() throws IOException
	{
		ByteArrayOutputStream stream = new ByteArrayOutputStream();
		IOUtils.writeString(stream, getIdentifier());
		byte[] bytes = stream.toByteArray();
		stream.close();
		return bytes;
	}

	public abstract void updateClients(ServerLevel world, ChunkPos chunkPos);
}
