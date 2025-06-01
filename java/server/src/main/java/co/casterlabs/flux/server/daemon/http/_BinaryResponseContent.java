package co.casterlabs.flux.server.daemon.http;

import java.io.IOException;
import java.io.OutputStream;

import co.casterlabs.commons.io.bytes.writing.ByteWriter;
import co.casterlabs.commons.io.bytes.writing.StreamByteWriter;
import co.casterlabs.flux.packets.Packet;
import co.casterlabs.flux.packets.protocols.BinaryWireProtocol;
import co.casterlabs.flux.packets.protocols.WireProtocolException;
import co.casterlabs.flux.server.Flux;
import co.casterlabs.rhs.protocol.http.HttpResponse.ResponseContent;

record _BinaryResponseContent(
    BinaryWireProtocol protocol,
    Packet packet
) implements ResponseContent {

    @Override
    public void write(int recommendedBufferSize, OutputStream out) throws IOException {
        try {
            ByteWriter writer = new StreamByteWriter(out);
            this.protocol.serialize(this.packet, writer);
        } catch (WireProtocolException e) {
            if (Flux.DEBUG) {
                e.printStackTrace();
            }
        }
    }

    @Override
    public long length() {
        return -1;
    }

    @Override
    public void close() throws IOException {
        // NOOP
    }

}
