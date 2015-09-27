package com.yuji.uav.comm;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.*;

public class MyMAVLinkClientTest {

    private MyMAVLinkClient client1;
    private MyMAVLinkClient client2;

    @Before
    public void setUp() throws Exception {
        this.client1 = new MyMAVLinkClient("MAV_1_SerialPort.properties");
        this.client2 = new MyMAVLinkClient("MAV_2_SerialPort.properties");
    }

    @After
    public void tearDown() throws Exception {

    }

    @Test
    public void testSend() throws Exception {

    }

    @Test
    public void testReceive() throws Exception {

    }
}