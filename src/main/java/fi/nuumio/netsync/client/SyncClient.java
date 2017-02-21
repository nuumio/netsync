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
import java.net.InetSocketAddress;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.SocketChannel;
import java.util.HashMap;
import java.util.Iterator;

import fi.nuumio.netsync.protocol.MessageHandler;
import fi.nuumio.netsync.protocol.Messenger;
import fi.nuumio.netsync.protocol.message.Message;
import fi.nuumio.netsync.protocol.message.Packet;
import fi.nuumio.netsync.protocol.message.Request;
import fi.nuumio.netsync.protocol.message.group.GroupNotify;
import fi.nuumio.netsync.protocol.message.group.JoinResponse;
import fi.nuumio.netsync.protocol.message.group.LeaveResponse;
import fi.nuumio.netsync.protocol.message.group.SyncNotify;
import fi.nuumio.netsync.protocol.message.group.SyncResponse;
import fi.nuumio.netsync.protocol.message.service.RegisterRequest;
import fi.nuumio.netsync.protocol.message.service.RegisterResponse;
import fi.nuumio.netsync.util.ClientSettings;
import fi.nuumio.netsync.util.Log;
import fi.nuumio.netsync.util.LoggableThread;
import fi.nuumio.netsync.util.NodeId;
import fi.nuumio.netsync.util.TimeUtils;
import fi.nuumio.netsync.util.Token;

import static fi.nuumio.netsync.protocol.Messenger.ReadMessageResult.CLOSE;
import static fi.nuumio.netsync.util.Token.NULL_TOKEN;

/**
 * SyncClient client is the main class for satisfying your synchronization needs.
 * <p>
 * Create new SyncClient, start(), createGroup(), join(), and waitSync().
 */
public class SyncClient {
    private final NodeId mClientId;
    private final ClientSettings mSettings;
    private final String mServerAddress;
    private final int mServerPort;
    private final LoggableThread mThread;
    private final HashMap<NodeId, SyncGroup> mGroups;
    private volatile RegisterResponse.Code mRegistrationStatus;
    private volatile boolean mMainLoopRunning;
    private Token mToken;
    private SelectionKey mMainKey;
    private Messenger<? extends Message> mMessenger;
    private SocketChannel mChannel;
    private Selector mSelector;

    public SyncClient(final String serverAddress, final NodeId id, final ClientSettings settings) {
        mClientId = id;
        mSettings = settings;
        mServerAddress = serverAddress;
        mServerPort = mSettings.getServerPort();
        mThread = new LoggableThread(new Runnable() {
            @Override
            public void run() {
                SyncClient.this.run();
            }
        });
        mThread.setLoggableName(mThread.getName() + "-" + id);
        mGroups = new HashMap<>();
        mRegistrationStatus = null;
        mMainLoopRunning = false;
        mToken = NULL_TOKEN;
        mMainKey = null;
        mMessenger = null;
        mChannel = null;
        mSelector = null;
    }

    public SyncGroup createGroup(final NodeId groupId, final Token groupToken) {
        SyncGroup group;
        if (mGroups.containsKey(groupId)) {
            group = mGroups.get(groupId);
            if (groupToken.equals(group.getToken())) {
                // We already have joined this group
                return group;
            } else {
                // Token mismatch, leave previous group and join new
                try {
                    group.leave();
                } catch (final GroupLeaveException e) {
                    Log.e("Fail when leaving previous group", e);
                }
                mGroups.remove(groupId);
            }
        }
        group = new SyncGroup(this, groupId, groupToken);
        mGroups.put(groupId, group);
        return group;
    }

    public NodeId getId() {
        return mClientId;
    }

    public boolean isRegistered() {
        return RegisterResponse.Code.ACCEPTED == mRegistrationStatus;
    }

    public void start() throws ClientStartFailedException {
        Log.d("Starting client " + this);
        clearRegistration();
        mMainLoopRunning = true;
        mThread.start();
        final TimeUtils.StopWatch watch = new TimeUtils.StopWatch();
        boolean interrupted = false;
        while (isRegistering() && watch.hasTimeLeft(mSettings.getClientRegisterTimeout())) {
            try {
                synchronized (this) {
                    wait(watch.getTimeLeft(mSettings.getClientRegisterTimeout()));
                }
            } catch (final InterruptedException e) {
                Log.w("Interrupted in start", e);
                interrupted = true;
                Thread.currentThread().interrupt();
            }
        }
        if (!isRegistered()) {
            stop();
            if (interrupted) {
                throw new ClientStartFailedException(
                        "Registration failure: Interrupted");
            } else if (getRegistrationStatus() != null) {
                throw new ClientStartFailedException
                        ("Registration failure: " + getRegistrationStatus());
            } else {
                throw new ClientStartFailedException
                        ("Registration failure: Connection lost. Server full?");
            }
        }
        Log.d("Started client " + this);
    }

