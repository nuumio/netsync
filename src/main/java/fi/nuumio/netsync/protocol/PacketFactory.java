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

import java.util.Collections;
import java.util.EnumMap;
import java.util.Map;

import fi.nuumio.netsync.protocol.message.Header;
import fi.nuumio.netsync.protocol.message.Message;
import fi.nuumio.netsync.protocol.message.MessageId;
import fi.nuumio.netsync.protocol.message.Packet;
import fi.nuumio.netsync.protocol.message.group.GroupNotify;
import fi.nuumio.netsync.protocol.message.group.JoinRequest;
import fi.nuumio.netsync.protocol.message.group.JoinResponse;
import fi.nuumio.netsync.protocol.message.group.LeaveRequest;
import fi.nuumio.netsync.protocol.message.group.LeaveResponse;
import fi.nuumio.netsync.protocol.message.group.SyncNotify;
import fi.nuumio.netsync.protocol.message.group.SyncRequest;
import fi.nuumio.netsync.protocol.message.group.SyncResponse;
import fi.nuumio.netsync.protocol.message.service.RegisterRequest;
import fi.nuumio.netsync.protocol.message.service.RegisterResponse;
import fi.nuumio.netsync.util.Log;

class PacketFactory<T extends Message> {
    private static final Map<MessageId, Class<? extends Message>> mMessageMap;

    static {
        MessageMap messageMap = new MessageMap();
        try {
            messageMap.put(RegisterRequest.class);
            messageMap.put(RegisterResponse.class);
            messageMap.put(JoinRequest.class);
            messageMap.put(JoinResponse.class);
            messageMap.put(GroupNotify.class);
            messageMap.put(LeaveRequest.class);
            messageMap.put(LeaveResponse.class);
            messageMap.put(SyncRequest.class);
            messageMap.put(SyncResponse.class);
            messageMap.put(SyncNotify.class);
        } catch (final InstantiationException | IllegalAccessException e) {
            throw new IllegalArgumentException("Init fail", e);
        }
        mMessageMap = Collections.unmodifiableMap(messageMap);
    }

    Packet<T> getMessage(final Header header) {
        try {
            T body = getNewInstance(mMessageMap.get(header.getMessageId()));
            return new Packet<>(header, body);
        } catch (final InstantiationException | IllegalAccessException e) {
            Log.wtf("Cannot instantiate: " + mMessageMap.get(header.getMessageId()), e);
        }
        return null;
    }

    @SuppressWarnings("unchecked")
    private T getNewInstance(Class<? extends Message> bodyClass) throws IllegalAccessException, InstantiationException {
        return (T) bodyClass.newInstance();
    }

    private static class MessageMap extends EnumMap<MessageId, Class<? extends Message>> {
        private MessageMap() {
            super(MessageId.class);
        }

        private void put(final Class<? extends Message> messageClass)
                throws IllegalAccessException, InstantiationException {
            Message message = messageClass.newInstance();
            if (containsKey(message.getMessageId())) {
                throw new IllegalArgumentException(
                        "Packet can be mapped only once: " + message.getMessageId());
            }
            put(message.getMessageId(), messageClass);
        }
    }
}
