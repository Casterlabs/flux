package co.casterlabs.flux.server.protocols;

import java.util.Map;

import co.casterlabs.flux.server.packet.incoming.ToFluxPacket;
import co.casterlabs.flux.server.packet.outgoing.FromFluxPacket;
import lombok.experimental.StandardException;

public interface WireProtocol {
    public static final Map<Type, WireProtocol> TYPES = Map.of(
        Type.JSON, JsonWireProtocol.INSTANCE
    );

    public ToFluxPacket parse(String str) throws WireProtocolException;

    public ToFluxPacket parse(byte[] bytes) throws WireProtocolException;

    public Object /* Either String or byte[] */ serialize(FromFluxPacket packet);

    public Type type();

    public boolean supportsBinary();

    public boolean supportsText();

    public static enum Type {
        JSON,
    }

    @StandardException
    public static class WireProtocolException extends Exception {
        private static final long serialVersionUID = -4287267622456707968L;
    }

}
