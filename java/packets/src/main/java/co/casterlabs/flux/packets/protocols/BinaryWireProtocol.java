package co.casterlabs.flux.packets.protocols;

import co.casterlabs.commons.io.bytes.reading.ByteReader;
import co.casterlabs.commons.io.bytes.writing.ByteWriter;
import co.casterlabs.flux.packets.Packet;

public non-sealed interface BinaryWireProtocol extends WireProtocol {

    public Packet parse(ByteReader reader) throws WireProtocolException;

    public <P extends Packet> void serialize(P packet, ByteWriter writer) throws WireProtocolException;

}
