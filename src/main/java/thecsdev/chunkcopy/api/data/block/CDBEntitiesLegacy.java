package thecsdev.chunkcopy.api.data.block;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Optional;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntitySpawnRequest;
import net.minecraft.world.entity.player.Player;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.nbt.TagParser;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.storage.TagValueOutput;
import net.minecraft.world.level.storage.TagValueInput;
import net.minecraft.util.ProblemReporter;
import thecsdev.chunkcopy.ChunkCopy;
import thecsdev.chunkcopy.api.ChunkCopyUtils;
import thecsdev.chunkcopy.api.data.ChunkDataBlock;
import thecsdev.chunkcopy.api.data.ChunkDataBlockID;
import thecsdev.chunkcopy.api.io.IOUtils;

@ChunkDataBlockID(namespace = ChunkCopy.ModID, path = "entities_legacy")
public class CDBEntitiesLegacy extends ChunkDataBlock
{
	public final ArrayList<CompoundTag> EntityNBTs = new ArrayList<>();

	@Override
	public void copyData(Level world, ChunkPos chunkPos)
	{
		EntityNBTs.clear();
		for (Entity entity : ChunkCopyUtils.getEntitiesInChunk(world, chunkPos))
		{
			TagValueOutput output = TagValueOutput.createWithContext(ProblemReporter.DISCARDING, world.registryAccess());
			entity.saveWithoutId(output);
			CompoundTag eNbt = output.buildResult();
			eNbt.putString("id", EntityType.getKey(entity.getType()).toString());
			EntityNBTs.add(eNbt);
		}
	}

	@Override
	public void pasteData(ServerLevel world, ChunkPos chunkPos)
	{
		for (Entity entity : ChunkCopyUtils.getEntitiesInChunk(world, chunkPos))
			if(!(entity instanceof Player)) entity.discard();

		for (CompoundTag eNbt : EntityNBTs)
			try
			{
				Optional<Entity> optEntity = EntityType.create(
					TagValueInput.create(ProblemReporter.DISCARDING, world.registryAccess(), eNbt),
					world, new EntitySpawnRequest(EntitySpawnReason.LOAD, false));
				if(optEntity.isEmpty()) continue;
				Entity entity = optEntity.get();
				Entity oldEntity = world.getEntity(entity.getUUID());
				if(oldEntity != null && !oldEntity.isRemoved()) oldEntity.discard();
				world.addFreshEntity(entity);
			}
			catch (Exception e) {}
	}

	@Override
	public void updateClients(ServerLevel world, ChunkPos chunkPos) {}

	@Override
	public void readData(InputStream stream) throws IOException
	{
		EntityNBTs.clear();
		while(stream.available() > 0)
		{
			try { EntityNBTs.add(TagParser.parseCompoundFully(IOUtils.readString(stream))); }
			catch (Exception e) { throw new IOException("Invalid or corrupted Entity NBT data.", e); }
		}
	}

	@Override
	public void writeData(OutputStream stream) throws IOException
	{
		for (CompoundTag eNbt : EntityNBTs)
			IOUtils.writeString(stream, NbtUtils.structureToSnbt(eNbt));
	}
}
