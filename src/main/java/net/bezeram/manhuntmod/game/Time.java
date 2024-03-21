package net.bezeram.manhuntmod.game;

public class Time {
	public Time() {}
	private Time(long ticks) { this.ticks = ticks; }

	public static Time TimeSeconds(double seconds) {
		Time time = new Time();
		time.ticks = Timer.secondsToTicks(seconds);
		return time;
	}

	public static Time TimeMinutes(double minutes) {
		Time time = new Time();
		time.ticks = Timer.minutesToTicks(minutes);
		return time;
	}

	public static Time TimeTicks(long ticks) {
		Time time = new Time();
		time.ticks = ticks;
		return time;
	}

	public void setTicks(long ticks) { this.ticks = ticks; }

	public Time clone() {
		return new Time(ticks);
	}

	public void advance() { ticks++; }
	public void advance(Time time) { ticks += time.asTicks(); }
	public void advance(long ticks) { this.ticks += ticks; }

	public double asSeconds() 	{ return Timer.ticksToSeconds(ticks); }
	public double asMinutes() 	{ return Timer.ticksToMinutes(ticks); }
	public long asTicks()		{ return ticks; }

	private long ticks = 0;
}
