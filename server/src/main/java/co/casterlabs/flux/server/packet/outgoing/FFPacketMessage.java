package co.casterlabs.flux.server.packet.outgoing;

import java.io.IOException;

import co.casterlabs.commons.io.bytes.writing.ByteWriter;
import co.casterlabs.flux.server.packet.PacketType;
import co.casterlabs.flux.server.types.Message;
import co.casterlabs.flux.server.types.TubeID;
import co.casterlabs.flux.server.types.UserID;
import co.casterlabs.rakurai.json.annotating.JsonClass;

@JsonClass(exposeAll = true)
public record FFPacketMessage(
    Message<?> message,
    UserID from,
    TubeID tube,
    PacketType type
) implements FromFluxPacket {

    public FFPacketMessage(Message<?> message, UserID from, TubeID tube) {
        this(message, from, tube, PacketType.MESSAGE);
    }

    @Override
    public int size() {
        return FromFluxPacket.super.size() +
            1 + // type; 0=string, 1=bytes
            4 +
            this.message.bytes().length;
    }

    @Override
    public void serialize(ByteWriter writer) throws IOException {
        FromFluxPacket.super.serialize(writer);

        if (this.message.isBinary()) {
            writer.be.u8(1);
        } else {
            writer.be.u8(0);
        }

        byte[] bytes = this.message.bytes();
        writer.be.s32(bytes.length);
        writer.write(bytes);
    }

}
