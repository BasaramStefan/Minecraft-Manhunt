package net.bezeram.manhuntmod.game.players;

import net.bezeram.manhuntmod.game.Timer;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.players.PlayerList;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.scores.PlayerTeam;
import net.minecraftforge.event.TickEvent;

import java.util.Hashtable;

public class PlayerData {
    private final PlayerRespawner playerRespawner;
    private final PlayerArray playerArray;
    private final PlayerList playerList;

    private final PlayerTeam teamRunner;
    private final PlayerTeam teamHunter;

    private final ServerPlayer[] listRunners;
    private final ServerPlayer[] listHunters;

    private final Hashtable<String, Vec3> huntersStartCoords = new Hashtable<>();
    private final Hashtable<String, Vec3> runnersStartCoords = new Hashtable<>();
    private final Hashtable<String, Vec3> playersPrevCoords = new Hashtable<>();

    public PlayerData(PlayerTeam teamRunner, PlayerTeam teamHunter,
                      PlayerList playerList, final Timer timer) {
        this.teamRunner = teamRunner;
        this.teamHunter = teamHunter;
        this.listRunners = new ServerPlayer[teamHunter.getPlayers().size()];
        this.listHunters = new ServerPlayer[teamHunter.getPlayers().size()];
        int indexHunters = 0;
        int indexRunners = 0;
        for (ServerPlayer player : playerList.getPlayers()) {
            if (isHunter(player)) {
                huntersStartCoords.put(player.getName().getString(), player.getPosition(1));
                listHunters[indexHunters++] = player;
            }
            else if (isRunner(player)) {
                runnersStartCoords.put(player.getName().getString(), player.getPosition(1));
                listRunners[indexRunners++] = player;
            }
        }

        this.playerRespawner = new PlayerRespawner(timer);
        this.playerArray = new PlayerArray(listRunners, listHunters);
        this.playerList = playerList;
    }

    public final PlayerRespawner getPlayerRespawner() { return playerRespawner;}
    public final PlayerArray getPlayerArray() { return playerArray; }
    public final PlayerList getPlayerList() { return playerList; }

    public final Hashtable<String, Vec3> getHuntersStartCoords() { return huntersStartCoords; }
    public final Hashtable<String, Vec3> getRunnersStartCoords() { return runnersStartCoords; }
    public final Hashtable<String, Vec3> getPlayersPrevCoords() { return playersPrevCoords; }

    public void updatePlayersPrevPosition() {
        for (ServerPlayer player : playerList.getPlayers())
            playersPrevCoords.put(player.getName().getString(), player.getPosition(1));
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

    public boolean isInGame(Player player) {
        return isRunner(player) || isHunter(player);
    }

    public PlayerTeam getTeamRunner() {
        return teamRunner;
    }

    public PlayerTeam getTeamHunter() {
        return teamHunter;
    }

    public ServerPlayer[] getListRunners() {
        return listRunners;
    }

    public ServerPlayer[] getListHunters() {
        return listHunters;
    }

    public enum PlayerLastLocations {
        Overworld, Nether, End;

        public void update(String playerName, Vec3 newPosition) {
            lastPlayerPosition.put(playerName, newPosition);
        }

        public static void updateAll(TickEvent.ServerTickEvent event) {
            PlayerList allPlayers = event.getServer().getPlayerList();
            for (ServerPlayer player : allPlayers.getPlayers()) {
                ServerLevel level = player.getLevel();
                String name = player.getName().getString();
                Vec3 newPosition = player.getPosition(0);

                PlayerLastLocations location = getByDimension(level.dimension());

                if (location != null)
                    location.update(name, newPosition);
            }
        }

        public static PlayerLastLocations getByDimension(ResourceKey<Level> dimension) {
            if (dimension == Level.OVERWORLD)
                return PlayerLastLocations.Overworld;
            else if (dimension == Level.NETHER)
                return PlayerLastLocations.Nether;
            else if (dimension == Level.END)
                return PlayerLastLocations.End;
            return null;
        }

        public Vec3 getLastPosition(String playerName) {
            return lastPlayerPosition.get(playerName);
        }

        private final Hashtable<String, Vec3> lastPlayerPosition = new Hashtable<>();
    }
}
