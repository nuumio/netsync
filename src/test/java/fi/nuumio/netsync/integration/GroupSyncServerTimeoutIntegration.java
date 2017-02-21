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

import fi.nuumio.netsync.client.GroupSyncException;
import fi.nuumio.netsync.client.SyncClient;
import fi.nuumio.netsync.client.SyncGroup;
import fi.nuumio.netsync.integration.util.BlockingReturn;
import fi.nuumio.netsync.integration.util.BlockingReturn.Blocker;
import fi.nuumio.netsync.integration.util.TestUtil;
import fi.nuumio.netsync.protocol.message.group.SyncType;
import fi.nuumio.netsync.server.SyncServer;
import fi.nuumio.netsync.util.ClientSettings;
import fi.nuumio.netsync.util.Log;
import fi.nuumio.netsync.util.NodeId;
import fi.nuumio.netsync.util.ServerSettings;
import fi.nuumio.netsync.util.Token;

import static fi.nuumio.netsync.client.SyncGroup.MemberCount.atLeast;
import static fi.nuumio.netsync.client.SyncGroup.MemberCount.equalTo;
import static fi.nuumio.netsync.integration.util.TestUtil.DEFAULT_TIMEOUT;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

@RunWith(JUnit4.class)
public class GroupSyncServerTimeoutIntegration {
    private static final String GROUP_NAME = "Group_1";
    private static final String GROUP_TOKEN = "GroupToken";
    private static final String SYNC_POINT_1 = "SP_1";
    private static final ClientSettings sClientSettings =
            new ClientSettings("/TestSettingsQuickServerGroupTimeout.properties");
    private static final ServerSettings sServerSettings =
            new ServerSettings("/TestSettingsQuickServerGroupTimeout.properties");
    @Rule
    public Timeout globalTimeout = new Timeout(DEFAULT_TIMEOUT, TimeUnit.MILLISECONDS);
    private SyncServer mServer;
    private SyncClient mClient1;
    private SyncClient mClient2;
    private SyncClient mClient3;
    private SyncClient mClient4;
    private SyncGroup mGroup1;
    private SyncGroup mGroup2;
    private SyncGroup mGroup3;
    private SyncGroup mGroup4;

    @Before
    public void setup() throws Exception {
        Log.setLevel(Log.VERBOSE);
        mServer = new SyncServer(sServerSettings);
        mServer.start();
        mClient1 = new SyncClient("localhost", new NodeId("Client_1"), sClientSettings);
        mClient2 = new SyncClient("localhost", new NodeId("Client_2"), sClientSettings);
        mClient3 = new SyncClient("localhost", new NodeId("Client_3"), sClientSettings);
        mClient4 = new SyncClient("localhost", new NodeId("Client_4"), sClientSettings);
        if (!mServer.isRunning()) {
            throw new IllegalStateException("Server not started");
        }
        mClient1.start();
        mClient2.start();
        mClient3.start();
        mClient4.start();
        mGroup1 = mClient1.createGroup(new NodeId(GROUP_NAME), new Token(GROUP_TOKEN));
        mGroup2 = mClient2.createGroup(new NodeId(GROUP_NAME), new Token(GROUP_TOKEN));
        mGroup3 = mClient3.createGroup(new NodeId(GROUP_NAME), new Token(GROUP_TOKEN));
        mGroup4 = mClient4.createGroup(new NodeId(GROUP_NAME), new Token(GROUP_TOKEN));
        mGroup1.join();
        mGroup2.join();
        mGroup1.waitMembers(atLeast(mGroup1, 2), sClientSettings.getClientGroupMessageTimeout());
        mGroup2.waitMembers(atLeast(mGroup2, 2), sClientSettings.getClientGroupMessageTimeout());
    }

    @After
    public void tearDown() throws Exception {
        mGroup1.leave();
        mGroup2.leave();
        mGroup3.leave();
        mGroup4.leave();
        mClient1.stop();
        mClient2.stop();
        mClient3.stop();
        mClient4.stop();
        mServer.stop();
    }

