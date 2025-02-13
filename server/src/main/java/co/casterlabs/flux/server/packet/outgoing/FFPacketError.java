package co.casterlabs.flux.server.packet.outgoing;

import co.casterlabs.flux.server.Flux;
import co.casterlabs.flux.server.types.TubeID;
import co.casterlabs.flux.server.types.UserID;
import co.casterlabs.rakurai.json.annotating.JsonClass;

@JsonClass(exposeAll = true)
public record FFPacketError(
    Reason reason,
    UserID from,
    TubeID tube,
    Type type
) implements FromFluxPacket {

    public FFPacketError(Reason reason, TubeID tube) {
        this(reason, Flux.CONTROL_PUBLISHER, tube, Type.ERROR);
    }

    public static enum Reason {
        AUTHENTICATION_FAILED,

        SERVER_INTERNAL_ERROR,

        SUBSCRIPTION_FAILED_PERMISSIONS,

        PACKET_INVALID,

        PUBLISH_FAILED_PERMISSIONS,
        PUBLISH_FAILED_NO_RECIPIENT,
        PUBLISH_FAILED_META_TUBE,
    }

}
