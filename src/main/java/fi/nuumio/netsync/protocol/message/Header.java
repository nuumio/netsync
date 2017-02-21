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
import java.util.Collections;
import java.util.EnumMap;
import java.util.Map;

import fi.nuumio.netsync.util.Constants;
import fi.nuumio.netsync.util.RandomHelper;

public class Header implements Bufferable {
    private static final int PROTOCOL_VERSION_NOT_SET = Integer.MIN_VALUE;
    private static final int SEQN_NOT_SET = Integer.MIN_VALUE;
    private static final int SEQN_UPDATE_FAIL_RANDOM = -1;
    private static final int SEQN_UPDATE_FAIL_NO_GENERATOR = -2;
    private static final int SEQN_MESSAGE_NUMBER_START = 1000;
    private static final int SEQN_MESSAGE_NUMBER_RANGE = 100;
    private static final int SEQN_MESSAGE_NUMBER_GAP = 100;
    private static final Map<MessageId, SequenceNumberGenerator> sSeqGenerators;

    static {
        EnumMap<MessageId, SequenceNumberGenerator> seqGenerators = new EnumMap<>(MessageId.class);
        int min = SEQN_MESSAGE_NUMBER_START;
        for (final MessageId id : MessageId.values()) {
            seqGenerators.put(id, new SequenceNumberGenerator(min, min + SEQN_MESSAGE_NUMBER_RANGE));
            min += SEQN_MESSAGE_NUMBER_RANGE + SEQN_MESSAGE_NUMBER_GAP;
        }
        sSeqGenerators = Collections.unmodifiableMap(seqGenerators);
    }

    private int mProtocolVersion;
    private int mFullPacketLength;
    private int mSequenceNumber;
    private MessageId mMessageId;

    public Header() {
        mProtocolVersion = PROTOCOL_VERSION_NOT_SET;
        mFullPacketLength = 0;
        mSequenceNumber = SEQN_NOT_SET;
        mMessageId = null;
    }

    Header(final Header header) {
        mProtocolVersion = header.mProtocolVersion;
        mFullPacketLength = header.mFullPacketLength;
        mSequenceNumber = header.mSequenceNumber;
        mMessageId = header.mMessageId;
    }

    @Override
    public void get(final ByteBuffer buffer) {
        mProtocolVersion = buffer.getInt();
        mFullPacketLength = buffer.getInt();
        mSequenceNumber = buffer.getInt();
        mMessageId = MessageId.valueOf(buffer.getInt());
    }

    public boolean getHeader(final ByteBuffer buffer) {
        if (buffer.position() < length()) {
            return false;
        }
        buffer.flip();
        get(buffer);
        return true;
    }

    public MessageId getMessageId() {
        return mMessageId;
    }

    void setMessageId(final MessageId messageId) {
        mMessageId = messageId;
        updateSequenceNumber();
    }

    @Override
    public int length() {
        return Constants.INT_BYTES * 4;
    }

    @Override
    public void put(final ByteBuffer buffer) {
        buffer.putInt(mProtocolVersion);
        buffer.putInt(mFullPacketLength);
        buffer.putInt(mSequenceNumber);
        buffer.putInt(mMessageId.getId());
    }

    @Override
    public String toString() {
        return getClass().getSimpleName() + "@" + Integer.toHexString(hashCode()) + "[" +
                "v=" + mProtocolVersion +
                ",l=" + mFullPacketLength +
                ",s=" + mSequenceNumber +
                ",m=" + mMessageId + "]";
    }

    int getMessageLength() {
        return mFullPacketLength - length();
    }

    void setMessageLength(final int messageLength) {
        mFullPacketLength = messageLength + length();
    }

    int getProtocolVersion() {
        return mProtocolVersion;
    }

    void setProtocolVersion(final int protocolVersion) {
        mProtocolVersion = protocolVersion;
    }

    int getSequenceNumber() {
        return mSequenceNumber;
    }

    void setAsResponseTo(final Header requestHeader) {
        this.mSequenceNumber = requestHeader.mSequenceNumber;
    }

    private void updateSequenceNumber() {
        final SequenceNumberGenerator generator = sSeqGenerators.get(mMessageId);
        if (generator != null) {
            mSequenceNumber = generator.next();
        } else {
            mSequenceNumber = SEQN_UPDATE_FAIL_NO_GENERATOR;
        }
    }

    private static class SequenceNumberGenerator {
        private static final int MAX_ROUNDS = 20;
        private final int mMin;
        private final int mRange;
        private int mLast = Integer.MIN_VALUE;

        private SequenceNumberGenerator(final int min, final int max) {
            if (min < 0 || max <= min) {
                throw new IllegalArgumentException("min must be >= 0, max must be > min");
            }
            mMin = min;
            mRange = max - min;
        }

        private int next() {
            int round = 0;
            int next;
            do {
                next = mMin + RandomHelper.nextInt(mRange);
                round++;
            } while (next == mLast && round < MAX_ROUNDS);
            if (next == mLast) {
                return SEQN_UPDATE_FAIL_RANDOM;
            }
            mLast = next;
            return next;
        }
    }
}
