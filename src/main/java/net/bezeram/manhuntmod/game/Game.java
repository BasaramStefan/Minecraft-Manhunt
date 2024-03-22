package net.bezeram.manhuntmod.game;

import com.mojang.brigadier.context.CommandContext;
import net.bezeram.manhuntmod.events.ModEvents;
import net.bezeram.manhuntmod.game.players.PlayerData;
import net.bezeram.manhuntmod.item.custom.HunterCompassItem;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.nbt.ListTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.ServerScoreboard;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.players.PlayerList;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.scores.Objective;
import net.minecraft.world.scores.PlayerTeam;
import net.minecraftforge.event.TickEvent;

import java.util.*;

public class Game {
	private static Game GAME_INSTANCE = null;

	private Game(PlayerTeam teamRunner, PlayerTeam teamHunter, PlayerList playerList, MinecraftServer server) {
		this.server = server;
		currentState = GameState.START;
		prevState = currentState;
		ModEvents.ForgeEvents.SuddenDeathWarning.hasTriggered = false;

		this.timer = new Timer();
		this.playerData = new PlayerData(teamRunner, teamHunter, playerList, timer);
	}

	public static void init(PlayerTeam teamRunner, PlayerTeam teamHunter, PlayerList playerList,
	                        MinecraftServer server) {
		GAME_INSTANCE = new Game(teamRunner, teamHunter, playerList, server);
	}

	public static boolean inSession() {
		return GAME_INSTANCE != null;
	}

	public static Game get() {
		return GAME_INSTANCE;
	}

	public static GameState getGameState() {
		return currentState;
	}

	public static GameState getPrevGameState() {
		return prevState;
	}

	public static void setGameState(GameState state) {
		prevState = currentState;
		currentState = state;
	}

	public void stopGame() {
		ServerScoreboard scoreboard = server.getScoreboard();
		Objective objective = scoreboard.getObjective("TimeLeft");
		if (objective != null) {
			scoreboard.resetPlayerScore("PAUSED", objective);
			scoreboard.getOrCreatePlayerScore("STOPPED", objective);
		}

		currentState = GameState.NULL;
		GAME_INSTANCE = null;
	}

	public MinecraftServer getServer() { return server; }

	public Time getElapsedTime()    { return timer.getGameElapsed(); }
	public Time getGameTime()       { return timer.getSessionGame(); }
	public Time getStartDelay()     { return timer.getSessionStart(); }
	public Time getHeadstartTime()  { return timer.getSessionHeadstart(); }
	public Time getPauseTime()      { return timer.getSessionPause(); }
	public Time getResumeDelay()    { return timer.getSessionResume(); }
	public Time getTimeLeft()       { return Time.TimeTicks(timer.getSessionGame().asTicks() - getElapsedTime().asTicks()); }
	public void resetResumeTime()   { timer.resetResumeHints(); }
	public boolean isSuddenDeath()  { return getTimeLeft().asTicks() < timer.getSessionDeathPenalty().asTicks(); }

	public void runnerHasWon() {
		setGameState(GameState.END);
		runnerWins = true;
	}

	public void hunterHasWon() {
		setGameState(GameState.END);
		runnerWins = false;
	}

	public static boolean canPauseGame(CommandContext<CommandSourceStack> command) {
		if (!Game.inSession() && Game.getGameState() == GameState.PAUSE)
			return false;

		List<ServerPlayer> playersList = command.getSource().getServer().getPlayerList().getPlayers();
		for (ServerPlayer player : playersList) {
			Vec3 lastPosition = PlayerLastLocations.Overworld.getLastPosition(player.getName().getString());
			Vec3 currentPosition = player.getPosition(1);
			Vec3 deltaPos = new Vec3(currentPosition.x - lastPosition.x,
										currentPosition.y - lastPosition.y,
										currentPosition.z - lastPosition.z);
			if (deltaPos.x != 0 || deltaPos.y != 0 || deltaPos.z != 0)
				return false;
		}

		return true;
	}

	public static boolean canResumeGame() {
		return Game.getGameState() == GameState.PAUSE;
	}

	public void pauseGame() {
		playerData.updatePlayersPrevPosition();

		ServerScoreboard scoreboard = server.getScoreboard();
		Objective objective = scoreboard.getObjective("TimeLeft");
		assert objective != null;
		scoreboard.getOrCreatePlayerScore("PAUSED", objective);

		prevState = currentState;
		currentState = GameState.PAUSE;
	}

	public void resumeGame() {
		ServerScoreboard scoreboard = server.getScoreboard();
		Objective objective = scoreboard.getObjective("TimeLeft");
		assert objective != null;
		scoreboard.resetPlayerScore("PAUSED", objective);

		currentState = GameState.RESUME;
	}

