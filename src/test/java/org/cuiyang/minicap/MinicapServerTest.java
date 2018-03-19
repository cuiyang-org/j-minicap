package org.cuiyang.minicap;

import org.junit.Test;

/**
 * MinicapServerTest
 *
 * @author cuiyang
 */
public class MinicapServerTest extends BaseIDeviceTest {

    @Test
    public void testRun() throws Exception {
        MinicapServer server = new MinicapServer(device, 1717);
        server.start();
        // noinspection ResultOfMethodCallIgnored
        System.in.read();
    }
}
