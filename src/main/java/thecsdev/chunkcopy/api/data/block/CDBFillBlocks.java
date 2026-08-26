package thecsdev.chunkcopy.api.data.block;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.chunk.LevelChunkSection;
import net.minecraft.world.level.chunk.LevelChunk;
import thecsdev.chunkcopy.ChunkCopy;
import thecsdev.chunkcopy.api.data.ChunkDataBlock;
import thecsdev.chunkcopy.api.data.ChunkDataBlockID;
import thecsdev.chunkcopy.api.io.IOUtils;

@ChunkDataBlockID(namespace = ChunkCopy.ModID, path = "fill_blocks")
public class CDBFillBlocks extends ChunkDataBlock
{
	public BlockState state = Blocks.AIR.defaultBlockState();
	@Override public void copyData(Level world, ChunkPos chunkPos) {}
	@Override public void pasteData(ServerLevel world, ChunkPos chunkPos)
	{
		LevelChunk chunk = world.getChunk(chunkPos.x(), chunkPos.z());
		int chunkWidthX = Math.abs(chunkPos.getMaxBlockX() - chunkPos.getMinBlockX()); int chunkWidthZ = Math.abs(chunkPos.getMaxBlockZ() - chunkPos.getMinBlockZ());
		int x = 0, y = chunk.getMinY(), z = 0;
		while(y < chunk.getMaxY() + 1)
		{
			try { LevelChunkSection section = chunk.getSection(chunk.getSectionIndex(y)); section.setBlockState(x, y & 0xF, z, state); } catch(Exception e) { break; }
			x++; if(x > chunkWidthX) { z++; x = 0; if(z > chunkWidthZ) { y++; z = 0; } }
		}
		chunk.markUnsaved();
	}
	@Override public void updateClients(ServerLevel world, ChunkPos chunkPos) { new CDBBlocksLegacy().updateClients(world, chunkPos); }
	@Override public void readData(InputStream stream) throws IOException { state = Block.stateById(IOUtils.readVarInt(stream)); }
	@Override public void writeData(OutputStream stream) throws IOException { IOUtils.writeVarInt(stream, Block.getId(state)); }
}
