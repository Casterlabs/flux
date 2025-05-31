package co.casterlabs.flux.server.daemon.http;

import java.io.IOException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;

import co.casterlabs.flux.server.Client;
import co.casterlabs.flux.server.Flux;
import co.casterlabs.flux.server.authenticator.Authenticator.AuthenticationException;
import co.casterlabs.flux.server.packet.incoming.ToFluxPacket;
import co.casterlabs.flux.server.packet.outgoing.FFPacketAck;
import co.casterlabs.flux.server.packet.outgoing.FFPacketError;
import co.casterlabs.flux.server.packet.outgoing.FromFluxPacket;
import co.casterlabs.flux.server.protocols.StringWireProtocol;
import co.casterlabs.flux.server.protocols.WireProtocol.WireProtocolException;
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

    private void handle(FromFluxPacket packet) throws IOException {
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
            this.handle(FFPacketAck.INSTANCE);
        } catch (AuthenticationException e) {
            if (Flux.DEBUG) {
                e.printStackTrace();
            }

            this.handle(
                new FFPacketError(
                    FFPacketError.Reason.AUTHENTICATION_FAILED,
                    Flux.CONTROL_TUBEID
                )
            );

            for (Runnable r : this.executor.shutdownNow()) {
                r.run();
            }
            websocket.close();
        }
    }

    @Override
    public void onBinary(Websocket websocket, byte[] bytes) throws IOException {
        this.handle(new FFPacketError(FFPacketError.Reason.PACKET_INVALID, Flux.CONTROL_TUBEID));
    }

    @Override
    public void onText(Websocket websocket, String message) throws IOException {
        try {
            ToFluxPacket packet = this.protocol.parse(message);
            this.client.processIncoming(packet);
        } catch (WireProtocolException e) {
            if (Flux.DEBUG) {
                e.printStackTrace();
            }
            this.handle(new FFPacketError(FFPacketError.Reason.PACKET_INVALID, Flux.CONTROL_TUBEID));
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
