package co.casterlabs.flux.server.packet.incoming;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

import co.casterlabs.commons.io.bytes.reading.ByteReader;
import co.casterlabs.flux.server.packet.PacketType;
import co.casterlabs.flux.server.types.Message;
import co.casterlabs.flux.server.types.TubeID;
import co.casterlabs.rakurai.json.annotating.JsonClass;
import lombok.AllArgsConstructor;
import lombok.ToString;

@ToString
@AllArgsConstructor
@JsonClass(exposeAll = true, unsafeInstantiation = true)
public class TFPacketPublish implements ToFluxPacket {
    public final TubeID tube;
    public final Message<?> message;

    @Override
    public PacketType type() {
        return PacketType.PUBLISH;
    }

    public static TFPacketPublish parse(ByteReader reader) throws IOException {
        int tubeLength = reader.be.u16();
        String tube = new String(reader.read(tubeLength), StandardCharsets.UTF_8);

        int messageType = reader.be.u8(); // 0=string, 1=bytes
        int messageLength = reader.be.s32();
        byte[] messageBytes = reader.read(messageLength);

        Message<?> message;
        if (messageType == 0) {
            String str = new String(messageBytes, StandardCharsets.UTF_8);
            message = new Message<>(str);
        } else {
            message = new Message<>(messageBytes);
        }

        return new TFPacketPublish(new TubeID(tube), message);
    }

}
