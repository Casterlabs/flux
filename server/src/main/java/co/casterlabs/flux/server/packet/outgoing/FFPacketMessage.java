package co.casterlabs.flux.server.packet.outgoing;

import co.casterlabs.flux.server.types.Message;
import co.casterlabs.flux.server.types.TubeID;
import co.casterlabs.flux.server.types.UserID;
import co.casterlabs.rakurai.json.annotating.JsonClass;

@JsonClass(exposeAll = true)
public record FFPacketMessage(
    Message<?> message,
    UserID from,
    TubeID tube,
    Type type
) implements FromFluxPacket {

    public FFPacketMessage(Message<?> message, UserID from, TubeID tube) {
        this(message, from, tube, Type.MESSAGE);
    }

}
