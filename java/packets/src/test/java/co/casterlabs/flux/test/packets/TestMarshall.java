package co.casterlabs.flux.test.packets;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;

import java.io.IOException;
import java.util.concurrent.ThreadLocalRandom;

import org.junit.jupiter.api.Test;

import co.casterlabs.commons.io.bytes.reading.ArrayByteReader;
import co.casterlabs.commons.io.bytes.reading.ByteReader;
import co.casterlabs.commons.io.bytes.writing.ArrayByteWriter;
import co.casterlabs.flux.packets.protocols.bytes._Marshall;

public class TestMarshall {

    @Test
    public void varstr16() throws IOException {
        byte[] bytes = new byte[128];
        ThreadLocalRandom.current().nextBytes(bytes);

        ArrayByteWriter writer = new ArrayByteWriter(0);
        _Marshall.varstr16(writer, bytes);

        ByteReader reader = new ArrayByteReader(writer.buffer());
        byte[] copy = _Marshall.varstr16(reader);

        assertArrayEquals(bytes, copy, "sane");
    }

    @Test
    public void varstr31() throws IOException {
        byte[] bytes = new byte[128];
        ThreadLocalRandom.current().nextBytes(bytes);

        ArrayByteWriter writer = new ArrayByteWriter(0);
        _Marshall.varstr31(writer, bytes);

        ByteReader reader = new ArrayByteReader(writer.buffer());
        byte[] copy = _Marshall.varstr31(reader);

        assertArrayEquals(bytes, copy, "sane");
    }

}
