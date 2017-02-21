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

import java.nio.charset.StandardCharsets;

public final class StringUtil {
    private static final char[] HEX_CHARS = "0123456789ABCDEF".toCharArray();

    private StringUtil() {
        // Not instantiable
    }

    public static String fromBytes(final byte[] bytes, final int byteLength) {
        return new String(bytes, 0, byteLength, StandardCharsets.UTF_8);
    }

    public static String fromBytes(final byte[] bytes) {
        return new String(bytes, StandardCharsets.UTF_8);
    }

    public static byte[] getBytes(final String string) {
        return string.getBytes(StandardCharsets.UTF_8);
    }

    public static String toHexString(final byte[] bytes) {
        final char[] hexChars = new char[bytes.length * 2];
        for (int j = 0; j < bytes.length; j++) {
            final int v = bytes[j] & 0xFF;
            hexChars[j * 2] = HEX_CHARS[v >>> 4];
            hexChars[j * 2 + 1] = HEX_CHARS[v & 0x0F];
        }
        return new String(hexChars);
    }
}
