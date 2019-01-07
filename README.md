# uavcomm
A project to simplify MAVLink serial communication.

The design concept of this package is to view an external application communicating with a UAV as an entity that can both send commands to, and receive telemetry from, the UAV via MAVLink messages. As such, all communication to/from the UAV is done via a "Portal" class that implements the MAVCommunicator interface. The interface is shown below.

public interface MAVCommunicator {

    /**
     * This method is used to received messages (i.e., telemetry) from the UAV.
     * It is called automatically on each MAVLink message arrival.
     * @param messageFromUav
     */
    public void receive(MAVTelemetryMessage messageFromUav);

    /**
     * This method is used to send MAVLinkMessages (i.e., commands) to the UAV
     * @param messageToUav
     * @throws MAVLinkSerialPortException if there is a problem with the connection
     */
    public void send(MAVCommandMessage messageToUav) throws MAVLinkSerialPortException;
}

Code examples and the properties file setup (for the serial port) are shown in these files:
- MAVLinkPortal.java
- MAVLinkPortalTest.java
- MAV_1_SerialPort.properties

This code has been tested with the following 2 UAV autopilots running MAVLink.
- Pixhawk 2
- UAVDev board (UDB)
