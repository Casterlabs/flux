package co.casterlabs.flux.server.packet.incoming;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

import co.casterlabs.commons.io.bytes.reading.ByteReader;
import co.casterlabs.flux.server.packet.PacketType;
import co.casterlabs.flux.server.types.TubeID;
import co.casterlabs.rakurai.json.annotating.JsonClass;
import lombok.AllArgsConstructor;
import lombok.ToString;

@ToString
@AllArgsConstructor
@JsonClass(exposeAll = true, unsafeInstantiation = true)
public class TFPacketSubscribe implements ToFluxPacket {
    public final TubeID tube;

    @Override
    public PacketType type() {
        return PacketType.SUBSCRIBE;
    }

    public static TFPacketSubscribe parse(ByteReader reader) throws IOException {
        int tubeLength = reader.be.u16();
        String tube = new String(reader.read(tubeLength), StandardCharsets.UTF_8);

        return new TFPacketSubscribe(new TubeID(tube));
    }

}
