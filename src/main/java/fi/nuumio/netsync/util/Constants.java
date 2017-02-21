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

public final class Constants {
    public static final int PROTOCOL_VERSION_1 = 1;
    public static final int INT_BYTES = Integer.SIZE / Byte.SIZE;
    // TOKEN_SIZE = SHA-256 Size
    public static final int TOKEN_SIZE = 32;
    public static final int NODE_ID_SIZE = 32;
    public static final int MESSAGE_BUFFER_SIZE = 1024;

    private Constants() {
        // Not to be created
    }
}
