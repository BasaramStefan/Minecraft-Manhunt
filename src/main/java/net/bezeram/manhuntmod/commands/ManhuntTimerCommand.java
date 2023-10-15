package net.bezeram.manhuntmod.commands;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.DoubleArgumentType;
import net.bezeram.manhuntmod.game_manager.ManhuntGameRules;
import net.bezeram.manhuntmod.game_manager.TimerManager;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;

public class ManhuntTimerCommand {
	public ManhuntTimerCommand(CommandDispatcher<CommandSourceStack> dispatcher) {
		dispatcher.register(Commands.literal("ManhuntTimer")
				.then(Commands.literal("game")
				.then(Commands.argument("timeMinutes", DoubleArgumentType.doubleArg(0))
						.executes((command) -> {
							double minutes = DoubleArgumentType.getDouble(command, "timeMinutes");
							TimerManager.setGameTime(minutes);

							String playerFeedback = "Game time has been set to " + minutes + " minutes";
							ServerPlayer player = command.getSource().getPlayerOrException();

							player.sendSystemMessage(Component.literal(playerFeedback));
							if (!ManhuntGameRules.TIME_LIMIT) {
								player.sendSystemMessage(Component
										.literal("Warning: doTimeLimit command is set to " + "false")
										.withStyle(ChatFormatting.GOLD));
							}

							return 0;
						})))
				.then(Commands.literal("headstart")
				.then(Commands.argument("timeSeconds", DoubleArgumentType.doubleArg(0))
						.executes((command) -> {
							double seconds = DoubleArgumentType.getDouble(command, "timeSeconds");
							TimerManager.setHeadstart(seconds);

							String playerFeedback = "Headstart time has been set to " + seconds + " seconds";
							ServerPlayer player = command.getSource().getPlayerOrException();

							player.sendSystemMessage(Component.literal(playerFeedback));
							if (!ManhuntGameRules.HEADSTART) {
								player.sendSystemMessage(Component
										.literal("Warning: doHeadstart gamerule is set to false")
										.withStyle(ChatFormatting.GOLD));
							}
							return 0;
						})))
				.then(Commands.literal("deathPenalty")
				.then(Commands.argument("timeMinutes", DoubleArgumentType.doubleArg(0))
						.executes((command) -> {
							double minutes = DoubleArgumentType.getDouble(command, "timeMinutes");
							TimerManager.setDeathPenalty(minutes);

							String playerFeedback = "Penalty for death has been set to " + minutes + " minutes";
							ServerPlayer player = command.getSource().getPlayerOrException();

							player.sendSystemMessage(Component.literal(playerFeedback));
							if (ManhuntGameRules.DEATH_PENALTY == ManhuntGameRules.DeathPenaltyType.FALSE) {
								player.sendSystemMessage(Component
										.literal("Warning: doDeathPenalty gamerule is set to false")
										.withStyle(ChatFormatting.GOLD));
							}

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
