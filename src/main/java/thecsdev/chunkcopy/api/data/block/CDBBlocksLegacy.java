package thecsdev.chunkcopy.api.data.block;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.BitSet;

import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.network.protocol.game.ClientboundLevelChunkWithLightPacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.chunk.LevelChunkSection;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.level.lighting.LevelLightEngine;
import thecsdev.chunkcopy.ChunkCopy;
import thecsdev.chunkcopy.api.data.ChunkDataBlock;
import thecsdev.chunkcopy.api.data.ChunkDataBlockID;
import thecsdev.chunkcopy.api.io.IOUtils;

@ChunkDataBlockID(namespace = ChunkCopy.ModID, path = "blocks_legacy")
public class CDBBlocksLegacy extends ChunkDataBlock
{
	public int StartY = 0;
	public final ArrayList<Integer> BlockIDs = new ArrayList<>();

	@Deprecated(forRemoval = true, since = "v2.0.0")
	@Override
	public void copyData(Level world, ChunkPos chunkPos)
	{
		LevelChunk chunk = world.getChunk(chunkPos.x, chunkPos.z);
		int chunkWidthX = Math.abs(chunkPos.getMaxBlockX() - chunkPos.getMinBlockX());
		int chunkWidthZ = Math.abs(chunkPos.getMaxBlockZ() - chunkPos.getMinBlockZ());
		StartY = chunk.getMinY();
		BlockIDs.clear();
		int x = 0, y = chunk.getMinY(), z = 0;
		while(y < chunk.getMaxY() + 1)
		{
			BlockPos blockPos = chunk.getPos().getBlockAt(x, y, z);
			BlockState blockState = chunk.getBlockState(blockPos);
			BlockIDs.add(Block.getId(blockState));
			x++;
			if(x > chunkWidthX) { z++; x = 0; if(z > chunkWidthZ) { y++; z = 0; } }
		}
	}

	@Deprecated(forRemoval = true, since = "v2.0.0")
	@Override
	public void pasteData(ServerLevel world, ChunkPos chunkPos)
	{
		LevelChunk chunk = world.getChunk(chunkPos.x, chunkPos.z);
		int chunkWidthX = Math.abs(chunkPos.getMaxBlockX() - chunkPos.getMinBlockX());
		int chunkWidthZ = Math.abs(chunkPos.getMaxBlockZ() - chunkPos.getMinBlockZ());
		int x = 0, y = StartY, z = 0;
		for (int blockID : BlockIDs)
		{
			if(y > chunk.getMaxY()) break;
			try
			{
				do {
					if(y < chunk.getMinY() || y > chunk.getMaxY()) break;
					BlockState state = Block.stateById(blockID);
					if(state.hasBlockEntity()) break;
					LevelChunkSection toChunkSection = chunk.getSection(chunk.getSectionIndex(y));
					toChunkSection.setBlockState(x, y & 0xF, z, state);
				} while(false);
			}
			catch (Exception e) { break; }
			x++;
			if(x > chunkWidthX) { z++; x = 0; if(z > chunkWidthZ) { y++; z = 0; } }
		}
		chunk.markUnsaved();
	}

	@Override
	public void updateClients(ServerLevel world, ChunkPos chunkPos)
	{
		ClientboundLevelChunkWithLightPacket chunkData = makeMeAChunkDataPacketPls(world, chunkPos);
		world.players().forEach(p -> p.connection.send(chunkData));
	}

	@Override
	public void readData(InputStream stream) throws IOException
	{
		StartY = IOUtils.readVarInt(stream);
		BlockIDs.clear();
		while(stream.available() > 0)
		{
			try { BlockIDs.add(IOUtils.readVarInt(stream)); }
			catch(IOException e) {}
		}
	}

	@Override
	public void writeData(OutputStream stream) throws IOException
	{
		IOUtils.writeVarInt(stream, StartY);
		for (Integer blockId : BlockIDs) { IOUtils.writeVarInt(stream, blockId); }
	}

	private static ClientboundLevelChunkWithLightPacket makeMeAChunkDataPacketPls(Level world, ChunkPos chunkPos)
	{
		LevelChunk wchunk = world.getChunk(chunkPos.x, chunkPos.z);
		LevelLightEngine lp = world.getLightEngine();
		BitSet skyBits = new BitSet(0);
		BitSet blockBits = new BitSet(0);
		return new ClientboundLevelChunkWithLightPacket(wchunk, lp, skyBits, blockBits);
	}
}
