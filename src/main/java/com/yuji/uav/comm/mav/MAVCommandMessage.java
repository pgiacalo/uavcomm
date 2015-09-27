package com.yuji.uav.comm.mav;

import com.MAVLink.Messages.MAVLinkMessage;

/**
 * A simple wrapper class that holds a MAVLinkMessage to be sent to the remote vehicle
 *
 * @author Philip L. Giacalone
 */
public class MAVCommandMessage //implements Serializable
{
    private com.MAVLink.Messages.MAVLinkMessage mavLinkMessage;

    private static final String PREFIX = "MAVCommandMessage: ";

    /**
     * Constructor
     * @param mavLinkMessage
     */
    public MAVCommandMessage(com.MAVLink.Messages.MAVLinkMessage mavLinkMessage) {
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
