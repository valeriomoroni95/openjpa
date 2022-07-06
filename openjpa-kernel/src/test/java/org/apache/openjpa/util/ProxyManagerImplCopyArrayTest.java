/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package org.apache.openjpa.util;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import java.util.*;

//Return a new array of the same component type as the given array and containing the same elements.

@RunWith(Parameterized.class)
public class ProxyManagerImplCopyArrayTest {

    //Class instance
    private ProxyManagerImpl proxyManager;

    //Test parameter
    private Object orig; //the original method parameter
    private Class<? extends Exception> expectedException; //expected Exception of any type



    public ProxyManagerImplCopyArrayTest(Object orig, Class<? extends Exception> expectedException) {
        configure(orig, expectedException);
    }


    //Parameters instantiation
    public void configure(Object orig, Class<? extends Exception> expectedException){
        this.orig = orig;
        this.expectedException = expectedException;
    }


    @Before
    public void setUp(){
        //Create a proxyManager instance
        this.proxyManager = new ProxyManagerImpl();

    }


    //Parameters association
    @Parameterized.Parameters
    public static Collection<Object[]> getParameters(){

        //Get a random generator instance
        Random randomInstance = new Random();

        //Instantiate a nonBean class with two random int values
        NonBeanClass nonBean = new NonBeanClass(randomInstance.nextInt(), randomInstance.nextInt());

        //Create a new list of integer with 4 random values
        Integer[] list = new Integer[]{randomInstance.nextInt(), randomInstance.nextInt(),
                randomInstance.nextInt(), randomInstance.nextInt()};

        return Arrays.asList(new Object[][]{

                //orig                   expectedException
                {null,                                null},
                {nonBean,       UnsupportedException.class},
                {list,                                null}
        });

    }


    @Test
    public void copyCustomTest() {

        Object outcome;
        try{
            //Check the return value of the tested method, passing each time the orig value in test parameters
            outcome = this.proxyManager.copyArray(this.orig);
            //I'm expecting outcome to be exactly equal to orig if UnsupportedException is not raised
            Assert.assertArrayEquals((Object[]) this.orig, (Object[]) outcome);
            System.out.println("the two values are: " +this.orig +" and "+outcome);
        }catch (UnsupportedException e){
            //Otherwise I'm expecting exactly this exception.
            System.out.println("expectedException value is: " +this.expectedException);
            Assert.assertEquals(this.expectedException, e.getClass());
        }

    }

    //Generic NonBeanClass, without getter and setter but only a simple constructor
    // used to create a not valid Instance for the tested method.
    public static class NonBeanClass {
        Integer value;
        Integer value2;

        public NonBeanClass(Integer value, Integer value2) {
            this.value = value;
            this.value2 = value2;
        }
    }

}
