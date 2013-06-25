package org.jboss.arquillian.container.android.utils;


import org.jboss.arquillian.container.android.managed.configuration.AndroidManagedContainerConfiguration;
import org.jboss.arquillian.container.android.managed.configuration.LogLevel;
import org.jboss.arquillian.container.android.managed.configuration.LogType;
import org.jboss.arquillian.container.android.managed.impl.AndroidLogInitializer;
import org.jboss.arquillian.core.api.Instance;
import org.jboss.arquillian.core.api.annotation.Inject;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.io.Writer;
import java.util.logging.Logger;

public class LogcatHelper {

    private static final Logger logger = Logger.getLogger(LogcatHelper.class.getName());

    AndroidManagedContainerConfiguration configuration;

    public LogcatHelper(AndroidManagedContainerConfiguration configuration) {
        this.configuration = configuration;
    }

    public Writer prepareWriter() { // TODO implement log4j
        if(configuration.getLogType().equals(LogType.OUTPUT)) {
            return new LogcatToConsoleWriter();
        } else if(configuration.getLogType().equals(LogType.LOGGER)) {
            return new LogcatToLoggerWriter(Logger.getLogger("Logcat"));
        } else if(configuration.getLogType().equals(LogType.FILE)) {
            try {
                return new LogcatToFileWriter(configuration.getLogFile());
            } catch (IOException e) {
                logger.warning("Couldn't open log file!");
                return null;
            }
        } else {
            return null;
        }

    }

    public static class LogcatToFileWriter extends FileWriter {

        public LogcatToFileWriter(String fileName) throws IOException {
            super(fileName);
        }

        @Override
        public void write(String str) throws IOException {
            super.write(str + "\n");
        }
    }

    public static class LogcatToConsoleWriter extends  Writer {

        @Override
        public void write(char[] cbuf, int off, int len) throws IOException {
            String line = String.copyValueOf(cbuf, off, len);
            System.out.println("LOGCAT: " + line);
        }

        @Override
        public void flush() throws IOException {
            System.out.flush();
        }

        @Override
        public void close() throws IOException {
            System.out.flush();
        }
    }

    public static class LogcatToLoggerWriter extends Writer {

        private Logger logger;

        public LogcatToLoggerWriter(Logger logger) {
            this.logger = logger;
        }

        @Override
        public void write(char[] cbuf, int off, int len) throws IOException {
            String line = String.copyValueOf(cbuf, off, len);
            if(line.startsWith(LogLevel.ERROR)) {
                logger.severe(line);
            } else if(line.startsWith(LogLevel.WARN)) {
                logger.warning(line);
            } else if(line.startsWith(LogLevel.INFO)) {
                logger.info(line);
            } else if(line.startsWith(LogLevel.DEBUG)) {
                logger.config(line); // TODO should we use config or fine?
            } else if(line.startsWith(LogLevel.VERBOSE)) {
                logger.fine(line);
            } else {
                logger.finer(line);
            }
        }

        @Override
        public void flush() throws IOException {

        }

        @Override
        public void close() throws IOException {

        }
    }

}
