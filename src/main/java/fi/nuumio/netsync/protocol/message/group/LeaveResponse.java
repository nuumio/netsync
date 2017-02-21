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

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import fi.nuumio.netsync.protocol.message.MessageId;
import fi.nuumio.netsync.protocol.message.Response;
import fi.nuumio.netsync.util.Constants;
import fi.nuumio.netsync.util.NodeId;
import fi.nuumio.netsync.util.Token;

public class LeaveResponse extends BaseGroupIntegerResponse implements Response<LeaveRequest> {
    private Code mCode;
    private LeaveRequest mRequest;

    public LeaveResponse() {
        // Used via reflection
        super();
        mRequest = null;
    }

    LeaveResponse(final NodeId groupId, final Token groupToken, final Token sourceToken) {
        super(groupId, groupToken, sourceToken);
        mRequest = null;
    }

    public Code getCode() {
        return mCode;
    }

    public void setCode(final Code code) {
        mCode = code;
    }

    @Override
    public MessageId getMessageId() {
        return MessageId.GROUP_LEAVE_RESPONSE;
    }

    @Override
    public LeaveRequest getOriginalRequest() {
        return mRequest;
    }

    @Override
    public void setOriginalRequest(final LeaveRequest request) {
        mRequest = request;
    }

    @Override
    public int getProtocolVersion() {
        return Constants.PROTOCOL_VERSION_1;
    }

    @Override
    int getIntResponse() {
        return mCode.getCode();
    }

    @Override
    void responseFromInt(final int response) {
        mCode = Code.valueOf(response);
    }

    public enum Code {
        ACCEPTED(0);

        static final Map<Integer, Code> sIntMapping;

        static {
            HashMap<Integer, Code> intMapping = new HashMap<>();
            for (Code result : Code.values()) {
                intMapping.put(result.getCode(), result);
            }
            sIntMapping = Collections.unmodifiableMap(intMapping);
        }

        final int mCode;

        Code(final int code) {
            mCode = code;
        }

        public static Code valueOf(final int code) {
            return sIntMapping.get(code);
        }

        public int getCode() {
            return mCode;
        }
    }
}
