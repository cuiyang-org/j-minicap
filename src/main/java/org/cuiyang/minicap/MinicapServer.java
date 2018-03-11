package org.cuiyang.minicap;

import com.android.ddmlib.IDevice;
import com.android.ddmlib.IShellOutputReceiver;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.cuiyang.minicap.ddmlib.DdmlibUtils;
import org.cuiyang.minicap.ddmlib.PhysicalSize;

import java.io.Closeable;
import java.util.concurrent.TimeUnit;

import static org.cuiyang.minicap.util.ResourceUtils.getResourceAsStream;

/**
 * Minicap server
 *
 * @author cuiyang
 */
@Slf4j
public class MinicapServer extends Thread implements Closeable {

    /** android 临时文件存放目录 */
    private static final String ANDROID_TMP_DIR = "/data/local/tmp/";
    /** minicap 临时存放目录 */
    private static final String MINICAP_TMP_DIR = ANDROID_TMP_DIR + "minicap";
    /** minicap-nopie 临时存放目录 */
    private static final String MINICAP_NOPIE_TMP_DIR = ANDROID_TMP_DIR + "minicap-nopie";
    /** minicap.so 临时存放目录 */
    private static final String MINICAP_SO_TMP_DIR = ANDROID_TMP_DIR + "minicap.so";

    /** cpu abi */
    private String abi;
    /** sdk api */
    private int api;
    /** 屏幕尺寸 */
    private PhysicalSize size;
    /** 运行Minicap的设备 */
    private IDevice device;
    /** 端口 */
    private int port = 1717;
    /** 缩放 */
    private float zoom = 1;
    /** 旋转 0|90|180|270 */
    private int rotate = 0;
    /** -Q <value>: JPEG quality (0-100) */
    private int quality = 100;
    /** 是否运行 */
    private boolean isRunning;

    public MinicapServer(IDevice device) {
        super("minicap-server");
        this.device = device;
    }

    public MinicapServer(IDevice device, int port, float zoom, int rotate, int quality) {
        super("minicap-server");
        this.device = device;
        this.port = port;
        this.zoom = zoom;
        this.rotate = rotate;
        this.quality = quality;
    }

    /**
     * 是否运行
     * @return true 已运行 false 未运行
     */
    public boolean isRunning() {
        return this.isRunning;
    }

    @Override
    public void close() {
        this.isRunning = false;
    }

    @Override
    public void run() {
        if (this.isRunning) {
            throw new IllegalStateException("Minicap服务已运行");
        } else {
            this.isRunning = true;
        }
        log.info("The minicap server running...");
        try {
            // push minicap
            String minicapPath = getMinicapPath();
            log.info("Push file local: {}, remote: {}", minicapPath, MINICAP_TMP_DIR);
            DdmlibUtils.pushFile(device, getResourceAsStream(minicapPath), MINICAP_TMP_DIR, "777");

            // push minicap-nopie
            String minicapNopiePath = getMinicapNopiePath();
            log.info("Push file local: {}, remote: {}", minicapNopiePath, MINICAP_NOPIE_TMP_DIR);
            DdmlibUtils.pushFile(device, getResourceAsStream(minicapNopiePath), MINICAP_NOPIE_TMP_DIR, "777");

            // push minicap.so
            String minicapSoPath = getMinicapSoPath();
            log.info("Push file local: {}, remote: {}", minicapSoPath, MINICAP_SO_TMP_DIR);
            DdmlibUtils.pushFile(device, getResourceAsStream(minicapSoPath), MINICAP_SO_TMP_DIR, "777");

            // forward port
            device.createForward(port, "minicap", IDevice.DeviceUnixSocketNamespace.ABSTRACT);
            log.info("Forward tcp:{} localabstract:minicap", port);

            // run minicap server
            String command = getCommand();
            log.info("Execute shell command: {}", command);
            device.executeShellCommand(command, new IShellOutputReceiver() {
                @Override
                public void addOutput(byte[] bytes, int i, int i1) {
                    String ret = new String(bytes, i, i1);
                    String[] split = ret.split("\n");
                    for (String line : split) {
                        if (StringUtils.isNotEmpty(line)) {
                            log.info(line.trim());
                        }
                    }
                }

                @Override
                public void flush() {
                }

                @Override
                public boolean isCancelled() {
                    return !MinicapServer.this.isRunning;
                }
            }, Integer.MAX_VALUE, TimeUnit.DAYS);
        } catch (Exception e) {
            log.error("Minicap server exception", e);
        } finally {
            try {
                device.removeForward(port, "minicap", IDevice.DeviceUnixSocketNamespace.ABSTRACT);
            } catch (Exception e) {
                log.error("Remove forward fail. port: {}", e, port);
            }
        }
        this.isRunning = false;
        log.info("Minicap server closed");
    }

    /**
     * 获取cpu abi
     * @return cpu abi
     * @throws Exception 获取失败
     */
    protected String getAbi() throws Exception {
        if (abi == null) {
            abi = DdmlibUtils.getAbi(device);
        }
        return abi;
    }

    /**
     * 获取sdk api
     * @return sdk api
     * @throws Exception 获取失败
     */
    protected int getApi() throws Exception {
        if (api == 0) {
            api = DdmlibUtils.getApi(device);
        }
        return api;
    }

    /**
     * 获取屏幕支持
     * @return PhysicalSize
     * @throws Exception 获取失败
     */
    protected PhysicalSize getSize() throws Exception {
        if (size == null) {
            size = DdmlibUtils.getPhysicalSize(device);
        }
        return size;
    }

    /**
     * Get display projection (<w>x<h>@<w>x<h>/{0|90|180|270})
     * @return Display projection
     */
    protected String getProjection() throws Exception {
        PhysicalSize size = getSize();
        return String.format("%sx%s@%sx%s/%s", size.getWidth(), size.getHeight(),
                Math.round(size.getWidth() * zoom), Math.round(size.getHeight() * zoom), rotate);
    }

    /**
     * 获取执行的命令
     * @return shell命令
     */
    protected String getCommand() throws Exception {
        return String.format("LD_LIBRARY_PATH=/data/local/tmp /data/local/tmp/minicap -P %s -Q %s", getProjection(), quality);
    }

    /**
     * 获取minicap路径
     * @return minicap路径
     */
    protected String getMinicapPath() throws Exception {
        return String.format("minicap/bin/%s/minicap", getAbi());
    }

    /**
     * 获取minicap-nopie路径
     * @return minicap-nopie路径
     */
    protected String getMinicapNopiePath() throws Exception {
        return String.format("minicap/bin/%s/minicap-nopie", getAbi());
    }

    /**
     * 获取minicap.so路径
     * @return minicap.so路径
     */
    protected String getMinicapSoPath() throws Exception {
        return String.format("minicap/shared/android-%s/%s/minicap.so", getApi(), getAbi());
    }
}
