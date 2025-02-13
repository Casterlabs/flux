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
import co.casterlabs.flux.server.protocols.WireProtocol;
import co.casterlabs.flux.server.protocols.WireProtocol.WireProtocolException;
import co.casterlabs.flux.server.util.Profiler;
import co.casterlabs.flux.server.util.Profiler.Profile;
import co.casterlabs.rhs.HttpStatus.StandardHttpStatus;
import co.casterlabs.rhs.protocol.websocket.Websocket;
import co.casterlabs.rhs.protocol.websocket.WebsocketListener;
import co.casterlabs.rhs.protocol.websocket.WebsocketProtocol.WebsocketHandler;
import co.casterlabs.rhs.protocol.websocket.WebsocketResponse;
import co.casterlabs.rhs.protocol.websocket.WebsocketSession;
import lombok.RequiredArgsConstructor;

class _WSHandler implements WebsocketHandler {

    @Override
    public WebsocketResponse handle(WebsocketSession session) {
        Profile profile = Profiler.DAEMON_HTTP_WEBSOCKET_CONNECT.start();

        try {
            String acceptedProtocol = null;
            WireProtocol wireProtocol = null;
            if (session.acceptedProtocols().isEmpty()) {
                wireProtocol = WireProtocol.TYPES.get(WireProtocol.Type.JSON);
            } else {
                for (String p : session.acceptedProtocols()) {
                    try {
                        wireProtocol = WireProtocol.TYPES.get(WireProtocol.Type.valueOf(p.toUpperCase()));
                        acceptedProtocol = p;
                    } catch (Throwable ignored) {}
                }

                if (acceptedProtocol == null || wireProtocol == null) {
                    return WebsocketResponse.reject(StandardHttpStatus.BAD_REQUEST);
                }
            }

            return WebsocketResponse.accept(new Listener(wireProtocol), acceptedProtocol);
        } finally {
            profile.end();
        }
    }

    @RequiredArgsConstructor
    private static class Listener implements WebsocketListener {
        private static final ThreadFactory THREAD_FACTORY = Thread.ofVirtual().name("HTTP Daemon - WebSocket - #", 0).factory();

        private final WireProtocol protocol;
        private final ExecutorService executor = Executors.newSingleThreadExecutor(THREAD_FACTORY);

        private Websocket websocket;
        private Client client;

        private void handle(FromFluxPacket packet) throws IOException {
            this.executor.submit(() -> {
                try {
                    Object /* String or byte[] */ serialized = this.protocol.serialize(packet);
                    if (serialized instanceof byte[]) {
                        this.websocket.send((byte[]) serialized);
                    } else {
                        this.websocket.send((String) serialized);
                    }
                } catch (IOException ignored) {}
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
                websocket.close();
            }
        }

        @Override
        public void onBinary(Websocket websocket, byte[] bytes) throws IOException {
            try {
                ToFluxPacket packet = this.protocol.parse(bytes);
                this.client.processIncoming(packet);
            } catch (WireProtocolException e) {
                if (Flux.DEBUG) {
                    e.printStackTrace();
                }
                this.handle(new FFPacketError(FFPacketError.Reason.PACKET_INVALID, Flux.CONTROL_TUBEID));
            }
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
            this.client.close();
            this.executor.shutdownNow();
        }

    }

}
