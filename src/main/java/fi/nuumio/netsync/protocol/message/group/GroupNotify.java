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

public class GroupNotify extends BaseGroupMemberMessage {
    private final StringBuilder mSb;
    private Code mCode;

    public GroupNotify() {
        // Used via reflection
        super();
        mCode = null;
        mSb = new StringBuilder();
    }

    @Override
    public void get(final ByteBuffer buffer) {
        super.get(buffer);
        getCode(buffer);
    }

    @Override
    public MessageId getMessageId() {
        return MessageId.GROUP_NOTIFY;
    }

    @Override
    public int getProtocolVersion() {
        return Constants.PROTOCOL_VERSION_1;
    }

    @Override
    public int length() {
        return super.length() + Constants.INT_BYTES;
    }

    @Override
    public void put(final ByteBuffer buffer) {
        super.put(buffer);
        putCode(buffer);
    }

    public void setNotifyCode(final Code code) {
        mCode = code;
    }

    @Override
    public String toString() {
        final NodeId[] members = getMembers();
        synchronized (mSb) {
            mSb.setLength(0);
            if (members != null) {
                mSb.append("[");
                for (int i = 0; i < members.length; i++) {
                    if (i > 0) {
                        mSb.append(",");
                    }
                    mSb.append(members[i].asString());
                }
                mSb.append("]");
            } else {
                mSb.append("[null]");
            }
            return getClass().getSimpleName() + "@" + Integer.toHexString(hashCode()) + "[" +
                    codeToString() + "/" + mSb.toString() + "]";
        }
    }

    String codeToString() {
        return "" + mCode;
    }

    void getCode(final ByteBuffer buffer) {
        mCode = Code.valueOf(buffer.getInt());
    }

    void putCode(final ByteBuffer buffer) {
        buffer.putInt(mCode.getCode());
    }

    public enum Code {
        JOIN(0),
        LEAVE(1),
        CLOSE(2);

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
