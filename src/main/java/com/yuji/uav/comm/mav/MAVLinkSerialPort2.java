package com.yuji.uav.comm.mav;

import com.MAVLink.Messages.MAVLinkMessage;
import com.MAVLink.Messages.MAVLinkPacket;
import com.MAVLink.Parser;
import com.google.common.eventbus.AllowConcurrentEvents;
import com.google.common.eventbus.Subscribe;
import jssc.SerialPort;
import jssc.SerialPortEvent;
import jssc.SerialPortEventListener;
import jssc.SerialPortException;

import java.util.Properties;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * This is a package scope class NOT intended for outside use.
 *
 * A class used to communicate with UAVs via the local serial port using the MAVLink protocol.
 * It is designed to hide the low-level details of MAVLink communication over serial.
 *
 * !!! Uses the google guava event bus !!!
 *
 * @author Philip L. Giacalone
 */
class MAVLinkSerialPort2 {

    private static Logger LOGGER = Logger.getLogger("com.yuji.uav.comm.mav");

    MAVLinkCommunicationBus mavLinkCommunicationBus;

    //the logical name of the device sending/receiving data to/from this serial port (i.e., Pixhawk, UDB, etc)
    private String deviceName;

    private SerialPort jsscSerialPort;

    /**
     * Constructor provided for convenience that creates a new serial port using the given baud rate
     * along with common serial port settings (i.e., 8 data bits, 1 stop bit, parity none)
     *
     * @param portName the name of the serial port. Examples: COM1 (Windows), /dev/ttyUSB0 (Linux)
     * @baudRate the serial port baud rate (e.g., 57600, etc)
     * @param bus
     */
    MAVLinkSerialPort2(String deviceName, String portName, Integer baudRate, MAVLinkCommunicationBus bus)
            throws MAVLinkSerialPortException {
        this(   deviceName,
                portName,
                baudRate,
                SerialPort.DATABITS_8,
                SerialPort.STOPBITS_1,
                SerialPort.PARITY_NONE,
                bus);
    }

    /**
     * Constructor that allows all the serial port settings to be controlled
     * via settings from a Properties object
     *
     * @param settings
     * @param bus
     * @throws MAVLinkSerialPortException
     */
    MAVLinkSerialPort2(SerialPortSettings settings, MAVLinkCommunicationBus bus)
            throws MAVLinkSerialPortException {

        this(   settings.getUniqueDeviceName(),
                settings.getPortName(),
                settings.getBaudRate(),
                settings.getDataBits(),
                settings.getStopBits(),
                settings.getParity(),
                bus  );
    }

    /**
     * Constructor that allows all the serial port settings to be controlled
     * via settings from a Properties object
     *
     * @param props - properties object using keynames defined in SerialPortSettings.java
     * @param bus
     * @throws MAVLinkSerialPortException
     */
    MAVLinkSerialPort2(Properties props, MAVLinkCommunicationBus bus)
            throws MAVLinkSerialPortException {

        this(   props.getProperty(SerialPortSettings.UNIQUE_DEVICE_NAME),
                props.getProperty(SerialPortSettings.PORT_NAME),
                Integer.parseInt(props.getProperty(SerialPortSettings.BAUD_RATE)),
                Integer.parseInt(props.getProperty(SerialPortSettings.DATA_BITS)),
                Integer.parseInt(props.getProperty(SerialPortSettings.STOP_BITS)),
                Integer.parseInt(props.getProperty(SerialPortSettings.PARITY)),
                bus
            );
    }

    /**
     * Constructor that allows all the serial port settings to be individually passed in
     *
     * @param portName the name of the serial port. Examples: COM1 (Windows), /dev/ttyUSB0 (Linux)
     * @param baudRate
     * @param dataBits
     * @param stopBits
     * @param parity - Parity is specified as an integer, as follows: PARITY_NONE=0, PARITY_ODD=1, PARITY_EVEN=2, PARITY_MARK=3, PARITY_SPACE=4
     * @param bus
     */
    MAVLinkSerialPort2(String deviceName, String portName, int baudRate, int dataBits, int stopBits, int parity, MAVLinkCommunicationBus bus)
    throws MAVLinkSerialPortException {

        this.deviceName = deviceName;
        this.jsscSerialPort = new SerialPort(portName);

        try {
            jsscSerialPort.openPort();
            jsscSerialPort.setParams(baudRate, dataBits, stopBits, parity);
            //Preparing a mask. In a mask, we need to specify the types of events that we want to track.
            //Well, for example, we need to know what came some data, thus in the mask must have the
            //following value: MASK_RXCHAR. If we, for example, still need to know about changes in states
            //of lines CTS and DSR, the mask has to look like this: SerialPort.MASK_RXCHAR + SerialPort.MASK_CTS + SerialPort.MASK_DSR
            int mask = SerialPort.MASK_RXCHAR + SerialPort.MASK_CTS + SerialPort.MASK_DSR;//Prepare mask
            boolean success = jsscSerialPort.setEventsMask(mask);//Set mask
            if (!success){
                throw new MAVLinkSerialPortException("Failed to set the serial port events mask");
            }
            //register as a subscriber to the mavLinkCommunicationBus, since this class will forward MAVLink messages to the UAV (via the send(MAVLinkMessage messageToUav) method)
            this.mavLinkCommunicationBus = bus;
            this.mavLinkCommunicationBus.registerSubscriber(this);

            //Add an interface through which we will receive information about events
            this.jsscSerialPort.addEventListener(new SerialPortReader(bus));

        }
        catch (SerialPortException e) {
            throw new MAVLinkSerialPortException(e.getMessage());
        }
    }

