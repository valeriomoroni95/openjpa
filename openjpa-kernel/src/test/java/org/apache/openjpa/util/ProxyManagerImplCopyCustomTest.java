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

/*The default proxy manager can proxy the standard methods of any Collection, List, Map, Queue, Date, or Calendar class,
 including custom implementations. It can also proxy custom classes whose accessor and mutator methods follow JavaBean
 naming conventions. Your custom types must, however, meet the following criteria:

- Custom container types must have a public no-arg constructor or a public constructor that takes a single Comparator
  parameter.
- Custom date types must have a public no-arg constructor or a public constructor that takes a single long parameter
  representing the current time.
- Other custom types must have a public no-arg constructor or a public copy constructor. If a custom types does not
  have a copy constructor, it must be possible to fully copy an instance A by creating a new instance B and calling
  each of B's setters with the value from the corresponding getter on A.*/


//Return a copy of the given object with the same information,
// or null if this manager cannot copy the object.

@RunWith(Parameterized.class)
public class ProxyManagerImplCopyCustomTest {

    //Class instance
    private ProxyManagerImpl proxyManager;

    //Test parameters
    private Object orig; //Method original parameter
    private boolean nullOutcome; //To check the result



    public ProxyManagerImplCopyCustomTest(Object orig, boolean nullOutcome) {

        configure(orig, nullOutcome);
    }

    //Parameters instantiation
    public void configure(Object object, boolean nullOutcome){
        this.orig = object;
        this.nullOutcome = nullOutcome;
    }

    //Parameters association
    @Parameterized.Parameters
    public static Collection<Object[]> getTestParameters(){

        //Get a random generator instance
        Random randomInstance = new Random();

        //Create a new NonBeanClass with two random values
        NonBeanClass nonBean = new NonBeanClass(randomInstance.nextInt(), randomInstance.nextInt());

        //Create a new BeanClass and set in a random value
        BeanClass beanClass = new BeanClass();
        beanClass.setValue(randomInstance.nextInt());

        //Create a new map and put in 3 random key values pairs
        Map<Integer, Integer> map = new HashMap<>();
        map.put(randomInstance.nextInt(), randomInstance.nextInt());
        map.put(randomInstance.nextInt(), randomInstance.nextInt());
        map.put(randomInstance.nextInt(), randomInstance.nextInt());

        //Create a newDateProxy
        Proxy proxy = new ProxyManagerImpl().newDateProxy(Date.class);

        //Create a new list of integers and add in 3 random integers
        List<Integer> list = new ArrayList<>();
        list.add(randomInstance.nextInt());
        list.add(randomInstance.nextInt());
        list.add(randomInstance.nextInt());

        return Arrays.asList(new Object[][]{

                //Orig                          nullOutcome
                {null,                           true},
                {nonBean,                        true},
                {beanClass,                      false},
                {map,                            false},
                {new Date(),                     false},
                {new GregorianCalendar(),        false},
                {proxy,                          false},
                {list,                           false}
        });
    }


    @Before
    public void setUp(){

        //Create a proxyManager instance
        this.proxyManager = new ProxyManagerImpl();
    }


    @Test
    public void copyCustomTest() {

        //Check the return value of the tested method, passing each time the orig value in test parameters
        Object outcome = this.proxyManager.copyCustom(this.orig);

        //If I'm expecting a null outcome
        if (this.nullOutcome) {
            Assert.assertNull(outcome);
        } else {
            //Otherwise check that orig is equal to his return value
            Assert.assertEquals(this.orig, outcome);
            System.out.println("These two elements are exactly the same: "+this.orig+" and "+outcome);
        }

    }

    //Creating a generic BeanClass with getter and setter
    public static class BeanClass {
        Integer value;


        public BeanClass() {

        }

        //BeanClass getter
        public Integer getValue() {

            return value;
        }

        //BeanClass setter
        public void setValue(Integer value) {

            this.value = value;
        }

        //Needed for assert condition for the BeanClass
        @Override
        public boolean equals(Object obj) {
            try {
                BeanClass bean = (BeanClass) obj;
                return this.value.equals(bean.getValue());
            } catch (Exception e) {
                return false;
            }
        }
    }


    //Generic NonBeanClass, without getter and setter but only a simple constructor
    public static class NonBeanClass {
        Integer value;
        Integer value2;

        public NonBeanClass(Integer value, Integer value2) {
            this.value = value;
            this.value2 = value2;
        }

    }

}
