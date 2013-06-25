package org.jboss.arquillian.container.android.managed.impl;

import org.jboss.arquillian.android.spi.event.AndroidBridgeInitialized;
import org.jboss.arquillian.android.spi.event.AndroidContainerStart;
import org.jboss.arquillian.android.spi.event.AndroidDeviceReady;
import org.jboss.arquillian.container.android.api.AndroidBridge;
import org.jboss.arquillian.container.android.api.AndroidDevice;
import org.jboss.arquillian.container.android.api.IdentifierGenerator;
import org.jboss.arquillian.container.android.managed.AbstractAndroidTestTestBase;
import org.jboss.arquillian.container.android.managed.configuration.AndroidManagedContainerConfiguration;
import org.jboss.arquillian.container.android.managed.configuration.AndroidSDK;
import org.jboss.arquillian.container.android.utils.AndroidIdentifierGenerator;
import org.jboss.arquillian.container.android.utils.LogcatHelper;
import org.jboss.arquillian.container.spi.context.ContainerContext;
import org.jboss.arquillian.container.spi.context.annotation.ContainerScoped;
import org.junit.After;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.rules.Timeout;
import org.junit.runner.RunWith;
import org.mockito.Mockito;
import org.mockito.runners.MockitoJUnitRunner;

import java.io.IOException;
import java.io.Writer;
import java.util.List;

import static org.junit.Assert.assertNotNull;

@RunWith(MockitoJUnitRunner.class)
//@Ignore
public class AndroidLogInitializerTestCase extends AbstractAndroidTestTestBase {

    private AndroidManagedContainerConfiguration configuration;

    private AndroidSDK androidSDK;

    private String RUNNING_EMULATOR_AVD_NAME = "android-arm-container";

    private String RUNNING_EMULATOR_CONSOLE_PORT = "5554";

    @Override
    protected void addExtensions(List<Class<?>> extensions) {
        extensions.add(AndroidBridgeConnector.class);
        extensions.add(AndroidDeviceSelectorImpl.class);
        extensions.add(AndroidLogInitializer.class);
    }

    @Before
    public void setup() {
        configuration = new AndroidManagedContainerConfiguration();
        configuration.setAvdName(RUNNING_EMULATOR_AVD_NAME);
        configuration.setConsolePort(RUNNING_EMULATOR_CONSOLE_PORT);
        androidSDK = new AndroidSDK(configuration);

        getManager().getContext(ContainerContext.class).activate("container1");

        bind(ContainerScoped.class, AndroidManagedContainerConfiguration.class, configuration);
        bind(ContainerScoped.class, AndroidSDK.class, androidSDK);
    }

    @After
    public void tearDown() {
        AndroidBridge bridge = getManager().getContext(ContainerContext.class).getObjectStore().get(AndroidBridge.class);
        bridge.disconnect();
    }

    @Test//(timeout = 15000) // TODO are 15 seconds enough?
    public void testLogInitialization() {

        ProcessExecutor processExecutor = new ProcessExecutor();

        TestWriter testWriter = new TestWriter();
        LogcatHelper mockedLogcatHelper = Mockito.mock(LogcatHelper.class);
        Mockito.when(mockedLogcatHelper.prepareWriter()).thenReturn(testWriter);

        bind(ContainerScoped.class, ProcessExecutor.class, processExecutor);
        bind(ContainerScoped.class, LogcatHelper.class, mockedLogcatHelper);
        bind(ContainerScoped.class, IdentifierGenerator.class, new AndroidIdentifierGenerator());

        fire(new AndroidContainerStart());

        AndroidBridge bridge = getManager().getContext(ContainerContext.class).getObjectStore().get(AndroidBridge.class);
        assertNotNull("Android Bridge should be created but is null!", bridge);

        AndroidDevice runningDevice = getManager().getContext(ContainerContext.class).getObjectStore().get(AndroidDevice.class);
        assertNotNull("Android device is null object!", runningDevice);

        assertEventFired(AndroidDeviceReady.class, 1);

        while(!testWriter.success) {

        }
    }

    private class TestWriter extends Writer {

        public volatile boolean success = false;

        @Override
        public void write(char[] cbuf, int off, int len) throws IOException {
            if(success) {
                return;
            }

            String line = String.copyValueOf(cbuf, off, len);
            if(line.matches("./.+?\\([\\s0-9]+?\\):.*")) {
                success = true;
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
