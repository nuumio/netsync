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

import java.io.IOException;
import java.util.Collections;
import java.util.Set;
import java.util.TreeSet;
import java.util.concurrent.atomic.AtomicBoolean;

import fi.nuumio.netsync.protocol.message.Packet;
import fi.nuumio.netsync.protocol.message.group.JoinRequest;
import fi.nuumio.netsync.protocol.message.group.JoinResponse;
import fi.nuumio.netsync.protocol.message.group.LeaveRequest;
import fi.nuumio.netsync.protocol.message.group.SyncNotify;
import fi.nuumio.netsync.protocol.message.group.SyncRequest;
import fi.nuumio.netsync.protocol.message.group.SyncResponse;
import fi.nuumio.netsync.protocol.message.group.SyncType;
import fi.nuumio.netsync.util.ClientSettings;
import fi.nuumio.netsync.util.FatalRuntimeException;
import fi.nuumio.netsync.util.Log;
import fi.nuumio.netsync.util.NodeId;
import fi.nuumio.netsync.util.TimeUtils;
import fi.nuumio.netsync.util.Token;

import static fi.nuumio.netsync.protocol.message.group.JoinResponse.Code.ACCEPTED;

/**
 * SyncGroup represents a group of SyncClients. Client's in same group can use waitSync() method to
 * synchronized with each other.
 */
public class SyncGroup {
    private final NodeId mGroupId;
    private final Token mGroupToken;
    private final Set<NodeId> mMembers;
    private final AtomicBoolean mSync;
    private final SyncClient mClient;
    private JoinResponse.Code mJoinStatus;
    private SyncCode mSyncCode;

    SyncGroup(final SyncClient client, final NodeId groupId, final Token groupToken) {
        mClient = client;
        if (null == groupId) {
            throw new IllegalArgumentException("groupId may not be null");
        }
        if (null == groupToken) {
            throw new IllegalArgumentException("groupToken may not be null");
        }
        mGroupId = groupId;
        mGroupToken = groupToken;
        mJoinStatus = null;
        mMembers = new TreeSet<>();
        mSync = new AtomicBoolean(false);
        mSyncCode = null;
    }

    public boolean contains(final NodeId id) {
        synchronized (mMembers) {
            return mMembers.contains(id);
        }
    }

    public NodeId getId() {
        return mGroupId;
    }

    public Token getToken() {
        return mGroupToken;
    }

    public boolean hasJoinError() {
        return mJoinStatus != null && mJoinStatus != ACCEPTED;
    }

    public boolean isJoined() {
        return ACCEPTED == mJoinStatus;
    }

    public void join() throws GroupJoinException {
        if (isJoined()) {
            return;
        }
        mJoinStatus = null;
        final Packet<JoinRequest> packet = new Packet<>(JoinRequest.class, mClient.getToken());
        final JoinRequest request = packet.getMessage();
        request.setGroupId(mGroupId);
        request.setGroupToken(mGroupToken);
        try {
            mClient.send(packet);
        } catch (final IOException e) {
            Log.e("Message send failed: ", e);
            throw new GroupJoinException("Message send failed",
                    GroupJoinException.ErrorCode.MESSAGE_SEND_FAILED, e);
        }
        final TimeUtils.StopWatch watch = new TimeUtils.StopWatch();
        final ClientSettings settings = mClient.getSettings();
        synchronized (this) {
            while (!isJoined() && !hasJoinError()
                    && watch.hasTimeLeft(settings.getClientGroupMessageTimeout())) {
                try {
                    wait(watch.getTimeLeft(settings.getClientGroupMessageTimeout()));
                } catch (final InterruptedException e) {
                    Log.w("Group join interrupted", e);
                    Thread.currentThread().interrupt();
                }
            }
        }
        mClient.cancel(packet);
        if (!isJoined()) {
            if (mJoinStatus != null) {
                switch (mJoinStatus) {
                    case FAIL_AUTHENTICATION_FAILURE:
                        throw new GroupJoinException(
                                "Group join error", GroupJoinException.ErrorCode.GROUP_AUTHENTICATION_ERROR);
                    case FAIL_GROUP_FULL:
                        throw new GroupJoinException(
                                "Group join error", GroupJoinException.ErrorCode.GROUP_FULL);
                    default:
                        throw new FatalRuntimeException("Unexpected join status");
                }
            }
            throw new GroupJoinException("Join timed out", GroupJoinException.ErrorCode.JOIN_TIMEOUT);
        }
    }

    public void leave() throws GroupLeaveException {
        if (!isJoined()) {
            return;
        }
        final Packet<LeaveRequest> packet = new Packet<>(LeaveRequest.class, mClient.getToken());
        final LeaveRequest request = packet.getMessage();
        request.setGroupId(mGroupId);
        request.setGroupToken(mGroupToken);
        try {
            mClient.send(packet);
        } catch (final IOException e) {
            Log.e("Message send failed: ", e);
            throw new GroupLeaveException("Message send failed", e);
        }
        final TimeUtils.StopWatch watch = new TimeUtils.StopWatch();
        final ClientSettings settings = mClient.getSettings();
        synchronized (this) {
            while (isJoined() && watch.hasTimeLeft(settings.getClientGroupMessageTimeout())) {
                try {
                    wait(watch.getTimeLeft(settings.getClientGroupMessageTimeout()));
                } catch (final InterruptedException e) {
                    Log.w("Group leave interrupted", e);
                    Thread.currentThread().interrupt();
                }
            }
        }
        mClient.cancel(packet);
        if (isJoined()) {
            throw new GroupLeaveException("Leave timed out");
        }
    }

