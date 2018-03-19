package org.cuiyang.minicap;

import org.junit.Test;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;

/**
 * ScreenProjectionTest
 *
 * @author cuiyang
 * @since 2018/3/19
 */
public class ScreenProjectionTest extends BaseIDeviceTest {

    @Test
    public void testStart() throws IOException {
        ScreenProjection screenProjection = new ScreenProjection(device, frame -> {
            BufferedImage image;
            try {
                image = ImageIO.read(new ByteArrayInputStream(frame));
                ImageIO.write(image, "jpg", new File("d:/minicap-client.png"));
            } catch (IOException e) {
                e.printStackTrace();
            }
        });
        screenProjection.start();
        //noinspection ResultOfMethodCallIgnored
        System.in.read();
    }
}
