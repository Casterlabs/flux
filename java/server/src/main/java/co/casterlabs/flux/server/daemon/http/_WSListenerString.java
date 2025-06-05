package co.casterlabs.flux.server.daemon.http;

import java.io.IOException;

import co.casterlabs.flux.packets.Packet;
import co.casterlabs.flux.packets.PacketError;
import co.casterlabs.flux.packets.PacketError.Reason;
import co.casterlabs.flux.packets.protocols.StringWireProtocol;
import co.casterlabs.flux.packets.protocols.WireProtocolException;
import co.casterlabs.flux.server.Flux;
import co.casterlabs.rhs.protocol.websocket.Websocket;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
class _WSListenerString extends _WSListenerBase {
    private final StringWireProtocol protocol;

    @Override
    public void handleOutgoing(Packet packet) {
        try {
            String serialized = this.protocol.serialize(packet);
            this.websocket.send(serialized);
        } catch (IOException | WireProtocolException e) {
            if (Flux.DEBUG) {
                e.printStackTrace();
            }
        }
    }

    @Override
    public void onBinary(Websocket websocket, byte[] bytes) throws IOException {
        this.handleOutgoing(new PacketError(Reason.PACKET_INVALID));
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
            this.handleOutgoing(new PacketError(Reason.PACKET_INVALID));
        }
    }

}