    public void stop() {
        Log.d("Stopping client " + this);
        mRegistrationStatus = null;
        if (mMainKey != null) {
            mMainKey.cancel();
        }
        if (mChannel != null && mChannel.isOpen()) {
            try {
                mChannel.close();
            } catch (final IOException e) {
                Log.w("Channel close failed when stopping", e);
            }
        }
        if (mThread.isAlive()) {
            try {
                mThread.join();
            } catch (final InterruptedException e) {
                Log.w("Interrupted in stop", e);
                Thread.currentThread().interrupt();
            }
        }
        mThread.cleanUp();
        Log.d("Stopped client " + this);
    }

    @Override
    public String toString() {
        return getClass().getSimpleName() + "@" + Integer.toHexString(hashCode()) + mClientId;
    }

    void cancel(Packet<? extends Request> packet) {
        mMessenger.cancel(packet);
    }

    ClientSettings getSettings() {
        return mSettings;
    }

    Token getToken() {
        return mToken;
    }

    void send(Packet<? extends Message> packet) throws IOException {
        mMessenger.send(packet);
    }

    private void clearRegistration() {
        mRegistrationStatus = null;
    }

    private void doConnect() throws IOException {
        final InetSocketAddress serverAddress = new InetSocketAddress(mServerAddress, mServerPort);
        mChannel.configureBlocking(false);
        mChannel.connect(serverAddress);
        mChannel.register(mSelector, SelectionKey.OP_CONNECT);

        if (mSelector.select(mSettings.getClientConnectTimeout()) < 1) {
            mChannel.close();
            throw new IOException("Connection timed out");
        }

        // We should have only one key but let's loop anyway
        final Iterator<SelectionKey> keys = mSelector.selectedKeys().iterator();
        while (keys.hasNext()) {
            final SelectionKey key = keys.next();
            keys.remove();
            if (!key.isConnectable()) {
                continue;
            }
            final SocketChannel connectedChannel = (SocketChannel) key.channel();
            if (connectedChannel.isConnectionPending()) {
                connectedChannel.finishConnect();
                connectedChannel.configureBlocking(false);
            }
        }

        // New our channel should be connected
        if (!mChannel.isConnected()) {
            throw new IOException("Connect failed");
        }
        mMessenger = new Messenger<>(mChannel);
        setHandlers();
    }

    private void doSelect() throws IOException {
        mMainKey = mChannel.register(mSelector, SelectionKey.OP_READ);
        mMainKey.attach(mMessenger);
        while (mMainKey.isValid()) {
            mSelector.select(mSettings.getClientSelectTimeout());
            final Iterator<SelectionKey> keys = mSelector.selectedKeys().iterator();
            while (keys.hasNext()) {
                SelectionKey key = keys.next();
                keys.remove();
                if (!key.isReadable()) {
                    continue;
                }
                if (key.attachment() == mMessenger && mMessenger.read() == CLOSE) {
                    Log.v("Client closing: " + this);
                    mChannel.close();
                }
            }
        }
    }

    private RegisterResponse.Code getRegistrationStatus() {
        return mRegistrationStatus;
    }

    private void handleGroupNotify(final Packet<GroupNotify> packet) {
        Log.v("Got group notify: " + packet);
        final NodeId groupId = packet.getMessage().getGroupId();
        final Token groupToken = packet.getMessage().getGroupToken();
        final NodeId[] members = packet.getMessage().getMembers();
        if (mGroups.containsKey(groupId)) {
            final SyncGroup group = mGroups.get(groupId);
            if (group.getToken().equals(groupToken)) {
                group.setMembers(members);
            } else {
                Log.e("Notify to unauthorized group: " + groupId);
            }
        } else {
            Log.e("Notify to unknown group: " + groupId);
        }
    }

    private void handleJoinResponse(final Packet<JoinResponse> packet) {
        Log.v("Got join response: " + packet + ": " + packet.getMessage().getCode());
        final NodeId groupId = packet.getMessage().getGroupId();
        final Token groupToken = packet.getMessage().getGroupToken();
        final JoinResponse.Code result = packet.getMessage().getCode();
        if (mGroups.containsKey(groupId)) {
            final SyncGroup group = mGroups.get(groupId);
            if (group.getToken().equals(groupToken)) {
                group.setJoinStatus(result);
            } else {
                Log.e("Response to unauthorized group: " + groupId);
            }
        } else {
            Log.e("Response to unknown group: " + groupId);
        }
    }

