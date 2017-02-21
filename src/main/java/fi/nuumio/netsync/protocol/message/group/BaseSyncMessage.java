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

import fi.nuumio.netsync.util.NodeId;
import fi.nuumio.netsync.util.Token;

public abstract class BaseSyncMessage extends BaseGroupMemberMessage {
    private NodeId mSyncPoint;
    private long mTimeout;

    BaseSyncMessage(final NodeId groupId, final Token groupToken,
                    final Token sourceToken) {
        super(groupId, groupToken, sourceToken);
    }

    BaseSyncMessage() {
        super();
    }

    @Override
    public void get(ByteBuffer buffer) {
        super.get(buffer);
        mTimeout = buffer.getLong();
        if (null == mSyncPoint) {
            mSyncPoint = new NodeId(buffer);
        } else {
            mSyncPoint.get(buffer);
        }
    }

    public NodeId getSyncPoint() {
        return mSyncPoint;
    }

    public void setSyncPoint(final NodeId syncPoint) {
        mSyncPoint = syncPoint;
    }

    public long getTimeout() {
        return mTimeout;
    }

    public void setTimeout(final long timeout) {
        this.mTimeout = timeout;
    }

    @Override
    public int length() {
        return super.length() + (mSyncPoint != null ? mSyncPoint.length() : 0);
    }

    @Override
    public void put(ByteBuffer buffer) {
        super.put(buffer);
        buffer.putLong(mTimeout);
        mSyncPoint.put(buffer);
    }
}
