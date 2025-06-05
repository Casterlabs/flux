package co.casterlabs.flux.packets.protocols.bytes;

import java.io.IOException;

import co.casterlabs.commons.io.bytes.reading.ByteReader;
import co.casterlabs.commons.io.bytes.writing.ByteWriter;
import co.casterlabs.flux.packets.PacketAck;
import co.casterlabs.flux.packets.types.UserID;

class _PacketAckMarshall implements _Marshall<PacketAck> {

    @Override
    public PacketAck read(ByteReader reader) throws IOException {
        UserID id = new UserID(_Marshall.varstr16(reader));
        return new PacketAck(id);
    }

    @Override
    public void write(PacketAck packet, ByteWriter writer) throws IOException {
        _Marshall.varstr16(writer, packet.id.bytes());
    }

}
