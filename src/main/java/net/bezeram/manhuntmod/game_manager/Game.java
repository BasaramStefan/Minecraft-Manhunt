package net.bezeram.manhuntmod.game_manager;

import net.minecraft.network.chat.Component;
import net.minecraft.server.players.PlayerList;
import net.minecraft.world.scores.PlayerTeam;
import net.minecraftforge.event.TickEvent;

public class Game {

	private static Game GAME_INSTANCE = null;

	private Game(PlayerTeam teamRunner, PlayerTeam teamHunter) {
		this.teamRunner = teamRunner;
		this.teamHunter = teamHunter;
		this.timer = new TimerManager();
		currentState = GameState.HEADSTART;
	}

	public static Game init(PlayerTeam teamRunner, PlayerTeam teamHunter) {
		return GAME_INSTANCE = new Game(teamRunner, teamHunter);
	}
	public static boolean isInSession() {
		return GAME_INSTANCE != null;
	}
	public static Game get() {
		return GAME_INSTANCE;
	}

	public static void stopGame() {
		toggleRules(false);
		currentState = GameState.NULL;
		GAME_INSTANCE = null;
	}

	public static GameState getGameState() { return currentState; }

	public static void toggleRules(boolean enabled) {
		if (currentState == GameState.NULL ||
				currentState == GameState.GAME_END ||
				currentState == GameState.RUNNER_WIN ||
				currentState == GameState.HUNTER_WIN)
		{
			rulesEnabled = enabled;
		}
	}

	public static boolean rulesEnabled() { return rulesEnabled; }

	public Time getElapsedTime() 	{ return timer.getTime(); }

	public void runnerHasWon() { currentState = GameState.RUNNER_WIN; }

	public PlayerTeam getTeamRunner() {
		return teamRunner;
	}
	public PlayerTeam getTeamHunter() {
		return teamHunter;
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
						TODO:
						Play pillager raid start of wave sound
					*/
				}
			}
			case ONGOING -> {
				timer.updateActive();

				// Update the game
				if (timer.activeTimeHasEnded())
					currentState = GameState.HUNTER_WIN;

				// TODO:
				// Update compass
			}
			case HUNTER_WIN -> {
				// TODO:
				// Display winner team with title on everyone's screen
				// Cease all the restrictive rules
				Game.toggleRules(false);

				PlayerList playerList = event.getServer().getPlayerList();

				String feedbackServer = "";
				if (teamHunter.getPlayers().size() == 1)
					feedbackServer = teamHunter.getDisplayName() + " has won the game!";
				else
					feedbackServer = teamHunter.getDisplayName() + " team has won the game!";
				playerList.broadcastSystemMessage(Component.literal(feedbackServer), false);
				
				currentState = GameState.GAME_END;
			}
			case RUNNER_WIN -> {
				Game.toggleRules(false);

				PlayerList playerList = event.getServer().getPlayerList();

				String feedbackServer = "";
				if (teamRunner.getPlayers().size() == 1)
					feedbackServer = teamRunner.getDisplayName() + " has won the game!";
				else
					feedbackServer = teamRunner.getDisplayName() + " team has won the game!";
				playerList.broadcastSystemMessage(Component.literal(feedbackServer), false);
				currentState = GameState.GAME_END;
			}
			case GAME_END -> {
				// Common end of game functionality

			}
		}
	}

	// TODO:
	// Implement the PAUSE game state
	public enum GameState {
		NULL, HEADSTART, ONGOING, RUNNER_WIN, HUNTER_WIN, GAME_END
	}
	private static GameState currentState = GameState.NULL;

	private final PlayerTeam teamRunner;
	private final PlayerTeam teamHunter;

	private final TimerManager timer;

	private static boolean rulesEnabled = false;
}
