package co.casterlabs.flux.server.packet.outgoing;

import co.casterlabs.flux.server.types.TubeID;
import co.casterlabs.flux.server.types.UserID;
import co.casterlabs.rakurai.json.annotating.JsonClass;

@JsonClass(exposeAll = true)
public record FFPacketMessage(
    String message,
    UserID from,
    TubeID tube,
    Type type
) implements FromFluxPacket {

    public FFPacketMessage(String message, UserID from, TubeID tube) {
        this(message, from, tube, Type.MESSAGE);
    }

}
