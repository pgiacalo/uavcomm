package com.yuji.uav.comm;

import com.MAVLink.Messages.MAVLinkMessage;
import com.MAVLink.Messages.ardupilotmega.msg_ahrs;
import com.MAVLink.Messages.ardupilotmega.msg_attitude;
import com.MAVLink.Messages.ardupilotmega.msg_global_position_int;
import com.MAVLink.Messages.ardupilotmega.msg_sensor_offsets;

import com.google.common.eventbus.AllowConcurrentEvents;
import com.google.common.eventbus.Subscribe;

import com.yuji.uav.comm.mav.*;
import com.yuji.uav.comm.mav.MAVLinkSerialPortException;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;
import java.util.logging.Logger;


/**
 * A MAVLink client that communicates with the UAV
 *
 * @author Philip L. Giacalone
 */
public class MyMAVLinkClient implements MAVCommunicator {

    private String name;

    private static Logger LOGGER = Logger.getLogger("com.yuji.uav.comm");

    private MAVLinkCommunicationBus bus;

    private static final String DEFAULT_SERIAL_PORT_PROPERTIES_FILE = "SerialPort.properties";

    /**
     * Constructor - uses the settings from the SerialPort.properties file to create this client
     *
     * @throws MAVLinkSerialPortException
     * @throws IOException
     */
    public MyMAVLinkClient() throws MAVLinkSerialPortException, IOException {
        this(DEFAULT_SERIAL_PORT_PROPERTIES_FILE);
    }

    /**
     * Constructor
     *
     * @param serialPortPropertiesFileName
     * @throws MAVLinkSerialPortException, IOException
     */
    public MyMAVLinkClient(String serialPortPropertiesFileName)
        throws MAVLinkSerialPortException, IOException {

        SerialPortSettings serialPortSettings = new SerialPortSettings(serialPortPropertiesFileName);
        this.name = serialPortSettings.getUniqueDeviceName();
        boolean asynchronous = true;
        this.bus = new MAVLinkCommunicationBus(asynchronous, serialPortSettings);
        this.bus.registerSubscriber(this);

    }

    /**
     * Constructor
     *
     * @param name - the name given to this client (it should be unique)
     * @param bus
     */
    public MyMAVLinkClient(String name, MAVLinkCommunicationBus bus) {
        this.name = name;
        this.bus = bus;
        this.bus.registerSubscriber(this);
    }

    /**
     * This method sends the given message to the remote vehicle
     *
     * @param messageToUAV the message to be sent to the remote vehicle
     */
    @Override   //MAVCommunicator
    public void send(MAVCommandMessage messageToUAV){
        LOGGER.info(this.name + " MyMAVLinkClient.send(): " + messageToUAV.toString());
        //the following call sends the message to the UAV (via the bus)
        this.bus.postEvent(messageToUAV);
    }

    /**
     * This method is called whenever a message is received from the remote vehicle
     *
     * @param messageFromUAV the message received from the remote vehicle
     */
    @Override   //MAVCommunicator
    @Subscribe  //MAVTelemetryMessage
    @AllowConcurrentEvents
    public void receive(MAVTelemetryMessage messageFromUAV) {
        LOGGER.info(this.name + " MyMAVLinkClient.receive(): " + messageFromUAV);
        processMessage(messageFromUAV);
    }

    /**
     * This method is called whenever a message is received from the remote vehicle
     *
     * @param messageFromUAV the message received from the remote vehicle
     */
    @Subscribe  //msg_attitude
    @AllowConcurrentEvents
    public void receiveAttitude(com.MAVLink.Messages.ardupilotmega.msg_attitude messageFromUAV) {
        LOGGER.info(this.name + " MyMAVLinkClient.receiveAttitude(): " + messageFromUAV);
//        processMessage(messageFromUAV);
    }

    /**
     * This method is called whenever a message is received from the remote vehicle
     *
     * @param messageFromUAV the message received from the remote vehicle
     */
    @Subscribe  //msg_global_position_int
    @AllowConcurrentEvents
    public void receivePosition(com.MAVLink.Messages.ardupilotmega.msg_global_position_int messageFromUAV) {
        LOGGER.info(this.name + " MyMAVLinkClient.receivePosition(): " + messageFromUAV);
//        processMessage(messageFromUAV);
    }

    private void processMessage(MAVTelemetryMessage messageFromUAV) {
        if (messageFromUAV != null && messageFromUAV.getMavLinkMessage() != null) {

            MAVLinkMessage mavLinkMessage = messageFromUAV.getMavLinkMessage();

            //now do something with the messageFromUAV
            switch (mavLinkMessage.msgid) {
                case msg_attitude.MAVLINK_MSG_ID_ATTITUDE:
                    msg_attitude attitudeMessage = (msg_attitude) mavLinkMessage;
                    //do something with this type of message
                    break;
                case msg_global_position_int.MAVLINK_MSG_ID_GLOBAL_POSITION_INT:
                    msg_global_position_int positionMessage = (msg_global_position_int) mavLinkMessage;
                    //do something with this type of message
                    break;
                case msg_ahrs.MAVLINK_MSG_ID_AHRS:  //#163
                    msg_ahrs ahrs = (msg_ahrs)mavLinkMessage;
                    //do something with the arriving message
                    break;
                case msg_sensor_offsets.MAVLINK_MSG_ID_SENSOR_OFFSETS:  //#150
                    //do something with the arriving message
                    break;
//                case etc:
//                    //do something with the arriving message
//                    break;
                default:
                    //if we reach here then a message type is not being handled
                    LOGGER.warning("MyMAVLinkClient.receive(UNHANDLED): " + mavLinkMessage);
                    break;
            }
        }
    }

    public String getName() {
        return this.name;
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


    /** For testing this class */
    public static void main(String[] args) throws Exception {
//        MyMAVLinkClient client = new MyMAVLinkClient();
        MyMAVLinkClient client1 = new MyMAVLinkClient("MAV_1_SerialPort.properties");
        MyMAVLinkClient client2 = new MyMAVLinkClient("MAV_2_SerialPort.properties");
//        MyMAVLinkClient client = new MyMAVLinkClient("SerialPort.properties");
        //sleep for 5 seconds to allow MAVLink messages to arrive from the UAV
        Thread.sleep(5000);
        client1.bus.closeSerialPort();
        client2.bus.closeSerialPort();
        System.out.println("MyMAVLinkClient DONE");
        System.exit(0);
    }
}
