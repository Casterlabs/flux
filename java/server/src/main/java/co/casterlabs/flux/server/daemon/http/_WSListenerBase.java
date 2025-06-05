package co.casterlabs.flux.server.daemon.http;

import java.io.IOException;

import co.casterlabs.flux.packets.PacketAck;
import co.casterlabs.flux.packets.PacketError;
import co.casterlabs.flux.packets.PacketError.Reason;
import co.casterlabs.flux.server.Client;
import co.casterlabs.flux.server.Flux;
import co.casterlabs.flux.server.authenticator.Authenticator.AuthenticationException;
import co.casterlabs.rhs.protocol.websocket.Websocket;
import co.casterlabs.rhs.protocol.websocket.WebsocketListener;

abstract class _WSListenerBase implements WebsocketListener, Client.Handle {
    protected Websocket websocket;
    protected Client client;

    @Override
    public void onOpen(Websocket websocket) throws IOException {
        this.websocket = websocket;
        try {
            this.client = new Client(
                websocket.session().uri().query.getSingleOrDefault("authorization", _HTTPHandler.ANONYMOUS_BEARER.raw()),
                this,
                true
            );
            this.handleOutgoing(new PacketAck(this.client.auth.id()));
        } catch (AuthenticationException e) {
            if (Flux.DEBUG) {
                e.printStackTrace();
            }

            this.handleOutgoing(new PacketError(Reason.AUTHENTICATION_FAILED));
            websocket.close();
        }
    }

    @Override
    public void onClose(Websocket websocket) {
        if (this.client != null) {
            this.client.close();
        }
    }

}
