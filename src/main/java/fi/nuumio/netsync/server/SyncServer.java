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

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Objects;
import java.util.PriorityQueue;

import fi.nuumio.netsync.protocol.Messenger;
import fi.nuumio.netsync.protocol.message.group.JoinRequest;
import fi.nuumio.netsync.protocol.message.group.JoinResponse;
import fi.nuumio.netsync.protocol.message.group.LeaveRequest;
import fi.nuumio.netsync.protocol.message.group.SyncRequest;
import fi.nuumio.netsync.protocol.message.group.SyncResponse;
import fi.nuumio.netsync.protocol.message.group.SyncType;
import fi.nuumio.netsync.protocol.message.service.RegisterRequest;
import fi.nuumio.netsync.protocol.message.service.RegisterResponse;
import fi.nuumio.netsync.util.Log;
import fi.nuumio.netsync.util.LoggableThread;
import fi.nuumio.netsync.util.NodeId;
import fi.nuumio.netsync.util.ServerSettings;
import fi.nuumio.netsync.util.TimeUtils;
import fi.nuumio.netsync.util.Token;
import fi.nuumio.netsync.util.TokenVerificationFailureException;

public class SyncServer {
    private final int mPort;
    private final HashMap<String, SyncGroup> mGroups;
    private final HashMap<NodeId, ClientConnection> mClients;
    private final ServerSettings mSettings;
    private final PriorityQueue<Event> mEventQueue;
    private final Token mToken;
    private final LoggableThread mThread;
    private volatile boolean mRunning;
    private volatile boolean mStopping;
    private volatile boolean mFinished;
    private Exception mStoredException;

    /**
     * Create new {@link SyncServer}.
     *
     * @param settings {@link ServerSettings} to use
     */
    public SyncServer(final ServerSettings settings) {
        mSettings = settings;
        mPort = mSettings.getServerPort();
        mGroups = new HashMap<>();
        mClients = new HashMap<>();
        mRunning = false;
        mStopping = false;
        mFinished = false;
        mToken = new Token();
        mEventQueue = new PriorityQueue<>();
        mThread = new LoggableThread(new Runnable() {
            @Override
            public void run() {
                SyncServer.this.run();
            }
        });
        mThread.setLoggableName("Server" + mThread.getName());
        mStoredException = null;
    }

    public boolean isRunning() {
        return mRunning;
    }

    public void start() throws ServerStartException {
        mThread.start();
        synchronized (this) {
            while (!mRunning) {
                try {
                    wait(mSettings.getServerStartTimeout());
                } catch (final InterruptedException e) {
                    Log.d("Start interrupted");
                    Thread.currentThread().interrupt();
                }
            }
        }
        if (!isRunning()) {
            if (null != mStoredException) {
                throw new ServerStartException("Server start failed", mStoredException);
            } else {
                throw new ServerStartException("Server start failed");
            }
        }
    }

    public void stop() {
        final TimeUtils.StopWatch watch = new TimeUtils.StopWatch();
        mStopping = true;
        synchronized (this) {
            Log.d("Stopping server " + this);
            mThread.interrupt();
            while (!mFinished && watch.hasTimeLeft(mSettings.getServerStopTimeout())) {
                try {
                    wait(watch.getTimeLeft(mSettings.getServerStopTimeout()));
                } catch (final InterruptedException e) {
                    Log.d("Interrupted");
                    Thread.currentThread().interrupt();
                }
            }
        }
        try {
            mThread.join();
        } catch (final InterruptedException e) {
            Log.e("Interrupted", e);
            Thread.currentThread().interrupt();
        }
        mThread.cleanUp();
        mRunning = false;
        synchronized (this) {
            notifyAll();
        }
        if (mFinished) {
            Log.d("Server stopped in " + watch.elapsed() + " ms");
        } else {
            Log.e("Server stop failed, took " + watch.elapsed() + " ms");
        }
    }

    Event addEvent(final long delayMs, final EventHandler event, final boolean recurring) {
        if (delayMs < 0) {
            throw new IllegalArgumentException("Event delay must be > 0");
        }
        final Event e = new Event(delayMs, event, recurring);
        mEventQueue.add(e);
        return e;
    }

    void cancelEvent(final Event event) {
        mEventQueue.remove(event);
    }

    Token generateClientToken() {
        return new Token();
    }

    ServerSettings getSettings() {
        return mSettings;
    }

    Token getToken() {
        return mToken;
    }

    RegisterResponse.Code handleClientRegister(ClientConnection client, RegisterRequest request) {
        final NodeId clientId = request.getClientId();
        if (mClients.size() >= mSettings.getServerMaxClients()) {
            Log.w("Server full: " + mClients.size() + " / " + mSettings.getServerMaxClients());
            return RegisterResponse.Code.SERVER_FULL;
        }
        if (!mClients.containsKey(clientId)) {
            client.setClientId(clientId);
            mClients.put(clientId, client);
            Log.d("Client accepted: " + client + ". Now having " + mClients.size() +
                    " / " + mSettings.getServerMaxClients() + " clients");
            return RegisterResponse.Code.ACCEPTED;
        }
        Log.w("Client rejected (id in use): " + client);
        return RegisterResponse.Code.FAIL_AUTHENTICATION_FAILURE;
    }

