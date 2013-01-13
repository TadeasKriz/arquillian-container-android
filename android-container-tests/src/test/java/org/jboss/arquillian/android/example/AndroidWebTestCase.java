package org.jboss.arquillian.android.example;

import org.jboss.arquillian.drone.api.annotation.Drone;
import org.jboss.arquillian.junit.Arquillian;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.openqa.selenium.WebDriver;

@RunWith(Arquillian.class)
public class AndroidWebTestCase {
    
    @Test
    public void testGoogle(@Drone WebDriver driver) throws Exception {
        driver.get("http://www.google.com");
        Assert.assertTrue(true);
    }
}
