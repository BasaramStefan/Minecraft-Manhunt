package net.bezeram.manhuntmod.commands;

import com.mojang.brigadier.CommandDispatcher;
import net.bezeram.manhuntmod.game_manager.Game;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.commands.arguments.TeamArgument;
import net.minecraft.network.chat.Component;
import net.minecraft.server.ServerScoreboard;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.scores.PlayerTeam;
import org.jetbrains.annotations.NotNull;

public class ManhuntCommand {
	public ManhuntCommand(CommandDispatcher<CommandSourceStack> dispatcher) {
		dispatcher.register(Commands.literal("Manhunt")
		.then(Commands.literal("teams")
		.then(Commands.argument("teamRunner", TeamArgument.team())
		.then(Commands.argument("teamHunter", TeamArgument.team())
				.executes((command) -> {
					if (Game.isInSession()) {
						command.getSource()
								.getPlayerOrException()
								.sendSystemMessage(Component.literal("Game already in session"));
						return 1;
					}

					// Extract arguments from command
					PlayerTeam teamRunner = TeamArgument.getTeam(command, "teamRunner");
					PlayerTeam teamHunter = TeamArgument.getTeam(command, "teamHunter");

					String teamRunnerName = teamRunner.getName();
					String teamHunterName = teamHunter.getName();
					if (teamRunnerName.equals(teamHunterName)) {
						command.getSource().getPlayerOrException().sendSystemMessage(
								Component.literal("One team cannot play against themselves"));
						return 1;
					}

					// Validate command conditions
					// Check if teams are empty
					ValidateType pack = checkTeamsEmpty(teamRunner, teamHunter);

					if (pack.success)
						Game.init(teamRunner, teamHunter);

					command.getSource()
							.getPlayerOrException()
							.sendSystemMessage(Component.literal(pack.feedback));
				return 0;
		}))))
		.then(Commands.argument("runner", EntityArgument.entity())
		.then(Commands.argument("hunter", EntityArgument.entity())
				.executes((command) -> {
					if (Game.isInSession()) {
						command.getSource()
								.getPlayerOrException()
								.sendSystemMessage(Component.literal("Game already in session"));
						return 1;
					}

					// Extract arguments
					ServerPlayer runner = EntityArgument.getPlayer(command, "runner");
					ServerPlayer hunter = EntityArgument.getPlayer(command, "hunter");

					// TODO:
					// If the runner and hunter are the same player, the game will crash because it will automatically make one team empty
					String runnerName = runner.getName().getString();
					String hunterName = hunter.getName().getString();
					if (runnerName.equals(hunterName)) {
						command.getSource()
								.getPlayerOrException()
								.sendSystemMessage(Component.literal("One cannot play against themselves"));
						return 1;
					}

					// Instantiate runner team and add the runner
					ServerScoreboard serverScoreboard = command.getSource().getServer().getScoreboard();
					PlayerTeam teamRunner = new PlayerTeam(serverScoreboard, runner.getName().getString());
					PlayerTeam teamHunter = new PlayerTeam(serverScoreboard, hunter.getName().getString());
					teamRunner.setDisplayName(Component.literal(runner.getName().toString()));
					teamHunter.setDisplayName(Component.literal(hunter.getName().toString()));

					serverScoreboard.addPlayerToTeam(runner.toString(), teamRunner);
					serverScoreboard.addPlayerToTeam(hunter.toString(), teamHunter);

					Game.init(teamRunner, teamHunter);

					command.getSource()
							.getPlayerOrException()
							.sendSystemMessage(Component.literal("Starting game..."));
					return 0;
		})))
		.then(Commands.literal("stop")
				.executes((command) -> {
					// Stop game
					boolean exists = Game.isInSession();
					String playerFeedback = "";

					if (exists) {
						Game.stopGame();
						playerFeedback = "Game of Manhunt forcefully stopped";
					}
					else
						playerFeedback = "No game in session";

					command.getSource()
							.getPlayerOrException()
							.sendSystemMessage(Component
									.literal(playerFeedback));
					return 0;
		})));
	}

	static class ValidateType {
		ValidateType() {}
		ValidateType(String feedback, boolean success) {
			this.feedback = feedback;
			this.success = success;
		}

		ValidateType intersection(ValidateType otherPack) {
			if (!this.success && !otherPack.success)
				return new ValidateType(this.feedback, false);
			if (!this.success)
				return new ValidateType(this.feedback, true);
			if (!otherPack.success)
				return new ValidateType(otherPack.feedback, false);
			return new ValidateType(this.feedback, true);
		}

		private String feedback = "";
		private boolean success = false;
	}

	private static ValidateType checkTeamsEmpty(PlayerTeam teamRunner, PlayerTeam teamHunter) {
		boolean teamRunnerEmpty = teamRunner.getPlayers().isEmpty();
		boolean teamHunterEmpty = teamHunter.getPlayers().isEmpty();

		if (teamRunnerEmpty && teamHunterEmpty)
			return new ValidateType("Both teams are empty", false);
		if (teamRunnerEmpty)
			return new ValidateType("Runner team is empty", false);
		if (teamHunterEmpty)
			return new ValidateType("Hunter team is empty", false);
		return new ValidateType("Starting game...", true);
	}

	private static ValidateType checkExists() {
		if (Game.isInSession())
			return new ValidateType("Already in session", false);
		return new ValidateType("Starting game...", true);
	}
}