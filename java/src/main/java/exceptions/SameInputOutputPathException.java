package exceptions;
/*
Written by sarker.
Written at 4/18/20.
*/

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.invoke.MethodHandles;

public class SameInputOutputPathException extends Exception {
    final static Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());
    // log level: ALL < DEBUG < INFO < WARN < ERROR < FATAL < OFF


    public SameInputOutputPathException() {
        super();
    }

    public SameInputOutputPathException(String message) {
        super(message);
    }

    public SameInputOutputPathException(String message, Throwable cause) {
        super(message, cause);
    }

    public SameInputOutputPathException(Throwable cause) {
        super(cause);
    }

    protected SameInputOutputPathException(String message, Throwable cause, boolean enableSuppression, boolean writableStackTrace) {
        super(message, cause, enableSuppression, writableStackTrace);
    }
}
