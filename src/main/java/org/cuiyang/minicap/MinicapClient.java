package org.cuiyang.minicap;

import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.ArrayUtils;

import java.io.IOException;
import java.io.InputStream;
import java.net.Socket;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

/**
 * Minicap client
 *
 * @author cuiyang
 */
@Slf4j
public class MinicapClient extends Thread {

    /** 主机 */
    private String host = "localhost";
    /** 端口号 */
    private int port;
    /** 队列大小 */
    private int queueSize = 50;
    /** 和minicap通信 */
    private Socket socket;
    /** 存放图片队列 */
    private BlockingQueue<byte[]> frameQueue;
    /** 是否运行 */
    private RunningState state;
    /** 调用take的线程 */
    private Thread takeThread;

    /** Banner */
    private Banner banner;
    /** 已读取Banner的长度 */
    private int readBannerBytes = 0;
    /** Banner的长度 */
    private int bannerLength = 2;
    /** 已读取帧的长度 */
    private int readFrameBytes = 0;
    /** 帧的长度 */
    private int frameBodyLength = 0;

    public MinicapClient(int port) {
        super("minicap-client");
        this.port = port;
        init();
    }

    public MinicapClient(String host, int port) {
        super("minicap-client");
        this.host = host;
        this.port = port;
        init();
    }

    /**
     * 获取帧，如果没有则阻塞
     * @return 帧
     * @throws InterruptedException 阻塞中断
     */
    public synchronized byte[] take() throws InterruptedException {
        checkClosed();
        takeThread = Thread.currentThread();
        return frameQueue.take();
    }

    @Override
    public void run() {
        if (this.state != RunningState.READY) {
            throw new IllegalStateException("The minicap client no ready");
        } else {
            this.state = RunningState.STARTUP;
        }
        log.info("The minicap client running...");
        try {
            this.socket = new Socket(host, port);
            this.state = RunningState.RUNNING;
            InputStream inputStream = socket.getInputStream();
            handleServerResponse(inputStream);
        } catch (Exception e) {
            log.error("The minicap client run error", e);
        } finally {
            IOUtils.closeQuietly(this.socket);
        }
        if (takeThread != null) {
            takeThread.interrupt();
        }
        this.state = RunningState.CLOSED;
        log.info("Minicap client closed");
    }

    /**
     * 处理服务端响应
     */
    protected void handleServerResponse(InputStream inputStream) throws IOException {
        // 帧
        byte[] frameBody = new byte[0];
        // 缓存
        byte[] chunk = new byte[102400];
        // 读取的长度
        int len = 0;
        while (len >= 0) {
            len = inputStream.read(chunk);
            for (int cursor = 0; cursor < len;) {
                int data = chunk[cursor] & 0xff;
                if (readBannerBytes < bannerLength) {
                    // 读取Banner
                    parseBanner(data);
                    cursor ++;
                } else if (readFrameBytes < 4) {
                    // 读取帧的长度
                    frameBodyLength += (data << (readFrameBytes * 8));
                    cursor ++;
                    readFrameBytes ++;
                } else {
                    // 读取帧
                    if (len - cursor >= frameBodyLength) {
                        byte[] subByte = ArrayUtils.subarray(chunk, cursor, cursor + frameBodyLength);
                        frameBody = ArrayUtils.addAll(frameBody, subByte);
                        // 获取到一帧
                        offer(frameBody);
                        cursor += frameBodyLength;
                        frameBodyLength = readFrameBytes = 0;
                        frameBody = new byte[0];
                    } else {
                        byte[] subByte = ArrayUtils.subarray(chunk, cursor, len);
                        frameBody = ArrayUtils.addAll(frameBody, subByte);
                        frameBodyLength -= len - cursor;
                        readFrameBytes += len - cursor;
                        cursor = len;
                    }
                }
            }
        }
    }

    /**
     * 解析Banner
     */
    protected void parseBanner(int data) {
        if (banner == null) {
            banner = new Banner();
        }
        switch (readBannerBytes) {
            case 0:
                // version
                banner.setVersion(data);
                break;
            case 1:
                // length
                banner.setLength(bannerLength = data);
                break;
            case 2:
            case 3:
            case 4:
            case 5:
                // pid
                int pid = banner.getPid();
                pid += (data << ((readBannerBytes - 2) * 8));
                banner.setPid(pid);
                break;
            case 6:
            case 7:
            case 8:
            case 9:
                // real width
                int realWidth = banner.getRealWidth();
                realWidth += (data << ((readBannerBytes - 6) * 8));
                banner.setRealWidth(realWidth);
                break;
            case 10:
            case 11:
            case 12:
            case 13:
                // real height
                int realHeight = banner.getRealHeight();
                realHeight += (data << ((readBannerBytes - 10) * 8));
                banner.setRealHeight(realHeight);
                break;
            case 14:
            case 15:
            case 16:
            case 17:
                // virtual width
                int virtualWidth = banner.getVirtualWidth();
                virtualWidth += (data << ((readBannerBytes - 14) * 8));
                banner.setVirtualWidth(virtualWidth);
                break;
            case 18:
            case 19:
            case 20:
            case 21:
                // virtual height
                int virtualHeight = banner.getVirtualHeight();
                virtualHeight += (data << ((readBannerBytes - 18) * 8));
                banner.setVirtualHeight(virtualHeight);
                break;
            case 22:
                // orientation
                banner.setOrientation(data * 90);
                break;
            case 23:
                // quirks
                banner.setQuirks(data);
                break;
            default:
        }

        readBannerBytes ++;
        if (readBannerBytes == bannerLength) {
            log.info("Banner: {}", banner);
        }
    }

    /**
     * 将一帧放入到队列
     */
    protected void offer(byte[] frame) {
        if (frameQueue.size() >= queueSize) {
            frameQueue.poll();
            log.debug("Frame queue full,will throw away a frame");
        }
        frameQueue.offer(frame);
    }

    /**
     * 检查是否关闭
     */
    protected void checkClosed() {
        if (this.state == RunningState.CLOSED) {
            throw new IllegalStateException("The minicap client has closed");
        }
    }

    /**
     * 初始化
     */
    protected void init() {
        this.frameQueue = new LinkedBlockingQueue<>(queueSize);
        this.state = RunningState.READY;
    }
}
