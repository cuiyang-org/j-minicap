package org.cuiyang.minicap.util;

import java.io.InputStream;

/**
 * ResourceUtils
 *
 * @author cuiyang
 */
public class ResourceUtils {

    public static InputStream getResource(String name) {
        return ResourceUtils.class.getClassLoader().getResourceAsStream(name);
    }
}
