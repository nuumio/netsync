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

public class ServerSettings extends Settings {
    private static final String KEY_SERVER_GROUP_MAX_SIZE = "serverGroupMaxSize";
    private static final String KEY_SERVER_GROUP_TIMEOUT = "serverGroupTimeout";
    private static final String KEY_SERVER_HOUSEKEEPING_INTERVAL = "serverHouseKeepingInterval";
    private static final String KEY_SERVER_MAX_CLIENTS = "serverMaxClients";
    private static final String KEY_SERVER_SELECT_TIMEOUT = "serverSelectTimeout";
    private static final String KEY_SERVER_START_TIMEOUT = "serverStartTimeout";
    private static final String KEY_SERVER_STOP_TIMEOUT = "serverStopTimeout";

    public ServerSettings() {
        super();
    }

    public ServerSettings(final String resourcePath) {
        super(resourcePath);
    }

    public int getGroupMaxSize() {
        return Integer.parseInt(mSettings.getProperty(KEY_SERVER_GROUP_MAX_SIZE));
    }

    public long getGroupTimeout() {
        return Long.parseLong(mSettings.getProperty(KEY_SERVER_GROUP_TIMEOUT));
    }

    public long getHousekeepingInterval() {
        return Long.parseLong(mSettings.getProperty(KEY_SERVER_HOUSEKEEPING_INTERVAL));
    }

    public int getServerMaxClients() {
        return Integer.parseInt(mSettings.getProperty(KEY_SERVER_MAX_CLIENTS));
    }

    public long getServerSelectTimeout() {
        return Long.parseLong(mSettings.getProperty(KEY_SERVER_SELECT_TIMEOUT));
    }

    public long getServerStartTimeout() {
        return Long.parseLong(mSettings.getProperty(KEY_SERVER_START_TIMEOUT));
    }

    public long getServerStopTimeout() {
        return Long.parseLong(mSettings.getProperty(KEY_SERVER_STOP_TIMEOUT));
    }
}
