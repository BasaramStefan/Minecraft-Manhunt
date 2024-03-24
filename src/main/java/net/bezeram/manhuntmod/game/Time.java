package net.bezeram.manhuntmod.game;

public class Time {
	public Time() {}
	private Time(long ticks) { this.ticks = ticks; }

	public static Time TimeSeconds(double seconds) {
		Time time = new Time();
		time.ticks = Time.secondsToTicks(seconds);
		return time;
	}

	public static Time TimeMinutes(double minutes) {
		Time time = new Time();
		time.ticks = Time.minutesToTicks(minutes);
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

	public static double round(double value, int precision) {
		int scale = (int) Math.pow(10, precision);
		return (double) Math.round(value * scale) / scale;
	}

	public void advance() { ticks++; }
	public void advance(Time time) { ticks += time.asTicks(); }
	public void advance(long ticks) { this.ticks += ticks; }

	public double asSeconds() 	{ return Time.ticksToSeconds(ticks); }
	public double asMinutes() 	{ return Time.ticksToMinutes(ticks); }
	public long asTicks()		{ return ticks; }

	public static long minutesToTicks(double minutes) 	{ return (long)(minutes * 2400.f); }
	public static long secondsToTicks(double seconds) 	{ return (long)(seconds * 40.f); }
	public static double ticksToSeconds(long ticks) 	{ return ((double)ticks) / 40.f; }
	public static double ticksToMinutes(long ticks) 	{ return ((double)ticks) / 2400.f; }

	private long ticks = 0;
}
