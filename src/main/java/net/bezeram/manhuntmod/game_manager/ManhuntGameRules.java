package net.bezeram.manhuntmod.game_manager;

public class ManhuntGameRules {
	public enum DeathPenaltyType {
		FALSE, TRUE, TRUE_EXCEPT_END
	}

	public enum PotionBanType {
		DISABLED, ALL, EXTENDED_ONLY, AMPLIFIED_ONLY, EXTENDED_OR_AMPLIFIED
	}

	public static void resetDefaults() {
		LIMITED_RESPAWNS = DEFAULT_LIMITED_RESPAWNS;
		TIME_LIMIT = DEFAULT_TIME_LIMIT;
		SAVE_INVENTORIES = DEFAULT_SAVE_INVENTORIES;
		HEADSTART = DEFAULT_HEADSTART;
		DISABLE_RESPAWN_BLOCK_EXPLOSION = DEFAULT_DISABLE_RESPAWN_BLOCK_EXPLOSION;
		BAN_ENCHANTMENTS.removeAll();
		BAN_ENCHANTMENTS.toggleDefaultOptions(true);
		BAN_POTIONS = DEFAULT_BAN_POTIONS;
		DEATH_PENALTY = DEFAULT_DEATH_PENALTY;
		BAN_END_CRYSTALS = DEFAULT_BAN_END_CRYSTALS;
		CAN_BREAK_SPAWNERS = DEFAULT_CAN_BREAK_SPAWNERS;
	}

	public static boolean LIMITED_RESPAWNS = true;
	public static boolean TIME_LIMIT = true;
	public static boolean SAVE_INVENTORIES = true;
	public static boolean HEADSTART = true;
	public static boolean DISABLE_RESPAWN_BLOCK_EXPLOSION = true;
	public final static EnchantmentBanType BAN_ENCHANTMENTS = new EnchantmentBanType();
	public static PotionBanType BAN_POTIONS = PotionBanType.EXTENDED_OR_AMPLIFIED;
	public static DeathPenaltyType DEATH_PENALTY = DeathPenaltyType.TRUE_EXCEPT_END;
	public static boolean BAN_END_CRYSTALS = false;
	public static boolean CAN_BREAK_SPAWNERS = false;

	public static boolean DEFAULT_LIMITED_RESPAWNS = true;
	public static boolean DEFAULT_TIME_LIMIT = true;
	public static boolean DEFAULT_SAVE_INVENTORIES = true;
	public static boolean DEFAULT_HEADSTART = true;
	public static boolean DEFAULT_DISABLE_RESPAWN_BLOCK_EXPLOSION = true;
	public static PotionBanType DEFAULT_BAN_POTIONS = PotionBanType.EXTENDED_OR_AMPLIFIED;
	public static DeathPenaltyType DEFAULT_DEATH_PENALTY = DeathPenaltyType.TRUE_EXCEPT_END;
	public static boolean DEFAULT_BAN_END_CRYSTALS = false;
	public static boolean DEFAULT_CAN_BREAK_SPAWNERS = false;
}
