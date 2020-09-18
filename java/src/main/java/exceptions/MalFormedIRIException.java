package exceptions;
/*
Written by sarker.
Written at 7/31/18.
*/

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.invoke.MethodHandles;

public class MalFormedIRIException extends Exception {

    final static Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());
    // log level: ALL < DEBUG < INFO < WARN < ERROR < FATAL < OFF

    protected MalFormedIRIException() {
        super();
    }

    public MalFormedIRIException(String message) {
        super(message);
    }

    public MalFormedIRIException(String message, Throwable cause) {
        super(message, cause);
    }

    public MalFormedIRIException(Throwable cause) {
        super(cause);
    }
}
