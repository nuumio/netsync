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

/**
 * Settings for SyncClient.
 */
public class ClientSettings extends Settings {
    private static final String KEY_CLIENT_CONNECT_TIMEOUT = "clientConnectTimeout";
    private static final String KEY_CLIENT_GROUP_MESSAGE_TIMEOUT = "clientGroupMessageTimeout";
    private static final String KEY_CLIENT_REGISTER_TIMEOUT = "clientRegisterTimeout";
    private static final String KEY_CLIENT_SELECT_TIMEOUT = "clientSelectTimeout";
    private static final String KEY_CLIENT_SYNC_EXTRA_LATENCY = "clientSyncExtraLatency";

    /**
     * Create default SyncClient settings.
     */
    public ClientSettings() {
        super();
    }

    /**
     * Create default SyncClient settings and change values defined in resourcePath.
     *
     * @param resourcePath Path to settings property resource.
     */
    public ClientSettings(final String resourcePath) {
        super(resourcePath);
    }

    public long getClientConnectTimeout() {
        return Long.parseLong(mSettings.getProperty(KEY_CLIENT_CONNECT_TIMEOUT));
    }

    public long getClientGroupMessageTimeout() {
        return Long.parseLong(mSettings.getProperty(KEY_CLIENT_GROUP_MESSAGE_TIMEOUT));
    }

    public long getClientRegisterTimeout() {
        return Long.parseLong(mSettings.getProperty(KEY_CLIENT_REGISTER_TIMEOUT));
    }

    public long getClientSelectTimeout() {
        return Long.parseLong(mSettings.getProperty(KEY_CLIENT_SELECT_TIMEOUT));
    }

    public long getSyncExtraLatency() {
        return Long.parseLong(mSettings.getProperty(KEY_CLIENT_SYNC_EXTRA_LATENCY));
    }
}
