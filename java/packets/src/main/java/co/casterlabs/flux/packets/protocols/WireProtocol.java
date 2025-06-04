package co.casterlabs.flux.packets.protocols;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

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

    public static final Map<Type, WireProtocol> TYPES = ALL.stream().collect(Collectors.toMap(p -> p.type(), p -> p));
    public static final Map<String, WireProtocol> MIMES = ALL.stream().collect(Collectors.toMap(p -> p.mime(), p -> p));

    public Type type();

    public String mime();

    public static enum Type {
        JSON,
        BYTES
    }

}
