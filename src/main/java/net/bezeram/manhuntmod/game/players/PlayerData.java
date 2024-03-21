package net.bezeram.manhuntmod.game.players;

import net.bezeram.manhuntmod.game.Timer;

public class PlayerData {
    private final PlayerRespawner playerRespawner;

    public PlayerData(final Timer timer) {
        this.playerRespawner = new PlayerRespawner(timer);
    }

    public final PlayerRespawner getPlayerRespawner() {
        return playerRespawner;
    }
}
