package org.cuiyang.minicap;

import com.android.ddmlib.AndroidDebugBridge;
import com.android.ddmlib.IDevice;
import org.junit.Before;

/**
 * BaseIDeviceTest
 *
 * @author cuiyang
 */
public abstract class BaseIDeviceTest {
    protected IDevice device;

    @Before
    public void before() {
        AndroidDebugBridge.init(false);
        AndroidDebugBridge bridge = AndroidDebugBridge.createBridge("D:\\program\\Android\\Sdk\\platform-tools\\adb", false);
        waitForDevice(bridge);
        IDevice[] devices = bridge.getDevices();
        device = devices[0];
    }

    private void waitForDevice(AndroidDebugBridge bridge) {
        int count = 0;
        while (!bridge.hasInitialDeviceList()) {
            try {
                Thread.sleep(100);
                count++;
            } catch (InterruptedException ignored) {
            }
            if (count > 300) {
                System.err.print("Time out");
                break;
            }
        }
    }
}
