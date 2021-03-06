package com.yuji.uav.comm.mav;

import com.MAVLink.Messages.MAVLinkMessage;
import com.MAVLink.Messages.MAVLinkPacket;
import com.MAVLink.Parser;

import jssc.SerialPort;
import jssc.SerialPortEvent;
import jssc.SerialPortEventListener;
import jssc.SerialPortException;

import java.util.Properties;
import java.util.logging.Logger;
import java.util.logging.Level;

/**
 * Package scope class. This class is NOT intended for outside use.
 *
 * A class used to communicate with UAVs via the local serial port using the MAVLink protocol.
 * It is designed to hide the low-level details of MAVLink communication over serial.
 *
 * @author Philip L. Giacalone
 */
class MAVLinkSerialPort {

    private static Logger LOGGER = Logger.getLogger("com.yuji.uav.comm.mav");

    private SerialPort serialPort;


    /**
     * Constructor
     * @param settings
     * @param portal
     * @throws MAVLinkSerialPortException
     */
    MAVLinkSerialPort(SerialPortSettings settings, MAVCommunicator portal)
            throws MAVLinkSerialPortException {
        this.setup(settings.getPortName(), settings.getBaudRate(), settings.getDataBits(), settings.getStopBits(), settings.getParity(), portal);
    }

    /**
     * Constructor provided for convenience that creates a new serial port using the given baud rate
     * along with common serial port settings (i.e., 8 data bits, 1 stop bit, parity none)
     *
     * @param portName the name of the serial port. Examples: COM1 (Windows), /dev/ttyUSB0 (Linux)
     * @baudRate the serial port baud rate (e.g., 57600, etc)
     * @param portal
     */
    MAVLinkSerialPort(String portName, Integer baudRate, MAVCommunicator portal)
            throws MAVLinkSerialPortException {
        this.setup(portName,
                baudRate,
                jssc.SerialPort.DATABITS_8,
                jssc.SerialPort.STOPBITS_1,
                jssc.SerialPort.PARITY_NONE,
                portal);
    }

    /**
     * Constructor that allows all the serial port settings to be controlled
     * via settings from a Properties object
     *
     * @param props - properties object using keynames defined in SerialPortSettings.java
     * @param portal
     * @throws MAVLinkSerialPortException
     */
    MAVLinkSerialPort(Properties props, MAVCommunicator portal)
            throws MAVLinkSerialPortException {

        this.setup(props.getProperty(SerialPortSettings.PORT_NAME),
                Integer.parseInt(props.getProperty(SerialPortSettings.BAUD_RATE)),
                Integer.parseInt(props.getProperty(SerialPortSettings.DATA_BITS)),
                Integer.parseInt(props.getProperty(SerialPortSettings.STOP_BITS)),
                Integer.parseInt(props.getProperty(SerialPortSettings.PARITY)),
                portal
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
     * @param portal
     */
    MAVLinkSerialPort(String portName, int baudRate, int dataBits, int stopBits, int parity, MAVCommunicator portal)
            throws MAVLinkSerialPortException {
        setup(portName, baudRate, dataBits, stopBits, parity, portal);
    }

    private void setup(String portName, int baudRate, int dataBits, int stopBits, int parity, MAVCommunicator portal) throws MAVLinkSerialPortException {
        this.serialPort = new SerialPort(portName);

        try {
            serialPort.openPort();
            serialPort.setParams(baudRate, dataBits, stopBits, parity);
            //Preparing a mask. In a mask, we need to specify the types of events that we want to track.
            //Well, for example, we need to know what came some data, thus in the mask must have the
            //following value: MASK_RXCHAR. If we, for example, still need to know about changes in states
            //of lines CTS and DSR, the mask has to look like this: SerialPort.MASK_RXCHAR + SerialPort.MASK_CTS + SerialPort.MASK_DSR
            int mask = SerialPort.MASK_RXCHAR + SerialPort.MASK_CTS + SerialPort.MASK_DSR;//Prepare mask
            boolean success = serialPort.setEventsMask(mask);//Set mask
            if (!success){
                throw new MAVLinkSerialPortException("Failed to set the serial port events mask");
            }
            //Add an interface through which we will receive information about events
            serialPort.addEventListener(new SerialPortReader(portal));
        }
        catch (SerialPortException e) {
            throw new MAVLinkSerialPortException(e.getMessage());
        }
    }

    /**
     * This method sends the given MAVLinkMessage to the UAV via this serial port
     * @param messageToUav
     * @throws SerialPortException
     * @return true if successful, else false
     */
    void send(MAVCommandMessage messageToUav) throws MAVLinkSerialPortException {
        try {
            if (messageToUav != null && messageToUav.getMavLinkMessage() != null) {
                MAVLinkPacket packet = messageToUav.getMavLinkMessage().pack();
                byte[] bytes = packet.encodePacket();
                this.serialPort.writeBytes(bytes);
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
        MAVCommunicator portal;
        int byteCount;

        /**
         * Constructs this SerialPortReader/SerialPortEventListener object
         * @param portal - the object to be called back whenever a mavlink message arrives
         */
        SerialPortReader(MAVCommunicator portal) {
            this.portal = portal;
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
                    int[] intArray = serialPort.readIntArray(event.getEventValue());
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
                                    this.portal.receive(new MAVTelemetryMessage(message));

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
