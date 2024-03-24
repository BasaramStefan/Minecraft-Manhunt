package net.bezeram.manhuntmod.game;

import net.bezeram.manhuntmod.networking.ModMessages;
import net.bezeram.manhuntmod.networking.packets.HunterCompassGetPosC2SPacket;

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

    public static class Compass {
        public int targetX = 0;
        public int targetZ = 0;
        public boolean canRequest = false;
    };

    private final Compass compassData = new Compass();
    private final ClientTimer timer = new ClientTimer();

    public Compass getCompassData() {
        return compassData;
    }
}
