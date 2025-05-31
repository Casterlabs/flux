package co.casterlabs.flux.server.protocols;

import java.util.Map;

import lombok.experimental.StandardException;

public sealed interface WireProtocol permits BinaryWireProtocol, StringWireProtocol {
    public static final Map<Type, WireProtocol> TYPES = Map.of(
        Type.JSON, _JsonWireProtocol.INSTANCE,
        Type.BINARY, _FluxByteWireProtocol.INSTANCE
    );

    public Type type();

    public static enum Type {
        JSON,
        BINARY
    }

    @StandardException
    public static class WireProtocolException extends Exception {
        private static final long serialVersionUID = -4287267622456707968L;
    }

}
