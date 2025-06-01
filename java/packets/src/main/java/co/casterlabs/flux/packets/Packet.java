package co.casterlabs.flux.packets;

import co.casterlabs.rakurai.json.annotating.JsonSerializationMethod;
import co.casterlabs.rakurai.json.element.JsonElement;
import co.casterlabs.rakurai.json.element.JsonString;
import lombok.RequiredArgsConstructor;

public sealed abstract class Packet permits PacketAck, PacketError, PacketKeepAlive, PacketMemberJoin, PacketMemberLeave, PacketMembers, PacketMessage, PacketPublish, PacketSubscribe, PacketUnsubscribe {

    @JsonSerializationMethod("type")
    private JsonElement $serialize_type() {
        return new JsonString(this.type().name());
    }

    public abstract PacketType type();

    @RequiredArgsConstructor
    public static enum PacketType {
        // @formatter:off
          KEEP_ALIVE(0x00), // bidirectional
                 ACK(0x01), // from flux
               ERROR(0x02), // from flux

             MEMBERS(0x10), // from flux
         MEMBER_JOIN(0x11), // from flux
        MEMBER_LEAVE(0x12), // from flux

             MESSAGE(0x20), // from flux
             PUBLISH(0x21), // to flux
           SUBSCRIBE(0x22), // to flux
         UNSUBSCRIBE(0x23), // to flux
        // @formatter:on
        ;

        public static final PacketType[] LUT = new PacketType[0xFF];
        static {
            for (PacketType e : values()) {
                LUT[e.id] = e;
            }
        }

        public final int id;

    }

}
