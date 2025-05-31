package co.casterlabs.flux.server.packet.outgoing;

import java.io.IOException;

import co.casterlabs.commons.io.bytes.writing.ByteWriter;
import co.casterlabs.flux.server.Flux;
import co.casterlabs.flux.server.packet.PacketType;
import co.casterlabs.flux.server.types.TubeID;
import co.casterlabs.flux.server.types.UserID;
import co.casterlabs.rakurai.json.annotating.JsonClass;

@JsonClass(exposeAll = true)
public record FFPacketMembers(
    int count,
    UserID from,
    TubeID tube,
    PacketType type
) implements FromFluxPacket {

    public FFPacketMembers(int count, TubeID tube) {
        this(count, Flux.CONTROL_PUBLISHER, tube, PacketType.MEMBERS);
    }

    @Override
    public int size() {
        return FromFluxPacket.super.size() + 4;
    }

    @Override
    public void serialize(ByteWriter writer) throws IOException {
        FromFluxPacket.super.serialize(writer);

        writer.be.s32(this.count);
    }

}
