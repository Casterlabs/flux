package co.casterlabs.flux.server.packet.incoming;

import co.casterlabs.rakurai.json.annotating.JsonClass;
import lombok.ToString;

@ToString
@JsonClass(exposeAll = true)
public class TFPacketKeepAlive implements ToFluxPacket {
    public static final TFPacketKeepAlive INSTANCE = new TFPacketKeepAlive();

    @Override
    public Type type() {
        return Type.KEEP_ALIVE;
    }

}
