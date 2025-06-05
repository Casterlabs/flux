package co.casterlabs.flux.server.daemon.http;

import java.io.IOException;

import co.casterlabs.commons.io.bytes.reading.ArrayByteReader;
import co.casterlabs.commons.io.bytes.writing.ArrayByteWriter;
import co.casterlabs.flux.packets.Packet;
import co.casterlabs.flux.packets.PacketError;
import co.casterlabs.flux.packets.PacketError.Reason;
import co.casterlabs.flux.packets.protocols.BinaryWireProtocol;
import co.casterlabs.flux.packets.protocols.WireProtocolException;
import co.casterlabs.flux.server.Flux;
import co.casterlabs.rhs.protocol.websocket.Websocket;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
class _WSListenerBinary extends _WSListenerBase {
    private final BinaryWireProtocol protocol;

    @Override
    public void handleOutgoing(Packet packet) {
        try {
            long length = this.protocol.sizeOf(packet);
            ArrayByteWriter writer = new ArrayByteWriter((int) length);

            this.protocol.serialize(packet, writer);

            this.websocket.send(writer.buffer());
        } catch (IOException | WireProtocolException ignored) {}
    }

    @Override
    public void onBinary(Websocket websocket, byte[] bytes) throws IOException {
        try {
            Packet packet = this.protocol.parse(new ArrayByteReader(bytes));
            this.client.processIncoming(packet);
        } catch (WireProtocolException e) {
            if (Flux.DEBUG) {
                e.printStackTrace();
            }
            this.handleOutgoing(new PacketError(Reason.PACKET_INVALID));
        }
    }

    @Override
    public void onText(Websocket websocket, String message) throws IOException {
        this.handleOutgoing(new PacketError(Reason.PACKET_INVALID));
    }

}
