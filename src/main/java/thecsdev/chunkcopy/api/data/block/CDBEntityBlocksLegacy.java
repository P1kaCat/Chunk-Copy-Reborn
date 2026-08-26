package thecsdev.chunkcopy.api.data.block;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;

import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.nbt.TagParser;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.level.storage.TagValueInput;
import net.minecraft.util.ProblemReporter;
import thecsdev.chunkcopy.ChunkCopy;
import thecsdev.chunkcopy.api.data.ChunkDataBlock;
import thecsdev.chunkcopy.api.data.ChunkDataBlockID;
import thecsdev.chunkcopy.api.io.IOUtils;

@ChunkDataBlockID(namespace = ChunkCopy.ModID, path = "entity_blocks_legacy")
public class CDBEntityBlocksLegacy extends ChunkDataBlock
{
	public final ArrayList<CDBEntityBlock> BlockEntities = new ArrayList<>();

	@Override public void copyData(Level world, ChunkPos chunkPos)
	{
		BlockEntities.clear();
		LevelChunk chunk = world.getChunk(chunkPos.x(), chunkPos.z());
		for (BlockPos eBlockPos : chunk.getBlockEntitiesPos())
		{
			BlockState eBlockState = chunk.getBlockState(eBlockPos);
			BlockEntity eBlock = chunk.getBlockEntity(eBlockPos);
			CDBEntityBlock cdbBlock = new CDBEntityBlock();
			cdbBlock.x = eBlockPos.getX(); cdbBlock.y = eBlockPos.getY(); cdbBlock.z = eBlockPos.getZ();
			cdbBlock.blockId = Block.getId(eBlockState);
			if(eBlock != null) cdbBlock.nbtData = eBlock.saveWithFullMetadata(world.registryAccess());
			BlockEntities.add(cdbBlock);
		}
	}

	@Override public void pasteData(ServerLevel world, ChunkPos chunkPos)
	{
		LevelChunk chunk = world.getChunk(chunkPos.x(), chunkPos.z());
		for (CDBEntityBlock cdbBlock : BlockEntities)
		{
			BlockPos pos = new BlockPos(cdbBlock.x, cdbBlock.y, cdbBlock.z);
			BlockState state = Block.stateById(cdbBlock.blockId);
			if(!state.isAir() && cdbBlock.nbtData != null && state.getBlock() instanceof EntityBlock entityBlock)
			{
				chunk.setBlockState(pos, state, Block.UPDATE_ALL);
				BlockEntity be = entityBlock.newBlockEntity(pos, state);
				if(be != null)
				{
					be.loadWithComponents(TagValueInput.create(ProblemReporter.DISCARDING, world.registryAccess(), cdbBlock.nbtData));
					chunk.setBlockEntity(be);
				}
			}
		}
		chunk.markUnsaved();
	}

	@Override public void updateClients(ServerLevel world, ChunkPos chunkPos) {}
	@Override public void readData(InputStream stream) throws IOException
	{
		BlockEntities.clear();
		while(stream.available() > 0) { int len = IOUtils.readVarInt(stream); byte[] bytes = stream.readNBytes(len); CDBEntityBlock b = new CDBEntityBlock(); b.readByteArray(bytes); BlockEntities.add(b); }
	}
	@Override public void writeData(OutputStream stream) throws IOException
	{
		for(CDBEntityBlock b : BlockEntities) { byte[] bytes = b.toByteArray(); IOUtils.writeVarInt(stream, bytes.length); stream.write(bytes); }
	}

	public class CDBEntityBlock
	{
		public int x, y, z, blockId; public CompoundTag nbtData;
		public byte[] toByteArray() throws IOException
		{
			if(nbtData == null) nbtData = new CompoundTag();
			ByteArrayOutputStream stream = new ByteArrayOutputStream();
			IOUtils.writeVarInt(stream, x); IOUtils.writeVarInt(stream, y); IOUtils.writeVarInt(stream, z); IOUtils.writeVarInt(stream, blockId);
			IOUtils.writeString(stream, NbtUtils.structureToSnbt(nbtData)); return stream.toByteArray();
		}
		public void readByteArray(byte[] bytes) throws IOException
		{
			ByteArrayInputStream stream = new ByteArrayInputStream(bytes); x = IOUtils.readVarInt(stream); y = IOUtils.readVarInt(stream); z = IOUtils.readVarInt(stream); blockId = IOUtils.readVarInt(stream);
			try { nbtData = TagParser.parseCompoundFully(IOUtils.readString(stream)); } catch(Exception e) { throw new IOException("Invalid or corrupted BlockEntity NBT data.", e); }
		}
	}
}
