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

package fi.nuumio.netsync.server;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import fi.nuumio.netsync.protocol.message.Packet;
import fi.nuumio.netsync.protocol.message.group.GroupNotify;
import fi.nuumio.netsync.protocol.message.group.SyncNotify;
import fi.nuumio.netsync.protocol.message.group.SyncResponse;
import fi.nuumio.netsync.protocol.message.group.SyncType;
import fi.nuumio.netsync.util.Log;
import fi.nuumio.netsync.util.NodeId;
import fi.nuumio.netsync.util.ServerSettings;
import fi.nuumio.netsync.util.TimeUtils;
import fi.nuumio.netsync.util.Token;
import fi.nuumio.netsync.util.TokenVerificationFailureException;

/**
 * Server side internal re-presentation of a sync group.
 */
class SyncGroup {
    private final StringBuilder mSb;
    private final List<ClientConnection> mClients = new ArrayList<>();
    private final HashMap<NodeId, SyncPoint> mSyncPoints = new HashMap<>();
    private final NodeId mGroupId;
    private final Token mGroupToken;
    private final ServerSettings mSettings;
    private final SyncServer mServer;
    private long lastActivity = Long.MIN_VALUE;

    SyncGroup(final NodeId groupId, final Token groupToken, final SyncServer server)
            throws InvalidTokenException {
        if (null == groupToken) {
            throw new InvalidTokenException("Group token may not be null");
        }
        mGroupId = groupId;
        mGroupToken = groupToken;
        mServer = server;
        mSettings = mServer.getSettings();
        mSb = new StringBuilder();
        refresh();
    }

    @Override
    public String toString() {
        synchronized (mSb) {
            mSb.setLength(0);
            mSb.append("[");
            for (int i = 0; i < mClients.size(); i++) {
                if (i > 0) {
                    mSb.append(",");
                }
                mSb.append(mClients.get(i).getClientId());
            }
            mSb.append("]");
            return getClass().getSimpleName() + "@" + Integer.toHexString(hashCode()) + "[" +
                    mSb.toString() + "]";
        }
    }

    void add(final ClientConnection client, final Token groupToken)
            throws GroupFullException, TokenVerificationFailureException {
        mGroupToken.verify(groupToken);
        if (!mClients.contains(client)) {
            if (mClients.size() >= mSettings.getGroupMaxSize()) {
                throw new GroupFullException("Max client count reached: " + mClients.size());
            }
            mClients.add(client);
        }
        refresh();
        notifyMembers(GroupNotify.Code.JOIN);
    }

    void close() {
        notifyMembers(GroupNotify.Code.CLOSE);
        mClients.clear();
    }

    boolean hasSameClients(final List<ClientConnection> clients) {
        // Now it's enough to check if given (sync point) list contains all group members as
        // sync point has subset of is = contains all means they're the same.
        return clients.containsAll(mClients);
    }

    boolean hasTimedOut() {
        final long elapsedSinceRefresh = TimeUtils.msTime() - lastActivity;
        return elapsedSinceRefresh > mSettings.getGroupTimeout();
    }

    boolean isEmpty() {
        return mClients.isEmpty();
    }

    void remove(final ClientConnection clientConnection) {
        final boolean removed = mClients.remove(clientConnection);
        if (removed) {
            notifyMembers(GroupNotify.Code.LEAVE);
            for (final SyncPoint syncPoint : mSyncPoints.values()) {
                if (syncPoint.leave(clientConnection)) {
                    notifySyncMembers(syncPoint, SyncNotify.Code.LEAVE);
                }
            }
            refresh();
        }
    }

    SyncServer.ServerSyncResult sync(final ClientConnection client,
                                     final Token requestedGroupToken,
                                     final NodeId syncPointId,
                                     final SyncType type,
                                     final long timeout)
            throws TokenVerificationFailureException {
        mGroupToken.verify(requestedGroupToken);
        if (!mClients.contains(client)) {
            return new SyncServer.ServerSyncResult(
                    SyncResponse.Code.FAIL_AUTHENTICATION_FAILURE, 0);
        }
        final SyncPoint syncPoint;
        final SyncResponse.Code response;
        if (!mSyncPoints.containsKey(syncPointId)) {
            syncPoint = new SyncPoint(mServer, this, syncPointId, timeout);
            response = SyncResponse.Code.CREATED;
            mSyncPoints.put(syncPointId, syncPoint);
        } else {
            syncPoint = mSyncPoints.get(syncPointId);
            response = SyncResponse.Code.JOINED;
        }
        final boolean joinOk = syncPoint.join(client);
        if (joinOk) {
            refresh();
            notifySyncMembers(syncPoint, SyncNotify.Code.JOIN);
        } else {
            Log.d("Sync group already expired");
        }
        return new SyncServer.ServerSyncResult(!joinOk ? SyncResponse.Code.EXPIRED : response,
                syncPoint.timeLeft());
    }

    void syncPointExpired(final SyncPoint syncPoint) {
        notifySyncMembers(syncPoint, SyncNotify.Code.TIMEOUT);
        mSyncPoints.remove(syncPoint.getId());
    }

    void syncPointTriggered(final SyncPoint syncPoint) {
        notifySyncMembers(syncPoint, SyncNotify.Code.SUCCESS);
        mSyncPoints.remove(syncPoint.getId());
    }

    private void notifyMembers(final GroupNotify.Code code) {
        Packet<GroupNotify> packet = new Packet<>(GroupNotify.class, mServer.getToken());
        GroupNotify notify = packet.getMessage();
        notify.setGroupId(mGroupId);
        notify.setGroupToken(mGroupToken);
        notify.setNotifyCode(code);
        final NodeId[] members;
        if (GroupNotify.Code.CLOSE == code) {
            members = new NodeId[0];
        } else {
            members = new NodeId[mClients.size()];
            for (int i = 0; i < members.length; i++) {
                members[i] = mClients.get(i).getClientId();
            }
        }
        notify.setMembers(members);
        for (final ClientConnection client : mClients) {
            client.send(packet);
        }
    }

    private void notifySyncMembers(final SyncPoint syncPoint, final SyncNotify.Code code) {
        Packet<SyncNotify> packet = new Packet<>(SyncNotify.class, mServer.getToken());
        SyncNotify notify = packet.getMessage();
        notify.setGroupId(mGroupId);
        notify.setGroupToken(mGroupToken);
        notify.setSyncCode(code);
        notify.setSyncPoint(syncPoint.getId());
        final List<ClientConnection> clients = syncPoint.getClients();
        final NodeId[] members = new NodeId[clients.size()];
        for (int i = 0; i < members.length; i++) {
            members[i] = clients.get(i).getClientId();
        }
        notify.setMembers(members);
        for (final ClientConnection client : clients) {
            client.send(packet);
        }
    }

    private void refresh() {
        lastActivity = TimeUtils.msTime();
    }
}
