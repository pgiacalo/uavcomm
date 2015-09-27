package com.yuji.uav.comm.mav;

/**
 * Exception type thrown whenever there is a problem with serial port communication.
 *
 * This class is just a wrapper that avoids exposing the internal details of the serial port implementation
 *
 * @author Philip L. Giacalone
 */
public class MAVLinkSerialPortException extends Exception {

    public MAVLinkSerialPortException(String msg){
        super(msg);
    }
}
