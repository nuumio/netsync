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

package fi.nuumio.netsync.protocol.message.service;

import java.nio.ByteBuffer;

import fi.nuumio.netsync.protocol.message.Message;
import fi.nuumio.netsync.protocol.message.MessageId;
import fi.nuumio.netsync.protocol.message.Request;
import fi.nuumio.netsync.util.Constants;
import fi.nuumio.netsync.util.NodeId;
import fi.nuumio.netsync.util.Token;

public class RegisterRequest extends Message implements Request<RegisterResponse> {
    private NodeId mClientId;

    public RegisterRequest() {
        // Used via reflection
        super();
    }

    @Override
    public RegisterResponse createResponse(final Token sourceToken) {
        return new RegisterResponse(sourceToken);
    }

    @Override
    public void get(final ByteBuffer buffer) {
        super.get(buffer);
        if (null == mClientId) {
            mClientId = new NodeId(buffer);
        } else {
            mClientId.get(buffer);
        }
    }

    public NodeId getClientId() {
        return mClientId;
    }

    public void setClientId(final NodeId clientId) {
        mClientId = clientId;
    }

    @Override
    public MessageId getMessageId() {
        return MessageId.REGISTER_REQUEST;
    }

    @Override
    public int getProtocolVersion() {
        return Constants.PROTOCOL_VERSION_1;
    }

    @Override
    public int length() {
        return super.length() + (mClientId != null ? mClientId.length() : 0);
    }

    @Override
    public void put(final ByteBuffer buffer) {
        super.put(buffer);
        mClientId.put(buffer);
    }
}
