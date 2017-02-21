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
import java.nio.channels.SocketChannel;

import fi.nuumio.netsync.protocol.MessageHandler;
import fi.nuumio.netsync.protocol.Messenger;
import fi.nuumio.netsync.protocol.message.Message;
import fi.nuumio.netsync.protocol.message.Packet;
import fi.nuumio.netsync.protocol.message.group.JoinRequest;
import fi.nuumio.netsync.protocol.message.group.JoinResponse;
import fi.nuumio.netsync.protocol.message.group.LeaveRequest;
import fi.nuumio.netsync.protocol.message.group.LeaveResponse;
import fi.nuumio.netsync.protocol.message.group.SyncRequest;
import fi.nuumio.netsync.protocol.message.group.SyncResponse;
import fi.nuumio.netsync.protocol.message.service.RegisterRequest;
import fi.nuumio.netsync.protocol.message.service.RegisterResponse;
import fi.nuumio.netsync.util.Log;
import fi.nuumio.netsync.util.NodeId;
import fi.nuumio.netsync.util.Token;

class ClientConnection {
    private final Token mClientToken;
    private final Messenger<? extends Message> mMessenger;
    private final SyncServer mServer;
    private NodeId mClientId;

    ClientConnection(final SyncServer server, final SocketChannel channel) {
        mClientToken = server.generateClientToken();
        mServer = server;
        mMessenger = new Messenger<>(channel);
        mMessenger.setHandler(new MessageHandler<RegisterRequest>(RegisterRequest.class) {
            @Override
            public void handleMessage(final Packet<RegisterRequest> request) {
                handleRegisterRequest(request);
            }
        });
        mMessenger.setHandler(new MessageHandler<JoinRequest>(JoinRequest.class) {
            @Override
            public void handleMessage(final Packet<JoinRequest> request) {
                handleJoinRequest(request);
            }
        });
        mMessenger.setHandler(new MessageHandler<LeaveRequest>(LeaveRequest.class) {
            @Override
            public void handleMessage(final Packet<LeaveRequest> request) {
                handleLeaveRequest(request);
            }
        });
        mMessenger.setHandler(new MessageHandler<SyncRequest>(SyncRequest.class) {
            @Override
            public void handleMessage(final Packet<SyncRequest> request) {
                handleSyncRequest(request);
            }
        });
    }

    @Override
    public String toString() {
        return getClass().getSimpleName() + "@" + Integer.toHexString(hashCode()) + "[" +
                mClientId + "@" + mMessenger + "]";
    }

    void close() {
        mMessenger.close();
    }

    String getAddress() {
        return mMessenger.getRemoteAddressString();
    }

    NodeId getClientId() {
        return mClientId;
    }

    void setClientId(NodeId mClientId) {
        this.mClientId = mClientId;
    }

    Messenger.ReadMessageResult read() throws IOException {
        Log.v("Reading from client: " + this);
        final Messenger.ReadMessageResult result = mMessenger.read();
        Log.v("Result of reading: " + result);
        return result;
    }

    void send(final Packet<? extends Message> packet) {
        try {
            mMessenger.send(packet);
        } catch (final IOException e) {
            Log.e("Message send failed: " + packet + "->" + this, e);
        }
    }

    private void handleJoinRequest(final Packet<JoinRequest> request) {
        final JoinResponse.Code result =
                mServer.handleGroupJoin(ClientConnection.this, request.getMessage());
        final Packet<JoinResponse> packet = new Packet<>(request, mServer.getToken());
        final JoinResponse response = packet.getMessage();
        response.setCode(result);
        send(packet);
    }

    private void handleLeaveRequest(final Packet<LeaveRequest> request) {
        mServer.handleGroupLeave(ClientConnection.this, request.getMessage());
        final Packet<LeaveResponse> packet = new Packet<>(request, mServer.getToken());
        final LeaveResponse response = packet.getMessage();
        response.setCode(LeaveResponse.Code.ACCEPTED);
        send(packet);
    }

    private void handleRegisterRequest(final Packet<RegisterRequest> request) {
        final RegisterResponse.Code code =
                mServer.handleClientRegister(ClientConnection.this, request.getMessage());
        final Packet<RegisterResponse> packet = new Packet<>(request, mServer.getToken());
        final RegisterResponse response = packet.getMessage();
        response.setClientToken(mClientToken);
        response.setCode(code);
        send(packet);
    }

    private void handleSyncRequest(final Packet<SyncRequest> request) {
        SyncServer.ServerSyncResult result =
                mServer.handleSyncRequest(ClientConnection.this, request.getMessage());
        final Packet<SyncResponse> packet = new Packet<>(request, mServer.getToken());
        final SyncResponse response = packet.getMessage();
        response.setCode(result.mCode);
        response.setTimeout(result.mTimeLeft);
        send(packet);
    }
}
