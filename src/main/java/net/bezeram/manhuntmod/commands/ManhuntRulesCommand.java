package net.bezeram.manhuntmod.commands;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.BoolArgumentType;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import net.bezeram.manhuntmod.game_manager.ManhuntGameRules;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;

public class ManhuntRulesCommand {
	public ManhuntRulesCommand(CommandDispatcher<CommandSourceStack> dispatcher) {
		dispatcher.register(Commands.literal("ManhuntRules")

		.then(Commands.literal("resetDefaults").executes((command) -> {
			ManhuntGameRules.resetDefaults();

			command.getSource().getPlayerOrException().displayClientMessage(Component
					.literal("Manhunt: rules have been reset to default"), false);
			return 0;
		}))
		.then(Commands.literal("timeLimit")
		.then(Commands.argument("toggle", BoolArgumentType.bool()).executes((command) -> {
			ManhuntGameRules.TIME_LIMIT = BoolArgumentType.getBool(command, "toggle");

			String toggle = (ManhuntGameRules.TIME_LIMIT) ? "enabled" : "disabled";
			command.getSource().getPlayerOrException().displayClientMessage(Component
					.literal("Manhunt: time limit has been " + toggle), false);
			return 0;
		})))
		.then(Commands.literal("doLimitedRespawns")
		.then(Commands.argument("toggle", BoolArgumentType.bool()).executes((command) -> {
			// TODO: Implement this
			ManhuntGameRules.LIMITED_RESPAWNS = BoolArgumentType.getBool(command, "toggle");

			String toggle = (ManhuntGameRules.LIMITED_RESPAWNS) ? "enabled" : "disabled";
			command.getSource().getPlayerOrException().displayClientMessage(Component
					.literal("Manhunt: limited respawns have been " + toggle), false);
			return 0;
		})))
		.then(Commands.literal("saveInventory")
		.then(Commands.argument("toggle", BoolArgumentType.bool()).executes((command) -> {
			ManhuntGameRules.SAVE_INVENTORIES = BoolArgumentType.getBool(command, "toggle");

			String toggle = (ManhuntGameRules.SAVE_INVENTORIES) ? "enabled" : "false";
			command.getSource().getPlayerOrException().displayClientMessage(Component
					.literal("Manhunt: saved inventories have been " + toggle), false);
			return 0;
		})))
		.then(Commands.literal("doHeadstart")
		.then(Commands.argument("toggle", BoolArgumentType.bool()).executes((command) -> {
			ManhuntGameRules.HEADSTART = BoolArgumentType.getBool(command, "toggle");

			String toggle = (ManhuntGameRules.HEADSTART) ? "enabled" : "false";
			command.getSource().getPlayerOrException().displayClientMessage(Component
					.literal("Manhunt: headstart has been " + toggle), false);
			return 0;
		})))
		.then(Commands.literal("disableRespawnBlockExplosions")
		.then(Commands.argument("toggle", BoolArgumentType.bool()).executes((command) -> {
			ManhuntGameRules.DISABLE_RESPAWN_BLOCK_EXPLOSION = BoolArgumentType.getBool(command, "toggle");

			String toggle = (ManhuntGameRules.DISABLE_RESPAWN_BLOCK_EXPLOSION) ? "disabled" : "enabled";
			command.getSource().getPlayerOrException().displayClientMessage(Component
					.literal("Manhunt: beds and respawn-anchors explosions have been " + toggle), false);
			return 0;
		})))
		// TODO: actually implement the banning
		.then(Commands.literal("banEnchantments")
				.then(Commands.literal("enableDefault")
				.then(Commands.argument("toggle", BoolArgumentType.bool()).executes((command) -> {
					ManhuntGameRules.BAN_ENCHANTMENTS.toggleDefaultOptions(BoolArgumentType.getBool(command, "toggle"));

					String toggle = (BoolArgumentType.getBool(command, "toggle")) ? "banned" : "unbanned";
					command.getSource().getPlayerOrException().displayClientMessage(Component
							.literal("Manhunt: default-enchantments have been " + toggle),
							false);
					return 0;
				})))
				.then(Commands.literal("add")
						.then(Commands.literal("default").executes((command) -> {
							ManhuntGameRules.BAN_ENCHANTMENTS.addDefaultOptions();

							command.getSource().getPlayerOrException().displayClientMessage(Component
											.literal("Manhunt: default-enchantments have been added to banned list"),
									false);
							return 0;
						}))
						.then(Commands.literal("piercing")
						.then(Commands.argument("max_level", IntegerArgumentType.integer()).executes((command) -> {
							int max_level = IntegerArgumentType.getInteger(command, "max_level");
							ManhuntGameRules.BAN_ENCHANTMENTS.add("piercing", max_level);

							command.getSource().getPlayerOrException().displayClientMessage(Component
											.literal("Manhunt: piercing (level: " + max_level + ") has been banned"),
									false);
							return 0;
						})))
						.then(Commands.literal("quickCharge")
						.then(Commands.argument("max_level", IntegerArgumentType.integer()).executes((command) -> {
							int max_level = IntegerArgumentType.getInteger(command, "max_level");
							ManhuntGameRules.BAN_ENCHANTMENTS.add("quick_charge", max_level);

							command.getSource().getPlayerOrException().displayClientMessage(Component
											.literal("Manhunt: quickCharge (level: " + max_level + ") has been banned"),
									false);
							return 0;
				}))))
				.then(Commands.literal("remove")
						.then(Commands.literal("default").executes((command) -> {
							ManhuntGameRules.BAN_ENCHANTMENTS.removeDefaultOptions();

							command.getSource().getPlayerOrException().displayClientMessage(Component
											.literal("Manhunt: default-enchantments have been removed from banned " +
													"list"),
									false);
							return 0;
						}))
						.then(Commands.literal("piercing").executes((command) -> {
							ManhuntGameRules.BAN_ENCHANTMENTS.remove("piercing");

							command.getSource().getPlayerOrException().displayClientMessage(Component
											.literal("Manhunt: Piercing enchantment has been removed from banned list"),
									false);
							return 0;
						}))
						.then(Commands.literal("quickCharge").executes((command) -> {
							ManhuntGameRules.BAN_ENCHANTMENTS.remove("quick_charge");

							command.getSource().getPlayerOrException().displayClientMessage(Component
											.literal("Manhunt: Quick-Charge enchantment has been removed from banned " +
													"list"),
									false);
							return 0;
						}))
				)
		)
		// TODO: Implement the a similar class from banning enchantments for banning potions
		// TODO: actually implement the banning
		.then(Commands.literal("banPotions")
				.then(Commands.literal("disabled").executes((command) -> {
					ManhuntGameRules.BAN_POTIONS = ManhuntGameRules.PotionBanType.DISABLED;

					command.getSource().getPlayerOrException().displayClientMessage(Component
									.literal("Manhunt: all potions have been enabled"),
							false);
					return 0;
				}))
				.then(Commands.literal("all").executes((command) -> {
					ManhuntGameRules.BAN_POTIONS = ManhuntGameRules.PotionBanType.ALL;

					command.getSource().getPlayerOrException().displayClientMessage(Component
									.literal("Manhunt: all potions have been banned"),
							false);
					return 0;
				}))
				.then(Commands.literal("extendedOnly").executes((command) -> {
					ManhuntGameRules.BAN_POTIONS = ManhuntGameRules.PotionBanType.EXTENDED_ONLY;

					command.getSource().getPlayerOrException().displayClientMessage(Component
									.literal("Manhunt: extended-potions have been banned"),
							false);
					return 0;
				}))
				.then(Commands.literal("amplifiedOnly").executes((command) -> {
					ManhuntGameRules.BAN_POTIONS = ManhuntGameRules.PotionBanType.AMPLIFIED_ONLY;

					command.getSource().getPlayerOrException().displayClientMessage(Component
									.literal("Manhunt: extended-potions have been banned"),
							false);
					return 0;
				}))
				.then(Commands.literal("extendedOrAmplified").executes((command) -> {
					ManhuntGameRules.BAN_POTIONS = ManhuntGameRules.PotionBanType.EXTENDED_OR_AMPLIFIED;

					command.getSource().getPlayerOrException().displayClientMessage(Component
							.literal("Manhunt: extended or amplified (including both combined) potions have been " +
									"banned"),
							false);
					return 0;
		})))
		.then(Commands.literal("doDeathPenalty")
				.then(Commands.literal("trueExceptEnd").executes((command) -> {
					ManhuntGameRules.DEATH_PENALTY = ManhuntGameRules.DeathPenaltyType.TRUE_EXCEPT_END;

					command.getSource().getPlayerOrException().displayClientMessage(Component
									.literal("Manhunt: death penalty has been enabled in the Overworld and Nether"),
							false);
					return 0;
				}))
				.then(Commands.literal("true").executes((command) -> {
					ManhuntGameRules.DEATH_PENALTY = ManhuntGameRules.DeathPenaltyType.TRUE;

					command.getSource().getPlayerOrException().displayClientMessage(Component
									.literal("Manhunt: death penalty has been enabled in every dimension"),
							false);
					return 0;
				}))
				.then(Commands.literal("false").executes((command) -> {
					ManhuntGameRules.DEATH_PENALTY = ManhuntGameRules.DeathPenaltyType.FALSE;

					command.getSource().getPlayerOrException().displayClientMessage(Component
									.literal("Manhunt: death penalty has been disabled"),
							false);
					return 0;
		})))
		.then(Commands.literal("banEndCrystals")
		.then(Commands.argument("toggle", BoolArgumentType.bool()).executes((command) -> {
			ManhuntGameRules.BAN_END_CRYSTALS = BoolArgumentType.getBool(command, "toggle");

			String toggle = (ManhuntGameRules.BAN_END_CRYSTALS) ? "banned" : "unbanned";
			command.getSource().getPlayerOrException().displayClientMessage(Component
							.literal("Manhunt: End Crystals have been " + toggle), false);
			return 0;
		})))
		.then(Commands.literal("canBreakSpawners")
		.then(Commands.argument("toggle", BoolArgumentType.bool()).executes((command) -> {
			ManhuntGameRules.CAN_BREAK_SPAWNERS = BoolArgumentType.getBool(command, "toggle");

			String toggle = (ManhuntGameRules.CAN_BREAK_SPAWNERS) ? "enabled" : "disabled";
			command.getSource().getPlayerOrException().displayClientMessage(Component
					.literal("Manhunt: breakable spawners have been " + toggle), false);
			return 0;
		}))));
	}
}
