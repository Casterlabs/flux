package co.casterlabs.flux.client.realtime.websocket;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.locks.ReentrantLock;
import java.util.function.Function;

import org.jetbrains.annotations.Nullable;

import co.casterlabs.commons.io.bytes.reading.ArrayByteReader;
import co.casterlabs.commons.io.bytes.writing.ArrayByteWriter;
import co.casterlabs.commons.websocket.WebSocketClient;
import co.casterlabs.commons.websocket.WebSocketListener;
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
import co.casterlabs.flux.packets.protocols.StringWireProtocol;
import co.casterlabs.flux.packets.protocols.WireProtocol;
import co.casterlabs.flux.packets.protocols.WireProtocolException;
import co.casterlabs.flux.packets.types.Message;
import co.casterlabs.flux.packets.types.TubeID;
import lombok.NonNull;

class _FluxWebsocketClient implements FluxRealtimeClient {
    private final ReentrantLock tubeLock = new ReentrantLock();

    private final Map<TubeID, FluxTubeListener> tubeListeners = new HashMap<>();
    private final WebSocketListener wsListener = new WSListener();
    private final FluxRealtimeClientListener realtimeListener;

    private final Function<WireProtocol, WebSocketClient> websocketFactory;
    private final WireProtocol protocol;
    private final long timeout;

    private State state = State.CONNECTED;
    private WebSocketClient websocket;

    public _FluxWebsocketClient(@NonNull FluxWebsocketClientBuilder config) throws IOException {
        this.realtimeListener = config.realtimeListener();
        this.websocketFactory = config.websocketFactory();
        this.protocol = WireProtocol.TYPES.get(config.protocol());
        this.timeout = config.timeout();

        try {
            this.reconnect();
        } catch (IOException e) {
            // We failed to connect.
            realtimeListener.onException(e);
            realtimeListener.onClose();
            throw e;
        }
    }

    private void reconnect() throws IOException {
        if (this.state == State.CLOSED) return;

        this.websocket = this.websocketFactory.apply(this.protocol);
        this.websocket.setListener(this.wsListener);
        this.websocket.connect(this.timeout, this.timeout / 2);
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
        if (this.websocket != null) this.websocket.close();
    }

    private void send(Packet packet) throws WireProtocolException, IOException {
        if (this.protocol instanceof StringWireProtocol str) {
            String payload = str.serialize(packet);
            this.websocket.send(payload);
        } else {
            BinaryWireProtocol bin = (BinaryWireProtocol) this.protocol;

            ArrayByteWriter dest = new ArrayByteWriter();
            bin.serialize(packet, dest);

            this.websocket.send(dest.buffer());
        }
    }

    private class WSListener implements WebSocketListener {

        @Override
        public void onOpen(WebSocketClient client, Map<String, String> headers, @Nullable String acceptedProtocol) {
            if (state == State.CLOSED) {
                client.close(); // Just in case...
            }
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

        @Override
        public void onText(WebSocketClient client, String string) {
            try {
                if (protocol instanceof StringWireProtocol str) {
                    Packet packet = str.parse(string);
                    handle(packet);
                } else {
                    throw new IllegalArgumentException("Cannot handle text frames from a non-string protocol.");
                }
            } catch (Throwable t) {
                realtimeListener.onException(t);
            }
        }

        @Override
        public void onBinary(WebSocketClient client, byte[] bytes) {
            try {
                if (protocol instanceof BinaryWireProtocol bin) {
                    Packet packet = bin.parse(new ArrayByteReader(bytes));
                    handle(packet);
                } else {
                    throw new IllegalArgumentException("Cannot handle binary frames from a non-binary protocol.");
                }
            } catch (Throwable t) {
                realtimeListener.onException(t);
            }
        }

        @Override
        public void onClosed(WebSocketClient client) {
            if (state == State.RECONNECTING) return;

            if (state == State.CLOSED) {
                // We're done.
                realtimeListener.onClose();
                return;
            }

            state = State.RECONNECTING;

            Thread.ofPlatform()
                .name("FluxWebsocketClient.ReconnectThread")
                .daemon(false)
                .start(() -> {
                    while (state == State.RECONNECTING) {
                        try {
                            // Retry connection.
                            Thread.sleep(timeout / 2);
                            reconnect();
                            state = State.CONNECTED;
                        } catch (Throwable t) {
                            // We failed to connect, try again.
                            realtimeListener.onException(t);
                        }
                    }
                });
        }

    }

    private static enum State {
        CONNECTED,
        RECONNECTING,
        CLOSED
    }

}
