package co.casterlabs.flux.packets.protocols.json;

import co.casterlabs.flux.packets.Packet;
import co.casterlabs.flux.packets.Packet.PacketType;
import co.casterlabs.flux.packets.PacketAck;
import co.casterlabs.flux.packets.PacketError;
import co.casterlabs.flux.packets.PacketKeepAlive;
import co.casterlabs.flux.packets.PacketMemberJoin;
import co.casterlabs.flux.packets.PacketMemberLeave;
import co.casterlabs.flux.packets.PacketMembers;
import co.casterlabs.flux.packets.PacketMessage;
import co.casterlabs.flux.packets.PacketPublish;
import co.casterlabs.flux.packets.PacketSubscribe;
import co.casterlabs.flux.packets.PacketUnsubscribe;
import co.casterlabs.flux.packets.protocols.StringWireProtocol;
import co.casterlabs.flux.packets.protocols.WireProtocolException;
import co.casterlabs.rakurai.json.Rson;
import co.casterlabs.rakurai.json.element.JsonObject;
import co.casterlabs.rakurai.json.serialization.JsonParseException;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class JsonWireProtocol implements StringWireProtocol {
    public static final JsonWireProtocol INSTANCE = new JsonWireProtocol();

    @Override
    public Packet parse(String str) throws WireProtocolException {
        try {
            JsonObject obj = Rson.DEFAULT.fromJson(str, JsonObject.class);
            PacketType type = PacketType.valueOf(obj.getString("type").toUpperCase());
            return switch (type) {
                // @formatter:off
                case KEEP_ALIVE ->   PacketKeepAlive.INSTANCE;

                case ACK ->          Rson.DEFAULT.fromJson(obj, PacketAck.class);
                case ERROR ->        Rson.DEFAULT.fromJson(obj, PacketError.class);
                case MEMBER_JOIN ->  Rson.DEFAULT.fromJson(obj, PacketMemberJoin.class);
                case MEMBER_LEAVE -> Rson.DEFAULT.fromJson(obj, PacketMemberLeave.class);
                case MEMBERS ->      Rson.DEFAULT.fromJson(obj, PacketMembers.class);
                case MESSAGE ->      Rson.DEFAULT.fromJson(obj, PacketMessage.class);
                case PUBLISH ->      Rson.DEFAULT.fromJson(obj, PacketPublish.class);
                case SUBSCRIBE ->    Rson.DEFAULT.fromJson(obj, PacketSubscribe.class);
                case UNSUBSCRIBE ->  Rson.DEFAULT.fromJson(obj, PacketUnsubscribe.class);
                // @formatter:on
            };
        } catch (IllegalArgumentException e) {
            throw new WireProtocolException("Unknown packet type.");
        } catch (JsonParseException e) {
            throw new WireProtocolException("Failed to parse packet: " + e.getMessage(), e);
        }
    }

    @Override
    public String serialize(Packet packet) throws WireProtocolException {
        return Rson.DEFAULT
            .toJson(packet)
            .toString(false);
    }

    @Override
    public Type type() {
        return Type.JSON;
    }

}
