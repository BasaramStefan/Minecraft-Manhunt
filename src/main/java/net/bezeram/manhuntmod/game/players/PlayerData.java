package net.bezeram.manhuntmod.game.players;

import net.bezeram.manhuntmod.enums.DimensionID;
import net.bezeram.manhuntmod.game.Game;
import net.bezeram.manhuntmod.game.GameTimer;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.players.PlayerList;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.scores.PlayerTeam;

import java.util.UUID;

public class PlayerData {
    public PlayerData(PlayerTeam teamRunner, PlayerTeam teamHunter,
                      PlayerList playerList, final GameTimer timer, MinecraftServer server) {
        this.list = playerList;
        this.server = server;
        this.teamRunner = teamRunner;
        this.teamHunter = teamHunter;
        this.runnersArray = new ServerPlayer[teamHunter.getPlayers().size()];
        this.huntersArray = new ServerPlayer[teamHunter.getPlayers().size()];
        this.prevCoordsOverworld = new PlayerCoords(this);
        this.prevCoordsNether = new PlayerCoords(this);
        this.prevCoordsEnd = new PlayerCoords(this);

        int indexHunters = 0;
        int indexRunners = 0;
        for (ServerPlayer player : playerList.getPlayers())
            if (isHunter(player))
                huntersArray[indexHunters++] = player;
            else if (isRunner(player))
                runnersArray[indexRunners++] = player;

        this.compassArray = new CompassArray(runnersArray, huntersArray, server);
        for (int i : compassArray.getRunnersIDs()) {
            ServerPlayer player = compassArray.getPlayer(i);
            PlayerCoords playerCoords = getCoords(player.getLevel().dimension());

            playerCoords.update(player.getUUID(), player.getPosition(1));
            System.out.println(player.getName().getString() + " UUID: " + player.getStringUUID() + "\n");
        }
        for (int i : compassArray.getHuntersIDs()) {
            ServerPlayer player = compassArray.getPlayer(i);
            PlayerCoords playerCoords = getCoords(player.getLevel().dimension());

            playerCoords.update(player.getUUID(), player.getPosition(1));
            System.out.println(player.getName().getString() + " UUID: " + player.getStringUUID() + "\n");
        }

        this.playerRespawner = new PlayerRespawner(timer);
    }

    public void updateCoords() {
        for (ServerPlayer player : list.getPlayers()) {
            PlayerCoords coords = getCoords(player.getLevel().dimension());
            coords.update(player.getUUID(), player.getPosition(1));
        }
    }

    public boolean isHunter(Player player) {
        if (player.getTeam() == null)
            return false;

        String playerName = player.getName().getString();
        for (String hunter : teamHunter.getPlayers()) {
            if (hunter.contains(playerName)) {
                return true;
            }
        }

        return false;
    }

    public boolean isRunner(Player player) {
        if (player.getTeam() == null)
            return false;

        String playerName = player.getName().getString();
        for (String runner : teamRunner.getPlayers()) {
            if (runner.contains(playerName)) {
                return true;
            }
        }

        return false;
    }

    public boolean isManhuntPlayer(Player player) {
        return isRunner(player) || isHunter(player);
    }

    public PlayerTeam getTeamRunner() {
        return teamRunner;
    }

    public PlayerTeam getTeamHunter() {
        return teamHunter;
    }

    public ServerPlayer[] getRunners() {
        return runnersArray;
    }

    public ServerPlayer[] getHunters() {
        return huntersArray;
    }

    public void updateAllCoords() {
        for (ServerPlayer player : getList().getPlayers()) {
            ServerLevel level = player.getLevel();
            PlayerCoords coords = getCoords(level.dimension());

            if (coords != null)
                coords.update(player.getUUID(), player.getPosition(0));
        }
    }

    public void update(final UUID uuid) {
        if (!Game.inSession()) {
            System.out.println("PlayerData::update() - Game not in session\n");
            return;
        }

        ServerPlayer player = Game.get().getPlayer(uuid);
        PlayerCoords coords = getCoords(player.getLevel().dimension());
        if (coords != null)
            coords.update(uuid, player.getPosition(0));
    }

    public final PlayerCoords getCoords(final DimensionID dimensionID) {
        return switch (dimensionID) {
            case OVERWORLD -> prevCoordsOverworld;
            case NETHER -> prevCoordsNether;
            case END -> prevCoordsEnd;
            default -> null;
        };
    }
    public final PlayerCoords getCoords(final ResourceKey<Level> dimension) {
        return switch (Game.getDimensionID(dimension)) {
            case OVERWORLD -> prevCoordsOverworld;
            case NETHER -> prevCoordsNether;
            case END -> prevCoordsEnd;
            default -> null;
        };
    }
    public final Vec3 getCoords(final ServerPlayer serverPlayer) {
        PlayerCoords coords = getCoords(serverPlayer.getLevel().dimension());
        return coords.get(serverPlayer.getUUID());
    }
    public final PlayerCoords getPrevCoordsOverworld() { return prevCoordsOverworld; }
    public final PlayerCoords getPrevCoordsNether() { return prevCoordsNether; }
    public final PlayerCoords getPrevCoordsEnd() { return prevCoordsEnd; }

    public final PlayerRespawner getPlayerRespawner() { return playerRespawner;}
    public final CompassArray getPlayerArray() { return compassArray; }
    public final ServerPlayer getPlayer(int MAID) {
        return compassArray.getPlayer(MAID);
    }

    public final PlayerList getList() { return list; }
    public final ServerPlayer[] getPlayers() {
        ServerPlayer[] players = new ServerPlayer[runnersArray.length + huntersArray.length];
        System.arraycopy(runnersArray, 0, players, 0, runnersArray.length);
        System.arraycopy(huntersArray, 0, players, runnersArray.length, huntersArray.length);

        return players;
    }

    private final PlayerRespawner playerRespawner;

    private final CompassArray compassArray;
    private final PlayerList list;

    private final PlayerTeam teamRunner;
    private final PlayerTeam teamHunter;

    private final ServerPlayer[] runnersArray;
    private final ServerPlayer[] huntersArray;

    private final PlayerCoords prevCoordsOverworld;
    private final PlayerCoords prevCoordsNether;
    private final PlayerCoords prevCoordsEnd;

    private final MinecraftServer server;
}
