package net.bezeram.manhuntmod.game;

public class ClientTimer {

    private static final Time COMPASS_REQUEST = Time.TimeTicks(3);

    public void update() {
        updateCompassRequest();
    }

    private void updateCompassRequest() {
        compass.advance();

        if (compass.asTicks() > COMPASS_REQUEST.asTicks()) {
            ClientData.Compass compassData = ClientData.get().getCompassData();
            compassData.canRequest = !compassData.canRequest;
            compass.setTicks(0);
        }
    }

    public Time getCompassTimer() { return compass; }

    private final Time compass = new Time();
}
