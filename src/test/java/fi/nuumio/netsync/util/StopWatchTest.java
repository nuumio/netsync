/*
 * Copyright 2017 Jari Hämäläinen / https://github.com/nuumio
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package fi.nuumio.netsync.util;

import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

// NOTE: These test rely on Thread.sleep(). Mocking System.nanoTime() would require PowerMock.
public class StopWatchTest {
    @Before
    public void setUp() throws Exception {
        Log.setLevel(Log.VERBOSE);
    }

    @Test
    public void elapsed() throws Exception {
        final TimeUtils.StopWatch watch = new TimeUtils.StopWatch();
        final long elapsed1 = watch.elapsed();
        Thread.sleep(2);
        final long elapsed2 = watch.elapsed();
        Thread.sleep(2);
        final long elapsed3 = watch.elapsed();
        assertTrue(elapsed2 > elapsed1);
        assertTrue(elapsed3 > elapsed2);
    }

    @Test
    public void getTimeLeft() throws Exception {
        final TimeUtils.StopWatch watch = new TimeUtils.StopWatch();
        final long timeLeft1 = watch.getTimeLeft(10);
        Thread.sleep(20);
        final long timeLeft2 = watch.getTimeLeft(10);
        assertTrue(timeLeft1 > timeLeft2);
        assertEquals(0, timeLeft2);
    }

    @Test
    public void hasTimeLeft() throws Exception {
        final TimeUtils.StopWatch watch = new TimeUtils.StopWatch();
        final boolean timeLeft1 = watch.hasTimeLeft(10);
        Thread.sleep(20);
        final boolean timeLeft2 = watch.hasTimeLeft(10);
        assertTrue(timeLeft1);
        assertFalse(timeLeft2);
    }
}