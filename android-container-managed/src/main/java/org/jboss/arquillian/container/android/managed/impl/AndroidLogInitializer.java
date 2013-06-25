package org.jboss.arquillian.container.android.managed.impl;


import org.jboss.arquillian.android.spi.event.AndroidBridgeInitialized;
import org.jboss.arquillian.android.spi.event.AndroidBridgeTerminated;
import org.jboss.arquillian.android.spi.event.AndroidDeviceReady;
import org.jboss.arquillian.container.android.api.AndroidDevice;
import org.jboss.arquillian.container.android.managed.configuration.AndroidManagedContainerConfiguration;
import org.jboss.arquillian.container.android.managed.configuration.AndroidSDK;
import org.jboss.arquillian.container.android.utils.LogcatHelper;
import org.jboss.arquillian.container.spi.context.annotation.ContainerScoped;
import org.jboss.arquillian.core.api.Instance;
import org.jboss.arquillian.core.api.InstanceProducer;
import org.jboss.arquillian.core.api.annotation.Inject;
import org.jboss.arquillian.core.api.annotation.Observes;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.Future;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class AndroidLogInitializer {

    private static final Logger logger = Logger.getLogger(AndroidLogInitializer.class.getName());

    @Inject
    @ContainerScoped
    private InstanceProducer<LogcatReader> logcat;

    @Inject
    @ContainerScoped
    private InstanceProducer<LogcatHelper> logcatHelper;

    @Inject
    private Instance<AndroidManagedContainerConfiguration> configuration;

    @Inject
    private Instance<ProcessExecutor> executor;

    @Inject
    private Instance<AndroidSDK> androidSDK;

    @Inject
    private Instance<AndroidDevice> androidDevice;

    private Future<Void> logcatFuture;

    public void initAndroidLog(@Observes AndroidDeviceReady event) {
        logger.info("Initializing Android LogcatReader");

        ProcessExecutor executor = this.executor.get();

        if(logcatHelper.get() == null) {
            logcatHelper.set(new LogcatHelper(configuration.get()));
        }

        LogcatReader logcat = new LogcatReader(configuration.get(), androidSDK.get(), androidDevice.get());
        logcat.setWriter(logcatHelper.get().prepareWriter());

        logcatFuture = executor.submit(logcat);

        this.logcat.set(logcat);
    }

    public void terminateAndroidLog(@Observes AndroidBridgeTerminated event) {

        logcatFuture.cancel(true);

    }

    public class LogcatReader implements Callable<Void> {

        private AndroidManagedContainerConfiguration configuration;
        private AndroidSDK androidSDK;
        private AndroidDevice androidDevice;

        private Writer writer;

        private Pattern pattern;

        private List<String> whiteList;
        private List<String> blackList;
        private Map<Integer, String> processMap = new HashMap<Integer, String>();

        public LogcatReader(AndroidManagedContainerConfiguration configuration, AndroidSDK androidSDK, AndroidDevice androidDevice) {
            this.configuration = configuration;
            this.androidSDK = androidSDK;
            this.androidDevice = androidDevice;

            String[] whiteList = configuration.getLogPackageWhitelist().split(",");
            this.whiteList = new ArrayList<String>();
            for(String packageName : whiteList) {
                this.whiteList.add(escapePackageName(packageName));
            }

            String[] blackList = configuration.getLogPackageBlacklist().split(",");
            this.blackList = new ArrayList<String>();
            for(String packageName : blackList) {
                this.blackList.add(escapePackageName(packageName));
            }

        }

        @Override
        public Void call() throws Exception {
            if(writer == null) {
                return null;
            }

            try {
                ProcessBuilder builder = new ProcessBuilder(androidSDK.getAdbPath(), "-s", androidDevice.getSerialNumber(), "logcat", "-c");
                Process process = builder.start();

                builder = new ProcessBuilder(androidSDK.getAdbPath(), "-s", androidDevice.getSerialNumber(), "logcat", "*:" + configuration.getLogLevel());
                process = builder.start();

                BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));
                String line = null;
                while((line = reader.readLine()) != null) {
                    if(shouldWrite(line)) {
                        writer.write(line);
                    }
                }

            } catch(IOException e) {
                logger.log(Level.SEVERE, "Error with logging", e);
            }
            return null;
        }

        private String escapePackageName(String packageName) {
            return packageName
                    .replace("\\", "\\\\")
                    .replace(".", "\\.")
                    .replace("[", "\\[")
                    .replace("]", "\\]")
                    .replace("(", "\\(")
                    .replace(")", "\\)")
                    .replace("?", "\\?")
                    .replace("+", "\\+")
                    .replace("*", ".*?");
        }

        private void loadProcessMap() {
            try {
                processMap.clear();

                ProcessBuilder processBuilder = new ProcessBuilder(this.androidSDK.getAdbPath(), "-s", this.androidDevice.getSerialNumber(), "shell", "ps");
                Process process = processBuilder.start();

                Pattern pattern = Pattern.compile(".*?\\s+([0-9]+)\\s+[0-9]+\\s+[0-9]+\\s+[0-9]+\\s+[0-9a-f]+\\s+[0-9a-f]+\\s.?\\s(.*)");
                BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));
                String processLine = null;
                while((processLine = reader.readLine()) != null) {
                    Matcher matcher = pattern.matcher(processLine);

                    if(!matcher.matches()) {
                        continue;
                    }

                    Integer processId = Integer.valueOf(matcher.group(1));
                    String processName = matcher.group(2);

                    processMap.put(processId, processName);
                }

            } catch (IOException e) {
                logger.log(Level.SEVERE, "Couldn't load process map!", e);
            }
        }

        private boolean shouldWrite(String line) {
            if(!configuration.isIntelliLogEnabled()) {
                return true;
            }

            if(pattern == null) {
                pattern = Pattern.compile("./.+?\\(([\\s0-9]+?)\\):.*");
            }

            Matcher matcher = pattern.matcher(line);
            if(!matcher.matches()) {
                return false;
            }

            String processIdString = matcher.group(1).trim();
            Integer processId = Integer.valueOf(processIdString);

            if(!processMap.containsKey(processId)) {
                loadProcessMap();
            }

            String processName = processMap.get(processId);
            if(processName == null) {
                processName = "";
            }

            for(String regex : whiteList) {
                if(processName.matches(regex)) {
                    return true;
                }
            }

            for(String regex : blackList) {
                if(processName.matches(regex)) {
                    return false;
                }
            }

            return true;
        }

        public Writer getWriter() {
            return writer;
        }

        public void setWriter(Writer writer) {
            this.writer = writer;
        }

        public void setAndroidSDK(AndroidSDK androidSDK) {
            this.androidSDK = androidSDK;
        }

        public void setAndroidDevice(AndroidDevice androidDevice) {
            this.androidDevice = androidDevice;
        }

        public void setConfiguration(AndroidManagedContainerConfiguration configuration) {
            this.configuration = configuration;
        }
    }


}
