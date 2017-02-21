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

import fi.nuumio.netsync.client.GroupJoinException;
import fi.nuumio.netsync.client.GroupSyncException;
import fi.nuumio.netsync.client.SyncClient;
import fi.nuumio.netsync.client.SyncGroup;
import fi.nuumio.netsync.client.SyncGroup.MemberCount;
import fi.nuumio.netsync.integration.util.BlockingReturn;
import fi.nuumio.netsync.integration.util.BlockingReturn.Blocker;
import fi.nuumio.netsync.integration.util.TestUtil;
import fi.nuumio.netsync.server.SyncServer;
import fi.nuumio.netsync.util.ClientSettings;
import fi.nuumio.netsync.util.Log;
import fi.nuumio.netsync.util.NodeId;
import fi.nuumio.netsync.util.ServerSettings;
import fi.nuumio.netsync.util.Token;

import static fi.nuumio.netsync.integration.util.TestUtil.DEFAULT_TIMEOUT;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

@RunWith(JUnit4.class)
public class GroupJoinLeaveIntegration {
    private static final String GROUP_NAME = "Group_1";
    private static final String GROUP_TOKEN = "GroupToken";
    private static final String WRONG_GROUP_TOKEN = "Open Sesame";
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
    public void afterFullGroupMemberLeave() throws Exception {
        SyncGroup group1 = mClient1.createGroup(new NodeId(GROUP_NAME), new Token(GROUP_TOKEN));
        SyncGroup group2 = mClient2.createGroup(new NodeId(GROUP_NAME), new Token(GROUP_TOKEN));
        SyncGroup group3 = mClient3.createGroup(new NodeId(GROUP_NAME), new Token(GROUP_TOKEN));
        SyncGroup group4 = mClient4.createGroup(new NodeId(GROUP_NAME), new Token(GROUP_TOKEN));
        group1.join();
        group2.join();
        group3.join();
        try {
            group4.join();
        } catch (GroupJoinException e) {
            // Ignore
        }
        group2.leave();
        TestUtil.waitMemberCount(2, group1, group3);
        group4.join();
        TestUtil.waitMemberCount(3, group1, group3, group4);
        assertTrue(group1.contains(mClient4.getId()));
        assertTrue(group3.contains(mClient4.getId()));
        assertTrue(group4.contains(mClient4.getId()));
        assertTrue(group4.isJoined());
    }

    @Test
    public void groupFull() throws Exception {
        SyncGroup group1 = mClient1.createGroup(new NodeId(GROUP_NAME), new Token(GROUP_TOKEN));
        SyncGroup group2 = mClient2.createGroup(new NodeId(GROUP_NAME), new Token(GROUP_TOKEN));
        SyncGroup group3 = mClient3.createGroup(new NodeId(GROUP_NAME), new Token(GROUP_TOKEN));
        SyncGroup group4 = mClient4.createGroup(new NodeId(GROUP_NAME), new Token(GROUP_TOKEN));
        group1.join();
        group2.join();
        group3.join();
        GroupJoinException expected = null;
        try {
            group4.join();
        } catch (GroupJoinException e) {
            expected = e;
        }
        TestUtil.waitMemberCount(3, group1, group2, group3);
        assertNotNull(expected);
        assertEquals(GroupJoinException.ErrorCode.GROUP_FULL, expected.getErrorCode());
        assertFalse(group1.contains(mClient4.getId()));
        assertFalse(group2.contains(mClient4.getId()));
        assertFalse(group3.contains(mClient4.getId()));
    }

    @Test
    public void leaveGroup() throws Exception {
        SyncGroup group1 = mClient1.createGroup(new NodeId(GROUP_NAME), new Token(GROUP_TOKEN));
        SyncGroup group2 = mClient2.createGroup(new NodeId(GROUP_NAME), new Token(GROUP_TOKEN));
        group1.join();
        group2.join();
        group2.leave();
        TestUtil.waitMemberCount(1, group1);
        assertFalse(group1.contains(mClient2.getId()));
        assertFalse(group2.contains(mClient2.getId()));
        assertFalse(group2.isJoined());
    }

