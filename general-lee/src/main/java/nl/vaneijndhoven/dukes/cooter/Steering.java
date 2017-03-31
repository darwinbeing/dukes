package nl.vaneijndhoven.dukes.cooter;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static nl.vaneijndhoven.dukes.cooter.Command.servoBlaster;

public class Steering {

    SteeringMap steeringMap;

    private static final int SERVOBLASTER_ID_WHEEL = 2; // GPIO-18

    private static final Logger LOG = LoggerFactory.getLogger(Steering.class);

    public Steering(SteeringMap steeringMap) {
        this.steeringMap = steeringMap;
    }

    public void center() {
        setWheelPosition(steeringMap.center());
    }

    public static void setWheelPosition(int position) {
        if (!Command.powerIsOn()) {
            LOG.debug("Not setting servo value; power is off.");
            return;
        }

        LOG.debug("Setting servo to value " + position);
        servoBlaster(SERVOBLASTER_ID_WHEEL, position);
    }

    public SteeringMap getSteeringMap() {
        return steeringMap;
    }
}
