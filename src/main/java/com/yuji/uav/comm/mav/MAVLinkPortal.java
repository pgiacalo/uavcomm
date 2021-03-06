package com.yuji.uav.comm.mav;

import com.MAVLink.Messages.MAVLinkMessage;
import com.MAVLink.Messages.ardupilotmega.msg_attitude;
import com.MAVLink.Messages.ardupilotmega.msg_global_position_int;

import java.io.IOException;
import java.io.InputStream;

import java.util.Properties;

/**
 * This class is an implementation of the MAVCommunicator interface.
 *
 * @author Philip L. Giacalone
 */
public class MAVLinkPortal implements MAVCommunicator {

    //===== members =====
    private MAVLinkSerialPort mavlinkSerialPort;

    //===== constants =====
    private final static String PROPERTIES_FILE_NAME = "SerialPort.properties";

    /**
     * Constructor
     * Constructs this object using the serial port settings defined
     * in the SerialPort.properties file
     * @throws MAVLinkSerialPortException
     * @throws IOException
     */
    public MAVLinkPortal()
            throws MAVLinkSerialPortException, IOException {
        this(PROPERTIES_FILE_NAME);
    }

    /**
     * Constructor
     * Constructs this object using the serial port settings
     * defined in the given properties file
     * @param propertiesFileName
     * @throws MAVLinkSerialPortException
     * @throws IOException
     */
    public MAVLinkPortal(String propertiesFileName)
            throws MAVLinkSerialPortException, IOException {
        Properties props = this.readPropertiesFile(propertiesFileName);

        String portName = props.getProperty(SerialPortSettings.PORT_NAME);
        int baudRate = Integer.parseInt(props.getProperty(SerialPortSettings.BAUD_RATE));
        int dataBits = Integer.parseInt(props.getProperty(SerialPortSettings.DATA_BITS));
        int stopBits = Integer.parseInt(props.getProperty(SerialPortSettings.STOP_BITS));
        int parity = Integer.parseInt(props.getProperty(SerialPortSettings.PARITY));

        this.mavlinkSerialPort = new MAVLinkSerialPort(portName, baudRate, dataBits, stopBits, parity, this);
    }

    /**
     * Constructor
     * Constructs this object using the given serial port settings
     * @param portName
     * @param baudRate
     * @param dataBits
     * @param stopBits
     * @param parity
     */
    public MAVLinkPortal(String portName, int baudRate, int dataBits, int stopBits, int parity)
    throws MAVLinkSerialPortException {
        this.mavlinkSerialPort = new MAVLinkSerialPort(portName, baudRate, dataBits, stopBits, parity, this);
    }

    /**
     * Implementation of MAVCommunicator
     * @param messageFromUav
     */
    @Override
    public void receive(MAVTelemetryMessage messageFromUav) {
        if (messageFromUav != null && messageFromUav.getMavLinkMessage() != null) {
            MAVLinkMessage mavLinkMessage = messageFromUav.getMavLinkMessage();

            switch (mavLinkMessage.msgid) {
                case msg_attitude.MAVLINK_MSG_ID_ATTITUDE:
                    msg_attitude attitudeMessage = (msg_attitude) mavLinkMessage;
                    System.out.println("Attitude: " + attitudeMessage.toString());
                    break;
                case msg_global_position_int.MAVLINK_MSG_ID_GLOBAL_POSITION_INT:
                    msg_global_position_int positionMessage = (msg_global_position_int) mavLinkMessage;
                    System.out.println("Position: " + positionMessage.toString());
                    break;
                default:
                    //do nothing
                    break;
            }
        }
    }

    /**
     * Implementation of MAVCommunicator
     * @param messageToUav
     */
    @Override
    public void send(MAVCommandMessage messageToUav) throws MAVLinkSerialPortException {
        this.mavlinkSerialPort.send(messageToUav);
    }

    /**
     * Helper method to read and return the values from a properties file
     * @return a Properties object populated from the give file
     * @throws IOException
     */
    private Properties readPropertiesFile(String fileName) throws IOException {
        Properties props = new Properties();
        InputStream is = ClassLoader.getSystemResourceAsStream(fileName);
        props.load(is);
        return props;
    }

}
