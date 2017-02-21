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

public class TimeUtils {
    private static final long NANOS_TO_MILLIS = 1000000L;

    private TimeUtils() {
        // Not to be constructed
    }

    public static long msTime() {
        return System.nanoTime() / NANOS_TO_MILLIS;
    }

    public static class StopWatch {
        private final long mStart = msTime();

        public long elapsed() {
            return msTime() - mStart;
        }

        public long getTimeLeft(final long timeout) {
            return Math.max(0, timeout - elapsed());
        }

        public boolean hasTimeLeft(final long timeout) {
            return getTimeLeft(timeout) > 0;
        }
    }
}
