package co.casterlabs.flux.server.packet.outgoing;

import java.io.IOException;

import co.casterlabs.commons.io.bytes.ByteSizer;
import co.casterlabs.commons.io.bytes.writing.ByteWriter;
import co.casterlabs.flux.server.packet.PacketType;
import co.casterlabs.flux.server.types.TubeID;
import co.casterlabs.flux.server.types.UserID;

public interface FromFluxPacket {

    public UserID from();

    public TubeID tube();

    public PacketType type();

    public default int size() {
        return new ByteSizer()
            .b8() // type
            .b16() // from length
            .b16() // tube length
            .result() +
            this.from().bytes().length +
            this.tube().bytes().length;
    }

    public default void serialize(ByteWriter writer) throws IOException {
        writer.be.u8(this.type().id);

        byte[] from = this.from().bytes();
        writer.be.u16(from.length);
        writer.write(from);

        byte[] tube = this.tube().bytes();
        writer.be.u16(tube.length);
        writer.write(tube);
    }

}
