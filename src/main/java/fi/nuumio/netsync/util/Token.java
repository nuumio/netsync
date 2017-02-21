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

package fi.nuumio.netsync.util;

import java.nio.ByteBuffer;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;

import fi.nuumio.netsync.protocol.message.Bufferable;

/**
 * Token is used to "authenticate" nodes in system. NOTE: There's no encryption so the system
 * is not secure in any way. Token just helps to avoid accidental mishaps.
 * <p>
 * Server gives client's their token in registration.
 * <p>
 * Groups have shared token that all client's must know.
 */
public class Token implements Bufferable {
    public static final Token NULL_TOKEN = new Token((byte) 0);
    private byte[] mToken;

    public Token() {
        mToken = new byte[Constants.TOKEN_SIZE];
        do {
            RandomHelper.nextBytes(mToken);
        } while (NULL_TOKEN.equals(this));
    }

    public Token(final ByteBuffer buffer) {
        mToken = new byte[Constants.TOKEN_SIZE];
        get(buffer);
    }

    public Token(final String token) {
        MessageDigest md;
        try {
            md = MessageDigest.getInstance("SHA-256");
        } catch (final NoSuchAlgorithmException e) {
            Log.wtf("Cannot operate without SHA-256. Get better java.", e);
            throw new FatalRuntimeException("Cannot operate without SHA-256. Get better java.", e);
        }
        mToken = md.digest(StringUtil.getBytes(token));
    }

    private Token(final byte fill) {
        mToken = new byte[Constants.TOKEN_SIZE];
        Arrays.fill(mToken, fill);
    }

    @Override
    public boolean equals(final Object obj) {
        return obj instanceof Token && Arrays.equals(mToken, ((Token) obj).mToken);
    }

    @Override
    public void get(final ByteBuffer buffer) {
        buffer.get(mToken);
    }

    @Override
    public int hashCode() {
        return Arrays.hashCode(mToken);
    }

    @Override
    public int length() {
        return mToken.length;
    }

    @Override
    public void put(final ByteBuffer buffer) {
        buffer.put(mToken);
    }

    @Override
    public String toString() {
        return getClass().getSimpleName() + "@" + Integer.toHexString(hashCode()) + "[" +
                StringUtil.toHexString(mToken) + "]";
    }

    public void verify(final Token token) throws TokenVerificationFailureException {
        if (!Arrays.equals(mToken, token.mToken)) {
            throw new TokenVerificationFailureException("Token verification failed");
        }
    }
}
