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

import fi.nuumio.netsync.util.Constants;
import fi.nuumio.netsync.util.NodeId;
import fi.nuumio.netsync.util.Token;

public abstract class BaseGroupMemberMessage extends BaseGroupMessage {
    private final StringBuilder mSb;
    private NodeId[] mMembers;

    BaseGroupMemberMessage() {
        // Used via reflection
        super();
        mSb = new StringBuilder();
    }

    BaseGroupMemberMessage(final NodeId groupId, final Token groupToken, final Token sourceToken) {
        super(groupId, groupToken, sourceToken);
        mSb = new StringBuilder();
    }

    @Override
    public void get(ByteBuffer buffer) {
        super.get(buffer);
        final int memberCount = buffer.getInt();
        if (null == mMembers || mMembers.length != memberCount) {
            mMembers = new NodeId[memberCount];
        }
        for (int i = 0; i < mMembers.length; i++) {
            if (mMembers[i] != null) {
                mMembers[i].get(buffer);
            } else {
                mMembers[i] = new NodeId(buffer);
            }
        }
    }

    public NodeId[] getMembers() {
        if (mMembers == null) {
            mMembers = new NodeId[0];
        }
        return mMembers;
    }

    public void setMembers(NodeId[] members) {
        mMembers = members;
    }

    @Override
    public int length() {
        return super.length() + Constants.INT_BYTES + getMemberLength();
    }

    @Override
    public void put(ByteBuffer buffer) {
        super.put(buffer);
        if (mMembers != null) {
            buffer.putInt(mMembers.length);
            for (final NodeId id : mMembers) {
                id.put(buffer);
            }
        } else {
            buffer.putInt(0);
        }
    }

    @Override
    public String toString() {
        synchronized (mSb) {
            mSb.setLength(0);
            if (mMembers != null) {
                mSb.append("[");
                for (int i = 0; i < mMembers.length; i++) {
                    if (i > 0) {
                        mSb.append(",");
                    }
                    mSb.append(mMembers[i].asString());
                }
                mSb.append("]");
            } else {
                mSb.append("[null]");
            }
            return getClass().getSimpleName() + "@" + Integer.toHexString(hashCode()) + "[" +
                    mSb.toString() + "]";
        }
    }

    private int getMemberLength() {
        if (mMembers != null) {
            int length = 0;
            for (final NodeId id : mMembers) {
                length += id.length();
            }
            return length;
        } else {
            return 0;
        }
    }
}
