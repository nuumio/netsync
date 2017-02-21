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

import java.nio.ByteBuffer;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import fi.nuumio.netsync.protocol.message.MessageId;
import fi.nuumio.netsync.util.Constants;
import fi.nuumio.netsync.util.NodeId;

public class SyncNotify extends GroupNotify {
    private NodeId mSyncPoint;
    private Code mCode;

    public SyncNotify() {
        // Used via reflection
        super();
        mCode = null;
    }

    @Override
    public void get(ByteBuffer buffer) {
        super.get(buffer);
        if (null == mSyncPoint) {
            mSyncPoint = new NodeId(buffer);
        } else {
            mSyncPoint.get(buffer);
        }
    }

    @Override
    public MessageId getMessageId() {
        return MessageId.SYNC_NOTIFY;
    }

    @Override
    public int getProtocolVersion() {
        return Constants.PROTOCOL_VERSION_1;
    }

    public Code getSyncCode() {
        return mCode;
    }

    public void setSyncCode(final Code code) {
        mCode = code;
    }

    public NodeId getSyncPoint() {
        return mSyncPoint;
    }

    public void setSyncPoint(NodeId syncPoint) {
        mSyncPoint = syncPoint;
    }

    @Override
    public int length() {
        return super.length() + (mSyncPoint != null ? mSyncPoint.length() : 0);
    }

    @Override
    public void put(ByteBuffer buffer) {
        super.put(buffer);
        mSyncPoint.put(buffer);
    }

    @Override
    String codeToString() {
        return "" + mCode;
    }

    @Override
    void getCode(final ByteBuffer buffer) {
        mCode = Code.valueOf(buffer.getInt());
    }

    @Override
    void putCode(final ByteBuffer buffer) {
        buffer.putInt(mCode.getCode());
    }

    public enum Code {
        SUCCESS(0),
        JOIN(1),
        LEAVE(2),
        TIMEOUT(3);

        static final Map<Integer, Code> sIntMapping;

        static {
            HashMap<Integer, Code> intMapping = new HashMap<>();
            for (Code result : Code.values()) {
                intMapping.put(result.getCode(), result);
            }
            sIntMapping = Collections.unmodifiableMap(intMapping);
        }

        final int mCode;

        Code(final int code) {
            mCode = code;
        }

        public static Code valueOf(final int code) {
            return sIntMapping.get(code);
        }

        public int getCode() {
            return mCode;
        }
    }
}