    public boolean waitMembers(final MemberCount matcher, final long timeout) throws GroupSyncException {
        final TimeUtils.StopWatch watch = new TimeUtils.StopWatch();
        if (!isJoined()) {
            throw new GroupSyncException("Not joined to group");
        }
        synchronized (mMembers) {
            while (!matcher.matches() && watch.hasTimeLeft(timeout) && isJoined()) {
                try {
                    mMembers.wait(watch.getTimeLeft(timeout));
                } catch (final InterruptedException e) {
                    Log.w("Member count wait interrupted", e);
                    Thread.currentThread().interrupt();
                }
            }
        }
        return matcher.matches();
    }

    public boolean waitSync(final String syncPoint, final SyncType type, final long timeout)
            throws GroupSyncException {
        if (mSync.get()) {
            throw new IllegalStateException("Already waiting for sync");
        }
        if (!isJoined()) {
            throw new GroupSyncException("Not joined to group");
        }

        final Packet<SyncRequest> packet = new Packet<>(SyncRequest.class, mClient.getToken());
        final SyncRequest request = packet.getMessage();
        request.setGroupId(mGroupId);
        request.setGroupToken(mGroupToken);
        request.setTimeout(timeout);
        request.setType(type);
        request.setSyncPoint(new NodeId(syncPoint));
        try {
            mClient.send(packet);
        } catch (final IOException e) {
            Log.e("Message send failed: ", e);
            throw new GroupSyncException("Message send failed", e);
        }

        mSyncCode = null;
        final TimeUtils.StopWatch watch = new TimeUtils.StopWatch();
        final long extraLatency = mClient.getSettings().getSyncExtraLatency();
        synchronized (mSync) {
            mSync.set(true);
            while (isWaitingSync() && watch.hasTimeLeft(timeout + extraLatency)) {
                try {
                    mSync.wait(watch.getTimeLeft(timeout + extraLatency));
                } catch (final InterruptedException e) {
                    Log.w("Sync wait interrupted", e);
                    Thread.currentThread().interrupt();
                }
            }
        }
        if (mSyncCode != null) {
            Log.d("Got sync code: " + mSyncCode);
        } else if (!isJoined()) {
            Log.d("Group closed during sync (tool " + watch.elapsed() + " ms)");
        } else {
            Log.d("Sync timed out in " + watch.elapsed() + " ms");
        }
        if (mSync.get()) {
            mSync.set(false);
            return false;
        }
        return true;
    }

    void handleSyncPointNotify(NodeId syncPoint, SyncNotify.Code code) {
        synchronized (mSync) {
            if (mSync.get()) {
                switch (code) {
                    case SUCCESS:
                        mSyncCode = SyncCode.SUCCESS;
                        mSync.set(false);
                        mSync.notifyAll();
                        break;
                    case TIMEOUT:
                        mSyncCode = SyncCode.TIMEOUT;
                        mSync.notifyAll();
                        break;
                    case JOIN: // Fall-through
                    case LEAVE:
                        break;
                }
            } else {
                Log.d("Not waiting sync, response scrapped for " + syncPoint);
            }
        }
    }

    void handleSyncPointResponse(final NodeId syncPoint, final SyncResponse.Code code) {
        synchronized (mSync) {
            if (mSync.get()) {
                switch (code) {
                    case EXPIRED:
                        mSyncCode = SyncCode.TIMEOUT;
                        mSync.notifyAll();
                        break;
                    case FAIL_AUTHENTICATION_FAILURE:
                        mSyncCode = SyncCode.ERROR;
                        mSync.notifyAll();
                        break;
                    case CREATED: // Fall-trough
                    case JOINED:
                        break;
                }
            } else {
                Log.d("Not waiting sync, response scrapped for " + syncPoint);
            }
        }
    }

    void setJoinStatus(final JoinResponse.Code code) {
        mJoinStatus = code;
        synchronized (mMembers) {
            // If join status is set to null at leave response, clear all members, as for us it's an
            // empty group not since we left.
            if (null == code) {
                mMembers.clear();
            }
            mMembers.notifyAll();
        }
        synchronized (this) {
            notifyAll();
        }
        synchronized (mSync) {
            mSync.notifyAll();
        }
    }

    void setMembers(final NodeId[] members) {
        synchronized (mMembers) {
            mMembers.clear();
            Collections.addAll(mMembers, members);

            // We may be dropped out by notify (at least close), update joined status
            if (!mMembers.contains(mClient.getId())) {
                setJoinStatus(null);
            }
            mMembers.notifyAll();
        }
    }

    private boolean isWaitingSync() {
        return isWaitingSyncCode() && isJoined() && mSync.get();
    }

    private boolean isWaitingSyncCode() {
        return mSyncCode == null || SyncCode.WAITING == mSyncCode;
    }

    private enum SyncCode {
        WAITING,
        SUCCESS,
        TIMEOUT,
        ERROR
    }

    public abstract static class MemberCount {
        public static MemberCount atLeast(final SyncGroup group, final int count) {
            if (count < 1) {
                throw new IllegalArgumentException("count must be >= 1");
            }
            return new MemberCount() {
                @Override
                boolean matches() {
                    return group.mMembers.size() >= count;
                }
            };
        }

        public static MemberCount equalTo(final SyncGroup group, final int count) {
            if (count < 0) {
                throw new IllegalArgumentException("count must be >= 0");
            }
            return new MemberCount() {
                @Override
                boolean matches() {
                    return group.mMembers.size() == count;
                }
            };
        }

        public static MemberCount lessThan(final SyncGroup group, final int count) {
            if (count < 1) {
                throw new IllegalArgumentException("count must be >= 1");
            }
            return new MemberCount() {
                @Override
                boolean matches() {
                    return group.mMembers.size() < count;
                }
            };
        }

        abstract boolean matches();
    }
}
