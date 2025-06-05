package co.casterlabs.flux.packets.protocols.bytes;

import java.io.IOException;

import co.casterlabs.commons.io.bytes.reading.ByteReader;
import co.casterlabs.commons.io.bytes.writing.ByteWriter;
import co.casterlabs.flux.packets.PacketMemberJoin;
import co.casterlabs.flux.packets.types.TubeID;
import co.casterlabs.flux.packets.types.UserID;

class _PacketMemberJoinMarshall implements _Marshall<PacketMemberJoin> {

    @Override
    public PacketMemberJoin read(ByteReader reader) throws IOException {
        TubeID tube = new TubeID(_Marshall.varstr16(reader));
        UserID member = new UserID(_Marshall.varstr16(reader));
        return new PacketMemberJoin(tube, member);
    }

    @Override
    public void write(PacketMemberJoin packet, ByteWriter writer) throws IOException {
        _Marshall.varstr16(writer, packet.tube.bytes());
        _Marshall.varstr16(writer, packet.member.bytes());
    }

}