    @Test
    public void maxClientsJoin() throws Exception {
        SyncGroup group1 = mClient1.createGroup(new NodeId(GROUP_NAME), new Token(GROUP_TOKEN));
        SyncGroup group2 = mClient2.createGroup(new NodeId(GROUP_NAME), new Token(GROUP_TOKEN));
        SyncGroup group3 = mClient3.createGroup(new NodeId(GROUP_NAME), new Token(GROUP_TOKEN));
        assertNotNull(group1);
        assertNotNull(group2);
        assertNotNull(group3);
        group1.join();
        assertTrue(group1.isJoined());
        assertTrue(group1.contains(mClient1.getId()));
        group2.join();
        TestUtil.waitMemberCount(2, group1, group2);
        assertTrue(group2.isJoined());
        assertTrue(group1.contains(mClient1.getId()));
        assertTrue(group1.contains(mClient2.getId()));
        assertTrue(group2.contains(mClient1.getId()));
        assertTrue(group2.contains(mClient2.getId()));
        group3.join();
        TestUtil.waitMemberCount(3, group1, group2, group3);
        assertTrue(group3.isJoined());
        assertTrue(group1.contains(mClient1.getId()));
        assertTrue(group1.contains(mClient2.getId()));
        assertTrue(group1.contains(mClient3.getId()));
        assertTrue(group2.contains(mClient1.getId()));
        assertTrue(group2.contains(mClient2.getId()));
        assertTrue(group2.contains(mClient3.getId()));
        assertTrue(group3.contains(mClient1.getId()));
        assertTrue(group3.contains(mClient2.getId()));
        assertTrue(group3.contains(mClient3.getId()));
    }

    @Test
    public void oneClientJoin() throws Exception {
        SyncGroup group = mClient1.createGroup(new NodeId(GROUP_NAME), new Token(GROUP_TOKEN));
        assertNotNull(group);
        group.join();
        assertTrue(group.isJoined());
        assertTrue(group.contains(mClient1.getId()));
    }

    @Test
    public void rejoinAfterLeave() throws Exception {
        SyncGroup group1 = mClient1.createGroup(new NodeId(GROUP_NAME), new Token(GROUP_TOKEN));
        SyncGroup group2 = mClient2.createGroup(new NodeId(GROUP_NAME), new Token(GROUP_TOKEN));
        group1.join();
        group2.join();
        group2.leave();
        group2.join();
        TestUtil.waitMemberCount(2, group1, group2);
        assertTrue(group1.contains(mClient2.getId()));
        assertTrue(group2.contains(mClient2.getId()));
        assertTrue(group2.isJoined());
    }

    @Test
    public void rejoinEmptyGroupAfterLeaveDifferentToken() throws Exception {
        SyncGroup group1 = mClient1.createGroup(new NodeId(GROUP_NAME), new Token(GROUP_TOKEN));
        SyncGroup group2 = mClient2.createGroup(new NodeId(GROUP_NAME), new Token(GROUP_TOKEN));
        group1.join();
        group2.join();
        group2.leave();
        group1.leave();
        // This creates a new group
        SyncGroup group2New = mClient2.createGroup(new NodeId(GROUP_NAME), new Token(WRONG_GROUP_TOKEN));
        group2New.join();
        TestUtil.waitMemberCount(1, group2New);
        assertTrue(group2New.contains(mClient2.getId()));
        assertTrue(group2New.isJoined());
    }

    @Test
    public void waitMemberCountAtLeastOneAlreadyExists() throws Exception {
        SyncGroup group1 = mClient1.createGroup(new NodeId(GROUP_NAME), new Token(GROUP_TOKEN));
        group1.join();
        final boolean oneMember =
                group1.waitMembers(MemberCount.atLeast(group1, 1),
                        sClientSettings.getClientGroupMessageTimeout() * 2);
        assertTrue(oneMember);
        assertTrue(group1.contains(mClient1.getId()));
        assertTrue(group1.isJoined());
    }