	public void update(TickEvent.ServerTickEvent event) {
		timer.updatePlayerPosition();
		if (timer.getPlayerPositionElapsed().asSeconds() > 1) {
			PlayerLastLocations.updateAll(event);
			timer.resetPlayerPositionTime();
		}

		switch (currentState) {
			case PAUSE -> {
				lockPlayersPos(event);
			}
			case RESUME -> {
				timer.updateResume();
				timer.updateResumeHints(event);
				lockPlayersPos(event);

				if (timer.gameResumed()) {
					currentState = prevState;
					PlayerList allPlayers = event.getServer().getPlayerList();
					allPlayers.broadcastSystemMessage(Component
									.literal("Game resumed").withStyle(ChatFormatting.DARK_GREEN), false);

					for (ServerPlayer player : allPlayers.getPlayers()) {
						player.setInvulnerable(false);
						player.setInvisible(false);
					}
				}
			}
			case START -> {
				timer.updateStart();
				timer.updateStartHints(event);
				lockHuntersPos(event);
				lockRunnersPos(event);

				if (timer.runnersHaveStarted()) {
					if (ManhuntGameRules.HEADSTART)
						setGameState(GameState.HEADSTART);
					else
						setGameState(GameState.ONGOING);

					PlayerList playerList = event.getServer().getPlayerList();
					playerList.broadcastSystemMessage(Component
							.literal("GO!").withStyle(ChatFormatting.DARK_GREEN), false);
				}
			}
			case HEADSTART -> {
				if (ManhuntGameRules.TIME_LIMIT)
					timer.updateActive();

				timer.updateHeadstart();
				timer.updateHeadstartHints(event);

				lockHuntersPos(event);

				if (timer.huntersHaveStarted()) {
					setGameState(GameState.ONGOING);
					PlayerList playerList = event.getServer().getPlayerList();
					playerList.broadcastSystemMessage(Component.literal("Hunters have been unleashed!"), false);
					/*
						TODO: Play pillager raid start of wave sound for all players
					*/
				}
			}
			case ONGOING -> {
				if (ManhuntGameRules.TIME_LIMIT) {
					timer.updateActive();

					if (timer.activeTimeHasEnded())
						hunterHasWon();
				}

				// TODO:
				// Update compass
				// Use the Beacon powered / unpowered sounds for when it detects dimension change
			}
			case END -> {
				// Common end of game functionality
				PlayerList playerList = event.getServer().getPlayerList();
				PlayerTeam winnerTeam = (runnerWins) ? playerData.getTeamRunner() : playerData.getTeamHunter();
				PlayerTeam loserTeam  = (!runnerWins) ? playerData.getTeamRunner() : playerData.getTeamHunter();

				String feedbackServer = "";
				boolean team;
				if (winnerTeam.getPlayers().size() == 1) {
					feedbackServer = winnerTeam.getDisplayName().getString() + " has won the game!";
					team = false;
				}
				else {
					feedbackServer = winnerTeam.getDisplayName().getString() + " team has won the game!";
					team = true;
				}
				playerList.broadcastSystemMessage(Component.literal(feedbackServer), false);

				// TODO: Play sounds correctly, this bs does not work
//				for (String playerName : winnerTeam.getPlayers()) {
//					ServerPlayer player = playerList.getPlayerByName(playerName);
//
//					if (player != null)
//						player.playSound(SoundEvents.PLAYER_LEVELUP);
//				}
//
//				for (String playerName : loserTeam.getPlayers()) {
//					ServerPlayer player = playerList.getPlayerByName(playerName);
//
//					if (player != null)
//						player.playSound(SoundEvents.PILLAGER_CELEBRATE);
//				}

				ServerScoreboard scoreboard = event.getServer().getScoreboard();
				Objective objective = scoreboard.getObjective("TimeLeft");
				String victoriousTeamDisplay = (runnerWins) ? "RUNNER" : "HUNTER";
				if (team) {
					victoriousTeamDisplay += "S";
					victoriousTeamDisplay += " WIN";
				}
				else {
					victoriousTeamDisplay += " WINS";
				}

				assert objective != null;
				scoreboard.getOrCreatePlayerScore(victoriousTeamDisplay, objective);
				scoreboard.addPlayerToTeam(victoriousTeamDisplay, winnerTeam);

				setGameState(GameState.ERASE);
			}
			case ERASE -> {
				ModEvents.ForgeEvents.SuddenDeathWarning.hasTriggered = false;
				HunterCompassItem.clearCompassList();
			}
		}
	}

