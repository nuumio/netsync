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

package fi.nuumio.netsync.client;

public class GroupJoinException extends Exception {
    private final ErrorCode mError;

    GroupJoinException(final String message, final ErrorCode error) {
        super(message);
        mError = error;
    }

    GroupJoinException(final String message, final ErrorCode error,
                       final Throwable cause) {
        super(message, cause);
        mError = error;
    }

    public ErrorCode getErrorCode() {
        return mError;
    }

    public enum ErrorCode {
        /**
         * Sending GroupJoin message failed
         */
        MESSAGE_SEND_FAILED,
        /**
         * Join timed out
         */
        JOIN_TIMEOUT,
        /**
         * Group is full in server
         */
        GROUP_FULL,
        /**
         * Wrong group token given
         */
        GROUP_AUTHENTICATION_ERROR
    }
}
