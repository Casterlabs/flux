package co.casterlabs.flux.packets.protocols.bytes;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

import co.casterlabs.commons.io.bytes.reading.ByteReader;
import co.casterlabs.commons.io.bytes.writing.ByteWriter;
import co.casterlabs.flux.packets.PacketMessage;
import co.casterlabs.flux.packets.types.Message;
import co.casterlabs.flux.packets.types.TubeID;
import co.casterlabs.flux.packets.types.UserID;

class _PacketMessageMarshall implements _Marshall<PacketMessage> {

    @Override
    public PacketMessage read(ByteReader reader) throws IOException {
        TubeID tube = new TubeID(_Marshall.varstr16(reader));
        UserID from = new UserID(_Marshall.varstr16(reader));

        int messageType = reader.be.u8();
        byte[] messageBytes = _Marshall.varstr31(reader);

        Message<?> message;
        if (messageType == 1) {
            message = new Message<>(messageBytes);
        } else {
            String str = new String(messageBytes, StandardCharsets.UTF_8);
            message = new Message<>(str);
        }

        return new PacketMessage(tube, from, message);
    }

    @Override
    public void write(PacketMessage packet, ByteWriter writer) throws IOException {
        _Marshall.varstr16(writer, packet.tube.bytes());
        _Marshall.varstr16(writer, packet.from.bytes());

        if (packet.message.isBinary()) {
            writer.be.u8(1);
        } else {
            writer.be.u8(0);
        }

        _Marshall.varstr31(writer, packet.message.toBytes());
    }

}
