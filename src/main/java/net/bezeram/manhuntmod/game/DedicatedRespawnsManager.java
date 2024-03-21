package net.bezeram.manhuntmod.game;

import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.dimension.BuiltinDimensionTypes;

// Every player has a dedicated respawn manager
public class DedicatedRespawnsManager {
	public static int DEFAULT_SET_RESPAWNS_OVERWORLD = 5;
	public static int DEFAULT_SET_RESPAWNS_NETHER = 5;
	public static int DEFAULT_SET_RESPAWNS_END = Integer.MAX_VALUE;

	public boolean canRespawnAtSetOverworld() {
		return respawnCountOverworld >= 0;
	}
	public boolean canRespawnAtSetNether() {
		return respawnCountNether >= 0;
	}
	public boolean canRespawnAtSetEnd() { return respawnCountEnd >= 0; }
	public boolean canRespawnDedicated(Player player) {
		Level level = player.getLevel();
		if (level.dimensionTypeRegistration().is(BuiltinDimensionTypes.OVERWORLD))
			return canRespawnAtSetOverworld();

		if (level.dimensionTypeRegistration().is(BuiltinDimensionTypes.NETHER))
			return canRespawnAtSetNether();

		if (level.dimensionTypeRegistration().is(BuiltinDimensionTypes.END))
			return canRespawnAtSetEnd();

		return false;
	}

	public DedicatedRespawnsManager diedInOverworld() {
		if (respawnCountOverworld >= 0) {
			respawnCountOverworld--;
		}

		return this;
	}

	public DedicatedRespawnsManager diedInNether() {
		if (respawnCountNether >= 0) {
			respawnCountNether--;
		}

		return this;
	}

	public DedicatedRespawnsManager diedInEnd() {
		if (respawnCountEnd >= 0) {
			respawnCountEnd--;
		}

		return this;
	}

	private int respawnCountOverworld = DEFAULT_SET_RESPAWNS_OVERWORLD;
	private int respawnCountNether = DEFAULT_SET_RESPAWNS_NETHER;
	private int respawnCountEnd = DEFAULT_SET_RESPAWNS_END;
}
