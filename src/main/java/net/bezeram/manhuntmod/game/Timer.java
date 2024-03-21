package net.bezeram.manhuntmod.game;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.players.PlayerList;
import net.minecraftforge.event.TickEvent;

public class Timer {
	private static Time RUNNER_LIMIT 			= Time.TimeMinutes(90);
	private static Time RUNNER_START 			= Time.TimeSeconds(10);
	private static Time HEADSTART 				= Time.TimeSeconds(30);
	private static Time DEATH_PENALTY 			= Time.TimeMinutes(5);
	private static Time PAUSE 					= Time.TimeMinutes(10);
	private static Time RESUME 					= Time.TimeSeconds(5);

	public void updatePlayerPosition()  { activePlayerPosition.advance(); }
	public void updateActive() 		    { activeGame.advance(); }
	public void updateStart() 	        { activeStart.advance(); }
	public void updateHeadstart() 	    { activeHeadstart.advance(); }
	public void updateResume() 	        { activeResume.advance(); }
	public void applyDeathPenalty() {
		// If the current game time is below the death penalty,
		// it will not be updated as to show how much time was left on scoreboard
		if (game.asTicks() - activeGame.asTicks() < deathPenalty.asTicks()) {
			Game.get().hunterHasWon();
			return;
		}

		activeGame.advance(deathPenalty);
	}

	public void updateHeadstartHints(TickEvent.ServerTickEvent event)   { displayHeadstartHint(event); }
	public void updateStartHints(TickEvent.ServerTickEvent event)       { displayStartHint(event); }
	public void updateResumeHints(TickEvent.ServerTickEvent event)      { displayResumeHint(event); }
	public void resetPlayerPositionTime() { activePlayerPosition.setTicks(0); }
	public void resetResumeHints() {
		activeResume.setTicks(0);
		prevActiveResume.setTicks(0);
	}

	public boolean activeTimeHasEnded()		{ return activeGame.asTicks() 		>= game.asTicks(); }
	public boolean huntersHaveStarted()		{ return activeHeadstart.asTicks() 	>= headstart.asTicks(); }
	public boolean runnersHaveStarted()     { return activeStart.asTicks()      >= start.asTicks(); }
	public boolean gameResumed()            { return activeResume.asTicks()     >= resume.asTicks(); }

	public Time getSessionGame()            { return game; }
	public Time getSessionStart()           { return start; }
	public Time getSessionHeadstart()       { return headstart; }
	public Time getSessionDeathPenalty()    { return deathPenalty; }
	public Time getSessionPause()           { return pause; }
	public Time getSessionResume()          { return resume; }

	public static void setGameTime(double minutes) 		{ RUNNER_LIMIT 		= Time.TimeMinutes(minutes); }
	public static void setHeadstart(double seconds) 	{ HEADSTART 		= Time.TimeSeconds(seconds); }
	public static void setDeathPenalty(double minutes) 	{ DEATH_PENALTY 	= Time.TimeMinutes(minutes); }
	public static void setPauseTime(double minutes) 	{ PAUSE 			= Time.TimeMinutes(minutes); }

	public static Time getGameTime() 		{ return RUNNER_LIMIT;  }
	public static Time getStartTime() 		{ return RUNNER_START;  }
	public static Time getHeadstart() 		{ return HEADSTART;  	}
	public static Time getDeathPenalty() 	{ return DEATH_PENALTY; }
	public static Time getPauseTime() 		{ return PAUSE; 	 	}
	public static Time getResumeTime() 		{ return RESUME; 	 	}

	public static long minutesToTicks(double minutes) 	{ return (long)(minutes * 2400.f); }
	public static long secondsToTicks(double seconds) 	{ return (long)(seconds * 40.f); }
	public static double ticksToSeconds(long ticks) 	{ return ((double)ticks) / 40.f; }
	public static double ticksToMinutes(long ticks) 	{ return ((double)ticks) / 2400.f; }

	public Time getGameElapsed()        { return activeGame; }
	public Time getPlayerPositionElapsed() { return activePlayerPosition; }

	public void displayHeadstartHint(TickEvent.ServerTickEvent event) {
		PlayerList playerList = event.getServer().getPlayerList();
		double seconds = headstart.asSeconds() - activeHeadstart.asSeconds();
		seconds = round(seconds, 1);
		playerList.broadcastSystemMessage(Component
				.literal(String.valueOf(seconds)).withStyle(ChatFormatting.GREEN), true);
	}

	private void displayStartHint(TickEvent.ServerTickEvent event) {
		PlayerList playerList = event.getServer().getPlayerList();
		if (activeStart.asSeconds() - prevActiveStart.asSeconds() > 1) {
			int seconds = (int) Math.ceil(start.asSeconds() - activeStart.asSeconds());
			playerList.broadcastSystemMessage(Component
					.literal(String.valueOf(seconds)).withStyle(ChatFormatting.GREEN), false);

			prevActiveStart = activeStart.clone();
		}
	}

	private void displayResumeHint(TickEvent.ServerTickEvent event) {
		if (activeResume.asSeconds() - prevActiveResume.asSeconds() > 1) {
			PlayerList playerList = event.getServer().getPlayerList();
			int seconds = (int) Math.ceil(resume.asSeconds() - activeResume.asSeconds());
			playerList.broadcastSystemMessage(Component
					.literal(String.valueOf(seconds)).withStyle(ChatFormatting.GREEN), false);

			prevActiveResume = activeResume.clone();
		}
	}

	private static double round(double value, int precision) {
		int scale = (int) Math.pow(10, precision);
		return (double) Math.round(value * scale) / scale;
	}

	private Time activePlayerPosition = new Time();

	private Time activeGame = new Time();
	private Time activeStart = new Time();
	private Time prevActiveStart = new Time();
	private Time activeHeadstart = new Time();
	private Time activeResume = new Time();
	private Time prevActiveResume = new Time();

	private final Time game = RUNNER_LIMIT;
	private final Time start = RUNNER_START;
	private final Time headstart = HEADSTART;
	private final Time deathPenalty = DEATH_PENALTY;
	private final Time pause = PAUSE;
	private final Time resume = RESUME;
}
