package net.bezeram.manhuntmod.commands;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.BoolArgumentType;
import net.bezeram.manhuntmod.game_manager.Game;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;

public class ManhuntToggleRulesCommand {
	public ManhuntToggleRulesCommand(CommandDispatcher<CommandSourceStack> dispatcher) {
		dispatcher.register(Commands.literal("ManhuntToggleRules").then(Commands
				.argument("toggle", BoolArgumentType.bool())
				.executes((command) -> {
					if (Game.isInSession()) {
						String feedback = "Game is already in session";
						command.getSource().getPlayerOrException().sendSystemMessage(Component.literal(feedback));
					}

					boolean toggle = BoolArgumentType.getBool(command, "toggle");
					Game.toggleRules(toggle);

					String feedback = "";
					if (toggle)
						feedback = "Manhunt rules have been activated";
					else
						feedback = "Manhunt rules have been deactivated";

					command.getSource().getPlayerOrException().sendSystemMessage(Component.literal(feedback));
					return 0;
				})));
	}
}
