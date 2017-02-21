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

import fi.nuumio.netsync.protocol.message.Message;
import fi.nuumio.netsync.util.NodeId;
import fi.nuumio.netsync.util.Token;

public abstract class BaseGroupMessage extends Message {
    private NodeId mGroupId;
    private Token mGroupToken;

    BaseGroupMessage() {
        super();
    }

    BaseGroupMessage(final NodeId groupId, final Token groupToken, final Token sourceToken) {
        super(sourceToken);
        mGroupId = groupId;
        mGroupToken = groupToken;
    }

    @Override
    public void get(final ByteBuffer buffer) {
        super.get(buffer);
        if (null == mGroupId) {
            mGroupId = new NodeId(buffer);
        } else {
            mGroupId.get(buffer);
        }
        if (null == mGroupToken) {
            mGroupToken = new Token(buffer);
        } else {
            mGroupToken.get(buffer);
        }
    }

    public NodeId getGroupId() {
        return mGroupId;
    }

    public void setGroupId(NodeId mGroupId) {
        this.mGroupId = mGroupId;
    }

    public Token getGroupToken() {
        return mGroupToken;
    }

    public void setGroupToken(Token mGroupToken) {
        this.mGroupToken = mGroupToken;
    }

    @Override
    public int length() {
        return super.length() +
                (mGroupToken != null ? mGroupToken.length() : 0) +
                (mGroupId != null ? mGroupId.length() : 0);
    }

    @Override
    public void put(final ByteBuffer buffer) {
        super.put(buffer);
        mGroupId.put(buffer);
        mGroupToken.put(buffer);
    }
}
