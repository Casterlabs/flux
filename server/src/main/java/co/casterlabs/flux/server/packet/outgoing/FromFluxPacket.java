package co.casterlabs.flux.server.packet.outgoing;

import co.casterlabs.flux.server.types.TubeID;
import co.casterlabs.flux.server.types.UserID;

public interface FromFluxPacket {

    public UserID from();

    public TubeID tube();

    public Type type();

    static enum Type {
        KEEP_ALIVE,
        MEMBERS,
        MEMBER_JOIN,
        MEMBER_LEAVE,
        MESSAGE,
        ERROR,
        ACK,
    }

}
