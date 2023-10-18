package net.bezeram.manhuntmod.game_manager;

import com.mojang.brigadier.context.CommandContext;
import net.bezeram.manhuntmod.events.ModEvents;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.nbt.ListTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.players.PlayerList;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.dimension.BuiltinDimensionTypes;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.scores.PlayerTeam;
import net.minecraftforge.event.TickEvent;

import java.util.*;

public class Game {
	private static Game GAME_INSTANCE = null;

	private Game(PlayerTeam teamRunner, PlayerTeam teamHunter, PlayerList playerList) {
		this.teamRunner = teamRunner;
		this.teamHunter = teamHunter;
		currentState = GameState.START;
		prevState = currentState;
		ModEvents.ForgeEvents.SuddenDeathWarning.hasTriggered = false;

		for (ServerPlayer player : playerList.getPlayers())
			if (isHunter(player))
				huntersStartCoords.put(player.getName().getString(), player.getPosition(1));
			else if (isRunner(player))
				runnersStartCoords.put(player.getName().getString(), player.getPosition(1));
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

	public static GameState getPrevGameState() {
		return prevState;
	}

	public static void setGameState(GameState state) {
		prevState = currentState;
		currentState = state;
	}

	public static void stopGame() {
		currentState = GameState.NULL;
		GAME_INSTANCE = null;
	}

	public void applyDeathPenalty(Level level) {
		switch (ManhuntGameRules.DEATH_PENALTY) {
			case TRUE -> {
				timer.deathPenalty();
			}
			case TRUE_EXCEPT_END -> {
				boolean diedInEnd = level.dimensionTypeRegistration().is(BuiltinDimensionTypes.END);
				if (!diedInEnd) {
					timer.deathPenalty();
				}
			}
		}
	}

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

	public static boolean canPauseGame(CommandContext<CommandSourceStack> command) {
		if (!Game.isInSession() && Game.getGameState() == GameState.PAUSE)
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

	public static boolean canResumeGame(CommandContext<CommandSourceStack> command) {
		return Game.getGameState() == GameState.PAUSE;
	}

	public void pauseGame(PlayerList playerList) {
		for (ServerPlayer player : playerList.getPlayers())
			playersPrevCoords.put(player.getName().getString(), player.getPosition(1));

		prevState = currentState;
		currentState = GameState.PAUSE;
	}

	public void resumeGame() {
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
						TODO: Play pillager raid start of wave sound,
						 this bs does not work
					*/

//					for (ServerPlayer player : playerList.getPlayers()) {
//						player.playSound(SoundEvents.PILLAGER_CELEBRATE);
//					}
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
				PlayerTeam winnerTeam = (runnerWins) ? teamRunner : teamHunter;
				PlayerTeam loserTeam  = (!runnerWins) ? teamRunner : teamHunter;

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

				setGameState(GameState.ERASE);
				ModEvents.ForgeEvents.SuddenDeathWarning.hasTriggered = false;
			}
			case ERASE -> {}
		}
	}

	public PlayerTeam getTeamRunner() { return teamRunner; }
	public PlayerTeam getTeamHunter() { return teamHunter; }

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
		if (player.getTeam() == null)
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

	private void lockRunnersPos(TickEvent.ServerTickEvent event) {
		PlayerList playerList = event.getServer().getPlayerList();
		for (ServerPlayer player : playerList.getPlayers())
			if (isRunner(player)) {
				Vec3 coords = runnersStartCoords.get(player.getName().getString());
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
			Vec3 coords = playersPrevCoords.get(player.getName().getString());
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
		NULL, START, HEADSTART, ONGOING, END, RESUME, PAUSE, ERASE
	}

	private static GameState currentState = GameState.NULL;
	private static GameState prevState = GameState.NULL;
	private boolean runnerWins = true;

	private final PlayerTeam teamRunner;
	private final PlayerTeam teamHunter;
	private final Hashtable<String, Vec3> huntersStartCoords = new Hashtable<>();
	private final Hashtable<String, Vec3> runnersStartCoords = new Hashtable<>();
	private final Hashtable<String, Vec3> playersPrevCoords = new Hashtable<>();

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

				if (level.dimension() == Level.OVERWORLD)
					PlayerLastLocations.Overworld.update(name, newPosition);
				else if (level.dimension() == Level.NETHER)
					PlayerLastLocations.Nether.update(name, newPosition);
				else if (level.dimension() == Level.END)
					PlayerLastLocations.End.update(name, newPosition);
			}
		}

		public Vec3 getLastPosition(String playerName) {
			return lastPlayerPosition.get(playerName);
		}

		private final Hashtable<String, Vec3> lastPlayerPosition = new Hashtable<>();
	}

	private final Hashtable<String, Inventory> playerInventories = new Hashtable<>();

	private final TimerManager timer = new TimerManager();
	private final Hashtable<Player, DedicatedRespawnsManager> respawnerMap = new Hashtable<>();
}
