package net.bezeram.manhuntmod.commands;

import com.mojang.brigadier.CommandDispatcher;
import net.bezeram.manhuntmod.game.Game;
import net.bezeram.manhuntmod.game.ManhuntGameRules;
import net.bezeram.manhuntmod.game.GameTimer;
import net.bezeram.manhuntmod.item.ModItems;
import net.bezeram.manhuntmod.item.custom.HunterCompassItem;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.commands.arguments.TeamArgument;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.ServerScoreboard;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.GameRules;
import net.minecraft.world.scores.Objective;
import net.minecraft.world.scores.PlayerTeam;
import net.minecraft.world.scores.criteria.ObjectiveCriteria;

public class ManhuntCommand {
	public ManhuntCommand(CommandDispatcher<CommandSourceStack> dispatcher) {
		dispatcher.register(Commands.literal("Manhunt")
		.then(Commands.literal("teams")
		.then(Commands.argument("runnerTeam", TeamArgument.team())
		.then(Commands.argument("hunterTeam", TeamArgument.team())
				.executes((command) -> {
					if (Game.inSession()) {
						command.getSource().getPlayerOrException().sendSystemMessage(Component
								.literal("Game already in session").withStyle(ChatFormatting.RED));
						return 1;
					}

					// Extract arguments from command
					PlayerTeam teamRunner = TeamArgument.getTeam(command, "runnerTeam");
					PlayerTeam teamHunter = TeamArgument.getTeam(command, "hunterTeam");

					String teamRunnerName = teamRunner.getName();
					String teamHunterName = teamHunter.getName();
					if (teamRunnerName.equals(teamHunterName)) {
						command.getSource().getPlayerOrException().sendSystemMessage(Component
								.literal("One team cannot play against themselves").withStyle(ChatFormatting.RED));
						return 1;
					}

					teamRunner.setColor(ChatFormatting.DARK_GREEN);
					teamHunter.setColor(ChatFormatting.DARK_PURPLE);

					// Validate command conditions
					// Check if teams are empty
					ValidateType pack = checkTeamsEmpty(teamRunner, teamHunter);

					if (pack.success) {
						// Initiate the scoreboard objectives and personalize
						ServerScoreboard scoreboard = command.getSource().getServer().getScoreboard();
						setupScoreboard(scoreboard, teamRunner, teamHunter);

						// Setup vanilla gamerules
						GameRules gameRules = command.getSource().getServer().getWorldData().getGameRules();
						// If this isn't false, it messes up the mechanic of safe death items
						gameRules.getRule(GameRules.RULE_KEEPINVENTORY).set(false, command.getSource().getServer());

						MinecraftServer server = command.getSource().getServer();
						Game.init(teamRunner, teamHunter, server.getPlayerList(), server);

						for (ServerPlayer player : server.getPlayerList().getPlayers()) {
							if (Game.get().getPlayerData().isHunter(player)) {
								ItemStack compass = new ItemStack(ModItems.HUNTER_COMPASS.get());
								HunterCompassItem.addOrUpdateTags(player.getLevel(), compass.getOrCreateTag());
								if (!player.getInventory().add(compass))
									player.drop(compass, false);

//								int dimensionID = Game.getDimensionID(player.gtLevel().dimension());
//								HunterCompassItem.putGlobalCompass(player.getUUID(), compass, dimensionID);
							}
						}

						command.getSource().getServer().getPlayerList().broadcastSystemMessage(Component
										.literal("Starting game in: " + (int)Game.get().getStartDelay().asSeconds() + " " +
												"seconds")
										.withStyle(ChatFormatting.GREEN),
								false);
					}
					else
						command.getSource().getServer().getPlayerList().broadcastSystemMessage(Component
							.literal(pack.feedback).withStyle(pack.format), false);

					return 0;
		}))))
		.then(Commands.argument("runner", EntityArgument.entity())
		.then(Commands.argument("hunter", EntityArgument.entity())
				.executes((command) -> {
					if (Game.inSession()) {
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
					ServerScoreboard scoreboard = command.getSource().getServer().getScoreboard();
					PlayerTeam teamRunner = scoreboard.addPlayerTeam(runnerName);
					PlayerTeam teamHunter = scoreboard.addPlayerTeam(hunterName);
					teamRunner.setDisplayName(Component.literal(runner.getName().getString()));
					teamHunter.setDisplayName(Component.literal(hunter.getName().getString()));
					teamRunner.setColor(ChatFormatting.DARK_GREEN);
					teamHunter.setColor(ChatFormatting.DARK_PURPLE);
					scoreboard.addPlayerToTeam(runner.getName().getString(), teamRunner);
					scoreboard.addPlayerToTeam(hunter.getName().getString(), teamHunter);

					// Initiate the scoreboard objectives and personalize
					setupScoreboard(scoreboard, teamRunner, teamHunter);

					// Setup vanilla gamerules
					GameRules gameRules = command.getSource().getServer().getWorldData().getGameRules();
					// If this isn't false, it messes up the mechanic of safe death items
					gameRules.getRule(GameRules.RULE_KEEPINVENTORY).set(false, command.getSource().getServer());

					MinecraftServer server = command.getSource().getServer();
					Game.init(teamRunner, teamHunter, server.getPlayerList(), server);
					System.out.println("Successfully initiated Game class\n");

					for (ServerPlayer player : Game.get().getHuntersArray()) {
						ItemStack compass = new ItemStack(ModItems.HUNTER_COMPASS.get());
						HunterCompassItem.addOrUpdateTags(player.getLevel(), compass.getOrCreateTag());
						if (!player.getInventory().add(compass))
							player.drop(compass, false);
					}
					System.out.println("Successfully given out compasses\n");

					command.getSource().getServer().getPlayerList().broadcastSystemMessage(Component
								.literal("Starting game in: " + (int)Game.get().getStartDelay().asSeconds() + " " +
										"seconds")
								.withStyle(ChatFormatting.GREEN),
							false);
					return 0;
		})))
		.then(Commands.literal("stop")
				.executes((command) -> {
					if (Game.inSession()) {
						Game.get().stopGame();

						ServerScoreboard scoreboard = command.getSource().getServer().getScoreboard();
						PlayerTeam playerTeam = scoreboard.getPlayerTeam("SuddenDeath");
						if (playerTeam != null)
							command.getSource().getServer().getScoreboard().removePlayerTeam(playerTeam);

						command.getSource().getPlayerOrException().sendSystemMessage(Component
								.literal("Game of Manhunt forcefully stopped").withStyle(ChatFormatting.GREEN));
					}
					else {
						command.getSource().getPlayerOrException().sendSystemMessage(Component
								.literal("No game in session").withStyle(ChatFormatting.RED));
					}

					return 0;
		}))
		.then(Commands.literal("pause")
				.executes((command) -> {
					if (Game.inSession()) {
						if (Game.canPauseGame(command)) {
							Game.get().pauseGame();

							ServerScoreboard scoreboard = command.getSource().getServer().getScoreboard();
							Objective timer = scoreboard.getObjective("TimeLeft");
							assert timer != null;
							scoreboard.getOrCreatePlayerScore("PAUSED", timer).setScore(0);

							Game.get().resetResumeTime();

							for (ServerPlayer player : Game.get().getPlayerData().getList().getPlayers()) {
								player.setInvulnerable(true);
								player.setInvisible(true);
							}

							command.getSource().getServer().getPlayerList().broadcastSystemMessage(Component
									.literal("Game has been paused: resume at will")
									.withStyle(ChatFormatting.GREEN),	false);
							return 0;
						}

						command.getSource().getServer().getPlayerList().broadcastSystemMessage(Component
								.literal("Pause request invalid: all players must sit still")
								.withStyle(ChatFormatting.GOLD),	false);
						return 1;
					}

					command.getSource().getPlayerOrException().sendSystemMessage(Component
							.literal("No game in session").withStyle(ChatFormatting.RED));

					return 1;
		}))
		.then(Commands.literal("resume")
				.executes((command) -> {
					if (Game.inSession()) {
						if (Game.canResumeGame()) {
							Game.get().resumeGame();

							ServerScoreboard scoreboard = command.getSource().getServer().getScoreboard();
							Objective timer = scoreboard.getObjective("TimeLeft");
							assert timer != null;
							scoreboard.resetPlayerScore("PAUSED", timer);

							command.getSource().getServer().getPlayerList().broadcastSystemMessage(Component
									.literal("Game will be resumed in " + (int)Game.get().getResumeDelay().asSeconds() + " seconds")
									.withStyle(ChatFormatting.GREEN),	false);
							return 0;
						}
					}

					command.getSource().getPlayerOrException().sendSystemMessage(Component
							.literal("No game in session").withStyle(ChatFormatting.RED));

					return 1;
		})));
	}

	private static void setupScoreboard(ServerScoreboard scoreboard, PlayerTeam teamRunner, PlayerTeam teamHunter) {
		Objective timer = scoreboard.getObjective("TimeLeft");
		scoreboard.resetPlayerScore("PAUSED", timer);
		scoreboard.resetPlayerScore("STOPPED", timer);
		scoreboard.resetPlayerScore("RUNNER WINS", timer);
		scoreboard.resetPlayerScore("RUNNERS WIN", timer);
		scoreboard.resetPlayerScore("HUNTER WINS", timer);
		scoreboard.resetPlayerScore("HUNTERS WIN", timer);

		if (timer == null) {
			Component sidebar = Component.translatable(ServerScoreboard.getDisplaySlotName(1));
			ObjectiveCriteria.RenderType renderType = ObjectiveCriteria.RenderType.INTEGER;
			ObjectiveCriteria playerKillCount = ObjectiveCriteria.KILL_COUNT_PLAYERS;

			timer = scoreboard.addObjective("TimeLeft", playerKillCount, sidebar, renderType);
		}


		// Display teams
		int score = 1;
		for (String hunter : teamHunter.getPlayers()) {
			scoreboard.getOrCreatePlayerScore(hunter, timer).setScore(score);
			score++;
		}

		for (String runner : teamRunner.getPlayers()) {
			scoreboard.getOrCreatePlayerScore(runner, timer).setScore(score);
			score++;
		}

		if (ManhuntGameRules.TIME_LIMIT) {
			timer.setDisplayName(Component.literal("Time / Kills").withStyle(ChatFormatting.GOLD));

			if (GameTimer.getGameTime().asMinutes() > 1) {
				scoreboard.getOrCreatePlayerScore("Minutes", timer).setScore(score);
				scoreboard.resetPlayerScore("Seconds", timer);
			}
			else {
				scoreboard.getOrCreatePlayerScore("Seconds", timer).setScore(score);
				scoreboard.resetPlayerScore("Minutes", timer);
			}
		}
		else {
			// We only have teams on the scoreboard, might as well be labeled kills
			scoreboard.resetPlayerScore("Seconds", timer);
			scoreboard.resetPlayerScore("Minutes", timer);
			timer.setDisplayName(Component.literal("Kills").withStyle(ChatFormatting.GOLD));
		}

		scoreboard.setDisplayObjective(1, scoreboard.getObjective("TimeLeft"));
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
			return new ValidateType("Runner team (" + teamRunner.getName() + ") is empty", false,
					ValidateType.FAILURE_FORMAT);

		if (teamHunterEmpty)
			return new ValidateType("Hunter team (" + teamHunter.getName() + ") is empty", false,
					ValidateType.FAILURE_FORMAT);

		return new ValidateType("Teams valid", true,
				ValidateType.SUCCESS_FORMAT);
	}

	private static ValidateType checkExists() {
		if (Game.inSession())
			return new ValidateType("Already in session", false, ValidateType.FAILURE_FORMAT);
		return new ValidateType("Starting game in " + (int)Game.get().getStartDelay().asSeconds() + " seconds", true,
				ValidateType.SUCCESS_FORMAT);
	}
}