package co.casterlabs.flux.packets.protocols;

import java.util.List;
import java.util.Map;

import co.casterlabs.flux.packets.protocols.bytes.ByteWireProtocol;
import co.casterlabs.flux.packets.protocols.json.JsonWireProtocol;

/**
 * Implementing wire protocols MUST implement either BinaryWireProtocol or
 * StringWireProtocol.
 */
public sealed interface WireProtocol permits BinaryWireProtocol, StringWireProtocol {
    public static final List<WireProtocol> ALL = List.of(
        JsonWireProtocol.INSTANCE,
        ByteWireProtocol.INSTANCE
    );

    public static final Map<Type, WireProtocol> TYPES = Map.of(
        Type.JSON, JsonWireProtocol.INSTANCE,
        Type.BYTES, ByteWireProtocol.INSTANCE
    );

    public static final Map<String, WireProtocol> MIMES = Map.of(
        JsonWireProtocol.INSTANCE.mime(), JsonWireProtocol.INSTANCE,
        JsonWireProtocol.INSTANCE.mime(), ByteWireProtocol.INSTANCE
    );

    public Type type();

    public String mime();

    public static enum Type {
        JSON,
        BYTES
    }

}
