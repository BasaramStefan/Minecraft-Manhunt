package net.bezeram.manhuntmod.game;

public class ManhuntGameRules {
	public enum DeathPenaltyType {
		FALSE, TRUE, TRUE_EXCEPT_END
	}

	public enum PotionBanType {
		DISABLED, ALL, EXTENDED_ONLY, AMPLIFIED_ONLY, EXTENDED_OR_AMPLIFIED
	}

	public static boolean keepPartialInventory() {
		return ManhuntGameRules.SAVE_INVENTORIES.keep;
	}

	public static boolean keepInventoryEnd() {
		return ManhuntGameRules.SAVE_INVENTORIES.keepAllEnd;
	}

	public static boolean isTimeLimit() {
		return ManhuntGameRules.TIME_LIMIT;
	}

	public static DeathPenaltyType getDeathPenalty() { return ManhuntGameRules.DEATH_PENALTY; }

	public static boolean canBreakSpawners() { return ManhuntGameRules.CAN_BREAK_SPAWNERS; }

	public enum SaveInventoryType {
		FALSE(false, false),
		TRUE(true, false),
		TRUE_KEEP_END(true, true);

		SaveInventoryType(final boolean canSave, final boolean keepAllEnd) {
			this.keep = canSave;
			this.keepAllEnd = keepAllEnd;
		}

		public final boolean keep;
		public final boolean keepAllEnd; // All items are kept when player dies in the end dimension
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

	public static final boolean             DEFAULT_LIMITED_RESPAWNS                    = true;
	public static final boolean             DEFAULT_TIME_LIMIT                          = true;
	public static final SaveInventoryType   DEFAULT_SAVE_INVENTORIES                    = SaveInventoryType.TRUE_KEEP_END;
	public static final boolean             DEFAULT_HEADSTART                           = true;
	public static final boolean             DEFAULT_DISABLE_RESPAWN_BLOCK_EXPLOSION     = true;
	public static final PotionBanType       DEFAULT_BAN_POTIONS                         = PotionBanType.EXTENDED_OR_AMPLIFIED;
	public static final DeathPenaltyType    DEFAULT_DEATH_PENALTY                       = DeathPenaltyType.TRUE_EXCEPT_END;
	public static final boolean             DEFAULT_BAN_END_CRYSTALS                    = false;
	public static final boolean             DEFAULT_CAN_BREAK_SPAWNERS                  = false;

	public static boolean                   LIMITED_RESPAWNS                = DEFAULT_LIMITED_RESPAWNS;
	public static boolean                   TIME_LIMIT                      = DEFAULT_TIME_LIMIT;
	public static SaveInventoryType         SAVE_INVENTORIES                = DEFAULT_SAVE_INVENTORIES;
	public static boolean                   HEADSTART                       = DEFAULT_HEADSTART;
	public static boolean                   DISABLE_RESPAWN_BLOCK_EXPLOSION = DEFAULT_DISABLE_RESPAWN_BLOCK_EXPLOSION;
	public final static EnchantmentBanType  BAN_ENCHANTMENTS                = new EnchantmentBanType();
	public static PotionBanType             BAN_POTIONS                     = DEFAULT_BAN_POTIONS;
	public static DeathPenaltyType          DEATH_PENALTY                   = DEFAULT_DEATH_PENALTY;
	public static boolean                   BAN_END_CRYSTALS                = DEFAULT_BAN_END_CRYSTALS;
	public static boolean                   CAN_BREAK_SPAWNERS              = DEFAULT_CAN_BREAK_SPAWNERS;
}
