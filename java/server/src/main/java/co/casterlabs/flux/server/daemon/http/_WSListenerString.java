package co.casterlabs.flux.server.daemon.http;

import java.io.IOException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;

import co.casterlabs.flux.packets.Packet;
import co.casterlabs.flux.packets.PacketAck;
import co.casterlabs.flux.packets.PacketError;
import co.casterlabs.flux.packets.PacketError.Reason;
import co.casterlabs.flux.packets.protocols.StringWireProtocol;
import co.casterlabs.flux.packets.protocols.WireProtocolException;
import co.casterlabs.flux.server.Client;
import co.casterlabs.flux.server.Flux;
import co.casterlabs.flux.server.authenticator.Authenticator.AuthenticationException;
import co.casterlabs.rhs.protocol.websocket.Websocket;
import co.casterlabs.rhs.protocol.websocket.WebsocketListener;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
class _WSListenerString implements WebsocketListener {
    private static final ThreadFactory THREAD_FACTORY = Thread.ofVirtual().name("HTTP Daemon - WebSocket - #", 0).factory();

    private final StringWireProtocol protocol;
    private final ExecutorService executor = Executors.newSingleThreadExecutor(THREAD_FACTORY);

    private Websocket websocket;
    private Client client;

    private void handle(Packet packet) throws IOException {
        this.executor.submit(() -> {
            try {
                String serialized = this.protocol.serialize(packet);
                this.websocket.send(serialized);
            } catch (IOException | WireProtocolException e) {
                if (Flux.DEBUG) {
                    e.printStackTrace();
                }
            }
        });
    }

    @Override
    public void onOpen(Websocket websocket) throws IOException {
        this.websocket = websocket;
        try {
            this.client = new Client(
                websocket.session().uri().query.getSingleOrDefault("authorization", _HTTPHandler.ANONYMOUS_BEARER.raw()),
                this::handle,
                true
            );
            this.handle(new PacketAck(this.client.auth.id()));
        } catch (AuthenticationException e) {
            if (Flux.DEBUG) {
                e.printStackTrace();
            }

            this.handle(new PacketError(Reason.AUTHENTICATION_FAILED));

            for (Runnable r : this.executor.shutdownNow()) {
                r.run();
            }
            websocket.close();
        }
    }

    @Override
    public void onBinary(Websocket websocket, byte[] bytes) throws IOException {
        this.handle(new PacketError(Reason.PACKET_INVALID));
    }

    @Override
    public void onText(Websocket websocket, String message) throws IOException {
        try {
            Packet packet = this.protocol.parse(message);
            this.client.processIncoming(packet);
        } catch (WireProtocolException e) {
            if (Flux.DEBUG) {
                e.printStackTrace();
            }
            this.handle(new PacketError(Reason.PACKET_INVALID));
        }
    }

    @Override
    public void onClose(Websocket websocket) {
        if (this.client != null) {
            this.client.close();
        }
        this.executor.shutdownNow();
    }

}
