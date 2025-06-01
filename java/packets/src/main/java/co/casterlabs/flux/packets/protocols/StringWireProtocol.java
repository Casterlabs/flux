package co.casterlabs.flux.packets.protocols;

import co.casterlabs.flux.packets.Packet;

public non-sealed interface StringWireProtocol extends WireProtocol {

    public Packet parse(String str) throws WireProtocolException;

    public String serialize(Packet packet) throws WireProtocolException;

}
