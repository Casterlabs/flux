package co.casterlabs.flux.packets.protocols.bytes;

import java.io.IOException;
import java.util.Map;

import co.casterlabs.commons.io.bytes.reading.ByteReader;
import co.casterlabs.commons.io.bytes.writing.ByteWriter;
import co.casterlabs.flux.packets.Packet;
import co.casterlabs.flux.packets.Packet.PacketType;
import co.casterlabs.flux.packets.PacketKeepAlive;
import co.casterlabs.flux.packets.protocols.BinaryWireProtocol;
import co.casterlabs.flux.packets.protocols.WireProtocolException;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class ByteWireProtocol implements BinaryWireProtocol {
    public static final ByteWireProtocol INSTANCE = new ByteWireProtocol();

    private static final Map<PacketType, _Marshall<?>> MARSHALLS = Map.of(
        PacketType.KEEP_ALIVE, new _StaticPacketMarshall<>(PacketKeepAlive.INSTANCE),

        PacketType.ACK, new _PacketAckMarshall(),
        PacketType.ERROR, new _PacketErrorMarshall(),
        PacketType.MEMBERS, new _PacketMembersMarshall(),
        PacketType.MEMBER_JOIN, new _PacketMemberJoinMarshall(),
        PacketType.MEMBER_LEAVE, new _PacketMemberLeaveMarshall(),
        PacketType.MESSAGE, new _PacketMessageMarshall(),
        PacketType.PUBLISH, new _PacketPublishMarshall(),
        PacketType.SUBSCRIBE, new _PacketSubscribeMarshall(),
        PacketType.UNSUBSCRIBE, new _PacketUnsubscribeMarshall()
    );

    @Override
    public Packet parse(ByteReader reader) throws WireProtocolException {
        try {
            PacketType type = PacketType.LUT[reader.be.u8()];

            _Marshall<?> marshall = MARSHALLS.get(type);
            if (marshall == null) {
                throw new RuntimeException("Unhandled packet type: " + type);
            }

            return marshall.read(reader);
        } catch (IOException e) {
            throw new WireProtocolException("Failed to parse packet: " + e.getMessage(), e);
        }
    }

    @SuppressWarnings("unchecked")
    @Override
    public <P extends Packet> void serialize(P packet, ByteWriter writer) throws WireProtocolException {
        try {
            _Marshall<P> marshall = (_Marshall<P>) MARSHALLS.get(packet.type());
            if (marshall == null) {
                throw new RuntimeException("Unhandled packet type: " + packet.type());
            }

            writer.be.u8(packet.type().id); // type
            marshall.write(packet, writer);
        } catch (IOException e) {
            throw new WireProtocolException("Failed to serialize packet.", e);
        }
    }

    @SuppressWarnings("unchecked")
    @Override
    public <P extends Packet> long sizeOf(P packet) {
        _Marshall<P> marshall = (_Marshall<P>) MARSHALLS.get(packet.type());
        if (marshall == null) {
            throw new RuntimeException("Unhandled packet type: " + packet.type());
        }

        return 1 + marshall.sizeOf(packet); // +1 for the type
    }

    @Override
    public Type type() {
        return Type.BYTES;
    }

    @Override
    public String mime() {
        return "application/octet-stream";
    }

}
