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
import fi.nuumio.netsync.protocol.message.Response;
import fi.nuumio.netsync.util.Constants;
import fi.nuumio.netsync.util.NodeId;
import fi.nuumio.netsync.util.Token;

public class SyncResponse extends BaseSyncMessage implements Response<SyncRequest> {
    private final StringBuilder mSb;
    private Code mCode;
    private SyncRequest mRequest;

    public SyncResponse() {
        // Used via reflection
        super();
        mRequest = null;
        mSb = new StringBuilder();
    }

    SyncResponse(final NodeId groupId, final Token groupToken, final Token sourceToken,
                 final NodeId syncPoint) {
        super(groupId, groupToken, sourceToken);
        setSyncPoint(syncPoint);
        mSb = new StringBuilder();
    }

    @Override
    public void get(ByteBuffer buffer) {
        super.get(buffer);
        mCode = Code.valueOf(buffer.getInt());
    }

    public Code getCode() {
        return mCode;
    }

    public void setCode(final Code code) {
        mCode = code;
    }

    @Override
    public MessageId getMessageId() {
        return MessageId.SYNC_RESPONSE;
    }

    @Override
    public SyncRequest getOriginalRequest() {
        return mRequest;
    }

    @Override
    public void setOriginalRequest(final SyncRequest request) {
        mRequest = request;
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
    public void put(ByteBuffer buffer) {
        super.put(buffer);
        buffer.putInt(mCode.getCode());
    }

    @Override
    public String toString() {
        synchronized (mSb) {
            NodeId[] members = getMembers();
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
                    mCode + " / " + mSb.toString() + "]";
        }
    }

    public enum Code {
        CREATED(0),
        JOINED(1),
        EXPIRED(2),
        FAIL_AUTHENTICATION_FAILURE(3);

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
