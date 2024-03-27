package net.bezeram.manhuntmod.game.players;

import net.bezeram.manhuntmod.game.Game;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;

import java.util.UUID;

public class CompassArray {
    public CompassArray(UUID[] runners, UUID[] hunters, final MinecraftServer server) {
        int runnersCount = runners.length;
        int huntersCount = hunters.length;
        this.runnersCount = runnersCount;
        this.playerArray = new UUID[runnersCount + huntersCount];
        this.prevRunnerIndex = 0;
        this.prevHunterIndex = runnersCount;
        this.server = server;

        int indexPlayers = 0;
        for (UUID runnerUUID : runners) {
            playerArray[indexPlayers] = runnerUUID;
            indexPlayers++;
        }

        for (UUID hunterUUID : hunters) {
            playerArray[indexPlayers] = hunterUUID;
            indexPlayers++;
        }
    }

    public int cycleRunners(int ID) {
        ID = (ID + 1) % runnersCount;
        return ID;
    }

    public int cycleHunters(int ID) {
        if (ID < runnersCount)
            return runnersCount;
        return (ID + 1 - runnersCount) % getHunterCount() + runnersCount;
    }

    public boolean samePlayer(final ServerPlayer player, int ID) {
        return player.getUUID().equals(playerArray[ID]);
    }
    public boolean samePlayer(final UUID uuid, int ID) {
        return uuid.equals(playerArray[ID]);
    }

    public int getRunnersCount() { return runnersCount; }
    public int getHunterCount() { return playerArray.length - runnersCount; }
    public int getPlayerCount() { return playerArray.length; }
    public int getFirstRunnerID() { return 0; }
    public int getFirstHunterID() { return runnersCount; }

    public final ServerPlayer getPlayer(int MAID) {
        if (!Game.inSession()) {
            System.out.println("CompassArray::getPlayer() - Game not in session\n");
            return null;
        }

        if (MAID < 0 || MAID > getPlayerCount() || !Game.inSession())
            return null;

        return server.getPlayerList().getPlayer(playerArray[MAID]);
    }

    public final UUID getPlayerUUID(int index) { return playerArray[index]; }
    public final UUID getFirstRunner() { return playerArray[0]; }
    public final UUID getFirstHunter() { return playerArray[runnersCount]; }

    public int getMAIDByUUID(final UUID uuid) {
        for (int i = 0; i < playerArray.length; i++)
            if (playerArray[i].equals(uuid))
                return i;
        return -1;
    }

    public boolean isRunner(int ID) { return ID >= 0 && ID < runnersCount; }
    public boolean isHunter(int ID) { return ID >= runnersCount && ID < playerArray.length; }

    public void setPrevHunterID(int index) {
        prevHunterIndex = index;
    }

    public void setPrevRunnerID(int index) {
        prevRunnerIndex = index;
    }

    public int getPrevHunterID() {
        return prevHunterIndex;
    }

    public int getPrevRunnerID() {
        return prevRunnerIndex;
    }

    private final UUID[] playerArray;
    private final int runnersCount;

    private int prevRunnerIndex;
    private int prevHunterIndex;

    private MinecraftServer server;

    public int[] getRunnersIDs() {
        int[] ids = new int[runnersCount];

        for (int i = 0; i < runnersCount; i++)
            ids[i] = i;
        return ids;
    }

    public int[] getHuntersIDs() {
        int[] ids = new int[getHunterCount()];

        for (int i = runnersCount, j = 0; i < playerArray.length; i++, j++)
            ids[j] = i;
        return ids;
    }
}
