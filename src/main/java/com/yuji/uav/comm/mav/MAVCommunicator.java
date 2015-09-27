package com.yuji.uav.comm.mav;

/**
 * @author Philip L. Giacalone
 */
public interface MAVCommunicator {

    /**
     * This method is used to received messages from the UAV.
     * @param messageFromUav
     */
    public void receive(MAVTelemetryMessage messageFromUav);

    /**
     * This method is used to send MAVLinkMessages to the UAV
     * @param messageToUav
     * @throws MAVLinkSerialPortException if there is a problem with the connection
     */
    public void send(MAVCommandMessage messageToUav) throws MAVLinkSerialPortException;

}
