package net.bezeram.manhuntmod.game.players;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;

public class PlayerArray {
    public PlayerArray(ServerPlayer[] runners, ServerPlayer[] hunters) {
        int runnersCount = runners.length;
        int huntersCount = hunters.length;
        this.runnerCount = runnersCount;
        this.playerArray = new ServerPlayer[runnersCount + huntersCount];
        this.prevRunnerIndex = 0;
        this.prevHunterIndex = runnersCount;

        int indexPlayers = 0;
        for (ServerPlayer runner : runners) {
            playerArray[indexPlayers] = runner;
            indexPlayers++;
        }

        for (ServerPlayer hunter : hunters) {
            playerArray[indexPlayers] = hunter;
            indexPlayers++;
        }
    }

    public int cycleRunners(int ID) {
        ID = (ID + 1) % runnerCount;
        return ID;
    }

    public int cycleHunters(int ID) {
        if (ID < runnerCount)
            return runnerCount;
        return (ID + 1 - runnerCount) % getHunterCount() + runnerCount;
    }

    public boolean samePlayer(Player player, int ID2) {
        return player.getUUID() == playerArray[ID2].getUUID();
    }

    public int getRunnerCount() { return runnerCount; }
    public int getHunterCount() { return playerArray.length - runnerCount; }
    public int getPlayerCount() { return playerArray.length; }
    public int getFirstRunnerID() { return 0; }
    public int getFirstHunterID() { return runnerCount; }

    public ServerPlayer getPlayer(int index) { return playerArray[index]; }
    public ServerPlayer getFirstRunner() { return playerArray[0]; }
    public ServerPlayer getFirstHunter() { return playerArray[runnerCount]; }

    public int getIDByName(String playerName) {
        for (int i = 0; i < playerArray.length; i++)
            if (playerArray[i].getName().getString().equals(playerName))
                return i;
        return -1;
    }

    public boolean isRunner(int ID) { return ID >= 0 && ID < runnerCount; }
    public boolean isHunter(int ID) { return ID >= runnerCount && ID < playerArray.length; }

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

    private final ServerPlayer[] playerArray;
    private final int runnerCount;

    private int prevRunnerIndex;
    private int prevHunterIndex;
}