    JoinResponse.Code handleGroupJoin(final ClientConnection client,
                                      final JoinRequest request) {
        final SyncGroup group;
        final String requestedGroupId = request.getGroupId().asString();
        final Token requestedGroupToken = request.getGroupToken();
        if (mGroups.containsKey(requestedGroupId)) {
            group = mGroups.get(requestedGroupId);
            Log.d("Existing group: " + group);
        } else {
            try {
                group = new SyncGroup(request.getGroupId(), requestedGroupToken, this);
                mGroups.put(requestedGroupId, group);
                Log.d("New group: " + group);
            } catch (final InvalidTokenException e) {
                Log.e("Invalid group token", e);
                return JoinResponse.Code.FAIL_AUTHENTICATION_FAILURE;
            }
        }
        try {
            group.add(client, requestedGroupToken);
        } catch (final GroupFullException e) {
            Log.i("Group full", e);
            return JoinResponse.Code.FAIL_GROUP_FULL;
        } catch (final TokenVerificationFailureException e) {
            Log.w("Group token verification failure", e);
            return JoinResponse.Code.FAIL_AUTHENTICATION_FAILURE;
        }
        return JoinResponse.Code.ACCEPTED;
    }

    void handleGroupLeave(final ClientConnection client, final LeaveRequest request) {
        final String requestedGroupId = request.getGroupId().asString();
        if (mGroups.containsKey(requestedGroupId)) {
            final SyncGroup group = mGroups.get(requestedGroupId);
            group.remove(client);
            if (group.isEmpty()) {
                group.close();
                mGroups.remove(requestedGroupId);
            }
        }
    }

    ServerSyncResult handleSyncRequest(final ClientConnection client,
                                       final SyncRequest request) {
        final String requestedGroupId = request.getGroupId().asString();
        final Token requestedGroupToken = request.getGroupToken();
        if (!mGroups.containsKey(requestedGroupId)) {
            return new ServerSyncResult(SyncResponse.Code.FAIL_AUTHENTICATION_FAILURE, 0);
        }
        final SyncGroup group = mGroups.get(requestedGroupId);
        final NodeId syncPoint = request.getSyncPoint();
        final SyncType type = request.getType();
        final long timeout = request.getTimeout();
        try {
            return group.sync(client, requestedGroupToken, syncPoint, type, timeout);
        } catch (final TokenVerificationFailureException e) {
            Log.w("Group sync failed in token verification", e);
            return new ServerSyncResult(SyncResponse.Code.FAIL_AUTHENTICATION_FAILURE, 0);
        }
    }

    private void accept(final SelectionKey key) {
        final ServerSocketChannel serverChannel = (ServerSocketChannel) key.channel();
        final SocketChannel clientChannel;
        SelectionKey clientKey;
        try {
            clientChannel = serverChannel.accept();
            clientChannel.configureBlocking(false);
            clientKey = clientChannel.register(key.selector(), SelectionKey.OP_READ);
        } catch (final IOException e) {
            Log.e("Failed to accept/register connection", e);
            key.cancel();
            return;
        }
        if (mClients.size() < mSettings.getServerMaxClients()) {
            ClientConnection clientConnection = new ClientConnection(this, clientChannel);
            clientKey.attach(clientConnection);
            Log.d("New connection from " + clientConnection.getAddress() +
                    ". Client count is now " + mClients.size() +
                    " / " + mSettings.getServerMaxClients());
        } else {
            Log.w("Client max count of " + mSettings.getServerMaxClients() +
                    " reached, not accepting new client");
            try {
                clientChannel.close();
            } catch (final IOException e) {
                Log.w("Failed to close rejected client's channel", e);
            }
        }
    }

    private void clearEvents() {
        mEventQueue.clear();
    }

    private void closeClients() {
        for (final ClientConnection client : mClients.values()) {
            client.close();
        }
        mClients.clear();
    }

    private void doEventHandling() {
        while (true) {
            final Event event = mEventQueue.peek();
            if (event.mTriggerTime <= TimeUtils.msTime()) {
                mEventQueue.remove(event);
                if (event.mRecurring) {
                    event.schedule();
                    mEventQueue.add(event);
                }
                event.mEvent.handle(event);
            } else {
                break;
            }
        }
    }

    private void doHousekeeping() {
        Log.d("Housekeeping time: groups: " + mGroups.size() + ", events: " + mEventQueue.size());
        // Check timed out groups
        Iterator<Map.Entry<String, SyncGroup>> iterator = mGroups.entrySet().iterator();
        while (iterator.hasNext()) {
            Map.Entry<String, SyncGroup> entry = iterator.next();
            final SyncGroup group = entry.getValue();
            if (group.hasTimedOut()) {
                Log.d("Group timed out: " + group);
                group.close();
                iterator.remove();
            }
        }
        Log.d("Housekeeping done: groups: " + mGroups.size() +
                ", events: " + mEventQueue.size() +
                ", next event: " + mEventQueue.peek());
    }

