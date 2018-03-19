package org.cuiyang.minicap;

import com.android.ddmlib.IDevice;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.IOUtils;

import java.io.Closeable;
import java.util.concurrent.TimeoutException;

/**
 * 屏幕映射
 *
 * @author cuiyang
 * @since 2018/3/6
 */
@Slf4j
public class ScreenProjection extends Thread implements Closeable {

    /** 缩放 */
    private float zoom = 1;
    /** 旋转 0|90|180|270 */
    private int rotate = 0;
    /** -Q <value>: JPEG quality (0-100) */
    private int quality = 100;

    private IDevice device;
    private MinicapServer server;
    private MinicapClient client;
    private ScreenListener listener;

    public ScreenProjection(IDevice device, ScreenListener listener) {
        this.device = device;
        this.listener = listener;
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
    public void restart() {
        this.server.setZoom(zoom);
        this.server.setRotate(rotate);
        this.server.setQuality(quality);
        this.server.restart();
    }

    private void startServer() throws TimeoutException, InterruptedException {
        this.server = new MinicapServer(device, 1717);
        this.server.setZoom(zoom);
        this.server.setRotate(rotate);
        this.server.setQuality(quality);
        this.server.start();
    }

    private void startClient() {
        this.client = new MinicapClient(1717);
        this.client.start();
    }

    @Override
    public void run() {
        try {
            startServer();
            startClient();

            while (this.server.isRunning()) {
                byte[] take = this.client.take();
                try {
                    this.listener.projection(take);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        } catch (Exception e) {
            log.error("屏幕映射失败", e);
        } finally {
            close();
        }
    }

    @Override
    public void close() {
        IOUtils.closeQuietly(server);
        IOUtils.closeQuietly(client);
    }
}
