package co.casterlabs.flux.packets.protocols.bytes;

import java.io.IOException;

import co.casterlabs.commons.io.bytes.reading.ByteReader;
import co.casterlabs.commons.io.bytes.writing.ByteWriter;
import co.casterlabs.flux.packets.Packet;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
class _StaticPacketMarshall<T extends Packet> implements _Marshall<T> {
    private final T packet;

    @Override
    public T read(ByteReader reader) throws IOException {
        return this.packet;
    }

    @Override
    public void write(T packet, ByteWriter writer) throws IOException {
        // NOOP.
    }

}
