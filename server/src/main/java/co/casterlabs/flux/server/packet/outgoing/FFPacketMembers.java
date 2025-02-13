package co.casterlabs.flux.server.packet.outgoing;

import co.casterlabs.flux.server.Flux;
import co.casterlabs.flux.server.types.TubeID;
import co.casterlabs.flux.server.types.UserID;
import co.casterlabs.rakurai.json.annotating.JsonClass;

@JsonClass(exposeAll = true)
public record FFPacketMembers(
    int count,
    UserID from,
    TubeID tube,
    Type type
) implements FromFluxPacket {

    public FFPacketMembers(int count, TubeID tube) {
        this(count, Flux.CONTROL_PUBLISHER, tube, Type.MEMBERS);
    }

}
