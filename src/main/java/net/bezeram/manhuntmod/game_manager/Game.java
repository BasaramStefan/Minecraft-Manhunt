package net.bezeram.manhuntmod.game_manager;

import net.minecraft.ChatFormatting;
import net.minecraft.client.resources.sounds.Sound;
import net.minecraft.network.chat.Component;
import net.minecraft.server.ServerScoreboard;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.players.PlayerList;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.GameRules;
import net.minecraft.world.level.dimension.BuiltinDimensionTypes;
import net.minecraft.world.scores.Objective;
import net.minecraft.world.scores.PlayerTeam;
import net.minecraftforge.event.TickEvent;

import java.util.*;

public class Game {
	// TODO: Add event: When EnderDragon dies -> runner (team) wins

	private static Game GAME_INSTANCE = null;

	private Game(PlayerTeam teamRunner, PlayerTeam teamHunter) {
		timer           = new TimerManager();
		respawnerMap    = new Hashtable<>();
		this.teamRunner = teamRunner;
		this.teamHunter = teamHunter;
		currentState = GameState.HEADSTART;
	}

	public static void init(PlayerTeam teamRunner, PlayerTeam teamHunter) {
		GAME_INSTANCE = new Game(teamRunner, teamHunter);
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

	public boolean canRespawnDedicated(Player player) {
		if (respawnerMap.containsKey(player)) {
			return respawnerMap.get(player).canRespawnDedicated(player);
		}

		return false;
	}

	public void update(TickEvent.ServerTickEvent event) {
		switch (currentState) {
			case HEADSTART -> {
				/*
					TODO:
				 	Do not allow hunters to move or break blocks
				*/
				timer.updateActive();
				timer.updateHeadstart();
				timer.updateHeadstartHints(event);

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

	// TODO: Implement the PAUSE game state
	public enum GameState {
		NULL, HEADSTART, ONGOING, END, ERASE
	}
	private static GameState currentState = GameState.NULL;
	private boolean rulesEnabled = false;
	private boolean runnerWins = true;

	private final PlayerTeam teamRunner;
	private final PlayerTeam teamHunter;

	private final Hashtable<String, Inventory> playerInventories = new Hashtable<>();

	private final TimerManager timer;
	private final Hashtable<Player, DedicatedRespawnsManager> respawnerMap;
}
