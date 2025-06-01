package co.casterlabs.flux.packets.protocols.bytes;

import java.io.IOException;

import co.casterlabs.commons.io.bytes.reading.ByteReader;
import co.casterlabs.commons.io.bytes.writing.ByteWriter;
import co.casterlabs.flux.packets.PacketError;
import co.casterlabs.flux.packets.PacketError.Reason;

class _PacketErrorMarshall implements _Marshall<PacketError> {

    @Override
    public PacketError read(ByteReader reader) throws IOException {
        Reason reason = Reason.LUT[reader.be.u8()];

        return new PacketError(reason);
    }

    @Override
    public void write(PacketError packet, ByteWriter writer) throws IOException {
        writer.be.u8(packet.reason.id);
    }

    @Override
    public long sizeOf(PacketError packet) {
        return 1; // reason
    }

}
