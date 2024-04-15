package net.bezeram.manhuntmod.game;

import com.mojang.brigadier.context.CommandContext;
import net.bezeram.manhuntmod.enums.DimensionID;
import net.bezeram.manhuntmod.events.ModEvents;
import net.bezeram.manhuntmod.game.players.PlayerData;
import net.bezeram.manhuntmod.gui.custom.ExtendedDeathScreen;
import net.bezeram.manhuntmod.networking.ModMessages;
import net.bezeram.manhuntmod.networking.packets.UpdateGameStateS2CPacket;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.ServerScoreboard;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.players.PlayerList;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.scores.Objective;
import net.minecraft.world.scores.PlayerTeam;
import net.minecraftforge.event.TickEvent;

import java.util.*;
import java.util.List;

public class Game {
	private static Game INSTANCE = null;

	private Game(PlayerTeam teamRunner, PlayerTeam teamHunter, PlayerList playerList, MinecraftServer server) {
		this.server = server;
		prevState = currentState;
		ModEvents.ForgeEvents.SuddenDeathWarning.hasTriggered = false;

		this.timer = new GameTimer();
		this.playerData = new PlayerData(teamRunner, teamHunter, playerList, timer, server);
	}

	public static void init(PlayerTeam teamRunner, PlayerTeam teamHunter, PlayerList playerList,
	                        MinecraftServer server) {
		INSTANCE = new Game(teamRunner, teamHunter, playerList, server);
		currentState = GameState.START;

		INSTANCE.updateClient();
	}

	public static boolean inSession() {
		// TODO: change this to INSTANCE != null
		//  Test if it crashes the game before committing :P
		return currentState != GameState.NULL;
	}

	private void updateClient() {
		for (ServerPlayer player : playerData.getPlayers())
			ModMessages.sendToPlayer(new UpdateGameStateS2CPacket(inSession()), player);
	}

	public static Game get() {
		if (INSTANCE == null)
			return null;
		return INSTANCE;
	}

	public static UUID cloneUUID(final UUID uuid) {
		return new UUID(uuid.getMostSignificantBits(), uuid.getLeastSignificantBits());
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
		INSTANCE = null;
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
			try {
				Vec3 lastPosition = player.getPosition(0);
				Vec3 currentPosition = player.getPosition(1);
				Vec3 deltaPos = new Vec3(currentPosition.x - lastPosition.x,
											currentPosition.y - lastPosition.y,
											currentPosition.z - lastPosition.z);
				if (deltaPos.x != 0 || deltaPos.y != 0 || deltaPos.z != 0)
					return false;
			} catch (NullPointerException ignored) {}
		}

