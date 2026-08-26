package thecsdev.chunkcopy.client.mixin;

import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.chunk.LevelChunk;
import thecsdev.chunkcopy.api.AutoChunkCopy;
import thecsdev.chunkcopy.api.ChunkCopyAPI;

@Mixin(targets = "net/minecraft/client/multiplayer/ClientChunkCache$Storage")
public abstract class ClientChunkMapMixin
{
	@Inject(method = "replace", at = @At("TAIL"))
	protected void replace(int index, @Nullable LevelChunk chunk, CallbackInfo callback)
	{
		if(!AutoChunkCopy.isRunning()) return;
		if(chunk == null) return;
		try
		{
			Level world = (Level) chunk.getLevel();
			ChunkPos chunkPos = chunk.getPos();
			String fileName = AutoChunkCopy.getFileName();
			ChunkCopyAPI.saveChunkDataIO(world, chunkPos, fileName);
		}
		catch (Throwable e) {}
	}
}
