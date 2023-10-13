package net.bezeram.manhuntmod.commands;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.ParseResults;
import net.bezeram.manhuntmod.game_manager.Game;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.commands.arguments.TeamArgument;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.ServerScoreboard;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.GameRules;
import net.minecraft.world.scores.Objective;
import net.minecraft.world.scores.PlayerTeam;
import net.minecraft.world.scores.Scoreboard;
import net.minecraft.world.scores.criteria.ObjectiveCriteria;
import org.jetbrains.annotations.NotNull;

public class ManhuntCommand {
	public ManhuntCommand(CommandDispatcher<CommandSourceStack> dispatcher) {
		dispatcher.register(Commands.literal("Manhunt")
		.then(Commands.literal("teams")
		.then(Commands.argument("teamRunner", TeamArgument.team())
		.then(Commands.argument("teamHunter", TeamArgument.team())
				.executes((command) -> {
					if (Game.isInSession()) {
						command.getSource().getPlayerOrException().sendSystemMessage(Component
								.literal("Game already in session").withStyle(ChatFormatting.RED));
						return 1;
					}

					// Extract arguments from command
					PlayerTeam teamRunner = TeamArgument.getTeam(command, "teamRunner");
					PlayerTeam teamHunter = TeamArgument.getTeam(command, "teamHunter");

					String teamRunnerName = teamRunner.getName();
					String teamHunterName = teamHunter.getName();
					if (teamRunnerName.equals(teamHunterName)) {
						command.getSource().getPlayerOrException().sendSystemMessage(Component
								.literal("One team cannot play against themselves").withStyle(ChatFormatting.RED));
						return 1;
					}

					// Validate command conditions
					// Check if teams are empty
					ValidateType pack = checkTeamsEmpty(teamRunner, teamHunter);

					if (pack.success) {
						Game.init(teamRunner, teamHunter);

						// Initiate the scoreboard objectives and personalize
						ServerScoreboard serverScoreboard = command.getSource().getServer().getScoreboard();
						Objective timer = serverScoreboard.addObjective("TimeLeft", ObjectiveCriteria.DUMMY,
								Component.translatable(ServerScoreboard.getDisplaySlotName(1)),
								ObjectiveCriteria.RenderType.INTEGER);

						timer.setDisplayName(Component.literal("Time left"));
						serverScoreboard.getOrCreatePlayerScore("Minutes", timer);
						serverScoreboard.setDisplayObjective(1, serverScoreboard.getObjective("TimeLeft"));

						// Setup vanilla gamerules
						GameRules gameRules = command.getSource().getServer().getWorldData().getGameRules();
						// If this isn't false, it messes up the mechanic of safe death items
						gameRules.getRule(GameRules.RULE_KEEPINVENTORY).set(false, command.getSource().getServer());
					}

					command.getSource().getPlayerOrException().sendSystemMessage(
							Component.literal(pack.feedback).withStyle(pack.format));

					return 0;
		}))))
		.then(Commands.argument("runner", EntityArgument.entity())
		.then(Commands.argument("hunter", EntityArgument.entity())
				.executes((command) -> {
					if (Game.isInSession()) {
						command.getSource().getPlayerOrException().sendSystemMessage(Component
								.literal("Game already in session").withStyle(ChatFormatting.RED));
						return 1;
					}

					// Extract arguments
					ServerPlayer runner = EntityArgument.getPlayer(command, "runner");
					ServerPlayer hunter = EntityArgument.getPlayer(command, "hunter");

					String runnerName = runner.getName().getString();
					String hunterName = hunter.getName().getString();
					if (runnerName.equals(hunterName)) {
						command.getSource().getPlayerOrException().sendSystemMessage(Component
								.literal("One cannot play against themselves").withStyle(ChatFormatting.RED));
						return 1;
					}

					// Instantiate runner team and add the runner
					ServerScoreboard serverScoreboard = command.getSource().getServer().getScoreboard();
					PlayerTeam teamRunner = new PlayerTeam(serverScoreboard, runnerName);
					PlayerTeam teamHunter = new PlayerTeam(serverScoreboard, hunterName);
					teamRunner.setDisplayName(Component.literal(runner.getName().getString()));
					teamHunter.setDisplayName(Component.literal(hunter.getName().getString()));

					serverScoreboard.addPlayerToTeam(runner.getName().getString(), teamRunner);
					serverScoreboard.addPlayerToTeam(hunter.getName().getString(), teamHunter);

					Game.init(teamRunner, teamHunter);

					// Initiate the scoreboard objectives and personalize
					Objective timer = serverScoreboard.addObjective("TimeLeft", ObjectiveCriteria.DUMMY,
									Component.translatable(ServerScoreboard.getDisplaySlotName(1)),
									ObjectiveCriteria.RenderType.INTEGER);

					timer.setDisplayName(Component.literal("Time left"));
					serverScoreboard.getOrCreatePlayerScore("Minutes", timer);
					serverScoreboard.setDisplayObjective(1, serverScoreboard.getObjective("TimeLeft"));

					command.getSource().getPlayerOrException().sendSystemMessage(Component
							.literal("Starting game...").withStyle(ChatFormatting.GREEN));

					// Setup vanilla gamerules
					GameRules gameRules = command.getSource().getServer().getWorldData().getGameRules();
					// If this isn't false, it messes up the mechanic of safe death items
					gameRules.getRule(GameRules.RULE_KEEPINVENTORY).set(false, command.getSource().getServer());
					return 0;
		})))
		.then(Commands.literal("stop")
				.executes((command) -> {
					if (Game.isInSession()) {
						Game.stopGame();

						ServerScoreboard scoreboard = command.getSource().getServer().getScoreboard();
						Objective objective = scoreboard.getObjective("TimeLeft");
						if (objective != null) {
							scoreboard.removeObjective(objective);
						}

						command.getSource().getPlayerOrException().sendSystemMessage(Component
								.literal("Game of Manhunt forcefully stopped").withStyle(ChatFormatting.GREEN));
					}
					else {
						command.getSource().getPlayerOrException().sendSystemMessage(Component
								.literal("No game in session").withStyle(ChatFormatting.RED));
					}

					return 0;
		})));
	}

