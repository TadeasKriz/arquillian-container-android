package org.jboss.arquillian.android.example;

import org.jboss.arquillian.container.test.api.TargetsContainer;
import org.jboss.arquillian.drone.api.annotation.Drone;
import org.jboss.arquillian.junit.Arquillian;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.openqa.selenium.WebDriver;

@RunWith(Arquillian.class)abstract
public class AndroidWebTestCase {
    
    @Test
    @TargetsContainer("android-single")
    public void testGoogle(@Drone WebDriver driver) throws Exception {
        driver.get("http://www.google.com");
        Assert.assertTrue(true);
    }
}
