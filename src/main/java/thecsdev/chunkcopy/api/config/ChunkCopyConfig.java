package thecsdev.chunkcopy.api.config;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.HashSet;
import java.util.Properties;

import org.jetbrains.annotations.Nullable;

import com.mojang.brigadier.arguments.BoolArgumentType;

import net.minecraft.resources.Identifier;
import thecsdev.chunkcopy.ChunkCopy;

public final class ChunkCopyConfig
{
	public static final ConfigKey<Boolean> PASTE_ENTITIES = new ConfigKey<>(ChunkCopy.getModID(), "paste_entities", BoolArgumentType.bool());
	private static final Properties PROPERTIES = new Properties();
	public static final HashSet<ConfigKey<?>> KEYS = new HashSet<>();

	public static File getPropertiesFile()
	{ return new File(ChunkCopy.getRunDirectory().getAbsolutePath() + "/config/" + ChunkCopy.ModID + ".properties"); }

	public static boolean saveConfig()
	{
		FileOutputStream fos = null;
		try { File file = getPropertiesFile(); file.getParentFile().mkdirs(); file.createNewFile(); fos = new FileOutputStream(file); PROPERTIES.store(fos, ChunkCopy.ModName + " config"); return true; }
		catch (IOException e) { return false; }
		finally { if(fos != null) try { fos.close(); } catch(Exception e) {} }
	}

	public static boolean loadConfig()
	{
		FileInputStream fis = null;
		try { File file = getPropertiesFile(); file.getParentFile().mkdirs(); file.createNewFile(); fis = new FileInputStream(file); PROPERTIES.load(fis); return true; }
		catch (IOException e) { return false; }
		finally { if(fis != null) try { fis.close(); } catch(Exception e) {} }
	}

	@Nullable
	public static ConfigKey<?> getKeyByName(Identifier keyName)
	{ try { return KEYS.stream().filter(i -> i.keyName.compareTo(keyName) == 0).findFirst().get(); } catch(Exception e) { return null; } }

	@SuppressWarnings("unchecked")
	public static <T> T get(ConfigKey<T> key, T defaultValue)
	{
		try { String value = PROPERTIES.getProperty(key.keyName.toString()); if(value == null) return defaultValue; if(key.argumentType instanceof BoolArgumentType) return (T) Boolean.valueOf(value); return (T) value; }
		catch(Exception e) { return defaultValue; }
	}

	public static <T> void set(ConfigKey<T> key, T value)
	{ PROPERTIES.setProperty(key.keyName.toString(), key.valueToString(value)); }
}
