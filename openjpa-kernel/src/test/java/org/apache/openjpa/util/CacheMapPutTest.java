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
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoRule;
import java.util.Arrays;
import java.util.Collection;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
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
public class CacheMapPutTest {

    //CacheMap instance
    private CacheMap cacheMap;

    //Test parameters
    private Object key; //map key
    private Object value; //map value
    private Object previousValue; //the actual previous value
    private boolean hasPreviousValue; //if there's a previous value
    private boolean pinned; //If it's pinned or not
    private Integer cachedMaxMapSize; //the max size of the cached map
    private Integer numObjectToInsert; //the number of objects to insert

    @Rule
    public MockitoRule mockitoRule = MockitoJUnit.rule();


    public CacheMapPutTest(Object key,Object value, boolean alreadyExist, boolean pinned, Integer cachedMaxMapSize, Integer numObjectToInsert) {
        configure(key, value, alreadyExist, pinned, cachedMaxMapSize, numObjectToInsert);
    }

    //parameters instantiation
    public void configure(Object key,Object value, boolean alreadyExist, boolean pinned, Integer cacheMaxMapSize, Integer numObjectToInsert){
        this.key = key;
        this.value = value;
        this.hasPreviousValue = alreadyExist;
        this.pinned = pinned;
        this.cachedMaxMapSize = cacheMaxMapSize;
        this.numObjectToInsert = numObjectToInsert;

        //if there's a previous value, create a new object and assign it to previousValue
        if (this.hasPreviousValue) {
            this.previousValue = new Object();
        } else {
            //Otherwise set it to null
            this.previousValue = null;
        }

    }

    //Parameters association
    @Parameterized.Parameters
    public static Collection<Object[]> getTestParameters() {
        return Arrays.asList(new Object[][]{

                //key          value       alreadyExist      pinned       cachedMaxMapSize     numObjectToInsert
                {null,         null,           false,        false,             0,                     1},
                {new Object(), null,           false,         true,             1,                     2},
                {new Object(), new Object(),   false,        false,             1,                     1},
                {new Object(), new Object(),    true,        false,             2,                     0},
                {new Object(), new Object(),    true,        false,             1,                     1},
                {new Object(), new Object(),    true,         true,             1,                     1},


        });
    }


    //Setting up the environment
    @Before
    public void setUp(){

        //Create a new LRU CacheMap, with the given properties.
        this.cacheMap = new CacheMap(true, this.cachedMaxMapSize, this.cachedMaxMapSize + 1, 1L, 1);
        //If hasPreviousValue is true
        if (this.hasPreviousValue) {
            //Put in the cacheMap the previous value associated with key
            this.cacheMap.put(this.key, this.previousValue);
        }
        //Put in the cacheMap as many object as indicated in numObjectToInsert
        for (int i = 0; i < this.numObjectToInsert; i++) {
            this.cacheMap.put(new Object(), new Object());
        }
        //If pinned is set to true
        if (this.pinned) {
            //Pin this key in the cacheMap
            this.cacheMap.pin(this.key);
        }
        //After that, spy cacheMap behavior
        this.cacheMap = spy(this.cacheMap);

    }

    @Test
    public void putTest() {
        //Put in the cacheMap key and value
        Object previousValue = this.cacheMap.put(this.key, this.value);
        //Verify the spied object is calling the real methods, once put the pair key value
        verify(this.cacheMap).writeLock();
        verify(this.cacheMap).writeUnlock();

        //If there were a previous value and this cacheMap size is != 0
        if (this.hasPreviousValue && this.cachedMaxMapSize != 0) {
            //the two values should be equals
            Assert.assertEquals(this.previousValue, previousValue);
            System.out.println("same values: "+this.previousValue+ " and "+previousValue);
        } else {
            //Otherwise, previousValue should be null
            Assert.assertNull(previousValue);

        }
        //Get the value mapped with the key
        Object returnedValue = this.cacheMap.get(this.key);
        //These two values should be the same
        Assert.assertEquals(this.value, returnedValue);

        System.out.println("These two values are the same: "+this.value+ " and "+returnedValue);

    }

}
