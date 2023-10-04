package net.bezeram.manhuntmod.commands;

import com.mojang.brigadier.CommandDispatcher;
import net.bezeram.manhuntmod.game_manager.Game;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.world.scores.PlayerTeam;
import org.jetbrains.annotations.NotNull;

public class DebugCommand {
	public DebugCommand(CommandDispatcher<CommandSourceStack> dispatcher) {
		dispatcher.register(Commands.literal("Debug")
				.then(Commands.literal("ActiveTime").executes((command) -> {
					// Print time elapsed since start of game
					boolean isInSession = Game.isInSession();

					if (isInSession) {
						double timeSeconds = Game.get().getElapsedTime().asSeconds();

						command.getSource().getPlayerOrException().sendSystemMessage(
								Component.literal(timeSeconds + " have elapsed since start of manhunt"));
					}
					else {
						command.getSource().getPlayerOrException().sendSystemMessage(
								Component.literal("No game in session"));
					}

					return 0;
				}))
				.then(Commands.literal("GameState").executes((command) -> {
					command.getSource().getPlayerOrException().sendSystemMessage(
							Component.literal(Game.getGameState().toString())
					);

					return 0;
				}))
				.then(Commands.literal("Teams").executes((command) -> {
					boolean inSession = Game.isInSession();
					String feedback = "";

					if (inSession)
						feedback = getTeamData().toString();
					else
						feedback = "No game in session";

					command.getSource().getPlayerOrException().sendSystemMessage(
							Component.literal(feedback));

					return 0;
				})));
	}

	@NotNull
	private static StringBuilder getTeamData() {
		PlayerTeam teamRunner = Game.get().getTeamRunner();
		PlayerTeam teamHunter = Game.get().getTeamHunter();

		StringBuilder output = new StringBuilder(
				"Team Runner Name: " + teamRunner.getDisplayName() +
						"\nTeam Hunter Name: " + teamHunter.getDisplayName());

		output.append("\nTeam Runner Members: ");
		for (String runner : teamRunner.getPlayers()) {
			output.append(runner);
			output.append(' ');
		}

		output.append("\nTeam Hunter Members: ");
		for (String hunter : teamHunter.getPlayers()) {
			output.append(hunter);
			output.append(' ');
		}

		return output;
	}
}
