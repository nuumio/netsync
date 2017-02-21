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

package fi.nuumio.netsync.protocol;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.SocketChannel;
import java.util.HashMap;

import fi.nuumio.netsync.protocol.message.Header;
import fi.nuumio.netsync.protocol.message.Message;
import fi.nuumio.netsync.protocol.message.Packet;
import fi.nuumio.netsync.protocol.message.Request;
import fi.nuumio.netsync.protocol.message.Response;
import fi.nuumio.netsync.util.Constants;
import fi.nuumio.netsync.util.Log;

import static fi.nuumio.netsync.protocol.Messenger.ReadMessageResult.CLOSE;
import static fi.nuumio.netsync.protocol.Messenger.ReadMessageResult.EXHAUSTED;
import static fi.nuumio.netsync.protocol.Messenger.ReadMessageResult.READ_MORE;
import static fi.nuumio.netsync.protocol.Messenger.State.FULLY_READ;
import static fi.nuumio.netsync.protocol.Messenger.State.READING_DATA;
import static fi.nuumio.netsync.protocol.Messenger.State.READING_HEADER;

public class Messenger<M extends Message> {
    private final HashMap<Class<M>, MessageHandler<M>> mHandlers;
    private final HashMap<Integer, Packet<? extends Message>> mPendingRequests;
    private final Header mCurrentHeader;
    private final SocketChannel mChannel;
    private final ByteBuffer mInBuffer;
    private final ByteBuffer mOutBuffer;
    private final PacketFactory<M> mPacketFactory;
    private Packet<M> mCurrentPacket;
    private State mState;

    public Messenger(final SocketChannel channel) {
        if (!channel.isOpen() || !channel.isConnected()) {
            throw new IllegalArgumentException("Messenger's channel must be connected and open");
        }
        mChannel = channel;
        mHandlers = new HashMap<>();
        mPendingRequests = new HashMap<>();
        mInBuffer = ByteBuffer.allocate(Constants.MESSAGE_BUFFER_SIZE);
        mOutBuffer = ByteBuffer.allocate(Constants.MESSAGE_BUFFER_SIZE);
        mState = READING_HEADER;
        mCurrentHeader = new Header();
        mPacketFactory = new PacketFactory<>();
    }

    public void cancel(Packet<? extends Request> packet) {
        mPendingRequests.remove(packet.getSequenceNumber());
    }

    public void close() {
        try {
            mChannel.close();
        } catch (final IOException e) {
            Log.w("Could not close channel when closing messenger", e);
        }
    }

    public String getRemoteAddressString() {
        return mChannel.socket().getRemoteSocketAddress().toString();
    }

    public ReadMessageResult read() throws IOException {
        if (mChannel.read(mInBuffer) <= 0) {
            Log.d("Connection closed: " + getRemoteAddressString());
            return CLOSE;
        }
        ReadMessageResult readResult;
        do {
            readResult = readMessage(mInBuffer);
        } while (READ_MORE == readResult);
        return readResult;
    }

    public void send(Packet<? extends Message> packet) throws IOException {
        if (packet.isRequest()) {
            mPendingRequests.put(packet.getSequenceNumber(), packet);
        }
        mOutBuffer.clear();
        packet.put(mOutBuffer);
        mOutBuffer.flip();
        mChannel.write(mOutBuffer);
        Log.v("Sent: " + packet);
    }

    @SuppressWarnings("unchecked")
    public void setHandler(MessageHandler<? extends Message> messageHandler) {
        mHandlers.put(
                ((MessageHandler<M>) messageHandler).getType(), (MessageHandler<M>) messageHandler);
    }

    @Override
    public String toString() {
        return getClass().getSimpleName() + "@" + Integer.toHexString(hashCode()) + "[" +
                getRemoteAddressString() + "]";
    }

    @SuppressWarnings("unchecked")
    private void handleCurrentMessage() {
        if (mCurrentPacket.isResponse()) {
            final int respSequence = mCurrentPacket.getSequenceNumber();
            if (mPendingRequests.containsKey(respSequence)) {
                final Packet<? extends Request> requestPacket =
                        (Packet<? extends Request>) mPendingRequests.get(respSequence);
                Request<? extends Response> request = requestPacket.getMessage();
                ((Response<Request>) mCurrentPacket.getMessage()).setOriginalRequest(request);
                mPendingRequests.remove(respSequence);
                Log.v("Got request for response " + requestPacket);
            } else {
                Log.e("Dropped response w/o request " + mCurrentPacket);
                return;
            }
        }
        final MessageHandler<M> handler = mHandlers.get(mCurrentPacket.getMessageType());
        if (handler != null) {
            Log.v("Handling message: " + mCurrentPacket);
            handler.handleMessage(mCurrentPacket);
        } else {
            Log.d("Unhandled message scrapped: " + mCurrentPacket);
        }
    }

    private ReadMessageResult readMessage(final ByteBuffer buffer) {
        // Read header
        final State oldState = mState;
        final boolean isFlipped;
        if (READING_HEADER == mState && mCurrentHeader.getHeader(buffer)) {
            Log.v("Got header: " + mCurrentHeader);
            mState = READING_DATA;
            isFlipped = true;
            // NOTE: Header is reused. It's copied to new Packet.
            mCurrentPacket = mPacketFactory.getMessage(mCurrentHeader);
        } else {
            isFlipped = false;
        }

        // Read message
        final boolean gotMessage;
        if (READING_DATA == mState) {
            // Generate message and get
            gotMessage = mCurrentPacket.get(buffer, isFlipped);
            if (gotMessage) {
                mState = FULLY_READ;
            }
        } else {
            gotMessage = false;
        }

        if (gotMessage && mCurrentPacket.hasMessage()) {
            handleCurrentMessage();
        }

        // Compact buffer if we get anything (we only get full "chunks" to state change indicates
        // that something was get)
        if (oldState != mState) {
            buffer.compact();
            // If we got full message next we read header
            if (FULLY_READ == mState) {
                mState = READING_HEADER;
            }
            return ReadMessageResult.READ_MORE;
        }
        return EXHAUSTED;
    }

    public enum ReadMessageResult {
        READ_MORE,
        EXHAUSTED,
        CLOSE
    }

    enum State {
        READING_HEADER,
        READING_DATA,
        FULLY_READ
    }
}