	static class ValidateType {
		ValidateType() {}
		ValidateType(String feedback, boolean success, ChatFormatting format) {
			this.feedback = feedback;
			this.success = success;
			this.format = format;
		}

		ValidateType intersection(ValidateType otherPack) {
			if (!this.success && !otherPack.success)
				return new ValidateType(this.feedback, false, this.format);
			if (!this.success)
				return new ValidateType(this.feedback, true, this.format);
			if (!otherPack.success)
				return new ValidateType(otherPack.feedback, false, otherPack.format);
			return new ValidateType(this.feedback, true, this.format);
		}

		private String feedback = "";
		private boolean success = false;
		private ChatFormatting format = DEFAULT_FORMAT;

		public static final ChatFormatting FAILURE_FORMAT = ChatFormatting.RED;
		public static final ChatFormatting SUCCESS_FORMAT = ChatFormatting.GREEN;
		public static final ChatFormatting DEFAULT_FORMAT = ChatFormatting.WHITE;
	}

	private static ValidateType checkTeamsEmpty(PlayerTeam teamRunner, PlayerTeam teamHunter) {
		boolean teamRunnerEmpty = teamRunner.getPlayers().isEmpty();
		boolean teamHunterEmpty = teamHunter.getPlayers().isEmpty();

		if (teamRunnerEmpty && teamHunterEmpty)
			return new ValidateType("Both teams are empty", false, ValidateType.FAILURE_FORMAT);
		if (teamRunnerEmpty)
			return new ValidateType("Runner team is empty", false, ValidateType.FAILURE_FORMAT);
		if (teamHunterEmpty)
			return new ValidateType("Hunter team is empty", false, ValidateType.FAILURE_FORMAT);
		return new ValidateType("Starting game...", true, ValidateType.SUCCESS_FORMAT);
	}

	private static ValidateType checkExists() {
		if (Game.isInSession())
			return new ValidateType("Already in session", false, ValidateType.FAILURE_FORMAT);
		return new ValidateType("Starting game...", true, ValidateType.SUCCESS_FORMAT);
	}
}