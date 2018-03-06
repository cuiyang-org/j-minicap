package org.cuiyang.minicap.ddmlib;

import org.cuiyang.minicap.BaseIDeviceTest;
import org.junit.Assert;
import org.junit.Test;

import java.util.Arrays;

/**
 * DdmlibUtilsTest
 *
 * @author cuiyang
 */
public class DdmlibUtilsTest extends BaseIDeviceTest {

    @Test
    public void testGetAbi() throws Exception {
        String abi = DdmlibUtils.getAbi(device);
        Assert.assertTrue(Arrays.asList("arm64-v8a", "armeabi-v7a", "x86", "x86_64").contains(abi));
    }

    @Test
    public void testGetApi() throws Exception {
        int api = DdmlibUtils.getApi(device);
        Assert.assertTrue(api >=7 && api <= 27);
    }

    @Test
    public void testGetPhysicalSize() throws Exception {
        PhysicalSize size = DdmlibUtils.getPhysicalSize(device);
        Assert.assertNotNull(size);
    }
}
