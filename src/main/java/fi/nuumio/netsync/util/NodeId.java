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

import java.nio.ByteBuffer;

import fi.nuumio.netsync.protocol.message.Bufferable;

/**
 * Id (name) of for a node in system (SyncClient, SyncGroup).
 */
public class NodeId implements Bufferable, Comparable<NodeId> {
    private byte[] mBytes = null;
    private String mIdString = null;

    public NodeId(final String id) {
        final byte[] idBytes = StringUtil.getBytes(id);
        if (idBytes.length < 1 || idBytes.length > Constants.NODE_ID_SIZE) {
            throw new IllegalArgumentException(
                    "Id size in bytes must be [1," + Constants.NODE_ID_SIZE + "]");
        }
        mIdString = id;
        mBytes = idBytes;
    }

    public NodeId(final ByteBuffer buffer) {
        get(buffer);
    }

    public String asString() {
        if (null == mIdString) {
            mIdString = StringUtil.fromBytes(mBytes);
        }
        return mIdString;
    }

    @Override
    public int compareTo(final NodeId other) {
        return asString().compareTo(other.asString());
    }

    @Override
    public boolean equals(final Object obj) {
        return obj instanceof NodeId && asString().equals(((NodeId) obj).asString());
    }

    @Override
    public void get(final ByteBuffer buffer) {
        final int length = buffer.getInt();
        mBytes = new byte[length];
        buffer.get(mBytes, 0, length);
    }

    @Override
    public int hashCode() {
        return asString().hashCode();
    }

    @Override
    public int length() {
        return Constants.INT_BYTES + (mBytes != null ? mBytes.length : 0);
    }

    @Override
    public void put(final ByteBuffer buffer) {
        buffer.putInt(mBytes.length);
        buffer.put(mBytes, 0, mBytes.length);
    }

    @Override
    public String toString() {
        return getClass().getSimpleName() + "@" + Integer.toHexString(hashCode()) + "[" +
                asString() + "]";
    }
}
