package co.casterlabs.flux.packets.protocols.bytes;

import java.io.IOException;

import co.casterlabs.commons.io.bytes.reading.ByteReader;
import co.casterlabs.commons.io.bytes.writing.ByteWriter;
import co.casterlabs.flux.packets.PacketUnsubscribe;
import co.casterlabs.flux.packets.types.TubeID;

class _PacketUnsubscribeMarshall implements _Marshall<PacketUnsubscribe> {

    @Override
    public PacketUnsubscribe read(ByteReader reader) throws IOException {
        TubeID tube = new TubeID(_Marshall.varstr16(reader));

        return new PacketUnsubscribe(tube);
    }

    @Override
    public void write(PacketUnsubscribe packet, ByteWriter writer) throws IOException {
        _Marshall.varstr16(writer, packet.tube.bytes());
    }

}