    @Test
    public void waitMemberCountAtLeastTwo() throws Exception {
        final SyncGroup group1 = mClient1.createGroup(new NodeId(GROUP_NAME), new Token(GROUP_TOKEN));
        final SyncGroup group2 = mClient2.createGroup(new NodeId(GROUP_NAME), new Token(GROUP_TOKEN));
        group1.join();
        BlockingReturn<Boolean> ret = new BlockingReturn<>(new Blocker<Boolean>() {
            @Override
            public Boolean getValue() throws Throwable {
                return group1.waitMembers(MemberCount.atLeast(group1, 2),
                        sClientSettings.getClientGroupMessageTimeout() * 2);
            }
        });
        ret.start();
        group2.join();
        final boolean twoMembers = ret.get();
        assertTrue(twoMembers);
        assertTrue(group1.contains(mClient1.getId()));
        assertTrue(group1.contains(mClient2.getId()));
        assertTrue(group2.contains(mClient1.getId()));
        assertTrue(group2.contains(mClient2.getId()));
        assertTrue(group1.isJoined());
        assertTrue(group2.isJoined());
    }

    @Test
    public void waitMemberCountAtLeastTwoTimeout() throws Exception {
        final SyncGroup group1 = mClient1.createGroup(new NodeId(GROUP_NAME), new Token(GROUP_TOKEN));
        group1.join();
        final boolean twoMembers = group1.waitMembers(MemberCount.atLeast(group1, 2),
                sClientSettings.getClientGroupMessageTimeout() / 10);
        assertFalse(twoMembers);
    }

    @Test
    public void waitMemberCountEqualToTwo() throws Exception {
        final SyncGroup group1 = mClient1.createGroup(new NodeId(GROUP_NAME), new Token(GROUP_TOKEN));
        final SyncGroup group2 = mClient2.createGroup(new NodeId(GROUP_NAME), new Token(GROUP_TOKEN));
        group1.join();
        BlockingReturn<Boolean> ret = new BlockingReturn<>(new Blocker<Boolean>() {
            @Override
            public Boolean getValue() throws Throwable {
                return group1.waitMembers(MemberCount.equalTo(group1, 2),
                        sClientSettings.getClientGroupMessageTimeout() * 2);
            }
        });
        ret.start();
        group2.join();
        final boolean twoMembers = ret.get();
        assertTrue(twoMembers);
    }

    @Test
    public void waitMemberCountLessThanTwo() throws Exception {
        final SyncGroup group1 = mClient1.createGroup(new NodeId(GROUP_NAME), new Token(GROUP_TOKEN));
        final SyncGroup group2 = mClient2.createGroup(new NodeId(GROUP_NAME), new Token(GROUP_TOKEN));
        group1.join();
        group2.join();
        TestUtil.waitMemberCount(2, group1, group2);
        BlockingReturn<Boolean> ret = new BlockingReturn<>(new Blocker<Boolean>() {
            @Override
            public Boolean getValue() throws Throwable {
                return group1.waitMembers(MemberCount.lessThan(group1, 2),
                        sClientSettings.getClientGroupMessageTimeout() * 2);
            }
        });
        group2.leave();
        ret.start();
        final boolean lessThanTwoMembers = ret.get();
        assertTrue(lessThanTwoMembers);
        assertFalse(group1.contains(mClient2.getId()));
        assertFalse(group2.contains(mClient2.getId()));
        assertFalse(group2.isJoined());
    }

    @Test
    public void wrongGroupSecret() throws Exception {
        SyncGroup group1 = mClient1.createGroup(new NodeId(GROUP_NAME), new Token(GROUP_TOKEN));
        SyncGroup group2 = mClient2.createGroup(new NodeId(GROUP_NAME), new Token(WRONG_GROUP_TOKEN));
        group1.join();
        assertTrue(group1.isJoined());
        GroupJoinException expected = null;
        try {
            group2.join();
        } catch (GroupJoinException e) {
            expected = e;
        }
        assertNotNull(expected);
        assertEquals(GroupJoinException.ErrorCode.GROUP_AUTHENTICATION_ERROR,
                expected.getErrorCode());
        assertFalse(group1.contains(mClient2.getId()));
        assertFalse(group2.contains(mClient2.getId()));
    }
}
