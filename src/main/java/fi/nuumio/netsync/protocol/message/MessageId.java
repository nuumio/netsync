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

package fi.nuumio.netsync.protocol.message;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

public enum MessageId {
    REGISTER_REQUEST(1),
    REGISTER_RESPONSE(2),
    GROUP_JOIN_REQUEST(3),
    GROUP_JOIN_RESPONSE(4),
    GROUP_NOTIFY(5),
    GROUP_LEAVE_REQUEST(6),
    GROUP_LEAVE_RESPONSE(7),
    SYNC_REQUEST(8),
    SYNC_RESPONSE(9),
    SYNC_NOTIFY(10);

    static final Map<Integer, MessageId> sIntMapping;

    static {
        HashMap<Integer, MessageId> intMapping = new HashMap<>();
        for (MessageId id : MessageId.values()) {
            intMapping.put(id.getId(), id);
        }
        sIntMapping = Collections.unmodifiableMap(intMapping);
    }

    private final int mId;

    MessageId(final int id) {
        mId = id;
    }

    public static MessageId valueOf(final int value) {
        return sIntMapping.get(value);
    }

    public int getId() {
        return mId;
    }
}
