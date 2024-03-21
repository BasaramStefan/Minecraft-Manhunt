package net.bezeram.manhuntmod.commands;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import net.bezeram.manhuntmod.events.ModEvents;
import net.bezeram.manhuntmod.game.Game;
import net.bezeram.manhuntmod.game.Time;
import net.bezeram.manhuntmod.game.players.PlayerRespawner;
import net.bezeram.manhuntmod.item.DeathSafeItems;
import net.bezeram.manhuntmod.item.custom.HunterCompassItem;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandBuildContext;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.data.registries.VanillaRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.scores.PlayerTeam;
import org.jetbrains.annotations.NotNull;

public class DebugCommand {
	public DebugCommand(CommandDispatcher<CommandSourceStack> dispatcher) {
		CommandBuildContext commandbuildcontext = Commands.createValidationContext(VanillaRegistries.createLookup());
		dispatcher.register(Commands.literal("Debug")
				.then(Commands.literal("ActiveTime").executes((command) -> {
					// Print time elapsed since start of game
					boolean isInSession = Game.inSession();

					if (isInSession) {
						double timeSeconds = Game.get().getElapsedTime().asSeconds();

						command.getSource().getPlayerOrException().sendSystemMessage(
								Component.literal(timeSeconds + " have elapsed since start of manhunt"));
					} else {
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
					boolean inSession = Game.inSession();
					String feedback = "";

					if (inSession)
						feedback = getTeamData().toString();
					else
						feedback = "No game in session";

					command.getSource().getPlayerOrException().sendSystemMessage(
							Component.literal(feedback));

					return 0;
				}))
				.then(Commands.literal("GetInventoryItem")
						.then(Commands.argument("slot", IntegerArgumentType.integer())
								.executes((command) -> {
									ServerPlayer player = command.getSource().getPlayerOrException();
									ItemStack item = player.getInventory().getItem(IntegerArgumentType.getInteger(command, "slot"));
									player.getEnderChestInventory().addItem(item);

									return 0;
								})))
				.then(Commands.literal("RemoveInventoryItem")
						.then(Commands.argument("slot", IntegerArgumentType.integer())
								.then(Commands.argument("count", IntegerArgumentType.integer())
										.executes((command) -> {
											ServerPlayer player = command.getSource().getPlayerOrException();
											ItemStack item = player.getInventory()
													.removeItem(IntegerArgumentType.getInteger(command, "slot"),
															IntegerArgumentType.getInteger(command, "count"));
											player.getEnderChestInventory().addItem(item);

											return 0;
										}))))
				.then(Commands.literal("SetInventoryItem")
						.then(Commands.argument("slot", IntegerArgumentType.integer())
								.executes((command) -> {
									ServerPlayer player = command.getSource().getPlayerOrException();
									player.getInventory().setItem(
											IntegerArgumentType.getInteger(command, "slot"),
											new ItemStack(Items.DIRT));

									return 0;
								})))
				.then(Commands.literal("SaveInventory")
						.executes((command) -> {
							final int SLOT_COUNT = 41;

							if (command.getSource().getEntity() instanceof ServerPlayer player && !player.isCreative()) {
								Inventory savedInventory = new Inventory(player);

								for (int slot = 0; slot < SLOT_COUNT; slot++) {
									ItemStack itemStack = player.getInventory().getItem(slot);

									if (DeathSafeItems.isDeathSafe(itemStack.getItem())) {
										savedInventory.setItem(slot, itemStack);
										System.out.println("current slot: " + slot + " " + itemStack.getItem());
										player.getInventory().setItem(slot, ItemStack.EMPTY);
									}
								}

								System.out.println("Player: " + player.getDisplayName().getString() + " requests inventory save");
								PlayerRespawner.saveInventoryStatic(player.getDisplayName().getString(), savedInventory);
								player.displayClientMessage(Component.literal("Inventory saved"), false);
							}

							return 0;
						}))
				.then(Commands.literal("LoadInventory")
						.executes((command) -> {
							final int SLOT_COUNT = 41;

							Player player = command.getSource().getPlayerOrException();
							String playerName = player.getDisplayName().getString();
							System.out.println("Player: " + playerName + " requests inventory load");

							if (PlayerRespawner.getInventoryStatic(playerName) == null) {
								player.displayClientMessage(Component.literal("No inventory saved!")
										.withStyle(ChatFormatting.RED), false);
							}

							for (int slot = 0; slot < SLOT_COUNT; slot++) {
								ItemStack itemStack = PlayerRespawner.getInventoryStatic(playerName).getItem(slot);
								player.getInventory().setItem(slot, itemStack);
							}

							player.displayClientMessage(Component.literal("Inventory loaded"), false);
							return 0;
				}))
				.then(Commands.literal("RemoveEnchantment")
						.executes((command) -> {
							Game.removePiercing(command.getSource().getPlayerOrException());
							return 0;
				}))
				.then(Commands.literal("SuddenDeathHighlight")
						.executes((command) -> {
							Time cycle = ModEvents.ForgeEvents.SuddenDeathWarning.highlightCycleTimer;
							Time highlight = ModEvents.ForgeEvents.SuddenDeathWarning.highlightChangeDelayTimer;

							command.getSource().getPlayerOrException().displayClientMessage(Component
									.literal("HighlightCycleTimer:" + cycle.asSeconds() + " -/- HighlightChangeDelay" + highlight.asSeconds()), false);
							return 0;
				}))
				.then(Commands.literal("PrintLastPlayerPositions")
						.executes((command) -> {
							Player player = command.getSource().getPlayerOrException();
							String playerName = player.getName().getString();
							Vec3[] positions = new Vec3[]{
									Game.PlayerLastLocations.Overworld.getLastPosition(playerName),
									Game.PlayerLastLocations.Nether.getLastPosition(playerName),
									Game.PlayerLastLocations.End.getLastPosition(playerName)
							};

							player.displayClientMessage(Component.literal(
									"(" + positions[0].x + ", " + positions[0].y + ", " + positions[0].z + ")   " +
											"(" + positions[1].x + ", " + positions[1].y + ", " + positions[1].z + ")   " +
											"(" + positions[2].x + ", " + positions[2].y + ", " + positions[2].z + ")"),
									true);
							return 0;
				}))
				.then(Commands.literal("GetCompassTags")
						.executes((command) -> {
							if (!Game.inSession())
								return 1;

							Player player = command.getSource().getPlayerOrException();
							ItemStack itemStack = player.getInventory().getItem(0);

							if (itemStack.getItem() instanceof HunterCompassItem) {
								CompoundTag tag = itemStack.getTag();
								if (tag == null) {
									player.displayClientMessage(Component.literal("Compass has no tags"), false);
									return 1;
								}

								int trackedPlayerID = tag.getInt(HunterCompassItem.TAG_TARGET_PLAYER);
								boolean trackingPlayer = tag.getBoolean(HunterCompassItem.TAG_TARGET_TRACKED);
								ServerPlayer trackedPlayer = Game.get().getPlayers().getPlayer(trackedPlayerID);
								String trackedPlayerName = trackedPlayer.getName().getString();

								player.displayClientMessage(Component.literal("Tracking: " + trackedPlayerName), false);
								player.displayClientMessage(Component.literal("Tracking: " + trackingPlayer), false);
							}
							else
								player.displayClientMessage(Component.literal(
										"Item in slot 0 is not a hunter compass"), false);

						return 0;
				}))
				.then(Commands.literal("SetHoverName")
						.executes((command) -> {
							if (!Game.inSession())
								return 1;

							Player player = command.getSource().getPlayerOrException();
							ItemStack itemStack = player.getInventory().getItem(0);

							itemStack.setHoverName(Component.literal("Hello World!").withStyle(ChatFormatting.BLUE));
							return 0;
				}))
				.then(Commands.literal("ResetHoverName")
						.executes((command) -> {
							if (!Game.inSession())
								return 1;

							Player player = command.getSource().getPlayerOrException();
							ItemStack itemStack = player.getInventory().getItem(0);

							itemStack.resetHoverName();
							return 0;
				})));
	}

	private static void printInventory(String playerName) {
		Inventory inventory = PlayerRespawner.getInventoryStatic(playerName);
		for (int slot = 0; slot < 41; slot++)
			System.out.println("slot: " + slot + " " + inventory.getItem(slot));
	}

	@NotNull
	private static StringBuilder getTeamData() {
		PlayerTeam teamRunner = Game.get().getTeamRunner();
		PlayerTeam teamHunter = Game.get().getTeamHunter();

		StringBuilder output = new StringBuilder(
				"Team Runner display name: " + teamRunner.getDisplayName().getString() +
						"\nTeam Hunter display name: " + teamHunter.getDisplayName().getString());

		output.append("\nTeam Runner real name: " + teamRunner.getName() +
				"\nTeam Hunter real name: " + teamHunter.getName());

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
