package co.casterlabs.flux.client.realtime.tcp;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.ReentrantLock;

import javax.net.SocketFactory;

import co.casterlabs.commons.io.bytes.EndOfStreamException;
import co.casterlabs.flux.client.realtime.FluxRealtimeClient;
import co.casterlabs.flux.client.realtime.FluxRealtimeClientListener;
import co.casterlabs.flux.client.realtime.FluxTubeListener;
import co.casterlabs.flux.packets.Packet;
import co.casterlabs.flux.packets.PacketError;
import co.casterlabs.flux.packets.PacketError.Reason;
import co.casterlabs.flux.packets.PacketKeepAlive;
import co.casterlabs.flux.packets.PacketMemberJoin;
import co.casterlabs.flux.packets.PacketMemberLeave;
import co.casterlabs.flux.packets.PacketMembers;
import co.casterlabs.flux.packets.PacketMessage;
import co.casterlabs.flux.packets.PacketPublish;
import co.casterlabs.flux.packets.PacketSubscribe;
import co.casterlabs.flux.packets.PacketUnsubscribe;
import co.casterlabs.flux.packets.protocols.BinaryWireProtocol;
import co.casterlabs.flux.packets.protocols.WireProtocolException;
import co.casterlabs.flux.packets.protocols.bytes.ByteWireProtocol;
import co.casterlabs.flux.packets.types.Message;
import co.casterlabs.flux.packets.types.TubeID;
import lombok.NonNull;

class _FluxTcpClient implements FluxRealtimeClient {
    private static final BinaryWireProtocol PROTOCOL = ByteWireProtocol.INSTANCE;
    private static final long TIMEOUT = TimeUnit.SECONDS.toMillis(60);
    private static final TubeID META_TUBE_AUTENTICATE = new TubeID("__flux:authenticate");

    private final ReentrantLock tubeLock = new ReentrantLock();

    private final Map<TubeID, FluxTubeListener> tubeListeners = new HashMap<>();
    private final FluxRealtimeClientListener realtimeListener;

    private final SocketFactory socketFactory;
    private final String hostAddress;
    private final int hostPort;
    private final String token;

    private State state = State.CONNECTED;
    private _SocketConnection socket;

    public _FluxTcpClient(@NonNull FluxTcpClientBuilder config) throws IOException {
        this.realtimeListener = config.realtimeListener();
        this.socketFactory = config.socketFactory();
        this.hostAddress = config.hostAddress();
        this.hostPort = config.hostPort();
        this.token = config.token();

        try {
            this.reconnect();
        } catch (IOException e) {
            // We failed to connect.
            this.close();
            realtimeListener.onException(e);
            realtimeListener.onClose();
            throw e;
        }
    }

    private void read() {
        try {
            while (this.state == State.CONNECTED) {
                Packet packet = PROTOCOL.parse(this.socket.in);
                this.handle(packet);
            }
        } catch (EndOfStreamException e) {
            // Handled by finally{}
        } catch (Throwable t) {
            this.realtimeListener.onException(t);
        } finally {
            try {
                this.socket.close();
            } catch (IOException ignored) {}
            this.socket = null;

            if (this.state == State.CLOSED) {
                // We're done.
                this.realtimeListener.onClose();
                return;
            }

            this.state = State.RECONNECTING;

            Thread.ofPlatform()
                .name("FluxTcpClient.ReconnectThread")
                .daemon(false)
                .start(() -> {
                    while (this.state == State.RECONNECTING) {
                        try {
                            // Retry connection.
                            Thread.sleep(TIMEOUT / 2);
                            reconnect();
                        } catch (Throwable t) {
                            // We failed to connect, try again.
                            this.realtimeListener.onException(t);
                        }
                    }
                });
        }
    }

    private void reconnect() throws IOException {
        if (this.state == State.CLOSED) return;

        Socket socket = this.socketFactory.createSocket();
        socket.setTcpNoDelay(true);
        socket.setSoTimeout((int) TIMEOUT);
        socket.connect(new InetSocketAddress(this.hostAddress, this.hostPort));

        // Success!
        this.socket = new _SocketConnection(socket);

        try {
            send(new PacketPublish(META_TUBE_AUTENTICATE, new Message<>(this.token)));
            this.state = State.CONNECTED;
        } catch (WireProtocolException e) {
            throw new IOException(e);
        }

        Thread.ofPlatform()
            .name("FluxTcpClient.ReadThread")
            .daemon(false)
            .start(this::read);
    }

    @Override
    public FluxRealtimeClient publish(@NonNull String tube, @NonNull String message) {
        if (this.state != State.CONNECTED) {
            return this; // NOOP
        }

        try {
            TubeID tubeId = new TubeID(tube);

            send(new PacketPublish(tubeId, new Message<>(message)));
        } catch (WireProtocolException | IOException e) {
            this.realtimeListener.onException(e);
        }
        return this;
    }

