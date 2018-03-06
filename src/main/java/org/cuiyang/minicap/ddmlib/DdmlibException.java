package org.cuiyang.minicap.ddmlib;

/**
 * Ddmlib Exception
 *
 * @author cuiyang
 */
public class DdmlibException extends Exception {

    public DdmlibException() {
    }

    public DdmlibException(String message) {
        super(message);
    }

    public DdmlibException(String message, Throwable cause) {
        super(message, cause);
    }

    public DdmlibException(Throwable cause) {
        super(cause);
    }

    public DdmlibException(String message, Throwable cause, boolean enableSuppression, boolean writableStackTrace) {
        super(message, cause, enableSuppression, writableStackTrace);
    }
}
