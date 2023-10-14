package net.bezeram.manhuntmod.game_manager;

import net.minecraft.ChatFormatting;
import net.minecraft.client.resources.sounds.Sound;
import net.minecraft.nbt.ListTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.ServerScoreboard;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.players.PlayerList;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.GameRules;
import net.minecraft.world.level.dimension.BuiltinDimensionTypes;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.scores.Objective;
import net.minecraft.world.scores.PlayerTeam;
import net.minecraftforge.event.TickEvent;

import java.util.*;

public class Game {
	// TODO: Add event: When EnderDragon dies -> runner (team) wins

	private static Game GAME_INSTANCE = null;

	private Game(PlayerTeam teamRunner, PlayerTeam teamHunter, PlayerList playerList) {
		this.teamRunner = teamRunner;
		this.teamHunter = teamHunter;
		currentState = GameState.HEADSTART;

		for (ServerPlayer player : playerList.getPlayers())
			if (isHunter(player))
				huntersStartCoords.put(player.getName().getString(), player.getPosition(1));
	}

	public static void init(PlayerTeam teamRunner, PlayerTeam teamHunter, PlayerList playerList) {
		GAME_INSTANCE = new Game(teamRunner, teamHunter, playerList);
	}

	public static boolean isInSession() {
		return GAME_INSTANCE != null;
	}

	public static Game get() {
		return GAME_INSTANCE;
	}

	public static GameState getGameState() {
		return currentState;
	}

	public static void stopGame() {
		GAME_INSTANCE.toggleRules(false);
		currentState = GameState.NULL;
		GAME_INSTANCE = null;
	}

	public void toggleRules(boolean enabled) {
		if (!Game.isInSession())
			rulesEnabled = enabled;
	}

	public void applyDeathPenalty() {
		timer.deathPenalty();
	}

	public boolean rulesEnabled() {
		return rulesEnabled;
	}

	public Time getElapsedTime() {
		return timer.getTime();
	}

	public void runnerHasWon() {
		currentState = GameState.END;
		runnerWins = true;
	}

	public void hunterHasWon() {
		currentState = GameState.END;
		runnerWins = false;
	}

	public void saveInventory(String playerDisplayName, Inventory inventory) {
		playerInventories.put(playerDisplayName, inventory);
	}

	public Inventory getInventory(String playerDisplayName) {
		if (!playerInventories.containsKey(playerDisplayName))
			return null;
		return playerInventories.get(playerDisplayName);
	}

	public boolean isInventorySaved(String playerDisplayName) {
		return playerInventories.containsKey(playerDisplayName);
	}

	// TODO: Fix this
	public static void removePiercing(ServerPlayer player) {
		Inventory inventory = player.getInventory();

		int foundSlot = -1;
		for (int slot = 0; slot < Inventory.INVENTORY_SIZE; slot++) {
			ItemStack itemStack = inventory.getItem(slot);
			// TODO: how do I identify any crossbow, not just pure default
			if (itemStack.getItem().getName(itemStack).getString().equals("crossbow")) {
				foundSlot = slot;
				break;
			}
		}

		if (foundSlot != -1) {
			boolean hasEnchantments = !inventory.getItem(foundSlot).getEnchantmentTags().isEmpty();
			if (hasEnchantments) {
				ListTag list = inventory.getItem(foundSlot).getEnchantmentTags();
				player.displayClientMessage(Component.literal("Tags:"), false);
				for (int i = 0; i < list.size(); i++) {
					player.displayClientMessage(Component.literal(list.getString(i)), false);
				}
			}
			else
				player.displayClientMessage(Component.literal("No enchant tags"), false);
		}
		else
			player.displayClientMessage(Component.literal("No crossbow"), false);
	}

	public boolean canRespawnDedicated(Player player) {
		if (respawnerMap.containsKey(player)) {
			return respawnerMap.get(player).canRespawnDedicated(player);
		}

		return false;
	}

	// Game can only be paused while in the ONGOING GameState
	public void pauseGame() {
		currentState = GameState.PAUSE;
	}

	public void unPauseGame() {
		currentState = GameState.ONGOING;
	}

