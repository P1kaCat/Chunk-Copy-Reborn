package thecsdev.chunkcopy.client.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import thecsdev.chunkcopy.api.AutoChunkCopy;

@Mixin(Minecraft.class)
public abstract class MinecraftClientMixin
{
	@Inject(method = "disconnect(Lnet/minecraft/client/gui/screens/Screen;Z)V", at = @At("RETURN"))
	public void onDisconnect(Screen screen, boolean keepResourcePacks, CallbackInfo callback)
	{
		AutoChunkCopy.stop();
	}
}