    @Test
    public void syncPointKeepAlive() throws Exception {
        mGroup3.join();
        mGroup4.join();
        mGroup1.waitMembers(atLeast(mGroup1, 4), sClientSettings.getClientGroupMessageTimeout());
        mGroup2.waitMembers(atLeast(mGroup2, 4), sClientSettings.getClientGroupMessageTimeout());
        mGroup3.waitMembers(atLeast(mGroup3, 4), sClientSettings.getClientGroupMessageTimeout());
        mGroup4.waitMembers(atLeast(mGroup3, 4), sClientSettings.getClientGroupMessageTimeout());
        BlockingReturn<Boolean> ret1 = new BlockingReturn<>(new Blocker<Boolean>() {
            @Override
            public Boolean getValue() throws Throwable {
                return mGroup1.waitSync(
                        SYNC_POINT_1, SyncType.ALL, sClientSettings.getClientGroupMessageTimeout() * 2);
            }
        });
        BlockingReturn<Boolean> ret2 = new BlockingReturn<>(new Blocker<Boolean>() {
            @Override
            public Boolean getValue() throws Throwable {
                return mGroup2.waitSync(
                        SYNC_POINT_1, SyncType.ALL, sClientSettings.getClientGroupMessageTimeout() * 2);
            }
        });
        BlockingReturn<Boolean> ret3 = new BlockingReturn<>(new Blocker<Boolean>() {
            @Override
            public Boolean getValue() throws Throwable {
                return mGroup3.waitSync(
                        SYNC_POINT_1, SyncType.ALL, sClientSettings.getClientGroupMessageTimeout() * 2);
            }
        });
        // This is quite unreliable but it'll have to do for now
        TestUtil.sleep(sServerSettings.getGroupTimeout() / 2);
        ret1.start();
        // This is quite unreliable but it'll have to do for now
        TestUtil.sleep(sServerSettings.getGroupTimeout() / 2);
        ret2.start();
        // This is quite unreliable but it'll have to do for now
        TestUtil.sleep(sServerSettings.getGroupTimeout() / 2);
        ret3.start();
        // This is quite unreliable but it'll have to do for now
        TestUtil.sleep(sServerSettings.getGroupTimeout() / 2);
        assertTrue(
                mGroup1.waitMembers(atLeast(mGroup1, 4), sClientSettings.getClientGroupMessageTimeout()));
        assertTrue(
                mGroup2.waitMembers(atLeast(mGroup2, 4), sClientSettings.getClientGroupMessageTimeout()));
        assertTrue(
                mGroup3.waitMembers(atLeast(mGroup3, 4), sClientSettings.getClientGroupMessageTimeout()));
        assertTrue(
                mGroup4.waitMembers(atLeast(mGroup3, 4), sClientSettings.getClientGroupMessageTimeout()));
        assertTrue(mGroup4.waitSync(
                SYNC_POINT_1, SyncType.ALL, sClientSettings.getClientGroupMessageTimeout() * 2));
        assertTrue(ret1.get());
        assertTrue(ret2.get());
        assertTrue(ret3.get());
    }

    @Test(expected = GroupSyncException.class)
    public void syncPointServerTimeout() throws Exception {
        mGroup3.join();
        mGroup3.waitMembers(atLeast(mGroup3, 3), sClientSettings.getClientGroupMessageTimeout());
        BlockingReturn<Boolean> ret1 = new BlockingReturn<>(new Blocker<Boolean>() {
            @Override
            public Boolean getValue() throws Throwable {
                return mGroup1.waitSync(
                        SYNC_POINT_1, SyncType.ALL, sClientSettings.getClientGroupMessageTimeout() * 2);
            }
        });
        BlockingReturn<Boolean> ret2 = new BlockingReturn<>(new Blocker<Boolean>() {
            @Override
            public Boolean getValue() throws Throwable {
                return mGroup2.waitSync(
                        SYNC_POINT_1, SyncType.ALL, sClientSettings.getClientGroupMessageTimeout() * 2);
            }
        });
        ret1.start();
        ret2.start();
        mGroup3.waitMembers(equalTo(mGroup3, 0), sClientSettings.getClientGroupMessageTimeout());
        ret1.get();
        ret2.get();
        final boolean b3 = mGroup3.waitSync(SYNC_POINT_1, SyncType.ALL,
                sClientSettings.getClientGroupMessageTimeout() * 2);
    }

    @Test
    public void syncPointThreeMembersGroupServerTimeout() throws Exception {
        mGroup1.join();
        mGroup2.join();
        mGroup3.join();
        mGroup1.waitMembers(atLeast(mGroup1, 3), sClientSettings.getClientGroupMessageTimeout());
        mGroup2.waitMembers(atLeast(mGroup2, 3), sClientSettings.getClientGroupMessageTimeout());
        mGroup3.waitMembers(atLeast(mGroup3, 3), sClientSettings.getClientGroupMessageTimeout());
        BlockingReturn<Boolean> ret1 = new BlockingReturn<>(new Blocker<Boolean>() {
            @Override
            public Boolean getValue() throws Throwable {
                return mGroup1.waitSync(
                        SYNC_POINT_1, SyncType.ALL,
                        sClientSettings.getClientGroupMessageTimeout());
            }
        });
        ret1.start();
        final boolean result = mGroup2.waitSync(
                SYNC_POINT_1, SyncType.ALL, sClientSettings.getClientGroupMessageTimeout());
        assertFalse(result);
        assertFalse(ret1.get());
    }
}
