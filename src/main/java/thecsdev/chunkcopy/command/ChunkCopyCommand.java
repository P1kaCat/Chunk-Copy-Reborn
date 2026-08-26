package thecsdev.chunkcopy.command;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.ArgumentType;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.builder.RequiredArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;

import net.fabricmc.api.EnvType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.commands.CommandBuildContext;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.commands.arguments.blocks.BlockStateArgument;
import net.minecraft.commands.arguments.blocks.BlockInput;
import thecsdev.chunkcopy.ChunkCopy;
import thecsdev.chunkcopy.api.AutoChunkCopy;
import thecsdev.chunkcopy.api.AutoChunkCopy.ACCMode;
import thecsdev.chunkcopy.command.argument.CopiedChunksArgumentType;

public abstract class ChunkCopyCommand<CS extends SharedSuggestionProvider>
{
	protected static final IntegerArgumentType ChunkDistArg = IntegerArgumentType.integer(1, 8);

	public void register(CommandDispatcher<CS> dispatcher, CommandBuildContext cra)
	{
		LiteralArgumentBuilder<CS> command = literal(getCommandName())
			.requires(cs -> canChunkCopy(cs))
			.executes(arg -> exec(arg))
			.then(literal("copy")
				.requires(arg -> canCopy(arg))
				.then(argument("fileName", CopiedChunksArgumentType.forCopying())
					.executes(arg -> exec_copy_fileName(arg))
					.then(argument("chunkDistance", ChunkDistArg)
						.executes(arg -> exec_copy_fileName_chunkDistance(arg)))))
			.then(literal("paste")
				.requires(arg -> canPaste(arg))
				.then(argument("fileName", CopiedChunksArgumentType.forPasting())
					.executes(arg -> exec_paste_fileName(arg))
					.then(argument("chunkDistance", ChunkDistArg)
						.executes(arg -> exec_paste_fileName_chunkDistance(arg)))))
			.then(literal("fill")
				.requires(arg -> canPaste(arg))
				.then(argument("chunkDistance", ChunkDistArg)
					.then(argument("blockState", BlockStateArgument.block(cra))
						.executes(arg -> exec_fill_chunkDistance_block(arg)))))
			.then(literal("clear")
				.requires(arg -> canPaste(arg))
				.then(argument("chunkDistance", ChunkDistArg)
					.executes(arg -> exec_clear_chunkDistance(arg))))
			.then(literal("auto")
				.requires(arg -> AutoChunkCopy.validate())
				.then(literal("copy")
					.requires(arg -> canCopy(arg))
					.then(argument("fileName", CopiedChunksArgumentType.forCopying())
						.executes(arg -> exec_autoChunkCopy_start_fileName(arg, ACCMode.Copying))))
				.then(literal("paste")
					.requires(arg -> canPaste(arg))
					.then(argument("fileName", CopiedChunksArgumentType.forPasting())
						.executes(arg -> exec_autoChunkCopy_start_fileName(arg, ACCMode.Pasting))))
				.then(literal("stop")
					.executes(arg -> exec_autoChunkCopy_stop(arg))));

		dispatcher.register(command);
	}

	private int exec(CommandContext<CS> cs) { execMain(cs.getSource()); return 1; }
	private int exec_copy_fileName(CommandContext<CS> cs) { copy(cs.getSource(), cs.getArgument("fileName", String.class), 8); return 1; }
	private int exec_copy_fileName_chunkDistance(CommandContext<CS> cs) { copy(cs.getSource(), cs.getArgument("fileName", String.class), cs.getArgument("chunkDistance", Integer.class)); return 1; }
	private int exec_paste_fileName(CommandContext<CS> cs) { paste(cs.getSource(), cs.getArgument("fileName", String.class), 8); return 1; }
	private int exec_paste_fileName_chunkDistance(CommandContext<CS> cs) { paste(cs.getSource(), cs.getArgument("fileName", String.class), cs.getArgument("chunkDistance", Integer.class)); return 1; }
	private int exec_fill_chunkDistance_block(CommandContext<CS> cs) { fill(cs.getSource(), cs.getArgument("chunkDistance", Integer.class), ((BlockInput)cs.getArgument("blockState", BlockInput.class)).getState()); return 1; }
	private int exec_clear_chunkDistance(CommandContext<CS> cs) { fill(cs.getSource(), cs.getArgument("chunkDistance", Integer.class), Blocks.AIR.defaultBlockState()); return 1; }
	private int exec_autoChunkCopy_start_fileName(CommandContext<CS> cs, ACCMode accMode) { autoChunkCopyStart(cs.getSource(), cs.getArgument("fileName", String.class), accMode); return 1; }
	private int exec_autoChunkCopy_stop(CommandContext<CS> cs) { autoChunkCopyStop(cs.getSource()); return 1; }

	public abstract String getCommandName();
	protected abstract boolean canChunkCopy(CS commandSource);
	protected abstract boolean canCopy(CS commandSource);
	protected abstract boolean canPaste(CS commandSource);
	protected abstract boolean canConfig(CS commandSource);
	protected final boolean requireEnv(EnvType env) { try { return ChunkCopy.getEnviroment() == env; } catch (Exception e) { return false; } }
	protected abstract void execMain(CS commandSource);
	protected abstract void copy(CS commandSource, String fileName, int chunkDistance);
	protected abstract void paste(CS commandSource, String fileName, int chunkDistance);
	protected abstract void fill(CS commandSource, int chunkDistance, BlockState block);
	protected final void clear(CS commandSource, int chunkDistance) { fill(commandSource, chunkDistance, Blocks.AIR.defaultBlockState()); }
	protected abstract void autoChunkCopyStart(CS commandSource, String fileName, ACCMode accMode);
	protected abstract void autoChunkCopyStop(CS commandSource);
	public LiteralArgumentBuilder<CS> literal(String name) { return LiteralArgumentBuilder.literal(name); }
	public <ARG> RequiredArgumentBuilder<CS, ARG> argument(String name, ArgumentType<ARG> type) { return RequiredArgumentBuilder.argument(name, type); }
	protected static String getExceptionMessage(Throwable e)
	{
		StringBuilder sb = new StringBuilder(); sb.append(e.getClass().getCanonicalName() + ": " + e.getMessage() + "\n");
		for(StackTraceElement ste : e.getStackTrace()) { if(!ste.getClassName().contains("thecsdev")) continue; sb.append(ste.toString() + "\n"); break; }
		return sb.toString().trim();
	}
}