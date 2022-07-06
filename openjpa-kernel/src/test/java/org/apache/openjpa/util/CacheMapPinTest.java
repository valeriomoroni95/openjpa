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
import java.util.Arrays;
import java.util.Collection;

import static org.mockito.Mockito.*;

/*
CacheMap maintains
a fixed number of cache entries, and an
optional soft reference map for entries
that are moved out of the LRU space. So,
for applications that have a monotonically
increasing number of distinct queries, this
option can be used to ensure that a fixed
amount of memory is used by the cache.
*/

@RunWith(Parameterized.class)
public class CacheMapPinTest {

    //CacheMap instance
    private CacheMap cacheMap;
    //test attributes
    private Object key; //object key
    private Object value;//object value
    private boolean hasPreviousValue; //boolean which tells if there was a previous value
    private boolean pinned; //if it is pinned or not
    private boolean expectedResult;


    public CacheMapPinTest(Object key, Object value, boolean alreadyExist, boolean pinned, boolean expectedResult) {
        configure(key, value, alreadyExist, pinned, expectedResult);
    }

    //Parameters instantiation
    public void configure(Object key, Object value, boolean alreadyExist, boolean pinned, boolean expectedResult){
        this.key = key;
        this.hasPreviousValue = alreadyExist;
        this.pinned = pinned;
        this.expectedResult = expectedResult;
        //if there's a previous value, make it the actual value
        if (this.hasPreviousValue) {
            this.value = value;
        } else {
            //Otherwise set it to null
            this.value = null;
        }
    }


    //Setup the environment: it determines the test result
    @Before
    public void setUp(){
        //Create a new CacheMap with LRU (last recently used)
        this.cacheMap = new CacheMap(true);

        //If there is a previous value
        if (this.hasPreviousValue) {
            //Put this value in the cache map
            this.cacheMap.put(this.key, this.value);
        }
        //If pinned == true
        if (this.pinned) {
            //Pin key into the cacheMap
            this.cacheMap.pin(this.key);
        }
        //Spy on the real object
        this.cacheMap = spy(this.cacheMap);
    }


    //Parameters association
    @Parameterized.Parameters
    public static Collection<Object[]> getTestParameters(){
        return Arrays.asList(new Object[][]{

                //key            value      alreadyExist     pinned      expectedOutcome
                {null,           null,         false,        false,           false},
                {new Object(),   new Object(),  true,        false,            true},
                {new Object(),   null,         false,         true,           false},
                {new Object(),   new Object(),  true,          true,           true},
        });
    }


    @Test
    public void pinTest() {
        boolean result = this.cacheMap.pin(this.key);
        //Verify the spied object has called writeLock and writeUnlock
        verify(this.cacheMap).writeLock();
        verify(this.cacheMap).writeUnlock();

        Assert.assertEquals(this.expectedResult, result);

    }

}

