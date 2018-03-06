package org.cuiyang.minicap.ddmlib;

import com.android.ddmlib.CollectingOutputReceiver;
import com.android.ddmlib.IDevice;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.StringUtils;

import java.io.File;
import java.io.InputStream;

/**
 * ddmlib utils
 *
 * @author cuiyang
 */
public class DdmlibUtils {

    /**
     * 执行命令
     * @param device IDevice
     * @param command 命令
     * @return 命令执行结果
     */
    public static String command(IDevice device, String command) throws Exception {
        CollectingOutputReceiver abiReceiver = new CollectingOutputReceiver();
        device.executeShellCommand(command, abiReceiver);
        return StringUtils.trim(abiReceiver.getOutput());
    }

    /**
     * 获取cpu/abi (arm64-v8a armeabi-v7a x86 x86_64)
     * @param device IDevice
     * @return abi
     */
    public static String getAbi(IDevice device) throws Exception {
        return command(device, "getprop ro.product.cpu.abi");
    }

    /**
     * 获取sdk api
     * @param device IDevice
     * @return sdk api
     */
    public static int getApi(IDevice device) throws Exception {
        return Integer.parseInt(command(device, "getprop ro.build.version.sdk"));
    }

    /**
     * 获取屏幕尺寸
     * @param device IDevice
     * @return PhysicalSize
     */
    public static PhysicalSize getPhysicalSize(IDevice device) throws Exception {
        String ret = command(device, "wm size");
        String[] px = StringUtils.substringAfter(ret, "Physical size: ").split("x");
        return new PhysicalSize(Integer.parseInt(px[0]), Integer.parseInt(px[1]));
    }

    /**
     * 修改执行权限
     * @param device IDevice
     * @param mode 文件权限
     * @param path file path
     * @return result
     */
    public static String chmod(IDevice device, String path, String mode) throws Exception {
        return command(device, String.format("chmod %s %s", mode, path));
    }

    /**
     * 将文件推送到设备
     * @param device IDevice
     * @param local 本地文件路径
     * @param remote 远程文件路径
     * @param mode 文件权限
     */
    public static void pushFile(IDevice device, String local, String remote, String mode) throws Exception {
        device.pushFile(local, remote);
        String ret = chmod(device, remote, mode);
        if (StringUtils.isNotEmpty(ret)) {
            throw new DdmlibException(ret);
        }
    }

    /**
     * 将文件推送到设备
     * @param device IDevice
     * @param in 输入流
     * @param remote 远程文件路径
     * @param mode 文件权限
     */
    public static void pushFile(IDevice device, InputStream in, String remote, String mode) throws Exception {
        File local = File.createTempFile("minicap_", ".tmp");
        try {
            FileUtils.copyInputStreamToFile(in, local);
            pushFile(device, local.getAbsolutePath(), remote, mode);
        } finally {
            local.deleteOnExit();
        }
    }
}
