package thecsdev.chunkcopy.api.config;

import com.mojang.brigadier.arguments.ArgumentType;

import net.minecraft.resources.Identifier;

public /*non final*/ class ConfigKey<T>
{
	public final Identifier keyName;
	public final ArgumentType<T> argumentType;

	public ConfigKey(String namespace, String path, ArgumentType<T> argType)
	{ this(new Identifier(namespace, path), argType); }

	public ConfigKey(Identifier name, ArgumentType<T> argType)
	{ this.keyName = name; this.argumentType = argType; }

	@Override
	public final int hashCode()
	{ return keyName.toString().hashCode() + argumentType.toString().hashCode(); }

	public String valueToString(T value) { return value.toString(); }
}
