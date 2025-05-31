package co.casterlabs.flux.server.protocols;

import java.io.IOException;

import co.casterlabs.commons.io.bytes.reading.ByteReader;
import co.casterlabs.commons.io.bytes.writing.ByteWriter;
import co.casterlabs.flux.server.packet.PacketType;
import co.casterlabs.flux.server.packet.incoming.TFPacketKeepAlive;
import co.casterlabs.flux.server.packet.incoming.TFPacketPublish;
import co.casterlabs.flux.server.packet.incoming.TFPacketSubscribe;
import co.casterlabs.flux.server.packet.incoming.TFPacketUnsubscribe;
import co.casterlabs.flux.server.packet.incoming.ToFluxPacket;
import co.casterlabs.flux.server.packet.outgoing.FromFluxPacket;
import co.casterlabs.flux.server.util.Profiler;
import co.casterlabs.flux.server.util.Profiler.Profile;

record _FluxByteWireProtocol(
    Type type
) implements BinaryWireProtocol {
    static final _FluxByteWireProtocol INSTANCE = new _FluxByteWireProtocol(Type.JSON);

    @Override
    public ToFluxPacket parse(ByteReader reader) throws WireProtocolException {
        Profile profile = Profiler.PROTOCOL_BINARY_PARSE.start();
        try {
            PacketType type = PacketType.LUT[reader.be.u8()];

            return switch (type) {
                // @formatter:off
                case KEEP_ALIVE  -> TFPacketKeepAlive.INSTANCE;
                case PUBLISH     -> TFPacketPublish.parse(reader);
                case SUBSCRIBE   -> TFPacketSubscribe.parse(reader);
                case UNSUBSCRIBE -> TFPacketUnsubscribe.parse(reader);
                
                default -> throw new WireProtocolException("Unsupported packet type: " + type);
                // @formatter:on
            };
        } catch (IOException e) {
            throw new WireProtocolException("Failed to parse packet: " + e.getMessage(), e);
        } finally {
            profile.end();
        }
    }

    @Override
    public void serialize(FromFluxPacket packet, ByteWriter writer) throws WireProtocolException {
        Profile profile = Profiler.PROTOCOL_BINARY_SERIALIZE.start();
        try {
            packet.serialize(writer);
        } catch (IOException e) {
            throw new WireProtocolException("Failed to serialize packet.", e);
        } finally {
            profile.end();
        }
    }

    @Override
    public int sizeOf(FromFluxPacket packet) {
        return packet.size();
    }

}
