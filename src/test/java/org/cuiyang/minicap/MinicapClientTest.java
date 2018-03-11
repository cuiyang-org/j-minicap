package org.cuiyang.minicap;

import org.apache.commons.io.IOUtils;
import org.junit.Test;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.File;

/**
 * MinicapClientTest
 *
 * @author cuiyang
 */
public class MinicapClientTest {

    @Test
    public void testRun() throws Exception {
        MinicapClient minicapClient = new MinicapClient(1717);
        minicapClient.start();
        while (true) {
            byte[] take = minicapClient.take();
            BufferedImage image = ImageIO.read(new ByteArrayInputStream(take));
            ImageIO.write(image, "jpg", new File("d:/minicap-client.png"));
        }
    }

    @Test(expected = InterruptedException.class)
    public void testClose() throws Exception {
        MinicapClient minicapClient = new MinicapClient(1717);
        minicapClient.start();
        new Thread(() -> {
            try {
                Thread.sleep(5000);
            } catch (InterruptedException ignore) {
            }
            IOUtils.closeQuietly(minicapClient);
            System.out.println("关闭Minicap客户端");
        }).start();
        minicapClient.take();
    }
}
