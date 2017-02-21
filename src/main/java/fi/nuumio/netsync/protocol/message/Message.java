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

import java.nio.ByteBuffer;

import fi.nuumio.netsync.util.Token;

public abstract class Message implements Bufferable {
    private Token mSourceToken;

    protected Message() {
    }

    protected Message(final Token sourceToken) {
        mSourceToken = sourceToken;
    }

    @Override
    public void get(final ByteBuffer buffer) {
        if (null == mSourceToken) {
            mSourceToken = new Token(buffer);
        } else {
            mSourceToken.get(buffer);
        }
    }

    public abstract MessageId getMessageId();

    public abstract int getProtocolVersion();

    public Token getSourceToken() {
        return mSourceToken;
    }

    public void setSourceToken(Token sourceToken) {
        mSourceToken = sourceToken;
    }

    @Override
    public int length() {
        return mSourceToken != null ? mSourceToken.length() : 0;
    }

    @Override
    public void put(final ByteBuffer buffer) {
        mSourceToken.put(buffer);
    }

    @Override
    public String toString() {
        return getClass().getSimpleName() + "@" + Integer.toHexString(hashCode());
    }
}
