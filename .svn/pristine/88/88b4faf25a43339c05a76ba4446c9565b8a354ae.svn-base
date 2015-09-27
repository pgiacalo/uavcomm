package com.yuji.uav.comm.mav;

/**
 * A simple wrapper class that holds a MAVLinkMessage that was received from the remote vehicle
 *
 * @author Philip L. Giacalone
 */
public class MAVTelemetryMessage {

    private com.MAVLink.Messages.MAVLinkMessage mavLinkMessage;

    private static final String PREFIX = "MAVTelemetryMessage: ";

    /**
     * Constructor
     * @param mavLinkMessage
     */
    public MAVTelemetryMessage(com.MAVLink.Messages.MAVLinkMessage mavLinkMessage) {
        this.mavLinkMessage = mavLinkMessage;
    }

    /**
     * Returns the MAVLinkMessage contained in this class
     * @return
     */
    public com.MAVLink.Messages.MAVLinkMessage getMavLinkMessage() {
        return mavLinkMessage;
    }

    public String toString(){
        return PREFIX + this.mavLinkMessage.toString();
    }
}
