package org.cuiyang.minicap;

import com.android.ddmlib.IDevice;
import com.android.ddmlib.IShellOutputReceiver;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.cuiyang.minicap.ddmlib.DdmlibUtils;
import org.cuiyang.minicap.ddmlib.PhysicalSize;

import java.io.Closeable;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

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
    private AtomicBoolean isRunning = new AtomicBoolean(false);
    /** 是否重启 */
    private AtomicBoolean restart = new AtomicBoolean(false);

    public MinicapServer(IDevice device) {
        super("minicap-server");
        this.device = device;
    }

    public MinicapServer(IDevice device, int port) {
        super("minicap-server");
        this.device = device;
        this.port = port;
    }

    /**
     * 是否运行
     * @return true 已运行 false 未运行
     */
    public boolean isRunning() {
        return this.isRunning.get();
    }

    @Override
    public void close() {
        this.isRunning.set(false);
        this.restart.set(true);
    }

    /**
     * 设置缩放比例，默认为1不缩放
     * @param zoom 缩放比例
     */
    public void setZoom(float zoom) {
        this.zoom = zoom;
    }

    /**
     * 设置旋转角度，默认为0不旋转
     * @param rotate 旋转角度
     */
    public void setRotate(int rotate) {
        this.rotate = rotate;
    }

    /**
     * 设置图片质量，默认为100最高质量
     * @param quality 图片质量
     */
    public void setQuality(int quality) {
        this.quality = quality;
    }

    /**
     * 重启
     */
    public synchronized void restart() {
        this.restart.set(true);
    }

    @Override
    public synchronized void start() {
        if (this.isRunning.get()) {
            throw new IllegalStateException("Minicap服务已运行");
        } else {
            this.isRunning.set(true);
        }
        try {
            // push minicap
            String minicapPath = getMinicapPath();
            log.info("推送文件 local: {}, remote: {}", minicapPath, MINICAP_TMP_DIR);
            DdmlibUtils.pushFile(device, getResourceAsStream(minicapPath), MINICAP_TMP_DIR, "777");

            // push minicap-nopie
            String minicapNopiePath = getMinicapNopiePath();
            log.info("推送文件 local: {}, remote: {}", minicapNopiePath, MINICAP_NOPIE_TMP_DIR);
            DdmlibUtils.pushFile(device, getResourceAsStream(minicapNopiePath), MINICAP_NOPIE_TMP_DIR, "777");

            // push minicap.so
            String minicapSoPath = getMinicapSoPath();
            log.info("推送文件 local: {}, remote: {}", minicapSoPath, MINICAP_SO_TMP_DIR);
            DdmlibUtils.pushFile(device, getResourceAsStream(minicapSoPath), MINICAP_SO_TMP_DIR, "777");

            // forward port
            device.createForward(port, "minicap", IDevice.DeviceUnixSocketNamespace.ABSTRACT);
            log.info("端口转发 tcp:{} localabstract:minicap", port);
        } catch (Exception e) {
            throw new IllegalStateException("Minicap服务启动失败");
        }
        super.start();
    }

    @Override
    public void run() {
        log.info("Minicap服务已启动");
        while (this.isRunning.get()) {
            try {
                // run minicap server
                String command = getCommand();
                log.info("拉起Minicap服务 command: {}", command);
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
                        boolean result = restart.get();
                        restart.set(false);
                        return result;
                    }
                }, Integer.MAX_VALUE, TimeUnit.DAYS);
            } catch (Exception e) {
                log.error("Minicap服务运行异常", e);
            }
        }
        try {
            device.removeForward(port, "minicap", IDevice.DeviceUnixSocketNamespace.ABSTRACT);
        } catch (Exception e) {
            log.error("移除端口转发失败. port: {}", e, port);
        }
        this.isRunning.set(false);
        log.info("Minicap服务已关闭");
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
