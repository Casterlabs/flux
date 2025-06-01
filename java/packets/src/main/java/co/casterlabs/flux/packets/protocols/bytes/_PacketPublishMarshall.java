package co.casterlabs.flux.packets.protocols.bytes;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

import co.casterlabs.commons.io.bytes.reading.ByteReader;
import co.casterlabs.commons.io.bytes.writing.ByteWriter;
import co.casterlabs.flux.packets.PacketPublish;
import co.casterlabs.flux.packets.types.Message;
import co.casterlabs.flux.packets.types.TubeID;

class _PacketPublishMarshall implements _Marshall<PacketPublish> {

    @Override
    public PacketPublish read(ByteReader reader) throws IOException {
        TubeID tube = new TubeID(_Marshall.varstr16(reader));

        int messageType = reader.be.u8();
        byte[] messageBytes = _Marshall.varstr31(reader);

        Message<?> message;
        if (messageType == 1) {
            message = new Message<>(messageBytes);
        } else {
            String str = new String(messageBytes, StandardCharsets.UTF_8);
            message = new Message<>(str);
        }

        return new PacketPublish(tube, message);
    }

    @Override
    public void write(PacketPublish packet, ByteWriter writer) throws IOException {
        _Marshall.varstr16(writer, packet.tube.bytes());

        if (packet.message.isBinary()) {
            writer.be.u8(1);
        } else {
            writer.be.u8(0);
        }

        _Marshall.varstr31(writer, packet.message.toBytes());
    }

    @Override
    public long sizeOf(PacketPublish packet) {
        return 2 + packet.tube.bytes().length      // tube
            + 1                                    // message type
            + 4 + packet.message.toBytes().length; // message
    }

}
