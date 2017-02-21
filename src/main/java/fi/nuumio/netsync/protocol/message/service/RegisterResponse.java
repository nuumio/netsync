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
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import fi.nuumio.netsync.protocol.message.Message;
import fi.nuumio.netsync.protocol.message.MessageId;
import fi.nuumio.netsync.protocol.message.Response;
import fi.nuumio.netsync.util.Constants;
import fi.nuumio.netsync.util.Token;

public class RegisterResponse extends Message implements Response<RegisterRequest> {
    private Token mClientToken;
    private Code mCode;
    private RegisterRequest mRequest;

    public RegisterResponse() {
        // Used via reflection
        super();
        mRequest = null;
    }

    RegisterResponse(final Token sourceToken) {
        super(sourceToken);
        mRequest = null;
    }

    @Override
    public void get(final ByteBuffer buffer) {
        super.get(buffer);
        if (null == mClientToken) {
            mClientToken = new Token(buffer);
        } else {
            mClientToken.get(buffer);
        }
        mCode = Code.valueOf(buffer.getInt());
    }

    public Token getClientToken() {
        return mClientToken;
    }

    public void setClientToken(final Token clientToken) {
        mClientToken = clientToken;
    }

    public Code getCode() {
        return mCode;
    }

    public void setCode(final Code code) {
        mCode = code;
    }

    @Override
    public MessageId getMessageId() {
        return MessageId.REGISTER_RESPONSE;
    }

    @Override
    public RegisterRequest getOriginalRequest() {
        return mRequest;
    }

    @Override
    public void setOriginalRequest(final RegisterRequest request) {
        mRequest = request;
    }

    @Override
    public int getProtocolVersion() {
        return Constants.PROTOCOL_VERSION_1;
    }

    @Override
    public int length() {
        return super.length() +
                (mClientToken != null ? mClientToken.length() : 0) + Constants.INT_BYTES;
    }

    @Override
    public void put(final ByteBuffer buffer) {
        super.put(buffer);
        mClientToken.put(buffer);
        buffer.putInt(mCode.getCode());
    }

    public enum Code {
        ACCEPTED(0),
        SERVER_FULL(1),
        FAIL_AUTHENTICATION_FAILURE(2);

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