		return true;
	}

	public static boolean canResumeGame() {
		return Game.getGameState() == GameState.PAUSE;
	}

	public void pauseGame() {
		playerData.updateCoords();

		ServerScoreboard scoreboard = server.getScoreboard();
		Objective objective = scoreboard.getObjective("TimeLeft");
		if (objective != null)
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

		if (timer.getPlayerPositionElapsed().asSeconds() > 0.1f) {
			playerData.updateAllCoords();
			timer.resetPlayerPositionTime();
		}

		switch (currentState) {
			case PAUSE -> {
				lockPlayersPos();
			}
			case RESUME -> {
				timer.updateResume();
				timer.updateResumeHints(event);
				lockPlayersPos();

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
				lockHuntersPos();
				lockRunnersPos();

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
				if (ManhuntGameRules.isTimeLimit())
					timer.updateActive();

				timer.updateHeadstart();
				timer.updateHeadstartHints(event);

				lockHuntersPos();

				if (timer.huntersHaveStarted()) {
					setGameState(GameState.ONGOING);
					PlayerList playerList = event.getServer().getPlayerList();
					playerList.broadcastSystemMessage(Component.literal("Hunters have been unleashed!"), false);
					/*
						Play pillager raid start of wave sound for all players
					*/
				}
			}
			case ONGOING -> {
				if (ManhuntGameRules.isTimeLimit()) {
					timer.updateActive();

					if (timer.activeTimeHasEnded())
						hunterHasWon();
				}

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

				if (objective != null) {
					scoreboard.getOrCreatePlayerScore(victoriousTeamDisplay, objective);
					scoreboard.addPlayerToTeam(victoriousTeamDisplay, winnerTeam);
				}

				setGameState(GameState.ERASE);
			}
			case ERASE -> {
				ModEvents.ForgeEvents.SuddenDeathWarning.hasTriggered = false;
				currentState = GameState.NULL;
			}
		}
	}

	public PlayerTeam getTeamRunner() { return playerData.getTeamRunner(); }
	public PlayerTeam getTeamHunter() { return playerData.getTeamHunter(); }
	public ServerPlayer[] getHuntersArray() { return playerData.getHunters(); }
	public ServerPlayer[] getRunnersArray() { return playerData.getRunners(); }

	public static boolean isHunterAtGameState(Player player, GameState targetGameState) {
		if (player.getTeam() == null || currentState != targetGameState || !Game.inSession())
			return false;

		try {
			String playerName = player.getName().getString();
			PlayerTeam hunterTeam = Game.get().getTeamHunter();
			for (String hunter : hunterTeam.getPlayers()) {
				if (hunter.contains(playerName)) {
					return true;
				}
			}

			return false;
		} catch (Exception ignored) { return false;}
	}

	public static boolean isRunnerAtGameState(Player player, GameState targetGameState) {
		if (player.getTeam() == null || currentState != targetGameState)
			return false;

		try {
			String playerName = player.getName().getString();
			PlayerTeam teamRunner = Game.get().getTeamRunner();
			for (String runner : teamRunner.getPlayers()) {
				if (runner.contains(playerName)) {
					return true;
				}
			}

			return false;
		} catch (Exception ignored) { return false; }
	}

	private void teleportIfMoving(final ServerPlayer serverPlayer) {
		try {
			Vec3 prevPos = playerData.getCoords(serverPlayer);
			Vec3 currentPos = serverPlayer.getPosition(1);
			if (prevPos == null)
				return;

			if (currentPos.x != prevPos.x ||
				currentPos.y != prevPos.y ||
				currentPos.z != prevPos.z)
				serverPlayer.teleportTo(prevPos.x, prevPos.y, prevPos.z);
		} catch (Exception ignored) {}
	}

	private void lockHuntersPos() {
		for (ServerPlayer player : playerData.getHunters())
			if (playerData.isHunter(player))
				teleportIfMoving(player);
	}

	private void lockRunnersPos() {
		for (ServerPlayer player : playerData.getRunners())
			if (playerData.isRunner(player))
				teleportIfMoving(player);
	}

	private void lockPlayersPos() {
		for (ServerPlayer player : playerData.getPlayers())
			teleportIfMoving(player);
	}

	public enum GameState {
		NULL, START, HEADSTART, ONGOING, END, RESUME, PAUSE, ERASE
	}

	private static GameState currentState = GameState.NULL;
	private static GameState prevState = GameState.NULL;
	private boolean runnerWins = true;

	public ServerPlayer getPlayer(UUID uuid) {
		return server.getPlayerList().getPlayer(uuid);
	}
	public ServerPlayer getPlayer(int MAID) { return playerData.getPlayer(MAID); }

	public static DimensionID getDimensionID(final ResourceKey<Level> dimension) {
		if (dimension == Level.OVERWORLD)
			return DimensionID.OVERWORLD;
		if (dimension == Level.NETHER)
			return DimensionID.NETHER;
		if (dimension == Level.END)
			return DimensionID.END;
		return DimensionID.NULL;
	}

	public static ResourceKey<Level> getDimensionByID(final DimensionID ID) {
		return switch (ID) {
			case OVERWORLD -> Level.OVERWORLD;
			case NETHER -> Level.NETHER;
			case END -> Level.END;
			default -> null;
		};
	}

	public static void LOG(final String log) {
		System.out.println("[LOG] " + log);
	}

	public final PlayerData getPlayerData() { return playerData; }

	private final PlayerData playerData;

	private final GameTimer timer;

	private final MinecraftServer server;
}
