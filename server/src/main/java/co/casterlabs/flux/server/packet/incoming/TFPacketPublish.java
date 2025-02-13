package co.casterlabs.flux.server.packet.incoming;

import co.casterlabs.flux.server.types.TubeID;
import co.casterlabs.rakurai.json.annotating.JsonClass;
import lombok.ToString;

@ToString
@JsonClass(exposeAll = true)
public class TFPacketPublish implements ToFluxPacket {
    public final String message = null;
    public final TubeID tube = null;

    @Override
    public Type type() {
        return Type.PUBLISH;
    }

}
