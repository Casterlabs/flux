package co.casterlabs.flux.server.packet.outgoing;

import co.casterlabs.flux.server.Flux;
import co.casterlabs.flux.server.types.TubeID;
import co.casterlabs.flux.server.types.UserID;
import co.casterlabs.rakurai.json.annotating.JsonClass;

@JsonClass(exposeAll = true)
public record FFPacketMemberJoin(
    UserID member,
    UserID from,
    TubeID tube,
    Type type
) implements FromFluxPacket {

    public FFPacketMemberJoin(UserID member, TubeID tube) {
        this(member, Flux.CONTROL_PUBLISHER, tube, Type.MEMBER_JOIN);
    }

}
