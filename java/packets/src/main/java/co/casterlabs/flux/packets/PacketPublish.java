package co.casterlabs.flux.packets;

import co.casterlabs.flux.packets.types.Message;
import co.casterlabs.flux.packets.types.TubeID;
import co.casterlabs.rakurai.json.annotating.JsonClass;
import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.ToString;

@ToString
@AllArgsConstructor
@EqualsAndHashCode(callSuper = false)
@JsonClass(exposeAll = true, unsafeInstantiation = true)
public final class PacketPublish extends Packet {
    public final TubeID tube;
    public final Message<?> message;

    @Override
    public PacketType type() {
        return PacketType.PUBLISH;
    }

}
