package thecsdev.chunkcopy.api.data.block;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;

import io.netty.buffer.Unpooled;
import net.fabricmc.fabric.api.networking.v1.PacketByteBufs;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.chunk.LevelChunkSection;
import thecsdev.chunkcopy.ChunkCopy;
import thecsdev.chunkcopy.api.data.ChunkDataBlock;
import thecsdev.chunkcopy.api.data.ChunkDataBlockID;
import thecsdev.chunkcopy.api.io.IOUtils;
import thecsdev.chunkcopy.api.io.Tuple;

@ChunkDataBlockID(namespace = ChunkCopy.ModID, path = "chunk_sections")
public class CDBChunkSections extends ChunkDataBlock
{
	public final ArrayList<Tuple<Integer, FriendlyByteBuf>> ChunkSectionData = new ArrayList<>();

	@Override
	public void copyData(Level world, ChunkPos chunkPos)
	{
		ChunkSectionData.clear();
		ChunkAccess chunk = world.getChunk(chunkPos.x, chunkPos.z);
		LevelChunkSection[] sections = chunk.getSections();
		for (int i = 0; i < sections.length; i++)
		{
			FriendlyByteBuf pbb = PacketByteBufs.create();
			sections[i].write(pbb);
			int yOffset = chunk.getMinY() + (i * 16);
			ChunkSectionData.add(new Tuple<>(yOffset, pbb));
		}
	}

	@Override
	public void pasteData(ServerLevel world, ChunkPos chunkPos)
	{
		ChunkAccess chunk = world.getChunk(chunkPos.x, chunkPos.z);
		for (Tuple<Integer, FriendlyByteBuf> pbb : ChunkSectionData)
		{
			LevelChunkSection cs = chunk.getSection(chunk.getSectionIndex(pbb.Item1));
			cs.read(pbb.Item2);
		}
		chunk.markUnsaved();
	}

	@Override
	public void updateClients(ServerLevel world, ChunkPos chunkPos)
	{ new CDBBlocksLegacy().updateClients(world, chunkPos); }

	@Override
	public void readData(InputStream stream) throws IOException
	{
		ChunkSectionData.clear();
		while(stream.available() > 0)
		{
			int len = IOUtils.readVarInt(stream);
			byte[] pbbBytes = stream.readNBytes(len);
			ByteArrayInputStream pbbStream = new ByteArrayInputStream(pbbBytes);
			int offsetY = IOUtils.readVarInt(pbbStream);
			FriendlyByteBuf pbb = PacketByteBufs.copy(Unpooled.copiedBuffer(IOUtils.readByteArray(pbbStream)));
			pbbStream.close();
			ChunkSectionData.add(new Tuple<>(offsetY, pbb));
		}
	}

	@Override
	public void writeData(OutputStream stream) throws IOException
	{
		for (Tuple<Integer, FriendlyByteBuf> pbb : ChunkSectionData)
		{
			ByteArrayOutputStream pbbStream = new ByteArrayOutputStream();
			byte[] pbbCsBytes = new byte[pbb.Item2.readableBytes()];
			pbb.Item2.readBytes(pbbCsBytes);
			IOUtils.writeVarInt(pbbStream, pbb.Item1);
			IOUtils.writeByteArray(pbbStream, pbbCsBytes);
			IOUtils.writeByteArray(stream, pbbStream.toByteArray());
			pbbStream.close();
		}
	}
}
