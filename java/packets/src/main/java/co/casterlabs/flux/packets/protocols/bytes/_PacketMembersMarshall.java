package co.casterlabs.flux.packets.protocols.bytes;

import java.io.IOException;

import co.casterlabs.commons.io.bytes.reading.ByteReader;
import co.casterlabs.commons.io.bytes.writing.ByteWriter;
import co.casterlabs.flux.packets.PacketMembers;
import co.casterlabs.flux.packets.types.TubeID;
import co.casterlabs.flux.packets.types.UserID;

class _PacketMembersMarshall implements _Marshall<PacketMembers> {

    @Override
    public PacketMembers read(ByteReader reader) throws IOException {
        TubeID tube = new TubeID(_Marshall.varstr16(reader));

        UserID[] members = new UserID[reader.be.s32()];
        for (int i = 0; i < members.length; i++) {
            members[i] = new UserID(_Marshall.varstr16(reader));
        }

        return new PacketMembers(tube, members);
    }

    @Override
    public void write(PacketMembers packet, ByteWriter writer) throws IOException {
        _Marshall.varstr16(writer, packet.tube.bytes());

        writer.be.s32(packet.members.length);
        for (UserID member : packet.members) {
            _Marshall.varstr16(writer, member.bytes());
        }
    }

}
