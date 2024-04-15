package net.bezeram.manhuntmod.game.players;

import net.bezeram.manhuntmod.enums.DimensionID;
import net.bezeram.manhuntmod.game.Game;
import net.bezeram.manhuntmod.game.GameTimer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.GlobalPos;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.players.PlayerList;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.scores.PlayerTeam;

import java.util.ArrayList;
import java.util.Hashtable;
import java.util.List;
import java.util.UUID;

public class PlayerData {
    public PlayerData(PlayerTeam teamRunner, PlayerTeam teamHunter,
                      PlayerList playerList, final GameTimer timer, MinecraftServer server) {
        this.list = playerList;
        this.server = server;
        this.teamRunner = teamRunner;
        this.teamHunter = teamHunter;
        this.runnersArray = new UUID[teamRunner.getPlayers().size()];
        this.huntersArray = new UUID[teamHunter.getPlayers().size()];
        this.prevCoordsOverworld = new PlayerCoords(this);
        this.prevCoordsNether = new PlayerCoords(this);
        this.prevCoordsEnd = new PlayerCoords(this);
        this.portalRespawnCoords.add(new Hashtable<>());
        this.portalRespawnCoords.add(new Hashtable<>());

        int indexHunters = 0;
        int indexRunners = 0;
        for (ServerPlayer player : playerList.getPlayers()) {
            if (isHunter(player))
                huntersArray[indexHunters++] = Game.cloneUUID(player.getUUID());
            else if (isRunner(player))
                runnersArray[indexRunners++] = Game.cloneUUID(player.getUUID());
        }
        Game.LOG("PlayerData: created player arrays");

        this.compassArray = new CompassArray(runnersArray, huntersArray, server);
        for (UUID runnerUUID : runnersArray) {
            ServerPlayer runner = server.getPlayerList().getPlayer(runnerUUID);
            PlayerCoords playerCoords = getCoords(runner.getLevel().dimension());

            playerCoords.update(runner.getUUID(), runner.getPosition(1));
        }
        Game.LOG("PlayerData: created player coords runners");
        for (UUID hunterUUID : huntersArray) {
            ServerPlayer hunter = server.getPlayerList().getPlayer(hunterUUID);
            PlayerCoords playerCoords = getCoords(hunter.getLevel().dimension());

            playerCoords.update(hunter.getUUID(), hunter.getPosition(1));
        }
        Game.LOG("PlayerData: created player coords hunters");

        this.playerRespawner = new PlayerRespawner(timer);
        this.playersArray = new UUID[runnersArray.length + huntersArray.length];
        System.arraycopy(runnersArray, 0, playersArray, 0, runnersArray.length);
        System.arraycopy(huntersArray, 0, playersArray, runnersArray.length, huntersArray.length);
        Game.LOG("Constructed PlayerData");
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

    public boolean isManhuntPlayer(final Player player) {
        return isRunner(player) || isHunter(player);
    }

    public PlayerTeam getTeamRunner() {
        return teamRunner;
    }

    public PlayerTeam getTeamHunter() {
        return teamHunter;
    }

    public ServerPlayer[] getRunners() {
        ServerPlayer[] runners = new ServerPlayer[runnersArray.length];
        int i = 0;
        for (UUID uuid : runnersArray)
            runners[i++] = Game.get().getPlayer(uuid);

        return runners;
    }

    public ServerPlayer[] getHunters() {
        ServerPlayer[] hunters = new ServerPlayer[huntersArray.length];
        int i = 0;
        for (UUID uuid : huntersArray)
            hunters[i++] = Game.get().getPlayer(uuid);

        return hunters;
    }

    public void updateAllCoords() {
        for (ServerPlayer player : getPlayers()) {
            try {
                ServerLevel level = player.getLevel();
                PlayerCoords coords = getCoords(level.dimension());

                if (coords != null)
                    coords.update(player.getUUID(), player.getPosition(0));
            } catch (Exception ignored) {}
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
    public final BlockPos getPortalRespawnCoords(final UUID uuid) {
        try {
            ServerPlayer player = Game.get().getPlayer(uuid);
            DimensionID dimension = Game.getDimensionID(player.getLevel().dimension());

            if (!(portalRespawnCoords.get(dimension.index).containsKey(uuid))) {
                Game.LOG("Attempted to access Portal Respawn Coords for player: " + uuid.toString());
                return null;
            }
            if (uuid == null) {
                Game.LOG("Attempted to access Portal Respawn Coords with null uuid");
                return null;
            }

            return portalRespawnCoords.get(dimension.index).get(uuid);
        }
        catch (Exception e) {
            Game.LOG("Exception" + e + " caught in PlayerData::getPlayerRespawnCoords");
        }

        return null;
    }
    public void setRespawnBuffer(final UUID uuid, final GlobalPos portalPos) {
        if (portalPos == null || uuid == null) {
            Game.LOG("Attempted to change Portal Respawn Buffer with null for player "
                    + ((uuid != null) ? uuid : "  null"));
            return;
        }

        respawnBuffer.put(uuid, portalPos);
    }
    public GlobalPos getRespawnBuffer(final UUID uuid) {
        if (!respawnBuffer.containsKey(uuid)) {
            Game.LOG("Attempted to access Current Portal Respawn Buffer for player: " + uuid.toString());
            return null;
        }
        if (uuid == null) {
            Game.LOG("Attempted to access Current Portal Respawn Buffer with null uuid");
            return null;
        }

        return respawnBuffer.get(uuid);
    }

    public final PlayerRespawner getPlayerRespawner() { return playerRespawner;}
    public final CompassArray getPlayerArray() { return compassArray; }
    public final ServerPlayer getPlayer(int MAID) {
        return compassArray.getPlayer(MAID);
    }

    public void setUsedPortalRespawn(boolean active) { usedPortalRespawn = active; }
    public boolean hasUsedPortalRespawn() { return usedPortalRespawn; }

    public final PlayerList getList() { return list; }
    public final ServerPlayer[] getPlayers() {
        ServerPlayer[] players = new ServerPlayer[playersArray.length];
        int i = 0;
        for (UUID uuid : playersArray)
            players[i++] = Game.get().getPlayer(uuid);
        return players;
    }

    public void updatePortal(final UUID uuid, final BlockPos portalCoords) {
        try {
            if (portalCoords == null || uuid == null) {
                Game.LOG("Attempted to change Portal Respawn with null for player "
                    + ((uuid != null) ? uuid : "  null"));
                return;
            }

            ServerPlayer player = Game.get().getPlayer(uuid);
            DimensionID dimension = Game.getDimensionID(player.getLevel().dimension());
            portalRespawnCoords.get(dimension.index).put(uuid, portalCoords);
        }
        catch (Exception e) {
            Game.LOG("Exception " + e + " caught in PlayerData::updatePortal");
        }
    }

    private final PlayerRespawner playerRespawner;

    private final CompassArray compassArray;
    private final PlayerList list;

    private final PlayerTeam teamRunner;
    private final PlayerTeam teamHunter;

    private final UUID[] runnersArray;
    private final UUID[] huntersArray;
    private final UUID[] playersArray;

    private final PlayerCoords prevCoordsOverworld;
    private final PlayerCoords prevCoordsNether;
    private final PlayerCoords prevCoordsEnd;

    // 0:Overworld, 1:Nether
    private final List<Hashtable<UUID, BlockPos>> portalRespawnCoords = new ArrayList<>(2);
    private final Hashtable<UUID, GlobalPos> respawnBuffer = new Hashtable<>();
    private boolean usedPortalRespawn = false;

    private final MinecraftServer server;
}
