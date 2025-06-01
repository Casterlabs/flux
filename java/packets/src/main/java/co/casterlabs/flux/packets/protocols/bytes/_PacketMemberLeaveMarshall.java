package co.casterlabs.flux.packets.protocols.bytes;

import java.io.IOException;

import co.casterlabs.commons.io.bytes.reading.ByteReader;
import co.casterlabs.commons.io.bytes.writing.ByteWriter;
import co.casterlabs.flux.packets.PacketMemberLeave;
import co.casterlabs.flux.packets.types.TubeID;
import co.casterlabs.flux.packets.types.UserID;

class _PacketMemberLeaveMarshall implements _Marshall<PacketMemberLeave> {

    @Override
    public PacketMemberLeave read(ByteReader reader) throws IOException {
        TubeID tube = new TubeID(_Marshall.varstr16(reader));
        UserID member = new UserID(_Marshall.varstr16(reader));

        return new PacketMemberLeave(tube, member);
    }

    @Override
    public void write(PacketMemberLeave packet, ByteWriter writer) throws IOException {
        _Marshall.varstr16(writer, packet.tube.bytes());
        _Marshall.varstr16(writer, packet.member.bytes());
    }

    @Override
    public long sizeOf(PacketMemberLeave packet) {
        return 2 + packet.tube.bytes().length // tube
            + 2 + packet.member.bytes().length; // member
    }

}
