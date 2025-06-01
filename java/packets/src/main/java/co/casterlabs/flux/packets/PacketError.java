package co.casterlabs.flux.packets;

import co.casterlabs.rakurai.json.annotating.JsonClass;
import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.RequiredArgsConstructor;
import lombok.ToString;

@ToString
@AllArgsConstructor
@EqualsAndHashCode(callSuper = false)
@JsonClass(exposeAll = true, unsafeInstantiation = true)
public final class PacketError extends Packet {
    public final Reason reason;

    @Override
    public PacketType type() {
        return PacketType.ERROR;
    }

    @RequiredArgsConstructor
    public static enum Reason {
        // Protocol
        PACKET_INVALID(0x00),
        SERVER_INTERNAL_ERROR(0x01),

        // Authentication
        AUTHENTICATION_FAILED(0x10),
        SUBSCRIPTION_FAILED_PERMISSIONS(0x11),
        PUBLISH_FAILED_PERMISSIONS(0x12),

        // Publishing
        PUBLISH_FAILED_NO_RECIPIENT(0x20),
        PUBLISH_FAILED_META_TUBE(0x21),
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
