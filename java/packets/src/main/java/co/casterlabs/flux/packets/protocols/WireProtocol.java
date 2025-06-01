package co.casterlabs.flux.packets.protocols;

import java.util.Map;

import co.casterlabs.flux.packets.protocols.bytes.ByteWireProtocol;
import co.casterlabs.flux.packets.protocols.json.JsonWireProtocol;

/**
 * Implementing wire protocols MUST implement either BinaryWireProtocol or
 * StringWireProtocol.
 */
public sealed interface WireProtocol permits BinaryWireProtocol, StringWireProtocol {
    public static final Map<Type, WireProtocol> TYPES = Map.of(
        Type.JSON, JsonWireProtocol.INSTANCE,
        Type.BYTES, ByteWireProtocol.INSTANCE
    );

    public Type type();

    public static enum Type {
        JSON,
        BYTES
    }

}