    @Override
    public FluxRealtimeClient publish(@NonNull String tube, @NonNull byte[] message) {
        if (this.state != State.CONNECTED) {
            return this; // NOOP
        }

        try {
            TubeID tubeId = new TubeID(tube);

            send(new PacketPublish(tubeId, new Message<>(message)));
        } catch (WireProtocolException | IOException e) {
            this.realtimeListener.onException(e);
        }
        return this;
    }

    @Override
    public FluxRealtimeClient subscribe(@NonNull String tube, @NonNull FluxTubeListener listener) {
        tubeLock.lock();
        try {
            switch (this.state) {
                case CLOSED:
                    break; // NOOP

                case CONNECTED:
                    try {
                        TubeID tubeId = new TubeID(tube);

                        if (this.tubeListeners.containsKey(tubeId)) {
                            // ONLY replace the listener.
                            this.tubeListeners.put(tubeId, listener);
                            return this;
                        }

                        this.tubeListeners.put(tubeId, listener);
                        send(new PacketSubscribe(tubeId));
                    } catch (WireProtocolException | IOException e) {
                        this.realtimeListener.onException(e);
                    }
                    break;

                case RECONNECTING: {
                    TubeID tubeId = new TubeID(tube);
                    this.tubeListeners.put(tubeId, listener);
                    break;
                }

            }
            return this;
        } finally {
            tubeLock.unlock();
        }
    }

    @Override
    public FluxRealtimeClient unsubscribe(@NonNull String tube) {
        tubeLock.lock();
        try {
            switch (this.state) {
                case CLOSED:
                    break; // NOOP

                case CONNECTED:
                    try {
                        TubeID tubeId = new TubeID(tube);

                        this.tubeListeners.remove(tubeId);
                        send(new PacketUnsubscribe(tubeId));
                    } catch (WireProtocolException | IOException e) {
                        this.realtimeListener.onException(e);
                    }
                    break;

                case RECONNECTING: {
                    TubeID tubeId = new TubeID(tube);
                    this.tubeListeners.remove(tubeId);
                    break;
                }
            }
            return this;
        } finally {
            tubeLock.unlock();
        }
    }

    @Override
    public void close() throws IOException {
        if (this.state == State.CLOSED) {
            return; // NOOP
        }

        this.state = State.CLOSED;
        if (this.socket != null) this.socket.close();
    }

    private void send(Packet packet) throws WireProtocolException, IOException {
        PROTOCOL.serialize(packet, this.socket.out);
    }

    private void handle(Packet packet) throws WireProtocolException, IOException {
        switch (packet.type()) {
            case ACK:
                tubeLock.lock();
                try {
                    for (TubeID id : tubeListeners.keySet()) {
                        send(new PacketSubscribe(id));
                    }
                } finally {
                    tubeLock.unlock();
                }
                return;

            case KEEP_ALIVE:
                send(PacketKeepAlive.INSTANCE);
                return;

            case ERROR: {
                PacketError error = (PacketError) packet;

                if (error.reason == Reason.AUTHENTICATION_FAILED) {
                    close();
                }

                realtimeListener.onError(error.reason);
                return;
            }

            case MEMBERS: {
                PacketMembers members = (PacketMembers) packet;
                FluxTubeListener listener = tubeListeners.get(members.tube);

                if (listener == null) {
                    return;
                }

                String[] memberIds = new String[members.members.length];
                for (int idx = 0; idx < memberIds.length; idx++) {
                    memberIds[idx] = members.members[idx].toString();
                }

                listener.onMembers(memberIds);
                return;
            }

            case MEMBER_JOIN: {
                PacketMemberJoin join = (PacketMemberJoin) packet;
                FluxTubeListener listener = tubeListeners.get(join.tube);

                if (listener == null) {
                    return;
                }

                listener.onMemberJoin(join.member.toString());
                return;
            }

            case MEMBER_LEAVE: {
                PacketMemberLeave leave = (PacketMemberLeave) packet;
                FluxTubeListener listener = tubeListeners.get(leave.tube);

                if (listener == null) {
                    return;
                }

                listener.onMemberLeave(leave.member.toString());
                return;
            }

            case MESSAGE: {
                PacketMessage message = (PacketMessage) packet;
                FluxTubeListener listener = tubeListeners.get(message.tube);

                if (listener == null) {
                    return;
                }

                if (message.message.isBinary()) {
                    listener.onBinaryMessage(message.from.toString(), message.message.asBinary());
                } else {
                    listener.onStringMessage(message.from.toString(), message.message.asString());
                }
                return;
            }

            default: // Unhandled.
                return;
        }
    }

    private static enum State {
        CONNECTED,
        RECONNECTING,
        CLOSED
    }

}