    private void handleLeaveResponse(final Packet<LeaveResponse> packet) {
        Log.v("Got leave response: " + packet + ": " + packet.getMessage().getCode());
        final NodeId groupId = packet.getMessage().getGroupId();
        final Token groupToken = packet.getMessage().getGroupToken();
        if (mGroups.containsKey(groupId)) {
            final SyncGroup group = mGroups.get(groupId);
            if (group.getToken().equals(groupToken)) {
                group.setJoinStatus(null);
            } else {
                Log.e("Response to unauthorized group: " + groupId);
            }
        } else {
            Log.e("Response to unknown group: " + groupId);
        }
    }

    private void handleRegisterResponse(final Packet<RegisterResponse> packet) {
        Log.v("Got register response: " + packet + ": " + packet.getMessage().getCode() +
                " / " + packet.getMessage().getClientToken());
        mToken = packet.getMessage().getClientToken();
        mRegistrationStatus = packet.getMessage().getCode();
        synchronized (this) {
            this.notifyAll();
        }
    }

    private void handleSyncNotify(final Packet<SyncNotify> packet) {
        Log.v("Got sync notify: " + packet);
        final NodeId syncPoint = packet.getMessage().getSyncPoint();
        final SyncNotify.Code code = packet.getMessage().getSyncCode();
        final NodeId groupId = packet.getMessage().getGroupId();
        final Token groupToken = packet.getMessage().getGroupToken();
        if (mGroups.containsKey(groupId)) {
            final SyncGroup group = mGroups.get(groupId);
            if (group.getToken().equals(groupToken)) {
                group.handleSyncPointNotify(syncPoint, code);
            } else {
                Log.e("Sync notify to unauthorized group: " + groupId);
            }
        } else {
            Log.e("Sync notify to unknown group: " + groupId);
        }
    }

    private void handleSyncResponse(Packet<SyncResponse> packet) {
        Log.v("Got sync response: " + packet);
        final NodeId syncPoint = packet.getMessage().getSyncPoint();
        final SyncResponse.Code code = packet.getMessage().getCode();
        final NodeId groupId = packet.getMessage().getGroupId();
        final Token groupToken = packet.getMessage().getGroupToken();
        if (mGroups.containsKey(groupId)) {
            final SyncGroup group = mGroups.get(groupId);
            if (group.getToken().equals(groupToken)) {
                group.handleSyncPointResponse(syncPoint, code);
            } else {
                Log.e("Sync response to unauthorized group: " + groupId);
            }
        } else {
            Log.e("Sync response to unknown group: " + groupId);
        }
    }

    private boolean isRegistering() {
        return null == mRegistrationStatus && mMainLoopRunning;
    }

    private void run() {
        try (
                final SocketChannel channel = SocketChannel.open();
                final Selector selector = Selector.open()
        ) {
            mChannel = channel;
            mSelector = selector;
            doConnect();
            Log.v("Registering client " + this);
            sendRegister();
            Log.v("Main loop start for client " + this);
            doSelect();
            Log.v("Main loop stop for client " + this);
        } catch (final IOException e) {
            Log.e("Got exception from main loop", e);
        } finally {
            mMainLoopRunning = false;
            synchronized (this) {
                notifyAll();
            }
        }
    }

    private void sendRegister() throws IOException {
        Packet<RegisterRequest> message = new Packet<>(RegisterRequest.class, mToken);
        RegisterRequest request = message.getMessage();
        request.setClientId(mClientId);
        send(message);
    }

    private void setHandlers() {
        mMessenger.setHandler(new MessageHandler<RegisterResponse>(RegisterResponse.class) {
            @Override
            public void handleMessage(final Packet<RegisterResponse> packet) {
                handleRegisterResponse(packet);
            }
        });
        mMessenger.setHandler(new MessageHandler<JoinResponse>(JoinResponse.class) {
            @Override
            public void handleMessage(final Packet<JoinResponse> packet) {
                handleJoinResponse(packet);
            }
        });
        mMessenger.setHandler(new MessageHandler<LeaveResponse>(LeaveResponse.class) {
            @Override
            public void handleMessage(final Packet<LeaveResponse> packet) {
                handleLeaveResponse(packet);
            }
        });
        mMessenger.setHandler(new MessageHandler<GroupNotify>(GroupNotify.class) {
            @Override
            public void handleMessage(final Packet<GroupNotify> packet) {
                handleGroupNotify(packet);
            }
        });
        mMessenger.setHandler(new MessageHandler<SyncResponse>(SyncResponse.class) {
            @Override
            public void handleMessage(final Packet<SyncResponse> packet) {
                handleSyncResponse(packet);
            }
        });
        mMessenger.setHandler(new MessageHandler<SyncNotify>(SyncNotify.class) {
            @Override
            public void handleMessage(final Packet<SyncNotify> packet) {
                handleSyncNotify(packet);
            }
        });
    }
}
