package thecsdev.chunkcopy.client.gui;

import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

public final class CCConfigScreen extends Screen
{
	protected CCConfigScreen() { super(Component.translatable("chunkcopy.title")); }
	@Override public boolean isPauseScreen() { return true; }
	@Override public boolean shouldCloseOnEsc() { return true; }
}