    /**
     * Closes this serial port
     * @throws MAVLinkSerialPortException
     */
    public void close() throws MAVLinkSerialPortException {
        try {
            this.jsscSerialPort.closePort();
        } catch (SerialPortException e) {
            throw new MAVLinkSerialPortException(e.getMessage());
        }
    }

    /**
     * This method sends the given MAVLinkMessage to the UAV via this serial port
     * @param messageToUav
     * @throws jssc.SerialPortException
     * @return true if successful, else false
     */
    //GOTCHA: Methods annotated with @Subscribe MUST be public
    @Subscribe  //MAVLinkCommand
    @AllowConcurrentEvents
    public void send(MAVCommandMessage messageToUav) throws MAVLinkSerialPortException {
        try {
            if (messageToUav != null && messageToUav.getMavLinkMessage() != null) {
                MAVLinkPacket packet = messageToUav.getMavLinkMessage().pack();
                byte[] bytes = packet.encodePacket();
                this.jsscSerialPort.writeBytes(bytes);
            }
        } catch (SerialPortException e) {
            throw new MAVLinkSerialPortException(e.getMessage());
        }
    }

    //============================================================

    /**
     * This internal class is an implementation of jssc.SerialPortEventListener.
     * This class is responsible for receiving arriving bytes from the serial port,
     * parsing bytes, creating mavlink messages from those bytes, and forwarding
     * the messages to the portal specified in the constructor.
     */
    class SerialPortReader implements SerialPortEventListener {

        Parser parser = new Parser();
        MAVLinkPacket packet = null;
        MAVLinkMessage message = null;
        MAVLinkCommunicationBus mavLinkCommunicationBus;
        int byteCount;

        /**
         * Constructs this SerialPortReader/SerialPortEventListener object
         * @param bus - the mavLinkCommunicationBus that carries messages to/from the autopilot
         */
        SerialPortReader(MAVLinkCommunicationBus bus) {
            this.mavLinkCommunicationBus = bus;
        }

        /**
         * Implementation of SerialPortEventListener interface
         * @param event
         */
        @Override
        public void serialEvent(SerialPortEvent event) {
            //Object type SerialPortEvent carries information about which event occurred and a value.
            //For example, if the data came a method event.getEventValue() returns us the number of bytes in the input buffer.
            if(event.isRXCHAR()){
                //Read data
                try {
                    int[] intArray = jsscSerialPort.readIntArray(event.getEventValue());
                    int count = intArray.length;

//                    LOGGER.log(Level.FINE, "\nReceived " + count + " bytes.");
//                    LOGGER.log(Level.FINE, event.getEventValue() + " bytes: ");

                    for (int i=0; i<count; i++) {
                        byteCount++;
                        int intValue = intArray[i];

                        try {
                            //==================================
                            // call mavlink to parse the message
                            //==================================
                            packet = parser.mavlink_parse_char(intValue);
                        } catch (RuntimeException e) {
                            LOGGER.log(Level.WARNING, "MAVLinkSerialPort: Error while parsing: " + e);
//                            e.printStackTrace();
                        }

                        if (packet != null) {
                            //we now have a fully parsed packet
                            //so now we'll unpack it to create the correct type of logical MAVLinkMessage
                            try {
                                //===========================
                                // unpack the mavlink message
                                //===========================
                                message = packet.unpack();
                                //debug
                                if (message != null){
                                    LOGGER.log(Level.FINE, "MAVLinkSerialPort: received mavlink msgid = " + message.msgid + ", byte count=" + byteCount);
                                    LOGGER.log(Level.FINE, "MAVLinkSerialPort: mavlink msg = " + message.toString());

                                    //====================================
                                    //send the message to the portal
                                    //====================================
                                    LOGGER.log(Level.INFO, "MAVLink msg from [" + deviceName + " (" + message.sysid + ")]: " + message.getClass().getName());
//                                    this.mavLinkCommunicationBus.postEvent(new DownlinkMessage(message));
                                    this.mavLinkCommunicationBus.postEvent(message);

                                } else {
                                    LOGGER.log(Level.WARNING, "MAVLinkSerialPort: Unpacking mavlink message produced a null result");
                                }
                            } catch (Exception e){
                                if (message != null){
                                    LOGGER.log(Level.WARNING, "MAVLinkSerialPort: Error while unpacking mavlink packet: msgId=" + message.msgid + " : " + e);
                                } else {
                                    LOGGER.log(Level.WARNING, "MAVLinkSerialPort: Error while unpacking mavlink packet: packet=" + packet + " : " + e);
                                }
                            }
                        }

                    } //end for() loop

                }
                catch (SerialPortException ex) {
                    LOGGER.log(Level.WARNING, "MAVLinkSerialPort error: " + ex.toString());
                }
            }
            //If the CTS line status has changed, then the method event.getEventValue() returns 1 if the line is ON and 0 if it is OFF.
            else if(event.isCTS()){
                if(event.getEventValue() == 1){
                    LOGGER.log(Level.FINE, "MAVLinkSerialPort CTS - ON");
                }
                else {
                    LOGGER.log(Level.FINE, "MAVLinkSerialPort CTS - OFF");
                }
            }
            else if(event.isDSR()){
                if(event.getEventValue() == 1){
                    LOGGER.log(Level.FINE, "MAVLinkSerialPort DSR - ON");
                }
                else {
                    LOGGER.log(Level.FINE, "MAVLinkSerialPort DSR - OFF");
                }
            }
        }
    }

}
