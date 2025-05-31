package co.casterlabs.flux.server.protocols;

import co.casterlabs.flux.server.packet.incoming.ToFluxPacket;
import co.casterlabs.flux.server.packet.outgoing.FromFluxPacket;

public non-sealed interface StringWireProtocol extends WireProtocol {

    public ToFluxPacket parse(String str) throws WireProtocolException;

    public String serialize(FromFluxPacket packet) throws WireProtocolException;

}
