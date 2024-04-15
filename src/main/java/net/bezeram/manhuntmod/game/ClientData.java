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

    public void reset(boolean isGameInSession) {
        this.isGameInSession = isGameInSession;
        hunterCompass.reset();
        altRespawnPacket.reset();
    }

    private final HunterCompass hunterCompass = new HunterCompass();
    private final AltRespawnPacket altRespawnPacket = new AltRespawnPacket();
    private final ClientTimer timer = new ClientTimer();
    private boolean isGameInSession = false;

    public static class HunterCompass {
        public void reset() { targetX = 0; targetZ = 0; }

        public int targetX = 0;
        public int targetZ = 0;
    }

    public static class AltRespawnPacket {
        public void reset() { coords = null; respawnPointChangeAcknowledged = false; }

        public BlockPos coords = null;
        public boolean respawnPointChangeAcknowledged = false;
    }
}
