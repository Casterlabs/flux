package co.casterlabs.flux.server.protocols;

import co.casterlabs.commons.io.bytes.reading.ByteReader;
import co.casterlabs.commons.io.bytes.writing.ByteWriter;
import co.casterlabs.flux.server.packet.incoming.ToFluxPacket;
import co.casterlabs.flux.server.packet.outgoing.FromFluxPacket;

public non-sealed interface BinaryWireProtocol extends WireProtocol {

    public ToFluxPacket parse(ByteReader reader) throws WireProtocolException;

    public void serialize(FromFluxPacket packet, ByteWriter writer) throws WireProtocolException;

    public int sizeOf(FromFluxPacket packet);

}
