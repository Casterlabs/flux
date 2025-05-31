package co.casterlabs.flux.server.packet.outgoing;

import co.casterlabs.flux.server.Flux;
import co.casterlabs.flux.server.packet.PacketType;
import co.casterlabs.flux.server.types.TubeID;
import co.casterlabs.flux.server.types.UserID;
import co.casterlabs.rakurai.json.annotating.JsonClass;

@JsonClass(exposeAll = true)
public class FFPacketKeepAlive extends _StaticFFPacket {
    public static final FFPacketKeepAlive INSTANCE = new FFPacketKeepAlive();

    @Override
    public UserID from() {
        return Flux.CONTROL_PUBLISHER;
    }

    @Override
    public TubeID tube() {
        return Flux.CONTROL_TUBEID;
    }

    @Override
    public PacketType type() {
        return PacketType.KEEP_ALIVE;
    }

}
