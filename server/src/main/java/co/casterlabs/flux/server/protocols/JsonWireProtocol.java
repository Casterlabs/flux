package co.casterlabs.flux.server.protocols;

import co.casterlabs.flux.server.packet.incoming.TFPacketKeepAlive;
import co.casterlabs.flux.server.packet.incoming.TFPacketPublish;
import co.casterlabs.flux.server.packet.incoming.TFPacketSubscribe;
import co.casterlabs.flux.server.packet.incoming.TFPacketUnsubscribe;
import co.casterlabs.flux.server.packet.incoming.ToFluxPacket;
import co.casterlabs.flux.server.packet.outgoing.FromFluxPacket;
import co.casterlabs.flux.server.util.Profiler;
import co.casterlabs.flux.server.util.Profiler.Profile;
import co.casterlabs.rakurai.json.Rson;
import co.casterlabs.rakurai.json.element.JsonObject;
import co.casterlabs.rakurai.json.serialization.JsonParseException;

record JsonWireProtocol(
    Type type
) implements WireProtocol {
    static final JsonWireProtocol INSTANCE = new JsonWireProtocol(Type.JSON);

    @Override
    public ToFluxPacket parse(byte[] bytes) throws WireProtocolException {
        throw new WireProtocolException("Binary not supported.");
    }

    @Override
    public ToFluxPacket parse(String str) throws WireProtocolException {
        Profile profile = Profiler.PROTOCOL_JSON_PARSE.start();
        try {
            JsonObject obj = Rson.DEFAULT.fromJson(str, JsonObject.class);
            ToFluxPacket.Type type = ToFluxPacket.Type.valueOf(obj.getString("type").toUpperCase());
            return switch (type) {
                // @formatter:off
                case KEEP_ALIVE  -> TFPacketKeepAlive.INSTANCE;
                case PUBLISH     -> Rson.DEFAULT.fromJson(obj, TFPacketPublish.class);
                case SUBSCRIBE   -> Rson.DEFAULT.fromJson(obj, TFPacketSubscribe.class);
                case UNSUBSCRIBE -> Rson.DEFAULT.fromJson(obj, TFPacketUnsubscribe.class);
                // @formatter:on
            };
        } catch (IllegalArgumentException e) {
            throw new WireProtocolException("Unknown packet type.");
        } catch (JsonParseException e) {
            throw new WireProtocolException("Failed to parse packet: " + e.getMessage(), e);
        } finally {
            profile.end();
        }
    }

    @Override
    public Object serialize(FromFluxPacket packet) {
        Profile profile = Profiler.PROTOCOL_JSON_SERIALIZE.start();
        try {
            return Rson.DEFAULT
                .toJson(packet)
                .toString(false);
        } finally {
            profile.end();
        }
    }

    @Override
    public boolean supportsBinary() {
        return false;
    }

    @Override
    public boolean supportsText() {
        return true;
    }

}