    private boolean doSelect(final Selector selector) {
        final long nextEventTimeout = mEventQueue.isEmpty() ?
                Long.MAX_VALUE : mEventQueue.peek().mTriggerTime;
        final long selectTimeout = Math.min(nextEventTimeout, mSettings.getServerSelectTimeout());
        try {
            selector.select(selectTimeout);
        } catch (final IOException e) {
            Log.e("Select failed", e);
            return false;
        }

        final Iterator<SelectionKey> iterator = selector.selectedKeys().iterator();
        while (iterator.hasNext()) {
            final SelectionKey key = iterator.next();
            iterator.remove();

            if (key.isAcceptable()) {
                accept(key);
            }
            if (key.isReadable()) {
                read(key);
            }
        }
        return true;
    }

    private void read(final SelectionKey key) {
        final ClientConnection clientConnection = (ClientConnection) key.attachment();
        Messenger.ReadMessageResult readResult;
        try {
            readResult = clientConnection.read();
        } catch (final IOException e) {
            Log.e("Failed to read from client", e);
            readResult = Messenger.ReadMessageResult.CLOSE;
        }
        if (Messenger.ReadMessageResult.CLOSE == readResult) {
            clientConnection.close();
            key.cancel();
            removeClientConnection(clientConnection);
        }
    }

    private void removeClientConnection(ClientConnection clientConnection) {
        final NodeId clientId = clientConnection.getClientId();
        if (mClients.containsKey(clientId)) {
            Iterator<Map.Entry<String, SyncGroup>> iterator = mGroups.entrySet().iterator();
            while (iterator.hasNext()) {
                Map.Entry<String, SyncGroup> entry = iterator.next();
                final SyncGroup group = entry.getValue();
                group.remove(clientConnection);
                if (group.isEmpty()) {
                    group.close();
                    iterator.remove();
                }
            }
            mClients.remove(clientId);
            Log.d("Client removed: " + clientConnection);
        }
    }

    private void run() {
        try (
                final ServerSocketChannel serverChannel = ServerSocketChannel.open();
                final Selector selector = Selector.open()
        ) {
            serverChannel.socket().bind(new InetSocketAddress(mPort));
            serverChannel.configureBlocking(false);
            serverChannel.register(selector, SelectionKey.OP_ACCEPT);

            addEvent(mSettings.getHousekeepingInterval(), new EventHandler() {
                @Override
                public void handle(final Event event) {
                    SyncServer.this.doHousekeeping();
                }
            }, true);

            mRunning = true;
            Log.d("Started server " + this);
            synchronized (this) {
                notifyAll();
            }
            while (serverChannel.isOpen() && mRunning && !mStopping) {
                if (!doSelect(selector)) {
                    mStopping = true;
                }
                doEventHandling();
            }
        } catch (final IOException e) {
            mStoredException = e;
            Log.e("Server run failed", e);
        } finally {
            clearEvents();
            closeClients();
            mRunning = false;
            mFinished = true;
            synchronized (this) {
                notifyAll();
            }
        }
    }

    public interface EventHandler {
        void handle(Event event);
    }

    static class ServerSyncResult {
        final SyncResponse.Code mCode;
        final long mTimeLeft;

        ServerSyncResult(final SyncResponse.Code code, final long timeLeft) {
            mCode = code;
            mTimeLeft = timeLeft;
        }
    }

    class Event implements Comparable<Event> {
        private final long mDelayMs;
        private final EventHandler mEvent;
        private final boolean mRecurring;
        private long mTriggerTime;

        private Event(final long delayMs, final EventHandler event, final boolean recurring) {
            mDelayMs = delayMs;
            mEvent = event;
            mRecurring = recurring;
            schedule();
        }

        @Override
        public int compareTo(final Event other) {
            if (mTriggerTime != other.mTriggerTime) {
                return mTriggerTime > other.mTriggerTime ? 1 : -1;
            } else if (mDelayMs != other.mDelayMs) {
                return mDelayMs > other.mDelayMs ? 1 : -1;
            } else if (mRecurring != other.mRecurring) {
                return mRecurring ? 1 : -1;
            }
            return mEvent.hashCode() - other.mEvent.hashCode();
        }

        @Override
        public boolean equals(Object obj) {
            if (obj instanceof Event) {
                final Event other = (Event) obj;
                return mTriggerTime == other.mTriggerTime &&
                        mDelayMs == other.mDelayMs &&
                        mRecurring == other.mRecurring &&
                        mEvent.equals(other.mEvent);
            }
            return false;
        }

        @Override
        public int hashCode() {
            return Objects.hash(mTriggerTime, mDelayMs, mRecurring, mEvent);
        }

        @Override
        public String toString() {
            return getClass().getSimpleName() + "@" + Integer.toHexString(hashCode()) + "[" +
                    "mTriggerTime=" + mTriggerTime +
                    ",mDelayMs=" + mDelayMs +
                    ",mRecurring=" + mRecurring +
                    ",mEvent=" + mEvent
                    + "]";
        }

        private void schedule() {
            mTriggerTime = TimeUtils.msTime() + mDelayMs;
        }
    }
}