	public PlayerTeam getTeamRunner() { return playerData.getTeamRunner(); }
	public PlayerTeam getTeamHunter() { return playerData.getTeamHunter(); }
	public ServerPlayer[] getHuntersArray() { return playerData.getListHunters(); }
	public ServerPlayer[] getRunnersArray() { return playerData.getListRunners(); }

	public static boolean isHunterAtGameState(Player player, GameState targetGameState) {
		if (player.getTeam() == null || currentState != targetGameState)
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

	public static boolean isRunnerAtGameState(Player player, GameState targetGameState) {
		if (player.getTeam() == null || currentState != targetGameState)
			return false;

		String playerName = player.getName().getString();
		PlayerTeam teamRunner = Game.get().getTeamRunner();
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
			if (playerData.isHunter(player)) {
				Vec3 coords = playerData.getHuntersStartCoords().get(player.getName().getString());
				Vec3 currentPlayerCoords = player.getPosition(1);

				if (currentPlayerCoords.x != coords.x ||
						currentPlayerCoords.y != coords.y ||
						currentPlayerCoords.z != coords.z) {
					player.teleportTo(coords.x, coords.y, coords.z);
				}
			}
	}

	private void lockRunnersPos(TickEvent.ServerTickEvent event) {
		PlayerList playerList = event.getServer().getPlayerList();
		for (ServerPlayer player : playerList.getPlayers())
			if (playerData.isRunner(player)) {
				Vec3 coords = playerData.getRunnersStartCoords().get(player.getName().getString());
				Vec3 currentPlayerCoords = player.getPosition(1);

				if (currentPlayerCoords.x != coords.x ||
						currentPlayerCoords.y != coords.y ||
						currentPlayerCoords.z != coords.z) {
					player.teleportTo(coords.x, coords.y, coords.z);
				}
			}
	}

	private void lockPlayersPos(TickEvent.ServerTickEvent event) {
		PlayerList playerList = event.getServer().getPlayerList();
		for (ServerPlayer player : playerList.getPlayers()) {
			Vec3 coords = playerData.getPlayersPrevCoords().get(player.getName().getString());
			Vec3 currentPlayerCoords = player.getPosition(1);

			if (currentPlayerCoords.x != coords.x ||
					currentPlayerCoords.y != coords.y ||
					currentPlayerCoords.z != coords.z) {
				player.teleportTo(coords.x, coords.y, coords.z);
			}
		}
	}

	public enum GameState {
		NULL, START, HEADSTART, ONGOING, END, RESUME, PAUSE, ERASE
	}

	private static GameState currentState = GameState.NULL;
	private static GameState prevState = GameState.NULL;
	private boolean runnerWins = true;

	public static int getDimensionID(ResourceKey<Level> dimension) {
		if (dimension == Level.OVERWORLD)
			return 0;
		if (dimension == Level.NETHER)
			return 1;
		if (dimension == Level.END)
			return 2;
		return -1;
	}

	public static int getDimensionIDByName(String name) {
		switch (name) {
			case "Overworld" -> {
				return 0;
			}
			case "Nether" -> {
				return 1;
			}
			case "End" -> {
				return 2;
			}
			default -> {
				return -1;
			}
		}
	}

	public static String getDimensionNameByID(int ID) {
		switch (ID) {
			case 0 -> {
				return "Overworld";
			}
			case 1 -> {
				return "Nether";
			}
			case 2 -> {
				return "End";
			}
			default -> {
				return null;
			}
		}
	}

	public enum PlayerLastLocations {
		Overworld, Nether, End;

		public void update(String playerName, Vec3 newPosition) {
			lastPlayerPosition.put(playerName, newPosition);
		}

		public static void updateAll(TickEvent.ServerTickEvent event) {
			PlayerList allPlayers = event.getServer().getPlayerList();
			for (ServerPlayer player : allPlayers.getPlayers()) {
				ServerLevel level = player.getLevel();
				String name = player.getName().getString();
				Vec3 newPosition = player.getPosition(0);

				PlayerLastLocations location = getByDimension(level.dimension());

				if (location != null)
					location.update(name, newPosition);
			}
		}

		public static PlayerLastLocations getByDimension(ResourceKey<Level> dimension) {
			if (dimension == Level.OVERWORLD)
				return PlayerLastLocations.Overworld;
			else if (dimension == Level.NETHER)
				return PlayerLastLocations.Nether;
			else if (dimension == Level.END)
				return PlayerLastLocations.End;
			return null;
		}

		public Vec3 getLastPosition(String playerName) {
			return lastPlayerPosition.get(playerName);
		}

		private final Hashtable<String, Vec3> lastPlayerPosition = new Hashtable<>();
	}

	public final PlayerData getPlayerData() { return playerData; }

	private final PlayerData playerData;

	private final Timer timer;

	private final MinecraftServer server;
}
