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

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

public class Settings {
    private static final String KEY_SERVER_PORT = "serverPort";
    final Properties mSettings;

    Settings() {
        mSettings = new Properties();
        loadPropertiesFromResource("/DefaultSettings.properties");
    }

    Settings(final String resourcePath) {
        this();
        loadPropertiesFromResource(resourcePath);
    }

    public int getServerPort() {
        return Integer.valueOf(mSettings.getProperty(KEY_SERVER_PORT));
    }

    public void setServerPort(final int serverPort) {
        mSettings.put(KEY_SERVER_PORT, serverPort);
    }

    private void loadPropertiesFromResource(final String resource) {
        try {
            final InputStream in = getClass().getResourceAsStream(resource);
            mSettings.load(in);
        } catch (final IOException e) {
            Log.wtf("Failed to load properties from " + resource);
            throw new FatalRuntimeException("Failed to load properties from " + resource, e);
        }
    }
}
