package co.casterlabs.flux.packets;

import co.casterlabs.flux.packets.types.UserID;
import co.casterlabs.rakurai.json.annotating.JsonClass;
import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.ToString;

@ToString
@AllArgsConstructor
@EqualsAndHashCode(callSuper = false)
@JsonClass(exposeAll = true, unsafeInstantiation = true)
public final class PacketAck extends Packet {
    public final UserID id;

    @Override
    public PacketType type() {
        return PacketType.ACK;
    }

}
