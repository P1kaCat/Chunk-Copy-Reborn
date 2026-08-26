package thecsdev.chunkcopy.api.data;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;

public interface ChunkDataIO
{
	public abstract void copyData(Level world, ChunkPos chunkPos);
	public abstract void pasteData(ServerLevel world, ChunkPos chunkPos);
	public abstract void readData(InputStream stream) throws IOException;
	public abstract void writeData(OutputStream stream) throws IOException;
	public default byte[] toByteArray() throws IOException
	{
		ByteArrayOutputStream stream = new ByteArrayOutputStream();
		writeData(stream);
		byte[] bytes = stream.toByteArray();
		stream.close();
		return bytes;
	}
}
