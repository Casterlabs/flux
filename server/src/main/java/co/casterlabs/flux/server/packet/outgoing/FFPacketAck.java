package co.casterlabs.flux.server.packet.outgoing;

import co.casterlabs.flux.server.Flux;
import co.casterlabs.flux.server.types.TubeID;
import co.casterlabs.flux.server.types.UserID;
import co.casterlabs.rakurai.json.annotating.JsonClass;

@JsonClass(exposeAll = true)
public record FFPacketAck(
    UserID from,
    TubeID tube,
    Type type
) implements FromFluxPacket {

    public static final FFPacketAck INSTANCE = new FFPacketAck(
        Flux.CONTROL_PUBLISHER,
        Flux.CONTROL_TUBEID,
        Type.ACK
    );

}
