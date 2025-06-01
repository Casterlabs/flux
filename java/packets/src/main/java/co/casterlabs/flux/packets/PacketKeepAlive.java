package co.casterlabs.flux.packets;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import lombok.ToString;

@ToString
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class PacketKeepAlive extends Packet {
    public static final PacketKeepAlive INSTANCE = new PacketKeepAlive();

    @Override
    public PacketType type() {
        return PacketType.KEEP_ALIVE;
    }

}
