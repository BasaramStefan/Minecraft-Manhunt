package net.bezeram.manhuntmod.commands;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import net.bezeram.manhuntmod.game.DedicatedRespawnsManager;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;

public class ManhuntSetSpawnCountCommand {
	public ManhuntSetSpawnCountCommand(CommandDispatcher<CommandSourceStack> dispatcher) {
		dispatcher.register(Commands.literal("ManhuntSetSpawnCount")
				.then(Commands.argument("count", IntegerArgumentType.integer(0))
				.executes((command) -> {
					int input = IntegerArgumentType.getInteger(command, "count");
					DedicatedRespawnsManager.DEFAULT_SET_RESPAWNS_OVERWORLD = input;
					DedicatedRespawnsManager.DEFAULT_SET_RESPAWNS_NETHER = input;

					command.getSource().getPlayerOrException().sendSystemMessage(
							Component.literal(
									"Global dedicated-respawn count set to " +
									DedicatedRespawnsManager.DEFAULT_SET_RESPAWNS_OVERWORLD
							));
					return 0;
		}))
		.then(Commands.argument("count", IntegerArgumentType.integer(0))
		.then(Commands.literal("Overworld")
			.executes((command) -> {
				int input = IntegerArgumentType.getInteger(command, "count");
				DedicatedRespawnsManager.DEFAULT_SET_RESPAWNS_OVERWORLD = input;

				command.getSource().getPlayerOrException().sendSystemMessage(
						Component.literal(
								"Overworld dedicated-respawn count set to " +
								DedicatedRespawnsManager.DEFAULT_SET_RESPAWNS_OVERWORLD
						));
				return 0;
		})))
		.then(Commands.argument("count", IntegerArgumentType.integer(0))
		.then(Commands.literal("Nether")
			.executes((command) -> {
				int input = IntegerArgumentType.getInteger(command, "count");
				DedicatedRespawnsManager.DEFAULT_SET_RESPAWNS_NETHER = input;

				command.getSource().getPlayerOrException().sendSystemMessage(
						Component.literal(
								"Nether dedicated-respawn count set to " +
								DedicatedRespawnsManager.DEFAULT_SET_RESPAWNS_NETHER
						));
				return 0;
		})))
		.then(Commands.argument("count", IntegerArgumentType.integer(0))
		.then(Commands.literal("End")
			.executes((command) -> {
				int input = IntegerArgumentType.getInteger(command, "count");
				DedicatedRespawnsManager.DEFAULT_SET_RESPAWNS_END = input;

				command.getSource().getPlayerOrException().sendSystemMessage(
						Component.literal(
								"End dedicated-respawn count set to " +
								DedicatedRespawnsManager.DEFAULT_SET_RESPAWNS_END
						));
				return 0;
		})))
		.then(Commands.literal("infinite")
			.executes((command) -> {
				int input = Integer.MAX_VALUE;
				DedicatedRespawnsManager.DEFAULT_SET_RESPAWNS_OVERWORLD = input;
				DedicatedRespawnsManager.DEFAULT_SET_RESPAWNS_NETHER = input;
				DedicatedRespawnsManager.DEFAULT_SET_RESPAWNS_END = input;

				command.getSource().getPlayerOrException().sendSystemMessage(
						Component.literal("Global dedicated-respawn count set to infinite"));
				return 0;
		}))
		.then(Commands.literal("infinite")
		.then(Commands.literal("Overworld")
			.executes((command) -> {
				DedicatedRespawnsManager.DEFAULT_SET_RESPAWNS_OVERWORLD = Integer.MAX_VALUE;

				command.getSource().getPlayerOrException().sendSystemMessage(
						Component.literal("Overworld dedicated-respawn count set to infinite"));
				return 0;
		})))
		.then(Commands
		.literal("infinite")
		.then(Commands.literal("Nether")
			.executes((command) -> {
				DedicatedRespawnsManager.DEFAULT_SET_RESPAWNS_NETHER = Integer.MAX_VALUE;

				command.getSource().getPlayerOrException().sendSystemMessage(
						Component.literal("Nether dedicated-respawn count set to infinite"));
				return 0;
		})))
		.then(Commands.literal("infinite")
		.then(Commands.literal("End")
			.executes((command) -> {
				DedicatedRespawnsManager.DEFAULT_SET_RESPAWNS_END = Integer.MAX_VALUE;

				command.getSource().getPlayerOrException().sendSystemMessage(
						Component.literal("End dedicated-respawn count set to infinite"));
				return 0;
		})))
		.then(Commands.literal("list")
			.executes((command) -> {
				int overworldCount = DedicatedRespawnsManager.DEFAULT_SET_RESPAWNS_OVERWORLD;
				int netherCount = DedicatedRespawnsManager.DEFAULT_SET_RESPAWNS_NETHER;
				int endCount = DedicatedRespawnsManager.DEFAULT_SET_RESPAWNS_END;
				command.getSource().getPlayerOrException().sendSystemMessage(
						Component.literal(
					"Overworld: " + parseCount(overworldCount) + "\n" +
							"Nether: " + parseCount(netherCount) + "\n" +
							"End: " + parseCount(endCount)
						));
				return 0;
		})));
	}

	private String parseCount(int count) {
		if (count == Integer.MAX_VALUE)
			return "infinite";
		return String.valueOf(count);
	}
}
