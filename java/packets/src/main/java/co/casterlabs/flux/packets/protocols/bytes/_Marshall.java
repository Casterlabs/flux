package co.casterlabs.flux.packets.protocols.bytes;

import java.io.IOException;

import co.casterlabs.commons.io.bytes.reading.ByteReader;
import co.casterlabs.commons.io.bytes.writing.ByteWriter;
import co.casterlabs.flux.packets.Packet;

public interface _Marshall<T extends Packet> {

    public T read(ByteReader reader) throws IOException;

    public void write(T packet, ByteWriter writer) throws IOException;

    public long sizeOf(T packet);

    public static void varstr16(ByteWriter writer, byte[] bytes) throws IOException {
        writer.be.u16(bytes.length);
        writer.write(bytes);
    }

    public static byte[] varstr16(ByteReader reader) throws IOException {
        int length = reader.be.u16();
        return reader.read(length);
    }

    public static void varstr31(ByteWriter writer, byte[] bytes) throws IOException {
        writer.be.s32(bytes.length);
        writer.write(bytes);
    }

    public static byte[] varstr31(ByteReader reader) throws IOException {
        int length = reader.be.s32();
        return reader.read(length);
    }

}
