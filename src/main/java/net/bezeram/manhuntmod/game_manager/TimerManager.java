package net.bezeram.manhuntmod.game_manager;

import net.minecraft.network.chat.Component;
import net.minecraft.server.players.PlayerList;
import net.minecraftforge.event.TickEvent;

public class TimerManager {
	private static Time RUNNER_LIMIT 			= Time.TimeMinutes(90);
	private static Time HEADSTART 				= Time.TimeSeconds(30);
	private static Time DEATH_PENALTY 			= Time.TimeMinutes(5);
	private static Time PAUSE 					= Time.TimeMinutes(10);
	private static Time HEADSTART_HINT_BOUND 	= Time.TimeSeconds(5);

	public TimerManager() {
		activeGame 		= new Time();
		activeHeadstart = new Time();
		game 			= RUNNER_LIMIT;
		headstart 		= HEADSTART;
		deathPenalty 	= DEATH_PENALTY;
		pause			= PAUSE;
	}

	public void updateActive() 		{ activeGame.advance(); }
	public void updateHeadstart() 	{ activeHeadstart.advance(); }
	public void deathPenalty() 		{ activeGame.advance(deathPenalty); }
	public void updateHeadstartHints(TickEvent.ServerTickEvent event) {
		if (activeHeadstartHints()) {
			displayHeadstartHint(event);
		}
	}

	public boolean activeTimeHasEnded()		{ return activeGame.asTicks() 		>= game.asTicks(); }
	public boolean huntersHaveStarted()		{ return activeHeadstart.asTicks() 	>= headstart.asTicks(); }
	public boolean activeHeadstartHints()	{ return activeHeadstart.asTicks() 	<= headstart.asTicks(); }

	public static void setGameTime(double minutes) 		{ RUNNER_LIMIT 		= Time.TimeMinutes(minutes); }
	public static void setHeadstart(double seconds) 	{ HEADSTART 		= Time.TimeSeconds(seconds); }
	public static void setDeathPenalty(double minutes) 	{ DEATH_PENALTY 	= Time.TimeMinutes(minutes); }
	public static void setPauseTime(double minutes) 	{ PAUSE 			= Time.TimeMinutes(minutes); }

	public static Time getGameTime() 		{ return RUNNER_LIMIT;  }
	public static Time getHeadstart() 		{ return HEADSTART;  	}
	public static Time getDeathPenalty() 	{ return DEATH_PENALTY; }
	public static Time getPauseTime() 		{ return PAUSE; 	 	}

	public static long minutesToTicks(double minutes) 	{ return (long)(minutes * 2400.f); }
	public static long secondsToTicks(double seconds) 	{ return (long)(seconds * 40.f); }
	public static double ticksToSeconds(long ticks) 	{ return ((double)ticks) / 40.f; }
	public static double ticksToMinutes(long ticks) 	{ return ((double)ticks) / 2400.f; }

	public Time getTime() { return activeGame; }

	public void displayHeadstartHint(TickEvent.ServerTickEvent event) {
		PlayerList playerList = event.getServer().getPlayerList();
		double seconds = headstart.asSeconds() - activeHeadstart.asSeconds();
		seconds = round(seconds, 1);
		playerList.broadcastSystemMessage(Component.literal(String.valueOf(seconds)), true);
	}

	private static double round(double value, int precision) {
		int scale = (int) Math.pow(10, precision);
		return (double) Math.round(value * scale) / scale;
	}

	private Time activeGame;
	private Time activeHeadstart;
	private final Time game;
	private final Time headstart;
	private final Time deathPenalty;
	private final Time pause;
}
