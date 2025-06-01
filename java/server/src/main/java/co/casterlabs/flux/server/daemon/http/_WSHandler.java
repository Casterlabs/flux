package co.casterlabs.flux.server.daemon.http;

import co.casterlabs.flux.packets.protocols.BinaryWireProtocol;
import co.casterlabs.flux.packets.protocols.StringWireProtocol;
import co.casterlabs.flux.packets.protocols.WireProtocol;
import co.casterlabs.flux.server.util.Profiler;
import co.casterlabs.flux.server.util.Profiler.Profile;
import co.casterlabs.rhs.HttpStatus.StandardHttpStatus;
import co.casterlabs.rhs.protocol.websocket.WebsocketProtocol.WebsocketHandler;
import co.casterlabs.rhs.protocol.websocket.WebsocketResponse;
import co.casterlabs.rhs.protocol.websocket.WebsocketSession;

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

            if (wireProtocol instanceof StringWireProtocol) {
                return WebsocketResponse.accept(new _WSListenerString((StringWireProtocol) wireProtocol), acceptedProtocol);
            } else {
                return WebsocketResponse.accept(new _WSListenerBinary((BinaryWireProtocol) wireProtocol), acceptedProtocol);
            }
        } finally {
            profile.end();
        }
    }

}
