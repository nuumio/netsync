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

import java.util.HashMap;

public class LoggableThread extends Thread {
    private static HashMap<Long, String> sNames = new HashMap<>();

    public LoggableThread(final Runnable runnable) {
        super(runnable);
    }

    public static String getName(final long threadId) {
        synchronized (sNames) {
            if (sNames.containsKey(threadId)) {
                return sNames.get(threadId);
            } else {
                return "Thread-" + String.valueOf(threadId);
            }
        }
    }

    public void cleanUp() {
        removeName();
    }

    public void setLoggableName(final String name) {
        super.setName(name);
        storeName();
    }

    private void removeName() {
        synchronized (sNames) {
            sNames.remove(getId());
        }
    }

    private void storeName() {
        synchronized (sNames) {
            sNames.put(getId(), getName());
        }
    }
}
