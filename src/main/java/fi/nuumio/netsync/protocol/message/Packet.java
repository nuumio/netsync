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

import fi.nuumio.netsync.util.Constants;
import fi.nuumio.netsync.util.Log;
import fi.nuumio.netsync.util.Token;

public class Packet<M extends Message> {
    private final Header mHeader;
    private M mMessage;

    public Packet(final Header header, final M message) {
        mHeader = new Header(header);
        mMessage = message;
    }

    private Packet(final M message) {
        mHeader = new Header();
        mMessage = message;
        mHeader.setProtocolVersion(mMessage.getProtocolVersion());
        mHeader.setMessageId(mMessage.getMessageId());
        mHeader.setMessageLength(mMessage.length());
    }

    private Packet(final Class<M> type) {
        mHeader = new Header();
        try {
            mMessage = type.newInstance();
        } catch (final InstantiationException | IllegalAccessException e) {
            Log.wtf("Cannot create message of type " + type);
            throw new WrongMessageTypeException("Cannot create message of type " + type, e);
        }
        mHeader.setProtocolVersion(mMessage.getProtocolVersion());
        mHeader.setMessageId(mMessage.getMessageId());
        mHeader.setMessageLength(mMessage.length());
    }

    public Packet(final Class<M> type, final Token sourceToken) {
        this(type);
        getMessage().setSourceToken(sourceToken);
    }

    @SuppressWarnings("unchecked")
    public <R extends Message & Request> Packet(final Packet<R> request,
                                                final Token sourceToken) {
        this((M) request.getMessage().createResponse(sourceToken));
        mHeader.setAsResponseTo(request.mHeader);
    }

    public boolean get(final ByteBuffer buffer, final boolean isFlipped) {
        final int bytesAvailable;
        if (isFlipped) {
            bytesAvailable = buffer.limit() - buffer.position();
        } else {
            bytesAvailable = buffer.position();
        }
        if (bytesAvailable >= mHeader.getMessageLength()) {
            if (!isFlipped) {
                buffer.flip();
            }
            if (isSupportedProtocolVersion(mHeader.getProtocolVersion())) {
                mMessage.get(buffer);
            } else {
                Log.d("Message " + mHeader.getMessageId() +
                        " skipped because of unsupported protocol version " +
                        mHeader.getProtocolVersion());
                buffer.position(buffer.position() + mHeader.getMessageLength());
                mMessage = null;
            }
            return true;
        }
        return false;
    }

    public M getMessage() {
        return mMessage;
    }

    @SuppressWarnings("unchecked")
    public Class<M> getMessageType() {
        return (Class<M>) mMessage.getClass();
    }

    public int getSequenceNumber() {
        return mHeader.getSequenceNumber();
    }

    public boolean hasMessage() {
        return mMessage != null;
    }

    public boolean isRequest() {
        return mMessage instanceof Request;
    }

    public boolean isResponse() {
        return mMessage instanceof Response;
    }

    public void put(final ByteBuffer buffer) {
        mHeader.setMessageLength(mMessage.length());
        mHeader.put(buffer);
        mMessage.put(buffer);
    }

    @Override
    public String toString() {
        return getClass().getSimpleName() + "@" + Integer.toHexString(hashCode()) +
                "[" + mHeader + "][" + mMessage + "]";
    }

    private boolean isSupportedProtocolVersion(final int protocolVersion) {
        return protocolVersion == Constants.PROTOCOL_VERSION_1;
    }
}
