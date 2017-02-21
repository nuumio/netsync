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

package fi.nuumio.netsync.protocol.message.group;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

public enum SyncType {
    /**
     * Wait until all SyncGroup members arrive to same sync point
     */
    ALL(0);

    static final Map<Integer, SyncType> sIntMapping;

    static {
        HashMap<Integer, SyncType> intMapping = new HashMap<>();
        for (SyncType result : SyncType.values()) {
            intMapping.put(result.getCode(), result);
        }
        sIntMapping = Collections.unmodifiableMap(intMapping);
    }

    final int mCode;

    SyncType(final int code) {
        mCode = code;
    }

    public static SyncType valueOf(final int code) {
        return sIntMapping.get(code);
    }

    public int getCode() {
        return mCode;
    }
}
