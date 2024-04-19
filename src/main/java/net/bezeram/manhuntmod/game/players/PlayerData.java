package net.bezeram.manhuntmod.game.players;

import net.bezeram.manhuntmod.enums.DimensionID;
import net.bezeram.manhuntmod.game.Game;
import net.bezeram.manhuntmod.game.GameTimer;
import net.bezeram.manhuntmod.networking.ModMessages;
import net.bezeram.manhuntmod.networking.packets.UpdateGameStateS2CPacket;
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
        this.portalNetherRespawnPositions.add(new Hashtable<>());
        this.portalNetherRespawnPositions.add(new Hashtable<>());

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
            PlayerCoords playerCoords = getLastPosition(runner.getLevel().dimension());

            playerCoords.update(runner.getUUID(), runner.getPosition(1));
        }
        Game.LOG("PlayerData: created player coords runners");
        for (UUID hunterUUID : huntersArray) {
            ServerPlayer hunter = server.getPlayerList().getPlayer(hunterUUID);
            PlayerCoords playerCoords = getLastPosition(hunter.getLevel().dimension());

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
            PlayerCoords coords = getLastPosition(player.getLevel().dimension());
            coords.update(player.getUUID(), player.getPosition(1));
        }
    }

    public boolean isHunter(final Player player) {
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

    public boolean isRunner(final Player player) {
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
                PlayerCoords coords = getLastPosition(level.dimension());

                if (coords != null)
                    coords.update(player.getUUID(), player.getPosition(1));
            } catch (Exception ignored) {}
        }
    }

    public void update(final UUID uuid) {
        if (!Game.inSession()) {
            System.out.println("PlayerData::update() - Game not in session\n");
            return;
        }

        ServerPlayer player = Game.get().getPlayer(uuid);
        PlayerCoords coords = getLastPosition(player.getLevel().dimension());
        if (coords != null)
            coords.update(uuid, player.getPosition(0));
    }

    public final PlayerCoords getLastPosition(final DimensionID dimensionID) {
        return switch (dimensionID) {
            case OVERWORLD -> prevCoordsOverworld;
            case NETHER -> prevCoordsNether;
            case END -> prevCoordsEnd;
            default -> null;
        };
    }
    public final PlayerCoords getLastPosition(final ResourceKey<Level> dimension) {
        return switch (Game.getDimensionID(dimension)) {
            case OVERWORLD -> prevCoordsOverworld;
            case NETHER -> prevCoordsNether;
            case END -> prevCoordsEnd;
            default -> null;
        };
    }
    public final Vec3 getLastPosition(final DimensionID dimensionID, final ServerPlayer player) {
        try {
            PlayerCoords playerCoords = getLastPosition(dimensionID);

            if (playerCoords != null)
                return playerCoords.get(player.getUUID());
            return null;
        } catch (Exception ignored) {}
        return null;
    }

    /**
     * Returns the last position of the player in the dimension they are currently.
     * @param player Target player
     * @return Player's position
     */
    public final Vec3 getLastPosition(final ServerPlayer player) {
        try {
            PlayerCoords coords = getLastPosition(player.getLevel().dimension());
            if (coords != null)
                return coords.get(player.getUUID());
            return null;
        } catch (Exception ignored) {
            return null;
        }
    }
    public final BlockPos getPortalRespawnCoords(final UUID uuid) {
        try {
            ServerPlayer player = Game.get().getPlayer(uuid);
            DimensionID dimension = Game.getDimensionID(player.getLevel().dimension());

            if (!(portalNetherRespawnPositions.get(dimension.index).containsKey(uuid))) {
                Game.LOG("Attempted to access Portal Respawn Coords for player: " + uuid.toString());
                return null;
            }
            if (uuid == null) {
                Game.LOG("Attempted to access Portal Respawn Coords with null uuid");
                return null;
            }

            return portalNetherRespawnPositions.get(dimension.index).get(uuid);
        }
        catch (Exception e) {
            Game.LOG("Exception" + e + " caught in PlayerData::getPlayerRespawnCoords");
        }

        return null;
    }

    public final BlockPos getEndRespawnPosition(final UUID uuid) {
        if (!(endRespawnPositions.containsKey(uuid))) {
            Game.LOG("Attempted to access End Respawn Position for player: " + Game.get().getPlayer(uuid).getName().getString());
            return null;
        }
        if (uuid == null) {
            Game.LOG("Attempted to access End Respawn Position with null UUID");
            return null;
        }

        return endRespawnPositions.get(uuid);
    }

    public void saveEndRespawnPosition(final UUID uuid, final BlockPos respawnPos) {
        try {
            if (respawnPos == null || uuid == null) {
                Game.LOG("Attempted to lock End Respawn with "
                        + ((respawnPos != null) ? respawnPos : "null")
                        + " for player " + ((uuid != null) ? Game.get().getPlayer(uuid).getName().getString() : " null"));
                return;
            }

            endRespawnPositions.put(uuid, respawnPos);
            ModMessages.sendToPlayer(new UpdateGameStateS2CPacket(Game.inSession(), true), Game.get().getPlayer(uuid));
            Game.LOG("Locked End Respawn for player " + Game.get().getPlayer(uuid).getName().getString() +
                    "at" + respawnPos + " successfully");
        } catch (Exception ignored) {}
    }

    public boolean isEndLocked(final UUID uuid) { return endRespawnPositions.containsKey(uuid); }

    public void setRespawnBuffer(final UUID uuid, final GlobalPos portalPos) {
        if (portalPos == null || uuid == null) {
            Game.LOG("Attempted to change Portal Respawn Buffer with null for player "
                    + ((uuid != null) ? uuid : "  null"));
            return;
        }

        netherRespawnBuffer.put(uuid, portalPos);
    }
    public GlobalPos getRespawnBuffer(final UUID uuid) {
        if (!netherRespawnBuffer.containsKey(uuid)) {
            Game.LOG("Attempted to access Current Portal Respawn Buffer for player: " + uuid.toString());
            return null;
        }
        if (uuid == null) {
            Game.LOG("Attempted to access Current Portal Respawn Buffer with null uuid");
            return null;
        }

        return netherRespawnBuffer.get(uuid);
    }

    public final PlayerRespawner getPlayerRespawner() { return playerRespawner;}
    public final CompassArray getPlayerArray() { return compassArray; }
    public final ServerPlayer getPlayer(int MAID) {
        return compassArray.getPlayer(MAID);
    }

    public void setUsedPortalRespawn(boolean active) { usedPortalRespawn = active; }
    public boolean hasUsedPortalRespawn() { return usedPortalRespawn; }

    public final ServerPlayer[] getPlayers() {
        ServerPlayer[] players = new ServerPlayer[playersArray.length];
        int i = 0;
        for (UUID uuid : playersArray)
            players[i++] = Game.get().getPlayer(uuid);
        return players;
    }

    public void updateNetherPortalPosition(final UUID uuid, final BlockPos portalPos) {
        try {
            if (portalPos == null || uuid == null) {
                Game.LOG("Attempted to change Portal Respawn with null for player "
                    + ((uuid != null) ? uuid : "  null"));
                return;
            }

            ServerPlayer player = Game.get().getPlayer(uuid);
            DimensionID dimension = Game.getDimensionID(player.getLevel().dimension());
            portalNetherRespawnPositions.get(dimension.index).put(uuid, portalPos);
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
    private final List<Hashtable<UUID, BlockPos>> portalNetherRespawnPositions = new ArrayList<>(2);
    private final Hashtable<UUID, BlockPos> endRespawnPositions = new Hashtable<>();
    private final Hashtable<UUID, GlobalPos> netherRespawnBuffer = new Hashtable<>();
    private boolean usedPortalRespawn = false;

    private final MinecraftServer server;
}