	public void update(TickEvent.ServerTickEvent event) {
		switch (currentState) {
			case PAUSE -> {

			}
			case HEADSTART -> {
				/*
					TODO:
				 	Do not allow hunters to move or break blocks
				*/

				timer.updateActive();
				timer.updateHeadstart();
				timer.updateHeadstartHints(event);

				lockHuntersPos(event);

				if (timer.huntersHaveStarted()) {
					currentState = GameState.ONGOING;
					PlayerList playerList = event.getServer().getPlayerList();
					playerList.broadcastSystemMessage(Component.literal("Hunters have been unleashed!"), false);
					/*
						TODO: Play pillager raid start of wave sound,
						 this bs does not work
					*/

//					for (ServerPlayer player : playerList.getPlayers()) {
//						player.playSound(SoundEvents.PILLAGER_CELEBRATE);
//					}
				}
			}
			case ONGOING -> {
				timer.updateActive();

				// Update the game
				if (timer.activeTimeHasEnded())
					hunterHasWon();

				// TODO:
				// Update compass
				// Use the Beacon powered / unpowered sounds for when it detects dimension change
			}
			case END -> {
				// Common end of game functionality
				PlayerList playerList = event.getServer().getPlayerList();
				PlayerTeam winnerTeam = (runnerWins) ? teamRunner : teamHunter;
				PlayerTeam loserTeam  = (!runnerWins) ? teamRunner : teamHunter;

				// TODO: Display the feedback correctly
				String feedbackServer = "";
				if (winnerTeam.getPlayers().size() == 1)
					feedbackServer = winnerTeam.getDisplayName().getString() + " has won the game!";
				else
					feedbackServer = winnerTeam.getDisplayName().getString() + " team has won the game!";
				playerList.broadcastSystemMessage(Component.literal(feedbackServer), false);

				// TODO: Play sounds correctly, this bs does not work
				for (String playerName : winnerTeam.getPlayers()) {
					ServerPlayer player = playerList.getPlayerByName(playerName);

					if (player != null)
						player.playSound(SoundEvents.PLAYER_LEVELUP);
				}

				for (String playerName : loserTeam.getPlayers()) {
					ServerPlayer player = playerList.getPlayerByName(playerName);

					if (player != null)
						player.playSound(SoundEvents.PILLAGER_CELEBRATE);
				}

				ServerScoreboard scoreboard = event.getServer().getScoreboard();
				Objective timer = scoreboard.getObjective("TimeLeft");
				if (timer != null) {
					scoreboard.removeObjective(timer);
				}

				currentState = GameState.ERASE;
			}
		}
	}

	public PlayerTeam getTeamRunner() { return teamRunner; }
	public PlayerTeam getTeamHunter() { return teamHunter; }

	public static boolean isHunterAtHeadstart(Player player) {
		if (player.getTeam() == null || currentState != GameState.HEADSTART)
			return false;

		String playerName = player.getName().getString();
		PlayerTeam hunterTeam = Game.get().getTeamHunter();
		for (String hunter : hunterTeam.getPlayers()) {
			if (hunter.contains(playerName)) {
				return true;
			}
		}

		return false;
	}

	public boolean isHunter(Player player) {
		if (player.getTeam() == null)
			return false;

		String playerName = player.getName().getString();
		for (String hunter : teamHunter.getPlayers()) {
			if (hunter.contains(playerName)) {
				return true;
			}
		}

		return false;
	}

	public boolean isRunner(Player player) {
		if (player.getTeam() == null || currentState != GameState.ONGOING)
			return false;

		String playerName = player.getName().getString();
		for (String runner : teamRunner.getPlayers()) {
			if (runner.contains(playerName)) {
				return true;
			}
		}

		return false;
	}

	private void lockHuntersPos(TickEvent.ServerTickEvent event) {
		PlayerList playerList = event.getServer().getPlayerList();
		for (ServerPlayer player : playerList.getPlayers())
			if (isHunter(player)) {
				Vec3 coords = huntersStartCoords.get(player.getName().getString());
				Vec3 currentPlayerCoords = player.getPosition(1);

				if (currentPlayerCoords.x != coords.x ||
						currentPlayerCoords.y != coords.y ||
						currentPlayerCoords.z != coords.z) {
					player.teleportTo(coords.x, coords.y, coords.z);
				}
			}
	}

	// TODO: Implement the PAUSE game state
	public enum GameState {
		NULL, HEADSTART, ONGOING, END, PAUSE, ERASE
	}
	private static GameState currentState = GameState.NULL;
	private boolean rulesEnabled = false;
	private boolean runnerWins = true;

	private final PlayerTeam teamRunner;
	private final PlayerTeam teamHunter;
	private final Hashtable<String, Vec3> huntersStartCoords = new Hashtable<>();

	private final Hashtable<String, Inventory> playerInventories = new Hashtable<>();

	private final TimerManager timer = new TimerManager();
	private final Hashtable<Player, DedicatedRespawnsManager> respawnerMap = new Hashtable<>();
}
