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

import fi.nuumio.netsync.protocol.message.MessageId;
import fi.nuumio.netsync.protocol.message.Request;
import fi.nuumio.netsync.util.Constants;
import fi.nuumio.netsync.util.Token;

public class JoinRequest extends BaseGroupMessage implements Request<JoinResponse> {
    public JoinRequest() {
        // Used via reflection
        super();
    }

    @Override
    public JoinResponse createResponse(final Token sourceToken) {
        return new JoinResponse(getGroupId(), getGroupToken(), sourceToken);
    }

    @Override
    public MessageId getMessageId() {
        return MessageId.GROUP_JOIN_REQUEST;
    }

    @Override
    public int getProtocolVersion() {
        return Constants.PROTOCOL_VERSION_1;
    }
}
