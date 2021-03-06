package com.yuji.uav.comm.mav;

import com.MAVLink.Messages.MAVLinkMessage;
import com.google.common.eventbus.AsyncEventBus;
import com.google.common.eventbus.EventBus;

import java.util.concurrent.Executors;
import java.util.logging.Logger;

/**
 * An messaging bus (with built-in queue) used to communicate with a UAV via the serial port.
 *
 * This class relies on google guava event bus.
 * @see https://code.google.com/p/guava-libraries/wiki/EventBusExplained
 * @see http://insightfullogic.com/blog/2011/oct/10/eventbus/
 *
 * @author Philip L. Giacalone
 */
public class MAVLinkCommunicationBus {

    private static Logger LOGGER = Logger.getLogger("com.yuji.uav.comm.mav");

    //an asynchronous event mavLinkCommunicationBus that has an internal queue
    private EventBus eventBus = null;

    //the serial port used for sending and receiving messages
    private MAVLinkSerialPort2 serialPort;

    /**
     * Constructor
     * @param asyncFlag - controls whether the internal bus will be asynchronous (true) or synchronous (false).
     * @param settings object holding all the serial port settings
     * @throws MAVLinkSerialPortException
     */
    public MAVLinkCommunicationBus(boolean asyncFlag, SerialPortSettings settings) throws MAVLinkSerialPortException {
        if (asyncFlag){
            this.eventBus = new AsyncEventBus(settings.getUniqueDeviceName(), Executors.newCachedThreadPool());
        } else {
            this.eventBus = new EventBus(settings.getUniqueDeviceName());
        }
        this.serialPort = new MAVLinkSerialPort2(settings, this);
    }


    public void registerSubscriber(Object subscriber) {
//        LOGGER.info("bus=" + this.asyncEventBus + ", subscriber=" + subscriber);
        this.eventBus.register(subscriber);
    }

    public void unRegisterSubscriber(Object subscriber) {
        this.eventBus.unregister(subscriber);
    }

    public void postEvent(Object e) {
//        LOGGER.info("bus=" + this.eventBus + ", event=" + e);
        this.eventBus.post(e);
    }

    /**
     * Closes this serial port
     * @throws jssc.SerialPortException
     */
    public void closeSerialPort() throws MAVLinkSerialPortException {
        this.serialPort.close();
    }

}
