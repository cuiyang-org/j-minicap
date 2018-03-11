package org.cuiyang.minicap.util;

import java.io.InputStream;
import java.net.URL;

/**
 * ResourceUtils
 *
 * @author cuiyang
 */
public class ResourceUtils {

    public static InputStream getResourceAsStream(String name) {
        return ResourceUtils.class.getClassLoader().getResourceAsStream(name);
    }

    public static URL getResource(String name) {
        return ResourceUtils.class.getClassLoader().getResource(name);
    }
}
