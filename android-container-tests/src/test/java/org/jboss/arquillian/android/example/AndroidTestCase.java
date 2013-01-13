/*
 * JBoss, Home of Professional Open Source
 * Copyright 2012, Red Hat Middleware LLC, and individual contributors
 * by the @authors tag. See the copyright.txt in the distribution for a
 * full listing of individual contributors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * http://www.apache.org/licenses/LICENSE-2.0
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.jboss.arquillian.android.example;

import org.jboss.arquillian.android.test.TestingClass;
import org.jboss.arquillian.container.android.api.AndroidDevice;
import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.container.test.api.OperateOnDeployment;
import org.jboss.arquillian.container.test.api.TargetsContainer;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.arquillian.junit.InSequence;
import org.jboss.arquillian.test.api.ArquillianResource;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.Test;
import org.junit.runner.RunWith;

import junit.framework.Assert;

@RunWith(Arquillian.class)
public class AndroidTestCase {

    @Deployment(name = "android", order = 1, managed = true, testable = false)
    @TargetsContainer("android-managed-1")
    public static JavaArchive createAndroidDeployment1() {
        // simulates installing something at an Android device but does really nothing
        JavaArchive jar = ShrinkWrap.create(JavaArchive.class, "androidTest.jar").addClass(TestingClass.class);
        System.out.println(jar.toString(true));
        return jar;
    }   
   
    @Deployment(name = "jbossas", order = 2, managed = true, testable = false)
    @TargetsContainer("jbossas-managed")
    public static JavaArchive createJBossASDeployment() {
        JavaArchive jar = ShrinkWrap.create(JavaArchive.class, "jbossASTest.jar").addClass(TestingClass.class);
        System.out.println(jar.toString(true));
        return jar;
    }    
    
    @ArquillianResource
    AndroidDevice device;
    
    @Test
    @InSequence(1)
    @OperateOnDeployment("android")
    public void test01() {
        Assert.assertTrue(device != null);
    }
    
    @Test
    @InSequence(2)
    @OperateOnDeployment("jbossas")
    public void test03() {
        Assert.assertTrue(true);
    }
}
