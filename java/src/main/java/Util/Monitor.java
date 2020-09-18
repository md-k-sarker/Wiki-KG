package Util;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.swing.*;
import java.io.PrintStream;
import java.lang.invoke.MethodHandles;
import java.text.DateFormat;
import java.util.Date;


/**
 * Monitor class to monitor the progress of the program.
 */
public class Monitor {

    final static Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());
    // log level: ALL < DEBUG < INFO < WARN < ERROR < FATAL < OFF

    private long startTime;
    private PrintStream out;
    private DateFormat dateFormat;
    private JTextPane jTextPane;

    public DateFormat getDateFormat() {
        return dateFormat;
    }

    public void setDateFormat(DateFormat dateFormat) {
        this.dateFormat = dateFormat;
    }

    /**
     *
     */
    public Monitor() {
        this(null);
    }

    /**
     * @param _printStream
     */
    public Monitor(PrintStream _printStream) {
        this(_printStream, null);
    }

    /**
     * @param _printStream
     * @param textPane
     */
    public Monitor(PrintStream _printStream, JTextPane textPane) {
        if (null != _printStream) {
            this.out = _printStream;
        }
        this.dateFormat = Utility.getDateTimeFormat();
        if (null != textPane) {
            this.jTextPane = textPane;
        }
    }

    /*
     * http://slf4j.42922.n3.nabble.com/Logging-to-file-with-slf4j-Logger-Where-do
     * -log-file-go-td46087.html It appears that you have misunderstood the purpose
     * of SLF4J. If you place slf4j-jdk14-1.5.6.jar then slf4j-api will bind with
     * java.util.logging. Logback will not be used. Only if you place
     * logback-core.jar and logback-classic.jar on your class path (but not
     * slf4j-jdk14-1.5.6.jar) will SLF4J API bind with logback. SLF4J binds with one
     * and only one underlying logging API (per JVM launch).
     */
    // public static void safeExit(ErrorCodes.Error errorCode) {
    // stop("Error occurred: Safe Exiting...");
    // }

    public void displayMessage(String message, boolean write) {

        System.out.println(message);
        if (write && null != out) {
            out.println(message);
        }
        String txt = "";

        if (null != jTextPane) {
            txt = jTextPane.getText();
        }
        if (txt == null) {
            txt = "";
        }
        if (null != jTextPane)
            jTextPane.setText(txt + "\n" + message);
    }

    public void writeMessage(String message) {
        if (null != out) {
            out.println(message);
            out.flush();
        }

//        String txt = jTextPane.getText();
//        if (txt == null) {
//            txt = "";
//        }
//        jTextPane.setText(txt + "\n" + message);
    }

    public void error(String message) {
        System.err.println(message);
    }

    public void start(String message, boolean write) {
        if (message.trim() != "") {
            displayMessage(message, write);
        }
        Date date = new Date();
        startTime = System.currentTimeMillis();
        displayMessage("Program starts at: " + dateFormat.format(date), write);
    }

    public void displayMessageWithTime(String message, boolean write) {
        Date date = new Date();
        Long end = System.currentTimeMillis();
        if (message.trim() != "") {
            displayMessage(message, write);
        }
        displayMessage(" Time Now:" + dateFormat.format(date), write);
        displayMessage("Duration from startTime:" + (end - startTime) / 1000.0 + " sec", write);
    }

    public void stop(String message, boolean write) {
        Date date = new Date();
        Long end = System.currentTimeMillis();
        if (message.trim() != "") {
            displayMessage(message, write);
        }
        displayMessage("Program ends at: " + dateFormat.format(date), write);
        displayMessage("Program duration: " + (end - startTime) / 1000.0 + " sec", write);
    }

    public void stopSystem(String message, boolean write) {
        stop(message, write);
        if (null != out) {
            out.close();
        }
        System.exit(1);
    }
}

