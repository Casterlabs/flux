package co.casterlabs.flux.server.packet;

import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public enum PacketType {
    KEEP_ALIVE(0x00),
    ACK(0x01),
    ERROR(0x02),

    MEMBERS(0x10),
    MEMBER_JOIN(0x11),
    MEMBER_LEAVE(0x12),

    MESSAGE(0x20),
    PUBLISH(0x21),
    SUBSCRIBE(0x22),
    UNSUBSCRIBE(0x23),
    ;

    public static final PacketType[] LUT = new PacketType[0xFF];
    static {
        for (PacketType e : values()) {
            LUT[e.id] = e;
        }
    }

    public final int id;

}
