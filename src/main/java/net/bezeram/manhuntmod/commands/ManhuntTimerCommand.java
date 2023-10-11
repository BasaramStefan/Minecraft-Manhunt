package net.bezeram.manhuntmod.commands;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.DoubleArgumentType;
import net.bezeram.manhuntmod.game_manager.TimerManager;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;

public class ManhuntTimerCommand {
	public ManhuntTimerCommand(CommandDispatcher<CommandSourceStack> dispatcher) {
		dispatcher.register(Commands.literal("ManhuntTimer")
				.then(Commands.literal("game")
				.then(Commands.argument("timeMinutes", DoubleArgumentType.doubleArg(0))
						.executes((command) -> {
							double minutes = DoubleArgumentType.getDouble(command, "timeMinutes");
							TimerManager.setGameTime(minutes);

							command.getSource().getPlayerOrException().sendSystemMessage(Component.literal(
									"Game time has been set to " + minutes + " minutes"
							));
							return 0;
						})))
				.then(Commands.literal("headstart")
				.then(Commands.argument("timeSeconds", DoubleArgumentType.doubleArg(0))
						.executes((command) -> {
							double seconds = DoubleArgumentType.getDouble(command, "timeSeconds");
							TimerManager.setHeadstart(seconds);

							command.getSource().getPlayerOrException().sendSystemMessage(Component.literal(
									"Headstart time has been set to " + seconds + " seconds"
							));
							return 0;
						})))
				.then(Commands.literal("deathPenalty")
				.then(Commands.argument("timeMinutes", DoubleArgumentType.doubleArg(0))
						.executes((command) -> {
							double minutes = DoubleArgumentType.getDouble(command, "timeMinutes");
							TimerManager.setDeathPenalty(minutes);

							command.getSource().getPlayerOrException().sendSystemMessage(Component.literal(
									"Penalty for death has been set to " + minutes + " minutes"
							));
							return 0;
						})))
				.then(Commands.literal("pause")
				.then(Commands.argument("timeMinutes", DoubleArgumentType.doubleArg(0))
						.executes((command) -> {
							double minutes = DoubleArgumentType.getDouble(command, "timeMinutes");
							TimerManager.setPauseTime(minutes);

							command.getSource().getPlayerOrException().sendSystemMessage(Component.literal(
									"Pause time has been set to " + minutes + " minutes"
							));
							return 0;
		}))));
	}
}
