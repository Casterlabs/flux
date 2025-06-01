package co.casterlabs.flux.packets.protocols.bytes;

import java.io.IOException;

import co.casterlabs.commons.io.bytes.reading.ByteReader;
import co.casterlabs.commons.io.bytes.writing.ByteWriter;
import co.casterlabs.flux.packets.PacketSubscribe;
import co.casterlabs.flux.packets.types.TubeID;

class _PacketSubscribeMarshall implements _Marshall<PacketSubscribe> {

    @Override
    public PacketSubscribe read(ByteReader reader) throws IOException {
        TubeID tube = new TubeID(_Marshall.varstr16(reader));

        return new PacketSubscribe(tube);
    }

    @Override
    public void write(PacketSubscribe packet, ByteWriter writer) throws IOException {
        _Marshall.varstr16(writer, packet.tube.bytes());
    }

    @Override
    public long sizeOf(PacketSubscribe packet) {
        return 2 + packet.tube.bytes().length; // tube
    }

}
