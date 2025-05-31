package co.casterlabs.flux.server.packet.outgoing;

import java.io.IOException;

import co.casterlabs.commons.io.bytes.writing.ByteWriter;
import co.casterlabs.flux.server.Flux;
import co.casterlabs.flux.server.packet.PacketType;
import co.casterlabs.flux.server.types.TubeID;
import co.casterlabs.flux.server.types.UserID;
import co.casterlabs.rakurai.json.annotating.JsonClass;

@JsonClass(exposeAll = true)
public record FFPacketMemberJoin(
    UserID member,
    UserID from,
    TubeID tube,
    PacketType type
) implements FromFluxPacket {

    public FFPacketMemberJoin(UserID member, TubeID tube) {
        this(member, Flux.CONTROL_PUBLISHER, tube, PacketType.MEMBER_JOIN);
    }

    @Override
    public int size() {
        return FromFluxPacket.super.size() + 2 + this.member.bytes().length;
    }

    @Override
    public void serialize(ByteWriter writer) throws IOException {
        FromFluxPacket.super.serialize(writer);

        byte[] member = this.member.bytes();
        writer.be.u16(member.length);
        writer.write(member);
    }

}
