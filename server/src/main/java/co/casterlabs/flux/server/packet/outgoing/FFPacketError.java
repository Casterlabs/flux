package co.casterlabs.flux.server.packet.outgoing;

import java.io.IOException;

import co.casterlabs.commons.io.bytes.writing.ByteWriter;
import co.casterlabs.flux.server.Flux;
import co.casterlabs.flux.server.packet.PacketType;
import co.casterlabs.flux.server.types.TubeID;
import co.casterlabs.flux.server.types.UserID;
import co.casterlabs.rakurai.json.annotating.JsonClass;
import lombok.RequiredArgsConstructor;

@JsonClass(exposeAll = true)
public record FFPacketError(
    Reason reason,
    UserID from,
    TubeID tube,
    PacketType type
) implements FromFluxPacket {

    public FFPacketError(Reason reason, TubeID tube) {
        this(reason, Flux.CONTROL_PUBLISHER, tube, PacketType.ERROR);
    }

    @Override
    public int size() {
        return FromFluxPacket.super.size() + 1;
    }

    @Override
    public void serialize(ByteWriter writer) throws IOException {
        writer.be.u8(this.reason.id);
    }

    @RequiredArgsConstructor
    public static enum Reason {
        PACKET_INVALID(0x00),
        SERVER_INTERNAL_ERROR(0x01),

        AUTHENTICATION_FAILED(0x10),
        SUBSCRIPTION_FAILED_PERMISSIONS(0x11),

        PUBLISH_FAILED_PERMISSIONS(0x20),
        PUBLISH_FAILED_NO_RECIPIENT(0x21),
        PUBLISH_FAILED_META_TUBE(0x22),
        ;

        public static final Reason[] LUT = new Reason[0xFF];
        static {
            for (Reason e : values()) {
                LUT[e.id] = e;
            }
        }

        public final int id;

    }

}
