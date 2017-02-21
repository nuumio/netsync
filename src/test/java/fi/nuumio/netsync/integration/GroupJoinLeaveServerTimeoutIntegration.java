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
import fi.nuumio.netsync.client.SyncGroup.MemberCount;
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
import static fi.nuumio.netsync.integration.util.TestUtil.DEFAULT_TIMEOUT;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

@RunWith(JUnit4.class)
public class GroupJoinLeaveServerTimeoutIntegration {
    private static final String GROUP_NAME = "Group_1";
    private static final String GROUP_TOKEN = "GroupToken";
    private static final String WRONG_GROUP_TOKEN = "Open Sesame";
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
    }

    @After
    public void tearDown() throws Exception {
        mClient1.stop();
        mClient2.stop();
        mClient3.stop();
        mClient4.stop();
        mServer.stop();
    }

    @Test
    public void groupKeepAliveJoin() throws Exception {
        SyncGroup group1 = mClient1.createGroup(new NodeId(GROUP_NAME), new Token(GROUP_TOKEN));
        SyncGroup group2 = mClient2.createGroup(new NodeId(GROUP_NAME), new Token(GROUP_TOKEN));
        SyncGroup group3 = mClient3.createGroup(new NodeId(GROUP_NAME), new Token(GROUP_TOKEN));
        SyncGroup group4 = mClient4.createGroup(new NodeId(GROUP_NAME), new Token(GROUP_TOKEN));
        group1.join();
        // This is quite unreliable but it'll have to do for now
        TestUtil.sleep(sServerSettings.getGroupTimeout() / 2);
        group2.join();
        // This is quite unreliable but it'll have to do for now
        TestUtil.sleep(sServerSettings.getGroupTimeout() / 2);
        group3.join();
        // This is quite unreliable but it'll have to do for now
        TestUtil.sleep(sServerSettings.getGroupTimeout() / 2);
        group4.join();
        final boolean allMembers1 =
                group1.waitMembers(MemberCount.equalTo(group1, 4), DEFAULT_TIMEOUT);
        final boolean allMembers2 =
                group2.waitMembers(MemberCount.equalTo(group2, 4), DEFAULT_TIMEOUT);
        final boolean allMembers3 =
                group3.waitMembers(MemberCount.equalTo(group3, 4), DEFAULT_TIMEOUT);
        final boolean allMembers4 =
                group4.waitMembers(MemberCount.equalTo(group4, 4), DEFAULT_TIMEOUT);
        assertTrue(allMembers1);
        assertTrue(allMembers2);
        assertTrue(allMembers3);
        assertTrue(allMembers4);
        assertTrue(group1.isJoined());
        assertTrue(group2.isJoined());
        assertTrue(group3.isJoined());
        assertTrue(group4.isJoined());
    }

    @Test
    public void groupKeepAliveLeave() throws Exception {
        SyncGroup group1 = mClient1.createGroup(new NodeId(GROUP_NAME), new Token(GROUP_TOKEN));
        SyncGroup group2 = mClient2.createGroup(new NodeId(GROUP_NAME), new Token(GROUP_TOKEN));
        SyncGroup group3 = mClient3.createGroup(new NodeId(GROUP_NAME), new Token(GROUP_TOKEN));
        SyncGroup group4 = mClient4.createGroup(new NodeId(GROUP_NAME), new Token(GROUP_TOKEN));
        group1.join();
        group2.join();
        group3.join();
        group4.join();
        group1.waitMembers(MemberCount.equalTo(group1, 4), DEFAULT_TIMEOUT);
        group2.waitMembers(MemberCount.equalTo(group2, 4), DEFAULT_TIMEOUT);
        group3.waitMembers(MemberCount.equalTo(group3, 4), DEFAULT_TIMEOUT);
        group4.waitMembers(MemberCount.equalTo(group4, 4), DEFAULT_TIMEOUT);
        group1.leave();
        // This is quite unreliable but it'll have to do for now
        TestUtil.sleep(sServerSettings.getGroupTimeout() / 2);
        group2.leave();
        // This is quite unreliable but it'll have to do for now
        TestUtil.sleep(sServerSettings.getGroupTimeout() / 2);
        group3.leave();
        // This is quite unreliable but it'll have to do for now
        TestUtil.sleep(sServerSettings.getGroupTimeout() / 2);
        final boolean oneMember4 =
                group4.waitMembers(MemberCount.equalTo(group4, 1), DEFAULT_TIMEOUT);
        assertTrue(oneMember4);
        group4.leave();
        assertFalse(group1.isJoined());
        assertFalse(group2.isJoined());
        assertFalse(group3.isJoined());
        assertFalse(group4.isJoined());
    }

    @Test
    public void groupServerTimeout() throws Exception {
        SyncGroup group1 = mClient1.createGroup(new NodeId(GROUP_NAME), new Token(GROUP_TOKEN));
        SyncGroup group2 = mClient2.createGroup(new NodeId(GROUP_NAME), new Token(GROUP_TOKEN));
        group1.join();
        group2.join();
        boolean noMembers2 = false;
        final boolean noMembers1 =
                group1.waitMembers(MemberCount.equalTo(group1, 0), DEFAULT_TIMEOUT);
        try {
            noMembers2 =
                    group2.waitMembers(MemberCount.equalTo(group2, 0), DEFAULT_TIMEOUT);
        } catch (GroupSyncException e) {
            // It's ok as this may get thrown if group2 gets close notidy before we run wait.
            noMembers2 = true;
        }
        assertTrue(noMembers1);
        assertTrue(noMembers2);
        assertFalse(group1.isJoined());
        assertFalse(group2.isJoined());
    }

    @Test
    public void groupServerTimeoutRejoin() throws Exception {
        SyncGroup group1 = mClient1.createGroup(new NodeId(GROUP_NAME), new Token(GROUP_TOKEN));
        group1.join();
        final boolean noMembers =
                group1.waitMembers(MemberCount.equalTo(group1, 0), DEFAULT_TIMEOUT);
        assertTrue(noMembers);
        group1.join();
        assertTrue(group1.isJoined());
        assertTrue(group1.contains(mClient1.getId()));
    }

    @Test
    public void groupServerTimeoutRejoinDifferentToken() throws Exception {
        SyncGroup group1 = mClient1.createGroup(new NodeId(GROUP_NAME), new Token(GROUP_TOKEN));
        group1.join();
        final boolean noMembers =
                group1.waitMembers(MemberCount.equalTo(group1, 0), DEFAULT_TIMEOUT);
        assertTrue(noMembers);
        SyncGroup group1New = mClient1.createGroup(new NodeId(GROUP_NAME), new Token(WRONG_GROUP_TOKEN));
        group1New.join();
        assertTrue(group1New.isJoined());
        assertTrue(group1New.contains(mClient1.getId()));
    }

    @Test
    public void waitMemberCountAtLeastTwoGroupServerTimeout() throws Exception {
        final SyncGroup group1 = mClient1.createGroup(new NodeId(GROUP_NAME), new Token(GROUP_TOKEN));
        group1.join();
        final boolean twoMembers =
                group1.waitMembers(MemberCount.atLeast(group1, 2), DEFAULT_TIMEOUT);
        assertFalse(twoMembers);
        assertFalse(group1.isJoined());
        assertFalse(group1.contains(mClient1.getId()));
    }
}
