package net.bezeram.manhuntmod.game;

import com.mojang.brigadier.context.CommandContext;
import net.bezeram.manhuntmod.enums.DimensionID;
import net.bezeram.manhuntmod.events.ModEvents;
import net.bezeram.manhuntmod.game.players.PlayerData;
import net.bezeram.manhuntmod.networking.ModMessages;
import net.bezeram.manhuntmod.networking.packets.UpdateGameStateS2CPacket;
import net.bezeram.manhuntmod.networking.packets.UpdatePortalRespawnS2CPacket;
import net.bezeram.manhuntmod.utils.MHUtils;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.ServerScoreboard;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.players.PlayerList;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.scores.Objective;
import net.minecraft.world.scores.PlayerTeam;
import net.minecraftforge.event.TickEvent;
import org.jetbrains.annotations.NotNull;

import java.util.*;
import java.util.List;

public class Game {
	private static Game INSTANCE = null;

	private Game(PlayerTeam teamRunner, PlayerTeam teamHunter, PlayerList playerList, MinecraftServer server) {
		this.server = server;
		prevState = gameState;
		ModEvents.ForgeEvents.SuddenDeathWarning.hasTriggered = false;

		this.timer = new GameTimer();
		this.playerData = new PlayerData(teamRunner, teamHunter, playerList, timer, server);
	}

	public static void init(PlayerTeam teamRunner, PlayerTeam teamHunter, PlayerList playerList,
	                        MinecraftServer server) {
		INSTANCE = new Game(teamRunner, teamHunter, playerList, server);
		gameState = GameState.START;

		INSTANCE.updateClient();
	}

	public static boolean inSession() {
		return INSTANCE != null && gameState != GameState.NULL;
	}

	private void updateClient() {
		for (ServerPlayer player : playerData.getPlayers())
			ModMessages.sendToPlayer(
					new UpdateGameStateS2CPacket(inSession(), playerData.isEndLocked(player.getUUID())), player);
	}

	public static Game get() {
		return INSTANCE;
	}

	public static UUID cloneUUID(final UUID uuid) {
		return new UUID(uuid.getMostSignificantBits(), uuid.getLeastSignificantBits());
	}

	public static GameState getGameState() {
		return gameState;
	}

	public static GameState getPrevGameState() {
		return prevState;
	}

	public static void setGameState(GameState state) {
		prevState = gameState;
		gameState = state;
	}

	public void stopGame() {
		gameState = GameState.ERASE;
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

		prevState = gameState;
		gameState = GameState.PAUSE;
	}

	public void resumeGame() {
		ServerScoreboard scoreboard = server.getScoreboard();
		Objective objective = scoreboard.getObjective("TimeLeft");
		assert objective != null;
		scoreboard.resetPlayerScore("PAUSED", objective);

		gameState = GameState.RESUME;
	}

