package net.bezeram.manhuntmod.game;

import net.minecraft.core.BlockPos;

/**
 * This class is only instantiated for the clients and contains data to be shared between
 * different parts of code.
 */
public class ClientData {

    private static ClientData INSTANCE = null;
    private ClientData() {}

    public static ClientData get() {
        if (INSTANCE == null)
            INSTANCE = new ClientData();
        return INSTANCE;
    }

    public void update() {
        timer.update();
    }

    public HunterCompass getHunterCompass() {
        return hunterCompass;
    }

    public BlockPos getPortalRespawnCoords() { return altRespawnPacket.coords; }
    public void setPortalRespawnCoords(final BlockPos coords) { altRespawnPacket.coords = coords; }
    public boolean isRespawnPointChangeAcknowledged() { return altRespawnPacket.respawnPointChangeAcknowledged; }
    public void respawnPointAcknowledged() { altRespawnPacket.respawnPointChangeAcknowledged = true; }
    public void respawnPointAcknowledgeReset() { altRespawnPacket.respawnPointChangeAcknowledged = false; }

    public boolean isGameInSession() { return isGameInSession; }
    public void setGameSession(boolean session) { isGameInSession = session; }

    public boolean isEndLocked() { return isEndLocked; }
    public void setEndLocked(boolean value) { isEndLocked = value; }

    public void reset(boolean isGameInSession) {
        this.isGameInSession = isGameInSession;
        hunterCompass.reset();
        altRespawnPacket.reset();
    }

    private final HunterCompass hunterCompass = new HunterCompass();
    private final PortalRespawnPacket altRespawnPacket = new PortalRespawnPacket();
    private final ClientTimer timer = new ClientTimer();
    private boolean isGameInSession = false;
    private boolean isEndLocked = false;

    public static class HunterCompass {
        public void reset() { targetX = Integer.MAX_VALUE; targetZ = Integer.MAX_VALUE; }

        public int targetX = 0;
        public int targetZ = 0;

        @Override
        public String toString() {
            return targetX + " " + targetZ;
        }
    }

    public static class PortalRespawnPacket {
        public void reset() { coords = null; respawnPointChangeAcknowledged = false; }

        public BlockPos coords = null;
        public boolean respawnPointChangeAcknowledged = false;

        @Override
        public String toString() {
            return coords + " Portal Respawn " + (respawnPointChangeAcknowledged ? "Acknowledged" : "NOT Acknowledged");
        }
    }
}
