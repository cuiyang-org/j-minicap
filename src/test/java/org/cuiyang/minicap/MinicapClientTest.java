package org.cuiyang.minicap;

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
public class MinicapClientTest extends BaseIDeviceTest {

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
}
