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
import java.util.List;

import fi.nuumio.netsync.util.Log;
import fi.nuumio.netsync.util.NodeId;
import fi.nuumio.netsync.util.TimeUtils;

class SyncPoint {
    private final StringBuilder mSb;
    private final List<ClientConnection> mClients = new ArrayList<>();
    private final NodeId mId;
    private final SyncServer mServer;
    private final SyncGroup mGroup;
    private final long mTimeout;
    private final SyncServer.Event mTimeoutEvent;
    private final TimeUtils.StopWatch mWatch;
    private boolean mExpired;

    SyncPoint(final SyncServer server, final SyncGroup group, final NodeId id,
              final long timeout) {
        mExpired = false;
        mServer = server;
        mGroup = group;
        mId = id;
        mTimeout = timeout;
        mTimeoutEvent = mServer.addEvent(timeout, new SyncServer.EventHandler() {
            @Override
            public void handle(final SyncServer.Event event) {
                Log.d("Sync timed out");
                SyncPoint.this.mGroup.syncPointExpired(SyncPoint.this);
                SyncPoint.this.mClients.clear();
                SyncPoint.this.mExpired = true;
            }
        }, false);
        mWatch = new TimeUtils.StopWatch();
        mSb = new StringBuilder();
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

    List<ClientConnection> getClients() {
        return mClients;
    }

    NodeId getId() {
        return mId;
    }

    boolean join(final ClientConnection client) {
        // Currently only "wait all" is supported
        if (!mClients.contains(client)) {
            if (mExpired) {
                return false;
            }
            mClients.add(client);
            Log.d("Client added to sync group: " + this);
            if (mGroup.hasSameClients(mClients)) {
                // Cancel timeout event and add trigger event
                Log.d("Adding sync trigger event");
                mServer.cancelEvent(mTimeoutEvent);
                mServer.addEvent(0, new SyncServer.EventHandler() {
                    @Override
                    public void handle(final SyncServer.Event event) {
                        Log.d("Sync triggered");
                        SyncPoint.this.mGroup.syncPointTriggered(SyncPoint.this);
                        SyncPoint.this.mClients.clear();
                        SyncPoint.this.mExpired = true;
                    }
                }, false);
            }
        } else {
            Log.d("Client already in sync group: " + this);
        }
        return true;
    }

    boolean leave(final ClientConnection client) {
        Log.d("Sync client leave: " + client);
        return mClients.remove(client);
    }

    long timeLeft() {
        return Math.max(0, mTimeout - mWatch.elapsed());
    }
}
