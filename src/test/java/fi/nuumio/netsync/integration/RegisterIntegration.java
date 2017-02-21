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

package fi.nuumio.netsync.integration;

import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.Timeout;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

import java.util.concurrent.TimeUnit;

import fi.nuumio.netsync.client.ClientStartFailedException;
import fi.nuumio.netsync.client.SyncClient;
import fi.nuumio.netsync.server.SyncServer;
import fi.nuumio.netsync.util.ClientSettings;
import fi.nuumio.netsync.util.Log;
import fi.nuumio.netsync.util.NodeId;
import fi.nuumio.netsync.util.ServerSettings;

import static fi.nuumio.netsync.integration.util.TestUtil.DEFAULT_TIMEOUT;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

@RunWith(JUnit4.class)
public class RegisterIntegration {
    private static final ClientSettings sClientSettings =
            new ClientSettings("/TestSettings.properties");
    private static final ServerSettings sServerSettings =
            new ServerSettings("/TestSettings.properties");
    @Rule
    public Timeout globalTimeout = new Timeout(DEFAULT_TIMEOUT, TimeUnit.MILLISECONDS);
    private SyncServer mServer;
    private SyncClient mClient1;
    private SyncClient mClient2;
    private SyncClient mClient3;
    private SyncClient mClient4;
    private SyncClient mClient5;
    private SyncClient mClient6;

    @Before
    public void setup() throws Exception {
        Log.setLevel(Log.VERBOSE);
        mServer = new SyncServer(sServerSettings);
        mServer.start();
        mClient1 = new SyncClient("localhost", new NodeId("Client_1"), sClientSettings);
        mClient2 = new SyncClient("localhost", new NodeId("Client_2"), sClientSettings);
        if (!mServer.isRunning()) {
            throw new IllegalStateException("Server not started");
        }
    }

    @After
    public void tearDown() throws Exception {
        mClient2.stop();
        mClient1.stop();
        if (mClient3 != null) {
            mClient3.stop();
        }
        if (mClient4 != null) {
            mClient4.stop();
        }
        if (mClient5 != null) {
            mClient5.stop();
        }
        if (mClient6 != null) {
            mClient6.stop();
        }
        mServer.stop();
    }

    @Test
    public void maxClientsRegister() throws Exception {
        mClient3 = new SyncClient("localhost", new NodeId("Client_3"), sClientSettings);
        mClient4 = new SyncClient("localhost", new NodeId("Client_4"), sClientSettings);
        mClient5 = new SyncClient("localhost", new NodeId("Client_5"), sClientSettings);
        mClient1.start();
        mClient2.start();
        mClient3.start();
        mClient4.start();
        mClient5.start();
        assertTrue(mClient1.isRegistered());
        assertTrue(mClient2.isRegistered());
        assertTrue(mClient3.isRegistered());
        assertTrue(mClient4.isRegistered());
        assertTrue(mClient5.isRegistered());
    }

    @Test
    public void oneClientRegister() throws Exception {
        mClient1.start();
        assertTrue(mClient1.isRegistered());
    }

    @Test
    public void oneClientStop() throws Exception {
        mClient1.start();
        mClient1.stop();
        assertFalse(mClient1.isRegistered());
    }

    @Test
    public void registerAfterNotAtMaxAnymore() throws Exception {
        mClient3 = new SyncClient("localhost", new NodeId("Client_3"), sClientSettings);
        mClient4 = new SyncClient("localhost", new NodeId("Client_4"), sClientSettings);
        mClient5 = new SyncClient("localhost", new NodeId("Client_5"), sClientSettings);
        mClient6 = new SyncClient("localhost", new NodeId("Client_6"), sClientSettings);
        mClient1.start();
        mClient2.start();
        mClient3.start();
        mClient4.start();
        mClient5.start();
        mClient5.stop();
        mClient6.start();
        assertFalse(mClient5.isRegistered());
        assertTrue(mClient6.isRegistered());
    }

    @Test(expected = ClientStartFailedException.class)
    public void registerFailSameId() throws Exception {
        mClient3 = new SyncClient("localhost", new NodeId("Client_1"), sClientSettings);
        mClient3.start();
        mClient1.start();
    }

    @Test(expected = ClientStartFailedException.class)
    public void tooManyClientsRegister() throws Exception {
        mClient3 = new SyncClient("localhost", new NodeId("Client_3"), sClientSettings);
        mClient4 = new SyncClient("localhost", new NodeId("Client_4"), sClientSettings);
        mClient5 = new SyncClient("localhost", new NodeId("Client_5"), sClientSettings);
        mClient6 = new SyncClient("localhost", new NodeId("Client_6"), sClientSettings);
        mClient1.start();
        mClient2.start();
        mClient3.start();
        mClient4.start();
        mClient5.start();
        mClient6.start();
    }

    @Test
    public void twoClientsRegister() throws Exception {
        mClient1.start();
        mClient2.start();
        assertTrue(mClient1.isRegistered());
        assertTrue(mClient2.isRegistered());
    }
}