	public void update(TickEvent.ServerTickEvent event) {
		timer.updatePlayerPosition();
		timer.updatePortalRespawnCheck();

		if (timer.getPlayerPositionElapsed().asSeconds() > 2.f) {
			playerData.updateAllCoords();
			timer.resetPlayerPositionTime();
		}

		switch (gameState) {
			case PAUSE -> {
				lockPlayersPos();
			}
			case RESUME -> {
				timer.updateResume();
				timer.updateResumeHints(event);
				lockPlayersPos();

				if (timer.gameResumed()) {
					gameState = prevState;
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
				ServerScoreboard scoreboard = server.getScoreboard();
				Objective objective = scoreboard.getObjective("TimeLeft");
				if (objective != null) {
					scoreboard.resetPlayerScore("PAUSED", objective);
					scoreboard.getOrCreatePlayerScore("STOPPED", objective);
				}

				gameState = GameState.NULL;
				updateClient();
			}
		}
	}

	public PlayerTeam getTeamRunner() { return playerData.getTeamRunner(); }
	public PlayerTeam getTeamHunter() { return playerData.getTeamHunter(); }
	public ServerPlayer[] getHuntersArray() { return playerData.getHunters(); }
	public ServerPlayer[] getRunnersArray() { return playerData.getRunners(); }

	public static boolean isHunterAtGameState(Player player, GameState targetGameState) {
		if (player.getTeam() == null || gameState != targetGameState || !Game.inSession())
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
		if (player.getTeam() == null || gameState != targetGameState)
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
			Vec3 prevPos = playerData.getLastPosition(serverPlayer);
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

	public void updateScoreboard(TickEvent.ServerTickEvent event) {
		// Update scoreboard
		ServerScoreboard scoreboard = event.getServer().getScoreboard();
		Objective objective = scoreboard.getObjective("TimeLeft");

		if (objective != null) {
			int secondsLeft = (int)(Game.get().getGameTime().asSeconds() - Game.get().getElapsedTime().asSeconds());
			int minutesLeft = secondsLeft / 60;

			String scoreLabel   = (minutesLeft >= 1) ? "Minutes" : "Seconds";
			int score           = (minutesLeft >= 1) ? minutesLeft : secondsLeft;
			if (minutesLeft < 1) {
				scoreboard.resetPlayerScore("Minutes", objective);
			}

			scoreboard.getOrCreatePlayerScore(scoreLabel, objective).setScore(score);

			// Sudden death
			if (Game.get().isSuddenDeath() && !ModEvents.ForgeEvents.SuddenDeathWarning.hasTriggered) {
				ModEvents.ForgeEvents.SuddenDeathWarning.broadcastMessage(event.getServer().getPlayerList(), Game.get().getTimeLeft());
				PlayerTeam timeHighlight = scoreboard.addPlayerTeam("SuddenDeath");
				scoreboard.addPlayerToTeam("Minutes", timeHighlight);
				scoreboard.addPlayerToTeam("Seconds", timeHighlight);
			}

			if (ModEvents.ForgeEvents.SuddenDeathWarning.hasTriggered) {
				// Cycle highlight
				ModEvents.ForgeEvents.SuddenDeathWarning.updateScoreboardTime();
				PlayerTeam playerTeam = scoreboard.getPlayerTeam("SuddenDeath");

				assert playerTeam != null;
				playerTeam.setColor(ModEvents.ForgeEvents.SuddenDeathWarning.scoreboardTimeColor);
			}
		}
		else
			Game.LOG("ERROR: Game display scoreboard has null objective");
	}

	// Gets the block along one axis depending on the player's hitbox.
	// Used to detect if the player is inside a portal block.
	private static int getOffsetTowardsPortal(double pos) {
		if (MHUtils.fractional(pos) <= 0.3)
			return -1;
		else if (MHUtils.fractional(pos) >= 0.7)
			return 1;
		return 0;
	}

	private static BlockPos selectPortalBlock(BlockState[] blockStates, BlockPos[] blockPos) {
		if (blockStates[0].getBlock() == Blocks.NETHER_PORTAL)
			return blockPos[0];
		else if (blockStates[1].getBlock() == Blocks.NETHER_PORTAL)
			return blockPos[1];
		return null;
	}

	/**
	 * Obtains the blocks corresponding to the direction a portal might be in.
	 * Checking if player is inside a portal block.
	 * To precisely check if the player is inside a portal block
	 *  the function must account for the player being slightly in between two blocks.
	 * Depending on the player's position, we check in a specific direction for portal blocks.
	 * For a lower fractional value (<=0.3) we check in the negative direction.
	 * For a higher fractional value (>=0.7) we check in the positive direction.
	 * @param player Potential traveller
	 * @return Returns the possible BlockPos where the portal is located
	 */
	private static BlockPos getBlockTowardsPortal(final ServerPlayer player) {
		try {

			Vec3 pos = player.getPosition(1);
			int offsetX = getOffsetTowardsPortal(pos.x);
			int offsetZ = getOffsetTowardsPortal(pos.z);
			BlockPos targetBlockX = new BlockPos((int)pos.x + offsetX,  (int)pos.y, (int)pos.z);
			BlockPos targetBlockZ = new BlockPos((int)pos.x,            (int)pos.y, (int)pos.z + offsetZ);
			BlockPos[] blockPos = { targetBlockX, targetBlockZ };
			BlockState[] blockStates = new BlockState[] {
				player.getLevel().getBlockState(blockPos[0]),
				player.getLevel().getBlockState(blockPos[1])
			};

			return selectPortalBlock(blockStates, blockPos);
		} catch (Exception ignored) {
			return null;
		}
	}

	public void ifPlayersInPortal() {

		for (int i = 0; i < playerData.getPlayers().length; i++) {
			try {
				ServerPlayer player = playerData.getPlayers()[i];
				BlockPos blockPos = getBlockTowardsPortal(player);
				if (blockPos != null) {
					updatePortalCoords(player, blockPos);

					// Players in a Nether Portal are safeguarded whilst in the portal
					if (gameState != GameState.PAUSE && gameState != GameState.RESUME) {
						if (!player.isInvulnerable())
							Game.LOG("[PortalCheck] Setting player invulnerable: " + player.getName().getString());

						player.setInvulnerable(true);
					}
				}
				else if (gameState != GameState.PAUSE && gameState != GameState.RESUME) {
					if (player.isInvulnerable())
						Game.LOG("[PortalCheck] Setting player NOT invulnerable: " + player.getName().getString());

					player.setInvulnerable(false);
				}
			} catch (Exception ignored) {}
		}
	}

	public void updatePortalCoords(@NotNull final ServerPlayer player, @NotNull BlockPos portalCoords) {
		if (timer.portalRespawnCheck()) {
			timer.resetPortalRespawnCheck();

			try {
				Game.get().getPlayerData().updateNetherPortalPosition(player.getUUID(), portalCoords);

				ModMessages.sendToPlayer(new UpdatePortalRespawnS2CPacket(portalCoords), player);
			} catch (Exception ignored) {}
		}
	}

	public enum GameState {
		NULL, START, HEADSTART, ONGOING, END, RESUME, PAUSE, ERASE
	}

	private static GameState gameState = GameState.NULL;
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
