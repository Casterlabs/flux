package co.casterlabs.flux.server.packet.incoming;

import co.casterlabs.flux.server.packet.PacketType;
import co.casterlabs.rakurai.json.annotating.JsonClass;
import lombok.ToString;

@ToString
@JsonClass(exposeAll = true)
public class TFPacketKeepAlive implements ToFluxPacket {
    public static final TFPacketKeepAlive INSTANCE = new TFPacketKeepAlive();

    @Override
    public PacketType type() {
        return PacketType.KEEP_ALIVE;
    }

}
