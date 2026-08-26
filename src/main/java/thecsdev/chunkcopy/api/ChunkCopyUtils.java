package thecsdev.chunkcopy.api;

import java.util.ArrayList;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.boss.enderdragon.EnderDragon;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.chunk.EmptyLevelChunk;
import net.minecraft.world.level.chunk.LevelChunk;

public final class ChunkCopyUtils
{
	public static ArrayList<ChunkPos> getNearbyLoadedChunks(Level world, ChunkPos chunkPos, int chunkDistance)
	{
		if(chunkDistance < 1) chunkDistance = 1;
		else if(chunkDistance > 8) chunkDistance = 8;
		ArrayList<ChunkPos> result = new ArrayList<>();
		if(chunkDistance == 1) { result.add(chunkPos); }
		else if(chunkDistance > 1)
		{
			chunkDistance--;
			for(int chunkX = chunkPos.getX() - chunkDistance; chunkX < chunkPos.getX() + chunkDistance; chunkX++)
			{
				for(int chunkZ = chunkPos.getZ() - chunkDistance; chunkZ < chunkPos.getZ() + chunkDistance; chunkZ++)
				{
					if(!world.hasChunk(chunkX, chunkZ)) continue;
					LevelChunk chunk = world.getChunk(chunkX, chunkZ);
					if(chunk == null || (chunk instanceof EmptyLevelChunk)) continue;
					result.add(new ChunkPos(chunkX, chunkZ));
				}
			}
		}
		return result;
	}

	public static AABB getChunkBox(Level world, ChunkPos chunkPos)
	{
		ChunkAccess chunk = world.getChunk(chunkPos.getX(), chunkPos.getZ());
		return new AABB(
				chunkPos.getMinBlockX(), chunk.getMinY(), chunkPos.getMinBlockZ(),
				chunkPos.getMaxBlockX() + 1, chunk.getMaxY() + 1, chunkPos.getMaxBlockZ() + 1);
	}

	public static ArrayList<Entity> getEntitiesInChunk(Level world, ChunkPos chunkPos)
	{
		ArrayList<Entity> result = new ArrayList<>();
		AABB chunkBox = getChunkBox(world, chunkPos);
		result.addAll(world.getEntities((Entity)null, chunkBox, e ->
			!(e instanceof Player) && !(e instanceof EnderDragon)
		));
		return result;
	}
}
